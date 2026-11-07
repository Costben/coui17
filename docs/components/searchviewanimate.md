# SearchViewAnimate

`SearchViewAnimate` is the ColorOS "magnetic" search bar. It animates between two states: collapsed, where the glyph and the hint sit in the capsule, and expanded, where the input field and a trailing Cancel button take over. The bar's height, vertical offset, content alpha and background tint are all derived from a single collapse progress value fed by its host, which is what makes it feel magnetically attached to the content that scrolls under it.

It is aligned with the ColorOS 17 magnetic search bar.

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

## The two states

`changeStateWithOutAnimation` only hides the functional button and the divider in `STATE_NORMAL`; the search view itself keeps `alpha = 1`, so **the hint stays readable while the bar is collapsed**. That is the state the bar sits in inside a toolbar, and the reason this port no longer fades the input out when `expanded` is false.

Entering `STATE_EDIT` fades the button in and narrows the input field to make room for it; the glyph is left alone. `startSearchIconEnterAnimator` / `startSearchIconExitAnimator` — the helpers that flip the glyph 80° around its start edge — are public API on the native class that no code inside `COUISearchViewAnimate` ever calls, so the glyph only turns when a host drives it.

A non-empty query shows the SearchView's clear affordance (`search_close_btn`): an 18dp `couiColorHintNeutral` disc with a white cross, in the 36dp slot the native view reserves at the end of the input. Clearing it reports an empty string through `onQueryChange`; the label is `SearchViewAnimateDefaults.CLEAR_TEXT`.

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
| Glyph and input alpha | `(f - 0.5) * 2` |
| Background | `ArgbEvaluator(clamp(f / 0.3, 0, 1))` from `couiColorDivider` to `coui_search_view_selector_color_normal` |

The alpha applies to the glyph and the input field only: the background is filled by `onDraw` and never turns transparent. The blend is a plain sRGB channel interpolation, the way `ArgbEvaluator` computes it, not the Oklab mix `Color.lerp` performs.

## Parameters

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `query` | `String` | The text shown in the input field |
| `onQueryChange` | `(String) -> Unit` | Called with the updated text |
| `expanded` | `Boolean` | Whether the bar is in `SearchViewState.Edit` |
| `onExpandedChange` | `(Boolean) -> Unit` | Called when the bar wants to enter or leave the edit state |
| `modifier` | `Modifier` | The modifier to be applied to the bar |
| `hint` | `String` | The placeholder shown while `query` is empty, including when collapsed |
| `type` | `SearchViewAnimateType` | `InstantSearch` or `NonInstantSearch` |
| `collapsePercent` | `Float` | The collapse progress, 0..1 |
| `onSearch` | `(String) -> Unit` | Called with the current query when the IME search action fires |
| `cancelText` | `String` | The label of the trailing button (`string/coui_search_view_cancel`) |
| `cancelButton` | `(@Composable () -> Unit)?` | Replaces the trailing button entirely |
| `leadingIcon` | `ImageVector` | The glyph at the start |
| `colors` | `SearchViewAnimateColors` | The colors of the bar |

## Animation

The trailing button and the divider are the only tracks this port animates between the two states, both on `COUIMoveEaseInterpolator` (`pathInterpolator(0.3, 0, 0.1, 1)`):

| Track | Duration | Constant |
| :--- | :--- | :--- |
| Button alpha, instant search | 350ms, 100ms start delay | `DEFAULT_BUTTON_ALPHA_CHANGE_DURATION`, `setStartDelay(... ? 0L : 100L)` |
| Button alpha, non-instant search | 100ms | `NON_INSTANT_SEARCH_BUTTON_ALPHA_CHANGE_DURATION` |

The divider is `GONE` while collapsed and rides the button's alpha track, so it fades rather than sliding open.

The press feedback (`playPressAnimation` / `playReleaseAnimation`) is a 150ms `COUIEaseInterpolator` (`pathInterpolator(0.33, 0, 0.67, 1)`) ramp between the normal and pressed selectors.

## Recovered Tokens

Every value below is aligned with the ColorOS 17 implementation; none of them is invented.

| Token | Value | Source resource |
| :--- | :--- | :--- |
| `WrapperHeight` | 40dp | `dimen/coui_search_view_wrapper_height` |
| `AnimateHeight` | 50dp | `dimen/coui_search_view_animate_height` |
| `CollapsedMinHeight` | 1dp | `dimen/coui_search_view_collapsed_min_height` (declared `1px`; `CollapsedMinHeightPx` = 1) |
| `ICON_ROTATION_Y` | 80° | `mSearchIcon.animate().rotationY(80.0f)`; not applied by the widget itself |
| `ICON_ROTATION_DURATION_MILLIS` | 150 | the same call's `setDuration(150L)` |
| `IconSize` | 24dp | `dimen/coui_search_view_icon_size` |
| `SearchIconMargin` | 12dp | `dimen/coui_search_view_search_icon_margin` |
| `IconInputGap` | 8dp | `animated_search_view`'s `android:layout_marginStart` |
| `CornerRadius` | 18dp | `dimen/coui_search_view_corner` |
| `InputTextSize` | 15sp | `dimen/coui_search_view_input_text_size` |
| `InputEndPadding` | 16dp | `dimen/coui_search_view_auto_complete_padding_end` |
| `CancelTextSize` | 14sp | `dimen/coui_search_view_cancel_text_size` |
| `CancelButtonBackgroundRadius` | 17dp | `drawable/coui_searchview_cancel_button_bg`'s `android:radius` (the `dimen/coui_searchview_cancel_button_bg_radius` 24dp is unused) |
| `CancelButtonMinHeight` | 0dp | the layout's `android:minHeight` |
| `CancelButtonMaxWidth` | 152dp | `dimen/coui_search_function_button_max_width` |
| `CancelButtonPaddingHorizontal` | 8dp | `dimen/coui_search_cancel_button_bg_padding_horizontal` |
| `CancelButtonPaddingVertical` | 4dp | `dimen/coui_search_cancel_button_bg_padding_vertical` |
| `CancelMarginSmall` | 4dp | `dimen/coui_search_view_cancel_margin_small` |
| `CancelMarginLarge` | 8dp | `dimen/coui_search_view_cancel_margin_large` |
| `ButtonDividerMarginStart` | 0dp | `dimen/coui_search_view_button_divider_margin_start` |
| `DividerWidth` | 1dp | `dimen/coui_search_linear_divider_width` |
| `DividerHeight` | 16dp | `drawable/search_linear_divider` `android:height` |
| `ClearButtonSize` / `DiscSize` / `GlyphSize` | 36dp / 18dp / 12dp | `ic_edit_text_delete_search_view`, a 36dp viewport with a radius-9 circle |
| Background, normal | `#14000000` light / `#1affffff` night | `color/coui_search_view_selector_color_normal` |
| Background, pressed | `#1e000000` light / `#33ffffff` night | `color/coui_search_view_selector_color_pressed` |
| Hint | `#8a000000` light / `#33ffffff` night | `normalHintColor`: `couiColorLabelSecondary` light, `coui_search_view_hint_color_dark` night |
| Input text | `#e6000000` light / `#ffffffff` night | `inputTextColor`: `couiColorPrimaryNeutral` light, `coui_search_view_input_text_color_dark` night |
| Cancel text | `#0080ff` light / `#5c9dff` night | `functionalButtonTextColor`: `couiColorPrimaryText` |
| Glyph | `#8a000000` light / `#8affffff` night | `couiColorSecondNeutral` |
| Clear disc | `#42000000` light / `#4dffffff` night | `couiColorHintNeutral` |
| Divider | `#1f000000` light / `#33ffffff` night | `color/coui_color_divider` |
| Cancel label | `"Cancel"` | `string/coui_search_view_cancel` |
| `ClampAnimationPercent` | 0.3 | `COUISearchViewAnimate.CLAMP_ANIMATION_PERCENT` |
| `MoveEase` | `cubic-bezier(0.3, 0, 0.1, 1)` | `COUIMoveEaseInterpolator` |
| `Ease` | `cubic-bezier(0.33, 0, 0.67, 1)` | `COUIEaseInterpolator` |

Padding notes recovered from the layout: `coui_search_view_background_start_gap` (40dp) and `coui_search_view_background_end_gap` (4dp) set the input field's start/end clearance in the non-instant layout.

## Not Ported

- **The whole-view offset and the input's end margin** — `initSmoothEnterAnimatorSet` / `initSmoothExitAnimatorSet` move the view against its toolbar and animate the SearchView's `marginEnd`; both depend on the host's toolbar geometry and stay with the caller, so this port only carries their 450ms / `COUIMoveEaseInterpolator` constants (`REVEAL_DURATION_MILLIS`).
- **The split background** — once the non-instant layout is expanded, `mDivideBackground` redraws the fill as two half-capsules that meet at the divider. The visible result differs from a single capsule by the 1dp seam, which this port keeps as one shape.
- **The hint-rotation layout** — `COUIHintAnimationLayout` rotates several hint strings; it is a separate component and this port shows the hint as a plain placeholder.
- **The toolbar attachment offsets** — the native offsets the bar against a toolbar's height and inset; that arithmetic is host-side and stays with the caller here.
- **`SearchViewAnimateDefaults.CornerRadius`** — exposed for callers that want the native 18dp value, but the bar itself clips with `CircleShape`, because the recovered height (40dp) makes the capsule radius 20dp and the native radius is clamped to `height / 2`.
