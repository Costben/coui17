// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
import io.github.suqi8.coui.kmp.blur.isBlurEnabled
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalContentColor
import io.github.suqi8.coui.kmp.utils.hasFocusReassignBug
import kotlinx.coroutines.delay
import kotlin.math.PI

/**
 * A [SearchBar] component with COUI style.
 *
 * @param inputField the input field to input a query in the [SearchBar].
 * @param onExpandedChange the callback to be invoked when the [SearchBar]'s expanded state is
 *   changed.
 * @param modifier the [Modifier] to be applied to the [SearchBar].
 * @param insideMargin The margin inside the [SearchBar].
 * @param expanded whether the [SearchBar] is expanded and showing search results.
 * @param outsideEndAction the action to be shown at the end side of the [SearchBar] when it is
 *   expanded.
 * @param content the content to be shown when the [SearchBar] is expanded.
 */
@Composable
fun SearchBar(
    inputField: @Composable () -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    insideMargin: DpSize = SearchBarDefaults.InsideMargin,
    expanded: Boolean = false,
    outsideEndAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    val actionSlideDistancePx = with(LocalDensity.current) { ActionSlideDistance.roundToPx() }
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = insideMargin.height, horizontal = insideMargin.width),
            ) {
                inputField()
            }
            // COUISearchBar edit-state transition: background resize, button slide and fade,
            // all with COUIMoveEaseInterpolator. Bound to a local so the non-null branch
            // stays smart-cast inside the trailing lambdas below.
            val endAction = outsideEndAction
            if (endAction != null) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandHorizontally(animationSpec = tween(BackgroundResizeDurationMillis, easing = MoveEase)) +
                        slideInHorizontally(
                            animationSpec = tween(ActionOffsetDurationMillis, easing = MoveEase),
                            initialOffsetX = { actionSlideDistancePx },
                        ) +
                        fadeIn(animationSpec = tween(ActionAlphaDurationMillis, easing = MoveEase)),
                    exit = shrinkHorizontally(animationSpec = tween(BackgroundResizeDurationMillis, easing = MoveEase)) +
                        slideOutHorizontally(
                            animationSpec = tween(ActionOffsetDurationMillis, easing = MoveEase),
                            targetOffsetX = { actionSlideDistancePx },
                        ) +
                        fadeOut(animationSpec = tween(ActionAlphaDurationMillis, easing = MoveEase)),
                ) {
                    Box(
                        modifier = Modifier.padding(
                            start = ActionExtraStartPadding,
                            end = ActionEndPadding,
                        ),
                    ) {
                        endAction()
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
        ) {
            content()
        }
    }

    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = expanded,
        onBackCompleted = {
            currentOnExpandedChange(false)
        },
    )
}

/**
 * A [SearchBar] that owns its own [InputField] and its edit-state "cancel" capsule, and melts the
 * two together with COUI's melt ball while the bar is being edited.
 *
 * `COUISearchBar.changeStateWithAnimation` calls `setBallChain()` before it starts the transition:
 * the search capsule and the functional button join one metaball chain
 * (`OplusMaterialMetaBallParams(1, 0, 4dp, 1.5f, 2.0f)` for the field, index 2 for the button), so
 * the button grows out of the capsule's trailing end instead of sliding in beside it. The chain is
 * only built when the material is on — `mUseMaterialEffectAttr != 0 && isSupportBlur() &&
 * isBlurEnable()` — so a bar whose blur is off falls back to two separate capsules, which is what
 * [metaBallFusion] selects.
 *
 * The geometry is the native one: `measureBackground` shrinks the capsule to
 * `width - buttonWidth - coui_search_bar_functional_button_start_gap`, `doWidthChangedAnimate`
 * lerps that back to the full width over the edit progress, and the button scales from
 * `StateChangedToEditParamsHigh.functionalButtonInitScale` (0.5) with `translationX` running from
 * `mFunctionalButtonOffsetX` to zero. All three tracks ride one spring, response 0.5s / bounce
 * 0.25 (`StateChangedToEditParamsHigh` and `StateChangedToNormalParamsHigh` carry the same pair).
 *
 * @param query the query text to be shown in the input field.
 * @param onQueryChange the callback to be invoked when the input service updates the query.
 * @param onSearch the callback invoked when the input service triggers the [ImeAction.Search] action.
 * @param onExpandedChange the callback to be invoked when the bar's expanded state is changed.
 * @param modifier the [Modifier] to be applied to the bar.
 * @param expanded whether the bar is expanded and showing search results.
 * @param label the hint shown while [query] is empty.
 * @param enabled the enabled state of the input field.
 * @param textStyle Style configuration applied at character level to the input text.
 * @param colors the colors of the field and of the cancel capsule.
 * @param stroke the material rim of the capsules, or `null` for no rim. On the fused path the rim
 *   follows the merged outline, so both capsules share one edge.
 * @param leadingIcon the leading icon of the input field.
 * @param trailingIcon the trailing icon of the input field.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for the input field.
 * @param glowColor the caustic light cast below the capsules, or `null` to disable it. See
 *   [SearchBarDefaults.causticGlowColor]: the system gates it behind a material feature that the
 *   reference devices report off, so it is `null` by default.
 * @param backdrop the backdrop blurred under the capsules, or `null` for a flat fill.
 * @param insideMargin the margin around the bar's content, `(16dp, 6dp)` by default: the 40dp
 *   capsule inside the 52dp search band.
 * @param cancelText the label of the cancel capsule, or `null` for a bar without one.
 * @param onCancel the callback invoked when the cancel capsule is clicked; collapses by default.
 * @param metaBallFusion whether the cancel capsule may merge with the field. Defaults to the
 *   system's condition, [SearchBarDefaults.metaBallFusion]; pass `false` for the two-capsule form.
 * @param clearContentDescription the accessibility label of the default clear button. COUI reads
 *   it from `R.string.coui_search_clear_button_description`; a library has no string resources, so
 *   the caller passes its own localized copy here.
 * @param content the content to be shown when the bar is expanded.
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    label: String = "",
    enabled: Boolean = true,
    textStyle: TextStyle? = null,
    colors: SearchBarColors = SearchBarDefaults.searchBarColors(),
    stroke: MaterialStrokeParams? = SearchBarDefaults.searchBarStroke(),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    glowColor: Color? = null,
    backdrop: Backdrop? = null,
    insideMargin: DpSize = SearchBarDefaults.InsideMargin,
    cancelText: String? = null,
    onCancel: () -> Unit = { onExpandedChange(false) },
    metaBallFusion: Boolean = SearchBarDefaults.metaBallFusion(),
    clearContentDescription: String = SearchBarDefaults.ClearContentDescription,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val field: @Composable (Boolean) -> Unit = { fillOwnSurface ->
        InputField(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            modifier = Modifier.fillMaxWidth(),
            label = label,
            enabled = enabled,
            textStyle = textStyle,
            // The fused path paints the merged silhouette itself, so the field contributes only its
            // content: a transparent fill, no rim and no caustic of its own.
            colors = if (fillOwnSurface) colors else colors.copy(backgroundColor = Color.Transparent),
            stroke = if (fillOwnSurface) stroke else null,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            interactionSource = interactionSource,
            glowColor = if (fillOwnSurface) glowColor else null,
            backdrop = null,
            clearContentDescription = clearContentDescription,
        )
    }
    if (cancelText == null || !metaBallFusion) {
        SearchBar(
            inputField = { field(true) },
            onExpandedChange = onExpandedChange,
            modifier = modifier,
            insideMargin = insideMargin,
            expanded = expanded,
            outsideEndAction = if (cancelText == null) {
                null
            } else {
                {
                    SearchBarActionButton(
                        text = cancelText,
                        onClick = onCancel,
                        containerColor = colors.backgroundColor,
                    )
                }
            },
            content = content,
        )
        return
    }

    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    val progress = remember { Animatable(if (expanded) 1f else 0f) }
    LaunchedEffect(expanded) {
        progress.animateTo(if (expanded) 1f else 0f, MetaBallSpring)
    }
    // The particles are a function of the measured sizes, so the measure pass publishes them and
    // the layer above the children draws them.
    var geometry by remember { mutableStateOf<SearchBarMetaBallGeometry?>(null) }
    // `COUIViewTouchAnimator` deforms the whole button view, so a press shrinks the capsule's
    // outline together with its label. The merged silhouette is painted outside the button, so the
    // press scale has to reach the particle as well as the content.
    val cancelMotionState = rememberCOUITouchMotionState()
    val squircleEnabled = isSquircleEnabled()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = insideMargin.width, vertical = insideMargin.height),
        ) {
            geometry?.let { measured ->
                SearchBarMetaBallLayer(
                    geometry = measured,
                    progress = { progress.value },
                    cancelMotionScale = cancelMotionState.scaleX,
                    colors = colors,
                    stroke = stroke,
                    glowColor = glowColor,
                    backdrop = backdrop,
                    continuousCurve = squircleEnabled,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Layout(
                content = {
                    // Before the first measure there is no silhouette to merge, so the field paints
                    // its own capsule for that one frame instead of leaving the bar blank.
                    field(geometry == null)
                    SearchBarActionButtonSurface(
                        onClick = onCancel,
                        modifier = Modifier.graphicsLayer {
                            val value = progress.value
                            val scale = searchBarCancelScale(value)
                            scaleX = scale
                            scaleY = scale
                            alpha = searchBarCancelAlpha(value)
                            val slide = geometry?.let { searchBarCancelTranslationX(it.cancelSlide, value) } ?: 0.dp
                            translationX = if (rtl) -slide.value else slide.value
                        },
                        // `setVisibility(8)` while collapsed: the capsule is still laid out so the
                        // melt ball has its silhouette, but it must not take taps from the field.
                        enabled = expanded,
                        motionState = cancelMotionState,
                        // The merged silhouette below carries the fill, the rim and the caustic.
                        containerColor = Color.Transparent,
                        glowColor = null,
                        stroke = null,
                    ) {
                        Box(
                            // `setFunctionalButtonTextAlpha(progress)`: the label keeps its own
                            // linear alpha on top of the view's remapped one.
                            modifier = Modifier.graphicsLayer { alpha = progress.value },
                        ) {
                            SearchBarActionLabel(
                                text = cancelText,
                                contentColor = SearchBarActionButtonDefaults.contentColor(),
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { measurables, constraints ->
                val totalWidth = constraints.maxWidth
                val cancelPlaceable = measurables[1].measure(Constraints())
                val gapPx = SearchBarActionButtonDefaults.StartGap.roundToPx()
                val restingWidth = (totalWidth - cancelPlaceable.width - gapPx).coerceAtLeast(0)
                // Deferred read: the field spans the drawn capsule, not the resting one, so a tap
                // anywhere on the visible silhouette reaches it.
                val capsuleWidth = searchBarCapsuleWidth(totalWidth.toDp(), restingWidth.toDp(), progress.value).roundToPx()
                val fieldPlaceable = measurables[0].measure(
                    Constraints(
                        minWidth = capsuleWidth,
                        maxWidth = capsuleWidth,
                        minHeight = constraints.minHeight,
                        maxHeight = constraints.maxHeight,
                    ),
                )
                val barHeight = maxOf(fieldPlaceable.height, cancelPlaceable.height)
                val measured = SearchBarMetaBallGeometry(
                    barWidth = totalWidth.toDp(),
                    barHeight = barHeight.toDp(),
                    restingCapsuleWidth = restingWidth.toDp(),
                    cancelWidth = cancelPlaceable.width.toDp(),
                    cancelHeight = cancelPlaceable.height.toDp(),
                    cancelGap = SearchBarActionButtonDefaults.StartGap,
                    cancelSlide = SearchBarDefaults.CancelSlidePx.toDp(),
                )
                if (measured != geometry) geometry = measured
                layout(totalWidth, barHeight) {
                    fieldPlaceable.placeRelative(0, (barHeight - fieldPlaceable.height) / 2)
                    cancelPlaceable.placeRelative(restingWidth + gapPx, (barHeight - cancelPlaceable.height) / 2)
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            content()
        }
    }

    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = expanded,
        onBackCompleted = {
            currentOnExpandedChange(false)
        },
    )
}

/**
 * Draws the merged silhouette of the search capsule and its cancel capsule behind the bar's
 * content.
 *
 * `COUISearchBar.setBallChain` hands the system's material chain two `OplusMaterialMetaBallParams`
 * with `extension` 4dp and factors 1.5 / 2.0, which is exactly [MetaBallDefaults]: the fold radius
 * is `extension * length(factor * u)`, so two capsules side by side bridge gaps below 6dp. The
 * native `coui_search_bar_functional_button_start_gap` is 12dp, so the resting capsules meet but do
 * not overlap; the bridge only shows while the button is still travelling in.
 *
 * The union is stroked with [searchBarMetaBallRim] rather than with the bottom bar's 2.5dp
 * `TYPE_FRAMEWORK_CAPSULE_3` rim: both chained views resolve `TYPE_FRAMEWORK_CAPSULE_8`, whose
 * `edgeWidthDp` is 1.8dp.
 */
@Composable
private fun SearchBarMetaBallLayer(
    geometry: SearchBarMetaBallGeometry,
    progress: () -> Float,
    cancelMotionScale: Float,
    colors: SearchBarColors,
    stroke: MaterialStrokeParams?,
    glowColor: Color?,
    backdrop: Backdrop?,
    continuousCurve: Boolean,
    modifier: Modifier = Modifier,
) {
    val value = progress()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val particles = remember(geometry, value, rtl, cancelMotionScale) {
        geometry.particles(value, rtl, cancelMotionScale)
    }
    val rim = searchBarMetaBallRim(stroke)
    val density = LocalDensity.current
    val blurScale = rememberToolbarAttenuatedBlurScale()
    MetaBallSurfaceWithRim(
        particles = particles,
        blurColors = if (backdrop == null) null else materialBarBlendColors(),
        // A merged chain draws the stroke once, along the union's outline.
        rimColor = rim.color,
        rimLineWidth = rim.lineWidth,
        rimLineAngle = rim.lineAngle,
        modifier = modifier,
        color = colors.backgroundColor,
        backdrop = backdrop,
        blurRadius = with(density) { (SearchBarDefaults.NATIVE_TOP_BAR_BLUR_SIGMA_PX / TEXTURE_BLUR_SIGMA_SCALE).toDp().value } * blurScale(),
        extension = MetaBallDefaults.Extension,
        extensionFactorX = MetaBallDefaults.EXTENSION_FACTOR_X,
        extensionFactorY = MetaBallDefaults.EXTENSION_FACTOR_Y,
        rimBottomScale = 1f,
        // `updateDropShadow(progress)` ramps the caustic with the edit progress and
        // `mNoShadowInNormalState` leaves the resting pill unlit, so a collapsed bar casts none.
        bottomGlowColor = glowColor?.let { it.copy(alpha = it.alpha * value) },
        continuousCurve = continuousCurve,
        // `COUISearchBar` strokes its capsule with the view-level `COUIMaterialStrokeEffect`
        // (`TYPE_FRAMEWORK_CAPSULE_8`), not with the metaball chain's own edge: the chain only
        // merges the two silhouettes. The two blends differ on dark material — see
        // `Modifier.nativeMaterialEdge`.
        plainRim = true,
    )
}

/**
 * The stroke of the merged search silhouette, resolved from the bar's own material instead of from
 * the bottom bar's preset.
 *
 * @property color the rim colour, or `null` when the caller paints the outline itself.
 * @property lineWidth the rim width in dp, `StrokeEffectParams.edgeWidthDp`.
 * @property lineAngle the rim angle in radians, `StrokeEffectParams.edgeAngle`.
 */
@Immutable
internal data class SearchBarMetaBallRim(
    val color: Color?,
    val lineWidth: Dp,
    val lineAngle: Float,
)

/**
 * `COUISearchBar` resolves the capsule's stroke through
 * `ToolbarMaterialEffectResolver.getCapsuleStrokeEffectTypeInWindow` — `TYPE_FRAMEWORK_CAPSULE_9`
 * for preset 1, `TYPE_FRAMEWORK_CAPSULE_8` otherwise (`ToolbarMaterialEffectResolver.java:32-33`;
 * the background view asks for it at `SearchBarBackgroundView.java:1282`, the cancel button at
 * `SearchBarFunctionButton.java:452`) — so both chained views wear the same stroke and the union
 * draws it once.
 *
 * `FRAMEWORK_CAPSULE_PARAMS_8_LIGHT` / `_DARK` (`COUIMaterialStrokeEffect.java:59` / `:96`) are
 * `(strokeType 1, edgeAlpha, edgeWidthDp, edgeAngle, shadowFadeIn, shadowFadeOut, shadowFadeScale)`
 * = `(1, 0.8f, 1.8f, 0f, 0.1f, 0f, 1f)` light and `(1, 0.15f, 1.8f, 0f, 0.01f, 0f, 1f)` dark. The
 * 1.8dp width is why the merged outline must not inherit the bottom bar's
 * `TYPE_FRAMEWORK_CAPSULE_3` 2.5dp (`:32` / `:70`); the alphas and the zero angle are already what
 * [SearchBarDefaults.searchBarStroke] carries.
 *
 * The preset's shadow fade-in / fade-out belong to the inner shadow `setShadowParams` paints inside
 * the capsule, which the merged silhouette does not draw — neither does it for the bottom bar — so
 * only the edge half of the tuple reaches the shader. A `null` [stroke] means the caller paints the
 * outline elsewhere; the width then only has to keep a valid value.
 */
internal fun searchBarMetaBallRim(stroke: MaterialStrokeParams?): SearchBarMetaBallRim = SearchBarMetaBallRim(
    color = stroke?.let { Color.White.copy(alpha = it.edgeAlpha) },
    lineWidth = stroke?.edgeWidth ?: META_BALL_RIM_LINE_WIDTH,
    lineAngle = stroke?.edgeAngle ?: 0f,
)

/**
 * The measured geometry of the edit-state transition, in the bar's content box.
 *
 * @param barWidth the full width the capsule occupies while collapsed.
 * @param barHeight the height of the taller of the two capsules.
 * @param restingCapsuleWidth the capsule's width in the edit state, `barWidth - cancelWidth - cancelGap`.
 * @param cancelWidth the cancel capsule's width.
 * @param cancelHeight the cancel capsule's height.
 * @param cancelGap the gap between the capsules at rest, `coui_search_bar_functional_button_start_gap`.
 * @param cancelSlide the button's starting offset, `mFunctionalButtonOffsetX`.
 */
@Immutable
internal data class SearchBarMetaBallGeometry(
    val barWidth: Dp,
    val barHeight: Dp,
    val restingCapsuleWidth: Dp,
    val cancelWidth: Dp,
    val cancelHeight: Dp,
    val cancelGap: Dp,
    val cancelSlide: Dp,
) {
    /**
     * The two silhouettes at edit progress [progress], with `true` mirroring them for RTL.
     *
     * [cancelPressScale] is the button's live touch scale: `COUIViewTouchAnimator` shrinks the whole
     * view on press, so the melted capsule has to shrink with it.
     */
    fun particles(
        progress: Float,
        rtl: Boolean = false,
        cancelPressScale: Float = 1f,
    ): List<MetaBallParticle> {
        val capsuleWidth = searchBarCapsuleWidth(barWidth, restingCapsuleWidth, progress)
        val scale = searchBarCancelScale(progress)
        val cancelCentre = restingCapsuleWidth + cancelGap + cancelWidth / 2 +
            searchBarCancelTranslationX(cancelSlide, progress)
        return listOf(
            MetaBallParticle(
                centerX = if (rtl) barWidth - capsuleWidth / 2 else capsuleWidth / 2,
                centerY = barHeight / 2,
                width = capsuleWidth,
                height = barHeight,
                cornerRadius = barHeight / 2,
            ),
            MetaBallParticle(
                centerX = if (rtl) barWidth - cancelCentre else cancelCentre,
                centerY = barHeight / 2,
                width = cancelWidth,
                height = cancelHeight,
                cornerRadius = cancelHeight / 2,
                scaleX = scale * cancelPressScale,
                scaleY = scale * cancelPressScale,
                alpha = progress,
            ),
        )
    }
}

/**
 * `SearchBarBackgroundView.doWidthChangedAnimate`: the drawn capsule runs from the full width to the
 * edit-state width over the transition progress.
 */
internal fun searchBarCapsuleWidth(barWidth: Dp, restingCapsuleWidth: Dp, progress: Float): Dp = barWidth + (restingCapsuleWidth - barWidth) * progress

/**
 * `COUISearchBar.e(...)`: `scale = functionalButtonInitScale + (1 - initScale) * progress`, with
 * `StateChangedToEditParamsHigh.functionalButtonInitScale` = 0.5.
 */
internal fun searchBarCancelScale(progress: Float): Float = SearchBarDefaults.CancelInitialScale +
    (1f - SearchBarDefaults.CancelInitialScale) * progress

/**
 * `COUISearchBar.updateFunctionalButtonAlpha`: `setAlpha(remapAlphaProgress(progress, 0.2f))`, where
 * `remapAlphaProgress(v, 0.2f)` is `v / 0.2` below the knee and 1 above it. The capsule is therefore
 * fully opaque once the spring passes 20% of its travel, while the label keeps its own linear alpha.
 */
internal fun searchBarCancelAlpha(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    return if (clamped < SEARCH_BAR_BUTTON_ALPHA_KNEE) clamped / SEARCH_BAR_BUTTON_ALPHA_KNEE else 1f
}

private const val SEARCH_BAR_BUTTON_ALPHA_KNEE = 0.2f

/** `COUISearchBar.e(...)`: `translationX = mFunctionalButtonOffsetX * (1 - progress)`. */
internal fun searchBarCancelTranslationX(cancelSlide: Dp, progress: Float): Dp = cancelSlide * (1f - progress)

/**
 * A text field to input a query in a search bar with COUI style.
 *
 * @param query the query text to be shown in the input field.
 * @param onQueryChange the callback to be invoked when the input service updates the query. An
 *   updated text comes as a parameter of the callback.
 * @param onSearch the callback to be invoked when the input service triggers the
 *   [ImeAction.Search] action. The current [query] comes as a parameter of the callback.
 * @param expanded whether the search bar is expanded and showing search results.
 * @param onExpandedChange the callback to be invoked when the search bar's expanded state is
 *   changed.
 * @param modifier the [Modifier] to be applied to this input field.
 * @param label the hint shown while [query] is empty. The device keeps the hint behind the cursor
 *   in the expanded search bar until the first character is typed, so it is not a collapsed-only
 *   label.
 * @param enabled the enabled state of this input field. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param colors [SearchBarColors] that will be used to resolve the colors used for this input
 *   field in different states. See [SearchBarDefaults.searchBarColors].
 * @param stroke the material rim and inner shadow drawn over the capsule, or `null` for a flat
 *   fill. Defaults to [SearchBarDefaults.searchBarStroke]. Pass `null` when the caller paints the
 *   edge on a wider capsule that this field only occupies part of.
 * @param leadingIcon the leading icon to be displayed at the start of the input field.
 * @param trailingIcon the trailing icon to be displayed at the end of the input field.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this input field. You can use this to change the search bar's
 *   appearance or preview the search bar in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param glowColor the caustic light cast below the capsule, or `null` to disable it. COUI paints
 *   it through `COUIMaterialEffect.setOutlineCausticShadowColor` with `coui_caustic_color_1`, but
 *   only when the system material feature reports it on: `isCausticShadowEnable()` is
 *   `mSystemCausticShadowState && ...`, and the only writer of that flag is
 *   `onMaterialStateChange(8, enabled)`. Both reference devices report it off, and neither the
 *   expanded search bar nor the settings home pill shows a light band under the capsule on either
 *   theme, so the default is `null`; pass [SearchBarDefaults.causticGlowColor] to opt in.
 *   The light is clipped to the outside of the capsule, so it never brightens the translucent fill.
 * @param backdrop the backdrop blurred under the capsule, or `null` for a flat fill. `COUISearchBar`
 *   blurs nothing by default because the app bar under it already does; pass a backdrop for a field
 *   that floats over content, the blurred preset (`TYPE_FRAMEWORK_TOP_BAR_BLUR`), or a list scrolled
 *   under it shows straight through the translucent fill.
 * @param clearContentDescription the accessibility label of the default clear button,
 *   `R.string.coui_search_clear_button_description` on the device. Pass a localized copy.
 *
 * Submitting a search from the keyboard hides the keyboard and keeps [query]; collapsing the bar
 * (cancel or back) is what clears it.
 */
@Composable
fun InputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    enabled: Boolean = true,
    textStyle: TextStyle? = null,
    colors: SearchBarColors = SearchBarDefaults.searchBarColors(),
    stroke: MaterialStrokeParams? = SearchBarDefaults.searchBarStroke(),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    glowColor: Color? = null,
    backdrop: Backdrop? = null,
    clearContentDescription: String = SearchBarDefaults.ClearContentDescription,
) {
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)
    val currentOnSearch by rememberUpdatedState(onSearch)
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    // COUISearchBarDrawingProxyDrawable.setBackgroundRect: corner radius = rect.height() / 2.
    // The capsule follows the ColorOS system capsule curve (arc + 1.5R easing tail).
    val squircleEnabled = isSquircleEnabled()
    val capsuleShape = remember(squircleEnabled) {
        CapsuleOutlineShape(squircleEnabled = squircleEnabled)
    }

    val actualLeadingIcon = leadingIcon ?: {
        // coui_search_view_icon: 20dp glyph filled with couiColorSecondNeutral.
        Icon(
            modifier = Modifier.padding(start = SearchBarDefaults.LeadingIconStartPadding, end = SearchBarDefaults.LeadingIconEndPadding),
            imageVector = COUIIcons.Basic.Search,
            tint = colors.iconColor,
            contentDescription = "Search",
        )
    }

    val actualTrailingIcon = trailingIcon ?: {
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            // Clear button: a 36dp circular touch target holding an 18dp filled circle with a
            // white cross (ic_edit_text_delete_search_view).
            val clearCircleColor = colors.clearIconColor
            Box(
                modifier = Modifier
                    .padding(start = SearchBarDefaults.TrailingIconStartPadding, end = SearchBarDefaults.TrailingIconEndPadding)
                    .size(SearchBarDefaults.TrailingIconTouchTargetSize)
                    .clip(CircleShape)
                    .clickable { currentOnQueryChange("") }
                    .semantics { contentDescription = clearContentDescription },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(SearchBarDefaults.TrailingIconVisualSize)) {
                    drawCircle(color = clearCircleColor)
                    // ic_edit_text_delete_search_view cross: 8.2dp span, 1.6dp stroke, always white.
                    val half = 4.1.dp.toPx()
                    val stroke = 1.6.dp.toPx()
                    drawLine(
                        color = Color.White,
                        start = Offset(center.x - half, center.y - half),
                        end = Offset(center.x + half, center.y + half),
                        strokeWidth = stroke,
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(center.x - half, center.y + half),
                        end = Offset(center.x + half, center.y - half),
                        strokeWidth = stroke,
                    )
                }
            }
        }
    }

    val focused = internalInteractionSource.collectIsFocusedAsState().value
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val textAlpha = remember { Animatable(1f) }
    val density = LocalDensity.current
    val blurScale = rememberToolbarAttenuatedBlurScale()
    // A search submitted from the keyboard drops focus, so collapse must not depend on focus to
    // clear the query; it only skips the initial composition.
    var hasBeenExpanded by remember { mutableStateOf(expanded) }

    val textColor = LocalContentColor.current
    // COUI input text: 16sp regular (couiEditTextTextAppearance).
    val inputTextStyle = COUITheme.textStyles.main
        .copy(fontSize = SearchBarDefaults.InputFieldFontSize)
        .merge(textStyle)
        .copy(color = textColor)

    val cursorBrush = SolidColor(COUITheme.colorScheme.primary)
    // The hint is an EditText hint, not a collapsed-only label: the device's expanded search bar
    // still reads "搜索设置项" behind the cursor until the first character is typed.
    val labelText by remember(query, label) {
        derivedStateOf { if (query.isEmpty()) label else "" }
    }

    // On API 26-27, focus is incorrectly reassigned after clearFocus(), preventing the
    // SearchBar from closing. Workaround: disable the TextField when collapsed and use
    // pointerInput to handle tap-to-expand. https://issuetracker.google.com/issues/433382598
    val workaroundEnabled = !hasFocusReassignBug || expanded
    val expandOnTapModifier = if (workaroundEnabled || !enabled) {
        Modifier
    } else {
        Modifier.pointerInput(Unit) { detectTapGestures { currentOnExpandedChange(true) } }
    }

    BasicTextField(
        value = query,
        onValueChange = currentOnQueryChange,
        modifier = modifier
            .then(expandOnTapModifier)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) currentOnExpandedChange(true) }
            .semantics {
                onClick {
                    focusRequester.requestFocus()
                    true
                }
            },
        enabled = enabled && workaroundEnabled,
        singleLine = true,
        textStyle = inputTextStyle,
        cursorBrush = cursorBrush,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                // COUISearchBar: submitting only dismisses the keyboard; the filter stays.
                focusManager.clearFocus()
                currentOnSearch(query)
            },
        ),
        interactionSource = internalInteractionSource,
        decorationBox = { innerTextField ->
            // COUISearchBar capsule on the material path: the toolbar material layer
            // (COUIMaterialBlurEffect top bar), not coui_search_bar_no_blur_solid. The material
            // edge is COUISearchBar's COUIShadowEdgeDrawable pass, painted over the fill.
            val edgeModifier = if (stroke != null) {
                Modifier.materialShadowEdge(stroke = stroke)
            } else {
                Modifier
            }
            val fillModifier = if (backdrop != null) {
                Modifier.couiGlassSurface(
                    backdrop = backdrop,
                    shape = capsuleShape,
                    containerColor = colors.backgroundColor,
                    fallback = Modifier.background(color = colors.backgroundColor, shape = capsuleShape),
                    blurRadius = with(density) { (SearchBarDefaults.NATIVE_TOP_BAR_BLUR_SIGMA_PX / TEXTURE_BLUR_SIGMA_SCALE).toDp().value } * blurScale(),
                    // `FRAMEWORK_TOP_BAR_PARAMS_*`: two tint layers over the backdrop, no blur.
                    blendColors = materialBarBlendColors(),
                )
            } else {
                Modifier.background(color = colors.backgroundColor, shape = capsuleShape)
            }
            Box {
                if (glowColor != null) {
                    // COUISearchBar's outline caustic is an outside-only light: the capsule is
                    // punched out of the glow layer, so the translucent fill stays its own colour.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawWithContent {
                                val capsule = Path().apply {
                                    val outline = capsuleShape.createOutline(size, layoutDirection, this@drawWithContent)
                                    addOutline(outline)
                                }
                                clipPath(capsule, ClipOp.Difference) { this@drawWithContent.drawContent() }
                            },
                    ) {
                        SearchBarCausticGlow(
                            color = glowColor,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .then(fillModifier)
                        .then(edgeModifier),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        actualLeadingIcon()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = SearchBarDefaults.InputFieldMinHeight),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            val mergedLabelStyle = remember(textStyle) {
                                TextStyle(fontSize = SearchBarDefaults.InputFieldFontSize).merge(textStyle)
                            }
                            // Hint text: couiColorLabelSecondary (Widget.COUI.EditText textColorHint).
                            Text(
                                text = labelText,
                                style = mergedLabelStyle,
                                color = colors.labelColor,
                            )
                            Box(modifier = Modifier.graphicsLayer { alpha = textAlpha.value }) {
                                innerTextField()
                            }
                        }
                        actualTrailingIcon()
                    }
                }
            }
        },
    )

    LaunchedEffect(expanded) {
        if (expanded) {
            // Explicitly request focus when expanded. On API 26-27, the workaround disables
            // the TextField when collapsed, so the initial tap doesn't grant focus — this
            // ensures the keyboard appears after the TextField becomes enabled again.
            focusRequester.requestFocus()
            hasBeenExpanded = true
        } else if (hasBeenExpanded) {
            delay(100)
            if (query.isNotEmpty()) {
                textAlpha.animateTo(0f)
                currentOnQueryChange("")
                textAlpha.snapTo(1f)
            }
            if (focused) focusManager.clearFocus()
        }
    }
}

/** Binary-compatibility bridge for the [InputField] overload that predates `clearContentDescription`. */
@Deprecated(
    message = "Migrated to the overload that adds clearContentDescription.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun InputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    enabled: Boolean = true,
    textStyle: TextStyle? = null,
    colors: SearchBarColors = SearchBarDefaults.searchBarColors(),
    stroke: MaterialStrokeParams? = SearchBarDefaults.searchBarStroke(),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    glowColor: Color? = null,
    backdrop: Backdrop? = null,
) {
    InputField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        label = label,
        enabled = enabled,
        textStyle = textStyle,
        colors = colors,
        stroke = stroke,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        interactionSource = interactionSource,
        glowColor = glowColor,
        backdrop = backdrop,
    )
}

/** Binary-compatibility bridge for the [InputField] overload that predates `backdrop`. */
@Deprecated(
    message = "Migrated to the overload that adds backdrop.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun InputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    enabled: Boolean = true,
    textStyle: TextStyle? = null,
    colors: SearchBarColors = SearchBarDefaults.searchBarColors(),
    stroke: MaterialStrokeParams? = SearchBarDefaults.searchBarStroke(),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    glowColor: Color? = SearchBarDefaults.causticGlowColor(),
) {
    InputField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        label = label,
        enabled = enabled,
        textStyle = textStyle,
        colors = colors,
        stroke = stroke,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        interactionSource = interactionSource,
        glowColor = glowColor,
        backdrop = null,
    )
}

/**
 * The caustic light `COUISearchBar.setCausticShadow` casts under the capsule. The native level-2
 * layout is `coui_caustic_layout_b_level2` (4.899994dp) and `coui_caustic_layout_sdf_level2`
 * (9.22998dp), which is exactly [CausticShadowProfile.Toolbar].
 *
 * Measured on the device, the band reaches +18 grey levels right under the capsule and is gone by
 * 4.4dp; the raw `coui_caustic_color_1` renders +20 / 4.6dp here, which is the same light within
 * raster tolerance.
 */
@Composable
internal fun SearchBarCausticGlow(color: Color, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        MaterialCausticShadow(
            particles = listOf(
                CausticShadowParticle(
                    MetaBallParticle(
                        centerX = maxWidth / 2,
                        centerY = maxHeight / 2,
                        width = maxWidth,
                        height = maxHeight,
                        cornerRadius = maxHeight / 2,
                    ),
                    CausticShadowProfile.Toolbar,
                ),
            ),
            color = color,
            modifier = Modifier.matchParentSize(),
        )
    }
}

/**
 * `COUIMaterialBlurEffect` `FRAMEWORK_TOP_BAR_PARAMS_DARK`, the preset `COUISearchBar` resolves for
 * the toolbar material: layer 1 `#CC262626` over the backdrop and layer 2 `#B37F7F7F` blended with
 * `OVERLAY`. `FRAMEWORK_TOP_BAR_PARAMS_LIGHT` is `#66333333` / `COLOR_DODGE` and `#99F5F5F5` /
 * `LUMINOSITY`.
 *
 * Both presets carry a `blurRadius` of 0 (`ToolbarMaterialEffectResolver` maps preset 0 to
 * `TYPE_FRAMEWORK_TOP_BAR`) — the capsule is a two-layer tint over the sharp backdrop, not a blurred
 * surface. `FRAMEWORK_TOP_BAR_BLUR_PARAMS` (radius 20) is only used when the bar is set to blur.
 */
private val MaterialBarDark = Color(0xCC262626)
private val MaterialBarDarkHighlight = Color(0xB37F7F7F)
private val MaterialBarLight = Color(0x66333333)
private val MaterialBarLightHighlight = Color(0x99F5F5F5)

/** The two top-bar tint layers of the search capsule, blended the way the framework blends them. */
@Composable
private fun materialBarBlendColors(): List<BlendColorEntry> = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
    listOf(
        BlendColorEntry(color = MaterialBarDark, mode = BlurBlendMode.SrcOver),
        BlendColorEntry(color = MaterialBarDarkHighlight, mode = BlurBlendMode.Overlay),
    )
} else {
    listOf(
        BlendColorEntry(color = MaterialBarLight, mode = BlurBlendMode.ColorDodge),
        BlendColorEntry(color = MaterialBarLightHighlight, mode = BlurBlendMode.Luminosity),
    )
}

/**
 * The single fill the capsule keeps when the material layers cannot be applied.
 *
 * `COUISearchBar` asks `ToolbarMaterialEffectResolver` for the toolbar background, which returns the
 * `COUIMaterialBlurEffect` top-bar layers (see [materialBarBlendColors]); this collapses them into
 * one translucent colour. It is not `coui_search_bar_no_blur_solid` (`#28767676` at night), which
 * reads 12 levels brighter than the page where the system capsule reads 7 levels darker.
 */
@Composable
private fun materialBarColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
    // `FRAMEWORK_TOP_BAR_PARAMS_DARK` layer 1. The preset's second layer is `#B37F7F7F` with
    // `OVERLAY`, which maps a mid-grey blend colour onto the backdrop unchanged, so this single
    // `SrcOver` layer is the whole dark fill. It is translucent like the system material: over the
    // black settings home it lands on 30, over the `#2E2E2E` page on 40 (measured 39 on the device).
    MaterialBarDark
} else {
    // `FRAMEWORK_TOP_BAR_PARAMS_LIGHT` is `#66333333` / `COLOR_DODGE` over the backdrop and then
    // `#99F5F5F5` / `LUMINOSITY`, which lands a light page on a flat near-white: measured 247 over
    // the settings page's 235, i.e. 60% white. A 40% white reproduced only 242 over the same page.
    Color(0x99FFFFFF)
}

/** Contains default values used by [SearchBar] and [InputField]. */
object SearchBarDefaults {
    /**
     * The blur sigma of `FRAMEWORK_TOP_BAR_BLUR_PARAMS`, in pixels: the preset `COUISearchBar` uses
     * when it is set to blur (material type 2). Its default preset blurs nothing.
     */
    const val NATIVE_BLUR_SIGMA_PX = 20f

    /**
     * The blur sigma of `FRAMEWORK_TOP_BAR_PARAMS`, the preset `COUISearchBar` resolves by default
     * (`ToolbarMaterialEffectResolver` maps preset 0 to `TYPE_FRAMEWORK_TOP_BAR`).
     *
     * The preset's `blurRadius` is 0: the capsule is the preset's two colour layers over a sharp
     * backdrop, which is why a `SearchBar` with a backdrop shows the content behind it unblurred.
     */
    const val NATIVE_TOP_BAR_BLUR_SIGMA_PX = 0f

    /**
     * The default inside margin of the [SearchBar].
     * COUI `coui_search_bar_responsive_horizontal_padding_compat` (16dp) x (52dp - 40dp) / 2.
     */
    val InsideMargin = DpSize(16.dp, 6.dp)

    /**
     * The default minimum height of the [InputField] background capsule.
     * COUI `coui_search_bar_normal_background_height` (40dp).
     */
    val InputFieldMinHeight = 40.dp

    /**
     * The default font size for the [InputField] text and label.
     * COUI `coui_search_view_input_text_size` (15sp).
     */
    val InputFieldFontSize = 15.sp

    /**
     * The start padding for the default leading icon: the COUI 4dp icon container start gap plus
     * (36dp container - 20dp glyph) / 2.
     */
    val LeadingIconStartPadding = 12.dp

    /**
     * The end padding for the default leading icon: (36dp container - 20dp glyph) / 2, equal to
     * COUI `coui_search_view_hint_padding_start`.
     */
    val LeadingIconEndPadding = 8.dp

    /**
     * The start padding for the default trailing (clear) button touch target.
     * COUI `coui_search_bar_edittext_end_gap` (4dp).
     */
    val TrailingIconStartPadding = 4.dp

    /**
     * The end padding for the default trailing (clear) button touch target.
     * COUI `coui_search_bar_non_instant_search_inner_gap` (4dp).
     */
    val TrailingIconEndPadding = 4.dp

    /**
     * The circular touch target size of the default trailing (clear) button.
     * COUI `coui_search_bar_inner_search_icon_size` (36dp).
     */
    val TrailingIconTouchTargetSize = 36.dp

    /**
     * The visual circle size of the default trailing (clear) button, matching the 9dp-radius
     * circle of ic_edit_text_delete_search_view.
     */
    val TrailingIconVisualSize = 18.dp

    /**
     * Creates a [SearchBarColors] with the default colors of the [InputField], following
     * COUISearchBar on the material path (the toolbar material layer capsule, couiColorLabelSecondary
     * hint, couiColorSecondNeutral glyph, couiColorHintNeutral clear circle).
     */
    @Composable
    fun searchBarColors(
        backgroundColor: Color = materialBarColor(),
        labelColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
        iconColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
        clearIconColor: Color = COUITheme.colorScheme.onSurfaceContainerHigh,
    ): SearchBarColors = remember(backgroundColor, labelColor, iconColor, clearIconColor) {
        SearchBarColors(
            backgroundColor = backgroundColor,
            labelColor = labelColor,
            iconColor = iconColor,
            clearIconColor = clearIconColor,
        )
    }

    /**
     * The accessibility label of the default clear button when the caller passes none.
     *
     * `SearchBarBackgroundView.ensureQuickDeleteButton` labels the system button with
     * `R.string.coui_search_clear_button_description`, which is localized per device language. A
     * multiplatform library ships no string resources, so [InputField] takes the copy as a
     * parameter; this value only keeps the previous English default.
     */
    @Suppress("ktlint:standard:property-naming")
    const val ClearContentDescription = "Search Cleanup"

    /**
     * The edge ColorOS draws on the search capsule: `TYPE_FRAMEWORK_CAPSULE_8`. On the material path
     * `COUISearchBar` resolves the rim through `getCapsuleStrokeEffectTypeInWindow`, which returns the
     * in-window capsule (`FRAMEWORK_CAPSULE_PARAMS_8_DARK`, edge alpha 0.15) instead of
     * `TYPE_FRAMEWORK_CAPSULE_1`; the capsule-1 rim (0.6) measures 177 on the device where the
     * settings capsule measures 78.
     */
    @Composable
    fun searchBarStroke(): MaterialStrokeParams = MaterialStrokeParams.frameworkCapsule8(COUITheme.colorScheme.background.luminance() < 0.5f)

    /**
     * The caustic light under the capsule, COUI `coui_caustic_color_1`: `#2EFFFFFF` in the light
     * theme and `#33FFFFFF` at night. `COUISearchBar.setCausticShadow` passes it to
     * `COUIMaterialEffect.setOutlineCausticShadowColor`.
     *
     * The system only reaches that call when `isCausticShadowEnable()` is true, and the flag behind
     * it (`mSystemCausticShadowState`) starts `false` and is only written by
     * `onMaterialStateChange(8, enabled)`. Both reference devices report it off: neither the
     * expanded search bar nor the settings home pill shows a light band under the capsule in either
     * theme (`#262626` capsule over a black page reads 30 under the bottom rim, with no lift below
     * it). [SearchBar] therefore takes `null` by default; pass this colour to opt in.
     */
    @Composable
    fun causticGlowColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
        Color(0x33FFFFFF)
    } else {
        Color(0x2EFFFFFF)
    }

    /**
     * `COUISearchBar`'s edit-state spring: `StateChangedToEditParamsHigh.getResponse()` and
     * `getBounce()`. The capsule width, the cancel capsule's scale and its offset all ride it, and
     * `StateChangedToNormalParamsHigh` carries the same pair for the collapse.
     *
     * The four `const val`s here keep the object's existing naming (`InsideMargin`,
     * `InputFieldMinHeight` and friends) rather than SCREAMING_SNAKE_CASE: renaming a published
     * `const val` removes its recorded API field, so they are suppressed instead.
     */
    @Suppress("ktlint:standard:property-naming")
    const val MetaBallSpringResponse = 0.5f

    /** See [MetaBallSpringResponse]; `COUISpringForce.setBounce` sets `dampingRatio = 1 - bounce`. */
    @Suppress("ktlint:standard:property-naming")
    const val MetaBallSpringBounce = 0.25f

    /**
     * `StateChangedToEditParamsHigh.getFunctionalButtonInitScale()`: the cancel capsule grows from
     * half size while the bar expands.
     */
    @Suppress("ktlint:standard:property-naming")
    const val CancelInitialScale = 0.5f

    /**
     * `COUISearchBar.mFunctionalButtonOffsetX`: `handleStateChangedAnimation`
     * (`COUISearchBar.java:972`, LTR branch) lays the background view out
     * `max(maxWidth, minWidth) + left + 20` wide, and the offset is that view's right edge minus
     * the button's (`:988-991`).
     *
     * The 20 is a **raw pixel** count, not dp: the native value goes straight to `View.layout`
     * without `applyDimension`, so on the reference 3.875× device it is 5.2dp while at 1× it is
     * 20dp. It is kept as-is to stay identical to the system rather than normalised by density.
     */
    @Suppress("ktlint:standard:property-naming")
    const val CancelSlidePx = 20f

    /**
     * Whether the cancel capsule may merge with the field, `COUISearchBar.setBallChain`'s
     * `mUseMaterialEffectAttr != 0 && isSupportBlur() && isBlurEnable()`. [isBlurEnabled] is this
     * library's equivalent: the local blur switch, the platform's, and a runtime shader to draw the
     * union. A bar without it draws two separate capsules.
     */
    @Composable
    fun metaBallFusion(): Boolean = isBlurEnabled()
}

/**
 * Colors used by the [InputField] of a [SearchBar].
 *
 * @param backgroundColor the color of the capsule background.
 * @param labelColor the color of the label (hint) text.
 * @param iconColor the tint of the default leading search icon.
 * @param clearIconColor the fill color of the default clear button circle.
 */
@Immutable
data class SearchBarColors(
    val backgroundColor: Color,
    val labelColor: Color,
    val iconColor: Color,
    val clearIconColor: Color,
)

/** COUIMoveEaseInterpolator: PathInterpolator(0.3f, 0f, 0.1f, 1f). */
private val MoveEase = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

/**
 * `COUISearchBar`'s edit-state spring. `COUISpringForce.setResponse(r)` sets the natural frequency
 * to `2π / r` and `setBounce(b)` sets `dampingRatio = 1 - b`; Compose's `spring` derives the same
 * frequency from `sqrt(stiffness)`.
 */
private val MetaBallSpring = spring<Float>(
    dampingRatio = 1f - SearchBarDefaults.MetaBallSpringBounce,
    stiffness = (2f * PI.toFloat() / SearchBarDefaults.MetaBallSpringResponse).let { it * it },
)

/** COUISearchBar DEFAULT_SEARCH_VIEW_OFFSET/SCALE_CHANGE_DURATION (450ms). */
private val BackgroundResizeDurationMillis = 450

/** COUISearchBar DEFAULT_SEARCH_VIEW_BUTTON_OFFSET_DURATION (400ms). */
private val ActionOffsetDurationMillis = 400

/** COUISearchBar DEFAULT_BUTTON_ALPHA_CHANGE_DURATION (350ms). */
private val ActionAlphaDurationMillis = 350

/** COUI `coui_search_bar_functional_button_offset_distance` (60dp). */
private val ActionSlideDistance = 60.dp

/**
 * Extra start padding for the outside end action: the COUI 20dp visual gap from the capsule
 * (8dp button start gap + 12dp ripple text padding) minus the 16dp input field margin.
 */
private val ActionExtraStartPadding = 4.dp

/**
 * End padding for the outside end action: `coui_search_bar_functional_button_end_gap_compat`
 * (4dp) + 12dp ripple text padding.
 */
private val ActionEndPadding = 16.dp
