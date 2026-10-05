// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The dismiss affordance added in 1.2.0 is an independent [androidx.compose.ui.semantics.Role]
 * `Button` target: it fires [Chip]'s `onDismiss` without toggling the chip's selection and is
 * disabled together with the chip.
 */
@OptIn(ExperimentalTestApi::class)
class ChipDismissTest {

    @Test
    fun dismissRunsItsOwnCallbackWithoutTogglingSelection() = runSkikoComposeUiTest(
        size = Size(320f, 160f),
        density = Density(1f),
    ) {
        var selected = false
        var dismissed = 0
        setContent {
            COUITheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Chip(
                        selected = selected,
                        onClick = { selected = true },
                        label = "Tag",
                        onDismiss = { dismissed++ },
                    )
                }
            }
        }
        waitForIdle()

        onNodeWithContentDescription(ChipDefaults.DISMISS_CONTENT_DESCRIPTION).performClick()
        waitForIdle()

        assertEquals(1, dismissed)
        assertFalse(selected, "the dismiss button must not toggle the chip selection")
    }

    @Test
    fun aDisabledChipDisablesTheDismissAffordance() = runSkikoComposeUiTest(
        size = Size(320f, 160f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Chip(
                        selected = false,
                        onClick = {},
                        label = "Disabled",
                        enabled = false,
                        onDismiss = {},
                    )
                }
            }
        }
        waitForIdle()

        onNodeWithContentDescription(ChipDefaults.DISMISS_CONTENT_DESCRIPTION).assertIsNotEnabled()
    }

    @Test
    fun noDismissAffordanceIsExposedWhenOnDismissIsNull() = runSkikoComposeUiTest(
        size = Size(320f, 160f),
        density = Density(1f),
    ) {
        setContent { PlainChip() }
        waitForIdle()

        onNodeWithContentDescription(ChipDefaults.DISMISS_CONTENT_DESCRIPTION).assertDoesNotExist()
    }
}

@Composable
private fun PlainChip() {
    COUITheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Chip(selected = false, onClick = {}, label = "Tag")
        }
    }
}
