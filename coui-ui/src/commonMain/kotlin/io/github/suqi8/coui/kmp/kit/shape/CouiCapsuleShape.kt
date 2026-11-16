// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.shape

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Oplus cornerType=4 / weight=1 sdCapsule zero-isoline shape. */
@OptIn(InternalCoroutinesApi::class)
internal object CouiCapsuleShape : Shape {
    private const val MAX_CACHE_SIZE = 16
    private val cacheLock = SynchronizedObject()

    // Share only read-only contour sample points keyed by exact pixel size; each caller owns its Path.
    private val quarterContours = LinkedHashMap<Size, List<Offset>>(MAX_CACHE_SIZE)

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        if (size.width <= 0f || size.height <= 0f) return Outline.Generic(path)
        val quarter = getCachedContour(size) ?: buildQuarterContour(size).also { built ->
            putCachedContour(size, built)
        }
        if (quarter.isEmpty()) return Outline.Generic(path)
        val halfX = size.width * 0.5
        val halfY = size.height * 0.5
        path.moveTo((halfX + quarter.first().x).toFloat(), halfY.toFloat())
        repeat(4) { quadrant ->
            quarter.indices.forEach { i ->
                val p = quarter[if (quadrant % 2 == 0) i else quarter.lastIndex - i]
                val x = if (quadrant == 1 || quadrant == 2) -p.x else p.x
                val y = if (quadrant >= 2) -p.y else p.y
                path.lineTo((halfX + x).toFloat(), (halfY + y).toFloat())
            }
        }
        path.close()
        // shortcut: Path samples along the original SDF with public AA rather than vendor fwidth; replace when GPU clip parity is available.
        return Outline.Generic(path)
    }

    private fun getCachedContour(size: Size): List<Offset>? = synchronized(cacheLock) {
        val cached = quarterContours.remove(size) ?: return null
        quarterContours[size] = cached
        cached
    }

    private fun putCachedContour(size: Size, contour: List<Offset>) {
        synchronized(cacheLock) {
            quarterContours.remove(size)
            quarterContours[size] = contour
            if (quarterContours.size > MAX_CACHE_SIZE) {
                val eldestKey = quarterContours.keys.firstOrNull()
                if (eldestKey != null) {
                    quarterContours.remove(eldestKey)
                }
            }
        }
    }

    private fun buildQuarterContour(size: Size): List<Offset> {
        val halfX = size.width * 0.5
        val halfY = size.height * 0.5
        if (capsuleSdf(0.0, 0.0, halfX, halfY) >= 0.0) return emptyList()
        fun point(angle: Double): Offset {
            val x = cos(angle)
            val y = sin(angle)
            var inner = 0.0
            var outer = hypot(halfX, halfY)
            repeat(32) {
                val radius = (inner + outer) * 0.5
                if (capsuleSdf(radius * x, radius * y, halfX, halfY) < 0.0) inner = radius else outer = radius
            }
            val radius = (inner + outer) * 0.5
            return Offset((radius * x).toFloat(), (radius * y).toFloat())
        }
        val quarter = mutableListOf(point(0.0))
        fun append(start: Double, a: Offset, end: Double, b: Offset, depth: Int) {
            val middle = (start + end) * 0.5
            val p = point(middle)
            val error = hypot((p.x - (a.x + b.x) * 0.5f).toDouble(), (p.y - (a.y + b.y) * 0.5f).toDouble())
            if (error > 0.025 && depth < 16) {
                append(start, a, middle, p, depth + 1)
                append(middle, p, end, b, depth + 1)
            } else {
                quarter.add(b)
            }
        }
        append(0.0, quarter.first(), PI * 0.5, point(PI * 0.5), 0)
        return quarter
    }

    private fun capsuleSdf(x: Double, y: Double, halfX: Double, halfY: Double): Double {
        val vertical = halfX < halfY
        val qx = hypot(if (vertical) y else x, 1.0)
        val qy = hypot(if (vertical) x else y, 1.0)
        val radius = if (vertical) halfX else halfY
        val a = abs(halfX - halfY)
        val s = qx - a
        val hard = max(s, 0.0)
        val k = radius / 80.0 * 26.6
        val soft = 0.5 * (s + sqrt(s * s + k * k))
        val wx = 1.0 - smoothStep(0.0, max(radius / 80.0 * 70.0, 0.001), abs(s))
        val wy = smoothStep(radius - max(radius / 80.0 * 60.0, 0.001), radius, qy)
        return hypot(hard + (soft - hard) * wx * wy, qy) - radius
    }

    private fun smoothStep(start: Double, end: Double, value: Double): Double {
        val t = ((value - start) / (end - start)).coerceIn(0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)
    }
}
