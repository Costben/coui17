// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.squircle.squircleSurface
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * A tip banner shown at the top of a page, following the ColorOS top-tips family.
 *
 * The family's card sets a medium corner radius and weight and fills the card with
 * `?attr/couiColorFillThin`, and its show / dismiss animation scales the view on Y from 0 to 1
 * over 250ms. The port keeps that 250ms vertical scale for [visible].
 *
 * The default variant overrides the background:
 * [TopTipsBackgroundStyle] `White` fills with `?attr/couiColorCard`, `Gray` — the default of
 * `mLastBackgroundStyle` — with `?attr/couiColorContainer4`), switches the corner radius with
 * `updateRadius(hasTitle, isSingleLineText)` and picks its layout mode from the style with
 * `resolveTargetLayoutMode`. Its content is a `COUIDefaultTopTipsView`, a `ConstraintLayout`
 * inflated from `res/layout/coui_default_toptips.xml` (or `res/layout/coui_ai_payment_toptips.xml`)
 * holding an `image` icon, a `title`, a `COUIMarqueeTextView` `content`, an `ignore` and an
 * `action` `COUIButton`, and a `close` `ImageView`.
 *
 * `COUIMarqueeTopTips` adds only `startRoll` / `stopRoll`, which forward to
 * `COUIDefaultTopTipsView.startRoll` / `stopRoll` and therefore to
 * `COUIMarqueeTextView.continueRoll` / `stopRoll`; [marqueeEnabled] is that switch, and the
 * scrolling core is [MarqueeText].
 *
 * Not ported, because the corresponding machinery has no Compose equivalent:
 * - `COUIDefaultTopTips.animateExpand` / `animateCollapse` and the `mClipAnimator` /
 *   `calculateClipHeight` outline clipping that grows and shrinks the banner, plus
 *   `animateWidthChange` / `createSpaceAndFixLayout` and the `Space`-based width cross-fade; the
 *   port shows and hides the banner with `COUICustomTopTips`' scale instead.
 * - `setTipsBackgroundDrawable` / `COUIDefaultTopTips.BackgroundOverlayView` as an Android `View`
 *   that measures 0x0 and is laid out over the card; [backgroundOverlay] is the same slot, drawn
 *   over a transparent fill exactly as `applyBackgroundOverlayState` blanks the card colour.
 * - `RoundCornerUtil.getSmoothStyleType` and the `OplusOutline` / `OplusOutlineAdapter` smooth
 *   corner outlines, plus the `COUIVersionUtil.getOSVersionCode()` branches of `getMultiLineRadius`
 *   (OS > 37 `coui_toptips_view_radius`, OS 37 `coui_toptips_view_radius_os16`,
 *   `coui_toptips_view_radius_os15` below, and `couiRoundCornerXL` without smooth corners) and of
 *   `getSingleLineRadius` (`couiRoundCornerFULL`, `couiRoundCornerXXL` without smooth corners); the
 *   port keeps the ColorOS 17 branch.
 * - `setWeight` / the smooth-round-rect weight of `applyRoundCornerToOutline`; the port uses the
 *   squircle surface of this library.
 * - `setClipToOutline(true)` and the outline that `setupOutlineProvider` builds, so the port's
 *   content is not clipped to the rounded shape; the banner's own padding keeps it inside.
 * - `COUIButton` itself, so the `ButtonType` / `MaterialEffectState` / drawable radius state of
 *   `resetDefaultButtonStyle`, `applyAiPaymentButtonStyle`, `applyDefaultSecondaryButtonStyle` and
 *   `applyLegacyIgnoreLinkStyle`, and `COUITextViewCompatUtil.setPressRippleDrawable`; both labels
 *   are drawn as accent text with the `text_ripple_bg_padding_*` press padding.
 * - `OnLinesChangedListener` and `updateRadius`'s callback plumbing; the port derives the
 *   single-line radius from its own measurement instead.
 * - The `id/image`, `id/title`, `id/content`, `id/action`, `id/ignore` and `id/close` view ids and
 *   the `ConstraintSet` rewrites that reposition them; the port lays the same children out with
 *   `Row` / `Column`. `isNeedDefaultMultiTextWithTitle` only reaches its multi-line branch when
 *   there is a start icon, which the port's fit measurement does not require.
 *
 * @param text The message of the banner. `COUIDefaultTopTipsView`'s `content`, a
 *   `COUIMarqueeTextView` styled `couiTextBodyS` (13sp, `coui_spacing_multiplier_body_s`) in
 *   `?attr/couiColorLabelSecondary`, with `coui_toptips_view_content_min_height` (20dp) and the
 *   `coui_toptips_view_content_*` margins. Ignored when [content] is non-null; pass an empty
 *   string for a slot-only banner.
 * @param modifier The modifier to be applied to the banner.
 * @param visible Whether the banner is visible. Changing this plays `COUICustomTopTips`'
 *   `showWithAnim` / `dismissWithAnim` scale.
 * @param title The optional title above [text]. `COUIDefaultTopTipsView`'s `title`, styled
 *   `couiTextHeadlineXS` (16sp, `sans-serif-medium`) in `?attr/couiColorLabelPrimary`,
 *   `maxLines=2`, with `coui_toptips_view_title_min_height` (22dp). `hasTitle()` is what selects
 *   the multi-line corner radius.
 * @param startIcon The optional leading icon. `COUIDefaultTopTipsView`'s `image`, laid out at
 *   `coui_toptips_view_title_icon_size` (36dp) with a title and
 *   `coui_toptips_view_icon_btn_size` (24dp) without, inset by
 *   `coui_toptips_view_icon_start_margin` (16dp).
 * @param actionText The optional label of the `action` button, the primary button of the banner.
 * @param onAction The callback of the `action` button.
 * @param ignoreText The optional label of the `ignore` button, the secondary button.
 *   `setNegativeButton` routes here, and `hasIgnoreText()` is part of `hasDualButtons()`.
 * @param onIgnore The callback of the `ignore` button.
 * @param onClose The callback of the `close` button. `setCloseDrawable` is what makes the native
 *   show it, so a non-null callback is the port's equivalent.
 * @param marqueeEnabled Whether [text] may scroll. Mirrors `COUIMarqueeTopTips.startRoll` /
 *   `stopRoll`, which end at `COUIMarqueeTextView.setMarqueeEnable`.
 * @param style The banner style. `COUIDefaultTopTips.setTopTipsStyle` /
 *   `COUIDefaultTopTipsView.applyTopTipsStyle`, which only feed `resolveTargetLayoutMode`.
 * @param backgroundStyle The solid background. `COUIDefaultTopTips.setTipsBackgroundColor` and
 *   `mLastBackgroundStyle`.
 * @param backgroundOverlay An optional overlay drawn over the fill, mirroring
 *   `COUIDefaultTopTips.setTipsBackgroundDrawable` and its `BackgroundOverlayView`. When set, the
 *   card fill is left transparent the way `applyBackgroundOverlayState` blanks it.
 * @param cornerRadius An explicit corner radius. `null` follows `updateRadius`, i.e.
 *   [TopTipsDefaults.SingleLineCornerRadius] for an untitled single-line banner and
 *   [TopTipsDefaults.CornerRadius] otherwise.
 * @param colors The [TopTipsColors] of the banner.
 * @param content An optional custom message slot, the Compose equivalent of
 *   `COUICustomTopTips`'s fully custom content view. When non-null it replaces the plain [text]
 *   body and keeps the same row layout, padding and [TopTipsColors] as the built-in message, but
 *   it is not measured for the trailing-cluster wraparound decision and is never marqueed.
 *   New in 1.2.0.
 */
@Composable
fun TopTips(
    text: String,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    title: String? = null,
    startIcon: (@Composable () -> Unit)? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    ignoreText: String? = null,
    onIgnore: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    marqueeEnabled: Boolean = true,
    style: TopTipsStyle = TopTipsStyle.Standard,
    backgroundStyle: TopTipsBackgroundStyle = TopTipsBackgroundStyle.Gray,
    backgroundOverlay: (@Composable () -> Unit)? = null,
    cornerRadius: Dp? = null,
    colors: TopTipsColors = TopTipsDefaults.topTipsColors(backgroundStyle),
    content: (@Composable () -> Unit)? = null,
) {
    val visibility = remember { Animatable(if (visible) 1f else 0f) }
    LaunchedEffect(visible) {
        val target = if (visible) 1f else 0f
        if (visibility.value != target) {
            visibility.animateTo(
                targetValue = target,
                animationSpec = tween(SHOW_DISMISS_DURATION_MILLIS, easing = SHOW_DISMISS_EASING),
            )
        }
    }
    if (!visible && visibility.value == 0f) return

    val displayTitle = title?.takeIf { it.isNotEmpty() }
    val action = actionText?.takeIf { it.isNotEmpty() }
    val ignore = ignoreText?.takeIf { it.isNotEmpty() }
    val layoutMode = style.layoutMode()
    val titleStyle = TopTipsDefaults.titleStyle()
    val contentStyle = TopTipsDefaults.textStyle()
    val actionStyle = TopTipsDefaults.actionTextStyle()

    BoxWithConstraints(modifier = modifier.graphicsLayer { scaleY = visibility.value }) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val titleWidthPx = displayTitle?.let { textMeasurer.labelWidthPx(it, titleStyle) } ?: 0
        // A custom slot has no single-line width to measure, so it never forces the trailing
        // cluster onto its own row; the built-in message is measured exactly as before.
        val contentWidthPx = if (content == null) textMeasurer.labelWidthPx(text, contentStyle) else 0
        val leadingWidthPx = with(density) {
            if (startIcon == null) {
                CONTENT_END_MARGIN.toPx()
            } else {
                val iconSize = if (displayTitle == null) {
                    TopTipsDefaults.IconSize
                } else {
                    TopTipsDefaults.TitleIconSize
                }
                val gap = if (displayTitle == null) {
                    TopTipsDefaults.IconSpacing
                } else {
                    TITLE_ICON_TEXT_GAP
                }
                ICON_START_MARGIN.toPx() + iconSize.toPx() + gap.toPx()
            }
        }
        val trailingWidthPx = with(density) {
            val gap = TEXT_TO_BTN_GAP.toPx()
            val labelPadding = (ACTION_PADDING_HORIZONTAL * 2).toPx()
            var width = 0f
            if (ignore != null) {
                width += textMeasurer.labelWidthPx(ignore, actionStyle) + labelPadding + gap
            }
            if (action != null) {
                width += textMeasurer.labelWidthPx(action, actionStyle) + labelPadding + gap
            }
            if (onClose != null) width += TopTipsDefaults.IconSize.toPx() + gap
            width
        }
        val availableWidthPx = with(density) { maxWidth.toPx() }
        val textWidthPx = maxOf(titleWidthPx, contentWidthPx)
        val multiText = leadingWidthPx + textWidthPx + trailingWidthPx > availableWidthPx
        val radius = cornerRadius ?: if (displayTitle == null && !multiText) {
            TopTipsDefaults.SingleLineCornerRadius
        } else {
            TopTipsDefaults.CornerRadius
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .squircleSurface(
                    color = if (backgroundOverlay == null) {
                        colors.containerColor
                    } else {
                        Color.Transparent
                    },
                    cornerRadius = radius,
                ),
        ) {
            backgroundOverlay?.invoke()
        }

        if (multiText) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TopTipsTextArea(
                        title = displayTitle,
                        text = text,
                        content = content,
                        startIcon = startIcon,
                        marqueeEnabled = marqueeEnabled,
                        titleStyle = titleStyle,
                        contentStyle = contentStyle,
                        titleColor = colors.titleColor,
                        contentColor = colors.contentColor,
                    )
                }
                TopTipsTrailing(
                    action = action,
                    onAction = onAction,
                    ignore = ignore,
                    onIgnore = onIgnore,
                    onClose = onClose,
                    layoutMode = layoutMode,
                    actionStyle = actionStyle,
                    colors = colors,
                    expandDualButtons = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = BTN_TOP_MARGIN,
                            start = CONTENT_END_MARGIN,
                            end = CONTENT_END_MARGIN,
                            bottom = if (layoutMode == TopTipsLayoutMode.AiPayment && action != null && ignore != null) {
                                AI_PAYMENT_DUAL_BTN_BOTTOM_MARGIN
                            } else {
                                MULTI_BTN_BOTTOM_MARGIN
                            },
                        ),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopTipsTextArea(
                    title = displayTitle,
                    text = text,
                    content = content,
                    startIcon = startIcon,
                    marqueeEnabled = marqueeEnabled,
                    titleStyle = titleStyle,
                    contentStyle = contentStyle,
                    titleColor = colors.titleColor,
                    contentColor = colors.contentColor,
                )
                TopTipsTrailing(
                    action = action,
                    onAction = onAction,
                    ignore = ignore,
                    onIgnore = onIgnore,
                    onClose = onClose,
                    layoutMode = layoutMode,
                    actionStyle = actionStyle,
                    colors = colors,
                    modifier = Modifier.padding(end = CONTENT_END_MARGIN),
                )
            }
        }
    }
}

@Composable
fun TopTips(
    text: String,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    startIcon: (@Composable () -> Unit)? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    cornerRadius: Dp = TopTipsDefaults.CornerRadius,
    colors: TopTipsColors = TopTipsDefaults.topTipsColors(),
) {
    TopTips(
        text = text,
        modifier = modifier,
        visible = visible,
        startIcon = startIcon,
        actionText = actionText,
        onAction = onAction,
        onClose = onClose,
        // These exist only on the primary overload; passing them pins this forward to the primary
        // instead of re-selecting this same overload (which would recurse forever).
        title = null,
        ignoreText = null,
        onIgnore = null,
        marqueeEnabled = true,
        style = TopTipsStyle.Standard,
        backgroundStyle = TopTipsBackgroundStyle.Gray,
        backgroundOverlay = null,
        cornerRadius = cornerRadius,
        colors = colors,
        content = null,
    )
}

/**
 * The `image` + `title` + `content` block of `coui_default_toptips.xml`: the icon at
 * `coui_toptips_view_icon_start_margin` (16dp) from the start, then a column whose title sits
 * `coui_toptips_view_content_top_margin` (12dp) below the top and
 * `coui_toptips_view_title_content_gap` (0dp) above the content.
 *
 * The message is the built-in marquee text when [content] is null, and the caller's custom slot —
 * `COUICustomTopTips`'s custom content view — otherwise. Both take the same `weight(1f)` column
 * and the same minimum height, so the surrounding row layout, padding and colours are unchanged.
 */
@Composable
private fun RowScope.TopTipsTextArea(
    title: String?,
    text: String,
    content: (@Composable () -> Unit)?,
    startIcon: (@Composable () -> Unit)?,
    marqueeEnabled: Boolean,
    titleStyle: TextStyle,
    contentStyle: TextStyle,
    titleColor: Color,
    contentColor: Color,
) {
    if (startIcon != null) {
        Box(
            modifier = Modifier
                .padding(start = ICON_START_MARGIN)
                .size(
                    if (title == null) TopTipsDefaults.IconSize else TopTipsDefaults.TitleIconSize,
                ),
            contentAlignment = Alignment.Center,
        ) {
            startIcon()
        }
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(
                start = when {
                    startIcon == null -> CONTENT_END_MARGIN
                    title == null -> TopTipsDefaults.IconSpacing
                    else -> TITLE_ICON_TEXT_GAP
                },
                top = TopTipsDefaults.ContentPadding,
                bottom = TopTipsDefaults.ContentPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(TITLE_CONTENT_GAP),
    ) {
        if (title != null) {
            Text(
                text = title,
                color = titleColor,
                style = titleStyle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.defaultMinSize(minHeight = TopTipsDefaults.TitleMinHeight),
            )
        }
        if (content != null) {
            // COUICustomTopTips' custom content view: the same slot, min height and text column,
            // so the message colour and the trailing-cluster layout still apply to it.
            Box(modifier = Modifier.defaultMinSize(minHeight = TopTipsDefaults.TextMinHeight)) {
                content()
            }
        } else {
            MarqueeText(
                text = text,
                color = contentColor,
                style = contentStyle,
                marqueeEnabled = marqueeEnabled,
                modifier = Modifier.defaultMinSize(minHeight = TopTipsDefaults.TextMinHeight),
            )
        }
    }
}

/**
 * The `ignore` / `action` / `close` cluster. `changeLayoutWithDualButtons` hides `close` and sizes
 * both buttons with `configureDualButtonSizing` / `configureDualButtonTextLines` in the AI payment
 * layout; every other layout shows the buttons as they come and keeps `close` at the end.
 */
@Composable
private fun TopTipsTrailing(
    action: String?,
    onAction: (() -> Unit)?,
    ignore: String?,
    onIgnore: (() -> Unit)?,
    onClose: (() -> Unit)?,
    layoutMode: TopTipsLayoutMode,
    actionStyle: TextStyle,
    colors: TopTipsColors,
    modifier: Modifier = Modifier,
    expandDualButtons: Boolean = false,
) {
    if (layoutMode == TopTipsLayoutMode.AiPayment && action != null && ignore != null) {
        // `applyDualButtonsHorizontalConstraints()` keeps the pair as one left-aligned row inside
        // `mContentEndMargin` on both sides, with `coui_toptips_ai_payment_dual_btn_bottom_margin`
        // under it, and gives each button `coui_toptips_ai_payment_btn_min_width`. The two buttons
        // therefore share one baseline and one pair of margins; splitting the row in half keeps that
        // true even when the banner is narrower than twice the minimum width, which is the case for
        // every tip inside a card.
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TEXT_TO_BTN_GAP),
        ) {
            TopTipsActionLabel(
                text = ignore,
                color = colors.actionColor,
                style = actionStyle,
                onClick = onIgnore,
                modifier = if (expandDualButtons) Modifier.weight(1f) else Modifier,
                minWidth = AI_PAYMENT_BTN_MIN_WIDTH,
                minHeight = AI_PAYMENT_BTN_MIN_HEIGHT,
                horizontalPadding = AI_PAYMENT_BTN_HORIZONTAL_PADDING,
            )
            TopTipsActionLabel(
                text = action,
                color = colors.actionColor,
                style = actionStyle,
                onClick = onAction,
                modifier = if (expandDualButtons) Modifier.weight(1f) else Modifier,
                minWidth = AI_PAYMENT_BTN_MIN_WIDTH,
                minHeight = AI_PAYMENT_BTN_MIN_HEIGHT,
                horizontalPadding = AI_PAYMENT_BTN_HORIZONTAL_PADDING,
            )
        }
        return
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        if (ignore != null) {
            TopTipsActionLabel(
                text = ignore,
                color = colors.actionColor,
                style = actionStyle,
                onClick = onIgnore,
            )
            Spacer(modifier = Modifier.width(TEXT_TO_BTN_GAP))
        }
        if (action != null) {
            TopTipsActionLabel(
                text = action,
                color = colors.actionColor,
                style = actionStyle,
                onClick = onAction,
            )
            Spacer(modifier = Modifier.width(TEXT_TO_BTN_GAP))
        }
        if (onClose != null) {
            TopTipsCloseButton(
                backgroundColor = colors.closeButtonColor,
                iconColor = colors.closeIconColor,
                onClick = onClose,
            )
        }
    }
}

/**
 * One of the banner's text buttons: an accent coloured 13sp medium label carrying the capsule press
 * padding `COUITextViewCompatUtil.setPressRippleDrawable` uses.
 *
 * @param text The label of the button.
 * @param color The colour of the label.
 * @param style The text style of the label.
 * @param onClick The callback of the button, or `null` for a non-interactive label.
 * @param modifier The modifier to be applied to the label.
 * @param minWidth The minimum width of the button. `configureDualButtonSizing` applies
 *   `coui_toptips_ai_payment_btn_min_width` to both AI payment buttons.
 * @param minHeight The minimum height of the button. `configureDualButtonTextLines` applies
 *   `coui_toptips_ai_payment_btn_min_height` to both AI payment buttons.
 * @param horizontalPadding The horizontal padding of the button. `configureDualButtonSizing`
 *   overrides it with `coui_toptips_ai_payment_btn_horizontal_padding`.
 */
@Composable
private fun TopTipsActionLabel(
    text: String,
    color: Color,
    style: TextStyle,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    minWidth: Dp = 0.dp,
    minHeight: Dp = 0.dp,
    horizontalPadding: Dp = ACTION_PADDING_HORIZONTAL,
) {
    // The label sits centred in the width it is handed: `COUIButton` centres its text, and the
    // dual-button row hands each button half the row, so a start-aligned label would drift towards
    // the row's leading edge and leave the two labels visibly out of step with their margins.
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = minWidth, minHeight = minHeight)
            .clip(CircleShape)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(
                PaddingValues(
                    horizontal = horizontalPadding,
                    vertical = ACTION_PADDING_VERTICAL,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The `close` `ImageView` of `coui_default_toptips.xml`: a 24dp square inset
 * `coui_toptips_view_btn_margin` (16dp) from the end, redrawn from `coui_ic_toptips_close` — a
 * 24dp viewport holding an `r=9` circle in `?attr/couiColorControls` and a white cross whose arms
 * reach 8.464 and 15.536. The layout's own `android:src` is `ic_embedded_top_tips_close`, the same
 * glyph with an `r=10` circle.
 */
@Composable
private fun TopTipsCloseButton(
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(TopTipsDefaults.IconSize)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Close" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(TopTipsDefaults.IconSize)) {
            drawCircle(
                color = backgroundColor,
                radius = CLOSE_CIRCLE_RADIUS.toPx(),
                center = center,
            )
            val arm = CLOSE_CROSS_ARM.toPx()
            val stroke = CLOSE_CROSS_STROKE_WIDTH.toPx()
            drawLine(
                color = iconColor,
                start = Offset(center.x - arm, center.y - arm),
                end = Offset(center.x + arm, center.y + arm),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = iconColor,
                start = Offset(center.x - arm, center.y + arm),
                end = Offset(center.x + arm, center.y - arm),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * The style of a [TopTips]. `COUIDefaultTopTips.TopTipsStyle`, whose default is `STANDARD` and
 * whose `AI_PAYMENT` member is selected from the `couiTopTipsStyle` attribute of the
 * `COUIDefaultTopTips` styleable.
 */
enum class TopTipsStyle {

    /** `COUIDefaultTopTips.TopTipsStyle.STANDARD`. */
    Standard,

    /** `COUIDefaultTopTips.TopTipsStyle.AI_PAYMENT`. */
    AiPayment,
}

/**
 * The solid fill of a [TopTips]. `COUIDefaultTopTips.BackgroundStyle`, applied by
 * `applySolidBackgroundColor`, whose `mLastBackgroundStyle` starts at `GRAY`.
 */
enum class TopTipsBackgroundStyle {

    /** `COUIDefaultTopTips.BackgroundStyle.WHITE`, filled with `?attr/couiColorCard`. */
    White,

    /** `COUIDefaultTopTips.BackgroundStyle.GRAY`, filled with `?attr/couiColorContainer4`. */
    Gray,
}

/**
 * The layout a [TopTips] content view uses. `COUIDefaultTopTipsView.LayoutMode`, produced by
 * `resolveTargetLayoutMode` from the top tips style and consumed by `applyLayoutConstraints`; the
 * `AI_PAYMENT` mode is the one that reaches `changeLayoutWithDualButtons`.
 */
enum class TopTipsLayoutMode {

    /** `COUIDefaultTopTipsView.LayoutMode.DEFAULT`, inflated from `coui_default_toptips.xml`. */
    Default,

    /**
     * `COUIDefaultTopTipsView.LayoutMode.AI_PAYMENT`, inflated from
     * `coui_ai_payment_toptips.xml`.
     */
    AiPayment,
}

/** Contains default values used by [TopTips]. */
object TopTipsDefaults {

    /**
     * The corner radius of a banner that is not a single untitled line. `getMultiLineRadius` on
     * ColorOS 17 (`COUIVersionUtil.getOSVersionCode() > 37`), which reads
     * `coui_toptips_view_radius` (28dp); the older branches are
     * `coui_toptips_view_radius_os16` (20dp), `coui_toptips_view_radius_os15` (16dp) and
     * `couiRoundCornerXL` (20dp) when smooth corners are unavailable.
     */
    val CornerRadius = 28.dp

    /**
     * The corner radius of an untitled banner whose message fits one line. `getSingleLineRadius`,
     * which reads `couiRoundCornerFULL` (72dp) when smooth corners are supported and
     * `couiRoundCornerXXL` (24dp) otherwise.
     */
    val SingleLineCornerRadius = 72.dp

    /**
     * The vertical padding of the banner. `COUIDefaultTopTipsView.loadDimenResources` reads
     * `coui_toptips_view_content_top_margin` (12dp) into `mContentTopMargin` and puts it above the
     * title and below the content.
     */
    val ContentPadding = 12.dp

    /**
     * The top/bottom padding of the message (COUI `coui_toptips_view_title_top_margin`, 12dp).
     * Retained for binary compatibility with callers built before 1.2.0.
     */
    val VerticalPadding = 12.dp

    /**
     * The size of the leading icon without a title. COUI `coui_toptips_view_icon_btn_size`
     * (24dp).
     */
    val IconSize = 24.dp

    /**
     * The size of the leading icon next to a title. COUI `coui_toptips_view_title_icon_size`
     * (36dp).
     */
    val TitleIconSize = 36.dp

    /**
     * The gap between the leading icon and the content. COUI
     * `coui_toptips_view_content_start_margin` (8dp), `mContentStartMargin`.
     */
    val IconSpacing = 8.dp

    /** The minimum height of the message. COUI `coui_toptips_view_content_min_height` (20dp). */
    val TextMinHeight = 20.dp

    /** The minimum height of the title. COUI `coui_toptips_view_title_min_height` (22dp). */
    val TitleMinHeight = 22.dp

    /**
     * The default title style. `coui_default_toptips.xml` gives `title` the `couiTextHeadlineXS`
     * appearance — 16sp, `sans-serif-medium`, `coui_spacing_multiplier_headline_xs` — in
     * `?attr/couiColorLabelPrimary`; the port uses the theme's `headline1`, the 16sp
     * `couiTextAppearanceHeadline6` tier, with a medium weight.
     */
    @Composable
    fun titleStyle(): TextStyle = COUITheme.textStyles.headline1.copy(
        fontWeight = FontWeight.Medium,
    )

    /**
     * The default message style. `coui_default_toptips.xml` gives `content` the `couiTextBodyS`
     * appearance (13sp, `coui_spacing_multiplier_body_s`) in `?attr/couiColorLabelSecondary`; the
     * port uses the theme's `footnote1`, its 13sp tier.
     */
    @Composable
    fun textStyle(): TextStyle = COUITheme.textStyles.footnote1

    /**
     * The default button label style. `coui_default_toptips.xml` gives `ignore` the 13sp
     * `coui_toptips_view_default_text_size` in `?attr/couiColorPrimaryText`; the port adds the
     * medium weight the native buttons use.
     */
    @Composable
    fun actionTextStyle(): TextStyle = COUITheme.textStyles.footnote1.copy(
        fontWeight = FontWeight.Medium,
    )

    /**
     * The default [TopTipsColors].
     *
     * @param backgroundStyle The solid background of the banner; selects between `couiColorCard`
     *   (#FFFFFFFF light / #1AFFFFFF night) and `couiColorContainer4` (#0A000000 light /
     *   #1AFFFFFF night).
     * @param containerColor The fill of the banner.
     * @param contentColor The colour of the message, `?attr/couiColorLabelSecondary`
     *   (#8A000000 / #8AFFFFFF).
     * @param actionColor The colour of the button labels, `?attr/couiColorPrimaryText`.
     * @param closeButtonColor The fill of the close circle, `?attr/couiColorControls`
     *   (#29000000 / #40FFFFFF).
     * @param closeIconColor The colour of the cross inside the close circle, white in
     *   `coui_ic_toptips_close`.
     * @param titleColor The colour of the title, `?attr/couiColorLabelPrimary`
     *   (#E6000000 / #E6FFFFFF).
     */
    @Composable
    fun topTipsColors(
        backgroundStyle: TopTipsBackgroundStyle = TopTipsBackgroundStyle.Gray,
        containerColor: Color = topTipsContainerColor(backgroundStyle),
        contentColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
        actionColor: Color = COUITheme.colorScheme.primary,
        closeButtonColor: Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
            CLOSE_BUTTON_COLOR_DARK
        } else {
            CLOSE_BUTTON_COLOR_LIGHT
        },
        closeIconColor: Color = Color.White,
        titleColor: Color = COUITheme.colorScheme.onSurface,
    ): TopTipsColors = remember(
        containerColor,
        contentColor,
        actionColor,
        closeButtonColor,
        closeIconColor,
        titleColor,
    ) {
        TopTipsColors(
            containerColor = containerColor,
            contentColor = contentColor,
            actionColor = actionColor,
            closeButtonColor = closeButtonColor,
            closeIconColor = closeIconColor,
            titleColor = titleColor,
        )
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Migrated to the overload that adds backgroundStyle and titleColor parameters.",
        level = DeprecationLevel.HIDDEN,
    )
    @Composable
    fun topTipsColors(
        containerColor: Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) CONTAINER_COLOR_DARK else CONTAINER_COLOR_LIGHT,
        contentColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
        actionColor: Color = COUITheme.colorScheme.primary,
        closeButtonColor: Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) CLOSE_BUTTON_COLOR_DARK else CLOSE_BUTTON_COLOR_LIGHT,
        closeIconColor: Color = Color.White,
    ): TopTipsColors = topTipsColors(
        containerColor = containerColor,
        contentColor = contentColor,
        actionColor = actionColor,
        closeButtonColor = closeButtonColor,
        closeIconColor = closeIconColor,
    )

    /**
     * The solid fill `applySolidBackgroundColor` picks for a [TopTipsBackgroundStyle]:
     * `couiColorCard` (#FFFFFFFF light / #1AFFFFFF night) for [TopTipsBackgroundStyle.White] and
     * `couiColorContainer4` (#0A000000 light / #1AFFFFFF night) for
     * [TopTipsBackgroundStyle.Gray].
     */
    @Composable
    fun topTipsContainerColor(backgroundStyle: TopTipsBackgroundStyle): Color {
        val dark = COUITheme.colorScheme.background.luminance() < 0.5f
        return when (backgroundStyle) {
            TopTipsBackgroundStyle.White -> if (dark) CARD_COLOR_DARK else CARD_COLOR_LIGHT
            TopTipsBackgroundStyle.Gray -> if (dark) CONTAINER_COLOR_DARK else CONTAINER_COLOR_LIGHT
        }
    }
}

/**
 * Colors used by a [TopTips].
 *
 * @param containerColor The background color of the banner.
 * @param contentColor The color of the message.
 * @param actionColor The color of the button labels.
 * @param closeButtonColor The fill color of the close button circle.
 * @param closeIconColor The color of the cross inside the close button.
 * @param titleColor The color of the title.
 */
@Immutable
data class TopTipsColors(
    val containerColor: Color,
    val contentColor: Color,
    val actionColor: Color,
    val closeButtonColor: Color,
    val closeIconColor: Color,
    val titleColor: Color = contentColor,
) {
    /**
     * The five-color form, kept so callers written before [titleColor] existed still compile; the
     * title color falls back to [contentColor].
     */
    constructor(
        containerColor: Color,
        contentColor: Color,
        actionColor: Color,
        closeButtonColor: Color,
        closeIconColor: Color,
    ) : this(
        containerColor = containerColor,
        contentColor = contentColor,
        actionColor = actionColor,
        closeButtonColor = closeButtonColor,
        closeIconColor = closeIconColor,
        titleColor = contentColor,
    )

    /** The five-color [copy], kept for binary compatibility with callers built before 1.2.0. */
    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Migrated to the copy that also takes titleColor.",
        level = DeprecationLevel.HIDDEN,
    )
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        actionColor: Color = this.actionColor,
        closeButtonColor: Color = this.closeButtonColor,
        closeIconColor: Color = this.closeIconColor,
    ): TopTipsColors = copy(
        containerColor = containerColor,
        contentColor = contentColor,
        actionColor = actionColor,
        closeButtonColor = closeButtonColor,
        closeIconColor = closeIconColor,
        titleColor = titleColor,
    )
}

/** The single-line width of a label measured with [style], in pixels. */
private fun TextMeasurer.labelWidthPx(text: String, style: TextStyle): Int = measure(
    AnnotatedString(text),
    style = style,
    maxLines = 1,
).size.width

/** `COUIDefaultTopTipsView.resolveTargetLayoutMode`, which only reads `mTopTipsStyle`. */
private fun TopTipsStyle.layoutMode(): TopTipsLayoutMode = when (this) {
    TopTipsStyle.Standard -> TopTipsLayoutMode.Default
    TopTipsStyle.AiPayment -> TopTipsLayoutMode.AiPayment
}

/** `couiColorCard` in light themes (`coui_color_card` #FFFFFFFF). */
private val CARD_COLOR_LIGHT = Color(0xFFFFFFFF)

/** `couiColorCard` in dark themes (`coui_color_card` night #1AFFFFFF). */
private val CARD_COLOR_DARK = Color(0x1AFFFFFF)

/** `couiColorContainer4` in light themes (`coui_color_container4` #0A000000). */
private val CONTAINER_COLOR_LIGHT = Color(0x0A000000)

/** `couiColorContainer4` in dark themes (`coui_color_container4` night #1AFFFFFF). */
private val CONTAINER_COLOR_DARK = Color(0x1AFFFFFF)

/** `couiColorControls` in light themes (`coui_color_controls` #29000000). */
private val CLOSE_BUTTON_COLOR_LIGHT = Color(0x29000000)

/** `couiColorControls` in dark themes (`coui_color_controls` night #40FFFFFF). */
private val CLOSE_BUTTON_COLOR_DARK = Color(0x40FFFFFF)

/** COUI `coui_toptips_view_icon_start_margin` (16dp), `mIconStartMargin`. */
private val ICON_START_MARGIN = 16.dp

/** COUI `coui_toptips_view_title_icon_text_gap` (12dp), `mTitleIconTextGap`. */
private val TITLE_ICON_TEXT_GAP = 12.dp

/** COUI `coui_toptips_view_content_end_margin` (16dp), `mContentEndMargin`. */
private val CONTENT_END_MARGIN = 16.dp

/** COUI `coui_toptips_view_title_content_gap` (0dp), `mTitleContentGap`. */
private val TITLE_CONTENT_GAP = 0.dp

/** COUI `coui_toptips_view_text_to_btn_gap` (8dp), `mTextToBtnGap`. */
private val TEXT_TO_BTN_GAP = 8.dp

/** COUI `coui_toptips_view_btn_top_margin` (8dp), `mBtnTopMargin`. */
private val BTN_TOP_MARGIN = 8.dp

/** COUI `coui_toptips_view_multi_btn_text_bottom_margin` (12dp), `mMultiBtnBottomMargin`. */
private val MULTI_BTN_BOTTOM_MARGIN = 12.dp

/** COUI `coui_toptips_ai_payment_btn_min_width` (144dp), `mDualBtnMinWidth`. */
private val AI_PAYMENT_BTN_MIN_WIDTH = 144.dp

/** COUI `coui_toptips_ai_payment_btn_min_height` (32dp), `mDualBtnMinHeight`. */
private val AI_PAYMENT_BTN_MIN_HEIGHT = 32.dp

/** COUI `coui_toptips_ai_payment_btn_horizontal_padding` (12dp), `mHorButHorPadding`. */
private val AI_PAYMENT_BTN_HORIZONTAL_PADDING = 12.dp

/** COUI `coui_toptips_ai_payment_dual_btn_bottom_margin`, 16dp, under the dual-button row. */
private val AI_PAYMENT_DUAL_BTN_BOTTOM_MARGIN = 16.dp

/** COUI `text_ripple_bg_padding_horizontal` (12dp), the press area of a text button. */
private val ACTION_PADDING_HORIZONTAL = 12.dp

/** COUI `text_ripple_bg_padding_vertical` (4dp), the press area of a text button. */
private val ACTION_PADDING_VERTICAL = 4.dp

/** The radius of the close circle (`coui_ic_toptips_close`: `r=9` in a 24dp viewport). */
private val CLOSE_CIRCLE_RADIUS = 9.dp

/**
 * Half the reach of each cross arm in `coui_ic_toptips_close`: the rounded stroke spans
 * 8.464..15.536, so with the 1.6dp stroke caps the line endpoints sit 2.736dp from the center.
 */
private val CLOSE_CROSS_ARM = 2.736.dp

/** The stroke width of the cross in `coui_ic_toptips_close`. */
private val CLOSE_CROSS_STROKE_WIDTH = 1.6.dp

/** `COUICustomTopTips.showWithAnim` / `dismissWithAnim`: `setDuration(250)`. */
private const val SHOW_DISMISS_DURATION_MILLIS = 250

/**
 * The show/dismiss easing: Android's default `AccelerateDecelerateInterpolator`, closely matched by
 * the symmetric ease-in-out cubic bezier.
 */
private val SHOW_DISMISS_EASING = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
