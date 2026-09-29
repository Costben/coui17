# IntentSeekBar

在轨道与已播放进度之间多出一层**次级进度**的滑块,对应 ColorOS 的 `COUIIntentSeekBar`(`com/coui/appcompat/seekbar/COUIIntentSeekBar`)。它是 ColorOS 用于媒体播放的形态:一条半透明覆盖式进度条,叠在视频之上仍可辨识,已缓冲区间压在已播放区间之下。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.IntentSeekBar
import io.github.suqi8.coui.kmp.basic.IntentSeekBarColors
import io.github.suqi8.coui.kmp.basic.IntentSeekBarDefaults
import io.github.suqi8.coui.kmp.basic.IntentSeekBarSize
```

## 基本用法

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

手势模型与 [Slider](slider) 相同——按在轨道任意位置即可跳转、拖动擦洗、两端触发边缘触感、支持键盘与无障碍 `setProgress`——因为 `COUIIntentSeekBar` 与普通进度条共用 `COUISeekBar` 的核心。让它成为一个独立组件的是那层次级进度以及它自己的配色模型:它的颜色刻意**不是** [SliderColors](slider),并且保留了一套与主题无关、用于叠在视频上绘制的固定配色。

`secondaryProgress` 会被约束进 `valueRange`,并**同时**被夹到 `value` 以下,因此缓冲条不会越过已播放条——原生把两者绘制在同一个轨道胶囊裁剪区内,次级在前、已播放在后。

## 尺寸

`IntentSeekBarSize`(`com/coui/appcompat/seekbar/IntentSeekBarSize`,由 `attr/couiIntentSeekBarSize` 选择:0 = `Default`,1 = `Small`,2 = `Large`)由轨道高度、按压时背景高度、静止滑块半径与按压滑块半径四个值组成:

```kotlin
IntentSeekBar(
    value = value,
    onValueChange = { value = it },
    secondaryProgress = 0.75f,
    size = IntentSeekBarSize.Small,
)
```

## 叠在视频上绘制

`IntentSeekBarDefaults.fixedColors()` 返回 `couiIntentSeekBarFollowSystemTheme = false` 的配色,即固定的半透明白色覆盖色,用于叠在视频或壁纸之上、主题色无法辨识的场合:

```kotlin
IntentSeekBar(
    value = value,
    onValueChange = { value = it },
    secondaryProgress = 0.8f,
    colors = IntentSeekBarDefaults.fixedColors(),
    showThumb = true,
)
```

在原生组件里,只要调用方设置了 `couiSeekBarBackgroundColor`、`couiSeekBarProgressColor` 或 `couiSeekBarSecondaryProgressColor` 中的任意一个,模式就会自动切换;本移植改为让你直接传入不同的 `IntentSeekBarColors`。`IntentSeekBarDefaults.fallbackColors()` 是第三套配色,即 `COUIIntentSeekBar.getThemeFallbackColors`,仅在主题未定义 `couiColorLabelPrimary` / `couiColorLabelQuaternary` 时使用。

## 属性

| 属性                  | 类型                                    | 说明                                          | 默认值                              | 必需 |
| --------------------- | --------------------------------------- | --------------------------------------------- | ----------------------------------- | ---- |
| value                 | Float                                   | 当前已播放值,会被约束进 `valueRange`          | -                                   | 是   |
| onValueChange         | (Float) -> Unit                         | 用户擦洗已播放值时的回调                      | -                                   | 是   |
| modifier              | Modifier                                | 应用于该滑块的修饰符                          | Modifier                            | 否   |
| enabled               | Boolean                                 | 是否响应输入                                  | true                                | 否   |
| valueRange            | ClosedFloatingPointRange&lt;Float&gt;   | 滑块可取的数值范围                            | 0f..1f                              | 否   |
| secondaryProgress     | Float                                   | 绘制在 `value` 之下的缓冲值                    | valueRange.start                    | 否   |
| steps                 | Int                                     | `valueRange` 两端之间的离散值个数;0 表示连续  | 0                                   | 否   |
| onValueChangeFinished | (() -> Unit)?                           | 拖动结束时的回调                              | null                                | 否   |
| showSecondaryProgress | Boolean                                 | 是否绘制次级进度层                            | true                                | 否   |
| showThumb             | Boolean                                 | 是否绘制滑块                                  | false                               | 否   |
| size                  | IntentSeekBarSize                       | 轨道 / 滑块尺寸组                             | IntentSeekBarSize.Default           | 否   |
| colors                | IntentSeekBarColors                     | 颜色                                          | IntentSeekBarDefaults.colors()      | 否   |

### IntentSeekBarSize

| 常量    | trackHeight                  | maxBackgroundHeight          | thumbRadius                      | thumbMaxRadius                      |
| ------- | ---------------------------- | ---------------------------- | -------------------------------- | ----------------------------------- |
| Default | 4.dp(`_height_default`)      | 6.dp(`_max_..._default`)     | 4.dp(`_thumb_radius_default`)    | 5.dp(`_thumb_max_radius_default`)   |
| Small   | 2.dp(`_height_small`)        | 10.dp(`_max_..._small`)      | 3.dp(`_thumb_radius_small`)      | 8.dp(`_thumb_max_radius_small`)     |
| Large   | 6.dp(`_height_large`)        | 10.dp(`_max_..._large`)      | 5.dp(`_thumb_radius_large`)      | 8.dp(`_thumb_max_radius_large`)     |

这八个值分别来自 `dimen/coui_seekbar_intent_height_*`、`coui_seekbar_intent_max_background_height_*`、`coui_seekbar_intent_thumb_radius_*` 与 `coui_seekbar_intent_thumb_max_radius_*`。滑块的整体布局高度即原生 `onMeasure` 的结果:`max(maxBackgroundHeight, thumbMaxRadius * 2, MIN_TOUCH_TARGET_HEIGHT_DP)`,其中 `MIN_TOUCH_TARGET_HEIGHT_DP = 24.0f`,因此滑块始终不低于 24dp。

### IntentSeekBarDefaults

| 常量                        | 类型  | 取值        | COUI 来源                                        |
| --------------------------- | ----- | ----------- | ------------------------------------------------ |
| ConicLightColor             | Int   | 0xFFF87796  | attr/couiIntentSeekBarConicLightColor 默认值      |
| FixedBackgroundColor        | Color | #4dffffff   | color/coui_seekbar_intent_background_color_fixed  |
| FixedProgressColor          | Color | #8affffff   | color/coui_seekbar_intent_progress_color_fixed    |
| FixedSecondaryProgressColor | Color | #29ffffff   | color/coui_seekbar_intent_secondary_progress_color_fixed |
| FixedThumbColor             | Color | #ffffffff   | color/coui_seekbar_intent_thumb_color_fixed       |

#### 方法

| 方法名           | 类型                | 说明                                                                             |
| ---------------- | ------------------- | -------------------------------------------------------------------------------- |
| colors()         | IntentSeekBarColors | `couiIntentSeekBarFollowSystemTheme = true` 的配色,从主题解析                    |
| fixedColors()    | IntentSeekBarColors | `followSystemTheme = false` 的固定半透明覆盖配色                                 |
| fallbackColors() | IntentSeekBarColors | `getThemeFallbackColors`,即主题缺少对应属性时使用的普通进度条配色                |

### 颜色

| COUI token                                                        | 取值                  | 用途                | 移植目标                     |
| ----------------------------------------------------------------- | --------------------- | ------------------- | ---------------------------- |
| attr/couiColorLabelQuaternary → color/coui_color_label_quaternary | #14000000 / #26ffffff | 轨道背景            | secondaryVariant             |
| attr/couiColorLabelPrimary → color/coui_color_label_primary       | #e6000000 / #e6ffffff | 已播放进度          | onSurface                    |
| attr/couiColorLabelQuaternary → color/coui_color_label_quaternary | #14000000 / #26ffffff | 次级进度            | secondaryVariant             |
| color/coui_seekbar_intent_thumb_color                             | #ff000000 / #ffffffff | 滑块                | 按主题背景亮度推导           |
| color/coui_seekbar_intent_background_color_fixed                  | #4dffffff             | 固定模式的轨道背景  | 文件内常量                   |
| color/coui_seekbar_intent_progress_color_fixed                    | #8affffff             | 固定模式的已播放进度 | 文件内常量                  |
| color/coui_seekbar_intent_secondary_progress_color_fixed          | #29ffffff             | 固定模式的次级进度  | 文件内常量                   |
| color/coui_seekbar_intent_thumb_color_fixed                       | #ffffffff             | 固定模式的滑块      | 文件内常量                   |
| color/coui_seekbar_background_color_normal                        | #0d000000             | 回退背景            | 文件内常量                   |
| color/coui_seekbar_progress_color_normal                          | #ff4d4d4d / #ff999999 | 回退进度            | 文件内常量                   |
| color/coui_seekbar_secondary_progress_color                       | #4dffffff             | 回退次级进度        | 文件内常量                   |

次级进度与轨道背景共用 `?attr/couiColorLabelQuaternary`,即 0.08 透明度的黑色——原生把次级层绘制在同一个轨道胶囊内,因此两层 alpha 在视觉上叠加。四个 `fixed*` 颜色就是 `style/COUIIntentSeekBar` 与 `style/COUIIntentSeekBar.Dark` 为 `couiSeekBarSecondaryProgressColor` / `couiSeekBarThumbColor` 指定的 `@color/coui_seekbar_secondary_progress_color` 与 `@color/coui_seekbar_progress_selector`;两个样式同时把 `couiSeekBarBackgroundHeight` 与 `couiSeekBarProgressHeight` 设为 4dp,与 `IntentSeekBarSize.Default` 一致。

## 行为

- 图层顺序即 `drawBackground` + `drawActiveTrack`:先整宽轨道胶囊,再次级进度,再已播放进度,两条进度都裁剪在轨道胶囊之内。背景是整宽的,因为 `COUIIntentSeekBar.getBackgroundHorizontalInset` 返回 `0.0f`;进度胶囊内缩当前端帽高度的一半,使圆头始终落在条内。
- 拖动时已播放进度以快弹簧跟随手指(`dampingRatio = 0.9f`,`stiffness = 1755f`),松手后以较慢的弹簧收敛(`dampingRatio = 0.96f`,`stiffness = 322f`),对应 `COUISeekBarAdvanced.useDragTransitionAnimation()`,它对 intent 条返回 true。
- 次级进度使用临界阻尼的 response 0.5 弹簧(`SECONDARY_PROGRESS_SPRING_RESPONSE` 0.5 / `SECONDARY_PROGRESS_SPRING_BOUNCE` 0.0,`COUIIntentSeekBar.initSecondaryProgressAnim`;`stiffness = (2 * PI / 0.5)² = 158`)。
- `steps > 0` 时数值吸附到 `steps + 2` 个关键点中最接近的一个,并在两端触发边缘触感,二者都复用 [Slider](slider) 的共享触感状态。

## 未移植

- `COUIIntentSeekBar.drawConicLight` 的锥形高光「玻璃反光」渲染器及其 `attr/couiIntentSeekBarConicLightColor`(`#fff87796`)。`IntentSeekBarDefaults.ConicLightColor` 记录了该 token,但没有任何代码消费它。
- 加载 / 不定态(`PlaybackState`)、拖动时轨道放大的渲染路径(`DEFAULT_BACKGROUND_ENLARGE_SCALE` 6.0f、`DEFAULT_PROGRESS_ENLARGE_SCALE` 4.0f、`MIN_ENLARGE_HEIGHT_DP` 2.0f)以及模糊 / 材质合成路径。本移植的轨道按 `IntentSeekBarSize` 的静止高度绘制;滑块在按压或悬停时使用 `thumbMaxRadius`,这是 `COUIIntentSeekBar.applyThumbSize` 的静态近似。
- `coui_seekbar_intent_thumb_out_shade_radius`(2dp)与 `DEFAULT_INVALIDATE_PIXEL_THRESHOLD`(1.0f),两者都依附于上述基于失效重绘的渲染器。
- `showThumb = false`(原生 `couiIntentSeekBarShowThumb` 的默认值)在本移植中意味着滑块永不绘制,而原生会在用户交互期间绘制它(`shouldDrawThumbNow`)。需要交互形态时传 `showThumb = true`。
