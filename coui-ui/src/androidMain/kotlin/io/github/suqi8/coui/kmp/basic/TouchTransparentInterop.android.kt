// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(InternalCouiBlurApi::class)

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
import io.github.suqi8.coui.kmp.blur.InternalCouiBlurApi
import io.github.suqi8.coui.kmp.blur.LayerBackdrop

/**
 * Draws [content] — a purely decorative platform view — without letting it take touches.
 *
 * An `AndroidView` is a hit target over its whole bounds, and its pointer-input sharing only reaches
 * its own siblings, so the page under a floating bar never saw a swipe that started on the bar's
 * shadow or material host, which reaches well past the capsule. Clipping the host to nothing takes
 * it out of hit testing; its drawing is recorded inside the clip and drawn again outside it, so it
 * still shows exactly where it was.
 *
 * The recorded drawing is left out of [LayerBackdrop] recording passes. The view carries a ColorOS
 * window-surface background effect, and drawing it into a backdrop layer too made the effect lose
 * its blur for a few frames whenever that backdrop stopped recording — the bar flashed each time a
 * popup or dialog reading the backdrop closed.
 *
 * [drawTrigger] is any value whose change must re-record the view, such as its geometry or a tick
 * bumped when the view turns visible; without it the first, still empty recording sticks until
 * something else redraws the bar.
 */
@Composable
internal fun TouchTransparentInterop(
    modifier: Modifier = Modifier,
    drawTrigger: Any? = null,
    content: @Composable () -> Unit,
) {
    val picture = rememberGraphicsLayer()
    Box(
        modifier.drawWithContent {
            if (!LayerBackdrop.isRecording) drawLayer(picture)
        },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    clip = true
                    shape = OutOfReach
                }
                .drawWithContent {
                    // Read so a new trigger value rebuilds this lambda and re-records the view.
                    drawTrigger.hashCode()
                    picture.record { this@drawWithContent.drawContent() }
                },
        ) {
            content()
        }
    }
}

/** A clip that leaves nothing inside it, which also takes the node out of hit testing. */
private val OutOfReach = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = Outline.Rectangle(Rect.Zero)
}
