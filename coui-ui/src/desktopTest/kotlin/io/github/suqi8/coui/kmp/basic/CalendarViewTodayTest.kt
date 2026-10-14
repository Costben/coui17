// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalTestApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarViewTodayTest {
    /**
     * The default `today` reads the device's local date and keeps re-reading it. Composing that
     * path must leave the test clock idle.
     */
    @Test
    fun defaultTodayReadsTheLocalDateWithoutBlockingTheComposition() = runSkikoComposeUiTest(size = Size(360f, 480f), density = Density(1f)) {
        var selected = DateValue(2026, 10, 14)
        setContent {
            COUITheme {
                Box(Modifier.fillMaxSize()) {
                    CalendarView(value = selected, onValueChange = { selected = it })
                }
            }
        }
        waitForIdle()
        assertEquals(DateValue(2026, 10, 14), selected)
        onRoot().fetchSemanticsNode()
    }

    /** 2026-10-13T20:00:00Z is already the 14th east of UTC+4 and still the 13th to the west. */
    @Test
    fun localDateAppliesTheUtcOffset() {
        val instant = 1_791_921_600L
        assertEquals(DateValue(2026, 10, 13), localDateAt(instant, 0))
        assertEquals(DateValue(2026, 10, 14), localDateAt(instant, 8 * 3_600))
        assertEquals(DateValue(2026, 10, 14), localDateAt(instant, 5 * 3_600 + 30 * 60))
        assertEquals(DateValue(2026, 10, 13), localDateAt(instant, -5 * 3_600))
        assertEquals(DateValue(2026, 10, 12), localDateAt(instant - 20 * 3_600 - 1, -1))
    }

    /**
     * The refresh must not rely on one long wait for midnight: the monotonic clock behind `delay`
     * pauses while the device sleeps, so the date is re-read at least once a minute.
     */
    @Test
    fun currentDateIsReReadWithinAMinute() = runSkikoComposeUiTest(size = Size(10f, 10f), density = Density(1f)) {
        mainClock.autoAdvance = false
        var deviceDate = DateValue(2026, 10, 13)
        var shown: DateValue? = null
        setContent {
            shown = rememberCurrentDate { deviceDate }
        }
        mainClock.advanceTimeByFrame()
        assertEquals(DateValue(2026, 10, 13), shown)

        deviceDate = DateValue(2026, 10, 14)
        mainClock.advanceTimeBy(60_000L)
        assertEquals(DateValue(2026, 10, 14), shown)
    }
}
