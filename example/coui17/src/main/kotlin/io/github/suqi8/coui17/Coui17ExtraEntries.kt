// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package io.github.suqi8.coui17

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Banner
import io.github.suqi8.coui.kmp.basic.BannerIndicator
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.Chip
import io.github.suqi8.coui.kmp.basic.CompleteState
import io.github.suqi8.coui.kmp.basic.CompleteStateStatus
import io.github.suqi8.coui.kmp.basic.EmptyState
import io.github.suqi8.coui.kmp.basic.EmptyStateSize
import io.github.suqi8.coui.kmp.basic.FlowLayout
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.ListDetailView
import io.github.suqi8.coui.kmp.basic.LoadProgressState
import io.github.suqi8.coui.kmp.basic.LunarDatePicker
import io.github.suqi8.coui.kmp.basic.SideNavigationBar
import io.github.suqi8.coui.kmp.basic.SidePaneLayout
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.cardListPositionOf
import io.github.suqi8.coui.kmp.basic.rememberSideNavigationBarState
import io.github.suqi8.coui.kmp.basic.rememberSidePaneLayoutState
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.ChevronForward
import io.github.suqi8.coui.kmp.icon.extended.Download
import io.github.suqi8.coui.kmp.icon.extended.Refresh
import io.github.suqi8.coui.kmp.icon.extended.Settings
import io.github.suqi8.coui.kmp.preference.AppInfoPreference
import io.github.suqi8.coui.kmp.preference.BottomPreference
import io.github.suqi8.coui.kmp.preference.CardEntrancePreference
import io.github.suqi8.coui.kmp.preference.CardEntranceType
import io.github.suqi8.coui.kmp.preference.CardInstructionItem
import io.github.suqi8.coui.kmp.preference.CardInstructionPreference
import io.github.suqi8.coui.kmp.preference.CheckBoxPreferenceCategory
import io.github.suqi8.coui.kmp.preference.CheckBoxWithDividerPreference
import io.github.suqi8.coui.kmp.preference.EditTextPreference
import io.github.suqi8.coui.kmp.preference.InputPreference
import io.github.suqi8.coui.kmp.preference.LoadInstallProgressPreference
import io.github.suqi8.coui.kmp.preference.LoadingPreferenceCategory
import io.github.suqi8.coui.kmp.preference.MarkWithDividerPreference
import io.github.suqi8.coui.kmp.preference.PagerFooterPreference
import io.github.suqi8.coui.kmp.preference.PagerHeaderPreference
import io.github.suqi8.coui.kmp.preference.SpannablePreference
import io.github.suqi8.coui.kmp.preference.StatusSwitchingContent
import io.github.suqi8.coui.kmp.preference.StatusSwitchingPreference
import io.github.suqi8.coui.kmp.preference.StepperPreference
import io.github.suqi8.coui.kmp.preference.SwitchWithDividerPreference
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.LunarDateValue

/*
 * The component entries this app adds on top of the shared gallery content.
 *
 * The shared module demos 48 sections; the library ships more components than that. Each composable
 * below fills one of those gaps and follows the shared demos' layout contract — it owns its own 16dp
 * card inset and bottom spacing, so [Coui17HomePage]'s `coui17Entry` wrapper adds neither again.
 *
 * All user-facing copy goes through [str] with the English source string, so the language switch
 * keeps working and the Chinese table in `i18n/ZhComponents.kt` only has to carry the new keys.
 */

// ---------------------------------------------------------------------------------------------
// Basic components
// ---------------------------------------------------------------------------------------------

/** `EmptyState` in its three size buckets, plus the explicit-action variant. */
@Composable
fun EmptyStateDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            EmptyState(
                title = str("No results"),
                subtitle = str("Try a different keyword"),
                size = EmptyStateSize.Medium,
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            EmptyState(
                title = str("Nothing here yet"),
                subtitle = str("Items you add will show up here"),
                actionText = str("Add"),
                onActionClick = {},
            )
        }
    }
}

/** `CompleteState`'s three statuses. */
@Composable
fun CompleteStateDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            CompleteState(status = CompleteStateStatus.Success, title = str("Success"))
            CompleteState(status = CompleteStateStatus.Failed, title = str("Failed"))
            CompleteState(status = CompleteStateStatus.Default, title = str("Default"))
        }
    }
}

/** `FlowLayout` wrapping chips across lines. */
@Composable
fun FlowLayoutDemo() {
    val tags = listOf("Compose", "Multiplatform", "ColorOS 17", "Material", "Design")
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        FlowLayout(
            modifier = Modifier.fillMaxWidth(),
            itemSpacing = 8.dp,
            lineSpacing = 8.dp,
        ) {
            tags.forEach { tag ->
                Chip(selected = false, onClick = {}, label = str(tag))
            }
        }
    }
}

/** `ListDetailView` with a selectable main pane driving the detail pane. */
@Composable
fun ListDetailViewDemo() {
    var selected by remember { mutableIntStateOf(0) }
    val items = listOf("Overview", "Display", "Sound", "Battery")
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ListDetailView(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            detailVisible = true,
            mainContent = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    items.forEachIndexed { index, item ->
                        Text(
                            text = str(item),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            color = if (index == selected) {
                                COUITheme.colorScheme.primary
                            } else {
                                COUITheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            },
            detailContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = str(items[selected]),
                        style = COUITheme.textStyles.headline2,
                        color = COUITheme.colorScheme.onSurface,
                    )
                }
            },
        )
    }
}

/** `SideNavigationBar` opened by a button, hosted in a fixed-height box. */
@Composable
fun SideNavigationBarDemo() {
    val state = rememberSideNavigationBarState()
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            SideNavigationBar(
                state = state,
                drawerContent = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        listOf("Inbox", "Starred", "Drafts").forEach { item ->
                            Text(
                                text = str(item),
                                modifier = Modifier.padding(16.dp),
                                color = COUITheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(onClick = { state.open() }) {
                        Text(text = str("Open drawer"))
                    }
                }
            }
        }
    }
}

/** `SidePaneLayout` with its fold icon. */
@Composable
fun SidePaneLayoutDemo() {
    val state = rememberSidePaneLayoutState()
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            SidePaneLayout(
                state = state,
                paneContent = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = str("Side pane"),
                            modifier = Modifier.padding(16.dp),
                            style = COUITheme.textStyles.headline2,
                            color = COUITheme.colorScheme.onSurface,
                        )
                    }
                },
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(onClick = { state.toggle() }) {
                        Text(text = str("Toggle pane"))
                    }
                }
            }
        }
    }
}

/** `LunarDatePicker`'s three wheel columns. */
@Composable
fun LunarDatePickerDemo() {
    var value by remember { mutableStateOf(LunarDateValue(2025, 1, 1)) }
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LunarDatePicker(
                value = value,
                onValueChange = { value = it },
            )
            Text(
                text = "${value.year}-${value.month}-${value.day}",
                modifier = Modifier.padding(top = 8.dp),
                color = COUITheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/** `Banner` pager plus a detached `BannerIndicator`. */
@Composable
fun BannerIndicatorDemo() {
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf("Featured", "New arrivals", "Recommended")
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Banner(
                pageCount = pages.size,
                autoScrollIntervalMillis = 4000,
                modifier = Modifier.fillMaxWidth(),
                content = { index ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = str(pages[index]),
                            modifier = Modifier.padding(vertical = 32.dp),
                            style = COUITheme.textStyles.headline2,
                            color = COUITheme.colorScheme.onSurface,
                        )
                    }
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            BannerIndicator(count = pages.size, currentIndex = page)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { page = (page + 1) % pages.size }) {
                Text(text = str("Next page"))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Preferences
// ---------------------------------------------------------------------------------------------

/** The whole run of divider-bearing preference rows, laid out as one card group. */
@Composable
fun DividerPreferenceGroupDemo() {
    var checked by remember { mutableStateOf(false) }
    var switched by remember { mutableStateOf(true) }
    var marked by remember { mutableIntStateOf(0) }
    val rows = 3
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CheckBoxWithDividerPreference(
                title = str("Checkbox with divider"),
                checked = checked,
                onCheckedChange = { checked = it },
                cardListPosition = cardListPositionOf(0, rows),
            )
            SwitchWithDividerPreference(
                checked = switched,
                onCheckedChange = { switched = it },
                title = str("Switch with divider"),
                cardListPosition = cardListPositionOf(1, rows),
            )
            MarkWithDividerPreference(
                title = str("Mark with divider"),
                checked = marked == 2,
                onClick = { marked = 2 },
                cardListPosition = cardListPositionOf(2, rows),
            )
        }
    }
}

/** `EditTextPreference` — tapping the row opens the edit dialog. */
@Composable
fun EditTextPreferenceDemo() {
    var value by remember { mutableStateOf("ColorOS") }
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        EditTextPreference(
            title = str("Device name"),
            summary = str("Shown to other devices nearby"),
            value = value,
            onValueChange = { value = it },
            confirmButtonText = str("OK"),
            cancelButtonText = str("Cancel"),
            cardListPosition = CardListPosition.Full,
        )
    }
}

/** `InputPreference` — the inline text field row. */
@Composable
fun InputPreferenceDemo() {
    var value by remember { mutableStateOf("") }
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        InputPreference(
            title = str("Nickname"),
            value = value,
            onValueChange = { value = it },
            showClearButton = true,
            cardListPosition = CardListPosition.Full,
        )
    }
}

/** `LoadInstallProgressPreference` in its loading and idle states. */
@Composable
fun LoadInstallProgressPreferenceDemo() {
    var state by remember { mutableStateOf(LoadProgressState.Default) }
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        LoadInstallProgressPreference(
            title = str("Update package"),
            summary = str("Version 2.1.0 · 48 MB"),
            onClick = {
                state = if (state == LoadProgressState.Default) {
                    LoadProgressState.Loading
                } else {
                    LoadProgressState.Default
                }
            },
            state = state,
            progress = if (state == LoadProgressState.Default) 0 else 62,
            cardListPosition = CardListPosition.Full,
        )
    }
}

/** `AppInfoPreference` — the centered app header block. */
@Composable
fun AppInfoPreferenceDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        AppInfoPreference(
            appName = str("COUI Gallery"),
            appVersion = str("Version 1.0.9"),
        )
    }
}

/** `CardEntrancePreference` in its small and large card types. */
@Composable
fun CardEntrancePreferenceDemo() {
    val settingsIcon = rememberVectorPainter(COUIIcons.Settings)
    val downloadIcon = rememberVectorPainter(COUIIcons.Download)
    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
        CardEntrancePreference(
            title = str("Display & brightness"),
            summary = str("Dark mode, refresh rate, font size"),
            cardType = CardEntranceType.Large,
            icon = settingsIcon,
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        CardEntrancePreference(
            title = str("Storage"),
            cardType = CardEntranceType.Small,
            icon = downloadIcon,
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** `CardInstructionPreference` — the description pager and the selector row. */
@Composable
fun CardInstructionPreferenceDemo() {
    var index by remember { mutableIntStateOf(0) }
    val items = listOf(
        CardInstructionItem(str("Sync your data"), str("Keep contacts and photos up to date")),
        CardInstructionItem(str("Find your device"), str("Locate it when it goes missing")),
        CardInstructionItem(str("Back up safely"), str("Encrypted copies in the cloud")),
    )
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        CardInstructionPreference(
            items = items,
            selectedIndex = index,
            onSelectedIndexChange = { index = it },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** `PagerHeaderPreference` and `PagerFooterPreference` framing a preference group. */
@Composable
fun PagerHeaderFooterPreferenceDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PagerHeaderPreference(
                title = str("Notifications"),
                summary = str("Choose what can reach you"),
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            PagerFooterPreference(
                summary = AnnotatedString(str("Changes apply to every signed-in device.")),
            )
        }
    }
}

/** `SpannablePreference` — a summary carrying inline emphasis. */
@Composable
fun SpannablePreferenceDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        SpannablePreference(
            title = str("Cloud sync"),
            summary = buildAnnotatedString {
                append(str("Syncing "))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(str("1,284")) }
                append(str(" items, "))
                withStyle(SpanStyle(color = COUITheme.colorScheme.primary)) {
                    append(str("32 pending"))
                }
            },
            onClick = {},
            cardListPosition = CardListPosition.Full,
        )
    }
}

/** `StepperPreference` — the numeric stepper row. */
@Composable
fun StepperPreferenceDemo() {
    var value by remember { mutableIntStateOf(3) }
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        StepperPreference(
            title = str("Home screen columns"),
            summary = str("Between 1 and 6"),
            value = value,
            onValueChange = { value = it },
            minValue = 1,
            maxValue = 6,
            cardListPosition = CardListPosition.Full,
        )
    }
}

/** `StatusSwitchingPreference` — the trailing status text and status icon forms. */
@Composable
fun StatusSwitchingPreferenceDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            StatusSwitchingPreference(
                title = str("Connection"),
                status = StatusSwitchingContent.Text(str("Connected")),
                onClick = {},
                cardListPosition = cardListPositionOf(0, 2),
            )
            StatusSwitchingPreference(
                title = str("Sync"),
                status = StatusSwitchingContent.Icon(
                    imageVector = COUIIcons.Refresh,
                    contentDescription = str("Syncing"),
                ),
                cardListPosition = cardListPositionOf(1, 2),
            )
        }
    }
}

/** `CheckBoxPreferenceCategory` and `LoadingPreferenceCategory` — the two category headers. */
@Composable
fun CategoryHeaderPreferenceDemo() {
    var checked by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CheckBoxPreferenceCategory(
                title = str("Select all"),
                state = if (checked) ToggleableState.On else ToggleableState.Off,
                onClick = { checked = !checked },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            LoadingPreferenceCategory(
                title = str("Scanning…"),
                loading = true,
                loadingText = str("32%"),
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            LoadingPreferenceCategory(
                title = str("Up to date"),
                loading = false,
                widget = {
                    Icon(
                        imageVector = COUIIcons.ChevronForward,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                },
            )
            BottomPreference()
        }
    }
}
