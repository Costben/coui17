// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * How much of the free vertical space [EmptyState] leaves above its content, chosen by the
 * `emptyViewSizeType` of ColorOS's empty-state design.
 */
enum class EmptyStateSize {
    /** Picks the bucket from the measured constraints, like the auto size type (0). */
    Auto,

    /** The small size type (1): a window shorter than 200dp; hides the illustration. */
    Small,

    /** The medium size type (2): narrower than 320dp or shorter than 480dp. */
    Medium,

    /** The large size type (3). */
    Large,
}

/**
 * An empty state page with ColorOS style: a centered illustration, a title and a subtitle in a
 * max-height scroll area, and an optional accent-colored action below them.
 *
 * The content group is not centered vertically — the design places it at a fraction of the free
 * space ([EmptyStateDefaults.MarginTopHeightRatioSmall] / [EmptyStateDefaults.MarginTopHeightRatioNormal]),
 * so on a phone-sized page it sits a little above the middle. Reproduce that by giving the
 * [EmptyState] a fixed height (`fillMaxSize`, `height`, a `weight`, …); when its height is merely
 * bounded or unbounded it lays its content out at the top, exactly like a height measure spec that
 * is not EXACTLY falls back to the group height.
 *
 * @param title The title of the [EmptyState], centered, at most two lines
 *   (COUI couiTextHeadlineXS: 16sp, medium, line spacing multiplier 1.05).
 * @param modifier The modifier to be applied to the [EmptyState].
 * @param subtitle The subtitle below the title, centered, at most three lines
 *   (COUI couiTextBodyXS: 12sp, line spacing multiplier 1.0). Hidden when null or empty.
 * @param actionText The label of the action below the content (COUI couiTextButtonM: 14sp, medium,
 *   line spacing multiplier 1.1). Hidden when null or empty.
 * @param onActionClick The callback when the action is clicked.
 * @param illustration The illustration above the text, laid out in the device's
 *   [EmptyStateDefaults.IllustrationWidth] x [EmptyStateDefaults.IllustrationHeight] box. The COUI
 *   library ships no default asset (`rawAnimRes` / `imageRes` default to -1 and the layout sets
 *   none), so nothing is drawn when this is null. On the device the box hosts an oplus-anim
 *   playable set with `COUIEmptyStateView#setRawAnimRes`, or a still image set with
 *   `setImageRes`; it is hidden entirely in [EmptyStateSize.Small], where the device zeroes the
 *   anim size.
 * @param size The [EmptyStateSize] of the [EmptyState]. [EmptyStateSize.Auto] resolves the bucket
 *   from the constraints, as the device does from its measure specs.
 * @param colors The [EmptyStateColors] of the [EmptyState].
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    illustration: (@Composable () -> Unit)? = null,
    size: EmptyStateSize = EmptyStateSize.Auto,
    colors: EmptyStateColors = EmptyStateDefaults.emptyStateColors(),
) {
    BoxWithConstraints(modifier = modifier) {
        val resolvedSize = if (size == EmptyStateSize.Auto) {
            EmptyStateDefaults.sizeType(maxWidth, maxHeight)
        } else {
            size
        }
        val topRatio = EmptyStateDefaults.marginTopHeightRatio(resolvedSize)
        // COUIEmptyStateView#onMeasure keeps the ratio placement only when its height measure spec is
        // EXACTLY; anything looser measures the view at its content height and the margin collapses.
        if (constraints.hasFixedHeight) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(topRatio))
                EmptyStateContent(
                    title = title,
                    subtitle = subtitle,
                    actionText = actionText,
                    onActionClick = onActionClick,
                    showIllustration = resolvedSize != EmptyStateSize.Small,
                    illustration = illustration,
                    colors = colors,
                )
                Spacer(Modifier.weight(1f - topRatio))
            }
        } else {
            EmptyStateContent(
                title = title,
                subtitle = subtitle,
                actionText = actionText,
                onActionClick = onActionClick,
                showIllustration = resolvedSize != EmptyStateSize.Small,
                illustration = illustration,
                colors = colors,
            )
        }
    }
}

/** The `emptyStateGroup` LinearLayout of `coui_component_empty_state.xml`. */
@Composable
private fun EmptyStateContent(
    title: String,
    subtitle: String?,
    actionText: String?,
    onActionClick: (() -> Unit)?,
    showIllustration: Boolean,
    illustration: (@Composable () -> Unit)?,
    colors: EmptyStateColors,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showIllustration && illustration != null) {
            Box(
                modifier = Modifier.size(
                    width = EmptyStateDefaults.IllustrationWidth,
                    height = EmptyStateDefaults.IllustrationHeight,
                ),
                contentAlignment = Alignment.Center,
            ) {
                illustration()
            }
        }
        // empty_view_content: a COUIMaxHeightScrollView capped at
        // coui_component_empty_text_scroll_max_height (92dp) with hidden scrollbars.
        Box(
            modifier = Modifier
                .width(EmptyStateDefaults.ContentWidth)
                .heightIn(max = EmptyStateDefaults.ContentMaxHeight)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = EmptyStateDefaults.titleTextStyle(),
                    color = colors.titleColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = EmptyStateDefaults.SubtitleMarginTop),
                        style = EmptyStateDefaults.subtitleTextStyle(),
                        color = colors.subtitleColor,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (!actionText.isNullOrEmpty()) {
            // empty_view_action: a bare accent TextView whose ripple comes from
            // COUITextViewCompatUtil.setPressRippleDrawable (?couiColorPress, masked to the text).
            Text(
                text = actionText,
                modifier = Modifier
                    .padding(top = EmptyStateDefaults.ActionMarginTop)
                    .clickable(role = Role.Button, onClick = { onActionClick?.invoke() })
                    .padding(
                        horizontal = EmptyStateDefaults.ActionRipplePaddingHorizontal,
                        vertical = EmptyStateDefaults.ActionRipplePaddingVertical,
                    ),
                style = EmptyStateDefaults.actionTextStyle(),
                color = colors.actionColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Contains default values used by [EmptyState]. */
object EmptyStateDefaults {

    /**
     * The width of the illustration box
     * (COUI `coui_component_empty_anim_view_width_normal`, 120dp).
     */
    val IllustrationWidth = 120.dp

    /**
     * The height of the illustration box
     * (COUI `coui_component_empty_anim_view_height_normal`, 100dp).
     */
    val IllustrationHeight = 100.dp

    /**
     * The width of the title and subtitle group
     * (COUI `coui_component_empty_group_width`, 280dp).
     */
    val ContentWidth = 280.dp

    /**
     * The height the title and subtitle group scrolls within
     * (COUI `coui_component_empty_text_scroll_max_height`, 92dp).
     */
    val ContentMaxHeight = 92.dp

    /**
     * Top margin of the subtitle
     * (COUI `coui_component_empty_view_subtitle_margin_top`, the layout writes it inline as 2dp).
     */
    val SubtitleMarginTop = 2.dp

    /**
     * Top margin of the action. The layout writes it inline as 8dp;
     * `coui_component_empty_view_action_margin_top` (6dp) is not the value this layout uses.
     */
    val ActionMarginTop = 8.dp

    /**
     * Horizontal padding the action carries so its ripple has room
     * (COUI `text_ripple_bg_padding_horizontal`, 12dp).
     */
    val ActionRipplePaddingHorizontal = 12.dp

    /**
     * Vertical padding the action carries so its ripple has room
     * (COUI `text_ripple_bg_padding_vertical`, 4dp).
     */
    val ActionRipplePaddingVertical = 4.dp

    /**
     * Fraction of the free space left above the content in a short window
     * (`HEIGHT_RATIO_SMALL`, 0.5).
     */
    val MarginTopHeightRatioSmall = 0.5f

    /**
     * Fraction of the free space left above the content otherwise
     * (`HEIGHT_RATIO_NORMAL`, 0.45).
     */
    val MarginTopHeightRatioNormal = 0.45f

    /**
     * Height below which [sizeType] answers [EmptyStateSize.Small]
     * (COUI `coui_component_height_threshold_small`, 200dp).
     */
    val HeightThresholdSmall = 200.dp

    /**
     * Height below which [sizeType] answers [EmptyStateSize.Medium]
     * (COUI `coui_component_height_threshold_medium`, 480dp).
     */
    val HeightThresholdMedium = 480.dp

    /**
     * Width below which [sizeType] answers [EmptyStateSize.Medium]
     * (COUI `coui_component_width_threshold_medium`, 320dp).
     */
    val WidthThresholdMedium = 320.dp

    /**
     * Buckets the available size the way `COUIEmptyStateView#getEmptyStateGroupSizeType` does.
     *
     * The device compares its measured pixels against the same dimens resolved to pixels, so
     * comparing dp here is equivalent at any density.
     */
    fun sizeType(width: Dp, height: Dp): EmptyStateSize = when {
        height < HeightThresholdSmall -> EmptyStateSize.Small
        width < WidthThresholdMedium || height < HeightThresholdMedium -> EmptyStateSize.Medium
        else -> EmptyStateSize.Large
    }

    /**
     * The fraction of the free vertical space that stays above the content, per
     * `COUIEmptyStateView#getMarginTopHeightRatio`.
     */
    fun marginTopHeightRatio(size: EmptyStateSize): Float = if (size == EmptyStateSize.Small) MarginTopHeightRatioSmall else MarginTopHeightRatioNormal

    /** The title text style: COUI couiTextHeadlineXS (16sp, medium, line spacing multiplier 1.05). */
    @Composable
    fun titleTextStyle(): TextStyle = COUITheme.textStyles.headline2.copy(
        lineHeight = 1.05f.em,
        fontWeight = FontWeight.Medium,
    )

    /**
     * The subtitle text style: COUI couiTextBodyXS (12sp, line spacing multiplier 1.0). The theme
     * has no 12sp tier, so its 13sp `COUITheme.textStyles.footnote1` carries the line height under
     * the native size.
     */
    @Composable
    fun subtitleTextStyle(): TextStyle = COUITheme.textStyles.footnote1.copy(
        fontSize = 12.sp,
        lineHeight = 1.0f.em,
    )

    /** The action text style: COUI couiTextButtonM (14sp, medium, line spacing multiplier 1.1). */
    @Composable
    fun actionTextStyle(): TextStyle = COUITheme.textStyles.body2.copy(
        lineHeight = 1.1f.em,
        fontWeight = FontWeight.Medium,
    )

    /**
     * The default [EmptyStateColors]: title on couiColorLabelPrimary, subtitle on
     * couiColorLabelSecondary and action on couiColorPrimaryText.
     */
    @Composable
    fun emptyStateColors(
        titleColor: Color = COUITheme.colorScheme.onSurface,
        subtitleColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
        actionColor: Color = COUITheme.colorScheme.primary,
    ): EmptyStateColors = remember(titleColor, subtitleColor, actionColor) {
        EmptyStateColors(
            titleColor = titleColor,
            subtitleColor = subtitleColor,
            actionColor = actionColor,
        )
    }
}

/**
 * Colors used by an [EmptyState].
 *
 * `actionColor` follows the theme's primary. COUI resolves couiColorPrimaryText per accent theme:
 * `coui_color_primary_text_blue` is #0080FF, which the theme's light primary matches exactly, while
 * the theme's night primary is #1A8CFF against the token's #5C9DFF. The theme token wins so the
 * component follows the user's accent; only the night value is off.
 *
 * @param titleColor The color of the title (couiColorLabelPrimary).
 * @param subtitleColor The color of the subtitle (couiColorLabelSecondary).
 * @param actionColor The color of the action (couiColorPrimaryText).
 */
@Immutable
data class EmptyStateColors(
    val titleColor: Color,
    val subtitleColor: Color,
    val actionColor: Color,
)
