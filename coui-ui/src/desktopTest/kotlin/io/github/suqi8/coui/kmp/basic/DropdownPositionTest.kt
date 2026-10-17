// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class DropdownPositionTest {
    private val window = IntRect(0, 0, 400, 1000)
    private val margin = IntRect(16, 8, 16, 8)
    private val provider = ListPopupDefaults.DropdownPositionProvider

    private fun y(anchor: IntRect, popupHeight: Int) = provider.calculatePosition(
        anchorBounds = anchor,
        windowBounds = window,
        layoutDirection = LayoutDirection.Ltr,
        popupContentSize = IntSize(200, popupHeight),
        popupMargin = margin,
        alignment = PopupPositionProvider.Align.TopStart,
    ).y

    @Test
    fun aMenuThatFitsBelowItsAnchorOpensBelow() {
        assertEquals(208, y(IntRect(0, 140, 400, 200), popupHeight = 300))
    }

    @Test
    fun aMenuThatOnlyFitsAboveItsAnchorOpensAbove() {
        assertEquals(900 - 300 - 8, y(IntRect(0, 900, 400, 960), popupHeight = 300))
    }

    @Test
    fun aMenuThatFitsOnNeitherSideStartsAtTheTopOfTheWindowInsteadOfCoveringItsAnchor() {
        assertEquals(8, y(IntRect(0, 470, 400, 530), popupHeight = 600))
    }
}
