# BottomFloatingAccessoryNavigationBar

`BottomFloatingAccessoryNavigationBar` 是 ColorOS 17 的底部悬浮导航栏，两侧各带一个配件视图（`COUIBottomFloatingAccessoryNavigationBar`）。它相当于 `BottomFloatingNavigationBar` 再加上位于导航栏起始边与结束边的两个圆形视图。

两个配件视图都是 56dp 的圆形，与胶囊之间留 12dp 间距。已配置的配件始终会占位，但只有在对应的 `*Visible` 标志置位时才会展开显示：它以 0.5 的缩放起步淡入放大，并从胶囊中滑出，全部由原生弹簧驱动。任一配件展开期间，起始配件、胶囊、结束配件三个轮廓都由同一个 `MetaBallSurface` 绘制，因此配件是从胶囊中「融出」的。两个插槽都未配置时，该组件就是普通的玻璃胶囊，表现与 `BottomFloatingNavigationBar` 完全一致。

导航栏通过 `LocalBackdrop` 采样其后方的画面；没有可用的背景层时保持不透明填充。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.BottomFloatingAccessoryNavigationBar
import io.github.suqi8.coui.kmp.basic.BottomFloatingAccessoryNavigationBarDefaults
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.BottomFloatingRedDot
```

## 基本用法

```kotlin
var selectedIndex by remember { mutableIntStateOf(0) }

BottomFloatingAccessoryNavigationBar(
    startAccessory = {
        // 融合轮廓已经包含了配件所在的圆形，插槽本身不需要再画背景。
        Icon(imageVector = COUIIcons.Search, contentDescription = "搜索")
    },
    endAccessory = {
        Icon(imageVector = COUIIcons.Add, contentDescription = "添加")
    },
) {
    BottomFloatingNavigationBarItem(
        selected = selectedIndex == 0,
        onClick = { selectedIndex = 0 },
        icon = COUIIcons.Home,
        label = "首页",
    )
    BottomFloatingNavigationBarItem(
        selected = selectedIndex == 1,
        onClick = { selectedIndex = 1 },
        icon = COUIIcons.Messages,
        label = "消息",
    )
}
```

## 收起配件视图

`startAccessoryVisible` 与 `endAccessoryVisible` 分别控制两侧配件在展开与收起之间切换。无论是否展开，插槽都保持已配置状态，占位依旧保留，只有视图本身参与动画：

```kotlin
BottomFloatingAccessoryNavigationBar(
    startAccessoryVisible = showSearch,
    startAccessory = {
        Icon(imageVector = COUIIcons.Search, contentDescription = "搜索")
    },
) {
    // ... 导航项 ...
}
```

## 属性

### BottomFloatingAccessoryNavigationBar 属性

| 属性名                | 类型                                | 说明                                                          | 默认值                    | 是否必须 |
| --------------------- | ----------------------------------- | ------------------------------------------------------------- | ------------------------- | -------- |
| modifier              | Modifier                            | 应用于导航栏的修饰符                                          | Modifier                  | 否       |
| containerColor        | Color                               | 胶囊与融合轮廓的背景颜色                                      | COUITheme.colorScheme.bar | 否       |
| backdrop              | Backdrop?                           | 导航栏后方被模糊的内容；`null` 时保持不透明                   | LocalBackdrop.current     | 否       |
| startAccessoryVisible | Boolean                             | 起始配件视图是否展开                                          | true                      | 否       |
| endAccessoryVisible   | Boolean                             | 结束配件视图是否展开                                          | true                      | 否       |
| startAccessory        | (@Composable BoxScope.() -> Unit)?  | 起始边的视图；`null` 表示未配置，不占位                       | null                      | 否       |
| endAccessory          | (@Composable BoxScope.() -> Unit)?  | 结束边的视图；`null` 表示未配置，不占位                       | null                      | 否       |
| content               | @Composable RowScope.() -> Unit     | 导航栏内容，通常是 `BottomFloatingNavigationBarItem`          | -                         | 是       |

### BottomFloatingAccessoryNavigationBarDefaults 对象

#### 常量

| 常量名                   | 类型              | 说明                                        | 值                          |
| ------------------------ | ----------------- | ------------------------------------------- | --------------------------- |
| AccessorySize            | Dp                | 配件视图的直径                              | 56.dp                       |
| SideSpacing              | Dp                | 胶囊与配件视图之间的间距                    | 12.dp                       |
| PaddingHorizontal        | Dp                | 小屏下保留的外部内边距                      | 16.dp                       |
| MinScale                 | Float             | 配件视图动画的起始缩放                      | 0.5f                        |
| AlphaSpring              | SpringSpec<Float> | 配件的透明度弹簧                            | 响应 0.35 / 回弹 0          |
| ScaleSpring              | SpringSpec<Float> | 配件的缩放弹簧                              | 响应 0.55 / 回弹 0.35       |
| TranslationXSpring       | SpringSpec<Float> | 让配件从胶囊中滑出的弹簧                    | 响应 0.55 / 回弹 0.35       |
| LowEndAlphaSpring        | SpringSpec<Float> | 设备报告低动效等级时使用的透明度弹簧        | 响应 0.2 / 回弹 0           |
| LowEndScaleSpring        | SpringSpec<Float> | 低动效等级的缩放弹簧                        | 响应 0.5 / 回弹 0.3         |
| LowEndTranslationXSpring | SpringSpec<Float> | 低动效等级的位移动画弹簧                    | 响应 0.5 / 回弹 0.3         |

## 响应式边距

与普通悬浮导航栏一致，16dp 的外部内边距只在小屏下保留，中屏与大屏下收缩为 0。胶囊会在两个配件视图让出的空间中居中。

`onLayout` 在同一档位上会在 `layoutPositionsSmallScreen` 与 `layoutPositionsMediumScreen` 之间选择，但这个选择在这里观察不到。只有小屏方法在反编译后存活，它把每个配件视图贴到导航栏各自的外侧边缘，并把菜单居中于两者之间——本实现已经如此，因为原生组件把自身测量为「内容 + 内边距」，而本移植同样包裹内容。

## 与 BottomFloatingNavigationBar 的关系

两者共用导航项、红点与默认值：`BottomFloatingNavigationBarItem` 与 `BottomFloatingRedDot` 在这里原样使用，胶囊高度、圆角、导航项度量与触摸高亮都来自 `BottomFloatingNavigationBarDefaults`。不需要侧边配件时使用 `BottomFloatingNavigationBar`，需要在胶囊两侧放置一到两个圆形操作入口时使用本组件。
