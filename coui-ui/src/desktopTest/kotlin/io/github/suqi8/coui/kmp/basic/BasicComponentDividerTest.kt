// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BasicComponentDividerTest {
    @Test
    fun onlyTheHeadAndMiddleRowsOfACardGroupDrawADivider() {
        // COUIPreference.drawDivider(): positionInGroup == HEAD or MIDDLE, and couiShowDivider on.
        assertTrue(CardListPosition.Head.drawsRowDivider(true))
        assertTrue(CardListPosition.Middle.drawsRowDivider(true))
        assertFalse(CardListPosition.Tail.drawsRowDivider(true))
        assertFalse(CardListPosition.Full.drawsRowDivider(true))
        assertFalse(CardListPosition.None.drawsRowDivider(true))
    }

    @Test
    fun showDividerTurnsEveryHairlineOff() {
        CardListPosition.entries.forEach { position ->
            assertFalse(position.drawsRowDivider(false))
        }
    }
}
