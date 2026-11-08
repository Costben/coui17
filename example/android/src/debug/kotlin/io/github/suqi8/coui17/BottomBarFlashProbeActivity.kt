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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coui17.Coui17Icons
import io.github.suqi8.coui.kmp.basic.BottomFloatingAccessoryNavigationBar
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBar
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarDefaults
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.DropdownImpl
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.basic.LocalPlatformBottomBarMaterialEnabled
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.preciseClickAnchor
import io.github.suqi8.coui.kmp.basic.rememberPreciseClickState
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.LocalBlurEnabled
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.blur.textureBlur
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.layout.DialogButtonBar
import io.github.suqi8.coui.kmp.layout.DialogButtonBarAction
import io.github.suqi8.coui.kmp.overlay.OverlayDialog
import io.github.suqi8.coui.kmp.overlay.OverlayListPopup
import io.github.suqi8.coui.kmp.squircle.LocalSquircleEnabled
import io.github.suqi8.coui.kmp.theme.LocalDismissState
import io.github.suqi8.coui.kmp.utils.rememberGalleryDensity
import ui.AppTheme
import android.graphics.Color as AndroidColor

/**
 * Reproduction probe for floating bottom bar flash when an overlay popup or dialog is shown / dismissed.
 *
 * ```
 * adb shell am start -n io.github.suqi8.coui17/.BottomBarFlashProbeActivity \
 *   --es native true --es dim true --es bar accessory --es bg pattern
 * ```
 *
 * - `bg`: `pattern` | `pattern-bright` | `black` | `white`
 * - `native`: `true` (platform material) | `false` (Compose renderer)
 * - `dim`: `true` | `false` (window dim behind the overlay)
 * - `bar`: `accessory` | `capsule`
 * - `dark`: `true` | `false`
 * - `backdrop`: `true` (overlay samples the recorded layer) | `false`
 * - `keep_active`: `true` keeps one backdrop consumer alive
 */
class BottomBarFlashProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialNative = intent.getStringExtra("native")?.toBooleanStrictOrNull() ?: true
        val initialDim = intent.getStringExtra("dim")?.toBooleanStrictOrNull() ?: true
        val barKind = intent.getStringExtra("bar") ?: "accessory"
        val bgKind = intent.getStringExtra("bg") ?: "pattern"
        val dark = intent.getStringExtra("dark")?.toBooleanStrictOrNull() ?: false
        val withBackdrop = intent.getStringExtra("backdrop")?.toBooleanStrictOrNull() ?: true
        val keepActive = intent.getStringExtra("keep_active")?.toBooleanStrictOrNull() ?: false
        // Cobo layout: bar inside the recorded backdrop content, centered like-confirm dialog.
        val coboLayout = intent.getStringExtra("cobo")?.toBooleanStrictOrNull() ?: false
        // Cobo MainShell: an inner backdrop around the page only, sampled all the time by the top bar.
        val nested = intent.getStringExtra("nested")?.toBooleanStrictOrNull() ?: false
        probeLayered = intent.getStringExtra("layered")?.toBooleanStrictOrNull() ?: false
        // Cobo's top bar: the platform app bar material (AppBarBlurOverlay(platformRenderer = true)).
        val appBar = intent.getStringExtra("appbar")?.toBooleanStrictOrNull() ?: false

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
        )
        window.isNavigationBarContrastEnforced = false

        setContent {
            var nativeMaterial by remember { mutableStateOf(initialNative) }
            var windowDim by remember { mutableStateOf(initialDim) }

            AppTheme(colorMode = if (dark) 2 else 1) {
                CompositionLocalProvider(
                    LocalBlurEnabled provides true,
                    LocalSquircleEnabled provides true,
                    LocalPlatformBottomBarMaterialEnabled provides nativeMaterial,
                ) {
                    FlashProbeScene(
                        bgKind = bgKind,
                        barKind = barKind,
                        nativeMaterial = nativeMaterial,
                        onToggleNative = { nativeMaterial = !nativeMaterial },
                        windowDim = windowDim,
                        onToggleDim = { windowDim = !windowDim },
                        withBackdrop = withBackdrop,
                        keepActive = keepActive,
                        coboLayout = coboLayout,
                        nested = nested,
                        appBar = appBar,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashProbeScene(
    bgKind: String,
    barKind: String,
    nativeMaterial: Boolean,
    onToggleNative: () -> Unit,
    windowDim: Boolean,
    onToggleDim: () -> Unit,
    withBackdrop: Boolean = true,
    keepActive: Boolean = false,
    coboLayout: Boolean = false,
    nested: Boolean = false,
    appBar: Boolean = false,
) {
    val backdrop = rememberLayerBackdrop { drawContent() }
    val innerBackdrop = rememberLayerBackdrop()
    var showPopup by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    val preciseState = rememberPreciseClickState()

    Scaffold(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            if (keepActive) {
                // Keep 1 consumer active on backdrop at all times
                Spacer(Modifier.size(1.dp).textureBlur(backdrop, androidx.compose.ui.graphics.RectangleShape, blurRadius = 1f))
            }
            // Layer backdrop recording container (like Cobo Shell)
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (withBackdrop) Modifier.layerBackdrop(backdrop) else Modifier),
            ) {
                Box(Modifier.fillMaxSize().then(if (nested) Modifier.layerBackdrop(innerBackdrop) else Modifier)) {
                    ProbeBackground(bgKind)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(100.dp))
                        Text("COUI Bottom Bar Flash Probe", color = Color.White)
                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            Button(onClick = onToggleNative) {
                                Text(if (nativeMaterial) "Native: ON" else "Native: OFF")
                            }
                            Button(onClick = onToggleDim) {
                                Text(if (windowDim) "Dim: ON" else "Dim: OFF")
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Long press target for OverlayListPopup
                        Box(
                            modifier = Modifier
                                .size(260.dp, 160.dp)
                                .preciseClickAnchor(preciseState)
                                .background(Color(0x884488FF))
                                .combinedClickable(
                                    onClick = { showPopup = true },
                                    onLongClick = { showPopup = true },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Tap / Long Press to Pop Menu",
                                color = Color.White,
                                modifier = Modifier.padding(12.dp),
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(onClick = { showDialog = true }) {
                            Text("Show OverlayDialog")
                        }

                        Spacer(Modifier.height(300.dp))
                    }
                }
                if (appBar) {
                    CompositionLocalProvider(
                        LocalBackdrop provides innerBackdrop,
                        io.github.suqi8.coui.kmp.basic.LocalEdgeToEdge provides true,
                    ) {
                        io.github.suqi8.coui.kmp.basic.AppBarBlurOverlay(
                            scrollFraction = { 1f },
                            modifier = Modifier.fillMaxWidth(),
                            platformRenderer = true,
                        ) {
                            Spacer(Modifier.fillMaxWidth().height(110.dp))
                        }
                    }
                } else if (nested) {
                    // Stand-in for Cobo's top bar: a constant consumer of the inner backdrop.
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .textureBlur(innerBackdrop, androidx.compose.ui.graphics.RectangleShape, blurRadius = 30f),
                    )
                }
                if (coboLayout) {
                    CompositionLocalProvider(LocalBackdrop provides if (nested) innerBackdrop else LocalBackdrop.current) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .then(if (LocalProbeLayered.current) Modifier.graphicsLayer {} else Modifier),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            FlashProbeBar(barKind)
                        }
                    }
                }
            }

            // Bottom bar outside the layerBackdrop container
            if (!coboLayout) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    FlashProbeBar(barKind)
                }
            }

            // OverlayListPopup and OverlayDialog sampling backdrop (mirroring Cobo Root)
            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                OverlayListPopup(
                    show = showPopup,
                    onDismissRequest = { showPopup = false },
                    preciseClickState = preciseState,
                    enableWindowDim = windowDim,
                ) {
                    val dismiss = LocalDismissState.current
                    ListPopupColumn {
                        DropdownImpl(
                            text = "保存原图",
                            optionSize = 2,
                            isSelected = false,
                            index = 0,
                            onSelectedIndexChange = {
                                dismiss?.invoke()
                            },
                        )
                        DropdownImpl(
                            text = "取消",
                            optionSize = 2,
                            isSelected = false,
                            index = 1,
                            onSelectedIndexChange = {
                                dismiss?.invoke()
                            },
                        )
                    }
                }

                if (coboLayout) {
                    // Cobo's ConfirmDialog: a fresh instance per request that enters with show=false.
                    if (showDialog) {
                        var show by remember { mutableStateOf(false) }
                        androidx.compose.runtime.LaunchedEffect(Unit) { show = true }
                        OverlayDialog(
                            show = show,
                            title = "点赞",
                            summary = "确定要点赞吗？",
                            outsideMargin = DpSize(16.dp, 8.dp),
                            largeScreen = true,
                            enableWindowDim = windowDim,
                            onDismissRequest = { show = false },
                            onDismissFinished = { showDialog = false },
                        ) {
                            DialogButtonBar(
                                negative = DialogButtonBarAction(text = "取消", onClick = { show = false }),
                                positive = DialogButtonBarAction(text = "点赞", onClick = { show = false }),
                            )
                        }
                    }
                } else {
                    OverlayDialog(
                        show = showDialog,
                        title = "确认点赞",
                        summary = "确定要给这条内容点赞吗？",
                        enableWindowDim = windowDim,
                        onDismissRequest = { showDialog = false },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Button(onClick = { showDialog = false }) {
                                Text("取消")
                            }
                            Spacer(Modifier.size(12.dp))
                            Button(onClick = { showDialog = false }) {
                                Text("确定")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProbeBackground(kind: String) {
    when (kind) {
        "white" -> Box(Modifier.fillMaxSize().background(Color.White))
        "black" -> Box(Modifier.fillMaxSize().background(Color.Black))
        else -> ProbePatternBackground(bright = kind == "pattern-bright")
    }
}

@Composable
private fun FlashProbeBar(kind: String) {
    val galleryDensity = rememberGalleryDensity()
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
                BottomFloatingNavigationBar(modifier = modifier) { ProbeFlashItems() }
            } else {
                BottomFloatingAccessoryNavigationBar(
                    modifier = modifier.fillMaxWidth(),
                    startAccessory = { ProbeFlashBall(COUIIcons.Basic.Search) },
                    endAccessory = { ProbeFlashBall(COUIIcons.More) },
                ) {
                    ProbeFlashItems()
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ProbeFlashItems() {
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
private fun ProbeFlashBall(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(imageVector = icon, contentDescription = null)
    }
}

private var probeLayered = false
private val LocalProbeLayered = androidx.compose.runtime.compositionLocalOf { probeLayered }
