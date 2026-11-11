// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.DatePicker
import io.github.suqi8.coui.kmp.basic.DateValue
import io.github.suqi8.coui.kmp.basic.LunarDatePicker
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TimePicker
import io.github.suqi8.coui.kmp.basic.TimeValue
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.LunarDateValue

/** Renders the "DatePicker" gallery section content: a date picker that the option switches to the lunar calendar. */
@Composable
fun DatePickerDemo() {
    val lunar = remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(DateValue(year = 2026, month = 7, day = 26)) }
    var lunarDate by remember { mutableStateOf(LunarDateValue(2025, 1, 1)) }

    DemoOptions(DemoToggle("Lunar calendar", lunar))
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        if (lunar.value) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LunarDatePicker(
                    value = lunarDate,
                    onValueChange = { lunarDate = it },
                )
                Text(
                    text = "${lunarDate.year}-${lunarDate.month}-${lunarDate.day}",
                    modifier = Modifier.padding(top = 8.dp),
                    color = COUITheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        } else {
            DatePicker(
                value = date,
                onValueChange = { date = it },
            )
        }
    }
}

/** Renders the "TimePicker" gallery section content: a time picker whose clock format and unit labels come from the options. */
@Composable
fun TimePickerDemo() {
    val is24Hour = remember { mutableStateOf(true) }
    val showUnits = remember { mutableStateOf(true) }
    var time by remember { mutableStateOf(TimeValue(hour = 16, minute = 30)) }

    DemoOptions(
        DemoToggle("24-hour format", is24Hour),
        DemoToggle("Show unit labels", showUnits),
    )
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        TimePicker(
            value = time,
            onValueChange = { time = it },
            is24Hour = is24Hour.value,
            hourUnit = if (showUnits.value) str("h") else "",
            minuteUnit = if (showUnits.value) str("min") else "",
        )
    }
}
