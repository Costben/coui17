// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
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

/**
 * A [FloatingActionButton] component with COUI style.
 *
 * While pressed, the whole button shrinks to [FloatingActionButtonDefaults.PressedScale] with a
 * 200ms `COUIMoveEaseInterpolator` tween and grows back over 340ms when released
 * (COUIFABPressFeedbackUtil); the pressed tint overlay comes from `LocalIndication`.
 *
 * @param onClick The callback when the [FloatingActionButton] is clicked.
 * @param modifier The modifier to be applied to the [FloatingActionButton].
 * @param shape The shape of the [FloatingActionButton].
 * @param containerColor The color of the [FloatingActionButton].
 * @param shadowElevation The shadow elevation of the [FloatingActionButton].
 * @param minWidth The minimum width of the [FloatingActionButton].
 * @param minHeight The minimum height of the [FloatingActionButton].
 * @param interactionSource The [MutableInteractionSource] to be used for the [FloatingActionButton].
 * @param backdrop The backdrop to blur under the button, or `null` for a flat fill. With one, the
 *   button wears `COUIFloatingButton`'s `TYPE_FRAMEWORK_FAB` material instead: a 10px blur under
 *   [containerColor] at alpha 229 (light) / 230 (dark) and a faint white `Overlay` layer, so the
 *   content behind it shows through the accent.
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
    content: @Composable () -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    val isPressed by interactionSource.collectIsPressedAsState()
    // COUIFABPressFeedbackUtil: pressing runs a 200ms tween from 1.0 to 0.9, releasing runs a 340ms
    // tween back to 1.0 from wherever it stopped; both use COUIMoveEaseInterpolator.
    val pressScale = remember { Animatable(1f) }
    LaunchedEffect(isPressed) {
        if (isPressed) {
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

    val glass = backdrop != null && isBlurEnabled()
    val glassModifier = if (glass) {
        val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
        val layers = remember(containerColor, isDark) { FloatingActionButtonDefaults.fabBlurColors(containerColor, isDark) }
        Modifier.couiGlassSurface(
            backdrop = backdrop,
            shape = shape,
            containerColor = containerColor,
            fallback = Modifier,
            blurRadius = with(LocalDensity.current) {
                (FloatingActionButtonDefaults.NATIVE_BLUR_RADIUS_PX / TEXTURE_BLUR_SIGMA_SCALE).toDp().value
            },
            blendColors = layers,
        )
    } else {
        Modifier
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .semantics { role = Role.Button }
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            // COUIFABShadowUtils: the elevation shadow of the FAB is drawn with
            // coui_floating_button_elevation_color (#8A000000) instead of the platform colour.
            // The Surface only provides the shape / fill, so its own shadow is switched off.
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                clip = false,
                ambientColor = FloatingActionButtonDefaults.ShadowColor,
                spotColor = FloatingActionButtonDefaults.ShadowColor,
            )
            .then(glassModifier),
        shape = shape,
        color = if (glass) Color.Transparent else containerColor,
        shadowElevation = 0.dp,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier.defaultMinSize(
                minWidth = minWidth,
                minHeight = minHeight,
            ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
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
        content = content,
    )
}
