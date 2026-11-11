// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.TextField
import io.github.suqi8.coui.kmp.basic.TextFieldMode

/**
 * Renders the "TextField" gallery section content: one field assembled from the options, plus the
 * state-based overload.
 *
 * The option combinations cover the ColorOS forms: Line with a plain placeholder and a clear button
 * is the Settings dialog form (?couiEditTextLineHintDisableStyle + quickDelete); "Focus line only"
 * (COUIInputPreference, couiJustShowFocusLine=true) and the None mode are the card-preference forms
 * and are drawn on a card; "Floating label" is couiHintEnabled.
 */
@Composable
fun TextFieldDemo(focusManager: FocusManager) {
    val modeIndex = remember { mutableIntStateOf(TextFieldMode.Line.ordinal) }
    val enabled = remember { mutableStateOf(true) }
    val floatingLabel = remember { mutableStateOf(false) }
    val focusLineOnly = remember { mutableStateOf(false) }
    val clearButton = remember { mutableStateOf(true) }
    val passwordToggle = remember { mutableStateOf(false) }
    val counter = remember { mutableStateOf(false) }
    val digitsOnly = remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val stateText = rememberTextFieldState(initialText = "")
    val mode = TextFieldMode.entries[modeIndex.intValue]
    val onCard = mode == TextFieldMode.None || focusLineOnly.value

    DemoOptions(
        DemoChoice("Background", TextFieldMode.entries.map { it.name }, modeIndex),
        DemoToggle("Enabled", enabled),
        DemoToggle("Floating label", floatingLabel),
        DemoToggle("Focus line only", focusLineOnly),
        DemoToggle("Clear button", clearButton),
        DemoToggle("Password toggle", passwordToggle),
        DemoToggle("Max 10 characters", counter),
        DemoToggle("Digits only (error state)", digitsOnly),
    )
    val field: @Composable (Modifier) -> Unit = { modifier ->
        TextField(
            value = text,
            onValueChange = { text = it },
            label = str("Device name"),
            backgroundMode = mode,
            useLabelAsPlaceholder = !floatingLabel.value,
            justShowFocusLine = focusLineOnly.value,
            enabled = enabled.value,
            isError = digitsOnly.value && text.isNotEmpty() && !text.all { it.isDigit() },
            maxCount = if (counter.value) 10 else null,
            showClearButton = clearButton.value,
            showPasswordToggle = passwordToggle.value,
            singleLine = true,
            modifier = modifier,
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (passwordToggle.value) KeyboardType.Password else KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
        )
    }
    if (onCard) {
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                field(Modifier.padding(vertical = 6.dp))
            }
        }
    } else {
        field(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
        )
    }
    TextField(
        state = stateText,
        label = str("State-based"),
        enabled = enabled.value,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        onKeyboardAction = { focusManager.clearFocus() },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    )
}
