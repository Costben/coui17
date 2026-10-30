// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * Fades the content out over [fadeWidth] at whichever horizontal edge [scrollState] can still scroll
 * towards, the way COUI's horizontally scrolling groups mask their clipped edges.
 *
 * The fade erases the content itself with `DST_OUT` instead of painting a background-coloured
 * gradient over it, so it works on any background; the element gets an offscreen layer of its own
 * because `DST_OUT` can only erase what it is composited against.
 */
internal fun Modifier.scrollEdgeFade(scrollState: ScrollState, fadeWidth: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        if (scrollState.maxValue <= 0) return@drawWithContent
        val fadePx = fadeWidth.toPx().coerceAtMost(size.width / 2f)
        if (fadePx <= 0f) return@drawWithContent
        val rtl = layoutDirection == LayoutDirection.Rtl
        val fadeLeft = if (rtl) scrollState.canScrollForward else scrollState.canScrollBackward
        val fadeRight = if (rtl) scrollState.canScrollBackward else scrollState.canScrollForward
        if (fadeLeft) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    startX = 0f,
                    endX = fadePx,
                ),
                size = Size(fadePx, size.height),
                blendMode = BlendMode.DstOut,
            )
        }
        if (fadeRight) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White),
                    startX = size.width - fadePx,
                    endX = size.width,
                ),
                topLeft = Offset(size.width - fadePx, 0f),
                size = Size(fadePx, size.height),
                blendMode = BlendMode.DstOut,
            )
        }
    }
