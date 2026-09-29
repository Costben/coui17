# PopupMenuRotation

`PopupMenuRotation` is the rotation geometry ColorOS 17 applies to popup menus. On a rotated screen — a landscape device, a foldable's half-open posture, a rotated window — the native popup list can be laid out with its content turned a quarter turn, and the locate pass then has to reason about the *rotated* footprint rather than the menu's own width and height.

This is not a visible widget. It is the maths behind one: the quarter-angle classifier, the rotated axis bounds, the fit-rect clamp, and the two modifiers that turn them into a placed popup. It ports `com.coui.appcompat.poplist.MenuRotationHelper` and the geometry half of `COUIRotatingPopupListWindow`.

<div style="position: relative; height: 250px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../compose/index.html?id=popupMenuRotation" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.PopupMenuRotation
import io.github.suqi8.coui.kmp.basic.PopupMenuRotationDefaults
import io.github.suqi8.coui.kmp.basic.popupMenuRotation
import io.github.suqi8.coui.kmp.basic.popupMenuRotationPositionProvider
```

## Native Origin

The *positioning* half of `COUIContextMenu` is already covered in this library. [ListPopup](../components/listpopup) ports the same `PreciseLongPressHelper` as `Modifier.preciseClickAnchor`, the same `PopupMenuLocateHelper.setAnchor` as `ListPopupDefaults.dropdownPositionProvider`, and the same overlay and window containers as `OverlayListPopup` / `WindowListPopup`.

What was missing is the rotation. Nothing in this library rotated menu content, and no ported class carried `MenuRotationHelper`'s geometry. That is the gap this file fills.

`MenuRotationHelper` accepts a rotation only when it is a multiple of 90 degrees modulo 360 (within 0.05), and then only when it is within 0.05 of 0 or within 1.0 of ±90. Anything else is classified as `None`, exactly as the native `classifyDegrees` does after logging a warning.

## Basic Usage

```kotlin
var showPopup by remember { mutableStateOf(false) }
val rotation = PopupMenuRotation.Degrees90

WindowListPopup(
    show = showPopup,
    // The locate pass sees the swapped footprint: rotatedLocateWidth/Height.
    popupPositionProvider = popupMenuRotationPositionProvider(rotation),
    // The content itself is rotated about its own centre.
    popupModifier = Modifier.popupMenuRotation(rotation),
    onDismissRequest = { showPopup = false }
) {
    ListPopupColumn {
        DropdownImpl(text = "Copy", optionSize = 3, isSelected = false, index = 0) { }
    }
}
```

## Geometry API

```kotlin
val rotation = PopupMenuRotationDefaults.classifyDegrees(90f) // PopupMenuRotation.Degrees90

// The footprint the locate pass reasons about.
val locateWidth = PopupMenuRotationDefaults.rotatedLocateWidth(rotation, 200, 120)  // 120
val locateHeight = PopupMenuRotationDefaults.rotatedLocateHeight(rotation, 200, 120) // 200

// Where the wrapper has to go, and the bounds it occupies on screen.
val layout = PopupMenuRotationDefaults.computeMenuRotationLayout(
    anchorBounds = IntRect(100, 100, 300, 220),
    width = 200,
    height = 120,
    rotation = rotation,
    fitBounds = IntRect(0, 0, 1080, 2400)
)
```

`MenuRotationLayout` carries the wrapper origin, the rotation pivot, and the resulting on-screen bounds.

## Parameters

### `popupMenuRotation`

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `rotation` | `PopupMenuRotation` | The rotation to draw the content with |

`PopupMenuRotation.None` returns the modifier chain untouched, so an upright menu pays nothing.

### `popupMenuRotationPositionProvider`

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `rotation` | `PopupMenuRotation` | The rotation the content is drawn with |
| `base` | `PopupPositionProvider` | The provider that decides where the menu goes; defaults to `ListPopupDefaults.DropdownPositionProvider` |

### Rotation helpers

| Function | Mirror of |
| :--- | :--- |
| `classifyDegrees(degrees)` | `MenuRotationHelper.classifyDegrees` |
| `isMultipleOf90Mod360(degrees)` | `MenuRotationHelper.isMultipleOf90Mod360` |
| `smallestEquivalentDegrees(degrees)` | `MenuRotationHelper.smallestEquivalentDeg` |
| `clampPivot(value, size)` | `MenuRotationHelper.clampPivot` |
| `rotatedLocateWidth(rotation, width, height)` | `MenuRotationHelper.rotatedLocateWidth` |
| `rotatedLocateHeight(rotation, width, height)` | `MenuRotationHelper.rotatedLocateHeight` |
| `localAxisBounds(width, height, rotation)` | `MenuRotationHelper.localAxisBounds` |
| `screenAxisBounds(left, top, width, height, rotation)` | `MenuRotationHelper.screenAxisBounds` |
| `clampDeltaToFitRectInRect(bounds, fitBounds)` | `MenuRotationHelper.computeClampDeltaToFitRectInRect` |
| `computeRotationLayoutFromWrapper(...)` | `MenuRotationHelper.computeRotationLayoutFromWrapper` |
| `computeMenuRotationLayout(...)` | `MenuRotationHelper.computeRotationLayout` |

## Recovered Values

`MenuRotationHelper` is pure maths — it holds no resource tokens. Every constant below is a field of that class.

| Token | Value | Constant |
| :--- | :--- | :--- |
| `ZeroEpsilonDegrees` | 0.05 | `ZERO_EPS_DEG` |
| `Mod90EpsilonDegrees` | 0.05 | `MOD_90_EPS_DEG` |
| `QuarterEpsilonDegrees` | 1.0 | `QUARTER_EPS_DEG` |
| `FullDegrees` | 360 | `FULL_DEG` |
| `HalfDegrees` | 180 | `HALF_DEG` |
| `QuarterDegrees` | 90 | `QUARTER_DEG` |

| Value | Formula |
| :--- | :--- |
| Multiple of 90° | `((f % 360 + 360) % 360) % 90` is `< 0.05` or `> 89.95` |
| Smallest equivalent angle | `(f % 360 + 360) % 360`, minus 360 when above 180 |
| Upright axis bounds | `[0, w, 0, h]` |
| Quarter-turn axis bounds | `[(w - h) / 2, (w + h) / 2, (h - w) / 2, (w + h) / 2]` |
| Rotated locate width | `h` for the two quarter turns, otherwise `w` |
| Rotated locate height | `w` for the two quarter turns, otherwise `h` |
| Fit-rect clamp (x) | Rect wider than the fit rect: align to its left. Otherwise push back on the left edge, then the right |
| Pivot | `clamp(size * 0.5, 0, size)`, or 0 when the size is not positive |

From `COUIRotatingPopupListWindow`:

| Token | Value | Constant |
| :--- | :--- | :--- |
| Container vertical padding | 2 | `MENU_CONTAINER_VERTICAL_PADDING_EDGES` |

Its measure cap is `resolveMainMenuListMeasureAtMostWidth(width, atMost, cap)` = `atMost > 0 ? (width <= 0 || width < atMost ? min(cap, atMost) : width) : width`.

## Not Ported

- **The rotating window container** — `COUIRotatingPopupListWindow` itself is an Android `PopupWindow` subclass. This port supplies the geometry and the two modifiers; placing them in a container is left to `OverlayListPopup` / `WindowListPopup`, which already exist.
- **`PhysicalHAlign` and the alignment inference** — `inferMainMenuPhysicalHAlign`, `forcedMenuLeft`, `explicitSideDisplayBounds` and `computeExplicitSideHorizontalClearancePx` all operate on `PopupMenuDomain`, a package-private structure that tracks the anchor view, the main menu bounds, the window barriers and the forced edges. None of that state is ported.
- **The expandable sub-menu refusal** — `PopupMenuRotation.allowsExpandableSubMenu` reproduces the flag, but there is no expandable sub-menu implementation to refuse here against.
- **The RecyclerView measurement of `COUIRotatingPopupListWindow`** — the native re-measures the menu list at the rotated size before the locate pass. The port exposes `rotatedLocateWidth` / `rotatedLocateHeight` for callers that need the same cap.
