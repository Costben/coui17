// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The panel header pair has to keep the native `COUIToolbar` metrics.
 *
 * On ColorOS 17 a header action is a `COUIActionMenuItemView`: a 40dp square
 * (`coui_action_menu_item_min_width` in `values-v37`, down from the 48dp of older builds) holding a
 * 24dp icon, inside a 50dp title row (`coui_list_dialog_toolbar_height`). A plain [io.github.suqi8
 * .coui.kmp.basic.IconButton] would be 48dp with a rounded-square ripple, which is what made the
 * tick and the cross look wrong: too large, and square on press.
 */
@OptIn(ExperimentalTestApi::class)
class SheetActionButtonTest {

    @Test
    fun theHeaderActionKeepsTheNativeToolbarMetrics() = runSkikoComposeUiTest(
        size = Size(WIDTH_PX.toFloat(), HEIGHT_PX.toFloat()),
        density = Density(1f),
    ) {
        setContent { Harness() }
        waitForIdle()

        val start = onNodeWithTag("start").getBoundsInRoot()
        val end = onNodeWithTag("end").getBoundsInRoot()

        // coui_action_menu_item_min_width / _min_height = 40dp on API 37.
        assertEquals(40.dp, start.width)
        assertEquals(40.dp, start.height)
        assertEquals(40.dp, end.width)

        // The icons are 24dp inside them, i.e. 8dp of inset on every side.
        val icon = onNodeWithTag("startIcon", useUnmergedTree = true).getBoundsInRoot()
        assertEquals(24.dp, icon.width)
        assertEquals(24.dp, icon.height)
        assertEquals(8.dp, icon.left - start.left)

        // coui_list_dialog_toolbar_height = 50dp, so a 40dp button leaves 5dp above and below.
        assertEquals(5.dp, start.top)

        // Both actions sit on the same centre line.
        assertEquals(start.top + start.height / 2, end.top + end.height / 2)

        // The row is inset by toolbar_center_menu_padding_horizontal_compat = 16dp.
        assertEquals(16.dp, start.left)
    }

    @Composable
    private fun Harness() {
        COUITheme {
            Box(Modifier.fillMaxSize()) {
                TitleRow()
            }
        }
    }

    @Composable
    private fun TitleRow() {
        // Mirrors BottomSheetContentLayout's header: a 50dp row with 16dp side insets and one action
        // centred at each end. The real row is private, so the test rebuilds it from the same
        // defaults and asserts the pieces that make it up.
        Box(
            modifier = Modifier
                .size(width = 360.dp, height = BottomSheetDefaults.HeaderHeight)
                .padding(horizontal = BottomSheetDefaults.HeaderHorizontalPadding)
                .testTag("row"),
        ) {
            SheetActionButton(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .testTag("start"),
            ) {
                // Stands in for the 24dp icon: the assertion is on the slot's geometry, and the icon
                // set lives in a module this test source set does not depend on.
                Box(
                    Modifier.testTag("startIcon")
                        .size(BottomSheetDefaults.ActionButtonIconSize),
                )
            }
            SheetActionButton(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .testTag("end"),
            ) {
                Box(Modifier.size(BottomSheetDefaults.ActionButtonIconSize))
            }
        }
    }

    /**
     * `TYPE_FRAMEWORK_CIRCLE_1` is live on the device (`system_material_stroke_enable` = 1), so the
     * button has to paint its 1.9dp circular rim: the top of the circle reads brighter than the
     * transparent interior.
     */
    @Test
    fun theButtonPaintsItsCircularRim() = runSkikoComposeUiTest(
        size = Size(WIDTH_PX.toFloat(), HEIGHT_PX.toFloat()),
        density = Density(DENSITY),
    ) {
        setContent {
            COUITheme {
                Box(Modifier.fillMaxSize().background(Color(0xFFE5E5E5)), contentAlignment = Alignment.Center) {
                    SheetActionButton(onClick = {}, modifier = Modifier.testTag("button")) {
                        Box(Modifier.size(BottomSheetDefaults.ActionButtonIconSize))
                    }
                }
            }
        }
        waitForIdle()

        val image = onRoot().captureToImage().toAwtImage()
        val pixels = IntArray(image.width * image.height).also {
            image.getRGB(0, 0, image.width, image.height, it, 0, image.width)
        }
        fun lum(x: Int, y: Int): Int {
            val c = pixels[y * image.width + x]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            return (r * 299 + g * 587 + b * 114) / 1000
        }
        val centreX = image.width / 2
        val centreY = image.height / 2
        // 40dp at 3.854 px/dp is 154px, so the rim is the first ~7px of the circle.
        val column = (centreY - 78..centreY - 40).map { y -> lum(centreX, y) }
        println("RIM COLUMN $column")
        assertTrue(column.max() > column.min(), "the circular rim resolves above the interior: $column")
    }

    private companion object {
        const val WIDTH_PX = 400f
        const val HEIGHT_PX = 200f
        const val DENSITY = 3.854f
    }
}
