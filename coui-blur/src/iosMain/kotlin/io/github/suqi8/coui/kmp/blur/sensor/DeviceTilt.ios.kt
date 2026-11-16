// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur.sensor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import kotlin.math.asin
import kotlin.math.atan2

// Matches Android SENSOR_DELAY_GAME (~50-60 Hz) for smooth tilt-driven parallax highlights.
private const val DEVICE_MOTION_UPDATE_INTERVAL_SECONDS = 1.0 / 60.0

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberPlatformDeviceTilt(smoothing: Float): State<DeviceTilt> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val tilt = remember { mutableStateOf(DeviceTilt.Zero) }

    DisposableEffect(lifecycleOwner, smoothing) {
        val motionManager = CMMotionManager()
        if (!motionManager.deviceMotionAvailable) {
            return@DisposableEffect onDispose { }
        }

        motionManager.deviceMotionUpdateInterval = DEVICE_MOTION_UPDATE_INTERVAL_SECONDS
        val alpha = smoothing.coerceIn(0f, 1f)
        var smoothPitch = 0f
        var smoothRoll = 0f
        var smoothGravityX = 0f
        var smoothGravityY = 0f
        var initialized = false
        var active = false

        fun startUpdates() {
            if (active) return
            active = true
            motionManager.startDeviceMotionUpdatesToQueue(NSOperationQueue.mainQueue) { motion, error ->
                if (!active || error != null || motion == null) {
                    return@startDeviceMotionUpdatesToQueue
                }
                var gravityX = 0f
                var gravityY = 0f
                var gravityZ = -1f
                motion.gravity.useContents {
                    gravityX = x.toFloat()
                    gravityY = y.toFloat()
                    gravityZ = z.toFloat()
                }
                // Derive pitch/roll from fused gravity so sign conventions match Android's
                // SensorManager.getOrientation(R, _) across all device orientations.
                val pitch = asin(gravityY.coerceIn(-1f, 1f))
                val roll = if (gravityX == 0f && gravityZ == 0f) 0f else atan2(gravityX, -gravityZ)
                if (!initialized) {
                    smoothPitch = pitch
                    smoothRoll = roll
                    smoothGravityX = gravityX
                    smoothGravityY = gravityY
                    initialized = true
                } else {
                    smoothPitch += (pitch - smoothPitch) * alpha
                    smoothRoll += (roll - smoothRoll) * alpha
                    smoothGravityX += (gravityX - smoothGravityX) * alpha
                    smoothGravityY += (gravityY - smoothGravityY) * alpha
                }
                tilt.value = DeviceTilt(
                    pitch = smoothPitch,
                    roll = smoothRoll,
                    gravityX = smoothGravityX,
                    gravityY = smoothGravityY,
                )
            }
        }

        fun stopUpdates() {
            if (!active) return
            motionManager.stopDeviceMotionUpdates()
            active = false
            initialized = false
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> startUpdates()
                Lifecycle.Event.ON_STOP -> stopUpdates()
                else -> Unit
            }
        }

        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
            stopUpdates()
        }
    }

    return tilt
}
