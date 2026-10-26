// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.captionBarPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.suqi8.coui.kmp.basic.GradientStrokeStyle
import io.github.suqi8.coui.kmp.basic.InnerShadowStyle
import io.github.suqi8.coui.kmp.basic.MaterialShadowEdgeDefaults
import io.github.suqi8.coui.kmp.basic.MaterialStrokeParams
import io.github.suqi8.coui.kmp.basic.PopupBarrierBoundary
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.materialShadowEdge
import io.github.suqi8.coui.kmp.overlay.OverlayDialog
import io.github.suqi8.coui.kmp.squircle.squircleSurface
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalDismissState
import io.github.suqi8.coui.kmp.window.WindowDialog
import kotlinx.coroutines.launch

/**
 * Internal shared layout logic for [OverlayDialog] and [WindowDialog].
 *
 * @param show Whether the dialog is currently shown.
 * @param titleColor The color of the title.
 * @param summaryColor The color of the summary.
 * @param backgroundColor The background color of the dialog.
 * @param outsideMargin The margin outside the dialog.
 * @param insideMargin The margin for the built-in texts: width is the horizontal padding of
 *   the title and summary, height is the top padding above the title. The [content] slot is
 *   left unpadded so COUI-style button bars can span the full panel width.
 * @param popupHost A composable that provides the dialog container (e.g., DialogLayout or Dialog).
 *   It receives the visibility state and the inner content composable.
 * @param modifier The modifier to be applied to the dialog content.
 * @param title The title of the dialog.
 * @param summary The summary of the dialog.
 * @param enableWindowDim Whether to enable window dimming.
 * @param onDismissRequest The callback when the dialog is dismissed.
 * @param onDismissFinished Invoked when the hide animation completes; not invoked if the hide
 *   is cancelled mid-flight (e.g., by [show] toggling back to true).
 * @param defaultWindowInsetsPadding Whether to apply default window insets padding.
 * @param forceCentered When true, the dialog is centered with the center-dialog transitions
 *   regardless of window size (COUI Center-style dialogs, e.g. the rotating loading dialog).
 * @param maxWidth The maximum width of the dialog.
 * @param largeScreen Optional override for the large-screen presentation (centered scale/fade
 *   instead of bottom slide-in). If null, detected from the window size.
 * @param cornerRadius Optional corner radius override. If null, [DialogDefaults.CornerRadius].
 * @param topInset Optional top inset override. If null, calculated from window insets.
 * @param content The content of the dialog.
 */
@Suppress("ktlint:compose:modifier-not-used-at-root")
@Composable
internal fun DialogContentLayout(
    show: Boolean,
    titleColor: Color,
    summaryColor: Color,
    backgroundColor: Color,
    outsideMargin: DpSize,
    insideMargin: DpSize,
    popupHost: @Composable (visible: Boolean, content: @Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    edgeModifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    enableWindowDim: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    defaultWindowInsetsPadding: Boolean = true,
    forceCentered: Boolean = false,
    maxWidth: Dp = DialogDefaults.MaxWidth,
    largeScreen: Boolean? = null,
    cornerRadius: Dp? = null,
    topInset: Dp? = null,
    content: @Composable () -> Unit,
) {
    val animationProgress = remember { Animatable(0f, visibilityThreshold = 0.0001f) }
    val dimProgress = remember { Animatable(0f) }
    val currentOnDismissFinished by rememberUpdatedState(onDismissFinished)
    val internalVisible = remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val keyboardController = LocalSoftwareKeyboardController.current
    val isCentered = forceCentered || (largeScreen ?: DialogDefaults.isLargeScreen())

    LaunchedEffect(show) {
        // Snapshot at launch so a window-resize crossing the breakpoint mid-animation does not
        // swap the spec or relaunch the effect.
        val centeredAtStart = isCentered
        if (show) {
            internalVisible.value = true
            if (enableWindowDim) {
                launch { dimProgress.animateTo(1f, tween(EnterDurationMillis, easing = DialogEnterEasing)) }
            }
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(EnterDurationMillis, easing = DialogEnterEasing),
            )
        } else {
            if (!internalVisible.value) return@LaunchedEffect
            if (imeInsets.getBottom(density) > 0) {
                keyboardController?.hide()
            }
            val exitDuration = if (centeredAtStart) CenterExitDurationMillis else BottomExitDurationMillis
            if (enableWindowDim) {
                launch { dimProgress.animateTo(0f, tween(exitDuration, easing = DialogExitEasing)) }
            }
            animationProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(exitDuration, easing = DialogExitEasing),
            )
            dimProgress.snapTo(0f)
            internalVisible.value = false
            currentOnDismissFinished?.invoke()
        }
    }

    if (!show && !internalVisible.value) return

    val coroutineScope = rememberCoroutineScope()
    val dimAlpha = remember { mutableFloatStateOf(1f) }
    val dialogHeightPx = remember { mutableIntStateOf(0) }
    val backProgress = remember { Animatable(0f) }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)

    val windowInfo = LocalWindowInfo.current

    val requestDismiss: () -> Unit = remember {
        { currentOnDismissRequest?.invoke() }
    }

    val resetGesture: suspend () -> Unit = remember {
        {
            backProgress.animateTo(0f, animationSpec = tween(durationMillis = 150))
            animate(dimAlpha.floatValue, 1f, animationSpec = tween(durationMillis = 150)) { value, _ ->
                dimAlpha.floatValue = value
            }
        }
    }

    popupHost(internalVisible.value) {
        val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationEventState,
            isBackEnabled = show,
            onBackCancelled = {
                coroutineScope.launch { resetGesture() }
            },
            onBackCompleted = { requestDismiss() },
        )

        LaunchedEffect(Unit) {
            // Collect inside a single coroutine so the per-frame `transitionState` ticks during a
            // back gesture do not cancel/relaunch the LaunchedEffect on every progress update.
            snapshotFlow { navigationEventState.transitionState }
                .collect { transitionState ->
                    if (
                        transitionState is NavigationEventTransitionState.InProgress &&
                        transitionState.direction == NavigationEventTransitionState.TRANSITIONING_BACK
                    ) {
                        val progress = transitionState.latestEvent.progress
                        backProgress.snapTo(progress)
                        dimAlpha.floatValue = 1f - progress
                    }
                }
        }

        if (enableWindowDim) {
            val baseColor = COUITheme.colorScheme.windowDimming
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(baseColor.copy(alpha = baseColor.alpha * dimAlpha.floatValue * dimProgress.value))
                    },
            )
        }

        // Off-screen distance below the dialog so the enter slide starts at the bottom screen edge.
        val slideBottomPadding = if (isCentered) {
            0.dp
        } else {
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                WindowInsets.captionBar.asPaddingValues().calculateBottomPadding() +
                outsideMargin.height
        }
        val contentModifier = modifier.graphicsLayer {
            val progress = animationProgress.value
            if (isCentered) {
                // COUI: enter scales 0.8 -> 1 while fading in; exit only fades out.
                val scale = if (show) 0.8f + 0.2f * progress else 1f
                scaleX = scale
                scaleY = scale
                alpha = progress
            } else {
                // COUI: enter slides by the dialog's own height; exit slides to the parent bottom.
                val heightPx = dialogHeightPx.intValue
                val distance = if (show && heightPx > 0) {
                    heightPx + slideBottomPadding.toPx()
                } else {
                    windowInfo.containerDpSize.height.toPx()
                }
                translationY = (1f - progress) * distance
                alpha = 1f
            }
        }

        DialogContent(
            title = title,
            titleColor = titleColor,
            summary = summary,
            summaryColor = summaryColor,
            backgroundColor = backgroundColor,
            outsideMargin = outsideMargin,
            insideMargin = insideMargin,
            defaultWindowInsetsPadding = defaultWindowInsetsPadding,
            backProgress = backProgress,
            dialogHeightPx = dialogHeightPx,
            onDismissRequest = requestDismiss,
            modifier = contentModifier,
            edgeModifier = edgeModifier,
            forceCentered = forceCentered,
            maxWidth = maxWidth,
            largeScreen = largeScreen,
            cornerRadius = cornerRadius,
            topInset = topInset,
            content = {
                CompositionLocalProvider(LocalDismissState provides requestDismiss) {
                    content()
                }
            },
        )
    }
}

@Suppress("ktlint:compose:modifier-not-used-at-root")
@Composable
internal fun DialogContent(
    title: String?,
    titleColor: Color,
    summary: String?,
    summaryColor: Color,
    backgroundColor: Color,
    outsideMargin: DpSize,
    insideMargin: DpSize,
    defaultWindowInsetsPadding: Boolean,
    backProgress: Animatable<Float, *>,
    dialogHeightPx: MutableIntState,
    onDismissRequest: (() -> Unit)?,
    modifier: Modifier = Modifier,
    edgeModifier: Modifier = Modifier,
    forceCentered: Boolean = false,
    maxWidth: Dp = DialogDefaults.MaxWidth,
    largeScreen: Boolean? = null,
    cornerRadius: Dp? = null,
    topInset: Dp? = null,
    content: @Composable () -> Unit,
) {
    val resolvedCornerRadius = cornerRadius ?: DialogDefaults.CornerRadius
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val windowHeight = windowInfo.containerDpSize.height
    val isCentered = forceCentered || (largeScreen ?: DialogDefaults.isLargeScreen())
    val contentAlignment = remember(isCentered) {
        if (isCentered) Alignment.Center else Alignment.BottomCenter
    }
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)

    val calculatedTopInset = if (topInset != null) {
        topInset
    } else {
        val statusBars = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val captionBar = WindowInsets.captionBar.asPaddingValues().calculateTopPadding()
        val displayCutout = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
        maxOf(statusBars, captionBar, displayCutout)
    }

    // Predictive-back translation pad (small-screen only). Pre-converted to px so the per-frame
    // graphicsLayer block does not call asPaddingValues() / toPx() each invalidation.
    val bottomPadding = if (isCentered) {
        0.dp
    } else {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
            WindowInsets.captionBar.asPaddingValues().calculateBottomPadding()
    }
    val extraBottomPaddingPx = remember(density, bottomPadding, outsideMargin.height, isCentered) {
        if (isCentered) 0f else with(density) { (bottomPadding + outsideMargin.height).toPx() }
    }

    val contentModifier = modifier
        .widthIn(max = maxWidth)
        .heightIn(max = if (isCentered) windowHeight * (2f / 3f) else Dp.Unspecified)
        .onGloballyPositioned { coordinates ->
            dialogHeightPx.intValue = coordinates.size.height
        }
        .graphicsLayer {
            // Apply predictive back animation; branch inside the block so the modifier chain
            // produces a single graphicsLayer node instead of swapping nodes per recomposition.
            if (isCentered) {
                val scale = 1f - (backProgress.value * 0.2f)
                scaleX = scale
                scaleY = scale
            } else {
                val maxOffset = if (dialogHeightPx.intValue > 0) {
                    dialogHeightPx.intValue.toFloat() + extraBottomPaddingPx
                } else {
                    500f
                }
                translationY = backProgress.value * maxOffset
            }
        }
        .pointerInput(Unit) {
            detectTapGestures { /* Consume click */ }
        }
        // COUI panels carry no blanket inside padding; each slot brings its own margins.
        .squircleSurface(color = backgroundColor, cornerRadius = resolvedCornerRadius)
        // The material edge must sit inside the surface so its rim paints over the fill. ColorOS 17
        // gives every dialog panel the same rim, so it is drawn here rather than by each caller;
        // `edgeModifier` stays as the per-call extension point.
        .materialShadowEdge(
            stroke = DialogDefaults.stroke(),
            cornerWeight = DialogDefaults.CornerWeight,
            cornerRadius = resolvedCornerRadius,
            edgeStyle = GradientStrokeStyle.EDGE_STYLE_2,
            shadowStyle = InnerShadowStyle.SHADOW_STYLE_0,
        )
        .then(edgeModifier)

    Box(
        modifier = Modifier
            .then(
                if (defaultWindowInsetsPadding) {
                    Modifier
                        .imePadding()
                        .navigationBarsPadding()
                        .captionBarPadding()
                } else {
                    Modifier
                },
            )
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { currentOnDismiss?.invoke() },
                )
            }
            .semantics {
                onClick(label = "Dismiss") {
                    currentOnDismiss?.invoke()
                    true
                }
            }
            .padding(horizontal = outsideMargin.width)
            .padding(top = calculatedTopInset, bottom = outsideMargin.height),
    ) {
        Column(
            modifier = contentModifier.align(contentAlignment),
        ) {
            val titleMultiline = remember { mutableStateOf(false) }
            val summaryMultiline = remember { mutableStateOf(false) }
            // COUI swaps the message scroll paddings for the tall variant once the title or message wraps.
            val messagePadding = if (titleMultiline.value || summaryMultiline.value) {
                MessagePaddingVerticalTall
            } else {
                MessagePaddingVertical
            }
            title?.let {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = insideMargin.width)
                        .padding(top = insideMargin.height, bottom = TitleMarginBottom),
                    text = it,
                    fontSize = COUITheme.textStyles.title4.fontSize,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = DialogLineHeight,
                    color = titleColor,
                    onTextLayout = { titleMultiline.value = it.lineCount > 1 },
                )
            }
            summary?.let {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = insideMargin.width)
                        .padding(vertical = messagePadding),
                    text = it,
                    fontSize = COUITheme.textStyles.body2.fontSize,
                    // COUI centers a single-line message and start-aligns once it wraps.
                    textAlign = if (summaryMultiline.value) TextAlign.Start else TextAlign.Center,
                    lineHeight = DialogLineHeight,
                    color = summaryColor,
                    onTextLayout = { summaryMultiline.value = it.lineCount > 1 },
                )
            }
            PopupBarrierBoundary(content)
        }
    }
}

object DialogDefaults {
    /**
     * Window-size threshold above which the dialog is centered (instead of bottom-aligned)
     * and uses scale-in transitions. Roughly aligns with the Window Size Class
     * compact -> expanded boundary (840 dp width / 480 dp height).
     */
    @Composable
    internal fun isLargeScreen(): Boolean {
        val windowInfo = LocalWindowInfo.current
        val windowWidth = windowInfo.containerDpSize.width
        val windowHeight = windowInfo.containerDpSize.height
        return windowHeight >= 480.dp && windowWidth >= 840.dp
    }

    /**
     * The default color of the title.
     */
    @Composable
    fun titleColor() = COUITheme.colorScheme.onBackground

    /**
     * The default color of the summary. COUI uses the same primary label color as the title.
     */
    @Composable
    fun summaryColor() = COUITheme.colorScheme.onBackground

    /**
     * The default background color of the dialog.
     */
    @Composable
    fun backgroundColor() = COUITheme.colorScheme.background

    /**
     * The default corner radius of the dialog panel.
     *
     * `COUIAlertDialogMaxLinearLayout.setOutLineProvider()` gates on
     * `COUIVersionUtil.getOSVersionCode() > 37` and then clips the panel with
     * `R.dimen.coui_round_corner_xxxxl_radius_large_16_1` = **45dp** through
     * `OplusOutlineAdapter.setSmoothRoundRect(..., 45dp, R.dimen.weight_16_1_large)`, i.e. a smooth
     * round rect at weight 3.0. The 16-era branch used `attr/couiRoundCornerXXXXL` = 25dp at
     * weight 2.5. `COUIAlertDialogBuilder` and `COUIBottomSheetDialog` resolve the same 45dp / 3.0
     * pair for the blur backdrop layer.
     *
     * This library cannot reproduce 45dp at weight 3.0: `squircleSurface` bakes
     * `SquirclePath.SQUIRCLE_CONTROL` for weight 2.5 and exposes no weight parameter (see
     * `basic/Card.kt`). A heavier weight stretches the corner further along the edges, so the same
     * outline needs a *smaller* radius at 2.5 — measured against a live ColorOS 17 settings dialog,
     * 38dp on the baked squircle reproduces the native corner path to within 3px over the whole
     * arc. 45dp at weight 2.5 overshoots by 15px at the tangent point.
     */
    val CornerRadius = 38.dp

    /**
     * The `u_commonArray[10]` corner weight of a dialog panel.
     *
     * ColorOS 17 feeds the dialog's shadow-edge shader `R.dimen.weight_16_1_large` = 3.0, and the
     * native panel *silhouette* is generated with that same weight, so the rim's SDF and the drawn
     * shape agree. [`squircleSurface`] bakes `SquirclePath.SQUIRCLE_CONTROL` for weight 2.5 and
     * cannot be parameterized (see `basic/Card.kt`), so the rim has to use 2.5 as well: passing 3.0
     * here makes the shader's corner geometry disagree with the drawn outline and the rim fades out
     * along the whole corner arc (measured: 0 luminance over body on the arc, against +36 native).
     */
    val CornerWeight = MaterialShadowEdgeDefaults.SquircleCornerWeight

    /**
     * The default material edge of the dialog panel.
     *
     * ColorOS 17 draws the rim from `COUIShadowEdgeDrawable` fed by
     * `DialogShadowEdgeParamsExtKt.dialogShadowEdgeParamsDefault`, i.e. a 4dp / 0.6 alpha rim with
     * a 0.4 inner-shadow fade in light mode and 2dp / 0.3 with 0.03 in dark mode. The panel uses
     * the `EDGE_STYLE_2` "large panel" preset, which is the same gradient the dialog root layout
     * installs.
     */
    @Composable
    fun stroke(): MaterialStrokeParams = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
        MaterialStrokeParams(
            edgeAlpha = 0.3f,
            edgeWidth = 2.dp,
            shadowFadeIn = 0.03f,
        )
    } else {
        MaterialStrokeParams(
            edgeAlpha = 0.6f,
            edgeWidth = 4.dp,
            shadowFadeIn = 0.4f,
        )
    }

    /**
     * The default upper bound on dialog content width. Keeps dialogs from stretching across
     * tablet / desktop windows. COUI `coui_dialog_max_width`.
     */
    val MaxWidth = 392.dp

    /**
     * The default margin outside the dialog.
     * COUI `coui_dialog_layout_margin_horizontal` (16dp) / `coui_dialog_layout_margin_vertical`
     * (32dp).
     */
    val outsideMargin = DpSize(16.dp, 32.dp)

    /**
     * The default margin for the built-in title and summary texts: width is their horizontal
     * padding (COUI `coui_alert_dialog_message_padding_left`), height is the top padding above
     * the title (`coui_no_message_alert_dialog_title_margin_top`). The content slot is left
     * unpadded so COUI-style button bars can span the full panel width.
     */
    val insideMargin = DpSize(24.dp, 24.dp)

    /**
     * The min height of a horizontal dialog button bar. COUI `coui_alert_dialog_button_height`
     * (44dp, `mHorButPanelMinHeight`), the height COUI gives each bar button.
     */
    val ButtonBarMinHeight = 44.dp

    /**
     * The paddings of a button in a horizontal dialog button bar. The horizontal start/end are
     * COUI `coui_alert_dialog_button_horizontal_padding` (12dp); the vertical values are the
     * bar's own top/bottom inset, carried on the buttons so the panel bottom inset rides on them
     * (`COUIButtonBarLayout.resetHorButsPadding`).
     */
    val ButtonBarInsideMargin = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 22.dp)

    /**
     * The thickness of the divider between horizontal dialog bar buttons. COUI
     * `coui_delete_alert_dialog_divider_height_horizontalbutton`; the 0.33dp `_verticalbutton`
     * token applies only when the bar stacks vertically.
     */
    val ButtonBarDividerThickness = 1.dp

    /**
     * The top inset of the divider between horizontal dialog bar buttons. COUI
     * `coui_bottom_alert_dialog_horizontal_button_padding_top_extra_divider_new`.
     */
    val ButtonBarDividerInsetTop = 17.dp

    /**
     * The bottom inset of the divider between horizontal dialog bar buttons. COUI
     * `coui_bottom_alert_dialog_horizontal_button_padding_bottom_extra_divider_new`.
     */
    val ButtonBarDividerInsetBottom = 21.dp
}

/** COUI android_alert_dialog_enter: pathInterpolator(0.3, 0, 0.1, 1). */
private val DialogEnterEasing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

/** COUI android_alert_dialog_exit: pathInterpolator(0.3, 0, 1, 1). */
private val DialogExitEasing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

/** COUI dialog enter animations (bottom translate / center scale+fade) run for 250ms. */
private val EnterDurationMillis = 250

/** COUI coui_bottom_dialog_exit translate duration. */
private val BottomExitDurationMillis = 250

/** COUI coui_center_dialog_exit alpha duration. */
private val CenterExitDurationMillis = 150

/** COUI dialog title/message lineSpacingMultiplier 1.1, resolved against sans-serif font metrics. */
private val DialogLineHeight = 1.29f.em

/** COUI coui_no_message_alert_dialog_title_margin_bottom, applied for every non-tiny dialog. */
private val TitleMarginBottom = 6.dp

/** COUI coui_alert_dialog_scroll_padding_top/bottom_message_tallDialog (the resting values). */
private val MessagePaddingVertical = 18.dp

/** COUI message scroll padding once the title or message is multiline (a tall dialog). */
private val MessagePaddingVerticalTall = 8.dp
