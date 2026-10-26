// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package coui17

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import appstate.AppState
import appstate.LocalAppState
import appstate.LocalUpdateAppState
import component.SearchPager
import i18n.LocalAppStrings
import i18n.LocalLanguageSwitchEnabled
import i18n.str
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlayDefaults
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.ScrollBehavior
import io.github.suqi8.coui.kmp.basic.SnackbarHost
import io.github.suqi8.coui.kmp.basic.SnackbarHostState
import io.github.suqi8.coui.kmp.basic.TopAppBarDefaults
import io.github.suqi8.coui.kmp.basic.rememberTopAppBarState
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.squircle.LocalSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import pages.ColorPage
import pages.IconsPage
import pages.SecondaryPagesNavHost
import pages.SettingsPage
import pages.TextStylePage
import ui.AppTheme
import ui.keyColorFor
import utils.FPSMonitor
import utils.SearchStatus
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The app's five pages.
 *
 * The first three are what the accessory capsule carries; the last two are the panel entries, which
 * is how the coui16 gallery's five pages map onto ColorOS 17's one-third-two split.
 *
 * [title] is the English source string the bar renders; `str` resolves it for the language the user
 * picked, so nothing here has to know which language is selected.
 */
enum class Coui17Page(val title: String) {
    Home("COUI"),
    Icons("Icons"),
    Color("Color"),
    TextStyle("Text style"),
    Settings("Settings items"),
    ;

    /** Whether this page is reached from the panel rather than the capsule, so it carries a back key. */
    val isPanelPage: Boolean get() = this == TextStyle || this == Settings
}

private enum class Coui17PanelAnchor { TopBar, BottomBar }

/**
 * COUI 17 — the component gallery wearing ColorOS 17's clothes.
 *
 * The content is the coui16 gallery's, one-for-one; the layout is the system Settings app's: a bar
 * per page carrying the gradient-blur material, and an accessory bar along the bottom.
 */
@Composable
fun Coui17App(
    onColorModeChange: ((Int) -> Unit)? = null,
) {
    var appState by remember { mutableStateOf(AppState(showTopAppBar = false)) }
    // The pages are the coui16 ones and would each draw their own app bar, so the flag is pinned
    // off: ColorOS 17 gives every page the bar built here instead.
    val updateAppState: ((AppState) -> AppState) -> Unit = remember {
        { transform -> appState = transform(appState).copy(showTopAppBar = false) }
    }

    // Reported to the host so it can follow the colour mode (e.g. the Android system bar style).
    // The first value is the initial mode, not a change, hence drop(1).
    val currentOnColorModeChange by rememberUpdatedState(onColorModeChange)
    LaunchedEffect(Unit) {
        snapshotFlow { appState.colorMode }
            .drop(1)
            .collect { currentOnColorModeChange?.invoke(it) }
    }
    // The "Other" entry's arrows push routes. The stack, the `LocalNavigator` and the pages that
    // answer those routes all live in `SecondaryPagesNavHost`, which wraps this root; the root
    // itself only has to stay inside it so the pushes resolve to a surface that draws them.
    AppTheme(
        colorMode = appState.colorMode,
        keyColor = keyColorFor(appState.seedIndex),
        paletteStyle = appState.paletteStyle,
        colorSpec = appState.colorSpec,
    ) {
        CompositionLocalProvider(
            LocalAppState provides appState,
            LocalUpdateAppState provides updateAppState,
            LocalSquircleEnabled provides appState.enableSquircle,
            // One provider for the whole app: every screen resolves its copy through this, so
            // changing the language in the settings page recomposes all of them at once.
            LocalAppStrings provides appState.language.strings,
            LocalLanguageSwitchEnabled provides true,
        ) {
            // The gallery root is the nav host's `Route.Main` entry, so the pages it pushes land in
            // the same window instead of on a stack nothing draws.
            Box(Modifier.fillMaxSize()) {
                SecondaryPagesNavHost(
                    padding = PaddingValues(0.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Coui17Root()
                }
                if (appState.showFPSMonitor) {
                    FPSMonitor(Modifier.fillMaxSize().statusBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun Coui17Root() {
    val appState = LocalAppState.current
    val wallpaper = rememberWallpaper()
    val surfaceColor = COUITheme.colorScheme.surface
    // The page's own background, and the colour the app bar material is drawn in. The device resolves
    // the material's base colour from the page background (`resolvePageFallbackBackgroundColor`), so
    // the two are one colour and the material never shows as a band of its own — measured on the
    // device, the material's full-strength patch (y=0, alpha 1) is the page background's own
    // (240,240,241) over an identical (240,240,241) page.
    val pageColor = AppBarBlurOverlayDefaults.containerColor()
    // The page content on its own, with no background of its own, so the wallpaper shows through it.
    val pageBackdrop = rememberLayerBackdrop { drawContent() }
    // What the bars sample: the page, opaque. The device's page has a background behind the bar, and
    // the material blurs that — never the wallpaper, never empty space. A layer with a transparent
    // background cannot stand in for it: the blur reads the layer's clear colour, which washes the
    // bar out to near-white as soon as the ramp lets the blur through. The light ramp hides that
    // (its top alpha is 1, so only the base colour shows) but the dark one does not (0.4), and there
    // the bar came out at ~175 over the device's 48.
    val barBackdrop = rememberLayerBackdrop {
        drawRect(pageColor)
        drawLayer(pageBackdrop.graphicsLayer)
    }
    // What the page's own glass samples: the wallpaper with the page over it. Every surface that
    // reads it is a sibling of the recorded node, never a child, or the blur would sample a backdrop
    // that already has the blur baked into it.
    //
    // The three modifiers sit on the same node and draw outside in, so the page layer is recorded
    // before either of the two that blit it, and neither blit costs a second `drawContent()` — a
    // second call would draw the whole pager again for every layer on every frame.
    val backdrop = rememberLayerBackdrop {
        drawWallpaper(surfaceColor, wallpaper.image)
        drawLayer(pageBackdrop.graphicsLayer)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { Coui17Page.entries.size })
    val coroutineScope = rememberCoroutineScope()
    // One behavior is shared by the visible page and its top bar. The pager keeps the page
    // content alive while swiping, so resetting the state on a page change avoids carrying the
    // previous page's collapsed title into the next one.
    val topBarState = rememberTopAppBarState()
    val topBarScrollBehavior = COUIScrollBehavior(state = topBarState)
    // The device's hint is five character slots long, and the collapsed pill draws five glyphs at
    // x 219..501px.
    val searchLabel = str("Search settings")
    var searchStatus by remember { mutableStateOf(SearchStatus(label = searchLabel)) }
    // The pill's hint is part of the search state rather than of the bar, so a language change has to
    // be carried into it; re-seeding the state instead would throw away a search in progress.
    LaunchedEffect(searchLabel) { searchStatus = searchStatus.copy(label = searchLabel) }
    var panelOpen by remember { mutableStateOf(false) }
    var panelAnchor by remember { mutableStateOf(Coui17PanelAnchor.BottomBar) }
    var lastCapsulePage by remember { mutableIntStateOf(0) }
    val bottomSpace = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        Coui17Dimens.BottomBarSpace
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val expandedTopBarHeight = statusBar + TopAppBarDefaults.ExpandedHeight
    val expandedHomeTopBarHeight = expandedTopBarHeight + Coui17Dimens.HomeSearchBandHeight
    // Opening the search slides the bar from the pill up to the collapsed toolbar row: on the device
    // the capsule travels 615px → 164px over ~300ms. The pill's y depends on the app bar's collapse
    // state, so it is measured rather than assumed; the seed is the fully expanded app bar, which is
    // where a tap can actually land.
    var searchPillTop by remember { mutableStateOf(expandedTopBarHeight) }
    // The row the search bar docks into: the collapsed app bar's height, with the 40dp capsule
    // centred in it. Measured on the device at 42.3dp / 164px, which is exactly this formula.
    val dockedSearchTop = statusBar +
        (TopAppBarDefaults.CollapsedHeight - Coui17Dimens.SearchPillHeight) / 2
    val onSearch = {
        searchStatus = searchStatus.copy(current = SearchStatus.Status.EXPANDING)
    }
    val currentPage = Coui17Page.entries[pagerState.currentPage]
    LaunchedEffect(currentPage) {
        topBarState.heightOffset = 0f
        topBarState.contentOffset = 0f
        panelOpen = false
    }

    // Settings is a system app: on this 620 dpi device it lays out at 3.875 px/dp while a
    // third-party app gets 3.5 px/dp for the same screen. Every screen in this gallery is a replica
    // of a system screen, so the gallery is laid out at the system density — otherwise every dp here
    // renders 10% smaller than the panel it copies and no side-by-side comparison holds.
    val systemDensity = Density(Coui17Density.SYSTEM_APP, LocalDensity.current.fontScale)
    CompositionLocalProvider(
        LocalBackdrop provides if (appState.enableBlur) backdrop else null,
        LocalDensity provides systemDensity,
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = pageColor,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            // The pager fills the window and every bar floats over it, so the app bar material has
            // content to sample.
            edgeToEdge = true,
            snackbarHost = { SnackbarHost(state = snackbarHostState) },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Only the page content is recorded into the backdrop.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(backdrop)
                        .layerBackdrop(barBackdrop)
                        .layerBackdrop(pageBackdrop),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = appState.enablePageUserScroll && searchStatus.isCollapsed(),
                    ) { page ->
                        Coui17PageContent(
                            page = Coui17Page.entries[page],
                            snackbarHostState = snackbarHostState,
                            scrollBehavior = topBarScrollBehavior,
                            contentTop = if (page == Coui17Page.Home.ordinal) {
                                expandedHomeTopBarHeight
                            } else {
                                expandedTopBarHeight
                            },
                            contentBottom = bottomSpace,
                        )
                    }
                }
                // Every floating bar reads the page layer, not the wallpaper one: the device's material
                // blurs the page behind the bar and shows no wallpaper there. Both the top material and
                // the bottom accessory bar sit inside this provider — the bottom bar was left on the
                // wallpaper layer, where the page has no content of its own the blur read straight
                // through to the wallpaper the user cannot see behind the bar, so its capsules and
                // accessory balls came out at the wallpaper's own brightness instead of the page's.
                CompositionLocalProvider(LocalBackdrop provides if (appState.enableBlur) barBackdrop else null) {
                    Coui17TopAppBar(
                        title = str(currentPage.title),
                        searchLabel = searchLabel,
                        showSearchField = currentPage == Coui17Page.Home,
                        showBack = currentPage.isPanelPage,
                        onBack = if (currentPage.isPanelPage) {
                            { coroutineScope.launch { pagerState.animateScrollToPage(lastCapsulePage) } }
                        } else {
                            {}
                        },
                        onSearch = onSearch,
                        panelOpen = panelOpen && panelAnchor == Coui17PanelAnchor.TopBar,
                        onOpenPanel = {
                            panelAnchor = Coui17PanelAnchor.TopBar
                            panelOpen = true
                        },
                        onPanelSelect = { page ->
                            panelOpen = false
                            coroutineScope.launch { pagerState.animateScrollToPage(page) }
                        },
                        onPanelDismiss = { panelOpen = false },
                        scrollBehavior = topBarScrollBehavior,
                        modifier = Modifier.align(Alignment.TopCenter),
                        onSearchPillPositioned = { searchPillTop = it },
                        searchPillVisible = searchStatus.isCollapsed(),
                    )
                    Coui17AccessoryBar(
                        currentPage = pagerState.currentPage,
                        panelOpen = panelOpen && panelAnchor == Coui17PanelAnchor.BottomBar,
                        onSelectPage = { page ->
                            lastCapsulePage = page
                            coroutineScope.launch { pagerState.animateScrollToPage(page) }
                        },
                        onSearch = onSearch,
                        onOpenPanel = {
                            panelAnchor = Coui17PanelAnchor.BottomBar
                            panelOpen = true
                        },
                        onPanelSelect = { page ->
                            panelOpen = false
                            coroutineScope.launch { pagerState.animateScrollToPage(page) }
                        },
                        onPanelDismiss = { panelOpen = false },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
                searchStatus.SearchPager(
                    onSearchStatusChange = { searchStatus = it },
                    barTop = searchPillTop,
                    dockedBarTop = dockedSearchTop,
                    surfaceColor = pageColor,
                    defaultResult = {},
                    result = {},
                )
            }
        }
    }
}

/** One page's content. Its app bar is drawn by the caller, as a sibling of the recorded layer. */
@Composable
private fun Coui17PageContent(
    page: Coui17Page,
    snackbarHostState: SnackbarHostState,
    scrollBehavior: ScrollBehavior,
    contentTop: Dp,
    contentBottom: Dp,
) {
    val pagePadding = PaddingValues(top = contentTop, bottom = contentBottom)

    // The list rides up with the bar instead of waiting for it to finish collapsing. The bar's live
    // height is `expandedHeight + heightOffset`, so shifting the content by that same offset keeps the
    // two locked frame-for-frame — the native `COUIFloatingScrollingViewBehavior` reaches the same
    // coupling by writing the bar's height into the content view's top padding.
    //
    // The lambda overload is deliberate: it is read in the layout phase only, so a scrolling page
    // re-lays out without recomposing the list. `Modifier.offset(y = dp)` would recompose every frame.
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Keep the content's bottom edge attached to the viewport while the top follows
            // the collapsing app bar. A plain offset on a fillMaxSize node leaves a gap at the
            // bottom once heightOffset becomes negative, exposing the Scaffold's base colour.
            .layout { measurable, constraints ->
                val offsetY = scrollBehavior.state.heightOffset.roundToInt()
                val extraHeight = (-offsetY).coerceAtLeast(0)
                val expandedConstraints = constraints.copy(
                    minHeight = constraints.minHeight + extraHeight,
                    maxHeight = if (constraints.hasBoundedHeight) {
                        constraints.maxHeight + extraHeight
                    } else {
                        constraints.maxHeight
                    },
                )
                val placeable = measurable.measure(expandedConstraints)
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(0, offsetY)
                }
            }
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        when (page) {
            Coui17Page.Home -> Coui17HomePage(
                snackbarHostState = snackbarHostState,
                contentPadding = pagePadding,
                modifier = Modifier.fillMaxSize(),
            )

            Coui17Page.Icons -> IconsPage(padding = pagePadding)

            Coui17Page.Color -> ColorPage(padding = pagePadding)

            Coui17Page.TextStyle -> TextStylePage(padding = pagePadding)

            Coui17Page.Settings -> SettingsPage(padding = pagePadding)
        }
    }
}

/**
 * Paints the wallpaper into the backdrop layer, cover-fitted and centred, so the glass above it
 * refracts the same picture the user sees.
 */
private fun ContentDrawScope.drawWallpaper(
    surfaceColor: Color,
    wallpaper: ImageBitmap,
) {
    drawRect(surfaceColor)
    if (wallpaper.width <= 1 || wallpaper.height <= 1) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF16223B), Color(0xFF3B4F7D), Color(0xFF9AAECF)),
            ),
        )
        return
    }
    val targetSize = IntSize(size.width.toInt(), size.height.toInt())
    val scale = max(
        targetSize.width.toFloat() / wallpaper.width,
        targetSize.height.toFloat() / wallpaper.height,
    )
    val sourceSize = IntSize(
        width = (targetSize.width / scale).toInt().coerceAtLeast(1),
        height = (targetSize.height / scale).toInt().coerceAtLeast(1),
    )
    val sourceOffset = IntOffset(
        x = ((wallpaper.width - sourceSize.width) / 2).coerceAtLeast(0),
        y = ((wallpaper.height - sourceSize.height) / 2).coerceAtLeast(0),
    )
    drawImage(
        image = wallpaper,
        srcOffset = sourceOffset,
        srcSize = sourceSize,
        dstSize = targetSize,
    )
}
