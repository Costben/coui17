// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap

/** Shared wallpaper selected by the web preview. Other targets leave it unset. */
object WallpaperState {
    var bitmap: ImageBitmap? by mutableStateOf(null)
}
