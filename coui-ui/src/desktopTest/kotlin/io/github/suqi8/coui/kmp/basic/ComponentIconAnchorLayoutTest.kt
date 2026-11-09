// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalTestApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertTrue

class ComponentIconAnchorLayoutTest {
    @Test
    fun aThreeLineRowAnchorsTheIconInsideTheTextBlockNotAboveIt() = runSkikoComposeUiTest(size = Size(320f, 400f), density = Density(1f)) {
        val title = "Battery optimisation"
        setContent {
            COUITheme {
                BasicComponent(
                    title = title,
                    summary = "Limits background activity for apps you rarely open so the battery lasts " +
                        "longer, and wraps onto a second and third line here",
                    startAction = { Box(Modifier.size(24.dp).background(Color.Red).testTag("icon")) },
                )
            }
        }
        waitForIdle()

        val icon = onNodeWithTag("icon").getBoundsInRoot()
        val titleBounds = onNodeWithText(title).getBoundsInRoot()
        // setIconTopInParent centres the icon on the first two lines, measured from the text block,
        // and never lets it rise above that block.
        assertTrue(icon.top >= titleBounds.top, "icon top ${icon.top} is above the title top ${titleBounds.top}")
        val iconCentre = (icon.top + icon.bottom) / 2
        assertTrue(
            iconCentre > titleBounds.bottom - 4.dp && iconCentre < titleBounds.bottom + 16.dp,
            "icon centre $iconCentre is not on the first two lines (title bottom ${titleBounds.bottom})",
        )
    }
}
