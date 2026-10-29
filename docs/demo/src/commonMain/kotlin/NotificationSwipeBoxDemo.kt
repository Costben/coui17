// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.NotificationSwipeBox
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Delete
import io.github.suqi8.coui.kmp.icon.extended.Settings

@Composable
fun NotificationSwipeBoxDemo() {
    Column(Modifier.fillMaxWidth()) {
        NotificationSwipeBox(
            onSettings = {},
            onDismiss = {},
            settingsIcon = COUIIcons.Settings,
            dismissIcon = COUIIcons.Delete,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("New message")
                Text("Swipe left to reveal settings and delete")
            }
        }
        NotificationSwipeBox(
            onSettings = null,
            onDismiss = {},
            settingsIcon = null,
            dismissIcon = null,
            modifier = Modifier.padding(16.dp),
            dismissOnFullSwipe = true,
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Delete-only notification")
                Text("Swipe left to reveal delete, or all the way to delete")
            }
        }
    }
}
