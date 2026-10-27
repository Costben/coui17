# DialogButtonBar

`DialogButtonBar` 是 COUI 警告对话框的按钮栏，对标 ColorOS 的 `COUIButtonBarLayout`。它最核心的行为是**自动纵排**：按钮栏会测量自己的文案，一旦某个文案宽到放不进单元格，整栏就从横排翻成纵排，而不是把文字挤压或截断。

横排与纵排不只是方向不同——两档的最小高度、内边距、分割线粗细与内缩都不一样，而且按钮顺序**完全相反**。

## 导入

```kotlin
import io.github.suqi8.coui.kmp.layout.DialogButtonBar
import io.github.suqi8.coui.kmp.layout.DialogButtonBarAction
import io.github.suqi8.coui.kmp.layout.DialogButtonBarDefaults
```

## 基础用法

按角色而非位置传入动作——由按钮栏决定每个动作摆在哪：

```kotlin
OverlayDialog(
    show = showDialog,
    title = "Dialog Title",
    summary = "A dialog with a COUI button bar.",
    onDismissRequest = { showDialog = false },
) {
    DialogButtonBar(
        negative = DialogButtonBarAction(text = "Cancel", onClick = { showDialog = false }),
        positive = DialogButtonBarAction(text = "Confirm", onClick = { showDialog = false }),
    )
}
```

两个短文案会并排显示。长文案会自动把整栏翻成纵排，无需任何额外配置：

```kotlin
DialogButtonBar(
    negative = DialogButtonBarAction(text = "Not Now", onClick = { showDialog = false }),
    positive = DialogButtonBarAction(
        text = "Delete Backup And Local Copies",
        onClick = { showDialog = false },
    ),
)
```

也支持第三个中性动作。COUI 对三按钮栏**一律**纵排，无论文案多短：

```kotlin
DialogButtonBar(
    negative = DialogButtonBarAction(text = "Cancel", onClick = { showDialog = false }),
    positive = DialogButtonBarAction(text = "Save", onClick = { showDialog = false }),
    neutral = DialogButtonBarAction(text = "Discard", onClick = { showDialog = false }),
)
```

## 纵排判定规则

按钮栏严格复刻 `COUIButtonBarLayout.onMeasure`。**只有同时满足以下两条**才保持横排：

1. 所有文案都放得下，且
2. 恰好有两个按钮。

其余任何情况都纵排。特别地，三按钮栏与单按钮栏都走纵排路径。推荐按钮**不参与**这个判定：`onMeasure` 读的是 `mDynamicLayout && (getButtonCount() != 2 || needSetButVertical(...))`，`mRecommendButtonId` 只影响单按钮纵排栏的顶边距。

单元格宽度来自 `needSetButVertical`：栏宽（先夹到 `DialogDefaults.MaxWidth`，即 `coui_dialog_max_width` 392dp）减去两侧外边距和格间间距后按按钮数均分，每格再减去自己的两侧内边距：

```
cellWidth = (min(392.dp, barWidth) - ButtonBarPaddingHorizontal * 2 - ButtonBarGap * (buttonCount - 1)) / buttonCount
available = cellWidth - ButtonHorizontalPadding * 2
```

任一文案宽于 `available` 即触发翻转。文案用 `TextMeasurer` 按解析后的 `textStyle` 单行测量，对应 COUI 的原始 paint 测量。

::: tip
横排两档的单元格是等权重的，间距来自两个单元格各自的 4dp 内侧内边距（`coui_alert_dialog_button_parent_padding_horizontal_inside`），所以 `ButtonBarGap` 是 8dp，而不是某个单独的分割线宽度。ColorOS 17 的按钮栏没有任何分割线视图。
:::

传 `dynamicLayout = false` 可锁定横排，等同于 `COUIButtonBarLayout.setDynamicLayout(false)`。

## 顺序

按钮顺序在两档之间反转，对应 `COUIButtonBarLayout.resortButton`：

| 档位 | 顺序 |
| :--- | :--- |
| 横排 | negative → neutral → positive（从左到右） |
| 纵排 | neutral → positive → negative（从上到下） |

所以否定（取消）动作横排时在**最左**，纵排时在**最下**。

## 没有分割线

ColorOS 17 的对话框按钮栏不画分割线。`coui_center_alert_dialog_button_panel.xml` 里只有三个空的 `LinearLayout` 单元格，
`COUIButtonBarLayout.setShowDividerWhenHasItems` 是空方法，`coui_delete_alert_dialog_divider_height_*` 属于列表面板栏
（`coui_panel_view_layout.xml`）而不是对话框。两个胶囊之间的分隔只来自胶囊自身的空隙与面板底色。

`showDivider` 与 `dividerColor` 仅为源码兼容而保留，传任何值都不改变渲染。

## 每一层是怎么画的

按钮的观感由三层叠加而成，与 `COUIButton.draw()` / `onDraw()` 的顺序一致：

| 层 | 内容 | 实现 |
| :--- | :--- | :--- |
| 容器层 | 胶囊底色 | `DialogButtonBarDefaults.buttonFill()` = `0xAAFFFFFF`。COUI 在模糊窗口下**不画**自己的背景（`onDraw` 里 `if (!isBlurEffectEnabled() \|\| mIsDrawMaterialEffect)`），胶囊其实是平台模糊层 `TYPE_CONTENT_SECONDARY_BUTTON` 提亮后的面板：实测面板 229、胶囊 246，正是 67% 白叠在面板上的结果 |
| 光效层 | 描边高光 | `Modifier.materialShadowEdge`。非推荐按钮走 `MaterialStrokeParams.contentCapsule5()`（`TYPE_CONTENT_CAPSULE_5`，edgeAlpha 0.8），推荐按钮走 `ContentCapsule4`（edgeAlpha 0.3） |
| 交互层 | 按压反馈 | `pressScaleEnabled = true`，对应 `COUIAlertDialogBottomButtonSecondary` 的 `scaleEnable=true`；禁用时填充换成 `disabledButtonFill()`（`#14000000`） |

描边画在填充**之上**、文案**之下**——和 `COUIButton.onDraw()` 的 `drawButtonBackground` → `drawMaskAndStroke` → `super.onDraw` 一致。
这也是填充必须保持半透明的原因：底色若不透明，描边就不再是"从边缘透出来的光"。

## 尺寸

横排栏的每一处内缩都由 `COUIButtonBarLayout` 的字段推出，而不是逐值写死：

| 位置 | 值 | 来源 |
| :--- | :--- | :--- |
| 胶囊到面板侧边 | 16dp | `ButtonBarPaddingHorizontal`，`coui_alert_dialog_button_parent_padding_horizontal` |
| 胶囊之间 | 8dp | `ButtonBarGap`：两个单元格本身贴合，各自贡献 4dp `coui_alert_dialog_button_parent_padding_horizontal_inside` |
| 胶囊到上方内容 | 4dp | `ButtonBarPaddingTop`，`coui_alert_dialog_button_parent_padding_vertical` |
| 胶囊到面板底边 | 16dp | `ButtonBarPaddingBottom`，`coui_alert_dialog_button_parent_padding_bottom` |
| 胶囊高度 | 44dp | `ButtonHeight`，`coui_alert_dialog_button_height`；纵排折行时 56dp（`ButtonMultilineHeight`，`coui_alert_dialog_button_min_height_multiline`） |
| 胶囊圆角 | 高度的一半 | `DialogBarButton` 按即将绘制的高度取 `minHeight / 2`：44dp 档即 22dp，56dp 折行档即 28dp |

## 属性

### DialogButtonBar 属性

| 属性            | 类型                   | 说明                                             | 默认值                                      | 必需 |
| :-------------- | :--------------------- | :----------------------------------------------- | :------------------------------------------ | :--- |
| negative        | DialogButtonBarAction? | 否定（取消）动作，为 null 时隐藏                 | -                                           | 是   |
| positive        | DialogButtonBarAction? | 肯定（确认）动作，为 null 时隐藏                 | -                                           | 是   |
| modifier        | Modifier               | 应用于按钮栏的修饰符                             | Modifier                                    | 否   |
| neutral         | DialogButtonBarAction? | 中性（第三个）动作，为 null 时隐藏               | null                                        | 否   |
| dynamicLayout   | Boolean                | 是否允许自动纵排，false 则锁定横排               | true                                        | 否   |
| showDivider     | Boolean                | 仅为源码兼容保留，不画分割线                     | true                                        | 否   |
| hasContentAbove | Boolean                | 栏上方是否有标题、正文或自定义面板               | true                                        | 否   |
| colors          | TextButtonColors       | 非推荐按钮的配色                                 | ButtonDefaults.textButtonColorsBorderless() | 否   |
| recommendColors | TextButtonColors       | 标记 `recommended` 的按钮的配色                  | ButtonDefaults.textButtonColorsPrimary()    | 否   |
| dividerColor    | Color                  | 仅为源码兼容保留，不画分割线                     | COUITheme.colorScheme.dividerLine           | 否   |
| textStyle       | TextStyle              | 文案样式，同时用于纵排判定时测量文案             | COUITheme.textStyles.button                 | 否   |

::: warning
`hasContentAbove` 影响间距：栏上方什么都没有时，COUI 给最上面那个纵排按钮加 0dp 额外上内边距（`coui_bottom_alert_dialog_vertical_button_padding_top_extra_new`），给纵排栏加 0dp 额外上边距（`coui_bottom_alert_dialog_buttonbar_margintop`）。带标题或正文的普通对话框保持 `true` 即可。
:::

### DialogButtonBarAction 属性

| 属性        | 类型       | 说明                 | 默认值 | 必需 |
| :---------- | :--------- | :------------------- | :----- | :--- |
| text        | String     | 按钮文案             | -      | 是   |
| enabled     | Boolean    | 按钮是否启用         | true   | 否   |
| onClick     | () -> Unit | 点击按钮时的回调     | -      | 是   |
| recommended | Boolean    | 是否为该栏的推荐按钮 | false  | 否   |

### DialogButtonBarDefaults 对象

| 属性                     | 类型 | 说明                                             | 值        |
| :----------------------- | :--- | :----------------------------------------------- | :-------- |
| ButtonBarPaddingHorizontal | Dp | 胶囊到面板侧边的内边距                           | 16.dp     |
| ButtonBarPaddingTop        | Dp | 胶囊到上方内容的内边距                           | 4.dp      |
| ButtonBarPaddingBottom     | Dp | 胶囊到面板底边的内边距                           | 16.dp     |
| ButtonBarGap               | Dp | 两个胶囊之间的间距（横排与纵排同一值）           | 8.dp      |
| ButtonHeight               | Dp | 胶囊高度                                         | 44.dp     |
| ButtonMultilineHeight      | Dp | 文案折行时胶囊的最小高度                         | 56.dp     |
| ButtonInsideMargin         | PaddingValues | 胶囊内文案的留白                        | 12dp / 4dp |
| ButtonHorizontalPadding    | Dp | 按钮水平内边距，同时用于纵排判定                 | 12.dp     |
| StackedButtonPaddingTopExtra | Dp | 上方无内容时，最顶部纵排按钮的额外上内边距     | 0.dp      |
| StackedButtonPaddingBottomExtra | Dp | 最底部纵排按钮的额外下内边距                | 0.dp      |
| StackedBarMarginTop        | Dp | 含多个按钮的纵排栏的上外边距                     | 0.dp      |
| buttonFill()               | Color | 胶囊填充                                        | #AAFFFFFF |
| disabledButtonFill()       | Color | 禁用胶囊填充                                    | #14000000 |
| buttonColors()             | TextButtonColors | 普通胶囊的整套配色（填充 + 标签）      | 见上两行 + `onBackground` |
| DangerTextColor            | Color | 破坏性动作的标签色                              | #EA3447   |
| DangerDisabledTextColor    | Color | 破坏性动作禁用时的标签色                        | #66EA3447 |
| dangerButtonColors()       | TextButtonColors | 破坏性胶囊：同样的半透明填充 + 红标签  | 见上两行 |

### 破坏性（红色）按钮

ColorOS 17 里"删除"这类按钮**不是**一套独立样式：填充仍是普通半透明胶囊，只有标签变红。Settings 就是这么手写的 ——
`StyleDialogUtil` 与 `ScreenLockFeature` 里都有
`button.setTextColor(R.color.color_delete_alert_dialog_button_warning_color)`，该颜色是 `#ffea3447`，禁用态
`color_delete_alert_dialog_button_warning_color_disable` 是同一个红压到 40% alpha。

所以这里没有新增样式，只给了一个具名入口：

```kotlin
DialogButtonBar(
    negative = DialogButtonBarAction(text = "取消", onClick = onCancel),
    positive = DialogButtonBarAction(text = "删除车辆", onClick = onDelete),
    // 整栏都是破坏性动作时直接换配色；只有一个按钮危险时，用 recommendColors 或自建 TextButtonColors。
    colors = DialogButtonBarDefaults.dangerButtonColors(),
)
```

`OverlayAlertDialog` / `AlertDialogContentLayout` 也接受 `buttonColors` 与 `recommendColors`，所以对话框不必再降到更低一层。

::: tip
同一张资源表里还有两个红：`coui_color_label_theme_red` 与 `coui_color_primary_text_red`，都是 `#db382c`。它们是**主题强调红**，
用在列表项和偏好标题上，比删除对话框用的那个红暗一档，不要混用。
:::

面板侧的度量（最大宽度、圆角、内外边距、模糊半径）在 `DialogDefaults`：`MaxWidth` 392dp（`coui_dialog_max_width`）、
`CornerRadius` 38dp、`outsideMargin` 16dp/32dp、`insideMargin` 24dp、`ButtonBarMinHeight` 44dp、`NativeBlurRadiusPx` 200。

## 推荐按钮

把某个动作标成 `recommended = true`，该按钮就画成填充的主按钮，其余按钮仍走 `colors`：

```kotlin
DialogButtonBar(
    negative = DialogButtonBarAction(text = "稍后", onClick = { showDialog = false }),
    positive = DialogButtonBarAction(text = "安装", onClick = { showDialog = false }, recommended = true),
)
```

对齐 `COUIButtonBarLayout.setRecommendButtonId`：

- `getBlurWindowButtonType` 对 id 等于 `mRecommendButtonId` 的按钮返回 `ButtonType.PRIMARY`，其余返回 `SECONDARY`，所以按钮栏的填充正好沿 `recommended` 标记分界。
- `applyRecommendStyle` 把按钮切到 `ButtonType.PRIMARY`、用 `color/coui_btn_default_text_color` 重设文案色，并打开实心填充动画模式（`setAnimType(1)`；`COUIButton.java` 在 `mAnimType == 1` 时直接用 `mDrawableColor` 填充）。`textButtonColorsPrimary()` 覆盖填充与文案着色。
- 推荐按钮的光效换成 `TYPE_CONTENT_CAPSULE_4`（edgeAlpha 0.3）：主按钮本身已经很亮，描边要收得更淡。
- 单按钮纵排栏在该按钮是推荐按钮时仍会拿到 `StackedBarMarginTop`（`onMeasure` 判 `getButtonCount() == 1 && mRecommendButtonId != -1`）。

## 行为说明

- **按钮是胶囊，不是整格矩形。** `COUIAlertDialogBottomButtonSecondary` 的 `drawableRadius` 是 -1，`COUIButtonDrawable` 把它夹到高度的一半，
  所以圆角永远等于 `minHeight / 2`；本库由 `DialogBarButton` 在绘制前按实际高度算出同一个值。
- **面板底部留白由按钮栏承担。** `ButtonBarPaddingBottom` 的 16dp 就是面板圆角下方的空白，所以 `DialogContentLayout` 的面板不再额外加底部内边距。
- **纵排栏的上外边距是 0dp**，除非它是单按钮的推荐栏。
- **单独一个否定按钮回退到横排度量。** 这是纵排里唯一一种改用 44dp 面板最小高度与横排内边距、而非纵排那套的情况。
- **按下会缩放。** `scaleEnable=true`，配合整格的遮罩着色，这也是按钮栏传 `pressScaleEnabled = true` 的原因。

## 未实现部分

**AI 支付版推荐按钮**尚未实现。COUI 的 `setRecommendButtonType(AI_PAYMENT)` 会把推荐按钮送进另一条路径 —— `ButtonType.AI_PAYMENT`、`setDrawableRadius(-1)`、`couiColorAiPayment*Text` 系列文案色，以及在材质设备上由 `PopupGradientBackgroundHelper.drawRadialGradient` 绘制、并受 `COUIMaterialEffect` / `COUIMaterialStateManager` 特性开关控制的径向渐变背景。本库没有对应的材质特效能力，所以 `recommended = true` 始终画成普通的主按钮填充。
