# InstallLoadProgress

`InstallLoadProgress` is the app-install progress control used by the package installer: a button that morphs into a progress display and back. It is the button subclass the installer inflates, carrying the `InstallDownloadSmall` variant of the install-progress style.

It is aligned with the ColorOS 17 install-progress control, including a `Build.VERSION.SDK_INT >= 30` guard on the smooth-corner path.

<div style="position: relative; height: 250px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../compose/index.html?id=installLoadProgress" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.InstallLoadProgress
import io.github.suqi8.coui.kmp.basic.InstallLoadProgressColors
import io.github.suqi8.coui.kmp.basic.InstallLoadProgressDefaults
import io.github.suqi8.coui.kmp.basic.InstallLoadStyle
```

## Native Origin

[LoadProgressButton](../components/loadprogressbutton) is the other half of the same widget family, and it came first: it ports the state machine of the base class `COUILoadProgress` (`DEFAULT_UP_OR_DOWN` = 0, `UPING_OR_DOWNING` = 1, `UP_OR_DOWN_WAIT` = 2, `UP_OR_DOWN_FAIL` = 3, `INSTALL_HAVE_GIFT` = 4, and `toggle()`) together with a first pass at the drawing that `COUIInstallLoadProgress` adds.

`InstallLoadProgress` does not repeat any of that. It takes `LoadProgressState`, `LoadProgressButtonSize` and `LoadProgressButtonDefaults.glyph` from that component and adds the behaviour `COUILoadProgress` has no notion of — the load styles, the per-state fill, the two-pass label colours, the smooth corners and the pressed geometry.

`COUILoadingView` is the neighbouring class in the same package, but it is an unrelated read-only spinner (`ProgressIndicator` ports it) that shares no code with this control.

## Basic Usage

```kotlin
var state by remember { mutableStateOf(LoadProgressState.Default) }

InstallLoadProgress(
    onClick = { state = state.toggled() },
    state = state,
    text = "Download",
    style = InstallLoadStyle.Default
)
```

## Styles

`mLoadStyle` comes from the `couiStyle` attribute, whose enum is declared in the resource dump as `defaultSmallRoundButton` (0), `bigRoundButton` (1) and `smallCircleButton` (2). The layout in both APKs uses `InstallDownloadSmall`, which sets `couiStyle=0`.

| Style | `couiStyle` | Size | Shape | Source |
| :--- | :--- | :--- | :--- | :--- |
| `Default` | 0 | 52dp × 28dp | pill, half-height radius | `dimen/coui_install_download_progress_width` / `_height` |
| `BigRound` | 1 | 220dp × 44dp | pill, half-height radius | `dimen/coui_install_download_progress_width_large` / `_height_large` |
| `Circle` | 2 | 33dp × 33dp box | 30dp smooth-corner disk | `dimen/coui_install_download_progress_circle_width` / `_circle_height` / `_default_circle_radius` |

The `Circle` tier is the one to get wrong by eye. `updateRoundRectPath` offsets the path rect by `mCurrentCircleRadius`, which `setLoadStyle` computes as `getDefaultSize(dimen/coui_install_download_progress_default_circle_radius, 1.5f, true)` — the 16.5dp radius **minus** 1.5dp, so the disk is 30dp inside the 33dp touch box — and it passes `dimen/coui_install_download_progress_circle_round_border_radius` (7dp) as the corner radius, not half the disk. The silhouette is a smooth-corner square rather than a circle.

`isOs16()` (`RoundCornerUtil.getSmoothStyleType() == 1`) decides between two corner paths for the pills, and only for the pills: `updateRoundRectPath` returns before that branch for `mLoadStyle == 2`. The ColorOS 17 branch calls `OplusPathAdapter.addSmoothRoundRect(rect, rect.height() * getScaleY() / 2, …, CCW)`, i.e. a half-height radius through the platform smooth-corner path; the legacy branch calls `COUIShapePath.getRoundRectPath(rect, height / 2 - dimen/coui_install_download_progress_round_border_radius_offset)`. This port expresses both with `SquircleShape`, and keeps the legacy corner tile (`radius * 128.19 / 100`, the constant every curve offset in `COUIShapePath.getRoundRectPath` is scaled by) as `InstallLoadProgressDefaults.LEGACY_CORNER_EXTENSION` for the disk.

## State Matrix

`onDraw` branches on `mState` and then on `mLoadStyle`. The `z` argument of `onDrawCircle` / `onDrawRoundRect` selects `mColorPrimary` when true and `mColorSecondary` when false:

| State | `Circle` (2) | `BigRound` (1) | `Default` (0) |
| :--- | :--- | :--- | :--- |
| `Default` (0) | disk `z = false`, no fill | shape `z = true`, no fill | shape `z = false`, no fill |
| `Loading` (1) | disk `z = true` | shape `z = false` + fill `z = true` | same as `BigRound` |
| `Wait` (2) | disk `z = true` | shape `z = false` + fill `z = true` | same as `BigRound` |
| `Fail` (3) | disk `z = true`, reload glyph | shape `z = true`, **whole** shape | same as `BigRound` |
| `InstallHaveGift` (4) | nothing is drawn | label only | label only |

Three consequences that `LoadProgressButton` does not have:

- only the `bigRoundButton` pill is accent-filled at rest; the small pill and the disk rest on `mColorSecondary`;
- the fill exists only in the transfer states. `mSpace` is computed inside the `mState == 1 || mState == 2` branch, as `(int) (fraction * (mTouchModeWidth - 2 * mExpandOffsetX)) + mExpandOffsetX` — at rest simply `fraction * width`. `Fail` paints the whole path instead of a fraction, and `Default` paints no fill at all, so a `progress` value sitting on a resting button changes nothing;
- `INSTALL_HAVE_GIFT` matches no branch at all. `onDraw` falls through every drawing call to the label pass with nothing behind it, and the `Circle` tier draws nothing whatsoever, because that pass is itself guarded by `mLoadStyle != 2`. The gift badge itself is read from the `couiInstallGiftBg` attribute into `mInstallGiftBg` and then never drawn in this build.

## Label Colours

The label is drawn twice on the pill tiers. `onDrawText` paints it once in the resolved label colour, and, when `mIsChangeTextColor` is set, again in `mSurpassProgressColor` inside `canvas.clipRect(f, f2, mSpace, f4)` so the part sitting on the filled region flips colour:

| State | Outside the fill | Inside the fill |
| :--- | :--- | :--- |
| `Default` (0), `defaultSmallRoundButton` | `mColorPrimary` | — |
| `Default` (0), `bigRoundButton` | `mColorWhite` (`#ffffffff`) on the accent-filled pill | — |
| `Loading` / `Wait` | `mColorPrimary` | `mSurpassProgressColor` (`#ffffffff`) |
| `Fail` (3) | `mColorWhite` (`#ffffffff`), drawn once | — |

## Animation

| Animator | Duration | Effect |
| :--- | :--- | :--- |
| `mPressedAnimator` | 200ms | Brightness 1.0 → `brightness`; `mExpandOffsetX` / `mExpandOffsetY` ramp to 5% of the measured size; the label narrows to 0.92; on the `Circle` tier the radius shrinks to `mDefaultCircleRadius * 0.9` |
| `mNormalAnimator` | 340ms | All of the above returns to rest; on the `Circle` tier the glyph cross-fades (`CIRCLE_IN_ALPHA_HOLDER` 0 → 255, `CIRCLE_OUT_ALPHA_HOLDER` 255 → 0) |

Both use `COUIMoveEaseInterpolator`, a `PathInterpolator(0.3f, 0f, 0.1f, 1f)`.

The expand offsets are an *inset*: `updateRoundRectPath` builds `RectF(mExpandOffsetX, mExpandOffsetY, width - mExpandOffsetX, height - mExpandOffsetY)`, so the pressed shape shrinks by 5% on every edge rather than growing. The pressed animator floors each offset at `DEFAULT_MIN_PRESS_FEEDBACK` (0.005) of the corresponding measured dimension; the release animator does not, so the value is free to travel back through zero. `super.performClick()` fires from `mNormalAnimator`'s `onAnimationStart`, so the state flips on release — and only when the up event landed inside `mTouchModeWidth` × `mTouchModeHeight`.

`getCurrentColor` is not a plain RGB dim: it runs `ColorUtils.colorToHSL`, multiplies the lightness by `mCurrentBrightness` and converts back, keeping the original alpha. Only the shapes go through it; the label and the glyph take their colours raw.

## Recovered Tokens

| Token | Value | Source |
| :--- | :--- | :--- |
| `MAX_BRIGHTNESS` | 0.8 | `DEFAULT_BRIGHTNESS_MAX_VALUE` and the `brightness` attribute default |
| `MIN_PRESS_FEEDBACK` | 0.005 | `DEFAULT_MIN_PRESS_FEEDBACK` |
| `SCALE_PARAMETER` | 0.05 | `DEFAULT_SCALE_PARAMETER` |
| `NARROW_FINAL_VALUE` | 0.92 | `DEFAULT_NARROW_FINAL_VALUE` |
| `CIRCLE_RADIUS_SCALE` | 0.9 | `mDefaultCircleRadius * 0.9` |
| `PRESS_DURATION_MILLIS` / `MORPH_DURATION_MILLIS` | 200 / 340 | `PRESS_ANIMATOR_DURATION` / `NORMAL_ANIMATOR_DURATION` |
| `CROSSFADE_DURATION_MILLIS` | 340 | `mNormalAnimator` glyph-alpha track (`CIRCLE_MAX_ALPHA` 255) |
| `CircleCornerRadius` | 7dp | `dimen/coui_install_download_progress_circle_round_border_radius` |
| `OnePointFive` | 1.5dp | `ONE_POINT_FIVE`, the `getDefaultSize` delta |
| `RadiusOffset` | 0.5dp | `dimen/coui_install_download_progress_round_border_radius_offset` |
| `LEGACY_CORNER_EXTENSION` | 1.2819 | `COUIShapePath.getRoundRectPath` corner tile `128.19 / 100` |
| `MoveEase` | cubic-bezier(0.3, 0, 0.1, 1) | `COUIMoveEaseInterpolator` |
| Primary | `?couiColorPrimary` | `style/COUIOverlay.Theme.Blue.Default` → `color/couiBlueTintControlNormal` (`#ff006aff`) |
| Secondary | `?couiColorSecondary` | same theme → `color/couiBlueTintLightNormal` (`#26007aff`) |
| Label white | `#ffffffff` | `color/coui_install_load_progress_text_color_in_progress` |
| Disabled | `#14000000` | `disabledColor` = `?attr/couiBtnDrawableColorDisabled` → `color/coui_btn_drawable_color_disabled` |
| Text padding | 6dp | `couiInstallPadding` on `style/Widget.COUI.COUILoadProgress` |
| Text size | 14sp | `dimen/coui_install_download_progress_textsize` |
| Max / progress / label | 100 / 0 / `"Download"` | `COUILoadProgress.init()`, `string/coui_install_download_progress_textview` |

## Accessibility

`onInitializeAccessibilityEvent` publishes `mMax` as the item count and `mProgress` as the current item index, and `getAccessibilityClassName` reports `ProgressBar`. The content description is the label for states 0, 2 and 3, but for state 1 it is `mDownloadingContentDecrpition` — the `setDownloadingContentDecrpition(String)` value, exposed here as `downloadingContentDescription`.

`onTouchEvent` performs `performHapticFeedback(COUIHapticFeedbackConstants.GRANULAR_SHORT_VIBRATE)` on both `ACTION_DOWN` and the start of `performTouchEndAnim`, gated on `mIsNeedVibrate` (the `loadingButtonNeedVibrate` attribute, false in `style/Widget.COUI.COUILoadProgress`). It is exposed as `hapticFeedback`.

## Not Ported

- **`addApostrophe` and the label fitting** — the native measures with `TextPaint.breakText` against `mDefaultWidth - mTextPadding * 2 - apostropheWidth` (`mDefaultWidth` being `mTouchModeWidth - 1.5dp * 2`), cuts English text at its last space via `isEnglish`, and appends `string/coui_install_load_progress_apostrophe` (`"..."`). The port leaves this to `TextOverflow.Ellipsis`, which can break a multi-word label at a different character.
- **`mButtonDrawable` / `couiDefaultDrawable`** — `COUILoadProgress` can be given a vector that is drawn centred in place of the label, together with `mButtonDrawableReverseColor` tinted with `mSurpassProgressColor` inside the same fill `clipRect`. No style in either APK supplies `couiDefaultDrawable`, so the port always draws the label.
- **`mInstallGiftBg`** — read from `couiInstallGiftBg` in the constructor and never used; `INSTALL_HAVE_GIFT` renders nothing here to match.
- **RTL mirroring** — `onDraw` mirrors both the fill and the label `clipRect` when `ViewUtils.isLayoutRtl` is true. The fill is mirrored here too (`Alignment.CenterStart` follows the layout direction), but the label clip is not, because Compose's `drawWithContent` has no layout direction to read; in an RTL layout the label would flip colour across the wrong half. `LoadProgressButton` has the same gap.
- **`mThemeColorStateList`, `mThemeSecondaryColorStateList`, `mBtnTextColorStateList`** — the native lets a caller override each role with a `ColorStateList`; the port exposes plain `Color`s instead.
- **The `setTouchModeWidth` / `onConfigurationChanged` width compensation** — COUI adds or removes `dimen/coui_install_download_progress_width_in_foreign_language` (30dp) by hand when the locale crosses between Chinese and another language. The port lays the button out from its fixed dimensions and does not resize it.
- **`OplusPathAdapter` and the outline provider** — the native clips its own path through `setOutlineProvider` + `setClipToOutline(true)`. The port clips with `Modifier.clip(shape)`.
- **Dead fields** — `mTouchModeCircleRadius` (`dimen/coui_install_download_progress_default_circle_radius`) is assigned but never read, and `DEFAULT_SCALE_PERCENT` (0.4) is declared but never used. Neither is ported.
- **`setMaxBrightness(int)`** — the native setter takes an `int` while `mMaxBrightness` is a `float`, so the call cannot round-trip the value it stores. The port exposes brightness only through `InstallLoadProgressDefaults.MAX_BRIGHTNESS`.
- **The font-scale fit** — `setDefaultTextSize` runs the resolved size through `COUIChangeTextUtil.getSuitableFontSize(size, fontScale, 2)`. The port uses `sp`, which the Compose text stack already scales by the platform font scale.
- **The accessibility event throttling** — `AccessibilityEventSender` posts a progress event every 10ms and merges them; Compose's own semantics publish `progressBarRangeInfo` directly.
