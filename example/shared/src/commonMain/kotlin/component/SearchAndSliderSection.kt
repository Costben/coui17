// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")
@file:OptIn(ExperimentalCOUIApi::class)

package component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.IntentSeekBar
import io.github.suqi8.coui.kmp.basic.IntentSeekBarDefaults
import io.github.suqi8.coui.kmp.basic.IntentSeekBarSize
import io.github.suqi8.coui.kmp.basic.SearchHistoryView
import io.github.suqi8.coui.kmp.basic.SearchHistoryViewDefaults
import io.github.suqi8.coui.kmp.basic.Slider
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.TouchSearchView
import io.github.suqi8.coui.kmp.basic.TouchSearchViewDefaults
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.theme.COUITheme

private val DemoHistory = listOf(
    "wireless earbuds",
    "screen protector",
    "usb-c hub",
    "laptop stand",
    "mechanical keyboard",
    "noise cancelling headphones",
    "portable charger",
    "webcam",
    "monitor arm",
    "desk lamp",
    "trackball mouse",
    "thermal paste",
)

/** Renders the "IntentSeekBar" gallery section content: a card of intent seek bar variants and a plain slider. */
@Composable
fun IntentSeekBarDemoContent() {
    val enabled = remember { mutableStateOf(true) }
    val showThumb = remember { mutableStateOf(true) }
    val fixedColors = remember { mutableStateOf(false) }
    val sizeIndex = remember { mutableIntStateOf(0) }

    DemoOptions(
        DemoToggle("Enabled", enabled),
        DemoToggle("Show thumb", showThumb),
        DemoToggle("Fixed colors on a dark surface", fixedColors),
        DemoChoice("Size", IntentSeekBarSize.entries.map { it.name }, sizeIndex),
    )
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        IntentSeekBarDemo(
            enabled = enabled.value,
            showThumb = showThumb.value,
            fixedColors = fixedColors.value,
            size = IntentSeekBarSize.entries[sizeIndex.intValue],
        )
    }
}

/** Renders the "TouchSearchView" gallery section content: a card with the touch search keypad. */
@Composable
fun TouchSearchViewDemoContent() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        TouchSearchViewDemo()
    }
}

/** Renders the "SearchHistoryView" gallery section content: a card with the search history list and its folded variant. */
@Composable
fun SearchHistoryViewDemoContent() {
    val folded = remember { mutableStateOf(false) }
    val longPressDelete = remember { mutableStateOf(true) }

    DemoOptions(DemoToggle("Fold to 2 rows", folded), DemoToggle("Long-press to delete", longPressDelete))
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        SearchHistoryViewDemo(folded = folded.value, longPressDelete = longPressDelete.value)
    }
}

@Composable
private fun IntentSeekBarDemo(
    enabled: Boolean,
    showThumb: Boolean,
    fixedColors: Boolean,
    size: IntentSeekBarSize,
) {
    var played by remember { mutableFloatStateOf(0.35f) }
    var buffered by remember { mutableFloatStateOf(0.65f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = str("Played {}%  ·  Buffered {}%", (played * 100).toInt(), (buffered * 100).toInt()),
            style = COUITheme.textStyles.footnote1,
        )
        // The fixed colour set is meant for media surfaces that stay dark in both themes.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .then(
                    if (fixedColors) {
                        Modifier
                            .background(color = Color(0xFF1B1B1B), shape = SquircleShape(12.dp))
                            .padding(16.dp)
                    } else {
                        Modifier
                    },
                ),
        ) {
            IntentSeekBar(
                value = played,
                onValueChange = {
                    played = it
                    if (buffered < it) buffered = it
                },
                secondaryProgress = buffered,
                enabled = enabled,
                colors = if (fixedColors) IntentSeekBarDefaults.fixedColors() else IntentSeekBarDefaults.colors(),
                showThumb = showThumb,
                size = size,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Slider(
            value = buffered,
            onValueChange = { buffered = it.coerceAtLeast(played) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}

@Composable
private fun TouchSearchViewDemo() {
    var selectedKey by remember { mutableStateOf("-") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
    ) {
        Text(
            text = str("Selected: {}", selectedKey),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(end = 56.dp),
        )
        TouchSearchView(
            keys = TouchSearchViewDefaults.NormalKeys,
            onKeySelected = { selectedKey = it },
            modifier = Modifier.align(Alignment.TopEnd),
            // The native setName() list: the entries filed under the held key.
            sectionNames = mapOf("#" to listOf("12306", "114", "淘宝")),
        )
    }
}

@Composable
private fun SearchHistoryViewDemo(folded: Boolean, longPressDelete: Boolean) {
    val demoHistory = DemoHistory.map { str(it) }
    var history by remember { mutableStateOf(demoHistory) }
    var lastClicked by remember { mutableStateOf("-") }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (history.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = str("History cleared"),
                    modifier = Modifier.weight(1f),
                )
                TextButton(text = str("Restore"), onClick = { history = demoHistory })
            }
        } else {
            SearchHistoryView(
                items = history,
                onItemClick = { lastClicked = it },
                onDeleteClick = { history = emptyList() },
                title = str("Recent searches"),
                deleteIconDescription = str("Delete search history"),
                maxRowFolded = if (folded) 2 else SearchHistoryViewDefaults.MaxRowFolded,
                onItemDelete = if (longPressDelete) {
                    { item -> history = history - item }
                } else {
                    null
                },
            )
            Text(
                text = str("Last clicked: {}", lastClicked),
                style = COUITheme.textStyles.footnote1,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}
