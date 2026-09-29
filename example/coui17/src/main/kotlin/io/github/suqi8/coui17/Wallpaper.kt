// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/** Keeps the decoded wallpaper within a sane memory budget while still filling a phone screen. */
private const val MAX_WALLPAPER_EDGE = 1800

/**
 * The picture the ColorOS 17 glass samples.
 *
 * The device wallpaper is read through [WallpaperManager], which needs no permission; when it is a
 * live wallpaper, or reading fails, the bundled still is used so the light-sensing material always
 * has something to refract.
 */
@Composable
internal fun rememberWallpaper(): ImageBitmap {
    val context = LocalContext.current
    return remember(context) {
        val fromDevice = runCatching {
            WallpaperManager.getInstance(context).drawable?.toImageBitmap()
        }.getOrNull()
        fromDevice
            ?: runCatching {
                BitmapFactory.decodeResource(context.resources, R.drawable.blur_test_bg)?.asImageBitmap()
            }.getOrNull()
            ?: ImageBitmap(1, 1)
    }
}

private fun Drawable.toImageBitmap(): ImageBitmap? {
    val width = intrinsicWidth
    val height = intrinsicHeight
    if (width <= 0 || height <= 0) return null
    val scale = (MAX_WALLPAPER_EDGE.toFloat() / maxOf(width, height)).coerceAtMost(1f)
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    setBounds(0, 0, targetWidth, targetHeight)
    draw(Canvas(bitmap))
    return bitmap.asImageBitmap()
}
