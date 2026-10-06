# ListDetailView

`ListDetailView` 是 COUI 中的基础布局组件：响应式列表-详情容器，窄窗口一次只显示一个面板，窗口足够宽后拆成「列表 + 详情」双面板布局。对标 ColorOS 的列表-详情布局。

设备类持有三个 `FragmentContainerView`（main / sub / emptyPage）与一条细分隔线，在 `onMeasure` / `onLayout` 里按宽度排布。由于 Fragment 属于 Android 宿主层概念，Compose 移植改用 composable 槽位：`mainContent`、`detailContent` 与 `emptyPageContent`。窗口窄于 COUI `WindowTotalSizeClass.Compact` 分界（600dp）时只组合 `mainContent` / `detailContent` 之一，由 `detailVisible` 决定；达到 600dp 后主面板按比例占宽、后接分隔线，详情（未选中时为 `emptyPageContent`）占满剩余区域。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.ListDetailView
import io.github.suqi8.coui.kmp.basic.ListDetailViewDefaults
```

## 基本用法

```kotlin
var selected by remember { mutableStateOf<Item?>(null) }

ListDetailView(
    detailVisible = selected != null,
    mainContent = { ItemList(onSelect = { selected = it }) },
    detailContent = { selected?.let { ItemDetail(it) } },
    emptyPageContent = { Text("请选择一项") }
)
```

## 属性

| 属性名              | 类型                   | 说明                                       | 默认值                             | 是否必填 |
| ------------------- | ---------------------- | ------------------------------------------ | ---------------------------------- | -------- |
| mainContent         | @Composable () -> Unit | 列表面板，双面板模式位于起始边             | -                                  | 是       |
| detailContent       | @Composable () -> Unit | 详情面板                                   | -                                  | 是       |
| detailVisible       | Boolean                | 详情面板是否被选中                         | -                                  | 是       |
| modifier            | Modifier               | 应用于容器的修饰符                         | Modifier                           | 否       |
| emptyPageContent    | @Composable () -> Unit | 双面板模式下未选中时显示在详情区的占位内容 | {}                                 | 否       |
| mainFragmentPercent | Float                  | 主面板占宽比例，受 min / max 约束          | ListDetailViewDefaults.MainFragmentPercent | 否 |
| minMainWidth        | Dp                     | 主面板宽度下限                             | ListDetailViewDefaults.MinMainWidth | 否      |
| maxMainWidth        | Dp                     | 主面板宽度上限                             | ListDetailViewDefaults.MaxMainWidth | 否      |
| dividerWidth        | Dp                     | 两个面板之间细分隔线的宽度                 | ListDetailViewDefaults.DividerWidth | 否      |
| dividerColor        | Color                  | 细分隔线的颜色                             | COUITheme.colorScheme.dividerLine  | 否       |

## ListDetailViewDefaults 对象

| 属性名              | 类型  | 说明                     | 默认值        |
| ------------------- | ----- | ------------------------ | ------------- |
| MainFragmentPercent | Float | 主面板占宽比例           | 0.4f          |
| MinMainWidth        | Dp    | 主面板宽度下限           | 280.dp        |
| MaxMainWidth        | Dp    | 主面板宽度上限           | 412.dp        |
| DividerWidth        | Dp    | 面板间细分隔线宽度       | 0.33000004.dp |
| SplitModeMinWidth   | Dp    | 出现双面板分栏的宽度阈值 | 600.dp        |

`ListDetailViewDefaults.isSplitMode(width)` 返回给定宽度是否进入双面板分栏。

## 行为与 Token

- **分栏阈值**：设备调用 `WindowSizeClass.calculateFromSize(...).getWindowTotalSizeClass() != Compact`；`Compact` 即宽度小于 `Breakpoints.BP_MEDIUM_WINDOW_BASE_WIDTH`（600dp），因此 600dp 起分栏。
- **主面板宽度**：`min(max(width * percent, maxMainWidth), minMainWidth)` —— `COUIListDetailView.DEFAULT_MAIN_WIDTH_PERCENT = 0.4f`，由 `coui_main_fragment_min_width`（280dp）与 `coui_main_fragment_max_width`（412dp）约束。
- **分隔线**：`coui_fragment_gap_width` = 0.33000004dp，取色自 `?attr/couiColorDivider`（`coui_color_divider`）。
- **窄窗口行为**：设备把 main 与 sub 都按满宽测量、隐藏 emptyPage（`View.GONE`），可见 Fragment 交给宿主；Compose 移植把该选择暴露为 `detailVisible`。
