// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.kit.shape.CouiCapsuleShape
import io.github.suqi8.coui.kmp.kit.theme.CouiTheme

@Immutable
internal data class CouiSearchBarColors(
    val foreground: Color,
    val secondary: Color,
    /** Plain solid background on API 31-33; API 34+ uses native material rendering. */
    val fallbackBackground: Color,
    val clear: Color,
    val icon: Color = secondary,
)

/** Defaults for the instant-search SettingsSearchBar variant. Layout metrics use dp; extra motion padding retains px. */
internal object CouiSearchBarDefaults {
    @Composable
    fun colors(dark: Boolean = CouiTheme.isDark): CouiSearchBarColors {
        val theme = CouiTheme.colorSchemeFor(dark)
        return CouiSearchBarColors(
            theme.onSurface,
            theme.onSurfaceVariant,
            theme.container,
            theme.onSurface.copy(alpha = if (dark) .3f else .26f),
        )
    }

    val height = 52.dp
    val surfaceHeight = 40.dp
    val horizontalMargin = 16.dp
    val cancelGap = 12.dp
    val cancelPadding = 16.dp
    val cancelMaxWidth = 152.dp
    val innerIconSlot = 36.dp
    val iconSize = 20.dp
    val iconStart = 4.dp
    val textEnd = 4.dp
    val shape = CouiCapsuleShape
    internal const val cancelTranslationPx = 20f

    // TYPE_FRAMEWORK_TOP_BAR_BLUR; raw pixel radius without extra density scaling.
    internal const val blurRadius = 20f
    internal const val cancelContentBlurRadius = 30f
}
