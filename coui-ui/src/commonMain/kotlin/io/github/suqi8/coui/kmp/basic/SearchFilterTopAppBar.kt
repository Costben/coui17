// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.LayerBackdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The Notes two-row bar specialised for a search page: the search field on the first row, the
 * filters for what is being searched on the second.
 *
 * This is the pattern, not another generic bar. It exists because the two rows must be stacked in a
 * particular order to look right, and the order is not what a caller would guess:
 *
 * 1. The material is recorded into its own [LayerBackdrop] first.
 * 2. Both rows are drawn *above* that node, outside the recording.
 * 3. The filter row — and anything glass-like inside the search row, through the provided
 *    [LocalBackdrop] — samples the recording, not the page.
 *
 * Skipping step 1 is the trap. If the chips sample the page directly, they see the raw list: a
 * black video cover passes under them at full contrast, while the bar two pixels away has already
 * been blurred and lifted by the 80% container colour. The chips then read darker than their own
 * surroundings, and the unselected wash — roughly 8% ColorDodge plus 8% Luminosity — is nowhere
 * near enough to hide it. Sampling the recording instead puts the chips on the same sheet the bar
 * is made of, which is what the Notes app does. Drawing the rows inside the recorded node is the
 * other trap: the chips would be recorded into the layer they sample, feeding themselves back.
 *
 * The search row's [LocalBackdrop] follows the material: while the bar is visible it is the
 * recording, so a search capsule's own glass samples the same sheet; while the bar is invisible
 * (a page that fades it in with [scrollFraction]) it is the page [backdrop] again, so a collapsed
 * search button is not left sampling a flat sheet that is not on screen.
 *
 * Compared with [TwoRowTopAppBar], which takes two arbitrary slots and paints both inside the
 * material, this component fixes the roles of the rows and owns the recording. Use it for a search
 * page; use [TwoRowTopAppBar] when the second row is not a filter strip.
 *
 * The reported height is the status bar plus the rows: [firstRowHeight], and when [filters] is
 * present, [rowGap] + [filterRowHeight] + [TwoRowTopAppBarDefaults.SecondRowBottom]. The material's
 * [extraHeight] fade band draws past that edge without taking layout space, exactly as
 * [AppBarBlurOverlay] does. Reserve the list's top inset with that same sum rather than measuring
 * the bar.
 *
 * @param filters The filter chips of the second row, or `null` for a bar with a search row only —
 *   a search page shows no filters until it has something to filter. An empty list is a filter row
 *   with no chips, not the same thing: the row's height still applies.
 * @param selectedFilter The index of the selected chip.
 * @param onFilterSelected Called with the index of a tapped chip.
 * @param modifier The modifier to be applied to the bar.
 * @param searchRow The search bar of the first row, centred in [firstRowHeight]. A
 *   [TopAppBarSearchBar] with `expanded = true` is the usual choice.
 * @param firstRowHeight The height of the search row below the status bar.
 * @param filterRowHeight The height of the filter row.
 * @param rowGap The room between the search row's bottom edge and the filter row.
 * @param leadingIcon An optional icon-only chip pinned before the filters — the sort menu of a
 *   search page. A popup it opens has to be hosted by the caller; this bar only reports the tap.
 * @param onLeadingIconClick Called when the leading chip is tapped.
 * @param filterContentPadding The padding inside the filter row, before the first chip and after
 *   the last.
 * @param containerColor The base colour of the material.
 * @param backdrop What the material samples — the page behind the bar. The filters never sample it
 *   directly; see the class-level note.
 * @param extraHeight How far the material reaches past the bar's bottom edge. Defaults to
 *   [TwoRowTopAppBarDefaults.ExtraHeight] (6dp) when [filters] is non-null — stopping 12dp below
 *   the chips so resting content below is untouched while scrolled items enter a smooth ramp — and
 *   to [AppBarBlurOverlayDefaults.ExtraHeight] (8dp) for the single-row state.
 * @param scrollFraction The material's overall alpha, read in the draw phase. A bar that is always
 *   visible returns `1f`.
 * @param platformRenderer Whether the platform's own backdrop blur may be used. Defaults to `false`
 *   because the bar records its own material: the platform effect samples the whole window and
 *   would feed the recording back into itself.
 */
@Composable
fun SearchFilterTopAppBar(
    filters: List<ChipTabData>? = null,
    selectedFilter: Int = 0,
    onFilterSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    firstRowHeight: Dp = TwoRowTopAppBarDefaults.FirstRowHeight,
    filterRowHeight: Dp = TwoRowTopAppBarDefaults.SecondRowHeight,
    rowGap: Dp = TwoRowTopAppBarDefaults.SecondRowGap,
    leadingIcon: ImageVector? = null,
    onLeadingIconClick: () -> Unit = {},
    filterContentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    containerColor: Color = AppBarBlurOverlayDefaults.containerColor(),
    backdrop: Backdrop? = LocalBackdrop.current,
    extraHeight: Dp = if (filters != null) {
        TwoRowTopAppBarDefaults.ExtraHeight
    } else {
        AppBarBlurOverlayDefaults.ExtraHeight
    },
    scrollFraction: () -> Float = { 1f },
    platformRenderer: Boolean = false,
    searchRow: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val statusBarTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    // Mirrors the rows below row for row, with this bar's own [rowGap] rather than
    // [TwoRowTopAppBarDefaults.barHeight]'s fixed SecondRowGap: a caller that moves the gap must
    // move the material with it, or the fade band stops short of the bar's bottom edge.
    val rowsHeight = firstRowHeight + if (filters != null) {
        rowGap + filterRowHeight + TwoRowTopAppBarDefaults.SecondRowBottom
    } else {
        0.dp
    }
    val barHeight = statusBarTop + rowsHeight
    val pageColor = COUITheme.colorScheme.background
    // While the bar is still fading in, the recorded material is partly transparent. Paving the
    // layer with the page colour keeps anything sampling it composited on an opaque picture instead
    // of reading a translucent layer and picking up a grey edge.
    val upstream = backdrop as? LayerBackdrop
    val materialBackdrop = if (upstream != null) {
        rememberLayerBackdrop(upstream = upstream) {
            if (scrollFraction() < 0.999f) drawRect(pageColor)
            drawContent()
        }
    } else {
        rememberLayerBackdrop {
            if (scrollFraction() < 0.999f) drawRect(pageColor)
            drawContent()
        }
    }
    // Which backdrop the search row samples, as a boolean so the fade does not recompose the bar
    // on every frame; only the crossing into and out of visibility does.
    val currentScrollFraction = rememberUpdatedState(scrollFraction)
    val materialVisible by remember {
        derivedStateOf { currentScrollFraction.value() > 0.001f }
    }
    val searchRowBackdrop = if (materialVisible) materialBackdrop else backdrop
    val materialRamp = if (filters != null) {
        twoRowAppBarMaterialRamp(
            statusBarTop = statusBarTop,
            firstRowHeight = firstRowHeight,
            rowGap = rowGap,
            totalHeight = barHeight + extraHeight,
        )
    } else {
        AppBarMaterialRamp()
    }
    CompositionLocalProvider(LocalEdgeToEdge provides true) {
        Layout(
            modifier = modifier,
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight + extraHeight)
                        .layerBackdrop(materialBackdrop),
                ) {
                    AppBarMaterialLayout(
                        materialEnabled = true,
                        modifier = Modifier.fillMaxWidth().height(barHeight),
                        containerColor = containerColor,
                        backdrop = backdrop,
                        extraHeight = extraHeight,
                        scrollFraction = scrollFraction,
                        platformRenderer = platformRenderer,
                        materialRamp = materialRamp,
                    ) {}
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
                        .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(firstRowHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        CompositionLocalProvider(LocalBackdrop provides searchRowBackdrop) {
                            searchRow()
                        }
                    }
                    if (filters != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = rowGap,
                                    bottom = TwoRowTopAppBarDefaults.SecondRowBottom,
                                )
                                .height(filterRowHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            ChipTabGroup(
                                tabs = filters,
                                selectedIndex = selectedFilter,
                                onSelectedChange = onFilterSelected,
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = leadingIcon,
                                onLeadingIconClick = onLeadingIconClick,
                                contentPadding = filterContentPadding,
                                backdrop = materialBackdrop,
                            )
                        }
                    }
                }
            },
        ) { measurables, constraints ->
            // The material node is taller than the bar by the fade band, so it has to be measured
            // without the bar's height cap; the rows decide the size this component reports.
            val material = measurables[0].measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
            val rows = measurables[1].measure(constraints.copy(minHeight = 0))
            layout(rows.width, rows.height) {
                material.placeRelative(0, 0)
                rows.placeRelative(0, 0)
            }
        }
    }
}
