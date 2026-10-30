# NumericKeyboard

`NumericKeyboard` 是 ColorOS 17 的数字 PIN 键盘。248dp x 326dp 视图内是 4x3 个 68dp 单元格，排布为 `{ 1, 2, 3, 4, 5, 6, 7, 8, 9, -, 0, - }`：三行数字，底行两侧由调用方提供 side style。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.NumericKeyboard
import io.github.suqi8.coui.kmp.basic.NumericKeyboardColors
import io.github.suqi8.coui.kmp.basic.NumericKeyboardDefaults
import io.github.suqi8.coui.kmp.basic.NumericKeyboardSideStyle
import io.github.suqi8.coui.kmp.basic.NumericKeyboardSideType
```

## 基本用法

```kotlin
var pin by remember { mutableStateOf("") }

NumericKeyboard(
    onNumberClick = { digit -> pin += digit.toString() },
    leftStyle = NumericKeyboardDefaults.deleteSideStyle(COUIIcons.Delete),
    rightStyle = NumericKeyboardDefaults.finishSideStyle(),
    onLeftClick = { pin = pin.dropLast(1) },
    onRightClick = { /* 提交 */ },
)
```

键盘自身没有背景。`Widget.COUI.COUINumericKeyboard` 从不设置 `couiNumberBackgroundColor` 与 `couiSideBackgroundColor`，两者保持透明，键盘本身就是为贴在壁纸之上设计的。数字为白色，请把它放在深色底面上。

## 两侧格子

底行依次是 `leftStyle`、数字 `0`、`rightStyle`（`KEYCODE_0_ROW = 3`、`KEYCODE_0_COLUMN = 1`）。单个 side 格子只绘制文字或图标，二者不会同时出现——`NumericKeyboardSideStyle` 会拒绝同时携带两者的样式：

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

图标按自身固有尺寸绘制，并使用 `NumericKeyboardColors.numberColor` 着色，这正是原生用 `couiNumberColor` 给 `ic_coui_number_keyboard_launhcer_delete`（25dp x 14dp 退格图形）着色的方式。side style 被加入或移除时，会按 `ensureRightStyleAnimator` 的弹簧（response 0.2 / bounce 0）淡入淡出。

`NumericKeyboardDefaults.finishSideStyle()` 是构造函数的 `mFinishStyle`（`string/coui_numeric_keyboard_sure`，`"Done"` / `"完成"`），`NumericKeyboardDefaults.deleteSideStyle(icon)` 是 `mDeleteStyle`（`string/coui_number_keyboard_delete`，`"Delete"` / `"删除"`）。

## 回调

`onNumberClick` 在抬手格子的数字上回调。两侧格子不是数字，因此改为调用 `onLeftClick` 与 `onRightClick`，与 `OnClickItemListener` 一致：

| 格子下标 | 回调                                 |
| -------- | ------------------------------------ |
| 0..8     | onNumberClick(index + 1)，即 1..9    |
| 9        | onLeftClick                          |
| 10       | onNumberClick(0)                     |
| 11       | onRightClick                         |

## 属性

| 属性名        | 类型                      | 描述                                       | 默认值                           | 必填 |
| ------------- | ------------------------- | ------------------------------------------ | -------------------------------- | ---- |
| onNumberClick | (Int) -> Unit             | 抬手格子的数字，0..9                       | -                                | 是   |
| modifier      | Modifier                  | 应用在键盘上的修饰符                       | Modifier                         | 否   |
| leftStyle     | NumericKeyboardSideStyle? | 左下角格子内容，null 表示空格子            | null                             | 否   |
| rightStyle    | NumericKeyboardSideStyle? | 右下角格子内容，null 表示空格子            | null                             | 否   |
| colors        | NumericKeyboardColors     | 数字、按压与两侧颜色                       | NumericKeyboardDefaults.colors() | 否   |
| pressEffect   | NumericKeyboardPressEffect | `Light`（锁屏材质按压）或 `Ripple`（旧的圆形涟漪） | NumericKeyboardPressEffect.Light | 否   |
| enabled       | Boolean                   | 是否响应指针                               | true                             | 否   |
| onLeftClick   | () -> Unit                | 左侧格子抬手时回调                         | {}                               | 否   |
| onRightClick  | () -> Unit                | 右侧格子抬手时回调                         | {}                               | 否   |

### NumericKeyboardSideStyle

| 属性        | 类型                    | 描述                     | 默认值 | 必填 |
| ----------- | ----------------------- | ------------------------ | ------ | ---- |
| type        | NumericKeyboardSideType | 该格子的行为             | None   | 否   |
| text        | String?                 | 要绘制的文字，或 null    | null   | 否   |
| icon        | ImageVector?            | 要绘制的图标，或 null    | null   | 否   |
| description | String?                 | 无障碍描述               | null   | 否   |

`text` 与 `icon` 互斥，同时传入会抛异常。

### NumericKeyboardSideType

| 取值   | COUI 来源           |
| ------ | ------------------- |
| None   | SIDE_TYPE_NONE, 0   |
| Delete | SIDE_TYPE_DELETE, 1 |
| Finish | SIDE_TYPE_FINISH, 2 |

`getDeleteCellIndex` 与 `getFinishCellIndex` 依据它判断哪个格子承载删除与完成动作，因此调用方只要给两个样式标注类型即可获得原生的分工。

### NumericKeyboardDefaults

| 常量                      | 类型  | 值       | COUI 来源                                            |
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
| CircleMaxAlpha            | Float | 0.12f    | couiCircleMaxAlpha 以字面量写入                      |
| DarkCircleMaxAlpha        | Float | 0.06f    | coui_numeric_keyboard_dark_circle_max_alpha          |
| DarkDarkCircleMaxAlpha    | Float | 0.12f    | coui_numeric_keyboard_dark_circle_max_dark_alpha     |
| SideAlphaSpring           | SpringSpec&lt;Float&gt; | response 0.2 / bounce 0 | ensureRightStyleAnimator 的 COUISpringForce |

间距是剩余空间，与 `onSizeChanged` 的划分完全一致：水平为 `(width - padding - 3 * cell) / 2` = 22dp，垂直为 `(height - padding - 4 * cell) / 3` = 18dp，因此中心位于 `padding + cell / 2 + i * (cell + gap)`。

`number_keyboard_number_size`（32dp）与 `coui_number_keyboard_finish_text_size`（16dp）在原生中按像素读取，会忽略字体缩放；本移植按 sp 读取。

#### 方法

| 方法名                       | 类型                      | 描述                                                    |
| ---------------------------- | ------------------------- | ------------------------------------------------------- |
| colors()                     | NumericKeyboardColors     | `Widget.COUI.COUINumericKeyboard`，键盘唯一可到达的样式 |
| darkColors()                 | NumericKeyboardColors     | `Widget.COUI.COUINumericKeyboard.Dark`，依据主题选择    |
| finishSideStyle()            | NumericKeyboardSideStyle  | `mFinishStyle` 的默认完成样式                           |
| deleteSideStyle(ImageVector) | NumericKeyboardSideStyle  | `mDeleteStyle` 的默认删除样式                           |

### 颜色

| COUI token                                     | 值                    | 用途                            | 本移植                        |
| ---------------------------------------------- | --------------------- | ------------------------------- | ----------------------------- |
| coui_numeric_keyboard_number_color             | #ffffffff             | 数字                            | numberColor                   |
| coui_numeric_keyboard_number_press_color       | #ffffffff             | 按压圆着色                      | pressColor                    |
| coui_numeric_keyboard_word_text_normal_color   | #ffffffff             | 两侧文字                        | sideTextColor                 |
| coui_numeric_keyboard_line_color               | #00ffffff             | 设在 `mLinePaint` 上，从未绘制  | lineColor，已声明但未使用     |
| coui_numeric_keyboard_border_color             | #0dffffff             | 光效的 0.6dp 描边               | borderColor，已声明但未使用   |
| coui_numeric_keyboard_border_highlight_color   | #ccffffff             | 该描边的高光部分                | borderHighlightColor，未使用  |
| couiSideBackgroundColor                        | 未设置，透明          | 两侧格子圆形底                  | sideBackgroundColor           |
| coui_numeric_keyboard_dark_number_color        | #ff000000 / #ffffffff | Dark 样式数字                   | darkColors()                  |
| coui_numeric_keyboard_dark_word_text_normal_color | #ff2ad181          | Dark 样式两侧文字               | darkColors()                  |
| coui_numeric_keyboard_dark_circle_max_alpha    | 0.06 / 0.12           | Dark 样式按压峰值               | darkColors()                  |
| coui_numeric_keyboard_letter_color             | #b3ffffff             | 已设置但从未绘制                | LetterColor，已声明但未使用   |

`NumericKeyboardDefaults.colors()` 不随主题切换：`Widget.COUI.COUINumericKeyboard` 是该属性默认解析到的样式，本身就是白字深底调色板。`darkColors()` 提供 `Widget.COUI.COUINumericKeyboard.Dark` 调色板，供主题把 `couiNumericKeyboardStyle` 指到该样式的调用方使用；其 `_dark_` 颜色族带日夜限定，因此依据主题 surface 的亮度分支。

## 行为

- 指针落在某格中间范围内，再外扩 `coui_additional_pressable_area`（32dp）并钳制到间距的一半，即视为命中，对应 `checkForNewHit`。
- 按压反馈由 `drawPressCircle` 完成：一个单元格宽度的圆，用 `couiNumPressColor` 着色，100ms 内从 scale 1 弹到 `PressPeakScale` 同时淡入至 `couiCircleMaxAlpha`，随后 160ms 内增长到 `PressEndScale` 并淡出（`initShowAnimator`、`initFadeAnimator`）。
- 只有当指针仍停留在按下的那个格子之上时才派发事件，且 `getTouchIndex` 会拒绝两个空的侧边格子。
- 数字使用原生自身的基线算法定位，即 `drawCell` 的 `cy - (descent + ascent) / 2 - mNumberOffsetY`，等价于先居中行盒再扣掉偏移。

## 未移植 / 差异

- `couiPressEffect = LIGHT_EFFECT`：`LightEffectHelper` 聚光、`initRadialGradient` 的两个 `RadialGradient`、`initInnerShadowBitmap` 的位图、它们所使用的 `coui_numeric_keyboard_outer_gradient_*` / `_inner_gradient_*` / `_upper_inner_shadow_color` / `_lower_inner_shadow_color` 调色板、124dp 的 `coui_numeric_light_shader_radius`，以及 `drawInnerBorder` 的 0.6dp `coui_keyboard_button_border_width` 描边。这些全部位于 `UIUtil.confirmLevelAnim(ANIM_LEVEL_SUPPORT_BLUR_MIN)` 之后，而 commonMain 既没有模糊也没有光效着色器。本移植只实现 `RIPPLE_EFFECT` 样式。
- `KeyboardDrawDelegate.getCustomKeyboardPaint`，应用用它替换数字绘制画笔的钩子。
- `getTypeface` 的可变字体：`string/ttf_path` 为 `/system/fonts/SysSans-En-Regular.ttf`，字重轴为 `integer/font_variation_default_plus`（200，v35 起为 -50），commonMain 无法加载。数字使用主题字体，字号 32sp。
- `array/coui_number_keyboard_letters`（`abc`、`def` 等）：构造函数把它读进 `Cell.cellLettersStr`，而本类没有任何绘制路径使用它；`NumericKeyboardDefaults.Letters` 记录了这些取值。
- `PatternExploreByTouchHelper`，以及它读取的 `coui_number_keyboard_delete` / `coui_numeric_keyboard_sure` 描述。
- 按压圆的模糊副本 `coui_number_keyboard_blur_circle`，以及 `initFadeAnimator` 中 `blurAlpha` / `blurScale` 那一半：commonMain 的画布无法对绘制调用做模糊。
- 多指记账（`pointerId`、`isMultiPointerEvent`、`findCellByPointerId`）与按键路径（`onKeyDown` / `onKeyUp` / `handleKeyEvent`）。
- `setTouchFeedback` 的触感反馈与 `setTouchSoundFeedBack` 的 `playSoundEffect`。
- `getEnterAnim` 的逐格 `cellNumberAlpha` / `cellNumberTranslateY` 错位动画。
- `setEnabled` 的淡出：原生 `setEnabled` 只清除按压绘制状态（`mDownState`），而 `needFadeWhenDisabled` 定义了却从未调用，因此本移植中禁用后只是停止响应。
