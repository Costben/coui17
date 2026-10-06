// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import io.github.suqi8.coui.kmp.basic.DateValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins [LunarUtil]'s conversion against independently published dates.
 *
 * The expected values below are public calendar facts — the Gregorian date of Chinese New Year
 * (lunar 1/1), the years that carry a leap month and which month it is, and the Gregorian dates of
 * the Mid-Autumn and Dragon Boat festivals. They are taken from published ephemerides rather than
 * from this project, so the test checks the conversion tables instead of restating them.
 */
class LunarUtilTest {

    @Test
    fun chineseNewYearIsTheFirstDayOfTheFirstLunarMonth() {
        val newYearDates = listOf(
            DateValue(1990, 1, 27),
            DateValue(2000, 2, 5),
            DateValue(2010, 2, 14),
            DateValue(2020, 1, 25),
            DateValue(2021, 2, 12),
            DateValue(2022, 2, 1),
            DateValue(2023, 1, 22),
            DateValue(2024, 2, 10),
            DateValue(2025, 1, 29),
            DateValue(2026, 2, 17),
            DateValue(2027, 2, 6),
            DateValue(2028, 1, 26),
            DateValue(2029, 2, 13),
            DateValue(2030, 2, 3),
        )

        for (date in newYearDates) {
            val lunar = LunarUtil.toLunarDate(date.year, date.month, date.day)
            assertEquals(date.year, lunar.year, "lunar year of Chinese New Year $date")
            assertEquals(1, lunar.month, "lunar month of Chinese New Year $date")
            assertEquals(1, lunar.day, "lunar day of Chinese New Year $date")
            assertFalse(lunar.isLeapMonth, "Chinese New Year is never a leap month: $date")
        }
    }

    @Test
    fun leapMonthsMatchThePublishedAlmanac() {
        val leapMonths = mapOf(
            2020 to 4,
            2023 to 2,
            2025 to 6,
            2028 to 5,
            2031 to 3,
            2033 to 11,
            2036 to 6,
        )
        val yearHasNoLeapMonth = listOf(2021, 2022, 2024, 2026, 2027, 2029, 2030)

        for ((year, expected) in leapMonths) {
            assertEquals(expected, LunarUtil.leapMonth(year), "leap month of $year")
        }
        for (year in yearHasNoLeapMonth) {
            assertEquals(0, LunarUtil.leapMonth(year), "$year has no leap month")
        }
    }

    @Test
    fun festivalsFallOnTheExpectedLunarDays() {
        val midAutumn = listOf(
            DateValue(2023, 9, 29),
            DateValue(2024, 9, 17),
            DateValue(2025, 10, 6),
            DateValue(2026, 9, 25),
        )
        for (date in midAutumn) {
            val lunar = LunarUtil.toLunarDate(date.year, date.month, date.day)
            assertEquals(8, lunar.month, "Mid-Autumn is lunar 8/15: $date")
            assertEquals(15, lunar.day, "Mid-Autumn is lunar 8/15: $date")
        }

        val dragonBoat = listOf(
            DateValue(2024, 6, 10),
            DateValue(2025, 5, 31),
            DateValue(2026, 6, 19),
        )
        for (date in dragonBoat) {
            val lunar = LunarUtil.toLunarDate(date.year, date.month, date.day)
            assertEquals(5, lunar.month, "Dragon Boat is lunar 5/5: $date")
            assertEquals(5, lunar.day, "Dragon Boat is lunar 5/5: $date")
        }
    }

    /** Every Gregorian day of the covered range must survive a lunar round trip unchanged. */
    @Test
    fun lunarRoundTripIsTheIdentity() {
        for (year in 2019..2031) {
            for (month in 1..12) {
                for (day in 1..daysInGregorianMonth(year, month)) {
                    val lunar = LunarUtil.toLunarDate(year, month, day)
                    val back = LunarUtil.lunarToSolar(
                        lunar.year,
                        lunar.month,
                        lunar.day,
                        lunar.isLeapMonth,
                    )
                    assertNotNull(back, "lunar ${lunar} of $year-$month-$day must convert back")
                    assertEquals(
                        DateValue(year, month, day),
                        back,
                        "round trip of ${year}-$month-$day produced $back via $lunar",
                    )
                }
            }
        }
    }

    @Test
    fun theLeapMonthIsReportedAsLeapAndTheRegularMonthIsNot() {
        // 2020 has a leap 4th month: it begins 2020-05-23, one month after the regular 4th month.
        val regularFourth = LunarUtil.toLunarDate(2020, 4, 26)
        val leapFourth = LunarUtil.toLunarDate(2020, 5, 23)

        assertEquals(4, regularFourth.month)
        assertFalse(regularFourth.isLeapMonth, "the regular 4th month of 2020 is not the leap month")

        assertEquals(4, leapFourth.month)
        assertTrue(leapFourth.isLeapMonth, "2020-05-23 begins the leap 4th month")
    }

    @Test
    fun lunarDatesAreFormattedWithChineseNumerals() {
        assertEquals("2026年一月初一", LunarUtil.getLunarDateString(2026, 2, 17))
        assertEquals("2025年八月十五", LunarUtil.getLunarDateString(2025, 10, 6))
        assertEquals("初十", LunarUtil.chineseStringOfALunarDay(10))
        assertEquals("二十", LunarUtil.chineseStringOfALunarDay(20))
        assertEquals("三十", LunarUtil.chineseStringOfALunarDay(30))
        assertEquals("廿三", LunarUtil.chineseStringOfALunarDay(23))
    }

    @Test
    fun daysOfALunarMonthIsTwentyNineOrThirty() {
        for (year in 2019..2031) {
            for (month in 1..12) {
                val days = LunarUtil.daysOfALunarMonth(year, month)
                assertTrue(days == 29 || days == 30, "$year-$month had $days days")
            }
            val yearLength = LunarUtil.daysOfLunarYear(year)
            assertTrue(yearLength in 353..385, "lunar year $year was $yearLength days long")
        }
    }

    private fun daysInGregorianMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        else -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    }
}
