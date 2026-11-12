# ChipTab

横向可滚动的胶囊页签栏,对应 ColorOS 位于 `ChipMaskHorizontalScrollView` 内的 `COUIChipTab` / `COUIChipTabGroup`。页签是 32dp 高的胶囊,标签 14sp,可选 16dp 前置图标、红点与计数;同一时刻只有一个页签选中,选中会把该页签滚动到可见区域内,被滚出的一侧在 12dp 内渐隐。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.ChipTab
import io.github.suqi8.coui.kmp.basic.ChipTabData
import io.github.suqi8.coui.kmp.basic.ChipTabDefaults
import io.github.suqi8.coui.kmp.basic.ChipTabGroup
```

## 基本用法

```kotlin
var selectedIndex by remember { mutableIntStateOf(0) }
val tabs = remember {
    listOf(
        ChipTabData(label = "All"),
        ChipTabData(label = "Photos"),
        ChipTabData(label = "Videos"),
        ChipTabData(label = "Documents"),
    )
}

ChipTabGroup(
    tabs = tabs,
    selectedIndex = selectedIndex,
    onSelectedChange = { selectedIndex = it },
    modifier = Modifier.fillMaxWidth(),
)
```

该组接收显式的页签列表而非 content lambda,因为索引由组自己持有:单选状态与滚动到可见位置都由列表下标推导,与 `TabRow(tabs, selectedTabIndex, onTabSelected)` 一致。页签之间由 `ChipTabDefaults.GroupSpacing`(8dp,`coui_chip_group_default_spacing_horizontal`)分隔。

## 带前置图标

每个页签可带自己的 16dp 图标(`coui_chip_tab_style_chip_icon_size`),着色与标签一致,并与标签相隔 4dp(`coui_chip_suggest_style_chip_icon_end_padding`):

```kotlin
val tabs = listOf(
    ChipTabData(label = "All", icon = COUIIcons.All),
    ChipTabData(label = "Photos", icon = COUIIcons.Photos),
    ChipTabData(label = "Music", icon = COUIIcons.Music),
)
```

## 仅图标的前置 chip

`leadingIcon` 会加上 `COUIChipTabLayout` 的仅图标 chip,即便签应用在文件夹页签前显示的那一颗:32dp 圆形 chip,内含 20dp 图标(`ICON_SIZE_20`,左右内边距 6dp),始终呈选中样式,页签滚动时它固定不动。此时页签从它之后 8dp 处开始(`PADDING_HORIZONTAL_WITH_ICON_ONLY_CHIP`)。

```kotlin
ChipTabGroup(
    tabs = tabs,
    selectedIndex = selectedIndex,
    onSelectedChange = { selectedIndex = it },
    modifier = Modifier.fillMaxWidth(),
    leadingIcon = COUIIcons.GridView,
    onLeadingIconClick = { /* 打开文件夹列表 */ },
    contentPadding = PaddingValues(horizontal = 16.dp),
)
```

`contentPadding` 位于滚动区域内部,与原生滚动容器 16dp 的 `paddingHorizontal` 加 `clipToPadding = false` 一致:页签会一直滚到渐隐边缘,而不是提前 16dp 被裁掉。组件横跨屏幕或卡片时,优先用它而不是外层 `Modifier.padding`。

## 选中样式

选中的页签按 ColorOS 17 上 `COUIChipTab` 的方式抬起:一道沿整个轮廓描边的白色高光——顶边最亮、跟着圆角拐下去时仍有 3px 粗、走到直立边时已经消失——底边上方再有一段渐升到白的辉光,浅色下左边缘有一层内阴影、右边缘有一层极淡的提升,填充为实测的 `#F5F6F7`(浅色)/ `#4B4B4B`(深色),外加柔和的投影(`ChipTabDefaults.CheckedShadow`,按便签应用实测拟合)以及深色下沿的焦散光(`coui_caustic_color_3`)。高光的几何与强度均取自便签应用的真机实测,见 `drawChipTabCheckedRim` 上方的常量。以上均随选中渐显。组会在渐隐层之外绘制投影,请在上下方留出空间而不要裁切它。页签本体不会被滚动器裁掉:渐隐层与滚动容器上下各多留 `ChipTabDefaults.PressOutset` 再整体上移,松手回弹的过冲可以完整绘出,而行本身仍只占页签的高度。

## 红点与计数

```kotlin
val tabs = listOf(
    ChipTabData(label = "Inbox", showRedDot = true),
    ChipTabData(label = "Updates", count = 12),
    ChipTabData(label = "Archived", count = 128, showRedDot = true),
)
```

红点直径 6dp(`coui_dot_diameter_small`),填充 `coui_color_additional_red`(浅色 `#ffeb3b2f` / 深色 `#ffeb493d`),外圈为白色描边,置于其前一个内容之后 2dp(`coui_chip_red_dot_offset_horizontal`),并从胶囊顶部下移 6dp(`coui_chip_red_dot_offset_vertical`)。红点显示期间,页签尾部内边距由 12dp 收缩为 8dp(`coui_chip_tab_end_padding_with_red_dot`)。计数为 13sp(`couiTextBodyS`),颜色 `couiColorLabelSecondary`,位于标签之后 2dp(`coui_chip_tab_count_start_margin`)。

## 组件状态

### 禁用状态

禁用的页签保持其填充,仅把标签与图标切换为 `couiColorLabelTertiary`,且不再响应按压:

```kotlin
ChipTabData(label = "Unavailable", enabled = false)
```

## 单独使用 ChipTab

`ChipTab` 是组所渲染的叶子节点。当需要逐个页签控制时,可直接在自己的布局中使用它——把 `selected` 与 `onClick` 绑定到组上的是组本身:

```kotlin
ChipTab(
    label = "Photos",
    selected = true,
    onClick = { },
    icon = COUIIcons.Photos,
)
```

## 属性

### ChipTabGroup

| 属性             | 类型                    | 说明                     | 默认值   | 必需 |
| ---------------- | ----------------------- | ------------------------ | -------- | ---- |
| tabs             | List&lt;ChipTabData&gt; | 该组的页签,按顺序排列    | -        | 是   |
| selectedIndex    | Int                     | 选中页签的下标           | -        | 是   |
| onSelectedChange | (Int) -> Unit           | 用户选中某个页签时的回调 | -        | 是   |
| modifier         | Modifier                | 应用于该组的修饰符       | Modifier | 否   |
| leadingIcon      | ImageVector?            | 页签前仅图标 chip 的图标 | null     | 否   |
| onLeadingIconClick | () -> Unit            | 点击仅图标 chip 时的回调 | {}       | 否   |
| contentPadding   | PaddingValues           | 页签四周的内边距,位于滚动区域内 | PaddingValues() | 否 |
| backdrop         | Backdrop?               | 胶囊采样的页面背景;为 null 时走平涂 | null | 否 |

### ChipTab

| 属性       | 类型         | 说明                    | 默认值   | 必需 |
| ---------- | ------------ | ----------------------- | -------- | ---- |
| label      | String       | 页签标签(单行,超长省略) | -        | 是   |
| selected   | Boolean      | 是否选中该页签          | -        | 是   |
| onClick    | () -> Unit   | 点击该页签时的回调      | -        | 是   |
| modifier   | Modifier     | 应用于该页签的修饰符    | Modifier | 否   |
| icon       | ImageVector? | 可选 16dp 前置图标      | null     | 否   |
| enabled    | Boolean      | 是否启用该页签          | true     | 否   |
| showRedDot | Boolean      | 该页签是否带红点        | false    | 否   |
| backdrop   | Backdrop?    | 胶囊采样的页面背景      | null     | 否   |
| count      | Int?         | 标签之后可选绘制的计数  | null     | 否   |

### ChipTabData

| 属性       | 类型         | 说明                   | 默认值 | 必需 |
| ---------- | ------------ | ---------------------- | ------ | ---- |
| label      | String       | 页签标签               | -      | 是   |
| icon       | ImageVector? | 可选 16dp 前置图标     | null   | 否   |
| enabled    | Boolean      | 该页签是否可选中       | true   | 否   |
| showRedDot | Boolean      | 该页签是否带红点       | false  | 否   |
| count      | Int?         | 标签之后可选绘制的计数 | null   | 否   |

### ChipTabDefaults

| 常量                   | 类型                    | 默认值                    | COUI 来源                                       |
| ---------------------- | ----------------------- | ------------------------- | ----------------------------------------------- |
| Height                 | Dp                      | 32.dp                     | coui_chip_tab_style_height                      |
| MinWidth               | Dp                      | 52.dp                     | coui_chip_default_min_width                     |
| MaxWidth               | Dp                      | 300.dp                    | coui_chip_default_max_width                     |
| TextMaxWidth           | Dp                      | 200.dp                    | coui_chip_default_max_text_width                |
| CornerRadius           | Dp                      | 16.dp                     | chipCornerRadius 16777215dp → min(w, h) / 2     |
| IconSize               | Dp                      | 16.dp                     | coui_chip_tab_style_chip_icon_size              |
| IconEndPadding         | Dp                      | 4.dp                      | coui_chip_suggest_style_chip_icon_end_padding   |
| StartPadding           | Dp                      | 12.dp                     | coui_chip_suggest_style_chip_horizontal_padding |
| EndPadding             | Dp                      | 12.dp                     | coui_chip_suggest_style_chip_horizontal_padding |
| EndPaddingWithRedDot   | Dp                      | 8.dp                      | coui_chip_tab_end_padding_with_red_dot          |
| CountStartMargin       | Dp                      | 2.dp                      | coui_chip_tab_count_start_margin                |
| RedDotDiameter         | Dp                      | 6.dp                      | coui_dot_diameter_small                         |
| RedDotOffsetHorizontal | Dp                      | 2.dp                      | coui_chip_red_dot_offset_horizontal             |
| RedDotOffsetVertical   | Dp                      | 6.dp                      | coui_chip_red_dot_offset_vertical               |
| GroupSpacing           | Dp                      | 8.dp                      | coui_chip_group_default_spacing_horizontal      |
| FadeWidth              | Dp                      | 12.dp                     | ChipMaskHorizontalScrollView.FADING_EDGE_WIDTH  |
| IconOnlyIconSize       | Dp                      | 20.dp                     | COUIChipTabLayout.ICON_SIZE_20                  |
| IconOnlyPadding        | Dp                      | 6.dp                      | COUIChipTabLayout.PADDING_HORIZONTAL_6          |
| CheckedShadow          | Shadow                  | radius 21.82.dp, alpha 0.0865, offset (0, 6.2.dp) | ShadowUtils 6 级,按 ColorOS 17 便签实测拟合 |
| ElevationOutset        | Dp                      | 46.dp                     | 组允许页签投影伸出的范围                        |
| PressOutset            | Dp                      | 2.dp                      | 滚动容器为按压回弹在页签上下多留的范围          |
| MinVisibleChange       | Float                   | 0.00390625f               | COUIChipTabDrawable.ALPHA_MIN_VISIBLE_CHANGE    |
| LayerSpring            | SpringSpec&lt;Float&gt; | response 0.2 / bounce 0   | COUIChipTabDrawable 的图层动画                  |
| ScrollSpring           | SpringSpec&lt;Float&gt; | response 0.4 / bounce 0.2 | COUIChipTabGroup.SCROLL_ANIMATION_*             |

#### 方法

| 方法名           | 类型      | 说明                                                               |
| ---------------- | --------- | ------------------------------------------------------------------ |
| textStyle()      | TextStyle | 页签标签样式(COUITheme.textStyles.body2,COUI couiTextButtonM 14sp) |
| countTextStyle() | TextStyle | 计数样式(COUITheme.textStyles.footnote1,COUI couiTextBodyS 13sp)   |

### 颜色

| COUI token                                                              | 取值                  | 用途                   | 移植目标                                |
| ----------------------------------------------------------------------- | --------------------- | ---------------------- | --------------------------------------- |
| attr/couiColorBackgroundElevated → color/coui_color_background_elevated | #ffffffff / #ff1e1e1e | 选中胶囊填充           | #F5F6F7(浅色) / #4B4B4B(深色),实测 |
| attr/couiColorContainer4 → color/coui_color_container4                  | #0a000000 / #1affffff | 未选中胶囊填充         | 文件内常量,主题无对应槽位               |
| attr/couiColorLabelPrimary → color/coui_color_label_primary             | #e6000000 / #e6ffffff | 两种状态下的标签与图标 | onSurface                               |
| attr/couiColorLabelTertiary → color/coui_color_label_tertiary           | #42000000 / #4dffffff | 禁用状态的标签与图标   | onSurfaceVariantActions                 |
| attr/couiColorLabelSecondary → color/coui_color_label_secondary         | #8a000000 / #8affffff | 计数文本               | onSurfaceSecondary                      |
| color/coui_color_additional_red                                         | #ffeb3b2f / #ffeb493d | 红点填充               | Badge 的 couiColorAdditionalRed         |
| color/coui_color_white                                                  | #ffffffff             | 红点外圈,1dp           | Badge 的白色描边(coui_dot_stroke_width) |
| color/coui_chip_tab_surface_unchecked_surface                           | #ffffffff / #ff000000 | SDK ≤ 36 的未选中填充  | 未移植,见下文                           |

`attr/couiColorBackgroundWithCard` → `color/coui_color_background_with_card`(`#fff0f1f2` / `#ffffffff`)由 `COUIChipTabDrawable.setUncheckedBackgroundColorFromTheme` 与 `couiColorContainer4` 合成,作为胶囊的*基座*填充;但页签自己的 drawable 会用上述双层结构覆盖背景且从不读取它,因此这里不使用。

### 文本

| 项目     | 取值                                   | 来源                                               |
| -------- | -------------------------------------- | -------------------------------------------------- |
| 标签     | couiTextButtonM,14sp,sans-serif-medium | Widget.COUI.Chip.ChipTab android:textAppearance    |
| 省略     | END,单行,居中                          | Widget.COUI.Chip android:ellipsize / maxLines      |
| 标签宽度 | 最大 200dp                             | coui_chip_default_max_text_width(chipTextMaxWidth) |
| 计数     | couiTextBodyS,13sp                     | Widget.COUI.Chip.ChipTab countTextAppearance       |

## 行为

- 传入 `backdrop` 时,胶囊改为便签那套磨砂材质:用 `Modifier.textureBlur` 取背景的模糊副本,再叠 `GlassChipDefaults.blurColors` 那对混合色;选中层只额外铺一层 `highlightWash` 受光,不再画不透明的选中面,所以选中胶囊依然是玻璃而不是色块。不传 `backdrop`(或模糊被关闭)时保持原来的平涂双层填充。
- 胶囊填充即 `COUIChipTabDrawable.drawDualLayerBackground` 与 `drawLightOSCheckedBackground` 的双层结构:选中面随选中层 alpha 淡出——浅色为 `#F5F6F7`,深色为 `#4B4B4B`——`couiColorContainer4` 以未选中层 alpha 覆盖其上。两个取值都是便签应用胶囊主体的实测值。
- 选中胶囊的玻璃高光绘制在轮廓之内、填充与文字之间:一道 0.8dp 的描边沿胶囊轮廓行走,白色的 alpha 随纵向位置变化——顶边 100%,向内 34px 处衰减到 0,再在离底边 46px 的范围内回到 100%(深色为 0.55dp,顶边 27% 白、底边 21% 白,衰减距离 33px / 69px)。浅色另外在底边补 4px 纯白加 4px 渐升、从左边缘向内 60px 由 242 回到填充色的内阴影、以及右侧对称的 247 微亮。这些数值取自便签应用在 620dpi 上的真机实测,等价于它的 `TYPE_CONTENT_CHIP_TAB_SELECTED` 玻璃高光与 `TYPE_FRAMEWORK_CAPSULE_2` 描边,但不采样背景。
- 选中与取消选中会让两个图层在临界阻尼弹簧上交叉渐变(response 0.2 / bounce 0,`COUIChipTabDrawable.CheckedLayerAlphaAnimation`),在 1/256 的最小可见变化处截断。文本与图标颜色不随选中状态变化:两种状态都用 `couiColorLabelPrimary`。
- 选中某个页签时,若它的某条边落在可见矩形内,就把该页签滚动到可见位置;可见矩形在被滚出的一侧内缩 12dp 的渐隐宽度(`COUIChipTabGroup.checkOcclusion`),滚动位置用 response 0.4 / bounce 0.2 的弹簧动效(`SCROLL_ANIMATION_RESPONSE` / `SCROLL_ANIMATION_BOUNCE`)。首次组合时不滚动,与原生页签点击监听一致。
- 渐隐遮罩是只在真正被裁剪的边上绘制的 `BlendMode.DstOut` 横向渐变,对应 `ChipMaskHorizontalScrollView.dispatchDraw`。
- 按下时页签沿按压弹簧缩小,松手时越过静止尺寸回弹;渐隐层的离屏层与滚动容器都会按自身高度裁剪,因此两者上下各多留 `ChipTabDefaults.PressOutset` 再整体上移,行本身仍只报告页签高度。原生则是把父容器的裁剪外扩 100dp(`COUIChipTab.updateClipExpandForShadowOnParents`)。

## 未移植

- `COUIChipTab.applyMaterialEffect` 的 spot-light 路径,它由 `COUIMaterialStateManager` 的菜单特性开关与主题布尔量控制。模糊那一半改由调用方传入 `backdrop` 触发:胶囊位于滚动器的离屏渐隐层内,采样页面背景有让渲染线程节点树自我引用的风险,所以默认不采样,只在调用方明确给出背景时启用。
- `COUIChipTabLayout` 的纯图标前置胶囊(32dp 圆形胶囊,内含 20dp 图标、前后各 6dp 内边距),以及该布局的 16dp 滚动区留白。
- `COUIChipTabGroupFragmentMediator`,即 fragment 切换与淡入辅助类。
- `coui_chip_tab_unit` 与 `coui_chip_tab_position_in_group` 两个字符串;报告证明没有任何 chip 类引用它们。
- `drawStateEffects` 的按压遮罩(`COUIMaskEffectDrawable` / `COUIStrokeDrawable`),其几何无法还原。
- `COUIChipTabDrawable.updateCachedThemeColors` 中 `Build.VERSION.SDK_INT <= 36` 的分支,该分支下未选中面为不透明的 `coui_chip_tab_surface_unchecked_surface`(`#ffffffff` / `#ff000000`)。本库面向 ColorOS 17 / API 37 的行为:该面为透明,未选中页签仅由 `couiColorContainer4` 填充。
