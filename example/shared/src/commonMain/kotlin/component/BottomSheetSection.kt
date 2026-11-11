// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import io.github.suqi8.coui.kmp.basic.TextField
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

/**
 * Renders the "BottomSheet" gallery section content: the panel template row and one sheet row whose
 * host (overlay or window) comes from the option.
 */
@Composable
fun BottomSheetDemo() {
    val windowHost = remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var bottomSheetHoldDown by remember { mutableStateOf(false) }
    var bottomSheetDropdownSelectedOption by remember { mutableIntStateOf(0) }
    var bottomSheetSuperSwitchState by remember { mutableStateOf(true) }
    var showPanelTemplate by remember { mutableStateOf(false) }
    var panelTemplateHoldDown by remember { mutableStateOf(false) }

    DemoOptions(DemoToggle("Window host", windowHost))
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        // The reference panel first: the row under it is the same panel hosted as an overlay or as
        // a window, filled with interactive controls.
        ArrowPreference(
            title = str("Panel Template"),
            summary = str("The ColorOS 17 panel every sheet here follows"),
            onClick = {
                showPanelTemplate = true
                panelTemplateHoldDown = true
            },
            holdDownState = panelTemplateHoldDown,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("BottomSheet"),
            summary = if (windowHost.value) {
                str("Click to show a WindowBottomSheet")
            } else {
                str("Click to show an OverlayBottomSheet")
            },
            onClick = {
                showBottomSheet = true
                bottomSheetHoldDown = true
            },
            holdDownState = bottomSheetHoldDown,
        )
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
    // Both hosts stay composed so a sheet that is closing finishes on the host it opened on.
    SuperBottomSheetDemo(
        show = showBottomSheet && !windowHost.value,
        onDismissRequest = { showBottomSheet = false },
        dropdownSelectedIndex = bottomSheetDropdownSelectedOption,
        onDropdownSelectedIndexChange = { bottomSheetDropdownSelectedOption = it },
        switchChecked = bottomSheetSuperSwitchState,
        onSwitchCheckedChange = { bottomSheetSuperSwitchState = it },
        onDismissFinished = { bottomSheetHoldDown = false },
    )
    WindowBottomSheetDemo(
        show = showBottomSheet && windowHost.value,
        onDismissRequest = { showBottomSheet = false },
        dropdownSelectedIndex = bottomSheetDropdownSelectedOption,
        onDropdownSelectedIndexChange = { bottomSheetDropdownSelectedOption = it },
        switchChecked = bottomSheetSuperSwitchState,
        onSwitchCheckedChange = { bottomSheetSuperSwitchState = it },
        onDismissFinished = { bottomSheetHoldDown = false },
    )
}

@Composable
private fun SuperBottomSheetDemo(
    show: Boolean,
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

    OverlayBottomSheet(
        show = show,
        allowDismiss = allowDismiss,
        enableNestedScroll = enableNestedScroll,
        // Same dress as the ColorOS 17 panel template: no drag handle, the toolbar buttons carry
        // the panel's rim and halo, and the body runs on the template's insets.
        showDragHandle = false,
        insideMargin = PanelInsideMargin,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        startAction = { CloseCapsule(onClick = onDismissRequest) },
        endAction = { OverflowButton(onClick = {}) },
    ) {
        PanelDemoBody(
            title = str("BottomSheet (O)"),
            statusText = "OverlayBottomSheet",
            allowDismiss = allowDismiss,
            onAllowDismissChange = { allowDismiss = it },
            enableNestedScroll = enableNestedScroll,
            onEnableNestedScrollChange = { enableNestedScroll = it },
            switchChecked = switchChecked,
            onSwitchCheckedChange = onSwitchCheckedChange,
            onActionClick = onDismissRequest,
            dropdown = {
                OverlayDropdownPreference(
                    title = str("DropdownPref (O)"),
                    items = dropdownOptions,
                    selectedIndex = dropdownSelectedIndex,
                    onSelectedIndexChange = onDropdownSelectedIndexChange,
                )
            },
        )
    }
}

@Composable
private fun WindowBottomSheetDemo(
    show: Boolean,
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

    WindowBottomSheet(
        show = show,
        allowDismiss = allowDismiss,
        enableNestedScroll = enableNestedScroll,
        showDragHandle = false,
        insideMargin = PanelInsideMargin,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        // A window sheet animates out through its dismiss state rather than by flipping `show`.
        startAction = {
            val dismissState = LocalDismissState.current
            CloseCapsule(onClick = { dismissState?.invoke() })
        },
        endAction = { OverflowButton(onClick = {}) },
    ) {
        val dismissState = LocalDismissState.current
        PanelDemoBody(
            title = str("BottomSheet (W)"),
            statusText = "WindowBottomSheet",
            allowDismiss = allowDismiss,
            onAllowDismissChange = { allowDismiss = it },
            enableNestedScroll = enableNestedScroll,
            onEnableNestedScrollChange = { enableNestedScroll = it },
            switchChecked = switchChecked,
            onSwitchCheckedChange = onSwitchCheckedChange,
            onActionClick = { dismissState?.invoke() },
            dropdown = {
                WindowDropdownPreference(
                    title = str("DropdownPref (W)"),
                    items = dropdownOptions,
                    selectedIndex = dropdownSelectedIndex,
                    onSelectedIndexChange = onDropdownSelectedIndexChange,
                )
            },
        )
    }
}

/**
 * The body both sheet demos share, laid out on the panel template's rhythm: the large
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
    // The header and the capsule action stay put, as on a native panel: when the controls outgrow
    // the window only they scroll, so neither the title nor the action is cut by the scroll edge.
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
