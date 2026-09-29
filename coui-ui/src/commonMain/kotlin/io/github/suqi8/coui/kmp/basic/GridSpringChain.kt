// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import io.github.suqi8.coui.kmp.utils.LocalOverScrollState
import io.github.suqi8.coui.kmp.utils.OverScrollState
import io.github.suqi8.coui.kmp.utils.SpringEngine
import io.github.suqi8.coui.kmp.utils.SpringMath
import io.github.suqi8.coui.kmp.utils.runSettleAnimation
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign

/**
 * The ColorOS 17 grid spring-chain overscroll effect, mirroring
 * `com.coui.appcompat.springchain.COUIGridSpringChainScrollView` (a `COUIScrollView` subclass) and
 * the `com.coui.appcompat.springchain.COUIGridSpringChain` chain that drives it.
 *
 * On the device the scroll view holds a single child implementing
 * `ICOUIGridSpringChainViewGroup`; when a touch passes an edge it forwards the overscroll to the
 * chain, which moves each grid *row* by its own translation, so the tiles trail off in a chain
 * rather than moving as one block. This port keeps that split: [Modifier.gridSpringChainOverscroll]
 * on the grid measures the drag that the grid could not consume and damps it into one control
 * distance, while [Modifier.gridSpringChainItem] on each tile turns that distance into a per-row
 * translation.
 *
 * The damping curve and the release animation reuse the repository's existing overscroll engine —
 * [SpringMath.obtainDampingDistance] for the drag and `SpringEngine.runSettleAnimation`
 * (`utils/SpringUtils.kt`, the same spring `utils/Overscroll.kt` drives) for the settle — instead of
 * a second integrator. The node layout follows [Modifier.overScrollVertical].
 *
 * Not ported, because these device classes are Android View infrastructure with no Compose-side
 * equivalent: `COUIGridSpringChainScrollView` (`java.lang.Object` -> `COUIScrollView` ->
 * `ScrollView`), `COUIChainDecorator` (a `RecyclerView.ItemDecoration`-style touch helper that wires
 * a `View.OnTouchListener`), `ICOUIGridSpringChainViewGroup`, `ICheckViewEdge`, `IChainItem`,
 * `COUIChainItem` (a `View` proxy that calls `setTranslationX/Y`) and `COUIGridSpringChainItem`
 * (a `FrameLayout`). All of them only move `View`s, which Compose expresses with
 * [Modifier.gridSpringChainItem].
 *
 * @see GridSpringChainDefaults
 */
@Composable
fun rememberGridSpringChainState(
    translationCalculator: GridSpringChainTranslation = GridSpringChainDefaults.AutoAccumulatingTranslation,
): GridSpringChainState = remember(translationCalculator) {
    GridSpringChainState(translationCalculator)
}

/**
 * Holds the running overscroll distance of a [Modifier.gridSpringChainOverscroll] grid and resolves
 * the per-row translation each [Modifier.gridSpringChainItem] applies.
 *
 * Mirrors the state both device classes carry: the control distance is
 * `COUIGridSpringChain`'s current spring value, and the per-row translation is what
 * `COUIGridSpringChain#calculateTranslation` hands to `IChainItem#updateSpringY`.
 *
 * @param translationCalculator The chain function that maps a row index and the control distance to
 *   that row's translation. See [GridSpringChainTranslation].
 */
@Stable
class GridSpringChainState internal constructor(
    private val translationCalculator: GridSpringChainTranslation,
) {

    private var distance by mutableFloatStateOf(0f)

    /**
     * Whether the grid is currently pulled past its edge. Mirrors the `isOverScrollActive` flag
     * `COUIGridSpringChainScrollView` publishes through its overscroll helper; not the device's
     * `ICOUIGridSpringChainViewGroup#isSpringSystemRunning` return value.
     */
    var isActive: Boolean by mutableStateOf(false)
        internal set

    /**
     * The translation, in pixels, that the grid row [row] should carry for the current overscroll.
     * Read this inside a `graphicsLayer`/placement block so a changing distance only re-layers, not
     * recomposes.
     *
     * @param row The zero-based row index inside the grid.
     */
    fun translationFor(row: Int): Float = translationCalculator.translation(row, distance)

    internal fun updateDistance(value: Float) {
        distance = value
        isActive = abs(value) > 0.5f
    }

    internal fun currentDistance(): Float = distance

    internal fun resetDistance() {
        distance = 0f
        isActive = false
    }
}

/**
 * Maps a grid row and the current overscroll distance to that row's translation, the Compose
 * counterpart of `COUIGridSpringChain.TransCalculator` — the device's
 * `float getTrans(int index, float distance, int direction)`.
 */
fun interface GridSpringChainTranslation {

    /**
     * @param row The zero-based row index.
     * @param distance The signed control distance, in pixels: positive when the grid is pulled past
     *   its top edge, negative past its bottom edge.
     * @return The translation, in pixels, for [row].
     */
    fun translation(row: Int, distance: Float): Float
}

/**
 * Attaches the spring-chain overscroll to a grid. Put it on the scrollable grid itself, next to
 * [Modifier.gridSpringChainItem] on the tiles.
 *
 * A drag the grid cannot consume (it is already at an edge) is damped and stored as the state's
 * control distance; a fling then settles it back to zero with the repository's spring engine.
 *
 * @param state The [GridSpringChainState] shared with the grid's tiles.
 * @param enabled Whether the effect is attached. When false the grid scrolls normally.
 */
@Stable
fun Modifier.gridSpringChainOverscroll(
    state: GridSpringChainState,
    enabled: Boolean = true,
): Modifier = if (!enabled) this else this then GridSpringChainElement(state)

/**
 * Applies a grid tile's spring-chain translation. Put it on every tile of a
 * [Modifier.gridSpringChainOverscroll] grid.
 *
 * @param state The [GridSpringChainState] shared with the grid.
 * @param row The zero-based row index of the tile. Tiles of the same row share one translation,
 *   matching `COUIGridSpringChain`'s row-indexed `curSpringYArray` / `lastSpringYArray`.
 */
@Stable
fun Modifier.gridSpringChainItem(
    state: GridSpringChainState,
    row: Int,
): Modifier = this.graphicsLayer {
    translationY = state.translationFor(row)
}

private data class GridSpringChainElement(
    val state: GridSpringChainState,
) : ModifierNodeElement<GridSpringChainNode>() {
    override fun create(): GridSpringChainNode = GridSpringChainNode(state)

    override fun update(node: GridSpringChainNode) {
        node.update(state)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "gridSpringChainOverscroll"
    }
}

private class GridSpringChainNode(
    private var state: GridSpringChainState,
) : DelegatingNode(),
    CompositionLocalConsumerModifierNode,
    NestedScrollConnection {

    private val density: Density
        get() = currentValueOf(LocalDensity)

    private val windowInfo: WindowInfo
        get() = currentValueOf(LocalWindowInfo)

    private val overScrollState: OverScrollState
        get() = currentValueOf(LocalOverScrollState)

    private val dispatcher = NestedScrollDispatcher()
    private val springEngine = SpringEngine()
    private var animationJob: Job? = null

    private var rawAccumulation = 0f
    private var scrollRange = 0f
    private var cachedDensity: Density? = null
    private var cachedWindowInfo: WindowInfo? = null

    override fun onAttach() {
        super.onAttach()
        updateScrollRange()
        delegate(nestedScrollModifierNode(this, dispatcher))
    }

    override fun onDetach() {
        super.onDetach()
        animationJob?.cancel()
        rawAccumulation = 0f
        state.resetDistance()
    }

    fun update(newState: GridSpringChainState) {
        if (state === newState) return
        animationJob?.cancel()
        rawAccumulation = 0f
        state.resetDistance()
        state = newState
        if (isAttached) {
            overScrollState.isOverScrollActive = false
            updateScrollRange()
        }
    }

    private fun updateScrollRange() {
        val currentDensity = density
        val currentWindowInfo = windowInfo
        if (currentDensity == cachedDensity && currentWindowInfo == cachedWindowInfo) return
        cachedDensity = currentDensity
        cachedWindowInfo = currentWindowInfo
        // COUIGridSpringChainScrollView.MAX_DISTANCE_RATIO = 0.5f: the formal overscroll distance is
        // measured against half the host height.
        scrollRange = with(currentDensity) {
            currentWindowInfo.containerDpSize.height.toPx() * GridSpringChainDefaults.MaxDistanceRatio
        }
    }

    private fun applyDrag(delta: Float) {
        if (delta == 0f || scrollRange <= 0f) return
        rawAccumulation = (rawAccumulation + delta).coerceIn(-scrollRange, scrollRange)
        val normalized = min(abs(rawAccumulation) / scrollRange, 1.0f)
        val dampedDistance = SpringMath.obtainDampingDistance(normalized, scrollRange)
        state.updateDistance(sign(rawAccumulation) * dampedDistance)
        syncActiveState()
    }

    private fun syncActiveState() {
        if (!isAttached) return
        overScrollState.isOverScrollActive = state.isActive
    }

    private fun startSpringAnimation(initialVelocity: Float) {
        if (abs(state.currentDistance()) <= 0.5f && initialVelocity == 0f) {
            state.resetDistance()
            syncActiveState()
            return
        }

        animationJob?.cancel()
        animationJob = coroutineScope.launch {
            springEngine.runSettleAnimation(
                startValue = state.currentDistance(),
                initialVelocity = initialVelocity,
                onFrame = { state.updateDistance(it) },
                onSettle = {
                    if (abs(state.currentDistance()) <= 0.5f) {
                        state.resetDistance()
                        syncActiveState()
                    }
                },
            )
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (!isAttached || source != NestedScrollSource.UserInput) return Offset.Zero
        val delta = available.y
        if (delta == 0f) return Offset.Zero
        animationJob?.cancel()
        applyDrag(delta)
        // Consume the leftover so a parent does not also overscroll on top of the chain.
        return Offset(0f, delta)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (!isAttached) return Velocity.Zero
        animationJob?.cancel()
        if (abs(state.currentDistance()) <= 0.5f) return Velocity.Zero
        startSpringAnimation(available.y)
        return Velocity(0f, available.y)
    }
}

/** Contains constants and default calculators used by [rememberGridSpringChainState]. */
object GridSpringChainDefaults {

    /**
     * `COUIGridSpringChainScrollView.MAX_DISTANCE_FACTOR` (0.25f), the divisor of the formal-distance
     * formula `((height * 0.5f) - |translation|) / MAX_DISTANCE_FACTOR`.
     */
    val MaxDistanceFactor = 0.25f

    /** `COUIGridSpringChainScrollView.MAX_DISTANCE_RATIO` (0.5f), the formal distance as a fraction of the host height. */
    val MaxDistanceRatio = 0.5f

    /** `COUIGridSpringChainScrollView.OVER_SCROLL_FACTOR` (0.3f), the custom overscroll distance factor it passes to `setCustomOverScrollDistFactor`. */
    val OverScrollFactor = 0.3f

    /**
     * `COUIGridSpringChain.DefaultTransCalculator#getTrans` (0.1f) — the per-link translation factor
     * the device's default calculator multiplies `distance` by.
     */
    val DefaultTranslationFactor = 0.1f

    /**
     * `COUIChainDecorator.FACTOR` (0.3f), used by the decorator's own `backToTop` trans calculator.
     */
    val ChainFactor = 0.3f

    /**
     * `COUIChainDecorator.DEFAULT_MARGIN_VERTICAL` (24) — the trailing-row offset the decorator's
     * `backToTop` calculator subtracts. The device stores it as a raw int and uses it directly in the
     * pixel-space formula, so this port keeps it as a pixel value rather than converting it to dp.
     */
    val ChainMarginVertical = 24f

    /** `COUISpringChain.DEFAULT_MAIN_TENSION` (40). */
    val MainTension = 40f

    /** `COUISpringChain.DEFAULT_MAIN_FRICTION` (6). */
    val MainFriction = 6f

    /** `COUISpringChain.DEFAULT_ATTACHMENT_TENSION` (70). */
    val AttachmentTension = 70f

    /** `COUISpringChain.DEFAULT_ATTACHMENT_FRICTION` (10). */
    val AttachmentFriction = 10f

    /**
     * `COUIGridSpringChain.MAIN_TENSION` (150), the value the grid's constructor actually passes to
     * `COUISpringChain.create` — it overrides [MainTension].
     */
    val GridMainTension = 150f

    /**
     * `COUIGridSpringChain.MAIN_FRICTION` (10), the grid's constructor default — it overrides
     * [MainFriction].
     */
    val GridMainFriction = 10f

    /** `COUIGridSpringChain.BACK_TO_TOP` (1). */
    val BackToTop = 1

    /** `COUIGridSpringChain.BACK_TO_BOTTOM` (2). */
    val BackToBottom = 2

    /** `COUIGridSpringChain.BACK_TO_LEFT` (3). */
    val BackToLeft = 3

    /** `COUIGridSpringChain.BACK_TO_RIGHT` (4). */
    val BackToRight = 4

    /**
     * The device's default chain: `COUIGridSpringChain` runs with `enableAutoAcc = true` and both
     * back-to-top and back-to-bottom calculators left at `DefaultTransCalculator`, so
     * `calculateTranslation` accumulates `getTrans(0..row, distance)` =
     * `(row + 1) * 0.1f * distance`. Later rows carry a larger translation than earlier ones.
     */
    val AutoAccumulatingTranslation = GridSpringChainTranslation { row, distance ->
        (row + 1) * DefaultTranslationFactor * distance
    }

    /**
     * `COUIChainDecorator`'s own `backToTop` trans calculator, exposed for callers that want the
     * trailing-row ladder instead of the accumulating default. The device defines it but then
     * overwrites the installed calculators with `DefaultTransCalculator`, so it is *not* what the
     * default grid uses.
     *
     * Rows get `FACTOR * distance` scaled by 0.2f and the decorator's ratios
     * (0.6 / 0.8 / 0.75 / 0.75), minus [ChainMarginVertical], then clamped to the direction of
     * `distance` (`coerceAtLeast(0)` toward the top, `coerceAtMost(0)` toward the bottom).
     */
    val ChainLadderTranslation = GridSpringChainTranslation { row, distance ->
        val magnitude = when (row) {
            0 -> ChainFactor * distance
            1 -> ChainFactor * distance * 0.2f * 0.6f - ChainMarginVertical
            2 -> ChainFactor * distance * 0.2f - ChainMarginVertical
            3 -> ChainFactor * distance * 0.2f * 0.6f * 0.8f - ChainMarginVertical
            4 -> ChainFactor * distance * 0.2f * 0.75f - ChainMarginVertical
            5 -> ChainFactor * distance * 0.2f * 0.75f * 0.75f - ChainMarginVertical
            else -> ChainFactor * distance * 0.2f * 0.75f.pow(row - 3) - ChainMarginVertical
        }
        if (distance >= 0f) max(magnitude, 0f) else min(magnitude, 0f)
    }
}
