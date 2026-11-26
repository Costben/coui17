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

private const val IOS_BOLD_TEXT_WEIGHT_DELTA = 200

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
