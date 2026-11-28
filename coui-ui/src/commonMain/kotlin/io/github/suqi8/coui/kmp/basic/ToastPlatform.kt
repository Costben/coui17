// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable

/**
 * Pixel navigation-bar insets resolved for [ToastHost] positioning.
 */
internal data class ToastSystemBarInsetsPx(
    val left: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
) {
    val isNonZero: Boolean
        get() = left > 0 || right > 0 || bottom > 0

    fun maxWith(other: ToastSystemBarInsetsPx): ToastSystemBarInsetsPx = ToastSystemBarInsetsPx(
        left = maxOf(left, other.left),
        right = maxOf(right, other.right),
        bottom = maxOf(bottom, other.bottom),
    )
}

/**
 * Whether [ToastHost] should keep a lightweight navigation-bar-only observer mounted while idle so
 * visible navigation-bar insets are recorded before entering an immersive screen.
 *
 * Android queries `getInsetsIgnoringVisibility` / `stableInset*` directly on demand, so it needs
 * no idle inset observer at all.
 */
internal expect fun platformNeedsCompositionNavBarObserver(): Boolean

/**
 * Returns an on-demand reader for the platform's visibility-independent navigation-bar insets in
 * pixels, or `null` when unavailable on the current platform/window.
 */
@Composable
internal expect fun rememberPlatformLockedNavigationBarInsetsPx(): () -> ToastSystemBarInsetsPx?
