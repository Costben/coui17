// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.GradientStrokeStyle
import io.github.suqi8.coui.kmp.basic.InnerShadowStyle
import io.github.suqi8.coui.kmp.basic.MaterialShadowEdgeDefaults
import io.github.suqi8.coui.kmp.basic.MaterialStrokeParams
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.SpotLightDefaults
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.materialShadowEdge
import io.github.suqi8.coui.kmp.basic.spotLight
import io.github.suqi8.coui.kmp.theme.COUITheme

fun LazyListScope.materialEffectSection() {
    item(key = "materialShadowEdge") {
        SmallTitle(text = "MaterialShadowEdge")
        MaterialShadowEdgeDemo()
    }
    item(key = "spotLight") {
        SmallTitle(text = "SpotLight")
        SpotLightDemo()
    }
}

/** Renders the "MaterialShadowEdge" gallery section content (capsule, circle and rounded plates showing the material shadow edge). */
@Composable
fun MaterialShadowEdgeDemo() {
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    val surface = if (isDark) Color(0xFF2B2B2F) else Color(0xFFF0F1F3)

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        // A dark plate, so the bright rim of the edge shows up on both themes.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (isDark) Color(0xFF0B0B0D) else Color(0xFF1B1B1F)),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShadowEdgeSurface(
                    color = surface,
                    modifier = Modifier.size(width = 104.dp, height = 44.dp),
                )
                ShadowEdgeSurface(
                    color = surface,
                    modifier = Modifier.size(44.dp),
                    cornerWeight = MaterialShadowEdgeDefaults.SingleItemCornerWeight,
                )
                ShadowEdgeSurface(
                    color = surface,
                    modifier = Modifier.size(96.dp),
                    cornerRadius = 28.dp,
                    cornerWeight = MaterialShadowEdgeDefaults.SingleItemCornerWeight,
                )
            }
        }
    }
}

@Composable
private fun ShadowEdgeSurface(
    color: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp? = null,
    cornerWeight: Float = MaterialShadowEdgeDefaults.CornerWeight,
) {
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    val shape = RoundedCornerShape(cornerRadius ?: (44.dp / 2))

    Box(
        modifier = modifier
            .clip(shape)
            .background(color)
            .materialShadowEdge(
                stroke = MaterialStrokeParams.frameworkCapsule13(isDark),
                fallback = Modifier.background(color = color, shape = shape),
                cornerWeight = cornerWeight,
                cornerRadius = cornerRadius,
                edgeStyle = GradientStrokeStyle.EDGE_STYLE_0,
                shadowStyle = InnerShadowStyle.SHADOW_STYLE_0,
            ),
    )
}

/** Renders the "SpotLight" gallery section content (a static spotlight and an animated sweeping one). */
@Composable
fun SpotLightDemo() {
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    val params = if (isDark) SpotLightDefaults.CapsuleParams1Dark else SpotLightDefaults.CapsuleParams1Light
    val density = LocalDensity.current
    val surface = if (isDark) Color(0xFF3A3A3F) else Color(0xFFE7E9EC)
    // The native radius and light height are raw fragment-space values, so they are converted to the
    // dp this modifier takes.
    val endRadius = with(density) { params.endRadius.toDp() }
    val lightHeight = with(density) { params.lightHeight.toDp() }
    val shape = RoundedCornerShape(20.dp)

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(shape)
                    .background(surface)
                    .spotLight(
                        position = Offset(0.5f, 0.5f),
                        radius = endRadius,
                        intensity = params.endIntensity,
                        lightColor = Color.White,
                        lightHeight = lightHeight,
                        ambientStrength = params.ambientStrength,
                        diffuseStrength = params.diffuseStrength,
                        noise = params.noise,
                        dither = params.ditherN,
                        blendMode = params.blendMode,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Static | CapsuleParams1 | OVERLAY")
            }

            Spacer(Modifier.height(16.dp))

            val transition = rememberInfiniteTransition()
            val sweep by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            )
            val glow by transition.animateFloat(
                initialValue = params.startIntensity,
                targetValue = params.endIntensity,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(shape)
                    .background(surface)
                    .spotLight(
                        position = Offset(sweep, 0.5f),
                        radius = endRadius,
                        intensity = glow,
                        lightColor = Color.White,
                        lightHeight = lightHeight,
                        ambientStrength = params.ambientStrength,
                        diffuseStrength = params.diffuseStrength,
                        noise = params.noise,
                        dither = params.ditherN,
                        blendMode = params.blendMode,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Animated | position.x 0.2..0.8 | intensity 0..1")
            }
        }
    }
}
