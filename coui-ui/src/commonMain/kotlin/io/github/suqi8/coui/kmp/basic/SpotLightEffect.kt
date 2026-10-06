// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.shader.RuntimeShader
import io.github.suqi8.coui.kmp.shader.asBrush
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported

/**
 * The parameter set of the ColorOS 17 spotlight, the portable port of
 * `COUISpotLightEffect$SpotLightParams`.
 *
 * COUI keeps one of these per component type and animates the light along the preset's own start /
 * end pairs: `COUISpotLightEffect.setProgress` lerps `mStartIntensity` → `mEndIntensity` into the
 * shader's `setIntensity` and `mStartRadius` → `mEndRadius` into its `setLightRadius`, while the
 * remaining fields are pushed once through `setLightHeight` / `setAmbientStrength` /
 * `setDiffuseStrength` / `setNoise` / `setDitherN`. The animation itself is not part of this port:
 * the caller drives [Modifier.spotLight]'s `position`, `radius` and `intensity`, normally on
 * [SpotLightDefaults.MoveSpring].
 *
 * @param blendMode The blend mode of the light layer, `mBlendMode`. The natives use `OVERLAY`, `PLUS`
 *   and `SRC_OVER`; see [Modifier.spotLight] for the mapping onto Compose's [BlendMode].
 * @param startIntensity The intensity the light starts at, `mStartIntensity`.
 * @param endIntensity The intensity the light settles at, `mEndIntensity`.
 * @param startRadius The radius the light starts at, `mStartRadius`. In ColorOS this is a raw
 *   fragment-space value: `COUISpotLightEffectDrawable.onBoundsChange` sets the shader resolution
 *   from `bounds.width() / bounds.height()` in pixels, and `SpotLightShader.setLightRadius` folds the
 *   value into `u_lightPosAndHeight.z` unchanged, so it is compared against distances measured in
 *   those pixels. Convert it with the density before feeding it to [Modifier.spotLight]'s
 *   `radius: Dp`.
 * @param endRadius The radius the light settles at, `mEndRadius`; same unit as [startRadius].
 * @param lightHeight The height of the light source above the surface, `mLightHeight`; it becomes
 *   `u_lightingParams.y` and takes part in the same pixel-space distance as the radius.
 * @param ambientStrength The ambient term, `mAmbientStrength`, `u_lightColorAndAmbient.w`.
 * @param diffuseStrength The diffuse term, `mDiffuseStrength`, `u_lightingParams.x`.
 * @param ditherN The dither amount, `mDitherN`, `u_lightingParams.w`; 0 disables the dither pass.
 * @param noise The noise amount, `mNoise`, which is the shader's `noise_exp`
 *   (`u_lightingParams.z`); 0 disables the noise pass.
 */
@Immutable
data class SpotLightParams(
    val blendMode: BlendMode,
    val startIntensity: Float,
    val endIntensity: Float,
    val startRadius: Float,
    val endRadius: Float,
    val lightHeight: Float,
    val ambientStrength: Float,
    val diffuseStrength: Float,
    val ditherN: Float,
    val noise: Float,
)

/** Defaults for [Modifier.spotLight], the presets and constants of `COUISpotLightEffect`. */
object SpotLightDefaults {

    /** `COUISpotLightEffect.TYPE_TRANSLUCENT_LARGE_1_LIGHT`, used by popup RoundFrameLayout. */
    val TranslucentLarge1Light: SpotLightParams = SpotLightParams(
        blendMode = BlendMode.Overlay,
        startIntensity = 0f,
        endIntensity = 0.6f,
        startRadius = 100f,
        endRadius = 800f,
        lightHeight = 200f,
        ambientStrength = 0.4f,
        diffuseStrength = 0.4f,
        ditherN = 0f,
        noise = 0.08f,
    )

    /** The popup's dark preset retains Overlay and only lowers the light intensity. */
    val TranslucentLarge1Dark: SpotLightParams = TranslucentLarge1Light.copy(endIntensity = 0.1f)

    fun translucentLarge1(isDark: Boolean): SpotLightParams = if (isDark) TranslucentLarge1Dark else TranslucentLarge1Light

    /**
     * `SpotLightShader.mLightHeight` — the height of the light above the surface, in the shader's
     * pixel space like [SpotLightParams.lightHeight]. [Modifier.spotLight] takes a `Dp`, so its
     * default is `LightHeight.dp` and a preset's raw value is converted with the density.
     */
    val LightHeight = 200f

    /** `SpotLightShader.mAmbientStrength` — the ambient share of the lighting. */
    val AmbientStrength = 0.1f

    /**
     * `SpotLightShader.DEFAULT_DIFFUSE_STRENGTH` (the initial value of `mDiffuseStrength`) — the
     * diffuse share of the lighting.
     */
    val DiffuseStrength = 0.5f

    /** `SpotLightShader.mLightRadius` — the light radius the shader starts with. */
    val LightRadius = 200f

    /** `SpotLightShader.mLevel` — the multiplier applied to both the radius and the intensity. */
    val Level = 1f

    /**
     * `COUISpotLightEffect.mDiffuseExtinguishExtraRadius` = `400.0f * 2.0f` — the extra radius the
     * extinguish animation overshoots to, folded into the radius lerp by `setProgress`.
     */
    val ExtinguishExtraRadius = 800f

    /** `COUISpotLightEffect.CAPSULE_PARAMS_1_LIGHT` — a capsule in the light theme. */
    val CapsuleParams1Light: SpotLightParams = SpotLightParams(
        blendMode = BlendMode.Overlay,
        startIntensity = 0f,
        endIntensity = 1f,
        startRadius = 100f,
        endRadius = 400f,
        lightHeight = 80f,
        ambientStrength = 0.1f,
        diffuseStrength = 0.5f,
        ditherN = 0f,
        noise = 0f,
    )

    /** `COUISpotLightEffect.CAPSULE_PARAMS_1_DARK` — a capsule in the dark theme. */
    val CapsuleParams1Dark: SpotLightParams = SpotLightParams(
        blendMode = BlendMode.Overlay,
        startIntensity = 0f,
        endIntensity = 1f,
        startRadius = 100f,
        endRadius = 400f,
        lightHeight = 80f,
        ambientStrength = 0.1f,
        diffuseStrength = 0.5f,
        ditherN = 0f,
        noise = 0f,
    )

    /** `COUISpotLightEffect.CAPSULE_PARAMS_2_LIGHT` — a softer capsule with noise and dither. */
    val CapsuleParams2Light: SpotLightParams = SpotLightParams(
        blendMode = BlendMode.Overlay,
        startIntensity = 0f,
        endIntensity = 1f,
        startRadius = 100f,
        endRadius = 500f,
        lightHeight = 200f,
        ambientStrength = 0.4f,
        diffuseStrength = 0.4f,
        ditherN = 1f,
        noise = 0.3f,
    )

    /** `COUISpotLightEffect.CAPSULE_PARAMS_2_DARK` — a softer capsule with noise, dark theme. */
    val CapsuleParams2Dark: SpotLightParams = SpotLightParams(
        blendMode = BlendMode.Plus,
        startIntensity = 0f,
        endIntensity = 0.15f,
        startRadius = 100f,
        endRadius = 500f,
        lightHeight = 200f,
        ambientStrength = 0.4f,
        diffuseStrength = 0.4f,
        ditherN = 0f,
        noise = 0.1f,
    )

    /** `COUISpotLightEffect.TYPE_OPAQUE_MEDIUM_2`, used by the COS17 bottom navigation capsule. */
    val OpaqueMedium2Light: SpotLightParams = SpotLightParams(
        blendMode = BlendMode.Overlay,
        startIntensity = 0f,
        endIntensity = 1f,
        startRadius = 100f,
        endRadius = 500f,
        lightHeight = 200f,
        ambientStrength = 0.4f,
        diffuseStrength = 0.4f,
        ditherN = 0f,
        noise = 0.08f,
    )

    val OpaqueMedium2Dark: SpotLightParams = OpaqueMedium2Light.copy(
        blendMode = BlendMode.Plus,
        endIntensity = 0.15f,
    )

    /** `COUISpotLightEffect.CIRCLE_PARAMS_1_LIGHT` — a circular light, added on top. */
    val CircleParams1Light: SpotLightParams = SpotLightParams(
        blendMode = BlendMode.Plus,
        startIntensity = 0f,
        endIntensity = 0.2f,
        startRadius = 100f,
        endRadius = 600f,
        lightHeight = 200f,
        ambientStrength = 0.1f,
        diffuseStrength = 0.5f,
        ditherN = 0f,
        noise = 0f,
    )

    /** `COUISpotLightEffect.CIRCLE_PARAMS_1_DARK` — a circular light, added on top, dark theme. */
    val CircleParams1Dark: SpotLightParams = SpotLightParams(
        blendMode = BlendMode.Plus,
        startIntensity = 0f,
        endIntensity = 0.2f,
        startRadius = 100f,
        endRadius = 600f,
        lightHeight = 200f,
        ambientStrength = 0.1f,
        diffuseStrength = 0.5f,
        ditherN = 0f,
        noise = 0f,
    )

    /** `COUISpotLightEffect.ROUNDRECT_PARAMS_1_LIGHT` — a large rounded-rectangle light. */
    val RoundRectParams1Light: SpotLightParams = SpotLightParams(
        blendMode = BlendMode.Plus,
        startIntensity = 0f,
        endIntensity = 1.3f,
        startRadius = 100f,
        endRadius = 600f,
        lightHeight = 120f,
        ambientStrength = 0.2f,
        diffuseStrength = 0.2f,
        ditherN = 1f,
        noise = 0.08f,
    )

    /**
     * `COUISpotLightEffect.ROUNDRECT_PARAMS_1_DARK` — the only preset whose radius range starts at
     * 0, i.e. the light grows from nothing.
     */
    val RoundRectParams1Dark: SpotLightParams = SpotLightParams(
        blendMode = BlendMode.Plus,
        startIntensity = 0f,
        endIntensity = 0.4f,
        startRadius = 0f,
        endRadius = 600f,
        lightHeight = 100f,
        ambientStrength = 0.1f,
        diffuseStrength = 0.5f,
        ditherN = 1f,
        noise = 0.02f,
    )

    /** `COUISpotLightEffect.SWITCH_PARAMS_LIGHT` — the small switch light. */
    val SwitchParamsLight: SpotLightParams = SpotLightParams(
        blendMode = BlendMode.Overlay,
        startIntensity = 0f,
        endIntensity = 0.8f,
        startRadius = 100f,
        endRadius = 200f,
        lightHeight = 100f,
        ambientStrength = 0.1f,
        diffuseStrength = 0.5f,
        ditherN = 0f,
        noise = 0f,
    )

    /** `COUISpotLightEffect.SWITCH_PARAMS_DARK` — the small switch light, dark theme. */
    val SwitchParamsDark: SpotLightParams = SpotLightParams(
        blendMode = BlendMode.Overlay,
        startIntensity = 0f,
        endIntensity = 0.8f,
        startRadius = 100f,
        endRadius = 200f,
        lightHeight = 100f,
        ambientStrength = 0.1f,
        diffuseStrength = 0.5f,
        ditherN = 0f,
        noise = 0f,
    )

    /** `COUISpotLightEffect.CHIP_TAB_PARAMS_LIGHT` — the light theme chip tab. */
    val ChipTabParamsLight: SpotLightParams = SpotLightParams(
        blendMode = BlendMode.Plus,
        startIntensity = 0f,
        endIntensity = 0.2f,
        startRadius = 100f,
        endRadius = 600f,
        lightHeight = 200f,
        ambientStrength = 0.1f,
        diffuseStrength = 0.5f,
        ditherN = 0f,
        noise = 0f,
    )

    /**
     * `COUISpotLightEffect.CHIP_TAB_PARAMS_DARK` — the dark theme chip tab, and the only preset that
     * composites with `SRC_OVER` instead of `OVERLAY` / `PLUS`.
     */
    val ChipTabParamsDark: SpotLightParams = SpotLightParams(
        blendMode = BlendMode.SrcOver,
        startIntensity = 0f,
        endIntensity = 0.3f,
        startRadius = 100f,
        endRadius = 600f,
        lightHeight = 200f,
        ambientStrength = 0.1f,
        diffuseStrength = 0.5f,
        ditherN = 0f,
        noise = 0f,
    )

    /**
     * The glow spring: `LIGHT_GLOW_RESPONSE` (0.15) with `LIGHT_GLOW_BOUNCE` (0), i.e. a critically
     * damped spring. ColorOS cuts it at the framework's default visible change; only the move spring
     * has its own threshold.
     */
    val GlowSpring: SpringSpec<Float> = folmeSpring(damping = 1f, response = 0.15f)

    /**
     * The extinguish spring: `LIGHT_EXTINGUISH_RESPONSE` (0.4) with `LIGHT_EXTINGUISH_BOUNCE` (0),
     * also critically damped.
     */
    val ExtinguishSpring: SpringSpec<Float> = folmeSpring(damping = 1f, response = 0.4f)

    /**
     * The follow-the-finger move spring: `LIGHT_MOVE_RESPONSE` (0.35) with `LIGHT_MOVE_BOUNCE`
     * (0.15), cut at `LIGHT_MOVE_MIN_VISIBLE_CHANGE` of `1.0E-4f`.
     */
    val MoveSpring: SpringSpec<Float> = folmeSpring(
        damping = 0.85f,
        response = 0.35f,
        visibilityThreshold = 1.0E-4f,
    )

    /** Resolves `CAPSULE_PARAMS_1_LIGHT` / `_DARK` by night mode. */
    fun capsuleParams1(isDark: Boolean): SpotLightParams = if (isDark) CapsuleParams1Dark else CapsuleParams1Light

    /** Resolves `CAPSULE_PARAMS_2_LIGHT` / `_DARK` by night mode. */
    fun capsuleParams2(isDark: Boolean): SpotLightParams = if (isDark) CapsuleParams2Dark else CapsuleParams2Light

    fun opaqueMedium2(isDark: Boolean): SpotLightParams = if (isDark) OpaqueMedium2Dark else OpaqueMedium2Light

    /** Resolves `CIRCLE_PARAMS_1_LIGHT` / `_DARK` by night mode. */
    fun circleParams1(isDark: Boolean): SpotLightParams = if (isDark) CircleParams1Dark else CircleParams1Light

    /** Resolves `ROUNDRECT_PARAMS_1_LIGHT` / `_DARK` by night mode. */
    fun roundRectParams1(isDark: Boolean): SpotLightParams = if (isDark) RoundRectParams1Dark else RoundRectParams1Light

    /** Resolves `SWITCH_PARAMS_LIGHT` / `_DARK` by night mode. */
    fun switchParams(isDark: Boolean): SpotLightParams = if (isDark) SwitchParamsDark else SwitchParamsLight

    /** Resolves `CHIP_TAB_PARAMS_LIGHT` / `_DARK` by night mode. */
    fun chipTabParams(isDark: Boolean): SpotLightParams = if (isDark) ChipTabParamsDark else ChipTabParamsLight
}

/**
 * Paints the ColorOS 17 spotlight over this node's bounds.
 *
 * ColorOS treats the light as a real light source rather than a radial gradient: the shader builds
 * the direction vector from the fragment to the light, which sits [lightHeight] above the surface,
 * and shades with `(ambient + diffuse * lambert) * lightColor`, attenuated by
 * `smoothstep(1, 0, dist / radius)` so the falloff dies exactly at [radius]. The result is drawn with
 * the preset's own blend mode — `OVERLAY` for the capsules and the switch, `PLUS` for the circle and
 * the round rectangle, `SRC_OVER` for the dark chip tab — over whatever the callers have already
 * painted, so the light brightens the surface underneath it instead of replacing it.
 *
 * [position] is normalised to the node's size, exactly like `COUISpotLightEffectDrawable`, which
 * divides the touch point by `mBounds.width()` / `mBounds.height()` without clamping. Coordinates
 * may leave 0..1: the owner's clip shows only the part of the halo still inside it, and a distant
 * light no longer intersects the surface. [radius] and [lightHeight] are
 * [Dp] because the shader works in the node's pixels, whereas the native presets store those two in
 * raw pixels (see [SpotLightParams.startRadius]).
 *
 * The spring that drives the light is not part of this modifier: animate [position], [radius] and
 * [intensity] with [SpotLightDefaults.GlowSpring], [SpotLightDefaults.ExtinguishSpring] and
 * [SpotLightDefaults.MoveSpring] the way `COUISpotLightEffect.animate` / `glow` / `extinguish` do.
 *
 * @param position The light position normalised to this node; values outside 0..1 are valid.
 * @param radius The radius at which the light dies out, `u_lightPosAndHeight.z`.
 * @param intensity The intensity multiplier, `u_lightPosAndHeight.w`.
 * @param modifier Modifiers applied outside the light layer; the light is painted behind the inner
 *   content of the returned chain.
 * @param lightColor The light colour, `SpotLightShader.mLightColor` (white in every preset).
 * @param lightHeight The height of the light above the surface, `u_lightingParams.y`.
 * @param ambientStrength The ambient term, `u_lightColorAndAmbient.w`.
 * @param diffuseStrength The diffuse term, `u_lightingParams.x`.
 * @param noise The noise amount, `u_lightingParams.z`; 0 disables the noise pass.
 * @param dither The dither amount, `u_lightingParams.w`; 0 disables the dither pass.
 * @param blendMode How the light composites onto the surface; a preset's
 *   [SpotLightParams.blendMode] belongs here.
 * @param enabled Whether the light is painted at all; when false the modifier is a no-op and the
 *   native path's `setProgress(0)` behaviour of a zero radius is left to the caller.
 */
@Composable
fun Modifier.spotLight(
    position: Offset,
    radius: Dp,
    intensity: Float,
    modifier: Modifier = Modifier,
    lightColor: Color = Color.White,
    lightHeight: Dp = SpotLightDefaults.LightHeight.dp,
    ambientStrength: Float = SpotLightDefaults.AmbientStrength,
    diffuseStrength: Float = SpotLightDefaults.DiffuseStrength,
    noise: Float = 0f,
    dither: Float = 0f,
    blendMode: BlendMode = BlendMode.SrcOver,
    enabled: Boolean = true,
    clipShape: Shape? = null,
): Modifier {
    // A runtime shader is compiled by the driver on first use, so a source the platform rejects
    // would otherwise throw mid-composition; the fallback keeps the caller rendering.
    val shader = if (enabled) rememberSpotLightShader(isRuntimeShaderSupported()) else null
    if (shader == null) return modifier.then(this)

    return modifier.then(this).drawWithCache {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@drawWithCache onDrawBehind {}
        val radiusPx = radius.toPx()

        shader.setFloatUniform("resolution", width, height)
        // u_lightPosAndHeight = [position.x * width, position.y * height, radius * level, intensity * level]
        shader.setFloatUniform(
            "u_lightPosAndHeight",
            position.x * width,
            position.y * height,
            radiusPx * SpotLightDefaults.Level,
            intensity * SpotLightDefaults.Level,
        )
        shader.setFloatUniform(
            "u_lightColorAndAmbient",
            lightColor.red,
            lightColor.green,
            lightColor.blue,
            ambientStrength,
        )
        shader.setFloatUniform(
            "u_lightingParams",
            diffuseStrength,
            lightHeight.toPx(),
            noise,
            dither,
        )
        val lightBrush = shader.asBrush()
        val lightPath = clipShape?.createOutline(size, layoutDirection, this)?.toPath()
        onDrawBehind {
            if (lightPath == null) {
                drawRect(brush = lightBrush, size = size, blendMode = blendMode)
            } else {
                clipPath(lightPath) {
                    drawRect(brush = lightBrush, size = size, blendMode = blendMode)
                }
            }
        }
    }
}

private fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}

/** Compiles (and remembers) the spotlight shader; `null` when runtime shaders are unavailable. */
@Composable
private fun rememberSpotLightShader(enabled: Boolean): RuntimeShader? {
    if (!enabled) return null
    return remember { runCatching { RuntimeShader(SPOT_LIGHT_SHADER) }.getOrNull() }
}

/**
 * The ColorOS 17 `SpotLight` runtime shader, with only the SkSL dialect rename applied:
 * `vec2/vec3/vec4` → `float2/float3/float4`. The uniform names and packing, the `luminance`, `max3`, `noise`,
 * `softLight` and `dither` helpers, the early-out `dist >= radius` branch, the
 * `smoothstep(1.0, 0.0, dist / radius)` attenuation, the `1.5` fixed light intensity and the
 * `step`-gated noise / dither post pass are unchanged, as are the numbers `9.259e-4` (`1 / 1080`),
 * `127.1 / 311.7` and `43758.5453`.
 *
 * The one spelling change beyond the vector renames is the final `half4(lighting, alpha)`, written
 * as four scalars: SkSL narrows each `float` component to `half`, which a mixed vector / scalar
 * constructor would not do portably.
 *
 * The `SpotLightType` enum, `getParamsByType`, `COUISpotLightEffectDrawable`,
 * `SpotLightEffectRenderer` (the seekbar-only renderer), the deprecated no-op setters (`setEnableHDR`,
 * `setFalloffExponent`, `setFalloffType`, `setShininess`, `setSpecularStrength`,
 * `setLightIntensity`) and the touch-gesture driver are not ported; see
 * [SpotLightDefaults] for the presets that replace the type table.
 */
private val SPOT_LIGHT_SHADER = """
uniform float4 u_lightPosAndHeight;
uniform float4 u_lightColorAndAmbient;
uniform float4 u_lightingParams;
uniform float2 resolution;

float luminance(float3 c) { return dot(c, float3(0.299, 0.587, 0.114)); }

float max3(float3 v) { return max(max(v.r, v.g), v.b); }

// 简化的噪声函数
float noise(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
}

float softLight(float A, float B) {
    float l = B - (1.0 - 2.0 * A) * B * (1.0 - B);
    float h = B + (2.0 * A - 1.0) * (sqrt(B) - B);
    return mix(l, h, step(0.5, A));
}

// 优化的 dither 函数（保留原始双哈希算法）
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

    // 后处理总开关（step 替代提前返回分支，避免 warp divergence）
    float postOn = max(step(0.001, noise_exp), step(0.001, ditherN));
    float3 postLighting = lighting;
    float postAlpha = alpha;

    // 噪声处理（step 替代分支）
    float noiseOn = step(0.001, noise_exp);
    float2 uv_pos = fragCoord * 9.259e-4;
    float n = clamp((noise(uv_pos) - 0.5) * noise_exp + 0.5, 0.0, 1.0);
    float mask = max3(postLighting);
    float fadeAlpha = clamp(softLight(n, mask), 0.0, 1.0);
    float noiseDelta = clamp(abs(fadeAlpha - mask), 0.0, 1.0);
    postLighting += float3(noiseDelta) * noiseOn;

    // 抖动处理（step + mix 替代分支）
    float ditherOn = step(0.001, ditherN);
    float d = dither(fragCoord, ditherN);
    postLighting += float3(d) * ditherOn;
    postAlpha = mix(postAlpha, clamp(postAlpha + d, 0.0, 1.0), ditherOn);

    lighting = mix(lighting, postLighting, postOn);
    alpha = mix(alpha, postAlpha, postOn);

    return half4(lighting.x, lighting.y, lighting.z, alpha);
}
"""
