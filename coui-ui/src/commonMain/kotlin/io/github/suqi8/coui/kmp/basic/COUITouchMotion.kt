// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.isFinished
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/** The COS17 touch parameter set ported from CommonParams, TouchMotionTierParams and PressParams. */
@Immutable
data class COUITouchMotionSpec(
    val deadZone: Dp = 5.dp,
    // `TouchMotionTierParamsExtKt.touchMotionTierParamsDefault` builds the tier as
    // `TouchMotionTierParams(28f, 0.05f, 150f, 0.55f, ...)`, and the constructor's parameter order is
    // `(iolMaxStretchDistance, iolRubberCurveRatio, iolMaxDeformRubberDistance,
    // iolDeformRubberCurveRatio)`. The two budgets are therefore 28dp of translation against a 0.05
    // curve, and 150dp of deformation against a 0.55 one — not the other way round.
    val maxStretchDistance: Dp = 28.dp,
    val rubberCurveRatio: Float = 0.05f,
    val maxDeformRubberDistance: Dp = 150.dp,
    val deformRubberCurveRatio: Float = 0.55f,
    val maxScale: Float = 1.2f,
    val k1: Int = 350000,
    val k2: Int = 15,
    val pressScaleMin: Float = 0.85f,
    val pressScaleMax: Float = 0.97f,
    val pressMinAreaWidthDp: Float = 20f,
    val pressMaxAreaWidthDp: Float = 150f,
    val pressAspectActivationThreshold: Float = 2f,
    val pressAspectExtremeRatio: Float = 15f,
    val pressAspectBaseScale: Float = 0.85f,
    val pressAspectExtremeScale: Float = 1f,
    val dragSpring: SpringSpec<Float> = folmeSpring(damping = 0.85f, response = 0.15f),
    // COUISpringForce maps bounce to dampingRatio = 1 - bounce.
    val handUpSpring: SpringSpec<Float> = folmeSpring(damping = 0.35f, response = 0.42f),
    val pressSpring: SpringSpec<Float> = folmeSpring(damping = 1f, response = 0.25f),
    val releaseSpring: SpringSpec<Float> = folmeSpring(damping = 0.4f, response = 0.5f),
    val candyPressSpring: SpringSpec<Float> = folmeSpring(damping = 1f, response = 0.3f),
    val candyReleaseSpring: SpringSpec<Float> = folmeSpring(damping = 0.5f, response = 0.5f),
    // Additional COS17 PressCalc and hand-up resolver inputs follow the stable legacy fields.
    val pressBezierX1: Float = 0f,
    val pressBezierY1: Float = 0f,
    val pressBezierX2: Float = 0f,
    val pressBezierY2: Float = 1f,
    val handUpSmallAreaRefWidthDp: Float = 56f,
    val handUpLargeAreaRefWidthDp: Float = 100f,
    val handUpAreaBezierX1: Float = 0.55026454f,
    val handUpAreaBezierY1: Float = 0f,
    val handUpAreaBezierX2: Float = 0.8915344f,
    val handUpAreaBezierY2: Float = 0.18783069f,
    val handUpBaseResponse: Float = 0.42f,
    val handUpBaseBounce: Float = 0.65f,
    val handUpLargeAreaResponse: Float = 0.45f,
    val handUpLargeAreaBounce: Float = 0.65f,
    val handUpMaxInitialVelocity: Float = 1000f,
    val handUpBounceGain: Float = 1f,
)

/** The local contribution from one native-style touch owner. */
@Immutable
data class COUITouchMotionValues(
    val translation: Offset = Offset.Zero,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val pressed: Boolean = false,
    val lightPosition: Offset = Offset(0.5f, 0.5f),
    val lightProgress: Float = 0f,
)

/** Observable transform for a touch owner, useful when a material particle shares the owner. */
@Stable
class COUITouchMotionState internal constructor() {
    internal var values by mutableStateOf<State<COUITouchMotionValues>>(mutableStateOf(COUITouchMotionValues()))
    val translation: Offset get() = values.value.translation
    val scaleX: Float get() = values.value.scaleX
    val scaleY: Float get() = values.value.scaleY
}

@Composable
fun rememberCOUITouchMotionState(): COUITouchMotionState = remember { COUITouchMotionState() }

internal fun couiRubberBandApply(offset: Float, curveRatio: Float, maxDistance: Float): Float {
    if (maxDistance <= 0f) return 0f
    return sign(offset) * (1f - 1f / ((abs(offset) * curveRatio / maxDistance) + 1f)) * maxDistance
}

internal fun couiRubberBandInverse(result: Float, curveRatio: Float, maxDistance: Float): Float {
    if (maxDistance <= 0f) return result
    val absolute = abs(result)
    if (absolute >= maxDistance) return sign(result) * maxDistance
    val remaining = maxDistance - absolute
    if (curveRatio <= 0f || remaining <= 0f) return result
    return sign(result) * (maxDistance * absolute) / (curveRatio * remaining)
}

/** Exact TouchAnimatorParamProvider.calculateScaleWithK port. */
internal fun couiScaleWithK(width: Float, height: Float, maxScale: Float, k1: Int, k2: Int): Float {
    val area = (width * height).toInt()
    val maxSide = max(width, height)
    val minSide = min(width, height)
    if (area <= 38_416 || minSide == 0f) return maxScale
    val decay = min(
        (k2 / 1000f) * ((maxSide / minSide) - 1f) +
            (k1 / 1_000_000f) * ln(area / 38_416f),
        1f,
    )
    return max((maxScale - 1.03f) * (1f - decay) + 1.03f, 1.03f)
}

internal data class COUIDeformScales(val x: Float, val y: Float)

internal fun couiSpotlightRadius(
    start: Float,
    end: Float,
    extinguishExtra: Float,
    progress: Float,
    extinguishing: Boolean,
): Float {
    val t = progress.coerceIn(0f, 1f)
    val radius = start + (end - start) * t
    return if (extinguishing) radius + (extinguishExtra - start) * (1f - t) else radius
}

/** Native VelocityTracker uses units=1 (px/ms), followed by its 0.3 velocity gain. */
internal fun couiNativeTouchVelocity(pixelsPerSecond: Float): Float = pixelsPerSecond * 0.3f / 1000f

internal fun couiTouchMotionScaleContribution(deformScale: Float, pressScale: Float): Float = deformScale + pressScale - 1f

internal fun couiCubicBezierInterpolate(
    fraction: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
): Float {
    val x = fraction.coerceIn(0f, 1f)
    fun coordinate(t: Float, first: Float, second: Float): Float {
        val inverse = 1f - t
        return 3f * inverse * inverse * t * first + 3f * inverse * t * t * second + t * t * t
    }
    var low = 0f
    var high = 1f
    repeat(18) {
        val middle = (low + high) * 0.5f
        if (coordinate(middle, x1, x2) < x) low = middle else high = middle
    }
    return coordinate((low + high) * 0.5f, y1, y2)
}

internal data class COUIHandUpSpringValues(val response: Float, val bounce: Float, val initialVelocity: Float)

internal fun couiResolveHandUpSpring(
    widthPx: Float,
    heightPx: Float,
    density: Float,
    pan: Float,
    totalDistance: Float,
    previousVelocity: Float,
    baseResponse: Float,
    baseBounce: Float,
    largeAreaResponse: Float,
    largeAreaBounce: Float,
    smallAreaRefWidthDp: Float,
    largeAreaRefWidthDp: Float,
    bezierX1: Float,
    bezierY1: Float,
    bezierX2: Float,
    bezierY2: Float,
    maxInitialVelocity: Float,
    bounceGain: Float,
): COUIHandUpSpringValues {
    val area = widthPx * heightPx
    val smallArea = (smallAreaRefWidthDp * density) * (smallAreaRefWidthDp * density)
    val largeArea = (largeAreaRefWidthDp * density) * (largeAreaRefWidthDp * density)
    val areaFraction = if (area <= 0f || widthPx <= 0f || heightPx <= 0f || largeArea <= smallArea) {
        0f
    } else {
        ((area - smallArea) / (largeArea - smallArea)).coerceIn(0f, 1f)
    }
    val blend = couiCubicBezierInterpolate(areaFraction, bezierX1, bezierY1, bezierX2, bezierY2)
    val response = baseResponse + (largeAreaResponse - baseResponse) * blend
    val configuredBounce = baseBounce + (largeAreaBounce - baseBounce) * blend
    val initialVelocity = if (maxInitialVelocity <= 0f || totalDistance <= 0f) {
        0f
    } else {
        ((abs(pan) / totalDistance) * previousVelocity).coerceIn(-maxInitialVelocity, maxInitialVelocity)
    }
    val velocityBounce = if (maxInitialVelocity <= 0f) {
        0f
    } else {
        (kotlin.math.abs(initialVelocity) / maxInitialVelocity).coerceAtMost(1f) * bounceGain
    }
    val bounce = max(kotlin.math.abs(velocityBounce), kotlin.math.abs(configuredBounce * bounceGain)).coerceAtMost(1f)
    return COUIHandUpSpringValues(response, bounce, initialVelocity)
}

private fun directionScale(distance: Float, curveRatio: Float, maxDistance: Float, maxScale: Float, horizontal: Boolean): COUIDeformScales {
    if (distance <= 0f || maxDistance <= 0f) return COUIDeformScales(1f, 1f)
    val expanded = 1f + (couiRubberBandApply(distance, curveRatio, maxDistance) / maxDistance) * (maxScale - 1f)
    val compressed = if (expanded > 0f) 1f / expanded else 1f
    return if (horizontal) COUIDeformScales(expanded, compressed) else COUIDeformScales(compressed, expanded)
}

/** Exact log-space area-conserving blend used when both axes move. */
internal fun couiDeformationScales(dx: Float, dy: Float, spec: COUITouchMotionSpec, width: Float, height: Float, density: Float = 1f): COUIDeformScales {
    val maxScale = couiScaleWithK(width, height, spec.maxScale, spec.k1, spec.k2)
    val maxDistance = spec.maxDeformRubberDistance.value * density
    val x = directionScale(abs(dx), spec.deformRubberCurveRatio, maxDistance, maxScale, true)
    val y = directionScale(abs(dy), spec.deformRubberCurveRatio, maxDistance, maxScale, false)
    val total = abs(dx) + abs(dy)
    if (total <= 0f) return COUIDeformScales(1f, 1f)
    if (abs(dy) <= 0f) return x
    if (abs(dx) <= 0f) return y
    val wx = abs(dx) / total
    val wy = abs(dy) / total
    return COUIDeformScales(
        exp(ln(x.x) * wx + ln(y.x) * wy),
        exp(ln(x.y) * wx + ln(y.y) * wy),
    )
}

internal fun couiPressScale(width: Float, height: Float, spec: COUITouchMotionSpec): Float {
    if (width <= 0f || height <= 0f) return 1f
    val minArea = spec.pressMinAreaWidthDp * spec.pressMinAreaWidthDp
    val maxArea = spec.pressMaxAreaWidthDp * spec.pressMaxAreaWidthDp
    val area = (width * height).coerceIn(minArea, maxArea)
    val areaProgress = (area - minArea) / (maxArea - minArea)
    val areaCurve = couiCubicBezierInterpolate(
        areaProgress,
        spec.pressBezierX1,
        spec.pressBezierY1,
        spec.pressBezierX2,
        spec.pressBezierY2,
    )
    val areaScale = spec.pressScaleMin + (spec.pressScaleMax - spec.pressScaleMin) * areaCurve
    val aspect = max(width / height, height / width)
    if (aspect <= spec.pressAspectActivationThreshold) return areaScale
    val aspectProgress = (
        (aspect - spec.pressAspectActivationThreshold) /
            (spec.pressAspectExtremeRatio - spec.pressAspectActivationThreshold)
        ).coerceIn(0f, 1f)
    val aspectCurve = couiCubicBezierInterpolate(
        aspectProgress,
        spec.pressBezierX1,
        spec.pressBezierY1,
        spec.pressBezierX2,
        spec.pressBezierY2,
    )
    val aspectScale = spec.pressAspectBaseScale +
        (spec.pressAspectExtremeScale - spec.pressAspectBaseScale) * aspectCurve
    return ((areaScale + aspectScale) / 2f).coerceIn(0.4f, 1f)
}

/** A retargetable native-style spring that keeps its velocity when a drag target changes. */
@Stable
internal class COUITouchSpring(initialValue: Float) {
    private var animation by mutableStateOf(AnimationState(initialValue))
    val value: Float get() = animation.value
    val velocity: Float get() = animation.velocity

    fun snapTo(value: Float) {
        animation = AnimationState(value)
    }

    suspend fun animateTo(target: Float, spec: SpringSpec<Float>, initialVelocity: Float = velocity) {
        // Preserve the clock of an interrupted spring, so per-frame drag targets still advance.
        // A completed spring starts a fresh clock rather than consuming the idle time between gestures.
        val lastFrame = if (animation.isFinished) Long.MIN_VALUE else animation.lastFrameTimeNanos
        animation = AnimationState(initialValue = value, initialVelocity = initialVelocity, lastFrameTimeNanos = lastFrame)
        animation.animateTo(target, spec, sequentialAnimation = true)
    }
}

@Composable
internal fun Modifier.couiTouchMotion(
    surface: Modifier = Modifier,
    enabled: Boolean = true,
    spec: COUITouchMotionSpec = COUITouchMotionSpec(),
    shape: Shape? = null,
    lightParams: SpotLightParams = SpotLightDefaults.capsuleParams2(false),
    motionState: COUITouchMotionState? = null,
    pressScaleEnabled: Boolean = true,
    consumeDrag: Boolean = true,
    lightEnabled: Boolean = true,
    hitTestWhenDisabled: Boolean = true,
    extraTransform: GraphicsLayerScope.() -> Unit = {},
): Modifier {
    val ownerState = motionState ?: rememberCOUITouchMotionState()
    val density = LocalDensity.current
    // CommonParams.iolDeadZone is authored in the touch stream's physical pixels (5 px),
    // unlike the distance tiers below which are dp and are converted through density.
    val deadZonePx = spec.deadZone.value
    val maxStretchPx = with(density) { spec.maxStretchDistance.toPx() }
    val lightHeight = with(density) { lightParams.lightHeight.toDp() }
    val widthPx = remember { mutableFloatStateOf(0f) }
    val heightPx = remember { mutableFloatStateOf(0f) }
    val translateXAnim = remember { COUITouchSpring(0f) }
    val translateYAnim = remember { COUITouchSpring(0f) }
    val scaleXAnim = remember { COUITouchSpring(1f) }
    val scaleYAnim = remember { COUITouchSpring(1f) }
    val pressProgress = remember { COUITouchSpring(0f) }
    val lightX = remember { COUITouchSpring(0.5f) }
    val lightY = remember { COUITouchSpring(0.5f) }
    val lightProgress = remember { Animatable(0f) }
    var pressed by remember { mutableStateOf(false) }
    var extinguishing by remember { mutableStateOf(false) }
    val lightPosition = Offset(lightX.value, lightY.value)

    val widthDp = with(density) { widthPx.value.toDp().value }
    val heightDp = with(density) { heightPx.value.toDp().value }
    val pressTarget = if (pressScaleEnabled) couiPressScale(widthDp, heightDp, spec) else 1f
    val usesCandySpring = widthDp > 0f && heightDp > 0f &&
        max(widthDp / heightDp, heightDp / widthDp) > spec.pressAspectActivationThreshold
    LaunchedEffect(pressed, pressTarget, usesCandySpring) {
        val pressSpec = if (usesCandySpring) spec.candyPressSpring else spec.pressSpring
        val releaseSpec = if (usesCandySpring) spec.candyReleaseSpring else spec.releaseSpring
        pressProgress.animateTo(if (pressed) 1f else 0f, if (pressed) pressSpec else releaseSpec)
    }
    // A popup remains mounted during exit. Cancelling its pointer coroutine must also clear the
    // contributions whose release jobs belonged to that coroutine, before a possible re-entry.
    LaunchedEffect(enabled) {
        if (!enabled) {
            pressed = false
            translateXAnim.snapTo(0f)
            translateYAnim.snapTo(0f)
            scaleXAnim.snapTo(1f)
            scaleYAnim.snapTo(1f)
            lightProgress.snapTo(0f)
        }
    }
    ownerState.values = remember(translateXAnim, translateYAnim, scaleXAnim, scaleYAnim, pressTarget) {
        derivedStateOf {
            val pressScale = 1f - (1f - pressTarget) * pressProgress.value
            COUITouchMotionValues(
                translation = Offset(translateXAnim.value, translateYAnim.value),
                scaleX = couiTouchMotionScaleContribution(scaleXAnim.value, pressScale),
                scaleY = couiTouchMotionScaleContribution(scaleYAnim.value, pressScale),
                pressed = pressed,
            )
        }
    }

    val sizeModifier = Modifier.onSizeChanged {
        widthPx.value = it.width.toFloat()
        heightPx.value = it.height.toFloat()
    }
    val pointerModifier = sizeModifier
        .pointerInput(enabled, spec, density.density, consumeDrag) {
            if (!enabled) return@pointerInput
            coroutineScope {
                val motionScope = this
                var motionJob: Job? = null
                var lightMotionJob: Job? = null
                fun updateLight(position: Offset) {
                    // Native coordinates remain unbounded. Only the owner's silhouette clips the
                    // moving halo, allowing it to leave the surface and return in the same gesture.
                    val x = position.x / widthPx.value.coerceAtLeast(1f)
                    val y = position.y / heightPx.value.coerceAtLeast(1f)
                    lightMotionJob?.cancel()
                    if (lightProgress.value == 0f) {
                        lightX.snapTo(x)
                        lightY.snapTo(y)
                    } else {
                        lightMotionJob = motionScope.launch {
                            coroutineScope {
                                launch { lightX.animateTo(x, SpotLightDefaults.MoveSpring) }
                                launch { lightY.animateTo(y, SpotLightDefaults.MoveSpring) }
                            }
                        }
                    }
                }
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        var pointerId = down.id
                        motionJob?.cancel()
                        var start = down.position
                        val rubberRest = Offset(
                            couiRubberBandInverse(translateXAnim.value, spec.rubberCurveRatio, maxStretchPx),
                            couiRubberBandInverse(translateYAnim.value, spec.rubberCurveRatio, maxStretchPx),
                        )
                        val deformDistance = with(density) { spec.maxDeformRubberDistance.toPx() }
                        val deformRange = (couiScaleWithK(widthPx.value, heightPx.value, spec.maxScale, spec.k1, spec.k2) - 1f).coerceAtLeast(0.001f)
                        val deformRest = Offset(
                            couiRubberBandInverse(((scaleXAnim.value - 1f) / deformRange).coerceAtMost(1f) * deformDistance, spec.deformRubberCurveRatio, deformDistance),
                            couiRubberBandInverse(((scaleYAnim.value - 1f) / deformRange).coerceAtMost(1f) * deformDistance, spec.deformRubberCurveRatio, deformDistance),
                        )
                        var last = start
                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, down.position)
                        var dragging = false
                        var velocityX = 0f
                        var velocityY = 0f
                        pressed = true
                        extinguishing = false
                        updateLight(down.position)
                        launch { lightProgress.animateTo(1f, SpotLightDefaults.GlowSpring) }
                        var finished = false
                        try {
                            while (!finished) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                var change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null || change.changedToUpIgnoreConsumed()) {
                                    if (change != null) {
                                        tracker.addPosition(change.uptimeMillis, change.position)
                                        updateLight(change.position)
                                    }
                                    val replacement = event.changes.firstOrNull { it.pressed }
                                    if (replacement == null) {
                                        val velocity = tracker.calculateVelocity()
                                        velocityX = couiNativeTouchVelocity(velocity.x)
                                        velocityY = couiNativeTouchVelocity(velocity.y)
                                        finished = true
                                        continue
                                    }
                                    // Keep the accumulated displacement when the active finger changes.
                                    start = replacement.position - (last - start)
                                    change = replacement
                                    pointerId = replacement.id
                                    tracker.resetTracking()
                                }
                                val position = change.position
                                change.historical.forEach { tracker.addPosition(it.uptimeMillis, it.position) }
                                tracker.addPosition(change.uptimeMillis, position)
                                val dx = position.x - start.x
                                val dy = position.y - start.y
                                last = position
                                updateLight(position)
                                if (!dragging && (abs(dx) >= deadZonePx || abs(dy) >= deadZonePx)) dragging = true
                                if (dragging && (abs(dx) >= deadZonePx || abs(dy) >= deadZonePx)) {
                                    if (consumeDrag) change.consume()
                                    val deform = couiDeformationScales(dx + deformRest.x, dy + deformRest.y, spec, widthPx.value, heightPx.value, density.density)
                                    motionJob?.cancel()
                                    motionJob = motionScope.launch {
                                        coroutineScope {
                                            launch {
                                                translateXAnim.animateTo(
                                                    couiRubberBandApply(dx + rubberRest.x, spec.rubberCurveRatio, maxStretchPx),
                                                    spec.dragSpring,
                                                )
                                            }
                                            launch {
                                                translateYAnim.animateTo(
                                                    couiRubberBandApply(dy + rubberRest.y, spec.rubberCurveRatio, maxStretchPx),
                                                    spec.dragSpring,
                                                )
                                            }
                                            launch { scaleXAnim.animateTo(deform.x, spec.dragSpring) }
                                            launch { scaleYAnim.animateTo(deform.y, spec.dragSpring) }
                                        }
                                    }
                                }
                            }
                        } finally {
                            motionJob?.cancel()
                            pressed = false
                            extinguishing = true
                            motionScope.launch { lightProgress.animateTo(0f, SpotLightDefaults.ExtinguishSpring) }
                            val handUp = couiResolveHandUpSpring(
                                widthPx.value,
                                heightPx.value,
                                density.density,
                                translateXAnim.value,
                                with(density) { spec.maxStretchDistance.toPx() },
                                velocityX,
                                spec.handUpBaseResponse,
                                spec.handUpBaseBounce,
                                spec.handUpLargeAreaResponse,
                                spec.handUpLargeAreaBounce,
                                spec.handUpSmallAreaRefWidthDp,
                                spec.handUpLargeAreaRefWidthDp,
                                spec.handUpAreaBezierX1,
                                spec.handUpAreaBezierY1,
                                spec.handUpAreaBezierX2,
                                spec.handUpAreaBezierY2,
                                spec.handUpMaxInitialVelocity,
                                spec.handUpBounceGain,
                            )
                            val bounceSpring = folmeSpring<Float>(damping = 1f - handUp.bounce, response = handUp.response)
                            val handUpY = couiResolveHandUpSpring(
                                widthPx.value,
                                heightPx.value,
                                density.density,
                                translateYAnim.value,
                                with(density) { spec.maxStretchDistance.toPx() },
                                velocityY,
                                spec.handUpBaseResponse,
                                spec.handUpBaseBounce,
                                spec.handUpLargeAreaResponse,
                                spec.handUpLargeAreaBounce,
                                spec.handUpSmallAreaRefWidthDp,
                                spec.handUpLargeAreaRefWidthDp,
                                spec.handUpAreaBezierX1,
                                spec.handUpAreaBezierY1,
                                spec.handUpAreaBezierX2,
                                spec.handUpAreaBezierY2,
                                spec.handUpMaxInitialVelocity,
                                spec.handUpBounceGain,
                            )
                            fun deformationHandUp(scale: Float, velocity: Float): COUIHandUpSpringValues = couiResolveHandUpSpring(
                                widthPx.value, heightPx.value, density.density,
                                abs(scale - 1f), maxStretchPx, velocity,
                                spec.handUpBaseResponse, spec.handUpBaseBounce,
                                spec.handUpLargeAreaResponse, spec.handUpLargeAreaBounce,
                                spec.handUpSmallAreaRefWidthDp, spec.handUpLargeAreaRefWidthDp,
                                spec.handUpAreaBezierX1, spec.handUpAreaBezierY1,
                                spec.handUpAreaBezierX2, spec.handUpAreaBezierY2,
                                spec.handUpMaxInitialVelocity, spec.handUpBounceGain,
                            )
                            val deformX = deformationHandUp(scaleXAnim.value, velocityX)
                            val deformY = deformationHandUp(scaleYAnim.value, velocityY)
                            motionJob = motionScope.launch {
                                coroutineScope {
                                    launch { translateXAnim.animateTo(0f, bounceSpring, initialVelocity = handUp.initialVelocity) }
                                    launch {
                                        translateYAnim.animateTo(
                                            0f,
                                            folmeSpring(damping = 1f - handUpY.bounce, response = handUpY.response),
                                            initialVelocity = handUpY.initialVelocity,
                                        )
                                    }
                                    launch {
                                        scaleXAnim.animateTo(
                                            1f,
                                            folmeSpring(damping = 1f - deformX.bounce, response = deformX.response, visibilityThreshold = 0.0005f),
                                            initialVelocity = deformX.initialVelocity,
                                        )
                                    }
                                    launch {
                                        scaleYAnim.animateTo(
                                            1f,
                                            folmeSpring(damping = 1f - deformY.bounce, response = deformY.response, visibilityThreshold = 0.0005f),
                                            initialVelocity = deformY.initialVelocity,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    val visualModifier = Modifier.graphicsLayer {
        val pressScale = 1f - (1f - pressTarget) * pressProgress.value
        translationX = translateXAnim.value
        translationY = translateYAnim.value
        // Native collector combines unit contributions by adding deltas from 1.
        scaleX = scaleXAnim.value + pressScale - 1f
        scaleY = scaleYAnim.value + pressScale - 1f
        transformOrigin = TransformOrigin(0.5f, 0.5f)
        extraTransform()
    }
    val lightRadius = with(density) {
        couiSpotlightRadius(
            lightParams.startRadius,
            lightParams.endRadius,
            lightParams.endRadius * 2f,
            lightProgress.value,
            extinguishing,
        ).toDp()
    }
    val lightModifier = if (shape == null) {
        Modifier.spotLight(
            position = lightPosition,
            radius = lightRadius,
            intensity = lightParams.endIntensity * lightProgress.value,
            lightHeight = lightHeight,
            ambientStrength = lightParams.ambientStrength,
            diffuseStrength = lightParams.diffuseStrength,
            noise = lightParams.noise,
            dither = lightParams.ditherN,
            blendMode = lightParams.blendMode,
            enabled = enabled && lightEnabled,
        )
    } else {
        Modifier.spotLight(
            position = lightPosition,
            radius = lightRadius,
            intensity = lightParams.endIntensity * lightProgress.value,
            lightHeight = lightHeight,
            ambientStrength = lightParams.ambientStrength,
            diffuseStrength = lightParams.diffuseStrength,
            noise = lightParams.noise,
            dither = lightParams.ditherN,
            blendMode = lightParams.blendMode,
            enabled = enabled && lightEnabled,
            clipShape = shape,
        )
    }
    // Keep the surface, clipped light and content inside one transform. The pointer detector stays
    // outside it so the moving visual layer does not alter the gesture's displacement coordinates.
    // A disabled detector handles nothing but still wins the hit test, which hides every sibling
    // beneath it from the touch. Owners that hand touches on while disabled drop it altogether.
    val gestureModifier = if (enabled || hitTestWhenDisabled) pointerModifier else sizeModifier
    return this.then(gestureModifier).then(visualModifier).then(surface).then(lightModifier)
}
