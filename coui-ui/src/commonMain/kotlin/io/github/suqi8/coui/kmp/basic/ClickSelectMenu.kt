// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Where a ColorOS click-select menu is allowed to be placed, mirroring the `Side` int constants of
 * `com/coui/appcompat/poplist/MainMenuPlacementSpec` (`SIDE_AUTO`, `SIDE_ABOVE`, `SIDE_BELOW`,
 * `SIDE_START`, `SIDE_END`).
 *
 * [Above] and [Below] force the vertical side, `Start` and `End` force the menu beside the anchor
 * instead of above/below it, and [Auto] lets the fit test in
 * [ClickSelectMenuDefaults.locateMainMenuY] pick the side. `MainMenuPlacementSpec.isValidSide`
 * accepts exactly these five values; the enum makes the native's int validation structural.
 *
 * @property value The native `MainMenuPlacementSpec.SIDE_*` constant.
 */
enum class ClickSelectMenuSide(val value: Int) {
    /** `SIDE_AUTO` (0): the side is decided by which of the two fits. */
    Auto(0),

    /** `SIDE_ABOVE` (1): the menu is placed above the anchor and clamped into the available bounds. */
    Above(1),

    /** `SIDE_BELOW` (2): the menu is placed below the anchor and clamped into the available bounds. */
    Below(2),

    /** `SIDE_START` (3): the menu is placed before the anchor, on the cross axis. */
    Start(3),

    /** `SIDE_END` (4): the menu is placed after the anchor, on the cross axis. */
    End(4),
    ;

    /**
     * `MainMenuPlacementSpec.isVerticalForced`: [Above] or [Below], i.e. the side that
     * [ClickSelectMenuDefaults.locateMainMenuY] honours by clamping instead of by fitting.
     */
    val isVerticalForced: Boolean
        get() = this == Above || this == Below

    /**
     * `RotatingPopupMenuLocateHelper.isExplicitHorizontalSide`: `Start` or `End`, i.e. the side that
     * bypasses the locate rules entirely and is resolved by
     * [ClickSelectMenuDefaults.explicitSideBounds].
     */
    val isHorizontalForced: Boolean
        get() = this == Start || this == End
}

/**
 * How the menu is aligned against the cross axis, mirroring the `Alignment` int constants of
 * `com/coui/appcompat/poplist/MainMenuPlacementSpec` (`ALIGN_DEFAULT`, `ALIGN_START`, `ALIGN_END`).
 *
 * @property value The native `MainMenuPlacementSpec.ALIGN_*` constant.
 */
enum class ClickSelectMenuAlignment(val value: Int) {
    /** `ALIGN_DEFAULT` (0): the menu is centred on the anchor and clamped. */
    Default(0),

    /** `ALIGN_START` (1): the menu's start edge is forced onto the anchor's start edge. */
    Start(1),

    /** `ALIGN_END` (2): the menu's end edge is forced onto the anchor's end edge. */
    End(2),
    ;

    /**
     * `MainMenuPlacementSpec.isCrossAxisStartOrEnd`: whether the alignment forces an edge, which is
     * what makes [ClickSelectMenuDefaults.locateMainMenuAnchorX] and
     * [ClickSelectMenuDefaults.mainMenuMeasureCaps] measure the cross-axis clearance.
     */
    val isCrossAxisStartOrEnd: Boolean
        get() = this != Default
}

/**
 * The physical edge an alignment resolves to for the current layout direction, mirroring
 * `MenuRotationHelper$PhysicalHAlign` (`LEFT`, `CENTER`, `RIGHT`).
 *
 * `MenuRotationHelper.physicalHAlignForCrossAxis` flips `Start` and `End` under RTL, because the
 * native works in physical coordinates while the placement spec is written logically.
 */
enum class ClickSelectMenuPhysicalAlignment {
    /** `PhysicalHAlign.LEFT`. */
    Left,

    /** `PhysicalHAlign.CENTER`. */
    Center,

    /** `PhysicalHAlign.RIGHT`. */
    Right,
}

/**
 * A `MainMenuPlacementSpec`: the side and the cross-axis alignment a main menu is placed with.
 *
 * `MainMenuPlacementSpec` is an immutable int pair built through `ofSideAndAlignment`, which rejects
 * an unknown side or alignment and logs a warning before falling back to `DEFAULT`
 * (`ofSideAndAlignment(0, 0)`). Both halves are enums here, so the rejection cannot be reached and
 * [ClickSelectMenuDefaults.placementOf] is the direct equivalent of the native factory.
 *
 * @property side The side the menu is placed on; [ClickSelectMenuSide.Auto] by default.
 * @property alignment The cross-axis alignment; [ClickSelectMenuAlignment.Default] by default.
 */
@Immutable
data class ClickSelectMenuPlacement(
    val side: ClickSelectMenuSide = ClickSelectMenuSide.Auto,
    val alignment: ClickSelectMenuAlignment = ClickSelectMenuAlignment.Default,
) {
    /** `MainMenuPlacementSpec.isDefault`: neither half is forced. */
    val isDefault: Boolean
        get() = side == ClickSelectMenuSide.Auto && alignment == ClickSelectMenuAlignment.Default

    /** `MainMenuPlacementSpec.isSideForced`: the menu sits beside the anchor. */
    val isSideForced: Boolean
        get() = side.isHorizontalForced

    /** `MainMenuPlacementSpec.isVerticalForced`: the menu is pinned above or below the anchor. */
    val isVerticalForced: Boolean
        get() = side.isVerticalForced

    /** `MainMenuPlacementSpec.isCrossAxisStartOrEnd`: an edge of the menu is forced. */
    val isCrossAxisStartOrEnd: Boolean
        get() = alignment.isCrossAxisStartOrEnd
}

/**
 * The horizontal result of [ClickSelectMenuDefaults.locateMainMenuAnchorX], mirroring
 * `RotatingPopupMenuLocateHelper$LocateMainMenuAnchorXResult`.
 *
 * @property x The left edge the menu is placed at.
 * @property forcedEdge The edge that was forced, or [ClickSelectMenuAlignment.Default] when the menu
 *   fell back to being centred on the anchor. Stored by the native in
 *   `PopupMenuDomain.mForcedHorizontalEdge` and read back by `PopupMenuDomain.getMainMenuEnterPivotX`
 *   and `MenuRotationHelper.inferMainMenuPhysicalHAlign`.
 */
@Immutable
data class ClickSelectMenuHorizontalResult(
    val x: Int,
    val forcedEdge: ClickSelectMenuAlignment,
)

/**
 * The synthetic anchor `COUIIsolatedPopupListWindow.buildDummyAnchor` builds for an isolated popup.
 *
 * The native never attaches these views to a window: `DummyRootView` is a `ViewGroup` sized to the
 * default display whose `getGlobalVisibleRect` override reports its own laid-out box, and
 * `DummyAnchorView` is a 1x1 child placed at the requested point whose `getGlobalVisibleRect`
 * override does the same. Together they make `PopupMenuLocateHelper.prepareWindowAndAnchor` see a
 * window that is the whole display and an anchor that is the touch point, without a real view tree.
 * `completeAnchorSetup` then notices that the anchor's root is not attached to a window and sets
 * `mIgnoreBarrier`, so no window barrier is applied either.
 *
 * `DummyAnchorView` also implements `PopupMenuConfigRule` with `getType() == 1`, which is what makes
 * `PopupMenuLocateHelper.executeConfigRules` take the `view instanceof PopupMenuConfigRule` branch
 * and use the dummy's own 1x1 `getDisplayFrame` plus its empty `getOutsets` as the anchor rule.
 *
 * @property pointX The x the dummy anchor is placed at, in display coordinates.
 * @property pointY The y the dummy anchor is placed at, in display coordinates.
 * @property rootWidth The width of the dummy root, i.e. the default display width.
 * @property rootHeight The height of the dummy root, i.e. the default display height.
 */
@Immutable
data class IsolatedPopupAnchor(
    val pointX: Int,
    val pointY: Int,
    val rootWidth: Int,
    val rootHeight: Int,
) {
    /** `DummyAnchorView.getDisplayFrame()` / `getGlobalVisibleRect`: a 1x1 box at the point. */
    val anchorBounds: IntRect
        get() = IntRect(
            left = pointX,
            top = pointY,
            right = pointX + ClickSelectMenuDefaults.DUMMY_ANCHOR_SIZE,
            bottom = pointY + ClickSelectMenuDefaults.DUMMY_ANCHOR_SIZE,
        )

    /** The display-sized root the dummy anchor is a child of (`DummyRootView`). */
    val rootBounds: IntRect
        get() = IntRect(left = 0, top = 0, right = rootWidth, bottom = rootHeight)
}

/**
 * The placement maths of `com/coui/appcompat/poplist/RotatingPopupMenuLocateHelper`, the anchor
 * geometry of `COUIIsolatedPopupListWindow`, and the trigger timing of `COUIClickSelectMenu`.
 *
 * `COUIClickSelectMenu` itself is thin: `registerForClickSelectItems` makes the anchor clickable,
 * stores the item list on a `COUIPopupListWindow` and wires a `PreciseClickHelper` whose click
 * callback collapses the anchor to the recorded DOWN point before showing the menu. Both halves of
 * that are already in this library — `Modifier.preciseClickAnchor` ports the helper, and
 * `ListPopupLayout` collapses the point into the anchor bounds — so what is ported here is the part
 * that was still missing: the `MainMenuPlacementSpec` side/alignment rules that
 * `RotatingPopupMenuLocateHelper` applies to the located rectangle, and the isolated window's dummy
 * anchor contract.
 *
 * The locate helpers themselves are not re-ported. `PopupMenuLocateHelper.setAnchor`'s point
 * collapse is `ListPopupLayout`, `PreciseClickHelper` is `Modifier.preciseClickAnchor`,
 * `MenuRotationHelper`'s quarter classification and clamp are [PopupMenuRotationDefaults], and the
 * dropdown/context placement is [ListPopupDefaults.DropdownPositionProvider] /
 * [ListPopupDefaults.ContextMenuPositionProvider]. What those do **not** carry is any notion of a
 * placement spec: [ListPopupDefaults.dropdownPositionProvider] always centres the menu on the anchor,
 * while the native forces an edge or a side whenever `MainMenuPlacementSpec` asks for one.
 *
 * The two halves are separate classes natively. `RotatingPopupMenuLocateHelper` is constructed only by
 * `COUIRotatingPopupListWindow`, which extends `COUIPopupListWindow` and holds the spec itself
 * (`setMainMenuPlacement` / `resetMainMenuPlacement`; it resolves a fitting spec on
 * `reuseMainMenu`). `COUIClickSelectMenu` drives a plain `COUIPopupListWindow`, whose
 * `PopupMenuLocateHelper` has no spec at all. They are combined here because a Compose
 * [PopupPositionProvider] is the one place that decides where a popup opens, so the side/alignment
 * rules and the click-point anchor meet in the same call.
 *
 * All three of [ClickSelectMenuDefaults.locateMainMenuAnchorX], [ClickSelectMenuDefaults.locateMainMenuY]
 * and [ClickSelectMenuDefaults.explicitSideBounds] take the *available* bounds, which stand for the
 * native `PopupMenuDomain.getAvailableRect`: `mWindow` (the application window) shrunk by
 * `mWindowBarriers` (the status-bar and navigation-bar margins accumulated by the barrier rules).
 */
object ClickSelectMenuDefaults {

    /** `COUIIsolatedPopupListWindow.DummyAnchorView` is laid out with `setRight(1)`/`setBottom(1)`. */
    const val DUMMY_ANCHOR_SIZE = 1

    /**
     * `integer/support_menu_click_select_time` in the default configuration (100): the delay
     * `COUIClickSelectMenu` waits after the soft keyboard was visible before it shows the menu, so the
     * IME has begun hiding and the recorded click point is still valid.
     */
    const val CLICK_SELECT_DELAY_MILLIS = 100

    /**
     * `integer/support_menu_click_select_time` for the `h840dp` configuration (240): tall screens
     * carry a taller keyboard and therefore wait longer.
     */
    const val CLICK_SELECT_DELAY_MILLIS_HEIGHT_840DP = 240

    /**
     * The `WindowManager.LayoutParams.type` `COUIIsolatedPopupListWindow.show` uses (2038, the
     * application-overlay type), with `format = 1` (opaque) and `gravity = 51` (top or left).
     */
    const val ISOLATED_WINDOW_TYPE = 2038

    /** `WindowManager.LayoutParams.format` of the isolated popup window (1, opaque). */
    const val ISOLATED_WINDOW_FORMAT = 1

    /** `WindowManager.LayoutParams.gravity` of the isolated popup window (51, top or left). */
    const val ISOLATED_WINDOW_GRAVITY = 51

    /**
     * The vertical gap the default anchor rule keeps between the anchor and the menu, from
     * `dimen/coui_popup_list_window_default_vertical_gap_to_anchor` (8dp), which
     * `PopupMenuLocateHelper.DEFAULT_ANCHOR_OUTSETS` is built from.
     *
     * `PopupMenuRuleExecutor.executeConfigRules` picks that rule only when the anchor was set without
     * a click offset; when a click point was supplied it uses
     * `PopupMenuLocateHelper.setupContextAnchorConfigRule`, whose outsets are empty. Because
     * `setAnchor` collapses the anchor bounds exactly when a click offset was supplied, the two are
     * equivalent: a zero-area anchor means no gap.
     */
    val VerticalGapToAnchor = 8.dp

    /**
     * `style/Animation.COUI.IsolatedPopupListWindow`, whose exit animation is
     * `res/anim/coui_isolated_popup_list_exit.xml`: alpha 1 to 0 over 250ms with
     * `res/anim/coui_ease.xml`, a `pathInterpolator(0.33, 0, 0.67, 1)`.
     *
     * The same style pairs it with `res/anim/coui_popup_list_enter.xml` for the enter, which is an
     * empty `<set>`: the isolated window is revealed by the menu's own motion and only fades out.
     */
    val IsolatedExitAnimationSpec = tween<Float>(
        durationMillis = 250,
        easing = CubicBezierEasing(0.33f, 0f, 0.67f, 1f),
    )

    /** `MainMenuPlacementSpec.DEFAULT`, i.e. `ofSideAndAlignment(0, 0)`. */
    val DefaultPlacement = ClickSelectMenuPlacement()

    /** `MainMenuPlacementSpec.ofSideAndAlignment`, with the native's int validation done by the enums. */
    fun placementOf(
        side: ClickSelectMenuSide = ClickSelectMenuSide.Auto,
        alignment: ClickSelectMenuAlignment = ClickSelectMenuAlignment.Default,
    ): ClickSelectMenuPlacement = ClickSelectMenuPlacement(side = side, alignment = alignment)

    /**
     * `RotatingPopupMenuLocateHelper.setAnchorBoundsInWindow`: the anchor's layout-sized box
     * re-centred on its transformed centre.
     *
     * The native maps the anchor's local centre through `View.transformMatrixToGlobal` and back
     * through the inverse of the window's matrix, then rebuilds a box of the anchor's *layout* size
     * around that point. That is what keeps a rotated or scaled anchor locating correctly, unlike
     * `PopupMenuLocateHelper.getGlobalVisibleRectWithoutTransformation`, which only backs out the
     * scale and leaves the box axis-aligned at the layout position.
     *
     * In Compose the transformed centre is `LayoutCoordinates.localToWindow(size / 2)`; note that
     * `LayoutCoordinates.boundsInWindow` reports the axis-aligned box at the layout position and does
     * not re-centre it on the transformed centre, so a rotated anchor has to be routed through here.
     *
     * @param centerInWindow The anchor's centre after its transforms, in window coordinates.
     * @param size The anchor's layout size.
     */
    fun transformedAnchorBounds(centerInWindow: Offset, size: IntSize): IntRect {
        val halfWidth = size.width / 2f
        val halfHeight = size.height / 2f
        val left = (centerInWindow.x - halfWidth).roundToInt()
        val top = (centerInWindow.y - halfHeight).roundToInt()
        return IntRect(left = left, top = top, right = left + size.width, bottom = top + size.height)
    }

    /**
     * `COUIIsolatedPopupListWindow.buildDummyAnchor`: the synthetic anchor for an isolated popup,
     * built from the point the menu should open at and the display size the dummy root fakes.
     *
     * @param pointX The x of the point, in display coordinates.
     * @param pointY The y of the point, in display coordinates.
     * @param displaySize The default display size, from `Display.getSize`.
     */
    fun isolatedAnchor(pointX: Int, pointY: Int, displaySize: IntSize): IsolatedPopupAnchor = IsolatedPopupAnchor(
        pointX = pointX,
        pointY = pointY,
        rootWidth = displaySize.width,
        rootHeight = displaySize.height,
    )

    /**
     * `RotatingPopupMenuLocateHelper.resolvePopupSide(MainMenuPlacementSpec, boolean)`: the physical
     * side a forced horizontal placement resolves to. [ClickSelectMenuSide.Start] and
     * [ClickSelectMenuSide.End] swap under RTL, and every other side resolves to
     * [ClickSelectMenuSide.Auto].
     *
     * @param placement The placement spec.
     * @param isRtl Whether the anchor's layout direction is RTL.
     */
    fun resolvePopupSide(placement: ClickSelectMenuPlacement, isRtl: Boolean): ClickSelectMenuSide {
        if (!placement.isSideForced) return ClickSelectMenuSide.Auto
        return if (placement.side == ClickSelectMenuSide.Start) {
            if (isRtl) ClickSelectMenuSide.End else ClickSelectMenuSide.Start
        } else {
            if (isRtl) ClickSelectMenuSide.Start else ClickSelectMenuSide.End
        }
    }

    /**
     * `RotatingPopupMenuLocateHelper.shouldApplyCenterAlignY`: whether the centred-Y rule is applied
     * on top of the located Y. A forced vertical or horizontal side places the menu itself, so the
     * centre rule is skipped.
     */
    fun shouldApplyCenterAlignY(placement: ClickSelectMenuPlacement): Boolean = !placement.isVerticalForced && !placement.isSideForced

    /**
     * `MenuRotationHelper.physicalHAlignForCrossAxis`: the physical edge an alignment resolves to.
     * [ClickSelectMenuAlignment.Start] is [ClickSelectMenuPhysicalAlignment.Left] in LTR and
     * [ClickSelectMenuPhysicalAlignment.Right] in RTL, [ClickSelectMenuAlignment.End] is the mirror,
     * and [ClickSelectMenuAlignment.Default] is [ClickSelectMenuPhysicalAlignment.Center].
     */
    fun physicalAlignment(
        alignment: ClickSelectMenuAlignment,
        isRtl: Boolean,
    ): ClickSelectMenuPhysicalAlignment = when (alignment) {
        ClickSelectMenuAlignment.Start ->
            if (isRtl) ClickSelectMenuPhysicalAlignment.Right else ClickSelectMenuPhysicalAlignment.Left

        ClickSelectMenuAlignment.End ->
            if (isRtl) ClickSelectMenuPhysicalAlignment.Left else ClickSelectMenuPhysicalAlignment.Right

        ClickSelectMenuAlignment.Default -> ClickSelectMenuPhysicalAlignment.Center
    }

    /**
     * `MenuRotationHelper.forcedMenuLeft`: the menu's left edge when an alignment forces an edge.
     *
     * `ALIGN_START` returns the anchor's left in LTR and the anchor's right minus the menu width in
     * RTL; `ALIGN_END` is the mirror; any other alignment falls back to the anchor's centre minus half
     * the menu width, which is the native's warning path.
     */
    fun forcedMenuLeft(
        anchorBounds: IntRect,
        menuWidth: Int,
        alignment: ClickSelectMenuAlignment,
        isRtl: Boolean,
    ): Int = when (alignment) {
        ClickSelectMenuAlignment.Start ->
            if (isRtl) anchorBounds.right - menuWidth else anchorBounds.left

        ClickSelectMenuAlignment.End ->
            if (isRtl) anchorBounds.left else anchorBounds.right - menuWidth

        ClickSelectMenuAlignment.Default ->
            (anchorBounds.left + anchorBounds.right) / 2 - menuWidth / 2
    }

    /**
     * `RotatingPopupMenuLocateHelper.locateMainMenuXDefault`: the menu centred on the anchor, then
     * clamped into the available bounds, then — if the menu is wider than the available bounds — centred
     * on the available bounds instead.
     */
    fun locateMainMenuXDefault(
        anchorBounds: IntRect,
        menuWidth: Int,
        availableBounds: IntRect,
    ): Int {
        val halfWidth = menuWidth / 2
        var x = (anchorBounds.left + anchorBounds.right) / 2 - halfWidth
        if (x < availableBounds.left) x = availableBounds.left
        if (x + menuWidth > availableBounds.right) x = availableBounds.right - menuWidth
        if (x < availableBounds.left) x = (availableBounds.left + availableBounds.right) / 2 - halfWidth
        return x
    }

    /**
     * `RotatingPopupMenuLocateHelper.computeLocateMainMenuAnchorX`: the menu's left edge for a
     * placement spec.
     *
     * A centred pass (`centerAlign`, the native `PopupMenuDomain.mMainMenuCenterAlign`), an
     * [ClickSelectMenuAlignment.Default] alignment, or a forced horizontal side all take
     * [locateMainMenuXDefault]. Otherwise the edge is forced by [forcedMenuLeft], clamped into the
     * available bounds, and if that still leaves the menu off the start edge it is centred on the
     * available bounds and the forced edge is dropped.
     *
     * @param anchorBounds The anchor bounds, *without* the anchor outsets — the native reads
     *   `PopupMenuDomain.mAnchor` here, not `getAnchorRealRect`.
     * @param placement The placement spec.
     * @param menuWidth The measured menu width.
     * @param availableBounds The available bounds.
     * @param isRtl Whether the anchor's layout direction is RTL.
     * @param centerAlign Whether the centre rule is in effect.
     */
    fun locateMainMenuAnchorX(
        anchorBounds: IntRect,
        placement: ClickSelectMenuPlacement,
        menuWidth: Int,
        availableBounds: IntRect,
        isRtl: Boolean,
        centerAlign: Boolean = false,
    ): ClickSelectMenuHorizontalResult {
        val alignment = placement.alignment
        if (centerAlign || alignment == ClickSelectMenuAlignment.Default || placement.isSideForced) {
            return ClickSelectMenuHorizontalResult(
                x = locateMainMenuXDefault(anchorBounds, menuWidth, availableBounds),
                forcedEdge = ClickSelectMenuAlignment.Default,
            )
        }
        var x = forcedMenuLeft(anchorBounds, menuWidth, alignment, isRtl)
        var forcedEdge = alignment
        if (x < availableBounds.left) x = availableBounds.left
        if (x + menuWidth > availableBounds.right) x = availableBounds.right - menuWidth
        if (x < availableBounds.left) {
            x = (availableBounds.left + availableBounds.right) / 2 - menuWidth / 2
            forcedEdge = ClickSelectMenuAlignment.Default
        }
        return ClickSelectMenuHorizontalResult(x = x, forcedEdge = forcedEdge)
    }

    /**
     * `RotatingPopupMenuLocateHelper.computeLocateMainMenuY`: the menu's top edge for a placement
     * spec.
     *
     * [ClickSelectMenuSide.Below] and [ClickSelectMenuSide.Above] clamp the menu into the available
     * bounds on the requested side. Any other side runs the native's fit test: below the anchor if the
     * anchor's bottom (or top, when [locateFromAboveAnchorToBelow]) leaves room, otherwise the other
     * side, otherwise the top of the available bounds.
     *
     * @param anchorBounds The anchor bounds expanded by the anchor outsets, i.e. the native
     *   `PopupMenuDomain.getAnchorRealRect` — the 8dp `VerticalGapToAnchor` lives here.
     * @param placement The placement spec.
     * @param locateFromAboveAnchorToBelow The `mLocateFromAboveAnchorToBelow` flag; the click-select
     *   path passes `false`, i.e. below first. `RotatingPopupMenuLocateHelper.prepareShowMainMenu`
     *   overrides it to `true` for [ClickSelectMenuSide.Above] and `false` for
     *   [ClickSelectMenuSide.Below].
     * @param menuHeight The measured menu height.
     * @param availableBounds The available bounds.
     */
    fun locateMainMenuY(
        anchorBounds: IntRect,
        placement: ClickSelectMenuPlacement,
        locateFromAboveAnchorToBelow: Boolean,
        menuHeight: Int,
        availableBounds: IntRect,
    ): Int {
        val below = maxOf(anchorBounds.bottom, availableBounds.top)
        val above = minOf(anchorBounds.top, availableBounds.bottom)
        return when (placement.side) {
            ClickSelectMenuSide.Below ->
                maxOf(availableBounds.top, minOf(below, availableBounds.bottom - menuHeight))

            ClickSelectMenuSide.Above ->
                maxOf(availableBounds.top, minOf(above - menuHeight, availableBounds.bottom - menuHeight))

            else -> {
                val fitsAbove = above - availableBounds.top >= menuHeight
                val fitsBelow = availableBounds.bottom - below >= menuHeight
                when {
                    locateFromAboveAnchorToBelow && fitsAbove -> above - menuHeight
                    locateFromAboveAnchorToBelow -> if (fitsBelow) below else availableBounds.top
                    fitsBelow -> below
                    fitsAbove -> above - menuHeight
                    else -> availableBounds.top
                }
            }
        }
    }

    /**
     * `MenuRotationHelper.computeExplicitSideHorizontalClearancePx`: how much room the anchor leaves on
     * the requested side, measured against the available bounds. Any other side returns 0, which is the
     * native's warning path.
     */
    fun horizontalClearance(
        anchorBounds: IntRect,
        availableBounds: IntRect,
        side: ClickSelectMenuSide,
    ): Int = when (side) {
        ClickSelectMenuSide.Start -> anchorBounds.left - availableBounds.left
        ClickSelectMenuSide.End -> availableBounds.right - anchorBounds.right
        else -> 0
    }

    /**
     * `MenuRotationHelper.explicitSideDisplayBounds`: the menu's rectangle when a horizontal side is
     * forced. [ClickSelectMenuSide.Start] puts the menu's right edge on the anchor's left edge and
     * [ClickSelectMenuSide.End] puts its left edge on the anchor's right edge; a side that is not
     * forced falls back to [ClickSelectMenuSide.Start], which is the native's warning path.
     *
     * The cross-axis alignment is physical here — [ClickSelectMenuAlignment.Start] aligns the menu's
     * top with the anchor's top, [ClickSelectMenuAlignment.End] aligns its bottom with the anchor's
     * bottom, and [ClickSelectMenuAlignment.Default] centres it on the anchor.
     *
     * @param anchorBounds The anchor bounds expanded by the anchor outsets (`getAnchorRealRect`).
     * @param side The forced side.
     * @param alignment The cross-axis alignment.
     * @param width The menu width.
     * @param height The menu height.
     * @param offsetX The global offset the native adds afterwards (`mGlobalOffsetX`).
     * @param offsetY The global offset the native adds afterwards (`mGlobalOffsetY`).
     */
    fun explicitSideBounds(
        anchorBounds: IntRect,
        side: ClickSelectMenuSide,
        alignment: ClickSelectMenuAlignment,
        width: Int,
        height: Int,
        offsetX: Int = 0,
        offsetY: Int = 0,
    ): IntRect {
        val resolvedSide = if (side.isHorizontalForced) side else ClickSelectMenuSide.Start
        val left = if (resolvedSide == ClickSelectMenuSide.Start) {
            anchorBounds.left - width
        } else {
            anchorBounds.right
        }
        val top = when (alignment) {
            ClickSelectMenuAlignment.Start -> anchorBounds.top
            ClickSelectMenuAlignment.End -> anchorBounds.bottom - height
            ClickSelectMenuAlignment.Default -> (anchorBounds.top + anchorBounds.bottom) / 2 - height / 2
        }
        return IntRect(
            left = left + offsetX,
            top = top + offsetY,
            right = left + width + offsetX,
            bottom = top + height + offsetY,
        )
    }

    /**
     * `RotatingPopupMenuLocateHelper.resolveFittingSpec`: demotes a forced cross-axis alignment to
     * [ClickSelectMenuAlignment.Default] when the menu cannot actually be aligned that way.
     *
     * A forced horizontal side is never demoted. For a forced alignment with a positive minimum width,
     * the orthogonal case compares the rotated locate width of `max(menuWidth, contentWidth)` by
     * `contentHeight` against the available width, and the upright case demotes when the minimum width
     * is not wider than the available width or is wider than the cross-axis clearance.
     *
     * @param placement The placement spec to resolve.
     * @param isRtl Whether the anchor's layout direction is RTL.
     * @param menuWidth The menu's minimum width (`COUIPopupListWindow.getMainMenuMinWidth`, which
     *   `COUIRotatingPopupListWindow.reuseMainMenu` passes here); zero or less skips the test.
     * @param rotation The menu's rotation quarter, from [PopupMenuRotationDefaults.classifyDegrees].
     * @param contentWidth The menu's current width, used only by the orthogonal branch, where it
     *   competes with [menuWidth] for the `max` the native takes.
     * @param contentHeight The menu's current height, used only by the orthogonal branch.
     * @param anchorBounds The anchor bounds expanded by the anchor outsets (`getAnchorRealRect`).
     * @param availableBounds The available bounds.
     */
    fun resolveFittingSpec(
        placement: ClickSelectMenuPlacement,
        isRtl: Boolean,
        menuWidth: Int,
        rotation: PopupMenuRotation,
        contentWidth: Int,
        contentHeight: Int,
        anchorBounds: IntRect,
        availableBounds: IntRect,
    ): ClickSelectMenuPlacement {
        if (placement.isSideForced || !placement.alignment.isCrossAxisStartOrEnd || menuWidth <= 0) {
            return placement
        }
        val availableWidth = abs(availableBounds.width)
        val clearance = if (physicalAlignment(placement.alignment, isRtl) == ClickSelectMenuPhysicalAlignment.Left) {
            maxOf(0, availableBounds.right - anchorBounds.left)
        } else {
            maxOf(0, anchorBounds.right - availableBounds.left)
        }
        val demoted = ClickSelectMenuPlacement(
            side = placement.side,
            alignment = ClickSelectMenuAlignment.Default,
        )
        return if (rotation.isOrthogonalForLocate) {
            if (contentWidth > 0 && contentHeight > 0) {
                val rotatedWidth = PopupMenuRotationDefaults.rotatedLocateWidth(
                    rotation = rotation,
                    width = maxOf(menuWidth, contentWidth),
                    height = contentHeight,
                )
                if (rotatedWidth <= availableWidth) demoted else placement
            } else {
                placement
            }
        } else {
            if (menuWidth <= availableWidth || menuWidth > clearance) demoted else placement
        }
    }

    /**
     * `RotatingPopupMenuLocateHelper.computeMainMenuMeasureCaps`: the width and height caps the menu is
     * measured with, as `(maxWidth, maxHeight)`.
     *
     * A forced horizontal side caps the menu by the clearance on that side. The orthogonal case swaps
     * the axes: the native passes `Math.min(availableHeight, availableHeight)` as the width cap for
     * the explicit-side branch, and `min(sideHeight, availableHeight)` for the vertical one, while the
     * height cap becomes the cross-axis clearance clamped into the available width.
     *
     * Otherwise the height cap is the room on the requested vertical side and the width cap is the
     * cross-axis clearance when an alignment is forced, the requested width itself when it is not.
     *
     * @param requestedWidth The menu's maximum width (`COUIPopupListWindow.getMainMenuMaxWidth`, which
     *   `COUIRotatingPopupListWindow.layoutMainMenuForShow` passes here); clamped into the available
     *   width first.
     * @param placement The placement spec.
     * @param isRtl Whether the anchor's layout direction is RTL.
     * @param orthogonalForLocate Whether the menu is rotated a quarter turn.
     * @param anchorBounds The anchor bounds expanded by the anchor outsets (`getAnchorRealRect`).
     * @param availableBounds The available bounds.
     */
    fun mainMenuMeasureCaps(
        requestedWidth: Int,
        placement: ClickSelectMenuPlacement,
        isRtl: Boolean,
        orthogonalForLocate: Boolean,
        anchorBounds: IntRect,
        availableBounds: IntRect,
    ): IntSize {
        val availableWidth = maxOf(0, availableBounds.width)
        val availableHeight = maxOf(0, availableBounds.height)
        val requested = minOf(availableWidth, maxOf(0, requestedWidth))
        val side = resolvePopupSide(placement, isRtl)
        if (side.isHorizontalForced) {
            val clearance = maxOf(0, horizontalClearance(anchorBounds, availableBounds, side))
            return if (orthogonalForLocate) {
                IntSize(width = availableHeight, height = minOf(clearance, availableWidth))
            } else {
                IntSize(width = minOf(requested, clearance), height = availableHeight)
            }
        }
        val sideHeight = when (placement.side) {
            ClickSelectMenuSide.Above -> maxOf(0, anchorBounds.top - availableBounds.top)
            ClickSelectMenuSide.Below -> maxOf(0, availableBounds.bottom - anchorBounds.bottom)
            else -> availableHeight
        }
        val crossAxisClearance = if (placement.alignment.isCrossAxisStartOrEnd) {
            if (physicalAlignment(placement.alignment, isRtl) == ClickSelectMenuPhysicalAlignment.Left) {
                maxOf(0, availableBounds.right - anchorBounds.left)
            } else {
                maxOf(0, anchorBounds.right - availableBounds.left)
            }
        } else {
            availableWidth
        }
        val widthCap = if (placement.alignment.isCrossAxisStartOrEnd) {
            minOf(requested, crossAxisClearance)
        } else {
            requested
        }
        return if (orthogonalForLocate) {
            IntSize(width = minOf(sideHeight, availableHeight), height = minOf(crossAxisClearance, availableWidth))
        } else {
            IntSize(width = widthCap, height = sideHeight)
        }
    }
}

/**
 * A [PopupPositionProvider] that places a click-select menu with a
 * [ClickSelectMenuPlacement], porting the locate rules of `RotatingPopupMenuLocateHelper`.
 *
 * [ListPopupDefaults.dropdownPositionProvider] always centres the menu on the anchor;
 * `RotatingPopupMenuLocateHelper` first resolves the placement spec and then forces an edge, a side, or
 * nothing at all. Use this provider together with
 * [io.github.suqi8.coui.kmp.basic.preciseClickAnchor] so the menu opens at the recorded touch point,
 * which is what `COUIClickSelectMenu.registerForClickSelectItems` does through `PreciseClickHelper`.
 *
 * The anchor outsets are [ClickSelectMenuDefaults.VerticalGapToAnchor] vertically, and they are dropped
 * when the anchor arrives collapsed to a point — the native selects the empty-outset anchor rule for
 * exactly the case in which `setAnchor` collapsed the bounds.
 *
 * @param placement The placement spec.
 * @param verticalMargin The vertical gap to the anchor; defaults to
 *   `dimen/coui_popup_list_window_default_vertical_gap_to_anchor`.
 * @param horizontalMargin The margin kept from the window's horizontal edges, standing in for the
 *   responsive-grid barrier the native accumulates into `mWindowBarriers`.
 * @param locateFromAboveAnchorToBelow Whether the fit test tries the above side first; the
 *   click-select path passes `false`.
 */
fun clickSelectMenuPositionProvider(
    placement: ClickSelectMenuPlacement,
    verticalMargin: Dp = ClickSelectMenuDefaults.VerticalGapToAnchor,
    horizontalMargin: Dp = ListPopupDefaults.HorizontalBarrier,
    locateFromAboveAnchorToBelow: Boolean = false,
): PopupPositionProvider = object : PopupPositionProvider {
    private val margins = PaddingValues(horizontal = horizontalMargin, vertical = verticalMargin)

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowBounds: IntRect,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
        popupMargin: IntRect,
        alignment: PopupPositionProvider.Align,
    ): IntOffset {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val pointAnchored = anchorBounds.width == 0 && anchorBounds.height == 0
        val topOutset = if (pointAnchored) 0 else popupMargin.top
        val bottomOutset = if (pointAnchored) 0 else popupMargin.bottom
        val anchorReal = IntRect(
            left = anchorBounds.left,
            top = anchorBounds.top - topOutset,
            right = anchorBounds.right,
            bottom = anchorBounds.bottom + bottomOutset,
        )
        val available = IntRect(
            left = windowBounds.left + popupMargin.left,
            top = windowBounds.top,
            right = windowBounds.right - popupMargin.right,
            bottom = windowBounds.bottom,
        )
        val side = ClickSelectMenuDefaults.resolvePopupSide(placement, isRtl)
        val bounds = if (side.isHorizontalForced) {
            ClickSelectMenuDefaults.explicitSideBounds(
                anchorBounds = anchorReal,
                side = side,
                alignment = placement.alignment,
                width = popupContentSize.width,
                height = popupContentSize.height,
            )
        } else {
            val x = ClickSelectMenuDefaults.locateMainMenuAnchorX(
                anchorBounds = anchorBounds,
                placement = placement,
                menuWidth = popupContentSize.width,
                availableBounds = available,
                isRtl = isRtl,
            ).x
            val y = ClickSelectMenuDefaults.locateMainMenuY(
                anchorBounds = anchorReal,
                placement = placement,
                locateFromAboveAnchorToBelow = locateFromAboveAnchorToBelow,
                menuHeight = popupContentSize.height,
                availableBounds = available,
            )
            IntRect(left = x, top = y, right = x + popupContentSize.width, bottom = y + popupContentSize.height)
        }
        return IntOffset(bounds.left, bounds.top)
    }

    override fun getMargins(): PaddingValues = margins
}
