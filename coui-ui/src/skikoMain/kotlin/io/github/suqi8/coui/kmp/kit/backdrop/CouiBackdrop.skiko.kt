// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.backdrop

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.layer.GraphicsLayer

internal actual fun GraphicsLayer.backdropLayerId(): Long = -1L

internal actual fun GraphicsLayer.backdropOwnerViewId(): Long = -1L

internal actual class CouiBackdropPlatformObserver actual constructor() {
    actual fun attach(
        node: Modifier.Node,
        onPreDraw: () -> Unit,
        trackWindowFocus: Boolean,
        onWindowFocusChanged: (Boolean) -> Unit,
    ) = Unit

    actual fun detach(node: Modifier.Node) = Unit
}
