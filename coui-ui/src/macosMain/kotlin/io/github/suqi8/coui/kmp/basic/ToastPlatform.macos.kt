// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable

private val NoOpLockedNavBarInsetsReader: () -> ToastSystemBarInsetsPx? = { null }

internal actual fun platformNeedsCompositionNavBarObserver(): Boolean = true

@Composable
internal actual fun rememberPlatformLockedNavigationBarInsetsPx(): () -> ToastSystemBarInsetsPx? = NoOpLockedNavBarInsetsReader
