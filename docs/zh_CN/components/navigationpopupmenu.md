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

## 玻璃版：画在弹层宿主上

`OverlayNavigationPopupMenu` 是同一个菜单，但画在窗口内的弹层宿主上，而不是 `PopupWindow`：定位仍用 `NavigationPopupMenuDefaults.PositionProvider`，宽高仍受 `MinWidth` / `MaxHeight` 约束，材质换成 ColorOS 17 的弹窗玻璃——背后的模糊内容、面板自己的投影、材质边光，也就是相册底栏上那个「更多」菜单的样子。它的行是分组的 `DropdownEntry`，而不是内容插槽，所以带 `children` 的行会展开子菜单。

菜单要压在活的页面内容之上（弹层宿主才拿得到 backdrop 做模糊）时用它；底下没有页面可采样时用 `NavigationPopupMenu`。

```kotlin
import io.github.suqi8.coui.kmp.overlay.OverlayNavigationPopupMenu

Box {
    IconButton(onClick = { menuExpanded = true }) {
        Icon(imageVector = COUIIcons.More, contentDescription = null)
    }
    OverlayNavigationPopupMenu(
        expanded = menuExpanded,
        entries = listOf(
            DropdownEntry(
                items = listOf(
                    DropdownItem(text = "搜索", onClick = { menuExpanded = false }),
                    DropdownItem(text = "设置", onClick = { menuExpanded = false }),
                ),
            ),
        ),
        onDismissRequest = { menuExpanded = false },
    )
}
```

`alignment` 决定菜单贴锚点的哪条边：默认 `BottomEnd` 沿用导航栏右对齐的惯例，锚点在左侧时用 `BottomStart`。它也决定卡片从哪个角长出来。

卡片的展开与收起与相册「更多」菜单一致：沿一条弧线从锚点弹出，同时从零放大、淡入，三者各走各的弹簧；收起时沿原路退回。

收起不再独占触摸：菜单一被关闭，面板照常往锚点缩回去，但触摸已经交还给下方页面，用户不必等退场动画播完就能上下拖动、滚动或左右滑动。落在正在缩小的面板上的按压同样交给页面，不会选中任何一行。

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

### OverlayNavigationPopupMenu 属性

| 属性名           | 类型                      | 说明                                          | 默认值                                        | 是否必须 |
| ---------------- | ------------------------- | --------------------------------------------- | --------------------------------------------- | -------- |
| expanded         | Boolean                   | 是否显示菜单                                  | -                                             | 是       |
| entries          | List<DropdownEntry>       | 菜单各行，与下拉条目的分组方式一致            | -                                             | 是       |
| onDismissRequest | () -> Unit                | 用户关闭菜单（点按外部或返回）时调用          | -                                             | 是       |
| modifier         | Modifier                  | 应用于弹窗的修饰符                            | Modifier                                      | 否       |
| alignment        | PopupPositionProvider.Align | 菜单贴锚点的哪条边                          | `Align.BottomEnd`                             | 否       |
| popupPositionProvider | PopupPositionProvider | 定位策略                                      | `NavigationPopupMenuDefaults.PositionProvider` | 否      |
| minWidth         | Dp                        | 菜单最小宽度                                  | `NavigationPopupMenuDefaults.MinWidth`        | 否       |
| maxHeight        | Dp?                       | 菜单最大高度，`null` 时受安全区约束           | `NavigationPopupMenuDefaults.MaxHeight`       | 否       |
| onDismissFinished | (() -> Unit)?            | 退场动画结束时调用                            | null                                          | 否       |
| renderInRootScaffold | Boolean               | 是否渲染到最外层 `Scaffold` 的弹层宿主         | true                                          | 否       |
| dropdownColors   | DropdownColors            | 每行使用的配色                                | `DropdownDefaults.dropdownColors()`           | 否       |
| collapseOnSelection | Boolean                | 选择任意叶子行是否关闭菜单                    | true                                          | 否       |

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
