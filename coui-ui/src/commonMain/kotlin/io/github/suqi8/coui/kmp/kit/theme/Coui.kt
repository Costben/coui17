// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import kotlin.concurrent.Volatile
import kotlin.math.roundToInt

/** Process-level default policy. Initialized once at startup without retaining host theme or Context. */
@OptIn(InternalCoroutinesApi::class)
internal object Coui {
    private val lock = SynchronizedObject()

    @Volatile
    private var initialized = false

    @Volatile
    internal var defaultStyle: CouiStyle = NativeCouiStyle
        private set

    fun initialize(defaultStyle: CouiStyle) {
        synchronized(lock) {
            check(!initialized) { "Coui is already initialized and cannot set defaultStyle again" }
            this.defaultStyle = defaultStyle
            initialized = true
        }
    }
}

/** Optional adapter contract resolving theme defaults, content scope, and minimum touch target size. */
internal interface CouiStyle {
    @Composable
    fun themeDefaults(dark: Boolean): CouiThemeDefaults

    @Composable
    fun ProvideContent(color: Color, textStyle: TextStyle?, mergeTextStyle: Boolean, content: @Composable () -> Unit)

    @Composable
    fun minimumInteractiveComponentSize(): Modifier
}

@Immutable
internal data class CouiThemeDefaults(
    val colorScheme: CouiColorScheme,
    val typography: CouiTypography = CouiTypography(),
    val materialTint: Color = Color.Transparent,
)

internal object NativeCouiStyle : CouiStyle {
    @Composable
    override fun themeDefaults(dark: Boolean) = CouiThemeDefaults(if (dark) CouiPresets.Dark else CouiPresets.Light)

    @Composable
    override fun ProvideContent(color: Color, textStyle: TextStyle?, mergeTextStyle: Boolean, content: @Composable () -> Unit) {
        val currentContentColor = io.github.suqi8.coui.kmp.theme.LocalContentColor.current
        val resolvedColor = if (color.isSpecified) color else currentContentColor
        val currentTextStyle = LocalCouiTextStyle.current
        val resolvedTextStyle = when {
            textStyle == null -> currentTextStyle
            mergeTextStyle -> currentTextStyle.merge(textStyle)
            else -> textStyle
        }
        CompositionLocalProvider(
            io.github.suqi8.coui.kmp.theme.LocalContentColor provides resolvedColor,
            LocalCouiContentColor provides resolvedColor,
            LocalCouiTextStyle provides resolvedTextStyle,
            content = content,
        )
    }

    @Composable
    override fun minimumInteractiveComponentSize(): Modifier = Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val width = maxOf(placeable.width, 48.dp.roundToPx())
        val height = maxOf(placeable.height, 48.dp.roundToPx())
        layout(width, height) {
            placeable.place(
                ((width - placeable.width) / 2f).roundToInt(),
                ((height - placeable.height) / 2f).roundToInt(),
            )
        }
    }
}

@Composable
internal fun ProvideCouiContent(
    color: Color = Color.Unspecified,
    textStyle: TextStyle? = null,
    mergeTextStyle: Boolean = true,
    content: @Composable () -> Unit,
) {
    Coui.defaultStyle.ProvideContent(color, textStyle, mergeTextStyle, content)
}
