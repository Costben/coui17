// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop

/**
 * A toolbar row with a leading title and a trailing circular search button that expands into a
 * search field and a split "Cancel" capsule with dual-body metaball fusion and motion blur.
 *
 * When collapsed ([expanded] = `false`), [titleContent] (or [title]) is displayed on the start side
 * and a 40dp circular glass search button sits on the end side. Tapping the search button (or
 * setting [expanded] to `true`) fades the title out in place while the circular button stretches
 * from its resting center across the toolbar into a full-width search capsule and splits off the
 * trailing [cancelText] capsule. Tapping the cancel capsule or pressing the system back key
 * merges the capsules back into the circular button and restores the title.
 *
 * When [scrollBehavior] is supplied, the bar uses [TopAppBar]'s collapsible large-title layout
 * (transitioning between [largeTitle] and [title] on scroll) and expands the search field inside
 * the collapsed toolbar row.
 *
 * @param query The current search query text.
 * @param onQueryChange Called when the input text changes.
 * @param expanded Whether the toolbar search is expanded.
 * @param onExpandedChange Called when the expanded state changes.
 * @param modifier The [Modifier] applied to the toolbar surface.
 * @param title The title text shown when collapsed if [titleContent] is `null`.
 * @param largeTitle The expanded large title used when [scrollBehavior] is non-null.
 * @param placeholder The hint text shown inside the expanded search field when [query] is empty.
 * @param cancelText The text shown inside the trailing cancel capsule, or `null` for no cancel capsule.
 * @param onSearch Called when the IME search action is triggered.
 * @param onCancel Called when the cancel capsule or system back key is pressed while expanded.
 * @param enabled Whether the search button, input field, and cancel action are enabled.
 * @param showSearchButton Whether the trailing circular search button is visible when collapsed.
 * @param searchButtonContentDescription The accessibility content description of the search button.
 * @param scrollBehavior Optional [ScrollBehavior] enabling the collapsible large-title [TopAppBar] mode.
 * @param backdrop The [Backdrop] sampled by the toolbar glass and floating search capsules.
 * @param gradientColor Optional top bar gradient tint color (`Color.Unspecified` uses theme default).
 * @param extraActions Optional trailing action buttons placed beside the search button when collapsed.
 * @param trailingIcon Optional trailing composable inside the expanded search input field.
 * @param titleContent Optional custom title composable shown when collapsed.
 */
@Composable
fun TopAppBarSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    largeTitle: String = title,
    placeholder: String = "",
    cancelText: String? = SearchViewAnimateDefaults.CANCEL_TEXT,
    onSearch: (String) -> Unit = {},
    onCancel: () -> Unit = {
        onQueryChange("")
        onExpandedChange(false)
    },
    enabled: Boolean = true,
    showSearchButton: Boolean = true,
    searchButtonContentDescription: String = "Search",
    scrollBehavior: ScrollBehavior? = null,
    backdrop: Backdrop? = LocalBackdrop.current,
    gradientColor: Color = Color.Unspecified,
    extraActions: (@Composable RowScope.() -> Unit)? = null,
    trailingIcon: @Composable () -> Unit = {},
    titleContent: (@Composable () -> Unit)? = null,
) {
    TopAppBarSearchBarImpl(
        query = query,
        onQueryChange = onQueryChange,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        title = title,
        largeTitle = largeTitle,
        placeholder = placeholder,
        cancelText = cancelText,
        onSearch = onSearch,
        onCancel = onCancel,
        enabled = enabled,
        showSearchButton = showSearchButton,
        searchButtonContentDescription = searchButtonContentDescription,
        scrollBehavior = scrollBehavior,
        backdrop = backdrop,
        gradientColor = gradientColor,
        extraActions = extraActions,
        trailingIcon = trailingIcon,
        titleContent = titleContent,
    )
}

@Composable
internal expect fun TopAppBarSearchBarImpl(
    query: String,
    onQueryChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
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
)
