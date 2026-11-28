// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.Composable

private val NoOpSystemBarsApplier = CouiSystemBarsApplier {}

@Composable
internal actual fun rememberPlatformSystemBarsApplier(): CouiSystemBarsApplier = NoOpSystemBarsApplier
