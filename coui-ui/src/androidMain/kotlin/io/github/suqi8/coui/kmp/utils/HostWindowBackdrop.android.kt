// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(InternalCouiBlurApi::class)

package io.github.suqi8.coui.kmp.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.ImageBackdrop
import io.github.suqi8.coui.kmp.blur.InternalCouiBlurApi
import io.github.suqi8.coui.kmp.blur.isBlurEnabled

/**
 * Downscale of the capture. The panels blur it by 200–400 device pixels, far below what a quarter
 * resolution still resolves, and a quarter keeps a 1240 x 2772 page under 1 MB.
 */
private const val CAPTURE_DOWNSCALE = 4

@Composable
internal actual fun rememberHostWindowBackdrop(active: Boolean): State<Backdrop?> {
    val state = remember { mutableStateOf<Backdrop?>(null) }
    val view = LocalView.current
    // The glass needs runtime shaders (API 33); below that, or with blur off, the panel keeps its
    // flat fill and a capture would only cost memory.
    val capture = active && isBlurEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    DisposableEffect(capture, view) {
        var disposed = false
        val window = if (capture) view.context.findActivity()?.window else null
        val decor = window?.peekDecorView()
        if (window != null && decor != null && decor.width > 0 && decor.height > 0) {
            val sourceSize = IntSize(decor.width, decor.height)
            val origin = IntArray(2).also(decor::getLocationOnScreen)
            val bitmap = Bitmap.createBitmap(
                (sourceSize.width / CAPTURE_DOWNSCALE).coerceAtLeast(1),
                (sourceSize.height / CAPTURE_DOWNSCALE).coerceAtLeast(1),
                Bitmap.Config.ARGB_8888,
            )
            try {
                PixelCopy.request(
                    window,
                    bitmap,
                    { result ->
                        if (!disposed && result == PixelCopy.SUCCESS) {
                            state.value = ImageBackdrop(
                                image = bitmap.asImageBitmap(),
                                sourceSize = sourceSize,
                                sourceOriginOnScreen = Offset(origin[0].toFloat(), origin[1].toFloat()),
                            )
                        }
                    },
                    Handler(Looper.getMainLooper()),
                )
            } catch (_: IllegalArgumentException) {
                // The window has no surface yet; the panel keeps the inherited backdrop.
            }
        }
        onDispose {
            disposed = true
            state.value = null
        }
    }
    return state
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
