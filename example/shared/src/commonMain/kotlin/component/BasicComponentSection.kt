// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

/** Renders the enabled and disabled BasicComponent samples inside one card. */
@Composable
fun BasicComponentDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        BasicComponent(
            title = str("Title"),
            summary = str("Summary"),
            startAction = {
                Text(
                    text = str("Start"),
                )
            },
            endActions = {
                Text(
                    text = str("End1"),
                    fontSize = COUITheme.textStyles.body2.fontSize,
                    color = COUITheme.colorScheme.onSurfaceVariantActions,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = str("End2"),
                    fontSize = COUITheme.textStyles.body2.fontSize,
                    color = COUITheme.colorScheme.onSurfaceVariantActions,
                )
            },
            enabled = true,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        BasicComponent(
            title = str("Title"),
            summary = str("Summary"),
            startAction = {
                Text(
                    text = str("Start"),
                    color = COUITheme.colorScheme.disabledOnSecondaryVariant,
                )
            },
            endActions = {
                Text(
                    text = str("End1"),
                    fontSize = COUITheme.textStyles.body2.fontSize,
                    color = COUITheme.colorScheme.disabledOnSecondaryVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = str("End2"),
                    fontSize = COUITheme.textStyles.body2.fontSize,
                    color = COUITheme.colorScheme.disabledOnSecondaryVariant,
                )
            },
            enabled = false,
        )
    }
}
