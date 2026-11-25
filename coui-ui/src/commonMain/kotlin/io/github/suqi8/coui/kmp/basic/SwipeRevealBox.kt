// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.isBlurEnabled
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.squircle.addSquircleRect
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

/**
 * An action revealed by a physical left swipe on a [SwipeRevealBox].
 *
 * Supports four button content modes:
 * - **Icon only**: pass [icon] with `label = null`.
 * - **Built-in animated trash**: pass `icon = null, label = null` (sets [useTrashIcon] `= true`),
 *   drawing [NotificationTrashIcon] whose lid opens at the full-swipe threshold.
 * - **Text only** (COUI `SwipeActionItem` style): pass [label] with `icon = null`.
 * - **Icon + Text**: pass both [icon] (or `useTrashIcon = true`) and [label].
 *
 * Pass [containerColor] to give an action its own pill background (e.g. primary blue, orange, or
 * error red) with smooth liquid-melt color blending; leave it `null` to share the card surface.
 *
 * Set [requireConfirmation] to `true` for the COUI-Kit two-step confirmation pattern: the first tap
 * switches the glyph to a checkmark and arms confirmation; the second tap invokes [onClick].
 * Closing the menu cancels any pending confirmation.
 */
@Immutable
@ExperimentalCOUIApi
data class SwipeRevealAction(
    val icon: ImageVector? = null,
    val description: String = "",
    val onClick: () -> Unit,
    val color: Color? = null,
    val label: String? = null,
    val containerColor: Color? = null,
    val useTrashIcon: Boolean = icon == null && label == null,
    val requireConfirmation: Boolean = false,
    val confirmDescription: String = description,
)

/** A saveable, interruptible swipe state for [SwipeRevealBox]. */
@Stable
@ExperimentalCOUIApi
class SwipeRevealState(initiallyRevealed: Boolean = false) {
    /** The card's travel in pixels: zero when closed, negative towards the menu and beyond. */
    internal var offsetPx by mutableFloatStateOf(0f)

    /** How far the card may travel: the menu, or the whole row when a full swipe deletes. */
    internal var maxPx by mutableFloatStateOf(0f)

    /** Full row width in pixels, used when a delete action sweeps the row off to the left. */
    internal var sweepMaxPx by mutableFloatStateOf(0f)

    /** Menu strip width in pixels, published by [SwipeRevealBox] once it knows its actions. */
    internal var revealPx by mutableFloatStateOf(0f)

    /** Active settle spring published by the hosting [SwipeRevealBox]. */
    internal var settleSpec: SpringSpec<Float> = SwipeRevealDefaults.SettleSpring

    /** Whether a released full swipe is carrying the row away. */
    internal var dismissing by mutableStateOf(false)

    /** Whether the finger is on the row. */
    internal var dragging by mutableStateOf(false)

    /** The destination the row last settled or is settling on. */
    internal var revealedTarget by mutableStateOf(initiallyRevealed)

    internal val draggable = DraggableState { delta ->
        val boundPx = if (dismissing) maxOf(maxPx, sweepMaxPx) else maxPx
        offsetPx = (offsetPx + delta).coerceIn(-boundPx, 0f)
    }

    /** Current visual reveal fraction; it remains in 0..1 even during spring overshoot. */
    val revealFraction: Float
        get() {
            val offset = offsetPx
            // A settled offset is positive zero, whose negation is negative zero; report plain zero.
            return if (offset >= 0f || revealPx == 0f) 0f else (-offset / revealPx).coerceIn(0f, 1f)
        }

    /** How far past the menu a full swipe has carried the card, in pixels. */
    internal val overshootPx: Float get() = (-offsetPx - revealPx).coerceAtLeast(0f)

    /**
     * Whether the menu is open or opening. While the finger is down this follows the side of the
     * half-way point the card is on; afterwards it is the destination the row settles on.
     */
    val isRevealed: Boolean get() = if (dragging) -offsetPx > revealPx / 2 else revealedTarget

    /** Reveals the actions with the active settle spring. A new drag cancels this animation. */
    suspend fun reveal() = settle(true, spec = settleSpec)

    /** Returns the card and its revealed actions to the closed position. */
    suspend fun close() = settle(false, spec = settleSpec)

    internal suspend fun settle(
        revealed: Boolean,
        velocity: Float = 0f,
        spec: SpringSpec<Float> = settleSpec,
    ) {
        revealedTarget = revealed
        // Critically damped settle springs mirror COUISlideView.animationScrollTo: ignore fling
        // carry-over velocity so the row settles cleanly without rubber-band overshoot.
        val initialVelocity = if (spec.dampingRatio >= 0.99f) 0f else velocity
        animateOffsetTo(if (revealed) -revealPx else 0f, initialVelocity, spec)
    }

    internal suspend fun animateOffsetTo(target: Float, velocity: Float, spec: AnimationSpec<Float>) {
        draggable.drag {
            animate(offsetPx, target, velocity, spec) { value, _ -> dragBy(value - offsetPx) }
        }
    }

    internal suspend fun snapTo(target: Float) {
        draggable.drag { dragBy(target - offsetPx) }
    }

    companion object {
        /** Saves the destination rather than an unfinished drag's pixel offset. */
        val Saver = Saver<SwipeRevealState, Boolean>(
            save = { it.isRevealed },
            restore = { SwipeRevealState(it) },
        )
    }
}

/** Remembers a [SwipeRevealState] across recreation. */
@Composable
@ExperimentalCOUIApi
fun rememberSwipeRevealState(initiallyRevealed: Boolean = false): SwipeRevealState = rememberSaveable(saver = SwipeRevealState.Saver) { SwipeRevealState(initiallyRevealed) }

/**
 * Visual, geometric, and motion styling for [SwipeRevealBox].
 *
 * Every notification-specific trait — corner radius, corner curve family (standard COUI Lp squircle
 * vs. SystemUI `CONIC`), surface blur, edge rim highlight, minimum height, action button size,
 * corner radius, alignment, gaps, follow distance, and settle/recoil springs — is configured
 * independently here and can be customized or turned off.
 *
 * Presets in [SwipeRevealDefaults]:
 * - [SwipeRevealDefaults.cardStyle]: standard COUI `Card` (`17.dp` Lp `3.0`) with compact `48.dp`
 *   floating squircle action buttons and critically damped settle physics.
 * - [SwipeRevealDefaults.circleActionStyle]: COUI-Kit droplet style with compact `44.dp` circular
 *   action buttons (`22.dp` radius) centered vertically.
 * - [SwipeRevealDefaults.roundRectActionStyle]: upstream COUI 17 `SwipeActionItem` RoundRect style
 *   (`68.dp` wide, `12.dp` radius) suited for colored pills, text labels, and icon + text buttons.
 * - [SwipeRevealDefaults.groupCardStyle]: `24.dp` Lp `2.5` preference group card corners.
 * - [SwipeRevealDefaults.notificationStyle]: ColorOS 17 SystemUI notification shade styling.
 */
@Immutable
@ExperimentalCOUIApi
data class SwipeRevealStyle(
    val cornerRadius: Dp = CardDefaults.CornerRadius,
    val actionCornerRadius: Dp = SwipeRevealDefaults.CompactActionCornerRadius,
    val cornerWeight: Float = CardDefaults.Weight,
    val conicCornerWeight: Float = 0f,
    val containerColor: Color = Color.Unspecified,
    val actionColor: Color = Color.Unspecified,
    val enableBlur: Boolean = false,
    val blurRadius: Float = 0f,
    val blurColors: List<BlendColorEntry>? = null,
    val enableRim: Boolean = false,
    val rimColor: Color = Color.Unspecified,
    val rimLineWidth: Dp = NotificationSwipeDefaults.RimLineWidth,
    val edgeStyle: GradientStrokeStyle? = null,
    val minHeight: Dp = 0.dp,
    val actionWidth: Dp = SwipeRevealDefaults.CompactActionSize,
    val actionMaxHeight: Dp = SwipeRevealDefaults.CompactActionSize,
    val actionVerticalInset: Dp = SwipeRevealDefaults.CompactActionVerticalInset,
    val centerActionsVertically: Boolean = true,
    val actionGap: Dp = SwipeRevealDefaults.ActionGap,
    val endPadding: Dp = SwipeRevealDefaults.EndPadding,
    val blendRange: Dp = SwipeRevealDefaults.BlendRange,
    val followDistance: Dp = SwipeRevealDefaults.FollowDistance,
    val settleSpring: SpringSpec<Float> = SwipeRevealDefaults.SettleSpring,
    val recoilSpring: SpringSpec<Float> = SwipeRevealDefaults.RecoilSpring,
)

/** Defaults and style presets for [SwipeRevealBox]. */
@ExperimentalCOUIApi
object SwipeRevealDefaults {
    /** Default compact floating button size in [cardStyle] and [groupCardStyle]. */
    val CompactActionSize = 48.dp

    /** Default corner radius of compact floating action buttons in [cardStyle]. */
    val CompactActionCornerRadius = 14.dp

    /** Minimum vertical inset keeping compact action buttons visibly smaller than the card. */
    val CompactActionVerticalInset = 6.dp

    /** Compact circular droplet button size in [circleActionStyle] (inspired by COUI-Kit). */
    val CircleActionSize = 44.dp

    /** Default width of wider RoundRect buttons in [roundRectActionStyle] (for text / icon+text). */
    val RoundRectActionWidth = 68.dp

    /** Default max height of RoundRect buttons in [roundRectActionStyle]. */
    val RoundRectActionMaxHeight = 52.dp

    /** Default corner radius of RoundRect buttons (`coui_round_corner_m = 12.dp`). */
    val RoundRectActionCornerRadius = 12.dp

    /** Default action width for notification-sized buttons (`60.dp`). */
    val ActionWidth = NotificationSwipeDefaults.ActionWidth

    /** Default horizontal gap between the card and each action cell in [cardStyle]. */
    val ActionGap = 8.dp

    /** Default trailing inset after the rightmost action cell in [cardStyle]. */
    val EndPadding = 0.dp

    /** Default melt-ball blend range connecting the card and action silhouettes. */
    val BlendRange = NotificationSwipeDefaults.BlendRange

    /** Default extra swipe distance past the revealed menu before a full swipe arms deletion. */
    val MergeDistance = NotificationSwipeDefaults.MergeDistance

    /** Restrained liquid follow distance in [cardStyle] so slow drags detach smoothly. */
    val FollowDistance = 6.dp

    /**
     * Critically damped card settle spring (`dampingRatio = 1f, stiffness = 320f`), mirroring
     * ColorOS `COUISlideView` (`SwipeActionItem`) so slow drags and flicks settle cleanly without
     * rubber-band overshoot.
     */
    val SettleSpring: SpringSpec<Float> = SpringSpec(dampingRatio = 1f, stiffness = 320f)

    /**
     * Restrained action detachment spring (`damping = 0.88f, response = 0.32f`): preserves the
     * tactile liquid release without exaggerated bouncing during slow drags.
     */
    val RecoilSpring: SpringSpec<Float> = folmeSpring(damping = 0.88f, response = 0.32f)

    /** Total revealed menu strip width for [actionCount] cells using the given geometry. */
    fun revealWidth(
        actionCount: Int,
        actionWidth: Dp = CompactActionSize,
        actionGap: Dp = ActionGap,
        endPadding: Dp = EndPadding,
    ): Dp = (actionWidth + actionGap) * actionCount + endPadding

    /** Total revealed menu strip width for [actionCount] cells using [style]. */
    fun revealWidth(
        actionCount: Int,
        style: SwipeRevealStyle,
    ): Dp = revealWidth(
        actionCount = actionCount,
        actionWidth = style.actionWidth,
        actionGap = style.actionGap,
        endPadding = style.endPadding,
    )

    /** Full-swipe distance past which releasing triggers the primary action. */
    fun dismissThreshold(revealWidth: Dp): Dp = revealWidth + MergeDistance

    /**
     * Standard COUI standalone card preset: matches [Card] corner geometry (`17.dp`, Lp `3.0`
     * squircle), opaque `surfaceContainer` fill, no glass blur, no notification rim highlight,
     * compact `48.dp × 48.dp` centered floating action buttons, and critically damped settle
     * physics so slow drags never over-bounce.
     */
    @Composable
    fun cardStyle(
        cornerRadius: Dp = COUITheme.dimens.card.cornerRadius,
        actionCornerRadius: Dp = CompactActionCornerRadius,
        cornerWeight: Float = CardDefaults.Weight,
        conicCornerWeight: Float = 0f,
        containerColor: Color = COUITheme.colorScheme.surfaceContainer,
        actionColor: Color = COUITheme.colorScheme.onSurface,
        enableBlur: Boolean = false,
        blurRadius: Float = NotificationSwipeDefaults.blurRadius(),
        blurColors: List<BlendColorEntry>? = null,
        enableRim: Boolean = false,
        rimColor: Color = Color.Unspecified,
        rimLineWidth: Dp = NotificationSwipeDefaults.RimLineWidth,
        edgeStyle: GradientStrokeStyle? = null,
        minHeight: Dp = 0.dp,
        actionWidth: Dp = CompactActionSize,
        actionMaxHeight: Dp = CompactActionSize,
        actionVerticalInset: Dp = CompactActionVerticalInset,
        centerActionsVertically: Boolean = true,
        actionGap: Dp = ActionGap,
        endPadding: Dp = EndPadding,
        blendRange: Dp = BlendRange,
        followDistance: Dp = FollowDistance,
        settleSpring: SpringSpec<Float> = SettleSpring,
        recoilSpring: SpringSpec<Float> = RecoilSpring,
    ): SwipeRevealStyle = SwipeRevealStyle(
        cornerRadius = cornerRadius,
        actionCornerRadius = actionCornerRadius,
        cornerWeight = cornerWeight,
        conicCornerWeight = conicCornerWeight,
        containerColor = containerColor,
        actionColor = actionColor,
        enableBlur = enableBlur,
        blurRadius = blurRadius,
        blurColors = blurColors,
        enableRim = enableRim,
        rimColor = rimColor,
        rimLineWidth = rimLineWidth,
        edgeStyle = edgeStyle,
        minHeight = minHeight,
        actionWidth = actionWidth,
        actionMaxHeight = actionMaxHeight,
        actionVerticalInset = actionVerticalInset,
        centerActionsVertically = centerActionsVertically,
        actionGap = actionGap,
        endPadding = endPadding,
        blendRange = blendRange,
        followDistance = followDistance,
        settleSpring = settleSpring,
        recoilSpring = recoilSpring,
    )

    /**
     * COUI-Kit droplet circular button preset: matches [Card] corners (`17.dp`, Lp `3.0`) with
     * compact circular (`44.dp × 44.dp`, `22.dp` corner radius) vertically centered action buttons
     * that detach cleanly from the card edge.
     */
    @Composable
    fun circleActionStyle(
        cornerRadius: Dp = COUITheme.dimens.card.cornerRadius,
        cornerWeight: Float = CardDefaults.Weight,
        conicCornerWeight: Float = 0f,
        containerColor: Color = COUITheme.colorScheme.surfaceContainer,
        actionColor: Color = COUITheme.colorScheme.onSurface,
        enableBlur: Boolean = false,
        blurRadius: Float = NotificationSwipeDefaults.blurRadius(),
        blurColors: List<BlendColorEntry>? = null,
        enableRim: Boolean = false,
        rimColor: Color = Color.Unspecified,
        rimLineWidth: Dp = NotificationSwipeDefaults.RimLineWidth,
        edgeStyle: GradientStrokeStyle? = null,
        minHeight: Dp = 0.dp,
        actionSize: Dp = CircleActionSize,
        actionVerticalInset: Dp = CompactActionVerticalInset,
        actionGap: Dp = 10.dp,
        endPadding: Dp = EndPadding,
        blendRange: Dp = BlendRange,
        followDistance: Dp = FollowDistance,
        settleSpring: SpringSpec<Float> = SettleSpring,
        recoilSpring: SpringSpec<Float> = RecoilSpring,
    ): SwipeRevealStyle = SwipeRevealStyle(
        cornerRadius = cornerRadius,
        actionCornerRadius = actionSize / 2,
        cornerWeight = cornerWeight,
        conicCornerWeight = conicCornerWeight,
        containerColor = containerColor,
        actionColor = actionColor,
        enableBlur = enableBlur,
        blurRadius = blurRadius,
        blurColors = blurColors,
        enableRim = enableRim,
        rimColor = rimColor,
        rimLineWidth = rimLineWidth,
        edgeStyle = edgeStyle,
        minHeight = minHeight,
        actionWidth = actionSize,
        actionMaxHeight = actionSize,
        actionVerticalInset = actionVerticalInset,
        centerActionsVertically = true,
        actionGap = actionGap,
        endPadding = endPadding,
        blendRange = blendRange,
        followDistance = followDistance,
        settleSpring = settleSpring,
        recoilSpring = recoilSpring,
    )

    /**
     * Upstream COUI 17 [SwipeActionItem] RoundRect button preset with liquid-melt physics:
     * `68.dp`-wide, `12.dp`-radius floating pills suited for colored actions, text-only labels, and
     * stacked icon + text buttons.
     */
    @Composable
    fun roundRectActionStyle(
        cornerRadius: Dp = COUITheme.dimens.card.cornerRadius,
        actionCornerRadius: Dp = RoundRectActionCornerRadius,
        cornerWeight: Float = CardDefaults.Weight,
        conicCornerWeight: Float = 0f,
        containerColor: Color = COUITheme.colorScheme.surfaceContainer,
        actionColor: Color = COUITheme.colorScheme.onSurface,
        enableBlur: Boolean = false,
        blurRadius: Float = NotificationSwipeDefaults.blurRadius(),
        blurColors: List<BlendColorEntry>? = null,
        enableRim: Boolean = false,
        rimColor: Color = Color.Unspecified,
        rimLineWidth: Dp = NotificationSwipeDefaults.RimLineWidth,
        edgeStyle: GradientStrokeStyle? = null,
        minHeight: Dp = 0.dp,
        actionWidth: Dp = RoundRectActionWidth,
        actionMaxHeight: Dp = RoundRectActionMaxHeight,
        actionVerticalInset: Dp = CompactActionVerticalInset,
        centerActionsVertically: Boolean = true,
        actionGap: Dp = 6.dp,
        endPadding: Dp = EndPadding,
        blendRange: Dp = BlendRange,
        followDistance: Dp = FollowDistance,
        settleSpring: SpringSpec<Float> = SettleSpring,
        recoilSpring: SpringSpec<Float> = RecoilSpring,
    ): SwipeRevealStyle = SwipeRevealStyle(
        cornerRadius = cornerRadius,
        actionCornerRadius = actionCornerRadius,
        cornerWeight = cornerWeight,
        conicCornerWeight = conicCornerWeight,
        containerColor = containerColor,
        actionColor = actionColor,
        enableBlur = enableBlur,
        blurRadius = blurRadius,
        blurColors = blurColors,
        enableRim = enableRim,
        rimColor = rimColor,
        rimLineWidth = rimLineWidth,
        edgeStyle = edgeStyle,
        minHeight = minHeight,
        actionWidth = actionWidth,
        actionMaxHeight = actionMaxHeight,
        actionVerticalInset = actionVerticalInset,
        centerActionsVertically = centerActionsVertically,
        actionGap = actionGap,
        endPadding = endPadding,
        blendRange = blendRange,
        followDistance = followDistance,
        settleSpring = settleSpring,
        recoilSpring = recoilSpring,
    )

    /**
     * COUI preference group card preset: matches [CardDefaults.GroupCornerRadius] (`24.dp`) and
     * [CardDefaults.GroupWeight] (Lp `2.5`) with compact floating buttons and no blur/rim.
     */
    @Composable
    fun groupCardStyle(
        cornerRadius: Dp = CardDefaults.GroupCornerRadius,
        actionCornerRadius: Dp = CompactActionCornerRadius,
        cornerWeight: Float = CardDefaults.GroupWeight,
        conicCornerWeight: Float = 0f,
        containerColor: Color = COUITheme.colorScheme.surfaceContainer,
        actionColor: Color = COUITheme.colorScheme.onSurface,
        enableBlur: Boolean = false,
        blurRadius: Float = NotificationSwipeDefaults.blurRadius(),
        blurColors: List<BlendColorEntry>? = null,
        enableRim: Boolean = false,
        rimColor: Color = Color.Unspecified,
        rimLineWidth: Dp = NotificationSwipeDefaults.RimLineWidth,
        edgeStyle: GradientStrokeStyle? = null,
        minHeight: Dp = 0.dp,
        actionWidth: Dp = CompactActionSize,
        actionMaxHeight: Dp = CompactActionSize,
        actionVerticalInset: Dp = CompactActionVerticalInset,
        centerActionsVertically: Boolean = true,
        actionGap: Dp = ActionGap,
        endPadding: Dp = EndPadding,
        blendRange: Dp = BlendRange,
        followDistance: Dp = FollowDistance,
        settleSpring: SpringSpec<Float> = SettleSpring,
        recoilSpring: SpringSpec<Float> = RecoilSpring,
    ): SwipeRevealStyle = SwipeRevealStyle(
        cornerRadius = cornerRadius,
        actionCornerRadius = actionCornerRadius,
        cornerWeight = cornerWeight,
        conicCornerWeight = conicCornerWeight,
        containerColor = containerColor,
        actionColor = actionColor,
        enableBlur = enableBlur,
        blurRadius = blurRadius,
        blurColors = blurColors,
        enableRim = enableRim,
        rimColor = rimColor,
        rimLineWidth = rimLineWidth,
        edgeStyle = edgeStyle,
        minHeight = minHeight,
        actionWidth = actionWidth,
        actionMaxHeight = actionMaxHeight,
        actionVerticalInset = actionVerticalInset,
        centerActionsVertically = centerActionsVertically,
        actionGap = actionGap,
        endPadding = endPadding,
        blendRange = blendRange,
        followDistance = followDistance,
        settleSpring = settleSpring,
        recoilSpring = recoilSpring,
    )

    /**
     * ColorOS 17 SystemUI notification shade preset: `16.dp` `CONIC` continuous curvature
     * (`weight = 1.1`), `250px` backdrop blur with light/dark blend layers, edge lighting rim,
     * `84.dp` minimum height, and top-aligned `60.dp × 68.dp` action cells.
     */
    @Composable
    fun notificationStyle(
        cornerRadius: Dp = NotificationSwipeDefaults.CardCornerRadius,
        actionCornerRadius: Dp = cornerRadius,
        cornerWeight: Float = CardDefaults.Weight,
        conicCornerWeight: Float = NotificationSwipeDefaults.CONIC_CORNER_WEIGHT,
        containerColor: Color = Color.Unspecified,
        actionColor: Color = COUITheme.colorScheme.onSurface,
        enableBlur: Boolean = true,
        blurRadius: Float = NotificationSwipeDefaults.blurRadius(),
        blurColors: List<BlendColorEntry>? = null,
        enableRim: Boolean = true,
        rimColor: Color = Color.Unspecified,
        rimLineWidth: Dp = NotificationSwipeDefaults.RimLineWidth,
        edgeStyle: GradientStrokeStyle? = null,
        minHeight: Dp = NotificationSwipeDefaults.MinHeight,
        actionWidth: Dp = NotificationSwipeDefaults.ActionWidth,
        actionMaxHeight: Dp = NotificationSwipeDefaults.ActionHeight,
        actionVerticalInset: Dp = NotificationSwipeDefaults.ActionTop,
        centerActionsVertically: Boolean = false,
        actionGap: Dp = NotificationSwipeDefaults.ActionGap,
        endPadding: Dp = NotificationSwipeDefaults.EndPadding,
        blendRange: Dp = NotificationSwipeDefaults.BlendRange,
        followDistance: Dp = NotificationSwipeDefaults.FollowDistance,
        settleSpring: SpringSpec<Float> = NotificationSwipeDefaults.SettleSpring,
        recoilSpring: SpringSpec<Float> = NotificationSwipeDefaults.MenuRecoilSpring,
    ): SwipeRevealStyle = SwipeRevealStyle(
        cornerRadius = cornerRadius,
        actionCornerRadius = actionCornerRadius,
        cornerWeight = cornerWeight,
        conicCornerWeight = conicCornerWeight,
        containerColor = containerColor,
        actionColor = actionColor,
        enableBlur = enableBlur,
        blurRadius = blurRadius,
        blurColors = blurColors,
        enableRim = enableRim,
        rimColor = rimColor,
        rimLineWidth = rimLineWidth,
        edgeStyle = edgeStyle,
        minHeight = minHeight,
        actionWidth = actionWidth,
        actionMaxHeight = actionMaxHeight,
        actionVerticalInset = actionVerticalInset,
        centerActionsVertically = centerActionsVertically,
        actionGap = actionGap,
        endPadding = endPadding,
        blendRange = blendRange,
        followDistance = followDistance,
        settleSpring = settleSpring,
        recoilSpring = recoilSpring,
    )
}

/**
 * A general-purpose swipe container that reveals up to 3 actions on a physical left swipe with
 * ColorOS 17 liquid-melt (`MetaBallSurface`) physics.
 *
 * Unlike [NotificationSwipeBox], [SwipeRevealBox] defaults to [SwipeRevealDefaults.cardStyle]:
 * standard COUI [Card] corners (`17.dp` Lp `3.0` squircle), opaque `surfaceContainer` fill, no
 * backdrop blur or rim highlight, compact `48.dp` centered floating action buttons, and critically
 * damped settle physics so slow drags settle smoothly without exaggerated bounce.
 *
 * Supply actions in visual left-to-right order: the last action sits at the trailing edge and is
 * revealed first. Actions can display an icon, a text label, both, the built-in animated trash
 * icon, a two-step confirmation checkmark, or per-button custom container colors.
 *
 * Supply foreground [content] without its own opaque background so it does not paint over the
 * melt silhouette.
 *
 * @param actions Up to 3 actions in visual left-to-right order.
 * @param modifier Modifier applied to the entire swipe container.
 * @param state Controls the reveal destination and observes visual swipe progress.
 * @param enabled Whether dragging and actions are enabled.
 * @param style Visual, geometric, and motion preset; defaults to [SwipeRevealDefaults.cardStyle].
 * @param dismissOnFullSwipe Whether dragging past [SwipeRevealDefaults.MergeDistance] arms and
 *   invokes the primary (rightmost) action on release.
 * @param dismissPrimaryOnClick Whether tapping the primary (rightmost) action sweeps the row away
 *   before invoking its callback, or simply closes the menu.
 * @param content Foreground content measured at full width throughout the gesture.
 */
@Composable
@ExperimentalCOUIApi
fun SwipeRevealBox(
    actions: List<SwipeRevealAction>,
    modifier: Modifier = Modifier,
    state: SwipeRevealState = rememberSwipeRevealState(),
    enabled: Boolean = true,
    style: SwipeRevealStyle = SwipeRevealDefaults.cardStyle(),
    dismissOnFullSwipe: Boolean = false,
    dismissPrimaryOnClick: Boolean = dismissOnFullSwipe,
    content: @Composable BoxScope.() -> Unit,
) {
    SwipeRevealBoxImpl(
        actions = actions,
        modifier = modifier,
        state = state,
        enabled = enabled,
        style = style,
        dismissOnFullSwipe = dismissOnFullSwipe,
        dismissPrimaryOnClick = dismissPrimaryOnClick,
        content = content,
    )
}

@Composable
internal fun SwipeRevealBoxImpl(
    actions: List<SwipeRevealAction>,
    modifier: Modifier = Modifier,
    state: SwipeRevealState,
    enabled: Boolean,
    style: SwipeRevealStyle,
    dismissOnFullSwipe: Boolean,
    dismissPrimaryOnClick: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }
    val actionCount = actions.size
    val revealWidth = SwipeRevealDefaults.revealWidth(actionCount, style)
    val revealWidthPx = with(density) { revealWidth.toPx() }
    val widthPx = measuredSize.width.toFloat()
    val width = with(density) { measuredSize.width.toDp() }
    val height = with(density) { measuredSize.height.toDp() }
    val sweepMaxPx = maxOf(widthPx, revealWidthPx)
    val maxPx = if (dismissOnFullSwipe) sweepMaxPx else revealWidthPx
    val dismissThresholdPx = with(density) { SwipeRevealDefaults.dismissThreshold(revealWidth).toPx() }
    val flingVelocityPx = with(density) { NotificationSwipeDefaults.FlingVelocity.toPx() }
    val maxVelocityPx = with(density) { 4000.dp.toPx() }

    LaunchedEffect(state, revealWidthPx, maxPx, sweepMaxPx, style.settleSpring) {
        state.revealPx = revealWidthPx
        state.maxPx = maxPx
        state.sweepMaxPx = sweepMaxPx
        state.settleSpec = style.settleSpring
        if (!state.dragging && !state.dismissing) state.snapTo(if (state.revealedTarget) -revealWidthPx else 0f)
    }

    val primaryAction = actions.lastOrNull()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { measuredSize = it }
            .draggable(
                state = state.draggable,
                orientation = Orientation.Horizontal,
                reverseDirection = false,
                enabled = enabled && !state.dismissing && width > revealWidth && actionCount > 0,
                onDragStarted = { state.dragging = true },
                onDragStopped = { velocity ->
                    state.dragging = false
                    val pulled = -state.offsetPx
                    val clampedVelocity = velocity.coerceIn(-maxVelocityPx, maxVelocityPx)
                    if (dismissOnFullSwipe && pulled >= dismissThresholdPx && primaryAction != null) {
                        dismissSwipeRevealRow(
                            state = state,
                            velocity = clampedVelocity,
                            flingVelocityPx = flingVelocityPx,
                            onDismiss = primaryAction.onClick,
                        )
                    } else {
                        val reveal = when {
                            clampedVelocity < -flingVelocityPx -> true
                            clampedVelocity > flingVelocityPx -> false
                            else -> pulled > state.revealPx / 2
                        }
                        state.settle(reveal, clampedVelocity, style.settleSpring)
                    }
                },
            )
            .semantics {
                customActions = if (enabled) {
                    actions.map { action ->
                        val actionLabel = action.description.ifEmpty { action.label.orEmpty() }
                        CustomAccessibilityAction(actionLabel) {
                            action.onClick()
                            true
                        }
                    }
                } else {
                    emptyList()
                }
            },
    ) {
        if (measuredSize != IntSize.Zero) {
            SwipeRevealMenu(
                state = state,
                width = width,
                height = height,
                actions = actions,
                style = style,
                enabled = enabled,
                dismissPrimaryOnClick = dismissPrimaryOnClick,
                flingVelocityPx = flingVelocityPx,
            )
        }
        SwipeRevealCard(state = state, style = style, content = content)
    }
}

private suspend fun dismissSwipeRevealRow(
    state: SwipeRevealState,
    velocity: Float,
    flingVelocityPx: Float,
    onDismiss: () -> Unit,
) {
    state.dismissing = true
    state.revealedTarget = false
    try {
        val sweepTargetPx = maxOf(state.maxPx, state.sweepMaxPx)
        val fling = velocity < -flingVelocityPx
        val remaining = (sweepTargetPx + state.offsetPx).coerceAtLeast(0f)
        val millis = if (fling) {
            (remaining * 1000f / -velocity).toInt().coerceIn(1, NotificationSwipeDefaults.SWEEP_MILLIS)
        } else {
            NotificationSwipeDefaults.SWEEP_MILLIS
        }
        state.animateOffsetTo(
            target = -sweepTargetPx,
            velocity = 0f,
            spec = tween(millis, easing = if (fling) LinearEasing else FastOutLinearInEasing),
        )
        onDismiss()
        state.snapTo(0f)
    } finally {
        state.dismissing = false
    }
}

/**
 * One-shot follow-and-rebound motion per action cell, following COUI-Kit's
 * `rememberCouiMetaballFollowingTranslation`:
 * - Follows the card smoothly as this cell exposes (`delta > 0`), capped to [followDistance].
 * - Fires ONE rebound back to `0.dp` once this cell reaches full exposure (`actionWidth + follow`).
 * - Eases smoothly back toward `0.dp` if the user drags back before full exposure, and stays calm
 *   at `0.dp` when closing from the revealed state (preventing exaggerated slow-drag bounce).
 */
@Composable
private fun rememberSwipeActionFollowDp(
    cellExposureDp: () -> Float,
    actionWidthDp: Float,
    followDistanceDp: Float,
    recoilSpec: SpringSpec<Float>,
): State<Float> {
    val follow = remember { Animatable(0f) }
    val currentExposure by rememberUpdatedState(cellExposureDp)
    LaunchedEffect(actionWidthDp, followDistanceDp, recoilSpec) {
        if (followDistanceDp <= 0f || actionWidthDp <= 0f) {
            follow.snapTo(0f)
            return@LaunchedEffect
        }
        val followEnd = actionWidthDp + followDistanceDp
        val followRatio = followDistanceDp / followEnd
        var previousExposure = currentExposure()
        var rebound: Job? = null
        var hasDetached = previousExposure >= followEnd
        snapshotFlow { currentExposure() }.collect { exposure ->
            val delta = exposure - previousExposure
            if (exposure <= 0.01f) {
                rebound?.cancelAndJoin()
                rebound = null
                hasDetached = false
                if (follow.value != 0f) follow.snapTo(0f)
            } else if (!hasDetached) {
                if (delta > 0f && rebound?.isActive != true) {
                    if (exposure <= followEnd) {
                        val next = (follow.value + delta * followRatio).coerceIn(0f, followDistanceDp)
                        follow.snapTo(next)
                    }
                    if ((previousExposure < followEnd && exposure >= followEnd) ||
                        follow.value >= followDistanceDp
                    ) {
                        hasDetached = true
                        rebound = launch {
                            follow.animateTo(0f, recoilSpec, initialVelocity = 0f)
                        }
                    }
                } else if (delta < 0f && follow.value > 0f && rebound?.isActive != true) {
                    // Dragging back before detaching eases the follow smoothly toward zero.
                    val cap = (exposure * followRatio).coerceIn(0f, followDistanceDp)
                    follow.snapTo(minOf(follow.value, cap))
                }
            }
            previousExposure = exposure
        }
    }
    return follow.asState()
}

/**
 * The merged card and action-cell material. It reads the swipe offset while it composes, so it is
 * the only part of [SwipeRevealBox] that re-runs on every drag frame.
 */
@Composable
private fun BoxScope.SwipeRevealMenu(
    state: SwipeRevealState,
    width: Dp,
    height: Dp,
    actions: List<SwipeRevealAction>,
    style: SwipeRevealStyle,
    enabled: Boolean,
    dismissPrimaryOnClick: Boolean,
    flingVelocityPx: Float,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val actionCount = actions.size
    val revealWidth = SwipeRevealDefaults.revealWidth(actionCount, style)
    val distance = revealWidth * state.revealFraction
    val overshoot = with(density) { state.overshootPx.toDp() }
    val dismissing = state.dismissing
    val merge by animateFloatAsState(
        targetValue = if (actionCount > 1 && (dismissing || overshoot >= NotificationSwipeDefaults.MergeDistance)) 1f else 0f,
        animationSpec = NotificationSwipeDefaults.MergeSpring,
    )
    val armed = state.dragging && overshoot >= NotificationSwipeDefaults.MergeDistance
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(armed) {
        if (armed) {
            haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        } else if (state.dragging) {
            haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
    }

    var confirmedActionIndex by remember(state) { mutableIntStateOf(-1) }
    LaunchedEffect(state.revealFraction <= 0.01f, dismissing) {
        if (state.revealFraction <= 0.01f || dismissing) {
            confirmedActionIndex = -1
        }
    }

    val lidClose by animateFloatAsState(
        targetValue = if (dismissing) 1f else 0f,
        animationSpec = tween(NotificationSwipeDefaults.LID_CLOSE_MILLIS, easing = NotificationSwipeDefaults.LidCloseEasing),
    )
    val lidProgress = if (dismissing) {
        0.5f + 0.5f * lidClose
    } else {
        ((overshoot - NotificationSwipeDefaults.MergeDistance) / NotificationSwipeDefaults.LidDistance).coerceIn(0f, 1f) / 2f
    }
    val deleteFade by animateFloatAsState(
        targetValue = if (dismissing) 0f else 1f,
        animationSpec = NotificationSwipeDefaults.DismissFadeSpring,
    )

    val pitch = style.actionWidth + style.actionGap
    val progress0 = swipeRevealActionProgress(distance, 0, style)
    val progress1 = if (actionCount > 1) swipeRevealActionProgress(distance, 1, style) else 0f
    val progress2 = if (actionCount > 2) swipeRevealActionProgress(distance, 2, style) else 0f

    val follow0 by rememberSwipeActionFollowDp(
        cellExposureDp = { (revealWidth * state.revealFraction - style.endPadding).value.coerceAtLeast(0f) },
        actionWidthDp = style.actionWidth.value,
        followDistanceDp = style.followDistance.value,
        recoilSpec = style.recoilSpring,
    )
    val follow1 by rememberSwipeActionFollowDp(
        cellExposureDp = {
            if (actionCount > 1) {
                (revealWidth * state.revealFraction - style.endPadding - pitch).value.coerceAtLeast(0f)
            } else {
                0f
            }
        },
        actionWidthDp = style.actionWidth.value,
        followDistanceDp = style.followDistance.value,
        recoilSpec = style.recoilSpring,
    )
    val follow2 by rememberSwipeActionFollowDp(
        cellExposureDp = {
            if (actionCount > 2) {
                (revealWidth * state.revealFraction - style.endPadding - pitch * 2).value.coerceAtLeast(0f)
            } else {
                0f
            }
        },
        actionWidthDp = style.actionWidth.value,
        followDistanceDp = style.followDistance.value,
        recoilSpec = style.recoilSpring,
    )

    val touchState0 = rememberCOUITouchMotionState()
    val touchState1 = rememberCOUITouchMotionState()
    val touchState2 = rememberCOUITouchMotionState()
    val touchStates = listOf(touchState0, touchState1, touchState2)

    val totalPull = distance + overshoot
    val baseParticles = swipeRevealParticles(
        width = width,
        height = height,
        distance = distance,
        style = style,
        actionFollowsDp = listOf(follow0.dp, follow1.dp, follow2.dp),
        actionCount = actionCount,
        overshoot = overshoot,
        merge = merge,
        menuAlpha = deleteFade.coerceIn(0f, 1f),
    )
    // Suppress hidden action particles while resting (totalPull == 0) or before the card edge
    // begins uncovering each cell so tighter action corners or custom fills never peek out behind
    // the card silhouette.
    val particles = baseParticles.mapIndexed { index, particle ->
        if (index == 0) {
            particle
        } else {
            val cellIndex = index - 1
            val uncoveredDp = (totalPull - style.endPadding - pitch * cellIndex).value
            val revealAlpha = (uncoveredDp / 3f).coerceIn(0f, 1f)
            val mergeFade = if (cellIndex == 0) 1f else (1f - merge).coerceIn(0f, 1f)
            val touchState = touchStates.getOrNull(cellIndex)
            val adjustedAlpha = particle.alpha * revealAlpha * mergeFade
            if (touchState == null) {
                particle.copy(alpha = adjustedAlpha)
            } else {
                particle.copy(
                    centerX = particle.centerX + with(density) { touchState.translation.x.toDp() },
                    centerY = particle.centerY + with(density) { touchState.translation.y.toDp() },
                    scaleX = particle.scaleX * touchState.scaleX,
                    scaleY = particle.scaleY * touchState.scaleY,
                    alpha = adjustedAlpha,
                )
            }
        }
    }
    val menuShape = remember(state) {
        GenericShape { size, _ ->
            addRect(Rect((size.width + state.offsetPx).coerceAtLeast(0f), 0f, size.width, size.height))
        }
    }

    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    val customContainer = style.containerColor != Color.Unspecified
    val resolvedColor = if (customContainer) style.containerColor else NotificationSwipeDefaults.containerColor(isDark)
    val resolvedActionColor = if (style.actionColor != Color.Unspecified) style.actionColor else COUITheme.colorScheme.onSurface
    val resolvedBackdrop = if (style.enableBlur) LocalBackdrop.current else null
    val isGlassActive = resolvedBackdrop != null && isBlurEnabled()
    val resolvedBlurColors = when {
        !style.enableBlur -> null
        style.blurColors != null -> style.blurColors
        customContainer -> null
        else -> NotificationSwipeDefaults.blurColors(isDark)
    }
    val resolvedRimColor = when {
        !style.enableRim -> null
        style.rimColor == Color.Unspecified -> Color.White
        style.rimColor.alpha <= 0f -> null
        else -> style.rimColor
    }
    val resolvedEdgeStyle = style.edgeStyle ?: NotificationSwipeDefaults.edgeStyle(isDark)
    val squircleEnabled = isSquircleEnabled()
    val resolvedConicWeight = if (squircleEnabled) style.conicCornerWeight.coerceAtLeast(0f) else 0f
    val resolvedSquircleWeight = if (squircleEnabled && resolvedConicWeight <= 0.001f) style.cornerWeight.coerceAtLeast(0f) else 0f

    val particleColors = remember(actions, resolvedColor) {
        if (actions.none { it.containerColor != null }) {
            null
        } else {
            buildList<Color>(actionCount + 1) {
                add(resolvedColor)
                for (i in 0 until actionCount) {
                    val action = actions[actionCount - 1 - i]
                    add(action.containerColor ?: resolvedColor)
                }
            }
        }
    }

    MetaBallSurfaceWithRim(
        particles = particles,
        blurColors = resolvedBlurColors,
        rimColor = resolvedRimColor,
        rimLineWidth = style.rimLineWidth,
        rimLineAngle = 0f,
        modifier = Modifier.matchParentSize(),
        color = resolvedColor,
        backdrop = resolvedBackdrop,
        blurRadius = style.blurRadius,
        extension = style.blendRange,
        extensionFactorX = 1.2f,
        extensionFactorY = 2f,
        rimBottomScale = 1f,
        edgeStyle = resolvedEdgeStyle,
        conicCornerWeight = resolvedConicWeight,
        squircleCornerWeight = resolvedSquircleWeight,
        particleColors = particleColors,
    )
    Box(Modifier.matchParentSize().clip(menuShape), contentAlignment = AbsoluteAlignment.TopLeft) {
        for (i in 0 until actionCount) {
            val action = actions[actionCount - 1 - i]
            val particle = baseParticles[i + 1]
            val progress = when (i) {
                0 -> progress0
                1 -> progress1
                else -> progress2
            }
            val isPrimary = i == 0
            val isConfirmed = action.requireConfirmation && confirmedActionIndex == i
            val fade = if (isPrimary) deleteFade else (1f - merge)
            val scale = if (isPrimary) 1f else particle.scaleX
            val resolvedTint = action.color ?: if (action.containerColor != null) Color.White else resolvedActionColor
            val glassOverlayColor = if (isGlassActive && action.containerColor != null) {
                action.containerColor.copy(alpha = 0.82f)
            } else {
                null
            }
            SwipeRevealActionCell(
                particle = particle,
                progress = progress,
                action = action,
                color = resolvedTint,
                glassOverlayColor = glassOverlayColor,
                isConfirmed = isConfirmed,
                enabled = enabled && !dismissing && (isPrimary || merge == 0f),
                isDark = isDark,
                style = style,
                touchState = touchStates.getOrNull(i),
                fade = fade,
                scale = scale,
                trashProgress = if (isPrimary && action.useTrashIcon) lidProgress else 0f,
                onClick = {
                    if (action.requireConfirmation && !isConfirmed) {
                        confirmedActionIndex = i
                        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    } else {
                        confirmedActionIndex = -1
                        if (isPrimary && dismissPrimaryOnClick) {
                            if (!state.dismissing) {
                                scope.launch {
                                    dismissSwipeRevealRow(
                                        state = state,
                                        velocity = 0f,
                                        flingVelocityPx = flingVelocityPx,
                                        onDismiss = action.onClick,
                                    )
                                }
                            }
                        } else {
                            scope.launch { state.close() }
                            action.onClick()
                        }
                    }
                },
            )
        }
    }
}

/**
 * The foreground card mask. The swipe offset is read only inside the clip shape and layer lambdas,
 * so a drag re-clips the card and moves its content without recomposing either.
 */
@Composable
private fun BoxScope.SwipeRevealCard(
    state: SwipeRevealState,
    style: SwipeRevealStyle,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val squircleEnabled = isSquircleEnabled()
    val cardShape = remember(
        density,
        state,
        squircleEnabled,
        style.cornerRadius,
        style.conicCornerWeight,
        style.cornerWeight,
    ) {
        GenericShape { size, _ ->
            val visibleWidth = (size.width + state.offsetPx).coerceAtLeast(0f)
            if (visibleWidth > 0f && size.height > 0f) {
                addSwipeRevealRect(
                    width = visibleWidth,
                    height = size.height,
                    cornerPx = with(density) { style.cornerRadius.toPx() },
                    conicCornerWeight = style.conicCornerWeight,
                    squircleCornerWeight = style.cornerWeight,
                    squircleEnabled = squircleEnabled,
                )
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = style.minHeight)
            .clip(cardShape)
            // Inside the clip layer, so the mask stays put while the content travels.
            .graphicsLayer { translationX = state.offsetPx },
        content = content,
    )
}

internal fun Path.addSwipeRevealRect(
    width: Float,
    height: Float,
    cornerPx: Float,
    conicCornerWeight: Float,
    squircleCornerWeight: Float,
    squircleEnabled: Boolean,
) {
    if (squircleEnabled && conicCornerWeight > 0.001f) {
        addConicRoundedRect(
            width = width,
            height = height,
            cornerRadius = cornerPx,
            weight = conicCornerWeight,
        )
    } else if (squircleEnabled && squircleCornerWeight > 2.001f) {
        addSquircleRect(
            width = width,
            height = height,
            cornerRadius = cornerPx,
            squircleEnabled = true,
            weight = squircleCornerWeight,
        )
    } else {
        addSquircleRect(
            width = width,
            height = height,
            cornerRadius = cornerPx,
            squircleEnabled = false,
        )
    }
}

@Composable
private fun SwipeRevealActionCell(
    particle: MetaBallParticle,
    progress: Float,
    action: SwipeRevealAction,
    color: Color,
    glassOverlayColor: Color?,
    isConfirmed: Boolean,
    enabled: Boolean,
    isDark: Boolean,
    style: SwipeRevealStyle,
    touchState: COUITouchMotionState?,
    fade: Float = 1f,
    scale: Float = 1f,
    trashProgress: Float = 0f,
    onClick: () -> Unit,
) {
    val alpha = (((progress - NotificationSwipeDefaults.FADE_START) / (1f - NotificationSwipeDefaults.FADE_START)).coerceIn(0f, 1f) * fade).coerceIn(0f, 1f)
    // An entirely covered button is absent from both hit testing and the accessibility tree.
    if (alpha <= 0f) return
    val density = LocalDensity.current
    val squircleEnabled = isSquircleEnabled()
    val actionShape = remember(
        density,
        squircleEnabled,
        style.actionCornerRadius,
        style.conicCornerWeight,
        style.cornerWeight,
    ) {
        GenericShape { size, _ ->
            addSwipeRevealRect(
                width = size.width,
                height = size.height,
                cornerPx = with(density) { style.actionCornerRadius.toPx() },
                conicCornerWeight = style.conicCornerWeight,
                squircleCornerWeight = style.cornerWeight,
                squircleEnabled = squircleEnabled,
            )
        }
    }
    val actionEnabled = enabled && progress >= 1f && fade > 0f
    val resolvedDescription = if (isConfirmed) {
        action.confirmDescription.ifEmpty { action.description.ifEmpty { action.label.orEmpty() } }
    } else {
        action.description.ifEmpty { action.label.orEmpty() }
    }
    val overlayModifier = if (glassOverlayColor != null) {
        Modifier.clip(actionShape).background(glassOverlayColor.copy(alpha = glassOverlayColor.alpha * alpha))
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .absoluteOffset {
                IntOffset(
                    with(density) { (particle.centerX - particle.width / 2).roundToPx() },
                    with(density) { (particle.centerY - particle.height / 2).roundToPx() },
                )
            }
            .size(particle.width, particle.height)
            .then(overlayModifier)
            .couiTouchMotion(
                enabled = actionEnabled,
                pressScaleEnabled = true,
                consumeDrag = false,
                lightEnabled = true,
                hitTestWhenDisabled = false,
                shape = actionShape,
                lightParams = SpotLightDefaults.opaqueMedium2(isDark),
                motionState = touchState,
            )
            .clickable(
                enabled = actionEnabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = resolvedDescription.takeIf { it.isNotEmpty() },
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val glyph = Modifier
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .blur(with(density) { (NotificationSwipeDefaults.ICON_BLUR_PX * (1f - alpha) / density.density).toDp() })
        val compactGlyph = minOf(particle.width, particle.height) <= 46.dp
        val hasLabel = !action.label.isNullOrEmpty()
        val hasIcon = isConfirmed || action.icon != null || action.useTrashIcon

        if (hasIcon && hasLabel) {
            Column(
                modifier = glyph.padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                SwipeRevealActionGlyph(
                    action = action,
                    isConfirmed = isConfirmed,
                    trashProgress = trashProgress,
                    color = color,
                    description = null,
                    iconSize = 18.dp,
                )
                Text(
                    text = action.label.orEmpty(),
                    color = color,
                    style = COUITheme.textStyles.footnote1.copy(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else if (hasLabel) {
            Text(
                text = action.label.orEmpty(),
                color = color,
                style = COUITheme.textStyles.main.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = glyph.padding(horizontal = 6.dp),
            )
        } else {
            val iconSize = if (compactGlyph) 20.dp else 24.dp
            SwipeRevealActionGlyph(
                action = action,
                isConfirmed = isConfirmed,
                trashProgress = trashProgress,
                color = color,
                description = resolvedDescription,
                iconSize = iconSize,
                modifier = glyph,
            )
        }
    }
}

@Composable
private fun SwipeRevealActionGlyph(
    action: SwipeRevealAction,
    isConfirmed: Boolean,
    trashProgress: Float,
    color: Color,
    description: String?,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    when {
        isConfirmed -> Icon(
            imageVector = SwipeRevealConfirmIcon,
            contentDescription = description,
            tint = color,
            modifier = modifier.size(iconSize),
        )

        action.useTrashIcon -> NotificationTrashIcon(
            progress = trashProgress,
            color = color,
            modifier = modifier.size(if (iconSize <= 20.dp) 22.dp else 28.dp),
            contentDescription = description,
        )

        action.icon != null -> Icon(
            imageVector = action.icon,
            contentDescription = description,
            tint = color,
            modifier = modifier.size(iconSize),
        )
    }
}

/** Checkmark glyph shown on the first tap of a [SwipeRevealAction.requireConfirmation] action. */
private val SwipeRevealConfirmIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SwipeRevealConfirm",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(5.5f, 12.5f)
            lineTo(10f, 17f)
            lineTo(18.5f, 7.5f)
        }
    }.build()
}

internal fun swipeRevealActionProgress(
    distance: Dp,
    index: Int,
    style: SwipeRevealStyle,
): Float {
    val denom = (style.actionWidth + style.followDistance).coerceAtLeast(1.dp)
    return ((distance - style.endPadding - (style.actionWidth + style.actionGap) * index) / denom).coerceIn(0f, 1f)
}

/**
 * The melded silhouettes for a [SwipeRevealBox] of [actionCount] cells: the foreground card first,
 * followed by the primary (rightmost) action cell and any secondary cells to its left.
 */
internal fun swipeRevealParticles(
    width: Dp,
    height: Dp,
    distance: Dp,
    style: SwipeRevealStyle,
    actionRecoils: List<Float> = emptyList(),
    actionFollowsDp: List<Dp>? = null,
    deleteRecoil: Float = 0f,
    settingsRecoil: Float = 0f,
    actionCount: Int = 2,
    overshoot: Dp = 0.dp,
    merge: Float = 0f,
    menuAlpha: Float = 1f,
): List<MetaBallParticle> {
    val cardWidth = (width - distance - overshoot).coerceAtLeast(0.dp)
    val availableHeight = (height - style.actionVerticalInset * 2).coerceAtLeast(0.dp)
    val buttonHeight = if (style.actionMaxHeight != Dp.Unspecified) {
        minOf(style.actionMaxHeight, availableHeight)
    } else {
        availableHeight
    }
    val pitch = style.actionWidth + style.actionGap
    val end = width - style.endPadding
    val centerY = if (style.centerActionsVertically) {
        height / 2
    } else {
        style.actionVerticalInset + buttonHeight / 2
    }
    val card = MetaBallParticle(cardWidth / 2, height / 2, cardWidth, height, style.cornerRadius)
    if (actionCount <= 0) return listOf(card)

    val followSpan = style.actionWidth + style.followDistance
    val followRatio = if (followSpan > 0.dp) style.followDistance.value / followSpan.value else 0f

    fun resolveFollow(index: Int): Dp {
        if (actionFollowsDp != null) return actionFollowsDp.getOrElse(index) { 0.dp }
        val recoil = when {
            actionRecoils.size > index -> actionRecoils[index]
            index == 0 -> deleteRecoil
            index == 1 -> settingsRecoil
            else -> 0f
        }
        // Cap each cell's follow to its own exposure so secondary buttons never jump by 2×/3×.
        val cellExposure = (distance - style.endPadding - pitch * index).coerceIn(0.dp, followSpan)
        return cellExposure * followRatio * (1f - recoil)
    }

    val deleteFollow = resolveFollow(0)
    val deleteWidth = (style.actionWidth + overshoot + if (actionCount > 1) pitch * merge else 0.dp).coerceIn(0.dp, end)
    val delete = MetaBallParticle(
        centerX = end - deleteWidth / 2 - deleteFollow,
        centerY = centerY,
        width = deleteWidth,
        height = buttonHeight,
        cornerRadius = style.actionCornerRadius,
        alpha = menuAlpha,
    )
    val particles = mutableListOf(card, delete)

    for (i in 1 until actionCount) {
        val follow = resolveFollow(i)
        val scale = 1f - (1f - NotificationSwipeDefaults.MERGED_SCALE) * merge.coerceIn(0f, 1f)
        particles.add(
            MetaBallParticle(
                centerX = end - style.actionWidth / 2 - pitch * i - follow - minOf(overshoot, SwipeRevealDefaults.MergeDistance),
                centerY = centerY,
                width = style.actionWidth,
                height = buttonHeight,
                cornerRadius = style.actionCornerRadius,
                scaleX = scale,
                scaleY = scale,
                alpha = menuAlpha,
            ),
        )
    }
    return particles
}
