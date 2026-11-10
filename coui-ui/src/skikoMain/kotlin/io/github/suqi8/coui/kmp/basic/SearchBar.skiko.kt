// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalContentColor
import io.github.suqi8.coui.kmp.utils.hasFocusReassignBug
import kotlinx.coroutines.delay
import kotlin.math.PI

@Composable
internal actual fun SearchBarImpl(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier,
    expanded: Boolean,
    label: String,
    enabled: Boolean,
    textStyle: TextStyle?,
    colors: SearchBarColors,
    stroke: MaterialStrokeParams?,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    interactionSource: MutableInteractionSource?,
    glowColor: Color?,
    backdrop: Backdrop?,
    insideMargin: DpSize,
    cancelText: String?,
    onCancel: () -> Unit,
    metaBallFusion: Boolean,
    clearContentDescription: String,
    content: @Composable ColumnScope.() -> Unit,
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
    var geometry by remember { mutableStateOf<SearchBarMetaBallGeometry?>(null) }
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
                        enabled = expanded,
                        motionState = cancelMotionState,
                        containerColor = Color.Transparent,
                        glowColor = null,
                        stroke = null,
                    ) {
                        Box(
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

@Composable
internal actual fun InputFieldImpl(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier,
    label: String,
    enabled: Boolean,
    textStyle: TextStyle?,
    colors: SearchBarColors,
    stroke: MaterialStrokeParams?,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    interactionSource: MutableInteractionSource?,
    glowColor: Color?,
    backdrop: Backdrop?,
    clearContentDescription: String,
) {
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)
    val currentOnSearch by rememberUpdatedState(onSearch)
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val squircleEnabled = isSquircleEnabled()
    val capsuleShape = remember(squircleEnabled) {
        CapsuleOutlineShape(squircleEnabled = squircleEnabled)
    }

    val actualLeadingIcon = leadingIcon ?: {
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
                    val half = 4.1.dp.toPx()
                    val strokeWidth = 1.6.dp.toPx()
                    drawLine(
                        color = Color.White,
                        start = Offset(center.x - half, center.y - half),
                        end = Offset(center.x + half, center.y + half),
                        strokeWidth = strokeWidth,
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(center.x - half, center.y + half),
                        end = Offset(center.x + half, center.y - half),
                        strokeWidth = strokeWidth,
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
    var hasBeenExpanded by remember { mutableStateOf(expanded) }

    val textColor = LocalContentColor.current
    val inputTextStyle = COUITheme.textStyles.main
        .copy(fontSize = SearchBarDefaults.InputFieldFontSize)
        .merge(textStyle)
        .copy(color = textColor)

    val cursorBrush = SolidColor(COUITheme.colorScheme.primary)
    val labelText by remember(query, label) {
        derivedStateOf { if (query.isEmpty()) label else "" }
    }

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
                focusManager.clearFocus()
                currentOnSearch(query)
            },
        ),
        interactionSource = internalInteractionSource,
        decorationBox = { innerTextField ->
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
                    blendColors = materialBarBlendColors(),
                )
            } else {
                Modifier.background(color = colors.backgroundColor, shape = capsuleShape)
            }
            Box {
                if (glowColor != null) {
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
        bottomGlowColor = glowColor?.let { it.copy(alpha = it.alpha * value) },
        continuousCurve = continuousCurve,
        plainRim = true,
    )
}

@Immutable
internal data class SearchBarMetaBallRim(
    val color: Color?,
    val lineWidth: Dp,
    val lineAngle: Float,
)

internal fun searchBarMetaBallRim(stroke: MaterialStrokeParams?): SearchBarMetaBallRim = SearchBarMetaBallRim(
    color = stroke?.let { Color.White.copy(alpha = it.edgeAlpha) },
    lineWidth = stroke?.edgeWidth ?: META_BALL_RIM_LINE_WIDTH,
    lineAngle = stroke?.edgeAngle ?: 0f,
)

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

internal fun searchBarCapsuleWidth(barWidth: Dp, restingCapsuleWidth: Dp, progress: Float): Dp = barWidth + (restingCapsuleWidth - barWidth) * progress

internal fun searchBarCancelScale(progress: Float): Float = SearchBarDefaults.CancelInitialScale + (1f - SearchBarDefaults.CancelInitialScale) * progress

internal fun searchBarCancelAlpha(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    return if (clamped < SEARCH_BAR_BUTTON_ALPHA_KNEE) clamped / SEARCH_BAR_BUTTON_ALPHA_KNEE else 1f
}

private const val SEARCH_BAR_BUTTON_ALPHA_KNEE = 0.2f

internal fun searchBarCancelTranslationX(cancelSlide: Dp, progress: Float): Dp = cancelSlide * (1f - progress)

private val MaterialBarDark = Color(0xCC262626)
private val MaterialBarDarkHighlight = Color(0xB37F7F7F)
private val MaterialBarLight = Color(0x66333333)
private val MaterialBarLightHighlight = Color(0x99F5F5F5)

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

@Composable
private fun materialBarColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
    MaterialBarDark
} else {
    Color(0x99FFFFFF)
}

private val MoveEase = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

private val MetaBallSpring = spring<Float>(
    dampingRatio = 1f - SearchBarDefaults.MetaBallSpringBounce,
    stiffness = (2f * PI.toFloat() / SearchBarDefaults.MetaBallSpringResponse).let { it * it },
)

private val BackgroundResizeDurationMillis = 450
private val ActionOffsetDurationMillis = 400
private val ActionAlphaDurationMillis = 350
private val ActionSlideDistance = 60.dp
private val ActionExtraStartPadding = 4.dp
private val ActionEndPadding = 16.dp
