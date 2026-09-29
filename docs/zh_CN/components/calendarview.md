# CalendarView

ColorOS 17 的月历视图：星期表头之下是 7 列日期网格，上方是年月标题与上一月/下一月按钮。对应 `com.coui.appcompat.calendar.COUIDateMonthView`（`COUICalendarDayViewPager` 中的单月页面）以及 `layout/coui_calendar_picker_header_material` 的表头。

`DatePicker` 不是这个组件：ColorOS 两者都有，`DatePicker` 是三轮滚动选择器，本组件是网格。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.CalendarView
import io.github.suqi8.coui.kmp.basic.CalendarViewColors
import io.github.suqi8.coui.kmp.basic.CalendarViewDefaults
import io.github.suqi8.coui.kmp.basic.DateValue
```

## 基本用法

```kotlin
var value by remember { mutableStateOf(DateValue(2026, 9, 29)) }

CalendarView(
    value = value,
    onValueChange = { value = it },
    modifier = Modifier.fillMaxWidth(),
)
```

当前显示的月份是内部状态,与原生日历分页一致:选中日期落到其它月份时显示月份跟随 `value`,而表头按钮只翻页、不改选中值。相邻月份的日期以变暗样式绘制且可点击,对应 `COUIDateMonthView.onTouchEvent` 对 `1..daysInMonth` 之外的日期索引的处理。

## 标题、星期标签与首列

`title` 与 `weekdayLabel` 是 lambda,因为原生用 `java.text.DateFormat.getBestDateTimePattern(locale, "MMMMy")` 与 `android.text.format.DateUtils.getDayOfWeekString(day, 50)` 生成它们,而公共代码无法向平台索取:

```kotlin
CalendarView(
    value = value,
    onValueChange = { value = it },
    title = { year, month -> "$month/$year" },
    weekdayLabel = { dayOfWeek -> listOf("一", "二", "三", "四", "五", "六", "日")[dayOfWeek - 1] },
    firstDayOfWeek = 1,
)
```

`firstDayOfWeek` 采用 ISO-8601(1 = 周一 .. 7 = 周日),默认周一。原生在缺少 `android:firstDayOfWeek` 属性时读取 `Calendar.getInstance().getFirstDayOfWeek()`,即取自设备区域。

`today` 决定哪一天带圆环;传 `null` 时从 `kotlin.time.Clock` 读取当前日期。`yearRange` 是可选年份区间(1900..2100,原生的回退值),位于区间两端的月份会禁用那个会越界的按钮。

## 属性

### CalendarView

| 属性             | 类型                             | 说明                                    | 默认值                        | 必需 |
| ---------------- | -------------------------------- | --------------------------------------- | ----------------------------- | ---- |
| value            | DateValue                        | 选中的日期                              | -                             | 是   |
| onValueChange    | (DateValue) -> Unit              | 用户选日期时的回调                      | -                             | 是   |
| modifier         | Modifier                         | 应用于日历的修饰符                      | Modifier                      | 否   |
| enabled          | Boolean                          | 日历是否响应点击                        | true                          | 否   |
| yearRange        | IntRange                         | 可选的年份区间                          | 1900..2100                    | 否   |
| firstDayOfWeek   | Int                              | 网格首列,ISO-8601                       | 1(周一)                       | 否   |
| today            | DateValue?                       | 视为今天的日期;`null` 时读取时钟        | null                          | 否   |
| colors           | CalendarViewColors               | 网格配色                                | CalendarViewDefaults.colors() | 否   |
| title            | (year: Int, month: Int) -> String | 由年份与月份(1..12)生成表头标题         | "September 2026"              | 否   |
| weekdayLabel     | (dayOfWeek: Int) -> String       | 由 ISO 星期序号生成列标题               | "Mon" .. "Sun"                | 否   |

### CalendarViewColors

| 属性                       | 类型  | 说明                     |
| -------------------------- | ----- | ------------------------ |
| titleColor                 | Color | 年月标题与左右箭头       |
| weekdayColor               | Color | 星期表头                 |
| dayColor                   | Color | 当月日期                 |
| otherMonthDayColor         | Color | 相邻月份的日期           |
| disabledDayColor           | Color | 超出可选区间的日期       |
| todayColor                 | Color | 今天的数字与其圆环       |
| selectedDayContainerColor  | Color | 选中日期的填充           |
| selectedDayContentColor    | Color | 选中日期上的数字         |
| highlightColor             | Color | 日期按下时的反馈         |

### CalendarViewDefaults

| 常量                  | 类型     | 默认值        | COUI 来源                                            |
| --------------------- | -------- | ------------- | ---------------------------------------------------- |
| HeaderHeight          | Dp       | 32.dp         | coui_calendar_picker_header_material layout_height   |
| HeaderPadding         | Dp       | 24.dp         | coui_calendar_picker_header_material paddingStart/End |
| NavigationIconSize    | Dp       | 24.dp         | 表头 ImageButton 的 minWidth/minHeight;两个箭头矢量均为 24x24dp |
| NavigationIconPadding | Dp       | 4.dp          | coui_calendar_picker_header_material padding         |
| NavigationButtonSize  | Dp       | 32.dp         | 图标尺寸 + 两侧内边距                                |
| WeekdayHeight         | Dp       | 40.dp         | coui_calendar_picker_day_of_week_height              |
| DayHeight             | Dp       | 40.dp         | coui_calendar_picker_day_height                      |
| DayWidth              | Dp       | 40.dp         | coui_calendar_picker_day_width                       |
| DayMinColumnPadding   | Dp       | 8.dp          | coui_calendar_picker_day_min_col_padding             |
| HorizontalPadding     | Dp       | 16.dp         | coui_calendar_picker_padding_horizontal              |
| VerticalPadding       | Dp       | 0.dp          | coui_calendar_picker_padding_top                     |
| SelectedCircleRadius  | Dp       | 16.dp         | calendar_picker_current_day_radius / 2               |
| TodayRingWidth        | Dp       | 1.8.dp        | coui_calendar_picker_current_day_stroke_radius       |
| ROW_COUNT             | Int      | 6             | COUIDateMonthView.onMeasure                          |
| YearRange             | IntRange | 1900..2100    | COUICalendarDayPickerView minDate / maxDate 回退值   |
| FIRST_DAY_OF_WEEK     | Int      | 1             | Calendar.getFirstDayOfWeek() 回退值(ISO-8601)        |

#### 方法

| 方法名            | 类型      | 说明                                                                          |
| ----------------- | --------- | ----------------------------------------------------------------------------- |
| titleTextStyle()  | TextStyle | 表头标题(COUITheme.textStyles.button,原生表头 TextView 16sp sans-serif-medium) |
| dayTextStyle()    | TextStyle | 日期数字(COUITheme.textStyles.button,原生 couiTextAppearanceHeadline6 16sp)    |
| weekdayTextStyle()| TextStyle | 星期表头(COUITheme.textStyles.body2 + Medium,原生 couiTextAppearanceButton 14sp) |
| colors()          | CalendarViewColors | 下表列出的主题槽位                                                   |

表头两个按钮绘制的是 `drawable/coui_ic_chevron_start` 与 `drawable/coui_ic_chevron_end`;本移植使用 `COUIIcons.Basic.ChevronStart` / `ChevronEnd`,按原文件 1:1 放入 `coui-ui` 自带的 `icon/basic` 包(24x24dp viewport、1.4dp 圆头描边、autoMirrored,着色 `?attr/couiColorPrimaryNeutral`)。表头 `TextView` 为 `maxLines="2"`,而表头高度固定为 32dp,因此标题换成两行时会溢出该行,而不是把行撑高。

### 颜色

| COUI token                                                       | 取值                  | 用途                         | 移植                          |
| ---------------------------------------------------------------- | --------------------- | ---------------------------- | ----------------------------- |
| attr/couiColorPrimaryNeutral → color/coui_color_primary_neutral  | #e6000000 / #e6ffffff | 表头标题、箭头、默认日期     | onSurface                     |
| attr/couiColorHintNeutral → color/coui_color_hint_neutral        | #42000000 / #4dffffff | 星期表头                     | disabledOnSurface             |
| attr/couiColorDisabledNeutral → color/coui_color_disabled_neutral| #42000000 / #4dffffff | 相邻月份的日期               | disabledOnSurface             |
| attr/couiColorPrimary → color/coui_color_primary                 | 主题主色              | 今天圆环与数字、选中填充     | primary                       |
| attr/couiColorOnPrimary → color/coui_color_on_primary            | #ffffffff             | 选中日期上的数字             | onPrimary                     |
| color/coui_color_control_highlight                               | #7fd0d0d0 / #7f707070 | 日期按下高亮                 | 文件常量,主题无对应槽位       |
| android:attr/disabledAlpha                                       | 0.5                   | 超出年份区间日期的透明度     | onSurface 上的 0.5f           |

表头标题与默认日期颜色来自同一个属性:表头 `TextView` 设置 `textColor=?attr/couiColorPrimaryNeutral`,而日期状态列表 `color/coui_primary_text_inverse_when_activated_material_light` 的默认项同样是 `?attr/couiColorPrimaryNeutral`。

### 文字

| 项目     | 取值                                | 来源                                                |
| -------- | ----------------------------------- | --------------------------------------------------- |
| 标题     | 16sp,sans-serif-medium              | coui_calendar_picker_header_material TextView        |
| 日期数字 | couiTextAppearanceHeadline6,16sp    | TextAppearance.Material.Widget.Calendar.Day          |
| 星期表头 | couiTextAppearanceButton,14sp,medium| TextAppearance.Material.Widget.Calendar.DayOfWeek    |
| 月份标签 | couiTextAppearanceButton,14sp       | TextAppearance.Material.Widget.Calendar.Month(从不绘制) |

主题没有 14sp medium 档位,因此 `weekdayTextStyle()` 用 14sp 的 `body2` 叠加原生字重。

## 行为

- 网格始终预留 6 行:`COUIDateMonthView.onMeasure` 按 `dayHeight * 6 + dayOfWeekHeight + monthHeight` 测量。前导日与本月天数合计超过 35 格的月份按 40dp 行距铺满 6 行;能放进 5 行的月份按 40dp + 8dp(`calendar_picker_day_min_col_padding`)排布,使 5 行仍覆盖同样的 240dp,第 6 行留空。这正是原生的 `fIsNeedSixRows`,即 `rowPitch = dayHeight + (fIsNeedSixRows ? 0 : dayMinColPadding)`。
- 选中日期是以 `couiColorPrimary` 绘制的实心圆,半径为 32dp 的 `calendar_picker_current_day_radius` 的一半——`canvas.drawCircle(x, y, this.H / 2f, …)`,即该 dimen 是直径而非半径。
- 今天是以同样半径、按 `calendar_picker_current_day_stroke_radius`(1.8dp)描边的 `couiColorPrimary` 圆环,数字同色。
- 原生先判断选中日期,因此选中日期上不会出现按下高亮;按下今天时 `coui_color_control_highlight` 圆会覆盖在圆环之上。
- 超出 `yearRange` 的日期按 `?android:attr/disabledAlpha`(0.5)绘制且不响应;相邻月份的日期可点击并触发 `onValueChange`。

## 未移植

- `COUICalendarYearView` 及其与月网格之间按 `anim/coui_calendar_picker_fade_in_material`(280ms,`android:interpolator/linear`)与 `anim/coui_calendar_picker_fade_out_material`(150ms,`anim/coui_ease`)交叉淡入淡出的 `COUICalendarViewAnimator`。这里没有年视图,因此表头中 `COUIRotateView` 里旋转的 `coui_calendar_arrow` 也一并去掉。
- `COUICalendarDayViewPager` 及其自定义 scroller,即月份之间的横向滑动。只有两个表头按钮能切换月份。
- `COUIDateMonthView` 的 `ExploreByTouchHelper` 无障碍树:承载选中状态的每日虚拟节点,以及 `calendar_picker_prev_content` / `calendar_picker_next_content` 字符串。这里改为普通的可选节点。
- `TextAppearance.Material.Widget.Calendar.Month` 的测量过程:原生把月份标签测量进 `mMonthWidth` 却从不绘制,可见标题来自表头 `TextView`。
- `calendar_picker_month_height`(0dp)、`calendar_picker_month_padding_start`(26dp)、`calendar_picker_title_icon_size`(16dp),它们只对年视图箭头有意义。
- `calendar_picker_min_width`(300dp)、`calendar_picker_max_width`(724dp)、`calendar_picker_textinput_margin_left`(32dp):dex 中没有任何日历类读取它们,它们属于日期选择器的文本输入模式。
- 表头尾部按钮的 8dp `android:layout_marginStart`,`RelativeLayout` + `android:layout_alignParentEnd` 下它不产生位置变化。
- 随区域变化的 `firstDayOfWeek` 与 `DateUtils` 星期名,见上文。
