// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * The search transition of a [TopAppBar] whose bottom content is a search bar, after the COUI 17
 * settings home (`HomepageImpl.animToEdit` / `animToNormal`).
 *
 * Entering the search, the bar's title area gives way: the large title shrinks to 80%, fades out
 * and moves up, the toolbar scrolls away above the search bar, and the search bar rises to just
 * below the status bar. Leaving it plays the same in reverse. Pass the state to [TopAppBar], put a
 * search bar (such as [SearchBar] or [TopAppBarSearchSlot]) in its `bottomContent`, and fade the
 * page content out and the search results in with [topAppBarSearchContent] and
 * [topAppBarSearchResults].
 *
 * Create it with [rememberTopAppBarSearchState].
 */
@Stable
class TopAppBarSearchState internal constructor(active: Boolean) {
    /** Whether the search is active, or entering. */
    var isActive: Boolean by mutableStateOf(active)
        internal set

    internal val transition = Animatable(if (active) 1f else 0f)
    internal val results = Animatable(if (active) 1f else 0f)

    /**
     * How far the bar has moved into the search: 0 shows the title area, 1 has the search bar
     * lifted to the top (`mMainContentSpringAnim` / `mSearchBarEnterAnim`, normalised).
     */
    val progress: Float get() = transition.value

    /**
     * How far the search results have come in: 0 hidden, 1 in place
     * (`mSearchResultSpringAnim`, normalised). The settings home moves its results container from
     * [TopAppBarSearchDefaults.ResultsOffset] below to its place and fades it in on this value.
     */
    val resultsProgress: Float get() = results.value

    /**
     * The alpha of the title and of the page content behind the search: `1 - progress`, cubed
     * while the search is entering so they are gone before the search bar settles
     * (`HomepageImpl` `mainContentSpringAnim` update listener).
     */
    val contentAlpha: Float
        get() {
            val remaining = (1f - transition.value).coerceIn(0f, 1f)
            return if (isActive) remaining * remaining * remaining else remaining
        }

    /**
     * Whether the page content behind the search still shows ([contentAlpha] above 0). Keep it
     * composed while it is hidden if it should keep its scroll position.
     */
    val isContentVisible: Boolean by derivedStateOf { contentAlpha > 0f }

    /** Whether the search results show: while the search is active, and until they have left. */
    val isResultsVisible: Boolean by derivedStateOf { isActive || results.value > 0f }
}

/**
 * A search bar slot for [TopAppBar.bottomContent], matching the COUI 17 settings home page.
 *
 * It renders a capsule search field inside a fixed-height row, with the COUI page-side
 * horizontal padding, so the bar collapses away with the rest of the expanded title area.
 *
 * @param inputField The search input field (e.g. [SearchBar]'s `InputField`).
 * @param modifier The [Modifier] to be applied to the slot.
 * @param height The height of the slot row. COUI `coui_search_view_height` (52dp).
 * @param insideMargin The horizontal page-side padding of the capsule.
 */
@Composable
fun TopAppBarSearchSlot(
    inputField: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = TopAppBarDefaults.SearchSlotHeight,
    insideMargin: PaddingValues = TopAppBarDefaults.SearchSlotInsideMargin,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(insideMargin),
        contentAlignment = Alignment.CenterStart,
    ) {
        inputField()
    }
}

/**
 * Fades the page content behind a [TopAppBar] search out as the search enters, and back in as it
 * leaves ([TopAppBarSearchState.contentAlpha]).
 */
fun Modifier.topAppBarSearchContent(state: TopAppBarSearchState): Modifier = graphicsLayer {
    alpha = state.contentAlpha
}

/**
 * Brings the search results of a [TopAppBar] search in: they fade in while moving up from
 * [TopAppBarSearchDefaults.ResultsOffset] below their place, and drop out quickly as the search
 * leaves ([TopAppBarSearchState.resultsProgress]).
 */
fun Modifier.topAppBarSearchResults(state: TopAppBarSearchState): Modifier = graphicsLayer {
    val progress = state.resultsProgress
    alpha = progress
    translationY = TopAppBarSearchDefaults.ResultsOffset.toPx() * (1f - progress)
}

/**
 * Creates a [TopAppBarSearchState] that follows [active], running the COUI 17 settings home search
 * transition each time it changes.
 *
 * @param active Whether the search is active, typically the search bar's expanded state.
 */
@Composable
fun rememberTopAppBarSearchState(active: Boolean): TopAppBarSearchState {
    val state = remember { TopAppBarSearchState(active) }
    LaunchedEffect(state, active) {
        state.isActive = active
        val target = if (active) 1f else 0f
        coroutineScope {
            launch { state.transition.animateTo(target, TopAppBarSearchDefaults.TransitionSpec) }
            launch {
                state.results.animateTo(
                    target,
                    if (active) TopAppBarSearchDefaults.ResultsEnterSpec else TopAppBarSearchDefaults.ResultsExitSpec,
                )
            }
        }
    }
    return state
}

/** Contains default values used by [TopAppBarSearchState]. */
object TopAppBarSearchDefaults {
    /**
     * The title, toolbar and search bar spring, both ways: response 0.35, no bounce
     * (`mMainContentSpringForce` / `mSearchUiSpringForce`).
     */
    val TransitionSpec: AnimationSpec<Float> = folmeSpring(
        damping = 1f,
        response = 0.35f,
        visibilityThreshold = 0.001f,
    )

    /** The search results spring on entering: response 0.35, no bounce (`mSearchResultSpringForce`). */
    val ResultsEnterSpec: AnimationSpec<Float> = folmeSpring(
        damping = 1f,
        response = 0.35f,
        visibilityThreshold = 0.001f,
    )

    /** The search results spring on leaving: response 0.1, no bounce, so they drop out at once. */
    val ResultsExitSpec: AnimationSpec<Float> = folmeSpring(
        damping = 1f,
        response = 0.1f,
        visibilityThreshold = 0.001f,
    )

    /**
     * How far below their place the search results start (`homepage_init_searchbar_top`, 108dp);
     * translate them by `ResultsOffset * (1 - resultsProgress)`.
     */
    val ResultsOffset = 108.dp

    /** The scale the title shrinks to as the search enters (`setTitleTransform`, 0.8). */
    val TitleEndScale = 0.8f

    /**
     * How far the title moves up as the search enters: 200px on the 3.5x reference display
     * (`setTitleTransform(..., progress * -200f)`).
     */
    val TitleTravel = 57.dp
}
