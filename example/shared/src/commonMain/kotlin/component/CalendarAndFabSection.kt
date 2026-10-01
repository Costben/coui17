// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.CalendarView
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.DateValue
import io.github.suqi8.coui.kmp.basic.ExpandableList
import io.github.suqi8.coui.kmp.basic.FloatingButton
import io.github.suqi8.coui.kmp.basic.FloatingButtonItem
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Add
import io.github.suqi8.coui.kmp.icon.extended.AddFolder
import io.github.suqi8.coui.kmp.icon.extended.ExpandLess
import io.github.suqi8.coui.kmp.icon.extended.ExpandMore
import io.github.suqi8.coui.kmp.icon.extended.Favorites
import io.github.suqi8.coui.kmp.icon.extended.Tune
import io.github.suqi8.coui.kmp.theme.COUITheme

fun LazyListScope.calendarAndFabSection() {
    item(key = "calendarView") {
        SmallTitle(text = "CalendarView")
        CalendarViewDemoContent()
    }
    item(key = "floatingButton") {
        SmallTitle(text = "FloatingButton")
        FloatingButtonDemoContent()
    }
    item(key = "expandableList") {
        SmallTitle(text = "ExpandableList")
        ExpandableListDemoContent()
    }
}

/** Renders the "CalendarView" gallery section content: a card wrapping the calendar demo. */
@Composable
fun CalendarViewDemoContent() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        CalendarViewDemo()
    }
}

/** Renders the "FloatingButton" gallery section content: a card wrapping the floating button demo. */
@Composable
fun FloatingButtonDemoContent() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        FloatingButtonDemo()
    }
}

/** Renders the "ExpandableList" gallery section content: a card with the expandable list and its accordion variant. */
@Composable
fun ExpandableListDemoContent() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ExpandableListDemo()
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        AccordionExpandableListDemo()
    }
}

@Composable
private fun CalendarViewDemo() {
    var value by remember { mutableStateOf(DateValue(2026, 9, 29)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        val selectedDate = value.day.toString().padStart(2, '0') + "." +
            value.month.toString().padStart(2, '0') + "." + value.year
        Text(
            text = str("Selected {}", selectedDate),
            color = COUITheme.colorScheme.onBackgroundVariant,
            style = COUITheme.textStyles.footnote1,
        )
        Spacer(modifier = Modifier.height(8.dp))
        CalendarView(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FloatingButtonDemo() {
    var expanded by remember { mutableStateOf(false) }
    val nothingPickedYet = str("Nothing picked yet")
    var picked by remember { mutableStateOf(nothingPickedYet) }
    val newFolderLabel = str("New folder")
    val addToFavoritesLabel = str("Add to favorites")
    val settingsLabel = str("Settings")
    val items = remember(newFolderLabel, addToFavoritesLabel, settingsLabel) {
        listOf(
            FloatingButtonItem(icon = COUIIcons.AddFolder, label = newFolderLabel),
            FloatingButtonItem(icon = COUIIcons.Favorites, label = addToFavoritesLabel),
            FloatingButtonItem(icon = COUIIcons.Tune, label = settingsLabel),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Text(
            text = picked,
            color = COUITheme.colorScheme.onBackgroundVariant,
            style = COUITheme.textStyles.footnote1,
            modifier = Modifier.align(Alignment.TopStart),
        )
        FloatingButton(
            items = items,
            expanded = expanded,
            onExpandedChange = { expanded = it },
            onItemClick = {
                picked = it.label
                false
            },
            mainIcon = COUIIcons.Add,
        )
    }
}

@Composable
private fun ExpandableListDemo() {
    var expandedGroups by remember { mutableStateOf(setOf(0)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        ExpandableList(
            groupCount = DemoSettingsGroups.size,
            childCount = { groupIndex -> DemoSettingsGroups[groupIndex].second.size },
            expandedGroups = expandedGroups,
            onExpandedGroupsChange = { expandedGroups = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            groupContent = { groupIndex, expanded ->
                SettingsGroupHeader(
                    title = str(DemoSettingsGroups[groupIndex].first),
                    expanded = expanded,
                )
            },
            childContent = { groupIndex, childIndex ->
                SettingsChildRow(text = str(DemoSettingsGroups[groupIndex].second[childIndex]))
            },
        )
    }
}

@Composable
private fun AccordionExpandableListDemo() {
    var expandedGroup by remember { mutableStateOf(setOf<Int>()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        ExpandableList(
            groupCount = DemoAccordionGroups.size,
            childCount = { groupIndex -> DemoAccordionGroups[groupIndex].second.size },
            expandedGroups = expandedGroup,
            onExpandedGroupsChange = { expandedGroup = it },
            maxExpandedGroupCount = 1,
            modifier = Modifier
                .fillMaxWidth()
                .height(224.dp),
            groupContent = { groupIndex, expanded ->
                SettingsGroupHeader(
                    title = str(DemoAccordionGroups[groupIndex].first),
                    expanded = expanded,
                )
            },
            childContent = { groupIndex, childIndex ->
                SettingsChildRow(text = str(DemoAccordionGroups[groupIndex].second[childIndex]))
            },
        )
    }
}

@Composable
private fun SettingsGroupHeader(title: String, expanded: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = COUITheme.colorScheme.onBackground,
            style = COUITheme.textStyles.body1,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) COUIIcons.ExpandLess else COUIIcons.ExpandMore,
            contentDescription = if (expanded) "Collapse $title" else "Expand $title",
            tint = COUITheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun SettingsChildRow(text: String) {
    Text(
        text = text,
        color = COUITheme.colorScheme.onBackgroundVariant,
        style = COUITheme.textStyles.footnote1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
    )
}

private val DemoSettingsGroups = listOf(
    "Display" to listOf("Brightness", "Dark mode", "Screen refresh rate"),
    "Sound" to listOf("Media volume", "Ringtone", "Do not disturb"),
    "Battery" to listOf("Battery saver", "Battery health"),
    "Network" to listOf("Wi-Fi", "Mobile data", "Hotspot"),
    "Storage" to listOf("Internal storage", "SD card"),
)

private val DemoAccordionGroups = listOf(
    "Account" to listOf("Profile", "Sign in"),
    "Privacy" to listOf("Permissions", "Location"),
    "About" to listOf("Version", "Build number"),
)
