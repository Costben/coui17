// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The functional button beside an expanded search bar: `SearchBarFunctionButton` of `COUISearchBar`.
 *
 * It is a 40dp capsule (`coui_search_bar_normal_background_height`) filled with the same material as
 * the search field. Its ends follow the ColorOS capsule curve — a circular arc joined to a
 * 1.5-radius easing tail — rather than a circular stadium.
 *
 * `COUISearchBar.updateCausticShadow` attaches the light to the functional button as well as to the
 * field, so the button casts its own caustic band while the search bar is being edited.
 *
 * COUI gives it the standard touch motion, so pressing shrinks the capsule and lights it from the
 * touch point.
 *
 * @param onClick the callback invoked when the button is clicked.
 * @param modifier the [Modifier] to be applied to the button.
 * @param enabled whether the button responds to input.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing presses.
 * @param containerColor the capsule fill; defaults to the search field's own material.
 * @param glowColor the caustic light cast below the capsule, or `null` to disable it. It is clipped
 *   to the outside of the capsule, so the fill is never brightened.
 * @param content the content to be displayed inside the button capsule.
 */
@Composable
fun SearchBarActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    containerColor: Color = SearchBarDefaults.searchBarColors().backgroundColor,
    glowColor: Color? = null,
    content: @Composable () -> Unit,
) {
    SearchBarActionButtonSurface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        containerColor = containerColor,
        glowColor = glowColor,
        stroke = SearchBarDefaults.searchBarStroke(),
        content = content,
    )
}

/**
 * [SearchBarActionButton] without the public colour defaults, so a caller that paints the material
 * itself can switch the capsule's own fill, rim and caustic off.
 *
 * The fused [SearchBar] chains this button and the field into one melt ball: the merged surface
 * carries one fill and one rim along the union's outline, so the button contributes only its label
 * and its touch motion.
 *
 * @param stroke the material rim drawn over the capsule, or `null` to draw no rim.
 */
@Composable
internal fun SearchBarActionButtonSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    motionState: COUITouchMotionState? = null,
    containerColor: Color = SearchBarDefaults.searchBarColors().backgroundColor,
    glowColor: Color? = null,
    stroke: MaterialStrokeParams? = SearchBarDefaults.searchBarStroke(),
    content: @Composable () -> Unit,
) {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    // The system capsule: the fill, the glow cut-out and the press clip all trace the arc plus the
    // 1.5-radius easing tail measured on ColorOS 17, not a circular stadium.
    val cornerWeightFor: (Float, Float) -> Float = remember {
        { width, height ->
            if (width == height) MaterialShadowEdgeDefaults.CapsuleCornerWeight else MaterialShadowEdgeDefaults.CornerWeight
        }
    }
    val edgeModifier = if (stroke != null) {
        Modifier.materialShadowEdgeBySize(stroke = stroke, cornerWeightFor = cornerWeightFor)
    } else {
        Modifier
    }
    Box(modifier = modifier.height(SearchBarActionButtonDefaults.Height)) {
        if (glowColor != null) {
            // Same outside-only layer the field uses: the capsule is punched out of the light.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        val capsule = Path().apply { addSegmentCapsule(size.toRect()) }
                        clipPath(capsule, ClipOp.Difference) { this@drawWithContent.drawContent() }
                    },
            ) {
                SearchBarCausticGlow(color = glowColor, modifier = Modifier.matchParentSize())
            }
        }
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = SearchBarActionButtonDefaults.Height)
                .fillMaxHeight()
                .clip(SegmentCapsuleShape)
                .background(color = containerColor)
                .then(edgeModifier)
                .couiTouchMotion(
                    enabled = enabled,
                    shape = SegmentCapsuleShape,
                    motionState = motionState,
                )
                .clickable(
                    interactionSource = actualInteractionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/**
 * The functional button beside an expanded search bar: `SearchBarFunctionButton` of `COUISearchBar`,
 * which carries the "取消"/"Cancel" label.
 *
 * It is a 40dp capsule (`coui_search_bar_normal_background_height`) filled with the same material as
 * the search field, with the 14sp `couiTextAppearanceButton` label at 90% `onSurface`. The native
 * button is inset by `coui_search_bar_functional_button_text_padding_horizontal` (16dp) and separated
 * from the field by `coui_search_bar_functional_button_start_gap` (12dp).
 *
 * `COUISearchBar.updateCausticShadow` attaches the light to the functional button as well as to the
 * field, so the button casts its own caustic band while the search bar is being edited.
 *
 * COUI gives it the standard touch motion, so pressing shrinks the capsule and lights it from the
 * touch point.
 *
 * @param text the button label, usually `coui_search_view_cancel`.
 * @param onClick the callback invoked when the button is clicked.
 * @param modifier the [Modifier] to be applied to the button.
 * @param enabled whether the button responds to input.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing presses.
 * @param containerColor the capsule fill; defaults to the search field's own material.
 * @param contentColor the label colour, `coui_searchview_cancel_button_color` (`uk_black_ninety_percent`).
 * @param glowColor the caustic light cast below the capsule, or `null` to disable it. It is clipped
 *   to the outside of the capsule, so the fill is never brightened.
 */
@Composable
fun SearchBarActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    containerColor: Color = SearchBarDefaults.searchBarColors().backgroundColor,
    contentColor: Color = SearchBarActionButtonDefaults.contentColor(),
    glowColor: Color? = null,
) {
    SearchBarActionButtonSurface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        containerColor = containerColor,
        glowColor = glowColor,
        stroke = SearchBarDefaults.searchBarStroke(),
    ) {
        SearchBarActionLabel(text = text, contentColor = contentColor)
    }
}

/** The "取消"/"Cancel" label of [SearchBarActionButton], with its 16dp horizontal inset. */
@Composable
internal fun SearchBarActionLabel(text: String, contentColor: Color) {
    Box(
        modifier = Modifier
            .widthIn(max = SearchBarActionButtonDefaults.MaxWidth)
            .padding(horizontal = SearchBarActionButtonDefaults.HorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = COUITheme.textStyles.body2.copy(fontWeight = FontWeight.Medium),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Contains default values used by [SearchBarActionButton]. */
object SearchBarActionButtonDefaults {
    /** The capsule height, COUI `coui_search_bar_normal_background_height` (40dp). */
    val Height = 40.dp

    /**
     * The widest the capsule may become, COUI `coui_search_bar_functional_button_max_width`
     * (152dp). `COUISearchBar.measureFunctionalButton` measures the button with
     * `MeasureSpec.makeMeasureSpec(mFunctionButtonMaxWidth, AT_MOST)`, so a longer label
     * ellipsizes instead of pushing the field off the bar.
     */
    val MaxWidth = 152.dp

    /** The horizontal label inset, COUI `coui_search_bar_functional_button_text_padding_horizontal`. */
    val HorizontalPadding = 16.dp

    /** The gap before the capsule, COUI `coui_search_bar_functional_button_start_gap` (12dp). */
    val StartGap = 12.dp

    /**
     * The label colour, COUI `coui_searchview_cancel_button_color` / `uk_black_ninety_percent`:
     * 90% of `onSurface`, not the full-strength label colour.
     */
    @Composable
    fun contentColor(): Color = COUITheme.colorScheme.onSurface.copy(alpha = 0.9f)
}
