// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test

/**
 * The horizontal progress dialog must expose a determinate bar while [OverlayProgressDialog.progress]
 * is set and an indefinite bar when it is null, and it must keep the optional message.
 */
@OptIn(ExperimentalTestApi::class)
class ProgressDialogTest {

    @Test
    fun theDeterminateBarTracksTheProgressValue() = runSkikoComposeUiTest(
        size = Size(400f, 600f),
        density = Density(1f),
    ) {
        var progress by mutableFloatStateOf(0.25f)
        setContent { ProgressDialogHarness(progress = { progress }) }
        waitForIdle()

        onNodeWithText("Downloading").assertExists()
        progressBar().assertRangeInfoEquals(ProgressBarRangeInfo(0.25f, 0f..1f))

        progress = 0.75f
        waitForIdle()
        progressBar().assertRangeInfoEquals(ProgressBarRangeInfo(0.75f, 0f..1f))
    }

    @Test
    fun aNullProgressTurnsTheSameBarIndeterminate() = runSkikoComposeUiTest(
        size = Size(400f, 600f),
        density = Density(1f),
    ) {
        var value by mutableFloatStateOf(1f)
        var indeterminate by mutableStateOf(false)
        setContent { ProgressDialogHarness(progress = { if (indeterminate) null else value }) }
        waitForIdle()
        progressBar().assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f))

        // The indefinite bar runs an endless animation, so stop the clock before it mounts: the
        // test clock must not keep advancing on its behalf while the semantics are read.
        mainClock.autoAdvance = false
        indeterminate = true
        mainClock.advanceTimeByFrame()
        progressBar().assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @Test
    fun theMessageIsOptional() = runSkikoComposeUiTest(
        size = Size(400f, 600f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                Scaffold {
                    Box(Modifier.fillMaxSize()) {
                        OverlayProgressDialog(show = true, progress = 0.5f)
                    }
                }
            }
        }
        waitForIdle()
        progressBar().assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
    }
}

/** The single node carrying the bar semantics, which the dialog gets from `LinearProgressIndicator`. */
private fun SemanticsNodeInteractionsProvider.progressBar(): SemanticsNodeInteraction = onNode(
    SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
    useUnmergedTree = true,
)

@Composable
private fun ProgressDialogHarness(progress: () -> Float?) {
    COUITheme {
        Scaffold {
            Box(Modifier.fillMaxSize()) {
                OverlayProgressDialog(
                    show = true,
                    progress = progress(),
                    text = "Downloading",
                )
            }
        }
    }
}
