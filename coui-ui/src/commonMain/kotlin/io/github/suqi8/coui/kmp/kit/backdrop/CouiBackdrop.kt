// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isFinite
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.InternalCouiBlurApi
import io.github.suqi8.coui.kmp.blur.acquireBackdropSampling
import io.github.suqi8.coui.kmp.blur.isReadyForSampling
import io.github.suqi8.coui.kmp.blur.releaseBackdropSampling
import io.github.suqi8.coui.kmp.kit.diagnostic.CouiDiagnosticJsonObject
import io.github.suqi8.coui.kmp.kit.diagnostic.CouiRenderDiagnostics
import io.github.suqi8.coui.kmp.kit.diagnostic.diagnosticCoordinates

/** Attach each source to a single content node that does not sample itself; [parent] stacks the owning window. */
@Composable
internal fun rememberCouiBackdrop(
    parent: CouiBackdrop? = null,
    diagnosticName: String? = null,
    drawSource: ContentDrawScope.() -> Unit = { drawContent() },
): CouiBackdrop {
    var diagnosticGeneration by remember { mutableIntStateOf(0) }
    val layer = key(diagnosticGeneration) { rememberGraphicsLayer() }
    val source = rememberUpdatedState(drawSource)
    return remember(layer, parent, diagnosticName) {
        CouiBackdrop(layer, parent, diagnosticName, { diagnosticGeneration++ }) { source.value(this) }
    }
}

internal fun Modifier.couiBackdrop(backdrop: CouiBackdrop): Modifier = then(BackdropElement(backdrop))

/** Holds only the source layer and coordinates without effect, animation, or input state. */
@OptIn(InternalCouiBlurApi::class)
internal class CouiBackdrop internal constructor(
    private val layer: GraphicsLayer?,
    private val parent: CouiBackdrop?,
    internal val diagnosticName: String?,
    private val recreateForDiagnostics: () -> Unit,
    private val drawSource: ContentDrawScope.() -> Unit,
) {
    private var backdropDelegate: Backdrop? = null
    private var delegateDensity: Density? = null

    // The density is captured up front: reading DrawScope.density inside a nested GraphicsLayer
    // recording (the floating surface patches) resolves to the recording scope itself and loops.
    internal constructor(backdrop: Backdrop, density: Density) : this(
        layer = null,
        parent = null,
        diagnosticName = "BackdropAdapter",
        recreateForDiagnostics = {},
        drawSource = {},
    ) {
        this.backdropDelegate = backdrop
        this.delegateDensity = density
    }

    internal val hasContent: Boolean
        get() = if (backdropDelegate != null) {
            backdropDelegate!!.isReadyForSampling
        } else {
            recorded && coordinates?.isAttached == true
        }

    private var drawPassCount = 0L
    private var recordCallbackCount = 0L
    private var sampleCount = 0L
    private var lastRecordMs = 0L
    private var isolateWhenIdle = true
    private var activeConsumers = 0
    private val redrawSignal = mutableStateOf(Unit, neverEqualPolicy())
    private val sourceToTarget = Matrix()

    // Initial sampling may run before the source draws; readiness state wakes waiting consumers.
    private var recorded by mutableStateOf(false)
    internal var coordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())

    init {
        layer?.let {
            it.compositingStrategy = CompositingStrategy.Offscreen
        }
    }

    internal fun setIsolateWhenIdle(isolate: Boolean) {
        isolateWhenIdle = isolate
        updateCompositingStrategy()
    }

    /** Material surfaces outlive exit transitions, so source and parent chain retention stays paired to composition. */
    @Composable
    internal fun RetainWhileComposed() {
        val delegate = backdropDelegate
        if (delegate != null) {
            DisposableEffect(delegate) {
                delegate.acquireBackdropSampling()
                onDispose { delegate.releaseBackdropSampling() }
            }
        } else if (layer != null) {
            DisposableEffect(this) {
                retain()
                onDispose { release() }
            }
        }
    }

    private fun retain() {
        val delegate = backdropDelegate
        if (delegate != null) {
            delegate.acquireBackdropSampling()
            return
        }
        activeConsumers++
        parent?.retain()
        updateCompositingStrategy()
    }

    private fun release() {
        val delegate = backdropDelegate
        if (delegate != null) {
            delegate.releaseBackdropSampling()
            return
        }
        check(activeConsumers > 0) { "Backdrop consumers must be released in pairs" }
        activeConsumers--
        parent?.release()
        updateCompositingStrategy()
    }

    private fun updateCompositingStrategy() {
        val l = layer ?: return
        if (l.isReleased) return
        // Keep source commands, coordinates, and clip; rasterize at native resolution while consumers are active.
        l.compositingStrategy = if (isolateWhenIdle || activeConsumers > 0) {
            CompositingStrategy.Offscreen
        } else {
            CompositingStrategy.Auto
        }
    }

    /** Target [DrawScope] origin must align with [target]; extended sampling regions are translated by the caller. */
    fun draw(scope: DrawScope, target: LayoutCoordinates) {
        val delegate = backdropDelegate
        if (delegate != null) {
            if (!target.isAttached) return
            with(delegate) {
                scope.drawBackdrop(
                    density = delegateDensity ?: return,
                    coordinates = target,
                    layerBlock = null,
                    downscaleFactor = 1,
                )
            }
            return
        }
        if (CouiRenderDiagnostics.enabled) sampleCount++
        parent?.draw(scope, target)
        val source = coordinates ?: return
        val l = layer ?: return
        if (!recorded || !source.isAttached || !target.isAttached) return
        val sameWindow = source.findRootCoordinates() === target.findRootCoordinates()
        // Same-window sampling relies on RenderNode dependency tracking; cross-window subscribes to PreDraw.
        if (!sameWindow) redrawSignal.value
        val width = source.size.width.toFloat()
        val height = source.size.height.toFloat()
        if (width <= 0f || height <= 0f) return
        val origin: Offset
        val right: Offset
        val bottom: Offset
        if (sameWindow) {
            origin = target.localPositionOf(source, Offset.Zero)
            right = target.localPositionOf(source, Offset(width, 0f))
            bottom = target.localPositionOf(source, Offset(0f, height))
        } else {
            val screenOrigin = target.screenToLocal(source.localToScreen(Offset.Zero))
            val screenRight = target.screenToLocal(source.localToScreen(Offset(width, 0f)))
            val screenBottom = target.screenToLocal(source.localToScreen(Offset(0f, height)))
            if (screenOrigin.isFinite && screenRight.isFinite && screenBottom.isFinite) {
                origin = screenOrigin
                right = screenRight
                bottom = screenBottom
            } else {
                origin = target.windowToLocal(source.localToWindow(Offset.Zero))
                right = target.windowToLocal(source.localToWindow(Offset(width, 0f)))
                bottom = target.windowToLocal(source.localToWindow(Offset(0f, height)))
            }
        }
        if (!origin.isFinite || !right.isFinite || !bottom.isFinite) return
        val xAxis = (right - origin) / width
        val yAxis = (bottom - origin) / height
        // Shortcut: supports 2D affine transforms only; 3D component flips require a full projection matrix.
        sourceToTarget[0, 0] = xAxis.x
        sourceToTarget[0, 1] = xAxis.y
        sourceToTarget[1, 0] = yAxis.x
        sourceToTarget[1, 1] = yAxis.y
        sourceToTarget[3, 0] = origin.x
        sourceToTarget[3, 1] = origin.y
        scope.withTransform({ transform(sourceToTarget) }) { drawLayer(l) }
    }

    internal fun recordAndDraw(scope: ContentDrawScope) {
        val l = layer ?: return
        if (CouiRenderDiagnostics.enabled) drawPassCount++
        scope.apply {
            // Redirect Canvas via DrawScope.record so fallback background and drawContent share one recording.
            l.record {
                if (CouiRenderDiagnostics.enabled) {
                    recordCallbackCount++
                    lastRecordMs = CouiRenderDiagnostics.uptimeMs()
                }
                drawSource(this@apply)
            }
            // Refresh the rectangular outline explicitly on Skiko where SkiaGraphicsLayer.setSize
            // keeps a cached internalOutline (0x0 if clip=true ran before first record).
            l.setRectOutline(Offset.Zero, size)
            l.clip = true
            drawLayer(l)
        }
        recorded = true
        invalidate()
    }

    internal fun invalidate() {
        redrawSignal.value = Unit
    }

    internal fun detach() {
        coordinates = null
        recorded = false
        invalidate()
    }

    internal fun diagnosticStateJson(): CouiDiagnosticJsonObject = CouiDiagnosticJsonObject()
        .put("name", diagnosticName ?: "unnamed")
        .put("layerId", layer?.backdropLayerId() ?: -1L)
        .put("ownerViewId", layer?.backdropOwnerViewId() ?: -1L)
        .put("parentLayerId", parent?.layer?.backdropLayerId())
        .put("recorded", recorded)
        .put("released", layer?.isReleased ?: false)
        .put("width", layer?.size?.width ?: 0)
        .put("height", layer?.size?.height ?: 0)
        .put("alpha", layer?.alpha?.toDouble() ?: 1.0)
        .put("clip", layer?.clip ?: false)
        .put("outline", layer?.outline?.toString() ?: "null")
        .put("scaleX", layer?.scaleX?.toDouble() ?: 1.0)
        .put("scaleY", layer?.scaleY?.toDouble() ?: 1.0)
        .put("translationX", layer?.translationX?.toDouble() ?: 0.0)
        .put("translationY", layer?.translationY?.toDouble() ?: 0.0)
        .put("blendMode", layer?.blendMode?.toString() ?: "null")
        .put("compositingStrategy", layer?.compositingStrategy?.toString() ?: "null")
        .put("isolateWhenIdle", isolateWhenIdle)
        .put("activeConsumers", activeConsumers)
        .put("drawPassCount", drawPassCount)
        .put("recordCallbackCount", recordCallbackCount)
        .put("sampleCount", sampleCount)
        .put("lastRecordMs", lastRecordMs)
        .put("coordinates", diagnosticCoordinates(coordinates))

    internal fun diagnosticState(): String = diagnosticStateJson().toJsonString()

    internal suspend fun diagnosticCapture(): ImageBitmap {
        val l = checkNotNull(layer) { "Backdrop layer is unavailable: $diagnosticName" }
        check(recorded && !l.isReleased) { "Backdrop layer is not recorded or already released: $diagnosticName" }
        return l.toImageBitmap().also {
            check(recorded && !l.isReleased) { "Backdrop layer invalidated during capture: $diagnosticName" }
        }
    }

    internal fun diagnosticRecreate() {
        recreateForDiagnostics()
    }
}

private data class BackdropElement(val backdrop: CouiBackdrop) : ModifierNodeElement<BackdropNode>() {
    override fun create() = BackdropNode(backdrop)

    override fun update(node: BackdropNode) {
        if (node.backdrop === backdrop) return
        val coordinates = node.backdrop.coordinates
        CouiRenderDiagnostics.detachSource(node.backdrop)
        node.backdrop.detach()
        node.backdrop = backdrop
        backdrop.coordinates = coordinates
        if (node.isAttached) CouiRenderDiagnostics.attachSource(backdrop) { node.invalidateDraw() }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "couiBackdrop"
    }
}

private class BackdropNode(var backdrop: CouiBackdrop) :
    Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode {
    private val platformObserver = CouiBackdropPlatformObserver()

    override fun onAttach() {
        val diagnosticsEnabled = CouiRenderDiagnostics.enabled
        platformObserver.attach(
            node = this,
            onPreDraw = { backdrop.invalidate() },
            trackWindowFocus = diagnosticsEnabled,
            onWindowFocusChanged = { hasFocus ->
                CouiRenderDiagnostics.event("window.focus source=${backdrop.diagnosticName} focused=$hasFocus")
            },
        )
        if (diagnosticsEnabled) {
            CouiRenderDiagnostics.attachSource(backdrop) { invalidateDraw() }
        }
    }

    override fun onDetach() {
        platformObserver.detach(this)
        CouiRenderDiagnostics.detachSource(backdrop)
        backdrop.detach()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        backdrop.coordinates = coordinates
    }

    override fun ContentDrawScope.draw() = backdrop.recordAndDraw(this)
}

internal expect fun GraphicsLayer.backdropLayerId(): Long

internal expect fun GraphicsLayer.backdropOwnerViewId(): Long

internal expect class CouiBackdropPlatformObserver() {
    fun attach(
        node: Modifier.Node,
        onPreDraw: () -> Unit,
        trackWindowFocus: Boolean,
        onWindowFocusChanged: (Boolean) -> Unit,
    )

    fun detach(node: Modifier.Node)
}
