// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.anim.COUIMoveEase
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch

/**
 * The ColorOS 17 A–Z index rail (`COUITouchSearchView`,
 * `com/coui/appcompat/touchsearchview/COUITouchSearchView`), the vertical alphabet strip pinned to
 * the trailing edge of a contact / settings list. Dragging a finger up and down the rail selects the
 * letter under it, and a magnified tile showing the current letter floats beside the rail.
 *
 * Geometry follows the native `updateKeys` + `onDraw`: each key occupies a
 * `coui_touchsearch_each_item_height` (14dp) cell plus `coui_touchsearch_item_spacing` (2dp), so the
 * stride is 16dp; the rail is `coui_touchsearch_background_width` (39dp) wide and is padded
 * `coui_touchsearch_padding_top` / `coui_touchsearch_padding_bottom` (16dp) above the first and
 * below the last key. Letters are `coui_touchsearch_key_textsize` (12dp) and are drawn horizontally
 * and vertically centred in their cell, which is where the native `onDraw` puts its baseline
 * (`key.top + (touchBottom - top) / 2 - (descent + ascent) / 2`).
 *
 * The magnified tile is the native first-level popup: a
 * `coui_touchsearch_popup_first_default_width` x `coui_touchsearch_popup_first_default_height`
 * (48dp x 48dp) tile whose background is `drawable/coui_touch_search_popup_bg` — a `shape` with a
 * single `<solid>` and no `<corners>`, i.e. a **square** tile filled with
 * `?attr/couiColorBackgroundTop` (`coui_color_background_top`, `#ffffffff` / `#ff333333`) — holding
 * the letter at `coui_touchsearch_popupwin_first_textsize` (20sp) in
 * `coui_touchsearch_popup_text_color` (`#d9000000` / `#d9ffffff`). Like the native popup window,
 * which is positioned at `locationInWindow[0] - mPopupWindowEndMargin - mPopupFirstWidth`, the tile
 * is drawn *outside* the rail's own bounds, `coui_touchsearch_popupwin_right_margin` (0.5dp) to the
 * start of the rail and centred on the pressed key — give the rail room on its start side (or put it
 * last in a non-clipping parent) so the tile stays visible.
 *
 * @param keys The index keys, top to bottom. Use [TouchSearchViewDefaults.NormalKeys] or
 *   [TouchSearchViewDefaults.SpecialKeys] for the two arrays ColorOS ships
 *   (`array/normal_touchsearch_keys`, `array/special_touchsearch_keys`).
 * @param onKeySelected Called with the key under the finger whenever it changes, including the first
 *   press. The native equivalent is `COUITouchSearchView.TouchSearchActionListener`.
 * @param modifier The modifier to be applied to the rail. The rail sizes itself to
 *   [TouchSearchViewDefaults.BackgroundWidth] x `PaddingTop + keys * stride + PaddingBottom`; wrap it
 *   in a sized parent (for example at [TouchSearchViewDefaults.DefaultHeight], 432dp) to make it
 *   shorter than its content.
 * @param enabled Whether the rail reacts to touch.
 * @param hapticFeedbackEnabled Whether a key change fires haptic feedback. Native default: true
 *   (`couiAdaptiveVibrator`, `attr/couiTouchSearchVibrateLevel` = 0), fired through
 *   `VibrateUtils`.
 * @param colors The [TouchSearchViewColors].
 * @param sectionNames The names listed beside the magnified tile while a key is held, the
 *   `COUITouchSearchView.setName(String[])` second popup: keyed by rail key, for example `#` to the
 *   contacts whose name starts with a non-letter. An empty map (the default) leaves the second
 *   popup out, as the native view does until the caller supplies names.
 */
@Composable
fun TouchSearchView(
    keys: List<String>,
    onKeySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hapticFeedbackEnabled: Boolean = true,
    colors: TouchSearchViewColors = TouchSearchViewDefaults.colors(),
    sectionNames: Map<String, List<String>> = emptyMap(),
) {
    if (keys.isEmpty()) return

    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()

    val stride = TouchSearchViewDefaults.EachItemHeight + TouchSearchViewDefaults.ItemSpacing
    val railHeight = TouchSearchViewDefaults.PaddingTop + stride * keys.size + TouchSearchViewDefaults.PaddingBottom
    val stridePx = with(density) { stride.toPx() }
    val paddingTopPx = with(density) { TouchSearchViewDefaults.PaddingTop.toPx() }
    val popupSizePx = with(density) { TouchSearchViewDefaults.PopupSize.toPx() }
    val popupEndMarginPx = with(density) { TouchSearchViewDefaults.PopupEndMargin.toPx() }
    val sectionPopupWidthPx = with(density) { TouchSearchViewDefaults.SecondPopupWidth.toPx() }
    val sectionRowHeightPx = with(density) { TouchSearchViewDefaults.SecondRowHeight.toPx() }
    val sectionMaxHeightPx = with(density) { TouchSearchViewDefaults.SecondPopupMaxHeight.toPx() }
    val sectionMarginPx = with(density) { TouchSearchViewDefaults.SecondPopupMargin.toPx() }
    val sectionOffsetPx = with(density) { TouchSearchViewDefaults.SecondPopupOffset.toPx() }
    val sectionCornerPx = with(density) { TouchSearchViewDefaults.SecondPopupCornerRadius.toPx() }

    val keyStyle = remember(colors.textColor) {
        TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.textColor)
    }
    val pressedKeyStyle = remember(colors.pressedTextColor) {
        TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.pressedTextColor)
    }
    val popupStyle = remember(colors.popupTextColor) {
        TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, color = colors.popupTextColor)
    }
    val sectionNameStyle = remember(colors.secondPopupTextColor) {
        TextStyle(fontSize = TouchSearchViewDefaults.SecondTextSize, color = colors.secondPopupTextColor)
    }

    val keyLayouts = remember(textMeasurer, keys, keyStyle) { keys.map { textMeasurer.measure(it, keyStyle) } }
    val pressedKeyLayouts = remember(textMeasurer, keys, pressedKeyStyle) {
        keys.map { textMeasurer.measure(it, pressedKeyStyle) }
    }
    val popupLayouts = remember(textMeasurer, keys, popupStyle) { keys.map { textMeasurer.measure(it, popupStyle) } }
    val sectionNameLayouts = remember(textMeasurer, sectionNames, sectionNameStyle) {
        sectionNames.mapValues { (_, names) -> names.map { textMeasurer.measure(it, sectionNameStyle) } }
    }

    var pressedIndex by remember(keys) { mutableStateOf(-1) }
    // The popup keeps its letter while it fades out, the way the native popup window stays on screen
    // until its disappear animation has run to the end (`onAnimationEnd` dismisses it).
    var popupIndex by remember(keys) { mutableIntStateOf(-1) }
    val onKeySelectedState by rememberUpdatedState(onKeySelected)

    // `initPopupWindowAnimator`: appear runs alpha 0 -> 1 and scale 0.8 -> 1 over 350ms, disappear
    // runs the same pair back over 300ms, both on `COUIMoveEaseInterpolator`. The native starts the
    // appear animation once per popup and only re-reads the letter while it is running, and it
    // cancels an in-flight animation when the finger goes the other way; both fall out of keying the
    // effect on visibility instead of on the letter.
    val popupAlpha = remember { Animatable(0f) }
    val popupScale = remember { Animatable(TouchSearchViewDefaults.PopupScaleMin) }
    val popupVisible = pressedIndex >= 0
    LaunchedEffect(popupVisible) {
        val spec = tween<Float>(
            durationMillis = if (popupVisible) {
                TouchSearchViewDefaults.PopupAppearDurationMillis
            } else {
                TouchSearchViewDefaults.PopupDisappearDurationMillis
            },
            easing = COUIMoveEase,
        )
        launch { popupAlpha.animateTo(if (popupVisible) 1f else 0f, spec) }
        popupScale.animateTo(
            if (popupVisible) 1f else TouchSearchViewDefaults.PopupScaleMin,
            spec,
        )
    }

    fun selectAt(y: Float) {
        val index = ((y - paddingTopPx) / stridePx).toInt().coerceIn(0, keys.lastIndex)
        if (index == pressedIndex) return
        pressedIndex = index
        popupIndex = index
        onKeySelectedState(keys[index])
        if (hapticFeedbackEnabled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Canvas(
        modifier = modifier
            .width(TouchSearchViewDefaults.BackgroundWidth)
            .height(railHeight)
            .then(
                if (enabled) {
                    Modifier.pointerInput(keys, stridePx, paddingTopPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            selectAt(down.position.y)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                                selectAt(change.position.y)
                                change.consume()
                            }
                            pressedIndex = -1
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        keys.forEachIndexed { index, _ ->
            val cellCenterY = paddingTopPx + index * stridePx + stridePx / 2f
            val layout = if (index == pressedIndex) pressedKeyLayouts[index] else keyLayouts[index]
            drawCenteredText(layout, cellCenterY)
        }

        val popupAlphaValue = popupAlpha.value
        if (popupAlphaValue > 0f && popupIndex in keys.indices) {
            val centerY = paddingTopPx + popupIndex * stridePx + stridePx / 2f
            val tileLeft = -popupEndMarginPx - popupSizePx
            drawMagnifiedKey(
                layout = popupLayouts[popupIndex],
                backgroundColor = colors.popupBackgroundColor,
                centerY = centerY,
                tileSizePx = popupSizePx,
                gapPx = popupEndMarginPx,
                alpha = popupAlphaValue,
                scale = popupScale.value,
            )
            // `COUITouchSearchView.setName` opens its second popup window beside the tile and
            // `closing()` dismisses it outright, so it is drawn only while a key is held.
            val names = sectionNameLayouts[keys[popupIndex]]
            if (pressedIndex >= 0 && !names.isNullOrEmpty()) {
                drawSectionNames(
                    layouts = names,
                    backgroundColor = colors.secondPopupBackgroundColor,
                    rightPx = tileLeft - sectionMarginPx,
                    centerY = centerY,
                    widthPx = sectionPopupWidthPx,
                    rowHeightPx = sectionRowHeightPx,
                    maxHeightPx = sectionMaxHeightPx,
                    offsetPx = sectionOffsetPx,
                    railHeightPx = size.height,
                    cornerRadiusPx = sectionCornerPx,
                )
            }
        }
    }
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds the sectionNames parameter.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun TouchSearchView(
    keys: List<String>,
    onKeySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hapticFeedbackEnabled: Boolean = true,
    colors: TouchSearchViewColors = TouchSearchViewDefaults.colors(),
): Unit = TouchSearchView(
    keys = keys,
    onKeySelected = onKeySelected,
    modifier = modifier,
    enabled = enabled,
    hapticFeedbackEnabled = hapticFeedbackEnabled,
    colors = colors,
    sectionNames = emptyMap(),
)

private fun DrawScope.drawCenteredText(layout: TextLayoutResult, centerY: Float) {
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            x = (size.width - layout.size.width) / 2f,
            y = centerY - layout.size.height / 2f,
        ),
    )
}

/**
 * Draws the magnified tile to the start of the rail, vertically centred on [centerY]. The tile is
 * drawn outside the rail's own bounds because the native popup is a separate window.
 *
 * [scale] is applied around the tile's own centre, which is what the native popup's
 * `setScaleX`/`setScaleY` on the popup window's content view do.
 */
private fun DrawScope.drawMagnifiedKey(
    layout: TextLayoutResult,
    backgroundColor: Color,
    centerY: Float,
    tileSizePx: Float,
    gapPx: Float,
    alpha: Float,
    scale: Float,
) {
    val left = -gapPx - tileSizePx
    withTransform({
        scale(scale, scale, pivot = Offset(left + tileSizePx / 2f, centerY))
    }) {
        drawRect(
            color = backgroundColor,
            topLeft = Offset(left, centerY - tileSizePx / 2f),
            size = Size(tileSizePx, tileSizePx),
            alpha = alpha,
        )
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(
                x = left + (tileSizePx - layout.size.width) / 2f,
                y = centerY - layout.size.height / 2f,
            ),
            alpha = alpha,
        )
    }
}

/**
 * Draws the `setName` popup: the names listed beside the magnified tile, on the start side of it.
 *
 * The native second popup is a `ScrollView` of `COUITouchSearchPopupContentItem` rows, `112dp` wide
 * (`couiPopupWinSecondWidth`) and `min(rows * rowHeight, coui_touchsearch_popupname_max_height)`
 * tall, filled with `coui_touchsearch_second_name_background` — a nine-patch tinted
 * `?attr/couiColorSecondary`. It is centred on the first tile and then kept inside the rail's own
 * span plus `coui_touchsearch_popupwin_default_offset` above and below, which is what the clamping
 * below reproduces.
 */
private fun DrawScope.drawSectionNames(
    layouts: List<TextLayoutResult>,
    backgroundColor: Color,
    rightPx: Float,
    centerY: Float,
    widthPx: Float,
    rowHeightPx: Float,
    maxHeightPx: Float,
    offsetPx: Float,
    railHeightPx: Float,
    cornerRadiusPx: Float,
) {
    val heightPx = (layouts.size * rowHeightPx).coerceAtMost(maxHeightPx)
    val left = rightPx - widthPx
    val top = (centerY - heightPx / 2f)
        .coerceIn(-offsetPx, (railHeightPx + offsetPx - heightPx).coerceAtLeast(-offsetPx))
    drawRoundRect(
        color = backgroundColor,
        topLeft = Offset(left, top),
        size = Size(widthPx, heightPx),
        cornerRadius = CornerRadius(cornerRadiusPx),
    )
    // The panel is a scroll view on the device; rows past the maximum height are clipped here.
    clipRect(left, top, left + widthPx, top + heightPx) {
        layouts.forEachIndexed { index, layout ->
            val rowCenterY = top + rowHeightPx * index + rowHeightPx / 2f
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = left + (widthPx - layout.size.width) / 2f,
                    y = rowCenterY - layout.size.height / 2f,
                ),
            )
        }
    }
}

/** Contains the default values used by [TouchSearchView]. */
object TouchSearchViewDefaults {
    /** `dimen/coui_touchsearch_background_width` = 39dp. */
    val BackgroundWidth = 39.dp

    /** `dimen/coui_touchsearch_each_item_height` = 14dp. */
    val EachItemHeight = 14.dp

    /** `dimen/coui_touchsearch_item_spacing` = 2dp. */
    val ItemSpacing = 2.dp

    /** `dimen/coui_touchsearch_padding_top` = 16dp. */
    val PaddingTop = 16.dp

    /** `dimen/coui_touchsearch_padding_bottom` = 16dp. */
    val PaddingBottom = 16.dp

    /** `dimen/coui_touchsearch_key_textsize` = 12dp; the native code reads it as a px size. */
    val KeyTextSize = 12.dp

    /** `dimen/coui_touchsearch_key_dot_textsize` = 20dp, the size of the hidden-letter dot. */
    val DotTextSize = 20.dp

    /** `dimen/coui_touchsearch_popup_first_default_width` / `_height` = 48dp. */
    val PopupSize = 48.dp

    /** `dimen/coui_touchsearch_popupwin_first_textsize` = 20sp. */
    val PopupTextSize = 20.sp

    /** `dimen/coui_touchsearch_popupwin_right_margin` = 0.5dp. */
    val PopupEndMargin = 0.5.dp

    /**
     * The `setName` popup's width, `attr/couiPopupWinSecondWidth` defaulting to
     * `coui_touchsearch_popup_first_layout_width` (112dp).
     */
    val SecondPopupWidth = 112.dp

    /**
     * One name row's height: `couiPopupWinSecondHeight` defaults to the first tile's height
     * (`coui_touchsearch_popup_first_default_height`, 48dp), and `setName` adds each row with that
     * height as its layout params. `coui_touchsearch_popup_second_text_height` (40dp) is what the
     * row style declares, but the layout params override it.
     */
    val SecondRowHeight = 48.dp

    /**
     * `dimen/coui_touchsearch_popupname_max_height` = 304dp, the tallest the name list gets before
     * it scrolls.
     */
    val SecondPopupMaxHeight = 304.dp

    /** `dimen/coui_touchsearch_popupwin_second_marginEnd` = 8dp, the gap to the first tile. */
    val SecondPopupMargin = 8.dp

    /**
     * `dimen/coui_touchsearch_popupwin_default_offset` = 23dp, how far the name list may reach above
     * and below the rail before it is pushed back inside.
     */
    val SecondPopupOffset = 23.dp

    /** `dimen/coui_touchsearch_popupwin_second_textsize` = 18sp. */
    val SecondTextSize = 18.sp

    /**
     * The corner of `coui_touchsearch_second_name_bg`: the nine-patch is a 60px square at 1.5x with
     * a 9px corner, i.e. 6dp.
     */
    val SecondPopupCornerRadius = 6.dp

    /**
     * The popup's appear / disappear durations, `initPopupWindowAnimator`'s inline
     * `setDuration(350L)` and `setDuration(300L)` (`COUIMoveEaseInterpolator`).
     */
    val PopupAppearDurationMillis = 350
    val PopupDisappearDurationMillis = 300

    /** `COUITouchSearchView.SCALE_MIN`, the scale the popup fades out from / in to. */
    val PopupScaleMin = 0.8f

    /** `dimen/coui_touchsearch_char_offset` = 0dp. */
    val CharOffset = 0.dp

    /** `dimen/coui_touchsearch_touch_padding_start` / `_end` = 8.5dp. */
    val TouchPadding = 8.5.dp

    /** `dimen/coui_touchsearch_touch_end_gap` = 8dp. */
    val TouchEndGap = 8.dp

    /**
     * `dimen/coui_touchsearch_default_height` = 432dp, the height the native rail assumes for its
     * full letter set at the 16dp stride.
     */
    val DefaultHeight = 432.dp

    /**
     * `dimen/coui_touchsearch_limit1_height`, `_limit2_height`, `_limit3_height`, `_limit4_height`
     * and `_limit5_height` = 416dp, 288dp, 224dp, 192dp, 144dp — the heights at which the native rail
     * collapses letters into dots.
     */
    val LimitHeights = listOf(416.dp, 288.dp, 224.dp, 192.dp, 144.dp)

    /** `dimen/coui_touchsearch_min_height` = 296dp. */
    val MinHeight = 296.dp

    /** `string/star_character` = `*`, the leading key of `array/normal_touchsearch_keys`. */
    val StarCharacter = "*"

    /** `string/well_character` = `#`, the trailing key of both touch search arrays. */
    val WellCharacter = "#"

    /**
     * `string/coui_touchsearch_dot` = `·`, the placeholder the native rail substitutes for hidden
     * letters when the rail is shorter than `coui_touchsearch_limit1_height`.
     */
    val Dot = "·"

    /**
     * The A–Z letters of `string/coui_touchsearch_character_a` …
     * `string/coui_touchsearch_character_z`. Each of those strings holds the bare upper-case letter.
     */
    val Letters: List<String> = ('A'..'Z').map(Char::toString)

    /**
     * `array/normal_touchsearch_keys`: the `*` collection key, then A–Z, then `#`.
     * `attr/couiFirstIsCharacter` is false for this array, which is why it carries a leading icon.
     */
    val NormalKeys: List<String> = listOf(StarCharacter) + Letters + WellCharacter

    /** `array/special_touchsearch_keys`: A–Z then `#`, with no leading collection key. */
    val SpecialKeys: List<String> = Letters + WellCharacter

    /**
     * The rail colours.
     *
     * `style/Widget.COUI.COUITouchSearchView` sets `couiKeyTextColor` to
     * `color/coui_touchsearchview_text_color`, whose unpressed item is `?attr/couiColorSecondNeutral`
     * (`coui_color_secondary_neutral`, `#8a000000` / `#8affffff` — exactly
     * [io.github.suqi8.coui.kmp.theme.Colors.onSurfaceSecondary]) and whose pressed item is
     * `?attr/couiColorPrimaryTextOnPopup` (the theme accent, `couiDefaultPrimaryR` = `#ff347cff` in
     * the default theme — mapped to [io.github.suqi8.coui.kmp.theme.Colors.primary]).
     *
     * The tile fill is `?attr/couiColorBackgroundTop` (`coui_color_background_top`, `#ffffffff` /
     * `#ff333333`) and its text is `coui_touchsearch_popup_text_color` (`#d9000000` / `#d9ffffff`).
     * Neither pair has an exact theme slot, so both are picked from the theme background luminance.
     */
    @Composable
    fun colors(
        textColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
        pressedTextColor: Color = COUITheme.colorScheme.primary,
        popupBackgroundColor: Color = if (isDarkTheme()) Color(0xFF333333) else Color(0xFFFFFFFF),
        popupTextColor: Color = if (isDarkTheme()) Color(0xD9FFFFFF) else Color(0xD9000000),
        secondPopupBackgroundColor: Color = COUITheme.colorScheme.tertiaryContainer,
        secondPopupTextColor: Color = COUITheme.colorScheme.primary,
    ): TouchSearchViewColors = remember(
        textColor,
        pressedTextColor,
        popupBackgroundColor,
        popupTextColor,
        secondPopupBackgroundColor,
        secondPopupTextColor,
    ) {
        TouchSearchViewColors(
            textColor,
            pressedTextColor,
            popupBackgroundColor,
            popupTextColor,
            secondPopupBackgroundColor,
            secondPopupTextColor,
        )
    }
}

@Composable
private fun isDarkTheme(): Boolean = COUITheme.colorScheme.background.luminance() < 0.5f

/**
 * The colours of a [TouchSearchView].
 *
 * @property textColor `coui_touchsearchview_text_color` unpressed item, i.e.
 *   `?attr/couiColorSecondNeutral`.
 * @property pressedTextColor `coui_touchsearchview_text_color` pressed item, i.e.
 *   `?attr/couiColorPrimaryTextOnPopup`.
 * @property popupBackgroundColor `drawable/coui_touch_search_popup_bg` solid, i.e.
 *   `?attr/couiColorBackgroundTop`.
 * @property popupTextColor `color/coui_touchsearch_popup_text_color`.
 * @property secondPopupBackgroundColor The `setName` list's fill, `coui_touchsearch_second_name_bg`
 *   tinted `?attr/couiColorSecondary` (`coui_color_secondary_blue`, `#260080FF` / `#401A8CFF`).
 * @property secondPopupTextColor `color/coui_touchsearch_popupwin_sub_textcolor`, i.e.
 *   `?attr/couiColorPrimaryTextOnPopup`.
 */
@Immutable
data class TouchSearchViewColors(
    val textColor: Color,
    val pressedTextColor: Color,
    val popupBackgroundColor: Color,
    val popupTextColor: Color,
    val secondPopupBackgroundColor: Color = Color(0x260080FF),
    val secondPopupTextColor: Color = Color(0xFF0080FF),
) {
    /**
     * The four-color form, kept so callers written before the `setName` popup existed still
     * compile; the name list falls back to the light `couiColorSecondary` and the theme accent.
     */
    constructor(
        textColor: Color,
        pressedTextColor: Color,
        popupBackgroundColor: Color,
        popupTextColor: Color,
    ) : this(
        textColor = textColor,
        pressedTextColor = pressedTextColor,
        popupBackgroundColor = popupBackgroundColor,
        popupTextColor = popupTextColor,
        secondPopupBackgroundColor = Color(0x260080FF),
        secondPopupTextColor = Color(0xFF0080FF),
    )

    /** The four-color [copy], kept for binary compatibility with callers built before 1.3.0. */
    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Migrated to the copy that also takes the setName popup colors.",
        level = DeprecationLevel.HIDDEN,
    )
    fun copy(
        textColor: Color = this.textColor,
        pressedTextColor: Color = this.pressedTextColor,
        popupBackgroundColor: Color = this.popupBackgroundColor,
        popupTextColor: Color = this.popupTextColor,
    ): TouchSearchViewColors = copy(
        textColor = textColor,
        pressedTextColor = pressedTextColor,
        popupBackgroundColor = popupBackgroundColor,
        popupTextColor = popupTextColor,
        secondPopupBackgroundColor = secondPopupBackgroundColor,
        secondPopupTextColor = secondPopupTextColor,
    )
}
