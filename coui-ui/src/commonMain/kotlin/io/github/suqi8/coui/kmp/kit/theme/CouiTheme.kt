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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalContentColor

/** Color roles used by COUI kit components; page tinting is provided separately and not mixed into native materials. */
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
    /** Page background separated from dialog/overlay surface; custom palettes fall back to surface. */
    val background: Color = surface,
    /** Card surface for list cards; separated from top-level containers such as menus. */
    val card: Color = container,
)

/** Default blue accent and neutral surfaces for Theme.COUI without reading dynamic wallpaper colors. */
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

/** Default content typography and optional font families for COUI kit components. */
@Immutable
internal data class CouiTypography(
    val body: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        platformStyle = couiDefaultPlatformTextStyle(),
    ),
    val label: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        platformStyle = couiDefaultPlatformTextStyle(),
    ),
    val menuItem: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        platformStyle = couiDefaultPlatformTextStyle(),
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

    /** Maps the active theme to [CouiColorScheme] or falls back to preset light/dark when overridden. */
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

    /** Transparent indicates no extra page hue tinting; explicit material configuration stays with callers. */
    @Composable
    fun materialTintFor(dark: Boolean): Color = Color.Transparent
}
