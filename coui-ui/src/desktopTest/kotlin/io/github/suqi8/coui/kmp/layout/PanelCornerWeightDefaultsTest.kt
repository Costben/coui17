// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.TooltipDefaults
import io.github.suqi8.coui.kmp.squircle.SquircleDefaults
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ColorOS 17 draws panels on one of two corner weights, and the radius token has to match it: a
 * `*_radius_large_16_1` radius belongs with weight 3.0, a `*_radius_normal_16_1` one with 2.5. Mixing
 * them leaves the material rim tracing a different curve than the silhouette underneath it.
 */
class PanelCornerWeightDefaultsTest {

    @Test
    fun theAlertPanelsPairTheLargeRadiusWithTheLargeWeight() {
        // coui_round_corner_xxxxl_radius_large_16_1 = 45dp, weight_16_1_large = 3.0.
        assertEquals(45.dp, DialogDefaults.CornerRadius)
        assertEquals(DialogDefaults.CornerRadius, BottomSheetDefaults.cornerRadius)
        assertEquals(SquircleDefaults.WeightLarge, DialogDefaults.CornerWeight)
        assertEquals(SquircleDefaults.WeightLarge, BottomSheetDefaults.CornerWeight)
    }

    @Test
    fun theTooltipBubbleUsesTheSingleNativeRadius() {
        // coui_tooltips_os_16_1_radius_17_dp: one bubble radius, plain and rich alike.
        assertEquals(17.dp, TooltipDefaults.PlainTooltipCornerRadius)
        assertEquals(17.dp, TooltipDefaults.RichTooltipCornerRadius)
    }
}
