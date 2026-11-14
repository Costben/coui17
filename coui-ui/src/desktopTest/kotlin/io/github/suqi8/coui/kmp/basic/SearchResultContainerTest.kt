// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies the coordinated state machine of [SearchResultContainer] and
 * [rememberSearchResultsTransitionState]:
 * - fast loads skip the loading spinner;
 * - slow loads fade the spinner in softly, dissolve it as results arrive, and never flash results
 *   at full alpha on their first visible frame;
 * - leaving results keeps [SearchResultsTransitionState.filtersVisible] true until the fade-out
 *   completes.
 */
@OptIn(ExperimentalTestApi::class)
class SearchResultContainerTest {

    @Test
    fun slowLoadDissolvesSpinnerAndRisesResultsWithoutFlash() = runSkikoComposeUiTest(
        size = Size(360f, 640f),
        density = Density(1f),
    ) {
        var showingResults by mutableStateOf(false)
        var loading by mutableStateOf(false)
        var queryKey by mutableStateOf("")
        var captured: SearchResultsTransitionState? = null

        setContent {
            COUITheme {
                val transition = rememberSearchResultsTransitionState(
                    showingResults = showingResults,
                    loading = loading,
                    queryKey = queryKey,
                )
                captured = transition
                SearchResultContainer(
                    state = transition,
                    modifier = Modifier.fillMaxSize(),
                    loadingContent = {
                        Box(Modifier.testTag("loading_indicator"))
                    },
                ) {
                    if (!shownResults) {
                        Box(Modifier.fillMaxSize().testTag("prompt_view"))
                    } else if (resultsVisible) {
                        Box(Modifier.fillMaxSize().testTag("results_view"))
                    }
                }
            }
        }

        mainClock.autoAdvance = false
        mainClock.advanceTimeByFrame()
        val state = requireNotNull(captured)
        assertFalse(state.shownResults)
        assertFalse(state.filtersVisible)
        assertEquals(1f, state.contentAlpha, 0.01f)

        // Submit query -> loading starts.
        runOnIdle {
            queryKey = "coui"
            loading = true
            showingResults = true
        }
        mainClock.advanceTimeByFrame()
        assertTrue(state.filtersVisible)

        // Advance past prompt fade-out (90ms) + loading delay (50ms) + loading fade-in (160ms).
        repeat(24) { mainClock.advanceTimeBy(16L) }
        assertTrue(state.shownResults)
        assertFalse(state.resultsVisible)
        assertTrue(state.loadingAlpha > 0.5f, "spinner faded in: ${state.loadingAlpha}")

        // Data arrives: first frame after resultsVisible becomes true must start at alpha ~0 and
        // shifted downwards (not flashed at alpha 1 / top 0).
        runOnIdle { loading = false }
        var firstResultAlpha = -1f
        var firstResultTop = -1f
        for (i in 0 until 8) {
            mainClock.advanceTimeBy(16L)
            mainClock.advanceTimeByFrame()
            if (state.resultsVisible) {
                firstResultAlpha = state.contentAlpha
                firstResultTop = onNodeWithTag("results_view").getBoundsInRoot().top.value
                break
            }
        }
        assertTrue(state.resultsVisible, "results became visible")
        assertTrue(firstResultAlpha < 0.35f, "results did not flash at full alpha: $firstResultAlpha")
        assertTrue(firstResultTop > 20f, "results started shifted downward: $firstResultTop")

        // Advance through the full rise & spinner exit.
        repeat(30) { mainClock.advanceTimeBy(16L) }
        assertEquals(0f, state.loadingAlpha, 0.01f)
        assertEquals(1f, state.contentAlpha, 0.01f)
        assertEquals(0f, onNodeWithTag("results_view").getBoundsInRoot().top.value, 0.5f)
    }

    @Test
    fun fastLoadSkipsSpinnerEntirely() = runSkikoComposeUiTest(
        size = Size(360f, 640f),
        density = Density(1f),
    ) {
        var showingResults by mutableStateOf(false)
        var loading by mutableStateOf(false)
        var captured: SearchResultsTransitionState? = null

        setContent {
            COUITheme {
                val transition = rememberSearchResultsTransitionState(
                    showingResults = showingResults,
                    loading = loading,
                )
                captured = transition
                SearchResultContainer(
                    state = transition,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (!shownResults) {
                        Box(Modifier.fillMaxSize())
                    } else if (resultsVisible) {
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }

        mainClock.autoAdvance = false
        mainClock.advanceTimeByFrame()
        val state = requireNotNull(captured)

        runOnIdle {
            loading = true
            showingResults = true
        }
        // Return data within 48ms (before 90ms prompt fade-out finishes).
        repeat(3) { mainClock.advanceTimeBy(16L) }
        runOnIdle { loading = false }

        var maxLoadingAlpha = 0f
        repeat(40) {
            mainClock.advanceTimeBy(16L)
            if (state.loadingAlpha > maxLoadingAlpha) maxLoadingAlpha = state.loadingAlpha
        }
        assertEquals(0f, maxLoadingAlpha, 0.001f)
        assertTrue(state.resultsVisible)
        assertEquals(1f, state.contentAlpha, 0.01f)
    }

    @Test
    fun exitKeepsFiltersVisibleUntilResultsFadeOut() = runSkikoComposeUiTest(
        size = Size(360f, 640f),
        density = Density(1f),
    ) {
        var showingResults by mutableStateOf(true)
        var loading by mutableStateOf(false)
        var captured: SearchResultsTransitionState? = null

        setContent {
            COUITheme {
                val transition = rememberSearchResultsTransitionState(
                    showingResults = showingResults,
                    loading = loading,
                    spec = SearchResultContainerDefaults.transitionSpec(
                        modeFadeOutMillis = 120,
                        modeFadeInMillis = 200,
                    ),
                )
                captured = transition
                SearchResultContainer(
                    state = transition,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (!shownResults) {
                        Box(Modifier.fillMaxSize().testTag("prompt_view"))
                    } else if (resultsVisible) {
                        Box(Modifier.fillMaxSize().testTag("results_view"))
                    }
                }
            }
        }

        mainClock.autoAdvance = false
        mainClock.advanceTimeByFrame()
        val state = requireNotNull(captured)
        assertTrue(state.shownResults)
        assertTrue(state.filtersVisible)
        assertTrue(state.resultsVisible)

        runOnIdle { showingResults = false }
        // Mid fade-out (48ms < 120ms): results & filter bar must still be mounted.
        repeat(3) { mainClock.advanceTimeBy(16L) }
        assertTrue(state.filtersVisible, "filters stay visible during fade-out")
        assertTrue(state.shownResults, "results stay mounted during fade-out")
        assertTrue(state.contentAlpha < 1f, "results are fading out: ${state.contentAlpha}")

        // Finish fade-out + fade-in (120 + 200ms).
        repeat(25) { mainClock.advanceTimeBy(16L) }
        assertFalse(state.filtersVisible)
        assertFalse(state.shownResults)
        assertEquals(1f, state.contentAlpha, 0.01f)
    }
}
