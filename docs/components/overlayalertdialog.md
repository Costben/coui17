---
title: OverlayAlertDialog
requiresScaffoldHost: true
prerequisites:
  - Must be used within `Scaffold` to provide `COUIPopupHost`
  - Using outside `Scaffold` will cause popup content not to render
  - Multiple nested or side-by-side `Scaffold`s are supported without extra configuration
hostComponent: Scaffold
popupHost: COUIPopupHost
---

# OverlayAlertDialog

`OverlayAlertDialog` is the centered warning variant of the dialog family, mirroring the COUI center-style alert dialog (`AlertDialogBuildStyle.Center` / `coui_center_alert_dialog_layout.xml`). It shows a title, a message, an optional extra body slot and a cancel / confirm button row.

The difference from `OverlayDialog` is the presentation: `OverlayDialog` is bottom-anchored on compact windows and only centers on large screens, while `OverlayAlertDialog` is always centered and always uses the COUI center-dialog scale + fade transitions, exactly like a warning / confirmation alert. New in 1.2.0.

::: danger Prerequisite
This component depends on `Scaffold` providing `COUIPopupHost` to render popup content. It must be used within `Scaffold`, otherwise popup content will not render correctly.
:::

## Import

```kotlin
import io.github.suqi8.coui.kmp.overlay.OverlayAlertDialog
import io.github.suqi8.coui.kmp.layout.AlertDialogDefaults
import io.github.suqi8.coui.kmp.layout.AlertDialogColors
```

## Basic Usage

```kotlin
var showAlert by remember { mutableStateOf(false) }

Scaffold {
    TextButton(
        text = "Delete backup",
        onClick = { showAlert = true }
    )

    OverlayAlertDialog(
        show = showAlert,
        onConfirm = { /* delete */ showAlert = false },
        onCancel = { showAlert = false },
        title = "Delete this backup?",
        summary = "The backup will be removed from the cloud and from every synced device.",
        confirmText = "Delete",
        cancelText = "Cancel",
    )
}
```

Tapping outside the panel, pressing back, and the cancel button all route to `onCancel`, matching COUI, which reports the back key as the negative selection.

## Custom Body Slot

`content` is an optional extra body rendered between the message and the button row. The title and message stay built in, so the slot only carries what the built-in texts cannot express:

```kotlin
OverlayAlertDialog(
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

### OverlayAlertDialog Properties

| Property Name        | Type                   | Description                                                   | Default Value                        | Required |
| -------------------- | ---------------------- | ------------------------------------------------------------- | ------------------------------------ | -------- |
| show                 | Boolean                | Whether to show the dialog                                    | -                                    | Yes      |
| onConfirm            | () -> Unit             | Called when the confirm (positive) button is clicked          | -                                    | Yes      |
| onCancel             | () -> Unit             | Called when the cancel (negative) button is clicked, the user taps outside, or presses back | - | Yes |
| modifier             | Modifier               | Modifier applied to the dialog                                | Modifier                             | No       |
| title                | String?                | Dialog title                                                  | null                                 | No       |
| summary              | String?                | Dialog message                                                | null                                 | No       |
| confirmText          | String                 | Label of the confirm (positive) button                        | AlertDialogDefaults.ConfirmText      | No       |
| cancelText           | String                 | Label of the cancel (negative) button                         | AlertDialogDefaults.CancelText       | No       |
| colors               | AlertDialogColors      | Colors of the title, message and panel                        | AlertDialogDefaults.alertDialogColors() | No    |
| enableWindowDim      | Boolean                | Whether to enable the dimming layer                           | true                                 | No       |
| onDismissFinished    | (() -> Unit)?          | Invoked after the hide animation completes; not invoked if the hide is cancelled mid-flight | null | No |
| renderInRootScaffold | Boolean                | Whether to render the dialog in the root (outermost) Scaffold. When true, the dialog covers the full screen. When false, it renders within the current Scaffold's bounds | true | No |
| cornerRadius         | Dp?                    | Corner radius override; when null, `AlertDialogDefaults.CornerRadius` is used | null | No |
| maxWidth             | Dp                     | Maximum width of the dialog                                   | AlertDialogDefaults.MaxWidth         | No       |
| content              | (@Composable () -> Unit)? | Optional extra body above the button row                   | null                                 | No       |

### AlertDialogDefaults Object

#### Properties

| Property Name      | Type | Description                                                                       |
| ------------------ | ---- | --------------------------------------------------------------------------------- |
| ConfirmText        | String | Default label of the confirm button (`OK`)                                      |
| CancelText         | String | Default label of the cancel button (`Cancel`)                                   |
| CornerRadius       | Dp   | Panel corner radius (19.dp), the shared `DialogDefaults.CornerRadius`            |
| MaxWidth           | Dp   | Maximum panel width (392.dp), the shared `DialogDefaults.MaxWidth`               |
| ButtonBarMarginTop | Dp   | Gap between the panel content and the button row (8.dp, COUI `coui_alert_dialog_customer_layout_padding_bottom`) |

#### Functions

| Function Name      | Return Type        | Description                                        |
| ------------------ | ------------------ | -------------------------------------------------- |
| alertDialogColors()| AlertDialogColors  | Creates the default color configuration (see below) |
| titleColor()       | Color              | Default title color                                |
| summaryColor()     | Color              | Default message color                              |
| backgroundColor()  | Color              | Default panel background color                     |

### `alertDialogColors()` factory

| Parameter      | Type  | Default                                | COUI Source                       |
| -------------- | ----- | -------------------------------------- | --------------------------------- |
| titleColor     | Color | DialogDefaults.titleColor()            | couiColorLabelPrimary             |
| summaryColor   | Color | DialogDefaults.summaryColor()          | couiColorLabelPrimary             |
| backgroundColor| Color | DialogDefaults.backgroundColor()       | couiColorBackground               |

### AlertDialogColors

| Property Name   | Type  | Description                    |
| --------------- | ----- | ------------------------------ |
| titleColor      | Color | Color of the title             |
| summaryColor    | Color | Color of the message           |
| backgroundColor | Color | Background color of the panel  |

## Behavior

- The panel is always centered, with the COUI center-dialog transitions: a 250ms enter that scales 0.8 → 1 while fading in, and a 150ms fade-out on exit.
- The button row is a `DialogButtonBar`, so it follows the COUI button-bar rules: it stays horizontal only when both labels fit, and otherwise stacks vertically.
- Tapping outside and the back gesture report the negative selection through `onCancel`.
- A panel taller than two thirds of the window is capped at that height, like the shared dialog layout.
