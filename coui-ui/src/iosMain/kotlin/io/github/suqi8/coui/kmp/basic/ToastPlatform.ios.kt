// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import kotlinx.cinterop.useContents
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import kotlin.math.roundToInt

internal actual fun platformNeedsCompositionNavBarObserver(): Boolean = true

@Composable
internal actual fun rememberPlatformLockedNavigationBarInsetsPx(): () -> ToastSystemBarInsetsPx? {
    val density = LocalDensity.current
    return remember(density) {
        {
            val scenes = UIApplication.sharedApplication.connectedScenes.filterIsInstance<UIWindowScene>()
            val windowScene = scenes.firstOrNull {
                it.activationState == UISceneActivationStateForegroundActive
            } ?: scenes.firstOrNull()
            val windows = windowScene?.windows?.filterIsInstance<UIWindow>().orEmpty()
            val window = windows.firstOrNull { it.isKeyWindow() } ?: windows.firstOrNull()
            window?.safeAreaInsets?.useContents {
                ToastSystemBarInsetsPx(
                    left = (left * density.density).roundToInt().coerceAtLeast(0),
                    right = (right * density.density).roundToInt().coerceAtLeast(0),
                    bottom = (bottom * density.density).roundToInt().coerceAtLeast(0),
                )
            }
        }
    }
}
