# LockPatternView

`LockPatternView` 是 ColorOS 17 的图案解锁九宫格，对应 `COUILockPatternView`（`com.coui.appcompat.lockview`，Settings APK）。九个圆点排布在 312dp x 280dp 视图内的 3x3 网格中；拖动时连接经过的圆点、补上被跨过的圆点，松手后以 `row * 3 + column` 下标的形式回调图案。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.LockPatternColors
import io.github.suqi8.coui.kmp.basic.LockPatternDisplayMode
import io.github.suqi8.coui.kmp.basic.LockPatternView
import io.github.suqi8.coui.kmp.basic.LockPatternViewDefaults
```

## 基本用法

```kotlin
var pattern by remember { mutableStateOf(emptyList<Int>()) }

LockPatternView(
    onPatternComplete = { drawn -> pattern = drawn },
    mode = LockPatternDisplayMode.Correct,
    pattern = pattern,
)
```

网格会持续绘制最后一次收到的图案，直到下一次手势开始，这与原生在 `clearPattern()` 前一直保留 `mPattern` 的行为一致。

## 展示校验结果

`mode` 决定已绘制图案的含义。`Wrong` 与 `FingerprintNoMatch` 会把路径和圆盘切换为错误色，并播放 `startWrongAnimator` / `startFingerprintNoMatchAnimator` 的 1000ms 闪烁；`Correct`、`Animate`、`FingerprintMatch` 使用成功色。

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

## 回放图案

`Animate` 会忽略指针输入，按每格 `LockPatternViewDefaults.AnimateStepMillis`（700ms，`MILLIS_PER_CIRCLE_ANIMATING`）循环回放 `pattern`：

```kotlin
LockPatternView(
    onPatternComplete = { },
    mode = LockPatternDisplayMode.Animate,
    pattern = listOf(0, 3, 6, 7, 8),
)
```

## 隐身模式

`stealthMode` 会抑制路径与激活动画，只保留圆点，对应原生的 `mInStealthMode`：

```kotlin
LockPatternView(
    onPatternComplete = { },
    stealthMode = true,
)
```

## 属性

| 属性名             | 类型                 | 描述                                                     | 默认值                         | 必填 |
| ------------------ | -------------------- | -------------------------------------------------------- | ------------------------------ | ---- |
| onPatternComplete  | (List&lt;Int&gt;) -> Unit | 指针在某格上抬起时回调用户绘制的图案                 | -                              | 是   |
| modifier           | Modifier             | 应用在网格上的修饰符                                     | Modifier                       | 否   |
| mode               | LockPatternDisplayMode | 当前已绘制图案的含义                                   | LockPatternDisplayMode.Correct | 否   |
| pattern            | List&lt;Int&gt;       | `Animate` 回放的图案                                      | emptyList()                    | 否   |
| enabled            | Boolean              | 是否响应指针                                              | true                           | 否   |
| stealthMode        | Boolean              | 是否抑制路径与激活动画                                    | false                          | 否   |
| colors             | LockPatternColors    | 圆点、路径、错误与成功颜色，以及涟漪峰值                  | LockPatternViewDefaults.colors() | 否  |
| onPatternStart     | () -> Unit           | 手势抓取第一格时回调                                      | {}                             | 否   |
| onPatternCellAdded | (List&lt;Int&gt;) -> Unit | 每新增一格时回调当前图案                             | {}                             | 否   |
| onPatternCleared   | () -> Unit           | 手势被取消或在所有格之外开始时回调                        | {}                             | 否   |

### LockPatternDisplayMode

| 取值               | COUI 来源                     | 含义                                 |
| ------------------ | ----------------------------- | ------------------------------------ |
| Correct            | DisplayMode.Correct           | 静止态，成功色                       |
| Animate            | DisplayMode.Animate           | 回放 `pattern`，忽略指针             |
| Wrong              | DisplayMode.Wrong             | 错误色加 1000ms 闪烁                 |
| FingerprintMatch   | DisplayMode.FingerprintMatch  | 成功色，无闪烁                       |
| FingerprintNoMatch | DisplayMode.FingerprintNoMatch | 错误色，九格全部闪烁                |

### LockPatternViewDefaults

| 常量                        | 类型  | 值     | COUI 来源                                        |
| --------------------------- | ----- | ------ | ------------------------------------------------ |
| CellCount                   | Int   | 9      | COUILockPatternUtils.ROWS x COLUMNS              |
| Width                       | Dp    | 312.dp | coui_lock_pattern_view_width                     |
| Height                      | Dp    | 280.dp | coui_lock_pattern_view_height                    |
| HorizontalPadding           | Dp    | 21.dp  | coui_lock_pattern_view_horizontal_padding        |
| VerticalPadding             | Dp    | 5.dp   | coui_lock_pattern_view_vertical_padding          |
| DotSize                     | Dp    | 10.dp  | lock_pattern_dot_size                            |
| PathWidth                   | Dp    | 2.dp   | lock_pattern_dot_line_width                      |
| HitFactor                   | Float | 0.6f   | mHitFactor                                       |
| InnerCircleRadius           | Dp    | 6.67.dp | coui_lock_pattern_inner_circle.png，30dp 素材   |
| InnerCircleGlowRadius       | Dp    | 14.67.dp | coui_lock_pattern_inner_circle.png，软边尾部   |
| InnerCircleSolidFraction    | Float | 0.4545f | 内圆盘半径 / 素材半径                           |
| InnerCircleEdgeFraction     | Float | 0.4773f | 内圆盘边缘 / 素材半径                           |
| InnerCircleGlowAlpha        | Float | 0.19f  | 内圆素材的辉光衰减                               |
| OuterCircleRadius           | Dp    | 3.5.dp | coui_lock_pattern_outer_circle.png，30dp 素材   |
| OuterCircleScale            | Float | 7f     | 涟漪缩放 1 -> 7                                  |
| InnerCircleDurationMillis   | Int   | 230    | startInnerAnimation 时长                        |
| OuterCircleDurationMillis   | Int   | 460    | startOuterAnimation 时长                        |
| LineEndDurationMillis       | Int   | 100    | startLineEndAnimation，fast_out_slow_in          |
| WrongFlashDurationMillis    | Int   | 1000   | startWrongAnimator 时长                          |
| AnimateStepMillis           | Int   | 700    | MILLIS_PER_CIRCLE_ANIMATING                      |
| EnterAlphaDelayMillis       | Long  | 166L   | ALPHA_DELAY                                      |
| EnterAlphaDurationMillis    | Int   | 167    | ALPHA_DURATION                                   |
| EnterStaggerMillis          | Long  | 16L    | ALPHA_OFFSET                                     |
| EnterTranslateDurationMillis | Int  | 500    | TRANSLATE_Y_DURATION                             |
| MaxTranslateY               | Dp    | 258.dp | color_lock_pattern_view_max_translate_y          |
| LightOuterCircleMaxAlpha    | Float | 0.1f   | coui_lock_pattern_outer_circle_max_alpha         |
| DarkOuterCircleMaxAlpha     | Float | 0.2f   | coui_lock_pattern_outer_circle_max_dark_alpha    |

单元格尺寸不是独立常量，而是 `(312 - 21 * 2) / 3` = `(280 - 5 * 2) / 3` = 90dp，即 `onSizeChanged` 中的 `mSquareWidth` / `mSquareHeight`。

#### 方法

| 方法名              | 类型              | 描述                                                      |
| ------------------- | ----------------- | --------------------------------------------------------- |
| colors()            | LockPatternColors | 浅色或 Dark 样式，依据主题背景亮度选择                     |
| launcherColors()    | LockPatternColors | 固定的 `coui_lock_pattern_launcher_*` 调色板，用于壁纸之上 |
| flashAlpha(Float)   | Float             | 错误图案闪烁的 `pathAlpha` 关键帧                          |
| rippleAlpha(Float)  | Float             | `outerCircleAlpha` 关键帧：0 -> 0.5 处峰值 -> 0            |

### 颜色

| COUI token                                  | 浅色 / 深色           | 用途                  | 本移植                       |
| ------------------------------------------- | --------------------- | --------------------- | ---------------------------- |
| color/coui_lock_pattern_dot_color           | #4d000000 / #66ffffff | 未激活圆点            | 文件常量，主题无对应槽位     |
| color/coui_lock_pattern_path_color          | #ff000000 / #ffffffff | 已连接路径            | 文件常量，主题无对应槽位     |
| color/coui_lock_pattern_error_color         | #ffea3447 两主题相同  | 错误图案及其闪烁      | 文件常量，主题无对应槽位     |
| color/coui_lock_pattern_success_color       | #ff000000 / #ffffffff | 正确图案              | 文件常量，主题无对应槽位     |
| color/coui_lock_pattern_launcher_dot_color  | #66ffffff             | Launcher 圆点         | launcherColors()             |
| color/coui_lock_pattern_launcher_path_color | #ffffffff             | Launcher 路径与成功   | launcherColors()             |
| color/coui_lock_pattern_launcher_error_color | #ffea3447            | Launcher 错误         | launcherColors()             |
| color/coui_lock_pattern_launcher_gradient_color | #4dffffff         | 无任何绘制路径读取    | 已声明但未使用               |

`COUITheme.colorScheme` 没有对应这些颜色的槽位。最接近的是 `COUITheme.colorScheme.error`（浅色 `#ffdb382c` / 深色 `#ffff6c61`），与原生 `#ffea3447` 不符，因此还原出的取值保留为 `LockPatternViewDefaults.colors()` 背后的文件常量。

## 行为

- 指针落在某一格中间 `mHitFactor`（0.6）范围内即视为抓取该格，对应 `getColumnHit` / `getRowHit`。
- `detectAndAddHit` 在拖动跨过某行、某列或对角线的中点时先插入被跳过的格子，并且每个事件至多新增一格，因此快速拖动不会穿过圆点而不被记录。
- 原生在 alpha 上刻意不一致，本移植同样如此：`mPathPaint` 先 `setColor(...)` 再 `setAlpha(mPathAlpha * 255)`，后者会**覆盖**颜色自带的 alpha，所以即使当前颜色是 30% 的圆点色，路径也是完全不透明的；而两个圆盘是用该颜色着色，因此会保留其 alpha。`getCurrentColor(true)` 在 `mPatternInProgress` 时返回常规颜色，这正是图案画完后从 30% 圆点色收紧为不透明成功色的原因。
- 绘制顺序遵循 `onDraw`：先绘制连接路径，再绘制每个已激活格子的两张 PNG 圆盘，最后为 `CellState.needDrawCircle` 为 true 的格子绘制普通圆点。

### 几何

`mSquareWidth` 为 `(width - paddingLeft - paddingRight) / 3`，在默认值下等于 90dp；圆点直径为 `DotSize`（10dp），路径为 2dp 的圆角连接、圆头端帽描边。

## 未移植 / 差异

- `PatternExploreByTouchHelper`（`ExploreByTouchHelper`）及其 1..9 虚拟视图，`notifyPatternStarted` / `notifyPatternCellAdded` / `notifyPatternCleared` / `notifyPatternDetected` 中的 `announceForAccessibility` 调用，以及 `lockscreen_access_pattern_*` 字符串。
- `SavedState` 与 `COUILockPatternUtils` 的图案序列化 / 反序列化：图案交给调用方，而不是写进视图自身的保存状态。
- `performHitFeedback` / `performWrongModeFeedback` 的触感反馈（`COUIHapticFeedbackConstants.GRANULAR_SHORT_VIBRATE` / `SHORT_TRIPLE_VIBRATE`）；commonMain 没有振动 API。
- `mIsLinearMotorVersion`（`VibrateUtils.isLinearMotorVersion`）与 `mIsSetPassword`，它们只用于控制上述触感反馈，从不参与绘制。
- `getEnterAnim`：`initCellAnim` 的错位动画改为首次组合时执行一次，而不是作为 `AnimatorSet` 返回。
- `dispatchHoverEvent` / `onHoverEvent`，触摸探索的重映射。
- `clearPattern(boolean)`、`getFailAnimator` 与 `getSuccessAnimator`，报告中显示它们已是废弃的空实现。
