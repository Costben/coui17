// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.CouiBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.LocalCouiBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.couiBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.rememberCouiBackdrop
import io.github.suqi8.coui.kmp.kit.search.CouiSearchBarColors
import io.github.suqi8.coui.kmp.kit.search.rememberCouiSearchBarState
import io.github.suqi8.coui.kmp.kit.topappbar.LocalCouiToolbarBackdrop
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.kit.search.SearchBar as KitSearchBar

@Composable
internal actual fun SearchBarImpl(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier,
    expanded: Boolean,
    label: String,
    enabled: Boolean,
    textStyle: TextStyle?,
    colors: SearchBarColors,
    stroke: MaterialStrokeParams?,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    interactionSource: MutableInteractionSource?,
    glowColor: Color?,
    backdrop: Backdrop?,
    insideMargin: DpSize,
    cancelText: String?,
    onCancel: () -> Unit,
    metaBallFusion: Boolean,
    clearContentDescription: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val dark = COUITheme.colorScheme.background.luminance() < 0.5f
    val effectiveBackdrop = backdrop ?: LocalBackdrop.current
    val ambientCouiBackdrop = LocalCouiToolbarBackdrop.current ?: LocalCouiBackdrop.current
    val explicitBackdrop = ambientCouiBackdrop
        ?: effectiveBackdrop?.let { remember(it, density) { CouiBackdrop(it, density) } }
    val selfBackdrop = if (explicitBackdrop == null) rememberCouiBackdrop() else null
    val resolvedBackdrop = explicitBackdrop ?: selfBackdrop

    val kitColors = CouiSearchBarColors(
        foreground = if (textStyle?.color != null && textStyle.color != Color.Unspecified) {
            textStyle.color
        } else {
            COUITheme.colorScheme.onSurface
        },
        secondary = colors.labelColor,
        fallbackBackground = colors.backgroundColor,
        clear = colors.clearIconColor,
        icon = colors.iconColor,
    )
    val state = rememberCouiSearchBarState(expanded)
    LaunchedEffect(expanded) {
        state.updateTarget(expanded)
    }

    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val currentOnCancel by rememberUpdatedState(onCancel)
    val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)

    Column(modifier = modifier) {
        DisabledPassThrough(enabled) {
            if (selfBackdrop != null) {
                Spacer(
                    Modifier
                        .matchParentSize()
                        .couiBackdrop(selfBackdrop)
                        .background(COUITheme.colorScheme.background),
                )
            }
            KitSearchBar(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                query = query,
                onQueryChange = onQueryChange,
                onExpandedChange = { isExpanded ->
                    if (!isExpanded && expanded) {
                        currentOnCancel()
                    } else {
                        currentOnExpandedChange(isExpanded)
                    }
                },
                placeholder = label,
                cancelText = cancelText,
                clearDescription = clearContentDescription,
                dark = dark,
                backdrop = resolvedBackdrop,
                onSearch = { onSearch(query) },
                colors = kitColors,
                enabled = enabled,
                textStyle = textStyle,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                interactionSource = interactionSource,
                horizontalMargin = insideMargin.width,
                verticalMargin = insideMargin.height,
            )
        }

        AnimatedVisibility(visible = expanded) {
            content()
        }
    }

    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = expanded,
        onBackCompleted = {
            currentOnCancel()
        },
    )
}

@Composable
internal actual fun InputFieldImpl(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier,
    label: String,
    enabled: Boolean,
    textStyle: TextStyle?,
    colors: SearchBarColors,
    stroke: MaterialStrokeParams?,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    interactionSource: MutableInteractionSource?,
    glowColor: Color?,
    backdrop: Backdrop?,
    clearContentDescription: String,
) {
    val density = LocalDensity.current
    val dark = COUITheme.colorScheme.background.luminance() < 0.5f
    val effectiveBackdrop = backdrop ?: LocalBackdrop.current
    val ambientCouiBackdrop = LocalCouiToolbarBackdrop.current ?: LocalCouiBackdrop.current
    val explicitBackdrop = ambientCouiBackdrop
        ?: effectiveBackdrop?.let { remember(it, density) { CouiBackdrop(it, density) } }
    val selfBackdrop = if (explicitBackdrop == null) rememberCouiBackdrop() else null
    val resolvedBackdrop = explicitBackdrop ?: selfBackdrop

    val kitColors = CouiSearchBarColors(
        foreground = if (textStyle?.color != null && textStyle.color != Color.Unspecified) {
            textStyle.color
        } else {
            COUITheme.colorScheme.onSurface
        },
        secondary = colors.labelColor,
        fallbackBackground = colors.backgroundColor,
        clear = colors.clearIconColor,
        icon = colors.iconColor,
    )
    val state = rememberCouiSearchBarState(expanded)
    LaunchedEffect(expanded) {
        state.updateTarget(expanded)
    }

    DisabledPassThrough(enabled, modifier) {
        if (selfBackdrop != null) {
            Spacer(
                Modifier
                    .matchParentSize()
                    .couiBackdrop(selfBackdrop)
                    .background(COUITheme.colorScheme.background),
            )
        }
        KitSearchBar(
            modifier = Modifier,
            state = state,
            query = query,
            onQueryChange = onQueryChange,
            onExpandedChange = onExpandedChange,
            placeholder = label,
            cancelText = null,
            clearDescription = clearContentDescription,
            dark = dark,
            backdrop = resolvedBackdrop,
            onSearch = { onSearch(query) },
            colors = kitColors,
            enabled = enabled,
            textStyle = textStyle,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            interactionSource = interactionSource,
            horizontalMargin = 0.dp,
            verticalMargin = 0.dp,
        )
    }
}

/**
 * A disabled field stays inert: the kit's text field still hit-tests while disabled, which kept taps
 * from reaching a clickable container (a search pill that only opens a search page). An empty
 * pointer node above it takes the hit without consuming it, so ancestors still see the gesture.
 */
@Composable
private fun DisabledPassThrough(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        content()
        if (!enabled) {
            Box(Modifier.matchParentSize().pointerInput(Unit) {})
        }
    }
}
