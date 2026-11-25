// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

/**
 * Returns a [Modifier] that ensures the entire input component (including its underline, card
 * chrome, or error caption plus [bottomMargin] of breathing room) scrolls cleanly above the soft
 * keyboard whenever it gains focus or the IME inset changes while focused.
 */
@Composable
internal fun rememberImeBringIntoViewModifier(
    isFocused: Boolean,
    bottomMargin: Dp = DefaultImeBringIntoViewMargin,
): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    var size by remember { mutableStateOf(IntSize.Zero) }
    val marginPx = with(density) { bottomMargin.toPx() }

    LaunchedEffect(isFocused, imeInsets, density, marginPx) {
        if (!isFocused) return@LaunchedEffect
        var lastImeBottom = -1
        snapshotFlow { imeInsets.getBottom(density) }
            .collectLatest { imeBottom ->
                if (imeBottom > 0 && imeBottom != lastImeBottom) {
                    lastImeBottom = imeBottom
                    // Wait one frame so ancestor imePadding() finishes remeasuring the scrollable viewport.
                    withFrameNanos { }
                    val currentSize = size
                    if (currentSize.width > 0 && currentSize.height > 0) {
                        requester.bringIntoView(
                            Rect(
                                left = 0f,
                                top = 0f,
                                right = currentSize.width.toFloat(),
                                bottom = currentSize.height.toFloat() + marginPx,
                            ),
                        )
                    }
                }
            }
    }

    return remember(requester) {
        Modifier
            .bringIntoViewRequester(requester)
            .onSizeChanged { size = it }
    }
}

private val DefaultImeBringIntoViewMargin = 16.dp
