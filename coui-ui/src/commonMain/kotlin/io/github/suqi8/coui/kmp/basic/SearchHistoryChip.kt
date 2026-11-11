// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.roundToInt

/**
 * A search-history keyword chip with long-press-to-delete.
 *
 * A tap runs [onClick]. A long press switches the chip into delete mode: a "×" springs out of the
 * chip's end edge (scaling up from 0.3 and fading in) while the chip widens by
 * [SearchHistoryChipDefaults.DeleteExtraWidth] to make room, all on one critically damped spring,
 * so the chip only grows sideways and keeps its height. Tapping the "×" runs [onDelete]; tapping
 * the chip body or long-pressing it again folds the "×" back in.
 *
 * The chip is a [SearchHistoryChipDefaults.Height] capsule along the system continuous-curvature
 * outline ([CapsuleOutlineShape]), filled with `surfaceContainerHigh`. While pressed it scales to
 * [SearchHistoryChipDefaults.PressedScale] and its fill lightens over 150ms.
 *
 * @param text The keyword shown in the chip, kept to one line and ellipsized.
 * @param onClick Called when the chip is tapped outside delete mode.
 * @param onDelete Called when the "×" is tapped in delete mode.
 * @param modifier The modifier to be applied to the chip.
 * @param deleteMode Whether the "×" is out. Use the overload without it to let the chip keep this
 *   state itself.
 * @param onDeleteModeChange Called with the new delete mode on a long press, or with `false` when
 *   the chip body is tapped in delete mode.
 * @param deleteContentDescription The accessibility label of the "×" button.
 * @param colors The [SearchHistoryChipColors].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchHistoryChip(
    text: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    deleteMode: Boolean,
    onDeleteModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    deleteContentDescription: String? = null,
    colors: SearchHistoryChipColors = SearchHistoryChipDefaults.colors(),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animProgress = remember { Animatable(if (deleteMode) 1f else 0f) }
    LaunchedEffect(deleteMode) {
        animProgress.animateTo(
            targetValue = if (deleteMode) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = SearchHistoryChipDefaults.SpringStiffness,
            ),
        )
    }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) SearchHistoryChipDefaults.PressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = SearchHistoryChipDefaults.SpringStiffness,
        ),
        label = "search-history-chip-press-scale",
    )
    val animatedBg by animateColorAsState(
        targetValue = if (isPressed) colors.pressedContainerColor else colors.containerColor,
        animationSpec = tween(durationMillis = SearchHistoryChipDefaults.PressedColorDurationMillis),
        label = "search-history-chip-bg",
    )

    val squircleEnabled = isSquircleEnabled()
    val shape = remember(squircleEnabled) { CapsuleOutlineShape(squircleEnabled = squircleEnabled) }

    Layout(
        content = {
            Text(
                text = text,
                fontSize = SearchHistoryChipDefaults.FontSize,
                color = colors.contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .size(
                        width = SearchHistoryChipDefaults.DeleteTouchWidth,
                        height = SearchHistoryChipDefaults.Height,
                    )
                    .graphicsLayer {
                        val fraction = animProgress.value
                        val scale = DELETE_ICON_MIN_SCALE + (1f - DELETE_ICON_MIN_SCALE) * fraction
                        scaleX = scale
                        scaleY = scale
                        alpha = fraction
                    }
                    .then(
                        if (deleteContentDescription != null && deleteMode) {
                            Modifier.semantics { contentDescription = deleteContentDescription }
                        } else {
                            Modifier
                        },
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = deleteMode && animProgress.value > 0.5f,
                        role = Role.Button,
                        onClick = onDelete,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val iconColor = colors.deleteIconColor
                Canvas(modifier = Modifier.size(SearchHistoryChipDefaults.DeleteIconSize)) {
                    val stroke = SearchHistoryChipDefaults.DeleteIconStrokeWidth.toPx()
                    drawLine(
                        color = iconColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(size.width, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }
        },
        modifier = modifier
            .height(SearchHistoryChipDefaults.Height)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .background(animatedBg)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (deleteMode) onDeleteModeChange(false) else onClick()
                },
                onLongClick = { onDeleteModeChange(!deleteMode) },
            ),
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val paddingStartPx = SearchHistoryChipDefaults.PaddingStart.roundToPx()
        val paddingEndPx = SearchHistoryChipDefaults.PaddingEnd.roundToPx()
        val extraWidthPx = SearchHistoryChipDefaults.DeleteExtraWidth.roundToPx()
        val currentExtraPx = (extraWidthPx * animProgress.value).roundToInt()

        // The text gives up width before the chip outgrows its row.
        val textMax = if (constraints.hasBoundedWidth) {
            (constraints.maxWidth - paddingStartPx - paddingEndPx - currentExtraPx).coerceAtLeast(0)
        } else {
            constraints.maxWidth
        }
        val textPlaceable = measurables[0].measure(loose.copy(maxWidth = textMax))
        val closePlaceable = measurables[1].measure(loose)

        val totalWidth = paddingStartPx + textPlaceable.width + paddingEndPx + currentExtraPx
        val totalHeight = constraints.maxHeight.coerceAtLeast(SearchHistoryChipDefaults.Height.roundToPx())

        layout(totalWidth, totalHeight) {
            textPlaceable.placeRelative(paddingStartPx, (totalHeight - textPlaceable.height) / 2)
            val closeX = paddingStartPx + textPlaceable.width + (currentExtraPx - extraWidthPx) +
                SearchHistoryChipDefaults.DeleteIconOffset.roundToPx()
            closePlaceable.placeRelative(closeX, (totalHeight - closePlaceable.height) / 2)
        }
    }
}

/**
 * A [SearchHistoryChip] that keeps its own delete mode: a long press brings the "×" out, and
 * tapping the chip body or long-pressing again folds it back.
 */
@Composable
fun SearchHistoryChip(
    text: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    deleteContentDescription: String? = null,
    colors: SearchHistoryChipColors = SearchHistoryChipDefaults.colors(),
) {
    var deleteMode by rememberSaveable(text) { mutableStateOf(false) }
    SearchHistoryChip(
        text = text,
        onClick = onClick,
        onDelete = onDelete,
        deleteMode = deleteMode,
        onDeleteModeChange = { deleteMode = it },
        modifier = modifier,
        deleteContentDescription = deleteContentDescription,
        colors = colors,
    )
}

/** The scale the "×" grows from as it springs out. */
private const val DELETE_ICON_MIN_SCALE = 0.3f

/** Contains the default values used by [SearchHistoryChip]. */
object SearchHistoryChipDefaults {
    /** The chip height. The chip only ever grows sideways. */
    val Height: Dp = 32.dp

    /** The padding before the keyword. */
    val PaddingStart: Dp = 12.dp

    /** The padding after the keyword (or after the "×" slot in delete mode). */
    val PaddingEnd: Dp = 12.dp

    /** How much wider the chip grows when the "×" is out. */
    val DeleteExtraWidth: Dp = 18.dp

    /** The tap target of the "×". */
    val DeleteTouchWidth: Dp = 24.dp

    /** The horizontal nudge of the "×" slot from the end of the keyword. */
    val DeleteIconOffset: Dp = 2.dp

    /** The drawn size of the "×" glyph. */
    val DeleteIconSize: Dp = 9.dp

    /** The stroke width of the "×" glyph. */
    val DeleteIconStrokeWidth: Dp = 1.3.dp

    /** The keyword text size. */
    val FontSize: TextUnit = 13.sp

    /** The chip's scale while pressed. */
    val PressedScale: Float = 0.96f

    /** The stiffness of the press and delete-mode springs (critically damped). */
    val SpringStiffness: Float = 438.65f

    /** How long the fill takes to lighten or return when pressed. */
    val PressedColorDurationMillis: Int = 150

    /**
     * The chip colours. The pressed fill is [containerColor] mixed 28% toward white, keeping its
     * alpha.
     */
    @Composable
    fun colors(
        containerColor: Color = COUITheme.colorScheme.surfaceContainerHigh,
        pressedContainerColor: Color = containerColor.lighten(PRESSED_LIGHTEN_FRACTION),
        contentColor: Color = COUITheme.colorScheme.onSurface,
        deleteIconColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
    ): SearchHistoryChipColors = remember(containerColor, pressedContainerColor, contentColor, deleteIconColor) {
        SearchHistoryChipColors(
            containerColor = containerColor,
            pressedContainerColor = pressedContainerColor,
            contentColor = contentColor,
            deleteIconColor = deleteIconColor,
        )
    }

    private const val PRESSED_LIGHTEN_FRACTION = 0.28f

    private fun Color.lighten(fraction: Float): Color = Color(
        red = (red * (1f - fraction) + fraction).coerceIn(0f, 1f),
        green = (green * (1f - fraction) + fraction).coerceIn(0f, 1f),
        blue = (blue * (1f - fraction) + fraction).coerceIn(0f, 1f),
        alpha = alpha,
    )
}

/**
 * The colours of a [SearchHistoryChip].
 *
 * @property containerColor The chip fill at rest.
 * @property pressedContainerColor The chip fill while pressed.
 * @property contentColor The keyword colour.
 * @property deleteIconColor The "×" colour.
 */
@Immutable
data class SearchHistoryChipColors(
    val containerColor: Color,
    val pressedContainerColor: Color,
    val contentColor: Color,
    val deleteIconColor: Color,
)
