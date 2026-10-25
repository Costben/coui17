# AppBarBlurOverlay

`AppBarBlurOverlay` is the ColorOS 17 app bar material overlay: the blurred, scroll-driven surface that sits over a top bar. It is a Compose port of that surface.

In ColorOS the overlay is the view the layout behavior sizes, and the app bar itself is hollowed out — the material lands on the overlay while the bar is left with no background and no elevation. The overlay paints nothing of its own: a helper gives it a gradient alpha ramp plus a progressive blur, and the behavior drives the view's whole alpha from the scroll offset.

The composable measures its `content` to find the bar, reports the **bar's** height, and draws the material over `bar + extraHeight`, past the reported bottom edge. The caller does not add `extraHeight` to the node: pass `Modifier.height(barHeight)`, or no height at all. The alpha ramp is anchored to the **material node's bottom edge** (bar + `extraHeight`), not to the bar's, so the bar's own bottom edge still carries some surface colour and blur, and both fade out over the extra band. The progressive blur is strongest along the node's top edge.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlay
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlayDefaults
import io.github.suqi8.coui.kmp.basic.couiGlassHeader
```

## Native Origin

The overlay view is inflated at `layout_height=0dp` under a layout behavior that measures it to `appBar.top .. appBar.bottom + extraHeightPx` — the app bar's own bounds plus the scroll-driven overlay's extra height, 24dp, which is `AppBarBlurOverlayDefaults.ExtraHeight`. (The settings homepage inflates its non-scroll-driven overlay with `homepage_appbar_blur_extra_height` = 14dp instead; the scroll-driven form uses the taller band, which is the one that leaves a visible fade under a collapsed bar.) The view carries no `onDraw` and no `onSizeChanged`: it is a canvas that a helper dresses.

The material's background resolves into a gradient drawable and its blur into a `RenderEffect`; the sibling overlay is preferred over the app bar itself, which is why the host hollows the bar out. The material's gradient lives in the overlay's background drawable rather than in the `RenderEffect`, so the ramp is painted even where no blur can run.

The gradient is built bottom-to-top, i.e. the ramp's positions are measured **from the node's bottom edge upward** (the gradient drawable builds `LinearGradient(0, height, 0, 0, …)`). Because the node is `bar + extraHeight` tall, the ramp is alpha 0 on the node's bottom edge, 0.7 at 40% up, and 1 at the top in the light theme (0 up to 0.4 in the dark theme). The bar's own bottom edge therefore still carries some surface colour, and the colour fades to nothing over the extra band.

The framework's uniform path — a normal blur background with `normalBlurRadiusPx` and `normalBackgroundAlphaFraction` — is dead code: it has no caller in ColorOS 17, so it is deliberately not implemented.

## Basic Usage

```kotlin
val density = LocalDensity.current
var scrollPx by remember { mutableStateOf(0f) }
val barHeight = TopAppBarDefaults.CollapsedHeight

Box(Modifier.fillMaxSize()) {
    ScrollingContent(onScroll = { scrollPx = it })

    AppBarBlurOverlay(
        // The node reports the bar height; the material reaches ExtraHeight past it.
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight),
        scrollFraction = AppBarBlurOverlayDefaults.scrollAlpha(scrollPx, density),
    ) {
        // The bar's own content, hollowed out: the material fades in beneath it.
        SmallTopAppBar(title = "Title")
    }
}
```

The overlay needs the content it samples through `LocalBackdrop`; place it where a backdrop is provided, or pass `backdrop = null` for the plain ramp alone. When the platform cannot run runtime shaders (everything below Android 13) the blur is skipped as well, and only the ramp is painted.

On ColorOS devices whose OplusView sub-SDK is 33.34 or later, the overlay does not use `backdrop` at all. It hosts the same platform view the native helper dresses: the gradient drawable as its background, and the framework's gradient-blur backdrop effect (30px to 0px, speed 3), which the system renderer applies to whatever is already drawn behind the view. The effect is set once; scrolling only changes the view's alpha, and the view is hidden at exactly 0. Every other platform keeps COUI's own blur.

The material is an image of the content **under** it, so COUI only paints it while `LocalEdgeToEdge` is `true`: put it inside a `Scaffold(edgeToEdge = true)`, or declare it yourself with `CompositionLocalProvider(LocalEdgeToEdge provides true)`. With the content laid out below the bar there is nothing to sample, the sample clamps to the nearest row and smears it into a flat band; the overlay then behaves as a plain container and paints no material.

### Scroll Producer Overload

`AppBarBlurOverlay` also has an overload whose first parameter is `scrollFraction: () -> Float` instead of a `Float`. The producer is read in the draw phase, so a scroll-driven alpha does not recompose the bar on every frame. At rest it must return `0`, which leaves the material drawing nothing and the content under the bar sharp; a caller with no scroll state to observe should return `1f`. The `Float` overload forwards to this one.

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

AppBarBlurOverlay(
    scrollFraction = scrollFraction,
    modifier = Modifier
        .fillMaxWidth()
        .height(TopAppBarDefaults.CollapsedHeight),
) {
    SmallTopAppBar(title = "Title")
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
| modifier       | Modifier                   | Applied to the overlay; the node reports the bar's height, so pass `Modifier.height(barHeight)` or no height | Modifier                             | No       |
| containerColor | Color                      | The base colour the alpha ramp is drawn in; light `#f0f1f2`, dark `#303030` (see Recovered Tokens) | AppBarBlurOverlayDefaults.containerColor() | No       |
| backdrop       | Backdrop?                  | The content to sample; `null` skips the blur and paints the ramp only                       | LocalBackdrop.current                | No       |
| extraHeight    | Dp                         | How far the overlay reaches past the app bar's bottom edge                                  | AppBarBlurOverlayDefaults.ExtraHeight | No       |
| scrollFraction | Float                      | The scroll-driven alpha, `min(1, scrollPx / 30dp)`; at `0` the overlay draws nothing         | 1f                                   | No       |
| content        | @Composable BoxScope.() -> Unit | The app bar content the caller hollows out; it is not part of the material            | {}                                   | No       |

`content` is drawn on top of the material and is not affected by `scrollFraction`: the title and the actions stay readable at rest, and only the material fades in with the scroll.

The producer overload takes the same parameters, with `scrollFraction: () -> Float` as its first, required parameter in place of the `Float` (see Scroll Producer Overload).

`containerColor` is only the colour the ramp is drawn in; which ramp the theme picks — the three-stop light one or the two-stop dark one — is decided by the theme, not by this colour (see Recovered Tokens).

### AppBarBlurOverlayDefaults Object

| Constant / Function   | Type  | Description                                                            | Value / Source |
| --------------------- | ----- | ---------------------------------------------------------------------- | -------------- |
| ExtraHeight           | Dp    | How far the overlay reaches past the bar's bottom edge                 | 24.dp (the scroll-driven overlay's extra height) |
| ScrollAlphaThresholdDp | Float | The scroll distance over which the overlay's alpha ramps from 0 to 1   | 30f |
| DarkContainerColor    | Color | The dark-theme material base colour: the platform's own dark background, and the colour the dark page renders as | `#303030` (`background_material_dark` → `material_grey_850`) |
| containerColor()      | Color | The base colour for the current theme: the light theme's `surface`, the dark theme's `DarkContainerColor` | `#f0f1f2` / `#303030` |
| scrollAlpha(scrollPx, density) | Float | `min(1, scrollPx / ScrollAlphaThresholdDp)`, coerced into `0f..1f` | — |
| scrollAlpha(scrollPx, density, minAlpha) | Float | The same ramp remapped into `minAlpha..1`, with `minAlpha` coerced into `0f..1f` | — |

Feed the `scrollAlpha` result to the overlay's `scrollFraction`: at rest the overlay is invisible, and it reaches full strength once the content has scrolled `ScrollAlphaThresholdDp` (30dp) beneath the bar.

## Recovered Tokens

Every value below is taken from the ColorOS 17 material's defaults or its resource table; none of them is invented.

| Token | Value | Source |
| :--- | :--- | :--- |
| `ExtraHeight` | 24dp | The scroll-driven overlay's extra height |
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

The dark base colour itself cannot be read off the same chain. ColorOS resolves it from `?couiColorBackgroundWithCard`, which is `#ffffff` in the night configuration — a colour that cannot produce the profile the material shows. The dark page background renders as a flat 48 / 48 / 48 (`#303030`), measured across whole pages on two native surfaces, and `#303030` is also the platform's own dark background (`background_material_dark` → `material_grey_850`), so it is a colour the platform declares rather than one inferred from a screenshot mean. The material's top edge over a `#454545` card lands at 60, and the dark ramp's hard-coded top alpha gives `0.4 * B + 0.6 * 69 = 60`, so `B ≈ 46.5`, consistent with 48. The colour token that would produce this cannot be resolved: an enabled runtime overlay sits on top of the night configuration, and the darkening layer is applied by the vendor framework itself. `DarkContainerColor` therefore takes the rendered value, `#303030`, instead. No explicit colour is hiding the gap: the gradient-blur background colour is never assigned in ColorOS 17, so its static never leaves `null` and the page fallback is what resolves.

## Not Ported

- **The uniform (normal blur background) path** — `normalBlurRadiusPx` (50px), `normalBackgroundAlphaFraction` (0.8) and the dark alpha (0.6) have no caller in ColorOS 17, so this port only implements the gradient shape.
- **The `scrollDrivenAlphaEnabled` attribute** — the native view reads its alpha from the scroll offset when the attribute is set; the port takes the resolved alpha through `scrollFraction` instead.
- **The base-colour resolution chain** — the native resolves the base colour from a static gradient-blur background colour, the app bar's own background, a restore snapshot and a page fallback; the port takes one `containerColor`. The static override is inert: it is never assigned in ColorOS 17, so it never leaves `null` and the page fallback always resolves. The port does not replay the chain for the dark theme; it uses `DarkContainerColor`, the rendered `#303030`, instead (see Recovered Tokens).
- **`setAppBarLayoutTransparent()`** — the host-side call that clears the app bar's background and elevation; the caller hollows out the bar it passes in `content`.
