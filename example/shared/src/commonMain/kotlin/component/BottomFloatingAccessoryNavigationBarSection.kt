// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BottomFloatingAccessoryNavigationBar
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.Switch
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Add
import io.github.suqi8.coui.kmp.icon.extended.Home
import io.github.suqi8.coui.kmp.icon.extended.Messages
import io.github.suqi8.coui.kmp.icon.extended.Search
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.blur_test_bg
import org.jetbrains.compose.resources.painterResource
import utils.WallpaperState
import utils.rememberBlurBackdrop

fun LazyListScope.bottomFloatingAccessoryNavigationBarSection() {
    item(key = "bottomFloatingAccessoryNavigationBar") {
        SmallTitle(text = "BottomFloatingAccessoryNavigationBar")
        var selectedIndex by remember { mutableIntStateOf(0) }
        var startVisible by remember { mutableStateOf(true) }
        var endVisible by remember { mutableStateOf(true) }
        val items = remember {
            listOf(
                "Home" to COUIIcons.Home,
                "Messages" to COUIIcons.Messages,
            )
        }
        val backdrop = rememberBlurBackdrop()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            // The blob samples this layer, so it stays a sibling of the bar rather than an ancestor.
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
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                CompositionLocalProvider(LocalBackdrop provides backdrop) {
                    BottomFloatingAccessoryNavigationBar(
                        startAccessoryVisible = startVisible,
                        endAccessoryVisible = endVisible,
                        // The merged blob already carries each accessory's circle, so the slots draw
                        // the icon alone.
                        startAccessory = {
                            Icon(
                                imageVector = COUIIcons.Search,
                                contentDescription = "Search",
                            )
                        },
                        endAccessory = {
                            Icon(
                                imageVector = COUIIcons.Add,
                                contentDescription = "Add",
                            )
                        },
                    ) {
                        items.forEachIndexed { index, (label, icon) ->
                            BottomFloatingNavigationBarItem(
                                selected = selectedIndex == index,
                                onClick = { selectedIndex = index },
                                icon = icon,
                                label = label,
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
            Text(text = "Start accessory")
            Switch(checked = startVisible, onCheckedChange = { startVisible = it })
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "End accessory")
            Switch(checked = endVisible, onCheckedChange = { endVisible = it })
        }
    }
}
