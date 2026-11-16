// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.effect

import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import io.github.suqi8.coui.kmp.shader.RuntimeShader
import kotlin.math.ceil

/** 随显示密度缓存平台模糊, 不在滚动时重建滤镜。 */
internal class CouiCoverageBlurEffect(radiusPx: Float) {
    val padding: Float = ceil(radiusPx)
    private val blur: RenderEffect = createBlurRenderEffect(radiusPx, radiusPx, TileMode.Decal)

    val composeEffect: RenderEffect = if (isKitRuntimeShaderSupported()) {
        normalizeCoverage(blur)
    } else {
        blur
    }

    val effect: PlatformRenderEffect = composeEffect.asPlatformRenderEffect()
}

/**
 * 顶部栏采样包含不透明页面背景, 平台模糊在屏幕边缘会混入透明外沿。
 * 在渐隐遮罩之前恢复有效样本的颜色与覆盖度, 避免原始正文从顶边透出。
 * 此处只读取一次已模糊结果, 卷积与降采样均交由平台执行。
 */
private fun normalizeCoverage(blur: RenderEffect): RenderEffect {
    val shader = RuntimeShader(
        """
            uniform shader content;

            float4 main(float2 coord) {
                float4 blurred = content.eval(coord);
                if (blurred.a > 0.0) {
                    return float4(blurred.rgb / blurred.a, 1.0);
                }
                return float4(0.0);
            }
        """.trimIndent(),
    )
    return createChainRenderEffect(
        shader.asComposeRenderEffect("content"),
        blur,
    )
}
