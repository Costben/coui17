// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Draws the app bar material through the platform's own backdrop blur.
 *
 * ColorOS dresses `AppBarBlurOverlayView` once — a gradient background drawable plus a gradient-blur
 * backdrop RenderEffect that hwui evaluates on the RenderThread against whatever is already drawn
 * behind the view — and from then on only changes the view's alpha while the page scrolls. A
 * renderer reproduces exactly that, so nothing about the blur is re-recorded or rebuilt per frame.
 */
internal interface AppBarBackdropRenderer {
    /**
     * @param containerColor The base colour of the alpha ramp.
     * @param dark Whether to use the dark theme's two-stop ramp instead of the light three-stop one.
     * @param alpha The material's overall alpha; at `0` the view is `INVISIBLE`.
     * @param modifier The size and position of the material node.
     */
    @Composable
    fun Material(containerColor: Color, dark: Boolean, alpha: () -> Float, modifier: Modifier)
}

/** The platform backdrop renderer, or `null` where the platform has none and COUI blurs itself. */
@Composable
internal expect fun rememberPlatformAppBarBackdropRenderer(): AppBarBackdropRenderer?
