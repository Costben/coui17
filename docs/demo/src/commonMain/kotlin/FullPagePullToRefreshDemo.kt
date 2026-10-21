// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.FullPagePullToRefresh
import io.github.suqi8.coui.kmp.basic.FullPagePullToRefreshTexts
import io.github.suqi8.coui.kmp.basic.rememberFullPagePullToRefreshState
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import kotlinx.coroutines.delay

@Composable
fun FullPagePullToRefreshDemo() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(demoBackground()),
    ) {
        val refreshState = rememberFullPagePullToRefreshState()
        var isRefreshing by rememberSaveable { mutableStateOf(false) }
        var itemCount by remember { mutableIntStateOf(6) }

        LaunchedEffect(isRefreshing) {
            if (isRefreshing) {
                delay(900)
                itemCount += 3
                isRefreshing = false
            }
        }

        FullPagePullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true },
            state = refreshState,
            contentPadding = PaddingValues(top = 72.dp),
            refreshTexts = FullPagePullToRefreshTexts(
                pulling = "Pull down to refresh resources",
                thresholdReached = "Release to refresh resources",
                refreshing = "Loading resources...",
                complete = "New resources are ready",
            ),
            background = { state ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = (state.pullProgress * 1.5f).coerceIn(0f, 1f)
                        }
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    COUITheme.colorScheme.primaryContainer,
                                    COUITheme.colorScheme.surface,
                                ),
                            ),
                        ),
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .overScrollVertical()
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 72.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                overscrollEffect = null,
            ) {
                items(itemCount) { index ->
                    Card(Modifier.fillMaxWidth()) {
                        BasicComponent(title = "Resource ${index + 1}")
                        BasicComponent(title = "Pull the page to reveal the refresh layer")
                    }
                }
            }
        }
    }
}
