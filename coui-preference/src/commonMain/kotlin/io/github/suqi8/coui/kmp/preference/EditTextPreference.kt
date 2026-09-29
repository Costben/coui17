// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.BasicComponentColors
import io.github.suqi8.coui.kmp.basic.BasicComponentDefaults
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.TextField
import io.github.suqi8.coui.kmp.overlay.OverlayBottomSheet
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * A preference row that shows its value as trailing text and edits it in a bottom sheet,
 * mirroring COUIEditTextPreference.
 *
 * On the device the row itself is the standard `coui_preference` layout with an
 * `app:couiAssignment` value; the editor is `COUIEditTextPreferenceDialogFragment`, an
 * `AlertDialog` built by `COUIAlertDialogBuilder` around `res/layout/coui_preference_dialog_edittext.xml`
 * (a single-line `COUIEditText` with `quickDelete`). This repo renders every dialog-bearing
 * preference — [ListPreference] and [MultiSelectListPreference] included — as an
 * [OverlayBottomSheet], so the editor is a sheet holding the same single-line [TextField].
 *
 * @param title The title of the [EditTextPreference].
 * @param value The current text value.
 * @param onValueChange The callback invoked with the edited text when the sheet is confirmed.
 * @param confirmButtonText The label of the sheet's confirm button (COUI `android:positiveButtonText`).
 * @param cancelButtonText The label of the sheet's cancel button (COUI `android:negativeButtonText`).
 * @param modifier The modifier to be applied to the [EditTextPreference].
 * @param titleColor The color of the title.
 * @param summary The summary of the [EditTextPreference].
 * @param summaryColor The color of the summary.
 * @param dialogTitle The title of the editor sheet. Defaults to [title].
 * @param hint The placeholder shown while the field is empty.
 * @param supportEmptyInput Whether an empty value may be confirmed (COUI `couiSupportEmptyInput`).
 * @param startAction The [Composable] content on the start side of the [EditTextPreference].
 * @param bottomAction The [Composable] content at the bottom of the [EditTextPreference].
 * @param insideMargin The margin inside the [EditTextPreference].
 * @param cardListPosition The row's position inside its card group. Rounded outer edges receive extra padding.
 * @param enabled Whether the [EditTextPreference] is clickable.
 * @param showValue Whether to show the current value as trailing text.
 * @param renderInRootScaffold Whether to render the sheet in the root (outermost) Scaffold.
 * @param onExpandedChange The callback when the sheet is shown or dismissed.
 */
@Composable
fun EditTextPreference(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    confirmButtonText: String,
    cancelButtonText: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    dialogTitle: String = title,
    hint: String = "",
    supportEmptyInput: Boolean = false,
    startAction: @Composable (() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    enabled: Boolean = true,
    showValue: Boolean = true,
    renderInRootScaffold: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    val isSheetShown = remember { mutableStateOf(false) }
    val isHoldDown = remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    val currentHapticFeedback by rememberUpdatedState(hapticFeedback)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentValue by rememberUpdatedState(value)
    val currentOnExpandedChange = rememberUpdatedState(onExpandedChange)

    var draft by remember { mutableStateOf(value) }
    LaunchedEffect(isSheetShown.value) {
        if (isSheetShown.value) draft = currentValue
    }

    val setExpanded: (Boolean) -> Unit = remember {
        { expanded ->
            if (isSheetShown.value != expanded) {
                isSheetShown.value = expanded
                currentOnExpandedChange.value?.invoke(expanded)
            }
        }
    }

    BasicComponent(
        modifier = modifier,
        insideMargin = insideMargin,
        cardListPosition = cardListPosition,
        title = title,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        startAction = startAction,
        endActions = {
            if (showValue && value.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(start = 8.dp, end = 4.dp)
                        .align(Alignment.CenterVertically)
                        .weight(1f, fill = false),
                ) {
                    Text(
                        text = value,
                        fontSize = COUITheme.textStyles.body2.fontSize,
                        color = COUITheme.colorScheme.onSurfaceSecondary,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        bottomAction = bottomAction,
        onClick = {
            if (enabled) {
                setExpanded(true)
                isHoldDown.value = true
                currentHapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            }
        },
        role = Role.Button,
        holdDownState = isHoldDown.value,
        enabled = enabled,
    )

    OverlayBottomSheet(
        show = isSheetShown.value,
        title = dialogTitle,
        onDismissRequest = { setExpanded(false) },
        onDismissFinished = { isHoldDown.value = false },
        renderInRootScaffold = renderInRootScaffold,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EditTextPreferenceDefaults.SheetHorizontalPadding),
                label = hint,
                showClearButton = true,
                singleLine = true,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = EditTextPreferenceDefaults.ButtonBarTopPadding,
                        bottom = EditTextPreferenceDefaults.ButtonBarBottomPadding,
                    ),
            ) {
                TextButton(
                    text = cancelButtonText,
                    onClick = { setExpanded(false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsBorderless(),
                )
                TextButton(
                    text = confirmButtonText,
                    onClick = {
                        currentOnValueChange(draft)
                        setExpanded(false)
                    },
                    enabled = supportEmptyInput || draft.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsBorderless(),
                )
            }
        }
    }
}

object EditTextPreferenceDefaults {

    /**
     * The horizontal inset of the editor field, from
     * `dimen/support_preference_dialog_edittext_margin_horizontal` (24dp).
     */
    val SheetHorizontalPadding = 24.dp

    /**
     * The gap between the editor field and the button bar, from
     * `dimen/coui_edittextdialog_marginbottom` (14dp).
     */
    val ButtonBarTopPadding = 14.dp

    /** The padding below the sheet button bar, matching [ListPreferenceDefaults.ButtonBarBottomPadding]. */
    val ButtonBarBottomPadding = 12.dp
}
