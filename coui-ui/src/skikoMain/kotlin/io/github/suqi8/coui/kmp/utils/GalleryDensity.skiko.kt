// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.Composable

/** Desktop and Apple/web targets have no ColorOS display-size override; use the provided density. */
@Composable
internal actual fun platformGalleryDensityDpi(): Int? = null
