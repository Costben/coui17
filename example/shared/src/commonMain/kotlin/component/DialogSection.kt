// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.layout.DialogButtonBar
import io.github.suqi8.coui.kmp.layout.DialogButtonBarAction
import io.github.suqi8.coui.kmp.layout.DialogDefaults
import io.github.suqi8.coui.kmp.overlay.OverlayAlertDialog
import io.github.suqi8.coui.kmp.overlay.OverlayDialog
import io.github.suqi8.coui.kmp.overlay.OverlayLoadingDialog
import io.github.suqi8.coui.kmp.overlay.OverlayProgressDialog
import io.github.suqi8.coui.kmp.overlay.OverlaySecurityDialog
import io.github.suqi8.coui.kmp.preference.ArrowPreference
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalDismissState
import io.github.suqi8.coui.kmp.window.WindowDialog
import kotlinx.coroutines.delay

/** Renders the "Dialog" gallery section content (the dialog trigger card and every dialog demo). */
@Composable
fun DialogDemo() {
    var showSuperDialog by remember { mutableStateOf(false) }
    var showWindowDialog by remember { mutableStateOf(false) }
    var superDialogHoldDown by remember { mutableStateOf(false) }
    var windowDialogHoldDown by remember { mutableStateOf(false) }
    var showLoadingDialog by remember { mutableStateOf(false) }
    var loadingDialogHoldDown by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var securityDialogHoldDown by remember { mutableStateOf(false) }
    var showStackedDialog by remember { mutableStateOf(false) }
    var stackedDialogHoldDown by remember { mutableStateOf(false) }
    var showRecommendedDialog by remember { mutableStateOf(false) }
    var recommendedDialogHoldDown by remember { mutableStateOf(false) }
    var showAlertDialog by remember { mutableStateOf(false) }
    var alertDialogHoldDown by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressDialogHoldDown by remember { mutableStateOf(false) }
    var progressDialogValue by remember { mutableStateOf<Float?>(null) }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ArrowPreference(
            title = str("Dialog (O)"),
            summary = str("Click to show an OverlayDialog"),
            onClick = {
                showSuperDialog = true
                superDialogHoldDown = true
            },
            holdDownState = superDialogHoldDown,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("Dialog (W)"),
            summary = str("Click to show a WindowDialog"),
            onClick = {
                showWindowDialog = true
                windowDialogHoldDown = true
            },
            holdDownState = windowDialogHoldDown,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("Loading Dialog (O)"),
            summary = str("Click to show an OverlayLoadingDialog"),
            onClick = {
                showLoadingDialog = true
                loadingDialogHoldDown = true
            },
            holdDownState = loadingDialogHoldDown,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("Stacked Button Bar (O)"),
            summary = str("Long labels flip the bar to a vertical stack"),
            onClick = {
                showStackedDialog = true
                stackedDialogHoldDown = true
            },
            holdDownState = stackedDialogHoldDown,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("Recommended Button Bar (O)"),
            summary = str("One action is highlighted as the recommend button"),
            onClick = {
                showRecommendedDialog = true
                recommendedDialogHoldDown = true
            },
            holdDownState = recommendedDialogHoldDown,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("Alert Dialog (O)"),
            summary = str("Centered warning dialog with a confirm / cancel row"),
            onClick = {
                showAlertDialog = true
                alertDialogHoldDown = true
            },
            holdDownState = alertDialogHoldDown,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("Progress Dialog (O)"),
            summary = str("Horizontal progress dialog, indefinite then determinate"),
            onClick = {
                showProgressDialog = true
                progressDialogHoldDown = true
            },
            holdDownState = progressDialogHoldDown,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("Security Dialog (O)"),
            summary = str("Click to show an OverlaySecurityDialog"),
            onClick = {
                showSecurityDialog = true
                securityDialogHoldDown = true
            },
            holdDownState = securityDialogHoldDown,
        )
    }

    SuperDialogDemo(
        show = showSuperDialog,
        onDismissRequest = { showSuperDialog = false },
        onDismissFinished = { superDialogHoldDown = false },
    )
    WindowDialogDemo(
        show = showWindowDialog,
        onDismissRequest = { showWindowDialog = false },
        onDismissFinished = { windowDialogHoldDown = false },
    )
    StackedButtonBarDialogDemo(
        show = showStackedDialog,
        onDismissRequest = { showStackedDialog = false },
        onDismissFinished = { stackedDialogHoldDown = false },
    )
    RecommendedButtonBarDialogDemo(
        show = showRecommendedDialog,
        onDismissRequest = { showRecommendedDialog = false },
        onDismissFinished = { recommendedDialogHoldDown = false },
    )

    // Auto-dismiss the loading demo after a short delay, like a finished task would.
    LaunchedEffect(showLoadingDialog) {
        if (showLoadingDialog) {
            delay(3000)
            showLoadingDialog = false
        }
    }
    OverlayLoadingDialog(
        show = showLoadingDialog,
        text = str("Loading..."),
        onDismissRequest = { showLoadingDialog = false },
        onDismissFinished = { loadingDialogHoldDown = false },
    )

    OverlaySecurityDialog(
        show = showSecurityDialog,
        title = str("Security Notice"),
        summary = str("This feature needs to connect to the network."),
        statement = str("Tap and view Privacy Policy for more information."),
        statementLinkText = str("Privacy Policy"),
        onLinkClick = { /* Open the privacy policy */ },
        onConfirm = { _ -> showSecurityDialog = false },
        onCancel = { showSecurityDialog = false },
        onDismissFinished = { securityDialogHoldDown = false },
    )

    AlertDialogDemo(
        show = showAlertDialog,
        onDismissRequest = { showAlertDialog = false },
        onDismissFinished = { alertDialogHoldDown = false },
    )

    // Walk the horizontal progress dialog through both of its modes: indefinite first, then
    // determinate, like a download that only learns its size once the request is accepted.
    LaunchedEffect(showProgressDialog) {
        if (showProgressDialog) {
            progressDialogValue = null
            delay(1200)
            repeat(40) { step ->
                progressDialogValue = step / 40f
                delay(60)
            }
            progressDialogValue = 1f
            delay(400)
            showProgressDialog = false
        }
    }
    OverlayProgressDialog(
        show = showProgressDialog,
        progress = progressDialogValue,
        text = if (progressDialogValue == null) {
            str("Connecting...")
        } else {
            str("Downloading...")
        },
        onDismissRequest = { showProgressDialog = false },
        onDismissFinished = { progressDialogHoldDown = false },
    )
}

/**
 * Shows the centered alert variant: the panel is always centered (unlike [OverlayDialog]'s
 * bottom-anchored presentation on compact windows) and closes through a cancel / confirm row.
 */
@Composable
private fun AlertDialogDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    OverlayAlertDialog(
        show = show,
        onConfirm = onDismissRequest,
        onCancel = onDismissRequest,
        title = str("Delete this backup?"),
        summary = str("The backup will be removed from the cloud and from every synced device."),
        confirmText = str("Delete"),
        cancelText = str("Cancel"),
        onDismissFinished = onDismissFinished,
        content = {
            // The content slot spans the panel edge to edge; pad it like the built-in message.
            Text(
                text = str("This action cannot be undone."),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DialogDefaults.insideMargin.width),
                color = COUITheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                style = COUITheme.textStyles.body2,
            )
        },
    )
}

/**
 * Shows the automatic vertical stacking of [DialogButtonBar]: the labels are wide enough that
 * COUIButtonBarLayout's needSetButVertical check fails, so the whole bar flips to a stack.
 */
@Composable
private fun StackedButtonBarDialogDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    OverlayDialog(
        show = show,
        title = str("Delete Backup"),
        summary = str("The button labels below do not fit side by side, so the bar stacks them."),
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        content = {
            DialogButtonBar(
                negative = DialogButtonBarAction(text = str("Not Now"), onClick = onDismissRequest),
                positive = DialogButtonBarAction(
                    text = str("Delete Backup And Local Copies"),
                    onClick = onDismissRequest,
                ),
            )
        },
    )
}

@Composable
private fun SuperDialogDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    OverlayDialog(
        show = show,
        title = str("Dialog (O)"),
        summary = str("A dialog component inside COUIPopupHost."),
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        content = {
            DialogButtonBar(
                negative = DialogButtonBarAction(text = str("Cancel"), onClick = onDismissRequest),
                positive = DialogButtonBarAction(text = str("Confirm"), onClick = onDismissRequest),
            )
        },
    )
}

@Composable
private fun WindowDialogDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    WindowDialog(
        show = show,
        title = str("Dialog (W)"),
        summary = str("A window-level dialog, no COUIPopupHost required."),
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        content = {
            val dismissState = LocalDismissState.current
            DialogButtonBar(
                negative = DialogButtonBarAction(text = str("Cancel"), onClick = { dismissState?.invoke() }),
                positive = DialogButtonBarAction(text = str("Confirm"), onClick = { dismissState?.invoke() }),
            )
        },
    )
}

/**
 * Shows the recommend-button tier of [DialogButtonBar]: the positive action is flagged
 * [DialogButtonBarAction.recommended], so it draws as a filled primary button while the negative
 * one stays borderless — COUI's `COUIButtonBarLayout.setRecommendButtonId` /
 * `getBlurWindowButtonType`.
 */
@Composable
private fun RecommendedButtonBarDialogDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    OverlayDialog(
        show = show,
        title = str("Install Update"),
        summary = str("The positive action below is the bar's recommend button."),
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        content = {
            DialogButtonBar(
                negative = DialogButtonBarAction(text = str("Later"), onClick = onDismissRequest),
                positive = DialogButtonBarAction(
                    text = str("Install"),
                    onClick = onDismissRequest,
                    recommended = true,
                ),
            )
        },
    )
}
