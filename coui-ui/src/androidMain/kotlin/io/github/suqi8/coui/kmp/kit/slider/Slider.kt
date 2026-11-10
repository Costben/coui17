// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.slider

// Ported from COUI-Kit (ItosEO/COUI-Kit@8a09da4) coui/src/main/java/com/itos/coui/slider/Slider.kt. Extended with reverseDirection, height, haptics, keypoints, magnet threshold, secondary progress, middle fill, and touch guards.

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.SliderDefaults
import io.github.suqi8.coui.kmp.basic.computeAllKeyPointFractions
import io.github.suqi8.coui.kmp.basic.computeKeyPointFractions
import io.github.suqi8.coui.kmp.basic.secondaryProgressFraction
import io.github.suqi8.coui.kmp.basic.stepsToTickFractions
import io.github.suqi8.coui.kmp.kit.effect.SpotlightPreset
import io.github.suqi8.coui.kmp.kit.effect.rememberCouiSpotlight
import io.github.suqi8.coui.kmp.kit.input.isCouiTouchCancelled
import io.github.suqi8.coui.kmp.kit.motion.CouiTouchMotion
import io.github.suqi8.coui.kmp.kit.shape.CouiCapsuleShape
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs

/**
 * steps 是内部等分点数, showTickMarks 独立决定是否使用 SectionSeekBar 的点位与吸附。
 * 不采样背景, 不依赖 Oplus SDK; 宿主提供配色、深浅色和轨道 Shape。
 */
@Composable
internal fun CouiKitSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    colors: CouiSliderColors? = null,
    dark: Boolean = COUITheme.colorScheme.background.luminance() < 0.5f,
    shape: Shape = CouiCapsuleShape,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    showTickMarks: Boolean = false,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    height: Dp = 20.dp,
    hapticEffect: SliderDefaults.SliderHapticEffect = SliderDefaults.DefaultHapticEffect,
    showKeyPoints: Boolean = false,
    keyPoints: List<Float>? = null,
    magnetThreshold: Float = 0.02f,
    secondaryProgress: Float? = null,
    showProgress: Boolean = true,
    showThumb: Boolean = true,
    startFromMiddle: Boolean = false,
    moveByFinger: Boolean = false,
    onValueChangeFinished: () -> Unit = {},
) {
    val resolvedColors = colors ?: CouiSliderDefaults.colors(COUITheme.colorScheme.primary, dark, enabled)
    require(steps >= 0 && steps < Int.MAX_VALUE)
    require(value.isFinite() && valueRange.start.isFinite() && valueRange.endInclusive.isFinite())
    require(valueRange.endInclusive >= valueRange.start && (valueRange.endInclusive - valueRange.start).isFinite())
    val interactive = enabled && valueRange.endInclusive > valueRange.start
    val density = LocalDensity.current.density
    val layoutDirection = LocalLayoutDirection.current
    val rtl = layoutDirection == LayoutDirection.Rtl
    val effectiveReverse = if (rtl) !reverseDirection else reverseDirection
    val stepFractions = remember(steps) { stepsToTickFractions(steps) }
    val keyPointFractions = remember(keyPoints, stepFractions, valueRange, showKeyPoints) {
        val fractions = computeKeyPointFractions(keyPoints, stepFractions, valueRange, showKeyPoints)
        if (fractions.size <= 50) fractions else floatArrayOf()
    }
    val secondaryFraction = remember(secondaryProgress, valueRange) {
        secondaryProgress?.let { secondaryProgressFraction(it, valueRange) }
    }
    val hapticCode = when (hapticEffect) {
        SliderDefaults.SliderHapticEffect.None -> 0
        SliderDefaults.SliderHapticEffect.Edge -> 1
        SliderDefaults.SliderHapticEffect.Step -> 2
    }
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val onChange by rememberUpdatedState(onValueChange)
    val onFinished by rememberUpdatedState(onValueChangeFinished)
    val motion = remember(valueRange, steps, showTickMarks, density, view, keyPointFractions, magnetThreshold, hapticCode, moveByFinger) {
        CouiSliderMotion(
            range = valueRange,
            steps = steps,
            section = showTickMarks,
            density = density,
            initialValue = value,
            onValueChange = { onChange(it) },
            onValueChangeFinished = { onFinished() },
            keyPointFractions = keyPointFractions,
            magnetThreshold = magnetThreshold,
            hapticEffect = hapticCode,
            moveByFinger = moveByFinger,
        ) { code, section ->
            performSliderHaptic(view, code, section)
        }
    }
    val touch = remember(scope, density) { CouiTouchMotion(scope, density, deformationEnabled = false) }
    val renderer = remember(density, shape, layoutDirection) { CouiSliderRenderer(density, shape, layoutDirection) }
    val (spotlightInput, drawSpotlight) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberCouiSpotlight(
            interactive = interactive,
            preset = SpotlightPreset.OpaqueMedium1,
            dark = dark,
        )
    } else {
        Modifier to null
    }
    SideEffect {
        motion.syncValue(value)
        if (!interactive) {
            motion.cancelGesture()
            touch.release(Offset.Zero)
        }
    }
    DisposableEffect(motion) {
        onDispose { motion.dispose() }
    }
    DisposableEffect(touch, renderer) {
        onDispose {
            touch.dispose()
            renderer.dispose()
        }
    }
    val span = valueRange.endInclusive - valueRange.start
    val keyIncrement = span / if (steps > 0 || showTickMarks) steps + 1 else 100
    val scale = (height / 20.dp).coerceAtLeast(0.1f)
    val capInset = 14f * density * scale
    Box(
        modifier
            .defaultMinSize(minWidth = 144.dp, minHeight = 48.dp)
            .progressSemantics(value.coerceIn(valueRange), valueRange, steps)
            .semantics {
                if (!interactive) disabled()
                setProgress { next ->
                    if (!interactive || !next.isFinite()) {
                        false
                    } else {
                        motion.setFromAction(next)
                        true
                    }
                }
            }
            .onKeyEvent { event ->
                if (!interactive || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val next = when (event.key) {
                    Key.DirectionLeft -> motion.value + keyIncrement * if (effectiveReverse) 1f else -1f
                    Key.DirectionRight -> motion.value + keyIncrement * if (effectiveReverse) -1f else 1f
                    Key.DirectionUp -> motion.value + keyIncrement
                    Key.DirectionDown -> motion.value - keyIncrement
                    Key.MoveHome -> valueRange.start
                    Key.MoveEnd -> valueRange.endInclusive
                    else -> return@onKeyEvent false
                }
                motion.setFromAction(next)
                true
            }
            .focusable(interactive)
            .onSizeChanged { motion.resize((it.width - 2f * capInset).coerceAtLeast(0f)) }
            .pointerInput(motion, touch, interactive, effectiveReverse, capInset) {
                if (!interactive) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (motion.length <= 0f) return@awaitEachGesture
                    val tracker = VelocityTracker.obtain()
                    val downTime = down.uptimeMillis
                    var active = down.id
                    var pointerOffset = 0f
                    var origin = down.position
                    var dragging = false
                    var completed = false
                    fun fingerX(x: Float): Float {
                        val local = x - pointerOffset - touch.translationX.value - capInset
                        return if (effectiveReverse) motion.length - local else local
                    }
                    try {
                        tracker.addChange(downTime, down)
                        motion.press(fingerX(down.position.x))
                        touch.begin(down.position, size)
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.isCouiTouchCancelled()) break
                            val tracked = event.changes.firstOrNull { it.id == active } ?: break
                            val next = if (!tracked.pressed) event.changes.firstOrNull { it.pressed } else null
                            if (next != null) {
                                pointerOffset += next.position.x - tracked.position.x
                                origin += next.position - tracked.position
                                touch.switchPointer(tracked.position, next.position)
                                active = next.id
                                tracker.clear()
                                tracker.addChange(downTime, next)
                                continue
                            }
                            if (tracked.isConsumed) break
                            if (!tracked.pressed) {
                                val local = fingerX(tracked.position.x)
                                val inside = tracked.position.x in 0f..size.width.toFloat() &&
                                    tracked.position.y in 0f..size.height.toFloat()
                                if (dragging || inside) {
                                    val refreshRate = view.display?.refreshRate?.takeIf { it > 0f } ?: 60f
                                    if (dragging) motion.drag(local, tracker.xVelocity * if (effectiveReverse) -1f else 1f)
                                    motion.release(local, tracker.xVelocity * if (effectiveReverse) -1f else 1f, 1f / refreshRate)
                                    tracked.consume()
                                } else {
                                    motion.cancelGesture()
                                }
                                touch.release(Offset(tracker.xVelocity * 0.0003f, 0f))
                                completed = true
                                break
                            }
                            tracker.addChange(downTime, tracked)
                            tracker.computeCurrentVelocity(1000, 8000f)
                            val delta = tracked.position - origin
                            if (!dragging && abs(delta.y) > viewConfiguration.touchSlop && abs(delta.y) > abs(delta.x)) break
                            if (abs(delta.x) >= 5f) touch.move(Offset(tracked.position.x, down.position.y))
                            if (dragging || abs(delta.x) > viewConfiguration.touchSlop) {
                                dragging = true
                                motion.drag(fingerX(tracked.position.x), tracker.xVelocity * if (effectiveReverse) -1f else 1f)
                                tracked.consume()
                            }
                        }
                    } finally {
                        tracker.recycle()
                        if (!completed) {
                            motion.cancelGesture()
                            touch.release(Offset.Zero)
                        }
                    }
                }
            },
    ) {
        Canvas(
            Modifier.matchParentSize()
                .graphicsLayer {
                    translationX = touch.translationX.value
                    compositingStrategy = if (drawSpotlight == null) CompositingStrategy.Auto else CompositingStrategy.Offscreen
                }
                .then(spotlightInput),
        ) {
            val canvas = drawContext.canvas.nativeCanvas
            val trackHeightPx = with(densityScope(density)) { height.toPx() }
            renderer.draw(
                canvas = canvas,
                height = size.height,
                motion = motion,
                colors = resolvedColors,
                enabled = interactive,
                rtl = effectiveReverse,
                tickCount = if (showTickMarks && steps in 1..50) steps + 1 else 0,
                keyPointFractions = if (showTickMarks && keyPointFractions.isNotEmpty()) keyPointFractions else null,
                secondaryProgressFraction = secondaryFraction,
                showProgress = showProgress,
                showThumb = showThumb,
                startFromMiddle = startFromMiddle,
                customTrackHeight = trackHeightPx,
            )
            if (drawSpotlight != null) {
                val checkpoint = canvas.save()
                renderer.clipTrack(canvas)
                drawSpotlight()
                canvas.restoreToCount(checkpoint)
            }
        }
    }
}

private fun densityScope(density: Float): androidx.compose.ui.unit.Density = androidx.compose.ui.unit.Density(density)

private fun VelocityTracker.addChange(downTime: Long, change: PointerInputChange) {
    for (sample in change.historical) {
        val event = MotionEvent.obtain(downTime, sample.uptimeMillis, MotionEvent.ACTION_MOVE, sample.position.x, sample.position.y, 0)
        addMovement(event)
        event.recycle()
    }
    val action = when {
        !change.previousPressed -> MotionEvent.ACTION_DOWN
        else -> MotionEvent.ACTION_MOVE
    }
    val event = MotionEvent.obtain(downTime, change.uptimeMillis, action, change.position.x, change.position.y, 0)
    addMovement(event)
    event.recycle()
}

private fun performSliderHaptic(view: View, code: Int, section: Boolean) {
    if (view.performHapticFeedback(code)) return
    if (section && code == 308 && view.performHapticFeedback(302)) return
    val standard = when {
        code == 306 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> HapticFeedbackConstants.GESTURE_END

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            if (section) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.SEGMENT_FREQUENT_TICK

        else -> HapticFeedbackConstants.CLOCK_TICK
    }
    view.performHapticFeedback(standard)
}
