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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.SearchBar
import io.github.suqi8.coui.kmp.basic.SearchBarDefaults
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TopAppBarSearchBar
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Messages
import io.github.suqi8.coui.kmp.icon.extended.Music
import io.github.suqi8.coui.kmp.icon.extended.Photos
import io.github.suqi8.coui.kmp.icon.extended.Settings
import io.github.suqi8.coui.kmp.icon.extended.Theme
import io.github.suqi8.coui.kmp.icon.extended.Timer
import io.github.suqi8.coui.kmp.icon.extended.TopDownloads
import io.github.suqi8.coui.kmp.icon.extended.Unlock
import io.github.suqi8.coui.kmp.icon.extended.VolumeUp
import io.github.suqi8.coui.kmp.preference.SwitchPreference
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.search_cancel
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import org.jetbrains.compose.resources.stringResource
import utils.SearchStatus
import utils.rememberBlurBackdrop

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

private enum class SearchSampleMode { Persistent, Toolbar }

private val TransferIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Transfer",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(
                "M8,11V21.3C8,21.686 8.313,22 8.7,22H9.3C9.687,22 10,21.686 10,21.3V2.637C10,2.049 9.242,1.812 8.907,2.295L3.628,9.901C3.305,10.365 3.638,11 4.203,11L8,11Z",
            ).toNodes(),
            fill = SolidColor(Color.Black),
        )
        addPath(
            pathData = PathParser().parsePathString(
                "M16,13V2.7C16,2.313 15.687,2 15.3,2H14.7C14.313,2 14,2.313 14,2.7V21.363C14,21.95 14.758,22.188 15.093,21.705L20.372,14.099C20.694,13.635 20.362,13 19.797,13L16,13Z",
            ).toNodes(),
            fill = SolidColor(Color.Black),
        )
    }.build()
}

private data class SearchPreviewEntry(
    val title: String,
    val summary: String,
    val value: String,
    val colorHex: Long,
    val icon: ImageVector,
)

private val SearchPreviewEntries = listOf(
    SearchPreviewEntry("Wireless network", "Connected to COUI Demo", "Wi-Fi 6", 0xFF1A8CFF, TransferIcon),
    SearchPreviewEntry("Bluetooth headphones", "Simulated device · Stereo audio", "Connected", 0xFF6366F1, COUIIcons.VolumeUp),
    SearchPreviewEntry("Notification messages", "", "3 items", 0xFFFF9B35, COUIIcons.Messages),
    SearchPreviewEntry("Battery status", "Sufficient battery, ready to use", "72%", 0xFF34B978, COUIIcons.Settings),
    SearchPreviewEntry("Calendar schedule", "3:00 PM · Project discussion", "Today", 0xFFF0667B, COUIIcons.Timer),
    SearchPreviewEntry("Music playback", "Afternoon radio · Light music", "Paused", 0xFFA884E7, COUIIcons.Music),
    SearchPreviewEntry("Photo album", "", "128 photos", 0xFF45B6AE, COUIIcons.Photos),
    SearchPreviewEntry("Download task", "sample.zip · 24 MB", "Completed", 0xFF1A8CFF, COUIIcons.TopDownloads),
    SearchPreviewEntry("Privacy & security", "Manage app permissions and local access logs", "", 0xFF34B978, COUIIcons.Unlock),
    SearchPreviewEntry("Screen display", "", "Auto", 0xFFFF9B35, COUIIcons.Theme),
)

/**
 * The gallery's [SearchBar] entry: demonstrates both the persistent [SearchBar] (expanding with a
 * split cancel capsule) and the toolbar [TopAppBarSearchBar] (circular search button expanding
 * into a search bar and cancel capsule with metaball fusion and motion blur).
 */
@Composable
fun SearchBarDemo() {
    var enabled by remember { mutableStateOf(true) }
    var active by remember { mutableStateOf<SearchSampleMode?>(null) }
    var query by remember { mutableStateOf("") }
    val persistentExpanded = active == SearchSampleMode.Persistent
    val toolbarExpanded = active == SearchSampleMode.Toolbar
    val persistentEnabled = enabled && active != SearchSampleMode.Toolbar
    val toolbarEnabled = enabled && active != SearchSampleMode.Persistent
    val scrollState = rememberScrollState()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val backdrop = rememberBlurBackdrop()

    val closeSearch = {
        active = null
        query = ""
        keyboard?.hide()
        focusManager.clearFocus()
    }

    LaunchedEffect(query) {
        scrollState.scrollTo(0)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            cornerRadius = CardDefaults.GroupCornerRadius,
            weight = CardDefaults.GroupWeight,
        ) {
            SwitchPreference(
                title = str("Enabled state"),
                checked = enabled,
                onCheckedChange = { enabled = it },
                cardListPosition = CardListPosition.Full,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .clip(RoundedCornerShape(CardDefaults.GroupCornerRadius)),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    .background(COUITheme.colorScheme.background),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(top = 128.dp, bottom = 16.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    SearchPreviewList(query = query)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
            ) {
                SearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = {
                        keyboard?.hide()
                        focusManager.clearFocus()
                    },
                    onExpandedChange = { isExpanded ->
                        if (!isExpanded) {
                            closeSearch()
                        } else if (active == null) {
                            active = SearchSampleMode.Persistent
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    expanded = persistentExpanded,
                    label = str("Search samples"),
                    enabled = persistentEnabled,
                    backdrop = backdrop,
                    insideMargin = DpSize(16.dp, 6.dp),
                    cancelText = str("Cancel"),
                    onCancel = closeSearch,
                    clearContentDescription = str("Clear"),
                )

                TopAppBarSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    expanded = toolbarExpanded,
                    onExpandedChange = { isExpanded ->
                        if (!isExpanded) {
                            closeSearch()
                        } else if (active == null) {
                            active = SearchSampleMode.Toolbar
                        }
                    },
                    title = str("Sample Demo"),
                    placeholder = str("Search samples"),
                    cancelText = str("Cancel"),
                    onSearch = {
                        keyboard?.hide()
                        focusManager.clearFocus()
                    },
                    onCancel = closeSearch,
                    enabled = toolbarEnabled,
                    searchButtonContentDescription = str("Search"),
                    backdrop = backdrop,
                )
            }
        }
    }
}

@Composable
private fun SearchPreviewList(query: String) {
    val needle = query.trim()
    val matches = remember(needle) {
        SearchPreviewEntries.mapIndexedNotNull { index, entry ->
            val localizedTitle = entry.title
            val localizedSummary = entry.summary
            val localizedValue = entry.value
            index.takeIf {
                needle.isEmpty() ||
                    localizedTitle.contains(needle, ignoreCase = true) ||
                    localizedSummary.contains(needle, ignoreCase = true) ||
                    localizedValue.contains(needle, ignoreCase = true)
            }
        }
    }
    val localizedMatches = SearchPreviewEntries.indices.filter { index ->
        val entry = SearchPreviewEntries[index]
        val titleText = str(entry.title)
        val summaryText = if (entry.summary.isNotEmpty()) str(entry.summary) else ""
        val valueText = if (entry.value.isNotEmpty()) str(entry.value) else ""
        needle.isEmpty() ||
            titleText.contains(needle, ignoreCase = true) ||
            summaryText.contains(needle, ignoreCase = true) ||
            valueText.contains(needle, ignoreCase = true) ||
            index in matches
    }

    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (localizedMatches.isEmpty()) {
            BasicComponent(
                title = str("No matching items"),
                cardListPosition = CardListPosition.Full,
            )
        } else {
            val last = localizedMatches.lastIndex
            localizedMatches.forEachIndexed { position, index ->
                key(index) {
                    val entry = SearchPreviewEntries[index]
                    val accent = Color(entry.colorHex)
                    val summaryText = entry.summary.takeIf { it.isNotEmpty() }?.let { str(it) }
                    val valueText = entry.value.takeIf { it.isNotEmpty() }?.let { str(it) }
                    BasicComponent(
                        modifier = Modifier.heightIn(min = if (summaryText == null) 56.dp else 72.dp),
                        title = str(entry.title),
                        summary = summaryText,
                        startAction = {
                            Box(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(32.dp)
                                    .background(
                                        color = accent.copy(alpha = 0.16f),
                                        shape = if (index % 2 == 0) CircleShape else RoundedCornerShape(8.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = entry.icon,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        endActions = {
                            if (valueText != null) {
                                Text(
                                    text = valueText,
                                    style = COUITheme.textStyles.body2,
                                    color = COUITheme.colorScheme.onSurfaceSecondary,
                                )
                            }
                        },
                        cardListPosition = when {
                            last == 0 -> CardListPosition.Full
                            position == 0 -> CardListPosition.Head
                            position == last -> CardListPosition.Tail
                            else -> CardListPosition.Middle
                        },
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
