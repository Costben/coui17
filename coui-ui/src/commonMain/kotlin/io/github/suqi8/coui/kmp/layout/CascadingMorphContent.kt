// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import io.github.suqi8.coui.kmp.anim.BezierGravityPathGeometry
import io.github.suqi8.coui.kmp.basic.DropdownColors
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.DropdownImpl
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.GlassContainer
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.basic.ListPopupContent
import io.github.suqi8.coui.kmp.basic.LocalPopupInteractionEnabled
import io.github.suqi8.coui.kmp.basic.PopupLayoutPosition
import io.github.suqi8.coui.kmp.basic.PopupListDivider
import io.github.suqi8.coui.kmp.basic.couiGlassSurface
import io.github.suqi8.coui.kmp.basic.popupGlassBlendColors
import io.github.suqi8.coui.kmp.basic.rememberGlassHighlight
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.squircle.addSquircleRoundRect
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled

/**
 * Primary side of a cascading popup. While a secondary is expanded, scales towards its top-right
 * pivot to [PRIMARY_SHRUNK_SCALE] and its content fades to [PRIMARY_SHRUNK_ALPHA].
 *
 * The card itself is [ListPopupContent], the same shell [ListPopupLayout] draws: COUI's main menu is
 * one `COUIPopupListWindow` whether or not a row owns a submenu, so it enters and leaves with the
 * same three waves — the Bezier gravity path, a true 0 -> 1 scale and the opacity spring — and wears
 * the same glass, rim, spot light and height-capped corner. The Gallery's bottom-bar "More" menu is
 * that window; a separate card here made the overflow menu open as a clip-reveal from 15% instead
 * of springing out of the ball along its arc.
 */
@Composable
internal fun CascadingPrimaryContent(
    entries: List<DropdownEntry>,
    expandedItem: DropdownItem?,
    onExpand: (DropdownItem) -> Unit,
    onLeafSelected: (DropdownItem) -> Unit,
    onAnchorBounds: (DropdownItem, IntRect) -> Unit,
    enterFraction: () -> Float,
    enterAlpha: () -> Float,
    pathProgress: () -> Float,
    gravityPath: BezierGravityPathGeometry,
    popupLayoutPosition: PopupLayoutPosition,
    transformOrigin: TransformOrigin,
    shrinkTransformOrigin: TransformOrigin,
    minWidth: Dp,
    dropdownColors: DropdownColors,
    primaryScale: () -> Float,
    primaryAlpha: () -> Float,
    primaryTranslationY: () -> Float,
    onCollapseSecondary: () -> Unit,
    enableGlass: Boolean,
) {
    ListPopupContent(
        popupContentSize = IntSize.Zero,
        onPopupContentSizeChange = NoPopupContentSizeChange,
        fractionProgress = enterFraction,
        alphaProgress = enterAlpha,
        pathProgress = pathProgress,
        gravityPath = gravityPath,
        popupLayoutPosition = popupLayoutPosition,
        localTransformOrigin = transformOrigin,
        enableGlass = enableGlass,
        interactionEnabled = LocalPopupInteractionEnabled.current,
        // The submenu shrink wraps the whole card, enter transform and surface included.
        modifier = Modifier.graphicsLayer {
            val s = primaryScale()
            scaleX = s
            scaleY = s
            translationY = primaryTranslationY()
            this.transformOrigin = shrinkTransformOrigin
        },
    ) {
        CascadingPrimaryColumn(
            entries = entries,
            isExpanded = expandedItem != null,
            onExpand = onExpand,
            onLeafSelected = onLeafSelected,
            onAnchorBounds = onAnchorBounds,
            minWidth = minWidth,
            dropdownColors = dropdownColors,
            primaryAlpha = primaryAlpha,
            onCollapseSecondary = onCollapseSecondary,
        )
    }
}

private val NoPopupContentSizeChange: (IntSize) -> Unit = {}

@Composable
private fun CascadingPrimaryColumn(
    entries: List<DropdownEntry>,
    isExpanded: Boolean,
    onExpand: (DropdownItem) -> Unit,
    onLeafSelected: (DropdownItem) -> Unit,
    onAnchorBounds: (DropdownItem, IntRect) -> Unit,
    minWidth: Dp,
    dropdownColors: DropdownColors,
    primaryAlpha: () -> Float,
    onCollapseSecondary: () -> Unit,
) {
    // ListPopupContent hands its child loose constraints, so the panel's minimum width is restated
    // here: [CascadingListPopupLayout] floors the panel at the caller's `minWidth`, and without
    // passing that floor down to the list the column clamps to its own `ListPopupDefaults.MinWidth`
    // — the row press/selected mask (8dp in from each side of the *row*) would then stop short of a
    // wider panel. Native gives the floor to the list.
    Box(
        propagateMinConstraints = true,
        modifier = Modifier.widthIn(min = minWidth),
    ) {
        Box(
            propagateMinConstraints = true,
            // COUI dims the main menu *content* (the list child) to mMainMenuAlphaPercent while the
            // card material stays opaque, so the panel's blur surface is not faded away.
            modifier = Modifier.graphicsLayer { alpha = primaryAlpha() },
        ) {
            ListPopupColumn {
                val lastEntryIdx = entries.lastIndex
                entries.forEachIndexed { entryIndex, entry ->
                    val lastItemIdx = entry.items.lastIndex
                    val isFirstEntry = entryIndex == 0
                    val isLastEntry = entryIndex == lastEntryIdx
                    entry.items.forEachIndexed { itemIndex, item ->
                        key(entry, item) {
                            CascadingPrimaryRow(
                                item = item,
                                optionSize = entry.items.size,
                                index = itemIndex,
                                entryEnabled = entry.enabled,
                                isFirst = isFirstEntry && itemIndex == 0,
                                isLast = isLastEntry && itemIndex == lastItemIdx,
                                dropdownColors = dropdownColors,
                                groupKey = entryIndex,
                                onClick = {
                                    // The cascading layer is two levels deep: a trigger expands its
                                    // children, and those children are leaves whatever they carry.
                                    if (item.children.isNullOrEmpty()) onLeafSelected(item) else onExpand(item)
                                },
                                onAnchorBounds = onAnchorBounds,
                            )
                        }
                    }
                    if (entryIndex != lastEntryIdx) {
                        key("divider", entry) {
                            // COUI renders every popup separator — group boundaries included — as
                            // the same 12dp band with a centred hairline.
                            PopupListDivider()
                        }
                    }
                }
            }
        }
        if (isExpanded) {
            // Input shield only: COUI dims the main menu through its own alpha
            // (mMainMenuAlphaPercent) instead of painting a scrim over it, and swallows taps so a
            // press on the shrunken main menu collapses the sub-menu rather than re-triggering a row.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onCollapseSecondary() })
                    },
            )
        }
    }
}

@Composable
private fun CascadingPrimaryRow(
    item: DropdownItem,
    optionSize: Int,
    index: Int,
    entryEnabled: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    dropdownColors: DropdownColors,
    onClick: () -> Unit,
    onAnchorBounds: (DropdownItem, IntRect) -> Unit,
    groupKey: Any? = null,
) {
    val rowEnabled = entryEnabled && item.enabled
    // The row has to stay ListPopupColumn's direct child: the column registers a row by reading its
    // parent data, so a wrapper here would hide the row from the gesture host and the popup would
    // treat every tap as a tap on its own backdrop.
    DropdownImpl(
        item = item,
        optionSize = optionSize,
        isSelected = item.selected,
        index = index,
        dropdownColors = dropdownColors,
        enabled = rowEnabled,
        hasSubmenu = !item.children.isNullOrEmpty(),
        isFirst = isFirst,
        isLast = isLast,
        onSelectedIndexChange = { onClick() },
        groupKey = groupKey,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                onAnchorBounds(
                    item,
                    IntRect(
                        left = pos.x.toInt(),
                        top = pos.y.toInt(),
                        right = pos.x.toInt() + coords.size.width,
                        bottom = pos.y.toInt() + coords.size.height,
                    ),
                )
            },
    )
}

/**
 * Secondary side of a cascading popup, laid out as its own card at the native small-screen
 * position. COUI `SmallScreenAnimationController.onSubMenuProgressUpdate` grows the card from a
 * band covering the tapped row (`RoundFrameLayout.setOverrideOutline`) while translating the list
 * from that row up to its final place; the primary menu shrinks and dims behind it.
 */
@Composable
internal fun CascadingSecondaryContent(
    triggerItem: DropdownItem,
    size: IntSize,
    anchorTopInWindow: Int,
    anchorHeightPx: Int,
    primaryBottomInWindow: Int,
    secondaryTopInWindow: Int,
    progress: () -> Float,
    onCollapseSecondary: () -> Unit,
    onLeafSelected: (DropdownItem) -> Unit,
    dropdownColors: DropdownColors,
    surfaceColor: Color,
    enableGlass: Boolean,
) {
    val density = LocalDensity.current
    val innerPaddingPx = with(density) { SubMenuInnerPadding.toPx() }
    val cornerPx = with(density) { CascadingPopupCornerRadius.toPx() }
    // COUI: clippedTopStart = list inner padding; clippedBottomStart = min(anchor height,
    // main menu bottom - anchor top).
    val clipBottomStartPx = minOf(anchorHeightPx, primaryBottomInWindow - anchorTopInWindow)
        .coerceAtLeast(0)
    val translateStartPx = (anchorTopInWindow - secondaryTopInWindow) - innerPaddingPx
    val squircleEnabled = isSquircleEnabled()
    val backdrop = if (enableGlass) LocalBackdrop.current else null
    val highlight = rememberGlassHighlight(GlassContainer.Panel, backdrop)
    // Equality is by value, which is what the glass surface's outline cache reads — re-emitting an
    // unchanged fraction keeps the cached outline, a moved one invalidates it.
    val bandShape = {
        SecondaryBandShape(
            progress = progress(),
            innerPaddingPx = innerPaddingPx,
            clipBottomStartPx = clipBottomStartPx.toFloat(),
            cornerPx = cornerPx,
            widthPx = size.width,
            heightPx = size.height,
            squircleEnabled = squircleEnabled,
        )
    }

    Box(
        // Same reason as the primary panel: without the floor the column clamps to
        // ListPopupDefaults.MinWidth (168dp) while the card keeps the caller's 196dp, leaving a
        // 28dp empty band on the trailing edge and shifting every row's trailing icon left.
        propagateMinConstraints = true,
        modifier = Modifier
            .then(
                with(density) {
                    Modifier
                        .requiredWidth(size.width.toDp())
                        .requiredHeight(size.height.toDp())
                },
            )
            .graphicsLayer {
                translationY = (1f - progress()) * translateStartPx
            }
            // Silhouette clip: the glass surface's own shape only clips its content layer, so the
            // blurred backdrop — and the fallback fill when glass is off — are cut here, in draw
            // phase, where the band fraction is current.
            .drawWithCache {
                onDrawWithContent {
                    clipPath(bandShape().path) {
                        this@onDrawWithContent.drawContent()
                    }
                }
            }
            .couiGlassSurface(
                backdrop = backdrop,
                // Re-emitted per outline request: the blur's outline cache compares by value, so a
                // moved fraction invalidates it and the silhouette tracks the band instead of
                // freezing at the anchor rect.
                shape = bandShape,
                containerColor = surfaceColor,
                fallback = Modifier.drawBehind { drawRect(surfaceColor) },
                blendColors = popupGlassBlendColors(),
                highlight = highlight,
            ),
    ) {
        CascadingSecondaryColumn(
            triggerItem = triggerItem,
            dropdownColors = dropdownColors,
            onHeaderClick = onCollapseSecondary,
            onLeafSelected = onLeafSelected,
        )
    }
}

/** Shared header + children body for the secondary popup. Used by both the real animated layout
 *  and the measurement probe so their sizes can't diverge. */
@Composable
internal fun CascadingSecondaryColumn(
    triggerItem: DropdownItem,
    dropdownColors: DropdownColors,
    onHeaderClick: () -> Unit,
    onLeafSelected: (DropdownItem) -> Unit,
) {
    val children = triggerItem.children.orEmpty()
    ListPopupColumn {
        // COUI's compact path prepends the trigger item itself to the sub menu list
        // (`showSub`: `arrayList.add(popupListItem)`), rendered as an expanded group header.
        MorphHeaderRow(
            triggerItem = triggerItem,
            dropdownColors = dropdownColors,
            onClick = onHeaderClick,
        )
        val lastChildIdx = children.lastIndex
        children.forEachIndexed { index, child ->
            key(child) {
                // No wrapper: the row must stay ListPopupColumn's direct child, or the column cannot
                // register it and the submenu's leaves stop responding to taps.
                DropdownImpl(
                    item = child,
                    optionSize = children.size,
                    isSelected = child.selected,
                    index = index,
                    dropdownColors = dropdownColors,
                    enabled = child.enabled,
                    hasSubmenu = false,
                    // Header owns popup-global "first"; only the trailing child owns "last".
                    isFirst = false,
                    isLast = index == lastChildIdx,
                    onSelectedIndexChange = { onLeafSelected(child) },
                    // Every leaf of one submenu belongs to the same COUI group, so the column keeps
                    // them contiguous; the header joins the same group in the native list.
                    groupKey = Unit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Silhouette of the secondary popup part-way through its expansion: a rounded band whose top edge
 * moves from the list's inner padding to 0 and whose bottom edge grows from the anchor row's
 * height to the card's full height (COUI `RoundFrameLayout.setOverrideOutline`).
 *
 * Equality is by value, which is what the glass surface's outline cache reads — re-emitting an
 * unchanged fraction keeps the cached outline, a moved one invalidates it.
 */
private class SecondaryBandShape(
    private val progress: Float,
    private val innerPaddingPx: Float,
    private val clipBottomStartPx: Float,
    private val cornerPx: Float,
    private val widthPx: Int,
    private val heightPx: Int,
    private val squircleEnabled: Boolean,
) : CornerBasedShape(
    topStart = CornerSize(cornerPx),
    topEnd = CornerSize(cornerPx),
    bottomEnd = CornerSize(cornerPx),
    bottomStart = CornerSize(cornerPx),
) {

    val rect: Rect
        get() = Rect(
            left = 0f,
            top = lerp(innerPaddingPx, 0f, progress),
            right = widthPx.toFloat(),
            bottom = lerp(clipBottomStartPx, heightPx.toFloat(), progress),
        )

    private val radiusPx: Float
        get() = minOf(cornerPx, rect.width / 2f, rect.height / 2f)

    /** Rebuilt per frame: the mask below is wiped between hits and the geometry is moving. */
    val path: Path
        get() {
            val r = radiusPx
            return Path().apply {
                addSquircleRoundRect(
                    rect = rect,
                    topLeft = r,
                    topRight = r,
                    bottomRight = r,
                    bottomLeft = r,
                    squircleEnabled = squircleEnabled,
                    capsuleCircleBlend = false,
                )
            }
        }

    val roundRect: RoundRect
        get() {
            val r = radiusPx
            val radius = CornerRadius(r, r)
            val box = rect
            return RoundRect(
                left = box.left,
                top = box.top,
                right = box.right,
                bottom = box.bottom,
                topLeftCornerRadius = radius,
                topRightCornerRadius = radius,
                bottomRightCornerRadius = radius,
                bottomLeftCornerRadius = radius,
            )
        }

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline = if (squircleEnabled) {
        Outline.Generic(path)
    } else {
        Outline.Rounded(roundRect)
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = if (topStart == this.topStart && topEnd == this.topEnd &&
        bottomEnd == this.bottomEnd && bottomStart == this.bottomStart
    ) {
        this
    } else {
        RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SecondaryBandShape) return false
        return progress == other.progress &&
            innerPaddingPx == other.innerPaddingPx &&
            clipBottomStartPx == other.clipBottomStartPx &&
            cornerPx == other.cornerPx &&
            widthPx == other.widthPx &&
            heightPx == other.heightPx &&
            squircleEnabled == other.squircleEnabled
    }

    override fun hashCode(): Int {
        var result = progress.hashCode()
        result = 31 * result + innerPaddingPx.hashCode()
        result = 31 * result + clipBottomStartPx.hashCode()
        result = 31 * result + cornerPx.hashCode()
        result = 31 * result + widthPx
        result = 31 * result + heightPx
        result = 31 * result + squircleEnabled.hashCode()
        return result
    }
}
