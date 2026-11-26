// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

/**
 * Whether the current platform can present a three-button navigation bar whose bottom
 * [androidx.compose.foundation.layout.WindowInsets.tappableElement] inset is distinct from a
 * gesture-handle safe area.
 *
 * On iOS/Skiko, `WindowInsets.tappableElement` mirrors the home-indicator safe area (34pt on
 * Face ID iPhones) even though there is no three-button navigation bar; treating it as
 * three-button navigation would lift floating bars to 36dp instead of 8dp.
 */
internal expect val platformSupportsButtonNavigationBar: Boolean
