// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.ChipTabData
import io.github.suqi8.coui.kmp.basic.SearchFilterTopAppBar
import io.github.suqi8.coui.kmp.basic.SearchResultContainer
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TopAppBarSearchBar
import io.github.suqi8.coui.kmp.basic.TwoRowTopAppBarDefaults
import io.github.suqi8.coui.kmp.basic.rememberSearchResultsTransitionState
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.delay

private val searchResultDemoTabs = listOf(
    ChipTabData(label = "All"),
    ChipTabData(label = "Display"),
    ChipTabData(label = "Sound"),
)

/**
 * Interactive preview for [SearchResultContainer] paired with [SearchFilterTopAppBar]:
 * submits a simulated query so the loading spinner dissolves cleanly into results rising from below,
 * and clears back to the prompt with a synchronized filter collapse and fade.
 */
@Composable
fun SearchResultContainerDemo() {
    var query by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var requestToken by remember { mutableIntStateOf(0) }

    val showingResults = submitted.isNotBlank() && query.trim() == submitted
    val transitionState = rememberSearchResultsTransitionState(
        showingResults = showingResults,
        loading = loading,
        queryKey = submitted,
    )

    LaunchedEffect(requestToken) {
        if (requestToken == 0) return@LaunchedEffect
        loading = true
        delay(260)
        loading = false
    }

    val barTop = if (transitionState.shownResults) {
        TwoRowTopAppBarDefaults.barHeight()
    } else {
        TwoRowTopAppBarDefaults.FirstRowHeight + 12.dp
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val next = if (query.isBlank()) "Wallpaper" else query.trim()
                    query = next
                    submitted = next
                    requestToken++
                },
            ) {
                Text("Search")
            }
            Button(
                onClick = {
                    query = ""
                    submitted = ""
                    loading = false
                },
            ) {
                Text("Clear")
            }
        }
        Column(modifier = Modifier.fillMaxWidth().height(320.dp)) {
            SearchFilterTopAppBar(
                filters = if (transitionState.filtersVisible) searchResultDemoTabs else null,
                selectedFilter = selectedTab,
                onFilterSelected = { index ->
                    selectedTab = index
                    if (showingResults) requestToken++
                },
                modifier = Modifier.fillMaxWidth(),
                searchRow = {
                    TopAppBarSearchBar(
                        query = query,
                        onQueryChange = {
                            query = it
                            if (it.trim() != submitted) loading = false
                        },
                        expanded = true,
                        onExpandedChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Search settings",
                        cancelText = null,
                        onSearch = { raw ->
                            val keyword = raw.trim()
                            if (keyword.isNotEmpty()) {
                                query = keyword
                                submitted = keyword
                                requestToken++
                            }
                        },
                    )
                },
            )
            SearchResultContainer(
                state = transitionState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                loadingTopPadding = 12.dp,
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!shownResults) {
                        items(4) { index ->
                            Text(
                                text = "Recent item ${index + 1}",
                                color = COUITheme.colorScheme.onSurfaceSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                            )
                        }
                    } else if (resultsVisible) {
                        items(6) { index ->
                            Text(
                                text = "${searchResultDemoTabs[selectedTab].label}: $submitted #${index + 1}",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = "topReserve=${barTop.value.toInt()}dp",
            color = COUITheme.colorScheme.onSurfaceSecondary,
        )
    }
}
