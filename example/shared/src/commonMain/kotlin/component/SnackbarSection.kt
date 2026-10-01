// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.SnackbarDuration
import io.github.suqi8.coui.kmp.basic.SnackbarHostState
import io.github.suqi8.coui.kmp.basic.SnackbarResult
import io.github.suqi8.coui.kmp.basic.TextButton
import kotlinx.coroutines.launch

fun LazyListScope.snackbarSection(snackbarHostState: SnackbarHostState) {
    item(key = "snackbar") {
        SmallTitle(text = "Snackbar")
        SnackbarDemo(snackbarHostState)
    }
}

/** Renders the "Snackbar" gallery section content (buttons that dismiss and show snackbars of every duration). */
@Composable
fun SnackbarDemo(snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    val shortMessage = str("This is a short message")
    val longMessage = str("This is a long message to display more text content")
    val customMessage = str("This message will last for 2 seconds")
    val actionMessage = str("This message has an action")
    val actionAlive = str("Action: Alive")
    val actionUndo = str("Action: Undo")
    val actionExpired = str("Action: Expired")
    val undoLabel = str("Undo")
    val dismissibleMessage = str("This message can be removed via the close button")
    val indefiniteMessage = str("Indefinite message, dismiss manually")
    val actionText = str("Action")
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    text = str("Dismiss oldest"),
                    onClick = {
                        scope.launch {
                            snackbarHostState.oldestSnackbarData()?.dismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = str("Dismiss newest"),
                    onClick = {
                        scope.launch {
                            snackbarHostState.newestSnackbarData()?.dismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    text = str("Short (4s)"),
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(shortMessage)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = str("Long (10s)"),
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = longMessage,
                                duration = SnackbarDuration.Long,
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    text = str("Custom (2s)"),
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = customMessage,
                                duration = SnackbarDuration.Custom(2000L),
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                var text by remember { mutableStateOf(actionText) }
                TextButton(
                    text = text,
                    onClick = {
                        scope.launch {
                            text = actionAlive
                            val result = snackbarHostState.showSnackbar(
                                message = actionMessage,
                                actionLabel = undoLabel,
                                duration = SnackbarDuration.Short,
                            )
                            text = when (result) {
                                SnackbarResult.ActionPerformed -> actionUndo
                                else -> actionExpired
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    text = str("Dismissible"),
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = dismissibleMessage,
                                withDismissAction = true,
                                duration = SnackbarDuration.Long,
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = str("Indefinite"),
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = indefiniteMessage,
                                withDismissAction = true,
                                duration = SnackbarDuration.Indefinite,
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
