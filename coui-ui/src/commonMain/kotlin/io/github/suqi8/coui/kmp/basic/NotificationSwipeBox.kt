// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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

/** A saveable, interruptible pair of closed/revealed anchors for [NotificationSwipeBox]. */
@Stable
@ExperimentalCOUIApi
class NotificationSwipeState(initiallyRevealed: Boolean = false) {
    internal val draggable = AnchoredDraggableState(
        initialValue = initiallyRevealed,
        anchors = DraggableAnchors {
            false at 0f
            true at -1f
        },
    )

    /** Current visual reveal fraction; it remains in 0..1 even during spring overshoot. */
    val revealFraction: Float
        get() = if (draggable.offset == 0f) 0f else (draggable.offset / draggable.anchors.positionOf(true)).coerceIn(0f, 1f)

    /** Whether the gesture or a command is settling towards the revealed position. */
    val isRevealed: Boolean get() = draggable.targetValue

    /** Reveals both actions with the native stage-one spring. A new drag cancels this animation. */
    suspend fun reveal() = draggable.animateTo(true, NotificationSwipeDefaults.SettleSpring)

    /** Returns the notification and its menu to the closed position. */
    suspend fun close() = draggable.animateTo(false, NotificationSwipeDefaults.SettleSpring)

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

/** Values from the ColorOS 17 notification menu resources and stage-one animation parameters. */
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
    val RevealWidth = (ActionWidth + ActionGap) * 2 + EndPadding

    /** AnimateParamOne: response 0.55, bounce 0.4. */
    val SettleSpring: SpringSpec<Float> = folmeSpring(damping = 0.6f, response = 0.55f)

    internal const val FADE_START = 0.6f
    internal const val ICON_BLUR = 15f
}

/**
 * A notification that reveals settings and delete buttons on a physical left swipe.
 *
 * The visible card and both menu backgrounds share one [MetaBallSurface]. Their signed-distance
 * fields merge while touching and split as the card retreats. Menu icons follow by 8dp, fade in
 * over the last 40% of each cell's exposure, and spring back when that cell clears the card.
 * Content keeps its original width and moves left inside the shrinking, rounded card mask.
 *
 * Supply foreground content without its own opaque background so it does not cover the melt.
 * A [io.github.suqi8.coui.kmp.blur.LocalBackdrop] gives the chain the existing glass treatment.
 * Below Android 13 the existing surface draws separate opaque shapes without the shader bridge.
 * The swipe settles onto a menu; deleting is always an explicit [onDismiss] action.
 *
 * @param onSettings Invoked by the revealed settings button or its accessibility action.
 * @param onDismiss Invoked by the revealed delete button or its accessibility action.
 * @param settingsIcon The settings button image.
 * @param dismissIcon The delete button image.
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
 * @param content The notification's foreground, measured at full width throughout the gesture.
 */
@Composable
@ExperimentalCOUIApi
fun NotificationSwipeBox(
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
    settingsIcon: ImageVector,
    dismissIcon: ImageVector,
    modifier: Modifier = Modifier,
    state: NotificationSwipeState = rememberNotificationSwipeState(),
    enabled: Boolean = true,
    minHeight: Dp = NotificationSwipeDefaults.MinHeight,
    containerColor: Color = COUITheme.colorScheme.bar,
    actionColor: Color = COUITheme.colorScheme.onSurface,
    rimColor: Color = Color.White.copy(alpha = 0.55f),
    settingsDescription: String = "Settings",
    dismissDescription: String = "Delete",
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }
    val revealWidthPx = with(density) { NotificationSwipeDefaults.RevealWidth.toPx() }
    val width = with(density) { measuredSize.width.toDp() }
    val height = with(density) { measuredSize.height.toDp() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged {
                measuredSize = it
                // Keep the target: the placeholder anchors put "revealed" at -1px, which would
                // otherwise snap an initially revealed or restored state back to closed.
                state.draggable.updateAnchors(
                    DraggableAnchors {
                        false at 0f
                        true at -revealWidthPx
                    },
                    state.draggable.targetValue,
                )
            }
            .anchoredDraggable(
                state = state.draggable,
                orientation = Orientation.Horizontal,
                // The native left-swipe affordance has a physical direction even in RTL content.
                reverseDirection = false,
                enabled = enabled && width > NotificationSwipeDefaults.RevealWidth,
                flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                    state = state.draggable,
                    positionalThreshold = { it * 0.5f },
                    animationSpec = NotificationSwipeDefaults.SettleSpring,
                ),
            )
            .semantics {
                customActions = if (enabled) {
                    listOf(
                        CustomAccessibilityAction(settingsDescription) {
                            onSettings()
                            true
                        },
                        CustomAccessibilityAction(dismissDescription) {
                            onDismiss()
                            true
                        },
                    )
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
 * The merged card and menu material. It reads [NotificationSwipeState.revealFraction] while it
 * composes, so it is the only part of [NotificationSwipeBox] that re-runs on every drag frame.
 */
@Composable
private fun BoxScope.NotificationSwipeMenu(
    state: NotificationSwipeState,
    width: Dp,
    height: Dp,
    containerColor: Color,
    actionColor: Color,
    rimColor: Color,
    settingsIcon: ImageVector,
    dismissIcon: ImageVector,
    settingsDescription: String,
    dismissDescription: String,
    enabled: Boolean,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val distance = NotificationSwipeDefaults.RevealWidth * state.revealFraction
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
    val particles = notificationSwipeParticles(width, height, distance, deleteRecoil, settingsRecoil)
    // The menu sheet covers exactly the strip the card has vacated.
    val menuShape = remember(density, state) {
        GenericShape { size, _ ->
            val distancePx = with(density) { (NotificationSwipeDefaults.RevealWidth * state.revealFraction).toPx() }
            addRect(Rect((size.width - distancePx).coerceAtLeast(0f), 0f, size.width, size.height))
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
            enabled = enabled,
            onClick = {
                scope.launch { state.close() }
                onDismiss()
            },
        )
        NotificationMenuAction(
            particle = particles[2],
            progress = settingsProgress,
            icon = settingsIcon,
            description = settingsDescription,
            color = actionColor,
            enabled = enabled,
            onClick = {
                scope.launch { state.close() }
                onSettings()
            },
        )
    }
}

/**
 * The notification card. [NotificationSwipeState.revealFraction] is read only inside the clip shape
 * and the offset lambda, so a drag re-draws and re-places the card without recomposing it, and
 * without recomposing [content].
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
            val visibleWidth = (size.width - with(density) { (NotificationSwipeDefaults.RevealWidth * state.revealFraction).toPx() }).coerceAtLeast(0f)
            addSquircleRect(
                width = visibleWidth,
                height = size.height,
                cornerRadius = with(density) { NotificationSwipeDefaults.CardCornerRadius.toPx() },
                squircleEnabled = squircleEnabled,
            )
        }
    }
    val cardOffset = remember(density, state) {
        Modifier.absoluteOffset {
            IntOffset(-with(density) { (NotificationSwipeDefaults.RevealWidth * state.revealFraction).roundToPx() }, 0)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(cardShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(cardOffset),
            content = content,
        )
    }
}

@Composable
private fun NotificationMenuAction(
    particle: MetaBallParticle,
    progress: Float,
    icon: ImageVector,
    description: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val alpha = ((progress - NotificationSwipeDefaults.FADE_START) / (1f - NotificationSwipeDefaults.FADE_START)).coerceIn(0f, 1f)
    // An entirely covered button is absent from both hit testing and the accessibility tree.
    if (alpha <= 0f) return
    val density = LocalDensity.current
    IconButton(
        onClick = onClick,
        enabled = enabled && progress >= 1f,
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
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = color,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { this.alpha = alpha }
                .blur(with(density) { (NotificationSwipeDefaults.ICON_BLUR * (1f - alpha)).toDp() }),
        )
    }
}

internal fun notificationActionProgress(distance: Dp, index: Int): Float = ((distance - NotificationSwipeDefaults.EndPadding - (NotificationSwipeDefaults.ActionWidth + NotificationSwipeDefaults.ActionGap) * index) / (NotificationSwipeDefaults.ActionWidth + NotificationSwipeDefaults.FollowDistance)).coerceIn(0f, 1f)

internal fun notificationSwipeParticles(
    width: Dp,
    height: Dp,
    distance: Dp,
    deleteRecoil: Float = 0f,
    settingsRecoil: Float = 0f,
): List<MetaBallParticle> {
    val cardWidth = (width - distance).coerceAtLeast(0.dp)
    val buttonHeight = minOf(NotificationSwipeDefaults.ActionHeight, (height - NotificationSwipeDefaults.ActionTop * 2).coerceAtLeast(0.dp))
    return listOf(
        MetaBallParticle(cardWidth / 2, height / 2, cardWidth, height, NotificationSwipeDefaults.CardCornerRadius),
    ) + List(2) { index ->
        val recoil = if (index == 0) deleteRecoil else settingsRecoil
        val follow = NotificationSwipeDefaults.FollowDistance * notificationActionProgress(distance, index) * (1f - recoil)
        MetaBallParticle(
            centerX = width - NotificationSwipeDefaults.EndPadding - NotificationSwipeDefaults.ActionWidth / 2 - (NotificationSwipeDefaults.ActionWidth + NotificationSwipeDefaults.ActionGap) * index - follow,
            centerY = NotificationSwipeDefaults.ActionTop + buttonHeight / 2,
            width = NotificationSwipeDefaults.ActionWidth,
            height = buttonHeight,
            cornerRadius = NotificationSwipeDefaults.CornerRadius,
        )
    }
}
