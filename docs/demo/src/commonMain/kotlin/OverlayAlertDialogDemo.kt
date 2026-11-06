// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.layout.AlertDialogButton
import io.github.suqi8.coui.kmp.layout.AlertDialogStyle
import io.github.suqi8.coui.kmp.overlay.OverlayAlertDialog

@Composable
fun OverlayAlertDialogDemo() {
    Scaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(demoBackground()),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .padding(16.dp)
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                var showDialog by remember { mutableStateOf(false) }
                var showTinyDialog by remember { mutableStateOf(false) }

                Card {
                    TextButton(
                        text = "Show a centered alert dialog",
                        onClick = { showDialog = true },
                    )
                }
                Card {
                    TextButton(
                        text = "Show a Center.Tiny alert dialog",
                        onClick = { showTinyDialog = true },
                    )
                }

                OverlayAlertDialog(
                    show = showDialog,
                    onConfirm = { showDialog = false },
                    onCancel = { showDialog = false },
                    title = "Delete this backup?",
                    summary = "The backup is removed from the cloud and from every synced device.",
                    neutralText = "Never",
                    onNeutral = { showDialog = false },
                    recommendedButton = AlertDialogButton.Positive,
                    confirmText = "Delete",
                    cancelText = "Cancel",
                )
                OverlayAlertDialog(
                    show = showTinyDialog,
                    onConfirm = { showTinyDialog = false },
                    onCancel = { showTinyDialog = false },
                    style = AlertDialogStyle.CenterTiny,
                    title = "Turn on location?",
                    summary = "Weather needs your location to show the forecast for this area.",
                    confirmText = "Turn on",
                    cancelText = "Not now",
                )
            }
        }
    }
}
