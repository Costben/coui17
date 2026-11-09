// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import kotlin.test.Test
import kotlin.test.assertEquals

class PageIndicatorColorTest {
    @Test
    fun theDotOfAPageAtRestIsFullyActive() {
        assertEquals(1f, PageIndicatorDefaults.dotActiveFraction(index = 2, position = 2f))
        assertEquals(0f, PageIndicatorDefaults.dotActiveFraction(index = 1, position = 2f))
        assertEquals(0f, PageIndicatorDefaults.dotActiveFraction(index = 3, position = 2f))
    }

    @Test
    fun midwayBothDotsOfTheTraceAreActive() {
        assertEquals(1f, PageIndicatorDefaults.dotActiveFraction(index = 2, position = 2.5f))
        assertEquals(1f, PageIndicatorDefaults.dotActiveFraction(index = 3, position = 2.5f))
    }

    @Test
    fun theFarDotFadesInOverTheThreshold() {
        assertEquals(1f, PageIndicatorDefaults.dotActiveFraction(index = 2, position = 2.025f))
        assertEquals(0.5f, PageIndicatorDefaults.dotActiveFraction(index = 3, position = 2.025f), 1e-3f)
        assertEquals(1f, PageIndicatorDefaults.dotActiveFraction(index = 3, position = 2.975f))
        assertEquals(0.5f, PageIndicatorDefaults.dotActiveFraction(index = 2, position = 2.975f), 1e-3f)
    }
}
