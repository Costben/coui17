---
title: WindowAlertDialog
requiresScaffoldHost: false
prerequisites:
  - Can be used anywhere, does not require `Scaffold` or `COUIPopupHost`
  - Renders at window level
hostComponent: None
popupHost: None
---

# WindowAlertDialog

`WindowAlertDialog` is the window-level sibling of `OverlayAlertDialog`: the same centered warning dialog, rendered through the platform `Dialog` instead of `COUIPopupHost`, so it needs no `Scaffold`. It keeps the COUI center-style alert presentation (`AlertDialogBuildStyle.Center` / `coui_center_alert_dialog_layout.xml`) and adds a top inset so the panel never slides under the status bar, caption bar or a display cutout. New in 1.2.0.

::: tip
This component is independent of `Scaffold` and can be used in any composable scope.
:::

## Import

```kotlin
import io.github.suqi8.coui.kmp.window.WindowAlertDialog
import io.github.suqi8.coui.kmp.layout.AlertDialogDefaults
import io.github.suqi8.coui.kmp.layout.AlertDialogColors
```

## Basic Usage

```kotlin
var showAlert by remember { mutableStateOf(false) }

TextButton(
    text = "Delete backup",
    onClick = { showAlert = true }
)

WindowAlertDialog(
    show = showAlert,
    onConfirm = { /* delete */ showAlert = false },
    onCancel = { showAlert = false },
    title = "Delete this backup?",
    summary = "The backup will be removed from the cloud and from every synced device.",
    confirmText = "Delete",
    cancelText = "Cancel",
)
```

## Custom Body Slot

```kotlin
WindowAlertDialog(
    show = showAlert,
    onConfirm = { /* ... */ },
    onCancel = { showAlert = false },
    title = "Delete this backup?",
    summary = "The backup will be removed from the cloud and from every synced device.",
    content = {
        Text(
            text = "This action cannot be undone.",
            style = COUITheme.textStyles.footnote1,
        )
    },
)
```

## Properties

### WindowAlertDialog Properties

| Property Name     | Type                      | Description                                                   | Default Value                            | Required |
| ----------------- | ------------------------- | ------------------------------------------------------------- | ---------------------------------------- | -------- |
| show              | Boolean                   | Whether to show the dialog                                    | -                                        | Yes      |
| onConfirm         | () -> Unit                | Called when the confirm (positive) button is clicked          | -                                        | Yes      |
| onCancel          | () -> Unit                | Called when the cancel (negative) button is clicked, the user taps outside, or presses back | -  | Yes |
| modifier          | Modifier                  | Modifier applied to the dialog                                | Modifier                                 | No       |
| title             | String?                   | Dialog title                                                  | null                                     | No       |
| summary           | String?                   | Dialog message                                                | null                                     | No       |
| confirmText       | String                    | Label of the confirm (positive) button                        | AlertDialogDefaults.ConfirmText          | No       |
| cancelText        | String                    | Label of the cancel (negative) button                         | AlertDialogDefaults.CancelText           | No       |
| colors            | AlertDialogColors         | Colors of the title, message and panel                        | AlertDialogDefaults.alertDialogColors()  | No       |
| enableWindowDim   | Boolean                   | Whether to enable the dimming layer                           | true                                     | No       |
| onDismissFinished | (() -> Unit)?             | Invoked after the hide animation completes                    | null                                     | No       |
| cornerRadius      | Dp?                       | Corner radius override; when null, `AlertDialogDefaults.CornerRadius` is used | null        | No       |
| maxWidth          | Dp                        | Maximum width of the dialog                                   | AlertDialogDefaults.MaxWidth             | No       |
| content           | (@Composable () -> Unit)? | Optional extra body above the button row                      | null                                     | No       |

### AlertDialogDefaults Object

The defaults are shared with `OverlayAlertDialog`, so see [OverlayAlertDialog](./overlayalertdialog) for the full table; `ConfirmText`, `CancelText`, `CornerRadius`, `MaxWidth`, `ButtonBarMarginTop` and the `alertDialogColors()` factory are the same.

## Behavior

- Identical presentation to `OverlayAlertDialog`: always centered, with the COUI center-dialog scale + fade transitions and a `DialogButtonBar` row.
- The platform dialog clears its default background, dim and animation effects, so only the COUI squircle panel is drawn.
- Tapping outside the platform window and the back gesture report the negative selection through `onCancel`.
