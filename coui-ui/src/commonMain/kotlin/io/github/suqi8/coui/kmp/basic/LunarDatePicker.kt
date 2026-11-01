// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A lunar date picker with COUI style.
 *
 * Three scroll wheels select a Chinese lunar date in a horizontal, center-gravity row. The month
 * wheel shows the lunar month names — 正月 / 二月 / 三月 / 四月 / 五月 / 六月 / 七月 / 八月 / 九月 /
 * 十月 / 冬月 / 腊月 — prefixed with the leap marker 闰 when the year's leap month is selected. The
 * year wheel shows the plain year and the day wheel the traditional day names (初一 … 三十). The day
 * range follows the selected lunar month (29 or 30 days, leap month included), and the day is
 * clamped when a shorter month is selected, exactly as [LunarUtil.daysOfALunarMonth] /
 * [LunarUtil.daysOfLeapMonthInLunarYear] report it.
 *
 * Two hairlines are drawn across the selected row, `coui_selected_background_radius` above and
 * below the center, exactly as `COUILunarDatePicker.dispatchDraw` paints them.
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
 * @param yearIgnorable Whether the year wheel carries an extra slot that ignores the year
 *   (`couiYearIgnorable`). With no year the month wheel offers all twelve months plus their twelve
 *   leap variants, matching `COUILunarDatePicker.IGNORED_YEAR_MONTH_COUNT`.
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
    yearIgnorable: Boolean = false,
) {
    require(yearRange.first <= yearRange.last) {
        "yearRange must not be empty"
    }

    val yearIgnored = yearIgnorable && value.year == LunarDatePickerDefaults.IgnoredYear
    // Without a year there is no leap-month table to consult; a reference lunar year that has a leap
    // month keeps both the leap option and the longer month reachable.
    val referenceYear = if (yearIgnored) {
        LunarDatePickerDefaults.IgnoredYearReference
    } else {
        value.year.coerceIn(yearRange)
    }
    val months = remember(referenceYear) { lunarMonthsOfYear(referenceYear) }
    val monthIndex = months.indexOfFirst { it.first == value.month && it.second == value.isLeapMonth }
        .takeIf { it >= 0 }
        ?: months.indexOfFirst { it.first == value.month }.coerceAtLeast(0)
    val lunarMonth = months[monthIndex].first
    val monthIsLeap = months[monthIndex].second
    val daysInMonth = if (monthIsLeap) {
        LunarUtil.daysOfLeapMonthInLunarYear(referenceYear)
    } else {
        LunarUtil.daysOfALunarMonth(referenceYear, lunarMonth)
    }
    val coercedDay = value.day.coerceIn(1, daysInMonth)

    // Latest state readable from the remember-cached per-column callbacks below.
    val currentValue by rememberUpdatedState(
        LunarDateValue(
            year = if (yearIgnored) LunarDatePickerDefaults.IgnoredYear else referenceYear,
            month = lunarMonth,
            day = coercedDay,
            isLeapMonth = monthIsLeap,
        ),
    )
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val onYearChange = remember {
        { newYear: Int ->
            val date = currentValue
            if (newYear == NumberPickerDefaults.IgnoredValue) {
                currentOnValueChange(date.copy(year = LunarDatePickerDefaults.IgnoredYear))
            } else {
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
    }
    val onMonthChange = remember {
        { newIndex: Int ->
            val date = currentValue
            // Recompute from the current value: the remembered lambda outlives a year change.
            val refYear = if (date.year == LunarDatePickerDefaults.IgnoredYear) {
                LunarDatePickerDefaults.IgnoredYearReference
            } else {
                date.year
            }
            val (newMonth, newLeap) = lunarMonthsOfYear(refYear)[newIndex]
            val days = if (newLeap) {
                LunarUtil.daysOfLeapMonthInLunarYear(refYear)
            } else {
                LunarUtil.daysOfALunarMonth(refYear, newMonth)
            }
            currentOnValueChange(
                date.copy(month = newMonth, day = date.day.coerceAtMost(days), isLeapMonth = newLeap),
            )
        }
    }
    val onDayChange = remember {
        { newDay: Int ->
            currentOnValueChange(currentValue.copy(day = newDay))
        }
    }

    // Without a year the month wheel lists every month twice: the twelve plain months, then the
    // twelve leap variants (COUILunarDatePicker.IGNORED_YEAR_MONTH_COUNT = 24).
    val monthCount = if (yearIgnored) {
        LunarDatePickerDefaults.IgnoredYearMonthCount
    } else {
        months.size
    }
    val monthWheelIndex = if (yearIgnored) {
        if (monthIsLeap) LunarDatePickerDefaults.NormalMonthCount + lunarMonth - 1 else lunarMonth - 1
    } else {
        monthIndex
    }
    val monthWheelLabel: (Int) -> String = if (yearIgnored) {
        { index ->
            if (index < LunarDatePickerDefaults.NormalMonthCount) {
                monthLabel(index + 1, false)
            } else {
                monthLabel(index - LunarDatePickerDefaults.NormalMonthCount + 1, true)
            }
        }
    } else {
        { index ->
            val (month, isLeap) = months[index]
            monthLabel(month, isLeap)
        }
    }

    Box(modifier) {
        PickerSelectionDividers(Modifier.matchParentSize())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(insideMargin),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberPicker(
                value = if (yearIgnored) NumberPickerDefaults.IgnoredValue else referenceYear,
                onValueChange = onYearChange,
                modifier = Modifier.width(yearColumnWidth),
                enabled = enabled,
                range = yearRange,
                label = yearLabel,
                wrapAround = true,
                colors = colors,
                textStyle = textStyle,
                itemHeight = itemHeight,
                ignorable = yearIgnorable,
                longPressUpdateInterval = LunarDatePickerDefaults.YearLongPressInterval,
            )
            NumberPicker(
                value = monthWheelIndex,
                onValueChange = onMonthChange,
                modifier = Modifier.width(monthColumnWidth),
                enabled = enabled,
                range = 0..monthCount - 1,
                label = monthWheelLabel,
                wrapAround = true,
                colors = colors,
                textStyle = textStyle,
                itemHeight = itemHeight,
                longPressUpdateInterval = LunarDatePickerDefaults.MonthLongPressInterval,
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
                longPressUpdateInterval = LunarDatePickerDefaults.DayLongPressInterval,
            )
        }
    }
}

/** Binary-compatibility bridge for the [LunarDatePicker] overload that predates `yearIgnorable`. */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds yearIgnorable.",
    level = DeprecationLevel.HIDDEN,
)
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
    LunarDatePicker(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        yearRange = yearRange,
        yearLabel = yearLabel,
        monthLabel = monthLabel,
        dayLabel = dayLabel,
        yearColumnWidth = yearColumnWidth,
        monthColumnWidth = monthColumnWidth,
        dayColumnWidth = dayColumnWidth,
        colors = colors,
        textStyle = textStyle,
        itemHeight = itemHeight,
        insideMargin = insideMargin,
    )
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

    /** Months offered when the year is ignored (`IGNORED_YEAR_MONTH_COUNT`, 24). */
    val IgnoredYearMonthCount = 24

    /** The lunar year value that means "no year selected" (`COUILunarDatePicker.IGNORED_YEAR`). */
    val IgnoredYear = Int.MIN_VALUE

    /**
     * The lunar year used to size a month when the year is ignored. It has a leap month, so both the
     * leap option and the longer month stay reachable while no year is chosen.
     */
    val IgnoredYearReference = 2001

    /** How often a held year row keeps stepping (`LONGPRESS_UPDATE_INTERVAL`, 100ms). */
    val YearLongPressInterval: Duration = 100.milliseconds

    /** How often a held month row keeps stepping (`MONTH_LONGPRESS_UPDATE_INTERVAL`, 200ms). */
    val MonthLongPressInterval: Duration = 200.milliseconds

    /** How often a held day row keeps stepping (`LONGPRESS_UPDATE_INTERVAL`, 100ms). */
    val DayLongPressInterval: Duration = 100.milliseconds

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
     * Default lunar day label: the traditional day name, 初一 / 十五 / 三十, as
     * `COUILunarDatePicker` fills the day wheel with `COUILunarUtil.chineseStringOfALunarDay`.
     */
    val DayLabel: (Int) -> String = { LunarUtil.chineseStringOfALunarDay(it) }
}
