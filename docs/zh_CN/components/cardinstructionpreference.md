# CardInstructionPreference

`CardInstructionPreference` 是 COUI 的卡片说明块:一张带插图、标题与描述的卡片,用于引导用户了解某项设置,对标 ColorOS 17 设计规范。页面布局按 `cardType` 切换:

- **Description** —— 由插图 / 标题 / 摘要页组成的 `ViewPager2`,下方 24dp 处是 `COUIPageIndicator`。
- **Selector** —— 一行可选的插图 / 标题 / 单选选项,等分排列,之间以 24dp 分隔。

动画视图(OPLUS 的 effective animation view)由调用方提供的 `illustration` 槽替代。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.CardInstructionItem
import io.github.suqi8.coui.kmp.preference.CardInstructionPreference
import io.github.suqi8.coui.kmp.preference.CardInstructionPreferenceDefaults
import io.github.suqi8.coui.kmp.preference.CardInstructionType
```

## 基本用法

```kotlin
CardInstructionPreference(
    items = listOf(
        CardInstructionItem("智能整理", "按人物、地点和时间自动归集照片"),
        CardInstructionItem("云端同步", "开启后可在其他设备继续浏览"),
    ),
    cardType = CardInstructionType.Description,
    illustration = { index -> Illustration(index) },
    onSelectedIndexChange = { /* 记录当前页 */ },
)
```

## 属性

### CardInstructionPreference 属性

| 属性名                | 类型                              | 说明                                                | 默认值                                                     | 是否必须 |
| --------------------- | --------------------------------- | --------------------------------------------------- | ---------------------------------------------------------- | -------- |
| items                 | `List<CardInstructionItem>`         | 页(Description)或选项(Selector)                   | -                                                          | 是       |
| modifier              | Modifier                          | 应用于卡片的修饰符                                  | Modifier                                                   | 否       |
| cardType              | CardInstructionType               | 使用哪套设备适配器,见 [CardInstructionType](#cardinstructiontype) | CardInstructionType.Description            | 否       |
| selectedIndex         | Int                               | 当前页或当前选中项                                  | 0                                                          | 否       |
| onSelectedIndexChange | (Int) -> Unit                     | 当前页变化时的回调                                  | {}                                                         | 否       |
| colors                | CardInstructionColors             | 卡片配色                                            | CardInstructionPreferenceDefaults.cardInstructionColors()  | 否       |
| illustration          | (@Composable (index: Int) -> Unit)? | 第 index 项的插图,为 null 时不显示                | null                                                       | 否       |

### CardInstructionItem

| 属性名  | 类型    | 说明                                          | 是否必须 |
| ------- | ------- | --------------------------------------------- | -------- |
| title   | String  | 页面标题(`?couiTextAppearanceHeadline6`)      | 是       |
| summary | String? | 页面摘要,为 null 时不显示该行                 | 否       |

### CardInstructionType

| 取值        | 设备常量                            | 布局                                                              |
| ----------- | ----------------------------------- | ----------------------------------------------------------------- |
| Description | `CARD_INSTRUCTION_TYPE_DESCRIPTION` (1) | 插图 / 标题 / 摘要页的 pager(`HorizontalPager` + `PageIndicator`) |
| Selector    | `CARD_INSTRUCTION_TYPE_SELECTOR` (2)    | 一行可选的插图 / 标题 / 单选项                                   |

### CardInstructionPreferenceDefaults 对象

| 常量                    | 类型          | 值                                  | 设备来源                                                          |
| ----------------------- | ------------- | ----------------------------------- | ----------------------------------------------------------------- |
| CornerRadius            | Dp            | 16.dp                               | `coui_component_card_instruction_bg` 的 `<corners android:radius>` |
| ContentPadding          | PaddingValues | horizontal 16.dp, vertical 24.dp    | `coui_component_card_instruction_preference.xml` 根内边距         |
| ContentHeightComplete   | Dp            | 102.dp                              | `coui_component_card_instruction_content_height_complete`         |
| ContentHeightPart       | Dp            | 64.dp                               | `coui_component_card_instruction_content_height_part`             |
| ContentFadingEdgeLength | Dp            | 40.dp                               | `coui_component_card_instruction_content_fading_edge_length` —— 未移植 |
| DividerWidth            | Dp            | 24.dp                               | `coui_component_card_instruction_divider_width`                   |
| AnimMarginEnd           | Dp            | 16.dp                               | `coui_component_card_instruction_anim_margin_end`                 |
| SummaryMarginTopSmall   | Dp            | 12.dp                               | `coui_component_card_instruction_summary_margin_top_small`        |
| SummaryMarginTopLarge   | Dp            | 24.dp                               | `coui_component_card_instruction_summary_margin_top_large`        |
| IndicatorMarginTop      | Dp            | 24.dp                               | 页码指示器的 `layout_marginTop`                                   |
| AnimMarginTop           | Dp            | 24.dp                               | description 内容容器的外边距,用作插图与文字的间距                |
| SelectorTitleMarginTop  | Dp            | 24.dp                               | selector 项标题 `layout_marginTop`                                |
| SelectorRadioMarginTop  | Dp            | 4.dp                                | selector 项单选 `layout_marginTop`                                |
| SelectorRadioMarginBottom | Dp          | 24.dp                               | selector 项单选 `layout_marginBottom`                             |
| SummaryTextSize         | TextUnit      | 12.sp                               | `couiTextAppearanceDescription`                                   |

`ContentHeightPart` 是设备在页面没有标题时对描述块的高度上限;本库的 `CardInstructionItem` 始终带标题,因此只会用到 `ContentHeightComplete`。

### `cardInstructionColors()` 工厂

| 参数                   | 类型  | 默认值                                     | COUI 角色                  |
| ---------------------- | ----- | ------------------------------------------ | -------------------------- |
| containerColor         | Color | COUITheme.colorScheme.surfaceContainer     | `?couiColorCardBackground` |
| titleColor             | Color | COUITheme.colorScheme.onBackground         | `?couiColorPrimaryNeutral` |
| summaryColor           | Color | COUITheme.colorScheme.onSurfaceSecondary   | `?couiColorSecondNeutral`  |
| indicatorActiveColor   | Color | COUITheme.colorScheme.onSurface            | `coui_color_label_primary` |
| indicatorInactiveColor | Color | COUITheme.colorScheme.disabledOnSurface    | `coui_color_hint_neutral`  |

### CardInstructionColors 类

| 属性名                 | 类型  | 说明                        |
| ---------------------- | ----- | --------------------------- |
| containerColor         | Color | 卡片背景                    |
| titleColor             | Color | 标题颜色                    |
| summaryColor           | Color | 摘要 / selector 标题颜色    |
| indicatorActiveColor   | Color | 激活圆点颜色                |
| indicatorInactiveColor | Color | 未激活圆点颜色              |

## 行为

- **Description** 渲染一个 `HorizontalPager`,仅在条目多于一个时显示页码指示器。`selectedIndex` 驱动 pager,pager 通过 `onSelectedIndexChange` 回报变化。
- 每个描述页居中排布:标题用 COUI `headline2` 文字样式(居中,最多两行),摘要用 `footnote1` 的 12sp,位于标题下 12dp。文本块上限 102dp,溢出时可滚动。
- **Selector** 把条目排进 `Row`,每项 `weight = 1f`,之间留 24dp 间隔。每项是可点击的 `Column`(role 为 `RadioButton`),含插图、其下 24dp 的标题(`button` 文字样式)与 24dp 的 `RadioButton`(上 4dp,下 24dp)。
- `items` 为空时组件不渲染任何内容。

## 未移植

由于设备类是 Android View 基础设施,`ViewPager2`、`COUIPageIndicator` 视图(由仓库 `basic/PageIndicator.kt` 顶替)、`RecyclerView.Adapter` 体系(`BaseCardInstructionAdapter`、`CardInstructionDescriptionAdapter`、`CardInstructionSelectorAdapter`)与 `COUIMutableSizeScrollView` 均未移植。40dp 的内容渐隐边同样未移植 —— 描述块直接以 `ContentHeightComplete` 为上限。

## 进阶用法

### Selector 卡片

```kotlin
CardInstructionPreference(
    items = listOf(
        CardInstructionItem("每天"),
        CardInstructionItem("每周"),
        CardInstructionItem("每月"),
    ),
    cardType = CardInstructionType.Selector,
    selectedIndex = period,
    onSelectedIndexChange = { period = it },
    illustration = { index -> PeriodIcon(index) },
)
```

### 自定义颜色

```kotlin
CardInstructionPreference(
    items = instructions,
    colors = CardInstructionPreferenceDefaults.cardInstructionColors(
        containerColor = COUITheme.colorScheme.surfaceContainerHigh,
    ),
)
```
