// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import appstate.LocalNavigator
import component.BackNavigationIcon
import component.NotificationSwipeDemo
import i18n.str
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar

/** A reachable, scrollable notification demonstration with normal back navigation. */
@Composable
fun NotificationSwipePage(padding: PaddingValues) {
    val navigator = LocalNavigator.current
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = str("NotificationSwipeBox"),
                navigationIcon = { BackNavigationIcon(onClick = { navigator.pop() }) },
            )
        },
    ) { innerPadding ->
        Box(
            Modifier.fillMaxSize().padding(padding).padding(innerPadding).verticalScroll(rememberScrollState()),
        ) {
            NotificationSwipeDemo()
        }
    }
}
