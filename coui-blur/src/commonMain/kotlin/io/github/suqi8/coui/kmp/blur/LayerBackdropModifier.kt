// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import io.github.suqi8.coui.kmp.blur.internal.recordLayer

/**
 * Captures the content of this composable into the given [LayerBackdrop]'s graphics layer.
 * Place this modifier on the container whose content should appear as the blurred background.
 *
 * **One node per [LayerBackdrop].** A [LayerBackdrop] owns a single graphics layer and a single
 * set of layer coordinates, so it can be recorded by exactly one `layerBackdrop` node at a time.
 * If several nodes are attached to the same backdrop, the most recently attached one owns it:
 * only the owner records into the layer and publishes coordinates, the others merely draw their
 * content. When the owner detaches, ownership falls back to the most recently attached remaining
 * node. Never rely on this fallback; it only keeps the backdrop consistent, it does not make
 * the result meaningful.
 *
 * **Pager pitfall.** Do not apply this modifier (with one shared backdrop) to every page of a
 * `HorizontalPager` or any other lazily composed container. The pages would compete for the
 * backdrop, so the blur could sample an off-screen neighbour, and pages entering or leaving
 * composition would keep handing ownership around. Record once, on the container that wraps the
 * whole pager, or give each page its own [rememberLayerBackdrop].
 *
 * @param backdrop The [LayerBackdrop] whose graphics layer records this composable's content.
 */
fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier = this then LayerBackdropElement(backdrop)

private class LayerBackdropElement(
    val backdrop: LayerBackdrop,
) : ModifierNodeElement<LayerBackdropNode>() {

    override fun create(): LayerBackdropNode = LayerBackdropNode(backdrop)

    override fun update(node: LayerBackdropNode) {
        node.switchBackdrop(backdrop)
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "layerBackdrop"
        properties["backdrop"] = backdrop
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LayerBackdropElement) return false
        return backdrop == other.backdrop
    }

    override fun hashCode(): Int = backdrop.hashCode()
}

/**
 * Records its content into [backdrop] while it is the backdrop's owner (see
 * [LayerBackdrop.attachNode]); a non-owner only draws its content and never touches the shared
 * layer or layer coordinates.
 */
internal class LayerBackdropNode(
    var backdrop: LayerBackdrop,
) : Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode {

    /** The latest attached coordinates of this node, kept so it can publish them when promoted to owner. */
    private var lastCoordinates: LayoutCoordinates? = null

    /** The latest coordinates of this node if they are still usable, `null` otherwise. */
    internal val attachedCoordinates: LayoutCoordinates?
        get() = lastCoordinates?.takeIf { it.isAttached }

    override fun onAttach() {
        backdrop.attachNode(this)
    }

    fun switchBackdrop(newBackdrop: LayerBackdrop) {
        if (backdrop === newBackdrop) return
        val old = backdrop
        backdrop = newBackdrop
        if (isAttached) {
            old.detachNode(this)
            newBackdrop.attachNode(this)
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val backdrop = backdrop
        if (backdrop.owner === this@LayerBackdropNode) {
            recordLayer(backdrop.graphicsLayer) { backdrop.onDraw(this@draw) }
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            lastCoordinates = coordinates
            if (backdrop.owner === this) {
                backdrop.layerCoordinates = coordinates
            }
        }
    }

    override fun onDetach() {
        lastCoordinates = null
        backdrop.detachNode(this)
    }
}
