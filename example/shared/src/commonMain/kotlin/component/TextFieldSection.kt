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
 * Renders the "TextField (Line, Settings dialog form)" gallery section content: line-mode fields with
 * clear, state, password, count and error variants.
 *
 * ColorOS Settings dialog form: Line mode with a plain placeholder and a clear button
 * (?couiEditTextLineHintDisableStyle + quickDelete).
 */
@Composable
fun TextFieldLineSettingsDialogFormDemo(focusManager: FocusManager) {
    var dialogText by remember { mutableStateOf("") }
    val stateText = rememberTextFieldState(initialText = "")
    var passwordText by remember { mutableStateOf("") }
    var countText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    TextField(
        value = dialogText,
        onValueChange = { dialogText = it },
        label = str("Device name"),
        showClearButton = true,
        singleLine = true,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    )
    TextField(
        state = stateText,
        label = str("State-based"),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        onKeyboardAction = { focusManager.clearFocus() },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    )
    TextField(
        value = passwordText,
        onValueChange = { passwordText = it },
        label = str("Password"),
        showPasswordToggle = true,
        singleLine = true,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
    )
    TextField(
        value = countText,
        onValueChange = { countText = it },
        label = str("Max 10 characters"),
        maxCount = 10,
        singleLine = true,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    )
    TextField(
        value = errorText,
        onValueChange = { errorText = it },
        label = str("Digits only (error state)"),
        isError = errorText.isNotEmpty() && !errorText.all { it.isDigit() },
        singleLine = true,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    )
}

/**
 * Renders the "TextField (in card, Settings preference form)" gallery section content: a focus-line
 * field and an undecorated field on a card.
 *
 * ColorOS Settings card-preference form: bare text on a white card (COUIInputPreference,
 * couiJustShowFocusLine=true — the focused blue line expands under the text; None mode never shows
 * any line).
 */
@Composable
fun TextFieldInCardSettingsPreferenceFormDemo(focusManager: FocusManager) {
    var cardText by remember { mutableStateOf("") }
    var focusLineText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            TextField(
                value = focusLineText,
                onValueChange = { focusLineText = it },
                label = str("Focus line only (COUIInputPreference)"),
                justShowFocusLine = true,
                singleLine = true,
                modifier = Modifier.padding(vertical = 6.dp),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            TextField(
                value = cardText,
                onValueChange = { cardText = it },
                label = str("No decoration (card input)"),
                backgroundMode = TextFieldMode.None,
                singleLine = true,
                modifier = Modifier.padding(vertical = 6.dp),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        }
    }
}

/** Renders the "TextField (Rectangle & floating label)" gallery section content: rectangle stroke fields with and without a floating label. */
@Composable
fun TextFieldRectangleFloatingLabelDemo(focusManager: FocusManager) {
    var rectText by remember { mutableStateOf("") }
    var floatingText by remember { mutableStateOf("") }

    TextField(
        value = rectText,
        onValueChange = { rectText = it },
        label = str("Rectangle (stroke only)"),
        backgroundMode = TextFieldMode.Rectangle,
        singleLine = true,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    )
    TextField(
        value = floatingText,
        onValueChange = { floatingText = it },
        label = str("Floating label (couiHintEnabled)"),
        useLabelAsPlaceholder = false,
        backgroundMode = TextFieldMode.Rectangle,
        singleLine = true,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    )
}
