// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.LocalAppStrings
import i18n.str
import io.github.suqi8.coui.kmp.basic.Badge
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Edit
import io.github.suqi8.coui.kmp.icon.extended.Sort
import io.github.suqi8.coui.kmp.menu.OverlayDropdownMenu
import io.github.suqi8.coui.kmp.menu.WindowDropdownMenu
import io.github.suqi8.coui.kmp.preference.OverlayDropdownPreference
import io.github.suqi8.coui.kmp.preference.WindowDropdownPreference
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * Renders the "Dropdown" gallery section content: one dropdown preference whose host, grouping,
 * option length and enabled state come from the options.
 */
@Composable
fun DropdownDemo() {
    val enabled = remember { mutableStateOf(true) }
    val windowHost = remember { mutableStateOf(false) }
    val grouped = remember { mutableStateOf(false) }
    val longOptions = remember { mutableStateOf(false) }
    var selected by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    val s = LocalAppStrings.current
    val items = remember(s, longOptions.value) {
        if (longOptions.value) {
            listOf(
                s["Option 1"],
                s["Long Option 2"],
                s["Long Long Option 3"],
                s["Long Long Long Option 4"],
                s["Long Long Long Long Option 5"],
                s["Long Long Long Long Long Option 6"],
                s["Long Long Long Long Long Long Option 7"],
                s["Long Long Long Long Long Long Long Option 8"],
                s["Long Long Long Long Long Long Long Long Option 9"],
                s["Long Long Long Long Long Long Long Long Long Option 10"],
                s["Long Long Long Long Long Long Long Long Long Long Option 11"],
                s["Long Long Long Long Long Long Long Long Long Long Long Option 12"],
            )
        } else {
            listOf(s["Option 1"], s["Option 2"], s["Option 3"], s["Option 4"])
        }
    }
    // The long list has more rows than the short one.
    if (selected > items.lastIndex) selected = 0

    var group1Selected by remember { mutableIntStateOf(0) }
    var group2Selected by remember { mutableIntStateOf(0) }
    var group3Selected by remember { mutableIntStateOf(0) }
    val groupedEntries = remember(s, group1Selected, group2Selected, group3Selected) {
        listOf(
            DropdownEntry(
                items = listOf(s["Option A-1"], s["Option A-2"])
                    .mapIndexed { index, text ->
                        DropdownItem(
                            text = text,
                            selected = group1Selected == index,
                            onClick = { group1Selected = index },
                        )
                    },
            ),
            DropdownEntry(
                items = listOf(s["Option B-1"], s["Option B-2"], s["Option B-3"])
                    .mapIndexed { index, text ->
                        DropdownItem(
                            text = text,
                            selected = group2Selected == index,
                            onClick = { group2Selected = index },
                        )
                    },
            ),
            DropdownEntry(
                items = listOf(s["Option C-1"], s["Option C-2"], s["Option C-3"], s["Option C-4"])
                    .mapIndexed { index, string ->
                        DropdownItem(
                            text = string,
                            enabled = index % 2 == 0,
                            selected = group3Selected == index,
                            onClick = { group3Selected = index },
                        )
                    },
            ),
        )
    }

    DemoOptions(
        DemoToggle("Enabled", enabled),
        DemoToggle("Window host", windowHost),
        DemoToggle("Grouped options", grouped),
        DemoToggle("Long options", longOptions),
    )
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        val title = str("DropdownPreference")
        val summary = if (expanded) str("Expanded") else str("Collapsed")
        when {
            windowHost.value && grouped.value -> WindowDropdownPreference(
                title = title,
                summary = summary,
                entries = groupedEntries,
                collapseOnSelection = false,
                enabled = enabled.value,
                onExpandedChange = { expanded = it },
            )

            windowHost.value -> WindowDropdownPreference(
                title = title,
                summary = summary,
                items = items,
                selectedIndex = selected,
                onSelectedIndexChange = { selected = it },
                enabled = enabled.value,
                onExpandedChange = { expanded = it },
            )

            grouped.value -> OverlayDropdownPreference(
                title = title,
                summary = summary,
                entries = groupedEntries,
                collapseOnSelection = false,
                enabled = enabled.value,
                onExpandedChange = { expanded = it },
            )

            else -> OverlayDropdownPreference(
                title = title,
                summary = summary,
                items = items,
                selectedIndex = selected,
                onSelectedIndexChange = { selected = it },
                enabled = enabled.value,
                onExpandedChange = { expanded = it },
            )
        }
    }
}

/**
 * Shows every COUI popup item variant in one popup each: leading icon, description, hint slot
 * (dot and count badge), group header, alert item, and a disabled row whose hint is suppressed.
 */
@Composable
fun DropdownItemVariants() {
    val windowHost = remember { mutableStateOf(false) }
    var variantsSelected by remember { mutableIntStateOf(0) }
    var headersSelected by remember { mutableIntStateOf(0) }
    val iconTint = COUITheme.colorScheme.onSurfaceContainer
    val s = LocalAppStrings.current

    val variantEntries = remember(s, variantsSelected, iconTint) {
        listOf(
            DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = s["Icon row"],
                        icon = { modifier ->
                            Icon(
                                modifier = modifier,
                                imageVector = COUIIcons.Edit,
                                tint = iconTint,
                                contentDescription = null,
                            )
                        },
                        selected = variantsSelected == 0,
                        onClick = { variantsSelected = 0 },
                    ),
                    DropdownItem(
                        text = s["Row with description"],
                        summary = s["A second line that explains what this option changes"],
                        selected = variantsSelected == 1,
                        onClick = { variantsSelected = 1 },
                    ),
                    DropdownItem(
                        text = s["Row with red dot"],
                        hint = { Badge() },
                        selected = variantsSelected == 2,
                        onClick = { variantsSelected = 2 },
                    ),
                    DropdownItem(
                        text = s["Row with count badge"],
                        hint = { Badge(count = 12) },
                        selected = variantsSelected == 3,
                        onClick = { variantsSelected = 3 },
                    ),
                    DropdownItem(
                        text = s["Icon, description and hint"],
                        summary = s["All three slots at once"],
                        icon = { modifier ->
                            Icon(
                                modifier = modifier,
                                imageVector = COUIIcons.Sort,
                                tint = iconTint,
                                contentDescription = null,
                            )
                        },
                        hint = { Badge(count = 3) },
                        selected = variantsSelected == 4,
                        onClick = { variantsSelected = 4 },
                    ),
                    DropdownItem(
                        text = s["Disabled row (hint hidden)"],
                        hint = { Badge(count = 9) },
                        enabled = false,
                    ),
                    DropdownItem(
                        text = s["Very long title that runs past the 248dp clamp and therefore "] +
                            s["wraps onto a third line before it finally ellipsizes"],
                        selected = variantsSelected == 6,
                        onClick = { variantsSelected = 6 },
                    ),
                    DropdownItem(
                        text = s["Delete everything"],
                        alert = true,
                        selected = variantsSelected == 7,
                        onClick = { variantsSelected = 7 },
                    ),
                ),
            ),
        )
    }

    val headerEntries = remember(s, headersSelected) {
        listOf(
            DropdownEntry(
                title = s["Sort by"],
                items = listOf(s["Name"], s["Date modified"]).mapIndexed { index, text ->
                    DropdownItem(
                        text = text,
                        selected = headersSelected == index,
                        onClick = { headersSelected = index },
                    )
                },
            ),
            DropdownEntry(
                title = s["A group header long enough to need its own second line before ellipsizing"],
                items = listOf(
                    DropdownItem(text = s["Ascending"], hint = { Badge() }),
                    DropdownItem(text = s["Descending"]),
                ),
            ),
            DropdownEntry(
                title = s["Danger zone"],
                items = listOf(DropdownItem(text = s["Reset all settings"], alert = true)),
            ),
        )
    }

    var lastTriggered by remember { mutableStateOf("") }

    val cascadingRowEntries = remember(s, lastTriggered) {
        listOf(
            DropdownEntry(
                title = s["Selection & submenu"],
                items = listOf(
                    DropdownItem(
                        text = s["Pinned"],
                        selected = lastTriggered == s["Pinned"],
                        onClick = { lastTriggered = s["Pinned"] },
                    ),
                    DropdownItem(
                        text = s["Standard mode"],
                        selected = lastTriggered == s["Standard mode"],
                        onClick = { lastTriggered = s["Standard mode"] },
                    ),
                    DropdownItem(
                        text = s["Submenu"],
                        children = listOf(s["Option 1"], s["Option 2"], s["Option 3"]).map { text ->
                            DropdownItem(
                                text = text,
                                selected = lastTriggered == text,
                                onClick = { lastTriggered = text },
                            )
                        },
                    ),
                ),
            ),
            DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = s["Clear"],
                        onClick = { lastTriggered = "" },
                    ),
                ),
            ),
        )
    }

    DemoOptions(DemoToggle("Window host", windowHost))
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        val rows = listOf(
            Triple(variantEntries, "Item variants", "Icon, description, hint, alert, clamping"),
            Triple(headerEntries, "Group headers", "Non-clickable 32dp header rows plus group dividers"),
            Triple(cascadingRowEntries, "Cascading Menu", "Tap row to expand menu with submenu"),
        )
        rows.forEachIndexed { index, (entries, title, summary) ->
            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            // Only the cascading menu closes on a pick; the other two keep the popup open to compare rows.
            val collapseOnSelection = index == rows.lastIndex
            if (windowHost.value) {
                WindowDropdownMenu(
                    entries = entries,
                    title = str(title),
                    summary = str(summary),
                    collapseOnSelection = collapseOnSelection,
                )
            } else {
                OverlayDropdownMenu(
                    entries = entries,
                    title = str(title),
                    summary = str(summary),
                    collapseOnSelection = collapseOnSelection,
                )
            }
        }
    }
}
