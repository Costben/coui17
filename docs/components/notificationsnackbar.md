# NotificationSnackBar

A notification-style snackbar aligned with the ColorOS 17 design language. It shows a leading notification image whose box follows the native `ImageType`, a two-line title, an optional two-line subtitle, an optional action button and an optional close icon pinned to the top-end corner. A host ([NotificationSnackBarHost] with [NotificationSnackBarHostState]) shows one at a time and clamps the display time to 4–10 seconds, like the native `make()`.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.NotificationSnackBar
import io.github.suqi8.coui.kmp.basic.NotificationSnackBarColors
import io.github.suqi8.coui.kmp.basic.NotificationSnackBarDefaults
import io.github.suqi8.coui.kmp.basic.NotificationSnackBarHost
import io.github.suqi8.coui.kmp.basic.NotificationSnackBarHostState
import io.github.suqi8.coui.kmp.basic.NotificationSnackBarImageType
```

## Basic Usage

```kotlin
val hostState = remember { NotificationSnackBarHostState() }

// Somewhere in the layout, pinned to the bottom:
NotificationSnackBarHost(state = hostState)

// Show one:
scope.launch {
    hostState.showNotification(
        title = "Charging",
        subtitle = "Battery is at 80%",
        icon = COUIIcons.WorldClock,
        actionText = "Details",
    )
}
```

## Image Types

`COUINotificationSnackBar.ImageType` has three geometries, all taken from the `coui_notification_snack_bar_*` dimensions:

```kotlin
NotificationSnackBar(
    title = "New message",
    subtitle = "Square image",
    icon = COUIIcons.Email,
    imageType = NotificationSnackBarImageType.Square, // 44dp x 44dp
)

NotificationSnackBar(
    title = "Now playing",
    subtitle = "Vertical image",
    icon = COUIIcons.Photos,
    imageType = NotificationSnackBarImageType.Vertical, // 36dp x 48dp
)

NotificationSnackBar(
    title = "Alarm set",
    subtitle = "Horizontal image",
    icon = COUIIcons.WorldClock,
    imageType = NotificationSnackBarImageType.Horizontal, // 58dp x 44dp
)
```

## Using the Snackbar on its own

`NotificationSnackBar` is the leaf the host renders; it can be placed directly when the app owns the timing:

```kotlin
NotificationSnackBar(
    title = "Sync failed",
    subtitle = "Check your network",
    actionText = "Retry",
    onAction = { retry() },
    onClose = { hide() },
)
```

## Properties

### NotificationSnackBar

| Property     | Type                            | Description                              | Default Value                     | Required |
| ------------ | ------------------------------- | ---------------------------------------- | --------------------------------- | -------- |
| title        | String                          | Title, up to two lines                   | -                                 | Yes      |
| modifier     | Modifier                        | Modifier applied to the snackbar         | Modifier                          | No       |
| subtitle     | String?                         | Optional secondary line                  | null                              | No       |
| icon         | ImageVector?                    | Optional leading notification image      | null                              | No       |
| imageType    | NotificationSnackBarImageType   | Box of `icon`                            | NotificationSnackBarImageType.Square | No     |
| actionText   | String?                         | Optional trailing action label           | null                              | No       |
| onAction     | (() -> Unit)?                   | Callback when the action is clicked      | null                              | No       |
| onClose      | (() -> Unit)?                   | Callback when the close icon is clicked  | null                              | No       |
| cornerRadius | Dp                              | Container corner radius                  | NotificationSnackBarDefaults.CornerRadius | No |
| colors       | NotificationSnackBarColors      | Snackbar colors                          | NotificationSnackBarDefaults.colors() | No    |

### NotificationSnackBarHostState

| Method                                                       | Description                                                                   |
| ------------------------------------------------------------ | ----------------------------------------------------------------------------- |
| `suspend showNotification(title, subtitle, icon, imageType, actionText, durationMillis, onAction, onClose)` | Shows a snackbar, replacing any visible one, and suspends until it is dismissed or its action is performed |

### NotificationSnackBarDefaults

| Constant               | Type    | Value   | COUI Source                                                    |
| ---------------------- | ------- | ------- | -------------------------------------------------------------- |
| CornerRadius           | Dp      | 16.dp   | `couiRoundCornerL` (`coui_round_corner_l`)                      |
| ContentMarginStart     | Dp      | 16.dp   | `coui_snack_bar_context_margin_start_with_icon`                 |
| ContentVerticalPadding | Dp      | 16.dp   | `coui_snack_bar_icon_margin_top_horizontal`                     |
| LayoutMarginEnd        | Dp      | 6.dp    | `coui_notification_snack_bar_layout_margin_end`                 |
| IconEndPadding         | Dp      | 8.dp    | `coui_notification_snack_bar_icon_end`                          |
| SubContentTopPadding   | Dp      | 2.dp    | `coui_notification_snack_bar_sub_content_top`                   |
| CloseSize              | Dp      | 20.dp   | `coui_notification_snack_bar_close_width`                       |
| ClosePadding           | Dp      | 2.dp    | `coui_notification_snack_bar_close_padding`                     |
| CloseStartMargin       | Dp      | 2.dp    | `coui_notification_snack_bar_close_start`                       |
| CloseEndMargin         | Dp      | 6.dp    | `coui_notification_snack_bar_close_end`                         |
| CloseTopMargin         | Dp      | 6.dp    | `coui_notification_snack_bar_close_top`                         |
| ActionMaxWidth         | Dp      | 120.dp  | `coui_snack_bar_button_width`                                   |
| ActionMinWidth         | Dp      | 52.dp   | `coui_btn_small_width_min`                                      |
| ActionMinHeight        | Dp      | 28.dp   | `coui_btn_small_height_min`                                     |
| ActionStartMargin      | Dp      | 12.dp   | `coui_snack_bar_action_margin_horizontal_start`                 |
| ActionPadding          | PaddingValues | 12dp x 4dp | `Widget.COUI.Button.Small` padding                         |
| BottomMargin           | Dp      | 32.dp   | `coui_snack_bar_margin_bottom`                                  |
| MinDurationMillis      | Long    | 4000    | `COUINotificationSnackBar.MIN_DURATION`                         |
| MaxDurationMillis      | Long    | 10000   | `COUINotificationSnackBar.MAX_DURATION`                         |
| ReturnSpring           | SpringSpec&lt;Float&gt; | response 0.35 / bounce 0 | `getViewSpringTranslationAnimator`     |

#### Image Boxes

| `ImageType` | `iconWidth`               | `iconHeight`                |
| ----------- | ------------------------- | --------------------------- |
| Square      | 44dp (`coui_notification_snack_bar_icon_width`) | 44dp (same) |
| Vertical    | 36dp (`..._vertical_icon_width`) | 48dp (`..._vertical_icon_height`) |
| Horizontal  | 58dp (`..._horizontal_icon_width`) | 44dp (`..._horizontal_icon_height`) |

### Colors

| COUI token                                   | Value                 | Used for         | Port                                    |
| -------------------------------------------- | --------------------- | ---------------- | --------------------------------------- |
| `attr/couiColorBackgroundTop` → `color/coui_color_background_top` | `#ffffffff` / `#ff333333` | Container fill | `surfaceContainerHighest` (exact in light, `#ff1e1e1e` in dark) |
| `attr/couiColorPrimaryNeutral` → `color/coui_color_primary_neutral` | `#e6000000` / `#e6ffffff` | Title | `onSurface`                         |
| `attr/couiColorSecondNeutral` → `color/coui_color_secondary_neutral` | `#8a000000` / `#8affffff` | Subtitle | `onSurfaceSecondary`             |
| `color/coui_snack_bar_action_text_selector` (`..._enable`) | `#ffffffff` | Action label | file constant              |
| `attr/couiColorLabelTertiary` → `color/coui_color_label_tertiary` | `#42000000` / `#4dffffff` | Close icon tint | `onSurfaceVariantActions` |

## Not Ported / Deviations

- The smooth-round corner of `OplusOutline.setSmoothRoundRect` (`couiRoundCornerLRadius` 13dp / `couiRoundCornerLWeight` 0.99), used when `RoundCornerUtil.isVersionSupport()` is true, is not ported; the component draws the `RoundedCornerShape` fallback the native uses otherwise (`outline.setRoundRect` with `couiRoundCornerL`).
- The swipe-to-dismiss gesture of `onInterceptTouchEvent` / `onTouchEvent` (1/8 of the measured width along X or a downward Y swipe dismisses, the cancel returns on the response 0.35 / bounce 0 spring exposed as `NotificationSnackBarDefaults.ReturnSpring`) is not ported.
- The `ShadowUtils.setElevationToView(..., 2, couiRoundCornerL, support_shadow_size_level_four, coui_snack_bar_background_shadow_color)` elevation is left to the caller's modifier rather than baked in.
- The "one snackbar at a time" scan of `make()` is replaced by `NotificationSnackBarHostState`, which dismisses the previous entry with `NotificationSnackBarResult.Dismissed`.
- The `ContextThemeWrapper(Theme.COUI_Main)` fallback of `make()` (used when the host theme does not resolve `couiColorBackgroundTop` / `couiColorPrimaryNeutral`) is not ported.
- The show / dismiss animation is a plain fade plus vertical slide: the native show path lives in `COUISnackBar` and the reference data does not expose its curve.
- `tv_snack_bar_action` of `coui_notification_snack_bar_item.xml` is dead in the notification path (the native uses the `bt_notification_snack_bar` button), so the port only implements the button form.
