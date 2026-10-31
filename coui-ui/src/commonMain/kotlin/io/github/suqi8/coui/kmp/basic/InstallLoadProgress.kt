// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.max
import kotlin.math.min

/**
 * The package-installer progress control.
 *
 * A button that morphs into a progress display and back — install → downloading → installing → done.
 * It is the `AppCompatButton` subclass the installer actually inflates, an install-progress control
 * carrying the install-download-small style. The smooth-corner path is guarded on API 30.
 *
 * ## Relationship to [LoadProgressButton]
 *
 * `LoadProgressButton` already ports the state machine (`COUILoadProgress.DEFAULT_UP_OR_DOWN` = 0,
 * `UPING_OR_DOWNING` = 1, `UP_OR_DOWN_WAIT` = 2, `UP_OR_DOWN_FAIL` = 3, `INSTALL_HAVE_GIFT` = 4,
 * `toggle()`), the three shape tiers with their dimensions, the three
 * `coui_install_load_progress_circle_*` glyph vectors, the `COUISpringForce(bounce = 0, response =
 * 1.0)` progress spring and the `ONE_MILLION` (1.0E-7) skip threshold. This file does **not** repeat
 * any of that: it takes [LoadProgressState], [LoadProgressButtonSize] and
 * `LoadProgressButtonDefaults.glyph` as given.
 *
 * What it adds is everything in `COUIInstallLoadProgress` that `COUILoadProgress` — and therefore
 * `LoadProgressButton` — has no notion of:
 *
 * - **The `mLoadStyle` axis and its rest fill.** `LoadProgressButton` always paints the pill with
 *   the accent colour. The native picks the shape colour per state *and* style through the `z`
 *   argument of `onDrawRoundRect` / `onDrawCircle`; see [InstallLoadProgressDefaults.isFilled].
 * - **A fill that only exists in the transfer states.** `mSpace` is computed inside the
 *   `mState == 1 || mState == 2` branch only. `Fail` (3) paints the whole shape instead of a
 *   fraction, and `Default` (0) paints no fill at all — [InstallLoadProgressDefaults.fillFraction].
 * - **Two-pass label colours.** The label is `mColorPrimary` outside the fill and
 *   `mSurpassProgressColor` (`#ffffffff`) inside it; only `Fail` draws it once, in white.
 * - **Smooth corners.** `COUIShapePath` / `OplusPathAdapter` rather than a circular arc; see
 *   [InstallLoadProgressDefaults.CircleCornerRadius] and
 *   [InstallLoadProgressDefaults.LEGACY_CORNER_EXTENSION].
 * - **The pressed geometry.** While pressed the shape insets by
 *   [InstallLoadProgressDefaults.SCALE_PARAMETER] of the measured size (floored at
 *   [InstallLoadProgressDefaults.MIN_PRESS_FEEDBACK]), the label narrows to
 *   [InstallLoadProgressDefaults.NARROW_FINAL_VALUE], and the brightness scales in HSL rather than
 *   in RGB. `LoadProgressButton` declares those tokens but only animates brightness and the circle
 *   radius.
 * - **Haptics and the downloading accessibility description.**
 *
 * ## Shape and fill matrix
 *
 * `onDraw` branches on `mState` then on `mLoadStyle`. `z` selects `mColorPrimary` when true and
 * `mColorSecondary` when false:
 *
 * | state | `Circle` (`couiStyle` 2) | `BigRound` (1) | `Default` (0) |
 * | :--- | :--- | :--- | :--- |
 * | 0 `Default` | disk `z = false`, no fill | shape `z = true`, no fill | shape `z = false`, no fill |
 * | 1 `Loading` | disk `z = true` | shape `z = false` + fill `z = true` | same as `BigRound` |
 * | 2 `Wait` | disk `z = true` | shape `z = false` + fill `z = true` | same as `BigRound` |
 * | 3 `Fail` | disk `z = true` | shape `z = true`, **whole** shape | same as `BigRound` |
 * | 4 `InstallHaveGift` | nothing is drawn | label only | label only |
 *
 * State 4 falls through both `onDraw` branches: `mInstallGiftBg` is read from the
 * `couiInstallGiftBg` attribute and stored, but this build never draws it.
 *
 * @param onClick Called when the button is pressed and released inside its bounds, the
 *   `super.performClick()` that `mNormalAnimator` fires from `onAnimationStart`.
 * @param modifier The modifier to be applied to the button.
 * @param state The current [LoadProgressState].
 * @param progress The current progress, clamped to `0..max`.
 * @param max The value [progress] is measured against (COUI `mMax`, default 100).
 * @param text The label. The `Circle` style draws no label; the pill styles truncate it with
 *   `string/coui_install_load_progress_apostrophe` (`"..."`) when it does not fit.
 * @param style The `mLoadStyle` tier, set from the `couiStyle` attribute.
 * @param colors The [InstallLoadProgressColors] of the button.
 * @param enabled When false every resolved colour collapses to
 *   [InstallLoadProgressColors.disabledColor], as `getCurrentColor` does when `isEnabled()` is
 *   false.
 * @param hapticFeedback Whether to vibrate on press and release, the `loadingButtonNeedVibrate`
 *   attribute (`mIsNeedVibrate`, false in `style/Widget.COUI.COUILoadProgress`).
 * @param downloadingContentDescription The accessibility description used instead of [text] while
 *   the state is [LoadProgressState.Loading], the `setDownloadingContentDecrpition` value.
 */
@Composable
fun InstallLoadProgress(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: LoadProgressState = LoadProgressState.Default,
    progress: Int = LoadProgressButtonDefaults.PROGRESS,
    max: Int = LoadProgressButtonDefaults.MAX,
    text: String = LoadProgressButtonDefaults.DEFAULT_TEXT,
    style: InstallLoadStyle = InstallLoadStyle.Default,
    colors: InstallLoadProgressColors = InstallLoadProgressDefaults.colors(),
    enabled: Boolean = true,
    hapticFeedback: Boolean = false,
    downloadingContentDescription: String? = null,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // ACTION_DOWN and the ACTION_UP / ACTION_CANCEL that starts mNormalAnimator each call
    // performHapticFeedback(COUIHapticFeedbackConstants.GRANULAR_SHORT_VIBRATE), gated on
    // mIsNeedVibrate. The flag keeps the first composition from firing a stray vibration.
    val haptics = LocalHapticFeedback.current
    var pressWasSeen by remember { mutableStateOf(false) }
    LaunchedEffect(pressed, hapticFeedback) {
        if (hapticFeedback && (pressed || pressWasSeen)) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        pressWasSeen = pressed
    }

    // mPressedAnimator (200ms) and mNormalAnimator (340ms) both use COUIMoveEaseInterpolator, i.e.
    // PathInterpolator(0.3f, 0f, 0.1f, 1f).
    val pressFraction by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (pressed) {
                InstallLoadProgressDefaults.PRESS_DURATION_MILLIS
            } else {
                InstallLoadProgressDefaults.MORPH_DURATION_MILLIS
            },
            easing = InstallLoadProgressDefaults.MoveEase,
        ),
        label = "installLoadProgressPress",
    )

    val box = style.size
    val primary = if (enabled) colors.primaryColor else colors.disabledColor
    val secondary = if (enabled) colors.secondaryColor else colors.disabledColor
    val brightness = 1f - pressFraction * (1f - InstallLoadProgressDefaults.MAX_BRIGHTNESS)

    val targetFraction = if (max > 0) progress.coerceIn(0, max).toFloat() / max else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = LoadProgressButtonDefaults.ProgressSpring(),
        label = "installLoadProgressProgress",
    )
    val fillFraction = InstallLoadProgressDefaults.fillFraction(state, animatedFraction)
    val drawsShape = InstallLoadProgressDefaults.drawsShape(state)

    val squircleEnabled = isSquircleEnabled()
    val labelStyle = COUITheme.textStyles.button.copy(
        fontSize = LoadProgressButtonDefaults.TextSize * (
            1f - pressFraction * (1f - InstallLoadProgressDefaults.NARROW_FINAL_VALUE)
            ),
    )
    val labelColor = InstallLoadProgressDefaults.labelColor(state, style, colors)

    Box(
        modifier = modifier
            .size(width = box.width, height = box.height)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { currentOnClick() },
            )
            .semantics {
                contentDescription = if (state == LoadProgressState.Loading) {
                    downloadingContentDescription ?: text
                } else {
                    text
                }
                progressBarRangeInfo = ProgressBarRangeInfo(animatedFraction, 0f..1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (style == InstallLoadStyle.Circle) {
            // updateRoundRectPath: the disk is (coui_install_download_progress_default_circle_radius
            // - ONE_POINT_FIVE) in radius, i.e. 30dp inside the 33dp touch box, and the pressed
            // animator drives it to mDefaultCircleRadius * 0.9.
            val diameter = InstallLoadProgressDefaults.circleDiameter(box, pressFraction)
            val shape = remember(diameter, squircleEnabled) {
                SquircleShape(
                    cornerRadius = InstallLoadProgressDefaults.CircleCornerRadius,
                    extension = InstallLoadProgressDefaults.LEGACY_CORNER_EXTENSION,
                    squircleEnabled = squircleEnabled,
                )
            }
            Box(
                modifier = Modifier
                    .size(diameter)
                    .clip(shape)
                    .background(
                        if (drawsShape) {
                            InstallLoadProgressDefaults.diskColor(state, style, primary, secondary)
                                .withBrightness(brightness)
                        } else {
                            Color.Transparent
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (drawsShape) {
                    Crossfade(
                        targetState = state,
                        animationSpec = tween(InstallLoadProgressDefaults.CROSSFADE_DURATION_MILLIS),
                        label = "installLoadProgressGlyph",
                    ) { current ->
                        Icon(
                            imageVector = LoadProgressButtonDefaults.glyph(current),
                            contentDescription = null,
                            tint = InstallLoadProgressDefaults.glyphTint(current, colors),
                            modifier = Modifier.size(
                                width = LoadProgressButtonDefaults.GlyphWidth,
                                height = LoadProgressButtonDefaults.GlyphHeight,
                            ),
                        )
                    }
                }
            }
        } else {
            // performTouchStartAnim: mExpandOffsetX/Y ramp to 0 / measured * DEFAULT_SCALE_PARAMETER,
            // floored at measured * DEFAULT_MIN_PRESS_FEEDBACK, and the path rect insets by them.
            val expandFraction = pressFraction * InstallLoadProgressDefaults.SCALE_PARAMETER
            val expandFloor = if (pressed && enabled) {
                expandFraction.coerceAtLeast(InstallLoadProgressDefaults.MIN_PRESS_FEEDBACK)
            } else {
                expandFraction
            }
            val expandX = box.width * expandFloor
            val expandY = box.height * expandFloor
            // updateRoundRectPath: the radius is half the path rect's height. The legacy ColorOS 16
            // branch subtracts dimen/coui_install_download_progress_round_border_radius_offset
            // (0.5dp); the ColorOS 17 branch passes the half height straight through.
            //
            // The shape reads that radius off the size the clip node ends up with, so the press
            // expansion never has to rebuild it frame by frame. Capsule ends follow the system
            // capsule curve; `squircleEnabled = false` keeps the plain rounded fallback.
            val shape = remember(squircleEnabled) {
                CapsuleOutlineShape(squircleEnabled = squircleEnabled)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = expandX, vertical = expandY)
                    .clip(shape)
                    .background(
                        if (drawsShape) {
                            InstallLoadProgressDefaults.diskColor(state, style, primary, secondary)
                                .withBrightness(brightness)
                        } else {
                            Color.Transparent
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (fillFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .fillMaxWidth(fillFraction)
                            .background(primary.withBrightness(brightness)),
                    )
                }
                Text(
                    text = text,
                    color = labelColor,
                    style = labelStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = colors.textPadding),
                )
                if (fillFraction > 0f) {
                    // onDrawText re-draws the label with mSurpassProgressColor inside
                    // canvas.clipRect(..., mSpace, ...) so the part over the fill flips colour.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clipToProgressFraction(fillFraction),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = text,
                            color = colors.surpassProgressColor,
                            style = labelStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = colors.textPadding),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The `mLoadStyle` values of `COUIInstallLoadProgress`, set from the `couiStyle` attribute, whose
 * enum is declared in the resource dump as `defaultSmallRoundButton` (0), `bigRoundButton` (1) and
 * `smallCircleButton` (2).
 *
 * @property value The native `int` constant (`LOAD_STYLE_DEFAULT`, `LOAD_STYLE_BIG_ROUND`,
 *   `LOAD_STYLE_CIRCLE`).
 * @property size The [LoadProgressButtonSize] carrying the same dimensions: `Widget.COUI.COUILoadProgress`
 *   sets `couiInstallViewWidth` / `couiInstallViewHeight`, the `bigRoundButton` layouts use
 *   `dimen/coui_install_download_progress_width_large` / `_height_large`, and `smallCircleButton`
 *   uses `dimen/coui_install_download_progress_circle_width` / `_circle_height`.
 */
enum class InstallLoadStyle(val value: Int, val size: LoadProgressButtonSize) {
    /** `LOAD_STYLE_DEFAULT` (0), `couiStyle=defaultSmallRoundButton`: the 52dp x 28dp pill. */
    Default(0, LoadProgressButtonSize.Small),

    /** `LOAD_STYLE_BIG_ROUND` (1), `couiStyle=bigRoundButton`: the 220dp x 44dp pill. */
    BigRound(1, LoadProgressButtonSize.Large),

    /** `LOAD_STYLE_CIRCLE` (2), `couiStyle=smallCircleButton`: the 33dp square touch box. */
    Circle(2, LoadProgressButtonSize.Circle),
}

/**
 * Colors used by an [InstallLoadProgress].
 *
 * It is deliberately not [LoadProgressButtonColors]: the native resolves four independent colour
 * roles per state (`mColorPrimary`, `mColorSecondary`, `mColorWhite` / `mSurpassProgressColor` and
 * `mDisabledColor`), while [LoadProgressButtonColors] models the roles of the pill alone.
 *
 * @param primaryColor `mColorPrimary`, read straight from `R.attr.couiColorPrimary`.
 * @param secondaryColor `mColorSecondary`, read from `R.attr.couiColorSecondary`. It is both the
 *   unfilled pill track and the resting circle disk.
 * @param textColor `mColorWhite`, `color/coui_install_load_progress_text_color_in_progress`.
 * @param surpassProgressColor `mSurpassProgressColor`, initialised from the same colour resource;
 *   the label is re-drawn in it where it overlaps the filled part.
 * @param disabledColor `mDisabledColor`, from the `disabledColor` attribute, which
 *   `style/Widget.COUI.COUILoadProgress.InstallDownload` sets to
 *   `?attr/couiBtnDrawableColorDisabled`; `getCurrentColor` returns it for both the primary and the
 *   secondary role while the view is disabled.
 * @param textPadding The horizontal label padding, the style's `couiInstallPadding` (6dp).
 */
@Immutable
data class InstallLoadProgressColors(
    val primaryColor: Color,
    val secondaryColor: Color,
    val textColor: Color,
    val surpassProgressColor: Color,
    val disabledColor: Color,
    val textPadding: Dp,
)

/** Contains the values [InstallLoadProgress] adds on top of [LoadProgressButtonDefaults]. */
object InstallLoadProgressDefaults {

    /**
     * `COUIInstallLoadProgress.DEFAULT_MIN_PRESS_FEEDBACK` (0.005): the floor the pressed animator
     * applies to `mExpandOffsetX` / `mExpandOffsetY`, as a fraction of the measured size.
     */
    const val MIN_PRESS_FEEDBACK = 0.005f

    /**
     * `COUIInstallLoadProgress.DEFAULT_SCALE_PARAMETER` (0.05), the same value
     * [LoadProgressButtonDefaults.PRESS_SCALE_PARAMETER] carries. Each edge of the path rect is
     * inset by this fraction of the measured width and height while pressed, so the shape shrinks
     * rather than grows.
     */
    const val SCALE_PARAMETER = LoadProgressButtonDefaults.PRESS_SCALE_PARAMETER

    /**
     * `COUIInstallLoadProgress.DEFAULT_NARROW_FINAL_VALUE` (0.92): the factor applied to the label
     * size while pressed.
     */
    const val NARROW_FINAL_VALUE = LoadProgressButtonDefaults.PRESS_NARROW_FINAL_VALUE

    /**
     * The factor `mPressedAnimator` applies to `mDefaultCircleRadius` on the `Circle` style: the
     * disk radius animates to `mDefaultCircleRadius * 0.9`.
     */
    const val CIRCLE_RADIUS_SCALE = LoadProgressButtonDefaults.PRESS_CIRCLE_RADIUS_SCALE

    /**
     * `COUIInstallLoadProgress.DEFAULT_BRIGHTNESS_MAX_VALUE` (0.8), also the default of the
     * `brightness` attribute. Both install styles set `brightness=0.8`.
     */
    const val MAX_BRIGHTNESS = LoadProgressButtonDefaults.PRESS_BRIGHTNESS

    /** `COUIInstallLoadProgress.PRESS_ANIMATOR_DURATION` (200ms). */
    const val PRESS_DURATION_MILLIS = LoadProgressButtonDefaults.PRESS_DURATION_MILLIS

    /** `COUIInstallLoadProgress.NORMAL_ANIMATOR_DURATION` (340ms). */
    const val MORPH_DURATION_MILLIS = LoadProgressButtonDefaults.MORPH_DURATION_MILLIS

    /**
     * The glyph cross-fade duration, the `CIRCLE_IN_ALPHA_HOLDER` / `CIRCLE_OUT_ALPHA_HOLDER` track
     * of `mNormalAnimator` (`CIRCLE_MAX_ALPHA` = 255).
     */
    const val CROSSFADE_DURATION_MILLIS = LoadProgressButtonDefaults.CROSSFADE_DURATION_MILLIS

    /**
     * The corner-tile multiple `COUIShapePath.getRoundRectPath` uses — every curve offset in it is
     * scaled by `radius * 128.19 / 100`. The `Circle` style is the only `mLoadStyle` that reaches
     * that legacy path on ColorOS 17 (`updateRoundRectPath` returns early for `mLoadStyle == 2`),
     * so this is the extension the disk is drawn with.
     */
    const val LEGACY_CORNER_EXTENSION = 1.2819f

    /**
     * `dimen/coui_install_download_progress_circle_round_border_radius` (7dp): the corner radius
     * `updateRoundRectPath` passes for the `Circle` style, on a disk that is only 30dp across — the
     * silhouette is a smooth-corner square, not a circle.
     */
    val CircleCornerRadius = 7.dp

    /**
     * `COUIInstallLoadProgress.ONE_POINT_FIVE` (1.5dp): `getDefaultSize` subtracts it once
     * (`z == true`) from `dimen/coui_install_download_progress_default_circle_radius` to get the
     * disk radius, and twice from the touch width to get `mDefaultWidth`, the width the label is
     * fitted to.
     */
    val OnePointFive = 1.5.dp

    /**
     * `dimen/coui_install_download_progress_round_border_radius_offset` (0.5dp): the legacy
     * `updateRoundRectPath` subtracts it from the half height. The ColorOS 17 smooth-corner branch
     * does not.
     */
    val RadiusOffset = 0.5.dp

    /** `COUIMoveEaseInterpolator`: `PathInterpolator(0.3f, 0f, 0.1f, 1f)`. */
    val MoveEase: Easing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

    /** The disk diameter for the `Circle` style at [pressFraction] of the press animation. */
    fun circleDiameter(box: LoadProgressButtonSize, pressFraction: Float): Dp {
        val radius = box.width / 2 - OnePointFive
        return radius * 2 * (1f - pressFraction * (1f - CIRCLE_RADIUS_SCALE))
    }

    /**
     * The `z` argument `onDraw` passes to `onDrawRoundRect` / `onDrawCircle` for the shape it paints
     * *before* any fill: `true` selects `mColorPrimary`, `false` `mColorSecondary`.
     *
     * `Default` is the only state that looks at `mLoadStyle`: the `bigRoundButton` branch passes
     * `true`, every other branch `false`. `Loading` and `Wait` pass `false` and then paint the
     * filled part over it in the accent colour; `Fail` passes `true`.
     */
    fun isFilled(state: LoadProgressState, style: InstallLoadStyle): Boolean = when (state) {
        LoadProgressState.Default -> style == InstallLoadStyle.BigRound
        LoadProgressState.Loading -> false
        LoadProgressState.Wait -> false
        LoadProgressState.Fail -> true
        LoadProgressState.InstallHaveGift -> false
    }

    /**
     * The fraction of the shape painted in the accent colour.
     *
     * `mSpace` is computed only in the `mState == 1 || mState == 2` branch — and as
     * `(int) (fraction * (mTouchModeWidth - 2 * mExpandOffsetX)) + mExpandOffsetX`, which at rest is
     * simply `fraction * width`. `Fail` paints the whole path instead, and `Default` /
     * `InstallHaveGift` paint no fill at all.
     */
    fun fillFraction(state: LoadProgressState, fraction: Float): Float = when (state) {
        LoadProgressState.Loading -> fraction
        LoadProgressState.Wait -> fraction
        LoadProgressState.Fail -> 1f
        LoadProgressState.Default -> 0f
        LoadProgressState.InstallHaveGift -> 0f
    }

    /**
     * Whether `onDraw` paints any shape for [state]. `INSTALL_HAVE_GIFT` (4) matches neither the
     * `mState == 0` nor the `mState == 1 || mState == 2` branch and is not `mState == 3` either, so
     * it falls through to the label pass with nothing behind it; the `Circle` style draws nothing at
     * all for it, because that pass is itself guarded by `mLoadStyle != 2`.
     */
    fun drawsShape(state: LoadProgressState): Boolean = state != LoadProgressState.InstallHaveGift

    /**
     * The colour of the shape for [state] and [style], before the pressed brightness is applied.
     * See [isFilled] for which of the two roles applies.
     */
    fun diskColor(
        state: LoadProgressState,
        style: InstallLoadStyle,
        primaryColor: Color,
        secondaryColor: Color,
    ): Color = if (isFilled(state, style)) primaryColor else secondaryColor

    /**
     * The tint of the state glyph on the `Circle` style. `onAttachedToWindow` tints
     * `mCircleLoadBitmap` with `mColorPrimary` — it sits on the resting disk, which is painted with
     * `mColorSecondary` — while the pause and reload vectors carry
     * `color/coui_install_load_progress_circle_color_pause` / `_reload` (`#ffffffff`) and ride the
     * accent disk.
     */
    fun glyphTint(state: LoadProgressState, colors: InstallLoadProgressColors): Color = if (state == LoadProgressState.Default) colors.primaryColor else colors.textColor

    /**
     * The colour the label is drawn in outside the filled part.
     *
     * `onDraw` sets `mColorWhite` for `Fail` and for `Default` on the `bigRoundButton` style, whose
     * shape it paints in `mColorPrimary`; `mColorPrimary` for `Default` on the
     * `defaultSmallRoundButton` style; and `mColorPrimary` again for `Loading` / `Wait` — the label
     * only turns `mSurpassProgressColor` where it overlaps the fill (ColorOS 17
     * `COUIInstallLoadProgress.onDraw`).
     */
    fun labelColor(
        state: LoadProgressState,
        style: InstallLoadStyle,
        colors: InstallLoadProgressColors,
    ): Color = when {
        state == LoadProgressState.Fail -> colors.textColor
        state == LoadProgressState.Default && style == InstallLoadStyle.BigRound -> colors.textColor
        else -> colors.primaryColor
    }

    /** The label colour for the `defaultSmallRoundButton` style. */
    @Deprecated(
        message = "The label colour depends on the style: the big round button draws a white label on its filled shape.",
        replaceWith = ReplaceWith("labelColor(state, InstallLoadStyle.Default, colors)"),
    )
    fun labelColor(state: LoadProgressState, colors: InstallLoadProgressColors): Color = labelColor(state, InstallLoadStyle.Default, colors)

    /**
     * Creates the default [InstallLoadProgressColors].
     *
     * `COUIInstallLoadProgress` reads `couiColorPrimary` and `couiColorSecondary` straight off the
     * theme; `style/COUIOverlay.Theme.Blue.Default` binds them to `color/couiBlueTintControlNormal`
     * (`#ff006aff`) and `color/couiBlueTintLightNormal` (`#26007aff`) respectively. The port maps the
     * primary to the theme accent and the secondary — a 15% alpha wash of the accent — to
     * `COUITheme.colorScheme.tertiaryContainer` (`0x260080FF`), the role the theme already carries
     * for exactly that wash.
     *
     * @param primaryColor `R.attr.couiColorPrimary`.
     * @param secondaryColor `R.attr.couiColorSecondary`.
     * @param textColor `color/coui_install_load_progress_text_color_in_progress` (`#ffffffff`).
     * @param surpassProgressColor `mSurpassProgressColor`, the same `#ffffffff`.
     * @param disabledColor `disabledColor` = `?attr/couiBtnDrawableColorDisabled` =
     *   `color/coui_btn_drawable_color_disabled` (`#14000000`), the value the theme's
     *   `COUITheme.colorScheme.disabledSecondaryVariant` carries.
     * @param textPadding `couiInstallPadding` (6dp).
     */
    @Composable
    fun colors(
        primaryColor: Color = COUITheme.colorScheme.primary,
        secondaryColor: Color = COUITheme.colorScheme.tertiaryContainer,
        textColor: Color = PROGRESS_TEXT_WHITE,
        surpassProgressColor: Color = PROGRESS_TEXT_WHITE,
        disabledColor: Color = COUITheme.colorScheme.disabledSecondaryVariant,
        textPadding: Dp = LoadProgressButtonDefaults.TextPadding,
    ): InstallLoadProgressColors = remember(
        primaryColor,
        secondaryColor,
        textColor,
        surpassProgressColor,
        disabledColor,
        textPadding,
    ) {
        InstallLoadProgressColors(
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            textColor = textColor,
            surpassProgressColor = surpassProgressColor,
            disabledColor = disabledColor,
            textPadding = textPadding,
        )
    }
}

/** `color/coui_install_load_progress_text_color_in_progress` (`#ffffffff`). */
private val PROGRESS_TEXT_WHITE = Color(0xFFFFFFFF)

/**
 * `getCurrentColor` converts the resolved colour to HSL with `ColorUtils.colorToHSL`, multiplies the
 * lightness by `mCurrentBrightness` and converts back — a lightness scale, not the per-channel
 * multiply a plain RGB dim would produce.
 */
private fun Color.withBrightness(brightness: Float): Color {
    if (brightness == 1f) return this
    val maxChannel = max(red, max(green, blue))
    val minChannel = min(red, min(green, blue))
    val lightness = (maxChannel + minChannel) / 2f
    val delta = maxChannel - minChannel
    val hue: Float
    val saturation: Float
    if (delta == 0f) {
        hue = 0f
        saturation = 0f
    } else {
        saturation = if (lightness > 0.5f) {
            delta / (2f - maxChannel - minChannel)
        } else {
            delta / (maxChannel + minChannel)
        }
        val rawHue = when (maxChannel) {
            red -> 60f * (((green - blue) / delta) % 6f)
            green -> 60f * (((blue - red) / delta) + 2f)
            else -> 60f * (((red - green) / delta) + 4f)
        }
        hue = if (rawHue < 0f) rawHue + 360f else rawHue
    }
    return Color.hsl(
        hue = hue,
        saturation = saturation.coerceIn(0f, 1f),
        lightness = (lightness * brightness).coerceIn(0f, 1f),
        alpha = alpha,
    )
}

/**
 * Clips drawing to the first [fraction] of the width, the `canvas.clipRect(f, f2, mSpace, ...)` of
 * the native fill and label-over-fill passes.
 */
private fun Modifier.clipToProgressFraction(fraction: Float): Modifier = drawWithContent {
    clipRect(right = size.width * fraction) { this@drawWithContent.drawContent() }
}
