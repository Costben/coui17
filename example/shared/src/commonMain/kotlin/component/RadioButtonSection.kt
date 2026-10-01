// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.preference.RadioButtonPreference

fun LazyListScope.radioButtonSection() {
    item(key = "radioButton") {
        SmallTitle(text = "RadioButton")
        RadioButtonDemo()
    }
}

/** Renders the "RadioButton" gallery section content (a group of radio preferences followed by a disabled one). */
@Composable
fun RadioButtonDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        SuperRadioButtonDemo()
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        RadioButtonPreference(
            title = str("Disabled RadioButton"),
            selected = true,
            enabled = false,
            onClick = {},
        )
    }
}

@Composable
private fun SuperRadioButtonDemo() {
    var selectedIndex by remember { mutableIntStateOf(0) }

    Column {
        RadioButtonPreference(
            title = str("Option A"),
            summary = str("Selected: {}", selectedIndex == 0),
            selected = selectedIndex == 0,
            onClick = { selectedIndex = 0 },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        RadioButtonPreference(
            title = str("Option B"),
            summary = str("Selected: {}", selectedIndex == 1),
            selected = selectedIndex == 1,
            onClick = { selectedIndex = 1 },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        RadioButtonPreference(
            title = str("Option C"),
            summary = str("Selected: {}", selectedIndex == 2),
            selected = selectedIndex == 2,
            onClick = { selectedIndex = 2 },
        )
    }
}
