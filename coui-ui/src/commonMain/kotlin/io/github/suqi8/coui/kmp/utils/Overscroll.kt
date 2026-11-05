// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.updateLayerBlock
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import io.github.suqi8.coui.kmp.basic.LocalPullToRefreshState
import io.github.suqi8.coui.kmp.basic.PullToRefreshState
import io.github.suqi8.coui.kmp.basic.RefreshState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sign
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * @see overScrollOutOfBound
 */
@Stable
fun Modifier.overScrollVertical(
    nestedScrollToParent: Boolean = true,
    isEnabled: () -> Boolean = { platform() == Platform.Android || platform() == Platform.IOS },
): Modifier = overScrollOutOfBound(
    isVertical = true,
    nestedScrollToParent = nestedScrollToParent,
    isEnabled = isEnabled,
)

/**
 * @see overScrollOutOfBound
 */
@Stable
fun Modifier.overScrollHorizontal(
    nestedScrollToParent: Boolean = true,
    isEnabled: () -> Boolean = { platform() == Platform.Android || platform() == Platform.IOS },
): Modifier = overScrollOutOfBound(
    isVertical = false,
    nestedScrollToParent = nestedScrollToParent,
    isEnabled = isEnabled,
)

/**
 * Overscroll effect when scrolling to the boundary.
 *
 * Both halves of the physics are ported from the ColorOS 17 list overscroll: the drag damping is
 * `COUIPhysicalAnimationUtil.calcRealOverScrollDist` (see [SpringMath.obtainOverScrollOffset]) and
 * the release is `SpringOverScroller.ReboundOverScroller.springBack`, at the vertical or horizontal
 * stiffness depending on the axis (see [SpringMath.SPRING_BACK] and
 * [SpringMath.SPRING_BACK_HORIZONTAL]).
 *
 * @param isVertical Whether the overscroll effect is vertical or horizontal.
 * @param nestedScrollToParent Whether to dispatch nested scroll events to parent.
 * @param isEnabled Whether the overscroll effect is enabled. Default is enabled on Android and iOS only.
 */
@Stable
fun Modifier.overScrollOutOfBound(
    isVertical: Boolean = true,
    nestedScrollToParent: Boolean = true,
    isEnabled: () -> Boolean = { platform() == Platform.Android || platform() == Platform.IOS },
): Modifier {
    if (!isEnabled()) return this

    return this
        .clipToBounds()
        .then(
            OverscrollElement(
                isVertical = isVertical,
                nestedScrollToParent = nestedScrollToParent,
            ),
        )
}

private data class OverscrollElement(
    val isVertical: Boolean,
    val nestedScrollToParent: Boolean,
) : ModifierNodeElement<OverscrollNode>() {
    override fun create(): OverscrollNode = OverscrollNode(
        isVertical = isVertical,
        nestedScrollToParent = nestedScrollToParent,
    )

    override fun update(node: OverscrollNode) {
        node.update(
            isVertical = isVertical,
            nestedScrollToParent = nestedScrollToParent,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "overScrollOutOfBound"
        properties["isVertical"] = isVertical
        properties["nestedScrollToParent"] = nestedScrollToParent
    }
}

private class OverscrollNode(
    var isVertical: Boolean,
    var nestedScrollToParent: Boolean,
) : DelegatingNode(),
    CompositionLocalConsumerModifierNode,
    LayoutModifierNode,
    NestedScrollConnection {
    private val density: Density
        get() = currentValueOf(LocalDensity)

    private val windowInfo: WindowInfo
        get() = currentValueOf(LocalWindowInfo)

    private val overScrollState: OverScrollState
        get() = currentValueOf(LocalOverScrollState)

    private val pullToRefreshState: PullToRefreshState?
        get() = currentValueOf(LocalPullToRefreshState)

    private val dispatcher = NestedScrollDispatcher()
    private val springEngine = SpringEngine()
    private var animationJob: Job? = null
    private val offsetThreshold = 1f

    /** When the finger last moved the content; the settle is dated from here, not from the fling. */
    private var lastUserInputAt: TimeMark? = null

    private var lastPlacedOffset = 0f

    /**
     * The content's translation, applied to the layer the child is placed with. It is a single
     * stable lambda so [offset] can refresh the layer through [updateLayerBlock] instead of a
     * placement pass: moving the content is a layer property change, and re-placing the subtree for
     * it costs a full traversal of the scrollable on every frame of the drag and the rebound.
     */
    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        if (isVertical) {
            translationY = round(offset)
        } else {
            translationX = round(offset)
        }
        clip = true
    }

    var offset = 0f
        private set(value) {
            if (field != value) {
                field = value
                // Placement pixel-snaps via round(), so only refresh when the whole-pixel value changes.
                val rounded = round(value)
                if (rounded != lastPlacedOffset) {
                    lastPlacedOffset = rounded
                    if (layerReady) updateLayerBlock(layerBlock)
                }
            }
        }

    /**
     * Whether the child's layer may be touched. `isAttached` is still true inside [onDetach], and
     * [updateLayerBlock] would then build a layer for a node that is on its way out.
     */
    private var layerReady = false

    private var rawTouchAccumulation = 0f
    private var scrollRange: Float = 0f
    private var cachedScrollRangeDensity: Density? = null
    private var cachedScrollRangeWindowInfo: WindowInfo? = null

    override fun onAttach() {
        super.onAttach()
        layerReady = true
        updateScrollRange()
        delegate(nestedScrollModifierNode(this, dispatcher))
    }

    override fun onDetach() {
        layerReady = false
        super.onDetach()
        resetState()
    }

    fun update(
        isVertical: Boolean,
        nestedScrollToParent: Boolean,
    ) {
        var rangeChanged = false
        if (this.isVertical != isVertical) {
            rangeChanged = true
        }

        this.isVertical = isVertical
        this.nestedScrollToParent = nestedScrollToParent

        if (rangeChanged && isAttached) {
            updateScrollRange()
            // The layer block reads `isVertical`, and a placement is no longer what refreshes it.
            if (layerReady) updateLayerBlock(layerBlock)
        }
    }

    private fun updateScrollRange() {
        val currentDensity = density
        val currentWindowInfo = windowInfo
        if (currentDensity == cachedScrollRangeDensity && currentWindowInfo == cachedScrollRangeWindowInfo) return
        cachedScrollRangeDensity = currentDensity
        cachedScrollRangeWindowInfo = currentWindowInfo
        // The reference feeds `COUIPhysicalAnimationUtil.calcRealOverScrollDist` the display height
        // in px (`COUIRecyclerView.mOverscrollDistance`); the window size is the same number for a
        // full-screen list and stays correct for a list that does not fill the display.
        scrollRange = with(currentDensity) {
            if (isVertical) {
                currentWindowInfo.containerDpSize.height.toPx()
            } else {
                currentWindowInfo.containerDpSize.width.toPx()
            }
        }
    }

    private fun resetState() {
        offset = 0f
        rawTouchAccumulation = 0f
        if (isAttached) {
            overScrollState.isOverScrollActive = false
        }
    }

    /**
     * @param params The rebound curve; both entry points use the axis' `springBack` pair, so a fling
     *   that runs into the boundary and a finger-up release come back at the same speed.
     * @param startedAt When the gesture ended, so the first frame steps the real elapsed time.
     */
    private fun startSpringAnimation(
        initialVelocity: Float = 0f,
        params: SpringParams = if (isVertical) SpringMath.SPRING_BACK else SpringMath.SPRING_BACK_HORIZONTAL,
        startedAt: TimeMark = TimeSource.Monotonic.markNow(),
    ) {
        if (abs(offset) <= offsetThreshold && initialVelocity == 0f) {
            resetState()
            return
        }

        animationJob?.cancel()
        animationJob = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            springEngine.runSettleAnimation(
                startValue = offset,
                initialVelocity = initialVelocity,
                params = params,
                startedAt = startedAt,
                onFrame = { currentPos ->
                    offset = currentPos
                },
                onSettle = {
                    if (abs(offset) <= offsetThreshold) resetState()
                },
            )
        }
    }

    private fun shouldBypassForPullToRefresh(): Boolean {
        // When pull-to-refresh is active (not Idle), always bypass.
        return pullToRefreshState != null && pullToRefreshState?.refreshState != RefreshState.Idle && isVertical
    }

    private fun applyDrag(delta: Float) {
        if (delta == 0f || scrollRange <= 0f) return
        rawTouchAccumulation += delta
        // `COUIPhysicalAnimationUtil.calcRealOverScrollDist`: the curve saturates towards the scroll
        // range on its own, so the accumulation needs no clamp.
        offset = SpringMath.obtainOverScrollOffset(rawTouchAccumulation, scrollRange)
    }

    /** Inverse of the damping curve: re-derive [rawTouchAccumulation] from [offset] when a drag takes over a spring. */
    private fun syncRawAccumulationFromOffset() {
        rawTouchAccumulation = SpringMath.obtainOverScrollTouchDistance(offset, scrollRange)
    }

    /** Reclaims a stale offset once the child can scroll again in the accumulated direction (e.g. pagination); otherwise [onPreFling] swallows the next fling. */
    private fun unwindStaleOffset(consumedDelta: Float) {
        if (abs(offset) <= offsetThreshold || consumedDelta == 0f) return
        if (rawTouchAccumulation == 0f) syncRawAccumulationFromOffset()
        if (sign(consumedDelta) != sign(rawTouchAccumulation)) return
        if (abs(rawTouchAccumulation) <= abs(consumedDelta)) {
            resetState()
        } else {
            applyDrag(-consumedDelta)
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        updateScrollRange()
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (!isAttached) return Offset.Zero
        val isActive = abs(offset) > offsetThreshold
        if (overScrollState.isOverScrollActive != isActive) {
            overScrollState.isOverScrollActive = isActive
        }

        if (source == NestedScrollSource.UserInput) lastUserInputAt = TimeSource.Monotonic.markNow()

        if (shouldBypassForPullToRefresh() || source != NestedScrollSource.UserInput) {
            return dispatcher.dispatchPreScroll(available, source)
        }

        // Resync raw accumulation when a drag takes over a running spring.
        if (animationJob?.isActive == true) syncRawAccumulationFromOffset()
        animationJob?.cancel()

        val parentConsumed = if (nestedScrollToParent) {
            dispatcher.dispatchPreScroll(available, source)
        } else {
            Offset.Zero
        }

        val realAvailable = available - parentConsumed
        val delta = if (isVertical) realAvailable.y else realAvailable.x

        if (abs(offset) <= offsetThreshold || sign(delta) == sign(rawTouchAccumulation)) {
            return parentConsumed
        }

        if (sign(delta) != sign(rawTouchAccumulation)) { // opposite direction
            val actualConsumed = if (abs(rawTouchAccumulation) <= abs(delta)) {
                -rawTouchAccumulation // can be fully consumed
            } else {
                delta
            }

            if (abs(rawTouchAccumulation) <= abs(delta)) {
                resetState() // reset directly after complete consumption
            } else {
                applyDrag(actualConsumed)
            }

            return if (isVertical) {
                Offset(parentConsumed.x, actualConsumed + parentConsumed.y)
            } else {
                Offset(actualConsumed + parentConsumed.x, parentConsumed.y)
            }
        }

        applyDrag(delta)
        return if (isVertical) Offset(parentConsumed.x, available.y) else Offset(available.x, parentConsumed.y)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (!isAttached) return Offset.Zero
        val isActive = abs(offset) > offsetThreshold
        if (overScrollState.isOverScrollActive != isActive) {
            overScrollState.isOverScrollActive = isActive
        }

        if (source == NestedScrollSource.UserInput) lastUserInputAt = TimeSource.Monotonic.markNow()

        if (shouldBypassForPullToRefresh() || source != NestedScrollSource.UserInput) {
            // A running spring settles to zero on its own; only reclaim when it was interrupted.
            if (animationJob?.isActive != true) {
                unwindStaleOffset(if (isVertical) consumed.y else consumed.x)
            }
            return dispatcher.dispatchPostScroll(consumed, available, source)
        }

        animationJob?.cancel()
        unwindStaleOffset(if (isVertical) consumed.y else consumed.x)

        val parentConsumed = if (nestedScrollToParent) {
            dispatcher.dispatchPostScroll(consumed, available, source)
        } else {
            Offset.Zero
        }

        val realAvailable = available - parentConsumed
        val delta = if (isVertical) realAvailable.y else realAvailable.x

        applyDrag(delta)
        return if (isVertical) Offset(parentConsumed.x, available.y) else Offset(available.x, parentConsumed.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (!isAttached) return Velocity.Zero
        val isActive = abs(offset) > offsetThreshold
        if (overScrollState.isOverScrollActive != isActive) {
            overScrollState.isOverScrollActive = isActive
        }

        if (shouldBypassForPullToRefresh() && !overScrollState.isOverScrollActive) {
            return dispatcher.dispatchPreFling(available)
        }

        // The gesture ends here; the first frame has to count from the last finger movement.
        val gestureEnd = lastUserInputAt ?: TimeSource.Monotonic.markNow()
        animationJob?.cancel()

        val parentConsumed = if (nestedScrollToParent) {
            dispatcher.dispatchPreFling(available)
        } else {
            Velocity.Zero
        }

        val realAvailable = available - parentConsumed
        val velocity = if (isVertical) realAvailable.y else realAvailable.x

        if (abs(offset) > offsetThreshold) {
            // `ReboundOverScroller.springBack` springs from the current displacement alone and never
            // reads a velocity, so the release velocity is dropped here rather than carried outwards.
            if (sign(velocity) != sign(offset)) {
                startSpringAnimation(0f, startedAt = gestureEnd)
                // Optimize speed and feel to prevent violent throwing
                return parentConsumed + if (isVertical) {
                    Velocity(
                        0f,
                        realAvailable.y / 2.13333f,
                    )
                } else {
                    Velocity(realAvailable.x / 2.13333f, 0f)
                }
            } else {
                startSpringAnimation(0f, startedAt = gestureEnd)
                return parentConsumed + if (isVertical) Velocity(0f, realAvailable.y) else Velocity(realAvailable.x, 0f)
            }
        }

        return parentConsumed
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (!isAttached) return Velocity.Zero
        val isActive = abs(offset) > offsetThreshold
        if (overScrollState.isOverScrollActive != isActive) {
            overScrollState.isOverScrollActive = isActive
        }

        if (shouldBypassForPullToRefresh() && !overScrollState.isOverScrollActive) {
            return dispatcher.dispatchPostFling(consumed, available)
        }

        // A rebound the finger-up started in `onPreFling` is already settling on `SPRING_BACK`.
        val springingBack = animationJob?.isActive == true
        if (!springingBack) animationJob?.cancel()

        val gestureEnd = lastUserInputAt ?: TimeSource.Monotonic.markNow()

        val parentConsumed = if (nestedScrollToParent) {
            dispatcher.dispatchPostFling(consumed, available)
        } else {
            Velocity.Zero
        }

        if (springingBack) return parentConsumed

        val realAvailable = available - parentConsumed
        val velocity = (if (isVertical) realAvailable.y else realAvailable.x) / 1.53333f // attenuation speed
        // A fling that runs into the boundary also settles on `SPRING_BACK`. The fling curve
        // (`COUISpringInterpolator(40, 1.15)`) shortens as the leftover velocity drops, so the same
        // gesture came back in 0.2s or 2s depending on how hard it was thrown; the finger-up curve
        // is back in about 1.1s from anything a fling can leave.
        startSpringAnimation(velocity, startedAt = gestureEnd)

        return parentConsumed + if (isVertical) Velocity(0f, velocity) else Velocity(velocity, 0f)
    }
}

/**
 * OverScrollState is used to control the overscroll effect.
 *
 * @param isOverScrollActive Whether the overscroll effect is active.
 */
class OverScrollState {
    var isOverScrollActive by mutableStateOf(false)
        internal set
}

/**
 * [LocalOverScrollState] is used to provide the [OverScrollState] instance to the composition.
 *
 * @see OverScrollState
 */
val LocalOverScrollState = compositionLocalOf { OverScrollState() }
