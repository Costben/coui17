// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.kit.topappbar.CouiTopAppBarButtonDefaults
import io.github.suqi8.coui.kmp.kit.topappbar.LocalCouiToolbarBackdrop
import io.github.suqi8.coui.kmp.kit.topappbar.TopAppBarButton

@Composable
internal actual fun shouldUseToolbarSurfaceIconButton(
    size: Dp,
    backdrop: Backdrop?,
    spotLightEnabled: Boolean,
): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
    LocalCouiToolbarBackdrop.current != null &&
    backdrop == null &&
    spotLightEnabled &&
    size == HighlightedIconButtonDefaults.Size

@Composable
internal actual fun ToolbarSurfaceIconButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    TopAppBarButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        enforceMinimumTouchTarget = false,
        style = CouiTopAppBarButtonDefaults.style(search = true),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
