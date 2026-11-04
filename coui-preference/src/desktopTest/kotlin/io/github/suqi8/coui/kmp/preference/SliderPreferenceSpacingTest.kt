// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A slider preference stacks its track directly under the title. The track is centred in a 36dp
 * touch target, so the drawn capsule stays 8dp below the text — the gap ColorOS 17's Settings
 * slider shows. Adding the row's bottom inset as well used to push the track 40dp away.
 */
@OptIn(ExperimentalTestApi::class)
class SliderPreferenceSpacingTest {

    @Test
    fun theTrackStartsWhereTheTitleBlockEnds() = runSkikoComposeUiTest(
        size = Size(400f, 600f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                Card(modifier = Modifier.fillMaxWidth()) {
                    SliderPreference(
                        value = 0.5f,
                        onValueChange = {},
                        title = "Blur",
                        valueText = "100",
                        modifier = Modifier.testTag("row"),
                    )
                }
            }
        }
        waitForIdle()

        val row = onNodeWithTag("row").getBoundsInRoot()
        val title = onNodeWithText("Blur").getBoundsInRoot()
        val slider = onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
        ).getBoundsInRoot()

        assertEquals(0.0, (slider.top - title.bottom).value.toDouble(), 0.5)
        assertEquals(36.0, (slider.bottom - slider.top).value.toDouble(), 0.5)
        // 10dp top inset + a 19dp title line + the 36dp slider + the 10dp bottom inset.
        assertEquals(75.0, (row.bottom - row.top).value.toDouble(), 1.0)
    }
}
