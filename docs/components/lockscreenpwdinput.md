# LockScreenPwdInput

`LockScreenPwdInput` is the ColorOS 17 lock screen password field. It is a pill-shaped password field with a 48dp circular next button: the pill carries a stroke, an inner shadow along its top and bottom edges, and, while the next button is pressed, a light that sweeps out of the button and across the field.

The layout is `layout/coui_input_lock_screen_pwd_layout.xml`, a `ConstraintLayout` with the input view (`0dp` wide, chained) and a 48dp `ImageView` next button whose `layout_marginStart` is `coui_input_lock_screen_pwd_icon_margin` (12dp) and whose `src` is `coui_input_lock_screen_pwd_next_src`, a 24dp right arrow. `COUILockScreenPwdInputView.addSmoothRoundRect` builds the pill path with `rectF.height() / 2` as both corner radii, so the corner radius is `LockScreenPwdInputDefaults.Height / 2` (24dp).

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.LockScreenPwdInput
import io.github.suqi8.coui.kmp.basic.LockScreenPwdInputColors
import io.github.suqi8.coui.kmp.basic.LockScreenPwdInputDefaults
```

## Basic Usage

```kotlin
var password by remember { mutableStateOf("") }

LockScreenPwdInput(
    value = password,
    onValueChange = { password = it },
    label = "Password",
)
```

The text is masked by default: the native sets `couiEnablePassword` to true, and the port defaults `visualTransformation` to `PasswordVisualTransformation()`.

## Submitting the Password

`onNextClick` fires when the next button is released. `minCount` (the native's `couiInputMinCount`) gates it: while the text is shorter the button is drawn with the disabled arrow colour and ignores pointers, which is the `_allow` / normal drawable swap of `changeNextTransparentImageResource`.

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

`onValueChange` is truncated to `maxCount` (`couiInputMaxCount`), the same way the native's `TextWatcher` calls `editable.subSequence(0, mInputView.getMaxCount())`. Pass `nextIcon = null` to drop the button entirely.

## Properties

| Property Name          | Type                     | Description                                                                | Default Value                       | Required |
| ---------------------- | ------------------------ | -------------------------------------------------------------------------- | ----------------------------------- | -------- |
| value                  | String                   | The password text                                                          | -                                   | Yes      |
| onValueChange          | (String) -> Unit         | Called with the new text, truncated to `maxCount`                          | -                                   | Yes      |
| modifier               | Modifier                 | Modifier applied to the row; the pill takes the remaining width            | Modifier                            | No       |
| label                  | String                   | The hint shown while the field is empty                                    | ""                                  | No       |
| enabled       | Boolean                   | Whether the field and the button react to input                            | true                                | No       |
| scene         | LockScreenPwdInputScene   | `Desktop` (lock screen pill, inner shadow, light sweep) or `Setting` (plain card field) | LockScreenPwdInputScene.Desktop | No |
| minCount               | Int                      | Characters needed before the next button fires                             | LockScreenPwdInputDefaults.MinInputCount | No  |
| maxCount               | Int                      | Hard character limit                                                       | LockScreenPwdInputDefaults.MaxInputCount | No  |
| onNextClick            | (String) -> Unit         | Called with the text when the next button is released                      | {}                                  | No       |
| nextIcon               | ImageVector?             | The arrow in the button; null hides the button                             | LockScreenPwdInputDefaults.NextIcon | No       |
| nextContentDescription | String                   | The description of the next button                                         | LockScreenPwdInputDefaults.NextDescription | No |
| colors                 | LockScreenPwdInputColors | The border, shadow, light and button colours                               | LockScreenPwdInputDefaults.colors(scene) | No  |
| textStyle              | TextStyle                | The text style; its colour is overridden with `colors.textColor`          | LockScreenPwdInputDefaults.textStyle() | No    |
| keyboardOptions        | KeyboardOptions          | The keyboard options of the field                                          | KeyboardOptions.Default             | No       |
| keyboardActions        | KeyboardActions          | The keyboard actions of the field                                          | KeyboardActions.Default             | No       |
| visualTransformation   | VisualTransformation     | The visual transformation of the field                                     | PasswordVisualTransformation()      | No       |
| interactionSource      | MutableInteractionSource? | The interaction source of the field                                       | null                                | No       |

### LockScreenPwdInputColors

| Property Name          | Description                                                                                          |
| ---------------------- | ---------------------------------------------------------------------------------------------------- |
| containerColor         | The pill's background, `coui_input_lock_screen_pwd_view_bg_color_desktop` (#33ffffff)               |
| borderColor            | The pill's stroke, `coui_input_lock_screen_border_color` (#0dffffff)                                 |
| upperInnerShadowColor  | The shadow along the top inner edge, `..._upper_inner_shadow_color` (#5cffffff)                      |
| lowerInnerShadowColor  | The shadow along the bottom inner edge, `..._lower_inner_shadow_color` (#33ffffff)                   |
| outerGradientColor1    | The light sweep's outer gradient, `..._outer_gradient_color_1` (#80ffffff)                           |
| outerGradientColor2    | `..._outer_gradient_color_2` (#33ffffff)                                                             |
| outerGradientColor3    | `..._outer_gradient_color_3` (#0dffffff)                                                             |
| innerGradientColor1    | The light sweep's inner gradient, `..._inner_gradient_color_1` (#8fffffff)                           |
| innerGradientColor2    | `..._inner_gradient_color_2` (#e6ffffff)                                                             |
| textColor              | The text, `..._pwd_view_edittext_text_color_desktop` (#ffffffff)                                     |
| nextBackgroundColor    | The circle behind the arrow, written literally in `..._pwd_next_desktop_bg` (#33ffffff)              |
| nextIconColor          | The enabled arrow, `?couiColorPrimaryNeutral` (#e6000000 / #e6ffffff)                                |
| nextDisabledIconColor  | The disabled arrow, `coui_color_label_tertiary_dark` (#4dffffff)                                     |

### LockScreenPwdInputDefaults

| Constant               | Type       | Value    | COUI Source                                    |
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
| BorderWidth            | Dp         | 0.6.dp   | coui_input_lock_screen_border_width, drawn at 2x |
| LightShaderRadius      | Dp         | 90.dp    | coui_input_lock_screen_light_shader_radius     |
| NextIconSize           | Dp         | 24.dp    | the size of coui_input_lock_screen_pwd_next_src |
| NextDescription        | String     | "Next"   | string/coui_lock_screen_next_button_description |
| LightRadiusStartFraction | Float    | 0.6f     | mOuterLightRadiusStart = 0.6f * f2             |
| OuterGradientStops     | FloatArray | 0, 0.3, 0.6, 0.8, 1 | GRADIENT_OUTER_STOP_1..3          |
| InnerGradientStops     | FloatArray | 0, 0.7, 1 | GRADIENT_INNER_STOP_1                         |
| InnerShadowRadius1     | Float      | 32f      | INNER_SHADOW_RADIUS_1, raw px                  |
| InnerShadowDy1         | Float      | -8f      | INNER_SHADOW_DY_1, raw px                      |
| InnerShadowStrokeWidth1 | Float     | 20f      | INNER_SHADOW_STROKE_WIDTH_1, raw px            |
| InnerShadowRadius2     | Float      | 8f       | INNER_SHADOW_RADIUS_2, raw px                  |
| InnerShadowDy2         | Float      | 2f       | INNER_SHADOW_DY_2, raw px                      |
| InnerShadowStrokeWidth2 | Float     | 12f      | INNER_SHADOW_STROKE_WIDTH_2, raw px            |
| PressedScale           | Float      | 0.92f    | COUIPressFeedbackHelper's end ratio for 48dp   |
| NextIcon               | ImageVector | 24dp    | drawable/coui_input_lock_screen_pwd_next_src   |

The border is stored at `BorderWidth` (0.6dp) but painted at twice that value (1.2dp), which is what the native does with `mButtonBorderWidth`. `LightShaderRadius` is the light's radius before `onSizeChanged` recomputes it to `layoutWidth - inputWidth / 2 - nextIconWidth / 2`, i.e. the distance from the middle of the input to the middle of the button; the port uses that same expression, derived from the measured pill and the icon margin.

#### Methods

| Method Name            | Type                    | Description                                                            |
| ---------------------- | ----------------------- | ---------------------------------------------------------------------- |
| upperShadowReach()     | Float                   | 32 + 8 + 10 = 50px, how far the top shadow reaches inside the pill     |
| lowerShadowReach()     | Float                   | 8 + 2 + 6 = 16px, the same for the bottom shadow                       |
| outerGradientStops(c)  | Array&lt;Pair&lt;Float, Color&gt;&gt; | The five stops of the outer radial gradient, ends transparent |
| innerGradientStops(c)  | Array&lt;Pair&lt;Float, Color&gt;&gt; | The three stops of the inner radial gradient          |
| colors()               | LockScreenPwdInputColors | The desktop scene palette, day/night for the arrow only                |
| textStyle()            | TextStyle               | The COUI input text of `TextFieldMode.None`                            |
| textFieldColors(label) | TextFieldColors         | The inner field's colours; only the label colour is set                |

### Colours

| COUI token                                          | Value     | Used for                          | Port                         |
| --------------------------------------------------- | --------- | --------------------------------- | ---------------------------- |
| color/coui_input_lock_screen_border_color           | #0dffffff | Pill stroke                       | file constant, no theme slot |
| color/coui_input_lock_screen_upper_inner_shadow_color | #5cffffff | Top inner shadow                | file constant, no theme slot |
| color/coui_input_lock_screen_lower_inner_shadow_color | #33ffffff | Bottom inner shadow             | file constant, no theme slot |
| color/coui_input_lock_screen_outer_gradient_color_1 | #80ffffff | Outer light sweep                 | file constant, no theme slot |
| color/coui_input_lock_screen_outer_gradient_color_2 | #33ffffff | Outer light sweep                 | file constant, no theme slot |
| color/coui_input_lock_screen_outer_gradient_color_3 | #0dffffff | Outer light sweep                 | file constant, no theme slot |
| color/coui_input_lock_screen_inner_gradient_color_1 | #8fffffff | Inner light sweep                 | file constant, no theme slot |
| color/coui_input_lock_screen_inner_gradient_color_2 | #e6ffffff | Inner light sweep                 | file constant, no theme slot |
| color/coui_input_lock_screen_pwd_view_bg_color_desktop | #33ffffff | Pill fill                      | file constant, no theme slot |
| color/coui_input_lock_screen_pwd_view_edittext_text_color_desktop | #ffffffff | Text              | file constant, no theme slot |
| color/coui_input_lock_screen_pwd_next_desktop_bg    | #33ffffff | Button circle                     | file constant, no theme slot |
| attr/couiColorPrimaryNeutral                        | #e6000000 / #e6ffffff | Enabled arrow          | colors(), day/night          |
| color/coui_color_label_tertiary_dark                | #4dffffff | Disabled arrow                    | file constant, no theme slot |

The corresponding constants of `LockScreenPwdInputDefaults` are `BorderColor`, `UpperInnerShadowColor`, `LowerInnerShadowColor`, `OuterGradientColor1` / `2` / `3`, `InnerGradientColor1` / `2`, `ContainerColorDesktop`, `TextColorDesktop`, `NextBackgroundColor`, `NextDisabledIconColor`, and `NextIconColorLight` / `NextIconColorDark`. `CardBackgroundLight` (#ffffff) and `CardBackgroundDark` (#1affffff) are the settings scene mode's pill fill (`coui_color_card_background`) and are recovered but unused, since the port is the desktop presentation.

The native picks the desktop scene mode with `mScenesMode`, not from the theme, so the pill, the border, the shadows and the light are fixed values; only `coui_color_primary_neutral`, which the `_allow` arrow points at, is day/night qualified. `COUITheme.colorScheme` has no slot for any of these, so they stay file constants behind `LockScreenPwdInputDefaults.colors()`.

## Behavior

- **Draw order.** `COUILockScreenPwdInputView.onDraw` strokes the pill with `mBorderPaint` at `coui_input_lock_screen_border_width * 2` (1.2dp) and then blits the `InnerShadowHelper` bitmap over it; `COUILockScreenPwdInputLayout.dispatchDraw` draws the same border and inner shadow around the next button and then the light of `LightEffectHelper`. The port follows that order: fill, top shadow, bottom shadow, stroke, field, light.
- **Inner shadow.** Two `addInnerShadowLayer` calls build a `Style.STROKE` paint carrying a `setShadowLayer`, clipped to the path, so only the blurred stroke shows inside the pill: `(32, 0, -8, upper, 20)` and `(8, 0, 2, lower, 12)`. Those are raw pixels. Because a commonMain canvas has no `BlurMaskFilter`, the port approximates each with a vertical gradient that fades the shadow colour out over `radius + |dy| + strokeWidth / 2` from the pill edge.
- **Light sweep.** The next button's touch runs `executeLightEffectAnimator(true / false)`, springing `mOuterLightAlpha` and `mInnerLightAlpha` between 0 and 255. The outer `RadialGradient` spreads over the pill, its radius growing from `LightRadiusStartFraction` (0.6x) to 1x as the alpha rises; the inner `RadialGradient` sits over the button at 24dp and is scaled by the button's press scale. Both are painted with `BlendMode.Lighten`. The two springs are `COUISpringForce` at bounce 0 and response 0.3, except that the outer one is re-tuned to response 0.6 while the light disappears.
- **Press scale.** The button shrinks to `LockScreenPwdInputDefaults.PressedScale` (0.92f), the `COUIPressFeedbackHelper` end ratio for a 48dp surface, on a critical-damped response 0.3 spring.

### Geometry

The pill and the button are both `Height` (48dp) tall, so the pill's corner radius is 24dp. The button is a `Height` x `Height` circle, `IconMargin` (12dp) from the field, and its arrow is `NextIconSize` (24dp). The field's text is inset by `CardPadding` (16dp) horizontally and `TitlePadding` (10.5dp) vertically.

## Not Ported

- The smooth-corner variants (`RoundCornerUtil.getSmoothStyleType() == 1` with `OplusPathAdapter` / `OplusOutlineAdapter`); the port always draws the plain round rect, which is the `smoothType != 1` branch of `addSmoothRoundRect` and of both outline providers.
- `UIUtil.confirmLevelAnim(UIUtil.ANIM_LEVEL_SUPPORT_BLUR_MIN)` and `mScenesMode`: the native only paints the border, the inner shadow and the light in desktop scene mode on a device that supports the blur animation level. The port always paints them, i.e. it is the desktop presentation.
- The settings scene mode's pill fill (`COUIContextUtil.getAttrColor(couiColorCard)`), its `coui_input_lock_screen_pwd_setting_width` (268dp) / `_setting1_width` (220dp) sizing and the card's `_card_padding` (16dp) / `_card_max_height` (63dp) / `_title_padding_top` and `_bottom` (10.5dp) metrics. The defaults record the values, but the width is the caller's `modifier`.
- The next button's four drawables (`coui_input_lock_screen_pwd_next_bg`, `_next_src` / `_next_src_allow`, `_next_desktop_bg`, `_next_desktop_src` / `_next_desktop_src_allow`): the port draws a circle in `nextBackgroundColor` and tints one arrow. The `_allow` pair only swaps the arrow's fill between `?couiColorPrimaryNeutral` and `coui_color_label_tertiary_dark`, which is the enabled / disabled pair.
- The inner edit text: `COUILockScreenPwdInputView` is a `COUIInputView` around a `COUIEditText` styled by `R.attr.COUICardLockScreenPwdInputStyleEdit(.Desktop)`, with its password check box (`checkbox_password`, `coui_edittext_password_icon_desktop`), the `couiInputMaxCount` truncation, the `couiInputMinCount` gating, `COUIChangeTextUtil.adaptFontSize` and the `coui_input_lock_screen_pwd_title_padding_*` metrics. The port embeds the library's `TextField` instead, so its hint colour comes from the theme rather than from that style.
- `COUILockScreenPwdInputView.SmoothRoundCornerHelper`, `updateCardWidth` and `setDefaultInputLockScreenPwdWidth`, which the report shows are smooth-corner plumbing or deprecated no-ops.
- `popupKeyboard`, `closeError`, `showError` and the `COUIErrorEditTextHelper` error state of the inner edit text.
- The `COUIPressFeedbackHelper` press tint and the `COUIMaskEffectDrawable` of the button; only its scale is ported.
