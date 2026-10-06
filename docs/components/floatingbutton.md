# FloatingButton

The ColorOS 17 speed dial: one round main button that opens a stack of labelled child buttons, aligned with the ColorOS 17 design language. Each child row carries a label and a leading icon.

`FloatingActionButton` is the single COUI floating button; this is the container built on top of it, so the main button and the children keep that component's press feedback, ripple and role.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.FloatingButton
import io.github.suqi8.coui.kmp.basic.FloatingButtonDefaults
import io.github.suqi8.coui.kmp.basic.FloatingButtonItem
```

## Basic Usage

```kotlin
var expanded by remember { mutableStateOf(false) }
val items = remember {
    listOf(
        FloatingButtonItem(icon = COUIIcons.AddFolder, label = "New folder"),
        FloatingButtonItem(icon = COUIIcons.Favorites, label = "Add to favorites"),
        FloatingButtonItem(icon = COUIIcons.Tune, label = "Settings"),
    )
}

Box(
    modifier = Modifier.fillMaxWidth().height(320.dp).padding(24.dp),
    contentAlignment = Alignment.BottomEnd,
) {
    FloatingButton(
        items = items,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        onItemClick = { false },
        mainIcon = COUIIcons.Add,
    )
}
```

`items.first()` is the row nearest the main button, and the last item is the one that opens first. The dial is as wide as its widest row while it is open, so anchor it to the end of a `Box` to keep the main button in place. A closed dial is exactly one button tall: the native sets every `COUIFloatingButtonLabel` to `View.GONE` in `visibilitySetup`, and this port leaves the rows out of the composition once the close animation has run.

## Keeping the Menu Open

`onItemClick` returns a `Boolean`, like `OnActionSelectedListener.onActionSelected`: returning `false` closes the menu, returning `true` keeps it open.

```kotlin
FloatingButton(
    items = items,
    expanded = expanded,
    onExpandedChange = { expanded = it },
    onItemClick = { item -> item.label == "Settings" },
    mainIcon = COUIIcons.Add,
)
```

## Properties

### FloatingButton

| Property         | Type                          | Description                                                       | Default Value                | Required |
| ---------------- | ----------------------------- | ----------------------------------------------------------------- | ---------------------------- | -------- |
| items            | List&lt;FloatingButtonItem&gt;| The actions; the first is the row nearest the main button         | -                            | Yes      |
| expanded         | Boolean                       | Whether the menu is open                                          | -                            | Yes      |
| onExpandedChange | (Boolean) -> Unit             | Callback with the new state when the main button is clicked       | -                            | Yes      |
| onItemClick      | (FloatingButtonItem) -> Boolean | Called with the clicked action; `true` keeps the menu open      | -                            | Yes      |
| mainIcon         | ImageVector                   | The glyph of the main button                                      | -                            | Yes      |
| modifier         | Modifier                      | Modifier applied to the speed dial                                | Modifier                     | No       |
| enabled          | Boolean                       | Whether the speed dial reacts to clicks                           | true                         | No       |
| colors           | FloatingButtonColors          | The palette of the buttons and the label chips                    | FloatingButtonDefaults.colors() | No    |

The native reads the main button's glyph from `attr/mainFloatingButtonSrc`, which the app owns, so `mainIcon` has no default. Gallery passes `@drawable/base_menu_ic_add_sliding_up` (a 32dp plus); the `coui-icons` module's `COUIIcons.Add` is the equivalent glyph, and that module is not a dependency of `coui-ui`.

### FloatingButtonItem

| Property | Type        | Description                                                  | Default Value | Required |
| -------- | ----------- | ------------------------------------------------------------ | ------------- | -------- |
| icon     | ImageVector | The glyph of the child button                                | -             | Yes      |
| label    | String      | The label chip; an empty label draws no chip                 | -             | Yes      |
| enabled  | Boolean     | Whether the action can be clicked                            | true          | No       |

### FloatingButtonColors

| Property                | Type  | Description                                    |
| ----------------------- | ----- | ---------------------------------------------- |
| containerColor          | Color | Fill of the main and child buttons             |
| contentColor            | Color | Glyphs, drawn over that container              |
| disabledContainerColor  | Color | Fill of a disabled button                      |
| labelContainerColor     | Color | The chip behind a label                        |
| labelTextColor          | Color | The label text                                 |
| labelDisabledTextColor  | Color | A disabled label                               |

### FloatingButtonDefaults

| Constant            | Type                   | Default Value                  | COUI Source                                          |
| ------------------- | ---------------------- | ------------------------------ | ---------------------------------------------------- |
| ItemSize            | Dp                     | 56.dp                          | coui_floating_button_size / coui_floating_button_fab_normal_size |
| IconSize            | Dp                     | 24.dp                          | no dimen in the dumps; the app-supplied glyphs are 24dp boxes on a 56dp button |
| FirstItemSpacing    | Dp                     | 32.dp                          | coui_floating_button_item_first_bottom_margin        |
| ItemSpacing         | Dp                     | 16.dp                          | coui_floating_button_item_normal_bottom_margin       |
| LabelSpacing        | Dp                     | 12.dp                          | coui_floating_button_fab_side_margin                 |
| LabelCornerRadius   | Dp                     | 4.dp                           | attr/couiRoundCornerXS → coui_round_corner_xs        |
| LabelTextHorizontalPadding | Dp              | 8.dp                           | coui_floating_button_item_label_label_text_margin_start/end |
| LabelTextVerticalPadding   | Dp              | 4.dp                           | coui_floating_button_item_label_label_text_margin_top/bottom |
| LabelMaxWidth       | Dp                     | 220.dp                         | coui_floating_button_item_label_label_text_max_width |
| LABEL_MAX_LINES     | Int                    | 2                              | integer/coui_floating_button_item_label_label_text_max_line |
| EXPAND_ICON_ROTATION | Float                 | 45f                            | COUIFloatingButton.DEFAULT_ROTATE_ANGLE              |
| ROTATE_DURATION     | Int                    | 250                            | COUIFloatingButton.DEFAULT_ROTATE_ANIMATION_DURATION |
| ROTATE_DURATION_NO_ITEM | Int                | 300                            | COUIFloatingButton.DEFAULT_ROTATE_ANIMATION_DURATION_NO_ITEM |
| OpenElevation       | Dp                     | 8.dp                           | coui_floating_button_open_elevation                  |
| CloseElevation      | Dp                     | 5.dp                           | coui_floating_button_close_elevation                 |
| ExpandStagger       | Duration               | 50.milliseconds                | COUIFloatingButton.ACTION_ANIM_DELAY                 |
| MIN_SCALE           | Float                  | 0.6f                           | COUIFloatingButton.DEFAULT_SCALE_ANIMATION_MIN_VALUE |
| ANIMATION_DURATION  | Int                    | 300                            | DEFAULT_MAIN_FLOATING_BUTTON_ANIMATION_DURATION      |
| MoveEaseIn          | Easing                 | cubic-bezier(0.25, 0.1, 0.25, 1) | mExpandMenuAnimationInterpolator / mLabelPathInterpolator |
| MoveEase            | Easing                 | cubic-bezier(0.3, 0, 0.1, 1)   | COUIMoveEaseInterpolator, anim/coui_ease             |
| ExpandSpec          | FiniteAnimationSpec&lt;Float&gt; | 300ms MoveEaseIn      | animationFloatingButtonMenuExpand animatorSet        |
| CollapseSpec        | FiniteAnimationSpec&lt;Float&gt; | 300ms MoveEase        | closeFloatingButtonMenu(false, 300)                  |
| SlideSpring         | FiniteAnimationSpec&lt;Float&gt; | stiffness 500 / damping 0.8 | animationFloatingButtonMenuExpand SpringAnimation |

#### Methods

| Method Name             | Type      | Description                                                                     |
| ----------------------- | --------- | ------------------------------------------------------------------------------- |
| itemBottomOffset(index)  | Dp       | Distance from the container bottom to a row's bottom edge — `getTotalLabelHeight`, `(index * 72) + 88` dp |
| labelTextStyle()        | TextStyle | Label style (COUITheme.textStyles.footnote1, COUI coui_floating_button_item_label_label_text_size 12sp) |

### Colours

| COUI token                                                                    | Value                 | Used for                       | Port                          |
| ----------------------------------------------------------------------------- | --------------------- | ------------------------------ | ----------------------------- |
| attr/couiColorPrimary → color/coui_floating_button_main_button_color_selector | theme primary         | Enabled button fill            | primary                       |
| attr/couiColorPressBackground → main_button_color_selector state_enabled=false | #14000000 / #26ffffff | Disabled button fill           | file constant, no theme slot  |
| attr/couiColorOnPrimary                                                       | #ffffffff             | Glyphs                         | onPrimary                     |
| color/coui_floating_button_label_broader_color                                | #1a000000 / #1affffff | Label chip fill and stroke     | file constant, no theme slot  |
| color/coui_floating_button_label_text_color_enable                           | #ffffffff             | Label text                     | file constant, no theme slot  |
| color/coui_floating_button_label_text_color_disable                          | #26000000 / #26ffffff | Disabled label text, disabled main glyph | file constant, no theme slot |
| color/coui_button_neutral_color                                               | #0f000000 / #4cffffff | `FloatingButton.Icon.Secondary` fill | Not ported — see below   |

The label chip's stroke uses the same `coui_floating_button_label_broader_color` at `coui_floating_button_item_stroke_width` (0dp), so it is invisible. `coui_floating_button_item_label_background_radius` (6dp) exists but `coui_floating_button_item_label` resolves `cardCornerRadius` from `?attr/couiRoundCornerXS` (4dp) instead.

### Text

| Item        | Value                | Source                                                     |
| ----------- | -------------------- | ---------------------------------------------------------- |
| Label       | 12sp, max 2 lines    | coui_floating_button_item_label_label_text_size            |
| Ellipsize   | END                  | layout/coui_floating_button_item_label TextView            |
| Label width | maximum 220dp        | coui_floating_button_item_label_label_text_max_width       |

## Behavior

- A row's bottom edge sits `getTotalLabelHeight(index)` = `(index * 72) + 88` dp above the container's bottom while the menu is open: 56dp of row, 32dp of `coui_floating_button_item_first_bottom_margin`, then `index` times 56dp + 16dp. The rows are a bottom-anchored overlay, not a stack in the layout.
- Opening scales and fades each row from `0.6f` to `1f` over 300ms on cubic-bezier(0.25, 0.1, 0.25, 1) while a `SpringAnimation(TRANSLATION_Y, 0f)` with stiffness 500 and damping ratio 0.8 slides it up from that offset. The animation lives in `animatorSet` inside `animationFloatingButtonMenuExpand`, so the native applies the same spring to the translation and the interpolator to the scale/fade.
- The rows are staggered by `ACTION_ANIM_DELAY` (50ms). Opening animates the row *farthest* from the main button first — `visibilitySetup` walks `i3` from 0 and animates label `(size - 1) - i3`; closing animates the nearest one first (`i5 * 50`).
- Both animations clear the action listener while they run (`setOnActionSelectedListener(null)` in the first/last label's `onAnimationStart`, restored in `onAnimationEnd`), so a row only reacts once the menu has settled.
- The main glyph rotates 45 degrees while the menu is open, over `DEFAULT_ROTATE_ANIMATION_DURATION` (250ms) with `mRotateForwardInterpolator` / `mRotateBackwardInterpolator`, or 300ms when there is nothing to open.
- The main button's shadow is 8dp open (`coui_floating_button_open_elevation`) and 5dp closed (`coui_floating_button_close_elevation`); the child buttons carry no shadow (`style/FloatingButton.Icon` sets `fabNeedElevation=false`).

## Not Ported

- `COUIFloatingButtonSeamlessImpl` and `COUIFloatingButtonTouchAnimation`, the seamless material transitions, together with the `COUIMaterialBlurEffect` path of `setBlurEffectEnable`.
- `COUIMaskEffectDrawable` (`mMaskDrawable`, touched by `onTouchEvent` and `animatePress`) and the caustic outer shadow of `COUIFABShadowUtils.setOuterShadowEffect`.
- `COUIFloatingButton$COUIFloatingButtonBehavior`, `$ScrollViewBehavior`, the `removeFloatingButtonItemWithWindowHeight` window-height clipping that drops rows that would not fit on screen, and the `AutoDismissRunnable` scheduled 5s after a scroll.
- The `fabTranslateEnhancementRatio` / `fabAlignmentMode` / `fabCradleMargin` attribute families, which `FloatingButton.Icon.Medium.Secondary` / `.Translate` set but no ported path reads. `FloatingButton.Icon.Medium.Primary` / `.Secondary` / `.Tiny` / `.Small` therefore all draw the same 56dp button.
- The vertical pivot the native switches per direction (the label chip's bottom edge while closing and its top edge while opening); the port keeps its end edge and centres vertically.
- The disabled-state compositing of `COUIFloatingButton.getDisabledColor` (`ColorUtils.compositeColors(couiColorDisable, coui_floating_button_disabled_base_color)`), which needs the runtime `couiColorDisable` attribute; the port uses the state list's own disabled item, `?attr/couiColorPressBackground`.
- The `performHapticFeedback()` of `animatePress`.
- Tap-through while a row is moving. A row that is still animating is inert here, as it is in the native, but a Compose row still consumes the tap it receives, so a tap landing during the 300ms open/close animation is swallowed instead of reaching the main button underneath.
- `FloatingActionButton` has no `enabled` parameter, so a disabled button here is disabled by colour and by a guarded click rather than by an inert surface.
