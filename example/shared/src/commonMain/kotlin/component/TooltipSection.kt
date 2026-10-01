// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.RichTooltipBox
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.TooltipBox
import io.github.suqi8.coui.kmp.basic.rememberTooltipState
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Edit
import io.github.suqi8.coui.kmp.icon.extended.Info

fun LazyListScope.tooltipSection() {
    item(key = "tooltip") {
        SmallTitle(text = "Tooltip")
        TooltipDemo()
    }
}

/** Renders the "Tooltip" gallery section content (a card pairing a plain tooltip with a rich tooltip). */
@Composable
fun TooltipDemo() {
    val richState = rememberTooltipState(isPersistent = true)
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TooltipBox(text = str("Edit")) {
                    IconButton(onClick = {}) {
                        Icon(imageVector = COUIIcons.Edit, contentDescription = str("Edit"))
                    }
                }
                RichTooltipBox(
                    title = str("Rich tooltip"),
                    text = str("Rich tooltips show a title, supporting text, and an optional action. ") +
                        str("Move onto the tooltip to use the action, or tap outside to dismiss."),
                    actionText = str("Got it"),
                    onActionClick = {},
                    state = richState,
                ) {
                    IconButton(onClick = {}) {
                        Icon(imageVector = COUIIcons.Info, contentDescription = str("Rich tooltip"))
                    }
                }
            }
        }
    }
}
