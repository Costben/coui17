// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The ColorOS 17 install/download button, mirroring `COUILoadProgress`
 * (`com/coui/appcompat/progressbar/COUILoadProgress`, an `AppCompatButton`) and the drawing half of
 * its subclass `COUIInstallLoadProgress`, driven by the style
 * `Widget.COUI.COUILoadProgress` and `Widget.COUI.COUILoadProgress.InstallDownloadSmall`.
 *
 * It is a *button*, not a progress indicator: `ProgressIndicator` ports `COUILoadingView` and
 * `COUICircularProgressDrawable`, which are read-only spinners, while this widget is clickable and
 * owns a five-value state machine (`COUILoadProgress.DEFAULT_UP_OR_DOWN` = 0,
 * `UPING_OR_DOWNING` = 1, `UP_OR_DOWN_WAIT` = 2, `UP_OR_DOWN_FAIL` = 3, `INSTALL_HAVE_GIFT` = 4)
 * surfaced here as [LoadProgressState]. `performClick()` calls `toggle()`, whose transitions are
 * reproduced by [LoadProgressState.toggled]:
 * 0 -> 1, 1 -> 2, 2 -> 1, 3 -> 1, and 4 is left alone (the native `toggle()` has no branch for
 * `INSTALL_HAVE_GIFT`).
 *
 * Drawing is delegated to [InstallLoadProgress], the port of the only drawing class the native has:
 * [LoadProgressButtonSize] picks the [InstallLoadStyle], and the capsule outline, the per-state fill,
 * the two-pass label and the press animators all live there. This overload remains for callers that
 * work with [LoadProgressButtonColors]; new code can call [InstallLoadProgress] directly.
 *
 * @param onClick Called when the button is pressed and released.
 * @param modifier The modifier to be applied to the button.
 * @param state The current [LoadProgressState].
 * @param progress The current progress, clamped to `0..max`.
 * @param max The value [progress] is measured against (COUI `mMax`, default 100).
 * @param text The label. The circular tier draws no label; the pill tiers truncate it with the
 *   `coui_install_load_progress_apostrophe` ("...") suffix when it does not fit.
 * @param size The shape tier: [LoadProgressButtonSize.Circle] for the `InstallDownloadSmall` circle,
 *   [LoadProgressButtonSize.Small] / [LoadProgressButtonSize.Large] for the pills.
 * @param colors The [LoadProgressButtonColors] of the button.
 */
@Composable
@NonRestartableComposable
fun LoadProgressButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: LoadProgressState = LoadProgressState.Default,
    progress: Int = 0,
    max: Int = LoadProgressButtonDefaults.MAX,
    text: String = LoadProgressButtonDefaults.DEFAULT_TEXT,
    size: LoadProgressButtonSize = LoadProgressButtonSize.Small,
    colors: LoadProgressButtonColors = LoadProgressButtonDefaults.colors(),
) {
    val isTransferring = state == LoadProgressState.Loading || state == LoadProgressState.Wait
    InstallLoadProgress(
        onClick = onClick,
        modifier = modifier,
        state = state,
        progress = progress,
        max = max,
        text = text,
        style = when (size) {
            LoadProgressButtonSize.Circle -> InstallLoadStyle.Circle
            LoadProgressButtonSize.Small -> InstallLoadStyle.Default
            LoadProgressButtonSize.Large -> InstallLoadStyle.BigRound
        },
        colors = InstallLoadProgressDefaults.colors(
            primaryColor = if (isTransferring) colors.progressColor else colors.containerColor,
            secondaryColor = colors.trackColor,
            textColor = colors.contentColor,
            surpassProgressColor = colors.surpassContentColor,
            textPadding = colors.textPadding,
        ),
    )
}

/**
 * The five states of `COUILoadProgress`.
 *
 * @property value The native `int` constant.
 */
enum class LoadProgressState(val value: Int) {
    /** `COUILoadProgress.DEFAULT_UP_OR_DOWN` (0): idle, "Download". */
    Default(0),

    /** `COUILoadProgress.UPING_OR_DOWNING` (1): a transfer is running. */
    Loading(1),

    /** `COUILoadProgress.UP_OR_DOWN_WAIT` (2): a transfer is queued. */
    Wait(2),

    /** `COUILoadProgress.UP_OR_DOWN_FAIL` (3): a transfer failed. */
    Fail(3),

    /** `COUILoadProgress.INSTALL_HAVE_GIFT` (4): the install bundles a gift. */
    InstallHaveGift(4),
    ;

    /**
     * The state `COUILoadProgress.toggle()` moves to, which is what `performClick()` applies:
     * 0 -> 1, 1 -> 2, 2 -> 1, 3 -> 1, and `INSTALL_HAVE_GIFT` unchanged.
     */
    fun toggled(): LoadProgressState = when (this) {
        Default -> Loading
        Loading -> Wait
        Wait -> Loading
        Fail -> Loading
        InstallHaveGift -> InstallHaveGift
    }
}

/**
 * The shape tiers of a [LoadProgressButton].
 *
 * @property width The width (COUI `dimen/coui_install_download_progress_width{,_large}` and
 *   `dimen/coui_install_download_progress_circle_width`).
 * @property height The height (COUI `dimen/coui_install_download_progress_height{,_large}` and
 *   `dimen/coui_install_download_progress_circle_height`).
 * @property cornerRadius The corner radius (COUI
 *   `dimen/coui_install_download_progress_round_border_radius_small` for [Small],
 *   `dimen/coui_install_download_progress_round_border_radius` for [Large], and
 *   `dimen/coui_install_download_progress_default_circle_radius` for [Circle]).
 */
enum class LoadProgressButtonSize(val width: Dp, val height: Dp, val cornerRadius: Dp) {
    /** `Widget.COUI.COUILoadProgress.InstallDownloadSmall`: the `mLoadStyle == 2` circle, glyph only. */
    Circle(33.dp, 33.dp, 16.5.dp),

    /** `dimen/coui_install_download_progress_width` x `_height`, the small install pill. */
    Small(52.dp, 28.dp, 13.5.dp),

    /** `dimen/coui_install_download_progress_width_large` x `_height_large`, the wide install pill. */
    Large(220.dp, 44.dp, 21.5.dp),
}

/** Contains default values used by [LoadProgressButton]. */
object LoadProgressButtonDefaults {

    /** `COUILoadProgress.mMax` after `init()`. */
    const val MAX = 100

    /** `COUILoadProgress.mProgress` after `init()`. */
    const val PROGRESS = 0

    /** The label the style sets through `couiInstallTextview` (`string/coui_install_download_progress_textview`, "Download"). */
    const val DEFAULT_TEXT = "Download"

    /**
     * The `coui_install_load_progress_apostrophe` suffix the native `addApostrophe()` appends when
     * the label does not fit the widget width.
     */
    const val APOSTROPHE = "..."

    /**
     * `COUIInstallLoadProgress.NORMAL_ANIMATOR_DURATION` (340ms): the `mNormalAnimator` that
     * restores the resting shape after a press and, on the circle style, cross-fades the glyph.
     */
    const val MORPH_DURATION_MILLIS = 340

    /** `COUIInstallLoadProgress.PRESS_ANIMATOR_DURATION` (200ms): the `mPressedAnimator`. */
    const val PRESS_DURATION_MILLIS = 200

    /** The glyph cross-fade duration, the `CIRCLE_IN_ALPHA_HOLDER` / `CIRCLE_OUT_ALPHA_HOLDER` track of `mNormalAnimator`. */
    const val CROSSFADE_DURATION_MILLIS = 340

    /**
     * The `brightness` attribute both install styles set (0.8). `mPressedAnimator` ramps the widget
     * brightness from 1.0 down to this on touch down.
     */
    const val PRESS_BRIGHTNESS = 0.8f

    /**
     * `COUIInstallLoadProgress.DEFAULT_SCALE_PARAMETER` (0.05): the fraction of its own size the pill
     * expands by while pressed.
     */
    const val PRESS_SCALE_PARAMETER = 0.05f

    /**
     * `COUIInstallLoadProgress.DEFAULT_NARROW_FINAL_VALUE` (0.92): the label scale the pill narrows
     * to while pressed.
     */
    const val PRESS_NARROW_FINAL_VALUE = 0.92f

    /**
     * The factor `mPressedAnimator` applies to `mDefaultCircleRadius` on the circle style: the
     * radius animates to `mDefaultCircleRadius * 0.9`.
     */
    const val PRESS_CIRCLE_RADIUS_SCALE = 0.9f

    /** The label size (COUI `dimen/coui_install_download_progress_textsize{,_large}`, 14sp). */
    val TextSize = 14.sp

    /** The label padding the style sets through `couiInstallPadding` (6dp). */
    val TextPadding = 6.dp

    /**
     * The extra width COUI adds to (or removes from) the widget when the locale changes, from
     * `dimen/coui_install_download_progress_width_in_foreign_language` (30dp). Not applied by this
     * port — see the file's "not ported" note in the documentation.
     */
    val ForeignLanguageWidthDelta = 30.dp

    /**
     * The width of the state glyph, the `android:width` of
     * `drawable/coui_install_load_progress_circle_{load,pause,reload}` (12dp).
     */
    val GlyphWidth = 12.dp

    /**
     * The height of the state glyph, the `android:height` of the same vectors (14dp).
     */
    val GlyphHeight = 14.dp

    /**
     * The `COUISpringForce` `refreshProgressWithAnim` builds: `setBounce(0)` with
     * `setResponse(1.0)`, i.e. a critically damped spring over a one second response.
     */
    @Composable
    fun ProgressSpring() = folmeSpring<Float>(damping = 1f, response = 1f)

    /**
     * The vector `drawable/coui_install_load_progress_circle_load`: the download arrow whose fill is
     * `?couiColorPrimary`, a 36x42 path on a 12dp x 14dp box.
     */
    val LoadGlyph: ImageVector by lazy { buildProgressGlyph("LoadProgressLoad", LOAD_GLYPH_PATH) }

    /**
     * The vector `drawable/coui_install_load_progress_circle_pause`: the two rounded bars whose fill
     * is `color/coui_install_load_progress_circle_color_pause` (#ffffffff).
     */
    val PauseGlyph: ImageVector by lazy { buildProgressGlyph("LoadProgressPause", PAUSE_GLYPH_PATH) }

    /**
     * The vector `drawable/coui_install_load_progress_circle_reload`: the same arrow as
     * [LoadGlyph], filled with `color/coui_install_load_progress_circle_color_reload` (#ffffffff).
     */
    val ReloadGlyph: ImageVector by lazy { buildProgressGlyph("LoadProgressReload", LOAD_GLYPH_PATH) }

    /**
     * The glyph `COUIInstallLoadProgress.onDrawCircle` draws for a state. `InstallHaveGift` has no
     * circle glyph in the native code — that state is reserved for the `couiInstallGiftBg` badge —
     * so it falls back to the download arrow.
     */
    fun glyph(state: LoadProgressState): ImageVector = when (state) {
        LoadProgressState.Default -> LoadGlyph
        LoadProgressState.Loading -> PauseGlyph
        LoadProgressState.Wait -> ReloadGlyph
        LoadProgressState.Fail -> ReloadGlyph
        LoadProgressState.InstallHaveGift -> LoadGlyph
    }

    /**
     * Creates the default [LoadProgressButtonColors].
     *
     * `Widget.COUI.COUILoadProgress` sets both `couiInstallDefaultColor` and `couiLoadProgressColor`
     * to `color/coui_tint_selector`, which resolves to `couiColorPrimary` (pressed:
     * `couiColorPrimaryPressed`); the container and the progress fill are therefore the accent color,
     * and `coui_install_load_progress_text_color_in_progress` makes the label white.
     *
     * @param containerColor The pill fill. COUI `coui_tint_selector` in the default state.
     * @param progressColor The filled part of the pill while [LoadProgressState.Loading]. COUI
     *   `coui_tint_selector`.
     * @param trackColor The un-filled part of the pill. The native `onDrawRoundRect(..., z = false)`
     *   paints it with the style's `couiColorSecondary`; this port maps that to the theme's track role
     *   used by [ProgressIndicatorDefaults], since the port has no recovered value for that attribute.
     * @param contentColor The label and the glyph. COUI
     *   `color/coui_install_load_progress_text_color_in_progress` / `_circle_color_pause` /
     *   `_circle_color_reload` (#ffffffff).
     * @param surpassContentColor The label where it overlaps the filled part. The native
     *   `COUIInstallLoadProgress.mSurpassProgressColor`.
     * @param textPadding The horizontal label padding. COUI `couiInstallPadding` (6dp).
     */
    @Composable
    fun colors(
        containerColor: Color = COUITheme.colorScheme.primary,
        progressColor: Color = COUITheme.colorScheme.primary,
        trackColor: Color = COUITheme.colorScheme.tertiaryContainer,
        contentColor: Color = PROGRESS_COLOR_WHITE,
        surpassContentColor: Color = PROGRESS_COLOR_WHITE,
        textPadding: Dp = TextPadding,
    ): LoadProgressButtonColors = remember(
        containerColor,
        progressColor,
        trackColor,
        contentColor,
        surpassContentColor,
        textPadding,
    ) {
        LoadProgressButtonColors(
            containerColor = containerColor,
            progressColor = progressColor,
            trackColor = trackColor,
            contentColor = contentColor,
            surpassContentColor = surpassContentColor,
            textPadding = textPadding,
        )
    }
}

/**
 * Colors used by a [LoadProgressButton].
 *
 * @param containerColor The pill fill, and the disk fill of the circular tier
 *   (`couiInstallDefaultColor` / `couiLoadProgressColor` = `color/coui_tint_selector`).
 * @param progressColor The filled part of the pill while the progress is advancing.
 * @param trackColor The un-filled part of the pill.
 * @param contentColor The label and the state glyph
 *   (`color/coui_install_load_progress_text_color_in_progress`, `_circle_color_pause`,
 *   `_circle_color_reload`).
 * @param surpassContentColor The label where it overlaps the filled part (`mSurpassProgressColor`).
 * @param textPadding The horizontal label padding (`couiInstallPadding`).
 */
@Immutable
data class LoadProgressButtonColors(
    val containerColor: Color,
    val progressColor: Color,
    val trackColor: Color,
    val contentColor: Color,
    val surpassContentColor: Color,
    val textPadding: Dp,
)

/** `color/coui_install_load_progress_text_color_in_progress` (#ffffffff). */
private val PROGRESS_COLOR_WHITE = Color(0xFFFFFFFF)

/**
 * `color/pathData` of `drawable/coui_install_load_progress_circle_load` and `..._reload`: a download
 * arrow. The two drawables differ only in their fill color.
 */
private const val LOAD_GLYPH_PATH =
    "M 20 34.2 L 31.6 22.5 C 32.4 21.7 33.6 21.7 34.4 22.5 C 35.2 23.3 35.2 24.5 34.4 25.3 " +
        "L 20.8 39.1 C 19.2 40.7 16.7 40.7 15.1 39.1 L 1.6 25.5 C 0.8 24.7 0.8 23.5 1.6 22.7 " +
        "C 2.4 21.9 3.6 21.9 4.4 22.7 L 16 34.2 L 16 4 C 16 2.9 16.9 2 18 2 C 19.1 2 20 2.9 20 4 " +
        "L 20 34.2 Z"

/**
 * `color/pathData` of `drawable/coui_install_load_progress_circle_pause`: two rounded vertical bars.
 */
private const val PAUSE_GLYPH_PATH =
    "M 12 8 L 12 34 C 12 35.1 11.1 36 10 36 C 8.9 36 8 35.1 8 34 L 8 8 C 8 6.9 8.9 6 10 6 " +
        "C 11.1 6 12 6.9 12 8 Z M 28 8 L 28 34 C 28 35.1 27.1 36 26 36 C 24.9 36 24 35.1 24 34 " +
        "L 24 8 C 24 6.9 24.9 6 26 6 C 27.1 6 28 6.9 28 8 Z"

/**
 * Builds one of the `coui_install_load_progress_circle_*` vectors. They are all declared as
 * 12dp x 14dp with a 36 x 42 viewport, so the viewport maps straight onto the icon box.
 */
private fun buildProgressGlyph(name: String, pathData: String): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = LoadProgressButtonDefaults.GlyphWidth,
    defaultHeight = LoadProgressButtonDefaults.GlyphHeight,
    viewportWidth = 36f,
    viewportHeight = 42f,
).apply {
    addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        pathFillType = PathFillType.NonZero,
        fill = SolidColor(Color.White),
    )
}.build()
