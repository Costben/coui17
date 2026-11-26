// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur.internal

/**
 * Whether [io.github.suqi8.coui.kmp.blur.LayerBackdropNode] can record default content once into
 * its [androidx.compose.ui.graphics.layer.GraphicsLayer] and replay that layer to the screen,
 * avoiding a second full Compose tree draw pass on every frame.
 *
 * Enabled on Apple platforms (iOS/macOS) where Android ColorOS window-blur interop views do not
 * exist and recording a Skia display list once halves per-frame draw overhead for blurred screens.
 */
internal expect val singlePassLayerBackdrop: Boolean
