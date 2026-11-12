// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.Indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.github.suqi8.coui.kmp.blur.Backdrop

@Composable
internal actual fun SegmentButtonRowImpl(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabReselected: ((Int) -> Unit)?,
    onTabClick: ((Int) -> Unit)?,
    colors: TabRowColors,
    minWidth: Dp,
    maxWidth: Dp,
    height: Dp,
    cornerRadius: Dp,
    contourPadding: Dp,
    segmentPadding: Dp,
    itemSpacing: Dp,
    contentAlignment: Alignment,
    listState: LazyListState?,
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    indicatorMaterial: MaterialStrokeParams?,
    containerMaterial: MaterialStrokeParams?,
    backdrop: Backdrop?,
    dragSelectionEnabled: Boolean,
    commitOnRelease: Boolean,
    touchMotionEnabled: Boolean,
    tabModifier: ((index: Int) -> Modifier)?,
    tabContent: (@Composable (index: Int, label: String, selected: Boolean) -> Unit)?,
    modifier: Modifier,
) {
    CommonSegmentButtonRow(
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        onTabReselected = onTabReselected,
        onTabClick = onTabClick,
        colors = colors,
        minWidth = minWidth,
        maxWidth = maxWidth,
        height = height,
        cornerRadius = cornerRadius,
        contourPadding = contourPadding,
        segmentPadding = segmentPadding,
        itemSpacing = itemSpacing,
        contentAlignment = contentAlignment,
        listState = listState,
        interactionSource = interactionSource,
        indication = indication,
        indicatorMaterial = indicatorMaterial,
        containerMaterial = containerMaterial,
        backdrop = backdrop,
        dragSelectionEnabled = dragSelectionEnabled,
        commitOnRelease = commitOnRelease,
        touchMotionEnabled = touchMotionEnabled,
        tabModifier = tabModifier,
        tabContent = tabContent,
        modifier = modifier,
    )
}
