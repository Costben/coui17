// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.topappbar

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import io.github.suqi8.coui.kmp.kit.effect.CouiFloatingMaterial
import io.github.suqi8.coui.kmp.kit.effect.CouiFloatingShadow

internal data class CouiToolbarStyle(
    val dark: Boolean,
    val circleShape: Shape,
    val capsuleShape: Shape,
    val material: (capsule: Boolean) -> CouiFloatingMaterial,
    val shadow: (capsule: Boolean) -> CouiFloatingShadow,
    val contentColor: Color,
    val textStyle: TextStyle,
    val cursorColor: Color,
    val contourGlowEnabled: Boolean,
)

internal val LocalCouiToolbarStyle = staticCompositionLocalOf<CouiToolbarStyle> {
    error("顶栏样式必须由组件入口提供")
}
