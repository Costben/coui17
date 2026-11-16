// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.effect

import androidx.compose.ui.graphics.NativeCanvas
import kotlin.math.min

/** 普通投影的 caster, 与组件材质的裁剪轮廓分开。 */
internal enum class CouiShadowOutline { Oval, SmoothCapsule, RoundRect }

/** 跨平台投影渲染器; Android API 34+ 使用原生 Mesh/RenderNode, Skiko 使用 Skia 顶点网格与 ImageFilter。 */
internal expect class CouiShadowRenderer(surfaceCount: Int = 1) {
    /** 长度均为像素; 网格包含轴向缩放, Canvas 只承接平移, alpha 不再由外层重复相乘。 */
    fun draw(
        canvas: NativeCanvas,
        slot: Int,
        width: Float,
        height: Float,
        sx: Float,
        sy: Float,
        windowX: Float,
        windowY: Float,
        alpha: Float,
        color: Int,
        elevation: Float,
        lightY: Float,
        lightZ: Float,
        lightRadius: Float,
        outline: CouiShadowOutline,
        radius: Float = min(width, height) * 0.5f,
        clipping: Boolean = true,
    )
}
