// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.cardListPositionOf
import io.github.suqi8.coui.kmp.preference.OverlayDropdownPreference
import io.github.suqi8.coui.kmp.preference.SwitchPreference

/**
 * One row of a demo's option card.
 *
 * A demo shows a component once and lets these rows change it, instead of stacking a second copy
 * that differs by one flag. Every title and choice label is an English source key.
 */
@Stable
sealed interface DemoOption

/** An on/off option, e.g. "Enabled" or "Show icon". */
@Stable
class DemoToggle(val title: String, val state: MutableState<Boolean>) : DemoOption

/** A pick-one option for a parameter with more than two values, e.g. a size or a style. */
@Stable
class DemoChoice(val title: String, val items: List<String>, val state: MutableIntState) : DemoOption

/**
 * The option card that sits above a demo: a group of switch rows and dropdown rows.
 *
 * It owns the same 16dp inset and bottom spacing as every demo card, so it lines up with the sample
 * below it.
 */
@Composable
fun DemoOptions(vararg options: DemoOption) {
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        options.forEachIndexed { index, option ->
            val position = cardListPositionOf(index, options.size)
            when (option) {
                is DemoToggle -> SwitchPreference(
                    title = str(option.title),
                    checked = option.state.value,
                    onCheckedChange = { option.state.value = it },
                    cardListPosition = position,
                )

                is DemoChoice -> OverlayDropdownPreference(
                    title = str(option.title),
                    items = option.items.map { str(it) },
                    selectedIndex = option.state.intValue,
                    onSelectedIndexChange = { option.state.intValue = it },
                    cardListPosition = position,
                )
            }
        }
    }
}
