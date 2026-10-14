// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

// Date#getTimezoneOffset counts minutes behind UTC (positive west of Greenwich), hence the sign.
internal actual fun currentUtcOffsetSeconds(): Int = js("-(new Date().getTimezoneOffset()) * 60")
