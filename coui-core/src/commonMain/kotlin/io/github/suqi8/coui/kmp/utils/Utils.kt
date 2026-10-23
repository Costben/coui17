// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties

/**
 * Platform enum class.
 */
enum class Platform {
    Android,
    IOS,
    Desktop,
    WasmJs,
    MacOS,
    Js,
}

/**
 * Returns the current platform name.
 */
expect fun platform(): Platform

/**
 * Returns the rounded corner of the current device.
 */
@Composable
expect fun getRoundedCorner(): Dp

/**
 * Returns platform-specific DialogProperties.
 */
@Composable
expect fun platformDialogProperties(): DialogProperties

/**
 * Removes platform default dialog effects such as window animations and dimming.
 *
 * Platform implementations should clear default window animations and dim amount to ensure
 * COUI custom dialog animations and dim layer behave consistently across devices. Where the
 * dialog gets its own window that covers the system bars, it should also keep the host's system
 * bar icon appearance.
 */
@Composable
expect fun RemovePlatformDialogDefaultEffects()

/**
 * Lets touches reach the window beneath while the dialog is only playing its exit animation.
 *
 * A dialog window stays touch-modal for as long as it is mounted, so a popup that keeps its window
 * around to render the close animation keeps swallowing every drag until that animation ends.
 * Call this from inside the dialog content, with a lambda that turns true as soon as the popup has
 * been asked to dismiss, and the page underneath becomes draggable before the popup is gone.
 *
 * The lambda is observed rather than read in composition: the flag only has to change once, and
 * recomposing the closing menu to deliver it would restart the very animation it is meant to free.
 */
@Composable
expect fun DialogTouchPassThrough(passTouches: () -> Boolean)

/**
 * Whether the platform has a bug where clearing focus causes automatic re-assignment
 * to the first focusable view. This occurs on Android API levels 26-27.
 *
 * See: https://issuetracker.google.com/issues/433382598
 */
expect val hasFocusReassignBug: Boolean
