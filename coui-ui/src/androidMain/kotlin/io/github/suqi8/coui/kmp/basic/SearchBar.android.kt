// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.kit.backdrop.CouiBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.LocalCouiBackdrop
import io.github.suqi8.coui.kmp.kit.search.CouiSearchBarColors
import io.github.suqi8.coui.kmp.kit.search.rememberCouiSearchBarState
import io.github.suqi8.coui.kmp.kit.topappbar.LocalCouiToolbarBackdrop
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalContentColor
import io.github.suqi8.coui.kmp.utils.hasFocusReassignBug
import kotlinx.coroutines.delay
import io.github.suqi8.coui.kmp.kit.search.SearchBar as KitSearchBar

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
    val density = LocalDensity.current
    val dark = COUITheme.colorScheme.background.luminance() < 0.5f
    val effectiveBackdrop = backdrop ?: LocalBackdrop.current
    val ambientCouiBackdrop = LocalCouiToolbarBackdrop.current ?: LocalCouiBackdrop.current
    val resolvedBackdrop = ambientCouiBackdrop
        ?: effectiveBackdrop?.let { remember(it, density) { CouiBackdrop(it, density) } }
    resolvedBackdrop?.RetainWhileComposed()

    val kitColors = CouiSearchBarColors(
        foreground = if (textStyle?.color != null && textStyle.color != Color.Unspecified) {
            textStyle.color
        } else {
            COUITheme.colorScheme.onSurface
        },
        secondary = colors.labelColor,
        fallbackBackground = colors.backgroundColor,
        clear = colors.clearIconColor,
        icon = colors.iconColor,
    )
    val state = rememberCouiSearchBarState(expanded)
    LaunchedEffect(expanded) {
        state.updateTarget(expanded)
    }

    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val currentOnCancel by rememberUpdatedState(onCancel)
    val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)

    Column(modifier = modifier) {
        DisabledPassThrough(enabled) {
            KitSearchBar(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                query = query,
                onQueryChange = onQueryChange,
                onExpandedChange = { isExpanded ->
                    if (!isExpanded && expanded) {
                        currentOnCancel()
                    } else {
                        currentOnExpandedChange(isExpanded)
                    }
                },
                placeholder = label,
                cancelText = cancelText,
                clearDescription = clearContentDescription,
                dark = dark,
                backdrop = resolvedBackdrop,
                onSearch = { onSearch(query) },
                colors = kitColors,
                enabled = enabled,
                textStyle = textStyle,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                interactionSource = interactionSource,
                horizontalMargin = insideMargin.width,
                verticalMargin = insideMargin.height,
            )
        }

        AnimatedVisibility(visible = expanded) {
            content()
        }
    }

    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = expanded,
        onBackCompleted = {
            currentOnCancel()
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

/**
 * A disabled field stays inert: the kit's text field still hit-tests while disabled, which kept taps
 * from reaching a clickable container (a search pill that only opens a search page). An empty
 * pointer node above it takes the hit without consuming it, so ancestors still see the gesture.
 */
@Composable
private fun DisabledPassThrough(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        content()
        if (!enabled) {
            Box(Modifier.matchParentSize().pointerInput(Unit) {})
        }
    }
}
