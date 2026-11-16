// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.backdrop

import android.view.ViewTreeObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.node.requireView

internal actual fun GraphicsLayer.backdropLayerId(): Long = layerId

internal actual fun GraphicsLayer.backdropOwnerViewId(): Long = ownerViewId

internal actual class CouiBackdropPlatformObserver actual constructor() :
    ViewTreeObserver.OnPreDrawListener,
    ViewTreeObserver.OnWindowFocusChangeListener {
    private var observer: ViewTreeObserver? = null
    private var onPreDrawCallback: (() -> Unit)? = null
    private var onWindowFocusChangedCallback: ((Boolean) -> Unit)? = null

    actual fun attach(
        node: Modifier.Node,
        onPreDraw: () -> Unit,
        trackWindowFocus: Boolean,
        onWindowFocusChanged: (Boolean) -> Unit,
    ) {
        onPreDrawCallback = onPreDraw
        onWindowFocusChangedCallback = onWindowFocusChanged
        // Inside dialogs, resolve the View from the attached node instead of the outer Composition LocalView.
        observer = node.requireView().viewTreeObserver.also {
            it.addOnPreDrawListener(this)
            if (trackWindowFocus) {
                it.addOnWindowFocusChangeListener(this)
            }
        }
    }

    actual fun detach(node: Modifier.Node) {
        val current = observer?.takeIf { it.isAlive } ?: node.requireView().viewTreeObserver
        current.removeOnPreDrawListener(this)
        current.removeOnWindowFocusChangeListener(this)
        observer = null
        onPreDrawCallback = null
        onWindowFocusChangedCallback = null
    }

    override fun onPreDraw(): Boolean {
        onPreDrawCallback?.invoke()
        return true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        onWindowFocusChangedCallback?.invoke(hasFocus)
    }
}
