# SimpleLockView

`SimpleLockView` is the ColorOS 17 lock screen PIN indicator, the port of `COUISimpleLock` (`com.coui.appcompat.lockview`, Settings APK). A row of rounded squares fills in one by one as the user types; when the PIN is rejected the whole row shakes and the filled squares drop away. The count is the caller's: pass how many squares should light up.

The native reads its geometry from `R.styleable.COUISimpleLock_couiCircleNum` (0 -> four squares, 1 -> six), `couiRectanglePadding` (10dp, the gap), and the two `<shape>` drawables `couiFilledRectangleIconDrawable` / `couiOutLinedRectangleIconDrawable` — both a 10dp x 10dp square with a 2dp corner radius. The row is therefore `dotCount * 10dp + (dotCount - 1) * 10dp` wide, 70dp for four and 110dp for six.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.SimpleLockType
import io.github.suqi8.coui.kmp.basic.SimpleLockView
import io.github.suqi8.coui.kmp.basic.SimpleLockViewColors
import io.github.suqi8.coui.kmp.basic.SimpleLockViewDefaults
```

## Basic Usage

```kotlin
var count by remember { mutableStateOf(0) }

SimpleLockView(
    count = count,
    type = SimpleLockType.Six,
)
```

`count` is `mCodeNumber + 1`: 0 is the empty row and `SimpleLockType.dotCount` is the full row. Values outside that range are clamped.

## Four or Six Squares

`type` picks the row length, the native's `couiCircleNum` / `mRectangleType`:

```kotlin
SimpleLockView(count = 4, type = SimpleLockType.Four)
SimpleLockView(count = 4, type = SimpleLockType.Six)
```

Growing `count` fades the newly filled squares in over `SimpleLockViewDefaults.MorphDurationMillis`; shrinking it fades them back out over the same duration. The two directions run on different curves — see [Behavior](#behavior).

## Reporting a Rejected PIN

`failed` is a one-shot trigger of the native's `setFailed`, not a persistent state. When it turns true the row plays the 800ms shake and fall and is left empty afterwards, exactly as the native ends with `mCodeNumber = -1`. Drive it from a counter so a repeated failure retriggers:

```kotlin
var attempts by remember { mutableStateOf(0) }
var failed by remember { mutableStateOf(false) }

SimpleLockView(
    count = count,
    failed = failed,
)
```

## Properties

| Property Name | Type                | Description                                                                | Default Value                    | Required |
| ------------- | ------------------- | -------------------------------------------------------------------------- | -------------------------------- | -------- |
| count         | Int                 | How many squares are filled, `mCodeNumber + 1`                              | -                                | Yes      |
| modifier      | Modifier            | Modifier applied to the row                                                 | Modifier                         | No       |
| type          | SimpleLockType      | How many squares the row holds                                              | SimpleLockType.Six               | No       |
| failed        | Boolean             | Plays the shake and fall when it turns true                                 | false                            | No       |
| colors        | SimpleLockViewColors | The filled and outlined square colours                                     | SimpleLockViewDefaults.colors()  | No       |

### SimpleLockType

| Value | COUI Source          | dotCount | Row Width |
| ----- | -------------------- | -------- | --------- |
| Four  | DEFAULTTYPE, `couiCircleNum` 0 | 4 | 70dp |
| Six   | SIXCIRCLETYPE, `couiCircleNum` 1 | 6 | 110dp |

### SimpleLockViewDefaults

| Constant                     | Type       | Value                            | COUI Source                                     |
| ---------------------------- | ---------- | -------------------------------- | ----------------------------------------------- |
| DotSize                      | Dp         | 10.dp                            | `coui_simple_lock_filled_rectangle_icon` size   |
| CornerRadius                 | Dp         | 2.dp                             | `<corners android:radius>` of that drawable     |
| RectanglePadding             | Dp         | 10.dp                            | `couiRectanglePadding`, `mRectanglePadding`     |
| MorphDurationMillis          | Int        | 230                              | MORPHING_OUTLINED_TO_FILLED_TIME / _FILLED_TO_OUTLINED_TIME |
| ShakeAndFallDurationMillis   | Int        | 800                              | SHAKE_AND_FALL_TIME                             |
| FallSpeed                    | Float      | 250f                             | FALL_SPEED, `ValueAnimator.ofFloat(0f, 250f)`    |
| FallHeight                   | Float      | 150f                             | FALL_HEIGHT, alpha divisor and reserved height   |
| ShakeXPoints                 | FloatArray | 0, 30, -28, 14, -8, 4, -3, 0     | SHAKE_X_POINT                                   |
| DelayFour                    | FloatArray | 0, 38.5, 91, 63                  | DELAY_FOUR                                      |
| DelaySix                     | FloatArray | 0, 38.5, 91, 63, 38.5, 70        | DELAY_SIX                                       |
| FilledColorLight             | Color      | #ff000000                        | `coui_simple_lock_filled_rectangle_icon_color`  |
| FilledColorDark              | Color      | #e6ffffff                        | the same, night-qualified                       |
| OutlinedColorLight           | Color      | #19000000                        | `coui_simple_lock_outlined_rectangle_icon_color` |
| OutlinedColorDark            | Color      | #66ffffff                        | the same, night-qualified                       |
| FilledColorDarkStyle         | Color      | #e6ffffff                        | `..._filled_rectangle_icon_dark_color`          |
| OutlinedColorDarkStyle       | Color      | #66ffffff                        | `..._outlined_rectangle_icon_dark_color`        |
| TransparentFilledColor       | Color      | #ffffffff                        | `..._transparent_filled_rectangle_icon_color`, unused |
| TransparentOutlinedColor     | Color      | #26ffffff                        | `..._transparent_outlined_rectangle_icon_color`, unused |
| CircleMaxScale               | Float      | 1.2f                             | CIRCLE_MAX_SCALE, transparent style only        |
| SpringResponse               | Float      | 0.35f                            | SPRING_RESPONSE, transparent style only         |
| SpringBounce                 | Float      | 0.5f                             | SPRING_BOUNCE, transparent style only           |
| AddSpec                      | AnimationSpec&lt;Float&gt; | tween 230ms | `createMorphingAnimationOutLinedToFilled` |
| RemoveSpec                   | AnimationSpec&lt;Float&gt; | tween 230ms | `createMorphingAnimationFilledToOutLined` |

`FallSpeed`, `FallHeight`, `ShakeXPoints` and the two delay arrays are raw pixels of the native, not dp; the component converts them with the current density.

#### Methods

| Method Name        | Type                    | Description                                                              |
| ------------------ | ----------------------- | ------------------------------------------------------------------------ |
| fallDelays(type)   | FloatArray              | `DELAY_FOUR` or `DELAY_SIX`, whichever matches the row                   |
| shakeX(progress)   | Float                   | The `SHAKE_X_POINT` curve, evaluated piecewise-linearly over 0..1        |
| colors()           | SimpleLockViewColors    | The light or night palette, picked from the theme background luminance   |
| darkColors()       | SimpleLockViewColors    | The fixed `Widget.COUI.COUISimpleLock.Dark` palette                      |

### Colours

| COUI token                                            | Light / Dark          | Used for             | Port                         |
| ----------------------------------------------------- | --------------------- | -------------------- | ---------------------------- |
| color/coui_simple_lock_filled_rectangle_icon_color    | #ff000000 / #e6ffffff | The filled square    | file constant, no theme slot |
| color/coui_simple_lock_outlined_rectangle_icon_color  | #19000000 / #66ffffff | The empty square     | file constant, no theme slot |
| color/coui_simple_lock_..._dark_color                 | #e6ffffff / #66ffffff | The `.Dark` style    | darkColors()                 |
| color/coui_simple_lock_transparent_*_icon_color       | #ffffffff / #26ffffff | Transparent style    | exposed but unused           |

`COUITheme.colorScheme` has no slot for these. The resources are day/night qualified rather than theme-attribute driven, so `colors()` branches on the theme background's luminance, and `darkColors()` returns the fixed `.Dark` palette for use over a wallpaper. The transparent palette stays a file constant because that style is not ported.

## Behavior

- **Filling.** `createMorphingAnimationOutLinedToFilled` is a `ValueAnimator.ofInt(0, 255)` with no interpolator set, i.e. the framework's accelerate-decelerate curve; the port reproduces it exactly (`AddSpec`).
- **Emptying.** `createMorphingAnimationFilledToOutLined` runs the same 230ms on `COUIEaseInterpolator`, the `cubic-bezier(0.33, 0, 0.67, 1)` (`RemoveSpec`). So the two directions are deliberately not symmetric.
- **Failure.** `createFailedAnimator` is a single 800ms linear `ValueAnimator`: the row's horizontal offset follows `SHAKE_X_POINT`, and every solid square drops by `max(0, translationY - DELAY[i])`, fading out once that distance reaches `FALL_HEIGHT` (150 px). `translationY` itself runs 0 -> 250 over the same 800ms. The outlined squares shake but never fall.
- **After the failure.** The native sets `fail_lastDraw`, clears `mCodeNumber` to -1 and draws the empty state, so the row reads as empty even though the caller's count has not changed. `count` must change again to refill it.
- The outlined squares are drawn first, then each filled square on top, matching `onDraw`.

### Geometry

A dot is `DotSize` (10dp) across with a `CornerRadius` (2dp) corner, and the gap is `RectanglePadding` (10dp). The row is centred in the available width, which is the native's `mCodeImageStart = (width - mRectanglesWidth) / 2`. The height reserves the fall: `DotSize + FALL_HEIGHT`, the native's `mDrawableHeight + 150 px`.

## Not Ported

- The "transparent style" (`isTransparentStyle()`, i.e. `mGlowEffectDrawable != null` and SDK >= 34): it replaces the alpha morph with a per-square `COUISpringAnimation` (`SPRING_RESPONSE` 0.35, `SPRING_BOUNCE` 0.5, `CIRCLE_MAX_SCALE` 1.2) and adds the `coui_simple_lock_glow_effect_drawable` halo of `drawGlowEffect` with its `coui_glow_ball_offset` (25dp) bounds and the `coui_simple_lock_transparent_*` palette. The halo is a blurred PNG, which a commonMain canvas cannot composite; only the classic style is drawn.
- `SimpleLockTouchHelper` (`ExploreByTouchHelper`) and the `string/coui_simple_lock_access_description` template it fills in ("Password input area. You have entered x digits. You need to enter y digits in total.").
- `performFeedback` / `mIsLinearMotorVersion` (`VibrateUtils.isLinearMotorVersion`, `COUIHapticFeedbackConstants.SHORT_TRIPLE_VIBRATE`, raw haptic 300) and `setFingerprintRecognition`, which only suppress that feedback.
- `getNumberStrList` and the `mDecription` bookkeeping, which exist only for the accessibility announcements above.
- `getAddSpringAnimator` and `getAddAnimator` / `getDeleteAnimator` / `getFailedAnimator`: the `Animator` handles the app used to drive the view. Here the transitions of `count` and `failed` drive the same curves.
- `refresh()` and `setOutlinedRectangleDrawable` / `setFilledRectangleDrawable`, which swap bitmaps; the port draws the recovered shape itself.
- `setOpacity(int)`, `setRectanglePadding(int)`, `setRectangleType(int)`, `setSimpleLockType(int)` and `setScaleX` / `setScaleY`, which mutate state that `type` and the defaults now fix.
