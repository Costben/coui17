// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.github.suqi8.coui.kmp.blur.Backdrop

@Composable
internal actual fun shouldUseToolbarSurfaceIconButton(
    size: Dp,
    backdrop: Backdrop?,
    spotLightEnabled: Boolean,
): Boolean = false

@Composable
internal actual fun ToolbarSurfaceIconButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    content: @Composable BoxScope.() -> Unit,
) = Unit
