// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

/** Renders one BasicComponent whose summary, slots and enabled state come from the options. */
@Composable
fun BasicComponentDemo() {
    val enabled = remember { mutableStateOf(true) }
    val showSummary = remember { mutableStateOf(true) }
    val showStart = remember { mutableStateOf(true) }
    val showEnd = remember { mutableStateOf(true) }
    val actionColor = if (enabled.value) {
        COUITheme.colorScheme.onSurfaceVariantActions
    } else {
        COUITheme.colorScheme.disabledOnSecondaryVariant
    }

    DemoOptions(
        DemoToggle("Enabled", enabled),
        DemoToggle("Show summary", showSummary),
        DemoToggle("Start action", showStart),
        DemoToggle("End actions", showEnd),
    )
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        BasicComponent(
            title = str("Title"),
            summary = if (showSummary.value) str("Summary") else null,
            startAction = if (showStart.value) {
                {
                    Text(
                        text = str("Start"),
                        color = if (enabled.value) {
                            COUITheme.colorScheme.onSurface
                        } else {
                            COUITheme.colorScheme.disabledOnSecondaryVariant
                        },
                    )
                }
            } else {
                null
            },
            endActions = {
                if (showEnd.value) {
                    Text(
                        text = str("End1"),
                        fontSize = COUITheme.textStyles.body2.fontSize,
                        color = actionColor,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = str("End2"),
                        fontSize = COUITheme.textStyles.body2.fontSize,
                        color = actionColor,
                    )
                }
            },
            enabled = enabled.value,
        )
    }
}
