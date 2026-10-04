// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.shader.RuntimeShader
import io.github.suqi8.coui.kmp.shader.asBrush
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported

/**
 * The gradient-stroke style of a [Modifier.materialShadowEdge], the portable port of COUI's
 * `GradientStrokeStyleSpec`.
 *
 * ColorOS draws the rim of a "material" surface with a single runtime shader rather than a solid
 * stroke: [`GradientStrokeStyleMapper.toEdgeArray16`][GradientStrokeStyleMapper] flattens this spec
 * into the shader's 16-float `u_edgeArray`, whose near / far line widths and fade pairs let the rim
 * thin out toward the middle of each edge and brighten toward the corners.
 *
 * The arrays are the raw spec arrays (`rectLineSize`, `rectLineColor`, ...), each read with a
 * per-slot default when it is too short, exactly like `GradientStrokeStyleKt.at`.
 *
 * @param rectLineSize Base size the fade pair is measured against; `{width, height}` in pixels.
 * @param rectLineColor Rim colour, `{r, g, b}` in 0..1.
 * @param rectLineNear Near-edge line thickness pair `{x, y}`.
 * @param rectLineFar Far-edge line thickness pair `{x, y}`.
 * @param rectLineAlpha Near / far line alpha.
 * @param rectLineFade Near / far fade pair, `{nearX, nearY, farX, farY}`.
 * @param angle The stroke angle, `u_edgeArray[13]`.
 * @param rectLinePara1 / rectLinePara2 The `pow` shaping pair that rounds off the line profile.
 */
@Immutable
class GradientStrokeStyle(
    val rectLineSize: FloatArray,
    val rectLineColor: FloatArray,
    val rectLineNear: FloatArray,
    val rectLineFar: FloatArray,
    val rectLineAlpha: FloatArray,
    val rectLineFade: FloatArray,
    val angle: Float,
    val rectLinePara1: Float,
    val rectLinePara2: Float,
) {
    companion object {
        /** `GradientStrokeStylePresets.EDGE_STYLE_LIGHT_0` — the capsule rim. Dark is identical. */
        val EDGE_STYLE_0: GradientStrokeStyle = GradientStrokeStyle(
            rectLineSize = floatArrayOf(400f, 100f),
            rectLineColor = floatArrayOf(1f, 1f, 1f),
            rectLineNear = floatArrayOf(2f, 6f),
            rectLineFar = floatArrayOf(2f, 3f),
            rectLineAlpha = floatArrayOf(0.7f, 1f),
            rectLineFade = floatArrayOf(0.2f, 0.4f, 0.15f, 0.35f),
            angle = 0f,
            rectLinePara1 = 4f,
            rectLinePara2 = 0.8f,
        )

        /** `GradientStrokeStylePresets.EDGE_STYLE_LIGHT_1` — the compact icon-button rim. */
        val EDGE_STYLE_1: GradientStrokeStyle = GradientStrokeStyle(
            rectLineSize = floatArrayOf(160f, 160f),
            rectLineColor = floatArrayOf(1f, 1f, 1f),
            rectLineNear = floatArrayOf(2f, 6f),
            rectLineFar = floatArrayOf(2f, 3f),
            rectLineAlpha = floatArrayOf(0.7f, 1f),
            rectLineFade = floatArrayOf(0.06f, 0.4f, 0.01f, 0.28f),
            angle = 0f,
            rectLinePara1 = 4f,
            rectLinePara2 = 0.8f,
        )

        /** `GradientStrokeStylePresets.EDGE_STYLE_LIGHT_2` — the large panel rim. */
        val EDGE_STYLE_2: GradientStrokeStyle = GradientStrokeStyle(
            rectLineSize = floatArrayOf(600f, 200f),
            rectLineColor = floatArrayOf(1f, 1f, 1f),
            rectLineNear = floatArrayOf(2f, 8f),
            rectLineFar = floatArrayOf(2f, 2f),
            rectLineAlpha = floatArrayOf(0.7f, 0.6f),
            rectLineFade = floatArrayOf(0.18f, 0.35f, 0.25f, 0.25f),
            angle = 0f,
            rectLinePara1 = 4f,
            rectLinePara2 = 0.8f,
        )
    }
}

/**
 * The inner-shadow style of a [Modifier.materialShadowEdge], the portable port of COUI's
 * `InnerShadowStyleSpec`.
 *
 * The shadow is not a blurred drop shadow but a fade painted inward from the rim: the shader walks
 * the surface's signed distance field, and the near / far thresholds in
 * [`InnerShadowStyleMapper.toShadowArray16`][InnerShadowStyleMapper] decide how far the fade reaches
 * and how it is shaped. Dark presets are byte-identical to light ones in ColorOS.
 *
 * @param fadeSize Base size the fade scale is measured against; `{width, height}` in pixels.
 * @param fadeColor Fade colour, `{r, g, b}` in 0..1.
 * @param fadeAlpha The in / out alphas, `{fadeIn, fadeOut, fadeAlphaOut}`.
 * @param fadeOffset Vertical (and horizontal) fade offset, `{x, y}`.
 * @param fadeScale Fade size multiplier, `{x, y}`.
 * @param fadeIn How far the fade reaches in from the rim; divided through the 0.34 density factor.
 * @param fadeInPara1 / fadeInPara2 The `pow` shaping pair of the fade-in ramp.
 * @param fadeOut How far the fade-out band reaches; also the band that closes the rim.
 * @param fadeOutPara1 / fadeOutPara2 The `pow` shaping pair of the fade-out ramp.
 */
@Immutable
class InnerShadowStyle(
    val fadeSize: FloatArray,
    val fadeColor: FloatArray,
    val fadeAlpha: FloatArray,
    val fadeOffset: FloatArray,
    val fadeScale: FloatArray,
    val fadeIn: Float,
    val fadeInPara1: Float,
    val fadeInPara2: Float,
    val fadeOut: Float,
    val fadeOutPara1: Float,
    val fadeOutPara2: Float,
) {
    companion object {
        /** `InnerShadowStylePresets.SHADOW_STYLE_LIGHT_0` — the capsule inner shadow. */
        val SHADOW_STYLE_0: InnerShadowStyle = InnerShadowStyle(
            fadeSize = floatArrayOf(400f, 100f),
            fadeColor = floatArrayOf(1f, 1f, 1f),
            fadeAlpha = floatArrayOf(1f, 0f, 0f),
            fadeOffset = floatArrayOf(0f, 20f),
            fadeScale = floatArrayOf(1.3f, 1.15f),
            fadeIn = 120f,
            fadeInPara1 = 4f,
            fadeInPara2 = 0.9f,
            fadeOut = 20f,
            fadeOutPara1 = 4f,
            fadeOutPara2 = 0.7f,
        )

        /** `InnerShadowStylePresets.SHADOW_STYLE_LIGHT_1` — the compact icon-button inner shadow. */
        val SHADOW_STYLE_1: InnerShadowStyle = InnerShadowStyle(
            fadeSize = floatArrayOf(160f, 160f),
            fadeColor = floatArrayOf(1f, 1f, 1f),
            fadeAlpha = floatArrayOf(0.4f, 0f, 0f),
            fadeOffset = floatArrayOf(0f, 20f),
            fadeScale = floatArrayOf(1.4f, 1.35f),
            fadeIn = 140f,
            fadeInPara1 = 4f,
            fadeInPara2 = 0.9f,
            fadeOut = 20f,
            fadeOutPara1 = 4f,
            fadeOutPara2 = 0.7f,
        )

        /** `InnerShadowStylePresets.SHADOW_STYLE_LIGHT_2` — the large panel inner shadow. */
        val SHADOW_STYLE_2: InnerShadowStyle = InnerShadowStyle(
            fadeSize = floatArrayOf(600f, 600f),
            fadeColor = floatArrayOf(1f, 1f, 1f),
            fadeAlpha = floatArrayOf(0.6f, 0f, 0f),
            fadeOffset = floatArrayOf(0f, 10f),
            fadeScale = floatArrayOf(1.1f, 1.07f),
            fadeIn = 86f,
            fadeInPara1 = 4f,
            fadeInPara2 = 0.7f,
            fadeOut = 1f,
            fadeOutPara1 = 1f,
            fadeOutPara2 = 0f,
        )
    }
}

/**
 * The edge parameters of a material surface, the portable port of
 * `COUIMaterialStrokeEffect.StrokeEffectParams`.
 *
 * `COUIMaterialStrokeEffect` is a table of `(strokeType, edgeAlpha, edgeWidthDp, edgeAngle,
 * shadowFadeIn, shadowFadeOut, shadowFadeScale)` tuples; the two entries needed by the floating
 * toolbar are [FrameworkCapsule5Light] / [FrameworkCapsule5Dark] (the framework edge-effect path,
 * `TYPE_FRAMEWORK_CAPSULE_5`) and [FrameworkCapsule13Light] / [FrameworkCapsule13Dark] (the
 * runtime-shader `COUIShadowEdgeDrawable` path, `TYPE_FRAMEWORK_CAPSULE_13`).
 *
 * The presets are split by night mode; `getStrokeParamsByType` resolves the pair by
 * `isNightMode`, and `getEdgeWidth(context)` converts [edgeWidth] from dp to px before it reaches
 * the shader's `setLineWidth`.
 *
 * @param edgeAlpha The rim alpha, `StrokeEffectParams.edgeAlpha`.
 * @param edgeWidth The rim width in dp, `StrokeEffectParams.edgeWidthDp`.
 * @param edgeAngle The rim angle in radians, `StrokeEffectParams.edgeAngle` (0 for every preset).
 * @param shadowFadeIn / shadowFadeOut The inner-shadow in / out reach, mapped onto the shader's
 *   `setFadeAlpha(in, out)`.
 * @param shadowFadeScale The inner-shadow width scale, mapped onto `setFadeWidthScale`.
 */
@Immutable
data class MaterialStrokeParams(
    val edgeAlpha: Float,
    val edgeWidth: Dp,
    val edgeAngle: Float = 0f,
    val shadowFadeIn: Float = 0f,
    val shadowFadeOut: Float = 0f,
    val shadowFadeScale: Float = 1f,
) {
    companion object {
        /** `FRAMEWORK_CAPSULE_PARAMS_5_LIGHT` — `TYPE_FRAMEWORK_CAPSULE_5`, light theme. */
        val FrameworkCapsule5Light: MaterialStrokeParams = MaterialStrokeParams(
            edgeAlpha = 0.2f,
            edgeWidth = 2.3.dp,
            edgeAngle = 0f,
            shadowFadeIn = 0.1f,
            shadowFadeOut = 1f,
            shadowFadeScale = 1f,
        )

        /** `FRAMEWORK_CAPSULE_PARAMS_5_DARK` — `TYPE_FRAMEWORK_CAPSULE_5`, dark theme. */
        val FrameworkCapsule5Dark: MaterialStrokeParams = MaterialStrokeParams(
            edgeAlpha = 0.8f,
            edgeWidth = 2.5.dp,
            edgeAngle = 0f,
            shadowFadeIn = 0.03f,
            shadowFadeOut = 1f,
            shadowFadeScale = 1f,
        )

        /** `FRAMEWORK_CAPSULE_PARAMS_13_LIGHT` — `TYPE_FRAMEWORK_CAPSULE_13`, light theme. */
        val FrameworkCapsule13Light: MaterialStrokeParams = MaterialStrokeParams(
            edgeAlpha = 0.8f,
            edgeWidth = 2.3.dp,
            edgeAngle = 0f,
            shadowFadeIn = 0.1f,
            shadowFadeOut = 0f,
            shadowFadeScale = 1f,
        )

        /** `FRAMEWORK_CAPSULE_PARAMS_13_DARK` — `TYPE_FRAMEWORK_CAPSULE_13`, dark theme. */
        val FrameworkCapsule13Dark: MaterialStrokeParams = MaterialStrokeParams(
            edgeAlpha = 0.15f,
            edgeWidth = 2.3.dp,
            edgeAngle = 0f,
            shadowFadeIn = 0.01f,
            shadowFadeOut = 0f,
            shadowFadeScale = 1f,
        )

        /** Resolves `TYPE_FRAMEWORK_CAPSULE_5` by night mode, as `getStrokeParamsByType` does. */
        fun frameworkCapsule5(isDark: Boolean): MaterialStrokeParams = if (isDark) FrameworkCapsule5Dark else FrameworkCapsule5Light

        /** Resolves `TYPE_FRAMEWORK_CAPSULE_13` by night mode, as `getStrokeParamsByType` does. */
        fun frameworkCapsule13(isDark: Boolean): MaterialStrokeParams = if (isDark) FrameworkCapsule13Dark else FrameworkCapsule13Light
    }
}

/** Defaults for [Modifier.materialShadowEdge]. */
object MaterialShadowEdgeDefaults {

    /**
     * The shader's `u_commonArray[11]` anti-aliasing value.
     *
     * `ShadowEdgeShader.mCommonUniform` starts at `1.0f` and no consumer in ColorOS calls
     * `setAntiAliasing`, so the band stays at one pixel either way.
     */
    val AntiAliasing = 1f

    /** The `u_commonArray[10]` corner weight of a surface with more than one item. */
    val CornerWeight = 1f

    /** The corner weight of COUI's smooth squircle silhouette (Lp exponent 2.5). */
    val SquircleCornerWeight = 2.5f

    /**
     * The `u_commonArray[10]` corner weight of a surface with exactly one item
     * (`COUIBottomFloatingToolbarMenuView.SINGLE_ITEM_CORNER_WEIGHT = 2.0f`, returned by
     * `computeMaterialCornerWeight` when the visible item count is one).
     */
    val SingleItemCornerWeight = 2f
}

/**
 * Draws the ColorOS 17 "Material" edge — a gradient stroke plus an inner shadow — over this node's
 * full bounds, the portable port of `com.coui.appcompat.shadowedge.COUIShadowEdgeDrawable` and its
 * `ShadowEdgeShader`.
 *
 * COUI gives every material surface the same runtime shader (`ShadowStroke`) and feeds it the
 * surface's own geometry, so the edge is computed per pixel from an SDF rather than stroked by the
 * canvas. The shader's `u_commonArray` carries that geometry: resolution, centre, size, the four
 * corner radii, the corner weight and the anti-aliasing band. This modifier always writes the
 * capsule form — all four radii equal and derived from the node height — because every consumer of
 * [`GradientStrokeStyle.EDGE_STYLE_0`] here is a capsule; pass an explicit [cornerRadius] for a
 * surface whose corners are not a capsule.
 *
 * The rim uses `setCornerType(2)`, i.e. the shader's `NEW_G2` corner geometry with its
 * `smoothSDF_D` capsule distance and `r * 0.0125` capsule scale; the `COMMON`, `BEZIER` and `OLD_G2`
 * variants are not ported because no consumer sets them.
 *
 * Uniforms are pushed with the plain float-array setters, never `setColorUniform`: AGSL reserves the
 * colour setter for `layout(color)` uniforms and SkSL rejects that qualifier, so the two blend
 * colours travel as raw 0..1 floats like the rest of the array.
 *
 * @param stroke The rim and inner-shadow parameters, normally a [MaterialStrokeParams] preset.
 * @param fallback Used when the platform cannot run runtime shaders; the caller's opaque
 *   background belongs here.
 * @param cornerWeight The `u_commonArray[10]` corner weight; see
 *   [MaterialShadowEdgeDefaults.SingleItemCornerWeight].
 * @param cornerRadius The corner radius, or `null` for the capsule rule `height / 2`.
 * @param edgeStyle The rim style, one of the [GradientStrokeStyle] presets.
 * @param shadowStyle The inner-shadow style, one of the [InnerShadowStyle] presets.
 * @param antiAliasing The shader's anti-aliasing band, `u_commonArray[11]`.
 */
@Composable
fun Modifier.materialShadowEdge(
    stroke: MaterialStrokeParams,
    fallback: Modifier = Modifier,
    cornerWeight: Float = MaterialShadowEdgeDefaults.CornerWeight,
    cornerRadius: Dp? = null,
    edgeStyle: GradientStrokeStyle = GradientStrokeStyle.EDGE_STYLE_0,
    shadowStyle: InnerShadowStyle = InnerShadowStyle.SHADOW_STYLE_0,
    antiAliasing: Float = MaterialShadowEdgeDefaults.AntiAliasing,
): Modifier {
    val density = LocalDensity.current.density
    // A runtime shader is compiled by the driver on first use, so a source the platform rejects
    // would otherwise throw mid-composition; the fallback keeps the caller rendering.
    val shader = rememberMaterialShadowEdgeShader(isRuntimeShaderSupported()) ?: return fallback

    return this.drawWithCache {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@drawWithCache onDrawBehind {}
        val radius = cornerRadius?.toPx() ?: (height / 2f)

        // u_commonArray = [resolution.x, resolution.y, position.x, position.y, size.w, size.h,
        //                  cornerRadius.(lb, rb, rt, lt), weight, antiAliasing]
        shader.setFloatUniform(
            "u_commonArray",
            floatArrayOf(width, height, 0f, 0f, width, height, radius, radius, radius, radius, cornerWeight, antiAliasing),
        )
        shader.setFloatUniform("u_shadow", 1f)
        shader.setFloatUniform(
            "u_shadowArray",
            InnerShadowStyleMapper.toShadowArray16(
                style = shadowStyle,
                width = width,
                height = height,
                fadeWidthScale = stroke.shadowFadeScale,
                fadeAlphaInScale = stroke.shadowFadeIn,
                fadeAlphaOutScale = stroke.shadowFadeOut,
                density = density,
            ),
        )
        shader.setFloatUniform("u_edge", 1f)
        shader.setFloatUniform(
            "u_edgeArray",
            GradientStrokeStyleMapper.toEdgeArray16(
                style = edgeStyle,
                width = width,
                height = height,
                lineWidth = stroke.edgeWidth.toPx(),
                lineAlpha = stroke.edgeAlpha,
                lineAngle = stroke.edgeAngle,
                density = density,
            ),
        )
        onDrawBehind { drawRect(brush = shader.asBrush(), size = size) }
    }
}

/** Compiles (and remembers) the material edge shader; `null` when runtime shaders are unavailable. */
@Composable
private fun rememberMaterialShadowEdgeShader(enabled: Boolean): RuntimeShader? {
    if (!enabled) return null
    return remember { runCatching { RuntimeShader(MATERIAL_SHADOW_EDGE_SHADER) }.getOrNull() }
}

/**
 * The portable port of `com.coui.appcompat.shadowedge.GradientStrokeStyleMapper`.
 *
 * It flattens a [GradientStrokeStyle] plus the surface size and rim parameters into the shader's 16
 * floats, reproducing the native accumulation steps exactly: `[4] += [3]`, `[6] += [5]`,
 * `[10] += [9]` and `[12] += [11]`, and the `(lineWidth > 0 ? lineWidth : 7) / 7` line factor where
 * 7 is `coerceAtLeast(max(near.x + near.y, far.x + far.y), 1e-4f)`.
 */
internal object GradientStrokeStyleMapper {

    fun toEdgeArray16(
        style: GradientStrokeStyle,
        width: Float,
        height: Float,
        lineWidth: Float,
        lineAlpha: Float,
        lineAngle: Float,
        density: Float,
        outArray: FloatArray? = null,
    ): FloatArray {
        val factor = maxOf(1f, density) * 0.34f
        val nearX = style.rectLineNear.at(0, 1f)
        val nearY = style.rectLineNear.at(1, 6f)
        val farX = style.rectLineFar.at(0, 1f)
        val farY = style.rectLineFar.at(1, 4f)
        val sizeX = maxOf(1f, width) / factor
        val sizeY = maxOf(1f, height) / factor
        val baseW = maxOf(style.rectLineSize.at(0, sizeX), 1f)
        val baseH = maxOf(style.rectLineSize.at(1, sizeY), 1f)
        val lineDivisor = maxOf(maxOf(nearX + nearY, farX + farY), 1e-4f)
        val lineFactor = (if (lineWidth > 0f) lineWidth else lineDivisor) / lineDivisor

        val out = outArray ?: FloatArray(16)
        out[0] = style.rectLineColor.at(0, 1f)
        out[1] = style.rectLineColor.at(1, 0f)
        out[2] = style.rectLineColor.at(2, 0f)
        out[3] = maxOf(nearX * lineFactor, 2f)
        out[4] = maxOf(nearY * lineFactor, 2f)
        out[5] = maxOf(farX * lineFactor, 2f)
        out[6] = maxOf(farY * lineFactor, 2f)
        out[7] = style.rectLineAlpha.at(0, 1f) * lineAlpha
        out[8] = style.rectLineAlpha.at(1, 0.8f) * lineAlpha
        rectLineFadeForSize(
            baseFadex = style.rectLineFade.at(0, 0f),
            baseFadey = style.rectLineFade.at(1, 0f),
            baseW = baseW,
            baseH = baseH,
            w = sizeX,
            h = sizeY,
            out = out,
            offset0 = 9,
            offset1 = 10,
        )
        rectLineFadeForSize(
            baseFadex = style.rectLineFade.at(2, 1f),
            baseFadey = style.rectLineFade.at(3, 0f),
            baseW = baseW,
            baseH = baseH,
            w = sizeX,
            h = sizeY,
            out = out,
            offset0 = 11,
            offset1 = 12,
        )
        out[4] += out[3]
        out[6] += out[5]
        out[10] += out[9]
        out[12] += out[11]
        out[13] = lineAngle
        out[14] = style.rectLinePara1
        out[15] = style.rectLinePara2
        return out
    }

    /**
     * Ports `GradientStrokeStyleKt.rectLineFadeForSize`: the near / far fade pair is expressed as a
     * fraction of the perimeter `(h + w) * 2`, then folded into the surface size so the fade keeps
     * its proportion when the surface grows.
     */
    private fun rectLineFadeForSize(
        baseFadex: Float,
        baseFadey: Float,
        baseW: Float,
        baseH: Float,
        w: Float,
        h: Float,
        out: FloatArray,
        offset0: Int,
        offset1: Int,
    ) {
        val basePerimeter = (baseH + baseW) * 2f
        val perimeter = (h + w) * 2f
        val fadeSum = baseFadex + baseFadey
        val faded = fadeSum * basePerimeter
        if (faded > baseW) {
            val farFade = baseFadey * basePerimeter
            out[offset0] = (((faded - baseW) + w) - farFade) / perimeter
            out[offset1] = farFade / perimeter
            return
        }
        val remaining = baseW - faded
        if (fadeSum <= 0f) {
            out[offset0] = baseFadex
            out[offset1] = baseFadey
        } else {
            val fraction = (w - remaining) / perimeter
            out[offset0] = (baseFadex * fraction) / fadeSum
            out[offset1] = (baseFadey * fraction) / fadeSum
        }
    }
}

/**
 * The portable port of `com.coui.appcompat.shadowedge.InnerShadowStyleMapper`.
 *
 * It flattens an [InnerShadowStyle] into the shader's 16-float `u_shadowArray` with the native
 * density geometry factor `fMax = max(1, density) * 0.34f`: the fade reach and offsets scale with
 * `fMax`, while the fade scales divide `max(1, width) / fMax` and `max(1, height) / fMax`, so the
 * inner shadow keeps the same visual reach at any density.
 */
internal object InnerShadowStyleMapper {

    fun toShadowArray16(
        style: InnerShadowStyle,
        width: Float,
        height: Float,
        fadeWidthScale: Float,
        fadeAlphaInScale: Float,
        fadeAlphaOutScale: Float,
        density: Float,
        outArray: FloatArray? = null,
    ): FloatArray {
        val factor = maxOf(1f, density) * 0.34f
        val sizeX = maxOf(1f, width) / factor
        val sizeY = maxOf(1f, height) / factor
        val out = outArray ?: FloatArray(16)
        out[0] = style.fadeColor.at(0, 1f)
        out[1] = style.fadeColor.at(1, 0f)
        out[2] = style.fadeColor.at(2, 0f)
        out[3] = style.fadeAlpha.at(0, 0f) * fadeAlphaInScale
        out[4] = style.fadeAlpha.at(1, 0f) * fadeAlphaInScale
        out[5] = style.fadeAlpha.at(2, 0f) * fadeAlphaOutScale
        out[6] = style.fadeIn * fadeWidthScale * factor
        out[7] = style.fadeOffset.at(0, 0f) * factor
        out[8] = style.fadeOffset.at(1, 0f) * factor
        out[9] = ((style.fadeScale.at(0, 1f) - 1f) * style.fadeSize.at(0, 1f)) / sizeX + 1f
        out[10] = ((style.fadeScale.at(1, 1f) - 1f) * style.fadeSize.at(1, 1f)) / sizeY + 1f
        out[11] = style.fadeInPara1
        out[12] = style.fadeInPara2
        out[13] = style.fadeOut * fadeWidthScale * factor
        out[14] = style.fadeOutPara1
        out[15] = style.fadeOutPara2
        return out
    }
}

/** `GradientStrokeStyleKt.at` / `InnerShadowStyleKt.at`: the slot value, or [default] when absent. */
private fun FloatArray.at(index: Int, default: Float): Float = if (index < 0 || index >= size) default else this[index]

/**
 * The ColorOS 17 `ShadowStroke` runtime shader (`com.coui.appcompat.shadowedge.agsl.ShadowEdge`),
 * reduced to the `NEW_G2` corner branch that every consumer enables via `setCornerType(2)`.
 *
 * Ported verbatim from the Java string constants, with only the SkSL dialect rename applied:
 * `vec2/vec3/vec4` → `float2/float3/float4`, the entry point `vec4 main(vec2)` → `half4
 * main(float2)`, scalar broadcast in `max(q, 0.0)` spelled as `max(q, float2(0.0))`, and the two
 * returns converted to `half4`. The `u_commonArray`, `u_shadow`, `u_shadowArray`, `u_edge` and
 * `u_edgeArray` declarations are unchanged, as is the `smoothSDF_D` capsule geometry with
 * `DEFAULT_TRANSITION_PIXELS = 4.0`, `kAbs = 1.0`, `rRef = 80.0`, `kMax = 26.6`,
 * `junctionXBand = 70.0`, `junctionYBand = 60.0` and the `r * 0.0125` capsule scale.
 *
 * The `COMMON` / `BEZIER` / `OLD_G2` corner branches are not ported: `setCornerType` is called with
 * `2` everywhere in ColorOS, so their fragments would be dead code.
 */
private val MATERIAL_SHADOW_EDGE_SHADER = """
uniform float u_commonArray[12];
uniform float u_shadow;
uniform float u_shadowArray[16];
uniform float u_edge;
uniform float u_edgeArray[16];

const float EPS = 1e-6;
const float EFFECT_PARAM_THRESHOLD = 0.001;

struct CommonDataRect{
    float2 halfResolution;
    float2 pos;
    float2 halfSize;
    float coner;
    float weight;
    float preCalcMinC;
};

float getQuadrantValue(float2 pos, float4 values){
    float2 s = step(float2(0.0), pos);
    float valueTop = mix(values.x, values.y, s.x);
    float valueBot = mix(values.w, values.z, s.x);
    float value = mix(valueTop, valueBot, s.y);
    return value;
}

float noise(float2 p){
    return fract(sin(dot(p.yx,float2(79.1214,78.233)))*43760.5453);
}

float softLight(float A,float B){
    float l=B-(1.-2.*A)*B*(1.-B);
    float h=B+(2.*A-1.)*(sqrt(B)-B);
    return mix(l,h,step(.5,A));
}

const float DEFAULT_TRANSITION_PIXELS = 4.0;
const float kAbs = 1.0;
const float rRef = 80.0;
const float kMax = 26.6;
const float junctionXBand = 70.0;
const float junctionYBand = 60.0;
const float junctionPower = 1.0;

float smoothAbs(float x, float k) {
    return sqrt(x * x + k * k);
}

float smoothMaxSqrt(float a, float b, float k) {
    float d = a - b;
    return 0.5 * (a + b + sqrt(d * d + k * k));
}

float sdCapsule(float2 pLocal, float2 halfSize, inout float zoneCap, inout float transitionPixels) {
    bool isVertical = halfSize.x < halfSize.y;
    float2 hs = isVertical ? halfSize.yx : halfSize;
    float2 pl = isVertical ? pLocal.yx : pLocal;

    float2 q = float2(smoothAbs(pl.x, kAbs), smoothAbs(pl.y, kAbs));
    float r = hs.y;
    float a = max(hs.x - r, 0.0);

    zoneCap = abs(pl.x) - a;

    // float scale = r / max(rRef, 1e-3);
    float scale = r * 0.0125;
    float kMaxEff = kMax * scale;
    float xBandEff = junctionXBand * scale;
    float yBandEff = junctionYBand * scale;

    transitionPixels = max(DEFAULT_TRANSITION_PIXELS, xBandEff);

    float s = q.x - a;
    float xHard = max(s, 0.0);
    float xSoft = (kMaxEff > 0.0) ? smoothMaxSqrt(s, 0.0, kMaxEff) : xHard;
    float wx = 1.0 - smoothstep(0.0, max(xBandEff, 1e-3), abs(s));
    float wy = smoothstep(r - max(yBandEff, 1e-3), r, q.y);
    // float w = pow(clamp(wx * wy, 0.0, 1.0), junctionPower);
    float w = clamp(wx * wy, 0.0, 1.0);
    float x = mix(xHard, xSoft, w);
    return length(float2(x, q.y)) - r;
}

float powFast(float x, float n) {
    // 保护极小值，防止 log2(0)
    x = max(x, 1e-8);
    return exp2(log2(x) * n);
}

float lpNorm(float2 v, float n) {
    v = abs(v);
    float m = max(v.x, v.y);
    if (m < 1e-8) return 0.0;
    float2 u = v / m;
    float s = powFast(u.x, n) + powFast(u.y, n);
    return m * exp2(log2(max(s, EPS)) / n);
}

float lpNorm4(float2 v) {
    v = abs(v);
    float2 v2 = v * v;
    float2 v4 = v2 * v2;
    return sqrt(sqrt(max(v4.x + v4.y, 0.0)));
}

float sdRoundRect(float2 p, float2 halfSize, float r, float n, inout float2 q) {
    q = abs(p) - halfSize + float2(r);
    float nClamped = max(n, 2.0);
    float dCorner = (abs(nClamped - 4.0) < 1e-3) ? lpNorm4(max(q, float2(0.0))) : lpNorm(max(q, float2(0.0)), nClamped);
    if (q.x > 0.0 && q.y > 0.0) return dCorner - r;
    else return max(q.x, q.y) - r;
}


float smoothSDF_D(float2 p, float2 b, float corner, float weight, inout float aaFactor){
    float rectSdf = 0.0;
    float d_corner_zone = 0.0;
    float2 q_rect = float2(0.0, 0.0);
    float transitionPixels = DEFAULT_TRANSITION_PIXELS; // 默认过渡像素

    if (weight  < 2.0) {
        float zoneCap = 0.0;
        if(weight  > 1.005){
            float t = smoothstep(2.0, 1.0, weight);
            float d_G2 = sdRoundRect( p, b, corner, 2.0, q_rect);
            float zone_G2 = min(q_rect.x, q_rect.y);

            float d_Capsule = sdCapsule( p, b, zoneCap, transitionPixels);
            rectSdf = mix(d_G2, d_Capsule, t);
            d_corner_zone = mix(zone_G2, zoneCap, t);
        }
        else
        {
            rectSdf = sdCapsule( p, b, zoneCap, transitionPixels);
            d_corner_zone = zoneCap;
        }
    } else {
        rectSdf = sdRoundRect( p, b, corner, weight, q_rect);
        d_corner_zone = min(q_rect.x, q_rect.y);
    }
    // 计算抗锯齿因子 (注意区间：从直线区走向圆角区)
    aaFactor = smoothstep(-transitionPixels - 2.0, -transitionPixels * 0.2, d_corner_zone);
    return rectSdf;
}

float4 blendShadowColor(CommonDataRect rectData, float4 color, float d, float2 fragCoord){

    float3 uFadeColor = float3(u_shadowArray[0], u_shadowArray[1], u_shadowArray[2]);
        float3 uFadeAlpha = float3(u_shadowArray[3], u_shadowArray[4], u_shadowArray[5]);
        float uFadeIn = u_shadowArray[6];
        float2 uFadeOffset = float2(u_shadowArray[7], u_shadowArray[8]);
        float2 uFadeScale = float2(u_shadowArray[9], u_shadowArray[10]);
        float uFadeInPara1 =  u_shadowArray[11];
        float uFadeInPara2 =  u_shadowArray[12];
        float uFadeOut = u_shadowArray[13];
        float uFadeOutPara1 =  u_shadowArray[14];
        float uFadeOutPara2 = u_shadowArray[15];

        float2 fadeConer;
        fadeConer.y = min(rectData.preCalcMinC, max(uFadeOut*1.2, rectData.coner));
        float fadeAlpha = 0.0;

                if(any(greaterThan(uFadeAlpha.xy, float2(EFFECT_PARAM_THRESHOLD)))){
                    float2 pos_new = (rectData.pos-uFadeOffset);
                    fadeConer.x = getQuadrantValue(pos_new, float4(u_commonArray[6],u_commonArray[7],u_commonArray[8],u_commonArray[9]));
                    float fadeLine = uFadeIn*-1.0;
                    float fadeSdf = fadeLine;
                    float2 halfSizeFade = rectData.halfSize*uFadeScale;
                    fadeConer.x = min(min(halfSizeFade.x, halfSizeFade.y), max(uFadeIn*1.2, fadeConer.x));
                    if(rectData.weight<2.0 || any(greaterThan(abs(pos_new), halfSizeFade - float2(fadeConer.x)))){
                        float aaFactor = 1.0;
                        fadeSdf = smoothSDF_D(pos_new, halfSizeFade, fadeConer.x, rectData.weight, aaFactor);
                        float alpha = smoothstep(fadeLine,0.0,fadeSdf);
                        if(uFadeInPara2>EFFECT_PARAM_THRESHOLD){
                            alpha = pow(alpha, uFadeInPara1)*uFadeInPara2+(1.0-uFadeInPara2)*alpha;
                        }
                        alpha = alpha*(uFadeAlpha.x-uFadeAlpha.y) + uFadeAlpha.y;
                        fadeAlpha = alpha;
                    }else{
                        fadeAlpha = uFadeAlpha.y;
                    }
                }

                if(uFadeAlpha.z>EFFECT_PARAM_THRESHOLD){
                    float fadeLine =  uFadeOut *-1.0;
                    float fadeSdf = fadeLine;
                    if(rectData.weight<2.0 || any(greaterThan(abs(rectData.pos), rectData.halfSize - float2(fadeConer.y)))){
                        if(abs(fadeConer.y - rectData.coner) < EFFECT_PARAM_THRESHOLD){
                            fadeSdf = d;
                        }else{
                            float aaFactor = 1.0;
                            fadeSdf = smoothSDF_D(rectData.pos, rectData.halfSize, fadeConer.y, rectData.weight, aaFactor);
                        }
                        float alpha = smoothstep(fadeLine,0.0,fadeSdf);
                        if(uFadeOutPara2>EFFECT_PARAM_THRESHOLD){
                            alpha = pow(alpha, uFadeOutPara1)*uFadeOutPara2+(1.0-uFadeOutPara2)*alpha;
                        }
                        alpha *= uFadeAlpha.z;
                        fadeAlpha = mix(fadeAlpha, 1.0,alpha);
                    }
                }

                float2 uv_pos=fragCoord.xy*0.013;
                float n=clamp((noise(uv_pos)-.5)*0.01+.5,0.,1.);
                fadeAlpha=clamp(softLight(n,fadeAlpha),0.0,1.0);

                float4 fadeColor = float4(uFadeColor, fadeAlpha);
                fadeColor.rgb *= fadeColor.a;

                float4 reColor = float4(0.0);
                reColor.rgb = color.rgb*(1.0 - fadeColor.a)+fadeColor.rgb;
                reColor.a = color.a*(1.0 - fadeColor.a)+fadeColor.a;
                return reColor;
        }

float getDis(float2 pos,float2 halfsize){
        float2 rect=abs(pos-halfsize);
        return rect.x+rect.y;
    }

float4 blendEdge(CommonDataRect rectData, float4 color, float d){

        float3 uRectLineColor = float3(u_edgeArray[0], u_edgeArray[1], u_edgeArray[2]);
        float2 uRectLineNear = float2(u_edgeArray[3], u_edgeArray[4]);
        float2 uRectLineFar = float2(u_edgeArray[5], u_edgeArray[6]);
        float2 uRectLineAlpha = float2(u_edgeArray[7], u_edgeArray[8]);
        float4 uRectLineFade = float4(u_edgeArray[9], u_edgeArray[10], u_edgeArray[11], u_edgeArray[12]);
        float uAngle = u_edgeArray[13];
        float uRectLinePara1 = u_edgeArray[14];
        float uRectLinePara2 = u_edgeArray[15];

        float2 rectLineNear=uRectLineNear;
        float2 rectLineFar=uRectLineFar;
        float4 rectLineFade=uRectLineFade;

        if(d<-max(rectLineNear.y, rectLineFar.y)){
            return color;
        }

        float distanceAll = (rectData.halfSize.x+rectData.halfSize.y)*2.0;
        float2 halfSize = rectData.halfSize;

        float ratio = mod(uAngle+rectData.halfSize.x/distanceAll*0.5+0.5,1.0);
        float2 dir_pos= normalize(rectData.pos);
        float2 dirAbs_pos = abs(dir_pos);
        float2 pos_n = min(float2(dirAbs_pos.xy*halfSize.yx/max(float2(EFFECT_PARAM_THRESHOLD),dirAbs_pos.yx)),halfSize)*sign(dir_pos);
        float2 halfSize_symobol = float2(-halfSize.y,halfSize.x);
        float flag = mix(-1.0,1.0, step(ratio,0.5))*dot(halfSize_symobol,pos_n);
        float dis_point = distanceAll*min(ratio,1.0-ratio)*2.0;
        float dis_pos = getDis(pos_n,halfSize);
        float distance_near = mix(dis_point+dis_pos,abs(dis_point-dis_pos),step(0.,flag));
        distance_near=min(2.*distanceAll-distance_near,distance_near);
        float lineNearAlpha = uRectLineAlpha.x*smoothstep(rectLineFade.y*distanceAll, rectLineFade.x*distanceAll, distance_near);
        float lineFarAlpha = uRectLineAlpha.y*smoothstep(rectLineFade.w*distanceAll, rectLineFade.z*distanceAll, distanceAll-distance_near);

        float lineNearSdf=smoothstep(-rectLineNear.y,-rectLineNear.x,d);
        float lineFarSdf=smoothstep(-rectLineFar.y,-rectLineFar.x,d);

        if(uRectLinePara2 > EFFECT_PARAM_THRESHOLD){
            lineNearSdf = pow(lineNearSdf, uRectLinePara1)*uRectLinePara2+(1.0-uRectLinePara2)*lineNearSdf;
            lineFarSdf = pow(lineFarSdf, uRectLinePara1)*uRectLinePara2+(1.0-uRectLinePara2)*lineFarSdf;
        }

        lineNearAlpha*=lineNearSdf;
        lineFarAlpha*=lineFarSdf;

        float lineAlpha = clamp(lineNearAlpha+lineFarAlpha,0.,1.);

        float4 rectLineColor = float4(uRectLineColor, lineAlpha);
        rectLineColor.rgb *= rectLineColor.a;

        float4 reColor = float4(0.0);
        reColor.rgb = color.rgb*(1.0 - rectLineColor.a)+rectLineColor.rgb;
        reColor.a = color.a*(1.0 - rectLineColor.a)+rectLineColor.a;
        return reColor;
    }

half4 main(float2 fragCoord) {
        //数据准备
        float antiAliasing = u_commonArray[11];
        CommonDataRect rectData;
        rectData.halfResolution = float2(u_commonArray[0], u_commonArray[1])*0.5;
        // 将fragCoord转换为以矩形中心为原点的坐标系统
        rectData.pos = float2(fragCoord.x, u_commonArray[1]-fragCoord.y) - float2(u_commonArray[2], u_commonArray[3]) - rectData.halfResolution;
        rectData.halfSize =  float2(u_commonArray[4], u_commonArray[5])*0.5;

        float2 s = step(float2(0.0), rectData.pos);
        float rTop = mix(u_commonArray[6], u_commonArray[7], s.x);
        float rBot = mix(u_commonArray[9], u_commonArray[8], s.x);
        float r = mix(rTop, rBot, s.y);
        rectData.coner =  r;

        rectData.weight = u_commonArray[10];
        rectData.preCalcMinC = min(rectData.halfSize.x, rectData.halfSize.y);

        //形状计算
        float d = 0.0;
        float aaFactor = 1.0;

        d = smoothSDF_D(rectData.pos, rectData.halfSize, rectData.coner, rectData.weight, aaFactor);

         float aa = 1.0;
            aa = mix(EPS, aa, aaFactor);

            float shape = 1.0 - smoothstep(-aa, aa, d);

            if(shape<EFFECT_PARAM_THRESHOLD){
                return half4(0.0);
            }

            //基础颜色
            float4 color = float4(0.0);

            //内阴影
            if(u_shadow>0.5){
                color = blendShadowColor(rectData, color, d, fragCoord);
            }

            //描边
            if(u_edge>0.5){
                color = blendEdge(rectData, color, d);
            }

            return half4(color*shape);
        }
"""
