// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.SideNavigationBar
import io.github.suqi8.coui.kmp.basic.SidePaneLayout
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TopAppBar
import io.github.suqi8.coui.kmp.basic.rememberSideNavigationBarState
import io.github.suqi8.coui.kmp.basic.rememberSidePaneLayoutState
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.FoldPane
import io.github.suqi8.coui.kmp.icon.extended.Album
import io.github.suqi8.coui.kmp.icon.extended.Back
import io.github.suqi8.coui.kmp.icon.extended.Create
import io.github.suqi8.coui.kmp.icon.extended.Download
import io.github.suqi8.coui.kmp.icon.extended.ExpandMore
import io.github.suqi8.coui.kmp.icon.extended.Favorites
import io.github.suqi8.coui.kmp.icon.extended.Photos
import io.github.suqi8.coui.kmp.icon.extended.Play
import io.github.suqi8.coui.kmp.icon.extended.Recent
import io.github.suqi8.coui.kmp.icon.extended.ScreenCapture
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.ColorSchemeMode
import io.github.suqi8.coui.kmp.theme.ThemeController

/**
 * COUI 17 — the ColorOS Gallery album page.
 *
 * Two pages, each giving one of the side panel families a position in a running app:
 *
 * - the album list is the page [SideNavigationBar] (G-3) draws its drawer over. The Gallery ships a
 *   real subclass of the device class — `ColorOS.app.sidepane.ui.SidePaneLayout
 *   extends COUISideNavigationBar`, hosted by `layout/app_activity_support_side_pane.xml` — and the
 *   album list of `layout/app_side_pane_fragment.xml` is its drawer content.
 * - the album detail is the page [SidePaneLayout] (G-4) draws its draggable pane on: the pane holds
 *   the same album list, and the fold button that `COUISidePaneLayout#createIconView` builds is the
 *   only control it adds. No studied app references `COUISidePaneLayout`, so this page is a
 *   reconstruction from the device class rather than a copy of a Gallery screen.
 */
@Composable
fun GalleryApp() {
    var colorMode by remember { mutableIntStateOf(1) }
    val controller = remember(colorMode) {
        ThemeController(
            when (colorMode) {
                1 -> ColorSchemeMode.Light
                2 -> ColorSchemeMode.Dark
                else -> ColorSchemeMode.System
            },
        )
    }

    COUITheme(controller = controller) {
        GalleryPages()
    }
}

@Composable
private fun GalleryPages() {
    var openAlbum by remember { mutableStateOf<GalleryAlbum?>(null) }
    val album = openAlbum

    if (album == null) {
        AlbumListPage(onOpenAlbum = { openAlbum = it })
    } else {
        AlbumDetailPage(album = album, onBack = { openAlbum = null })
    }
}

/**
 * The album list page.
 *
 * The Gallery hosts the drawer through its own `SidePaneLayout` subclass in
 * `layout/app_activity_support_side_pane.xml`, with a floating expand control (`iv_expand`, backed
 * by `drawable/coui_pane_icon_bg` and the Gallery's `drawable/app_ic_side_pane_control`) at the
 * page's top start. The demo places that control in the app bar's navigation slot and uses the COUI
 * fold glyph, because `app_ic_side_pane_control` is a Gallery asset rather than a COUI one.
 */
@Composable
private fun AlbumListPage(onOpenAlbum: (GalleryAlbum) -> Unit) {
    val drawerState = rememberSideNavigationBarState()
    val scrollBehavior = COUIScrollBehavior()

    SideNavigationBar(
        drawerContent = { AlbumNavigationContent(onOpenAlbum = onOpenAlbum) },
        state = drawerState,
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = "相册",
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = { drawerState.open() }) {
                            Icon(
                                imageVector = COUIIcons.Basic.FoldPane,
                                contentDescription = "打开侧边导航",
                            )
                        }
                    },
                )
            },
        ) { padding ->
            AlbumGrid(padding = padding, onOpenAlbum = onOpenAlbum)
        }
    }
}

/**
 * The album detail page.
 *
 * The [SidePaneLayout] sits inside the page body rather than over the app bar, so its fold button
 * keeps the `coui_side_pane_layout_icon_margin_start` / `_margin_top` offset from the top of the
 * pane layout — which is where `COUISidePaneLayout#createIconView` puts it — without colliding with
 * the app bar.
 */
@Composable
private fun AlbumDetailPage(album: GalleryAlbum, onBack: () -> Unit) {
    val paneState = rememberSidePaneLayoutState()
    val scrollBehavior = COUIScrollBehavior()

    BackHandler(enabled = paneState.isOpen) { paneState.close() }
    BackHandler(enabled = !paneState.isOpen) { onBack() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = album.name,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = COUIIcons.Back, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        SidePaneLayout(
            paneContent = { AlbumNavigationContent(onOpenAlbum = {}) },
            state = paneState,
            modifier = Modifier.padding(padding),
            coverStyle = true,
            alwaysShowMask = true,
        ) {
            PhotoGrid()
        }
    }
}

/**
 * The album navigation list: the body of the Gallery's side pane (`layout/app_side_pane_fragment.xml`
 * over `layout/app_side_pane_header.xml` and `layout/app_side_pane_item.xml`) — the three top
 * entries, a hairline, the pinned-album title, then the album rows.
 */
@Composable
private fun AlbumNavigationContent(onOpenAlbum: (GalleryAlbum) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(COUITheme.colorScheme.surface)
            // The device host puts a collapsing app bar above the list; the demo offsets the list
            // below the status bar instead.
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = NavigationContentTopPadding),
    ) {
        NavigationRow(icon = COUIIcons.Photos, label = "照片", onClick = {})
        NavigationRow(icon = COUIIcons.Album, label = "图集", onClick = {})
        NavigationRow(icon = COUIIcons.Create, label = "创作", onClick = {})

        // app_side_pane_header.xml divider: toolbar_divider_height tall, couiColorDivider colored,
        // 12dp from either side, 9.669983dp below the last entry and 14dp above the pinned title.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SidePaneItemMarginStart,
                    end = SidePaneItemMarginEnd,
                    top = SidePaneHeaderDividerMarginTop,
                    bottom = SidePaneHeaderDividerMarginBottom,
                )
                .height(SidePaneDividerHeight)
                .background(COUITheme.colorScheme.dividerLine),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SidePaneItemMarginStart,
                    end = SidePaneItemMarginEnd,
                    bottom = SidePaneItemMarginBottom,
                )
                .height(SidePaneItemMinHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "固定图集",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = SidePaneItemTextMarginStart),
                fontSize = SidePaneItemTitleSize,
                fontWeight = FontWeight.SemiBold,
                color = COUITheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = COUIIcons.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(SidePaneItemIconGlyphSize),
                tint = COUITheme.colorScheme.onSurface,
            )
        }

        GalleryAlbums.forEach { album ->
            AlbumNavigationRow(album = album, onClick = { onOpenAlbum(album) })
        }
    }
}

@Composable
private fun NavigationRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(
                start = SidePaneItemMarginStart,
                end = SidePaneItemMarginEnd,
                bottom = SidePaneItemMarginBottom,
            )
            .height(SidePaneItemMinHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(SidePaneItemIconSize),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(SidePaneItemIconGlyphSize),
                tint = COUITheme.colorScheme.onSurface,
            )
        }
        Text(
            text = label,
            modifier = Modifier
                .weight(1f)
                .padding(start = SidePaneItemTextMarginStart),
            fontSize = SidePaneItemTextSize,
            fontWeight = FontWeight.Medium,
            color = COUITheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AlbumNavigationRow(album: GalleryAlbum, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(
                start = SidePaneItemMarginStart,
                end = SidePaneItemMarginEnd,
                bottom = SidePaneItemMarginBottom,
            )
            .height(SidePaneItemMinHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(SidePaneItemIconSize),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = album.icon,
                contentDescription = null,
                modifier = Modifier.size(SidePaneItemIconGlyphSize),
                tint = COUITheme.colorScheme.onSurface,
            )
        }
        Text(
            text = album.name,
            modifier = Modifier
                .weight(1f)
                .padding(start = SidePaneItemTitleTextMarginStart),
            fontSize = SidePaneItemTextSize,
            fontWeight = FontWeight.Medium,
            color = COUITheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AlbumGrid(padding: PaddingValues, onOpenAlbum: (GalleryAlbum) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(GalleryAlbums, key = { it.name }) { album ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Button) { onOpenAlbum(album) }
                        .background(Brush.linearGradient(album.cover)),
                )
                Text(
                    text = album.name,
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = COUITheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${album.count} 张",
                    fontSize = 12.sp,
                    color = COUITheme.colorScheme.onSurfaceSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PhotoGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(PhotoPlaceholders) { cover ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Brush.linearGradient(cover)),
            )
        }
    }
}

/** `app_side_pane_item_min_height` — 44dp. */
private val SidePaneItemMinHeight = 44.dp

/** `app_side_pane_item_margin_bottom` — 4dp. */
private val SidePaneItemMarginBottom = 4.dp

/** `app_side_pane_item_margin_start` — 12dp. */
private val SidePaneItemMarginStart = 12.dp

/** `app_side_pane_recyclerview_margin_end` — 12dp. */
private val SidePaneItemMarginEnd = 12.dp

/** `app_side_pane_item_icon_width` / `_icon_height` — 44dp. */
private val SidePaneItemIconSize = 44.dp

/** `app_side_pane_item_icon_no_padding_width` / `_height` — 24dp. */
private val SidePaneItemIconGlyphSize = 24.dp

/** `app_side_pane_item_text_size` — 16sp. */
private val SidePaneItemTextSize = 16.sp

/** `app_side_pane_item_title_size` — 18sp. */
private val SidePaneItemTitleSize = 18.sp

/** `app_side_pane_item_text_margin_start` — 10dp. */
private val SidePaneItemTextMarginStart = 10.dp

/** `app_side_pane_item_title_text_margin_start` — 10dp. */
private val SidePaneItemTitleTextMarginStart = 10.dp

/** `toolbar_divider_height` — 0.33dp, the same hairline the drawer divider uses. */
private val SidePaneDividerHeight = 0.33.dp

/** `app_side_pane_header_divider_line_margin_top` — 9.669983dp. */
private val SidePaneHeaderDividerMarginTop = 9.669983.dp

/** `app_side_pane_header_divider_line_margin_bottom` — 14dp. */
private val SidePaneHeaderDividerMarginBottom = 14.dp

/** Offset of the album list below the status bar, standing in for the host's collapsing app bar. */
private val NavigationContentTopPadding = 56.dp

/** One album of the demo Gallery. */
private data class GalleryAlbum(
    val name: String,
    val count: Int,
    val icon: ImageVector,
    val cover: List<Color>,
)

/**
 * The demo album list. The names are page content rather than design tokens, and the covers are
 * gradients because the demo ships no photo assets.
 */
private val GalleryAlbums = listOf(
    GalleryAlbum("相机", 412, COUIIcons.Photos, listOf(Color(0xFF2F4858), Color(0xFF86BBD8))),
    GalleryAlbum("截屏", 96, COUIIcons.ScreenCapture, listOf(Color(0xFF33658A), Color(0xFF86BBD8))),
    GalleryAlbum("下载", 58, COUIIcons.Download, listOf(Color(0xFF3B5249), Color(0xFFA3C9A8))),
    GalleryAlbum("视频", 24, COUIIcons.Play, listOf(Color(0xFF4A3B52), Color(0xFFB8A3C9))),
    GalleryAlbum("收藏", 17, COUIIcons.Favorites, listOf(Color(0xFF5B3A3A), Color(0xFFD9A5A5))),
    GalleryAlbum("最近", 203, COUIIcons.Recent, listOf(Color(0xFF3A4A5B), Color(0xFF9AAECF))),
)

private val PhotoPlaceholders: List<List<Color>> = List(24) { index ->
    val cover = GalleryAlbums[index % GalleryAlbums.size].cover
    if (index % 2 == 0) cover else cover.reversed()
}
