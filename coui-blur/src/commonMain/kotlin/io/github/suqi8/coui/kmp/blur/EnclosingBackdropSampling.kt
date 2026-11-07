// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.platform.InspectorInfo

/**
 * Reports whether a [layerBackdrop] enclosing this node is sampled by a blur consumer, that is
 * whether this node's drawing is also being composited into a layer that something else blurs.
 *
 * [onSamplingChanged] runs on the UI thread from inside the consumer's own draw pass, so the frame
 * in which the drawing is first sampled has not reached the renderer yet: a platform view can still
 * change its state for that very frame. It is also called with `true` on attach when an enclosing
 * backdrop is already sampled. The enclosing backdrops are looked up once, on attach.
 *
 * @param onSamplingChanged Called with `true` when the first enclosing backdrop gains a consumer
 *   and with `false` when the last sampled one loses its final consumer.
 */
@InternalCouiBlurApi
fun Modifier.onEnclosingBackdropSampling(onSamplingChanged: (sampled: Boolean) -> Unit): Modifier = this then EnclosingBackdropSamplingElement(onSamplingChanged)

private class EnclosingBackdropSamplingElement(
    val onSamplingChanged: (Boolean) -> Unit,
) : ModifierNodeElement<EnclosingBackdropSamplingNode>() {

    override fun create(): EnclosingBackdropSamplingNode = EnclosingBackdropSamplingNode(onSamplingChanged)

    override fun update(node: EnclosingBackdropSamplingNode) {
        node.onSamplingChanged = onSamplingChanged
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "onEnclosingBackdropSampling"
    }

    override fun equals(other: Any?): Boolean = other is EnclosingBackdropSamplingElement && other.onSamplingChanged === onSamplingChanged

    override fun hashCode(): Int = onSamplingChanged.hashCode()
}

private class EnclosingBackdropSamplingNode(
    var onSamplingChanged: (Boolean) -> Unit,
) : Modifier.Node() {

    private var backdrops: List<LayerBackdrop> = emptyList()
    private var sampledCount = 0

    private val listener: (Boolean) -> Unit = { sampled ->
        val wasSampled = sampledCount > 0
        sampledCount = (sampledCount + if (sampled) 1 else -1).coerceAtLeast(0)
        val isSampled = sampledCount > 0
        if (isSampled != wasSampled) onSamplingChanged(isSampled)
    }

    override fun onAttach() {
        val found = ArrayList<LayerBackdrop>(1)
        traverseAncestors(LayerBackdropTraverseKey) { node ->
            val backdrop = (node as LayerBackdropNode).backdrop
            if (backdrop !in found) found.add(backdrop)
            true
        }
        backdrops = found
        sampledCount = found.count { it.hasConsumers }
        found.forEach { it.addSamplingListener(listener) }
        if (sampledCount > 0) onSamplingChanged(true)
    }

    override fun onDetach() {
        backdrops.forEach { it.removeSamplingListener(listener) }
        backdrops = emptyList()
        sampledCount = 0
    }
}
