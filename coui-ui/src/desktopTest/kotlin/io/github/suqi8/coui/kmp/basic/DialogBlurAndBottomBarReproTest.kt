package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.layout.DialogButtonBar
import io.github.suqi8.coui.kmp.layout.DialogButtonBarAction
import io.github.suqi8.coui.kmp.overlay.OverlayBottomSheet
import io.github.suqi8.coui.kmp.overlay.OverlayDialog
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.ColorSchemeMode
import io.github.suqi8.coui.kmp.theme.ThemeController
import io.github.suqi8.coui.kmp.utils.COUIPopupUtils.Companion.COUIPopupHost
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DialogBlurAndBottomBarReproTest {

    @Test
    fun overlayDialogSlideInSamplesGreenMiddleNotYellowBottomEdge() = runSkikoComposeUiTest(
        size = Size(1179f, 2556f),
        density = Density(3f),
    ) {
        mainClock.autoAdvance = false
        var show by mutableStateOf(false)
        setContent {
            COUITheme(controller = ThemeController(ColorSchemeMode.Dark)) {
                val rootBackdrop = rememberLayerBackdrop()
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().layerBackdrop(rootBackdrop)) {
                        Canvas(Modifier.fillMaxSize()) {
                            // y = 0..1700: RED
                            drawRect(Color(0xFFEE2222), size = Size(size.width, 1700f))
                            // y = 1700..2480: GREEN (where the settled dialog sits!)
                            drawRect(Color(0xFF11EE33), topLeft = Offset(0f, 1700f), size = Size(size.width, 780f))
                            // y = 2480..2556: BLUE (only very bottom 76px edge)
                            drawRect(Color(0xFF1133FF), topLeft = Offset(0f, 2480f), size = Size(size.width, 76f))
                        }
                    }
                    Box(Modifier.fillMaxSize()) {
                        CompositionLocalProvider(LocalBackdrop provides rootBackdrop) {
                            COUIPopupHost()
                        }
                    }
                    if (show) {
                        OverlayDialog(
                            show = show,
                            title = "测试弹窗",
                            summary = "检查底部滑入后是否停留在屏外或底边长条",
                            outsideMargin = DpSize(16.dp, 8.dp),
                            onDismissRequest = { show = false },
                        ) {
                            DialogButtonBar(
                                negative = DialogButtonBarAction("取消") { show = false },
                                positive = DialogButtonBarAction("确定") { show = false },
                            )
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeByFrame()
        waitForIdle()

        show = true
        repeat(30) {
            mainClock.advanceTimeBy(16L)
        }
        waitForIdle()

        val pixels = onRoot().captureToImage().toPixelMap()
        val pTop = pixels[pixels.width / 2, 2080]
        val pMid = pixels[pixels.width / 2, 2220]
        val pBot = pixels[pixels.width / 2, 2420]
        println("DIALOG_BANDS pTop=$pTop pMid=$pMid pBot=$pBot")
        // At y=2220 (middle of dialog, well inside GREEN band 1700..2480), green MUST strongly dominate blue & red!
        assertTrue(
            pMid.green > pMid.blue + 0.08f && pMid.green > pMid.red + 0.08f,
            "Dialog blur at y=2220 MUST be GREEN (not stale offscreen/blue edge)! got top=$pTop mid=$pMid bot=$pBot",
        )
    }

    @Test
    fun overlayBottomSheetSlideInSamplesGreenMiddleNotBlueBottomEdge() = runSkikoComposeUiTest(
        size = Size(1179f, 2556f),
        density = Density(3f),
    ) {
        mainClock.autoAdvance = false
        var show by mutableStateOf(false)
        setContent {
            COUITheme(controller = ThemeController(ColorSchemeMode.Dark)) {
                val rootBackdrop = rememberLayerBackdrop()
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().layerBackdrop(rootBackdrop)) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawRect(Color(0xFFEE2222), size = Size(size.width, 1400f))
                            drawRect(Color(0xFF11EE33), topLeft = Offset(0f, 1400f), size = Size(size.width, 1080f))
                            drawRect(Color(0xFF1133FF), topLeft = Offset(0f, 2480f), size = Size(size.width, 76f))
                        }
                    }
                    Box(Modifier.fillMaxSize()) {
                        CompositionLocalProvider(LocalBackdrop provides rootBackdrop) {
                            COUIPopupHost()
                        }
                    }
                    if (show) {
                        OverlayBottomSheet(
                            show = show,
                            title = "测试面板",
                            onDismissRequest = { show = false },
                        ) {
                            Box(Modifier.fillMaxWidth().height(300.dp))
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeByFrame()
        waitForIdle()

        show = true
        repeat(40) {
            mainClock.advanceTimeBy(16L)
        }
        waitForIdle()

        val pixels = onRoot().captureToImage().toPixelMap()
        val pTop = pixels[pixels.width / 2, 1700]
        val pMid = pixels[pixels.width / 2, 2000]
        val pBot = pixels[pixels.width / 2, 2350]
        println("SHEET_BANDS pTop=$pTop pMid=$pMid pBot=$pBot")
        assertTrue(
            pMid.green > pMid.blue + 0.08f && pMid.green > pMid.red + 0.08f,
            "Sheet blur at y=2000 MUST be GREEN (not stale offscreen/blue edge)! got top=$pTop mid=$pMid bot=$pBot",
        )
    }

    @Test
    fun bottomFloatingAccessoryBarLabelsStayVisibleAcrossTabSwitches() = runSkikoComposeUiTest(
        size = Size(393f, 120f),
        density = Density(1f),
    ) {
        val dummyIcon = ImageVector.Builder(
            name = "TestIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 4f)
            lineTo(20f, 4f)
            lineTo(20f, 20f)
            lineTo(4f, 20f)
            close()
        }.build()
        var tab by mutableStateOf(0)
        setContent {
            COUITheme(controller = ThemeController(ColorSchemeMode.Light)) {
                Box(Modifier.fillMaxSize()) {
                    BottomFloatingAccessoryNavigationBar(
                        backdrop = null,
                        containerColor = Color.White,
                        startAccessoryVisible = tab == 1,
                        endAccessoryVisible = true,
                        startAccessory = { Box(Modifier.fillMaxSize()) },
                        endAccessory = { Box(Modifier.fillMaxSize()) },
                    ) {
                        BottomFloatingNavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = dummyIcon,
                            label = "推荐",
                        )
                        BottomFloatingNavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = dummyIcon,
                            label = "动态",
                        )
                        BottomFloatingNavigationBarItem(
                            selected = tab == 2,
                            onClick = { tab = 2 },
                            icon = dummyIcon,
                            label = "我的",
                        )
                    }
                }
            }
        }
        for (target in listOf(0, 1, 2, 1, 0, 2, 0)) {
            tab = target
            waitForIdle()
            val pixels = onRoot().captureToImage().toPixelMap()
            // Label band sits in lower half of the 56dp capsule (y ~ 60..76 inside 120px canvas).
            // Count dark text pixels across the capsule label strip; must stay well above 0 on every switch.
            var darkLabelPixels = 0
            for (y in 56 until 86) {
                for (x in 80 until 310) {
                    val c = pixels[x, y]
                    if (c.red < 0.55f && c.green < 0.55f && c.blue < 0.55f) darkLabelPixels++
                }
            }
            assertTrue(
                darkLabelPixels > 40,
                "Bottom bar labels must remain visible after switching to tab=$target, darkPixels=$darkLabelPixels",
            )
        }
    }
}
