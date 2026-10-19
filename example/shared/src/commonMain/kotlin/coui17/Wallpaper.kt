// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package coui17

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.coui17_wallpaper
import org.jetbrains.compose.resources.imageResource
import utils.WallpaperState

/** Which picture ended up behind the glass. */
internal enum class WallpaperSource {
    /** The system (home screen) wallpaper. */
    System,

    /** The lock screen wallpaper, used when the system one has no static image. */
    Lock,

    /** The image the web preview's wallpaper picker chose. */
    Picked,

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
 * Resolution order: the system wallpaper (platform-provided, `null` where there is none), then the
 * image picked in the web preview, then the bundled still. The bundled still is the last resort so
 * the light-sensing material always has something to refract.
 */
@Composable
internal fun rememberWallpaper(): Wallpaper {
    // Only the branch that wins is read, so the bundled still is never decoded where the device
    // already supplies a wallpaper.
    rememberSystemWallpaper()?.let { return it }
    WallpaperState.bitmap?.let { picked ->
        return remember(picked) { Wallpaper(picked, WallpaperSource.Picked) }
    }
    val bundled = imageResource(Res.drawable.coui17_wallpaper)
    return remember(bundled) { Wallpaper(bundled, WallpaperSource.Bundled) }
}

/** The platform's own wallpaper, or `null` when the platform has no readable static image. */
@Composable
internal expect fun rememberSystemWallpaper(): Wallpaper?
