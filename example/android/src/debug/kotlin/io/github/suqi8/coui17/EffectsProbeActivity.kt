// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.anim.BezierGravityPathGeometry
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlay
import io.github.suqi8.coui.kmp.basic.ListPopupContent
import io.github.suqi8.coui.kmp.basic.LocalEdgeToEdge
import io.github.suqi8.coui.kmp.basic.PopupLayoutPosition
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * Fixed scenes for measuring this library's effects on a device against the ColorOS 17 system,
 * from screenshots and screen recordings.
 *
 * `adb shell am start -n io.github.suqi8.coui17/.EffectsProbeActivity --es effect shadow`
 * effect: shadow | blur | gradient | scroll.
 *
 * `--es bg RRGGBB` replaces the striped strip scene with a flat colour. The blur probe needs it to
 * measure the material against known backdrops instead of one busy pattern.
 */
class EffectsProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val effect = intent.getStringExtra("effect") ?: "shadow"
        val flatBackground = intent.getStringExtra("bg")?.let(::parseSceneColor)
        setContent {
            COUITheme {
                when (effect) {
                    "blur" -> BlurScene(flatBackground)
                    "gradient" -> GradientScene()
                    "scroll" -> ScrollScene()
                    else -> ShadowScene()
                }
            }
        }
    }
}

private val ProbeSize = Modifier.size(168.dp, 88.dp)

/** Parses the `--es bg RRGGBB` probe backdrop; anything unparsable keeps the striped scene. */
private fun parseSceneColor(raw: String): Color? = raw.removePrefix("#").takeIf { it.length == 6 }?.toLongOrNull(16)?.let { Color(0xFF000000L or it) }

/** The scene behind the probes: the striped busy pattern unless the caller asked for a flat colour. */
@Composable
private fun SceneBackground(flat: Color?) {
    if (flat == null) {
        BusyBackground()
    } else {
        Box(Modifier.fillMaxSize().background(flat))
    }
}

@Composable
private fun ShadowScene() {
    Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        StaticPopup(glass = false) { Box(ProbeSize.semantics { contentDescription = "mine-shadow-probe" }) }
    }
}

@Composable
private fun BlurScene(flatBackground: Color?) {
    val backdrop = rememberLayerBackdrop { drawContent() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) { SceneBackground(flatBackground) }
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            StaticPopup(glass = true) { Box(ProbeSize.semantics { contentDescription = "mine-blur-probe" }) }
        }
    }
}

@Composable
private fun GradientScene() {
    val backdrop = rememberLayerBackdrop { drawContent() }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) { BusyBackground() }
        CompositionLocalProvider(LocalBackdrop provides backdrop, LocalEdgeToEdge provides true) {
            // Default container color: a transparent one turns the material's alpha ramp into black.
            AppBarBlurOverlay(
                modifier = Modifier.fillMaxWidth().height(120.dp).semantics { contentDescription = "mine-gradient-probe" },
            )
        }
    }
}

@Composable
private fun ScrollScene() {
    val state = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F2))
            .verticalScroll(state)
            .semantics { contentDescription = "mine-scroll" }
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        repeat(30) { index ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(56.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) { Text("Row $index") }
        }
    }
}

@Composable
private fun StaticPopup(glass: Boolean, content: @Composable () -> Unit) {
    ListPopupContent(
        popupContentSize = IntSize.Zero,
        onPopupContentSizeChange = {},
        fractionProgress = { 1f },
        alphaProgress = { 1f },
        pathProgress = { 1f },
        gravityPath = BezierGravityPathGeometry(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        popupLayoutPosition = PopupLayoutPosition(showBelow = false, showAbove = true, isRightAligned = true),
        localTransformOrigin = TransformOrigin.Center,
        enableGlass = glass,
        content = content,
    )
}

/** High-contrast stripes and text so blur radius and falloff are readable in a screenshot. */
@Composable
private fun BusyBackground() {
    val colors = listOf(Color(0xFFE53935), Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA), Color.White, Color.Black)
    Column(Modifier.fillMaxSize()) {
        repeat(14) { row ->
            Row(Modifier.fillMaxWidth().height(64.dp)) {
                repeat(7) { column ->
                    Box(
                        Modifier.weight(1f).fillMaxSize().background(colors[(row + column) % colors.size]),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${row}$column", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors[(row + column + 3) % colors.size])
                    }
                }
            }
        }
    }
}
