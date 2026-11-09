// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop

/**
 * Captures the window this composition is drawn in, for a surface about to open in a window of its
 * own.
 *
 * A `Dialog` window has no page underneath it to sample, so a glass panel inside it would frost
 * nothing and show only its tint. While [active], this captures the host window once and exposes it
 * as a backdrop; [ProvideHostWindowBackdrop] then hands it to the dialog's content, which runs the
 * same blur and tint as an in-window panel. Must be called in the host composition, outside the
 * `Dialog`, so it captures the page rather than the dialog.
 *
 * The value is `null` until the capture lands, when not [active], and on platforms whose dialogs
 * draw inside the host window and can sample it directly.
 */
@Composable
internal expect fun rememberHostWindowBackdrop(active: Boolean): State<Backdrop?>

/**
 * Publishes [hostBackdrop] as [LocalBackdrop] for [content], keeping the inherited backdrop while
 * the capture is not available.
 */
@Composable
internal fun ProvideHostWindowBackdrop(hostBackdrop: Backdrop?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBackdrop provides (hostBackdrop ?: LocalBackdrop.current)) {
        content()
    }
}
