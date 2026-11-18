// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import kotlin.time.TimeSource

private val uptimeOrigin = TimeSource.Monotonic.markNow()

internal actual fun predictiveBackUptimeMillis(): Long = uptimeOrigin.elapsedNow().inWholeMilliseconds + 1L
