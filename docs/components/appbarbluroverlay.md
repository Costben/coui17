# AppBarBlurOverlay

`AppBarBlurOverlay` is the ColorOS 17 app bar material overlay: the blurred, scroll-driven surface that sits over a top bar. It is the Compose port of `AppBarBlurOverlayView`, `AppBarBlurOverlayBehavior` and `AppBarBlurHelper`.

On the device the overlay is the view the behavior sizes, and the app bar itself is hollowed out — the material lands on the overlay while the bar is left with no background and no elevation. The overlay paints nothing of its own: the helper gives it a gradient alpha ramp plus a progressive blur, and the behavior drives the view's whole alpha from the scroll offset.

Two anchors matter, and they are not the same one. **The caller sizes the node**: the native view is inflated at `layout_height=0dp` and resized by the behavior to `appBar.top .. appBar.bottom + extraHeightPx`, so pass `Modifier.height(appBarHeight + extraHeight)`. The alpha ramp is anchored to the **bar's bottom edge** inside that node, while the progressive blur is full strength along the node's top edge.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlay
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlayDefaults
```

## Native Origin

`AppBarBlurOverlayView` is inflated at `layout_height=0dp` under the layout behavior `AppBarBlurOverlayBehavior`, which measures it to `appBar.top .. appBar.bottom + extraHeightPx` — the app bar's own bounds plus `homepage_appbar_blur_extra_height` (14dp), which is `AppBarBlurOverlayDefaults.ExtraHeight`. The view carries no `onDraw` and no `onSizeChanged`: it is a canvas that the helper dresses.

`AppBarBlurHelper` resolves the material's background into a `GradientBackgroundDrawable` and its blur into a `RenderEffect`, and `blurViewForAppBar()` prefers the sibling overlay over the app bar itself, which is why the host hollows the bar out. The material's gradient lives in the overlay's background drawable rather than in the `RenderEffect`, so the ramp is painted even where no blur can run.

`GradientBackgroundDrawable` builds its `LinearGradient` bottom-to-top, i.e. the ramp's positions are measured **from the bar's bottom edge upward**, with `p = 1` at the bar's top edge. The extra band below the bar is therefore transparent, and the material reads as the bar's own surface rather than as a taller gradient.

The framework's uniform path — `setUpNormalBlurBackground` with `normalBlurRadiusPx` and `normalBackgroundAlphaFraction` — is dead code: it has no caller in the ColorOS 17 artifacts this port was written against, so it is deliberately not implemented.

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
| ScrollAlphaThresholdDp | Float | The scroll distance over which the overlay's alpha ramps from 0 to 1   | 30f (`AppBarBlurHelper.ALPHA_SCROLL_THRESHOLD_DP`) |
| DarkContainerColor    | Color | The dark-theme material base colour, taken from the nearest ColorOS night token rather than from the framework attribute | `#333333` (`coui_color_background_top` / `coui_color_surface_top`) |
| containerColor()      | Color | The base colour for the current theme: the light theme's `surface`, the dark theme's `DarkContainerColor` | `#f0f1f2` / `#333333` |
| scrollAlpha(scrollPx, density) | Float | `min(1, scrollPx / ScrollAlphaThresholdDp)`, coerced into `0f..1f` | — |

Feed the `scrollAlpha` result to the overlay's `scrollFraction`: at rest the overlay is invisible, and it reaches full strength once the content has scrolled `ScrollAlphaThresholdDp` (30dp) beneath the bar.

## Recovered Tokens

Every value below is taken from the decompiled `AppBarBlurParams` defaults, from `AppBarBlurHelper`, or from the ColorOS 17 resource table; none of them is invented.

| Token | Value | Source |
| :--- | :--- | :--- |
| `ExtraHeight` | 14dp | `homepage_appbar_blur_extra_height` |
| `ScrollAlphaThresholdDp` | 30 | `AppBarBlurHelper.ALPHA_SCROLL_THRESHOLD_DP` |
| `startBlurRadius` | 30px | `AppBarBlurParams.startBlurRadius` |
| `endBlurRadius` | 0px | `AppBarBlurParams.endBlurRadius` |
| Blend curve | `pow(1 - t, 3.0)` | `AppBarBlurParams.gradientBlurSpeed` = 3.0f |
| Light stop positions | `[0.0, 0.4, 1.0]` | `AppBarBlurParams.gradientPos0/1/2` |
| Light stop alphas | `[0.0, 0.7, 1.0]` | `AppBarBlurParams.gradientAlpha0/1/2` |
| Dark stop positions | `[0.0, 1.0]` | `gradientPos0` / `gradientPos2` |
| Dark stop alphas | `[0.0, 0.4]` | `gradientAlpha0` / `AppBarBlurHelper.GRADIENT_DARK_ALPHA` |

The dark theme has only two stops: the `0.4` top-edge alpha is the hard-coded `GRADIENT_DARK_ALPHA`, not a third `gradientAlpha` value. The blur radius and the blend exponent are carried by `AppBarGlassDefaults` in `TopAppBar.kt` (`GradientBlurRadiusPx`, `GradientEndBlurRadiusPx`, `GradientBlurSpeed`), which is also where the app bar's own `gradientGlass` material reads them from; `endBlurRadius` is 0 because the gradient shape blurs at `max(start, end)` and lets the blend curve do the ramping.

Which of the two ramps is drawn is a property of the theme, not of the base colour: the port selects the dark ramp when `COUITheme.colorScheme.background.luminance() < 0.5f` — the framework's `isCOUIDarkThemeOrNightMode` — so passing a dark `containerColor` on a light theme cannot move the ramp to the other branch.

The dark base colour itself is not recoverable from the same chain. The framework resolves it from `?couiColorBackgroundWithCard`, which is `#ffffff` in the night configuration — a colour that cannot produce the profile measured on the device, where the material's top edge lands at 60 over a `#454545` card and, with the dark ramp's hard-coded top alpha, `0.4 * B + 0.6 * 69 = 60` gives `B ≈ 46.5`. The nearest ColorOS night token is `coui_color_background_top` / `coui_color_surface_top` = `#333333`, which is what `DarkContainerColor` takes, confirmed on the device at the visual layer. No explicit colour can be hiding the gap: `AppBarBlurHelper.setGradientBlurBgColor` has no call site anywhere in the Settings APK's fourteen dex files, so its static never leaves `null` and the page fallback is what resolves; the dark path carries a further darkening layer this port cannot read — the framework's `com.oplus.view.*`, of which the repository only holds the call site `COUIMaterialEffect.java:94-113`.

## Not Ported

- **The uniform (`setUpNormalBlurBackground`) path** — `normalBlurRadiusPx` (50px), `normalBackgroundAlphaFraction` (0.8) and `NORMAL_BLUR_DARK_ALPHA` (0.6) have no caller in the studied artifacts, so this port only implements the gradient shape.
- **The `scrollDrivenAlphaEnabled` attribute** — the device view reads its alpha from the scroll offset when the attribute is set; the port takes the resolved alpha through `scrollFraction` instead.
- **The base-colour resolution chain** — `resolveGradientBlurBaseColorForAppBar` walks a static `gradientBlurBgColor`, the app bar's own background, a restore snapshot and a page fallback; the port takes one `containerColor`. The static override is inert on the device: `AppBarBlurHelper.setGradientBlurBgColor` has no call site in the Settings APK's fourteen dex files, so `gradientBlurBgColor` never leaves `null` and the page fallback always resolves; the base colour the port cannot reach is the dark theme's.
- **`setAppBarLayoutTransparent()`** — the host-side call that clears the app bar's background and elevation; the caller hollows out the bar it passes in `content`.
