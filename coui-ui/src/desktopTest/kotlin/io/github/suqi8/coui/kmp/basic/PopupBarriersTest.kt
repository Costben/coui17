// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PopupBarriersTest {

    @Test
    fun aBarThatEndsAboveTheAnchorDoesNotBoundThePopup() {
        val barriers = PopupBarriers()
        barriers.update(owner = "bar", top = 300, bottom = 400)

        assertNull(barriers.bottomBarrierTop(anchorTop = 400))
        assertNull(barriers.bottomBarrierTop(anchorTop = 900))
    }

    @Test
    fun aBarHoldingTheAnchorStillBoundsThePopup() {
        val barriers = PopupBarriers()
        barriers.update(owner = "bar", top = 2000, bottom = 2100)

        assertEquals(2000, barriers.bottomBarrierTop(anchorTop = 2010))
    }

    @Test
    fun theHighestBarBelowTheAnchorWins() {
        val barriers = PopupBarriers()
        barriers.update(owner = "above", top = 100, bottom = 200)
        barriers.update(owner = "low", top = 2000, bottom = 2100)
        barriers.update(owner = "lower", top = 2200, bottom = 2300)

        assertEquals(2000, barriers.bottomBarrierTop(anchorTop = 900))
    }
}
