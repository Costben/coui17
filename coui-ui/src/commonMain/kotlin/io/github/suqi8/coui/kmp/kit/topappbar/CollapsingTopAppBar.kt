// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.topappbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import io.github.suqi8.coui.kmp.kit.backdrop.CouiBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.LocalCouiBackdrop
import io.github.suqi8.coui.kmp.kit.search.CouiSearchBarState
import io.github.suqi8.coui.kmp.kit.search.rememberCouiSearchBarState
import io.github.suqi8.coui.kmp.kit.text.CouiTextKind
import io.github.suqi8.coui.kmp.kit.text.couiFontMetrics
import io.github.suqi8.coui.kmp.kit.text.couiTextStyle
import io.github.suqi8.coui.kmp.kit.text.rememberCouiTextStyle
import io.github.suqi8.coui.kmp.kit.theme.CouiTheme
import kotlin.math.roundToInt

/**
 * Large title with an optional persistent search slot; when the search slot is omitted, the title
 * shrinks and remains pinned in the top bar without fading out.
 * [state]'s connection must be attached to an ancestor of the scrolling content container.
 * [dark] only controls this top bar; the search slot receives the same mode from the host or shares
 * a local theme, and title alpha multiplies with the animation.
 */
@Composable
internal fun CollapsingTopAppBar(
    modifier: Modifier = Modifier,
    state: CouiCollapsingAppBarState,
    searchState: CouiSearchBarState = rememberCouiSearchBarState(false),
    title: String,
    color: Color = Color.Unspecified,
    backdrop: CouiBackdrop? = LocalCouiBackdrop.current,
    backgroundColor: Color = Color.Unspecified,
    dark: Boolean = CouiTheme.isDark,
    searchBar: (@Composable () -> Unit)? = null,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val hasSearchBar = searchBar != null
    val searchActive = hasSearchBar && searchState.active
    val titleColor = if (color == Color.Unspecified) CouiTheme.colorSchemeFor(dark).onSurface else color
    val titleStyle = if (hasSearchBar) {
        rememberCouiTextStyle(CouiTextKind.HomeTitle)
    } else {
        remember(density) {
            couiTextStyle(
                familyName = "sans-serif-semibold",
                weight = 700,
                pixels = with(density) { CouiCollapsingAppBarDefaults.expandedTitleSize.toPx() },
                lineSpacing = 1f,
                includeFontPadding = false,
                density = density,
            )
        }
    }
    val descent = remember(titleStyle, density) {
        couiFontMetrics(
            familyName = "sans-serif-semibold",
            weight = 700,
            pixels = with(density) { titleStyle.fontSize.toPx() },
        ).descentPx
    }
    val titleHeight = with(density) {
        (if (hasSearchBar) CouiCollapsingAppBarDefaults.titleHeight else CouiCollapsingAppBarDefaults.titleOnlyHeight)
            .roundToPx().toFloat()
    }
    val collapsedHeight = with(density) { CouiCollapsingAppBarDefaults.collapsedTitleHeight.roundToPx().toFloat() }
    val titleBottom = with(density) { CouiCollapsingAppBarDefaults.titleBottom.toPx().toInt().toFloat() }
    val start = with(density) {
        (if (hasSearchBar) CouiCollapsingAppBarDefaults.titleStart else CouiCollapsingAppBarDefaults.titleOnlyStart)
            .toPx().toInt().toFloat()
    }
    val offset = state.offsetPx.coerceIn(0f, if (hasSearchBar) titleHeight else titleHeight - collapsedHeight)
    val f = (offset / (titleHeight - collapsedHeight)).coerceIn(0f, 1f)
    val titleFraction = if (hasSearchBar) f else CouiCollapsingAppBarDefaults.titleEasing.transform(f)
    val collapsedScale = if (hasSearchBar) {
        .7f
    } else {
        CouiCollapsingAppBarDefaults.collapsedTitleSize / CouiCollapsingAppBarDefaults.expandedTitleSize
    }
    val p = if (hasSearchBar) searchState.pageProgress.coerceIn(0f, 1f) else 0f
    SideEffect {
        state.updateRange(
            density,
            hasSearchBar,
            CouiCollapsingAppBarDefaults.titleOnlyHeight,
            CouiCollapsingAppBarDefaults.collapsedTitleHeight,
        )
        state.updateSearch(hasSearchBar && searchState.expanded, searchActive)
    }
    TopAppBarSurface(
        modifier.fillMaxWidth(),
        backdrop,
        referenceBlurRadius = with(density) { CouiCollapsingAppBarDefaults.referenceBlurRadiusPx.toDp() },
        bottomExtension = if (hasSearchBar) {
            CouiCollapsingAppBarDefaults.searchBlurExtension
        } else {
            CouiCollapsingAppBarDefaults.titleOnlyBlurExtension
        },
        gradientSpeed = 3f,
        gradientColor = backgroundColor,
        dark = dark,
        // Content follows accepted scroll directly while the title keeps spring follow; blur must not lag behind the title.
        backgroundAlpha = if (!hasSearchBar || searchActive) {
            1f
        } else {
            (state.contentOffsetPx / (30 * density.density)).coerceIn(0f, 1f)
        },
        topExtension = { with(density) { offset.toDp() } },
    ) {
        Layout(
            modifier = Modifier.statusBarsPadding(),
            content = {
                BasicText(
                    title,
                    Modifier.semantics { heading() }.graphicsLayer {
                        alpha = if (hasSearchBar) (1f - f) * searchState.contentAlpha else 1f
                        scaleX = (1f - (1f - collapsedScale) * titleFraction) *
                            (if (hasSearchBar) .8f + .2f * searchState.contentAlpha else 1f)
                        scaleY = scaleX
                        translationY = -200f * p
                        transformOrigin = TransformOrigin(if (layoutDirection == LayoutDirection.Rtl) 1f else 0f, 0f)
                    },
                    style = titleStyle.copy(color = titleColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (searchBar != null) Box(propagateMinConstraints = true) { searchBar() }
            },
        ) { children, constraints ->
            val titleNode = children[0].measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxWidth = (constraints.maxWidth - 2 * start.roundToInt()).coerceAtLeast(0),
                ),
            )
            val search = children.getOrNull(1)?.measure(constraints.copy(minWidth = constraints.maxWidth, minHeight = 0))
            val remainingTitle = (titleHeight - offset).coerceAtLeast(if (hasSearchBar) 0f else collapsedHeight)
            val searchY = remainingTitle
            layout(constraints.maxWidth, (searchY + (search?.height ?: 0)).roundToInt()) {
                val expandedY = if (hasSearchBar) {
                    titleHeight - titleBottom - titleNode.height + descent
                } else {
                    with(density) { CouiCollapsingAppBarDefaults.titleOnlyTop.toPx() }
                }
                val collapsedY = titleHeight - collapsedHeight +
                    (collapsedHeight - titleNode.height * (if (hasSearchBar) 1f else collapsedScale)) / 2
                val titleY = expandedY + (collapsedY - expandedY) * titleFraction - offset
                titleNode.placeRelative(
                    (start - if (hasSearchBar) (8 * density.density) * f else 0f).roundToInt(),
                    titleY.roundToInt(),
                )
                search?.placeRelative(0, searchY.roundToInt())
            }
        }
    }
}
