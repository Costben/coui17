---
title: OverlayBottomAssignmentDialog
requiresScaffoldHost: true
prerequisites:
  - Must be used within `Scaffold` to provide `COUIPopupHost`
  - Using outside `Scaffold` will cause popup content not to render
  - Multiple nested or side-by-side `Scaffold`s are supported without extra configuration
hostComponent: Scaffold
popupHost: COUIPopupHost
---

# OverlayBottomAssignmentDialog

`OverlayBottomAssignmentDialog` is COUI's `COUIAlertDialog.BottomAssignment`: an alert dialog whose window is pinned to the **bottom edge of the screen on every window size**.

<div style="position: relative; height: 240px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../compose/index.html?id=overlayBottomAssignmentDialog" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

::: danger Prerequisite
This component depends on `Scaffold` providing `COUIPopupHost` to render popup content. It must be used within `Scaffold`, otherwise popup content will not render correctly.
:::

It is not a bottom sheet. The panel is the alert-dialog surface (the `coui_alert_dialog_builder_background` material, inset by `coui_dialog_layout_margin_horizontal` / `..._vertical`), it slides up from the bottom edge, and it has none of the sheet's drag handle, drag-to-dismiss gesture or blur material. `BottomAssignment` sets the window gravity to bottom (`0x50`) and keeps it there, so the large-screen centred presentation never applies either.

::: warning Experimental
The component is annotated `@ExperimentalCOUIApi`; opt in with `@OptIn(ExperimentalCOUIApi::class)`.
:::

## Import

```kotlin
import io.github.suqi8.coui.kmp.overlay.OverlayBottomAssignmentDialog
```

## Basic Usage

```kotlin
var show by remember { mutableStateOf(false) }

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
```

`content` is the dialog body. It owns its button row, exactly like the COUI preference editors that ship their own button panel — pass a [`DialogButtonBar`](./dialogbuttonbar) for the standard alert-dialog actions.

## Properties

| Name | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `show` | `Boolean` | — | Whether the dialog is shown. |
| `onDismissRequest` | `() -> Unit` | — | Invoked when the user taps outside or presses back. |
| `modifier` | `Modifier` | `Modifier` | Applied to the dialog content. |
| `title` | `String?` | `null` | Title, laid out like the alert dialog's `alertTitle`. |
| `summary` | `String?` | `null` | Message, laid out like the alert dialog's `message`. |
| `colors` | `AlertDialogColors` | `AlertDialogDefaults.alertDialogColors()` | Colours of the title, message and panel. |
| `enableWindowDim` | `Boolean` | `true` | Whether the window dims behind the dialog. |
| `onDismissFinished` | `(() -> Unit)?` | `null` | Called once the dismiss animation completes. |
| `renderInRootScaffold` | `Boolean` | `true` | Render in the outermost Scaffold so the dialog covers the full screen; `false` keeps it inside the current Scaffold. |
| `cornerRadius` | `Dp?` | `null` | Corner radius override; `null` uses `AlertDialogDefaults.CornerRadius`. |
| `maxWidth` | `Dp` | `AlertDialogDefaults.MaxWidth` | Maximum width of the panel. |
| `content` | `@Composable () -> Unit` | — | Dialog body, placed under the title and message. |

## Differences from the native dialog

COUI's `create(preferenceView, point)` also anchors the window to the row that opened it. A Compose-scaffold hosted dialog is one full-screen layer, so the port keeps the bottom placement and drops the row anchor.
