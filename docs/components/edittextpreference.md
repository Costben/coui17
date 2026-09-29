# EditTextPreference

`EditTextPreference` is a preference row that shows its value as trailing text and edits it in a bottom sheet, mirroring ColorOS's `COUIEditTextPreference`. On the device the row is the standard `coui_preference` layout with an `app:couiAssignment` value, and the editor is `COUIEditTextPreferenceDialogFragment`, built on `coui_preference_dialog_edittext.xml`. Because this port keeps the editor in the same visual language as the other choice preferences, it renders it through `OverlayBottomSheet` (like `ListPreference`) rather than an alert dialog — which also means the screen must be wrapped in a `Scaffold`.

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.EditTextPreference
```

## Basic Usage

The sheet holds a single-line `TextField` and a row of cancel/confirm `TextButton`s. The caller owns the value: edits are staged in the sheet and only committed through `onValueChange` when confirm is pressed.

```kotlin
var deviceName by remember { mutableStateOf("OnePlus PLK110") }

EditTextPreference(
    title = "Device name",
    value = deviceName,
    onValueChange = { deviceName = it },
    confirmButtonText = "OK",
    cancelButtonText = "Cancel",
    dialogTitle = "Device name",
    hint = "Up to 24 characters"
)
```

## Component States

### Empty Input

By default the confirm button stays disabled while the field is empty. Set `supportEmptyInput` to allow clearing the value:

```kotlin
EditTextPreference(
    title = "Nickname",
    value = nickname,
    onValueChange = { nickname = it },
    confirmButtonText = "OK",
    cancelButtonText = "Cancel",
    supportEmptyInput = true
)
```

### Disabled State

```kotlin
EditTextPreference(
    title = "Disabled field",
    value = "Cannot be edited",
    onValueChange = {},
    confirmButtonText = "OK",
    cancelButtonText = "Cancel",
    enabled = false
)
```

## Properties

### EditTextPreference Properties

| Property Name     | Type                            | Description                                                       | Default Value                         | Required |
| ----------------- | ------------------------------- | ----------------------------------------------------------------- | ------------------------------------- | -------- |
| title             | String                          | Preference title                                                  | -                                     | Yes      |
| value             | String                          | Current value, shown at the end of the row                        | -                                     | Yes      |
| onValueChange     | (String) -> Unit                | Callback invoked with the edited text when the sheet is confirmed | -                                     | Yes      |
| confirmButtonText | String                          | Label of the confirm button in the sheet                          | -                                     | Yes      |
| cancelButtonText  | String                          | Label of the cancel button in the sheet                           | -                                     | Yes      |
| modifier          | Modifier                        | Component modifier                                                | Modifier                              | No       |
| titleColor        | BasicComponentColors            | Title text color configuration                                    | BasicComponentDefaults.titleColor()   | No       |
| summary           | String?                         | Preference summary                                                | null                                  | No       |
| summaryColor      | BasicComponentColors            | Summary text color configuration                                  | BasicComponentDefaults.summaryColor() | No       |
| dialogTitle       | String                          | Title of the editing sheet                                        | title                                 | No       |
| hint              | String                          | Placeholder shown inside the field while it is empty              | ""                                    | No       |
| supportEmptyInput | Boolean                         | Whether an empty value may be confirmed                           | false                                 | No       |
| startAction       | @Composable (() -> Unit)?       | Custom start side content                                         | null                                  | No       |
| bottomAction      | @Composable (() -> Unit)?       | Custom bottom side content                                        | null                                  | No       |
| insideMargin      | PaddingValues                   | Component internal content padding                                | BasicComponentDefaults.InsideMargin   | No       |
| cardListPosition  | CardListPosition                | Row position inside its card group; rounded outer edges gain extra padding | CardListPosition.None           | No       |
| enabled           | Boolean                         | Component interactive state                                       | true                                  | No       |
| showValue         | Boolean                         | Whether the value is shown at the end of the row                  | true                                  | No       |
| renderInRootScaffold | Boolean                     | Whether the sheet is rendered in the root scaffold                | true                                  | No       |
| onExpandedChange  | ((Boolean) -> Unit)?            | Callback invoked when the sheet opens or closes                   | null                                  | No       |

### EditTextPreferenceDefaults

| Property               | Type | Description                                                                   | Value |
| ---------------------- | ---- | ----------------------------------------------------------------------------- | ----- |
| SheetHorizontalPadding | Dp   | Horizontal inset of the editor field, from `support_preference_dialog_edittext_margin_horizontal` | 24dp |
| ButtonBarTopPadding    | Dp   | Gap above the cancel/confirm row, from `coui_edittextdialog_marginbottom`     | 14dp  |
| ButtonBarBottomPadding | Dp   | Bottom padding below the cancel/confirm row                                   | 12dp  |
