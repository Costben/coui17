// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * A small text corner badge with ColorOS style.
 *
 * The design is a `LinearLayout` whose background is a `MaterialShapeDrawable` with four
 * independent corner radii; [TagView] draws the same silhouette with [RoundedCornerShape] and an
 * optional [strokeWidth] outline, wraps a single-line label that is ellipsized at the end, and
 * offers an optional leading and trailing image slot. A "NEW" / "BETA" badge is text only.
 *
 * The label uses [TagViewDefaults.textStyle] (`couiTextTag`: 10sp, `sans-serif-medium`), the
 * leading slot is the fixed 5dp x 10dp image of the layout with 3dp top/bottom and 2dp end margins,
 * and the container carries 3dp at each side. The trailing slot is `wrap_content` in the layout, so
 * the caller's [trailingIcon] is drawn at its intrinsic size.
 *
 * Values that the reference dump did not resolve are called out in KDoc and in
 * [TagViewDefaults]; the port never substitutes a made-up number.
 *
 * Not ported, because no consumer in this library reaches them:
 * - the four per-corner radii of `COUITagBackgroundView` (`couiTagTLCornerRadius`,
 *   `couiTagTRCornerRadius`, `couiTagBLCornerRadius`, `couiTagBRCornerRadius`); the port exposes a
 *   single [TagViewDefaults.CornerRadius] because every recovered consumer uses the uniform
 *   `couiTagCornerRadius`;
 * - `COUITagBackgroundView.onAttachedToWindow` setting the parent's `clipChildren = false` and the
 *   `Path` clip of its `dispatchDraw` (a Compose parent does not clip its children by default);
 * - `COUITagView.setImageBitmap` / `setImageDrawable` / `setImageResoure` and their left-image
 *   twins, which are the imperative setters of the same two image slots exposed as parameters here.
 *
 * @param text The label of the badge, single line, ellipsized at the end
 *   (`coui_tag_view_layout.xml` sets `android:ellipsize="end"` and `android:maxLines="1"`).
 * @param modifier The modifier to be applied to the badge.
 * @param leadingIcon The optional leading image, laid out in the fixed
 *   [TagViewDefaults.LeadingIconWidth] x [TagViewDefaults.LeadingIconHeight] slot of
 *   `coui_tag_view_layout.xml` (`tagLeftImageView`).
 * @param trailingIcon The optional trailing image (`tagImageView`, `wrap_content`).
 * @param cornerRadius The uniform corner radius of the badge. COUI `couiTagCornerRadius`
 *   ([TagViewDefaults.CornerRadius]).
 * @param strokeWidth The width of the background outline. COUI `couiTagStrokeWidth`
 *   ([TagViewDefaults.StrokeWidth]).
 * @param colors The [TagViewColors] of the badge.
 */
@Composable
fun TagView(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    cornerRadius: Dp = TagViewDefaults.CornerRadius,
    strokeWidth: Dp = TagViewDefaults.StrokeWidth,
    colors: TagViewColors = TagViewDefaults.tagViewColors(),
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    Row(
        modifier = modifier
            .background(color = colors.containerColor, shape = shape)
            .then(
                if (strokeWidth > 0.dp) Modifier.border(strokeWidth, colors.strokeColor, shape) else Modifier,
            )
            .padding(horizontal = TagViewDefaults.HorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = colors.contentColor,
                modifier = Modifier
                    .padding(
                        top = TagViewDefaults.LeadingIconVerticalMargin,
                        bottom = TagViewDefaults.LeadingIconVerticalMargin,
                        end = TagViewDefaults.LeadingIconEndMargin,
                    )
                    .size(
                        width = TagViewDefaults.LeadingIconWidth,
                        height = TagViewDefaults.LeadingIconHeight,
                    ),
            )
        }
        Text(
            text = text,
            color = colors.contentColor,
            style = TagViewDefaults.textStyle(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = colors.contentColor,
            )
        }
    }
}

/**
 * Colors used by a [TagView], mapping the `COUITagBackgroundView` / `COUITagView` attributes.
 *
 * @param containerColor The fill of the badge. COUI `couiTagBackgroundColor`, whose default is
 *   `?attr/couiColorBackgroundWithTag`.
 * @param contentColor The color of the label and of the two image slots. COUI
 *   `couiTagViewTextColor`, whose default is `color/coui_color_white` (`#ffffffff`).
 * @param strokeColor The color of the outline. COUI `couiTagStrokeColor`, whose default is fully
 *   transparent (`ColorStateList.valueOf(0)`).
 */
@Immutable
data class TagViewColors(
    val containerColor: Color,
    val contentColor: Color,
    val strokeColor: Color,
)

/** Contains default values used by [TagView]. */
object TagViewDefaults {

    /**
     * The uniform corner radius of the badge. COUI `couiTagCornerRadius`, whose
     * `COUITagBackgroundView` default is `0dp`. The tag has no `Widget.COUI.*` style in the
     * Settings / Clock resource dumps, so the native radius is always supplied by the app; the one
     * recovered consumer, Dialer's `customize_popup_list_window_item.xml`, passes
     * `app:couiTagCornerRadius="4dp"`.
     */
    val CornerRadius = 0.dp

    /**
     * The width of the background outline. COUI `couiTagStrokeWidth`, default `0dp` (no outline).
     */
    val StrokeWidth = 0.dp

    /**
     * The start / end padding of the badge. `coui_tag_view_layout.xml` sets
     * `android:paddingStart="3dp"` and `android:paddingEnd="3dp"` on the root
     * `COUITagBackgroundView`.
     */
    val HorizontalPadding = 3.dp

    /** The width of the leading image slot. `coui_tag_view_layout.xml` `tagLeftImageView` (5dp). */
    val LeadingIconWidth = 5.dp

    /** The height of the leading image slot. `coui_tag_view_layout.xml` `tagLeftImageView` (10dp). */
    val LeadingIconHeight = 10.dp

    /**
     * The top / bottom margin of the leading image slot. `coui_tag_view_layout.xml`
     * `tagLeftImageView` `android:layout_marginTop` / `android:layout_marginBottom` (3dp).
     */
    val LeadingIconVerticalMargin = 3.dp

    /**
     * The end margin of the leading image slot. `coui_tag_view_layout.xml` `tagLeftImageView`
     * `android:layout_marginEnd` (2dp).
     */
    val LeadingIconEndMargin = 2.dp

    /**
     * The size of the label. COUI `coui_default_tag_textsize` (10dp) is applied through
     * `COUITagView.setTextSize(0, px)`, while the `couiTextTag` style the layout gives the label
     * declares 10sp; the port uses the declared 10sp.
     */
    val TextSize = 10.sp

    /**
     * The label style of the badge. COUI `couiTextTag` (`style/couiTextTag`): 10sp,
     * `sans-serif-medium` (the layout gives the label this style), with
     * `android:includeFontPadding="false"` and `coui_spacing_multiplier_tag` = 1.
     */
    @Composable
    fun textStyle(): TextStyle = COUITheme.textStyles.footnote2.copy(
        fontSize = TextSize,
        fontWeight = FontWeight.Medium,
        lineHeight = TextUnit.Unspecified,
    )

    /**
     * The default [TagViewColors].
     *
     * The design defaults are `couiTagBackgroundColor` = `?attr/couiColorBackgroundWithTag` and
     * `couiTagViewTextColor` = `color/coui_color_white` (`#ffffffff`).
     * The background attribute is **not resolvable from the captured tokens**: white text would be
     * invisible on the resulting light fill anyway. The port therefore adopts the concrete pair a
     * real consumer passes — `?attr/couiColorContainer8` and
     * `@color/coui_color_label_secondary` — which resolves to the container8 fill (`#14000000` light
     * / `#1affffff` dark) and the secondary label colour
     * (`COUITheme.colorScheme.onSurfaceSecondary`, `#8a000000` / `#8affffff`).
     */
    @Composable
    fun tagViewColors(
        containerColor: Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) TagContainer8Dark else TagContainer8Light,
        contentColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
        strokeColor: Color = Color.Transparent,
    ): TagViewColors = remember(containerColor, contentColor, strokeColor) {
        TagViewColors(
            containerColor = containerColor,
            contentColor = contentColor,
            strokeColor = strokeColor,
        )
    }
}

/** `couiColorContainer8` in light themes (`color/coui_color_container8` #14000000). */
private val TagContainer8Light = Color(0x14000000)

/** `couiColorContainer8` in dark themes (`color/coui_color_container8_dark` #1affffff). */
private val TagContainer8Dark = Color(0x1AFFFFFF)
