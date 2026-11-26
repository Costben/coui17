// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.effect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarDefaults
import io.github.suqi8.coui.kmp.basic.FloatingActionButtonDefaults
import io.github.suqi8.coui.kmp.basic.MetaBallParticle
import io.github.suqi8.coui.kmp.basic.MetaBallSurface
import io.github.suqi8.coui.kmp.basic.SpotLightDefaults
import io.github.suqi8.coui.kmp.basic.causticShadowLight
import io.github.suqi8.coui.kmp.basic.materialShadowEdge
import io.github.suqi8.coui.kmp.basic.spotLight
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
import io.github.suqi8.coui.kmp.blur.BlurColors
import io.github.suqi8.coui.kmp.blur.ProgressiveBlur
import io.github.suqi8.coui.kmp.blur.highlight.BloomStroke
import io.github.suqi8.coui.kmp.blur.highlight.Highlight
import io.github.suqi8.coui.kmp.blur.highlight.HighlightStyle
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.progressiveTextureBlur
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.blur.textureBlur
import io.github.suqi8.coui.kmp.shader.RuntimeShader
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported
import kotlinx.coroutines.delay
import kotlin.concurrent.Volatile

private const val WARMUP_STAGE_COUNT = 7
private const val WARMUP_PATCH_PX = 4
private const val WARMUP_PROGRESSIVE_PATCH_PX = 16
private const val WARMUP_ALPHA = 0.004f
private const val WARMUP_INITIAL_DELAY_MS = 250L
private const val WARMUP_STAGE_GAP_MS = 40L

@Volatile
private var couiMetalWarmupClaimed = false

@Volatile
private var couiMetalWarmupFinished = false

/**
 * Deferred, low-impact one-shot GPU shader warmup for Skiko/Metal.
 *
 * Waits until after initial launch frames settle, then submits tiny 4×4 draws at imperceptible
 * alpha (1/255) spaced across separate frames for only the 5 high-frequency Metal pipelines
 * (unified blur, progressive blur, search decal motion blur, standalone floating glass, and
 * unified metaball glass + caustic). Unmounts completely once done.
 */
@Composable
internal fun CouiMetalShaderWarmup(forceRunForTest: Boolean = false) {
    if (!isRuntimeShaderSupported()) return
    val ownsSession = remember(forceRunForTest) {
        if (forceRunForTest) {
            true
        } else if (!couiMetalWarmupClaimed && !couiMetalWarmupFinished) {
            couiMetalWarmupClaimed = true
            true
        } else {
            false
        }
    }
    if (!ownsSession) return

    var stage by remember { mutableIntStateOf(-1) }
    if (stage >= WARMUP_STAGE_COUNT) return

    DisposableEffect(Unit) {
        onDispose {
            if (!forceRunForTest && stage < WARMUP_STAGE_COUNT) {
                couiMetalWarmupClaimed = false
            }
        }
    }

    LaunchedEffect(Unit) {
        repeat(2) { withFrameNanos {} }
        if (!forceRunForTest) {
            delay(WARMUP_INITIAL_DELAY_MS)
        }
        for (current in 0 until WARMUP_STAGE_COUNT) {
            stage = current
            withFrameNanos {}
            if (!forceRunForTest && current + 1 < WARMUP_STAGE_COUNT) {
                delay(WARMUP_STAGE_GAP_MS)
            }
        }
        if (!forceRunForTest) {
            couiMetalWarmupFinished = true
        }
        stage = WARMUP_STAGE_COUNT
    }

    if (stage < 0) return

    val warmupBackdrop = rememberLayerBackdrop()
    val warmupLayer = rememberGraphicsLayer()
    val warmupRenderer = remember { CouiMetalWarmupShaders() }

    // Report 0×0 layout size so placing this inside COUITheme never perturbs enclosing layouts.
    Layout(
        content = {
            if (stage == 0 || stage == 1) {
                val patchDp = if (stage == 1) WARMUP_PROGRESSIVE_PATCH_PX.dp else WARMUP_PATCH_PX.dp
                Box(
                    Modifier
                        .size(patchDp)
                        .layerBackdrop(warmupBackdrop)
                        .background(Color(0xFF808080)),
                )
                if (stage == 0) {
                    Box(
                        Modifier
                            .size(WARMUP_PATCH_PX.dp)
                            .textureBlur(
                                backdrop = warmupBackdrop,
                                shape = CircleShape,
                                blurRadius = 12f,
                                colors = BlurColors(listOf(BlendColorEntry(Color.White.copy(alpha = 0.4f)))),
                                highlight = Highlight(style = HighlightStyle.Default),
                            ),
                    )
                } else {
                    // Warm both plain progressive blur (DetailBarGlassSurface / TopAppBar) and
                    // post-effect + bloom progressive blur at 16px so sharpOverlayActive compiles too.
                    Box(
                        Modifier
                            .size(WARMUP_PROGRESSIVE_PATCH_PX.dp)
                            .progressiveTextureBlur(
                                backdrop = warmupBackdrop,
                                shape = RectangleShape,
                                blurRadius = 24f,
                                gradient = ProgressiveBlur(
                                    angle = 90f,
                                    startFraction = 0.2f,
                                    endFraction = 1f,
                                    curve = 2f,
                                ),
                            ),
                    )
                    Box(
                        Modifier
                            .size(WARMUP_PATCH_PX.dp)
                            .progressiveTextureBlur(
                                backdrop = warmupBackdrop,
                                shape = RectangleShape,
                                blurRadius = 24f,
                                gradient = ProgressiveBlur(
                                    angle = 90f,
                                    startFraction = 0.2f,
                                    endFraction = 1f,
                                    curve = 2f,
                                ),
                                colors = BlurColors(
                                    listOf(BlendColorEntry(Color.White.copy(alpha = 0.4f), BlurBlendMode.Luminosity)),
                                ),
                                highlight = Highlight(style = BloomStroke(dualPeak = true)),
                            ),
                    )
                }
            } else if (stage == 5) {
                // Warm MaterialShadowEdge + MaterialCausticShadow + SpotLight (both Overlay and Plus blends).
                Box(
                    Modifier
                        .size(WARMUP_PATCH_PX.dp)
                        .causticShadowLight(
                            color = Color.White.copy(alpha = 0.2f),
                            profile = FloatingActionButtonDefaults.CausticProfile,
                            cornerRadius = 2.dp,
                            alpha = { 1f },
                        )
                        .materialShadowEdge(
                            stroke = FloatingActionButtonDefaults.EdgeStroke,
                            cornerRadius = 2.dp,
                        )
                        .spotLight(
                            position = Offset(0.5f, 0.5f),
                            radius = 12.dp,
                            intensity = 0.5f,
                            blendMode = SpotLightDefaults.OpaqueMedium2Light.blendMode,
                            enabled = true,
                        )
                        .spotLight(
                            position = Offset(0.5f, 0.5f),
                            radius = 12.dp,
                            intensity = 0.2f,
                            blendMode = SpotLightDefaults.OpaqueMedium2Dark.blendMode,
                            enabled = true,
                        ),
                )
            } else if (stage == 6) {
                // Warm multi-particle MetaBallSurface + bottom bar light/dark blurColors + nativeMaterialEdge
                // used by BottomFloatingAccessoryNavigationBar (capsule + playback orb) & SearchBar.
                Box(
                    Modifier
                        .size(WARMUP_PROGRESSIVE_PATCH_PX.dp)
                        .layerBackdrop(warmupBackdrop)
                        .background(Color(0xFF808080)),
                )
                val warmupParticles = listOf(
                    MetaBallParticle(
                        centerX = 5.dp,
                        centerY = 8.dp,
                        width = 8.dp,
                        height = 4.dp,
                        cornerRadius = 2.dp,
                    ),
                    MetaBallParticle(
                        centerX = 12.dp,
                        centerY = 8.dp,
                        width = 4.dp,
                        height = 4.dp,
                        cornerRadius = 2.dp,
                    ),
                )
                MetaBallSurface(
                    particles = warmupParticles,
                    modifier = Modifier.size(WARMUP_PROGRESSIVE_PATCH_PX.dp),
                    backdrop = warmupBackdrop,
                    blurColors = BottomFloatingNavigationBarDefaults.BlurColorsLight,
                    blurRadius = 12f,
                    rimColor = Color.White.copy(alpha = 0.4f),
                    bottomGlowColor = Color.White.copy(alpha = 0.2f),
                    continuousCurve = true,
                )
                MetaBallSurface(
                    particles = warmupParticles,
                    modifier = Modifier.size(WARMUP_PROGRESSIVE_PATCH_PX.dp),
                    backdrop = warmupBackdrop,
                    blurColors = BottomFloatingNavigationBarDefaults.BlurColorsDark,
                    blurRadius = 12f,
                    rimColor = Color.White.copy(alpha = 0.8f),
                    bottomGlowColor = Color.White.copy(alpha = 0.2f),
                    continuousCurve = true,
                )
            }
        },
        modifier = Modifier.drawBehind {
            when (stage) {
                2 -> {
                    // Warm all Gaussian blur sigma buckets used by TopAppBarSearch & TopAppBarSearchPair.
                    drawWarmupEffect(warmupLayer, warmupRenderer.decalMotionBlurEffect(4f))
                    drawWarmupEffect(warmupLayer, warmupRenderer.decalMotionBlurEffect(12f))
                    drawWarmupEffect(warmupLayer, warmupRenderer.decalMotionBlurEffect(24f))
                    drawWarmupEffect(warmupLayer, warmupRenderer.decalMotionBlurEffect(32f))
                }

                3 -> {
                    drawWarmupEffect(warmupLayer, warmupRenderer.floatingStandaloneEffect())
                    drawWarmupEffect(warmupLayer, warmupRenderer.causticEffect())
                }

                4 -> {
                    drawWarmupEffect(warmupLayer, warmupRenderer.floatingMetaballEffect())
                    drawWarmupEffect(warmupLayer, warmupRenderer.gradientBlurEffect())
                }
            }
        },
    ) { measurables, _ ->
        val sizePx = if (stage == 1 || stage == 6) WARMUP_PROGRESSIVE_PATCH_PX else WARMUP_PATCH_PX
        val fixed = Constraints.fixed(sizePx, sizePx)
        val placeables = measurables.map { it.measure(fixed) }
        layout(0, 0) {
            placeables.forEach { placeable ->
                placeable.placeWithLayer(0, 0) {
                    alpha = WARMUP_ALPHA
                }
            }
        }
    }
}

private fun DrawScope.drawWarmupEffect(
    layer: GraphicsLayer,
    effect: RenderEffect?,
) {
    if (effect == null) return
    val patchSize = IntSize(WARMUP_PATCH_PX, WARMUP_PATCH_PX)
    layer.compositingStrategy = CompositingStrategy.Offscreen
    layer.renderEffect = effect
    layer.alpha = WARMUP_ALPHA
    layer.setRectOutline(Offset.Zero, Size(WARMUP_PATCH_PX.toFloat(), WARMUP_PATCH_PX.toFloat()))
    layer.clip = true
    layer.record(size = patchSize) {
        drawRect(Color(0xFF808080), size = Size(WARMUP_PATCH_PX.toFloat(), WARMUP_PATCH_PX.toFloat()))
    }
    drawLayer(layer)
}

private class CouiMetalWarmupShaders {
    fun decalMotionBlurEffect(radius: Float = 12f): RenderEffect? = runCatching {
        createBlurRenderEffect(radius, radius, TileMode.Decal)
    }.getOrNull()

    fun floatingStandaloneEffect(): RenderEffect? = runCatching {
        val shader = RuntimeShader(couiFloatingStandaloneShader())
        configureFloatingCommon(shader, standalone = true)
        createChainRenderEffect(
            shader.asComposeRenderEffect("u_originShader"),
            floatingMaterialInput(),
        )
    }.getOrNull()

    fun floatingMetaballEffect(): RenderEffect? = runCatching {
        val shader = RuntimeShader(couiFloatingShader())
        configureFloatingCommon(shader, standalone = false)
        createChainRenderEffect(
            shader.asComposeRenderEffect("u_originShader"),
            floatingMaterialInput(),
        )
    }.getOrNull()

    fun causticEffect(): RenderEffect? = runCatching {
        val shader = RuntimeShader(CouiCausticShader).apply {
            setIntUniform("u_foregroundComposition", 1)
            setFloatUniform("u_offset", 0f, 0f)
            setFloatUniform("u_invScale", 1f, 1f)
            setFloatUniform("u_commonArray", floatArrayOf(0f, 0f, 0f, 0f, 4f, 4f, 2f, 0f, 0f, 0f, 0f, 0f))
            setFloatUniform(
                "u_pyrolysisArray",
                floatArrayOf(1f, 1f, 1f, 0.3f, 0f, 0f, 0f, 0f, 2f, 0.6f, 0.6f, 18f, 0.45f, 0.6f),
            )
        }
        shader.asComposeRenderEffect("u_originShader")
    }.getOrNull()

    fun gradientBlurEffect(): RenderEffect? = runCatching {
        val shader = RuntimeShader(gradientGaussianShader()).apply {
            setFloatUniform("inputSize", 4f, 4f)
            setFloatUniform("gradientTop", 0f)
            setFloatUniform("gradientHeight", 4f)
            setFloatUniform("in_blurRadius", 8f)
            setFloatUniform("in_gradientSpeed", -1f)
            setFloatUniform("in_blurOffset", 2f, 0f)
        }
        shader.asComposeRenderEffect("child")
    }.getOrNull()

    private fun floatingMaterialInput(): RenderEffect {
        val blur = createBlurRenderEffect(25f, 25f, TileMode.Mirror)
        val color1 = createBlendModeColorFilterRenderEffect(Color.White.copy(alpha = 0.6f), BlendMode.SrcOver, blur)
        return createBlendModeColorFilterRenderEffect(Color.White.copy(alpha = 0.2f), BlendMode.Luminosity, color1)
    }

    private fun configureFloatingCommon(shader: RuntimeShader, standalone: Boolean) {
        val ball = floatArrayOf(0f, 0f, 2f, 2f, 2f, 1f, 1f, 1f)
        val common = floatArrayOf(2f, 2f, ball[0], ball[1], ball[2], ball[3], ball[4], ball[5], ball[6], ball[7], 4f, 0f)
        shader.setFloatUniform("u_size", 4f, 4f)
        shader.setFloatUniform("u_alpha", 1f)
        shader.setFloatUniform("uUseMenuLightEdge", 0f)
        shader.setFloatUniform("u_commonArray", common)
        if (standalone) {
            shader.setFloatUniform("u_baseShapeCtxArray", floatingShapeContext(2f, 2f, 1f, 2f))
        } else {
            shader.setFloatUniform("u_materialIndex", 1f)
            val ball2 = floatArrayOf(0f, 0f, 3f, 2f, 1f, 1f, 1f, 1f)
            val emptyBall = FloatArray(8)
            val emptyCtx = floatingShapeContext(0f, 0f, 1f, 0f)
            val emptyPair = FloatArray(24)
            shader.setFloatUniform("u_ball1Array", ball)
            shader.setFloatUniform("u_ball2Array", ball2)
            shader.setFloatUniform("u_ball3Array", emptyBall)
            shader.setFloatUniform("u_ball1ShapeCtxArray", floatingShapeContext(2f, 2f, 1f, 2f))
            shader.setFloatUniform("u_ball2ShapeCtxArray", floatingShapeContext(1f, 1f, 1f, 1f))
            shader.setFloatUniform("u_ball3ShapeCtxArray", emptyCtx)
            shader.setFloatUniform("u_preCmpArray12", emptyPair)
            shader.setFloatUniform("u_preCmpArray13", emptyPair)
            shader.setFloatUniform("u_preCmpArray23", emptyPair)
        }
        val edge = floatingEdge(ball, 2f, 0.5f, 1)
        val glow = floatingInnerGlow(ball, 0.3f, 1, 1f)
        shader.setFloatUniform("u_edgeArray", edge)
        shader.setFloatUniform("u_shadowArray", glow)
        shader.setFloatUniform("u_shadowShapeCtxArray", floatingShadowShapeContext(ball, glow))
        shader.setColorUniform("u_color", Color.Transparent)
        shader.setColorUniform("u_themeTint", Color.Transparent)
    }
}
