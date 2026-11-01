// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.BottomFloatingToolbar
import io.github.suqi8.coui.kmp.basic.BottomFloatingToolbarGroup
import io.github.suqi8.coui.kmp.basic.BottomFloatingToolbarItem
import io.github.suqi8.coui.kmp.basic.Switch
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Add
import io.github.suqi8.coui.kmp.icon.extended.Favorites
import io.github.suqi8.coui.kmp.icon.extended.GridView
import io.github.suqi8.coui.kmp.icon.extended.Layers
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.blur_test_bg
import io.github.suqi8.coui.kmp.theme.COUITheme
import org.jetbrains.compose.resources.painterResource
import utils.WallpaperState
import utils.rememberBlurBackdrop

/** A toolbar of icon-only items over a page of placeholder text. */
@Composable
fun IconToolbarDemo(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    val backdrop = rememberBlurBackdrop()
    val lineColor = COUITheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.18f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clipToBounds(),
    ) {
        // The capsule samples this page, so it stays a sibling of the toolbar, not an ancestor.
        Column(
            modifier = Modifier
                .matchParentSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .background(COUITheme.colorScheme.surfaceContainer)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            repeat(6) { line ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (line % 3 == 2) 0.6f else 1f)
                        .height(10.dp)
                        .background(lineColor),
                )
            }
        }
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            BottomFloatingToolbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                visible = visible,
            ) {
                BottomFloatingToolbarGroup {
                    BottomFloatingToolbarItem(label = str("Item {}", 1), icon = COUIIcons.Favorites, onClick = {})
                    BottomFloatingToolbarItem(label = str("Item {}", 2), icon = COUIIcons.Layers, onClick = {})
                    BottomFloatingToolbarItem(label = str("Item {}", 3), icon = COUIIcons.GridView, onClick = {})
                    BottomFloatingToolbarItem(label = str("Item {}", 4), icon = COUIIcons.Add, onClick = {})
                }
            }
        }
    }
}

/** Renders the "BottomFloatingToolbar" gallery section content: the Notes toolbar, then two floating toolbars over a blurred wallpaper plus a visibility switch. */
@Composable
fun BottomFloatingToolbarDemo() {
    var visible by remember { mutableStateOf(true) }
    val backdrop = rememberBlurBackdrop()

    IconToolbarDemo(visible = visible)
    Spacer(Modifier.height(12.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clipToBounds(),
    ) {
        // The capsules sample this layer, so it stays a sibling of the toolbars, not an ancestor.
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
        ) {
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
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                BottomFloatingToolbar(visible = visible) {
                    BottomFloatingToolbarGroup {
                        BottomFloatingToolbarItem(label = str("Item {}", 1), icon = COUIIcons.Favorites, onClick = {})
                        BottomFloatingToolbarItem(label = str("Item {}", 2), icon = COUIIcons.Layers, onClick = {})
                        BottomFloatingToolbarItem(
                            label = str("Item {}", 3),
                            icon = COUIIcons.GridView,
                            onClick = {},
                            enabled = false,
                        )
                    }
                }
                BottomFloatingToolbar(visible = visible) {
                    BottomFloatingToolbarGroup {
                        BottomFloatingToolbarItem(label = str("Item {}", 1), icon = COUIIcons.Add, onClick = {})
                        BottomFloatingToolbarItem(label = str("Item {}", 2), onClick = {})
                    }
                    BottomFloatingToolbarGroup {
                        BottomFloatingToolbarItem(label = str("Item {}", 3), icon = COUIIcons.More, onClick = {})
                    }
                }
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = str("Visible"))
        Switch(checked = visible, onCheckedChange = { visible = it })
    }
}
