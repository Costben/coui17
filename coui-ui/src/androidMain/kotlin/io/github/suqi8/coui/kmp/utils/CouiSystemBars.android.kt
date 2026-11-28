// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun rememberPlatformSystemBarsApplier(): CouiSystemBarsApplier {
    val view = LocalView.current
    return remember(view) {
        var lastStatusVisible: Boolean? = null
        var lastNavVisible: Boolean? = null
        CouiSystemBarsApplier { style ->
            val window = view.context.findSystemBarsActivity()?.window ?: return@CouiSystemBarsApplier
            val decor = window.decorView
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = window.insetsController ?: return@CouiSystemBarsApplier
                val appearance =
                    (if (style.darkStatusBarIcons) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0) or
                        (if (style.darkNavigationBarIcons) WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS else 0)
                val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                controller.setSystemBarsAppearance(appearance, mask)
                if (lastStatusVisible != style.statusBarVisible || lastNavVisible != style.navigationBarVisible) {
                    controller.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    if (style.statusBarVisible) {
                        controller.show(WindowInsets.Type.statusBars())
                    } else {
                        controller.hide(WindowInsets.Type.statusBars())
                    }
                    if (style.navigationBarVisible) {
                        controller.show(WindowInsets.Type.navigationBars())
                    } else {
                        controller.hide(WindowInsets.Type.navigationBars())
                    }
                    lastStatusVisible = style.statusBarVisible
                    lastNavVisible = style.navigationBarVisible
                }
            } else {
                @Suppress("DEPRECATION")
                var flags = decor.systemUiVisibility
                @Suppress("DEPRECATION")
                flags = if (style.darkStatusBarIcons) {
                    flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    @Suppress("DEPRECATION")
                    flags = if (style.darkNavigationBarIcons) {
                        flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    } else {
                        flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                    }
                }
                @Suppress("DEPRECATION")
                flags = if (style.statusBarVisible) {
                    flags and View.SYSTEM_UI_FLAG_FULLSCREEN.inv()
                } else {
                    flags or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                }
                @Suppress("DEPRECATION")
                flags = if (style.navigationBarVisible) {
                    flags and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv()
                } else {
                    flags or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                }
                @Suppress("DEPRECATION")
                decor.systemUiVisibility = flags
                lastStatusVisible = style.statusBarVisible
                lastNavVisible = style.navigationBarVisible
            }
        }
    }
}

private tailrec fun Context.findSystemBarsActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findSystemBarsActivity()
    else -> null
}
