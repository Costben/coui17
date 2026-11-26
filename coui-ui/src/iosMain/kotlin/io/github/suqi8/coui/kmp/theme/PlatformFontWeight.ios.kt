// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIAccessibilityBoldTextStatusDidChangeNotification
import platform.UIKit.UIAccessibilityIsBoldTextEnabled

// Shift up by 1 weight grade (Regular 400 -> Medium 500, Medium 500 -> SemiBold 600,
// SemiBold 600 -> Bold 700, Bold 700 -> Heavy 800) so PingFang SC retains visual hierarchy
// across Regular/Medium/Semibold and Bold titles do not bloat to Black (900).
private const val IOS_BOLD_TEXT_WEIGHT_DELTA = 100

@Composable
internal actual fun rememberPlatformFontWeightAdjustment(): Int {
    var boldEnabled by remember { mutableStateOf(UIAccessibilityIsBoldTextEnabled()) }
    DisposableEffect(Unit) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIAccessibilityBoldTextStatusDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            boldEnabled = UIAccessibilityIsBoldTextEnabled()
        }
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }
    return if (boldEnabled) IOS_BOLD_TEXT_WEIGHT_DELTA else 0
}
