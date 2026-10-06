// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * A text that scrolls horizontally when it overflows the space it is given, following ColorOS's
 * marquee-text design.
 *
 * The native view measures its text in `onMeasure`; when the text is wider than the view it appends
 * `coui_top_tips_scroll_text_interval` (80dp) of trailing space, repeats the string enough times to
 * fill the view, and then walks `mCurrentScrollLocation` left by `coui_top_tips_scroll_speed` (25dp)
 * per second on an endless `ValueAnimator` with a linear interpolator. Scrolling starts
 * `DEFAULT_SCROLL_DELAY_DURATION` (1000ms) after the view is laid out, begins
 * `coui_top_tips_scroll_text_start_location` (10dp) inside the left edge, and the two clipped edges
 * fade over `coui_top_tips_fading_edge_size` (26dp) because the view enables horizontal fading
 * edges. Text that fits is drawn normally and never scrolls.
 *
 * The port renders two copies of the text separated by [MarqueeTextDefaults.TextInterval] and
 * translates them, which produces the same seamless loop as the native string repetition. The
 * whole loop is one period of the animation: it advances by the text width plus the interval, which
 * is exactly the distance the native inserts between two copies of the string.
 *
 * Not ported, because no consumer in this library reaches them:
 * - `stopRoll()` / `continueRoll()` as an imperative pair; [marqueeEnabled] toggles the same state,
 *   and setting it back to `false` stops the scroll and returns the text to its start.
 * - the accessibility suppression of `mSuppressAccessibilityEvents` /
 *   `onInitializeAccessibilityEvent` and the `onInitializeAccessibilityNodeInfo` override that
 *   reports `mOriginText` instead of the repeated string; the port always reports the original
 *   [text] because it never replaces the laid-out string.
 * - `checkAllCharactersDirections` and the `isAllCharactersLtR` mirroring of the scroll direction;
 *   the port leaves direction handling to Compose's `LayoutDirection`.
 *
 * @param text The text to display.
 * @param modifier The modifier to be applied to the text.
 * @param color The color of the text. [Color.Unspecified] keeps the style's colour.
 * @param style The text style. `COUIMarqueeTextView` declares no text appearance of its own — it is
 *   an `AppCompatTextView` whose style comes from the app — so the port defaults to the theme's
 *   `COUITheme.textStyles.main`, the same default as [Text].
 * @param marqueeEnabled Whether the text may scroll. Mirrors `COUIMarqueeTextView.setMarqueeEnable`,
 *   which the native keeps `false` until the app enables it.
 * @param speed The scroll speed. COUI `coui_top_tips_scroll_speed`
 *   ([MarqueeTextDefaults.ScrollSpeed]).
 * @param startLocation The distance the text starts inside the left edge. COUI
 *   `coui_top_tips_scroll_text_start_location` ([MarqueeTextDefaults.ScrollStartLocation]).
 * @param textInterval The gap between the end of one pass and the start of the next. COUI
 *   `coui_top_tips_scroll_text_interval` ([MarqueeTextDefaults.TextInterval]).
 * @param fadingEdgeWidth The width of the fade on each clipped edge. COUI
 *   `coui_top_tips_fading_edge_size` ([MarqueeTextDefaults.FadingEdgeWidth]).
 * @param scrollDelayMillis The delay before the scroll starts once the text is laid out. COUI
 *   `COUIMarqueeTextView.DEFAULT_SCROLL_DELAY_DURATION` (1000ms).
 */
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MarqueeTextDefaults.textStyle(),
    marqueeEnabled: Boolean = true,
    speed: Dp = MarqueeTextDefaults.ScrollSpeed,
    startLocation: Dp = MarqueeTextDefaults.ScrollStartLocation,
    textInterval: Dp = MarqueeTextDefaults.TextInterval,
    fadingEdgeWidth: Dp = MarqueeTextDefaults.FadingEdgeWidth,
    scrollDelayMillis: Long = MarqueeTextDefaults.ScrollDelayMillis,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val textWidthPx = remember(text, style) {
            textMeasurer.measure(AnnotatedString(text), style = style, maxLines = 1).size.width
        }
        val availableWidthPx = with(density) { maxWidth.toPx() }
        // COUIMarqueeTextView.onMeasure: scrolling only happens when the text is wider than the view.
        val overflows = marqueeEnabled && textWidthPx > availableWidthPx

        if (!overflows) {
            Text(
                text = text,
                color = color,
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            return@BoxWithConstraints
        }

        val startPx = with(density) { startLocation.toPx() }
        val intervalPx = with(density) { textInterval.toPx() }
        val distancePx = textWidthPx + intervalPx
        val speedPxPerSecond = with(density) { speed.toPx() }
        val durationMillis = ((distancePx / speedPxPerSecond) * 1000f).roundToInt().coerceAtLeast(1)
        val fadeWidthPx = with(density) { fadingEdgeWidth.toPx() }

        val progress = remember { Animatable(0f) }
        LaunchedEffect(durationMillis) {
            progress.snapTo(0f)
            delay(scrollDelayMillis)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = durationMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            )
        }

        Box(
            modifier = Modifier
                // The fading edges are a DST_OUT mask, which can only erase what it is composited
                // against, so the marquee gets an offscreen layer of its own.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .clipToBounds()
                .drawWithContent {
                    drawContent()
                    val offset = startPx - progress.value * distancePx
                    val fade = fadeWidthPx.coerceAtMost(size.width / 2f)
                    if (fade <= 0f) return@drawWithContent
                    if (offset < 0f) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.White, Color.Transparent),
                                startX = 0f,
                                endX = fade,
                            ),
                            size = Size(fade, size.height),
                            blendMode = BlendMode.DstOut,
                        )
                    }
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.White),
                            startX = size.width - fade,
                            endX = size.width,
                        ),
                        topLeft = Offset(size.width - fade, 0f),
                        size = Size(fade, size.height),
                        blendMode = BlendMode.DstOut,
                    )
                },
        ) {
            Row(
                modifier = Modifier
                    .wrapContentWidth(unbounded = true)
                    .offset { IntOffset((startPx - progress.value * distancePx).roundToInt(), 0) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MarqueeLine(text = text, color = color, style = style)
                Spacer(modifier = Modifier.width(textInterval))
                MarqueeLine(text = text, color = color, style = style)
            }
        }
    }
}

/** One copy of the scrolling line: a single, non-wrapping line of the marquee's text. */
@Composable
private fun MarqueeLine(
    text: String,
    color: Color,
    style: TextStyle,
) {
    Text(
        text = text,
        color = color,
        style = style,
        maxLines = 1,
        softWrap = false,
    )
}

/** Contains default values used by [MarqueeText]. */
object MarqueeTextDefaults {

    /**
     * The scroll speed. COUI `coui_top_tips_scroll_speed` (25dp); `COUIMarqueeTextView.initSpeed`
     * divides it by `Display.getRefreshRate()` to get the per-frame step, which is 25dp per second.
     */
    val ScrollSpeed = 25.dp

    /**
     * The distance the text starts inside the left edge. COUI
     * `coui_top_tips_scroll_text_start_location` (10dp), the initial and reset value of
     * `mCurrentScrollLocation`.
     */
    val ScrollStartLocation = 10.dp

    /**
     * The gap appended after one pass of the text. COUI `coui_top_tips_scroll_text_interval`
     * (80dp), `COUIMarqueeTextView.mTextViewScrollDistance`.
     */
    val TextInterval = 80.dp

    /**
     * The width of the fading edge. COUI `coui_top_tips_fading_edge_size` (26dp), the value
     * `setFadingEdgeLength` is given.
     */
    val FadingEdgeWidth = 26.dp

    /**
     * The delay before the scroll starts. COUI
     * `COUIMarqueeTextView.DEFAULT_SCROLL_DELAY_DURATION` (1000ms).
     */
    val ScrollDelayMillis = 1000L

    /**
     * The default text style. `COUIMarqueeTextView` is an `AppCompatTextView` and declares no text
     * appearance, so the port uses the theme's `COUITheme.textStyles.main` like [Text].
     */
    @Composable
    fun textStyle(): TextStyle = COUITheme.textStyles.main
}
