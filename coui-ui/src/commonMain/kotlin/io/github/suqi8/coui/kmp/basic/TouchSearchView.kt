// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import io.github.suqi8.coui.kmp.theme.COUITheme

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
 */
@Composable
fun TouchSearchView(
    keys: List<String>,
    onKeySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hapticFeedbackEnabled: Boolean = true,
    colors: TouchSearchViewColors = TouchSearchViewDefaults.colors(),
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

    val keyStyle = remember(colors.textColor) {
        TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.textColor)
    }
    val pressedKeyStyle = remember(colors.pressedTextColor) {
        TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.pressedTextColor)
    }
    val popupStyle = remember(colors.popupTextColor) {
        TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, color = colors.popupTextColor)
    }

    val keyLayouts = remember(textMeasurer, keys, keyStyle) { keys.map { textMeasurer.measure(it, keyStyle) } }
    val pressedKeyLayouts = remember(textMeasurer, keys, pressedKeyStyle) {
        keys.map { textMeasurer.measure(it, pressedKeyStyle) }
    }
    val popupLayouts = remember(textMeasurer, keys, popupStyle) { keys.map { textMeasurer.measure(it, popupStyle) } }

    var pressedIndex by remember(keys) { mutableStateOf(-1) }
    val onKeySelectedState by rememberUpdatedState(onKeySelected)

    fun selectAt(y: Float) {
        val index = ((y - paddingTopPx) / stridePx).toInt().coerceIn(0, keys.lastIndex)
        if (index == pressedIndex) return
        pressedIndex = index
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

        if (pressedIndex in keys.indices) {
            drawMagnifiedKey(
                layout = popupLayouts[pressedIndex],
                backgroundColor = colors.popupBackgroundColor,
                centerY = paddingTopPx + pressedIndex * stridePx + stridePx / 2f,
                tileSizePx = popupSizePx,
                gapPx = popupEndMarginPx,
            )
        }
    }
}

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
 */
private fun DrawScope.drawMagnifiedKey(
    layout: TextLayoutResult,
    backgroundColor: Color,
    centerY: Float,
    tileSizePx: Float,
    gapPx: Float,
) {
    val left = -gapPx - tileSizePx
    drawRect(
        color = backgroundColor,
        topLeft = Offset(left, centerY - tileSizePx / 2f),
        size = Size(tileSizePx, tileSizePx),
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            x = left + (tileSizePx - layout.size.width) / 2f,
            y = centerY - layout.size.height / 2f,
        ),
    )
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
    ): TouchSearchViewColors = remember(textColor, pressedTextColor, popupBackgroundColor, popupTextColor) {
        TouchSearchViewColors(textColor, pressedTextColor, popupBackgroundColor, popupTextColor)
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
 */
@Immutable
data class TouchSearchViewColors(
    val textColor: Color,
    val pressedTextColor: Color,
    val popupBackgroundColor: Color,
    val popupTextColor: Color,
)
