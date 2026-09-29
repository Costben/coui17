# CompleteState

`CompleteState` is the full-page completion state in COUI, shown once an operation has finished — an optimisation, a sync, a timed-out activation — mirroring ColorOS's COUICompleteStateView (coui_component_complete_state.xml): a 100dp playable, a title and an optional subtitle, all centered horizontally.

There is no button: the device page carries none, and leaves the exit to the caller. The device lays the view out through the responsive grid — its style sets `gridNumber = @integer/complete_page_grid_guide_column` (4) and `paddingType = cardListType` — so its width follows the screen size class rather than a fixed dp; pass the width you want through `modifier`.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.CompleteState
import io.github.suqi8.coui.kmp.basic.CompleteStateDefaults
import io.github.suqi8.coui.kmp.basic.CompleteStateStatus
```

## Basic Usage

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(COUITheme.colorScheme.surface),
    contentAlignment = Alignment.Center,
) {
    CompleteState(
        status = CompleteStateStatus.Success,
        title = "Optimised 12 settings",
        subtitle = "System state updated",
        modifier = Modifier.fillMaxWidth(),
    )
}
```

## Properties

### CompleteState Properties

| Property     | Type                    | Description                                        | Default Value                             | Required |
| ------------ | ----------------------- | -------------------------------------------------- | ----------------------------------------- | -------- |
| status       | CompleteStateStatus     | Which completion state the page shows              | -                                         | Yes      |
| title        | String                  | Centered title (20sp, medium)                      | -                                         | Yes      |
| modifier     | Modifier                | Modifier applied to the completion state           | Modifier                                  | No       |
| subtitle     | String?                 | Centered subtitle (12sp); hidden when null         | null                                      | No       |
| illustration | (@Composable () -> Unit)? | Playable above the text, in the 100dp box        | null                                      | No       |
| colors       | CompleteStateColors     | Text color configuration                           | CompleteStateDefaults.completeStateColors() | No     |

### CompleteStateStatus

| Value   | COUI constant             | Playable on the device                      | Duration |
| ------- | ------------------------- | ------------------------------------------- | -------- |
| Default | COMPLETE_TYPE_DEFAULT (0) | none                                        | 0ms      |
| Success | COMPLETE_TYPE_SUCCESS (1) | raw/complete_page_success_anim.json         | 1483ms   |
| Failed  | COMPLETE_TYPE_FAILED (2)  | raw/complete_page_fail_anim.json            | 1017ms   |
| Wait    | COMPLETE_TYPE_WAIT (3)    | raw/complete_page_wait_anim.json            | 3000ms   |

### CompleteStateDefaults Object

| Constant / Function            | Type     | Default Value   | COUI source                                 |
| ------------------------------ | -------- | --------------- | ------------------------------------------- |
| IllustrationSize               | Dp       | 100.dp          | complete_anim (100dp x 100dp)               |
| TextPaddingHorizontal          | Dp       | 8.dp            | complete_page_text_padding_hor              |
| TitleMarginTop                 | Dp       | 8.dp            | complete_page_text_title_margin_top         |
| SubtitleMarginTop              | Dp       | 8.dp            | complete_page_text_subtitle_margin_top      |
| SuccessDuration                | Duration | 1483ms          | complete_page_success_anim.json (89 frames at 60fps) |
| FailedDuration                 | Duration | 1017ms          | complete_page_fail_anim.json (61 frames at 60fps) |
| WaitDuration                   | Duration | 3000ms          | complete_page_wait_anim.json (180 frames at 60fps) |
| animationDuration(status)      | Duration | per status      | Lottie `op` / `fr` of each raw              |

`CompleteStateDefaults.animationDuration(status)` returns how long that status's playable runs, which is what a caller times the page with.

### `completeStateColors()` factory

| Parameter     | Type  | Default                                  | COUI role               |
| ------------- | ----- | ---------------------------------------- | ----------------------- |
| titleColor    | Color | COUITheme.colorScheme.onSurface          | couiColorLabelPrimary   |
| subtitleColor | Color | COUITheme.colorScheme.onSurfaceSecondary | couiColorLabelSecondary |

### CompleteStateColors Class

| Property Name | Type  | Description           |
| ------------- | ----- | --------------------- |
| titleColor    | Color | Color of the title    |
| subtitleColor | Color | Color of the subtitle |

## Behavior

- The illustration box is 100dp x 100dp and centered. The device always measures it at that size, even in `CompleteStateStatus.Default` where no playable is set, and always starts it (`anim_autoPlay` defaults to true); the port reserves the box only when an `illustration` is supplied, because the library ships no animation to fill it with.
- The playables are oplus-anim (Lottie) files on a 300x300 canvas, scaled into the 100dp box. None of the three has a night variant, so the illustration is the same drawing in both themes while the text follows the theme colors.
- The title and the subtitle fill the width and are padded by 8dp on each side (`complete_page_text_padding_hor`), with 8dp above each of them.
- The page draws no button and no scrim: on the device the caller swaps it in for the page content and takes it back out.

## Advanced Usage

### Timing the Page After Its Playable

```kotlin
var status by remember { mutableStateOf(CompleteStateStatus.Wait) }

LaunchedEffect(status) {
    delay(CompleteStateDefaults.animationDuration(status))
    /* Leave the page */
}
```

### Custom Colors

```kotlin
CompleteState(
    status = CompleteStateStatus.Failed,
    title = "Couldn't finish",
    subtitle = "Check your connection and try again",
    colors = CompleteStateDefaults.completeStateColors(
        titleColor = COUITheme.colorScheme.error
    ),
)
```
