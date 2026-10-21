// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.offset
import androidx.compose.ui.zIndex
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import i18n.str
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.InputField
import io.github.suqi8.coui.kmp.basic.SearchBarDefaults
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.materialShadowEdge
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.icon.basic.SearchCleanup
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.search_cancel
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import org.jetbrains.compose.resources.stringResource
import utils.SearchStatus

// Search Pager Composable

/**
 * The full-screen search surface.
 *
 * @param surfaceColor The colour the surface paints. It is the *page's* background, not
 *   `colorScheme.surface`: on the device the expanded search keeps the page behind it, while the
 *   ColorOS 17 dark scheme's `surface` is pure black — a full screen of it reads as the page going
 *   black the moment the search bar is tapped.
 */
@Composable
fun SearchStatus.SearchPager(
    onSearchStatusChange: (SearchStatus) -> Unit,
    offsetY: Dp,
    defaultResult: @Composable () -> Unit,
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
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val topPadding by animateDpAsState(
        // The device's expanded search bar keeps the collapsed bar's place — its `search_src_text`
        // sits at y 164..319px in both states, 23px (6dp) below the 141px status bar.
        targetValue = if (searchStatus.shouldExpand()) {
            systemBarsPadding + 6.dp
        } else {
            max(offsetY, 0.dp)
        },
        animationSpec = tween(300, easing = LinearOutSlowInEasing),
        label = "SearchPagerTopPadding",
    ) {
        onSearchStatusChange(searchStatus.onAnimationComplete())
    }
    val surfaceAlpha by animateFloatAsState(
        if (searchStatus.shouldExpand()) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "SearchPagerSurfaceAlpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(5f)
            .drawBehind { drawRect(surfaceColor.copy(alpha = surfaceAlpha)) }
            .then(
                // The a11y click sink only belongs to the surface while it is up. Collapsed, the
                // surface is invisible and already lets input through, and claiming a click here
                // leaves a full-screen node behind that hides the page's own semantics tree from
                // accessibility services.
                if (!searchStatus.isCollapsed()) {
                    Modifier
                        .semantics { onClick { false } }
                        .pointerInput(Unit) { }
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val topPaddingPx = topPadding.roundToPx()
                    val placeable = measurable.measure(constraints.offset(vertical = -topPaddingPx))
                    layout(placeable.width, placeable.height + topPaddingPx) {
                        placeable.placeRelative(0, topPaddingPx)
                    }
                }
                .then(
                    if (!searchStatus.isCollapsed()) {
                        Modifier.background(surfaceColor)
                    } else {
                        Modifier
                    },
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!searchStatus.isCollapsed()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(surfaceColor),
                ) {
                    expandBar(searchStatus, onSearchStatusChange)
                }
            }
            AnimatedVisibility(
                visible = searchStatus.isExpand() || searchStatus.isAnimatingExpand(),
                enter = expandHorizontally() + slideInHorizontally(initialOffsetX = { it }),
                exit = shrinkHorizontally() + slideOutHorizontally(targetOffsetX = { it }),
            ) {
                Text(
                    text = stringResource(Res.string.search_cancel),
                    fontWeight = FontWeight.Bold,
                    color = COUITheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 16.dp, bottom = 6.dp)
                        .clickable(
                            interactionSource = null,
                            enabled = searchStatus.isExpand(),
                            indication = null,
                            onClick = onCancelSearch,
                        ),
                )
                val navEventState = rememberNavigationEventState(NavigationEventInfo.None)
                NavigationBackHandler(
                    state = navEventState,
                    isBackEnabled = true,
                    onBackCompleted = {
                        onSearchStatusChange(
                            searchStatus.copy(
                                searchText = "",
                                current = SearchStatus.Status.COLLAPSING,
                            ),
                        )
                    },
                )
            }
        }
        AnimatedVisibility(
            visible = searchStatus.isExpand(),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
            enter = fadeIn(),
            exit = fadeOut(),
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
    }
}

@Composable
fun SearchBar(
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val onSearchStatusChangeUpdated = rememberUpdatedState(onSearchStatusChange)
    val searchStatusUpdated = rememberUpdatedState(searchStatus)
    val onClearSearch = remember {
        { onSearchStatusChangeUpdated.value(searchStatusUpdated.value.copy(searchText = "")) }
    }

    // The capsule belongs to the row rather than to the field. The device keeps the magnifier as a
    // sibling of `search_src_text`, so the field's own node starts at x=216px — after the 40dp icon
    // slot — instead of stretching back over the glyph.
    val colors = SearchBarDefaults.searchBarColors()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 6.dp),
    ) {
        // The expanded field keeps the shadow the device draws as soon as the search page is open.
        // The fill is translucent at night, so the shadow sits on its own layer with the capsule
        // punched out — drawn behind the fill it would darken the field itself.
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    val radius = size.height / 2f
                    val capsule = Path().apply {
                        addRoundRect(RoundRect(0f, 0f, size.width, size.height, radius, radius))
                    }
                    clipPath(capsule, ClipOp.Difference) { this@drawWithContent.drawContent() }
                }
                .dropShadow(shape = CircleShape, shadow = searchBarShadow()),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = colors.backgroundColor, shape = CircleShape)
                .materialShadowEdge(stroke = SearchBarDefaults.searchBarStroke()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The device's leading slot is 40dp, 36dp of it the icon view; the glyph reads 17.5dp.
            // Same numbers as the collapsed home pill.
            Icon(
                imageVector = COUIIcons.Basic.Search,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .padding(start = 10.dp, end = 7.dp),
                tint = COUITheme.colorScheme.onSurfaceContainerHigh,
            )
            InputField(
                query = searchStatus.searchText,
                onQueryChange = { onSearchStatusChange(searchStatus.copy(searchText = it)) },
                label = searchStatus.label,
                leadingIcon = {},
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
                colors = colors.copy(backgroundColor = Color.Transparent),
                // The row paints the capsule and its material edge; a field-level edge would trace
                // only the input slot instead of the whole capsule.
                stroke = null,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                onSearch = {},
                expanded = searchStatus.shouldExpand(),
                onExpandedChange = {
                    onSearchStatusChange(
                        searchStatus.copy(
                            current = if (it) SearchStatus.Status.EXPANDED else SearchStatus.Status.COLLAPSED,
                        ),
                    )
                },
            )
        }
    }
    LaunchedEffect(Unit) {
        if (!expanded && searchStatus.shouldExpand()) {
            focusRequester.requestFocus()
            expanded = true
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
