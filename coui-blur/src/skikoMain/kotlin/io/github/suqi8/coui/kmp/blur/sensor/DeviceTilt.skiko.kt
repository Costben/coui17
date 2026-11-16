// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur.sensor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

@Composable
actual fun rememberDeviceTilt(smoothing: Float): State<DeviceTilt> = rememberPlatformDeviceTilt(smoothing)

@Composable
internal expect fun rememberPlatformDeviceTilt(smoothing: Float): State<DeviceTilt>
