# MarqueeText

A text that scrolls horizontally when it overflows the space it is given, mirroring ColorOS's `com.coui.appcompat.tips.COUIMarqueeTextView`. Text that fits is drawn normally and never scrolls; a text wider than its box walks left at 25dp per second, starts 10dp inside the left edge, leaves an 80dp gap after each pass, waits 1000ms before starting and fades both clipped edges over 26dp.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.MarqueeText
import io.github.suqi8.coui.kmp.basic.MarqueeTextDefaults
```

## Basic Usage

```kotlin
MarqueeText(
    text = "This sentence is far too long for the box it is given",
    modifier = Modifier.width(160.dp),
)
```

## Scrolling on demand

The native keeps `isMarqueeEnable` false until the app enables it; `marqueeEnabled` mirrors that switch. Turning it off stops the scroll and returns the text to its start:

```kotlin
MarqueeText(
    text = longText,
    marqueeEnabled = keepScrolling,
    modifier = Modifier.width(160.dp),
)
```

## Properties

### MarqueeText

| Property          | Type      | Description                                    | Default Value                        | Required |
| ----------------- | --------- | ---------------------------------------------- | ------------------------------------ | -------- |
| text              | String    | Text to display                                | -                                    | Yes      |
| modifier          | Modifier  | Modifier applied to the text                   | Modifier                             | No       |
| color             | Color     | Text color                                     | Color.Unspecified                    | No       |
| style             | TextStyle | Text style                                     | MarqueeTextDefaults.textStyle()      | No       |
| marqueeEnabled    | Boolean   | Whether the text may scroll                    | true                                 | No       |
| speed             | Dp        | Scroll speed                                   | MarqueeTextDefaults.ScrollSpeed      | No       |
| startLocation     | Dp        | Distance the text starts inside the left edge  | MarqueeTextDefaults.ScrollStartLocation | No    |
| textInterval      | Dp        | Gap between two passes of the text             | MarqueeTextDefaults.TextInterval     | No       |
| fadingEdgeWidth   | Dp        | Width of the fade on each clipped edge         | MarqueeTextDefaults.FadingEdgeWidth  | No       |
| scrollDelayMillis | Long      | Delay before the scroll starts                 | MarqueeTextDefaults.ScrollDelayMillis | No      |

### MarqueeTextDefaults

| Constant           | Type      | Value  | COUI Source                                              |
| ------------------ | --------- | ------ | -------------------------------------------------------- |
| ScrollSpeed        | Dp        | 25.dp  | `coui_top_tips_scroll_speed`                             |
| ScrollStartLocation | Dp       | 10.dp  | `coui_top_tips_scroll_text_start_location`               |
| TextInterval       | Dp        | 80.dp  | `coui_top_tips_scroll_text_interval`                     |
| FadingEdgeWidth    | Dp        | 26.dp  | `coui_top_tips_fading_edge_size`                         |
| ScrollDelayMillis  | Long      | 1000   | `COUIMarqueeTextView.DEFAULT_SCROLL_DELAY_DURATION`      |

#### Methods

| Method Name | Type      | Description                                                     |
| ----------- | --------- | --------------------------------------------------------------- |
| textStyle() | TextStyle | Default text style (`COUITheme.textStyles.main`, as in `Text`)  |

## Behavior

- `COUIMarqueeTextView.onMeasure` only starts the scroll when the measured text is wider than the view; the port measures the text with the same style and falls back to a single static, ellipsized line otherwise.
- `initSpeed` computes the per-frame step as `coui_top_tips_scroll_speed / Display.getRefreshRate()`, i.e. 25dp per second; the port uses that as the scroll duration per loop.
- `setContent` appends `coui_top_tips_scroll_text_interval` (80dp) after the text and repeats the string until it fills the view; the port renders two copies separated by the interval, which is the same seamless loop.
- `initTextViewAttributes` enables horizontal fading edges with `setFadingEdgeLength(coui_top_tips_fading_edge_size)` (26dp); the port draws the same edge fade as a `BlendMode.DstOut` gradient, applied on the right edge while the content continues and on the left edge once the text has moved past its start.
- The scroll starts `DEFAULT_SCROLL_DELAY_DURATION` (1000ms) after the text is laid out, and `mCurrentScrollLocation` begins at `coui_top_tips_scroll_text_start_location` (10dp).

## Not Ported / Deviations

- `stopRoll()` / `continueRoll()` as an imperative pair: `marqueeEnabled` toggles the same state, and setting it back to `false` stops the scroll and returns the text to its start.
- The accessibility suppression of `mSuppressAccessibilityEvents` / `onInitializeAccessibilityEvent` and the `onInitializeAccessibilityNodeInfo` override that reports `mOriginText` instead of the repeated string; the port always reports the original text because it never replaces the laid-out string.
- `checkAllCharactersDirections` and the `isAllCharactersLtR` mirroring of the scroll direction; the port leaves direction handling to Compose's `LayoutDirection`.
- `COUIMarqueeTextView` is an `AppCompatTextView` and declares no text appearance, so `textStyle()` defaults to the theme's `COUITheme.textStyles.main`, the same default as `Text`.
- The `id/marquee` view id belongs to the app layout, not to the component.
