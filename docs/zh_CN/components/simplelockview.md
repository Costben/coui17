# SimpleLockView

`SimpleLockView` 是 ColorOS 17 的锁屏 PIN 指示器，对应 `COUISimpleLock`（`com.coui.appcompat.lockview`，Settings APK）。一排圆角方块随着输入逐格点亮；密码被拒绝时整排抖动，已点亮的方块下坠消失。点亮数量由调用方决定。

原生从 `R.styleable.COUISimpleLock_couiCircleNum`（0 -> 四格，1 -> 六格）、`couiRectanglePadding`（10dp，格间距）以及两张 `<shape>` 图 `couiFilledRectangleIconDrawable` / `couiOutLinedRectangleIconDrawable` 读取几何量——两者都是 10dp x 10dp、圆角 2dp 的方块。因此一排宽度为 `dotCount * 10dp + (dotCount - 1) * 10dp`，四格 70dp，六格 110dp。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.SimpleLockType
import io.github.suqi8.coui.kmp.basic.SimpleLockView
import io.github.suqi8.coui.kmp.basic.SimpleLockViewColors
import io.github.suqi8.coui.kmp.basic.SimpleLockViewDefaults
```

## 基本用法

```kotlin
var count by remember { mutableStateOf(0) }

SimpleLockView(
    count = count,
    type = SimpleLockType.Six,
)
```

`count` 即 `mCodeNumber + 1`：0 表示空排，`SimpleLockType.dotCount` 表示满排。超出范围会被截断。

## 四格与六格

`type` 决定一排的格数，即原生的 `couiCircleNum` / `mRectangleType`：

```kotlin
SimpleLockView(count = 4, type = SimpleLockType.Four)
SimpleLockView(count = 4, type = SimpleLockType.Six)
```

`count` 变大时新点亮的方块在 `SimpleLockViewDefaults.MorphDurationMillis` 内淡入，变小时同样时长淡出。两个方向使用不同的缓动曲线，详见[行为](#行为)。

## 上报被拒绝的密码

`failed` 是原生 `setFailed` 的一次性触发，而不是持续状态。它变为 true 时播放 800ms 的抖动与下坠，结束后整排变空——这正是原生在动画结尾把 `mCodeNumber` 置为 -1 的结果。请用计数器等方式驱动，使连续失败可以重复触发：

```kotlin
var attempts by remember { mutableStateOf(0) }
var failed by remember { mutableStateOf(false) }

SimpleLockView(
    count = count,
    failed = failed,
)
```

## 属性

| 属性名    | 类型                 | 描述                                       | 默认值                            | 必填 |
| --------- | -------------------- | ------------------------------------------ | --------------------------------- | ---- |
| count     | Int                  | 已点亮的格数，`mCodeNumber + 1`             | -                                 | 是   |
| modifier  | Modifier             | 应用在整排上的修饰符                       | Modifier                          | 否   |
| type      | SimpleLockType       | 一排包含的格数                             | SimpleLockType.Six                | 否   |
| failed    | Boolean              | 变为 true 时播放抖动与下坠                  | false                             | 否   |
| colors    | SimpleLockViewColors | 填充色与描边色                             | SimpleLockViewDefaults.colors()   | 否   |

### SimpleLockType

| 取值 | COUI 来源                     | dotCount | 一排宽度 |
| ---- | ----------------------------- | -------- | -------- |
| Four | DEFAULTTYPE，`couiCircleNum` 0 | 4        | 70dp     |
| Six  | SIXCIRCLETYPE，`couiCircleNum` 1 | 6      | 110dp    |

### SimpleLockViewDefaults

| 常量                       | 类型       | 值                            | COUI 来源                                        |
| -------------------------- | ---------- | ----------------------------- | ------------------------------------------------ |
| DotSize                    | Dp         | 10.dp                         | `coui_simple_lock_filled_rectangle_icon` 的尺寸   |
| CornerRadius               | Dp         | 2.dp                          | 该图的 `<corners android:radius>`                 |
| RectanglePadding           | Dp         | 10.dp                         | `couiRectanglePadding`，`mRectanglePadding`       |
| MorphDurationMillis        | Int        | 230                           | MORPHING_OUTLINED_TO_FILLED_TIME / _FILLED_TO_OUTLINED_TIME |
| ShakeAndFallDurationMillis | Int        | 800                           | SHAKE_AND_FALL_TIME                              |
| FallSpeed                  | Float      | 250f                          | FALL_SPEED，`ValueAnimator.ofFloat(0f, 250f)`     |
| FallHeight                 | Float      | 150f                          | FALL_HEIGHT，alpha 除数与预留高度                 |
| ShakeXPoints               | FloatArray | 0, 30, -28, 14, -8, 4, -3, 0  | SHAKE_X_POINT                                    |
| DelayFour                  | FloatArray | 0, 38.5, 91, 63               | DELAY_FOUR                                       |
| DelaySix                   | FloatArray | 0, 38.5, 91, 63, 38.5, 70     | DELAY_SIX                                        |
| FilledColorLight           | Color      | #ff000000                     | `coui_simple_lock_filled_rectangle_icon_color`   |
| FilledColorDark            | Color      | #e6ffffff                     | 同上，夜态限定                                    |
| OutlinedColorLight         | Color      | #19000000                     | `coui_simple_lock_outlined_rectangle_icon_color` |
| OutlinedColorDark          | Color      | #66ffffff                     | 同上，夜态限定                                    |
| FilledColorDarkStyle       | Color      | #e6ffffff                     | `..._filled_rectangle_icon_dark_color`           |
| OutlinedColorDarkStyle     | Color      | #66ffffff                     | `..._outlined_rectangle_icon_dark_color`         |
| TransparentFilledColor     | Color      | #ffffffff                     | `..._transparent_filled_rectangle_icon_color`，未使用 |
| TransparentOutlinedColor   | Color      | #26ffffff                     | `..._transparent_outlined_rectangle_icon_color`，未使用 |
| CircleMaxScale             | Float      | 1.2f                          | CIRCLE_MAX_SCALE，仅透明样式                     |
| SpringResponse             | Float      | 0.35f                         | SPRING_RESPONSE，仅透明样式                      |
| SpringBounce               | Float      | 0.5f                          | SPRING_BOUNCE，仅透明样式                        |
| AddSpec                    | AnimationSpec&lt;Float&gt; | tween 230ms | `createMorphingAnimationOutLinedToFilled` |
| RemoveSpec                 | AnimationSpec&lt;Float&gt; | tween 230ms | `createMorphingAnimationFilledToOutLined` |

`FallSpeed`、`FallHeight`、`ShakeXPoints` 与两个延迟数组都是原生的原始像素，不是 dp；组件会按当前密度换算。

#### 方法

| 方法名            | 类型                 | 描述                                                       |
| ----------------- | -------------------- | ---------------------------------------------------------- |
| fallDelays(type)  | FloatArray           | 与整排匹配的 `DELAY_FOUR` 或 `DELAY_SIX`                    |
| shakeX(progress)  | Float                | `SHAKE_X_POINT` 曲线，在 0..1 上分段线性求值                |
| colors()          | SimpleLockViewColors | 依据主题背景亮度选择浅色或夜态配色                          |
| darkColors()      | SimpleLockViewColors | 固定的 `Widget.COUI.COUISimpleLock.Dark` 配色               |

### 颜色

| COUI token                                           | 浅色 / 深色           | 用途         | 本移植                       |
| ---------------------------------------------------- | --------------------- | ------------ | ---------------------------- |
| color/coui_simple_lock_filled_rectangle_icon_color   | #ff000000 / #e6ffffff | 已点亮方块   | 文件常量，主题无对应槽位     |
| color/coui_simple_lock_outlined_rectangle_icon_color | #19000000 / #66ffffff | 空方块       | 文件常量，主题无对应槽位     |
| color/coui_simple_lock_..._dark_color                | #e6ffffff / #66ffffff | `.Dark` 样式 | darkColors()                 |
| color/coui_simple_lock_transparent_*_icon_color      | #ffffffff / #26ffffff | 透明样式     | 已声明但未使用               |

`COUITheme.colorScheme` 没有对应这些颜色的槽位。这些资源是按日 / 夜限定而非主题属性驱动的，因此 `colors()` 依据主题背景的亮度分支，`darkColors()` 返回固定的 `.Dark` 配色，用于壁纸之上。透明样式的配色保留为文件常量，因为该样式未移植。

## 行为

- **点亮。** `createMorphingAnimationOutLinedToFilled` 是 `ValueAnimator.ofInt(0, 255)`，未设置插值器，即框架自带的 accelerate-decelerate 曲线；本移植精确复现（`AddSpec`）。
- **熄灭。** `createMorphingAnimationFilledToOutLined` 同样 230ms，但跑在 `COUIEaseInterpolator` 上，即 `cubic-bezier(0.33, 0, 0.67, 1)`（`RemoveSpec`）。两个方向刻意不对称。
- **失败。** `createFailedAnimator` 是一个 800ms 的线性 `ValueAnimator`：整排的水平偏移沿 `SHAKE_X_POINT` 变化，每个实心方块下坠 `max(0, translationY - DELAY[i])`，当这段距离达到 `FALL_HEIGHT`（150 px）后淡出。`translationY` 本身在同一 800ms 内从 0 走到 250。描边方块只抖动，不下坠。
- **失败之后。** 原生设置 `fail_lastDraw`、把 `mCodeNumber` 置为 -1 并绘制空状态，因此即便调用方的 count 没变，整排读起来也是空的。必须再次改变 `count` 才会重新填充。
- 先绘制描边方块，再把每个已点亮方块叠在上面，与 `onDraw` 一致。

### 几何

方块边长 `DotSize`（10dp）、圆角 `CornerRadius`（2dp）、间距 `RectanglePadding`（10dp）。整排在可用宽度内居中，即原生的 `mCodeImageStart = (width - mRectanglesWidth) / 2`。高度为下坠预留空间：`DotSize + FALL_HEIGHT`，对应原生的 `mDrawableHeight + 150 px`。

## 未移植 / 差异

- 「透明样式」（`isTransparentStyle()`，即 `mGlowEffectDrawable != null` 且 SDK >= 34）：它把 alpha 渐变换成逐格 `COUISpringAnimation`（`SPRING_RESPONSE` 0.35、`SPRING_BOUNCE` 0.5、`CIRCLE_MAX_SCALE` 1.2），并加入 `drawGlowEffect` 的 `coui_simple_lock_glow_effect_drawable` 光晕，含其 `coui_glow_ball_offset`（25dp）边界与 `coui_simple_lock_transparent_*` 配色。光晕是一张模糊 PNG，commonMain 的 canvas 无法合成；本移植只绘制经典样式。
- `SimpleLockTouchHelper`（`ExploreByTouchHelper`）及其填充的 `string/coui_simple_lock_access_description` 模板（"Password input area. You have entered x digits. You need to enter y digits in total."）。
- `performFeedback` / `mIsLinearMotorVersion`（`VibrateUtils.isLinearMotorVersion`、`COUIHapticFeedbackConstants.SHORT_TRIPLE_VIBRATE`、原始触感 300）以及 `setFingerprintRecognition`，它们只用于抑制上述反馈。
- `getNumberStrList` 与 `mDecription` 记账字段，仅为上述无障碍播报存在。
- `getAddSpringAnimator` 与 `getAddAnimator` / `getDeleteAnimator` / `getFailedAnimator`：这些是应用驱动视图所用的 `Animator` 句柄。这里改由 `count` 与 `failed` 的变化驱动同样的曲线。
- `refresh()` 与 `setOutlinedRectangleDrawable` / `setFilledRectangleDrawable`，它们替换位图；本移植直接绘制还原出的形状。
- `setOpacity(int)`、`setRectanglePadding(int)`、`setRectangleType(int)`、`setSimpleLockType(int)` 与 `setScaleX` / `setScaleY`，它们修改的状态现在由 `type` 与默认值固定。
