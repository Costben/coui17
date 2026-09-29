---
title: GuideDialog
requiresScaffoldHost: false
prerequisites:
  - Can be used anywhere, does not require `Scaffold` or `COUIPopupHost`
  - Renders at window level
hostComponent: None
popupHost: None
---

# GuideDialog

`WindowGuideDialog` is the ColorOS 17 first-run multi-page guide dialog, mirroring `COUIGuideDialogBuilder` and its content view `COUIGuideLayoutContentView`. It is a bottom-anchored panel of pages with an optional image, a title and a description, a page indicator, and a Skip / Next button bar.

The panel reuses the shared dialog layout, so the window presentation, the dim, the insets, the squircle clip and the bottom slide-in / slide-out of `Animation.COUI.Dialog` are the same as `WindowDialog`; only the content column is specific to the guide.

::: tip
This component is independent of `Scaffold` and can be used in any composable scope.
:::

## Import

```kotlin
import io.github.suqi8.coui.kmp.window.GuidePage
import io.github.suqi8.coui.kmp.window.GuideDialogDefaults
import io.github.suqi8.coui.kmp.window.WindowGuideDialog
```

## Basic Usage

```kotlin
var showGuide by remember { mutableStateOf(false) }

ArrowPreference(
    title = "Show the guide",
    onClick = { showGuide = true },
)

WindowGuideDialog(
    show = showGuide,
    pages = listOf(
        GuidePage(
            title = "Welcome to COUI",
            description = "A ColorOS component library for Compose Multiplatform.",
            image = painterResource(Res.drawable.onboarding),
        ),
        GuidePage(
            title = "Every component is themed",
            description = "Colours, text styles and shapes come from a single theme.",
            image = painterResource(Res.drawable.theming),
        ),
        GuidePage(
            title = "Ready to build",
            description = "Drop a component into your app.",
            image = painterResource(Res.drawable.build),
        ),
    ),
    onDismissRequest = { showGuide = false },
)
```

The primary button advances the pager while the guide has pages left. On the last page it turns into the "Got it" button, whose click dismisses the guide. Skip dismisses immediately from any page.

## Single Page

With a single page the indicator is hidden, the Skip button is removed and the primary button spans the whole button row, exactly like `COUIGuideLayoutContentView.setDefaultButtonText` switching the bar to `SINGLE_BUTTON`:

```kotlin
WindowGuideDialog(
    show = showGuide,
    pages = listOf(
        GuidePage(
            title = "A single-page guide",
            description = "With one page, Skip and the indicator are hidden.",
        ),
    ),
    onDismissRequest = { showGuide = false },
)
```

## Properties

### WindowGuideDialog

| Property Name     | Type                | Description                                                                                     | Default Value                | Required |
| ----------------- | ------------------- | ----------------------------------------------------------------------------------------------- | ---------------------------- | -------- |
| show              | Boolean             | Whether to show the guide                                                                       | -                            | Yes      |
| pages             | List&lt;GuidePage&gt; | The pages of the guide, in order                                                              | -                            | Yes      |
| onDismissRequest  | () -> Unit          | Called when the user dismisses the guide (Skip, the last-page primary button, back or outside tap) | -                          | Yes      |
| modifier          | Modifier            | Root content modifier                                                                           | Modifier                     | No       |
| onDismissFinished | () -> Unit          | Invoked after the hide animation completes; not invoked if the hide is cancelled mid-flight      | {}                           | No       |
| skipText          | String              | Label of the secondary button, `coui_guide_dialog_skip_text`                                     | GuideDialogDefaults.SkipText | No       |
| nextText          | String              | Label of the primary button before the last page, `coui_guide_dialog_next_text`                  | GuideDialogDefaults.NextText | No       |
| knownText         | String              | Label of the primary button once the last page is reached, `coui_guide_dialog_known_text`        | GuideDialogDefaults.KnownText | No      |
| imageHeight       | Dp                  | Height of a page image, `ImageHeightStyle.DEFAULT` (328dp) or `HEIGHT_240DP` (240dp)             | GuideDialogDefaults.ImageHeight | No    |
| onPageChanged     | (Int) -> Unit       | Called with the index of the page the pager settles on, including the first                      | {}                           | No       |

### GuidePage

| Property    | Type    | Description                                                                       | Default Value | Required |
| ----------- | ------- | --------------------------------------------------------------------------------- | ------------- | -------- |
| title       | String  | Page title, 18sp medium in `couiColorLabelPrimary` (`guide_tips_title`)           | -             | Yes      |
| description | String? | Optional page description, 13sp in `couiColorLabelSecondary` (`guide_tips_description`) | null     | No       |
| image       | Painter? | Optional page image, drawn full width and `imageHeight` tall with `CENTER_CROP` (`pager_item_im`) | null | No |

### GuideDialogDefaults

| Constant                     | Type                 | Value  | COUI Source                                                |
| ---------------------------- | -------------------- | ------ | ---------------------------------------------------------- |
| CornerRadius                 | Dp                   | 32.dp  | coui_dialog_guide_clip_radius_root                         |
| MaxHeight                    | Dp                   | 632.dp | coui_dialog_guide_max_height                               |
| ViewPagerMarginBottom        | Dp                   | 16.dp  | coui_dialog_guide_viewpager2_margin_bottom                 |
| ViewPagerMarginBottomSingle  | Dp                   | 20.dp  | coui_dialog_guide_viewpager2_margin_single_bottom          |
| IndicatorMarginBottom        | Dp                   | 20.dp  | coui_dialog_guide_indicator_margin_bottom                  |
| ButtonBarMarginBottom        | Dp                   | 8.dp   | coui_dialog_guide_button_layout_margin_bottom              |
| ButtonBarPaddingHorizontal   | Dp                   | 16.dp  | coui_dialog_guide_button_layout_padding_horizontal         |
| ButtonSpacing                | Dp                   | 8.dp   | coui_dialog_guide_button_layout_space                      |
| ButtonMinWidth               | Dp                   | 132.dp | coui_btn_large_width_min                                   |
| ButtonMinHeight              | Dp                   | 44.dp  | coui_btn_large_height_min                                  |
| ImageHeight                  | Dp                   | 328.dp | ImageHeightStyle.DEFAULT                                   |
| ImageHeightAlt               | Dp                   | 240.dp | ImageHeightStyle.HEIGHT_240DP                              |
| TitlePaddingHorizontal       | Dp                   | 24.dp  | coui_guide_page_item.xml guide_tips_title paddingHorizontal |
| TitleMarginTop               | Dp                   | 24.dp  | coui_guide_page_item.xml guide_tips_title marginTop         |
| TitleMarginBottom            | Dp                   | 6.dp   | coui_guide_page_item.xml guide_tips_title marginBottom      |
| BodyPaddingHorizontal        | Dp                   | 24.dp  | coui_guide_page_item.xml guide_tips_description paddingHorizontal |
| SkipText                     | String               | "Skip" | coui_guide_dialog_skip_text                                 |
| NextText                     | String               | "Next" | coui_guide_dialog_next_text                                 |
| KnownText                    | String               | "Got it" | coui_guide_dialog_known_text                             |
| TextFadeStartProgress        | Float                | 0.5f   | COUIGuideDialogButtonLayout.TEXT_FADE_START_PROGRESS        |
| TextFadeEndProgress          | Float                | 0.85f  | COUIGuideDialogButtonLayout.TEXT_FADE_END_PROGRESS          |
| TextFadeRange                | Float                | 0.35f  | COUIGuideDialogButtonLayout.TEXT_FADE_RANGE                 |

## Layout

The content column follows `COUIGuideLayoutContentView.initLayout()` exactly, top to bottom:

1. the pager, weight 1, bottom margin `ViewPagerMarginBottom` (16dp) — or `ViewPagerMarginBottomSingle` (20dp) for a single page. Each page is a vertically scrollable column: image, title, description;
2. the `PageIndicator`, centred, bottom margin `IndicatorMarginBottom` (20dp). Hidden for a single page, which the native also forces `GONE`;
3. the button bar, full width, bottom margin `ButtonBarMarginBottom` (8dp) and `ButtonBarPaddingHorizontal` (16dp) horizontal padding.

`MaxHeight` (632dp, `coui_dialog_guide_max_height`) caps the whole column.

## Colours and Text

| Item                   | COUI token                                                  | Light / Night         | Port                |
| ---------------------- | ----------------------------------------------------------- | --------------------- | ------------------- |
| Page title             | attr/couiColorLabelPrimary → coui_color_label_primary        | #e6000000 / #e6ffffff | onSurface           |
| Page description       | attr/couiColorLabelSecondary → coui_color_label_secondary    | #8a000000 / #8affffff | onSurfaceSecondary  |
| Indicator active dot   | coui_color_label_primary                                     | #e6000000 / #e6ffffff | onSurface           |
| Indicator inactive dot | coui_color_hint_neutral                                      | #42000000 / #4dffffff | disabledOnSurface   |

| Item        | Value                                                        | Source                                          |
| ----------- | ------------------------------------------------------------ | ----------------------------------------------- |
| Title       | couiTextHeadlineS, 18sp, sans-serif-medium (weight 550)      | coui_guide_page_item.xml attr/couiTextHeadlineS |
| Description | couiTextBodyS, 13sp                                          | coui_guide_page_item.xml attr/couiTextBodyS     |

## Behavior

- The button bar's animation follows `COUIGuideDialogButtonLayout`. The pager position maps to `clamp((currentPage + offsetFraction) - (count - 2), 0f, 1f)` and is eased on `COUIEaseInterpolator` (cubic-bezier 0.33, 0, 0.67, 1). Skip fades out by progress 0.6 (`SECONDARY_BUTTON_ALPHA_END_PROGRESS`) and is hidden once fully transparent; the primary button grows from half to full row width from progress 0.2 (`WIDTH_CHANGE_START_PROGRESS`) on the response 0.15 / bounce 0 spring (`SPRING_RESPONSE`, `SPRING_MINIMUM_VISIBLE_CHANGE` 0.001); and its label cross-fades from `Next` to `Got it` over progress 0.5 to 0.85 (`TEXT_FADE_*`).
- Dismissal: Skip, the primary button on the last page, the back gesture and a tap outside all call `onDismissRequest`. On an intermediate page the primary button scrolls to the next page instead.
- `onPageChanged` fires for the page the pager settles on, the first page included.

## Not Ported / Deviations

- The native `COUIGuideDialogBuilder.show()` calls `setCanceledOnTouchOutside(false)`. This port keeps outside-tap dismissal enabled, because the shared dialog layout owns the outside-tap handler for every dialog in this library and special-casing one dialog would change the shared behaviour.
- `COUIGuideBehavior` is not ported: the report shows it is a renamed Material `BottomSheetBehavior` with OPlus pull-up extensions, it references no guide-dialog resource, and `COUIGuideDialogBuilder` does not use it.
- The ViewPager2 fling tuning of `createGuideDialogConfig` is not ported: the 650ms program scroll, the 500–1000ms fling, the velocity threshold 2500, the max 6000, the decay factor 2.0 and `COUISpringInterpolator(0.6, 0.0)` are all ViewPager2-specific. Compose's `HorizontalPager` carries its own fling model, so the primary button uses the default `animateScrollToPage` spec.
- The dual-`TextPaint` drawing of `COUITwoTextButton` is not ported; the label cross-fade is two overlaid texts with the recovered alphas.
- The 32dp vertical fading edge of the native page root (`COUINestedScrollView` with `bottom_dialog_scroll_fading_edge_length` = 32dp) is not reproduced; the pages scroll without it.
- Only the default English strings of `coui_guide_dialog_skip_text` / `_next_text` / `_known_text` are carried; the other locales' translations are not.
- The report's gap 1 notes that no style, drawable or guide class references `coui_dialog_guide_clip_radius_root`. The 32dp value is confirmed and used as the panel corner radius here, but the native call site is unresolved.
- The button corner radius and surface colour are inherited from `Widget.COUI.Button` in the native, not pinned to a guide-dialog literal; this port uses the shared `Button` / `TextButton` styles (`ButtonDefaults.buttonColorsPrimary()` for the primary button and the default secondary `TextButton` colours for Skip), whose corner radius follows the theme.
