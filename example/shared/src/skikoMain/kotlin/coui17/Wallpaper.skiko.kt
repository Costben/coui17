// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package coui17

import androidx.compose.runtime.Composable

/** Non-Android targets have no system wallpaper to read; the bundled still is used instead. */
@Composable
internal actual fun rememberSystemWallpaper(): Wallpaper? = null
