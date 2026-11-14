// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.COUIInEase
import io.github.suqi8.coui.kmp.anim.COUIMoveEase
import io.github.suqi8.coui.kmp.anim.COUIOutEase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Default timings and distances for [SearchResultContainer] and
 * [rememberSearchResultsTransitionState].
 */
object SearchResultContainerDefaults {
    val ResultsRiseDistance: Dp = 48.dp
    val ResultsRiseMillis: Int = 360
    val RefreshRiseDistance: Dp = 20.dp
    val RefreshRiseMillis: Int = 260
    val SuggestFadeOutMillis: Int = 90
    val LoadingEnterDelayMillis: Long = 50L
    val LoadingFadeInMillis: Int = 160
    val LoadingFadeOutMillis: Int = 120
    val LoadingEnterShift: Dp = 12.dp
    val LoadingExitShift: Dp = 16.dp
    val LoadingEnterScale: Float = 0.88f
    val ModeFadeOutMillis: Int = 120
    val ModeFadeInMillis: Int = 200

    /**
     * Creates a [SearchResultsTransitionSpec] with overridable durations and travel distances.
     */
    @Composable
    fun transitionSpec(
        resultsRiseDistance: Dp = ResultsRiseDistance,
        resultsRiseMillis: Int = ResultsRiseMillis,
        refreshRiseDistance: Dp = RefreshRiseDistance,
        refreshRiseMillis: Int = RefreshRiseMillis,
        suggestFadeOutMillis: Int = SuggestFadeOutMillis,
        loadingEnterDelayMillis: Long = LoadingEnterDelayMillis,
        loadingFadeInMillis: Int = LoadingFadeInMillis,
        loadingFadeOutMillis: Int = LoadingFadeOutMillis,
        loadingEnterShift: Dp = LoadingEnterShift,
        loadingExitShift: Dp = LoadingExitShift,
        loadingEnterScale: Float = LoadingEnterScale,
        modeFadeOutMillis: Int = ModeFadeOutMillis,
        modeFadeInMillis: Int = ModeFadeInMillis,
    ): SearchResultsTransitionSpec = remember(
        resultsRiseDistance,
        resultsRiseMillis,
        refreshRiseDistance,
        refreshRiseMillis,
        suggestFadeOutMillis,
        loadingEnterDelayMillis,
        loadingFadeInMillis,
        loadingFadeOutMillis,
        loadingEnterShift,
        loadingExitShift,
        loadingEnterScale,
        modeFadeOutMillis,
        modeFadeInMillis,
    ) {
        SearchResultsTransitionSpec(
            resultsRiseDistance = resultsRiseDistance,
            resultsRiseMillis = resultsRiseMillis,
            refreshRiseDistance = refreshRiseDistance,
            refreshRiseMillis = refreshRiseMillis,
            suggestFadeOutMillis = suggestFadeOutMillis,
            loadingEnterDelayMillis = loadingEnterDelayMillis,
            loadingFadeInMillis = loadingFadeInMillis,
            loadingFadeOutMillis = loadingFadeOutMillis,
            loadingEnterShift = loadingEnterShift,
            loadingExitShift = loadingExitShift,
            loadingEnterScale = loadingEnterScale,
            modeFadeOutMillis = modeFadeOutMillis,
            modeFadeInMillis = modeFadeInMillis,
        )
    }

    /** Default loading spinner shown centered horizontally below the container's top padding. */
    @Composable
    fun DefaultLoadingIndicator() {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

/**
 * Immutable motion parameters consumed by [rememberSearchResultsTransitionState] and
 * [SearchResultContainer].
 */
@Immutable
data class SearchResultsTransitionSpec(
    val resultsRiseDistance: Dp = SearchResultContainerDefaults.ResultsRiseDistance,
    val resultsRiseMillis: Int = SearchResultContainerDefaults.ResultsRiseMillis,
    val refreshRiseDistance: Dp = SearchResultContainerDefaults.RefreshRiseDistance,
    val refreshRiseMillis: Int = SearchResultContainerDefaults.RefreshRiseMillis,
    val suggestFadeOutMillis: Int = SearchResultContainerDefaults.SuggestFadeOutMillis,
    val loadingEnterDelayMillis: Long = SearchResultContainerDefaults.LoadingEnterDelayMillis,
    val loadingFadeInMillis: Int = SearchResultContainerDefaults.LoadingFadeInMillis,
    val loadingFadeOutMillis: Int = SearchResultContainerDefaults.LoadingFadeOutMillis,
    val loadingEnterShift: Dp = SearchResultContainerDefaults.LoadingEnterShift,
    val loadingExitShift: Dp = SearchResultContainerDefaults.LoadingExitShift,
    val loadingEnterScale: Float = SearchResultContainerDefaults.LoadingEnterScale,
    val modeFadeOutMillis: Int = SearchResultContainerDefaults.ModeFadeOutMillis,
    val modeFadeInMillis: Int = SearchResultContainerDefaults.ModeFadeInMillis,
)

/**
 * Coordinates the search page transitions across prompt/suggestions, loading indicator, results,
 * and top-bar filter visibility:
 *
 * - Entering results: fades the prompt/suggestion list out first, skips the loading spinner when
 *   results arrive before [SearchResultsTransitionSpec.loadingEnterDelayMillis], otherwise fades
 *   the spinner in softly. Once data arrives, the spinner lifts and dissolves in sync while
 *   results rise from below. [resultsVisible] flips true only after [riseProgress] snaps to `0f`
 *   so results never flash for one frame at full alpha before rising.
 * - Switching filters or refreshing inside results: runs a lighter refresh rise
 *   ([SearchResultsTransitionSpec.refreshRiseDistance]) instead of replaying the full entry rise.
 * - Returning to prompt/history: fades results (and any active spinner) out first, keeps
 *   [filtersVisible] true until the fade-out finishes so a companion [SearchFilterTopAppBar]
 *   collapses in lockstep, then swaps content and fades the prompt back in.
 */
@Stable
class SearchResultsTransitionState internal constructor(
    initialShowingResults: Boolean,
    initialLoading: Boolean,
    initialQueryKey: Any?,
    internal val spec: SearchResultsTransitionSpec,
) {
    /**
     * Whether the content slot should render the results branch rather than the
     * prompt/history/suggestions branch. Lags behind `showingResults` by one fade-out duration so
     * content swaps while transparent.
     */
    var shownResults: Boolean by mutableStateOf(initialShowingResults)
        internal set

    /**
     * Whether result items should be emitted when [shownResults] is true. Remains `false` while
     * loading and flips `true` only after [riseProgress] has been snapped to `0f`.
     */
    var resultsVisible: Boolean by mutableStateOf(initialShowingResults && !initialLoading)
        internal set

    /**
     * Whether a companion [SearchFilterTopAppBar] should keep its filter strip visible. Turns
     * `true` immediately on submit and waits for the result fade-out to complete before collapsing
     * on return.
     */
    var filtersVisible: Boolean by mutableStateOf(initialShowingResults)
        internal set

    internal var animatedQueryKey: Any? =
        if (initialShowingResults && !initialLoading) initialQueryKey else null
    internal var riseOnNextResults: Boolean = initialShowingResults && initialLoading
    internal var riseDistanceTarget: Dp by mutableStateOf(spec.resultsRiseDistance)
    internal var loadingExiting: Boolean by mutableStateOf(false)

    internal val modeFade = Animatable(1f)
    internal val riseProgress = Animatable(if (initialShowingResults && !initialLoading) 1f else 0f)
    internal val loadingProgress = Animatable(0f)

    /** Combined alpha currently applied to the main content layer. */
    val contentAlpha: Float
        get() {
            val rise = if (shownResults) riseProgress.value else 1f
            return modeFade.value * rise
        }

    /** Alpha currently applied to the loading indicator overlay. */
    val loadingAlpha: Float
        get() = loadingProgress.value

    internal suspend fun syncMode(showingResults: Boolean) {
        if (showingResults) {
            filtersVisible = true
            if (!shownResults) {
                modeFade.animateTo(0f, tween(spec.suggestFadeOutMillis, easing = COUIInEase))
                resultsVisible = false
                riseProgress.snapTo(0f)
                riseOnNextResults = true
                shownResults = true
                modeFade.snapTo(1f)
            } else {
                modeFade.animateTo(1f, tween(spec.modeFadeInMillis, easing = COUIOutEase))
            }
        } else {
            if (shownResults) {
                loadingExiting = true
                coroutineScope {
                    if (loadingProgress.value > 0.001f) {
                        launch {
                            loadingProgress.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(spec.modeFadeOutMillis, easing = COUIInEase),
                            )
                        }
                    } else {
                        loadingProgress.snapTo(0f)
                    }
                    if (resultsVisible) {
                        launch {
                            modeFade.animateTo(0f, tween(spec.modeFadeOutMillis, easing = COUIInEase))
                        }
                    } else {
                        modeFade.snapTo(0f)
                    }
                }
                filtersVisible = false
                resultsVisible = false
                riseProgress.snapTo(1f)
                riseOnNextResults = false
                animatedQueryKey = null
                shownResults = false
            } else {
                filtersVisible = false
                loadingProgress.snapTo(0f)
            }
            modeFade.animateTo(1f, tween(spec.modeFadeInMillis, easing = COUIOutEase))
        }
    }

    internal suspend fun syncLoading(
        activeResults: Boolean,
        loading: Boolean,
        queryKey: Any?,
    ) {
        if (!activeResults) return
        if (loading) {
            resultsVisible = false
            riseProgress.snapTo(0f)
            if (spec.loadingEnterDelayMillis > 0L) {
                delay(spec.loadingEnterDelayMillis)
            }
            loadingExiting = false
            loadingProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(spec.loadingFadeInMillis, easing = COUIOutEase),
            )
        } else {
            val firstRise = riseOnNextResults || animatedQueryKey != queryKey
            if (resultsVisible && !firstRise) return
            loadingExiting = true
            animatedQueryKey = queryKey
            riseOnNextResults = false
            riseDistanceTarget = if (firstRise) spec.resultsRiseDistance else spec.refreshRiseDistance
            val riseMillis = if (firstRise) spec.resultsRiseMillis else spec.refreshRiseMillis
            coroutineScope {
                if (loadingProgress.value > 0.001f) {
                    launch {
                        loadingProgress.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(spec.loadingFadeOutMillis, easing = COUIInEase),
                        )
                    }
                } else {
                    loadingProgress.snapTo(0f)
                }
                launch {
                    riseProgress.snapTo(0f)
                    resultsVisible = true
                    riseProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = riseMillis,
                            easing = COUIMoveEase,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Remembers and drives a [SearchResultsTransitionState] synchronized with [showingResults],
 * [loading], and [queryKey].
 *
 * @param showingResults Whether the search page is currently in the submitted results mode.
 * @param loading Whether the active result query or tab is currently fetching data.
 * @param queryKey Optional submitted query identifier. Changing it while [showingResults] stays
 *   `true` triggers a full bottom-up entry rise instead of the lighter refresh rise.
 * @param spec Motion specification controlling durations and travel distances.
 */
@Composable
fun rememberSearchResultsTransitionState(
    showingResults: Boolean,
    loading: Boolean,
    queryKey: Any? = null,
    spec: SearchResultsTransitionSpec = SearchResultContainerDefaults.transitionSpec(),
): SearchResultsTransitionState {
    val state = remember(spec) {
        SearchResultsTransitionState(
            initialShowingResults = showingResults,
            initialLoading = loading,
            initialQueryKey = queryKey,
            spec = spec,
        )
    }
    LaunchedEffect(state, showingResults) {
        state.syncMode(showingResults)
    }
    val activeResults = state.shownResults && showingResults
    LaunchedEffect(state, activeResults, loading, queryKey) {
        state.syncLoading(activeResults, loading, queryKey)
    }
    return state
}

/**
 * Receiver scope passed to [SearchResultContainer]'s content slot.
 */
@Stable
class SearchResultContainerScope internal constructor(
    private val state: SearchResultsTransitionState,
) {
    /** Mirrors [SearchResultsTransitionState.shownResults]. */
    val shownResults: Boolean get() = state.shownResults

    /** Mirrors [SearchResultsTransitionState.resultsVisible]. */
    val resultsVisible: Boolean get() = state.resultsVisible
}

/**
 * Hosts a search page's content and loading overlay under a single coordinated transition.
 *
 * Pair this container with [rememberSearchResultsTransitionState] and optionally bind
 * [SearchResultsTransitionState.filtersVisible] to [SearchFilterTopAppBar] so the filter strip
 * collapses in lockstep with the outgoing result fade.
 *
 * @param state Transition state created via [rememberSearchResultsTransitionState].
 * @param modifier Modifier applied to the root container.
 * @param loadingTopPadding Top padding applied to [loadingContent] so it clears the pinned top bar.
 * @param loadingContent Loading indicator composable rendered above the list while fetching.
 * @param content Main content slot; switch between prompt/suggestions (`!shownResults`) and
 *   results (`shownResults && resultsVisible`) using the receiver scope.
 */
@Composable
fun SearchResultContainer(
    state: SearchResultsTransitionState,
    modifier: Modifier = Modifier,
    loadingTopPadding: Dp = 0.dp,
    loadingContent: @Composable () -> Unit = { SearchResultContainerDefaults.DefaultLoadingIndicator() },
    content: @Composable SearchResultContainerScope.() -> Unit,
) {
    val density = LocalDensity.current
    val riseDistancePx = with(density) { state.riseDistanceTarget.toPx() }
    val loadingEnterShiftPx = with(density) { state.spec.loadingEnterShift.toPx() }
    val loadingExitShiftPx = with(density) { state.spec.loadingExitShift.toPx() }
    val scope = remember(state) { SearchResultContainerScope(state) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val rise = if (state.shownResults) state.riseProgress.value else 1f
                    translationY = if (rise < 0.999f) (1f - rise) * riseDistancePx else 0f
                    alpha = state.modeFade.value * rise
                },
        ) {
            scope.content()
        }
        if (state.loadingProgress.value > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = loadingTopPadding)
                    .graphicsLayer {
                        val p = state.loadingProgress.value
                        val minScale = state.spec.loadingEnterScale
                        val scale = minScale + (1f - minScale) * p
                        alpha = p
                        scaleX = scale
                        scaleY = scale
                        translationY = if (state.loadingExiting) {
                            -(1f - p) * loadingExitShiftPx
                        } else {
                            (1f - p) * loadingEnterShiftPx
                        }
                    },
            ) {
                loadingContent()
            }
        }
    }
}
