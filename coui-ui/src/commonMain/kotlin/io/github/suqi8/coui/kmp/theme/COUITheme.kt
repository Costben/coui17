// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import io.github.suqi8.coui.kmp.basic.LocalPopupBarriers
import io.github.suqi8.coui.kmp.basic.ObserveSystemMaterialState
import io.github.suqi8.coui.kmp.basic.PopupBarriers
import io.github.suqi8.coui.kmp.utils.COUIIndication
import io.github.suqi8.coui.kmp.utils.COUIOverscrollFactory

/**
 * The COUI theme that provides colors, text styles and metrics for the COUI components.
 * This theme supports dynamic color schemes through the [ThemeController].
 *
 * @param controller The [ThemeController] that controls the current color scheme.
 * @param textStyles The text styles for the COUI components.
 * @param dimens The metrics for the COUI components. Defaults to the enclosing theme's, so a nested
 *   theme inherits the metrics it does not override.
 * @param content The content of the COUI theme.
 */
@Composable
fun COUITheme(
    controller: ThemeController,
    textStyles: TextStyles = COUITheme.textStyles,
    dimens: Dimens = COUITheme.dimens,
    content: @Composable () -> Unit,
) {
    val rawColors = controller.currentColors()
    val couiColors = remember { rawColors.copy() }.apply { updateColorsFrom(rawColors) }
    val couiTextStyles = remember { textStyles.copy() }.apply { updateTextStylesFrom(textStyles) }
    val couiIndication = remember(couiColors.onBackground) { COUIIndication(color = couiColors.onBackground) }
    CompositionLocalProvider(
        LocalColors provides couiColors,
        LocalTextStyles provides couiTextStyles,
        LocalDimens provides dimens,
        LocalIndication provides couiIndication,
        LocalContentColor provides couiColors.onBackground,
        LocalColorSchemeMode provides controller.colorSchemeMode,
        LocalOverscrollFactory provides COUIOverscrollFactory,
        LocalPopupBarriers provides rememberPopupBarriers(),
    ) {
        ObserveSystemMaterialState()
        content()
    }
}

/**
 * The COUI theme that provides colors, text styles and metrics for the COUI components.
 * This theme uses the provided [colors] and [textStyles].
 *
 * @param colors The color scheme for the COUI components.
 * @param textStyles The text styles for the COUI components.
 * @param dimens The metrics for the COUI components. Defaults to the enclosing theme's, so a nested
 *   theme inherits the metrics it does not override.
 * @param content The content of the COUI theme.
 */
@Composable
fun COUITheme(
    colors: Colors = COUITheme.colorScheme,
    textStyles: TextStyles = COUITheme.textStyles,
    dimens: Dimens = COUITheme.dimens,
    content: @Composable () -> Unit,
) {
    val couiColors = remember { colors.copy() }.apply { updateColorsFrom(colors) }
    val couiTextStyles = remember { textStyles.copy() }.apply { updateTextStylesFrom(textStyles) }
    val couiIndication = remember(couiColors.onBackground) { COUIIndication(color = couiColors.onBackground) }
    CompositionLocalProvider(
        LocalColors provides couiColors,
        LocalTextStyles provides couiTextStyles,
        LocalDimens provides dimens,
        LocalIndication provides couiIndication,
        LocalOverscrollFactory provides COUIOverscrollFactory,
        LocalPopupBarriers provides rememberPopupBarriers(),
    ) {
        ObserveSystemMaterialState()
        content()
    }
}

/**
 * The pre-1.3.0 shape of [COUITheme], kept so bytecode compiled against the previous release still
 * links. Source code cannot see it: [Dimens] defaults to the stock metrics, which is what this
 * overload used to do implicitly.
 */
@Deprecated(
    message = "Migrated to the overload that also takes Dimens.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun COUITheme(
    controller: ThemeController,
    textStyles: TextStyles = COUITheme.textStyles,
    content: @Composable () -> Unit,
) {
    COUITheme(controller = controller, textStyles = textStyles, dimens = Dimens(), content = content)
}

/**
 * The pre-1.3.0 shape of [COUITheme], kept so bytecode compiled against the previous release still
 * links. Source code cannot see it: [Dimens] defaults to the stock metrics, which is what this
 * overload used to do implicitly.
 */
@Deprecated(
    message = "Migrated to the overload that also takes Dimens.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun COUITheme(
    colors: Colors = COUITheme.colorScheme,
    textStyles: TextStyles = COUITheme.textStyles,
    content: @Composable () -> Unit,
) {
    COUITheme(colors = colors, textStyles = textStyles, dimens = Dimens(), content = content)
}

/**
 * Overrides the [Dimens] of the enclosing [COUITheme] for [content], without touching its colors or
 * text styles. Useful for a screen that needs its own metrics inside an app-wide theme.
 *
 * @param dimens The metrics to provide.
 * @param content The content that reads them.
 */
@Composable
fun ProvideCOUIDimens(
    dimens: Dimens,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDimens provides dimens, content = content)
}

/** The barrier registry of an enclosing COUI theme, so nested themes share one; a new one otherwise. */
@Composable
private fun rememberPopupBarriers(): PopupBarriers {
    val outer = LocalPopupBarriers.current
    return outer ?: remember { PopupBarriers() }
}

object COUITheme {
    val colorScheme: Colors
        @Composable @ReadOnlyComposable
        get() = LocalColors.current

    val textStyles: TextStyles
        @Composable @ReadOnlyComposable
        get() = LocalTextStyles.current

    /**
     * The metrics of the enclosing theme. Components read this when their own metric argument is not
     * passed explicitly; outside a [COUITheme] it is the stock ColorOS metric set.
     */
    val dimens: Dimens
        @Composable @ReadOnlyComposable
        get() = LocalDimens.current

    val colorSchemeMode: ColorSchemeMode?
        @Composable @ReadOnlyComposable
        get() = LocalColorSchemeMode.current

    val isDynamicColor: Boolean
        @Composable @ReadOnlyComposable
        get() = when (colorSchemeMode) {
            ColorSchemeMode.MonetSystem,
            ColorSchemeMode.MonetLight,
            ColorSchemeMode.MonetDark,
            -> true

            else -> false
        }
}
