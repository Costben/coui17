// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBar
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.BottomFloatingRedDot
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.ScrollBehavior
import io.github.suqi8.coui.kmp.basic.SnackbarHost
import io.github.suqi8.coui.kmp.basic.SnackbarHostState
import io.github.suqi8.coui.kmp.basic.TopAppBar
import io.github.suqi8.coui.kmp.basic.couiGlassSurface
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Add
import io.github.suqi8.coui.kmp.icon.extended.Home
import io.github.suqi8.coui.kmp.icon.extended.Messages
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Settings
import io.github.suqi8.coui.kmp.menu.OverlayIconDropdownMenu
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.ColorSchemeMode
import io.github.suqi8.coui.kmp.theme.ThemeController
import kotlin.math.max

/**
 * COUI 17 — a ColorOS 17 settings screen.
 *
 * Everything is placed where it would live in the real app: the overflow menu hangs off the three
 * dots in the app bar, search suggestions open under the search field, and the floating navigation
 * bar carries the ColorOS 17 melt ball.
 */
@Composable
fun Coui17App() {
    var colorMode by remember { mutableIntStateOf(1) }
    val dark = colorMode == 2
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
        SettingsScaffold(
            darkMode = dark,
            onDarkModeChange = { colorMode = if (it) 2 else 1 },
        )
    }
}

@Composable
private fun SettingsScaffold(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
) {
    val wallpaper = rememberWallpaper()
    val surfaceColor = COUITheme.colorScheme.surface
    // The recorded layer is what the light-sensing surfaces sample. It has to be a sibling of every
    // glass surface, never their ancestor, or the blur would sample itself.
    val backdrop = rememberLayerBackdrop {
        drawWallpaper(surfaceColor, wallpaper)
        drawContent()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = COUIScrollBehavior()
    var meltBallExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                SettingsTopBar(scrollBehavior = scrollBehavior)
            }
        },
        bottomBar = {
            SettingsBottomBar(
                backdrop = backdrop,
                expanded = meltBallExpanded,
                onExpandedChange = { meltBallExpanded = it },
            )
        },
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            SettingsScreen(
                padding = padding,
                scrollBehavior = scrollBehavior,
                snackbarHostState = snackbarHostState,
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange,
                meltBallExpanded = meltBallExpanded,
                onMeltBallExpandedChange = { meltBallExpanded = it },
            )
        }
    }
}

/** The ColorOS 17 gradient-glass app bar, with the overflow menu hanging off its three dots. */
@Composable
private fun SettingsTopBar(scrollBehavior: ScrollBehavior) {
    var sortIndex by remember { mutableIntStateOf(0) }
    var viewIndex by remember { mutableIntStateOf(0) }
    val entries = remember(sortIndex, viewIndex) {
        listOf(
            DropdownEntry(
                items = listOf("按名称排序", "按修改时间排序", "按大小排序")
                    .mapIndexed { index, text ->
                        DropdownItem(
                            text = text,
                            selected = sortIndex == index,
                            onClick = { sortIndex = index },
                        )
                    },
            ),
            DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = "显示模式",
                        children = listOf("标准", "紧凑").mapIndexed { index, text ->
                            DropdownItem(
                                text = text,
                                selected = viewIndex == index,
                                onClick = { viewIndex = index },
                            )
                        },
                    ),
                    DropdownItem(
                        text = "筛选",
                        children = listOf(
                            DropdownItem(text = "全部"),
                            DropdownItem(text = "仅已启用"),
                        ),
                    ),
                ),
            ),
        )
    }

    TopAppBar(
        title = "设置",
        scrollBehavior = scrollBehavior,
        gradientGlass = true,
        actions = {
            OverlayIconDropdownMenu(
                entries = entries,
                content = {
                    Icon(imageVector = COUIIcons.More, contentDescription = "更多")
                },
            )
        },
    )
}

/** The floating capsule with the melt ball that melts out of it. */
@Composable
private fun SettingsBottomBar(
    backdrop: Backdrop?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    var selected by remember { mutableIntStateOf(2) }
    val barColor = COUITheme.colorScheme.bar

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
    ) {
        BottomFloatingNavigationBar(
            backdrop = backdrop,
            expanded = expanded,
            expansion = {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .couiGlassSurface(
                            backdrop = backdrop,
                            shape = CircleShape,
                            containerColor = barColor,
                            fallback = Modifier.background(barColor, CircleShape),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = COUIIcons.Add, contentDescription = "新建")
                }
            },
        ) {
            BottomFloatingNavigationBarItem(
                selected = selected == 0,
                onClick = { selected = 0 },
                icon = COUIIcons.Home,
                label = "首页",
            )
            BottomFloatingNavigationBarItem(
                selected = selected == 1,
                onClick = { selected = 1 },
                icon = COUIIcons.Messages,
                label = "消息",
                redDot = BottomFloatingRedDot.Count(3),
            )
            BottomFloatingNavigationBarItem(
                selected = selected == 2,
                onClick = { selected = 2 },
                icon = COUIIcons.Settings,
                label = "设置",
            )
            BottomFloatingNavigationBarItem(
                selected = expanded,
                onClick = { onExpandedChange(!expanded) },
                icon = COUIIcons.Add,
                label = "更多",
            )
        }
    }
}

/**
 * Paints the wallpaper into the backdrop layer, cover-fitted and centred, so the glass above it
 * refracts the same picture the user sees.
 */
private fun ContentDrawScope.drawWallpaper(
    surfaceColor: Color,
    wallpaper: ImageBitmap?,
) {
    drawRect(surfaceColor)
    if (wallpaper == null) {
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
