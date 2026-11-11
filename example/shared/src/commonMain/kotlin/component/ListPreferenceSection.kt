// Copyright 2026, compose-coui-ui contributors
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
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.preference.ListPreference
import io.github.suqi8.coui.kmp.preference.ListPreferenceEntry
import io.github.suqi8.coui.kmp.preference.MultiSelectListPreference

/** Renders the "ListPreference" gallery section content: single and multi select list preferences, enabled from the option. */
@Composable
fun ListPreferenceDemoContent() {
    val enabled = remember { mutableStateOf(true) }

    DemoOptions(DemoToggle("Enabled", enabled))
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ListPreferenceDemo(enabled.value)
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        MultiSelectListPreferenceDemo(enabled.value)
    }
}

@Composable
private fun ListPreferenceDemo(enabled: Boolean) {
    val lightLabel = str("Light")
    val darkLabel = str("Dark")
    val followSystemLabel = str("Follow system")
    val followSystemSummary = str("Switch with the system dark mode")
    val entries = remember(lightLabel, darkLabel, followSystemLabel, followSystemSummary) {
        listOf(
            ListPreferenceEntry(lightLabel),
            ListPreferenceEntry(darkLabel),
            ListPreferenceEntry(followSystemLabel, summary = followSystemSummary),
        )
    }
    var selectedIndex by remember { mutableIntStateOf(2) }

    ListPreference(
        entries = entries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { selectedIndex = it },
        title = str("Theme"),
        cancelButtonText = str("Cancel"),
        summary = str("Single choice bottom panel"),
        enabled = enabled,
    )
}

@Composable
private fun MultiSelectListPreferenceDemo(enabled: Boolean) {
    val photosLabel = str("Photos")
    val documentsLabel = str("Documents")
    val contactsLabel = str("Contacts")
    val contactsSummary = str("Contacts and call logs")
    val systemSettingsLabel = str("System settings")
    val entries = remember(
        photosLabel,
        documentsLabel,
        contactsLabel,
        contactsSummary,
        systemSettingsLabel,
    ) {
        listOf(
            ListPreferenceEntry(photosLabel),
            ListPreferenceEntry(documentsLabel),
            ListPreferenceEntry(contactsLabel, summary = contactsSummary),
            ListPreferenceEntry(systemSettingsLabel, enabled = false),
        )
    }
    var selectedIndices by remember { mutableStateOf(setOf(0, 1)) }

    MultiSelectListPreference(
        entries = entries,
        selectedIndices = selectedIndices,
        onSelectedIndicesChange = { selectedIndices = it },
        title = str("Sync items"),
        confirmButtonText = str("Confirm"),
        cancelButtonText = str("Cancel"),
        summary = str("Multi choice bottom panel"),
        enabled = enabled,
    )
}
