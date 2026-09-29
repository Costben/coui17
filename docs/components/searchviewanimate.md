# SearchViewAnimate

`SearchViewAnimate` is the ColorOS "magnetic" search bar. It animates between two states: collapsed, where only a search glyph shows, and expanded, where the input field and a trailing Cancel button slide into place. The bar's height, vertical offset, content alpha and background tint are all derived from a single collapse progress value fed by its host, which is what makes it feel magnetically attached to the content that scrolls under it.

It ports `com.coui.appcompat.searchview.COUISearchViewAnimate` and its layout `res/layout/coui_search_view_animate_layout.xml`.

<div style="position: relative; height: 250px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../compose/index.html?id=searchViewAnimate" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.SearchViewAnimate
import io.github.suqi8.coui.kmp.basic.SearchViewAnimateDefaults
import io.github.suqi8.coui.kmp.basic.SearchViewAnimateType
import io.github.suqi8.coui.kmp.basic.SearchViewState
```

## Native Origin

This component is a port of `COUISearchViewAnimate`, which is **not** the same class as `COUISearchBar` — [SearchBar](../components/searchbar) ports that one, and it is a plain capsule that opens a result panel. The two have no shared state model, so they live in separate files and `SearchBar`'s public API is unaffected.

The native widget carries `STATE_NORMAL` (0) and `STATE_EDIT` (1), exposed here as `SearchViewState`, and two layouts: `TYPE_INSTANT_SEARCH` (0), where the trailing button sits directly after the input field, and `TYPE_NON_INSTANT_SEARCH` (1), where the 1dp `search_linear_divider` is inserted between them. `mSearchViewType` starts at 1, so `NonInstantSearch` is the default.

## Basic Usage

```kotlin
var query by remember { mutableStateOf("") }
var expanded by remember { mutableStateOf(false) }

SearchViewAnimate(
    query = query,
    onQueryChange = { query = it },
    expanded = expanded,
    onExpandedChange = { expanded = it },
    hint = "Search settings",
    modifier = Modifier.fillMaxWidth()
)
```

## Collapse Progress

`collapsePercent` is `setSearchViewAnimateHeightPercent`: the host feeds it the collapse progress of whatever the bar is attached to (0 = fully collapsed to the glyph, 1 = fully open), and the bar derives everything else from it.

```kotlin
var collapsePercent by remember { mutableFloatStateOf(1f) }

Column {
    SearchViewAnimate(
        query = query,
        onQueryChange = { query = it },
        expanded = expanded,
        onExpandedChange = { expanded = it },
        hint = "Search",
        collapsePercent = collapsePercent,
        modifier = Modifier.fillMaxWidth()
    )
    Slider(value = collapsePercent, onValueChange = { collapsePercent = it })
}
```

The derived values are:

| Output | Formula |
| :--- | :--- |
| Wrapper height | `max(coui_search_view_collapsed_min_height, wrapperHeight * (f / 0.7 - 0.42857146))` |
| Vertical offset | `(animateHeight / 2) * (1 - f)` |
| Content alpha | `(f - 0.5) * 2` |
| Background | `ArgbEvaluator(clamp(f / 0.3, 0, 1))` from `couiColorDivider` to `coui_search_view_selector_color_normal` |

## Parameters

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `query` | `String` | The text shown in the input field |
| `onQueryChange` | `(String) -> Unit` | Called with the updated text |
| `expanded` | `Boolean` | Whether the bar is in `SearchViewState.Edit` |
| `onExpandedChange` | `(Boolean) -> Unit` | Called when the bar wants to enter or leave the edit state |
| `modifier` | `Modifier` | The modifier to be applied to the bar |
| `hint` | `String` | The placeholder shown while `query` is empty |
| `type` | `SearchViewAnimateType` | `InstantSearch` or `NonInstantSearch` |
| `collapsePercent` | `Float` | The collapse progress, 0..1 |
| `onSearch` | `(String) -> Unit` | Called with the current query when the IME search action fires |
| `cancelText` | `String` | The label of the trailing button (`string/coui_search_view_cancel`) |
| `cancelButton` | `(@Composable () -> Unit)?` | Replaces the trailing button entirely |
| `leadingIcon` | `ImageVector` | The glyph shown while collapsed |
| `colors` | `SearchViewAnimateColors` | The colors of the bar |

## Animation

Enter and exit are `initSmoothEnterAnimatorSet` / `initSmoothExitAnimatorSet`, all on `COUIMoveEaseInterpolator` (`pathInterpolator(0.3, 0, 0.1, 1)`):

| Track | Duration | Constant |
| :--- | :--- | :--- |
| Offset | 450ms | `DEFAULT_SEARCH_VIEW_OFFSET_CHANGE_DURATION` |
| Scale | 450ms | `DEFAULT_SEARCH_VIEW_SCALE_CHANGE_DURATION` |
| Button alpha, instant search | 350ms | `DEFAULT_BUTTON_ALPHA_CHANGE_DURATION` |
| Button alpha, non-instant search | 100ms | `NON_INSTANT_SEARCH_BUTTON_ALPHA_CHANGE_DURATION` |

The press feedback (`playPressAnimation` / `playReleaseAnimation`) is a 150ms `COUIEaseInterpolator` (`pathInterpolator(0.33, 0, 0.67, 1)`) ramp between the normal and pressed selectors.

## Recovered Tokens

Every value below was recovered from the ColorOS 17 resource dump of `Settings.apk`; none of them is invented.

| Token | Value | Source resource |
| :--- | :--- | :--- |
| `WrapperHeight` | 40dp | `dimen/coui_search_view_wrapper_height` |
| `AnimateHeight` | 50dp | `dimen/coui_search_view_animate_height` |
| `CollapsedMinHeight` | 1dp | `dimen/coui_search_view_collapsed_min_height` (declared `1px`) |
| `IconSize` | 24dp | `dimen/coui_search_view_icon_size` |
| `SearchIconMargin` | 12dp | `dimen/coui_search_view_search_icon_margin` |
| `CornerRadius` | 18dp | `dimen/coui_search_view_corner` |
| `InputTextSize` | 15sp | `dimen/coui_search_view_input_text_size` |
| `CancelTextSize` | 14sp | `dimen/coui_search_view_cancel_text_size` |
| `CancelButtonBackgroundRadius` | 24dp | `dimen/coui_searchview_cancel_button_bg_radius` |
| `CancelButtonMinHeight` | 36dp | `dimen/coui_search_bar_inner_search_icon_size` |
| `CancelButtonPaddingHorizontal` | 8dp | `dimen/coui_search_cancel_button_bg_padding_horizontal` |
| `CancelButtonPaddingVertical` | 4dp | `dimen/coui_search_cancel_button_bg_padding_vertical` |
| `CancelMarginSmall` | 4dp | `dimen/coui_search_view_cancel_margin_small` |
| `CancelMarginLarge` | 8dp | `dimen/coui_search_view_cancel_margin_large` |
| `ButtonDividerMarginStart` | 0dp | `dimen/coui_search_view_button_divider_margin_start` |
| `DividerWidth` | 1dp | `dimen/coui_search_linear_divider_width` |
| `DividerHeight` | 16dp | `drawable/search_linear_divider` `android:height` |
| Background, normal | `#14000000` light / `#1affffff` night | `color/coui_search_view_selector_color_normal` |
| Background, pressed | `#1e000000` light / `#33ffffff` night | `color/coui_search_view_selector_color_pressed` |
| Hint | `#23000000` light / `#33ffffff` night | `color/coui_search_view_hint_color` |
| Input text | `#ff000000` light / `#ffffffff` night | `color/coui_search_view_input_text_color` |
| Cancel text | `#ff000000` light / `#ffffffff` night | `color/coui_searchview_cancel_text_color` |
| Divider | `#1a000000` light / `#33ffffff` night | `color/coui_search_view_linear_divider_color` |
| Cancel label | `"Cancel"` | `string/coui_search_view_cancel` |
| `ClampAnimationPercent` | 0.3 | `COUISearchViewAnimate.CLAMP_ANIMATION_PERCENT` |
| `MoveEase` | `cubic-bezier(0.3, 0, 0.1, 1)` | `COUIMoveEaseInterpolator` |
| `Ease` | `cubic-bezier(0.33, 0, 0.67, 1)` | `COUIEaseInterpolator` |

Padding notes recovered from the layout: `coui_search_view_background_start_gap` (40dp) and `coui_search_view_background_end_gap` (4dp) set the input field's start/end clearance in the non-instant layout, and `coui_search_view_auto_complete_padding_end` (16dp) is the input's end padding.

## Not Ported

- **`COUISearchViewAnimate$AnimatorHelper`** — the class could not be decompiled from `classes.dex`, so the exact dispatch inside `runStateChangeAnimation(int)` is not recovered. The port reproduces the tracks and durations that `initSmoothEnterAnimatorSet` / `initSmoothExitAnimatorSet` compose, but not the intermediate keyframes that helper may add.
- **`COUIHintAnimationLayout`** — the layout that rotates the hint text between the collapsed and expanded states. It is a separate class and is not part of this port; the port shows the hint as a plain placeholder.
- **The toolbar attachment offsets** — the native offsets the bar against a toolbar's height and inset; that arithmetic is host-side and stays with the caller here.
- **`SearchViewAnimateDefaults.CornerRadius`** — exposed for callers that want the native 18dp value, but the bar itself clips with `CircleShape`, because the recovered height (40dp) makes the capsule radius 20dp and the native radius is clamped to `height / 2`.
