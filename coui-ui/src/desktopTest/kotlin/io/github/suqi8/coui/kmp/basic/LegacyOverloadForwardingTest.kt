// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
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
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.layout.DialogButtonBar
import io.github.suqi8.coui.kmp.layout.DialogButtonBarAction
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test

/**
 * The 1.1.0-shaped overloads kept visible for source compatibility must actually forward to the
 * primary overload.
 *
 * A visible old overload whose body calls the same function by name is re-selected by overload
 * resolution (it needs fewer default arguments than the primary), so it would call itself and
 * overflow the stack. This test composes each such shape inside a tagged wrapper and waits for idle:
 * a self-forwarding overload makes the JVM throw `StackOverflowError` before the assertions run.
 */
@OptIn(ExperimentalTestApi::class)
class LegacyOverloadForwardingTest {

    @Test
    fun oneOneZeroCallShapesRenderWithoutRecursing() = runSkikoComposeUiTest(
        size = Size(480f, 720f),
        density = Density(1f),
    ) {
        setContent { LegacyShapesScene() }
        waitForIdle()

        onNodeWithTag("legacy-button").fetchSemanticsNode()
        onNodeWithTag("legacy-navigation-bar").fetchSemanticsNode()
        onNodeWithTag("legacy-top-app-bar").fetchSemanticsNode()
        onNodeWithTag("legacy-small-top-app-bar").fetchSemanticsNode()
        onNodeWithTag("legacy-top-tips").fetchSemanticsNode()
        onNodeWithTag("legacy-dialog-button-bar").fetchSemanticsNode()
        onNodeWithTag("legacy-dropdown").fetchSemanticsNode()
    }
}

@Composable
private fun LegacyShapesScene() {
    COUITheme {
        Column(Modifier.fillMaxSize()) {
            // Button's 1.1.0 shape: a trailing content lambda and no `size` argument.
            Box(Modifier.testTag("legacy-button")) {
                Button(onClick = {}) { Text("B") }
            }
            // NavigationBar's 1.1.0 shape: no `backdrop` / `visible` argument.
            Box(Modifier.testTag("legacy-navigation-bar")) {
                NavigationBar {}
            }
            // TopAppBar / SmallTopAppBar 1.1.0 shapes: no `gradientGlass` / `centerTitle` arguments.
            Box(Modifier.testTag("legacy-top-app-bar")) {
                TopAppBar(title = "T")
            }
            Box(Modifier.testTag("legacy-small-top-app-bar")) {
                SmallTopAppBar(title = "T")
            }
            // TopTips' 1.1.0 shape: none of the title / style / content additions.
            Box(Modifier.testTag("legacy-top-tips")) {
                TopTips(text = "tip")
            }
            // DialogButtonBar's 1.1.0 shape: no `recommendColors` argument.
            Box(Modifier.testTag("legacy-dialog-button-bar")) {
                DialogButtonBar(
                    negative = null,
                    positive = DialogButtonBarAction(text = "OK", onClick = {}),
                )
            }
            // DropdownImpl's 1.1.0 shape: no `modifier` / `groupKey` / `role` arguments.
            Box(Modifier.testTag("legacy-dropdown")) {
                DropdownImpl(
                    item = DropdownItem(text = "A"),
                    optionSize = 1,
                    isSelected = false,
                    index = 0,
                    onSelectedIndexChange = {},
                )
            }
        }
    }
}
