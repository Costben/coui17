// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.SearchBar
import io.github.suqi8.coui.kmp.basic.SearchBarDefaults
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.search_cancel
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import org.jetbrains.compose.resources.stringResource
import utils.SearchStatus

/** COUIMoveEaseInterpolator: PathInterpolator(0.3f, 0f, 0.1f, 1f). */
private val MoveEase = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

// Search Pager Composable

/**
 * The full-screen search surface.
 *
 * Opening the search slides the bar from the collapsed pill up to the toolbar row: on the device the
 * capsule travels from y=614px to y=164px (≈116dp) over about 300ms, accelerating out of the pill
 * and easing into the docked row. [barTop] is the pill's measured window position and [dockedBarTop]
 * is the row the search page uses; the content below is laid out at the docked row for the whole
 * transition, so only the bar travels.
 *
 * @param barTop the collapsed search pill's top edge, in window coordinates.
 * @param dockedBarTop the search bar's top edge once the search page is up, in window coordinates.
 *   Defaults to [barTop], which pins the bar in place.
 * @param surfaceColor The colour the surface paints. It is the *page's* background, not
 *   `colorScheme.surface`: on the device the expanded search keeps the page behind it, while the
 *   ColorOS 17 dark scheme's `surface` is pure black — a full screen of it reads as the page going
 *   black the moment the search bar is tapped.
 */
@Composable
fun SearchStatus.SearchPager(
    onSearchStatusChange: (SearchStatus) -> Unit,
    barTop: Dp,
    defaultResult: @Composable () -> Unit,
    dockedBarTop: Dp = barTop,
    surfaceColor: Color = COUITheme.colorScheme.surface,
    result: LazyListScope.() -> Unit,
) {
    val searchStatus = this
    val onSearchStatusChangeUpdated = rememberUpdatedState(onSearchStatusChange)
    val searchStatusUpdated = rememberUpdatedState(searchStatus)
    val onCancelSearch = remember {
        {
            onSearchStatusChangeUpdated.value(
                searchStatusUpdated.value.copy(
                    searchText = "",
                    current = SearchStatus.Status.COLLAPSING,
                ),
            )
        }
    }
    val visible = !searchStatus.isCollapsed()
    // The bar travels between the two rows; the surface cross-fades underneath it. Both run for
    // 300ms so the exit ends exactly when the bar reaches the pill and the page can drop it.
    val barOffset = remember { Animatable(barTop, Dp.VectorConverter) }
    LaunchedEffect(searchStatus.current, barTop, dockedBarTop) {
        when (searchStatus.current) {
            SearchStatus.Status.EXPANDING, SearchStatus.Status.EXPANDED ->
                barOffset.animateTo(dockedBarTop, tween(300, easing = MoveEase))

            SearchStatus.Status.COLLAPSING ->
                barOffset.animateTo(barTop, tween(300, easing = MoveEase))

            // Collapsed, the bar is gone; keep it on the pill so the next open starts from there.
            SearchStatus.Status.COLLAPSED -> barOffset.snapTo(barTop)
        }
    }
    val surfaceAlpha by animateFloatAsState(
        targetValue = if (searchStatus.shouldExpand()) 1f else 0f,
        animationSpec = tween(300),
        finishedListener = {
            val current = searchStatusUpdated.value
            if (current.current == SearchStatus.Status.EXPANDING || current.current == SearchStatus.Status.COLLAPSING) {
                onSearchStatusChangeUpdated.value(current.onAnimationComplete())
            }
        },
        label = "SearchPagerSurfaceAlpha",
    )
    // The page is composed the moment the search opens, so a plain `visible` flag would start this
    // already true and skip the enter transition.
    val contentVisible = remember { MutableTransitionState(false) }
    contentVisible.targetState = searchStatus.shouldExpand()

    // The back key belongs to the whole surface, not to the cancel button: it has to work from the
    // first frame of the enter animation and on the last frame of the exit.
    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = visible,
        onBackCompleted = onCancelSearch,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(5f)
            .drawBehind { drawRect(surfaceColor.copy(alpha = surfaceAlpha)) }
            .then(
                // The a11y click sink only belongs to the surface while it is up. Collapsed, the
                // surface is invisible and already lets input through, and claiming a click here
                // leaves a full-screen node behind that hides the page's own semantics tree from
                // accessibility services.
                if (visible) {
                    Modifier
                        .semantics { onClick { false } }
                        .pointerInput(Unit) { }
                } else {
                    Modifier
                },
            ),
    ) {
        // The results never move: they are laid out below the docked row while the bar slides in
        // above them.
        AnimatedVisibility(
            visibleState = contentVisible,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = dockedBarTop + SearchBarDefaults.InputFieldMinHeight + SearchBarDefaults.InsideMargin.height * 2)
                .zIndex(1f),
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250)),
        ) {
            when (searchStatus.resultStatus) {
                SearchStatus.ResultStatus.DEFAULT -> defaultResult()

                SearchStatus.ResultStatus.EMPTY -> {}

                SearchStatus.ResultStatus.LOAD -> {}

                SearchStatus.ResultStatus.SHOW -> LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .overScrollVertical(),
                ) {
                    result()
                }
            }
        }
        if (visible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = barOffset.value),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SearchStatusBar(
                        searchStatus = searchStatus,
                        onSearchStatusChange = onSearchStatusChange,
                    )
                }
            }
        }
    }
}

/**
 * The expanded search page's bar: the library [SearchBar] with the device's "取消" capsule, so the
 * field and the button merge into one melt ball while the page opens.
 *
 * `updateDropShadow` is driven by the edit progress and `mNoShadowInNormalState` zeroes it while the
 * bar is collapsed, so the caustic light only exists on the search page: +18 grey levels under the
 * capsule, gone by 4.4dp. It ramps in with the expand animation rather than snapping on.
 */
@Composable
private fun SearchStatusBar(
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
) {
    val onSearchStatusChangeUpdated = rememberUpdatedState(onSearchStatusChange)
    val searchStatusUpdated = rememberUpdatedState(searchStatus)
    // The page composes this bar already expanded, so start it collapsed for one frame and let the
    // spring run: that is what makes the cancel capsule grow out of the field instead of popping in
    // with the page. Cancelling collapses it again while the page slides away.
    var barExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { barExpanded = true }
    LaunchedEffect(searchStatus.current) {
        if (searchStatus.current == SearchStatus.Status.COLLAPSING) barExpanded = false
    }
    val cancelLabel = stringResource(Res.string.search_cancel)
    SearchBar(
        query = searchStatus.searchText,
        onQueryChange = { onSearchStatusChange(searchStatus.copy(searchText = it)) },
        onSearch = {},
        onExpandedChange = { expanded ->
            if (!expanded) {
                onSearchStatusChangeUpdated.value(
                    searchStatusUpdated.value.copy(
                        searchText = "",
                        current = SearchStatus.Status.COLLAPSING,
                    ),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        expanded = barExpanded,
        label = searchStatus.label,
        glowColor = null,
        cancelText = cancelLabel,
        clearContentDescription = str("Clear"),
        onCancel = {
            onSearchStatusChangeUpdated.value(
                searchStatusUpdated.value.copy(
                    searchText = "",
                    current = SearchStatus.Status.COLLAPSING,
                ),
            )
        },
    )
}

/**
 * The gallery's [SearchBar] entry: the collapsed field on its own, and the same field with the
 * cancel capsule that melts out of it.
 */
@Composable
fun SearchBarDemo() {
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf("") }
    SearchBar(
        query = query,
        onQueryChange = { query = it },
        onSearch = { submitted = it },
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
        expanded = expanded,
        label = str("Search settings"),
        cancelText = str("Cancel"),
        clearContentDescription = str("Clear"),
    ) {
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = str("query: \"{}\"", query),
                    style = COUITheme.textStyles.footnote1,
                )
                if (submitted.isNotEmpty()) {
                    Text(
                        text = str("Searched") + ": " + submitted,
                        style = COUITheme.textStyles.footnote1,
                    )
                }
            }
        }
    }
}

/**
 * A collapsed search pill: the library [SearchBar] without a cancel capsule, so the gallery's
 * collapsed stand-ins share the expanded bar's field style.
 */
@Composable
fun SearchBarFake(
    label: String,
    searchBarTopPadding: Dp = 12.dp,
) {
    SearchBar(
        query = "",
        onQueryChange = { },
        onSearch = { },
        onExpandedChange = { },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = searchBarTopPadding),
        expanded = false,
        label = label,
        enabled = false,
        // A collapsed stand-in: the device's unedited search bar casts no caustic light.
        glowColor = null,
        insideMargin = DpSize(16.dp, 6.dp),
        cancelText = null,
    )
}
