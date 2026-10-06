// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import io.github.suqi8.coui.kmp.basic.DateValue

/**
 * A lunar (Chinese calendar) date.
 *
 * The fields are a lunar year, a lunar month (1 = first month, 12 = last month), a lunar day (1..30)
 * and whether the month is the leap month of that year. A leap month repeats the number of the month it follows
 * (`isLeapMonth = true`), which is why [month] alone does not identify the month.
 *
 * @param year The lunar year (e.g. 2026).
 * @param month The lunar month, 1-based (1..12).
 * @param day The lunar day, 1-based (1..30).
 * @param isLeapMonth Whether [month] is the leap month of [year]. Only one year in a few has one.
 */
data class LunarDateValue(
    val year: Int,
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean = false,
)

/**
 * Chinese lunar calendar conversion.
 *
 * The conversion is table-driven: [LUNAR_INFO] holds one entry per lunar year from [MIN_YEAR] to
 * [MAX_YEAR], and [SOLAR_TERM_DAYS] holds the two solar-term day numbers of each Gregorian month
 * from 1970 to 2036. Both are the standard published lunar-calendar data set — the entries record
 * astronomical facts (which lunar months run 29 or 30 days, which year carries a leap month and
 * which month it is), and the same values appear in any published ephemeris covering these years.
 *
 * Entry points that would take a `java.util.Calendar` or return a `java.util.Date` are not
 * provided; [changeLunarYear] and [getLunarDateString] cover the same behaviour on plain integers.
 */
object LunarUtil {

    /** The earliest supported lunar year. */
    const val MIN_YEAR = 1900

    /** The latest supported lunar year. */
    const val MAX_YEAR = 2100

    /** Year value meaning "ignore the year". */
    const val IGNORED_YEAR = Int.MIN_VALUE

    /** Step one lunar year backwards. */
    const val DECREASE_A_LUNAR_YEAR = -1

    /** Step one lunar year forwards. */
    const val INCREASE_A_LUNAR_YEAR = 1

    /** The leap-month flag value. */
    const val LEAP_MONTH = 0

    /** The normal-month flag value. */
    const val NORMAL_MONTH = 1

    private const val HIGH_BIT_VALUE = 32768
    private const val LOW_BIT_VALUE = 8
    private const val LEAP_MONTH_BIT_FLAG = 15
    private const val LEAP_MONTH_BIT_MASK = 65536
    private const val TWO = 2
    private const val TWELVE = 12
    private const val THIRTY = 30
    private const val LAUN_MONTH_DAYS = 30
    private const val LAUN_LEAP_MONTH_DAYS = 29
    private const val BASE_DAY_COUNT = 348
    private const val SOLAR_TERM_MASK = 65520
    private const val START_DATE_YEAR = 1900
    private const val START_DATE_MONTH = 1
    private const val START_DATE_DAY = 30

    /** Chinese numerals 一 .. 十二, indexed by month number and day unit. */
    private val CHINESE_NUMBER = arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "十二")

    /** The twenty-four solar-term names in traditional characters, in calendar order. */
    private val ALL_TC_SOLAR_TERM_NAMES = arrayOf(
        "小寒", "大寒", "立春", "雨水", "驚蟄", "春分", "清明", "穀雨", "立夏", "小滿", "芒種", "夏至",
        "小暑", "大暑", "立秋", "處暑", "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至",
    )

    /**
     * Lunar-year data, one entry per year from [MIN_YEAR] to [MAX_YEAR]. The low four bits
     * ([LEAP_MONTH_BIT_FLAG], mask `0xF`) are the leap-month number, 0 when the year has no leap
     * month. Bits 15..4 mark which of the twelve months run 30 days rather than 29: month `m` maps
     * to bit `16 - m`. Bit 16 ([LEAP_MONTH_BIT_MASK], `0x10000`) is set when the leap month itself
     * runs 30 days.
     */
    private val LUNAR_INFO = longArrayOf(
        19416L, 19168L, 42352L, 21717L, 53856L, 55632L, 91476L, 22176L, 39632L, 21970L, 19168L, 42422L, 42192L, 53840L, 119381L, 46400L,
        54944L, 44450L, 38320L, 84343L, 18800L, 42160L, 46261L, 27216L, 27968L, 109396L, 11104L, 38256L, 21234L, 18800L, 25958L, 54432L,
        59984L, 92821L, 23248L, 11104L, 100067L, 37600L, 116951L, 51536L, 54432L, 120998L, 46416L, 22176L, 107956L, 9680L, 37584L, 53938L,
        43344L, 46423L, 27808L, 46416L, 86869L, 19872L, 42416L, 83315L, 21168L, 43432L, 59728L, 27296L, 44710L, 43856L, 19296L, 43748L,
        42352L, 21088L, 62051L, 55632L, 23383L, 22176L, 38608L, 19925L, 19152L, 42192L, 54484L, 53840L, 54616L, 46400L, 46752L, 103846L,
        38320L, 18864L, 43380L, 42160L, 45690L, 27216L, 27968L, 44870L, 43872L, 38256L, 19189L, 18800L, 25776L, 29859L, 59984L, 27480L,
        23232L, 43872L, 38613L, 37600L, 51552L, 55636L, 54432L, 55888L, 30034L, 22176L, 43959L, 9680L, 37584L, 51893L, 43344L, 46240L,
        47780L, 44368L, 21977L, 19360L, 42416L, 86390L, 21168L, 43312L, 31060L, 27296L, 44368L, 23378L, 19296L, 42726L, 42208L, 53856L,
        60005L, 54576L, 23200L, 30371L, 38608L, 19195L, 19152L, 42192L, 118966L, 53840L, 54560L, 56645L, 46496L, 22224L, 21938L, 18864L,
        42359L, 42160L, 43600L, 111189L, 27936L, 44448L, 84835L, 37744L, 18936L, 18800L, 25776L, 92326L, 59984L, 27424L, 108228L, 43744L,
        37600L, 53987L, 51552L, 54615L, 54432L, 55888L, 23893L, 22176L, 42704L, 21972L, 21200L, 43448L, 43344L, 46240L, 46758L, 44368L,
        21920L, 43940L, 42416L, 21168L, 45683L, 26928L, 29495L, 27296L, 44368L, 84821L, 19296L, 42352L, 21732L, 53600L, 59752L, 54560L,
        55968L, 92838L, 22224L, 19168L, 43476L, 41680L, 53584L, 62034L, 54560L,
    )

    private val SOLAR_TERM_DAYS = arrayOf(
        intArrayOf(6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 9, 24, 8, 23, 7, 22),
        intArrayOf(6, 21, 4, 19, 6, 21, 5, 21, 6, 22, 6, 22, 8, 23, 8, 24, 8, 24, 9, 24, 8, 23, 8, 22),
        intArrayOf(6, 21, 5, 19, 5, 20, 5, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 21, 7, 23, 8, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 9, 24, 8, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 6, 21, 5, 21, 6, 22, 6, 22, 8, 23, 8, 24, 8, 24, 9, 24, 8, 23, 8, 22),
        intArrayOf(6, 21, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 5, 21, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 8, 24, 8, 23, 7, 22),
        intArrayOf(6, 20, 4, 19, 6, 21, 5, 21, 6, 22, 6, 22, 8, 23, 8, 24, 8, 23, 9, 24, 8, 23, 8, 22),
        intArrayOf(6, 21, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21),
        intArrayOf(5, 20, 4, 19, 5, 21, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 8, 24, 8, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 8, 23, 8, 24, 8, 23, 9, 24, 8, 23, 8, 22),
        intArrayOf(6, 21, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21),
        intArrayOf(5, 20, 4, 19, 5, 20, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 8, 24, 8, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 8, 23, 8, 24, 8, 23, 9, 24, 8, 23, 7, 22),
        intArrayOf(6, 21, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21),
        intArrayOf(5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 21, 7, 23, 8, 23, 8, 23, 8, 24, 8, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 9, 24, 8, 23, 7, 22),
        intArrayOf(6, 21, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21),
        intArrayOf(5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 9, 24, 8, 23, 7, 22),
        intArrayOf(6, 21, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21),
        intArrayOf(5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 21, 7, 23, 8, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 9, 24, 8, 23, 7, 22),
        intArrayOf(6, 21, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21),
        intArrayOf(5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 21, 7, 23, 8, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 9, 24, 8, 23, 7, 22),
        intArrayOf(6, 21, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21),
        intArrayOf(5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 21, 7, 23, 8, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 9, 24, 8, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21),
        intArrayOf(5, 20, 3, 18, 5, 20, 4, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 6, 21, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 8, 24, 8, 23, 7, 22),
        intArrayOf(6, 21, 4, 19, 5, 20, 4, 20, 5, 20, 5, 21, 7, 22, 7, 23, 7, 22, 8, 23, 7, 22, 7, 21),
        intArrayOf(5, 20, 4, 18, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 5, 21, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 8, 24, 8, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 5, 20, 4, 19, 5, 20, 5, 21, 7, 22, 7, 23, 7, 22, 8, 23, 7, 22, 6, 21),
        intArrayOf(5, 20, 3, 18, 5, 20, 4, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21),
        intArrayOf(5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 21, 7, 23, 8, 23, 8, 23, 8, 24, 8, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 5, 20, 4, 19, 5, 20, 5, 21, 7, 22, 7, 23, 7, 22, 8, 23, 7, 22, 6, 21),
        intArrayOf(5, 20, 3, 18, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21),
        intArrayOf(5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 5, 20, 4, 19, 5, 20, 5, 21, 6, 22, 7, 23, 7, 22, 8, 23, 7, 22, 6, 21),
        intArrayOf(5, 20, 3, 18, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21),
        intArrayOf(5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 21, 7, 23, 8, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 5, 20, 4, 19, 5, 20, 5, 21, 6, 22, 7, 22, 7, 22, 8, 23, 7, 22, 6, 21),
        intArrayOf(5, 20, 3, 18, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21),
        intArrayOf(5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 21, 7, 23, 8, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 5, 20, 4, 19, 5, 20, 5, 21, 6, 22, 7, 22, 7, 22, 8, 23, 7, 22, 6, 21),
        intArrayOf(5, 20, 3, 18, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21),
        intArrayOf(5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(5, 20, 4, 19, 6, 21, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22),
        intArrayOf(6, 20, 4, 19, 5, 20, 4, 19, 5, 20, 5, 21, 6, 22, 7, 22, 7, 22, 8, 23, 7, 22, 6, 21),
    )

    /**
     * Converts a Gregorian date to the lunar calendar.
     *
     * The returned array is `[lunarYear, lunarMonth, lunarDay, leapFlag]`: **`leapFlag == 0` means
     * the month is the leap month** and `1` means it is a normal month. Prefer [toLunarDate] for a
     * typed result.
     *
     * @param year The Gregorian year.
     * @param month The Gregorian month, 1-based.
     * @param day The Gregorian day of month.
     * @return `[lunarYear, lunarMonth, lunarDay, leapFlag]`. When [year] is [IGNORED_YEAR] a value
     *   is synthesised from the month and day alone (a partial date).
     */
    fun calculateLunarByGregorian(year: Int, month: Int, day: Int): IntArray {
        val result = intArrayOf(2000, 1, 1, 1)
        if (year == IGNORED_YEAR) {
            result[0] = year
            val offset = month - 1
            result[1] = (offset % TWELVE) + 1
            result[2] = day
            result[3] = if (offset / TWELVE <= 0) 1 else 0
            return result
        }

        // The base date is 1900-01-31; the whole-day delta is computed here with
        // proleptic-Gregorian day numbers.
        val dayDelta = (toEpochDay(year, month, day) - toEpochDay(1900, 1, 31)).toInt()
        var remaining = dayDelta
        var lunarYear = MIN_YEAR
        var daysOfYear = 0
        while (lunarYear < 10000 && remaining > 0) {
            daysOfYear = daysOfLunarYear(lunarYear)
            remaining -= daysOfYear
            lunarYear++
        }
        if (remaining < 0) {
            remaining += daysOfYear
            lunarYear--
        }

        val leapMonth = leapMonth(lunarYear)
        var leapConsumed = 0
        var daysOfMonth = 0
        var lunarMonth = 1
        while (lunarMonth < 13 && remaining > 0) {
            if (leapMonth > 0 && lunarMonth == leapMonth + 1 && leapConsumed == 0) {
                lunarMonth--
                daysOfMonth = daysOfLeapMonthInLunarYear(lunarYear)
                leapConsumed = 1
            } else {
                daysOfMonth = daysOfALunarMonth(lunarYear, lunarMonth)
            }
            remaining -= daysOfMonth
            if (leapConsumed != 0 && lunarMonth == leapMonth + 1) {
                leapConsumed = 0
            }
            lunarMonth++
        }
        if (remaining == 0 && leapMonth > 0 && lunarMonth == leapMonth + 1) {
            if (leapConsumed != 0) {
                leapConsumed = 0
            } else {
                lunarMonth--
                leapConsumed = 1
            }
        }
        if (remaining < 0) {
            remaining += daysOfMonth
            lunarMonth--
        }
        result[0] = lunarYear
        result[1] = lunarMonth
        result[2] = remaining + 1
        result[3] = leapConsumed xor 1
        return result
    }

    /**
     * Converts a Gregorian date to a [LunarDateValue], wrapping [calculateLunarByGregorian] with the
     * leap-flag convention (`leapFlag == 0` means leap).
     */
    fun toLunarDate(year: Int, month: Int, day: Int): LunarDateValue {
        val fields = calculateLunarByGregorian(year, month, day)
        return LunarDateValue(
            year = fields[0],
            month = fields[1],
            day = fields[2],
            isLeapMonth = fields[3] == LEAP_MONTH,
        )
    }

    /**
     * Re-clamps a lunar month and day into [year] and returns the Gregorian date, or null when the
     * lunar date is invalid.
     *
     * @param year The target lunar year.
     * @param month The requested lunar month (1..12).
     * @param day The requested lunar day (1..30).
     * @param isLeapMonth Whether [month] should be the leap month; it falls back to the normal month
     *   when [year] has no leap month at [month].
     */
    fun changeLunarYear(year: Int, month: Int, day: Int, isLeapMonth: Boolean): DateValue? {
        val clampedMonth = clampMonth(year, month, isLeapMonth)
        val clampedDay = clampDay(year, clampedMonth.first, day, clampedMonth.second)
        return lunarToSolar(year, clampedMonth.first, clampedDay, clampedMonth.second)
    }

    /**
     * Formats a lunar day as Chinese numerals: 1 -> "初一" .. 10 -> "初十", 20 -> "二十",
     * 30 -> "三十", and "" for a day above 30.
     */
    fun chineseStringOfALunarDay(day: Int): String {
        val prefixes = arrayOf("初", "十", "廿", "卅")
        val unit = day % 10
        val index = if (unit == 0) 9 else unit - 1
        if (day > THIRTY) return ""
        if (day == 10) return "初十"
        if (day == 20) return "二十"
        if (day == THIRTY) return "三十"
        return prefixes[day / 10] + CHINESE_NUMBER[index]
    }

    /**
     * Clamps [day] to the number of days in the lunar month, using the leap-month length when
     * [isLeapMonth] is set.
     */
    fun clampDay(year: Int, month: Int, day: Int, isLeapMonth: Boolean): Int {
        val days = if (!isLeapMonth) daysOfALunarMonth(year, month) else daysOfLeapMonthInLunarYear(year)
        return if (day > days) days else day
    }

    /** Returns `(month, isLeapMonth)`, keeping the leap flag only when [month] is the year's leap month. */
    private fun clampMonth(year: Int, month: Int, isLeapMonth: Boolean): Pair<Int, Boolean> {
        val flag = if (isLeapMonth && leapMonth(year) == month) 1 else 0
        return month to (flag == 0)
    }

    /**
     * The number of days (29 or 30) in a normal lunar month, read from bit `16 - [month]` of
     * `LUNAR_INFO[year - MIN_YEAR]`.
     */
    fun daysOfALunarMonth(year: Int, month: Int): Int {
        if (year != IGNORED_YEAR && year >= MIN_YEAR) {
            val info = LUNAR_INFO[year - MIN_YEAR]
            if (((LEAP_MONTH_BIT_MASK shr month).toLong() and info) == 0L) {
                return LAUN_LEAP_MONTH_DAYS
            }
        }
        return LAUN_MONTH_DAYS
    }

    /**
     * The number of days (29 or 30) in the year's leap month, or 0 when the year has none.
     */
    fun daysOfLeapMonthInLunarYear(year: Int): Int {
        if (leapMonth(year) != 0) {
            return if ((LUNAR_INFO[year - MIN_YEAR] and LEAP_MONTH_BIT_MASK.toLong()) != 0L) {
                LAUN_MONTH_DAYS
            } else {
                LAUN_LEAP_MONTH_DAYS
            }
        }
        return 0
    }

    /** The total number of days in a lunar year. */
    fun daysOfLunarYear(year: Int): Int {
        if (year == IGNORED_YEAR) return 0
        var days = BASE_DAY_COUNT
        var bit = HIGH_BIT_VALUE
        while (bit > LOW_BIT_VALUE) {
            val index = year - MIN_YEAR
            if (index >= 0 && index < LUNAR_INFO.size && (LUNAR_INFO[index] and bit.toLong()) != 0L) {
                days++
            }
            bit = bit shr 1
        }
        return daysOfLeapMonthInLunarYear(year) + days
    }

    /**
     * [day] advanced past the preceding [month] months, plus the leap month's days when [month]
     * follows it.
     */
    fun getDays(year: Int, month: Int, day: Int, isLeapMonth: Boolean): Int {
        if (year == IGNORED_YEAR) return 0
        var total = day
        var index = 1
        while (index < month) {
            total += daysOfALunarMonth(year, index)
            index++
        }
        val leap = leapMonth(year)
        if (leap < month) {
            return daysOfLeapMonthInLunarYear(year) + total
        }
        if (leap != month || !isLeapMonth) {
            return total
        }
        return daysOfALunarMonth(year, month) + total
    }

    /**
     * Converts a Gregorian date and formats it as e.g. "2026年正月初一" (the leap prefix "闰" is
     * inserted before a leap month).
     */
    fun getLunarDateString(year: Int, month: Int, day: Int): String {
        val fields = calculateLunarByGregorian(year, month, day)
        return lunarDateString(fields[0], fields[1], fields[2], fields[3] == LEAP_MONTH)
    }

    /**
     * Formats a lunar date as e.g. "2026年正月初一"; with [lunarYear] = [IGNORED_YEAR] the year is
     * omitted, producing e.g. "正月初一".
     */
    fun lunarDateString(lunarYear: Int, lunarMonth: Int, lunarDay: Int, isLeapMonth: Boolean): String {
        val builder = StringBuilder()
        if (lunarYear != IGNORED_YEAR) {
            builder.append(lunarYear)
            builder.append("年")
        }
        builder.append(if (isLeapMonth) "闰" else "")
        builder.append(CHINESE_NUMBER[lunarMonth - 1])
        builder.append("月")
        builder.append(chineseStringOfALunarDay(lunarDay))
        return builder.toString()
    }

    /**
     * Returns the Gregorian festival first, then the lunar festival, then the solar term, then the
     * plain lunar month/day. Gregorian Jan/May/Oct 1 and the lunar 1-1 / 5-5 / 8-15 map to "",
     * "春節", "端午", "中秋".
     */
    fun getLunarFestivalChineseString(year: Int, month: Int, day: Int): String {
        val gregFestival = gregFestival(month, day)
        if (!gregFestival.isNullOrEmpty()) return gregFestival
        val fields = calculateLunarByGregorian(year, month, day)
        val lunarFestival = lunarFestival(fields[1], fields[2])
        if (!lunarFestival.isNullOrEmpty()) return lunarFestival
        val solarTerm = getSolarTerm(year, month, day)
        if (solarTerm.isNullOrEmpty()) {
            return lunarNumber(fields[1], fields[2], fields[3] == LEAP_MONTH)
        }
        return solarTerm
    }

    /**
     * The name of the solar term falling on the Gregorian [year]-[month]-[day], or null. The
     * traditional names ([ALL_TC_SOLAR_TERM_NAMES]) are used, and only 1970..2036 is covered.
     */
    fun getSolarTerm(year: Int, month: Int, day: Int): String? {
        val termDays = aMonthSolarTermDays(year, month)
        if (day != termDays[0] && day != termDays[1]) return null
        val names = aMonthSolarTermNames(month)
        if (day == termDays[0]) return names[0]
        if (day == termDays[1]) return names[1]
        return null
    }

    /** The leap month number of [year], or 0 when it has none. */
    fun leapMonth(year: Int): Int {
        if (year in MIN_YEAR..MAX_YEAR) {
            return (LUNAR_INFO[year - MIN_YEAR] and LEAP_MONTH_BIT_FLAG.toLong()).toInt()
        }
        return 0
    }

    /**
     * Converts a lunar date to the Gregorian [DateValue], or null when the lunar date is invalid.
     * The anchor date is 1900-01-30 ([START_DATE_YEAR]-[START_DATE_MONTH]-[START_DATE_DAY]).
     */
    fun lunarToSolar(year: Int, month: Int, day: Int, isLeapMonth: Boolean): DateValue? {
        if (!isLunarDate(year, month, day, isLeapMonth)) return null
        var totalDays = 0
        var index = MIN_YEAR
        while (index < year) {
            totalDays += yearDays(index)
            index++
        }
        val leap = leapMonth(year)
        if (isLeapMonth && leap != month) return null
        var cursor = 1
        val offset: Int
        if (leap == 0 || month < leap || (month == leap && !isLeapMonth)) {
            while (cursor < month) {
                totalDays += daysOfALunarMonth(year, cursor)
                cursor++
            }
            if (day > daysOfALunarMonth(year, month)) return null
            offset = totalDays + day
        } else {
            while (cursor < month) {
                totalDays += daysOfALunarMonth(year, cursor)
                cursor++
            }
            if (month > leap) {
                val leapDays = daysOfLeapMonthInLunarYear(year) + totalDays
                if (day > daysOfALunarMonth(year, month)) return null
                offset = leapDays + day
            } else {
                val normalDays = daysOfALunarMonth(year, month) + totalDays
                if (day > daysOfLeapMonthInLunarYear(year)) return null
                offset = normalDays + day
            }
        }
        return fromEpochDay(toEpochDay(START_DATE_YEAR, START_DATE_MONTH, START_DATE_DAY) + offset)
    }

    /** Whether the fields are a valid supported lunar date. */
    private fun isLunarDate(year: Int, month: Int, day: Int, isLeapMonth: Boolean): Boolean {
        if (year < MIN_YEAR || year > MAX_YEAR || month < 1 || month > TWELVE || day < 1 || day > THIRTY) {
            return false
        }
        return !isLeapMonth || month == leapMonth(year)
    }

    /** The total number of days in a lunar year, including its leap month. */
    private fun yearDays(year: Int): Int {
        var days = BASE_DAY_COUNT
        var bit = HIGH_BIT_VALUE
        while (bit >= LOW_BIT_VALUE) {
            if ((LUNAR_INFO[year - MIN_YEAR] and SOLAR_TERM_MASK.toLong() and bit.toLong()) != 0L) {
                days++
            }
            bit = bit shr 1
        }
        return daysOfLeapMonthInLunarYear(year) + days
    }

    /** The two solar-term day numbers of [month] in [year], or `[0, 0]` outside 1970..2036. */
    private fun aMonthSolarTermDays(year: Int, month: Int): IntArray {
        val index = (month - 1) * TWO
        if (year > 1969 && year < 2037) {
            val row = SOLAR_TERM_DAYS[year - 1970]
            return intArrayOf(row[index], row[index + 1])
        }
        return intArrayOf(0, 0)
    }

    /** The two solar-term names of [month], or two empty strings outside 1..12. */
    private fun aMonthSolarTermNames(month: Int): Array<String> {
        if (month in 1..TWELVE) {
            val index = (month - 1) * TWO
            return arrayOf(ALL_TC_SOLAR_TERM_NAMES[index], ALL_TC_SOLAR_TERM_NAMES[index + 1])
        }
        return arrayOf("", "")
    }

    /** The Gregorian festival for [month]/[day]: "" for Jan/May/Oct 1, otherwise null. */
    private fun gregFestival(month: Int, day: Int): String? {
        if ((month == 1 || month == 5 || month == 10) && day == 1) return ""
        return null
    }

    /** The lunar festival for [month]/[day] ("春節", "端午", "中秋"), or null. */
    private fun lunarFestival(month: Int, day: Int): String? = when {
        month == 1 && day == 1 -> "春節"
        month == 5 && day == 5 -> "端午"
        month == 8 && day == 15 -> "中秋"
        else -> null
    }

    /** The display name of a lunar [month]/[day]: the day name unless it is the first day. */
    private fun lunarNumber(month: Int, day: Int, isLeapMonth: Boolean): String {
        if (day != 1) return chineseStringOfALunarDay(day)
        return if (!isLeapMonth) {
            CHINESE_NUMBER[month - 1] + "月"
        } else {
            "闰" + CHINESE_NUMBER[month - 1]
        }
    }

    /**
     * Days since 1970-01-01 for a proleptic-Gregorian date (Howard Hinnant's `days_from_civil`).
     * This is plain calendar arithmetic, not a lunar algorithm.
     */
    private fun toEpochDay(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era.toLong() * 146097L + doe - 719468L
    }

    /** The inverse of [toEpochDay] (Howard Hinnant's `civil_from_days`). */
    private fun fromEpochDay(epochDay: Long): DateValue {
        val z = epochDay + 719468L
        val era = (if (z >= 0) z else z - 146096L) / 146097L
        val doe = z - era * 146097L
        val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
        val y = yoe + era * 400L
        val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
        val mp = (5L * doy + 2L) / 153L
        val d = (doy - (153L * mp + 2L) / 5L + 1L).toInt()
        val m = (if (mp < 10L) mp + 3L else mp - 9L).toInt()
        return DateValue((if (m <= 2) y + 1L else y).toInt(), m, d)
    }
}
