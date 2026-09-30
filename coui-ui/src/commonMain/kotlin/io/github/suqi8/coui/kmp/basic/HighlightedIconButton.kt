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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
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
 * angle-distributed white perimeter stroke, and a theme-dependent halo outside the edge. While
 * pressed the whole control springs to [HighlightedIconButtonDefaults.PressedScale], the rim and the
 * halo disappear and the fill and icon brighten; both themes behave identically.
 *
 * The rim is [Modifier.materialShadowEdge], the port of the `ShadowEdge` runtime shader whose
 * `u_edgeArray[13]` angle is what distributes the stroke alpha around the perimeter — strong at the
 * top and bottom, nearly invisible at the left and right. The halo and the fill are painted here
 * because the shader only covers the surface's own bounds: the halo reaches outside the node, so
 * nothing in the caller's chain may clip it.
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (enabled && isPressed) 1f else 0f,
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
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .drawWithContent {
                    val radius = this.size.minDimension / 2f
                    val center = this.center
                    val highlight = 1f - pressProgress
                    if (highlight > 0f) {
                        if (isDark) {
                            drawLowerArcGlow(center, radius, highlight)
                        } else {
                            drawSoftDropHalo(center, radius, highlight)
                        }
                    }
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
                }
                .materialShadowEdge(
                    stroke = rimStroke(pressProgress),
                    cornerRadius = size / 2,
                    edgeStyle = GradientStrokeStyle.EDGE_STYLE_1,
                    shadowStyle = InnerShadowStyle.SHADOW_STYLE_1,
                )
                .then(clickableModifier),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/**
 * The dark theme's halo: a glow confined to the lower arc outside the rim.
 *
 * The measured profile is brightest right at the edge — ≈8% white 1px outside — and dies at
 * [HighlightedIconButtonDefaults.DarkHaloExtent] past it, with nothing above or beside the button.
 * The gradient's inner half is transparent; the fill covers that region anyway.
 */
private fun DrawScope.drawLowerArcGlow(center: Offset, radius: Float, progress: Float) {
    val outerRadius = radius + HighlightedIconButtonDefaults.DarkHaloExtent.toPx()
    val color = HighlightedIconButtonDefaults.DarkHaloColor.copy(
        alpha = HighlightedIconButtonDefaults.DarkHaloColor.alpha * progress,
    )
    drawArc(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                (radius / outerRadius) to color,
                1f to Color.Transparent,
            ),
            center = center,
            radius = outerRadius,
        ),
        startAngle = HighlightedIconButtonDefaults.DarkHaloStartAngle,
        sweepAngle = HighlightedIconButtonDefaults.DarkHaloSweepAngle,
        useCenter = true,
        topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
        size = Size(outerRadius * 2f, outerRadius * 2f),
    )
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

/**
 * The rim parameters for the current press, with the stroke and the inner shadow faded out as the
 * button goes down — the measured pressed state loses its outer stroke and glow entirely.
 */
private fun rimStroke(pressProgress: Float): MaterialStrokeParams = HighlightedIconButtonDefaults.Stroke.copy(
    edgeAlpha = HighlightedIconButtonDefaults.StrokeAlpha * (1f - pressProgress),
    shadowFadeIn = HighlightedIconButtonDefaults.StrokeShadowFadeIn * (1f - pressProgress),
)

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

    /** The rim alpha. Measurement: the perimeter stroke is ≈15% white. */
    val StrokeAlpha = 0.15f

    /** The rim width. Measurement: ≈0.8dp at the top of the circle and ≈1.2dp at the bottom. */
    val StrokeWidth = 1.dp

    /**
     * The rim's inner-shadow fade-in scale. Measurement: the button carries no inner shadow, so the
     * shader's fade is kept at the `TYPE_FRAMEWORK_CAPSULE_13` dark value where it is invisible.
     */
    val StrokeShadowFadeIn = 0.01f

    /** The rim: [StrokeAlpha] white, [StrokeWidth] wide, no inner shadow. */
    val Stroke = MaterialStrokeParams(
        edgeAlpha = StrokeAlpha,
        edgeWidth = StrokeWidth,
        edgeAngle = 0f,
        shadowFadeIn = StrokeShadowFadeIn,
        shadowFadeOut = 0f,
        shadowFadeScale = 1f,
    )

    /** The dark theme's glow colour. Measurement: ≈8% white 1px outside the edge. */
    val DarkHaloColor = Color.White.copy(alpha = 0.08f)

    /** How far the dark theme's glow reaches past the edge. Measurement: R+20px ≈ 5.2dp. */
    val DarkHaloExtent = 5.2.dp

    /** The glow arc's start angle; 0° is 3 o'clock, so 90° points straight down. */
    val DarkHaloStartAngle = 60f

    /** The glow arc's sweep. Measurement: the glow occupies only the lower 60°–120° arc. */
    val DarkHaloSweepAngle = 60f

    /**
     * The light theme's halo colour. Measurement: a soft dark drop shadow, deepest -13 luma over the
     * background the button was measured against.
     */
    val LightHaloColor = Color.Black.copy(alpha = 0.05f)

    /** How far the light theme's halo reaches past the edge. Measurement: ≈60px ≈ 15.5dp. */
    val LightHaloRadius = 15.5.dp

    /**
     * The light theme's halo vertical offset. Measurement: the halo is near symmetric but
     * bottom-weighted (bottom -13, sides -11, top -8 luma).
     */
    val LightHaloOffsetY = 1.dp
}
