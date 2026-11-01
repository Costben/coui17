// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.TextField
import io.github.suqi8.coui.kmp.basic.TextFieldMode

/**
 * A card row whose input field is edited in place, mirroring COUIInputPreference.
 *
 * On the device the row is `res/layout/coui_input_preference.xml`: an empty `edittext_container`
 * that `COUIInputPreference.onBindViewHolder` fills with its own inner
 * `COUICardListItemInputView`, a `COUIInputView` whose `mTitle` is the preference's title and whose
 * `COUIScrolledEditText` runs in line mode with `couiJustShowFocusLine`. There is no dialog: the
 * field is part of the row, which is what separates this class from [EditTextPreference].
 *
 * The row text is the field's own floating label ([TextField] with
 * `useLabelAsPlaceholder = false`), matching `COUIScrolledEditText.setTextDirection(5)`'s
 * locale-directed entry inside the `COUIInputPreferenceTheme`.
 *
 * @param title The title of the [InputPreference], i.e. the field's floating label.
 * @param value The current text value.
 * @param onValueChange The callback invoked on every edit.
 * @param modifier The modifier to be applied to the [InputPreference].
 * @param startAction The [Composable] content on the start side of the [InputPreference].
 * @param endActions The [Composable] content on the end side of the [InputPreference].
 * @param bottomAction The [Composable] content at the bottom of the [InputPreference].
 * @param insideMargin The margin inside the [InputPreference].
 * @param cardListPosition The row's position inside its card group. Rounded outer edges receive extra padding.
 * @param enabled Whether the [InputPreference]'s field is editable (COUI `setEnabled(isEnabled())`).
 * @param readOnly Whether the field is read-only.
 * @param maxCount The maximum number of characters, when limited.
 * @param showClearButton Whether to show the clear button while the field is focused and not empty.
 */
@Composable
@NonRestartableComposable
fun InputPreference(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = InputPreferenceDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    maxCount: Int? = null,
    showClearButton: Boolean = false,
) {
    // COUIInputPreference.getTitlePaddingTop() switches on `mTitle`: an empty title gets
    // coui_input_edit_text_no_title_padding_*_inPreference, otherwise the field's own label
    // supplies the top inset and only the bottom one is left to the row.
    val resolvedInsideMargin = remember(title, insideMargin) {
        if (title.isEmpty()) InputPreferenceDefaults.NoTitleInsideMargin else insideMargin
    }
    BasicComponent(
        modifier = modifier,
        insideMargin = resolvedInsideMargin,
        cardListPosition = cardListPosition,
        startAction = startAction,
        endActions = endActions,
        bottomAction = bottomAction,
        enabled = enabled,
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            backgroundMode = TextFieldMode.Line,
            justShowFocusLine = true,
            label = title,
            useLabelAsPlaceholder = false,
            enabled = enabled,
            readOnly = readOnly,
            maxCount = maxCount,
            showClearButton = showClearButton,
        )
    }
}

object InputPreferenceDefaults {

    /**
     * The row inset when the field carries its own label.
     *
     * `COUIInputPreference` reads `coui_input_edit_text_has_title_padding_top` (0dp) and
     * `coui_input_edit_text_has_title_preference_padding_bottom` (7dp) for a titled field.
     */
    val InsideMargin = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 7.dp)

    /**
     * The row inset for a field without a label
     * (`coui_input_edit_text_no_title_padding_top_inPreference` /
     * `..._bottom_inPreference`, 13dp each).
     */
    val NoTitleInsideMargin = PaddingValues(horizontal = 16.dp, vertical = 13.dp)
}
