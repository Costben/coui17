// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import io.github.suqi8.coui.kmp.basic.Slider
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.TouchSearchView
import io.github.suqi8.coui.kmp.basic.TouchSearchViewDefaults
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

fun LazyListScope.searchAndSliderSection() {
    item(key = "intentSeekBar") {
        SmallTitle(text = "IntentSeekBar")
        IntentSeekBarDemoContent()
    }
    item(key = "touchSearchView") {
        SmallTitle(text = "TouchSearchView")
        TouchSearchViewDemoContent()
    }
    item(key = "searchHistoryView") {
        SmallTitle(text = "SearchHistoryView")
        SearchHistoryViewDemoContent()
    }
}

/** Renders the "IntentSeekBar" gallery section content: a card of intent seek bar variants and a plain slider. */
@Composable
fun IntentSeekBarDemoContent() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        IntentSeekBarDemo()
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        IntentSeekBarSizeDemo()
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        IntentSeekBarFixedColorsDemo()
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
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        SearchHistoryViewDemo()
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        FoldableSearchHistoryViewDemo()
    }
}

@Composable
private fun IntentSeekBarDemo() {
    var played by remember { mutableFloatStateOf(0.35f) }
    var buffered by remember { mutableFloatStateOf(0.65f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = "Played ${(played * 100).toInt()}%  ·  Buffered ${(buffered * 100).toInt()}%",
            style = COUITheme.textStyles.footnote1,
        )
        IntentSeekBar(
            value = played,
            onValueChange = {
                played = it
                if (buffered < it) buffered = it
            },
            secondaryProgress = buffered,
            showThumb = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
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
private fun IntentSeekBarSizeDemo() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        var value by remember { mutableFloatStateOf(0.5f) }
        IntentSeekBarSize.entries.forEach { size ->
            Text(text = size.name, style = COUITheme.textStyles.footnote1)
            IntentSeekBar(
                value = value,
                onValueChange = { value = it },
                secondaryProgress = 0.75f,
                size = size,
                showThumb = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun IntentSeekBarFixedColorsDemo() {
    var value by remember { mutableFloatStateOf(0.4f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(color = Color(0xFF1B1B1B), shape = RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        IntentSeekBar(
            value = value,
            onValueChange = { value = it },
            secondaryProgress = 0.8f,
            colors = IntentSeekBarDefaults.fixedColors(),
            showThumb = true,
            modifier = Modifier.fillMaxWidth(),
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
            text = "Selected: $selectedKey",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(end = 56.dp),
        )
        TouchSearchView(
            keys = TouchSearchViewDefaults.NormalKeys,
            onKeySelected = { selectedKey = it },
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun SearchHistoryViewDemo() {
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
            )
            Text(
                text = "Last clicked: $lastClicked",
                style = COUITheme.textStyles.footnote1,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun FoldableSearchHistoryViewDemo() {
    val demoHistory = DemoHistory.map { str(it) }
    var history by remember { mutableStateOf(demoHistory.take(9)) }

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
            TextButton(text = str("Restore"), onClick = { history = demoHistory.take(9) })
        }
    } else {
        SearchHistoryView(
            items = history,
            onItemClick = {},
            onDeleteClick = { history = emptyList() },
            title = str("Recent searches (folded to 2 rows)"),
            maxRowFolded = 2,
        )
    }
}
