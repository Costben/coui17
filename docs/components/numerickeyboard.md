# NumericKeyboard

`NumericKeyboard` is the ColorOS 17 numeric PIN pad. A 4x3 grid of 68dp cells inside a 248dp x 326dp view holds `{ 1, 2, 3, 4, 5, 6, 7, 8, 9, -, 0, - }`: three rows of digits and a bottom row whose outer cells are caller-supplied side styles.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.NumericKeyboard
import io.github.suqi8.coui.kmp.basic.NumericKeyboardColors
import io.github.suqi8.coui.kmp.basic.NumericKeyboardDefaults
import io.github.suqi8.coui.kmp.basic.NumericKeyboardSideStyle
import io.github.suqi8.coui.kmp.basic.NumericKeyboardSideType
```

## Basic Usage

```kotlin
var pin by remember { mutableStateOf("") }

NumericKeyboard(
    onNumberClick = { digit -> pin += digit.toString() },
    leftStyle = NumericKeyboardDefaults.deleteSideStyle(COUIIcons.Delete),
    rightStyle = NumericKeyboardDefaults.finishSideStyle(),
    onLeftClick = { pin = pin.dropLast(1) },
    onRightClick = { /* submit */ },
)
```

The pad has no background of its own. `couiNumberBackgroundColor` and `couiSideBackgroundColor` are never set by `Widget.COUI.COUINumericKeyboard`, so both stay transparent and the pad is meant to sit on a wallpaper. The digits are white, so place it on a dark surface.

## Side Cells

The bottom row is `leftStyle`, the digit `0`, and `rightStyle` (`KEYCODE_0_ROW = 3`, `KEYCODE_0_COLUMN = 1`). A side cell draws either a label or an icon, never both — `NumericKeyboardSideStyle` rejects a style that carries both:

```kotlin
NumericKeyboard(
    onNumberClick = { },
    leftStyle = NumericKeyboardSideStyle(
        type = NumericKeyboardSideType.Delete,
        icon = COUIIcons.Delete,
        description = "Delete",
    ),
    rightStyle = NumericKeyboardSideStyle(
        type = NumericKeyboardSideType.Finish,
        text = "Done",
    ),
)
```

An icon is drawn at its own intrinsic size and tinted with `NumericKeyboardColors.numberColor`, which is how the native tints `ic_coui_number_keyboard_launhcer_delete` (a 25dp x 14dp backspace glyph) with `couiNumberColor`. The style animates in and out on the `ensureRightStyleAnimator` spring (response 0.2 / bounce 0) when it is added or removed.

`NumericKeyboardDefaults.finishSideStyle()` is the constructor's `mFinishStyle` (`string/coui_numeric_keyboard_sure`, `"Done"` / `"完成"`), and `NumericKeyboardDefaults.deleteSideStyle(icon)` is the `mDeleteStyle` (`string/coui_number_keyboard_delete`, `"Delete"` / `"删除"`).

## Callbacks

`onNumberClick` fires with the digit of the cell that was released. The two side cells are not digits, so they call `onLeftClick` and `onRightClick` instead, matching `OnClickItemListener`:

| Cell index | Callback                                |
| ---------- | --------------------------------------- |
| 0..8       | onNumberClick(index + 1), i.e. 1..9     |
| 9          | onLeftClick                             |
| 10         | onNumberClick(0)                        |
| 11         | onRightClick                            |

## Properties

| Property Name | Type                      | Description                                              | Default Value                    | Required |
| ------------- | ------------------------- | -------------------------------------------------------- | -------------------------------- | -------- |
| onNumberClick | (Int) -> Unit             | Called with the digit of the released cell, 0..9         | -                                | Yes      |
| modifier      | Modifier                  | Modifier applied to the pad                              | Modifier                         | No       |
| leftStyle     | NumericKeyboardSideStyle? | Content of the bottom-left cell, or null for an empty cell | null                           | No       |
| rightStyle    | NumericKeyboardSideStyle? | Content of the bottom-right cell, or null for an empty cell | null                          | No       |
| colors        | NumericKeyboardColors     | The digit, press and side colours                        | NumericKeyboardDefaults.colors() | No       |
| enabled       | Boolean                   | Whether the pad reacts to pointers                       | true                             | No       |
| onLeftClick   | () -> Unit                | Called when the left side cell is released               | {}                               | No       |
| onRightClick  | () -> Unit                | Called when the right side cell is released              | {}                               | No       |

### NumericKeyboardSideStyle

| Property    | Type                    | Description                                   | Default Value | Required |
| ----------- | ----------------------- | --------------------------------------------- | ------------- | -------- |
| type        | NumericKeyboardSideType | What the cell does                            | None          | No       |
| text        | String?                 | Label to draw, or null                        | null          | No       |
| icon        | ImageVector?            | Icon to draw, or null                         | null          | No       |
| description | String?                 | Accessibility description                     | null          | No       |

`text` and `icon` are mutually exclusive; passing both throws.

### NumericKeyboardSideType

| Value  | COUI Source     |
| ------ | --------------- |
| None   | SIDE_TYPE_NONE, 0 |
| Delete | SIDE_TYPE_DELETE, 1 |
| Finish | SIDE_TYPE_FINISH, 2 |

`getDeleteCellIndex` and `getFinishCellIndex` read this to answer which cell carries the delete and finish actions, so a caller that wants the native split only has to label the two styles.

### NumericKeyboardDefaults

| Constant                  | Type  | Value    | COUI Source                                          |
| ------------------------- | ----- | -------- | ---------------------------------------------------- |
| RowCount                  | Int   | 4        | COUINumericKeyboard.CELL_ROW_COUNT                   |
| ColumnCount               | Int   | 3        | COUINumericKeyboard.CELL_COLUMN_COUNT                |
| CellCount                 | Int   | 12       | 4 * 3                                                |
| Width                     | Dp    | 248.dp   | coui_numeric_keyboard_view_width                     |
| Height                    | Dp    | 326.dp   | coui_numeric_keyboard_view_height                    |
| CellSize                  | Dp    | 68.dp    | coui_numeric_keyboard_view_size                      |
| HorizontalPadding         | Dp    | 0.dp     | coui_numeric_keyboard_view_horizontal_padding        |
| VerticalPadding           | Dp    | 0.dp     | coui_numeric_keyboard_view_vertical_padding          |
| NumberOffsetY             | Dp    | 1.dp     | coui_numeric_keyboard_number_offset_y                |
| AdditionalPressableArea   | Dp    | 32.dp    | coui_additional_pressable_area                       |
| NumberTextSize            | TextUnit | 32.sp | number_keyboard_number_size                          |
| FinishTextSize            | TextUnit | 16.sp | coui_number_keyboard_finish_text_size                |
| FinishText                | String | "Done"  | string/coui_numeric_keyboard_sure                    |
| DeleteText                | String | "Delete" | string/coui_number_keyboard_delete                   |
| PressPeakScale            | Float | 2.15f    | initShowAnimator SHOW_END_SCALE                      |
| PressEndScale             | Float | 2.5f     | initFadeAnimator FADE_END_SCALE                      |
| PressShowDurationMillis   | Int   | 100      | initShowAnimator SHOW_ANIMATOR_TIME                  |
| PressFadeDurationMillis   | Int   | 160      | initFadeAnimator FADE_ANIMATOR_TIME                  |
| Numbers                   | List&lt;Int&gt; | `{ 1..9, -1, 0, -1 }` | mKeyboardNumbers                    |
| Letters                   | List&lt;String&gt; | `{ "", "", "ABC", ... }` | array/coui_number_keyboard_letters |
| CircleMaxAlpha            | Float | 0.12f    | couiCircleMaxAlpha written literally                 |
| DarkCircleMaxAlpha        | Float | 0.06f    | coui_numeric_keyboard_dark_circle_max_alpha          |
| DarkDarkCircleMaxAlpha    | Float | 0.12f    | coui_numeric_keyboard_dark_circle_max_dark_alpha     |
| SideAlphaSpring           | SpringSpec&lt;Float&gt; | response 0.2 / bounce 0 | COUISpringForce of ensureRightStyleAnimator |

The gaps are whatever is left over, exactly as `onSizeChanged` divides it: `(width - padding - 3 * cell) / 2` = 22dp horizontally and `(height - padding - 4 * cell) / 3` = 18dp vertically, so a centre sits at `padding + cell / 2 + i * (cell + gap)`.

`number_keyboard_number_size` (32dp) and `coui_number_keyboard_finish_text_size` (16dp) are dimensions the native reads as pixels, which ignores the font scale; the port reads them as sp.

#### Methods

| Method Name                  | Type                      | Description                                                       |
| ---------------------------- | ------------------------- | ----------------------------------------------------------------- |
| colors()                     | NumericKeyboardColors     | `Widget.COUI.COUINumericKeyboard`, the pad's only reachable style |
| darkColors()                 | NumericKeyboardColors     | `Widget.COUI.COUINumericKeyboard.Dark`, picked from the theme      |
| finishSideStyle()            | NumericKeyboardSideStyle  | The default finish style of `mFinishStyle`                        |
| deleteSideStyle(ImageVector) | NumericKeyboardSideStyle  | The default delete style of `mDeleteStyle`                        |

### Colours

| COUI token                                     | Value                 | Used for                        | Port                              |
| ---------------------------------------------- | --------------------- | ------------------------------- | --------------------------------- |
| coui_numeric_keyboard_number_color             | #ffffffff             | Digit                           | numberColor                       |
| coui_numeric_keyboard_number_press_color       | #ffffffff             | Press circle tint               | pressColor                        |
| coui_numeric_keyboard_word_text_normal_color   | #ffffffff             | Side label                      | sideTextColor                     |
| coui_numeric_keyboard_line_color               | #00ffffff             | Set on `mLinePaint`, never drawn | lineColor, exposed but unused     |
| coui_numeric_keyboard_border_color             | #0dffffff             | 0.6dp border of the light effect | borderColor, exposed but unused   |
| coui_numeric_keyboard_border_highlight_color   | #ccffffff             | Highlight half of that border   | borderHighlightColor, unused      |
| couiSideBackgroundColor                        | unset, transparent    | Side circle                     | sideBackgroundColor               |
| coui_numeric_keyboard_dark_number_color        | #ff000000 / #ffffffff | Dark style digit                | darkColors()                      |
| coui_numeric_keyboard_dark_word_text_normal_color | #ff2ad181          | Dark style side label           | darkColors()                      |
| coui_numeric_keyboard_dark_circle_max_alpha    | 0.06 / 0.12           | Dark style press peak           | darkColors()                      |
| coui_numeric_keyboard_letter_color             | #b3ffffff             | Set but never drawn             | LetterColor, exposed but unused   |

`NumericKeyboardDefaults.colors()` never switches on the theme: `Widget.COUI.COUINumericKeyboard` is the style the attribute resolves to by default, and it is the white-on-dark palette. `darkColors()` carries the `Widget.COUI.COUINumericKeyboard.Dark` palette for callers whose theme points `couiNumericKeyboardStyle` there; its `_dark_` colour family is day/night qualified, so it branches on the theme surface's luminance.

## Behavior

- A cell is hit when the pointer is inside the middle of it plus `coui_additional_pressable_area` (32dp), clamped to half the gap, matching `checkForNewHit`.
- The press feedback is `drawPressCircle`: a circle the width of a cell, tinted with `couiNumPressColor`, that pops from scale 1 to `PressPeakScale` over 100ms while fading in to `couiCircleMaxAlpha`, then grows to `PressEndScale` and fades out over 160ms (`initShowAnimator`, `initFadeAnimator`).
- A release only dispatches when the pointer is still over the cell it started on, and `getTouchIndex` refuses the two empty side cells.
- The digit is placed with the native's own baseline arithmetic, `cy - (descent + ascent) / 2 - mNumberOffsetY` of `drawCell`, which is the same as centring the line box and then removing the offset.

## Not Ported

- `couiPressEffect = LIGHT_EFFECT`: the `LightEffectHelper` spot light, the two `RadialGradient`s of `initRadialGradient`, the `InnerShadowHelper` bitmap of `initInnerShadowBitmap`, the `coui_numeric_keyboard_outer_gradient_*` / `_inner_gradient_*` / `_upper_inner_shadow_color` / `_lower_inner_shadow_color` palette they paint with, the 124dp `coui_numeric_light_shader_radius`, and the 0.6dp `coui_keyboard_button_border_width` stroke of `drawInnerBorder`. All of it sits behind `UIUtil.confirmLevelAnim(ANIM_LEVEL_SUPPORT_BLUR_MIN)`, and commonMain has neither the blur nor the light-effect shader. Only the `RIPPLE_EFFECT` style is ported.
- `KeyboardDrawDelegate.getCustomKeyboardPaint`, the hook the app uses to replace the digit with its own paints.
- The variable font of `getTypeface`: `string/ttf_path` is `/system/fonts/SysSans-En-Regular.ttf` and the weight axis is `integer/font_variation_default_plus` (200, -50 from v35), which commonMain cannot load. The digits use the theme font at 32sp.
- `array/coui_number_keyboard_letters` (`abc`, `def`, ...): the constructor reads it into `Cell.cellLettersStr`, which no draw path of the class consumes. `NumericKeyboardDefaults.Letters` records the values.
- `PatternExploreByTouchHelper` and the `coui_number_keyboard_delete` / `coui_numeric_keyboard_sure` descriptions it reads.
- The blurred companion of the press circle, `coui_number_keyboard_blur_circle` and the `blurAlpha` / `blurScale` half of `initFadeAnimator`: a canvas in commonMain cannot blur a draw call.
- The multi-pointer bookkeeping (`pointerId`, `isMultiPointerEvent`, `findCellByPointerId`) and the key-event path (`onKeyDown` / `onKeyUp` / `handleKeyEvent`).
- The haptics of `setTouchFeedback` and the `playSoundEffect` of `setTouchSoundFeedBack`.
- `getEnterAnim`'s per-cell `cellNumberAlpha` / `cellNumberTranslateY` stagger.
- `setEnabled`'s fade-out: the native `setEnabled` only clears the press paint (`mDownState`) and `needFadeWhenDisabled` is defined but never called, so a disabled pad here simply stops responding.
