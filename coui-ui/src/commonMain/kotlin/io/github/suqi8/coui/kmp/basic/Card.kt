// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.interfaces.HoldDownObserver
import io.github.suqi8.coui.kmp.interfaces.collectIsHeldDownAsState
import io.github.suqi8.coui.kmp.squircle.SquircleDefaults
import io.github.suqi8.coui.kmp.squircle.squircleSurface
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalContentColor
import io.github.suqi8.coui.kmp.utils.PressFeedbackType
import io.github.suqi8.coui.kmp.utils.SinkFeedback
import io.github.suqi8.coui.kmp.utils.TiltFeedback
import io.github.suqi8.coui.kmp.utils.pressable
import kotlin.coroutines.cancellation.CancellationException

/**
 * A [Card] component with COUI style.
 * Card contain content and actions that relate information about a subject.
 *
 * This [Card] does not handle input events
 *
 * @param modifier The modifier to be applied to the [Card].
 * @param cornerRadius The corner radius of the [Card].
 * @param insideMargin The margin inside the [Card].
 * @param colors [CardColors] that will be used to resolve the color(s) used for the [Card].
 * @param weight The Lp exponent of the corner curve. Defaults to [CardDefaults.Weight], the
 *   standalone card silhouette. Pass [CardDefaults.GroupWeight] with
 *   [CardDefaults.GroupCornerRadius] for the card that holds a preference group.
 * @param content The [Composable] content of the [Card].
 */
@Composable
@NonRestartableComposable
fun Card(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = COUITheme.dimens.card.cornerRadius,
    insideMargin: PaddingValues = COUITheme.dimens.card.insideMargin,
    colors: CardColors = CardDefaults.defaultColors(),
    weight: Float = CardDefaults.Weight,
    content: @Composable ColumnScope.() -> Unit,
) {
    BasicCard(
        color = colors.color,
        contentColor = colors.contentColor,
        modifier = modifier,
        cornerRadius = cornerRadius,
        weight = weight,
    ) {
        Column(
            modifier = Modifier.padding(insideMargin),
            content = content,
        )
    }
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds the weight parameter.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
@NonRestartableComposable
fun Card(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = COUITheme.dimens.card.cornerRadius,
    insideMargin: PaddingValues = COUITheme.dimens.card.insideMargin,
    colors: CardColors = CardDefaults.defaultColors(),
    content: @Composable ColumnScope.() -> Unit,
): Unit = Card(
    modifier = modifier,
    cornerRadius = cornerRadius,
    insideMargin = insideMargin,
    colors = colors,
    weight = CardDefaults.Weight,
    content = content,
)

/**
 * A [Card] component with COUI style.
 * Card contain contain content and actions that relate information about a subject.
 *
 * This [Card] handles input events
 *
 * @param modifier The modifier to be applied to the [Card].
 * @param cornerRadius The corner radius of the [Card].
 * @param insideMargin The margin inside the [Card].
 * @param colors [CardColors] that will be used to resolve the color(s) used for the [Card].
 * @param pressFeedbackType The press feedback type of the [Card]. Defaults to
 *   [PressFeedbackType.Tint], which tints the card with [CardColors.pressedColor].
 * @param showIndication Whether to show indication of the [Card].
 * @param holdDownState Whether the [Card] is in a hold-down state.
 * @param selected Whether the [Card] is in a locked selected state: the press tint is held at its
 *   end value, mirroring COUICardListSelectedItemLayout.setIsSelected(selected, animate), which
 *   locks the press state effect of the card background. New in 1.2.0.
 * @param onClick The callback to be invoked when the [Card] is clicked.
 * @param onLongPress The callback to be invoked when the [Card] is long pressed.
 * @param weight The Lp exponent of the corner curve; see the non-clickable [Card].
 * @param content The [Composable] content of the [Card].
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = COUITheme.dimens.card.cornerRadius,
    insideMargin: PaddingValues = COUITheme.dimens.card.insideMargin,
    colors: CardColors = CardDefaults.defaultColors(),
    pressFeedbackType: PressFeedbackType = PressFeedbackType.Tint,
    showIndication: Boolean = false,
    holdDownState: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    weight: Float = CardDefaults.Weight,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongPress by rememberUpdatedState(onLongPress)

    HoldDownObserver(holdDownState, interactionSource)

    val pressFeedback = remember(pressFeedbackType) {
        when (pressFeedbackType) {
            PressFeedbackType.None, PressFeedbackType.Tint -> null
            PressFeedbackType.Sink -> SinkFeedback()
            PressFeedbackType.Tilt -> TiltFeedback()
        }
    }

    // COUI card press feedback (COUICardListSelectedItemLayout): the background drawable is a
    // COUIStateEffectDrawable whose mask layer animates towards couiColorPress. hover, focus and
    // press are three independent StateEffectAnimators, and setIsSelected locks the press state so
    // a selected row keeps the tint (COUIMaskEffectDrawable.setStateLocked).
    val tintEnabled = pressFeedbackType == PressFeedbackType.Tint
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHeldDown by interactionSource.collectIsHeldDownAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val tintProgress = remember { Animatable(0f) }
    LaunchedEffect(tintEnabled, isPressed, isHeldDown, selected) {
        if (!tintEnabled) {
            tintProgress.snapTo(0f)
        } else if (isPressed || isHeldDown || selected) {
            tintProgress.animateTo(1f, CardPressFeedbackSpring)
        } else if (tintProgress.value > 0f) {
            if (tintProgress.value < PressedTintMinVisibleProgress) {
                // Keep the press-in animation running until the tint is clearly visible.
                try {
                    tintProgress.animateTo(1f, CardPressFeedbackSpring) {
                        if (value >= PressedTintMinVisibleProgress) throw CardPressTintThresholdReached()
                    }
                } catch (_: CardPressTintThresholdReached) {
                    // Minimum visible progress reached; fall through to the fade-out.
                }
            }
            tintProgress.animateTo(0f, CardPressFeedbackSpring)
        }
    }
    val hoverProgress by animateFloatAsState(
        targetValue = if (isHovered) 1f else 0f,
        animationSpec = CardPressFeedbackSpring,
        label = "cardHover",
    )
    val focusProgress by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = CardPressFeedbackSpring,
        label = "cardFocus",
    )
    // COUIMaskEffectDrawable draws the hover mask, then the focus mask, then the press mask, all
    // over the card background and inside the same squircle, so the press tint is stacked as a
    // translucent layer instead of replacing the fill.
    val fillColor = colors.color
        .maskedBy(cardHoverTint(), hoverProgress)
        .maskedBy(cardFocusTint(), focusProgress)
        .maskedBy(colors.pressedColor, tintProgress.value)

    val usedInteractionSource = if (pressFeedback != null) interactionSource else null
    val indicationToUse = if (showIndication) LocalIndication.current else null

    val hasOnClick = onClick != null
    val hasLongPress = onLongPress != null
    val isClickable = hasOnClick || hasLongPress
    val clickableModifier = remember(isClickable, hasLongPress, interactionSource, indicationToUse) {
        if (isClickable) {
            Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = indicationToUse,
                onClick = { currentOnClick?.invoke() },
                onLongClick = if (hasLongPress) {
                    { currentOnLongPress?.invoke() }
                } else {
                    null
                },
            )
        } else {
            Modifier
        }
    }

    BasicCard(
        color = fillColor,
        contentColor = colors.contentColor,
        modifier = modifier.pressable(
            interactionSource = usedInteractionSource,
            indication = pressFeedback,
            delay = null,
        ),
        cornerRadius = cornerRadius,
        weight = weight,
    ) {
        Column(
            modifier = Modifier
                .then(clickableModifier)
                .padding(insideMargin),
            content = content,
        )
    }
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds the selected parameter.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun Card(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = COUITheme.dimens.card.cornerRadius,
    insideMargin: PaddingValues = COUITheme.dimens.card.insideMargin,
    colors: CardColors = CardDefaults.defaultColors(),
    pressFeedbackType: PressFeedbackType = PressFeedbackType.Tint,
    showIndication: Boolean = false,
    holdDownState: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    weight: Float = CardDefaults.Weight,
    content: @Composable ColumnScope.() -> Unit,
): Unit = Card(
    modifier = modifier,
    cornerRadius = cornerRadius,
    insideMargin = insideMargin,
    colors = colors,
    pressFeedbackType = pressFeedbackType,
    showIndication = showIndication,
    holdDownState = holdDownState,
    selected = false,
    onClick = onClick,
    onLongPress = onLongPress,
    weight = weight,
    content = content,
)

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds the weight parameter.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun Card(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = COUITheme.dimens.card.cornerRadius,
    insideMargin: PaddingValues = COUITheme.dimens.card.insideMargin,
    colors: CardColors = CardDefaults.defaultColors(),
    pressFeedbackType: PressFeedbackType = PressFeedbackType.Tint,
    showIndication: Boolean = false,
    holdDownState: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
): Unit = Card(
    modifier = modifier,
    cornerRadius = cornerRadius,
    insideMargin = insideMargin,
    colors = colors,
    pressFeedbackType = pressFeedbackType,
    showIndication = showIndication,
    holdDownState = holdDownState,
    selected = false,
    onClick = onClick,
    onLongPress = onLongPress,
    weight = CardDefaults.Weight,
    content = content,
)

/**
 * A [BasicCard] component.
 *
 * @param color The background color of the [BasicCard].
 * @param contentColor The content color of the [BasicCard].
 * @param modifier The modifier to be applied to the [BasicCard].
 * @param cornerRadius The corner radius of the [BasicCard].
 * @param weight The Lp exponent of the corner curve.
 * @param content The [Composable] content of the [BasicCard].
 */
@Composable
private fun BasicCard(
    color: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CardDefaults.CornerRadius,
    weight: Float = CardDefaults.Weight,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
    ) {
        Box(
            modifier = modifier
                .semantics(mergeDescendants = false) {
                    isTraversalGroup = true
                }
                .squircleSurface(
                    color = color,
                    cornerRadius = cornerRadius,
                    weight = weight,
                ),
            propagateMinConstraints = true,
        ) {
            content()
        }
    }
}

/**
 * The position of a row inside a card group, mirroring COUI's `COUICardListHelper` constants
 * (`NONE = 0`, `HEAD = 1`, `MIDDLE = 2`, `TAIL = 3`, `FULL = 4`).
 *
 * COUI's `COUICardListSelectedItemLayout.setPadding` uses the position to decide which of the card's
 * outer edges get an extra [CardListPosition.HeadOrTailPadding] of vertical padding, and grows the
 * row's minimum height by the same amount. Only the edges that are visually rounded receive it, so a
 * standalone row (both edges rounded) gains the padding twice while a middle row gains none.
 *
 * @see cardListPositionOf
 */
enum class CardListPosition {
    /** Not part of a card group; no rounded edges and no extra padding. */
    None,

    /** First row of a multi-row group; the top edge is rounded. */
    Head,

    /** A row with a sibling on both sides; neither edge is rounded. */
    Middle,

    /** Last row of a multi-row group; the bottom edge is rounded. */
    Tail,

    /** The only row of the group; both edges are rounded. */
    Full,
    ;

    /** The extra top padding contributed by this position. */
    internal val extraPaddingTop: Dp
        get() = if (this == Head || this == Full) HeadOrTailPadding else 0.dp

    /** The extra bottom padding contributed by this position. */
    internal val extraPaddingBottom: Dp
        get() = if (this == Tail || this == Full) HeadOrTailPadding else 0.dp

    companion object {
        /**
         * The padding COUI adds to each rounded outer edge of a card group
         * (COUI `coui_list_card_head_or_tail_padding`).
         */
        val HeadOrTailPadding = 2.dp
    }
}

/**
 * Resolves the [CardListPosition] of the row at [index] within a card group of [count] rows,
 * mirroring COUI's `COUICardListHelper.getPositionInGroup(int, int)`.
 *
 * @param index The zero-based index of the row within the group.
 * @param count The total number of rows in the group.
 */
@Stable
fun cardListPositionOf(index: Int, count: Int): CardListPosition = when {
    count <= 0 || index !in 0 until count -> CardListPosition.None
    count == 1 -> CardListPosition.Full
    index == 0 -> CardListPosition.Head
    index == count - 1 -> CardListPosition.Tail
    else -> CardListPosition.Middle
}

object CardDefaults {

    /**
     * The default corner radius of the [Card].
     *
     * ColorOS 17 keeps the COUI `couiRoundCornerM` / `coui_round_corner_m` token at 12dp but remaps
     * it at draw time through `RoundCornerUtil.getRoundCornerForOS17` to
     * `coui_round_corner_m_radius_large_16_1` = 17dp.
     *
     * The native card outline (`RoundRectDrawable.getOutline`) draws that radius with smooth weight
     * 3.0, which is why the standalone card surface is painted at [SquircleDefaults.WeightLarge].
     *
     * A card that holds a preference group is not this shape: see [GroupCornerRadius].
     */
    val CornerRadius = 17.dp

    /**
     * The corner weight of a standalone card (`COUICardView`), i.e. [CornerRadius] drawn at Lp 3.0.
     */
    val Weight = SquircleDefaults.WeightLarge

    /**
     * The corner radius of the card that holds a preference group.
     *
     * `COUICardListSelectedItemLayout` defaults `couiCardRadius` to `?attr/couiRoundCornerXL` →
     * `coui_round_corner_xl` = 20dp and draws it through `addSmoothRoundRect(..., 2.5f)`, i.e. one
     * step softer than the standalone card. Use it together with [GroupWeight].
     */
    val GroupCornerRadius = 20.dp

    /**
     * The corner weight of the card that holds a preference group: `COUICardListSelectedItemLayout`
     * bakes `addSmoothRoundRect` at Lp 2.5.
     */
    val GroupWeight = SquircleDefaults.WeightNormal

    /**
     * The default margin inside the [Card].
     */
    val InsideMargin = PaddingValues(0.dp)

    /**
     * The default colors width of the [Card].
     */
    @Composable
    fun defaultColors(
        color: Color = COUITheme.colorScheme.surfaceContainer,
        contentColor: Color = COUITheme.colorScheme.onSurfaceContainer,
        pressedColor: Color = COUITheme.colorScheme.surfaceContainerHigh,
    ): CardColors = remember(color, contentColor, pressedColor) {
        CardColors(
            color = color,
            contentColor = contentColor,
            pressedColor = pressedColor,
        )
    }
}

/**
 * Colors used by a [Card].
 *
 * @param color The background color of the card.
 * @param contentColor The content color of the card.
 * @param pressedColor The mask stacked over the card while pressed, COUI `couiColorCardPressed`
 *   (`#E6E6E6` / `#33FFFFFF`).
 */
@Immutable
data class CardColors(
    val color: Color,
    val contentColor: Color,
    val pressedColor: Color,
)

/** The COUI card press feedback spring (COUISpringForce response 0.3f / bounce 0f). */
private val CardPressFeedbackSpring = spring<Float>(dampingRatio = 1f, stiffness = 438.65f)

/** couiColorHover in light themes (coui_color_hover #14000000). */
private val CardHoverTintLight = Color(0x14000000)

/** couiColorHover in dark themes (coui_color_hover_dark #26FFFFFF). */
private val CardHoverTintDark = Color(0x26FFFFFF)

/** The hover mask color, `couiColorHover`. */
@Composable
private fun cardHoverTint(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) CardHoverTintDark else CardHoverTintLight

/**
 * The focus mask color, COUI `couiColorFocus` (`coui_color_focus_blue`, `#1F0080FF` / `#331A8CFF`),
 * which the theme derives from its accent.
 */
@Composable
private fun cardFocusTint(): Color = COUITheme.colorScheme.focus

/** Draws [mask] over this color at [progress], as COUIMaskEffectDrawable stacks its mask layers. */
internal fun Color.maskedBy(mask: Color, progress: Float): Color = if (progress <= 0f || mask.alpha == 0f) this else mask.copy(alpha = mask.alpha * progress).compositeOver(this)

/** The minimum press tint progress before a release may fade out (COUIMaskEffectDrawable). */
private val PressedTintMinVisibleProgress = 0.7f

/** Thrown to stop the deferred press-in tint animation once [PressedTintMinVisibleProgress] is reached. */
private class CardPressTintThresholdReached : CancellationException("Press tint reached the minimum visible progress")
