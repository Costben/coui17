// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.COUIResponsiveUtils

/**
 * A responsive list-detail container with ColorOS style.
 *
 * It holds three panes — main, sub and emptyPage — plus a hairline divider, and lays them out by
 * window width: while the window is narrower than the compact boundary (600dp) it stacks the main
 * and detail containers at full width and hides the empty page, so the caller shows a single pane;
 * from 600dp on it places the main pane at a fraction of the width, a divider, and the
 * detail/empty-page pane in the rest.
 *
 * Fragments are an Android host concern, so the Compose port takes composable slots instead of
 * `FragmentContainerView`s. [mainContent] is the list pane, [detailContent] the detail pane, and
 * [emptyPageContent] the placeholder shown in the detail area while nothing is selected in split
 * mode. In compact mode only one of [mainContent] / [detailContent] is composed, chosen by
 * [detailVisible], which is what the device leaves to its host's fragment transactions.
 *
 * @param mainContent The list pane, laid out on the leading edge in split mode.
 * @param detailContent The detail pane.
 * @param detailVisible Whether the detail pane is selected. In compact mode it chooses between the
 *   two panes; in split mode it chooses between [detailContent] and [emptyPageContent].
 * @param modifier The modifier to be applied to the [ListDetailView].
 * @param emptyPageContent The placeholder shown in the detail area in split mode when
 *   [detailVisible] is false. The device's `mEmptyPageFragment` is hidden in compact mode.
 * @param mainFragmentPercent The main pane's share of the width, clamped by [minMainWidth] /
 *   [maxMainWidth]. The device default is `COUIListDetailView.DEFAULT_MAIN_WIDTH_PERCENT` (0.4f).
 * @param minMainWidth The floor of the main pane width (COUI `coui_main_fragment_min_width`, 280dp).
 * @param maxMainWidth The ceiling of the main pane width (COUI `coui_main_fragment_max_width`, 412dp).
 * @param dividerWidth The width of the hairline between the panes (COUI `coui_fragment_gap_width`,
 *   0.33000004dp).
 * @param dividerColor The divider color. The device reads `?attr/couiColorDivider`, whose
 *   `coui_color_divider` value is `#1f000000` in light and `#33ffffff` in dark.
 */
@Composable
fun ListDetailView(
    mainContent: @Composable () -> Unit,
    detailContent: @Composable () -> Unit,
    detailVisible: Boolean,
    modifier: Modifier = Modifier,
    emptyPageContent: @Composable () -> Unit = {},
    mainFragmentPercent: Float = ListDetailViewDefaults.MainFragmentPercent,
    minMainWidth: Dp = ListDetailViewDefaults.MinMainWidth,
    maxMainWidth: Dp = ListDetailViewDefaults.MaxMainWidth,
    dividerWidth: Dp = ListDetailViewDefaults.DividerWidth,
    dividerColor: Color = COUITheme.colorScheme.dividerLine,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = maxWidth
        val splitMode = ListDetailViewDefaults.isSplitMode(width)
        val mainWidth = if (splitMode) {
            (width * mainFragmentPercent).coerceIn(minMainWidth, maxMainWidth)
        } else {
            width
        }
        // Compact: the device measures both main and sub at the full width, hides the empty page and
        // lets its host pick the visible fragment; here [detailVisible] picks it. Split: main pane on
        // the leading edge, a hairline, then the detail / empty-page pane. Each slot is invoked once.
        Row(modifier = Modifier.fillMaxSize()) {
            if (splitMode || !detailVisible) {
                Box(
                    modifier = if (splitMode) {
                        Modifier.width(mainWidth).fillMaxHeight()
                    } else {
                        Modifier.weight(1f).fillMaxHeight()
                    },
                ) {
                    mainContent()
                }
            }
            if (splitMode) {
                Box(
                    modifier = Modifier
                        .width(dividerWidth)
                        .fillMaxHeight()
                        .background(dividerColor),
                )
            }
            if (splitMode || detailVisible) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (detailVisible) detailContent() else emptyPageContent()
                }
            }
        }
    }
}

/** Contains default values used by [ListDetailView]. */
object ListDetailViewDefaults {

    /**
     * Default share of the width given to the main pane
     * (`COUIListDetailView.DEFAULT_MAIN_WIDTH_PERCENT`, 0.4f).
     */
    val MainFragmentPercent = 0.4f

    /** Floor of the main pane width (COUI `coui_main_fragment_min_width`, 280dp). */
    val MinMainWidth = 280.dp

    /** Ceiling of the main pane width (COUI `coui_main_fragment_max_width`, 412dp). */
    val MaxMainWidth = 412.dp

    /** Width of the panes' hairline divider (COUI `coui_fragment_gap_width`, 0.33000004dp). */
    val DividerWidth = 0.33000004.dp

    /**
     * Width from which the view switches to the two-pane split, i.e. the width at which the COUI
     * `WindowTotalSizeClass` stops being `Compact`
     * (`Breakpoints.BP_MEDIUM_WINDOW_BASE_WIDTH`, 600dp). This is the same boundary the repo's
     * [COUIResponsiveUtils] width tiers use.
     */
    val SplitModeMinWidth = 600.dp

    /** Whether a window of [width] gets the two-pane split, porting `isInSplitMode`. */
    fun isSplitMode(width: Dp): Boolean = width >= SplitModeMinWidth
}
