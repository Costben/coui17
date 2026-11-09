// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.github.suqi8.coui.kmp.blur.Backdrop

@Composable
internal actual fun rememberHostWindowBackdrop(active: Boolean): State<Backdrop?> {
    // Dialogs are layered inside the host window, so the inherited backdrop already samples the page.
    return remember { mutableStateOf(null) }
}
