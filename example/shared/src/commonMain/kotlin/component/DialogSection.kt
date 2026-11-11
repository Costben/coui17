// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")
@file:OptIn(ExperimentalCOUIApi::class)

package component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coui17.SingleInputDialogDemo
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.layout.AlertDialogButton
import io.github.suqi8.coui.kmp.layout.AlertDialogStyle
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
    val windowHost = remember { mutableStateOf(false) }
    val alertVariant = remember { mutableIntStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogHoldDown by remember { mutableStateOf(false) }
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
    var showSingleInputDialog by remember { mutableStateOf(false) }
    var singleInputDialogHoldDown by remember { mutableStateOf(false) }

    DemoOptions(
        DemoToggle("Window host", windowHost),
        DemoChoice(
            "Alert dialog variant",
            listOf("Standard", "3 actions", "Recommended action", "Tiny", "Long text"),
            alertVariant,
        ),
    )
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ArrowPreference(
            title = str("Dialog"),
            summary = if (windowHost.value) {
                str("Click to show a WindowDialog")
            } else {
                str("Click to show an OverlayDialog")
            },
            onClick = {
                showDialog = true
                dialogHoldDown = true
            },
            holdDownState = dialogHoldDown,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("Loading Dialog"),
            summary = str("Click to show an OverlayLoadingDialog"),
            onClick = {
                showLoadingDialog = true
                loadingDialogHoldDown = true
            },
            holdDownState = loadingDialogHoldDown,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("Stacked Button Bar"),
            summary = str("Long labels flip the bar to a vertical stack"),
            onClick = {
                showStackedDialog = true
                stackedDialogHoldDown = true
            },
            holdDownState = stackedDialogHoldDown,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("Recommended Button Bar"),
            summary = str("One action is highlighted as the recommend button"),
            onClick = {
                showRecommendedDialog = true
                recommendedDialogHoldDown = true
            },
            holdDownState = recommendedDialogHoldDown,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("Alert Dialog"),
            summary = str(
                when (alertVariant.intValue) {
                    1 -> "Cancel / Never / OK stacked because three actions never fit one row"
                    2 -> "The confirm action is the bar's recommend button"
                    3 -> "Center.Tiny: a compact panel pinned to the bottom edge"
                    4 -> "The panel caps its height and the message scrolls"
                    else -> "Centered warning dialog with a confirm / cancel row"
                },
            ),
            onClick = {
                showAlertDialog = true
                alertDialogHoldDown = true
            },
            holdDownState = alertDialogHoldDown,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("Progress Dialog"),
            summary = str("Horizontal progress dialog, indefinite then determinate"),
            onClick = {
                showProgressDialog = true
                progressDialogHoldDown = true
            },
            holdDownState = progressDialogHoldDown,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("Security Dialog"),
            summary = str("Click to show an OverlaySecurityDialog"),
            onClick = {
                showSecurityDialog = true
                securityDialogHoldDown = true
            },
            holdDownState = securityDialogHoldDown,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("Single Input Dialog"),
            summary = str("A text field with a byte counter above the button bar"),
            onClick = {
                showSingleInputDialog = true
                singleInputDialogHoldDown = true
            },
            holdDownState = singleInputDialogHoldDown,
        )
    }

    SingleInputDialogDemo(
        show = showSingleInputDialog,
        onDismissRequest = { showSingleInputDialog = false },
        onDismissFinished = { singleInputDialogHoldDown = false },
    )

    // Both hosts stay composed so a dialog that is closing finishes on the host it opened on.
    SuperDialogDemo(
        show = showDialog && !windowHost.value,
        onDismissRequest = { showDialog = false },
        onDismissFinished = { dialogHoldDown = false },
    )
    WindowDialogDemo(
        show = showDialog && windowHost.value,
        onDismissRequest = { showDialog = false },
        onDismissFinished = { dialogHoldDown = false },
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

    // The Center family's tiers: the standard confirm / cancel row, a third action, the recommend
    // marker, the tiny bottom-pinned variant and a scrolling message. Each stays composed so the one
    // that is closing finishes its exit after the option changes.
    val dismissAlert = { showAlertDialog = false }
    val alertFinished = { alertDialogHoldDown = false }
    AlertDialogDemo(showAlertDialog && alertVariant.intValue == 0, dismissAlert, alertFinished)
    NeutralAlertDialogDemo(showAlertDialog && alertVariant.intValue == 1, dismissAlert, alertFinished)
    RecommendedAlertDialogDemo(showAlertDialog && alertVariant.intValue == 2, dismissAlert, alertFinished)
    TinyAlertDialogDemo(showAlertDialog && alertVariant.intValue == 3, dismissAlert, alertFinished)
    LongTextAlertDialogDemo(showAlertDialog && alertVariant.intValue == 4, dismissAlert, alertFinished)

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

/**
 * The neutral (third) action of the centered alert dialog: `COUIAlertDialogBuilder`'s
 * `setNeutralButton`. Three actions drop `COUIButtonBarLayout` out of its horizontal tier, so the
 * capsules stack neutral / positive / negative.
 */
@Composable
private fun NeutralAlertDialogDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    OverlayAlertDialog(
        show = show,
        onConfirm = onDismissRequest,
        onCancel = onDismissRequest,
        title = str("Clear browsing data?"),
        summary = str("Cookies, cache and history are removed from every synced device."),
        neutralText = str("Never"),
        onNeutral = onDismissRequest,
        confirmText = str("Clear"),
        cancelText = str("Cancel"),
        onDismissFinished = onDismissFinished,
    )
}

/** The centered alert dialog with its confirm action marked as the bar's recommend button. */
@Composable
private fun RecommendedAlertDialogDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    OverlayAlertDialog(
        show = show,
        onConfirm = onDismissRequest,
        onCancel = onDismissRequest,
        title = str("Update available"),
        summary = str("Install the update now, or keep using this version for a while."),
        recommendedButton = AlertDialogButton.Positive,
        confirmText = str("Install"),
        cancelText = str("Later"),
        onDismissFinished = onDismissFinished,
    )
}

/** `AlertDialogStyle.CenterTiny`: the compact panel `AlertDialogBuildStyle.Center.Tiny` draws. */
@Composable
private fun TinyAlertDialogDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    OverlayAlertDialog(
        show = show,
        onConfirm = onDismissRequest,
        onCancel = onDismissRequest,
        style = AlertDialogStyle.CenterTiny,
        title = str("Turn on location?"),
        summary = str("Weather needs your location to show the forecast for this area."),
        confirmText = str("Turn on"),
        cancelText = str("Not now"),
        onDismissFinished = onDismissFinished,
    )
}

/**
 * The height-capped tier: the message is far longer than the panel is allowed to grow, so it has
 * to scroll inside the panel the way COUI's `COUIMaxHeightNestedScrollView` body does, while the
 * title and the button bar keep their own heights.
 */
@Composable
private fun LongTextAlertDialogDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    OverlayAlertDialog(
        show = show,
        onConfirm = onDismissRequest,
        onCancel = onDismissRequest,
        title = str("User Agreement"),
        summary = str(
            "Welcome to the ColorOS user experience programme. Before you continue, please read " +
                "the following terms carefully. This agreement describes what we collect, why we " +
                "collect it and how long we keep it, and it explains the choices you have at every " +
                "step. Participation is entirely optional: you can decline now and change your mind " +
                "at any time from Settings, System and updates, User experience programme. " +
                "We collect two kinds of information. Usage statistics record how often a feature " +
                "is opened, how long it takes to respond and whether it succeeds, without any " +
                "content you type, save or share. Diagnostic reports are collected only after a " +
                "problem occurs and contain the state of the system at the moment it happened, " +
                "including the model name, the software version and a list of running services. " +
                "Nothing in either category identifies you personally, and neither category " +
                "includes your messages, photographs, contacts, calendar entries, call history or " +
                "the contents of any file. Reports travel over an encrypted connection, are stored " +
                "for no longer than thirty days and are deleted afterwards without further notice. " +
                "You may review every report before it is sent, refuse to send an individual report " +
                "or stop sending them altogether. If you decline, the device continues to work " +
                "exactly as it does today; the only difference is that we learn about a problem " +
                "from somewhere else, which usually takes longer. If you accept, you can withdraw " +
                "consent at any time and the change takes effect immediately, with no effect on " +
                "the reports already sent. We never sell this data, we never share it with " +
                "advertisers, and we share it with a third party only when a component of the " +
                "system is maintained by that party and only for the purpose described above. " +
                "Questions about this agreement can be sent to the privacy team from the feedback " +
                "entry in Settings, and a written answer will follow within fifteen working days.",
        ),
        confirmText = str("Agree"),
        cancelText = str("Cancel"),
        onDismissFinished = onDismissFinished,
    )
}
