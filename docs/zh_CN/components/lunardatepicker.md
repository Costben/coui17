# LunarDatePicker

`LunarDatePicker` 是 COUI 中的基础交互组件，通过三列垂直滚轮（年 / 月 / 日）选择农历日期。

月列显示农历月名 —— 正月 / 二月 / 三月 / 四月 / 五月 / 六月 / 七月 / 八月 / 九月 / 十月 / 冬月 / 腊月 —— 选中该年闰月时前缀 闰。年列与日列显示纯数字。日列范围随所选农历月自动联动（29 或 30 天，含闰月），切换到天数更少的月份时日值自动收敛。

取值为 `LunarDateValue`（农历年 / 月 / 日 + 闰月标志）。换算位于 `LunarUtil`，采用表驱动实现 —— 1900..2100 的 `LUNAR_INFO` 表与 1970..2036 的 `SOLAR_TERM_DAYS` 表 —— 不依赖任何第三方农历库。用 `LunarUtil.lunarToSolar` 可把 `LunarDateValue` 转成公历 `DateValue`。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.LunarDatePicker
import io.github.suqi8.coui.kmp.basic.LunarDatePickerDefaults
import io.github.suqi8.coui.kmp.utils.LunarDateValue
import io.github.suqi8.coui.kmp.utils.LunarUtil
```

## 基本用法

```kotlin
var date by remember { mutableStateOf(LunarDateValue(year = 2026, month = 8, day = 20)) }

LunarDatePicker(
    value = date,
    onValueChange = { date = it }
)
```

## 组件状态

### 禁用状态

```kotlin
LunarDatePicker(
    value = date,
    onValueChange = { date = it },
    enabled = false
)
```

## 属性

### LunarDatePicker 属性

| 属性名           | 类型                     | 说明                                 | 默认值                              | 是否必填 |
| ---------------- | ------------------------ | ------------------------------------ | ----------------------------------- | -------- |
| value            | LunarDateValue           | 当前选中的农历日期，越界字段会收敛   | -                                   | 是       |
| onValueChange    | (LunarDateValue) -> Unit | 所选农历日期变化时的回调             | -                                   | 是       |
| modifier         | Modifier                 | 应用于选择器的修饰符                 | Modifier                            | 否       |
| enabled          | Boolean                  | 是否可交互                           | true                                | 否       |
| yearRange        | IntRange                 | 可选农历年份范围                     | LunarDatePickerDefaults.YearRange   | 否       |
| yearLabel        | (Int) -> String          | 农历年 -> 显示字符串                 | LunarDatePickerDefaults.YearLabel   | 否       |
| monthLabel       | (Int, Boolean) -> String | 农历月与闰月标志 -> 显示字符串       | LunarDatePickerDefaults.MonthLabel  | 否       |
| dayLabel         | (Int) -> String          | 农历日 -> 显示字符串                 | LunarDatePickerDefaults.DayLabel    | 否       |
| yearColumnWidth  | Dp                       | 年列宽度                             | LunarDatePickerDefaults.ColumnWidth | 否       |
| monthColumnWidth | Dp                       | 月列宽度                             | LunarDatePickerDefaults.ColumnWidth | 否       |
| dayColumnWidth   | Dp                       | 日列宽度                             | LunarDatePickerDefaults.ColumnWidth | 否       |
| colors           | NumberPickerColors       | 滚轮配色                             | NumberPickerDefaults.colors()       | 否       |
| textStyle        | TextStyle                | 滚轮文字样式                         | COUITheme.textStyles.title3         | 否       |
| itemHeight       | Dp                       | 单个滚轮项的高度                     | NumberPickerDefaults.ItemHeight     | 否       |
| insideMargin     | PaddingValues            | 滚轮区域的内边距                     | LunarDatePickerDefaults.InsideMargin | 否      |

### LunarDateValue 数据类

| 属性名      | 类型    | 说明                       |
| ----------- | ------- | -------------------------- |
| year        | Int     | 农历年（如 2026）          |
| month       | Int     | 农历月，1 起（1 = 正月）   |
| day         | Int     | 农历日，1 起（1..30）      |
| isLeapMonth | Boolean | 该月是否为这一年的闰月     |

### LunarDatePickerDefaults 对象

| 属性名             | 类型             | 说明                         | 默认值        |
| ------------------ | ---------------- | ---------------------------- | ------------- |
| YearRange          | IntRange         | 默认可选农历年份范围         | 1910..2099    |
| ColumnWidth        | Dp               | 各滚轮列宽度                 | 76.dp         |
| InsideMargin       | PaddingValues    | 滚轮区域内边距               | PaddingValues(vertical = 12.dp) |
| NormalMonthCount   | Int              | 无闰月年份的月数             | 12            |
| LeapYearMonthCount | Int              | 有闰月年份的月数             | 13            |
| MonthNames         | `List<String>`     | `coui_lunar_month` 月名      | 正月 .. 腊月   |
| LeapPrefix         | String           | `coui_lunar_leap_string` 标记 | "闰"         |
| YearLabel          | (Int) -> String  | 纯年份数字                   | `{ it.toString() }` |
| MonthLabel         | (Int, Boolean) -> String | 带闰月前缀的农历月名 | 正月 .. 腊月，闰月前缀 闰 |
| DayLabel           | (Int) -> String  | 纯日期数字                   | `{ it.toString() }` |

## LunarUtil

| 函数 | 说明 |
| ---- | ---- |
| `calculateLunarByGregorian(year, month, day): IntArray` | 公历 -> 农历，返回 `[年, 月, 日, 闰标志]`；`闰标志 == 0` 表示闰月 |
| `toLunarDate(year, month, day): LunarDateValue` | 上述结果的类型化形式 |
| `lunarToSolar(year, month, day, isLeapMonth): DateValue?` | 农历 -> 公历，非法时返回 null |
| `changeLunarYear(year, month, day, isLeapMonth): DateValue?` | 重新收敛月/日到某年并换算 |
| `chineseStringOfALunarDay(day): String` | 初一 .. 三十 |
| `leapMonth(year): Int` | 该年的闰月，无则 0 |
| `daysOfALunarMonth(year, month): Int` | 平月天数（29 / 30） |
| `daysOfLeapMonthInLunarYear(year): Int` | 闰月天数，无则 0 |
| `getLunarDateString(year, month, day): String` | 如「2026年正月初一」 |
| `lunarDateString(year, month, day, isLeapMonth): String` | 直接格式化农历日期 |
| `getLunarFestivalChineseString(year, month, day): String` | 节日 / 节气 / 农历日名 |
| `getSolarTerm(year, month, day): String?` | 公历日期对应的节气 |

## 行为与 Token

- **年份范围**：1910 到 2099，对应 `LunarDatePickerDefaults.YearRange`。
- **列尺寸**：三列同为 `LunarDatePickerDefaults.ColumnWidth`（76dp）；项高取 `NumberPickerDefaults.ItemHeight`（45dp）。
- **滚轮内边距**：`LunarDatePickerDefaults.InsideMargin`（垂直 12dp）。
- **月名**：`LunarDatePickerDefaults.MonthNames`：正月 / 二月 / 三月 / 四月 / 五月 / 六月 / 七月 / 八月 / 九月 / 十月 / 冬月 / 腊月。闰月标记为 `LunarDatePickerDefaults.LeapPrefix`（闰）。
- **天数**：农历月为 29 或 30 天，取自 `daysOfALunarMonth` / `daysOfLeapMonthInLunarYear`；有闰月的年份月列有 13 项（`LEAPYEAR_MONTH_COUNT`），否则 12 项（`NORMAL_MONTH_COUNT`）。
