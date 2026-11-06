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

<div style="position: relative; height: 240px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../compose/index.html?id=overlayAlertDialog" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

`style` selects the build style. `AlertDialogStyle.Center` (the default) is `AlertDialogBuildStyle.Center`; `AlertDialogStyle.CenterTiny` is `AlertDialogBuildStyle.Center.Tiny`, which keeps the same title / message content but pins a compact panel to the bottom edge — see [Center.Tiny](#center-tiny).

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
| neutralText          | String?                | Label of the optional third (neutral) button, between the other two | null                          | No       |
| onNeutral            | (() -> Unit)?          | Called when the neutral button is clicked                     | null                                 | No       |
| recommendedButton    | AlertDialogButton?     | Action drawn as the recommend (filled primary) button: `Negative` / `Neutral` / `Positive` | null | No |
| style                | AlertDialogStyle       | Center-family build style: `Center` or `CenterTiny`           | AlertDialogStyle.Center              | No       |
| colors               | AlertDialogColors      | Colors of the title, message and panel                        | AlertDialogDefaults.alertDialogColors() | No    |
| enableWindowDim      | Boolean                | Whether to enable the dimming layer                           | true                                 | No       |
| onDismissFinished    | (() -> Unit)?          | Invoked after the hide animation completes; not invoked if the hide is cancelled mid-flight | null | No |
| renderInRootScaffold | Boolean                | Whether to render the dialog in the root (outermost) Scaffold. When true, the dialog covers the full screen. When false, it renders within the current Scaffold's bounds | true | No |
| cornerRadius         | Dp?                    | Corner radius override; when null, `AlertDialogDefaults.CornerRadius` is used | null | No |
| maxWidth             | Dp                     | Maximum width of the dialog window; the panel is 16dp narrower per side | AlertDialogDefaults.MaxWidth | No   |
| content              | (@Composable () -> Unit)? | Optional extra body above the button row                   | null                                 | No       |

### AlertDialogDefaults Object

#### Properties

| Property Name      | Type | Description                                                                       |
| ------------------ | ---- | --------------------------------------------------------------------------------- |
| ConfirmText        | String | Default label of the confirm button (`OK`)                                      |
| CancelText         | String | Default label of the cancel button (`Cancel`)                                   |
| CornerRadius       | Dp   | Panel corner radius (45.dp), the shared `DialogDefaults.CornerRadius`            |
| MaxWidth           | Dp   | Maximum dialog window width (392.dp), the shared `DialogDefaults.MaxWidth`; the panel is 32dp narrower |
| ButtonBarMarginTop | Dp   | Gap between the panel content and the button row (8.dp, COUI `coui_alert_dialog_customer_layout_padding_bottom`) |
| TinyMaxHeight      | Dp   | Height cap of the tiny panel (320.dp, `coui_tiny_dialog_max_height` 352dp minus the 32dp the native max-height arithmetic subtracts) |
| TinyInsideMargin   | DpSize | Title padding of the tiny panel: 16dp sides, 16dp top (`coui_tiny_dialog_message_padding_horizontal`) |
| TinyTitleMarginBottom | Dp | Bottom margin of the tiny title (8dp)                                            |
| TinyTitleFontSize  | TextUnit | Tiny title size (18.sp, `COUIDialogTextAppearance.Title.Tiny`)                  |
| TinyMessageFontSize | TextUnit | Tiny message size (14.sp, `COUIAlertDialogMessageStyle.Tiny`)                   |
| TinyMessageMarginTop | Dp   | Tiny message scroller top padding (4dp, `center_dialog_scroll_padding_top`)       |
| TinyMessageMarginBottom | Dp | Tiny message scroller bottom padding (8dp, `coui_tiny_dialog_scroll_padding_bottom`) |
| TinyButtonBarPaddingBottom | Dp | Bottom padding of the tiny bar (12dp, `coui_tiny_dialog_btn_bar_padding_vertical`) |
| TinyButtonMarginVertical | Dp | Margin above and below a tiny capsule (4dp, `coui_tiny_dialog_btn_margin_vertical`) |
| TinyButtonFill     | Color | Fill of a tiny capsule (`coui_alert_dialog_button_color_tiny`, `#E6E6E6`)         |
| TinyButtonTextColor | Color | Label of a tiny capsule (`coui_alert_dialog_button_text_color_tiny`, `#000000`)  |

#### Functions

| Function Name      | Return Type        | Description                                        |
| ------------------ | ------------------ | -------------------------------------------------- |
| alertDialogColors()| AlertDialogColors  | Creates the default color configuration (see below) |
| titleColor()       | Color              | Default title color                                |
| summaryColor()     | Color              | Default message color                              |
| backgroundColor()  | Color              | Default panel background color                     |
| tinyButtonColors() | TextButtonColors   | Colors of a tiny capsule, including its themed disabled fill |

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

## Center.Tiny

`AlertDialogStyle.CenterTiny` is the compact member of the family, `AlertDialogBuildStyle.Center.Tiny`:

```kotlin
OverlayAlertDialog(
    show = showTiny,
    onConfirm = { showTiny = false },
    onCancel = { showTiny = false },
    style = AlertDialogStyle.CenterTiny,
    title = "Turn on location?",
    summary = "Weather needs your location to show the forecast for this area.",
    confirmText = "Turn on",
    cancelText = "Not now",
)
```

Despite the name the native style overrides `android:gravity` back to `bottom`: the panel is pinned to the bottom edge, spans the whole window (the tiny background carries no `coui_dialog_layout_margin_horizontal` inset), rounds only its top corners, uses the 250ms bottom-slide `Animation.COUI.Dialog`, caps at `TinyMaxHeight`, and closes with the full-width capsules of `coui_center_alert_dialog_button_panel_tiny.xml` — neutral / positive / negative, 16dp in from the sides, 4dp apart, 12dp above the panel edge. The capsules keep the fixed `#E6E6E6` fill with a black label in dark theme too, exactly like the system's.

## Behavior

- `AlertDialogStyle.Center` is always centered, with the COUI center-dialog transitions: a 250ms enter that scales 0.8 → 1 while fading in, and a 150ms fade-out on exit.
- `AlertDialogStyle.CenterTiny` slides up from the bottom edge over the same 250ms and fades out on exit.
- The button row is a `DialogButtonBar`, so it follows the COUI button-bar rules: it stays horizontal only when exactly two labels fit, and otherwise stacks vertically (neutral, positive, negative).
- `recommendedButton` marks one action as the bar's recommend button — the filled primary capsule of `COUIButtonBarLayout.setRecommendButtonId`.
- Tapping outside and the back gesture report the negative selection through `onCancel`.
- A centered panel is capped at two thirds of the window height, like the shared dialog layout.
