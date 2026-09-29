// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.ChipTabData
import io.github.suqi8.coui.kmp.basic.ChipTabGroup
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.All
import io.github.suqi8.coui.kmp.icon.extended.File
import io.github.suqi8.coui.kmp.icon.extended.Music
import io.github.suqi8.coui.kmp.icon.extended.Photos

fun LazyListScope.chipTabSection() {
    item(key = "chipTab") {
        SmallTitle(text = "ChipTab")
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            SingleSelectChipTabsDemo()
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            IconChipTabsDemo()
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            RedDotAndCountChipTabsDemo()
        }
    }
}

@Composable
private fun SingleSelectChipTabsDemo() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val tabs = remember {
        listOf(
            ChipTabData(label = "All"),
            ChipTabData(label = "Photos"),
            ChipTabData(label = "Videos"),
            ChipTabData(label = "Documents"),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        ChipTabGroup(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onSelectedChange = { selectedIndex = it },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun IconChipTabsDemo() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val tabs = remember {
        listOf(
            ChipTabData(label = "All", icon = COUIIcons.All),
            ChipTabData(label = "Photos", icon = COUIIcons.Photos),
            ChipTabData(label = "Music", icon = COUIIcons.Music),
            ChipTabData(label = "Files", icon = COUIIcons.File),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        ChipTabGroup(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onSelectedChange = { selectedIndex = it },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RedDotAndCountChipTabsDemo() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val tabs = remember {
        listOf(
            ChipTabData(label = "Inbox", showRedDot = true),
            ChipTabData(label = "Updates", count = 12),
            ChipTabData(label = "Archived", count = 128, showRedDot = true),
            ChipTabData(label = "Unavailable", enabled = false),
            ChipTabData(label = "Spam"),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        ChipTabGroup(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onSelectedChange = { selectedIndex = it },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
