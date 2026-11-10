// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalContentColor

/** 组件实际使用的色彩角色；页面染色由主题单独提供，不混入原生材质。 */
@Immutable
internal data class CouiColorScheme(
    val accent: Color,
    val onAccent: Color,
    val surface: Color,
    val container: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outlineVariant: Color,
    val accentOnSurface: Color = accent,
    val menuSurface: Color = container,
    val checkboxAccent: Color = accent,
    val menuCheckboxAccent: Color = accentOnSurface,
    /** 页面底色与弹窗等组件的 surface 分开；自定义色板缺省沿用 surface。 */
    val background: Color = surface,
    /** 列表卡片使用 couiColorCardBackground；与菜单等顶层容器分开。 */
    val card: Color = container,
)

/** Theme.COUI 的蓝色强调色与中性表面；不读取壁纸动态色。 */
internal object CouiPresets {
    val Light = CouiColorScheme(
        accent = CouiColor.Day.BLUE,
        onAccent = Color.White,
        surface = Color.White,
        container = Color.White,
        onSurface = Color(0xE6000000),
        onSurfaceVariant = Color(0x8A000000),
        outlineVariant = Color(0x1F000000),
        background = Color(0xFFF0F1F2),
    )
    val Dark = CouiColorScheme(
        accent = CouiColor.Night.BLUE,
        onAccent = Color.White,
        surface = Color(0xFF1E1E1E),
        container = Color(0xFF333333),
        onSurface = Color(0xE6FFFFFF),
        onSurfaceVariant = Color(0x8AFFFFFF),
        outlineVariant = Color(0x33FFFFFF),
        accentOnSurface = Color(0xFF5C9DFF),
        background = Color.Black,
        card = Color(0x1AFFFFFF),
    )
}

/** 普通内容预设及可选字体族；Dialog 保留字号与行距，M3 仅适配字体族，native 保留系统字体。 */
@Immutable
internal data class CouiTypography(
    val body: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    ),
    val label: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    ),
    val menuItem: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    ),
    val chipLabelFontFamily: FontFamily? = null,
    val dialogTitleFontFamily: FontFamily? = null,
    val dialogMessageFontFamily: FontFamily? = null,
    val dialogButtonFontFamily: FontFamily? = null,
    val dialogInputFontFamily: FontFamily? = null,
    val dialogCounterFontFamily: FontFamily? = null,
)

internal val LocalCouiDarkTheme = staticCompositionLocalOf<Boolean?> { null }
internal val LocalCouiContentColor = staticCompositionLocalOf { Color.Unspecified }
internal val LocalCouiTextStyle = staticCompositionLocalOf { CouiTypography().body }

internal object CouiTheme {
    val isDark: Boolean
        @Composable get() = COUITheme.colorScheme.background.luminance() < 0.5f

    val colorScheme: CouiColorScheme
        @Composable get() = colorSchemeFor(isDark)

    internal val contentColor: Color
        @Composable get() = LocalContentColor.current

    val typography: CouiTypography
        @Composable get() = CouiTypography()

    /** 组件覆盖深浅色时，映射到 COUITheme.colorScheme。 */
    @Composable
    fun colorSchemeFor(dark: Boolean): CouiColorScheme {
        if (dark != isDark) {
            return if (dark) CouiPresets.Dark else CouiPresets.Light
        }
        val cs = COUITheme.colorScheme
        return CouiColorScheme(
            accent = cs.primary,
            onAccent = cs.onPrimary,
            surface = cs.surface,
            container = cs.surfaceContainer,
            onSurface = cs.onSurface,
            onSurfaceVariant = cs.onSurfaceSecondary,
            outlineVariant = cs.outline,
            accentOnSurface = cs.primary,
            menuSurface = cs.surfaceContainer,
            checkboxAccent = cs.primary,
            menuCheckboxAccent = cs.primary,
            background = cs.background,
            card = cs.surfaceContainer,
        )
    }

    /** 透明表示不做额外页面色相染色；显式材质配置始终由调用方负责。 */
    @Composable
    fun materialTintFor(dark: Boolean): Color = Color.Transparent
}
