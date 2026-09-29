// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The completion state [CompleteState] shows, mirroring the `completeType` values of ColorOS's
 * `com.coui.appcompat.completeview.COUICompleteStateView` (`coui_component_complete_state.xml`).
 */
enum class CompleteStateStatus {
    /** `COMPLETE_TYPE_DEFAULT` (0): no playable is set, so the illustration box stays empty. */
    Default,

    /** `COMPLETE_TYPE_SUCCESS` (1): plays `raw/complete_page_success_anim.json`. */
    Success,

    /** `COMPLETE_TYPE_FAILED` (2): plays `raw/complete_page_fail_anim.json`. */
    Failed,

    /** `COMPLETE_TYPE_WAIT` (3): plays `raw/complete_page_wait_anim.json`. */
    Wait,
}

/**
 * A full-page completion state with COUI style, mirroring ColorOS's COUICompleteStateView
 * (`coui_component_complete_state.xml`): a 100dp playable, a title and an optional subtitle, all
 * centered horizontally. Used after an operation finishes — a finished optimisation, a completed
 * sync, a timed-out activation. There is no button: the device page carries none and leaves the
 * exit to the caller.
 *
 * On the device the view is a `COUIPercentWidthLinearLayout` whose style sets
 * `gridNumber = @integer/complete_page_grid_guide_column` (4) and `paddingType = cardListType`, so
 * its width comes out of the responsive grid rather than a fixed dp; the port fills whatever width
 * the [modifier] gives it, since the repo has no responsive grid model.
 *
 * The COUI library ships no playable, so nothing is drawn unless [illustration] is supplied. On the
 * device the box hosts an oplus-anim (`raw/complete_page_{success,fail,wait}_anim.json`, a 300x300
 * Lottie canvas) chosen by [status], replaying whenever `setCompleteType` changes it.
 *
 * @param status The [CompleteStateStatus] of the [CompleteState]. It selects the device's playable;
 *   with no asset in the library it only tells the caller which state this page shows — pair it
 *   with [CompleteStateDefaults.animationDuration] to time the page the way the raw anims run.
 * @param title The title of the [CompleteState], centered (COUI couiTextHeadlineM: 20sp, medium,
 *   line spacing multiplier 1.1).
 * @param modifier The modifier to be applied to the [CompleteState].
 * @param subtitle The subtitle below the title, centered (COUI couiTextBodyXS: 12sp, line spacing
 *   multiplier 1.0). Hidden when null or empty.
 * @param illustration The playable above the text, laid out in the device's
 *   [CompleteStateDefaults.IllustrationSize] box. The device always measures the animation view at
 *   100dp x 100dp, even while [CompleteStateStatus.Default] has no playable set, and always plays
 *   it (`anim_autoPlay` defaults to true); the port only reserves the box when an illustration is
 *   supplied, because the library ships no asset to fill it with.
 * @param colors The [CompleteStateColors] of the [CompleteState].
 */
@Composable
fun CompleteState(
    status: CompleteStateStatus,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    illustration: (@Composable () -> Unit)? = null,
    colors: CompleteStateColors = CompleteStateDefaults.completeStateColors(),
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (illustration != null) {
            // complete_anim: 100dp x 100dp, centered. Its layout_marginStart/End of
            // complete_page_text_padding_hor is symmetric, so it does not offset the centered box.
            Box(
                modifier = Modifier.size(CompleteStateDefaults.IllustrationSize),
                contentAlignment = Alignment.Center,
            ) {
                illustration()
            }
        }
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = CompleteStateDefaults.TitleMarginTop,
                    start = CompleteStateDefaults.TextPaddingHorizontal,
                    end = CompleteStateDefaults.TextPaddingHorizontal,
                ),
            style = CompleteStateDefaults.titleTextStyle(),
            color = colors.titleColor,
            textAlign = TextAlign.Center,
        )
        if (!subtitle.isNullOrEmpty()) {
            Text(
                text = subtitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = CompleteStateDefaults.SubtitleMarginTop,
                        start = CompleteStateDefaults.TextPaddingHorizontal,
                        end = CompleteStateDefaults.TextPaddingHorizontal,
                    ),
                style = CompleteStateDefaults.subtitleTextStyle(),
                color = colors.subtitleColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Contains default values used by [CompleteState]. */
object CompleteStateDefaults {

    /**
     * The size of the illustration box. `complete_anim` is declared as 100dp x 100dp; the playables
     * are 300x300 canvases (`w` / `h` in the JSON) scaled down into it.
     */
    val IllustrationSize = 100.dp

    /**
     * Horizontal padding around the title and subtitle
     * (COUI `complete_page_text_padding_hor`, 8dp).
     */
    val TextPaddingHorizontal = 8.dp

    /**
     * Top margin of the title
     * (COUI `complete_page_text_title_margin_top`, 8dp).
     */
    val TitleMarginTop = 8.dp

    /**
     * Top margin of the subtitle
     * (COUI `complete_page_text_subtitle_margin_top`, 8dp).
     */
    val SubtitleMarginTop = 8.dp

    /**
     * Length of `raw/complete_page_success_anim.json`: 89 frames at 60fps
     * (`op` 89, `fr` 60), 1483ms.
     */
    val SuccessDuration = 1483.milliseconds

    /**
     * Length of `raw/complete_page_fail_anim.json`: 61 frames at 60fps
     * (`op` 61, `fr` 60), 1017ms. The raw is named `complete_page_fail_anim` and titled
     * "定版-开通超时".
     */
    val FailedDuration = 1017.milliseconds

    /**
     * Length of `raw/complete_page_wait_anim.json`: 180 frames at 60fps
     * (`op` 180, `fr` 60), 3000ms.
     */
    val WaitDuration = 3000.milliseconds

    /**
     * How long the playable of [status] runs. [CompleteStateStatus.Default] has no playable, so it
     * is [Duration.ZERO]; none of the raw anims has a night variant.
     */
    fun animationDuration(status: CompleteStateStatus): Duration = when (status) {
        CompleteStateStatus.Default -> Duration.ZERO
        CompleteStateStatus.Success -> SuccessDuration
        CompleteStateStatus.Failed -> FailedDuration
        CompleteStateStatus.Wait -> WaitDuration
    }

    /** The title text style: COUI couiTextHeadlineM (20sp, medium, line spacing multiplier 1.1). */
    @Composable
    fun titleTextStyle(): TextStyle = COUITheme.textStyles.title3.copy(
        lineHeight = 1.1f.em,
        fontWeight = FontWeight.Medium,
    )

    /**
     * The subtitle text style: COUI couiTextBodyXS (12sp, line spacing multiplier 1.0). The theme
     * has no 12sp tier, so its 13sp [COUITheme.textStyles.footnote1] carries the line height under
     * the native size.
     */
    @Composable
    fun subtitleTextStyle(): TextStyle = COUITheme.textStyles.footnote1.copy(
        fontSize = 12.sp,
        lineHeight = 1.0f.em,
    )

    /**
     * The default [CompleteStateColors]: title on couiColorLabelPrimary and subtitle on
     * couiColorLabelSecondary.
     */
    @Composable
    fun completeStateColors(
        titleColor: Color = COUITheme.colorScheme.onSurface,
        subtitleColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
    ): CompleteStateColors = remember(titleColor, subtitleColor) {
        CompleteStateColors(
            titleColor = titleColor,
            subtitleColor = subtitleColor,
        )
    }
}

/**
 * Colors used by a [CompleteState].
 *
 * @param titleColor The color of the title (couiColorLabelPrimary).
 * @param subtitleColor The color of the subtitle (couiColorLabelSecondary).
 */
@Immutable
data class CompleteStateColors(
    val titleColor: Color,
    val subtitleColor: Color,
)
