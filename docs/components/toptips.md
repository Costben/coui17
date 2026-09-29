# TopTips

A tip banner shown at the top of a page, mirroring the ColorOS top-tips family (`COUICustomTopTips`, `COUIDefaultTopTips`, `COUIDefaultTopTipsView`, `COUIMarqueeTopTips`): a rounded card holding an optional leading icon, an optional title, a message and a trailing cluster of up to two accent text buttons and a round close button. Toggling visibility plays the COUI 250ms vertical scale animation.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.TopTips
import io.github.suqi8.coui.kmp.basic.TopTipsBackgroundStyle
import io.github.suqi8.coui.kmp.basic.TopTipsColors
import io.github.suqi8.coui.kmp.basic.TopTipsDefaults
import io.github.suqi8.coui.kmp.basic.TopTipsStyle
```

## Basic Usage

```kotlin
TopTips(
    text = "New security update available",
    actionText = "Update",
    onAction = { /* handle action */ },
    modifier = Modifier.fillMaxWidth(),
)
```

## Closable Tips with a Start Icon

```kotlin
var visible by remember { mutableStateOf(true) }

TopTips(
    text = "Closable tips with a start icon",
    visible = visible,
    startIcon = {
        Icon(
            imageVector = COUIIcons.Basic.Search,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    },
    onClose = { visible = false },
    modifier = Modifier.fillMaxWidth(),
)
```

## AI Payment Style

`TopTipsStyle.AiPayment` resolves to `TopTipsLayoutMode.AiPayment`, which lays the two buttons out side by side with `coui_toptips_ai_payment_btn_min_width` (144dp), `coui_toptips_ai_payment_btn_min_height` (32dp) and `coui_toptips_ai_payment_btn_horizontal_padding` (12dp). In that layout `changeLayoutWithDualButtons` hides `close`, so `onClose` is ignored while both `ignoreText` and `actionText` are present.

```kotlin
TopTips(
    text = "Review the payment details before you confirm this transaction",
    title = "AI payment",
    startIcon = { Icon(COUIIcons.Basic.Search, contentDescription = null) },
    style = TopTipsStyle.AiPayment,
    ignoreText = "Not now",
    onIgnore = { /* dismiss */ },
    actionText = "Continue",
    onAction = { /* confirm */ },
    modifier = Modifier.fillMaxWidth(),
)
```

## Background Styles

`backgroundStyle` selects the solid fill and is the default for `colors`. `TopTipsBackgroundStyle.White` fills with `?attr/couiColorCard` and `Gray` — the default of `mLastBackgroundStyle` — with `?attr/couiColorContainer4`.

```kotlin
TopTips(
    text = "White tips fill with couiColorCard",
    backgroundStyle = TopTipsBackgroundStyle.White,
    actionText = "Got it",
    onAction = {},
    modifier = Modifier.fillMaxWidth(),
)
```

## Properties

### TopTips

| Property           | Type                       | Description                                                              | Default Value                              | Required |
| ------------------ | -------------------------- | ------------------------------------------------------------------------ | ------------------------------------------ | -------- |
| text               | String                     | The message of the banner                                                | -                                          | Yes      |
| modifier           | Modifier                   | Modifier applied to the banner                                           | Modifier                                   | No       |
| visible            | Boolean                    | Whether the banner is visible (animated in/out)                          | true                                       | No       |
| title              | String?                    | Optional title above the message (2 lines max)                           | null                                       | No       |
| startIcon          | (@Composable () -> Unit)?  | Optional leading icon                                                    | null                                       | No       |
| actionText         | String?                    | Optional label of the primary action button                              | null                                       | No       |
| onAction           | (() -> Unit)?              | Callback when the action button is clicked                               | null                                       | No       |
| ignoreText         | String?                    | Optional label of the secondary button                                   | null                                       | No       |
| onIgnore           | (() -> Unit)?              | Callback when the ignore button is clicked                               | null                                       | No       |
| onClose            | (() -> Unit)?              | Callback of the close button                                             | null                                       | No       |
| marqueeEnabled     | Boolean                    | Whether the message may scroll when it overflows                         | true                                       | No       |
| style              | TopTipsStyle               | `Standard` or `AiPayment`; selects the layout mode                       | TopTipsStyle.Standard                      | No       |
| backgroundStyle    | TopTipsBackgroundStyle     | Solid fill: `White` (`couiColorCard`) or `Gray` (`couiColorContainer4`)  | TopTipsBackgroundStyle.Gray                | No       |
| backgroundOverlay  | (@Composable () -> Unit)?  | Overlay drawn over the fill; blanks the card fill when set               | null                                       | No       |
| cornerRadius       | Dp?                        | Explicit corner radius; null follows the native radius rules            | null                                       | No       |
| colors             | TopTipsColors              | Color configuration                                                      | TopTipsDefaults.topTipsColors(backgroundStyle) | No   |

### TopTipsDefaults

| Constant              | Type | Value  | COUI Source                                |
| --------------------- | ---- | ------ | ------------------------------------------ |
| CornerRadius          | Dp   | 28.dp  | coui_toptips_view_radius (ColorOS 17)      |
| SingleLineCornerRadius| Dp   | 72.dp  | couiRoundCornerFULL                        |
| ContentPadding        | Dp   | 12.dp  | coui_toptips_view_content_top_margin       |
| IconSize              | Dp   | 24.dp  | coui_toptips_view_icon_btn_size            |
| TitleIconSize         | Dp   | 36.dp  | coui_toptips_view_title_icon_size          |
| IconSpacing           | Dp   | 8.dp   | coui_toptips_view_content_start_margin     |
| TextMinHeight         | Dp   | 20.dp  | coui_toptips_view_content_min_height       |
| TitleMinHeight        | Dp   | 22.dp  | coui_toptips_view_title_min_height         |

`getMultiLineRadius` uses `CornerRadius` for any banner that is not a single untitled line; `getSingleLineRadius` uses `SingleLineCornerRadius` for an untitled message that fits one line.

#### Methods

| Method Name            | Type          | Description                                                                     |
| ---------------------- | ------------- | ------------------------------------------------------------------------------- |
| titleStyle()           | TextStyle     | Default title style (16sp medium, `couiTextHeadlineXS`)                          |
| textStyle()            | TextStyle     | Default message style (13sp, `couiTextBodyS`)                                    |
| actionTextStyle()      | TextStyle     | Default button label style (13sp medium, `coui_toptips_view_default_text_size`)  |
| topTipsColors()        | TopTipsColors | Creates the default color configuration (see below)                              |
| topTipsContainerColor()| Color         | Solid fill for a `TopTipsBackgroundStyle`                                       |

### `topTipsColors()` factory

| Parameter        | Type                 | Default                                        | COUI Attribute        |
| ---------------- | -------------------- | ---------------------------------------------- | --------------------- |
| backgroundStyle  | TopTipsBackgroundStyle | TopTipsBackgroundStyle.Gray                  | mLastBackgroundStyle  |
| containerColor   | Color                | topTipsContainerColor(backgroundStyle)         | couiColorCard / couiColorContainer4 |
| contentColor     | Color                | COUITheme.colorScheme.onSurfaceSecondary       | couiColorLabelSecondary |
| actionColor      | Color                | COUITheme.colorScheme.primary                  | couiColorPrimaryText  |
| closeButtonColor | Color                | #29000000 light / #40FFFFFF dark               | couiColorControls     |
| closeIconColor   | Color                | Color.White                                    | coui_ic_toptips_close |
| titleColor       | Color                | COUITheme.colorScheme.onSurface                | couiColorLabelPrimary |

## Enums

| Enum                    | Members                  | Source                                                          |
| ----------------------- | ------------------------ | --------------------------------------------------------------- |
| TopTipsStyle            | Standard, AiPayment      | `COUIDefaultTopTips.TopTipsStyle` (`STANDARD`, `AI_PAYMENT`)    |
| TopTipsBackgroundStyle  | White, Gray              | `COUIDefaultTopTips.BackgroundStyle` (`WHITE`, `GRAY`)          |
| TopTipsLayoutMode       | Default, AiPayment       | `COUIDefaultTopTipsView.LayoutMode`, from `resolveTargetLayoutMode` |

## Behavior

- The trailing cluster is `ignore` then `action` then `close`, matching `COUIDefaultTopTipsView`. `close` is hidden in the AI payment dual-button layout (`changeLayoutWithDualButtons`).
- When the single-line message would collide with the trailing cluster, the cluster drops to its own end-aligned row below the text (`isNeedMultiText`); in that layout the start icon aligns with the first text line.
- The leading icon is `IconSize` (24dp) without a title and `TitleIconSize` (36dp) with one.
- The message scrolls when it overflows, matching `COUIMarqueeTopTips.startRoll` / `stopRoll`; `marqueeEnabled` toggles the same state.
- Toggling `visible` plays the `COUICustomTopTips` show/dismiss animation: a 250ms vertical scale pivoting at the center; the banner leaves the composition once the dismiss animation finishes.
- The message is 13sp (`couiTextBodyS`) and the button labels are 13sp medium; the title is 16sp medium (`couiTextHeadlineXS`).
