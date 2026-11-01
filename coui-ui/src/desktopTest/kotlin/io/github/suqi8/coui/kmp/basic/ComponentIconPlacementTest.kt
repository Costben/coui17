// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.unit.Density
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ComponentIconPlacementTest {
    private val titleLine = TextLineGeometry(lines = 1, firstLineBottomPx = 22, heightPx = 22)
    private val noSummary = TextLineGeometry()

    @Test
    fun aBlockOfAtMostTwoLinesKeepsTheIconCentred() {
        // setIconCenterInParent leaves gravity at centre and only sets the margins.
        assertNull(preferenceFirstTwoLineCentrePx(PreferenceTextMetrics(titleLine, noSummary), 2))
        assertNull(preferenceFirstTwoLineCentrePx(PreferenceTextMetrics(titleLine, titleLine), 2))
        assertNull(preferenceIconTopPx(PreferenceTextMetrics(titleLine, titleLine), 24, 2))
    }

    @Test
    fun aWrappedTitleAnchorsTheIconOnItsFirstTwoLines() {
        // checkFirstTwoLineCenter spends the two-line budget inside the title: the centre is the
        // bottom of the title's second line over two.
        val title = TextLineGeometry(lines = 3, firstLineBottomPx = 22, secondLineBottomPx = 44, heightPx = 66)
        val metrics = PreferenceTextMetrics(title, noSummary)
        assertEquals(22f, preferenceFirstTwoLineCentrePx(metrics, 2))
        assertEquals(10, preferenceIconTopPx(metrics, 24, 2))
    }

    @Test
    fun aSingleTitleLinePairsWithTheSummaryFirstLine() {
        val summary = TextLineGeometry(lines = 2, firstLineBottomPx = 20, secondLineBottomPx = 40, heightPx = 40)
        val metrics = PreferenceTextMetrics(titleLine, summary)
        assertEquals(22f, preferenceFirstTwoLineCentrePx(metrics, 2))
        assertEquals(10, preferenceIconTopPx(metrics, 24, 2))
    }

    @Test
    fun theIconNeverRisesAboveTheBlock() {
        val title = TextLineGeometry(lines = 3, firstLineBottomPx = 22, secondLineBottomPx = 44, heightPx = 66)
        assertEquals(0, preferenceIconTopPx(PreferenceTextMetrics(title, noSummary), 50, 2))
    }

    @Test
    fun theMarginFollowsTheLineCountLadder() {
        with(Density(1f)) {
            // coui_preference_icon_margin_top for anything outside the sized icons.
            assertEquals(14, iconVerticalMarginPx(48, 1))
            // coui_preference_24icon_margin_vertical_default, then its _top_multiline pair.
            assertEquals(12, iconVerticalMarginPx(24, 1))
            assertEquals(20, iconVerticalMarginPx(24, 2))
            // setIconTopInParent sets only the top margin, so the centred pair drops out.
            assertEquals(0, iconVerticalMarginPx(24, 3))
            // The 50dp branch ignores the line count in setIconCenterInParent.
            assertEquals(8, iconVerticalMarginPx(50, 2))
        }
    }
}
