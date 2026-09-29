# EmptyState

`EmptyState` 是 COUI 中的空状态页组件,用于列表或搜索无内容可显示时,对应 ColorOS 的 COUIEmptyStateView(coui_component_empty_state.xml):居中的插画、标题与副标题组成限高可滚动区域,下方是可选的主题色操作文字。

内容组并非垂直居中。设备把它放在剩余空间的一个比例处——矮窗口取 0.5,其余取 0.45——因此在手机尺寸的页面上它略微偏上。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.EmptyState
import io.github.suqi8.coui.kmp.basic.EmptyStateDefaults
import io.github.suqi8.coui.kmp.basic.EmptyStateSize
```

## 基本用法

```kotlin
EmptyState(
    title = "未找到结果",
    subtitle = "试试搜索 WLAN、蓝牙或电池",
    actionText = "清除搜索内容",
    onActionClick = { /* 清空查询 */ },
    modifier = Modifier.fillMaxSize(),
)
```

给组件一个确定的高度(`fillMaxSize`、`height`、`weight` 等)即可得到设备的摆放位置;高度只是受限或不受限时内容贴在顶部,与设备在高度测量模式不是精确值时的回退行为一致。

## 属性

### EmptyState 属性

| 属性名        | 类型                       | 说明                                                    | 默认值                                | 是否必须 |
| ------------- | -------------------------- | ------------------------------------------------------- | ------------------------------------- | -------- |
| title         | String                     | 居中标题,最多两行(16sp,中等字重)                      | -                                     | 是       |
| modifier      | Modifier                   | 应用于空状态页的修饰符                                  | Modifier                              | 否       |
| subtitle      | String?                    | 居中副标题,最多三行(12sp),为 null 时不显示            | null                                  | 否       |
| actionText    | String?                    | 内容下方主题色操作文字(14sp,中等字重),为 null 时不显示 | null                                 | 否       |
| onActionClick | (() -> Unit)?              | 操作文字点击回调                                        | null                                  | 否       |
| illustration  | (@Composable () -> Unit)?  | 文字上方的插画,置于 120dp × 100dp 的框内               | null                                  | 否       |
| size          | EmptyStateSize             | 尺寸档位,`Auto` 按约束解析                             | EmptyStateSize.Auto                   | 否       |
| colors        | EmptyStateColors           | 文字颜色配置                                            | EmptyStateDefaults.emptyStateColors() | 否       |

### EmptyStateSize

| 取值   | COUI 常量                       | 说明                                 |
| ------ | ------------------------------- | ------------------------------------ |
| Auto   | EMPTY_VIEW_SIZE_TYPE_AUTO (0)   | 按下方阈值对测量尺寸分档             |
| Small  | EMPTY_VIEW_SIZE_TYPE_SMALL (1)  | 窗口高度小于 200dp;不显示插画       |
| Medium | EMPTY_VIEW_SIZE_TYPE_MEDIUM (2) | 窗口宽度小于 320dp 或高度小于 480dp  |
| Large  | EMPTY_VIEW_SIZE_TYPE_LARGE (3)  | 其余情况                             |

`EmptyStateDefaults.sizeType(width, height)` 解析档位;`EmptyStateDefaults.marginTopHeightRatio(size)` 返回内容上方保留的剩余空间比例。

### EmptyStateDefaults 对象

| 常量                          | 类型  | 默认值 | COUI 来源                                        |
| ----------------------------- | ----- | ------ | ------------------------------------------------ |
| IllustrationWidth             | Dp    | 120.dp | coui_component_empty_anim_view_width_normal       |
| IllustrationHeight            | Dp    | 100.dp | coui_component_empty_anim_view_height_normal      |
| ContentWidth                  | Dp    | 280.dp | coui_component_empty_group_width                  |
| ContentMaxHeight              | Dp    | 92.dp  | coui_component_empty_text_scroll_max_height       |
| SubtitleMarginTop             | Dp    | 2.dp   | coui_component_empty_view_subtitle_margin_top     |
| ActionMarginTop               | Dp    | 8.dp   | coui_component_empty_state.xml 中直接写死         |
| ActionRipplePaddingHorizontal | Dp    | 12.dp  | text_ripple_bg_padding_horizontal                 |
| ActionRipplePaddingVertical   | Dp    | 4.dp   | text_ripple_bg_padding_vertical                   |
| MarginTopHeightRatioSmall     | Float | 0.5f   | HEIGHT_RATIO_SMALL                                |
| MarginTopHeightRatioNormal    | Float | 0.45f  | HEIGHT_RATIO_NORMAL                               |
| HeightThresholdSmall          | Dp    | 200.dp | coui_component_height_threshold_small             |
| HeightThresholdMedium         | Dp    | 480.dp | coui_component_height_threshold_medium            |
| WidthThresholdMedium          | Dp    | 320.dp | coui_component_width_threshold_medium             |

### `emptyStateColors()` 工厂

| 参数          | 类型  | 默认值                                   | COUI 角色               |
| ------------- | ----- | ---------------------------------------- | ----------------------- |
| titleColor    | Color | COUITheme.colorScheme.onSurface          | couiColorLabelPrimary   |
| subtitleColor | Color | COUITheme.colorScheme.onSurfaceSecondary | couiColorLabelSecondary |
| actionColor   | Color | COUITheme.colorScheme.primary            | couiColorPrimaryText    |

### EmptyStateColors 类

| 属性名        | 类型  | 说明           |
| ------------- | ----- | -------------- |
| titleColor    | Color | 标题颜色       |
| subtitleColor | Color | 副标题颜色     |
| actionColor   | Color | 操作文字颜色   |

## 行为

- 插画框宽 120dp、高 100dp;在 `EmptyStateSize.Small` 下整个隐藏——设备此时把动画尺寸置零,视图随之退出布局。
- 标题与副标题共用 280dp 一列,合计高度超过 92dp(coui_component_empty_text_scroll_max_height)后开始滚动,且不显示滚动条——设备把它们放进 `android:scrollbars="none"` 的 COUIMaxHeightScrollView。
- 标题最多两行,副标题最多三行,超出均以省略号结尾。
- 操作文字是无底色的主题色纯文字,带 12dp × 4dp 内边距,水波纹即设备用 `COUITextViewCompatUtil.setPressRippleDrawable` 装的 COUI `?couiColorPress` 蒙层;它带按钮无障碍角色,对应设备设置的 `BUTTON_CLASS_NAME`。
- 库内不含插画。COUI 的 `rawAnimRes` 与 `imageRes` 默认都是 -1,`coui_component_empty_state.xml` 也未指定任何一个,设备上这个框由 `COUIEmptyStateView#setRawAnimRes` 传入的 oplus-anim(或 `setImageRes` 传入的静态图)填充。请通过 `illustration` 传入自己的内容,为 null 时不绘制任何内容。

## 进阶用法

### 搜索结果页

ColorOS 相册的搜索页(`search_result_fragment.xml`)把空状态放在结果列表下方,只给标题,并自动播放 `raw/base_empty_view_search_result`:

```kotlin
EmptyState(
    title = "未找到结果",
    illustration = { /* 你的空状态动画或图片 */ },
)
```

### 自定义颜色

```kotlin
EmptyState(
    title = "未找到结果",
    subtitle = "试试搜索 WLAN、蓝牙或电池",
    actionText = "清除搜索内容",
    onActionClick = { /* 清空查询 */ },
    colors = EmptyStateDefaults.emptyStateColors(
        actionColor = COUITheme.colorScheme.primaryContainer
    ),
)
```

`actionColor` 跟随主题主色。COUI 的 couiColorPrimaryText 按强调色主题取值:`coui_color_primary_text_blue` 为 #0080FF,与主题浅色主色完全一致;而主题深色主色是 #1A8CFF,对应 token 的 #5C9DFF,仅深色值有偏差。
