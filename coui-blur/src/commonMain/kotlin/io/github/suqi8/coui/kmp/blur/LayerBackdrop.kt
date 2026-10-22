// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.blur.internal.InverseLayerScope

private val DefaultOnDraw: ContentDrawScope.() -> Unit = { drawContent() }

/**
 * Creates and remembers a [LayerBackdrop] that captures content from a [GraphicsLayer].
 *
 * Use [Modifier.layerBackdrop][layerBackdrop] on the content container to capture its
 * rendered output, then pass this [LayerBackdrop] to blur modifiers.
 *
 * The instance is keyed only on [graphicsLayer]; [onDraw] is read via [rememberUpdatedState]
 * so a fresh lambda each frame does not rebuild the backdrop and reset its layer coordinates.
 *
 * A backdrop must be recorded by exactly one [layerBackdrop] node; see that modifier for the
 * ownership rule and why it must not be applied per pager page.
 *
 * @param graphicsLayer The graphics layer to record content into.
 * @param onDraw Custom draw logic for the layer content.
 */
@Composable
fun rememberLayerBackdrop(
    graphicsLayer: GraphicsLayer = rememberGraphicsLayer(),
    onDraw: ContentDrawScope.() -> Unit = DefaultOnDraw,
): LayerBackdrop {
    val currentOnDraw by rememberUpdatedState(onDraw)
    return remember(graphicsLayer) {
        LayerBackdrop(graphicsLayer) { currentOnDraw() }
    }
}

/**
 * A [Backdrop] that draws from a captured [GraphicsLayer].
 * The layer content is captured via [Modifier.layerBackdrop][layerBackdrop].
 *
 * A [LayerBackdrop] has a single graphics layer and a single set of layer coordinates, so it supports
 * exactly one attached [layerBackdrop] node at a time. Attaching a second node while another is
 * still attached is a misuse: the most recently attached node becomes the owner and the others stop
 * recording and publishing coordinates (they can no longer overwrite the owner's registration or
 * clear it when they detach). In particular, do not attach one backdrop to every page of a pager;
 * record once on the container that wraps the whole pager.
 *
 * @property graphicsLayer The [GraphicsLayer] holding the captured content drawn as the backdrop.
 */
@Stable
class LayerBackdrop internal constructor(
    val graphicsLayer: GraphicsLayer,
    internal val onDraw: ContentDrawScope.() -> Unit,
) : Backdrop {

    override val isCoordinatesDependent: Boolean = true

    internal var layerCoordinates: LayoutCoordinates? by mutableStateOf(null)

    // Attached nodes in attach order; the last one is the owner. Only touched on attach/detach.
    private val attachedNodes = ArrayList<LayerBackdropNode>(1)

    /**
     * The node allowed to record into [graphicsLayer] and to publish [layerCoordinates]: the most
     * recently attached one. Plain field so the draw path can compare it without allocating.
     */
    internal var owner: LayerBackdropNode? = null
        private set

    /** The number of [layerBackdrop] nodes currently attached; anything above 1 is a misuse. */
    internal val attachedNodeCount: Int get() = attachedNodes.size

    internal fun attachNode(node: LayerBackdropNode) {
        attachedNodes.remove(node)
        attachedNodes.add(node)
        // A previous owner keeps drawing its content but stops recording; the new owner publishes
        // its own coordinates once positioned.
        owner = node
        layerCoordinates = node.attachedCoordinates
    }

    internal fun detachNode(node: LayerBackdropNode) {
        if (!attachedNodes.remove(node)) return
        if (owner !== node) return
        // Only the owner may clear the registration; hand it to the previous node, if any.
        val next = if (attachedNodes.isEmpty()) null else attachedNodes[attachedNodes.lastIndex]
        owner = next
        layerCoordinates = next?.attachedCoordinates
        next?.invalidateDraw()
    }

    override var offsetResidualX: Float = 0f
        internal set
    override var offsetResidualY: Float = 0f
        internal set

    private var inverseLayerScope: InverseLayerScope? = null

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Int,
    ) {
        val coordinates = coordinates ?: return
        val layerCoordinates = layerCoordinates ?: return

        val offset = try {
            layerCoordinates.localPositionOf(coordinates)
        } catch (_: Exception) {
            coordinates.positionInWindow() - layerCoordinates.positionInWindow()
        }

        val consumerSize = (density as? BackdropEffectScope)?.size ?: size

        withTransform({
            if (layerBlock != null) {
                with(obtainInverseLayerScope()) { inverseTransform(density, consumerSize, layerBlock) }
            }
            if (downscaleFactor > 1) {
                val inv = 1f / downscaleFactor
                val scaledX = offset.x * inv
                val scaledY = offset.y * inv
                val roundedX = kotlin.math.round(scaledX * 0.5f).toInt().toFloat() * 2f
                val roundedY = kotlin.math.round(scaledY * 0.5f).toInt().toFloat() * 2f
                offsetResidualX = (scaledX - roundedX) * downscaleFactor
                offsetResidualY = (scaledY - roundedY) * downscaleFactor
                translate(-roundedX, -roundedY)
                scale(inv, inv, Offset.Zero)
            } else {
                offsetResidualX = 0f
                offsetResidualY = 0f
                translate(-offset.x, -offset.y)
            }
        }) {
            drawLayer(graphicsLayer)
        }
    }

    private fun obtainInverseLayerScope(): InverseLayerScope = inverseLayerScope?.apply { reset() }
        ?: InverseLayerScope().also { inverseLayerScope = it }
}
