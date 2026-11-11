// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

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
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.cardListPositionOf
import io.github.suqi8.coui.kmp.preference.RadioButtonPreference

/** Renders the "RadioButton" gallery section content: one radio group driven by the options. */
@Composable
fun RadioButtonDemo() {
    val enabled = remember { mutableStateOf(true) }
    val showSummary = remember { mutableStateOf(true) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val options = listOf("Option A", "Option B", "Option C")

    DemoOptions(
        DemoToggle("Enabled", enabled),
        DemoToggle("Show summary", showSummary),
    )
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        options.forEachIndexed { index, option ->
            RadioButtonPreference(
                title = str(option),
                summary = if (showSummary.value) str("Selected: {}", selectedIndex == index) else null,
                selected = selectedIndex == index,
                enabled = enabled.value,
                onClick = { selectedIndex = index },
                cardListPosition = cardListPositionOf(index, options.size),
            )
        }
    }
}
