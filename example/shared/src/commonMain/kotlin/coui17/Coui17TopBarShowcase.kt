// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package coui17

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import appstate.LocalNavigator
import i18n.str
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlayDefaults
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.ChipTabData
import io.github.suqi8.coui.kmp.basic.ChipTabGroup
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.PinnedScrollBehavior
import io.github.suqi8.coui.kmp.basic.PinnedTopAppBar
import io.github.suqi8.coui.kmp.basic.PinnedTopAppBarDefaults
import io.github.suqi8.coui.kmp.basic.PinnedTopAppBarTextAction
import io.github.suqi8.coui.kmp.basic.ScrollBehavior
import io.github.suqi8.coui.kmp.basic.SearchBar
import io.github.suqi8.coui.kmp.basic.SearchFilterTopAppBar
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar
import io.github.suqi8.coui.kmp.basic.SnackbarHostState
import io.github.suqi8.coui.kmp.basic.TabRowWithContour
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.ToolbarTextButton
import io.github.suqi8.coui.kmp.basic.TopAppBar
import io.github.suqi8.coui.kmp.basic.TopAppBarDefaults
import io.github.suqi8.coui.kmp.basic.TopAppBarSearchBar
import io.github.suqi8.coui.kmp.basic.TopAppBarSearchState
import io.github.suqi8.coui.kmp.basic.TwoRowTopAppBar
import io.github.suqi8.coui.kmp.basic.TwoRowTopAppBarDefaults
import io.github.suqi8.coui.kmp.basic.cardListPositionOf
import io.github.suqi8.coui.kmp.basic.couiGlassHeader
import io.github.suqi8.coui.kmp.basic.couiTopAppBarActionItem
import io.github.suqi8.coui.kmp.basic.rememberTopAppBarSearchState
import io.github.suqi8.coui.kmp.basic.rememberTopAppBarState
import io.github.suqi8.coui.kmp.basic.topAppBarSearchContent
import io.github.suqi8.coui.kmp.basic.topAppBarSearchResults
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.icon.extended.Back
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Sort
import io.github.suqi8.coui.kmp.preference.OverlayDropdownPreference
import io.github.suqi8.coui.kmp.preference.SwitchPreference
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.blur_test_bg
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import io.github.suqi8.coui.kmp.utils.scrollEndHaptic
import org.jetbrains.compose.resources.painterResource
import utils.WallpaperState

/**
 * Every COUI 17 top bar style offered by the dedicated "Top bar" home section, combining this
 * fork's ColorOS 17 bars with upstream COUI 17 variants.
 */
private enum class Coui17TopBarStyle(val label: String) {
    LargeCollapsing("Large collapsing (TopAppBar)"),
    SettingsHome("Settings home (40sp)"),
    FadeLargeTitle("Fade large title"),
    SmallToolbar("Small toolbar (SmallTopAppBar)"),
    CenterTitle("Center title"),
    SegmentToolbar("Segment toolbar"),
    PinnedLargeTitle("Pinned large title (PinnedTopAppBar)"),
    ToolbarSearch("Toolbar search (TopAppBarSearchBar)"),
    TwoRowBar("Two-row bar (TwoRowTopAppBar)"),
    SearchFilterBar("Search filter bar (SearchFilterTopAppBar)"),
    GlassHeader("Glass header (couiGlassHeader)"),
    ;

    val isCollapsibleTopAppBar: Boolean
        get() = this == LargeCollapsing || this == SettingsHome || this == FadeLargeTitle

    val supportsSurfaceMode: Boolean
        get() = isCollapsibleTopAppBar || this == SmallToolbar || this == CenterTitle || this == SegmentToolbar

    val supportsSubtitle: Boolean
        get() = isCollapsibleTopAppBar || this == SmallToolbar || this == CenterTitle

    val supportsNavigationIcon: Boolean
        get() = supportsSurfaceMode || this == PinnedLargeTitle || this == GlassHeader

    val supportsActions: Boolean
        get() = supportsSurfaceMode || this == PinnedLargeTitle

    val supportsTitleOptions: Boolean
        get() = supportsSurfaceMode || this == PinnedLargeTitle || this == ToolbarSearch || this == GlassHeader
}

private enum class TopBarSurfaceMode(val label: String) {
    GradientGlass("Gradient glass"),
    UniformGlass("Uniform glass"),
    Solid("Solid"),
}

private enum class TopBarActionStyle(val label: String) {
    Icons("Icon buttons"),
    RedDot("Icons with red dot"),
    TextButton("Text button"),
}

private sealed interface TopBarControlItem {
    class Dropdown(
        val title: String,
        val items: List<String>,
        val selectedIndex: Int,
        val onSelected: (Int) -> Unit,
    ) : TopBarControlItem

    class Toggle(
        val title: String,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
    ) : TopBarControlItem
}

private val ShowcaseSampleColors = listOf(
    Color(0xFF1A8CFF),
    Color(0xFF24B232),
    Color(0xFFF08222),
    Color(0xFFE5A100),
    Color(0xFFEB493D),
    Color(0xFF7B61FF),
)

private const val SHOWCASE_ITEM_COUNT = 30

/**
 * Full-screen interactive showcase for every COUI 17 top bar style.
 *
 * The active screen top bar itself switches live via the style dropdown, while switches and
 * secondary dropdowns configure glass material, linked in-place search, subtitle collapse,
 * red-dot / text actions, title accent / overflow, and wallpaper backdrop in place.
 */
@Composable
internal fun Coui17TopBarShowcaseScreen() {
    val navigator = LocalNavigator.current
    val snackbarHostState = remember { SnackbarHostState() }
    val density = LocalDensity.current

    var styleIndex by rememberSaveable { mutableIntStateOf(0) }
    var surfaceModeIndex by rememberSaveable { mutableIntStateOf(0) }
    var actionStyleIndex by rememberSaveable { mutableIntStateOf(0) }
    var showSubtitle by rememberSaveable { mutableStateOf(false) }
    var hideSubtitleOnCollapse by rememberSaveable { mutableStateOf(true) }
    var showLinkedSearch by rememberSaveable { mutableStateOf(false) }
    var showBackButton by rememberSaveable { mutableStateOf(true) }
    var showActions by rememberSaveable { mutableStateOf(true) }
    var accentTitle by rememberSaveable { mutableStateOf(false) }
    var longTitle by rememberSaveable { mutableStateOf(false) }
    var wallpaperBackdrop by rememberSaveable { mutableStateOf(false) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedSegment by rememberSaveable { mutableIntStateOf(0) }
    var selectedFilter by rememberSaveable { mutableIntStateOf(0) }
    var measuredBarHeightPx by remember { mutableIntStateOf(0) }

    val style = Coui17TopBarStyle.entries[styleIndex.coerceIn(0, Coui17TopBarStyle.entries.lastIndex)]
    val surfaceMode = TopBarSurfaceMode.entries[surfaceModeIndex]
    val actionStyle = TopBarActionStyle.entries[actionStyleIndex]

    // Reset per-style transient states whenever the style changes so each bar starts cleanly.
    val scrollBehavior = key(style) {
        if (style == Coui17TopBarStyle.PinnedLargeTitle) {
            PinnedScrollBehavior(state = rememberTopAppBarState())
        } else {
            COUIScrollBehavior(state = rememberTopAppBarState())
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(style) {
        searchExpanded = false
        searchQuery = ""
        measuredBarHeightPx = 0
        listState.scrollToItem(0)
    }

    val linkedSearchEnabled = style.isCollapsibleTopAppBar && showLinkedSearch
    val topBarSearchState = rememberTopAppBarSearchState(active = linkedSearchEnabled && searchExpanded)

    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val fallbackContentTop = defaultShowcaseContentTop(
        style = style,
        statusBar = statusBar,
        showSubtitle = showSubtitle,
        showLinkedSearch = linkedSearchEnabled,
    )
    // Collapsing bars drive Coui17PageContent via heightOffset, so use their expanded top height;
    // static / responsive bars (such as TabWithSearchLayout) follow their measured height directly.
    val contentTop = if (
        style.isCollapsibleTopAppBar || style == Coui17TopBarStyle.ToolbarSearch
    ) {
        fallbackContentTop
    } else if (measuredBarHeightPx > 0) {
        with(density) { measuredBarHeightPx.toDp() }
    } else {
        fallbackContentTop
    }

    val filterTabs = listOf(
        ChipTabData(label = str("All")),
        ChipTabData(label = str("Photos")),
        ChipTabData(label = str("Videos")),
        ChipTabData(label = str("Documents")),
    )

    val filteredItems = remember(searchQuery, selectedFilter, style) {
        val needle = searchQuery.trim()
        val categoryBucket = when (style) {
            Coui17TopBarStyle.TwoRowBar, Coui17TopBarStyle.SearchFilterBar -> selectedFilter
            else -> 0
        }
        (1..SHOWCASE_ITEM_COUNT).filter { number ->
            val matchesQuery = needle.isEmpty() || number.toString().contains(needle)
            val matchesCategory = categoryBucket == 0 || ((number - 1) % 3) + 1 == categoryBucket
            matchesQuery && matchesCategory
        }
    }

    val titleText = if (longTitle) {
        str("Long Long Long Long Long Title")
    } else {
        str(
            when (style) {
                Coui17TopBarStyle.SettingsHome -> "Settings"
                Coui17TopBarStyle.PinnedLargeTitle -> "Photos"
                Coui17TopBarStyle.CenterTitle -> "Center title"
                Coui17TopBarStyle.SegmentToolbar -> "Segment"
                Coui17TopBarStyle.GlassHeader -> "Glass header"
                else -> "Top bar"
            },
        )
    }
    val subtitleText = if (showSubtitle && style.supportsSubtitle) str(style.label) else ""
    val titleColor = if (accentTitle) COUITheme.colorScheme.primary else COUITheme.colorScheme.onSurface

    val controls = buildList<TopBarControlItem> {
        add(
            TopBarControlItem.Dropdown(
                title = "Top bar style",
                items = Coui17TopBarStyle.entries.map { it.label },
                selectedIndex = styleIndex,
                onSelected = { styleIndex = it },
            ),
        )
        if (style.supportsSurfaceMode) {
            add(
                TopBarControlItem.Dropdown(
                    title = "Surface material",
                    items = TopBarSurfaceMode.entries.map { it.label },
                    selectedIndex = surfaceModeIndex,
                    onSelected = { surfaceModeIndex = it },
                ),
            )
        }
        if (style.isCollapsibleTopAppBar) {
            add(
                TopBarControlItem.Toggle(
                    title = "Linked search bar",
                    checked = showLinkedSearch,
                    onCheckedChange = { enabled ->
                        showLinkedSearch = enabled
                        if (!enabled) {
                            searchExpanded = false
                            searchQuery = ""
                        }
                    },
                ),
            )
        }
        if (style.supportsSubtitle) {
            add(
                TopBarControlItem.Toggle(
                    title = "Show subtitle",
                    checked = showSubtitle,
                    onCheckedChange = { showSubtitle = it },
                ),
            )
            if (style.isCollapsibleTopAppBar && showSubtitle) {
                add(
                    TopBarControlItem.Toggle(
                        title = "Hide subtitle on collapse",
                        checked = hideSubtitleOnCollapse,
                        onCheckedChange = { hideSubtitleOnCollapse = it },
                    ),
                )
            }
        }
        if (style.supportsNavigationIcon) {
            add(
                TopBarControlItem.Toggle(
                    title = "Show back button",
                    checked = showBackButton,
                    onCheckedChange = { showBackButton = it },
                ),
            )
        }
        if (style.supportsActions) {
            add(
                TopBarControlItem.Toggle(
                    title = "Show actions",
                    checked = showActions,
                    onCheckedChange = { showActions = it },
                ),
            )
            if (showActions) {
                add(
                    TopBarControlItem.Dropdown(
                        title = "Action style",
                        items = TopBarActionStyle.entries.map { it.label },
                        selectedIndex = actionStyleIndex,
                        onSelected = { actionStyleIndex = it },
                    ),
                )
            }
        }
        if (style.supportsTitleOptions) {
            add(
                TopBarControlItem.Toggle(
                    title = "Accent title",
                    checked = accentTitle,
                    onCheckedChange = { accentTitle = it },
                ),
            )
            add(
                TopBarControlItem.Toggle(
                    title = "Long title",
                    checked = longTitle,
                    onCheckedChange = { longTitle = it },
                ),
            )
        }
        add(
            TopBarControlItem.Toggle(
                title = "Wallpaper backdrop",
                checked = wallpaperBackdrop,
                onCheckedChange = { wallpaperBackdrop = it },
            ),
        )
    }

    Coui17Frame(
        snackbarHostState = snackbarHostState,
        pages = {
            Box(modifier = Modifier.fillMaxSize()) {
                if (wallpaperBackdrop) {
                    ShowcaseWallpaperLayer()
                }
                Coui17PageContent(
                    scrollBehavior = scrollBehavior,
                    contentTop = contentTop,
                    contentBottom = navigationBar,
                ) { pagePadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!linkedSearchEnabled || topBarSearchState.isContentVisible) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (linkedSearchEnabled) {
                                            Modifier.topAppBarSearchContent(topBarSearchState)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .scrollEndHaptic()
                                    .overScrollVertical(),
                                contentPadding = pagePadding,
                            ) {
                                item(key = "controls-card") {
                                    TopBarControlsCard(
                                        controls = controls,
                                        showReturnRow = !showBackButton || !style.supportsNavigationIcon,
                                        onReturn = { navigator.pop() },
                                    )
                                }
                                showcaseSampleItems(
                                    numbers = filteredItems,
                                    surfaceTranslucent = wallpaperBackdrop,
                                )
                            }
                        }
                        if (linkedSearchEnabled && topBarSearchState.isResultsVisible) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .topAppBarSearchResults(topBarSearchState)
                                    .scrollEndHaptic()
                                    .overScrollVertical(),
                                contentPadding = PaddingValues(
                                    top = statusBar + TopAppBarDefaults.SearchSlotHeight + 12.dp,
                                    bottom = navigationBar + 16.dp,
                                ),
                            ) {
                                showcaseSampleItems(
                                    numbers = filteredItems,
                                    surfaceTranslucent = wallpaperBackdrop,
                                )
                            }
                        }
                    }
                }
            }
        },
        bars = {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onSizeChanged { measuredBarHeightPx = it.height },
            ) {
                ShowcaseTopBarHost(
                    style = style,
                    surfaceMode = surfaceMode,
                    actionStyle = actionStyle,
                    title = titleText,
                    subtitle = subtitleText,
                    titleColor = titleColor,
                    hideSubtitleOnCollapse = hideSubtitleOnCollapse,
                    showBackButton = showBackButton,
                    showActions = showActions,
                    showLinkedSearch = linkedSearchEnabled,
                    searchState = topBarSearchState,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    searchExpanded = searchExpanded,
                    onSearchExpandedChange = { expanded ->
                        searchExpanded = expanded
                        if (!expanded) searchQuery = ""
                    },
                    selectedSegment = selectedSegment,
                    onSegmentSelected = { selectedSegment = it },
                    filterTabs = filterTabs,
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    scrollBehavior = scrollBehavior,
                    onBack = { navigator.pop() },
                )
            }
        },
    )
}

@Composable
private fun ShowcaseTopBarHost(
    style: Coui17TopBarStyle,
    surfaceMode: TopBarSurfaceMode,
    actionStyle: TopBarActionStyle,
    title: String,
    subtitle: String,
    titleColor: Color,
    hideSubtitleOnCollapse: Boolean,
    showBackButton: Boolean,
    showActions: Boolean,
    showLinkedSearch: Boolean,
    searchState: TopAppBarSearchState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    selectedSegment: Int,
    onSegmentSelected: (Int) -> Unit,
    filterTabs: List<ChipTabData>,
    selectedFilter: Int,
    onFilterSelected: (Int) -> Unit,
    scrollBehavior: ScrollBehavior,
    onBack: () -> Unit,
) {
    val barColor = AppBarBlurOverlayDefaults.containerColor()
    val effectiveBackdrop = if (style.supportsSurfaceMode && surfaceMode == TopBarSurfaceMode.Solid) {
        null
    } else {
        LocalBackdrop.current
    }
    val gradientGlass = surfaceMode == TopBarSurfaceMode.GradientGlass
    val navigationIcon: @Composable () -> Unit = {
        if (showBackButton) {
            HighlightedIconButton(
                onClick = onBack,
                modifier = Modifier.couiTopAppBarActionItem(),
            ) {
                Icon(imageVector = COUIIcons.Back, contentDescription = str("Back"))
            }
        }
    }
    val actions: @Composable RowScope.() -> Unit = {
        if (showActions) {
            ShowcaseBarActions(
                style = style,
                actionStyle = actionStyle,
            )
        }
    }
    val linkedSearchBottomContent: @Composable () -> Unit = {
        if (showLinkedSearch) {
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearch = {},
                onExpandedChange = onSearchExpandedChange,
                expanded = searchExpanded,
                label = str("Search"),
                cancelText = str("Cancel"),
                onCancel = {
                    onSearchQueryChange("")
                    onSearchExpandedChange(false)
                },
            )
        }
    }

    CompositionLocalProvider(LocalBackdrop provides effectiveBackdrop) {
        when (style) {
            Coui17TopBarStyle.LargeCollapsing -> TopAppBar(
                title = title,
                largeTitle = title,
                subtitle = subtitle,
                color = barColor,
                titleColor = titleColor,
                largeTitleColor = titleColor,
                navigationIcon = navigationIcon,
                actions = actions,
                scrollBehavior = scrollBehavior,
                showDivider = surfaceMode == TopBarSurfaceMode.Solid,
                hideSubtitleOnCollapse = hideSubtitleOnCollapse,
                gradientGlass = gradientGlass,
                searchState = if (showLinkedSearch) searchState else null,
                bottomContent = linkedSearchBottomContent,
            )

            Coui17TopBarStyle.SettingsHome -> TopAppBar(
                title = title,
                largeTitle = title,
                subtitle = subtitle,
                color = barColor,
                titleColor = titleColor,
                largeTitleColor = titleColor,
                navigationIcon = navigationIcon,
                actions = actions,
                scrollBehavior = scrollBehavior,
                showDivider = surfaceMode == TopBarSurfaceMode.Solid,
                hideSubtitleOnCollapse = hideSubtitleOnCollapse,
                gradientGlass = gradientGlass,
                titlePadding = TopAppBarDefaults.SettingsHomeTitlePadding,
                expandedHeight = TopAppBarDefaults.SettingsHomeExpandedHeight,
                largeTitleFontSize = TopAppBarDefaults.SettingsHomeLargeTitleFontSize,
                largeTitleBottomMargin = TopAppBarDefaults.SettingsHomeLargeTitleBottomMargin,
                searchState = if (showLinkedSearch) searchState else null,
                bottomContent = linkedSearchBottomContent,
            )

            Coui17TopBarStyle.FadeLargeTitle -> TopAppBar(
                title = title,
                largeTitle = title,
                subtitle = subtitle,
                color = barColor,
                titleColor = titleColor,
                largeTitleColor = titleColor,
                navigationIcon = navigationIcon,
                actions = actions,
                scrollBehavior = scrollBehavior,
                showDivider = surfaceMode == TopBarSurfaceMode.Solid,
                hideSubtitleOnCollapse = hideSubtitleOnCollapse,
                gradientGlass = gradientGlass,
                fadeLargeTitleOnCollapse = true,
                searchState = if (showLinkedSearch) searchState else null,
                bottomContent = linkedSearchBottomContent,
            )

            Coui17TopBarStyle.SmallToolbar -> SmallTopAppBar(
                title = title,
                subtitle = subtitle,
                color = barColor,
                titleColor = titleColor,
                navigationIcon = navigationIcon,
                actions = actions,
                scrollBehavior = scrollBehavior,
                showDivider = surfaceMode == TopBarSurfaceMode.Solid,
                gradientGlass = gradientGlass,
            )

            Coui17TopBarStyle.CenterTitle -> SmallTopAppBar(
                title = title,
                subtitle = subtitle,
                color = barColor,
                titleColor = titleColor,
                navigationIcon = navigationIcon,
                actions = actions,
                scrollBehavior = scrollBehavior,
                showDivider = surfaceMode == TopBarSurfaceMode.Solid,
                centerTitle = true,
                gradientGlass = gradientGlass,
            )

            Coui17TopBarStyle.SegmentToolbar -> SmallTopAppBar(
                title = title,
                color = barColor,
                titleColor = titleColor,
                navigationIcon = navigationIcon,
                actions = actions,
                scrollBehavior = scrollBehavior,
                showDivider = surfaceMode == TopBarSurfaceMode.Solid,
                segmentButton = {
                    TabRowWithContour(
                        tabs = listOf(str("All"), str("Photos")),
                        selectedTabIndex = selectedSegment,
                        onTabSelected = onSegmentSelected,
                        height = 32.dp,
                    )
                },
                gradientGlass = gradientGlass,
            )

            Coui17TopBarStyle.PinnedLargeTitle -> PinnedTopAppBar(
                title = title,
                scrollBehavior = scrollBehavior,
                titleColor = titleColor,
                navigationIcon = if (showBackButton) navigationIcon else null,
                actions = actions,
            )

            Coui17TopBarStyle.ToolbarSearch -> TopAppBarSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                expanded = searchExpanded,
                onExpandedChange = onSearchExpandedChange,
                modifier = Modifier.fillMaxWidth(),
                title = title,
                largeTitle = title,
                placeholder = str("Search"),
                cancelText = str("Cancel"),
                onCancel = {
                    onSearchQueryChange("")
                    onSearchExpandedChange(false)
                },
                searchButtonContentDescription = str("Search"),
                scrollBehavior = scrollBehavior,
                extraActions = {
                    HighlightedIconButton(
                        onClick = onBack,
                        modifier = Modifier.couiTopAppBarActionItem(),
                    ) {
                        Icon(imageVector = COUIIcons.Back, contentDescription = str("Back"))
                    }
                },
            )

            Coui17TopBarStyle.TwoRowBar -> TwoRowTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                platformRenderer = false,
                secondRow = {
                    ChipTabGroup(
                        tabs = filterTabs,
                        selectedIndex = selectedFilter,
                        onSelectedChange = onFilterSelected,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        backdrop = LocalBackdrop.current,
                    )
                },
            ) {
                TopAppBarSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    expanded = true,
                    onExpandedChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = str("Search"),
                    cancelText = null,
                    gradientColor = Color.Transparent,
                )
            }

            Coui17TopBarStyle.SearchFilterBar -> SearchFilterTopAppBar(
                filters = filterTabs,
                selectedFilter = selectedFilter,
                onFilterSelected = onFilterSelected,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = COUIIcons.Sort,
                searchRow = {
                    TopAppBarSearchBar(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        expanded = true,
                        onExpandedChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = str("Search"),
                        cancelText = null,
                        gradientColor = Color.Transparent,
                    )
                },
            )

            Coui17TopBarStyle.GlassHeader -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .couiGlassHeader(
                        backdrop = LocalBackdrop.current,
                        containerColor = barColor,
                    )
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (showBackButton) {
                        HighlightedIconButton(
                            onClick = onBack,
                            modifier = Modifier.couiTopAppBarActionItem(),
                        ) {
                            Icon(imageVector = COUIIcons.Back, contentDescription = str("Back"))
                        }
                    }
                    Text(
                        text = title,
                        color = titleColor,
                        style = COUITheme.textStyles.title4,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.ShowcaseBarActions(
    style: Coui17TopBarStyle,
    actionStyle: TopBarActionStyle,
) {
    when (actionStyle) {
        TopBarActionStyle.Icons -> {
            HighlightedIconButton(
                onClick = {},
                modifier = Modifier.couiTopAppBarActionItem(),
            ) {
                Icon(imageVector = COUIIcons.Basic.Search, contentDescription = str("Search"))
            }
            HighlightedIconButton(
                onClick = {},
                modifier = Modifier.couiTopAppBarActionItem(),
            ) {
                Icon(imageVector = COUIIcons.More, contentDescription = str("More"))
            }
        }

        TopBarActionStyle.RedDot -> {
            IconButton(
                onClick = {},
                modifier = Modifier.couiTopAppBarActionItem(),
                minWidth = 40.dp,
                minHeight = 40.dp,
                redDot = 0,
            ) {
                Icon(imageVector = COUIIcons.Basic.Search, contentDescription = str("Search"))
            }
            IconButton(
                onClick = {},
                modifier = Modifier.couiTopAppBarActionItem(),
                minWidth = 40.dp,
                minHeight = 40.dp,
                redDot = 9,
            ) {
                Icon(imageVector = COUIIcons.More, contentDescription = str("More"))
            }
        }

        TopBarActionStyle.TextButton -> {
            if (style == Coui17TopBarStyle.PinnedLargeTitle) {
                PinnedTopAppBarTextAction(
                    text = str("Edit"),
                    onClick = {},
                )
            } else {
                ToolbarTextButton(
                    text = str("Done"),
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun TopBarControlsCard(
    controls: List<TopBarControlItem>,
    showReturnRow: Boolean,
    onReturn: () -> Unit,
) {
    val totalCount = controls.size + if (showReturnRow) 1 else 0
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = Coui17Dimens.ListCardInset)
            .padding(top = Coui17Dimens.ListGroupTitlePadding, bottom = 16.dp),
    ) {
        controls.forEachIndexed { index, item ->
            val position = cardListPositionOf(index, totalCount)
            when (item) {
                is TopBarControlItem.Dropdown -> OverlayDropdownPreference(
                    title = str(item.title),
                    items = item.items.map { str(it) },
                    selectedIndex = item.selectedIndex,
                    onSelectedIndexChange = item.onSelected,
                    cardListPosition = position,
                )

                is TopBarControlItem.Toggle -> SwitchPreference(
                    title = str(item.title),
                    checked = item.checked,
                    onCheckedChange = item.onCheckedChange,
                    cardListPosition = position,
                )
            }
        }
        if (showReturnRow) {
            BasicComponent(
                title = str("Back"),
                summary = str("Return to home page"),
                onClick = onReturn,
                cardListPosition = cardListPositionOf(totalCount - 1, totalCount),
            )
        }
    }
}

private fun LazyListScope.showcaseSampleItems(
    numbers: List<Int>,
    surfaceTranslucent: Boolean,
) {
    item(key = "sample-list-card") {
        Card(
            cornerRadius = CardDefaults.GroupCornerRadius,
            weight = CardDefaults.GroupWeight,
            colors = if (surfaceTranslucent) {
                CardDefaults.defaultColors(
                    color = COUITheme.colorScheme.surfaceContainer.copy(alpha = 0.82f),
                )
            } else {
                CardDefaults.defaultColors()
            },
            modifier = Modifier
                .padding(horizontal = Coui17Dimens.ListCardInset)
                .padding(bottom = 24.dp),
        ) {
            if (numbers.isEmpty()) {
                BasicComponent(
                    title = str("No matching items"),
                    cardListPosition = CardListPosition.Full,
                )
            } else {
                numbers.forEachIndexed { index, number ->
                    val swatch = ShowcaseSampleColors[(number - 1) % ShowcaseSampleColors.size]
                    BasicComponent(
                        title = str("Scroll row {}", number),
                        summary = str("Scroll up and down to inspect collapse and glass blur"),
                        startAction = {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(swatch, swatch.copy(alpha = 0.55f)),
                                        ),
                                    ),
                            )
                        },
                        cardListPosition = cardListPositionOf(index, numbers.size),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowcaseWallpaperLayer() {
    WallpaperState.bitmap?.let { wallpaper ->
        Image(
            bitmap = wallpaper,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } ?: Image(
        painter = painterResource(Res.drawable.blur_test_bg),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}

private fun defaultShowcaseContentTop(
    style: Coui17TopBarStyle,
    statusBar: Dp,
    showSubtitle: Boolean,
    showLinkedSearch: Boolean,
): Dp {
    val subtitleExtra = if (showSubtitle && style.supportsSubtitle) 22.dp else 0.dp
    val searchExtra = if (showLinkedSearch) TopAppBarDefaults.SearchSlotHeight else 0.dp
    val barHeight = when (style) {
        Coui17TopBarStyle.LargeCollapsing,
        Coui17TopBarStyle.FadeLargeTitle,
        -> TopAppBarDefaults.ExpandedHeight + subtitleExtra + searchExtra

        Coui17TopBarStyle.SettingsHome ->
            TopAppBarDefaults.SettingsHomeExpandedHeight + subtitleExtra + searchExtra

        Coui17TopBarStyle.SmallToolbar,
        Coui17TopBarStyle.CenterTitle,
        Coui17TopBarStyle.SegmentToolbar,
        -> TopAppBarDefaults.SmallTopAppBarCenterHeight + subtitleExtra

        Coui17TopBarStyle.PinnedLargeTitle -> PinnedTopAppBarDefaults.Height

        Coui17TopBarStyle.ToolbarSearch -> TopAppBarDefaults.ExpandedHeight

        Coui17TopBarStyle.TwoRowBar,
        Coui17TopBarStyle.SearchFilterBar,
        -> TwoRowTopAppBarDefaults.barHeight(secondRowHeight = TwoRowTopAppBarDefaults.SecondRowHeight)

        Coui17TopBarStyle.GlassHeader -> 56.dp
    }
    return statusBar + barHeight
}
