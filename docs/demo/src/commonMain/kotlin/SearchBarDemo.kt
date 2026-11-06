// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.BasicComponentDefaults
import io.github.suqi8.coui.kmp.basic.SearchBar

@Composable
fun SearchBarDemo() {
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            var searchValue by remember { mutableStateOf("") }
            var expanded by remember { mutableStateOf(false) }
            SearchBar(
                query = searchValue,
                onQueryChange = { searchValue = it },
                onSearch = { expanded = false },
                onExpandedChange = { expanded = it },
                expanded = expanded,
                label = "Search",
                // The cancel capsule melts out of the field while the bar expands.
                cancelText = "Cancel",
                onCancel = {
                    expanded = false
                    searchValue = ""
                },
            ) {
                Column(
                    Modifier.fillMaxSize(),
                ) {
                    repeat(4) { idx ->
                        val resultText = "Suggestion $idx"
                        BasicComponent(
                            title = resultText,
                            titleColor = BasicComponentDefaults.titleColor(Color.White),
                            modifier = Modifier
                                .fillMaxWidth(),
                            onClick = {
                                searchValue = resultText
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}
