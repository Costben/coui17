// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.overlay.OverlayAlertDialog
import io.github.suqi8.coui.kmp.theme.COUITheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Measures the *painted* button bar, not its layout box, against a ColorOS 17 screenshot.
 *
 * The reference ("要删除车辆吗?") was shot on a 1272px-wide 330dp phone, so it renders at 3.854
 * px/dp and its panel is 1148px = 298dp wide — the same `min(392dp, 330dp - 2 * 16dp)` this test
 * reproduces. Sampling the same rows on both sides is the only way to check the two numbers a
 * screenshot can actually see: how far the capsule sits from the panel edge (62.5px = 16.2dp) and
 * how wide the gap between the capsules is (33.6px = 8.7dp). Both are the layout values 16dp / 8dp
 * once the capsules' anti-aliased edge is taken into account, which is why the assertions allow a
 * pixel of slack on either side.
 */
@OptIn(ExperimentalTestApi::class)
class DialogButtonBarPaintTest {

    @Test
    fun theCapsulesPaintWhereAColorOsScreenshotShowsThem() = runSkikoComposeUiTest(
        size = Size(WIDTH_PX.toFloat(), HEIGHT_PX.toFloat()),
        density = Density(DENSITY),
    ) {
        setContent { Harness() }
        waitForIdle()

        val image = onRoot().captureToImage().toAwtImage()
        File("/tmp/coui-bar-paint.png").let { ImageIO.write(image, "png", it) }
        val pixels = IntArray(image.width * image.height).also {
            image.getRGB(0, 0, image.width, image.height, it, 0, image.width)
        }
        fun lum(x: Int, y: Int): Int {
            val rgb = pixels[y * image.width + x]
            // Rec. 601 luma, the same weighting a screenshot probe uses.
            val r = (rgb shr 16) and 0xFF
            val g = (rgb shr 8) and 0xFF
            val b = rgb and 0xFF
            return (r * 299 + g * 587 + b * 114) / 1000
        }

        // Panel edges: the frosted panel is much brighter than the dimmed window behind it.
        val midRow = HEIGHT_PX / 2
        val panelLeft = (0 until WIDTH_PX).first { lum(it, midRow) > PANEL_THRESHOLD }
        val panelRight = (WIDTH_PX - 1 downTo 0).first { lum(it, midRow) > PANEL_THRESHOLD }
        val panelWidth = panelRight - panelLeft + 1

        // Walk down a column that sits well inside the left capsule. Only the capsule body and its
        // rim are brighter than the panel there, so the run of such rows is the capsule's height.
        // The panel's own rim is a 1-4px band that also clears the threshold, so take the longest
        // contiguous run instead of the first and last matching rows.
        val probeX = panelLeft + panelWidth / 8
        val runs = mutableListOf<IntRange>()
        var runStart = -1
        for (y in 0..HEIGHT_PX) {
            val bright = y < HEIGHT_PX && lum(probeX, y) > CAPSULE_THRESHOLD
            if (bright && runStart < 0) runStart = y
            if (!bright && runStart >= 0) {
                runs += runStart until y
                runStart = -1
            }
        }
        val capsuleBand = runs.maxBy { it.last - it.first }
        val capsuleTop = capsuleBand.first
        val capsuleBottom = capsuleBand.last
        val capsuleRow = (capsuleTop + capsuleBottom) / 2
        // Column profile across the capsule band, max over rows: the label ink would otherwise
        // cut the horizontal scan short at the first glyph.
        val band = (panelLeft..panelRight).map { x ->
            (capsuleTop + 6..capsuleBottom - 6).maxOf { y -> lum(x, y) }
        }
        val fillLum = band.max()
        val panelLum = lum(panelLeft + panelWidth / 2, capsuleRow - 200)

        // Capsule extents along the midline, walking in from each panel edge. `leftEnd` is the first
        // panel-brightness column after the left capsule, `rightStart` the last one before the right
        // capsule, so `rightStart - leftEnd` is the gap between their painted edges.
        val leftStart = band.indexOfFirst { it > CAPSULE_THRESHOLD }
        val leftEnd = (leftStart until panelWidth).first { band[it] < CAPSULE_THRESHOLD } - 1
        val rightEnd = band.indexOfLast { it > CAPSULE_THRESHOLD }
        val rightStart = (rightEnd downTo 0).first { band[it] < CAPSULE_THRESHOLD } + 1
        val capsuleHeight = capsuleBottom - capsuleTop + 1

        println(
            "PANEL x=$panelLeft..$panelRight w=$panelWidth  CAPSULE y=$capsuleTop..$capsuleBottom " +
                "h=$capsuleHeight  SIDE=${leftStart}px GAP=${rightStart - leftEnd}px " +
                "WIDTHS=${leftEnd - leftStart + 1}px/${rightEnd - rightStart + 1}px " +
                "fill=$fillLum panel=$panelLum",
        )

        // 298dp at 3.854 px/dp is 1148px; the reference screenshot's panel is also 1148px wide.
        assertEquals(1148f, panelWidth.toFloat(), 3f, "panel width in px")

        // coui_alert_dialog_button_parent_padding_horizontal = 16dp -> 61.7px; reference 62.5px.
        assertTrue(abs(leftStart - 61.7f) < 2f, "capsule starts ${leftStart}px from the panel edge")

        // Two 4dp inner insets -> 8dp -> 30.8px; reference 33.6px across the capsules' soft edges.
        val gap = rightStart - leftEnd
        assertTrue(abs(gap - 30.8f) < 2.5f, "capsules are ${gap}px apart")

        // Each capsule is (1148 - 2 * 61.7 - 30.8) / 2 = 497px; reference 495.9 / 494.4px.
        val leftWidth = leftEnd - leftStart + 1
        assertTrue(abs(leftWidth - 497f) < 3f, "left capsule is ${leftWidth}px wide")
        assertTrue(abs(rightEnd - rightStart - leftWidth) < 2, "the two capsules are equal width")

        // coui_alert_dialog_button_height = 44dp -> 169.6px; reference 170.3px.
        assertEquals(169.6f, capsuleHeight.toFloat(), 2f, "capsule height in px")

        // The fill is the translucent white the platform blur layer paints over the panel; the
        // reference sits ~16 levels above its panel.
        assertTrue(fillLum > panelLum + 12, "capsule fill $fillLum must sit above panel $panelLum")
    }

    /**
     * A destructive action keeps the ordinary translucent capsule and only turns its label red:
     * Settings paints it by hand with `color_delete_alert_dialog_button_warning_color` (`#ffea3447`),
     * which is what [DialogButtonBarDefaults.dangerButtonColors] wraps.
     */
    @Test
    fun theDangerColorsPaintARedLabelOnTheSameCapsule() = runSkikoComposeUiTest(
        size = Size(WIDTH_PX.toFloat(), HEIGHT_PX.toFloat()),
        density = Density(DENSITY),
    ) {
        setContent {
            COUITheme {
                Scaffold(containerColor = Color(0xFFF0F0F0)) {
                    Box(Modifier.fillMaxSize()) {
                        OverlayAlertDialog(
                            show = true,
                            onConfirm = {},
                            onCancel = {},
                            title = "要删除车辆吗?",
                            summary = "删除后，该车辆的记录将无法恢复。",
                            confirmText = "删除车辆",
                            cancelText = "取消",
                            colors = AlertDialogDefaults.alertDialogColors(
                                backgroundColor = Color(0xFFE5E5E5),
                            ),
                            // The reference's confirm is a destructive action, which ColorOS 17
                            // paints as a red label on the ordinary translucent capsule.
                            buttonColors = DialogButtonBarDefaults.dangerButtonColors(),
                        )
                    }
                }
            }
        }
        waitForIdle()

        val image = onRoot().captureToImage().toAwtImage()
        val pixels = IntArray(image.width * image.height).also {
            image.getRGB(0, 0, image.width, image.height, it, 0, image.width)
        }
        fun rgb(x: Int, y: Int) = pixels[y * image.width + x]
        fun red(x: Int, y: Int) = (rgb(x, y) shr 16) and 0xFF
        fun green(x: Int, y: Int) = (rgb(x, y) shr 8) and 0xFF
        fun blue(x: Int, y: Int) = rgb(x, y) and 0xFF

        // The right capsule's label ink: the most saturated red anywhere in the panel.
        val row = (HEIGHT_PX / 2 until HEIGHT_PX).first { y ->
            (0 until WIDTH_PX).any { x -> red(x, y) - green(x, y) > 60 }
        }
        val inkX = (0 until WIDTH_PX).maxBy { x -> red(x, row) - green(x, row) }
        println("INK x=$inkX y=$row rgb=(${red(inkX, row)},${green(inkX, row)},${blue(inkX, row)})")

        // #ffea3447 is (234, 52, 71): red channel high, green and blue far below it.
        assertTrue(red(inkX, row) > 190, "danger label red channel ${red(inkX, row)}")
        assertTrue(green(inkX, row) < 110, "danger label green channel ${green(inkX, row)}")
        assertTrue(blue(inkX, row) < 130, "danger label blue channel ${blue(inkX, row)}")

        // The fill under the label is still the translucent white capsule, not a red one. The glyphs
        // are ~57px tall inside a 170px capsule, so 70px below the ink's top row is clean fill.
        val fillLum = rgb(inkX, row + 70).let { c ->
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            (r * 299 + g * 587 + b * 114) / 1000
        }
        assertTrue(fillLum > 240, "capsule fill stays bright: $fillLum")
    }

    @Composable
    private fun Harness() {
        COUITheme {
            Scaffold(containerColor = Color(0xFFF0F0F0)) {
                Box(Modifier.fillMaxSize()) {
                    OverlayAlertDialog(
                        show = true,
                        onConfirm = {},
                        onCancel = {},
                        title = "要删除车辆吗?",
                        summary = "删除后，该车辆的记录将无法恢复。",
                        confirmText = "删除车辆",
                        cancelText = "取消",
                        // The desktop has no platform blur, so the panel is painted with the same
                        // value the device's blur layer lands on (~230) instead of pure white.
                        // Without it the translucent 0xAAFFFFFF capsule is invisible against a
                        // 255 panel and the probe measures nothing.
                        colors = AlertDialogDefaults.alertDialogColors(
                            backgroundColor = Color(0xFFE5E5E5),
                        ),
                    )
                }
            }
        }
    }

    private companion object {
        const val WIDTH_PX = 1272
        const val HEIGHT_PX = 2772
        const val DENSITY = 3.854f
        const val PANEL_THRESHOLD = 200
        const val CAPSULE_THRESHOLD = 240
    }
}
