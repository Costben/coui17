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
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Switch
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.preference.SwitchPreference
import io.github.suqi8.coui.kmp.theme.COUITheme

/** Renders the "Switch" gallery section content: a bare switch and a switch preference driven by the options. */
@Composable
fun SwitchDemo() {
    val enabled = remember { mutableStateOf(true) }
    val loading = remember { mutableStateOf(false) }
    val showSummary = remember { mutableStateOf(true) }
    val showState = remember { mutableStateOf(false) }
    var switch by remember { mutableStateOf(true) }
    var preference by remember { mutableStateOf(false) }

    DemoOptions(
        DemoToggle("Enabled", enabled),
        DemoToggle("Loading", loading),
        DemoToggle("Show summary", showSummary),
        DemoToggle("Show state text", showState),
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
            Text(text = str("Switch"))
            Switch(
                checked = switch,
                onCheckedChange = { switch = it },
                enabled = enabled.value,
                isLoading = loading.value,
            )
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        SwitchPreference(
            title = str("SwitchPreference"),
            summary = if (showSummary.value) str("State: {}", preference) else null,
            checked = preference,
            enabled = enabled.value,
            endActions = {
                if (showState.value) {
                    Text(
                        text = "$preference",
                        color = COUITheme.colorScheme.onSurfaceVariantActions,
                    )
                }
            },
            onCheckedChange = { preference = it },
        )
    }
}
