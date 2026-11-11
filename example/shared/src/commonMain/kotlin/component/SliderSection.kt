// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.Slider
import io.github.suqi8.coui.kmp.basic.SliderDefaults
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.VerticalSlider
import io.github.suqi8.coui.kmp.preference.RangeSliderPreference
import io.github.suqi8.coui.kmp.preference.SliderPreference

/**
 * Renders the "Slider" gallery section content: horizontal, range and vertical sliders that share one
 * option card for the enabled state and the key-point marks.
 */
@Composable
fun SliderDemo() {
    val enabled = remember { mutableStateOf(true) }
    val keyPoints = remember { mutableStateOf(true) }

    DemoOptions(
        DemoToggle("Enabled", enabled),
        DemoToggle("Show key points", keyPoints),
    )
    HorizontalSliders(enabled.value, keyPoints.value)
    SmallTitle(str("RangeSlider"))
    RangeSliders(enabled.value, keyPoints.value)
    SmallTitle(str("VerticalSlider"))
    VerticalSliders(enabled.value, keyPoints.value)
}

@Composable
private fun HorizontalSliders(enabled: Boolean, showKeyPoints: Boolean) {
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        var sliderValue by remember { mutableFloatStateOf(0.3f) }
        SliderPreference(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            title = str("Normal"),
            valueText = "${(sliderValue * 100).toInt()}%",
            enabled = enabled,
        )
        var stepsValue by remember { mutableFloatStateOf(5f) }
        SliderPreference(
            value = stepsValue,
            onValueChange = { stepsValue = it },
            title = str("Steps"),
            valueText = "${stepsValue.toInt()}/8",
            valueRange = 0f..8f,
            steps = 7,
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            showKeyPoints = showKeyPoints,
            enabled = enabled,
        )
        var customKeyPointsValue by remember { mutableFloatStateOf(25f) }
        SliderPreference(
            value = customKeyPointsValue,
            onValueChange = { customKeyPointsValue = it },
            title = str("Custom Key Points"),
            valueText = "${customKeyPointsValue.toInt()}%",
            valueRange = 0f..100f,
            showKeyPoints = showKeyPoints,
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            keyPoints = listOf(0f, 25f, 50f, 75f, 100f),
            enabled = enabled,
        )
        var secondaryValue by remember { mutableFloatStateOf(0.25f) }
        Slider(
            value = secondaryValue,
            onValueChange = { secondaryValue = it },
            secondaryProgress = 0.75f,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun RangeSliders(enabled: Boolean, showKeyPoints: Boolean) {
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        var rangeValue by remember { mutableStateOf(0.2f..0.8f) }
        RangeSliderPreference(
            value = rangeValue,
            onValueChange = { rangeValue = it },
            title = str("Range"),
            valueText = "${(rangeValue.start * 100).toInt()}% - ${(rangeValue.endInclusive * 100).toInt()}%",
            enabled = enabled,
        )
        var rangeStepsValue by remember { mutableStateOf(2f..8f) }
        RangeSliderPreference(
            value = rangeStepsValue,
            onValueChange = { rangeStepsValue = it },
            title = str("Steps"),
            valueText = "${rangeStepsValue.start.toInt()} - ${rangeStepsValue.endInclusive.toInt()}",
            valueRange = 0f..8f,
            steps = 7,
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            showKeyPoints = showKeyPoints,
            enabled = enabled,
        )
        var customRangeValue by remember { mutableStateOf(20f..80f) }
        RangeSliderPreference(
            value = customRangeValue,
            onValueChange = { customRangeValue = it },
            title = str("Custom Key Points"),
            valueText = "${customRangeValue.start.toInt()}% - ${customRangeValue.endInclusive.toInt()}%",
            valueRange = 0f..100f,
            showKeyPoints = showKeyPoints,
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            keyPoints = listOf(0f, 20f, 40f, 60f, 80f, 100f),
            enabled = enabled,
        )
    }
}

@Composable
private fun VerticalSliders(enabled: Boolean, showKeyPoints: Boolean) {
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var normalValue by remember { mutableFloatStateOf(0.3f) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                VerticalSlider(
                    value = normalValue,
                    onValueChange = { normalValue = it },
                    secondaryProgress = 0.8f,
                    enabled = enabled,
                    modifier = Modifier.size(25.dp, 160.dp),
                )
                Text(
                    text = str("Normal\n{}%", (normalValue * 100).toInt()),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            var stepsValue by remember { mutableFloatStateOf(5f) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                VerticalSlider(
                    value = stepsValue,
                    onValueChange = { stepsValue = it },
                    valueRange = 0f..6f,
                    steps = 5,
                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                    showKeyPoints = showKeyPoints,
                    enabled = enabled,
                    modifier = Modifier.size(25.dp, 160.dp),
                )
                Text(
                    text = str("Steps\n{}/6", stepsValue.toInt()),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            var customValue by remember { mutableFloatStateOf(50f) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                VerticalSlider(
                    value = customValue,
                    onValueChange = { customValue = it },
                    valueRange = 0f..100f,
                    showKeyPoints = showKeyPoints,
                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                    keyPoints = listOf(0f, 25f, 50f, 75f, 100f),
                    enabled = enabled,
                    modifier = Modifier.size(25.dp, 160.dp),
                )
                Text(
                    text = str("Custom\n{}%", customValue.toInt()),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
