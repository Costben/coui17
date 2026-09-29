# TopTips

页面顶部提示条，对应 ColorOS 的整套顶部提示组件（`COUICustomTopTips`、`COUIDefaultTopTips`、`COUIDefaultTopTipsView`、`COUIMarqueeTopTips`）：圆角卡片内含可选的前置图标、可选标题、文案，以及最多两个强调色文字按钮加一个圆形关闭按钮组成的尾部区。切换可见性会播放 COUI 的 250ms 纵向缩放动画。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.TopTips
import io.github.suqi8.coui.kmp.basic.TopTipsBackgroundStyle
import io.github.suqi8.coui.kmp.basic.TopTipsColors
import io.github.suqi8.coui.kmp.basic.TopTipsDefaults
import io.github.suqi8.coui.kmp.basic.TopTipsStyle
```

## 基本用法

```kotlin
TopTips(
    text = "发现新的安全更新",
    actionText = "更新",
    onAction = { /* 处理行动 */ },
    modifier = Modifier.fillMaxWidth(),
)
```

## 带前置图标的可关闭提示

```kotlin
var visible by remember { mutableStateOf(true) }

TopTips(
    text = "带前置图标的可关闭提示",
    visible = visible,
    startIcon = {
        Icon(
            imageVector = COUIIcons.Basic.Search,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    },
    onClose = { visible = false },
    modifier = Modifier.fillMaxWidth(),
)
```

## AI 支付样式

`TopTipsStyle.AiPayment` 解析为 `TopTipsLayoutMode.AiPayment`，会用 `coui_toptips_ai_payment_btn_min_width`（144dp）、`coui_toptips_ai_payment_btn_min_height`（32dp）和 `coui_toptips_ai_payment_btn_horizontal_padding`（12dp）把两个按钮并排摆放。该布局下 `changeLayoutWithDualButtons` 会隐藏 `close`，所以当 `ignoreText` 与 `actionText` 同时存在时 `onClose` 不生效。

```kotlin
TopTips(
    text = "确认这笔交易前请先核对支付信息",
    title = "AI 支付",
    startIcon = { Icon(COUIIcons.Basic.Search, contentDescription = null) },
    style = TopTipsStyle.AiPayment,
    ignoreText = "暂不",
    onIgnore = { /* 关闭 */ },
    actionText = "继续",
    onAction = { /* 确认 */ },
    modifier = Modifier.fillMaxWidth(),
)
```

## 背景样式

`backgroundStyle` 决定纯色填充，并作为 `colors` 的默认值。`TopTipsBackgroundStyle.White` 使用 `?attr/couiColorCard` 填充，`Gray`（`mLastBackgroundStyle` 的默认值）使用 `?attr/couiColorContainer4`。

```kotlin
TopTips(
    text = "白色提示条使用 couiColorCard 填充",
    backgroundStyle = TopTipsBackgroundStyle.White,
    actionText = "知道了",
    onAction = {},
    modifier = Modifier.fillMaxWidth(),
)
```

## 属性

### TopTips

| 属性              | 类型                       | 说明                                                     | 默认值                                     | 必需 |
| ----------------- | -------------------------- | -------------------------------------------------------- | ------------------------------------------ | ---- |
| text              | String                     | 提示文案                                                 | -                                          | 是   |
| modifier          | Modifier                   | 应用于提示条的修饰符                                     | Modifier                                   | 否   |
| visible           | Boolean                    | 是否可见（切换时播放进出动画）                            | true                                       | 否   |
| title             | String?                    | 文案上方的可选标题（最多 2 行）                          | null                                       | 否   |
| startIcon         | (@Composable () -> Unit)?  | 可选前置图标                                             | null                                       | 否   |
| actionText        | String?                    | 主行动按钮的可选文案                                     | null                                       | 否   |
| onAction          | (() -> Unit)?              | 行动按钮点击回调                                         | null                                       | 否   |
| ignoreText        | String?                    | 次级按钮的可选文案                                       | null                                       | 否   |
| onIgnore          | (() -> Unit)?              | 次级按钮点击回调                                         | null                                       | 否   |
| onClose           | (() -> Unit)?              | 关闭按钮回调                                             | null                                       | 否   |
| marqueeEnabled    | Boolean                    | 文案溢出时是否横向滚动                                   | true                                       | 否   |
| style             | TopTipsStyle               | `Standard` 或 `AiPayment`；决定布局模式                  | TopTipsStyle.Standard                      | 否   |
| backgroundStyle   | TopTipsBackgroundStyle     | 纯色填充：`White`（`couiColorCard`）或 `Gray`（`couiColorContainer4`） | TopTipsBackgroundStyle.Gray     | 否   |
| backgroundOverlay | (@Composable () -> Unit)?  | 覆盖在填充之上的叠层；设置后卡片填充变透明                | null                                       | 否   |
| cornerRadius      | Dp?                        | 显式圆角；null 时遵循原生圆角规则                        | null                                       | 否   |
| colors            | TopTipsColors              | 颜色配置                                                 | TopTipsDefaults.topTipsColors(backgroundStyle) | 否 |

### TopTipsDefaults

| 常量                   | 类型 | 值     | COUI 来源                                  |
| ---------------------- | ---- | ------ | ------------------------------------------ |
| CornerRadius           | Dp   | 28.dp  | coui_toptips_view_radius（ColorOS 17）     |
| SingleLineCornerRadius | Dp   | 72.dp  | couiRoundCornerFULL                        |
| ContentPadding         | Dp   | 12.dp  | coui_toptips_view_content_top_margin       |
| IconSize               | Dp   | 24.dp  | coui_toptips_view_icon_btn_size            |
| TitleIconSize          | Dp   | 36.dp  | coui_toptips_view_title_icon_size          |
| IconSpacing            | Dp   | 8.dp   | coui_toptips_view_content_start_margin     |
| TextMinHeight          | Dp   | 20.dp  | coui_toptips_view_content_min_height       |
| TitleMinHeight         | Dp   | 22.dp  | coui_toptips_view_title_min_height         |

`getMultiLineRadius` 对不是「无标题单行」的提示条使用 `CornerRadius`；`getSingleLineRadius` 对能放进一行的无标题文案使用 `SingleLineCornerRadius`。

#### 方法

| 方法名                 | 类型          | 说明                                                          |
| ---------------------- | ------------- | ------------------------------------------------------------- |
| titleStyle()           | TextStyle     | 标题默认样式（16sp Medium，`couiTextHeadlineXS`）              |
| textStyle()            | TextStyle     | 文案默认样式（13sp，`couiTextBodyS`）                          |
| actionTextStyle()      | TextStyle     | 按钮文案默认样式（13sp Medium，`coui_toptips_view_default_text_size`） |
| topTipsColors()        | TopTipsColors | 创建默认颜色配置（见下）                                       |
| topTipsContainerColor()| Color         | 某个 `TopTipsBackgroundStyle` 对应的纯色填充                   |

### `topTipsColors()` 工厂

| 参数             | 类型                   | 默认值                                         | COUI 属性              |
| ---------------- | ---------------------- | ---------------------------------------------- | ---------------------- |
| backgroundStyle  | TopTipsBackgroundStyle | TopTipsBackgroundStyle.Gray                    | mLastBackgroundStyle   |
| containerColor   | Color                  | topTipsContainerColor(backgroundStyle)          | couiColorCard / couiColorContainer4 |
| contentColor     | Color                  | COUITheme.colorScheme.onSurfaceSecondary        | couiColorLabelSecondary |
| actionColor      | Color                  | COUITheme.colorScheme.primary                   | couiColorPrimaryText   |
| closeButtonColor | Color                  | 亮色 #29000000 / 暗色 #40FFFFFF                 | couiColorControls      |
| closeIconColor   | Color                  | Color.White                                     | coui_ic_toptips_close  |
| titleColor       | Color                  | COUITheme.colorScheme.onSurface                 | couiColorLabelPrimary  |

## 枚举

| 枚举                   | 成员                | 来源                                                           |
| ---------------------- | ------------------- | -------------------------------------------------------------- |
| TopTipsStyle           | Standard, AiPayment | `COUIDefaultTopTips.TopTipsStyle`（`STANDARD`、`AI_PAYMENT`）  |
| TopTipsBackgroundStyle | White, Gray         | `COUIDefaultTopTips.BackgroundStyle`（`WHITE`、`GRAY`）        |
| TopTipsLayoutMode      | Default, AiPayment  | `COUIDefaultTopTipsView.LayoutMode`，由 `resolveTargetLayoutMode` 得到 |

## 行为

- 尾部区顺序为 `ignore`、`action`、`close`，与 `COUIDefaultTopTipsView` 一致。AI 支付双按钮布局（`changeLayoutWithDualButtons`）会隐藏 `close`。
- 当单行文案将与尾部区相撞时，尾部区下移到文字下方靠右单独一行（`isNeedMultiText`）；此布局中前置图标与第一行文字对齐。
- 前置图标在无标题时为 `IconSize`（24dp），有标题时为 `TitleIconSize`（36dp）。
- 文案溢出时会滚动，对应 `COUIMarqueeTopTips.startRoll` / `stopRoll`；`marqueeEnabled` 切换的是同一状态。
- 切换 `visible` 会播放 `COUICustomTopTips` 的显示/消失动画：以中心为轴的 250ms 纵向缩放；消失动画结束后提示条移出组合。
- 文案为 13sp（`couiTextBodyS`），按钮文案为 13sp Medium，标题为 16sp Medium（`couiTextHeadlineXS`）。
