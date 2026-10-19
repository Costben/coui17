// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.ComposeViewport
import coui17.Coui17App
import kotlinx.browser.document
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import utils.WallpaperState
import webfont.preloadWebFonts
import webfont.queryParam
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val DEFAULT_CSS_URL =
    "https://cdn-font.hyperos.mi.com/font/css?family=MiSans_VF:VF:Chinese_Simplify&display=swap"

// Safety cap on how long the loading overlay waits for fonts; afterwards it hides and any remaining
// subsets keep downloading in the background.
private const val LOADING_FONT_TIMEOUT_MS = 10_000L

@OptIn(ExperimentalComposeUiApi::class, ExperimentalEncodingApi::class)
fun main() {
    installWallpaperPicker()
    val cssUrl = queryParam("cssUrl").ifBlank { DEFAULT_CSS_URL }
    ComposeViewport(viewportContainerId = "composeApp") {
        var insetTopPx by remember { mutableDoubleStateOf(0.0) }
        var insetBottomPx by remember { mutableDoubleStateOf(0.0) }
        var insetStartPx by remember { mutableDoubleStateOf(0.0) }
        var insetEndPx by remember { mutableDoubleStateOf(0.0) }

        LaunchedEffect(Unit) {
            insetTopPx = platformGetCssVar("--safe-area-inset-top")
            insetStartPx = platformGetCssVar("--safe-area-inset-left")
            insetEndPx = platformGetCssVar("--safe-area-inset-right")
            insetBottomPx = platformGetCssVar("--safe-area-inset-bottom")
        }

        val safePadding = PaddingValues(
            top = Dp(insetTopPx.toFloat()),
            bottom = Dp(insetBottomPx.toFloat()),
            start = Dp(insetStartPx.toFloat()),
            end = Dp(insetEndPx.toFloat()),
        )

        // Register each CJK subset with the shared resolver so the whole app picks the glyphs up
        // through the global font fallback (no per-text wrappers), while feeding download progress
        // into the overlay's font bar. Runs in an outer scope, so it survives the overlay's timeout.
        val fontFamilyResolver = LocalFontFamilyResolver.current
        val scope = rememberCoroutineScope()
        var fontsReady by remember { mutableStateOf(false) }
        var fontGeneration by remember { mutableIntStateOf(0) }
        LaunchedEffect(cssUrl, fontFamilyResolver) {
            withFrameNanos {} // let the first frame paint behind the overlay
            val preload = scope.launch {
                preloadWebFonts(cssUrl, fontFamilyResolver) { done, total ->
                    platformSetFontProgress(done, total)
                }
            }
            // Hold the app back until the CJK fallback is registered: text laid out before that is
            // never re-laid out, so the first screen would stay as tofu.
            val fontsLoaded = withTimeoutOrNull(LOADING_FONT_TIMEOUT_MS) { preload.join() } != null
            fontsReady = true
            platformHideLoading()
            if (!fontsLoaded) {
                // The app is already on screen with fallback fonts. Once the late subsets land,
                // rebuild the tree once so text composed from now on uses them. Trade-off: the app
                // state resets once, and only on slow networks where the timeout fires.
                preload.join()
                fontGeneration++
            }
        }

        if (fontsReady) {
            key(fontGeneration) {
                Box(Modifier.fillMaxSize().padding(safePadding)) {
                    Coui17App()
                }
            }
        }
    }
}

private fun installWallpaperPicker() {
    val input = document.getElementById("wallpaperInput") as? HTMLInputElement ?: return
    val clear = document.getElementById("clearWallpaper")
    input.onchange = {
        input.files?.item(0)?.let { file ->
            val reader = org.w3c.files.FileReader()
            reader.onload = {
                val dataUrl = reader.result?.toString()
                if (dataUrl != null) {
                    runCatching {
                        val bytes = Base64.decode(dataUrl.substringAfter(','))
                        WallpaperState.bitmap = org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
                    }.onSuccess {
                        setWallpaperStatus("Active")
                    }.onFailure {
                        setWallpaperStatus("Invalid image")
                    }
                }
                null
            }
            reader.readAsDataURL(file)
        }
        null
    }
    clear?.addEventListener("click", {
        WallpaperState.bitmap = null
        input.value = ""
        setWallpaperStatus("Default")
    })
}

private fun setWallpaperStatus(status: String) {
    (document.getElementById("wallpaperStatus") as? HTMLElement)?.textContent = status
}
