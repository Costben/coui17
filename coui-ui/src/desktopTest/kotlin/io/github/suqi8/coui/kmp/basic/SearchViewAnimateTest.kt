// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * `COUISearchViewAnimate` geometry and state mapping.
 *
 * The native class only hides the functional button and the divider in
 * `changeStateWithOutAnimation(0)` — the search view itself stays at alpha 1, so the hint is readable
 * while the bar is collapsed. The collapse progress (`setSearchViewAnimateHeightPercent`) fades the
 * glyph and the input but is drawn by `onDraw`, so the capsule itself never turns transparent.
 *
 * At density 3.875 the 40dp wrapper is 155px, the 24dp glyph 93px and the 8dp gap 31px.
 */
@OptIn(ExperimentalTestApi::class)
class SearchViewAnimateTest {

    private val dpi = 3.875f
    private val pageWidth = 328f
    private val pageHeight = 120f

    private fun render(
        expanded: Boolean,
        query: String = "",
        collapsePercent: Float = 1f,
        type: SearchViewAnimateType = SearchViewAnimateType.NonInstantSearch,
    ): PixelMap {
        var pixels: PixelMap? = null
        runSkikoComposeUiTest(
            size = Size(pageWidth * dpi, pageHeight * dpi),
            density = Density(dpi),
        ) {
            setContent { Scene(expanded, query, collapsePercent, type) }
            waitForIdle()
            pixels = onRoot().captureToImage().toPixelMap()
        }
        return requireNotNull(pixels)
    }

    @Composable
    private fun Scene(
        expanded: Boolean,
        query: String,
        collapsePercent: Float,
        type: SearchViewAnimateType,
    ) {
        COUITheme(colors = lightColorScheme()) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                SearchViewAnimate(
                    query = query,
                    onQueryChange = {},
                    expanded = expanded,
                    onExpandedChange = {},
                    modifier = Modifier.fillMaxWidth().testTag("bar"),
                    hint = "Search settings",
                    type = type,
                    collapsePercent = collapsePercent,
                )
            }
        }
    }

    @Test
    fun collapsedBarKeepsTheHintReadable() {
        runSkikoComposeUiTest(size = Size(pageWidth * dpi, pageHeight * dpi), density = Density(dpi)) {
            setContent { Scene(expanded = false, query = "", collapsePercent = 1f, type = SearchViewAnimateType.NonInstantSearch) }
            waitForIdle()
            onNodeWithText("Search settings").assertIsDisplayed()
            onNodeWithText("Cancel").assertDoesNotExist()
        }
    }

    @Test
    fun expandedBarShowsTheFunctionalButton() {
        runSkikoComposeUiTest(size = Size(pageWidth * dpi, pageHeight * dpi), density = Density(dpi)) {
            setContent { Scene(expanded = true, query = "", collapsePercent = 1f, type = SearchViewAnimateType.NonInstantSearch) }
            waitForIdle()
            onNodeWithText("Cancel").assertIsDisplayed()
        }
    }

    @Test
    fun hintStartsAfterTheEightDpGapBehindTheGlyph() {
        runSkikoComposeUiTest(size = Size(pageWidth * dpi, pageHeight * dpi), density = Density(dpi)) {
            setContent { Scene(expanded = false, query = "", collapsePercent = 1f, type = SearchViewAnimateType.NonInstantSearch) }
            waitForIdle()
            val hint = onNodeWithText("Search settings").fetchSemanticsNode().boundsInRoot
            // 12dp icon margin + 24dp glyph + 8dp `android:layout_marginStart` on animated_search_view.
            val expected = 44f * dpi
            assertTrue(
                abs(hint.left - expected) <= 2f,
                "the hint must start at 44dp, was ${hint.left / dpi}dp",
            )
        }
    }

    @Test
    fun typingRevealsTheClearAffordance() {
        runSkikoComposeUiTest(size = Size(pageWidth * dpi, pageHeight * dpi), density = Density(dpi)) {
            setContent { Scene(expanded = true, query = "abc", collapsePercent = 1f, type = SearchViewAnimateType.NonInstantSearch) }
            waitForIdle()
            onNodeWithContentDescription(SearchViewAnimateDefaults.CLEAR_TEXT).assertIsDisplayed()
        }
    }

    @Test
    fun collapsedBarHasNoClearAffordance() {
        runSkikoComposeUiTest(size = Size(pageWidth * dpi, pageHeight * dpi), density = Density(dpi)) {
            setContent { Scene(expanded = false, query = "abc", collapsePercent = 1f, type = SearchViewAnimateType.NonInstantSearch) }
            waitForIdle()
            onNodeWithContentDescription(SearchViewAnimateDefaults.CLEAR_TEXT).assertDoesNotExist()
        }
    }

    @Test
    fun collapsePercentScalesTheWrapperHeight() {
        runSkikoComposeUiTest(size = Size(pageWidth * dpi, pageHeight * dpi), density = Density(dpi)) {
            setContent { Scene(expanded = true, query = "", collapsePercent = 1f, type = SearchViewAnimateType.NonInstantSearch) }
            waitForIdle()
            val full = onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot.height
            assertTrue(abs(full - 40f * dpi) <= 2f, "the open bar must be 40dp tall, was ${full / dpi}dp")
        }
        runSkikoComposeUiTest(size = Size(pageWidth * dpi, pageHeight * dpi), density = Density(dpi)) {
            setContent { Scene(expanded = true, query = "", collapsePercent = 0.5f, type = SearchViewAnimateType.NonInstantSearch) }
            waitForIdle()
            val half = onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot.height
            // clampSearchViewHeight(0.5) = 0.5 / 0.7 - 0.42857146 = 0.2857143, so 40dp * that.
            val expected = 40f * (0.5f / 0.7f - 0.42857146f) * dpi
            assertTrue(abs(half - expected) <= 2f, "the half-collapsed bar must be 11.4dp tall, was ${half / dpi}dp")
        }
    }

    @Test
    fun collapseKeepsTheCapsuleOpaque() {
        // At 0.4 the glyph and input alpha is already 0, but onDraw still fills the path, so the
        // capsule has to stay visible over the page.
        val percent = 0.4f
        val pixels = render(expanded = true, query = "", collapsePercent = percent)
        val x = (pageWidth * dpi / 2f).toInt()
        // translationY = animateHeight / 2 * (1 - f) pushes the 5.7dp capsule down by 15dp.
        val centerDp = 25f * (1f - percent) + (40f * (percent / 0.7f - 0.42857146f)) / 2f
        val y = (centerDp * dpi).toInt()
        val sampled = pixels[x, y]
        assertTrue(
            sampled.red < 0.95f,
            "the collapsed capsule must still be filled, sampled ${sampled.red}",
        )
    }
}
