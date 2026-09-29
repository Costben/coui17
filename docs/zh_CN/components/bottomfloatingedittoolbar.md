# BottomFloatingEditToolbar

`BottomFloatingEditToolbar` 是 ColorOS 17 的悬浮编辑工具栏（`COUIBottomFloatingEditToolbar`）。它是一条由 `BottomFloatingEditToolbarItem` 组成的胶囊，从屏幕下边缘滑入、再滑出，动画由原生显示 / 隐藏弹簧驱动。

它是 `BottomFloatingNavigationBar` 的工具栏姊妹组件：两者都是悬浮玻璃胶囊，但编辑工具栏的导航项排布方式不同，且在宽屏下更矮。胶囊通过 `LocalBackdrop` 采样其后方的画面；没有背景层时保持不透明填充，圆角半径为菜单高度的一半。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.BottomFloatingEditToolbar
import io.github.suqi8.coui.kmp.basic.BottomFloatingEditToolbarItem
import io.github.suqi8.coui.kmp.basic.BottomFloatingEditToolbarDefaults
import io.github.suqi8.coui.kmp.basic.BottomFloatingEditToolbarStyle
import io.github.suqi8.coui.kmp.basic.LocalBottomFloatingEditToolbarStyle
import io.github.suqi8.coui.kmp.basic.BottomFloatingRedDot
```

## 基本用法

```kotlin
var visible by remember { mutableStateOf(true) }

BottomFloatingEditToolbar(
    visible = visible,
) {
    BottomFloatingEditToolbarItem(
        onClick = { /* 复制 */ },
        icon = COUIIcons.Copy,
        label = "复制",
    )
    BottomFloatingEditToolbarItem(
        onClick = { /* 删除 */ },
        icon = COUIIcons.Delete,
        label = "删除",
        redDot = BottomFloatingRedDot.Count(3),
    )
    BottomFloatingEditToolbarItem(
        onClick = { /* 分享 */ },
        icon = COUIIcons.Share,
        label = "分享",
        enabled = false,
    )
}
```

## 显示与隐藏

`visible` 驱动滑动动画：工具栏向下平移「刚好让胶囊越过下边缘」的距离，恢复时回到 0。隐藏期间工具栏仍保留 88dp 的最小外部高度，因此布局不会跳动：

```kotlin
BottomFloatingEditToolbar(visible = isEditing) {
    // ... 工具栏项 ...
}
```

## 导航项排布

导航项的排布跟随窗口档位。小屏下使用纵向布局——图标在上、单行 10sp 标题在下；中屏与大屏下切换为横向布局——14sp 标题位于图标旁边，最多可占两行。工具栏通过 `LocalBottomFloatingEditToolbarStyle` 把当前选择发布给各个导航项，该组合局部值默认为 `BottomFloatingEditToolbarStyle.Vertical`；导航项会自行读取，无需额外接线。

## 属性

### BottomFloatingEditToolbar 属性

| 属性名         | 类型                    | 说明                                            | 默认值                    | 是否必须 |
| -------------- | ----------------------- | ----------------------------------------------- | ------------------------- | -------- |
| modifier       | Modifier                | 应用于工具栏的修饰符                            | Modifier                  | 否       |
| containerColor | Color                   | 胶囊的背景颜色                                  | COUITheme.colorScheme.bar | 否       |
| backdrop       | Backdrop?               | 胶囊后方被模糊的内容；`null` 时保持不透明       | LocalBackdrop.current     | 否       |
| visible        | Boolean                 | 工具栏是否在屏幕上                              | true                      | 否       |
| content        | @Composable () -> Unit   | 工具栏内容，通常是 `BottomFloatingEditToolbarItem` | -                      | 是       |

### BottomFloatingEditToolbarItem 属性

| 属性名  | 类型                  | 说明                             | 默认值   | 是否必须 |
| ------- | --------------------- | -------------------------------- | -------- | -------- |
| onClick | () -> Unit            | 导航项被激活时调用               | -        | 是       |
| icon    | ImageVector           | 导航项的图标                     | -        | 是       |
| label   | String                | 导航项的标题                     | -        | 是       |
| modifier| Modifier              | 应用于导航项的修饰符             | Modifier | 否       |
| enabled | Boolean               | 导航项是否可激活                 | true     | 否       |
| redDot  | BottomFloatingRedDot? | 固定在图标顶部结束角的红点       | null     | 否       |

### BottomFloatingEditToolbarStyle 枚举

| 值         | 说明                                     |
| ---------- | ---------------------------------------- |
| Horizontal | 宽布局：图标与可占两行的标题并排         |
| Vertical   | 窄布局：图标在上、单行标题在下           |

### BottomFloatingEditToolbarDefaults 对象

#### 常量

| 常量名                          | 类型              | 说明                                       | 值                          |
| ------------------------------- | ----------------- | ------------------------------------------ | --------------------------- |
| MenuHeightSmall                 | Dp                | 小屏下的胶囊高度                           | 56.dp                       |
| MenuHeightMediumLarge           | Dp                | 中屏与大屏下的胶囊高度                     | 44.dp                       |
| MinHeightWithoutPaddingVertical | Dp                | 最小外部高度                               | 88.dp                       |
| PaddingHorizontalSmallScreen    | Dp                | 小屏下的外部内边距                         | 16.dp                       |
| PaddingHorizontalMediumScreen   | Dp                | 中屏下的外部内边距                         | 24.dp                       |
| PaddingHorizontalLargeScreen    | Dp                | 大屏下的外部内边距                         | 40.dp                       |
| InnerPaddingHorizontal          | Dp                | 胶囊端边与首尾导航项之间的内边距           | 10.dp                       |
| InnerPaddingVertical            | Dp                | 窄布局下的同一内边距                       | 12.dp                       |
| ItemHeightHorizontal            | Dp                | 宽布局下的导航项高度                       | 40.dp                       |
| ItemHeightVertical              | Dp                | 窄布局下的导航项高度                       | 48.dp                       |
| ItemMaxWidthHorizontal          | Dp                | 宽布局下单项的宽度上限                     | 98.dp                       |
| ItemMaxWidthVertical            | Dp                | 窄布局下单项的宽度上限                     | 70.dp                       |
| SingleItemWidthHorizontal       | Dp                | 宽布局下单项独占时的宽度                   | 152.dp                      |
| SingleItemWidthVertical         | Dp                | 窄布局下单项独占时的宽度                   | 148.dp                      |
| ItemSpacing                     | Dp                | 导航项之间的间距                           | 8.dp                        |
| IconSize                        | Dp                | 图标尺寸                                   | 24.dp                       |
| IconTopMargin                   | Dp                | 窄布局下图标的上边距                       | 4.dp                        |
| IconTitleGapVertical            | Dp                | 窄布局下图标与标题之间的垂直间距           | 2.dp                        |
| IconTitleGapHorizontal          | Dp                | 宽布局下图标与标题之间的水平间距           | 4.dp                        |
| LabelFontSizeHorizontal         | TextUnit          | 宽布局下的标题字号                         | 14.sp                       |
| LabelFontSizeVertical           | TextUnit          | 窄布局下的标题字号                         | 10.sp                       |
| TranslationYSpring              | SpringSpec<Float> | 显示 / 隐藏弹簧                            | 响应 0.38 / 回弹 0.25       |
