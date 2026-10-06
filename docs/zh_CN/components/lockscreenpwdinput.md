# LockScreenPwdInput

`LockScreenPwdInput` 是 ColorOS 17 的锁屏密码输入框。它是一个胶囊形密码框加 48dp 圆形下一步按钮：胶囊带描边、上下内阴影，按下下一步按钮时还有一道从按钮扫过整个输入框的光。

布局为 `layout/coui_input_lock_screen_pwd_layout.xml`，一个 `ConstraintLayout`，内含输入框（`0dp` 宽、参与链式约束）和 48dp 的 `ImageView` 下一步按钮；按钮的 `layout_marginStart` 为 `coui_input_lock_screen_pwd_icon_margin`（12dp），`src` 为 `coui_input_lock_screen_pwd_next_src`，一张 24dp 的右箭头。`COUILockScreenPwdInputView.addSmoothRoundRect` 以 `rectF.height() / 2` 作为两个圆角半径构建胶囊路径，因此圆角半径为 `LockScreenPwdInputDefaults.Height / 2`（24dp）。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.LockScreenPwdInput
import io.github.suqi8.coui.kmp.basic.LockScreenPwdInputColors
import io.github.suqi8.coui.kmp.basic.LockScreenPwdInputDefaults
```

## 基本用法

```kotlin
var password by remember { mutableStateOf("") }

LockScreenPwdInput(
    value = password,
    onValueChange = { password = it },
    label = "Password",
)
```

文本默认被遮蔽：原生把 `couiEnablePassword` 设为 true，本移植的 `visualTransformation` 默认即 `PasswordVisualTransformation()`。

## 提交密码

`onNextClick` 在下一步按钮松开时触发。`minCount`（原生的 `couiInputMinCount`）控制门槛：文本长度不足时按钮使用禁用箭头色并忽略指针，这正是 `changeNextTransparentImageResource` 的 `_allow` / 普通图切换。

```kotlin
LockScreenPwdInput(
    value = password,
    onValueChange = { password = it },
    label = "Password",
    minCount = 6,
    maxCount = 16,
    onNextClick = { submitted -> verify(submitted) },
)
```

`onValueChange` 会把文本截断到 `maxCount`（`couiInputMaxCount`），与原生 `TextWatcher` 调用 `editable.subSequence(0, mInputView.getMaxCount())` 一致。传入 `nextIcon = null` 可完全去掉按钮。

## 属性

| 属性名                 | 类型                      | 描述                                            | 默认值                              | 必填 |
| ---------------------- | ------------------------- | ----------------------------------------------- | ----------------------------------- | ---- |
| value                  | String                    | 密码文本                                        | -                                   | 是   |
| onValueChange          | (String) -> Unit          | 回调新文本，已截断到 `maxCount`                  | -                                   | 是   |
| modifier               | Modifier                  | 应用在整行上的修饰符；胶囊占据剩余宽度            | Modifier                            | 否   |
| label                  | String                    | 文本为空时显示的提示                            | ""                                  | 否   |
| enabled                | Boolean                   | 输入框与按钮是否响应输入                        | true                                | 否   |
| minCount               | Int                       | 下一步按钮生效前所需的字符数                     | LockScreenPwdInputDefaults.MinInputCount | 否 |
| maxCount               | Int                       | 硬性字符上限                                    | LockScreenPwdInputDefaults.MaxInputCount | 否 |
| onNextClick            | (String) -> Unit          | 下一步按钮松开时回调文本                        | {}                                  | 否   |
| nextIcon               | ImageVector?              | 按钮内的箭头；为 null 时隐藏按钮                 | LockScreenPwdInputDefaults.NextIcon | 否   |
| nextContentDescription | String                    | 下一步按钮的描述                                | LockScreenPwdInputDefaults.NextDescription | 否 |
| colors                 | LockScreenPwdInputColors  | 描边、阴影、光效与按钮颜色                      | LockScreenPwdInputDefaults.colors() | 否   |
| textStyle              | TextStyle                 | 文本样式，其颜色会被 `colors.textColor` 覆盖     | LockScreenPwdInputDefaults.textStyle() | 否 |
| keyboardOptions        | KeyboardOptions           | 输入框的键盘选项                                | KeyboardOptions.Default             | 否   |
| keyboardActions        | KeyboardActions           | 输入框的键盘动作                                | KeyboardActions.Default             | 否   |
| visualTransformation   | VisualTransformation      | 输入框的视觉变换                                | PasswordVisualTransformation()      | 否   |
| interactionSource      | MutableInteractionSource? | 输入框的交互源                                  | null                                | 否   |

### LockScreenPwdInputColors

| 属性名                | 描述                                                                                     |
| --------------------- | ---------------------------------------------------------------------------------------- |
| containerColor        | 胶囊背景，`coui_input_lock_screen_pwd_view_bg_color_desktop`（#33ffffff）                 |
| borderColor           | 胶囊描边，`coui_input_lock_screen_border_color`（#0dffffff）                              |
| upperInnerShadowColor | 上内边缘阴影，`..._upper_inner_shadow_color`（#5cffffff）                                 |
| lowerInnerShadowColor | 下内边缘阴影，`..._lower_inner_shadow_color`（#33ffffff）                                 |
| outerGradientColor1   | 光效外圈渐变，`..._outer_gradient_color_1`（#80ffffff）                                   |
| outerGradientColor2   | `..._outer_gradient_color_2`（#33ffffff）                                                 |
| outerGradientColor3   | `..._outer_gradient_color_3`（#0dffffff）                                                 |
| innerGradientColor1   | 光效内圈渐变，`..._inner_gradient_color_1`（#8fffffff）                                   |
| innerGradientColor2   | `..._inner_gradient_color_2`（#e6ffffff）                                                 |
| textColor             | 文本，`..._pwd_view_edittext_text_color_desktop`（#ffffffff）                             |
| nextBackgroundColor   | 箭头背后的圆形，字面写在 `..._pwd_next_desktop_bg` 中（#33ffffff）                        |
| nextIconColor         | 启用态箭头，`?couiColorPrimaryNeutral`（#e6000000 / #e6ffffff）                           |
| nextDisabledIconColor | 禁用态箭头，`coui_color_label_tertiary_dark`（#4dffffff）                                 |

### LockScreenPwdInputDefaults

| 常量                   | 类型       | 值       | COUI 来源                                      |
| ---------------------- | ---------- | -------- | ---------------------------------------------- |
| MinInputCount          | Int        | 6        | DEFAULT_MIN_INPUT_COUNT                        |
| MaxInputCount          | Int        | 16       | DEFAULT_MAX_INPUT_COUNT                        |
| Height                 | Dp         | 48.dp    | coui_input_lock_screen_pwd_height              |
| IconMargin             | Dp         | 12.dp    | coui_input_lock_screen_pwd_icon_margin         |
| DesktopWidth           | Dp         | 280.dp   | coui_input_lock_screen_pwd_desktop_width       |
| SettingWidth           | Dp         | 268.dp   | coui_input_lock_screen_pwd_setting_width       |
| Setting1Width          | Dp         | 220.dp   | coui_input_lock_screen_pwd_setting1_width      |
| CardPadding            | Dp         | 16.dp    | coui_input_lock_screen_pwd_card_padding        |
| CardMaxHeight          | Dp         | 63.dp    | coui_input_lock_screen_pwd_card_max_height     |
| EditMargin             | Dp         | 4.dp     | coui_input_lock_screen_pwd_edit_margin         |
| TitlePadding           | Dp         | 10.5.dp  | coui_input_lock_screen_pwd_title_padding_top / _bottom |
| BorderWidth            | Dp         | 0.6.dp   | coui_input_lock_screen_border_width，按 2 倍绘制 |
| LightShaderRadius      | Dp         | 90.dp    | coui_input_lock_screen_light_shader_radius     |
| NextIconSize           | Dp         | 24.dp    | coui_input_lock_screen_pwd_next_src 的尺寸      |
| NextDescription        | String     | "Next"   | string/coui_lock_screen_next_button_description |
| LightRadiusStartFraction | Float    | 0.6f     | mOuterLightRadiusStart = 0.6f * f2             |
| OuterGradientStops     | FloatArray | 0, 0.3, 0.6, 0.8, 1 | GRADIENT_OUTER_STOP_1..3             |
| InnerGradientStops     | FloatArray | 0, 0.7, 1 | GRADIENT_INNER_STOP_1                         |
| InnerShadowRadius1     | Float      | 32f      | INNER_SHADOW_RADIUS_1，原始像素                 |
| InnerShadowDy1         | Float      | -8f      | INNER_SHADOW_DY_1，原始像素                     |
| InnerShadowStrokeWidth1 | Float     | 20f      | INNER_SHADOW_STROKE_WIDTH_1，原始像素           |
| InnerShadowRadius2     | Float      | 8f       | INNER_SHADOW_RADIUS_2，原始像素                 |
| InnerShadowDy2         | Float      | 2f       | INNER_SHADOW_DY_2，原始像素                     |
| InnerShadowStrokeWidth2 | Float     | 12f      | INNER_SHADOW_STROKE_WIDTH_2，原始像素           |
| PressedScale           | Float      | 0.92f    | COUIPressFeedbackHelper 对 48dp 的终止比例      |
| NextIcon               | ImageVector | 24dp    | drawable/coui_input_lock_screen_pwd_next_src   |

描边以 `BorderWidth`（0.6dp）记录，但按两倍值（1.2dp）绘制，与原生 `mButtonBorderWidth` 的做法一致。`LightShaderRadius` 是光效在 `onSizeChanged` 重算前的半径；重算后为 `layoutWidth - inputWidth / 2 - nextIconWidth / 2`，即输入框中心到按钮中心的距离，本移植用同一表达式，由实测的胶囊宽度与图标边距推出。

#### 方法

| 方法名                 | 类型                      | 描述                                                       |
| ---------------------- | ------------------------- | ---------------------------------------------------------- |
| upperShadowReach()     | Float                     | 32 + 8 + 10 = 50px，上阴影在胶囊内延伸的距离               |
| lowerShadowReach()     | Float                     | 8 + 2 + 6 = 16px，下阴影同理                               |
| outerGradientStops(c)  | Array&lt;Pair&lt;Float, Color&gt;&gt; | 外圈径向渐变的五个停靠点，两端透明           |
| innerGradientStops(c)  | Array&lt;Pair&lt;Float, Color&gt;&gt; | 内圈径向渐变的三个停靠点                     |
| colors()               | LockScreenPwdInputColors  | 桌面场景配色，仅箭头分日 / 夜                              |
| textStyle()            | TextStyle                 | `TextFieldMode.None` 的 COUI 输入文本样式                  |
| textFieldColors(label) | TextFieldColors           | 内层输入框的颜色，仅设置 label 颜色                        |

### 颜色

| COUI token                                          | 值        | 用途                     | 本移植                       |
| --------------------------------------------------- | --------- | ------------------------ | ---------------------------- |
| color/coui_input_lock_screen_border_color           | #0dffffff | 胶囊描边                 | 文件常量，主题无对应槽位     |
| color/coui_input_lock_screen_upper_inner_shadow_color | #5cffffff | 上内阴影               | 文件常量，主题无对应槽位     |
| color/coui_input_lock_screen_lower_inner_shadow_color | #33ffffff | 下内阴影               | 文件常量，主题无对应槽位     |
| color/coui_input_lock_screen_outer_gradient_color_1 | #80ffffff | 外圈光效                 | 文件常量，主题无对应槽位     |
| color/coui_input_lock_screen_outer_gradient_color_2 | #33ffffff | 外圈光效                 | 文件常量，主题无对应槽位     |
| color/coui_input_lock_screen_outer_gradient_color_3 | #0dffffff | 外圈光效                 | 文件常量，主题无对应槽位     |
| color/coui_input_lock_screen_inner_gradient_color_1 | #8fffffff | 内圈光效                 | 文件常量，主题无对应槽位     |
| color/coui_input_lock_screen_inner_gradient_color_2 | #e6ffffff | 内圈光效                 | 文件常量，主题无对应槽位     |
| color/coui_input_lock_screen_pwd_view_bg_color_desktop | #33ffffff | 胶囊填充               | 文件常量，主题无对应槽位     |
| color/coui_input_lock_screen_pwd_view_edittext_text_color_desktop | #ffffffff | 文本      | 文件常量，主题无对应槽位     |
| color/coui_input_lock_screen_pwd_next_desktop_bg    | #33ffffff | 按钮圆形                 | 文件常量，主题无对应槽位     |
| attr/couiColorPrimaryNeutral                        | #e6000000 / #e6ffffff | 启用态箭头  | colors()，分日 / 夜          |
| color/coui_color_label_tertiary_dark                | #4dffffff | 禁用态箭头               | 文件常量，主题无对应槽位     |

`LockScreenPwdInputDefaults` 中对应的常量为 `BorderColor`、`UpperInnerShadowColor`、`LowerInnerShadowColor`、`OuterGradientColor1` / `2` / `3`、`InnerGradientColor1` / `2`、`ContainerColorDesktop`、`TextColorDesktop`、`NextBackgroundColor`、`NextDisabledIconColor` 以及 `NextIconColorLight` / `NextIconColorDark`。`CardBackgroundLight`（#ffffff）与 `CardBackgroundDark`（#1affffff）是设置场景模式的胶囊填充（`coui_color_card_background`），已还原但未使用，因为本移植是桌面形态。

原生以 `mScenesMode` 而非主题来选择桌面场景模式，因此胶囊、描边、阴影与光效都是固定值；只有 `_allow` 箭头指向的 `coui_color_primary_neutral` 分日 / 夜。`COUITheme.colorScheme` 没有对应这些颜色的槽位，因此它们保留为 `LockScreenPwdInputDefaults.colors()` 背后的文件常量。

## 行为

- **绘制顺序。** `COUILockScreenPwdInputView.onDraw` 先以 `coui_input_lock_screen_border_width * 2`（1.2dp）用 `mBorderPaint` 描边胶囊，再把 `InnerShadowHelper` 的位图叠上去；`COUILockScreenPwdInputLayout.dispatchDraw` 在下一步按钮周围绘制同样的描边与内阴影，然后绘制 `LightEffectHelper` 的光。本移植遵循同样的顺序：填充、上阴影、下阴影、描边、输入框、光。
- **内阴影。** 两次 `addInnerShadowLayer` 构造一支带 `setShadowLayer` 的 `Style.STROKE` 画笔，裁剪到路径内，因此只有模糊描边在胶囊内可见：`(32, 0, -8, 上, 20)` 与 `(8, 0, 2, 下, 12)`。这些数字是原始像素。commonMain 的 canvas 没有 `BlurMaskFilter`，因此本移植用垂直渐变近似，阴影色从胶囊边缘向内延伸 `radius + |dy| + strokeWidth / 2` 后淡出。
- **光效扫过。** 下一步按钮的触摸会执行 `executeLightEffectAnimator(true / false)`，把 `mOuterLightAlpha` 与 `mInnerLightAlpha` 在 0 与 255 之间弹动。外圈 `RadialGradient` 覆盖胶囊，半径随 alpha 从 `LightRadiusStartFraction`（0.6 倍）增长到 1 倍；内圈 `RadialGradient` 位于按钮上，半径 24dp，并按按钮的按压缩放缩小。两者都以 `BlendMode.Lighten` 绘制。两个弹簧都是 `COUISpringForce`，bounce 0、response 0.3，只有外圈在光消失时改为 response 0.6。
- **按压缩放。** 按钮缩到 `LockScreenPwdInputDefaults.PressedScale`（0.92f），即 `COUIPressFeedbackHelper` 对 48dp 表面的终止比例，使用临界阻尼、response 0.3 的弹簧。

### 几何

胶囊与按钮高度都是 `Height`（48dp），因此胶囊圆角为 24dp。按钮是 `Height` x `Height` 的圆形，距输入框 `IconMargin`（12dp），箭头为 `NextIconSize`（24dp）。输入文本的水平内边距为 `CardPadding`（16dp），垂直为 `TitlePadding`（10.5dp）。

## 未移植 / 差异

- 平滑圆角变体（`RoundCornerUtil.getSmoothStyleType() == 1` 配合 `OplusPathAdapter` / `OplusOutlineAdapter`）；本移植始终绘制普通圆角矩形，即 `addSmoothRoundRect` 与两个 outline provider 中 `smoothType != 1` 的分支。
- `UIUtil.confirmLevelAnim(UIUtil.ANIM_LEVEL_SUPPORT_BLUR_MIN)` 与 `mScenesMode`：原生只在支持模糊动画等级的设备的桌面场景模式下绘制描边、内阴影与光效。本移植始终绘制，即桌面形态。
- 设置场景模式的胶囊填充（`COUIContextUtil.getAttrColor(couiColorCard)`）、其 `coui_input_lock_screen_pwd_setting_width`（268dp）/ `_setting1_width`（220dp）尺寸，以及卡片的 `_card_padding`（16dp）/ `_card_max_height`（63dp）/ `_title_padding_top` 与 `_bottom`（10.5dp）度量。默认值记录了这些数值，但宽度交由调用方的 `modifier` 决定。
- 下一步按钮的四张图（`coui_input_lock_screen_pwd_next_bg`、`_next_src` / `_next_src_allow`、`_next_desktop_bg`、`_next_desktop_src` / `_next_desktop_src_allow`）：本移植绘制一个使用 `nextBackgroundColor` 的圆形，并着色一个箭头。`_allow` 组合只是把箭头的填充在 `?couiColorPrimaryNeutral` 与 `coui_color_label_tertiary_dark` 之间切换，即启用 / 禁用两种状态。
- 内层编辑框：`COUILockScreenPwdInputView` 是 `COUIInputView` 包裹一个 `COUIEditText`，由 `R.attr.COUICardLockScreenPwdInputStyleEdit(.Desktop)` 定样式，包含密码显示勾选框（`checkbox_password`、`coui_edittext_password_icon_desktop`）、`couiInputMaxCount` 截断、`couiInputMinCount` 门槛、`COUIChangeTextUtil.adaptFontSize` 以及 `coui_input_lock_screen_pwd_title_padding_*` 度量。本移植改为嵌入本库的 `TextField`，因此其提示颜色来自主题而非该样式。
- `COUILockScreenPwdInputView.SmoothRoundCornerHelper`、`updateCardWidth` 与 `setDefaultInputLockScreenPwdWidth`，报告中显示它们是平滑圆角配套代码或已废弃的空实现。
- `popupKeyboard`、`closeError`、`showError` 以及内层编辑框的 `COUIErrorEditTextHelper` 错误态。
- 按钮的 `COUIPressFeedbackHelper` 按压着色与 `COUIMaskEffectDrawable`；本移植只保留其缩放。
