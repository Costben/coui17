// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The rotation a ColorOS 17 popup menu list can be laid out with, mirroring
 * `com/coui/appcompat/poplist/MenuRotationHelper$RotationQuarter` and the
 * `COUIRotatingPopupListWindow` that consumes it.
 *
 * `COUIContextMenu`'s *positioning* is already covered in this library: [ListPopup] ports the same
 * `PreciseLongPressHelper` as [preciseClickAnchor], the same `PopupMenuLocateHelper.setAnchor` as
 * [ListPopupDefaults.dropdownPositionProvider] and [ListPopupDefaults.ContextMenuPositionProvider],
 * and the same overlay/window containers as [OverlayListPopup] / [WindowListPopup]. What was missing
 * is the rotation: nothing in this library rotates menu content, and none of the ported classes carry
 * `MenuRotationHelper`'s geometry. This file ports exactly that missing half — the quarter-angle
 * classifier, the rotated footprint and axis-bounds maths, and the fit-rect clamp the rotating
 * window uses to keep a rotated menu on screen.
 *
 * `MenuRotationHelper` only ever accepts a rotation that is a multiple of 90 degrees modulo 360
 * ([PopupMenuRotationDefaults.isMultipleOf90Mod360] with the `MOD_90_EPS_DEG` tolerance), and then
 * only within [PopupMenuRotationDefaults.ZERO_EPSILON_DEGREES] of 0 or
 * [PopupMenuRotationDefaults.QUARTER_EPSILON_DEGREES] of ±90; anything else is rejected and treated as
 * [None].
 *
 * @property displayDegrees The `RotationQuarter.toDisplayDegrees()` value: 0, 90 or 270.
 */
enum class PopupMenuRotation(val displayDegrees: Int) {
    /** `RotationQuarter.NONE`: the menu is laid out upright. */
    None(0),

    /** `RotationQuarter.DEG_90`: the menu content is rotated 90 degrees clockwise. */
    Degrees90(90),

    /** `RotationQuarter.DEG_270`: the menu content is rotated 270 degrees clockwise. */
    Degrees270(270),
    ;

    /**
     * `RotationQuarter.allowsExpandableSubMenu`: only an upright menu may open a sub-menu, because a
     * rotated sub-menu would leave the rotated footprint.
     */
    val allowsExpandableSubMenu: Boolean
        get() = this == None

    /** `RotationQuarter.isNonZeroRotation`. */
    val isNonZero: Boolean
        get() = this != None

    /**
     * `RotationQuarter.isOrthogonalForLocate`: whether the locate pass has to swap the menu's width
     * and height, which is the case for the two quarter turns.
     */
    val isOrthogonalForLocate: Boolean
        get() = this == Degrees90 || this == Degrees270
}

/**
 * The geometry `MenuRotationHelper` computes for a rotated menu list.
 *
 * @property wrapperLeft The left edge the wrapper has to be laid out at, after the fit-rect clamp
 *   (`RotationLayoutOut.mWrapperLeft`).
 * @property wrapperTop The top edge the wrapper has to be laid out at (`RotationLayoutOut.mWrapperTop`).
 * @property pivotX The rotation pivot on the x axis (`RotationLayoutOut.mPivotX`), the clamped half of
 *   the wrapper width.
 * @property pivotY The rotation pivot on the y axis (`RotationLayoutOut.mPivotY`).
 * @property screenBounds The menu's axis-aligned bounds on screen after rotation and clamping
 *   (`RotationLayoutOut.mScreenBounds`).
 */
@Immutable
data class MenuRotationLayout(
    val wrapperLeft: Int,
    val wrapperTop: Int,
    val pivotX: Float,
    val pivotY: Float,
    val screenBounds: IntRect,
)

/** Contains the values and the geometry maths of `MenuRotationHelper`. */
object PopupMenuRotationDefaults {

    /**
     * `MenuRotationHelper.ZERO_EPS_DEG` (0.05): how close to 0 degrees a rotation has to be to count
     * as [PopupMenuRotation.None].
     */
    const val ZERO_EPSILON_DEGREES = 0.05f

    /**
     * `MenuRotationHelper.MOD_90_EPS_DEG` (0.05): how close the remainder modulo 90 degrees has to be
     * to 0 (or 90) for an angle to count as a multiple of a quarter turn.
     */
    const val MOD_90_EPSILON_DEGREES = 0.05f

    /**
     * `MenuRotationHelper.QUARTER_EPS_DEG` (1.0): how close to ±90 degrees the smallest equivalent
     * angle has to be for a rotation to count as a quarter turn.
     */
    const val QUARTER_EPSILON_DEGREES = 1f

    /** `MenuRotationHelper.FULL_DEG` (360). */
    const val FULL_DEGREES = 360f

    /** `MenuRotationHelper.HALF_DEG` (180): the angle above which [smallestEquivalentDegrees] wraps to a negative value. */
    const val HALF_DEGREES = 180f

    /** `MenuRotationHelper.QUARTER_DEG` (90). */
    const val QUARTER_DEGREES = 90f

    /**
     * `MenuRotationHelper.classifyDegrees`: an angle that is not a supported quarter turn is reported
     * as [PopupMenuRotation.None] (the native logs a warning and does the same).
     */
    fun classifyDegrees(degrees: Float): PopupMenuRotation {
        if (!degrees.isFinite() || !isMultipleOf90Mod360(degrees)) return PopupMenuRotation.None
        val smallest = smallestEquivalentDegrees(degrees)
        if (abs(smallest) <= ZERO_EPSILON_DEGREES) return PopupMenuRotation.None
        if (abs(abs(smallest) - QUARTER_DEGREES) < QUARTER_EPSILON_DEGREES) {
            return if (smallest > 0f) PopupMenuRotation.Degrees90 else PopupMenuRotation.Degrees270
        }
        return PopupMenuRotation.None
    }

    /**
     * `MenuRotationHelper.isMultipleOf90Mod360`: `((f % 360) + 360) % 360 % 90` is within
     * [MOD_90_EPSILON_DEGREES] of 0 or 90.
     */
    fun isMultipleOf90Mod360(degrees: Float): Boolean {
        val remainder = (((degrees % FULL_DEGREES) + FULL_DEGREES) % FULL_DEGREES) % QUARTER_DEGREES
        return remainder < MOD_90_EPSILON_DEGREES || remainder > QUARTER_DEGREES - MOD_90_EPSILON_DEGREES
    }

    /**
     * `MenuRotationHelper.smallestEquivalentDeg`: the angle normalised into `(-180, 180]`. A
     * non-finite angle is reported as 0.
     */
    fun smallestEquivalentDegrees(degrees: Float): Float {
        if (!degrees.isFinite()) return 0f
        val normalized = ((degrees % FULL_DEGREES) + FULL_DEGREES) % FULL_DEGREES
        return if (normalized > HALF_DEGREES) normalized - FULL_DEGREES else normalized
    }

    /**
     * `MenuRotationHelper.clampPivot`: the pivot is clamped into `0..size`, and a non-positive size
     * yields 0.
     */
    fun clampPivot(value: Float, size: Int): Float = if (size <= 0) 0f else value.coerceIn(0f, size.toFloat())

    /**
     * `MenuRotationHelper.rotatedLocateWidth`: the width the locate pass sees. An orthogonal rotation
     * swaps the axes, so it is the menu's height.
     */
    fun rotatedLocateWidth(rotation: PopupMenuRotation, width: Int, height: Int): Int = if (rotation.isOrthogonalForLocate) height else width

    /**
     * `MenuRotationHelper.rotatedLocateHeight`: the height the locate pass sees. An orthogonal
     * rotation swaps the axes, so it is the menu's width.
     */
    fun rotatedLocateHeight(rotation: PopupMenuRotation, width: Int, height: Int): Int = if (rotation.isOrthogonalForLocate) width else height

    /**
     * `MenuRotationHelper.localAxisBounds`: the bounding box of the rotated rectangle in its own axis
     * space, as `[left, right, top, bottom]`. An upright rectangle is `[0, w, 0, h]`; a quarter turn
     * yields a square of side `(w + h) / 2` centred on the original centre, i.e.
     * `[(w - h) / 2, (w + h) / 2, (h - w) / 2, (w + h) / 2]`.
     *
     * A non-positive or non-finite size falls back to `[0, max(0, w), 0, max(0, h)]`, and an
     * unsupported angle is classified through [classifyDegrees].
     */
    fun localAxisBounds(width: Int, height: Int, rotation: PopupMenuRotation): Rect {
        if (width <= 0 || height <= 0) {
            return Rect(0f, 0f, width.toFloat().coerceAtLeast(0f), height.toFloat().coerceAtLeast(0f))
        }
        return when (rotation) {
            PopupMenuRotation.None -> Rect(0f, 0f, width.toFloat(), height.toFloat())

            PopupMenuRotation.Degrees90, PopupMenuRotation.Degrees270 -> {
                val halfSum = (width + height) * 0.5f
                Rect(
                    left = (width - height) * 0.5f,
                    top = (height - width) * 0.5f,
                    right = halfSum,
                    bottom = halfSum,
                )
            }
        }
    }

    /**
     * `MenuRotationHelper.localAxisBounds` by angle: the same bounds, with the angle classified first.
     */
    fun localAxisBounds(width: Int, height: Int, degrees: Float): Rect = localAxisBounds(width, height, classifyDegrees(degrees))

    /**
     * `MenuRotationHelper.screenAxisBounds`: the rotated rectangle's axis-aligned bounds on screen,
     * rounded to whole pixels.
     */
    fun screenAxisBounds(
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        rotation: PopupMenuRotation,
    ): IntRect {
        val local = localAxisBounds(width, height, rotation)
        return IntRect(
            left = (left + local.left).roundToInt(),
            top = (top + local.top).roundToInt(),
            right = (left + local.right).roundToInt(),
            bottom = (top + local.bottom).roundToInt(),
        )
    }

    /**
     * `MenuRotationHelper.computeClampDeltaToFitRectInRect`: how far [bounds] has to move to sit
     * inside [fitBounds]. A rectangle larger than the fit rect is aligned to the fit rect's leading
     * edge rather than pushed out of it.
     */
    fun clampDeltaToFitRectInRect(bounds: IntRect, fitBounds: IntRect): IntOffset {
        val dx = when {
            bounds.width > fitBounds.width -> fitBounds.left - bounds.left
            bounds.left < fitBounds.left -> fitBounds.left - bounds.left
            bounds.right > fitBounds.right -> fitBounds.right - bounds.right
            else -> 0
        }
        val dy = when {
            bounds.height > fitBounds.height -> fitBounds.top - bounds.top
            bounds.top < fitBounds.top -> fitBounds.top - bounds.top
            bounds.bottom > fitBounds.bottom -> fitBounds.bottom - bounds.bottom
            else -> 0
        }
        return IntOffset(dx, dy)
    }

    /**
     * `MenuRotationHelper.computeRotationLayoutFromWrapper`: the layout for a wrapper already placed
     * at ([wrapperLeft], [wrapperTop]).
     *
     * The pivot is the centre of the *un-rotated* wrapper, the rotated footprint is derived from it,
     * and both the footprint and the wrapper are then shifted by the delta that fits the footprint
     * into [fitBounds].
     */
    fun computeRotationLayoutFromWrapper(
        wrapperLeft: Int,
        wrapperTop: Int,
        width: Int,
        height: Int,
        rotation: PopupMenuRotation,
        fitBounds: IntRect,
    ): MenuRotationLayout {
        val pivotX = if (width > 0) clampPivot(width * 0.5f, width) else 0f
        val pivotY = if (height > 0) clampPivot(height * 0.5f, height) else 0f
        val unclamped = screenAxisBounds(wrapperLeft, wrapperTop, width, height, rotation)
        val delta = clampDeltaToFitRectInRect(unclamped, fitBounds)
        return MenuRotationLayout(
            wrapperLeft = wrapperLeft + delta.x,
            wrapperTop = wrapperTop + delta.y,
            pivotX = pivotX,
            pivotY = pivotY,
            screenBounds = unclamped.translate(delta.x, delta.y),
        )
    }

    /**
     * `MenuRotationHelper.computeRotationLayout`: the layout for a menu whose *un-rotated* bounds on
     * screen are [anchorBounds].
     *
     * The wrapper origin is backed out of the rotated axis bounds, which is what keeps the rotated
     * menu centred on the anchor instead of on the wrapper's top-left corner.
     */
    fun computeMenuRotationLayout(
        anchorBounds: IntRect,
        width: Int,
        height: Int,
        rotation: PopupMenuRotation,
        fitBounds: IntRect,
    ): MenuRotationLayout {
        val local = localAxisBounds(width, height, rotation)
        return computeRotationLayoutFromWrapper(
            wrapperLeft = (anchorBounds.left - local.left).roundToInt(),
            wrapperTop = (anchorBounds.top - local.top).roundToInt(),
            width = width,
            height = height,
            rotation = rotation,
            fitBounds = fitBounds,
        )
    }

    /**
     * `MenuRotationHelper.computeRotationLayout` by angle: the same layout, with the angle classified
     * first.
     */
    fun computeMenuRotationLayout(
        anchorBounds: IntRect,
        width: Int,
        height: Int,
        degrees: Float,
        fitBounds: IntRect,
    ): MenuRotationLayout = computeMenuRotationLayout(anchorBounds, width, height, classifyDegrees(degrees), fitBounds)
}

/**
 * Rotates popup menu content by [rotation], about its own centre — the
 * `RotationLayoutOut.mPivotX` / `mPivotY` pair `MenuRotationHelper` computes is always the clamped
 * half of the wrapper size, so the native rotation is a centre rotation.
 *
 * [PopupMenuRotation.None] leaves the modifier chain untouched, so an upright menu pays nothing.
 *
 * The content keeps its un-rotated measured size; the rotated footprint is what
 * [PopupMenuRotationDefaults.computeMenuRotationLayout] and [popupMenuRotationPositionProvider]
 * reason about.
 */
fun Modifier.popupMenuRotation(rotation: PopupMenuRotation): Modifier = if (rotation.isOrthogonalForLocate) {
    graphicsLayer { rotationZ = rotation.displayDegrees.toFloat() }
} else {
    this
}

/**
 * Wraps [base] so that a rotated menu is positioned by its *rotated* footprint, the
 * `MenuRotationHelper.rotatedLocateWidth` / `rotatedLocateHeight` swap
 * `COUIRotatingPopupListWindow` applies before it locates the menu.
 *
 * The base provider is asked for a position using the swapped size, and the result is then pulled
 * back by half the size difference, because the wrapper is still laid out at the un-rotated content
 * size and [popupMenuRotation] rotates it about its centre.
 *
 * @param rotation The rotation the menu content is drawn with.
 * @param base The provider that decides where the menu goes; defaults to
 *   [ListPopupDefaults.DropdownPositionProvider].
 */
fun popupMenuRotationPositionProvider(
    rotation: PopupMenuRotation,
    base: PopupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
): PopupPositionProvider = object : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowBounds: IntRect,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
        popupMargin: IntRect,
        alignment: PopupPositionProvider.Align,
    ): IntOffset {
        if (!rotation.isOrthogonalForLocate) {
            return base.calculatePosition(
                anchorBounds = anchorBounds,
                windowBounds = windowBounds,
                layoutDirection = layoutDirection,
                popupContentSize = popupContentSize,
                popupMargin = popupMargin,
                alignment = alignment,
            )
        }
        val locateWidth = PopupMenuRotationDefaults.rotatedLocateWidth(
            rotation,
            popupContentSize.width,
            popupContentSize.height,
        )
        val locateHeight = PopupMenuRotationDefaults.rotatedLocateHeight(
            rotation,
            popupContentSize.width,
            popupContentSize.height,
        )
        val located = base.calculatePosition(
            anchorBounds = anchorBounds,
            windowBounds = windowBounds,
            layoutDirection = layoutDirection,
            popupContentSize = IntSize(locateWidth, locateHeight),
            popupMargin = popupMargin,
            alignment = alignment,
        )
        return located + IntOffset(
            x = (locateWidth - popupContentSize.width) / 2,
            y = (locateHeight - popupContentSize.height) / 2,
        )
    }

    override fun getMargins(): PaddingValues = base.getMargins()
}
