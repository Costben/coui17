// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

// Fallback granular tick used when the vendor linear-motor API is unavailable.
private const val OPLUS_HAPTIC_STRENGTH = 308
private const val OPLUS_HAPTIC_SWITCH = 302
private const val AOSP_CLOCK_TICK = 4
private const val AOSP_CONTEXT_CLICK = 6

// The wheel velocity above which the vendor strength call asks for the sharper waveform.
private const val SHARP_WAVEFORM_VELOCITY = 2000f
private const val MAX_FLING_VELOCITY = 5000
private const val LINEAR_MOTOR_LOW_VELOCITY = 1200
private const val LINEAR_MOTOR_HIGH_VELOCITY = 1600

/**
 * Mirrors `COUINumberPicker.performAdaptiveFeedback()`: asks `VibrateUtils` to scale the linear-motor
 * waveform by the current scroll velocity. The vendor classes ship with ColorOS apps, not with this
 * library, so every lookup is optional and a missing API simply returns `false`.
 */
private object LinearMotorStrength {
    private val vibrateUtils: Class<*>? =
        runCatching { Class.forName("com.coui.appcompat.vibrateutil.VibrateUtils") }.getOrNull()

    private val getVibrator = vibrateUtils?.let { type ->
        runCatching { type.getMethod("getLinearMotorVibrator", Context::class.java) }.getOrNull()
    }

    private val setStrength = vibrateUtils?.let { type ->
        runCatching {
            val vibratorClass = Class.forName("com.oplus.os.LinearmotorVibrator")
            type.getMethod(
                "setLinearMotorVibratorStrength",
                vibratorClass,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
            )
        }.getOrNull()
    }

    private var vibrator: Any? = null
    private var resolved = false

    fun perform(context: Context, velocity: Float, level: CouiVibrateLevel): Boolean {
        val setter = setStrength ?: return false
        val getter = getVibrator ?: return false
        if (!resolved) {
            resolved = true
            vibrator = runCatching { getter.invoke(null, context) }.getOrNull()
        }
        val target = vibrator ?: return false
        val speed = velocity.toInt()
        val waveform = if (velocity > SHARP_WAVEFORM_VELOCITY) 0 else 1
        return runCatching {
            setter.invoke(
                null,
                target,
                waveform,
                speed,
                MAX_FLING_VELOCITY,
                LINEAR_MOTOR_LOW_VELOCITY,
                LINEAR_MOTOR_HIGH_VELOCITY,
                if (level == CouiVibrateLevel.Crisp) 0 else 1,
                1.0f,
            )
            true
        }.getOrDefault(false)
    }
}

@Composable
actual fun rememberCouiPickerHaptic(): (velocity: Float, level: CouiVibrateLevel) -> Unit {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view, context) {
        { velocity: Float, level: CouiVibrateLevel ->
            // COUINumberPicker.performFeedback(): adaptive strength, then 308, then 302.
            when {
                LinearMotorStrength.perform(context, velocity, level) -> Unit

                view.performHapticFeedback(OPLUS_HAPTIC_STRENGTH) -> Unit

                view.performHapticFeedback(OPLUS_HAPTIC_SWITCH) -> Unit

                // Neither granular constant is known: plain AOSP build, use the closest system tick.
                !view.performHapticFeedback(AOSP_CLOCK_TICK) -> view.performHapticFeedback(AOSP_CONTEXT_CLICK)

                else -> Unit
            }
        }
    }
}

@Composable
actual fun rememberCouiPickerClick(): () -> Unit {
    val context = LocalContext.current
    val pool = remember(context) {
        SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
    }
    // The click asset belongs to ColorOS; look it up in the host app's resources by name.
    val soundId = remember(context, pool) {
        val resId = context.resources.getIdentifier(
            "coui_numberpicker_click",
            "raw",
            context.packageName,
        )
        if (resId == 0) 0 else pool.load(context, resId, 1)
    }
    DisposableEffect(pool) {
        onDispose { pool.release() }
    }
    return remember(pool, soundId) {
        {
            if (soundId != 0) {
                pool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
            }
        }
    }
}
