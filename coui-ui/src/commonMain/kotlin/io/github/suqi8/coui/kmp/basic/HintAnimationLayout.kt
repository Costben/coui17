// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The rotation of search-bar hint strings, mirroring ColorOS's
 * `com.coui.appcompat.searchview.COUIHintAnimationLayout`.
 *
 * The native view is a `FrameLayout` that keeps two hint `TextView`s (`mHintTextViewFirst` /
 * `mHintTextViewThen`) and swaps between them: every `INTERVAL_TIME` it advances the hint index by
 * one modulo the hint list and `applyHintChangeAnimation` plays two `AnimatorSet`s of
 * [HintAnimationDefaults.DurationMillis] (250ms). The outgoing text translates by `-distance` while
 * fading out, the incoming text enters from `+distance` while fading in, and the incoming pair
 * starts `IN_DELAY_TIME` (150ms) after the outgoing one. `distance` is
 * `(getMeasuredHeight() - currentHintTextView.getLineHeight()) / 2` plus
 * `mCouiSearchBarAnimationTranslateExtra`, which walks each text just past the clipped edge. The
 * two sets use different interpolators: `TRANSLATEINTERPOLATOR`, a
 * `PathInterpolatorCompat.create(0.3f, 0f, 0.2f, 1f)`, drives the translation, while
 * `EASEINTERPOLATOR`, a `COUIEaseInterpolator` (`PathInterpolator(0.33f, 0f, 0.67f, 1f)`), drives
 * the alpha. When an `AnimatorSet` already exists the native only re-targets its animations, which
 * the port reproduces by reusing one pair of sliders per slot.
 *
 * `COUIHintAnimationLayout` attaches a `TextWatcher` to the search `EditText` through
 * `setSearchEditText` — or by taking child 0, as `coui_search_view_animated_support_layout.xml`
 * relies on. A non-empty query removes the pending rotation, hides both hint views, cancels the
 * animation and resets their translation and alpha; an empty query puts the last string back.
 * [query] carries that text into the port, because Compose's text field owns its own value.
 *
 * The hints are `couiTextAppearanceBodyL` (16sp, `coui_spacing_multiplier_body_l`) `TextView`s
 * inside a `Widget_COUI_EditText_SearchViewStyle_HintText` theme wrapper, both marked
 * `importantForAccessibility = 2`. The port centres the hint on the same line as the field content
 * instead of reproducing the native's own child box; only the travel distance above is taken from
 * the native.
 *
 * Not ported, because the corresponding state has no Compose equivalent:
 * - `setHintsAnimation` refusing to run when the search field already holds text, logging through
 *   `TAG`, and calling `mSearchEditText.setHint("")`; the port stops rotating while [query] is not
 *   empty and leaves the field's own placeholder to the caller.
 * - `setSearchEditText` / the "child 0 must be an EditText" fallback and the two `Log.e` guards
 *   that enforce them; the search field is supplied by the caller through [content].
 * - `setHintTextColor(ColorStateList)`; [textColor] carries a single [Color], and
 *   `Color.Unspecified` keeps the style's own colour.
 * - `setTextSize(int)` copying the `EditText`'s pixel text size; [textSize] is a `TextUnit`
 *   override.
 * - `onAttachedToWindow` / `onDetachedFromWindow` / `onWindowVisibilityChanged` pausing and
 *   resuming the rotation, and `pauseHintsAnimation` / `resumeHintsAnimation` as an imperative
 *   pair; the rotation is tied to the composition and to [enabled] and [query] instead.
 * - `getCurrentHintTextView()` and the `TextView` argument of `COUIHintAnimationChangeListener`;
 *   [onHintAnimationChange] reports the index and the string only.
 * - The `id/coui_hint_text_view_first` / `id/coui_hint_text_view_then` view ids, which the native
 *   assigns to its own children.
 *
 * @param hints The strings to rotate through. An empty list, or a list of empty strings, stops the
 *   rotation. Mirrors `COUIHintAnimationLayout.setHintsAnimation`, which rejects an empty list.
 * @param modifier The modifier to be applied to the layout.
 * @param query The current text of the search field. A non-empty value hides the hints and stops
 *   the rotation, matching the native `TextWatcher.afterTextChanged`.
 * @param enabled Whether the rotation runs. Mirrors `COUIHintAnimationLayout.setEnabled`, which the
 *   native forwards to the two hint `TextView`s.
 * @param repeatCount How many transitions to play before stopping.
 *   `COUIHintAnimationLayout.mRepeatCount` ([HintAnimationDefaults.RepeatCount], -1) rotates until
 *   the composition changes.
 * @param textSize The size of the hint text. `TextUnit.Unspecified` keeps the size of [style].
 * @param textColor The colour of the hint text. `Color.Unspecified` keeps the style's colour.
 * @param style The text style of the hints. Defaults to `COUITheme.textStyles.body1`, the 16sp
 *   `couiTextAppearanceBodyL` tier.
 * @param onHintAnimationChange Called when a transition starts, with the new index and the incoming
 *   string. Mirrors `COUIHintAnimationChangeListener.hintAnimationChange`, which fires from
 *   `applyHintChangeAnimation` once the outgoing animation has been started.
 * @param content The search field the hints are drawn over. The native adds the `EditText` as the
 *   first child of the `FrameLayout` and the two hint views on top of it.
 */
@Composable
fun HintAnimationLayout(
    hints: List<String>,
    modifier: Modifier = Modifier,
    query: String = "",
    enabled: Boolean = true,
    repeatCount: Int = HintAnimationDefaults.RepeatCount,
    textSize: TextUnit = TextUnit.Unspecified,
    textColor: Color = Color.Unspecified,
    style: TextStyle = HintAnimationDefaults.textStyle(),
    onHintAnimationChange: (index: Int, hint: String) -> Unit = { _, _ -> },
    content: @Composable BoxScope.() -> Unit = {},
) {
    val availableHints = remember(hints) { hints.filter { it.isNotEmpty() } }
    val rotating = enabled && query.isEmpty() && availableHints.isNotEmpty()

    val shifts = remember { listOf(Animatable(0f), Animatable(0f)) }
    val alphas = remember { listOf(Animatable(1f), Animatable(0f)) }
    val slotTexts = remember { mutableStateListOf("", "") }
    var frontSlot by remember { mutableIntStateOf(0) }
    var containerHeight by remember { mutableIntStateOf(0) }

    val effectiveStyle = if (textSize != TextUnit.Unspecified) {
        style.copy(fontSize = textSize)
    } else {
        style
    }
    val textMeasurer = rememberTextMeasurer()
    val lineHeightPx = remember(textMeasurer, effectiveStyle) {
        textMeasurer.measure(AnnotatedString("M"), style = effectiveStyle).size.height
    }
    val extraPx = with(LocalDensity.current) { HintAnimationDefaults.TranslateExtra.toPx() }
    val notifyHintChanged by rememberUpdatedState(onHintAnimationChange)

    LaunchedEffect(availableHints, enabled, query, repeatCount) {
        slotTexts[0] = availableHints.firstOrNull().orEmpty()
        slotTexts[1] = ""
        shifts[0].snapTo(0f)
        shifts[1].snapTo(0f)
        alphas[0].snapTo(1f)
        alphas[1].snapTo(0f)
        frontSlot = 0
        if (!rotating) return@LaunchedEffect

        val translateSpec = tween<Float>(
            durationMillis = HintAnimationDefaults.DurationMillis,
            easing = TRANSLATE_EASING,
        )
        val fadeSpec = tween<Float>(
            durationMillis = HintAnimationDefaults.DurationMillis,
            easing = EASE_EASING,
        )
        var index = 0
        var played = 0
        while (true) {
            delay(HintAnimationDefaults.IntervalMillis)
            if (repeatCount != HintAnimationDefaults.RepeatCount && played >= repeatCount) break
            played += 1
            index = (index + 1) % availableHints.size
            val next = availableHints[index]
            val outgoing = frontSlot
            val incoming = 1 - outgoing
            val distance = ((containerHeight - lineHeightPx) / 2f) + extraPx
            slotTexts[incoming] = next
            shifts[incoming].snapTo(distance)
            alphas[incoming].snapTo(0f)
            notifyHintChanged(index, next)
            val enter = launch {
                delay(HintAnimationDefaults.InDelayMillis)
                launch { shifts[incoming].animateTo(0f, translateSpec) }
                alphas[incoming].animateTo(1f, fadeSpec)
            }
            launch { shifts[outgoing].animateTo(-distance, translateSpec) }
            launch { alphas[outgoing].animateTo(0f, fadeSpec) }
            enter.join()
            frontSlot = incoming
        }
    }

    Box(modifier = modifier.onSizeChanged { containerHeight = it.height }) {
        content()
        if (rotating) {
            shifts.indices.forEach { slot ->
                Text(
                    text = slotTexts[slot],
                    color = textColor,
                    style = effectiveStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .graphicsLayer {
                            translationY = shifts[slot].value
                            alpha = alphas[slot].value
                        },
                )
            }
        }
    }
}

/** Contains default values used by [HintAnimationLayout]. */
object HintAnimationDefaults {

    /** How long one cross-fade / slide takes. `COUIHintAnimationLayout.DURATION_TIME` (250ms). */
    val DurationMillis = 250

    /**
     * The head start of the incoming text. `COUIHintAnimationLayout.IN_DELAY_TIME` (150ms), the
     * delay `applyHintChangeAnimation` posts `mAnimatorSetIn.start()` with.
     */
    val InDelayMillis = 150L

    /**
     * The pause between two transitions. `COUIHintAnimationLayout.INTERVAL_TIME`, read from
     * `RippleShader.DEFAULT_RIPPLE_EFFECT_DURATION` (3000ms), which `mChangeHintRunnable` re-posts
     * itself with.
     */
    val IntervalMillis = 3000L

    /**
     * Rotate forever. `COUIHintAnimationLayout.mRepeatCount` starts at -1, and `setRepeatCount`
     * rejects anything that is not greater than zero.
     */
    val RepeatCount = -1

    /**
     * How far past the clipped edge each text travels on top of half the leftover height. COUI
     * `coui_search_bar_animation_translate_extra` (5dp), `mCouiSearchBarAnimationTranslateExtra`.
     */
    val TranslateExtra = 5.dp

    /**
     * The hint text style. `COUIHintAnimationLayout` themes both hint views with
     * `couiTextAppearanceBodyL` (16sp, `coui_spacing_multiplier_body_l`) inside a
     * `Widget_COUI_EditText_SearchViewStyle_HintText` wrapper; the port uses the theme's `body1`,
     * the 16sp `couiTextAppearanceBodyL` tier.
     */
    @Composable
    fun textStyle(): TextStyle = COUITheme.textStyles.body1
}

/**
 * `COUIHintAnimationLayout.TRANSLATEINTERPOLATOR`:
 * `PathInterpolatorCompat.create(0.3f, 0.0f, 0.2f, 1.0f)`.
 */
private val TRANSLATE_EASING = CubicBezierEasing(0.3f, 0.0f, 0.2f, 1.0f)

/** `COUIEaseInterpolator`: `PathInterpolator(0.33f, 0.0f, 0.67f, 1.0f)`. */
private val EASE_EASING = CubicBezierEasing(0.33f, 0.0f, 0.67f, 1.0f)
