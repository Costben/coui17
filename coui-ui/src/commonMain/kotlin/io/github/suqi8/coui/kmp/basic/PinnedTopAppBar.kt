// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.theme.COUITheme

/** Default values for [PinnedTopAppBar]. */
object PinnedTopAppBarDefaults {

    /**
     * The bar's height below the status bar.
     *
     * Reuses [TopAppBarDefaults.CollapsedHeight] (COUI `coui_appbar_title_toolbar_height`, 52dp):
     * the ColorOS 17 Gallery `toolbar` node on the 照片 tab measures 184px at 565 dpi
     * (`com.coloros.gallery3d`, 3.53125 px/dp), which is 52.1dp — the same 52.13dp the Settings
     * compact toolbar measures. Reference device: OnePlus PLK110, ColorOS V17.0.0, 1272x2772.
     */
    val Height = TopAppBarDefaults.CollapsedHeight

    /**
     * The inset between the start edge and the first child (the navigation icon, or the title when
     * there is none). Gallery's title `TextView` starts at x=57px = 16.1dp; the same 16dp as
     * [TopAppBarDefaults.TitlePadding].
     */
    val StartPadding = TopAppBarDefaults.TitlePadding

    /**
     * The inset between the end edge and the last action. Gallery's rightmost action ends 56px =
     * 15.9dp from the edge; the same 16dp as [TopAppBarDefaults.ActionIconPadding].
     */
    val EndPadding = TopAppBarDefaults.ActionIconPadding

    /**
     * The spacing between neighbouring actions. Gallery's two actions (`action_main_tab_search`
     * and the overflow key) sit 35px = 9.9dp apart, which is
     * [TopAppBarDefaults.ActionMenuItemSpacing] (10dp, `coui_actionbar_menuitemview_item_spacing`
     * under `values-v37/`).
     */
    val ActionSpacing = TopAppBarDefaults.ActionMenuItemSpacing

    /**
     * The size of one action item. Gallery's actions measure 141px = 39.9dp square, which is
     * [TopAppBarDefaults.ActionItemMinWidth] (40dp); apply it to each icon-only action with
     * [couiTopAppBarActionItem].
     */
    val ActionItemSize = TopAppBarDefaults.ActionItemMinWidth

    /**
     * The gap between a navigation icon and the title. Not measured on Gallery (its 照片 tab has
     * no navigation icon); it is the Settings toolbar's value, [TopAppBarDefaults.NavigationIconGap].
     * It is only inserted when a navigation icon is present.
     */
    val NavigationIconGap = TopAppBarDefaults.NavigationIconGap

    /** The minimum gap between the title and the first action, [TopAppBarDefaults.ActionIconGap]. */
    val TitleActionGap = TopAppBarDefaults.ActionIconGap

    /**
     * The title text style: `COUITheme.textStyles.title1` (couiTextAppearanceHeadline1, 32sp).
     *
     * Gallery's "照片" title ink spans x 63..271 and y 181..283 at 3.53125 px/dp, i.e. 29.2dp of
     * ink height and two glyphs 59dp wide, consistent with a 32dp headline. The font weight was
     * not measured, so none is imposed here; pass a `titleStyle` with a weight to override.
     */
    @Composable
    fun titleStyle(): TextStyle = COUITheme.textStyles.title1
}

/**
 * The ColorOS 17 Gallery-style top app bar: a large title on the **first row**, next to the action
 * buttons, that never moves.
 *
 * [TopAppBar] puts its large title on a second row and collapses it while the content scrolls;
 * this bar keeps one [PinnedTopAppBarDefaults.Height] row below the status bar and lets only the
 * material change. The material is [AppBarBlurOverlay]: invisible at rest, so the page background
 * shows through, and fading in over the first 30dp of content scrolled under the bar
 * ([AppBarBlurOverlayDefaults.scrollAlpha]). The alpha is read in the draw phase from
 * `-scrollBehavior.state.contentOffset`, so scrolling never recomposes the bar.
 *
 * Give it a [PinnedScrollBehavior] and attach that behavior's `nestedScrollConnection` to the
 * scrolling content. Any other [ScrollBehavior] also works as long as it keeps
 * [TopAppBarState.contentOffset] in step with the content, but the bar itself never collapses.
 *
 * The material samples [LocalBackdrop]; without a backdrop (or runtime shaders) it paints the
 * plain alpha ramp.
 *
 * Measured on ColorOS V17.0.0 (OnePlus PLK110, 1272x2772) in the Gallery 照片 tab — see
 * [PinnedTopAppBarDefaults] for each token.
 *
 * @param title The title, a single ellipsized line at the start of the row.
 * @param modifier The modifier to be applied to the bar. The bar reports its own height, the
 *   status-bar inset plus [PinnedTopAppBarDefaults.Height]; the material reaches [extraHeight]
 *   past it without taking layout space.
 * @param scrollBehavior The behavior whose [TopAppBarState.contentOffset] drives the material
 *   alpha, normally a [PinnedScrollBehavior]. `null` keeps the material at its resting alpha, `0`.
 * @param defaultWindowInsetsPadding Whether to inset the row from the display cutout and the
 *   navigation bars on the horizontal sides, the way [TopAppBar] does. The status-bar inset is
 *   always applied.
 * @param containerColor The base colour of the material. Defaults to
 *   [AppBarBlurOverlayDefaults.containerColor] (the dark page background, `#303030`, measured on
 *   Gallery, in the dark theme).
 * @param titleColor The title colour.
 * @param titleStyle The title text style, [PinnedTopAppBarDefaults.titleStyle] by default.
 * @param backdrop The content the material samples, [LocalBackdrop] by default.
 * @param extraHeight How far the material reaches past the bar's bottom edge.
 * @param startPadding The inset between the start edge and the first child.
 * @param endPadding The inset between the end edge and the last action.
 * @param navigationIcon An optional icon at the start. [PinnedTopAppBarDefaults.NavigationIconGap]
 *   is inserted before the title only when this is non-null.
 * @param actions The actions at the end, laid out in a row spaced by
 *   [PinnedTopAppBarDefaults.ActionSpacing] and centred vertically. Size icon-only actions with
 *   [couiTopAppBarActionItem].
 */
@Composable
fun PinnedTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    scrollBehavior: ScrollBehavior? = null,
    defaultWindowInsetsPadding: Boolean = true,
    containerColor: Color = AppBarBlurOverlayDefaults.containerColor(),
    titleColor: Color = COUITheme.colorScheme.onSurface,
    titleStyle: TextStyle = PinnedTopAppBarDefaults.titleStyle(),
    backdrop: Backdrop? = LocalBackdrop.current,
    extraHeight: Dp = AppBarBlurOverlayDefaults.ExtraHeight,
    startPadding: Dp = PinnedTopAppBarDefaults.StartPadding,
    endPadding: Dp = PinnedTopAppBarDefaults.EndPadding,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val density = LocalDensity.current
    // Read in the draw phase only: the producer touches the state, the composition does not.
    val scrollFraction = remember(scrollBehavior, density) {
        {
            val behavior = scrollBehavior
            if (behavior == null) {
                0f
            } else {
                AppBarBlurOverlayDefaults.scrollAlpha(-behavior.state.contentOffset, density)
            }
        }
    }
    AppBarBlurOverlay(
        scrollFraction = scrollFraction,
        modifier = modifier,
        containerColor = containerColor,
        backdrop = backdrop,
        extraHeight = extraHeight,
    ) {
        Row(
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
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                .height(PinnedTopAppBarDefaults.Height)
                .padding(start = startPadding, end = endPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (navigationIcon != null) {
                navigationIcon()
                Spacer(Modifier.width(PinnedTopAppBarDefaults.NavigationIconGap))
            }
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = PinnedTopAppBarDefaults.TitleActionGap)
                    .semantics { heading() },
                color = titleColor,
                style = titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    PinnedTopAppBarDefaults.ActionSpacing,
                    Alignment.End,
                ),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

/**
 * Returns a [ScrollBehavior] that never moves the app bar.
 *
 * Every other [ScrollBehavior] collapses or hides the bar. This one only measures: it accumulates
 * the content's scroll into [TopAppBarState.contentOffset] — clamped to `<= 0`, so
 * `-state.contentOffset` is the distance the content has scrolled under the bar — and leaves
 * [TopAppBarState.heightOffset] alone. It is [ScrollBehavior.isPinned], has no snap or fling
 * animation, and does not intercept the pre-scroll phase, so the content always gets the whole
 * gesture.
 *
 * It is meant for [PinnedTopAppBar]. Handed to a [TopAppBar] it keeps the bar fully expanded and
 * fades that bar's divider in with the content's displacement instead of the collapse; handed to a
 * [SmallTopAppBar] it drives the divider as any behavior does.
 *
 * @param state The state the behavior writes [TopAppBarState.contentOffset] to. See
 *   [rememberTopAppBarState].
 * @param canScroll A callback used to determine whether scroll events are tracked. While it
 *   returns `false` the content offset is left untouched.
 */
@Suppress("ComposableNaming")
@Composable
fun PinnedScrollBehavior(
    state: TopAppBarState = rememberTopAppBarState(),
    canScroll: () -> Boolean = { true },
): ScrollBehavior = remember(state, canScroll) {
    PinnedScrollBehaviorImpl(state = state, canScroll = canScroll)
}

/** See [PinnedScrollBehavior]. */
@Stable
private class PinnedScrollBehaviorImpl(
    override val state: TopAppBarState,
    private val canScroll: () -> Boolean,
) : ScrollBehavior {
    override val isPinned: Boolean = true
    override val snapAnimationSpec: AnimationSpec<Float>? = null
    override val flingAnimationSpec: DecayAnimationSpec<Float>? = null
    override val nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (canScroll()) {
                    state.contentOffset = (state.contentOffset + consumed.y).coerceAtMost(0f)
                }
                // The bar consumes nothing: the content keeps its whole gesture.
                return Offset.Zero
            }
        }
}
