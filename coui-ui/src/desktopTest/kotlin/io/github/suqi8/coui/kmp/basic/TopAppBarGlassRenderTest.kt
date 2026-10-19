// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private const val APP_BAR_TAG = "glass-app-bar"

/** Short enough that the title glyphs stop well before the left sample column. */
private const val APP_BAR_TITLE = "G"

private const val SURFACE_WIDTH_DP = 240f
private const val SURFACE_HEIGHT_DP = 120f

/** How far the material paints past the bar's bottom edge. */
private val ExtraHeightPx = with(Density(1f)) { AppBarBlurOverlayDefaults.ExtraHeight.toPx() }

/** The recorded backdrop is split down the middle: blue on the left half, red on the right half. */
private val BackdropLeftColor = Color(0xFF0000FF)
private val BackdropRightColor = Color(0xFFFF0000)

/**
 * An opaque flat cover drawn on top of the backdrop layer and below the app bar. Whatever the bar
 * shows over it either came from the backdrop or is flat — the cover itself carries no split.
 */
private val CoverColor = Color(0xFF00FF00)

/**
 * The app bar paints its `color` on its own when no backdrop is published, and samples the backdrop
 * (blurred, tinted by that same `color` at the ColorOS background alpha) when one is. A flat cover
 * sits between the recorded layer and the bar, so a left/right difference inside the bar can only
 * come from the sampling path: a bar that failed to sample, sampled an empty layer, or simply
 * composited its own translucent fill over the cover would come out uniform.
 */
@OptIn(ExperimentalTestApi::class)
class TopAppBarGlassRenderTest {

    @Test
    fun appBarSamplesTheLayerBehindItInsteadOfPaintingAFlatColor() = runSkikoComposeUiTest(
        size = Size(SURFACE_WIDTH_DP, SURFACE_HEIGHT_DP),
        density = Density(1f),
    ) {
        setContent { GlassScene(withBackdrop = true) }
        waitForIdle()

        val (left, right) = captureAppBarSamples()

        assertNotEquals(left, right, "the bar must pick up the blue/red split of the backdrop")
        assertTrue(
            maxChannelDelta(left, right) >= 8,
            "left/right must differ on some channel by at least 8, got $left vs $right",
        )
        assertNotEquals(Color.White.toArgb(), left.toArgb(), "the left sample must not be flat white")
        assertNotEquals(Color.White.toArgb(), right.toArgb(), "the right sample must not be flat white")
    }

    /**
     * Control: with no [LocalBackdrop] the bar is expected to fall back to its plain fill, which
     * makes the assertions above meaningful — the sampled row really is inside the bar.
     */
    @Test
    fun withoutABackdropTheAppBarPaintsOneFlatColor() = runSkikoComposeUiTest(
        size = Size(SURFACE_WIDTH_DP, SURFACE_HEIGHT_DP),
        density = Density(1f),
    ) {
        setContent { GlassScene(withBackdrop = false) }
        waitForIdle()

        val (left, right) = captureAppBarSamples()

        assertEquals(left.toArgb(), right.toArgb(), "the plain fill must be uniform across the bar")
        assertTrue(
            channelDistanceToWhite(left) <= 2,
            "the plain fill is expected to be white, got $left",
        )
    }

    /**
     * The gradient shape (`AppBarBlurHelper`'s `GradientBackgroundDrawable`) ramps the surface's
     * alpha to zero at the material's bottom edge, which sits [ExtraHeightPx] below the bar. A
     * column through the material must fade from the painted surface at the top into the raw
     * backdrop at the material bottom. The backdrop is flat blue under the sampled column, so the
     * white surface's red channel is the ramp: 255 at the top, 0 once the ramp has run out.
     */
    @Test
    fun gradientAppBarFadesTowardItsBottomEdge() = runSkikoComposeUiTest(
        size = Size(SURFACE_WIDTH_DP, SURFACE_HEIGHT_DP),
        density = Density(1f),
    ) {
        setContent { GlassScene(withBackdrop = true, gradient = true) }
        waitForIdle()

        val (top, middle, bottom) = captureVerticalAppBarSamples()

        assertTrue(redChannel(top) >= 200, "the top of the material is the painted surface, got $top")
        assertTrue(
            redChannel(top) > redChannel(middle) + 40,
            "the ramp must have faded by mid-height, got top=$top middle=$middle",
        )
        assertTrue(
            redChannel(middle) > redChannel(bottom) + 40,
            "the ramp must keep fading below mid-height, got middle=$middle bottom=$bottom",
        )
        assertTrue(redChannel(bottom) <= 60, "the material bottom is the bare backdrop, got $bottom")
        assertTrue(blueChannel(bottom) >= 180, "the material bottom keeps the blue backdrop, got $bottom")
    }

    /**
     * Control for [gradientAppBarFadesTowardItsBottomEdge]: the uniform shape keeps the same
     * background alpha down the bar, so the fade measured above can only come from the gradient and
     * not from the sampled column, the blur, or the surface itself.
     *
     * The top and middle samples are read at the gradient test's own rows, which extend past the
     * bar by the material band. The middle row sits about 38px below the bar's top, inside the 52dp
     * bar, so both samples fall on the uniform surface and must not fade. The bottom sample stays
     * inside the bar, at its bottom edge, where the surface must still be painted.
     */
    @Test
    fun uniformAppBarStaysOpaqueAtItsBottomEdge() = runSkikoComposeUiTest(
        size = Size(SURFACE_WIDTH_DP, SURFACE_HEIGHT_DP),
        density = Density(1f),
    ) {
        setContent { GlassScene(withBackdrop = true, gradient = false) }
        waitForIdle()

        val (top, middle, _) = captureVerticalAppBarSamples()
        val (_, _, bottom) = captureVerticalAppBarSamples(materialPaintsExtraHeight = false)

        assertTrue(
            abs(redChannel(top) - redChannel(middle)) <= 20,
            "the uniform surface must not fade inside the bar, got top=$top middle=$middle",
        )
        assertTrue(redChannel(bottom) >= 150, "the uniform surface stays painted, got $bottom")
        assertTrue(
            maxChannelDelta(top, bottom) <= 2,
            "the uniform surface must not vary with height, got top=$top bottom=$bottom",
        )
    }

    /**
     * `centerTitle` (COUI `setIsTitleCenterStyle`) puts the title in the middle of the bar instead
     * of after the navigation icon. The glyph is the only dark thing on the white surface, so its
     * horizontal centre is readable straight off the pixels.
     */
    @Test
    fun centerTitlePutsTheTitleInTheMiddleOfTheBar() = runSkikoComposeUiTest(
        size = Size(SURFACE_WIDTH_DP, SURFACE_HEIGHT_DP),
        density = Density(1f),
    ) {
        setContent { TitleOnlyScene(centerTitle = true) }
        waitForIdle()

        val bar = appBarBounds()
        val glyph = capturePixelBounds(bar) { isDark(it) }

        assertTrue(
            abs(glyph.centerX - bar.center.x) <= 8f,
            "the centred title must sit on the bar's centre, got glyph=$glyph bar=$bar",
        )
    }

    /**
     * Control for [centerTitlePutsTheTitleInTheMiddleOfTheBar]: without it the title is laid out
     * from the start edge, so the measurement above really discriminates the two forms.
     */
    @Test
    fun withoutCenterTitleTheTitleStartsAtTheLeadingEdge() = runSkikoComposeUiTest(
        size = Size(SURFACE_WIDTH_DP, SURFACE_HEIGHT_DP),
        density = Density(1f),
    ) {
        setContent { TitleOnlyScene(centerTitle = false) }
        waitForIdle()

        val bar = appBarBounds()
        val glyph = capturePixelBounds(bar) { isDark(it) }

        assertTrue(
            glyph.centerX < bar.center.x - 40f,
            "the start-aligned title must stay left of the centre, got glyph=$glyph bar=$bar",
        )
    }

    /**
     * The segment slot (`COUIToolbar.setSegmentButtons`) is laid out at `segment_button_height`
     * (32dp), inside COUI's `coui_toolbar_segment_button_min_width` / `_max_width` band
     * (108–207dp), and centred between the navigation icon and the actions.
     */
    @Test
    fun segmentSlotIsCenteredAndThirtyTwoDpTall() = runSkikoComposeUiTest(
        size = Size(SURFACE_WIDTH_DP, SURFACE_HEIGHT_DP),
        density = Density(1f),
    ) {
        setContent { SegmentScene() }
        waitForIdle()

        val bar = appBarBounds()
        val slot = capturePixelBounds(bar) { isSegment(it) }

        assertEquals(32, slot.height, "the segment slot is 32dp tall, got $slot")
        assertTrue(slot.width in 108..207, "the segment slot stays in 108..207dp, got $slot")
        assertTrue(
            abs(slot.centerX - bar.center.x) <= 2f,
            "the segment slot must be centred, got slot=$slot bar=$bar",
        )
    }
}

/** A 240dp-wide surface with just a title on it, so the glyph is the only thing to measure. */
@Composable
private fun TitleOnlyScene(centerTitle: Boolean) {
    Box(Modifier.fillMaxSize()) {
        SmallTopAppBar(
            title = APP_BAR_TITLE,
            modifier = Modifier.fillMaxWidth().testTag(APP_BAR_TAG),
            color = Color.White,
            centerTitle = centerTitle,
            defaultWindowInsetsPadding = false,
        )
    }
}

/** A cyan fill handed to the segment slot, so the slot's own bounds are measurable. */
private val SegmentColor = Color(0xFF00FFFF)

@Composable
private fun SegmentScene() {
    Box(Modifier.fillMaxSize()) {
        SmallTopAppBar(
            title = "Title",
            modifier = Modifier.fillMaxWidth().testTag(APP_BAR_TAG),
            color = Color.White,
            segmentButton = {
                Box(Modifier.fillMaxSize().background(SegmentColor))
            },
            defaultWindowInsetsPadding = false,
        )
    }
}

@Composable
private fun GlassScene(withBackdrop: Boolean, gradient: Boolean = false) {
    val backdrop = rememberLayerBackdrop {
        drawRect(Color.Black)
        drawContent()
    }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val half = size.width / 2f
                drawRect(BackdropLeftColor, size = Size(half, size.height))
                drawRect(
                    color = BackdropRightColor,
                    topLeft = Offset(half, 0f),
                    size = Size(size.width - half, size.height),
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(CoverColor),
        )
        if (withBackdrop) {
            CompositionLocalProvider(LocalBackdrop provides backdrop) { GlassAppBar(gradient) }
        } else {
            GlassAppBar(gradient)
        }
    }
}

@Composable
private fun GlassAppBar(gradient: Boolean = false) {
    SmallTopAppBar(
        title = APP_BAR_TITLE,
        modifier = Modifier.fillMaxWidth().testTag(APP_BAR_TAG),
        color = Color.White,
        gradientGlass = gradient,
        defaultWindowInsetsPadding = false,
    )
}

/**
 * Reads the two comparison pixels: the same row across the middle of the bar, at a quarter and
 * three quarters of the surface width.
 */
@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.captureAppBarSamples(): Pair<Color, Color> {
    val bar = onNodeWithTag(APP_BAR_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
    val image = onRoot().captureToImage()
    val pixels = image.toPixelMap()
    val sampleY = ((bar.top + bar.bottom) / 2f).roundToInt()
    val leftX = image.width / 4
    val rightX = image.width * 3 / 4
    val left = pixels[leftX, sampleY]
    val right = pixels[rightX, sampleY]
    println(
        "[glass] surface=${image.width}x${image.height} bar=$bar y=$sampleY " +
            "left($leftX)=${left.argbHex()} right($rightX)=${right.argbHex()}",
    )
    assertTrue(
        bar.top <= sampleY && sampleY < bar.bottom && leftX >= bar.left && rightX <= bar.right,
        "the sample points must sit inside the bar, bar=$bar y=$sampleY left=$leftX right=$rightX",
    )
    return left to right
}

private fun Color.argbHex(): String = "#%08X".format(toArgb())

/**
 * Reads three pixels down the material's left quarter: just below the top edge, mid-height, and
 * just above the material's bottom edge. The gradient shape paints [ExtraHeightPx] past the bar, so
 * [materialPaintsExtraHeight] extends the sampled material to the bar bottom plus that band; the
 * uniform shape stops at the bar. The backdrop is flat blue under that column, so a vertical ramp
 * in the material shows up in the red channel — the painted surface carries red 255, the blue
 * backdrop carries 0.
 */
@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.captureVerticalAppBarSamples(
    materialPaintsExtraHeight: Boolean = true,
): Triple<Color, Color, Color> {
    val bar = onNodeWithTag(APP_BAR_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
    val image = onRoot().captureToImage()
    val pixels = image.toPixelMap()
    val x = image.width / 4
    val materialBottom = bar.bottom + if (materialPaintsExtraHeight) ExtraHeightPx else 0f
    val topY = bar.top.roundToInt() + 2
    val middleY = ((bar.top + materialBottom) / 2f).roundToInt()
    val bottomY = materialBottom.roundToInt() - 3
    val top = pixels[x, topY]
    val middle = pixels[x, middleY]
    val bottom = pixels[x, bottomY]
    println(
        "[glass] bar=$bar materialBottom=$materialBottom x=$x top[$topY]=${top.argbHex()} " +
            "middle[$middleY]=${middle.argbHex()} bottom[$bottomY]=${bottom.argbHex()}",
    )
    assertTrue(
        bar.top <= topY && middleY < bottomY && bottomY < materialBottom.roundToInt() && x <= bar.right,
        "the sample points must sit inside the material, bar=$bar x=$x y=$topY/$middleY/$bottomY",
    )
    return Triple(top, middle, bottom)
}

private fun redChannel(color: Color): Int = color.toArgb() shr 16 and 0xFF

private fun blueChannel(color: Color): Int = color.toArgb() and 0xFF

/** The app bar's bounds in the captured image. */
@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.appBarBounds(): Rect = onNodeWithTag(APP_BAR_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

/** The bounding box of the pixels inside [bar] that satisfy [predicate], in image coordinates. */
@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.capturePixelBounds(
    bar: Rect,
    predicate: (Color) -> Boolean,
): PixelBounds {
    val pixels = onRoot().captureToImage().toPixelMap()
    var left = Int.MAX_VALUE
    var top = Int.MAX_VALUE
    var right = Int.MIN_VALUE
    var bottom = Int.MIN_VALUE
    for (y in bar.top.roundToInt() until bar.bottom.roundToInt()) {
        for (x in bar.left.roundToInt() until bar.right.roundToInt()) {
            if (predicate(pixels[x, y])) {
                left = minOf(left, x)
                top = minOf(top, y)
                right = maxOf(right, x)
                bottom = maxOf(bottom, y)
            }
        }
    }
    val bounds = PixelBounds(left, top, right, bottom)
    println("[glass] bounds(${bounds.width}x${bounds.height}) at $bounds inside bar=$bar")
    assertTrue(bounds.width > 0, "no pixel matched inside the bar, bar=$bar")
    return bounds
}

/** True for the dark title glyph on the white surface these scenes paint. */
private fun isDark(color: Color): Boolean = redChannel(color) < 128

/** True for the cyan fill the segment scene hands to the slot. */
private fun isSegment(color: Color): Boolean {
    val argb = color.toArgb()
    return (argb shr 16 and 0xFF) < 64 && (argb shr 8 and 0xFF) > 200 && (argb and 0xFF) > 200
}

private data class PixelBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left + 1
    val height: Int get() = bottom - top + 1
    val centerX: Float get() = (left + right) / 2f
}

/** Largest per-channel distance between two pixels, alpha excluded. */
private fun maxChannelDelta(a: Color, b: Color): Int {
    val aArgb = a.toArgb()
    val bArgb = b.toArgb()
    return maxOf(
        abs((aArgb shr 16 and 0xFF) - (bArgb shr 16 and 0xFF)),
        abs((aArgb shr 8 and 0xFF) - (bArgb shr 8 and 0xFF)),
        abs((aArgb and 0xFF) - (bArgb and 0xFF)),
    )
}

private fun channelDistanceToWhite(color: Color): Int {
    val argb = color.toArgb()
    return maxOf(
        255 - (argb shr 16 and 0xFF),
        255 - (argb shr 8 and 0xFF),
        255 - (argb and 0xFF),
    )
}
