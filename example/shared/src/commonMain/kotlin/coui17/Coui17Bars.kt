// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package coui17

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlay
import io.github.suqi8.coui.kmp.basic.BottomFloatingAccessoryNavigationBar
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarDefaults
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.DropdownImpl
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.basic.PopupPositionProvider
import io.github.suqi8.coui.kmp.basic.ScrollBehavior
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TopAppBarSearchBar
import io.github.suqi8.coui.kmp.basic.couiTopAppBarActionItem
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.icon.extended.Back
import io.github.suqi8.coui.kmp.icon.extended.Layers
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Settings
import io.github.suqi8.coui.kmp.icon.extended.Translate
import io.github.suqi8.coui.kmp.overlay.OverlayListPopup
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.rememberGalleryDensity

/** One capsule tab of the accessory bar. [label] is the English source string `str` resolves. */
private class Coui17Tab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

/**
 * The capsule's three tabs, in the order the pages sit in the pager.
 *
 * The pair of glyphs is the set's line and filled form: the bar draws the line on an idle tab and
 * swaps in the filled one on selection, which is the only place ColorOS 17 uses the filled weight
 * in a bottom bar.
 */
private val Coui17Tabs = listOf(
    Coui17Tab("Home", Coui17Icons.HomeLine, Coui17Icons.HomeFill),
    Coui17Tab("Icons", Coui17Icons.GridLine, Coui17Icons.GridFill),
    Coui17Tab("Color", Coui17Icons.PaletteLine, Coui17Icons.PaletteFill),
)

/**
 * The capsule pages' chrome. The collapsible [TopAppBarSearchBar] owns the native large-title
 * transition (32dp expanded title -> 24dp toolbar title) and expands the trailing circular search
 * button directly inside the toolbar row with a split "Cancel" capsule.
 *
 * The large title belongs to the keyless top level alone: a page with a back key is a second-level
 * page and wears [Coui17SecondaryTopAppBar] instead.
 */
@Composable
fun Coui17TopAppBar(
    title: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    searchPlaceholder: String,
    panelOpen: Boolean,
    onOpenPanel: () -> Unit,
    onPanelSelect: (Coui17PanelPage) -> Unit,
    onPanelDismiss: () -> Unit,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
    showSearchButton: Boolean = true,
) {
    val clearTint = COUITheme.colorScheme.onSurface.copy(alpha = 0.28f)
    val clearLabel = str("Clear")
    TopAppBarSearchBar(
        query = searchQuery,
        onQueryChange = onSearchQueryChange,
        expanded = searchExpanded,
        onExpandedChange = onSearchExpandedChange,
        modifier = modifier.fillMaxWidth(),
        title = title,
        largeTitle = title,
        placeholder = searchPlaceholder,
        cancelText = str("Cancel"),
        onCancel = {
            onSearchQueryChange("")
            onSearchExpandedChange(false)
        },
        showSearchButton = showSearchButton,
        searchButtonContentDescription = str("Search"),
        scrollBehavior = scrollBehavior,
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                Canvas(
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = clearLabel }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = { onSearchQueryChange("") },
                        ),
                ) {
                    val unit = size.width / 36f
                    drawCircle(clearTint, radius = 9f * unit)
                    val path = Path().apply {
                        moveTo(18f, 16.868f)
                        lineTo(15.031f, 13.898f)
                        lineTo(13.899f, 15.03f)
                        lineTo(16.869f, 18f)
                        lineTo(13.898f, 20.97f)
                        lineTo(15.03f, 22.101f)
                        lineTo(18f, 19.131f)
                        lineTo(20.97f, 22.101f)
                        lineTo(22.102f, 20.969f)
                        lineTo(19.132f, 18f)
                        lineTo(22.101f, 15.03f)
                        lineTo(20.969f, 13.899f)
                        close()
                    }
                    withTransform({ scale(unit, unit, Offset.Zero) }) {
                        drawPath(path, Color.White)
                    }
                }
            }
        },
        extraActions = {
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
    )
}

/**
 * A second-level page's chrome: the back key, an 18dp title right after it and the overflow key on
 * the gradient-blur material.
 *
 * A toolbar with keys on both sides sets its title in `TextAppearance.COUI.Toolbar.SecondTitle`,
 * start-aligned against the back key, and never grows a large title, so the bar stays pinned at the
 * 52dp toolbar row however far the page scrolls.
 */
@Composable
fun Coui17SecondaryTopAppBar(
    title: String,
    onBack: () -> Unit,
    panelOpen: Boolean,
    onOpenPanel: () -> Unit,
    onPanelSelect: (Coui17PanelPage) -> Unit,
    onPanelDismiss: () -> Unit,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
) {
    SmallTopAppBar(
        title = title,
        modifier = modifier.fillMaxWidth(),
        scrollBehavior = scrollBehavior,
        showDivider = false,
        gradientGlass = true,
        navigationIcon = {
            HighlightedIconButton(
                onClick = onBack,
                modifier = Modifier.couiTopAppBarActionItem(),
            ) {
                Icon(imageVector = COUIIcons.Back, contentDescription = str("Back"))
            }
        },
        actions = {
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
    )
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
    onPanelSelect: (Coui17PanelPage) -> Unit,
    onPanelDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val systemDensity = LocalDensity.current
    // The density Gallery pins itself to, whatever display size the user picked.
    val galleryDensity = rememberGalleryDensity()
    // Gallery retracts both accessories once the third capsule tab is selected: `TabFragment`'s page
    // listener calls `hideStartAccessoryView(true)` / `hideEndAccessoryView(true)` for the 创作 tab and
    // shows them again for 照片 and 图集, so 创作 leaves a bare three-item capsule. `配色` is this bar's
    // third tab, so it takes the same state; the capsule keeps its items and stays centred, because the
    // reserve is computed from the *configured* accessory icons, not from the active ones
    // (`computeSideReserveForConfiguredIcons`).
    val accessoriesVisible = Coui17Page.entries[currentPage] != Coui17Page.Color
    // Gallery pins its own density (565dpi on the PLK110, the panel's 560dpi on the PKT110).
    // Scope that density to the bar; the anchored popup uses the device density below.
    CompositionLocalProvider(LocalDensity provides galleryDensity) {
        BottomFloatingAccessoryNavigationBar(
            // Gallery floats the bar over the navigation handle instead of above its inset: the
            // host pads 8dp under gesture navigation and 36dp under three-button navigation.
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = BottomFloatingNavigationBarDefaults.navigationBarPadding()),
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
                    selectedIcon = tab.selectedIcon,
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
 * The panel the overflow keys open: the list panel COUI uses for its filter menus, carrying the two
 * second-level entries the capsule has no room for.
 */
@Composable
fun Coui17Panel(
    show: Boolean,
    onSelect: (Coui17PanelPage) -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayListPopup(
        show = show,
        alignment = PopupPositionProvider.Align.End,
        onDismissRequest = onDismiss,
    ) {
        ListPopupColumn {
            Coui17PanelPage.entries.forEachIndexed { index, page ->
                DropdownImpl(
                    item = DropdownItem(
                        text = str(page.title),
                        icon = {
                            Icon(
                                imageVector = when (page) {
                                    Coui17PanelPage.TextStyle -> COUIIcons.Translate
                                    Coui17PanelPage.Settings -> COUIIcons.Settings
                                    Coui17PanelPage.SystemAlignment -> COUIIcons.Layers
                                },
                                contentDescription = null,
                                modifier = it,
                            )
                        },
                    ),
                    optionSize = Coui17PanelPage.entries.size,
                    isSelected = false,
                    index = index,
                    groupKey = "coui17-panel",
                    role = Role.Button,
                    onSelectedIndexChange = { onSelect(page) },
                )
            }
        }
    }
}
