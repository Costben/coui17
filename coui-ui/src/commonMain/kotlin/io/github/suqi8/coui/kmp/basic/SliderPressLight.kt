// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.shader.RuntimeShader
import io.github.suqi8.coui.kmp.shader.asBrush
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * The press feedback of `COUISeekBar` while a finger is down, shared by every slider.
 *
 * Three native pieces are driven from the same touch:
 * - `COUISeekBarCore.touchAnim` runs on `ACTION_DOWN` (`shouldEnlargeOnDown` is `true`), so the
 *   inactive track grows and the thumb goes from `coui_seekbar_thumb_radius` 6dp to
 *   `coui_seekbar_thumb_max_radius` 8dp as soon as the slider is touched, not only once it is
 *   dragged; [isPressed] carries that.
 * - `SpotLightEffectRenderer` paints a `TYPE_OPAQUE_MEDIUM_1` spotlight that follows the finger,
 *   clipped to the (enlarged) background capsule and drawn in the `OVERLAY` phase, i.e. over the
 *   active track and the thumb; see [drawLight].
 * - `TexturedThumbRenderer` turns the solid thumb into a frosted one while pressed; see
 *   [drawTexturedThumb].
 *
 * Positions are kept in the coordinates of the slider's touch container; [drawLight] maps them
 * onto the track canvas, which is centred in the container on the cross axis.
 */
@Stable
internal class SliderPressLight {
    /** Whether a finger is down on the slider. */
    var isPressed by mutableStateOf(false)
        private set

    /** The last touch position in the touch container. */
    var touchPosition by mutableStateOf(Offset.Zero)
        private set

    private var containerSize by mutableStateOf(Size.Zero)
    private var extinguishing by mutableStateOf(false)
    private var lightX by mutableFloatStateOf(0f)
    private var lightY by mutableFloatStateOf(0f)
    private var lightVelocityX = 0f
    private var lightVelocityY = 0f
    private var targetX = 0f
    private var targetY = 0f
    private var followJob: Job? = null
    private val glow = Animatable(0f)

    // TexturedThumbRenderer.mTextureAlpha and mLayerCAlpha.
    private val texture = Animatable(0f)
    private val solid = Animatable(0f)

    internal fun press(scope: CoroutineScope, position: Offset, container: Size) {
        containerSize = container
        touchPosition = position
        isPressed = true
        extinguishing = false
        moveLight(scope, position)
        scope.launch { glow.animateTo(1f, SpotLightDefaults.GlowSpring) }
        // onPressDown: the texture fades in while the solid white layer C fades out from 1.
        scope.launch { texture.animateTo(1f, TextureSpring) }
        scope.launch {
            solid.snapTo(1f)
            solid.animateTo(0f, SolidInSpring)
        }
    }

    internal fun move(scope: CoroutineScope, position: Offset) {
        touchPosition = position
        moveLight(scope, position)
    }

    internal fun release(scope: CoroutineScope) {
        isPressed = false
        extinguishing = true
        scope.launch { glow.animateTo(0f, SpotLightDefaults.ExtinguishSpring) }
        // onPressUp: the texture fades out while layer C returns to solid white.
        scope.launch { texture.animateTo(0f, TextureSpring) }
        scope.launch { solid.animateTo(1f, SolidOutSpring) }
    }

    private fun moveLight(scope: CoroutineScope, position: Offset) {
        targetX = position.x
        targetY = position.y
        if (glow.value == 0f && followJob?.isActive != true) {
            // `COUISpotLightEffect.glow` starts the light under the finger.
            lightX = position.x
            lightY = position.y
            lightVelocityX = 0f
            lightVelocityY = 0f
        }
        if (followJob?.isActive == true) return
        // One spring that keeps chasing the latest touch every frame. Restarting an animation per
        // move event would cancel it before its first frame and pin the light while the finger moves.
        followJob = scope.launch {
            var last = withFrameNanos { it }
            while (isPressed || glow.value > 0f) {
                withFrameNanos { now ->
                    val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, MAX_FOLLOW_STEP_SECONDS)
                    last = now
                    val x = sliderLightFollowStep(lightX, lightVelocityX, targetX, dt)
                    val y = sliderLightFollowStep(lightY, lightVelocityY, targetY, dt)
                    lightX = x.position
                    lightVelocityX = x.velocity
                    lightY = y.position
                    lightVelocityY = y.velocity
                }
            }
        }
    }

    /** Maps a container position onto a canvas of [canvasSize] centred in the container. */
    internal fun toCanvas(position: Offset, canvasSize: Size): Offset = Offset(
        position.x - (containerSize.width - canvasSize.width) / 2f,
        position.y - (containerSize.height - canvasSize.height) / 2f,
    )

    /**
     * Paints the spotlight clipped to [background], the capsule `SpotLightEffectRenderer` clips to
     * (`mBackgroundRect` with `getBackgroundDrawRadius`). The preset's radii and light height are raw
     * pixels, as in `COUISpotLightEffectDrawable`.
     */
    internal fun drawLight(scope: DrawScope, shader: RuntimeShader?, params: SpotLightParams, background: Rect) {
        val progress = glow.value
        if (shader == null || progress <= 0f || background.isEmpty) return
        with(scope) {
            val light = toCanvas(Offset(lightX, lightY), size)
            shader.setSpotLightUniforms(
                width = size.width,
                height = size.height,
                lightPx = light,
                radiusPx = couiSpotlightRadius(
                    start = params.startRadius,
                    end = params.endRadius,
                    extinguishExtra = SpotLightDefaults.ExtinguishExtraRadius,
                    progress = progress,
                    extinguishing = extinguishing,
                ),
                intensity = params.startIntensity + (params.endIntensity - params.startIntensity) * progress,
                lightColor = Color.White,
                lightHeightPx = params.lightHeight,
                ambientStrength = params.ambientStrength,
                diffuseStrength = params.diffuseStrength,
                noise = params.noise,
                dither = params.ditherN,
            )
            val corner = minOf(background.width, background.height) / 2f
            val clip = Path().apply { addRoundRect(RoundRect(background, CornerRadius(corner))) }
            clipPath(clip) {
                drawRect(
                    brush = shader.asBrush(),
                    topLeft = background.topLeft,
                    size = background.size,
                    blendMode = params.blendMode,
                )
            }
        }
    }

    /**
     * Paints the thumb the way `TexturedThumbRenderer.drawTexturedThumb` does and returns `true`, or
     * returns `false` once the texture has faded out so the caller draws the standard thumb.
     *
     * Layers A and B share one `saveLayerAlpha` at the texture alpha: A is a white fill whose alpha
     * runs 140 at the top to 230 from 80% down, B is a `#77FFFFFF` fill plus the inner glow of a 10px
     * white stroke shadowed with radius 5 and clipped to the circle. Layer C, a solid white disc,
     * covers the switch between the two looks.
     */
    internal fun drawTexturedThumb(scope: DrawScope, center: Offset, radius: Float): Boolean {
        val textureAlpha = texture.value.coerceIn(0f, 1f)
        if (textureAlpha <= 0f || radius <= 0f) return false
        with(scope) {
            val bounds = Rect(center, radius)
            drawContext.canvas.saveLayer(bounds, Paint().apply { alpha = textureAlpha })
            drawCircle(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = LAYER_A_TOP),
                    0.8f to Color.White.copy(alpha = LAYER_A_BOTTOM),
                    1f to Color.White.copy(alpha = LAYER_A_BOTTOM),
                    startY = bounds.top,
                    endY = bounds.bottom,
                ),
                radius = radius,
                center = center,
            )
            drawCircle(color = LayerBFill, radius = radius, center = center)
            drawCircle(brush = innerGlowBrush(center, radius), radius = radius, center = center)
            drawContext.canvas.restore()
            val solidAlpha = solid.value.coerceIn(0f, 1f)
            if (solidAlpha > 0f) {
                drawCircle(color = Color.White.copy(alpha = solidAlpha), radius = radius, center = center)
            }
        }
        return true
    }
}

/**
 * Feeds [light] from the raw touch stream on the initial pass, without consuming anything, so the
 * slider's own drag handling still sees every event.
 */
internal fun Modifier.sliderPressLight(light: SliderPressLight, enabled: Boolean): Modifier {
    if (!enabled) return this
    return pointerInput(light) {
        coroutineScope {
            val scope = this
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                light.press(scope, down.position, Size(size.width.toFloat(), size.height.toFloat()))
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (change.changedToUpIgnoreConsumed() || !change.pressed) break
                    light.move(scope, change.position)
                }
                light.release(scope)
            }
        }
    }
}

/** One axis of the light after a [sliderLightFollowStep]. */
internal class SliderLightFollow(val position: Float, val velocity: Float)

/**
 * Advances the light towards [target] by [dt] seconds on `COUISpotLightEffect`'s move spring
 * (`LIGHT_MOVE_RESPONSE` 0.35, `LIGHT_MOVE_BOUNCE` 0.15, i.e. damping ratio 0.85), in small
 * semi-implicit Euler substeps so a long frame stays stable.
 */
internal fun sliderLightFollowStep(position: Float, velocity: Float, target: Float, dt: Float): SliderLightFollow {
    var x = position
    var v = velocity
    var remaining = dt
    while (remaining > 0f) {
        val h = minOf(remaining, FOLLOW_SUBSTEP_SECONDS)
        v += (-MOVE_STIFFNESS * (x - target) - MOVE_DAMPING * v) * h
        x += v * h
        remaining -= h
    }
    return SliderLightFollow(x, v)
}

/**
 * The alpha the inner glow of layer B has at [inset] pixels inside the thumb edge: a 10px band
 * centred on the edge, blurred with Android's `setShadowLayer(5f, …)` sigma `5 * 0.57735 + 0.5`.
 */
internal fun sliderThumbInnerGlowAlpha(inset: Float): Float {
    val sigma = INNER_GLOW_SHADOW_RADIUS * 0.57735f + 0.5f
    val half = INNER_GLOW_STROKE_WIDTH / 2f
    return (gaussianCdf((half - inset) / sigma) - gaussianCdf((-half - inset) / sigma)).coerceIn(0f, 1f)
}

private fun innerGlowBrush(center: Offset, radius: Float): Brush {
    val reach = minOf(radius, INNER_GLOW_REACH)
    val stops = Array(INNER_GLOW_STOPS) { i ->
        val inset = reach * (INNER_GLOW_STOPS - 1 - i) / (INNER_GLOW_STOPS - 1)
        val stop = ((radius - inset) / radius).coerceIn(0f, 1f)
        stop to Color.White.copy(alpha = sliderThumbInnerGlowAlpha(inset))
    }
    return Brush.radialGradient(
        colorStops = arrayOf(0f to Color.Transparent) + stops,
        center = center,
        radius = radius,
    )
}

/** Standard normal CDF via the Abramowitz–Stegun 7.1.26 erf approximation. */
private fun gaussianCdf(x: Float): Float {
    val z = abs(x) / 1.4142135f
    val t = 1f / (1f + 0.3275911f * z)
    val poly = t * (0.2548296f + t * (-0.28449672f + t * (1.4214138f + t * (-1.4531521f + t * 1.0614054f))))
    val erf = 1f - poly * exp(-z * z)
    return if (x >= 0f) 0.5f * (1f + erf) else 0.5f * (1f - erf)
}

private const val MOVE_RESPONSE = 0.35f
private const val MOVE_DAMPING_RATIO = 0.85f
private val MOVE_STIFFNESS = (2f * PI.toFloat() / MOVE_RESPONSE).let { it * it }
private val MOVE_DAMPING = 2f * MOVE_DAMPING_RATIO * sqrt(MOVE_STIFFNESS)
private const val FOLLOW_SUBSTEP_SECONDS = 0.004f
private const val MAX_FOLLOW_STEP_SECONDS = 0.05f
private const val LAYER_A_TOP = 140f / 255f
private const val LAYER_A_BOTTOM = 230f / 255f
private val LayerBFill = Color(0x77FFFFFF)
private const val INNER_GLOW_STROKE_WIDTH = 10f
private const val INNER_GLOW_SHADOW_RADIUS = 5f
private const val INNER_GLOW_REACH = 16f
private const val INNER_GLOW_STOPS = 9

// TexturedThumbRenderer: TEXTURE_SPRING_RESPONSE 0.3; LAYER_C_SPRING_RESPONSE_IN 0.5 / _OUT 0.3; bounce 0.
private val TextureSpring = folmeSpring<Float>(damping = 1f, response = 0.3f)
private val SolidInSpring = folmeSpring<Float>(damping = 1f, response = 0.5f)
private val SolidOutSpring = folmeSpring<Float>(damping = 1f, response = 0.3f)
