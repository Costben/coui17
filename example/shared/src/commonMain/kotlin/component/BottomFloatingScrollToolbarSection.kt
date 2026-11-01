// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.BottomFloatingScrollToolbar
import io.github.suqi8.coui.kmp.basic.BottomFloatingScrollToolbarItem
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Add
import io.github.suqi8.coui.kmp.icon.extended.Favorites
import io.github.suqi8.coui.kmp.icon.extended.Filter
import io.github.suqi8.coui.kmp.icon.extended.GridView
import io.github.suqi8.coui.kmp.icon.extended.Image
import io.github.suqi8.coui.kmp.icon.extended.Layers
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Search
import io.github.suqi8.coui.kmp.icon.extended.Tune

/** The dark page the bar is designed to float on. */
private val ToolbarPageColor = Color(0xFF121212)

private val ToolbarItemIcons = listOf(
    COUIIcons.Favorites,
    COUIIcons.Layers,
    COUIIcons.GridView,
    COUIIcons.Add,
    COUIIcons.Search,
    COUIIcons.Filter,
    COUIIcons.Tune,
    COUIIcons.Image,
    COUIIcons.More,
)

/** A scrolling bar of items on a dark page, with the selection following the taps. */
@Composable
fun BottomFloatingScrollToolbarDemo(modifier: Modifier = Modifier) {
    var selected by remember { mutableIntStateOf(1) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(ToolbarPageColor),
        contentAlignment = Alignment.Center,
    ) {
        BottomFloatingScrollToolbar {
            ToolbarItemIcons.forEachIndexed { index, icon ->
                BottomFloatingScrollToolbarItem(
                    onClick = { selected = index },
                    icon = icon,
                    label = str("Item {}", index + 1),
                    selected = index == selected,
                )
            }
        }
    }
}
