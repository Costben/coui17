// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import io.github.suqi8.coui.kmp.basic.CodeTextField
import io.github.suqi8.coui.kmp.basic.CodeTextFieldDefaults
import io.github.suqi8.coui.kmp.basic.InputView

/**
 * Renders the "InputView" gallery section content: one card input whose line count, counter, clear
 * button, password toggle and error state come from the options.
 *
 * ColorOS card inputs: COUICardSingleInputView (single line, title inside the card, counter / clear
 * button at the end, error caption below the card) and COUICardMultiInputView (multi line, counter
 * at the bottom-end corner).
 */
@Composable
fun InputViewDemo(focusManager: FocusManager) {
    val multiLine = remember { mutableStateOf(false) }
    val password = remember { mutableStateOf(false) }
    val showCount = remember { mutableStateOf(true) }
    val clearButton = remember { mutableStateOf(true) }
    val errorState = remember { mutableStateOf(false) }
    var value by remember { mutableStateOf("") }

    DemoOptions(
        DemoToggle("Multi-line", multiLine),
        DemoToggle("Password", password),
        DemoToggle("Show counter", showCount),
        DemoToggle("Clear button", clearButton),
        DemoToggle("Error state", errorState),
    )
    InputView(
        value = value,
        onValueChange = { value = it },
        title = if (password.value) str("Password") else str("Nickname"),
        label = when {
            multiLine.value -> str("Signature (multi line)")
            password.value -> str("At least 8 characters")
            else -> str("Enter your nickname")
        },
        showCount = showCount.value,
        maxCount = if (multiLine.value) 100 else 10,
        showClearButton = clearButton.value,
        showPasswordToggle = password.value,
        singleLine = !multiLine.value,
        errorMessage = if (errorState.value) {
            if (password.value) str("Password is too short") else str("Nickname cannot contain spaces")
        } else {
            ""
        },
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (password.value) KeyboardType.Password else KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
    )
}

/** Renders the "CodeTextField" gallery section content: a code field whose masking and length come from the options. */
@Composable
fun CodeTextFieldDemo(focusManager: FocusManager) {
    val security = remember { mutableStateOf(false) }
    val fourCells = remember { mutableStateOf(false) }
    val cellCount = if (fourCells.value) 4 else CodeTextFieldDefaults.CellCount
    var code by remember { mutableStateOf("") }
    // A shorter field cannot hold what the longer one collected.
    if (code.length > cellCount) code = code.take(cellCount)

    DemoOptions(
        DemoToggle("Hide input", security),
        DemoToggle("4 digits", fourCells),
    )
    CodeTextField(
        value = code,
        onValueChange = { code = it.filter(Char::isDigit) },
        cellCount = cellCount,
        security = security.value,
        onComplete = { focusManager.clearFocus() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    )
}
