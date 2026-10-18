# AppBarBlurOverlay

`AppBarBlurOverlay` is the ColorOS 17 app bar material overlay: the blurred, scroll-driven surface that sits over a top bar. It is a Compose port of that surface.

In ColorOS the overlay is the view the layout behavior sizes, and the app bar itself is hollowed out — the material lands on the overlay while the bar is left with no background and no elevation. The overlay paints nothing of its own: a helper gives it a gradient alpha ramp plus a progressive blur, and the behavior drives the view's whole alpha from the scroll offset.

Two anchors matter, and they are not the same one. **The caller sizes the node**: the overlay view is inflated at `layout_height=0dp` and resized by the behavior to `appBar.top .. appBar.bottom + extraHeightPx`, so pass `Modifier.height(appBarHeight + extraHeight)`. The alpha ramp is anchored to the **bar's bottom edge** inside that node, while the progressive blur is full strength along the node's top edge.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlay
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlayDefaults
import io.github.suqi8.coui.kmp.basic.couiGlassHeader
```

## Native Origin

The overlay view is inflated at `layout_height=0dp` under a layout behavior that measures it to `appBar.top .. appBar.bottom + extraHeightPx` — the app bar's own bounds plus `homepage_appbar_blur_extra_height` (14dp), which is `AppBarBlurOverlayDefaults.ExtraHeight`. The view carries no `onDraw` and no `onSizeChanged`: it is a canvas that a helper dresses.

The material's background resolves into a gradient drawable and its blur into a `RenderEffect`; the sibling overlay is preferred over the app bar itself, which is why the host hollows the bar out. The material's gradient lives in the overlay's background drawable rather than in the `RenderEffect`, so the ramp is painted even where no blur can run.

The gradient is built bottom-to-top, i.e. the ramp's positions are measured **from the bar's bottom edge upward**, with `p = 1` at the bar's top edge. The extra band below the bar is therefore transparent, and the material reads as the bar's own surface rather than as a taller gradient.

The framework's uniform path — a normal blur background with `normalBlurRadiusPx` and `normalBackgroundAlphaFraction` — is dead code: it has no caller in ColorOS 17, so it is deliberately not implemented.

## Basic Usage

```kotlin
val density = LocalDensity.current
var scrollPx by remember { mutableStateOf(0f) }
val barHeight = TopAppBarDefaults.CollapsedHeight

Box(Modifier.fillMaxSize()) {
    ScrollingContent(onScroll = { scrollPx = it })

    AppBarBlurOverlay(
        // The node carries the bar plus the 14dp band below it.
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight + AppBarBlurOverlayDefaults.ExtraHeight),
        scrollFraction = AppBarBlurOverlayDefaults.scrollAlpha(scrollPx, density),
    ) {
        // The bar's own content, hollowed out: the material fades in beneath it.
        SmallTopAppBar(title = "Title")
    }
}
```

The overlay needs the content it samples through `LocalBackdrop`; place it where a backdrop is provided, or pass `backdrop = null` for the plain ramp alone. When the platform cannot run runtime shaders (everything below Android 13) the blur is skipped as well, and only the ramp is painted.

### Resting Alpha Floor

ColorOS 17's home page never removes the material completely: with the header fully expanded it still keeps a layer of surface colour. The downstream project measured that floor as `0.55` on a PLK110 running ColorOS 17, and the `minAlpha` overload expresses it directly in the `scrollFraction` producer:

```kotlin
val scrollPx = remember { mutableFloatStateOf(0f) }
val scrollFraction = remember(density) {
    {
        AppBarBlurOverlayDefaults.scrollAlpha(
            scrollPx = scrollPx.floatValue,
            density = density,
            minAlpha = 0.55f,
        )
    }
}
```

`minAlpha` is coerced into `0f..1f`. The result stays at the floor at rest, then ramps from `minAlpha` to `1` over the same 30dp threshold as the two-argument overload.

### Follow Content Displacement, Not the Collapsed Fraction

Drive `scrollFraction` from the content's actual displacement (`scrollPx`), not from the app bar's collapsed fraction. The two only agree while the list is at its top.

The downstream project originally bound the material alpha to the collapse ratio and saw the material visibly thin out whenever the list was dragged down from the middle: a 240px downward drag brought the avatar row back, and the alpha fell from `1.0` to `0.55` while the bar was expanding. After driving the alpha from displacement, the same 240px drag kept the status-bar band luminance at 222–225 (0–255), matching the 223–235 baseline of the collapsed state, and the dip was gone: the value now moves with the content, in the same direction as the list.

The collapsible bar can follow the same displacement with `COUIScrollBehavior(expandOnContentScroll = true)`, which expands the bar on a downward drag anywhere in the list rather than only at its top (see [TopAppBar](./topappbar)).

## Glass Header

For a long page header — the ColorOS 17 "Me" page is the reference — use `Modifier.couiGlassHeader`. It paints a top-weighted progressive blur and holds the container colour down to a fade band at the bottom edge, so the header dissolves into the content instead of ending on a hard line. The downstream project measured that fade band as roughly 60–80px (about 18dp on a 560dpi device) and reported a smooth edge with no visible seam.

```kotlin
val backdrop = rememberLayerBackdrop {
    drawRect(Color.Black)
    drawContent()
}

Box(Modifier.fillMaxSize()) {
    // The backdrop is a sibling of the header, never its ancestor.
    Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
        Content()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .couiGlassHeader(
                backdrop = backdrop,
                containerColor = COUITheme.colorScheme.surface,
            ),
    ) {
        // Header content.
    }
}
```

`fadeHeight` defaults to `GlassDefaults.HeaderFadeHeight` (28dp); pass `fadeHeight = 18.dp` to match the downstream page. When `backdrop` is `null` — or the platform cannot run runtime shaders — the modifier falls back to an opaque `containerColor` fill.

## Parameters

### AppBarBlurOverlay Properties

| Property Name  | Type                       | Description                                                                                | Default Value                        | Required |
| -------------- | -------------------------- | ------------------------------------------------------------------------------------------ | ------------------------------------ | -------- |
| modifier       | Modifier                   | Applied to the overlay; the caller sizes the node to the app bar plus `extraHeight`        | Modifier                             | No       |
| containerColor | Color                      | The base colour the alpha ramp is drawn in; light `#f0f1f2`, dark `#333333` (see Recovered Tokens) | AppBarBlurOverlayDefaults.containerColor() | No       |
| backdrop       | Backdrop?                  | The content to sample; `null` skips the blur and paints the ramp only                       | LocalBackdrop.current                | No       |
| extraHeight    | Dp                         | How far the overlay reaches past the app bar's bottom edge                                  | AppBarBlurOverlayDefaults.ExtraHeight | No       |
| scrollFraction | Float                      | The scroll-driven alpha, `min(1, scrollPx / 30dp)`; at `0` the overlay draws nothing         | 1f                                   | No       |
| content        | @Composable BoxScope.() -> Unit | The app bar content the caller hollows out; it is not part of the material            | {}                                   | No       |

`content` is drawn on top of the material and is not affected by `scrollFraction`: the title and the actions stay readable at rest, and only the material fades in with the scroll.

`containerColor` is only the colour the ramp is drawn in; which ramp the theme picks — the three-stop light one or the two-stop dark one — is decided by the theme, not by this colour (see Recovered Tokens).

### AppBarBlurOverlayDefaults Object

| Constant / Function   | Type  | Description                                                            | Value / Source |
| --------------------- | ----- | ---------------------------------------------------------------------- | -------------- |
| ExtraHeight           | Dp    | How far the overlay reaches past the bar's bottom edge                 | 14.dp (`homepage_appbar_blur_extra_height`) |
| ScrollAlphaThresholdDp | Float | The scroll distance over which the overlay's alpha ramps from 0 to 1   | 30f |
| DarkContainerColor    | Color | The dark-theme material base colour, taken from the nearest ColorOS night token rather than from the framework attribute | `#333333` (`coui_color_background_top` / `coui_color_surface_top`) |
| containerColor()      | Color | The base colour for the current theme: the light theme's `surface`, the dark theme's `DarkContainerColor` | `#f0f1f2` / `#333333` |
| scrollAlpha(scrollPx, density) | Float | `min(1, scrollPx / ScrollAlphaThresholdDp)`, coerced into `0f..1f` | — |
| scrollAlpha(scrollPx, density, minAlpha) | Float | The same ramp remapped into `minAlpha..1`, with `minAlpha` coerced into `0f..1f` | — |

Feed the `scrollAlpha` result to the overlay's `scrollFraction`: at rest the overlay is invisible, and it reaches full strength once the content has scrolled `ScrollAlphaThresholdDp` (30dp) beneath the bar.

## Recovered Tokens

Every value below is taken from the ColorOS 17 material's defaults or its resource table; none of them is invented.

| Token | Value | Source |
| :--- | :--- | :--- |
| `ExtraHeight` | 14dp | `homepage_appbar_blur_extra_height` |
| `ScrollAlphaThresholdDp` | 30 | ColorOS 17 scroll threshold |
| `startBlurRadius` | 30px | ColorOS 17 start blur radius |
| `endBlurRadius` | 0px | ColorOS 17 end blur radius |
| Blend curve | `pow(1 - t, 3.0)` | ColorOS 17 gradient blur speed = 3.0f |
| Light stop positions | `[0.0, 0.4, 1.0]` | ColorOS 17 light ramp |
| Light stop alphas | `[0.0, 0.7, 1.0]` | ColorOS 17 light ramp |
| Dark stop positions | `[0.0, 1.0]` | ColorOS 17 dark ramp |
| Dark stop alphas | `[0.0, 0.4]` | ColorOS 17 dark ramp |

The dark theme has only two stops: the `0.4` top-edge alpha is a hard-coded value, not a third `gradientAlpha` value. The blur radius and the blend exponent are carried by `AppBarGlassDefaults` in `TopAppBar.kt` (`GradientBlurRadiusPx`, `GradientEndBlurRadiusPx`, `GradientBlurSpeed`), which is also where the app bar's own `gradientGlass` material reads them from; `endBlurRadius` is 0 because the gradient shape blurs at `max(start, end)` and lets the blend curve do the ramping.

Which of the two ramps is drawn is a property of the theme, not of the base colour: the port selects the dark ramp when `COUITheme.colorScheme.background.luminance() < 0.5f`, so passing a dark `containerColor` on a light theme cannot move the ramp to the other branch.

The dark base colour itself cannot be read off the same chain. ColorOS resolves it from `?couiColorBackgroundWithCard`, which is `#ffffff` in the night configuration — a colour that cannot produce the profile the material shows, where the material's top edge lands at 60 over a `#454545` card and, with the dark ramp's hard-coded top alpha, `0.4 * B + 0.6 * 69 = 60` gives `B ≈ 46.5`. The nearest ColorOS night token is `coui_color_background_top` / `coui_color_surface_top` = `#333333`, which is what `DarkContainerColor` takes. No explicit colour can be hiding the gap: the gradient-blur background colour is never assigned in ColorOS 17, so its static never leaves `null` and the page fallback is what resolves; the dark path carries a further darkening layer this port cannot reproduce.

## Not Ported

- **The uniform (normal blur background) path** — `normalBlurRadiusPx` (50px), `normalBackgroundAlphaFraction` (0.8) and the dark alpha (0.6) have no caller in ColorOS 17, so this port only implements the gradient shape.
- **The `scrollDrivenAlphaEnabled` attribute** — the native view reads its alpha from the scroll offset when the attribute is set; the port takes the resolved alpha through `scrollFraction` instead.
- **The base-colour resolution chain** — the native resolves the base colour from a static gradient-blur background colour, the app bar's own background, a restore snapshot and a page fallback; the port takes one `containerColor`. The static override is inert: it is never assigned in ColorOS 17, so it never leaves `null` and the page fallback always resolves; the base colour the port cannot reach is the dark theme's.
- **`setAppBarLayoutTransparent()`** — the host-side call that clears the app bar's background and elevation; the caller hollows out the bar it passes in `content`.
