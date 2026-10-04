// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import component.searchBarShadow
import i18n.str
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlay
import io.github.suqi8.coui.kmp.basic.BottomFloatingAccessoryNavigationBar
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarDefaults
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.DropdownImpl
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.InputField
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.basic.PopupPositionProvider
import io.github.suqi8.coui.kmp.basic.ScrollBehavior
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TopAppBar
import io.github.suqi8.coui.kmp.basic.couiTopAppBarActionItem
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.icon.extended.Back
import io.github.suqi8.coui.kmp.icon.extended.Home
import io.github.suqi8.coui.kmp.icon.extended.Image
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Settings
import io.github.suqi8.coui.kmp.icon.extended.Theme
import io.github.suqi8.coui.kmp.icon.extended.Translate
import io.github.suqi8.coui.kmp.overlay.OverlayListPopup
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
 * The shared ColorOS 17 page chrome. The library's [TopAppBar] owns the native large-title
 * transition, so every page follows the same 32dp expanded title -> 24dp toolbar title motion.
 * Search and overflow actions stay in the 40dp toolbar slots throughout the transition.
 */
@Composable
fun Coui17TopAppBar(
    title: String,
    searchLabel: String,
    showSearchField: Boolean,
    showBack: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    panelOpen: Boolean,
    onOpenPanel: () -> Unit,
    onPanelSelect: (Int) -> Unit,
    onPanelDismiss: () -> Unit,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = title,
        largeTitle = title,
        modifier = modifier.fillMaxWidth(),
        scrollBehavior = scrollBehavior,
        gradientGlass = true,
        defaultWindowInsetsPadding = true,
        // ColorOS 17's home bar carries no hairline: the material ramps out on its own, and the
        // library's line would otherwise fade in under the search pill as soon as the page scrolls.
        showDivider = false,
        navigationIcon = {
            // Only a panel page has somewhere to go back to; the capsule pages would otherwise show
            // a dead key. Dropping it also moves the collapsed title to the 16dp start edge, which is
            // what the device's keyless toolbars do.
            if (showBack) {
                HighlightedIconButton(
                    onClick = onBack,
                    modifier = Modifier.couiTopAppBarActionItem(),
                ) {
                    Icon(imageVector = COUIIcons.Back, contentDescription = str("Back"))
                }
            }
        },
        actions = {
            HighlightedIconButton(
                onClick = onSearch,
                modifier = Modifier.couiTopAppBarActionItem(),
            ) {
                Icon(imageVector = COUIIcons.Basic.Search, contentDescription = str("Search"))
            }
            Box {
                HighlightedIconButton(
                    onClick = onOpenPanel,
                    modifier = Modifier.couiTopAppBarActionItem(),
                ) {
                    Icon(imageVector = COUIIcons.More, contentDescription = str("More"))
                }
                Coui17Panel(
                    show = panelOpen,
                    onSelect = onPanelSelect,
                    onDismiss = onPanelDismiss,
                )
            }
        },
        bottomContent = {
            if (showSearchField) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Coui17Dimens.HomeSearchBandHeight),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Coui17SearchPill(
                            label = searchLabel,
                            onClick = onSearch,
                            scrollFraction = scrollBehavior.state.collapsedFraction,
                        )
                    }
                    // The device's `blur_overlay` runs `homepage_appbar_blur_extra_height` past
                    // `app_bar_layout`'s bottom edge. That band is empty material, but it is also the
                    // room the collapsed pill's shadow falls into: without it the bar's own layer ends
                    // a few px under the pill and the shadow is cut off flat along the bar's bottom
                    // edge instead of fading out.
                    Spacer(modifier = Modifier.height(Coui17Dimens.AppBarBlurExtra))
                }
            }
        },
    )
}

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
 * Both accessories retract into the capsule while the `配色` tab is the current page — the state
 * Gallery reaches by hiding its two side views on the `创作` tab — and spring back out on the other
 * two, leaving the capsule alone in the middle either way.
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
    val windowSize = LocalWindowInfo.current.containerSize
    val galleryDensity = remember(systemDensity, windowSize) {
        Density(
            density = Coui17Dimens.galleryDensity(minOf(windowSize.width, windowSize.height)),
            fontScale = systemDensity.fontScale,
        )
    }
    // Gallery retracts both accessories once the third capsule tab is selected: `TabFragment`'s page
    // listener calls `hideStartAccessoryView(true)` / `hideEndAccessoryView(true)` for the 创作 tab and
    // shows them again for 照片 and 图集, so 创作 leaves a bare three-item capsule. `配色` is this bar's
    // third tab, so it takes the same state; the capsule keeps its items and stays centred, because the
    // reserve is computed from the *configured* accessory icons, not from the active ones
    // (`computeSideReserveForConfiguredIcons`).
    val accessoriesVisible = Coui17Page.entries[currentPage] != Coui17Page.Color
    // Gallery uses a 360dp short edge (540dpi on the 15T, 565dpi on the earlier reference).
    // Scope that density to the bar; the anchored popup uses the device density below.
    CompositionLocalProvider(LocalDensity provides galleryDensity) {
        BottomFloatingAccessoryNavigationBar(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            startAccessoryVisible = accessoriesVisible,
            endAccessoryVisible = accessoriesVisible,
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
    OverlayListPopup(
        show = show,
        alignment = PopupPositionProvider.Align.End,
        onDismissRequest = onDismiss,
    ) {
        ListPopupColumn {
            DropdownImpl(
                item = DropdownItem(
                    text = str("Text style"),
                    icon = { Icon(imageVector = COUIIcons.Translate, contentDescription = null, modifier = it) },
                ),
                optionSize = 2,
                isSelected = false,
                index = 0,
                groupKey = "coui17-panel",
                role = Role.Button,
                onSelectedIndexChange = { onSelect(Coui17Page.TextStyle.ordinal) },
            )
            DropdownImpl(
                item = DropdownItem(
                    text = str("Settings items"),
                    icon = { Icon(imageVector = COUIIcons.Settings, contentDescription = null, modifier = it) },
                ),
                optionSize = 2,
                isSelected = false,
                index = 1,
                groupKey = "coui17-panel",
                role = Role.Button,
                onSelectedIndexChange = { onSelect(Coui17Page.Settings.ordinal) },
            )
        }
    }
}
