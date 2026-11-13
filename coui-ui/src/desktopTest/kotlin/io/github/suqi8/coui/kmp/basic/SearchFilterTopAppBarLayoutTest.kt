// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.LayerBackdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [SearchFilterTopAppBar] stacks its two rows above the recorded material and reports only the
 * rows: the fade band the material paints past the bar's bottom edge must not take layout space,
 * and the search row must sample the composite rather than the page.
 */
@OptIn(ExperimentalTestApi::class)
class SearchFilterTopAppBarLayoutTest {

    private val filters = listOf(
        ChipTabData(label = "Videos"),
        ChipTabData(label = "Users"),
        ChipTabData(label = "Articles"),
    )

    private val rowsHeight = TwoRowTopAppBarDefaults.barHeight(
        firstRowHeight = TwoRowTopAppBarDefaults.FirstRowHeight,
        secondRowHeight = TwoRowTopAppBarDefaults.SecondRowHeight,
    )

    @Test
    fun theBarReportsTheRowsWithoutTheFadeBand() = runSkikoComposeUiTest(
        size = Size(360f, 300f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                SearchFilterTopAppBar(
                    filters = filters,
                    selectedFilter = 0,
                    onFilterSelected = {},
                    modifier = Modifier.fillMaxWidth().testTag("bar"),
                    searchRow = {
                        Box(Modifier.fillMaxWidth().height(40.dp).testTag("search"))
                    },
                )
            }
        }
        waitForIdle()

        val bar = onNodeWithTag("bar").getBoundsInRoot()
        assertEquals(rowsHeight.value, bar.height.value, 0.5f)
        // Nothing sits above the first row on desktop: no status bar to inset the rows by.
        // The 40dp field is centred in the 52dp row.
        assertEquals(6f, onNodeWithTag("search").getBoundsInRoot().top.value, 0.5f)
        assertEquals(40f, onNodeWithTag("search").getBoundsInRoot().height.value, 0.5f)
    }

    @Test
    fun aCustomRowGapMovesTheReportedHeight() = runSkikoComposeUiTest(
        size = Size(360f, 300f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                SearchFilterTopAppBar(
                    filters = filters,
                    modifier = Modifier.fillMaxWidth().testTag("bar"),
                    rowGap = 15.dp,
                    searchRow = {
                        Box(Modifier.fillMaxWidth().height(40.dp))
                    },
                )
            }
        }
        waitForIdle()

        // The material is sized from the same sum: a caller that widens the gap must not leave the
        // fade band starting above the bar's bottom edge.
        assertEquals(
            (
                TwoRowTopAppBarDefaults.FirstRowHeight + 15.dp +
                    TwoRowTopAppBarDefaults.SecondRowHeight +
                    TwoRowTopAppBarDefaults.SecondRowBottom
                ).value,
            onNodeWithTag("bar").getBoundsInRoot().height.value,
            0.5f,
        )
    }

    @Test
    fun noFiltersReportsOnlyTheSearchRow() = runSkikoComposeUiTest(
        size = Size(360f, 300f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                SearchFilterTopAppBar(
                    modifier = Modifier.fillMaxWidth().testTag("bar"),
                    searchRow = {
                        Box(Modifier.fillMaxWidth().height(40.dp).testTag("search"))
                    },
                )
            }
        }
        waitForIdle()

        assertEquals(
            TwoRowTopAppBarDefaults.FirstRowHeight.value,
            onNodeWithTag("bar").getBoundsInRoot().height.value,
            0.5f,
        )
    }

    @Test
    fun theSearchRowSamplesTheCompositeNotThePage() = runSkikoComposeUiTest(
        size = Size(360f, 300f),
        density = Density(1f),
    ) {
        var pageBackdrop: LayerBackdrop? = null
        lateinit var inner: Backdrop
        setContent {
            COUITheme {
                val page = rememberLayerBackdrop()
                pageBackdrop = page
                CompositionLocalProvider(LocalBackdrop provides page) {
                    SearchFilterTopAppBar(
                        filters = filters,
                        selectedFilter = 0,
                        onFilterSelected = {},
                        modifier = Modifier.fillMaxWidth(),
                        searchRow = { inner = LocalBackdrop.current!! },
                    )
                }
            }
        }
        waitForIdle()

        assertTrue(inner is LayerBackdrop, "the search row must sample a recorded layer")
        assertTrue(pageBackdrop !== inner, "the search row must not sample the page directly")
    }
}
