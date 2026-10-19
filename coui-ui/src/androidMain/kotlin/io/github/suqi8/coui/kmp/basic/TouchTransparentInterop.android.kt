// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Draws [content] — a purely decorative platform view — without letting it take touches.
 *
 * An `AndroidView` is a hit target over its whole bounds, and its pointer-input sharing only reaches
 * its own siblings, so the page under a floating bar never saw a swipe that started on the bar's
 * shadow or material host, which reaches well past the capsule. Clipping the host to nothing takes
 * it out of hit testing; its drawing is recorded inside the clip and drawn again outside it, so it
 * still shows exactly where it was.
 */
@Composable
internal fun TouchTransparentInterop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val picture = rememberGraphicsLayer()
    Box(modifier.drawWithContent { drawLayer(picture) }) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    clip = true
                    shape = OutOfReach
                }
                .drawWithContent { picture.record { this@drawWithContent.drawContent() } },
        ) {
            content()
        }
    }
}

/** A clip that leaves nothing inside it, which also takes the node out of hit testing. */
private val OutOfReach = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = Outline.Rectangle(Rect.Zero)
}
