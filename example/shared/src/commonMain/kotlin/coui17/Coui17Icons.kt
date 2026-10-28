// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package coui17

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.unit.dp

/**
 * The gallery's bottom-bar set: each icon is drawn twice, once as a line and once as a filled shape.
 *
 * The pair shares one outline. The line form strokes it at 1.9 on the 24dp grid; the filled form
 * fills the same centreline and turns what the line draws inside it into holes — the doorway of the
 * house, the three colour dots of the palette, the gaps between the four tiles. Filling a centreline
 * leaves the filled glyph half a stroke narrower than its line twin, which is why the two read as
 * the same size when the bar swaps one for the other on selection.
 */
object Coui17Icons {

    val HomeLine: ImageVector by lazy {
        line(
            name = "Coui17.HomeLine",
            pathData = listOf(
                PathNode.MoveTo(12f, 2.75f),
                PathNode.CurveTo(12.46f, 2.75f, 12.9f, 2.9f, 13.27f, 3.18f),
                PathNode.LineTo(20.6f, 8.85f),
                PathNode.CurveTo(21.3f, 9.39f, 21.7f, 10.2f, 21.7f, 11.08f),
                PathNode.LineTo(21.7f, 18.05f),
                PathNode.CurveTo(21.7f, 19.82f, 20.28f, 21.24f, 18.51f, 21.24f),
                PathNode.LineTo(16.2f, 21.24f),
                PathNode.CurveTo(15.87f, 21.24f, 15.6f, 20.97f, 15.6f, 20.64f),
                PathNode.LineTo(15.6f, 17.4f),
                PathNode.ArcTo(3.6f, 3.6f, 0f, false, false, 8.4f, 17.4f),
                PathNode.LineTo(8.4f, 20.64f),
                PathNode.CurveTo(8.4f, 20.97f, 8.13f, 21.24f, 7.8f, 21.24f),
                PathNode.LineTo(5.49f, 21.24f),
                PathNode.CurveTo(3.72f, 21.24f, 2.3f, 19.82f, 2.3f, 18.05f),
                PathNode.LineTo(2.3f, 11.08f),
                PathNode.CurveTo(2.3f, 10.2f, 2.7f, 9.39f, 3.4f, 8.85f),
                PathNode.LineTo(10.73f, 3.18f),
                PathNode.CurveTo(11.1f, 2.9f, 11.54f, 2.75f, 12f, 2.75f),
                PathNode.Close,
            ),
        )
    }

    val HomeFill: ImageVector by lazy {
        solid(
            name = "Coui17.HomeFill",
            pathData = listOf(
                PathNode.MoveTo(12f, 2.75f),
                PathNode.CurveTo(12.46f, 2.75f, 12.9f, 2.9f, 13.27f, 3.18f),
                PathNode.LineTo(20.6f, 8.85f),
                PathNode.CurveTo(21.3f, 9.39f, 21.7f, 10.2f, 21.7f, 11.08f),
                PathNode.LineTo(21.7f, 18.05f),
                PathNode.CurveTo(21.7f, 19.82f, 20.28f, 21.24f, 18.51f, 21.24f),
                PathNode.LineTo(16.2f, 21.24f),
                PathNode.CurveTo(15.87f, 21.24f, 15.6f, 20.97f, 15.6f, 20.64f),
                PathNode.LineTo(15.6f, 17.4f),
                PathNode.ArcTo(3.6f, 3.6f, 0f, false, false, 8.4f, 17.4f),
                PathNode.LineTo(8.4f, 20.64f),
                PathNode.CurveTo(8.4f, 20.97f, 8.13f, 21.24f, 7.8f, 21.24f),
                PathNode.LineTo(5.49f, 21.24f),
                PathNode.CurveTo(3.72f, 21.24f, 2.3f, 19.82f, 2.3f, 18.05f),
                PathNode.LineTo(2.3f, 11.08f),
                PathNode.CurveTo(2.3f, 10.2f, 2.7f, 9.39f, 3.4f, 8.85f),
                PathNode.LineTo(10.73f, 3.18f),
                PathNode.CurveTo(11.1f, 2.9f, 11.54f, 2.75f, 12f, 2.75f),
                PathNode.Close,
            ),
        )
    }

    val GridLine: ImageVector by lazy {
        line(
            name = "Coui17.GridLine",
            pathData = listOf(
                PathNode.MoveTo(5.65f, 2.9f),
                PathNode.LineTo(7.85f, 2.9f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 10.6f, 5.65f),
                PathNode.LineTo(10.6f, 7.85f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 7.85f, 10.6f),
                PathNode.LineTo(5.65f, 10.6f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 2.9f, 7.85f),
                PathNode.LineTo(2.9f, 5.65f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 5.65f, 2.9f),
                PathNode.Close,
                PathNode.MoveTo(16.15f, 2.9f),
                PathNode.LineTo(18.35f, 2.9f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 21.1f, 5.65f),
                PathNode.LineTo(21.1f, 7.85f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 18.35f, 10.6f),
                PathNode.LineTo(16.15f, 10.6f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 13.4f, 7.85f),
                PathNode.LineTo(13.4f, 5.65f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 16.15f, 2.9f),
                PathNode.Close,
                PathNode.MoveTo(5.65f, 13.4f),
                PathNode.LineTo(7.85f, 13.4f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 10.6f, 16.15f),
                PathNode.LineTo(10.6f, 18.35f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 7.85f, 21.1f),
                PathNode.LineTo(5.65f, 21.1f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 2.9f, 18.35f),
                PathNode.LineTo(2.9f, 16.15f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 5.65f, 13.4f),
                PathNode.Close,
                PathNode.MoveTo(16.15f, 13.4f),
                PathNode.LineTo(18.35f, 13.4f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 21.1f, 16.15f),
                PathNode.LineTo(21.1f, 18.35f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 18.35f, 21.1f),
                PathNode.LineTo(16.15f, 21.1f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 13.4f, 18.35f),
                PathNode.LineTo(13.4f, 16.15f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 16.15f, 13.4f),
                PathNode.Close,
            ),
        )
    }

    val GridFill: ImageVector by lazy {
        solid(
            name = "Coui17.GridFill",
            pathData = listOf(
                PathNode.MoveTo(5.65f, 2.9f),
                PathNode.LineTo(7.85f, 2.9f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 10.6f, 5.65f),
                PathNode.LineTo(10.6f, 7.85f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 7.85f, 10.6f),
                PathNode.LineTo(5.65f, 10.6f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 2.9f, 7.85f),
                PathNode.LineTo(2.9f, 5.65f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 5.65f, 2.9f),
                PathNode.Close,
                PathNode.MoveTo(16.15f, 2.9f),
                PathNode.LineTo(18.35f, 2.9f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 21.1f, 5.65f),
                PathNode.LineTo(21.1f, 7.85f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 18.35f, 10.6f),
                PathNode.LineTo(16.15f, 10.6f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 13.4f, 7.85f),
                PathNode.LineTo(13.4f, 5.65f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 16.15f, 2.9f),
                PathNode.Close,
                PathNode.MoveTo(5.65f, 13.4f),
                PathNode.LineTo(7.85f, 13.4f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 10.6f, 16.15f),
                PathNode.LineTo(10.6f, 18.35f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 7.85f, 21.1f),
                PathNode.LineTo(5.65f, 21.1f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 2.9f, 18.35f),
                PathNode.LineTo(2.9f, 16.15f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 5.65f, 13.4f),
                PathNode.Close,
                PathNode.MoveTo(16.15f, 13.4f),
                PathNode.LineTo(18.35f, 13.4f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 21.1f, 16.15f),
                PathNode.LineTo(21.1f, 18.35f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 18.35f, 21.1f),
                PathNode.LineTo(16.15f, 21.1f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 13.4f, 18.35f),
                PathNode.LineTo(13.4f, 16.15f),
                PathNode.ArcTo(2.75f, 2.75f, 0f, false, true, 16.15f, 13.4f),
                PathNode.Close,
            ),
        )
    }

    val PaletteLine: ImageVector by lazy {
        strokeAndDots(
            name = "Coui17.PaletteLine",
            outline = listOf(
                PathNode.MoveTo(20.601f, 14.975f),
                PathNode.ArcTo(9.1f, 9.1f, 0f, true, false, 17.737f, 19.065f),
                PathNode.ArcTo(2.5f, 2.5f, 0f, false, true, 20.601f, 14.975f),
                PathNode.Close,
            ),
            dots = listOf(
                PathNode.MoveTo(6.45f, 9.6f),
                PathNode.ArcTo(1.45f, 1.45f, 0f, true, false, 9.35f, 9.6f),
                PathNode.ArcTo(1.45f, 1.45f, 0f, true, false, 6.45f, 9.6f),
                PathNode.Close,
                PathNode.MoveTo(9.85f, 7.2f),
                PathNode.ArcTo(1.45f, 1.45f, 0f, true, false, 12.75f, 7.2f),
                PathNode.ArcTo(1.45f, 1.45f, 0f, true, false, 9.85f, 7.2f),
                PathNode.Close,
                PathNode.MoveTo(13.75f, 8.4f),
                PathNode.ArcTo(1.45f, 1.45f, 0f, true, false, 16.65f, 8.4f),
                PathNode.ArcTo(1.45f, 1.45f, 0f, true, false, 13.75f, 8.4f),
                PathNode.Close,
            ),
        )
    }

    val PaletteFill: ImageVector by lazy {
        solid(
            name = "Coui17.PaletteFill",
            pathData = listOf(
                PathNode.MoveTo(20.601f, 14.975f),
                PathNode.ArcTo(9.1f, 9.1f, 0f, true, false, 17.737f, 19.065f),
                PathNode.ArcTo(2.5f, 2.5f, 0f, false, true, 20.601f, 14.975f),
                PathNode.Close,
                PathNode.MoveTo(6.45f, 9.6f),
                PathNode.ArcTo(1.45f, 1.45f, 0f, true, false, 9.35f, 9.6f),
                PathNode.ArcTo(1.45f, 1.45f, 0f, true, false, 6.45f, 9.6f),
                PathNode.Close,
                PathNode.MoveTo(9.85f, 7.2f),
                PathNode.ArcTo(1.45f, 1.45f, 0f, true, false, 12.75f, 7.2f),
                PathNode.ArcTo(1.45f, 1.45f, 0f, true, false, 9.85f, 7.2f),
                PathNode.Close,
                PathNode.MoveTo(13.75f, 8.4f),
                PathNode.ArcTo(1.45f, 1.45f, 0f, true, false, 16.65f, 8.4f),
                PathNode.ArcTo(1.45f, 1.45f, 0f, true, false, 13.75f, 8.4f),
                PathNode.Close,
            ),
            evenOdd = true,
        )
    }

    /** The stroke the line forms share, in the 24dp viewport. */
    private const val STROKE = 1.9f

    private fun line(name: String, pathData: List<PathNode>): ImageVector = builder(name).apply {
        addPath(
            pathData = pathData,
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()

    private fun solid(
        name: String,
        pathData: List<PathNode>,
        evenOdd: Boolean = false,
    ): ImageVector = builder(name).apply {
        addPath(
            pathData = pathData,
            pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
            fill = SolidColor(Color.Black),
        )
    }.build()

    /** The palette's outline is stroked; its three dots are inked, the way the line form draws them. */
    private fun strokeAndDots(
        name: String,
        outline: List<PathNode>,
        dots: List<PathNode>,
    ): ImageVector = builder(name).apply {
        addPath(
            pathData = outline,
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineJoin = StrokeJoin.Round,
        )
        addPath(
            pathData = dots,
            pathFillType = PathFillType.NonZero,
            fill = SolidColor(Color.Black),
        )
    }.build()

    private fun builder(name: String): ImageVector.Builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24f.dp,
        defaultHeight = 24f.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
}
