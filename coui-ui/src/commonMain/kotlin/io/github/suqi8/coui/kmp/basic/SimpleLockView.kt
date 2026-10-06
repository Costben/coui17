// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/**
 * The ColorOS 17 "simple lock" PIN indicator.
 *
 * It is the row of rounded squares that fills in as the user types a PIN: the style's circle-count
 * attribute (0 -> [SimpleLockType.Four], 1 -> [SimpleLockType.Six]) decides how many squares the row
 * holds. `couiRectanglePadding`
 * ([SimpleLockViewDefaults.RectanglePadding], 10dp) is the gap and
 * `couiFilledRectangleIconDrawable` / `couiOutLinedRectangleIconDrawable` are the two states, both a
 * `<shape>` of 10dp x 10dp with a 2dp `<corners>` radius — the intrinsic size the constructor reads
 * into `mDrawableWidth` / `mDrawableHeight`. The row is
 * `dotCount * 10dp + (dotCount - 1) * 10dp` wide (70dp for four, 110dp for six) and
 * `onMeasure` centres it with `mCodeImageStart = (width - mRectanglesWidth) / 2`.
 *
 * [count] is `mCodeNumber + 1`, the number of filled squares, and every draw path of the class is
 * derived from it:
 * - [count] grows: the squares that were empty fade their fill in over
 *   [SimpleLockViewDefaults.MorphDurationMillis] (`createMorphingAnimationOutLinedToFilled`,
 *   `ValueAnimator.ofInt(0, 255)` with no interpolator set, i.e. the framework's accelerate-decelerate
 *   curve);
 * - [count] shrinks: the squares that became empty fade out over the same duration
 *   (`createMorphingAnimationFilledToOutLined`, on `COUIEaseInterpolator`, the
 *   `cubic-bezier(0.33, 0, 0.67, 1)` of [SimpleLockEase]);
 * - [failed]: `createFailedAnimator` shakes the whole row through `SHAKE_X_POINT`
 *   (`{ 0, 30, -28, 14, -8, 4, -3, 0 }` px) and drops it by `FALL_SPEED` (250 px) over
 *   `SHAKE_AND_FALL_TIME` (800ms) on `COUILinearInterpolator`, while each filled square falls by
 *   `getDelayFallHeight` = `max(0, translationY - DELAY_FOUR[i] / DELAY_SIX[i])` and fades out once
 *   that distance reaches `FALL_HEIGHT` (150 px). The outlined squares shake but never fall. When the
 *   animation ends the native sets `fail_lastDraw`, clears `mCodeNumber` to -1 and draws the empty
 *   state, which is why the row reads as empty afterwards.
 *
 * The vertical room the native reserves for the fall is `mDrawableHeight + FALL_HEIGHT`, so the
 * component is [SimpleLockViewDefaults.DotSize] + 150 px tall — the raw pixel constants of the class
 * are pixels, not dp, and the port converts them with the current density.
 *
 * The colours are the `coui_simple_lock_*_rectangle_icon_color` resources, which are day/night
 * qualified; `COUITheme.colorScheme` has no slot for them, so they are file constants behind
 * [SimpleLockViewDefaults.colors].
 *
 * Not ported:
 * - the "transparent style" (`isTransparentStyle()` = `mGlowEffectDrawable != null` and SDK >= 34),
 *   which replaces the alpha morph with a `COUISpringAnimation` per square (`SPRING_RESPONSE` 0.35,
 *   `SPRING_BOUNCE` 0.5, `mCircleScales` clamped to `CIRCLE_MAX_SCALE` 1.2) plus the
 *   `coui_simple_lock_glow_effect_drawable` halo of `drawGlowEffect`, its `coui_glow_ball_offset`
 *   (25dp) bounds offset, and the `coui_simple_lock_transparent_*` palette. The halo is a blurred
 *   PNG, which a commonMain canvas cannot composite. Only the classic style is ported, so
 *   [SimpleLockViewDefaults.TransparentFilledColor] and
 *   [SimpleLockViewDefaults.TransparentOutlinedColor] are recovered but unused;
 * - `SimpleLockTouchHelper` (`ExploreByTouchHelper`) and the
 *   `string/coui_simple_lock_access_description` template it fills in (`"Password input area. You
 *   have entered x digits. You need to enter y digits in total."`);
 * - `performFeedback` / `mIsLinearMotorVersion` (`VibrateUtils.isLinearMotorVersion`,
 *   `COUIHapticFeedbackConstants.SHORT_TRIPLE_VIBRATE`, raw haptic 300) and
 *   `setFingerprintRecognition`, which only suppress that feedback;
 * - `getNumberStrList` and the `mDecription` bookkeeping, which exist only for the accessibility
 *   announcements above;
 * - `getAddSpringAnimator` and `getAddAnimator` / `getDeleteAnimator` / `getFailedAnimator`, the
 *   `Animator` handles the app used to drive the view; here the transitions of [count] and [failed]
 *   drive the same curves;
 * - `refresh()`, the re-read of the styled drawables, and `setOutlinedRectangleDrawable` /
 *   `setFilledRectangleDrawable`, which swap the bitmaps; the port draws the recovered shape itself;
 * - `setOpacity(int)`, `setRectanglePadding(int)`, `setRectangleType(int)`, `setSimpleLockType(int)`
 *   and `setScaleX` / `setScaleY`, which mutate state that [type] and the defaults now fix.
 *
 * @param count How many squares are filled, `mCodeNumber + 1`. 0 is the empty row and
 *   [SimpleLockType.dotCount] is the full row.
 * @param modifier The modifier to be applied to the row.
 * @param type How many squares the row holds, `couiCircleNum`.
 * @param failed Whether the row is playing the failure animation. The native's `setFailed` is a
 *   one-shot trigger and so is this: the curve runs when the flag turns true and the row is left
 *   empty afterwards, until [count] changes again.
 * @param colors The filled and outlined colours.
 */
@Composable
fun SimpleLockView(
    count: Int,
    modifier: Modifier = Modifier,
    type: SimpleLockType = SimpleLockType.Six,
    failed: Boolean = false,
    colors: SimpleLockViewColors = SimpleLockViewDefaults.colors(),
) {
    val dotCount = type.dotCount
    val dots = remember(dotCount) { List(dotCount) { SimpleLockDot() } }
    val density = LocalDensity.current

    val dotSizePx = with(density) { SimpleLockViewDefaults.DotSize.toPx() }
    val paddingPx = with(density) { SimpleLockViewDefaults.RectanglePadding.toPx() }
    val cornerRadiusPx = with(density) { SimpleLockViewDefaults.CornerRadius.toPx() }
    val fallHeightDp = with(density) { SimpleLockViewDefaults.FallHeight.toDp() }

    var previousCount by remember(dotCount) { mutableIntStateOf(count.coerceIn(0, dotCount)) }
    var shakeX by remember { mutableFloatStateOf(0f) }
    var fallY by remember { mutableFloatStateOf(0f) }
    var falling by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(count, dotCount) {
        val target = count.coerceIn(0, dotCount)
        val from = previousCount
        previousCount = target
        if (target == from) return@LaunchedEffect
        for (index in 0 until dotCount) {
            val value = if (index < target) 1f else 0f
            if (abs(dots[index].fill.value - value) > 0.001f) {
                val spec = if (value > 0f) {
                    SimpleLockViewDefaults.AddSpec
                } else {
                    SimpleLockViewDefaults.RemoveSpec
                }
                launch { dots[index].fill.animateTo(value, spec) }
            }
        }
    }

    LaunchedEffect(failed) {
        if (!failed) return@LaunchedEffect
        falling = 1f
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = SimpleLockViewDefaults.ShakeAndFallDurationMillis,
                easing = LinearEasing,
            ),
        ) { value, _ ->
            shakeX = SimpleLockViewDefaults.shakeX(value)
            fallY = value * SimpleLockViewDefaults.FallSpeed
        }
        shakeX = 0f
        fallY = 0f
        falling = 0f
        dots.forEach { it.fill.snapTo(0f) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SimpleLockViewDefaults.DotSize + fallHeightDp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rowWidth = dotCount * dotSizePx + (dotCount - 1) * paddingPx
            val startX = (size.width - rowWidth) / 2f
            val fallDelays = SimpleLockViewDefaults.fallDelays(type)
            for (index in 0 until dotCount) {
                val left = startX + index * (dotSizePx + paddingPx)
                drawRoundRect(
                    color = colors.outlinedColor,
                    topLeft = Offset(left + shakeX, 0f),
                    size = Size(dotSizePx, dotSizePx),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                )
                val fill = dots[index].fill.value.coerceIn(0f, 1f)
                if (fill <= 0f) continue
                val fall = if (falling > 0f) (fallY - fallDelays[index]).coerceAtLeast(0f) else 0f
                val alpha = if (falling > 0f) {
                    (1f - fall / SimpleLockViewDefaults.FallHeight).coerceIn(0f, 1f)
                } else {
                    fill
                }
                drawRoundRect(
                    color = colors.filledColor.copy(alpha = colors.filledColor.alpha * alpha),
                    topLeft = Offset(left + shakeX, fall),
                    size = Size(dotSizePx, dotSizePx),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                )
            }
        }
    }
}

/**
 * How many squares a [SimpleLockView] holds, `COUISimpleLock.couiCircleNum` /
 * `DEFAULTTYPE` / `SIXCIRCLETYPE`.
 */
enum class SimpleLockType(
    /** `judgeType()`: the number of squares, also `mRectangleNum`. */
    val dotCount: Int,
) {
    /** `DEFAULTTYPE`, `couiCircleNum` 0: four squares, `mRectanglesWidth = 10dp * 4 + 10dp * 3`. */
    Four(4),

    /** `SIXCIRCLETYPE`, `couiCircleNum` 1: six squares, `mRectanglesWidth = 10dp * 6 + 10dp * 5`. */
    Six(6),
}

/**
 * The colours of a [SimpleLockView] — the two drawables `Widget.COUI.COUISimpleLock` resolves
 * through `R.styleable.COUISimpleLock`, and the `coui_simple_lock_*_rectangle_icon_color` resources
 * their `<solid>` points at.
 *
 * @param filledColor The filled square. `couiFilledRectangleIconDrawable` ->
 *   `coui_simple_lock_filled_rectangle_icon_color`.
 * @param outlinedColor The empty square. `couiOutLinedRectangleIconDrawable` ->
 *   `coui_simple_lock_outlined_rectangle_icon_color`.
 */
@Immutable
data class SimpleLockViewColors(
    val filledColor: Color,
    val outlinedColor: Color,
)

/** Contains the default values used by [SimpleLockView]. */
object SimpleLockViewDefaults {

    /**
     * The side of one square, the `<size>` of `drawable/coui_simple_lock_filled_rectangle_icon`, read
     * into `mDrawableWidth` / `mDrawableHeight`, 10dp.
     */
    val DotSize = 10.dp

    /**
     * The corner radius of a square, the `<corners android:radius>` of
     * `drawable/coui_simple_lock_filled_rectangle_icon`, 2dp.
     */
    val CornerRadius = 2.dp

    /**
     * The gap between two squares. COUI `couiRectanglePadding` of `Widget.COUI.COUISimpleLock`, read
     * into `mRectanglePadding`, 10dp.
     */
    val RectanglePadding = 10.dp

    /**
     * The duration of the alpha morph. `MORPHING_OUTLINED_TO_FILLED_TIME` and
     * `MORPHING_FILLED_TO_OUTLINED_TIME` are both 230ms.
     */
    val MorphDurationMillis = 230

    /** The duration of the shake and fall. `SHAKE_AND_FALL_TIME`, 800ms. */
    val ShakeAndFallDurationMillis = 800

    /**
     * How far the row drops during the failure. `FALL_SPEED`, 250 raw pixels — `ValueAnimator
     * .ofFloat(0.0f, 250.0f)` on `mTransitionY`.
     */
    val FallSpeed = 250f

    /**
     * How far a square must fall before it is invisible. `FALL_HEIGHT`, 150 raw pixels, the divisor
     * of `drawFilledRectangleShakeAndFall`'s alpha and the extra height `onMeasure` reserves below
     * `mDrawableHeight`.
     */
    val FallHeight = 150f

    /**
     * `COUISimpleLock.SHAKE_X_POINT`, the horizontal offsets of `createFailedAnimator`'s
     * `ValueAnimator.ofFloat`, in raw pixels: `{ 0, 30, -28, 14, -8, 4, -3, 0 }`.
     */
    val ShakeXPoints = floatArrayOf(0f, 30f, -28f, 14f, -8f, 4f, -3f, 0f)

    /**
     * `COUISimpleLock.DELAY_FOUR`, how long each of the four squares waits before it starts to fall,
     * in raw pixels of `mTransitionY`: `{ 0, 38.5, 91, 63 }`.
     */
    val DelayFour = floatArrayOf(0f, 38.5f, 91f, 63f)

    /**
     * `COUISimpleLock.DELAY_SIX`, the same for the six-square row:
     * `{ 0, 38.5, 91, 63, 38.5, 70 }`.
     */
    val DelaySix = floatArrayOf(0f, 38.5f, 91f, 63f, 38.5f, 70f)

    /** The filled colour of the light style. `coui_simple_lock_filled_rectangle_icon_color`, `#ff000000`. */
    val FilledColorLight = Color(0xFF000000)

    /** The filled colour at night. `coui_simple_lock_filled_rectangle_icon_color` (night), `#e6ffffff`. */
    val FilledColorDark = Color(0xE6FFFFFF)

    /** The outlined colour of the light style. `coui_simple_lock_outlined_rectangle_icon_color`, `#19000000`. */
    val OutlinedColorLight = Color(0x19000000)

    /** The outlined colour at night. `coui_simple_lock_outlined_rectangle_icon_color` (night), `#66ffffff`. */
    val OutlinedColorDark = Color(0x66FFFFFF)

    /**
     * The filled colour of `Widget.COUI.COUISimpleLock.Dark`, which maps `couiFilledRectangleIconDrawable`
     * to `coui_simple_lock_filled_rectangle_icon_dark` -> `coui_simple_lock_filled_rectangle_icon_dark_color`,
     * `#e6ffffff` — the same value as [FilledColorDark].
     */
    val FilledColorDarkStyle = Color(0xE6FFFFFF)

    /**
     * The outlined colour of `Widget.COUI.COUISimpleLock.Dark`:
     * `coui_simple_lock_outlined_rectangle_icon_dark_color`, `#66ffffff`.
     */
    val OutlinedColorDarkStyle = Color(0x66FFFFFF)

    /**
     * The filled colour of the transparent style.
     * `coui_simple_lock_transparent_filled_rectangle_icon_color`, `#ffffffff`. Recovered but unused:
     * see the "not ported" list of [SimpleLockView].
     */
    val TransparentFilledColor = Color(0xFFFFFFFF)

    /**
     * The outlined colour of the transparent style.
     * `coui_simple_lock_transparent_outlined_rectangle_icon_color`, `#26ffffff`. Recovered but
     * unused: see the "not ported" list of [SimpleLockView].
     */
    val TransparentOutlinedColor = Color(0x26FFFFFF)

    /**
     * `CIRCLE_MAX_SCALE`, the clamp the transparent style applies to a square's spring scale. Kept
     * because it documents the overshoot that style allows; the classic morph ported here never
     * scales.
     */
    val CircleMaxScale = 1.2f

    /** `SPRING_RESPONSE`, the `COUISpringForce` response of the transparent style's pop-in. */
    val SpringResponse = 0.35f

    /** `SPRING_BOUNCE`, the `COUISpringForce` bounce of the transparent style's pop-in. */
    val SpringBounce = 0.5f

    /**
     * The `0 -> 255` morph of `createMorphingAnimationOutLinedToFilled`: `MorphDurationMillis` with
     * no interpolator set on the `ValueAnimator`, i.e. the framework's accelerate-decelerate curve.
     */
    val AddSpec = tween<Float>(durationMillis = MorphDurationMillis, easing = AccelerateDecelerate)

    /**
     * The `255 -> 0` morph of `createMorphingAnimationFilledToOutLined`:
     * `MorphDurationMillis` on `mDeleteAnimatorInterpolator`, a `COUIEaseInterpolator`.
     */
    val RemoveSpec = tween<Float>(durationMillis = MorphDurationMillis, easing = SimpleLockEase)

    /** `DELAY_FOUR` or `DELAY_SIX`, whichever matches [type]. */
    fun fallDelays(type: SimpleLockType): FloatArray = when (type) {
        SimpleLockType.Four -> DelayFour
        SimpleLockType.Six -> DelaySix
    }

    /**
     * The `SHAKE_X_POINT` curve of `createFailedAnimator`, evaluated piecewise-linearly on the linear
     * 800ms progress, which is what `ValueAnimator.ofFloat` does with a linear interpolator.
     */
    fun shakeX(progress: Float): Float {
        val clamped = progress.coerceIn(0f, 1f)
        val spans = ShakeXPoints.size - 1
        val scaled = clamped * spans
        val index = scaled.toInt().coerceAtMost(spans - 1)
        val fraction = scaled - index
        return ShakeXPoints[index] + (ShakeXPoints[index + 1] - ShakeXPoints[index]) * fraction
    }

    /**
     * The light style's colours, or the night ones when the theme background is dark. The resources
     * are day/night qualified rather than theme-attribute driven, so the port branches on the theme
     * luminance, as the rest of this library does.
     */
    @Composable
    fun colors(): SimpleLockViewColors {
        val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
        return SimpleLockViewColors(
            filledColor = if (isDark) FilledColorDark else FilledColorLight,
            outlinedColor = if (isDark) OutlinedColorDark else OutlinedColorLight,
        )
    }

    /**
     * The palette of `Widget.COUI.COUISimpleLock.Dark`, the style the constructor picks through
     * `COUIContextUtil.isCOUIDarkTheme(context)`.
     */
    fun darkColors(): SimpleLockViewColors = SimpleLockViewColors(
        filledColor = FilledColorDarkStyle,
        outlinedColor = OutlinedColorDarkStyle,
    )
}

/** `COUIEaseInterpolator`: the `cubic-bezier(0.33, 0, 0.67, 1)` of `mDeleteAnimatorInterpolator`. */
private val SimpleLockEase: Easing = CubicBezierEasing(0.33f, 0f, 0.67f, 1f)

/** `AccelerateDecelerateInterpolator`: `cos((t + 1) * PI) / 2 + 0.5`, the default of `mAddAnimator`. */
private val AccelerateDecelerate: Easing = Easing { fraction ->
    (cos((fraction + 1f) * PI.toFloat()) / 2.0 + 0.5).toFloat()
}

/** The per-square filled state, `mOpacitys[i]` of `COUISimpleLock`. */
private class SimpleLockDot {
    val fill = Animatable(0f)
}
