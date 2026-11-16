// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSThread
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.math.abs

private const val MAX_FLING_VELOCITY = 5000f

/**
 * Drives iOS wheel-picker ticks through UIKit impact generators, scaling intensity by scroll
 * velocity and keeping the Taptic Engine prepared across consecutive ticks.
 */
private class IosPickerHapticEngine {
    private val crispGenerator by lazy(LazyThreadSafetyMode.NONE) {
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleRigid)
    }
    private val softGenerator by lazy(LazyThreadSafetyMode.NONE) {
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleSoft)
    }

    fun perform(velocity: Float, level: CouiVibrateLevel) {
        if (NSThread.isMainThread) {
            performOnMain(velocity, level)
        } else {
            dispatch_async(dispatch_get_main_queue()) {
                performOnMain(velocity, level)
            }
        }
    }

    private fun performOnMain(velocity: Float, level: CouiVibrateLevel) {
        val normalized = (abs(velocity) / MAX_FLING_VELOCITY).coerceIn(0f, 1f).toDouble()
        val (generator, intensity) = when (level) {
            CouiVibrateLevel.Crisp -> crispGenerator to (0.50 + 0.50 * normalized)
            CouiVibrateLevel.Soft -> softGenerator to (0.35 + 0.40 * normalized)
        }
        generator.prepare()
        generator.impactOccurredWithIntensity(intensity)
        // Re-prepare immediately so rapid consecutive wheel steps fire with minimal latency.
        generator.prepare()
    }
}

@Composable
actual fun rememberCouiPickerHaptic(): (velocity: Float, level: CouiVibrateLevel) -> Unit {
    val engine = remember { IosPickerHapticEngine() }
    return remember(engine) {
        { velocity: Float, level: CouiVibrateLevel ->
            engine.perform(velocity, level)
        }
    }
}

// No audio asset off Android; the picker stays silent.
@Composable
actual fun rememberCouiPickerClick(): () -> Unit = remember { {} }
