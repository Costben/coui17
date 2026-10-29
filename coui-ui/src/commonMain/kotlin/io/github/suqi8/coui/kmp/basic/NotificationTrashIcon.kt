// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi

/**
 * The ColorOS 17 notification delete glyph with its opening lid.
 *
 * This draws SystemUI's `notification_menu_delete_icon_lottie_*_final.json` (84 × 84 at 60fps, shown
 * at 28dp) without a Lottie runtime: the bin layer at (42, 52), and the lid layer parented to it,
 * pivoting about the right end of its bar. The bar is drawn tilted 18°, so -18° lays it flat on the
 * bin. [progress] is the animation's progress: the first half turns the lid to 0°, lifting its far
 * end open, which the shade drives with the drag past the merge; the second half lays it flat again,
 * which the shade plays over 200ms as the row is thrown away.
 *
 * SystemUI fills it with white at 72% in the dark and black at 54% in the light.
 *
 * @param progress Animation progress, 0..1.
 * @param color The fill colour.
 * @param modifier Modifier applied to the 28dp icon.
 * @param contentDescription Accessibility label, or `null` when the icon is decorative.
 */
@Composable
@ExperimentalCOUIApi
fun NotificationTrashIcon(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Canvas(
        modifier
            .size(28.dp)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics {
                        this.contentDescription = contentDescription
                        role = Role.Image
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        drawNotificationTrash(progress, color)
    }
}

/** The lid's rotation in degrees for an animation [progress], keyframed -18° → 0° → -18° (linear). */
internal fun notificationTrashLidRotation(progress: Float): Float {
    val frame = progress.coerceIn(0f, 1f) * TRASH_LAST_FRAME
    val half = TRASH_LAST_FRAME / 2
    return if (frame <= half) TRASH_LID_CLOSED * (1f - frame / half) else TRASH_LID_CLOSED * ((frame - half) / half)
}

internal fun DrawScope.drawNotificationTrash(progress: Float, color: Color) {
    val unit = size.minDimension / TRASH_CANVAS
    scale(unit, unit, pivot = Offset.Zero) {
        translate(42f, 52f) {
            scale(TRASH_GROUP_SCALE, TRASH_GROUP_SCALE, pivot = Offset.Zero) {
                drawPath(TrashBinPath, color)
            }
            translate(27.714f, -28.143f) {
                rotate(notificationTrashLidRotation(progress), pivot = Offset.Zero) {
                    translate(-25.64f, -10.793f) {
                        scale(TRASH_GROUP_SCALE, TRASH_GROUP_SCALE, pivot = Offset.Zero) {
                            drawPath(TrashLidPath, color)
                        }
                    }
                }
            }
        }
    }
}

private const val TRASH_CANVAS = 84f
private const val TRASH_LAST_FRAME = 60f
private const val TRASH_LID_CLOSED = -18f
private const val TRASH_GROUP_SCALE = 3f

/** One closed Lottie bezier contour: vertices plus in- and out-tangents relative to them. */
private class LottieContour(val v: FloatArray, val i: FloatArray, val o: FloatArray)

/** "Merge Paths" over a contour and the holes cut into it. */
private fun lottiePath(vararg contours: LottieContour): Path = Path().apply {
    fillType = PathFillType.EvenOdd
    for (c in contours) {
        val n = c.v.size / 2
        moveTo(c.v[0], c.v[1])
        for (k in 0 until n) {
            val from = k * 2
            val to = (k + 1) % n * 2
            cubicTo(
                c.v[from] + c.o[from],
                c.v[from + 1] + c.o[from + 1],
                c.v[to] + c.i[to],
                c.v[to + 1] + c.i[to + 1],
                c.v[to],
                c.v[to + 1],
            )
        }
        close()
    }
}

/** 垃圾桶盖子: the bar, drawn tilted 18°, with its handle and the handle's opening. */
private val TrashLidPath: Path by lazy {
    lottiePath(
        LottieContour(
            v = floatArrayOf(8.063f, 2.232f, 3.549f, 0.765f, 3.536f, -0.956f, 2.154f, -2.843f, 0.132f, -3.5f, -2.095f, -2.785f, -3.117f, -1.401f, -7.63f, -2.867f, -8.512f, -2.418f, -8.063f, -1.535f, 7.63f, 3.563f, 8.512f, 3.114f),
            i = floatArrayOf(0.368f, 0.12f, 0f, 0f, 0f, 0f, 0.82f, 0.266f, 0f, 0f, 0.512f, -0.694f, 0f, 0f, 0f, 0f, 0.12f, -0.368f, -0.368f, -0.12f, 0f, 0f, -0.12f, 0.368f),
            o = floatArrayOf(0f, 0f, 0f, 0f, -0.007f, -0.862f, 0f, 0f, -0.82f, -0.266f, 0f, 0f, 0f, 0f, -0.368f, -0.12f, -0.12f, 0.368f, 0f, 0f, 0.368f, 0.12f, 0.12f, -0.368f),
        ),
        LottieContour(
            v = floatArrayOf(-0.969f, -1.954f, -1.713f, -0.945f, 2.146f, 0.309f, 2.137f, -0.945f, 1.722f, -1.511f, -0.3f, -2.168f),
            i = floatArrayOf(0.154f, -0.208f, 0f, 0f, 0f, 0f, 0f, 0f, 0.246f, 0.08f, 0f, 0f),
            o = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, -0.002f, -0.258f, 0f, 0f, -0.246f, -0.08f),
        ),
    )
}

/** 垃圾桶箱子: the bin and its three slots. */
private val TrashBinPath: Path by lazy {
    lottiePath(
        LottieContour(
            v = floatArrayOf(-4.774f, 7.85f, -6.771f, 5.967f, -7.583f, -7.85f, 7.583f, -7.85f, 6.771f, 5.967f, 4.774f, 7.85f),
            i = floatArrayOf(0f, 0f, 0.062f, 1.056f, 0f, 0f, 0f, 0f, 0f, 0f, 1.058f, 0f),
            o = floatArrayOf(-1.058f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, -0.062f, 1.056f, 0f, 0f),
        ),
        LottieContour(
            v = floatArrayOf(3.273f, -5.55f, 2.551f, -4.873f, 2.3f, 2.877f, 2.978f, 3.6f, 3f, 3.6f, 3.7f, 2.922f, 3.951f, -4.828f),
            i = floatArrayOf(0.387f, 0.013f, 0.013f, -0.387f, 0f, 0f, -0.387f, -0.013f, -0.008f, 0f, -0.012f, 0.379f, 0f, 0f),
            o = floatArrayOf(-0.371f, -0.008f, 0f, 0f, -0.012f, 0.386f, 0.007f, 0f, 0.376f, 0f, 0f, 0f, 0.012f, -0.386f),
        ),
        LottieContour(
            v = floatArrayOf(0f, -5.55f, -0.7f, -4.85f, -0.7f, 2.9f, 0f, 3.6f, 0.7f, 2.9f, 0.7f, -4.85f),
            i = floatArrayOf(0.387f, 0f, 0f, -0.387f, 0f, 0f, -0.387f, 0f, 0f, 0.387f, 0f, 0f),
            o = floatArrayOf(-0.387f, 0f, 0f, 0f, 0f, 0.387f, 0.387f, 0f, 0f, 0f, 0f, -0.387f),
        ),
        LottieContour(
            v = floatArrayOf(-2.551f, -4.873f, -3.273f, -5.55f, -3.951f, -4.828f, -3.7f, 2.922f, -3f, 3.6f, -2.978f, 3.6f, -2.3f, 2.877f),
            i = floatArrayOf(0f, 0f, 0.373f, -0.006f, -0.012f, -0.386f, 0f, 0f, -0.376f, 0f, -0.007f, 0f, 0.012f, 0.386f),
            o = floatArrayOf(-0.013f, -0.387f, -0.387f, 0.013f, 0f, 0f, 0.012f, 0.379f, 0.008f, 0f, 0.387f, -0.013f, 0f, 0f),
        ),
    )
}
