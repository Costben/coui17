// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package appstate

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import i18n.AppLanguage
import navigation3.Navigator

@Stable
data class AppState(
    // Language
    val language: AppLanguage = AppLanguage.Chinese,
    // Theme
    val colorMode: Int = 0,
    val seedIndex: Int = 0,
    val paletteStyle: Int = 0,
    val colorSpec: Int = 0,
    // UI
    val showFPSMonitor: Boolean = false,
    val showTopAppBar: Boolean = true,
    val enablePageUserScroll: Boolean = true,
    val enableScrollEndHaptic: Boolean = true,
    val enableCornerClip: Boolean = true,
    val enableDim: Boolean = true,
    val blockInputDuringTransition: Boolean = true,
    val popDirectionFollowsSwipeEdge: Boolean = false,
    // Squircle
    val enableSquircle: Boolean = true,
    // Blur
    val enableBlur: Boolean = true,
)

val LocalAppState = compositionLocalOf<AppState> {
    error("No AppState provided!")
}

val LocalUpdateAppState = staticCompositionLocalOf<((AppState) -> AppState) -> Unit> {
    error("No AppState updater provided!")
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No navigator found!") }
val LocalIsWideScreen = staticCompositionLocalOf { false }
