// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalScrollBar
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.rememberScrollBarAdapter
import io.github.suqi8.coui.kmp.interfaces.ExperimentalScrollBarApi

/**
 * The horizontal twin of the vertical scrollbar the license page uses: a horizontally scrollable row
 * with [HorizontalScrollBar] bound to the same [rememberScrollBarAdapter]. The library exposes both
 * orientations from one implementation; only the vertical one had a demo before.
 */
@OptIn(ExperimentalScrollBarApi::class)
@Composable
fun HorizontalScrollBarDemo() {
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
            ) {
                repeat(12) { index ->
                    Text(
                        text = "Item $index",
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalScrollBar(
                adapter = rememberScrollBarAdapter(scrollState),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
