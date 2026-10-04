// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.shader.RuntimeShader
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported

/** A snapshot of the sampled surface, preserving its alpha like libhwui's blendEdge. */
internal expect fun RuntimeShader.nativeMaterialEdgeEffect(): RenderEffect

/**
 * Samples the actual painted material before applying the framework edge. In the glass path,
 * [tint] is applied here so the native edge still sees the untinted backdrop as rawColor.
 * Content/icons and exterior caustics must be siblings, outside this surface-only effect.
 */
@Composable
internal fun Modifier.nativeMaterialEdge(
    particles: List<MetaBallParticle>,
    rim: Color,
    bottomScale: Float = 1f,
    tint: Color = Color.Transparent,
    edgeStyle: GradientStrokeStyle = GradientStrokeStyle.EDGE_STYLE_0,
    lineWidth: Dp = 2.5.dp,
    extension: Dp = MetaBallDefaults.Extension,
    factorX: Float = MetaBallDefaults.EXTENSION_FACTOR_X,
    factorY: Float = MetaBallDefaults.EXTENSION_FACTOR_Y,
): Modifier {
    if (!isRuntimeShaderSupported()) return this
    val shader = remember { RuntimeShader(META_BALL_NATIVE_EDGE_SHADER) }
    return drawWithCache {
        val layer = obtainGraphicsLayer()
        setMetaBallUniforms(shader, particles, Color.Transparent, rim, extension, factorX, factorY, MetaBallDefaults.RimBlur, bottomScale, edgeStyle, lineWidth)
        shader.setFloatUniform("in_tint", tint.red, tint.green, tint.blue, tint.alpha)
        layer.renderEffect = shader.nativeMaterialEdgeEffect()
        onDrawWithContent {
            layer.record { this@onDrawWithContent.drawContent() }
            drawLayer(layer)
        }
    }
}

/** Exact RGB body of OplusMaterialEffectShader::blendEdge (fragments 0xE9157 + 0xE2306). */
internal const val NATIVE_MATERIAL_EDGE_BLEND = """
half3 couiNativeEdgeBlend(half3 materialColor, half3 rawColor, half3 lineColor, float lineAlpha) {
    float gray = clamp(dot(float3(0.2126, 0.7152, 0.0722), float3(rawColor)) * 1.2, 0.0, 1.0);
    float edgeImprove = (1.0 - gray) * (1.0 - gray);
    edgeImprove *= edgeImprove;
    edgeImprove *= 0.1;
    half3 improved = clamp(rawColor + half3(edgeImprove), 0.0, 1.0);
    half3 edgeColor = mix(2.0 * improved * lineColor,
        1.0 - 2.0 * (1.0 - improved) * (1.0 - lineColor), step(lineColor, half3(0.5)));
    return mix(materialColor, edgeColor, half(lineAlpha));
}
"""
