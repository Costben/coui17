// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.overlay

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.layout.DialogDefaults
import io.github.suqi8.coui.kmp.layout.ProgressDialogColors
import io.github.suqi8.coui.kmp.layout.ProgressDialogContentLayout
import io.github.suqi8.coui.kmp.layout.ProgressDialogDefaults
import io.github.suqi8.coui.kmp.utils.COUIPopupUtils.Companion.DialogLayout

/**
 * A small always-centered dialog with a deterministic horizontal progress bar and an optional
 * message, matching the COUI horizontal progress dialog
 * (COUIRotatingDialogBuilder / `coui_progress_dialog_horizontal.xml`).
 *
 * The bar is determinate while [progress] is a value between 0.0f and 1.0f, and indefinite when
 * [progress] is null — the horizontal sibling of [OverlayLoadingDialog]'s rotating spinner.
 *
 * New in 1.2.0.
 *
 * @param show Whether the [OverlayProgressDialog] is shown.
 * @param modifier The modifier to be applied to the [OverlayProgressDialog].
 * @param progress The current progress between 0.0f and 1.0f, or null for an indefinite bar.
 * @param text The optional message shown below the bar.
 * @param colors The [ProgressDialogColors] of the message and the bar.
 * @param backgroundColor The background color of the dialog card.
 * @param enableWindowDim Whether to enable window dimming when the dialog is shown.
 * @param onDismissRequest Will be called when the user tries to dismiss the dialog by clicking
 *   outside or pressing the back button. When null (default), the dialog cannot be dismissed
 *   by the user.
 * @param onDismissFinished The callback when the [OverlayProgressDialog] is completely dismissed.
 * @param renderInRootScaffold Whether to render the dialog in the root (outermost) Scaffold.
 */
@OptIn(ExperimentalCOUIApi::class)
@ExperimentalCOUIApi
@Composable
fun OverlayProgressDialog(
    show: Boolean,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    text: String? = null,
    colors: ProgressDialogColors = ProgressDialogDefaults.progressDialogColors(),
    backgroundColor: Color = DialogDefaults.backgroundColor(),
    enableWindowDim: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    renderInRootScaffold: Boolean = true,
) {
    ProgressDialogContentLayout(
        show = show,
        popupHost = { visible, hostContent ->
            val visibleState = remember { mutableStateOf(false) }
            visibleState.value = visible
            DialogLayout(
                visible = visibleState,
                enableWindowDim = false,
                enterTransition = EnterTransition.None,
                exitTransition = ExitTransition.None,
                enableAutoLargeScreen = false,
                renderInRootScaffold = renderInRootScaffold,
            ) {
                hostContent()
            }
        },
        modifier = modifier,
        progress = progress,
        text = text,
        colors = colors,
        backgroundColor = backgroundColor,
        enableWindowDim = enableWindowDim,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
    )
}
