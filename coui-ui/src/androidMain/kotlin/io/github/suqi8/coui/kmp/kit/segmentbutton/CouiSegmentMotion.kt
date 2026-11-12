// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.segmentbutton

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import io.github.suqi8.coui.kmp.kit.motion.CouiSpring
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Allocates natural widths first and distributes remaining space to items wider than the mean,
 * preserving short labels when overall space is tight.
 */
internal fun segmentWidths(natural: List<Float>, available: Int, maxWidth: Float = Float.MAX_VALUE): List<Int> {
    if (natural.isEmpty()) return emptyList()
    val safeAvailable = available.coerceAtLeast(0)
    val order = natural.indices.sortedBy { natural[it] }
    val result = MutableList(natural.size) { 0f }
    val mean = safeAvailable.toFloat() / natural.size
    var remaining = safeAvailable.toFloat()
    var limit = mean
    var index = 0
    var last = -1
    while (index < order.size) {
        val item = order[index]
        if (natural[item] < limit) {
            result[item] = natural[item]
            remaining -= natural[item]
            index++
        } else if (last == index) {
            while (index < order.size) result[order[index++]] = limit
            remaining = 0f
            break
        } else {
            limit = remaining / (order.size - index)
            last = index
        }
    }
    if (last != -1 && remaining > 0f && order.size > 1) {
        order.dropLast(1).forEach { result[it] += remaining / (order.size - 1) }
    }
    // Accumulate rounded boundaries so child integer widths end flush at the content edge.
    var position = 0.0
    var start = 0
    return result.mapIndexed { i, width ->
        position += min(if (last == -1) mean else width, maxWidth)
        val end = if (i == result.lastIndex && maxWidth >= Float.MAX_VALUE / 2f) {
            safeAvailable
        } else {
            position.roundToInt().coerceIn(start, safeAvailable)
        }
        val measuredWidth = end - start
        start = end
        measuredWidth
    }
}

internal data class SegmentGeometry(
    val width: Float,
    val height: Float,
    val padding: Float,
    val widths: List<Int>,
    val rtl: Boolean,
    val small: Boolean,
) {
    private val starts = widths.runningFold(padding) { start, width -> start + width }
    val indicatorHeight get() = (height - padding * 2f).coerceAtLeast(0f)

    fun center(index: Int): Float {
        val logical = starts[index] + widths[index] / 2f
        return if (rtl) width - logical else logical
    }

    fun hit(x: Float, inset: Float = 0f): Int? {
        val logical = if (rtl) width - x else x
        return widths.indices.firstOrNull { logical >= starts[it] + inset && logical < starts[it + 1] - inset }
    }
}

internal class SegmentSelectionSession(selected: Int) {
    var allowLight = true
    var touching by mutableStateOf(false)
        private set
    var preview by mutableIntStateOf(selected)
        private set
    private var emitted = selected

    fun sync(selected: Int) {
        if (!touching) preview = selected
    }

    fun begin(selected: Int) {
        touching = true
        preview = selected
        emitted = selected
    }

    fun select(index: Int, onRelease: Boolean, submit: (Int) -> Unit) {
        preview = index
        if (!onRelease && index != emitted) {
            emitted = index
            submit(index)
        }
    }

    fun finish(cancelled: Boolean, selected: Int, onRelease: Boolean, submit: (Int) -> Unit) {
        if (!touching) return
        if (!cancelled && onRelease && preview != selected) submit(preview)
        if (cancelled) preview = selected
        touching = false
    }
}

internal class CouiSegmentMotion {
    private var disposed = false
    private val position = CouiSpring(0f)
    private val width = CouiSpring(0f)
    private val height = CouiSpring(0f)
    private val rubber = CouiSpring(0f)
    private var geometry: SegmentGeometry? = null
    private var widthLimit = Float.MAX_VALUE
    private var rubberAnchor = 0f
    var resetRubberOrigin = true
    val moving get() = position.isRunning
    val indicatorWidth get() = width.value.coerceIn(0f, widthLimit)

    fun select(index: Int, next: SegmentGeometry, density: Float) {
        if (disposed || next.widths.isEmpty()) return
        val safeIndex = index.coerceIn(0, next.widths.lastIndex)
        rubberAnchor = next.center(safeIndex)
        if (geometry != next) {
            geometry = next
            widthLimit = Float.MAX_VALUE
            position.setBounds(-Float.MAX_VALUE, Float.MAX_VALUE)
            width.setBounds(-Float.MAX_VALUE, Float.MAX_VALUE)
            height.setBounds(-Float.MAX_VALUE, Float.MAX_VALUE)
            position.snapTo(next.center(safeIndex))
            width.snapTo(next.widths[safeIndex].toFloat())
            height.snapTo(next.indicatorHeight)
            rubber.snapTo(0f)
            return
        }
        val targetWidth = next.widths[safeIndex].toFloat()
        val growing = targetWidth > width.value
        widthLimit = if (growing) targetWidth + (if (next.small) 5f else 7f) * density else Float.MAX_VALUE
        val minimumCenter = min(next.center(0), next.center(next.widths.lastIndex))
        val maximumCenter = max(next.center(0), next.center(next.widths.lastIndex))
        val transferRubber = rubber.value != 0f
        val positionVelocity = position.velocity
        if (transferRubber) {
            // Native runs both tracks against mIndicatorCenterX, so selection inherits the visible center.
            position.snapTo(
                (position.value.coerceIn(minimumCenter, maximumCenter) + rubber.value)
                    .coerceIn(minimumCenter, maximumCenter),
            )
        }
        rubber.snapTo(0f)
        position.setBounds(minimumCenter, maximumCenter)
        width.setBounds(0f, widthLimit)
        height.setBounds(next.indicatorHeight * .9f, Float.MAX_VALUE)
        position.animateTo(
            next.center(safeIndex),
            .4f,
            if (growing && next.small) dynamic(width.value, targetWidth, .1f, .03f, 1f) else .1f,
            velocity = if (transferRubber) positionVelocity else null,
            onEnd = { resetRubberOrigin = true },
        )
        width.animateTo(
            targetWidth,
            dynamic(width.value, targetWidth, .6f, .4f, 2f),
            if (growing) .5f else dynamic(width.value, targetWidth, .5f, .1f, 2f),
            velocity = 1000f,
        )
        height.animateTo(next.indicatorHeight, .6f, .5f, velocity = -500f)
    }

    fun rect(geometry: SegmentGeometry, scale: Float): Rect {
        val first = geometry.center(0)
        val last = geometry.center(geometry.widths.lastIndex)
        val center = position.value.coerceIn(min(first, last), max(first, last)) + rubber.value
        val halfWidth = indicatorWidth * scale / 2f
        val halfHeight = height.value.coerceAtLeast(geometry.indicatorHeight * .9f) * scale / 2f
        val minLeft = geometry.padding
        val maxRight = (geometry.width - geometry.padding).coerceAtLeast(minLeft)
        val left = (center - halfWidth).coerceIn(minLeft, maxRight)
        val right = (center + halfWidth).coerceIn(left, maxRight)
        return Rect(
            left,
            geometry.height / 2f - halfHeight,
            right,
            geometry.height / 2f + halfHeight,
        )
    }

    /** Captures the visible center on DOWN so grabbing an active spring preserves the drag origin. */
    fun beginDrag() {
        if (disposed) return
        rubberAnchor = position.value + rubber.value
        resetRubberOrigin = false
    }

    fun dragRubber(delta: Float, scale: Float, edge: Boolean, switchDistance: Float) {
        if (disposed || delta == 0f) return
        val distance = (
            ((1f - scale) * indicatorWidth / 2f).toInt().coerceAtLeast(0) +
                if (edge) 0 else switchDistance.toInt()
            ).toFloat()
        val offset = if (distance == 0f) {
            0f
        } else {
            (1f - 1f / (abs(delta) * .12f / distance + 1f)) * distance * sign(delta)
        }
        rubber.animateTo(rubberAnchor - position.value + offset, .6f)
    }

    fun release() {
        if (!disposed) rubber.animateTo(0f, .6f)
    }

    fun dispose() {
        disposed = true
        listOf(position, width, height, rubber).forEach { it.cancelComplete() }
    }

    private fun dynamic(from: Float, to: Float, normal: Float, minimum: Float, exponent: Float): Float = if (from <= 0f || to <= 0f) {
        normal
    } else {
        (normal - (normal - minimum) * (abs(from - to) / max(from, to)).pow(exponent)).coerceIn(0f, 1f)
    }
}
