# LunarDatePicker

`LunarDatePicker` is a basic interactive component in COUI used for selecting a Chinese lunar date with three vertical scroll wheels (year / month / day).

The month wheel shows the lunar month names — 正月 / 二月 / 三月 / 四月 / 五月 / 六月 / 七月 / 八月 / 九月 / 十月 / 冬月 / 腊月 — prefixed with the leap marker 闰 when the year's leap month is selected. The year and day wheels show plain numbers. The day range follows the selected lunar month (29 or 30 days, leap month included) and the day is clamped when a shorter month is selected.

The value is a `LunarDateValue` (lunar year / month / day plus a leap-month flag). The conversion lives in `LunarUtil`, a table-driven implementation using `LUNAR_INFO` for 1900..2100 and `SOLAR_TERM_DAYS` for 1970..2036, so no third-party lunar library is required. Use `LunarUtil.lunarToSolar` to turn a `LunarDateValue` into a Gregorian `DateValue`.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.LunarDatePicker
import io.github.suqi8.coui.kmp.basic.LunarDatePickerDefaults
import io.github.suqi8.coui.kmp.utils.LunarDateValue
import io.github.suqi8.coui.kmp.utils.LunarUtil
```

## Basic Usage

```kotlin
var date by remember { mutableStateOf(LunarDateValue(year = 2026, month = 8, day = 20)) }

LunarDatePicker(
    value = date,
    onValueChange = { date = it }
)
```

## Component States

### Disabled State

```kotlin
LunarDatePicker(
    value = date,
    onValueChange = { date = it },
    enabled = false
)
```

## Properties

### LunarDatePicker Properties

| Property Name    | Type                    | Description                                            | Default Value                       | Required |
| ---------------- | ----------------------- | ------------------------------------------------------ | ----------------------------------- | -------- |
| value            | LunarDateValue          | Currently selected lunar date. Out-of-range fields coerced | -                               | Yes      |
| onValueChange    | (LunarDateValue) -> Unit | Callback invoked when the selected lunar date changes | -                                   | Yes      |
| modifier         | Modifier                | Modifier applied to the picker                         | Modifier                            | No       |
| enabled          | Boolean                 | Whether the picker is enabled for user interaction     | true                                | No       |
| yearRange        | IntRange                | The selectable lunar year range                        | LunarDatePickerDefaults.YearRange   | No       |
| yearLabel        | (Int) -> String         | Converts a lunar year to its display string            | LunarDatePickerDefaults.YearLabel   | No       |
| monthLabel       | (Int, Boolean) -> String | Converts a lunar month and its leap flag to its display string | LunarDatePickerDefaults.MonthLabel | No |
| dayLabel         | (Int) -> String         | Converts a lunar day to its display string             | LunarDatePickerDefaults.DayLabel    | No       |
| yearColumnWidth  | Dp                      | Width of the year column                               | LunarDatePickerDefaults.ColumnWidth | No       |
| monthColumnWidth | Dp                      | Width of the month column                              | LunarDatePickerDefaults.ColumnWidth | No       |
| dayColumnWidth   | Dp                      | Width of the day column                                | LunarDatePickerDefaults.ColumnWidth | No       |
| colors           | NumberPickerColors      | Color configuration of the wheels                      | NumberPickerDefaults.colors()       | No       |
| textStyle        | TextStyle               | Text style for the wheel items                         | COUITheme.textStyles.title3         | No       |
| itemHeight       | Dp                      | The height of each wheel item                          | NumberPickerDefaults.ItemHeight     | No       |
| insideMargin     | PaddingValues           | Padding around the wheel columns                       | LunarDatePickerDefaults.InsideMargin | No      |

### LunarDateValue Class

| Property Name | Type    | Description                                                            |
| ------------- | ------- | ---------------------------------------------------------------------- |
| year          | Int     | The lunar year (e.g. 2026)                                             |
| month         | Int     | The lunar month, 1-based (1 = first month)                             |
| day           | Int     | The lunar day, 1-based (1..30)                                         |
| isLeapMonth   | Boolean | Whether the month is the leap month of the year                        |

### LunarDatePickerDefaults Object

| Property Name      | Type             | Description                              | Default Value |
| ------------------ | ---------------- | ---------------------------------------- | ------------- |
| YearRange          | IntRange         | Default selectable lunar year range      | 1910..2099    |
| ColumnWidth        | Dp               | Width of every wheel column              | 76.dp         |
| InsideMargin       | PaddingValues    | Padding around the wheel columns         | PaddingValues(vertical = 12.dp) |
| NormalMonthCount   | Int              | Months in a year without a leap month    | 12            |
| LeapYearMonthCount | Int              | Months in a year with a leap month       | 13            |
| MonthNames         | `List<String>`     | The `coui_lunar_month` names             | 正月 .. 腊月   |
| LeapPrefix         | String           | The `coui_lunar_leap_string` marker      | "闰"          |
| YearLabel          | (Int) -> String  | Plain lunar year number                  | `{ it.toString() }` |
| MonthLabel         | (Int, Boolean) -> String | Lunar month name with the leap prefix | 正月 .. 腊月 with 闰 |
| DayLabel           | (Int) -> String  | Plain day number                         | `{ it.toString() }` |

## LunarUtil

| Function | Description |
| -------- | ----------- |
| `calculateLunarByGregorian(year, month, day): IntArray` | Gregorian -> lunar, returning `[year, month, day, leapFlag]`; `leapFlag == 0` means leap |
| `toLunarDate(year, month, day): LunarDateValue` | The typed form of the above |
| `lunarToSolar(year, month, day, isLeapMonth): DateValue?` | Lunar -> Gregorian, or null when invalid |
| `changeLunarYear(year, month, day, isLeapMonth): DateValue?` | Re-clamps a month / day into a year and converts |
| `chineseStringOfALunarDay(day): String` | 初一 .. 三十 |
| `leapMonth(year): Int` | The year's leap month, or 0 |
| `daysOfALunarMonth(year, month): Int` | Days in a normal lunar month (29 / 30) |
| `daysOfLeapMonthInLunarYear(year): Int` | Days in the year's leap month, or 0 |
| `getLunarDateString(year, month, day): String` | e.g. "2026年正月初一" |
| `lunarDateString(year, month, day, isLeapMonth): String` | Formats a lunar date directly |
| `getLunarFestivalChineseString(year, month, day): String` | Festival / solar term / lunar day name |
| `getSolarTerm(year, month, day): String?` | The solar term on a Gregorian date |

## Behaviour and Tokens

- **Year range.** 1910 to 2099, matching `LunarDatePickerDefaults.YearRange`.
- **Column sizes.** Every column is `LunarDatePickerDefaults.ColumnWidth` (76dp) wide; item height is `NumberPickerDefaults.ItemHeight` (45dp).
- **Wheel padding.** `LunarDatePickerDefaults.InsideMargin` (12dp vertical).
- **Month names.** `LunarDatePickerDefaults.MonthNames`: 正月 / 二月 / 三月 / 四月 / 五月 / 六月 / 七月 / 八月 / 九月 / 十月 / 冬月 / 腊月. The leap marker is `LunarDatePickerDefaults.LeapPrefix` (闰).
- **Day count.** A lunar month has 29 or 30 days, read from `daysOfALunarMonth` / `daysOfLeapMonthInLunarYear`; a year with a leap month offers 13 month entries (`LEAPYEAR_MONTH_COUNT`), otherwise 12 (`NORMAL_MONTH_COUNT`).
