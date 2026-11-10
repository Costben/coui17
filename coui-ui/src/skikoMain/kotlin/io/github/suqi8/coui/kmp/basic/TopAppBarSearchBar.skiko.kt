// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.theme.COUITheme

@Composable
internal actual fun TopAppBarSearchBarImpl(
    query: String,
    onQueryChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier,
    title: String,
    largeTitle: String,
    placeholder: String,
    cancelText: String?,
    onSearch: (String) -> Unit,
    onCancel: () -> Unit,
    enabled: Boolean,
    showSearchButton: Boolean,
    searchButtonContentDescription: String,
    scrollBehavior: ScrollBehavior?,
    backdrop: Backdrop?,
    gradientColor: Color,
    extraActions: (@Composable RowScope.() -> Unit)?,
    trailingIcon: @Composable () -> Unit,
    titleContent: (@Composable () -> Unit)?,
) {
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val currentOnCancel by rememberUpdatedState(onCancel)

    if (scrollBehavior != null) {
        TopAppBarWithSearch(
            title = title,
            largeTitle = largeTitle,
            modifier = modifier.fillMaxWidth(),
            scrollBehavior = scrollBehavior,
            gradientGlass = true,
            showDivider = false,
            titleAlpha = { if (expanded) 0f else 1f },
            forceCollapsed = expanded,
            searchOverlay = {
                if (expanded) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SearchBar(
                            query = query,
                            onQueryChange = onQueryChange,
                            onSearch = onSearch,
                            onExpandedChange = { isExpanded ->
                                if (!isExpanded) currentOnCancel() else currentOnExpandedChange(true)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            expanded = true,
                            label = placeholder,
                            enabled = enabled,
                            backdrop = backdrop,
                            insideMargin = DpSize(0.dp, 0.dp),
                            cancelText = cancelText,
                            onCancel = currentOnCancel,
                        )
                    }
                }
            },
            actions = {
                if (!expanded) {
                    if (showSearchButton) {
                        HighlightedIconButton(
                            onClick = { if (enabled) currentOnExpandedChange(true) },
                            enabled = enabled,
                            modifier = Modifier.couiTopAppBarActionItem(),
                        ) {
                            Icon(
                                imageVector = COUIIcons.Basic.Search,
                                contentDescription = searchButtonContentDescription,
                            )
                        }
                    }
                    extraActions?.invoke(this)
                }
            },
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (titleContent != null) {
                        titleContent()
                    } else {
                        Text(
                            text = title,
                            style = COUITheme.textStyles.title3.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = COUITheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        TopAppBarDefaults.ActionMenuItemSpacing,
                        Alignment.End,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showSearchButton) {
                        HighlightedIconButton(
                            onClick = { if (enabled) currentOnExpandedChange(true) },
                            enabled = enabled,
                        ) {
                            Icon(
                                imageVector = COUIIcons.Basic.Search,
                                contentDescription = searchButtonContentDescription,
                            )
                        }
                    }
                    extraActions?.invoke(this)
                }
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            SearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onExpandedChange = { isExpanded ->
                    if (!isExpanded) currentOnCancel() else currentOnExpandedChange(true)
                },
                modifier = Modifier.fillMaxWidth(),
                expanded = expanded,
                label = placeholder,
                enabled = enabled,
                backdrop = backdrop,
                insideMargin = DpSize(0.dp, 0.dp),
                cancelText = cancelText,
                onCancel = currentOnCancel,
            )
        }
    }
}
