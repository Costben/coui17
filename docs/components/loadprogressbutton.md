# LoadProgressButton

`LoadProgressButton` is the ColorOS install/download button. It is a *button*, not a spinner: tapping it walks a five-value state machine that swaps the glyph it shows — a download arrow, a pause pair, a retry arrow — and, on its pill tiers, fills a progress bar behind the label.

It ports `com.coui.appcompat.progressbar.COUILoadProgress` (an `AppCompatButton`) together with the drawing half of its subclass `COUIInstallLoadProgress`, under the style `Widget.COUI.COUILoadProgress` and its `InstallDownload` / `InstallDownloadSmall` variants.

<div style="position: relative; height: 250px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../compose/index.html?id=loadProgressButton" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.LoadProgressButton
import io.github.suqi8.coui.kmp.basic.LoadProgressButtonDefaults
import io.github.suqi8.coui.kmp.basic.LoadProgressButtonSize
import io.github.suqi8.coui.kmp.basic.LoadProgressState
```

## Native Origin

[ProgressIndicator](../components/progressindicator) ports `COUILoadingView`, `COUICircularProgressDrawable` and `COUILottieLoadingView` — all read-only spinners. `COUILoadProgress` is a different widget: it extends `AppCompatButton`, it is clickable, and it owns a state machine. The two do not overlap, so this is a separate component.

`performClick()` calls `toggle()`, whose transitions are `0 -> 1`, `1 -> 2`, `2 -> 1`, `3 -> 1`, with `INSTALL_HAVE_GIFT` (4) left unchanged by the native code. `LoadProgressState.toggled()` reproduces exactly that:

| State | Value | Meaning | Circle glyph |
| :--- | :--- | :--- | :--- |
| `Default` | 0 | Idle, "Download" | `circle_load` |
| `Loading` | 1 | A transfer is running | `circle_pause` |
| `Wait` | 2 | A transfer is queued | `circle_reload` |
| `Fail` | 3 | A transfer failed | `circle_reload` |
| `InstallHaveGift` | 4 | The install bundles a gift | `circle_load` (native has no circle glyph for state 4) |

## Basic Usage

```kotlin
var state by remember { mutableStateOf(LoadProgressState.Default) }

LoadProgressButton(
    onClick = { state = state.toggled() },
    state = state,
    size = LoadProgressButtonSize.Circle
)
```

## Progress

```kotlin
var state by remember { mutableStateOf(LoadProgressState.Default) }
var progress by remember { mutableIntStateOf(0) }

LoadProgressButton(
    onClick = { state = state.toggled() },
    state = state,
    progress = progress,
    max = 100,
    text = if (state == LoadProgressState.Loading) "$progress%" else "Download",
    size = LoadProgressButtonSize.Large
)
```

`progress` is clamped to `0..max` exactly as `setProgress` does, and the fill animates on a `COUISpringForce` with `setBounce(0)` and `setResponse(1.0)`. `refreshProgressWithAnim` skips the animation when the new value is within `ONE_MILLION` (1.0E-7) of the current one, which this port keeps as a guard against painting a hairline of accent on an empty track.

## Sizes

`mLoadStyle` picks the shape. `mLoadStyle == 2` (`InstallDownloadSmall`) is the circle: a filled disk the size of the recovered radius, carrying a glyph and no label. Every other style is a rounded rectangle carrying the label.

| Tier | Width | Height | Corner radius | Source resource |
| :--- | :--- | :--- | :--- | :--- |
| `Circle` | 33dp | 33dp | 16.5dp | `dimen/coui_install_download_progress_circle_width` / `_circle_height` / `_default_circle_radius` |
| `Small` | 52dp | 28dp | 13.5dp | `dimen/coui_install_download_progress_width` / `_height` / `_round_border_radius_small` |
| `Large` | 220dp | 44dp | 21.5dp | `dimen/coui_install_download_progress_width_large` / `_height_large` / `_round_border_radius` |

## Animation

| Animator | Duration | Effect |
| :--- | :--- | :--- |
| `mPressedAnimator` | 200ms | Brightness 1.0 → the style's `brightness` (0.8); the pill expands by `DEFAULT_SCALE_PARAMETER` (0.05) of its size; the label narrows to `DEFAULT_NARROW_FINAL_VALUE` (0.92); the circle radius shrinks to `mDefaultCircleRadius * 0.9` |
| `mNormalAnimator` | 340ms | All of the above returns to rest, and on the circle tier the glyph cross-fades (`CIRCLE_IN_ALPHA_HOLDER` 0 → 255, `CIRCLE_OUT_ALPHA_HOLDER` 255 → 0) |

`super.performClick()` fires at the *start* of `mNormalAnimator`, so the state flips on release and the glyph then cross-fades into place.

## Recovered Tokens

| Token | Value | Source resource |
| :--- | :--- | :--- |
| `Max` | 100 | `COUILoadProgress.init()` |
| `Progress` | 0 | `COUILoadProgress.init()` |
| `DefaultText` | `"Download"` | `string/coui_install_download_progress_textview` |
| `Apostrophe` | `"..."` | `string/coui_install_load_progress_apostrophe` |
| `TextSize` | 14sp | `dimen/coui_install_download_progress_textsize` / `_textsize_large` |
| `TextPadding` | 6dp | `style/Widget.COUI.COUILoadProgress` `couiInstallPadding` |
| `MorphDurationMillis` | 340 | `COUIInstallLoadProgress.NORMAL_ANIMATOR_DURATION` |
| `PressDurationMillis` | 200 | `COUIInstallLoadProgress.PRESS_ANIMATOR_DURATION` |
| `CrossfadeDurationMillis` | 340 | `mNormalAnimator` glyph-alpha track |
| `PressBrightness` | 0.8 | `style/Widget.COUI.COUILoadProgress.InstallDownload{Small}` `brightness` |
| `PressScaleParameter` | 0.05 | `DEFAULT_SCALE_PARAMETER` |
| `PressNarrowFinalValue` | 0.92 | `DEFAULT_NARROW_FINAL_VALUE` |
| `PressCircleRadiusScale` | 0.9 | `mDefaultCircleRadius * 0.9` |
| `GlyphWidth` / `GlyphHeight` | 12dp / 14dp | `drawable/coui_install_load_progress_circle_{load,pause,reload}` `android:width` / `android:height` |
| `ForeignLanguageWidthDelta` | 30dp | `dimen/coui_install_download_progress_width_in_foreign_language` |
| Round border radius offset | 0.5dp | `dimen/coui_install_download_progress_round_border_radius_offset` |
| Container / progress fill | `?couiColorPrimary`, pressed `?couiColorPrimaryPressed` | `color/coui_tint_selector`, set as `couiInstallDefaultColor` and `couiLoadProgressColor` |
| Content (label and glyph) | `#ffffffff` | `color/coui_install_load_progress_text_color_in_progress`, `color/coui_install_load_progress_circle_color_pause`, `color/coui_install_load_progress_circle_color_reload` |
| Load glyph fill | `?couiColorPrimary` | `drawable/coui_install_load_progress_circle_load` `fillColor` |
| Progress spring | bounce 0, response 1.0 | `COUISpringForce` in `refreshProgressWithAnim` |

The load and reload glyphs share a path, a 36x42 viewport, and differ only in fill colour:

```
M 20 34.2 L 31.6 22.5 C 32.4 21.7 33.6 21.7 34.4 22.5 C 35.2 23.3 35.2 24.5 34.4 25.3
L 20.8 39.1 C 19.2 40.7 16.7 40.7 15.1 39.1 L 1.6 25.5 C 0.8 24.7 0.8 23.5 1.6 22.7
C 2.4 21.9 3.6 21.9 4.4 22.7 L 16 34.2 L 16 4 C 16 2.9 16.9 2 18 2 C 19.1 2 20 2.9 20 4
L 20 34.2 Z
```

The pause glyph is two rounded bars:

```
M 12 8 L 12 34 C 12 35.1 11.1 36 10 36 C 8.9 36 8 35.1 8 34 L 8 8 C 8 6.9 8.9 6 10 6
C 11.1 6 12 6.9 12 8 Z M 28 8 L 28 34 C 28 35.1 27.1 36 26 36 C 24.9 36 24 35.1 24 34
L 24 8 C 24 6.9 24.9 6 26 6 C 27.1 6 28 6.9 28 8 Z
```

## Not Ported

- **The `couiInstallGiftBg` badge for `INSTALL_HAVE_GIFT` (4)** — the gift background is set from an attribute, not from a recovered drawable, so the state falls back to the download arrow here.
- **`mSurpassProgressColor`** — the native sets it from a resource id that could not be resolved in the dump; the port maps the label-over-fill colour to `COUITheme.colorScheme.onPrimary` instead of a recovered literal.
- **The unfilled track colour** — `onDrawRoundRect(..., z = false)` paints it with the style's `couiColorSecondary`, whose attribute value is not in the resource dump; the port maps it to `COUITheme.colorScheme.dividerLine`, the same track role `ProgressIndicatorDefaults` uses.
- **`ForeignLanguageWidthDelta`** — COUI adds or removes this width when the locale switches between Chinese and another language, adjusting the touch target by hand. It is exposed as a token but not applied.
- **`mThemeColorStateList` / `mThemeSecondaryColorStateList`** — the native allows a caller to override the resolved colours with a `ColorStateList`; this port exposes plain `Color`s through `LoadProgressButtonColors` instead.
- **The accessibility event throttling** — `AccessibilityEventSender` posts a progress event every 10ms and merges them; Compose's own semantics publish `progressBarRangeInfo` directly.
