// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package coui17

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.GradientStrokeStyle
import io.github.suqi8.coui.kmp.basic.InnerShadowStyle
import io.github.suqi8.coui.kmp.basic.MaterialStrokeParams
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.materialShadowEdge
import io.github.suqi8.coui.kmp.overlay.OverlayBottomSheet
import io.github.suqi8.coui.kmp.squircle.squircleSurface
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The gallery entry for [PanelTemplate]: opens the template with placeholder copy.
 *
 * The panel is a shape / rhythm template, not a copy of any particular screen, so every string here
 * is a placeholder — swap them for whatever the caller needs.
 */
@Composable
fun PanelTemplateDemo() {
    var show by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Coui17Dimens.ListCardInset),
    ) {
        Button(onClick = { show = true }) { Text(str("Open panel")) }
    }
    PanelTemplate(
        show = show,
        onDismissRequest = { show = false },
        title = str("Title"),
        statusText = str("Status text"),
        settingsText = str("List item"),
        actionText = str("Button"),
    )
}

/**
 * The ColorOS 17 bottom panel template: a full-width bottom sheet with a capsule close button and an
 * overflow button in its toolbar, a large title, a status line, a media slot, one 52dp list row and a
 * capsule call-to-action. Every string is a placeholder; nothing from the measured screen is baked in.
 *
 * Geometry is taken from the reference panel, measured at 3.875 px/dp on a 1272x2772 screen:
 * panel 1272x2049 px (328x529 dp) with the native 45dp top-corner profile (reproduced here by the
 * library's 38dp squircle at corner weight 2.5, which matches the native outline to within 5px) and
 * square bottom corners, 16dp side insets, a 60x40 dp close capsule, a 40dp overflow circle, a 28sp
 * title, a 6dp status dot, a 144x147 dp media slot, a 52dp / 23dp-radius list row and a 44dp
 * capsule action.
 *
 * @param show Whether the panel is shown.
 * @param onDismissRequest Called when the panel is dismissed by the close button, an outside tap,
 *   a drag or back.
 * @param modifier The modifier applied to the panel.
 * The defaults are English source strings, not copy: the gallery passes [i18n.str] values in, so the
 * panel follows the language switch, and a caller that does not localise still gets readable text.
 *
 * @param title The panel title.
 * @param statusText The text shown next to the status dot.
 * @param settingsText The label of the list row.
 * @param actionText The label of the call-to-action.
 * @param onSettingsClick Called when the list row is tapped.
 * @param onActionClick Called when the call-to-action is tapped.
 */
@Composable
fun PanelTemplate(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Title",
    statusText: String = "Status text",
    settingsText: String = "List item",
    actionText: String = "Button",
    onSettingsClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
) {
    OverlayBottomSheet(
        show = show,
        modifier = modifier,
        // The panel carries its own toolbar, so no drag handle and no title row are drawn; the
        // sheet still reserves the handle strip so the toolbar keeps the native 16dp offset.
        showDragHandle = false,
        insideMargin = DpSize(PanelTemplateDefaults.SideInset, PanelTemplateDefaults.BottomInset),
        onDismissRequest = onDismissRequest,
        startAction = { CloseCapsule(onClick = onDismissRequest) },
        endAction = { OverflowButton(onClick = {}) },
    ) {
        Spacer(Modifier.height(PanelTemplateDefaults.ToolbarGap))
        // The trimmed line box is taller than the measured ink box, so the text is centred inside a
        // pinned-height box; left to itself it would sit 6.75dp too low.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PanelTemplateDefaults.TitleHeight),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = title,
                style = trimmedLineHeight,
                fontSize = 28.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = COUITheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(PanelTemplateDefaults.StatusGap))
        StatusRow(text = statusText)
        Spacer(Modifier.height(PanelTemplateDefaults.ImageGap))
        // The media slot is not a ColumnScope, so the render is centred by a full-width box.
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProductSlot()
        }
        Spacer(Modifier.height(PanelTemplateDefaults.SettingsGap))
        SettingsRow(text = settingsText, onClick = onSettingsClick)
        Spacer(Modifier.height(PanelTemplateDefaults.ActionGap))
        ActionCapsule(text = actionText, onClick = onActionClick)
    }
}

/**
 * The surface of the two panel toolbar buttons: the native icon-button treatment, i.e. a translucent
 * fill, the rim of `EDGE_STYLE_1` / `SHADOW_STYLE_1` and the soft fade below the button.
 */
@Composable
private fun Modifier.toolbarButtonSurface(): Modifier {
    val fill = COUITheme.colorScheme.surfaceContainer
    return this
        // The native button edge is hard on top but dissolves into the panel over ~6dp below, so the
        // fill is repeated as a fading capsule under the button instead of a drop shadow.
        .drawBehind {
            // The fill is repeated as a capsule that starts just below the button edge and fades out
            // over 6dp, which is what the native blurred bottom edge looks like against the panel.
            val fade = 6.dp.toPx()
            val total = size.height + fade
            val start = (size.height - 2.dp.toPx()) / total
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(fill.copy(alpha = fill.alpha * 0.5f), Color.Transparent),
                    startY = size.height,
                    endY = size.height + fade,
                    // Decal keeps the gradient transparent above the edge instead of clamping it.
                    tileMode = TileMode.Decal,
                ),
                size = Size(size.width, size.height + fade),
                cornerRadius = CornerRadius(size.height / 2f),
            )
        }
        .clip(CircleShape)
        .background(fill)
        .materialShadowEdge(
            stroke = PanelTemplateDefaults.ToolbarButtonStroke,
            edgeStyle = GradientStrokeStyle.EDGE_STYLE_1,
            shadowStyle = InnerShadowStyle.SHADOW_STYLE_1,
        )
}

/** The capsule close button in the panel toolbar: 60x40 dp, full radius. */
@Composable
private fun CloseCapsule(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(PanelTemplateDefaults.ToolbarHeight)
            .toolbarButtonSurface()
            .clickable(onClick = onClick)
            // 16dp a side keeps the capsule at the native 60dp for a two-character label.
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = str("Close"),
            style = trimmedLineHeight,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            color = COUITheme.colorScheme.onBackground,
        )
    }
}

/** The circular overflow button in the panel toolbar: a 40dp circle with three dots. */
@Composable
private fun OverflowButton(onClick: () -> Unit) {
    val ink = COUITheme.colorScheme.onBackground
    Box(
        modifier = Modifier
            .size(PanelTemplateDefaults.ToolbarHeight)
            .toolbarButtonSurface()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(width = 3.5.dp, height = 17.5.dp)) {
            val radius = size.width / 2f
            val pitch = (size.height - size.width) / 2f
            repeat(3) { index ->
                drawCircle(
                    color = ink,
                    radius = radius,
                    center = Offset(size.width / 2f, radius + pitch * index),
                )
            }
        }
    }
}

/** The `● 未连接` line: a 6dp dot, a 5dp gap and the state text. */
@Composable
private fun StatusRow(text: String) {
    val dotColor = COUITheme.colorScheme.onSurfaceVariantActions
    Row(
        modifier = Modifier.height(PanelTemplateDefaults.StatusHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(6.dp)) {
            drawCircle(color = dotColor)
        }
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = trimmedLineHeight,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            color = COUITheme.colorScheme.onBackground,
        )
    }
}

/**
 * The product image slot: a 144x147 dp box holding the soft radial glow and a neutral device
 * silhouette. Callers replace this with their own artwork.
 */
@Composable
private fun ProductSlot() {
    val scheme = COUITheme.colorScheme
    Canvas(
        modifier = Modifier.size(
            width = PanelTemplateDefaults.ImageWidth,
            height = PanelTemplateDefaults.ImageHeight,
        ),
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        // The media slot sits on a faint neutral halo that dies out just past the slot edges.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(scheme.onBackground.copy(alpha = 0.03f), Color.Transparent),
                center = center,
                radius = size.width * 0.95f,
            ),
            radius = size.width * 0.95f,
            center = center,
        )
        val bodyWidth = size.width * 0.92f
        val bodyHeight = size.height * 0.88f
        val bodyTopLeft = Offset(
            x = (size.width - bodyWidth) / 2f,
            y = (size.height - bodyHeight) / 2f,
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    scheme.onBackground.copy(alpha = 0.12f),
                    scheme.onBackground.copy(alpha = 0.04f),
                ),
                startY = bodyTopLeft.y,
                endY = bodyTopLeft.y + bodyHeight,
            ),
            topLeft = bodyTopLeft,
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(20.dp.toPx()),
        )
        drawRoundRect(
            color = scheme.onBackground.copy(alpha = 0.10f),
            topLeft = bodyTopLeft,
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(20.dp.toPx()),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

/** The settings row: a 52dp squircle card with a 23dp radius and a trailing chevron. */
@Composable
private fun SettingsRow(text: String, onClick: () -> Unit) {
    val scheme = COUITheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(PanelTemplateDefaults.SettingsRowHeight)
            .squircleSurface(
                color = scheme.surfaceContainer,
                cornerRadius = PanelTemplateDefaults.SettingsRowRadius,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = trimmedLineHeight,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            color = COUITheme.colorScheme.onBackground,
        )
        Chevron(color = scheme.onSurfaceVariantSummary)
    }
}

/**
 * The forward chevron used by the settings row. Measured on the live panel the ink covers 7.5x13.7
 * dp, which is a 7.1x13.2 dp box stroked at 1.4 dp.
 */
@Composable
private fun Chevron(color: Color) {
    Canvas(modifier = Modifier.size(width = 7.1.dp, height = 13.2.dp)) {
        val stroke = 1.4.dp.toPx()
        val path = Path().apply {
            moveTo(stroke / 2f, stroke / 2f)
            lineTo(size.width - stroke / 2f, size.height / 2f)
            lineTo(stroke / 2f, size.height - stroke / 2f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

/**
 * The capsule call-to-action: 44dp tall, full radius, filled with the ColorOS 17 primary
 * gradient measured on the live panel (`#1A8CFE` at the top, `#3198FF` at the bottom).
 */
@Composable
private fun ActionCapsule(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PanelTemplateDefaults.ActionHeight)
            .clip(CircleShape)
            .background(Brush.verticalGradient(PanelTemplateDefaults.ActionGradient))
            // The native capsule is lit along its top and bottom edge only: 1dp of white that peaks
            // at (117,167,226) over a (34,144,255) fill at the top and (115,161,210) over
            // (49,152,255) at the bottom, fading to nothing across the middle.
            .border(
                BorderStroke(1.dp, Brush.verticalGradient(PanelTemplateDefaults.ActionEdgeGradient)),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = trimmedLineHeight,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
        )
    }
}

/**
 * Geometry of the ColorOS 17 device panel, measured on a live panel at 3.875 px/dp.
 *
 * The panel is 529dp tall on a 1272x2772 px screen: a 10dp handle strip, a 40dp toolbar with 6dp
 * above and 12dp below it, then a 28sp title, the status line, the image slot, the settings row and
 * the capsule action, closing with a 40dp bottom inset.
 */
object PanelTemplateDefaults {
    /** Horizontal inset of every row inside the panel: 62px at 3.875 px/dp. */
    val SideInset = 16.dp

    /** Inset between the capsule action and the window bottom: 155px. */
    val BottomInset = 40.dp

    /** Height of the close capsule and of the overflow circle: 155px. */
    val ToolbarHeight = 40.dp

    /** Gap between the toolbar and the title: 80px from the capsule bottom to the title ink. */
    val ToolbarGap = 3.dp

    /** Ink height of the 28sp title: 105px. */
    val TitleHeight = 27.dp

    /** Gap between the title and the status line: 29px. */
    val StatusGap = 12.dp

    /** Ink height of the 12sp status line: 44px. */
    val StatusHeight = 12.dp

    /** Gap between the status line and the image slot: 130px. */
    val ImageGap = 33.5.dp

    /** Width of the product image slot: 557px. */
    val ImageWidth = 144.dp

    /** Height of the product image slot: 569px. */
    val ImageHeight = 147.dp

    /** Gap between the image slot and the settings row: 226px. */
    val SettingsGap = 57.5.dp

    /** Height of the settings row: 201px. */
    val SettingsRowHeight = 52.dp

    /** Corner radius of the settings row: `coui_round_corner_l_radius_large_16_1` = 23dp. */
    val SettingsRowRadius = 23.dp

    /** Gap between the settings row and the capsule action: 125px. */
    val ActionGap = 31.5.dp

    /** Height of the capsule action: 171px. */
    val ActionHeight = 44.dp

    /** `couiColorPrimary` vertical gradient measured on the live capsule: #1A8CFE -> #3198FF. */
    val ToolbarButtonStroke = MaterialStrokeParams(
        edgeAlpha = 0.17f,
        edgeWidth = 2.dp,
        shadowFadeIn = 0.03f,
    )
    val ActionGradient = listOf(Color(0xFF1A8CFE), Color(0xFF3198FF))

    /** The 1dp edge light of the capsule: white at both ends, clear across the middle. */
    val ActionEdgeGradient = listOf(
        Color.White.copy(alpha = 0.18f),
        Color.Transparent,
        Color.White.copy(alpha = 0.15f),
    )
}

/**
 * `COUILineHeightStyle` keeps the full font box (`Trim.None`), which renders taller than the native
 * line boxes. Every text slot here is pinned to its measured height, so the trimmed style keeps the
 * glyphs centred inside them.
 */
private val trimmedLineHeight: TextStyle = TextStyle(
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)
