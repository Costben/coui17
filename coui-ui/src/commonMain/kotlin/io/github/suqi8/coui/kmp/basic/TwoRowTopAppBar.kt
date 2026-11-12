// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop

/**
 * Default values for [TwoRowTopAppBar].
 *
 * All four numbers come from the same place: the Notes app (`便签`) on the reference device — the
 * page whose bar this component reproduces. It puts a search field on the first row and a row of
 * chips right below it, over one continuous sheet of glass, with a circle-and-label pair at the
 * start of the chip row.
 *
 * Measured at 3.5 px/dp: the search field's bottom edge sits 58px (16.6dp) above the chip row's top
 * edge, and the first chip is 124px (35.4dp) tall. COUI's own search field is
 * `CouiSearchBarDefaults.surfaceHeight` (40dp) and its chip tab is `ChipTabDefaults.Height` (32dp),
 * so the rows are taller than the fields inside them and the gaps are stated as the room left
 * around a row, not as field-to-field distances.
 */
object TwoRowTopAppBarDefaults {

    /**
     * The first row's height below the status bar: [TopAppBarDefaults.CollapsedHeight] (52dp).
     *
     * The same 52dp the ColorOS 17 Gallery toolbar and the Settings compact toolbar measure. A row
     * that holds a 40dp search field leaves 6dp above and below it.
     */
    val FirstRowHeight = TopAppBarDefaults.CollapsedHeight

    /**
     * The room between the first row's bottom edge and the chip row: 11dp.
     *
     * Notes leaves 16.6dp between the search field and the chips; the search field itself sits 6dp
     * above the first row's bottom edge (see [FirstRowHeight]), so the row-to-row gap is the
     * remaining 10.6dp, rounded to 11dp. Stating it as a gap between rows rather than as a distance
     * between the field and the chips keeps the two rows independent: a caller can give the first
     * row another height without moving the chips.
     */
    val SecondRowGap = 11.dp

    /**
     * The chip row's height: [ChipTabDefaults.Height] (32dp).
     *
     * Notes' first chip measures 124px (35.4dp); its extra 3.4dp is the chip's own touch slop, which
     * COUI applies outside the tab rather than inside the row.
     */
    val SecondRowHeight = ChipTabDefaults.Height

    /**
     * The room below the chip row, inside the bar: 6dp.
     *
     * The same 6dp the search field leaves inside the first row, so the chip row is inset from the
     * bar's bottom edge the way the field is from the row's. The material then keeps fading out for
     * [AppBarBlurOverlayDefaults.ExtraHeight] (8dp) past that edge, which is where the bar meets the
     * list.
     */
    val SecondRowBottom = 6.dp

    /**
     * The height the bar occupies below the status bar, as [TwoRowTopAppBar] lays it out.
     *
     * Use it for the inset a list has to reserve under the bar; measuring the bar instead would
     * couple the list's padding to a layout pass.
     *
     * @param firstRowHeight The height of the first row.
     * @param secondRowHeight The height of the chip row, or `null` for a first row only.
     */
    fun barHeight(
        firstRowHeight: Dp = FirstRowHeight,
        secondRowHeight: Dp? = null,
    ): Dp = firstRowHeight +
        if (secondRowHeight == null) {
            0.dp
        } else {
            SecondRowGap + secondRowHeight + SecondRowBottom
        }
}

/**
 * An app bar whose material spans one or two rows, the way the ColorOS 17 Notes bar does.
 *
 * The bar is **one** material node: [firstRow] and the optional [secondRow] are content drawn on
 * top of a single [AppBarBlurOverlay] whose gradient reaches the bar's own bottom edge. Nothing
 * inside the rows carries a material of its own, so a chip row cannot stack a second sheet of glass
 * — and a second ramp — under the first row. Controls that want their own glass — a
 * [ChipTabGroup]'s chips, a search field — sample the backdrop through
 * [LocalToolbarBackdropBlurFraction], which this bar drives, and so attenuate to
 * `TOOLBAR_NESTED_BLUR_SCALE` instead of compounding.
 *
 * The heights are pinned rather than measured: [firstRow] is [firstRowHeight] tall and [secondRow]
 * is [secondRowHeight] tall, both centred, so [TwoRowTopAppBarDefaults.barHeight] can predict the
 * bar without a layout pass. A second row is inset from the first by
 * [TwoRowTopAppBarDefaults.SecondRowGap] and from the bar's bottom edge by
 * [TwoRowTopAppBarDefaults.SecondRowBottom].
 *
 * The status bar inset is applied inside the material, so the glass reaches the top of the window.
 * The reported size is the status bar plus the rows; the material then reaches [extraHeight] past
 * that bottom edge without taking layout space, the way [AppBarBlurOverlay] does.
 *
 * @param modifier The modifier to be applied to the bar.
 * @param secondRow The chip row, or `null` for a first row only. Leave it null — do not pass an
 *   empty composable — when the caller has no second row: the height depends on whether it is
 *   present.
 * @param firstRowHeight The height of [firstRow] below the status bar.
 * @param secondRowHeight The height of [secondRow].
 * @param containerColor The base colour of the material, [AppBarBlurOverlayDefaults.containerColor]
 *   by default.
 * @param backdrop The content the material samples, [LocalBackdrop] by default.
 * @param extraHeight How far the material reaches past the bar's bottom edge.
 * @param scrollFraction The material's overall alpha, read in the draw phase. A bar that never
 *   fades out returns `1f`, which is the default.
 * @param platformRenderer Whether the platform's own backdrop blur may be used. See
 *   [AppBarBlurOverlay] for when a bar has to refuse it.
 * @param defaultWindowInsetsPadding Whether to inset the rows from the display cutout and the
 *   navigation bars on the horizontal sides, the way [PinnedTopAppBar] and [TopAppBar] do.
 * @param firstRow The first row's content, centred in a [firstRowHeight] tall row.
 */
@Composable
fun TwoRowTopAppBar(
    modifier: Modifier = Modifier,
    secondRow: (@Composable () -> Unit)? = null,
    firstRowHeight: Dp = TwoRowTopAppBarDefaults.FirstRowHeight,
    secondRowHeight: Dp = TwoRowTopAppBarDefaults.SecondRowHeight,
    containerColor: Color = AppBarBlurOverlayDefaults.containerColor(),
    backdrop: Backdrop? = LocalBackdrop.current,
    extraHeight: Dp = AppBarBlurOverlayDefaults.ExtraHeight,
    scrollFraction: () -> Float = { 1f },
    platformRenderer: Boolean = true,
    defaultWindowInsetsPadding: Boolean = true,
    firstRow: @Composable () -> Unit,
) {
    AppBarBlurOverlay(
        modifier = modifier,
        containerColor = containerColor,
        backdrop = backdrop,
        extraHeight = extraHeight,
        scrollFraction = scrollFraction,
        platformRenderer = platformRenderer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (defaultWindowInsetsPadding) {
                        Modifier
                            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
                    } else {
                        Modifier
                    },
                )
                .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(firstRowHeight),
                contentAlignment = Alignment.Center,
            ) {
                firstRow()
            }
            if (secondRow != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = TwoRowTopAppBarDefaults.SecondRowGap,
                            bottom = TwoRowTopAppBarDefaults.SecondRowBottom,
                        )
                        .height(secondRowHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    secondRow()
                }
            }
        }
    }
}
