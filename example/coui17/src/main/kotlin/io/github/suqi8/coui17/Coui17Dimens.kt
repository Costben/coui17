// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import androidx.compose.ui.unit.dp

/**
 * The ColorOS 17 dimensions this app is built to.
 *
 * Every number was read off the reference device — OnePlus PLK110, ColorOS V17.0.0, 1272x2772 px at
 * 620 dpi, so **1dp = 3.875px** — either from a `uiautomator` dump of the system Settings app or, for
 * the app-bar material, from the decompiled constants (see `docs/coui17-gallery-plan.md`).
 */
object Coui17Dimens {

    /** Gallery's measured activity density relative to the device-wide 620dpi density. */
    const val GALLERY_DENSITY_SCALE = 565f / 620f

    /**
     * The settings home's `app_bar_layout`: 343px from the top of the screen.
     *
     * It covers the title band and the search band together, so it already includes the status bar.
     */
    val HomeAppBarBottom = 88.52.dp

    /**
     * `homepage_appbar_blur_extra_height` (`res/values/dimens.xml:2812`): how far the material overlay
     * reaches past the bar's bottom edge. The device's `blur_overlay` measures 397px = 343px + 54px.
     */
    val AppBarBlurExtra = 14.dp

    /**
     * The search band under the title band: `app_bar_layout` minus the title band, 343px - 141px = 202px.
     */
    val HomeSearchBandHeight = 52.13.dp

    /** The empty title band above the search pill; it includes the status-bar-safe space. */
    val HomeTitleBandHeight = HomeAppBarBottom - HomeSearchBandHeight

    /** The search pill inside that band: 296.3dp wide, 40dp tall, 16dp in from each edge. */
    val SearchPillHeight = 40.dp

    /** The bar's horizontal padding: 16dp (the pill spans x 62..1210px). */
    val BarHorizontalPadding = 16.dp

    /**
     * The leading magnifier's slot in the collapsed search pill: 40dp.
     *
     * On the device the glyph spans x 112..180px and the label starts at x=218px, so the slot itself
     * ends at x=217px — 62px (the pill's start edge) + 155px = 40dp.
     */
    val SearchLeadingIconSlot = 40.dp

    /** Inset before the magnifier inside its slot, and the gap after it. */
    val SearchLeadingIconIndent = 10.dp

    val SearchLeadingIconEndGap = 7.dp

    /** A second-level page's toolbar: `toolbar` spans y 141..343px, so 202px tall. */
    val CompactToolbarHeight = 52.13.dp

    /** `coui_toolbar_back_view`: a 40dp square, 16dp from the start edge (x 62..217px). */
    val BackButtonSize = 40.dp

    /**
     * Where a second-level page's title starts: x=255px.
     *
     * The gap after the 40dp back key (which ends at x=217px) is 38px = 9.8dp.
     */
    val ToolbarTitleStart = 65.8.dp

    /** A compact section heading measured from the Settings second-level page. */
    val ListRowHeight = 32.dp

    /** Heading text inset inside the 16dp card-aligned heading container. */
    val ListTextIndent = 16.dp

    /** The measured gap from a section heading's text baseline box to its first card. */
    val ListHeadingContentGap = 3.5.dp

    /** How far a list row's card is inset from the screen edge: 62px (the card spans x 62..1210px). */
    val ListCardInset = 16.dp

    /** Shared demo cards already carry their own 16dp bottom spacing. */
    val ListGroupGap = 0.dp

    /**
     * How much room the bottom content must leave for the accessory bar.
     *
     * The Gallery-scaled 56dp capsule plus its lift and the app's navigation-bar inset.
     */
    val BottomBarSpace = 80.dp
}
