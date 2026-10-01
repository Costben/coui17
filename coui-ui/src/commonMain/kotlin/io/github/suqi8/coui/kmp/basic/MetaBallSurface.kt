// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("FunctionName")

package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurDefaults
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.RuntimeShader
import io.github.suqi8.coui.kmp.blur.asBrush
import io.github.suqi8.coui.kmp.blur.textureBlur
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * One silhouette in a [MetaBallSurface]: a rounded rectangle, or a circle when [cornerRadius] is
 * half the shorter side.
 *
 * Positions are relative to the surface's own top-left corner, so a caller keeps animating the
 * particles without re-measuring anything.
 *
 * @property centerX The horizontal centre of the particle.
 * @property centerY The vertical centre of the particle.
 * @property width The full width of the particle.
 * @property height The full height of the particle.
 * @property cornerRadius The corner radius; defaults to a full capsule.
 */
@Immutable
data class MetaBallParticle(
    val centerX: Dp,
    val centerY: Dp,
    val width: Dp,
    val height: Dp,
    val cornerRadius: Dp = minOf(width, height) / 2,
) {
    companion object {
        /** A circular particle; COUI's melt ball itself is one of these. */
        fun circle(centerX: Dp, centerY: Dp, diameter: Dp): MetaBallParticle = MetaBallParticle(centerX = centerX, centerY = centerY, width = diameter, height = diameter)
    }
}

/** Defaults for the ColorOS 17 melt ball surface. */
object MetaBallDefaults {

    /**
     * How far each silhouette's field reaches past its own outline, in dp.
     *
     * COUI `mMeltBallParamExtension` (`coui_bottom_floating_navigation_bar_melt_ball_param_extension`)
     * — the `extension` argument of `OplusMaterialMetaBallParams`.
     */
    val Extension = 4.dp

    /**
     * Horizontal reach multiplier, COUI `MELT_BALL_EXTENSION_FACTOR_X`. The bridging fillet is
     * wider than it is tall, so two silhouettes side by side join with a shallow waist.
     */
    const val EXTENSION_FACTOR_X = 1.5f

    /** Vertical reach multiplier, COUI `MELT_BALL_EXTENSION_FACTOR_Y`. */
    const val EXTENSION_FACTOR_Y = 2.0f

    /**
     * Width of the light rim traced along the merged outline, in dp.
     *
     * The device's highlight is a thin line, not a glow: sampled down column x=636 of Gallery's dark
     * accessory bar the capsule's top edge steps 45, 89, 89, 66, 44 in one-pixel samples, so about
     * 0.7dp at Gallery's 565dpi.
     */
    val RimWidth = 0.5.dp

    /**
     * How far the rim's light bleeds inward from the outline before it dies, in dp.
     *
     * The rim is the glass highlight, not a drawn outline: it is brightest on the top edge and gone
     * at the sides, because COUI lights its glass with two directional lights that only catch a
     * surface whose normal points up. The old flat `White.copy(alpha = 0.55f)` traced that ring all
     * the way round at 151/255, where the device draws a single top edge at 89 over a 41 body.
     */
    val RimBlur = 0.5.dp
}

/**
 * The ColorOS 17 **melt ball** surface: several silhouettes that merge as one while their fields
 * overlap, the way two droplets of liquid pull together before they touch.
 *
 * COUI does this with `COUIMaterialEffect.setMetaBallChain` — a system effect that takes the views
 * in the chain (`COUIBottomFloatingNavigationBar` chains its main bar and its expanding menu) and
 * renders their union as one blob, so the menu melts out of the bar instead of landing on top of it.
 * The recipe is `OplusMaterialMetaBallParams(MODE, 0, extension, factorX, factorY)`.
 *
 * The union is computed per pixel from each silhouette's signed distance: the distances are folded
 * together with a smooth minimum whose radius is [extension] stretched by [extensionFactorX] /
 * [extensionFactorY] along the direction joining the two silhouettes. The result is a single
 * silhouette with an anti-aliased fillet where the shapes meet — a metaball, not an overlap.
 *
 * With a [backdrop] the merged silhouette masks the backdrop blur, so the blob is frosted glass the
 * same way [couiGlassSurface] renders a bar or a sheet; without one it is a solid [color] fill.
 * [content] draws above whichever of the two rendered.
 *
 * @param particles The silhouettes to merge, in this surface's own coordinates.
 * @param modifier The modifier to be applied to the surface.
 * @param color The body colour of the merged silhouette, normally the bar colour.
 * @param backdrop The content blurred behind the silhouette, defaulting to [LocalBackdrop]. `null`
 *   keeps the fill opaque.
 * @param blurRadius The blur radius in dp.
 * @param containerAlpha The alpha of [color] over the blur.
 * @param extension The field reach past each outline, in dp.
 * @param extensionFactorX The horizontal reach multiplier.
 * @param extensionFactorY The vertical reach multiplier.
 * @param rimColor The light traced along the merged outline, or `null` to skip it.
 * @param rimBlur How far that light bleeds inward before it dies, in dp.
 * @param bottomGlowColor A directional light that diffuses below the silhouette, or `null` to skip it.
 * @param bottomGlowBlur How far the bottom light diffuses outside the silhouette, in dp.
 * @param content The content drawn on top of the silhouette.
 */
@Composable
fun MetaBallSurface(
    particles: List<MetaBallParticle>,
    modifier: Modifier = Modifier,
    color: Color = COUITheme.colorScheme.bar,
    backdrop: Backdrop? = LocalBackdrop.current,
    blurRadius: Float = GlassDefaults.BlurRadius,
    containerAlpha: Float = GlassDefaults.ContainerAlpha,
    extension: Dp = MetaBallDefaults.Extension,
    extensionFactorX: Float = MetaBallDefaults.EXTENSION_FACTOR_X,
    extensionFactorY: Float = MetaBallDefaults.EXTENSION_FACTOR_Y,
    rimColor: Color? = Color.White.copy(alpha = 0.55f),
    rimBlur: Dp = MetaBallDefaults.RimBlur,
    bottomGlowColor: Color? = null,
    bottomGlowBlur: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val runsShader = isRuntimeShaderSupported()
    val shader = rememberMetaBallShader(runsShader)
    val glass = backdrop != null && shader != null

    Box(modifier = modifier) {
        if (bottomGlowColor != null && shader != null) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawMetaBall(
                    shader = shader,
                    particles = particles,
                    body = Color.Transparent,
                    rim = Color.Transparent,
                    extension = extension,
                    extensionFactorX = extensionFactorX,
                    extensionFactorY = extensionFactorY,
                    rimBlur = rimBlur,
                    bottomGlow = bottomGlowColor,
                    bottomGlowBlur = bottomGlowBlur,
                )
            }
        }
        if (glass) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .textureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        blurRadius = blurRadius,
                        colors = BlurDefaults.blurColors(
                            blendColors = listOf(BlendColorEntry(color = color.copy(alpha = containerAlpha))),
                        ),
                        // The blob is this node's content, so DstIn keeps exactly the blur that
                        // falls inside the merged outline.
                        contentBlendMode = BlendMode.DstIn,
                    ),
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawMetaBall(
                        shader = shader,
                        particles = particles,
                        body = Color.White,
                        rim = Color.Transparent,
                        extension = extension,
                        extensionFactorX = extensionFactorX,
                        extensionFactorY = extensionFactorY,
                        rimBlur = rimBlur,
                    )
                }
            }
            // A sibling, not a child: inside the mask the rim's own alpha would punch the blur out.
            if (rimColor != null) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawMetaBall(
                        shader = shader,
                        particles = particles,
                        body = Color.Transparent,
                        rim = rimColor,
                        extension = extension,
                        extensionFactorX = extensionFactorX,
                        extensionFactorY = extensionFactorY,
                        rimBlur = rimBlur,
                    )
                }
            }
        } else if (shader != null) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawMetaBall(
                    shader = shader,
                    particles = particles,
                    body = color,
                    rim = rimColor ?: Color.Transparent,
                    extension = extension,
                    extensionFactorX = extensionFactorX,
                    extensionFactorY = extensionFactorY,
                    rimBlur = rimBlur,
                )
            }
        } else {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawUnmergedParticles(particles = particles, color = color)
            }
        }
        content()
    }
}

/** Compiles (and remembers) the melt ball shader; `null` when runtime shaders are unavailable. */
@Composable
private fun rememberMetaBallShader(enabled: Boolean): RuntimeShader? {
    if (!enabled) return null
    // A runtime shader is compiled by the driver on first use, so a source the platform rejects
    // would otherwise throw mid-composition; falling back to the unmerged silhouettes keeps the
    // caller rendering.
    return remember { runCatching { RuntimeShader(META_BALL_SHADER) }.getOrNull() }
}

/** Uploads the particle uniforms and fills this draw scope with the merged silhouette. */
private fun DrawScope.drawMetaBall(
    shader: RuntimeShader,
    particles: List<MetaBallParticle>,
    body: Color,
    rim: Color,
    extension: Dp,
    extensionFactorX: Float,
    extensionFactorY: Float,
    rimBlur: Dp,
    bottomGlow: Color = Color.Transparent,
    bottomGlowBlur: Dp = 0.dp,
) {
    val count = minOf(particles.size, META_BALL_MAX_PARTICLES)
    if (count == 0) return

    val centers = FloatArray(META_BALL_MAX_PARTICLES * 2)
    val halves = FloatArray(META_BALL_MAX_PARTICLES * 2)
    val corners = FloatArray(META_BALL_MAX_PARTICLES)
    for (i in 0 until count) {
        val particle = particles[i]
        centers[i * 2] = particle.centerX.toPx()
        centers[i * 2 + 1] = particle.centerY.toPx()
        halves[i * 2] = particle.width.toPx() / 2
        halves[i * 2 + 1] = particle.height.toPx() / 2
        corners[i] = particle.cornerRadius.toPx()
    }
    shader.setFloatUniform("in_center", centers)
    shader.setFloatUniform("in_half", halves)
    shader.setFloatUniform("in_corner", corners)
    shader.setFloatUniform("in_count", count.toFloat())
    shader.setFloatUniform("in_extension", extension.toPx())
    shader.setFloatUniform("in_factor", extensionFactorX, extensionFactorY)
    // The body and rim are plain `float4` uniforms set through the float API, not `setColorUniform`:
    // AGSL only accepts the colour API for uniforms declared `layout(color)`, and SkSL rejects that
    // qualifier. Passing the four components directly works on both backends and keeps the shader's
    // straight-alpha arithmetic (rgb * a) valid.
    shader.setFloatUniform("in_body", body.red, body.green, body.blue, body.alpha)
    shader.setFloatUniform("in_rim", rim.red, rim.green, rim.blue, rim.alpha)
    shader.setFloatUniform("in_rimWidth", MetaBallDefaults.RimWidth.toPx())
    shader.setFloatUniform("in_rimBlur", rimBlur.toPx())
    shader.setFloatUniform("in_glow", bottomGlow.red, bottomGlow.green, bottomGlow.blue, bottomGlow.alpha)
    shader.setFloatUniform("in_glowRadius", bottomGlowBlur.toPx())

    drawRect(brush = shader.asBrush(), size = size)
}

/** Degraded path for platforms without runtime shaders: the silhouettes, but not merged. */
private fun DrawScope.drawUnmergedParticles(particles: List<MetaBallParticle>, color: Color) {
    particles.forEach { particle ->
        drawRoundRect(
            color = color,
            topLeft = Offset(
                (particle.centerX - particle.width / 2).toPx(),
                (particle.centerY - particle.height / 2).toPx(),
            ),
            size = Size(particle.width.toPx(), particle.height.toPx()),
            cornerRadius = CornerRadius(particle.cornerRadius.toPx()),
        )
    }
}

/** COUI's melt ball chain is short; the shader has one slot per silhouette. */
private const val META_BALL_MAX_PARTICLES = 8

/**
 * Smooth-union metaball over up to [META_BALL_MAX_PARTICLES] rounded rectangles.
 *
 * `d` accumulates the union through a variable-radius `smin`: the fold radius for each pair is
 * `in_extension * length(in_factor * u)`, where `u` is the unit vector joining the two centres —
 * that is what makes the fillet 1.5× wider than tall. `nearest` tracks the centre of the silhouette
 * that currently owns the accumulated distance so each fold knows its direction; it is refreshed
 * whenever a new silhouette takes the lead.
 *
 * Output is premultiplied and coverage comes from a constant antialiasing band rather than `fwidth`
 * (see `aa` below), so the outline stays one pixel wide at any density.
 */
private val META_BALL_SHADER = """
uniform float2 in_center[8];
uniform float2 in_half[8];
uniform float in_corner[8];
uniform float in_count;
uniform float in_extension;
uniform float2 in_factor;
uniform float4 in_body;
uniform float4 in_rim;
uniform float in_rimWidth;
uniform float in_rimBlur;
uniform float4 in_glow;
uniform float in_glowRadius;

float couiSdRoundBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0, 0.0))) - r;
}

float couiSmin(float a, float b, float k) {
    float kk = max(k, 0.0001);
    float h = clamp(0.5 + 0.5 * (b - a) / kk, 0.0, 1.0);
    return mix(b, a, h) - kk * h * (1.0 - h);
}

half4 main(float2 xy) {
    float d = 1e6;
    float2 nearest = float2(0.0, 0.0);
    float2 nearestHalf = float2(1.0, 1.0);
    float has = 0.0;
    for (int i = 0; i < 8; i++) {
        if (float(i) >= in_count) {
            break;
        }
        float2 c = in_center[i];
        float di = couiSdRoundBox(xy - c, in_half[i], in_corner[i]);
        if (has < 0.5) {
            d = di;
            nearest = c;
            nearestHalf = in_half[i];
            has = 1.0;
        } else {
            float2 delta = c - nearest;
            float len = length(delta);
            float2 u = len > 0.001 ? delta / len : float2(0.7071, 0.7071);
            float k = in_extension * length(in_factor * u);
            float prev = d;
            d = couiSmin(d, di, k);
            if (di < prev) {
                nearest = c;
                nearestHalf = in_half[i];
            }
        }
    }
    if (has < 0.5) {
        return half4(0.0);
    }

    // SkSL(ES2) has no fwidth, dFdx or dFdy: any of them fails the whole shader to compile and
    // silently degrades to the unmerged silhouettes. The SDF gradient magnitude is ~1, so a fixed
    // 0.6 device pixels approximates the one-pixel antialiasing band fwidth would have measured.
    float aa = 0.6;
    float cover = 1.0 - smoothstep(-aa, aa, d);
    float2 rel = (xy - nearest) / max(nearestHalf, float2(1.0, 1.0));
    // The native bottom-bar material sends a small amount of light through the lower edge. It is
    // directional: the upper and side edges stay dark while the light fans out below the shape.
    float outside = step(0.0, d);
    float distanceFade = 1.0 - smoothstep(0.0, max(in_glowRadius, 0.001), d);
    float down = smoothstep(0.0, 1.0, rel.y);
    half glowA = in_glow.w * half(outside * distanceFade * down);
    if (cover <= 0.003 && glowA <= 0.003) {
        return half4(0.0);
    }

    // The rim is the glass highlight, not a drawn outline. COUI lights it with a flat 0.05-alpha
    // stroke plus two directional lights, so the light gathers along the top edge and dies at the
    // sides: on Gallery's accessory bar the top edge sits ~48/255 above the body while the capsule's
    // side edges show no highlight at all. `-d` is the depth inside the merged silhouette, so the
    // mask keeps full strength for `in_rimWidth` and fades out over `in_rimBlur`; `up` is 1 on the
    // top edge of the ownership silhouette and 0 at its vertical centre.
    half4 body = half4(in_body.x, in_body.y, in_body.z, in_body.w);
    half4 rim = half4(in_rim.x, in_rim.y, in_rim.z, in_rim.w);
    float rimMask = smoothstep(in_rimWidth + in_rimBlur, in_rimWidth, -d);
    half srcA = half(0.0);
    if (rimMask > 0.0) {
        float up = clamp(-rel.y, 0.0, 1.0);
        srcA = rim.a * half(rimMask * up * up);
    }
    half4 result = half4(0.0, 0.0, 0.0, 0.0);
    half baseA = (srcA + body.a * (1.0 - srcA)) * half(cover);
    half3 baseRgb = (rim.rgb * srcA + body.rgb * body.a * (1.0 - srcA)) * half(cover);
    result.a = glowA + baseA * (1.0 - glowA);
    half3 glowRgb = half3(in_glow.x, in_glow.y, in_glow.z);
    result.rgb = glowRgb * glowA + baseRgb * (1.0 - glowA);
    return result;
}
"""
