// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.effect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import io.github.suqi8.coui.kmp.kit.input.isCouiTouchCancelled
import io.github.suqi8.coui.kmp.kit.motion.CouiSpring
import io.github.suqi8.coui.kmp.shader.RuntimeShader
import io.github.suqi8.coui.kmp.shader.asBrush

/** Vendor press-following spotlight presets; radii and light height are in device pixels without density scaling. */
internal enum class SpotlightPreset(
    internal val lightIntensity: Float,
    internal val darkIntensity: Float,
    internal val endRadius: Float,
    internal val darkBlendMode: BlendMode = BlendMode.Overlay,
    internal val lightBlendMode: BlendMode = BlendMode.Overlay,
    internal val ambient: Float = 0.4f,
    internal val diffuse: Float = 0.4f,
    internal val noise: Float = 0.08f,
) {
    TranslucentLarge1(0.6f, 0.1f, 800f),
    OpaqueSmall1(0.6f, 0.5f, 500f),
    OpaqueMedium1(0.6f, 0.8f, 500f),
    OpaqueSmall2(1f, 0.15f, 500f, BlendMode.Plus),
    OpaqueMedium2(1f, 0.15f, 500f, BlendMode.Plus),
    ChipTab(0.2f, 0.3f, 600f, BlendMode.SrcOver, BlendMode.Plus, 0.1f, 0.5f, 0f),
}

private data class SpotlightStyle(
    val intensity: Float,
    val ambient: Float,
    val diffuse: Float,
    val lightHeight: Float,
    val noise: Float,
    val dither: Float,
    val blendMode: BlendMode = BlendMode.Overlay,
    val startRadius: Float = 100f,
    val endRadius: Float = 800f,
    val color: Color = Color.White,
)

internal sealed interface SpotlightDiagnostic {
    data object Pressed : SpotlightDiagnostic
    data class Released(val progress: Float) : SpotlightDiagnostic
    data class FirstDraw(val hardware: Boolean, val size: IntSize, val progress: Float) : SpotlightDiagnostic
}

/**
 * Observes touch input without intercepting clicks, drags, or scrolls. The draw callback should
 * be placed in a transparent foreground layer and becomes null once extinguished.
 * A preset and light/dark flag are required; optional parameters inherit from the preset when null.
 * [inputTarget] receives the local touch point and whether a session is active; returning null cancels the session.
 * [cancelOnConsumedMove] is for scrollable children: cancels the current session once a parent
 * consumes a move, and stays off until all pointers lift.
 */
@Composable
internal fun rememberCouiSpotlight(
    interactive: Boolean,
    preset: SpotlightPreset,
    dark: Boolean,
    intensity: Float? = null,
    ambient: Float? = null,
    diffuse: Float? = null,
    lightHeight: Float? = null,
    noise: Float? = null,
    dither: Float? = null,
    blendMode: BlendMode? = null,
    startRadius: Float? = null,
    endRadius: Float? = null,
    color: Color? = null,
    inputTarget: ((Offset, Boolean) -> LayoutCoordinates?)? = null,
    cancelOnConsumedMove: Boolean = false,
    onDiagnostic: (SpotlightDiagnostic) -> Unit = {},
): Pair<Modifier, (DrawScope.() -> Unit)?> = if (isKitRuntimeShaderSupported()) {
    val style = SpotlightStyle(
        intensity = intensity ?: if (dark) preset.darkIntensity else preset.lightIntensity,
        ambient = ambient ?: preset.ambient,
        diffuse = diffuse ?: preset.diffuse,
        lightHeight = lightHeight ?: 200f,
        noise = noise ?: preset.noise,
        dither = dither ?: 0f,
        blendMode = blendMode ?: if (dark) preset.darkBlendMode else preset.lightBlendMode,
        startRadius = startRadius ?: 100f,
        endRadius = endRadius ?: preset.endRadius,
        color = color ?: Color.White,
    )
    runtimeSpotlight(interactive, style, inputTarget, cancelOnConsumedMove, onDiagnostic)
} else {
    Modifier to null
}

@Composable
private fun runtimeSpotlight(
    interactive: Boolean,
    style: SpotlightStyle,
    inputTarget: ((Offset, Boolean) -> LayoutCoordinates?)?,
    cancelOnConsumedMove: Boolean,
    onDiagnostic: (SpotlightDiagnostic) -> Unit,
): Pair<Modifier, (DrawScope.() -> Unit)?> {
    val diagnostic = rememberUpdatedState(onDiagnostic)
    val currentInputTarget by rememberUpdatedState(inputTarget)
    val light = remember(style.startRadius, style.endRadius) {
        SpotlightState(style.startRadius, style.endRadius) { diagnostic.value(it) }
    }
    DisposableEffect(light) {
        onDispose { light.dispose() }
    }
    val visible by remember(light) {
        derivedStateOf(structuralEqualityPolicy()) { light.progress > 0f }
    }
    val shader = remember { RuntimeShader(SPOTLIGHT_SHADER) }
    var inputCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val touchModifier = Modifier
        .onGloballyPositioned { inputCoordinates = it }
        .onSizeChanged { if (inputTarget == null) light.setSize(it) }
        .pointerInput(light, interactive, inputTarget == null, cancelOnConsumedMove) {
            if (!interactive) return@pointerInput
            try {
                awaitPointerEventScope {
                    var activePointer: PointerId? = null
                    var cancelled = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.isCouiTouchCancelled()) {
                            activePointer = null
                            light.release()
                            cancelled = false
                            continue
                        }
                        if (cancelled) {
                            if (event.changes.none { it.pressed }) cancelled = false
                            continue
                        }
                        val wasIdle = activePointer == null
                        val newPointer = event.changes.firstOrNull { it.pressed && !it.previousPressed }
                        val tracked = event.changes.firstOrNull { it.id == activePointer }
                        // Matches drawable behavior: a newly pressed finger takes over, releasing falls back to the first held pointer.
                        val change = if (newPointer != null) {
                            newPointer
                        } else if (tracked != null && !tracked.pressed) {
                            event.changes.firstOrNull { it.pressed } ?: tracked
                        } else {
                            tracked ?: if (currentInputTarget != null) event.changes.firstOrNull { it.pressed } else null
                        } ?: continue
                        val target = currentInputTarget?.invoke(change.position, !wasIdle)
                        if (currentInputTarget != null && target?.isAttached != true) {
                            light.release()
                            activePointer = null
                            continue
                        }
                        val bounds = target?.size ?: size
                        if (bounds.width <= 0 || bounds.height <= 0) continue
                        val localPosition = if (target != null) {
                            val input = inputCoordinates?.takeIf { it.isAttached } ?: continue
                            target.localPositionOf(input, change.position)
                        } else {
                            change.position
                        }
                        light.setSize(bounds)
                        val position = Offset(
                            localPosition.x / bounds.width,
                            localPosition.y / bounds.height,
                        )
                        light.move(position)
                        activePointer = change.id
                        if (wasIdle) light.press()
                        if (!change.pressed) {
                            activePointer = null
                            light.release()
                        }
                        if (cancelOnConsumedMove) {
                            val finalEvent = awaitPointerEvent(PointerEventPass.Final)
                            if (activePointer != null && finalEvent.changes.any {
                                    it.isConsumed && it.pressed && it.position != it.previousPosition
                                }
                            ) {
                                activePointer = null
                                light.release()
                                cancelled = true
                            }
                        }
                    }
                }
            } finally {
                light.release()
            }
        }
    val drawSpotlight: DrawScope.() -> Unit = {
        val progress = light.progress
        val hardware = drawContext.canvas.isSpotlightHardwareAccelerated()
        if (progress > 0f && size.width > 0f && size.height > 0f) {
            light.reportDraw(hardware, IntSize(size.width.toInt(), size.height.toInt()), progress)
        }
        if (progress > 0f && size.width > 0f && size.height > 0f && hardware) {
            val position = light.position
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform(
                "u_lightPosAndHeight",
                position.x * size.width,
                position.y * size.height,
                light.radius,
                style.intensity * progress,
            )
            shader.setFloatUniform("u_lightColorAndAmbient", style.color.red, style.color.green, style.color.blue, style.ambient)
            shader.setFloatUniform("u_lightingParams", style.diffuse, style.lightHeight, style.noise, style.dither)
            drawRect(brush = shader.asBrush(), size = size, blendMode = style.blendMode)
        }
    }
    return touchModifier to if (visible) drawSpotlight else null
}

/** Returns true when this [Canvas] supports runtime shader drawing (hardware-accelerated on Android, always true on Skiko). */
internal expect fun Canvas.isSpotlightHardwareAccelerated(): Boolean

private class SpotlightState(
    private val startRadius: Float,
    private val endRadius: Float,
    private val onDiagnostic: (SpotlightDiagnostic) -> Unit,
) {
    private var reportNextDraw = false
    private var disposed = false
    private var extinguishing = false
    private var bounds = IntSize.Zero
    private var lastTargetPosition = Offset.Zero

    // Matches COUI's legacy AndroidX spring path: continuously retarget instead of restarting on every MOVE.
    private val positionXSpring = CouiSpring(0f, minimumChange = 0.0001f)
    private val positionYSpring = CouiSpring(0f, minimumChange = 0.0001f)

    // Keep vendor 0..10000 raw progress and settle threshold, normalizing only when drawing.
    private val progressSpring = CouiSpring(0f, minimumChange = 1f)

    var progress by mutableFloatStateOf(0f)
        private set
    val position: Offset
        get() = Offset(positionXSpring.value, positionYSpring.value)
    var radius by mutableFloatStateOf(0f)
        private set

    fun setSize(size: IntSize) {
        bounds = size
    }

    fun move(target: Offset) {
        if (disposed) return
        lastTargetPosition = target
        if (progressSpring.value == 0f) {
            positionXSpring.snapTo(target.x)
            positionYSpring.snapTo(target.y)
        } else {
            positionXSpring.animateTo(target.x, response = 0.35f, bounce = 0.15f)
            positionYSpring.animateTo(target.y, response = 0.35f, bounce = 0.15f)
        }
    }

    fun press() {
        if (disposed) return
        extinguishing = false
        reportNextDraw = true
        onDiagnostic(SpotlightDiagnostic.Pressed)
        progressSpring.animateTo(
            target = 10000f,
            response = 0.15f,
            bounce = 0f,
            onFrame = ::updateProgress,
        )
    }

    fun release() {
        if (disposed || extinguishing) return
        extinguishing = true
        onDiagnostic(SpotlightDiagnostic.Released(progress))
        progressSpring.animateTo(
            target = 0f,
            response = 0.4f,
            bounce = 0f,
            onFrame = ::updateProgress,
        )
    }

    fun dispose() {
        // Prevent pointerInput's finally block from restarting frame callbacks after node removal.
        disposed = true
        progressSpring.cancel()
        positionXSpring.cancel()
        positionYSpring.cancel()
    }

    fun reportDraw(hardware: Boolean, size: IntSize, progress: Float) {
        if (!reportNextDraw) return
        reportNextDraw = false
        onDiagnostic(SpotlightDiagnostic.FirstDraw(hardware, size, progress))
    }

    private fun updateProgress(value: Float) {
        progress = (value / 10000f).coerceIn(0f, 1f)
        if (value == 0f) {
            radius = 0f
            extinguishing = false
            positionXSpring.snapTo(lastTargetPosition.x)
            positionYSpring.snapTo(lastTargetPosition.y)
        } else {
            radius = startRadius + (endRadius - startRadius) * progress +
                if (extinguishing && value > 0f) 2f * endRadius * (1f - progress) else 0f
            if (spotLightOutOfBounds()) {
                if (extinguishing) {
                    progressSpring.snapTo(0f)
                    progress = 0f
                    radius = 0f
                    extinguishing = false
                    positionXSpring.snapTo(lastTargetPosition.x)
                    positionYSpring.snapTo(lastTargetPosition.y)
                } else {
                    progressSpring.snapTo(10000f)
                    progress = 1f
                    radius = endRadius
                }
            }
        }
    }

    private fun spotLightOutOfBounds(): Boolean {
        val x = position.x * bounds.width
        val y = position.y * bounds.height
        return x - radius > bounds.width || x + radius < 0f ||
            y - radius > bounds.height || y + radius < 0f
    }
}

// COUI SpotLight.AGSL_SOURCE adapted for cross-platform AGSL/SkSL compilation, preserving vendor lighting and noise math.
private const val SPOTLIGHT_SHADER = """
uniform float4 u_lightPosAndHeight;
uniform float4 u_lightColorAndAmbient;
uniform float4 u_lightingParams;
uniform float2 resolution;

float luminance(float3 c) {
    return dot(c, float3(0.299, 0.587, 0.114));
}

float max3(float3 v) {
    return max(max(v.r, v.g), v.b);
}

float noise(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
}

float softLight(float A, float B) {
    float l = B - (1.0 - 2.0 * A) * B * (1.0 - B);
    float h = B + (2.0 * A - 1.0) * (sqrt(B) - B);
    return mix(l, h, step(0.5, A));
}

float dither(float2 coord, float ditherN) {
    float n1 = fract(sin(dot(coord, float2(12.9898, 78.233))) * 43758.5453);
    float n2 = fract(sin(dot(coord + float2(1.0), float2(12.9898, 78.233))) * 43758.5453);
    return (n1 + n2 - 1.0) * ditherN / 255.0;
}

half4 main(float2 fragCoord) {
    float2 lightP = u_lightPosAndHeight.xy;
    float radius = u_lightPosAndHeight.z;
    float intensity = u_lightPosAndHeight.w;

    float3 lightCol = u_lightColorAndAmbient.rgb;
    float ambientStr = u_lightColorAndAmbient.w;
    float diffuseStr = u_lightingParams.x;
    float lightH = u_lightingParams.y;
    float noise_exp = u_lightingParams.z;
    float ditherN = u_lightingParams.w;
    const float lightIntensity = 1.5;

    float2 delta = fragCoord - lightP;
    float3 L = float3(delta, lightH);
    float dist = length(L);
    L = normalize(L);

    float3 N = float3(0.0, 0.0, 1.0);
    float diff = max(dot(N, L), 0.0);
    float3 lighting = (ambientStr + diffuseStr * diff) * lightCol;
    if (dist >= radius) {
        return half4(0.0, 0.0, 0.0, 0.0);
    }

    float att = smoothstep(1.0, 0.0, dist / radius) * step(dist, radius);
    lighting *= att * lightIntensity * intensity;
    float alpha = clamp(luminance(lighting), 0.0, 1.0);

    float postOn = max(step(0.001, noise_exp), step(0.001, ditherN));
    float3 postLighting = lighting;
    float postAlpha = alpha;

    float noiseOn = step(0.001, noise_exp);
    float2 uv_pos = fragCoord * 9.259e-4;
    float n = clamp((noise(uv_pos) - 0.5) * noise_exp + 0.5, 0.0, 1.0);
    float mask = max3(postLighting);
    float fadeAlpha = clamp(softLight(n, mask), 0.0, 1.0);
    float noiseDelta = clamp(abs(fadeAlpha - mask), 0.0, 1.0);
    postLighting += float3(noiseDelta) * noiseOn;

    float ditherOn = step(0.001, ditherN);
    float d = dither(fragCoord, ditherN);
    postLighting += float3(d) * ditherOn;
    postAlpha = mix(postAlpha, clamp(postAlpha + d, 0.0, 1.0), ditherOn);

    lighting = mix(lighting, postLighting, postOn);
    alpha = mix(alpha, postAlpha, postOn);
    return half4(lighting.x, lighting.y, lighting.z, alpha);
}
"""
