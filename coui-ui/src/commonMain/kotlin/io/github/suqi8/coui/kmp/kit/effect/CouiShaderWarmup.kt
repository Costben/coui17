// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.effect

import androidx.compose.runtime.Composable

/**
 * Staggered one-shot GPU shader pipeline warmup hook invoked inside `COUITheme`.
 * Active on Apple Metal targets (iOS/macOS) to eliminate first-animation PSO compilation stalls;
 * no-op on Android, Desktop, and Web.
 */
@Composable
internal expect fun CouiShaderWarmup()
