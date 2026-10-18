// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalContentColor

/**
 * The ColorOS 17 toolbar icon button with its light halo.
 *
 * ColorOS gives the toolbar's back key and overflow key the same effect: a true 40dp circle — not a
 * squircle — carrying a radial fill that is darker in the middle and brighter at the rim, an
 * angle-distributed material perimeter highlight, and a theme-dependent halo outside the edge. While
 * pressed the whole control springs to [HighlightedIconButtonDefaults.PressedScale], the rim and the
 * halo disappear and the fill and icon brighten; both themes behave identically.
 *
 * The rim uses the framework's material-luminance blend with TYPE_FRAMEWORK_CIRCLE_1 and
 * EDGE_STYLE_1. Its near/far ramps keep the top sharp and the lower edge softer. Fill and rim
 * share one sampled surface, while icons and exterior caustics remain outside the RGB effect.
 *
 * The content colour is published through [LocalContentColor], so a default [Icon] in [content]
 * follows the fill as it brightens.
 *
 * @param onClick The callback when the button is clicked.
 * @param modifier The modifier to be applied to the button.
 * @param size The diameter of the button.
 * @param enabled Whether the button can be clicked.
 * @param content The content of the button, typically an [Icon], drawn centred in the circle.
 */
@Composable
fun HighlightedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = HighlightedIconButtonDefaults.Size,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val touchMotion = rememberCOUITouchMotionState()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (enabled && (isPressed || touchMotion.values.value.pressed)) 1f else 0f,
        animationSpec = HighlightedIconButtonDefaults.PressSpring,
        label = "highlightedIconButtonPress",
    )

    val scheme = COUITheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val scale = 1f - (1f - HighlightedIconButtonDefaults.PressedScale) * pressProgress
    val contentColor = lerp(
        scheme.onBackground,
        Color.White,
        HighlightedIconButtonDefaults.PressedContentBrighten * pressProgress,
    )

    val currentOnClick by rememberUpdatedState(onClick)
    val clickableModifier = remember(enabled, interactionSource) {
        if (enabled) {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { currentOnClick() },
            )
        } else {
            Modifier
        }
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .size(size)
                .couiTouchMotion(
                    enabled = enabled,
                    motionState = touchMotion,
                    pressScaleEnabled = false,
                    lightEnabled = false,
                    extraTransform = {
                        // Press shrink and native drag deformation belong to one visual layer.
                        scaleX += scale - 1f
                        scaleY += scale - 1f
                    },
                )
                .then(clickableModifier),
            contentAlignment = Alignment.Center,
        ) {
            if (isDark) {
                MaterialCausticShadow(
                    particles = listOf(
                        CausticShadowParticle(
                            MetaBallParticle.circle(size / 2, size / 2, size),
                            CausticShadowProfile.Toolbar,
                        ),
                    ),
                    color = HighlightedIconButtonDefaults.DarkHaloColor.copy(
                        alpha = HighlightedIconButtonDefaults.DarkHaloColor.alpha * (1f - pressProgress).coerceIn(0f, 1f),
                    ),
                    modifier = Modifier.matchParentSize(),
                )
            }
            Box(
                modifier = Modifier.matchParentSize()
                    .drawWithContent {
                        if (!isDark && pressProgress < 1f) drawSoftDropHalo(center, this.size.minDimension / 2f, 1f - pressProgress)
                        drawContent()
                    }
                    .nativeMaterialEdge(
                        particles = listOf(MetaBallParticle.circle(size / 2, size / 2, size)),
                        rim = Color.White.copy(alpha = HighlightedIconButtonDefaults.stroke(isDark).edgeAlpha * (1f - pressProgress).coerceIn(0f, 1f)),
                        edgeStyle = GradientStrokeStyle.EDGE_STYLE_1,
                        lineWidth = HighlightedIconButtonDefaults.StrokeWidth,
                        extension = 0.dp,
                    )
                    .drawWithContent {
                        val radius = this.size.minDimension / 2f
                        val center = this.center
                        val brighten = HighlightedIconButtonDefaults.PressedFillBrighten * pressProgress
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0f to lerp(
                                        HighlightedIconButtonDefaults.fillCenter(isDark),
                                        Color.White,
                                        brighten,
                                    ).copy(alpha = HighlightedIconButtonDefaults.FillAlpha),
                                    HighlightedIconButtonDefaults.FillEdgeStop to lerp(
                                        HighlightedIconButtonDefaults.fillEdge(isDark),
                                        Color.White,
                                        brighten,
                                    ).copy(alpha = HighlightedIconButtonDefaults.FillAlpha),
                                ),
                                center = center,
                                radius = radius,
                            ),
                            radius = radius,
                            center = center,
                        )
                        drawContent()
                    },
            )
            // Keep the icon out of the material RGB effect.
            Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center, content = content)
        }
    }
}

/**
 * The light theme's halo: a soft dark shadow all around the rim, offset slightly downward.
 *
 * The measured profile is near symmetric but bottom-weighted (bottom -13, sides -11, top -8 luma),
 * reaching [HighlightedIconButtonDefaults.LightHaloRadius] past the edge.
 */
private fun DrawScope.drawSoftDropHalo(center: Offset, radius: Float, progress: Float) {
    val outerRadius = radius + HighlightedIconButtonDefaults.LightHaloRadius.toPx()
    val haloCenter = center + Offset(0f, HighlightedIconButtonDefaults.LightHaloOffsetY.toPx())
    val color = HighlightedIconButtonDefaults.LightHaloColor.copy(
        alpha = HighlightedIconButtonDefaults.LightHaloColor.alpha * progress,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to color,
                (radius / outerRadius) to color,
                1f to Color.Transparent,
            ),
            center = haloCenter,
            radius = outerRadius,
        ),
        radius = outerRadius,
        center = haloCenter,
    )
}

/** Defaults for [HighlightedIconButton]. */
object HighlightedIconButtonDefaults {

    /** The diameter of the button. Measurement: a true circle, 155px at 1dp = 3.875px, i.e. 40dp. */
    val Size = 40.dp

    /**
     * The scale the whole control springs to while pressed. Measurement: the 77.5px radius shrinks
     * to 69px, a ratio of ≈0.89.
     */
    val PressedScale = 0.89f

    /**
     * The press spring: the COUI press-feedback spring (`COUISpringForce`, response 0.3s / bounce 0).
     */
    val PressSpring: SpringSpec<Float> = folmeSpring(
        damping = 1f,
        response = 0.3f,
        visibilityThreshold = 0.001f,
    )

    /** The alpha the radial fill is painted at. Measurement: a two-point estimate of ≈0.78. */
    val FillAlpha = 0.78f

    /**
     * The radial-fill stop at which the edge colour is reached, as a fraction of the button radius.
     * Measurement: the edge colour appears at r=70px of the 77.5px radius in the dark theme and at
     * r=68px in the light theme, i.e. ≈0.88–0.90 of the radius.
     */
    val FillEdgeStop = 0.89f

    /** The radial-fill centre colour of the dark theme. Measurement: #1e1e1e at r=0. */
    val FillCenterDark = Color(0xFF1E1E1E)

    /** The radial-fill edge colour of the dark theme. Measurement: #242424 at r=70px. */
    val FillEdgeDark = Color(0xFF242424)

    /** The radial-fill centre colour of the light theme. Measurement: 249 at r=0. */
    val FillCenterLight = Color(0xFFF9F9F9)

    /** The radial-fill edge colour of the light theme. Measurement: 251 at r=68px. */
    val FillEdgeLight = Color(0xFFFBFBFB)

    /** The radial-fill centre colour for [isDark]. */
    fun fillCenter(isDark: Boolean): Color = if (isDark) FillCenterDark else FillCenterLight

    /** The radial-fill edge colour for [isDark]. */
    fun fillEdge(isDark: Boolean): Color = if (isDark) FillEdgeDark else FillEdgeLight

    /**
     * How far the pressed fill is lerped toward white. The measurement records that the fill
     * brightens while pressed but carries no magnitude, so this is a nominal value.
     */
    val PressedFillBrighten = 0.06f

    /**
     * How far the pressed content colour is lerped toward white; see [PressedFillBrighten].
     */
    val PressedContentBrighten = 0.1f

    /** TYPE_FRAMEWORK_CIRCLE_1 dark edge alpha; applied through native material RGB blending. */
    val StrokeAlpha = 0.6f

    /** TYPE_FRAMEWORK_CIRCLE_1 light edge alpha. */
    val LightStrokeAlpha = 0.2f

    /** TYPE_FRAMEWORK_CIRCLE_1 edge width in both themes. */
    val StrokeWidth = 1.9.dp

    /** TYPE_FRAMEWORK_CIRCLE_1 dark inner-shadow parameter, retained with the native preset. */
    val StrokeShadowFadeIn = 0.1f

    /** The regular dark toolbar circle preset, not the popup-window circle variant. */
    val Stroke = MaterialStrokeParams(
        edgeAlpha = StrokeAlpha,
        edgeWidth = StrokeWidth,
        edgeAngle = 0f,
        shadowFadeIn = StrokeShadowFadeIn,
        shadowFadeOut = 1f,
        shadowFadeScale = 1f,
    )

    /** TYPE_FRAMEWORK_CIRCLE_1_LIGHT from ToolbarMaterialEffectResolver. */
    val LightStroke = Stroke.copy(edgeAlpha = LightStrokeAlpha, shadowFadeIn = 0.15f)

    /** Resolves the regular toolbar edge parameters by theme. */
    fun stroke(isDark: Boolean): MaterialStrokeParams = if (isDark) Stroke else LightStroke

    /** ToolbarMaterialEffectDelegate uses coui_caustic_color_1: 0x33ffffff at night. */
    val DarkHaloColor = Color.White.copy(alpha = 51f / 255f)

    /** Native level-2 bottom layout offset; the SDF fades before reaching the draw rectangle. */
    val DarkHaloExtent = 4.899994.dp

    /** Native level-2 distance field width. */
    val DarkHaloSdfWidth = 9.22998.dp

    /**
     * The light theme's halo colour. Measurement: a soft dark drop shadow, deepest -13 luma over the
     * background the button was measured against.
     *
     * Provisional: alpha lowered from 0.05 to 0.03 because the user reports the ColorOS 17 halo reads
     * heavier than on the device. No circular-button reference could be measured on the test
     * devices, so this is a fallback value, to be calibrated against a live reference.
     */
    val LightHaloColor = Color.Black.copy(alpha = 0.03f)

    /**
     * How far the light theme's halo reaches past the edge. Measurement: ≈60px ≈ 15.5dp.
     *
     * Provisional: lowered from 15.5dp to 12dp together with [LightHaloColor], pending a live
     * reference measurement.
     */
    val LightHaloRadius = 12.dp

    /**
     * The light theme's halo vertical offset. Measurement: the halo is near symmetric but
     * bottom-weighted (bottom -13, sides -11, top -8 luma).
     */
    val LightHaloOffsetY = 1.dp
}
