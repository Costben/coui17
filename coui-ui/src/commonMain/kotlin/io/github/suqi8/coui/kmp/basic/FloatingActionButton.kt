// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
import io.github.suqi8.coui.kmp.blur.isBlurEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalContentColor

/**
 * A [FloatingActionButton] component with COUI style.
 *
 * It follows `COUIFloatingButton` on a ColorOS 17 material build:
 *
 * - **Touch motion.** The button owns a `COUITouchMotionAnimator` with deformation, rubber band and
 *   press scale enabled, so it shrinks under the finger, stretches toward a drag and springs back
 *   on release ([touchMotionEnabled]). Without it, the legacy `COUIFABPressFeedbackUtil` tween runs
 *   instead: 0.9 over 200ms, back over 340ms on `COUIMoveEaseInterpolator`.
 * - **Spotlight.** `FloatingButtonWrapper` lights the disc from the touch point with
 *   `TYPE_OPAQUE_SMALL_1` ([spotLightEnabled]).
 * - **Rim.** `ensureStrokeEffect` draws `TYPE_FRAMEWORK_CIRCLE_2` on the wrapper ([edgeStroke]).
 * - **Shadow.** With [causticShadowEnabled], `COUIFABShadowUtils.setOuterShadowEffect` replaces the
 *   level-three elevation with an 8dp shadow at alpha 30 and a caustic light in the button's own
 *   hue (`convertCausticShadowColor`, alpha 153) on the level-6 layout.
 * - **Blur.** With a [backdrop], the fill becomes `TYPE_FRAMEWORK_FAB`: a 10px blur under
 *   [containerColor] at alpha 229 (light) / 230 (dark) and a faint white `Overlay` layer. The
 *   pressed tint mask is off in that state, as `ensureBlurEffect` disables it; the spotlight is
 *   the press feedback.
 *
 * @param onClick The callback when the [FloatingActionButton] is clicked.
 * @param modifier The modifier to be applied to the [FloatingActionButton].
 * @param shape The shape of the [FloatingActionButton].
 * @param containerColor The color of the [FloatingActionButton].
 * @param shadowElevation The shadow elevation of the [FloatingActionButton] when
 *   [causticShadowEnabled] is `false`.
 * @param minWidth The minimum width of the [FloatingActionButton].
 * @param minHeight The minimum height of the [FloatingActionButton].
 * @param interactionSource The [MutableInteractionSource] to be used for the [FloatingActionButton].
 * @param backdrop The backdrop to blur under the button, or `null` for a flat fill.
 * @param touchMotionEnabled Whether the button deforms under the finger and follows a drag.
 * @param spotLightEnabled Whether pressing lights the button from the touch point.
 * @param edgeStroke The material rim, or `null` for none.
 * @param causticShadowEnabled Whether the button casts the material shadow with a caustic light.
 * @param contentColor The color of the content, white by default as on the system button.
 * @param content The [Composable] content of the [FloatingActionButton].
 */
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    containerColor: Color = COUITheme.colorScheme.primary,
    shadowElevation: Dp = FloatingActionButtonDefaults.ShadowElevation,
    minWidth: Dp = FloatingActionButtonDefaults.MinWidth,
    minHeight: Dp = FloatingActionButtonDefaults.MinHeight,
    interactionSource: MutableInteractionSource? = null,
    backdrop: Backdrop? = null,
    touchMotionEnabled: Boolean = true,
    spotLightEnabled: Boolean = true,
    edgeStroke: MaterialStrokeParams? = FloatingActionButtonDefaults.EdgeStroke,
    causticShadowEnabled: Boolean = true,
    contentColor: Color = FloatingActionButtonDefaults.ContentColor,
    content: @Composable () -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    val isPressed by interactionSource.collectIsPressedAsState()
    // COUIFABPressFeedbackUtil: pressing runs a 200ms tween from 1.0 to 0.9, releasing runs a 340ms
    // tween back to 1.0 from wherever it stopped; both use COUIMoveEaseInterpolator. Only the
    // fallback path uses it: with touch motion the scale comes from COUITouchMotionAnimator.
    val pressScale = remember { Animatable(1f) }
    LaunchedEffect(isPressed, touchMotionEnabled) {
        if (touchMotionEnabled) {
            pressScale.snapTo(1f)
        } else if (isPressed) {
            pressScale.animateTo(
                targetValue = FloatingActionButtonDefaults.PressedScale,
                animationSpec = tween(
                    durationMillis = FloatingActionButtonDefaults.PRESS_DURATION,
                    easing = FloatingActionButtonDefaults.MoveEase,
                ),
            )
        } else {
            pressScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = FloatingActionButtonDefaults.RELEASE_DURATION,
                    easing = FloatingActionButtonDefaults.MoveEase,
                ),
            )
        }
    }

    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    val glass = backdrop != null && isBlurEnabled()
    val fillModifier = if (glass) {
        val layers = remember(containerColor, isDark) { FloatingActionButtonDefaults.fabBlurColors(containerColor, isDark) }
        Modifier.couiGlassSurface(
            backdrop = backdrop,
            shape = shape,
            containerColor = containerColor,
            fallback = Modifier.background(containerColor, shape),
            blurRadius = with(LocalDensity.current) {
                (FloatingActionButtonDefaults.NATIVE_BLUR_RADIUS_PX / TEXTURE_BLUR_SIGMA_SCALE).toDp().value
            },
            blendColors = layers,
        )
    } else {
        Modifier.background(containerColor, shape)
    }
    // COUIFloatingButton sets OplusMaterialCornerParams(min(w, h) / 2, 2.0): the capsule distance
    // field of the rim shader.
    val rimModifier = if (edgeStroke != null) {
        Modifier.materialShadowEdge(
            stroke = edgeStroke,
            cornerWeight = MaterialShadowEdgeDefaults.CapsuleCornerWeight,
        )
    } else {
        Modifier
    }
    val shadowModifier = if (causticShadowEnabled) {
        val caustic = remember(containerColor) { FloatingActionButtonDefaults.causticColor(containerColor) }
        Modifier
            .shadow(
                elevation = FloatingActionButtonDefaults.CausticShadowElevation,
                shape = shape,
                clip = false,
                ambientColor = FloatingActionButtonDefaults.CausticShadowColor,
                spotColor = FloatingActionButtonDefaults.CausticShadowColor,
            )
            .causticShadowLight(
                color = caustic,
                profile = FloatingActionButtonDefaults.CausticProfile,
                cornerRadius = FloatingActionButtonDefaults.MinHeight / 2,
                alpha = { 1f },
            )
    } else {
        // COUIFABShadowUtils: the elevation shadow of the FAB is drawn with
        // coui_floating_button_elevation_color (#8A000000) instead of the platform colour.
        Modifier.shadow(
            elevation = shadowElevation,
            shape = shape,
            clip = false,
            ambientColor = FloatingActionButtonDefaults.ShadowColor,
            spotColor = FloatingActionButtonDefaults.ShadowColor,
        )
    }
    val surface = shadowModifier.then(fillModifier).then(rimModifier)
    val motionModifier = if (touchMotionEnabled) {
        Modifier.couiTouchMotion(
            surface = surface,
            shape = shape,
            lightParams = SpotLightDefaults.opaqueSmallParams1(isDark),
            lightEnabled = spotLightEnabled,
        )
    } else {
        Modifier
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            .then(surface)
            .then(
                if (spotLightEnabled) {
                    Modifier.pressSpotLight(params = SpotLightDefaults.opaqueSmallParams1(isDark), shape = shape)
                } else {
                    Modifier
                },
            )
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .semantics { role = Role.Button }
                .then(motionModifier)
                .clickable(
                    interactionSource = interactionSource,
                    // ensureBlurEffect turns the pressed mask off on the material fill.
                    indication = if (glass) null else LocalIndication.current,
                    onClick = onClick,
                )
                .defaultMinSize(minWidth = minWidth, minHeight = minHeight),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/** Binary-compatibility bridge for the [FloatingActionButton] overload that predates the material switches. */
@Deprecated(
    message = "Migrated to the overload that adds the touch motion and material switches.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    containerColor: Color = COUITheme.colorScheme.primary,
    shadowElevation: Dp = FloatingActionButtonDefaults.ShadowElevation,
    minWidth: Dp = FloatingActionButtonDefaults.MinWidth,
    minHeight: Dp = FloatingActionButtonDefaults.MinHeight,
    interactionSource: MutableInteractionSource? = null,
    backdrop: Backdrop? = null,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        shadowElevation = shadowElevation,
        minWidth = minWidth,
        minHeight = minHeight,
        interactionSource = interactionSource,
        backdrop = backdrop,
        touchMotionEnabled = true,
        spotLightEnabled = true,
        edgeStroke = FloatingActionButtonDefaults.EdgeStroke,
        causticShadowEnabled = true,
        contentColor = FloatingActionButtonDefaults.ContentColor,
        content = content,
    )
}

/** Contains default values used by [FloatingActionButton]. */
object FloatingActionButtonDefaults {
    /** The default minimum width of the [FloatingActionButton] (COUI coui_floating_button_size). */
    val MinWidth = 56.dp

    /** The default minimum height of the [FloatingActionButton] (COUI coui_floating_button_size). */
    val MinHeight = 56.dp

    /** The default shadow elevation of the [FloatingActionButton] (COUI support_shadow_size_level_three). */
    val ShadowElevation = 12.dp

    /** The colour of the elevation shadow (COUI `coui_floating_button_elevation_color`). */
    val ShadowColor = Color(0x8A000000)

    /** The scale the button shrinks to while pressed (COUIFABPressFeedbackUtil DEFAULT_PRESS_FEEDBACK_SCALE_MIN_VALUE). */
    val PressedScale = 0.9f

    /** The press-in duration (COUIFABPressFeedbackUtil DEFAULT_PRESS_FEEDBACK_ANIMATION_DURATION). */
    const val PRESS_DURATION = 200

    /** The release duration (COUIFABPressFeedbackUtil DEFAULT_RELEASE_ANIMATION_DURATION). */
    const val RELEASE_DURATION = 340

    /** The press feedback curve, `COUIMoveEaseInterpolator` (cubic-bezier(0.3, 0, 0.1, 1)). */
    val MoveEase: Easing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

    /** The content colour; the system button draws its glyph in opaque white in both themes. */
    val ContentColor = Color.White

    /**
     * `FRAMEWORK_CIRCLE_PARAMS_2_LIGHT` / `_DARK`, `TYPE_FRAMEWORK_CIRCLE_2`: identical in both
     * themes, a 2.5dp rim at alpha 0.25 with a 0.3 inner fade.
     */
    val EdgeStroke = MaterialStrokeParams(
        edgeAlpha = 0.25f,
        edgeWidth = 2.5.dp,
        edgeAngle = 0f,
        shadowFadeIn = 0.3f,
        shadowFadeOut = 1f,
        shadowFadeScale = 1f,
    )

    /** The material shadow elevation (`coui_shadow_elevation_caustic_fab`). */
    val CausticShadowElevation = 8.dp

    /** The material shadow colour (`coui_shadow_color_alpha_caustic_fab`, black at alpha 30). */
    val CausticShadowColor = Color(0x1E000000)

    /** The caustic layout (`coui_caustic_layout_b_level6` 11.2dp, `_sdf_level6` 14dp). */
    internal val CausticProfile = CausticShadowProfile(bottomOffset = 11.199982.dp, sdfWidth = 14.dp)

    /** The caustic alpha (`coui_caustic_shadow_color_alpha_caustic_fab`, 153). */
    private const val CAUSTIC_ALPHA = 153f / 255f

    /**
     * The caustic light colour for a button filled with [containerColor]:
     * `COUICausticShadowEffect.convertCausticShadowColor` rescales the colour to a luma of 80, with
     * `#828282` for black and `#0B0B0B` for white, at alpha 153.
     */
    fun causticColor(containerColor: Color): Color {
        val r = containerColor.red * 255f
        val g = containerColor.green * 255f
        val b = containerColor.blue * 255f
        val luma = 0.299f * r + 0.587f * g + 0.114f * b
        val rgb = when (luma) {
            0f -> Color(0xFF828282)

            255f -> Color(0xFF0B0B0B)

            else -> Color(
                red = (r * 80f / luma).coerceIn(0f, 255f) / 255f,
                green = (g * 80f / luma).coerceIn(0f, 255f) / 255f,
                blue = (b * 80f / luma).coerceIn(0f, 255f) / 255f,
            )
        }
        return rgb.copy(alpha = CAUSTIC_ALPHA)
    }

    /** The blur sigma of `FRAMEWORK_FAB_PARAMS`, in pixels. */
    const val NATIVE_BLUR_RADIUS_PX = 10f

    /**
     * The colour layers of `FRAMEWORK_FAB_PARAMS_LIGHT` / `_DARK`: the container colour
     * (`couiColorContainerTheme`) at alpha 229 / 230 in `SrcOver`, then `#1AFFFFFF` / `#19FFFFFF` in
     * `Overlay`.
     */
    fun fabBlurColors(containerColor: Color, isDark: Boolean): List<BlendColorEntry> = listOf(
        BlendColorEntry(color = containerColor.copy(alpha = (if (isDark) 230 else 229) / 255f)),
        BlendColorEntry(color = if (isDark) Color(0x19FFFFFF) else Color(0x1AFFFFFF), mode = BlurBlendMode.Overlay),
    )
}

/** Binary-compatibility bridge for the [FloatingActionButton] overload that predates `backdrop`. */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds backdrop.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    containerColor: Color = COUITheme.colorScheme.primary,
    shadowElevation: Dp = FloatingActionButtonDefaults.ShadowElevation,
    minWidth: Dp = FloatingActionButtonDefaults.MinWidth,
    minHeight: Dp = FloatingActionButtonDefaults.MinHeight,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        shadowElevation = shadowElevation,
        minWidth = minWidth,
        minHeight = minHeight,
        interactionSource = interactionSource,
        backdrop = null,
        touchMotionEnabled = true,
        spotLightEnabled = true,
        edgeStroke = FloatingActionButtonDefaults.EdgeStroke,
        causticShadowEnabled = true,
        contentColor = FloatingActionButtonDefaults.ContentColor,
        content = content,
    )
}
