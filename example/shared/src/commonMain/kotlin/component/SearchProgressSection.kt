// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.ChipTabData
import io.github.suqi8.coui.kmp.basic.ChipTabGroup
import io.github.suqi8.coui.kmp.basic.DropdownImpl
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.basic.LoadProgressButton
import io.github.suqi8.coui.kmp.basic.LoadProgressButtonSize
import io.github.suqi8.coui.kmp.basic.LoadProgressState
import io.github.suqi8.coui.kmp.basic.PopupMenuRotation
import io.github.suqi8.coui.kmp.basic.SearchViewAnimate
import io.github.suqi8.coui.kmp.basic.SearchViewAnimateType
import io.github.suqi8.coui.kmp.basic.Slider
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.popupMenuRotation
import io.github.suqi8.coui.kmp.basic.popupMenuRotationPositionProvider
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalDismissState
import io.github.suqi8.coui.kmp.window.WindowListPopup
import kotlinx.coroutines.delay

fun LazyListScope.searchProgressSection() {
    item(key = "searchViewAnimate") {
        SmallTitle(text = "SearchViewAnimate")
        SearchViewAnimateDemo()
    }
    item(key = "loadProgressButton") {
        SmallTitle(text = "LoadProgressButton")
        LoadProgressButtonDemo()
    }
    item(key = "popupMenuRotation") {
        SmallTitle(text = "PopupMenuRotation")
        PopupMenuRotationDemo()
    }
}

/** Renders the "SearchViewAnimate" gallery section content (a search bar with a collapse slider and query readout). */
@Composable
fun SearchViewAnimateDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        MagneticSearchBarDemo()
    }
}

/** Renders the "LoadProgressButton" gallery section content (state buttons and a pill button with progress). */
@Composable
fun LoadProgressButtonDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        LoadProgressStatesDemo()
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        LoadProgressPillDemo()
    }
}

/** Renders the "PopupMenuRotation" gallery section content (a rotated popup menu with rotation tabs). */
@Composable
fun PopupMenuRotationDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        RotatingPopupMenuDemo()
    }
}

@Composable
private fun MagneticSearchBarDemo() {
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(SearchViewAnimateType.NonInstantSearch) }
    // setSearchViewAnimateHeightPercent is fed by the host; drag it to collapse the bar into its icon.
    var collapsePercent by remember { mutableFloatStateOf(1f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SearchViewAnimate(
            query = query,
            onQueryChange = { query = it },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            hint = "Search settings",
            type = type,
            collapsePercent = collapsePercent,
            onSearch = { expanded = false },
            modifier = Modifier.fillMaxWidth(),
        )
        Slider(
            value = collapsePercent,
            onValueChange = { collapsePercent = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                text = if (expanded) "Collapse" else "Expand",
                onClick = { expanded = !expanded },
            )
            TextButton(
                text = if (type == SearchViewAnimateType.InstantSearch) "Instant" else "Non-instant",
                onClick = {
                    type = if (type == SearchViewAnimateType.InstantSearch) {
                        SearchViewAnimateType.NonInstantSearch
                    } else {
                        SearchViewAnimateType.InstantSearch
                    }
                },
            )
        }
        Text(
            text = "query: \"$query\"",
            style = COUITheme.textStyles.footnote1,
        )
    }
}

@Composable
private fun LoadProgressStatesDemo() {
    // performClick() calls toggle(): 0 -> 1, 1 -> 2, 2 -> 1, 3 -> 1.
    var state by remember { mutableStateOf(LoadProgressState.Default) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LoadProgressState.entries.forEach { entry ->
                Box(contentAlignment = Alignment.Center) {
                    LoadProgressButton(
                        onClick = { state = entry.toggled() },
                        state = entry,
                        size = LoadProgressButtonSize.Circle,
                    )
                }
            }
        }
        Text(
            text = "state: ${state.name} (${state.value})",
            style = COUITheme.textStyles.footnote1,
        )
    }
}

@Composable
private fun LoadProgressPillDemo() {
    var state by remember { mutableStateOf(LoadProgressState.Default) }
    var progress by remember { mutableIntStateOf(0) }

    LaunchedEffect(state) {
        if (state == LoadProgressState.Loading) {
            progress = 0
            while (progress < 100) {
                delay(120)
                progress += 5
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LoadProgressButton(
            onClick = { state = state.toggled() },
            state = state,
            progress = progress,
            text = if (state == LoadProgressState.Loading) "$progress%" else "Download",
            size = LoadProgressButtonSize.Large,
        )
        Text(
            text = "state: ${state.name}, progress: $progress / 100",
            style = COUITheme.textStyles.footnote1,
        )
    }
}

@Composable
private fun RotatingPopupMenuDemo() {
    var showPopup by remember { mutableStateOf(false) }
    var rotationIndex by remember { mutableIntStateOf(0) }
    val rotations = remember {
        listOf(PopupMenuRotation.None, PopupMenuRotation.Degrees90, PopupMenuRotation.Degrees270)
    }
    val rotation = rotations[rotationIndex]
    val rows = remember { listOf("Copy", "Share", "Delete") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChipTabGroup(
            tabs = remember {
                listOf(
                    ChipTabData(label = "0°"),
                    ChipTabData(label = "90°"),
                    ChipTabData(label = "270°"),
                )
            },
            selectedIndex = rotationIndex,
            onSelectedChange = { rotationIndex = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            TextButton(text = "Show rotated menu", onClick = { showPopup = true })
            WindowListPopup(
                show = showPopup,
                // MenuRotationHelper.rotatedLocateWidth/Height: the locate pass sees the swapped size.
                popupPositionProvider = popupMenuRotationPositionProvider(rotation),
                // RotationLayoutOut: the content is rotated about its own centre.
                popupModifier = Modifier.popupMenuRotation(rotation),
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
    }
}
