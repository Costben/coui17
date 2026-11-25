// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
import io.github.suqi8.coui.kmp.blur.BlurDefaults
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.PI
import kotlin.math.cos

/** An action revealed by a left swipe on a [NotificationSwipeBox]. */
@Immutable
@ExperimentalCOUIApi
data class NotificationSwipeAction(
    val icon: ImageVector?,
    val description: String,
    val onClick: () -> Unit,
    val color: Color? = null,
)

/** A saveable, interruptible swipe position for [NotificationSwipeBox]. */
@Stable
@ExperimentalCOUIApi
class NotificationSwipeState(initiallyRevealed: Boolean = false) {
    internal val delegate = SwipeRevealState(initiallyRevealed)

    /** The card's travel in pixels: zero when closed, negative towards the menu and beyond. */
    internal var offsetPx: Float
        get() = delegate.offsetPx
        set(value) {
            delegate.offsetPx = value
        }

    /** How far the card may travel: the menu, or the whole row when a full swipe deletes. */
    internal var maxPx: Float
        get() = delegate.maxPx
        set(value) {
            delegate.maxPx = value
        }

    /** Full row width in pixels, used when a delete action sweeps the row off to the left. */
    internal var sweepMaxPx: Float
        get() = delegate.sweepMaxPx
        set(value) {
            delegate.sweepMaxPx = value
        }

    /** Menu strip width in pixels, published by [NotificationSwipeBox] once it knows its actions. */
    internal var revealPx: Float
        get() = delegate.revealPx
        set(value) {
            delegate.revealPx = value
        }

    /** Whether a released full swipe is carrying the row away. */
    internal var dismissing: Boolean
        get() = delegate.dismissing
        set(value) {
            delegate.dismissing = value
        }

    /** Whether the finger is on the row. */
    internal var dragging: Boolean
        get() = delegate.dragging
        set(value) {
            delegate.dragging = value
        }

    /** The destination the row last settled or is settling on. */
    internal var revealedTarget: Boolean
        get() = delegate.revealedTarget
        set(value) {
            delegate.revealedTarget = value
        }

    internal val draggable: DraggableState get() = delegate.draggable

    /** Current visual reveal fraction; it remains in 0..1 even during spring overshoot. */
    val revealFraction: Float get() = delegate.revealFraction

    /** How far past the menu a full swipe has carried the card, in pixels. */
    internal val overshootPx: Float get() = delegate.overshootPx

    /**
     * Whether the menu is open or opening. While the finger is down this follows the side of the
     * half-way point the card is on; afterwards it is the destination the row settles on.
     */
    val isRevealed: Boolean get() = delegate.isRevealed

    /** Reveals the actions with the native stage-one spring. A new drag cancels this animation. */
    suspend fun reveal() = delegate.reveal()

    /** Returns the notification and its menu to the closed position. */
    suspend fun close() = delegate.close()

    internal suspend fun settle(revealed: Boolean, velocity: Float = 0f) = delegate.settle(revealed, velocity)

    internal suspend fun animateOffsetTo(target: Float, velocity: Float, spec: AnimationSpec<Float>) = delegate.animateOffsetTo(target, velocity, spec)

    internal suspend fun snapTo(target: Float) = delegate.snapTo(target)

    companion object {
        /** Saves the destination rather than an unfinished drag's pixel offset. */
        val Saver = Saver<NotificationSwipeState, Boolean>(
            save = { it.isRevealed },
            restore = { NotificationSwipeState(it) },
        )
    }
}

/** Remembers the menu destination across recreation. */
@Composable
@ExperimentalCOUIApi
fun rememberNotificationSwipeState(initiallyRevealed: Boolean = false): NotificationSwipeState = rememberSaveable(saver = NotificationSwipeState.Saver) { NotificationSwipeState(initiallyRevealed) }

/** Values from the ColorOS 17 notification menu resources and its swipe animation parameters. */
@ExperimentalCOUIApi
object NotificationSwipeDefaults {
    /** notification_menu_icon_with_background_width. */
    val ActionWidth = 60.dp

    /** notification_menu_icon_margin. */
    val ActionGap = 10.dp

    /** notification_menu_icon_margin_extra. */
    val EndPadding = 4.dp

    /** notification_menu_icon_with_background_parent_standard_height. */
    val MinHeight = 84.dp

    /** notification_menu_icon_with_background_standard_height. */
    val ActionHeight = 68.dp

    /** notification_menu_icon_with_background_standard_height_diff. */
    val ActionTop = 8.dp

    /** RadiusParams.CARD_MENU_CONER, shared by the card and menu while melting. */
    val CornerRadius = 16.dp

    /**
     * `notification_corner_radius` (`16dp`), matching the menu cell radius and drawn with
     * ColorOS 17 `CornerType.CONIC` continuous curvature (`weight = 1.1`, `radius * 1.5 = 24dp`
     * Bezier span).
     */
    val CardCornerRadius = 16.dp

    /** `SmoothRoundEx`: ColorOS 17 `CornerParams(CornerType.CONIC, 16.dp, 1.1f)` weight. */
    internal const val CONIC_CORNER_WEIGHT = 1.1f

    /** AnimateParamFour.BlendRange. */
    val BlendRange = 3.5.dp

    /** Both stage-one SettingDistance and DeleteDistance. */
    val FollowDistance = 8.dp

    /** Two menu cells, including the gap before each cell and the outer padding. */
    val RevealWidth: Dp = revealWidth(2)

    /**
     * The menu strip width for [actionCount] cells: every cell brings its own gap, and the outer
     * padding follows the last one.
     */
    fun revealWidth(actionCount: Int): Dp = (ActionWidth + ActionGap) * actionCount + EndPadding

    /**
     * How far past the menu a full swipe stretches the delete cell before it swallows the settings
     * cell, `AnimateParamTwo.TWO_ANIMATE_THRESHOLD` ("0-158.4px (40dp)").
     */
    val MergeDistance = 40.dp

    /**
     * How far a full swipe has to carry the card before a release deletes it: the menu plus
     * [MergeDistance], where the delete cell takes over, the lid opens and the device ticks. Once
     * the tick has been felt a release always deletes.
     */
    fun dismissThreshold(revealWidth: Dp): Dp = revealWidth + MergeDistance

    /**
     * `NotificationMenuRowItemAnimatorStage2.getDeleteDistance()`: the span past [MergeDistance]
     * over which the bin's lid opens, `dp2px(12.8f)` in SystemUI.
     */
    val LidDistance = 12.8.dp

    /**
     * `NotificationMenuRowItemAnimatorStage1`: a menu item's extra travel is the drag times
     * `followDistance / (iconWidth + followDistance)`, not a fixed distance.
     */
    val FollowRatio: Float = FollowDistance.value / (ActionWidth + FollowDistance).value

    /**
     * Card snap-back spring, SystemUI `NotificationSwipeHelper.mSnapBackSpringConfig`:
     * `SpringConfig(stiffness = 200f, dampingRatio = 0.75f)`.
     */
    val SettleSpring: SpringSpec<Float> = SpringSpec(dampingRatio = 0.75f, stiffness = 200f)

    /**
     * Menu action follow-recoil spring, SystemUI `NotificationMenuRowItemAnimatorStage1`
     * `AnimateParamOne` (`response = 0.55f`, `bounce = 0.4f`).
     */
    internal val MenuRecoilSpring: SpringSpec<Float> = folmeSpring(damping = 0.6f, response = 0.55f)

    /**
     * The delete cell's merge and the settings cell's collapse.
     *
     * `AnimateParamTwo` is response 0.3 / bounce 0.5, but that overshoot lands on a cell the icon
     * sits in the middle of, so the bin jolts sideways by half the overshoot right as the lid
     * opens. Critically damped keeps the timing and drops the jolt.
     */
    val MergeSpring: SpringSpec<Float> = folmeSpring(damping = 1f, response = 0.3f)

    /** AnimateParamThree, the delete cell's fade once the row is thrown away: response 0.3, bounce 0.5. */
    val DismissFadeSpring: SpringSpec<Float> = folmeSpring(damping = 0.5f, response = 0.3f)

    /**
     * SwipeHelper's dismiss: a plain release sweeps the rest of the row in 200ms, speeding up all
     * the way out; a fling keeps the finger's speed, `FlingAnimationUtils.applyDismissing`, and is
     * never slower than the plain sweep. Either way the row is gone the moment it leaves.
     */
    internal const val SWEEP_MILLIS = 200

    /** AnimateParamThree.LOTTIE_DURATION, the lid closing as the row goes. */
    internal const val LID_CLOSE_MILLIS = 200

    /** The lid's close, Android's AccelerateDecelerateInterpolator. */
    internal val LidCloseEasing = Easing { (cos((it + 1f) * PI.toFloat()) / 2f) + 0.5f }

    /** AnchoredDraggable's default fling threshold. */
    internal val FlingVelocity = 125.dp

    internal const val FADE_START = 0.6f

    /** Stage1's `updateViewBlur`: a `RenderEffect` radius of 15 **pixels**, not dp. */
    internal const val ICON_BLUR_PX = 15f

    /** The settings cell keeps a fifth of its size while the delete cell swallows it. */
    internal const val MERGED_SCALE = 0.2f

    /** `QSBlurConfigProvider.getDefaultBlurRadius()` = 250px. */
    internal const val NATIVE_BLUR_RADIUS_PX = 250f

    /** `NotificationPlatFormBlurParamsManager` stroke line thickness (`near` `2 + 6 = 8px` at 3x density). */
    internal val RimLineWidth = (8f / 3f).dp

    /** `notification_material_blur_bg` fallback tint on light theme (`#CCFFFFFF`). */
    internal val ContainerColorLight = Color(0xCCFFFFFF)

    /** `notification_material_blur_bg` fallback tint on dark theme (`#CC333333`). */
    internal val ContainerColorDark = Color(0xCC333333)

    /**
     * ColorOS 17 light notification card blur tint layers
     * (`NotificationBlurMixColorParams.NormalCardMixedColorsLight`): `Luminosity` `#19404040`,
     * then `ColorDodge` `#4D737373`.
     */
    internal val BlurColorsLight: List<BlendColorEntry> = listOf(
        BlendColorEntry(color = Color(0x19404040), mode = BlurBlendMode.Luminosity),
        BlendColorEntry(color = Color(0x4D737373), mode = BlurBlendMode.ColorDodge),
    )

    /**
     * ColorOS 17 dark notification card blur tint layers
     * (`NotificationBlurMixColorParams.NormalCardMixedColorsDark`): `Luminosity` `#59404040`,
     * then `ColorDodge` `#66737373`.
     */
    internal val BlurColorsDark: List<BlendColorEntry> = listOf(
        BlendColorEntry(color = Color(0x59404040), mode = BlurBlendMode.Luminosity),
        BlendColorEntry(color = Color(0x66737373), mode = BlurBlendMode.ColorDodge),
    )

    /** The notification card blur tint layers for [isDark]. */
    internal fun blurColors(isDark: Boolean): List<BlendColorEntry> = if (isDark) BlurColorsDark else BlurColorsLight

    /** [NATIVE_BLUR_RADIUS_PX] converted to dp and clamped to [BlurDefaults.MaxBlurRadius]. */
    @Composable
    internal fun blurRadius(): Float {
        val density = LocalDensity.current.density
        return (NATIVE_BLUR_RADIUS_PX / density).coerceIn(0f, BlurDefaults.MaxBlurRadius)
    }

    internal fun containerColor(isDark: Boolean): Color = if (isDark) ContainerColorDark else ContainerColorLight

    internal fun edgeStyle(isDark: Boolean): GradientStrokeStyle = if (isDark) GradientStrokeStyle.NOTIFICATION_DARK else GradientStrokeStyle.NOTIFICATION_LIGHT
}

/**
 * A notification that reveals settings and delete buttons on a physical left swipe.
 *
 * Built on [SwipeRevealBox] with [SwipeRevealDefaults.notificationStyle]. For standard card lists
 * without notification-specific geometry or glass material, prefer [SwipeRevealBox] directly, or
 * detach the notification traits here via [cornerRadius], [actionCornerRadius], [enableBlur],
 * [enableRim], and [useConicCorner].
 *
 * @param onSettings Invoked by the revealed settings button or its accessibility action; `null`
 *   omits the settings button entirely.
 * @param onDismiss Invoked by the revealed delete button, its accessibility action, or a released
 *   full swipe when [dismissOnFullSwipe] is set.
 * @param settingsIcon The settings button image; `null` omits the settings button entirely.
 * @param dismissIcon The delete button image; `null` draws the shade's animated
 *   [NotificationTrashIcon].
 * @param modifier Modifier applied to the entire notification and menu.
 * @param state Controls the reveal destination and observes the visual progress.
 * @param enabled Whether dragging and actions are enabled.
 * @param minHeight Minimum card height; set to zero for compact, content-sized notifications.
 * @param containerColor Opaque/custom tint shared by the card and action cells; defaults to
 *   [Color.Unspecified], which applies the SystemUI light/dark notification blur mix layers over
 *   [io.github.suqi8.coui.kmp.blur.LocalBackdrop] and `#CCFFFFFF`/`#CC333333` when blurred backdrop is absent.
 * @param actionColor The menu icon tint.
 * @param rimColor The light traced along the merged outline; [Color.Unspecified] uses ColorOS 17's
 *   light/dark `NotificationStrokeLine*Group` edge highlight.
 * @param settingsDescription Localized settings accessibility label.
 * @param dismissDescription Localized delete accessibility label.
 * @param dismissOnFullSwipe Whether the card may be dragged past the menu and thrown away.
 * @param cornerRadius Card corner radius; defaults to [NotificationSwipeDefaults.CardCornerRadius].
 * @param actionCornerRadius Action cell corner radius; defaults to [cornerRadius].
 * @param enableBlur Whether to sample [io.github.suqi8.coui.kmp.blur.LocalBackdrop] with the
 *   SystemUI glass blur recipe. Set `false` to detach blur and render an opaque fill.
 * @param enableRim Whether to draw the glass edge highlight rim. Set `false` to detach the rim.
 * @param useConicCorner Whether to draw ColorOS 17 `CornerType.CONIC` continuous curvature (`1.5×`
 *   Bezier span). Set `false` to use standard COUI [CardDefaults.Weight] Lp squircle corners.
 * @param content The notification's foreground, measured at full width throughout the gesture.
 */
@Composable
@ExperimentalCOUIApi
fun NotificationSwipeBox(
    onSettings: (() -> Unit)?,
    onDismiss: () -> Unit,
    settingsIcon: ImageVector?,
    dismissIcon: ImageVector?,
    modifier: Modifier = Modifier,
    state: NotificationSwipeState = rememberNotificationSwipeState(),
    enabled: Boolean = true,
    minHeight: Dp = NotificationSwipeDefaults.MinHeight,
    containerColor: Color = Color.Unspecified,
    actionColor: Color = COUITheme.colorScheme.onSurface,
    rimColor: Color = Color.Unspecified,
    settingsDescription: String = "Settings",
    dismissDescription: String = "Delete",
    dismissOnFullSwipe: Boolean = false,
    cornerRadius: Dp = NotificationSwipeDefaults.CardCornerRadius,
    actionCornerRadius: Dp = cornerRadius,
    enableBlur: Boolean = true,
    enableRim: Boolean = true,
    useConicCorner: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val actions = remember(onSettings, onDismiss, settingsIcon, dismissIcon, settingsDescription, dismissDescription) {
        buildList {
            if (onSettings != null && settingsIcon != null) {
                add(SwipeRevealAction(settingsIcon, settingsDescription, onSettings))
            }
            add(SwipeRevealAction(dismissIcon, dismissDescription, onDismiss))
        }
    }
    val resolvedContainerColor = if (containerColor == Color.Unspecified && !enableBlur) {
        COUITheme.colorScheme.surfaceContainer
    } else {
        containerColor
    }
    val style = SwipeRevealDefaults.notificationStyle(
        cornerRadius = cornerRadius,
        actionCornerRadius = actionCornerRadius,
        cornerWeight = CardDefaults.Weight,
        conicCornerWeight = if (useConicCorner) NotificationSwipeDefaults.CONIC_CORNER_WEIGHT else 0f,
        containerColor = resolvedContainerColor,
        actionColor = actionColor,
        enableBlur = enableBlur,
        enableRim = enableRim,
        rimColor = rimColor,
        minHeight = minHeight,
    )
    SwipeRevealBoxImpl(
        actions = actions,
        modifier = modifier,
        state = state.delegate,
        enabled = enabled,
        style = style,
        dismissOnFullSwipe = dismissOnFullSwipe,
        dismissPrimaryOnClick = true,
        content = content,
    )
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that exposes detachable cornerRadius, actionCornerRadius, enableBlur, enableRim, and useConicCorner parameters.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
@ExperimentalCOUIApi
fun NotificationSwipeBox(
    onSettings: (() -> Unit)?,
    onDismiss: () -> Unit,
    settingsIcon: ImageVector?,
    dismissIcon: ImageVector?,
    modifier: Modifier = Modifier,
    state: NotificationSwipeState = rememberNotificationSwipeState(),
    enabled: Boolean = true,
    minHeight: Dp = NotificationSwipeDefaults.MinHeight,
    containerColor: Color = Color.Unspecified,
    actionColor: Color = COUITheme.colorScheme.onSurface,
    rimColor: Color = Color.Unspecified,
    settingsDescription: String = "Settings",
    dismissDescription: String = "Delete",
    dismissOnFullSwipe: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
): Unit = NotificationSwipeBox(
    onSettings = onSettings,
    onDismiss = onDismiss,
    settingsIcon = settingsIcon,
    dismissIcon = dismissIcon,
    modifier = modifier,
    state = state,
    enabled = enabled,
    minHeight = minHeight,
    containerColor = containerColor,
    actionColor = actionColor,
    rimColor = rimColor,
    settingsDescription = settingsDescription,
    dismissDescription = dismissDescription,
    dismissOnFullSwipe = dismissOnFullSwipe,
    cornerRadius = NotificationSwipeDefaults.CardCornerRadius,
    actionCornerRadius = NotificationSwipeDefaults.CardCornerRadius,
    enableBlur = true,
    enableRim = true,
    useConicCorner = true,
    content = content,
)

/**
 * A notification that reveals up to 3 actions on a physical left swipe.
 *
 * Supply actions in visual left-to-right order: the last action sits at the far outer edge and is revealed first.
 */
@Composable
@ExperimentalCOUIApi
fun NotificationSwipeBox(
    actions: List<NotificationSwipeAction>,
    modifier: Modifier = Modifier,
    state: NotificationSwipeState = rememberNotificationSwipeState(),
    enabled: Boolean = true,
    minHeight: Dp = NotificationSwipeDefaults.MinHeight,
    containerColor: Color = Color.Unspecified,
    actionColor: Color = COUITheme.colorScheme.onSurface,
    rimColor: Color = Color.Unspecified,
    dismissOnFullSwipe: Boolean = false,
    cornerRadius: Dp = NotificationSwipeDefaults.CardCornerRadius,
    actionCornerRadius: Dp = cornerRadius,
    enableBlur: Boolean = true,
    enableRim: Boolean = true,
    useConicCorner: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val revealActions = remember(actions) {
        actions.map { SwipeRevealAction(it.icon, it.description, it.onClick, it.color) }
    }
    val resolvedContainerColor = if (containerColor == Color.Unspecified && !enableBlur) {
        COUITheme.colorScheme.surfaceContainer
    } else {
        containerColor
    }
    val style = SwipeRevealDefaults.notificationStyle(
        cornerRadius = cornerRadius,
        actionCornerRadius = actionCornerRadius,
        cornerWeight = CardDefaults.Weight,
        conicCornerWeight = if (useConicCorner) NotificationSwipeDefaults.CONIC_CORNER_WEIGHT else 0f,
        containerColor = resolvedContainerColor,
        actionColor = actionColor,
        enableBlur = enableBlur,
        enableRim = enableRim,
        rimColor = rimColor,
        minHeight = minHeight,
    )
    SwipeRevealBoxImpl(
        actions = revealActions,
        modifier = modifier,
        state = state.delegate,
        enabled = enabled,
        style = style,
        dismissOnFullSwipe = dismissOnFullSwipe,
        dismissPrimaryOnClick = dismissOnFullSwipe,
        content = content,
    )
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that exposes detachable cornerRadius, actionCornerRadius, enableBlur, enableRim, and useConicCorner parameters.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
@ExperimentalCOUIApi
fun NotificationSwipeBox(
    actions: List<NotificationSwipeAction>,
    modifier: Modifier = Modifier,
    state: NotificationSwipeState = rememberNotificationSwipeState(),
    enabled: Boolean = true,
    minHeight: Dp = NotificationSwipeDefaults.MinHeight,
    containerColor: Color = Color.Unspecified,
    actionColor: Color = COUITheme.colorScheme.onSurface,
    rimColor: Color = Color.Unspecified,
    dismissOnFullSwipe: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
): Unit = NotificationSwipeBox(
    actions = actions,
    modifier = modifier,
    state = state,
    enabled = enabled,
    minHeight = minHeight,
    containerColor = containerColor,
    actionColor = actionColor,
    rimColor = rimColor,
    dismissOnFullSwipe = dismissOnFullSwipe,
    cornerRadius = NotificationSwipeDefaults.CardCornerRadius,
    actionCornerRadius = NotificationSwipeDefaults.CardCornerRadius,
    enableBlur = true,
    enableRim = true,
    useConicCorner = true,
    content = content,
)

private val NotificationParticleStyle = SwipeRevealStyle(
    cornerRadius = NotificationSwipeDefaults.CardCornerRadius,
    actionCornerRadius = NotificationSwipeDefaults.CornerRadius,
    cornerWeight = CardDefaults.Weight,
    conicCornerWeight = NotificationSwipeDefaults.CONIC_CORNER_WEIGHT,
    minHeight = NotificationSwipeDefaults.MinHeight,
    actionWidth = NotificationSwipeDefaults.ActionWidth,
    actionMaxHeight = NotificationSwipeDefaults.ActionHeight,
    actionVerticalInset = NotificationSwipeDefaults.ActionTop,
    centerActionsVertically = false,
    actionGap = NotificationSwipeDefaults.ActionGap,
    endPadding = NotificationSwipeDefaults.EndPadding,
    blendRange = NotificationSwipeDefaults.BlendRange,
    followDistance = NotificationSwipeDefaults.FollowDistance,
    settleSpring = NotificationSwipeDefaults.SettleSpring,
    recoilSpring = NotificationSwipeDefaults.MenuRecoilSpring,
)

internal fun notificationActionProgress(distance: Dp, index: Int): Float = swipeRevealActionProgress(distance, index, NotificationParticleStyle)

/**
 * The melded silhouettes for a menu of [actionCount] cells: the card first, then the delete cell
 * against the outer padding and, when there is one, the settings cell after it.
 */
internal fun notificationSwipeParticles(
    width: Dp,
    height: Dp,
    distance: Dp,
    actionRecoils: List<Float> = emptyList(),
    deleteRecoil: Float = 0f,
    settingsRecoil: Float = 0f,
    actionCount: Int = 2,
    overshoot: Dp = 0.dp,
    merge: Float = 0f,
    menuAlpha: Float = 1f,
): List<MetaBallParticle> = swipeRevealParticles(
    width = width,
    height = height,
    distance = distance,
    style = NotificationParticleStyle,
    actionRecoils = actionRecoils,
    deleteRecoil = deleteRecoil,
    settingsRecoil = settingsRecoil,
    actionCount = actionCount,
    overshoot = overshoot,
    merge = merge,
    menuAlpha = menuAlpha,
)
