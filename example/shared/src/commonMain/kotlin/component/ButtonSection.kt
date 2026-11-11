// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")
@file:OptIn(ExperimentalCOUIApi::class)

package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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

/**
 * Renders the "Button" gallery section content: the three text-button colour sets driven by the
 * enabled and size options, plus a stepper row.
 */
@Composable
fun ButtonDemo() {
    val enabled = remember { mutableStateOf(true) }
    val sizeIndex = remember { mutableIntStateOf(0) }
    var clickCount by remember { mutableIntStateOf(0) }
    val size = ButtonSize.entries[sizeIndex.intValue]

    DemoOptions(
        DemoToggle("Enabled", enabled),
        DemoChoice("Size", listOf("Regular", "Small", "Tiny"), sizeIndex),
    )
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Only the regular tier stretches; the small tiers keep their own width.
        val weight = if (size == ButtonSize.Regular) Modifier.weight(1f) else Modifier
        TextButton(
            text = if (clickCount > 0) "Click: $clickCount" else str("Cancel"),
            onClick = { clickCount++ },
            modifier = weight,
            enabled = enabled.value,
            size = size,
        )
        TextButton(
            text = str("Submit"),
            onClick = { clickCount++ },
            modifier = weight,
            enabled = enabled.value,
            size = size,
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
        TextButton(
            text = str("Borderless"),
            onClick = { clickCount++ },
            modifier = weight,
            enabled = enabled.value,
            size = size,
            colors = ButtonDefaults.textButtonColorsBorderless(),
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
            enabled = enabled.value,
        )
    }
}
