# HorizontalPopupMenu

`HorizontalPopupMenu` 是 ColorOS 17 弹窗列表中的横向图标菜单块。它把各项按等宽列铺满弹窗宽度：只要有一项带标签就固定为 3 列，否则根据项数在 3 到 5 列之间取值。

每一项都是方形按钮——图标在上、可选标签在下——整项覆盖一层平滑圆角的按压蒙版。纯图标项使用 24dp 图标与 40dp 方形；带标签的项使用 20dp 图标，下方是 12sp、最多两行、居中的标题。请把该块组合在弹窗内容中，与纵向行并列。

与原生适配器有两处刻意不同的做法：COUI 会丢弃超出列数上限的横向项，而本组件把多出的项换到第二行网格；COUI 在该块之后插入的分隔带则由 `ListPopupColumn` 的测量策略负责，只会为相邻的已注册行添加分隔。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.HorizontalPopupMenu
import io.github.suqi8.coui.kmp.basic.HorizontalPopupMenuItem
import io.github.suqi8.coui.kmp.basic.HorizontalPopupMenuDefaults
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.window.WindowListPopup
```

## 基本用法

```kotlin
var showPopup by remember { mutableStateOf(false) }

Box {
    TextButton(
        text = "显示菜单",
        onClick = { showPopup = true },
    )
    WindowListPopup(
        show = showPopup,
        onDismissRequest = { showPopup = false },
    ) {
        val dismiss = LocalDismissState.current
        ListPopupColumn {
            HorizontalPopupMenu {
                HorizontalPopupMenuItem(
                    icon = COUIIcons.Copy,
                    label = "复制",
                    onClick = { dismiss?.invoke() },
                )
                HorizontalPopupMenuItem(
                    icon = COUIIcons.Share,
                    label = "分享",
                    onClick = { dismiss?.invoke() },
                )
                HorizontalPopupMenuItem(
                    icon = COUIIcons.Delete,
                    label = "删除",
                    onClick = { dismiss?.invoke() },
                )
            }
        }
    }
}
```

## 纯图标项

把 `label` 留为 `null`（或空字符串）即选择纯图标形态。此时该项采用 40dp 方形、24dp 图标与纯图标内边距，`contentDescription` 为按钮提供无障碍名称：

```kotlin
HorizontalPopupMenu {
    HorizontalPopupMenuItem(
        icon = COUIIcons.Copy,
        contentDescription = "复制",
        onClick = { /* ... */ },
    )
    HorizontalPopupMenuItem(
        icon = COUIIcons.Delete,
        contentDescription = "删除",
        onClick = { /* ... */ },
    )
}
```

各项的宽度由 `HorizontalPopupMenu` 精确测量，调用方无需指定尺寸。

## 属性

### HorizontalPopupMenu 属性

| 属性名  | 类型                    | 说明                                              | 默认值   | 是否必须 |
| ------- | ----------------------- | ------------------------------------------------- | -------- | -------- |
| modifier| Modifier                | 应用于该块的修饰符                                | Modifier | 否       |
| content | @Composable () -> Unit   | `HorizontalPopupMenuItem`，按组合顺序排列         | -        | 是       |

### HorizontalPopupMenuItem 属性

| 属性名             | 类型        | 说明                                                       | 默认值 | 是否必须 |
| ------------------ | ----------- | ---------------------------------------------------------- | ------ | -------- |
| icon               | ImageVector | 绘制在标签上方的图标                                       | -      | 是       |
| onClick            | () -> Unit  | 项被激活时调用                                             | -      | 是       |
| modifier           | Modifier    | 应用于该项的修饰符                                         | Modifier | 否     |
| label              | String?     | 绘制在图标下方的标签；`null` 或空字符串选择纯图标形态      | null   | 否       |
| enabled            | Boolean     | 该项是否响应输入                                           | true   | 否       |
| contentDescription | String?     | 纯图标项的无障碍名称；带标签形态由标签本身命名             | null   | 否       |

### HorizontalPopupMenuDefaults 对象

#### 常量

| 常量名                    | 类型     | 说明                                                     | 值      |
| ------------------------- | -------- | -------------------------------------------------------- | ------- |
| ColumnsWithText           | Int      | 至少有一项带标签时的列数                                 | 3       |
| MaxIconOnlyColumns        | Int      | 纯图标块的最多列数                                       | 5       |
| MinColumns                | Int      | 纯图标块的最少列数                                       | 3       |
| MinColumnWidth            | Dp       | 单列最小宽度                                             | 52.dp   |
| IconOnlySize              | Dp       | 纯图标项的高度，同时也是其宽度下限                       | 40.dp   |
| TextMinHeight             | Dp       | 带标签项的最小高度                                       | 40.dp   |
| TextPaddingHorizontal     | Dp       | 带标签项的水平内边距                                     | 6.dp    |
| TextPaddingVertical       | Dp       | 带标签项的垂直内边距                                     | 6.dp    |
| IconOnlyPaddingHorizontal | Dp       | 纯图标项的水平内边距                                     | 8.dp    |
| IconSizeWithText          | Dp       | 带标签项的图标尺寸                                       | 20.dp   |
| IconSizeIconOnly          | Dp       | 纯图标项的图标尺寸                                       | 24.dp   |
| IconTitleGap              | Dp       | 图标与标签之间的间距                                     | 4.dp    |
| TextSpacing               | Dp       | 两个带标签项之间的间距                                   | 4.dp    |
| IconOnlySpacing           | Dp       | 两个纯图标项之间的间距                                   | 8.dp    |
| SpacingMin                | Dp       | 由容器推导出的间距不得低于的下限                         | 4.dp    |
| TextEdgeMargin            | Dp       | 带标签块的首尾边距                                       | 8.dp    |
| IconOnlyEdgeMargin        | Dp       | 纯图标块的首尾边距                                       | 12.dp   |
| MaskRadiusIconOnly        | Dp       | 纯图标项的按压蒙版圆角，`OS <= 37` 圆弧半径              | 12.dp   |
| MaskRadiusIconOnlySmooth  | Dp       | 平滑圆角世代下纯图标项的按压蒙版圆角                     | 17.dp   |
| MaskRadiusWithText        | Dp       | 带标签项的按压蒙版圆角，`OS <= 37` 圆弧半径              | 16.dp   |
| MaskRadiusWithTextSmooth  | Dp       | 平滑圆角世代下带标签项的按压蒙版圆角                     | 23.dp   |
| LabelFontSize             | TextUnit | 标签字号                                                 | 12.sp   |

## 尺寸

该块是弹窗中最宽的子项，因此它会在弹窗列表的最小与最大宽度区间内自定尺寸（`ListPopupDefaults.MinWidth` 168dp、`ListPopupDefaults.MaxWidth` 256dp）。首尾边距与项间距由块的宽度和项数推导：默认的边距与间距会先被限制在容器可承受的范围内，剩余量再分摊到项的两侧，因此每个纯图标列都能落在 40dp 方形上。
