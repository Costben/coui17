// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")
@file:OptIn(ExperimentalCOUIApi::class)

package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.Chip
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.TopTips
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Check
import io.github.suqi8.coui.kmp.icon.basic.Search

/**
 * Renders the "Chip" gallery section content: a multi-select row and a single-select row, with the
 * enabled state, leading icon and dismiss button turned on from the options.
 */
@Composable
fun ChipDemo() {
    val enabled = remember { mutableStateOf(true) }
    val showIcon = remember { mutableStateOf(false) }
    val dismissible = remember { mutableStateOf(false) }

    DemoOptions(
        DemoToggle("Enabled", enabled),
        DemoToggle("Show icon", showIcon),
        DemoToggle("Dismissible", dismissible),
    )
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
    ) {
        ChipGroup(
            labels = listOf("Recommended", "Nearby", "Popular"),
            multiSelect = true,
            enabled = enabled.value,
            showIcon = showIcon.value,
            dismissible = dismissible.value,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ChipGroup(
            labels = listOf("All", "Photos", "Videos", "Documents"),
            multiSelect = false,
            enabled = enabled.value,
            showIcon = showIcon.value,
            dismissible = dismissible.value,
        )
    }
}

@Composable
private fun ChipGroup(
    labels: List<String>,
    multiSelect: Boolean,
    enabled: Boolean,
    showIcon: Boolean,
    dismissible: Boolean,
) {
    var selected by remember { mutableStateOf(setOf(labels.first())) }
    var dismissed by remember { mutableStateOf(emptySet<String>()) }
    // Turning the option off brings the dismissed chips back.
    if (!dismissible && dismissed.isNotEmpty()) dismissed = emptySet()

    // A chip group wraps onto new lines rather than scrolling, so no chip is cut at the card edge.
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.filterNot { it in dismissed }.forEach { label ->
            val checked = label in selected
            Chip(
                selected = checked,
                onClick = {
                    selected = when {
                        !multiSelect -> setOf(label)
                        checked -> selected - label
                        else -> selected + label
                    }
                },
                label = str(label),
                enabled = enabled,
                icon = if (showIcon) {
                    {
                        Icon(
                            imageVector = COUIIcons.Basic.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else {
                    null
                },
                onDismiss = if (dismissible) {
                    { dismissed = dismissed + label }
                } else {
                    null
                },
            )
        }
    }
}
