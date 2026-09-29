# SearchHistoryView

搜索历史面板,对应 ColorOS 的 `COUISearchHistoryView`(`com/coui/appcompat/searchhistory/COUISearchHistoryView`),即位于搜索框下方、把用户最近的搜索词以换行标签形式展示的区块。它由标题栏、[FlowLayout](#flowlayout) 标签流,以及标签放不下时出现的展开箭头按钮三部分组成。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.FlowLayout
import io.github.suqi8.coui.kmp.basic.FlowLayoutDefaults
import io.github.suqi8.coui.kmp.basic.SearchHistoryDeleteIcon
import io.github.suqi8.coui.kmp.basic.SearchHistoryExpandIcon
import io.github.suqi8.coui.kmp.basic.SearchHistoryView
import io.github.suqi8.coui.kmp.basic.SearchHistoryViewColors
import io.github.suqi8.coui.kmp.basic.SearchHistoryViewDefaults
```

## 基本用法

```kotlin
var history by remember { mutableStateOf(listOf("无线耳机", "usb-c 扩展坞", "笔记本支架")) }

SearchHistoryView(
    items = history,
    onItemClick = { query -> /* 执行搜索 */ },
    onDeleteClick = { history = emptyList() },
    title = "最近搜索",
    deleteIconDescription = "删除搜索历史",
)
```

每个条目在 [FlowLayout](#flowlayout) 中渲染为一个未选中的 [Chip](chip),按面板宽度换行,同一行标签之间 8dp,行与行之间 8dp。

`onDeleteClick` 是在面板**淡出之后**才触发的,而不是在点击瞬间:本移植执行原生的 `doDeleteAnimation`,用 `COUIMoveEaseInterpolator`(`cubic-bezier(0.3, 0, 0.1, 1)`)在 `SEARCH_VIEW_DISAPPEAR_DURATION`(300ms)内把整个面板的 alpha 从 1 动到 0,然后才调用回调。面板之后保持透明,与原生视图一致,因此宿主应当移除该面板或重新填充它——可参考下文的[折叠](#折叠)用法。

## 折叠

`maxRowFolded` 是折叠时保留的行数;`maxRowUnfolded` 是展开后的行数。原生构造函数把后者硬编码为 `COUISearchHistoryView.DEFAULT_MAX_ROW_UNFOLDED` = 8,而 `maxRowFolded` 与 `expandable` 来自 XML 属性,原生默认值分别是 `Integer.MAX_VALUE` 与 `true`——所以默认情况下**完全不折叠**,只有当调用方明确要求时面板才会折叠:

```kotlin
SearchHistoryView(
    items = history,
    onItemClick = {},
    onDeleteClick = {},
    title = "最近搜索",
    maxRowFolded = 2,
    expandable = true,
)
```

展开箭头按钮出现在标签流下方,是一个 28dp 的方盒,内含 16dp 的 `drawable/coui_component_expand_arrow_drop_down` 箭头;展开时把它旋转 180 度,结果恰好等于 `drawable/coui_component_expand_arrow_drop_up`。`expandable = false` 会去掉按钮,并把面板永久固定在 `maxRowFolded` 行。

## 属性

### SearchHistoryView

| 属性                  | 类型                    | 说明                                       | 默认值                                 | 必需 |
| --------------------- | ----------------------- | ------------------------------------------ | -------------------------------------- | ---- |
| items                 | List&lt;String&gt;      | 最近搜索,由旧到新                          | -                                      | 是   |
| onItemClick           | (String) -> Unit        | 点击某个标签时的回调,参数为该标签文本       | -                                      | 是   |
| onDeleteClick         | () -> Unit              | 删除动画结束后的回调                       | -                                      | 是   |
| modifier              | Modifier                | 应用于该面板的修饰符                       | Modifier                               | 否   |
| title                 | String                  | 标题栏文本                                 | ""                                     | 否   |
| expandable            | Boolean                 | 面板能否展开                               | true                                   | 否   |
| maxRowFolded          | Int                     | 折叠时显示的行数                           | SearchHistoryViewDefaults.MaxRowFolded | 否   |
| maxRowUnfolded        | Int                     | 展开时显示的行数                           | SearchHistoryViewDefaults.MaxRowUnfolded | 否 |
| deleteIcon            | ImageVector             | 删除按钮中绘制的图形                       | SearchHistoryDeleteIcon                | 否   |
| deleteIconDescription | String?                 | 删除按钮的无障碍标签                       | null                                   | 否   |
| colors                | SearchHistoryViewColors | 颜色                                       | SearchHistoryViewDefaults.colors()     | 否   |

### SearchHistoryViewDefaults

| 常量                | 类型 | 取值           | COUI 来源                                                      |
| ------------------- | ---- | -------------- | -------------------------------------------------------------- |
| PaddingStart        | Dp   | 16.dp          | coui_component_search_history_padding_start(w600dp 为 20dp)     |
| PaddingTop          | Dp   | 16.dp          | coui_component_search_history_padding_top                       |
| PaddingEnd          | Dp   | 16.dp          | coui_component_search_history_padding_end(w600dp 为 20dp)       |
| PaddingBottom       | Dp   | 32.dp          | coui_component_search_history_padding_bottom                    |
| TitleMarginVertical | Dp   | 7.dp           | coui_component_search_history_title_margin_vertical             |
| DeleteIconSize      | Dp   | 18.dp          | coui_component_search_history_delete_icon_size                  |
| DeleteIconEndMargin | Dp   | 0.dp           | coui_component_search_history_delete_icon_end_margin            |
| FlowMarginTop       | Dp   | 8.dp           | coui_component_search_history_flow_margin_top                   |
| FlowItemSpacing     | Dp   | 8.dp           | coui_component_search_history_flow_item_spacing                 |
| FlowLineSpacing     | Dp   | 8.dp           | coui_component_search_history_flow_line_spacing                 |
| ChipHeight          | Dp   | 28.dp          | coui_chip_suggest_style_height                                  |
| ChipCornerRadius    | Dp   | 14.dp          | chipCornerRadius 16777215 → 高度 / 2                            |
| ExpandButtonSize    | Dp   | 28.dp          | coui_chip_default_min_height                                    |
| ExpandIconSize      | Dp   | 16.dp          | drawable/coui_component_expand_arrow_drop_down                  |
| MaxRowFolded        | Int  | Int.MAX_VALUE  | attr/COUISearchHistoryView_maxRowFolded                         |
| MaxRowUnfolded      | Int  | 8              | COUISearchHistoryView.DEFAULT_MAX_ROW_UNFOLDED                  |

#### 方法

| 方法名           | 类型                    | 说明                                                                     |
| ---------------- | ----------------------- | ------------------------------------------------------------------------ |
| titleTextStyle() | TextStyle               | 标题样式(COUITheme.textStyles.body2 取 Medium,COUI couiTextAppearanceButton 14sp sans-serif-medium) |
| colors()         | SearchHistoryViewColors | 从主题解析的面板配色                                                      |

### 颜色

| COUI token                                                      | 取值                  | 用途          | 移植目标           |
| --------------------------------------------------------------- | --------------------- | ------------- | ------------------ |
| attr/couiColorPrimaryNeutral → color/coui_color_primary_neutral  | #e6000000 / #e6ffffff | 标题文本      | onSurface          |
| attr/couiColorSecondNeutral → color/coui_color_secondary_neutral | #8a000000 / #8affffff | 删除图标填充  | onSurfaceSecondary |
| attr/couiColorSecondNeutral → color/coui_color_secondary_neutral | #8a000000 / #8affffff | 展开箭头描边  | onSurfaceSecondary |

删除图标是 `drawable/coui_component_search_history_delete`——一个 18x18dp 的矢量图,只有一条 `fillType="evenOdd"` 的路径,`android:fillColor` 为 `?attr/couiColorSecondNeutral`;箭头是 `drawable/coui_component_expand_arrow_drop_down`,一个 16x16dp 的矢量图,`android:strokeColor="?attr/couiColorSecondNeutral"`,描边宽度 0.933333,端点与拐角均为圆角。两者解析到同一个属性,因此本移植提供两个默认值相同的颜色槽位。

### 文本

| 项目 | 取值                                          | 来源                              |
| ---- | --------------------------------------------- | --------------------------------- |
| 标题 | couiTextAppearanceButton,14sp,sans-serif-medium | COUISearchHistoryView 构造函数     |

## FlowLayout

`FlowLayout`(`com/coui/appcompat/searchhistory/COUIFlowLayout`)是公开的,因为面板就建立在它之上;当你只需要换行行为、不需要标题栏与删除按钮时,可以直接使用它。

### FlowLayout

| 属性        | 类型                       | 说明                              | 默认值                         | 必需 |
| ----------- | -------------------------- | --------------------------------- | ------------------------------ | ---- |
| modifier    | Modifier                   | 应用于该流式布局的修饰符           | Modifier                       | 否   |
| itemSpacing | Dp                         | 同一行两个标签之间的水平间距        | FlowLayoutDefaults.ItemSpacing | 否   |
| lineSpacing | Dp                         | 两行之间的垂直间距                 | FlowLayoutDefaults.LineSpacing | 否   |
| maxRows     | Int                        | 放置的行数;小于等于 0 表示不限制    | FlowLayoutDefaults.MaxRows     | 否   |
| content     | @Composable () -> Unit      | 要排列的标签                       | -                              | 是   |

### FlowLayoutDefaults

| 常量        | 类型 | 取值          | COUI 来源                          |
| ----------- | ---- | ------------- | ---------------------------------- |
| ItemSpacing | Dp   | 0.dp          | attr/COUIFlowLayout_itemSpacing    |
| LineSpacing | Dp   | 0.dp          | attr/COUIFlowLayout_lineSpacing    |
| MaxRows     | Int  | Int.MAX_VALUE | attr/COUIFlowLayout_maxRowFolded / _maxRowUnfolded |

## 行为

- 排布逻辑复刻原生 `Line.canAdd` / `Line.addView`:`usedWidth + itemSpacing + childWidth <= maxWidth`,而一行的第一个子项总是被接受,因此比容器还宽的标签仍能独占一行。行高为该行最高子项的高度,整体高度为 `sum(各行高度) + lineSpacing * (行数 - 1)`,即原生 `calculateLinesHeight`。标签贴着行的上边缘排布,与原生 `Line.layout` 一致;RTL 下每个标签的镜像方式也与原生 `isRtl` 分支相同。
- 标签即由 `res/layout/coui_component_item_search_history` 填充出的 `COUIChip`,也就是 `style/Widget.COUI.Chip.Suggestion`:28dp 高的胶囊,前后内边距各 12dp(`coui_chip_suggest_style_chip_horizontal_padding`),`chipCornerRadius` 16777215 解析为高度的一半。
- 面板起始、顶部与末尾内边距均为 16dp,底部为 32dp;标题栏上下各有 7dp 内边距,标签流位于其下 8dp。原生的起始 / 末尾两个 dimen 还带有 `w600dp` 值 20dp,这是 Kotlin 的 `Dp` 常量无法表达的。
- 面板按「不放置超出 `maxRows` 的行中的子项」实现折叠;原生则保留它们的布局,再用 `View.INVISIBLE` 与 250 / 100 / 400 / 300ms 的淡入淡出隐藏(`EXPAND_BUTTON_APPEAR_DURATION`、`HIDDEN_VIEWS_APPEAR_DELAY`、`HIDDEN_VIEWS_APPEAR_DURATION`、`HIDDEN_VIEWS_DISAPPEAR_DURATION`)。因此这里的展开与折叠是瞬时切换。

## 未移植

- 标签自身的文字样式。`style/Widget.COUI.Chip` 设置 `android:textAppearance="?attr/couiTextBodyXS"`(12sp),但 [Chip](chip) 没有文字样式参数,标签按 `ChipDefaults.textStyle()` 渲染,即 `COUITheme.textStyles.body2` 的 14sp。因此标签比 ColorOS 中大约 2sp。
- 展开箭头按钮居中放在标签流**下方**;原生 `ensureAndExpandButton` 把它追加到最后一条折叠行末尾,当该行放不下时会把该行末位的标签挤出到 `foldLineRemovedChip`。此外,只要 `expandable` 为 true 且 `maxRowFolded < maxRowUnfolded` 本移植就显示按钮,而原生还会先对已排布的标签执行 `isNeedExpandButton`,在它们本就放得进 `maxRowFolded` 行时不显示任何按钮。
- 删除按钮的涟漪背景 `drawable/coui_component_icon_bg`(`android:radius="24dp"`,配 `?attr/couiColorPress`),以及 Suggestion 胶囊样式的 `coui_chip_suggest_style_close_icon*` 系列。
- 展开 / 折叠状态为内部状态。原生暴露了 `setExpandState(isExpand)`,本移植没有对应参数。
- `COUIFlowLayout.clearAll()`、`COUISearchHistoryView.clearAll()` 与 `setItems(List<IItem>)` 通过变更 `items` 列表表达;`IItem.getDisplayContent()` 简化为普通 `String`。
- `COUISearchHistoryView.onConfigurationChanged`,它只重新应用删除图标的末尾外边距。
