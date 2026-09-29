# IntentSeekBar

A seek bar carrying a third, **secondary progress** layer between the track and the played progress, mirroring ColorOS's `COUIIntentSeekBar` (`com/coui/appcompat/seekbar/COUIIntentSeekBar`). It is what ColorOS draws for media playback: a translucent overlay bar that stays legible on top of video, with a buffered range sitting under the played range.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.IntentSeekBar
import io.github.suqi8.coui.kmp.basic.IntentSeekBarColors
import io.github.suqi8.coui.kmp.basic.IntentSeekBarDefaults
import io.github.suqi8.coui.kmp.basic.IntentSeekBarSize
```

## Basic Usage

```kotlin
var played by remember { mutableFloatStateOf(0.35f) }
var buffered by remember { mutableFloatStateOf(0.65f) }

IntentSeekBar(
    value = played,
    onValueChange = {
        played = it
        if (buffered < it) buffered = it
    },
    secondaryProgress = buffered,
    showThumb = true,
    modifier = Modifier.fillMaxWidth(),
)
```

The gesture model is the same one [Slider](slider) uses — press anywhere on the bar to seek, drag to scrub, edge haptics at the ends, keyboard and accessibility `setProgress` support — because `COUIIntentSeekBar` shares `COUISeekBar`'s core with the plain seek bar. What makes it a separate component is the second progress layer and its own colour model: its colours are deliberately *not* [SliderColors](slider), and it keeps a second, theme-independent palette for drawing over video.

`secondaryProgress` is coerced into `valueRange` **and** clamped to `value`, so the buffered bar never runs past the played bar — the native renders both inside one clip of the track capsule, secondary first.

## Sizes

`IntentSeekBarSize` (`com/coui/appcompat/seekbar/IntentSeekBarSize`, selected by `attr/couiIntentSeekBarSize`: 0 = `Default`, 1 = `Small`, 2 = `Large`) is a triplet of track height, pressed background height, resting thumb radius and pressed thumb radius:

```kotlin
IntentSeekBar(
    value = value,
    onValueChange = { value = it },
    secondaryProgress = 0.75f,
    size = IntentSeekBarSize.Small,
)
```

## Drawing over Video

`IntentSeekBarDefaults.fixedColors()` returns the `couiIntentSeekBarFollowSystemTheme = false` palette — the fixed translucent white overlay colours — for a bar drawn on top of video or a wallpaper, where theme colours would be unreadable:

```kotlin
IntentSeekBar(
    value = value,
    onValueChange = { value = it },
    secondaryProgress = 0.8f,
    colors = IntentSeekBarDefaults.fixedColors(),
    showThumb = true,
)
```

In the native component the mode switches automatically as soon as the caller sets `couiSeekBarBackgroundColor`, `couiSeekBarProgressColor` or `couiSeekBarSecondaryProgressColor`; this port expresses that by letting you pass a different `IntentSeekBarColors`. `IntentSeekBarDefaults.fallbackColors()` is the third palette, `COUIIntentSeekBar.getThemeFallbackColors`, used only when the theme does not define `couiColorLabelPrimary` / `couiColorLabelQuaternary`.

## Properties

| Property              | Type                                    | Description                                                        | Default Value                       | Required |
| --------------------- | --------------------------------------- | ------------------------------------------------------------------ | ----------------------------------- | -------- |
| value                 | Float                                   | The current played value, coerced into `valueRange`                | -                                   | Yes      |
| onValueChange         | (Float) -> Unit                         | Called while the user scrubs the played value                      | -                                   | Yes      |
| modifier              | Modifier                                | Modifier applied to the seek bar                                   | Modifier                            | No       |
| enabled               | Boolean                                 | Whether the seek bar reacts to input                               | true                                | No       |
| valueRange            | ClosedFloatingPointRange&lt;Float&gt;   | The range of values the bar can take                               | 0f..1f                              | No       |
| secondaryProgress     | Float                                   | The buffered value drawn under `value`                             | valueRange.start                    | No       |
| steps                 | Int                                     | Discrete values between the ends of `valueRange`; 0 = continuous   | 0                                   | No       |
| onValueChangeFinished | (() -> Unit)?                           | Called when a drag ends                                            | null                                | No       |
| showSecondaryProgress | Boolean                                 | Whether the secondary layer is drawn                               | true                                | No       |
| showThumb             | Boolean                                 | Whether the thumb is drawn                                         | false                               | No       |
| size                  | IntentSeekBarSize                       | The track / thumb triplet                                          | IntentSeekBarSize.Default           | No       |
| colors                | IntentSeekBarColors                     | The colours                                                        | IntentSeekBarDefaults.colors()      | No       |

### IntentSeekBarSize

| Constant | trackHeight                | maxBackgroundHeight        | thumbRadius                | thumbMaxRadius                 |
| -------- | -------------------------- | -------------------------- | -------------------------- | ------------------------------ |
| Default  | 4.dp (`_height_default`)   | 6.dp (`_max_..._default`)  | 4.dp (`_thumb_radius_default`) | 5.dp (`_thumb_max_radius_default`) |
| Small    | 2.dp (`_height_small`)     | 10.dp (`_max_..._small`)   | 3.dp (`_thumb_radius_small`)   | 8.dp (`_thumb_max_radius_small`)   |
| Large    | 6.dp (`_height_large`)     | 10.dp (`_max_..._large`)   | 5.dp (`_thumb_radius_large`)   | 8.dp (`_thumb_max_radius_large`)   |

All eight come from `dimen/coui_seekbar_intent_height_*`, `coui_seekbar_intent_max_background_height_*`, `coui_seekbar_intent_thumb_radius_*` and `coui_seekbar_intent_thumb_max_radius_*`. The layout height of the bar is the native `onMeasure`: `max(maxBackgroundHeight, thumbMaxRadius * 2, MIN_TOUCH_TARGET_HEIGHT_DP)` with `MIN_TOUCH_TARGET_HEIGHT_DP = 24.0f`, so the bar is always at least 24dp tall.

### IntentSeekBarDefaults

| Constant                   | Type    | Value       | COUI Source                                     |
| -------------------------- | ------- | ----------- | ----------------------------------------------- |
| ConicLightColor            | Int     | 0xFFF87796  | attr/couiIntentSeekBarConicLightColor default   |
| FixedBackgroundColor       | Color   | #4dffffff   | color/coui_seekbar_intent_background_color_fixed |
| FixedProgressColor         | Color   | #8affffff   | color/coui_seekbar_intent_progress_color_fixed  |
| FixedSecondaryProgressColor | Color  | #29ffffff   | color/coui_seekbar_intent_secondary_progress_color_fixed |
| FixedThumbColor            | Color   | #ffffffff   | color/coui_seekbar_intent_thumb_color_fixed     |

#### Methods

| Method Name      | Type               | Description                                                                                        |
| ---------------- | ------------------ | -------------------------------------------------------------------------------------------------- |
| colors()         | IntentSeekBarColors | The `couiIntentSeekBarFollowSystemTheme = true` palette, resolved from the theme                   |
| fixedColors()    | IntentSeekBarColors | The `followSystemTheme = false` fixed translucent overlay palette                                   |
| fallbackColors() | IntentSeekBarColors | `getThemeFallbackColors`: the plain seek bar colours, used when the theme lacks the label attributes |

### Colours

| COUI token                                                        | Value                 | Used for            | Port                                 |
| ----------------------------------------------------------------- | --------------------- | ------------------- | ------------------------------------ |
| attr/couiColorLabelQuaternary → color/coui_color_label_quaternary | #14000000 / #26ffffff | Track background    | secondaryVariant                     |
| attr/couiColorLabelPrimary → color/coui_color_label_primary       | #e6000000 / #e6ffffff | Played progress     | onSurface                            |
| attr/couiColorLabelQuaternary → color/coui_color_label_quaternary | #14000000 / #26ffffff | Secondary progress  | secondaryVariant                     |
| color/coui_seekbar_intent_thumb_color                             | #ff000000 / #ffffffff | Thumb               | derived from theme background luminance |
| color/coui_seekbar_intent_background_color_fixed                  | #4dffffff             | Track background, fixed mode | file constant                |
| color/coui_seekbar_intent_progress_color_fixed                    | #8affffff             | Played progress, fixed mode  | file constant                |
| color/coui_seekbar_intent_secondary_progress_color_fixed          | #29ffffff             | Secondary progress, fixed mode | file constant              |
| color/coui_seekbar_intent_thumb_color_fixed                       | #ffffffff             | Thumb, fixed mode   | file constant                        |
| color/coui_seekbar_background_color_normal                        | #0d000000             | Fallback background | file constant                        |
| color/coui_seekbar_progress_color_normal                          | #ff4d4d4d / #ff999999 | Fallback progress   | file constant                        |
| color/coui_seekbar_secondary_progress_color                       | #4dffffff             | Fallback secondary progress | file constant                |

The secondary progress shares `?attr/couiColorLabelQuaternary` with the track background, which is `0.08` alpha black — the native draws the secondary layer inside the same track capsule, so the two alpha layers stack visually. All four `fixed*` colours are what `style/COUIIntentSeekBar` and `style/COUIIntentSeekBar.Dark` point their `couiSeekBarSecondaryProgressColor` / `couiSeekBarThumbColor` at (`@color/coui_seekbar_secondary_progress_color` and `@color/coui_seekbar_progress_selector`); both styles also set `couiSeekBarBackgroundHeight` and `couiSeekBarProgressHeight` to 4dp, matching `IntentSeekBarSize.Default`.

## Behavior

- Layer order is `drawBackground` + `drawActiveTrack`: the full-width track capsule, then the secondary progress, then the played progress, with both progress bars clipped to the track capsule. The background is full width because `COUIIntentSeekBar.getBackgroundHorizontalInset` returns `0.0f`; the progress capsules are inset by half the current cap height so their round ends stay inside the bar.
- The played progress follows the finger on a fast spring (`dampingRatio = 0.9f`, `stiffness = 1755f`) while dragging and settles on a slower one (`dampingRatio = 0.96f`, `stiffness = 322f`), matching `COUISeekBarAdvanced.useDragTransitionAnimation()`, which returns true for the intent bar.
- The secondary progress animates on a critically damped response-0.5 spring (`SECONDARY_PROGRESS_SPRING_RESPONSE` 0.5 / `SECONDARY_PROGRESS_SPRING_BOUNCE` 0.0, `COUIIntentSeekBar.initSecondaryProgressAnim`; `stiffness = (2 * PI / 0.5)² = 158`).
- With `steps > 0` the value snaps to the nearest of `steps + 2` key points and the edge haptic fires at the ends, both taken from [Slider](slider)'s shared haptic state.

## Not Ported

- The conic-light "glass reflection" renderer of `COUIIntentSeekBar.drawConicLight` and its `attr/couiIntentSeekBarConicLightColor` (`#fff87796`). `IntentSeekBarDefaults.ConicLightColor` records the recovered token but nothing consumes it.
- The loading / indeterminate second state (`PlaybackState`), the drag-scale track renderer (`DEFAULT_BACKGROUND_ENLARGE_SCALE` 6.0f, `DEFAULT_PROGRESS_ENLARGE_SCALE` 4.0f, `MIN_ENLARGE_HEIGHT_DP` 2.0f) and the blur / material compositing path. The track here is drawn at its resting height from `IntentSeekBarSize`; the thumb uses `thumbMaxRadius` whenever the bar is pressed or hovered, which is the static approximation of `COUIIntentSeekBar.applyThumbSize`.
- `coui_seekbar_intent_thumb_out_shade_radius` (2dp) and `DEFAULT_INVALIDATE_PIXEL_THRESHOLD` (1.0f), both tied to the invalidation-driven renderers above.
- `showThumb = false` (the native `couiIntentSeekBarShowThumb` default) means the thumb is never painted here, whereas the native paints it while the user interacts (`shouldDrawThumbNow`). Pass `showThumb = true` for the interactive look.
