// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.blur.LocalBlurEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The page behind the bar, so anything the merged silhouette does not cover reads green. */
private val Background = Color(0xFF00FF00)

/** The capsule body, far from both the background and the rim. */
private val Body = Color(0xFF0000FF)

private val BarWidth = 300.dp
private val CapsuleHeight = 40.dp
private val CancelWidth = 60.dp
private val CancelGap = 12.dp
private val CancelSlide = 20.dp

/** `300 - 60 - 12`: what `measureBackground` shrinks the capsule to in the edit state. */
private val RestingCapsuleWidth = BarWidth - CancelWidth - CancelGap

private val Geometry = SearchBarMetaBallGeometry(
    barWidth = BarWidth,
    barHeight = CapsuleHeight,
    restingCapsuleWidth = RestingCapsuleWidth,
    cancelWidth = CancelWidth,
    cancelHeight = CapsuleHeight,
    cancelGap = CancelGap,
    cancelSlide = CancelSlide,
)

private val TestColors = SearchBarColors(
    backgroundColor = Body,
    labelColor = Color.White,
    iconColor = Color.White,
    clearIconColor = Color.White,
)

/**
 * The cancel capsule is not laid out beside the field: `COUISearchBar` chains the two views into one
 * melt ball and runs three tracks off a single spring, so the shape of the transition is arithmetic
 * that can be pinned without rendering anything.
 */
@OptIn(ExperimentalTestApi::class)
class SearchBarMetaBallGeometryTest {
    @Test
    fun theCapsuleShrinksByTheButtonSlotOverTheEditProgress() {
        assertEquals(BarWidth, searchBarCapsuleWidth(BarWidth, RestingCapsuleWidth, 0f))
        assertEquals(RestingCapsuleWidth, searchBarCapsuleWidth(BarWidth, RestingCapsuleWidth, 1f))
        assertEquals(
            (BarWidth.value + RestingCapsuleWidth.value) / 2f,
            searchBarCapsuleWidth(BarWidth, RestingCapsuleWidth, 0.5f).value,
            1e-4f,
        )
    }

    @Test
    fun theCancelCapsuleGrowsFromHalfScale() {
        assertEquals(SearchBarDefaults.CancelInitialScale, searchBarCancelScale(0f))
        assertEquals(1f, searchBarCancelScale(1f))
        assertEquals(0.75f, searchBarCancelScale(0.5f))
    }

    @Test
    fun theCancelCapsuleSlidesBackToItsRestingPlace() {
        assertEquals(CancelSlide, searchBarCancelTranslationX(CancelSlide, 0f))
        assertEquals(0.dp, searchBarCancelTranslationX(CancelSlide, 1f))
    }

    @Test
    fun theCollapsedBarIsOneCapsule() {
        val particles = Geometry.particles(0f)
        assertEquals(BarWidth, particles[0].width)
        // The button already overlaps the capsule, so the union is a single blob from the first
        // frame; `mFunctionalButtonOffsetX` keeps it inside on the reference 3x density.
        val button = particles[1]
        val buttonLeft = button.centerX - button.width * button.scaleX / 2
        val capsuleRight = particles[0].centerX + particles[0].width / 2
        assertTrue(buttonLeft < capsuleRight, "the collapsed button must overlap the capsule, was $buttonLeft vs $capsuleRight")
        assertEquals(0f, button.alpha)
    }

    /**
     * At rest the capsules are one gap apart and the fold radius is
     * `extension * factorX = 4dp * 1.5 = 6dp` per side, so they meet without overlapping: the gap
     * equals the native `coui_search_bar_functional_button_start_gap`.
     */
    @Test
    fun theRestingCapsulesAreOneStartGapApart() {
        val particles = Geometry.particles(1f)
        val capsule = particles[0]
        val cancel = particles[1]
        val capsuleRight = capsule.centerX + capsule.width / 2
        val cancelLeft = cancel.centerX - cancel.width * cancel.scaleX / 2
        assertEquals(CancelGap.value, (cancelLeft - capsuleRight).value, 1e-3f)
        assertEquals(1f, cancel.scaleX)
        assertEquals(1f, cancel.alpha)
    }

    @Test
    fun rtlMirrorsBothSilhouettes() {
        val ltr = Geometry.particles(0.5f, rtl = false)
        val rtl = Geometry.particles(0.5f, rtl = true)
        ltr.zip(rtl).forEach { (left, right) ->
            assertEquals(BarWidth.value - left.centerX.value, right.centerX.value, 1e-4f)
            assertEquals(left.width, right.width)
        }
    }

    /** `setBallChain` only builds the chain while the material's blur is on. */
    @Test
    fun fusionFollowsTheBlurSwitch() = runSkikoComposeUiTest(size = Size(100f, 100f)) {
        var enabled: Boolean? = null
        setContent {
            CompositionLocalProvider(LocalBlurEnabled provides false) {
                enabled = SearchBarDefaults.metaBallFusion()
            }
        }
        waitForIdle()
        assertFalse(requireNotNull(enabled), "a bar without blur must fall back to two capsules")
    }
}

/**
 * The merged outline wears the search bar's own material, `TYPE_FRAMEWORK_CAPSULE_8`, rather than
 * the bottom bar's `TYPE_FRAMEWORK_CAPSULE_3` rim it inherited while the two shared a hardcoded
 * width. `COUIMaterialStrokeEffect.java:59` / `:96` against `:32` / `:70`: 1.8dp against 2.5dp.
 */
@OptIn(ExperimentalTestApi::class)
class SearchBarMetaBallStrokeTest {

    /** `FRAMEWORK_CAPSULE_PARAMS_8_LIGHT` / `_DARK`: alpha 0.8 / 0.15, width 1.8dp, angle 0. */
    @Test
    fun theMergedOutlineUsesTheCapsule8Edge() {
        val light = searchBarMetaBallRim(MaterialStrokeParams.FrameworkCapsule8Light)
        assertEquals(1.8.dp, light.lineWidth)
        assertEquals(Color.White.copy(alpha = 0.8f), light.color)
        assertEquals(0f, light.lineAngle)

        val dark = searchBarMetaBallRim(MaterialStrokeParams.FrameworkCapsule8Dark)
        assertEquals(1.8.dp, dark.lineWidth)
        assertEquals(Color.White.copy(alpha = 0.15f), dark.color)
        assertEquals(0f, dark.lineAngle)
    }

    /** `SearchBarDefaults.searchBarStroke` is what the bar hands over, so it must resolve to it too. */
    @Test
    fun theBarStrokeResolvesToTheCapsule8EdgeInTheLightTheme() = runSkikoComposeUiTest(size = Size(320f, 80f)) {
        var rim: SearchBarMetaBallRim? = null
        setContent {
            COUITheme(colors = lightColorScheme()) {
                rim = searchBarMetaBallRim(SearchBarDefaults.searchBarStroke())
            }
        }
        waitForIdle()
        val resolved = requireNotNull(rim)
        assertEquals(1.8.dp, resolved.lineWidth)
        assertEquals(Color.White.copy(alpha = 0.8f), resolved.color)
    }

    @Test
    fun theBarStrokeResolvesToTheCapsule8EdgeInTheDarkTheme() = runSkikoComposeUiTest(size = Size(320f, 80f)) {
        var rim: SearchBarMetaBallRim? = null
        setContent {
            COUITheme(colors = darkColorScheme()) {
                rim = searchBarMetaBallRim(SearchBarDefaults.searchBarStroke())
            }
        }
        waitForIdle()
        val resolved = requireNotNull(rim)
        assertEquals(1.8.dp, resolved.lineWidth)
        assertEquals(Color.White.copy(alpha = 0.15f), resolved.color)
    }

    /** The bottom bar's merged outline is untouched: `FRAMEWORK_CAPSULE_PARAMS_3_*`'s 2.5dp rim. */
    @Test
    fun theBottomBarKeepsTheCapsule3Edge() {
        assertEquals(2.5.dp, META_BALL_RIM_LINE_WIDTH)
        assertEquals(0.25f, BottomFloatingNavigationBarDefaults.RIM_ALPHA_LIGHT)
        assertEquals(0.8f, BottomFloatingNavigationBarDefaults.RIM_ALPHA_DARK)
    }

    @Test
    fun theSearchRimIsNarrowerThanTheBottomBarRim() {
        val search = searchBarMetaBallRim(MaterialStrokeParams.FrameworkCapsule8Light).lineWidth
        assertTrue(search < META_BALL_RIM_LINE_WIDTH, "the search outline must not inherit the bottom bar's $META_BALL_RIM_LINE_WIDTH")
    }

    /** A caller that paints the outline itself still has to leave a valid width behind. */
    @Test
    fun aBarWithoutItsOwnStrokeFallsBackToTheBottomBarWidth() {
        val rim = searchBarMetaBallRim(null)
        assertNull(rim.color)
        assertEquals(META_BALL_RIM_LINE_WIDTH, rim.lineWidth)
        assertEquals(0f, rim.lineAngle)
    }
}

/** The rim width has to reach the shader, not only the resolved parameters. */
@OptIn(ExperimentalTestApi::class)
class MetaBallRimWidthRenderTest {

    @Test
    fun aWiderRimLightsMoreOfTheOutline() {
        val narrow = rimEnergy(1.8.dp)
        val wide = rimEnergy(2.5.dp)
        assertTrue(narrow > 0f, "the runtime shader drew no rim at all")
        assertTrue(wide > narrow * 1.1f, "2.5dp rim lit $wide, 1.8dp lit $narrow: the width never reached the shader")
    }

    /**
     * Total red of a capsule stroked in white over black, at the reference 3.875 density where the
     * two presets' near / far line pairs differ (both clamp to the 2px floor at 1x).
     */
    private fun rimEnergy(lineWidth: Dp): Float {
        var energy = 0f
        runSkikoComposeUiTest(size = Size(220f, 60f), density = Density(3.875f)) {
            setContent {
                MetaBallSurfaceWithRim(
                    particles = listOf(MetaBallParticle(centerX = 110.dp, centerY = 30.dp, width = 180.dp, height = 40.dp)),
                    blurColors = null,
                    rimColor = Color.White,
                    rimLineWidth = lineWidth,
                    rimLineAngle = 0f,
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black,
                    backdrop = null,
                    rimBottomScale = 1f,
                )
            }
            waitForIdle()
            val pixels = onRoot().captureToImage().toPixelMap()
            for (y in 0 until pixels.height) {
                for (x in 0 until pixels.width) {
                    energy += pixels[x, y].red
                }
            }
        }
        return energy
    }
}

@OptIn(ExperimentalTestApi::class)
class SearchBarMetaBallRenderTest {

    /**
     * Halfway through the transition the capsules overlap, so the fold bridges them: the midpoint of
     * the gap has to read as the capsule body.
     */
    @Test
    fun theCapsulesMergeWhileTheButtonIsTravellingIn() = runSkikoComposeUiTest(
        size = Size(BarWidth.value, CapsuleHeight.value),
        density = Density(1f),
    ) {
        setContent { MergedScene(0.5f) }
        waitForIdle()
        val pixel = sampleGapMidpoint(0.5f)
        assertTrue(blueChannel(pixel) >= 200, "the gap must be bridged mid-transition: $pixel")
        assertTrue(greenChannel(pixel) <= 40, "the gap must not show the page: $pixel")
    }

    /**
     * The smooth minimum pulls the union's surface out by `k / 4 = 1.5dp` at the midpoint of a gap,
     * so the bridge only reaches gaps below `k / 2 = 3dp`. At 80% the gap is 4.6dp and the page has
     * to show through.
     */
    @Test
    fun theFoldOnlyBridgesGapsBelowThreeDp() = runSkikoComposeUiTest(
        size = Size(BarWidth.value, CapsuleHeight.value),
        density = Density(1f),
    ) {
        setContent { MergedScene(0.8f) }
        waitForIdle()
        val pixel = sampleGapMidpoint(0.8f)
        assertTrue(greenChannel(pixel) >= 200, "a 4.6dp gap is past the 3dp merge threshold: $pixel")
        assertTrue(blueChannel(pixel) <= 40, "the gap must not read as the capsule body: $pixel")
    }

    /** The whole point of the component: one silhouette collapsed, two capsules once expanded. */
    @Test
    fun aCollapsedBarIsOneCapsule() = runSkikoComposeUiTest(
        size = Size(BarWidth.value, CapsuleHeight.value),
        density = Density(1f),
    ) {
        setContent { SearchBarScene(expanded = false) }
        waitForIdle()
        assertEquals(1, bodyRuns(sampleRow()), "a collapsed bar is a single capsule")
    }

    @Test
    fun anExpandedBarIsTwoCapsules() = runSkikoComposeUiTest(
        size = Size(BarWidth.value, CapsuleHeight.value),
        density = Density(1f),
    ) {
        setContent { SearchBarScene(expanded = true) }
        waitForIdle()
        assertEquals(2, bodyRuns(sampleRow()), "an expanded bar is the field plus the cancel capsule")
    }

    /** The fused bar must still render its two capsules when the melt ball is switched off. */
    @Test
    fun theUnfusedBarKeepsTwoCapsules() = runSkikoComposeUiTest(
        size = Size(BarWidth.value, CapsuleHeight.value),
        density = Density(1f),
    ) {
        setContent { SearchBarScene(expanded = true, metaBallFusion = false) }
        waitForIdle()
        assertEquals(2, bodyRuns(sampleRow()))
    }

    @Composable
    private fun MergedScene(progress: Float) {
        Box(Modifier.fillMaxSize().background(Background)) {
            MetaBallSurface(
                particles = Geometry.particles(progress),
                modifier = Modifier.fillMaxSize(),
                color = Body,
                backdrop = null,
                rimColor = null,
            )
        }
    }

    @Composable
    private fun SearchBarScene(expanded: Boolean, metaBallFusion: Boolean = true) {
        COUITheme {
            Box(Modifier.fillMaxSize().background(Background)) {
                SearchBar(
                    query = "",
                    onQueryChange = {},
                    onSearch = {},
                    onExpandedChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    expanded = expanded,
                    colors = TestColors,
                    // No rim and no caustic: the sampled row has to be the plain capsule body.
                    stroke = null,
                    glowColor = null,
                    insideMargin = DpSize(0.dp, 0.dp),
                    cancelText = "Cancel",
                    metaBallFusion = metaBallFusion,
                )
            }
        }
    }

    private fun SkikoComposeUiTest.sampleGapMidpoint(progress: Float): Color {
        val x = with(density) { gapMidpointX(progress).roundToPx() }
        val y = with(density) { (CapsuleHeight / 2).roundToPx() }
        return onRoot().captureToImage().toPixelMap()[x, y]
    }

    private fun SkikoComposeUiTest.sampleRow(): PixelMap = onRoot().captureToImage().toPixelMap()
}

/**
 * Number of contiguous horizontal runs of [Body] along the row 8px below the capsules' top edge —
 * above the label glyph and the leading icon, so only the silhouettes themselves are counted.
 */
private fun bodyRuns(pixels: PixelMap): Int {
    val y = 8
    var runs = 0
    var inside = false
    for (x in 0 until pixels.width) {
        val pixel = pixels[x, y]
        val isBody = blueChannel(pixel) >= 200 && greenChannel(pixel) <= 40
        if (isBody && !inside) runs++
        inside = isBody
    }
    return runs
}

/** Midpoint of the horizontal gap between the two silhouettes at [progress]. */
private fun gapMidpointX(progress: Float): androidx.compose.ui.unit.Dp {
    val particles = Geometry.particles(progress)
    val capsuleRight = particles[0].centerX + particles[0].width / 2
    val cancelLeft = particles[1].centerX - particles[1].width * particles[1].scaleX / 2
    return (capsuleRight + cancelLeft) / 2
}

private fun greenChannel(color: Color): Int = color.toArgb() shr 8 and 0xFF

private fun blueChannel(color: Color): Int = color.toArgb() and 0xFF
