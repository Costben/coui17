// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import android.os.Build
import androidx.compose.foundation.Indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.isBlurEnabled
import io.github.suqi8.coui.kmp.kit.backdrop.CouiBackdrop
import io.github.suqi8.coui.kmp.kit.segmentbutton.CouiSegmentedControlDefaults
import io.github.suqi8.coui.kmp.kit.segmentbutton.SegmentedControl
import io.github.suqi8.coui.kmp.theme.COUITheme

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
    if (tabs.isEmpty()) return
    val density = LocalDensity.current
    val resolvedHeight = resolvedTabRowHeight(height)
    val containerColor = colors.backgroundColor(false)
    val indicatorColor = colors.backgroundColor(true)
    val hasContainerSurface = contourPadding > 0.dp && containerColor.alpha > 0f
    val blurActive = backdrop != null && isBlurEnabled()
    val isDarkTheme = COUITheme.colorScheme.background.luminance() < 0.5f

    val resolvedBackdrop = if (hasContainerSurface && blurActive && Build.VERSION.SDK_INT >= 34) {
        remember(backdrop, density) { CouiBackdrop(backdrop, density) }
    } else {
        null
    }
    resolvedBackdrop?.RetainWhileComposed()

    val canUseSegmentedControl = hasContainerSurface &&
        listState == null &&
        itemSpacing == 0.dp &&
        indication == null &&
        (resolvedBackdrop != null || (containerMaterial == null && indicatorMaterial == null))

    if (!canUseSegmentedControl) {
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
        return
    }

    BoxWithConstraints(modifier = modifier) {
        val minRequiredPx = with(density) { minWidth.roundToPx() } * tabs.size +
            with(density) { contourPadding.roundToPx() } * 2
        val overflows = constraints.hasBoundedWidth && minRequiredPx > constraints.maxWidth
        if (overflows) {
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
                listState = null,
                interactionSource = interactionSource,
                indication = null,
                indicatorMaterial = indicatorMaterial,
                containerMaterial = containerMaterial,
                backdrop = backdrop,
                dragSelectionEnabled = dragSelectionEnabled,
                commitOnRelease = commitOnRelease,
                touchMotionEnabled = touchMotionEnabled,
                tabModifier = tabModifier,
                tabContent = tabContent,
                modifier = Modifier,
            )
        } else {
            val compact = contourPadding < 3.dp || resolvedHeight <= 34.dp
            SegmentedControl(
                modifier = Modifier,
                labels = tabs,
                selectedIndex = selectedTabIndex,
                onSelected = onTabSelected,
                onReselected = onTabReselected,
                onTabClick = onTabClick,
                backdrop = resolvedBackdrop,
                dark = isDarkTheme,
                enabled = true,
                small = compact,
                dragSelectionEnabled = dragSelectionEnabled,
                commitOnRelease = commitOnRelease,
                touchMotionEnabled = touchMotionEnabled,
                minWidth = minWidth,
                maxWidth = maxWidth,
                height = resolvedHeight,
                cornerRadius = cornerRadius,
                contourPadding = contourPadding,
                segmentPadding = segmentPadding,
                contentAlignment = contentAlignment,
                interactionSource = interactionSource,
                textStyle = CouiSegmentedControlDefaults.textStyle(),
                containerColor = containerColor,
                indicatorColor = indicatorColor,
                contentColor = colors.contentColor(false),
                selectedContentColor = colors.contentColor(true),
                shadowColor = colors.indicatorShadowColor(),
                tabModifier = tabModifier,
                tabContent = tabContent,
            )
        }
    }
}
