// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.Checkbox
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.preference.CheckboxLocation
import io.github.suqi8.coui.kmp.preference.CheckboxPreference

/** Renders the "Checkbox" gallery section content: a bare tri-state checkbox and a checkbox preference driven by the options. */
@Composable
fun CheckboxDemo() {
    val enabled = remember { mutableStateOf(true) }
    val atEnd = remember { mutableStateOf(false) }
    val showSummary = remember { mutableStateOf(true) }
    var triState by remember { mutableStateOf(ToggleableState.Indeterminate) }
    var checked by remember { mutableStateOf(false) }

    DemoOptions(
        DemoToggle("Enabled", enabled),
        DemoToggle("Checkbox at the end", atEnd),
        DemoToggle("Show summary", showSummary),
    )
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = str("Tri-state Checkbox"))
            Checkbox(
                state = triState,
                onClick = {
                    triState = when (triState) {
                        ToggleableState.Off -> ToggleableState.Indeterminate
                        ToggleableState.Indeterminate -> ToggleableState.On
                        ToggleableState.On -> ToggleableState.Off
                    }
                },
                enabled = enabled.value,
            )
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        CheckboxPreference(
            checkboxLocation = if (atEnd.value) CheckboxLocation.End else CheckboxLocation.Start,
            title = str("CheckboxPreference"),
            summary = if (showSummary.value) str("State: {}", checked) else null,
            checked = checked,
            enabled = enabled.value,
            onCheckedChange = { checked = it },
        )
    }
}
