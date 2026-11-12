// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.ChipTabData
import io.github.suqi8.coui.kmp.basic.ChipTabGroup
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TopAppBarSearchBar
import io.github.suqi8.coui.kmp.basic.TwoRowTopAppBar
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.blur_test_bg
import org.jetbrains.compose.resources.painterResource
import utils.WallpaperState
import utils.rememberBlurBackdrop

private val twoRowDemoTabs = listOf(
    ChipTabData(label = "视频"),
    ChipTabData(label = "用户"),
    ChipTabData(label = "专栏"),
)

/**
 * `TwoRowTopAppBar`: a search field and a chip row over one sheet of glass, the Notes bar's shape.
 *
 * The chip row is the bar's `secondRow`, never a material of its own — that is what keeps the bar a
 * single gradient down to the chips' bottom edge.
 */
@Composable
fun TwoRowTopAppBarDemo() {
    val backdrop = rememberBlurBackdrop()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
    ) {
        // A sibling of the bar, never an ancestor: the bar reads this layer while the layer is
        // being recorded otherwise.
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
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            Column(modifier = Modifier.fillMaxSize()) {
                TwoRowTopAppBar(
                    modifier = Modifier.fillMaxWidth(),
                    secondRow = {
                        ChipTabGroup(
                            tabs = twoRowDemoTabs,
                            selectedIndex = selected,
                            onSelectedChange = { selected = it },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            backdrop = LocalBackdrop.current,
                        )
                    },
                ) {
                    TopAppBarSearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        expanded = true,
                        onExpandedChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = str("Search"),
                        cancelText = null,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    repeat(8) { index ->
                        Text(
                            text = str("Scroll row {}", index),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}
