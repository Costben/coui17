# BottomFloatingToolbar

`BottomFloatingToolbar` 是 ColorOS 17 的悬浮文本选择工具栏（`COUIBottomFloatingToolbar`）。它由一条或多条 `BottomFloatingToolbarItem` 组成的胶囊构成，悬浮在内容之上，从屏幕下边缘滑入、再滑出。

它是 `BottomFloatingNavigationBar` 与 `BottomFloatingScrollToolbar` 的文本选择 / 便签姊妹组件：滑动动画相同，但导航项按组划分——文本选择工具栏会把「剪切 / 复制 / 粘贴」放在一条胶囊里，「全选 / 翻译」放在另一条里——并且每条胶囊使用 ColorOS 17 的材质描边，而不是导航栏使用的玻璃高光。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.BottomFloatingToolbar
import io.github.suqi8.coui.kmp.basic.BottomFloatingToolbarGroup
import io.github.suqi8.coui.kmp.basic.BottomFloatingToolbarItem
import io.github.suqi8.coui.kmp.basic.BottomFloatingToolbarDefaults
```

## 基本用法

```kotlin
var visible by remember { mutableStateOf(true) }

BottomFloatingToolbar(
    visible = visible,
) {
    BottomFloatingToolbarGroup {
        BottomFloatingToolbarItem(
            label = "复制",
            icon = COUIIcons.Copy,
            onClick = { /* 复制 */ },
        )
        BottomFloatingToolbarItem(
            label = "剪切",
            icon = COUIIcons.Cut,
            onClick = { /* 剪切 */ },
        )
        BottomFloatingToolbarItem(
            label = "删除",
            icon = COUIIcons.Delete,
            onClick = { /* 删除 */ },
            enabled = false,
        )
    }
}
```

## 分组

每个 `BottomFloatingToolbarGroup` 都会被绘制成独立的胶囊，因此组的边界就是一条可见的间隙。分组的摆放与原生 `COUIBottomFloatingToolbar.onLayout` 一致：

- 一组时居中；
- 两组时分别贴起始边与结束边，并按组边距内缩；
- 三组及以上时首尾两组贴边，其余在两者之间均分，与相邻组的间距不小于 16dp。

组边距跟随宽度档位——600dp 以下为 16dp，600dp 至 840dp 为 24dp，840dp 以上为 40dp——与原生 `COUIBottomFloatingToolbar.onMeasure` 相同。

```kotlin
BottomFloatingToolbar {
    BottomFloatingToolbarGroup {
        BottomFloatingToolbarItem(label = "全选", icon = COUIIcons.SelectAll, onClick = {})
        BottomFloatingToolbarItem(label = "翻译", onClick = {})
    }
    BottomFloatingToolbarGroup {
        BottomFloatingToolbarItem(label = "分享", icon = COUIIcons.Share, onClick = {})
    }
}
```

## 显示与隐藏

`visible` 驱动滑动动画：工具栏向下平移「刚好让胶囊越过容器下边缘」的距离，恢复时回到 0，动画由显示 / 隐藏弹簧驱动。隐藏期间工具栏仍保留 76dp 的最小外部高度，因此布局不会跳动。

```kotlin
BottomFloatingToolbar(visible = isSelecting) {
    // ... 分组 ...
}
```

## 胶囊表面

胶囊通过 `LocalBackdrop` 采样其后方的画面。存在背景层、平台支持运行时着色器且系统启用了材质模糊（`SystemMaterialState.blurEnabled`）时，胶囊会被模糊并叠加工具栏配色；否则回退为不透明的 `containerColor` 填充。两种情况下，描边与内阴影都由 `Modifier.materialShadowEdge` 以 `TYPE_FRAMEWORK_CAPSULE_13` 参数绘制，胶囊下方带有系统焦散光影（不支持系统焦散时回退为 `BottomFloatingToolbarDefaults.Shadow` 投影）。按压与拖动通过 `couiTouchMotion` 产生形变、橡皮筋位移与缩放，并携带 `SpotLightDefaults.opaqueMedium2` 聚光反馈。

## 多色图标

传入 `Painter` 而非 `ImageVector` 时，图标会保留自身原始颜色而不被单色着色，适用于便签底部悬浮工具栏中的渐变 AI 写作图标等场景：

```kotlin
BottomFloatingToolbarItem(
    label = "AI 写作",
    onClick = { /* 打开 AI 写作 */ },
    painter = painterResource(Res.drawable.note_toolbar_ai),
)
```

## 属性

### BottomFloatingToolbar 属性

| 属性名         | 类型                  | 说明                                             | 默认值                    | 是否必须 |
| -------------- | --------------------- | ------------------------------------------------ | ------------------------- | -------- |
| modifier       | Modifier              | 应用于工具栏的修饰符                             | Modifier                  | 否       |
| containerColor | Color                 | 每条胶囊的背景颜色，以及叠加在模糊上的着色       | COUITheme.colorScheme.bar | 否       |
| backdrop       | Backdrop?             | 胶囊后方被模糊的内容；`null` 时保持不透明        | LocalBackdrop.current     | 否       |
| visible        | Boolean               | 工具栏是否在屏幕上                               | true                      | 否       |
| content        | @Composable () -> Unit | 工具栏内容，一个或多个 `BottomFloatingToolbarGroup` | -                      | 是       |

### BottomFloatingToolbarGroup 属性

| 属性名  | 类型                  | 说明                     | 默认值   | 是否必须 |
| ------- | --------------------- | ------------------------ | -------- | -------- |
| modifier| Modifier              | 应用于胶囊的修饰符       | Modifier | 否       |
| content | @Composable () -> Unit | 胶囊内容，通常是导航项   | -        | 是       |

### BottomFloatingToolbarItem 属性（矢量图标 / 纯文字重载）

| 属性名             | 类型         | 说明                                             | 默认值 | 是否必须 |
| ------------------ | ------------ | ------------------------------------------------ | ------ | -------- |
| label              | String       | 导航项的标签；同时也是图标的无障碍描述           | -      | 是       |
| onClick            | () -> Unit   | 导航项被激活时调用                               | -      | 是       |
| icon               | ImageVector? | 导航项的图标，`null` 时只显示文字                | null   | 否       |
| enabled            | Boolean      | 导航项是否可激活                                 | true   | 否       |
| contentDescription | String?      | 图标的无障碍描述，`null` 时使用 label            | null   | 否       |

### BottomFloatingToolbarItem 属性（Painter 多色图标重载）

| 属性名             | 类型       | 说明                                             | 默认值   | 是否必须 |
| ------------------ | ---------- | ------------------------------------------------ | -------- | -------- |
| label              | String     | 导航项的标签；同时也是图标的无障碍描述           | -        | 是       |
| onClick            | () -> Unit | 导航项被激活时调用                               | -        | 是       |
| painter            | Painter    | 导航项的图标，不叠加单色滤镜                     | -        | 是       |
| modifier           | Modifier   | 应用于导航项的修饰符                             | Modifier | 否       |
| enabled            | Boolean    | 导航项是否可激活                                 | true     | 否       |
| contentDescription | String?    | 图标的无障碍描述，`null` 时使用 label            | null     | 否       |

### BottomFloatingToolbarDefaults 对象

| 常量名                          | 类型                        | 说明                                       | 值                           |
| ------------------------------- | --------------------------- | ------------------------------------------ | ---------------------------- |
| MenuViewHeight                  | Dp                          | 分组胶囊的高度                             | 44.dp                        |
| MinHeight                       | Dp                          | 工具栏的最小外部高度                       | 76.dp                        |
| MenuViewPadding                 | Dp                          | 原生菜单视图在胶囊外侧保留的留白           | 16.dp                        |
| MenuViewItemBgInsetHorizontal   | Dp                          | 胶囊端边与首尾导航项之间的内缩间距         | 6.dp                         |
| Shadow                          | Shadow                      | 无系统焦散接口时的回退投影                 | BarShadow                    |
| ItemWidth                       | Dp                          | 图标单元格宽度                             | 44.dp                        |
| ItemHeight                      | Dp                          | 图标单元格高度                             | 40.dp                        |
| ItemViewSpace                   | Dp                          | 胶囊内导航项之间的间距                     | 4.dp                         |
| IconSize                        | Dp                          | 图标尺寸                                   | 24.dp                        |
| LabelFontSize                   | TextUnit                    | 标签字号                                   | 16.sp                        |
| ItemTextPaddingHorizontal       | Dp                          | 文字单元格的水平内边距                     | 16.dp                        |
| ItemTextPaddingVertical         | Dp                          | 文字单元格的垂直内边距                     | 10.dp                        |
| ItemTextMinWidth                | Dp                          | 文字单元格的最小宽度                       | 52.dp                        |
| ItemTextMaxWidth                | Dp                          | 文字单元格的最大宽度                       | 200.dp                       |
| ItemCircleRadius                | Dp                          | 导航项圆形触摸遮罩的半径                   | 20.dp                        |
| MaskViewIconRadius              | Dp                          | 图标项后方圆形遮罩的半径                   | 20.dp                        |
| MenuTouchEffectRadius           | Dp                          | 径向按压高光的半径                         | 40.dp                        |
| ItemSingleMaskInsetHorizontal   | Dp                          | 单项遮罩相对胶囊端边的内缩量               | 2.dp                         |
| RedDotOffset                    | Dp                          | 红点相对图标的水平偏移                     | 12.dp                        |
| RedDotRadius                    | Dp                          | 红点直径                                   | 6.dp                         |
| MinGroupSpace                   | Dp                          | 两条胶囊之间保留的最小间距                 | 16.dp                        |
| GroupMarginSmallScreen          | Dp                          | 小屏下最外侧胶囊的边距                     | 16.dp                        |
| GroupMarginMediumScreen         | Dp                          | 中屏下的同一间距                           | 24.dp                        |
| GroupMarginLargeScreen          | Dp                          | 大屏下的同一间距                           | 40.dp                        |
| BlurRadius                      | Float                       | 模糊半径（dp）                             | 25f                          |
| BlurColorsLight                 | List&lt;BlendColorEntry&gt; | 浅色主题下叠加在模糊上的着色层             | `#99F5F5F5` Luminosity、`#B3666666` ColorDodge |
| BlurColorsDark                  | List&lt;BlendColorEntry&gt; | 深色主题下的同一组着色层                   | `#B3262626` Luminosity、`#80666666` Overlay |
| TranslationYSpring              | SpringSpec&lt;Float&gt;     | 显示 / 隐藏弹簧                            | 响应 0.38 / 回弹 0.75        |

`BottomFloatingToolbarDefaults` 还提供两个函数：

| 函数名              | 签名                                                            | 说明                             |
| ------------------- | --------------------------------------------------------------- | -------------------------------- |
| capsuleCornerRadius | (height: Dp, paddingTop: Dp = 0.dp, paddingBottom: Dp = 0.dp): Dp | 胶囊圆角半径，`(高度 - 内边距) / 2` |
| blurColors          | @Composable (isDark: Boolean): BlurColors                        | 按主题取记忆化的着色层           |

## 原生资源对照

| 默认值                        | COUI 资源名                                                |
| ----------------------------- | ---------------------------------------------------------- |
| MenuViewHeight                | `coui_floating_toolbar_menu_view_height`                   |
| MinHeight                     | `coui_floating_toolbar_min_height`                         |
| MenuViewPadding               | `coui_floating_toolbar_menu_view_padding`                  |
| MenuViewItemBgInsetHorizontal | `coui_floating_toolbar_menu_view_item_bg_inset_horizontal` |
| ItemWidth                     | `coui_floating_toolbar_item_width`                         |
| ItemHeight                    | `coui_floating_toolbar_item_height`                        |
| ItemViewSpace                 | `coui_floating_toolbar_item_view_space`                    |
| IconSize                      | `coui_floating_toolbar_item_icon_width` / `..._icon_height` |
| LabelFontSize                 | `coui_floating_toolbar_text_size`                          |
| ItemTextPadding*              | `coui_floating_toolbar_item_text_padding_horizontal` / `..._vertical` |
| ItemTextMinWidth              | `coui_floating_toolbar_item_text_min_width`                |
| ItemTextMaxWidth              | `coui_floating_toolbar_item_text_max_width`                |
| ItemCircleRadius              | `coui_floating_toolbar_item_circle_radius`                 |
| MaskViewIconRadius            | `coui_floating_toolbar_mask_view_icon_radius`              |
| MenuTouchEffectRadius         | `coui_floating_toolbar_menu_touch_effect_radius`           |
| ItemSingleMaskInsetHorizontal | `coui_floating_toolbar_item_single_mask_inset_horizontal`  |
| RedDotOffset                  | `coui_floating_toolbar_red_dot_offset`                     |
| RedDotRadius                  | `coui_floating_toolbar_red_dot_radius`                     |
| MinGroupSpace                 | `coui_floating_toolbar_min_group_space`                    |
| GroupMargin*                  | `coui_floating_toolbar_group_margin_small_screen` / `..._medium_screen` / `..._large_screen` |

## 未移植内容

- 仅框架可用的 `OplusMaterialEdgeParams` 路径（`TYPE_FRAMEWORK_CAPSULE_5`），即运行时着色器不可用时的兜底；这里由不透明的 `containerColor` 代替。
- 分组插入 / 移除动画（`COUIBottomFloatingToolbarAction`）与 XML 菜单加载（`inflateMenu`）。
