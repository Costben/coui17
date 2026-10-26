// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package coui17

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * Renders the single-input dialog at the device's exact pixel grid (1272x2772 @ 3.875 px/dp) and
 * dumps the result, so the rim, corners and the input row can be checked numerically instead of by
 * eye.
 */
@OptIn(ExperimentalTestApi::class)
class SingleInputDialogRenderTest {

    @Test
    fun renderDark() = runSkikoComposeUiTest(
        size = Size(1272f, 2772f),
        density = Density(3.875f),
    ) {
        setContent {
            COUITheme(colors = darkColorScheme()) {
                Scaffold(containerColor = Color(0xFF141414)) { _ ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        SingleInputDialog(
                            show = true,
                            title = "更改手机名称",
                            message = "在使用蓝牙，WLAN 直连等操作时，都会看到你的手机名称",
                            value = "一加 15",
                            onValueChange = {},
                            onDismissRequest = {},
                            onConfirm = {},
                        )
                    }
                }
            }
        }
        waitForIdle()
        val image = onRoot().captureToImage().toAwtImage()
        val out = File("/tmp/replica-dark.png")
        ImageIO.write(image, "png", out)
        println("WROTE ${out.absolutePath} ${image.width}x${image.height}")
    }
}
