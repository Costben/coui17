// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.util.lerp
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

private const val SECTION_DRAG_MOVE_RATIO = 0.4f
private val SectionSnapSpec = tween<Float>(durationMillis = 100, easing = CubicBezierEasing(0f, 0f, 0.25f, 1f))
private val DeformationSpring = spring<Float>(dampingRatio = 1f, stiffness = 3947.8f)
private val MaxDeformedWidth = 4.7.dp
private const val MAX_MOVING_DISTANCE = 30f

@Composable
internal actual fun CouiPlatformSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)?,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    reverseDirection: Boolean,
    height: Dp,
    colors: SliderColors,
    hapticEffect: SliderDefaults.SliderHapticEffect,
    showKeyPoints: Boolean,
    keyPoints: List<Float>?,
    magnetThreshold: Float,
    secondaryProgress: Float?,
    showProgress: Boolean,
    showThumb: Boolean,
    startFromMiddle: Boolean,
    moveByFinger: Boolean,
    modifier: Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val layoutDirection = LocalLayoutDirection.current
    val effectiveReverseDirection = if (layoutDirection == LayoutDirection.Rtl) !reverseDirection else reverseDirection
    val onValueChangeState by rememberUpdatedState(onValueChange)
    val onValueChangeFinishedState by rememberUpdatedState(onValueChangeFinished)
    val density = LocalDensity.current
    val trackHeightPx = with(density) { height.toPx() }
    val restingInsetPx = trackHeightPx * TrackCapInsetRatio
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isThumbGrabbed by remember { mutableStateOf(false) }
    var isHoveringThumb by remember { mutableStateOf(false) }
    var deformationRaw by remember { mutableFloatStateOf(0f) }
    var layoutWidth by remember { mutableIntStateOf(0) }
    var layoutHeight by remember { mutableIntStateOf(0) }
    val hapticState = remember { SliderHapticState() }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()
    val flingPosition = remember { Animatable(0f) }
    var flingJob by remember { mutableStateOf<Job?>(null) }
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

    fun applySectionThumb(width: Float) {
        val thumb = (sectionAnchorX + sectionSnap.value * SECTION_DRAG_MOVE_RATIO + sectionDelta * (1f - SECTION_DRAG_MOVE_RATIO))
            .coerceIn(0f, width)
        sectionThumbFraction = thumb / width
    }

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
                                        sectionDragDirection = direction
                                        sectionSnapJob?.cancel()
                                        if (sectionAnchorIndex != sectionValueIndex) {
                                            sectionAnchorIndex = sectionValueIndex
                                            sectionAnchorX = sectionValueIndex * pixelsPerSection
                                            scope.launch { sectionSnap.snapTo(0f) }
                                        }
                                    }
                                    val delta = fingerX - sectionAnchorX
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
                                flingJob?.cancel()
                                val capInset = restingInsetPx
                                val availableWidth = (layoutWidth - 2f * capInset).coerceAtLeast(0f)
                                val fraction = (animatedValueState.value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                                val effectiveFraction = if (effectiveReverseDirection) 1f - fraction else fraction
                                val thumbX = capInset + effectiveFraction * availableWidth
                                isThumbGrabbed = abs(offset.x - thumbX) <= capInset
                                isDragging = true
                                glitter.onDragStarted()
                                dragOffset = if (moveByFinger) thumbX else offset.x
                                val visualFraction = horizontalVisualFraction(offset.x, layoutWidth, capInset)
                                val fractionForValue = if (effectiveReverseDirection) 1f - visualFraction else visualFraction
                                if (steps > 0) {
                                    val width = (layoutWidth - 2f * restingInsetPx).coerceAtLeast(1f)
                                    val pixelsPerSection = width / sectionCount
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
        SkikoSliderTrack(
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

@Composable
private fun SkikoSliderTrack(
    backgroundColor: Color,
    foregroundColor: Color,
    thumbColor: Color,
    keyPointColor: Color,
    keyPointForegroundColor: Color,
    valueProvider: () -> Float,
    valueRange: ClosedFloatingPointRange<Float>,
    thumbFractionProvider: (() -> Float?)? = null,
    isDragging: Boolean,
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
        val fraction = thumbFractionProvider?.invoke()
            ?: ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))

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
