// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.squircle.addSquircleRect
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos

/** A saveable, interruptible swipe position for [NotificationSwipeBox]. */
@Stable
@ExperimentalCOUIApi
class NotificationSwipeState(initiallyRevealed: Boolean = false) {
    /** The card's travel in pixels: zero when closed, negative towards the menu and beyond. */
    internal var offsetPx by mutableFloatStateOf(0f)

    /** How far the card may travel: the menu, or the whole row when a full swipe deletes. */
    internal var maxPx by mutableFloatStateOf(0f)

    /** Menu strip width in pixels, published by [NotificationSwipeBox] once it knows its actions. */
    internal var revealPx by mutableFloatStateOf(0f)

    /** Whether a released full swipe is carrying the row away. */
    internal var dismissing by mutableStateOf(false)

    /** Whether the finger is on the row. */
    internal var dragging by mutableStateOf(false)

    /** The destination the row last settled or is settling on. */
    internal var revealedTarget by mutableStateOf(initiallyRevealed)

    internal val draggable = DraggableState { delta -> offsetPx = (offsetPx + delta).coerceIn(-maxPx, 0f) }

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

    /** Reveals the actions with the native stage-one spring. A new drag cancels this animation. */
    suspend fun reveal() = settle(true)

    /** Returns the notification and its menu to the closed position. */
    suspend fun close() = settle(false)

    internal suspend fun settle(revealed: Boolean, velocity: Float = 0f) {
        revealedTarget = revealed
        animateOffsetTo(if (revealed) -revealPx else 0f, velocity, NotificationSwipeDefaults.SettleSpring)
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
     * The card's own corner.
     *
     * `notification_corner_radius` is 16dp, but ColorOS 17 draws that corner with continuous
     * curvature: the outline leaves the card's edge 79px (20.4dp) below the top at 620dpi, where a
     * circular 16dp corner would leave 49px. Compose only draws circular corners, so 20dp follows
     * the device outline more closely than the 16dp token. The menu cells keep the circular 16dp
     * of `notif_menu_icon_background`.
     */
    val CardCornerRadius = 20.dp

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

    /** AnimateParamOne: response 0.55, bounce 0.4. */
    val SettleSpring: SpringSpec<Float> = folmeSpring(damping = 0.6f, response = 0.55f)

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

    /** The lid's open: fast enough to read as one move, slow enough not to snap. */
    internal const val LID_OPEN_MILLIS = 150

    /** The lid's close, Android's AccelerateDecelerateInterpolator. */
    internal val LidCloseEasing = Easing { (cos((it + 1f) * PI.toFloat()) / 2f) + 0.5f }

    /** AnchoredDraggable's default fling threshold. */
    internal val FlingVelocity = 125.dp

    internal const val FADE_START = 0.6f
    internal const val ICON_BLUR = 15f

    /** The settings cell keeps a fifth of its size while the delete cell swallows it. */
    internal const val MERGED_SCALE = 0.2f
}

/**
 * A notification that reveals settings and delete buttons on a physical left swipe.
 *
 * The visible card and both menu backgrounds share one [MetaBallSurface]. Their signed-distance
 * fields merge while touching and split as the card retreats. Menu icons follow by 8dp, fade in
 * over the last 40% of each cell's exposure, and spring back when that cell clears the card.
 * Content keeps its original width and slides left with the finger inside the card mask, whose
 * end edge retreats.
 *
 * Supply foreground content without its own opaque background so it does not cover the melt.
 * A [io.github.suqi8.coui.kmp.blur.LocalBackdrop] gives the chain the existing glass treatment.
 * Below Android 13 the existing surface draws separate opaque shapes without the shader bridge.
 *
 * Pass `null` for [onSettings] and [settingsIcon] to draw a delete-only notification: the menu is
 * then one cell wide and the delete button sits against the outer padding.
 *
 * With [dismissOnFullSwipe] the card keeps following the finger past the menu, the way the shade
 * does. The delete cell stretches into the space it leaves while the settings cell moves with it;
 * [NotificationSwipeDefaults.MergeDistance] past the menu the delete cell springs over the
 * settings cell, which shrinks into it, and the device ticks; it ticks again if the
 * finger backs out. That point is [NotificationSwipeDefaults.dismissThreshold]: releasing beyond
 * it sweeps the row away and calls [onDismiss] as soon as it is gone, releasing anywhere short of
 * it springs back to the menu, and nothing is deleted while the finger is still down. The built-in
 * [NotificationTrashIcon] lifts its lid once, at that same point, and lays it back down as the row
 * goes.
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
 * @param containerColor The material shared by the card and both buttons.
 * @param actionColor The menu icon tint.
 * @param rimColor The light traced along the merged outline. A notification's top and bottom edges
 *   measure `#4E4E4E` over a `#2D2D2D` body, which needs opaque white.
 * @param settingsDescription Localized settings accessibility label.
 * @param dismissDescription Localized delete accessibility label.
 * @param dismissOnFullSwipe Whether the card may be dragged past the menu and thrown away.
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
    containerColor: Color = COUITheme.colorScheme.bar,
    actionColor: Color = COUITheme.colorScheme.onSurface,
    rimColor: Color = Color.White.copy(alpha = 0.55f),
    settingsDescription: String = "Settings",
    dismissDescription: String = "Delete",
    dismissOnFullSwipe: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }
    val actions = if (onSettings != null && settingsIcon != null) 2 else 1
    val revealWidth = NotificationSwipeDefaults.revealWidth(actions)
    val revealWidthPx = with(density) { revealWidth.toPx() }
    val widthPx = measuredSize.width.toFloat()
    val width = with(density) { measuredSize.width.toDp() }
    val height = with(density) { measuredSize.height.toDp() }
    val maxPx = if (dismissOnFullSwipe) maxOf(widthPx, revealWidthPx) else revealWidthPx
    val dismissThresholdPx = with(density) { NotificationSwipeDefaults.dismissThreshold(revealWidth).toPx() }
    val flingVelocityPx = with(density) { NotificationSwipeDefaults.FlingVelocity.toPx() }

    // A revealed or restored state learns where "revealed" is only once the menu is measured.
    LaunchedEffect(state, revealWidthPx, maxPx) {
        state.revealPx = revealWidthPx
        state.maxPx = maxPx
        if (!state.dragging && !state.dismissing) state.snapTo(if (state.revealedTarget) -revealWidthPx else 0f)
    }

    val currentOnDismiss by rememberUpdatedState(onDismiss)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { measuredSize = it }
            .draggable(
                state = state.draggable,
                orientation = Orientation.Horizontal,
                // The native left-swipe affordance has a physical direction even in RTL content.
                reverseDirection = false,
                enabled = enabled && !state.dismissing && width > revealWidth,
                onDragStarted = { state.dragging = true },
                onDragStopped = { velocity ->
                    state.dragging = false
                    val pulled = -state.offsetPx
                    if (dismissOnFullSwipe && pulled >= dismissThresholdPx) {
                        // Deciding on the release alone: a held drag past the threshold must still be
                        // able to come back, and velocity never deletes a row the finger left short.
                        state.dismissing = true
                        state.revealedTarget = false
                        try {
                            // A tween rather than a spring: there is no settling tail to wait out, and
                            // a fling's speed is not spent overshooting the clamped end.
                            val fling = velocity < -flingVelocityPx
                            val remaining = state.maxPx + state.offsetPx
                            val millis = if (fling) {
                                (remaining * 1000f / -velocity).toInt().coerceAtMost(NotificationSwipeDefaults.SWEEP_MILLIS)
                            } else {
                                NotificationSwipeDefaults.SWEEP_MILLIS
                            }
                            state.animateOffsetTo(
                                target = -state.maxPx,
                                velocity = 0f,
                                spec = tween(millis, easing = if (fling) LinearEasing else FastOutLinearInEasing),
                            )
                            currentOnDismiss()
                            state.snapTo(0f)
                        } finally {
                            state.dismissing = false
                        }
                    } else {
                        val reveal = when {
                            velocity < -flingVelocityPx -> true
                            velocity > flingVelocityPx -> false
                            else -> pulled > state.revealPx / 2
                        }
                        state.settle(reveal, velocity)
                    }
                },
            )
            .semantics {
                customActions = if (enabled) {
                    buildList {
                        if (onSettings != null) {
                            add(
                                CustomAccessibilityAction(settingsDescription) {
                                    onSettings()
                                    true
                                },
                            )
                        }
                        add(
                            CustomAccessibilityAction(dismissDescription) {
                                onDismiss()
                                true
                            },
                        )
                    }
                } else {
                    emptyList()
                }
            },
    ) {
        if (measuredSize != IntSize.Zero) {
            NotificationSwipeMenu(
                state = state,
                width = width,
                height = height,
                actions = actions,
                containerColor = containerColor,
                actionColor = actionColor,
                rimColor = rimColor,
                settingsIcon = settingsIcon,
                dismissIcon = dismissIcon,
                settingsDescription = settingsDescription,
                dismissDescription = dismissDescription,
                enabled = enabled,
                onSettings = onSettings,
                onDismiss = onDismiss,
            )
        }
        NotificationSwipeCard(state = state, minHeight = minHeight, content = content)
    }
}

/**
 * The merged card and menu material. It reads the swipe offset while it composes, so it is the only
 * part of [NotificationSwipeBox] that re-runs on every drag frame.
 */
@Composable
private fun BoxScope.NotificationSwipeMenu(
    state: NotificationSwipeState,
    width: Dp,
    height: Dp,
    actions: Int,
    containerColor: Color,
    actionColor: Color,
    rimColor: Color,
    settingsIcon: ImageVector?,
    dismissIcon: ImageVector?,
    settingsDescription: String,
    dismissDescription: String,
    enabled: Boolean,
    onSettings: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val revealWidth = NotificationSwipeDefaults.revealWidth(actions)
    val distance = revealWidth * state.revealFraction
    val overshoot = with(density) { state.overshootPx.toDp() }
    val dismissing = state.dismissing
    // Stage two: past the merge distance the delete cell springs across the settings cell, and
    // springs back off it when the card returns inside that distance.
    val merge by animateFloatAsState(
        targetValue = if (actions > 1 && (dismissing || overshoot >= NotificationSwipeDefaults.MergeDistance)) 1f else 0f,
        animationSpec = NotificationSwipeDefaults.MergeSpring,
    )
    // The dismiss threshold: the device ticks as the finger crosses it either way, as
    // NotificationMenuRow.onTouchMove does. The tick is the only emphasis; the bin itself just
    // lifts its lid, so nothing is left mid-animation when the finger releases.
    val armed = state.dragging && overshoot >= NotificationSwipeDefaults.MergeDistance
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(armed) {
        if (armed) {
            haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        } else if (state.dragging) {
            haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
    }
    val lidClose by animateFloatAsState(
        targetValue = if (dismissing) 1f else 0f,
        animationSpec = tween(NotificationSwipeDefaults.LID_CLOSE_MILLIS, easing = NotificationSwipeDefaults.LidCloseEasing),
    )
    // The lid opens once, as soon as the finger is past the merge, and stays open; it does not
    // track the finger, so a slow drag cannot make it flap open and shut a pixel at a time.
    val lidLatch by animateFloatAsState(
        targetValue = if (dismissing || overshoot >= NotificationSwipeDefaults.MergeDistance) 1f else 0f,
        animationSpec = tween(NotificationSwipeDefaults.LID_OPEN_MILLIS, easing = NotificationSwipeDefaults.LidCloseEasing),
    )
    // The lottie's first half is the lift; its second half lays the lid back down as the row goes.
    val lidProgress = if (dismissing) {
        0.5f + 0.5f * lidClose
    } else {
        lidLatch / 2f
    }
    val deleteFade by animateFloatAsState(
        targetValue = if (dismissing) 0f else 1f,
        animationSpec = NotificationSwipeDefaults.DismissFadeSpring,
    )
    val deleteProgress = notificationActionProgress(distance, 0)
    val settingsProgress = notificationActionProgress(distance, 1)
    val deleteRecoil by animateFloatAsState(
        targetValue = if (deleteProgress >= 1f) 1f else 0f,
        animationSpec = NotificationSwipeDefaults.SettleSpring,
    )
    val settingsRecoil by animateFloatAsState(
        targetValue = if (settingsProgress >= 1f) 1f else 0f,
        animationSpec = NotificationSwipeDefaults.SettleSpring,
    )
    val particles = notificationSwipeParticles(
        width = width,
        height = height,
        distance = distance,
        deleteRecoil = deleteRecoil,
        settingsRecoil = settingsRecoil,
        actionCount = actions,
        overshoot = overshoot,
        merge = merge,
        menuAlpha = deleteFade.coerceIn(0f, 1f),
    )
    // The menu sheet covers exactly the strip the card has vacated.
    val menuShape = remember(state) {
        GenericShape { size, _ ->
            addRect(Rect((size.width + state.offsetPx).coerceAtLeast(0f), 0f, size.width, size.height))
        }
    }

    MetaBallSurface(
        particles = particles,
        modifier = Modifier.matchParentSize(),
        color = containerColor,
        extension = NotificationSwipeDefaults.BlendRange,
        extensionFactorX = 1.2f,
        extensionFactorY = 2f,
        rimColor = rimColor,
        // The shade lights the lower edge of a card as brightly as the upper one.
        rimBottomScale = 2f,
    )
    Box(Modifier.matchParentSize().clip(menuShape), contentAlignment = AbsoluteAlignment.TopLeft) {
        NotificationMenuAction(
            particle = particles[1],
            progress = deleteProgress,
            icon = dismissIcon,
            description = dismissDescription,
            color = actionColor,
            enabled = enabled && !dismissing,
            fade = deleteFade,
            trashProgress = lidProgress,
            onClick = {
                scope.launch { state.close() }
                onDismiss()
            },
        )
        if (actions > 1 && settingsIcon != null && onSettings != null) {
            NotificationMenuAction(
                particle = particles[2],
                progress = settingsProgress,
                icon = settingsIcon,
                description = settingsDescription,
                color = actionColor,
                enabled = enabled && merge == 0f,
                fade = 1f - merge,
                scale = particles[2].scaleX,
                onClick = {
                    scope.launch { state.close() }
                    onSettings()
                },
            )
        }
    }
}

/**
 * The notification card. The swipe offset is read only inside the clip shape and layer lambdas, so
 * a drag re-clips the card and moves its content without recomposing either. The card keeps its
 * left edge while its right edge retreats ahead of the menu, and the content slides left with the
 * finger inside that mask, as the shade's notification text does.
 */
@Composable
private fun BoxScope.NotificationSwipeCard(
    state: NotificationSwipeState,
    minHeight: Dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val squircleEnabled = isSquircleEnabled()
    val cardShape = remember(density, state, squircleEnabled) {
        GenericShape { size, _ ->
            val visibleWidth = (size.width + state.offsetPx).coerceAtLeast(0f)
            addSquircleRect(
                width = visibleWidth,
                height = size.height,
                cornerRadius = with(density) { NotificationSwipeDefaults.CardCornerRadius.toPx() },
                squircleEnabled = squircleEnabled,
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(cardShape)
            // Inside the clip layer, so the mask stays put while the content travels.
            .graphicsLayer { translationX = state.offsetPx },
        content = content,
    )
}

@Composable
private fun NotificationMenuAction(
    particle: MetaBallParticle,
    progress: Float,
    icon: ImageVector?,
    description: String,
    color: Color,
    enabled: Boolean,
    fade: Float = 1f,
    scale: Float = 1f,
    trashProgress: Float = 0f,
    onClick: () -> Unit,
) {
    val alpha = (((progress - NotificationSwipeDefaults.FADE_START) / (1f - NotificationSwipeDefaults.FADE_START)).coerceIn(0f, 1f) * fade).coerceIn(0f, 1f)
    // An entirely covered button is absent from both hit testing and the accessibility tree.
    if (alpha <= 0f) return
    val density = LocalDensity.current
    IconButton(
        onClick = onClick,
        enabled = enabled && progress >= 1f && fade > 0f,
        modifier = Modifier
            .absoluteOffset {
                IntOffset(
                    with(density) { (particle.centerX - particle.width / 2).roundToPx() },
                    with(density) { (particle.centerY - particle.height / 2).roundToPx() },
                )
            }
            .size(particle.width, particle.height),
        cornerRadius = NotificationSwipeDefaults.CornerRadius,
    ) {
        val glyph = Modifier
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .blur(with(density) { (NotificationSwipeDefaults.ICON_BLUR * (1f - alpha)).toDp() })
        if (icon == null) {
            NotificationTrashIcon(progress = trashProgress, color = color, modifier = glyph, contentDescription = description)
        } else {
            Icon(imageVector = icon, contentDescription = description, tint = color, modifier = glyph.size(24.dp))
        }
    }
}

internal fun notificationActionProgress(distance: Dp, index: Int): Float = ((distance - NotificationSwipeDefaults.EndPadding - (NotificationSwipeDefaults.ActionWidth + NotificationSwipeDefaults.ActionGap) * index) / (NotificationSwipeDefaults.ActionWidth + NotificationSwipeDefaults.FollowDistance)).coerceIn(0f, 1f)

/**
 * The melded silhouettes for a menu of [actionCount] cells: the card first, then the delete cell
 * against the outer padding and, when there is one, the settings cell after it.
 *
 * Past the menu, [overshoot] widens the delete cell towards the card and carries the settings cell
 * along, up to the merge distance. [merge] then grows the delete cell by one more cell pitch over
 * the settings cell, which shrinks to a fifth of its size inside it, so the delete cell always ends
 * one gap short of the card.
 */
internal fun notificationSwipeParticles(
    width: Dp,
    height: Dp,
    distance: Dp,
    deleteRecoil: Float = 0f,
    settingsRecoil: Float = 0f,
    actionCount: Int = 2,
    overshoot: Dp = 0.dp,
    merge: Float = 0f,
    menuAlpha: Float = 1f,
): List<MetaBallParticle> {
    val cardWidth = (width - distance - overshoot).coerceAtLeast(0.dp)
    val buttonHeight = minOf(NotificationSwipeDefaults.ActionHeight, (height - NotificationSwipeDefaults.ActionTop * 2).coerceAtLeast(0.dp))
    val pitch = NotificationSwipeDefaults.ActionWidth + NotificationSwipeDefaults.ActionGap
    val end = width - NotificationSwipeDefaults.EndPadding
    val centerY = NotificationSwipeDefaults.ActionTop + buttonHeight / 2
    val deleteFollow = NotificationSwipeDefaults.FollowDistance * notificationActionProgress(distance, 0) * (1f - deleteRecoil)
    val deleteWidth = (NotificationSwipeDefaults.ActionWidth + overshoot + if (actionCount > 1) pitch * merge else 0.dp).coerceIn(0.dp, end)
    val delete = MetaBallParticle(
        centerX = end - deleteWidth / 2 - deleteFollow,
        centerY = centerY,
        width = deleteWidth,
        height = buttonHeight,
        cornerRadius = NotificationSwipeDefaults.CornerRadius,
        alpha = menuAlpha,
    )
    val card = MetaBallParticle(cardWidth / 2, height / 2, cardWidth, height, NotificationSwipeDefaults.CardCornerRadius)
    if (actionCount < 2) return listOf(card, delete)
    val settingsFollow = NotificationSwipeDefaults.FollowDistance * notificationActionProgress(distance, 1) * (1f - settingsRecoil)
    val settingsScale = 1f - (1f - NotificationSwipeDefaults.MERGED_SCALE) * merge.coerceIn(0f, 1f)
    val settings = MetaBallParticle(
        centerX = end - NotificationSwipeDefaults.ActionWidth / 2 - pitch - settingsFollow - minOf(overshoot, NotificationSwipeDefaults.MergeDistance),
        centerY = centerY,
        width = NotificationSwipeDefaults.ActionWidth,
        height = buttonHeight,
        cornerRadius = NotificationSwipeDefaults.CornerRadius,
        scaleX = settingsScale,
        scaleY = settingsScale,
        alpha = menuAlpha,
    )
    return listOf(card, delete, settings)
}
