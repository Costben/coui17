// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The popup's blur and shadow are the two places where a source token is **not** the value COUI's
 * engine or Compose's shadow painter takes, so both are pinned here: the compositor's 160px blur
 * radius is a Kawase ladder level, and the panel's rendered shadow is a Gaussian fitted on the
 * reference device rather than Skia's spot parameters. See `POPUP_BLUR_SIGMA_PX` and
 * `PopupShadow` for the derivations.
 */
class ListPopupMaterialTest {

    /** COUI's engine derives its sigma from the radius it is given with this slope. */
    private val engineSigmaScale = 0.45f

    /** Compose's `BlurEffect.convertRadiusToSigma`. */
    private val composeRadiusToSigma = 0.57735f

    /**
     * `RoundFrameLayout.DEFAULT_BLUR_RADIUS_PX` is a window-compositor level, not a radius. The
     * compositor renders it at sigma 25px on the reference device; the engine radius must convert
     * back to exactly that, and must not be the token itself (which asked for 72px).
     */
    @Test
    fun popupBlurConvertsTheCompositorLevelToItsRenderedSigma() {
        assertEquals(25f, popupEngineBlurRadiusPx() * engineSigmaScale, 1e-3f)
        assertEquals(72f, 160f * engineSigmaScale, 1e-3f)
        assertTrue(
            popupEngineBlurRadiusPx() < 160f,
            "the compositor level must not be handed to the engine as a radius",
        )
    }

    /**
     * The rendered profile: `A = 0.145`, `sigma = 16.58dp`, `offset = 6.42dp`. The radius is the one
     * whose `0.57735 r + 0.5` lands on that sigma.
     */
    @Test
    fun popupShadowLandsOnTheRenderedProfile() {
        val sigma = PopupShadow.radius.value * composeRadiusToSigma + 0.5f
        assertEquals(16.58f, sigma, 0.02f)
        assertEquals(0.145f, PopupShadow.color.alpha, 1e-3f)
        assertEquals(6.42f, PopupShadow.offset.y.value, 0.01f)
        assertEquals(0f, PopupShadow.offset.x.value, 0.01f)
        assertEquals(0.dp, PopupShadow.spread)
    }
}
