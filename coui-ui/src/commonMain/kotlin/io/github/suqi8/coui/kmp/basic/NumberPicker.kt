// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.CouiVibrateLevel
import io.github.suqi8.coui.kmp.utils.rememberCouiPickerClick
import io.github.suqi8.coui.kmp.utils.rememberCouiPickerHaptic
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A [NumberPicker] component with COUI style.
 *
 * A vertical scroll picker that displays a range of numbers with the selected value centered.
 * The centered item is enlarged and highlighted; items transition to the normal size and
 * color within half an item of the center, matching the COUI picker.
 *
 * The scroll model mirrors `COUINumberPicker`: a release faster than
 * [NumberPickerDefaults.MinFlingVelocity] throws the wheel with the velocity scaled by 1.6 and
 * decelerates on the `SLOW_FLING_INTERPOLATOR` curve for 1.5x the spline duration, every stop is
 * followed by a 300ms snap onto the nearest row, a tap on a row outside the selection band steps one
 * row over 300ms, and holding outside the band keeps stepping every [longPressUpdateInterval].
 *
 * @param value The current selected value. If outside [range], it will be coerced into the range.
 *   Pass [NumberPickerDefaults.IgnoredValue] together with [ignorable] to show the ignore bar.
 * @param onValueChange The callback invoked when the selected value changes.
 * @param modifier The modifier to be applied to the [NumberPicker].
 * @param enabled Whether the [NumberPicker] is enabled for user interaction.
 * @param range The range of selectable values.
 * @param label A function that converts a value to its display string.
 * @param visibleItemCount The number of visible items. Must be odd and at least 3.
 * @param wrapAround Whether the picker wraps around from the last item to the first. Ignored when
 *   the range is too short to fill the wheel.
 * @param colors The [NumberPickerColors] for this [NumberPicker].
 * @param textStyle The [TextStyle] for the picker items. Its font size is replaced by
 *   [focusTextSize] and [normalTextSize], which are dimensions rather than scalable sp values.
 * @param itemHeight The height of each item in the picker.
 * @param ignorable Whether the wheel carries one extra slot past [range] that renders the ignore
 *   bar instead of a value (`COUINumberPicker.setIgnorable`).
 * @param maxWidth The widest the picker may grow (`couiPickersMaxWidth`).
 * @param focusTextSize The text size of the centered row (`coui_numberpicker_textSize_big`).
 * @param normalTextSize The text size of the other rows (`coui_numberpicker_textSize_small`).
 * @param vibrateLevel The strength of the step tick (`couiVibrateLevel`).
 * @param longPressUpdateInterval How often a held row keeps stepping (`setOnLongPressUpdateInterval`).
 * @param onScrollingStop Invoked once a scroll settles, matching `OnScrollingStopListener`.
 */
@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    range: IntRange = 0..10,
    label: (Int) -> String = { it.toString() },
    visibleItemCount: Int = 3,
    wrapAround: Boolean = true,
    colors: NumberPickerColors = NumberPickerDefaults.colors(),
    textStyle: TextStyle = COUITheme.textStyles.title3,
    itemHeight: Dp = NumberPickerDefaults.ItemHeight,
    ignorable: Boolean = false,
    maxWidth: Dp = NumberPickerDefaults.MaxWidth,
    focusTextSize: Dp = NumberPickerDefaults.FocusTextSize,
    normalTextSize: Dp = NumberPickerDefaults.NormalTextSize,
    vibrateLevel: CouiVibrateLevel = CouiVibrateLevel.Crisp,
    longPressUpdateInterval: Duration = NumberPickerDefaults.LongPressUpdateInterval,
    onScrollingStop: (() -> Unit)? = null,
) {
    require(visibleItemCount % 2 == 1 && visibleItemCount >= 3) {
        "visibleItemCount must be odd and at least 3, but was $visibleItemCount"
    }
    require(range.first <= range.last) {
        "range must not be empty"
    }

    val valueCount = range.last - range.first + 1
    val itemCount = valueCount + if (ignorable) 1 else 0
    // COUINumberPicker.updateWrapSelectorWheel() drops wrapping when the range cannot fill the wheel.
    val wraps = wrapAround && itemCount - 1 >= visibleItemCount
    val ignored = ignorable && value == NumberPickerDefaults.IgnoredValue
    val coercedValue = if (ignored) range.first else value.coerceIn(range)
    val currentIndex = if (ignored) valueCount else coercedValue - range.first
    val halfVisibleCount = visibleItemCount / 2

    val pickerHaptic = rememberCouiPickerHaptic()
    val pickerClick = rememberCouiPickerClick()
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnScrollingStop by rememberUpdatedState(onScrollingStop)

    val state = remember { PickerState() }
    state.density = LocalDensity.current.density
    state.itemCount = itemCount
    state.valueCount = valueCount
    state.rangeFirst = range.first
    state.ignorable = ignorable
    state.wraps = wraps
    state.halfVisibleCount = halfVisibleCount
    state.onValueChange = currentOnValueChange
    state.onScrollingStop = currentOnScrollingStop
    state.haptic = pickerHaptic
    state.click = pickerClick
    state.vibrateLevel = vibrateLevel

    LaunchedEffect(currentIndex, ignorable) {
        if (!state.isDragging) {
            state.index = currentIndex
            state.offsetPx = 0f
        }
    }

    val effectiveIndex by remember { derivedStateOf { state.effectiveIndex() } }
    var lastFeedbackIndex by remember { mutableIntStateOf(currentIndex) }
    LaunchedEffect(Unit) {
        snapshotFlow { effectiveIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (index != lastFeedbackIndex) {
                    if (state.isScrolling) {
                        state.haptic(state.velocity, state.vibrateLevel)
                        state.click()
                    }
                    lastFeedbackIndex = index
                }
            }
    }

    var holdStep by remember { mutableStateOf<HoldStep?>(null) }
    LaunchedEffect(holdStep) {
        val hold = holdStep ?: return@LaunchedEffect
        // COUINumberPicker posts the first step at the long-press timeout, then repeats.
        state.stepOne(hold.increment)
        while (true) {
            delay(longPressUpdateInterval)
            state.stepOne(hold.increment)
        }
    }

    // The gesture scope is a restricted coroutine scope, so it only records what the release asks
    // for; the animation itself runs here, where suspending on an Animatable is allowed.
    var command by remember { mutableStateOf<PickerCommand?>(null) }
    LaunchedEffect(command) {
        val requested = command ?: return@LaunchedEffect
        when (requested.action) {
            PickerAction.Fling -> {
                state.flingBy(requested.velocity)
                state.snapToNearest()
            }

            PickerAction.Snap -> state.snapToNearest()

            PickerAction.StepForward -> state.stepOne(increment = true)

            PickerAction.StepBackward -> state.stepOne(increment = false)
        }
        state.isScrolling = false
    }

    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val resolvedTextStyle = remember(textStyle, focusTextSize, normalTextSize, density) {
        textStyle.copy(
            // Both sizes are dimensions in the native picker, so they never follow the font scale.
            fontSize = with(density) { focusTextSize.toSp() },
            fontWeight = textStyle.fontWeight ?: FontWeight.Medium,
        )
    }
    val normalTextStyle = remember(resolvedTextStyle, focusTextSize, normalTextSize) {
        resolvedTextStyle.copy(fontSize = resolvedTextStyle.fontSize * (normalTextSize / focusTextSize))
    }
    // COUI focus text is 20dp and normal text is 16dp, so the scale floor is 0.8.
    val scaleFloor = normalTextSize / focusTextSize

    val displayValue = if (ignored) "" else label(coercedValue)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = maxWidth)
            .height(itemHeight * visibleItemCount)
            .clipToBounds()
            .semantics {
                contentDescription = "$displayValue, ${range.first} - ${range.last}"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = coercedValue.toFloat(),
                    range = (range.first - 1).toFloat()..(range.last + 1).toFloat(),
                )
                setProgress { target ->
                    val newValue = target.roundToInt().coerceIn(range)
                    if (newValue != coercedValue) {
                        currentOnValueChange(newValue)
                        true
                    } else {
                        false
                    }
                }
                scrollBy { _, vertical ->
                    val step = if (vertical > 0f) -1 else 1
                    val newValue = (coercedValue + step).coerceIn(range)
                    if (newValue != coercedValue) {
                        currentOnValueChange(newValue)
                        true
                    } else {
                        false
                    }
                }
            }
            .onSizeChanged { size ->
                state.itemHeightPx = size.height / visibleItemCount
                state.columnWidthPx = size.width
            }
            .then(
                if (enabled) {
                    Modifier.pointerInput(state, itemHeight, visibleItemCount) {
                        awaitEachGesture {
                            if (state.itemHeightPx <= 0) return@awaitEachGesture
                            val touchSlop = viewConfiguration.touchSlop
                            val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downY = down.position.y
                            val zone = state.rowZone(downY)
                            var lastY = downY
                            var dragged = false
                            var travelled = 0f
                            val tracker = VelocityTracker()
                            tracker.addPosition(down.uptimeMillis, down.position)
                            state.beginGesture()
                            // A press outside the band starts the hold-to-step timer.
                            if (zone != 0) holdStep = HoldStep(increment = zone > 0)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.changedToUpIgnoreConsumed()) {
                                    change.consume()
                                    holdStep = null
                                    state.endGesture()
                                    val velocity = tracker.calculateVelocity().y
                                    state.velocity = abs(velocity)
                                    val elapsed = change.uptimeMillis - down.uptimeMillis
                                    state.isScrolling = true
                                    command = when {
                                        abs(velocity) > NumberPickerDefaults.MinFlingVelocity ->
                                            PickerCommand(PickerAction.Fling, velocity)

                                        dragged || elapsed >= longPressTimeout ->
                                            PickerCommand(PickerAction.Snap)

                                        zone != 0 ->
                                            PickerCommand(
                                                if (zone > 0) PickerAction.StepForward else PickerAction.StepBackward,
                                            )

                                        else -> null
                                    }
                                    if (command == null) state.isScrolling = false
                                    break
                                }
                                val dy = change.position.y - lastY
                                lastY = change.position.y
                                travelled += dy
                                tracker.addPosition(change.uptimeMillis, change.position)
                                if (!dragged && abs(travelled) > touchSlop) {
                                    dragged = true
                                    // A drag cancels the pending hold-to-step.
                                    holdStep = null
                                }
                                if (dragged) {
                                    change.consume()
                                    state.drag(dy)
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val unit = state.itemHeightPx.toFloat()
        if (unit > 0f) {
            val offsetPx = state.offsetPx
            val roundedOffset = (offsetPx / unit).roundToInt()
            val fraction = offsetPx / unit - roundedOffset
            val selectedColor = colors.selectedTextColor(enabled)
            val unselectedColor = colors.unselectedTextColor(enabled)

            for (i in -halfVisibleCount - 1..halfVisibleCount + 1) {
                val rawItemIndex = state.index + i + roundedOffset
                val itemIndex = if (wraps) {
                    ((rawItemIndex % itemCount) + itemCount) % itemCount
                } else {
                    if (rawItemIndex !in 0..<itemCount) continue
                    rawItemIndex
                }

                val distanceFromCenter = i - fraction
                // COUI completes the focus-to-normal transition within half an item of the center;
                // items are never faded out per-item (the normal color carries its own alpha).
                val transition = (abs(distanceFromCenter) * 2f).coerceIn(0f, 1f)

                // COUI focus text is 20dp and normal text is 16dp, so the scale floor is 0.8.
                val scale = 1f - (1f - scaleFloor) * transition
                val yOffset = distanceFromCenter * unit

                val textColor = Color(
                    red = lerp(selectedColor.red, unselectedColor.red, transition),
                    green = lerp(selectedColor.green, unselectedColor.green, transition),
                    blue = lerp(selectedColor.blue, unselectedColor.blue, transition),
                    alpha = lerp(selectedColor.alpha, unselectedColor.alpha, transition),
                )

                if (ignorable && itemIndex >= valueCount) {
                    IgnoreBar(
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationY = yOffset
                        },
                        color = textColor,
                    )
                } else {
                    val text = label(range.first + itemIndex)
                    // COUINumberPicker switches Paint.Align to LEFT once the text is as wide as the
                    // column, so a long label keeps its leading edge instead of being ellipsized.
                    val tooLong = state.isTextTooLong(measurer, text, normalTextStyle)
                    Text(
                        text = text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationY = yOffset
                            },
                        style = resolvedTextStyle,
                        color = textColor,
                        textAlign = if (tooLong) TextAlign.Start else TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

/** Binary-compatibility bridge for the [NumberPicker] overload that predates the picker attributes. */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds the ignorable bar, unit sizing and feedback.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    range: IntRange = 0..10,
    label: (Int) -> String = { it.toString() },
    visibleItemCount: Int = 3,
    wrapAround: Boolean = false,
    colors: NumberPickerColors = NumberPickerDefaults.colors(),
    textStyle: TextStyle = COUITheme.textStyles.title3,
    itemHeight: Dp = NumberPickerDefaults.ItemHeight,
) {
    NumberPicker(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        range = range,
        label = label,
        visibleItemCount = visibleItemCount,
        wrapAround = wrapAround,
        colors = colors,
        textStyle = textStyle,
        itemHeight = itemHeight,
    )
}

/**
 * The mutable scroll model of a [NumberPicker].
 *
 * Holding it in one stable object keeps the gesture handler, the hold-to-step loop and the render
 * pass on the same numbers: they all read the same [index] instead of the value captured when their
 * lambda was created.
 */
@Stable
private class PickerState {
    var itemHeightPx by mutableIntStateOf(0)
    var columnWidthPx by mutableIntStateOf(0)
    var density: Float = 1f
    var itemCount by mutableIntStateOf(0)
    var valueCount by mutableIntStateOf(0)
    var rangeFirst by mutableIntStateOf(0)
    var halfVisibleCount by mutableIntStateOf(0)
    var ignorable by mutableStateOf(false)
    var wraps by mutableStateOf(false)

    /** Index of the committed value inside the wheel. */
    var index by mutableIntStateOf(0)

    /**
     * Offset of the wheel from the committed value, in pixels; positive means later values are
     * moving toward the center. The wheel is always drawn from this single number.
     */
    var offsetPx by mutableFloatStateOf(0f)
    var isScrolling by mutableStateOf(false)
    var isDragging by mutableStateOf(false)

    /** Absolute velocity of the gesture driving the wheel, for the adaptive tick. */
    var velocity by mutableFloatStateOf(0f)
    var vibrateLevel: CouiVibrateLevel = CouiVibrateLevel.Crisp
    var haptic: (Float, CouiVibrateLevel) -> Unit = { _, _ -> }
    var click: () -> Unit = {}
    var onValueChange: (Int) -> Unit = {}
    var onScrollingStop: (() -> Unit)? = null

    /** Bumped by every new animation so a superseded one stops on its next frame. */
    private var animationToken by mutableIntStateOf(0)

    val unitPx: Float get() = itemHeightPx.toFloat()

    fun effectiveIndex(): Int {
        val unit = unitPx
        if (unit <= 0f) return index
        val raw = index + (offsetPx / unit).roundToInt()
        return if (wraps) ((raw % itemCount) + itemCount) % itemCount else raw.coerceIn(0, itemCount - 1)
    }

    fun valueOfIndex(target: Int): Int = if (ignorable && target >= valueCount) NumberPickerDefaults.IgnoredValue else rangeFirst + target

    /** Which band of the wheel a press landed in: -1 above the selection, 0 inside it, 1 below. */
    fun rowZone(downY: Float): Int {
        val unit = unitPx
        if (unit <= 0f) return 0
        val row = (downY / unit).toInt() - halfVisibleCount
        return row.coerceIn(-1, 1)
    }

    /** ACTION_DOWN: the native aborts a running fling and keeps the wheel where it stopped. */
    fun beginGesture() {
        cancelAnimation()
        isDragging = true
        isScrolling = true
    }

    fun endGesture() {
        isDragging = false
    }

    fun drag(deltaY: Float) {
        val unit = unitPx
        if (unit <= 0f) return
        val next = offsetPx - deltaY
        offsetPx = if (wraps) next else next.coerceIn(-index * unit, (itemCount - 1 - index) * unit)
    }

    fun commit() {
        val unit = unitPx
        if (unit <= 0f) return
        val target = index + (offsetPx / unit).roundToInt()
        val wrapped = if (wraps) ((target % itemCount) + itemCount) % itemCount else target.coerceIn(0, itemCount - 1)
        offsetPx = 0f
        if (wrapped != index) {
            val previous = valueOfIndex(index)
            index = wrapped
            val next = valueOfIndex(wrapped)
            if (next != previous) onValueChange(next)
        }
    }

    fun cancelAnimation() {
        animationToken++
    }

    /**
     * Drives [offsetPx] from its current value to [target] on [easing] over [durationMillis].
     *
     * This is a hand-rolled tween rather than an [androidx.compose.animation.core.Animatable] because
     * a gesture has to be able to cancel a running throw from a non-suspending callback.
     */
    suspend fun animateTo(target: Float, durationMillis: Int, easing: Easing) {
        val token = ++animationToken
        val start = offsetPx
        if (durationMillis <= 0 || start == target) {
            offsetPx = target
            return
        }
        val startNanos = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            if (animationToken != token) return
            val fraction = ((now - startNanos) / 1_000_000f / durationMillis).coerceIn(0f, 1f)
            offsetPx = start + (target - start) * easing.transform(fraction)
            if (fraction >= 1f) return
        }
    }

    /** `changeValueByOne()`: a 300ms scroll of exactly one element on the fling curve. */
    suspend fun stepOne(increment: Boolean) {
        val unit = unitPx
        if (unit <= 0f) return
        isScrolling = true
        val target = offsetPx + if (increment) unit else -unit
        animateTo(
            target = if (wraps) target else target.coerceIn(-index * unit, (itemCount - 1 - index) * unit),
            durationMillis = STEP_DURATION_MILLIS,
            easing = SlowFlingEasing,
        )
        commit()
        onScrollingStop?.invoke()
    }

    /** `ensureScrollWheelAdjusted()`: 300ms onto the nearest element on the fling curve. */
    suspend fun snapToNearest() {
        val unit = unitPx
        if (unit <= 0f) return
        animateTo(
            target = (offsetPx / unit).roundToInt() * unit,
            durationMillis = SNAP_DURATION_MILLIS,
            easing = FlingEasing,
        )
        commit()
        onScrollingStop?.invoke()
    }

    /**
     * `fling(velocity)`: the release velocity is scaled by `getDampRatio()` (1.6), the travel comes
     * from the AOSP spline and is truncated to whole elements, and the whole throw runs for 1.5x the
     * spline duration on the slow fling curve.
     */
    suspend fun flingBy(velocity: Float) {
        val unit = unitPx
        if (unit <= 0f) return
        val speed = (velocity * DAMP_RATIO).coerceIn(-NumberPickerDefaults.MaxFlingVelocity, NumberPickerDefaults.MaxFlingVelocity)
        if (abs(speed) <= NumberPickerDefaults.MinFlingVelocity) return
        isScrolling = true
        val distance = splineFlingDistance(abs(speed), density)
        val stepped = if (distance > unit) distance - distance % unit else distance % unit
        val duration = (splineFlingDuration(abs(speed), density) * 1.5f).roundToInt().coerceAtLeast(1)
        val target = offsetPx - sign(speed) * stepped
        animateTo(
            target = if (wraps) target else target.coerceIn(-index * unit, (itemCount - 1 - index) * unit),
            durationMillis = duration,
            easing = SlowFlingEasing,
        )
    }

    fun isTextTooLong(measurer: TextMeasurer, text: String, style: TextStyle): Boolean {
        if (columnWidthPx <= 0) return false
        return measurer.measure(text = text, style = style, maxLines = 1, softWrap = false).size.width >= columnWidthPx
    }
}

/** One press-and-hold cycle of a picker row: a new instance restarts the repeat loop. */
@Immutable
private data class HoldStep(val increment: Boolean)

/** What a release asks the wheel to do; identity equality keeps every request distinct. */
private class PickerCommand(val action: PickerAction, val velocity: Float = 0f)

private enum class PickerAction { Fling, Snap, StepForward, StepBackward }

private const val STEP_DURATION_MILLIS = 300
private const val SNAP_DURATION_MILLIS = 300

/** `COUINumberPicker.getDampRatio()`: `min(VELOCITY_SPEED_UP_RATIO, 1.6)`. */
private const val DAMP_RATIO = 1.6f

/** `COUINumberPicker.DECELERATION_RATE` = `log(0.78) / log(0.9)`. */
private const val DECELERATION_RATE = 2.3582018f

/** `ViewConfiguration.getScrollFriction()`. */
private const val FLING_FRICTION = 0.015f

/** `COUINumberPicker.SLOW_FLING_INTERPOLATOR`, used by the fling scroller and one-row steps. */
private val SlowFlingEasing: Easing = CubicBezierEasing(0f, 0.23f, 0.1f, 1f)

/** `COUINumberPicker.FLING_INTERPOLATOR`, used by the snap-back scroller. */
private val FlingEasing: Easing = CubicBezierEasing(0f, 0f, 0.4f, 1f)

/** `COUINumberPicker.computeDeceleration(0.84f)`: ppi * 386.0878 * friction. */
private fun physicalCoeff(density: Float): Float = density * 160f * 386.0878f * 0.84f

internal fun splineDeceleration(velocity: Float, density: Float = 1f): Float = ln(abs(velocity) * 0.35f / (FLING_FRICTION * physicalCoeff(density)))

internal fun splineFlingDistance(velocity: Float, density: Float = 1f): Float {
    val coeff = FLING_FRICTION * physicalCoeff(density)
    return coeff * exp((DECELERATION_RATE / (DECELERATION_RATE - 1f)) * splineDeceleration(velocity, density))
}

internal fun splineFlingDuration(velocity: Float, density: Float = 1f): Float = exp(splineDeceleration(velocity, density) / (DECELERATION_RATE - 1f)) * 1000f

/**
 * The two hairline rules a COUI picker container draws across the selected row, one at
 * `coui_selected_background_radius` above the center and one below, inset by
 * `coui_selected_background_horizontal_padding`.
 */
@Composable
internal fun PickerSelectionDividers(
    modifier: Modifier = Modifier,
    color: Color = COUITheme.colorScheme.dividerLine,
) {
    Canvas(modifier) {
        val line = 1f
        val centerY = size.height / 2f
        val radius = NumberPickerDefaults.SelectionDividerRadius.toPx()
        val inset = NumberPickerDefaults.SelectionDividerInset.toPx()
        val width = size.width - inset * 2f
        if (width <= 0f) return@Canvas
        drawRect(color, Offset(inset, centerY - radius), Size(width, line))
        drawRect(color, Offset(inset, centerY + radius), Size(width, line))
    }
}

/** The two horizontal bars `COUINumberPicker` draws in place of an ignored value. */
@Composable
private fun IgnoreBar(modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val barWidth = NumberPickerDefaults.IgnoreBarWidth
    val barHeight = NumberPickerDefaults.IgnoreBarHeight
    val barSpacing = NumberPickerDefaults.IgnoreBarSpacing
    Canvas(modifier.size(barWidth * 2 + barSpacing, barHeight)) {
        val w = barWidth.toPx()
        val h = barHeight.toPx()
        val gap = barSpacing.toPx()
        drawRect(color, Offset(0f, 0f), Size(w, h))
        drawRect(color, Offset(w + gap, 0f), Size(w, h))
    }
}

/**
 * Contains default values used by [NumberPicker].
 */
object NumberPickerDefaults {

    val ItemHeight = 45.dp

    /** The widest a picker may grow (`coui_number_picker_view_max_width`). */
    val MaxWidth = 640.dp

    /** The text size of the centered row (`coui_numberpicker_textSize_big`, 20dp). */
    val FocusTextSize = 20.dp

    /** The text size of the other rows (`coui_numberpicker_textSize_small`, 16dp). */
    val NormalTextSize = 16.dp

    /** Width of one ignore bar (`coui_numberpicker_ignore_bar_width`, 17.67dp). */
    val IgnoreBarWidth = 17.67.dp

    /** Height of one ignore bar (`coui_numberpicker_ignore_bar_height`, 1.67dp). */
    val IgnoreBarHeight = 1.67.dp

    /** Gap between the two ignore bars (`coui_numberpicker_ignore_bar_spacing`, 4.67dp). */
    val IgnoreBarSpacing = 4.67.dp

    /** Distance from the container center to a selection divider (`coui_selected_background_radius`). */
    val SelectionDividerRadius = 24.dp

    /** Horizontal inset of the selection dividers (`coui_selected_background_horizontal_padding`). */
    val SelectionDividerInset = 16.dp

    /** How often a held row keeps stepping (`DEFAULT_LONG_PRESS_UPDATE_INTERVAL`, 300ms). */
    val LongPressUpdateInterval = 300.milliseconds

    /** Below this release velocity the wheel snaps instead of flinging (`MINIMUM_FLING_VELOCITY`). */
    val MinFlingVelocity = 750f

    /** Upper bound of the release velocity (`MAX_VELOCITY`). */
    val MaxFlingVelocity = 5000f

    /** The value a wheel reports when its ignore bar is selected (`SELECTOR_INDEX_IGNORE`). */
    val IgnoredValue = Int.MIN_VALUE

    /**
     * Creates the default [NumberPickerColors] for a [NumberPicker].
     *
     * @param selectedTextColor The color for the selected (center) item text.
     * @param unselectedTextColor The color for unselected item text.
     * @param disabledSelectedTextColor The color for the selected item text when disabled.
     * @param disabledUnselectedTextColor The color for unselected item text when disabled.
     */
    @Composable
    fun colors(
        selectedTextColor: Color = COUITheme.colorScheme.onSurface,
        unselectedTextColor: Color = COUITheme.colorScheme.onSurfaceVariantActions,
        disabledSelectedTextColor: Color = COUITheme.colorScheme.disabledOnSecondary,
        disabledUnselectedTextColor: Color = COUITheme.colorScheme.disabledOnSecondary,
    ): NumberPickerColors = remember(
        selectedTextColor,
        unselectedTextColor,
        disabledSelectedTextColor,
        disabledUnselectedTextColor,
    ) {
        NumberPickerColors(
            selectedTextColor = selectedTextColor,
            unselectedTextColor = unselectedTextColor,
            disabledSelectedTextColor = disabledSelectedTextColor,
            disabledUnselectedTextColor = disabledUnselectedTextColor,
        )
    }
}

/**
 * The colors for a [NumberPicker].
 *
 * @see NumberPickerDefaults.colors
 */
@Immutable
data class NumberPickerColors(
    private val selectedTextColor: Color,
    private val unselectedTextColor: Color,
    private val disabledSelectedTextColor: Color,
    private val disabledUnselectedTextColor: Color,
) {

    @Stable
    internal fun selectedTextColor(enabled: Boolean): Color = if (enabled) selectedTextColor else disabledSelectedTextColor

    @Stable
    internal fun unselectedTextColor(enabled: Boolean): Color = if (enabled) unselectedTextColor else disabledUnselectedTextColor
}
