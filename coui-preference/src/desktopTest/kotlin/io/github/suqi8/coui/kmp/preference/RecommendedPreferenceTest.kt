// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * `RecommendedPreference.enabled` must gate every `RecommendedItem.onClick` and report the state to
 * accessibility services, so a row can be asserted as enabled or disabled.
 */
@OptIn(ExperimentalTestApi::class)
class RecommendedPreferenceTest {

    @Test
    fun anEnabledCardKeepsEveryRowClickable() = runSkikoComposeUiTest(
        size = Size(400f, 400f),
        density = Density(1f),
    ) {
        var clicks = 0
        setContent { RecommendedPreferenceHarness(enabled = true, onItemClick = { clicks++ }) }
        waitForIdle()

        onNodeWithText("Battery").assertIsEnabled().performClick()
        assertEquals(1, clicks)
        onNodeWithText("Wallpapers & style").assertIsEnabled().performClick()
        assertEquals(2, clicks)
    }

    @Test
    fun aDisabledCardSuppressesEveryRowClick() = runSkikoComposeUiTest(
        size = Size(400f, 400f),
        density = Density(1f),
    ) {
        var clicks = 0
        setContent { RecommendedPreferenceHarness(enabled = false, onItemClick = { clicks++ }) }
        waitForIdle()

        onNodeWithText("Battery").assertIsNotEnabled().performClick()
        onNodeWithText("Wallpapers & style").assertIsNotEnabled().performClick()
        assertEquals(0, clicks, "a disabled card must swallow every row click")
    }

    @Test
    fun aDisabledRowWithoutAHandlerStillReportsItsState() = runSkikoComposeUiTest(
        size = Size(400f, 400f),
        density = Density(1f),
    ) {
        setContent { RecommendedPreferenceHarness(enabled = false, onItemClick = null) }
        waitForIdle()

        onNodeWithText("Battery").assertIsNotEnabled()
    }

    @Test
    fun theDisabledColorsDifferFromTheEnabledOnes() = runSkikoComposeUiTest(
        size = Size(400f, 400f),
        density = Density(1f),
    ) {
        var colors: RecommendedPreferenceColors? = null
        setContent {
            COUITheme {
                colors = RecommendedPreferenceDefaults.recommendedPreferenceColors()
            }
        }
        waitForIdle()

        val resolved = requireNotNull(colors)
        assertNotEquals(resolved.titleColor, resolved.disabledTitleColor)
        assertNotEquals(resolved.itemColor, resolved.disabledItemColor)
    }
}

@Composable
private fun RecommendedPreferenceHarness(
    enabled: Boolean,
    onItemClick: (() -> Unit)?,
) {
    val items = remember(onItemClick) {
        listOf(
            RecommendedItem(text = "Battery", onClick = onItemClick),
            RecommendedItem(text = "Wallpapers & style", onClick = onItemClick),
        )
    }
    COUITheme {
        RecommendedPreference(
            title = "You might be looking for:",
            items = items,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
    }
}
