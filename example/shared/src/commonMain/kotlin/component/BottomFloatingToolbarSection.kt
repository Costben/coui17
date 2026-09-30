// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BottomFloatingToolbar
import io.github.suqi8.coui.kmp.basic.BottomFloatingToolbarGroup
import io.github.suqi8.coui.kmp.basic.BottomFloatingToolbarItem
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.Switch
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Copy
import io.github.suqi8.coui.kmp.icon.extended.Cut
import io.github.suqi8.coui.kmp.icon.extended.Delete
import io.github.suqi8.coui.kmp.icon.extended.SelectAll
import io.github.suqi8.coui.kmp.icon.extended.Share
import io.github.suqi8.coui.kmp.icon.extended.Translate
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.blur_test_bg
import org.jetbrains.compose.resources.painterResource
import utils.WallpaperState
import utils.rememberBlurBackdrop

fun LazyListScope.bottomFloatingToolbarSection() {
    item(key = "bottomFloatingToolbar") {
        SmallTitle(text = "BottomFloatingToolbar")
        BottomFloatingToolbarDemo()
    }
}

/** Renders the "BottomFloatingToolbar" gallery section content: two floating toolbars over a blurred wallpaper plus a visibility switch. */
@Composable
fun BottomFloatingToolbarDemo() {
    var visible by remember { mutableStateOf(true) }
    val backdrop = rememberBlurBackdrop()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
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
                        BottomFloatingToolbarItem(
                            label = "Copy",
                            icon = COUIIcons.Copy,
                            onClick = {},
                        )
                        BottomFloatingToolbarItem(
                            label = "Cut",
                            icon = COUIIcons.Cut,
                            onClick = {},
                        )
                        BottomFloatingToolbarItem(
                            label = "Delete",
                            icon = COUIIcons.Delete,
                            onClick = {},
                            enabled = false,
                        )
                    }
                }
                BottomFloatingToolbar(visible = visible) {
                    BottomFloatingToolbarGroup {
                        BottomFloatingToolbarItem(
                            label = "Select all",
                            icon = COUIIcons.SelectAll,
                            onClick = {},
                        )
                        BottomFloatingToolbarItem(
                            label = "Translate",
                            onClick = {},
                        )
                    }
                    BottomFloatingToolbarGroup {
                        BottomFloatingToolbarItem(
                            label = "Share",
                            icon = COUIIcons.Share,
                            onClick = {},
                        )
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
        Text(text = "Visible")
        Switch(checked = visible, onCheckedChange = { visible = it })
    }
}
