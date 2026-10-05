// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The [ButtonSize] tier selector added in 1.2.0: [ButtonSize.Tiny] is smaller than the default
 * [ButtonSize.Regular], and explicitly passed metrics still override the selected tier.
 */
@OptIn(ExperimentalTestApi::class)
class ButtonSizeTest {

    @Test
    fun tinyTierIsShorterAndNarrowerThanRegular() = runSkikoComposeUiTest(
        size = Size(300f, 240f),
        density = Density(1f),
    ) {
        setContent { SizeTierScene() }
        waitForIdle()

        val regular = onNodeWithTag("regular").fetchSemanticsNode().boundsInRoot
        val tiny = onNodeWithTag("tiny").fetchSemanticsNode().boundsInRoot

        assertTrue(
            tiny.height < regular.height,
            "the tiny tier must be shorter than regular: regular=$regular tiny=$tiny",
        )
        assertTrue(
            tiny.width < regular.width,
            "the tiny tier must be narrower than regular: regular=$regular tiny=$tiny",
        )
    }

    @Test
    fun explicitMetricsOverrideTheSelectedTier() = runSkikoComposeUiTest(
        size = Size(300f, 240f),
        density = Density(1f),
    ) {
        setContent { OverrideScene() }
        waitForIdle()

        val overridden = onNodeWithTag("override").fetchSemanticsNode().boundsInRoot
        assertTrue(
            overridden.height >= 80f - 0.5f,
            "an explicit minHeight must win over the tiny tier: bounds=$overridden",
        )
    }
}

@Composable
private fun SizeTierScene() {
    COUITheme {
        Column(Modifier.fillMaxSize()) {
            TextButton(text = "Btn", onClick = {}, modifier = Modifier.testTag("regular"))
            TextButton(text = "Btn", onClick = {}, size = ButtonSize.Tiny, modifier = Modifier.testTag("tiny"))
        }
    }
}

@Composable
private fun OverrideScene() {
    COUITheme {
        Column(Modifier.fillMaxSize()) {
            TextButton(
                text = "Btn",
                onClick = {},
                size = ButtonSize.Tiny,
                minHeight = 80.dp,
                modifier = Modifier.testTag("override"),
            )
        }
    }
}
