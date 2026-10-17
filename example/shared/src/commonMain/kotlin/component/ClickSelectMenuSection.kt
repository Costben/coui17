// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.ChipTabData
import io.github.suqi8.coui.kmp.basic.ChipTabGroup
import io.github.suqi8.coui.kmp.basic.ClickSelectMenuAlignment
import io.github.suqi8.coui.kmp.basic.ClickSelectMenuDefaults
import io.github.suqi8.coui.kmp.basic.ClickSelectMenuPlacement
import io.github.suqi8.coui.kmp.basic.ClickSelectMenuSide
import io.github.suqi8.coui.kmp.basic.DropdownImpl
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.clickSelectMenuPositionProvider
import io.github.suqi8.coui.kmp.basic.preciseClickAnchor
import io.github.suqi8.coui.kmp.basic.rememberPreciseClickState
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalDismissState
import io.github.suqi8.coui.kmp.window.WindowListPopup

fun LazyListScope.clickSelectMenuSection() {
    item(key = "clickSelectMenu") {
        SmallTitle(text = "ClickSelectMenu")
        ClickSelectMenuDemoContent()
    }
}

/** Renders the "ClickSelectMenu" gallery section content: a card hosting the click-select menu demo. */
@Composable
fun ClickSelectMenuDemoContent() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ClickSelectMenuDemo()
    }
}

@Composable
private fun ClickSelectMenuDemo() {
    val sides = ClickSelectMenuSide.entries
    val alignments = ClickSelectMenuAlignment.entries
    var sideIndex by remember { mutableIntStateOf(0) }
    var alignmentIndex by remember { mutableIntStateOf(0) }
    var showPopup by remember { mutableStateOf(false) }
    val placement = ClickSelectMenuPlacement(sides[sideIndex], alignments[alignmentIndex])
    val preciseClickState = rememberPreciseClickState()
    val rows = listOf(str("Cut"), str("Copy"), str("Paste"))
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChipTabGroup(
            tabs = remember { sides.map { ChipTabData(label = it.name) } },
            selectedIndex = sideIndex,
            onSelectedChange = { sideIndex = it },
            modifier = Modifier.fillMaxWidth(),
        )
        ChipTabGroup(
            tabs = remember { alignments.map { ChipTabData(label = it.name) } },
            selectedIndex = alignmentIndex,
            onSelectedChange = { alignmentIndex = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = str("Tap anywhere on this row"),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPopup = true }
                    .preciseClickAnchor(preciseClickState)
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
                style = COUITheme.textStyles.body1,
            )
            WindowListPopup(
                show = showPopup,
                // MainMenuPlacementSpec: the side and alignment force the menu's edge, which the
                // stock dropdown provider never does.
                popupPositionProvider = clickSelectMenuPositionProvider(placement),
                preciseClickState = preciseClickState,
                onDismissRequest = { showPopup = false },
            ) {
                val dismiss = LocalDismissState.current
                ListPopupColumn {
                    rows.forEachIndexed { index, text ->
                        DropdownImpl(
                            text = text,
                            optionSize = rows.size,
                            isSelected = false,
                            index = index,
                            onSelectedIndexChange = { dismiss?.invoke() },
                        )
                    }
                }
            }
        }
        Text(
            text = "placement: side=${placement.side} (${placement.side.value}), " +
                "alignment=${placement.alignment} (${placement.alignment.value})",
            style = COUITheme.textStyles.footnote1,
        )
        Text(
            text = "resolvedSide=${ClickSelectMenuDefaults.resolvePopupSide(placement, isRtl)}, " +
                "centerAlignY=${ClickSelectMenuDefaults.shouldApplyCenterAlignY(placement)}",
            style = COUITheme.textStyles.footnote1,
        )
        Text(
            text = "point=${preciseClickState.touchPoint}",
            style = COUITheme.textStyles.footnote1,
        )
    }
}
