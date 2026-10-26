// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package coui17

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import i18n.AppStrings
import i18n.LocalAppStrings
import i18n.str
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.SnackbarHostState
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * Renders the two ColorOS 17 templates at the device's exact pixel grid (1272x2772 @ 3.875 px/dp) and
 * dumps the result, so their geometry can be checked numerically instead of by eye.
 *
 * The copy is resolved through the Chinese table, not baked in: the panel is a template whose defaults
 * are English source strings, and the fixture passes [str] values in exactly as the gallery does, so
 * the render also proves the template follows the language switch.
 */
@OptIn(ExperimentalTestApi::class)
class PanelTemplateRenderTest {

    @Test
    fun renderDark() = runSkikoComposeUiTest(
        size = Size(1272f, 2772f),
        density = Density(3.875f),
    ) {
        setContent {
            COUITheme(colors = darkColorScheme()) {
                CompositionLocalProvider(LocalAppStrings provides AppStrings.Chinese) {
                    Scaffold(containerColor = Color(0xFF141414)) { _ ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            PanelTemplate(
                                show = true,
                                onDismissRequest = {},
                                title = str("Title"),
                                statusText = str("Status text"),
                                settingsText = str("List item"),
                                actionText = str("Button"),
                            )
                        }
                    }
                }
            }
        }
        waitForIdle()
        val image = onRoot().captureToImage().toAwtImage()
        val out = File("/tmp/panel-dark.png")
        ImageIO.write(image, "png", out)
        println("WROTE ${out.absolutePath} ${image.width}x${image.height}")
    }

    /**
     * The top of the gallery, in Chinese: one section heading, then the first entries under it.
     *
     * The headings are the reason this fixture exists — a section heading is 20sp over a 16sp entry
     * title with a 24dp gap above it, and those three numbers are what a screenshot cannot confirm.
     */
    @Test
    fun renderGalleryTop() = runSkikoComposeUiTest(
        size = Size(1272f, 2772f),
        density = Density(3.875f),
    ) {
        setContent {
            COUITheme(colors = darkColorScheme()) {
                CompositionLocalProvider(LocalAppStrings provides AppStrings.Chinese) {
                    Scaffold(containerColor = Color(0xFF141414)) { _ ->
                        val snackbarHostState = remember { SnackbarHostState() }
                        Coui17HomePage(
                            snackbarHostState = snackbarHostState,
                            contentPadding = PaddingValues(top = 24.dp),
                        )
                    }
                }
            }
        }
        waitForIdle()
        val image = onRoot().captureToImage().toAwtImage()
        val out = File("/tmp/gallery-top.png")
        ImageIO.write(image, "png", out)
        println("WROTE ${out.absolutePath} ${image.width}x${image.height}")
    }
}
