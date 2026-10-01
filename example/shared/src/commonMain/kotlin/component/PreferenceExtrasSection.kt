// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.cardListPositionOf
import io.github.suqi8.coui.kmp.preference.ButtonPreference
import io.github.suqi8.coui.kmp.preference.MarkPreference
import io.github.suqi8.coui.kmp.preference.RecommendedItem
import io.github.suqi8.coui.kmp.preference.RecommendedPreference
import io.github.suqi8.coui.kmp.preference.SwitchLoadingPreference
import kotlinx.coroutines.delay

fun LazyListScope.preferenceExtrasSection() {
    item(key = "preferenceExtras") {
        SmallTitle(text = "SwitchLoadingPreference")
        SwitchLoadingPreferenceDemo()

        SmallTitle(text = "MarkPreference")
        MarkPreferenceDemo()

        // A standalone row is CardListPosition.Full: both rounded edges get the padding, so this
        // card is 4dp taller than a middle row of the group above.
        SmallTitle(text = "MarkPreference (standalone, Full)")
        MarkPreferenceStandaloneFullDemo()

        SmallTitle(text = "ButtonPreference")
        ButtonPreferenceDemo()

        SmallTitle(text = "RecommendedPreference")
        RecommendedPreferenceDemo()
    }
}

/** Renders the "SwitchLoadingPreference" gallery section content: an async toggle and an always loading one. */
@Composable
fun SwitchLoadingPreferenceDemo() {
    // SwitchLoadingPreference: simulate an asynchronous toggle that resolves after a delay.
    val loadingChecked = remember { mutableStateOf(false) }
    val loadingTarget = remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(false) }
    LaunchedEffect(isLoading.value) {
        if (isLoading.value) {
            delay(1500)
            loadingChecked.value = loadingTarget.value
            isLoading.value = false
        }
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        SwitchLoadingPreference(
            checked = loadingChecked.value,
            onCheckedChange = {
                loadingTarget.value = it
                isLoading.value = true
            },
            title = str("Async Switch"),
            summary = str("Applies the change after a short delay"),
            isLoading = isLoading.value,
            cardListPosition = CardListPosition.Head,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        SwitchLoadingPreference(
            checked = true,
            onCheckedChange = {},
            title = str("Always Loading"),
            summary = str("A switch stuck in the loading state"),
            isLoading = true,
            cardListPosition = CardListPosition.Tail,
        )
    }
}

/** Renders the "MarkPreference" gallery section content: a single-select group of mark rows. */
@Composable
fun MarkPreferenceDemo() {
    // MarkPreference: a single-select group where the mark follows the checked row.
    val selectedMark = remember { mutableIntStateOf(0) }
    val everyDayLabel = str("Every day")
    val workdaysOnlyLabel = str("Workdays only")
    val neverLabel = str("Never")
    val markOptions = remember(everyDayLabel, workdaysOnlyLabel, neverLabel) {
        listOf(everyDayLabel, workdaysOnlyLabel, neverLabel)
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        markOptions.forEachIndexed { index, option ->
            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            MarkPreference(
                title = option,
                checked = selectedMark.intValue == index,
                onClick = { selectedMark.intValue = index },
                // Head gets 2dp on top, Tail 2dp on the bottom, Middle none.
                cardListPosition = cardListPositionOf(index, markOptions.size),
            )
        }
    }
}

/** Renders the "MarkPreference (standalone, Full)" gallery section content: a card with a single mark row. */
@Composable
fun MarkPreferenceStandaloneFullDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        MarkPreference(
            title = str("Only option"),
            checked = true,
            onClick = {},
            cardListPosition = CardListPosition.Full,
        )
    }
}

/** Renders the "ButtonPreference" gallery section content: a row with an inline button and a disabled row. */
@Composable
fun ButtonPreferenceDemo() {
    // ButtonPreference: an inline small button whose click is independent from the row.
    val signInCount = remember { mutableIntStateOf(0) }

    Card(
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
            cardListPosition = CardListPosition.Head,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ButtonPreference(
            title = str("Disabled Row"),
            summary = str("Button and row are disabled"),
            buttonText = str("Action"),
            onButtonClick = {},
            enabled = false,
            cardListPosition = CardListPosition.Tail,
        )
    }
}

/** Renders the "RecommendedPreference" gallery section content: a rounded card of related settings entries. */
@Composable
fun RecommendedPreferenceDemo() {
    // RecommendedPreference: a standalone rounded card with related settings entries.
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

    RecommendedPreference(
        title = str("You might be looking for:"),
        items = recommendedItems,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    )
}
