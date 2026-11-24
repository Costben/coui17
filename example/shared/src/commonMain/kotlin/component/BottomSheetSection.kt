// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coui17.ActionCapsule
import coui17.CloseCapsule
import coui17.OverflowButton
import coui17.PanelHeader
import coui17.PanelTemplate
import coui17.PanelTemplateDefaults
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.TextField
import io.github.suqi8.coui.kmp.basic.cardListPositionOf
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Check
import io.github.suqi8.coui.kmp.icon.basic.Close
import io.github.suqi8.coui.kmp.layout.BottomSheetDefaults
import io.github.suqi8.coui.kmp.layout.SheetActionButton
import io.github.suqi8.coui.kmp.overlay.OverlayBottomSheet
import io.github.suqi8.coui.kmp.preference.ArrowPreference
import io.github.suqi8.coui.kmp.preference.OverlayDropdownPreference
import io.github.suqi8.coui.kmp.preference.SliderPreference
import io.github.suqi8.coui.kmp.preference.SwitchPreference
import io.github.suqi8.coui.kmp.preference.WindowDropdownPreference
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalDismissState
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import io.github.suqi8.coui.kmp.utils.scrollEndHaptic
import io.github.suqi8.coui.kmp.window.WindowBottomSheet

private val SheetRowCounts = listOf(3, 8, 20)

/**
 * Renders the "BottomSheet" gallery section content: upstream-aligned configuration options
 * plus triggers for the configurable sheet, the ColorOS 17 panel template, and the embedded
 * controls sheet.
 */
@Composable
fun BottomSheetDemo() {
    val windowHost = remember { mutableStateOf(false) }
    val titleStyle = remember { mutableIntStateOf(1) }
    val showSubtitle = remember { mutableStateOf(false) }
    val showActions = remember { mutableStateOf(true) }
    val rowCountIndex = remember { mutableIntStateOf(2) }
    val showDragHandle = remember { mutableStateOf(false) }
    val draggable = remember { mutableStateOf(true) }
    val allowDismiss = remember { mutableStateOf(true) }
    val dismissOnClickOutside = remember { mutableStateOf(true) }
    val enableNestedScroll = remember { mutableStateOf(true) }
    val enablePullUp = remember { mutableStateOf(true) }
    val showInMaxHeight = remember { mutableStateOf(false) }
    val enableWindowDim = remember { mutableStateOf(true) }
    val topScrollBlur = remember { mutableStateOf(true) }

    var showPlaygroundSheet by remember { mutableStateOf(false) }
    var playgroundHoldDown by remember { mutableStateOf(false) }
    var showPanelTemplate by remember { mutableStateOf(false) }
    var panelTemplateHoldDown by remember { mutableStateOf(false) }
    var showControlsSheet by remember { mutableStateOf(false) }
    var controlsSheetHoldDown by remember { mutableStateOf(false) }
    var bottomSheetDropdownSelectedOption by remember { mutableIntStateOf(0) }
    var bottomSheetSuperSwitchState by remember { mutableStateOf(true) }

    DemoOptions(
        DemoToggle("Window host", windowHost),
        DemoChoice("Title", listOf("None", "Short", "Long"), titleStyle),
        DemoToggle("Show subtitle", showSubtitle),
        DemoToggle("Toolbar actions", showActions),
        DemoChoice("Content rows", SheetRowCounts.map { it.toString() }, rowCountIndex),
        DemoToggle("Drag handle", showDragHandle),
        DemoToggle("Draggable", draggable),
        DemoToggle("Allow dismiss", allowDismiss),
        DemoToggle("Dismiss on outside tap", dismissOnClickOutside),
        DemoToggle("Nested scroll", enableNestedScroll),
        DemoToggle("Pull up", enablePullUp),
        DemoToggle("Max height", showInMaxHeight),
        DemoToggle("Window dim", enableWindowDim),
        DemoToggle("Top scroll fade", topScrollBlur),
    )

    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ArrowPreference(
            title = str("BottomSheet"),
            summary = if (windowHost.value) {
                str("Click to show a WindowBottomSheet")
            } else {
                str("Click to show an OverlayBottomSheet")
            },
            onClick = {
                showPlaygroundSheet = true
                playgroundHoldDown = true
            },
            holdDownState = playgroundHoldDown,
            cardListPosition = cardListPositionOf(0, 3),
        )
        ArrowPreference(
            title = str("Panel Template"),
            summary = str("The ColorOS 17 panel every sheet here follows"),
            onClick = {
                showPanelTemplate = true
                panelTemplateHoldDown = true
            },
            holdDownState = panelTemplateHoldDown,
            cardListPosition = cardListPositionOf(1, 3),
        )
        ArrowPreference(
            title = str("Interactive Controls Sheet"),
            summary = str("Sheet containing live switches, slider, text field and dropdown"),
            onClick = {
                showControlsSheet = true
                controlsSheetHoldDown = true
            },
            holdDownState = controlsSheetHoldDown,
            cardListPosition = cardListPositionOf(2, 3),
        )
    }

    val title = when (titleStyle.intValue) {
        0 -> null
        1 -> str("BottomSheet")
        else -> str("A longer bottom sheet title that ellipsizes between actions")
    }
    val subtitle = if (showSubtitle.value) str("Subtitle description") else null
    val rowCount = SheetRowCounts[rowCountIndex.intValue]
    val dismissPlayground = { showPlaygroundSheet = false }
    val closeAction: @Composable () -> Unit = {
        val dismissState = LocalDismissState.current
        SheetActionButton(onClick = { dismissState?.invoke() ?: dismissPlayground() }) {
            Icon(
                imageVector = COUIIcons.Basic.Close,
                contentDescription = str("Cancel"),
                modifier = Modifier.size(BottomSheetDefaults.ActionButtonIconSize),
                tint = COUITheme.colorScheme.onBackground,
            )
        }
    }
    val confirmAction: @Composable () -> Unit = {
        val dismissState = LocalDismissState.current
        SheetActionButton(onClick = { dismissState?.invoke() ?: dismissPlayground() }) {
            Icon(
                imageVector = COUIIcons.Basic.Check,
                contentDescription = str("Confirm"),
                modifier = Modifier.size(BottomSheetDefaults.ActionButtonIconSize),
                tint = COUITheme.colorScheme.onBackground,
            )
        }
    }
    val startAction = if (showActions.value) closeAction else null
    val endAction = if (showActions.value) confirmAction else null

    // Both hosts stay composed so a closing sheet finishes on the host it opened on.
    OverlayBottomSheet(
        show = showPlaygroundSheet && !windowHost.value,
        title = title,
        subtitle = subtitle,
        startAction = startAction,
        endAction = endAction,
        enableWindowDim = enableWindowDim.value,
        showDragHandle = showDragHandle.value,
        showInMaxHeight = showInMaxHeight.value,
        allowDismiss = allowDismiss.value,
        dismissOnClickOutside = dismissOnClickOutside.value,
        draggable = draggable.value,
        enablePullUp = enablePullUp.value,
        enableNestedScroll = enableNestedScroll.value,
        topScrollBlur = topScrollBlur.value,
        onDismissRequest = dismissPlayground,
        onDismissFinished = { playgroundHoldDown = false },
    ) {
        SheetRowsContent(rowCount = rowCount)
    }

    WindowBottomSheet(
        show = showPlaygroundSheet && windowHost.value,
        title = title,
        subtitle = subtitle,
        startAction = startAction,
        endAction = endAction,
        enableWindowDim = enableWindowDim.value,
        showDragHandle = showDragHandle.value,
        showInMaxHeight = showInMaxHeight.value,
        allowDismiss = allowDismiss.value,
        dismissOnClickOutside = dismissOnClickOutside.value,
        draggable = draggable.value,
        enablePullUp = enablePullUp.value,
        enableNestedScroll = enableNestedScroll.value,
        topScrollBlur = topScrollBlur.value,
        onDismissRequest = dismissPlayground,
        onDismissFinished = { playgroundHoldDown = false },
    ) {
        SheetRowsContent(rowCount = rowCount)
    }

    PanelTemplate(
        show = showPanelTemplate,
        onDismissRequest = { showPanelTemplate = false },
        onDismissFinished = { panelTemplateHoldDown = false },
        title = str("Title"),
        statusText = str("Status text"),
        settingsText = str("List item"),
        actionText = str("Button"),
    )

    ControlsBottomSheetDemo(
        show = showControlsSheet && !windowHost.value,
        windowHost = false,
        onDismissRequest = { showControlsSheet = false },
        dropdownSelectedIndex = bottomSheetDropdownSelectedOption,
        onDropdownSelectedIndexChange = { bottomSheetDropdownSelectedOption = it },
        switchChecked = bottomSheetSuperSwitchState,
        onSwitchCheckedChange = { bottomSheetSuperSwitchState = it },
        onDismissFinished = { controlsSheetHoldDown = false },
    )
    ControlsBottomSheetDemo(
        show = showControlsSheet && windowHost.value,
        windowHost = true,
        onDismissRequest = { showControlsSheet = false },
        dropdownSelectedIndex = bottomSheetDropdownSelectedOption,
        onDropdownSelectedIndexChange = { bottomSheetDropdownSelectedOption = it },
        switchChecked = bottomSheetSuperSwitchState,
        onSwitchCheckedChange = { bottomSheetSuperSwitchState = it },
        onDismissFinished = { controlsSheetHoldDown = false },
    )
}

@Composable
private fun SheetRowsContent(
    rowCount: Int,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .scrollEndHaptic()
            .overScrollVertical(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
    ) {
        item {
            Card(
                cornerRadius = PanelTemplateDefaults.SettingsRowRadius,
                colors = CardDefaults.defaultColors(color = COUITheme.colorScheme.surfaceContainer),
            ) {
                repeat(rowCount) { index ->
                    ArrowPreference(
                        title = "${str("List item")} ${index + 1}",
                        onClick = {},
                        cardListPosition = cardListPositionOf(index, rowCount),
                    )
                }
            }
            Spacer(
                Modifier.windowInsetsBottomHeight(
                    WindowInsets.navigationBars.union(WindowInsets.captionBar),
                ),
            )
        }
    }
}

@Composable
private fun ControlsBottomSheetDemo(
    show: Boolean,
    windowHost: Boolean,
    onDismissRequest: () -> Unit,
    dropdownSelectedIndex: Int,
    onDropdownSelectedIndexChange: (Int) -> Unit,
    switchChecked: Boolean,
    onSwitchCheckedChange: (Boolean) -> Unit,
    onDismissFinished: () -> Unit,
) {
    var allowDismiss by remember { mutableStateOf(true) }
    var enableNestedScroll by remember { mutableStateOf(true) }
    val dropdownOptions = listOf(str("Option 1"), str("Option 2"))

    val body: @Composable () -> Unit = {
        val dismissState = LocalDismissState.current
        PanelDemoBody(
            title = str(if (windowHost) "BottomSheet (W)" else "BottomSheet (O)"),
            statusText = if (windowHost) "WindowBottomSheet" else "OverlayBottomSheet",
            allowDismiss = allowDismiss,
            onAllowDismissChange = { allowDismiss = it },
            enableNestedScroll = enableNestedScroll,
            onEnableNestedScrollChange = { enableNestedScroll = it },
            switchChecked = switchChecked,
            onSwitchCheckedChange = onSwitchCheckedChange,
            onActionClick = { if (windowHost) dismissState?.invoke() else onDismissRequest() },
            dropdown = {
                if (windowHost) {
                    WindowDropdownPreference(
                        title = str("DropdownPref (W)"),
                        items = dropdownOptions,
                        selectedIndex = dropdownSelectedIndex,
                        onSelectedIndexChange = onDropdownSelectedIndexChange,
                    )
                } else {
                    OverlayDropdownPreference(
                        title = str("DropdownPref (O)"),
                        items = dropdownOptions,
                        selectedIndex = dropdownSelectedIndex,
                        onSelectedIndexChange = onDropdownSelectedIndexChange,
                    )
                }
            },
        )
    }

    if (windowHost) {
        WindowBottomSheet(
            show = show,
            allowDismiss = allowDismiss,
            enableNestedScroll = enableNestedScroll,
            showDragHandle = false,
            insideMargin = PanelInsideMargin,
            onDismissRequest = onDismissRequest,
            onDismissFinished = onDismissFinished,
            startAction = {
                val dismissState = LocalDismissState.current
                CloseCapsule(onClick = { dismissState?.invoke() })
            },
            endAction = { OverflowButton(onClick = {}) },
            content = body,
        )
    } else {
        OverlayBottomSheet(
            show = show,
            allowDismiss = allowDismiss,
            enableNestedScroll = enableNestedScroll,
            showDragHandle = false,
            insideMargin = PanelInsideMargin,
            onDismissRequest = onDismissRequest,
            onDismissFinished = onDismissFinished,
            startAction = { CloseCapsule(onClick = onDismissRequest) },
            endAction = { OverflowButton(onClick = {}) },
            content = body,
        )
    }
}

/**
 * The body both controls sheet demos share, laid out on the panel template's rhythm: the large
 * title and status line, then the controls grouped into 23dp-radius rows on `surfaceContainer`,
 * the line text field on the panel itself, and the capsule action closing the panel.
 */
@Composable
private fun PanelDemoBody(
    title: String,
    statusText: String,
    allowDismiss: Boolean,
    onAllowDismissChange: (Boolean) -> Unit,
    enableNestedScroll: Boolean,
    onEnableNestedScrollChange: (Boolean) -> Unit,
    switchChecked: Boolean,
    onSwitchCheckedChange: (Boolean) -> Unit,
    onActionClick: () -> Unit,
    dropdown: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PanelHeader(title = title, statusText = statusText)
        Spacer(Modifier.height(PanelSectionGap))
        PanelControls(
            allowDismiss = allowDismiss,
            onAllowDismissChange = onAllowDismissChange,
            enableNestedScroll = enableNestedScroll,
            onEnableNestedScrollChange = onEnableNestedScrollChange,
            switchChecked = switchChecked,
            onSwitchCheckedChange = onSwitchCheckedChange,
            dropdown = dropdown,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.height(PanelTemplateDefaults.ActionGap))
        ActionCapsule(text = str("Confirm"), onClick = onActionClick)
    }
}

/** The scrolling part of [PanelDemoBody]: the grouped controls. */
@Composable
private fun PanelControls(
    allowDismiss: Boolean,
    onAllowDismissChange: (Boolean) -> Unit,
    enableNestedScroll: Boolean,
    onEnableNestedScrollChange: (Boolean) -> Unit,
    switchChecked: Boolean,
    onSwitchCheckedChange: (Boolean) -> Unit,
    dropdown: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .scrollEndHaptic()
            .overScrollVertical(),
    ) {
        item {
            PanelCard {
                SwitchPreference(
                    title = str("Allow Dismiss"),
                    summary = str("Drag or Back to dismiss"),
                    checked = allowDismiss,
                    onCheckedChange = onAllowDismissChange,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SwitchPreference(
                    title = str("Enable NestedScroll"),
                    summary = str("Scroll content vs Drag sheet"),
                    checked = enableNestedScroll,
                    onCheckedChange = onEnableNestedScrollChange,
                )
            }
            Spacer(Modifier.height(PanelRowGap))
        }
        item {
            var textFieldValue by remember { mutableStateOf("") }
            TextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                label = str("TextField"),
            )
            Spacer(Modifier.height(PanelRowGap))
        }
        item {
            var sliderValue by remember { mutableFloatStateOf(0.5f) }
            PanelCard {
                SliderPreference(
                    title = str("Slider"),
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                dropdown()
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SwitchPreference(
                    title = str("SwitchPref"),
                    checked = switchChecked,
                    onCheckedChange = onSwitchCheckedChange,
                )
            }
        }
    }
}

/** A group of rows dressed like the template's list row: `surfaceContainer`, 23dp squircle. */
@Composable
private fun PanelCard(content: @Composable () -> Unit) {
    Card(
        cornerRadius = PanelTemplateDefaults.SettingsRowRadius,
        colors = CardDefaults.defaultColors(color = COUITheme.colorScheme.surfaceContainer),
    ) {
        content()
    }
}

private val PanelInsideMargin = DpSize(PanelTemplateDefaults.SideInset, PanelTemplateDefaults.BottomInset)

/** Gap between the status line and the first group of controls. */
private val PanelSectionGap = 24.dp

/** Gap between two groups of controls. */
private val PanelRowGap = 12.dp
