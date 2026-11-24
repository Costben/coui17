// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.SwipeAction
import io.github.suqi8.coui.kmp.basic.SwipeActionItem
import io.github.suqi8.coui.kmp.basic.SwipeActionMenuStyle
import io.github.suqi8.coui.kmp.basic.rememberSwipeActionItemState
import io.github.suqi8.coui.kmp.squircle.squircleSurface
import kotlinx.coroutines.launch

private val InitialFiles = listOf("Meeting notes.m4a", "Interview.m4a", "Voice memo 12.m4a", "Recording 4.m4a")

/** The pin action's fill: the theme's blue, so it reads apart from the red delete. */
private val PinColor = Color(0xFF0072F5)

/**
 * A group of sliding rows powered by upstream COUI 17 [SwipeActionItem]: supports both ColorOS 17
 * [SwipeActionMenuStyle.RoundRect] floating buttons and classic [SwipeActionMenuStyle.Rect] panels,
 * built-in delete animation, and fast over-slide delete.
 */
@Composable
fun SlideViewDemo() {
    val files = remember { mutableStateListOf(*InitialFiles.toTypedArray()) }
    var message by remember { mutableStateOf("") }
    val styleIndex = remember { mutableIntStateOf(0) }
    val secondAction = remember { mutableStateOf(true) }
    val fastDelete = remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val rowColor = CardDefaults.defaultColors().color
    val deleteLabel = str("Delete")
    val pinLabel = str("Pin")
    val menuStyle = if (styleIndex.intValue == 0) SwipeActionMenuStyle.RoundRect else SwipeActionMenuStyle.Rect

    DemoOptions(
        DemoChoice("Menu style", listOf("RoundRect", "Rect"), styleIndex),
        DemoToggle("Second action", secondAction),
        DemoToggle("Fast delete", fastDelete),
    )
    Column(Modifier.fillMaxWidth()) {
        if (menuStyle == SwipeActionMenuStyle.RoundRect) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                files.forEach { file ->
                    key(file) {
                        val state = rememberSwipeActionItemState()
                        val pinned = str("Pinned {}", file)
                        val actions = if (secondAction.value) {
                            listOf(SwipeAction(label = pinLabel, color = PinColor))
                        } else {
                            emptyList()
                        }
                        SwipeActionItem(
                            actions = actions,
                            onActionClick = {
                                message = pinned
                                scope.launch { state.close() }
                            },
                            state = state,
                            onDelete = { files.remove(file) },
                            fastDeleteEnabled = fastDelete.value,
                            deleteContentDescription = deleteLabel,
                            menuStyle = SwipeActionMenuStyle.RoundRect,
                        ) {
                            BasicComponent(
                                modifier = Modifier.squircleSurface(
                                    color = rowColor,
                                    cornerRadius = CardDefaults.CornerRadius,
                                ),
                                title = file,
                            )
                        }
                    }
                }
            }
        } else {
            Card(Modifier.padding(horizontal = 16.dp), insideMargin = PaddingValues(0.dp)) {
                files.forEachIndexed { index, file ->
                    key(file) {
                        val state = rememberSwipeActionItemState()
                        val pinned = str("Pinned {}", file)
                        val actions = if (secondAction.value) {
                            listOf(SwipeAction(label = pinLabel, color = PinColor))
                        } else {
                            emptyList()
                        }
                        SwipeActionItem(
                            actions = actions,
                            onActionClick = {
                                message = pinned
                                scope.launch { state.close() }
                            },
                            state = state,
                            onDelete = { files.remove(file) },
                            fastDeleteEnabled = fastDelete.value,
                            deleteContentDescription = deleteLabel,
                            menuStyle = SwipeActionMenuStyle.Rect,
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
        }
        if (message.isNotEmpty()) {
            SmallTitle(message)
        }
        BasicComponent(
            title = str("Reset"),
            onClick = {
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
