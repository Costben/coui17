# ClickSelectMenu

`ClickSelectMenu` 让 ColorOS 弹窗列表出现在用户真正点击的位置。普通下拉菜单会以锚点行居中弹出；点击选择菜单则在手指下方弹出，并由一份*放置规格*决定菜单的哪一侧、哪一条边贴向该点。ColorOS 设置项里的内联菜单用的就是这套机制——菜单出现在你点的位置，而不是行的位置。

本组件移植自 `com.coui.appcompat.poplist.COUIClickSelectMenu`、`COUIIsolatedPopupListWindow`、`MainMenuPlacementSpec`，以及 `RotatingPopupMenuLocateHelper` 的放置规则，并复用本库已移植的弹窗列表外观。

<div style="position: relative; height: 250px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../../compose/index.html?id=clickSelectMenu" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## 导入

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

## 原生来源

`COUIClickSelectMenu` 本身很薄：它把菜单项存到一个 `COUIPopupListWindow` 上、设置锚点视图，并挂上一个 `PreciseClickHelper`，由后者的点击回调带着记录下的 DOWN 坐标调用 `setPopupShow(view, x, y)`。其中两部分本库已经移植，因此不再重复：

| 原生实现 | 已移植为 |
| :--- | :--- |
| `PreciseClickHelper`（记录 DOWN 坐标，TalkBack 时回退到视图中心） | `Modifier.preciseClickAnchor` |
| `PopupMenuLocateHelper.setAnchor`（把锚点边界塌缩为该点） | `ListPopupLayout` |
| `MenuRotationHelper` 的象限判定与适配矩形夹取 | `PopupMenuRotationDefaults` |
| 默认放置与上下文菜单放置 | `ListPopupDefaults.DropdownPositionProvider` / `ContextMenuPositionProvider` |

它们都没有携带**放置规格**。`ListPopupDefaults.dropdownPositionProvider` 永远以锚点居中再夹取；而 `RotatingPopupMenuLocateHelper` 会先解析 `MainMenuPlacementSpec`，再决定是强制某一侧、强制某条边，还是交给自适应判断。这一差异，加上 `COUIIsolatedPopupListWindow` 为其浮层窗口构造的合成锚点，就是本页记录的内容。

这两半在原生的两个类里。`RotatingPopupMenuLocateHelper` 只由 `COUIRotatingPopupListWindow` 构造，后者继承 `COUIPopupListWindow` 并自己持有规格——`setMainMenuPlacement` / `resetMainMenuPlacement`，且 `reuseMainMenu` 会先解析出适配后的规格再定位。而 `COUIClickSelectMenu` 驱动的是普通 `COUIPopupListWindow`，其 `PopupMenuLocateHelper` 完全没有规格的概念。这里把两者放在一起，是因为在 Compose 里由单个 `PopupPositionProvider` 决定弹窗开在哪里，侧向／对齐规则与点击点锚点必然在此汇合。

## 基本用法

把锚点接到点击位置，再把同一个状态交给弹窗：

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

`preciseClickAnchor` 以窗口坐标记录触点，且不消费任何事件，因此行自身的 `clickable` 仍能收到完整手势。随后 `WindowListPopup` 会像原生 `PopupMenuLocateHelper.setAnchor` 那样，先把锚点边界塌缩到该点，再询问位置提供者菜单该放在哪里。

## 放置规格

`ClickSelectMenuPlacement` 对应 `MainMenuPlacementSpec`，其两半是由 `isValidSide` / `isValidAlignment` 校验的 int 常量（非法值会打日志并回退到 `DEFAULT = ofSideAndAlignment(0, 0)`）。此处两半都是枚举，因此该回退路径不会被触发。

`ClickSelectMenuSide`（`MainMenuPlacementSpec.SIDE_*`）：

| 侧 | 值 | 行为 |
| :--- | :--- | :--- |
| `Auto` | 0 | 由自适应判断决定：下方放得下就放下方，否则上方，再否则可用区域顶部 |
| `Above` | 1 | 固定在上方，并夹取进可用区域 |
| `Below` | 2 | 固定在下方，并夹取进可用区域 |
| `Start` | 3 | 放在锚点旁（交叉轴方向）的起始侧 |
| `End` | 4 | 放在锚点旁（交叉轴方向）的结束侧 |

`ClickSelectMenuAlignment`（`MainMenuPlacementSpec.ALIGN_*`）：

| 对齐 | 值 | 行为 |
| :--- | :--- | :--- |
| `Default` | 0 | 以锚点居中，再夹取进可用区域 |
| `Start` | 1 | 菜单起始边强制对齐锚点起始边 |
| `End` | 2 | 菜单结束边强制对齐锚点结束边 |

`Start` / `End` 是逻辑值：`resolvePopupSide` 与 `physicalAlignment` 会在 RTL 下互换两者，因为 `MenuRotationHelper.physicalHAlignForCrossAxis` 工作在物理坐标，而规格是按逻辑书写的。

## 复原的规则

`ClickSelectMenuDefaults` 把放置规则暴露为普通函数，调用方无需打开弹窗即可推算结果。每一条都标注了对应的原生方法。

| 函数 | 原生来源 |
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

有两个细节容易写错，这里如实保留：

- **X 方向读取裸锚点，Y 方向读取加上外扩的锚点。** `setupMainMenuLocateXRule` 读的是 `PopupMenuDomain.mAnchor`；而 `setupMainMenuLocateYRule`、`computeMainMenuMeasureCaps` 与 `explicitSideDisplayBounds` 读的是 `getAnchorRealRect`，即 `mAnchor` 加上 `mAnchorOutsets` 外扩后的矩形。
- **点状锚点意味着没有垂直间距。** `PopupMenuRuleExecutor.executeConfigRules` 只在锚点未携带点击偏移时选用默认锚点规则（外扩为 `0, 8dp, 0, 8dp`），携带偏移时改用上下文锚点规则（外扩为空）。而 `setAnchor` 恰好在后一种情况下把边界塌缩，因此零面积锚点总能等价于空外扩。

## 隔离弹窗

`COUIIsolatedPopupListWindow` 通过 `WindowManager` 显示弹窗，而不是把它挂到真实视图上。`buildDummyAnchor` 会创建一个与默认屏幕同尺寸的 `DummyRootView`，在其中按目标坐标放入一个 1x1 的 `DummyAnchorView`，且两者都不会被挂到任何窗口上：

- `DummyAnchorView` 覆写 `getGlobalVisibleRect`，直接报告自己的布局矩形，于是在没有视图树的情况下锚点就是那个触点。
- `DummyAnchorView` 同时以 `getType() == 1` 实现 `PopupMenuConfigRule`，因此 `executeConfigRules` 会走 `view instanceof PopupMenuConfigRule` 分支，采用该哑元自身的 1x1 `getDisplayFrame` 与其空 `getOutsets` 作为锚点规则。
- 锚点的根视图未被挂载，`completeAnchorSetup` 会检测到并置 `mIgnoreBarrier`，于是不施加任何窗口屏障。

`IsolatedPopupAnchor` 建模了这套契约——`anchorBounds` 是那个 1x1 矩形，`rootBounds` 是整块屏幕——`ClickSelectMenuDefaults.isolatedAnchor` 由坐标与屏幕尺寸构造它。窗口本身以 `type = 2038`、`format = 1`、`gravity = 51` 添加。

## 复原的令牌

以下数值全部取自 ColorOS 17 的 `Settings.apk` 资源转储及其 dex（`classes7.dex`），没有一个是臆造的。

| 令牌 | 值 | 来源 |
| :--- | :--- | :--- |
| `DUMMY_ANCHOR_SIZE` | 1px | `COUIIsolatedPopupListWindow.DummyAnchorView`（`setRight(1)` / `setBottom(1)`） |
| `CLICK_SELECT_DELAY_MILLIS` | 100 | `integer/support_menu_click_select_time` |
| `CLICK_SELECT_DELAY_MILLIS_HEIGHT_840DP` | 240 | `integer/support_menu_click_select_time`（`h840dp`） |
| `VerticalGapToAnchor` | 8dp | `dimen/coui_popup_list_window_default_vertical_gap_to_anchor` |
| `ISOLATED_WINDOW_TYPE` | 2038 | `COUIIsolatedPopupListWindow.show` |
| `ISOLATED_WINDOW_FORMAT` | 1 | `COUIIsolatedPopupListWindow.show` |
| `ISOLATED_WINDOW_GRAVITY` | 51 | `COUIIsolatedPopupListWindow.show` |
| 隔离窗口退出 | alpha 1 → 0，250ms，`cubic-bezier(0.33, 0, 0.67, 1)` | `style/Animation.COUI.IsolatedPopupListWindow` → `anim/coui_isolated_popup_list_exit` + `anim/coui_ease` |
| 隔离窗口进入 | 空动画 | `style/Animation.COUI.IsolatedPopupListWindow` → `anim/coui_popup_list_enter`，内容为空 `<set>` |
| `SIDE_AUTO` / `SIDE_ABOVE` / `SIDE_BELOW` / `SIDE_START` / `SIDE_END` | 0 / 1 / 2 / 3 / 4 | `MainMenuPlacementSpec` |
| `ALIGN_DEFAULT` / `ALIGN_START` / `ALIGN_END` | 0 / 1 / 2 | `MainMenuPlacementSpec` |
| `MainMenuPlacementSpec.DEFAULT` | `ofSideAndAlignment(0, 0)` | `MainMenuPlacementSpec.DEFAULT` |
| 默认水平边距 | 16dp | `ListPopupDefaults.HorizontalBarrier`（`mWindowBarriers` 里的响应式栅格边距） |

弹窗外观的颜色、圆角与缓动没有在此重复：外观由 `ListPopupContent` 承担，它已经带有复原出的 `coui_popup_round_frame_layout_corner_radius`、模糊混色对与阴影。

## 未移植

- **`WindowManager` 浮层本身。** `COUIIsolatedPopupListWindow.show` 会把内容视图以 `type = 2038` 添加到 `WindowManager`；Compose Multiplatform 没有可移植的等价物，因此这里用 `IsolatedPopupAnchor` 建模了隔离窗口的*锚点契约*、并把窗口参数暴露为常量，而宿主仍使用既有的 `WindowListPopup` / `OverlayListPopup`。
- **`COUIClickSelectMenu` 的输入法处理。** 原生会先隐藏软键盘，若键盘此前可见则等待 `support_menu_click_select_time` 再显示菜单，以保证布局已稳定。本库没有可挂载的共享输入法控制器，因此该延迟只作为常量暴露，由调用方决定是否应用。
- **作为控件形态的 `COUIClickSelectMenu.registerForClickSelectItems`。** 原生是在既有 `View` 上做变更（设为可点击、可长按，写入菜单项列表，锚点变化时替换 helper）。Compose 的等价做法是在锚点上组合 `preciseClickAnchor` 并把共享状态交给弹窗，即上面示例的写法。
- **`computeMainMenuMeasureCaps` 在正交场景下的宽度上限**按原生字节码原样保留（`Math.min(availableHeight, availableHeight)`），没有「修正」为它从未计算过的值。
- **`transformedAnchorBounds` 尚无宿主接线。** 规则已复原并暴露，但本库没有把调用方算出的变换后锚点边界送入 `PopupPositionProvider` 的通道：弹窗布局的锚点边界来自 `onGloballyPositioned`。面对旋转锚点的调用方可以用它算出边界，但还无法注入。
- **`getMainMenuEnterPivotX` / `inferMainMenuPhysicalHAlign`。** 它们读取的是本移植在 `ClickSelectMenuHorizontalResult.forcedEdge` 中返回的强制水平边。`ListPopupLayout` 已改为按锚点中心推导缩放轴心，而非按强制边，因此强制边的轴心未复现。
- **`COUIRotatingPopupListWindow` 的分发。** 该窗口会在默认、强制侧向、正交与旧版布局之间选择（`needsExtendedMainMenuLayout`、`useSidePlacement`、`prepareShowMainMenuExplicitSide` / `prepareOrthogonalMainMenu`），并把定位尺寸换成 `getRotatedLocateWidth` / `getRotatedLocateHeight`。本移植暴露了各条规则与尺寸互换（`PopupMenuRotationDefaults.rotatedLocateWidth`），但分发仍留给调用方：旋转菜单请把 `popupMenuRotationPositionProvider` 套在 `clickSelectMenuPositionProvider` 外面。
