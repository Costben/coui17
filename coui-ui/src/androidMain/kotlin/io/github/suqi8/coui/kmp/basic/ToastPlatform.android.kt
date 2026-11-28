// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import android.view.WindowInsets as AndroidWindowInsets

internal actual fun platformNeedsCompositionNavBarObserver(): Boolean = false

@Composable
internal actual fun rememberPlatformLockedNavigationBarInsetsPx(): () -> ToastSystemBarInsetsPx? {
    val view = LocalView.current
    return remember(view) {
        {
            val rootInsets = view.rootWindowInsets
            when {
                rootInsets == null -> null

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    val nav = rootInsets.getInsetsIgnoringVisibility(AndroidWindowInsets.Type.navigationBars())
                    ToastSystemBarInsetsPx(
                        left = nav.left.coerceAtLeast(0),
                        right = nav.right.coerceAtLeast(0),
                        bottom = nav.bottom.coerceAtLeast(0),
                    )
                }

                else -> {
                    @Suppress("DEPRECATION")
                    ToastSystemBarInsetsPx(
                        left = rootInsets.stableInsetLeft.coerceAtLeast(0),
                        right = rootInsets.stableInsetRight.coerceAtLeast(0),
                        bottom = rootInsets.stableInsetBottom.coerceAtLeast(0),
                    )
                }
            }
        }
    }
}
