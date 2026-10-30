// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.COUITheme.isDynamicColor
import io.github.suqi8.coui.kmp.theme.LocalColors
import io.github.suqi8.coui.kmp.utils.CouiHapticEffect
import io.github.suqi8.coui.kmp.utils.rememberCouiHaptic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A [Switch] component with COUI style.
 *
 * @param checked The checked state of the [Switch].
 * @param onCheckedChange The callback to be called when the state of the [Switch] changes.
 * @param modifier The modifier to be applied to the [Switch].
 * @param colors The [SwitchColors] of the [Switch].
 * @param enabled Whether the [Switch] is enabled.
 * @param isLoading Whether the [Switch] is in the COUI loading state: the thumb shrinks away and
 *   is replaced by a spinning gradient arc; touch input is swallowed while loading.
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    colors: SwitchColors = SwitchDefaults.switchColors(),
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val couiHaptic = rememberCouiHaptic()

    val capsuleShape = CircleShape
    // COUI toggle interpolator, shared by every child animator (COUISwitch.animateWhenStateChanged()).
    val couiToggleEasing = remember { CubicBezierEasing(0.3f, 0f, 0.1f, 1f) }

    val thumb = remember { SwitchThumb(checked) }

    // Every way the thumb comes to rest ends here, so a caller that refuses a drag's result pulls
    // the thumb back instead of leaving it on the wrong side.
    LaunchedEffect(checked, thumb.settleRequests) {
        val rest = switchRestOffset(checked)
        if (thumb.held || thumb.offset.value == rest) return@LaunchedEffect
        thumb.toggling = true
        try {
            thumb.offset.animateTo(rest, SwitchDefaults.ToggleSpring)
        } finally {
            thumb.toggling = false
        }
    }

    val thumbColorState = animateColorAsState(
        if (checked) colors.checkedThumbColor(enabled) else colors.uncheckedThumbColor(enabled),
    )

    // The track blends by thumb position, whether a finger or a spring moves it (COUISwitch.setBarColor).
    val checkedTrackColorState = animateColorAsState(
        colors.checkedTrackColor(enabled),
        animationSpec = tween(durationMillis = 450, easing = couiToggleEasing),
    )
    val uncheckedTrackColorState = animateColorAsState(
        colors.uncheckedTrackColor(enabled),
        animationSpec = tween(durationMillis = 450, easing = couiToggleEasing),
    )

    // COUI press/hover feedback: translucent overlay over the bar color (coui_color_press / coui_color_hover).
    val overlayTone = COUITheme.colorScheme.onSurface
    val pressOverlayState = animateColorAsState(
        when {
            !enabled -> Color.Transparent
            isPressed -> overlayTone.copy(alpha = 0.12f)
            isHovered -> overlayTone.copy(alpha = 0.078f)
            else -> Color.Transparent
        },
    )

    // COUI loading state (COUISwitch.startLoading()/stopLoading()).
    val thumbScale = remember { Animatable(1f) }
    val loadingAlpha = remember { Animatable(0f) }
    val loadingScale = remember { Animatable(0.5f) }
    val loadingRotation = remember { Animatable(0f) }
    LaunchedEffect(isLoading) {
        if (isLoading) {
            loadingAlpha.snapTo(0f)
            loadingScale.snapTo(0.5f)
            launch { thumbScale.animateTo(0f, tween(durationMillis = 433, easing = couiToggleEasing)) }
            launch { loadingScale.animateTo(1f, tween(durationMillis = 550, easing = couiToggleEasing)) }
            launch { loadingAlpha.animateTo(1f, tween(durationMillis = 550, easing = couiToggleEasing)) }
            while (true) {
                loadingRotation.snapTo(0f)
                loadingRotation.animateTo(360f, tween(durationMillis = 800, easing = LinearEasing))
            }
        } else {
            thumbScale.snapTo(1f)
            loadingAlpha.animateTo(0f, tween(durationMillis = 100, easing = couiToggleEasing))
        }
    }

    val currentCouiHaptic by rememberUpdatedState(couiHaptic)
    // Loading switches cannot be toggled (COUISwitch loading style swallows touch).
    val interactive = enabled && !isLoading
    val hasCallback = onCheckedChange != null
    val onToggle: (Boolean) -> Unit = remember {
        { value ->
            currentOnCheckedChange?.invoke(value)
            currentCouiHaptic(CouiHapticEffect.Switch)
        }
    }
    val currentChecked by rememberUpdatedState(checked)
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val toggleableModifier = if (hasCallback) {
        remember(checked, interactive, interactionSource, thumb, layoutDirection) {
            // The touch modifier sits outside toggleable and takes the pointer first, which leaves
            // toggleable with the semantics, focus and keyboard handling.
            val touch = if (interactive) {
                Modifier.switchTouch(
                    thumb = thumb,
                    interactionSource = interactionSource,
                    layoutDirection = layoutDirection,
                    scope = scope,
                    isChecked = { currentChecked },
                    onToggle = onToggle,
                )
            } else {
                Modifier
            }
            touch.toggleable(
                value = checked,
                onValueChange = onToggle,
                enabled = interactive,
                role = Role.Switch,
                interactionSource = interactionSource,
            )
        }
    } else {
        Modifier.semantics {
            role = Role.Switch
            toggleableState = ToggleableState(checked)
            if (!enabled) disabled()
        }
    }

    Box(
        modifier = modifier
            .wrapContentSize(Alignment.Center)
            .size(SwitchDefaults.TrackWidth, SwitchDefaults.TrackHeight)
            .clip(capsuleShape)
            .drawBehind {
                drawRect(
                    blendArgb(
                        uncheckedTrackColorState.value,
                        checkedTrackColorState.value,
                        thumb.progress.coerceIn(0f, 1f),
                    ),
                )
                drawRect(pressOverlayState.value)
            }
            .hoverable(
                interactionSource = interactionSource,
                enabled = interactive,
            )
            .then(toggleableModifier),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(thumb.offset.value.roundToPx(), 0) }
                .layout { measurable, _ ->
                    // Stretch by resizing instead of scaling so the thumb stays a capsule. The
                    // leading edge stays put (COUISwitch.setOuterCircleRectF()).
                    val thumbPx = SwitchDefaults.ThumbSize.roundToPx()
                    val stretchedPx = (thumbPx * thumb.stretch).roundToInt()
                    val placeable = measurable.measure(Constraints.fixed(stretchedPx, thumbPx))
                    layout(stretchedPx, thumbPx) { placeable.place(0, 0) }
                },
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        // COUI loading hides the thumb by scaling it about its centre.
                        scaleX = thumbScale.value
                        scaleY = thumbScale.value
                    }
                    .then(
                        if (enabled) {
                            // COUISwitch clears the shadow layer when disabled.
                            Modifier.dropShadow(shape = CircleShape, shadow = SwitchDefaults.ThumbShadow)
                        } else {
                            Modifier
                        },
                    )
                    .drawBehind {
                        // A circle at rest, a capsule while stretched (COUISwitch.drawOuterCircle).
                        drawRoundRect(
                            color = thumbColorState.value,
                            cornerRadius = CornerRadius(size.height / 2f),
                        )
                    },
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        // COUISwitch.drawLoading(): arc geometry follows switch_loading.png
                        // (stroke 8/40, centre-line diameter 32/40, ~312 deg alpha-tail sweep).
                        val alpha = loadingAlpha.value
                        if (alpha > 0f) {
                            val side = size.minDimension
                            val strokeWidth = side * 0.2f
                            val arcDiameter = side * 0.8f
                            val topLeft = Offset((size.width - arcDiameter) / 2f, (size.height - arcDiameter) / 2f)
                            val color = thumbColorState.value
                            val brush = Brush.sweepGradient(
                                0f to color.copy(alpha = 0f),
                                312f / 360f to color.copy(alpha = color.alpha * alpha),
                                center = Offset(size.width / 2f, size.height / 2f),
                            )
                            scale(loadingScale.value) {
                                // +150 deg phase puts the head at ~102 deg like the source PNG.
                                rotate(loadingRotation.value + 150f) {
                                    drawArc(
                                        brush = brush,
                                        startAngle = 0f,
                                        sweepAngle = 312f,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = Size(arcDiameter, arcDiameter),
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                    )
                                }
                            }
                        }
                    },
            )
        }
    }
}

/** The thumb position, and what is moving it right now. */
@Stable
private class SwitchThumb(checked: Boolean) {
    val offset = Animatable(switchRestOffset(checked), Dp.VectorConverter)

    /** A toggle spring is running: the thumb stretches and a new press cannot grab it. */
    var toggling by mutableStateOf(false)

    /** A finger holds the thumb or just let go of it, and the state is reported once it lands. */
    var held = false

    /** Bumped when [held] ends, to send the thumb back to wherever `checked` says it belongs. */
    var settleRequests by mutableIntStateOf(0)
        private set

    val progress: Float
        get() = (offset.value - SwitchDefaults.ThumbStartOffset) / SwitchDefaults.Travel

    val stretch: Float
        get() = if (toggling) switchThumbStretch(progress) else 1f

    fun release() {
        held = false
        settleRequests++
    }
}

private fun switchRestOffset(checked: Boolean): Dp = if (checked) SwitchDefaults.ThumbEndOffset else SwitchDefaults.ThumbStartOffset

/**
 * The width of the thumb during a toggle, as a multiple of its height: it peaks at a third of the
 * track and tapers off on either side of it, overshoot included (COUISwitch.initToggleSpringAnim()).
 */
internal fun switchThumbStretch(progress: Float): Float {
    val peak = 0.34725848f
    val amount = 0.29999995f
    return if (progress < peak) {
        1f + progress / peak * amount
    } else {
        1.3f - (progress - peak) / (1f - peak) * amount
    }
}

/**
 * Where a released thumb lands: towards a fast enough fling, otherwise on the nearer side, with the
 * exact middle going to off (COUISpringDragHelper.getAttachPosition()).
 *
 * @param offset The thumb offset when it was released.
 * @param velocity The release velocity in dp per second, positive towards on.
 */
internal fun switchAttachChecked(offset: Dp, velocity: Float): Boolean = if (abs(velocity) >= SwitchDefaults.FLING_VELOCITY) {
    velocity > 0f
} else {
    offset - SwitchDefaults.ThumbStartOffset > SwitchDefaults.Travel / 2
}

/**
 * Takes over the pointer: a press that stays within the touch slop toggles, one that leaves it
 * drags the thumb. The thumb cannot be grabbed mid-toggle, and a landing thumb ignores touch
 * (COUISwitch.canDrag(), COUISwitch.onTouchEvent()).
 */
private fun Modifier.switchTouch(
    thumb: SwitchThumb,
    interactionSource: MutableInteractionSource,
    layoutDirection: LayoutDirection,
    scope: CoroutineScope,
    isChecked: () -> Boolean,
    onToggle: (Boolean) -> Unit,
): Modifier = pointerInput(thumb, interactionSource, layoutDirection) {
    val direction = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
    // The finger's travel across the track maps onto the thumb's travel (COUISwitch.convertToTranslationCoordinates()).
    val thumbPerFinger = SwitchDefaults.Travel / SwitchDefaults.TrackWidth
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (down.type == PointerType.Mouse && !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
        // Consuming from the first event keeps a scrolling parent out, as requestDisallowInterceptTouchEvent does.
        down.consume()
        if (thumb.held) return@awaitEachGesture

        val draggable = !thumb.toggling
        val startOffset = thumb.offset.value
        val press = PressInteraction.Press(down.position)
        interactionSource.tryEmit(press)

        // The native tracker reads the finger scaled from the view onto the thumb's travel, so its
        // velocity is a fraction of the finger's.
        val velocityTracker = VelocityTracker()
        val trackWidth = size.width.toFloat()
        fun trackVelocity(change: PointerInputChange) {
            val fromLeading = if (direction < 0f) trackWidth - change.position.x else change.position.x
            val travelled = (fromLeading / trackWidth).coerceIn(0f, 1f) * SwitchDefaults.Travel.value
            velocityTracker.addPosition(change.uptimeMillis, Offset(travelled, 0f))
        }
        trackVelocity(down)

        var dragging = false
        var ended = false
        try {
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    ended = true
                    // A cancelled pointer arrives consumed; a drag still lands, a tap does not fire.
                    val cancelled = change.isConsumed
                    change.consume()
                    interactionSource.tryEmit(if (cancelled) PressInteraction.Cancel(press) else PressInteraction.Release(press))
                    if (dragging) {
                        val limit = SwitchDefaults.FLING_VELOCITY
                        val velocity = velocityTracker.calculateVelocity().x.coerceIn(-limit, limit)
                        val checked = switchAttachChecked(thumb.offset.value, velocity)
                        scope.launch {
                            thumb.offset.animateTo(switchRestOffset(checked), SwitchDefaults.AttachSpring, velocity.dp)
                            if (checked != isChecked()) onToggle(checked)
                            thumb.release()
                        }
                    } else if (!cancelled) {
                        onToggle(!isChecked())
                    }
                    break
                }
                change.consume()

                trackVelocity(change)

                val moved = change.position - down.position
                if (draggable && (dragging || moved.getDistance() > viewConfiguration.touchSlop)) {
                    dragging = true
                    thumb.held = true
                    val target = (startOffset + (moved.x * direction).toDp() * thumbPerFinger)
                        .coerceIn(SwitchDefaults.ThumbStartOffset, SwitchDefaults.ThumbEndOffset)
                    scope.launch { thumb.offset.animateTo(target, SwitchDefaults.DragSpring) }
                }
            }
        } finally {
            if (!ended) {
                interactionSource.tryEmit(PressInteraction.Cancel(press))
                if (dragging) thumb.release()
            }
        }
    }
}

/** Blends two colors channel by channel in sRGB, as `ColorUtils.blendARGB` does. */
private fun blendArgb(start: Color, end: Color, ratio: Float): Color = Color(
    red = lerp(start.red, end.red, ratio),
    green = lerp(start.green, end.green, ratio),
    blue = lerp(start.blue, end.blue, ratio),
    alpha = lerp(start.alpha, end.alpha, ratio),
)

object SwitchDefaults {

    /** The track width of the [Switch] (COUISwitch bar_width). */
    val TrackWidth: Dp = 38.dp

    /** The track height of the [Switch] (COUISwitch bar_height). */
    val TrackHeight: Dp = 24.dp

    /** The diameter of the [Switch] thumb (COUISwitch outer_circle_width). */
    val ThumbSize: Dp = 18.dp

    /** The inset of the thumb from the track edge when fully on one side (COUISwitch circle_padding). */
    private val ThumbMargin: Dp = 3.dp

    /** The thumb offset in the unchecked (off) position. */
    val ThumbStartOffset: Dp = ThumbMargin

    /** The thumb offset in the checked (on) position. */
    val ThumbEndOffset: Dp = TrackWidth - ThumbSize - ThumbMargin

    /** The distance the thumb travels between off and on. */
    val Travel: Dp = ThumbEndOffset - ThumbStartOffset

    /** The spring of a toggle (COUISwitchParams.toggleSpring: bounce 0.3, response 0.4). */
    internal val ToggleSpring: SpringSpec<Dp> = folmeSpring(damping = 0.7f, response = 0.4f, visibilityThreshold = 0.1.dp)

    /** The spring that trails the finger (COUISwitchParams.responseDrag, bounceDrag). */
    internal val DragSpring: SpringSpec<Dp> = folmeSpring(damping = 1f, response = 0.3f, visibilityThreshold = 0.1.dp)

    /** The spring of a thumb landing after a drag (COUISwitchParams.responseAttach, bounceAttach). */
    internal val AttachSpring: SpringSpec<Dp> = folmeSpring(damping = 0.8f, response = 0.4f, visibilityThreshold = 0.1.dp)

    /**
     * The release velocity in dp per second past which a drag lands towards the fling, and the most
     * it hands the landing spring (COUISwitchParams.transformVelocity: 1000 px/s at the 3.5x ColorOS
     * reference density).
     */
    internal const val FLING_VELOCITY: Float = 1000f / 3.5f

    /**
     * The drop shadow under the thumb while the [Switch] is enabled
     * (COUISwitch.setPaintShadowLayer at the 3.5x ColorOS reference density).
     */
    val ThumbShadow: Shadow = Shadow(
        radius = 2.29.dp,
        color = Color(0x19000000),
        offset = DpOffset(x = 0.dp, y = 1.14.dp),
    )

    /**
     * The default colors for the [Switch]. COUISwitch keeps the thumb white in both states.
     */
    @Composable
    fun switchColors(
        checkedThumbColor: Color = if (isDynamicColor) LocalColors.current.onPrimary else COUITheme.colorScheme.onPrimary,
        uncheckedThumbColor: Color = if (isDynamicColor) LocalColors.current.onPrimary else COUITheme.colorScheme.onSecondary,
        disabledCheckedThumbColor: Color = if (isDynamicColor) LocalColors.current.surface else COUITheme.colorScheme.disabledOnPrimary,
        disabledUncheckedThumbColor: Color = COUITheme.colorScheme.disabledOnSecondary,
        checkedTrackColor: Color = COUITheme.colorScheme.primary,
        uncheckedTrackColor: Color = COUITheme.colorScheme.secondary,
        disabledCheckedTrackColor: Color = COUITheme.colorScheme.disabledPrimary,
        disabledUncheckedTrackColor: Color = COUITheme.colorScheme.disabledSecondary,
    ): SwitchColors = remember(
        checkedThumbColor,
        uncheckedThumbColor,
        disabledCheckedThumbColor,
        disabledUncheckedThumbColor,
        checkedTrackColor,
        uncheckedTrackColor,
        disabledCheckedTrackColor,
        disabledUncheckedTrackColor,
    ) {
        SwitchColors(
            checkedThumbColor = checkedThumbColor,
            uncheckedThumbColor = uncheckedThumbColor,
            disabledCheckedThumbColor = disabledCheckedThumbColor,
            disabledUncheckedThumbColor = disabledUncheckedThumbColor,
            checkedTrackColor = checkedTrackColor,
            uncheckedTrackColor = uncheckedTrackColor,
            disabledCheckedTrackColor = disabledCheckedTrackColor,
            disabledUncheckedTrackColor = disabledUncheckedTrackColor,
        )
    }
}

@Immutable
data class SwitchColors(
    private val checkedThumbColor: Color,
    private val uncheckedThumbColor: Color,
    private val disabledCheckedThumbColor: Color,
    private val disabledUncheckedThumbColor: Color,
    private val checkedTrackColor: Color,
    private val uncheckedTrackColor: Color,
    private val disabledCheckedTrackColor: Color,
    private val disabledUncheckedTrackColor: Color,
) {
    @Stable
    internal fun checkedThumbColor(enabled: Boolean): Color = if (enabled) checkedThumbColor else disabledCheckedThumbColor

    @Stable
    internal fun uncheckedThumbColor(enabled: Boolean): Color = if (enabled) uncheckedThumbColor else disabledUncheckedThumbColor

    @Stable
    internal fun checkedTrackColor(enabled: Boolean): Color = if (enabled) checkedTrackColor else disabledCheckedTrackColor

    @Stable
    internal fun uncheckedTrackColor(enabled: Boolean): Color = if (enabled) uncheckedTrackColor else disabledUncheckedTrackColor
}
