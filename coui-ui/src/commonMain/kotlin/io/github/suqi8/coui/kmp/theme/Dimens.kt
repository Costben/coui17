// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.ButtonSize
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.ChipDefaults
import io.github.suqi8.coui.kmp.basic.TextFieldDefaults
import io.github.suqi8.coui.kmp.basic.TextFieldMode
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi

/**
 * The metrics a [COUITheme] hands to the components that read them from the theme instead of from
 * their own `Defaults` object.
 *
 * Every field defaults to the matching `Defaults` constant, so `Dimens()` reproduces the stock
 * ColorOS metrics exactly. Override only the fields you need; the nested groups are data classes, so
 * a partial override is a single [copy] call:
 *
 * ```kotlin
 * val dimens = Dimens(
 *     button = Dimens().button.copy(cornerRadius = 16.dp),
 *     card = CardDimens(cornerRadius = 20.dp),
 * )
 * COUITheme(colors = colors, dimens = dimens) { App() }
 * ```
 *
 * A component argument that is passed explicitly still wins over the theme value, so this is a
 * default provider rather than a hard override.
 *
 * @param button The metrics of [io.github.suqi8.coui.kmp.basic.Button], per size tier.
 * @param card The metrics of [io.github.suqi8.coui.kmp.basic.Card].
 * @param chip The metrics of [io.github.suqi8.coui.kmp.basic.Chip].
 * @param textField The metrics of [io.github.suqi8.coui.kmp.basic.TextField], per background mode.
 */
@Immutable
data class Dimens(
    val button: ButtonDimens = ButtonDimens(),
    val card: CardDimens = CardDimens(),
    val chip: ChipDimens = ChipDimens(),
    val textField: TextFieldDimens = TextFieldDimens(),
)

/**
 * The metrics of [io.github.suqi8.coui.kmp.basic.Button], one set per [ButtonSize] tier.
 *
 * @param regular The [ButtonSize.Regular] tier.
 * @param small The [ButtonSize.Small] tier.
 * @param tiny The [ButtonSize.Tiny] tier.
 */
@Immutable
data class ButtonDimens(
    val regular: ButtonTierDimens = ButtonTierDimens(
        minWidth = ButtonDefaults.MinWidth,
        minHeight = ButtonDefaults.MinHeight,
        cornerRadius = ButtonDefaults.CornerRadius,
        insideMargin = ButtonDefaults.InsideMargin,
    ),
    val small: ButtonTierDimens = ButtonTierDimens(
        minWidth = ButtonDefaults.MinWidthSmall,
        minHeight = ButtonDefaults.MinHeightSmall,
        cornerRadius = ButtonDefaults.CornerRadiusSmall,
        insideMargin = ButtonDefaults.InsideMarginSmall,
    ),
    val tiny: ButtonTierDimens = ButtonTierDimens(
        minWidth = ButtonDefaults.MinWidthTiny,
        minHeight = ButtonDefaults.MinHeightTiny,
        cornerRadius = ButtonDefaults.CornerRadiusTiny,
        insideMargin = ButtonDefaults.InsideMarginTiny,
    ),
) {
    /** Returns the metrics of the given [size] tier. */
    fun tier(size: ButtonSize): ButtonTierDimens = when (size) {
        ButtonSize.Regular -> regular
        ButtonSize.Small -> small
        ButtonSize.Tiny -> tiny
    }
}

/**
 * The metrics of one [ButtonSize] tier.
 *
 * @param minWidth The minimum width of the button.
 * @param minHeight The minimum height of the button.
 * @param cornerRadius The corner radius of the button.
 * @param insideMargin The padding inside the button.
 */
@Immutable
data class ButtonTierDimens(
    val minWidth: Dp,
    val minHeight: Dp,
    val cornerRadius: Dp,
    val insideMargin: PaddingValues,
)

/**
 * The metrics of [io.github.suqi8.coui.kmp.basic.Card].
 *
 * @param cornerRadius The corner radius of the card.
 * @param insideMargin The padding inside the card.
 */
@Immutable
data class CardDimens(
    val cornerRadius: Dp = CardDefaults.CornerRadius,
    val insideMargin: PaddingValues = CardDefaults.InsideMargin,
)

/**
 * The metrics of [io.github.suqi8.coui.kmp.basic.Chip].
 *
 * @param cornerRadius The corner radius of the chip.
 * @param minWidth The minimum width of the chip.
 * @param minHeight The minimum height of the chip.
 * @param maxWidth The maximum width of the chip.
 * @param insideMargin The padding inside the chip.
 */
@Immutable
data class ChipDimens(
    val cornerRadius: Dp = ChipDefaults.CornerRadius,
    val minWidth: Dp = ChipDefaults.MinWidth,
    val minHeight: Dp = ChipDefaults.MinHeight,
    val maxWidth: Dp = ChipDefaults.MaxWidth,
    val insideMargin: PaddingValues = ChipDefaults.InsideMargin,
)

/**
 * The metrics of [io.github.suqi8.coui.kmp.basic.TextField], one inside margin per
 * [TextFieldMode].
 *
 * @param cornerRadius The corner radius of the field.
 * @param rectangleInsideMargin The inside margin in [TextFieldMode.Rectangle].
 * @param lineInsideMargin The inside margin in [TextFieldMode.Line].
 * @param noneInsideMargin The inside margin in [TextFieldMode.None].
 */
@Immutable
data class TextFieldDimens(
    val cornerRadius: Dp = TextFieldDefaults.CornerRadius,
    val rectangleInsideMargin: DpSize = TextFieldDefaults.InsideMargin,
    val lineInsideMargin: DpSize = TextFieldDefaults.LineInsideMargin,
    val noneInsideMargin: DpSize = TextFieldDefaults.NoneInsideMargin,
) {
    /** Returns the inside margin of the given [mode]. */
    fun insideMargin(mode: TextFieldMode): DpSize = when (mode) {
        TextFieldMode.Rectangle -> rectangleInsideMargin
        TextFieldMode.Line -> lineInsideMargin
        TextFieldMode.None -> noneInsideMargin
    }
}

/**
 * The [Dimens] of the enclosing [COUITheme], or the stock ColorOS metrics outside one.
 *
 * Kept internal: read it through [COUITheme.dimens], the same way colors and text styles are read.
 */
internal val LocalDimens = staticCompositionLocalOf { Dimens() }
