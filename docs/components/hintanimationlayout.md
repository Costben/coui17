# HintAnimationLayout

`HintAnimationLayout` is the rotating hint of a search bar. It draws two hint strings over the search field and, every few seconds, slides the current one out while the next one slides in, both cross-fading along the way. An empty query shows the rotation; typing a query stops it and hides the hints, exactly as the native `TextWatcher` does.

It is aligned with the ColorOS 17 search-bar hint rotation.

<div style="position: relative; height: 250px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../compose/index.html?id=hintAnimationLayout" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.HintAnimationDefaults
import io.github.suqi8.coui.kmp.basic.HintAnimationLayout
```

## Native Origin

The native view is a `FrameLayout` holding two hint `TextView`s (`mHintTextViewFirst` / `mHintTextViewThen`). Every `INTERVAL_TIME` it advances the hint index by one modulo the hint list and `applyHintChangeAnimation` plays two `AnimatorSet`s of `DURATION_TIME`. The outgoing text translates by `-distance` while fading out; the incoming text enters from `+distance` while fading in, and its set starts `IN_DELAY_TIME` later. The translate track uses `TRANSLATEINTERPOLATOR`, a `PathInterpolatorCompat.create(0.3f, 0f, 0.2f, 1f)`, while the alpha track uses `EASEINTERPOLATOR`, a `COUIEaseInterpolator` (`PathInterpolator(0.33f, 0f, 0.67f, 1f)`).

`COUIHintAnimationLayout` attaches a `TextWatcher` to the search `EditText` through `setSearchEditText` — or by taking child 0, which `coui_search_view_animated_support_layout.xml` relies on. A non-empty query cancels the pending rotation, hides both hint views and resets their translation and alpha; an empty query puts the last string back. In Compose the text field owns its own value, so the current text is passed in as `query`.

This component is the one [SearchViewAnimate](../components/searchviewanimate) leaves out; the two are separate classes and this one has its own public API.

## Basic Usage

```kotlin
var query by remember { mutableStateOf("") }
val hints = listOf("Search settings", "Search messages", "Search apps and files")

HintAnimationLayout(
    hints = hints,
    query = query,
    modifier = Modifier.fillMaxWidth()
) {
    InputField(
        query = query,
        onQueryChange = { query = it },
        onSearch = {},
        expanded = false,
        onExpandedChange = {},
        leadingIcon = {},
        label = "",
        modifier = Modifier.fillMaxWidth()
    )
}
```

## Parameters

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `hints` | `List<String>` | The strings to rotate through; an empty list (or a list of empty strings) stops the rotation |
| `modifier` | `Modifier` | The modifier applied to the layout |
| `query` | `String` | The current text of the search field; a non-empty value hides the hints and stops the rotation |
| `enabled` | `Boolean` | Whether the rotation runs |
| `repeatCount` | `Int` | How many transitions to play before stopping; `-1` rotates until the composition changes |
| `textSize` | `TextUnit` | The hint text size; `TextUnit.Unspecified` keeps the size of `style` |
| `textColor` | `Color` | The hint colour; `Color.Unspecified` keeps the style's colour |
| `style` | `TextStyle` | The hint text style, defaulting to the theme's `couiTextAppearanceBodyL` tier |
| `hintStartPadding` | `Dp` | The start inset of the hint, so it sits behind the search field's leading icon |
| `onHintAnimationChange` | `(index: Int, hint: String) -> Unit` | Called when a transition starts, with the new index and the incoming string |
| `content` | `@Composable BoxScope.() -> Unit` | The search field the hints are drawn over |

### HintAnimationDefaults

| Constant | Type | Value | COUI Source |
| :--- | :--- | :--- | :--- |
| `DurationMillis` | `Int` | 250 | `COUIHintAnimationLayout.DURATION_TIME` |
| `InDelayMillis` | `Long` | 150 | `COUIHintAnimationLayout.IN_DELAY_TIME` |
| `IntervalMillis` | `Long` | 3000 | `COUIHintAnimationLayout.INTERVAL_TIME` |
| `RepeatCount` | `Int` | -1 | `COUIHintAnimationLayout.mRepeatCount` |
| `TranslateExtra` | `Dp` | 5.dp | `dimen/coui_search_bar_animation_translate_extra` |

`textStyle()` is the default hint style: `COUIHintAnimationLayout` themes both hint views with `couiTextAppearanceBodyL` (16sp, `coui_spacing_multiplier_body_l`) inside a `Widget_COUI_EditText_SearchViewStyle_HintText` wrapper, and the port uses the theme's `body1`, the same 16sp tier.

## Animation

One transition advances the hint index modulo the list, then plays both tracks for `DurationMillis`:

| Track | Target | Duration | Easing |
| :--- | :--- | :--- | :--- |
| Outgoing translate | `-distance` | 250ms | `TRANSLATE_EASING` |
| Outgoing alpha | 0 | 250ms | `EASE_EASING` |
| Incoming translate | 0 (from `+distance`) | 250ms, delayed 150ms | `TRANSLATE_EASING` |
| Incoming alpha | 1 (from 0) | 250ms, delayed 150ms | `EASE_EASING` |

`distance` is `(measuredHeight - lineHeight) / 2 + TranslateExtra`, which walks each text just past the clipped edge. The two slots each keep one pair of sliders and reuse it, reproducing the native's re-targeting of an existing `AnimatorSet`.

## Recovered Tokens

Every value below is aligned with the ColorOS 17 implementation; none of them is invented.

| Token | Value | Source |
| :--- | :--- | :--- |
| `DurationMillis` | 250ms | ColorOS 17 hint animation |
| `InDelayMillis` | 150ms | ColorOS 17 hint animation |
| `IntervalMillis` | 3000ms | ColorOS 17 hint animation |
| `RepeatCount` | -1 | ColorOS 17 hint animation |
| `TranslateExtra` | 5dp | `dimen/coui_search_bar_animation_translate_extra` |
| `TranslateEasing` | `cubic-bezier(0.3, 0, 0.2, 1)` | ColorOS 17 translate interpolator |
| `EaseEasing` | `cubic-bezier(0.33, 0, 0.67, 1)` | ColorOS 17 ease interpolator |
| Hint text appearance | `couiTextAppearanceBodyL` (16sp) | `Widget_COUI_EditText_SearchViewStyle_HintText` |
| Distance | `(measuredHeight - lineHeight) / 2 + TranslateExtra` | `applyHintChangeAnimation` |

## Not Ported

- **`setHintsAnimation`'s refusal to run when the field already holds text** — the native logs through `TAG` and calls `mSearchEditText.setHint("")`; the port simply stops rotating while `query` is not empty and leaves the field's own placeholder to the caller.
- **`setSearchEditText` and the "child 0 must be an `EditText`" fallback**, plus the two `Log.e` guards that enforce them; the search field is supplied by the caller through `content`.
- **`setHintTextColor(ColorStateList)`** — `textColor` carries a single `Color`; `Color.Unspecified` keeps the style's own colour.
- **`setTextSize(int)` copying the `EditText`'s pixel text size** — `textSize` is a `TextUnit` override instead.
- **`onAttachedToWindow` / `onDetachedFromWindow` / `onWindowVisibilityChanged`** and the imperative `pauseHintsAnimation` / `resumeHintsAnimation` pair; the rotation is tied to the composition, `enabled` and `query`.
- **`getCurrentHintTextView()` and the `TextView` argument of `COUIHintAnimationChangeListener`** — `onHintAnimationChange` reports the index and the string only.
- **The `id/coui_hint_text_view_first` / `id/coui_hint_text_view_then` view ids**, which the native assigns to its own children.
