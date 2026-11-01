// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.anim.COUIMoveEase
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.LocalOverScrollState
import io.github.suqi8.coui.kmp.utils.OverScrollState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A container that supports the "pull-to-refresh" gesture.
 *
 * This composable follows a hoisted state pattern, where the logical `isRefreshing` state
 * is managed by the caller (e.g., a ViewModel). It coordinates nested scrolling to enable a
 * pull-to-refresh action, displays a customizable indicator, and triggers a callback when a
 * refresh is requested.
 *
 * @param isRefreshing A boolean state representing whether a refresh is currently in progress.
 * This state should be hoisted and is the source of truth for the refresh operation.
 * @param onRefresh A lambda to be invoked when a refresh is triggered by the user. This lambda
 * should initiate the data loading and is responsible for eventually setting `isRefreshing`
 * back to `false` upon completion.
 * @param modifier The modifier to be applied to this container.
 * @param pullToRefreshState The state object that manages the UI and animations of the indicator.
 * See [rememberPullToRefreshState].
 * @param contentPadding The padding to be applied to the content. The top padding is used to
 * correctly offset the refresh indicator.
 * @param topAppBarScrollBehavior An optional [ScrollBehavior] for a `TopAppBar` to coordinate
 * scrolling between the app bar and the pull-to-refresh gesture.
 * @param color The color of the refresh indicator. If [Color.Unspecified], the theme
 * onSurface color at 85% opacity is used.
 * @param circleSize The size of the refresh indicator. The refresh threshold is twice this size.
 * @param refreshTexts The text shown below the indicator for the pulling, threshold reached,
 * refreshing and complete states. ColorOS list refresh shows no text, so this defaults to empty.
 * @param refreshTextStyle The [TextStyle] for the refresh indicator text.
 * @param headerBottomGap The empty band kept between the indicator block and the top of the
 * content, so the indicator never sits flush against the list.
 * @param content The content to be displayed inside the container.
 */
@Composable
fun PullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    pullToRefreshState: PullToRefreshState = rememberPullToRefreshState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    topAppBarScrollBehavior: ScrollBehavior? = null,
    color: Color = PullToRefreshDefaults.color,
    circleSize: Dp = PullToRefreshDefaults.circleSize,
    refreshTexts: List<String> = PullToRefreshDefaults.refreshTexts,
    refreshTextStyle: TextStyle = PullToRefreshDefaults.refreshTextStyle,
    headerBottomGap: Dp = PullToRefreshDefaults.HeaderBottomGap,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val overScrollState = LocalOverScrollState.current
    val currentOnRefresh by rememberUpdatedState(onRefresh)

    // pull_down_loading.json strokes black at 85% opacity, its night copy white at 85%.
    val indicatorColor = color.takeOrElse { COUITheme.colorScheme.onSurface.copy(alpha = 0.85f) }

    // DefaultHeader: dragDistanceThreshold = 2 * loadingViewHeight.
    val density = LocalDensity.current
    pullToRefreshState.indicatorHeightPx = with(density) { circleSize.toPx() }
    pullToRefreshState.refreshThresholdOffset = pullToRefreshState.indicatorHeightPx * 2f

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && pullToRefreshState.refreshState == RefreshState.Refreshing) {
            // Data loading completed, end refresh
            pullToRefreshState.finishRefreshing()
        }
    }

    // This connection establishes the chain of responsibility for nested scroll events.
    val nestedScrollConnection =
        remember(pullToRefreshState, topAppBarScrollBehavior, overScrollState) {
            // Reset cached connection when dependencies change
            pullToRefreshState.cachedNestedScrollConnection = null
            createPullToRefreshConnection(
                pullToRefreshState,
                topAppBarScrollBehavior,
                overScrollState,
            )
        }

    // A modifier to detect when the user releases their finger and trigger the refresh logic.
    val pointerModifier = remember(pullToRefreshState) {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (
                        (pullToRefreshState.refreshState == RefreshState.Pulling || pullToRefreshState.refreshState == RefreshState.ThresholdReached) &&
                        event.changes.all { !it.pressed }
                    ) {
                        coroutineScope.launch {
                            pullToRefreshState.handlePointerRelease(currentOnRefresh)
                        }
                    }
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalPullToRefreshState provides pullToRefreshState,
    ) {
        val boxModifier = remember(modifier, nestedScrollConnection, pointerModifier) {
            modifier
                .onSizeChanged { pullToRefreshState.maxDragDistancePx = it.height.toFloat() }
                .nestedScroll(nestedScrollConnection)
                .then(pointerModifier)
        }

        Box(modifier = boxModifier) {
            Column {
                RefreshHeader(
                    pullToRefreshState = pullToRefreshState,
                    circleSize = circleSize,
                    color = indicatorColor,
                    refreshTexts = refreshTexts,
                    refreshTextStyle = refreshTextStyle,
                    bottomGap = headerBottomGap,
                    modifier = Modifier.offset(y = contentPadding.calculateTopPadding()),
                )
                content()
            }
        }
    }
}

/**
 * Creates and remembers a [PullToRefreshState] across recompositions.
 *
 * This state object is responsible for managing the visual aspects of the refresh indicator,
 * such as its position and animation. The logical `isRefreshing` state should be hoisted and
 * managed separately.
 *
 * @return A remembered instance of [PullToRefreshState].
 */
@Composable
fun rememberPullToRefreshState(): PullToRefreshState {
    val coroutineScope = rememberCoroutineScope()

    // The state object is created using `remember` as it's a runtime UI state manager.
    // The logical `isRefreshing` state, which survives process death, is hoisted.
    return remember {
        PullToRefreshState(coroutineScope)
    }
}

/** Represents the various visual states of the pull-to-refresh indicator. */
sealed interface RefreshState {
    data object Idle : RefreshState
    data object Pulling : RefreshState
    data object ThresholdReached : RefreshState
    data object Refreshing : RefreshState
    data object RefreshComplete : RefreshState
}

/**
 * A UI state holder for the [PullToRefresh] composable.
 *
 * This class manages the internal state machine for animations and nested scroll interactions,
 * driven by the hoisted `isRefreshing` boolean.
 *
 * @param coroutineScope A [CoroutineScope] used to launch animations and state updates.
 */
class PullToRefreshState(
    internal var coroutineScope: CoroutineScope,
) {
    internal var maxDragDistancePx: Float = 0f
    internal var refreshThresholdOffset: Float = 0f
    internal var indicatorHeightPx: Float = 0f

    /** The indicator's rotation when the refresh started, kept for as long as it spins. */
    internal var heldIndicatorRotation: Float = 0f

    /** The drag offset in pixels. */
    var dragOffset by mutableFloatStateOf(0f)

    /** Cached NestedScrollConnection to avoid creating new instances on every scroll event. */
    var cachedNestedScrollConnection: NestedScrollConnection? = null

    private var internalRefreshState by mutableStateOf<RefreshState>(RefreshState.Idle)

    /** The current visual [RefreshState] of the component. */
    val refreshState: RefreshState get() = internalRefreshState

    /** The progress of the pull gesture, from 0.0 to 1.0, until the threshold is reached. */
    val pullProgress: Float by derivedStateOf {
        if (refreshThresholdOffset > 0f) {
            (dragOffset / refreshThresholdOffset).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    internal var isRefreshing by mutableStateOf(false)
    internal var isTouching by mutableStateOf(false)
    internal var isRebounding by mutableStateOf(false)
    private var isProcessingRelease = false
    internal var animationJob: Job? = null

    /**
     * Scrolls [dragOffset] to [targetValue] the way `PullLayout` drives its Scroller: 500 ms on the
     * COUI move ease, whatever the distance. Cancelling the job hands the offset back to the finger.
     */
    internal suspend fun animateOffsetTo(targetValue: Float) {
        animationJob?.cancel()
        val currentJob = currentCoroutineContext()[Job]
        animationJob = currentJob
        try {
            Animatable(dragOffset).animateTo(
                targetValue = targetValue,
                animationSpec = tween(REFRESH_SCROLL_MILLIS, easing = COUIMoveEase),
            ) {
                dragOffset = value
            }
        } finally {
            if (animationJob == currentJob) animationJob = null
        }
    }

    /**
     * Starts the pull-to-refresh animation without a gesture: the indicator scrolls down to the
     * threshold and then runs [onRefresh]. Lets shortcuts such as "back to top" show the same
     * indicator the user would get by pulling.
     */
    fun startRefresh(onRefresh: () -> Unit) {
        coroutineScope.launch { startRefreshing(onRefresh) }
    }

    /**
     * Enter the refresh queue
     */
    internal suspend fun startRefreshing(onRefresh: () -> Unit) {
        if (!isRefreshing) {
            // DefaultHeader stops turning the view on release and plays the animation from there.
            heldIndicatorRotation = indicatorRotation()
            isRefreshing = true
            try {
                // Enter the pull state first so a refresh without a gesture reads as one in progress.
                if (!isTouching) internalRefreshState = RefreshState.Pulling
                animateOffsetTo(refreshThresholdOffset)
            } finally {
                if (!isTouching) {
                    internalRefreshState = RefreshState.Refreshing
                    onRefresh()
                } else {
                    isRefreshing = false
                }
            }
        }
    }

    /**
     * Called when the hoisted `isRefreshing` state becomes false.
     * Triggers the completion animation and resets the state.
     */
    internal suspend fun finishRefreshing() {
        if (isRefreshing) {
            isRefreshing = false
            internalRefreshState = RefreshState.RefreshComplete
            animateOffsetTo(0f)
            internalRefreshState = RefreshState.Idle
        }
    }

    /** Handles the pointer release event to either trigger a refresh or rebound the indicator. */
    internal suspend fun handlePointerRelease(onRefresh: () -> Unit) {
        isTouching = false

        if (isProcessingRelease || isRefreshing || isRebounding) return
        isProcessingRelease = true
        try {
            if (dragOffset >= refreshThresholdOffset) {
                // If pulled past threshold, will then call startRefreshing().
                startRefreshing(onRefresh)
            } else {
                // If not pulled past threshold, rebound to the resting state.
                try {
                    isRebounding = true
                    animateOffsetTo(0f)
                } finally {
                    isRebounding = false
                    resetToIdle()
                }
            }
        } finally {
            isProcessingRelease = false
        }
    }

    /** The rotation `DefaultHeader` gives the indicator at the current [dragOffset]. */
    internal fun indicatorRotation(): Float = pullToRefreshIndicatorRotation(
        offset = dragOffset,
        begin = indicatorHeightPx,
        threshold = refreshThresholdOffset,
    )

    /** Resets the refresh state to [RefreshState.Idle]. */
    internal fun resetToIdle() {
        if (internalRefreshState != RefreshState.Refreshing && internalRefreshState != RefreshState.RefreshComplete) {
            internalRefreshState = RefreshState.Idle
        }
    }

    /** Creates a [NestedScrollConnection] for the pull-to-refresh logic itself. */
    internal fun getOrCreateNestedScrollConnection(
        overScrollState: OverScrollState,
    ): NestedScrollConnection {
        // Return cached instance if already created to avoid allocations during scrolling
        cachedNestedScrollConnection?.let { return it }

        return (
            object : NestedScrollConnection {
                private fun applyDrag(delta: Float) {
                    if (delta == 0f) return
                    dragOffset = pullToRefreshDragStep(dragOffset, delta, maxDragDistancePx)

                    when {
                        refreshThresholdOffset > 0f && dragOffset >= refreshThresholdOffset -> RefreshState.ThresholdReached
                        dragOffset > 0 -> RefreshState.Pulling
                        else -> RefreshState.Idle
                    }.also { nextState ->
                        if (refreshState != nextState) {
                            internalRefreshState = nextState
                        }
                    }
                }

                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    // Only defer to overscroll when refresh is idle.
                    if (overScrollState.isOverScrollActive && refreshState == RefreshState.Idle) return Offset.Zero
                    // If the refresh is in progress, consume all scroll events.
                    if (refreshState == RefreshState.Refreshing || refreshState == RefreshState.RefreshComplete) {
                        return available
                    }

                    // When pulling up while the indicator is visible, consume the scroll to hide it.
                    if (source == NestedScrollSource.UserInput && available.y < 0 && dragOffset > 0f) {
                        isTouching = true
                        animationJob?.cancel()
                        applyDrag(available.y)
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    // If the refresh is in progress, consume all scroll events.
                    if (refreshState == RefreshState.Refreshing || refreshState == RefreshState.RefreshComplete) {
                        return available
                    }

                    // When pulling down after the content is at its top, consume the scroll to show the indicator.
                    if (source == NestedScrollSource.UserInput && available.y > 0f) {
                        isTouching = true
                        animationJob?.cancel()
                        applyDrag(available.y)
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }
            }
            ).also { cachedNestedScrollConnection = it }
    }
}

/**
 * A factory function to create the main [NestedScrollConnection] for the [PullToRefresh] component.
 */
private fun createPullToRefreshConnection(
    pullToRefreshState: PullToRefreshState,
    topAppBarScrollBehavior: ScrollBehavior?,
    overScrollState: OverScrollState,
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        when (pullToRefreshState.refreshState) {
            RefreshState.Idle -> {
                val consumedByAppBar = topAppBarScrollBehavior?.nestedScrollConnection?.onPreScroll(available, source) ?: Offset.Zero
                val remaining = available - consumedByAppBar
                val consumedByRefresh = pullToRefreshState
                    .getOrCreateNestedScrollConnection(overScrollState)
                    .onPreScroll(remaining, source)
                return consumedByAppBar + consumedByRefresh
            }

            RefreshState.RefreshComplete, RefreshState.Refreshing -> {
                return available
            }

            else -> {
                val consumedByRefresh = pullToRefreshState.getOrCreateNestedScrollConnection(overScrollState).onPreScroll(available, source)
                val remaining = available - consumedByRefresh
                val consumedByAppBar = topAppBarScrollBehavior?.nestedScrollConnection?.onPreScroll(remaining, source) ?: Offset.Zero
                return consumedByRefresh + consumedByAppBar
            }
        }
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        when (pullToRefreshState.refreshState) {
            RefreshState.RefreshComplete, RefreshState.Refreshing -> {
                return available
            }

            else -> {
                val consumedByAppBar =
                    topAppBarScrollBehavior?.nestedScrollConnection?.onPostScroll(consumed, available, source) ?: Offset.Zero
                val remaining = available - consumedByAppBar
                val consumedByRefresh = pullToRefreshState
                    .getOrCreateNestedScrollConnection(overScrollState)
                    .onPostScroll(consumed, remaining, source)
                return consumedByAppBar + consumedByRefresh
            }
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (pullToRefreshState.refreshState != RefreshState.Idle) {
            return available
        }
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (pullToRefreshState.refreshState != RefreshState.Idle) {
            // Ensure the indicator cancels and rebounds to zero.
            if (
                pullToRefreshState.refreshState != RefreshState.Refreshing &&
                pullToRefreshState.refreshState != RefreshState.RefreshComplete &&
                !pullToRefreshState.isRefreshing &&
                !pullToRefreshState.isRebounding &&
                pullToRefreshState.dragOffset > 0f &&
                pullToRefreshState.dragOffset < pullToRefreshState.refreshThresholdOffset
            ) {
                try {
                    pullToRefreshState.isRebounding = true
                    pullToRefreshState.animateOffsetTo(0f)
                } finally {
                    pullToRefreshState.isRebounding = false
                    pullToRefreshState.resetToIdle()
                }
            }
            return available
        }
        return topAppBarScrollBehavior?.nestedScrollConnection?.onPostFling(consumed, available) ?: Velocity.Zero
    }
}

/**
 * The header sits on the top edge of the content and travels with it: `PullLayout` scrolls both, so
 * the indicator is clipped until the offset has revealed all of it.
 */
@Composable
private fun RefreshHeader(
    pullToRefreshState: PullToRefreshState,
    circleSize: Dp,
    color: Color,
    refreshTexts: List<String>,
    refreshTextStyle: TextStyle,
    bottomGap: Dp,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(pullToRefreshState.refreshState) {
        if (pullToRefreshState.refreshState == RefreshState.ThresholdReached) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
    }

    val refreshText by remember(pullToRefreshState, refreshTexts) {
        derivedStateOf {
            when (pullToRefreshState.refreshState) {
                RefreshState.Idle -> ""
                RefreshState.Pulling -> refreshTexts.getOrElse(0) { "" }
                RefreshState.ThresholdReached -> refreshTexts.getOrElse(1) { "" }
                RefreshState.Refreshing -> refreshTexts.getOrElse(2) { "" }
                RefreshState.RefreshComplete -> refreshTexts.getOrElse(3) { "" }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity),
                )
                val height = pullToRefreshState.dragOffset.roundToInt().coerceAtLeast(0)
                layout(placeable.width, height) {
                    placeable.place(0, height - placeable.height)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RefreshIndicator(
            pullToRefreshState = pullToRefreshState,
            circleSize = circleSize,
            color = color,
        )
        if (refreshTexts.isNotEmpty()) {
            Text(
                text = refreshText,
                style = refreshTextStyle,
                color = color,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(bottomGap))
    }
}

@Composable
private fun RefreshIndicator(
    pullToRefreshState: PullToRefreshState,
    circleSize: Dp,
    color: Color,
) {
    val spinning = pullToRefreshState.isRefreshing ||
        pullToRefreshState.refreshState == RefreshState.RefreshComplete
    val cycle = remember { Animatable(0f) }

    LaunchedEffect(spinning) {
        if (!spinning) {
            cycle.snapTo(0f)
            return@LaunchedEffect
        }
        delay(SPINNER_START_DELAY_MILLIS)
        while (true) {
            cycle.animateTo(1f, tween(RotatingSpinnerCycleMillis, easing = LinearEasing))
            cycle.snapTo(0f)
        }
    }

    Canvas(modifier = Modifier.size(circleSize)) {
        val rotation = if (spinning) {
            pullToRefreshState.heldIndicatorRotation
        } else {
            pullToRefreshState.indicatorRotation()
        }
        // The layer's `s: [-100, -100, 100]` is half a turn. A running spinner can ignore that
        // phase, but this one rests on frame 0, so the gap has to land where the asset puts it.
        rotate(rotation + 180f) {
            drawRotatingSpinner(
                cycle = cycle.value,
                color = color,
                diameterPx = size.minDimension * SPINNER_RING_RATIO,
                strokeWidthPx = size.minDimension * SPINNER_STROKE_RATIO,
            )
        }
    }
}

/**
 * One drag event: `PullLayout` divides [delta] by `2.5 / (1 - offset / height)`, so the page resists
 * harder the further it has travelled, and clamps the result to the container.
 */
internal fun pullToRefreshDragStep(offset: Float, delta: Float, height: Float): Float {
    if (height <= 0f) return offset
    val resistance = 1f - (offset / height).coerceIn(0f, 1f)
    return (offset + delta * resistance / REFRESH_DAMPING).coerceIn(0f, height)
}

/**
 * The indicator's rotation at [offset], from `DefaultHeader`: it rests at 270 degrees until it is
 * fully revealed at [begin], turns a quarter to [threshold], then keeps turning with the finger.
 */
internal fun pullToRefreshIndicatorRotation(offset: Float, begin: Float, threshold: Float): Float = when {
    offset >= threshold -> (offset - threshold) / 2f
    offset > begin -> (offset - begin) / (threshold - begin) * 90f + 270f
    else -> 270f
}

/** `PullLayout.dampingCoefficient`. */
private const val REFRESH_DAMPING = 2.5f

/** The duration of every `PullLayout` Scroller run. */
private const val REFRESH_SCROLL_MILLIS = 500

/** `DefaultHeader` posts its loading start this long after the finger lifts. */
private const val SPINNER_START_DELAY_MILLIS = 100L

/** pull_down_loading.json: 26 units at 300% group scale on an 84-unit comp. */
private const val SPINNER_RING_RATIO = 78f / 84f

/** pull_down_loading.json: a 2-unit stroke at 300% group scale on an 84-unit comp. */
private const val SPINNER_STROKE_RATIO = 6f / 84f

internal val LocalPullToRefreshState = staticCompositionLocalOf<PullToRefreshState?> { null }

/** Default values for the [PullToRefresh] component. */
object PullToRefreshDefaults {
    /**
     * The default color of the refresh indicator. [Color.Unspecified] makes [PullToRefresh] fall
     * back to the theme onSurface color at 85% opacity, the stroke of `pull_down_loading.json`.
     */
    val color: Color = Color.Unspecified

    /** The default size of the refresh indicator (`default_header_loading_height`). */
    val circleSize: Dp = 26.dp

    /** ColorOS list refresh shows only the indicator. */
    val refreshTexts: List<String> = emptyList()

    /**
     * The empty band kept between the indicator block and the top of the content, so the
     * indicator never sits flush against the list.
     */
    val HeaderBottomGap: Dp = 8.dp

    val refreshTextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}
