// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur

/**
 * Registers a consumer that samples this backdrop without going through [drawBackdrop], so a
 * [LayerBackdrop] keeps recording its content. Every call must be paired with
 * [releaseBackdropSampling]. Backdrops that record nothing ignore it.
 */
@InternalCouiBlurApi
fun Backdrop.acquireBackdropSampling() {
    (this as? LayerBackdrop)?.acquireConsumer()
}

/** Releases a consumer registered with [acquireBackdropSampling]. */
@InternalCouiBlurApi
fun Backdrop.releaseBackdropSampling() {
    (this as? LayerBackdrop)?.releaseConsumer()
}

/**
 * Whether this backdrop currently has content to sample: a [LayerBackdrop] whose source node is
 * attached. Other backdrops always report `true`.
 */
@InternalCouiBlurApi
val Backdrop.isReadyForSampling: Boolean
    get() = (this as? LayerBackdrop)?.layerCoordinates?.isAttached ?: true
