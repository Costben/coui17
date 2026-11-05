// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ROOT_TAG = "overscroll-root"
private const val CONTENT_TAG = "overscroll-content"

/**
 * The overscroll translation is a layer property, not a placement: dragging past the edge moves the
 * content without measuring, laying it out, or re-recording it.
 *
 * This pins the contract the implementation relies on rather than separating it from the placement
 * form it replaced — both end up translating the same layer, so the difference (a placement pass per
 * offset change) is not visible from the outside. What it does catch is the layer never receiving
 * the offset at all, which is how the `updateLayerBlock` switch fails when the block it refreshes is
 * not the one the child was placed with: the drag then goes nowhere.
 */
@OptIn(ExperimentalTestApi::class)
class OverscrollLayerTest {

    @Test
    fun theDragMovesTheContentWithoutLayingItOutAgain() = runSkikoComposeUiTest(
        size = Size(400f, 400f),
        density = Density(1f),
    ) {
        var measures = 0
        var placements = 0
        var draws = 0
        setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .overScrollVertical(isEnabled = { true })
                    .testTag(ROOT_TAG),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .layout { measurable, constraints ->
                            measures++
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placements++
                                placeable.place(0, 0)
                            }
                        }
                        .drawWithContent {
                            draws++
                            drawContent()
                        }
                        .testTag(CONTENT_TAG),
                ) {
                    repeat(20) { Box(Modifier.fillMaxWidth().height(48.dp)) }
                }
            }
        }
        waitForIdle()

        val before = onNodeWithTag(CONTENT_TAG).fetchSemanticsNode().positionInRoot
        val measuresBefore = measures
        val placementsBefore = placements
        val drawsBefore = draws

        // Hold the content past the top edge. The finger stays down so the rebound does not start
        // and the content stays where the drag put it.
        onNodeWithTag(ROOT_TAG).performTouchInput {
            down(center)
            repeat(5) { moveBy(Offset(0f, 60f)) }
        }
        waitForIdle()

        val moved = onNodeWithTag(CONTENT_TAG).fetchSemanticsNode().positionInRoot.y - before.y
        assertTrue(moved > 20f, "the content has to follow the drag past the top edge, moved $moved px")
        assertEquals(measuresBefore, measures, "the overscroll must not measure the content again")
        assertEquals(placementsBefore, placements, "the overscroll must not lay the content out again")
        // The layer is moved, not its content. Re-recording it on every offset change is what made
        // the drag cost 7ms of draw on the reference device against 3.5ms without the effect.
        assertTrue(draws - drawsBefore <= 1, "the overscroll must move the layer, not redraw the content, redrew ${draws - drawsBefore} times")
    }
}
