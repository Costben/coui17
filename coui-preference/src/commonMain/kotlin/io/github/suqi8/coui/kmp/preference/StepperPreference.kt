// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.BasicComponentColors
import io.github.suqi8.coui.kmp.basic.BasicComponentDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.Stepper
import io.github.suqi8.coui.kmp.basic.StepperColors
import io.github.suqi8.coui.kmp.basic.StepperDefaults

/**
 * A preference row with a [Stepper] in its widget frame, mirroring COUIStepperPreference.
 *
 * The row carries no widget of its own: `COUIStepperPreference` inflates
 * `res/layout/coui_preference_widget_stepper_view.xml` (a bare `COUIStepperView` with
 * `style="?couiStepperViewStyle"`) into the standard `coui_preference` row's `widget_frame`, and
 * `coui_stepper_view.xml` inside that view is what [Stepper] already ports (32dp circular buttons,
 * a 44dp indicator, 12dp gaps). The stepper reads `couiMaximum` / `couiMinimum` / `couiDefStep` /
 * `couiUnit`; `couiUnit` is the step increment, not a label, which is why it is exposed here as
 * [step] alone.
 *
 * @param value The current value of the [StepperPreference].
 * @param onValueChange The callback invoked with the new value when the user steps up or down.
 * @param title The title of the [StepperPreference].
 * @param modifier The modifier to be applied to the [StepperPreference].
 * @param titleColor The color of the title.
 * @param summary The summary of the [StepperPreference].
 * @param summaryColor The color of the summary.
 * @param minValue The minimum value the stepper can reach (COUI `couiMinimum`, `ObservableStep.MIN_VALUE`).
 * @param maxValue The maximum value the stepper can reach (COUI `couiMaximum`, `ObservableStep.MAX_VALUE`).
 * @param step The increment applied on each button press (COUI `couiUnit`).
 * @param startAction The [Composable] content on the start side of the [StepperPreference].
 * @param endActions The [Composable] content on the end side of the [StepperPreference], placed
 *   before the stepper.
 * @param stepperColors The [StepperColors] of the stepper.
 * @param bottomAction The [Composable] content at the bottom of the [StepperPreference].
 * @param insideMargin The margin inside the [StepperPreference].
 * @param cardListPosition The row's position inside its card group. Rounded outer edges receive extra padding.
 * @param holdDownState Used to determine whether it is in the pressed state.
 * @param enabled Whether the [StepperPreference] is clickable.
 */
@Composable
@NonRestartableComposable
fun StepperPreference(
    value: Int,
    onValueChange: (Int) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    minValue: Int = StepperPreferenceDefaults.MinValue,
    maxValue: Int = StepperPreferenceDefaults.MaxValue,
    step: Int = StepperPreferenceDefaults.Step,
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    stepperColors: StepperColors = StepperDefaults.stepperColors(),
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    BasicComponent(
        modifier = modifier,
        insideMargin = insideMargin,
        cardListPosition = cardListPosition,
        title = title,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        startAction = startAction,
        endActions = {
            endActions?.let {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .weight(1f, fill = false),
                ) {
                    it()
                }
            }
            Stepper(
                value = value,
                onValueChange = { currentOnValueChange(it) },
                minValue = minValue,
                maxValue = maxValue,
                step = step,
                enabled = enabled,
                colors = stepperColors,
            )
        },
        bottomAction = bottomAction,
        holdDownState = holdDownState,
        enabled = enabled,
    )
}

object StepperPreferenceDefaults {

    /**
     * `ObservableStep.MIN_VALUE` (-999), the default of `COUIStepperPreference`'s
     * `couiMinimum`.
     */
    val MinValue = -999

    /**
     * `ObservableStep.MAX_VALUE` (9999), the default of `COUIStepperPreference`'s
     * `couiMaximum`.
     */
    val MaxValue = 9999

    /**
     * The default of `COUIStepperPreference`'s `couiUnit` attribute: 1. `COUIStepperView.plus()`
     * adds it and `minus()` subtracts it, so it is the step increment.
     */
    val Step = 1
}
