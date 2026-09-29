# LoadInstallProgressPreference

`LoadInstallProgressPreference` is a preference row whose trailing widget is an `InstallLoadProgress` button, mirroring ColorOS's `COUILoadInstallProgressPreference`. The device class extends `COUIPreference` and relies on the row style `Preference_COUI_COUILoadInstallProgressPreference` to inflate `coui_preference_load_progress.xml` into the standard row's `widget_frame`: a bare `COUIInstallLoadProgress` with `style="Widget.COUI.COUILoadProgress.InstallDownloadSmall"`, i.e. `InstallLoadStyle.Default` (the 52dp x 28dp pill). `onBindViewHolder` then copies the preference's text, text size, colour state lists, `max`, `progress` and `state` onto it.

The device's `setState` is called by the settings app, not by the button: `COUILoadProgress` toggles itself on click and reports the new state through `OnStateChangeListener`, which the preference forwards. In Compose the state is a parameter, so there is no listener — `onClick` is the click and the caller moves `state`.

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.LoadInstallProgressPreference
import io.github.suqi8.coui.kmp.basic.LoadProgressState
```

## Basic Usage

```kotlin
var progress by remember { mutableIntStateOf(0) }
var state by remember { mutableStateOf(LoadProgressState.Default) }

LoadInstallProgressPreference(
    title = "System update",
    summary = "Up to date",
    state = state,
    progress = progress,
    progressText = if (state == LoadProgressState.Loading) "Pause" else "Check for updates",
    onClick = { /* start or pause the download */ }
)
```

## Component States

### Downloading

While the state is `Loading`, the pill fills with `progress` and the label is re-drawn where it overlaps the filled part:

```kotlin
LoadInstallProgressPreference(
    title = "System update",
    state = LoadProgressState.Loading,
    progress = 40,
    max = 100,
    progressText = "Pause",
    onClick = {}
)
```

### Other States

```kotlin
LoadInstallProgressPreference(
    title = "Install",
    state = LoadProgressState.Wait,
    progressText = "Retry",
    onClick = {}
)
```

### Disabled State

```kotlin
LoadInstallProgressPreference(
    title = "Disabled",
    state = LoadProgressState.Default,
    progressText = "Check",
    onClick = {},
    enabled = false
)
```

## Properties

### LoadInstallProgressPreference Properties

| Property Name    | Type                          | Description                                                       | Default Value                          | Required |
| ---------------- | ----------------------------- | ----------------------------------------------------------------- | -------------------------------------- | -------- |
| title            | String                        | Preference title                                                  | -                                      | Yes      |
| onClick          | () -> Unit                    | Callback when the progress button is pressed and released inside its bounds | -                             | Yes      |
| modifier         | Modifier                      | Component modifier                                                | Modifier                               | No       |
| titleColor       | BasicComponentColors          | Title text color configuration                                    | BasicComponentDefaults.titleColor()    | No       |
| summary          | String?                       | Preference summary                                                | null                                   | No       |
| summaryColor     | BasicComponentColors          | Summary text color configuration                                  | BasicComponentDefaults.summaryColor()  | No       |
| state            | LoadProgressState             | Current state (COUI `mState`)                                     | LoadProgressState.Default              | No       |
| progress         | Int                           | Current progress, clamped to `0..max` (COUI `mProgress`)           | LoadProgressButtonDefaults.PROGRESS    | No       |
| max              | Int                           | Value `progress` is measured against (COUI `mMax`)                | LoadProgressButtonDefaults.MAX         | No       |
| progressText     | String                        | Label of the button (COUI `progressText`)                         | LoadProgressButtonDefaults.DEFAULT_TEXT | No      |
| style            | InstallLoadStyle              | The `couiStyle` tier; the device row uses `Default` (52x28dp pill) | InstallLoadStyle.Default              | No       |
| colors           | InstallLoadProgressColors     | Button colors                                                     | InstallLoadProgressDefaults.colors()   | No       |
| startAction      | @Composable (() -> Unit)?     | Custom start side content                                         | null                                   | No       |
| endActions       | @Composable RowScope.() -> Unit | Custom end side content, placed before the progress button      | {}                                     | No       |
| bottomAction     | @Composable (() -> Unit)?     | Custom bottom side content                                        | null                                   | No       |
| insideMargin     | PaddingValues                 | Component internal content padding                                | BasicComponentDefaults.InsideMargin    | No       |
| cardListPosition | CardListPosition              | Row position inside its card group; rounded outer edges gain extra padding | CardListPosition.None            | No       |
| holdDownState    | Boolean                       | Whether the component is held down                                | false                                  | No       |
| enabled          | Boolean                       | Whether the progress button is enabled                            | true                                   | No       |
| hapticFeedback   | Boolean                       | Whether the button vibrates on press and release                  | false                                  | No       |
| downloadingContentDescription | String?          | Accessibility description used instead of `progressText` while loading | null                              | No       |

## Notes

### Deliberate Omissions

- **`OnStateChangeListener`.** The device's `COUILoadProgress.toggle()` changes the state itself and notifies through this listener, which the preference re-exposes. In Compose the port's `InstallLoadProgress` never changes state on its own, so a listener would have nothing to fire it — the caller owns `state` and moves it from `onClick`.
- **`progressTextSize` and the three colour state lists.** The device exposes setters for the text size and for `ColorStateList` variants of the text, background and install colours. `InstallLoadProgress` resolves all of those internally from the theme, so only `colors` is surfaced.
