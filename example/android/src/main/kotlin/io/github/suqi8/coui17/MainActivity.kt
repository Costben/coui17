// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import coui17.Coui17App
import io.github.suqi8.coui.kmp.utils.rememberGalleryDensity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voteHighRefreshRate()

        setContent {
            var colorMode by remember { mutableIntStateOf(0) }
            val darkMode = when (colorMode) {
                2, 5 -> true
                0, 3 -> isSystemInDarkTheme()
                else -> false
            }

            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkMode },
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false // Xiaomi moment, this code must be here
                }

                onDispose {}
            }

            // ColorOS pins every system app (Gallery, Settings, the launcher) to a 360dp short
            // edge, so they ignore the display-size setting entirely. Pin the whole demo the same
            // way: with this, every control lands on the same pixels ColorOS uses, at any
            // user-selected density. Remove it and the demo scales with the system again.
            CompositionLocalProvider(LocalDensity provides rememberGalleryDensity()) {
                Coui17App(onColorModeChange = { colorMode = it })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        voteHighRefreshRate()
    }

    /**
     * Asks the display to run this window at the panel's high refresh rate.
     *
     * Left without a preference the window carries no frame-rate vote, and the OEM's adaptive
     * refresh policy settles the whole screen on 90Hz: frames then land 11ms apart on a panel that
     * ticks every 8.3ms, so a scroll advances in visible steps next to a system app. The gallery is
     * nothing but scrolling lists, so it asks for 120Hz outright — the frame budget (roughly 4.5ms
     * of UI and draw work) leaves room for it.
     */
    private fun voteHighRefreshRate() {
        val decor = window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            decor.setRequestedFrameRate(HIGH_REFRESH_RATE)
        } else {
            // The window-level preference is the only public route before the view-level one.
            @Suppress("DEPRECATION")
            window.attributes = window.attributes.apply { preferredRefreshRate = HIGH_REFRESH_RATE }
        }
    }

    private companion object {
        /** The panel's default mode; 165Hz is also offered but leaves no frame-budget margin. */
        const val HIGH_REFRESH_RATE = 120f
    }
}
