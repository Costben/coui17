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
import io.github.suqi8.coui.kmp.basic.couiGlassSurface
import io.github.suqi8.coui.kmp.basic.materialShadowEdge
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
import io.github.suqi8.coui.kmp.blur.BlurDefaults
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.overlay.OverlayDialog
import io.github.suqi8.coui.kmp.squircle.SquircleShape
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
                    windowInfo.containerSize.height.toFloat()
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
    // Converted at our density, not the platform's: see `BottomSheetContentLayout`.
    val windowHeight = with(density) { windowInfo.containerSize.height.toDp() }
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

    val dialogShape = remember(resolvedCornerRadius) { SquircleShape(resolvedCornerRadius) }
    val dialogSurface = Modifier.couiGlassSurface(
        backdrop = LocalBackdrop.current,
        shape = dialogShape,
        containerColor = backgroundColor,
        fallback = Modifier.squircleSurface(color = backgroundColor, cornerRadius = resolvedCornerRadius),
        blurRadius = DialogDefaults.blurRadius(),
        blendColors = DialogDefaults.blurColors(),
    )

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
        // ColorOS 17 frosts the dialog: `COUIAlertDialogBuilder` feeds `COUIBackgroundBlurBuilder`
        // `coui_dialog_list_blend_blur_*` + `coui_dialog_list_mix_blur_*` at
        // `coui_dialog_blur_radius`, and `coui_dialog_list_background_color_above_blur` is
        // transparent, so the panel is blur + two tints with no opaque fill. Below Android 13 — or
        // with no backdrop — it falls back to the flat squircle fill.
        .then(dialogSurface)
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
            // `COUIAlertDialogMessageScrollViewStyle` pads the message scroller, not the message text,
            // and the builder swaps in the no-title variant when `alertTitle` is absent. With a title
            // the gap above the message is the title's own
            // `coui_alert_dialog_title_margin_bottom` (6dp); with no title the scroller supplies
            // `coui_alert_dialog_scroll_padding_top_message_no_title` (22dp) instead.
            val messageTopPadding = if (title == null) MessageTopPaddingNoTitle else MessageTopPadding
            val messageBottomPadding = if (title == null) MessageBottomPaddingNoTitle else MessageBottomPadding
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
                        .padding(top = messageTopPadding, bottom = messageBottomPadding),
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
     * The default color of the summary.
     *
     * `COUIAlertDialogMessageStyle` sets `android:textColor="?couiColorLabelSecondary"`, which is
     * `coui_color_label_secondary` (`#8a000000`): over the frosted panel that lands on `#696969`,
     * noticeably lighter than the title. Not `onBackground`, which is the title's own
     * `?couiColorLabelPrimary`.
     */
    @Composable
    fun summaryColor() = COUITheme.colorScheme.onBackgroundVariant

    /**
     * The default background color of the dialog.
     */
    @Composable
    fun backgroundColor() = COUITheme.colorScheme.background

    /**
     * The dialog blur radius in px.
     *
     * `COUIAlertDialogBuilder` reads `R.integer.coui_dialog_blur_radius` (200) and hands it to
     * `COUIBackgroundBlurBuilder` in device pixels — half the sheet's 400, because a centred dialog
     * sits over less of the page.
     */
    val NativeBlurRadiusPx = 200f

    /**
     * [NativeBlurRadiusPx] in dp for the current display, clamped to [BlurDefaults.MaxBlurRadius].
     *
     * The native panel keeps none of the page's structure: on the ColorOS 17 "change device name"
     * dialog the flat part of the panel varies by 1.7 levels over a page that varies by 26–43. Only
     * the full strength gets there; anything weaker leaves the rows behind readable through the
     * panel.
     */
    @Composable
    fun blurRadius(): Float {
        val density = LocalDensity.current.density
        return (NativeBlurRadiusPx / density).coerceIn(0f, BlurDefaults.MaxBlurRadius)
    }

    /**
     * The tint of the light dialog over its blur.
     *
     * The native recipe is `coui_dialog_list_blend_blur_light` (`#509F9F9F`) and
     * `coui_dialog_list_mix_blur_light` (`#DC909090`), handed to `OplusBlurParam.setMaterialParams`.
     * That platform shader weighs them as material params, not as `SrcOver` layers — stacked with
     * `SrcOver` the two greys could never come out lighter than 160, yet the live panel is (228,228,228).
     * So the layer here is fitted to the measured output instead: a neutral grey at the same 86%
     * weight as the native mix, which lands on 228 over a light page and lets the blurred page tint
     * the remaining 14%.
     */
    val BlurColorsLight: List<BlendColorEntry> = listOf(
        BlendColorEntry(color = Color(0xDCE7E7E7)),
    )

    /**
     * The tint of the dark dialog over its blur: the native recipe itself, not a fit.
     *
     * In night mode `COUIBackgroundBlurBuilder` hands `OplusBlurParam.setMaterialParams` mode 2 with
     * `coui_dialog_list_blend_blur_dark` (`#B2292929`) and `coui_dialog_list_mix_blur_dark`
     * (`#8C777777`). BlurService's composite shader (`librenderkit.so`, `bgBlend`) reads mode 2 as
     * "luminosity + dodge": the blur takes the blend colour's luminance at its alpha, keeping its own
     * hue and saturation, then the mix colour dodges it at its alpha. Both steps are standard blend
     * modes, so the layers reproduce it exactly. Over a black page it lands on (42,42,42) against the
     * measured (40,40,40), and because luminosity keeps the page's hue a coloured page still tints
     * the panel — the translucency a flat grey layer erased.
     */
    val BlurColorsDark: List<BlendColorEntry> = listOf(
        BlendColorEntry(color = Color(0xB2292929), mode = BlurBlendMode.Luminosity),
        BlendColorEntry(color = Color(0x8C777777), mode = BlurBlendMode.ColorDodge),
    )

    /**
     * [BlurColorsLight] or, in dark theme, [BlurColorsDark] behind a `windowDimming` layer.
     *
     * The platform blurs what lies under the dialog's window, and the dim layer is one of those
     * layers, so the native recipe works on the dimmed page: over the gallery's grey dark page the
     * undimmed input lands the dialog on (85,85,85) instead of the system's ~(40,40,40). Blurring then
     * dimming equals dimming then blurring, so the dim leads the layer list. The light layers are
     * fitted to the measured output, which already carries the dim, and stay as they are.
     */
    @Composable
    fun blurColors(): List<BlendColorEntry> {
        val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
        if (!isDark) return BlurColorsLight
        val dim = COUITheme.colorScheme.windowDimming
        return remember(dim) { listOf(BlendColorEntry(color = dim)) + BlurColorsDark }
    }

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
     * The interior padding of a button in a horizontal dialog button bar, i.e. the space between
     * the capsule's edge and its label.
     *
     * The horizontal start/end are COUI `coui_alert_dialog_button_horizontal_padding` (12dp); the
     * vertical values are `coui_bottom_alert_dialog_horizontal_button_padding_top_extra_new` and
     * `..._bottom_extra_new` (4dp each), the insets `COUIButtonBarLayout.resetHorButsPadding` adds
     * on top of the 44dp capsule height.
     *
     * These are *not* the bar's own insets: the 4dp above the capsules and the 16dp below them are
     * cell padding (`coui_alert_dialog_button_parent_padding_vertical` / `..._bottom`), applied by
     * the bar — see `DialogButtonBarDefaults.ButtonBarPaddingTop` / `ButtonBarPaddingBottom`. A
     * capsule that carries the panel inset itself is what made the ported buttons spill over the
     * panel's bottom corners.
     */
    val ButtonBarInsideMargin = PaddingValues(horizontal = 12.dp, vertical = 4.dp)

    /**
     * Retained for source compatibility and unused: the ColorOS 17 alert-dialog bar draws no
     * dividers.
     *
     * `coui_center_alert_dialog_button_panel.xml` holds three bare button cells, and
     * `COUIButtonBarLayout.setShowDividerWhenHasItems` is an empty method in ColorOS 17. This dimen
     * (`coui_delete_alert_dialog_divider_height_horizontalbutton`) belongs to the delete-dialog bar,
     * not to the alert dialog, whose capsules are separated by an 8dp gap instead.
     */
    val ButtonBarDividerThickness = 1.dp

    /**
     * Retained for source compatibility and unused, see [ButtonBarDividerThickness].
     *
     * COUI `coui_bottom_alert_dialog_horizontal_button_padding_top_extra_divider_new`.
     */
    val ButtonBarDividerInsetTop = 17.dp

    /**
     * Retained for source compatibility and unused, see [ButtonBarDividerThickness].
     *
     * COUI `coui_bottom_alert_dialog_horizontal_button_padding_bottom_extra_divider_new`.
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

/** `coui_alert_dialog_scroll_padding_top_message` (0dp) — the title already ends with 6dp. */
private val MessageTopPadding = 0.dp

/** `coui_alert_dialog_scroll_padding_bottom_message` (16dp), shared by every titled dialog. */
private val MessageBottomPadding = 16.dp

/**
 * `coui_alert_dialog_scroll_padding_top_message_no_title` (22dp) — a dialog without a title still
 * needs roughly the 24dp `coui_alert_dialog_title_margin_top` in front of its first line of text.
 */
private val MessageTopPaddingNoTitle = 22.dp

/** `coui_alert_dialog_scroll_padding_bottom_message_no_title` (4dp). */
private val MessageBottomPaddingNoTitle = 4.dp
