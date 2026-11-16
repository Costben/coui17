// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.effect

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import io.github.suqi8.coui.kmp.kit.backdrop.CouiBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.CouiBackdropEffectScope
import io.github.suqi8.coui.kmp.kit.backdrop.DrawCouiBackdropNode
import io.github.suqi8.coui.kmp.shader.RuntimeShader
import kotlin.math.ceil
import androidx.compose.ui.graphics.CompositingStrategy as ComposeCompositingStrategy

private const val BlurOffsetPx = 2f
private const val SigmaScale = 0.57735f

/**
 * Draws only the blurred backdrop with a vertical gradient kernel radius.
 * Lengths are in pixels; each node owns independent horizontal/vertical layers and creates shaders on first draw.
 */
@Composable
internal fun Modifier.drawCouiGradientBackdrop(
    backdrop: CouiBackdrop,
    fadeHeightPx: Float,
    referenceBlurRadiusPx: Float,
    gradientSpeed: Float? = null,
): Modifier {
    require(gradientSpeed == null || (gradientSpeed.isFinite() && gradientSpeed > 0f))
    if (!isKitRuntimeShaderSupported()) return this
    backdrop.RetainWhileComposed()
    return graphicsLayer {
        shape = RectangleShape
        clip = true
        compositingStrategy = ComposeCompositingStrategy.Offscreen
    }.then(GradientBackdropElement(backdrop, fadeHeightPx, referenceBlurRadiusPx, gradientSpeed))
}

private data class GradientBackdropElement(
    val backdrop: CouiBackdrop,
    val fadeHeightPx: Float,
    val referenceBlurRadiusPx: Float,
    val gradientSpeed: Float?,
) : ModifierNodeElement<GradientBackdropNode>() {
    override fun create() = GradientBackdropNode(backdrop, fadeHeightPx, referenceBlurRadiusPx, gradientSpeed)
    override fun update(node: GradientBackdropNode) {
        node.backdrop = backdrop
        node.update(fadeHeightPx, referenceBlurRadiusPx, gradientSpeed)
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "drawCouiGradientBackdrop"
    }
}

private class GradientBackdropNode(
    backdrop: CouiBackdrop,
    private var fadeHeightPx: Float,
    private var referenceBlurRadiusPx: Float,
    private var gradientSpeed: Float?,
) : DrawCouiBackdropNode(backdrop) {
    private var passes = emptyList<GraphicsLayer>()
    private var renderer: CouiGradientRenderer? = null
    private var rendererDensity: Density? = null

    override fun onAttach() {
        super.onAttach()
        passes = List(2) { requireGraphicsContext().createGraphicsLayer() }
    }

    override fun onDetach() {
        passes.forEach { requireGraphicsContext().releaseGraphicsLayer(it) }
        passes = emptyList()
        renderer = null
        rendererDensity = null
        super.onDetach()
    }

    fun update(fadeHeightPx: Float, referenceBlurRadiusPx: Float, gradientSpeed: Float?) {
        if (this.fadeHeightPx != fadeHeightPx || this.referenceBlurRadiusPx != referenceBlurRadiusPx ||
            this.gradientSpeed != gradientSpeed
        ) {
            this.fadeHeightPx = fadeHeightPx
            this.referenceBlurRadiusPx = referenceBlurRadiusPx
            this.gradientSpeed = gradientSpeed
            renderer = null
        }
    }

    override fun configureEffects(scope: CouiBackdropEffectScope) {
        val density = scope.recordingDensity
        val current = renderer?.takeIf { rendererDensity == density }
            ?: CouiGradientRenderer(fadeHeightPx, referenceBlurRadiusPx, gradientSpeed, density, passes).also {
                renderer = it
                rendererDensity = density
            }
        scope.padding = current.padding
        current.height = scope.size.height
    }

    override fun drawSource(scope: DrawScope, source: DrawScope.() -> Unit) {
        checkNotNull(renderer).draw(scope, source)
    }
}

/**
 * Separable progressive Gaussian blur (horizontal pass followed by vertical pass).
 * Each pass keeps a full-resolution intermediate layer without downsampled pyramid blending.
 */
private class CouiGradientRenderer(
    private val fadeHeightPx: Float,
    referenceBlurRadiusPx: Float,
    private val gradientSpeed: Float?,
    private val density: Density,
    private val passes: List<GraphicsLayer>,
) {
    // Convert host reference radius to the COUI kernel: sigma = radius / 3, sample spacing = 2px.
    private val kernelRadius = ceil(
        (referenceBlurRadiusPx * SigmaScale + 0.5f) * 3f / BlurOffsetPx,
    ).toInt()
    private val shaders = List(2) { index ->
        RuntimeShader(gradientGaussianShader(kernelRadius, gradientSpeed)).apply {
            setFloatUniform(
                "in_blurOffset",
                if (index == 0) BlurOffsetPx else 0f,
                if (index == 1) BlurOffsetPx else 0f,
            )
        }
    }
    private var lastSize = IntSize.Zero
    private var lastHeight = Float.NaN
    private var recordedSize = IntSize.Zero
    private var recordedLayoutDirection: LayoutDirection? = null

    var height: Float = 0f

    // Cover maximum sample offset plus bilinear outer margin so edge clamping stays outside the visible area.
    val padding: Float = kernelRadius * BlurOffsetPx + 1f

    init {
        passes.forEach {
            it.compositingStrategy = CompositingStrategy.Offscreen
            it.clip = true
        }
    }

    fun draw(scope: DrawScope, drawBackdrop: DrawScope.() -> Unit) {
        if (scope.size.width <= 0f || scope.size.height <= 0f) return
        val paddedSize = IntSize(scope.size.width.toInt(), scope.size.height.toInt())
        updateEffects(paddedSize)
        // New Canvas does not inherit CouiBackdrop padding translation; explicit density avoids cyclic DrawScope delegation.
        passes[0].record(density, scope.layoutDirection, paddedSize) {
            translate(padding, padding) { drawBackdrop() }
        }
        // Separate layers enforce two rasterizations so the second pass does not nest-evaluate the first pass inside its shader.
        // The second pass only references the first pass; child layer updates still propagate without re-recording on scroll.
        if (recordedSize != paddedSize || recordedLayoutDirection != scope.layoutDirection) {
            val horizontal = passes[0]
            passes[1].record(density, scope.layoutDirection, paddedSize) { drawLayer(horizontal) }
            recordedSize = paddedSize
            recordedLayoutDirection = scope.layoutDirection
        }
        scope.translate(-padding, -padding) { drawLayer(passes[1]) }
    }

    private fun updateEffects(size: IntSize) {
        if (lastSize == size && lastHeight == height) return
        shaders.forEachIndexed { index, shader ->
            shader.setFloatUniform("inputSize", size.width.toFloat(), size.height.toFloat())
            val gradientHeight = if (gradientSpeed == null) fadeHeightPx else height
            shader.setFloatUniform("gradientTop", padding + height - gradientHeight)
            shader.setFloatUniform("gradientHeight", gradientHeight)
            // RenderEffect caches a uniform snapshot; update only on geometry changes and reuse during scroll.
            passes[index].renderEffect = shader.asComposeRenderEffect("child")
        }
        lastSize = size
        lastHeight = height
    }
}

// libhwui.so: OplusGradientGaussianBlurEffect::make_blur_effect, preserving integer radius, weights, and remaining center weight.
// shortcut: fixed noise seed without per-frame temporal jitter; hook up the render clock if dynamic noise is needed later.
private fun gradientGaussianShader(kernelRadius: Int, gradientSpeed: Float?): String = """
    const int in_blurRadius = $kernelRadius;
    const float PI = 3.1415926;
    uniform shader child;
    uniform float2 inputSize;
    uniform float gradientTop;
    uniform float gradientHeight;
    uniform half2 in_blurOffset;

    float4 sampleChild(float2 coord) {
        return child.eval(clamp(coord, float2(0.5), inputSize - 0.5));
    }

    float getWeight(int i, int radius) {
        float sigma = float(radius) / 3.0;
        float value = (1.0 / sqrt(2.0 * PI * sigma * sigma))
            * exp(float(-i * i) / (2.0 * sigma * sigma + 0.00000001));
        return clamp(value, 0.0, 1.0);
    }

    float interleavedGradientNoise(float2 coord) {
        const float3 magicNumbers = float3(0.06711056, 0.00583715, 52.9829189);
        return fract(magicNumbers.z * fract(dot(coord, magicNumbers.xy)));
    }

    half4 main(float2 coord) {
        float4 c = sampleChild(coord);
        float position = clamp((coord.y - gradientTop) / gradientHeight, 0.0, 1.0);
        // Keep smoothstep for the default path; use 1 - pow(position, speed) when an explicit speed is provided.
        float ratio = ${if (gradientSpeed == null) "smoothstep(0.0, 1.0, 1.0 - position)" else "1.0 - pow(position, $gradientSpeed)"};
        int radius = int(float(in_blurRadius) * ratio);
        if (radius <= 1) return half4(c);
        // Convolution result becomes zero after restoring center alpha; pass sharp edge through without truncating low non-zero coverage.
        if (c.a == 0.0) return half4(0.0);
        float2 offset = float2(0.0);
        float sumWeight = 0.0;
        // Accumulate color and normalize coverage in float to prevent half-precision rounding from causing horizontal banding.
        float4 finalColor = float4(0.0);
        for (int i = 1; i <= in_blurRadius; i++) {
            if (i > radius) break;
            float weight = getWeight(i, radius);
            offset.x = min(in_blurOffset.x, float(radius)) * float(i);
            offset.y = min(in_blurOffset.y, float(radius)) * float(i);
            finalColor += sampleChild(coord - offset) * weight;
            finalColor += sampleChild(coord + offset) * weight;
            sumWeight += 2.0 * weight;
        }
        finalColor += c * (1.0 - sumWeight);
        // No off-screen page samples exist; restore valid color without expanding center coverage so top edge stays clean.
        if (finalColor.a <= 0.0) return half4(0.0);
        finalColor = float4(finalColor.rgb * (c.a / finalColor.a), c.a);
        float noise = interleavedGradientNoise(coord + 0.5) - 0.5;
        finalColor.rgb = clamp(finalColor.rgb + noise * c.a / 200.0, float3(0.0), float3(c.a));
        return half4(finalColor);
    }
""".trimIndent()
