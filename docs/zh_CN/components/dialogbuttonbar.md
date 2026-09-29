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

单元格宽度来自 `needSetButVertical`：栏宽（先夹到 `DialogDefaults.MaxWidth`，即 `coui_dialog_max_width` 392dp）减去分割线后按按钮数均分，每格再减去自己的两侧内边距：

```
available = (min(392.dp, barWidth) - (buttonCount - 1) * StackedDividerThickness) / buttonCount
            - ButtonHorizontalPadding * 2
```

任一文案宽于 `available` 即触发翻转。文案用 `TextMeasurer` 按解析后的 `textStyle` 单行测量，对应 COUI 的原始 paint 测量。

::: tip
该公式里减掉的是**纵排**档的 0.33dp 分割线厚度，即使这一栏最终画的是更粗的 1dp 横排分割线也一样。这是 `COUIButtonBarLayout` 的原始行为，此处如实复刻。
:::

传 `dynamicLayout = false` 可锁定横排，等同于 `COUIButtonBarLayout.setDynamicLayout(false)`。

## 顺序与分割线

按钮顺序在两档之间反转，对应 `resortButton`：

| 档位 | 顺序 |
| :--- | :--- |
| 横排 | negative → neutral → positive（从左到右） |
| 纵排 | neutral → positive → negative（从上到下） |

所以否定（取消）动作横排时在**最左**，纵排时在**最下**。

分割线也不同：

| 档位 | 粗细 | 内缩 |
| :--- | :--- | :--- |
| 横排 | 1dp（`DialogDefaults.ButtonBarDividerThickness`） | 上 17dp / 下 21dp |
| 纵排 | 0.33dp（`DialogButtonBarDefaults.StackedDividerThickness`） | 左右各 24dp |

相邻的两个存在按钮之间会画一条分割线。传 `showDivider = false` 可全部隐藏（对应 `buttonBarShowDivider` 属性）。

## 属性

### DialogButtonBar 属性

| 属性            | 类型                   | 说明                                             | 默认值                                      | 必需 |
| :-------------- | :--------------------- | :----------------------------------------------- | :------------------------------------------ | :--- |
| negative        | DialogButtonBarAction? | 否定（取消）动作，为 null 时隐藏                 | -                                           | 是   |
| positive        | DialogButtonBarAction? | 肯定（确认）动作，为 null 时隐藏                 | -                                           | 是   |
| modifier        | Modifier               | 应用于按钮栏的修饰符                             | Modifier                                    | 否   |
| neutral         | DialogButtonBarAction? | 中性（第三个）动作，为 null 时隐藏               | null                                        | 否   |
| dynamicLayout   | Boolean                | 是否允许自动纵排，false 则锁定横排               | true                                        | 否   |
| showDivider     | Boolean                | 是否显示按钮之间的分割线                         | true                                        | 否   |
| hasContentAbove | Boolean                | 栏上方是否有标题、正文或自定义面板               | true                                        | 否   |
| colors          | TextButtonColors       | 非推荐按钮的配色                                 | ButtonDefaults.textButtonColorsBorderless() | 否   |
| recommendColors | TextButtonColors       | 标记 `recommended` 的按钮的配色                  | ButtonDefaults.textButtonColorsPrimary()    | 否   |
| dividerColor    | Color                  | 分割线颜色                                       | COUITheme.colorScheme.dividerLine           | 否   |
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

纵排档的度量以及两档共用的值。横排档自身的度量在 `DialogDefaults.ButtonBar*`。

| 属性                            | 类型 | 说明                                             | 值      |
| :------------------------------ | :--- | :----------------------------------------------- | :------ |
| ButtonHorizontalPadding         | Dp   | 按钮水平内边距，两档通用                         | 12.dp   |
| StackedButtonMinHeight          | Dp   | 纵排按钮最小高度                                 | 44.dp   |
| StackedButtonMinHeightBottom    | Dp   | 最底部纵排按钮的最小高度（44dp + 0dp 额外）      | 44.dp   |
| StackedButtonPaddingVertical    | Dp   | 纵排按钮垂直内边距                               | 4.dp    |
| StackedButtonPaddingTopExtra    | Dp   | 上方无内容时，最顶部纵排按钮的额外上内边距       | 0.dp    |
| StackedButtonPaddingBottomExtra | Dp   | 最底部纵排按钮的额外下内边距（承载面板底部留白） | 0.dp    |
| StackedBarMarginTop             | Dp   | 含多个按钮、或单个推荐按钮的纵排栏的上外边距     | 0.dp    |
| StackedDividerThickness         | Dp   | 纵排按钮之间分割线的厚度                         | 0.33.dp |
| StackedDividerInsetHorizontal   | Dp   | 纵排按钮之间分割线的水平内缩                     | 24.dp   |
| ButtonCornerRadius              | Dp   | 按钮圆角（整格直角矩形）                         | 0.dp    |

横排档自身的度量同样出自设备资源表：

| 属性                      | 值         | COUI 令牌                                                |
| :------------------------ | :--------- | :------------------------------------------------------- |
| MaxWidth                  | 392.dp     | `coui_dialog_max_width`                                  |
| ButtonBarMinHeight        | 44.dp      | `coui_alert_dialog_button_height`                        |
| ButtonBarInsideMargin     | 12/12/12/22 | `coui_alert_dialog_button_horizontal_padding` + 栏自身上下留白 |
| ButtonBarDividerThickness | 1.dp       | `coui_delete_alert_dialog_divider_height_horizontalbutton` |
| ButtonBarDividerInsetTop  | 17.dp      | `coui_bottom_alert_dialog_horizontal_button_padding_top_extra_divider_new` |
| ButtonBarDividerInsetBottom | 21.dp    | `coui_bottom_alert_dialog_horizontal_button_padding_bottom_extra_divider_new` |

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
- 单按钮纵排栏在该按钮是推荐按钮时仍会拿到 `StackedBarMarginTop`（`onMeasure` 判 `getButtonCount() == 1 && mRecommendButtonId != -1`）。

## 行为说明

- **底部按钮承载面板留白。** 最底部的纵排按钮拿到 0dp 额外下内边距和 44dp 最小高度，所以对话框面板本身不需要再加底部内边距。
- **多按钮纵排栏有 0dp 上外边距。** 单个纵排按钮没有，除非它是推荐按钮。
- **单独一个否定按钮回退到横排度量。** 这是纵排里唯一一种 `resetVerButsPadding` 改用 44dp 面板最小高度与横排内边距、而非纵排那套的情况。
- **按钮是直角且无按压缩放。** `COUIAlertDialogBottomButtonNewNormal` 设了 `drawableRadius=0dp`、`scaleEnable=false`、`stateListAnimator=@null`：唯一的按压反馈是整格的着色。

## 未实现部分

**AI 支付版推荐按钮**尚未实现。COUI 的 `setRecommendButtonType(AI_PAYMENT)` 会把推荐按钮送进另一条路径 —— `ButtonType.AI_PAYMENT`、`setDrawableRadius(-1)`、`couiColorAiPayment*Text` 系列文案色，以及在材质设备上由 `PopupGradientBackgroundHelper.drawRadialGradient` 绘制、并受 `COUIMaterialEffect` / `COUIMaterialStateManager` 特性开关控制的径向渐变背景。本库没有对应的材质特效能力，所以 `recommended = true` 始终画成普通的主按钮填充。
