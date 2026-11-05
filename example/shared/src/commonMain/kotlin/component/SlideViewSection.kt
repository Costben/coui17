// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.SlideMenuItem
import io.github.suqi8.coui.kmp.basic.SlideView
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.rememberSlideViewState
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Delete
import kotlinx.coroutines.launch

private val InitialReminders = listOf("3 hours before", "4 hours before", "6 hours before", "3 days before")
private val InitialFiles = listOf("Meeting notes.m4a", "Interview.m4a", "Voice memo 12.m4a")

/** The pin action's fill: the theme's blue, so it reads apart from the red delete. */
private val PinColor = Color(0xFF0072F5)

/**
 * Two groups of sliding rows. The first is ColorOS 17 Calendar's custom reminder list: one delete
 * action, deleted with [io.github.suqi8.coui.kmp.basic.SlideViewState.delete] after a tap. The second
 * adds a second action and fast delete, so sliding far past the menu deletes the row by itself.
 */
@Composable
fun SlideViewDemo() {
    val reminders = remember { mutableStateListOf(*InitialReminders.toTypedArray()) }
    val files = remember { mutableStateListOf(*InitialFiles.toTypedArray()) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val rowColor = CardDefaults.defaultColors().color
    val deleteLabel = str("Delete")

    Column(Modifier.fillMaxWidth()) {
        SmallTitle(str("Custom reminders (tap Delete)"))
        Card(Modifier.padding(horizontal = 12.dp), insideMargin = PaddingValues(0.dp)) {
            reminders.forEachIndexed { index, reminder ->
                key(reminder) {
                    val state = rememberSlideViewState()
                    SlideView(
                        menuItems = listOf(SlideMenuItem(text = null, icon = COUIIcons.Delete, contentDescription = deleteLabel)),
                        onMenuItemClick = {
                            scope.launch {
                                state.delete()
                                reminders.remove(reminder)
                            }
                        },
                        state = state,
                    ) {
                        BasicComponent(
                            modifier = Modifier.background(rowColor),
                            title = str(reminder),
                            cardListPosition = positionOf(index, reminders.size),
                            showDivider = index != reminders.lastIndex,
                        )
                    }
                }
            }
        }
        SmallTitle(str("Recordings (slide far to delete)"))
        Card(Modifier.padding(horizontal = 12.dp), insideMargin = PaddingValues(0.dp)) {
            files.forEachIndexed { index, file ->
                key(file) {
                    val state = rememberSlideViewState()
                    val pinned = str("Pinned {}", file)
                    SlideView(
                        menuItems = listOf(
                            SlideMenuItem(text = null, icon = COUIIcons.Delete, contentDescription = deleteLabel),
                            SlideMenuItem(str("Pin"), color = PinColor),
                        ),
                        onMenuItemClick = { action ->
                            if (action == 0) {
                                scope.launch {
                                    state.delete()
                                    files.remove(file)
                                }
                            } else {
                                message = pinned
                                scope.launch { state.close() }
                            }
                        },
                        state = state,
                        fastDeleteEnabled = true,
                        onFastDelete = { files.remove(file) },
                    ) {
                        BasicComponent(
                            modifier = Modifier.background(rowColor),
                            title = file,
                            cardListPosition = positionOf(index, files.size),
                            showDivider = index != files.lastIndex,
                        )
                    }
                }
            }
        }
        if (message.isNotEmpty()) {
            SmallTitle(message)
        }
        BasicComponent(
            title = str("Reset"),
            onClick = {
                reminders.clear()
                reminders.addAll(InitialReminders)
                files.clear()
                files.addAll(InitialFiles)
                message = ""
            },
        )
    }
}

private fun positionOf(index: Int, size: Int): CardListPosition = when {
    size == 1 -> CardListPosition.Full
    index == 0 -> CardListPosition.Head
    index == size - 1 -> CardListPosition.Tail
    else -> CardListPosition.Middle
}
