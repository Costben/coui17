// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.IconButtonDefaults
import io.github.suqi8.coui.kmp.basic.RichTooltipBox
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TooltipAnchorPosition
import io.github.suqi8.coui.kmp.basic.TooltipBox
import io.github.suqi8.coui.kmp.basic.rememberTooltipState
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Edit
import io.github.suqi8.coui.kmp.icon.extended.Info
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch

/**
 * Renders the "Tooltip" gallery section content: preferred placement options above a card pairing
 * a plain tooltip with a persistent rich tooltip. Each trigger icon sits horizontally centred so
 * Above, Below, Start, and End placements all have room on the requested side of the anchor.
 */
@Composable
fun TooltipDemo() {
    val positionIndex = remember { mutableIntStateOf(0) }
    val positions = remember {
        listOf(
            TooltipAnchorPosition.Below,
            TooltipAnchorPosition.Above,
            TooltipAnchorPosition.Start,
            TooltipAnchorPosition.End,
        )
    }
    val positioning = positions[positionIndex.intValue]
    val plainState = rememberTooltipState(isPersistent = false)
    val richState = rememberTooltipState(isPersistent = true)
    val coroutineScope = rememberCoroutineScope()
    var acknowledged by remember { mutableIntStateOf(0) }

    val togglePlain: () -> Unit = {
        coroutineScope.launch {
            if (plainState.isVisible) {
                plainState.dismiss()
            } else {
                plainState.show()
            }
        }
    }
    val toggleRich: () -> Unit = {
        coroutineScope.launch {
            if (richState.isVisible) {
                richState.dismiss()
            } else {
                richState.show()
            }
        }
    }

    DemoOptions(
        DemoChoice("Position", listOf("Below", "Above", "Start", "End"), positionIndex),
    )
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TooltipAnchorRow(
                title = str("Plain tooltip"),
                summary = str("Tap or long press to show a brief label"),
                onClick = togglePlain,
            ) {
                TooltipBox(
                    text = str("Edit"),
                    state = plainState,
                    positioning = positioning,
                ) {
                    IconButton(
                        onClick = togglePlain,
                        minWidth = IconButtonDefaults.ToolbarButtonHeight,
                        minHeight = IconButtonDefaults.ToolbarButtonHeight,
                    ) {
                        Icon(imageVector = COUIIcons.Edit, contentDescription = str("Edit"))
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            TooltipAnchorRow(
                title = str("Rich tooltip"),
                summary = if (acknowledged > 0) {
                    str("Last clicked: {}", str("Got it"))
                } else {
                    str("Tap or long press for title, body and action")
                },
                onClick = toggleRich,
            ) {
                RichTooltipBox(
                    title = str("Rich tooltip"),
                    text = str("Rich tooltips show a title, supporting text, and an optional action. ").trimEnd(),
                    actionText = str("Got it"),
                    onActionClick = { acknowledged++ },
                    state = richState,
                    positioning = positioning,
                ) {
                    IconButton(
                        onClick = toggleRich,
                        minWidth = IconButtonDefaults.ToolbarButtonHeight,
                        minHeight = IconButtonDefaults.ToolbarButtonHeight,
                    ) {
                        Icon(imageVector = COUIIcons.Info, contentDescription = str("Rich tooltip"))
                    }
                }
            }
        }
    }
}

@Composable
private fun TooltipAnchorRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
    anchor: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = COUITheme.textStyles.headline2,
                color = COUITheme.colorScheme.onSurface,
            )
            Text(
                text = summary,
                style = COUITheme.textStyles.body2,
                color = COUITheme.colorScheme.onSurfaceSecondary,
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            anchor()
        }
    }
}
