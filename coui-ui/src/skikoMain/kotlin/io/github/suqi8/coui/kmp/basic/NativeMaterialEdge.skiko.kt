// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import io.github.suqi8.coui.kmp.shader.RuntimeShader
import io.github.suqi8.coui.kmp.shader.asSkikoRuntimeShader
import org.jetbrains.skia.ImageFilter

internal actual fun RuntimeShader.nativeMaterialEdgeEffect(): RenderEffect = ImageFilter.makeRuntimeShader(
    asSkikoRuntimeShader(),
    "child",
    null,
).asComposeRenderEffect()
