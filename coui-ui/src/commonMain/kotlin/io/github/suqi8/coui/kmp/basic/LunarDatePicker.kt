// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.LunarDateValue
import io.github.suqi8.coui.kmp.utils.LunarUtil

/**
 * A lunar date picker with COUI style.
 *
 * Three scroll wheels select a Chinese lunar date in a horizontal, center-gravity row. The month
 * wheel shows the lunar month names — 正月 / 二月 / 三月 / 四月 / 五月 / 六月 / 七月 / 八月 / 九月 /
 * 十月 / 冬月 / 腊月 — prefixed with the leap marker 闰 when the year's leap month is selected. The
 * year and day wheels show plain numbers. The day range follows the selected lunar month (29 or 30
 * days, leap month included), and the day is clamped when a shorter month is selected, exactly as
 * [LunarUtil.daysOfALunarMonth] / [LunarUtil.daysOfLeapMonthInLunarYear] report it.
 *
 * The value is a [LunarDateValue] (lunar year/month/day plus a leap-month flag); use
 * [LunarUtil.lunarToSolar] to turn it into a Gregorian [DateValue]. The `calculateLunarByGregorian`
 * conversion lives in [LunarUtil].
 *
 * @param value The currently selected lunar date. Out-of-range fields are coerced.
 * @param onValueChange The callback invoked when the selected lunar date changes.
 * @param modifier The modifier to be applied to the [LunarDatePicker].
 * @param enabled Whether the [LunarDatePicker] is enabled for user interaction.
 * @param yearRange The selectable lunar year range.
 * @param yearLabel Converts a lunar year to its display string.
 * @param monthLabel Converts a lunar month (1..12) and its leap flag to its display string.
 * @param dayLabel Converts a lunar day to its display string.
 * @param yearColumnWidth The width of the year column.
 * @param monthColumnWidth The width of the month column.
 * @param dayColumnWidth The width of the day column.
 * @param colors The [NumberPickerColors] for the wheels.
 * @param textStyle The [TextStyle] for the wheel items.
 * @param itemHeight The height of each wheel item.
 * @param insideMargin The padding around the wheel columns.
 */
@Composable
fun LunarDatePicker(
    value: LunarDateValue,
    onValueChange: (LunarDateValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    yearRange: IntRange = LunarDatePickerDefaults.YearRange,
    yearLabel: (Int) -> String = LunarDatePickerDefaults.YearLabel,
    monthLabel: (Int, Boolean) -> String = LunarDatePickerDefaults.MonthLabel,
    dayLabel: (Int) -> String = LunarDatePickerDefaults.DayLabel,
    yearColumnWidth: Dp = LunarDatePickerDefaults.ColumnWidth,
    monthColumnWidth: Dp = LunarDatePickerDefaults.ColumnWidth,
    dayColumnWidth: Dp = LunarDatePickerDefaults.ColumnWidth,
    colors: NumberPickerColors = NumberPickerDefaults.colors(),
    textStyle: TextStyle = COUITheme.textStyles.title3,
    itemHeight: Dp = NumberPickerDefaults.ItemHeight,
    insideMargin: PaddingValues = LunarDatePickerDefaults.InsideMargin,
) {
    require(yearRange.first <= yearRange.last) {
        "yearRange must not be empty"
    }

    val coercedYear = value.year.coerceIn(yearRange)
    val months = remember(coercedYear) { lunarMonthsOfYear(coercedYear) }
    val monthIndex = months.indexOfFirst { it.first == value.month && it.second == value.isLeapMonth }
        .takeIf { it >= 0 }
        ?: months.indexOfFirst { it.first == value.month }.coerceAtLeast(0)
    val lunarMonth = months[monthIndex].first
    val monthIsLeap = months[monthIndex].second
    val daysInMonth = if (monthIsLeap) {
        LunarUtil.daysOfLeapMonthInLunarYear(coercedYear)
    } else {
        LunarUtil.daysOfALunarMonth(coercedYear, lunarMonth)
    }
    val coercedDay = value.day.coerceIn(1, daysInMonth)

    // Latest state readable from the remember-cached per-column callbacks below.
    val currentValue by rememberUpdatedState(
        LunarDateValue(coercedYear, lunarMonth, coercedDay, monthIsLeap),
    )
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val onYearChange = remember {
        { newYear: Int ->
            val date = currentValue
            val newMonths = lunarMonthsOfYear(newYear)
            val newIndex = newMonths.indexOfFirst {
                it.first == date.month && it.second == date.isLeapMonth
            }.takeIf { it >= 0 }
                ?: newMonths.indexOfFirst { it.first == date.month }.coerceAtLeast(0)
            val (newMonth, newLeap) = newMonths[newIndex]
            val days = if (newLeap) {
                LunarUtil.daysOfLeapMonthInLunarYear(newYear)
            } else {
                LunarUtil.daysOfALunarMonth(newYear, newMonth)
            }
            currentOnValueChange(
                LunarDateValue(newYear, newMonth, date.day.coerceAtMost(days), newLeap),
            )
        }
    }
    val onMonthChange = remember {
        { newIndex: Int ->
            val date = currentValue
            // Recompute from the current year: the remembered lambda outlives a year change.
            val currentMonths = lunarMonthsOfYear(date.year)
            val (newMonth, newLeap) = currentMonths[newIndex]
            val days = if (newLeap) {
                LunarUtil.daysOfLeapMonthInLunarYear(date.year)
            } else {
                LunarUtil.daysOfALunarMonth(date.year, newMonth)
            }
            currentOnValueChange(
                LunarDateValue(date.year, newMonth, date.day.coerceAtMost(days), newLeap),
            )
        }
    }
    val onDayChange = remember {
        { newDay: Int ->
            currentOnValueChange(currentValue.copy(day = newDay))
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(insideMargin),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberPicker(
            value = coercedYear,
            onValueChange = onYearChange,
            modifier = Modifier.width(yearColumnWidth),
            enabled = enabled,
            range = yearRange,
            label = yearLabel,
            wrapAround = true,
            colors = colors,
            textStyle = textStyle,
            itemHeight = itemHeight,
        )
        NumberPicker(
            value = monthIndex,
            onValueChange = onMonthChange,
            modifier = Modifier.width(monthColumnWidth),
            enabled = enabled,
            range = 0..months.lastIndex,
            label = { index ->
                val (month, isLeap) = months[index]
                monthLabel(month, isLeap)
            },
            wrapAround = true,
            colors = colors,
            textStyle = textStyle,
            itemHeight = itemHeight,
        )
        NumberPicker(
            value = coercedDay,
            onValueChange = onDayChange,
            modifier = Modifier.width(dayColumnWidth),
            enabled = enabled,
            range = 1..daysInMonth,
            label = dayLabel,
            wrapAround = true,
            colors = colors,
            textStyle = textStyle,
            itemHeight = itemHeight,
        )
    }
}

/**
 * The lunar months of [year] in calendar order: 1..12, with the leap month (if any) inserted right
 * after the month it repeats. The leap month is the same number with [Pair.second] set.
 */
private fun lunarMonthsOfYear(year: Int): List<Pair<Int, Boolean>> {
    val leap = LunarUtil.leapMonth(year)
    val months = ArrayList<Pair<Int, Boolean>>(LunarDatePickerDefaults.LeapYearMonthCount)
    for (month in 1..LunarDatePickerDefaults.NormalMonthCount) {
        months.add(month to false)
        if (leap == month) {
            months.add(month to true)
        }
    }
    return months
}

/** Contains default values used by [LunarDatePicker]. */
object LunarDatePickerDefaults {

    /** Default selectable lunar year range: 1910..2099. */
    val YearRange: IntRange = 1910..2099

    /**
     * Width of every wheel column (COUI `coui_number_picker_width_middle`, 76dp) — the
     * `coui_lunar_date_picker.xml` layout gives the year, month and day columns the same width.
     */
    val ColumnWidth = 76.dp

    /**
     * Padding around the wheel columns (COUI `coui_number_picker_view_padding_vertical`, 12dp), the
     * `coui_lunar_date_picker.xml` root's `android:paddingVertical`.
     */
    val InsideMargin = PaddingValues(vertical = 12.dp)

    /** Number of months in a lunar year without a leap month (`NORMAL_MONTH_COUNT`, 12). */
    val NormalMonthCount = 12

    /** Number of months in a lunar year with a leap month (`LEAPYEAR_MONTH_COUNT`, 13). */
    val LeapYearMonthCount = 13

    /**
     * The lunar month names behind the COUI `coui_lunar_month` array (which resolves to
     * `lunar_january` .. `lunar_december`): 正月 / 二月 / 三月 / 四月 / 五月 / 六月 / 七月 / 八月 /
     * 九月 / 十月 / 冬月 / 腊月.
     */
    val MonthNames: List<String> = listOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月",
    )

    /** The leap-month marker `coui_lunar_leap_string` (闰). */
    val LeapPrefix = "闰"

    /** Default lunar year label: the plain year number. */
    val YearLabel: (Int) -> String = { it.toString() }

    /**
     * Default lunar month label: the `coui_lunar_month` name, prefixed with [LeapPrefix] when the
     * leap month is selected.
     */
    val MonthLabel: (Int, Boolean) -> String = { month, isLeap ->
        (if (isLeap) LeapPrefix else "") + MonthNames[month - 1]
    }

    /**
     * Default lunar day label: the plain day number. Use [LunarUtil.chineseStringOfALunarDay] for the
     * 初一 / 十五 / 三十 form.
     */
    val DayLabel: (Int) -> String = { it.toString() }
}
