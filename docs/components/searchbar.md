# SearchBar

`SearchBar` is a component in COUI used for user search input. It provides an intuitive and
easy-to-use search interface with support for expanded/collapsed state switching and search
suggestions display.

It comes in two forms. The low-level form takes the field as an `inputField` slot and an optional
`outsideEndAction`, which is what a caller that composes its own field wants. The other form takes
`query` / `onQueryChange` / `cancelText` and builds both the field and the edit-state cancel
capsule itself, so the two capsules melt into one silhouette the way `COUISearchBar` does — see
[Cancel capsule with the melt ball](#cancel-capsule-with-the-melt-ball).

<div style="position: relative; height: 250px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../compose/index.html?id=searchBar" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.SearchBar
import io.github.suqi8.coui.kmp.basic.InputField
```

## Basic Usage

```kotlin
var searchText by remember { mutableStateOf("") }
var expanded by remember { mutableStateOf(false) }

SearchBar(
    inputField = {
        InputField(
            query = searchText,
            onQueryChange = { searchText = it },
            onSearch = { /* Handle search action */ },
            expanded = expanded,
            onExpandedChange = { expanded = it }
        )
    },
    expanded = expanded,
    onExpandedChange = { expanded = it }
) {
    // Search results content
    Column {
        // Add search suggestions or results here
    }
}
```

## Properties

### SearchBar Properties

| Property Name    | Type                               | Description                                     | Default Value       | Required |
|------------------|------------------------------------|-------------------------------------------------|---------------------|----------|
| inputField       | @Composable () -> Unit             | Search input field component                    | -                   | Yes      |
| onExpandedChange | (Boolean) -> Unit                  | Callback when expanded state changes            | -                   | Yes      |
| modifier         | Modifier                           | Modifier applied to the search bar              | Modifier            | No       |
| insideMargin     | DpSize                             | Internal padding                                | SearchBarDefaults.InsideMargin | No       |
| expanded         | Boolean                            | Whether to show search results                  | false               | No       |
| outsideEndAction | @Composable (() -> Unit)?          | Action component shown on the end when expanded | null                | No       |
| content          | @Composable ColumnScope.() -> Unit | Content shown when expanded                     | -                   | Yes      |

### InputField Properties

| Property Name     | Type                      | Description                          | Default Value        | Required |
|-------------------|---------------------------|--------------------------------------|----------------------|----------|
| query             | String                    | Text content in search field         | -                    | Yes      |
| onQueryChange     | (String) -> Unit          | Callback when text content changes   | -                    | Yes      |
| onSearch          | (String) -> Unit          | Callback when search is executed     | -                    | Yes      |
| expanded          | Boolean                   | Whether in expanded state            | -                    | Yes      |
| onExpandedChange  | (Boolean) -> Unit         | Callback when expanded state changes | -                    | Yes      |
| modifier          | Modifier                  | Modifier applied to the input field  | Modifier             | No       |
| label             | String                    | Placeholder text when empty          | ""                   | No       |
| enabled           | Boolean                   | Whether search field is enabled      | true                 | No       |
| textStyle         | TextStyle?                | Style of text in search box          | null                 | No       |
| colors            | SearchBarColors           | Colors of the input field            | SearchBarDefaults.searchBarColors() | No       |
| stroke            | MaterialStrokeParams?     | Material rim and inner shadow over the capsule | SearchBarDefaults.searchBarStroke() | No       |
| glowColor         | Color?                    | Caustic light below the capsule, clipped to its outside. Off by default: the system gates it behind a material feature both reference devices report off; pass `SearchBarDefaults.causticGlowColor()` to opt in | null | No       |
| leadingIcon       | @Composable (() -> Unit)? | Leading icon                         | default search icon  | No       |
| trailingIcon      | @Composable (() -> Unit)? | Trailing icon                        | default clear button | No       |
| interactionSource | MutableInteractionSource? | Interaction source                   | null                 | No       |
| clearContentDescription | String              | Accessibility label of the default clear button, `R.string.coui_search_clear_button_description` on the device | "Search Cleanup" | No |

### SearchBarDefaults Object

The SearchBarDefaults object provides default values for SearchBar and InputField components.

#### Constants

| Constant Name              | Type     | Description                                        | Default Value      |
| -------------------------- | -------- | -------------------------------------------------- | ------------------ |
| InsideMargin               | DpSize   | Internal padding of the SearchBar                  | DpSize(16.dp, 6.dp)|
| NATIVE_BLUR_SIGMA_PX       | Float    | Blur sigma of the blurred preset (`FRAMEWORK_TOP_BAR_BLUR_PARAMS`) | 20f |
| NATIVE_TOP_BAR_BLUR_SIGMA_PX | Float  | Blur sigma of the default preset (`FRAMEWORK_TOP_BAR_PARAMS`): none, the capsule is two tint layers | 0f |
| InputFieldMinHeight        | Dp       | Minimum height of the InputField background        | 40.dp              |
| InputFieldFontSize         | TextUnit | Font size for the InputField text and label        | 15.sp              |
| LeadingIconStartPadding    | Dp       | Start padding for default leading icon             | 12.dp              |
| LeadingIconEndPadding      | Dp       | End padding for default leading icon               | 8.dp               |
| TrailingIconStartPadding   | Dp       | Start padding for default clear button             | 4.dp               |
| TrailingIconEndPadding     | Dp       | End padding for default clear button               | 4.dp               |
| TrailingIconTouchTargetSize | Dp      | Circular touch target size of default clear button | 36.dp              |
| TrailingIconVisualSize     | Dp       | Visual circle size of default clear button         | 18.dp              |
| MetaBallSpringResponse     | Float    | Edit-state spring response, `StateChangedToEditParamsHigh.getResponse()` | 0.5f |
| MetaBallSpringBounce       | Float    | Edit-state spring bounce, `COUISpringForce.setBounce` | 0.25f          |
| CancelInitialScale         | Float    | Scale the cancel capsule grows from, `getFunctionalButtonInitScale()` | 0.5f |
| CancelSlidePx              | Float    | `mFunctionalButtonOffsetX`: the button starts 20 raw pixels past its resting place | 20f |

#### Methods

| Method Name       | Type            | Description                             |
| ----------------- | --------------- | --------------------------------------- |
| searchBarColors() | SearchBarColors | Creates the default input field colors  |
| searchBarStroke() | MaterialStrokeParams | Resolves the capsule rim and inner shadow |
| causticGlowColor() | Color | Resolves the light under the capsule: `#2EFFFFFF` light, `#33FFFFFF` night. Opt-in: `isCausticShadowEnable()` needs `mSystemCausticShadowState`, which starts `false` |
| metaBallFusion() | Boolean | Whether the cancel capsule may merge with the field: `setBallChain`'s material condition, this library's `isBlurEnabled()` |

### SearchBarActionButton

`COUISearchBar`'s functional button — the 40dp capsule beside the expanded field that carries the
"取消"/"Cancel" label.

| Property Name   | Type                        | Description                                     | Default Value                        | Required |
|-----------------|-----------------------------|-------------------------------------------------|--------------------------------------|----------|
| text            | String                      | Button label                                    | -                                    | Yes      |
| onClick         | () -> Unit                  | Callback invoked on click                       | -                                    | Yes      |
| modifier        | Modifier                    | Modifier applied to the button                  | Modifier                             | No       |
| enabled         | Boolean                     | Whether the button responds to input            | true                                 | No       |
| interactionSource | MutableInteractionSource? | Optional hoisted interaction source             | null                                 | No       |
| containerColor  | Color                       | Capsule fill                                    | the search field's own material      | No       |
| contentColor    | Color                       | Label colour                                    | onSurface at 90%                     | No       |
| glowColor       | Color?                      | Caustic light below the capsule, clipped to its outside; off by default like the field's | null                                 | No       |

`SearchBarActionButtonDefaults` holds the native geometry: `Height` (40dp,
`coui_search_bar_normal_background_height`), `MaxWidth` (152dp,
`coui_search_bar_functional_button_max_width`: `measureFunctionalButton` measures the button
`AT_MOST` that width and the label ellipsizes), `HorizontalPadding` (16dp,
`coui_search_bar_functional_button_text_padding_horizontal`) and `StartGap` (12dp,
`coui_search_bar_functional_button_start_gap`). The label is 14sp Medium
(`couiTextAppearanceButton`).

### SearchBarColors Class
| Property Name   | Type  | Description                                                                       |
| --------------- | ----- | --------------------------------------------------------------------------------- |
| backgroundColor | Color | Capsule fill (toolbar material layer: 80% #262626 at night, 60% white in light)    |
| labelColor      | Color | Label (hint) text color (onSurfaceSecondary, 54% black / 54% white)               |
| iconColor       | Color | Tint of the default leading search icon (onSurfaceSecondary)                      |
| clearIconColor  | Color | Fill of the default clear button circle (onSurfaceContainerHigh)                  |

## Cancel capsule with the melt ball

`COUISearchBar.changeStateWithAnimation` calls `setBallChain()` before it starts the edit
transition: the field and the functional button join one metaball chain
(`OplusMaterialMetaBallParams(1, 0, 4dp, 1.5f, 2.0f)` for the field, index 2 for the button), so the
"取消" capsule grows out of the field's trailing end instead of sliding in beside it. The overload
that owns the field reproduces that.

```kotlin
var query by remember { mutableStateOf("") }
var expanded by remember { mutableStateOf(false) }

SearchBar(
    query = query,
    onQueryChange = { query = it },
    onSearch = { expanded = false },
    onExpandedChange = { expanded = it },
    expanded = expanded,
    label = "Search settings",
    cancelText = "Cancel",
    onCancel = { expanded = false },
) {
    // Search results content
}
```

The chain is only built when the material is on — `mUseMaterialEffectAttr != 0 && isSupportBlur()
&& isBlurEnable()` in `setBallChain` — so `metaBallFusion` defaults to `isBlurEnabled()`. With blur
off, the bar falls back to the low-level form: two separate capsules and the
`outsideEndAction` enter/exit animation.

The transition is the native one. `measureBackground` shrinks the field to
`width - buttonWidth - 12dp`, `doWidthChangedAnimate` lerps that back to the full width, the button
scales from 0.5 and its `translationX` runs from `mFunctionalButtonOffsetX` to zero, all on one
spring: response 0.5s, bounce 0.25 (`StateChangedToEditParamsHigh` and
`StateChangedToNormalParamsHigh` carry the same pair). At rest the capsules are exactly one
`coui_search_bar_functional_button_start_gap` (12dp) apart, which is twice the 6dp fold radius, so
they meet without a visible bridge; the bridge only shows while the button is still travelling in. The merged outline carries the **view-level** stroke, not the melt ball's own material edge: `COUISearchBar` sets `COUIMaterialStrokeEffect`'s `TYPE_FRAMEWORK_CAPSULE_8` (alpha 0.8 light / 0.15 dark, 1.8dp) on both the field and the button, and the chain only merges their silhouettes. The two differ on dark material — the melt ball's `blendEdge` maps the line to `2 * improvedColor`, so over a dark fill it tops out at roughly twice that fill, while the view stroke composites the line colour itself. Measured on the reference device the view stroke draws 78 over a 39 body where the melt-ball blend reaches 47, which reads as a dimmed ring around the outline. The fused path therefore asks for `plainRim` (an internal parameter of `Modifier.nativeMaterialEdge`); the bottom bars keep the melt-ball blend.

The fill is the material itself: `FRAMEWORK_TOP_BAR_PARAMS_DARK`'s first layer `#CC262626` (its `#B37F7F7F` + `OVERLAY` second layer leaves a neutral colour unchanged) and, for the light preset, a flat 60% white that reproduces the preset's `COLOR_DODGE` + `LUMINOSITY` pair (measured 247 over the settings page's 235, which 40% white missed by five levels). In dark it lands slightly below the page (39 on a `#2E2E2E` page, matching the device) instead of the `coui_search_bar_no_blur_solid` `#28767676` grey. When a `glowColor` is supplied it fades in over the last 20% of the edit progress (`remapShadowProgress`), but the default is `null`: the system only paints it when `isCausticShadowEnable()` reports the material feature on, and neither reference device does.


| Property Name   | Type                        | Description                                                    | Default Value                        | Required |
|-----------------|-----------------------------|----------------------------------------------------------------|--------------------------------------|----------|
| query           | String                      | Text content in the search field                               | -                                    | Yes      |
| onQueryChange   | (String) -> Unit            | Callback when the text changes                                 | -                                    | Yes      |
| onSearch        | (String) -> Unit            | Callback when the keyboard's search action fires                | -                                    | Yes      |
| onExpandedChange | (Boolean) -> Unit          | Callback when the expanded state changes                       | -                                    | Yes      |
| modifier        | Modifier                    | Modifier applied to the bar                                    | Modifier                             | No       |
| expanded        | Boolean                     | Whether the bar is expanded                                    | false                                | No       |
| label           | String                      | Placeholder text when empty                                    | ""                                   | No       |
| enabled         | Boolean                     | Whether the field is enabled                                   | true                                 | No       |
| textStyle       | TextStyle?                  | Style of the input text                                        | null                                 | No       |
| colors          | SearchBarColors             | Colors of the field and of the cancel capsule                  | SearchBarDefaults.searchBarColors()  | No       |
| stroke          | MaterialStrokeParams?       | Material rim; on the fused path it follows the merged outline at the preset's own 1.8dp width | SearchBarDefaults.searchBarStroke()  | No       |
| leadingIcon     | @Composable (() -> Unit)?   | Leading icon                                                   | default search icon                  | No       |
| trailingIcon    | @Composable (() -> Unit)?   | Trailing icon                                                  | default clear button                 | No       |
| interactionSource | MutableInteractionSource? | Interaction source                                             | null                                 | No       |
| glowColor       | Color?                      | Caustic light below the capsules, clipped to their outside; off by default (the system's material feature is off on the reference devices) | null | No       |
| backdrop        | Backdrop?                   | Backdrop blurred under the capsules                            | null                                 | No       |
| insideMargin    | DpSize                      | Margin around the bar's content                                | SearchBarDefaults.InsideMargin       | No       |
| cancelText      | String?                     | Label of the cancel capsule, or `null` for a bar without one   | null                                 | No       |
| onCancel        | () -> Unit                  | Callback invoked by the cancel capsule                         | collapses the bar                    | No       |
| metaBallFusion  | Boolean                     | Whether the capsules may merge                                 | SearchBarDefaults.metaBallFusion()   | No       |
| clearContentDescription | String             | Accessibility label of the default clear button               | "Search Cleanup"                     | No       |
| content         | @Composable ColumnScope.() -> Unit | Content shown when expanded                             | {}                                   | No       |

## Advanced Usage

### SearchBar with Icons

```kotlin
var searchText by remember { mutableStateOf("") }
var expanded by remember { mutableStateOf(false) }

SearchBar(
    inputField = {
        InputField(
            query = searchText,
            onQueryChange = { searchText = it },
            onSearch = { /* Handle search action */ },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            leadingIcon = {
                Icon(
                    modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                    imageVector = COUIIcons.Basic.Search,
                    contentDescription = "Search"
                )
            }
        )
    },
    expanded = expanded,
    onExpandedChange = { expanded = it }
) {
    // Search results content
}
```

### SearchBar with Suggestions

```kotlin
var searchText by remember { mutableStateOf("") }
var expanded by remember { mutableStateOf(false) }
val suggestions = listOf("Suggestion 1", "Suggestion 2", "Suggestion 3")

SearchBar(
    inputField = {
        InputField(
            query = searchText,
            onQueryChange = { searchText = it },
            onSearch = { /* Handle search action */ },
            expanded = expanded,
            onExpandedChange = { expanded = it }
        )
    },
    expanded = expanded,
    onExpandedChange = { expanded = it }
) {
    Column {
        suggestions.forEach { suggestion ->
            BasicComponent(
                title = suggestion,
                onClick = {
                    searchText = suggestion
                    expanded = false
                }
            )
        }
    }
}
```

### SearchBar with Cancel Button

```kotlin
var searchText by remember { mutableStateOf("") }
var expanded by remember { mutableStateOf(false) }

SearchBar(
    modifier = Modifier.padding(horizontal = 12.dp),
    inputField = {
        InputField(
            query = searchText,
            onQueryChange = { searchText = it },
            onSearch = { expanded = false },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            label = "Search"
        )
    },
    expanded = expanded,
    onExpandedChange = { expanded = it },
    outsideEndAction = {
        // The SearchBar already provides COUI-accurate spacing around this slot.
        Text(
            modifier = Modifier
                .clickable(
                    interactionSource = null,
                    indication = null
                ) {
                    expanded = false
                    searchText = ""
                },
            text = "Cancel",
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            color = COUITheme.colorScheme.primary
        )
    }
) {
    // Search results content
}
```

> The `outsideEndAction` slot is for a caller that composes its own end action. When the action is
> the system's cancel capsule, use the query-based overload with `cancelText` instead: only that
> form can merge the two capsules into one melt ball, because the merged silhouette has to carry the
> field's own fill and rim.