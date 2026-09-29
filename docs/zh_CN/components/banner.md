# Banner

`Banner` 是 ColorOS 17 的自动轮播卡片横幅，对应 Clock APK 的 `res/layout/coui_banner_content_layout.xml`：一排横向滚动的圆角卡片，下方是圆点指示器。它由两个类构成，横向滚动部分对应 `COUIBannerRecyclerView`（`com.coui.appcompat.banner`），指示器对应同包的 `COUIPageIndicatorKit`。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.Banner
import io.github.suqi8.coui.kmp.basic.BannerDefaults
import io.github.suqi8.coui.kmp.basic.BannerIndicator
import io.github.suqi8.coui.kmp.basic.BannerIndicatorColors
```

## 基本用法

```kotlin
Banner(
    pageCount = 4,
    autoScrollIntervalMillis = 3000L,
    modifier = Modifier
        .fillMaxWidth()
        .height(160.dp),
) { page ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageColors[page]),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Card $page", color = Color.White)
    }
}
```

分页容器会占满横幅剩余的高度，因此请给横幅指定高度。指示器居中位于卡片行下方，其下留有 `BannerDefaults.IndicatorMarginBottom`（10dp）。

## 轮播间隔是必填参数

原生中完全没有自动轮播。`COUIBannerRecyclerView` 与 `COUIPageIndicatorKit` 都不持有定时器，Clock dex 中也没有任何类引用它们——`coui_banner_content_layout` 是 APK 中一个未被引用的资源——因此轮播间隔取决于宿主应用自己怎么写。正因如此 `autoScrollIntervalMillis` 没有默认值：任何取值都只能是臆造。

传入 `autoScrollEnabled = false`、`pageCount` 不大于 1，或间隔 `<= 0`，都会关闭自动轮播。拖拽过程中不会与手势争抢，而是跳过该次计时。

## 指示器不复用 `PageIndicator`

`BannerIndicator` 移植的是 `COUIPageIndicatorKit`，刻意没有包装 [PageIndicator](pageindicator)。两者共享几何：都读取 `coui_page_indicator_dot_size`（6dp）与 `coui_page_indicator_dot_spacing`（3dp），因此本移植复用了 `PageIndicatorDefaults.DotSize` 与 `PageIndicatorDefaults.DotSpacing`，圆心间距同样是 12dp。但两者能表达的内容不同：

| | BannerIndicator | PageIndicator |
| --- | --- | --- |
| 圆点数量 | 每页一个，无上限（`layoutWidth = dotStepDistance * count`） | 最多 6 个的滑动窗口 |
| 当前标记 | trace 药丸，两端分别移动，因此可以横跨两个圆点 | 改变颜色的圆点 |
| 边缘圆点 | 保持原尺寸 | 在窗口边缘缩小为 `DotSizeMedium` |
| 点击 | 原生中圆点会挂载监听器 | 仅展示 |

`BannerIndicator` 也可以单独使用：

```kotlin
BannerIndicator(
    count = 5,
    currentIndex = page,
)
```

## 属性

### Banner

| 属性名                   | 类型                        | 描述                                        | 默认值                              | 必填 |
| ------------------------ | --------------------------- | ------------------------------------------- | ----------------------------------- | ---- |
| pageCount                | Int                         | 卡片数量；必须与 `state` 一致               | -                                   | 是   |
| autoScrollIntervalMillis | Long                        | 一张卡片停留多久后轮播，单位毫秒            | -                                   | 是   |
| modifier                 | Modifier                    | 应用在横幅上的修饰符                        | Modifier                            | 否   |
| state                    | PagerState                  | 分页状态                                    | 基于 pageCount 的 rememberPagerState | 否  |
| autoScrollEnabled        | Boolean                     | 是否自动轮播                                | true                                | 否   |
| itemCornerRadius         | Dp                          | 卡片圆角                                    | CardDefaults.CornerRadius（17dp）   | 否   |
| itemColor                | Color                       | 卡片填充色                                  | COUITheme.colorScheme.surfaceContainer | 否 |
| contentPadding           | Dp                          | 卡片行的水平内边距                          | 24.dp                               | 否   |
| itemSpacing              | Dp                          | 两张卡片之间的间距                          | 8.dp                                | 否   |
| indicatorMarginBottom    | Dp                          | 指示器下方留白                              | 10.dp                               | 否   |
| indicatorColors          | BannerIndicatorColors       | 指示器的圆点与 trace 颜色                   | BannerDefaults.bannerIndicatorColors() | 否 |
| content                  | @Composable (Int) -> Unit   | 指定页的卡片内容                            | -                                   | 是   |

### BannerIndicator

| 属性名       | 类型                  | 描述                                        | 默认值                                 | 必填 |
| ------------ | --------------------- | ------------------------------------------- | -------------------------------------- | ---- |
| count        | Int                   | 页数；`count <= 0` 时不绘制任何内容         | -                                      | 是   |
| currentIndex | Int                   | 当前页下标，会被钳制到 `0 until count`      | -                                      | 是   |
| modifier     | Modifier              | 应用在指示器上的修饰符                      | Modifier                               | 否   |
| colors       | BannerIndicatorColors | 圆点与 trace 颜色                           | BannerDefaults.bannerIndicatorColors() | 否   |

### BannerIndicatorColors

| 属性       | 类型  | 描述                              |
| ---------- | ----- | --------------------------------- |
| dotColor   | Color | 未选中圆点（`dotColor`）          |
| traceColor | Color | 当前圆点上的 trace 药丸（`traceDotColor`） |

### BannerDefaults

| 常量                                | 类型                        | 值        | COUI 来源                                                       |
| ----------------------------------- | --------------------------- | --------- | --------------------------------------------------------------- |
| ContentPadding                      | Dp                          | 24.dp     | COUIBannerRecyclerView.c setPadding(24, 0, 24, 0)               |
| ItemSpacing                         | Dp                          | 8.dp      | coui_item_decoration_8dp                                        |
| IndicatorMarginBottom               | Dp                          | 10.dp     | COUIPageIndicatorKit 上的 layout_marginBottom                   |
| FlingFrictionScale                  | Float                       | 2.5f      | setHorizontalFlingFriction(getScrollFriction() * 2.5f)          |
| TraceAnimationDurationMillis        | Int                         | 240       | traceAnimator.setDuration(240L)                                 |
| TraceAnimationEasing                | Easing                      | cubic-bezier(0.25, 0.1, 0.25, 1) | PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f)     |
| IndicatorDotColorOnDarkBackground   | Color                       | #4dffffff | color/coui_page_indicator_dot_color_in_dark_bg                  |
| IndicatorTraceColorOnDarkBackground | Color                       | #e6ffffff | color/coui_color_label_primary_dark                             |

#### 方法

| 方法名                  | 类型                  | 描述                                                       |
| ----------------------- | --------------------- | ---------------------------------------------------------- |
| bannerIndicatorColors() | BannerIndicatorColors | 横幅布局指定的颜色，为照片背景而绘制                       |
| indicatorColors()       | BannerIndicatorColors | `Widget.COUI.COUIPageIndicator` 的主题配色，用于普通底面   |

## 颜色

| COUI token                                      | 浅色 / 深色           | 用途                        | 本移植                        |
| ----------------------------------------------- | --------------------- | --------------------------- | ----------------------------- |
| color/coui_page_indicator_dot_color             | #4d000000 / #4dffffff | 未选中圆点（类内默认值）    | 未使用，见下                  |
| color/coui_page_indicator_trace_dot_color       | #ff666666 / #ffababab | trace 药丸（类内默认值）    | 未使用，见下                  |
| attr/couiColorHintNeutral → color/coui_color_hint_neutral | #42000000 / #4dffffff | 未选中圆点（样式） | COUITheme.colorScheme.disabledOnSurface |
| color/coui_color_label_primary                  | #e6000000 / #e6ffffff | trace 药丸（样式）          | COUITheme.colorScheme.onSurface |
| color/coui_page_indicator_dot_color_in_dark_bg  | #4dffffff             | 未选中圆点（横幅布局）      | IndicatorDotColorOnDarkBackground |
| color/coui_color_label_primary_dark             | #e6ffffff             | trace 药丸（横幅布局）      | IndicatorTraceColorOnDarkBackground |

`BannerDefaults.bannerIndicatorColors()` 返回最后两项，因为 `coui_banner_content_layout` 显式设置了 `dotColor` 与 `traceDotColor`，覆盖了样式——Clock 的横幅是为照片背景绘制圆点的，而这两个 `_in_dark_bg` 资源都没有夜间限定。`BannerDefaults.indicatorColors()` 返回主题配色，用于普通底面上的指示器。

`coui_page_indicator_dot_color` 与 `coui_page_indicator_trace_dot_color` 是 `COUIPageIndicatorKit` 字段的初始值，但 APK 中所有样式都会用上面的样式属性覆盖两者，因此这两个值实际不可达。

## 几何

指示器尺寸为 `layoutWidth` x `dotSize`（`onMeasure`），其中 `layoutWidth = dotStepDistance * count`，`dotStepDistance = dotSize + dotSpacing * 2` = 12dp。位置 `p` 的 trace 矩形为 `finalRight = dotSpacing + dotSize + p * dotStepDistance`，`finalLeft = finalRight - dotSize`；在 RTL 布局方向下 `finalRight = layoutWidth - (dotSpacing + p * dotStepDistance)`。圆点与 trace 共用 `dotCornerRadius = dotSize / 2`，因此圆点是圆形，trace 是胶囊形。

卡片行由 `COUIBannerRecyclerView.c()` 配置：横向 `LinearLayoutManager`、`setHorizontalItemAlign(1)`、开启原生 over-scroll、`setHorizontalFlingFriction` 取平台滚动摩擦的 2.5 倍、`setPadding(24dp, 0, 24dp, 0)` 且 `setClipToPadding(false)`，以及一个横向 `DividerItemDecoration`，其 `coui_item_decoration_8dp` 图形是 8dp 宽的透明形状。

## 行为

- trace 药丸两端从原位置沿 `PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f)` 曲线在 240ms 内移动到 `finalLeft` / `finalRight`，移植自 `COUIPageIndicatorKit.c(ValueAnimator)`。原生实际上从未运行该动画——`B()` 是 `traceAnimator.start()` 的唯一调用者，而它只能由 handler 的 `MSG_START_TRACE_ANIMATION`（17）触达，类中没有任何地方发送该消息——因此实际发布的组件是通过 `A(position)` 直接落位。本移植让它动起来，这正是那段死代码原本要做的事。
- 当前圆点不会被重新着色。所有圆点都以 `dotColor` 绘制，trace 胶囊再以 `traceColor` 覆盖其上，与 `dispatchDraw` 在圆点视图之后绘制 `traceRect` 完全一致。
- 原生的更新监听器实际上只移动左边缘，因为它所判断的 `needSettlePositionTemp` 与 `tranceCutTailRight` 分支从未被赋值；本移植对两端都做插值。

## 未移植 / 差异

- 布局中 `COUIBannerRecyclerView` 的那一半。`coui_banner_content_layout` 是 `COUIBannerRecyclerView`、`ViewPager2` 与 `COUIPageIndicatorKit` 三者并列的 `merge`，因此 Clock 应用只会显示两个滚动视图中的一个。本移植保留分页器。
- `setHorizontalItemAlign(1)` 与 `setIsUseNativeOverScroll(true)`，它们是 `COUIRecyclerView` 的能力，Compose 没有对应物。
- 2.5 倍的滑动摩擦：`BannerDefaults.FlingFrictionScale` 记录了该值，但 `HorizontalPager` 没有暴露摩擦系数开关。
- `COUIPageIndicatorKit.setIsClickable` / `setOnDotClickListener`：监听接口是空的（`COUIPageIndicatorKit.e` 没有声明任何成员），点击处理函数体也是空的，因此即使圆点挂载了点击监听器也无法派发任何东西。
- `mPortStickyPath` / `mDepartStickyPath` 的粘连贝塞尔：`mPortPosition` 与 `mDepartPosition` 只会在 `w()` 中被设为 `MIS_POSITION`，而该分支会清空路径，因此没有任何代码填充它们。
- `dotIsStrokeStyle` 与 `coui_page_indicator_dot_stroke`：该样式属性默认 false，APK 中也没有样式把它打开。
- 由 `coui_page_indicator_description` 构建的无障碍描述，报告中未列出该字符串。
- 卡片样式本身：原生 RecyclerView 把条目视图交给宿主应用，因此这里的圆角卡片是本移植新增的。它借用 `CardDefaults.CornerRadius`（17dp，ColorOS 17 中 `Card` 使用的圆角），而不是某个横幅专属取值，因为原生并未定义。
