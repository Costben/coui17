// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.anim.COUIEase
import io.github.suqi8.coui.kmp.anim.COUIMoveEase
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * Default metrics, colors, and motion parameters for [SearchResultGroupCard] and
 * [SearchFilterResultsContainer], aligned with ColorOS 17 Settings search
 * (`SearchResultsGroupAdapter`, `search_intent_item_*.xml`, and
 * `HomepageSearchFeature.startShowHideAnimation`).
 */
object SearchResultGroupCardDefaults {

    /** Vertical spacing between grouped result cards (`dimen/dimen_16`). */
    val CardSpacing: Dp = 16.dp

    /** Top spacing above the first result group card (`dimen/dimen_12`). */
    val FirstCardTopSpacing: Dp = 12.dp

    /**
     * Padding of the embedded icon-and-title header inside a [SearchResultGroupCard]
     * (`search_intent_item_head.xml`: 16dp start/end/top, 4dp bottom).
     */
    val HeaderPadding: PaddingValues = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = 16.dp,
        bottom = 4.dp,
    )

    /** Leading group icon size inside the embedded header (`dimen/dimen_16`). */
    val HeaderIconSize: Dp = 16.dp

    /** Gap between the leading group icon and title text (`dimen/dimen_8`). */
    val HeaderIconSpacing: Dp = 8.dp

    /**
     * Upward travel distance when the search results panel enters
     * (`dimen/search_result_list_translation_y`, 15dp).
     */
    val RiseDistance: Dp = 15.dp

    /** Enter fade/rise duration (`HomepageSearchFeature.DURATION_300`). */
    val EnterDurationMillis: Int = 300

    /** Exit fade duration (`HomepageSearchFeature.DURATION_150`). */
    val ExitDurationMillis: Int = 150

    /** Fade-in duration for newly appearing group cards during live filtering. */
    val ItemFadeInDurationMillis: Int = 220

    /**
     * Returns the [CardListPosition] for an item placed below a [SearchResultGroupCard]'s
     * embedded header. Because the header already occupies the card's rounded top edge, items
     * before the last use [CardListPosition.Middle] (drawing hairline dividers between rows)
     * and the final item uses [CardListPosition.Tail].
     */
    fun itemPositionFor(index: Int, total: Int): CardListPosition = if (index == total - 1) CardListPosition.Tail else CardListPosition.Middle

    /** Recommended fade-in animation spec for `LazyItemScope.animateItem` during live filtering. */
    fun itemFadeInSpec(): FiniteAnimationSpec<Float> = tween(ItemFadeInDurationMillis, easing = COUIMoveEase)

    /** Recommended placement spring for `LazyItemScope.animateItem` during live filtering. */
    fun itemPlacementSpec(): FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Recommended fade-out animation spec for `LazyItemScope.animateItem` during live filtering. */
    fun itemFadeOutSpec(): FiniteAnimationSpec<Float> = tween(ExitDurationMillis, easing = COUIEase)

    /**
     * Creates a [SearchResultGroupCardColors] resolving ColorOS default header, highlight, and
     * card surface roles from [COUITheme].
     */
    @Composable
    fun colors(
        headerColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
        highlightColor: Color = COUITheme.colorScheme.primary,
        cardColors: CardColors = CardDefaults.defaultColors(),
    ): SearchResultGroupCardColors = remember(headerColor, highlightColor, cardColors) {
        SearchResultGroupCardColors(
            headerColor = headerColor,
            highlightColor = highlightColor,
            cardColors = cardColors,
        )
    }
}

/**
 * Colors used by [SearchResultGroupCard].
 *
 * @param headerColor Text color of the embedded group header title.
 * @param highlightColor Accent color used for matched query spans (`couiColorPrimary`).
 * @param cardColors Surface colors forwarded to the underlying [Card].
 */
@Immutable
data class SearchResultGroupCardColors(
    val headerColor: Color,
    val highlightColor: Color,
    val cardColors: CardColors,
)

/**
 * A ColorOS 17 Settings-style grouped search result card with an embedded icon-and-title header
 * (`SearchResultsGroupAdapter` / `search_intent_item_head.xml`).
 *
 * Place matching preference or list rows in [content], assigning each row's `cardListPosition`
 * with [SearchResultGroupCardDefaults.itemPositionFor] so dividers and bottom corner padding
 * match native COUI search cards.
 *
 * @param title The group title displayed in the embedded card header.
 * @param modifier The [Modifier] applied to the card.
 * @param icon Optional leading group icon rendered inside a 16dp slot before [title].
 * @param colors Color roles for the header and card surface.
 * @param headerPadding Padding applied around the embedded header row.
 * @param cornerRadius Corner radius of the card. Defaults to [CardDefaults.GroupCornerRadius].
 * @param weight Corner curve exponent. Defaults to [CardDefaults.GroupWeight].
 * @param content Matched items rendered inside the card below the header.
 */
@Composable
@NonRestartableComposable
fun SearchResultGroupCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    colors: SearchResultGroupCardColors = SearchResultGroupCardDefaults.colors(),
    headerPadding: PaddingValues = SearchResultGroupCardDefaults.HeaderPadding,
    cornerRadius: Dp = CardDefaults.GroupCornerRadius,
    weight: Float = CardDefaults.GroupWeight,
    content: @Composable ColumnScope.() -> Unit,
) {
    SearchResultGroupCard(
        title = AnnotatedString(title),
        modifier = modifier,
        icon = icon,
        colors = colors,
        headerPadding = headerPadding,
        cornerRadius = cornerRadius,
        weight = weight,
        content = content,
    )
}

/**
 * A ColorOS 17 Settings-style grouped search result card whose embedded header supports styled
 * [AnnotatedString] highlighting (for example when the search query matches the group title).
 *
 * @param title The styled group title displayed in the embedded card header.
 * @param modifier The [Modifier] applied to the card.
 * @param icon Optional leading group icon rendered inside a 16dp slot before [title].
 * @param colors Color roles for the header and card surface.
 * @param headerPadding Padding applied around the embedded header row.
 * @param cornerRadius Corner radius of the card. Defaults to [CardDefaults.GroupCornerRadius].
 * @param weight Corner curve exponent. Defaults to [CardDefaults.GroupWeight].
 * @param content Matched items rendered inside the card below the header.
 */
@Composable
fun SearchResultGroupCard(
    title: AnnotatedString,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    colors: SearchResultGroupCardColors = SearchResultGroupCardDefaults.colors(),
    headerPadding: PaddingValues = SearchResultGroupCardDefaults.HeaderPadding,
    cornerRadius: Dp = CardDefaults.GroupCornerRadius,
    weight: Float = CardDefaults.GroupWeight,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = cornerRadius,
        insideMargin = PaddingValues(0.dp),
        colors = colors.cardColors,
        weight = weight,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(headerPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier.size(SearchResultGroupCardDefaults.HeaderIconSize),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }
                Spacer(Modifier.width(SearchResultGroupCardDefaults.HeaderIconSpacing))
            }
            Text(
                text = title,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.headerColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        content()
    }
}

/**
 * Hosts a live-filtered search results overlay with the ColorOS 17 Settings panel transition
 * (`HomepageSearchFeature.startShowHideAnimation`).
 *
 * Pair this container with [rememberTopAppBarSearchState] and [Modifier.topAppBarSearchContent]
 * on the underlying page list:
 * - Entering results: rises [riseDistance] (15dp) and fades in over [enterDurationMillis] (300ms,
 *   [COUIMoveEase]).
 * - Leaving results: freezes the last visible [state] snapshot while fading out over
 *   [exitDurationMillis] (150ms, [COUIEase]) so clearing the query never flashes unfiltered content.
 * - Switching between results and empty state cross-fades cleanly with the same curves.
 *
 * @param visible Whether the filtered search results overlay should be visible (typically
 *   `query.isNotBlank()`).
 * @param state Immutable snapshot of the current search query and filtered results.
 * @param isEmpty Predicate returning `true` when [state] has no matching results.
 * @param modifier The [Modifier] applied to the container.
 * @param riseDistance Vertical rise offset on entering results.
 * @param enterDurationMillis Duration in milliseconds for entering transitions.
 * @param exitDurationMillis Duration in milliseconds for exiting transitions.
 * @param emptyContent Composable shown when [isEmpty] returns `true`.
 * @param resultsContent Composable shown when [isEmpty] returns `false`.
 */
@Composable
fun <T> SearchFilterResultsContainer(
    visible: Boolean,
    state: T,
    isEmpty: (T) -> Boolean,
    modifier: Modifier = Modifier,
    riseDistance: Dp = SearchResultGroupCardDefaults.RiseDistance,
    enterDurationMillis: Int = SearchResultGroupCardDefaults.EnterDurationMillis,
    exitDurationMillis: Int = SearchResultGroupCardDefaults.ExitDurationMillis,
    emptyContent: @Composable (T) -> Unit,
    resultsContent: @Composable (T) -> Unit,
) {
    val density = LocalDensity.current
    val risePx = with(density) { riseDistance.roundToPx() }

    val snapshot = remember { mutableStateOf(state) }
    if (visible) {
        snapshot.value = state
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(
            animationSpec = tween(enterDurationMillis, easing = COUIMoveEase),
        ) + slideInVertically(
            animationSpec = tween(enterDurationMillis, easing = COUIMoveEase),
            initialOffsetY = { risePx },
        ),
        exit = fadeOut(
            animationSpec = tween(exitDurationMillis, easing = COUIEase),
        ),
    ) {
        val displayState = if (visible) state else snapshot.value
        val hasResults = !isEmpty(displayState)

        AnimatedContent(
            targetState = hasResults,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val enterSpec = if (targetState) {
                    fadeIn(tween(enterDurationMillis, easing = COUIMoveEase)) +
                        slideInVertically(
                            animationSpec = tween(enterDurationMillis, easing = COUIMoveEase),
                            initialOffsetY = { risePx },
                        )
                } else {
                    fadeIn(tween(enterDurationMillis, easing = COUIMoveEase))
                }
                enterSpec togetherWith fadeOut(tween(exitDurationMillis, easing = COUIEase))
            },
            label = "coui_search_filter_results",
        ) { showingResults ->
            if (showingResults) {
                resultsContent(displayState)
            } else {
                emptyContent(displayState)
            }
        }
    }
}
