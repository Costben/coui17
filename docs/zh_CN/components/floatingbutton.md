# FloatingButton

ColorOS 17 的快速拨号:一个圆形主按钮,展开后弹出一列带标签的子按钮。对应 `com.coui.appcompat.floatingactionbutton.COUIFloatingButton` 及其 `COUIFloatingButtonItem` 行(`COUIFloatingButtonLabel`,由 `layout/coui_floating_button_item_label` 填充)。

`FloatingActionButton` 是单个 COUI 浮动按钮,本组件是建立在它之上的容器,因此主按钮与子按钮都保留该组件的按压反馈、水波纹与语义角色。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.FloatingButton
import io.github.suqi8.coui.kmp.basic.FloatingButtonDefaults
import io.github.suqi8.coui.kmp.basic.FloatingButtonItem
```

## 基本用法

```kotlin
var expanded by remember { mutableStateOf(false) }
val items = remember {
    listOf(
        FloatingButtonItem(icon = COUIIcons.AddFolder, label = "New folder"),
        FloatingButtonItem(icon = COUIIcons.Favorites, label = "Add to favorites"),
        FloatingButtonItem(icon = COUIIcons.Tune, label = "Settings"),
    )
}

Box(
    modifier = Modifier.fillMaxWidth().height(320.dp).padding(24.dp),
    contentAlignment = Alignment.BottomEnd,
) {
    FloatingButton(
        items = items,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        onItemClick = { false },
        mainIcon = COUIIcons.Add,
    )
}
```

`items.first()` 是离主按钮最近的一行,最后一项最先展开。展开时拨号宽度等于最宽的一行,因此请把它靠在一个 `Box` 的末端,以保持主按钮位置不变。收起后的拨号高度只有一个按钮:原生在 `visibilitySetup` 中把所有 `COUIFloatingButtonLabel` 置为 `View.GONE`,本移植在关闭动画结束后把各行移出组合。

## 保持菜单不关闭

`onItemClick` 返回 `Boolean`,与 `OnActionSelectedListener.onActionSelected` 一致:返回 `false` 关闭菜单,返回 `true` 保持打开。

```kotlin
FloatingButton(
    items = items,
    expanded = expanded,
    onExpandedChange = { expanded = it },
    onItemClick = { item -> item.label == "Settings" },
    mainIcon = COUIIcons.Add,
)
```

## 属性

### FloatingButton

| 属性             | 类型                            | 说明                                     | 默认值                          | 必需 |
| ---------------- | ------------------------------- | ---------------------------------------- | ------------------------------- | ---- |
| items            | List&lt;FloatingButtonItem&gt;  | 动作列表,第一项离主按钮最近              | -                               | 是   |
| expanded         | Boolean                         | 菜单是否展开                             | -                               | 是   |
| onExpandedChange | (Boolean) -> Unit               | 点击主按钮时返回新状态的回调             | -                               | 是   |
| onItemClick      | (FloatingButtonItem) -> Boolean | 点击动作时的回调;返回 `true` 保持菜单打开 | -                               | 是   |
| mainIcon         | ImageVector                     | 主按钮图标                               | -                               | 是   |
| modifier         | Modifier                        | 应用于拨号的修饰符                       | Modifier                        | 否   |
| enabled          | Boolean                         | 拨号是否响应点击                         | true                            | 否   |
| colors           | FloatingButtonColors            | 按钮与标签的配色                         | FloatingButtonDefaults.colors() | 否   |

原生从 `attr/mainFloatingButtonSrc` 读取主按钮图标,该属性由调用方提供,因此 `mainIcon` 没有默认值。Gallery 传入的是 `@drawable/base_menu_ic_add_sliding_up`(32dp 加号);`coui-icons` 模块的 `COUIIcons.Add` 是等价的图标,而该模块并非 `coui-ui` 的依赖。

### FloatingButtonItem

| 属性    | 类型        | 说明                            | 默认值 | 必需 |
| ------- | ----------- | ------------------------------- | ------ | ---- |
| icon    | ImageVector | 子按钮图标                      | -      | 是   |
| label   | String      | 标签;空字符串则不绘制标签        | -      | 是   |
| enabled | Boolean     | 该动作是否可点击                | true   | 否   |

### FloatingButtonColors

| 属性                   | 类型  | 说明                     |
| ---------------------- | ----- | ------------------------ |
| containerColor         | Color | 主按钮与子按钮的填充     |
| contentColor           | Color | 绘制在该填充上的图标     |
| disabledContainerColor | Color | 禁用按钮的填充           |
| labelContainerColor    | Color | 标签底板                 |
| labelTextColor         | Color | 标签文字                 |
| labelDisabledTextColor | Color | 禁用状态的标签文字       |

### FloatingButtonDefaults

| 常量                | 类型                     | 默认值                          | COUI 来源                                            |
| ------------------- | ------------------------ | ------------------------------- | ---------------------------------------------------- |
| ItemSize            | Dp                       | 56.dp                           | coui_floating_button_size / coui_floating_button_fab_normal_size |
| IconSize            | Dp                       | 24.dp                           | 转储中没有该 dimen;图标由调用方提供,为 56dp 按钮上的 24dp 矢量盒 |
| FirstItemSpacing    | Dp                       | 32.dp                           | coui_floating_button_item_first_bottom_margin        |
| ItemSpacing         | Dp                       | 16.dp                           | coui_floating_button_item_normal_bottom_margin       |
| LabelSpacing        | Dp                       | 12.dp                           | coui_floating_button_fab_side_margin                 |
| LabelCornerRadius   | Dp                       | 4.dp                            | attr/couiRoundCornerXS → coui_round_corner_xs        |
| LabelTextHorizontalPadding | Dp                | 8.dp                            | coui_floating_button_item_label_label_text_margin_start/end |
| LabelTextVerticalPadding   | Dp                | 4.dp                            | coui_floating_button_item_label_label_text_margin_top/bottom |
| LabelMaxWidth       | Dp                       | 220.dp                          | coui_floating_button_item_label_label_text_max_width |
| LABEL_MAX_LINES     | Int                      | 2                               | integer/coui_floating_button_item_label_label_text_max_line |
| EXPAND_ICON_ROTATION | Float                   | 45f                             | COUIFloatingButton.DEFAULT_ROTATE_ANGLE              |
| ROTATE_DURATION     | Int                      | 250                             | COUIFloatingButton.DEFAULT_ROTATE_ANIMATION_DURATION |
| ROTATE_DURATION_NO_ITEM | Int                  | 300                             | COUIFloatingButton.DEFAULT_ROTATE_ANIMATION_DURATION_NO_ITEM |
| OpenElevation       | Dp                       | 8.dp                            | coui_floating_button_open_elevation                  |
| CloseElevation      | Dp                       | 5.dp                            | coui_floating_button_close_elevation                 |
| ExpandStagger       | Duration                 | 50.milliseconds                 | COUIFloatingButton.ACTION_ANIM_DELAY                 |
| MIN_SCALE           | Float                    | 0.6f                            | COUIFloatingButton.DEFAULT_SCALE_ANIMATION_MIN_VALUE |
| ANIMATION_DURATION  | Int                      | 300                             | DEFAULT_MAIN_FLOATING_BUTTON_ANIMATION_DURATION      |
| MoveEaseIn          | Easing                   | cubic-bezier(0.25, 0.1, 0.25, 1) | mExpandMenuAnimationInterpolator / mLabelPathInterpolator |
| MoveEase            | Easing                   | cubic-bezier(0.3, 0, 0.1, 1)    | COUIMoveEaseInterpolator,anim/coui_ease              |
| ExpandSpec          | FiniteAnimationSpec&lt;Float&gt; | 300ms MoveEaseIn        | animationFloatingButtonMenuExpand 的 animatorSet     |
| CollapseSpec        | FiniteAnimationSpec&lt;Float&gt; | 300ms MoveEase          | closeFloatingButtonMenu(false, 300)                  |
| SlideSpring         | FiniteAnimationSpec&lt;Float&gt; | stiffness 500 / damping 0.8 | animationFloatingButtonMenuExpand 的 SpringAnimation |

#### 方法

| 方法名                   | 类型      | 说明                                                                          |
| ------------------------ | --------- | ----------------------------------------------------------------------------- |
| itemBottomOffset(index)  | Dp        | 容器底部到该行底边的距离——`getTotalLabelHeight`,即 `(index * 72) + 88` dp      |
| labelTextStyle()         | TextStyle | 标签样式(COUITheme.textStyles.footnote1,原生 coui_floating_button_item_label_label_text_size 12sp) |

### 颜色

| COUI token                                                                     | 取值                  | 用途                          | 移植                        |
| ------------------------------------------------------------------------------ | --------------------- | ----------------------------- | --------------------------- |
| attr/couiColorPrimary → color/coui_floating_button_main_button_color_selector   | 主题主色              | 启用状态按钮填充              | primary                     |
| attr/couiColorPressBackground → main_button_color_selector state_enabled=false  | #14000000 / #26ffffff | 禁用按钮填充                  | 文件常量,主题无对应槽位     |
| attr/couiColorOnPrimary                                                        | #ffffffff             | 图标                          | onPrimary                   |
| color/coui_floating_button_label_broader_color                                 | #1a000000 / #1affffff | 标签底板填充与描边            | 文件常量,主题无对应槽位     |
| color/coui_floating_button_label_text_color_enable                            | #ffffffff             | 标签文字                      | 文件常量,主题无对应槽位     |
| color/coui_floating_button_label_text_color_disable                           | #26000000 / #26ffffff | 禁用标签文字、禁用的主按钮图标 | 文件常量,主题无对应槽位     |
| color/coui_button_neutral_color                                                | #0f000000 / #4cffffff | `FloatingButton.Icon.Secondary` 填充 | 未移植,见下文        |

标签底板的描边使用同一个 `coui_floating_button_label_broader_color`,宽度为 `coui_floating_button_item_stroke_width`(0dp),因此不可见。`coui_floating_button_item_label_background_radius`(6dp)存在,但 `coui_floating_button_item_label` 实际把 `cardCornerRadius` 解析为 `?attr/couiRoundCornerXS`(4dp)。

### 文字

| 项目     | 取值             | 来源                                                |
| -------- | ---------------- | --------------------------------------------------- |
| 标签     | 12sp,最多 2 行   | coui_floating_button_item_label_label_text_size      |
| 省略方式 | END              | layout/coui_floating_button_item_label 的 TextView   |
| 标签宽度 | 最大 220dp       | coui_floating_button_item_label_label_text_max_width |

## 行为

- 展开时,一行的底边位于容器底部之上 `getTotalLabelHeight(index)` = `(index * 72) + 88` dp 处:56dp 行高、32dp 的 `coui_floating_button_item_first_bottom_margin`,再加上 `index` 个 56dp + 16dp。各行是底部对齐的浮层,而不是布局中的堆叠。
- 展开时每行从 `0.6f` 到 `1f` 按 cubic-bezier(0.25, 0.1, 0.25, 1)缩放并淡入,历时 300ms;同时 stiffness 500、damping ratio 0.8 的 `SpringAnimation(TRANSLATION_Y, 0f)` 把它从该偏移滑上来。动画位于 `animationFloatingButtonMenuExpand` 内的 `animatorSet` 中,因此原生对位移用弹簧、对缩放与透明度用插值器。
- 各行按 `ACTION_ANIM_DELAY`(50ms)错开。展开时离主按钮*最远*的一行最先动——`visibilitySetup` 让 `i3` 从 0 递增并动画标签 `(size - 1) - i3`;收起时最近的一行最先动(`i5 * 50`)。
- 两段动画期间都会清空动作监听器(首/末标签的 `onAnimationStart` 调用 `setOnActionSelectedListener(null)`,`onAnimationEnd` 恢复),因此只有在菜单稳定后各行才响应点击。
- 菜单展开时主按钮图标旋转 45 度,历时 `DEFAULT_ROTATE_ANIMATION_DURATION`(250ms),使用 `mRotateForwardInterpolator` / `mRotateBackwardInterpolator`;没有可展开项时为 300ms。
- 主按钮阴影展开时 8dp(`coui_floating_button_open_elevation`),收起时 5dp(`coui_floating_button_close_elevation`);子按钮不带阴影(`style/FloatingButton.Icon` 设置 `fabNeedElevation=false`)。

## 未移植

- `COUIFloatingButtonSeamlessImpl` 与 `COUIFloatingButtonTouchAnimation` 这两条 seamless 材质过渡,以及 `setBlurEffectEnable` 的 `COUIMaterialBlurEffect` 分支。
- `COUIMaskEffectDrawable`(`mMaskDrawable`,`onTouchEvent` 与 `animatePress` 会用到)以及 `COUIFABShadowUtils.setOuterShadowEffect` 的外发光阴影。
- `COUIFloatingButton$COUIFloatingButtonBehavior`、`$ScrollViewBehavior`、按窗口高度裁剪行的 `removeFloatingButtonItemWithWindowHeight`,以及滚动 5 秒后触发的 `AutoDismissRunnable`。
- `fabTranslateEnhancementRatio` / `fabAlignmentMode` / `fabCradleMargin` 这组属性,`FloatingButton.Icon.Medium.Secondary` / `.Translate` 会设置它们,但没有任何已移植路径读取。因此 `FloatingButton.Icon.Medium.Primary` / `.Secondary` / `.Tiny` / `.Small` 在这里都绘制同一个 56dp 按钮。
- 原生按方向切换的纵向轴心(收起时标签底边、展开时顶边);本移植保持末端轴心并纵向居中。
- `COUIFloatingButton.getDisabledColor` 的禁用态混色(`ColorUtils.compositeColors(couiColorDisable, coui_floating_button_disabled_base_color)`),它需要运行时的 `couiColorDisable` 属性;本移植改用状态列表自带的禁用项 `?attr/couiColorPressBackground`。
- `animatePress` 的 `performHapticFeedback()`。
- 行移动期间的点击穿透。移动中的行在本移植与原生中都不响应,但 Compose 的行仍会消费收到的点击,因此落在 300ms 展开/收起动画期间的点击会被吞掉,而不会传给下方的主按钮。
- `FloatingActionButton` 没有 `enabled` 参数,因此这里的禁用按钮是靠颜色与被拦截的点击实现的,而不是靠不可交互的表面。
