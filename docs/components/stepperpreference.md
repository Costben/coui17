# StepperPreference

`StepperPreference` is a preference row with a stepper control in its widget frame, mirroring ColorOS's `COUIStepperPreference`. The device inflates `coui_preference_widget_stepper_view.xml` — a bare `COUIStepperView` — into the standard `coui_preference` row, so the row keeps the usual title/summary layout and the stepper sits at the tail. It is meant for numeric settings such as font size or a repeat count.

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.StepperPreference
```

## Basic Usage

```kotlin
var level by remember { mutableIntStateOf(2) }

StepperPreference(
    value = level,
    onValueChange = { level = it },
    title = "Font size",
    summary = listOf("Small", "Default", "Large", "Extra large")[level],
    minValue = 0,
    maxValue = 3
)
```

## Component States

### Bounded Range

The stepper disables the button that would leave the range, so the row cannot produce a value outside `minValue..maxValue`:

```kotlin
StepperPreference(
    value = 0,
    onValueChange = {},
    title = "Minimum reached",
    minValue = 0,
    maxValue = 10
)
```

### Disabled State

```kotlin
StepperPreference(
    value = 3,
    onValueChange = {},
    title = "Disabled stepper",
    enabled = false
)
```

## Properties

### StepperPreference Properties

| Property Name    | Type                          | Description                                                        | Default Value                         | Required |
| ---------------- | ----------------------------- | ------------------------------------------------------------------ | ------------------------------------- | -------- |
| value            | Int                           | Current value of the stepper                                       | -                                     | Yes      |
| onValueChange    | (Int) -> Unit                 | Callback invoked with the new value when the user steps up or down | -                                     | Yes      |
| title            | String                        | Preference title                                                   | -                                     | Yes      |
| modifier         | Modifier                      | Component modifier                                                 | Modifier                              | No       |
| titleColor       | BasicComponentColors          | Title text color configuration                                     | BasicComponentDefaults.titleColor()   | No       |
| summary          | String?                       | Preference summary                                                 | null                                  | No       |
| summaryColor     | BasicComponentColors          | Summary text color configuration                                   | BasicComponentDefaults.summaryColor() | No       |
| minValue         | Int                           | Lowest value the stepper can reach (COUI `couiMinimum`)            | StepperPreferenceDefaults.MinValue    | No       |
| maxValue         | Int                           | Highest value the stepper can reach (COUI `couiMaximum`)           | StepperPreferenceDefaults.MaxValue    | No       |
| step             | Int                           | Increment applied on each button press (COUI `couiUnit`)           | StepperPreferenceDefaults.Step        | No       |
| startAction      | @Composable (() -> Unit)?     | Custom start side content                                          | null                                  | No       |
| endActions       | @Composable (RowScope.() -> Unit)? | Custom end side content, placed before the stepper            | null                                  | No       |
| stepperColors    | StepperColors                 | Stepper control color configuration                                | StepperDefaults.stepperColors()       | No       |
| bottomAction     | @Composable (() -> Unit)?     | Custom bottom side content                                         | null                                  | No       |
| insideMargin     | PaddingValues                 | Component internal content padding                                 | BasicComponentDefaults.InsideMargin   | No       |
| cardListPosition | CardListPosition              | Row position inside its card group; rounded outer edges gain extra padding | CardListPosition.None          | No       |
| holdDownState    | Boolean                       | Whether the component is held down                                 | false                                 | No       |
| enabled          | Boolean                       | Component interactive state                                        | true                                  | No       |

### StepperPreferenceDefaults

| Property | Type | Description                                                       | Value |
| -------- | ---- | ----------------------------------------------------------------- | ----- |
| MinValue | Int  | `ObservableStep.MIN_VALUE`, the default of `couiMinimum`          | -999  |
| MaxValue | Int  | `ObservableStep.MAX_VALUE`, the default of `couiMaximum`          | 9999  |
| Step     | Int  | Default of `couiUnit`; `plus()` adds it, `minus()` subtracts it   | 1     |

## Advanced Usage

### With a Start Icon

```kotlin
StepperPreference(
    value = repeatCount,
    onValueChange = { repeatCount = it },
    title = "Repeat",
    minValue = 1,
    maxValue = 99,
    startAction = {
        Icon(
            imageVector = COUIIcons.Refresh,
            contentDescription = null,
            tint = COUITheme.colorScheme.onSurfaceSecondary
        )
    }
)
```
