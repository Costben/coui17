// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.search

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.kit.backdrop.CouiBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.LocalCouiBackdrop
import io.github.suqi8.coui.kmp.kit.effect.CouiFloatingRenderer
import io.github.suqi8.coui.kmp.kit.effect.CouiFloatingSurface
import io.github.suqi8.coui.kmp.kit.effect.LocalCouiContourGlowEnabled
import io.github.suqi8.coui.kmp.kit.effect.SpotlightPreset
import io.github.suqi8.coui.kmp.kit.effect.couiFloatingPair
import io.github.suqi8.coui.kmp.kit.effect.couiToolbarFeedback
import io.github.suqi8.coui.kmp.kit.effect.createBlurRenderEffect
import io.github.suqi8.coui.kmp.kit.effect.isKitRenderEffectSupported
import io.github.suqi8.coui.kmp.kit.effect.isKitSurfaceSupported
import io.github.suqi8.coui.kmp.kit.effect.rememberCouiSpotlight
import io.github.suqi8.coui.kmp.kit.input.CouiSearchRelocationBoundary
import io.github.suqi8.coui.kmp.kit.input.couiMovingActionClick
import io.github.suqi8.coui.kmp.kit.motion.couiPressMotion
import io.github.suqi8.coui.kmp.kit.motion.couiTouchMotion
import io.github.suqi8.coui.kmp.kit.motion.rememberCouiPressMotion
import io.github.suqi8.coui.kmp.kit.motion.rememberCouiTouchMotion
import io.github.suqi8.coui.kmp.kit.text.CouiTextKind
import io.github.suqi8.coui.kmp.kit.text.rememberCouiTextStyle
import io.github.suqi8.coui.kmp.kit.theme.CouiTheme
import io.github.suqi8.coui.kmp.kit.topappbar.CouiTopAppBarButtonDefaults
import io.github.suqi8.coui.kmp.kit.topappbar.LocalCouiToolbarBackdrop
import kotlin.math.roundToInt
import io.github.suqi8.coui.kmp.kit.theme.CouiIcon as Icon

/**
 * Persistent settings-style search bar with a split cancel capsule, rendered on the host page's
 * overlay layer with a backdrop that excludes the search bar itself.
 *
 * The host owns query text, expansion state, and search results; cancelling only requests collapse,
 * and clearing only submits an empty query without owning navigation. When [enabled] is false,
 * input, clicks, and interaction feedback stop and own focus is released while query and expansion
 * state remain owned by the host.
 */
@Composable
internal fun SearchBar(
    modifier: Modifier = Modifier,
    state: CouiSearchBarState,
    query: String,
    onQueryChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    placeholder: String,
    cancelText: String? = null,
    clearDescription: String,
    dark: Boolean = CouiTheme.isDark,
    backdrop: CouiBackdrop? = LocalCouiToolbarBackdrop.current ?: LocalCouiBackdrop.current,
    onSearch: () -> Unit = {},
    shadowProgress: Float = 0f,
    colors: CouiSearchBarColors = CouiSearchBarDefaults.colors(dark),
    enabled: Boolean = true,
    textStyle: TextStyle? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    horizontalMargin: Dp = CouiSearchBarDefaults.horizontalMargin,
    verticalMargin: Dp = (CouiSearchBarDefaults.height - CouiSearchBarDefaults.surfaceHeight) / 2,
) {
    val density = LocalDensity.current
    // Positioned after AppBarBlurOverlay; samples the gradient-processed backdrop without title/search.
    val surfaceBackdrop = LocalCouiToolbarBackdrop.current ?: backdrop
    val layoutDirection = LocalLayoutDirection.current
    val rtl = layoutDirection == LayoutDirection.Rtl
    val defaultTextStyle = rememberCouiTextStyle(CouiTextKind.Search)
    val inputTextStyle = if (textStyle != null) defaultTextStyle.merge(textStyle) else defaultTextStyle
    val cancelStyle = rememberCouiTextStyle(CouiTextKind.SearchCancel)
    val textMeasurer = rememberTextMeasurer()
    val hasCancel = !cancelText.isNullOrEmpty()
    val cancelWidth = remember(cancelText, cancelStyle, density, layoutDirection, textMeasurer, hasCancel) {
        if (!hasCancel) {
            0
        } else {
            with(density) {
                (
                    textMeasurer.measure(cancelText, cancelStyle, maxLines = 1).size.width +
                        2 * CouiSearchBarDefaults.cancelPadding.roundToPx()
                    ).coerceAtMost(CouiSearchBarDefaults.cancelMaxWidth.roundToPx())
            }
        }
    }
    val defaultSurfaceHeight = with(density) { CouiSearchBarDefaults.surfaceHeight.roundToPx() }
    val gap = with(density) { CouiSearchBarDefaults.cancelGap.roundToPx() }
    var boundsSize by remember { mutableStateOf(IntSize.Zero) }
    val surfaceHeight = if (boundsSize.height in 1 until defaultSurfaceHeight) boundsSize.height else defaultSurfaceHeight
    val shape = CouiSearchBarDefaults.shape
    val bodyLayer = rememberGraphicsLayer()
    val cancelLayer = rememberGraphicsLayer()
    val renderer = if (isKitSurfaceSupported() && surfaceBackdrop != null) remember { CouiFloatingRenderer.pair() } else null
    val bodySource = interactionSource ?: remember { MutableInteractionSource() }
    val cancelSource = remember { MutableInteractionSource() }
    val press = rememberCouiPressMotion()
    val touch = rememberCouiTouchMotion(enabled && state.expanded && !state.running)
    val bodyTouch = rememberCouiTouchMotion(enabled && !state.expanded && !state.running)
    val bodyPress = rememberCouiPressMotion()
    val (bodyLightInput, bodyLight) = rememberCouiSpotlight(enabled, SpotlightPreset.OpaqueMedium2, dark)
    val (cancelLightInput, cancelLight) = rememberCouiSpotlight(enabled && state.expanded, SpotlightPreset.OpaqueMedium2, dark)
    val foreground = colors.foreground
    val secondary = colors.secondary
    val background = colors.fallbackBackground
    val iconColor = colors.icon
    val requester = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var ownsFocus by remember { mutableStateOf(false) }
    fun releaseInput() {
        if (ownsFocus) {
            keyboard?.hide()
            focus.clearFocus()
        }
    }
    val cancelSearch = {
        if (enabled && state.expanded) {
            releaseInput()
            onExpandedChange(false)
        }
    }
    LaunchedEffect(state.expanded, enabled) {
        if (enabled && state.expanded) {
            if (requester.requestFocus()) keyboard?.show()
        } else {
            releaseInput()
        }
    }
    fun bodySurface(): CouiFloatingSurface {
        val width = (boundsSize.width - (cancelWidth + gap) * state.widthProgress).roundToInt().coerceAtLeast(1).toFloat()
        val y = ((boundsSize.height - surfaceHeight) / 2).toFloat()
        val bodyX = if (rtl) boundsSize.width - width else 0f
        return CouiFloatingSurface(
            Rect(
                bodyX + bodyTouch.translationX.value,
                y + bodyTouch.translationY.value,
                bodyX + bodyTouch.translationX.value + width,
                y + bodyTouch.translationY.value + surfaceHeight,
            ),
            scaleX = bodyTouch.scaleX.value + bodyPress.scale.value - 1f,
            scaleY = bodyTouch.scaleY.value + bodyPress.scale.value - 1f,
            cornerWeight = 1f,
        )
    }
    fun cancelSurface(): CouiFloatingSurface {
        val f = state.cancelProgress
        val y = ((boundsSize.height - surfaceHeight) / 2).toFloat()
        val cancelX = if (rtl) 0f else boundsSize.width - cancelWidth.toFloat()
        val translation = CouiSearchBarDefaults.cancelTranslationPx * (1f - f) * if (rtl) -1f else 1f
        val scale = (.5f + .5f * f).coerceAtLeast(.001f)
        return CouiFloatingSurface(
            Rect(
                cancelX + translation + touch.translationX.value,
                y + touch.translationY.value,
                cancelX + translation + touch.translationX.value + cancelWidth,
                y + touch.translationY.value + surfaceHeight,
            ),
            scaleX = scale * (touch.scaleX.value + press.scale.value - 1f),
            scaleY = scale * (touch.scaleY.value + press.scale.value - 1f),
            cornerWeight = 1f,
            alpha = (f / .2f).coerceIn(0f, 1f),
            visible = hasCancel && state.active,
        )
    }
    val contour = LocalCouiContourGlowEnabled.current
    val themeTint = CouiTheme.materialTintFor(dark)
    var normalShadow by remember { mutableFloatStateOf(shadowProgress) }
    if (!state.active) SideEffect { normalShadow = shadowProgress }
    // The home page pauses scroll shadow callbacks during the search spring; do not illuminate
    // the shadow early from scroll offsets caused by collapsing search.
    val retainedShadow = if (state.active) normalShadow else shadowProgress
    val keepScrollShadow = if (state.active) {
        retainedShadow >= 1f || (state.expanded && retainedShadow > .5f)
    } else {
        retainedShadow > .5f
    }
    val drop = if (keepScrollShadow) {
        retainedShadow.coerceIn(0f, 1f)
    } else {
        ((state.widthProgress - .8f) / .2f).coerceIn(0f, 1f)
    }
    val baseShadow = CouiTopAppBarButtonDefaults.shadow(dark, true, density.density, contour, search = true, alphaScale = drop)
    val shadow = baseShadow.copy(causticColor = baseShadow.causticColor.copy(alpha = baseShadow.causticColor.alpha * drop))
    val surfaceModifier = if (isKitSurfaceSupported() && renderer != null && surfaceBackdrop != null) {
        Modifier.couiFloatingPair(
            surfaceBackdrop,
            renderer,
            listOf(
                CouiTopAppBarButtonDefaults.systemMaterial(dark, true).copy(
                    index = 1,
                    blurRadius = CouiSearchBarDefaults.blurRadius,
                    extension = with(density) { 4.dp.roundToPx() },
                    themeTint = themeTint,
                ),
                CouiTopAppBarButtonDefaults.systemMaterial(dark, true).copy(
                    index = 2,
                    blurRadius = CouiSearchBarDefaults.blurRadius,
                    extension = with(density) { 4.dp.roundToPx() },
                    themeTint = themeTint,
                ),
            ),
            listOf(shadow, shadow),
            listOf(shape, shape),
            listOf(bodyLayer, cancelLayer),
            contour,
            // Match upstream bringChildToFront(functional) before elevating searchBarBackgroundView:
            // reorder whole material+content layers rather than only lifting the transparent text layer.
            drawOrder = listOf(1, 0),
            // Composite only foregrounds so neither capsule erases the gradient backdrop or sibling shadow with Src.
            foregroundComposition = true,
            beforeContent = { index, path ->
                val node = if (index == 0) bodySurface() else cancelSurface()
                val draw = if (index == 0) bodyLight else cancelLight
                if (draw != null && node.visible) {
                    inset(
                        node.bounds.left,
                        node.bounds.top,
                        size.width - node.bounds.right,
                        size.height - node.bounds.bottom,
                    ) {
                        withTransform({ scale(node.scaleX, node.scaleY, center) }) { clipPath(path) { draw() } }
                    }
                }
            },
            surfaces = { listOf(bodySurface(), cancelSurface()) },
        )
    } else {
        Modifier
    }

    Layout(
        modifier = modifier.fillMaxWidth().height(CouiSearchBarDefaults.surfaceHeight + verticalMargin * 2)
            .padding(horizontal = horizontalMargin).onSizeChanged { boundsSize = it }
            .then(CouiSearchRelocationBoundary)
            .then(surfaceModifier),
        content = {
            val recordBody = if (renderer != null && surfaceBackdrop != null) {
                Modifier.drawWithContent { bodyLayer.record { this@drawWithContent.drawContent() } }
            } else {
                Modifier.background(background, shape).clip(shape).drawWithContent {
                    bodyLight?.invoke(this)
                    drawContent()
                }
            }
            Box(
                Modifier.graphicsLayer {
                    val node = bodySurface()
                    translationX = bodyTouch.translationX.value
                    translationY = bodyTouch.translationY.value
                    scaleX = node.scaleX
                    scaleY = node.scaleY
                }.couiTouchMotion(bodyTouch, enabled && !state.expanded && !state.running)
                    .couiPressMotion(bodyPress, enabled && !state.expanded && !state.running)
                    .then(bodyLightInput).then(recordBody).clip(shape).couiToolbarFeedback(dark, bodySource, enabled && !state.expanded)
                    .then(
                        if (!state.expanded) {
                            Modifier.semantics { contentDescription = placeholder }
                                .clickable(bodySource, null, enabled = enabled, role = Role.Button) { onExpandedChange(true) }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                // Decoration belongs to the input body; the full capsule's touch and semantics must
                // not fall through padding onto the underlying cancel button.
                BasicTextField(
                    query,
                    { if (enabled && state.expanded) onQueryChange(it) },
                    Modifier.fillMaxSize().focusRequester(requester)
                        .onFocusChanged { ownsFocus = it.hasFocus }
                        .then(if (!state.expanded) Modifier.clearAndSetSemantics {} else Modifier),
                    enabled = enabled && state.expanded,
                    readOnly = !enabled || !state.expanded,
                    singleLine = true,
                    textStyle = if (inputTextStyle.color != Color.Unspecified) inputTextStyle else inputTextStyle.copy(color = foreground),
                    cursorBrush = SolidColor(foreground),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (enabled && state.expanded) {
                            keyboard?.hide()
                            onSearch()
                        }
                    }),
                    interactionSource = bodySource,
                    decorationBox = { field ->
                        Row(
                            Modifier.fillMaxSize().padding(
                                start = CouiSearchBarDefaults.iconStart,
                                end = CouiSearchBarDefaults.textEnd,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(CouiSearchBarDefaults.innerIconSlot), contentAlignment = Alignment.Center) {
                                if (leadingIcon != null) {
                                    leadingIcon()
                                } else {
                                    Icon(
                                        modifier = Modifier.size(CouiSearchBarDefaults.iconSize),
                                        imageVector = CouiSearchIcon.imageVector,
                                        contentDescription = null,
                                        tint = iconColor,
                                    )
                                }
                            }
                            Box(Modifier.weight(1f)) {
                                if (query.isEmpty()) BasicText(placeholder, style = inputTextStyle.copy(color = secondary), maxLines = 1)
                                field()
                            }
                            if (trailingIcon != null) {
                                Box(Modifier.size(CouiSearchBarDefaults.innerIconSlot), contentAlignment = Alignment.Center) {
                                    trailingIcon()
                                }
                            } else if (state.expanded && query.isNotEmpty()) {
                                Canvas(
                                    Modifier.size(CouiSearchBarDefaults.innerIconSlot).semantics { contentDescription = clearDescription }
                                        .clickable(
                                            remember { MutableInteractionSource() },
                                            null,
                                            enabled = enabled,
                                            role = Role.Button,
                                        ) { onQueryChange("") },
                                ) {
                                    val unit = this.size.width / 36f
                                    drawCircle(colors.clear, radius = 9 * unit)
                                    val p = Path().apply {
                                        moveTo(18f, 16.868f)
                                        lineTo(15.031f, 13.898f)
                                        lineTo(13.899f, 15.03f)
                                        lineTo(16.869f, 18f)
                                        lineTo(13.898f, 20.97f)
                                        lineTo(15.03f, 22.101f)
                                        lineTo(18f, 19.131f)
                                        lineTo(20.97f, 22.101f)
                                        lineTo(22.102f, 20.969f)
                                        lineTo(19.132f, 18f)
                                        lineTo(22.101f, 15.03f)
                                        lineTo(20.969f, 13.899f)
                                        this.close()
                                    }
                                    withTransform({ scale(unit, unit, Offset.Zero) }) { drawPath(p, Color.White) }
                                }
                            }
                        }
                    },
                )
            }
            val recordCancel = if (renderer != null && surfaceBackdrop != null) {
                Modifier.drawWithContent { cancelLayer.record { this@drawWithContent.drawContent() } }
            } else {
                Modifier.background(background, shape)
                    .clip(shape).drawWithContent {
                        cancelLight?.invoke(this)
                        drawContent()
                    }
            }
            Box(
                Modifier.width(with(density) { cancelWidth.toDp() }).height(CouiSearchBarDefaults.surfaceHeight)
                    .graphicsLayer {
                        val node = cancelSurface()
                        translationX = node.bounds.left - (if (rtl) 0f else boundsSize.width - cancelWidth.toFloat())
                        translationY = touch.translationY.value
                        scaleX = node.scaleX
                        scaleY = node.scaleY
                        alpha = if (renderer != null && surfaceBackdrop != null) node.alpha else state.cancelProgress.coerceIn(0f, 1f)
                    }
                    .couiTouchMotion(touch, enabled && state.expanded && !state.running).couiPressMotion(press, enabled && state.expanded)
                    .then(cancelLightInput).then(recordCancel).clip(shape).couiToolbarFeedback(dark, cancelSource, enabled && state.expanded)
                    .couiMovingActionClick(cancelSource, enabled && state.expanded, state.running, cancelSearch),
                contentAlignment = Alignment.Center,
            ) {
                val textAlpha = (foreground.alpha * 255f * state.cancelProgress.coerceIn(0f, 1f)).roundToInt() / 255f
                val blur = (1f - state.cancelProgress).coerceAtLeast(0f) * CouiSearchBarDefaults.cancelContentBlurRadius
                BasicText(
                    cancelText ?: "",
                    Modifier.graphicsLayer {
                        renderEffect = if (isKitRenderEffectSupported() && blur > 0f) {
                            createBlurRenderEffect(blur, blur, TileMode.Decal)
                        } else {
                            null
                        }
                    },
                    style = cancelStyle.copy(color = foreground.copy(alpha = textAlpha)),
                    maxLines = 1,
                )
            }
        },
    ) { children, constraints ->
        val width = (constraints.maxWidth - (if (hasCancel) (cancelWidth + gap) * state.widthProgress else 0f)).roundToInt().coerceAtLeast(1)
        val body = children[0].measure(Constraints.fixed(width, surfaceHeight))
        val button = children[1].measure(Constraints.fixed(cancelWidth, surfaceHeight))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val y = (constraints.maxHeight - surfaceHeight) / 2
            if (hasCancel && state.active && state.cancelProgress > 0f) {
                button.placeRelative(constraints.maxWidth - cancelWidth, y, zIndex = 1f)
            }
            body.placeRelative(0, y)
        }
    }
}
