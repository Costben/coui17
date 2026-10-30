// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * A [FloatingActionButton] component with COUI style.
 *
 * While pressed, the whole button springs down to [FloatingActionButtonDefaults.PressedScale]
 * (COUIPressFeedbackHelper); the pressed tint overlay comes from `LocalIndication`.
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
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) FloatingActionButtonDefaults.PressedScale else 1f,
        // The COUI press feedback spring (COUISpringForce response 0.3f / bounce 0f).
        animationSpec = spring(dampingRatio = 1f, stiffness = 438.65f),
        label = "fabPressScale",
    )

    val glass = backdrop != null && isRuntimeShaderSupported()
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
                scaleX = pressScale
                scaleY = pressScale
            }
            .then(glassModifier),
        shape = shape,
        color = if (glass) Color.Transparent else containerColor,
        shadowElevation = shadowElevation,
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

    /** The scale the button springs down to while pressed (COUIPressFeedbackHelper end ratio). */
    val PressedScale = 0.92f

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
