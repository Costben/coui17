// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The ColorOS 17 speed-dial floating button: one round main button that opens a stack of labelled
 * child buttons.
 *
 * It mirrors `com.coui.appcompat.floatingactionbutton.COUIFloatingButton` with its
 * `COUIFloatingButtonItem` rows (`COUIFloatingButtonLabel`, inflated from
 * `res/layout/coui_floating_button_item_label.xml`) and the state machine of
 * `COUIFloatingButton$InstanceState`:
 * - the main button is 56dp (`coui_floating_button_size`, also the `android:layout_width` of
 *   `style/FloatingButton.Icon.Medium`), carries the `attr/mainFloatingButtonSrc` glyph and rotates
 *   it [FloatingButtonDefaults.EXPAND_ICON_ROTATION] degrees while the menu is open, over
 *   [FloatingButtonDefaults.ROTATE_DURATION];
 * - each row is a 56dp child button with the label chip
 *   [FloatingButtonDefaults.LabelSpacing] to its start. A row's bottom edge sits
 *   `getTotalLabelHeight(index)` above the container's bottom, `(index * 72) + 88` dp =
 *   `itemSize + firstItemSpacing + index * (itemSize + itemSpacing)`, so `items.first()` is the row
 *   nearest the main button;
 * - the rows are overlaid on the main button rather than stacked in a column: `visibilitySetup`
 *   sets every `COUIFloatingButtonLabel` to `View.GONE` when the menu closes, so a closed speed dial
 *   is exactly one button tall and no row can be touched. Here the rows are a bottom-anchored
 *   overlay in the same [Box] as the main button, and they leave the composition once the close
 *   animation has run;
 * - opening slides every row up from that stacked offset on a
 *   [FloatingButtonDefaults.SlideSpring] (`SpringAnimation` with stiffness 500 and damping ratio 0.8)
 *   and scales/fades it from [FloatingButtonDefaults.MIN_SCALE] to 1 on the 300ms
 *   cubic-bezier(0.25, 0.1, 0.25, 1) `mExpandMenuAnimationInterpolator`. The rows are staggered by
 *   [FloatingButtonDefaults.ExpandStagger] (`ACTION_ANIM_DELAY`), the one *farthest* from the main
 *   button first — `visibilitySetup` walks `i3` from 0 and animates label `(size - 1) - i3`;
 * - closing slides the rows back down, nearest first (`i5 * 50` in the same method), on the 300ms
 *   `COUIMoveEaseInterpolator` (cubic-bezier(0.3, 0, 0.1, 1)) that `closeFloatingButtonMenu(false,
 *   300)` uses;
 * - both animations clear the action listener while they run (`setOnActionSelectedListener(null)`
 *   in the first/last label's `onAnimationStart`, restored in `onAnimationEnd`), so rows only react
 *   once the menu has settled;
 * - picking an action closes the menu unless the callback returns `true`, exactly like
 *   `OnActionSelectedListener.onActionSelected`.
 *
 * This file adds a container and leaves `basic/FloatingActionButton.kt` untouched: that component is
 * the single COUI floating button, and the speed dial is built on top of it — the main button and the
 * children are all [FloatingActionButton]s, so its press feedback, ripple and role survive instead of
 * being reimplemented. The cost is that [FloatingActionButton] has no `enabled` parameter, so a
 * disabled button here is disabled by colour and by a guarded click rather than by an inert surface.
 *
 * Not ported:
 * - `COUIFloatingButtonSeamlessImpl` and `COUIFloatingButtonTouchAnimation`, the seamless material
 *   transitions, together with the `COUIMaterialBlurEffect` path of `setBlurEffectEnable`;
 * - `COUIMaskEffectDrawable` (`mMaskDrawable`, touched by `onTouchEvent` and `animatePress`) and the
 *   caustic outer shadow of `COUIFABShadowUtils.setOuterShadowEffect`;
 * - `COUIFloatingButton$COUIFloatingButtonBehavior`, `$ScrollViewBehavior`, the
 *   `removeFloatingButtonItemWithWindowHeight` window-height clipping that drops rows that would not
 *   fit on screen, and the `AutoDismissRunnable` scheduled 5s after a scroll;
 * - the `fabTranslateEnhancementRatio` / `fabAlignmentMode` / `fabCradleMargin` attribute families,
 *   which `FloatingButton.Icon.Medium.Secondary` / `.Translate` set but no ported path reads;
 * - the vertical pivot the native switches per direction (the label chip's bottom edge while closing
 *   and its top edge while opening); the port keeps its end edge and centres vertically;
 * - the disabled-state compositing of `COUIFloatingButton.getDisabledColor`
 *   (`ColorUtils.compositeColors(couiColorDisable, coui_floating_button_disabled_base_color)`), which
 *   needs the runtime `couiColorDisable` attribute; the port uses the state list's own disabled item,
 *   `?attr/couiColorPressBackground`;
 * - the `performHapticFeedback()` / `playSoundEffect(0)` of `animatePress` and the group click;
 * - tap-through while a row is moving. A row that is still animating is inert here, as it is in the
 *   native, but a Compose row still consumes the tap it receives, so a tap landing during the 300ms
 *   open/close animation is swallowed instead of reaching the main button underneath.
 *
 * @param items The actions, in the order the native adds them: `items.first()` is the row nearest the
 *   main button and the one that animates last when the menu opens.
 * @param expanded Whether the menu is open.
 * @param onExpandedChange Called with the new state when the main button is clicked.
 * @param onItemClick Called with the action that was clicked. Return `true` to keep the menu open,
 *   like `OnActionSelectedListener.onActionSelected`.
 * @param mainIcon The glyph of the main button. The native reads it from `attr/mainFloatingButtonSrc`,
 *   which the app owns, so there is no default. Gallery passes
 *   `@drawable/base_menu_ic_add_sliding_up`, a 32dp plus; the `coui-icons` module's `COUIIcons.Add`
 *   is the equivalent glyph, and that module is not a dependency of this one.
 * @param modifier The modifier to be applied to the speed dial. The dial is as wide as its widest
 *   row while it is open, so anchor it to the end of a [Box] to keep the main button in place.
 * @param enabled Whether the speed dial reacts to clicks.
 * @param colors The [FloatingButtonColors] of the buttons and the label chips.
 */
@Composable
fun FloatingButton(
    items: List<FloatingButtonItem>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onItemClick: (FloatingButtonItem) -> Boolean,
    mainIcon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: FloatingButtonColors = FloatingButtonDefaults.colors(),
) {
    // mRotateForwardInterpolator / mRotateBackwardInterpolator: PathInterpolator(0.25, 0.1, 0.25, 1).
    val rotation by animateFloatAsState(
        targetValue = if (expanded) FloatingButtonDefaults.EXPAND_ICON_ROTATION else 0f,
        animationSpec = tween(
            durationMillis = if (items.isEmpty()) {
                FloatingButtonDefaults.ROTATE_DURATION_NO_ITEM
            } else {
                FloatingButtonDefaults.ROTATE_DURATION
            },
            easing = FloatingButtonDefaults.MoveEaseIn,
        ),
        label = "floatingButtonRotation",
    )

    Box(modifier = modifier) {
        for (index in items.indices) {
            FloatingButtonAction(
                item = items[index],
                index = index,
                lastIndex = items.lastIndex,
                expanded = expanded,
                enabled = enabled,
                keepOpen = onItemClick,
                onExpandedChange = onExpandedChange,
                colors = colors,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
        FloatingActionButton(
            onClick = { if (enabled) onExpandedChange(!expanded) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .graphicsLayer { rotationZ = rotation },
            shape = CircleShape,
            containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor,
            shadowElevation = if (expanded) {
                FloatingButtonDefaults.OpenElevation
            } else {
                FloatingButtonDefaults.CloseElevation
            },
            minWidth = FloatingButtonDefaults.ItemSize,
            minHeight = FloatingButtonDefaults.ItemSize,
        ) {
            Icon(
                imageVector = mainIcon,
                contentDescription = null,
                tint = if (enabled) colors.contentColor else colors.labelDisabledTextColor,
                modifier = Modifier.size(FloatingButtonDefaults.IconSize),
            )
        }
    }
}

/**
 * One action of a [FloatingButton]: a labelled child button.
 *
 * @param icon The glyph of the child button, `COUIFloatingButtonItem.getFabImageDrawable`.
 * @param label The label drawn in the chip at the start of the child button,
 *   `COUIFloatingButtonItem.getLabel`. An empty label draws no chip, as `setLabel` disables it.
 * @param enabled Whether the action can be clicked.
 */
@Immutable
data class FloatingButtonItem(
    val icon: ImageVector,
    val label: String,
    val enabled: Boolean = true,
)

/**
 * The palette of a [FloatingButton], resolved from the ColorOS 17 colour resources the
 * `FloatingButton.Icon` styles read.
 *
 * @property containerColor The fill of the main and child buttons.
 *   `style/FloatingButton.Icon.Medium.Main` resolves `mainFloatingButtonBackgroundColor` from
 *   `color/coui_floating_button_main_button_color_selector`, whose enabled item is
 *   `?attr/couiColorPrimary` (`coui_color_primary`), the theme's [COUITheme.colorScheme.primary]. The
 *   label item's `ShapeableImageView` carries the same attribute as its background.
 * @property contentColor The glyphs, which are drawn over that primary container:
 *   `?attr/couiColorOnPrimary` (`coui_color_on_primary`, `#ffffffff`), the theme's
 *   [COUITheme.colorScheme.onPrimary] — the same white as
 *   `color/coui_floating_button_label_text_color_enable`.
 * @property disabledContainerColor The fill of a disabled button:
 *   `color/coui_floating_button_main_button_color_selector`'s `state_enabled=false` item,
 *   `?attr/couiColorPressBackground` (`coui_color_press_background`, `#14000000` light /
 *   `#26ffffff` night).
 * @property labelContainerColor The chip behind a label:
 *   `color/coui_floating_button_label_broader_color` (`#1a000000` light / `#1affffff` night), which
 *   the layout also uses as the chip's stroke colour at `coui_floating_button_item_stroke_width`
 *   (0dp, so the stroke is invisible).
 * @property labelTextColor The label text:
 *   `color/coui_floating_button_label_text_color_enable` (`#ffffffff`).
 * @property labelDisabledTextColor A disabled label:
 *   `color/coui_floating_button_label_text_color_disable` (`#26000000` light / `#26ffffff` night).
 */
@Immutable
class FloatingButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val labelContainerColor: Color,
    val labelTextColor: Color,
    val labelDisabledTextColor: Color,
)

/** Contains default values used by [FloatingButton]. */
object FloatingButtonDefaults {

    /** The size of a button. `coui_floating_button_size` / `coui_floating_button_fab_normal_size`. */
    val ItemSize = 56.dp

    /**
     * The glyph size. The dumps carry no dimen for it — the glyph comes from the app
     * (`attr/mainFloatingButtonSrc` and `COUIFloatingButtonItem`'s icon), and the ColorOS glyphs
     * are 24dp boxes drawn on a 56dp button.
     */
    val IconSize = 24.dp

    /**
     * The gap between the main button and the row nearest it.
     * `coui_floating_button_item_first_bottom_margin`.
     */
    val FirstItemSpacing = 32.dp

    /** The gap between the remaining rows. `coui_floating_button_item_normal_bottom_margin`. */
    val ItemSpacing = 16.dp

    /** The gap between a child button and its label chip. `coui_floating_button_fab_side_margin`. */
    val LabelSpacing = 12.dp

    /**
     * The corner radius of the label chip. `coui_floating_button_item_label` resolves
     * `cardCornerRadius` from `?attr/couiRoundCornerXS`, i.e. `coui_round_corner_xs` (4dp).
     * `coui_floating_button_item_label_background_radius` (6dp) exists but the layout does not use it.
     */
    val LabelCornerRadius = 4.dp

    /** The start/end padding of the label text. `coui_floating_button_item_label_label_text_margin_*`. */
    val LabelTextHorizontalPadding = 8.dp

    /** The top/bottom padding of the label text. `coui_floating_button_item_label_label_text_margin_*`. */
    val LabelTextVerticalPadding = 4.dp

    /** The maximum width of the label text. `coui_floating_button_item_label_label_text_max_width`. */
    val LabelMaxWidth = 220.dp

    /** The maximum number of label lines. `integer/coui_floating_button_item_label_label_text_max_line`. */
    const val LABEL_MAX_LINES = 2

    /** The rotation of the main glyph while the menu is open. `DEFAULT_ROTATE_ANGLE`. */
    const val EXPAND_ICON_ROTATION = 45f

    /** The rotation duration when there are actions to open. `DEFAULT_ROTATE_ANIMATION_DURATION`. */
    const val ROTATE_DURATION = 250

    /**
     * The rotation duration when the speed dial has no actions.
     * `DEFAULT_ROTATE_ANIMATION_DURATION_NO_ITEM`.
     */
    const val ROTATE_DURATION_NO_ITEM = 300

    /** The shadow of the open main button. `coui_floating_button_open_elevation`. */
    val OpenElevation = 8.dp

    /** The shadow of the closed main button. `coui_floating_button_close_elevation`. */
    val CloseElevation = 5.dp

    /** The stagger between two rows. `COUIFloatingButton.ACTION_ANIM_DELAY`. */
    val ExpandStagger: Duration = 50.milliseconds

    /** The smallest scale a row is drawn at while hidden. `DEFAULT_SCALE_ANIMATION_MIN_VALUE`. */
    const val MIN_SCALE = 0.6f

    /** The open/close tween duration. `DEFAULT_MAIN_FLOATING_BUTTON_ANIMATION_DURATION`. */
    const val ANIMATION_DURATION = 300

    /**
     * The opening easing: `mExpandMenuAnimationInterpolator` / `mLabelPathInterpolator`, i.e.
     * `PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f)`.
     */
    val MoveEaseIn: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    /**
     * The closing easing: `mCloseMenuPathInterpolator`, i.e. `COUIMoveEaseInterpolator`'s
     * `PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f)` — the same curve as `anim/coui_ease`.
     */
    val MoveEase: Easing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

    /**
     * The opening scale/fade spec: the 300ms `animatorSet` of `animationFloatingButtonMenuExpand`
     * running on `mExpandMenuAnimationInterpolator`.
     */
    val ExpandSpec: FiniteAnimationSpec<Float> =
        tween(durationMillis = ANIMATION_DURATION, easing = MoveEaseIn)

    /**
     * The closing scale/fade and slide spec: the 300ms close of `closeFloatingButtonMenu(false, 300)`
     * running on `mCloseMenuPathInterpolator`.
     */
    val CollapseSpec: FiniteAnimationSpec<Float> =
        tween(durationMillis = ANIMATION_DURATION, easing = MoveEase)

    /**
     * The opening slide: `animationFloatingButtonMenuExpand`'s `SpringAnimation(TRANSLATION_Y, 0f)`
     * with `stiffness = 500` and `dampingRatio = 0.8`.
     */
    val SlideSpring: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 500f)

    /**
     * The distance from the container's bottom to a row's bottom edge while the menu is open —
     * the native's `getTotalLabelHeight(index)`, `(index * 72) + 88` dp. Subtracting
     * [ItemSize] leaves the gap between the row and the one below it.
     */
    fun itemBottomOffset(index: Int): Dp = ItemSize + FirstItemSpacing +
        (ItemSize + ItemSpacing) * index

    /**
     * The label text style: `coui_floating_button_item_label_label_text_size` is 12sp, and the theme
     * has no 12sp tier, so its 13sp [COUITheme.textStyles.footnote1] carries the line height under
     * the native size.
     */
    @Composable
    fun labelTextStyle(): TextStyle = COUITheme.textStyles.footnote1.copy(fontSize = 12.sp)

    /** The default [FloatingButtonColors]: the theme slots listed on [FloatingButtonColors]. */
    @Composable
    fun colors(
        containerColor: Color = COUITheme.colorScheme.primary,
        contentColor: Color = COUITheme.colorScheme.onPrimary,
        disabledContainerColor: Color = if (isDark()) {
            FloatingButtonPressBackgroundDark
        } else {
            FloatingButtonPressBackgroundLight
        },
        labelContainerColor: Color = if (isDark()) {
            FloatingButtonLabelContainerDark
        } else {
            FloatingButtonLabelContainerLight
        },
        labelTextColor: Color = FloatingButtonLabelText,
        labelDisabledTextColor: Color = if (isDark()) {
            FloatingButtonLabelDisabledTextDark
        } else {
            FloatingButtonLabelDisabledTextLight
        },
    ): FloatingButtonColors = FloatingButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        labelContainerColor = labelContainerColor,
        labelTextColor = labelTextColor,
        labelDisabledTextColor = labelDisabledTextColor,
    )

    /** Whether the current theme reads as a dark one, which every colour resource above branches on. */
    @Composable
    private fun isDark(): Boolean = COUITheme.colorScheme.background.luminance() < 0.5f
}

/** coui_color_press_background in light themes (#14000000): the disabled container. */
private val FloatingButtonPressBackgroundLight = Color(0x14000000)

/** coui_color_press_background in dark themes (#26ffffff): the disabled container. */
private val FloatingButtonPressBackgroundDark = Color(0x26FFFFFF)

/** coui_floating_button_label_broader_color in light themes (#1a000000): the label chip. */
private val FloatingButtonLabelContainerLight = Color(0x1A000000)

/** coui_floating_button_label_broader_color in dark themes (#1affffff): the label chip. */
private val FloatingButtonLabelContainerDark = Color(0x1AFFFFFF)

/** coui_floating_button_label_text_color_enable (#ffffffff in both themes): the label text. */
private val FloatingButtonLabelText = Color(0xFFFFFFFF)

/** coui_floating_button_label_text_color_disable in light themes (#26000000). */
private val FloatingButtonLabelDisabledTextLight = Color(0x26000000)

/** coui_floating_button_label_text_color_disable in dark themes (#26ffffff). */
private val FloatingButtonLabelDisabledTextDark = Color(0x26FFFFFF)

/**
 * One row of the speed dial: the label chip and the child button, sliding and scaling as a unit.
 *
 * @param item The action.
 * @param index The action's index; it sets the row's offset from the main button.
 * @param lastIndex The index of the row farthest from the main button, which opens first.
 * @param expanded Whether the menu is open.
 * @param enabled Whether the speed dial reacts to clicks.
 * @param keepOpen Called with the clicked action; `true` keeps the menu open.
 * @param onExpandedChange Called to close the menu after an action is picked.
 * @param colors The palette.
 * @param modifier The modifier to be applied to the row.
 */
@Composable
private fun FloatingButtonAction(
    item: FloatingButtonItem,
    index: Int,
    lastIndex: Int,
    expanded: Boolean,
    enabled: Boolean,
    keepOpen: (FloatingButtonItem) -> Boolean,
    onExpandedChange: (Boolean) -> Unit,
    colors: FloatingButtonColors,
    modifier: Modifier = Modifier,
) {
    // A row is composed while the menu is open and until the close animation has finished; the native
    // reaches the same state by leaving the labels at View.GONE.
    var visible by remember { mutableStateOf(expanded) }
    // The action listener is only attached between the two animations.
    var settled by remember { mutableStateOf(expanded) }
    val progress = remember { Animatable(0f) }
    val slide = remember { Animatable(1f) }
    // The farthest row starts first when opening; the nearest one starts first when closing.
    val startDelay = FloatingButtonDefaults.ExpandStagger.inWholeMilliseconds *
        (if (expanded) lastIndex - index else index)

    LaunchedEffect(expanded) {
        if (expanded) visible = true
        settled = false
        if (startDelay > 0) delay(startDelay)
        coroutineScope {
            launch {
                progress.animateTo(
                    targetValue = if (expanded) 1f else 0f,
                    animationSpec = if (expanded) {
                        FloatingButtonDefaults.ExpandSpec
                    } else {
                        FloatingButtonDefaults.CollapseSpec
                    },
                )
            }
            launch {
                slide.animateTo(
                    targetValue = if (expanded) 0f else 1f,
                    animationSpec = if (expanded) {
                        FloatingButtonDefaults.SlideSpring
                    } else {
                        FloatingButtonDefaults.CollapseSpec
                    },
                )
            }
        }
        settled = expanded
        if (!expanded) visible = false
    }

    if (!visible) return

    val interactive = enabled && item.enabled && settled
    val scale = FloatingButtonDefaults.MIN_SCALE +
        (1f - FloatingButtonDefaults.MIN_SCALE) * progress.value
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val distance = FloatingButtonDefaults.itemBottomOffset(index)

    Row(
        modifier = modifier
            .offset { IntOffset(x = 0, y = -((distance * (1f - slide.value)).toPx()).roundToInt()) }
            .heightIn(min = FloatingButtonDefaults.ItemSize)
            .graphicsLayer { alpha = progress.value },
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.label.isNotEmpty()) {
            Text(
                text = item.label,
                color = if (enabled && item.enabled) {
                    colors.labelTextColor
                } else {
                    colors.labelDisabledTextColor
                },
                style = FloatingButtonDefaults.labelTextStyle(),
                maxLines = FloatingButtonDefaults.LABEL_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        // setPivotX(width) for LTR and 0 for RTL.
                        transformOrigin = TransformOrigin(
                            pivotFractionX = if (isRtl) 0f else 1f,
                            pivotFractionY = 0.5f,
                        )
                    }
                    .clip(RoundedCornerShape(FloatingButtonDefaults.LabelCornerRadius))
                    .background(colors.labelContainerColor)
                    .padding(
                        horizontal = FloatingButtonDefaults.LabelTextHorizontalPadding,
                        vertical = FloatingButtonDefaults.LabelTextVerticalPadding,
                    )
                    .widthIn(max = FloatingButtonDefaults.LabelMaxWidth),
            )
            Spacer(modifier = Modifier.width(FloatingButtonDefaults.LabelSpacing))
        }
        FloatingActionButton(
            onClick = { if (interactive && !keepOpen(item)) onExpandedChange(false) },
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
            shape = CircleShape,
            containerColor = if (enabled && item.enabled) {
                colors.containerColor
            } else {
                colors.disabledContainerColor
            },
            // style/FloatingButton.Icon sets fabNeedElevation=false for the child buttons.
            shadowElevation = 0.dp,
            minWidth = FloatingButtonDefaults.ItemSize,
            minHeight = FloatingButtonDefaults.ItemSize,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label.takeIf(String::isNotEmpty),
                tint = colors.contentColor,
                modifier = Modifier.size(FloatingButtonDefaults.IconSize),
            )
        }
    }
}
