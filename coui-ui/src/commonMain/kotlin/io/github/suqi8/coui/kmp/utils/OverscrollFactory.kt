// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.updateLayerBlock
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import io.github.suqi8.coui.kmp.basic.LocalPullToRefreshState
import io.github.suqi8.coui.kmp.basic.PullToRefreshState
import io.github.suqi8.coui.kmp.basic.RefreshState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sign
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * A COUI implementation of [OverscrollFactory] that creates [OverscrollEffect] instances
 * with the same spring-based overscroll physics as [overScrollVertical] and [overScrollHorizontal].
 *
 * Provide this as [androidx.compose.foundation.LocalOverscrollFactory] in
 * [io.github.suqi8.coui.kmp.theme.COUITheme] to automatically apply the COUI overscroll
 * effect to all Compose scrollable components.
 */
object COUIOverscrollFactory : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = COUIOverscrollEffect()

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = 1
}

/**
 * A COUI [OverscrollEffect] implementing spring-based overscroll physics identical to
 * [overScrollVertical] and [overScrollHorizontal].
 *
 * Uses the same damping formula, spring parameters, and PullToRefresh coordination as the
 * modifier-based implementation. Both X and Y axes are handled independently.
 */
class COUIOverscrollEffect : OverscrollEffect {
    private val offsetThreshold = 1f

    // Placement pixel-snaps via round(), so only refresh when the whole-pixel value changes.
    private var lastPlacedOffsetX = 0f
    internal var offsetX = 0f
        private set(value) {
            if (field != value) {
                field = value
                val rounded = round(value)
                if (rounded != lastPlacedOffsetX) {
                    lastPlacedOffsetX = rounded
                    invalidateNodeLayer?.invoke()
                }
            }
        }

    private var lastPlacedOffsetY = 0f
    internal var offsetY = 0f
        private set(value) {
            if (field != value) {
                field = value
                val rounded = round(value)
                if (rounded != lastPlacedOffsetY) {
                    lastPlacedOffsetY = rounded
                    invalidateNodeLayer?.invoke()
                }
            }
        }

    // Injected by COUIOverscrollEffectNode when attached
    internal var invalidateNodeLayer: (() -> Unit)? = null

    private var rawTouchAccumulationX = 0f
    private var rawTouchAccumulationY = 0f

    internal var scrollRangeV = 0f
    internal var scrollRangeH = 0f

    private val springEngineX = SpringEngine()
    private val springEngineY = SpringEngine()
    private var animationJobX: Job? = null
    private var animationJobY: Job? = null

    // Injected by COUIOverscrollEffectNode when attached
    internal var launchAnimation: ((suspend CoroutineScope.() -> Unit) -> Job)? = null
    internal var getPullToRefreshState: (() -> PullToRefreshState?)? = null
    internal var getOverScrollState: (() -> OverScrollState?)? = null

    /** When the finger last moved the content; the settle is dated from here, not from the fling. */
    private var lastUserInputAt: TimeMark? = null

    override val isInProgress: Boolean
        get() = abs(offsetX) > offsetThreshold || abs(offsetY) > offsetThreshold

    override val node: DelegatableNode = COUIOverscrollEffectNode(this)

    private fun shouldBypassForPullToRefreshY(): Boolean {
        val pts = getPullToRefreshState?.invoke() ?: return false
        return pts.refreshState != RefreshState.Idle
    }

    internal fun resetAll() {
        offsetX = 0f
        offsetY = 0f
        rawTouchAccumulationX = 0f
        rawTouchAccumulationY = 0f
        getOverScrollState?.invoke()?.let { state ->
            if (state.isOverScrollActive) state.isOverScrollActive = false
        }
    }

    private fun resetStateX() {
        offsetX = 0f
        rawTouchAccumulationX = 0f
    }

    private fun resetStateY() {
        offsetY = 0f
        rawTouchAccumulationY = 0f
    }

    private fun applyDragX(delta: Float) {
        if (delta == 0f || scrollRangeH == 0f) return
        rawTouchAccumulationX += delta
        // `COUIPhysicalAnimationUtil.calcRealOverScrollDist`: the curve saturates towards the scroll
        // range on its own, so the accumulation needs no clamp.
        offsetX = SpringMath.obtainOverScrollOffset(rawTouchAccumulationX, scrollRangeH)
    }

    private fun applyDragY(delta: Float) {
        if (delta == 0f || scrollRangeV == 0f) return
        rawTouchAccumulationY += delta
        offsetY = SpringMath.obtainOverScrollOffset(rawTouchAccumulationY, scrollRangeV)
    }

    // Inverse of the damping curve: re-derive raw accumulation from offset when a drag takes over a spring.
    private fun syncRawAccumulationFromOffsetX() {
        rawTouchAccumulationX = SpringMath.obtainOverScrollTouchDistance(offsetX, scrollRangeH)
    }

    private fun syncRawAccumulationFromOffsetY() {
        rawTouchAccumulationY = SpringMath.obtainOverScrollTouchDistance(offsetY, scrollRangeV)
    }

    // Reclaims a stale offset once the child can scroll again in the accumulated direction (e.g. pagination); otherwise applyToFling swallows the next fling.
    private fun unwindStaleOffsetX(consumedDelta: Float) {
        if (abs(offsetX) <= offsetThreshold || consumedDelta == 0f) return
        if (rawTouchAccumulationX == 0f) syncRawAccumulationFromOffsetX()
        if (sign(consumedDelta) != sign(rawTouchAccumulationX)) return
        if (abs(rawTouchAccumulationX) <= abs(consumedDelta)) {
            resetStateX()
        } else {
            applyDragX(-consumedDelta)
        }
    }

    private fun unwindStaleOffsetY(consumedDelta: Float) {
        if (abs(offsetY) <= offsetThreshold || consumedDelta == 0f) return
        if (rawTouchAccumulationY == 0f) syncRawAccumulationFromOffsetY()
        if (sign(consumedDelta) != sign(rawTouchAccumulationY)) return
        if (abs(rawTouchAccumulationY) <= abs(consumedDelta)) {
            resetStateY()
        } else {
            applyDragY(-consumedDelta)
        }
    }

    /**
     * @param params The rebound curve; both entry points use the horizontal `springBack` pair, so a
     *   fling that runs into the boundary and a finger-up release come back at the same speed.
     * @param startedAt When the gesture ended, so the first frame steps the real elapsed time.
     */
    private fun startSpringAnimationX(
        initialVelocity: Float = 0f,
        params: SpringParams = SpringMath.SPRING_BACK_HORIZONTAL,
        startedAt: TimeMark = TimeSource.Monotonic.markNow(),
    ) {
        if (abs(offsetX) <= offsetThreshold && initialVelocity == 0f) {
            resetStateX()
            return
        }
        animationJobX?.cancel()
        animationJobX = launchAnimation?.invoke {
            springEngineX.runSettleAnimation(
                startValue = offsetX,
                initialVelocity = initialVelocity,
                params = params,
                startedAt = startedAt,
                onFrame = { currentPos ->
                    offsetX = currentPos
                },
                onSettle = {
                    if (abs(offsetX) <= offsetThreshold) resetStateX()
                },
            )
        }
    }

    /** @see startSpringAnimationX */
    private fun startSpringAnimationY(
        initialVelocity: Float = 0f,
        params: SpringParams = SpringMath.SPRING_BACK,
        startedAt: TimeMark = TimeSource.Monotonic.markNow(),
    ) {
        if (abs(offsetY) <= offsetThreshold && initialVelocity == 0f) {
            resetStateY()
            return
        }
        animationJobY?.cancel()
        animationJobY = launchAnimation?.invoke {
            springEngineY.runSettleAnimation(
                startValue = offsetY,
                initialVelocity = initialVelocity,
                params = params,
                startedAt = startedAt,
                onFrame = { currentPos ->
                    offsetY = currentPos
                },
                onSettle = {
                    if (abs(offsetY) <= offsetThreshold) resetStateY()
                },
            )
        }
    }

    private fun updateOverScrollState() {
        val state = getOverScrollState?.invoke() ?: return
        val isActive = isInProgress
        if (state.isOverScrollActive != isActive) {
            state.isOverScrollActive = isActive
        }
    }

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        if (source != NestedScrollSource.UserInput) {
            val consumed = performScroll(delta)
            // A running spring settles to zero on its own; only reclaim when it was interrupted.
            if (animationJobY?.isActive != true) unwindStaleOffsetY(consumed.y)
            if (animationJobX?.isActive != true) unwindStaleOffsetX(consumed.x)
            updateOverScrollState()
            return consumed
        }

        // The gesture's last movement. `applyToFling` is dispatched a frame after the pointer lifts,
        // so the settle has to be dated from here to land on the same frame the reference does.
        lastUserInputAt = TimeSource.Monotonic.markNow()

        val bypassY = shouldBypassForPullToRefreshY()

        // Resync raw accumulation, then cancel running springs the drag is taking over.
        if (!bypassY) {
            if (animationJobY?.isActive == true) syncRawAccumulationFromOffsetY()
            animationJobY?.cancel()
        }
        if (animationJobX?.isActive == true) syncRawAccumulationFromOffsetX()
        animationJobX?.cancel()

        // Y-axis pre-scroll: consume from overscroll first when scrolling back toward center
        var performScrollDeltaY = delta.y
        var extraConsumedY = 0f
        if (!bypassY && abs(offsetY) > offsetThreshold && delta.y != 0f && sign(delta.y) != sign(rawTouchAccumulationY)) {
            val canConsumeY = if (abs(rawTouchAccumulationY) <= abs(delta.y)) {
                -rawTouchAccumulationY
            } else {
                delta.y
            }
            if (abs(rawTouchAccumulationY) <= abs(delta.y)) {
                resetStateY()
                performScrollDeltaY = delta.y - canConsumeY
                extraConsumedY = canConsumeY
            } else {
                applyDragY(canConsumeY)
                performScrollDeltaY = 0f
                extraConsumedY = delta.y
            }
        }

        // X-axis pre-scroll: consume from overscroll first when scrolling back toward center
        var performScrollDeltaX = delta.x
        var extraConsumedX = 0f
        if (abs(offsetX) > offsetThreshold && delta.x != 0f && sign(delta.x) != sign(rawTouchAccumulationX)) {
            val canConsumeX = if (abs(rawTouchAccumulationX) <= abs(delta.x)) {
                -rawTouchAccumulationX
            } else {
                delta.x
            }
            if (abs(rawTouchAccumulationX) <= abs(delta.x)) {
                resetStateX()
                performScrollDeltaX = delta.x - canConsumeX
                extraConsumedX = canConsumeX
            } else {
                applyDragX(canConsumeX)
                performScrollDeltaX = 0f
                extraConsumedX = delta.x
            }
        }

        // Post-scroll: call performScroll, apply any unconsumed remainder to overscroll
        val adjustedDelta = Offset(performScrollDeltaX, performScrollDeltaY)
        val scrollConsumed = performScroll(adjustedDelta)
        val scrollRemaining = adjustedDelta - scrollConsumed

        if (!bypassY || animationJobY?.isActive != true) unwindStaleOffsetY(scrollConsumed.y)
        unwindStaleOffsetX(scrollConsumed.x)

        if (scrollRemaining.y != 0f && !bypassY) applyDragY(scrollRemaining.y)
        if (scrollRemaining.x != 0f) applyDragX(scrollRemaining.x)

        updateOverScrollState()

        // Total consumed = overscroll reduction + scroll consumed + overscroll-absorbed remainder
        return Offset(
            x = extraConsumedX + scrollConsumed.x + (if (scrollRemaining.x != 0f) scrollRemaining.x else 0f),
            y = extraConsumedY + scrollConsumed.y + (if (scrollRemaining.y != 0f && !bypassY) scrollRemaining.y else 0f),
        )
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        val isActiveY = abs(offsetY) > offsetThreshold
        val isActiveX = abs(offsetX) > offsetThreshold
        val bypassY = shouldBypassForPullToRefreshY() && !isActiveY

        // The gesture ends here; the first frame has to count from the last finger movement, which is
        // a frame earlier than this callback.
        val gestureEnd = lastUserInputAt ?: TimeSource.Monotonic.markNow()

        animationJobY?.cancel()
        animationJobX?.cancel()

        var performVelocity = velocity
        var springBackY = false
        var springBackX = false

        // Y-axis: the finger is up while the content is held past the edge, i.e.
        // `ReboundOverScroller.springBack`. That call springs from the current displacement alone —
        // it never reads a velocity — so a fast release does not carry the content further out.
        if (!bypassY && isActiveY) {
            springBackY = true
            startSpringAnimationY(0f, startedAt = gestureEnd)
            performVelocity = Velocity(
                performVelocity.x,
                // Same direction fling: the spring owns the overscroll, scroll gets nothing.
                // Opposite direction fling: scroll gets an attenuated share.
                if (sign(velocity.y) == sign(offsetY)) 0f else velocity.y / 2.13333f,
            )
        }

        // X-axis: same logic
        if (isActiveX) {
            springBackX = true
            startSpringAnimationX(0f, startedAt = gestureEnd)
            performVelocity = Velocity(
                if (sign(velocity.x) == sign(offsetX)) 0f else velocity.x / 2.13333f,
                performVelocity.y,
            )
        }

        val consumed = performFling(performVelocity)
        val remaining = performVelocity - consumed

        // Post-fling: a fling that runs into the boundary settles on `SPRING_BACK` like a finger-up
        // release. The fling curve's tail grows with the leftover velocity, which made the same
        // gesture come back in 0.2s or 2s depending on how hard it was thrown.
        if (!bypassY && !springBackY) {
            startSpringAnimationY(remaining.y / 1.53333f, startedAt = gestureEnd)
        }
        if (!springBackX) {
            startSpringAnimationX(remaining.x / 1.53333f, startedAt = gestureEnd)
        }

        updateOverScrollState()
    }
}

private class COUIOverscrollEffectNode(
    val effect: COUIOverscrollEffect,
) : Modifier.Node(),
    CompositionLocalConsumerModifierNode,
    LayoutModifierNode {

    /**
     * The content's translation, applied to the layer the child is placed with. It is a single
     * stable lambda so the offsets can refresh the layer through [updateLayerBlock] instead of a
     * placement pass: moving the content is a layer property change, and re-placing the subtree for
     * it costs a full traversal of the scrollable on every frame of the drag and the rebound.
     */
    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        translationX = round(effect.offsetX)
        translationY = round(effect.offsetY)
        clip = true
    }

    override fun onAttach() {
        super.onAttach()
        updateScrollRange()
        // `UNDISPATCHED` so the settle registers its frame callback inside the gesture callback
        // instead of waiting for the next frame to dispatch the coroutine: a dispatched launch costs
        // the release a whole frame before the spring moves at all.
        effect.launchAnimation = { block -> coroutineScope.launch(start = CoroutineStart.UNDISPATCHED, block = block) }
        effect.getPullToRefreshState = { currentValueOf(LocalPullToRefreshState) }
        effect.getOverScrollState = { currentValueOf(LocalOverScrollState) }
        effect.invalidateNodeLayer = { if (isAttached) updateLayerBlock(layerBlock) }
    }

    override fun onDetach() {
        super.onDetach()
        effect.launchAnimation = null
        effect.getPullToRefreshState = null
        effect.getOverScrollState = null
        effect.invalidateNodeLayer = null
        effect.resetAll()
    }

    private var cachedDensity: Density? = null
    private var cachedWindowInfo: WindowInfo? = null

    private fun updateScrollRange() {
        val density = currentValueOf(LocalDensity)
        val windowInfo = currentValueOf(LocalWindowInfo)
        if (density == cachedDensity && windowInfo == cachedWindowInfo) return
        cachedDensity = density
        cachedWindowInfo = windowInfo
        with(density) {
            effect.scrollRangeV = windowInfo.containerDpSize.height.toPx()
            effect.scrollRangeH = windowInfo.containerDpSize.width.toPx()
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        updateScrollRange()
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }
}
