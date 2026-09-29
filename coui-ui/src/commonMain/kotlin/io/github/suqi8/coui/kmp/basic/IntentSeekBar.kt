// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.annotation.IntRange
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The ColorOS 17 *intent* seek bar (`COUIIntentSeekBar`,
 * `com/coui/appcompat/seekbar/COUIIntentSeekBar`), the slider ColorOS uses for media playback and
 * other "intent" surfaces. It is a [Slider] with a third layer: a **secondary progress** bar
 * (`ProgressBar.setSecondaryProgress`) drawn between the track and the played progress, used for
 * buffered / pre-loaded ranges.
 *
 * It shares the `COUISeekBar` gesture model with [Slider] — press anywhere to seek, drag to scrub,
 * edge haptics — but in ColorOS it is a separate class with its own style
 * (`style/COUIIntentSeekBar` / `COUIIntentSeekBar.Dark`) and its own attribute set, and its colours
 * are deliberately *not* [SliderColors]: it paints translucent overlay colours so the bar stays
 * legible on top of video, and it keeps a second, fully opaque-independent palette for when the
 * system theme must be ignored. That is why it lives in this file rather than being folded into
 * [Slider] — the two components share no colour model, only the gesture model.
 *
 * Geometry is the `IntentSeekBarSize` triplet: the track is 4dp / 2dp / 6dp tall for
 * [IntentSeekBarSize.Default] / [IntentSeekBarSize.Small] / [IntentSeekBarSize.Large]
 * (`coui_seekbar_intent_height_default|small|large`), and the layout height is the native
 * `onMeasure`: `max(maxBackgroundHeight, thumbMaxRadius * 2, MIN_TOUCH_TARGET_HEIGHT_DP = 24dp)`.
 *
 * @param value The current played value. Coerced into [valueRange].
 * @param onValueChange The callback invoked while the user scrubs the played value.
 * @param modifier The modifier to be applied to the seek bar.
 * @param enabled Whether the seek bar reacts to input.
 * @param valueRange The range of values the bar can take.
 * @param secondaryProgress The buffered value, drawn under [value]. Coerced into [valueRange] and
 *   never drawn past [value], matching `COUISeekBarCore.drawActiveTrack`, which paints the secondary
 *   renderer first and the played progress over it inside one clip of the track capsule.
 * @param steps If positive, the number of discrete values between the ends of [valueRange]. Must not
 *   be negative.
 * @param onValueChangeFinished Called when a drag ends.
 * @param showSecondaryProgress Whether the secondary layer is drawn. Native default: true
 *   (`couiIntentSeekBarShowSecondaryProgress`).
 * @param showThumb Whether the thumb is drawn. Native default: false
 *   (`couiIntentSeekBarShowThumb`) — the intent bar is normally a bare overlay bar and the thumb
 *   only appears while the user interacts. This port draws it whenever it is enabled, which is the
 *   static approximation of `COUIIntentSeekBar.shouldDrawThumbNow`.
 * @param size The `IntentSeekBarSize` triplet of `attr/couiIntentSeekBarSize`
 *   (0 = DEFAULT, 1 = SMALL, 2 = LARGE).
 * @param colors The [IntentSeekBarColors]. Defaults to [IntentSeekBarDefaults.colors], the
 *   `couiIntentSeekBarFollowSystemTheme = true` palette.
 */
@Composable
fun IntentSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    secondaryProgress: Float = valueRange.start,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    showSecondaryProgress: Boolean = true,
    showThumb: Boolean = false,
    size: IntentSeekBarSize = IntentSeekBarSize.Default,
    colors: IntentSeekBarColors = IntentSeekBarDefaults.colors(),
) {
    require(steps >= 0) { "steps should be >= 0" }
    require(valueRange.start < valueRange.endInclusive) { "valueRange start should be less than end" }

    val hapticFeedback = LocalHapticFeedback.current
    val layoutDirection = LocalLayoutDirection.current
    val reverseDirection = layoutDirection == LayoutDirection.Rtl
    val onValueChangeState by rememberUpdatedState(onValueChange)
    val onValueChangeFinishedState by rememberUpdatedState(onValueChangeFinished)

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isThumbGrabbed by remember { mutableStateOf(false) }
    var isHoveringThumb by remember { mutableStateOf(false) }
    var layoutWidth by remember { mutableIntStateOf(0) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val hapticState = remember { SliderHapticState() }

    val coercedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val coercedSecondary = secondaryProgress
        .coerceIn(valueRange.start, valueRange.endInclusive)
        .coerceAtMost(coercedValue)

    // COUISeekBarAdvanced.useDragTransitionAnimation() returns true for the intent bar: the played
    // progress follows the finger on a fast spring and settles on a slower one, exactly like Slider.
    val progressAnimationSpec = remember(isDragging) {
        if (isDragging) {
            spring(dampingRatio = 0.9f, stiffness = 1755f)
        } else {
            spring<Float>(dampingRatio = 0.96f, stiffness = 322f)
        }
    }
    val animatedValueState = animateFloatAsState(coercedValue, progressAnimationSpec)

    // COUIIntentSeekBar.initSecondaryProgressAnim: COUISpringForce response 0.5 / bounce 0, i.e. a
    // critically damped spring with stiffness (2 * PI / response)^2 = 158, the same derivation
    // Slider.kt uses for its response 0.2 thumb spring (987).
    val animatedSecondaryState = animateFloatAsState(
        coercedSecondary,
        remember { spring<Float>(dampingRatio = 1f, stiffness = 158f) },
    )

    // Geometry: the background rect is full width (COUIIntentSeekBar.getBackgroundHorizontalInset
    // returns 0.0f), and the progress / thumb travel is inset by the cap radius so the round ends
    // stay inside the bar. The cap is half of whichever is taller, the track or the pressed thumb.
    val capInset = remember(size, showThumb) {
        maxOf(size.trackHeight, if (showThumb) size.thumbMaxRadius * 2 else 0.dp) / 2
    }
    val capInsetPx = with(LocalDensity.current) { capInset.toPx() }
    val trackHeightPx = with(LocalDensity.current) { size.trackHeight.toPx() }
    val thumbRadiusPx = with(LocalDensity.current) { size.thumbRadius.toPx() }
    val layoutHeight = maxOf(
        size.maxBackgroundHeight,
        if (showThumb) size.thumbMaxRadius * 2 else 0.dp,
        MinTouchTargetHeight,
    )

    val fractionSpan = valueRange.endInclusive - valueRange.start
    val valueToFraction: (Float) -> Float = { target -> ((target - valueRange.start) / fractionSpan).coerceIn(0f, 1f) }
    val fractionToValue: (Float) -> Float = { fraction ->
        val raw = valueRange.start + fraction * fractionSpan
        if (steps > 0) {
            val position = (fraction * (steps + 1)).roundToInt()
            valueRange.start + position / (steps + 1f) * fractionSpan
        } else {
            raw
        }
    }
    val keyPointFractions = remember(steps) {
        if (steps <= 0) floatArrayOf() else FloatArray(steps + 2) { it / (steps + 1f) }
    }

    Box(
        modifier = modifier
            .then(
                if (enabled) {
                    Modifier
                        .onSizeChanged { layoutWidth = it.width }
                        .pointerInput(reverseDirection, capInsetPx, valueRange, steps) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.last()

                                    if (event.type == PointerEventType.Exit ||
                                        event.type == PointerEventType.Release ||
                                        change.type != PointerType.Mouse
                                    ) {
                                        isHoveringThumb = false
                                        continue
                                    }

                                    val available = (layoutWidth - 2f * capInsetPx).coerceAtLeast(0f)
                                    val fraction = valueToFraction(animatedValueState.value)
                                    val effective = if (reverseDirection) 1f - fraction else fraction
                                    val thumbX = capInsetPx + effective * available
                                    val isOver = abs(change.position.x - thumbX) <= capInsetPx
                                    if (isHoveringThumb != isOver) {
                                        isHoveringThumb = isOver
                                    }
                                }
                            }
                        }
                        .hoverable(
                            interactionSource = interactionSource,
                            enabled = enabled,
                        )
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { dragAmount ->
                                dragOffset += dragAmount
                                val visual = intentFraction(dragOffset, layoutWidth, capInsetPx)
                                val fraction = if (reverseDirection) 1f - visual else visual
                                val calculated = fractionToValue(fraction)
                                onValueChangeState(calculated)
                                hapticState.handleHapticFeedback(
                                    currentValue = calculated,
                                    valueRange = valueRange,
                                    hapticEffect = SliderDefaults.DefaultHapticEffect,
                                    hapticFeedback = hapticFeedback,
                                    keyPointFractions = keyPointFractions,
                                )
                            },
                            onDragStarted = { offset ->
                                isDragging = true
                                isThumbGrabbed = true
                                dragOffset = offset.x
                                val visual = intentFraction(offset.x, layoutWidth, capInsetPx)
                                val fraction = if (reverseDirection) 1f - visual else visual
                                val calculated = fractionToValue(fraction)
                                onValueChangeState(calculated)
                                hapticState.reset(calculated)
                            },
                            onDragStopped = {
                                isDragging = false
                                isThumbGrabbed = false
                                onValueChangeFinishedState?.invoke()
                            },
                        )
                        .indication(interactionSource, null)
                } else {
                    Modifier
                },
            )
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    coercedValue,
                    valueRange.start..valueRange.endInclusive,
                    if (steps > 0) steps else 0,
                )
                setProgress { target ->
                    onValueChangeState(target.coerceIn(valueRange.start, valueRange.endInclusive))
                    true
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val valueFraction = valueToFraction(animatedValueState.value).let { if (reverseDirection) 1f - it else it }
        val secondaryFraction = valueToFraction(animatedSecondaryState.value)
            .let { if (reverseDirection) 1f - it else it }
        IntentSeekBarTrack(
            backgroundColor = colors.backgroundColor,
            progressColor = colors.progressColor,
            secondaryProgressColor = colors.secondaryProgressColor,
            thumbColor = colors.thumbColor,
            valueFraction = valueFraction,
            secondaryFraction = secondaryFraction,
            trackHeightPx = trackHeightPx,
            thumbRadiusPx = thumbRadiusPx,
            capInsetPx = capInsetPx,
            showSecondaryProgress = showSecondaryProgress,
            showThumb = showThumb,
            thumbPressed = isPressed || (isDragging && isThumbGrabbed) || isHoveringThumb,
            modifier = Modifier.fillMaxWidth().height(layoutHeight),
        )
    }
}

/**
 * Draws the intent seek bar in the native layer order of `COUIIntentSeekBar.drawBackground` +
 * `drawActiveTrack`: the full-width capsule track, then the secondary progress, then the played
 * progress — both clipped to the track capsule — and finally the thumb.
 */
@Composable
private fun IntentSeekBarTrack(
    backgroundColor: Color,
    progressColor: Color,
    secondaryProgressColor: Color,
    thumbColor: Color,
    valueFraction: Float,
    secondaryFraction: Float,
    trackHeightPx: Float,
    thumbRadiusPx: Float,
    capInsetPx: Float,
    showSecondaryProgress: Boolean,
    showThumb: Boolean,
    thumbPressed: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val radius = trackHeightPx / 2f
        val centerY = size.height / 2f
        val top = centerY - radius
        val available = (size.width - 2f * capInsetPx).coerceAtLeast(0f)
        val capsule = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = top,
                    right = size.width,
                    bottom = top + trackHeightPx,
                    radiusX = radius,
                    radiusY = radius,
                ),
            )
        }

        drawPath(capsule, color = backgroundColor)

        clipPath(capsule) {
            if (showSecondaryProgress) {
                drawIntentBar(secondaryProgressColor, secondaryFraction, capInsetPx, available, top, trackHeightPx, radius)
            }
            drawIntentBar(progressColor, valueFraction, capInsetPx, available, top, trackHeightPx, radius)
        }

        if (showThumb) {
            // COUIIntentSeekBar.applyThumbSize: thumbMaxRadius is the pressed radius.
            val thumbRadius = if (thumbPressed) maxOf(thumbRadiusPx, capInsetPx) else thumbRadiusPx
            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(capInsetPx + valueFraction * available, centerY),
            )
        }
    }
}

private fun DrawScope.drawIntentBar(
    color: Color,
    fraction: Float,
    capInsetPx: Float,
    available: Float,
    top: Float,
    trackHeightPx: Float,
    radius: Float,
) {
    val width = fraction * available
    if (width <= 0f) return
    drawRoundRect(
        color = color,
        topLeft = Offset(capInsetPx, top),
        size = Size(width, trackHeightPx),
        cornerRadius = CornerRadius(radius, radius),
    )
}

private fun intentFraction(offsetX: Float, sizeWidth: Int, capInsetPx: Float): Float {
    val available = (sizeWidth - 2f * capInsetPx).coerceAtLeast(0f)
    return if (available == 0f) 0f else ((offsetX - capInsetPx) / available).coerceIn(0f, 1f)
}

/**
 * COUIIntentSeekBar.MIN_TOUCH_TARGET_HEIGHT_DP = 24.0f — the shortest the bar is ever laid out,
 * applied by the native `onMeasure`.
 */
private val MinTouchTargetHeight = 24.dp

/**
 * The `IntentSeekBarSize` triplet of `com/coui/appcompat/seekbar/IntentSeekBarSize`, selected by
 * `attr/couiIntentSeekBarSize` (0 = DEFAULT, 1 = SMALL, 2 = LARGE).
 *
 * @property trackHeight `coui_seekbar_intent_height_default|small|large` — the resting track height.
 * @property maxBackgroundHeight `coui_seekbar_intent_max_background_height_default|small|large` —
 *   the height the track grows to while pressed (`COUIIntentSeekBar.DEFAULT_BACKGROUND_ENLARGE_SCALE`
 *   = 6.0f). This is also the resting layout height of the bar.
 * @property thumbRadius `coui_seekbar_intent_thumb_radius_default|small|large`.
 * @property thumbMaxRadius `coui_seekbar_intent_thumb_max_radius_default|small|large` — the pressed
 *   thumb radius.
 */
enum class IntentSeekBarSize(
    val trackHeight: Dp,
    val maxBackgroundHeight: Dp,
    val thumbRadius: Dp,
    val thumbMaxRadius: Dp,
) {
    /** `IntentSeekBarSize.DEFAULT`. */
    Default(trackHeight = 4.dp, maxBackgroundHeight = 6.dp, thumbRadius = 4.dp, thumbMaxRadius = 5.dp),

    /** `IntentSeekBarSize.SMALL`. */
    Small(trackHeight = 2.dp, maxBackgroundHeight = 10.dp, thumbRadius = 3.dp, thumbMaxRadius = 8.dp),

    /** `IntentSeekBarSize.LARGE`. */
    Large(trackHeight = 6.dp, maxBackgroundHeight = 10.dp, thumbRadius = 5.dp, thumbMaxRadius = 8.dp),
}

/** Contains the default values used by [IntentSeekBar]. */
object IntentSeekBarDefaults {
    /**
     * `attr/couiIntentSeekBarConicLightColor` default:
     * `COUIIntentSeekBar.DEFAULT_CONIC_LIGHT_COLOR` = -493674 = `#fff87796`. The conic-light renderer
     * that consumes it is not ported.
     */
    val ConicLightColor = 0xFFF87796

    /** `color/coui_seekbar_intent_background_color_fixed` = `#4dffffff`. */
    val FixedBackgroundColor = Color(0x4DFFFFFF)

    /** `color/coui_seekbar_intent_progress_color_fixed` = `#8affffff`. */
    val FixedProgressColor = Color(0x8AFFFFFF)

    /** `color/coui_seekbar_intent_secondary_progress_color_fixed` = `#29ffffff`. */
    val FixedSecondaryProgressColor = Color(0x29FFFFFF)

    /** `color/coui_seekbar_intent_thumb_color_fixed` = `#ffffffff`. */
    val FixedThumbColor = Color(0xFFFFFFFF)

    /**
     * The `couiIntentSeekBarFollowSystemTheme = true` palette, which is the native default.
     *
     * `COUIIntentSeekBar.applyIntentColors` resolves the background and the secondary progress from
     * `?attr/couiColorLabelQuaternary` (`#14000000` light / `#26ffffff` night) and the played progress
     * from `?attr/couiColorLabelPrimary` (`#e6000000` light / `#e6ffffff` night). Both already exist
     * as theme slots — [io.github.suqi8.coui.kmp.theme.Colors.secondaryVariant] and
     * [io.github.suqi8.coui.kmp.theme.Colors.onSurface] — so no colour slot is added for them.
     *
     * The thumb is `color/coui_seekbar_intent_thumb_color` (`#ff000000` light / `#ffffffff` night),
     * pure black or pure white, which has no theme slot; it is picked from the theme background
     * luminance.
     *
     * The native component also forces [backgroundColor] / [progressColor] to the caller's values and
     * switches to [fixedColors] as soon as any of `couiSeekBarBackgroundColor`,
     * `couiSeekBarProgressColor` or `couiSeekBarSecondaryProgressColor` is set; this port expresses
     * that by simply letting the caller pass a different [IntentSeekBarColors].
     */
    @Composable
    fun colors(
        backgroundColor: Color = COUITheme.colorScheme.secondaryVariant,
        progressColor: Color = COUITheme.colorScheme.onSurface,
        secondaryProgressColor: Color = COUITheme.colorScheme.secondaryVariant,
        thumbColor: Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
            Color(0xFFFFFFFF)
        } else {
            Color(0xFF000000)
        },
    ): IntentSeekBarColors = remember(backgroundColor, progressColor, secondaryProgressColor, thumbColor) {
        IntentSeekBarColors(backgroundColor, progressColor, secondaryProgressColor, thumbColor)
    }

    /**
     * The `couiIntentSeekBarFollowSystemTheme = false` palette: the fixed translucent overlay colours,
     * for drawing the bar on top of video or a wallpaper where theme colours would be unreadable.
     */
    fun fixedColors(
        backgroundColor: Color = FixedBackgroundColor,
        progressColor: Color = FixedProgressColor,
        secondaryProgressColor: Color = FixedSecondaryProgressColor,
        thumbColor: Color = FixedThumbColor,
    ): IntentSeekBarColors = IntentSeekBarColors(backgroundColor, progressColor, secondaryProgressColor, thumbColor)

    /**
     * `COUIIntentSeekBar.getThemeFallbackColors`, used by the native component only when the theme
     * does not define `couiColorLabelPrimary` / `couiColorLabelQuaternary`: the plain seek bar colours
     * `coui_seekbar_background_color_normal` (`#0d000000`),
     * `coui_seekbar_progress_color_normal` (`#ff4d4d4d` / `#ff999999`) and
     * `coui_seekbar_secondary_progress_color` (`#4dffffff`).
     */
    fun fallbackColors(
        backgroundColor: Color = Color(0x0D000000),
        progressColor: Color = Color(0xFFFF4D4D4D),
        secondaryProgressColor: Color = Color(0x4DFFFFFF),
        thumbColor: Color = Color(0xFF000000),
    ): IntentSeekBarColors = IntentSeekBarColors(backgroundColor, progressColor, secondaryProgressColor, thumbColor)
}

/**
 * The colours of an [IntentSeekBar].
 *
 * @property backgroundColor `coui_seekbar_intent_background_color_fixed` in fixed mode, otherwise
 *   `?attr/couiColorLabelQuaternary`.
 * @property progressColor `coui_seekbar_intent_progress_color_fixed` in fixed mode, otherwise
 *   `?attr/couiColorLabelPrimary`.
 * @property secondaryProgressColor `coui_seekbar_intent_secondary_progress_color_fixed` in fixed
 *   mode, otherwise `?attr/couiColorLabelQuaternary`.
 * @property thumbColor `coui_seekbar_intent_thumb_color` / `coui_seekbar_intent_thumb_color_fixed`.
 */
@Immutable
data class IntentSeekBarColors(
    val backgroundColor: Color,
    val progressColor: Color,
    val secondaryProgressColor: Color,
    val thumbColor: Color,
)
