// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import io.github.suqi8.coui.kmp.layout.platformPhysicalDensityDpi

/** The ColorOS feature Gallery checks before pinning a 360dp short edge. */
private const val RESOLUTION_SWITCH_FEATURE = "oplus.software.display.resolution_switch_support"

/**
 * Keyed on the configuration so a resolution switch, which changes the display's pixels and density
 * together, is picked up.
 */
@Composable
internal actual fun platformGalleryDensityDpi(): Int? {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) { galleryDensityDpi(context) }
}

private fun galleryDensityDpi(context: Context): Int? {
    val physicalDpi = platformPhysicalDensityDpi() ?: return null
    val resolutionSwitch = hasOplusFeature(RESOLUTION_SWITCH_FEATURE)
    val shortEdgePx = if (resolutionSwitch) displayShortEdgePx(context) else 0
    return GalleryDensityDefaults.densityDpi(physicalDpi, shortEdgePx, resolutionSwitch)
}

/**
 * The short edge of the whole display, not of this window: Gallery reads the default display's real
 * metrics, so a split-screen window keeps the full-screen density.
 */
@Suppress("DEPRECATION")
private fun displayShortEdgePx(context: Context): Int {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return 0
    val metrics = DisplayMetrics()
    windowManager.defaultDisplay.getRealMetrics(metrics)
    return minOf(metrics.widthPixels, metrics.heightPixels)
}

/** `OplusFeatureConfigManager.hasFeature`, or `false` outside ColorOS. */
private fun hasOplusFeature(name: String): Boolean = runCatching {
    val managerClass = Class.forName("com.oplus.content.OplusFeatureConfigManager")
    val manager = managerClass.getMethod("getInstance").invoke(null)
    managerClass.getMethod("hasFeature", String::class.java).invoke(manager, name) as Boolean
}.getOrDefault(false)
