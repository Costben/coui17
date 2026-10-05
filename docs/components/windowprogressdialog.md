---
title: WindowProgressDialog
requiresScaffoldHost: false
prerequisites:
  - Can be used anywhere, does not require `Scaffold` or `COUIPopupHost`
  - Renders at window level
hostComponent: None
popupHost: None
---

# WindowProgressDialog

`WindowProgressDialog` is the window-level sibling of `OverlayProgressDialog`: the same small centered card with a horizontal progress bar and an optional message, rendered through the platform `Dialog` instead of `COUIPopupHost`, so it needs no `Scaffold`. It mirrors ColorOS's horizontal progress dialog (COUIRotatingDialogBuilder / `coui_progress_dialog_horizontal.xml`). New in 1.2.0.

The bar is determinate while `progress` is a value between `0.0f` and `1.0f`, and indefinite when `progress` is null.

::: tip
This component is independent of `Scaffold` and can be used in any composable scope.
:::

## Import

```kotlin
import io.github.suqi8.coui.kmp.window.WindowProgressDialog
import io.github.suqi8.coui.kmp.layout.ProgressDialogDefaults
import io.github.suqi8.coui.kmp.layout.ProgressDialogColors
```

## Basic Usage

```kotlin
var showProgress by remember { mutableStateOf(false) }
var progress by remember { mutableStateOf<Float?>(null) }

TextButton(
    text = "Download",
    onClick = { showProgress = true }
)

WindowProgressDialog(
    show = showProgress,
    progress = progress,
    text = "Downloading...",
    onDismissRequest = { showProgress = false },
)
```

Pass `progress = null` for the indeterminate sweep:

```kotlin
WindowProgressDialog(
    show = showProgress,
    progress = null,
    text = "Connecting...",
)
```

## Properties

### WindowProgressDialog Properties

| Property Name     | Type                 | Description                                                                                   | Default Value                                  | Required |
| ----------------- | -------------------- | --------------------------------------------------------------------------------------------- | ---------------------------------------------- | -------- |
| show              | Boolean              | Whether to show the dialog                                                                    | -                                              | Yes      |
| modifier          | Modifier             | Modifier applied to the dialog                                                                | Modifier                                       | No       |
| progress          | Float?               | Progress between 0.0f and 1.0f, or null for an indefinite bar                                 | null                                           | No       |
| text              | String?              | Message shown below the bar                                                                   | null                                           | No       |
| colors            | ProgressDialogColors | Colors of the message and the bar                                                             | ProgressDialogDefaults.progressDialogColors()  | No       |
| backgroundColor   | Color                | Card background color                                                                         | DialogDefaults.backgroundColor()               | No       |
| enableWindowDim   | Boolean              | Whether to enable the dimming layer                                                           | true                                           | No       |
| onDismissRequest  | (() -> Unit)?        | Called when the user taps outside or presses back; when null the dialog cannot be dismissed   | null                                           | No       |
| onDismissFinished | (() -> Unit)?        | Invoked after the hide animation completes                                                    | null                                           | No       |

### ProgressDialogDefaults Object

The defaults are shared with `OverlayProgressDialog`, so see [OverlayProgressDialog](./overlayprogressdialog) for the full table; `CardWidth`, `CardMinHeight`, `CornerRadius`, `ContentHorizontalPadding`, `ProgressBarHeight`, `TextMarginTop` and the `progressDialogColors()` factory are the same.

## Behavior

- Identical presentation to `OverlayProgressDialog`: always centered, with the COUI center-dialog transitions.
- The bar is the shared `LinearProgressIndicator`, so both modes carry the `ProgressBarRangeInfo` semantics: a determinate reading with a `0f..1f` range, or `Indeterminate` when `progress` is null.
- The platform dialog clears its default background, dim and animation effects, so only the COUI squircle card is drawn.
