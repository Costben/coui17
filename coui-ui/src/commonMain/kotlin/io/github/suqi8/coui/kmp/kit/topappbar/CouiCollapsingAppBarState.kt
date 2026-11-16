// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.topappbar

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.kit.motion.CouiSpring
import kotlin.math.roundToInt

internal object CouiCollapsingAppBarDefaults {
    val titleHeight = 116.dp
    val titleOnlyHeight = 110.dp
    val collapsedTitleHeight = 52.dp
    val expandedTitleSize = 32.dp
    val collapsedTitleSize = 24.dp

    // Standalone title uses compact note layout margins until wide-screen window-size class switching is wired.
    val titleOnlyStart = 16.dp
    val titleOnlyTop = 54.dp
    val titleStart = 24.dp
    val titleBottom = 18.dp
    val resultsStartOffset = 108.dp
    internal val titleOnlyBlurExtension = 8.dp
    internal val searchBlurExtension = 14.dp
    internal const val referenceBlurRadiusPx = 27f
    internal const val snapResponse = .25f
    internal val titleEasing = CubicBezierEasing(.33f, 0f, .67f, 1f)
}

/** Collapses the top bar first on forward scroll and expands after content reaches the top; shares nestedScrollConnection with the scrolling list. */
@Stable
internal class CouiCollapsingAppBarState internal constructor(
    rangePx: Float,
    initialFraction: Float,
    restoreFraction: Float = initialFraction,
    initialSearching: Boolean = false,
) {
    internal var rangePx by mutableFloatStateOf(rangePx)
    internal var offsetPx by mutableFloatStateOf((if (initialSearching) 1f else initialFraction).coerceIn(0f, 1f) * rangePx)
    internal var contentOffsetPx by mutableFloatStateOf(offsetPx)
        private set
    private var logicalOffset = offsetPx
    internal var searchLocked = false
    internal var canScroll: () -> Boolean = { true }
    private var snap: CouiSpring? = null
    private var contentSnap: CouiSpring? = null
    private var searching = initialSearching
    private var hasSearchBar = true
    private var expandedHeight = CouiCollapsingAppBarDefaults.titleOnlyHeight
    private var collapsedHeight = CouiCollapsingAppBarDefaults.collapsedTitleHeight
    private var restoreOffset = restoreFraction * rangePx
    val collapsedFraction: Float get() = (offsetPx / rangePx).coerceIn(0f, 1f)

    /** Pixels added to content layers using measured top bar padding so lists follow logical scroll while search follows its own spring. */
    val contentTranslationY: Float get() = offsetPx - contentOffsetPx

    /**
     * Adjusts top content padding by logical scroll while keeping bottom safe insets and viewport fixed.
     * Pass to scrolling content padding and consume original Scaffold padding instead of translating the viewport.
     */
    fun contentPadding(padding: PaddingValues, density: Density): PaddingValues = object : PaddingValues {
        override fun calculateLeftPadding(layoutDirection: LayoutDirection) = padding.calculateLeftPadding(layoutDirection)
        override fun calculateRightPadding(layoutDirection: LayoutDirection) = padding.calculateRightPadding(layoutDirection)
        override fun calculateTopPadding() = (padding.calculateTopPadding() + with(density) { contentTranslationY.toDp() }).coerceAtLeast(0.dp)
        override fun calculateBottomPadding() = padding.calculateBottomPadding()
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (searchLocked || available.y >= 0f) return Offset.Zero
            return Offset(0f, consume(available.y))
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (searchLocked || available.y <= 0f) return Offset.Zero
            return Offset(0f, consume(available.y))
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (!searchLocked) {
                if (hasSearchBar && !canScroll()) {
                    animateOffset(0f, animateContent = true)
                } else if (logicalOffset > 0f && logicalOffset < rangePx) {
                    settle()
                }
            }
            return Velocity.Zero
        }
    }

    private fun consume(delta: Float): Float {
        val previous = logicalOffset
        val target = (previous - delta).coerceIn(0f, rangePx)
        // ViewOffsetHelper returns early when offsetTop is unchanged so continued scrolling does not retarget repeatedly.
        if (target == previous) return 0f
        // Keep logical scroll target and spring offset separate so consumption is not computed from lagged visual position.
        animateOffset(target, if (delta < 0f) .2f else .1f)
        return previous - logicalOffset
    }

    private fun settle() {
        val target = if (logicalOffset < rangePx * .5f) 0f else rangePx
        animateOffset(target, animateContent = true)
    }

    private fun animateOffset(
        target: Float,
        response: Float = CouiCollapsingAppBarDefaults.snapResponse,
        animateContent: Boolean = false,
    ) {
        logicalOffset = target
        // CoordinatorScrollingViewAnimator: content follows direct scroll immediately and animates only on settle.
        if (animateContent) {
            val child = contentSnap ?: CouiSpring(contentOffsetPx, minimumChange = 1f).also { contentSnap = it }
            child.setStartValue(contentOffsetPx)
            child.animateTo(
                target = target,
                response = response,
                bounce = 0f,
                onFrame = { value -> contentOffsetPx = (-((-value).roundToInt())).toFloat().coerceIn(0f, rangePx) },
            )
        } else {
            contentSnap?.cancel()
            // Clear pending target and velocity once content switches back to direct scroll following.
            contentSnap = null
            contentOffsetPx = target
        }
        val animation = snap ?: CouiSpring(offsetPx, minimumChange = 1f).also { snap = it }
        // COUIViewOffsetHelper resets start to the visible rounded integer position on each retarget while preserving velocity.
        animation.setStartValue(offsetPx)
        animation.animateTo(
            target = target,
            response = response,
            bounce = 0f,
            minimumChange = 1f,
            onFrame = { value -> offsetPx = (-((-value).roundToInt())).toFloat().coerceIn(0f, rangePx) },
        )
    }

    internal fun updateSearch(expanded: Boolean, active: Boolean) {
        searchLocked = active
        if (searching == expanded) return
        searching = expanded
        if (expanded) restoreOffset = if (offsetPx >= rangePx) rangePx else 0f
        animateOffset(if (expanded) rangePx else restoreOffset, animateContent = true)
    }

    internal fun updateRange(
        density: Density,
        hasSearchBar: Boolean = this.hasSearchBar,
        expandedHeight: Dp = this.expandedHeight,
        collapsedHeight: Dp = this.collapsedHeight,
    ) {
        this.hasSearchBar = hasSearchBar
        this.expandedHeight = expandedHeight
        this.collapsedHeight = collapsedHeight
        val range = with(density) {
            if (hasSearchBar) {
                CouiCollapsingAppBarDefaults.titleHeight.roundToPx().toFloat()
            } else {
                (expandedHeight.roundToPx() - collapsedHeight.roundToPx()).toFloat().coerceAtLeast(1f)
            }
        }
        if (range == rangePx) return
        val fraction = logicalOffset / rangePx
        val restore = restoreOffset / rangePx
        dispose()
        rangePx = range
        // Adopt accepted logical position after re-measurement rather than intermediate cancelled spring coordinates.
        offsetPx = fraction * range
        logicalOffset = offsetPx
        contentOffsetPx = offsetPx
        restoreOffset = restore * range
    }

    internal fun saveState() = listOf(logicalOffset / rangePx, restoreOffset / rangePx, if (searching) 1f else 0f)

    internal fun dispose() {
        snap?.cancelComplete()
        snap = null
        contentSnap?.cancelComplete()
        contentSnap = null
    }
}

@Composable
internal fun rememberCouiCollapsingAppBarState(canScroll: () -> Boolean = { true }): CouiCollapsingAppBarState {
    val density = LocalDensity.current
    val range = with(density) { CouiCollapsingAppBarDefaults.titleHeight.roundToPx().toFloat() }
    val saver = listSaver<CouiCollapsingAppBarState, Float>(
        save = { it.saveState() },
        restore = { CouiCollapsingAppBarState(range, it[0], it[1], it[2] > 0f) },
    )
    val state = rememberSaveable(saver = saver) { CouiCollapsingAppBarState(range, 0f) }
    SideEffect {
        state.updateRange(density)
        state.canScroll = canScroll
    }
    DisposableEffect(state) { onDispose { state.dispose() } }
    return state
}
