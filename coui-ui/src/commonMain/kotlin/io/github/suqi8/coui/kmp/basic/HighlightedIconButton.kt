// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.isBlurEnabled
import io.github.suqi8.coui.kmp.interfaces.HoldDownObserver
import io.github.suqi8.coui.kmp.kit.backdrop.CouiBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.LocalCouiBackdrop
import io.github.suqi8.coui.kmp.kit.effect.isKitSurfaceSupported
import io.github.suqi8.coui.kmp.kit.topappbar.CouiTopAppBarButtonDefaults
import io.github.suqi8.coui.kmp.kit.topappbar.CouiTopAppBarDefaults
import io.github.suqi8.coui.kmp.kit.topappbar.LocalCouiToolbarBackdrop
import io.github.suqi8.coui.kmp.kit.topappbar.LocalCouiToolbarSampleBlurRadius
import io.github.suqi8.coui.kmp.kit.topappbar.TopAppBarButton
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalContentColor

/**
 * The ColorOS 17 toolbar icon button with its glass material and soft halo.
 *
 * ColorOS gives the toolbar's back key and overflow key the same effect: a true 40dp circle — not a
 * squircle — carrying frosted glass sampling (or a radial fallback fill when no backdrop is
 * available), an angle-distributed material perimeter highlight, and a theme-dependent halo outside
 * the edge. While pressed the whole control springs to [HighlightedIconButtonDefaults.PressedScale],
 * the rim and the halo disappear and the fill and icon brighten; both themes behave identically.
 *
 * When a [backdrop] (defaulting to [LocalBackdrop]) or an enclosing toolbar surface backdrop is
 * present on platforms that support native floating surfaces, the button uses the unified COUI 17
 * toolbar floating glass renderer (`TopAppBarButton`) so circular icon actions, red-dot actions,
 * and text capsules share identical glass, rim, and shadow rendering.
 *
 * The content colour is published through [LocalContentColor], so a default [Icon] in [content]
 * follows the fill as it brightens.
 *
 * @param onClick The callback when the button is clicked.
 * @param modifier The modifier to be applied to the button.
 * @param size The diameter of the button.
 * @param enabled Whether the button can be clicked.
 * @param backdrop The backdrop blurred inside the disc, defaulting to [LocalBackdrop], or `null`
 *   for the unblurred fallback preset.
 * @param spotLightEnabled Whether pressing lights the disc from the touch point
 *   (`TYPE_FRAMEWORK_CIRCLE_1` spotlight).
 * @param redDot Optional toolbar menu red dot (`COUIToolbar.setRedDot` ->
 *   `COUIActionMenuView.drawRedDot`): `null` for none, `0` for a plain dot, a positive number for
 *   a count [Badge]. Anchored at the top-end corner of the centered
 *   [IconButtonDefaults.RedDotAnchorSize] icon area without expanding the button bounds.
 * @param content The content of the button, typically an [Icon], drawn centred in the circle.
 */
@Composable
fun HighlightedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = HighlightedIconButtonDefaults.Size,
    enabled: Boolean = true,
    backdrop: Backdrop? = LocalBackdrop.current,
    spotLightEnabled: Boolean = true,
    redDot: Int? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    if (redDot != null) {
        ToolbarRedDotLayout(
            redDot = redDot,
            modifier = modifier,
        ) {
            HighlightedIconButtonContent(
                onClick = onClick,
                modifier = Modifier,
                size = size,
                enabled = enabled,
                backdrop = backdrop,
                spotLightEnabled = spotLightEnabled,
                content = content,
            )
        }
        return
    }
    HighlightedIconButtonContent(
        onClick = onClick,
        modifier = modifier,
        size = size,
        enabled = enabled,
        backdrop = backdrop,
        spotLightEnabled = spotLightEnabled,
        content = content,
    )
}

@Composable
private fun HighlightedIconButtonContent(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp,
    enabled: Boolean,
    backdrop: Backdrop?,
    spotLightEnabled: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val blurEnabled = isBlurEnabled()
    val ambientCouiBackdrop = LocalCouiToolbarBackdrop.current ?: LocalCouiBackdrop.current
    val useKitToolbarSurface = isKitSurfaceSupported() &&
        blurEnabled &&
        spotLightEnabled &&
        size == HighlightedIconButtonDefaults.Size &&
        (ambientCouiBackdrop != null || backdrop != null)
    val kitBackdrop = if (useKitToolbarSurface) {
        ambientCouiBackdrop ?: backdrop?.let { remember(it, density) { CouiBackdrop(it, density) } }
    } else {
        null
    }
    kitBackdrop?.RetainWhileComposed()
    if (kitBackdrop != null) {
        val sampleBlurRadius = if (ambientCouiBackdrop != null) {
            LocalCouiToolbarSampleBlurRadius.current
        } else {
            with(density) { CouiTopAppBarDefaults.referenceBlurRadius.toPx() }
        }
        ToolbarSurfaceIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            backdrop = kitBackdrop,
            blurRadius = sampleBlurRadius,
            content = content,
        )
        return
    }
    val touchMotion = rememberCOUITouchMotionState()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (enabled && (isPressed || touchMotion.values.value.pressed)) 1f else 0f,
        animationSpec = HighlightedIconButtonDefaults.PressSpring,
        label = "highlightedIconButtonPress",
    )

    val scheme = COUITheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val blurScale = rememberToolbarAttenuatedBlurScale()
    val scale = 1f - (1f - HighlightedIconButtonDefaults.PressedScale) * pressProgress
    val contentColor = lerp(
        scheme.onBackground,
        Color.White,
        HighlightedIconButtonDefaults.PressedContentBrighten * pressProgress,
    )

    val currentOnClick by rememberUpdatedState(onClick)
    val clickableModifier = remember(enabled, interactionSource) {
        if (enabled) {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { currentOnClick() },
            )
        } else {
            Modifier
        }
    }
    // Opt-in TYPE_FRAMEWORK_TOP_BAR_BLUR: only the blur is sampled, since the measured radial fill
    // above already carries the preset's colour layers.
    val glassDiscModifier = if (backdrop != null && blurEnabled) {
        Modifier.couiGlassSurface(
            backdrop = backdrop,
            shape = CircleShape,
            containerColor = Color.Transparent,
            fallback = Modifier,
            blurRadius = with(density) {
                (HighlightedIconButtonDefaults.NATIVE_BLUR_RADIUS_PX / TEXTURE_BLUR_SIGMA_SCALE).toDp().value
            } * blurScale(),
            blendColors = emptyList(),
        )
    } else {
        Modifier
    }

    val lightShape = remember(size) { CenteredDiscShape(size) }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                // COUIActionMenuItemView sizes each action to coui_action_menu_item_min_width and
                // _min_height, which are the 40dp square the disc draws: the touch target is the
                // key, and the toolbar spaces keys apart instead of padding them.
                .defaultMinSize(
                    minWidth = HighlightedIconButtonDefaults.IconMenuMinWidth,
                    minHeight = HighlightedIconButtonDefaults.IconMenuMinHeight,
                )
                .couiTouchMotion(
                    enabled = enabled,
                    motionState = touchMotion,
                    pressScaleEnabled = false,
                    // The light belongs to the disc; a plain circle clip on the pressed box would
                    // light a pill around it.
                    shape = lightShape,
                    lightParams = SpotLightDefaults.circleParams1(isDark),
                    lightEnabled = spotLightEnabled,
                    extraTransform = {
                        // Press shrink and native drag deformation belong to one visual layer.
                        scaleX += scale - 1f
                        scaleY += scale - 1f
                    },
                )
                .then(clickableModifier),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center,
            ) {
                if (isDark) {
                    MaterialCausticShadow(
                        particles = listOf(
                            CausticShadowParticle(
                                MetaBallParticle.circle(size / 2, size / 2, size),
                                CausticShadowProfile.Toolbar,
                            ),
                        ),
                        color = HighlightedIconButtonDefaults.DarkHaloColor.copy(
                            alpha = HighlightedIconButtonDefaults.DarkHaloColor.alpha * (1f - pressProgress).coerceIn(0f, 1f),
                        ),
                        modifier = Modifier.matchParentSize(),
                    )
                }
                Box(
                    modifier = Modifier.matchParentSize()
                        .drawWithContent {
                            if (!isDark && pressProgress < 1f) drawSoftDropHalo(center, this.size.minDimension / 2f, 1f - pressProgress)
                            drawContent()
                        }
                        .then(glassDiscModifier)
                        .nativeMaterialEdge(
                            particles = listOf(MetaBallParticle.circle(size / 2, size / 2, size)),
                            rim = Color.White.copy(alpha = HighlightedIconButtonDefaults.stroke(isDark).edgeAlpha * (1f - pressProgress).coerceIn(0f, 1f)),
                            edgeStyle = GradientStrokeStyle.EDGE_STYLE_1,
                            lineWidth = HighlightedIconButtonDefaults.StrokeWidth,
                            extension = 0.dp,
                        )
                        .drawWithContent {
                            val radius = this.size.minDimension / 2f
                            val center = this.center
                            val brighten = HighlightedIconButtonDefaults.PressedFillBrighten * pressProgress
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colorStops = arrayOf(
                                        0f to lerp(
                                            HighlightedIconButtonDefaults.fillCenter(isDark),
                                            Color.White,
                                            brighten,
                                        ).copy(alpha = HighlightedIconButtonDefaults.FillAlpha),
                                        HighlightedIconButtonDefaults.FillEdgeStop to lerp(
                                            HighlightedIconButtonDefaults.fillEdge(isDark),
                                            Color.White,
                                            brighten,
                                        ).copy(alpha = HighlightedIconButtonDefaults.FillAlpha),
                                    ),
                                    center = center,
                                    radius = radius,
                                ),
                                radius = radius,
                                center = center,
                            )
                            drawContent()
                        },
                )
                // Keep the icon out of the material RGB effect.
                Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center, content = content)
            }
        }
    }
}

/** Binary-compatibility bridge for the [HighlightedIconButton] overload that predates `redDot`. */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds redDot.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun HighlightedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = HighlightedIconButtonDefaults.Size,
    enabled: Boolean = true,
    backdrop: Backdrop? = null,
    spotLightEnabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    HighlightedIconButton(
        onClick = onClick,
        modifier = modifier,
        size = size,
        enabled = enabled,
        backdrop = backdrop,
        spotLightEnabled = spotLightEnabled,
        redDot = null,
        content = content,
    )
}

/** A disc of [diameter] in the middle of whatever box it outlines. */
private class CenteredDiscShape(private val diameter: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radius = with(density) { diameter.toPx() } / 2f
        return Outline.Rounded(RoundRect(Rect(size.center, radius), CornerRadius(radius)))
    }

    override fun equals(other: Any?): Boolean = other is CenteredDiscShape && other.diameter == diameter

    override fun hashCode(): Int = diameter.hashCode()
}

/** Binary-compatibility bridge for [HighlightedIconButton] callers compiled before backdrop/spotLight args. */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload with backdrop and spotLightEnabled.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun HighlightedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = HighlightedIconButtonDefaults.Size,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    HighlightedIconButton(
        onClick = onClick,
        modifier = modifier,
        size = size,
        enabled = enabled,
        backdrop = null,
        spotLightEnabled = true,
        content = content,
    )
}

/**
 * The light theme's halo: a soft dark shadow all around the rim, offset slightly downward.
 *
 * Clipped outside the disc so the shadow never darkens the translucent frosted fill inside, and
 * eased smoothly across multiple stops to avoid a hard circular band.
 */
private fun DrawScope.drawSoftDropHalo(center: Offset, radius: Float, progress: Float) {
    val haloRadius = HighlightedIconButtonDefaults.LightHaloRadius.toPx()
    val outerRadius = radius + haloRadius
    val haloCenter = center + Offset(0f, HighlightedIconButtonDefaults.LightHaloOffsetY.toPx())
    val baseAlpha = HighlightedIconButtonDefaults.LightHaloColor.alpha * progress
    if (baseAlpha <= 0f || outerRadius <= radius) return
    val innerStop = (radius / outerRadius).coerceIn(0f, 0.99f)
    val span = 1f - innerStop
    val discCutout = Path().apply {
        addOval(Rect(center = center, radius = radius))
    }
    clipPath(discCutout, ClipOp.Difference) {
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.Black.copy(alpha = baseAlpha),
                    innerStop to Color.Black.copy(alpha = baseAlpha),
                    (innerStop + span * 0.25f) to Color.Black.copy(alpha = baseAlpha * 0.56f),
                    (innerStop + span * 0.55f) to Color.Black.copy(alpha = baseAlpha * 0.20f),
                    (innerStop + span * 0.80f) to Color.Black.copy(alpha = baseAlpha * 0.05f),
                    1f to Color.Transparent,
                ),
                center = haloCenter,
                radius = outerRadius,
            ),
            radius = outerRadius,
            center = haloCenter,
        )
    }
}

/**
 * The light theme's halo for a capsule toolbar button: layered smooth falloff clipped outside the
 * capsule silhouette so the frosted interior stays bright and clean.
 */
private fun DrawScope.drawSoftCapsuleDropHalo(
    capsuleShape: Shape,
    progress: Float,
) {
    val haloRadius = HighlightedIconButtonDefaults.LightHaloRadius.toPx()
    val offsetY = HighlightedIconButtonDefaults.LightHaloOffsetY.toPx()
    val baseAlpha = HighlightedIconButtonDefaults.LightHaloColor.alpha * progress
    if (baseAlpha <= 0f || size.minDimension <= 0f) return
    val cutout = Path().apply {
        addOutline(capsuleShape.createOutline(size, layoutDirection, this@drawSoftCapsuleDropHalo))
    }
    val steps = 6
    val stepAlpha = baseAlpha / steps * 1.45f
    clipPath(cutout, ClipOp.Difference) {
        for (i in 1..steps) {
            val t = i / steps.toFloat()
            val expand = haloRadius * t * t
            val layerAlpha = stepAlpha * (1f - t * 0.72f)
            val corner = size.height / 2f + expand
            drawRoundRect(
                color = Color.Black.copy(alpha = layerAlpha),
                topLeft = Offset(-expand, -expand + offsetY * t),
                size = Size(size.width + expand * 2f, size.height + expand * 2f),
                cornerRadius = CornerRadius(corner, corner),
            )
        }
    }
}

/** Defaults for [HighlightedIconButton]. */
object HighlightedIconButtonDefaults {

    /** The diameter of the button. Measurement: a true circle, 155px at 1dp = 3.875px, i.e. 40dp. */
    val Size = 40.dp

    /**
     * The action box of a toolbar icon, `dimen/coui_action_menu_item_min_width`: 40dp, the same
     * square the disc draws, so a key never carries a touch target wider than the shape it shows.
     *
     * Keys in one toolbar sit [TopAppBarDefaults.ActionMenuItemSpacing] (10dp) apart and the last
     * one keeps the toolbar's 16dp end margin — two 40dp keys show a 10dp gap, and the row ends
     * 16dp from the window edge. Space the keys with that token rather than with the box, which is
     * the key itself.
     */
    val IconMenuMinWidth = 40.dp

    /** The action box height, `dimen/coui_action_menu_item_min_height`: 40dp, so the box is square. */
    val IconMenuMinHeight = 40.dp

    /**
     * The blur sigma of `TYPE_FRAMEWORK_TOP_BAR_BLUR` (COUIMaterialBlurEffect preset 2), in pixels.
     */
    const val TOP_BAR_BLUR_SIGMA_PX = 20f

    /**
     * The blur sigma of `FRAMEWORK_TOP_BAR_BLUR_PARAMS`, in pixels, used when a backdrop is passed.
     * Inside a blurred [AppBarBlurOverlay] or `TopAppBar(gradientGlass = true)` it drops toward
     * 0.35x.
     */
    const val NATIVE_BLUR_RADIUS_PX = 20f

    /**
     * The scale the whole control springs to while pressed. Measurement: the 77.5px radius shrinks
     * to 69px, a ratio of ≈0.89.
     */
    val PressedScale = 0.89f

    /**
     * The press spring: the COUI press-feedback spring (`COUISpringForce`, response 0.3s / bounce 0).
     */
    val PressSpring: SpringSpec<Float> = folmeSpring(
        damping = 1f,
        response = 0.3f,
        visibilityThreshold = 0.001f,
    )

    /** The alpha the radial fill is painted at. Measurement: a two-point estimate of ≈0.78. */
    val FillAlpha = 0.78f

    /**
     * The radial-fill stop at which the edge colour is reached, as a fraction of the button radius.
     * Measurement: the edge colour appears at r=70px of the 77.5px radius in the dark theme and at
     * r=68px in the light theme, i.e. ≈0.88–0.90 of the radius.
     */
    val FillEdgeStop = 0.89f

    /** The radial-fill centre colour of the dark theme. Measurement: #1e1e1e at r=0. */
    val FillCenterDark = Color(0xFF1E1E1E)

    /** The radial-fill edge colour of the dark theme. Measurement: #242424 at r=70px. */
    val FillEdgeDark = Color(0xFF242424)

    /** The radial-fill centre colour of the light theme. Measurement: 249 at r=0. */
    val FillCenterLight = Color(0xFFF9F9F9)

    /** The radial-fill edge colour of the light theme. Measurement: 251 at r=68px. */
    val FillEdgeLight = Color(0xFFFBFBFB)

    /** The radial-fill centre colour for [isDark]. */
    fun fillCenter(isDark: Boolean): Color = if (isDark) FillCenterDark else FillCenterLight

    /** The radial-fill edge colour for [isDark]. */
    fun fillEdge(isDark: Boolean): Color = if (isDark) FillEdgeDark else FillEdgeLight

    /**
     * How far the pressed fill is lerped toward white. The measurement records that the fill
     * brightens while pressed but carries no magnitude, so this is a nominal value.
     */
    val PressedFillBrighten = 0.06f

    /**
     * How far the pressed content colour is lerped toward white; see [PressedFillBrighten].
     */
    val PressedContentBrighten = 0.1f

    /** TYPE_FRAMEWORK_CIRCLE_1 dark edge alpha; applied through native material RGB blending. */
    val StrokeAlpha = 0.6f

    /** TYPE_FRAMEWORK_CIRCLE_1 light edge alpha. */
    val LightStrokeAlpha = 0.2f

    /** TYPE_FRAMEWORK_CIRCLE_1 edge width in both themes. */
    val StrokeWidth = 1.9.dp

    /** TYPE_FRAMEWORK_CIRCLE_1 dark inner-shadow parameter, retained with the native preset. */
    val StrokeShadowFadeIn = 0.1f

    /** The regular dark toolbar circle preset, not the popup-window circle variant. */
    val Stroke = MaterialStrokeParams(
        edgeAlpha = StrokeAlpha,
        edgeWidth = StrokeWidth,
        edgeAngle = 0f,
        shadowFadeIn = StrokeShadowFadeIn,
        shadowFadeOut = 1f,
        shadowFadeScale = 1f,
    )

    /** TYPE_FRAMEWORK_CIRCLE_1_LIGHT from ToolbarMaterialEffectResolver. */
    val LightStroke = Stroke.copy(edgeAlpha = LightStrokeAlpha, shadowFadeIn = 0.15f)

    /** Resolves the regular toolbar edge parameters by theme. */
    fun stroke(isDark: Boolean): MaterialStrokeParams = if (isDark) Stroke else LightStroke

    /** ToolbarMaterialEffectDelegate uses coui_caustic_color_1: 0x33ffffff at night. */
    val DarkHaloColor = Color.White.copy(alpha = 51f / 255f)

    /** Native level-2 bottom layout offset; the SDF fades before reaching the draw rectangle. */
    val DarkHaloExtent = 4.899994.dp

    /** Native level-2 distance field width. */
    val DarkHaloSdfWidth = 9.22998.dp

    /**
     * The light theme's halo colour. Measurement: a soft dark drop shadow, deepest -13 luma over the
     * background the button was measured against.
     *
     * Provisional: alpha lowered from 0.05 to 0.03 because the user reports the ColorOS 17 halo reads
     * heavier than on the device. No circular-button reference could be measured on the test
     * devices, so this is a fallback value, to be calibrated against a live reference.
     */
    val LightHaloColor = Color.Black.copy(alpha = 0.03f)

    /**
     * How far the light theme's halo reaches past the edge. Measurement: ≈60px ≈ 15.5dp.
     *
     * Provisional: lowered from 15.5dp to 12dp together with [LightHaloColor], pending a live
     * reference measurement.
     */
    val LightHaloRadius = 12.dp

    /**
     * The light theme's halo vertical offset. Measurement: the halo is near symmetric but
     * bottom-weighted (bottom -13, sides -11, top -8 luma).
     */
    val LightHaloOffsetY = 1.dp
}

@Composable
private fun ToolbarSurfaceIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backdrop: CouiBackdrop,
    blurRadius: Float,
    content: @Composable BoxScope.() -> Unit,
) {
    TopAppBarButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        enforceMinimumTouchTarget = false,
        backdrop = backdrop,
        style = CouiTopAppBarButtonDefaults.style(search = false, blurRadius = blurRadius),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/**
 * Shared COUI 17 frosted-glass capsule surface for toolbar text actions ([ToolbarTextButton] and
 * [PinnedTopAppBarTextAction]). Uses the same kit floating glass pipeline (`TopAppBarButton`)
 * when a backdrop and platform surface support are present, and falls back to a matched frosted
 * capsule with material rim highlight and soft halo otherwise.
 */
@Composable
internal fun ToolbarGlassCapsuleSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    holdDownState: Boolean = false,
    backdrop: Backdrop? = LocalBackdrop.current,
    minWidth: Dp = IconButtonDefaults.TextButtonMinWidth,
    maxWidth: Dp = IconButtonDefaults.TextButtonMaxWidth,
    minHeight: Dp = IconButtonDefaults.ToolbarButtonHeight,
    contentPadding: PaddingValues = IconButtonDefaults.TextButtonInsideMargin,
    content: @Composable RowScope.() -> Unit,
) {
    val density = LocalDensity.current
    val blurEnabled = isBlurEnabled()
    val ambientCouiBackdrop = LocalCouiToolbarBackdrop.current ?: LocalCouiBackdrop.current
    val useKitToolbarSurface = isKitSurfaceSupported() &&
        blurEnabled &&
        !holdDownState &&
        (ambientCouiBackdrop != null || backdrop != null)
    val kitBackdrop = if (useKitToolbarSurface) {
        ambientCouiBackdrop ?: backdrop?.let { remember(it, density) { CouiBackdrop(it, density) } }
    } else {
        null
    }
    kitBackdrop?.RetainWhileComposed()
    if (kitBackdrop != null) {
        val sampleBlurRadius = if (ambientCouiBackdrop != null) {
            LocalCouiToolbarSampleBlurRadius.current
        } else {
            with(density) { CouiTopAppBarDefaults.referenceBlurRadius.toPx() }
        }
        TopAppBarButton(
            modifier = modifier,
            onClick = onClick,
            enabled = enabled,
            text = true,
            enforceMinimumTouchTarget = false,
            backdrop = kitBackdrop,
            minWidth = minWidth,
            maxWidth = maxWidth,
            height = minHeight,
            contentPadding = contentPadding,
            style = CouiTopAppBarButtonDefaults.style(search = false, blurRadius = sampleBlurRadius),
            content = content,
        )
        return
    }

    val touchMotion = rememberCOUITouchMotionState()
    val interactionSource = remember { MutableInteractionSource() }
    HoldDownObserver(holdDownState, interactionSource)
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (enabled && (isPressed || touchMotion.values.value.pressed)) 1f else 0f,
        animationSpec = HighlightedIconButtonDefaults.PressSpring,
        label = "toolbarTextCapsulePress",
    )

    val scheme = COUITheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val blurScale = rememberToolbarAttenuatedBlurScale()
    val scale = 1f - (1f - HighlightedIconButtonDefaults.PressedScale) * pressProgress
    val squircleEnabled = isSquircleEnabled()
    val capsuleShape = remember(squircleEnabled) { CapsuleOutlineShape(squircleEnabled = squircleEnabled) }
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }

    val currentOnClick by rememberUpdatedState(onClick)
    val clickableModifier = remember(enabled, interactionSource) {
        if (enabled) {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { currentOnClick() },
            )
        } else {
            Modifier
        }
    }

    val glassCapsuleModifier = if (backdrop != null && blurEnabled) {
        Modifier.couiGlassSurface(
            backdrop = backdrop,
            shape = capsuleShape,
            containerColor = Color.Transparent,
            fallback = Modifier,
            blurRadius = with(density) {
                (HighlightedIconButtonDefaults.NATIVE_BLUR_RADIUS_PX / TEXTURE_BLUR_SIGMA_SCALE).toDp().value
            } * blurScale(),
            blendColors = emptyList(),
        )
    } else {
        Modifier
    }

    val particleWidth = with(density) { measuredSize.width.toDp() }.coerceAtLeast(minWidth)
    val particleHeight = with(density) { measuredSize.height.toDp() }.coerceAtLeast(minHeight)
    val capsuleParticle = MetaBallParticle(
        centerX = particleWidth / 2,
        centerY = particleHeight / 2,
        width = particleWidth,
        height = particleHeight,
        cornerRadius = particleHeight / 2,
    )

    Box(
        modifier = modifier
            .widthIn(min = minWidth, max = maxWidth)
            .height(minHeight)
            .onSizeChanged { measuredSize = it }
            .couiTouchMotion(
                enabled = enabled,
                motionState = touchMotion,
                pressScaleEnabled = false,
                shape = capsuleShape,
                lightParams = SpotLightDefaults.capsuleParams1(isDark),
                lightEnabled = true,
                extraTransform = {
                    scaleX += scale - 1f
                    scaleY += scale - 1f
                },
            )
            .then(clickableModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (isDark && measuredSize.width > 0 && measuredSize.height > 0) {
            MaterialCausticShadow(
                particles = listOf(
                    CausticShadowParticle(
                        capsuleParticle,
                        CausticShadowProfile.Toolbar,
                    ),
                ),
                color = HighlightedIconButtonDefaults.DarkHaloColor.copy(
                    alpha = HighlightedIconButtonDefaults.DarkHaloColor.alpha * (1f - pressProgress).coerceIn(0f, 1f),
                ),
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    if (!isDark && pressProgress < 1f) {
                        drawSoftCapsuleDropHalo(capsuleShape, 1f - pressProgress)
                    }
                    drawContent()
                }
                .then(glassCapsuleModifier)
                .nativeMaterialEdge(
                    particles = listOf(capsuleParticle),
                    rim = Color.White.copy(
                        alpha = HighlightedIconButtonDefaults.stroke(isDark).edgeAlpha * (1f - pressProgress).coerceIn(0f, 1f),
                    ),
                    edgeStyle = GradientStrokeStyle.EDGE_STYLE_1,
                    lineWidth = 2.2.dp,
                    extension = 0.dp,
                    continuousCurve = squircleEnabled,
                )
                .clip(capsuleShape)
                .drawWithContent {
                    val brighten = HighlightedIconButtonDefaults.PressedFillBrighten * pressProgress
                    val radius = size.maxDimension / 2f
                    drawRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to lerp(
                                    HighlightedIconButtonDefaults.fillCenter(isDark),
                                    Color.White,
                                    brighten,
                                ).copy(alpha = HighlightedIconButtonDefaults.FillAlpha),
                                HighlightedIconButtonDefaults.FillEdgeStop to lerp(
                                    HighlightedIconButtonDefaults.fillEdge(isDark),
                                    Color.White,
                                    brighten,
                                ).copy(alpha = HighlightedIconButtonDefaults.FillAlpha),
                            ),
                            center = center,
                            radius = radius.coerceAtLeast(1f),
                        ),
                    )
                    drawContent()
                },
        )
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
