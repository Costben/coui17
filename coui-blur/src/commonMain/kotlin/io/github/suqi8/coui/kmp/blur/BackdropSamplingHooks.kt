// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur

/**
 * A [Backdrop] decorator that composites over [upstreamBackdrop] (for example, adding a surface
 * veil inside each consumer's local sampling pass without allocating a nested [LayerBackdrop]) and
 * forwards consumer registration and readiness queries to [upstreamBackdrop].
 */
@InternalCouiBlurApi
interface DelegatingBackdrop : Backdrop {
    val upstreamBackdrop: Backdrop
}

/**
 * Unwraps any chain of [DelegatingBackdrop] wrappers to the underlying [LayerBackdrop], if any.
 */
@InternalCouiBlurApi
fun Backdrop.unwrapLayerBackdrop(): LayerBackdrop? {
    var current: Backdrop? = this
    var hops = 0
    while (current != null && hops < 16) {
        if (current is LayerBackdrop) return current
        current = (current as? DelegatingBackdrop)?.upstreamBackdrop
        hops++
    }
    return null
}

/**
 * Registers a consumer that samples this backdrop without going through [drawBackdrop], so a
 * [LayerBackdrop] keeps recording its content. Every call must be paired with
 * [releaseBackdropSampling]. Backdrops that record nothing ignore it.
 */
@InternalCouiBlurApi
fun Backdrop.acquireBackdropSampling() {
    unwrapLayerBackdrop()?.acquireConsumer()
}

/** Releases a consumer registered with [acquireBackdropSampling]. */
@InternalCouiBlurApi
fun Backdrop.releaseBackdropSampling() {
    unwrapLayerBackdrop()?.releaseConsumer()
}

/**
 * Whether this backdrop currently has content to sample: a [LayerBackdrop] (directly or via
 * [DelegatingBackdrop]) whose source node is attached and has recorded at least once since
 * becoming active. Other backdrops always report `true`.
 */
@InternalCouiBlurApi
val Backdrop.isReadyForSampling: Boolean
    get() {
        val layer = unwrapLayerBackdrop()
        return if (layer != null) {
            layer.hasRecorded && layer.layerCoordinates?.isAttached == true
        } else {
            true
        }
    }
