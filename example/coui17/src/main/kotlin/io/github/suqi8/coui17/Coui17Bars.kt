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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import component.searchBarShadow
import i18n.str
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
import io.github.suqi8.coui.kmp.icon.extended.Settings
import io.github.suqi8.coui.kmp.icon.extended.Theme
import io.github.suqi8.coui.kmp.icon.extended.Translate
import io.github.suqi8.coui.kmp.overlay.OverlayCascadingListPopup
import io.github.suqi8.coui.kmp.theme.COUITheme

/** One capsule tab of the accessory bar. [label] is the English source string `str` resolves. */
private class Coui17Tab(val label: String, val icon: ImageVector)

/** The capsule's three tabs, in the order the pages sit in the pager. */
private val Coui17Tabs = listOf(
    Coui17Tab("Home", COUIIcons.Home),
    Coui17Tab("Icons", COUIIcons.Image),
    Coui17Tab("Color", COUIIcons.Theme),
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
    AppBarBlurOverlay(
        modifier = modifier
            .fillMaxWidth()
            .height(Coui17Dimens.HomeAppBarBottom + Coui17Dimens.AppBarBlurExtra),
        extraHeight = Coui17Dimens.AppBarBlurExtra,
        scrollFraction = scrollFraction,
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(Coui17Dimens.HomeAppBarBottom)) {
            // The system Settings home leaves this band clear so its status-bar clock never
            // collides with a large title. Keeping the measured band preserves the search pill's
            // y=164px position while removing the overlapping "设置" text.
            Spacer(modifier = Modifier.height(Coui17Dimens.HomeTitleBandHeight))
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
 * The shadow sits on its own sibling layer with the capsule punched out: the pill's fill is
 * translucent, so a shadow drawn behind it would show through and darken the field, which the
 * device's shadow does not.
 */
@Composable
private fun Coui17SearchPill(label: String, onClick: () -> Unit, scrollFraction: Float) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Coui17Dimens.BarHorizontalPadding)
            .height(Coui17Dimens.SearchPillHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    val radius = size.height / 2f
                    val capsule = Path().apply {
                        addRoundRect(RoundRect(0f, 0f, size.width, size.height, radius, radius))
                    }
                    clipPath(capsule, ClipOp.Difference) { this@drawWithContent.drawContent() }
                }
                .dropShadow(shape = CircleShape, shadow = searchBarShadow(scrollFraction)),
        )
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
                        .size(Coui17Dimens.SearchLeadingIconSlot)
                        .padding(
                            start = Coui17Dimens.SearchLeadingIconIndent,
                            end = Coui17Dimens.SearchLeadingIconEndGap,
                        ),
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
                        Icon(imageVector = COUIIcons.Back, contentDescription = str("Back"))
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
 *
 * [Coui17Panel] is composed inside the end ball rather than beside the bar: the panel is a
 * `CascadingListPopupLayout`, which anchors itself to the window bounds of the layout node it is
 * measured in. Hung off the root box those bounds are the whole screen, no side has room below or
 * above and the menu lands centred in the middle of the display; inside the ball it opens above it,
 * right-aligned, the way the device's three-dot menu does.
 */
@Composable
fun Coui17AccessoryBar(
    currentPage: Int,
    panelOpen: Boolean,
    onSelectPage: (Int) -> Unit,
    onSearch: () -> Unit,
    onOpenPanel: () -> Unit,
    onPanelSelect: (Int) -> Unit,
    onPanelDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val systemDensity = LocalDensity.current
    val galleryDensity = remember(systemDensity) {
        Density(
            density = systemDensity.density * Coui17Dimens.GALLERY_DENSITY_SCALE,
            fontScale = systemDensity.fontScale,
        )
    }
    // Gallery creates this bar in its 565dpi activity while Settings and the device window use
    // 620dpi. Scope that physical scaling to the bar only; the anchored popup is restored below.
    CompositionLocalProvider(LocalDensity provides galleryDensity) {
        BottomFloatingAccessoryNavigationBar(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            startAccessory = {
                Coui17AccessoryBall(
                    icon = COUIIcons.Basic.Search,
                    description = str("Search"),
                    onClick = onSearch,
                )
            },
            endAccessory = {
                Coui17AccessoryBall(
                    icon = COUIIcons.More,
                    description = str("More"),
                    onClick = onOpenPanel,
                )
                CompositionLocalProvider(LocalDensity provides systemDensity) {
                    Coui17Panel(
                        show = panelOpen,
                        onSelect = onPanelSelect,
                        onDismiss = onPanelDismiss,
                    )
                }
            },
        ) {
            Coui17Tabs.forEachIndexed { index, tab ->
                BottomFloatingNavigationBarItem(
                    selected = currentPage == index,
                    onClick = { onSelectPage(index) },
                    icon = tab.icon,
                    label = str(tab.label),
                )
            }
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
    val iconTint = COUITheme.colorScheme.onSurfaceContainer
    val entries = listOf(
        DropdownEntry(
            items = listOf(
                DropdownItem(
                    text = str("Text style"),
                    icon = { modifier ->
                        Icon(
                            modifier = modifier,
                            imageVector = COUIIcons.Translate,
                            tint = iconTint,
                            contentDescription = null,
                        )
                    },
                    onClick = { onSelect(Coui17Page.TextStyle.ordinal) },
                ),
                DropdownItem(
                    text = str("Settings items"),
                    icon = { modifier ->
                        Icon(
                            modifier = modifier,
                            imageVector = COUIIcons.Settings,
                            tint = iconTint,
                            contentDescription = null,
                        )
                    },
                    onClick = { onSelect(Coui17Page.Settings.ordinal) },
                ),
            ),
        ),
    )
    OverlayCascadingListPopup(
        show = show,
        entries = entries,
        onDismissRequest = onDismiss,
    )
}
