# PageIndicator

`PageIndicator` is the ColorOS 17 page indicator: a centred row of dots mirroring `COUIPageIndicator` (`Widget.COUI.COUIPageIndicator`), one dot per page, with the current page drawn in the trace colour and every other dot in the dot colour. The dot geometry, the maximum visible dot count and the inactive / active colours all come from the native view.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.PageIndicator
import io.github.suqi8.coui.kmp.basic.PageIndicatorDefaults
```

## Basic Usage

```kotlin
var page by remember { mutableIntStateOf(0) }

PageIndicator(
    count = 5,
    currentIndex = page,
)
```

Dots are display-only. The guide dialog calls `setIsClickable(false)` on its indicator, and like that dialog, nothing here reacts to taps.

## More Pages Than Fit

Up to `PageIndicatorDefaults.MaxVisibleDots` (6) dots are drawn — the count the native view clamps its visible rect to (`MAX_VISIBLE_DOT_NUMBER` / `PageIndicatorModel.mMaxVisibleCount`). With more pages, the visible dots slide as a window over the page list, keeping the active dot near the middle and clamped so the window never leaves the list:

```kotlin
PageIndicator(count = 8, currentIndex = 6)
```

## Properties

| Property Name | Type    | Description                                                     | Default Value                            | Required |
| ------------- | ------- | --------------------------------------------------------------- | ---------------------------------------- | -------- |
| count         | Int     | Number of pages; `count <= 0` draws nothing                     | -                                        | Yes      |
| currentIndex  | Int     | Index of the active page, clamped into `0 until count`          | -                                        | Yes      |
| modifier      | Modifier | Modifier applied to the indicator                              | Modifier                                 | No       |
| activeColor   | Color   | Colour of the active dot (`traceDotColor`)                      | COUITheme.colorScheme.onSurface          | No       |
| inactiveColor | Color   | Colour of the other dots (`dotColor`)                           | COUITheme.colorScheme.disabledOnSurface  | No       |

### PageIndicatorDefaults

| Constant      | Type | Value  | COUI Source                                           |
| ------------- | ---- | ------ | ----------------------------------------------------- |
| DotSize       | Dp   | 6.dp   | coui_page_indicator_dot_size                          |
| DotSizeMedium | Dp   | 4.5.dp | coui_page_indicator_dot_size_medium                   |
| DotSizeSmall  | Dp   | 2.5.dp | coui_page_indicator_dot_size_small                    |
| DotSpacing    | Dp   | 3.dp   | coui_page_indicator_dot_spacing                       |
| MaxVisibleDots | Int | 6      | COUIPageIndicator.MAX_VISIBLE_DOT_NUMBER              |

## Geometry

The dots keep a constant centre-to-centre pitch of `DotSize + 2 * DotSpacing` = 12dp, matching `mInterval = mDotsInterval * 2.0f` (6dp) and `getVisibleRect`, so a dot of any diameter is centred inside its own 12dp cell. The indicator is `DotSize` (6dp) tall, i.e. exactly one normal dot.

`coui_page_indicator_dot_spacing` (3dp) is the native half-interval `mDotsInterval`; the effective interval between two neighbouring dots is twice that, 6dp. This is why the row gap is 6dp rather than 3dp.

## Colours

| COUI token                                                          | Light / Night         | Used for           | Port               |
| ------------------------------------------------------------------- | --------------------- | ------------------ | ------------------ |
| coui_color_label_primary                                            | #e6000000 / #e6ffffff | Active (trace) dot | onSurface          |
| coui_color_hint_neutral                                             | #42000000 / #4dffffff | Inactive dots      | disabledOnSurface  |
| coui_page_indicator_dot_color (declared, unused by the guide style) | #4d000000 / #4dffffff | -                  | not used           |
| coui_page_indicator_trace_dot_color (declared, unused by the guide style) | #ff666666 / #ffababab | -           | not used           |

`Widget.COUI.COUIPageIndicator` does not resolve its colours from `coui_page_indicator_*`; it takes `dotColor` from `?attr/couiColorHintNeutral` and `traceDotColor` from `@color/coui_color_label_primary`, with a `Widget.COUI.COUIPageIndicator.Dark` variant for dark themes. This port maps those onto `disabledOnSurface` and `onSurface`.

## Not Ported / Deviations

- The per-frame size/offset mask behind the sliding window. The native's exact offset maths is not available; its level ladder constants are known (large/medium/small/gone level sizes = 3/5/7/9, maximum large dots = 4, maximum medium dots = 2), so this port slides a clamped window and, as an approximation, shrinks to `DotSizeMedium` the two dots of the window that have hidden neighbours. `DotSizeSmall` is exposed but never applied.
- The moving-trace colour blend of `drawTrace` (`getColorFactor`, `DEFAULT_COLOR_FACTOR_THRESHOLD` = 0.05) and the 1500-stiffness position spring (`dampingRatio` = 1.0, `minimumVisibleChange` = 0.005). The active dot simply carries `activeColor`.
- The accessibility description built from `indicator_content_description` and `coui_page_indicator_description`.
- The three deprecated no-op setters (`setDotCornerRadius`, `setDotSpacing`, `setDotStrokeWidth`), `snapToPosition` and `stopTraceAnimator`.
