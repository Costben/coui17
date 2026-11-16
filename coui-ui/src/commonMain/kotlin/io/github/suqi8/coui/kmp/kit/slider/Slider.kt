// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.slider

// Ported from COUI-Kit (ItosEO/COUI-Kit@8a09da4) coui/src/main/java/com/itos/coui/slider/Slider.kt. Extended with reverseDirection, height, haptics, keypoints, magnet threshold, secondary progress, middle fill, and touch guards.

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.SliderDefaults
import io.github.suqi8.coui.kmp.basic.computeKeyPointFractions
import io.github.suqi8.coui.kmp.basic.secondaryProgressFraction
import io.github.suqi8.coui.kmp.basic.stepsToTickFractions
import io.github.suqi8.coui.kmp.kit.effect.SpotlightPreset
import io.github.suqi8.coui.kmp.kit.effect.rememberCouiSpotlight
import io.github.suqi8.coui.kmp.kit.input.isCouiTouchCancelled
import io.github.suqi8.coui.kmp.kit.motion.CouiTouchMotion
import io.github.suqi8.coui.kmp.kit.motion.CouiVelocityTracker
import io.github.suqi8.coui.kmp.kit.shape.CouiCapsuleShape
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.CouiHapticEffect
import io.github.suqi8.coui.kmp.utils.rememberCouiHaptic
import kotlin.math.abs

private const val DEFAULT_FRAME_SECONDS = 1f / 60f
private const val MAX_FLING_VELOCITY = 8000f
private const val RELEASE_VELOCITY_SCALE = 0.0003f

/**
 * Cross-platform COUI slider track and thumb component. [steps] defines discrete sections
 * (`COUISectionSeekBar` behavior when > 0), while [showTickMarks] independently controls drawing
 * tick dots on the track. Does not sample backdrop or depend on Oplus SDK; host supplies colors,
 * dark mode flag, and track [Shape].
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
    minHeight: Dp = 48.dp,
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
    val haptic by rememberUpdatedState(rememberCouiHaptic())
    val scope = rememberCoroutineScope()
    val onChange by rememberUpdatedState(onValueChange)
    val onFinished by rememberUpdatedState(onValueChangeFinished)
    val motion = remember(valueRange, steps, density, keyPointFractions, magnetThreshold, hapticCode, moveByFinger, scope) {
        CouiSliderMotion(
            scope = scope,
            range = valueRange,
            steps = steps,
            section = steps > 0,
            density = density,
            initialValue = value,
            onValueChange = { onChange(it) },
            onValueChangeFinished = { onFinished() },
            keyPointFractions = keyPointFractions,
            magnetThreshold = magnetThreshold,
            hapticEffect = hapticCode,
            moveByFinger = moveByFinger,
        ) { code, _ ->
            performSliderHaptic(haptic, code)
        }
    }
    val touch = remember(scope, density) { CouiTouchMotion(scope, density, deformationEnabled = false) }
    val renderer = remember(density, shape, layoutDirection) { CouiSliderRenderer(density, shape, layoutDirection) }
    // rememberCouiSpotlight internally gates on isKitRuntimeShaderSupported() (API 33+ on Android, true on Skiko).
    val (spotlightInput, drawSpotlight) = rememberCouiSpotlight(
        interactive = interactive,
        preset = SpotlightPreset.OpaqueMedium1,
        dark = dark,
    )
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
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        modifier
            .defaultMinSize(minWidth = 144.dp, minHeight = minHeight)
            .fillMaxWidth()
            .onGloballyPositioned { coordinates = it }
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
                    val tracker = CouiVelocityTracker(down.uptimeMillis)
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
                        coordinates?.takeIf { it.isAttached }?.let { tracker.add(down, it, start = true) }
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
                                coordinates?.takeIf { it.isAttached }?.let { tracker.add(next, it, start = true) }
                                continue
                            }
                            if (tracked.isConsumed) break
                            if (!tracked.pressed) {
                                val local = fingerX(tracked.position.x)
                                val inside = tracked.position.x in 0f..size.width.toFloat() &&
                                    tracked.position.y in 0f..size.height.toFloat()
                                val xVelocityPxPerSec = (tracker.releaseVelocity().x / RELEASE_VELOCITY_SCALE)
                                    .coerceIn(-MAX_FLING_VELOCITY, MAX_FLING_VELOCITY)
                                val directedVelocity = xVelocityPxPerSec * if (effectiveReverse) -1f else 1f
                                if (dragging || inside) {
                                    if (dragging) motion.drag(local, directedVelocity)
                                    motion.release(local, directedVelocity, DEFAULT_FRAME_SECONDS)
                                    tracked.consume()
                                } else {
                                    motion.cancelGesture()
                                }
                                touch.release(Offset(xVelocityPxPerSec * RELEASE_VELOCITY_SCALE, 0f))
                                completed = true
                                break
                            }
                            coordinates?.takeIf { it.isAttached }?.let { tracker.add(tracked, it) }
                            val xVelocityPxPerSec = (tracker.releaseVelocity().x / RELEASE_VELOCITY_SCALE)
                                .coerceIn(-MAX_FLING_VELOCITY, MAX_FLING_VELOCITY)
                            val delta = tracked.position - origin
                            if (!dragging && abs(delta.y) > viewConfiguration.touchSlop && abs(delta.y) > abs(delta.x)) break
                            if (abs(delta.x) >= 5f) touch.move(Offset(tracked.position.x, down.position.y))
                            if (dragging || abs(delta.x) > viewConfiguration.touchSlop) {
                                dragging = true
                                motion.drag(
                                    fingerX(tracked.position.x),
                                    xVelocityPxPerSec * if (effectiveReverse) -1f else 1f,
                                )
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
            val canvas = drawContext.canvas
            renderer.draw(
                canvas = canvas,
                height = size.height,
                length = motion.length,
                visualFraction = motion.visualFraction,
                heightFraction = motion.heightFraction,
                thumbRadiusDp = motion.thumbRadius.value,
                texture = motion.texture.value,
                thumbCover = motion.thumbCover.value,
                glitter = motion.glitter.value,
                colors = resolvedColors,
                enabled = interactive,
                rtl = effectiveReverse,
                tickCount = if (showTickMarks && steps in 1..50) steps + 1 else 0,
                keyPointFractions = if (showTickMarks && keyPointFractions.isNotEmpty()) keyPointFractions else null,
                secondaryProgressFraction = secondaryFraction,
                showProgress = showProgress,
                showThumb = showThumb,
                startFromMiddle = startFromMiddle,
                customTrackHeight = height.toPx(),
            )
            if (drawSpotlight != null) {
                canvas.save()
                try {
                    renderer.clipTrack(canvas)
                    drawSpotlight()
                } finally {
                    canvas.restore()
                }
            }
        }
    }
}

private fun performSliderHaptic(
    performHaptic: (CouiHapticEffect) -> Unit,
    code: Int,
) {
    val effect = when (code) {
        306 -> CouiHapticEffect.EdgeShort
        308 -> CouiHapticEffect.Strength
        305 -> CouiHapticEffect.Stepping
        302 -> CouiHapticEffect.Switch
        else -> CouiHapticEffect.Stepping
    }
    performHaptic(effect)
}
