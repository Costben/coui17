// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.NavigationBarEnterExit
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.preference.ArrowPreference

/**
 * [NavigationBarEnterExit] is the transition COUI plays when a navigation bar's menu *content* is
 * swapped (alpha plus a translate on enter, alpha-only on exit) — distinct from showing or hiding the
 * bar itself. Tapping the row swaps the menu so the transition can be watched.
 */
@Composable
fun NavigationBarEnterExitDemo() {
    var menu by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ArrowPreference(
            title = str("NavigationBarEnterExit"),
            summary = str("Enter/exit transition when the menu content is swapped"),
            onClick = {
                menu = (menu + 1) % 3
                visible = !visible
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            NavigationBarEnterExit(visible = visible) {
                Text(text = "${str("Menu")} ${menu + 1}")
            }
        }
    }
}
