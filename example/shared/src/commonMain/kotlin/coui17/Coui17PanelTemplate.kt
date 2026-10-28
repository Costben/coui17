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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.DpOffset
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
import io.github.suqi8.coui.kmp.shader.RuntimeShader
import io.github.suqi8.coui.kmp.shader.asBrush
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported
import io.github.suqi8.coui.kmp.squircle.addSquircleRect
import io.github.suqi8.coui.kmp.squircle.squircleSurface
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs

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
 * @param onDismissFinished Called once the exit animation has finished.
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
    onDismissFinished: (() -> Unit)? = null,
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
        onDismissFinished = onDismissFinished,
        startAction = { CloseCapsule(onClick = onDismissRequest) },
        endAction = { OverflowButton(onClick = {}) },
    ) {
        PanelHeader(title = title, statusText = statusText)
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
 * The top of a panel body: the gap under the toolbar, the 28sp title and the status line. Shared by
 * [PanelTemplate] and the gallery's bottom sheet demos so every panel opens on the same rhythm.
 */
@Composable
internal fun PanelHeader(title: String, statusText: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(PanelTemplateDefaults.ToolbarGap))
        // The trimmed line box is taller than the measured ink box, so the text is centred inside a
        // pinned-height box; left to itself it would sit 6.75dp too low.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PanelTemplateDefaults.TitleHeight),
            contentAlignment = Alignment.CenterStart,
        ) {
            // The 28sp line is taller than the 27dp ink box it is centred in; measured unbounded so
            // the box only positions the title and never clips its ascenders or descenders.
            Text(
                text = title,
                modifier = Modifier.wrapContentHeight(unbounded = true),
                style = trimmedLineHeight,
                fontSize = 28.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = COUITheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(PanelTemplateDefaults.StatusGap))
        StatusRow(text = statusText)
    }
}

/**
 * The two panel toolbar buttons' shared dress, as measured on a live ColorOS 17 device panel.
 *
 * Light mode: no fill of its own (the capsule reads the panel's 240), a 1dp highlight along the top
 * and bottom rim, and a soft dark halo around it that is deepest under the bottom edge (-18 levels)
 * and dies out ~14dp from the edge. Dark mode: a 12% white fill (51 over the panel's 22), the same
 * rim, and a faint light halo instead of a dark one. The halo never reaches inside the capsule, so
 * it is drawn with the capsule cut out — a plain gradient started at the bottom edge left a hard
 * line across the button's foot.
 */
@Composable
private fun ToolbarButtonSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    val fill = if (isDark) PanelTemplateDefaults.ToolbarFillDark else Color.Transparent
    val halo = if (isDark) PanelTemplateDefaults.ToolbarHaloDark else PanelTemplateDefaults.ToolbarHaloLight
    val haloShader = rememberToolbarHaloShader()
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (haloShader != null) {
                        Modifier.ditheredHalo(haloShader, halo)
                    } else {
                        Modifier
                            .drawWithContent {
                                val capsule = Path().apply {
                                    addOutline(CircleShape.createOutline(size, layoutDirection, this@drawWithContent))
                                }
                                clipPath(capsule, ClipOp.Difference) { this@drawWithContent.drawContent() }
                            }
                            .dropShadow(CircleShape, halo)
                    },
                ),
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(fill)
                .materialShadowEdge(
                    stroke = PanelTemplateDefaults.ToolbarButtonStroke,
                    edgeStyle = GradientStrokeStyle.EDGE_STYLE_1,
                    shadowStyle = InnerShadowStyle.SHADOW_STYLE_1,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/**
 * The toolbar halo drawn by a shader instead of [dropShadow], with the dither ColorOS puts on its
 * own material output.
 *
 * The dark halo is 5% white: across its ~60px falloff it climbs only 13 levels over the panel, so a
 * plain blurred shadow quantises into bands 4–5px wide that read as rings round the button. The
 * native composite shader (`librenderkit.so`) ends every material pass with `addNoise`, an
 * interleaved-gradient-noise dither, which is why the system halo is smooth. This draws the same
 * blurred capsule — a Gaussian of [halo]'s radius, converted to sigma the way Skia does — and adds
 * that noise to its alpha. The capsule itself is cut out, as before, so the halo never darkens the
 * button.
 */
private fun Modifier.ditheredHalo(shader: RuntimeShader, halo: Shadow): Modifier = drawWithCache {
    val sigma = halo.radius.toPx() * BLUR_RADIUS_TO_SIGMA
    val offsetX = halo.offset.x.toPx()
    val offsetY = halo.offset.y.toPx()
    // Three sigma past the furthest edge, where the Gaussian is below 0.2% of its peak.
    val extent = 3f * sigma + maxOf(abs(offsetX), abs(offsetY))
    shader.setFloatUniform("size", size.width, size.height)
    shader.setFloatUniform("offset", offsetX, offsetY)
    shader.setFloatUniform("sigma", sigma)
    shader.setFloatUniform("aa", 1f)
    shader.setFloatUniform("color", halo.color.red, halo.color.green, halo.color.blue, halo.alpha)
    val brush = shader.asBrush()
    onDrawBehind {
        drawRect(
            brush = brush,
            topLeft = Offset(-extent, -extent),
            size = Size(size.width + 2f * extent, size.height + 2f * extent),
        )
    }
}

@Composable
private fun rememberToolbarHaloShader(): RuntimeShader? {
    if (!isRuntimeShaderSupported()) return null
    return remember { runCatching { RuntimeShader(TOOLBAR_HALO_SHADER) }.getOrNull() }
}

/** Skia's blur-radius-to-sigma factor, `1 / sqrt(3)`. */
private const val BLUR_RADIUS_TO_SIGMA = 0.57735f

/**
 * A Gaussian-blurred capsule outside an unblurred one. The blur of a convex shape is approximated by
 * the error function of its signed distance, which is exact along straight edges and within a few
 * percent round a capsule's ends. `ign` is the native `interleavedGradientNoise`, scaled to one
 * alpha level so the halo dithers instead of stepping.
 */
private const val TOOLBAR_HALO_SHADER = """
    uniform float2 size;
    uniform float2 offset;
    uniform float sigma;
    uniform float aa;
    uniform float4 color;

    float sdCapsule(float2 p, float2 halfSize) {
        float r = min(halfSize.x, halfSize.y);
        float2 q = abs(p) - halfSize + r;
        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    }

    float erfApprox(float x) {
        float x2 = x * x;
        float t = 0.147 * x2;
        float v = sqrt(1.0 - exp(-x2 * (1.2732395 + t) / (1.0 + t)));
        return x < 0.0 ? -v : v;
    }

    float ign(float2 p) {
        return fract(52.9829189 * fract(dot(p, float2(0.06711056, 0.00583715))));
    }

    half4 main(float2 xy) {
        float2 halfSize = size * 0.5;
        float shadow = 0.5 - 0.5 * erfApprox(sdCapsule(xy - halfSize - offset, halfSize) / (sigma * 1.41421356));
        float outside = smoothstep(-aa, aa, sdCapsule(xy - halfSize, halfSize));
        float a = shadow * outside * color.a;
        // Dither only where the halo is, so the noise never lifts the panel around it.
        a += (ign(xy) - 0.5) * (2.0 / 255.0) * step(0.5 / 255.0, a);
        a = clamp(a, 0.0, 1.0);
        return half4(half3(color.rgb) * a, a);
    }
"""

/** The capsule close button in the panel toolbar: 60x40 dp, full radius. */
@Composable
internal fun CloseCapsule(onClick: () -> Unit) {
    ToolbarButtonSurface(onClick = onClick) {
        Box(
            modifier = Modifier
                .height(PanelTemplateDefaults.ToolbarHeight)
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
}

/** The circular overflow button in the panel toolbar: a 40dp circle with three dots. */
@Composable
internal fun OverflowButton(onClick: () -> Unit) {
    val ink = COUITheme.colorScheme.onBackground
    ToolbarButtonSurface(onClick = onClick) {
        Box(
            modifier = Modifier.size(PanelTemplateDefaults.ToolbarHeight),
            contentAlignment = Alignment.Center,
        ) {
            OverflowDots(ink)
        }
    }
}

/** The three stacked dots of the overflow button. */
@Composable
private fun OverflowDots(ink: Color) {
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
        val body = Path().apply { addSquircleRect(bodyWidth, bodyHeight, 20.dp.toPx()) }
        translate(left = bodyTopLeft.x, top = bodyTopLeft.y) {
            drawPath(
                path = body,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        scheme.onBackground.copy(alpha = 0.12f),
                        scheme.onBackground.copy(alpha = 0.04f),
                    ),
                    startY = 0f,
                    endY = bodyHeight,
                ),
            )
            drawPath(
                path = body,
                color = scheme.onBackground.copy(alpha = 0.10f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
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
internal fun ActionCapsule(text: String, onClick: () -> Unit) {
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

    /**
     * The toolbar buttons' rim. The live rim lifts the top edge 5–12 levels over the fill in light
     * mode and ~30 in dark mode, and the bottom edge 8 / 23.
     */
    val ToolbarButtonStroke = MaterialStrokeParams(
        edgeAlpha = 0.5f,
        edgeWidth = 2.dp,
        shadowFadeIn = 0.03f,
    )

    /** `couiColorPrimary` vertical gradient measured on the live capsule: #1A8CFE -> #3198FF. */
    val ActionGradient = listOf(Color(0xFF1A8CFE), Color(0xFF3198FF))

    /** The dark-mode toolbar button fill: 12% white, (51,51,51) over the (22,22,22) panel. */
    val ToolbarFillDark = Color(0x20FFFFFF)

    /** The light-mode halo round a toolbar button: dark, deepest below the button. */
    val ToolbarHaloLight = Shadow(
        radius = 16.dp,
        color = Color.Black,
        offset = DpOffset(0.dp, 6.dp),
        alpha = 0.12f,
    )

    /** The dark-mode halo: the same geometry in faint white. */
    val ToolbarHaloDark = Shadow(
        radius = 16.dp,
        color = Color.White,
        offset = DpOffset(0.dp, 6.dp),
        alpha = 0.05f,
    )

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
