// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
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
import io.github.suqi8.coui.kmp.layout.DialogButtonBar
import io.github.suqi8.coui.kmp.layout.DialogButtonBarAction
import io.github.suqi8.coui.kmp.overlay.OverlayBottomAssignmentDialog

@Composable
fun OverlayBottomAssignmentDialogDemo() {
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
                Card {
                    TextButton(
                        text = "Show a bottom-assignment dialog",
                        onClick = { showDialog = true },
                    )
                    // The alert-dialog panel pinned to the bottom edge, without a drag handle and
                    // without drag-to-dismiss: COUIAlertDialog.BottomAssignment.
                    OverlayBottomAssignmentDialog(
                        show = showDialog,
                        title = "Bottom assignment",
                        summary = "This panel sits on the bottom edge on every window size.",
                        onDismissRequest = { showDialog = false },
                    ) {
                        DialogButtonBar(
                            negative = DialogButtonBarAction(text = "Cancel", onClick = { showDialog = false }),
                            positive = DialogButtonBarAction(text = "Confirm", onClick = { showDialog = false }),
                        )
                    }
                }
            }
        }
    }
}
