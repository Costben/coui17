// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalScrollBarApi::class)

package pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import appstate.LocalAppState
import appstate.LocalIsWideScreen
import appstate.LocalNavigator
import component.BackNavigationIcon
import i18n.str
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.FullPagePullToRefresh
import io.github.suqi8.coui.kmp.basic.FullPagePullToRefreshTexts
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.VerticalScrollBar
import io.github.suqi8.coui.kmp.basic.rememberFullPagePullToRefreshState
import io.github.suqi8.coui.kmp.basic.rememberScrollBarAdapter
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.interfaces.ExperimentalScrollBarApi
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.delay
import utils.AdaptiveTopAppBar
import utils.pageContainerColor
import utils.pageContentPadding
import utils.pageScrollModifiers
import utils.rememberBlurBackdrop

@Composable
fun FullPagePullToRefreshPage(
    padding: PaddingValues,
) {
    val navigator = LocalNavigator.current
    val appState = LocalAppState.current
    val isWideScreen = LocalIsWideScreen.current
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    var itemCount by remember { mutableIntStateOf(12) }
    val refreshState = rememberFullPagePullToRefreshState()
    val topAppBarScrollBehavior = COUIScrollBehavior()

    val backdrop = rememberBlurBackdrop()

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(1200)
            itemCount += 4
            isRefreshing = false
        }
    }

    Scaffold(
        containerColor = pageContainerColor(),
        topBar = {
            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                AdaptiveTopAppBar(
                    title = str("Full Page PullToRefresh Test"),
                    showTopAppBar = appState.showTopAppBar,
                    isWideScreen = isWideScreen,
                    scrollBehavior = topAppBarScrollBehavior,
                    color = pageContainerColor(),
                    navigationIcon = {
                        BackNavigationIcon(onClick = { navigator.pop() })
                    },
                )
            }
        },
    ) { innerPadding ->
        val contentPadding = pageContentPadding(
            innerPadding,
            padding,
            true,
            extraTop = 12.dp,
            extraStart = WindowInsets.displayCutout.asPaddingValues().calculateLeftPadding(LayoutDirection.Ltr),
            extraEnd = WindowInsets.displayCutout.asPaddingValues().calculateRightPadding(LayoutDirection.Ltr),
            extraBottom = 12.dp,
        )
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            FullPagePullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true },
                state = refreshState,
                contentPadding = contentPadding,
                topAppBarScrollBehavior = if (appState.showTopAppBar) topAppBarScrollBehavior else null,
                refreshTexts = FullPagePullToRefreshTexts(
                    pulling = str("Pull down to refresh resources"),
                    thresholdReached = str("Release to refresh resources"),
                    refreshing = str("Loading resources..."),
                    complete = str("New resources are ready"),
                ),
                background = { state ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = (state.pullProgress * 1.4f).coerceIn(0f, 1f)
                            }
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        COUITheme.colorScheme.primaryContainer,
                                        pageContainerColor(),
                                    ),
                                ),
                            ),
                    )
                },
            ) {
                val lazyListState = rememberLazyListState()
                Box {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .pageScrollModifiers(
                                appState.enableScrollEndHaptic,
                                appState.showTopAppBar,
                                topAppBarScrollBehavior,
                            ),
                        contentPadding = contentPadding,
                    ) {
                        items(count = itemCount, key = { "full_page_refresh_$it" }) { index ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                            ) {
                                BasicComponent(title = str("Resource") + " ${index + 1}")
                                BasicComponent(title = "Pull down to reveal the layer")
                            }
                        }
                    }
                    VerticalScrollBar(
                        adapter = rememberScrollBarAdapter(lazyListState),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        trackPadding = contentPadding,
                    )
                }
            }
        }
    }
}
