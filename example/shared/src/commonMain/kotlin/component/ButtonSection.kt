// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")
@file:OptIn(ExperimentalCOUIApi::class)

package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.ButtonSize
import io.github.suqi8.coui.kmp.basic.Stepper
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi

/** Renders the "Button" gallery section content: text buttons, disabled and borderless variants, and a stepper row. */
@Composable
fun ButtonDemo() {
    val initialButtonText = str("Cancel")
    val initialSubmitButtonText = str("Submit")
    var buttonText by remember { mutableStateOf(initialButtonText) }
    var submitButtonText by remember { mutableStateOf(initialSubmitButtonText) }
    var clickCount by remember { mutableIntStateOf(0) }
    var submitClickCount by remember { mutableIntStateOf(0) }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(
            text = buttonText,
            onClick = {
                clickCount++
                buttonText = "Click: $clickCount"
            },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        TextButton(
            text = submitButtonText,
            onClick = {
                submitClickCount++
                submitButtonText = "Click: $submitClickCount"
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(
            text = str("Disabled"),
            onClick = {},
            modifier = Modifier.weight(1f),
            enabled = false,
        )
        Spacer(Modifier.width(12.dp))
        TextButton(
            text = str("Disabled"),
            onClick = {},
            enabled = false,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(
            text = str("Borderless"),
            onClick = {},
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColorsBorderless(),
        )
        Spacer(Modifier.width(12.dp))
        TextButton(
            text = str("Disabled"),
            onClick = {},
            enabled = false,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColorsBorderless(),
        )
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(
            text = str("Tiny"),
            onClick = {},
            size = ButtonSize.Tiny,
        )
        TextButton(
            text = str("Tiny Primary"),
            onClick = {},
            size = ButtonSize.Tiny,
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
    var stepperValue by remember { mutableIntStateOf(2) }
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = str("Stepper"))
        Stepper(
            value = stepperValue,
            onValueChange = { stepperValue = it },
            minValue = 0,
            maxValue = 10,
        )
    }
}
