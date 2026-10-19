// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

/**
 * `ro.sf.lcd_density`. `Resources.getSystem()` and `DisplayMetrics` both follow the display-size
 * override a third-party app is subject to, so the panel's own property is the only source that
 * still reports what a ColorOS system app renders with.
 */
internal actual fun platformPhysicalDensityDpi(): Int? = runCatching {
    Class.forName("android.os.SystemProperties")
        .getMethod("get", String::class.java, String::class.java)
        .invoke(null, "ro.sf.lcd_density", "") as String
}.getOrNull()?.toIntOrNull()
