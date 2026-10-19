// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.DatePicker
import io.github.suqi8.coui.kmp.basic.DateValue
import io.github.suqi8.coui.kmp.basic.TimePicker
import io.github.suqi8.coui.kmp.basic.TimeValue

/** Renders the "DatePicker" gallery section content: a card holding a date picker. */
@Composable
fun DatePickerDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        var date by remember { mutableStateOf(DateValue(year = 2026, month = 7, day = 26)) }
        DatePicker(
            value = date,
            onValueChange = { date = it },
        )
    }
}

/** Renders the "TimePicker (24-hour)" gallery section content: a card holding a 24-hour time picker. */
@Composable
fun TimePicker24HourDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        var time by remember { mutableStateOf(TimeValue(hour = 16, minute = 30)) }
        TimePicker(
            value = time,
            onValueChange = { time = it },
            hourUnit = str("h"),
            minuteUnit = str("min"),
        )
    }
}

/** Renders the "TimePicker (12-hour)" gallery section content: a card holding a 12-hour time picker. */
@Composable
fun TimePicker12HourDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        var time by remember { mutableStateOf(TimeValue(hour = 16, minute = 30)) }
        TimePicker(
            value = time,
            onValueChange = { time = it },
            is24Hour = false,
        )
    }
}
