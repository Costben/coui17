// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSThread
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Caches UIKit feedback generators so repeated haptic calls reuse warm Taptic Engine handles
 * without reallocating generators per event.
 */
private class IosHapticEngine {
    private val lightImpact by lazy(LazyThreadSafetyMode.NONE) {
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    }
    private val mediumImpact by lazy(LazyThreadSafetyMode.NONE) {
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    }
    private val heavyImpact by lazy(LazyThreadSafetyMode.NONE) {
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
    }
    private val softImpact by lazy(LazyThreadSafetyMode.NONE) {
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleSoft)
    }
    private val rigidImpact by lazy(LazyThreadSafetyMode.NONE) {
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleRigid)
    }
    private val selection by lazy(LazyThreadSafetyMode.NONE) {
        UISelectionFeedbackGenerator()
    }
    private val notification by lazy(LazyThreadSafetyMode.NONE) {
        UINotificationFeedbackGenerator()
    }

    fun perform(effect: CouiHapticEffect) {
        if (NSThread.isMainThread) {
            performOnMain(effect)
        } else {
            dispatch_async(dispatch_get_main_queue()) {
                performOnMain(effect)
            }
        }
    }

    private fun performOnMain(effect: CouiHapticEffect) {
        when (effect) {
            CouiHapticEffect.Switch -> triggerImpact(lightImpact)

            CouiHapticEffect.Strength -> triggerImpact(rigidImpact, intensity = 0.75)

            CouiHapticEffect.Long -> triggerImpact(heavyImpact)

            CouiHapticEffect.Keyboard -> triggerImpact(lightImpact, intensity = 0.65)

            CouiHapticEffect.Short -> triggerImpact(softImpact)

            CouiHapticEffect.ShortTriple -> {
                notification.prepare()
                notification.notificationOccurred(
                    UINotificationFeedbackType.UINotificationFeedbackTypeError,
                )
            }

            CouiHapticEffect.Stepping -> {
                selection.prepare()
                selection.selectionChanged()
            }

            CouiHapticEffect.EdgeShort -> triggerImpact(softImpact, intensity = 0.8)

            CouiHapticEffect.EdgeList -> triggerImpact(mediumImpact)

            CouiHapticEffect.EdgeSqueeze -> triggerImpact(heavyImpact)
        }
    }

    private fun triggerImpact(generator: UIImpactFeedbackGenerator, intensity: Double? = null) {
        generator.prepare()
        if (intensity != null) {
            generator.impactOccurredWithIntensity(intensity)
        } else {
            generator.impactOccurred()
        }
    }
}

@Composable
actual fun rememberCouiHaptic(): (CouiHapticEffect) -> Unit {
    val engine = remember { IosHapticEngine() }
    return remember(engine) {
        { effect: CouiHapticEffect ->
            engine.perform(effect)
        }
    }
}
