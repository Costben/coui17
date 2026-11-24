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
import io.github.suqi8.coui.kmp.window.WindowAlertDialog
import io.github.suqi8.coui.kmp.window.WindowDialog
import io.github.suqi8.coui.kmp.window.WindowLoadingDialog
import io.github.suqi8.coui.kmp.window.WindowProgressDialog
import io.github.suqi8.coui.kmp.window.WindowSecurityDialog
import kotlinx.coroutines.delay

private const val DIALOG_TYPE_BASIC = 0
private const val DIALOG_TYPE_ALERT = 1
private const val DIALOG_TYPE_INPUT = 2
private const val DIALOG_TYPE_LOADING = 3
private const val DIALOG_TYPE_PROGRESS = 4
private const val DIALOG_TYPE_SECURITY = 5

/**
 * Renders the "Dialog" gallery section content: a concise dropdown selector for choosing the
 * dialog type and style, a host toggle, and a single trigger card.
 */
@Composable
fun DialogDemo() {
    val dialogType = remember { mutableIntStateOf(DIALOG_TYPE_BASIC) }
    val basicButtonStyle = remember { mutableIntStateOf(0) }
    val alertVariant = remember { mutableIntStateOf(0) }
    val loadingCancelable = remember { mutableStateOf(false) }
    val windowHost = remember { mutableStateOf(false) }

    var showDialog by remember { mutableStateOf(false) }
    var dialogHoldDown by remember { mutableStateOf(false) }
    var progressDialogValue by remember { mutableStateOf<Float?>(null) }

    val options = buildList {
        add(
            DemoChoice(
                "Dialog type",
                listOf("Basic", "Alert", "Single input", "Loading", "Progress", "Security"),
                dialogType,
            ),
        )
        when (dialogType.intValue) {
            DIALOG_TYPE_BASIC -> add(
                DemoChoice(
                    "Button bar",
                    listOf("Standard", "Stacked", "Recommended"),
                    basicButtonStyle,
                ),
            )

            DIALOG_TYPE_ALERT -> add(
                DemoChoice(
                    "Alert dialog variant",
                    listOf("Standard", "3 actions", "Recommended action", "Tiny", "Long text"),
                    alertVariant,
                ),
            )

            DIALOG_TYPE_LOADING -> add(DemoToggle("Cancelable", loadingCancelable))
        }
        if (dialogType.intValue != DIALOG_TYPE_INPUT) {
            add(DemoToggle("Window host", windowHost))
        }
    }

    DemoOptions(*options.toTypedArray())

    val triggerTitle = when (dialogType.intValue) {
        DIALOG_TYPE_BASIC -> str("Dialog")
        DIALOG_TYPE_ALERT -> str("Alert Dialog")
        DIALOG_TYPE_INPUT -> str("Single Input Dialog")
        DIALOG_TYPE_LOADING -> str("Loading Dialog")
        DIALOG_TYPE_PROGRESS -> str("Progress Dialog")
        else -> str("Security Dialog")
    }
    val triggerSummary = when (dialogType.intValue) {
        DIALOG_TYPE_BASIC -> when (basicButtonStyle.intValue) {
            1 -> str("Long labels flip the bar to a vertical stack")

            2 -> str("One action is highlighted as the recommend button")

            else -> if (windowHost.value) {
                str("Click to show a WindowDialog")
            } else {
                str("Click to show an OverlayDialog")
            }
        }

        DIALOG_TYPE_ALERT -> str(
            when (alertVariant.intValue) {
                1 -> "Cancel / Never / OK stacked because three actions never fit one row"
                2 -> "The confirm action is the bar's recommend button"
                3 -> "Center.Tiny: a compact panel pinned to the bottom edge"
                4 -> "The panel caps its height and the message scrolls"
                else -> "Centered warning dialog with a confirm / cancel row"
            },
        )

        DIALOG_TYPE_INPUT -> str("A text field with a byte counter above the button bar")

        DIALOG_TYPE_LOADING -> if (windowHost.value) {
            str("Click to show a WindowLoadingDialog")
        } else {
            str("Click to show an OverlayLoadingDialog")
        }

        DIALOG_TYPE_PROGRESS -> str("Horizontal progress dialog, indefinite then determinate")

        else -> if (windowHost.value) {
            str("Click to show a WindowSecurityDialog")
        } else {
            str("Click to show an OverlaySecurityDialog")
        }
    }

    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ArrowPreference(
            title = triggerTitle,
            summary = triggerSummary,
            onClick = {
                showDialog = true
                dialogHoldDown = true
            },
            holdDownState = dialogHoldDown,
        )
    }

    val dismiss = { showDialog = false }
    val finished = { dialogHoldDown = false }
    val isWindow = windowHost.value

    // Basic dialogs (Standard / Stacked / Recommended, Overlay & Window hosts).
    BasicDialogDemo(
        show = showDialog && dialogType.intValue == DIALOG_TYPE_BASIC && !isWindow,
        windowHost = false,
        buttonStyle = basicButtonStyle.intValue,
        onDismissRequest = dismiss,
        onDismissFinished = finished,
    )
    BasicDialogDemo(
        show = showDialog && dialogType.intValue == DIALOG_TYPE_BASIC && isWindow,
        windowHost = true,
        buttonStyle = basicButtonStyle.intValue,
        onDismissRequest = dismiss,
        onDismissFinished = finished,
    )

    // Alert dialogs (Center variants, Overlay & Window hosts).
    AlertDialogVariantDemo(
        show = showDialog && dialogType.intValue == DIALOG_TYPE_ALERT && !isWindow,
        windowHost = false,
        variant = alertVariant.intValue,
        onDismissRequest = dismiss,
        onDismissFinished = finished,
    )
    AlertDialogVariantDemo(
        show = showDialog && dialogType.intValue == DIALOG_TYPE_ALERT && isWindow,
        windowHost = true,
        variant = alertVariant.intValue,
        onDismissRequest = dismiss,
        onDismissFinished = finished,
    )

    // Single input dialog.
    SingleInputDialogDemo(
        show = showDialog && dialogType.intValue == DIALOG_TYPE_INPUT,
        onDismissRequest = dismiss,
        onDismissFinished = finished,
    )

    // Auto-dismiss the non-cancelable loading demo after a short delay, like a finished task would.
    val showLoading = showDialog && dialogType.intValue == DIALOG_TYPE_LOADING
    LaunchedEffect(showLoading, loadingCancelable.value) {
        if (showLoading && !loadingCancelable.value) {
            delay(3000)
            showDialog = false
        }
    }
    val loadingCancelAction = if (loadingCancelable.value) {
        DialogButtonBarAction(text = str("Cancel"), onClick = dismiss)
    } else {
        null
    }
    OverlayLoadingDialog(
        show = showLoading && !isWindow,
        text = str(if (loadingCancelable.value) "Downloading..." else "Loading..."),
        title = if (loadingCancelable.value) str("Download") else null,
        cancelAction = loadingCancelAction,
        onDismissRequest = dismiss,
        onDismissFinished = finished,
    )
    WindowLoadingDialog(
        show = showLoading && isWindow,
        text = str(if (loadingCancelable.value) "Downloading..." else "Loading..."),
        title = if (loadingCancelable.value) str("Download") else null,
        cancelAction = loadingCancelAction,
        onDismissRequest = dismiss,
        onDismissFinished = finished,
    )

    // Walk the horizontal progress dialog through indefinite then determinate mode.
    val showProgress = showDialog && dialogType.intValue == DIALOG_TYPE_PROGRESS
    LaunchedEffect(showProgress) {
        if (showProgress) {
            progressDialogValue = null
            delay(1200)
            repeat(40) { step ->
                progressDialogValue = step / 40f
                delay(60)
            }
            progressDialogValue = 1f
            delay(400)
            showDialog = false
        }
    }
    val progressText = if (progressDialogValue == null) {
        str("Connecting...")
    } else {
        str("Downloading...")
    }
    OverlayProgressDialog(
        show = showProgress && !isWindow,
        progress = progressDialogValue,
        text = progressText,
        onDismissRequest = dismiss,
        onDismissFinished = finished,
    )
    WindowProgressDialog(
        show = showProgress && isWindow,
        progress = progressDialogValue,
        text = progressText,
        onDismissRequest = dismiss,
        onDismissFinished = finished,
    )

    // Security dialog (Overlay & Window hosts).
    val showSecurity = showDialog && dialogType.intValue == DIALOG_TYPE_SECURITY
    OverlaySecurityDialog(
        show = showSecurity && !isWindow,
        title = str("Security Notice"),
        summary = str("This feature needs to connect to the network."),
        statement = str("Tap and view Privacy Policy for more information."),
        statementLinkText = str("Privacy Policy"),
        onLinkClick = { /* Open the privacy policy */ },
        onConfirm = { _ -> showDialog = false },
        onCancel = dismiss,
        onDismissFinished = finished,
    )
    WindowSecurityDialog(
        show = showSecurity && isWindow,
        title = str("Security Notice"),
        summary = str("This feature needs to connect to the network."),
        statement = str("Tap and view Privacy Policy for more information."),
        statementLinkText = str("Privacy Policy"),
        onLinkClick = { /* Open the privacy policy */ },
        onConfirm = { _ -> showDialog = false },
        onCancel = dismiss,
        onDismissFinished = finished,
    )
}

@Composable
private fun BasicDialogDemo(
    show: Boolean,
    windowHost: Boolean,
    buttonStyle: Int,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    val title = when (buttonStyle) {
        1 -> str("Delete Backup")
        2 -> str("Install Update")
        else -> str(if (windowHost) "Dialog (W)" else "Dialog (O)")
    }
    val summary = when (buttonStyle) {
        1 -> str("The button labels below do not fit side by side, so the bar stacks them.")

        2 -> str("The positive action below is the bar's recommend button.")

        else -> str(
            if (windowHost) {
                "A window-level dialog, no COUIPopupHost required."
            } else {
                "A dialog component inside COUIPopupHost."
            },
        )
    }
    val body: @Composable () -> Unit = {
        val dismissState = LocalDismissState.current
        val dismissAction = { if (windowHost) dismissState?.invoke() else onDismissRequest() }
        when (buttonStyle) {
            1 -> DialogButtonBar(
                negative = DialogButtonBarAction(text = str("Not Now"), onClick = { dismissAction() }),
                positive = DialogButtonBarAction(
                    text = str("Delete Backup And Local Copies"),
                    onClick = { dismissAction() },
                ),
            )

            2 -> DialogButtonBar(
                negative = DialogButtonBarAction(text = str("Later"), onClick = { dismissAction() }),
                positive = DialogButtonBarAction(
                    text = str("Install"),
                    onClick = { dismissAction() },
                    recommended = true,
                ),
            )

            else -> DialogButtonBar(
                negative = DialogButtonBarAction(text = str("Cancel"), onClick = { dismissAction() }),
                positive = DialogButtonBarAction(text = str("Confirm"), onClick = { dismissAction() }),
            )
        }
    }

    if (windowHost) {
        WindowDialog(
            show = show,
            title = title,
            summary = summary,
            onDismissRequest = onDismissRequest,
            onDismissFinished = onDismissFinished,
            content = body,
        )
    } else {
        OverlayDialog(
            show = show,
            title = title,
            summary = summary,
            onDismissRequest = onDismissRequest,
            onDismissFinished = onDismissFinished,
            content = body,
        )
    }
}

@Composable
private fun AlertDialogVariantDemo(
    show: Boolean,
    windowHost: Boolean,
    variant: Int,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    val title = when (variant) {
        1 -> str("Clear browsing data?")
        2 -> str("Update available")
        3 -> str("Turn on location?")
        4 -> str("User Agreement")
        else -> str("Delete this backup?")
    }
    val summary = when (variant) {
        1 -> str("Cookies, cache and history are removed from every synced device.")
        2 -> str("Install the update now, or keep using this version for a while.")
        3 -> str("Weather needs your location to show the forecast for this area.")
        4 -> str(LONG_AGREEMENT_TEXT)
        else -> str("The backup will be removed from the cloud and from every synced device.")
    }
    val confirmText = when (variant) {
        1 -> str("Clear")
        2 -> str("Install")
        3 -> str("Turn on")
        4 -> str("Agree")
        else -> str("Delete")
    }
    val cancelText = when (variant) {
        2 -> str("Later")
        3 -> str("Not now")
        else -> str("Cancel")
    }
    val neutralText = if (variant == 1) str("Never") else null
    val recommendedButton = if (variant == 2) AlertDialogButton.Positive else null
    val style = if (variant == 3) AlertDialogStyle.CenterTiny else AlertDialogStyle.Center
    val extraContent: (@Composable () -> Unit)? = if (variant == 0) {
        {
            Text(
                text = str("This action cannot be undone."),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DialogDefaults.insideMargin.width),
                color = COUITheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                style = COUITheme.textStyles.body2,
            )
        }
    } else {
        null
    }

    if (windowHost) {
        WindowAlertDialog(
            show = show,
            onConfirm = onDismissRequest,
            onCancel = onDismissRequest,
            title = title,
            summary = summary,
            confirmText = confirmText,
            cancelText = cancelText,
            neutralText = neutralText,
            onNeutral = if (neutralText != null) onDismissRequest else null,
            recommendedButton = recommendedButton,
            style = style,
            onDismissFinished = onDismissFinished,
            content = extraContent,
        )
    } else {
        OverlayAlertDialog(
            show = show,
            onConfirm = onDismissRequest,
            onCancel = onDismissRequest,
            title = title,
            summary = summary,
            confirmText = confirmText,
            cancelText = cancelText,
            neutralText = neutralText,
            onNeutral = if (neutralText != null) onDismissRequest else null,
            recommendedButton = recommendedButton,
            style = style,
            onDismissFinished = onDismissFinished,
            content = extraContent,
        )
    }
}

private const val LONG_AGREEMENT_TEXT =
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
        "entry in Settings, and a written answer will follow within fifteen working days."
