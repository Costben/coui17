// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.BottomFloatingScrollToolbar
import io.github.suqi8.coui.kmp.basic.BottomFloatingScrollToolbarItem

/** The page of the Photos editor, measured from the screen. */
private val EditorPageColor = Color(0xFF121212)

private class EditorTool(val icon: ImageVector, val label: String)

/** The tools of the Photos editor's main menu, in its order. */
@Composable
private fun editorTools(): List<EditorTool> = listOf(
    EditorTool(FloatingToolbarIcons.EditorAi, str("AI edit")),
    EditorTool(FloatingToolbarIcons.EditorCrop, str("Crop & rotate")),
    EditorTool(FloatingToolbarIcons.EditorAdjust, str("Adjust")),
    EditorTool(FloatingToolbarIcons.EditorFilter, str("Photo filter")),
    EditorTool(FloatingToolbarIcons.EditorWatermark, str("Watermark")),
    EditorTool(FloatingToolbarIcons.EditorDoodle, str("Markup")),
    EditorTool(FloatingToolbarIcons.EditorText, str("Text")),
    EditorTool(FloatingToolbarIcons.EditorSticker, str("Sticker")),
    EditorTool(FloatingToolbarIcons.EditorMosaic, str("Mosaic")),
)

/** The Photos editor's scrolling bar on its dark page, with the selection following the taps. */
@Composable
fun BottomFloatingScrollToolbarDemo(modifier: Modifier = Modifier) {
    val tools = editorTools()
    var selected by remember { mutableIntStateOf(1) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(EditorPageColor),
        contentAlignment = Alignment.Center,
    ) {
        BottomFloatingScrollToolbar {
            tools.forEachIndexed { index, tool ->
                BottomFloatingScrollToolbarItem(
                    onClick = { selected = index },
                    icon = tool.icon,
                    label = tool.label,
                    selected = index == selected,
                )
            }
        }
    }
}
