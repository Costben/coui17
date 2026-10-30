// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.COUIInEase
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.squircle.squircleSurface
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalContentColor
import kotlin.coroutines.cancellation.CancellationException

/**
 * A [Button] component with COUI style.
 *
 * @param onClick The callback when the [Button] is clicked.
 * @param modifier The modifier to be applied to the [Button].
 * @param enabled Whether the [Button] is enabled.
 * @param pressScaleEnabled Whether the [Button] shrinks while pressed (COUIButton `scaleEnable`).
 *   Defaults to `true`, matching COUIButton's own default; `COUIAlertDialogBottomButtonNewNormal`
 *   also sets `scaleEnable=true` explicitly. Set to `false` only where the device style does.
 * @param cornerRadius The corner radius of the [Button]. `null` uses the radius of [size].
 * @param minWidth The minimum width of the [Button]. `null` uses the width of [size].
 * @param minHeight The minimum height of the [Button]. `null` uses the height of [size].
 * @param colors The [ButtonColors] of the [Button].
 * @param insideMargin The margin inside the [Button]. `null` uses the margin of [size].
 * @param interactionSource The [MutableInteractionSource] to be used for the [Button].
 * @param indication The [Indication] to be used for the [Button]. Defaults to `null` because
 *   the COUI press feedback (scale + press tint) is built into the button itself.
 * @param size The [ButtonSize] tier providing the default metrics when the explicit
 *   `cornerRadius` / `minWidth` / `minHeight` / `insideMargin` arguments are `null`.
 *   [ButtonSize.Regular] keeps the original [ButtonDefaults] metrics. New in 1.2.0; pass it (by name)
 *   to opt into a tier.
 * @param edgeStroke The rim drawn over the capsule fill, or `null` for no rim. ColorOS 17's
 *   `COUIButton` always draws one (`COUIShadowEdgeDrawable` fed by
 *   `COUIMaterialEffect.getStrokeParamsByType`); the default, [ButtonDefaults.defaultEdgeStroke],
 *   picks the preset matching the `ButtonType` implied by [colors]. The rim is drawn *over* the fill
 *   and *under* the label, inside the press-scale layer, exactly like the native draw order.
 * @param spotLight The press spotlight of a `COUIButton`, or `null` for no light. ColorOS 17 turns
 *   it on for the recommend button (`COUIButton.ButtonType.PRIMARY`) only, which is what
 *   [ButtonDefaults.defaultSpotLight] resolves. The light is painted *over* the fill and the rim and
 *   *under* the label, and follows the finger while pressed.
 * @param backdrop The backdrop to blur under the capsule, or `null` for a flat fill. Pass one when
 *   the button floats over content, as a `COUIButton` with its material effect enabled does. Inside
 *   a blurred app bar the blur radius drops toward 0.35x so the two blurs do not stack.
 * @param touchMotionEnabled Whether the button follows the finger with ColorOS 17's
 *   `COUITouchMotion` (rubber-band translation and area-preserving deformation). `false` falls back
 *   to the plain `COUIPressFeedbackHelper` scale.
 * @param content The [Composable] content of the [Button].
 */
@OptIn(ExperimentalCOUIApi::class)
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressScaleEnabled: Boolean = true,
    cornerRadius: Dp? = null,
    minWidth: Dp? = null,
    minHeight: Dp? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    insideMargin: PaddingValues? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    size: ButtonSize = ButtonSize.Regular,
    edgeStroke: MaterialStrokeParams? = ButtonDefaults.defaultEdgeStroke(colors),
    spotLight: SpotLightParams? = ButtonDefaults.defaultSpotLight(colors),
    backdrop: Backdrop? = null,
    touchMotionEnabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val tierDimens = COUITheme.dimens.button.tier(size)
    val resolvedCornerRadius = cornerRadius ?: tierDimens.cornerRadius
    val resolvedMinWidth = minWidth ?: tierDimens.minWidth
    val resolvedMinHeight = minHeight ?: tierDimens.minHeight
    val resolvedInsideMargin = insideMargin ?: tierDimens.insideMargin
    val rowModifier = remember(resolvedMinWidth, resolvedMinHeight, resolvedInsideMargin) {
        Modifier
            .defaultMinSize(minWidth = resolvedMinWidth, minHeight = resolvedMinHeight)
            .padding(resolvedInsideMargin)
    }
    val containerColor = if (enabled) colors.color else colors.disabledColor
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    val density = LocalDensity.current
    val blurScale = rememberToolbarAttenuatedBlurScale()
    val buttonShape = remember(resolvedCornerRadius) { SquircleShape(resolvedCornerRadius) }
    val touchMotionState = rememberCOUITouchMotionState()

    // COUI press feedback: scale/deformation (COUITouchMotion) + press tint (COUIMaskEffectDrawable).
    val isPressedFromInteraction by interactionSource.collectIsPressedAsState()
    val isPressed = isPressedFromInteraction || (touchMotionEnabled && touchMotionState.values.value.pressed)
    val scaleProgress by animateFloatAsState(
        targetValue = if (enabled && isPressed && pressScaleEnabled && !touchMotionEnabled) 1f else 0f,
        animationSpec = PressFeedbackSpring,
        label = "buttonPressScale",
    )
    val tintProgress = remember { Animatable(0f) }
    LaunchedEffect(enabled, isPressed) {
        if (enabled && isPressed) {
            tintProgress.animateTo(1f, PressFeedbackSpring)
        } else if (!enabled) {
            tintProgress.snapTo(0f)
        } else if (tintProgress.value > 0f) {
            if (tintProgress.value < PressedTintMinVisibleProgress) {
                // Keep the press-in animation running until the tint is clearly visible.
                try {
                    tintProgress.animateTo(1f, PressFeedbackSpring) {
                        if (value >= PressedTintMinVisibleProgress) throw PressTintThresholdReached()
                    }
                } catch (_: PressTintThresholdReached) {
                    // Minimum visible progress reached; fall through to the fade-out.
                }
            }
            tintProgress.animateTo(0f, PressFeedbackSpring)
        }
    }
    val pressTint = if (isDark) PressedTintDark else PressedTintLight
    val fillColor = if (tintProgress.value > 0f && containerColor.alpha > 0f) {
        pressTint.copy(alpha = pressTint.alpha * tintProgress.value).compositeOver(containerColor)
    } else {
        containerColor
    }
    // With a backdrop COUIButton swaps its flat fill for the TYPE_CONTENT_*_BUTTON material.
    val surfaceModifier = if (backdrop != null && fillColor.alpha > 0f) {
        val scheme = COUITheme.colorScheme
        val isAccent = ButtonDefaults.isAccentFill(colors.color, scheme.primary)
        val isDefaultSecondary = colors.color == scheme.secondaryVariant
        val material = remember(fillColor, isAccent, isDefaultSecondary, isDark, enabled) {
            ButtonDefaults.glassMaterial(fillColor, isAccent, isDefaultSecondary, isDark, enabled)
        }
        Modifier.couiGlassSurface(
            backdrop = backdrop,
            shape = buttonShape,
            containerColor = fillColor,
            fallback = Modifier.squircleSurface(color = fillColor, cornerRadius = resolvedCornerRadius),
            blurRadius = with(density) { (material.blurSigmaPx / TEXTURE_BLUR_SIGMA_SCALE).toDp().value } * blurScale(),
            blendColors = material.layers,
        )
    } else {
        Modifier.squircleSurface(color = fillColor, cornerRadius = resolvedCornerRadius)
    }
    // The native rim paints over the capsule fill (COUIButton.draw: background → mask/stroke →
    // text → shadow edge), so it must be a later modifier than the surface, and an earlier one than
    // the label. materialShadowEdge draws through onDrawBehind, which lands between the two.
    val rimModifier = if (edgeStroke != null && fillColor.alpha > 0f) {
        Modifier.materialShadowEdge(
            stroke = edgeStroke,
            cornerWeight = MaterialShadowEdgeDefaults.CapsuleCornerWeight,
            cornerRadius = resolvedCornerRadius,
            edgeStyle = GradientStrokeStyle.EDGE_STYLE_0,
            shadowStyle = InnerShadowStyle.SHADOW_STYLE_0,
        )
    } else {
        Modifier
    }
    // When touchMotionEnabled is active, couiTouchMotion drives both the rubber-band deformation and
    // the clipped spotlight; when disabled, fall back to pressSpotLight so explicit spotLight still works.
    val standaloneLightModifier = if (!touchMotionEnabled && spotLight != null && fillColor.alpha > 0f) {
        Modifier.pressSpotLight(
            params = spotLight,
            shape = buttonShape,
            enabled = enabled,
        )
    } else {
        Modifier
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        val composedSurface = surfaceModifier
            .then(rimModifier)
            .then(standaloneLightModifier)
        val motionWrapper = if (touchMotionEnabled) {
            Modifier.couiTouchMotion(
                surface = composedSurface,
                enabled = enabled,
                shape = buttonShape,
                lightParams = spotLight ?: SpotLightDefaults.capsuleParams1(isDark),
                motionState = touchMotionState,
                pressScaleEnabled = pressScaleEnabled,
                consumeDrag = false,
                lightEnabled = spotLight != null && fillColor.alpha > 0f,
            )
        } else {
            Modifier
                .graphicsLayer {
                    if (pressScaleEnabled) {
                        val scale = 1f - (1f - pressedScaleEndRatio(this.size)) * scaleProgress
                        scaleX = scale
                        scaleY = scale
                    }
                }
                .then(composedSurface)
        }
        Box(
            modifier = modifier
                .semantics { role = Role.Button }
                .then(motionWrapper)
                .clickable(
                    interactionSource = interactionSource,
                    indication = indication,
                    enabled = enabled,
                    onClick = onClick,
                ),
            propagateMinConstraints = true,
        ) {
            Row(
                modifier = rowModifier,
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/** Binary-compatibility bridge for the [Button] overload that predates `backdrop` and `touchMotionEnabled`. */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds backdrop and touchMotionEnabled.",
    level = DeprecationLevel.HIDDEN,
)
@OptIn(ExperimentalCOUIApi::class)
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressScaleEnabled: Boolean = true,
    cornerRadius: Dp? = null,
    minWidth: Dp? = null,
    minHeight: Dp? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    insideMargin: PaddingValues? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    size: ButtonSize = ButtonSize.Regular,
    edgeStroke: MaterialStrokeParams? = null,
    spotLight: SpotLightParams? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        pressScaleEnabled = pressScaleEnabled,
        cornerRadius = cornerRadius,
        minWidth = minWidth,
        minHeight = minHeight,
        colors = colors,
        insideMargin = insideMargin,
        interactionSource = interactionSource,
        indication = indication,
        size = size,
        edgeStroke = edgeStroke,
        spotLight = spotLight,
        backdrop = null,
        touchMotionEnabled = true,
        content = content,
    )
}

/** Binary-compatibility bridge for the [Button] overload that predates `spotLight`. */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds a spotLight parameter.",
    level = DeprecationLevel.HIDDEN,
)
@OptIn(ExperimentalCOUIApi::class)
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressScaleEnabled: Boolean = true,
    cornerRadius: Dp? = null,
    minWidth: Dp? = null,
    minHeight: Dp? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    insideMargin: PaddingValues? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    size: ButtonSize = ButtonSize.Regular,
    edgeStroke: MaterialStrokeParams? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        pressScaleEnabled = pressScaleEnabled,
        cornerRadius = cornerRadius,
        minWidth = minWidth,
        minHeight = minHeight,
        colors = colors,
        insideMargin = insideMargin,
        interactionSource = interactionSource,
        indication = indication,
        size = size,
        edgeStroke = edgeStroke,
        spotLight = null,
        content = content,
    )
}

/**
 * The 1.1.0-shaped [Button] overload: same parameter sequence as before the [ButtonSize] tier existed.
 *
 * Kept visible (not `HIDDEN`) so source written against 1.1.0 — including positional arguments and a
 * trailing `content` lambda — still compiles. It forwards to the overload that takes [ButtonSize] with
 * [ButtonSize.Regular], which is exactly the pre-1.2.0 metric set.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressScaleEnabled: Boolean = true,
    cornerRadius: Dp = COUITheme.dimens.button.regular.cornerRadius,
    minWidth: Dp = COUITheme.dimens.button.regular.minWidth,
    minHeight: Dp = COUITheme.dimens.button.regular.minHeight,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    insideMargin: PaddingValues = COUITheme.dimens.button.regular.insideMargin,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        pressScaleEnabled = pressScaleEnabled,
        cornerRadius = cornerRadius,
        minWidth = minWidth,
        minHeight = minHeight,
        colors = colors,
        insideMargin = insideMargin,
        interactionSource = interactionSource,
        indication = indication,
        size = ButtonSize.Regular,
        backdrop = null,
        touchMotionEnabled = true,
        content = content,
    )
}

/**
 * A [TextButton] component with COUI style.
 *
 * @param text The text of the [TextButton].
 * @param onClick The callback when the [TextButton] is clicked.
 * @param modifier The modifier to be applied to the [TextButton].
 * @param enabled Whether the [TextButton] is enabled.
 * @param pressScaleEnabled Whether the [TextButton] shrinks while pressed (COUIButton
 *   `scaleEnable`). Set to `false` for buttons that fill a container cell, such as the alert dialog
 *   button bar, leaving the full-cell press tint as the only feedback.
 * @param cornerRadius The corner radius of the [TextButton]. `null` uses the radius of [size].
 * @param minWidth The minimum width of the [TextButton]. `null` uses the width of [size].
 * @param minHeight The minimum height of the [TextButton]. `null` uses the height of [size].
 * @param colors The [TextButtonColors] of the [TextButton].
 * @param insideMargin The margin inside the [TextButton]. `null` uses the margin of [size].
 * @param textStyle The [TextStyle] of the label. Pass a 14sp style together with the `Small`
 *   metrics from [ButtonDefaults] to get the COUI small size tier.
 * @param interactionSource The [MutableInteractionSource] to be used for the [TextButton].
 * @param indication The [Indication] to be used for the [TextButton]. Defaults to `null` because
 *   the COUI press feedback (scale + press tint) is built into the button itself.
 * @param size The [ButtonSize] tier providing the default metrics when the explicit
 *   `cornerRadius` / `minWidth` / `minHeight` / `insideMargin` arguments are `null`.
 *   New in 1.2.0.
 * @param edgeStroke The rim drawn over the capsule fill, or `null` for no rim. See
 *   [Button]'s `edgeStroke`.
 * @param spotLight The press spotlight, or `null` for no light. See [Button]'s `spotLight`.
 * @param backdrop See [Button]'s `backdrop`.
 * @param touchMotionEnabled See [Button]'s `touchMotionEnabled`.
 */
@OptIn(ExperimentalCOUIApi::class)
@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressScaleEnabled: Boolean = true,
    cornerRadius: Dp? = null,
    minWidth: Dp? = null,
    minHeight: Dp? = null,
    colors: TextButtonColors = ButtonDefaults.textButtonColors(),
    insideMargin: PaddingValues? = null,
    textStyle: TextStyle = COUITheme.textStyles.button,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    size: ButtonSize = ButtonSize.Regular,
    edgeStroke: MaterialStrokeParams? = ButtonDefaults.defaultEdgeStroke(colors),
    spotLight: SpotLightParams? = ButtonDefaults.defaultSpotLight(colors),
    backdrop: Backdrop? = null,
    touchMotionEnabled: Boolean = true,
) {
    val mappedColors = remember(colors) {
        ButtonColors(
            color = colors.color,
            disabledColor = colors.disabledColor,
            contentColor = colors.textColor,
            disabledContentColor = colors.disabledTextColor,
        )
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        pressScaleEnabled = pressScaleEnabled,
        cornerRadius = cornerRadius,
        minWidth = minWidth,
        minHeight = minHeight,
        colors = mappedColors,
        insideMargin = insideMargin,
        interactionSource = interactionSource,
        indication = indication,
        size = size,
        edgeStroke = edgeStroke,
        spotLight = spotLight,
        backdrop = backdrop,
        touchMotionEnabled = touchMotionEnabled,
    ) {
        Text(
            text = text,
            style = textStyle,
        )
    }
}

/** Binary-compatibility bridge for the [TextButton] overload that predates `backdrop` and `touchMotionEnabled`. */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds backdrop and touchMotionEnabled.",
    level = DeprecationLevel.HIDDEN,
)
@OptIn(ExperimentalCOUIApi::class)
@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressScaleEnabled: Boolean = true,
    cornerRadius: Dp? = null,
    minWidth: Dp? = null,
    minHeight: Dp? = null,
    colors: TextButtonColors = ButtonDefaults.textButtonColors(),
    insideMargin: PaddingValues? = null,
    textStyle: TextStyle = COUITheme.textStyles.button,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    size: ButtonSize = ButtonSize.Regular,
    edgeStroke: MaterialStrokeParams? = null,
    spotLight: SpotLightParams? = null,
) {
    TextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        pressScaleEnabled = pressScaleEnabled,
        cornerRadius = cornerRadius,
        minWidth = minWidth,
        minHeight = minHeight,
        colors = colors,
        insideMargin = insideMargin,
        textStyle = textStyle,
        interactionSource = interactionSource,
        indication = indication,
        size = size,
        edgeStroke = edgeStroke,
        spotLight = spotLight,
        backdrop = null,
        touchMotionEnabled = true,
    )
}

/** Binary-compatibility bridge for the [TextButton] overload that predates `spotLight`. */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds a spotLight parameter.",
    level = DeprecationLevel.HIDDEN,
)
@OptIn(ExperimentalCOUIApi::class)
@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressScaleEnabled: Boolean = true,
    cornerRadius: Dp? = null,
    minWidth: Dp? = null,
    minHeight: Dp? = null,
    colors: TextButtonColors = ButtonDefaults.textButtonColors(),
    insideMargin: PaddingValues? = null,
    textStyle: TextStyle = COUITheme.textStyles.button,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    size: ButtonSize = ButtonSize.Regular,
    edgeStroke: MaterialStrokeParams? = null,
) {
    TextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        pressScaleEnabled = pressScaleEnabled,
        cornerRadius = cornerRadius,
        minWidth = minWidth,
        minHeight = minHeight,
        colors = colors,
        insideMargin = insideMargin,
        textStyle = textStyle,
        interactionSource = interactionSource,
        indication = indication,
        size = size,
        edgeStroke = edgeStroke,
        spotLight = null,
        backdrop = null,
        touchMotionEnabled = true,
    )
}

/**
 * The 1.1.0-shaped [TextButton] overload: same parameter sequence as before the [ButtonSize] tier
 * existed. Kept visible so old positional calls still compile; forwards with [ButtonSize.Regular].
 */
@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressScaleEnabled: Boolean = true,
    cornerRadius: Dp = ButtonDefaults.CornerRadius,
    minWidth: Dp = ButtonDefaults.MinWidth,
    minHeight: Dp = ButtonDefaults.MinHeight,
    colors: TextButtonColors = ButtonDefaults.textButtonColors(),
    insideMargin: PaddingValues = ButtonDefaults.InsideMargin,
    textStyle: TextStyle = COUITheme.textStyles.button,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
) {
    TextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        pressScaleEnabled = pressScaleEnabled,
        cornerRadius = cornerRadius,
        minWidth = minWidth,
        minHeight = minHeight,
        colors = colors,
        insideMargin = insideMargin,
        textStyle = textStyle,
        interactionSource = interactionSource,
        indication = indication,
        size = ButtonSize.Regular,
        backdrop = null,
        touchMotionEnabled = true,
    )
}

/**
 * The size tier of a [Button] / [TextButton].
 *
 * Each tier maps to a set of metrics in [ButtonDefaults]: [Regular] uses the plain
 * `MinWidth` / `MinHeight` / `CornerRadius` / `InsideMargin` constants, [Small] the `...Small`
 * ones and [Tiny] the `...Tiny` ones. Explicit `minWidth`, `minHeight`, `cornerRadius` and
 * `insideMargin` arguments override the selected tier.
 *
 * New in 1.2.0.
 *
 * Not annotated with [ExperimentalCOUIApi]: it is a parameter type of the long-stable `Button`, and
 * marking it would force every `Button(...)` call site onto an opt-in.
 */
enum class ButtonSize {
    /** The default size tier ([ButtonDefaults.MinWidth] / `MinHeight` / `CornerRadius` / `InsideMargin`). */
    Regular,

    /** The small size tier (`ButtonDefaults.MinWidthSmall` / `MinHeightSmall` / `CornerRadiusSmall` / `InsideMarginSmall`). */
    Small,

    /** The tiny size tier (`ButtonDefaults.MinWidthTiny` / `MinHeightTiny` / `CornerRadiusTiny` / `InsideMarginTiny`). */
    Tiny,
}

object ButtonDefaults {

    /**
     * The smallest scale a button shrinks to while pressed (COUIPressFeedbackHelper end ratio
     * for surfaces up to 48 x 48dp).
     */
    val PressedScale = 0.92f

    /**
     * The legacy COUIButton `brightness` attribute value. Retained for source compatibility only
     * and no longer used by [Button].
     */
    val PressedBrightness = 0.8f

    /**
     * The default min width applied for all buttons. Note that you can override it by applying
     * Modifier.widthIn directly on the button composable.
     */
    val MinWidth = 58.dp

    /**
     * The default min height applied for all buttons. Note that you can override it by applying
     * Modifier.heightIn directly on the button composable.
     */
    val MinHeight = 44.dp

    /**
     * The default corner radius applied for all buttons (COUI height-derived capsule:
     * radius = [MinHeight] / 2).
     */
    val CornerRadius = 22.dp

    /**
     * The default inside margin applied for all buttons.
     */
    val InsideMargin = PaddingValues(horizontal = 12.dp, vertical = 0.dp)

    /**
     * The min width of the small size tier (COUI coui_btn_small_width_min).
     */
    val MinWidthSmall = 52.dp

    /**
     * The min height of the small size tier (COUI coui_btn_small_height_min).
     */
    val MinHeightSmall = 28.dp

    /**
     * The corner radius of the small size tier (COUI height-derived capsule: radius = [MinHeightSmall] / 2).
     */
    val CornerRadiusSmall = 14.dp

    /**
     * The inside margin of the small size tier (COUI Widget.COUI.Button.Small padding 12dp/4dp).
     */
    val InsideMarginSmall = PaddingValues(horizontal = 12.dp, vertical = 4.dp)

    /**
     * The min width of the tiny size tier. Derived from the COUI small tier ([MinWidthSmall],
     * coui_btn_small_width_min) stepped down by 8dp; no separate COUI tiny token exists.
     *
     * New in 1.2.0.
     */
    @ExperimentalCOUIApi
    val MinWidthTiny = 44.dp

    /**
     * The min height of the tiny size tier. Derived from the COUI small tier ([MinHeightSmall],
     * coui_btn_small_height_min) by the same 8dp step.
     *
     * New in 1.2.0.
     */
    @ExperimentalCOUIApi
    val MinHeightTiny = 20.dp

    /**
     * The corner radius of the tiny size tier (height-derived capsule: radius = [MinHeightTiny] / 2).
     *
     * New in 1.2.0.
     */
    @ExperimentalCOUIApi
    val CornerRadiusTiny = 10.dp

    /**
     * The inside margin of the tiny size tier. Derived from the COUI small tier ([InsideMarginSmall],
     * 12dp/4dp) tightened to 8dp/2dp.
     *
     * New in 1.2.0.
     */
    @ExperimentalCOUIApi
    val InsideMarginTiny = PaddingValues(horizontal = 8.dp, vertical = 2.dp)

    /**
     * The default [ButtonColors] for all buttons.
     */
    @Composable
    fun buttonColors(
        color: Color = COUITheme.colorScheme.secondaryVariant,
        disabledColor: Color = COUITheme.colorScheme.disabledSecondaryVariant,
        contentColor: Color = COUITheme.colorScheme.onSecondaryVariant,
        disabledContentColor: Color = COUITheme.colorScheme.disabledOnSecondaryVariant,
    ): ButtonColors = remember(color, disabledColor, contentColor, disabledContentColor) {
        ButtonColors(
            color = color,
            disabledColor = disabledColor,
            contentColor = contentColor,
            disabledContentColor = disabledContentColor,
        )
    }

    /**
     * The [ButtonColors] for primary buttons.
     */
    @Composable
    fun buttonColorsPrimary(
        color: Color = COUITheme.colorScheme.primary,
        disabledColor: Color = COUITheme.colorScheme.disabledPrimaryButton,
        contentColor: Color = COUITheme.colorScheme.onPrimary,
        disabledContentColor: Color = COUITheme.colorScheme.disabledOnPrimaryButton,
    ): ButtonColors = remember(color, disabledColor, contentColor, disabledContentColor) {
        ButtonColors(
            color = color,
            disabledColor = disabledColor,
            contentColor = contentColor,
            disabledContentColor = disabledContentColor,
        )
    }

    /**
     * The default [TextButtonColors] for all text buttons.
     */
    @Composable
    fun textButtonColors(
        color: Color = COUITheme.colorScheme.secondaryVariant,
        disabledColor: Color = COUITheme.colorScheme.disabledSecondaryVariant,
        textColor: Color = COUITheme.colorScheme.onSecondaryVariant,
        disabledTextColor: Color = COUITheme.colorScheme.disabledOnSecondaryVariant,
    ): TextButtonColors = remember(color, disabledColor, textColor, disabledTextColor) {
        TextButtonColors(
            color = color,
            disabledColor = disabledColor,
            textColor = textColor,
            disabledTextColor = disabledTextColor,
        )
    }

    /**
     * The [TextButtonColors] for primary text buttons.
     */
    @Composable
    fun textButtonColorsPrimary(
        color: Color = COUITheme.colorScheme.primary,
        disabledColor: Color = COUITheme.colorScheme.disabledPrimaryButton,
        textColor: Color = COUITheme.colorScheme.onPrimary,
        disabledTextColor: Color = COUITheme.colorScheme.disabledOnPrimaryButton,
    ): TextButtonColors = remember(color, disabledColor, textColor, disabledTextColor) {
        TextButtonColors(
            color = color,
            disabledColor = disabledColor,
            textColor = textColor,
            disabledTextColor = disabledTextColor,
        )
    }

    /**
     * The [TextButtonColors] for borderless / translate text buttons (COUI
     * Widget.COUI.Button.Large.Borderless / Translate): no fill, primary-tinted label.
     */
    @Composable
    fun textButtonColorsBorderless(
        color: Color = Color.Transparent,
        disabledColor: Color = Color.Transparent,
        textColor: Color = COUITheme.colorScheme.primary,
        disabledTextColor: Color = COUITheme.colorScheme.disabledPrimary,
    ): TextButtonColors = remember(color, disabledColor, textColor, disabledTextColor) {
        TextButtonColors(
            color = color,
            disabledColor = disabledColor,
            textColor = textColor,
            disabledTextColor = disabledTextColor,
        )
    }

    /**
     * The rim `COUIButton` draws for [colors]: `TYPE_CONTENT_CAPSULE_4` for a filled accent
     * (`ButtonType.PRIMARY`) button, `TYPE_CONTENT_CAPSULE_5` for a secondary one, and none for a
     * borderless button whose fill is transparent.
     */
    @Composable
    fun defaultEdgeStroke(colors: ButtonColors): MaterialStrokeParams? = edgeStrokeFor(colors.color)

    /** [defaultEdgeStroke] for [TextButtonColors]. */
    @Composable
    fun defaultEdgeStroke(colors: TextButtonColors): MaterialStrokeParams? = edgeStrokeFor(colors.color)

    /**
     * The press spotlight `COUIButton` turns on for [colors]: [SpotLightDefaults.capsuleParams1] for
     * the recommend button (`ButtonType.PRIMARY`), `null` for every other type.
     */
    @Composable
    fun defaultSpotLight(colors: ButtonColors): SpotLightParams? = spotLightFor(colors.color)

    /** [defaultSpotLight] for [TextButtonColors]. */
    @Composable
    fun defaultSpotLight(colors: TextButtonColors): SpotLightParams? = spotLightFor(colors.color)

    @Composable
    private fun edgeStrokeFor(fill: Color): MaterialStrokeParams? {
        if (fill.alpha == 0f) return null
        val scheme = COUITheme.colorScheme
        return when {
            isAccentFill(fill, scheme.primary) -> MaterialStrokeParams.ContentCapsule4
            else -> MaterialStrokeParams.contentCapsule5(scheme.background.luminance() < 0.5f)
        }
    }

    @Composable
    private fun spotLightFor(fill: Color): SpotLightParams? {
        val scheme = COUITheme.colorScheme
        return if (fill.alpha > 0f && isAccentFill(fill, scheme.primary)) {
            SpotLightDefaults.capsuleParams1(scheme.background.luminance() < 0.5f)
        } else {
            null
        }
    }

    /** Whether [fill] is the theme accent, i.e. the button is `ButtonType.PRIMARY`. */
    internal fun isAccentFill(fill: Color, primary: Color): Boolean = fill.copy(alpha = 1f) == primary.copy(alpha = 1f)

    /**
     * The material `COUIButton.applyBlurEffect` lays over the blurred backdrop.
     *
     * A secondary button takes `TYPE_CONTENT_SECONDARY_BUTTON` (25px, `#CCFFFFFF` / `#33FFFFFF` in
     * `SrcOver`); a primary one takes `TYPE_CONTENT_PRIMARY_BUTTON` (10px, `couiColorContainerTheme`
     * at alpha 229 / 230 in `SrcOver`, then `#00FFFFFF` / `#19FFFFFF` in `Overlay`), or its disabled
     * variant at alpha 77. A fill that is neither the theme accent nor the default secondary colour
     * keeps its own colour over the blur.
     */
    internal fun glassMaterial(
        fill: Color,
        isAccent: Boolean,
        isDefaultSecondary: Boolean,
        isDark: Boolean,
        enabled: Boolean,
    ): ButtonGlassMaterial = when {
        isAccent -> ButtonGlassMaterial(
            blurSigmaPx = 10f,
            layers = listOf(
                BlendColorEntry(
                    color = fill.copy(
                        alpha = (
                            if (!enabled) {
                                77
                            } else if (isDark) {
                                230
                            } else {
                                229
                            }
                            ) / 255f,
                    ),
                ),
                BlendColorEntry(
                    color = if (isDark || !enabled) Color(0x19FFFFFF) else Color(0x00FFFFFF),
                    mode = BlurBlendMode.Overlay,
                ),
            ),
        )

        isDefaultSecondary -> ButtonGlassMaterial(
            blurSigmaPx = 25f,
            layers = listOf(BlendColorEntry(color = if (isDark) Color(0x33FFFFFF) else Color(0xCCFFFFFF))),
        )

        else -> ButtonGlassMaterial(blurSigmaPx = 25f, layers = listOf(BlendColorEntry(color = fill)))
    }
}

/** A blur sigma in pixels plus the colour layers laid over it, as `COUIMaterialEffect` hands them over. */
@Immutable
internal data class ButtonGlassMaterial(val blurSigmaPx: Float, val layers: List<BlendColorEntry>)

@Immutable
data class ButtonColors(
    val color: Color,
    val disabledColor: Color,
    val contentColor: Color,
    val disabledContentColor: Color,
)

@Immutable
data class TextButtonColors(
    val color: Color,
    val disabledColor: Color,
    val textColor: Color,
    val disabledTextColor: Color,
)

/** The COUI press feedback spring (COUISpringForce response 0.3f / bounce 0f). */
private val PressFeedbackSpring = spring<Float>(dampingRatio = 1f, stiffness = 438.65f)

/** couiColorPress in light themes (coui_color_press #1F000000). */
private val PressedTintLight = Color(0x1F000000)

/** couiColorPress in dark themes (coui_color_press_dark #33FFFFFF). */
private val PressedTintDark = Color(0x33FFFFFF)

/** The minimum press tint progress before a release may fade out (COUIMaskEffectDrawable). */
private val PressedTintMinVisibleProgress = 0.7f

/** Thrown to stop the deferred press-in tint animation once [PressedTintMinVisibleProgress] is reached. */
private class PressTintThresholdReached : CancellationException("Press tint reached the minimum visible progress")

/** COUI `COUIInEaseInterpolator`; shares [COUIInEase]. Maps the button area to the pressed end scale. */
private val PressedScaleEasing = COUIInEase

/** COUIPressFeedbackHelper area bounds: coui_min_end_value_size (48dp) squared. */
private val PressedScaleMinAreaDp2 = 48f * 48f

/** COUIPressFeedbackHelper area bounds: coui_max_end_value_width x height (328dp x 220dp). */
private val PressedScaleMaxAreaDp2 = 328f * 220f

/** The pressed end scale of the largest surfaces (COUIPressFeedbackHelper MAX_SCALE_END_RATIO). */
private val PressedScaleMax = 0.98f

/** The scale a surface of [size] shrinks to when fully pressed (COUIPressFeedbackHelper.getCardScaleRatio). */
private fun Density.pressedScaleEndRatio(size: Size): Float {
    val area = size.width.toDp().value * size.height.toDp().value
    return when {
        area <= PressedScaleMinAreaDp2 -> ButtonDefaults.PressedScale

        area >= PressedScaleMaxAreaDp2 -> PressedScaleMax

        else -> ButtonDefaults.PressedScale + (PressedScaleMax - ButtonDefaults.PressedScale) *
            PressedScaleEasing.transform((area - PressedScaleMinAreaDp2) / (PressedScaleMaxAreaDp2 - PressedScaleMinAreaDp2))
    }
}
