// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch

/**
 * The ColorOS 17 lock screen password field.
 *
 * The layout is a `ConstraintLayout` holding the input view (`0dp` wide, chained) and a 48dp
 * `ImageView` next button whose start margin is 12dp and whose glyph is a 24dp right arrow. The view
 * itself is a pill:
 * `COUILockScreenPwdInputView.addSmoothRoundRect` builds its path with `rectF.height() / 2` as both
 * corner radii, so the corner radius is [LockScreenPwdInputDefaults.Height] / 2 (24dp).
 *
 * What the two classes paint, in draw order:
 * - `COUILockScreenPwdInputView.onDraw` strokes the pill with `mBorderPaint` at
 *   `coui_input_lock_screen_border_width * 2` (1.2dp) in `coui_input_lock_screen_border_color`
 *   (`#0dffffff`), then blits the `InnerShadowHelper` bitmap over it;
 * - `COUILockScreenPwdInputLayout.dispatchDraw` draws the same border and inner shadow around the
 *   next button, and then the light sweep of `LightEffectHelper`.
 *
 * The inner shadow is two `addInnerShadowLayer` calls — a `Paint` of `Style.STROKE` carrying a
 * `setShadowLayer`, clipped to the path, so only the blurred stroke shows inside the pill:
 * `(32, 0, -8, coui_input_lock_screen_upper_inner_shadow_color, 20)` and
 * `(8, 0, 2, coui_input_lock_screen_lower_inner_shadow_color, 12)`. Those numbers are raw pixels,
 * not dp, and the native uses them as pixels. The port approximates the blurred stroke with a
 * vertical gradient that fades the shadow colour out over
 * `radius + |dy| + strokeWidth / 2` from the pill edge, because a commonMain canvas has no
 * `BlurMaskFilter`.
 *
 * The light sweep is `LightEffectHelper.drawLightEffect`, triggered by the next button's touch:
 * `COUILockScreenPwdInputLayout.executeLightEffectAnimator(true / false)` springs `mOuterLightAlpha`
 * and `mInnerLightAlpha` from 0 to 255 and back, and the helper paints
 * - the outer `RadialGradient` over the pill, radius `mLightShaderRadius` — recomputed by
 *   `onSizeChanged` as `layoutWidth - inputWidth / 2 - nextIconWidth / 2`, i.e. the distance from the
 *   middle of the input to the middle of the button — with the stops `0 / 0.3 / 0.6 / 0.8 / 1` of
 *   `0` (transparent), `coui_input_lock_screen_outer_gradient_color_1` (`#80ffffff`), `_2`
 *   (`#33ffffff`), `_3` (`#0dffffff`) and `0`, its radius scaled from `0.6x` to `1x` as the alpha
 *   rises;
 * - the inner `RadialGradient` over the button, radius `mNextIconRadius` (24dp), with the stops
 *   `0 / 0.7 / 1` of `0`, `coui_input_lock_screen_inner_gradient_color_1` (`#8fffffff`) and `_2`
 *   (`#e6ffffff`), drawn as a circle scaled by the button's press scale.
 *
 * Both are painted with `BlendMode.LIGHTEN`. The two alpha springs are `COUISpringForce` at bounce 0
 * and response 0.3, except that the outer one is re-tuned to response 0.6 while the light
 * disappears. The button's own press scale is `COUIPressFeedbackHelper`, whose end ratio for a 48dp
 * surface is [ButtonDefaults.PressedScale].
 *
 * Not ported:
 * - the smooth-corner variants (`RoundCornerUtil.getSmoothStyleType() == 1` with `OplusPathAdapter`
 *   / `OplusOutlineAdapter`); the port always draws the plain round rect, which is the
 *   `smoothType != 1` branch of `addSmoothRoundRect` and of both outline providers;
 * - `UIUtil.confirmLevelAnim(UIUtil.ANIM_LEVEL_SUPPORT_BLUR_MIN)` and `mScenesMode`: the native only
 *   paints the border, the inner shadow and the light in desktop scene mode on a device that
 *   supports the blur animation level. The port always paints them, i.e. it is the desktop
 *   presentation;
 * - the settings scene mode's pill fill (`COUIContextUtil.getAttrColor(couiColorCard)`), its
 *   `coui_input_lock_screen_pwd_setting_width` (268dp) / `_setting1_width` (220dp) sizing and the
 *   card's `_card_padding` (16dp) / `_card_max_height` (63dp) / `_title_padding_top` and `_bottom`
 *   (10.5dp) metrics. [LockScreenPwdInputDefaults] records the values, but the port leaves the width
 *   to the caller's `modifier` and documents `_desktop_width` (280dp);
 * - the next button's four drawables (`coui_input_lock_screen_pwd_next_bg`, `_next_src` /
 *   `_next_src_allow`, `_next_desktop_bg`, `_next_desktop_src` / `_next_desktop_src_allow`): the port
 *   draws a circle in [LockScreenPwdInputColors.nextBackgroundColor] and tints one arrow with
 *   [LockScreenPwdInputColors.nextIconColor] / [LockScreenPwdInputColors.nextDisabledIconColor], and
 *   uses the recovered `coui_input_lock_screen_pwd_next_src` path as
 *   [LockScreenPwdInputDefaults.NextIcon]. The `_allow` pair only swaps the arrow's fill between
 *   `?couiColorPrimaryNeutral` and `coui_color_label_tertiary_dark`, which is that enabled /
 *   disabled pair;
 * - the inner edit text: `COUILockScreenPwdInputView` is a `COUIInputView` around a `COUIEditText`
 *   styled by `R.attr.COUICardLockScreenPwdInputStyleEdit(.Desktop)`, with its password check box
 *   (`checkbox_password`, `coui_edittext_password_icon_desktop`), the `couiInputMaxCount` truncation,
 *   the `couiInputMinCount` gating, `COUIChangeTextUtil.adaptFontSize` and the
 *   `coui_input_lock_screen_pwd_title_padding_*` metrics. The port embeds the library's [TextField]
 *   instead, so its hint colour comes from the theme rather than from that style;
 * - `COUILockScreenPwdInputView.SmoothRoundCornerHelper`, `updateCardWidth` and
 *   `setDefaultInputLockScreenPwdWidth`, which the report shows are smooth-corner plumbing or
 *   deprecated no-ops;
 * - `popupKeyboard`, `closeError`, `showError` and the `COUIErrorEditTextHelper` error state of the
 *   inner edit text;
 * - the `COUIPressFeedbackHelper` press tint and the `COUIMaskEffectDrawable` of the button; only its
 *   scale is ported.
 *
 * @param value The password text.
 * @param onValueChange Called with the new text, truncated to [maxCount] characters — what
 *   `COUILockScreenPwdInputLayout`'s `TextWatcher` does with
 *   `editable.subSequence(0, mInputView.getMaxCount())`.
 * @param modifier The modifier to be applied to the row. The pill takes the remaining width,
 *   mirroring the `0dp` chained input view of the layout.
 * @param label The hint shown while the field is empty.
 * @param enabled Whether the field and the next button react to input.
 * @param scene Which presentation the field wears; see [LockScreenPwdInputScene]. [colors]
 *   defaults to the matching palette, and the border, inner shadow and light sweep only exist in
 *   [LockScreenPwdInputScene.Desktop].
 * @param minCount The number of characters the next button needs before it fires
 *   (`couiInputMinCount`, `DEFAULT_MIN_INPUT_COUNT`). While [value] is shorter the button is drawn
 *   with [LockScreenPwdInputColors.nextDisabledIconColor] and ignores pointers, which is the normal /
 *   `_allow` drawable swap of `changeNextTransparentImageResource`.
 * @param maxCount The hard character limit (`couiInputMaxCount`, `DEFAULT_MAX_INPUT_COUNT`).
 * @param onNextClick Called with the text when the next button is released
 *   (`NextIconCheckListener.checkedPwd`).
 * @param nextIcon The arrow drawn in the button, or null to hide the button entirely.
 * @param nextContentDescription The description of the next button,
 *   `string/coui_lock_screen_next_button_description`.
 * @param colors The border, inner-shadow, light-sweep and button colours.
 * @param textStyle The style of the text, whose colour the port overrides with
 *   [LockScreenPwdInputColors.textColor].
 * @param keyboardOptions The keyboard options of the field.
 * @param keyboardActions The keyboard actions of the field.
 * @param visualTransformation The visual transformation of the field. The layout sets
 *   `couiEnablePassword` to true, so the default masks the text.
 * @param interactionSource The interaction source of the field.
 */
@Composable
fun LockScreenPwdInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    enabled: Boolean = true,
    scene: LockScreenPwdInputScene = LockScreenPwdInputScene.Desktop,
    minCount: Int = LockScreenPwdInputDefaults.MinInputCount,
    maxCount: Int = LockScreenPwdInputDefaults.MaxInputCount,
    onNextClick: (String) -> Unit = {},
    nextIcon: ImageVector? = LockScreenPwdInputDefaults.NextIcon,
    nextContentDescription: String = LockScreenPwdInputDefaults.NextDescription,
    colors: LockScreenPwdInputColors = LockScreenPwdInputDefaults.colors(scene),
    textStyle: TextStyle = LockScreenPwdInputDefaults.textStyle(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = PasswordVisualTransformation(),
    interactionSource: MutableInteractionSource? = null,
) {
    val resolvedColors = if (scene == LockScreenPwdInputScene.Setting) {
        LockScreenPwdInputDefaults.settingColors()
    } else {
        colors
    }
    val decorated = scene == LockScreenPwdInputScene.Desktop
    val density = LocalDensity.current
    val heightPx = with(density) { LockScreenPwdInputDefaults.Height.toPx() }
    val iconMarginPx = with(density) { LockScreenPwdInputDefaults.IconMargin.toPx() }
    val borderWidthPx = with(density) { LockScreenPwdInputDefaults.BorderWidth.toPx() }
    val upperShadowReachPx = LockScreenPwdInputDefaults.upperShadowReach()
    val lowerShadowReachPx = LockScreenPwdInputDefaults.lowerShadowReach()

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val handleValueChange: (String) -> Unit = remember(maxCount) {
        { new ->
            currentOnValueChange(if (maxCount > 0 && new.length > maxCount) new.take(maxCount) else new)
        }
    }

    val nextEnabled = enabled && value.length >= minCount
    val nextInteractionSource = remember { MutableInteractionSource() }
    val isNextPressed by nextInteractionSource.collectIsPressedAsState()

    val innerLight = remember { Animatable(0f) }
    val outerLight = remember { Animatable(0f) }
    val pressProgress = remember { Animatable(0f) }
    val outerStops = remember(resolvedColors) { LockScreenPwdInputDefaults.outerGradientStops(resolvedColors) }
    val innerStops = remember(resolvedColors) { LockScreenPwdInputDefaults.innerGradientStops(resolvedColors) }

    LaunchedEffect(isNextPressed, nextEnabled) {
        if (!nextEnabled) {
            innerLight.snapTo(0f)
            outerLight.snapTo(0f)
            pressProgress.snapTo(0f)
            return@LaunchedEffect
        }
        val target = if (isNextPressed) 1f else 0f
        val outerSpec = if (isNextPressed) OuterLightAppearSpring else OuterLightDisappearSpring
        launch { innerLight.animateTo(target, InnerLightSpring) }
        launch { outerLight.animateTo(target, outerSpec) }
        launch { pressProgress.animateTo(target, NextPressSpring) }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(LockScreenPwdInputDefaults.Height)
                .clip(RoundedCornerShape(percent = 50))
                .background(resolvedColors.containerColor),
        ) {
            if (decorated) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val radius = size.height / 2f
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to resolvedColors.upperInnerShadowColor,
                                1f to resolvedColors.upperInnerShadowColor.copy(alpha = 0f),
                            ),
                            startY = 0f,
                            endY = upperShadowReachPx,
                        ),
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to resolvedColors.lowerInnerShadowColor.copy(alpha = 0f),
                                1f to resolvedColors.lowerInnerShadowColor,
                            ),
                            startY = size.height - lowerShadowReachPx,
                            endY = size.height,
                        ),
                    )
                    drawRoundRect(
                        color = resolvedColors.borderColor,
                        topLeft = Offset(borderWidthPx / 2f, borderWidthPx / 2f),
                        size = Size(size.width - borderWidthPx, size.height - borderWidthPx),
                        cornerRadius = CornerRadius(radius, radius),
                        style = Stroke(width = borderWidthPx),
                    )
                }
            }
            TextField(
                value = value,
                onValueChange = handleValueChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = LockScreenPwdInputDefaults.CardPadding,
                        vertical = LockScreenPwdInputDefaults.TitlePadding,
                    ),
                backgroundMode = TextFieldMode.None,
                insideMargin = DpSize(0.dp, 0.dp),
                colors = LockScreenPwdInputDefaults.textFieldColors(resolvedColors.textColor),
                label = label,
                enabled = enabled,
                textStyle = textStyle.copy(color = resolvedColors.textColor),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = true,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(resolvedColors.textColor),
            )
            if (decorated) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val outerAlpha = outerLight.value
                    val innerAlpha = innerLight.value
                    if (outerAlpha <= 0f && innerAlpha <= 0f) return@Canvas
                    val center = Offset(size.width + iconMarginPx + heightPx / 2f, size.height / 2f)
                    if (outerAlpha > 0f) {
                        val outerRadius = (center.x - size.width / 2f).coerceAtLeast(1f)
                        val outerStart = LockScreenPwdInputDefaults.LightRadiusStartFraction * outerRadius
                        val scale = (outerStart + (outerRadius - outerStart) * outerAlpha) / outerRadius
                        drawRect(
                            brush = Brush.radialGradient(
                                colorStops = outerStops,
                                center = center,
                                radius = outerRadius * scale,
                            ),
                            alpha = outerAlpha,
                            blendMode = BlendMode.Lighten,
                        )
                    }
                    if (innerAlpha > 0f) {
                        val scale = 1f - (1f - LockScreenPwdInputDefaults.PressedScale) * pressProgress.value
                        val innerRadius = heightPx / 2f * scale
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = innerStops,
                                center = center,
                                radius = innerRadius,
                            ),
                            radius = innerRadius,
                            center = center,
                            alpha = innerAlpha,
                            blendMode = BlendMode.Lighten,
                        )
                    }
                }
            }
        }
        if (nextIcon != null) {
            Spacer(modifier = Modifier.width(LockScreenPwdInputDefaults.IconMargin))
            Box(
                modifier = Modifier
                    .size(LockScreenPwdInputDefaults.Height)
                    .graphicsLayer {
                        val scale = 1f - (1f - LockScreenPwdInputDefaults.PressedScale) * pressProgress.value
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(resolvedColors.nextBackgroundColor)
                    .clickable(
                        interactionSource = nextInteractionSource,
                        indication = null,
                        enabled = nextEnabled,
                        onClick = { onNextClick(value) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = nextIcon,
                    contentDescription = nextContentDescription,
                    modifier = Modifier.size(LockScreenPwdInputDefaults.NextIconSize),
                    tint = if (nextEnabled) resolvedColors.nextIconColor else resolvedColors.nextDisabledIconColor,
                )
            }
        }
    }
}

/**
 * Which presentation a [LockScreenPwdInput] wears.
 *
 * `COUILockScreenPwdInputLayout` reads `couiIsScenesMode` and keeps two of them: the lock screen's
 * `desktop` pill — translucent white, an inner shadow and the light sweep behind the next key —
 * and the `setting` form used inside a settings card, which is a plain card-coloured field with no
 * border, shadow or light at all.
 */
enum class LockScreenPwdInputScene {
    /** The lock screen form: `#33FFFFFF` pill, inner shadow, light sweep. */
    Desktop,

    /** The settings form: card fill, no border, no inner shadow, no light. */
    Setting,
}

/**
 * The colours of a [LockScreenPwdInput] — the `coui_input_lock_screen_*` resources
 * `COUILockScreenPwdInputView` and `COUILockScreenPwdInputLayout` read, plus the two fills of the
 * next button's drawables.
 *
 * @param containerColor The pill's background. `coui_input_lock_screen_pwd_view_bg_color_desktop`,
 *   `#33ffffff`.
 * @param borderColor The pill's stroke. `coui_input_lock_screen_border_color`, `#0dffffff`.
 * @param upperInnerShadowColor The shadow along the top inner edge.
 *   `coui_input_lock_screen_upper_inner_shadow_color`, `#5cffffff`.
 * @param lowerInnerShadowColor The shadow along the bottom inner edge.
 *   `coui_input_lock_screen_lower_inner_shadow_color`, `#33ffffff`.
 * @param outerGradientColor1 The light sweep's outer gradient.
 *   `coui_input_lock_screen_outer_gradient_color_1`, `#80ffffff`.
 * @param outerGradientColor2 `coui_input_lock_screen_outer_gradient_color_2`, `#33ffffff`.
 * @param outerGradientColor3 `coui_input_lock_screen_outer_gradient_color_3`, `#0dffffff`.
 * @param innerGradientColor1 The light sweep's inner gradient.
 *   `coui_input_lock_screen_inner_gradient_color_1`, `#8fffffff`.
 * @param innerGradientColor2 `coui_input_lock_screen_inner_gradient_color_2`, `#e6ffffff`.
 * @param textColor The text. `coui_input_lock_screen_pwd_view_edittext_text_color_desktop`,
 *   `#ffffffff`.
 * @param nextBackgroundColor The circle behind the arrow, the fill written literally into
 *   `coui_input_lock_screen_pwd_next_desktop_bg`, `#33ffffff`.
 * @param nextIconColor The arrow while the button is enabled:
 *   `coui_input_lock_screen_pwd_next_src_allow`'s fill, `?couiColorPrimaryNeutral`
 *   (`coui_color_primary_neutral`, `#e6000000` / `#e6ffffff`).
 * @param nextDisabledIconColor The arrow while the button is disabled:
 *   `coui_input_lock_screen_pwd_next_desktop_src`'s fill, `coui_color_label_tertiary_dark`,
 *   `#4dffffff`.
 */
@Immutable
data class LockScreenPwdInputColors(
    val containerColor: Color,
    val borderColor: Color,
    val upperInnerShadowColor: Color,
    val lowerInnerShadowColor: Color,
    val outerGradientColor1: Color,
    val outerGradientColor2: Color,
    val outerGradientColor3: Color,
    val innerGradientColor1: Color,
    val innerGradientColor2: Color,
    val textColor: Color,
    val nextBackgroundColor: Color,
    val nextIconColor: Color,
    val nextDisabledIconColor: Color,
)

/** Contains the default values used by [LockScreenPwdInput]. */
object LockScreenPwdInputDefaults {

    /** `COUILockScreenPwdInputView.DEFAULT_MIN_INPUT_COUNT`, 6. */
    val MinInputCount = 6

    /** `COUILockScreenPwdInputView.DEFAULT_MAX_INPUT_COUNT`, 16. */
    val MaxInputCount = 16

    /**
     * The height of the pill and the side of the next button. COUI
     * `coui_input_lock_screen_pwd_height`, which the layout applies to both the input view and the
     * button, 48dp.
     */
    val Height = 48.dp

    /** COUI `coui_input_lock_screen_pwd_icon_margin`, the button's `layout_marginStart`, 12dp. */
    val IconMargin = 12.dp

    /** The width of the desktop form. COUI `coui_input_lock_screen_pwd_desktop_width`, 280dp. */
    val DesktopWidth = 280.dp

    /** The width of the settings form. COUI `coui_input_lock_screen_pwd_setting_width`, 268dp. */
    val SettingWidth = 268.dp

    /** The width of the first settings form. COUI `coui_input_lock_screen_pwd_setting1_width`, 220dp. */
    val Setting1Width = 220.dp

    /**
     * The horizontal padding of the field. COUI `coui_input_lock_screen_pwd_card_padding`, which
     * `coui_input_lock_screen_pwd_view.xml` sets as the `paddingStart` / `paddingEnd` of the row
     * holding the field, 16dp.
     */
    val CardPadding = 16.dp

    /** COUI `coui_input_lock_screen_pwd_card_max_height`, 63dp. */
    val CardMaxHeight = 63.dp

    /**
     * COUI `coui_input_lock_screen_pwd_edit_margin`, read into
     * `INPUT_LOCK_SCREEN_PWD_EDIT_MARGIN`, 4dp.
     */
    val EditMargin = 4.dp

    /**
     * The vertical padding of the field. `COUILockScreenPwdInputView.getEdittextPaddingTop()` and
     * `getEdittextPaddingBottom()` both return COUI
     * `coui_input_lock_screen_pwd_title_padding_top` / `_bottom`, 10.5dp.
     */
    val TitlePadding = 10.5.dp

    /**
     * The width of the pill's stroke. COUI `coui_input_lock_screen_border_width`, read into
     * `mButtonBorderWidth` and painted at twice its value, 0.6dp.
     */
    val BorderWidth = 0.6.dp

    /**
     * The radius of the light sweep's outer gradient before `onSizeChanged` recomputes it. COUI
     * `coui_input_lock_screen_light_shader_radius`, read into `mLightShaderRadius`, 90dp.
     */
    val LightShaderRadius = 90.dp

    /** The side of the arrow. `coui_input_lock_screen_pwd_next_src` is a 24dp vector. */
    val NextIconSize = 24.dp

    /**
     * The description of the next button. COUI `string/coui_lock_screen_next_button_description`,
     * `"Next"` (`"次へ"` in ja).
     */
    val NextDescription = "Next"

    /** `coui_input_lock_screen_border_color`, `#0dffffff`. */
    val BorderColor = Color(0x0DFFFFFF)

    /** `coui_input_lock_screen_upper_inner_shadow_color`, `#5cffffff`. */
    val UpperInnerShadowColor = Color(0x5CFFFFFF)

    /** `coui_input_lock_screen_lower_inner_shadow_color`, `#33ffffff`. */
    val LowerInnerShadowColor = Color(0x33FFFFFF)

    /** `coui_input_lock_screen_outer_gradient_color_1`, `#80ffffff`. */
    val OuterGradientColor1 = Color(0x80FFFFFF)

    /** `coui_input_lock_screen_outer_gradient_color_2`, `#33ffffff`. */
    val OuterGradientColor2 = Color(0x33FFFFFF)

    /** `coui_input_lock_screen_outer_gradient_color_3`, `#0dffffff`. */
    val OuterGradientColor3 = Color(0x0DFFFFFF)

    /** `coui_input_lock_screen_inner_gradient_color_1`, `#8fffffff`. */
    val InnerGradientColor1 = Color(0x8FFFFFFF)

    /** `coui_input_lock_screen_inner_gradient_color_2`, `#e6ffffff`. */
    val InnerGradientColor2 = Color(0xE6FFFFFF)

    /**
     * `coui_input_lock_screen_pwd_view_bg_color_desktop`, the translucent pill of the desktop scene
     * mode, `#33ffffff`.
     */
    val ContainerColorDesktop = Color(0x33FFFFFF)

    /** `coui_input_lock_screen_pwd_view_edittext_text_color_desktop`, `#ffffffff`. */
    val TextColorDesktop = Color(0xFFFFFFFF)

    /**
     * `coui_input_lock_screen_pwd_next_desktop_bg`'s fill, written literally in the vector,
     * `#33ffffff`.
     */
    val NextBackgroundColor = Color(0x33FFFFFF)

    /** `coui_color_card_background`, the settings scene mode's pill, `#ffffff`. */
    val CardBackgroundLight = Color(0xFFFFFFFF)

    /** `coui_color_card_background` at night, `#1affffff`. */
    val CardBackgroundDark = Color(0x1AFFFFFF)

    /** `coui_color_primary_neutral`, the enabled arrow, `#e6000000`. */
    val NextIconColorLight = Color(0xE6000000)

    /** `coui_color_primary_neutral` at night, `#e6ffffff`. */
    val NextIconColorDark = Color(0xE6FFFFFF)

    /** `coui_color_label_tertiary_dark`, the disabled arrow, `#4dffffff`. */
    val NextDisabledIconColor = Color(0x4DFFFFFF)

    /**
     * `LightEffectHelper.updateLightShaderConfig`'s `mOuterLightRadiusStart = 0.6f * f2`: the outer
     * gradient starts at 60% of the shader radius when the light appears and grows to 100% at full
     * alpha.
     */
    val LightRadiusStartFraction = 0.6f

    /**
     * `GRADIENT_OUTER_STOP_1`, `GRADIENT_OUTER_STOP_2` and `GRADIENT_OUTER_STOP_3`: the stops of the
     * outer `RadialGradient`, between `GRADIENT_COLOR_STOP_START` (0) and
     * `GRADIENT_COLOR_STOP_END` (1).
     */
    val OuterGradientStops = floatArrayOf(0f, 0.3f, 0.6f, 0.8f, 1f)

    /**
     * `GRADIENT_INNER_STOP_1`: the stop of `coui_input_lock_screen_inner_gradient_color_1` in the
     * inner `RadialGradient`, between 0 and 1.
     */
    val InnerGradientStops = floatArrayOf(0f, 0.7f, 1f)

    /** `INNER_SHADOW_RADIUS_1`, the blur radius of the first `addInnerShadowLayer`, in raw pixels. */
    val InnerShadowRadius1 = 32f

    /** `INNER_SHADOW_DY_1`, the vertical offset of the first layer, in raw pixels. */
    val InnerShadowDy1 = -8f

    /** `INNER_SHADOW_STROKE_WIDTH_1`, the stroke width of the first layer, in raw pixels. */
    val InnerShadowStrokeWidth1 = 20f

    /** `INNER_SHADOW_RADIUS_2`, the blur radius of the second layer, in raw pixels. */
    val InnerShadowRadius2 = 8f

    /** `INNER_SHADOW_DY_2`, the vertical offset of the second layer, in raw pixels. */
    val InnerShadowDy2 = 2f

    /** `INNER_SHADOW_STROKE_WIDTH_2`, the stroke width of the second layer, in raw pixels. */
    val InnerShadowStrokeWidth2 = 12f

    /**
     * `COUIPressFeedbackHelper`'s end ratio for the 48dp button, the value [ButtonDefaults] carries
     * for surfaces up to 48 x 48dp.
     */
    val PressedScale = ButtonDefaults.PressedScale

    /**
     * How far inside the pill the blurred stroke of the first `addInnerShadowLayer` reaches:
     * `INNER_SHADOW_RADIUS_1 + |INNER_SHADOW_DY_1| + INNER_SHADOW_STROKE_WIDTH_1 / 2`, in pixels.
     */
    fun upperShadowReach(): Float = InnerShadowRadius1 - InnerShadowDy1 + InnerShadowStrokeWidth1 / 2f

    /**
     * The same for the second layer, whose `INNER_SHADOW_DY_2` points down:
     * `INNER_SHADOW_RADIUS_2 + INNER_SHADOW_DY_2 + INNER_SHADOW_STROKE_WIDTH_2 / 2`, in pixels.
     */
    fun lowerShadowReach(): Float = InnerShadowRadius2 + InnerShadowDy2 + InnerShadowStrokeWidth2 / 2f

    /** The five `colorStops` of the outer `RadialGradient`, the first and last transparent. */
    fun outerGradientStops(colors: LockScreenPwdInputColors): Array<Pair<Float, Color>> = arrayOf(
        OuterGradientStops[0] to Color(0x00000000),
        OuterGradientStops[1] to colors.outerGradientColor1,
        OuterGradientStops[2] to colors.outerGradientColor2,
        OuterGradientStops[3] to colors.outerGradientColor3,
        OuterGradientStops[4] to Color(0x00000000),
    )

    /** The three `colorStops` of the inner `RadialGradient`, the first transparent. */
    fun innerGradientStops(colors: LockScreenPwdInputColors): Array<Pair<Float, Color>> = arrayOf(
        InnerGradientStops[0] to Color(0x00000000),
        InnerGradientStops[1] to colors.innerGradientColor1,
        InnerGradientStops[2] to colors.innerGradientColor2,
    )

    /**
     * The colours of the desktop scene mode. The native picks that mode with `mScenesMode`, not with
     * the theme, so the pill, the border, the shadows and the light are fixed; only
     * `coui_color_primary_neutral`, which the `_allow` arrow points at, is day/night qualified.
     */
    @Composable
    fun colors(scene: LockScreenPwdInputScene = LockScreenPwdInputScene.Desktop): LockScreenPwdInputColors {
        if (scene == LockScreenPwdInputScene.Setting) return settingColors()
        val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
        return LockScreenPwdInputColors(
            containerColor = ContainerColorDesktop,
            borderColor = BorderColor,
            upperInnerShadowColor = UpperInnerShadowColor,
            lowerInnerShadowColor = LowerInnerShadowColor,
            outerGradientColor1 = OuterGradientColor1,
            outerGradientColor2 = OuterGradientColor2,
            outerGradientColor3 = OuterGradientColor3,
            innerGradientColor1 = InnerGradientColor1,
            innerGradientColor2 = InnerGradientColor2,
            textColor = TextColorDesktop,
            nextBackgroundColor = NextBackgroundColor,
            nextIconColor = if (isDark) NextIconColorDark else NextIconColorLight,
            nextDisabledIconColor = NextDisabledIconColor,
        )
    }

    /**
     * The colours of the `setting` scene: `COUILockScreenPwdInputLayout.initScenesModeUI` fills the
     * field with `?attr/couiColorCard` and draws neither border nor inner shadow, and the text takes
     * the ordinary label colour instead of the desktop white.
     */
    @Composable
    fun settingColors(): LockScreenPwdInputColors {
        val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
        return LockScreenPwdInputColors(
            containerColor = if (isDark) CardBackgroundDark else CardBackgroundLight,
            borderColor = Color.Transparent,
            upperInnerShadowColor = Color.Transparent,
            lowerInnerShadowColor = Color.Transparent,
            outerGradientColor1 = Color.Transparent,
            outerGradientColor2 = Color.Transparent,
            outerGradientColor3 = Color.Transparent,
            innerGradientColor1 = Color.Transparent,
            innerGradientColor2 = Color.Transparent,
            textColor = COUITheme.colorScheme.onSurface,
            nextBackgroundColor = COUITheme.colorScheme.surfaceContainerHigh,
            nextIconColor = if (isDark) NextIconColorDark else NextIconColorLight,
            nextDisabledIconColor = NextDisabledIconColor,
        )
    }

    /**
     * The text style of the inner field: the COUI input text of [TextFieldMode.None], whose colour
     * [LockScreenPwdInput] overrides with [LockScreenPwdInputColors.textColor].
     */
    @Composable
    fun textStyle(): TextStyle = TextFieldDefaults.textStyle(TextFieldMode.None)

    /**
     * The colours of the inner [TextField]. The pill draws its own background and border, so only the
     * label colour is set here; the native's hint comes from
     * `R.attr.COUICardLockScreenPwdInputStyleEdit`, which is not recovered.
     *
     * @param labelColor The hint colour.
     */
    @Composable
    fun textFieldColors(labelColor: Color): TextFieldColors = TextFieldDefaults.textFieldColors(
        labelColor = labelColor,
    )

    /**
     * The next button's arrow, `drawable/coui_input_lock_screen_pwd_next_src`: a 24dp vector with
     * `android:autoMirrored="true"` and `android:fillType="evenOdd"`, whose `android:fillColor` is
     * `?couiColorLabelTertiary` and is overridden by [LockScreenPwdInput]'s tint.
     */
    val NextIcon: ImageVector = ImageVector.Builder(
        name = "LockScreenPwdNext",
        defaultWidth = NextIconSize,
        defaultHeight = NextIconSize,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = true,
    ).apply {
        addPath(
            pathData = listOf(
                PathNode.MoveTo(13.06f, 19.246f),
                PathNode.CurveTo(12.865f, 19.442f, 12.865f, 19.758f, 13.06f, 19.954f),
                PathNode.LineTo(13.696f, 20.59f),
                PathNode.CurveTo(13.892f, 20.785f, 14.208f, 20.785f, 14.404f, 20.59f),
                PathNode.LineTo(14.722f, 20.272f),
                PathNode.LineTo(22.322f, 12.672f),
                PathNode.CurveTo(22.693f, 12.301f, 22.693f, 11.699f, 22.322f, 11.328f),
                PathNode.LineTo(14.722f, 3.728f),
                PathNode.LineTo(14.404f, 3.41f),
                PathNode.CurveTo(14.208f, 3.215f, 13.892f, 3.215f, 13.696f, 3.41f),
                PathNode.LineTo(13.06f, 4.046f),
                PathNode.CurveTo(12.865f, 4.242f, 12.865f, 4.558f, 13.06f, 4.753f),
                PathNode.LineTo(13.378f, 5.072f),
                PathNode.LineTo(19.357f, 11.05f),
                PathNode.LineTo(2.35f, 11.05f),
                PathNode.LineTo(1.9f, 11.05f),
                PathNode.CurveTo(1.624f, 11.05f, 1.4f, 11.274f, 1.4f, 11.55f),
                PathNode.LineTo(1.4f, 12.45f),
                PathNode.CurveTo(1.4f, 12.726f, 1.624f, 12.95f, 1.9f, 12.95f),
                PathNode.LineTo(2.35f, 12.95f),
                PathNode.LineTo(19.356f, 12.95f),
                PathNode.LineTo(13.378f, 18.928f),
                PathNode.LineTo(13.06f, 19.246f),
                PathNode.Close,
            ),
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            pathFillType = PathFillType.EvenOdd,
        )
    }.build()
}

/** The inner light of `LightEffectHelper`: a `COUISpringForce` at bounce 0 and response 0.3. */
private val InnerLightSpring: SpringSpec<Float> = folmeSpring(damping = 1f, response = 0.3f)

/** The outer light appearing: `executeLightEffectAnimator(true)` sets response 0.3. */
private val OuterLightAppearSpring: SpringSpec<Float> = folmeSpring(damping = 1f, response = 0.3f)

/** The outer light disappearing: `executeLightEffectAnimator(false)` sets response 0.6. */
private val OuterLightDisappearSpring: SpringSpec<Float> = folmeSpring(damping = 1f, response = 0.6f)

/** `COUIPressFeedbackHelper`'s scale spring: response 0.3 at critical damping. */
private val NextPressSpring: SpringSpec<Float> = folmeSpring(damping = 1f, response = 0.3f)
