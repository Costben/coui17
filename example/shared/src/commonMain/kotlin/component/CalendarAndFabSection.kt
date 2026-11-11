// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.CalendarView
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.DateValue
import io.github.suqi8.coui.kmp.basic.ExpandArrow
import io.github.suqi8.coui.kmp.basic.ExpandableList
import io.github.suqi8.coui.kmp.basic.ExpandableListDefaults
import io.github.suqi8.coui.kmp.basic.FloatingButton
import io.github.suqi8.coui.kmp.basic.FloatingButtonItem
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Add
import io.github.suqi8.coui.kmp.icon.extended.AddFolder
import io.github.suqi8.coui.kmp.icon.extended.Favorites
import io.github.suqi8.coui.kmp.icon.extended.Tune
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.blur_test_bg
import io.github.suqi8.coui.kmp.theme.COUITheme
import org.jetbrains.compose.resources.painterResource
import utils.WallpaperState
import utils.rememberBlurBackdrop

/** Renders the "CalendarView" gallery section content: a card wrapping the calendar demo. */
@Composable
fun CalendarViewDemoContent() {
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        CalendarViewDemo()
    }
}

/**
 * Renders the "FloatingButton" gallery section content: one button, either on a plain card or over a
 * wallpaper, where its blur, rim, spotlight and drag deformation show.
 */
@Composable
fun FloatingButtonDemoContent() {
    val overWallpaper = remember { mutableStateOf(false) }

    DemoOptions(DemoToggle("Over a wallpaper", overWallpaper))
    FloatingButtonDemo(overWallpaper = overWallpaper.value)
}

/** Renders the "ExpandableList" gallery section content: the list whose accordion behaviour is switched by the option. */
@Composable
fun ExpandableListDemoContent() {
    val accordion = remember { mutableStateOf(false) }

    DemoOptions(DemoToggle("Accordion (one group at a time)", accordion))
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ExpandableListDemo(accordion = accordion.value)
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
private fun FloatingButtonDemo(overWallpaper: Boolean) {
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
    val onItemClick: (FloatingButtonItem) -> Boolean = { item ->
        picked = item.label
        false
    }
    val backdrop = rememberBlurBackdrop()

    if (!overWallpaper) {
        Card(
            cornerRadius = CardDefaults.GroupCornerRadius,
            weight = CardDefaults.GroupWeight,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
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
                    onItemClick = onItemClick,
                    mainIcon = COUIIcons.Add,
                )
            }
        }
    } else {
        // Over a picture, so the `TYPE_FRAMEWORK_FAB` blur, the rim, the press light and the drag
        // deformation are all visible.
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
            ) {
                WallpaperState.bitmap?.let { wallpaper ->
                    Image(
                        bitmap = wallpaper,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } ?: Image(
                    painter = painterResource(Res.drawable.blur_test_bg),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Text(
                text = picked,
                color = Color.White,
                style = COUITheme.textStyles.footnote1,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
            )
            FloatingButton(
                items = items,
                expanded = expanded,
                onExpandedChange = { expanded = it },
                onItemClick = onItemClick,
                mainIcon = COUIIcons.Add,
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                backdrop = backdrop,
            )
        }
    }
}

@Composable
private fun ExpandableListDemo(accordion: Boolean) {
    var expandedGroups by remember { mutableStateOf(setOf(0)) }

    // Full bleed inside the card: a row's press highlight is clipped by the card's own squircle,
    // so an inset row painted a square block that stopped short of both card edges.
    Column(modifier = Modifier.fillMaxWidth()) {
        ExpandableList(
            groupCount = DemoSettingsGroups.size,
            childCount = { groupIndex -> DemoSettingsGroups[groupIndex].second.size },
            expandedGroups = expandedGroups,
            onExpandedGroupsChange = { expandedGroups = it },
            maxExpandedGroupCount = if (accordion) 1 else ExpandableListDefaults.MAX_EXPANDED_GROUP_COUNT,
            modifier = Modifier.fillMaxWidth(),
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
private fun SettingsGroupHeader(title: String, expanded: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = COUITheme.colorScheme.onBackground,
            style = COUITheme.textStyles.body1,
            modifier = Modifier.weight(1f),
        )
        ExpandArrow(
            expanded = expanded,
            contentDescription = if (expanded) "Collapse $title" else "Expand $title",
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
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    )
}

private val DemoSettingsGroups = listOf(
    "Display" to listOf("Brightness", "Dark mode", "Screen refresh rate"),
    "Sound" to listOf("Media volume", "Ringtone", "Do not disturb"),
    "Battery" to listOf("Battery saver", "Battery health"),
    "Network" to listOf("Wi-Fi", "Mobile data", "Hotspot"),
    "Storage" to listOf("Internal storage", "SD card"),
)
