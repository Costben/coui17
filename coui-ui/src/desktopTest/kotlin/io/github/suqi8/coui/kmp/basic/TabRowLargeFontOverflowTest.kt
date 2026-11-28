// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.kit.segmentbutton.CouiSegmentedControlDefaults
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifies large-font behavior for [TabRowWithContour], `CommonSegmentButtonRow`, and
 * `SegmentedControl`:
 * - [CouiSegmentedControlDefaults.textStyle] caps font size at 1.35x using `LocalDensity.fontScale`.
 * - Overflowing natural tab widths switch to horizontal scrolling instead of squeezing tabs into
 *   single-character ellipsis even when `availableWidth / count >= minWidth`.
 */
@OptIn(ExperimentalTestApi::class)
class TabRowLargeFontOverflowTest {

    @Test
    fun computeSegmentWidthsKeepsNaturalWidthsOnOverflowEvenAboveMinWidth() {
        // 6 tabs needing 64px each (384px total) inside 330px: 330 / 6 = 55px >= 44px minWidth,
        // yet squeezing them to 55px would truncate every label. Must preserve natural widths.
        val natural = listOf(64, 64, 64, 64, 64, 64)
        val widths = computeSegmentWidths(
            naturalWidths = natural,
            availablePx = 330f,
            minWidthPx = 44,
            maxWidthPx = Int.MAX_VALUE,
        )
        assertEquals(natural, widths)
    }

    @Test
    fun computeSegmentWidthsRespectsMaxWidthWhenScrolling() {
        val widths = computeSegmentWidths(
            naturalWidths = listOf(120, 120, 120, 120),
            availablePx = 300f,
            minWidthPx = 44,
            maxWidthPx = 90,
        )
        assertEquals(listOf(90, 90, 90, 90), widths)
    }

    @Test
    fun computeSegmentWidthsDistributesWhenAllTabsFit() {
        val widths = computeSegmentWidths(
            naturalWidths = listOf(50, 50, 50),
            availablePx = 240f,
            minWidthPx = 44,
            maxWidthPx = Int.MAX_VALUE,
        )
        assertEquals(listOf(80, 80, 80), widths)
    }

    @Test
    fun segmentedControlTextStyleCapsLocalDensityFontScaleAt135x() {
        var normalStyle: TextStyle? = null
        var largeStyle: TextStyle? = null
        val normalDensity = Density(density = 2f, fontScale = 1.0f)
        val largeDensity = Density(density = 2f, fontScale = 2.0f)

        runSkikoComposeUiTest(size = Size(300f, 100f), density = normalDensity) {
            setContent {
                COUITheme(colors = lightColorScheme()) {
                    normalStyle = CouiSegmentedControlDefaults.textStyle()
                    CompositionLocalProvider(LocalDensity provides largeDensity) {
                        largeStyle = CouiSegmentedControlDefaults.textStyle()
                    }
                }
            }
            waitForIdle()
        }

        val normalPx = with(normalDensity) { assertNotNull(normalStyle).fontSize.toPx() }
        val largePx = with(largeDensity) { assertNotNull(largeStyle).fontSize.toPx() }
        assertEquals(28f, normalPx, 0.5f)
        // 14dp * 2px/dp * 1.35 = 37.8px (NOT 14 * 2 * 2.0 = 56px).
        assertEquals(37.8f, largePx, 0.5f)
    }

    @Test
    fun commonSegmentButtonRowScrollsInsteadOfSqueezingUnderLargeFontScale() {
        val tabs = listOf("Discover", "Moment", "HotList", "Topics", "Digital", "Gallery")
        val largeDensity = Density(density = 1f, fontScale = 1.8f)
        var capturedListState: LazyListState? = null

        runSkikoComposeUiTest(size = Size(360f, 100f), density = largeDensity) {
            setContent {
                COUITheme(colors = lightColorScheme()) {
                    val listState = rememberLazyListState()
                    capturedListState = listState
                    Box(Modifier.fillMaxSize()) {
                        TabRowWithContour(
                            tabs = tabs,
                            selectedTabIndex = 0,
                            onTabSelected = {},
                            minWidth = 44.dp,
                            segmentPadding = 8.dp,
                            listState = listState,
                            modifier = Modifier.width(320.dp),
                            tabModifier = { index -> Modifier.testTag("common_tab_$index") },
                        )
                    }
                }
            }
            waitForIdle()

            // Fair share would be (320 - 8) / 6 = 52dp; with natural widths preserved, the first
            // tab must keep its full natural width (> 52dp) and the LazyRow must be scrollable.
            val firstBounds = onNodeWithTag("common_tab_0").getUnclippedBoundsInRoot()
            val firstWidthDp = firstBounds.right - firstBounds.left
            assertTrue(
                firstWidthDp > 60.dp,
                "First tab must keep natural width instead of squeezing to ~52dp, got $firstWidthDp",
            )
            assertTrue(
                assertNotNull(capturedListState).canScrollForward,
                "Overflowing CommonSegmentButtonRow must scroll horizontally",
            )
        }
    }

    @Test
    fun segmentedControlScrollsInsteadOfSqueezingUnderLargeFontScale() {
        val tabs = listOf("Discover", "Moment", "HotList", "Topics", "Digital", "Gallery")
        val largeDensity = Density(density = 1f, fontScale = 1.8f)

        runSkikoComposeUiTest(size = Size(360f, 100f), density = largeDensity) {
            setContent {
                COUITheme(colors = lightColorScheme()) {
                    Box(Modifier.fillMaxSize()) {
                        TabRowWithContour(
                            tabs = tabs,
                            selectedTabIndex = 0,
                            onTabSelected = {},
                            minWidth = 44.dp,
                            segmentPadding = 8.dp,
                            modifier = Modifier.width(320.dp),
                            tabModifier = { index -> Modifier.testTag("seg_tab_$index") },
                        )
                    }
                }
            }
            waitForIdle()

            // Fair share would be (320 - 8) / 6 = 52dp; all 6 tabs must keep natural widths and
            // exceed the 320dp container width so the control scrolls without single-char ellipsis.
            var totalWidthDp = 0.dp
            for (i in tabs.indices) {
                val bounds = onNodeWithTag("seg_tab_$i").getUnclippedBoundsInRoot()
                val widthDp = bounds.right - bounds.left
                assertTrue(
                    widthDp > 60.dp,
                    "Segment $i ('${tabs[i]}') must keep natural width instead of squeezing, got $widthDp",
                )
                totalWidthDp += widthDp
            }
            assertTrue(
                totalWidthDp > 320.dp,
                "Total natural width of 6 tabs under 1.8x fontScale should exceed 320dp and scroll, got $totalWidthDp",
            )
        }
    }
}
