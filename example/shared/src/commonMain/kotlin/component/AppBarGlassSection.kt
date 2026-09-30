// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar
import io.github.suqi8.coui.kmp.basic.TabRowWithContour
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.couiTopAppBarActionItem
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Back
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.blur_test_bg
import org.jetbrains.compose.resources.painterResource
import utils.WallpaperState
import utils.rememberBlurBackdrop

fun LazyListScope.appBarGlassSection() {
    item(key = "appBarGlass") {
        SmallTitle(text = "TopAppBar (glass)")
        TopAppBarGlassDemo()
    }

    item(key = "appBarForms") {
        SmallTitle(text = "TopAppBar (ColorOS 17 forms)")
        TopAppBarColorOS17FormsDemo()
    }
}

/** Renders the "TopAppBar (glass)" gallery section content: a small top app bar over a blurred wallpaper. */
@Composable
fun TopAppBarGlassDemo() {
    val backdrop = rememberBlurBackdrop()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
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
                SmallTopAppBar(
                    title = "TopAppBar",
                    subtitle = "glass",
                    defaultWindowInsetsPadding = false,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    repeat(12) { index ->
                        Text(
                            text = "Scroll row $index",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Renders the "TopAppBar (ColorOS 17 forms)" gallery section content: the centred, segment and gradient glass bars. */
@Composable
fun TopAppBarColorOS17FormsDemo() {
    val backdrop = rememberBlurBackdrop()
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
    ) {
        // A sibling of the bars, never an ancestor: the bar reads this layer while the layer is
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
                // G1: the title is centred, with the responsive 16 / 24 / 40dp side clearance.
                SmallTopAppBar(
                    title = "Center title",
                    centerTitle = true,
                    defaultWindowInsetsPadding = false,
                    navigationIcon = {
                        IconButton(onClick = { /* Handle click event */ }) {
                            Icon(COUIIcons.Back, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // coui_action_menu_item_min_width: the bar cannot force this, so the
                        // action item opts in.
                        IconButton(
                            onClick = { /* Handle click event */ },
                            modifier = Modifier.couiTopAppBarActionItem(),
                        ) {
                            Icon(COUIIcons.More, contentDescription = "More")
                        }
                    },
                )
                // G2: the inline 32dp segment slot; mutually exclusive with the centred title.
                SmallTopAppBar(
                    title = "Segment",
                    defaultWindowInsetsPadding = false,
                    segmentButton = {
                        TabRowWithContour(
                            tabs = listOf("Tab", "Tab"),
                            selectedTabIndex = selectedTab,
                            onTabSelected = { selectedTab = it },
                            height = 32.dp,
                        )
                    },
                    actions = {
                        IconButton(onClick = { /* Handle click event */ }) {
                            Icon(COUIIcons.More, contentDescription = "More")
                        }
                    },
                )
                // G13: the uniform blur is replaced by the 30px -> 0 progressive blur.
                SmallTopAppBar(
                    title = "Gradient glass",
                    defaultWindowInsetsPadding = false,
                    gradientGlass = true,
                )
            }
        }
    }
}
