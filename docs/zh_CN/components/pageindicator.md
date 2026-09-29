# PageIndicator

`PageIndicator` 是 ColorOS 17 的圆点指示器，对应 `COUIPageIndicator`（`Widget.COUI.COUIPageIndicator`）：居中一排圆点，每页一个圆点，当前页使用 trace 颜色，其余使用普通圆点颜色。圆点几何、最多可见圆点数以及未选中 / 选中颜色都取自原生视图。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.PageIndicator
import io.github.suqi8.coui.kmp.basic.PageIndicatorDefaults
```

## 基本用法

```kotlin
var page by remember { mutableIntStateOf(0) }

PageIndicator(
    count = 5,
    currentIndex = page,
)
```

圆点仅用于展示。引导对话框会对指示器调用 `setIsClickable(false)`，与之一致，本组件同样不响应点击。

## 页数超出可见数量

最多绘制 `PageIndicatorDefaults.MaxVisibleDots`（6）个圆点——这是原生可见区域的上限（`MAX_VISIBLE_DOT_NUMBER` / `PageIndicatorModel.mMaxVisibleCount`）。页数更多时，可见圆点以窗口形式在页面列表上滑动，尽量让当前圆点保持在中间，并做钳制以免窗口越出列表：

```kotlin
PageIndicator(count = 8, currentIndex = 6)
```

## 属性

| 属性名        | 类型     | 描述                                              | 默认值                                   | 必填 |
| ------------- | -------- | ------------------------------------------------- | ---------------------------------------- | ---- |
| count         | Int      | 页数；`count <= 0` 时不绘制任何内容               | -                                        | 是   |
| currentIndex  | Int      | 当前页下标，会被钳制到 `0 until count`            | -                                        | 是   |
| modifier      | Modifier | 应用在指示器上的修饰符                            | Modifier                                 | 否   |
| activeColor   | Color    | 当前圆点颜色（`traceDotColor`）                   | COUITheme.colorScheme.onSurface          | 否   |
| inactiveColor | Color    | 其余圆点颜色（`dotColor`）                        | COUITheme.colorScheme.disabledOnSurface  | 否   |

### PageIndicatorDefaults

| 常量           | 类型 | 值     | COUI 来源                                  |
| -------------- | ---- | ------ | ------------------------------------------ |
| DotSize        | Dp   | 6.dp   | coui_page_indicator_dot_size               |
| DotSizeMedium  | Dp   | 4.5.dp | coui_page_indicator_dot_size_medium        |
| DotSizeSmall   | Dp   | 2.5.dp | coui_page_indicator_dot_size_small         |
| DotSpacing     | Dp   | 3.dp   | coui_page_indicator_dot_spacing            |
| MaxVisibleDots | Int  | 6      | COUIPageIndicator.MAX_VISIBLE_DOT_NUMBER   |

## 几何

圆点保持恒定的圆心间距 `DotSize + 2 * DotSpacing` = 12dp，与 `mInterval = mDotsInterval * 2.0f`（6dp）和 `getVisibleRect` 一致，因此任意直径的圆点都居中在各自的 12dp 单元内。指示器高度为 `DotSize`（6dp），即正好一个普通圆点。

`coui_page_indicator_dot_spacing`（3dp）是原生的半间距 `mDotsInterval`，相邻两个圆点之间实际间隔是它的两倍，即 6dp。因此行间距是 6dp 而不是 3dp。

## 颜色

| COUI token                                                            | 浅色 / 深色           | 用途             | 本移植            |
| --------------------------------------------------------------------- | --------------------- | ---------------- | ----------------- |
| coui_color_label_primary                                              | #e6000000 / #e6ffffff | 当前（trace）圆点 | onSurface        |
| coui_color_hint_neutral                                               | #42000000 / #4dffffff | 其余圆点         | disabledOnSurface |
| coui_page_indicator_dot_color（已声明，引导样式未使用）                 | #4d000000 / #4dffffff | -                | 未使用            |
| coui_page_indicator_trace_dot_color（已声明，引导样式未使用）           | #ff666666 / #ffababab | -                | 未使用            |

`Widget.COUI.COUIPageIndicator` 并不从 `coui_page_indicator_*` 取色：`dotColor` 取自 `?attr/couiColorHintNeutral`，`traceDotColor` 取自 `@color/coui_color_label_primary`，深色主题使用 `Widget.COUI.COUIPageIndicator.Dark` 变体。本移植将它们对应到 `disabledOnSurface` 与 `onSurface`。

## 未移植 / 差异

- 滑动窗口背后的逐帧尺寸 / 偏移掩码。原生 `setCurrentPositionInternal` 无法反编译；其分级常量已还原（`INDICATOR_DOT_LARGE/MEDIUM/SMALL/GONE_LEVEL_SIZE` 为 3/5/7/9，`DEFAULT_MAXIMUM_LARGE_DOT_NUMBER` 为 4，`DEFAULT_MAXIMUM_MEDIUM_DOT_NUMBER` 为 2），但确切的偏移算法没有。本移植改为滑动一个钳制窗口，并作为近似，把窗口中相邻页被隐藏的两个圆点缩小为 `DotSizeMedium`。`DotSizeSmall` 仅对外暴露，未实际使用。
- 未移植 `drawTrace` 的移动 trace 混色（`getColorFactor`、`DEFAULT_COLOR_FACTOR_THRESHOLD` 为 0.05）以及 stiffness 为 1500 的位置弹簧（`dampingRatio` 为 1.0、`minimumVisibleChange` 为 0.005）。当前圆点直接使用 `activeColor`。
- 未移植由 `indicator_content_description` 与 `coui_page_indicator_description` 构建的无障碍描述。
- 未移植三个已废弃的空实现 setter（`setDotCornerRadius`、`setDotSpacing`、`setDotStrokeWidth`）以及 `snapToPosition`、`stopTraceAnimator`。
