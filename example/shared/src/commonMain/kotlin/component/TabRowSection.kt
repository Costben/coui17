// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.LocalEdgeToEdge
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar
import io.github.suqi8.coui.kmp.basic.Switch
import io.github.suqi8.coui.kmp.basic.TabRow
import io.github.suqi8.coui.kmp.basic.TabRowDefaults
import io.github.suqi8.coui.kmp.basic.TabRowWithContour
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Back
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Scan
import io.github.suqi8.coui.kmp.squircle.addSquircleRoundRect
import io.github.suqi8.coui.kmp.squircle.squircleClip
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch

/** Renders the "TabRow" gallery section content: a frameless tab row, the frosted glass segment bar, and one contour tab row whose style comes from the option. */
@Composable
fun TabRowDemo() {
    val framelessTabs = listOf(str("Day"), str("Week"), str("Month"))
    var framelessSelected by remember { mutableIntStateOf(0) }
    val styleIndex = remember { mutableIntStateOf(0) }

    TabRow(
        tabs = framelessTabs,
        selectedTabIndex = framelessSelected,
        onTabSelected = { framelessSelected = it },
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
    )
    ComputerLinkGlassTabDemo()
    DemoOptions(DemoChoice("Style", listOf("Contour", "Opaque", "Tiny"), styleIndex))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        insideMargin = PaddingValues(16.dp),
    ) {
        // COUI segment button, as used by the special access page in ColorOS Settings. The option
        // picks the contour tier, the opaque Privacy & security capsule, or the 32dp tiny form.
        val segmentTabs = listOf(str("All"), str("Allowed"), str("Denied"))
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(pageCount = { segmentTabs.size })
        val tiny = styleIndex.intValue == 2
        TabRowWithContour(
            tabs = segmentTabs,
            selectedTabIndex = pagerState.currentPage,
            onTabSelected = {
                scope.launch {
                    pagerState.animateScrollToPage(it)
                }
            },
            colors = if (styleIndex.intValue == 1) {
                TabRowDefaults.opaqueTabRowColors()
            } else {
                TabRowDefaults.tabRowColors()
            },
            height = if (tiny) TabRowDefaults.TabRowWithContourTinyHeight else TabRowDefaults.TabRowWithContourHeight,
            cornerRadius = if (tiny) {
                TabRowDefaults.TabRowWithContourTinyCornerRadius
            } else {
                TabRowDefaults.TabRowWithContourCornerRadius
            },
            contourPadding = if (tiny) {
                TabRowDefaults.TabRowWithContourTinyPadding
            } else {
                TabRowDefaults.TabRowWithContourPadding
            },
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .padding(bottom = 16.dp),
            userScrollEnabled = true,
            key = { it },
            pageContent = { page ->
                Text(
                    text = str("Content of {}", segmentTabs[page]),
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            },
        )
    }
}

/**
 * Replicates the ColorOS 17 OPSynergy `ComputerLinkTabLayoutFragment` ("与电脑互联") frosted glass
 * segment bar: a `COUIAppBarLayout` (`OConnectSmallTabBehavior` progressive blur on scroll) hosting
 * a toolbar with circular glass buttons and a `COUISegmentButtonLayout` with
 * `setMaterialEffectEnabled(true)` (`TYPE_CONTENT_SEGMENT_BUTTON` 25px blur +
 * `TYPE_FRAMEWORK_CAPSULE_6` outer rim + `TYPE_FRAMEWORK_CAPSULE_10` indicator rim).
 */
@Composable
private fun ComputerLinkGlassTabDemo() {
    val tabs = listOf(str("Cross-screen connect"), str("Remote control PC"))
    var selectedTab by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState(initial = 110)
    val surfaceColor = COUITheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    var autoConnect by remember { mutableStateOf(true) }
    var clipboardSync by remember { mutableStateOf(true) }
    var notificationSync by remember { mutableStateOf(false) }

    SmallTitle(text = str("Connect to PC"))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .height(296.dp)
            .squircleClip(24.dp)
            .background(surfaceColor),
    ) {
        // Underlying scrollable ViewPager2 content (`clipToPadding = false`, `paddingTop = appBarHeight`)
        // that passes underneath the floating AppBar + COUISegmentButtonLayout.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .verticalScroll(scrollState)
                .padding(top = 120.dp, start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ComputerLinkHeroBanner(selectedTab = selectedTab)
            Text(
                text = if (selectedTab == 0) {
                    str("Mirror phone screen, drag files and share clipboard with your PC")
                } else {
                    str("Control your Windows or Mac desktop remotely from your phone")
                },
                style = COUITheme.textStyles.body2,
                color = COUITheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ComputerLinkSettingRow(
                        title = str("Auto-connect nearby PC"),
                        checked = autoConnect,
                        onCheckedChange = { autoConnect = it },
                    )
                    ComputerLinkSettingRow(
                        title = str("Clipboard sync"),
                        checked = clipboardSync,
                        onCheckedChange = { clipboardSync = it },
                    )
                    ComputerLinkSettingRow(
                        title = str("Notification sync"),
                        checked = notificationSync,
                        onCheckedChange = { notificationSync = it },
                    )
                }
            }
        }

        // Floating COUIAppBarLayout + OConnectSmallTabBehavior progressive blur + COUISegmentButtonLayout
        CompositionLocalProvider(
            LocalBackdrop provides backdrop,
            LocalEdgeToEdge provides true,
        ) {
            SmallTopAppBar(
                title = str("Connect to PC"),
                color = surfaceColor,
                defaultWindowInsetsPadding = false,
                showDivider = false,
                gradientGlass = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                navigationIcon = {
                    HighlightedIconButton(
                        onClick = {},
                        backdrop = backdrop,
                    ) {
                        Icon(
                            imageVector = COUIIcons.Back,
                            contentDescription = str("Connect to PC"),
                        )
                    }
                },
                actions = {
                    HighlightedIconButton(
                        onClick = {},
                        backdrop = backdrop,
                    ) {
                        Icon(
                            imageVector = COUIIcons.Scan,
                            contentDescription = null,
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    HighlightedIconButton(
                        onClick = {},
                        backdrop = backdrop,
                    ) {
                        Icon(
                            imageVector = COUIIcons.More,
                            contentDescription = null,
                        )
                    }
                },
                bottomContent = {
                    // `COUISegmentButtonLayout` with `setMaterialEffectEnabled(true)` (`marginTop = 12dp`)
                    TabRowWithContour(
                        tabs = tabs,
                        selectedTabIndex = selectedTab,
                        onTabSelected = { selectedTab = it },
                        backdrop = backdrop,
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun ComputerLinkHeroBanner(selectedTab: Int) {
    val blockPath = remember { Path() }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .squircleClip(20.dp),
    ) {
        // Blue/cyan PC screen illustration matching the OPSynergy "与电脑互联" illustration
        drawRect(
            brush = Brush.verticalGradient(
                colors = if (selectedTab == 0) {
                    listOf(Color(0xFF4295D6), Color(0xFF2CC5C9))
                } else {
                    listOf(Color(0xFF3E7BD6), Color(0xFF5A54D1))
                },
            ),
        )
        val screenW = size.width * 0.68f
        val screenH = size.height * 0.74f
        val screenLeft = (size.width - screenW) / 2f
        val screenTop = (size.height - screenH) / 2f
        blockPath.reset()
        blockPath.addSquircleRoundRect(
            rect = Rect(Offset(screenLeft, screenTop), Size(screenW, screenH)),
            topLeft = 14f,
            topRight = 14f,
            bottomRight = 14f,
            bottomLeft = 14f,
        )
        drawPath(path = blockPath, color = Color(0xFF283C4C))
        // Sidebar inside the PC monitor
        blockPath.reset()
        blockPath.addSquircleRoundRect(
            rect = Rect(Offset(screenLeft + 12f, screenTop + 12f), Size(screenW * 0.18f, screenH - 24f)),
            topLeft = 6f,
            topRight = 6f,
            bottomRight = 6f,
            bottomLeft = 6f,
        )
        drawPath(path = blockPath, color = Color(0xFF3E5566))
        // Warm orange & red app tiles near the top of the monitor (visible through the frosted segment bar)
        val tileY = screenTop + 16f
        val tileColors = listOf(
            Color(0xFFFF9800),
            Color(0xFFFFB300),
            Color(0xFF4CAF50),
            Color(0xFFE53935),
            Color(0xFFFF5252),
        )
        tileColors.forEachIndexed { idx, color ->
            blockPath.reset()
            blockPath.addSquircleRoundRect(
                rect = Rect(Offset(screenLeft + screenW * 0.26f + idx * 34f, tileY), Size(22f, 22f)),
                topLeft = 6f,
                topRight = 6f,
                bottomRight = 6f,
                bottomLeft = 6f,
            )
            drawPath(path = blockPath, color = color)
        }
    }
}

@Composable
private fun ComputerLinkSettingRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = COUITheme.textStyles.body1,
            color = COUITheme.colorScheme.onSurface,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
