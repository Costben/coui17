// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coui17.Coui17Icons
import io.github.suqi8.coui.kmp.basic.BottomFloatingAccessoryNavigationBar
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBar
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarDefaults
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.LocalPlatformBottomBarMaterialEnabled
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.LocalBlurEnabled
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.squircle.LocalSquircleEnabled
import io.github.suqi8.coui.kmp.utils.rememberGalleryDensity
import ui.AppTheme
import android.graphics.Color as AndroidColor

/**
 * Fixed scenes for comparing this library's floating bottom bars against ColorOS 17 with the
 * **same content behind the bar**, so the glass fill, rim, caustic and any bloom are measured
 * against a known backdrop instead of whatever page happened to be on screen.
 *
 * ```
 * adb shell am start -n io.github.suqi8.coui17/.BottomBarGlowProbeActivity \
 *   --es bg black --es dark true --es bar accessory --es native true
 * ```
 *
 * - `bg`: `pattern` | `pattern-bright` | `black` | `white`
 * - `dark`: `true` | `false`
 * - `bar`: `accessory` | `capsule` | `none` (no bar at all, for a reference frame)
 * - `native`: `true` (platform material) | `false` (Compose renderer)
 *
 * Every scene logs `bar <left>,<top>,<right>,<bottom> density=<d>` under the `COUIGlowProbe` tag so
 * the measurement script can locate the silhouette without image heuristics.
 */
class BottomBarGlowProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val background = intent.getStringExtra("bg") ?: "black"
        val dark = intent.getStringExtra("dark")?.toBooleanStrictOrNull() ?: true
        val bar = intent.getStringExtra("bar") ?: "accessory"
        val native = intent.getStringExtra("native")?.toBooleanStrictOrNull() ?: true
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
        )
        window.isNavigationBarContrastEnforced = false
        setContent {
            AppTheme(colorMode = if (dark) 2 else 1) {
                CompositionLocalProvider(
                    LocalBlurEnabled provides true,
                    LocalSquircleEnabled provides true,
                    LocalPlatformBottomBarMaterialEnabled provides native,
                ) {
                    GlowScene(background = background, bar = bar)
                }
            }
        }
    }
}

@Composable
private fun GlowScene(background: String, bar: String) {
    val backdrop = rememberLayerBackdrop { drawContent() }
    Box(Modifier.fillMaxSize()) {
        // The recorded layer is the scene only: the bar samples it and must not be inside it.
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            SceneBackground(background)
        }
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                if (bar != "none") ProbeBar(bar)
            }
        }
    }
}

@Composable
private fun SceneBackground(kind: String) {
    when (kind) {
        "white" -> Box(Modifier.fillMaxSize().background(Color.White))
        "black" -> Box(Modifier.fillMaxSize().background(Color.Black))
        else -> ProbePatternBackground(bright = kind == "pattern-bright")
    }
}

@Composable
private fun ProbeBar(kind: String) {
    val galleryDensity = rememberGalleryDensity()
    // The navigation padding belongs to the host, exactly as `Coui17AccessoryBar` does it: the
    // capsule form of `BottomFloatingNavigationBar` applies its own modifier to the capsule, so
    // padding it there would shrink the glass instead of lifting the bar.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = BottomFloatingNavigationBarDefaults.navigationBarPadding()),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalDensity provides galleryDensity) {
            val modifier = Modifier
                .onGloballyPositioned { coords ->
                    val position = coords.positionInWindow()
                    Log.d(
                        "COUIGlowProbe",
                        "bar ${position.x.toInt()},${position.y.toInt()}," +
                            "${(position.x + coords.size.width).toInt()},${(position.y + coords.size.height).toInt()} " +
                            "density=${galleryDensity.density}",
                    )
                }
            if (kind == "capsule") {
                BottomFloatingNavigationBar(modifier = modifier) { ProbeItems() }
            } else {
                BottomFloatingAccessoryNavigationBar(
                    modifier = modifier.fillMaxWidth(),
                    startAccessory = { ProbeBall(COUIIcons.Basic.Search) },
                    endAccessory = { ProbeBall(COUIIcons.More) },
                ) {
                    ProbeItems()
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ProbeItems() {
    BottomFloatingNavigationBarItem(
        selected = true,
        onClick = {},
        icon = Coui17Icons.HomeLine,
        selectedIcon = Coui17Icons.HomeFill,
        label = "Home",
    )
    BottomFloatingNavigationBarItem(
        selected = false,
        onClick = {},
        icon = Coui17Icons.GridLine,
        selectedIcon = Coui17Icons.GridFill,
        label = "Icons",
    )
    BottomFloatingNavigationBarItem(
        selected = false,
        onClick = {},
        icon = Coui17Icons.PaletteLine,
        selectedIcon = Coui17Icons.PaletteFill,
        label = "Color",
    )
}

@Composable
private fun ProbeBall(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(imageVector = icon, contentDescription = null)
    }
}
