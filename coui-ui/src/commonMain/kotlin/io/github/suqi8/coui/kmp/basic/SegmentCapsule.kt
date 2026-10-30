// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min

/*
 * The capsule outline of the ColorOS 17 segmented control, in corner-local units of the capsule
 * radius: x runs along the straight edge from the end of the capsule, y runs down its height.
 *
 * Measured from screen pixels of `COUISegmentButtonLayout` on PKT110 (ColorOS 17.0.0.102) rather than
 * derived from `OplusPathAdapter.addSmoothRoundRect`, whose output in a bare process is a plain
 * circular arc. On screen the outline follows a circular arc for 39.2 degrees from the tip, then eases
 * into the straight edge, which only ends 1.5 radii from the tip. The container (r = 20dp) and the
 * indicator (r = 16dp) trace the same normalised curve, within 0.1px of this model.
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
 * Appends the segmented control's capsule filling [rect], whose radius is half the rect's height.
 * The easing tail shortens when the rect is too narrow to hold it at full length.
 */
internal fun Path.addSegmentCapsule(rect: Rect) {
    val radius = rect.height / 2f
    if (radius <= 0f || rect.width <= 0f) return
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
