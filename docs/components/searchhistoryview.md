# SearchHistoryView

The search-history panel mirroring ColorOS's `COUISearchHistoryView` (`com/coui/appcompat/searchhistory/COUISearchHistoryView`), the block that sits under a search field and shows the user's recent queries as a wrapping field of chips. It is a title bar, a [FlowLayout](#flowlayout) of chips, and — when the chips do not fit the fold — a chevron button that toggles between the folded and unfolded row counts.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.FlowLayout
import io.github.suqi8.coui.kmp.basic.FlowLayoutDefaults
import io.github.suqi8.coui.kmp.basic.SearchHistoryDeleteIcon
import io.github.suqi8.coui.kmp.basic.SearchHistoryChip
import io.github.suqi8.coui.kmp.basic.SearchHistoryChipColors
import io.github.suqi8.coui.kmp.basic.SearchHistoryChipDefaults
import io.github.suqi8.coui.kmp.basic.SearchHistoryExpandIcon
import io.github.suqi8.coui.kmp.basic.SearchHistoryView
import io.github.suqi8.coui.kmp.basic.SearchHistoryViewColors
import io.github.suqi8.coui.kmp.basic.SearchHistoryViewDefaults
```

## Basic Usage

```kotlin
var history by remember { mutableStateOf(listOf("wireless earbuds", "usb-c hub", "laptop stand")) }

SearchHistoryView(
    items = history,
    onItemClick = { query -> /* run the search */ },
    onDeleteClick = { history = emptyList() },
    title = "Recent searches",
    deleteIconDescription = "Delete search history",
)
```

Each item becomes one unchecked [Chip](chip) in a [FlowLayout](#flowlayout), wrapped at the panel's width with 8dp between chips on a line and 8dp between lines.

`onDeleteClick` is invoked **after** the panel has faded out, not on the tap: the port runs the native `doDeleteAnimation`, animating the whole panel's alpha from 1 to 0 over `SEARCH_VIEW_DISAPPEAR_DURATION` (300ms) with `COUIMoveEaseInterpolator` (`cubic-bezier(0.3, 0, 0.1, 1)`), and only then calls the callback. The panel stays transparent afterwards, exactly like the native view, so the host is expected to drop the panel or repopulate it — see [Folding](#folding) below for a working pattern.

## Long-Press to Delete

Pass `onItemDelete` and each keyword becomes a [SearchHistoryChip](#searchhistorychip) instead of a `Chip`. A long press on a keyword springs a "×" out of its end edge while the chip widens to make room; tapping the "×" calls `onItemDelete` with that keyword, and tapping the chip body or long-pressing again folds the "×" back in.

```kotlin
SearchHistoryView(
    items = history,
    onItemClick = { query -> /* run the search */ },
    onDeleteClick = { history = emptyList() },
    title = "Recent searches",
    onItemDelete = { keyword -> history = history - keyword },
    itemDeleteContentDescription = { keyword -> "Delete $keyword" },
)
```

## Folding

`maxRowFolded` is the row count the panel is folded to; `maxRowUnfolded` is the row count it opens to. The native constructor hard-codes the latter to `COUISearchHistoryView.DEFAULT_MAX_ROW_UNFOLDED` = 8, and both `maxRowFolded` and `expandable` come from XML attributes whose native defaults are `Integer.MAX_VALUE` and `true` — so out of the box **nothing folds**, and a panel only folds when the caller asks for it:

```kotlin
SearchHistoryView(
    items = history,
    onItemClick = {},
    onDeleteClick = {},
    title = "Recent searches",
    maxRowFolded = 2,
    expandable = true,
)
```

The chevron button appears below the chips and is a 28dp box holding the 16dp `drawable/coui_component_expand_arrow_drop_down` chevron; unfolding rotates it 180 degrees, which is exactly `drawable/coui_component_expand_arrow_drop_up`. `expandable = false` drops the button and pins the panel to `maxRowFolded` rows forever.

## Properties

### SearchHistoryView

| Property              | Type                    | Description                                                       | Default Value                        | Required |
| --------------------- | ----------------------- | ----------------------------------------------------------------- | ------------------------------------ | -------- |
| items                 | List&lt;String&gt;      | The recent searches, oldest to newest                              | -                                    | Yes      |
| onItemClick           | (String) -> Unit        | Called with a chip's text when it is clicked                       | -                                    | Yes      |
| onDeleteClick         | () -> Unit              | Called after the delete animation finishes                         | -                                    | Yes      |
| modifier              | Modifier                | Modifier applied to the panel                                      | Modifier                             | No       |
| title                 | String                  | Title bar text                                                     | ""                                   | No       |
| expandable            | Boolean                 | Whether the panel can unfold                                       | true                                 | No       |
| maxRowFolded          | Int                     | Rows shown while folded                                            | SearchHistoryViewDefaults.MaxRowFolded | No     |
| maxRowUnfolded        | Int                     | Rows shown while unfolded                                          | SearchHistoryViewDefaults.MaxRowUnfolded | No   |
| deleteIcon            | ImageVector             | Image drawn in the delete button                                   | SearchHistoryDeleteIcon              | No       |
| deleteIconDescription | String?                 | Accessibility label of the delete button                           | null                                 | No       |
| colors                | SearchHistoryViewColors | The colours                                                        | SearchHistoryViewDefaults.colors()   | No       |
| onItemDelete          | ((String) -> Unit)?     | When non-null, chips are SearchHistoryChips with long-press delete; called with the deleted keyword | null | No |
| itemDeleteContentDescription | ((String) -> String)? | Accessibility label of each chip's "×", given the keyword | null | No |

### SearchHistoryViewDefaults

| Constant            | Type  | Value          | COUI Source                                                   |
| ------------------- | ----- | -------------- | ------------------------------------------------------------- |
| PaddingStart        | Dp    | 16.dp          | coui_component_search_history_padding_start (20dp from w600dp) |
| PaddingTop          | Dp    | 16.dp          | coui_component_search_history_padding_top                      |
| PaddingEnd          | Dp    | 16.dp          | coui_component_search_history_padding_end (20dp from w600dp)   |
| PaddingBottom       | Dp    | 32.dp          | coui_component_search_history_padding_bottom                   |
| TitleMarginVertical | Dp    | 7.dp           | coui_component_search_history_title_margin_vertical            |
| DeleteIconSize      | Dp    | 18.dp          | coui_component_search_history_delete_icon_size                 |
| DeleteIconEndMargin | Dp    | 0.dp           | coui_component_search_history_delete_icon_end_margin           |
| FlowMarginTop       | Dp    | 8.dp           | coui_component_search_history_flow_margin_top                  |
| FlowItemSpacing     | Dp    | 8.dp           | coui_component_search_history_flow_item_spacing                |
| FlowLineSpacing     | Dp    | 8.dp           | coui_component_search_history_flow_line_spacing                |
| ChipHeight          | Dp    | 28.dp          | coui_chip_suggest_style_height                                 |
| ChipCornerRadius    | Dp    | 14.dp          | chipCornerRadius 16777215 → height / 2                         |
| ExpandButtonSize    | Dp    | 28.dp          | coui_chip_default_min_height                                   |
| ExpandIconSize      | Dp    | 16.dp          | drawable/coui_component_expand_arrow_drop_down                 |
| MaxRowFolded        | Int   | Int.MAX_VALUE  | attr/COUISearchHistoryView_maxRowFolded                        |
| MaxRowUnfolded      | Int   | 8              | COUISearchHistoryView.DEFAULT_MAX_ROW_UNFOLDED                 |

#### Methods

| Method Name    | Type      | Description                                                             |
| -------------- | --------- | ----------------------------------------------------------------------- |
| titleTextStyle() | TextStyle | Title style (COUITheme.textStyles.body2 at Medium, COUI couiTextAppearanceButton 14sp sans-serif-medium) |
| colors()       | SearchHistoryViewColors | The panel colours from the theme                          |

### Colours

| COUI token                                                                    | Value                 | Used for          | Port              |
| ----------------------------------------------------------------------------- | --------------------- | ----------------- | ----------------- |
| attr/couiColorPrimaryNeutral → color/coui_color_primary_neutral                | #e6000000 / #e6ffffff | Title text        | onSurface         |
| attr/couiColorSecondNeutral → color/coui_color_secondary_neutral               | #8a000000 / #8affffff | Delete icon fill  | onSurfaceSecondary |
| attr/couiColorSecondNeutral → color/coui_color_secondary_neutral               | #8a000000 / #8affffff | Expand chevron stroke | onSurfaceSecondary |

The delete vector is `drawable/coui_component_search_history_delete` — an 18x18dp vector with a single `fillType="evenOdd"` path whose `android:fillColor` is `?attr/couiColorSecondNeutral` — and the chevron is `drawable/coui_component_expand_arrow_drop_down`, a 16x16dp vector with `android:strokeColor="?attr/couiColorSecondNeutral"`, stroke width 0.933333 and round cap/join. Both therefore resolve to the same attribute, which is why the port exposes two colour slots that default to the same theme value.

### Text

| Item  | Value                                   | Source                              |
| ----- | --------------------------------------- | ----------------------------------- |
| Title | couiTextAppearanceButton, 14sp, sans-serif-medium | COUISearchHistoryView constructor   |

## SearchHistoryChip

A keyword chip with long-press-to-delete, usable on its own in any [FlowLayout](#flowlayout). It is a 32dp capsule along the system continuous-curvature outline (`CapsuleOutlineShape`), filled with `surfaceContainerHigh`, with 12dp padding on both sides and a 13sp single-line label.

- A long press turns delete mode on: the "×" (a 9dp glyph of two 1.3dp round-capped strokes in a 24dp tap target) scales from 0.3 to 1 and fades in while the chip widens by 18dp. Both follow one critically damped spring (stiffness 438.65), so the chip only grows sideways and keeps its height.
- In delete mode, tapping the "×" calls `onDelete`; tapping the chip body or long-pressing again turns delete mode off. The "×" only takes taps once it is more than halfway out.
- While pressed the chip scales to 0.96 on the same spring and its fill lightens 28% toward white over 150ms.

```kotlin
FlowLayout(itemSpacing = 8.dp, lineSpacing = 8.dp) {
    history.forEach { keyword ->
        key(keyword) {
            SearchHistoryChip(
                text = keyword,
                onClick = { /* run the search */ },
                onDelete = { history = history - keyword },
                deleteContentDescription = "Delete $keyword",
            )
        }
    }
}
```

The overload with `deleteMode` / `onDeleteModeChange` hoists delete mode, for example to keep only one "×" out at a time.

### SearchHistoryChip

| Property                 | Type                    | Description                                         | Default Value                      | Required |
| ------------------------ | ----------------------- | --------------------------------------------------- | ---------------------------------- | -------- |
| text                     | String                  | The keyword                                          | -                                  | Yes      |
| onClick                  | () -> Unit              | Called on a tap outside delete mode                  | -                                  | Yes      |
| onDelete                 | () -> Unit              | Called when the "×" is tapped                        | -                                  | Yes      |
| deleteMode               | Boolean                 | Whether the "×" is out (hoisted overload only)       | -                                  | Yes      |
| onDeleteModeChange       | (Boolean) -> Unit       | Called on long press, or with false on a body tap in delete mode (hoisted overload only) | - | Yes |
| modifier                 | Modifier                | Modifier applied to the chip                         | Modifier                           | No       |
| deleteContentDescription | String?                 | Accessibility label of the "×"                       | null                               | No       |
| colors                   | SearchHistoryChipColors | The colours                                          | SearchHistoryChipDefaults.colors() | No       |

### SearchHistoryChipDefaults

| Constant                   | Type     | Value     |
| -------------------------- | -------- | --------- |
| Height                     | Dp       | 32.dp     |
| PaddingStart               | Dp       | 12.dp     |
| PaddingEnd                 | Dp       | 12.dp     |
| DeleteExtraWidth           | Dp       | 18.dp     |
| DeleteTouchWidth           | Dp       | 24.dp     |
| DeleteIconOffset           | Dp       | 2.dp      |
| DeleteIconSize             | Dp       | 9.dp      |
| DeleteIconStrokeWidth      | Dp       | 1.3.dp    |
| FontSize                   | TextUnit | 13.sp     |
| PressedScale               | Float    | 0.96f     |
| SpringStiffness            | Float    | 438.65f   |
| PressedColorDurationMillis | Int      | 150       |

`colors()` defaults the fill to `surfaceContainerHigh`, the pressed fill to that colour mixed 28% toward white, the label to `onSurface` and the "×" to `onSurfaceSecondary`.

## FlowLayout

`FlowLayout` (`com/coui/appcompat/searchhistory/COUIFlowLayout`) is public because it is the layout the panel is built on; use it directly when you need the wrapping behaviour without the title bar and the delete button.

### FlowLayout

| Property    | Type                       | Description                                        | Default Value              | Required |
| ----------- | -------------------------- | -------------------------------------------------- | -------------------------- | -------- |
| modifier    | Modifier                   | Modifier applied to the flow layout                 | Modifier                   | No       |
| itemSpacing | Dp                         | Horizontal gap between two chips on one line        | FlowLayoutDefaults.ItemSpacing | No   |
| lineSpacing | Dp                         | Vertical gap between two lines                      | FlowLayoutDefaults.LineSpacing | No   |
| maxRows     | Int                        | Lines to place; 0 or less means no limit            | FlowLayoutDefaults.MaxRows | No       |
| content     | @Composable () -> Unit      | The chips to flow                                   | -                          | Yes      |

### FlowLayoutDefaults

| Constant    | Type | Value         | COUI Source                       |
| ----------- | ---- | ------------- | --------------------------------- |
| ItemSpacing | Dp   | 0.dp          | attr/COUIFlowLayout_itemSpacing   |
| LineSpacing | Dp   | 0.dp          | attr/COUIFlowLayout_lineSpacing   |
| MaxRows     | Int  | Int.MAX_VALUE | attr/COUIFlowLayout_maxRowFolded / _maxRowUnfolded |

## Behavior

- Placement repeats the native `Line.canAdd` / `Line.addView`: `usedWidth + itemSpacing + childWidth <= maxWidth`, with the first child of a line always accepted so a chip wider than the container still gets a line of its own. A line is as tall as its tallest child and the block is `sum(lineHeights) + lineSpacing * (lineCount - 1)`, which is the native `calculateLinesHeight`. Chips sit at the top edge of their line, where the native `Line.layout` puts them, and RTL mirrors each chip the same way the native `isRtl` branch does.
- Chips are the `COUIChip` inflated from `res/layout/coui_component_item_search_history`, i.e. `style/Widget.COUI.Chip.Suggestion`: a 28dp capsule with 12dp start/end padding (`coui_chip_suggest_style_chip_horizontal_padding`) and `chipCornerRadius` 16777215 resolved to half the height.
- The panel is padded 16dp on the start, top and end and 32dp on the bottom; the title bar carries 7dp of vertical padding and the flow container sits 8dp below it. Both native start/end dimens also carry a `w600dp` value of 20dp, which a Kotlin `Dp` constant cannot express.

## Not Ported

- The chips' own text style. `style/Widget.COUI.Chip` sets `android:textAppearance="?attr/couiTextBodyXS"` (12sp), but [Chip](chip) has no text-style parameter and renders its label at `ChipDefaults.textStyle()` — `COUITheme.textStyles.body2`, 14sp. The chips are therefore 2sp larger than in ColorOS.
- Folding is done by **not placing** the children of the lines past `maxRows`; the native keeps them laid out and hides them with `View.INVISIBLE` plus a 250 / 100 / 400 / 300ms fade (`EXPAND_BUTTON_APPEAR_DURATION`, `HIDDEN_VIEWS_APPEAR_DELAY`, `HIDDEN_VIEWS_APPEAR_DURATION`, `HIDDEN_VIEWS_DISAPPEAR_DURATION`). Expanding and collapsing here is therefore instant.
- The chevron button is centred **below** the chips; the native `ensureAndExpandButton` appends it to the last folded line instead, evicting that line's trailing chip into `foldLineRemovedChip` when it no longer fits. The button is also shown whenever `expandable` is true and `maxRowFolded < maxRowUnfolded`, whereas the native additionally runs `isNeedExpandButton` over the laid-out chips and shows nothing when they already fit inside `maxRowFolded` rows.
- The ripple background of the delete button, `drawable/coui_component_icon_bg` (`android:radius="24dp"` with `?attr/couiColorPress`), and the `coui_chip_suggest_style_close_icon*` family of the Suggestion chip style.
- The expand/collapse state is internal. The native exposes `setExpandState(isExpand)`; there is no such parameter here.
- `COUIFlowLayout.clearAll()`, `COUISearchHistoryView.clearAll()` and `setItems(List<IItem>)` are expressed by changing the `items` list. `IItem.getDisplayContent()` collapses to a plain `String`.
- `COUISearchHistoryView.onConfigurationChanged`, which only re-applies the delete icon's end margin.
