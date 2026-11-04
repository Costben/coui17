// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode

@Composable
@ReadOnlyComposable
internal actual fun isSystemBlurEnabled(): Boolean = SystemBlurState.overrideForTest ?: true

internal actual fun CompositionLocalConsumerModifierNode.readSystemBlurEnabled(): Boolean = SystemBlurState.overrideForTest ?: true
