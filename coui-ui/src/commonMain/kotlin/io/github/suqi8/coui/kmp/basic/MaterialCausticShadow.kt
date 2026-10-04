// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.shader.RuntimeShader
import io.github.suqi8.coui.kmp.shader.asBrush
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.roundToInt

/** Parameters consumed by OplusCausticShadowEffect, in native setter order. */
@Immutable
internal data class CausticShadowProfile(
    val bottomOffset: Dp,
    val sdfWidth: Dp,
    val roundPixels: Boolean = false,
) {
    fun pixels(value: Float): Float = if (roundPixels) value.roundToInt().toFloat() else value.toInt().toFloat()

    companion object {
        // ToolbarMaterialEffectDelegate uses getDimensionPixelSize; bottom bars use PixelOffset.
        val Toolbar = CausticShadowProfile(4.899994.dp, 9.22998.dp, roundPixels = true)
        val Bar = CausticShadowProfile(4.899994.dp, 10.399994.dp)
        val Accessory = CausticShadowProfile(11.199982.dp, 20.dp)
    }
}

@Immutable
internal data class CausticShadowParticle(
    val shape: MetaBallParticle,
    val profile: CausticShadowProfile,
)

internal interface CausticShadowRenderer {
    @Composable
    fun Draw(particles: List<CausticShadowParticle>, color: Color, modifier: Modifier, fallback: @Composable () -> Unit)
}

@Composable
internal expect fun rememberPlatformCausticShadowRenderer(): CausticShadowRenderer?

/** Draws the system caustic beneath the surface; no angular mask or outside-only step. */
@Composable
internal fun MaterialCausticShadow(
    particles: List<CausticShadowParticle>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val renderer = rememberPlatformCausticShadowRenderer()
    // AndroidViewHolder owns a real rectangular layer. Give it the full shadow extent as well
    // as the shape bounds, otherwise its interop parent can truncate the native lower light.
    val outset = 32.dp
    val outsetPx = with(LocalDensity.current) { outset.roundToPx() }
    val shifted = particles.map { it.copy(shape = it.shape.copy(centerX = it.shape.centerX + outset, centerY = it.shape.centerY + outset)) }
    Layout(modifier = modifier, content = {
        Box {
            // The shader and the platform view use the same expanded local coordinate system.
            CausticShadowPaint(shifted, color, renderer)
        }
    }) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val paint = measurables.single().measure(Constraints.fixed(width + 2 * outsetPx, height + 2 * outsetPx))
        layout(width, height) { paint.place(-outsetPx, -outsetPx) }
    }
}

@Composable
private fun CausticShadowPaint(particles: List<CausticShadowParticle>, color: Color, renderer: CausticShadowRenderer?) {
    val modifier = Modifier.fillMaxSize()
    val background = COUITheme.colorScheme.background
    val shader = remember {
        if (isRuntimeShaderSupported()) RuntimeShader(CAUSTIC_SHADOW_SHADER) else null
    }
    val fallback: @Composable () -> Unit = {
        Canvas(modifier) {
            if (shader != null) {
                particles.forEach { (particle, profile) ->
                    val half = Size(particle.width.toPx() / 2f, particle.height.toPx() / 2f)
                    if (half.minDimension > 0f && particle.alpha > 0f) {
                        val center = Offset(particle.centerX.toPx(), particle.centerY.toPx())
                        val bottom = profile.pixels(profile.bottomOffset.toPx())
                        shader.setFloatUniform("in_half", half.width, half.height)
                        // Native Device::drawCausticShadow takes the magnitude of the RRect radius pair.
                        shader.setFloatUniform("in_corner", particle.cornerRadius.toPx() * 1.41421356f)
                        shader.setFloatUniform("in_bottom", bottom)
                        shader.setFloatUniform("in_sdf", profile.pixels(profile.sdfWidth.toPx()))
                        shader.setFloatUniform("in_color", color.red, color.green, color.blue, color.alpha * particle.alpha)
                        shader.setFloatUniform("in_background", background.red, background.green, background.blue)
                        withTransform({
                            translate(center.x, center.y)
                            scale(particle.scaleX, particle.scaleY, pivot = Offset.Zero)
                            translate(-half.width, -half.height)
                        }) {
                            // Layout offsets define the native drawRect. The SDF reaches zero at its bounds.
                            drawRect(shader.asBrush(), size = Size(half.width * 2f, half.height * 2f + bottom), blendMode = BlendMode.Plus)
                        }
                    }
                }
            }
        }
    }
    if (renderer == null) fallback() else renderer.Draw(particles, color, modifier, fallback)
}

/**
 * Port of libhwui OplusCausticShadowEffectImpl::emitCode. The only adaptations are local
 * coordinates, outlineType=0 (the native constructor's only live branch), and premultiplied
 * light output for additive blending. ColorOS evaluates luminance against the real destination;
 * the standalone renderer uses the theme background when no destination sampler is available.
 */
private const val CAUSTIC_SHADOW_SHADER = """
uniform float2 in_half;
uniform float in_corner;
uniform float in_bottom;
uniform float in_sdf;
uniform float4 in_color;
uniform float3 in_background;
float RBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - r;
}
half4 main(float2 pos) {
    float2 pos_optics = pos - in_half - float2(0.0, in_bottom * 0.5);
    float2 halfSizeOptics = in_half + float2(0.0, in_bottom * 0.5);
    float sdf = min(in_sdf, min(in_half.x, in_half.y));
    if (in_color.a <= 0.001 || sdf <= 0.0 || any(greaterThan(abs(pos_optics), halfSizeOptics))) {
        return half4(0.0);
    }
    halfSizeOptics = max(halfSizeOptics - sdf, float2(0.0));
    float cornerOptics = min(in_corner, min(halfSizeOptics.x, halfSizeOptics.y));
    float opticsSdf = RBox(pos_optics, halfSizeOptics, cornerOptics);
    float value = smoothstep(sdf, 0.0, opticsSdf);
    value *= pow(smoothstep(halfSizeOptics.y + sdf, -halfSizeOptics.y - sdf, -pos_optics.y), 18.0);
    value = pow(value, 0.6) * 0.6 + 0.4 * value;
    float gray = clamp((dot(float3(0.2126, 0.7152, 0.0722), in_background) - 0.45) * 0.6, 0.0, 1.0);
    float alpha = in_color.a * value * (1.0 - gray) * (1.0 - gray);
    return half4(in_color.rgb * alpha, alpha);
}
"""
