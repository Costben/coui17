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

    /**
     * The settings home's `app_bar_layout`: 343px from the top of the screen.
     *
     * It covers the title band and the search band together, so it already includes the status bar.
     */
    val HomeAppBarBottom = 88.52.dp

    /** Expanded page header height including the status-bar inset. */
    val ExpandedHeaderHeight = 144.dp

    /** The compact toolbar height excluding the status-bar inset. */
    val CollapsedHeaderHeight = 52.dp

    /** Distance over which a large title transitions into the compact toolbar. */
    val HeaderCollapseRange = ExpandedHeaderHeight - CollapsedHeaderHeight

    /**
     * `homepage_appbar_blur_extra_height` (`res/values/dimens.xml:2812`): how far the material overlay
     * reaches past the bar's bottom edge. The device's `blur_overlay` measures 397px = 343px + 54px.
     */
    val AppBarBlurExtra = 14.dp

    /**
     * The search band under the title band: `app_bar_layout` minus the title band, 343px - 141px = 202px.
     */
    val HomeSearchBandHeight = 52.13.dp

    /** The search pill inside that band: 296.3dp wide, 40dp tall, 16dp in from each edge. */
    val SearchPillHeight = 40.dp

    /** The bar's horizontal padding: 16dp (the pill spans x 62..1210px). */
    val BarHorizontalPadding = 16.dp

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

    /**
     * A settings list row: 202px.
     *
     * The device has three heights — 202px for a one-line row, 205px for the first row under the bar,
     * and 209px for rows with a taller subtitle. The common case is used here, and no dividers: the
     * device's home list has none, only a 62px empty band between groups.
     */
    val ListRowHeight = 52.13.dp

    /** Where a list row's text starts: x=279px. */
    val ListTextIndent = 72.dp

    /** How far a list row's card is inset from the screen edge: 62px (the card spans x 62..1210px). */
    val ListCardInset = 16.dp

    /** The empty band between two list groups: 62px. */
    val ListGroupGap = 16.dp

    /**
     * How much room the bottom content must leave for the accessory bar.
     *
     * The bar's own 56dp capsule plus the 8dp lift COUI leaves above the navigation bar and the app's
     * 24dp bottom gap.
     */
    val BottomBarSpace = 88.dp
}
