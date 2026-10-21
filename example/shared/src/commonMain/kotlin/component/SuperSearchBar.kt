// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import i18n.str
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.InputField
import io.github.suqi8.coui.kmp.basic.SearchBarActionButton
import io.github.suqi8.coui.kmp.basic.SearchBarActionButtonDefaults
import io.github.suqi8.coui.kmp.basic.SearchBarDefaults
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.icon.basic.SearchCleanup
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.search_cancel
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import org.jetbrains.compose.resources.stringResource
import utils.SearchStatus

/** COUIMoveEaseInterpolator: PathInterpolator(0.3f, 0f, 0.1f, 1f). */
private val MoveEase = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

/** The functional button's spring: COUI response 0.5s, bounce 0.25. */
private val ButtonSpring = spring<Float>(dampingRatio = 0.75f, stiffness = 400f)

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
    expandBar: @Composable (SearchStatus, (SearchStatus) -> Unit) -> Unit = { searchStatus, onStatusChange ->
        SearchBar(searchStatus, onStatusChange)
    },
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
                .padding(top = dockedBarTop + SearchBarActionButtonDefaults.Height)
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
                    expandBar(searchStatus, onSearchStatusChange)
                }
            }
        }
    }
}

/**
 * The expanded search bar: the field capsule plus the "取消" capsule, laid out the way
 * `COUISearchBar` arranges them in its edit state.
 *
 * The field is the flexible child, so growing the cancel capsule's slot shrinks the field exactly
 * like the device's `doWidthChangedAnimate` (450ms). The cancel capsule itself scales up from 0.5
 * with COUI's fun-button spring and fades in over 350ms.
 */
@Composable
fun SearchBar(
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
) {
    val onSearchStatusChangeUpdated = rememberUpdatedState(onSearchStatusChange)
    val searchStatusUpdated = rememberUpdatedState(searchStatus)
    val onClearSearch = remember {
        { onSearchStatusChangeUpdated.value(searchStatusUpdated.value.copy(searchText = "")) }
    }
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
    // `updateDropShadow` is driven by the edit progress and `mNoShadowInNormalState` zeroes it while
    // the bar is collapsed, so the light only exists on the search page: +18 grey levels under the
    // capsule, gone by 4.4dp. It ramps in with the expand animation rather than snapping on.
    val expandProgress by animateFloatAsState(
        targetValue = if (searchStatus.shouldExpand()) 1f else 0f,
        animationSpec = tween(300),
        label = "SearchBarGlow",
    )
    val glowColor = SearchBarDefaults.causticGlowColor().let {
        it.copy(alpha = it.alpha * expandProgress)
    }
    // The page is composed the moment the search opens, so a plain `visible` flag would start this
    // already true and skip the enter transition. Starting from `false` makes the cancel capsule
    // grow out of the end edge and the field shrink beside it, as the device's
    // `doWidthChangedAnimate` does.
    val cancelVisible = remember { MutableTransitionState(false) }
    cancelVisible.targetState = searchStatus.shouldExpand()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InputField(
            query = searchStatus.searchText,
            onQueryChange = { onSearchStatusChange(searchStatus.copy(searchText = it)) },
            label = searchStatus.label,
            leadingIcon = {
                // The device's leading slot is 40dp, 36dp of it the icon view; the glyph reads
                // 17.5dp. Same numbers as the collapsed home pill.
                Icon(
                    imageVector = COUIIcons.Basic.Search,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(start = 10.dp, end = 7.dp),
                    tint = COUITheme.colorScheme.onSurfaceContainerHigh,
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    searchStatus.searchText.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    Icon(
                        imageVector = COUIIcons.Basic.SearchCleanup,
                        tint = COUITheme.colorScheme.onSurface,
                        contentDescription = str("Clean"),
                        modifier = Modifier
                            .size(44.dp)
                            .padding(start = 8.dp, end = 16.dp)
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = onClearSearch,
                            ),
                    )
                }
            },
            colors = SearchBarDefaults.searchBarColors(),
            stroke = SearchBarDefaults.searchBarStroke(),
            glowColor = glowColor,
            modifier = Modifier.weight(1f),
            onSearch = {},
            expanded = true,
            onExpandedChange = { },
        )
        AnimatedVisibility(
            visibleState = cancelVisible,
            enter = fadeIn(tween(350)) +
                expandHorizontally(
                    animationSpec = tween(450, easing = MoveEase),
                    expandFrom = Alignment.End,
                ) +
                scaleIn(initialScale = 0.5f, animationSpec = ButtonSpring),
            exit = fadeOut(tween(350)) +
                shrinkHorizontally(
                    animationSpec = tween(450, easing = MoveEase),
                    shrinkTowards = Alignment.End,
                ) +
                scaleOut(targetScale = 0.5f, animationSpec = ButtonSpring),
        ) {
            // The gap rides inside the animated slot, so the field is full width whenever the
            // button is gone.
            Box(modifier = Modifier.padding(start = SearchBarActionButtonDefaults.StartGap)) {
                SearchBarActionButton(
                    text = stringResource(Res.string.search_cancel),
                    onClick = onCancelSearch,
                    glowColor = glowColor,
                )
            }
        }
    }
}

@Composable
fun SearchBarFake(
    label: String,
    searchBarTopPadding: Dp = 12.dp,
) {
    InputField(
        query = "",
        onQueryChange = { },
        label = label,
        // A collapsed stand-in: the device's unedited search bar casts no caustic light.
        glowColor = null,
        leadingIcon = {
            Icon(
                imageVector = COUIIcons.Basic.Search,
                contentDescription = str("Search"),
                modifier = Modifier
                    .size(44.dp)
                    .padding(start = 16.dp, end = 8.dp),
                tint = COUITheme.colorScheme.onSurfaceContainerHigh,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = searchBarTopPadding, bottom = 6.dp),
        onSearch = { },
        enabled = false,
        expanded = false,
        onExpandedChange = { },
    )
}
