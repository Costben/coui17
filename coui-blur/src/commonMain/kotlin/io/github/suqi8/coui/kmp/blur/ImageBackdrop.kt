// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.suqi8.coui.kmp.blur.internal.InverseLayerScope

/**
 * A backdrop that replays a captured image of another window.
 *
 * A surface hosted in its own platform window (a `Dialog`) cannot sample the page it opens over:
 * that page is recorded in a different window, and a recorded layer cannot be read across windows.
 * The host captures the page once instead and hands it over as this backdrop, so the surface runs
 * the same blur and tint pipeline an in-window surface does, over a still of the same page.
 *
 * Positions are matched on screen, because the consumer and the captured window live in different
 * windows whose origins need not coincide.
 *
 * @property image The captured window, possibly downscaled.
 * @property sourceSize Size of the captured window in pixels; [image] is stretched back to it.
 * @property sourceOriginOnScreen Top-left corner of the captured window on screen, in pixels.
 */
@InternalCouiBlurApi
@Stable
class ImageBackdrop(
    val image: ImageBitmap,
    val sourceSize: IntSize,
    val sourceOriginOnScreen: Offset,
) : Backdrop {

    override val isCoordinatesDependent: Boolean = true

    override var offsetResidualX: Float = 0f
        private set
    override var offsetResidualY: Float = 0f
        private set

    private var inverseLayerScope: InverseLayerScope? = null

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Int,
    ) {
        val coordinates = coordinates ?: return
        if (!coordinates.isAttached) return

        val offset = try {
            coordinates.positionOnScreen() - sourceOriginOnScreen
        } catch (_: Exception) {
            coordinates.positionInWindow()
        }

        val consumerSize = (density as? BackdropEffectScope)?.size ?: size

        withTransform({
            if (layerBlock != null) {
                with(obtainInverseLayerScope()) { inverseTransform(density, consumerSize, layerBlock) }
            }
            if (downscaleFactor > 1) {
                val inv = 1f / downscaleFactor
                val scaledX = offset.x * inv
                val scaledY = offset.y * inv
                val roundedX = kotlin.math.round(scaledX * 0.5f).toInt().toFloat() * 2f
                val roundedY = kotlin.math.round(scaledY * 0.5f).toInt().toFloat() * 2f
                offsetResidualX = (scaledX - roundedX) * downscaleFactor
                offsetResidualY = (scaledY - roundedY) * downscaleFactor
                translate(-roundedX, -roundedY)
                scale(inv, inv, Offset.Zero)
            } else {
                offsetResidualX = 0f
                offsetResidualY = 0f
                translate(-offset.x, -offset.y)
            }
        }) {
            drawImage(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(image.width, image.height),
                dstOffset = IntOffset.Zero,
                dstSize = sourceSize,
                filterQuality = FilterQuality.Medium,
            )
        }
    }

    private fun obtainInverseLayerScope(): InverseLayerScope = inverseLayerScope?.apply { reset() }
        ?: InverseLayerScope().also { inverseLayerScope = it }
}
