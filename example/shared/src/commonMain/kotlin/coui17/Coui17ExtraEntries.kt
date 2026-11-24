// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")
@file:OptIn(ExperimentalCOUIApi::class)

package coui17

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import component.DemoChoice
import component.DemoOptions
import component.DemoToggle
import i18n.str
import io.github.suqi8.coui.kmp.basic.Banner
import io.github.suqi8.coui.kmp.basic.BannerDefaults
import io.github.suqi8.coui.kmp.basic.BannerIndicator
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.Chip
import io.github.suqi8.coui.kmp.basic.ChipDefaults
import io.github.suqi8.coui.kmp.basic.CompleteState
import io.github.suqi8.coui.kmp.basic.CompleteStateStatus
import io.github.suqi8.coui.kmp.basic.EmptyState
import io.github.suqi8.coui.kmp.basic.EmptyStateSize
import io.github.suqi8.coui.kmp.basic.FlowLayout
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.ListDetailView
import io.github.suqi8.coui.kmp.basic.ListDetailViewDefaults
import io.github.suqi8.coui.kmp.basic.LoadProgressState
import io.github.suqi8.coui.kmp.basic.LunarDatePicker
import io.github.suqi8.coui.kmp.basic.SearchHistoryChip
import io.github.suqi8.coui.kmp.basic.SideNavigationBar
import io.github.suqi8.coui.kmp.basic.SidePaneLayout
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.cardListPositionOf
import io.github.suqi8.coui.kmp.basic.rememberSideNavigationBarState
import io.github.suqi8.coui.kmp.basic.rememberSidePaneLayoutState
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
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
import io.github.suqi8.coui.kmp.squircle.squircleBackground
import io.github.suqi8.coui.kmp.squircle.squircleSurface
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.LunarDateValue

/*
 * The component entries this app adds on top of the shared gallery content.
 *
 * The shared module demos 48 sections; the library ships more components than that. Each composable
 * below fills one of those gaps and follows the shared demos' layout contract — it owns its own 16dp
 * card inset and bottom spacing, so the page that renders it adds neither again.
 *
 * All user-facing copy goes through [str] with the English source string, so the language switch
 * keeps working and the Chinese table in `i18n/ZhComponents.kt` only has to carry the new keys.
 */

// ---------------------------------------------------------------------------------------------
// Basic components
// ---------------------------------------------------------------------------------------------

/** `EmptyState` with its size bucket, illustration, subtitle and action button picked from the options. */
@Composable
fun EmptyStateDemo() {
    val showSubtitle = remember { mutableStateOf(true) }
    val showAction = remember { mutableStateOf(true) }
    val showIllustration = remember { mutableStateOf(true) }
    val sizeIndex = remember { mutableIntStateOf(EmptyStateSize.Medium.ordinal) }
    var refreshes by remember { mutableIntStateOf(0) }
    val selectedSize = EmptyStateSize.entries[sizeIndex.intValue]

    DemoOptions(
        DemoToggle("Show subtitle", showSubtitle),
        DemoToggle("Show action button", showAction),
        DemoToggle("Show illustration", showIllustration),
        DemoChoice("Size", EmptyStateSize.entries.map { it.name }, sizeIndex),
    )
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        EmptyState(
            title = str("No results"),
            modifier = Modifier
                .fillMaxWidth()
                .height(if (selectedSize == EmptyStateSize.Small) 160.dp else 280.dp),
            subtitle = when {
                !showSubtitle.value -> null
                refreshes > 0 -> str("Refreshed {} times", refreshes)
                else -> str("Try a different keyword")
            },
            actionText = if (showAction.value) str("Retry") else null,
            onActionClick = if (showAction.value) {
                { refreshes++ }
            } else {
                null
            },
            illustration = if (showIllustration.value) {
                { EmptyStatePlaceholderIllustration() }
            } else {
                null
            },
            size = selectedSize,
        )
    }
}

/** A COUI continuous-curvature squircle tray placeholder for the empty-state illustration slot. */
@Composable
private fun EmptyStatePlaceholderIllustration() {
    val lineColor = COUITheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.25f)
    Column(
        modifier = Modifier
            .size(width = 72.dp, height = 60.dp)
            .squircleSurface(
                color = COUITheme.colorScheme.surfaceContainerHigh,
                cornerRadius = CardDefaults.CornerRadius,
                weight = CardDefaults.Weight,
            )
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .squircleBackground(color = lineColor, cornerRadius = 3.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .height(6.dp)
                .squircleBackground(color = lineColor, cornerRadius = 3.dp),
        )
    }
}

/** `CompleteState`'s three statuses. */
@Composable
fun CompleteStateDemo() {
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
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

/**
 * `FlowLayout` wrapping checkable chips across lines.
 *
 * This is the ColorOS 17 suggestion-chip pattern: `item_search_chip_history.xml` in Settings puts
 * `com.coui.appcompat.chip.COUIChip` rows inside a `COUICheckableGroup`, so every chip toggles its
 * own checked state and the group reports the selection. The chips keep the default selection-chip
 * height and capsule radius so they line up with the [SearchHistoryChip] capsules in the card below;
 * `couiTextBodyXS` label, `couiColorCard` while unchecked and `couiColorPrimary` with the on-color
 * label once checked.
 *
 * The "Fold to one row" option turns on `COUIFlowLayout` folding: the tags collapse to one row and
 * the last line carries the expand button, which grows the block over 400ms after a 100ms delay on
 * `COUIMoveEase`.
 *
 * The second card is the same flow layout filled with search history: each keyword is a
 * [SearchHistoryChip], so a long press springs a "×" out of the capsule and tapping it drops the
 * keyword from the list.
 */
@Composable
fun FlowLayoutDemo() {
    val foldable = remember { mutableStateOf(false) }
    val tags = listOf("Recommended", "Popular", "Nearby", "Archived", "Inbox", "Drafts", "Spam")
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var expanded by remember { mutableStateOf(false) }
    val recentSearches = remember {
        listOf("wireless earbuds", "screen protector", "usb-c hub", "laptop stand", "webcam")
    }
    var recent by remember { mutableStateOf(recentSearches) }
    val chips: @Composable () -> Unit = {
        tags.forEach { tag ->
            val checked = tag in selected
            Chip(
                selected = checked,
                onClick = {
                    selected = if (checked) selected - tag else selected + tag
                },
                label = str(tag),
                insideMargin = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }

    DemoOptions(DemoToggle("Fold to one row", foldable))
    FlowDemoCard(title = str("Tags")) {
        FlowLayout(
            modifier = Modifier.fillMaxWidth(),
            itemSpacing = 8.dp,
            lineSpacing = 8.dp,
            expanded = !foldable.value || expanded,
            onExpandedChange = { expanded = it },
            expandable = foldable.value,
            maxRowFolded = 1,
            expandButton = { toggle -> FlowFoldButton("+", toggle) },
            foldButton = { toggle -> FlowFoldButton("−", toggle) },
            content = chips,
        )
    }
    FlowDemoCard(
        title = str("Recent searches"),
        caption = str("Long-press a chip to delete it"),
    ) {
        if (recent.isEmpty()) {
            TextButton(text = str("Restore"), onClick = { recent = recentSearches })
        } else {
            FlowLayout(itemSpacing = 8.dp, lineSpacing = 8.dp) {
                recent.forEach { keyword ->
                    key(keyword) {
                        SearchHistoryChip(
                            text = str(keyword),
                            onClick = {},
                            onDelete = { recent = recent - keyword },
                            deleteContentDescription = str("Delete {}", keyword),
                        )
                    }
                }
            }
        }
    }
}

/** One section of the flow layout page: a group card carrying a title, an optional caption, content. */
@Composable
private fun FlowDemoCard(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    content: @Composable () -> Unit,
) {
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(text = title, style = COUITheme.textStyles.body2)
            if (caption != null) {
                Text(
                    text = caption,
                    style = COUITheme.textStyles.footnote1,
                    color = COUITheme.colorScheme.onSurfaceSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun FlowFoldButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(ChipDefaults.MinHeight)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = COUITheme.textStyles.button)
    }
}

private data class ListDetailSampleItem(
    val title: String,
    val summary: String,
    val description: String,
)

/**
 * `ListDetailView` with a selectable list pane driving the detail pane.
 *
 * Below [ListDetailViewDefaults.SplitModeMinWidth] (600dp) only one pane is visible at a time: the
 * list opens first, tapping a row opens its detail pane with a back action, and returning clears
 * the selection. At 600dp and wider both panes sit side by side with a hairline divider, and an
 * empty placeholder fills the detail pane until an item is selected.
 */
@Composable
fun ListDetailViewDemo() {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val previewSplit = remember { mutableStateOf(false) }
    val items = remember {
        listOf(
            ListDetailSampleItem(
                title = "Overview",
                summary = "Device status and quick summary",
                description = "Shows device health, storage usage, and recent system activity at a glance.",
            ),
            ListDetailSampleItem(
                title = "Display",
                summary = "Dark mode, refresh rate, font size",
                description = "Configure brightness, color mode, adaptive refresh rate, and typography scale.",
            ),
            ListDetailSampleItem(
                title = "Sound",
                summary = "Media volume, ringtone, haptics",
                description = "Adjust media and alarm volume, ringtones, and system haptic feedback.",
            ),
            ListDetailSampleItem(
                title = "Battery",
                summary = "Battery is at 80%",
                description = "Inspect app power usage, toggle battery saver, and manage charging optimization.",
            ),
        )
    }

    DemoOptions(DemoToggle("Preview split mode", previewSplit))
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val simulateSplit = previewSplit.value && maxWidth < ListDetailViewDefaults.SplitModeMinWidth
            val effectiveWidth = if (simulateSplit) 640.dp else maxWidth
            val isSplit = ListDetailViewDefaults.isSplitMode(effectiveWidth)

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isSplit) {
                        str("Split mode (≥ 600dp): list and detail side by side")
                    } else {
                        str("Single-pane mode (< 600dp): tap an item to view details")
                    },
                    style = COUITheme.textStyles.footnote1,
                    color = COUITheme.colorScheme.onSurfaceSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (simulateSplit) {
                                Modifier.horizontalScroll(rememberScrollState())
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    ListDetailView(
                        modifier = Modifier
                            .width(effectiveWidth)
                            .height(280.dp),
                        detailVisible = selectedIndex != null,
                        mainContent = {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                items.forEachIndexed { index, item ->
                                    BasicComponent(
                                        title = str(item.title),
                                        summary = str(item.summary),
                                        onClick = { selectedIndex = index },
                                        holdDownState = selectedIndex == index,
                                        endActions = {
                                            Icon(
                                                imageVector = COUIIcons.ChevronForward,
                                                contentDescription = null,
                                                tint = COUITheme.colorScheme.onSurfaceSecondary,
                                            )
                                        },
                                    )
                                    if (index < items.lastIndex) {
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        },
                        detailContent = {
                            val current = items[selectedIndex ?: 0]
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    IconButton(onClick = { selectedIndex = null }) {
                                        Icon(
                                            imageVector = COUIIcons.ChevronForward,
                                            contentDescription = str("Back"),
                                            modifier = Modifier.graphicsLayer { scaleX = -1f },
                                        )
                                    }
                                    Text(
                                        text = str(current.title),
                                        style = COUITheme.textStyles.headline2,
                                        color = COUITheme.colorScheme.onSurface,
                                    )
                                }
                                Text(
                                    text = str(current.summary),
                                    style = COUITheme.textStyles.body2,
                                    color = COUITheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp),
                                )
                                Text(
                                    text = str(current.description),
                                    style = COUITheme.textStyles.body2,
                                    color = COUITheme.colorScheme.onSurfaceSecondary,
                                    modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
                                )
                            }
                        },
                        emptyPageContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = str("Select an item to view details"),
                                    style = COUITheme.textStyles.body2,
                                    color = COUITheme.colorScheme.onSurfaceSecondary,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

/** `SideNavigationBar` opened by a button, hosted in a fixed-height box. */
@Composable
fun SideNavigationBarDemo() {
    val state = rememberSideNavigationBarState()
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
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
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

/** `Banner` pager plus a detached `BannerIndicator`. */
@Composable
fun BannerIndicatorDemo() {
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf("Featured", "New arrivals", "Recommended")
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { pages.size })
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Banner(
                pageCount = pages.size,
                autoScrollIntervalMillis = 4000,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                state = pagerState,
                itemColor = COUITheme.colorScheme.surfaceContainerHigh,
                indicatorColors = BannerDefaults.indicatorColors(),
                content = { index ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = str(pages[index]),
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
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
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
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
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
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
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
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
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
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
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

/** `CardEntrancePreference`, with its card type and summary picked from the options. */
@Composable
fun CardEntrancePreferenceDemo() {
    val large = remember { mutableStateOf(true) }
    val showSummary = remember { mutableStateOf(true) }
    val settingsIcon = rememberVectorPainter(COUIIcons.Settings)

    DemoOptions(
        DemoToggle("Large card", large),
        DemoToggle("Show summary", showSummary),
    )
    CardEntrancePreference(
        title = str("Display & brightness"),
        summary = if (showSummary.value) str("Dark mode, refresh rate, font size") else null,
        cardType = if (large.value) CardEntranceType.Large else CardEntranceType.Small,
        icon = settingsIcon,
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    )
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
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
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
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
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
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
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
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
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

/** `StatusSwitchingPreference` — the trailing status is either text or an icon, picked by the option. */
@Composable
fun StatusSwitchingPreferenceDemo() {
    val iconStatus = remember { mutableStateOf(false) }

    DemoOptions(DemoToggle("Status icon", iconStatus))
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        StatusSwitchingPreference(
            title = str("Connection"),
            status = if (iconStatus.value) {
                StatusSwitchingContent.Icon(
                    imageVector = COUIIcons.Refresh,
                    contentDescription = str("Syncing"),
                )
            } else {
                StatusSwitchingContent.Text(str("Connected"))
            },
            onClick = if (iconStatus.value) null else ({}),
            cardListPosition = CardListPosition.Full,
        )
    }
}

/** `CheckBoxPreferenceCategory` and `LoadingPreferenceCategory` — the two category headers, the latter with its loading state switched by the option. */
@Composable
fun CategoryHeaderPreferenceDemo() {
    var checked by remember { mutableStateOf(false) }
    val loading = remember { mutableStateOf(false) }

    DemoOptions(DemoToggle("Loading", loading))
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
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
                title = if (loading.value) str("Scanning…") else str("Up to date"),
                loading = loading.value,
                loadingText = if (loading.value) str("32%") else null,
                widget = if (loading.value) {
                    null
                } else {
                    {
                        Icon(
                            imageVector = COUIIcons.ChevronForward,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                },
            )
            BottomPreference()
        }
    }
}
