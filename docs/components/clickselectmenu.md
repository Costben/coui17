# ClickSelectMenu

`ClickSelectMenu` places a ColorOS popup list at the point the user actually clicked. A plain dropdown opens centred on the anchor row; a click-select menu opens under the finger, and a *placement spec* decides which side and which edge of the menu are pinned to that point. It is what ColorOS settings rows use for their inline menus — the menu appears where you tapped, not where the row is.

It ports `com.coui.appcompat.poplist.COUIClickSelectMenu`, `COUIIsolatedPopupListWindow`, `MainMenuPlacementSpec` and the placement rules of `RotatingPopupMenuLocateHelper`, and reuses the popup list surface this library already ports.

<div style="position: relative; height: 250px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../compose/index.html?id=clickSelectMenu" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.ClickSelectMenuAlignment
import io.github.suqi8.coui.kmp.basic.ClickSelectMenuDefaults
import io.github.suqi8.coui.kmp.basic.ClickSelectMenuPlacement
import io.github.suqi8.coui.kmp.basic.ClickSelectMenuSide
import io.github.suqi8.coui.kmp.basic.IsolatedPopupAnchor
import io.github.suqi8.coui.kmp.basic.clickSelectMenuPositionProvider
import io.github.suqi8.coui.kmp.basic.preciseClickAnchor
import io.github.suqi8.coui.kmp.basic.rememberPreciseClickState
```

## Native Origin

`COUIClickSelectMenu` is a thin coordinator: it stores its items on a `COUIPopupListWindow`, sets the anchor view, and attaches a `PreciseClickHelper` whose click callback calls `setPopupShow(view, x, y)` with the recorded DOWN point. Two halves of that are already in this library, so they were not re-ported:

| Native piece | Already ported as |
| :--- | :--- |
| `PreciseClickHelper` (records the DOWN point, falls back to the view centre for TalkBack) | `Modifier.preciseClickAnchor` |
| `PopupMenuLocateHelper.setAnchor` (collapses the anchor bounds to the point) | `ListPopupLayout` |
| `MenuRotationHelper` quarter classification and fit-rect clamp | `PopupMenuRotationDefaults` |
| The default and context-menu placement | `ListPopupDefaults.DropdownPositionProvider` / `ContextMenuPositionProvider` |

What none of them carry is a **placement spec**. `ListPopupDefaults.dropdownPositionProvider` always centres the menu on the anchor and then clamps; `RotatingPopupMenuLocateHelper` first resolves a `MainMenuPlacementSpec` and then forces a side, forces an edge, or defers to the fit test. That difference, plus the synthetic anchor `COUIIsolatedPopupListWindow` builds for its overlay window, is what this page documents.

The two halves are separate classes natively. `RotatingPopupMenuLocateHelper` is constructed only by `COUIRotatingPopupListWindow`, which extends `COUIPopupListWindow` and owns the spec itself — `setMainMenuPlacement` / `resetMainMenuPlacement`, with `reuseMainMenu` resolving a fitting spec before it locates. `COUIClickSelectMenu` drives a plain `COUIPopupListWindow`, whose `PopupMenuLocateHelper` carries no spec at all. They are documented together here because in Compose a single `PopupPositionProvider` is the one place that decides where a popup opens, so the side/alignment rules and the click-point anchor necessarily meet there.

## Basic Usage

Wire the anchor to a click point, then hand the same state to the popup:

```kotlin
val preciseClickState = rememberPreciseClickState()
var showPopup by remember { mutableStateOf(false) }
val placement = ClickSelectMenuPlacement(
    side = ClickSelectMenuSide.Auto,
    alignment = ClickSelectMenuAlignment.Default
)

Text(
    text = "Tap anywhere on this row",
    modifier = Modifier
        .fillMaxWidth()
        .clickable { showPopup = true }
        .preciseClickAnchor(preciseClickState)
)

WindowListPopup(
    show = showPopup,
    popupPositionProvider = clickSelectMenuPositionProvider(placement),
    preciseClickState = preciseClickState,
    onDismissRequest = { showPopup = false }
) {
    val dismiss = LocalDismissState.current
    ListPopupColumn {
        rows.forEachIndexed { index, text ->
            DropdownImpl(
                text = text,
                optionSize = rows.size,
                isSelected = false,
                index = index,
                onSelectedIndexChange = { dismiss?.invoke() }
            )
        }
    }
}
```

`preciseClickAnchor` records the touch point in window coordinates and consumes nothing, so the row's own `clickable` still sees the whole gesture. `WindowListPopup` then collapses the anchor bounds to that point before asking the position provider where the menu goes, exactly as `PopupMenuLocateHelper.setAnchor` does natively.

## Placement Spec

`ClickSelectMenuPlacement` mirrors `MainMenuPlacementSpec`, whose two halves are int constants validated by `isValidSide` / `isValidAlignment` (an unknown value logs a warning and falls back to `DEFAULT = ofSideAndAlignment(0, 0)`). Both halves are enums here, so the fallback is unreachable.

`ClickSelectMenuSide` (`MainMenuPlacementSpec.SIDE_*`):

| Side | Value | Behaviour |
| :--- | :--- | :--- |
| `Auto` | 0 | The fit test decides: below the anchor if it fits, otherwise above, otherwise the top of the available bounds |
| `Above` | 1 | Pinned above and clamped into the available bounds |
| `Below` | 2 | Pinned below and clamped into the available bounds |
| `Start` | 3 | Placed beside the anchor, on the cross axis, on the start side |
| `End` | 4 | Placed beside the anchor, on the cross axis, on the end side |

`ClickSelectMenuAlignment` (`MainMenuPlacementSpec.ALIGN_*`):

| Alignment | Value | Behaviour |
| :--- | :--- | :--- |
| `Default` | 0 | Centred on the anchor, then clamped into the available bounds |
| `Start` | 1 | The menu's start edge is forced onto the anchor's start edge |
| `End` | 2 | The menu's end edge is forced onto the anchor's end edge |

`Start` / `End` are logical: `resolvePopupSide` and `physicalAlignment` swap them under RTL, because `MenuRotationHelper.physicalHAlignForCrossAxis` works in physical coordinates while the spec is written logically.

## Recovered Rules

`ClickSelectMenuDefaults` holds the locate rules as plain functions, so a caller can reason about a placement without opening a popup. Each one cites the native method it came from.

| Function | Native source |
| :--- | :--- |
| `locateMainMenuXDefault` | `RotatingPopupMenuLocateHelper.locateMainMenuXDefault` |
| `locateMainMenuAnchorX` | `RotatingPopupMenuLocateHelper.computeLocateMainMenuAnchorX` |
| `locateMainMenuY` | `RotatingPopupMenuLocateHelper.computeLocateMainMenuY` |
| `forcedMenuLeft` | `MenuRotationHelper.forcedMenuLeft` |
| `physicalAlignment` | `MenuRotationHelper.physicalHAlignForCrossAxis` |
| `resolvePopupSide` | `RotatingPopupMenuLocateHelper.resolvePopupSide` |
| `shouldApplyCenterAlignY` | `RotatingPopupMenuLocateHelper.shouldApplyCenterAlignY` |
| `explicitSideBounds` | `MenuRotationHelper.explicitSideDisplayBounds` |
| `horizontalClearance` | `MenuRotationHelper.computeExplicitSideHorizontalClearancePx` |
| `resolveFittingSpec` | `RotatingPopupMenuLocateHelper.resolveFittingSpec` |
| `mainMenuMeasureCaps` | `RotatingPopupMenuLocateHelper.computeMainMenuMeasureCaps` |
| `transformedAnchorBounds` | `RotatingPopupMenuLocateHelper.setAnchorBoundsInWindow` |

Two details are easy to get wrong and are preserved:

- **The X locate reads the bare anchor; the Y locate reads the anchor expanded by its outsets.** `setupMainMenuLocateXRule` reads `PopupMenuDomain.mAnchor`, while `setupMainMenuLocateYRule`, `computeMainMenuMeasureCaps` and `explicitSideDisplayBounds` read `getAnchorRealRect`, which is `mAnchor` expanded by `mAnchorOutsets`.
- **A point anchor means no vertical gap.** `PopupMenuRuleExecutor.executeConfigRules` picks the default anchor rule (outsets `0, 8dp, 0, 8dp`) only when the anchor was set without a click offset, and the context anchor rule (empty outsets) when one was supplied. `setAnchor` collapses the bounds in exactly the second case, so a zero-area anchor always means empty outsets.

## Isolated Popup Window

`COUIIsolatedPopupListWindow` shows its popup through `WindowManager` instead of anchoring it to a real view. `buildDummyAnchor` creates a `DummyRootView` sized to the default display, adds a 1x1 `DummyAnchorView` at the requested point as its child, and never attaches either to a window:

- `DummyAnchorView` overrides `getGlobalVisibleRect` to report its own laid-out box, so the anchor is the touch point without a view tree.
- `DummyAnchorView` also implements `PopupMenuConfigRule` with `getType() == 1`, so `executeConfigRules` takes the `view instanceof PopupMenuConfigRule` branch and uses the dummy's 1x1 `getDisplayFrame` and its empty `getOutsets` as the anchor rule.
- The anchor's root is unattached, which `completeAnchorSetup` detects and turns into `mIgnoreBarrier`, so no window barrier applies.

`IsolatedPopupAnchor` models that contract — `anchorBounds` is the 1x1 box, `rootBounds` is the display — and `ClickSelectMenuDefaults.isolatedAnchor` builds it from a point and a display size. The window itself is added with `type = 2038`, `format = 1` and `gravity = 51`.

## Recovered Tokens

Every value below was recovered from the ColorOS 17 resource dump of `Settings.apk` and its dex, `classes7.dex`; none of them is invented.

| Token | Value | Source |
| :--- | :--- | :--- |
| `DUMMY_ANCHOR_SIZE` | 1px | `COUIIsolatedPopupListWindow.DummyAnchorView` (`setRight(1)` / `setBottom(1)`) |
| `CLICK_SELECT_DELAY_MILLIS` | 100 | `integer/support_menu_click_select_time` |
| `CLICK_SELECT_DELAY_MILLIS_HEIGHT_840DP` | 240 | `integer/support_menu_click_select_time` (`h840dp`) |
| `VerticalGapToAnchor` | 8dp | `dimen/coui_popup_list_window_default_vertical_gap_to_anchor` |
| `ISOLATED_WINDOW_TYPE` | 2038 | `COUIIsolatedPopupListWindow.show` |
| `ISOLATED_WINDOW_FORMAT` | 1 | `COUIIsolatedPopupListWindow.show` |
| `ISOLATED_WINDOW_GRAVITY` | 51 | `COUIIsolatedPopupListWindow.show` |
| Isolated window exit | alpha 1 → 0, 250ms, `cubic-bezier(0.33, 0, 0.67, 1)` | `style/Animation.COUI.IsolatedPopupListWindow` → `anim/coui_isolated_popup_list_exit` + `anim/coui_ease` |
| Isolated window enter | empty animation | `style/Animation.COUI.IsolatedPopupListWindow` → `anim/coui_popup_list_enter`, an empty `<set>` |
| `SIDE_AUTO` / `SIDE_ABOVE` / `SIDE_BELOW` / `SIDE_START` / `SIDE_END` | 0 / 1 / 2 / 3 / 4 | `MainMenuPlacementSpec` |
| `ALIGN_DEFAULT` / `ALIGN_START` / `ALIGN_END` | 0 / 1 / 2 | `MainMenuPlacementSpec` |
| `MainMenuPlacementSpec.DEFAULT` | `ofSideAndAlignment(0, 0)` | `MainMenuPlacementSpec.DEFAULT` |
| Default horizontal margin | 16dp | `ListPopupDefaults.HorizontalBarrier` (the responsive-grid margin in `mWindowBarriers`) |

No colour, radius or easing of the popup surface is duplicated here: the surface itself is `ListPopupContent`, which already carries the recovered `coui_popup_round_frame_layout_corner_radius`, blur blend pair and shadow.

## Not Ported

- **The `WindowManager` overlay itself.** `COUIIsolatedPopupListWindow.show` adds its content view to a `WindowManager` with `type = 2038`; Compose Multiplatform has no portable equivalent, so the isolated window's *anchor contract* is modelled by `IsolatedPopupAnchor` and the window's own parameters are exposed as constants, but the host is the existing `WindowListPopup` / `OverlayListPopup` instead.
- **`COUIClickSelectMenu`'s IME handling.** The native hides the soft keyboard and, when the keyboard was visible, waits `support_menu_click_select_time` before showing the menu so the layout has settled. The library has no shared IME controller to hang that off, so the delay is exposed as a constant and the host decides whether to apply it.
- **`COUIClickSelectMenu.registerForClickSelectItems` as a widget.** The native mutates an existing `View` (sets it clickable and long-clickable, sets the item list on its popup, replaces the helper when the anchor changes). The Compose equivalent is composing `preciseClickAnchor` on the anchor and passing the shared state to the popup, which is what the sample above does.
- **`computeMainMenuMeasureCaps`'s orthogonal width cap** is written verbatim as the native bytecode has it — `Math.min(availableHeight, availableHeight)` — rather than "corrected" to a value it never computes.
- **`transformedAnchorBounds` has no host plumbing yet.** The rule is recovered and exposed, but nothing in this library feeds a caller's transformed anchor bounds into a `PopupPositionProvider`: the popup layout takes its anchor bounds from `onGloballyPositioned`. A caller placing a menu against a rotated anchor can compute the bounds with it, but cannot yet inject them.
- **`getMainMenuEnterPivotX` / `inferMainMenuPhysicalHAlign`.** These read the forced horizontal edge this port returns in `ClickSelectMenuHorizontalResult.forcedEdge`. `ListPopupLayout` already derives its scale pivot from the anchor's centre rather than from the forced edge, so the forced-edge pivot is not reproduced.
- **`COUIRotatingPopupListWindow`'s dispatch.** The window chooses between its default, explicit-side, orthogonal and legacy layouts (`needsExtendedMainMenuLayout`, `useSidePlacement`, `prepareShowMainMenuExplicitSide` / `prepareOrthogonalMainMenu`) and swaps the located size for `getRotatedLocateWidth` / `getRotatedLocateHeight`. This port exposes the individual rules and the size swap (`PopupMenuRotationDefaults.rotatedLocateWidth`), but the dispatch itself stays with the caller: compose `popupMenuRotationPositionProvider` around `clickSelectMenuPositionProvider` for a rotated menu.
