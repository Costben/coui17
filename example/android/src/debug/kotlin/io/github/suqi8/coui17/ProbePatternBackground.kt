// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.ceil

/**
 * Stand-in for a photo behind the probe bars, drawn instead of shipped so the repository carries
 * no personal imagery.
 *
 * The bar samples whatever sits behind it, so the backdrop needs hard tile edges, a wide luminance
 * range and no motion of its own: a fixed mosaic over a vertical wash. Every frame is identical,
 * which keeps frame-diff measurements valid.
 *
 * @param bright `true` for the light scene, `false` for the dark one.
 */
@Composable
internal fun ProbePatternBackground(bright: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val tiles = if (bright) BrightTiles else DarkTiles
        drawRect(Brush.verticalGradient(if (bright) BrightWash else DarkWash))

        val columns = 7
        val cell = size.width / columns
        val rows = ceil(size.height / cell).toInt()
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val tile = row * columns + column
                if (tile % 5 == 3) continue
                drawRect(
                    color = tiles[(tile * 7) % tiles.size],
                    topLeft = Offset(column * cell, row * cell),
                    size = Size(cell, cell),
                )
            }
        }

        drawCircle(
            color = if (bright) Color(0x33FFFFFF) else Color(0x22FFFFFF),
            radius = size.minDimension * 0.30f,
            center = Offset(size.width * 0.22f, size.height * 0.28f),
        )
        drawCircle(
            color = if (bright) Color(0x1F000000) else Color(0x33000000),
            radius = size.minDimension * 0.22f,
            center = Offset(size.width * 0.78f, size.height * 0.62f),
        )
    }
}

private val DarkWash = listOf(Color(0xFF0B1016), Color(0xFF243444), Color(0xFF0B1016))

private val BrightWash = listOf(Color(0xFFF7FAFC), Color(0xFFD9E6F2), Color(0xFFF7FAFC))

private val DarkTiles = listOf(
    Color(0xFF16202A),
    Color(0xFF1F3444),
    Color(0xFF2F5062),
    Color(0xFF3F7A6A),
    Color(0xFF6B5A8C),
    Color(0xFFB07A3C),
    Color(0xFFD9E2E8),
    Color(0xFF7FA8C4),
)

private val BrightTiles = listOf(
    Color(0xFFFFFFFF),
    Color(0xFFF0E4D2),
    Color(0xFFD7E7F5),
    Color(0xFFF4D6D6),
    Color(0xFFCBE5CF),
    Color(0xFF9FC2E0),
    Color(0xFFE4C468),
    Color(0xFF8E77A8),
)
