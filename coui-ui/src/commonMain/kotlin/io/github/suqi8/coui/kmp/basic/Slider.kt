// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.annotation.IntRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedback
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A [Slider] component with COUI style.
 *
 * @param value The current value of the [Slider]. If outside of [valueRange] provided, value will be coerced to this range.
 * @param onValueChange The callback to be called when the value changes.
 * @param modifier The modifier to be applied to the [Slider].
 * @param enabled Whether the [Slider] is enabled.
 * @param valueRange Range of values that this slider can take. The passed [value] will be coerced to this range.
 * @param steps If positive, specifies the amount of discrete allowable values between the endpoints of [valueRange].
 *   For example, a range from 0 to 10 with 4 [steps] allows 4 values evenly distributed between 0 and 10 (i.e., 2, 4, 6, 8).
 *   If [steps] is 0, the slider will behave continuously and allow any value from the range. Must not be negative.
 * @param onValueChangeFinished Called when value change has ended. This should not be used to update the slider value
 *   (use [onValueChange] instead), but rather to know when the user has completed selecting a new value by ending a drag or a click.
 * @param reverseDirection Controls the direction of this slider. When false (default), slider increases from left to right.
 *   When true, slider increases from right to left (useful for RTL layouts or custom direction requirements).
 * @param height The rest height of the [Slider] track. While dragging, the inactive track
 *   enlarges to 1.4x of this height and is drawn beyond the layout bounds, following COUI behavior.
 * @param colors The [SliderColors] of the [Slider].
 * @param hapticEffect The haptic effect of the [Slider].
 * @param showKeyPoints Whether to show the key points (step indicators) on the slider. Only works when [keyPoints] is not null.
 * @param keyPoints Custom key point values to display on the slider. If null, uses step positions from [steps] parameter.
 *   Values should be within [valueRange]. For example, for a range of 0f..100f, you might specify listOf(0f, 25f, 50f, 75f, 100f).
 * @param magnetThreshold The magnetic snap threshold as a fraction (0.0 to 1.0). When the slider value is within this
 *   distance from a key point, it will snap to that point. Default is 0.02 (2%). Only applies when [keyPoints] is set.
 * @param secondaryProgress The optional secondary progress ("intent tier") drawn behind the primary active track.
 *   It is coerced into [valueRange]; the part beyond the primary value stays visible, matching the secondary layer
 *   [IntentSeekBar] draws. `null` (the default) draws nothing. New in 1.2.0.
 * @param showProgress Whether to draw the active track (COUI couiSeekBarShowProgress). New in 1.2.0.
 * @param showThumb Whether to draw the thumb (COUI couiSeekBarShowThumb). New in 1.2.0.
 * @param startFromMiddle Whether the active track grows from the middle of the bar towards the
 *   thumb instead of from the start edge (COUI couiSeekBarStartMiddle). New in 1.2.0.
 * @param moveByFinger Whether the thumb follows the finger's displacement instead of jumping to the
 *   finger's position (COUI `MOVE_BY_FINGER`; the default is `MOVE_BY_DISTANCE`). New in 1.2.0.
 */
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    reverseDirection: Boolean = false,
    height: Dp = SliderDefaults.MinHeight,
    colors: SliderColors = SliderDefaults.sliderColors(),
    hapticEffect: SliderDefaults.SliderHapticEffect = SliderDefaults.DefaultHapticEffect,
    showKeyPoints: Boolean = false,
    keyPoints: List<Float>? = null,
    magnetThreshold: Float = 0.02f,
    secondaryProgress: Float? = null,
    showProgress: Boolean = true,
    showThumb: Boolean = true,
    startFromMiddle: Boolean = false,
    moveByFinger: Boolean = false,
) {
    require(steps >= 0) { "steps should be >= 0" }
    require(valueRange.start < valueRange.endInclusive) { "valueRange start should be less than end" }

    val hapticFeedback = LocalHapticFeedback.current
    val layoutDirection = LocalLayoutDirection.current
    val effectiveReverseDirection = if (layoutDirection == LayoutDirection.Rtl) !reverseDirection else reverseDirection
    val onValueChangeState by rememberUpdatedState(onValueChange)
    val onValueChangeFinishedState by rememberUpdatedState(onValueChangeFinished)
    val density = LocalDensity.current
    // The touch target is 36dp tall (couiSeekBarMinHeight) but the track keeps its own height, so
    // the thumb inset and the value mapping are derived from that thickness, not from the layout.
    val trackHeightPx = with(density) { height.toPx() }
    val restingInsetPx = trackHeightPx * TrackCapInsetRatio
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isThumbGrabbed by remember { mutableStateOf(false) }
    var isHoveringThumb by remember { mutableStateOf(false) }
    // The deformation amount of the last drag frame, eased back to 0 on release
    // (DeformationEffectParams, MAX_MOVING_DISTANCE = 30px for the full 4.7dp stretch).
    var deformationRaw by remember { mutableFloatStateOf(0f) }
    var layoutWidth by remember { mutableIntStateOf(0) }
    var layoutHeight by remember { mutableIntStateOf(0) }
    val hapticState = remember { SliderHapticState() }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()
    val flingPosition = remember { Animatable(0f) }
    var flingJob by remember { mutableStateOf<Job?>(null) }
    // COUISectionSeekBar keeps a second thumb position while dragging a stepped slider: the thumb
    // trails the finger instead of jumping tick to tick. See [startSectionSnap] for the model.
    val sectionCount = steps + 1
    var sectionAnchorIndex by remember { mutableIntStateOf(0) }
    var sectionAnchorX by remember { mutableFloatStateOf(0f) }
    var sectionValueIndex by remember { mutableIntStateOf(0) }
    var sectionDragDirection by remember { mutableIntStateOf(0) }
    var sectionLastFingerX by remember { mutableFloatStateOf(0f) }
    var sectionDelta by remember { mutableFloatStateOf(0f) }
    var sectionAnimStartX by remember { mutableFloatStateOf(0f) }
    var sectionAnimEndX by remember { mutableFloatStateOf(0f) }
    var sectionThumbFraction by remember { mutableFloatStateOf(0f) }
    var sectionReleaseFrom by remember { mutableFloatStateOf(0f) }
    var sectionReleaseTo by remember { mutableFloatStateOf(0f) }
    var sectionReleasing by remember { mutableStateOf(false) }
    val sectionSnap = remember { Animatable(0f) }
    var sectionSnapJob by remember { mutableStateOf<Job?>(null) }
    var sectionReleaseJob by remember { mutableStateOf<Job?>(null) }
    val glitter = remember { SliderGlitter() }
    val pressLight = remember { SliderPressLight() }

    val coercedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)

    // COUISeekBarAdvanced DRAG_TRANSITION_SPRING_RESPONSE 0.15 while dragging and
    // ClickAnimationHandler CLICK_SPRING_RESPONSE 0.3 for a tap, both critically damped.
    val progressAnimationSpec = remember(isDragging) {
        if (isDragging) {
            spring(dampingRatio = 1f, stiffness = 1754.6f)
        } else {
            spring<Float>(dampingRatio = 1f, stiffness = 438.65f)
        }
    }

    val animatedValueState = animateFloatAsState(coercedValue, progressAnimationSpec)
    val deformation by animateFloatAsState(
        targetValue = if (isDragging) deformationRaw else 0f,
        animationSpec = DeformationSpring,
        label = "SliderDeformation",
    )
    // COUISeekBarCore.touchAnim enlarges the thumb on ACTION_DOWN wherever the slider is touched.
    val thumbScaleTarget = if (pressLight.isPressed || isPressed || (isDragging && isThumbGrabbed) || isHoveringThumb) {
        ThumbPressedScale
    } else {
        1f
    }
    val thumbScaleState = animateFloatAsState(
        targetValue = thumbScaleTarget,
        animationSpec = if (thumbScaleTarget > 1f) ThumbScaleEnlargeSpec else ThumbScaleShrinkSpec,
        label = "SliderThumbScale",
    )

    val stepFractions = remember(steps) { stepsToTickFractions(steps) }

    val keyPointFractions = remember(keyPoints, stepFractions, valueRange, showKeyPoints) {
        computeKeyPointFractions(keyPoints, stepFractions, valueRange, showKeyPoints)
    }

    val allKeyPointFractions = remember(keyPoints, stepFractions, valueRange) {
        computeAllKeyPointFractions(keyPoints, stepFractions, valueRange)
    }

    val fractionToValue = remember(valueRange, steps, stepFractions, allKeyPointFractions, magnetThreshold) {
        { fraction: Float ->
            resolveValueFromFraction(
                fraction = fraction,
                valueRange = valueRange,
                steps = steps,
                allKeyPointFractions = allKeyPointFractions,
                magnetThreshold = magnetThreshold,
            )
        }
    }

    fun sectionValue(index: Int): Float = valueRange.start + (valueRange.endInclusive - valueRange.start) * index / sectionCount

    // SectionAnimationHandler blends 40% of the snapped offset with 60% of the raw finger offset,
    // so the thumb trails the finger by at most 0.6 of a section.
    fun applySectionThumb(width: Float) {
        val thumb = (sectionAnchorX + sectionSnap.value * SECTION_DRAG_MOVE_RATIO + sectionDelta * (1f - SECTION_DRAG_MOVE_RATIO))
            .coerceIn(0f, width)
        sectionThumbFraction = thumb / width
    }

    // The value only moves when the blended thumb crosses a section: `round` while dragging
    // towards the end, `ceil` towards the start, so a reversed direction never re-reports a tick.
    fun applySectionThumbAndProgress(width: Float) {
        applySectionThumb(width)
        val sections = sectionThumbFraction * width / (width / sectionCount)
        val index = when {
            sectionAnimEndX > sectionAnimStartX -> sections.roundToInt()
            sectionAnimEndX < sectionAnimStartX -> ceil(sections).toInt()
            else -> return
        }.coerceIn(0, sectionCount)
        if (index != sectionValueIndex) {
            sectionValueIndex = index
            val newValue = sectionValue(index)
            onValueChangeState(newValue)
            hapticState.handleHapticFeedback(
                newValue,
                valueRange,
                hapticEffect,
                hapticFeedback,
                allKeyPointFractions,
                hasCustomKeyPoints = keyPoints != null,
            )
        }
    }

    fun startSectionSnap(targetDelta: Float, width: Float) {
        sectionAnimStartX = sectionAnchorX
        sectionAnimEndX = sectionAnchorX + targetDelta
        val start = sectionSnap.value
        sectionSnapJob?.cancel()
        sectionSnapJob = scope.launch {
            sectionSnap.snapTo(start)
            sectionSnap.animateTo(targetDelta, SectionSnapSpec) {
                applySectionThumbAndProgress(width)
            }
            applySectionThumbAndProgress(width)
        }
    }

    val currentLayoutWidth = layoutWidth
    val currentLayoutHeight = layoutHeight
    // COUISeekBar PhysicsAnimationHandler: the thumb keeps sliding after a fast release and settles
    // on a spring, reporting every intermediate value.
    val flingTarget: (Float, Float, Float) -> Unit = { target, from, velocity ->
        flingJob?.cancel()
        flingJob = scope.launch {
            flingPosition.snapTo(from)
            var lastFraction = if (effectiveReverseDirection) 1f - from else from
            flingPosition.animateTo(target, TrackGrowAnimationSpec) {
                val fraction = if (effectiveReverseDirection) 1f - flingPosition.value else flingPosition.value
                onValueChangeState(fractionToValue(fraction))
                glitter.onFling(scope, fraction, lastFraction, velocity, bothEdges = startFromMiddle)
                lastFraction = fraction
            }
            onValueChangeFinishedState?.invoke()
        }
    }

    Box(
        modifier = modifier
            .sliderPressLight(pressLight, enabled)
            .defaultMinSize(minHeight = SliderDefaults.MinTouchHeight)
            .then(
                if (enabled) {
                    Modifier
                        .onSizeChanged {
                            layoutWidth = it.width
                            layoutHeight = it.height
                        }
                        .pointerInput(effectiveReverseDirection, valueRange) {
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

                                    val capInset = restingInsetPx
                                    val availableWidth = (currentLayoutWidth - 2f * capInset).coerceAtLeast(0f)
                                    val hitRadius = capInset

                                    val position = change.position
                                    val fraction = (animatedValueState.value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                                    val effectiveFraction = if (effectiveReverseDirection) 1f - fraction else fraction
                                    val thumbX = capInset + effectiveFraction * availableWidth

                                    val isOver = abs(position.x - thumbX) <= hitRadius
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
                                deformationRaw = (abs(dragAmount) / MAX_MOVING_DISTANCE).coerceIn(0f, 1f)
                                val visualFraction = horizontalVisualFraction(dragOffset, layoutWidth, restingInsetPx)
                                val fractionForValue = if (effectiveReverseDirection) 1f - visualFraction else visualFraction
                                if (steps > 0) {
                                    val width = (layoutWidth - 2f * restingInsetPx).coerceAtLeast(1f)
                                    val fingerX = fractionForValue * width
                                    val pixelsPerSection = width / sectionCount
                                    val direction = when {
                                        fingerX > sectionLastFingerX + 0.5f -> 1
                                        fingerX < sectionLastFingerX - 0.5f -> -1
                                        else -> 0
                                    }
                                    if (direction != 0 && direction == -sectionDragDirection) {
                                        // Reversing re-anchors on the current value, so the value reacts
                                        // as soon as the finger comes back rather than after undoing
                                        // the whole outward travel.
                                        sectionDragDirection = direction
                                        sectionSnapJob?.cancel()
                                        if (sectionAnchorIndex != sectionValueIndex) {
                                            sectionAnchorIndex = sectionValueIndex
                                            sectionAnchorX = sectionValueIndex * pixelsPerSection
                                            scope.launch { sectionSnap.snapTo(0f) }
                                        }
                                    }
                                    val delta = fingerX - sectionAnchorX
                                    // The ±0.1px nudge keeps a finger resting on a section boundary
                                    // from flickering between the two neighbours.
                                    val snappedSections =
                                        ((if (delta >= 0f) delta + 0.1f else delta - 0.1f) / pixelsPerSection).toInt()
                                    val targetDelta = snappedSections * pixelsPerSection
                                    sectionDelta = delta
                                    if ((sectionAnchorIndex + snappedSections).coerceIn(0, sectionCount) != sectionValueIndex) {
                                        startSectionSnap(targetDelta, width)
                                    } else if (sectionSnapJob?.isActive != true) {
                                        val thumb = (
                                            sectionAnchorX + targetDelta +
                                                (delta - targetDelta) * (1f - SECTION_DRAG_MOVE_RATIO)
                                            ).coerceIn(0f, width)
                                        sectionThumbFraction = thumb / width
                                    }
                                    sectionLastFingerX = fingerX
                                } else {
                                    val calculatedValue = fractionToValue(fractionForValue)
                                    onValueChangeState(calculatedValue)
                                    hapticState.handleHapticFeedback(
                                        calculatedValue,
                                        valueRange,
                                        hapticEffect,
                                        hapticFeedback,
                                        allKeyPointFractions,
                                        hasCustomKeyPoints = keyPoints != null,
                                    )
                                }
                                glitter.onDrag(
                                    scope = scope,
                                    fraction = fractionForValue,
                                    delta = if (effectiveReverseDirection) -dragAmount else dragAmount,
                                    bothEdges = startFromMiddle,
                                )
                            },
                            onDragStarted = { offset ->
                                // A new touch owns the value: a fling still running from the previous
                                // release would otherwise keep writing its own target every frame.
                                flingJob?.cancel()
                                val capInset = restingInsetPx
                                val availableWidth = (layoutWidth - 2f * capInset).coerceAtLeast(0f)
                                val fraction = (animatedValueState.value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                                val effectiveFraction = if (effectiveReverseDirection) 1f - fraction else fraction
                                val thumbX = capInset + effectiveFraction * availableWidth
                                isThumbGrabbed = abs(offset.x - thumbX) <= capInset
                                isDragging = true
                                glitter.onDragStarted()
                                // MOVE_BY_FINGER (moveByFinger): the thumb keeps its value and only
                                // follows the displacement, instead of jumping under the finger.
                                dragOffset = if (moveByFinger) thumbX else offset.x
                                val visualFraction = horizontalVisualFraction(offset.x, layoutWidth, capInset)
                                val fractionForValue = if (effectiveReverseDirection) 1f - visualFraction else visualFraction
                                if (steps > 0) {
                                    // SectionAnimationHandler.initDragAnchor: the press itself already
                                    // resolves the value to the section under the finger, and that
                                    // section becomes the anchor every later offset is measured from.
                                    val width = (layoutWidth - 2f * restingInsetPx).coerceAtLeast(1f)
                                    val pixelsPerSection = width / sectionCount
                                    // Anchored in the same space the moves accumulate in, so a
                                    // `moveByFinger` press does not shift the anchor off the thumb.
                                    val pressed = horizontalVisualFraction(dragOffset, layoutWidth, capInset)
                                    val fingerX = (if (effectiveReverseDirection) 1f - pressed else pressed) * width
                                    val anchorIndex = (fingerX / pixelsPerSection).roundToInt().coerceIn(0, sectionCount)
                                    sectionAnchorIndex = anchorIndex
                                    sectionAnchorX = anchorIndex * pixelsPerSection
                                    sectionValueIndex = anchorIndex
                                    sectionDragDirection = if (fingerX < sectionAnchorX) -1 else 1
                                    sectionLastFingerX = fingerX
                                    sectionDelta = 0f
                                    sectionAnimStartX = sectionAnchorX
                                    sectionAnimEndX = sectionAnchorX
                                    sectionThumbFraction = sectionAnchorX / width
                                    sectionSnapJob?.cancel()
                                    sectionReleaseJob?.cancel()
                                    sectionReleasing = false
                                    scope.launch { sectionSnap.snapTo(0f) }
                                    val anchorValue = sectionValue(anchorIndex)
                                    onValueChangeState(anchorValue)
                                    hapticState.reset(anchorValue)
                                } else {
                                    val calculatedValue = fractionToValue(fractionForValue)
                                    onValueChangeState(calculatedValue)
                                    hapticState.reset(calculatedValue)
                                }
                            },
                            onDragStopped = { velocity ->
                                isDragging = false
                                isThumbGrabbed = false
                                if (steps > 0) {
                                    // COUISectionSeekBar.handleMotionEventUp: no physics fling, the
                                    // release snaps to the section nearest the last finger position
                                    // and glides the thumb there over RELEASE_SNAP_DURATION.
                                    val width = (layoutWidth - 2f * restingInsetPx).coerceAtLeast(1f)
                                    val pixelsPerSection = width / sectionCount
                                    val index = (sectionLastFingerX / pixelsPerSection).roundToInt().coerceIn(0, sectionCount)
                                    sectionSnapJob?.cancel()
                                    if (index != sectionValueIndex) {
                                        sectionValueIndex = index
                                        onValueChangeState(sectionValue(index))
                                    }
                                    sectionReleaseJob?.cancel()
                                    sectionReleaseFrom = sectionThumbFraction
                                    sectionReleaseTo = index.toFloat() / sectionCount
                                    sectionReleasing = true
                                    sectionReleaseJob = scope.launch {
                                        animate(0f, 1f, animationSpec = tween(150)) { fraction, _ ->
                                            sectionThumbFraction = lerp(sectionReleaseFrom, sectionReleaseTo, fraction)
                                        }
                                        sectionThumbFraction = sectionReleaseTo
                                        sectionReleasing = false
                                    }
                                    onValueChangeFinishedState?.invoke()
                                } else {
                                    val from = horizontalVisualFraction(dragOffset, layoutWidth, restingInsetPx)
                                    // COUISeekBar physics: a release faster than PHYSICAL_VELOCITY_LIMIT
                                    // keeps sliding (PhysicsAnimationHandler) instead of stopping dead.
                                    // A fling under linear damping travels `velocity / damping` pixels, and
                                    // FlingBehavior is built over the view's full length, not the cap-inset
                                    // span, so the fraction it adds is that distance over the layout width.
                                    val projected = if (layoutWidth == 0) {
                                        from
                                    } else {
                                        (from + velocity * SliderDefaults.FLING_PROJECTION_SECONDS / layoutWidth)
                                            .coerceIn(0f, 1f)
                                    }
                                    if (abs(velocity) >= SliderDefaults.FLING_VELOCITY_LIMIT && projected != from) {
                                        flingTarget(projected, from, velocity)
                                    } else {
                                        onValueChangeFinishedState?.invoke()
                                    }
                                }
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
                    val clamped = target.coerceIn(valueRange.start, valueRange.endInclusive)
                    onValueChangeState(clamped)
                    true
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        SliderTrack(
            backgroundColor = colors.backgroundColor(enabled),
            foregroundColor = colors.foregroundColor(enabled),
            thumbColor = colors.thumbColor(enabled),
            keyPointColor = colors.keyPointColor(),
            keyPointForegroundColor = colors.keyPointForegroundColor(),
            valueProvider = { animatedValueState.value },
            valueRange = valueRange,
            thumbFractionProvider = if (steps > 0) {
                { if (isDragging || sectionReleasing) sectionThumbFraction else null }
            } else {
                null
            },
            isDragging = isDragging || pressLight.isPressed,
            isVertical = false,
            enabled = enabled,
            showKeyPoints = showKeyPoints,
            stepFractions = keyPointFractions,
            thumbScaleProvider = { thumbScaleState.value },
            reverseDirection = effectiveReverseDirection,
            secondaryProgressFraction = secondaryProgress?.let { secondaryProgressFraction(it, valueRange) },
            secondaryProgressColor = colors.secondaryProgressColor(enabled),
            showProgress = showProgress,
            showThumb = showThumb,
            startFromMiddle = startFromMiddle,
            deformationProvider = { deformation },
            glitter = glitter,
            pressLight = pressLight,
            modifier = Modifier.fillMaxWidth().height(height),
        )
    }
}

/**
 * A vertical [Slider] component with COUI style.
 *
 * @param value The current value of the [Slider]. If outside of [valueRange] provided, value will be coerced to this range.
 * @param onValueChange The callback to be called when the value changes.
 * @param modifier The modifier to be applied to the [Slider].
 * @param enabled Whether the [Slider] is enabled.
 * @param valueRange Range of values that this slider can take. The passed [value] will be coerced to this range.
 * @param steps If positive, specifies the amount of discrete allowable values between the endpoints of [valueRange].
 * @param onValueChangeFinished Called when value change has ended.
 * @param reverseDirection Controls the direction of this slider. When false (default), slider increases from bottom to top.
 *   When true, slider increases from top to bottom.
 * @param width The rest width of the vertical [Slider] track. While dragging, the inactive track
 *   enlarges to 1.4x of this width and is drawn beyond the layout bounds, following COUI behavior.
 * @param colors The [SliderColors] of the [Slider].
 * @param effect Whether to show the effect of the [Slider].
 * @param hapticEffect The haptic effect of the [Slider].
 * @param showKeyPoints Whether to show the key points (step indicators) on the slider. Only works when [keyPoints] is not null.
 * @param keyPoints Custom key point values to display on the slider. If null, uses step positions from [steps] parameter.
 *   Values should be within [valueRange].
 * @param magnetThreshold The magnetic snap threshold as a fraction (0.0 to 1.0). When the slider value is within this
 *   distance from a key point, it will snap to that point. Default is 0.02 (2%). Only applies when [keyPoints] is set.
 * @param secondaryProgress The optional secondary progress ("intent tier") drawn behind the primary active track.
 *   It is coerced into [valueRange]; the part beyond the primary value stays visible, matching the secondary layer
 *   [IntentSeekBar] draws. `null` (the default) draws nothing. New in 1.2.0.
 */
@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    reverseDirection: Boolean = false,
    width: Dp = SliderDefaults.MinHeight,
    colors: SliderColors = SliderDefaults.sliderColors(),
    effect: Boolean = false,
    hapticEffect: SliderDefaults.SliderHapticEffect = SliderDefaults.DefaultHapticEffect,
    showKeyPoints: Boolean = false,
    keyPoints: List<Float>? = null,
    magnetThreshold: Float = 0.02f,
    secondaryProgress: Float? = null,
    showProgress: Boolean = true,
    showThumb: Boolean = true,
) {
    require(steps >= 0) { "steps should be >= 0" }
    require(valueRange.start < valueRange.endInclusive) { "valueRange start should be less than end" }

    val hapticFeedback = LocalHapticFeedback.current
    val onValueChangeState by rememberUpdatedState(onValueChange)
    val onValueChangeFinishedState by rememberUpdatedState(onValueChangeFinished)
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isThumbGrabbed by remember { mutableStateOf(false) }
    var isHoveringThumb by remember { mutableStateOf(false) }
    val hapticState = remember { SliderHapticState() }
    val interactionSource = remember { MutableInteractionSource() }
    var layoutWidth by remember { mutableIntStateOf(0) }
    var layoutHeight by remember { mutableIntStateOf(0) }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()
    val glitter = remember { SliderGlitter() }
    val pressLight = remember { SliderPressLight() }
    val flingPosition = remember { Animatable(0f) }
    var flingJob by remember { mutableStateOf<Job?>(null) }

    val coercedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)

    val progressAnimationSpec = remember(isDragging) {
        if (isDragging) {
            spring(dampingRatio = 0.9f, stiffness = 1755f)
        } else {
            spring<Float>(dampingRatio = 0.96f, stiffness = 322f)
        }
    }

    val animatedValueState = animateFloatAsState(coercedValue, progressAnimationSpec)
    // COUISeekBarCore.touchAnim enlarges the thumb on ACTION_DOWN wherever the slider is touched.
    val thumbScaleTarget = if (pressLight.isPressed || isPressed || (isDragging && isThumbGrabbed) || isHoveringThumb) {
        ThumbPressedScale
    } else {
        1f
    }
    val thumbScaleState = animateFloatAsState(
        targetValue = thumbScaleTarget,
        animationSpec = if (thumbScaleTarget > 1f) ThumbScaleEnlargeSpec else ThumbScaleShrinkSpec,
        label = "SliderThumbScale",
    )

    val stepFractions = remember(steps) { stepsToTickFractions(steps) }

    val keyPointFractions = remember(keyPoints, stepFractions, valueRange, showKeyPoints) {
        computeKeyPointFractions(keyPoints, stepFractions, valueRange, showKeyPoints)
    }

    val allKeyPointFractions = remember(keyPoints, stepFractions, valueRange) {
        computeAllKeyPointFractions(keyPoints, stepFractions, valueRange)
    }

    val fractionToValueVertical = remember(valueRange, steps, stepFractions, allKeyPointFractions, magnetThreshold) {
        { fraction: Float ->
            resolveValueFromFraction(
                fraction = fraction,
                valueRange = valueRange,
                steps = steps,
                allKeyPointFractions = allKeyPointFractions,
                magnetThreshold = magnetThreshold,
            )
        }
    }

    // COUIVerticalSeekBar runs the same PhysicsAnimationHandler fling as the horizontal bar:
    // a release faster than PHYSICAL_VELOCITY_LIMIT travels `velocity / 15` pixels along the
    // view's full height.
    val flingTarget: (Float, Float, Float) -> Unit = { target, from, velocity ->
        flingJob?.cancel()
        flingJob = scope.launch {
            flingPosition.snapTo(from)
            var lastFraction = if (reverseDirection) from else 1f - from
            flingPosition.animateTo(target, TrackGrowAnimationSpec) {
                val fraction = if (reverseDirection) flingPosition.value else 1f - flingPosition.value
                onValueChangeState(fractionToValueVertical(fraction))
                glitter.onFling(scope, fraction, lastFraction, velocity, bothEdges = false)
                lastFraction = fraction
            }
            onValueChangeFinishedState?.invoke()
        }
    }

    val currentLayoutWidth = layoutWidth
    val currentLayoutHeight = layoutHeight
    Box(
        modifier = modifier
            .sliderPressLight(pressLight, enabled)
            .defaultMinSize(minHeight = SliderDefaults.MinTouchHeight)
            .then(
                if (enabled) {
                    Modifier
                        .onSizeChanged {
                            layoutWidth = it.width
                            layoutHeight = it.height
                        }
                        .pointerInput(reverseDirection, valueRange) {
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

                                    val capInset = currentLayoutWidth * TrackCapInsetRatio
                                    val availableHeight = (currentLayoutHeight - 2f * capInset).coerceAtLeast(0f)
                                    val hitRadius = capInset

                                    val position = change.position
                                    val fraction =
                                        (animatedValueState.value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                                    val effectiveFraction = if (reverseDirection) fraction else 1f - fraction
                                    val thumbY = capInset + effectiveFraction * availableHeight

                                    val isOver = abs(position.y - thumbY) <= hitRadius
                                    if (isHoveringThumb != isOver) {
                                        isHoveringThumb = isOver
                                    }
                                }
                            }
                        }
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { dragAmount ->
                                dragOffset += dragAmount
                                val visualFraction = verticalVisualFraction(dragOffset, layoutHeight, layoutWidth)
                                val fractionForValue = if (reverseDirection) visualFraction else 1f - visualFraction
                                val calculatedValue = fractionToValueVertical(fractionForValue)
                                onValueChangeState(calculatedValue)
                                glitter.onDrag(
                                    scope = scope,
                                    fraction = fractionForValue,
                                    delta = if (reverseDirection) dragAmount else -dragAmount,
                                    bothEdges = false,
                                )
                                hapticState.handleHapticFeedback(
                                    calculatedValue,
                                    valueRange,
                                    hapticEffect,
                                    hapticFeedback,
                                    allKeyPointFractions,
                                    hasCustomKeyPoints = keyPoints != null,
                                )
                            },
                            onDragStarted = { offset ->
                                // A new touch owns the value: a fling still running from the previous
                                // release would otherwise keep writing its own target every frame.
                                flingJob?.cancel()
                                val capInset = layoutWidth * TrackCapInsetRatio
                                val availableHeight = (layoutHeight - 2f * capInset).coerceAtLeast(0f)
                                val fraction = (animatedValueState.value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                                val effectiveFraction = if (reverseDirection) fraction else 1f - fraction
                                val thumbY = capInset + effectiveFraction * availableHeight
                                isThumbGrabbed = abs(offset.y - thumbY) <= capInset
                                isDragging = true
                                glitter.onDragStarted()
                                dragOffset = offset.y
                                val visualFraction = verticalVisualFraction(offset.y, layoutHeight, layoutWidth)
                                val fractionForValue = if (reverseDirection) visualFraction else 1f - visualFraction
                                val calculatedValue = fractionToValueVertical(fractionForValue)
                                onValueChangeState(calculatedValue)
                                hapticState.reset(calculatedValue)
                            },
                            onDragStopped = { velocity ->
                                isDragging = false
                                isThumbGrabbed = false
                                val from = verticalVisualFraction(dragOffset, layoutHeight, layoutWidth)
                                val projected = if (layoutHeight == 0) {
                                    from
                                } else {
                                    (from + velocity * SliderDefaults.FLING_PROJECTION_SECONDS / layoutHeight)
                                        .coerceIn(0f, 1f)
                                }
                                // Same section rule as the horizontal bar: only a stepped slider maps to
                                // COUISectionSeekBar, which snaps instead of running the physics fling.
                                // Custom key points are magnet targets on a continuous bar, so they
                                // keep the fling.
                                if (steps == 0 &&
                                    abs(velocity) >= SliderDefaults.FLING_VELOCITY_LIMIT &&
                                    projected != from
                                ) {
                                    flingTarget(projected, from, velocity)
                                } else {
                                    onValueChangeFinishedState?.invoke()
                                }
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
                    val clamped = target.coerceIn(valueRange.start, valueRange.endInclusive)
                    onValueChangeState(clamped)
                    true
                }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        SliderTrack(
            backgroundColor = colors.backgroundColor(enabled),
            foregroundColor = colors.foregroundColor(enabled),
            thumbColor = colors.thumbColor(enabled),
            keyPointColor = colors.keyPointColor(),
            keyPointForegroundColor = colors.keyPointForegroundColor(),
            valueProvider = { animatedValueState.value },
            valueRange = valueRange,
            isDragging = isDragging || pressLight.isPressed,
            isVertical = true,
            enabled = enabled,
            showKeyPoints = showKeyPoints,
            stepFractions = keyPointFractions,
            thumbScaleProvider = { thumbScaleState.value },
            reverseDirection = reverseDirection,
            secondaryProgressFraction = secondaryProgress?.let { secondaryProgressFraction(it, valueRange) },
            secondaryProgressColor = colors.secondaryProgressColor(enabled),
            glitter = glitter,
            pressLight = pressLight,
            modifier = Modifier.width(width).fillMaxHeight(),
        )
    }
}

/**
 * A [RangeSlider] component with COUI style.
 *
 * Range Sliders expand upon [Slider] using the same concepts but allow the user to select 2 values.
 * The two values are still bounded by the value range but they also cannot cross each other.
 *
 * @param value Current values of the RangeSlider. If either value is outside of [valueRange] provided, it will be coerced to this range.
 * @param onValueChange Lambda in which values should be updated.
 * @param modifier The modifier to be applied to the [RangeSlider].
 * @param enabled Whether the [RangeSlider] is enabled.
 * @param valueRange Range of values that Range Slider values can take. Passed [value] will be coerced to this range.
 * @param steps If positive, specifies the amount of discrete allowable values between the endpoints of [valueRange].
 * @param onValueChangeFinished Lambda to be invoked when value change has ended.
 * @param height The rest height of the [RangeSlider] track. While dragging, the inactive track
 *   enlarges to 1.4x of this height and is drawn beyond the layout bounds, following COUI behavior.
 * @param colors The [SliderColors] of the [RangeSlider].
 * @param hapticEffect The haptic effect of the [RangeSlider].
 * @param showKeyPoints Whether to show the key points (step indicators) on the slider. Only works when [keyPoints] is not null.
 * @param keyPoints Custom key point values to display on the slider. If null, uses step positions from [steps] parameter.
 *   Values should be within [valueRange].
 * @param magnetThreshold The magnetic snap threshold as a fraction (0.0 to 1.0). When the slider value is within this
 *   distance from a key point, it will snap to that point. Default is 0.02 (2%). Only applies when [keyPoints] is set.
 * @param secondaryProgress The optional secondary progress ("intent tier") drawn behind the primary active track.
 *   It is coerced into [valueRange]; the part beyond the primary value stays visible, matching the secondary layer
 *   [IntentSeekBar] draws. `null` (the default) draws nothing. New in 1.2.0.
 */
@Composable
fun RangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    height: Dp = SliderDefaults.MinHeight,
    colors: SliderColors = SliderDefaults.sliderColors(),
    hapticEffect: SliderDefaults.SliderHapticEffect = SliderDefaults.DefaultHapticEffect,
    showKeyPoints: Boolean = false,
    keyPoints: List<Float>? = null,
    magnetThreshold: Float = 0.02f,
    secondaryProgress: Float? = null,
    showProgress: Boolean = true,
    showThumb: Boolean = true,
) {
    require(steps >= 0) { "steps should be >= 0" }
    require(valueRange.start < valueRange.endInclusive) { "valueRange start should be less than end" }

    val hapticFeedback = LocalHapticFeedback.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val onValueChangeState by rememberUpdatedState(onValueChange)
    val onValueChangeFinishedState by rememberUpdatedState(onValueChangeFinished)
    val density = LocalDensity.current
    // The touch target is 36dp tall (couiSeekBarMinHeight) but the track keeps its own height, so
    // the thumb inset and the value mapping are derived from that thickness, not from the layout.
    val trackHeightPx = with(density) { height.toPx() }
    val restingInsetPx = trackHeightPx * TrackCapInsetRatio
    var startDragOffset by remember { mutableFloatStateOf(0f) }
    var endDragOffset by remember { mutableFloatStateOf(0f) }
    var isDraggingStart by remember { mutableStateOf(false) }
    var isDraggingEnd by remember { mutableStateOf(false) }
    var isHoveringStartThumb by remember { mutableStateOf(false) }
    var isHoveringEndThumb by remember { mutableStateOf(false) }
    val isDragging by remember { derivedStateOf { isDraggingStart || isDraggingEnd } }
    val hapticState = remember { RangeSliderHapticState() }
    val scope = rememberCoroutineScope()
    val glitter = remember { SliderGlitter() }
    val pressLight = remember { SliderPressLight() }
    val interactionSource = remember { MutableInteractionSource() }
    var lastDraggedIsStart by remember { mutableStateOf(true) }
    var layoutWidth by remember { mutableIntStateOf(0) }
    val isPressed by interactionSource.collectIsPressedAsState()

    var currentStartValue by remember { mutableFloatStateOf(value.start) }
    var currentEndValue by remember { mutableFloatStateOf(value.endInclusive) }

    if (!isDragging) {
        currentStartValue = value.start
        currentEndValue = value.endInclusive
    }

    val coercedStart = currentStartValue.coerceIn(valueRange.start, valueRange.endInclusive)
    val coercedEnd = currentEndValue.coerceIn(valueRange.start, valueRange.endInclusive)

    val progressAnimationSpec = remember(isDragging) {
        if (isDragging) {
            spring(dampingRatio = 0.9f, stiffness = 1755f)
        } else {
            spring<Float>(dampingRatio = 0.96f, stiffness = 322f)
        }
    }

    val animatedStartValueState = animateFloatAsState(coercedStart, progressAnimationSpec)
    val animatedEndValueState = animateFloatAsState(coercedEnd, progressAnimationSpec)
    // The touch enlarges the thumb a drag from that point would pick, as touchAnim does on ACTION_DOWN.
    val pressPicksStart = pressLight.isPressed &&
        rangePressPicksStart(
            touchX = pressLight.touchPosition.x,
            layoutWidth = layoutWidth,
            capInset = restingInsetPx,
            startFraction = (coercedStart - valueRange.start) / (valueRange.endInclusive - valueRange.start),
            endFraction = (coercedEnd - valueRange.start) / (valueRange.endInclusive - valueRange.start),
            isRtl = isRtl,
            lastDraggedIsStart = lastDraggedIsStart,
        )
    val pressPicksEnd = pressLight.isPressed && !pressPicksStart
    val startThumbScaleTarget = if (isDraggingStart || pressPicksStart || isPressed || isHoveringStartThumb) ThumbPressedScale else 1f
    val startThumbScaleState = animateFloatAsState(
        targetValue = startThumbScaleTarget,
        animationSpec = if (startThumbScaleTarget > 1f) ThumbScaleEnlargeSpec else ThumbScaleShrinkSpec,
        label = "RangeSliderStartThumbScale",
    )
    val endThumbScaleTarget = if (isDraggingEnd || pressPicksEnd || isPressed || isHoveringEndThumb) ThumbPressedScale else 1f
    val endThumbScaleState = animateFloatAsState(
        targetValue = endThumbScaleTarget,
        animationSpec = if (endThumbScaleTarget > 1f) ThumbScaleEnlargeSpec else ThumbScaleShrinkSpec,
        label = "RangeSliderEndThumbScale",
    )

    val stepFractions = remember(steps) { stepsToTickFractions(steps) }

    val keyPointFractions = remember(keyPoints, stepFractions, valueRange, showKeyPoints) {
        computeKeyPointFractions(keyPoints, stepFractions, valueRange, showKeyPoints)
    }

    val allKeyPointFractions = remember(keyPoints, stepFractions, valueRange) {
        computeAllKeyPointFractions(keyPoints, stepFractions, valueRange)
    }

    val fractionToValueRange = remember(valueRange, steps, stepFractions, allKeyPointFractions, magnetThreshold) {
        { fraction: Float ->
            resolveValueFromFraction(
                fraction = fraction,
                valueRange = valueRange,
                steps = steps,
                allKeyPointFractions = allKeyPointFractions,
                magnetThreshold = magnetThreshold,
            )
        }
    }

    val currentLayoutWidth = layoutWidth
    Box(
        modifier = modifier
            .sliderPressLight(pressLight, enabled)
            .defaultMinSize(minHeight = SliderDefaults.MinTouchHeight)
            .then(
                if (enabled) {
                    Modifier
                        .onSizeChanged {
                            layoutWidth = it.width
                        }
                        .pointerInput(isRtl, valueRange) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.last()

                                    if (event.type == PointerEventType.Exit ||
                                        event.type == PointerEventType.Release ||
                                        change.type != PointerType.Mouse
                                    ) {
                                        isHoveringStartThumb = false
                                        isHoveringEndThumb = false
                                        continue
                                    }

                                    val capInset = restingInsetPx
                                    val availableWidth = (currentLayoutWidth - 2f * capInset).coerceAtLeast(0f)
                                    val hitRadius = capInset

                                    val position = change.position
                                    val startFraction = (animatedStartValueState.value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                                    val endFraction = (animatedEndValueState.value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                                    val effectiveStartFraction = if (isRtl) 1f - startFraction else startFraction
                                    val effectiveEndFraction = if (isRtl) 1f - endFraction else endFraction
                                    val startThumbX = capInset + effectiveStartFraction * availableWidth
                                    val endThumbX = capInset + effectiveEndFraction * availableWidth

                                    val isOverStart = abs(position.x - startThumbX) <= hitRadius
                                    val isOverEnd = abs(position.x - endThumbX) <= hitRadius

                                    if (isHoveringStartThumb != isOverStart) {
                                        isHoveringStartThumb = isOverStart
                                    }
                                    if (isHoveringEndThumb != isOverEnd) {
                                        isHoveringEndThumb = isOverEnd
                                    }
                                }
                            }
                        }
                        .hoverable(interactionSource = interactionSource, enabled = enabled)
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { dragAmount ->
                                if (isDraggingStart) {
                                    lastDraggedIsStart = true
                                    val tentativeStartOffset = startDragOffset + dragAmount
                                    val visualFractionStart = horizontalVisualFraction(tentativeStartOffset, layoutWidth, restingInsetPx)
                                    val fractionForValue = if (isRtl) 1f - visualFractionStart else visualFractionStart
                                    val newStart = fractionToValueRange(fractionForValue).coerceAtMost(currentEndValue)
                                    val crossCondition = if (isRtl) dragAmount < 0f else dragAmount > 0f

                                    if (newStart >= currentEndValue && crossCondition && currentStartValue == currentEndValue) {
                                        isDraggingStart = false
                                        isDraggingEnd = true

                                        endDragOffset = tentativeStartOffset
                                        hapticState.resetEnd(currentEndValue)
                                        hapticState.inheritEndKeyPoint()

                                        val visualFractionEnd = horizontalVisualFraction(endDragOffset, layoutWidth, restingInsetPx)
                                        val fractionForValueEnd = if (isRtl) 1f - visualFractionEnd else visualFractionEnd
                                        val newEnd = fractionToValueRange(fractionForValueEnd).coerceAtLeast(currentStartValue)
                                        currentEndValue = newEnd
                                        onValueChangeState(currentStartValue..newEnd)
                                        hapticState.handleEndHapticFeedback(
                                            newEnd,
                                            valueRange,
                                            hapticEffect,
                                            hapticFeedback,
                                            allKeyPointFractions,
                                            hasCustomKeyPoints = keyPoints != null,
                                        )
                                    } else {
                                        startDragOffset = tentativeStartOffset
                                        currentStartValue = newStart
                                        onValueChangeState(newStart..currentEndValue)
                                        hapticState.handleStartHapticFeedback(
                                            newStart,
                                            valueRange,
                                            hapticEffect,
                                            hapticFeedback,
                                            allKeyPointFractions,
                                            hasCustomKeyPoints = keyPoints != null,
                                        )
                                    }
                                } else if (isDraggingEnd) {
                                    lastDraggedIsStart = false
                                    val tentativeEndOffset = endDragOffset + dragAmount
                                    val visualFractionEnd = horizontalVisualFraction(tentativeEndOffset, layoutWidth, restingInsetPx)
                                    val fractionForValue = if (isRtl) 1f - visualFractionEnd else visualFractionEnd
                                    val newEnd = fractionToValueRange(fractionForValue).coerceAtLeast(currentStartValue)
                                    val crossCondition = if (isRtl) dragAmount > 0f else dragAmount < 0f

                                    if (newEnd <= currentStartValue && crossCondition && currentStartValue == currentEndValue) {
                                        isDraggingEnd = false
                                        isDraggingStart = true
                                        startDragOffset = tentativeEndOffset
                                        hapticState.resetStart(currentStartValue)
                                        hapticState.inheritStartKeyPoint()

                                        val visualFractionStart = horizontalVisualFraction(startDragOffset, layoutWidth, restingInsetPx)
                                        val fractionForValueStart = if (isRtl) 1f - visualFractionStart else visualFractionStart
                                        val newStart = fractionToValueRange(fractionForValueStart).coerceAtMost(currentEndValue)
                                        currentStartValue = newStart
                                        onValueChangeState(newStart..currentEndValue)
                                        hapticState.handleStartHapticFeedback(
                                            newStart,
                                            valueRange,
                                            hapticEffect,
                                            hapticFeedback,
                                            allKeyPointFractions,
                                            hasCustomKeyPoints = keyPoints != null,
                                        )
                                    } else {
                                        endDragOffset = tentativeEndOffset
                                        currentEndValue = newEnd
                                        onValueChangeState(currentStartValue..newEnd)
                                        hapticState.handleEndHapticFeedback(
                                            newEnd,
                                            valueRange,
                                            hapticEffect,
                                            hapticFeedback,
                                            allKeyPointFractions,
                                            hasCustomKeyPoints = keyPoints != null,
                                        )
                                    }
                                }
                                val span = valueRange.endInclusive - valueRange.start
                                val activeValue = if (isDraggingStart) currentStartValue else currentEndValue
                                glitter.onDrag(
                                    scope = scope,
                                    fraction = (activeValue - valueRange.start) / span,
                                    delta = if (isRtl) -dragAmount else dragAmount,
                                    bothEdges = true,
                                )
                            },
                            onDragStarted = { offset ->
                                glitter.onDragStarted()
                                val capInset = restingInsetPx
                                val availableWidth = (layoutWidth - 2f * capInset).coerceAtLeast(0f)
                                val startFraction =
                                    (currentStartValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                                val endFraction =
                                    (currentEndValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                                val effectiveStartFraction = if (isRtl) 1f - startFraction else startFraction
                                val effectiveEndFraction = if (isRtl) 1f - endFraction else endFraction
                                val startPos = capInset + effectiveStartFraction * availableWidth
                                val endPos = capInset + effectiveEndFraction * availableWidth

                                val hitRadius = capInset
                                val isOnStartThumb = abs(offset.x - startPos) <= hitRadius
                                val isOnEndThumb = abs(offset.x - endPos) <= hitRadius

                                when {
                                    isOnStartThumb && !isOnEndThumb -> {
                                        isDraggingStart = true
                                        startDragOffset = offset.x
                                        hapticState.resetStart(coercedStart)
                                    }

                                    !isOnStartThumb && isOnEndThumb -> {
                                        isDraggingEnd = true
                                        endDragOffset = offset.x
                                        hapticState.resetEnd(coercedEnd)
                                    }

                                    isOnStartThumb && isOnEndThumb -> {
                                        if (lastDraggedIsStart) {
                                            isDraggingStart = true
                                            startDragOffset = offset.x
                                            hapticState.resetStart(coercedStart)
                                        } else {
                                            isDraggingEnd = true
                                            endDragOffset = offset.x
                                            hapticState.resetEnd(coercedEnd)
                                        }
                                    }

                                    else -> {
                                        val diffStart = abs(offset.x - startPos)
                                        val diffEnd = abs(offset.x - endPos)
                                        if (diffStart <= diffEnd) {
                                            isDraggingStart = true
                                            startDragOffset = offset.x
                                            hapticState.resetStart(coercedStart)
                                        } else {
                                            isDraggingEnd = true
                                            endDragOffset = offset.x
                                            hapticState.resetEnd(coercedEnd)
                                        }
                                    }
                                }
                            },
                            onDragStopped = {
                                isDraggingStart = false
                                isDraggingEnd = false
                                onValueChangeFinishedState?.invoke()
                            },
                        )
                        .indication(interactionSource, null)
                } else {
                    Modifier
                },
            )
            .semantics {
                stateDescription = "$coercedStart-$coercedEnd"
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        RangeSliderTrack(
            backgroundColor = colors.backgroundColor(enabled),
            foregroundColor = colors.foregroundColor(enabled),
            thumbColor = colors.thumbColor(enabled),
            keyPointColor = colors.keyPointColor(),
            keyPointForegroundColor = colors.keyPointForegroundColor(),
            valueStartProvider = { animatedStartValueState.value },
            valueEndProvider = { animatedEndValueState.value },
            startThumbScaleProvider = { startThumbScaleState.value },
            endThumbScaleProvider = { endThumbScaleState.value },
            valueRange = valueRange,
            isDragging = isDragging || pressLight.isPressed,
            enabled = enabled,
            showKeyPoints = showKeyPoints,
            stepFractions = keyPointFractions,
            isRtl = isRtl,
            secondaryProgressFraction = secondaryProgress?.let { secondaryProgressFraction(it, valueRange) },
            secondaryProgressColor = colors.secondaryProgressColor(enabled),
            glitter = glitter,
            pressLight = pressLight,
            modifier = Modifier.fillMaxWidth().height(height),
        )
    }
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds startFromMiddle and moveByFinger.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    reverseDirection: Boolean = false,
    height: Dp = SliderDefaults.MinHeight,
    colors: SliderColors = SliderDefaults.sliderColors(),
    hapticEffect: SliderDefaults.SliderHapticEffect = SliderDefaults.DefaultHapticEffect,
    showKeyPoints: Boolean = false,
    keyPoints: List<Float>? = null,
    magnetThreshold: Float = 0.02f,
    secondaryProgress: Float? = null,
    showProgress: Boolean = true,
    showThumb: Boolean = true,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        reverseDirection = reverseDirection,
        height = height,
        colors = colors,
        hapticEffect = hapticEffect,
        showKeyPoints = showKeyPoints,
        keyPoints = keyPoints,
        magnetThreshold = magnetThreshold,
        secondaryProgress = secondaryProgress,
        showProgress = showProgress,
        showThumb = showThumb,
    )
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds showProgress and showThumb.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    reverseDirection: Boolean = false,
    height: Dp = SliderDefaults.MinHeight,
    colors: SliderColors = SliderDefaults.sliderColors(),
    hapticEffect: SliderDefaults.SliderHapticEffect = SliderDefaults.DefaultHapticEffect,
    showKeyPoints: Boolean = false,
    keyPoints: List<Float>? = null,
    magnetThreshold: Float = 0.02f,
    secondaryProgress: Float? = null,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        reverseDirection = reverseDirection,
        height = height,
        colors = colors,
        hapticEffect = hapticEffect,
        showKeyPoints = showKeyPoints,
        keyPoints = keyPoints,
        magnetThreshold = magnetThreshold,
        secondaryProgress = secondaryProgress,
    )
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds a secondaryProgress parameter.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    reverseDirection: Boolean = false,
    height: Dp = SliderDefaults.MinHeight,
    colors: SliderColors = SliderDefaults.sliderColors(),
    hapticEffect: SliderDefaults.SliderHapticEffect = SliderDefaults.DefaultHapticEffect,
    showKeyPoints: Boolean = false,
    keyPoints: List<Float>? = null,
    magnetThreshold: Float = 0.02f,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        reverseDirection = reverseDirection,
        height = height,
        colors = colors,
        hapticEffect = hapticEffect,
        showKeyPoints = showKeyPoints,
        keyPoints = keyPoints,
        magnetThreshold = magnetThreshold,
    )
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds a secondaryProgress parameter.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    reverseDirection: Boolean = false,
    width: Dp = SliderDefaults.MinHeight,
    colors: SliderColors = SliderDefaults.sliderColors(),
    effect: Boolean = false,
    hapticEffect: SliderDefaults.SliderHapticEffect = SliderDefaults.DefaultHapticEffect,
    showKeyPoints: Boolean = false,
    keyPoints: List<Float>? = null,
    magnetThreshold: Float = 0.02f,
) {
    VerticalSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        reverseDirection = reverseDirection,
        width = width,
        colors = colors,
        effect = effect,
        hapticEffect = hapticEffect,
        showKeyPoints = showKeyPoints,
        keyPoints = keyPoints,
        magnetThreshold = magnetThreshold,
    )
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds a secondaryProgress parameter.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun RangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    height: Dp = SliderDefaults.MinHeight,
    colors: SliderColors = SliderDefaults.sliderColors(),
    hapticEffect: SliderDefaults.SliderHapticEffect = SliderDefaults.DefaultHapticEffect,
    showKeyPoints: Boolean = false,
    keyPoints: List<Float>? = null,
    magnetThreshold: Float = 0.02f,
) {
    RangeSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        height = height,
        colors = colors,
        hapticEffect = hapticEffect,
        showKeyPoints = showKeyPoints,
        keyPoints = keyPoints,
        magnetThreshold = magnetThreshold,
    )
}

/**
 * Internal slider track renderer, following the COUISeekBar.onDraw rendering model.
 */
@Composable
private fun SliderTrack(
    backgroundColor: Color,
    foregroundColor: Color,
    thumbColor: Color,
    keyPointColor: Color,
    keyPointForegroundColor: Color,
    valueProvider: () -> Float,
    valueRange: ClosedFloatingPointRange<Float>,
    thumbFractionProvider: (() -> Float?)? = null,
    isDragging: Boolean,
    isVertical: Boolean,
    enabled: Boolean,
    showKeyPoints: Boolean,
    stepFractions: FloatArray,
    thumbScaleProvider: () -> Float,
    reverseDirection: Boolean,
    secondaryProgressFraction: Float?,
    secondaryProgressColor: Color,
    glitter: SliderGlitter,
    pressLight: SliderPressLight,
    showProgress: Boolean = true,
    showThumb: Boolean = true,
    startFromMiddle: Boolean = false,
    deformationProvider: () -> Float = { 0f },
    modifier: Modifier = Modifier,
) {
    val trackGrowFraction by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = TrackGrowAnimationSpec,
        label = "SliderTrackGrow",
    )

    val squircleEnabled = isSquircleEnabled()
    val lightShader = rememberSpotLightShader(isRuntimeShaderSupported())
    val lightParams = SpotLightDefaults.opaqueMedium1(COUITheme.colorScheme.background.luminance() < 0.5f)
    Canvas(modifier = modifier) {
        val barHeight = size.height
        val barWidth = size.width
        val value = valueProvider()
        val thumbScale = thumbScaleProvider()
        // COUISectionSeekBar draws the thumb and the filled track from `sectionThumbX`, which during
        // a drag is the blended position rather than the value's own section.
        val fraction = thumbFractionProvider?.invoke()
            ?: ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))

        if (isVertical) {
            val trackWidth = barWidth
            val backgroundWidth = lerp(trackWidth, trackWidth * TrackEnlargeScale, trackGrowFraction)
            val capInset = trackWidth * TrackCapInsetRatio
            val availableHeight = (barHeight - 2f * capInset).coerceAtLeast(0f)
            val effectiveFraction = if (reverseDirection) fraction else (1f - fraction)
            val thumbCenterY = capInset + effectiveFraction * availableHeight
            val centerX = barWidth / 2f

            drawRoundRect(
                color = backgroundColor,
                topLeft = Offset(centerX - backgroundWidth / 2f, capInset - backgroundWidth / 2f),
                size = Size(backgroundWidth, availableHeight + backgroundWidth),
                cornerRadius = CornerRadius(backgroundWidth / 2f),
            )

            if (secondaryProgressFraction != null) {
                val halfTrackSecondary = trackWidth / 2f
                val effectiveSecondary = if (reverseDirection) secondaryProgressFraction else 1f - secondaryProgressFraction
                val secondaryCenterY = capInset + effectiveSecondary * availableHeight
                val secondaryTop = if (reverseDirection) capInset - halfTrackSecondary else secondaryCenterY - halfTrackSecondary
                val secondaryBottom = if (reverseDirection) secondaryCenterY + halfTrackSecondary else barHeight - capInset + halfTrackSecondary
                drawRoundRect(
                    color = secondaryProgressColor,
                    topLeft = Offset(centerX - halfTrackSecondary, secondaryTop),
                    size = Size(trackWidth, (secondaryBottom - secondaryTop).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius(halfTrackSecondary),
                )
            }

            val halfTrack = trackWidth / 2f
            val progressTop = if (reverseDirection) capInset - halfTrack else thumbCenterY - halfTrack
            val progressBottom = if (reverseDirection) thumbCenterY + halfTrack else barHeight - capInset + halfTrack
            drawRoundRect(
                color = foregroundColor,
                topLeft = Offset(centerX - halfTrack, progressTop),
                size = Size(trackWidth, (progressBottom - progressTop).coerceAtLeast(0f)),
                cornerRadius = CornerRadius(halfTrack),
            )
            glitter.brush(progressTop, progressBottom, horizontal = false, valueEndAtFar = reverseDirection)?.let {
                drawRoundRect(
                    brush = it,
                    topLeft = Offset(centerX - halfTrack, progressTop),
                    size = Size(trackWidth, progressBottom - progressTop),
                    cornerRadius = CornerRadius(halfTrack),
                )
            }

            if (showKeyPoints && stepFractions.isNotEmpty()) {
                val keyPointRadius = SliderDefaults.KeyPointRadius.toPx()
                for (i in stepFractions.indices) {
                    val stepFraction = stepFractions[i]
                    val effectiveStep = if (reverseDirection) stepFraction else (1f - stepFraction)
                    val y = capInset + effectiveStep * availableHeight
                    val isSelected = if (reverseDirection) y <= thumbCenterY else y >= thumbCenterY
                    val kpColor = if (isSelected) keyPointForegroundColor else keyPointColor
                    drawCircle(
                        color = kpColor,
                        radius = keyPointRadius,
                        center = Offset(centerX, y),
                    )
                }
            }
            val thumbCenter = Offset(centerX, thumbCenterY)
            val thumbRadius = trackWidth * ThumbRadiusRatio * thumbScale
            if (!pressLight.drawTexturedThumb(this, thumbCenter, thumbRadius)) {
                drawThumbWithShadow(
                    center = thumbCenter,
                    radius = thumbRadius,
                    thumbColor = thumbColor,
                    shadowEnabled = enabled,
                )
            }
            pressLight.drawLight(
                scope = this,
                shader = lightShader,
                params = lightParams,
                background = Rect(
                    offset = Offset(centerX - backgroundWidth / 2f, capInset - backgroundWidth / 2f),
                    size = Size(backgroundWidth, availableHeight + backgroundWidth),
                ),
            )
        } else {
            val trackHeight = barHeight
            val backgroundHeight = lerp(trackHeight, trackHeight * TrackEnlargeScale, trackGrowFraction)
            val capInset = trackHeight * TrackCapInsetRatio
            val availableWidth = (barWidth - 2f * capInset).coerceAtLeast(0f)
            val effectiveFraction = if (reverseDirection) 1f - fraction else fraction
            val thumbCenterX = capInset + effectiveFraction * availableWidth
            val centerY = barHeight / 2f

            drawTrackCapsule(
                brush = SolidColor(backgroundColor),
                topLeft = Offset(capInset - backgroundHeight / 2f, centerY - backgroundHeight / 2f),
                size = Size(availableWidth + backgroundHeight, backgroundHeight),
                squircleEnabled = squircleEnabled,
            )

            if (secondaryProgressFraction != null) {
                val halfTrackSecondary = trackHeight / 2f
                val effectiveSecondary = if (reverseDirection) 1f - secondaryProgressFraction else secondaryProgressFraction
                val secondaryCenterX = capInset + effectiveSecondary * availableWidth
                val secondaryLeft = if (reverseDirection) secondaryCenterX - halfTrackSecondary else capInset - halfTrackSecondary
                val secondaryRight = if (reverseDirection) barWidth - capInset + halfTrackSecondary else secondaryCenterX + halfTrackSecondary
                drawTrackCapsule(
                    brush = SolidColor(secondaryProgressColor),
                    topLeft = Offset(secondaryLeft, centerY - halfTrackSecondary),
                    size = Size((secondaryRight - secondaryLeft).coerceAtLeast(0f), trackHeight),
                    squircleEnabled = squircleEnabled,
                )
            }

            val halfTrack = trackHeight / 2f
            if (showProgress && startFromMiddle) {
                val middle = capInset + availableWidth / 2f
                val stretch = MaxDeformedWidth.toPx() * deformationProvider()
                val left = min(middle, thumbCenterX - stretch)
                val right = max(middle, thumbCenterX + stretch)
                drawTrackCapsule(
                    brush = SolidColor(foregroundColor),
                    topLeft = Offset(left, centerY - halfTrack),
                    size = Size((right - left).coerceAtLeast(halfTrack * 2f), trackHeight),
                    squircleEnabled = squircleEnabled,
                )
                glitter.brush(left, right, horizontal = true, valueEndAtFar = !reverseDirection)?.let {
                    drawTrackCapsule(
                        brush = it,
                        topLeft = Offset(left, centerY - halfTrack),
                        size = Size(right - left, trackHeight),
                        squircleEnabled = squircleEnabled,
                    )
                }
            } else if (showProgress) {
                // COUI deformation (couiSeekBarDeformation): the edge that follows the finger
                // stretches by up to DEFAULT_MAX_WIDTH_DEFORMED_VALUE while the thumb moves.
                val stretch = MaxDeformedWidth.toPx() * deformationProvider()
                val progressLeft = if (reverseDirection) thumbCenterX - halfTrack - stretch else capInset - halfTrack
                val progressRight = if (reverseDirection) barWidth - capInset + halfTrack else thumbCenterX + halfTrack + stretch
                drawTrackCapsule(
                    brush = SolidColor(foregroundColor),
                    topLeft = Offset(progressLeft, centerY - halfTrack),
                    size = Size((progressRight - progressLeft).coerceAtLeast(0f), trackHeight),
                    squircleEnabled = squircleEnabled,
                )
                glitter.brush(progressLeft, progressRight, horizontal = true, valueEndAtFar = !reverseDirection)?.let {
                    drawTrackCapsule(
                        brush = it,
                        topLeft = Offset(progressLeft, centerY - halfTrack),
                        size = Size(progressRight - progressLeft, trackHeight),
                        squircleEnabled = squircleEnabled,
                    )
                }
            }

            if (showKeyPoints && stepFractions.isNotEmpty()) {
                val keyPointRadius = SliderDefaults.KeyPointRadius.toPx()
                for (i in stepFractions.indices) {
                    val stepFraction = stepFractions[i]
                    val effectiveStep = if (reverseDirection) 1f - stepFraction else stepFraction
                    val x = capInset + effectiveStep * availableWidth
                    val isSelected = if (reverseDirection) x >= thumbCenterX else x <= thumbCenterX
                    val kpColor = if (isSelected) keyPointForegroundColor else keyPointColor
                    drawCircle(
                        color = kpColor,
                        radius = keyPointRadius,
                        center = Offset(x, centerY),
                    )
                }
            }
            if (showThumb) {
                val thumbCenter = Offset(thumbCenterX, centerY)
                val thumbRadius = trackHeight * ThumbRadiusRatio * thumbScale
                if (!pressLight.drawTexturedThumb(this, thumbCenter, thumbRadius)) {
                    drawThumbWithShadow(
                        center = thumbCenter,
                        radius = thumbRadius,
                        thumbColor = thumbColor,
                        shadowEnabled = enabled,
                    )
                }
            }
            // SpotLightEffectRenderer draws in the OVERLAY phase, over the active track and the thumb.
            pressLight.drawLight(
                scope = this,
                shader = lightShader,
                params = lightParams,
                background = Rect(
                    offset = Offset(capInset - backgroundHeight / 2f, centerY - backgroundHeight / 2f),
                    size = Size(availableWidth + backgroundHeight, backgroundHeight),
                ),
            )
        }
    }
}

/**
 * Internal range slider track renderer, sharing the COUISeekBar rendering model with [SliderTrack].
 */
@Composable
private fun RangeSliderTrack(
    backgroundColor: Color,
    foregroundColor: Color,
    thumbColor: Color,
    keyPointColor: Color,
    keyPointForegroundColor: Color,
    valueStartProvider: () -> Float,
    valueEndProvider: () -> Float,
    startThumbScaleProvider: () -> Float,
    endThumbScaleProvider: () -> Float,
    valueRange: ClosedFloatingPointRange<Float>,
    isDragging: Boolean,
    enabled: Boolean,
    showKeyPoints: Boolean,
    stepFractions: FloatArray,
    isRtl: Boolean,
    secondaryProgressFraction: Float?,
    secondaryProgressColor: Color,
    glitter: SliderGlitter,
    pressLight: SliderPressLight,
    showProgress: Boolean = true,
    showThumb: Boolean = true,
    deformationProvider: () -> Float = { 0f },
    modifier: Modifier = Modifier,
) {
    val trackGrowFraction by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = TrackGrowAnimationSpec,
        label = "RangeSliderTrackGrow",
    )

    val squircleEnabled = isSquircleEnabled()
    val lightShader = rememberSpotLightShader(isRuntimeShaderSupported())
    val lightParams = SpotLightDefaults.opaqueMedium1(COUITheme.colorScheme.background.luminance() < 0.5f)
    Canvas(modifier = modifier) {
        val barHeight = size.height
        val barWidth = size.width
        val valueStart = valueStartProvider()
        val valueEnd = valueEndProvider()
        val startThumbScale = startThumbScaleProvider()
        val endThumbScale = endThumbScaleProvider()
        val startFraction = (valueStart - valueRange.start) / (valueRange.endInclusive - valueRange.start)
        val endFraction = (valueEnd - valueRange.start) / (valueRange.endInclusive - valueRange.start)
        val trackHeight = barHeight
        val backgroundHeight = lerp(trackHeight, trackHeight * TrackEnlargeScale, trackGrowFraction)
        val capInset = trackHeight * TrackCapInsetRatio
        val availableWidth = (barWidth - 2f * capInset).coerceAtLeast(0f)
        val effectiveStartFraction = if (isRtl) 1f - startFraction else startFraction
        val effectiveEndFraction = if (isRtl) 1f - endFraction else endFraction
        val startX = capInset + effectiveStartFraction * availableWidth
        val endX = capInset + effectiveEndFraction * availableWidth

        val centerY = barHeight / 2f

        drawTrackCapsule(
            brush = SolidColor(backgroundColor),
            topLeft = Offset(capInset - backgroundHeight / 2f, centerY - backgroundHeight / 2f),
            size = Size(availableWidth + backgroundHeight, backgroundHeight),
            squircleEnabled = squircleEnabled,
        )

        val halfTrack = trackHeight / 2f
        val rangeLeft = min(startX, endX)
        val rangeRight = max(startX, endX)
        if (secondaryProgressFraction != null) {
            val effectiveSecondary = if (isRtl) 1f - secondaryProgressFraction else secondaryProgressFraction
            val secondaryX = capInset + effectiveSecondary * availableWidth
            val secondaryLeft = if (isRtl) secondaryX - halfTrack else capInset - halfTrack
            val secondaryRight = if (isRtl) barWidth - capInset + halfTrack else secondaryX + halfTrack
            drawTrackCapsule(
                brush = SolidColor(secondaryProgressColor),
                topLeft = Offset(secondaryLeft, centerY - halfTrack),
                size = Size((secondaryRight - secondaryLeft).coerceAtLeast(0f), trackHeight),
                squircleEnabled = squircleEnabled,
            )
        }
        drawTrackCapsule(
            brush = SolidColor(foregroundColor),
            topLeft = Offset(rangeLeft - halfTrack, centerY - halfTrack),
            size = Size(rangeRight - rangeLeft + trackHeight, trackHeight),
            squircleEnabled = squircleEnabled,
        )
        glitter.brush(rangeLeft - halfTrack, rangeRight + halfTrack, horizontal = true, valueEndAtFar = !isRtl)?.let {
            drawTrackCapsule(
                brush = it,
                topLeft = Offset(rangeLeft - halfTrack, centerY - halfTrack),
                size = Size(rangeRight - rangeLeft + trackHeight, trackHeight),
                squircleEnabled = squircleEnabled,
            )
        }

        if (showKeyPoints && stepFractions.isNotEmpty()) {
            val keyPointRadius = SliderDefaults.KeyPointRadius.toPx()
            for (i in stepFractions.indices) {
                val stepFraction = stepFractions[i]
                val effectiveStep = if (isRtl) 1f - stepFraction else stepFraction
                val x = capInset + effectiveStep * availableWidth
                val isSelected = x in rangeLeft..rangeRight
                val kpColor = if (isSelected) keyPointForegroundColor else keyPointColor
                drawCircle(
                    color = kpColor,
                    radius = keyPointRadius,
                    center = Offset(x, centerY),
                )
            }
        }

        // Only the thumb under the finger takes the pressed texture.
        val touchX = pressLight.toCanvas(pressLight.touchPosition, size).x
        val texturedIsStart = abs(touchX - startX) <= abs(touchX - endX)
        val startCenter = Offset(startX, centerY)
        val startRadius = trackHeight * ThumbRadiusRatio * startThumbScale
        if (!texturedIsStart || !pressLight.drawTexturedThumb(this, startCenter, startRadius)) {
            drawThumbWithShadow(center = startCenter, radius = startRadius, thumbColor = thumbColor, shadowEnabled = enabled)
        }
        val endCenter = Offset(endX, centerY)
        val endRadius = trackHeight * ThumbRadiusRatio * endThumbScale
        if (texturedIsStart || !pressLight.drawTexturedThumb(this, endCenter, endRadius)) {
            drawThumbWithShadow(center = endCenter, radius = endRadius, thumbColor = thumbColor, shadowEnabled = enabled)
        }
        pressLight.drawLight(
            scope = this,
            shader = lightShader,
            params = lightParams,
            background = Rect(
                offset = Offset(capInset - backgroundHeight / 2f, centerY - backgroundHeight / 2f),
                size = Size(availableWidth + backgroundHeight, backgroundHeight),
            ),
        )
    }
}

/**
 * Which thumb a touch at [touchX] would drag, mirroring [RangeSlider]'s `onDragStarted`: the thumb
 * under the finger, the last dragged one when both are, otherwise the nearer one.
 */
internal fun rangePressPicksStart(
    touchX: Float,
    layoutWidth: Int,
    capInset: Float,
    startFraction: Float,
    endFraction: Float,
    isRtl: Boolean,
    lastDraggedIsStart: Boolean,
): Boolean {
    val availableWidth = (layoutWidth - 2f * capInset).coerceAtLeast(0f)
    val startPos = capInset + (if (isRtl) 1f - startFraction else startFraction) * availableWidth
    val endPos = capInset + (if (isRtl) 1f - endFraction else endFraction) * availableWidth
    val onStart = abs(touchX - startPos) <= capInset
    val onEnd = abs(touchX - endPos) <= capInset
    return when {
        onStart && !onEnd -> true
        !onStart && onEnd -> false
        onStart && onEnd -> lastDraggedIsStart
        else -> abs(touchX - startPos) <= abs(touchX - endPos)
    }
}

/**
 * Manages haptic feedback state for the slider.
 */
@Stable
internal class SliderHapticState {
    private var edgeFeedbackTriggered: Boolean = false
    private var lastStep: Float = 0f
    private var isAtKeyPoint: Boolean = false

    fun reset(currentValue: Float) {
        edgeFeedbackTriggered = false
        lastStep = currentValue
        isAtKeyPoint = false
    }

    fun handleHapticFeedback(
        currentValue: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        hapticEffect: SliderDefaults.SliderHapticEffect,
        hapticFeedback: HapticFeedback,
        keyPointFractions: FloatArray = floatArrayOf(),
        hasCustomKeyPoints: Boolean = false,
    ) {
        if (hapticEffect == SliderDefaults.SliderHapticEffect.None) return

        handleEdgeHaptic(currentValue, valueRange, hapticFeedback)

        if (hapticEffect == SliderDefaults.SliderHapticEffect.Step) {
            handleStepHaptic(currentValue, valueRange, hapticFeedback, keyPointFractions, hasCustomKeyPoints)
        }
    }

    private fun handleEdgeHaptic(
        currentValue: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        hapticFeedback: HapticFeedback,
    ) {
        val isAtEdge = currentValue == valueRange.start || currentValue == valueRange.endInclusive
        if (isAtEdge && !edgeFeedbackTriggered) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            edgeFeedbackTriggered = true
        } else if (!isAtEdge) {
            edgeFeedbackTriggered = false
        }
    }

    private fun handleStepHaptic(
        currentValue: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        hapticFeedback: HapticFeedback,
        keyPointFractions: FloatArray,
        hasCustomKeyPoints: Boolean,
    ) {
        val isNotAtEdge = currentValue != valueRange.start && currentValue != valueRange.endInclusive

        if (hasCustomKeyPoints && keyPointFractions.isNotEmpty()) {
            handleKeyPointHaptic(currentValue, valueRange, hapticFeedback, keyPointFractions, isNotAtEdge)
        } else if (currentValue != lastStep && isNotAtEdge) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastStep = currentValue
        }
    }

    private fun handleKeyPointHaptic(
        currentValue: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        hapticFeedback: HapticFeedback,
        keyPointFractions: FloatArray,
        isNotAtEdge: Boolean,
    ) {
        val fraction = (currentValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)
        val threshold = 0.005f

        var nearestDist = Float.MAX_VALUE
        for (i in keyPointFractions.indices) {
            val dist = abs(keyPointFractions[i] - fraction)
            if (dist < nearestDist) nearestDist = dist
        }
        val currentlyAtKeyPoint = nearestDist < threshold

        if (currentlyAtKeyPoint && !isAtKeyPoint && isNotAtEdge) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        isAtKeyPoint = currentlyAtKeyPoint
    }
}

/**
 * Manages haptic feedback state for the range slider.
 */
@Stable
internal class RangeSliderHapticState {
    private var startEdgeFeedbackTriggered: Boolean = false
    private var endEdgeFeedbackTriggered: Boolean = false
    private var startLastStep: Float = 0f
    private var endLastStep: Float = 0f
    private var startIsAtKeyPoint: Boolean = false
    private var endIsAtKeyPoint: Boolean = false

    fun resetStart(currentValue: Float) {
        startEdgeFeedbackTriggered = false
        startLastStep = currentValue
        startIsAtKeyPoint = false
    }

    fun resetEnd(currentValue: Float) {
        endEdgeFeedbackTriggered = false
        endLastStep = currentValue
        endIsAtKeyPoint = false
    }

    fun inheritStartKeyPoint() {
        startIsAtKeyPoint = endIsAtKeyPoint
    }

    fun inheritEndKeyPoint() {
        endIsAtKeyPoint = startIsAtKeyPoint
    }

    fun handleStartHapticFeedback(
        currentValue: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        hapticEffect: SliderDefaults.SliderHapticEffect,
        hapticFeedback: HapticFeedback,
        keyPointFractions: FloatArray = floatArrayOf(),
        hasCustomKeyPoints: Boolean = false,
    ) {
        handleHapticFeedbackInternal(
            currentValue = currentValue,
            valueRange = valueRange,
            hapticEffect = hapticEffect,
            hapticFeedback = hapticFeedback,
            keyPointFractions = keyPointFractions,
            edgeFeedbackTriggered = startEdgeFeedbackTriggered,
            lastStep = startLastStep,
            isAtKeyPoint = startIsAtKeyPoint,
            isStartEdge = true,
            hasCustomKeyPoints = hasCustomKeyPoints,
            onEdgeFeedbackUpdate = { startEdgeFeedbackTriggered = it },
            onLastStepUpdate = { startLastStep = it },
            onKeyPointUpdate = { startIsAtKeyPoint = it },
        )
    }

    fun handleEndHapticFeedback(
        currentValue: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        hapticEffect: SliderDefaults.SliderHapticEffect,
        hapticFeedback: HapticFeedback,
        keyPointFractions: FloatArray = floatArrayOf(),
        hasCustomKeyPoints: Boolean = false,
    ) {
        handleHapticFeedbackInternal(
            currentValue = currentValue,
            valueRange = valueRange,
            hapticEffect = hapticEffect,
            hapticFeedback = hapticFeedback,
            keyPointFractions = keyPointFractions,
            edgeFeedbackTriggered = endEdgeFeedbackTriggered,
            lastStep = endLastStep,
            isAtKeyPoint = endIsAtKeyPoint,
            isStartEdge = false,
            hasCustomKeyPoints = hasCustomKeyPoints,
            onEdgeFeedbackUpdate = { endEdgeFeedbackTriggered = it },
            onLastStepUpdate = { endLastStep = it },
            onKeyPointUpdate = { endIsAtKeyPoint = it },
        )
    }

    private fun handleHapticFeedbackInternal(
        currentValue: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        hapticEffect: SliderDefaults.SliderHapticEffect,
        hapticFeedback: HapticFeedback,
        keyPointFractions: FloatArray,
        edgeFeedbackTriggered: Boolean,
        lastStep: Float,
        isAtKeyPoint: Boolean,
        isStartEdge: Boolean,
        hasCustomKeyPoints: Boolean,
        onEdgeFeedbackUpdate: (Boolean) -> Unit,
        onLastStepUpdate: (Float) -> Unit,
        onKeyPointUpdate: (Boolean) -> Unit,
    ) {
        if (hapticEffect == SliderDefaults.SliderHapticEffect.None) return

        val targetEdge = if (isStartEdge) valueRange.start else valueRange.endInclusive
        val isAtEdge = currentValue == targetEdge

        if (isAtEdge && !edgeFeedbackTriggered) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            onEdgeFeedbackUpdate(true)
        } else if (!isAtEdge) {
            onEdgeFeedbackUpdate(false)
        }

        if (hapticEffect == SliderDefaults.SliderHapticEffect.Step) {
            val isNotAtEdge = currentValue != targetEdge

            if (hasCustomKeyPoints && keyPointFractions.isNotEmpty()) {
                val fraction = (currentValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                val threshold = 0.005f

                var nearestDist = Float.MAX_VALUE
                for (i in keyPointFractions.indices) {
                    val dist = abs(keyPointFractions[i] - fraction)
                    if (dist < nearestDist) nearestDist = dist
                }
                val currentlyAtKeyPoint = nearestDist < threshold

                if (currentlyAtKeyPoint && !isAtKeyPoint && isNotAtEdge) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                onKeyPointUpdate(currentlyAtKeyPoint)
            } else if (currentValue != lastStep && isNotAtEdge) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onLastStepUpdate(currentValue)
            }
        }
    }
}

private fun stepsToTickFractions(steps: Int): FloatArray = if (steps == 0) floatArrayOf() else FloatArray(steps + 2) { it.toFloat() / (steps + 1) }

private fun resolveValueFromFraction(
    fraction: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    allKeyPointFractions: FloatArray,
    magnetThreshold: Float,
): Float {
    val f = fraction.coerceIn(0f, 1f)
    val base = lerp(valueRange.start, valueRange.endInclusive, f)
    return when {
        steps > 0 -> {
            val stepCount = steps + 1
            val start = valueRange.start.toDouble()
            val end = valueRange.endInclusive.toDouble()
            val stepIndex = (f * stepCount).roundToInt().coerceIn(0, stepCount)
            (start + (end - start) * stepIndex / stepCount).toFloat()
        }

        allKeyPointFractions.isNotEmpty() -> {
            var closest = allKeyPointFractions[0]
            var bestDist = abs(closest - f)
            for (i in 1 until allKeyPointFractions.size) {
                val cand = allKeyPointFractions[i]
                val dist = abs(cand - f)
                if (dist < bestDist) {
                    bestDist = dist
                    closest = cand
                }
            }
            if (bestDist < magnetThreshold) {
                lerp(valueRange.start, valueRange.endInclusive, closest)
            } else {
                base
            }
        }

        else -> base
    }
}

/** The inactive track thickness multiplier while dragging (COUISeekBar BACKGROUND_RADIUS_SCALE). */
private val TrackEnlargeScale = 1.4f

/** Cap center inset as a fraction of track thickness (COUISeekBar mPaddingHorizontal). */
private val TrackCapInsetRatio = TrackEnlargeScale / 2f

/** COUISeekBar: coui_seekbar_thumb_radius (6dp) over track height (20dp). */
private val ThumbRadiusRatio = 0.3f

/** COUISeekBar: coui_seekbar_thumb_max_radius (8dp) over coui_seekbar_thumb_radius (6dp). */
private val ThumbPressedScale = 8f / 6f

/**
 * COUISeekBarCore track height spring: `HEIGHT_SPRING_RESPONSE` 0.5, `HEIGHT_SPRING_BOUNCE` 0,
 * i.e. a critically damped spring with stiffness (2 * PI / 0.5)^2.
 */
private val TrackGrowAnimationSpec = spring<Float>(dampingRatio = 1f, stiffness = 157.9f)

/**
 * COUISectionSeekBar SectionAnimationHandler: `DRAG_MOVE_RATIO` 0.4 — the share of the snapped
 * offset the thumb animates through, with the remaining 0.6 taken straight from the finger.
 */
private const val SECTION_DRAG_MOVE_RATIO = 0.4f

/**
 * SectionAnimationHandler: `DRAG_SNAP_DURATION` 100ms with a cubic-bezier(0, 0, 0.25, 1) curve.
 */
private val SectionSnapSpec = tween<Float>(durationMillis = 100, easing = CubicBezierEasing(0f, 0f, 0.25f, 1f))

/**
 * COUISeekBar thumb radius springs: `THUMB_SCALE_SPRING_RESPONSE` 0.5 with
 * `THUMB_SCALE_BOUNCE_ENLARGE` 0.3 when growing and `THUMB_SCALE_BOUNCE_SHRINK` 0 when shrinking.
 */
private val ThumbScaleEnlargeSpec = spring<Float>(dampingRatio = 0.7f, stiffness = 157.9f)

/** See [ThumbScaleEnlargeSpec]; shrinking is critically damped. */
private val ThumbScaleShrinkSpec = spring<Float>(dampingRatio = 1f, stiffness = 157.9f)

/**
 * COUISeekBar deformation follow spring: `DEFORMATION_SPRING_RESPONSE` 0.1, bounce 0.
 */
private val DeformationSpring = spring<Float>(dampingRatio = 1f, stiffness = 3947.8f)

/** DeformationEffectParams: `DEFAULT_MAX_WIDTH_DEFORMED_VALUE`, in dp. */
private val MaxDeformedWidth = 4.7.dp

/** DeformationEffectParams: `DEFAULT_MAX_MOVING_DISTANCE`, in px per frame. */
private const val MAX_MOVING_DISTANCE = 30f

/** The vertical gradient of the textured thumb (TexturedThumbRenderer LAYER_A_ALPHA_TOP). */
private val ThumbTextureTopAlpha = 230 / 255f

/** The vertical gradient of the textured thumb (TexturedThumbRenderer LAYER_A_ALPHA_BOTTOM). */
private val ThumbTextureBottomAlpha = 140 / 255f

/** TexturedThumbRenderer LAYER_B_FILL: the inner highlight of the thumb (#77FFFFFF). */
private val ThumbHighlightColor = Color(0x77FFFFFF)

/** COUISeekBar style: couiSeekBarThumbShadowSize = 4dp (shadow blur radius). */
private val ThumbShadowRadius = 4.dp

/** COUISeekBar: coui_seekbar_shadow_offset_y = 2dp. */
private val ThumbShadowOffsetY = 2.dp

/** COUISeekBar: coui_seekbar_thumb_shadow_color = #1A000000. */
private val ThumbShadowColor = Color(0x1A000000)

/**
 * Draws a horizontal track as the ColorOS capsule. COUISeekBarCore hands half the bar's height to
 * the smooth round rect path, so on ColorOS 17 the ends ease into the sides rather than meeting
 * them as a plain semicircle; `coui_seekbar_background_radius` is only read by the vertical bar.
 */
private fun DrawScope.drawTrackCapsule(
    brush: Brush,
    topLeft: Offset,
    size: Size,
    squircleEnabled: Boolean,
) {
    if (squircleEnabled) {
        drawPath(Path().apply { addSegmentCapsule(Rect(topLeft, size)) }, brush)
    } else {
        drawRoundRect(brush = brush, topLeft = topLeft, size = size, cornerRadius = CornerRadius(size.minDimension / 2f))
    }
}

/**
 * The soft drop shadow of a seek bar thumb: `couiSeekBarThumbShadowSize`, which StandardThumbRenderer
 * and IntentThumbRenderer both hand to the paint's shadow layer.
 */
internal fun DrawScope.drawThumbShadow(center: Offset, radius: Float) {
    val shadowRadius = radius + ThumbShadowRadius.toPx()
    val shadowCenter = Offset(center.x, center.y + ThumbShadowOffsetY.toPx())
    drawCircle(
        brush = Brush.radialGradient(
            0f to ThumbShadowColor,
            radius / shadowRadius to ThumbShadowColor,
            1f to ThumbShadowColor.copy(alpha = 0f),
            center = shadowCenter,
            radius = shadowRadius,
        ),
        radius = shadowRadius,
        center = shadowCenter,
    )
}

/**
 * Draws the slider thumb with a soft drop shadow (COUISeekBar.drawThumb), followed by the two
 * textured layers of COUISeekBar's [TexturedThumbRenderer]: a vertical white gradient that fades
 * towards the top (LAYER_A, argb 230 -> 140) and a white highlight ring (LAYER_B).
 * The shadow is skipped when the slider is disabled.
 */
private fun DrawScope.drawThumbWithShadow(
    center: Offset,
    radius: Float,
    thumbColor: Color,
    shadowEnabled: Boolean,
) {
    if (shadowEnabled && radius > 0f) drawThumbShadow(center, radius)
    if (radius <= 0f) return
    drawCircle(color = thumbColor, radius = radius, center = center)
    // LAYER_A: white gradient, opaque towards the bottom and translucent towards the top.
    drawCircle(
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = ThumbTextureBottomAlpha),
            0.8f to Color.White.copy(alpha = ThumbTextureTopAlpha),
            1f to Color.White.copy(alpha = ThumbTextureTopAlpha),
            startY = center.y - radius,
            endY = center.y + radius,
        ),
        radius = radius,
        center = center,
    )
    // LAYER_B: the white highlight ring that gives the ball its glassy edge.
    drawCircle(
        color = ThumbHighlightColor,
        radius = radius * 0.62f,
        center = Offset(center.x, center.y + radius * 0.12f),
        style = Stroke(width = radius * 0.3f),
    )
}

/** [horizontalVisualFraction] with the resting cap inset derived from the given track thickness. */
private fun horizontalVisualFraction(offsetX: Float, sizeWidth: Int, trackThickness: Int): Float = horizontalVisualFraction(offsetX, sizeWidth, trackThickness * TrackCapInsetRatio)

private fun horizontalVisualFraction(offsetX: Float, sizeWidth: Int, capInset: Float): Float {
    val availableWidth = (sizeWidth - 2f * capInset).coerceAtLeast(0f)
    return if (availableWidth == 0f) 0f else ((offsetX - capInset) / availableWidth).coerceIn(0f, 1f)
}

private fun verticalVisualFraction(offsetY: Float, sizeHeight: Int, sizeWidth: Int): Float {
    val capInset = sizeWidth * TrackCapInsetRatio
    val availableHeight = (sizeHeight - 2f * capInset).coerceAtLeast(0f)
    return if (availableHeight == 0f) 0f else ((offsetY - capInset) / availableHeight).coerceIn(0f, 1f)
}

/**
 * Normalizes `secondaryProgress` into the 0..1 fraction of [valueRange] the secondary track spans.
 *
 * The value is coerced into [valueRange] first, so values outside the range clamp to the track
 * ends, matching the `ProgressBar.setSecondaryProgress` layer [IntentSeekBar] draws.
 */
internal fun secondaryProgressFraction(
    secondaryProgress: Float,
    valueRange: ClosedFloatingPointRange<Float>,
): Float {
    val span = valueRange.endInclusive - valueRange.start
    return ((secondaryProgress - valueRange.start) / span).coerceIn(0f, 1f)
}

/**
 * Converts point values to normalized fractions within the value range.
 */
private fun pointsToFractions(
    points: List<Float>,
    valueRange: ClosedFloatingPointRange<Float>,
): FloatArray = points.map { point ->
    ((point - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)
}.toFloatArray()

/**
 * Computes key point fractions for slider display.
 * Filters out points too close to edges.
 */
private fun computeKeyPointFractions(
    keyPoints: List<Float>?,
    stepFractions: FloatArray,
    valueRange: ClosedFloatingPointRange<Float>,
    showKeyPoints: Boolean,
): FloatArray = when {
    keyPoints != null -> pointsToFractions(keyPoints, valueRange)
    showKeyPoints -> stepFractions
    else -> floatArrayOf()
}

/**
 * Computes all key point fractions including edge points.
 * Used for haptic feedback and magnetic snapping.
 */
private fun computeAllKeyPointFractions(
    keyPoints: List<Float>?,
    stepFractions: FloatArray,
    valueRange: ClosedFloatingPointRange<Float>,
): FloatArray = when {
    keyPoints != null -> pointsToFractions(keyPoints, valueRange)
    stepFractions.isNotEmpty() -> stepFractions
    else -> floatArrayOf()
}

object SliderDefaults {
    /**
     * The height of the drawn track of the [Slider] and [RangeSlider]
     * (COUI `coui_seekbar_background_height` / `coui_seekbar_progress_height`).
     */
    val MinHeight = 20.dp

    /**
     * The minimum height of the slider's touch target (COUI `couiSeekBarMinHeight`): the track is
     * 20dp but the view is 36dp tall, so the extra 16dp is tappable.
     *
     * New in 1.2.0.
     */
    val MinTouchHeight = 36.dp

    /**
     * The value range that matches COUI's integer progress model
     * (`SeekBarState.DEFAULT_MAX` = 100). `SliderDefaults` keeps the 0f..1f default for
     * compatibility; pass this range to work in COUI's 0..100 percent units.
     *
     * New in 1.2.0.
     */
    val PercentRange: ClosedFloatingPointRange<Float> = 0f..100f

    /**
     * The release speed above which the thumb keeps sliding
     * (COUI `PhysicsAnimationHandler.PHYSICAL_VELOCITY_LIMIT`, px per second).
     *
     * New in 1.2.0.
     */
    const val FLING_VELOCITY_LIMIT = 100f

    /**
     * How far a fling of one pixel per second travels, in seconds. COUI's
     * `PhysicsAnimationHandler` decelerates the fling with `DEFAULT_FLING_LINEAR_DAMPING` 15, so a
     * released thumb travels `velocity / 15` pixels before it stops.
     *
     * New in 1.2.0.
     */
    const val FLING_PROJECTION_SECONDS = 1f / 15f

    /**
     * The radius of the key points on the [Slider] and [RangeSlider].
     */
    val KeyPointRadius = 3.0.dp

    /**
     * The type of haptic feedback to be used for the slider.
     */
    enum class SliderHapticEffect {
        /** No haptic feedback. */
        None,

        /** Haptic feedback at 0% and 100%. */
        Edge,

        /** Haptic feedback at steps. */
        Step,
    }

    /**
     * The default haptic effect of the [Slider] and [RangeSlider].
     */
    val DefaultHapticEffect = SliderHapticEffect.Edge

    @OptIn(ExperimentalCOUIApi::class)
    @Composable
    fun sliderColors(
        foregroundColor: Color = COUITheme.colorScheme.primary,
        disabledForegroundColor: Color = COUITheme.colorScheme.disabledPrimarySlider,
        backgroundColor: Color = COUITheme.colorScheme.sliderBackground,
        disabledBackgroundColor: Color = COUITheme.colorScheme.sliderBackground,
        thumbColor: Color = COUITheme.colorScheme.onPrimary,
        // coui_seekbar_thumb_disable_color: 54% white (dark: 29ffffff).
        disabledThumbColor: Color = COUITheme.colorScheme.disabledOnPrimaryButton,
        keyPointColor: Color = COUITheme.colorScheme.sliderKeyPoint,
        keyPointForegroundColor: Color = COUITheme.colorScheme.sliderKeyPointForeground,
        secondaryProgressColor: Color = COUITheme.colorScheme.secondaryVariant,
        disabledSecondaryProgressColor: Color = COUITheme.colorScheme.disabledSecondaryVariant,
    ): SliderColors = remember(
        foregroundColor,
        disabledForegroundColor,
        backgroundColor,
        disabledBackgroundColor,
        thumbColor,
        disabledThumbColor,
        keyPointColor,
        keyPointForegroundColor,
        secondaryProgressColor,
        disabledSecondaryProgressColor,
    ) {
        SliderColors(
            foregroundColor = foregroundColor,
            disabledForegroundColor = disabledForegroundColor,
            backgroundColor = backgroundColor,
            disabledBackgroundColor = disabledBackgroundColor,
            thumbColor = thumbColor,
            disabledThumbColor = disabledThumbColor,
            keyPointColor = keyPointColor,
            keyPointForegroundColor = keyPointForegroundColor,
            secondaryProgressColor = secondaryProgressColor,
            disabledSecondaryProgressColor = disabledSecondaryProgressColor,
        )
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Migrated to the overload that adds secondary-progress colors.",
        level = DeprecationLevel.HIDDEN,
    )
    @Composable
    fun sliderColors(
        foregroundColor: Color = COUITheme.colorScheme.primary,
        disabledForegroundColor: Color = COUITheme.colorScheme.disabledPrimarySlider,
        backgroundColor: Color = COUITheme.colorScheme.sliderBackground,
        disabledBackgroundColor: Color = COUITheme.colorScheme.sliderBackground,
        thumbColor: Color = COUITheme.colorScheme.onPrimary,
        // coui_seekbar_thumb_disable_color: 54% white (dark: 29ffffff).
        disabledThumbColor: Color = COUITheme.colorScheme.disabledOnPrimaryButton,
        keyPointColor: Color = COUITheme.colorScheme.sliderKeyPoint,
        keyPointForegroundColor: Color = COUITheme.colorScheme.sliderKeyPointForeground,
    ): SliderColors = sliderColors(
        foregroundColor = foregroundColor,
        disabledForegroundColor = disabledForegroundColor,
        backgroundColor = backgroundColor,
        disabledBackgroundColor = disabledBackgroundColor,
        thumbColor = thumbColor,
        disabledThumbColor = disabledThumbColor,
        keyPointColor = keyPointColor,
        keyPointForegroundColor = keyPointForegroundColor,
    )
}

@Immutable
data class SliderColors(
    private val foregroundColor: Color,
    private val disabledForegroundColor: Color,
    private val backgroundColor: Color,
    private val disabledBackgroundColor: Color,
    private val thumbColor: Color,
    private val disabledThumbColor: Color,
    private val keyPointColor: Color,
    private val keyPointForegroundColor: Color,
    @property:ExperimentalCOUIApi private val secondaryProgressColor: Color = foregroundColor,
    @property:ExperimentalCOUIApi private val disabledSecondaryProgressColor: Color = disabledForegroundColor,
) {
    /**
     * The eight-color form, kept so callers written before `secondaryProgress` existed still
     * compile; the secondary-progress colors fall back to [foregroundColor] / [disabledForegroundColor].
     */
    @OptIn(ExperimentalCOUIApi::class)
    constructor(
        foregroundColor: Color,
        disabledForegroundColor: Color,
        backgroundColor: Color,
        disabledBackgroundColor: Color,
        thumbColor: Color,
        disabledThumbColor: Color,
        keyPointColor: Color,
        keyPointForegroundColor: Color,
    ) : this(
        foregroundColor = foregroundColor,
        disabledForegroundColor = disabledForegroundColor,
        backgroundColor = backgroundColor,
        disabledBackgroundColor = disabledBackgroundColor,
        thumbColor = thumbColor,
        disabledThumbColor = disabledThumbColor,
        keyPointColor = keyPointColor,
        keyPointForegroundColor = keyPointForegroundColor,
        secondaryProgressColor = foregroundColor,
        disabledSecondaryProgressColor = disabledForegroundColor,
    )

    /** The eight-color [copy], kept for binary compatibility with callers built before 1.2.0. */
    @OptIn(ExperimentalCOUIApi::class)
    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Migrated to the copy that also takes the secondary-progress colors.",
        level = DeprecationLevel.HIDDEN,
    )
    fun copy(
        foregroundColor: Color = this.foregroundColor,
        disabledForegroundColor: Color = this.disabledForegroundColor,
        backgroundColor: Color = this.backgroundColor,
        disabledBackgroundColor: Color = this.disabledBackgroundColor,
        thumbColor: Color = this.thumbColor,
        disabledThumbColor: Color = this.disabledThumbColor,
        keyPointColor: Color = this.keyPointColor,
        keyPointForegroundColor: Color = this.keyPointForegroundColor,
    ): SliderColors = copy(
        foregroundColor = foregroundColor,
        disabledForegroundColor = disabledForegroundColor,
        backgroundColor = backgroundColor,
        disabledBackgroundColor = disabledBackgroundColor,
        thumbColor = thumbColor,
        disabledThumbColor = disabledThumbColor,
        keyPointColor = keyPointColor,
        keyPointForegroundColor = keyPointForegroundColor,
        secondaryProgressColor = secondaryProgressColor,
        disabledSecondaryProgressColor = disabledSecondaryProgressColor,
    )

    @Stable
    internal fun foregroundColor(enabled: Boolean): Color = if (enabled) foregroundColor else disabledForegroundColor

    @Stable
    internal fun backgroundColor(enabled: Boolean): Color = if (enabled) backgroundColor else disabledBackgroundColor

    @Stable
    internal fun thumbColor(enabled: Boolean): Color = if (enabled) thumbColor else disabledThumbColor

    @Stable
    internal fun keyPointColor(): Color = keyPointColor

    @Stable
    internal fun keyPointForegroundColor(): Color = keyPointForegroundColor

    /** The color of the secondary progress layer, used when `secondaryProgress` is drawn. New in 1.2.0. */
    @OptIn(ExperimentalCOUIApi::class)
    @Stable
    internal fun secondaryProgressColor(enabled: Boolean): Color = if (enabled) secondaryProgressColor else disabledSecondaryProgressColor
}
