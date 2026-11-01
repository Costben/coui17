// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalScrollBarApi::class)

package pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import appstate.LocalAppState
import appstate.LocalIsWideScreen
import appstate.LocalNavigator
import component.BackNavigationIcon
import i18n.str
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.PullToRefresh
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.VerticalScrollBar
import io.github.suqi8.coui.kmp.basic.rememberPullToRefreshState
import io.github.suqi8.coui.kmp.basic.rememberScrollBarAdapter
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.interfaces.ExperimentalScrollBarApi
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.delay
import utils.AdaptiveTopAppBar
import utils.pageContainerColor
import utils.pageContentPadding
import utils.pageScrollModifiers
import utils.rememberBlurBackdrop

private const val ITEM_COUNT = 20
private const val REFRESH_DURATION_MILLIS = 1500L
private val ListTopShape = SquircleShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
private val ListBottomShape = SquircleShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 12.dp, bottomStart = 12.dp)

@Composable
fun PullToRefreshPage(
    padding: PaddingValues,
) {
    val navigator = LocalNavigator.current
    val appState = LocalAppState.current
    val isWideScreen = LocalIsWideScreen.current
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val topAppBarScrollBehavior = COUIScrollBehavior()

    val backdrop = rememberBlurBackdrop()

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(REFRESH_DURATION_MILLIS)
            isRefreshing = false
        }
    }

    Scaffold(
        containerColor = pageContainerColor(),
        topBar = {
            // The bar reads the content layer behind it, so it must be a sibling of that layer,
            // never inside it — LocalBackdrop is provided to the bar slot alone.
            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                AdaptiveTopAppBar(
                    title = str("PullToRefresh Test"),
                    showTopAppBar = appState.showTopAppBar,
                    isWideScreen = isWideScreen,
                    scrollBehavior = topAppBarScrollBehavior,
                    color = pageContainerColor(),
                    navigationIcon = {
                        BackNavigationIcon(
                            onClick = { navigator.pop() },
                        )
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
            PullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true },
                pullToRefreshState = pullToRefreshState,
                topAppBarScrollBehavior = if (appState.showTopAppBar) topAppBarScrollBehavior else null,
                contentPadding = contentPadding,
            ) {
                val lazyListState = rememberLazyListState()
                Box {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.pageScrollModifiers(
                            appState.enableScrollEndHaptic,
                            appState.showTopAppBar,
                            topAppBarScrollBehavior,
                        ),
                        contentPadding = contentPadding,
                    ) {
                        items(
                            count = ITEM_COUNT,
                            key = { "refresh_item_$it" },
                        ) { i ->
                            val isFirst = i == 0
                            val isLast = i == ITEM_COUNT - 1
                            val shape = when {
                                isFirst -> ListTopShape
                                isLast -> ListBottomShape
                                else -> RectangleShape
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(shape)
                                    .background(COUITheme.colorScheme.surfaceContainer),
                            ) {
                                BasicComponent(title = str("Item {}", i + 1))
                                if (!isLast) {
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                    VerticalScrollBar(
                        adapter = rememberScrollBarAdapter(lazyListState),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        trackPadding = contentPadding,
                    )
                }
            }
        }
    }
}
