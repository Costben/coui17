// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** Values and math behind [rememberGalleryDensity]. */
object GalleryDensityDefaults {

    /** The short edge, in dp, the Gallery lays out at on a device that can switch resolution. */
    val ShortEdgeDp = 360f

    /** Android's baseline density (`DENSITY_MEDIUM`): 160 dpi is 1dp = 1px. */
    private val BaselineDpi = 160f

    /**
     * The dpi the ColorOS 17 Gallery pins its resources to, whatever display size the user picked.
     *
     * This is `BaseActivity.adjustDensity` in `com.coloros.gallery3d`, which rewrites the
     * activity's configuration before any view is inflated:
     * - On a device that can switch the panel's resolution (the
     *   `oplus.software.display.resolution_switch_support` feature), the display's short edge is
     *   pinned to [ShortEdgeDp]: `(shortEdgePx / 360 * 160).toInt()`. The 1272x2772 PLK110 gets
     *   565 dpi = 3.53125 px/dp against the system's 620.
     * - On every other device it takes the panel's own density (`getInitialDisplayDensity`). The
     *   1240x2772 PKT110 gets its physical 560 dpi = 3.5 px/dp against the system's 620, where a
     *   360dp short edge would have given 551.
     *
     * @param physicalDpi the panel's own density in dpi.
     * @param shortEdgePx the display's short edge in pixels; only read when [resolutionSwitch] is
     *   set. A value of `0` or less falls back to [physicalDpi].
     * @param resolutionSwitch whether the device can switch the panel's resolution.
     */
    fun densityDpi(physicalDpi: Int, shortEdgePx: Int, resolutionSwitch: Boolean): Int {
        if (!resolutionSwitch || shortEdgePx <= 0) return physicalDpi
        return (shortEdgePx / ShortEdgeDp * BaselineDpi).toInt()
    }
}

/**
 * The density ColorOS 17's Gallery lays itself out at, with the platform's `fontScale` kept, or the
 * platform density on a platform without a display-size override to look past.
 *
 * Gallery ignores the display-size setting: it pins its resources to the dpi
 * [GalleryDensityDefaults.densityDpi] describes, so its toolbar keys, bottom bar and accessory balls
 * measure the same pixels at any density the user picks. Provide this density around that fixed-size
 * chrome and it lands on Gallery's pixels too: on the PKT110 a 56dp accessory ball measures 196px
 * and a 40dp toolbar key 140px, the same as Gallery's, instead of 217px and 155px at the system's
 * 620 dpi.
 *
 * ```kotlin
 * CompositionLocalProvider(LocalDensity provides rememberGalleryDensity()) {
 *     BottomFloatingNavigationBar { /* items */ }
 * }
 * ```
 *
 * Keep the layout around that chrome — its alignment, insets and outer margins — at the platform
 * density. Anchored popups convert to the panel density on their own, so they are safe either side
 * of the scope.
 */
@Composable
fun rememberGalleryDensity(): Density {
    val platformDensity = LocalDensity.current
    val galleryDpi = platformGalleryDensityDpi()
    return remember(platformDensity, galleryDpi) {
        if (galleryDpi == null || galleryDpi <= 0) {
            platformDensity
        } else {
            Density(density = galleryDpi / 160f, fontScale = platformDensity.fontScale)
        }
    }
}

/**
 * The dpi from [GalleryDensityDefaults.densityDpi] for the current display, or `null` on a platform
 * that has no display-size override to look past.
 */
@Composable
internal expect fun platformGalleryDensityDpi(): Int?
