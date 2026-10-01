// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import i18n.str
import io.github.suqi8.coui.kmp.basic.MetaBallParticle
import io.github.suqi8.coui.kmp.basic.MetaBallSurface
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.Switch
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.blur_test_bg
import org.jetbrains.compose.resources.painterResource
import utils.WallpaperState
import utils.rememberBlurBackdrop

fun LazyListScope.metaBallSection() {
    item(key = "metaBall") {
        SmallTitle(text = "MetaBallSurface")
        MetaBallSurfaceDemo()
    }
}

/** Renders the "MetaBallSurface" gallery section content: the melt ball over its wallpaper backdrop. */
@Composable
fun MetaBallSurfaceDemo() {
    var merged by remember { mutableStateOf(true) }
    val backdrop = rememberBlurBackdrop()
    val gap by animateDpAsState(
        targetValue = if (merged) 0.dp else 48.dp,
        animationSpec = tween(durationMillis = 420),
    )

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            // Same shape as the floating bar section: the backdrop layer is a sibling, never an
            // ancestor, of the surface that reads it.
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
            // The backdrop is provided, not passed, so the blob is the ColorOS 17 light-sensing
            // glass melt ball: the merged silhouette masks the blur of the mountain behind it.
            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                MetaBallSurface(
                    particles = listOf(
                        // A capsule, the shape COUIBottomFloatingNavigationBar draws.
                        MetaBallParticle(
                            centerX = 104.dp,
                            centerY = 118.dp,
                            width = 144.dp,
                            height = 56.dp,
                        ),
                        // The ball that melts out of it when the bar expands.
                        MetaBallParticle.circle(
                            centerX = 192.dp + gap,
                            centerY = 118.dp,
                            diameter = 56.dp,
                        ),
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = str("Melt into one"))
            Switch(checked = merged, onCheckedChange = { merged = it })
        }
    }
}
