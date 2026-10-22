// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import kotlin.math.round

/** Values and math behind [rememberGalleryDensity]. */
object GalleryDensityDefaults {

    /** The short edge, in dp, the ColorOS 17 Gallery lays its window out at. */
    val ShortEdgeDp = 360f

    /** Android's baseline density (`DENSITY_MEDIUM`): 160 dpi is 1dp = 1px. */
    private val BaselineDpi = 160f

    /**
     * The Gallery's density for a window whose short edge is [shortEdgePx] pixels, capped at
     * [platformDensity].
     *
     * `com.coloros.gallery3d` renders a 360dp short edge: the 1272px-wide PLK110 (OnePlus, ColorOS
     * V17.0.0, 1272x2772) runs it at 565 dpi = 3.53125 px/dp, and a 1216px device at 540 dpi, against
     * 620 dpi for the rest of the system. The dpi is `round(shortEdgePx / 360 * 160)`, an integer as
     * on Android, and the density is that over 160.
     *
     * The cap exists because 360dp only describes a phone: deriving a density from a larger window
     * keeps growing with it (a desktop window yields 5.16x against a 2x screen), so tablets and
     * desktops get [platformDensity] while a phone keeps the Gallery's slightly denser rendering.
     * An unknown window (a short edge of `0` or less, before the first layout) also yields
     * [platformDensity].
     */
    fun density(shortEdgePx: Int, platformDensity: Float): Float {
        if (shortEdgePx <= 0) return platformDensity
        val dpi = round(shortEdgePx / ShortEdgeDp * BaselineDpi).coerceAtLeast(BaselineDpi)
        return minOf(dpi / BaselineDpi, platformDensity)
    }
}

/**
 * The density the ColorOS 17 Gallery renders at: its 360dp short edge, capped at the platform
 * density, with the platform's `fontScale` kept.
 *
 * Gallery is laid out at a denser setting than the rest of the system (565 dpi against 620 dpi on
 * the reference 1272x2772 PLK110, so 3.53125 px/dp against 3.875). Provide this through
 * [LocalDensity] around a bottom bar or other Gallery-measured chrome to land on its pixels; with it,
 * a [BottomFloatingNavigationBar]'s item pitch measures 207px, identical to the real Gallery's.
 * Content that should follow the system density, such as an anchored popup, belongs outside the
 * scope:
 *
 * ```kotlin
 * CompositionLocalProvider(LocalDensity provides rememberGalleryDensity()) {
 *     BottomFloatingNavigationBar { ... }
 * }
 * ```
 *
 * See [GalleryDensityDefaults.density] for the math and why it is capped.
 */
@Composable
fun rememberGalleryDensity(): Density {
    val platformDensity = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    return remember(platformDensity, windowSize) {
        Density(
            density = GalleryDensityDefaults.density(
                shortEdgePx = minOf(windowSize.width, windowSize.height),
                platformDensity = platformDensity.density,
            ),
            fontScale = platformDensity.fontScale,
        )
    }
}
