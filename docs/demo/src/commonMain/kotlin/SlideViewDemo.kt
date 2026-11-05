// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.SlideMenuItem
import io.github.suqi8.coui.kmp.basic.SlideView
import io.github.suqi8.coui.kmp.basic.rememberSlideViewState
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Delete
import kotlinx.coroutines.launch

@Composable
fun SlideViewDemo() {
    val rows = remember { mutableStateListOf("3 hours before", "4 hours before", "6 hours before") }
    val scope = rememberCoroutineScope()
    val rowColor = CardDefaults.defaultColors().color
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(demoBackground()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
        ) {
            Card(insideMargin = PaddingValues(0.dp)) {
                rows.forEach { row ->
                    key(row) {
                        val state = rememberSlideViewState()
                        SlideView(
                            menuItems = listOf(SlideMenuItem(text = null, icon = COUIIcons.Delete, contentDescription = "Delete")),
                            onMenuItemClick = {
                                scope.launch {
                                    state.delete()
                                    rows.remove(row)
                                }
                            },
                            state = state,
                        ) {
                            BasicComponent(modifier = Modifier.background(rowColor), title = row)
                        }
                    }
                }
            }
        }
    }
}
