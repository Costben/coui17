# TouchSearchView

A vertical A–Z index rail mirroring ColorOS's `COUITouchSearchView` (`com/coui/appcompat/touchsearchview/COUITouchSearchView`), the alphabet strip pinned to the trailing edge of a contact or settings list. Dragging a finger up and down the rail selects the key under it, and a magnified tile showing the current key floats beside the rail.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.TouchSearchView
import io.github.suqi8.coui.kmp.basic.TouchSearchViewColors
import io.github.suqi8.coui.kmp.basic.TouchSearchViewDefaults
```

## Basic Usage

```kotlin
var selectedKey by remember { mutableStateOf("-") }

Box(modifier = Modifier.fillMaxWidth()) {
    Text(text = "Selected: $selectedKey", modifier = Modifier.align(Alignment.CenterStart))
    TouchSearchView(
        keys = TouchSearchViewDefaults.NormalKeys,
        onKeySelected = { selectedKey = it },
        modifier = Modifier.align(Alignment.TopEnd),
    )
}
```

`onKeySelected` fires whenever the key under the finger changes, including on the first press — the native `COUITouchSearchView.TouchSearchActionListener`. Re-pressing the same key does not fire again, matching `mFindKeyIndex`.

Give the rail room on its **start** side (or put it last in a non-clipping parent): the magnified tile is a separate window in the native component, positioned at `locationInWindow[0] - mPopupWindowEndMargin - mPopupFirstWidth`, so this port draws it *outside* the rail's own bounds, 0.5dp to the start of the rail.

## Key Sets

```kotlin
TouchSearchView(keys = TouchSearchViewDefaults.NormalKeys, onKeySelected = { })  // *, A…Z, #
TouchSearchView(keys = TouchSearchViewDefaults.SpecialKeys, onKeySelected = { }) // A…Z, #
```

These are `array/normal_touchsearch_keys` and `array/special_touchsearch_keys`. `NormalKeys` is `*` (`string/star_character`), then the 26 letters (`string/coui_touchsearch_character_a` … `_z`), then `#` (`string/well_character`); `SpecialKeys` drops the leading `*`. Any other key list works too — the rail is laid out purely from `keys.size`.

## Properties

| Property             | Type               | Description                                                  | Default Value                    | Required |
| -------------------- | ------------------ | ------------------------------------------------------------ | -------------------------------- | -------- |
| keys                 | List&lt;String&gt; | The index keys, top to bottom                                | -                                | Yes      |
| onKeySelected        | (String) -> Unit   | Called with the key under the finger whenever it changes      | -                                | Yes      |
| modifier             | Modifier           | Modifier applied to the rail                                 | Modifier                         | No       |
| enabled              | Boolean            | Whether the rail reacts to touch                             | true                             | No       |
| hapticFeedbackEnabled | Boolean           | Whether a key change fires haptic feedback                    | true                             | No       |
| colors               | TouchSearchViewColors | The colours                                               | TouchSearchViewDefaults.colors() | No       |

The rail sizes itself to `BackgroundWidth` x (`PaddingTop` + `keys.size` * (`EachItemHeight` + `ItemSpacing`) + `PaddingBottom`). Wrap it in a sized parent to make it shorter than its content — at `NormalKeys` and the default 16dp stride that is 39dp x 480dp.

### TouchSearchViewDefaults

| Constant        | Type               | Value                            | COUI Source                                          |
| --------------- | ------------------ | -------------------------------- | ---------------------------------------------------- |
| BackgroundWidth | Dp                 | 39.dp                            | coui_touchsearch_background_width                    |
| EachItemHeight  | Dp                 | 14.dp                            | coui_touchsearch_each_item_height                    |
| ItemSpacing     | Dp                 | 2.dp                             | coui_touchsearch_item_spacing                        |
| PaddingTop      | Dp                 | 16.dp                            | coui_touchsearch_padding_top                         |
| PaddingBottom   | Dp                 | 16.dp                            | coui_touchsearch_padding_bottom                      |
| KeyTextSize     | Dp                 | 12.dp                            | coui_touchsearch_key_textsize                        |
| DotTextSize     | Dp                 | 20.dp                            | coui_touchsearch_key_dot_textsize                    |
| PopupSize       | Dp                 | 48.dp                            | coui_touchsearch_popup_first_default_width / _height |
| PopupTextSize   | TextUnit           | 20.sp                            | coui_touchsearch_popupwin_first_textsize             |
| PopupEndMargin  | Dp                 | 0.5.dp                           | coui_touchsearch_popupwin_right_margin               |
| CharOffset      | Dp                 | 0.dp                             | coui_touchsearch_char_offset                         |
| TouchPadding    | Dp                 | 8.5.dp                           | coui_touchsearch_touch_padding_start / _end          |
| TouchEndGap     | Dp                 | 8.dp                             | coui_touchsearch_touch_end_gap                       |
| DefaultHeight   | Dp                 | 432.dp                           | coui_touchsearch_default_height                      |
| LimitHeights    | List&lt;Dp&gt;     | 416 / 288 / 224 / 192 / 144 dp   | coui_touchsearch_limit1..5_height                    |
| MinHeight       | Dp                 | 296.dp                           | coui_touchsearch_min_height                          |
| StarCharacter   | String             | `*`                              | string/star_character                                |
| WellCharacter   | String             | `#`                              | string/well_character                                |
| Dot             | String             | `·`                              | string/coui_touchsearch_dot                          |
| Letters         | List&lt;String&gt; | A…Z                              | string/coui_touchsearch_character_a … _z             |
| NormalKeys      | List&lt;String&gt; | `*`, A…Z, `#`                    | array/normal_touchsearch_keys                        |
| SpecialKeys     | List&lt;String&gt; | A…Z, `#`                         | array/special_touchsearch_keys                       |

#### Methods

| Method Name | Type                  | Description                    |
| ----------- | --------------------- | ------------------------------ |
| colors()    | TouchSearchViewColors | The rail colours from the theme |

### Colours

| COUI token                                                        | Value                 | Used for                       | Port                                        |
| ----------------------------------------------------------------- | --------------------- | ------------------------------ | ------------------------------------------- |
| attr/couiColorSecondNeutral → color/coui_color_secondary_neutral  | #8a000000 / #8affffff | Unpressed key text             | onSurfaceSecondary                          |
| attr/couiColorPrimaryTextOnPopup → color/couiDefaultPrimaryR      | #ff347cff             | Pressed key text               | primary (#FF0080FF — nearest slot)          |
| attr/couiColorBackgroundTop → color/coui_color_background_top     | #ffffffff / #ff333333 | Magnified tile fill            | file constant, no theme slot                |
| color/coui_touchsearch_popup_text_color                           | #d9000000 / #d9ffffff | Magnified tile text            | file constant, no theme slot                |
| color/coui_touchsearchview_key_text_color_unpressed               | #8c000000 / #8cffffff | Key text, unpressed (style)    | superseded by the selector above            |
| color/coui_touchsearchview_popup_bg_color                         | #ffffffff             | Popup background (style)       | superseded by couiColorBackgroundTop        |
| color/coui_touchsearchview_dot_color                              | selector              | Hidden-letter dot              | Not ported — see below                      |

`color/coui_touchsearchview_text_color` is a selector: the pressed item is `?attr/couiColorPrimaryTextOnPopup`, the default item is `?attr/couiColorSecondNeutral`. `coui_touchsearchview_popup_bg_color` (`#ffffffff`) is the popup background colour but the actual tile drawable is `drawable/coui_touch_search_popup_bg` — a `<shape android:shape="rectangle">` with a single `<solid android:color="?attr/couiColorBackgroundTop">` and **no `<corners>`**, i.e. a plain square, which is why the tile is drawn as an unrounded rectangle.

### Text

| Item        | Value                          | Source                                     |
| ----------- | ------------------------------ | ------------------------------------------ |
| Key         | 12dp, medium                   | coui_touchsearch_key_textsize              |
| Key typeface | `sans-serif-medium`           | COUITouchSearchView.refresh()              |
| Tile key    | 20sp, medium                   | coui_touchsearch_popupwin_first_textsize   |

## Behavior

- Geometry is the native `updateKeys` + `onDraw`: each key owns a 14dp cell plus 2dp of spacing, so the stride is 16dp; the rail is 39dp wide with 16dp of padding above the first key and below the last. Text is centred horizontally and vertically in its cell, which is where the native baseline lands (`key.top + (touchBottom - top) / 2 - (descent + ascent) / 2`).
- Pressing and dragging track the finger with `awaitFirstDown` + `awaitPointerEvent`, selecting the key at `(y - paddingTop) / stride` clamped into the key range. Releasing clears the selection and hides the tile.
- A key change fires `HapticFeedbackType.TextHandleMove`, standing in for the native `VibrationEffect` fired through `VibrateUtils` / `couiAdaptiveVibrator` (`attr/couiTouchSearchVibrateLevel` = 0). Set `hapticFeedbackEnabled = false` to silence it.

## Not Ported

- The dot-collapse path: `initHeightRangeSpec`, `getShowLettersSize`, `getLetterSizeFromDot`, `getLimitDotLevel` and `LetterLimitLevelInfo`, which fold letters into `·` (`string/coui_touchsearch_dot`) as the rail gets shorter than `coui_touchsearch_limit1_height`. `TouchSearchViewDefaults.LimitHeights`, `MinHeight` and `Dot` record the recovered tokens but nothing consumes them, so a rail shorter than its content simply clips.
- The leading collection key icon (`mKeyCollectDrawable`, `drawable/coui_touchsearch_collect_normal` / `_popup`, sized by `coui_touchsearch_collect_drawable_popup_width` / `_height` = 16dp) drawn in place of the `*` key when `attr/couiFirstIsCharacter` is false.
- The second-level popup: `coui_touchsearch_popupwin_second_textsize` (18sp), `coui_touchsearch_popupwin_second_marginEnd` (8dp), `coui_touchsearch_popupname_max_height` (304dp), `coui_touchsearch_popupwin_default_offset` (23dp) and `coui_touchsearch_popup_second_text_height` (40dp) all belong to the window that lists the entries under a letter, which lives outside the rail.
- `coui_touchsearch_right_margin` (5dp), `coui_touchsearch_popupwin_margin` (4dp) and `coui_touchsearch_popup_first_key_padding_bottom` (10dp), which position the native popup window rather than paint it.
- The magnified tile is drawn as an unrounded rectangle at the rail's start rather than in a separate `PopupWindow`, so it is clipped by any ancestor that clips its bounds. Give the rail a non-clipping parent with at least 48.5dp of space on its start side.
