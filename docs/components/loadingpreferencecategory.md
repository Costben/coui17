# LoadingPreferenceCategory

`LoadingPreferenceCategory` is a category header whose trailing widget is a loading indicator, mirroring ColorOS's `COUILoadingPreferenceCategory`. The device class extends `COUIPreferenceCategory` and swaps the category's `widget_frame` content through its `LoadingType`:

| `LoadingType`  | Widget                                                       |
| -------------- | ------------------------------------------------------------ |
| `LOADING` (0)  | `coui_preference_category_widget_layout_loading.xml`, indicator playing |
| `PAUSE` (1)    | The same layout, indicator paused                            |
| `INVISIBLE` (2) | The same layout, indicator `gone`                           |
| `AFTER_LOADING` (3) | The `coui_loading_after_layout` resource                |
| `BEFORE_LOADING` (4) | The `coui_loading_before_layout` resource               |

`PAUSE` and `INVISIBLE` are intentionally not modelled. Both keep whatever widget layout the category already had and only start or stop the `COUICompProgressIndicator`'s animation, so a Compose caller expresses them by not switching `loading` at all — there is no state to carry. The two resource-swapping types collapse into `widget`, which is what a caller would hand to either one.

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.LoadingPreferenceCategory
```

## Basic Usage

```kotlin
var loading by remember { mutableStateOf(true) }

LaunchedEffect(Unit) {
    delay(1200)
    loading = false
}

LoadingPreferenceCategory(
    title = "Battery",
    loading = loading,
    loadingText = "Reading battery stats…"
)
```

## Component States

### Bare Indicator

Leaving `loadingText` at `null` hides the label, leaving the bare 16dp indicator — exactly as the device's `TextUtils.isEmpty(mLoadingText)` branch does:

```kotlin
LoadingPreferenceCategory(
    title = "Scanning",
    loading = true
)
```

### After Loading

Once `loading` is false the category shows the `widget` slot, the port of `coui_loading_after_layout`:

```kotlin
LoadingPreferenceCategory(
    title = "Battery",
    loading = false,
    widget = {
        Text(
            text = "82%",
            color = COUITheme.colorScheme.onSurfaceSecondary
        )
    }
)
```

## Properties

### LoadingPreferenceCategory Properties

| Property Name | Type                      | Description                                                       | Default Value                             | Required |
| ------------- | ------------------------- | ----------------------------------------------------------------- | ----------------------------------------- | -------- |
| title         | String                    | Category title                                                    | -                                         | Yes      |
| modifier      | Modifier                  | Component modifier                                                | Modifier                                  | No       |
| loading       | Boolean                   | Whether the loading widget is shown (COUI `LoadingType.LOADING`)   | true                                      | No       |
| loadingText   | String?                   | Text before the indicator (COUI `text_in_loading`); null hides it   | null                                      | No       |
| widget        | (@Composable () -> Unit)? | Trailing widget shown while `loading` is false                     | null                                      | No       |
| titleColor    | Color                     | Title text color (`?couiColorSecondNeutral`)                       | COUITheme.colorScheme.onBackgroundVariant | No       |

### LoadingPreferenceCategoryDefaults

| Property      | Type | Description                                                                     | Value |
| ------------- | ---- | ------------------------------------------------------------------------------- | ----- |
| LoadingSize   | Dp   | `couiSmallLottieLoadingViewWidth` / `_Height`, the indicator's size              | 16dp  |
| TextMarginEnd | Dp   | `layout_marginEnd` of `text_in_loading` in the loading widget layout             | 4dp   |

## Notes

### Why It Reuses SmallTitle

The category title is `Widget.COUI.List.Category.Title`: `couiTextAppearanceSmallButton` (12sp medium) in `?couiColorSecondNeutral`, inset 32dp horizontally and 8dp vertically with a 16dp minimum height. The repo's `SmallTitle` already ports exactly that, so the header reuses it and mirrors its inset for the trailing widget instead of restating the text style.

### Deliberate Omissions

- **`LoadingType.PAUSE` and `LoadingType.INVISIBLE`.** The device uses them to pause or hide the indicator's Lottie animation without changing the widget layout. The repo's `CircularProgressIndicator` owns its own animation and is either drawn or not, so there is no separate paused state to expose.
