// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import appstate.AppState
import appstate.LocalAppState
import appstate.LocalNavigator
import appstate.LocalUpdateAppState
import component.SearchPager
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlayDefaults
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.SnackbarHost
import io.github.suqi8.coui.kmp.basic.SnackbarHostState
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.squircle.LocalSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch
import navigation3.Navigator
import navigation3.Route
import pages.ColorPage
import pages.IconsPage
import pages.SettingsPage
import pages.TextStylePage
import ui.AppTheme
import ui.keyColorFor
import utils.SearchStatus
import kotlin.math.max

/**
 * The app's five pages.
 *
 * The first three are what the accessory capsule carries; the last two are the panel entries, which
 * is how the coui16 gallery's five pages map onto ColorOS 17's one-third-two split.
 */
enum class Coui17Page(val title: String) {
    Home("设置"),
    Icons("图标"),
    Color("配色"),
    TextStyle("文字样式"),
    Settings("设置项"),
    ;

    /** Whether this page is reached from the panel rather than the capsule, so it carries a back key. */
    val isPanelPage: Boolean get() = this == TextStyle || this == Settings
}

/**
 * COUI 17 — the component gallery wearing ColorOS 17's clothes.
 *
 * The content is the coui16 gallery's, one-for-one; the layout is the system Settings app's: a bar
 * per page carrying the gradient-blur material, and an accessory bar along the bottom.
 */
@Composable
fun Coui17App() {
    var appState by remember { mutableStateOf(AppState(showTopAppBar = false)) }
    // The pages are the coui16 ones and would each draw their own app bar, so the flag is pinned
    // off: ColorOS 17 gives every page the bar built here instead.
    val updateAppState: ((AppState) -> AppState) -> Unit = remember {
        { transform -> appState = transform(appState).copy(showTopAppBar = false) }
    }
    // The "Other" entry's arrows push routes the coui16 gallery renders for itself. COUI 17 has no
    // navigation surface of its own, so the stack lives here and nothing draws it: the arrows stay
    // inert instead of throwing when the entry scrolls into view.
    val navigator = remember { Navigator(mutableListOf<NavKey>(Route.Main)) }
    AppTheme(
        colorMode = appState.colorMode,
        keyColor = keyColorFor(appState.seedIndex),
        paletteStyle = appState.paletteStyle,
        colorSpec = appState.colorSpec,
    ) {
        CompositionLocalProvider(
            LocalAppState provides appState,
            LocalUpdateAppState provides updateAppState,
            LocalNavigator provides navigator,
            LocalSquircleEnabled provides appState.enableSquircle,
        ) {
            Coui17Root()
        }
    }
}

@Composable
private fun Coui17Root() {
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
    val scrollOffset = remember { ScrollOffset() }
    // The device's hint reads `搜索设置项` (`Settings.apk` string table, length-prefixed 5), and the
    // collapsed pill draws five glyphs at x 219..501px.
    var searchStatus by remember { mutableStateOf(SearchStatus(label = "搜索设置项")) }
    var panelOpen by remember { mutableStateOf(false) }
    var lastCapsulePage by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val scrollFraction = AppBarBlurOverlayDefaults.scrollAlpha(
        scrollPx = scrollOffset.value,
        density = density,
    )
    val bottomSpace = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        Coui17Dimens.BottomBarSpace
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val onSearch = {
        searchStatus = searchStatus.copy(current = SearchStatus.Status.EXPANDING)
    }
    val currentPage = Coui17Page.entries[pagerState.currentPage]

    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = pageColor,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                        userScrollEnabled = searchStatus.isCollapsed(),
                    ) { page ->
                        Coui17PageContent(
                            page = Coui17Page.entries[page],
                            snackbarHostState = snackbarHostState,
                            scrollOffset = scrollOffset,
                            contentTop = if (page == Coui17Page.Home.ordinal) {
                                Coui17Dimens.HomeAppBarBottom
                            } else {
                                statusBar + Coui17Dimens.CompactToolbarHeight
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
                CompositionLocalProvider(LocalBackdrop provides barBackdrop) {
                    if (currentPage == Coui17Page.Home) {
                        Coui17HomeTopBar(
                            title = currentPage.title,
                            searchLabel = "搜索设置项",
                            onSearchClick = onSearch,
                            scrollFraction = scrollFraction,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    } else {
                        Coui17CompactToolbar(
                            title = currentPage.title,
                            onBack = if (currentPage.isPanelPage) {
                                { coroutineScope.launch { pagerState.animateScrollToPage(lastCapsulePage) } }
                            } else {
                                null
                            },
                            scrollFraction = scrollFraction,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }
                    Coui17AccessoryBar(
                        currentPage = pagerState.currentPage,
                        panelOpen = panelOpen,
                        onSelectPage = { page ->
                            lastCapsulePage = page
                            coroutineScope.launch { pagerState.animateScrollToPage(page) }
                        },
                        onSearch = onSearch,
                        onOpenPanel = { panelOpen = true },
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
                    offsetY = Coui17Dimens.HomeAppBarBottom,
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
    scrollOffset: ScrollOffset,
    contentTop: Dp,
    contentBottom: Dp,
) {
    // The pages keep their own scroll state, so the bar's fade is driven from here instead of from
    // a behaviour the pages do not share.
    val accumulator = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                scrollOffset.value = (scrollOffset.value - consumed.y).coerceAtLeast(0f)
                return Offset.Zero
            }
        }
    }
    val pagePadding = PaddingValues(top = contentTop, bottom = contentBottom)

    Box(modifier = Modifier.fillMaxSize().nestedScroll(accumulator)) {
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

/** How far the content under a page's app bar has scrolled, in pixels. */
@Stable
private class ScrollOffset {
    var value by mutableFloatStateOf(0f)
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
