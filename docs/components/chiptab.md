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

## Icon-only Leading Chip

`leadingIcon` adds the icon-only chip of `COUIChipTabLayout`, as the Notes app shows before its folder tabs: a 32dp round chip of a 20dp icon (`ICON_SIZE_20`, 6dp start/end padding) that always wears the selected style and stays pinned while the tabs scroll. The tabs then start 8dp after it (`PADDING_HORIZONTAL_WITH_ICON_ONLY_CHIP`).

```kotlin
ChipTabGroup(
    tabs = tabs,
    selectedIndex = selectedIndex,
    onSelectedChange = { selectedIndex = it },
    modifier = Modifier.fillMaxWidth(),
    leadingIcon = COUIIcons.GridView,
    onLeadingIconClick = { /* open the folder list */ },
    contentPadding = PaddingValues(horizontal = 16.dp),
)
```

`contentPadding` lies inside the scroll, like the native scroller's 16dp `paddingHorizontal` with `clipToPadding = false`: the tabs scroll on under it up to the fading edge instead of being cut 16dp short. Prefer it to an outer `Modifier.padding` when the group spans the screen or a card.

## Selected Style

The selected tab is lifted the way `COUIChipTab` lifts it on ColorOS 17: a white rim stroked along the whole silhouette — brightest across the top edge, still 3px thick where it follows the rounded ends, and gone by the time it reaches the straight sides — a ramp to white above the bottom edge, a soft inner shade down the left edge with a faint lift down the right one in light themes, the measured `#F5F6F7` / `#4B4B4B` checked fill, a soft drop shadow (`ChipTabDefaults.CheckedShadow`, fitted to the Notes app) and dark-theme caustic light under its lower edge (`coui_caustic_color_3`). The rim geometry and strengths are the on-device measurement of the Notes app; see the constants above `drawChipTabCheckedRim`. All fade in with the selection. The group draws the shadows outside its fading edge, so give it room above and below rather than clipping it. The tab itself is not cut by the scroller: the fading edge and the scroll container are laid out `ChipTabDefaults.PressOutset` taller on each side and placed back, so the overshoot of the release spring draws in full while the row keeps the tab's height.

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
| leadingIcon      | ImageVector?            | Icon of the icon-only chip before the tabs | null       | No       |
| onLeadingIconClick | () -> Unit            | Callback when the icon-only chip is clicked | {}        | No       |
| contentPadding   | PaddingValues           | Padding around the tabs, inside the scroll | PaddingValues() | No |
| backdrop         | Backdrop?               | Page backdrop the pills frost; null keeps the flat fill | null | No |

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
| backdrop   | Backdrop?    | Page backdrop the pill frosts | null     | No   |
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
| IconOnlyIconSize       | Dp                      | 20.dp                     | COUIChipTabLayout.ICON_SIZE_20                  |
| IconOnlyPadding        | Dp                      | 6.dp                      | COUIChipTabLayout.PADDING_HORIZONTAL_6          |
| CheckedShadow          | Shadow                  | radius 21.82.dp, alpha 0.0865, offset (0, 6.2.dp) | ShadowUtils level 6, fitted to ColorOS 17 Notes |
| ElevationOutset        | Dp                      | 46.dp                     | How far the group lets tab shadows reach        |
| PressOutset            | Dp                      | 2.dp                      | How far the scroller reaches past a tab for the press overshoot |
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
| attr/couiColorBackgroundElevated → color/coui_color_background_elevated | #ffffffff / #ff1e1e1e | Checked pill fill           | #F5F6F7 (light) / #4B4B4B (dark), measured |
| attr/couiColorContainer4 → color/coui_color_container4                  | #0a000000 / #1affffff | Unchecked pill fill         | file constant, no theme slot                 |
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

- The pill fill is the dual layer of `COUIChipTabDrawable.drawDualLayerBackground` and `drawLightOSCheckedBackground`: the checked surface fading with the checked layer alpha — `#F5F6F7` in light themes, `#4B4B4B` in dark ones — and `couiColorContainer4` over it at the unchecked layer alpha. Both values are the measured bodies of the Notes app's pills.
- With a `backdrop` the pill switches to the Notes app's frosted material: `Modifier.textureBlur` samples a blurred copy of the backdrop under `GlassChipDefaults.blurColors`, and the checked layer only adds `GlassChipDefaults.highlightWash` on top instead of the opaque surface, so a checked pill stays glass rather than a solid block. Without a backdrop (or with blur disabled) the flat dual-layer fill above is drawn instead.
- The checked pill's glass rim is drawn inside the silhouette between the fill and the label, as a 0.8dp stroke along the silhouette whose white alpha follows the outline: 100% down the top edge, fading out 34px in, back to 100% over the last 46px above the bottom edge (dark: 0.55dp, 27% white on top, 21% on the bottom, fading out 33px / 69px in). Light themes add a 4px solid white cap on the bottom edge with a 4px ramp above it, a vertical shade from 242 down 60px from the left edge and a faint lift to 247 from the right one. These are the on-device measurements of the Notes app at 620dpi, matching its `TYPE_CONTENT_CHIP_TAB_SELECTED` glass bloom and `TYPE_FRAMEWORK_CAPSULE_2` stroke without compositing a backdrop.
- Checking and unchecking cross-fade the two layers on a critically damped spring (response 0.2 / bounce 0, `COUIChipTabDrawable.CheckedLayerAlphaAnimation`), cut at the 1/256 minimum visible change. The text and icon colour does not change with the selection: both states use `couiColorLabelPrimary`.
- Selecting a tab scrolls it into view when one of its edges falls inside the visible rect, which is inset by the 12dp fade width on the sides that are scrolled off (`COUIChipTabGroup.checkOcclusion`); the scroll position animates on the response 0.4 / bounce 0.2 spring (`SCROLL_ANIMATION_RESPONSE` / `SCROLL_ANIMATION_BOUNCE`). The tab is not scrolled on the first composition, matching the native chip click listener.
- The fade mask is a `BlendMode.DstOut` horizontal gradient on the edges that are actually clipped, like `ChipMaskHorizontalScrollView.dispatchDraw`.
- Pressing shrinks the tab on the press spring and releasing springs it back past its resting size; the fading edge's offscreen layer and the scroll container both clip to their own height, so they are laid out `ChipTabDefaults.PressOutset` taller on each side and placed back up, and the row still reports the tab's height. The native instead expands the parents' clip by 100dp (`COUIChipTab.updateClipExpandForShadowOnParents`).

## Not Ported

- The spot-light path of `COUIChipTab.applyMaterialEffect`, gated behind the `COUIMaterialStateManager` device feature flags and theme booleans. The blur half is opt-in through the `backdrop` parameter: a tab sits inside the scroller's offscreen fading-edge layer, where sampling the page backdrop can re-enter the node tree on the render thread, so nothing is sampled unless the caller hands a backdrop in.
- The icon-only leading chip of `COUIChipTabLayout` (a 32dp round chip of a 20dp icon with 6dp start/end padding) and that layout's 16dp scroller padding.
- `COUIChipTabGroupFragmentMediator`, the fragment switch and fade-in helper.
- The `coui_chip_tab_unit` and `coui_chip_tab_position_in_group` strings; the report proves that no chip class references them.
- The press mask of `drawStateEffects` (`COUIMaskEffectDrawable` / `COUIStrokeDrawable`), whose geometry could not be resolved.
- The `Build.VERSION.SDK_INT <= 36` branch of `COUIChipTabDrawable.updateCachedThemeColors`, where the unchecked surface is the opaque `coui_chip_tab_surface_unchecked_surface` (`#ffffffff` / `#ff000000`). This library targets the ColorOS 17 / API 37 behaviour, where that surface is transparent and an unchecked tab is filled by `couiColorContainer4` alone.
