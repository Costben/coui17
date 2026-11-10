// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.topappbar

import androidx.compose.runtime.Stable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection

/** 先折叠顶栏，正文到顶后再展开；连接只挂到实际滚动容器的一个祖先。 */
@Stable
internal class CouiTopAppBarScrollBehavior internal constructor(val state: CouiCollapsingAppBarState) {
    val nestedScrollConnection: NestedScrollConnection get() = state.nestedScrollConnection
}
