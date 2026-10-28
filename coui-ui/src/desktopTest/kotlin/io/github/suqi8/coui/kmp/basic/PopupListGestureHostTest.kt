// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PopupListGestureHostTest {
    @Test
    fun reversingMidSlideHeadsStraightBackToTheNewRow() = runHost(gap = 0f) { host, scope, _ ->
        host.onDown(24f, scope)
        mainClock.advanceTimeBy(300)
        host.onMoveTo(72f, scope)
        mainClock.advanceTimeBy(32)
        val atReversal = host.pressTop()
        assertTrue(atReversal > 0f && atReversal < 48f, "The mask must be mid-slide: $atReversal")
        host.onMoveTo(24f, scope)
        repeat(10) {
            mainClock.advanceTimeByFrame()
            assertTrue(host.pressTop() <= atReversal + 0.01f, "The carried speed must point at the newest row, not coast past the reversal: $atReversal -> ${host.pressTop()}")
        }
        assertTrue(host.pressTop() < atReversal - 1f, "The mask must be heading back by now: ${host.pressTop()}")
        mainClock.advanceTimeBy(500)
        assertEquals(0f, host.pressTop(), 0.01f)
    }

    @Test
    fun crossingAnotherRowMidSlideKeepsTheSpeedItHas() = runHost(gap = 0f, rows = 3) { host, scope, _ ->
        host.onDown(24f, scope)
        mainClock.advanceTimeBy(300)
        host.onMoveTo(72f, scope)
        repeat(3) { mainClock.advanceTimeByFrame() }
        val from = host.pressTop()
        host.onMoveTo(120f, scope)
        val frames = 3
        repeat(frames) { mainClock.advanceTimeByFrame() }
        // A critically damped slide of the same distance started from rest; the first frame of an
        // animation only records its start time, hence one frame less of play time.
        val distance = 96f - from
        val omega = sqrt(1754.6f)
        val t = (frames - 1) * 16f / 1000f
        val fromRest = distance * (1f - (1f + omega * t) * exp(-omega * t))
        val travelled = host.pressTop() - from
        assertTrue(travelled > fromRest + 2f, "Each crossing must carry the running speed instead of restarting from rest: $travelled vs $fromRest")
    }

    @Test
    fun leavingTheListFadesTheMaskWithoutFreezingItsSlide() = runHost(gap = 0f) { host, scope, _ ->
        host.onDown(24f, scope)
        mainClock.advanceTimeBy(300)
        host.onMoveTo(72f, scope)
        mainClock.advanceTimeBy(16)
        host.cancel(scope)
        mainClock.advanceTimeBy(500)
        assertEquals(48f, host.pressTop(), 0.01f, "The slide must finish on its row while the colour fades")
        assertEquals(0f, host.pressAlpha(), 0.01f)
    }

    @Test
    fun aDividerBandKeepsTheHighlightAndReleaseSelectsTheLastRow() = runHost(gap = 12f) { host, scope, chosen ->
        host.onDown(24f, scope)
        mainClock.advanceTimeBy(300)
        host.onMoveTo(54f, scope)
        mainClock.advanceTimeBy(300)
        assertEquals(1f, host.pressAlpha(), 0.01f, "Crossing a divider band must not fade the mask")
        host.onUp(scope)
        assertEquals(listOf(0), chosen)
    }

    @Test
    fun reenteringAfterLeavingSnapsOntoTheRow() = runHost(gap = 0f) { host, scope, _ ->
        host.onDown(24f, scope)
        mainClock.advanceTimeBy(300)
        host.cancel(scope)
        mainClock.advanceTimeBy(16)
        assertTrue(host.pressAlpha() > 0f, "The mask must still be fading")
        host.onMoveTo(72f, scope)
        assertEquals(48f, host.pressTop(), 0.01f, "An exiting mask must not slide from its old row")
    }
}

@OptIn(ExperimentalTestApi::class)
private fun runHost(
    gap: Float,
    rows: Int = 2,
    block: SkikoComposeUiTest.(PopupListGestureHost, CoroutineScope, MutableList<Int>) -> Unit,
) = runSkikoComposeUiTest(size = Size(200f, 200f), density = Density(1f)) {
    lateinit var scope: CoroutineScope
    setContent { scope = rememberCoroutineScope() }
    waitForIdle()
    val chosen = mutableListOf<Int>()
    val handles = List(rows) { index ->
        PopupListItemHandle().apply {
            top = index * (48f + gap)
            bottom = top + 48f
            onClick = { chosen += index }
        }
    }
    val host = PopupListGestureHost()
    host.onPlaced(handles, emptyMap())
    mainClock.autoAdvance = false
    block(host, scope, chosen)
}
