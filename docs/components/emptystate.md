# EmptyState

`EmptyState` is the empty page component in COUI, shown when a list or a search has nothing to display, mirroring ColorOS's COUIEmptyStateView (coui_component_empty_state.xml): a centered illustration, a title and a subtitle inside a max-height scroll area, and an optional accent-colored action below them.

The content group is not centered vertically. The device places it at a fraction of the free space — 0.5 of it in a short window and 0.45 otherwise — so on a phone-sized page it sits a little above the middle.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.EmptyState
import io.github.suqi8.coui.kmp.basic.EmptyStateDefaults
import io.github.suqi8.coui.kmp.basic.EmptyStateSize
```

## Basic Usage

```kotlin
EmptyState(
    title = "No results",
    subtitle = "Try searching for WLAN, Bluetooth or Battery",
    actionText = "Clear search",
    onActionClick = { /* Clear the query */ },
    modifier = Modifier.fillMaxSize(),
)
```

Give the component a fixed height (`fillMaxSize`, `height`, a `weight`, …) to get the device's placement; when its height is only bounded or unbounded it lays its content out at the top, exactly like the device falls back to the content height whenever its height measure spec is not exactly specified.

## Properties

### EmptyState Properties

| Property      | Type                       | Description                                                       | Default Value                                | Required |
| ------------- | -------------------------- | ----------------------------------------------------------------- | -------------------------------------------- | -------- |
| title         | String                     | Centered title, at most two lines (16sp, medium)                   | -                                            | Yes      |
| modifier      | Modifier                   | Modifier applied to the empty state                                | Modifier                                     | No       |
| subtitle      | String?                    | Centered subtitle, at most three lines (12sp); hidden when null    | null                                         | No       |
| actionText    | String?                    | Accent-colored action below the content (14sp, medium); hidden when null | null                                   | No       |
| onActionClick | (() -> Unit)?              | Callback of the action                                             | null                                         | No       |
| illustration  | (@Composable () -> Unit)?  | Illustration above the text, in the 120dp x 100dp box              | null                                         | No       |
| size          | EmptyStateSize             | Size bucket, `Auto` resolves it from the constraints               | EmptyStateSize.Auto                          | No       |
| colors        | EmptyStateColors           | Text color configuration                                           | EmptyStateDefaults.emptyStateColors()        | No       |

### EmptyStateSize

| Value  | COUI constant                   | Description                                                      |
| ------ | ------------------------------- | ---------------------------------------------------------------- |
| Auto   | EMPTY_VIEW_SIZE_TYPE_AUTO (0)   | Buckets the measured size with the thresholds below              |
| Small  | EMPTY_VIEW_SIZE_TYPE_SMALL (1)  | Window shorter than 200dp; hides the illustration                |
| Medium | EMPTY_VIEW_SIZE_TYPE_MEDIUM (2) | Window narrower than 320dp or shorter than 480dp                 |
| Large  | EMPTY_VIEW_SIZE_TYPE_LARGE (3)  | Everything else                                                  |

`EmptyStateDefaults.sizeType(width, height)` resolves the bucket; `EmptyStateDefaults.marginTopHeightRatio(size)` returns the share of the free space kept above the content.

### EmptyStateDefaults Object

| Constant                       | Type  | Default Value | COUI source                                          |
| ------------------------------ | ----- | ------------- | ---------------------------------------------------- |
| IllustrationWidth              | Dp    | 120.dp        | coui_component_empty_anim_view_width_normal          |
| IllustrationHeight             | Dp    | 100.dp        | coui_component_empty_anim_view_height_normal         |
| ContentWidth                   | Dp    | 280.dp        | coui_component_empty_group_width                     |
| ContentMaxHeight               | Dp    | 92.dp         | coui_component_empty_text_scroll_max_height          |
| SubtitleMarginTop              | Dp    | 2.dp          | coui_component_empty_view_subtitle_margin_top        |
| ActionMarginTop                | Dp    | 8.dp          | written inline in coui_component_empty_state.xml     |
| ActionRipplePaddingHorizontal  | Dp    | 12.dp         | text_ripple_bg_padding_horizontal                    |
| ActionRipplePaddingVertical    | Dp    | 4.dp          | text_ripple_bg_padding_vertical                      |
| MarginTopHeightRatioSmall      | Float | 0.5f          | HEIGHT_RATIO_SMALL                                   |
| MarginTopHeightRatioNormal     | Float | 0.45f         | HEIGHT_RATIO_NORMAL                                  |
| HeightThresholdSmall           | Dp    | 200.dp        | coui_component_height_threshold_small                |
| HeightThresholdMedium          | Dp    | 480.dp        | coui_component_height_threshold_medium               |
| WidthThresholdMedium           | Dp    | 320.dp        | coui_component_width_threshold_medium                |

### `emptyStateColors()` factory

| Parameter     | Type  | Default                                   | COUI role                 |
| ------------- | ----- | ----------------------------------------- | ------------------------- |
| titleColor    | Color | COUITheme.colorScheme.onSurface           | couiColorLabelPrimary     |
| subtitleColor | Color | COUITheme.colorScheme.onSurfaceSecondary  | couiColorLabelSecondary   |
| actionColor   | Color | COUITheme.colorScheme.primary             | couiColorPrimaryText      |

### EmptyStateColors Class

| Property Name | Type  | Description             |
| ------------- | ----- | ----------------------- |
| titleColor    | Color | Color of the title      |
| subtitleColor | Color | Color of the subtitle   |
| actionColor   | Color | Color of the action     |

## Behavior

- The illustration box is 120dp wide and 100dp high, and it is hidden entirely in `EmptyStateSize.Small`: the device zeroes the animation size there and the view drops out of the layout.
- The title and the subtitle share a 280dp column and scroll once their combined height passes 92dp (coui_component_empty_text_scroll_max_height), with no scrollbar — the device wraps them in a COUIMaxHeightScrollView with `android:scrollbars="none"`.
- The title is capped at two lines and the subtitle at three, both ellipsized at the end.
- The action is a bare accent-colored text with 12dp x 4dp of padding, and its ripple is the COUI `?couiColorPress` mask the device installs with `COUITextViewCompatUtil.setPressRippleDrawable`. It carries the button accessibility role, like the device's `BUTTON_CLASS_NAME` delegate.
- The library ships no illustration. COUI's `rawAnimRes` and `imageRes` both default to -1 and `coui_component_empty_state.xml` sets neither, so on the device the box is filled with an oplus-anim passed to `COUIEmptyStateView#setRawAnimRes` (or a still image passed to `setImageRes`). Pass your own composable through `illustration`; nothing is drawn while it is null.

## Advanced Usage

### Search Results Page

ColorOS's Gallery search page (`search_result_fragment.xml`) hosts the empty view under its result list with a title only, auto-playing `raw/base_empty_view_search_result`:

```kotlin
EmptyState(
    title = "No results",
    illustration = { /* Your empty-state animation or image */ },
)
```

### Custom Colors

```kotlin
EmptyState(
    title = "No results",
    subtitle = "Try searching for WLAN, Bluetooth or Battery",
    actionText = "Clear search",
    onActionClick = { /* Clear the query */ },
    colors = EmptyStateDefaults.emptyStateColors(
        actionColor = COUITheme.colorScheme.primaryContainer
    ),
)
```

`actionColor` follows the theme's primary. COUI resolves couiColorPrimaryText per accent theme: `coui_color_primary_text_blue` is #0080FF, which the theme's light primary matches exactly, while the theme's night primary is #1A8CFF against the token's #5C9DFF. Only the night value is off.
