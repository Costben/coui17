// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.topappbar

import androidx.compose.runtime.Stable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection

/** Collapses the top bar before content scrolls and expands once content reaches the top; attach to one ancestor of the scrollable container. */
@Stable
internal class CouiTopAppBarScrollBehavior internal constructor(val state: CouiCollapsingAppBarState) {
    val nestedScrollConnection: NestedScrollConnection get() = state.nestedScrollConnection
}
