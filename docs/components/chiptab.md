# ChipTab

A horizontally scrollable bar of pill tabs mirroring ColorOS's `COUIChipTab` / `COUIChipTabGroup` inside `ChipMaskHorizontalScrollView`. The tabs are 32dp tall chips with a 14sp label, an optional 16dp icon, an optional red dot and an optional count; exactly one tab is selected at a time, selecting one scrolls it into view, and whichever edges are scrolled off fade out over 12dp.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.ChipTab
import io.github.suqi8.coui.kmp.basic.ChipTabData
import io.github.suqi8.coui.kmp.basic.ChipTabDefaults
import io.github.suqi8.coui.kmp.basic.ChipTabGroup
```

## Basic Usage

```kotlin
var selectedIndex by remember { mutableIntStateOf(0) }
val tabs = remember {
    listOf(
        ChipTabData(label = "All"),
        ChipTabData(label = "Photos"),
        ChipTabData(label = "Videos"),
        ChipTabData(label = "Documents"),
    )
}

ChipTabGroup(
    tabs = tabs,
    selectedIndex = selectedIndex,
    onSelectedChange = { selectedIndex = it },
    modifier = Modifier.fillMaxWidth(),
)
```

The group takes an explicit tab list rather than a content lambda, because it owns the tab indices: the single selection and the scroll-into-view both follow from the list position, exactly like `TabRow(tabs, selectedTabIndex, onTabSelected)`. Tabs are separated by `ChipTabDefaults.GroupSpacing` (8dp, `coui_chip_group_default_spacing_horizontal`).

## With a Leading Icon

Each tab can carry its own 16dp icon (`coui_chip_tab_style_chip_icon_size`), tinted like the label and separated from it by 4dp (`coui_chip_suggest_style_chip_icon_end_padding`):

```kotlin
val tabs = listOf(
    ChipTabData(label = "All", icon = COUIIcons.All),
    ChipTabData(label = "Photos", icon = COUIIcons.Photos),
    ChipTabData(label = "Music", icon = COUIIcons.Music),
)
```

## Red Dot and Count

```kotlin
val tabs = listOf(
    ChipTabData(label = "Inbox", showRedDot = true),
    ChipTabData(label = "Updates", count = 12),
    ChipTabData(label = "Archived", count = 128, showRedDot = true),
)
```

The red dot is 6dp wide (`coui_dot_diameter_small`), filled with `coui_color_additional_red` (`#ffeb3b2f` light / `#ffeb493d` dark) and ringed in white, placed 2dp after the content it follows (`coui_chip_red_dot_offset_horizontal`) and 6dp below the top of the pill (`coui_chip_red_dot_offset_vertical`). While it is shown the tab's end padding drops from 12dp to 8dp (`coui_chip_tab_end_padding_with_red_dot`). The count is 13sp (`couiTextBodyS`) in `couiColorLabelSecondary`, 2dp after the label (`coui_chip_tab_count_start_margin`).

## Component States

### Disabled State

A disabled tab keeps its fill and switches its label and icon to `couiColorLabelTertiary`; it no longer reacts to presses:

```kotlin
ChipTabData(label = "Unavailable", enabled = false)
```

## Using a ChipTab on its own

`ChipTab` is the leaf the group renders. Use it directly inside your own layout when you need per-tab control — the group is what binds `selected` and `onClick`:

```kotlin
ChipTab(
    label = "Photos",
    selected = true,
    onClick = { },
    icon = COUIIcons.Photos,
)
```

## Properties

### ChipTabGroup

| Property         | Type                    | Description                             | Default Value | Required |
| ---------------- | ----------------------- | --------------------------------------- | ------------- | -------- |
| tabs             | List&lt;ChipTabData&gt; | The tabs of the group, in order         | -             | Yes      |
| selectedIndex    | Int                     | Index of the selected tab               | -             | Yes      |
| onSelectedChange | (Int) -> Unit           | Callback with the tab the user selected | -             | Yes      |
| modifier         | Modifier                | Modifier applied to the group           | Modifier      | No       |

### ChipTab

| Property   | Type         | Description                          | Default Value | Required |
| ---------- | ------------ | ------------------------------------ | ------------- | -------- |
| label      | String       | Tab label (single line, ellipsized)  | -             | Yes      |
| selected   | Boolean      | Whether the tab is selected          | -             | Yes      |
| onClick    | () -> Unit   | Callback when the tab is clicked     | -             | Yes      |
| modifier   | Modifier     | Modifier applied to the tab          | Modifier      | No       |
| icon       | ImageVector? | Optional 16dp leading icon           | null          | No       |
| enabled    | Boolean      | Whether the tab is enabled           | true          | No       |
| showRedDot | Boolean      | Whether the tab carries the red dot  | false         | No       |
| count      | Int?         | Optional count drawn after the label | null          | No       |

### ChipTabData

| Property   | Type         | Description                          | Default Value | Required |
| ---------- | ------------ | ------------------------------------ | ------------- | -------- |
| label      | String       | Tab label                            | -             | Yes      |
| icon       | ImageVector? | Optional 16dp leading icon           | null          | No       |
| enabled    | Boolean      | Whether the tab can be selected      | true          | No       |
| showRedDot | Boolean      | Whether the tab carries the red dot  | false         | No       |
| count      | Int?         | Optional count drawn after the label | null          | No       |

### ChipTabDefaults

| Constant               | Type                    | Default Value             | COUI Source                                     |
| ---------------------- | ----------------------- | ------------------------- | ----------------------------------------------- |
| Height                 | Dp                      | 32.dp                     | coui_chip_tab_style_height                      |
| MinWidth               | Dp                      | 52.dp                     | coui_chip_default_min_width                     |
| MaxWidth               | Dp                      | 300.dp                    | coui_chip_default_max_width                     |
| TextMaxWidth           | Dp                      | 200.dp                    | coui_chip_default_max_text_width                |
| CornerRadius           | Dp                      | 16.dp                     | chipCornerRadius 16777215dp → min(w, h) / 2     |
| IconSize               | Dp                      | 16.dp                     | coui_chip_tab_style_chip_icon_size              |
| IconEndPadding         | Dp                      | 4.dp                      | coui_chip_suggest_style_chip_icon_end_padding   |
| StartPadding           | Dp                      | 12.dp                     | coui_chip_suggest_style_chip_horizontal_padding |
| EndPadding             | Dp                      | 12.dp                     | coui_chip_suggest_style_chip_horizontal_padding |
| EndPaddingWithRedDot   | Dp                      | 8.dp                      | coui_chip_tab_end_padding_with_red_dot          |
| CountStartMargin       | Dp                      | 2.dp                      | coui_chip_tab_count_start_margin                |
| RedDotDiameter         | Dp                      | 6.dp                      | coui_dot_diameter_small                         |
| RedDotOffsetHorizontal | Dp                      | 2.dp                      | coui_chip_red_dot_offset_horizontal             |
| RedDotOffsetVertical   | Dp                      | 6.dp                      | coui_chip_red_dot_offset_vertical               |
| GroupSpacing           | Dp                      | 8.dp                      | coui_chip_group_default_spacing_horizontal      |
| FadeWidth              | Dp                      | 12.dp                     | ChipMaskHorizontalScrollView.FADING_EDGE_WIDTH  |
| CheckedStrokeWidth     | Dp                      | 0.6.dp                    | COUIChipTabDrawable.CHECKED_STROKE_WIDTH_DP     |
| MinVisibleChange       | Float                   | 0.00390625f               | COUIChipTabDrawable.ALPHA_MIN_VISIBLE_CHANGE    |
| LayerSpring            | SpringSpec&lt;Float&gt; | response 0.2 / bounce 0   | COUIChipTabDrawable layer animations            |
| ScrollSpring           | SpringSpec&lt;Float&gt; | response 0.4 / bounce 0.2 | COUIChipTabGroup.SCROLL_ANIMATION_*             |

#### Methods

| Method Name      | Type      | Description                                                             |
| ---------------- | --------- | ----------------------------------------------------------------------- |
| textStyle()      | TextStyle | Tab label style (COUITheme.textStyles.body2, COUI couiTextButtonM 14sp) |
| countTextStyle() | TextStyle | Count style (COUITheme.textStyles.footnote1, COUI couiTextBodyS 13sp)   |

### Colours

| COUI token                                                              | Value                 | Used for                    | Port                                         |
| ----------------------------------------------------------------------- | --------------------- | --------------------------- | -------------------------------------------- |
| attr/couiColorBackgroundElevated → color/coui_color_background_elevated | #ffffffff / #ff1e1e1e | Checked pill fill           | surfaceContainerHighest                      |
| attr/couiColorContainer4 → color/coui_color_container4                  | #0a000000 / #1affffff | Unchecked pill fill         | file constant, no theme slot                 |
| color/coui_color_checked_background_gradient_start                      | #40ffffff             | Checked pill overlay        | file constant, no theme slot                 |
| color/coui_color_checked_background_stroke                              | #0a000000 / #0affffff | 0.6dp checked outline       | file constant, no theme slot                 |
| attr/couiColorLabelPrimary → color/coui_color_label_primary             | #e6000000 / #e6ffffff | Label and icon, both states | onSurface                                    |
| attr/couiColorLabelTertiary → color/coui_color_label_tertiary           | #42000000 / #4dffffff | Disabled label and icon     | onSurfaceVariantActions                      |
| attr/couiColorLabelSecondary → color/coui_color_label_secondary         | #8a000000 / #8affffff | Count text                  | onSurfaceSecondary                           |
| color/coui_color_additional_red                                         | #ffeb3b2f / #ffeb493d | Red dot fill                | Badge's couiColorAdditionalRed               |
| color/coui_color_white                                                  | #ffffffff             | Red dot ring, 1dp           | Badge's white stroke (coui_dot_stroke_width) |
| color/coui_chip_tab_surface_unchecked_surface                           | #ffffffff / #ff000000 | Unchecked fill on SDK ≤ 36  | Not ported — see below                       |

`attr/couiColorBackgroundWithCard` → `color/coui_color_background_with_card` (`#fff0f1f2` / `#ffffffff`) is composited with `couiColorContainer4` by `COUIChipTabDrawable.setUncheckedBackgroundColorFromTheme` for the *base* chip fill; the tab's own drawable overrides the background with the dual layer above and never reads it, so it is not used here.

### Text

| Item        | Value                                    | Source                                              |
| ----------- | ---------------------------------------- | --------------------------------------------------- |
| Label       | couiTextButtonM, 14sp, sans-serif-medium | Widget.COUI.Chip.ChipTab android:textAppearance     |
| Ellipsize   | END, single line, centred                | Widget.COUI.Chip android:ellipsize / maxLines       |
| Label width | maximum 200dp                            | coui_chip_default_max_text_width (chipTextMaxWidth) |
| Count       | couiTextBodyS, 13sp                      | Widget.COUI.Chip.ChipTab countTextAppearance        |

## Behavior

- The pill fill is the dual layer of `COUIChipTabDrawable.drawDualLayerBackground` and `drawLightOSCheckedBackground`: the checked surface (`couiColorBackgroundElevated`) fading with the checked layer alpha, `couiColorContainer4` over it at the unchecked layer alpha, and — when checked — `coui_color_checked_background_gradient_start` over the same silhouette plus a 0.6dp `coui_color_checked_background_stroke` outline inset by half the stroke, with the corner radius reduced by the same inset.
- Checking and unchecking cross-fade the two layers on a critically damped spring (response 0.2 / bounce 0, `COUIChipTabDrawable.CheckedLayerAlphaAnimation`), cut at the 1/256 minimum visible change. The text and icon colour does not change with the selection: both states use `couiColorLabelPrimary`.
- Selecting a tab scrolls it into view when one of its edges falls inside the visible rect, which is inset by the 12dp fade width on the sides that are scrolled off (`COUIChipTabGroup.checkOcclusion`); the scroll position animates on the response 0.4 / bounce 0.2 spring (`SCROLL_ANIMATION_RESPONSE` / `SCROLL_ANIMATION_BOUNCE`). The tab is not scrolled on the first composition, matching the native chip click listener.
- The fade mask is a `BlendMode.DstOut` horizontal gradient on the edges that are actually clipped, like `ChipMaskHorizontalScrollView.dispatchDraw`.

## Not Ported

- The material blur / edge-stroke / spot-light path of `COUIChipTab.applyMaterialEffect`, gated behind the `COUIMaterialStateManager` device feature flags and theme booleans. This port draws the non-material fallback: the flat dual-layer fill and the 0.6dp checked stroke.
- The icon-only leading chip of `COUIChipTabLayout` (a 32dp round chip of a 20dp icon with 6dp start/end padding) and that layout's 16dp scroller padding.
- `COUIChipTabGroupFragmentMediator`, the fragment switch and fade-in helper.
- The `coui_chip_tab_unit` and `coui_chip_tab_position_in_group` strings; the report proves that no chip class references them.
- The press mask of `drawStateEffects` (`COUIMaskEffectDrawable` / `COUIStrokeDrawable`), whose geometry could not be resolved.
- The `Build.VERSION.SDK_INT <= 36` branch of `COUIChipTabDrawable.updateCachedThemeColors`, where the unchecked surface is the opaque `coui_chip_tab_surface_unchecked_surface` (`#ffffffff` / `#ff000000`). This library targets the ColorOS 17 / API 37 behaviour, where that surface is transparent and an unchecked tab is filled by `couiColorContainer4` alone.
