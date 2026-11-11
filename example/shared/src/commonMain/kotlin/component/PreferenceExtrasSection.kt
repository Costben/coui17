// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.cardListPositionOf
import io.github.suqi8.coui.kmp.preference.ButtonPreference
import io.github.suqi8.coui.kmp.preference.MarkPreference
import io.github.suqi8.coui.kmp.preference.RecommendedItem
import io.github.suqi8.coui.kmp.preference.RecommendedPreference
import io.github.suqi8.coui.kmp.preference.SwitchLoadingPreference
import kotlinx.coroutines.delay

/** Renders the "SwitchLoadingPreference" gallery section content: one async toggle that can also be pinned to the loading state. */
@Composable
fun SwitchLoadingPreferenceDemo() {
    // SwitchLoadingPreference: simulate an asynchronous toggle that resolves after a delay.
    val loadingChecked = remember { mutableStateOf(false) }
    val loadingTarget = remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(false) }
    val alwaysLoading = remember { mutableStateOf(false) }
    LaunchedEffect(isLoading.value) {
        if (isLoading.value) {
            delay(1500)
            loadingChecked.value = loadingTarget.value
            isLoading.value = false
        }
    }

    DemoOptions(DemoToggle("Always loading", alwaysLoading))
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        SwitchLoadingPreference(
            checked = if (alwaysLoading.value) true else loadingChecked.value,
            onCheckedChange = {
                if (!alwaysLoading.value) {
                    loadingTarget.value = it
                    isLoading.value = true
                }
            },
            title = str("Async Switch"),
            summary = if (alwaysLoading.value) {
                str("A switch stuck in the loading state")
            } else {
                str("Applies the change after a short delay")
            },
            isLoading = alwaysLoading.value || isLoading.value,
            cardListPosition = CardListPosition.Full,
        )
    }
}

/**
 * Renders the "MarkPreference" gallery section content: a single-select group of mark rows.
 *
 * The "Single row" option leaves one standalone row: it is CardListPosition.Full, so both rounded
 * edges get the padding and the card is 4dp taller than a middle row of a group.
 */
@Composable
fun MarkPreferenceDemo() {
    val enabled = remember { mutableStateOf(true) }
    val singleRow = remember { mutableStateOf(false) }
    val selectedMark = remember { mutableIntStateOf(0) }
    val everyDayLabel = str("Every day")
    val workdaysOnlyLabel = str("Workdays only")
    val neverLabel = str("Never")
    val markOptions = remember(everyDayLabel, workdaysOnlyLabel, neverLabel, singleRow.value) {
        listOf(everyDayLabel, workdaysOnlyLabel, neverLabel).take(if (singleRow.value) 1 else 3)
    }
    if (selectedMark.intValue > markOptions.lastIndex) selectedMark.intValue = 0

    DemoOptions(
        DemoToggle("Enabled", enabled),
        DemoToggle("Single row", singleRow),
    )
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        markOptions.forEachIndexed { index, option ->
            MarkPreference(
                title = option,
                checked = selectedMark.intValue == index,
                onClick = { selectedMark.intValue = index },
                enabled = enabled.value,
                // Head gets 2dp on top, Tail 2dp on the bottom, Middle none, Full both.
                cardListPosition = cardListPositionOf(index, markOptions.size),
            )
        }
    }
}

/** Renders the "ButtonPreference" gallery section content: a row with an inline button, enabled from the option. */
@Composable
fun ButtonPreferenceDemo() {
    // ButtonPreference: an inline small button whose click is independent from the row.
    val enabled = remember { mutableStateOf(true) }
    val signInCount = remember { mutableIntStateOf(0) }

    DemoOptions(DemoToggle("Enabled", enabled))
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ButtonPreference(
            title = str("Account"),
            summary = if (signInCount.intValue > 0) {
                "Button clicked ${signInCount.intValue} time(s)"
            } else {
                str("Sign in to sync your data")
            },
            buttonText = str("Sign in"),
            onButtonClick = { signInCount.intValue++ },
            enabled = enabled.value,
            cardListPosition = CardListPosition.Full,
        )
    }
}

/** Renders the "RecommendedPreference" gallery section content: a rounded card of related settings entries. */
@Composable
fun RecommendedPreferenceDemo() {
    // RecommendedPreference: a standalone rounded card with related settings entries.
    val enabled = remember { mutableStateOf(true) }
    val displayAndBrightness = str("Display & brightness")
    val wallpapersAndStyle = str("Wallpapers & style")
    val batteryItem = str("Battery")
    val recommendedItems = remember(displayAndBrightness, wallpapersAndStyle, batteryItem) {
        listOf(
            RecommendedItem(text = displayAndBrightness, onClick = {}),
            RecommendedItem(text = wallpapersAndStyle, onClick = {}),
            RecommendedItem(text = batteryItem, onClick = {}),
        )
    }

    DemoOptions(DemoToggle("Enabled", enabled))
    // A disabled card greys out the header and every row and swallows all row clicks.
    RecommendedPreference(
        title = str("You might be looking for:"),
        items = recommendedItems,
        enabled = enabled.value,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    )
}
