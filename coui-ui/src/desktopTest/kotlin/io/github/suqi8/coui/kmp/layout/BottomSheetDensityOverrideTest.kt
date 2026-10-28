// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A sheet whose content outgrows the window must stop at the window top even when the app lays
 * out at its own density.
 *
 * The COUI gallery pins `LocalDensity` to the ColorOS system-app density (3.875 px/dp), above the
 * 3.0 a 1080px phone reports. The sheet used to cap its height with `containerDpSize`, which is
 * converted at the platform density, so the cap came out 29% taller than the window and a tall
 * sheet ran up under the status bar: the cap is the window height less the top inset, and the
 * inflated window height swallowed the inset.
 */
@OptIn(ExperimentalTestApi::class)
class BottomSheetDensityOverrideTest {

    @Test
    fun aTallSheetStaysInsideTheWindowUnderADensityOverride() = runSkikoComposeUiTest(
        size = Size(WIDTH_PX, HEIGHT_PX),
        density = Density(PLATFORM_DENSITY),
    ) {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(APP_DENSITY)) {
                COUITheme {
                    BottomSheetContentLayout(
                        show = true,
                        backgroundColor = Color.White,
                        cornerRadius = 16.dp,
                        sheetMaxWidth = 10_000.dp,
                        outsideMargin = DpSize(0.dp, 0.dp),
                        insideMargin = DpSize(0.dp, 0.dp),
                        dragHandleColor = Color.Gray,
                        popupHost = { visible, content -> if (visible) content() },
                        topInset = TOP_INSET_DP.dp,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(10_000.dp)
                                .testTag("content"),
                        )
                    }
                }
            }
        }
        waitForIdle()

        // In pixels, so the assertion does not depend on which density the bounds are reported in.
        // With the cap computed at the platform density the sheet asks for 700dp (1400px), is clamped
        // to the 800px window and covers the status bar strip.
        val contentTopPx = onNodeWithTag("content").fetchSemanticsNode().boundsInRoot.top
        val insetPx = TOP_INSET_DP * APP_DENSITY
        assertTrue(
            contentTopPx >= insetPx,
            "the sheet stops below the top inset ($insetPx px), content top = $contentTopPx px",
        )
    }

    private companion object {
        const val WIDTH_PX = 400f
        const val HEIGHT_PX = 800f
        const val PLATFORM_DENSITY = 1f
        const val APP_DENSITY = 2f
        const val TOP_INSET_DP = 100
    }
}
