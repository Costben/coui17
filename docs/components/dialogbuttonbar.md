# DialogButtonBar

`DialogButtonBar` is the button bar of a COUI alert dialog, mirroring ColorOS's `COUIButtonBarLayout`. Its defining behaviour is **automatic vertical stacking**: the bar measures its own labels, and as soon as one of them is too wide to fit in its cell, the whole bar flips from a horizontal row to a vertical stack instead of squeezing or ellipsizing the text.

The two tiers are not just different directions — they use different min heights, paddings, divider thickness and divider insets, and the buttons appear in the **opposite order**.

## Import

```kotlin
import io.github.suqi8.coui.kmp.layout.DialogButtonBar
import io.github.suqi8.coui.kmp.layout.DialogButtonBarAction
import io.github.suqi8.coui.kmp.layout.DialogButtonBarDefaults
```

## Basic Usage

Pass the actions by role, not by position — the bar decides where each one goes:

```kotlin
OverlayDialog(
    show = showDialog,
    title = "Dialog Title",
    summary = "A dialog with a COUI button bar.",
    onDismissRequest = { showDialog = false },
) {
    DialogButtonBar(
        negative = DialogButtonBarAction(text = "Cancel", onClick = { showDialog = false }),
        positive = DialogButtonBarAction(text = "Confirm", onClick = { showDialog = false }),
    )
}
```

Two short labels stay side by side. Long labels flip the bar to a stack automatically, with no extra configuration:

```kotlin
DialogButtonBar(
    negative = DialogButtonBarAction(text = "Not Now", onClick = { showDialog = false }),
    positive = DialogButtonBarAction(
        text = "Delete Backup And Local Copies",
        onClick = { showDialog = false },
    ),
)
```

A third, neutral action is also supported. COUI **always** stacks a three-button bar, regardless of how short the labels are:

```kotlin
DialogButtonBar(
    negative = DialogButtonBarAction(text = "Cancel", onClick = { showDialog = false }),
    positive = DialogButtonBarAction(text = "Save", onClick = { showDialog = false }),
    neutral = DialogButtonBarAction(text = "Discard", onClick = { showDialog = false }),
)
```

## The Stacking Rule

The bar reproduces `COUIButtonBarLayout.onMeasure` exactly. It stays **horizontal only when both of these hold**:

1. Every label fits in its cell, and
2. there are exactly two buttons.

In every other case the bar stacks. In particular, a three-button bar and a single-button bar both take the vertical path. The recommend button does **not** enter this decision: `onMeasure` reads `mDynamicLayout && (getButtonCount() != 2 || needSetButVertical(...))`, and `mRecommendButtonId` only affects the top margin of a single-button stacked bar.

Cell width comes from `needSetButVertical`: the bar width — clamped to `DialogDefaults.MaxWidth` (`coui_dialog_max_width`, 392dp) — minus the two side insets and the inter-cell gaps, split evenly between the buttons, and each cell then loses its two horizontal paddings:

```
cellWidth = (min(392.dp, barWidth) - ButtonBarPaddingHorizontal * 2 - ButtonBarGap * (buttonCount - 1)) / buttonCount
available = cellWidth - ButtonHorizontalPadding * 2
```

A label wider than `available` forces the flip. Labels are measured single-line with a `TextMeasurer` against the resolved `textStyle`, matching COUI's raw paint measurement.

::: tip
The horizontal cells are equal-weight, and the spacing between them comes from each cell's own 4dp inner padding (`coui_alert_dialog_button_parent_padding_horizontal_inside`), which is why `ButtonBarGap` is 8dp rather than a divider width. The ColorOS 17 bar has no divider views at all.
:::

Set `dynamicLayout = false` to pin the bar horizontal, like `COUIButtonBarLayout.setDynamicLayout(false)`.

## Order

The order flips between the two tiers, mirroring `COUIButtonBarLayout.resortButton`:

| Tier | Order |
| :--- | :--- |
| Horizontal | negative → neutral → positive (left to right) |
| Stacked | neutral → positive → negative (top to bottom) |

So the negative (cancel) action is the **leftmost** button when the bar is horizontal and the **bottom-most** one when it stacks.

## No Dividers

A ColorOS 17 dialog button bar draws no dividers. `coui_center_alert_dialog_button_panel.xml` holds three bare `LinearLayout` cells,
`COUIButtonBarLayout.setShowDividerWhenHasItems` is an empty method, and `coui_delete_alert_dialog_divider_height_*` belongs to the
list-panel bar (`coui_panel_view_layout.xml`), not to the dialog. The only separation between two capsules is their own gap and the panel fill.

`showDivider` and `dividerColor` are kept for source compatibility and never change the rendering.

## How the Layers Stack

The capsule is built from three layers, in the same order as `COUIButton.draw()` / `onDraw()`:

| Layer | What it is | How it is done |
| :--- | :--- | :--- |
| Container | The capsule fill | `DialogButtonBarDefaults.buttonFill()` = `0xAAFFFFFF`. Under a blur window COUI does **not** paint its own background (`onDraw` guards it with `if (!isBlurEffectEnabled() \|\| mIsDrawMaterialEffect)`); the capsule is really the panel brightened by the platform blur layer `TYPE_CONTENT_SECONDARY_BUTTON` — measured 229 on the panel and 246 on the capsule, exactly a 67% white overlay |
| Edge light | The rim highlight | `Modifier.materialShadowEdge`. A secondary button uses `MaterialStrokeParams.contentCapsule5()` (`TYPE_CONTENT_CAPSULE_5`, edgeAlpha 0.8); a recommended button uses `ContentCapsule4` (edgeAlpha 0.3) |
| Interaction | Press feedback | `pressScaleEnabled = true`, mirroring `scaleEnable=true` on `COUIAlertDialogBottomButtonSecondary`; a disabled button swaps the fill for `disabledButtonFill()` (`#14000000`) |

The rim paints **over** the fill and **under** the label, exactly like `drawButtonBackground` → `drawMaskAndStroke` → `super.onDraw`.
That is also why the fill has to stay translucent: an opaque fill would turn the rim into a plain border instead of light bleeding in from the edge.

## Metrics

Every inset of the horizontal bar is derived from a `COUIButtonBarLayout` field rather than hardcoded:

| Where | Value | Source |
| :--- | :--- | :--- |
| Capsule to panel side | 16dp | `ButtonBarPaddingHorizontal`, `coui_alert_dialog_button_parent_padding_horizontal` |
| Capsule to capsule | 8dp | `ButtonBarGap`: the cells are flush and each contributes 4dp of `coui_alert_dialog_button_parent_padding_horizontal_inside` |
| Capsule to the content above | 4dp | `ButtonBarPaddingTop`, `coui_alert_dialog_button_parent_padding_vertical` |
| Capsule to the panel bottom | 16dp | `ButtonBarPaddingBottom`, `coui_alert_dialog_button_parent_padding_bottom` |
| Capsule height | 44dp | `ButtonHeight`, `coui_alert_dialog_button_height`; 56dp for a wrapped stacked label (`ButtonMultilineHeight`, `coui_alert_dialog_button_min_height_multiline`) |
| Capsule corner radius | half the height | `DialogBarButton` resolves `minHeight / 2` from the height it is about to draw: 22dp at 44dp, 28dp at the 56dp wrapped tier |

## Properties

### DialogButtonBar Properties

| Property        | Type                   | Description                                      | Default                                     | Required |
| :-------------- | :--------------------- | :----------------------------------------------- | :------------------------------------------ | :------- |
| negative        | DialogButtonBarAction? | Negative (cancel) action, hidden when null       | -                                           | Yes      |
| positive        | DialogButtonBarAction? | Positive (confirm) action, hidden when null      | -                                           | Yes      |
| modifier        | Modifier               | Modifier applied to the bar                      | Modifier                                    | No       |
| neutral         | DialogButtonBarAction? | Third, neutral action, hidden when null          | null                                        | No       |
| dynamicLayout   | Boolean                | Allow the bar to stack; false pins it horizontal | true                                        | No       |
| showDivider     | Boolean                | Kept for source compatibility, draws nothing     | true                                        | No       |
| hasContentAbove | Boolean                | Whether a title, body or custom panel sits above | true                                        | No       |
| colors          | TextButtonColors       | Colors of the non-recommended buttons            | ButtonDefaults.textButtonColorsBorderless() | No       |
| recommendColors | TextButtonColors       | Colors of the button marked `recommended`        | ButtonDefaults.textButtonColorsPrimary()    | No       |
| dividerColor    | Color                  | Kept for source compatibility, draws nothing     | COUITheme.colorScheme.dividerLine           | No       |
| textStyle       | TextStyle              | Label style, also used to measure the labels     | COUITheme.textStyles.button                 | No       |

::: warning
`hasContentAbove` only moves spacing: with nothing above the bar, COUI adds 0dp of extra top padding to the top stacked button
(`coui_bottom_alert_dialog_vertical_button_padding_top_extra_new`) and 0dp of extra top margin to the stacked bar
(`coui_bottom_alert_dialog_buttonbar_margintop`). Keep the default `true` for an ordinary dialog with a title or body.
:::

### DialogButtonBarAction Properties

| Property    | Type       | Description                    | Default | Required |
| :---------- | :--------- | :----------------------------- | :------ | :------- |
| text        | String     | Button label                   | -       | Yes      |
| enabled     | Boolean    | Whether the button is enabled  | true    | No       |
| onClick     | () -> Unit | Invoked when the button is clicked | -   | Yes      |
| recommended | Boolean    | Whether this is the recommended button | false | No  |

### DialogButtonBarDefaults Object

| Property                   | Type          | Description                                        | Value      |
| :------------------------- | :------------ | :------------------------------------------------- | :--------- |
| ButtonBarPaddingHorizontal | Dp            | Capsule padding to the panel side                   | 16.dp      |
| ButtonBarPaddingTop        | Dp            | Padding from the content above                      | 4.dp       |
| ButtonBarPaddingBottom     | Dp            | Padding down to the panel bottom                    | 16.dp      |
| ButtonBarGap               | Dp            | Gap between two capsules, horizontal and stacked     | 8.dp       |
| ButtonHeight               | Dp            | Capsule height                                      | 44.dp      |
| ButtonMultilineHeight      | Dp            | Minimum capsule height once the label wraps          | 56.dp      |
| ButtonInsideMargin         | PaddingValues | Padding around the label inside the capsule          | 12dp / 4dp |
| ButtonHorizontalPadding    | Dp            | Horizontal button padding, also used by the stacking rule | 12.dp |
| StackedButtonPaddingTopExtra | Dp         | Extra top padding for the top stacked button        | 0.dp       |
| StackedButtonPaddingBottomExtra | Dp      | Extra bottom padding for the bottom stacked button  | 0.dp       |
| StackedBarMarginTop        | Dp            | Top margin of a stacked bar                         | 0.dp       |
| buttonFill()               | Color         | Capsule fill                                        | #AAFFFFFF  |
| disabledButtonFill()       | Color         | Disabled capsule fill                               | #14000000  |
| buttonColors()             | TextButtonColors | The whole colour set of an ordinary capsule      | the two above + `onBackground` |
| DangerTextColor            | Color         | Label colour of a destructive action                | #EA3447    |
| DangerDisabledTextColor    | Color         | Its disabled counterpart                            | #66EA3447  |
| dangerButtonColors()       | TextButtonColors | A destructive capsule: same translucent fill, red label | the two above |

### Destructive (red) buttons

A "delete" button is **not** a separate style in ColorOS 17: the fill stays the ordinary translucent
capsule and only the label turns red. Settings does it by hand — both `StyleDialogUtil` and
`ScreenLockFeature` call
`button.setTextColor(R.color.color_delete_alert_dialog_button_warning_color)`, which is `#ffea3447`;
the disabled variant `color_delete_alert_dialog_button_warning_color_disable` is the same red at 40%
alpha.

So there is no new style here, only a named entry point:

```kotlin
DialogButtonBar(
    negative = DialogButtonBarAction(text = "Cancel", onClick = onCancel),
    positive = DialogButtonBarAction(text = "Delete", onClick = onDelete),
    // Swap the whole bar when every action is destructive; for a single dangerous button use
    // recommendColors, or build a TextButtonColors yourself.
    colors = DialogButtonBarDefaults.dangerButtonColors(),
)
```

`OverlayAlertDialog` and `AlertDialogContentLayout` take `buttonColors` and `recommendColors` too, so
a dialog does not have to drop to the lower layer.

::: tip
The same resource table holds two other reds, `coui_color_label_theme_red` and
`coui_color_primary_text_red`, both `#db382c`. Those are the **theme accent** reds used by list rows
and preference titles — a stop darker than the delete-dialog red. Do not mix them up.
:::

The panel-side metrics live in `DialogDefaults`: `MaxWidth` 392dp (`coui_dialog_max_width`), `CornerRadius` 38dp,
`outsideMargin` 16dp/32dp, `insideMargin` 24dp, `ButtonBarMinHeight` 44dp and `NativeBlurRadiusPx` 200.

## Recommended Button

Mark an action with `recommended = true` and it renders as the filled primary button while the rest keep `colors`:

```kotlin
DialogButtonBar(
    negative = DialogButtonBarAction(text = "Later", onClick = { showDialog = false }),
    positive = DialogButtonBarAction(text = "Install", onClick = { showDialog = false }, recommended = true),
)
```

Mirrors `COUIButtonBarLayout.setRecommendButtonId`:

- `getBlurWindowButtonType` returns `ButtonType.PRIMARY` for the button whose id equals `mRecommendButtonId` and `SECONDARY` for the rest, so the fill of the bar splits exactly along the `recommended` flag.
- `applyRecommendStyle` switches the button to `ButtonType.PRIMARY`, recolours the label through `color/coui_btn_default_text_color` and turns on the solid-fill animation mode (`setAnimType(1)`; `COUIButton.java` fills straight from `mDrawableColor` when `mAnimType == 1`). `textButtonColorsPrimary()` covers both the fill and the label tint.
- The recommended button's rim switches to `TYPE_CONTENT_CAPSULE_4` (edgeAlpha 0.3): the primary fill is already bright, so the edge light has to be quieter.
- A single-button stacked bar still gets `StackedBarMarginTop` when that button is the recommended one (`onMeasure` checks `getButtonCount() == 1 && mRecommendButtonId != -1`).

## Behavior Notes

- **The button is a capsule, not a full cell.** `COUIAlertDialogBottomButtonSecondary` sets `drawableRadius` to -1, which `COUIButtonDrawable` clamps to half the measured height, so the radius is always `minHeight / 2`. `DialogBarButton` computes the same value from the height it is about to draw.
- **The bar owns the panel's bottom inset.** `ButtonBarPaddingBottom` (16dp) is the blank space under the panel's rounded footer, so `DialogContentLayout` adds no bottom padding of its own.
- **A stacked bar has a 0dp top margin** unless it is a lone recommended button.
- **A lone negative button falls back to the horizontal metrics.** It is the one stacked case that uses the 44dp panel minimum height and the horizontal padding instead of the stacked set.
- **Pressing scales the capsule.** `scaleEnable=true` together with the cell-wide wash is why the bar passes `pressScaleEnabled = true`.

## Limitations

**The AI-payment recommend button** is not implemented. COUI's `setRecommendButtonType(AI_PAYMENT)` sends the recommended button down a
different path — `ButtonType.AI_PAYMENT`, `setDrawableRadius(-1)`, the `couiColorAiPayment*Text` label colours, and a radial gradient
background drawn by `PopupGradientBackgroundHelper.drawRadialGradient` on material devices and gated by `COUIMaterialEffect` /
`COUIMaterialStateManager` feature flags. This library has no material-effect capability, so `recommended = true` always paints the plain
primary fill.
