// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The shadow a COUI search capsule casts, shared by the home bar's collapsed pill and the expanded
 * search field.
 *
 * The device wraps the capsule in a `mSearchBarBackgroundView` and drives it with
 * `COUISearchBar.setNoShadowInNormalState(!appBar.isCollapsed())`: the view drops the shadow only
 * while the bar is at rest *and* the search page is not open, and keeps it once the page has
 * scrolled or the field has expanded. The shadow itself is COUI's SHADOW_LV3_A, applied through
 * `ShadowUtils.setElevationToView(view, 5, f)` the same way the popup surface gets SHADOW_LV4 (see
 * `ListPopup.PopupShadow`).
 *
 * On an OPlus-view-elevation SDK that call resolves SHADOW_LV3_A and hands five values to
 * `OplusView.setOverrideLightSourceGeometry`:
 *
 * | source parameter | value | COUI token |
 * | :-- | :-- | :-- |
 * | occluder height (elevation) | 100dp | `coui_shadow_elevation_level3a` |
 * | spot + ambient colour | `argb(34, 0, 0, 0)` / `argb(68, 0, 0, 0)` at night | `coui_shadow_color_lv3a` |
 * | lightX / lightY | -1 / -200dp | `coui_shadow_light_y_level3a` |
 * | lightZ | 10000dp | `coui_shadow_light_z_level3a` |
 * | light radius | 2000dp | `coui_shadow_light_r_level3a` |
 * | extra blur radius | 0dp | `coui_shadow_blur_r_level3a` |
 *
 * hwui renders the caster through Skia's spot-shadow metrics
 * (`SkDrawShadowMetrics::GetSpotParams`), so there is no closed form to copy — the same model
 * `ListPopup.PopupShadow` approximates with a plain [Shadow] gives:
 *
 * ```
 * zRatio     = clamp(elevation / (lightZ - elevation), 0, 0.95) = 100 / 9900 = 0.01010
 * offset     = (-zRatio * lightX, -zRatio * lightY)                            = (0.01, +2.02) dp
 * blurRadius = lightRadius * zRatio                                            = 20.20 dp
 * ```
 *
 * The offset is horizontally centred: lightX is the literal -1 COUI passes. `radius` is handed to
 * the platform blur as-is: Compose's shadow painter feeds `Shadow.radius` straight into
 * `android.graphics.BlurMaskFilter(radius, Blur.NORMAL)` (`DropShadowRenderer`), which takes
 * sigma = 0.57735 · radius internally — there is no `ConvertRadiusToSigma` step on this path, so
 * `radius` is *not* the sigma. The soft tail therefore runs about 2 sigma ≈ 1.15 · radius past the
 * shape, which is what the 20.20dp above is sized for.
 *
 * The omni-directional ambient half of the native shadow is dropped, as it is for `PopupShadow`:
 * this modifier draws one shadow. That is the known residual — the far field comes out ~2.6 luma
 * lighter than the device with a tail ~40px short. A second wide [Shadow] standing in for the
 * ambient half was tried and made the near field worse, so it is left out.
 *
 * @param fraction how much of the shadow to show. The collapsed home pill fades it in with the
 *   scroll (`min(1, scrollPx / 30dp)`); the expanded search field shows all of it.
 */
@Composable
fun searchBarShadow(fraction: Float = 1f): Shadow {
    val dark = COUITheme.colorScheme.background.luminance() < 0.5f
    val alpha = (if (dark) 68f else 34f) / 255f * fraction.coerceIn(0f, 1f)
    return remember(alpha, dark) {
        Shadow(
            radius = 20.20.dp,
            color = Color.Black.copy(alpha = alpha),
            spread = 0.dp,
            offset = DpOffset(0.dp, 2.02.dp),
        )
    }
}
