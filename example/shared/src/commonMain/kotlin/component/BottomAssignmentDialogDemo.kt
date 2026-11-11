// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.layout.DialogButtonBar
import io.github.suqi8.coui.kmp.layout.DialogButtonBarAction
import io.github.suqi8.coui.kmp.overlay.OverlayBottomAssignmentDialog

/** Renders the "bottom assignment" gallery section: an alert-dialog panel pinned to the bottom edge. */
@Composable
fun BottomAssignmentDialogDemo() {
    var show by remember { mutableStateOf(false) }
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = "Show a bottom-assignment dialog",
                onClick = { show = true },
            )
        }
    }
    OverlayBottomAssignmentDialog(
        show = show,
        title = "Bottom assignment",
        summary = "This panel sits on the bottom edge on every window size.",
        onDismissRequest = { show = false },
    ) {
        DialogButtonBar(
            negative = DialogButtonBarAction(text = "Cancel", onClick = { show = false }),
            positive = DialogButtonBarAction(text = "Confirm", onClick = { show = false }),
        )
    }
}
