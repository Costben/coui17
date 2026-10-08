// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import appstate.AppState
import appstate.LocalAppState
import component.NotificationSwipeDemo
import i18n.AppStrings
import i18n.LocalAppStrings
import io.github.suqi8.coui.kmp.theme.COUITheme

/** Debug-only entry point for repeating the native notification swipe on a physical device. */
class NotificationSwipeComparisonActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalAppState provides AppState(), LocalAppStrings provides AppStrings.Chinese) {
                COUITheme { NotificationSwipeDemo() }
            }
        }
    }
}
