// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

/** Which picture ended up behind the glass. */
internal enum class WallpaperSource {
    /** The system (home screen) wallpaper. */
    System,

    /** The lock screen wallpaper, used when the system one has no static image. */
    Lock,

    /** The still bundled with the app. */
    Bundled,
}

internal data class Wallpaper(
    val image: ImageBitmap,
    val source: WallpaperSource,
)

/**
 * The picture the ColorOS 17 glass samples.
 *
 * Reading a wallpaper needs no permission, but a live wallpaper has no static image at all: the
 * system state then reports an engine and an empty wallpaper file, and both [WallpaperManager]
 * lookups answer `null`. The bundled still is the last resort so the light-sensing material always
 * has something to refract.
 */
@Composable
internal fun rememberWallpaper(): Wallpaper {
    val context = LocalContext.current
    return remember(context) {
        val manager = WallpaperManager.getInstance(context)
        readWallpaper { manager.drawable }?.let {
            return@remember Wallpaper(it, WallpaperSource.System)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            readWallpaper { manager.getDrawable(WallpaperManager.FLAG_LOCK) }?.let {
                return@remember Wallpaper(it, WallpaperSource.Lock)
            }
        }
        val bundled = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.coui17_wallpaper)?.asImageBitmap()
        }.getOrNull()
        Wallpaper(bundled ?: ImageBitmap(1, 1), WallpaperSource.Bundled)
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
