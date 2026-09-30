// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlay
import io.github.suqi8.coui.kmp.basic.BottomFloatingAccessoryNavigationBar
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.InputField
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.icon.extended.Back
import io.github.suqi8.coui.kmp.icon.extended.Home
import io.github.suqi8.coui.kmp.icon.extended.Image
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Theme
import io.github.suqi8.coui.kmp.overlay.OverlayCascadingListPopup
import io.github.suqi8.coui.kmp.theme.COUITheme

/** One capsule tab of the accessory bar. */
private class Coui17Tab(val label: String, val icon: ImageVector)

/** The capsule's three tabs. ColorOS 17 accessory bars label their tabs in Chinese. */
private val Coui17Tabs = listOf(
    Coui17Tab("首页", COUIIcons.Home),
    Coui17Tab("图标", COUIIcons.Image),
    Coui17Tab("配色", COUIIcons.Theme),
)

/**
 * The settings home's app bar: the title band over the search band, with the material overlay.
 *
 * The device's `app_bar_layout` is 343px from the top of the screen — a 141px title band that the
 * status bar sits on top of, and a 202px search band holding a 40dp pill inset 16dp. The overlay
 * reaches 54px (14dp) further down, to 397px.
 *
 * @param scrollFraction The scroll-driven alpha, `min(1, scrollPx / 30dp)`.
 */
@Composable
fun Coui17HomeTopBar(
    title: String,
    searchLabel: String,
    onSearchClick: () -> Unit,
    scrollFraction: Float,
    modifier: Modifier = Modifier,
) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    AppBarBlurOverlay(
        modifier = modifier
            .fillMaxWidth()
            .height(Coui17Dimens.HomeAppBarBottom + Coui17Dimens.AppBarBlurExtra),
        extraHeight = Coui17Dimens.AppBarBlurExtra,
        scrollFraction = scrollFraction,
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(Coui17Dimens.HomeAppBarBottom)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(statusBar),
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(
                        start = Coui17Dimens.BarHorizontalPadding,
                        bottom = 6.dp,
                    ),
                    style = COUITheme.textStyles.title3,
                    color = COUITheme.colorScheme.onSurface,
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(Coui17Dimens.HomeSearchBandHeight),
                contentAlignment = Alignment.Center,
            ) {
                Coui17SearchPill(
                    label = searchLabel,
                    onClick = onSearchClick,
                    scrollFraction = scrollFraction,
                )
            }
        }
    }
}

/**
 * The collapsed search field on the home bar.
 *
 * The field itself is inert — the whole pill is the tap target, exactly like the device, where the
 * `search_src_text` `EditText` carries the click and expands the page into the search view.
 *
 * The pill also casts the shadow the device keeps for the collapsed state; see [searchBarShadow].
 */
@Composable
private fun Coui17SearchPill(label: String, onClick: () -> Unit, scrollFraction: Float) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Coui17Dimens.BarHorizontalPadding)
            .height(Coui17Dimens.SearchPillHeight)
            .dropShadow(shape = CircleShape, shadow = searchBarShadow(scrollFraction))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        InputField(
            query = "",
            onQueryChange = {},
            onSearch = {},
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier.fillMaxSize(),
            label = label,
            enabled = false,
            leadingIcon = {
                Icon(
                    imageVector = COUIIcons.Basic.Search,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .padding(start = 16.dp, end = 8.dp),
                    tint = COUITheme.colorScheme.onSurfaceContainerHigh,
                )
            },
        )
    }
}

/**
 * A second-level page's toolbar: 52.13dp tall, back key 40dp at 16dp from the start edge, title at
 * x=65.8dp.
 *
 * The material band runs from the top of the screen — the device's `toolbar` node sits at y=141px
 * with the material above it — so the toolbar is offset by the status bar inside the overlay.
 */
@Composable
fun Coui17CompactToolbar(
    title: String,
    onBack: (() -> Unit)?,
    scrollFraction: Float,
    modifier: Modifier = Modifier,
) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    AppBarBlurOverlay(
        modifier = modifier
            .fillMaxWidth()
            .height(statusBar + Coui17Dimens.CompactToolbarHeight),
        extraHeight = 0.dp,
        scrollFraction = scrollFraction,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(statusBar))
            Box(modifier = Modifier.fillMaxWidth().height(Coui17Dimens.CompactToolbarHeight)) {
                if (onBack != null) {
                    HighlightedIconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = Coui17Dimens.BarHorizontalPadding),
                    ) {
                        Icon(imageVector = COUIIcons.Back, contentDescription = "返回")
                    }
                }
                Text(
                    text = title,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = Coui17Dimens.ToolbarTitleStart),
                    style = COUITheme.textStyles.title4,
                    color = COUITheme.colorScheme.onSurface,
                )
            }
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
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = description)
    }
}

/**
 * The panel the end ball opens: the list panel COUI uses for its filter menus, carrying the two
 * second-level entries the capsule has no room for.
 */
@Composable
fun Coui17Panel(
    show: Boolean,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val entries = listOf(
        DropdownEntry(
            items = listOf(
                DropdownItem(text = "文字样式", onClick = { onSelect(Coui17Page.TextStyle.ordinal) }),
                DropdownItem(text = "设置项", onClick = { onSelect(Coui17Page.Settings.ordinal) }),
            ),
        ),
    )
    OverlayCascadingListPopup(
        show = show,
        entries = entries,
        onDismissRequest = onDismiss,
    )
}
