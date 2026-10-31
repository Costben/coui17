// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import io.github.suqi8.coui.kmp.squircle.addSquircleRect
import kotlin.math.min

/*
 * The capsule outline of the ColorOS 17 system capsule, in corner-local units of the capsule
 * radius: x runs along the straight edge from the end of the capsule, y runs down its height.
 *
 * Measured from screen pixels of `COUISegmentButtonLayout` on PKT110 (ColorOS 17.0.0.102) rather than
 * derived from `OplusPathAdapter.addSmoothRoundRect`, whose output in a bare process is a plain
 * circular arc. On screen the outline follows a circular arc for 39.2 degrees from the tip, then eases
 * into the straight edge, which only ends 1.5 radii from the tip. The container (r = 20dp) and the
 * indicator (r = 16dp) trace the same normalised curve, within 0.1px of this model, and the 40/44dp
 * buttons of `COUIButton` sit on it as well (RMS 0.08px at 44dp, against 0.6px for a circular end).
 *
 * The cubic joins the arc with the arc's curvature and reaches the edge with zero curvature, which is
 * what hides the junction a circular arc leaves against a straight line.
 */
private const val TAIL_LENGTH = 1.5f
private const val ARC_HANDLE_1_Y = 0.7696933f
private const val ARC_HANDLE_2_X = 0.07949496f
private const val ARC_HANDLE_2_Y = 0.5464456f
private const val ARC_END_X = 0.2250555f
private const val ARC_END_Y = 0.3679707f
private const val EASE_HANDLE_1_X = 0.5251651f
private const val EASE_HANDLE_2_X = 0.9615864f

/** [addSegmentCapsule] as a [Shape], for the surfaces that clip or blur along the capsule. */
internal object SegmentCapsuleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = Outline.Generic(Path().apply { addSegmentCapsule(Rect(Offset.Zero, size)) })
}

/**
 * The capsule silhouette of a surface whose radius only reaches the capsule radius at some sizes.
 *
 * [cornerRadius] is the radius the caller asked for. Once it reaches half the shorter side the
 * outline is [addSegmentCapsule] — the arc-plus-easing end measured on ColorOS 17 capsules — and
 * below that it keeps the continuous-curvature squircle of [addSquircleRect]. `null` means the
 * radius is always half the node height, for the surfaces that are capsules at every size.
 *
 * The family is chosen in [createOutline] because only that call knows the size: composition never
 * reads it, so the same shape object can follow a surface while it deforms.
 *
 * @param cornerRadius The requested radius, or `null` for half the node height.
 * @param squircleEnabled Forward `isSquircleEnabled`; when `false` the outline is a plain rounded
 *   rectangle, the same fallback the shader-backed modifiers take.
 */
internal class CapsuleOutlineShape(
    private val cornerRadius: Dp? = null,
    private val squircleEnabled: Boolean = true,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val halfMin = size.minDimension / 2f
        val radius = cornerRadius?.let { with(density) { it.toPx() } } ?: halfMin
        if (!squircleEnabled) {
            return Outline.Rounded(
                RoundRect(
                    rect = size.toRect(),
                    cornerRadius = CornerRadius(radius.coerceIn(0f, halfMin)),
                ),
            )
        }
        val path = Path()
        if (radius >= halfMin - 0.5f) {
            if (size.height >= size.width) {
                return Outline.Rounded(
                    RoundRect(
                        rect = size.toRect(),
                        cornerRadius = CornerRadius(halfMin),
                    ),
                )
            }
            path.addSegmentCapsule(size.toRect())
        } else {
            path.addSquircleRect(width = size.width, height = size.height, cornerRadius = radius)
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean = other is CapsuleOutlineShape &&
        other.cornerRadius == cornerRadius &&
        other.squircleEnabled == squircleEnabled

    override fun hashCode(): Int = 31 * cornerRadius.hashCode() + squircleEnabled.hashCode()

    override fun toString(): String = "CapsuleOutlineShape(cornerRadius=$cornerRadius, squircleEnabled=$squircleEnabled)"
}

/**
 * Fills this node with [color] along [shape] and clips its content to the same silhouette.
 *
 * `squircleSurface` painted through a shader; that shader only knows the squircle corner family, so
 * a capsule that has to trace [addSegmentCapsule] paints through its [Outline] instead. The fill and
 * the content clip share one outline: the fill is drawn unclipped and the content is clipped with
 * the same path, so each edge is antialiased exactly once rather than being blended twice.
 *
 * The outline is resolved once per size change and [color] is read in the draw phase, so a fill that
 * only changes colour — the press tint of a button — redraws without rebuilding the path. Remember
 * the modifier on [shape] to keep the cache across those recompositions.
 */
internal fun Modifier.shapeFill(shape: Shape, color: () -> Color): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this@drawWithCache)
    val silhouette = Path().apply { addOutline(outline) }
    onDrawWithContent {
        drawOutline(outline, color())
        clipPath(silhouette) { this@onDrawWithContent.drawContent() }
    }
}

/**
 * Appends the system capsule filling [rect], whose radius is half the rect's height.
 * The easing tail shortens when the rect is too narrow to hold it at full length.
 */
internal fun Path.addSegmentCapsule(rect: Rect) {
    if (rect.height <= 0f || rect.width <= 0f) return
    if (rect.height >= rect.width) {
        addRoundRect(RoundRect(rect, CornerRadius(rect.width / 2f)))
        return
    }
    val radius = rect.height / 2f
    val tail = min(TAIL_LENGTH, rect.width / (2f * radius))
    val tailPx = tail * radius
    moveTo(rect.left + tailPx, rect.top)
    lineTo(rect.right - tailPx, rect.top)
    addCapsuleCornerToTip(rect.right, rect.top, -1f, 1f, radius, tail)
    addCapsuleCornerFromTip(rect.right, rect.bottom, -1f, -1f, radius, tail)
    lineTo(rect.left + tailPx, rect.bottom)
    addCapsuleCornerToTip(rect.left, rect.bottom, 1f, -1f, radius, tail)
    addCapsuleCornerFromTip(rect.left, rect.top, 1f, 1f, radius, tail)
    close()
}

private fun Path.addCapsuleCornerFromTip(cornerX: Float, cornerY: Float, signX: Float, signY: Float, radius: Float, tail: Float) {
    val scaleX = signX * radius
    val scaleY = signY * radius
    cubicTo(
        cornerX,
        cornerY + scaleY * ARC_HANDLE_1_Y,
        cornerX + scaleX * ARC_HANDLE_2_X,
        cornerY + scaleY * ARC_HANDLE_2_Y,
        cornerX + scaleX * ARC_END_X,
        cornerY + scaleY * ARC_END_Y,
    )
    cubicTo(
        cornerX + scaleX * EASE_HANDLE_1_X,
        cornerY,
        cornerX + scaleX * min(EASE_HANDLE_2_X, tail),
        cornerY,
        cornerX + scaleX * tail,
        cornerY,
    )
}

private fun Path.addCapsuleCornerToTip(cornerX: Float, cornerY: Float, signX: Float, signY: Float, radius: Float, tail: Float) {
    val scaleX = signX * radius
    val scaleY = signY * radius
    cubicTo(
        cornerX + scaleX * min(EASE_HANDLE_2_X, tail),
        cornerY,
        cornerX + scaleX * EASE_HANDLE_1_X,
        cornerY,
        cornerX + scaleX * ARC_END_X,
        cornerY + scaleY * ARC_END_Y,
    )
    cubicTo(
        cornerX + scaleX * ARC_HANDLE_2_X,
        cornerY + scaleY * ARC_HANDLE_2_Y,
        cornerX,
        cornerY + scaleY * ARC_HANDLE_1_Y,
        cornerX,
        cornerY + scaleY,
    )
}
