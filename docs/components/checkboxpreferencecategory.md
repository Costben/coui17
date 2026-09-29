# CheckBoxPreferenceCategory

`CheckBoxPreferenceCategory` is a category header whose trailing widget is a checkbox, mirroring ColorOS's `COUICheckBoxPreferenceCategory`. The device class extends `COUIPreferenceCategory` and binds `coui_preference_category_widget_layout_checkbox.xml` into the category's `widget_frame`: a bare `COUICheckBox` (id `android.R.id.checkbox`) with a `paddingEnd` of 16dp and a `layout_marginVertical` of 4dp.

The title itself stays a plain label: `COUIPreferenceCategory` keeps the row non-clickable and only the box reacts, which is why there is no `onClick` for the row here.

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.CheckBoxPreferenceCategory
```

## Basic Usage

The natural use is a group-level "select all": the box reads `Indeterminate` while only part of the group is on, which is exactly what `COUICheckBox`'s tri-state is for.

```kotlin
val allOn = itemA && itemB && itemC
val allOff = !itemA && !itemB && !itemC

CheckBoxPreferenceCategory(
    title = "Notifications",
    state = when {
        allOn -> ToggleableState.On
        allOff -> ToggleableState.Off
        else -> ToggleableState.Indeterminate
    },
    onClick = {
        val turnOn = !allOn
        itemA = turnOn
        itemB = turnOn
        itemC = turnOn
    }
)
```

## Component States

### Checked and Unchecked

```kotlin
CheckBoxPreferenceCategory(
    title = "All on",
    state = ToggleableState.On,
    onClick = {}
)
```

```kotlin
CheckBoxPreferenceCategory(
    title = "All off",
    state = ToggleableState.Off,
    onClick = {}
)
```

### Disabled State

```kotlin
CheckBoxPreferenceCategory(
    title = "Disabled category",
    state = ToggleableState.On,
    onClick = {},
    enabled = false
)
```

## Properties

### CheckBoxPreferenceCategory Properties

| Property Name  | Type                      | Description                                                       | Default Value                             | Required |
| -------------- | ------------------------- | ----------------------------------------------------------------- | ----------------------------------------- | -------- |
| title          | String                    | Category title                                                    | -                                         | Yes      |
| state          | ToggleableState           | Current state of the checkbox                                     | -                                         | Yes      |
| onClick        | (() -> Unit)?             | Callback when the checkbox is clicked; the caller updates `state`  | -                                         | Yes      |
| modifier       | Modifier                  | Component modifier                                                | Modifier                                  | No       |
| titleColor     | Color                     | Title text color (`?couiColorSecondNeutral`)                       | COUITheme.colorScheme.onBackgroundVariant | No       |
| checkboxColors | CheckboxColors            | Checkbox control color configuration                              | CheckboxDefaults.checkboxColors()         | No       |
| enabled        | Boolean                   | Whether the checkbox is enabled                                   | true                                      | No       |

### CheckBoxPreferenceCategoryDefaults

| Property            | Type | Description                                                                       | Value |
| ------------------- | ---- | --------------------------------------------------------------------------------- | ----- |
| WidgetMarginVertical | Dp  | `coui_preference_category_widget_layout_margin_vertical`, the checkbox's vertical margin | 4dp |

## Notes

### Why the State Is Tri-State

`COUICheckBox` is a tri-state view and the device's `OnStateChangeListener` reports all three states, so `state` is a `ToggleableState` rather than a `Boolean`. A group-level box that reflects partial selection needs `Indeterminate`; a `Boolean` parameter could not express it.

### Deliberate Omissions

- **`default_checkbox_state`.** The device reads it from the inflated attributes to pick the initial state. In Compose the caller owns the state, so the attribute has no parameter to map onto.
