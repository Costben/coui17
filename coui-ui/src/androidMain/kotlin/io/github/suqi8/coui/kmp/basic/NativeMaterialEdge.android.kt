// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import io.github.suqi8.coui.kmp.shader.RuntimeShader
import io.github.suqi8.coui.kmp.shader.asAndroidRuntimeShader

/** Only reachable when [io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported] holds. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal actual fun RuntimeShader.nativeMaterialEdgeEffect(): RenderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(
    asAndroidRuntimeShader(),
    "child",
).asComposeRenderEffect()
