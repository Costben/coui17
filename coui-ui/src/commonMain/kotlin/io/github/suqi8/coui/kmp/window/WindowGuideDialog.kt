// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.window

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.PageIndicator
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.layout.DialogContentLayout
import io.github.suqi8.coui.kmp.layout.DialogDefaults
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.RemovePlatformDialogDefaultEffects
import io.github.suqi8.coui.kmp.utils.WindowNavigationEventScope
import io.github.suqi8.coui.kmp.utils.platformDialogProperties
import kotlinx.coroutines.launch

/**
 * One page of a [WindowGuideDialog].
 *
 * The data counterpart of `COUIGuidePageItem` (`com.coui.appcompat.dialog.COUIGuidePageItem`), the
 * holder the native `COUIGuidePageAdapter` binds into `coui_guide_page_item.xml`.
 *
 * @param title The page title, drawn with `?attr/couiTextHeadlineS` — 18sp in `sans-serif-medium`
 *   (weight 550) — in `?attr/couiColorLabelPrimary`. `guide_tips_title` of the native layout.
 * @param description The optional page description, drawn with `?attr/couiTextBodyS` (13sp) in
 *   `?attr/couiColorLabelSecondary`. `guide_tips_description`.
 * @param image The optional page image, drawn `328dp` tall and full width with `scaleType`
 *   `CENTER_CROP` (`pager_item_im`). The native view is `GONE` until the adapter binds a drawable,
 *   so a null image leaves no gap.
 */
@Immutable
data class GuidePage(
    val title: String,
    val description: String? = null,
    val image: Painter? = null,
)

/**
 * The ColorOS 17 first-run multi-page guide dialog: a bottom-anchored panel of
 * [HorizontalPager] pages, a [PageIndicator], and a Skip / Next button bar, mirroring
 * `COUIGuideDialogBuilder` and its content view `COUIGuideLayoutContentView`.
 *
 * The panel reuses [DialogContentLayout], so the window presentation, the dim, the insets, the
 * squircle clip and the bottom slide-in / slide-out of `Animation.COUI.Dialog` are the same as every
 * other dialog in this library; only the content column is built here.
 *
 * The content column follows `COUIGuideLayoutContentView.initLayout()` exactly, top to bottom:
 * 1. the pager, weight 1, bottom margin 16dp (`coui_dialog_guide_viewpager2_margin_bottom`), or 20dp
 *    (`coui_dialog_guide_viewpager2_margin_single_bottom`) when there is a single page. Each page is
 *    a vertically scrollable column: image, then title, then description;
 * 2. the [PageIndicator], centred, bottom margin 20dp
 *    (`coui_dialog_guide_indicator_margin_bottom`). It is hidden for a single page, which the native
 *    `setDefaultButtonText` also forces `GONE`;
 * 3. the button bar, full width, bottom margin 8dp (`coui_dialog_guide_button_layout_margin_bottom`)
 *    and 16dp horizontal padding (`coui_dialog_guide_button_layout_padding_horizontal`). A single page
 *    has only the primary button spanning the row (`SINGLE_BUTTON`); with several pages the secondary
 *    Skip button sits on the left and the primary button on the right, each half the row minus the 8dp
 *    spacing (`coui_dialog_guide_button_layout_space`), both at least 132 x 44dp
 *    (`coui_btn_large_width_min` / `coui_btn_large_height_min`).
 *
 * On the penultimate page the bar animates exactly like `COUIGuideDialogButtonLayout`:
 * `onPageScrolled` maps the pager position to
 * `clamp((currentPage + offsetFraction) - (count - 2), 0f, 1f)`, eases it on
 * `COUIEaseInterpolator` (cubic-bezier 0.33, 0, 0.67, 1), fades Skip out by progress 0.6
 * (`SECONDARY_BUTTON_ALPHA_END_PROGRESS`), grows the primary button from half to full width from
 * progress 0.2 (`WIDTH_CHANGE_START_PROGRESS`) on the response 0.15 / bounce 0 spring, and
 * cross-fades its label from [GuideDialogDefaults.NextText] to [GuideDialogDefaults.KnownText] over
 * progress 0.5 to 0.85 (`TEXT_FADE_START_PROGRESS` / `TEXT_FADE_END_PROGRESS` / `TEXT_FADE_RANGE`).
 *
 * Dismissal: Skip, the primary button once the last page is reached, the back gesture and a tap
 * outside all invoke [onDismissRequest]. On an intermediate page the primary button advances the
 * pager instead. The native builder also calls `setCanceledOnTouchOutside(false)`, but the shared
 * [DialogContentLayout] owns the outside-tap handler for every dialog in this library, so outside-tap
 * dismissal stays enabled here rather than being special-cased for this one dialog.
 *
 * Not ported, because no consumer in this library reaches them:
 * - `COUIGuideBehavior`, a renamed Material `BottomSheetBehavior` with the OPlus pull-up extensions;
 *   the report shows it references no guide-dialog resource and is not used by
 *   `COUIGuideDialogBuilder`;
 * - the ViewPager2 fling tuning of `createGuideDialogConfig` (650ms program scroll, 500-1000ms fling,
 *   velocity threshold 2500, max 6000, decay 2.0, `COUISpringInterpolator(0.6, 0.0)`); Compose's pager
 *   carries its own fling model;
 * - the dual-`TextPaint` drawing of `COUITwoTextButton`; the cross-fade here is two overlaid texts
 *   with the recovered alphas;
 * - the 32dp vertical fading edge of the native page root (`COUINestedScrollView` with
 *   `bottom_dialog_scroll_fading_edge_length`); the pages scroll without it;
 * - the translations of `coui_guide_dialog_skip_text` / `_next_text` / `_known_text`; only the
 *   default English strings are carried.
 *
 * @param show Whether the [WindowGuideDialog] is shown.
 * @param pages The pages of the guide, in order.
 * @param onDismissRequest Called when the user dismisses the guide (Skip, the last-page primary
 *   button, the back gesture or a tap outside).
 * @param modifier The modifier to be applied to the [WindowGuideDialog].
 * @param onDismissFinished The callback when the [WindowGuideDialog] is completely dismissed.
 * @param skipText The label of the secondary button. COUI `coui_guide_dialog_skip_text`, "Skip".
 * @param nextText The label of the primary button before the last page. COUI
 *   `coui_guide_dialog_next_text`, "Next".
 * @param knownText The label of the primary button once the last page is reached. COUI
 *   `coui_guide_dialog_known_text`, "Got it".
 * @param imageHeight The height of a page image. COUI `ImageHeightStyle`, `DEFAULT` 328dp or
 *   `HEIGHT_240DP` 240dp.
 * @param onPageChanged Called with the index of the page the pager settles on, including the first.
 */
@Composable
fun WindowGuideDialog(
    show: Boolean,
    pages: List<GuidePage>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissFinished: () -> Unit = {},
    skipText: String = GuideDialogDefaults.SkipText,
    nextText: String = GuideDialogDefaults.NextText,
    knownText: String = GuideDialogDefaults.KnownText,
    imageHeight: Dp = GuideDialogDefaults.ImageHeight,
    onPageChanged: (Int) -> Unit = {},
) {
    val statusBarsPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val captionBarPadding = WindowInsets.captionBar.asPaddingValues().calculateTopPadding()
    val displayCutoutPadding = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val safeTopInset = remember(statusBarsPadding, captionBarPadding, displayCutoutPadding) {
        maxOf(statusBarsPadding, captionBarPadding, displayCutoutPadding)
    }

    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)

    DialogContentLayout(
        show = show,
        titleColor = DialogDefaults.titleColor(),
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        popupHost = { visible, hostContent ->
            if (visible) {
                Dialog(
                    onDismissRequest = { currentOnDismissRequest() },
                    properties = platformDialogProperties(),
                ) {
                    RemovePlatformDialogDefaultEffects()
                    WindowNavigationEventScope {
                        hostContent()
                    }
                }
            }
        },
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        topInset = safeTopInset,
        cornerRadius = GuideDialogDefaults.CornerRadius,
        content = {
            GuideDialogContent(
                pages = pages,
                imageHeight = imageHeight,
                skipText = skipText,
                nextText = nextText,
                knownText = knownText,
                onDismissRequest = onDismissRequest,
                onPageChanged = onPageChanged,
            )
        },
    )
}

@Composable
private fun GuideDialogContent(
    pages: List<GuidePage>,
    imageHeight: Dp,
    skipText: String,
    nextText: String,
    knownText: String,
    onDismissRequest: () -> Unit,
    onPageChanged: (Int) -> Unit,
) {
    if (pages.isEmpty()) return

    val pageCount = pages.size
    val isSinglePage = pageCount <= 1
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val coroutineScope = rememberCoroutineScope()
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val currentOnPageChanged by rememberUpdatedState(onPageChanged)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { currentOnPageChanged(it) }
    }

    // COUIGuideDialogButtonLayout.onPageScrolled: the button bar's progress over the last two pages.
    val progress by remember {
        derivedStateOf {
            ((pagerState.currentPage + pagerState.currentPageOffsetFraction) - (pageCount - 2))
                .coerceIn(0f, 1f)
        }
    }
    val buttonProgress = GuidePageScrollEasing.transform(progress)
    val skipAlpha = ((GuideSkipFadeEndProgress - buttonProgress) / GuideSkipFadeEndProgress).coerceIn(0f, 1f)
    val primaryWidthFraction by animateFloatAsState(
        targetValue = (
            (buttonProgress - GuideWidthChangeStartProgress) /
                (1f - GuideWidthChangeStartProgress)
            ).coerceIn(0f, 1f),
        animationSpec = GuideWidthSpring,
        label = "guidePrimaryButtonWidth",
    )
    val labelCrossFade = (
        (buttonProgress - GuideDialogDefaults.TextFadeStartProgress) /
            GuideDialogDefaults.TextFadeRange
        ).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = GuideDialogDefaults.MaxHeight),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    bottom = if (isSinglePage) {
                        GuideDialogDefaults.ViewPagerMarginBottomSingle
                    } else {
                        GuideDialogDefaults.ViewPagerMarginBottom
                    },
                ),
        ) { page ->
            GuidePageContent(page = pages[page], imageHeight = imageHeight)
        }

        if (!isSinglePage) {
            PageIndicator(
                count = pageCount,
                currentIndex = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = GuideDialogDefaults.IndicatorMarginBottom),
            )
        }

        GuideButtonBar(
            isSinglePage = isSinglePage,
            primaryWidthFraction = primaryWidthFraction,
            skipAlpha = skipAlpha,
            labelCrossFade = labelCrossFade,
            skipText = skipText,
            nextText = nextText,
            knownText = knownText,
            onSkip = { currentOnDismissRequest() },
            onPrimary = {
                if (pagerState.currentPage >= pageCount - 1) {
                    currentOnDismissRequest()
                } else {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
        )
    }
}

@Composable
private fun GuidePageContent(page: GuidePage, imageHeight: Dp) {
    val colorScheme = COUITheme.colorScheme
    val textStyles = COUITheme.textStyles

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        page.image?.let { painter ->
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight),
            )
        }
        Text(
            text = page.title,
            color = colorScheme.onSurface,
            style = textStyles.title4,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GuideDialogDefaults.TitlePaddingHorizontal)
                .padding(
                    top = GuideDialogDefaults.TitleMarginTop,
                    bottom = GuideDialogDefaults.TitleMarginBottom,
                ),
        )
        page.description?.let { description ->
            Text(
                text = description,
                color = colorScheme.onSurfaceSecondary,
                style = textStyles.footnote1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GuideDialogDefaults.BodyPaddingHorizontal),
            )
        }
    }
}

@OptIn(ExperimentalCOUIApi::class)
@Composable
private fun GuideButtonBar(
    isSinglePage: Boolean,
    primaryWidthFraction: Float,
    skipAlpha: Float,
    labelCrossFade: Float,
    skipText: String,
    nextText: String,
    knownText: String,
    onSkip: () -> Unit,
    onPrimary: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GuideDialogDefaults.ButtonBarPaddingHorizontal)
            .padding(bottom = GuideDialogDefaults.ButtonBarMarginBottom),
    ) {
        // COUIGuideDialogButtonLayout.onMeasure / onLayout: half the row minus the spacing, growing by
        // another (half + spacing) so the primary button reaches the full row width.
        val rowWidth = maxWidth
        val halfWidth = (rowWidth - GuideDialogDefaults.ButtonSpacing) / 2f
        val primaryWidth = (halfWidth + (halfWidth + GuideDialogDefaults.ButtonSpacing) * primaryWidthFraction)
            .coerceAtMost(rowWidth)
        val skipWidth = (rowWidth - primaryWidth - GuideDialogDefaults.ButtonSpacing).coerceAtLeast(0.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isSinglePage && skipWidth > 0.dp) {
                TextButton(
                    text = skipText,
                    onClick = onSkip,
                    modifier = Modifier
                        .width(skipWidth)
                        .graphicsLayer { alpha = skipAlpha },
                    pressScaleEnabled = false,
                    minWidth = GuideDialogDefaults.ButtonMinWidth,
                    minHeight = GuideDialogDefaults.ButtonMinHeight,
                )
                Spacer(modifier = Modifier.width(GuideDialogDefaults.ButtonSpacing))
            }
            Button(
                onClick = onPrimary,
                modifier = Modifier.width(if (isSinglePage) rowWidth else primaryWidth),
                pressScaleEnabled = false,
                minWidth = GuideDialogDefaults.ButtonMinWidth,
                minHeight = GuideDialogDefaults.ButtonMinHeight,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = nextText,
                        style = COUITheme.textStyles.button,
                        modifier = Modifier.graphicsLayer { alpha = 1f - labelCrossFade },
                    )
                    Text(
                        text = knownText,
                        style = COUITheme.textStyles.button,
                        modifier = Modifier.graphicsLayer { alpha = labelCrossFade },
                    )
                }
            }
        }
    }
}

/** Contains default values used by [WindowGuideDialog]. */
object GuideDialogDefaults {

    /**
     * The corner radius of the guide panel. COUI `coui_dialog_guide_clip_radius_root`, 32.0dp. The
     * report's gap 1 notes that the dimen's consumer could not be located in the guide classes — the
     * value is confirmed, the native call site is not.
     */
    val CornerRadius = 32.dp

    /** The maximum height of the guide panel. COUI `coui_dialog_guide_max_height`, 632.0dp. */
    val MaxHeight = 632.dp

    /**
     * The bottom margin of the pager with several pages. COUI
     * `coui_dialog_guide_viewpager2_margin_bottom`, 16.0dp.
     */
    val ViewPagerMarginBottom = 16.dp

    /**
     * The bottom margin of the pager with a single page. COUI
     * `coui_dialog_guide_viewpager2_margin_single_bottom`, 20.0dp.
     */
    val ViewPagerMarginBottomSingle = 20.dp

    /**
     * The bottom margin of the page indicator. COUI `coui_dialog_guide_indicator_margin_bottom`,
     * 20.0dp.
     */
    val IndicatorMarginBottom = 20.dp

    /**
     * The bottom margin of the button bar. COUI `coui_dialog_guide_button_layout_margin_bottom`,
     * 8.0dp.
     */
    val ButtonBarMarginBottom = 8.dp

    /**
     * The horizontal padding of the button bar. COUI
     * `coui_dialog_guide_button_layout_padding_horizontal`, 16.0dp.
     */
    val ButtonBarPaddingHorizontal = 16.dp

    /** The spacing between the two buttons. COUI `coui_dialog_guide_button_layout_space`, 8.0dp. */
    val ButtonSpacing = 8.dp

    /** The minimum width of a button. COUI `coui_btn_large_width_min`, 132dp. */
    val ButtonMinWidth = 132.dp

    /** The minimum height of a button. COUI `coui_btn_large_height_min`, 44dp. */
    val ButtonMinHeight = 44.dp

    /** The height of a page image. COUI `ImageHeightStyle.DEFAULT`, 328dp. */
    val ImageHeight = 328.dp

    /** The alternative height of a page image. COUI `ImageHeightStyle.HEIGHT_240DP`, 240dp. */
    val ImageHeightAlt = 240.dp

    /**
     * The horizontal padding of a page title. `guide_tips_title` of `coui_guide_page_item.xml`
     * (`android:paddingHorizontal`), 24dp.
     */
    val TitlePaddingHorizontal = 24.dp

    /**
     * The top margin of a page title. `guide_tips_title` of `coui_guide_page_item.xml`
     * (`android:layout_marginTop`), 24dp.
     */
    val TitleMarginTop = 24.dp

    /**
     * The bottom margin of a page title, i.e. the title-to-description gap. `guide_tips_title` of
     * `coui_guide_page_item.xml` (`android:layout_marginBottom`), 6dp.
     */
    val TitleMarginBottom = 6.dp

    /**
     * The horizontal padding of a page description. `guide_tips_description` of
     * `coui_guide_page_item.xml` (`android:paddingHorizontal`), 24dp.
     */
    val BodyPaddingHorizontal = 24.dp

    /** The label of the secondary button. COUI `coui_guide_dialog_skip_text`, "Skip". */
    val SkipText = "Skip"

    /** The label of the primary button before the last page. COUI `coui_guide_dialog_next_text`, "Next". */
    val NextText = "Next"

    /** The label of the primary button on the last page. COUI `coui_guide_dialog_known_text`, "Got it". */
    val KnownText = "Got it"

    /**
     * The progress at which the primary button's label starts fading from [NextText] to [KnownText].
     * COUI `COUIGuideDialogButtonLayout.TEXT_FADE_START_PROGRESS`, 0.5f.
     */
    val TextFadeStartProgress = 0.5f

    /**
     * The progress at which the label cross-fade ends. COUI
     * `COUIGuideDialogButtonLayout.TEXT_FADE_END_PROGRESS`, 0.85f.
     */
    val TextFadeEndProgress = 0.85f

    /**
     * The span of the label cross-fade. COUI `COUIGuideDialogButtonLayout.TEXT_FADE_RANGE`, 0.35f,
     * i.e. [TextFadeEndProgress] - [TextFadeStartProgress].
     */
    val TextFadeRange = 0.35f
}

/**
 * `COUIGuideDialogButtonLayout.PAGE_SCROLL_INTEPOLATOR` = `COUIEaseInterpolator`, a
 * `PathInterpolator(0.33, 0, 0.67, 1)`.
 */
private val GuidePageScrollEasing = CubicBezierEasing(0.33f, 0f, 0.67f, 1f)

/**
 * The progress at which the Skip button has fully faded out. COUI
 * `COUIGuideDialogButtonLayout.SECONDARY_BUTTON_ALPHA_END_PROGRESS`, 0.6f.
 */
private val GuideSkipFadeEndProgress = 0.6f

/**
 * The progress at which the primary button starts growing to the full row width. COUI
 * `COUIGuideDialogButtonLayout.WIDTH_CHANGE_START_PROGRESS`, 0.2f.
 */
private val GuideWidthChangeStartProgress = 0.2f

/**
 * The width spring of `COUIGuideDialogButtonLayout.initAnimation`: `COUISpringForce` response 0.15f
 * (`SPRING_RESPONSE`) and bounce 0.0f, cut at `SPRING_MINIMUM_VISIBLE_CHANGE` (0.001f).
 */
private val GuideWidthSpring: SpringSpec<Float> = folmeSpring(
    damping = 1f,
    response = 0.15f,
    visibilityThreshold = 0.001f,
)
