// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.RoundedCorner
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

actual fun platform(): Platform = Platform.Android

actual val hasFocusReassignBug: Boolean = Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1

@Composable
actual fun getRoundedCorner(): Dp = getSystemCornerRadius()

// Represents a rounded corner of the display.
@SuppressLint("NewApi")
@Composable
private fun getSystemCornerRadius(): Dp {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val insets = LocalView.current.rootWindowInsets

    val roundedCornerRadius = remember(context, insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            insets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius
                ?.takeIf { it > 0 }
                ?: getCornerRadiusBottom(context)
        } else {
            getCornerRadiusBottom(context)
        }
    }
    return (roundedCornerRadius / density).dp
}

// from https://dev.mi.com/distribute/doc/details?pId=1631
@SuppressLint("DiscouragedApi")
fun getCornerRadiusBottom(context: Context): Int {
    val resourceId = context.resources.getIdentifier("rounded_corner_radius_bottom", "dimen", "android")
    return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
}

@Composable
actual fun platformDialogProperties(): DialogProperties = DialogProperties(
    dismissOnBackPress = false,
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false,
)

@Composable
actual fun RemovePlatformDialogDefaultEffects() {
    val parent = LocalView.current.parent
    val context = LocalContext.current
    DisposableEffect(parent, context) {
        val provider = parent as? DialogWindowProvider
        val window = provider?.window
        window?.setWindowAnimations(0)
        window?.setDimAmount(0f)
        window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        val hostWindow = context.findActivity()?.window
        if (window != null && hostWindow != null) {
            copySystemBarAppearance(from = hostWindow, to = window)
        }
        onDispose {
            // No-op
        }
    }
}

@Composable
actual fun DialogTouchPassThrough(passTouches: () -> Boolean) {
    val parent = LocalView.current.parent
    LaunchedEffect(parent) {
        val window = (parent as? DialogWindowProvider)?.window ?: return@LaunchedEffect
        snapshotFlow(passTouches).collect { enabled ->
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
        }
    }
}

/**
 * The dialog window draws edge to edge and so takes over the system bars, but it starts from the
 * platform default appearance (light icons). Copy the host activity's light status / navigation bar
 * flags so the bar icons keep the colour the page under the dialog was given.
 */
@Suppress("DEPRECATION")
private fun copySystemBarAppearance(from: Window, to: Window) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        val appearance = from.insetsController?.systemBarsAppearance ?: return
        to.insetsController?.setSystemBarsAppearance(appearance and mask, mask)
    } else {
        var mask = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mask = mask or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        val source = from.decorView.systemUiVisibility and mask
        val target = to.decorView
        target.systemUiVisibility = (target.systemUiVisibility and mask.inv()) or source
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
