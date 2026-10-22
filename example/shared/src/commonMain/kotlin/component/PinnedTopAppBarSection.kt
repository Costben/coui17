// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.PinnedScrollBehavior
import io.github.suqi8.coui.kmp.basic.PinnedTopAppBar
import io.github.suqi8.coui.kmp.basic.PinnedTopAppBarDefaults
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.couiTopAppBarActionItem
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.blur_test_bg
import org.jetbrains.compose.resources.painterResource
import utils.WallpaperState
import utils.rememberBlurBackdrop

/**
 * Renders the "PinnedTopAppBar" gallery section content: the ColorOS 17 Gallery-style bar, a large
 * title on the first row with two actions, that stays put while the list scrolls under it and only
 * fades its material in.
 */
@Composable
fun PinnedTopAppBarDemo() {
    val backdrop = rememberBlurBackdrop()
    val scrollBehavior = PinnedScrollBehavior()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
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
            Box(modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
                // The list scrolls under the bar; the bar's own height is the list's top padding.
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Box(modifier = Modifier.height(PinnedTopAppBarDefaults.Height))
                    repeat(16) { index ->
                        Text(
                            text = str("Scroll row {}", index),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
                PinnedTopAppBar(
                    title = str("Photos"),
                    scrollBehavior = scrollBehavior,
                    defaultWindowInsetsPadding = false,
                    actions = {
                        HighlightedIconButton(
                            onClick = { /* Handle click event */ },
                            modifier = Modifier.couiTopAppBarActionItem(),
                        ) {
                            Icon(imageVector = COUIIcons.Basic.Search, contentDescription = str("Search"))
                        }
                        HighlightedIconButton(
                            onClick = { /* Handle click event */ },
                            modifier = Modifier.couiTopAppBarActionItem(),
                        ) {
                            Icon(imageVector = COUIIcons.More, contentDescription = str("More"))
                        }
                    },
                )
            }
        }
    }
}
