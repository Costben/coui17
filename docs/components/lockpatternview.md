# LockPatternView

`LockPatternView` is the ColorOS 17 pattern unlock grid. Nine dots sit in a 3x3 grid inside a 312dp x 280dp view; dragging across them connects the dots it passes, inserts the dot it jumps over, and reports the pattern as `row * 3 + column` indices on release.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.LockPatternColors
import io.github.suqi8.coui.kmp.basic.LockPatternDisplayMode
import io.github.suqi8.coui.kmp.basic.LockPatternView
import io.github.suqi8.coui.kmp.basic.LockPatternViewDefaults
```

## Basic Usage

```kotlin
var pattern by remember { mutableStateOf(emptyList<Int>()) }

LockPatternView(
    onPatternComplete = { drawn -> pattern = drawn },
    mode = LockPatternDisplayMode.Correct,
    pattern = pattern,
)
```

The grid keeps drawing the pattern it last received until the next gesture starts, which is how the native keeps `mPattern` until `clearPattern()`.

## Showing a Verdict

`mode` decides what the drawn pattern means. `Wrong` and `FingerprintNoMatch` switch the path and the discs to the error colour and run the 1000ms flash of `startWrongAnimator` / `startFingerprintNoMatchAnimator`; `Correct`, `Animate` and `FingerprintMatch` use the success colour.

```kotlin
LockPatternView(
    onPatternComplete = { drawn ->
        pattern = drawn
        mode = if (drawn == savedPattern) {
            LockPatternDisplayMode.Correct
        } else {
            LockPatternDisplayMode.Wrong
        }
    },
    mode = mode,
    pattern = pattern,
)
```

## Replaying a Pattern

`Animate` ignores the pointer and replays `pattern` one cell per `LockPatternViewDefaults.AnimateStepMillis` (700ms, `MILLIS_PER_CIRCLE_ANIMATING`), looping forever:

```kotlin
LockPatternView(
    onPatternComplete = { },
    mode = LockPatternDisplayMode.Animate,
    pattern = listOf(0, 3, 6, 7, 8),
)
```

## Stealth Mode

`stealthMode` suppresses the path and the activation animations, leaving only the dots — the native's `mInStealthMode`:

```kotlin
LockPatternView(
    onPatternComplete = { },
    stealthMode = true,
)
```

## Properties

| Property Name      | Type                 | Description                                                                        | Default Value                 | Required |
| ------------------ | -------------------- | ---------------------------------------------------------------------------------- | ----------------------------- | -------- |
| onPatternComplete  | (List&lt;Int&gt;) -> Unit | Called with the pattern the user drew when the pointer is lifted over a cell    | -                             | Yes      |
| modifier           | Modifier             | Modifier applied to the grid                                                       | Modifier                      | No       |
| mode               | LockPatternDisplayMode | What the drawn pattern means right now                                          | LockPatternDisplayMode.Correct | No       |
| pattern            | List&lt;Int&gt;       | The pattern `Animate` replays                                                       | emptyList()                   | No       |
| enabled            | Boolean              | Whether the grid reacts to pointers                                                 | true                          | No       |
| stealthMode        | Boolean              | Whether the path and the activation animations are suppressed                       | false                         | No       |
| colors             | LockPatternColors    | The dot, path, error and success colours plus the ripple peak                       | LockPatternViewDefaults.colors() | No    |
| onPatternStart     | () -> Unit           | Called when the first cell of a gesture is grabbed                                  | {}                            | No       |
| onPatternCellAdded | (List&lt;Int&gt;) -> Unit | Called with the pattern so far whenever a cell is added                          | {}                            | No       |
| onPatternCleared   | () -> Unit           | Called when a gesture is cancelled or begins outside every cell                     | {}                            | No       |

### LockPatternDisplayMode

| Value              | COUI Source                 | Meaning                                            |
| ------------------ | --------------------------- | -------------------------------------------------- |
| Correct            | DisplayMode.Correct         | Resting state, success colour                      |
| Animate            | DisplayMode.Animate         | Replays `pattern`, ignores the pointer             |
| Wrong              | DisplayMode.Wrong           | Error colour plus the 1000ms flash                 |
| FingerprintMatch   | DisplayMode.FingerprintMatch | Success colour, no flash                          |
| FingerprintNoMatch | DisplayMode.FingerprintNoMatch | Error colour, flash on all nine cells            |

### LockPatternViewDefaults

| Constant                    | Type  | Value  | COUI Source                                      |
| --------------------------- | ----- | ------ | ------------------------------------------------ |
| CellCount                   | Int   | 9      | COUILockPatternUtils.ROWS x COLUMNS              |
| Width                       | Dp    | 312.dp | coui_lock_pattern_view_width                     |
| Height                      | Dp    | 280.dp | coui_lock_pattern_view_height                    |
| HorizontalPadding           | Dp    | 21.dp  | coui_lock_pattern_view_horizontal_padding        |
| VerticalPadding             | Dp    | 5.dp   | coui_lock_pattern_view_vertical_padding          |
| DotSize                     | Dp    | 10.dp  | lock_pattern_dot_size                            |
| PathWidth                   | Dp    | 2.dp   | lock_pattern_dot_line_width                      |
| HitFactor                   | Float | 0.6f   | mHitFactor                                       |
| InnerCircleRadius           | Dp    | 6.67.dp | coui_lock_pattern_inner_circle.png, 30dp asset  |
| InnerCircleGlowRadius       | Dp    | 14.67.dp | coui_lock_pattern_inner_circle.png, soft tail  |
| InnerCircleSolidFraction    | Float | 0.4545f | inner disc radius / asset radius                 |
| InnerCircleEdgeFraction     | Float | 0.4773f | inner disc edge / asset radius                   |
| InnerCircleGlowAlpha        | Float | 0.19f  | glow falloff of the inner asset                  |
| OuterCircleRadius           | Dp    | 3.5.dp | coui_lock_pattern_outer_circle.png, 30dp asset   |
| OuterCircleScale            | Float | 7f     | ripple scale 1 -> 7                              |
| InnerCircleDurationMillis   | Int   | 230    | startInnerAnimation duration                     |
| OuterCircleDurationMillis   | Int   | 460    | startOuterAnimation duration                     |
| LineEndDurationMillis       | Int   | 100    | startLineEndAnimation, fast_out_slow_in          |
| WrongFlashDurationMillis    | Int   | 1000   | startWrongAnimator duration                      |
| AnimateStepMillis           | Int   | 700    | MILLIS_PER_CIRCLE_ANIMATING                      |
| EnterAlphaDelayMillis       | Long  | 166L   | ALPHA_DELAY                                      |
| EnterAlphaDurationMillis    | Int   | 167    | ALPHA_DURATION                                   |
| EnterStaggerMillis          | Long  | 16L    | ALPHA_OFFSET                                     |
| EnterTranslateDurationMillis | Int  | 500    | TRANSLATE_Y_DURATION                             |
| MaxTranslateY               | Dp    | 258.dp | color_lock_pattern_view_max_translate_y          |
| LightOuterCircleMaxAlpha    | Float | 0.1f   | coui_lock_pattern_outer_circle_max_alpha         |
| DarkOuterCircleMaxAlpha     | Float | 0.2f   | coui_lock_pattern_outer_circle_max_dark_alpha    |

The cell size is not a constant of its own: it is `(312 - 21 * 2) / 3` = `(280 - 5 * 2) / 3` = 90dp, the `mSquareWidth` / `mSquareHeight` of `onSizeChanged`.

#### Methods

| Method Name         | Type              | Description                                                                 |
| ------------------- | ----------------- | --------------------------------------------------------------------------- |
| colors()            | LockPatternColors | The light or Dark style, picked from the theme background's luminance       |
| launcherColors()    | LockPatternColors | The fixed `coui_lock_pattern_launcher_*` palette for use over a wallpaper   |
| flashAlpha(Float)   | Float             | The `pathAlpha` keyframes of the wrong-pattern flash                        |
| rippleAlpha(Float)  | Float             | The `outerCircleAlpha` keyframes 0 -> peak at 0.5 -> 0                      |

### Colours

| COUI token                                  | Light / Dark          | Used for                       | Port                                  |
| ------------------------------------------- | --------------------- | ------------------------------ | ------------------------------------- |
| color/coui_lock_pattern_dot_color           | #4d000000 / #66ffffff | Unactivated dot                | file constant, no theme slot          |
| color/coui_lock_pattern_path_color          | #ff000000 / #ffffffff | Connected path                 | file constant, no theme slot          |
| color/coui_lock_pattern_error_color         | #ffea3447 both        | Wrong pattern and its flash    | file constant, no theme slot          |
| color/coui_lock_pattern_success_color       | #ff000000 / #ffffffff | Correct pattern                | file constant, no theme slot          |
| color/coui_lock_pattern_launcher_dot_color  | #66ffffff             | Launcher dot                   | launcherColors()                      |
| color/coui_lock_pattern_launcher_path_color | #ffffffff             | Launcher path and success      | launcherColors()                      |
| color/coui_lock_pattern_launcher_error_color | #ffea3447            | Launcher error                 | launcherColors()                      |
| color/coui_lock_pattern_launcher_gradient_color | #4dffffff         | Not read by any draw path      | exposed but unused                    |

`COUITheme.colorScheme` has no slot for any of these. The nearest is `COUITheme.colorScheme.error` (`#ffdb382c` light / `#ffff6c61` dark) against the native `#ffea3447`, which does not match, so the recovered values stay file constants behind `LockPatternViewDefaults.colors()`.

## Behavior

- A cell is grabbed when the pointer is inside the middle `mHitFactor` (0.6) of it, matching `getColumnHit` / `getRowHit`.
- `detectAndAddHit` inserts the skipped cell when the drag jumps over the middle of a row, a column or a diagonal, and adds exactly one cell per event so a fast drag cannot pass through a dot unnoticed.
- The native is deliberately inconsistent about alpha and so is the port. `mPathPaint` gets `setColor(...)` and then `setAlpha(mPathAlpha * 255)`, which *overwrites* the colour's own alpha, so the path is fully opaque even while the 30% dot colour is the current colour; the two cell discs are tinted with that colour instead and keep its alpha. `getCurrentColor(true)` returns the regular colour while `mPatternInProgress`, which is why a finished pattern tightens from the 30% dot colour to the opaque success colour.
- Drawing order follows `onDraw`: the connected path first, then the two PNG discs of each activated cell, then the plain dot of every cell whose `CellState.needDrawCircle` is true.

### Geometry

`mSquareWidth` is `(width - paddingLeft - paddingRight) / 3`, which the defaults make 90dp; a dot is `DotSize` (10dp) across and the path is a 2dp round-joined, round-capped stroke.

## Not Ported

- `PatternExploreByTouchHelper` (`ExploreByTouchHelper`) with its virtual views 1..9, the `announceForAccessibility` calls of `notifyPatternStarted` / `notifyPatternCellAdded` / `notifyPatternCleared` / `notifyPatternDetected`, and the `lockscreen_access_pattern_*` strings.
- `SavedState` and the `(de)serialisation` of `COUILockPatternUtils`: the pattern goes to the caller instead of into the view's own saved state.
- The haptics of `performHitFeedback` / `performWrongModeFeedback` (`COUIHapticFeedbackConstants.GRANULAR_SHORT_VIBRATE` / `SHORT_TRIPLE_VIBRATE`); commonMain has no vibration API.
- `mIsLinearMotorVersion` (`VibrateUtils.isLinearMotorVersion`) and `mIsSetPassword`, which only gate those haptics and are never drawn.
- `getEnterAnim`: the stagger of `initCellAnim` runs once on first composition instead of being handed back as an `AnimatorSet`.
- `dispatchHoverEvent` / `onHoverEvent`, the touch-exploration remapping.
- `clearPattern(boolean)`, `getFailAnimator` and `getSuccessAnimator`, which the report shows are deprecated no-ops.
