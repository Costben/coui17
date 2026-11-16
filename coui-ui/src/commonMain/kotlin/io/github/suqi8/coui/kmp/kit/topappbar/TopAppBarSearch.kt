// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package io.github.suqi8.coui.kmp.kit.topappbar

import androidx.compose.animation.animateBounds
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.lookaheadScopeCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.kit.effect.CouiFloatingRenderer
import io.github.suqi8.coui.kmp.kit.effect.CouiFloatingSurface
import io.github.suqi8.coui.kmp.kit.effect.SpotlightPreset
import io.github.suqi8.coui.kmp.kit.effect.couiFloatingSurface
import io.github.suqi8.coui.kmp.kit.effect.createBlurRenderEffect
import io.github.suqi8.coui.kmp.kit.effect.isKitRenderEffectSupported
import io.github.suqi8.coui.kmp.kit.effect.rememberCouiSpotlight
import io.github.suqi8.coui.kmp.kit.input.CouiSearchRelocationBoundary
import io.github.suqi8.coui.kmp.kit.motion.couiPressMotion
import io.github.suqi8.coui.kmp.kit.motion.couiTouchMotion
import io.github.suqi8.coui.kmp.kit.motion.rememberCouiPressMotion
import io.github.suqi8.coui.kmp.kit.motion.rememberCouiTouchMotion
import io.github.suqi8.coui.kmp.kit.search.CouiSearchBarDefaults
import io.github.suqi8.coui.kmp.kit.search.CouiSearchIcon
import io.github.suqi8.coui.kmp.kit.theme.LocalCouiTextStyle
import io.github.suqi8.coui.kmp.kit.theme.ProvideCouiContent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import io.github.suqi8.coui.kmp.kit.theme.CouiIcon as Icon
import io.github.suqi8.coui.kmp.kit.theme.CouiText as Text
import io.github.suqi8.coui.kmp.kit.theme.LocalCouiContentColor as LocalContentColor

/**
 * Title slot entry point. [visible] fades the group without altering search expand/collapse targets or page query.
 * [action] provides non-interactive content for the paired trailing capsule; clicks, material, and feedback belong to this component.
 * Passing `null` for [onActionClick] disables the action; collapsing preserves merged drawing and immediately disables input and semantics.
 */
@Composable
internal fun TopAppBarSearch(
    modifier: Modifier = Modifier,
    state: CouiTopAppBarSearchState,
    style: CouiToolbarStyle = CouiTopAppBarButtonDefaults.style(search = true),
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    placeholder: String,
    focusPending: Boolean,
    onFocused: () -> Unit,
    enabled: Boolean = true,
    visible: Boolean = true,
    endPadding: Dp = 6.dp,
    toolbarHeight: Dp = 64.dp,
    trailingIcon: @Composable () -> Unit = {},
    action: (@Composable () -> Unit)? = null,
    onActionClick: (() -> Unit)? = null,
    title: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalCouiToolbarStyle provides style) {
        val requester = remember { FocusRequester() }
        BoxWithConstraints(
            modifier.fillMaxWidth().height(toolbarHeight)
                .then(CouiSearchRelocationBoundary),
            contentAlignment = Alignment.CenterStart,
        ) {
            val density = LocalDensity.current
            val targetWidth = (constraints.maxWidth - with(density) { endPadding.roundToPx() }).coerceAtLeast(0).toFloat()
            SideEffect {
                if ((visible && state.motion.expanded) || state.expandedWidth == 0f) state.expandedWidth = targetWidth
            }
            TopAppBarActionVisibility(visible = visible, animateSize = false) {
                Box(
                    Modifier.then(if (state.motion.expanded) Modifier.clearAndSetSemantics {} else Modifier)
                        .toolbarShadowOpacity { state.motion.toolbarAlpha },
                ) { title() }
            }

            val renderer = checkNotNull(state.renderer)
            if (state.motion.present && state.expandedWidth > 0f) {
                val overlay = checkNotNull(LocalCouiToolbarOverlay.current)
                val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                val fullWidth = state.expandedWidth
                val height = with(density) { 40.dp.roundToPx().toFloat() }
                val progress = state.motion.progress
                val visibleWidth = max(height, height + (fullWidth - height) * progress)
                val blurRadius = max(0f, 1f - abs((1f - progress) - 0.5f) * 2f) * 30f
                var expandedCenter by remember { mutableStateOf<Offset?>(null) }
                val blur = remember(blurRadius) {
                    if (blurRadius > 0f && isKitRenderEffectSupported()) {
                        createBlurRenderEffect(blurRadius, blurRadius, TileMode.Decal)
                    } else {
                        null
                    }
                }
                overlay.apply {
                    TopAppBarActionVisibility(
                        modifier = Modifier.padding(end = endPadding)
                            .renderInSharedTransitionScopeOverlay(renderInOverlay = { true }, zIndexInOverlay = 1f),
                        visible = visible,
                        animateSize = false,
                    ) {
                        Layout(
                            content = {
                                val input: @Composable (Float) -> Unit = { inputWidth ->
                                    SearchInput(
                                        query,
                                        onQueryChange,
                                        onSearch,
                                        placeholder,
                                        requester,
                                        enabled = enabled,
                                        active = visible && state.motion.expanded,
                                        progress = progress,
                                        fullWidth = inputWidth,
                                        rtl = rtl,
                                        focusPending = focusPending,
                                        onFocused = onFocused,
                                        trailingIcon = trailingIcon,
                                    )
                                }
                                val active = visible && state.motion.expanded
                                if (action != null) {
                                    TopAppBarSearchPair(
                                        fullWidth = fullWidth,
                                        progress = progress,
                                        enabled = enabled,
                                        active = active,
                                        deform = enabled && active && !state.motion.running,
                                        animating = state.motion.running,
                                        capsule = state.motion.expanded,
                                        contentRenderEffect = blur,
                                        action = action,
                                        onActionClick = onActionClick,
                                        input = input,
                                    )
                                } else {
                                    SearchSurface(
                                        modifier = Modifier,
                                        renderer = renderer,
                                        capsule = state.motion.expanded,
                                        enabled = enabled,
                                        interactive = enabled && active,
                                        deform = enabled && active && !state.motion.running,
                                        contentRenderEffect = blur,
                                    ) { input(fullWidth) }
                                }
                            },
                        ) { measurables, constraints ->
                            val child = measurables.single().measure(Constraints.fixed(visibleWidth.roundToInt(), height.roundToInt()))
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                val iconCenter = if (rtl) constraints.maxWidth - height * 0.5f else height * 0.5f
                                val top = (constraints.maxHeight - child.height) / 2
                                val center = Offset(iconCenter, top + height * 0.5f)
                                var offset = Offset.Zero
                                if (!isLookingAhead) {
                                    coordinates?.let { target ->
                                        val root = lookaheadScopeCoordinates
                                        if (expandedCenter == null || (visible && state.motion.expanded && !state.motion.running)) {
                                            expandedCenter = root.localPositionOf(target, center)
                                        }
                                        // Retain the expanded endpoint inside the toolbar so collapse does not recompute paths against a recovering title slot.
                                        val end = checkNotNull(expandedCenter)
                                        offset = target.localPositionOf(root, lerp(state.sourceCenter ?: end, end, progress)) - center
                                    }
                                }
                                child.placeWithLayer(if (rtl) constraints.maxWidth - child.width else 0, top) {
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Shares material with the expanded search field; allows pressing the entry button again near the end of collapse. */
@Composable
internal fun TopAppBarSearchButton(
    modifier: Modifier = Modifier,
    state: CouiTopAppBarSearchState,
    style: CouiToolbarStyle = CouiTopAppBarButtonDefaults.style(search = true),
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean = true,
) {
    CompositionLocalProvider(LocalCouiToolbarStyle provides style) {
        val overlay = LocalCouiToolbarOverlay.current
        TopAppBarSearchAction(
            modifier = modifier.onPlaced {
                if ((!state.motion.present || (!state.motion.expanded && state.sourceCenter == null))) {
                    state.sourceCenter = checkNotNull(overlay).lookaheadScopeCoordinates(it)
                        .localPositionOf(it, Offset(it.size.width * 0.5f, it.size.height * 0.5f))
                }
            },
            state = state,
        ) {
            Box(
                Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                val open = {
                    state.motion.finishCollapse()
                    onClick()
                }

                if (state.motion.present) {
                    Box(
                        Modifier.size(40.dp).semantics { this.contentDescription = contentDescription }
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                enabled = enabled && !state.motion.expanded,
                                role = Role.Button,
                                onClick = open,
                            ),
                    )
                } else {
                    SearchSurface(
                        modifier = Modifier.size(40.dp),
                        renderer = checkNotNull(state.renderer),
                        capsule = false,
                        enabled = enabled,
                        interactive = enabled,
                        deform = enabled,
                        onClick = { center ->
                            state.sourceCenter = center
                            open()
                        },
                    ) {
                        Icon(
                            modifier = Modifier.size(CouiSearchBarDefaults.iconSize),
                            imageVector = CouiSearchIcon.imageVector,
                            contentDescription = contentDescription,
                        )
                    }
                }
            }
        }
    }
}

/** Exiting actions fade in place; persistent actions animate smoothly to their new layout positions. */
@Composable
internal fun TopAppBarSearchAction(
    modifier: Modifier = Modifier,
    state: CouiTopAppBarSearchState,
    visibleWhileSearching: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (visibleWhileSearching) {
        val placement = modifier.animateBounds(
            lookaheadScope = checkNotNull(LocalCouiToolbarOverlay.current),
            boundsTransform = state.motion.actionBoundsTransform,
        )
        Box(placement) { content() }
        return
    }

    val overlay = checkNotNull(LocalCouiToolbarOverlay.current)
    var restingOrigin by remember { mutableStateOf<Offset?>(null) }
    overlay.apply {
        Layout(
            modifier = modifier.then(if (state.motion.expanded) Modifier.clearAndSetSemantics {} else Modifier),
            content = {
                Box(
                    Modifier.toolbarShadowOpacity { state.motion.toolbarAlpha },
                    propagateMinConstraints = true,
                ) { content() }
            },
        ) { measurables, constraints ->
            val child = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
            layout(if (state.motion.expanded) 0 else child.width, child.height) {
                var offset = Offset.Zero
                if (!isLookingAhead) {
                    coordinates?.let { target ->
                        val root = lookaheadScopeCoordinates
                        if (!state.motion.present || (restingOrigin == null && !state.motion.expanded)) {
                            restingOrigin = root.localPositionOf(target, Offset.Zero)
                        }
                        restingOrigin?.let { offset = target.localPositionOf(root, it) }
                    }
                }
                // Compensate action-area shift inside the same placement pass.
                if (!state.motion.expanded && state.motion.toolbarAlpha > 0f) {
                    child.placeWithLayer(0, 0) {
                        translationX = offset.x
                        translationY = offset.y
                        scaleX = state.motion.toolbarScale
                        scaleY = state.motion.toolbarScale
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    placeholder: String,
    requester: FocusRequester,
    enabled: Boolean,
    active: Boolean,
    progress: Float,
    fullWidth: Float,
    rtl: Boolean,
    focusPending: Boolean,
    onFocused: () -> Unit,
    trailingIcon: @Composable () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    // Compose alongside the input node so phased measurement of custom actions never outpaces focus requests.
    LaunchedEffect(focusPending, enabled, active) {
        if (focusPending && enabled && active) {
            if (requester.requestFocus()) keyboard?.show()
            onFocused()
        }
    }
    // Hide selection handles inside the 40dp capsule: 25dp handles overflow and CoreTextField recalculates visibility on position updates during spring bounce.
    val inheritedSelectionColors = LocalTextSelectionColors.current
    val selectionColors = remember(inheritedSelectionColors) {
        TextSelectionColors(
            handleColor = Color.Transparent,
            backgroundColor = inheritedSelectionColors.backgroundColor,
        )
    }
    // Lay out at full target width and clip to current width; full-width input prevents overlap regions from leaking touches to underlying buttons.
    Layout(content = {
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxSize().focusRequester(requester)
                    .semantics { contentDescription = placeholder }
                    .then(if (!active) Modifier.clearAndSetSemantics {} else Modifier),
                enabled = enabled && active,
                singleLine = true,
                textStyle = LocalCouiTextStyle.current.copy(color = LocalContentColor.current, fontSize = 15.sp),
                cursorBrush = SolidColor(LocalCouiToolbarStyle.current.cursorColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                decorationBox = { input ->
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.padding(start = 2.dp).size(36.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                modifier = Modifier.size(CouiSearchBarDefaults.iconSize),
                                imageVector = CouiSearchIcon.imageVector,
                                contentDescription = null,
                            )
                        }
                        Box(
                            Modifier.weight(1f).graphicsLayer { alpha = progress.coerceIn(0f, 1f) },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = LocalContentColor.current.copy(alpha = LocalContentColor.current.alpha * 0.6f),
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            input()
                        }
                        Box(Modifier.graphicsLayer { alpha = progress.coerceIn(0f, 1f) }) { trailingIcon() }
                    }
                },
            )
        }
    }) { measurables, constraints ->
        val child = measurables.single().measure(Constraints.fixed(fullWidth.roundToInt(), constraints.maxHeight))
        layout(constraints.maxWidth, constraints.maxHeight) {
            child.place(if (rtl) constraints.maxWidth - child.width else 0, 0)
        }
    }
}

@Composable
private fun SearchSurface(
    modifier: Modifier = Modifier,
    renderer: CouiFloatingRenderer,
    capsule: Boolean,
    enabled: Boolean,
    interactive: Boolean,
    deform: Boolean,
    onClick: ((Offset) -> Unit)? = null,
    contentRenderEffect: RenderEffect? = null,
    content: @Composable () -> Unit,
) {
    val backdrop = checkNotNull(LocalCouiToolbarBackdrop.current)
    val overlay = checkNotNull(LocalCouiToolbarOverlay.current)
    val style = LocalCouiToolbarStyle.current
    val dark = style.dark
    val contour = style.contourGlowEnabled
    var boundsSize by remember { mutableStateOf(IntSize.Zero) }
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val shape = style.capsuleShape
    val material = remember(style, capsule) { style.material(capsule) }
    val shadow = remember(style) { style.shadow(true) }
    val contentLayer = rememberGraphicsLayer()
    SideEffect { contentLayer.renderEffect = contentRenderEffect }
    val motion = rememberCouiTouchMotion(deform)
    val press = rememberCouiPressMotion()
    val (lightInput, drawLight) = rememberCouiSpotlight(interactive, SpotlightPreset.OpaqueMedium2, dark)
    val interactions = remember { MutableInteractionSource() }
    ProvideCouiContent(
        color = style.contentColor.copy(alpha = style.contentColor.alpha * if (enabled) 1f else 0.38f),
        textStyle = style.textStyle,
    ) {
        Box(
            modifier.onGloballyPositioned { coordinates = it }.onSizeChanged { boundsSize = it }
                .drawWithCache {
                    val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
                    onDrawWithContent {
                        drawContent()
                        if (drawLight != null) {
                            withTransform({
                                translate(motion.translationX.value, motion.translationY.value)
                                scale(motion.scaleX.value + press.scale.value - 1f, motion.scaleY.value + press.scale.value - 1f, center)
                            }) { clipPath(path) { drawLight.invoke(this) } }
                        }
                    }
                }
                .couiFloatingSurface(
                    backdrop,
                    renderer,
                    material,
                    shadow,
                    shape,
                    contentLayer,
                    contour,
                    foregroundComposition = true,
                ) {
                    CouiFloatingSurface(
                        bounds = Rect(Offset(motion.translationX.value, motion.translationY.value), Size(boundsSize.width.toFloat(), boundsSize.height.toFloat())),
                        scaleX = motion.scaleX.value + press.scale.value - 1f,
                        scaleY = motion.scaleY.value + press.scale.value - 1f,
                        cornerWeight = 1f,
                    )
                },
        ) {
            Box(
                Modifier.matchParentSize().graphicsLayer {
                    translationX = motion.translationX.value
                    translationY = motion.translationY.value
                    scaleX = motion.scaleX.value + press.scale.value - 1f
                    scaleY = motion.scaleY.value + press.scale.value - 1f
                }.couiTouchMotion(motion, deform).couiPressMotion(press, deform).then(lightInput)
                    .drawWithContent { contentLayer.record { this@drawWithContent.drawContent() } }
                    .clip(shape)
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(
                                interactionSource = interactions,
                                indication = null,
                                enabled = interactive,
                                role = Role.Button,
                                onClick = {
                                    val target = checkNotNull(coordinates)
                                    onClick(
                                        overlay.lookaheadScopeCoordinates(target).localPositionOf(
                                            target,
                                            Offset(
                                                target.size.width * 0.5f + motion.translationX.value,
                                                target.size.height * 0.5f + motion.translationY.value,
                                            ),
                                        ),
                                    )
                                },
                            )
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) { content() }
        }
    }
}
