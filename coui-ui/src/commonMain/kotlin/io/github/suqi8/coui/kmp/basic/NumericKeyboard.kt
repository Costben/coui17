// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.anim.COUIOutEase
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * The ColorOS 17 numeric PIN pad, the port of `COUINumericKeyboard`
 * (`com.coui.appcompat.lockview`).
 *
 * A 4x3 grid of [NumericKeyboardDefaults.CellSize] (68dp) cells inside
 * [NumericKeyboardDefaults.Width] x [NumericKeyboardDefaults.Height] (248dp x 326dp) holding
 * `mKeyboardNumbers = { 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, 0, -1 }`: the first three rows are the
 * digits, and the bottom row is a side column whose left cell is [leftStyle], whose right cell is
 * [rightStyle] and whose middle cell is the digit `0` (`KEYCODE_0_ROW = 3`,
 * `KEYCODE_0_COLUMN = 1`).
 *
 * The gaps are whatever is left over, exactly as `onSizeChanged` divides it: the horizontal gap is
 * `(width - padding - 3 * cell) / 2` (22dp at the default width) and the vertical gap is
 * `(height - padding - 4 * cell) / 3` (18dp at the default height), so a centre sits at
 * `padding + cell / 2 + i * (cell + gap)`.
 *
 * The pad has no background of its own. `mNumberBackgroundColor` and `mSideBackgroundColor` come
 * from the `couiNumberBackgroundColor` / `couiSideBackgroundColor` attributes, which
 * `Widget.COUI.COUINumericKeyboard` never sets, so both stay transparent and the pad is meant to sit
 * on a wallpaper. What is drawn is the digit — [NumericKeyboardDefaults.NumberTextSize] (32dp
 * `number_keyboard_number_size`) in [NumericKeyboardColors.numberColor], raised by
 * [NumericKeyboardDefaults.NumberOffsetY] — and the press feedback of `drawPressCircle`: a circle the
 * width of a cell, tinted with `couiNumPressColor`, that pops from scale 1 to
 * [NumericKeyboardDefaults.PressPeakScale] over [NumericKeyboardDefaults.PressShowDurationMillis]
 * while fading in to `couiCircleMaxAlpha`, then grows to [NumericKeyboardDefaults.PressEndScale] and
 * fades out over [NumericKeyboardDefaults.PressFadeDurationMillis] (`initShowAnimator`,
 * `initFadeAnimator`).
 *
 * The digit is placed with the native's own baseline arithmetic — `cy - (descent + ascent) / 2 -
 * mNumberOffsetY` of `drawCell` — which is the same as centring the line box and then removing the
 * offset.
 *
 * Not ported:
 * - `couiPressEffect = LIGHT_EFFECT`: the `LightEffectHelper` spot light, the two `RadialGradient`s
 *   of `initRadialGradient`, the `InnerShadowHelper` bitmap of `initInnerShadowBitmap`, the
 *   `coui_numeric_keyboard_outer_gradient_*` / `_inner_gradient_*` / `_upper_inner_shadow_color` /
 *   `_lower_inner_shadow_color` palette they paint with, the 124dp
 *   `coui_numeric_light_shader_radius`, and the 0.6dp `coui_keyboard_button_border_width` stroke of
 *   `drawInnerBorder` — all of it sits behind
 *   `UIUtil.confirmLevelAnim(ANIM_LEVEL_SUPPORT_BLUR_MIN)`, and commonMain has neither the blur nor
 *   the light-effect shader. Only the `RIPPLE_EFFECT` style is ported, so
 *   [NumericKeyboardColors.borderColor], [NumericKeyboardColors.borderHighlightColor] and
 *   [NumericKeyboardColors.lineColor] are recovered but never drawn;
 * - `KeyboardDrawDelegate.getCustomKeyboardPaint`, the hook the app uses to replace the digit with its
 *   own paints;
 * - the variable font of `getTypeface`: `string/ttf_path` is
 *   `/system/fonts/SysSans-En-Regular.ttf` and the weight axis is
 *   `integer/font_variation_default_plus` (200, -50 from v35), which commonMain cannot load. The
 *   digits use the theme font at [NumericKeyboardDefaults.NumberTextSize];
 * - `array/coui_number_keyboard_letters` (`abc`, `def`, ...): the constructor reads it into
 *   `Cell.cellLettersStr`, which no draw path of this class consumes. [NumericKeyboardDefaults.Letters]
 *   records the values;
 * - `PatternExploreByTouchHelper` and the `coui_number_keyboard_delete` /
 *   `coui_numeric_keyboard_sure` descriptions it reads;
 * - the blurred companion of the press circle, `coui_number_keyboard_blur_circle` and the `blurAlpha`
 *   / `blurScale` half of `initFadeAnimator`: it is a blurred copy of the same circle, and a canvas in
 *   commonMain cannot blur a draw call;
 * - the multi-pointer bookkeeping (`pointerId`, `isMultiPointerEvent`, `findCellByPointerId`) and the
 *   key-event path (`onKeyDown` / `onKeyUp` / `handleKeyEvent`), which only replay hardware keys the
 *   app already handles;
 * - the haptics of `setTouchFeedback` and the `playSoundEffect` of `setTouchSoundFeedBack`;
 * - `getEnterAnim`'s per-cell `cellNumberAlpha` / `cellNumberTranslateY` stagger;
 * - `setEnabled`, which only clears the press paint (`mDownState`); `needFadeWhenDisabled` is defined
 *   but never called, so a disabled pad here simply stops responding.
 *
 * @param onNumberClick Called with the digit of the cell that was released, `0..9`
 *   (`OnClickItemListener.onClickNumber`).
 * @param modifier The modifier to be applied to the pad.
 * @param leftStyle The content of the bottom-left cell, or null for an empty cell (`setLeftStyle`).
 *   `Delete` and `Finish` tell the keyboard which cell carries which action, as `getDeleteCellIndex`
 *   / `getFinishCellIndex` derive from the type.
 * @param rightStyle The content of the bottom-right cell, or null for an empty cell
 *   (`setRightStyle`).
 * @param colors The digit, press and side colours.
 * @param enabled Whether the pad reacts to pointers.
 * @param onLeftClick Called when the left side cell is released (`OnClickItemListener.onClickLeft`).
 * @param onRightClick Called when the right side cell is released (`OnClickItemListener.onClickRight`).
 */
@Composable
fun NumericKeyboard(
    onNumberClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    leftStyle: NumericKeyboardSideStyle? = null,
    rightStyle: NumericKeyboardSideStyle? = null,
    colors: NumericKeyboardColors = NumericKeyboardDefaults.colors(),
    enabled: Boolean = true,
    onLeftClick: () -> Unit = {},
    onRightClick: () -> Unit = {},
) {
    val currentOnNumberClick by rememberUpdatedState(onNumberClick)
    val currentOnLeftClick by rememberUpdatedState(onLeftClick)
    val currentOnRightClick by rememberUpdatedState(onRightClick)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()

    val horizontalPaddingPx = with(density) { NumericKeyboardDefaults.HorizontalPadding.toPx() }
    val verticalPaddingPx = with(density) { NumericKeyboardDefaults.VerticalPadding.toPx() }
    val cellSizePx = with(density) { NumericKeyboardDefaults.CellSize.toPx() }
    val pressableAreaPx = with(density) { NumericKeyboardDefaults.AdditionalPressableArea.toPx() }
    val numberOffsetYPx = with(density) { NumericKeyboardDefaults.NumberOffsetY.toPx() }

    val presses = remember { List(NumericKeyboardDefaults.CellCount) { NumericKeyboardPress() } }
    var nodeSize by remember { mutableStateOf(IntSize.Zero) }
    var pressedIndex by remember { mutableIntStateOf(-1) }

    // SideStyle.mAlpha is driven by a COUISpringForce at response 0.2 / bounce 0 in
    // ensureRightStyleAnimator and showSideStyle, where 0 is "not shown" and 1 is "shown".
    val leftAlpha by animateFloatAsState(
        targetValue = if (leftStyle == null) 0f else 1f,
        animationSpec = NumericKeyboardDefaults.SideAlphaSpring,
        label = "numericKeyboardLeftAlpha",
    )
    val rightAlpha by animateFloatAsState(
        targetValue = if (rightStyle == null) 0f else 1f,
        animationSpec = NumericKeyboardDefaults.SideAlphaSpring,
        label = "numericKeyboardRightAlpha",
    )

    val geometry = remember(nodeSize, horizontalPaddingPx, verticalPaddingPx, cellSizePx, pressableAreaPx) {
        if (nodeSize == IntSize.Zero) {
            null
        } else {
            NumericKeyboardGeometry(
                size = nodeSize,
                cellSize = cellSizePx,
                paddingLeft = horizontalPaddingPx,
                paddingTop = verticalPaddingPx,
                additionalPressableArea = pressableAreaPx,
            )
        }
    }

    val numberStyle = TextStyle(
        color = colors.numberColor,
        fontSize = NumericKeyboardDefaults.NumberTextSize,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
    )
    val sideTextStyle = TextStyle(
        color = colors.sideTextColor,
        fontSize = NumericKeyboardDefaults.FinishTextSize,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    val numberLayouts = remember(measurer, numberStyle) {
        NumericKeyboardDefaults.Numbers.map { number ->
            if (number < 0) null else measurer.measure(number.toString(), numberStyle)
        }
    }
    val leftTextLayout = leftStyle?.text?.let { remember(it, measurer, sideTextStyle) { measurer.measure(it, sideTextStyle) } }
    val rightTextLayout = rightStyle?.text?.let { remember(it, measurer, sideTextStyle) { measurer.measure(it, sideTextStyle) } }
    val leftIconPainter = leftStyle?.icon?.let { rememberVectorPainter(it) }
    val rightIconPainter = rightStyle?.icon?.let { rememberVectorPainter(it) }

    fun press(index: Int) {
        val state = presses[index]
        pressedIndex = index
        scope.launch {
            state.show.snapTo(0f)
            state.show.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = NumericKeyboardDefaults.PressShowDurationMillis,
                    easing = COUIOutEase,
                ),
            )
        }
    }

    fun release(index: Int) {
        val state = presses[index]
        if (pressedIndex == index) pressedIndex = -1
        scope.launch {
            state.fade.snapTo(0f)
            state.fade.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = NumericKeyboardDefaults.PressFadeDurationMillis,
                    easing = NumericKeyboardPathEase,
                ),
            )
        }
    }

    fun dispatch(index: Int) {
        when (index) {
            9 -> currentOnLeftClick()
            10 -> currentOnNumberClick(0)
            11 -> currentOnRightClick()
            else -> currentOnNumberClick(index + 1)
        }
    }

    Box(
        modifier = modifier
            .size(NumericKeyboardDefaults.Width, NumericKeyboardDefaults.Height)
            .clipToBounds()
            .onSizeChanged { nodeSize = it }
            .pointerInput(enabled, geometry) {
                if (!enabled || geometry == null) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downIndex = geometry.indexAt(down.position.x, down.position.y)
                    if (downIndex >= 0) press(downIndex)
                    down.consume()
                    var released = -1
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.pressed) {
                            change.consume()
                        } else {
                            released = geometry.indexAt(change.position.x, change.position.y)
                            change.consume()
                            break
                        }
                    }
                    val pressed = pressedIndex
                    if (pressed >= 0) release(pressed)
                    // handleActionUp only dispatches when the pointer is still over the cell it
                    // started on, and getTouchIndex refuses the two empty side cells.
                    if (released >= 0 && released == downIndex) dispatch(released)
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val geo = geometry ?: return@Canvas
            for (row in 0 until NumericKeyboardDefaults.RowCount) {
                for (column in 0 until NumericKeyboardDefaults.ColumnCount) {
                    val index = row * 3 + column
                    val center = geo.center(column, row)
                    val style = when (index) {
                        9 -> leftStyle
                        11 -> rightStyle
                        else -> null
                    }
                    val pressable = (index != 9 && index != 11) || style != null
                    val state = presses[index]
                    val pressed = pressedIndex == index
                    val scale: Float
                    val alpha: Float
                    if (pressed) {
                        scale = 1f + (NumericKeyboardDefaults.PressPeakScale - 1f) * state.show.value
                        alpha = colors.circleMaxAlpha * state.show.value
                    } else {
                        scale = NumericKeyboardDefaults.PressPeakScale +
                            (NumericKeyboardDefaults.PressEndScale - NumericKeyboardDefaults.PressPeakScale) * state.fade.value
                        alpha = colors.circleMaxAlpha * (1f - state.fade.value)
                    }
                    if (pressable && alpha > 0f) {
                        drawCircle(
                            color = colors.pressColor.copy(alpha = colors.pressColor.alpha * alpha),
                            radius = cellSizePx / 2f * scale,
                            center = center,
                        )
                    }
                    when (index) {
                        9, 11 -> drawNumericKeyboardSide(
                            style = style,
                            alpha = if (index == 9) leftAlpha else rightAlpha,
                            center = center,
                            cellSize = cellSizePx,
                            textLayout = if (index == 9) leftTextLayout else rightTextLayout,
                            iconPainter = if (index == 9) leftIconPainter else rightIconPainter,
                            colors = colors,
                        )

                        else -> {
                            val number = NumericKeyboardDefaults.Numbers[index]
                            val layout = numberLayouts[index]
                            if (number >= 0 && layout != null) {
                                drawText(
                                    textLayoutResult = layout,
                                    topLeft = Offset(
                                        x = center.x - layout.size.width / 2f,
                                        y = center.y - layout.size.height / 2f - numberOffsetYPx,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A side cell of a [NumericKeyboard], mirroring `COUINumericKeyboard.SideStyle`. */
@Immutable
data class NumericKeyboardSideStyle(
    val type: NumericKeyboardSideType = NumericKeyboardSideType.None,
    val text: String? = null,
    val icon: ImageVector? = null,
    val description: String? = null,
) {
    init {
        require(text == null || icon == null) { "a side style draws either text or an icon, never both" }
    }
}

/**
 * What a side cell does, `COUINumericKeyboard.SIDE_TYPE_*`.
 *
 * `getDeleteCellIndex` and `getFinishCellIndex` read this to answer which cell carries the delete and
 * finish actions, so a caller that wants the native split only has to label the two styles.
 */
enum class NumericKeyboardSideType {
    /** `SIDE_TYPE_NONE`, 0, the default. */
    None,

    /** `SIDE_TYPE_DELETE`, 1. */
    Delete,

    /** `SIDE_TYPE_FINISH`, 2. */
    Finish,
}

/**
 * The colours of a [NumericKeyboard] — the attributes `Widget.COUI.COUINumericKeyboard` resolves
 * through `R.styleable.COUINumericKeyboard`.
 *
 * @param numberColor The digits. `couiNumberColor` -> `coui_numeric_keyboard_number_color`.
 * @param pressColor The press circle. `couiNumPressColor` ->
 *   `coui_numeric_keyboard_number_press_color`, which the `Cell` constructor also tints
 *   `coui_number_keyboard_normal_circle` with.
 * @param sideTextColor The label of a side cell. `couiWordTextNormalColor` ->
 *   `coui_numeric_keyboard_word_text_normal_color`.
 * @param lineColor `couiLineColor` -> `coui_numeric_keyboard_line_color`, set on the `mLinePaint`
 *   that no draw path of the class uses; transparent in both themes.
 * @param borderColor `coui_numeric_keyboard_border_color`, the 0.6dp `drawInnerBorder` stroke of the
 *   light-effect press style.
 * @param borderHighlightColor `coui_numeric_keyboard_border_highlight_color`, the highlight half of
 *   that stroke.
 * @param sideBackgroundColor `couiSideBackgroundColor`, the circle behind a side cell. The style
 *   never sets it, so it is transparent.
 * @param circleMaxAlpha The press circle's peak alpha. `couiCircleMaxAlpha` — the base style writes
 *   0.12 literally, `.Dark` uses `coui_numeric_keyboard_dark_circle_max_alpha` (0.06, 0.12 at night)
 *   and `.Dark.Dark` uses `coui_numeric_keyboard_dark_circle_max_dark_alpha` (0.12).
 */
@Immutable
data class NumericKeyboardColors(
    val numberColor: Color,
    val pressColor: Color,
    val sideTextColor: Color,
    val lineColor: Color,
    val borderColor: Color,
    val borderHighlightColor: Color,
    val sideBackgroundColor: Color,
    val circleMaxAlpha: Float,
)

/** Contains the default values used by [NumericKeyboard]. */
object NumericKeyboardDefaults {

    /** `COUINumericKeyboard.CELL_ROW_COUNT`, 4. */
    val RowCount = 4

    /** `COUINumericKeyboard.CELL_COLUMN_COUNT`, 3. */
    val ColumnCount = 3

    /** The number of cells, `4 * 3`. */
    val CellCount = 12

    /** The default width. COUI `coui_numeric_keyboard_view_width`, 248dp. */
    val Width = 248.dp

    /** The default height. COUI `coui_numeric_keyboard_view_height`, 326dp. */
    val Height = 326.dp

    /**
     * The side of one cell. COUI `coui_numeric_keyboard_view_size`, read into `mViewSize` and split
     * into `mCellWidth` / `mCellHeight` / `mNumberBackgroundRadius` / `mCircleRadius`, 68dp.
     */
    val CellSize = 68.dp

    /**
     * The horizontal padding, `android:paddingStart` / `android:paddingEnd` of
     * `Widget.COUI.COUINumericKeyboard`. COUI `coui_numeric_keyboard_view_horizontal_padding`, 0dp.
     */
    val HorizontalPadding = 0.dp

    /** The vertical padding. COUI `coui_numeric_keyboard_view_vertical_padding`, 0dp. */
    val VerticalPadding = 0.dp

    /**
     * The upward shift of the digits. COUI `coui_numeric_keyboard_number_offset_y`, read into
     * `mNumberOffsetY`, 1dp.
     */
    val NumberOffsetY = 1.dp

    /**
     * How far outside a cell a pointer still hits it, clamped to half the gap. COUI
     * `coui_additional_pressable_area`, read into `mAdditionalPressableArea`, 32dp.
     */
    val AdditionalPressableArea = 32.dp

    /**
     * The size of a digit. COUI `number_keyboard_number_size`, the fallback of `couiNumberTextSize`,
     * 32dp. The native reads it as pixels and so ignores the font scale; the port uses sp.
     */
    val NumberTextSize = 32.sp

    /**
     * The size of the default finish label. COUI `coui_number_keyboard_finish_text_size`, 16dp, read
     * as pixels by the native and in sp here.
     */
    val FinishTextSize = 16.sp

    /**
     * The label of the default finish style. COUI `string/coui_numeric_keyboard_sure`, `"Done"`
     * (`"完成"` in zh-rCN).
     */
    val FinishText = "Done"

    /**
     * The description of the default delete style. COUI `string/coui_number_keyboard_delete`,
     * `"Delete"` (`"删除"` in zh-rCN).
     */
    val DeleteText = "Delete"

    /** The scale the press circle pops to. `initShowAnimator`, `SHOW_END_SCALE`, 2.15. */
    val PressPeakScale = 2.15f

    /** The scale the press circle leaves at. `initFadeAnimator`'s `FADE_END_SCALE`, 2.5. */
    val PressEndScale = 2.5f

    /** The duration of the press circle's pop. `initShowAnimator`, `SHOW_ANIMATOR_TIME`, 100ms. */
    val PressShowDurationMillis = 100

    /** The duration of the press circle's fade. `initFadeAnimator`, `FADE_ANIMATOR_TIME`, 160ms. */
    val PressFadeDurationMillis = 160

    /**
     * The digits of the grid, `COUINumericKeyboard.mKeyboardNumbers`. -1 is a cell with no digit: the
     * two side cells of the bottom row.
     */
    val Numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, -1, 0, -1)

    /**
     * `array/coui_number_keyboard_letters`, index-aligned with [Numbers]. The constructor reads them
     * into `Cell.cellLettersStr`, which no draw path of this class consumes.
     */
    val Letters = listOf("", "", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "")

    /** The digit colour of the base style. `coui_numeric_keyboard_number_color`, `#ffffffff`. */
    val NumberColor = Color(0xFFFFFFFF)

    /** The press colour of the base style. `coui_numeric_keyboard_number_press_color`, `#ffffffff`. */
    val PressColor = Color(0xFFFFFFFF)

    /**
     * The side label colour of the base style. `coui_numeric_keyboard_word_text_normal_color`,
     * `#ffffffff`; `.Dark` uses `?attr/couiColorHintNeutral` instead.
     */
    val SideTextColor = Color(0xFFFFFFFF)

    /** The line colour. `coui_numeric_keyboard_line_color`, `#00ffffff` — fully transparent. */
    val LineColor = Color(0x00FFFFFF)

    /** The border colour. `coui_numeric_keyboard_border_color`, `#0dffffff`. */
    val BorderColor = Color(0x0DFFFFFF)

    /** The highlight border colour. `coui_numeric_keyboard_border_highlight_color`, `#ccffffff`. */
    val BorderHighlightColor = Color(0xCCFFFFFF)

    /** The side circle colour. `couiSideBackgroundColor` is unset by the style, so transparent. */
    val SideBackgroundColor = Color(0x00000000)

    /** `couiCircleMaxAlpha` in `Widget.COUI.COUINumericKeyboard`, written literally as 0.12. */
    val CircleMaxAlpha = 0.12f

    /** `coui_numeric_keyboard_dark_circle_max_alpha`, the `.Dark` style's peak (0.12 at night). */
    val DarkCircleMaxAlpha = 0.06f

    /** `coui_numeric_keyboard_dark_circle_max_dark_alpha`, the `.Dark.Dark` style's peak. */
    val DarkDarkCircleMaxAlpha = 0.12f

    /** The Dark style's digit colour in a light theme. `coui_numeric_keyboard_dark_number_color`, `#ff000000`. */
    val DarkNumberColorLight = Color(0xFF000000)

    /** The `.Dark.Dark` digit colour. `coui_color_label_primary_dark`, `#e6ffffff`. */
    val DarkNumberColorDark = Color(0xE6FFFFFF)

    /** The Dark style's press colour in a light theme. `coui_numeric_keyboard_dark_number_press_color`, `#ff000000`. */
    val DarkPressColorLight = Color(0xFF000000)

    /** The Dark style's press colour at night. `coui_numeric_keyboard_dark_number_press_color` (night), `#ffffffff`. */
    val DarkPressColorDark = Color(0xFFFFFFFF)

    /** The Dark style's line colour in a light theme. `coui_numeric_keyboard_dark_line_color`, `#00000000`. */
    val DarkLineColorLight = Color(0x00000000)

    /** The Dark style's line colour at night. `coui_numeric_keyboard_dark_line_color` (night), `#00ffffff`. */
    val DarkLineColorDark = Color(0x00FFFFFF)

    /** The Dark style's letter colour in a light theme. `coui_numeric_keyboard_dark_letter_color`, `#59000000`. */
    val DarkLetterColorLight = Color(0x59000000)

    /** The Dark style's letter colour at night. `coui_numeric_keyboard_dark_letter_color` (night), `#59ffffff`. */
    val DarkLetterColorDark = Color(0x59FFFFFF)

    /**
     * The Dark style's side label colour. `coui_numeric_keyboard_dark_word_text_normal_color`,
     * `#ff2ad181` — the green "Done" label of the light-theme PIN pad.
     */
    val DarkWordTextNormalColor = Color(0xFF2AD181)

    /**
     * `coui_numeric_keyboard_letter_color`, `#b3ffffff`: what the style maps `couiLetterColor` to,
     * which `COUINumericKeyboard` never reads because it never draws the letters.
     */
    val LetterColor = Color(0xB3FFFFFF)

    /** `coui_numeric_keyboard_word_text_press_color`, `#4dffffff`: the pressed side label colour. */
    val WordTextPressColor = Color(0x4DFFFFFF)

    /** `coui_numeric_keyboard_outer_gradient_color_1`, `#80ffffff`: the light-effect outer gradient. */
    val OuterGradientColor1 = Color(0x80FFFFFF)

    /** `coui_numeric_keyboard_outer_gradient_color_2`, `#33ffffff`. */
    val OuterGradientColor2 = Color(0x33FFFFFF)

    /** `coui_numeric_keyboard_outer_gradient_color_3`, `#0dffffff`. */
    val OuterGradientColor3 = Color(0x0DFFFFFF)

    /** `coui_numeric_keyboard_inner_gradient_color_1`, `#8fffffff`. */
    val InnerGradientColor1 = Color(0x8FFFFFFF)

    /** `coui_numeric_keyboard_inner_gradient_color_2`, `#e6ffffff`. */
    val InnerGradientColor2 = Color(0xE6FFFFFF)

    /** `coui_numeric_keyboard_upper_inner_shadow_color`, `#5cffffff`. */
    val UpperInnerShadowColor = Color(0x5CFFFFFF)

    /** `coui_numeric_keyboard_lower_inner_shadow_color`, `#33ffffff`. */
    val LowerInnerShadowColor = Color(0x33FFFFFF)

    /**
     * The side-style alpha spring of `ensureRightStyleAnimator`: `COUISpringForce` at response 0.2 and
     * bounce 0.
     */
    val SideAlphaSpring = folmeSpring<Float>(damping = 1f, response = 0.2f)

    /**
     * The colours of `Widget.COUI.COUINumericKeyboard`, the style `COUINumericKeyboard` falls back to
     * when the theme does not point `couiNumericKeyboardStyle` elsewhere.
     *
     * It is a "dark surface" palette — white digits meant to sit over a wallpaper — and the native
     * never switches it on the theme, so the port does not either. [darkColors] carries the
     * `Widget.COUI.COUINumericKeyboard.Dark` palette for callers that need it.
     */
    @Composable
    fun colors(): NumericKeyboardColors = NumericKeyboardColors(
        numberColor = NumberColor,
        pressColor = PressColor,
        sideTextColor = SideTextColor,
        lineColor = LineColor,
        borderColor = BorderColor,
        borderHighlightColor = BorderHighlightColor,
        sideBackgroundColor = SideBackgroundColor,
        circleMaxAlpha = CircleMaxAlpha,
    )

    /**
     * The palette of `Widget.COUI.COUINumericKeyboard.Dark`, which the native only reaches when the
     * theme resolves `couiNumericKeyboardStyle` to it.
     *
     * The style's `_dark_` colour family is day/night qualified, so the port branches on the theme's
     * surface luminance to pick the same side the native resource qualifier would.
     */
    @Composable
    fun darkColors(): NumericKeyboardColors {
        val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
        return NumericKeyboardColors(
            numberColor = if (isDark) DarkNumberColorDark else DarkNumberColorLight,
            pressColor = if (isDark) DarkPressColorDark else DarkPressColorLight,
            sideTextColor = DarkWordTextNormalColor,
            lineColor = if (isDark) DarkLineColorDark else DarkLineColorLight,
            borderColor = BorderColor,
            borderHighlightColor = BorderHighlightColor,
            sideBackgroundColor = SideBackgroundColor,
            circleMaxAlpha = if (isDark) DarkDarkCircleMaxAlpha else DarkCircleMaxAlpha,
        )
    }

    /**
     * The default finish side style of the constructor's `mFinishStyle`: the
     * `string/coui_numeric_keyboard_sure` label in `coui_numeric_keyboard_word_text_normal_color` at
     * `coui_number_keyboard_finish_text_size`.
     */
    fun finishSideStyle(): NumericKeyboardSideStyle = NumericKeyboardSideStyle(
        type = NumericKeyboardSideType.Finish,
        text = FinishText,
        description = FinishText,
    )

    /**
     * The default delete side style of the constructor's `mDeleteStyle`:
     * `ic_coui_number_keyboard_launhcer_delete`, a 25dp x 14dp backspace glyph, described by
     * `string/coui_number_keyboard_delete`.
     *
     * The native tints it with `couiNumberColor`; the port draws the caller's icon with
     * [NumericKeyboardColors.numberColor] as the tint.
     *
     * @param icon The backspace icon to draw.
     */
    fun deleteSideStyle(icon: ImageVector): NumericKeyboardSideStyle = NumericKeyboardSideStyle(
        type = NumericKeyboardSideType.Delete,
        icon = icon,
        description = DeleteText,
    )
}

/** The `PathInterpolator(0, 0, 0.6f, 1f)` that `COUINumericKeyboard.PATH_INTERPOLATOR` carries. */
private val NumericKeyboardPathEase: Easing = CubicBezierEasing(0f, 0f, 0.6f, 1f)

/**
 * The two `Animatable`s of one cell's press feedback: `showAnimator`'s scale/alpha ramp and
 * `fadeAnimator`'s release.
 */
private class NumericKeyboardPress {
    val show = Animatable(0f)
    val fade = Animatable(1f)
}

/**
 * The cell geometry of `COUINumericKeyboard`: `mCellWidth` / `mCellHeight` / `mHorizontalSpacing` /
 * `mVerticalSpacing` from `onSizeChanged`, and `getCenterXForColumn` / `getCenterYForRow` /
 * `getColumnHit` / `getRowHit`.
 *
 * @param size The measured size of the pad.
 * @param cellSize The side of one cell, in pixels.
 * @param paddingLeft The horizontal padding, in pixels.
 * @param paddingTop The vertical padding, in pixels.
 * @param additionalPressableArea The extra hit area of `coui_additional_pressable_area`, in pixels.
 */
private class NumericKeyboardGeometry(
    size: IntSize,
    private val cellSize: Float,
    private val paddingLeft: Float,
    private val paddingTop: Float,
    private val additionalPressableArea: Float,
) {
    private val horizontalSpacing =
        ((size.width - paddingLeft * 2f - cellSize * 3f) / 2f).coerceAtLeast(0f)
    private val verticalSpacing =
        ((size.height - paddingTop * 2f - cellSize * 4f) / 3f).coerceAtLeast(0f)

    private fun centerX(column: Int): Float = paddingLeft + cellSize / 2f + column * (cellSize + horizontalSpacing)

    private fun centerY(row: Int): Float = paddingTop + cellSize / 2f + row * (cellSize + verticalSpacing)

    fun center(column: Int, row: Int): Offset = Offset(centerX(column), centerY(row))

    /** `checkForNewHit`: the cell index, or -1 when the position is outside the grid. */
    fun indexAt(x: Float, y: Float): Int {
        val extraY = max(0f, min(verticalSpacing / 2f, additionalPressableArea))
        val row = (0..3).firstOrNull { index ->
            val center = centerY(index)
            y in max(center - cellSize / 2f - extraY, 0f)..(center + cellSize / 2f + extraY)
        } ?: return -1
        val extraX = max(0f, min(horizontalSpacing / 2f, additionalPressableArea))
        val column = (0..2).firstOrNull { index ->
            val center = centerX(index)
            x in max(center - cellSize / 2f - extraX, 0f)..(center + cellSize / 2f + extraX)
        } ?: return -1
        return row * 3 + column
    }
}

/** `drawSide`: the side circle, then the label or the icon of a bottom-row side cell. */
private fun DrawScope.drawNumericKeyboardSide(
    style: NumericKeyboardSideStyle?,
    alpha: Float,
    center: Offset,
    cellSize: Float,
    textLayout: TextLayoutResult?,
    iconPainter: Painter?,
    colors: NumericKeyboardColors,
) {
    if (style == null || alpha <= 0f) return
    if (colors.sideBackgroundColor.alpha > 0f) {
        drawCircle(
            color = colors.sideBackgroundColor.copy(alpha = colors.sideBackgroundColor.alpha * alpha),
            radius = cellSize / 2f,
            center = center,
        )
    }
    if (iconPainter != null) {
        val size = iconPainter.intrinsicSize
        if (size.isSpecified && size.width > 0f && size.height > 0f) {
            translate(left = center.x - size.width / 2f, top = center.y - size.height / 2f) {
                with(iconPainter) {
                    draw(
                        size = Size(size.width, size.height),
                        alpha = alpha,
                        colorFilter = ColorFilter.tint(colors.numberColor),
                    )
                }
            }
        }
        return
    }
    if (textLayout != null) {
        drawText(
            textLayoutResult = textLayout,
            color = colors.sideTextColor.copy(alpha = colors.sideTextColor.alpha * alpha),
            topLeft = Offset(
                x = center.x - textLayout.size.width / 2f,
                y = center.y - textLayout.size.height / 2f,
            ),
        )
    }
}
