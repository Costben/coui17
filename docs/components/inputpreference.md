# InputPreference

`InputPreference` is a card row whose input field is edited in place, mirroring ColorOS's `COUIInputPreference`. On the device the row is `coui_input_preference.xml` — an empty `edittext_container` that `COUIInputPreference.onBindViewHolder` fills with its own inner `COUICardListItemInputView`, a `COUIInputView` whose `mTitle` is the preference title and whose `COUIScrolledEditText` runs in line mode with `couiJustShowFocusLine`. There is no sheet: the field is part of the row, which is what separates this class from `EditTextPreference`.

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.InputPreference
```

## Basic Usage

```kotlin
var name by remember { mutableStateOf("COUI-Lab 17") }

InputPreference(
    title = "Bluetooth name",
    value = name,
    onValueChange = { name = it }
)
```

## Component States

### Length Limit and Clear Button

```kotlin
InputPreference(
    title = "Bluetooth name",
    value = name,
    onValueChange = { name = it },
    maxCount = 24,
    showClearButton = true
)
```

### Read-Only and Disabled

`readOnly` keeps the field selectable but rejects edits; `enabled = false` also greys the row out, matching `COUIInputPreference` calling `setEnabled(isEnabled())` on the field:

```kotlin
InputPreference(
    title = "Read-only field",
    value = "Cannot be edited",
    onValueChange = {},
    readOnly = true
)
```

```kotlin
InputPreference(
    title = "Disabled field",
    value = "Cannot be edited",
    onValueChange = {},
    enabled = false
)
```

## Properties

### InputPreference Properties

| Property Name    | Type                            | Description                                                       | Default Value                         | Required |
| ---------------- | ------------------------------- | ----------------------------------------------------------------- | ------------------------------------- | -------- |
| title            | String                          | Preference title, i.e. the field's floating label                 | -                                     | Yes      |
| value            | String                          | Current text value                                                | -                                     | Yes      |
| onValueChange    | (String) -> Unit                | Callback invoked on every edit                                    | -                                     | Yes      |
| modifier         | Modifier                        | Component modifier                                                | Modifier                              | No       |
| startAction      | @Composable (() -> Unit)?       | Custom start side content                                         | null                                  | No       |
| endActions       | @Composable (RowScope.() -> Unit)? | Custom end side content                                        | null                                  | No       |
| bottomAction     | @Composable (() -> Unit)?       | Custom bottom side content                                        | null                                  | No       |
| insideMargin     | PaddingValues                   | Component internal content padding                                | BasicComponentDefaults.InsideMargin   | No       |
| cardListPosition | CardListPosition                | Row position inside its card group; rounded outer edges gain extra padding | CardListPosition.None           | No       |
| enabled          | Boolean                         | Whether the field is editable                                     | true                                  | No       |
| readOnly         | Boolean                         | Whether the field rejects edits                                   | false                                 | No       |
| maxCount         | Int?                            | Maximum number of characters, when limited                        | null                                  | No       |
| showClearButton  | Boolean                         | Whether to show the clear button while the field is focused and not empty | false                          | No       |

## Advanced Usage

### Aligned With the Row Title

On the device the field's floating label is the preference title, so the label sits on the row's text column rather than above a separate block. This is why [InputPreference] renders its own title instead of accepting `BasicComponent`'s:

```kotlin
InputPreference(
    title = "Device name",
    value = deviceName,
    onValueChange = { deviceName = it },
    modifier = Modifier.heightIn(min = 52.dp)
)
```
