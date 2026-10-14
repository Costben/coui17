// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import java.util.TimeZone

internal actual fun currentUtcOffsetSeconds(): Int = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1_000
