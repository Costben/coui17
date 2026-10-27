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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
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
 * @property cornerRadius The untransformed corner radius; defaults to a full capsule.
 * @property scaleX Horizontal transform of the original silhouette, about its centre.
 * @property scaleY Vertical transform; a stretched circle remains an ellipse.
 * @property alpha Opacity of this member of the material chain.
 */
@Immutable
data class MetaBallParticle(
    val centerX: Dp,
    val centerY: Dp,
    val width: Dp,
    val height: Dp,
    val cornerRadius: Dp = minOf(width, height) / 2,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val alpha: Float = 1f,
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
 * The union is drawn by [META_BALL_SHADER] on every platform, including ColorOS itself: the system
 * material renderer animates its own material whenever the window's overlay state changes, which
 * shrank the blob to 84% for one frame when a dialog opened. Measured against that renderer the
 * shader below draws the resting silhouette within one level, so there is nothing to gain by
 * deferring to it.
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
 * @param rimBlur Retained for source compatibility; native near/far gradients determine the rim fade.
 * @param rimBottomScale Additional weight for the native lower perimeter ramp. `1` preserves the
 *   system's near/far alpha and width difference; `0` suppresses the lower ramp.
 * @param bottomGlowColor A directional light that diffuses below the silhouette, or `null` to skip it.
 *   It uses the native caustic SDF and vertical attenuation, with per-component layout profiles.
 * @param bottomGlowBlur Retained for source compatibility; native caustic profiles determine the falloff.
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
    rimBottomScale: Float = 0f,
    bottomGlowColor: Color? = null,
    bottomGlowBlur: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    MetaBallSurface(
        particles = particles,
        blurColors = null,
        modifier = modifier,
        color = color,
        backdrop = backdrop,
        blurRadius = blurRadius,
        containerAlpha = containerAlpha,
        extension = extension,
        extensionFactorX = extensionFactorX,
        extensionFactorY = extensionFactorY,
        rimColor = rimColor,
        rimBlur = rimBlur,
        rimBottomScale = rimBottomScale,
        bottomGlowColor = bottomGlowColor,
        content = content,
    )
}

/**
 * [MetaBallSurface] whose glass is tinted by [blurColors] instead of one flat [color] layer.
 *
 * A COUI material such as `TYPE_FRAMEWORK_BOTTOM_BAR` tints its blur with blend-mode layers
 * (`ColorDodge`, `Overlay`, `Luminosity`) that keep the backdrop's hue and saturation and only remap
 * its lightness. A `SrcOver` layer of [color] at [containerAlpha] cannot express that: it replaces
 * most of the backdrop with a flat grey. When [blurColors] is non-null it is applied to the blur
 * and [color] / [containerAlpha] only colour the opaque fallback.
 */
@Composable
internal fun MetaBallSurface(
    particles: List<MetaBallParticle>,
    blurColors: List<BlendColorEntry>?,
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
    rimBottomScale: Float = 0f,
    bottomGlowColor: Color? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val runsShader = isRuntimeShaderSupported()
    val shader = rememberMetaBallShader(runsShader)
    val glass = backdrop != null && shader != null

    Box(modifier = modifier) {
        // Both the blur mask and the native material nodes need drawing room outside the resting
        // bar. Reserve paint space without changing the measured size or the caller's coordinates.
        MetaBallPaintArea(particles = particles, modifier = Modifier.matchParentSize()) { paintedParticles ->
            if (bottomGlowColor != null) {
                MaterialCausticShadow(
                    particles = paintedParticles.map {
                        CausticShadowParticle(
                            it,
                            if (it.width == it.height) CausticShadowProfile.Accessory else CausticShadowProfile.Bar,
                        )
                    },
                    color = bottomGlowColor,
                    modifier = Modifier.matchParentSize(),
                )
            }
            if (glass) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .nativeMaterialEdge(
                            particles = paintedParticles,
                            rim = rimColor ?: Color.Transparent,
                            bottomScale = rimBottomScale,
                            tint = if (blurColors == null) color.copy(alpha = containerAlpha) else Color.Transparent,
                            extension = extension,
                            factorX = extensionFactorX,
                            factorY = extensionFactorY,
                        )
                        .textureBlur(
                            backdrop = backdrop,
                            shape = RectangleShape,
                            blurRadius = blurRadius,
                            colors = BlurDefaults.blurColors(blendColors = blurColors.orEmpty()),
                            // The blob is this node's content, so DstIn keeps exactly the blur that
                            // falls inside the merged outline.
                            contentBlendMode = BlendMode.DstIn,
                        ),
                ) {
                    MetaBallSilhouette(
                        particles = paintedParticles,
                        modifier = Modifier.matchParentSize(),
                        shader = shader,
                        body = Color.White,
                        rim = Color.Transparent,
                        extension = extension,
                        factorX = extensionFactorX,
                        factorY = extensionFactorY,
                        rimBlur = rimBlur,
                    )
                }
            } else if (shader != null) {
                MetaBallSilhouette(
                    particles = paintedParticles,
                    modifier = Modifier.matchParentSize().nativeMaterialEdge(
                        particles = paintedParticles,
                        rim = rimColor ?: Color.Transparent,
                        bottomScale = rimBottomScale,
                        extension = extension,
                        factorX = extensionFactorX,
                        factorY = extensionFactorY,
                    ),
                    shader = shader,
                    body = color,
                    rim = Color.Transparent,
                    extension = extension,
                    factorX = extensionFactorX,
                    factorY = extensionFactorY,
                    rimBlur = rimBlur,
                    rimBottomScale = rimBottomScale,
                )
            } else {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawUnmergedParticles(particles = paintedParticles, color = color)
                }
            }
        }
        content()
    }
}

/** A surface can move by 28dp and stretch; its offscreen mask must contain those pixels. */
@Composable
private fun MetaBallPaintArea(
    particles: List<MetaBallParticle>,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(List<MetaBallParticle>) -> Unit,
) {
    val outset = 40.dp
    val outsetPx = with(LocalDensity.current) { outset.roundToPx() }
    val shifted = particles.map { it.copy(centerX = it.centerX + outset, centerY = it.centerY + outset) }
    Layout(modifier = modifier, content = { Box { content(shifted) } }) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val paint = measurables.single().measure(Constraints.fixed(width + 2 * outsetPx, height + 2 * outsetPx))
        layout(width, height) { paint.place(-outsetPx, -outsetPx) }
    }
}

@Composable
private fun MetaBallSilhouette(
    particles: List<MetaBallParticle>,
    shader: RuntimeShader,
    body: Color,
    rim: Color,
    extension: Dp,
    factorX: Float,
    factorY: Float,
    rimBlur: Dp,
    modifier: Modifier = Modifier,
    rimBottomScale: Float = 0f,
) {
    // Always the runtime shader, never the platform's `OplusMaterialUtil` chain: the vendor effect
    // re-runs its own material animation whenever the window's overlay state changes, so opening a
    // dialog shrank the whole silhouette to 84% of its size for one frame and sprang it back over
    // ~160ms. Sampled frame by frame on the device, the shader below draws the resting silhouette
    // within one level of the vendor chain, so the chain bought nothing and cost that blink.
    Canvas(modifier = modifier) {
        drawMetaBall(
            shader, particles, body, rim, extension, factorX, factorY, rimBlur,
            rimBottomScale = rimBottomScale,
        )
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
    rimBottomScale: Float = 0f,
) {
    setMetaBallUniforms(shader, particles, body, rim, extension, extensionFactorX, extensionFactorY, rimBlur, rimBottomScale)
    drawRect(brush = shader.asBrush())
}

internal fun Density.setMetaBallUniforms(
    shader: RuntimeShader,
    particles: List<MetaBallParticle>,
    body: Color,
    rim: Color,
    extension: Dp,
    extensionFactorX: Float,
    extensionFactorY: Float,
    rimBlur: Dp,
    rimBottomScale: Float,
    edgeStyle: GradientStrokeStyle = GradientStrokeStyle.EDGE_STYLE_0,
    lineWidth: Dp = 2.5.dp,
) {
    val count = minOf(particles.size, META_BALL_MAX_PARTICLES)
    if (count == 0) return

    val centers = FloatArray(META_BALL_MAX_PARTICLES * 2)
    val halves = FloatArray(META_BALL_MAX_PARTICLES * 2)
    val corners = FloatArray(META_BALL_MAX_PARTICLES)
    val scales = FloatArray(META_BALL_MAX_PARTICLES * 2) { 1f }
    val alphas = FloatArray(META_BALL_MAX_PARTICLES) { 1f }
    val edgeArrays = FloatArray(META_BALL_MAX_PARTICLES * 16)
    for (i in 0 until count) {
        val particle = particles[i]
        centers[i * 2] = particle.centerX.toPx()
        centers[i * 2 + 1] = particle.centerY.toPx()
        halves[i * 2] = particle.width.toPx() / 2
        halves[i * 2 + 1] = particle.height.toPx() / 2
        corners[i] = particle.cornerRadius.toPx()
        scales[i * 2] = particle.scaleX.coerceAtLeast(0.001f)
        scales[i * 2 + 1] = particle.scaleY.coerceAtLeast(0.001f)
        alphas[i] = particle.alpha.coerceIn(0f, 1f)
        GradientStrokeStyleMapper.toEdgeArray16(
            style = edgeStyle,
            width = particle.width.toPx(),
            height = particle.height.toPx(),
            lineWidth = lineWidth.toPx(),
            lineAlpha = rim.alpha,
            lineAngle = 0f,
            density = density,
        ).copyInto(edgeArrays, i * 16)
    }
    shader.setFloatUniform("in_center", centers)
    shader.setFloatUniform("in_half", halves)
    shader.setFloatUniform("in_corner", corners)
    shader.setFloatUniform("in_scale", scales)
    shader.setFloatUniform("in_alpha", alphas)
    shader.setFloatUniform("in_edge", edgeArrays)
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
    shader.setFloatUniform("in_rimBottom", rimBottomScale)
}

/** Degraded path for platforms without runtime shaders: the silhouettes, but not merged. */
private fun DrawScope.drawUnmergedParticles(particles: List<MetaBallParticle>, color: Color) {
    particles.forEach { particle ->
        val centre = Offset(particle.centerX.toPx(), particle.centerY.toPx())
        withTransform({ scale(particle.scaleX, particle.scaleY, pivot = centre) }) {
            drawRoundRect(
                color = color.copy(alpha = color.alpha * particle.alpha),
                topLeft = Offset((particle.centerX - particle.width / 2).toPx(), (particle.centerY - particle.height / 2).toPx()),
                size = Size(particle.width.toPx(), particle.height.toPx()),
                cornerRadius = CornerRadius(particle.cornerRadius.toPx()),
            )
        }
    }
}

/** COUI's melt ball chain is short; the shader has one slot per silhouette. */
private const val META_BALL_MAX_PARTICLES = 8

// SkSL ES2 requires constant uniform indices. Select an owner through branches rather than
// dynamically indexing the native 16-float edge profiles.
private val META_BALL_EDGE_OWNER_DISPATCH = buildString {
    append("float couiOwnerEdgeAlpha(float2 pos, float2 halfSize, float d, int owner) {\n")
    repeat(META_BALL_MAX_PARTICLES) { owner ->
        val offset = owner * 16
        fun slot(index: Int): String = "in_edge[${offset + index}]"
        append("if (owner == $owner) return couiEdgeAlpha(pos, halfSize, d, ")
        append("float2(${slot(3)}, ${slot(4)}), float2(${slot(5)}, ${slot(6)}), ")
        append("float2(${slot(7)}, ${slot(8)}), float4(${slot(9)}, ${slot(10)}, ${slot(11)}, ${slot(12)}), ")
        append("${slot(13)}, ${slot(14)}, ${slot(15)});\n")
    }
    append("return 0.0;\n}\n")
}

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
uniform float2 in_scale[8];
uniform float in_alpha[8];
uniform float in_edge[128];
uniform float in_count;
uniform float in_extension;
uniform float2 in_factor;
uniform float4 in_body;
uniform float4 in_rim;
uniform float in_rimWidth;
uniform float in_rimBlur;
uniform float in_rimBottom;

float couiSdRoundBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0, 0.0))) - r;
}

float couiSmin(float a, float b, float k) {
    float kk = max(k, 0.0001);
    float h = clamp(0.5 + 0.5 * (b - a) / kk, 0.0, 1.0);
    return mix(b, a, h) - kk * h * (1.0 - h);
}

float getDis(float2 pos, float2 halfsize) {
    float2 rect = abs(pos - halfsize);
    return rect.x + rect.y;
}

float couiEdgeAlpha(float2 pos, float2 halfSize, float d, float2 uRectLineNear,
    float2 uRectLineFar, float2 uRectLineAlpha, float4 uRectLineFade, float uAngle,
    float uRectLinePara1, float uRectLinePara2) {
        float2 rectLineNear=uRectLineNear;
        float2 rectLineFar=uRectLineFar;
        float4 rectLineFade=uRectLineFade;

        if(d<-max(rectLineNear.y, rectLineFar.y)){
            return 0.0;
        }

        float distanceAll = (halfSize.x+halfSize.y)*2.0;

        float ratio = fract(uAngle+halfSize.x/distanceAll*0.5+0.5);
        float2 dir_pos= normalize(pos);
        float2 dirAbs_pos = abs(dir_pos);
        float2 pos_n = min(float2(dirAbs_pos.xy*halfSize.yx/max(float2(0.001),dirAbs_pos.yx)),halfSize)*sign(dir_pos);
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

        if(uRectLinePara2 > 0.001){
            lineNearSdf = pow(lineNearSdf, uRectLinePara1)*uRectLinePara2+(1.0-uRectLinePara2)*lineNearSdf;
            lineFarSdf = pow(lineFarSdf, uRectLinePara1)*uRectLinePara2+(1.0-uRectLinePara2)*lineFarSdf;
        }

        lineNearAlpha*=lineNearSdf;
        lineFarAlpha*=lineFarSdf;

        float lineAlpha = clamp(lineNearAlpha+lineFarAlpha,0.,1.);

        return lineAlpha;
}

$META_BALL_EDGE_OWNER_DISPATCH

half4 main(float2 xy) {
    float d = 1e6;
    float2 nearest = float2(0.0, 0.0);
    float2 nearestHalf = float2(1.0, 1.0);
    float2 nearestScale = float2(1.0);
    int owner = 0;
    float has = 0.0;
    float opacity = 0.0;
    for (int i = 0; i < 8; i++) {
        if (float(i) >= in_count) {
            break;
        }
        float2 c = in_center[i];
        float di = couiSdRoundBox((xy - c) / in_scale[i], in_half[i],
            min(in_corner[i], min(in_half[i].x, in_half[i].y)));
        if (has < 0.5) {
            d = di;
            nearest = c;
            nearestHalf = in_half[i];
            nearestScale = in_scale[i];
            owner = i;
            opacity = in_alpha[i];
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
                nearestScale = in_scale[i];
                owner = i;
                opacity = in_alpha[i];
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
    float cover = (1.0 - smoothstep(-aa, aa, d)) * opacity;
    float2 localPos = (xy - nearest) / nearestScale;
    if (cover <= 0.003) return half4(0.0);

    // TYPE_FRAMEWORK_CAPSULE_3 uses the native EDGE_STYLE_0 near/far perimeter ramps.
    // The top is the narrow far line, the bottom the broader near line; both follow the union SDF.
    half4 body = half4(in_body);
    half4 rim = half4(in_rim);
    float bottomWeight = localPos.y > 0.0 ? in_rimBottom : 1.0;
    half srcA = half(couiOwnerEdgeAlpha(float2(localPos.x, -localPos.y), nearestHalf, d, owner) * bottomWeight);
    half4 result = half4(0.0, 0.0, 0.0, 0.0);
    half baseA = (srcA + body.a * (1.0 - srcA)) * half(cover);
    half3 baseRgb = (rim.rgb * srcA + body.rgb * body.a * (1.0 - srcA)) * half(cover);
    result.a = baseA;
    result.rgb = baseRgb;
    return result;
}
"""

/** The same merged outline, evaluated against a sampled material rather than a white overlay. */
internal val META_BALL_NATIVE_EDGE_SHADER = META_BALL_SHADER.substringBefore("half4 main(float2 xy)") + """
uniform shader child;
uniform float4 in_tint;
""" + NATIVE_MATERIAL_EDGE_BLEND + META_BALL_SHADER.substringAfter("$META_BALL_EDGE_OWNER_DISPATCH").substringBefore("    // TYPE_FRAMEWORK_CAPSULE_3") + """
    half4 inputColor = child.eval(xy);
    if (inputColor.a <= 0.000001) return inputColor;
    half3 rawColor = inputColor.rgb / inputColor.a;
    half3 materialColor = mix(rawColor, half3(in_tint.rgb), half(in_tint.a));
    float bottomWeight = localPos.y > 0.0 ? in_rimBottom : 1.0;
    float lineAlpha = couiOwnerEdgeAlpha(float2(localPos.x, -localPos.y), nearestHalf, d, owner) * bottomWeight;
    half3 result = couiNativeEdgeBlend(materialColor, rawColor, half3(in_rim.rgb), lineAlpha);
    // The child already carries silhouette coverage and member alpha: never apply them twice.
    return half4(result * inputColor.a, inputColor.a);
}
"""
