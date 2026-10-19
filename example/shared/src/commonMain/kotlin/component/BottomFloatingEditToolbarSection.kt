// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.BottomFloatingEditToolbar
import io.github.suqi8.coui.kmp.basic.BottomFloatingEditToolbarItem
import io.github.suqi8.coui.kmp.basic.BottomFloatingRedDot
import io.github.suqi8.coui.kmp.basic.Switch
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Copy
import io.github.suqi8.coui.kmp.icon.extended.Delete
import io.github.suqi8.coui.kmp.icon.extended.Share
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.blur_test_bg
import org.jetbrains.compose.resources.painterResource
import utils.WallpaperState
import utils.rememberBlurBackdrop

/** Renders the "BottomFloatingEditToolbar" gallery section content (the floating edit toolbar over a blurred wallpaper). */
@Composable
fun BottomFloatingEditToolbarDemo() {
    var visible by remember { mutableStateOf(true) }
    val backdrop = rememberBlurBackdrop()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        // The capsule samples this layer, so it stays a sibling of the toolbar, not an ancestor.
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
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
        ) {
            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                BottomFloatingEditToolbar(visible = visible) {
                    BottomFloatingEditToolbarItem(
                        onClick = {},
                        icon = COUIIcons.Copy,
                        label = str("Copy"),
                    )
                    BottomFloatingEditToolbarItem(
                        onClick = {},
                        icon = COUIIcons.Delete,
                        label = str("Delete"),
                        redDot = BottomFloatingRedDot.Count(3),
                    )
                    BottomFloatingEditToolbarItem(
                        onClick = {},
                        icon = COUIIcons.Share,
                        label = str("Share"),
                        enabled = false,
                    )
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
