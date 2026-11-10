// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalCouiBackdrop = staticCompositionLocalOf<CouiBackdrop?> { null }

/** 提供页面正文采样源；宿主仍须将 couiBackdrop 挂到唯一正文节点，并排除其消费者。 */
@Composable
internal fun ProvideCouiBackdrop(backdrop: CouiBackdrop, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCouiBackdrop provides backdrop, content = content)
}
