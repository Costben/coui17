// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlay
import io.github.suqi8.coui.kmp.basic.BottomFloatingAccessoryNavigationBar
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.NavigationPopupMenu
import io.github.suqi8.coui.kmp.basic.NavigationPopupMenuItem
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.icon.extended.Back
import io.github.suqi8.coui.kmp.icon.extended.Home
import io.github.suqi8.coui.kmp.icon.extended.Image
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Settings
import io.github.suqi8.coui.kmp.icon.extended.Theme
import io.github.suqi8.coui.kmp.theme.COUITheme

/** One capsule tab of the accessory bar. */
private class Coui17Tab(val label: String, val icon: ImageVector)

/** The capsule's three tabs. ColorOS 17 accessory bars label their tabs in Chinese. */
private val Coui17Tabs = listOf(
    Coui17Tab("首页", COUIIcons.Home),
    Coui17Tab("图标", COUIIcons.Image),
    Coui17Tab("配色", COUIIcons.Theme),
)

/** The large ColorOS page title that collapses into the compact toolbar on upward scroll. */
@Composable
fun Coui17PageTopBar(
    title: String,
    collapseFraction: Float,
    scrollFraction: Float,
    onBack: () -> Unit,
    onSearch: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val collapsedHeight = statusBar + Coui17Dimens.CollapsedHeaderHeight
    val headerHeight = lerp(
        Coui17Dimens.ExpandedHeaderHeight,
        collapsedHeight,
        collapseFraction.coerceIn(0f, 1f),
    )
    AppBarBlurOverlay(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight + Coui17Dimens.AppBarBlurExtra),
        extraHeight = Coui17Dimens.AppBarBlurExtra,
        scrollFraction = scrollFraction,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .clip(RectangleShape),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBar)
                    .height(Coui17Dimens.CollapsedHeaderHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HighlightedIconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(start = Coui17Dimens.BarHorizontalPadding),
                ) {
                    Icon(imageVector = COUIIcons.Back, contentDescription = "返回")
                }
                Text(
                    text = title,
                    modifier = Modifier
                        .padding(
                            start = Coui17Dimens.ToolbarTitleStart -
                                Coui17Dimens.BarHorizontalPadding - Coui17Dimens.BackButtonSize,
                        )
                        .graphicsLayer { alpha = collapseFraction },
                    style = COUITheme.textStyles.title4,
                    color = COUITheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Coui17Dimens.BarHorizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onSearch != null) {
                        HighlightedIconButton(onClick = onSearch) {
                            Icon(imageVector = COUIIcons.Basic.Search, contentDescription = "搜索")
                        }
                    }
                    if (onMore != null) {
                        HighlightedIconButton(onClick = onMore) {
                            Icon(imageVector = COUIIcons.More, contentDescription = "更多")
                        }
                    }
                }
            }
            Text(
                text = title,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = Coui17Dimens.BarHorizontalPadding,
                        top = statusBar + Coui17Dimens.CollapsedHeaderHeight + 8.dp,
                    )
                    .graphicsLayer {
                        alpha = 1f - collapseFraction
                        translationY = -collapseFraction * 22.dp.toPx()
                    },
                style = COUITheme.textStyles.title2,
                color = COUITheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * The accessory bar: a three-item capsule flanked by a search ball and a panel ball.
 *
 * The merged blob already carries each accessory's circle, so the slots draw the icon alone; the tap
 * target is the whole 56dp accessory square.
 */
@Composable
fun Coui17AccessoryBar(
    currentPage: Int,
    onSelectPage: (Int) -> Unit,
    onSearch: () -> Unit,
    onOpenPanel: () -> Unit,
    onClosePanel: () -> Unit,
    panelOpen: Boolean,
    onSelectPanel: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomFloatingAccessoryNavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        startAccessory = {
            Coui17AccessoryBall(
                icon = COUIIcons.Basic.Search,
                description = "搜索",
                onClick = onSearch,
            )
        },
        endAccessory = {
            Coui17AccessoryBall(
                icon = COUIIcons.More,
                description = "更多",
                onClick = onOpenPanel,
                menuOpen = panelOpen,
                onDismissMenu = onClosePanel,
                onSelectPanel = onSelectPanel,
            )
        },
    ) {
        Coui17Tabs.forEachIndexed { index, tab ->
            BottomFloatingNavigationBarItem(
                selected = currentPage == index,
                onClick = { onSelectPage(index) },
                icon = tab.icon,
                label = tab.label,
            )
        }
    }
}

@Composable
private fun Coui17AccessoryBall(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    menuOpen: Boolean = false,
    onDismissMenu: () -> Unit = {},
    onSelectPanel: (Int) -> Unit = {},
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        HighlightedIconButton(onClick = onClick, size = 56.dp) {
            Icon(imageVector = icon, contentDescription = description)
        }
        if (menuOpen) {
            NavigationPopupMenu(
                expanded = true,
                onDismissRequest = onDismissMenu,
            ) {
                NavigationPopupMenuItem(
                    label = "文字样式",
                    icon = COUIIcons.Theme,
                    onClick = { onSelectPanel(Coui17Page.TextStyle.ordinal) },
                )
                NavigationPopupMenuItem(
                    label = "设置",
                    icon = COUIIcons.Settings,
                    onClick = { onSelectPanel(Coui17Page.Settings.ordinal) },
                )
            }
        }
    }
}
