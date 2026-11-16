// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.effect

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.skiaCanvas
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Rect
import org.jetbrains.skia.VertexMode
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Skiko 投影渲染器; 使用 Skia 顶点网格与 ImageFilter.makeBlur 还原 ambient/spot 阴影。 */
internal actual class CouiShadowRenderer actual constructor(surfaceCount: Int) {
    private val slots = Array(surfaceCount) { arrayOfNulls<ProjectionDraw>(2) }

    actual fun draw(
        canvas: Canvas,
        slot: Int,
        width: Float,
        height: Float,
        sx: Float,
        sy: Float,
        windowX: Float,
        windowY: Float,
        alpha: Float,
        color: Int,
        elevation: Float,
        lightY: Float,
        lightZ: Float,
        lightRadius: Float,
        outline: CouiShadowOutline,
        radius: Float,
        clipping: Boolean,
    ) {
        if (alpha <= 0f || width <= 0f || height <= 0f || sx <= 0f || sy <= 0f || elevation <= 0f) return
        require(!clipping || outline != CouiShadowOutline.RoundRect) { "普通圆角投影尚未提供 inverse clip" }
        val skiaCanvas = canvas.skiaCanvas
        val fast = isFastShadowGeometry(width, height, sx, sy, outline == CouiShadowOutline.Oval)
        for (index in 0..1) {
            val multiplier = if (index == 0) 9f / 255f else 1f
            val opacity = ((alpha * multiplier) * ((color ushr 24) and 0xFF)).toInt().coerceIn(0, 255)
            if (opacity == 0) continue
            val argb = (color and 0xFFFFFF) or (opacity shl 24)
            val localOrigin = index == 0 || fast
            val key = ShadowKey(
                width, height, sx, sy,
                if (localOrigin) 0f else windowX, if (localOrigin) 0f else windowY, alpha < 1f,
                elevation, lightY, lightZ, lightRadius, outline,
                radius.coerceIn(0f, min(width, height) * 0.5f), clipping,
            )
            val draw = slots[slot][index]?.takeIf { it.key == key } ?: makeDraw(key, index == 1).also {
                slots[slot][index] = it
            }
            updateColor(draw, argb)
            if (draw.positions != null && draw.colors != null && draw.indices != null) {
                skiaCanvas.save()
                skiaCanvas.translate(draw.projection.offset.x, draw.projection.offset.y)
                draw.clipPath?.let { skiaCanvas.clipPath(it, ClipMode.DIFFERENCE, true) }
                skiaCanvas.drawVertices(
                    VertexMode.TRIANGLES,
                    draw.positions,
                    draw.colors,
                    null,
                    draw.indices,
                    BlendMode.MODULATE,
                    draw.paint,
                )
                skiaCanvas.restore()
            } else if (draw.path != null) {
                skiaCanvas.save()
                draw.clipPath?.let { skiaCanvas.clipPath(it, ClipMode.DIFFERENCE, true) }
                skiaCanvas.drawPath(draw.path, draw.paint)
                skiaCanvas.restore()
            }
        }
    }

    private fun makeDraw(key: ShadowKey, spot: Boolean): ProjectionDraw {
        val oval = key.outline == CouiShadowOutline.Oval
        val projection = floatingShadowGeometry(
            key.width, key.height, key.sx, key.sy, key.x, key.y,
            key.elevation, key.lightY, key.lightZ, key.lightRadius, key.transparent, oval, spot,
            key.radius, key.outline == CouiShadowOutline.RoundRect,
        )
        val data = projection.data
        val paint = Paint().apply {
            isAntiAlias = true
            color = -1
        }
        if (data == null) return blurDraw(key, projection, oval, spot, paint)
        if (data.indices.isEmpty()) return ProjectionDraw(key, projection, paint)

        val vertexCount = data.vertices.size / 5
        val positions = FloatArray(vertexCount * 2)
        val falloff = FloatArray(vertexCount) { v ->
            val offset = v * 5
            positions[v * 2] = data.vertices[offset]
            positions[v * 2 + 1] = data.vertices[offset + 1]
            if (projection.fast) {
                val u = data.vertices[offset + 2]
                val vCoord = data.vertices[offset + 3]
                val correction = data.vertices[offset + 4]
                val d = sqrt(u * u + vCoord * vCoord)
                val t = (correction * (1f - d)).coerceIn(0f, 1f)
                val lutD = 1f - t
                (exp(-4f * lutD * lutD) - 0.018f).coerceIn(0f, 1f)
            } else {
                val factor = 1f - data.vertices[offset + 4]
                (exp(-factor * factor * 4f) - 0.018f).coerceIn(0f, 1f)
            }
        }
        val clipPath = if (key.clipping) {
            val caster = Rect.makeXYWH(
                -projection.offset.x,
                -projection.offset.y,
                key.width * key.sx,
                key.height * key.sy,
            )
            buildClipPath(caster, oval, key.radius * key.sx, key.radius * key.sy)
        } else {
            null
        }
        return ProjectionDraw(
            key = key,
            projection = projection,
            paint = paint,
            positions = positions,
            falloffFactors = falloff,
            colors = IntArray(vertexCount),
            indices = data.indices,
            clipPath = clipPath,
        )
    }

    private fun blurDraw(
        key: ShadowKey,
        projection: ShadowProjection,
        oval: Boolean,
        spot: Boolean,
        paint: Paint,
    ): ProjectionDraw {
        val r = key.radius
        val rect = Rect.makeXYWH(0f, 0f, key.width * key.sx, key.height * key.sy)
        val source = if (spot) {
            val ratio = (key.elevation / (key.lightZ - key.elevation)).coerceIn(0f, 0.95f)
            val left = -ratio * key.sx * key.width * 0.5f
            val top = -ratio * key.sy * key.lightY
            Rect.makeXYWH(left, top, rect.width * projection.scale, rect.height * projection.scale)
        } else {
            rect
        }
        val path = buildClipPath(
            source,
            oval,
            r * key.sx * projection.scale,
            r * key.sy * projection.scale,
        )
        paint.mode = PaintMode.STROKE_AND_FILL
        paint.strokeWidth = max(projection.stroke, 0f)
        val sigma = if (projection.blur > 0f) projection.blur * 0.57735f + 0.5f else 0f
        if (sigma > 0f) {
            paint.imageFilter = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.DECAL, null, null)
        }
        val clipPath = if (key.clipping) {
            buildClipPath(rect, oval, r * key.sx, r * key.sy)
        } else {
            null
        }
        return ProjectionDraw(key, projection, paint, path = path, clipPath = clipPath)
    }

    private fun buildClipPath(rect: Rect, oval: Boolean, rx: Float, ry: Float): Path = PathBuilder().apply {
        if (oval) {
            addOval(rect)
        } else {
            addRRect(RRect.makeXYWH(rect.left, rect.top, rect.width, rect.height, rx, ry))
        }
    }.detach()

    private fun updateColor(draw: ProjectionDraw, color: Int) {
        if (draw.color == color) return
        draw.color = color
        val falloff = draw.falloffFactors
        val colors = draw.colors
        if (falloff != null && colors != null) {
            val baseAlpha = (color ushr 24) and 0xFF
            val rgb = color and 0x00FFFFFF
            for (v in falloff.indices) {
                val a = (baseAlpha * falloff[v] + 0.5f).toInt().coerceIn(0, 255)
                colors[v] = (a shl 24) or rgb
            }
        } else if (draw.path != null) {
            draw.paint.color = color
        }
    }

    private data class ShadowKey(
        val width: Float,
        val height: Float,
        val sx: Float,
        val sy: Float,
        val x: Float,
        val y: Float,
        val transparent: Boolean,
        val elevation: Float,
        val lightY: Float,
        val lightZ: Float,
        val lightRadius: Float,
        val outline: CouiShadowOutline,
        val radius: Float,
        val clipping: Boolean,
    )

    private class ProjectionDraw(
        val key: ShadowKey,
        val projection: ShadowProjection,
        val paint: Paint,
        val positions: FloatArray? = null,
        val falloffFactors: FloatArray? = null,
        val colors: IntArray? = null,
        val indices: ShortArray? = null,
        val path: Path? = null,
        val clipPath: Path? = null,
    ) {
        var color: Int? = null
    }
}
