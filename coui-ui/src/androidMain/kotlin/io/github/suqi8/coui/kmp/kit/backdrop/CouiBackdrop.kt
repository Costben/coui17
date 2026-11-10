// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.backdrop

import android.os.SystemClock
import android.view.ViewTreeObserver
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
import androidx.compose.ui.node.requireView
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.InternalCouiBlurApi
import io.github.suqi8.coui.kmp.blur.acquireBackdropSampling
import io.github.suqi8.coui.kmp.blur.isReadyForSampling
import io.github.suqi8.coui.kmp.blur.releaseBackdropSampling
import io.github.suqi8.coui.kmp.kit.diagnostic.CouiRenderDiagnostics
import io.github.suqi8.coui.kmp.kit.diagnostic.diagnosticCoordinates
import org.json.JSONObject

/** 每个源只挂到一个内容节点, 该子树不得采样自身。parent 用于叠加所属窗口。 */
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

/** 只持有源图层和坐标, 不持有效果、组件动画或输入状态。 */
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

    // 首次采样可能先于源绘制, 就绪状态必须唤醒已等待的消费者。
    private var recorded by mutableStateOf(false)
    internal var coordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())

    init {
        layer?.let {
            it.compositingStrategy = CompositingStrategy.Offscreen
            it.clip = true
        }
    }

    internal fun setIsolateWhenIdle(isolate: Boolean) {
        isolateWhenIdle = isolate
        updateCompositingStrategy()
    }

    /** 材质组合在退出动画期间仍须存在, 源及父链随该组合成对保留和释放。 */
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
        check(activeConsumers > 0) { "背景源消费者必须成对释放" }
        activeConsumers--
        parent?.release()
        updateCompositingStrategy()
    }

    private fun updateCompositingStrategy() {
        val l = layer ?: return
        if (l.isReleased) return
        // 保留源的指令、坐标和裁剪; 有消费者时仍先按原分辨率栅格化一次。
        l.compositingStrategy = if (isolateWhenIdle || activeConsumers > 0) {
            CompositingStrategy.Offscreen
        } else {
            CompositingStrategy.Auto
        }
    }

    /** 目标 DrawScope 原点须对应 target; 扩展采样区由调用方平移。 */
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
        // 同窗口沿用 RenderNode 依赖更新; 仅跨窗口订阅 PreDraw, 避免持续驱动页面重绘。
        if (source.findRootCoordinates() !== target.findRootCoordinates()) redrawSignal.value
        val width = source.size.width.toFloat()
        val height = source.size.height.toFloat()
        if (width <= 0f || height <= 0f) return
        val origin = target.screenToLocal(source.localToScreen(Offset.Zero))
        val right = target.screenToLocal(source.localToScreen(Offset(width, 0f)))
        val bottom = target.screenToLocal(source.localToScreen(Offset(0f, height)))
        if (!origin.isFinite || !right.isFinite || !bottom.isFinite) return
        val xAxis = (right - origin) / width
        val yAxis = (bottom - origin) / height
        // shortcut: 仅支持二维仿射; 组件引入三维翻转时需使用完整投影矩阵。
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
            // 使用 DrawScope 的 record 重定向 Canvas, 补底和 drawContent 共用同一份录制。
            l.record {
                if (CouiRenderDiagnostics.enabled) {
                    recordCallbackCount++
                    lastRecordMs = SystemClock.uptimeMillis()
                }
                drawSource(this@apply)
            }
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

    internal fun diagnosticState(): JSONObject = JSONObject()
        .put("name", diagnosticName ?: "unnamed")
        .put("layerId", layer?.layerId ?: -1L)
        .put("ownerViewId", layer?.ownerViewId ?: -1)
        .put("parentLayerId", parent?.layer?.layerId ?: JSONObject.NULL)
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

    internal suspend fun diagnosticCapture(): ImageBitmap {
        val l = checkNotNull(layer) { "采样层尚未就绪或不可用: $diagnosticName" }
        check(recorded && !l.isReleased) { "采样层尚未就绪或已释放: $diagnosticName" }
        return l.toImageBitmap().also {
            check(recorded && !l.isReleased) { "采样层在导出期间失效: $diagnosticName" }
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
    GlobalPositionAwareModifierNode,
    ViewTreeObserver.OnPreDrawListener,
    ViewTreeObserver.OnWindowFocusChangeListener {
    private var observer: ViewTreeObserver? = null

    override fun onAttach() {
        // Dialog 内必须从实际节点取得 View, 不使用外层 Composition 的 LocalView。
        observer = requireView().viewTreeObserver.also { it.addOnPreDrawListener(this) }
        if (CouiRenderDiagnostics.enabled) {
            observer?.addOnWindowFocusChangeListener(this)
            CouiRenderDiagnostics.attachSource(backdrop) { invalidateDraw() }
        }
    }

    override fun onDetach() {
        val current = observer?.takeIf { it.isAlive } ?: requireView().viewTreeObserver
        current.removeOnPreDrawListener(this)
        current.removeOnWindowFocusChangeListener(this)
        CouiRenderDiagnostics.detachSource(backdrop)
        observer = null
        backdrop.detach()
    }

    override fun onPreDraw(): Boolean {
        backdrop.invalidate()
        return true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        CouiRenderDiagnostics.event("window.focus source=${backdrop.diagnosticName} focused=$hasFocus")
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        backdrop.coordinates = coordinates
    }

    override fun ContentDrawScope.draw() = backdrop.recordAndDraw(this)
}
