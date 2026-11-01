// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.anim.COUIInEase
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Close
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalContentColor
import kotlin.coroutines.cancellation.CancellationException

/**
 * A [Chip] component with COUI style.
 *
 * A capsule shaped, checkable filter tag mirroring ColorOS's COUIChip in its selection style
 * (Widget.COUI.Chip.Choice): the unselected chip is a translucent gray capsule with a primary
 * label; selecting it fills the capsule with the accent color and flips the label to white.
 * The selected/unselected colors cross-fade with the same critically damped spring
 * COUIChipDrawable.TintAnimation uses, and pressing shrinks the chip while tinting the fill
 * (COUIPressFeedbackHelper + COUIMaskEffectDrawable).
 *
 * @param selected Whether the [Chip] is selected.
 * @param onClick The callback when the [Chip] is clicked.
 * @param label The text label of the [Chip]. Single line, ellipsized beyond
 *   [ChipDefaults.TextMaxWidth].
 * @param modifier The modifier to be applied to the [Chip].
 * @param enabled Whether the [Chip] is enabled.
 * @param icon The optional leading icon of the [Chip], laid out in a [ChipDefaults.IconSize] box.
 *   [LocalContentColor] is provided with the current label color so a default [Icon] is tinted
 *   like COUIChip with chipIconApplyTint enabled.
 * @param onDismiss The optional callback invoked when the trailing dismiss affordance is clicked.
 *   When non-null the chip grows a trailing close button that is an independent [Role.Button]
 *   target: it never toggles [selected] and is disabled together with the chip. New in 1.2.0.
 * @param dismissIcon The optional content of the dismiss affordance. Defaults to the built-in
 *   [COUIIcons.Basic.Close] glyph when `null`. New in 1.2.0.
 * @param dismissContentDescription The content description of the dismiss affordance, exposed as
 *   semantics so accessibility services and UI tests can find it. New in 1.2.0.
 * @param dismissButtonSize The touch target of the dismiss affordance. Defaults to
 *   [ChipDefaults.DismissButtonSize] (28dp, COUI
 *   `coui_chip_input_style_close_icon_touch_bounds_size`). New in 1.2.0.
 * @param count The optional trailing count text, drawn after the label in a
 *   [ChipDefaults.CountTextSize] style (COUIChipDrawable countText / `COUNT_TEXT_SIZE`) and spoken
 *   as part of the chip's content description (COUIChip.onInitializeAccessibilityNodeInfo).
 *   New in 1.2.0.
 * @param showRedDot Whether to draw the COUI hint red dot at the top-end corner of the label
 *   (COUIChipDrawable drawRedDot, COUIHintRedDot.Small). New in 1.2.0.
 * @param cornerRadius The corner radius of the [Chip]. The default resolves to half the measured
 *   height, as COUIChipDrawable's `-1` does, and follows the system capsule curve; an explicit
 *   smaller radius keeps the Lp squircle.
 * @param minWidth The minimum width of the [Chip].
 * @param minHeight The minimum height of the [Chip].
 * @param maxWidth The maximum width of the [Chip].
 * @param colors The [ChipColors] of the [Chip].
 * @param insideMargin The horizontal padding inside the [Chip].
 * @param interactionSource The [MutableInteractionSource] to be used for the [Chip].
 * @param indication The [Indication] to be used for the [Chip]. Defaults to `null` because the
 *   COUI press feedback (scale + press tint) is built into the chip itself.
 */
@OptIn(ExperimentalCOUIApi::class)
@Composable
fun Chip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    cornerRadius: Dp = COUITheme.dimens.chip.cornerRadius,
    minWidth: Dp = COUITheme.dimens.chip.minWidth,
    minHeight: Dp = COUITheme.dimens.chip.minHeight,
    maxWidth: Dp = COUITheme.dimens.chip.maxWidth,
    colors: ChipColors = ChipDefaults.chipColors(),
    insideMargin: PaddingValues = COUITheme.dimens.chip.insideMargin,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    onDismiss: (() -> Unit)? = null,
    dismissIcon: (@Composable () -> Unit)? = null,
    dismissContentDescription: String = ChipDefaults.DISMISS_CONTENT_DESCRIPTION,
    dismissButtonSize: Dp = ChipDefaults.DismissButtonSize,
    count: String? = null,
    showRedDot: Boolean = false,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val dismissInteractionSource = remember { MutableInteractionSource() }

    // COUIChipDrawable.TintAnimation: the checked/unchecked background, label and icon colors are
    // evaluated by an ArgbEvaluator driven by COUISpringForce(response 0.3, bounce 0).
    val containerColor by animateColorAsState(
        targetValue = colors.containerColor(enabled, selected),
        animationSpec = CheckTintColorSpring,
        label = "chipContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = colors.contentColor(enabled, selected),
        animationSpec = CheckTintColorSpring,
        label = "chipContentColor",
    )

    // COUI press feedback, identical to COUIButton: COUIPressFeedbackHelper scales the chip
    // towards a size dependent end ratio and COUIMaskEffectDrawable tints the fill with
    // couiColorPress, both on a critically damped response-0.3 spring.
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleProgress by animateFloatAsState(
        targetValue = if (enabled && isPressed) 1f else 0f,
        animationSpec = ChipPressFeedbackSpring,
        label = "chipPressScale",
    )
    val tintProgress = remember { Animatable(0f) }
    LaunchedEffect(enabled, isPressed) {
        if (enabled && isPressed) {
            tintProgress.animateTo(1f, ChipPressFeedbackSpring)
        } else if (!enabled) {
            // COUIMaskEffectDrawable resets the mask without animation when disabled.
            tintProgress.snapTo(0f)
        } else if (tintProgress.value > 0f) {
            if (tintProgress.value < ChipPressedTintMinVisibleProgress) {
                // COUIMaskEffectDrawable.animateToProgressUntil: keep the press-in animation
                // running until the tint is clearly visible, then reverse from there.
                try {
                    tintProgress.animateTo(1f, ChipPressFeedbackSpring) {
                        if (value >= ChipPressedTintMinVisibleProgress) throw ChipPressTintThresholdReached()
                    }
                } catch (_: ChipPressTintThresholdReached) {
                    // Minimum visible progress reached; fall through to the fade-out.
                }
            }
            tintProgress.animateTo(0f, ChipPressFeedbackSpring)
        }
    }
    // couiColorPress: #1F000000 in light themes, #33FFFFFF in dark themes.
    val pressTint = if (COUITheme.colorScheme.background.luminance() < 0.5f) ChipPressedTintDark else ChipPressedTintLight
    val fillColor = if (tintProgress.value > 0f) {
        pressTint.copy(alpha = pressTint.alpha * tintProgress.value).compositeOver(containerColor)
    } else {
        containerColor
    }
    // COUIChipDrawable.CloseIconAnimation: the close icon and the width it occupies grow from 0 to 1
    // on a COUISpringForce(response 0.3, bounce 0); the icon itself is scaled between
    // CLOSE_ICON_SCALE_MIN_VALUE (0.3) and 1 while it appears.
    val dismissProgress = remember { Animatable(if (onDismiss != null) 1f else 0f) }
    LaunchedEffect(onDismiss != null) {
        dismissProgress.animateTo(if (onDismiss != null) 1f else 0f, ChipCloseIconSpring)
    }
    val dismissFraction = dismissProgress.value
    val dismissVisualScale = CHIP_CLOSE_ICON_SCALE_MIN +
        (1f - CHIP_CLOSE_ICON_SCALE_MIN) * dismissFraction
    // COUIChipDrawable.clipSmoothRoundRect: the default chipCornerRadius (-1) resolves to half the
    // background height, so the default token follows the measured size and draws the system
    // capsule curve. An explicit other radius keeps the Lp squircle. See CapsuleOutlineShape.
    val squircleEnabled = isSquircleEnabled()
    val defaultRadius = COUITheme.dimens.chip.cornerRadius
    val shape = remember(cornerRadius, defaultRadius, squircleEnabled) {
        val resolvedRadius = cornerRadius.takeUnless { it == defaultRadius }
        CapsuleOutlineShape(cornerRadius = resolvedRadius, squircleEnabled = squircleEnabled)
    }
    val fillColorState = rememberUpdatedState(fillColor)
    val fillModifier = remember(shape, fillColorState) {
        Modifier.shapeFill(shape = shape) { fillColorState.value }
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    val scale = 1f - (1f - chipPressedScaleEndRatio(size)) * scaleProgress
                    scaleX = scale
                    scaleY = scale
                }
                .widthIn(max = maxWidth)
                .then(fillModifier)
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = indication,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onClick = onClick,
                )
                .semantics {
                    // COUIChip.onInitializeAccessibilityNodeInfo concatenates the label, the
                    // coui_chip_count_separator and the count text into the node's text.
                    if (count != null) {
                        contentDescription = label + ChipDefaults.COUNT_SEPARATOR + count
                    }
                },
            propagateMinConstraints = true,
        ) {
            val layoutDirection = LocalLayoutDirection.current
            Row(
                modifier = Modifier
                    .defaultMinSize(minWidth = minWidth, minHeight = minHeight)
                    .padding(
                        start = insideMargin.calculateStartPadding(layoutDirection),
                        top = insideMargin.calculateTopPadding(),
                        // COUIChipDrawable: a close icon takes the place of chipEndPadding, and a red
                        // dot widens the chip by mRedDotOffsetHorizontal + dot diameter.
                        end = if (onDismiss != null) {
                            0.dp
                        } else {
                            insideMargin.calculateEndPadding(layoutDirection) +
                                if (showRedDot) ChipDefaults.RedDotReservedWidth else 0.dp
                        },
                        bottom = insideMargin.calculateBottomPadding(),
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier.size(ChipDefaults.IconSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        icon()
                    }
                    Spacer(Modifier.width(ChipDefaults.IconSpacing))
                }
                Text(
                    text = label,
                    color = contentColor,
                    style = ChipDefaults.textStyle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = ChipDefaults.TextMaxWidth),
                )
                if (count != null) {
                    // COUIChipDrawable.drawCount: the count text follows the label after
                    // mCountMarginStart, in COUNT_TEXT_SIZE (12sp) or countTextAppearance.
                    Spacer(Modifier.width(ChipDefaults.CountMarginStart))
                    Text(
                        text = count,
                        color = contentColor,
                        style = ChipDefaults.countTextStyle(),
                        maxLines = 1,
                    )
                }
                if (dismissFraction > 0f) {
                    // COUIChipDrawable.calculateCloseIconWidth: closeIconStartPadding + closeIconSize +
                    // closeIconEndPadding, with the icon closeIconEndPadding from the chip's end edge.
                    // The whole block is scaled by the close icon animation progress, so the chip
                    // widens as the icon appears.
                    Box(
                        modifier = Modifier
                            .padding(
                                start = ChipDefaults.DismissIconStartPadding * dismissFraction,
                                end = ChipDefaults.DismissIconEndPadding * dismissFraction,
                            )
                            .size(ChipDefaults.DismissIconSize)
                            .graphicsLayer {
                                scaleX = dismissVisualScale
                                scaleY = dismissVisualScale
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        // calculateCloseIconTouchBounds: a closeIconTouchBoundsSize square centred on
                        // the icon. requiredSize centres it without growing the chip.
                        Box(
                            modifier = Modifier
                                .requiredSize(dismissButtonSize)
                                .clickable(
                                    interactionSource = dismissInteractionSource,
                                    indication = null,
                                    enabled = enabled && dismissFraction >= 1f,
                                    role = Role.Button,
                                    // COUIChipDrawable keeps the close icon drawn while the hide
                                    // animation runs, but it is only clickable while visible.
                                    onClick = { onDismiss?.invoke() },
                                )
                                .semantics { contentDescription = dismissContentDescription },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (dismissIcon != null) {
                                dismissIcon()
                            } else {
                                Icon(
                                    imageVector = COUIIcons.Basic.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(ChipDefaults.DismissIconSize),
                                )
                            }
                        }
                    }
                }
            }
            if (showRedDot) {
                // COUIChipDrawable.drawRedDot: a COUIHintRedDot.Small dot pinned to the top-end corner
                // of the label, mRedDotOffsetHorizontal from it and mRedDotOffsetVertical from the top.
                val dotEndInset = if (onDismiss != null) {
                    ChipDefaults.DismissIconStartPadding + ChipDefaults.DismissIconSize +
                        ChipDefaults.DismissIconEndPadding
                } else {
                    insideMargin.calculateEndPadding(layoutDirection)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            top = ChipDefaults.RedDotOffsetVertical,
                            end = (dotEndInset - ChipDefaults.RedDotReservedWidth).coerceAtLeast(0.dp),
                        )
                        .size(ChipDefaults.RedDotSize)
                        .background(color = ChipDefaults.redDotColor(), shape = CircleShape),
                )
            }
        }
    }
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds count and showRedDot parameters.",
    level = DeprecationLevel.HIDDEN,
)
@OptIn(ExperimentalCOUIApi::class)
@Composable
fun Chip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    cornerRadius: Dp = COUITheme.dimens.chip.cornerRadius,
    minWidth: Dp = COUITheme.dimens.chip.minWidth,
    minHeight: Dp = COUITheme.dimens.chip.minHeight,
    maxWidth: Dp = COUITheme.dimens.chip.maxWidth,
    colors: ChipColors = ChipDefaults.chipColors(),
    insideMargin: PaddingValues = COUITheme.dimens.chip.insideMargin,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    onDismiss: (() -> Unit)? = null,
    dismissIcon: (@Composable () -> Unit)? = null,
    dismissContentDescription: String = ChipDefaults.DISMISS_CONTENT_DESCRIPTION,
    dismissButtonSize: Dp = ChipDefaults.DismissButtonSize,
): Unit = Chip(
    selected = selected,
    onClick = onClick,
    label = label,
    modifier = modifier,
    enabled = enabled,
    icon = icon,
    cornerRadius = cornerRadius,
    minWidth = minWidth,
    minHeight = minHeight,
    maxWidth = maxWidth,
    colors = colors,
    insideMargin = insideMargin,
    interactionSource = interactionSource,
    indication = indication,
    onDismiss = onDismiss,
    dismissIcon = dismissIcon,
    dismissContentDescription = dismissContentDescription,
    dismissButtonSize = dismissButtonSize,
)

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds dismiss and sizing parameters.",
    level = DeprecationLevel.HIDDEN,
)
@OptIn(ExperimentalCOUIApi::class)
@Composable
fun Chip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    cornerRadius: Dp = COUITheme.dimens.chip.cornerRadius,
    minWidth: Dp = COUITheme.dimens.chip.minWidth,
    minHeight: Dp = COUITheme.dimens.chip.minHeight,
    maxWidth: Dp = COUITheme.dimens.chip.maxWidth,
    colors: ChipColors = ChipDefaults.chipColors(),
    insideMargin: PaddingValues = COUITheme.dimens.chip.insideMargin,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
) {
    Chip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        cornerRadius = cornerRadius,
        minWidth = minWidth,
        minHeight = minHeight,
        maxWidth = maxWidth,
        colors = colors,
        insideMargin = insideMargin,
        interactionSource = interactionSource,
        indication = indication,
    )
}

/** Contains default values used by [Chip]. */
object ChipDefaults {
    /** The minimum width of a chip (COUI coui_chip_default_min_width). */
    val MinWidth = 52.dp

    /** The minimum height of a chip (COUI coui_chip_selection_style_height). */
    val MinHeight = 32.dp

    /** The maximum width of a chip (COUI coui_chip_default_max_width). */
    val MaxWidth = 300.dp

    /** The maximum width of the label (COUI coui_chip_default_max_text_width / chipTextMaxWidth). */
    val TextMaxWidth = 200.dp

    /**
     * The corner radius of a chip. COUIChipDrawable resolves the default chipCornerRadius (-1) to
     * half of the background height, i.e. a capsule for the default 32dp height.
     */
    val CornerRadius = 16.dp

    /**
     * The size of the built-in dismiss affordance icon. COUI `coui_chip_input_style_close_icon_size`.
     *
     * New in 1.2.0.
     */
    @ExperimentalCOUIApi
    val DismissIconSize = 14.dp

    /**
     * The gap between the label and the dismiss icon. COUI
     * `coui_chip_input_style_close_icon_start_padding`.
     */
    @ExperimentalCOUIApi
    val DismissIconStartPadding = 8.dp

    /**
     * The gap between the dismiss icon and the chip's end edge; it replaces the end inside margin.
     * COUI `coui_chip_input_style_close_icon_end_padding`.
     */
    @ExperimentalCOUIApi
    val DismissIconEndPadding = 9.dp

    /**
     * The touch target of the dismiss affordance, a square centred on the icon that does not change
     * the chip's size. COUI `coui_chip_input_style_close_icon_touch_bounds_size`.
     *
     * New in 1.2.0.
     */
    @ExperimentalCOUIApi
    val DismissButtonSize = 28.dp

    /**
     * The content description of the built-in dismiss affordance, so the close button is reachable
     * from accessibility services and UI tests.
     *
     * New in 1.2.0.
     */
    @ExperimentalCOUIApi
    const val DISMISS_CONTENT_DESCRIPTION = "Dismiss"

    /**
     * The gap between the label and the count text (COUIChipDrawable `mCountMarginStart`, which
     * defaults to 2dp and is overridden by the chipCountStartMargin attribute).
     *
     * New in 1.2.0.
     */
    @ExperimentalCOUIApi
    val CountMarginStart = 2.dp

    /**
     * The separator spoken between the label and the count (COUI `coui_chip_count_separator`; the
     * decompiled build ships no strings resources, so this mirrors the concatenation order).
     *
     * New in 1.2.0.
     */
    @ExperimentalCOUIApi
    const val COUNT_SEPARATOR = ", "

    /**
     * The font size of the count text when no countTextAppearance is set
     * (COUIChipDrawable `COUNT_TEXT_SIZE` = 12sp).
     *
     * New in 1.2.0.
     */
    @ExperimentalCOUIApi
    val CountTextSize: TextUnit = 12.sp

    /**
     * The diameter of the hint red dot (COUI `Widget.COUI.COUIHintRedDot.Small`
     * couiDotDiameter = `coui_dot_diameter_small` 6dp).
     *
     * New in 1.2.0.
     */
    @ExperimentalCOUIApi
    val RedDotSize = 6.dp

    /**
     * The horizontal offset between the label and the red dot (COUI
     * `coui_chip_red_dot_offset_horizontal`).
     *
     * New in 1.2.0.
     */
    @ExperimentalCOUIApi
    val RedDotOffsetHorizontal = 2.dp

    /**
     * The vertical offset of the red dot from the chip's top edge (COUI
     * `coui_chip_red_dot_offset_vertical`).
     *
     * New in 1.2.0.
     */
    @ExperimentalCOUIApi
    val RedDotOffsetVertical = 6.dp

    /**
     * The width a red dot adds to the chip: the horizontal offset plus the dot diameter
     * (COUIChipDrawable.calculateRedDotWidth).
     *
     * New in 1.2.0.
     */
    @ExperimentalCOUIApi
    val RedDotReservedWidth: Dp = RedDotOffsetHorizontal + RedDotSize

    /**
     * The color of the hint red dot (COUI `coui_color_additional_red`: #EB3B2F light,
     * #EB493D dark).
     *
     * New in 1.2.0.
     */
    @Composable
    @ExperimentalCOUIApi
    fun redDotColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
        ChipRedDotDark
    } else {
        ChipRedDotLight
    }

    /**
     * The default text style of the count text: the label style at [CountTextSize]
     * (COUIChipDrawable falls back to 12sp when countTextAppearance is not set).
     *
     * New in 1.2.0.
     */
    @Composable
    @ExperimentalCOUIApi
    fun countTextStyle(): TextStyle = textStyle().copy(fontSize = CountTextSize)

    /** The size of the leading icon (COUI coui_chip_selection_style_chip_icon_size). */
    val IconSize = 16.dp

    /** The spacing between the icon and the label (COUI coui_chip_selection_style_chip_icon_end_padding). */
    val IconSpacing = 4.dp

    /**
     * The horizontal padding inside a chip (COUI coui_chip_selection_style_chip_horizontal_padding;
     * the icon start padding shares the same 12dp value).
     */
    val InsideMargin = PaddingValues(horizontal = 12.dp)

    /** The default text style of the label (COUI Widget.COUI.Chip.Selection textAppearance couiTextBodyM: 14sp). */
    @Composable
    fun textStyle(): TextStyle = COUITheme.textStyles.body2

    /**
     * The default [ChipColors], mapping the Widget.COUI.Chip color attributes:
     * uncheckedBackgroundColor/uncheckedDisabledBackgroundColor = couiColorContainer8,
     * checkedBackgroundColor = couiColorPrimary, checkedDisabledBackgroundColor = couiColorDisable,
     * uncheckedTextColor = couiColorLabelPrimary, checkedTextColor = couiColorLabelOnColor and the
     * chip_checked/unchecked_text_disable_color selectors.
     */
    @Composable
    fun chipColors(
        containerColor: Color = COUITheme.colorScheme.secondaryVariant,
        selectedContainerColor: Color = COUITheme.colorScheme.primary,
        disabledContainerColor: Color = COUITheme.colorScheme.disabledSecondaryVariant,
        selectedDisabledContainerColor: Color = COUITheme.colorScheme.disabledPrimaryButton,
        contentColor: Color = COUITheme.colorScheme.onSurface,
        selectedContentColor: Color = COUITheme.colorScheme.onPrimary,
        disabledContentColor: Color = chipDisabledContentColor(),
        selectedDisabledContentColor: Color = COUITheme.colorScheme.disabledOnPrimaryButton,
    ): ChipColors = remember(
        containerColor,
        selectedContainerColor,
        disabledContainerColor,
        selectedDisabledContainerColor,
        contentColor,
        selectedContentColor,
        disabledContentColor,
        selectedDisabledContentColor,
    ) {
        ChipColors(
            containerColor = containerColor,
            selectedContainerColor = selectedContainerColor,
            disabledContainerColor = disabledContainerColor,
            selectedDisabledContainerColor = selectedDisabledContainerColor,
            contentColor = contentColor,
            selectedContentColor = selectedContentColor,
            disabledContentColor = disabledContentColor,
            selectedDisabledContentColor = selectedDisabledContentColor,
        )
    }
}

/**
 * Colors used by a [Chip].
 *
 * @param containerColor The capsule color when unselected.
 * @param selectedContainerColor The capsule color when selected.
 * @param disabledContainerColor The capsule color when unselected and disabled.
 * @param selectedDisabledContainerColor The capsule color when selected and disabled.
 * @param contentColor The label and icon color when unselected.
 * @param selectedContentColor The label and icon color when selected.
 * @param disabledContentColor The label and icon color when unselected and disabled.
 * @param selectedDisabledContentColor The label and icon color when selected and disabled.
 */
@Immutable
data class ChipColors(
    val containerColor: Color,
    val selectedContainerColor: Color,
    val disabledContainerColor: Color,
    val selectedDisabledContainerColor: Color,
    val contentColor: Color,
    val selectedContentColor: Color,
    val disabledContentColor: Color,
    val selectedDisabledContentColor: Color,
) {
    @Stable
    internal fun containerColor(enabled: Boolean, selected: Boolean): Color = when {
        enabled && selected -> selectedContainerColor
        enabled -> containerColor
        selected -> selectedDisabledContainerColor
        else -> disabledContainerColor
    }

    @Stable
    internal fun contentColor(enabled: Boolean, selected: Boolean): Color = when {
        enabled && selected -> selectedContentColor
        enabled -> contentColor
        selected -> selectedDisabledContentColor
        else -> disabledContentColor
    }
}

/**
 * The COUI check tint spring for colors: COUIChipDrawable.TintAnimation uses
 * COUISpringForce(response = 0.3f, bounce = 0f), i.e. a critically damped spring with stiffness
 * (2 * PI / 0.3)^2 ~= 438.65.
 */
private val CheckTintColorSpring = spring<Color>(dampingRatio = 1f, stiffness = 438.65f)

/**
 * The COUI press feedback spring: COUISpringForce(response = 0.3f, bounce = 0f). Shared by the
 * press scale (COUIPressFeedbackHelper) and the press tint (COUIMaskEffectDrawable) animations.
 */
private val ChipPressFeedbackSpring = spring<Float>(dampingRatio = 1f, stiffness = 438.65f)

/** couiColorPress in light themes (coui_color_press #1F000000). */
private val ChipPressedTintLight = Color(0x1F000000)

/** couiColorPress in dark themes (coui_color_press_dark #33FFFFFF). */
private val ChipPressedTintDark = Color(0x33FFFFFF)

/** `chip_unchecked_text_disable_color` in light themes (#42000000). */
private val ChipUncheckedDisabledTextLight = Color(0x42000000)

/** `chip_unchecked_text_disable_color` in dark themes (#29FFFFFF). */
private val ChipUncheckedDisabledTextDark = Color(0x29FFFFFF)

/** COUI `coui_color_additional_red` in light themes (#EB3B2F). */
private val ChipRedDotLight = Color(0xFFEB3B2F)

/** COUI `coui_color_additional_red` in dark themes (#EB493D). */
private val ChipRedDotDark = Color(0xFFEB493D)

/** The smallest scale the close icon shrinks to while appearing (COUIChipDrawable CLOSE_ICON_SCALE_MIN_VALUE). */
private val CHIP_CLOSE_ICON_SCALE_MIN = 0.3f

/** COUIChipDrawable.CloseIconAnimation spring: COUISpringForce(response 0.3f, bounce 0f). */
private val ChipCloseIconSpring = spring<Float>(dampingRatio = 1f, stiffness = 438.65f)

/**
 * The text color of a disabled unselected chip: COUI `chip_unchecked_text_disable_color`
 * (#42000000 light / #29FFFFFF dark), which is not the same as the shared disabled secondary color.
 */
@Composable
private fun chipDisabledContentColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
    ChipUncheckedDisabledTextDark
} else {
    ChipUncheckedDisabledTextLight
}

/**
 * A release before the press tint reaches this progress defers the fade-out until it does, so
 * quick taps still flash visibly (COUIMaskEffectDrawable DEFAULT_MIN_PROGRESS_FOR_TOUCH_ENTER_ANIMATION).
 */
private val ChipPressedTintMinVisibleProgress = 0.7f

/** Thrown to stop the deferred press-in tint animation once [ChipPressedTintMinVisibleProgress] is reached. */
private class ChipPressTintThresholdReached : CancellationException("Press tint reached the minimum visible progress")

/** COUI `COUIInEaseInterpolator`; shares [COUIInEase]. Maps the chip area to the pressed end scale. */
private val ChipPressedScaleEasing = COUIInEase

/** COUIPressFeedbackHelper area bounds: coui_min_end_value_size (48dp) squared. */
private val ChipPressedScaleMinAreaDp2 = 48f * 48f

/** COUIPressFeedbackHelper area bounds: coui_max_end_value_width x height (328dp x 220dp). */
private val ChipPressedScaleMaxAreaDp2 = 328f * 220f

/** The pressed end scale of the smallest surfaces (COUIPressFeedbackHelper MIN_SCALE_END_RATIO). */
private val ChipPressedScaleMin = 0.92f

/** The pressed end scale of the largest surfaces (COUIPressFeedbackHelper MAX_SCALE_END_RATIO). */
private val ChipPressedScaleMax = 0.98f

/**
 * The scale a surface of [size] shrinks to when fully pressed: 0.92 for areas up to 48 x 48dp,
 * easing towards [ChipPressedScaleMax] at 328 x 220dp (COUIPressFeedbackHelper.getCardScaleRatio).
 */
private fun Density.chipPressedScaleEndRatio(size: Size): Float {
    val area = size.width.toDp().value * size.height.toDp().value
    return when {
        area <= ChipPressedScaleMinAreaDp2 -> ChipPressedScaleMin

        area >= ChipPressedScaleMaxAreaDp2 -> ChipPressedScaleMax

        else -> ChipPressedScaleMin + (ChipPressedScaleMax - ChipPressedScaleMin) *
            ChipPressedScaleEasing.transform((area - ChipPressedScaleMinAreaDp2) / (ChipPressedScaleMaxAreaDp2 - ChipPressedScaleMinAreaDp2))
    }
}
