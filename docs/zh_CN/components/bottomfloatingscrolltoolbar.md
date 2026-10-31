# BottomFloatingScrollToolbar

`BottomFloatingScrollToolbar` 是 ColorOS 17 相册编辑器的横向可滚动主工具胶囊（`picture3d_editor_preview_toolbar`）。每个 `BottomFloatingScrollToolbarItem` 由 24dp 图标与紧凑的 8dp 单行标题上下叠放组成；当工具项超出胶囊宽度时，列表支持带越界回弹的横向滚动，并在仍有内容可滚的一侧以 50dp 渐变遮罩柔和淡出。

它是 `BottomFloatingToolbar` 的带文字可滚动姊妹组件：外层胶囊通过 `couiTouchMotion` 响应手指按压（形变、橡皮筋位移、按压缩放与 `TYPE_CAPSULE_1` 白色聚光灯），同时不拦截内部列表的横向滚动手势。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.BottomFloatingScrollToolbar
import io.github.suqi8.coui.kmp.basic.BottomFloatingScrollToolbarItem
import io.github.suqi8.coui.kmp.basic.BottomFloatingScrollToolbarColors
import io.github.suqi8.coui.kmp.basic.BottomFloatingScrollToolbarDefaults
```

## 基本用法

```kotlin
var selected by remember { mutableIntStateOf(0) }

BottomFloatingScrollToolbar {
    tools.forEachIndexed { index, tool ->
        BottomFloatingScrollToolbarItem(
            onClick = { selected = index },
            icon = tool.icon,
            label = tool.label,
            selected = index == selected,
        )
    }
}
```

## 横向滚动、边缘渐隐与相邻项自动显露

- **胶囊尺寸**：胶囊包裹内部工具项并在可用宽度内居中，最大宽度为 `460.dp`，左右外侧各保留 `16.dp` 边距。
- **边缘渐隐**：当 `scrollState` 在某一侧仍有可滚动距离时，靠近该侧边缘的内容会在 `50.dp`（`base_editor_fading_edge_width`）宽度内平滑渐隐，并严格裁剪在 28dp 胶囊轮廓内。
- **点击显露相邻项**：点击靠近视口左端或右端的工具项时，列表会以 `298ms` 三次减速曲线平滑滚动，把该侧紧邻的下一项完整带入内容内边距之内（对齐原生 `EditorLinearListView`），无需手动拖拽即可连续点按相邻工具。

## 胶囊表面与配色

由于系统相册编辑器固定采用深色画布，`BottomFloatingScrollToolbarDefaults.colors()` 默认使用编辑器的深色配方（`#232323` 底色、`TYPE_FRAMEWORK_CAPSULE_13` 材质描边叠加 `0.67.dp` 的 `#0DFFFFFF` 细描边、未选中项 `#73FFFFFF`、选中项 `#FFB200`）。用于其他背景时可传入自定义 `BottomFloatingScrollToolbarColors`。

胶囊的填充、描边、裁剪与触摸高亮共用一个 `CapsuleOutlineShape`，走系统胶囊曲线（`addSegmentCapsule`）；材质描边按胶囊角权重绘制（宽胶囊 1.0，方形胶囊 2.0）。

## 属性

### BottomFloatingScrollToolbar 属性

| 属性名      | 类型                              | 说明                         | 默认值                                       | 是否必须 |
| ----------- | --------------------------------- | ---------------------------- | -------------------------------------------- | -------- |
| modifier    | Modifier                          | 应用于工具栏的修饰符         | Modifier                                     | 否       |
| colors      | BottomFloatingScrollToolbarColors | 胶囊与工具项的颜色配置       | BottomFloatingScrollToolbarDefaults.colors() | 否       |
| scrollState | ScrollState                       | 工具项行的横向滚动状态       | rememberScrollState()                        | 否       |
| content     | @Composable () -> Unit            | 工具栏内容，通常是工具项     | -                                            | 是       |

### BottomFloatingScrollToolbarItem 属性

| 属性名   | 类型        | 说明                       | 默认值   | 是否必须 |
| -------- | ----------- | -------------------------- | -------- | -------- |
| onClick  | () -> Unit  | 工具项被点击时调用         | -        | 是       |
| icon     | ImageVector | 工具项图标                 | -        | 是       |
| label    | String      | 图标下方的单行文字标签     | -        | 是       |
| modifier | Modifier    | 应用于工具项的修饰符       | Modifier | 否       |
| selected | Boolean     | 是否为当前选中的工具       | false    | 否       |
| enabled  | Boolean     | 工具项是否可用             | true     | 否       |

### BottomFloatingScrollToolbarColors

| 属性名               | 类型  | 说明                         | 默认值      |
| -------------------- | ----- | ---------------------------- | ----------- |
| containerColor       | Color | 胶囊填充底色                 | `#FF232323` |
| strokeColor          | Color | 材质描边下方的细描边颜色     | `#0DFFFFFF` |
| contentColor         | Color | 常态工具项的图标与文字颜色   | `#73FFFFFF` |
| selectedContentColor | Color | 选中工具项的图标与文字颜色   | `#FFFFB200` |
| disabledContentColor | Color | 禁用工具项的图标与文字颜色   | `#33FFFFFF` |

### BottomFloatingScrollToolbarDefaults 对象

| 常量名               | 类型                   | 说明                                       | 值                            |
| -------------------- | ---------------------- | ------------------------------------------ | ----------------------------- |
| Height               | Dp                     | 胶囊与工具项高度                           | 56.dp                         |
| CornerRadius         | Dp                     | 胶囊圆角；56dp 胶囊上的 28dp 走系统胶囊曲线 | 28.dp                         |
| MaxWidth             | Dp                     | 胶囊最大宽度                               | 460.dp                        |
| MarginHorizontal     | Dp                     | 胶囊两侧外边距                             | 16.dp                         |
| ContentPadding       | Dp                     | 滚动行首尾内边距                           | 16.dp                         |
| FadingEdgeWidth      | Dp                     | 可滚动边缘的渐隐宽度                       | 50.dp                         |
| ItemWidth            | Dp                     | 单个工具项宽度                             | 51.dp                         |
| ItemSpacing          | Dp                     | 相邻工具项间距                             | 0.dp                          |
| IconSize             | Dp                     | 工具项图标尺寸                             | 24.dp                         |
| IconLabelGap         | Dp                     | 图标与文字之间的间距                       | 1.dp                          |
| LabelWidth           | Dp                     | 文字标签宽度上限                           | 49.dp                         |
| LabelSize            | Dp                     | 固定文字尺寸（不随系统字号缩放）           | 8.dp                          |
| LabelLineHeight      | Dp                     | 固定文字行高                               | 14.dp                         |
| ContainerColor       | Color                  | 默认胶囊填充色                             | `Color(0xFF232323)`           |
| StrokeColor          | Color                  | 默认细描边色                               | `Color(0x0DFFFFFF)`           |
| StrokeWidth          | Dp                     | 细描边线宽                                 | 0.67.dp                       |
| ContentColor         | Color                  | 默认常态图标与文字颜色                     | `Color(0x73FFFFFF)`           |
| SelectedContentColor | Color                  | 默认选中图标与文字颜色                     | `Color(0xFFFFB200)`           |
| DisabledContentColor | Color                  | 默认禁用图标与文字颜色                     | `Color(0x33FFFFFF)`           |
| LabelShadow          | Shadow                 | 文字底部的可读性阴影                       | `#4C000000`，模糊半径 `12f`   |
| RevealSpec           | TweenSpec&lt;Float&gt; | 点击显露相邻工具项的滚动动画规格           | 298ms，三次减速曲线           |

`BottomFloatingScrollToolbarDefaults` 还提供两个 `@Composable` 函数：

| 函数名     | 签名                                                 | 说明                               |
| ---------- | ---------------------------------------------------- | ---------------------------------- |
| colors     | @Composable (...): BottomFloatingScrollToolbarColors | 创建并记忆化工具栏颜色集合         |
| labelStyle | @Composable (): TextStyle                            | 按固定 `LabelSize` dp 换算的中等字重样式 |

## 原生资源对照

| 默认值               | ColorOS 相册资源名                              |
| -------------------- | ----------------------------------------------- |
| Height               | `photoeditorpage_main_menu_portrait_height`     |
| CornerRadius         | `photo_editor_main_menu_pill_corner_radius`     |
| MaxWidth             | `photo_editor_main_menu_pill_max_size`          |
| MarginHorizontal     | `photo_editor_main_menu_pill_margin_horizontal` |
| FadingEdgeWidth      | `base_editor_fading_edge_width`                 |
| ItemWidth            | `photoeditorpage_main_menu_item_width`          |
| IconSize             | `base_editor_main_menu_item_icon_height_width`  |
| LabelWidth           | `photoeditorpage_main_menu_item_text_width`     |
| ContainerColor       | `photo_editor_main_menu_pill_fill_color`        |
| StrokeColor          | `photo_editor_main_menu_pill_stroke_color`      |
| StrokeWidth          | `base_editor_menu_item_stroke_width`            |
| ContentColor         | `picture3d_preview_menu_no_select_normal_color` |
| SelectedContentColor | `base_editor_theme_color`                       |
| DisabledContentColor | `picture3d_preview_menu_no_select_gray_color`   |
| LabelShadow          | `base_editor_menu_item_text_shadow_color`       |
