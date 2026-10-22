// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.LocalOverScrollState
import io.github.suqi8.coui.kmp.utils.OverScrollState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A full-page pull-to-refresh container modelled after the "Featured" tab of the OPPO App Market.
 *
 * Instead of growing a small indicator above the content, the whole page follows the finger while
 * a full-screen refresh layer is revealed underneath it. The refresh hint is anchored to the top
 * edge and slides down with the page, clipped so that it emerges line by line as the pull grows.
 *
 * The gesture uses a linear 0.5 damping factor: a 200px finger travel moves the page by 100px.
 * Releasing past [refreshThreshold] commits the refresh; releasing earlier plays the same
 * decelerating rebound as the original implementation. When the hoisted [isRefreshing] flag flips
 * back to `false`, the success hint stays on screen for [completeHoldDurationMillis] before the
 * page returns to its resting position. The return trip runs on its own timing — 45% of the
 * downward settle, capped at 220ms — so the page snaps back instead of gliding up.
 *
 * The content must be scrollable and should consume [contentPadding] at its top. That padding is
 * also used as the immersive height: the visible band of the hint starts at the top padding line,
 * exactly like the market page where the hint appears below the status bar.
 *
 * @param isRefreshing The hoisted refresh state. Set it to `true` in [onRefresh] and back to
 * `false` when loading finishes.
 * @param onRefresh Called after a gesture-triggered pull settles at the threshold.
 * @param modifier The modifier to be applied to the container.
 * @param state The visual state holder, see [rememberFullPagePullToRefreshState].
 * @param contentPadding Padding applied by the caller to the scrollable content. Its top value is
 * used as the immersive height for the hint.
 * @param topAppBarScrollBehavior An optional [ScrollBehavior] coordinated with the pull gesture.
 * @param hapticFeedbackEnabled Whether to play a haptic tick when the pull crosses the threshold.
 * @param completeHoldDurationMillis How long the success hint stays visible before rebounding.
 * @param colors The background, hint and spinner colors.
 * @param refreshThreshold The pull distance that triggers a refresh.
 * @param textStyle The style of the hint text. Its color is overridden by [colors].
 * @param refreshTexts The four hint strings used by the different states.
 * @param background An optional full-screen background drawn behind the content, for example a
 * promotional image. It receives the state so callers can fade or parallax it with the pull.
 * @param content The scrollable page content.
 */
@Composable
fun FullPagePullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: FullPagePullToRefreshState = rememberFullPagePullToRefreshState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    topAppBarScrollBehavior: ScrollBehavior? = null,
    hapticFeedbackEnabled: Boolean = true,
    completeHoldDurationMillis: Long = FullPagePullToRefreshDefaults.COMPLETE_HOLD_DURATION_MILLIS,
    colors: FullPagePullToRefreshColors = fullPagePullToRefreshColors(),
    refreshThreshold: Dp = FullPagePullToRefreshDefaults.refreshThreshold,
    textStyle: TextStyle = FullPagePullToRefreshDefaults.textStyle,
    refreshTexts: FullPagePullToRefreshTexts = FullPagePullToRefreshDefaults.refreshTexts,
    background: (@Composable BoxScope.(FullPagePullToRefreshState) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val hapticFeedback = LocalHapticFeedback.current
    val overScrollState = LocalOverScrollState.current
    val currentOnRefresh by rememberUpdatedState(onRefresh)

    val thresholdPx = with(density) { refreshThreshold.toPx() }
    val immersivePx = with(density) { contentPadding.calculateTopPadding().toPx() }
    val windowHeightPx = windowInfo.containerSize.height.toFloat()

    state.refreshThresholdPx = thresholdPx
    state.maxDragPx = windowHeightPx.takeIf { it > 0f } ?: (thresholdPx * 4f)
    state.completeHoldDurationMillis = completeHoldDurationMillis

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            state.onRefreshingFlagRaised()
        } else {
            state.onRefreshingFlagLowered()
        }
    }

    LaunchedEffect(state.refreshState) {
        if (state.refreshState == FullPageRefreshState.ThresholdReached && hapticFeedbackEnabled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
    }

    val nestedScrollConnection =
        remember(state, topAppBarScrollBehavior, overScrollState) {
            createFullPagePullToRefreshConnection(
                state = state,
                topAppBarScrollBehavior = topAppBarScrollBehavior,
                overScrollState = overScrollState,
                onRefresh = { currentOnRefresh() },
            )
        }

    val pointerModifier = remember(state) {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.any { it.pressed }) {
                        state.onTouchStart()
                    } else if (event.changes.isNotEmpty() && event.changes.all { !it.pressed }) {
                        state.onTouchRelease { currentOnRefresh() }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .nestedScroll(nestedScrollConnection)
            .then(pointerModifier),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(colors.backgroundColor),
        ) {
            if (background != null) {
                background(state)
            }
        }

        RefreshHint(
            state = state,
            immersivePx = immersivePx,
            spinnerSize = FullPagePullToRefreshDefaults.spinnerSize,
            spinnerColor = colors.indicatorColor,
            textColor = colors.textColor,
            textStyle = textStyle,
            refreshTexts = refreshTexts,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, state.dragOffset.roundToInt()) },
        ) {
            content()
        }
    }
}

/**
 * Creates and remembers a [FullPagePullToRefreshState] across recompositions.
 */
@Composable
fun rememberFullPagePullToRefreshState(): FullPagePullToRefreshState {
    val coroutineScope = rememberCoroutineScope()
    return remember(coroutineScope) {
        FullPagePullToRefreshState(coroutineScope)
    }
}

/** The visual states of the full-page pull-to-refresh gesture. */
sealed interface FullPageRefreshState {
    data object Idle : FullPageRefreshState
    data object Pulling : FullPageRefreshState
    data object ThresholdReached : FullPageRefreshState
    data object Refreshing : FullPageRefreshState
    data object RefreshComplete : FullPageRefreshState
}

/**
 * The state holder for [FullPagePullToRefresh].
 *
 * It is intentionally small: the hoisted `isRefreshing` flag remains the source of truth for the
 * refresh operation, while this class tracks the drag distance and the visual state machine.
 *
 * @param coroutineScope The scope used for release, rebound and completion animations.
 */
@Stable
class FullPagePullToRefreshState internal constructor(
    internal val coroutineScope: CoroutineScope,
) {
    internal var refreshThresholdPx by mutableFloatStateOf(0f)
    internal var maxDragPx by mutableFloatStateOf(0f)
    internal var completeHoldDurationMillis: Long =
        FullPagePullToRefreshDefaults.COMPLETE_HOLD_DURATION_MILLIS

    /** The current vertical displacement of the page in pixels. */
    var dragOffset by mutableFloatStateOf(0f)
        private set

    /** The current visual state. */
    var refreshState by mutableStateOf<FullPageRefreshState>(FullPageRefreshState.Idle)
        private set

    /** The pull progress from 0 to 1 relative to the refresh threshold. */
    val pullProgress: Float by derivedStateOf {
        if (refreshThresholdPx > 0f) {
            (dragOffset / refreshThresholdPx).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    internal var isRefreshingInternal = false
    internal var isTouching = false
    private var rawDragPx = 0f
    private var isReleasing = false
    private var animationJob: Job? = null
    internal var completionJob: Job? = null

    /**
     * Starts a refresh without a gesture, animating the page down to the threshold and then
     * invoking [onRefresh]. The caller is still responsible for raising the hoisted flag.
     */
    fun startRefresh(onRefresh: () -> Unit) {
        coroutineScope.launch {
            if (refreshState == FullPageRefreshState.Refreshing ||
                refreshState == FullPageRefreshState.RefreshComplete
            ) {
                return@launch
            }
            completionJob?.cancel()
            completionJob = null
            isRefreshingInternal = true
            refreshState = FullPageRefreshState.Refreshing
            animateDragTo(refreshThresholdPx)
            onRefresh()
        }
    }

    internal fun onTouchStart() {
        if (isTouching) return
        isTouching = true
        when (refreshState) {
            FullPageRefreshState.Pulling, FullPageRefreshState.ThresholdReached -> {
                animationJob?.cancel()
                animationJob = null
                rawDragPx = dragOffset / PULL_DAMPING
            }

            FullPageRefreshState.RefreshComplete -> {
                completionJob?.cancel()
                completionJob = null
                rebound()
            }

            else -> Unit
        }
    }

    internal fun onTouchRelease(onRefresh: () -> Unit) {
        isTouching = false
        when (refreshState) {
            FullPageRefreshState.Pulling, FullPageRefreshState.ThresholdReached -> {
                release(onRefresh)
            }

            else -> Unit
        }
    }

    internal fun onRefreshingFlagRaised() {
        when (refreshState) {
            FullPageRefreshState.Refreshing -> Unit

            FullPageRefreshState.RefreshComplete -> resumeRefreshing()

            else -> {
                completionJob?.cancel()
                completionJob = null
                isRefreshingInternal = true
                refreshState = FullPageRefreshState.Refreshing
                coroutineScope.launch { animateDragTo(refreshThresholdPx) }
            }
        }
    }

    internal fun onRefreshingFlagLowered() {
        if (isRefreshingInternal) {
            startComplete()
        }
    }

    internal fun release(onRefresh: () -> Unit) {
        if (isReleasing) return
        if (refreshState != FullPageRefreshState.Pulling &&
            refreshState != FullPageRefreshState.ThresholdReached
        ) {
            return
        }
        isReleasing = true
        coroutineScope.launch {
            try {
                if (dragOffset >= refreshThresholdPx) {
                    refreshState = FullPageRefreshState.Refreshing
                    animateDragTo(refreshThresholdPx)
                    isRefreshingInternal = true
                    onRefresh()
                } else {
                    animateDragTo(0f)
                    if (refreshState == FullPageRefreshState.Pulling ||
                        refreshState == FullPageRefreshState.ThresholdReached
                    ) {
                        refreshState = FullPageRefreshState.Idle
                    }
                }
            } finally {
                isReleasing = false
            }
        }
    }

    internal fun startComplete() {
        if (refreshState != FullPageRefreshState.Refreshing) {
            isRefreshingInternal = false
            return
        }
        isRefreshingInternal = false
        refreshState = FullPageRefreshState.RefreshComplete
        completionJob?.cancel()
        completionJob = coroutineScope.launch {
            try {
                if (completeHoldDurationMillis > 0L) {
                    delay(completeHoldDurationMillis)
                }
                animateDragTo(0f)
                if (refreshState == FullPageRefreshState.RefreshComplete) {
                    refreshState = FullPageRefreshState.Idle
                }
            } finally {
                if (completionJob == currentCoroutineContext()[Job]) {
                    completionJob = null
                }
            }
        }
    }

    internal fun onPreScroll(availableY: Float): Float {
        if (availableY >= 0f || dragOffset <= 0f) return 0f
        if (refreshState == FullPageRefreshState.Refreshing ||
            refreshState == FullPageRefreshState.RefreshComplete
        ) {
            return availableY
        }
        animationJob?.cancel()
        animationJob = null
        rawDragPx = dragOffset / PULL_DAMPING
        val consumed = (-rawDragPx).coerceAtLeast(availableY)
        applyDrag(consumed)
        return consumed
    }

    internal fun onPostScroll(availableY: Float): Float {
        if (availableY <= 0f) return 0f
        if (refreshState == FullPageRefreshState.Refreshing ||
            refreshState == FullPageRefreshState.RefreshComplete
        ) {
            return availableY
        }
        if (animationJob?.isActive == true) {
            animationJob?.cancel()
            animationJob = null
            rawDragPx = dragOffset / PULL_DAMPING
        }
        applyDrag(availableY)
        return availableY
    }

    internal suspend fun animateDragTo(target: Float) {
        val start = dragOffset
        val distance = abs(target - start)
        if (distance < 0.5f) {
            snapTo(target)
            return
        }
        // The downward settle (pull -> threshold) keeps the reference timing; the rebound back to
        // the resting position runs on its own, shorter scale so releasing feels snappy instead of
        // gliding up. Only the return trip is affected.
        val baseDuration = ((distance / refreshThresholdPx) * 500f).roundToInt()
        val duration = if (target <= 0f) {
            (baseDuration * REBOUND_DURATION_SCALE)
                .roundToInt()
                .coerceIn(0, REBOUND_MAX_DURATION_MILLIS)
        } else {
            baseDuration.coerceIn(0, 500)
        }
        if (duration == 0) {
            snapTo(target)
            return
        }
        animationJob?.cancel()
        val currentJob = currentCoroutineContext()[Job]
        animationJob = currentJob
        try {
            Animatable(start).animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = duration, easing = DecelerateQuadEasing),
            ) {
                this@FullPagePullToRefreshState.snapTo(value)
            }
        } finally {
            if (animationJob === currentJob) {
                animationJob = null
            }
        }
    }

    private fun applyDrag(delta: Float) {
        if (delta == 0f) return
        if (refreshState == FullPageRefreshState.Refreshing ||
            refreshState == FullPageRefreshState.RefreshComplete
        ) {
            return
        }
        val maxRawDrag = if (maxDragPx > 0f) maxDragPx / PULL_DAMPING else Float.MAX_VALUE
        rawDragPx = (rawDragPx + delta).coerceIn(0f, maxRawDrag)
        snapTo(rawDragPx * PULL_DAMPING)
        val nextState = when {
            refreshThresholdPx > 0f && dragOffset >= refreshThresholdPx ->
                FullPageRefreshState.ThresholdReached

            dragOffset > 0f -> FullPageRefreshState.Pulling

            else -> FullPageRefreshState.Idle
        }
        if (refreshState != nextState) {
            refreshState = nextState
        }
    }

    private fun snapTo(value: Float) {
        val clamped = if (maxDragPx > 0f) {
            value.coerceIn(0f, maxDragPx)
        } else {
            value.coerceAtLeast(0f)
        }
        dragOffset = clamped
        rawDragPx = clamped / PULL_DAMPING
    }

    private fun resumeRefreshing() {
        completionJob?.cancel()
        completionJob = null
        isRefreshingInternal = true
        refreshState = FullPageRefreshState.Refreshing
        coroutineScope.launch { animateDragTo(refreshThresholdPx) }
    }

    private fun rebound() {
        coroutineScope.launch {
            animateDragTo(0f)
            if (refreshState == FullPageRefreshState.RefreshComplete) {
                refreshState = FullPageRefreshState.Idle
            }
        }
    }
}

private fun createFullPagePullToRefreshConnection(
    state: FullPagePullToRefreshState,
    topAppBarScrollBehavior: ScrollBehavior?,
    overScrollState: OverScrollState,
    onRefresh: () -> Unit,
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y == 0f) return Offset.Zero
        if (state.refreshState == FullPageRefreshState.Refreshing ||
            state.refreshState == FullPageRefreshState.RefreshComplete
        ) {
            return available
        }
        if (overScrollState.isOverScrollActive && state.refreshState == FullPageRefreshState.Idle) {
            return Offset.Zero
        }

        val dragConsumed = if (source == NestedScrollSource.UserInput) {
            state.onPreScroll(available.y)
        } else {
            0f
        }
        val remaining = Offset(available.x, available.y - dragConsumed)
        val appBarConsumed = topAppBarScrollBehavior
            ?.nestedScrollConnection
            ?.onPreScroll(remaining, source)
            ?: Offset.Zero
        return Offset(0f, dragConsumed) + appBarConsumed
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (available.y == 0f) return Offset.Zero
        if (state.refreshState == FullPageRefreshState.Refreshing ||
            state.refreshState == FullPageRefreshState.RefreshComplete
        ) {
            return available
        }

        val appBarConsumed = topAppBarScrollBehavior
            ?.nestedScrollConnection
            ?.onPostScroll(consumed, available, source)
            ?: Offset.Zero
        val remaining = available - appBarConsumed
        val dragConsumed = if (source == NestedScrollSource.UserInput) {
            state.onPostScroll(remaining.y)
        } else {
            0f
        }
        return appBarConsumed + Offset(0f, dragConsumed)
    }

    override suspend fun onPreFling(available: Velocity): Velocity = if (state.refreshState != FullPageRefreshState.Idle) available else Velocity.Zero

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (state.refreshState != FullPageRefreshState.Idle) {
            if (state.refreshState == FullPageRefreshState.Pulling ||
                state.refreshState == FullPageRefreshState.ThresholdReached
            ) {
                state.release(onRefresh)
            }
            return available
        }
        return topAppBarScrollBehavior
            ?.nestedScrollConnection
            ?.onPostFling(consumed, available)
            ?: Velocity.Zero
    }
}

@Composable
private fun RefreshHint(
    state: FullPagePullToRefreshState,
    immersivePx: Float,
    spinnerSize: Dp,
    spinnerColor: Color,
    textColor: Color,
    textStyle: TextStyle,
    refreshTexts: FullPagePullToRefreshTexts,
    modifier: Modifier = Modifier,
) {
    var hintHeightPx by remember { mutableFloatStateOf(0f) }
    val rotation = when (state.refreshState) {
        FullPageRefreshState.Pulling,
        FullPageRefreshState.ThresholdReached,
        FullPageRefreshState.Refreshing,
        -> animateHintRotation()

        else -> null
    }

    val text = when (state.refreshState) {
        FullPageRefreshState.Idle -> ""
        FullPageRefreshState.Pulling -> refreshTexts.pulling
        FullPageRefreshState.ThresholdReached -> refreshTexts.thresholdReached
        FullPageRefreshState.Refreshing -> refreshTexts.refreshing
        FullPageRefreshState.RefreshComplete -> refreshTexts.complete
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { hintHeightPx = it.height.toFloat() }
            .offset {
                IntOffset(
                    x = 0,
                    y = (immersivePx + state.dragOffset - hintHeightPx).roundToInt(),
                )
            }
            .drawWithContent {
                val pull = state.dragOffset
                val clipTop = (size.height - pull).coerceIn(0f, size.height)
                if (clipTop < size.height) {
                    clipRect(top = clipTop) {
                        this@drawWithContent.drawContent()
                    }
                }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Crossfade(
            targetState = state.refreshState == FullPageRefreshState.RefreshComplete,
            animationSpec = tween(400),
            modifier = Modifier.fillMaxWidth(),
            label = "fullPageRefreshHint",
        ) { complete ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (complete) {
                    Spacer(Modifier.height(spinnerSize))
                } else {
                    RefreshHintSpinner(
                        size = spinnerSize,
                        color = spinnerColor,
                        rotation = rotation?.value ?: 0f,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (complete) refreshTexts.complete else text,
                    style = textStyle.copy(color = textColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RefreshHintSpinner(
    size: Dp,
    color: Color,
    rotation: Float,
) {
    Canvas(modifier = Modifier.size(size)) {
        val strokeWidth = this.size.minDimension / 10f
        drawArc(
            color = color,
            startAngle = -90f + rotation,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = Size(this.size.width - strokeWidth, this.size.height - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun animateHintRotation(): State<Float> {
    val infiniteTransition = rememberInfiniteTransition()
    return infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
}

/**
 * Default values for [FullPagePullToRefresh].
 */
object FullPagePullToRefreshDefaults {
    /**
     * The pull distance used by the OPPO App Market "Featured" page (93.187dp).
     */
    val refreshThreshold: Dp = 93.187.dp

    /** The diameter of the loading arc above the hint text. */
    val spinnerSize: Dp = 26.dp

    /** The hint text style (couiTextAppearanceBody 14sp). */
    val textStyle: TextStyle = TextStyle(fontSize = 14.sp)

    /** How long the success hint stays visible before the page rebounds (800ms). */
    const val COMPLETE_HOLD_DURATION_MILLIS: Long = 800L

    /** The default hint strings. */
    val refreshTexts: FullPagePullToRefreshTexts = FullPagePullToRefreshTexts(
        pulling = "Pull down to refresh",
        thresholdReached = "Release to refresh",
        refreshing = "Refreshing...",
        complete = "Refreshed successfully",
    )
}

/**
 * The hint strings of [FullPagePullToRefresh].
 *
 * @param pulling Shown while the page is following the finger.
 * @param thresholdReached Shown once the pull has passed the threshold.
 * @param refreshing Shown while `isRefreshing` is true.
 * @param complete Shown after the refresh finished, during the hold.
 */
@Immutable
data class FullPagePullToRefreshTexts(
    val pulling: String,
    val thresholdReached: String,
    val refreshing: String,
    val complete: String,
)

/**
 * The colors used by [FullPagePullToRefresh].
 *
 * @param backgroundColor The page background revealed under the content.
 * @param textColor The hint text color.
 * @param indicatorColor The loading arc color.
 */
@Immutable
data class FullPagePullToRefreshColors(
    val backgroundColor: Color,
    val textColor: Color,
    val indicatorColor: Color,
)

/** Creates the default [FullPagePullToRefreshColors] from the current theme. */
@Composable
fun fullPagePullToRefreshColors(
    backgroundColor: Color = COUITheme.colorScheme.surface,
    textColor: Color = COUITheme.colorScheme.onSurface.copy(alpha = 0.54f),
    indicatorColor: Color = COUITheme.colorScheme.primary,
): FullPagePullToRefreshColors = FullPagePullToRefreshColors(
    backgroundColor = backgroundColor,
    textColor = textColor,
    indicatorColor = indicatorColor,
)

private const val PULL_DAMPING = 0.5f

/** How much of the reference 500ms-per-threshold timing the rebound to rest keeps. */
private const val REBOUND_DURATION_SCALE = 0.45f

/** The longest the rebound to rest may take, no matter how far the page was pulled. */
private const val REBOUND_MAX_DURATION_MILLIS = 220

private val DecelerateQuadEasing = Easing { fraction ->
    1f - (1f - fraction) * (1f - fraction)
}
