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

    /** The short edge, in dp, ColorOS lays its own apps' windows out at. */
    val ShortEdgeDp = 360f

    /** Android's baseline density (`DENSITY_MEDIUM`): 160 dpi is 1dp = 1px. */
    private val BaselineDpi = 160f

    /**
     * The ColorOS system-app density for a window whose short edge is [shortEdgePx] pixels, capped
     * at [platformDensity].
     *
     * ColorOS's own apps render a 360dp short edge: on the 1272px-wide PLK110 (OnePlus, ColorOS
     * V17.0.0, 1272x2772) both `com.coloros.gallery3d` and `com.android.settings` run at 565 dpi =
     * 3.53125 px/dp, and a 1216px device at 540 dpi, against 620 dpi for the rest of the system.
     * The dpi is `round(shortEdgePx / 360 * 160)`, an integer as on Android, and the density is that
     * over 160.
     *
     * The cap exists because 360dp only describes a phone: deriving a density from a larger window
     * keeps growing with it (a desktop window yields 5.16x against a 2x screen), so tablets and
     * desktops get [platformDensity] while a phone keeps the system apps' rendering.
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
 * The density ColorOS lays its own apps out at: a 360dp short edge, capped at the platform density,
 * with the platform's `fontScale` kept.
 *
 * Every ColorOS system app — Gallery, Settings, the launcher — pins its window to a 360dp short
 * edge, so all of them ignore the display-size setting: on the reference 1272x2772 PLK110 the
 * system runs at 620 dpi while they stay at 565 dpi (3.53125 px/dp against 3.875), and their
 * controls measure the same pixels whatever density the user picks. The name comes from the first
 * app measured; the rule covers the whole ColorOS surface.
 *
 * **Default placement: the app root.** An app that should look like a system app pins everything,
 * so a toolbar button, a menu row, a tab and a search field all land on ColorOS's pixels together:
 *
 * ```kotlin
 * CompositionLocalProvider(LocalDensity provides rememberGalleryDensity()) {
 *     App()
 * }
 * ```
 *
 * Scope it around a single widget instead only when the host must keep following the system
 * density — embedding a ColorOS bottom bar into a Material-styled app, say. Anchored popups read
 * their own density, so they belong outside a narrow scope. With the pin in place a
 * [BottomFloatingNavigationBar]'s item pitch measures 207px, identical to the real Gallery's.
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
