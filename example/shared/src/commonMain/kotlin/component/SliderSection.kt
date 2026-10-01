// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
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
import io.github.suqi8.coui.kmp.basic.SliderDefaults
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.VerticalSlider
import io.github.suqi8.coui.kmp.preference.RangeSliderPreference
import io.github.suqi8.coui.kmp.preference.SliderPreference

fun LazyListScope.sliderSection() {
    item(key = "slider") {
        SmallTitle(text = "Slider")
        SliderDemo()

        // RangeSlider
        SmallTitle(text = "RangeSlider")
        RangeSliderDemo()

        // VerticalSlider
        SmallTitle(text = "VerticalSlider")
        VerticalSliderDemo()
    }
}

/** Renders the "Slider" gallery section content: a card of slider preferences. */
@Composable
fun SliderDemo() {
    Card(
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
        )
        var stepsValue by remember { mutableFloatStateOf(100f) }
        SliderPreference(
            value = stepsValue,
            onValueChange = { stepsValue = it },
            title = str("Steps"),
            valueText = "${stepsValue.toInt()}/200",
            valueRange = 0f..200f,
            steps = 199,
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
        )
        var stepsWithKeyPointsValue by remember { mutableFloatStateOf(5f) }
        SliderPreference(
            value = stepsWithKeyPointsValue,
            onValueChange = { stepsWithKeyPointsValue = it },
            title = str("Steps with Key Points"),
            valueText = "${stepsWithKeyPointsValue.toInt()}/8",
            valueRange = 0f..8f,
            steps = 7,
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            showKeyPoints = true,
        )
        var customKeyPointsValue by remember { mutableFloatStateOf(25f) }
        SliderPreference(
            value = customKeyPointsValue,
            onValueChange = { customKeyPointsValue = it },
            title = str("Custom Key Points"),
            valueText = "${customKeyPointsValue.toInt()}%",
            valueRange = 0f..100f,
            showKeyPoints = true,
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            keyPoints = listOf(0f, 25f, 50f, 75f, 100f),
        )
        val disabledValue by remember { mutableFloatStateOf(0.7f) }
        SliderPreference(
            value = disabledValue,
            onValueChange = {},
            title = str("Disabled"),
            valueText = "${(disabledValue * 100).toInt()}%",
            enabled = false,
        )
    }
}

/** Renders the "RangeSlider" gallery section content: a card of range-slider preferences. */
@Composable
fun RangeSliderDemo() {
    Card(
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
        )
        var rangeStepsValue by remember { mutableStateOf(2f..8f) }
        RangeSliderPreference(
            value = rangeStepsValue,
            onValueChange = { rangeStepsValue = it },
            title = str("Range with Key Points"),
            valueText = "${rangeStepsValue.start.toInt()} - ${rangeStepsValue.endInclusive.toInt()}",
            valueRange = 0f..8f,
            steps = 7,
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            showKeyPoints = true,
        )
        var customRangeValue by remember { mutableStateOf(20f..80f) }
        RangeSliderPreference(
            value = customRangeValue,
            onValueChange = { customRangeValue = it },
            title = str("Custom Range Points"),
            valueText = "${customRangeValue.start.toInt()}% - ${customRangeValue.endInclusive.toInt()}%",
            valueRange = 0f..100f,
            showKeyPoints = true,
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            keyPoints = listOf(0f, 20f, 40f, 60f, 80f, 100f),
        )
        var disabledRangeValue by remember { mutableStateOf(0.3f..0.7f) }
        RangeSliderPreference(
            value = disabledRangeValue,
            onValueChange = {},
            title = str("Disabled"),
            valueText = "${(disabledRangeValue.start * 100).toInt()}% - ${(disabledRangeValue.endInclusive * 100).toInt()}%",
            enabled = false,
        )
    }
}

/** Renders the "VerticalSlider" gallery section content: a row of vertical sliders with their labels. */
@Composable
fun VerticalSliderDemo() {
    Card(
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
            var verticalValue1 by remember { mutableFloatStateOf(0.3f) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                VerticalSlider(
                    value = verticalValue1,
                    onValueChange = { verticalValue1 = it },
                    modifier = Modifier.size(25.dp, 160.dp),
                )
                Text(
                    text = str("Normal\n{}%", (verticalValue1 * 100).toInt()),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            var verticalValue2 by remember { mutableFloatStateOf(5f) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                VerticalSlider(
                    value = verticalValue2,
                    onValueChange = { verticalValue2 = it },
                    valueRange = 0f..6f,
                    steps = 5,
                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                    modifier = Modifier.size(25.dp, 160.dp),
                )
                Text(
                    text = str("Steps\n{}/6", verticalValue2.toInt()),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            var verticalValue3 by remember { mutableFloatStateOf(5f) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                VerticalSlider(
                    value = verticalValue3,
                    onValueChange = { verticalValue3 = it },
                    valueRange = 0f..6f,
                    steps = 5,
                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                    showKeyPoints = true,
                    modifier = Modifier.size(25.dp, 160.dp),
                )
                Text(
                    text = str("Points\n{}/6", verticalValue3.toInt()),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            var verticalValue4 by remember { mutableFloatStateOf(50f) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                VerticalSlider(
                    value = verticalValue4,
                    onValueChange = { verticalValue4 = it },
                    valueRange = 0f..100f,
                    showKeyPoints = true,
                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                    keyPoints = listOf(0f, 25f, 50f, 75f, 100f),
                    modifier = Modifier.size(25.dp, 160.dp),
                )
                Text(
                    text = str("Custom\n{}%", verticalValue4.toInt()),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val disabledVerticalValue by remember { mutableFloatStateOf(0.7f) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                VerticalSlider(
                    value = disabledVerticalValue,
                    onValueChange = {},
                    enabled = false,
                    modifier = Modifier.size(25.dp, 160.dp),
                )
                Text(
                    text = str("Disabled\n{}%", (disabledVerticalValue * 100).toInt()),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
