---
title: OverlayProgressDialog
requiresScaffoldHost: true
prerequisites:
  - Must be used within `Scaffold` to provide `COUIPopupHost`
  - Using outside `Scaffold` will cause popup content not to render
  - Multiple nested or side-by-side `Scaffold`s are supported without extra configuration
hostComponent: Scaffold
popupHost: COUIPopupHost
---

# OverlayProgressDialog

`OverlayProgressDialog` is the horizontal-progress sibling of `LoadingDialog`, mirroring ColorOS's horizontal progress dialog (COUIRotatingDialogBuilder / `coui_progress_dialog_horizontal.xml`): the same small always-centered card, with the rotating spinner replaced by a horizontal progress bar and an optional message below it.

The bar is determinate while `progress` is a value between `0.0f` and `1.0f`, and indefinite when `progress` is null. New in 1.2.0.

::: danger Prerequisite
This component depends on `Scaffold` providing `COUIPopupHost` to render popup content. It must be used within `Scaffold`, otherwise popup content will not render correctly.
:::

## Import

```kotlin
import io.github.suqi8.coui.kmp.overlay.OverlayProgressDialog
import io.github.suqi8.coui.kmp.layout.ProgressDialogDefaults
import io.github.suqi8.coui.kmp.layout.ProgressDialogColors
```

## Basic Usage

A determinate download dialog:

```kotlin
var showProgress by remember { mutableStateOf(false) }
var progress by remember { mutableStateOf(0f) }

Scaffold {
    TextButton(
        text = "Download",
        onClick = { showProgress = true }
    )

    OverlayProgressDialog(
        show = showProgress,
        progress = progress,
        text = "Downloading...",
    )

    LaunchedEffect(showProgress) {
        if (showProgress) {
            repeat(40) { step ->
                progress = step / 40f
                delay(60)
            }
            showProgress = false
        }
    }
}
```

Pass `progress = null` while the size is still unknown; the bar then runs the indeterminate sweep of `LinearProgressIndicator` instead:

```kotlin
OverlayProgressDialog(
    show = showProgress,
    progress = null,
    text = "Connecting...",
)
```

The message is optional, so a bare bar is also valid:

```kotlin
OverlayProgressDialog(show = showProgress, progress = 0.5f)
```

## User Dismissal

By default the dialog cannot be dismissed by the user, matching a non-cancelable COUI progress dialog. Pass `onDismissRequest` to allow dismissal by tapping outside or pressing back:

```kotlin
OverlayProgressDialog(
    show = showProgress,
    progress = progress,
    text = "Downloading...",
    onDismissRequest = { showProgress = false }
)
```

## Properties

### OverlayProgressDialog Properties

| Property Name        | Type                   | Description                                                                                   | Default Value                                       | Required |
| -------------------- | ---------------------- | --------------------------------------------------------------------------------------------- | --------------------------------------------------- | -------- |
| show                 | Boolean                | Whether to show the dialog                                                                    | -                                                   | Yes      |
| modifier             | Modifier               | Modifier applied to the dialog                                                                | Modifier                                            | No       |
| progress             | Float?                 | Progress between 0.0f and 1.0f, or null for an indefinite bar                                 | null                                                | No       |
| text                 | String?                | Message shown below the bar                                                                   | null                                                | No       |
| colors               | ProgressDialogColors   | Colors of the message and the bar                                                             | ProgressDialogDefaults.progressDialogColors()       | No       |
| backgroundColor      | Color                  | Card background color                                                                         | DialogDefaults.backgroundColor()                    | No       |
| enableWindowDim      | Boolean                | Whether to enable the dimming layer                                                           | true                                                | No       |
| onDismissRequest     | (() -> Unit)?          | Called when the user taps outside or presses back; when null the dialog cannot be dismissed   | null                                                | No       |
| onDismissFinished    | (() -> Unit)?          | Invoked after the hide animation completes                                                    | null                                                | No       |
| renderInRootScaffold | Boolean                | Whether to render the dialog in the root (outermost) Scaffold. When true, the dialog covers the full screen. When false, it renders within the current Scaffold's bounds | true | No       |

### ProgressDialogDefaults Object

#### Properties

| Property Name             | Type | Description                                                                                     |
| ------------------------- | ---- | ----------------------------------------------------------------------------------------------- |
| CardWidth                 | Dp   | Card width, the loading card's COUI `coui_spinner_layout_width` (152.dp)                        |
| CardMinHeight             | Dp   | Minimum card height, COUI `coui_spinner_layout_min_height` (120.dp)                             |
| CornerRadius              | Dp   | Card corner radius, COUI `coui_round_corner_m_radius` (9.dp)                                    |
| ContentHorizontalPadding  | Dp   | Horizontal padding inside the card so the bar does not touch the card edge (16.dp)              |
| ProgressBarHeight         | Dp   | Bar height, COUI `coui_loading_dialog_progress_height` (4.dp)                                   |
| TextMarginTop             | Dp   | Gap between the bar and the message (12.dp)                                                     |

#### Functions

| Function Name           | Return Type          | Description                                                    |
| ----------------------- | -------------------- | -------------------------------------------------------------- |
| progressDialogColors()  | ProgressDialogColors | Creates the default color configuration (see below)             |
| textColor()             | Color                | Default message color (primary label color)                     |

### `progressDialogColors()` factory

| Parameter      | Type                    | Default                                                    | COUI Source                       |
| -------------- | ----------------------- | ---------------------------------------------------------- | --------------------------------- |
| textColor      | Color                   | COUITheme.colorScheme.onBackground                         | couiColorLabelPrimary             |
| progressColors | ProgressIndicatorColors | ProgressIndicatorDefaults.progressIndicatorColors()        | COUIProgressHorizontal style      |

### ProgressDialogColors

| Property Name  | Type                    | Description                     |
| -------------- | ----------------------- | ------------------------------- |
| textColor      | Color                   | Color of the message            |
| progressColors | ProgressIndicatorColors | Colors of the bar track and fill |

## Behavior

- The card is always centered and uses the COUI center-dialog transitions, like `LoadingDialog`.
- The bar is the shared `LinearProgressIndicator`, so both modes carry the `ProgressBarRangeInfo` semantics: a determinate reading with a `0f..1f` range, or `Indeterminate` when `progress` is null.
- `progress` outside `0.0f..1.0f` is coerced into range by the indicator.
- The message is `body2` sized and centered, matching the loading dialog's message.
