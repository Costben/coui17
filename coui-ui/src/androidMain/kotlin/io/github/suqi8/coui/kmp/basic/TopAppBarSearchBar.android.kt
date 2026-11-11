// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package io.github.suqi8.coui.kmp.basic

import android.os.Build
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.kit.backdrop.CouiBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.LocalCouiBackdrop
import io.github.suqi8.coui.kmp.kit.text.CouiTextKind
import io.github.suqi8.coui.kmp.kit.text.rememberCouiTextStyle
import io.github.suqi8.coui.kmp.kit.theme.CouiText
import io.github.suqi8.coui.kmp.kit.topappbar.CouiTopAppBarDefaults
import io.github.suqi8.coui.kmp.kit.topappbar.LocalCouiToolbarBackdrop
import io.github.suqi8.coui.kmp.kit.topappbar.LocalCouiToolbarOverlay
import io.github.suqi8.coui.kmp.kit.topappbar.LocalCouiToolbarSampleBlurRadius
import io.github.suqi8.coui.kmp.kit.topappbar.TopAppBarActionVisibility
import io.github.suqi8.coui.kmp.kit.topappbar.TopAppBarButtonGroup
import io.github.suqi8.coui.kmp.kit.topappbar.TopAppBarSearch
import io.github.suqi8.coui.kmp.kit.topappbar.TopAppBarSearchAction
import io.github.suqi8.coui.kmp.kit.topappbar.TopAppBarSearchButton
import io.github.suqi8.coui.kmp.kit.topappbar.TopAppBarSurface
import io.github.suqi8.coui.kmp.kit.topappbar.rememberCouiTopAppBarSearchState
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
    val density = LocalDensity.current
    val dark = COUITheme.colorScheme.background.luminance() < 0.5f
    val effectiveBackdrop = backdrop ?: LocalBackdrop.current
    val localCouiBackdrop = LocalCouiToolbarBackdrop.current ?: LocalCouiBackdrop.current
    val resolvedBackdrop = localCouiBackdrop
        ?: effectiveBackdrop?.let { remember(it, density) { CouiBackdrop(it, density) } }
    resolvedBackdrop?.RetainWhileComposed()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var focusPending by remember { mutableStateOf(false) }

    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val currentOnCancel by rememberUpdatedState(onCancel)
    val currentOnSearch by rememberUpdatedState(onSearch)

    LaunchedEffect(expanded) {
        if (expanded) {
            focusPending = true
        } else {
            focusPending = false
            keyboard?.hide()
            focusManager.clearFocus()
        }
    }

    val closeSearch = {
        focusPending = false
        keyboard?.hide()
        focusManager.clearFocus()
        currentOnCancel()
    }

    val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = expanded,
        onBackCompleted = closeSearch,
    )

    if (scrollBehavior != null) {
        if (Build.VERSION.SDK_INT >= 34 && resolvedBackdrop != null) {
            val state = rememberCouiTopAppBarSearchState(expanded)
            SharedTransitionScope { overlayModifier ->
                CompositionLocalProvider(
                    LocalCouiToolbarBackdrop provides resolvedBackdrop,
                    LocalCouiToolbarOverlay provides this@SharedTransitionScope,
                ) {
                    Box(modifier = modifier.fillMaxWidth().then(overlayModifier)) {
                        TopAppBarWithSearch(
                            title = title,
                            largeTitle = largeTitle,
                            scrollBehavior = scrollBehavior,
                            gradientGlass = true,
                            showDivider = false,
                            titleAlpha = { state.motion.toolbarAlpha },
                            forceCollapsed = expanded,
                            searchOverlay = {
                                TopAppBarSearch(
                                    modifier = Modifier.fillMaxWidth(),
                                    state = state,
                                    query = query,
                                    onQueryChange = onQueryChange,
                                    onSearch = {
                                        keyboard?.hide()
                                        focusManager.clearFocus()
                                        currentOnSearch(query)
                                    },
                                    placeholder = placeholder,
                                    focusPending = focusPending,
                                    onFocused = { focusPending = false },
                                    enabled = enabled,
                                    endPadding = 0.dp,
                                    toolbarHeight = TopAppBarDefaults.CollapsedHeight,
                                    trailingIcon = trailingIcon,
                                    action = if (!cancelText.isNullOrEmpty()) {
                                        { CouiText(text = cancelText, maxLines = 1, softWrap = false) }
                                    } else {
                                        null
                                    },
                                    onActionClick = if (enabled && !cancelText.isNullOrEmpty()) closeSearch else null,
                                    title = {},
                                )
                            },
                            actions = {
                                TopAppBarButtonGroup {
                                    TopAppBarActionVisibility(visible = showSearchButton) {
                                        TopAppBarSearchButton(
                                            state = state,
                                            onClick = {
                                                if (enabled) {
                                                    focusPending = true
                                                    currentOnExpandedChange(true)
                                                }
                                            },
                                            contentDescription = searchButtonContentDescription,
                                            enabled = enabled,
                                        )
                                    }
                                    if (extraActions != null) {
                                        TopAppBarSearchAction(state = state) {
                                            Row(
                                                modifier = Modifier.padding(start = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(
                                                    TopAppBarDefaults.ActionMenuItemSpacing,
                                                    Alignment.End,
                                                ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                content = extraActions,
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        } else {
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
                            SearchViewAnimate(
                                query = query,
                                onQueryChange = onQueryChange,
                                expanded = true,
                                onExpandedChange = { isExpanded ->
                                    if (!isExpanded) closeSearch() else currentOnExpandedChange(true)
                                },
                                hint = placeholder,
                                cancelText = cancelText ?: SearchViewAnimateDefaults.CANCEL_TEXT,
                                onSearch = currentOnSearch,
                                modifier = Modifier.fillMaxWidth(),
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
        }
        return
    }

    val toolbarTitleStyle = rememberCouiTextStyle(CouiTextKind.Toolbar)
    val resolvedTitle: @Composable () -> Unit = titleContent ?: {
        BasicText(
            text = title,
            style = toolbarTitleStyle.copy(color = COUITheme.colorScheme.onSurface),
            maxLines = 1,
        )
    }

    val embedded = gradientColor == Color.Transparent
    val rowHeight = if (embedded) 44.dp else 64.dp
    val barContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (Build.VERSION.SDK_INT >= 34 && resolvedBackdrop != null) {
                val state = rememberCouiTopAppBarSearchState(expanded)
                TopAppBarSearch(
                    modifier = Modifier.weight(1f),
                    state = state,
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = {
                        keyboard?.hide()
                        focusManager.clearFocus()
                        currentOnSearch(query)
                    },
                    placeholder = placeholder,
                    focusPending = focusPending,
                    onFocused = { focusPending = false },
                    enabled = enabled,
                    endPadding = if (extraActions != null) 0.dp else 6.dp,
                    toolbarHeight = rowHeight,
                    trailingIcon = trailingIcon,
                    action = if (!cancelText.isNullOrEmpty()) {
                        { CouiText(text = cancelText, maxLines = 1, softWrap = false) }
                    } else {
                        null
                    },
                    onActionClick = if (enabled && !cancelText.isNullOrEmpty()) closeSearch else null,
                    title = resolvedTitle,
                )
                TopAppBarButtonGroup {
                    TopAppBarActionVisibility(visible = showSearchButton) {
                        TopAppBarSearchButton(
                            state = state,
                            onClick = {
                                if (enabled) {
                                    focusPending = true
                                    currentOnExpandedChange(true)
                                }
                            },
                            contentDescription = searchButtonContentDescription,
                            enabled = enabled,
                        )
                    }
                    if (extraActions != null) {
                        TopAppBarSearchAction(state = state) {
                            Row(
                                modifier = Modifier.padding(start = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(
                                    TopAppBarDefaults.ActionMenuItemSpacing,
                                    Alignment.End,
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                content = extraActions,
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!expanded) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            resolvedTitle()
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
                    } else {
                        SearchViewAnimate(
                            query = query,
                            onQueryChange = onQueryChange,
                            expanded = true,
                            onExpandedChange = { isExpanded ->
                                if (!isExpanded) closeSearch() else currentOnExpandedChange(true)
                            },
                            hint = placeholder,
                            cancelText = cancelText ?: SearchViewAnimateDefaults.CANCEL_TEXT,
                            onSearch = currentOnSearch,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (embedded) {
        // Embedded in a host bar, the toolbar samples the host's raw page content rather than its
        // own blurred surface, so every button in it (the search key, the capsules, the extra
        // actions) blurs the sample at the surface's reference radius instead of showing it sharp.
        val sampleBlurRadius = if (localCouiBackdrop == null) {
            with(density) { CouiTopAppBarDefaults.referenceBlurRadius.toPx() }
        } else {
            0f
        }
        SharedTransitionScope { overlayModifier ->
            CompositionLocalProvider(
                LocalCouiToolbarBackdrop provides resolvedBackdrop,
                LocalCouiToolbarOverlay provides this@SharedTransitionScope,
                LocalCouiToolbarSampleBlurRadius provides sampleBlurRadius,
            ) {
                Box(modifier = modifier.fillMaxWidth().then(overlayModifier)) {
                    barContent()
                }
            }
        }
    } else {
        TopAppBarSurface(
            modifier = modifier.fillMaxWidth(),
            backdrop = resolvedBackdrop,
            gradientColor = gradientColor,
            dark = dark,
        ) {
            barContent()
        }
    }
}
