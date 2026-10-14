// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone

internal actual fun currentUtcOffsetSeconds(): Int = NSTimeZone.localTimeZone.secondsFromGMTForDate(NSDate()).toInt()
