# PopupMenuRotation

`PopupMenuRotation` 是 ColorOS 17 应用于弹窗菜单的旋转几何。在旋转后的屏幕上——横屏设备、折叠屏半开姿态、被旋转的窗口——原生弹窗列表可以把内容旋转四分之一圈来布局，此时定位过程必须基于**旋转后**的占位而不是菜单自身的宽高来推算。

它不是可见控件，而是控件背后的数学：四分之一圈角度判定、旋转后的轴对齐包围盒、适配矩形的夹取，以及把这两者变成已定位弹窗的两个修饰符。本组件移植自 `com.coui.appcompat.poplist.MenuRotationHelper` 与 `COUIRotatingPopupListWindow` 的几何部分。

<div style="position: relative; height: 250px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../../compose/index.html?id=popupMenuRotation" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## 导入

```kotlin
import io.github.suqi8.coui.kmp.basic.PopupMenuRotation
import io.github.suqi8.coui.kmp.basic.PopupMenuRotationDefaults
import io.github.suqi8.coui.kmp.basic.popupMenuRotation
import io.github.suqi8.coui.kmp.basic.popupMenuRotationPositionProvider
```

## 原生来源

`COUIContextMenu` 的**定位**部分在本库中已有覆盖。[ListPopup](../components/listpopup) 移植了同一个 `PreciseLongPressHelper`（即 `Modifier.preciseClickAnchor`）、同一个 `PopupMenuLocateHelper.setAnchor`（即 `ListPopupDefaults.dropdownPositionProvider`），以及同一套浮层与窗口容器（`OverlayListPopup` / `WindowListPopup`）。

缺失的是旋转：本库中没有任何代码旋转菜单内容，也没有任何已移植的类包含 `MenuRotationHelper` 的几何计算。本文件补上的正是这个缺口。

`MenuRotationHelper` 只接受“模 360 后是 90 的倍数”的旋转角（容差 0.05），并且该角还要与 0 相差不超过 0.05、或与 ±90 相差不超过 1.0。其余情况一律判定为 `None`，与原生 `classifyDegrees` 打日志后的行为一致。

## 基本用法

```kotlin
var showPopup by remember { mutableStateOf(false) }
val rotation = PopupMenuRotation.Degrees90

WindowListPopup(
    show = showPopup,
    // 定位过程看到的是交换后的占位：rotatedLocateWidth/Height。
    popupPositionProvider = popupMenuRotationPositionProvider(rotation),
    // 内容本身绕自身中心旋转。
    popupModifier = Modifier.popupMenuRotation(rotation),
    onDismissRequest = { showPopup = false }
) {
    ListPopupColumn {
        DropdownImpl(text = "Copy", optionSize = 3, isSelected = false, index = 0) { }
    }
}
```

## 几何 API

```kotlin
val rotation = PopupMenuRotationDefaults.classifyDegrees(90f) // PopupMenuRotation.Degrees90

// 定位过程所依据的占位。
val locateWidth = PopupMenuRotationDefaults.rotatedLocateWidth(rotation, 200, 120)  // 120
val locateHeight = PopupMenuRotationDefaults.rotatedLocateHeight(rotation, 200, 120) // 200

// 容器应放置的位置，以及它在屏幕上占据的范围。
val layout = PopupMenuRotationDefaults.computeMenuRotationLayout(
    anchorBounds = IntRect(100, 100, 300, 220),
    width = 200,
    height = 120,
    rotation = rotation,
    fitBounds = IntRect(0, 0, 1080, 2400)
)
```

`MenuRotationLayout` 包含容器原点、旋转轴心，以及最终的屏幕包围盒。

## 参数

### `popupMenuRotation`

| 参数 | 类型 | 说明 |
| :--- | :--- | :--- |
| `rotation` | `PopupMenuRotation` | 绘制内容时使用的旋转角 |

`PopupMenuRotation.None` 会原样返回修饰符链，因此正立的菜单没有任何额外开销。

### `popupMenuRotationPositionProvider`

| 参数 | 类型 | 说明 |
| :--- | :--- | :--- |
| `rotation` | `PopupMenuRotation` | 绘制内容时使用的旋转角 |
| `base` | `PopupPositionProvider` | 决定菜单去向的定位器；默认 `ListPopupDefaults.DropdownPositionProvider` |

### 旋转辅助函数

| 函数 | 对应原生 |
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

## 复原的数值

`MenuRotationHelper` 是纯数学，不含任何资源令牌。以下常量全部是该类的字段。

| 令牌 | 值 | 常量名 |
| :--- | :--- | :--- |
| `ZeroEpsilonDegrees` | 0.05 | `ZERO_EPS_DEG` |
| `Mod90EpsilonDegrees` | 0.05 | `MOD_90_EPS_DEG` |
| `QuarterEpsilonDegrees` | 1.0 | `QUARTER_EPS_DEG` |
| `FullDegrees` | 360 | `FULL_DEG` |
| `HalfDegrees` | 180 | `HALF_DEG` |
| `QuarterDegrees` | 90 | `QUARTER_DEG` |

| 数值 | 公式 |
| :--- | :--- |
| 是否为 90 的倍数 | `((f % 360 + 360) % 360) % 90` 小于 `0.05` 或大于 `89.95` |
| 最小等价角 | `(f % 360 + 360) % 360`，大于 180 时再减 360 |
| 正立轴对齐包围盒 | `[0, w, 0, h]` |
| 四分之一圈轴对齐包围盒 | `[(w - h) / 2, (w + h) / 2, (h - w) / 2, (w + h) / 2]` |
| 旋转后定位宽度 | 两个四分之一圈时为 `h`，否则为 `w` |
| 旋转后定位高度 | 两个四分之一圈时为 `w`，否则为 `h` |
| 适配夹取（x 轴） | 矩形比适配区更宽时左对齐；否则先向右推左边缘，再向左推右边缘 |
| 轴心 | `clamp(size * 0.5, 0, size)`，尺寸非正时为 0 |

来自 `COUIRotatingPopupListWindow`：

| 令牌 | 值 | 常量名 |
| :--- | :--- | :--- |
| 容器垂直内边距 | 2 | `MENU_CONTAINER_VERTICAL_PADDING_EDGES` |

其测量上限为 `resolveMainMenuListMeasureAtMostWidth(width, atMost, cap)` = `atMost > 0 ? (width <= 0 || width < atMost ? min(cap, atMost) : width) : width`。

## 未移植

- **旋转窗口容器**——`COUIRotatingPopupListWindow` 本身是 Android `PopupWindow` 的子类。本移植只提供几何与两个修饰符；如何放入容器交给已有的 `OverlayListPopup` / `WindowListPopup`。
- **`PhysicalHAlign` 与对齐推断**——`inferMainMenuPhysicalHAlign`、`forcedMenuLeft`、`explicitSideDisplayBounds`、`computeExplicitSideHorizontalClearancePx` 都依赖 `PopupMenuDomain`，那是一个包内私有结构，记录锚点视图、主菜单包围盒、窗口屏障和强制边缘。这些状态没有移植。
- **可展开子菜单的拒绝逻辑**——`PopupMenuRotation.allowsExpandableSubMenu` 重现了这个标志位，但这里没有可展开子菜单的实现可供拒绝。
- **`COUIRotatingPopupListWindow` 的 RecyclerView 测量**——原生会在定位前按旋转后的尺寸重新测量菜单列表。本移植暴露 `rotatedLocateWidth` / `rotatedLocateHeight`，供需要同样上限的调用方使用。
