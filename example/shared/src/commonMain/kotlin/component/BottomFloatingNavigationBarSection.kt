// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBar
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
import io.github.suqi8.coui.kmp.icon.extended.Settings
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.blur_test_bg
import io.github.suqi8.coui.kmp.theme.COUITheme
import org.jetbrains.compose.resources.painterResource
import utils.WallpaperState
import utils.rememberBlurBackdrop

fun LazyListScope.bottomFloatingNavigationBarSection() {
    item(key = "bottomFloatingNavigationBar") {
        SmallTitle(text = "BottomFloatingNavigationBar")
        BottomFloatingNavigationBarDemo()
    }
}

/** Renders the "BottomFloatingNavigationBar" gallery section content: a wallpaper-backed floating bar with an expansion-view switch. */
@Composable
fun BottomFloatingNavigationBarDemo() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf(true) }
    val colorScheme = COUITheme.colorScheme
    // Three items: the bar hands the expansion view's footprint plus the side spacing to the
    // capsule as a width bound, so the capsule has to leave that much of the row free.
    val homeLabel = str("Home")
    val messagesLabel = str("Messages")
    val settingsLabel = str("Settings")
    val items = remember(homeLabel, messagesLabel, settingsLabel) {
        listOf(
            homeLabel to COUIIcons.Home,
            messagesLabel to COUIIcons.Messages,
            settingsLabel to COUIIcons.Settings,
        )
    }
    val backdrop = rememberBlurBackdrop()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        // The capsule samples this layer, so the demo gives it content worth refracting. It is a
        // sibling of the bar, not an ancestor: a blur surface nested inside the layer it reads
        // would draw the layer while the layer is still being recorded.
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
                BottomFloatingNavigationBar(
                    expanded = expanded,
                    // The expansion view melts out of the capsule through one merged silhouette,
                    // so it is a plain round surface here rather than its own glass panel.
                    expansion = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = COUIIcons.Add,
                                contentDescription = str("Expand"),
                                tint = colorScheme.onPrimary,
                            )
                        }
                    },
                ) {
                    items.forEachIndexed { index, (label, icon) ->
                        BottomFloatingNavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                            icon = icon,
                            label = label,
                            enabled = index != items.lastIndex,
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
        Text(text = str("Expansion view"))
        Switch(checked = expanded, onCheckedChange = { expanded = it })
    }
}
