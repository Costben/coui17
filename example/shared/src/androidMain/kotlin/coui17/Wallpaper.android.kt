// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package coui17

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/** Keeps the decoded wallpaper within a sane memory budget while still filling a phone screen. */
private const val MAX_WALLPAPER_EDGE = 1800

/**
 * Reads the home or lock wallpaper through [WallpaperManager].
 *
 * Reading a wallpaper needs no permission on API < 34; from API 34 the lookup is gated behind
 * `READ_WALLPAPER_INTERNAL`, which only system apps hold, so the calls are suppressed and left
 * best-effort. A live wallpaper has no static image at all either: the system state then reports an
 * engine and an empty wallpaper file, and both [WallpaperManager] lookups answer `null`.
 */
@SuppressLint("MissingPermission")
@Composable
internal actual fun rememberSystemWallpaper(): Wallpaper? {
    val context = LocalContext.current
    return remember(context) {
        val manager = WallpaperManager.getInstance(context)
        readWallpaper { manager.drawable }?.let {
            return@remember Wallpaper(it, WallpaperSource.System)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            readWallpaper { manager.getDrawable(WallpaperManager.FLAG_LOCK) }?.let {
                return@remember Wallpaper(it, WallpaperSource.Lock)
            }
        }
        null
    }
}

private inline fun readWallpaper(load: () -> Drawable?): ImageBitmap? = runCatching { load()?.toImageBitmap() }.getOrNull()

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
