# NavigationPopupMenu

`NavigationPopupMenu` 是 COUI 的底部导航溢出菜单（`COUINavigationPopupMenu`）。它用于展示导航栏中放不下的导航项，以窗口级弹窗的形式出现在锚点上方，并与锚点的结束边对齐。

菜单在导航栏**上方**展开，水平留 16dp 边距、垂直留 8dp 边距，绘制的是本库其他弹窗同款的 28dp 平滑圆角玻璃面板。各行之间紧贴排列，没有分隔带；每行使用 COUI 弹窗列表的按压选择态。

请把它组合在需要锚定的可组合项内部——锚点是弹窗父布局节点的边界。`BottomFloatingNavigationBarItem` 就是用它承载 `overflowMenu` 插槽：长按时，插槽中的各行会显示在锚定到该导航项的 `NavigationPopupMenu` 中。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.NavigationPopupMenu
import io.github.suqi8.coui.kmp.basic.NavigationPopupMenuItem
import io.github.suqi8.coui.kmp.basic.NavigationPopupMenuDefaults
```

## 基本用法

```kotlin
var menuExpanded by remember { mutableStateOf(false) }

Box {
    TextButton(
        text = "显示菜单",
        onClick = { menuExpanded = true },
    )
    NavigationPopupMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
    ) {
        NavigationPopupMenuItem(
            label = "搜索",
            icon = COUIIcons.Search,
            onClick = { menuExpanded = false },
        )
        NavigationPopupMenuItem(
            label = "设置",
            icon = COUIIcons.Settings,
            onClick = { menuExpanded = false },
        )
        NavigationPopupMenuItem(
            label = "关于",
            icon = COUIIcons.More,
            enabled = false,
            onClick = {},
        )
    }
}
```

## 作为导航栏的溢出菜单

`BottomFloatingNavigationBarItem` 提供 `overflowMenu` 插槽；填入内容即启用长按菜单，置为 `null` 则禁用长按：

```kotlin
BottomFloatingNavigationBarItem(
    selected = selectedIndex == 0,
    onClick = { selectedIndex = 0 },
    icon = COUIIcons.Home,
    label = "首页",
    overflowMenu = {
        NavigationPopupMenuItem(
            label = "搜索",
            icon = COUIIcons.Search,
            onClick = { /* ... */ },
        )
    },
)
```

## 属性

### NavigationPopupMenu 属性

| 属性名           | 类型                    | 说明                                       | 默认值   | 是否必须 |
| ---------------- | ----------------------- | ------------------------------------------ | -------- | -------- |
| expanded         | Boolean                 | 是否显示菜单                               | -        | 是       |
| onDismissRequest | () -> Unit              | 用户关闭菜单（点按外部或返回）时调用       | -        | 是       |
| modifier         | Modifier                | 应用于弹窗的修饰符                         | Modifier | 否       |
| content          | @Composable () -> Unit   | 菜单各行，通常是 `NavigationPopupMenuItem` | -        | 是       |

### NavigationPopupMenuItem 属性

| 属性名  | 类型        | 说明                     | 默认值   | 是否必须 |
| ------- | ----------- | ------------------------ | -------- | -------- |
| label   | String      | 行的标题                 | -        | 是       |
| icon    | ImageVector | 行的图标                 | -        | 是       |
| onClick | () -> Unit  | 行被激活时调用           | -        | 是       |
| modifier| Modifier    | 应用于行的修饰符         | Modifier | 否       |
| enabled | Boolean     | 行是否可激活             | true     | 否       |

### NavigationPopupMenuDefaults 对象

#### 常量

| 常量名              | 类型                  | 说明                                                                       | 值      |
| ------------------- | --------------------- | -------------------------------------------------------------------------- | ------- |
| MinWidth            | Dp                    | 行的最小宽度                                                               | 208.dp  |
| MaxHeight           | Dp                    | 菜单最大高度                                                               | 324.dp  |
| HorizontalMargin    | Dp                    | 菜单与导航栏结束边之间的边距，同时是行内图标与标签的内缩                   | 16.dp   |
| VerticalMargin      | Dp                    | 菜单与导航栏之间的边距                                                     | 8.dp    |
| TextSize            | TextUnit              | 行标题字号                                                                 | 16.sp   |
| ItemPaddingVertical | Dp                    | 行的上下内边距                                                             | 12.dp   |
| IconSize            | Dp                    | 行的图标尺寸                                                               | 24.dp   |
| ItemUsedSpace       | Dp                    | 菜单测量宽度时为行的图标与边距预留的空间                                   | 72.dp   |
| PositionProvider    | PopupPositionProvider | 将菜单放在锚点上方并与锚点结束边对齐                                       | -       |

## 定位

`NavigationPopupMenuDefaults.PositionProvider` 复现了 `COUINavigationPopupMenu.tryShow()` 的行为：菜单在锚点上方展开，与锚点的结束边对齐，保留水平与垂直边距，并被约束在窗口内，不会越出屏幕边缘。由于菜单锚定在弹窗父布局节点上，把它包在哪个可组合项里就决定了它出现的位置。
