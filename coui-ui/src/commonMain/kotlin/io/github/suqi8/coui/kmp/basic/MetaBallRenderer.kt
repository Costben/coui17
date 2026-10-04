// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

internal interface MetaBallRenderer {
    @Composable
    fun Draw(
        particles: List<MetaBallParticle>,
        body: Color,
        rim: Color,
        extension: Dp,
        factorX: Float,
        factorY: Float,
        modifier: Modifier,
        fallback: @Composable () -> Unit,
    )
}

/** Remembers a renderer backed by the platform's ColorOS material chain when available. */
@Composable
internal expect fun rememberPlatformMetaBallRenderer(): MetaBallRenderer?
