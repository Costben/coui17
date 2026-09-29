# CalendarView

The ColorOS 17 month calendar: a weekday header row over a 7-column grid of day cells, with the month/year title and the previous/next buttons above it. It mirrors `com.coui.appcompat.calendar.COUIDateMonthView` — one month page of the `COUICalendarDayViewPager` — together with the header of `layout/coui_calendar_picker_header_material`.

`DatePicker` is *not* this component: ColorOS ships both, and `DatePicker` is the three-wheel scroll picker while this is the grid.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.CalendarView
import io.github.suqi8.coui.kmp.basic.CalendarViewColors
import io.github.suqi8.coui.kmp.basic.CalendarViewDefaults
import io.github.suqi8.coui.kmp.basic.DateValue
```

## Basic Usage

```kotlin
var value by remember { mutableStateOf(DateValue(2026, 9, 29)) }

CalendarView(
    value = value,
    onValueChange = { value = it },
    modifier = Modifier.fillMaxWidth(),
)
```

The displayed month is internal state, as in the native pager: it follows `value` when the selection moves to another month, and the two header buttons move it without touching the selection. Days of the neighbouring months are drawn dimmed and are clickable, which is what `COUIDateMonthView.onTouchEvent` does for a day index outside `1..daysInMonth`.

## Title, Weekday Labels and the First Column

`title` and `weekdayLabel` are lambdas, because the native formats them with `java.text.DateFormat.getBestDateTimePattern(locale, "MMMMy")` and `android.text.format.DateUtils.getDayOfWeekString(day, 50)`, neither of which common code can ask the platform for:

```kotlin
CalendarView(
    value = value,
    onValueChange = { value = it },
    title = { year, month -> "$month/$year" },
    weekdayLabel = { dayOfWeek -> listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")[dayOfWeek - 1] },
    firstDayOfWeek = 1,
)
```

`firstDayOfWeek` is ISO-8601 (1 = Monday .. 7 = Sunday) and defaults to Monday. The native reads `Calendar.getInstance().getFirstDayOfWeek()` when the `android:firstDayOfWeek` attribute is absent, i.e. from the device locale.

`today` decides which day gets the ring; leaving it `null` reads the current date from `kotlin.time.Clock`. `yearRange` is the selectable window (1900..2100, the native fallback), and a month at the ends of it greys out the button that would leave it.

## Properties

### CalendarView

| Property        | Type                           | Description                                                              | Default Value             | Required |
| --------------- | ------------------------------ | ------------------------------------------------------------------------ | ------------------------- | -------- |
| value           | DateValue                      | The selected date                                                        | -                         | Yes      |
| onValueChange   | (DateValue) -> Unit            | Callback with the date the user picked                                   | -                         | Yes      |
| modifier        | Modifier                       | Modifier applied to the calendar                                         | Modifier                  | No       |
| enabled         | Boolean                        | Whether the calendar reacts to clicks                                    | true                      | No       |
| yearRange       | IntRange                       | The selectable years                                                     | 1900..2100                | No       |
| firstDayOfWeek  | Int                            | First column of the grid, ISO-8601                                       | 1 (Monday)                | No       |
| today           | DateValue?                     | The date drawn as today; `null` reads the clock                          | null                      | No       |
| colors          | CalendarViewColors             | The palette of the grid                                                  | CalendarViewDefaults.colors() | No   |
| title           | (year: Int, month: Int) -> String | Converts a year and a month (1..12) to the header title               | "September 2026"          | No       |
| weekdayLabel    | (dayOfWeek: Int) -> String     | Converts an ISO day of week to a column header                           | "Mon" .. "Sun"            | No       |

### CalendarViewColors

| Property                    | Type  | Description                                                       |
| --------------------------- | ----- | ----------------------------------------------------------------- |
| titleColor                  | Color | Month/year title and the chevron buttons                          |
| weekdayColor                | Color | Column headers                                                    |
| dayColor                    | Color | Days of the displayed month                                       |
| otherMonthDayColor          | Color | Days of the neighbouring months                                   |
| disabledDayColor            | Color | Days outside the selectable range                                 |
| todayColor                  | Color | Today's number and its ring                                       |
| selectedDayContainerColor   | Color | Fill of the selected day                                          |
| selectedDayContentColor     | Color | Number on the selected day                                        |
| highlightColor              | Color | Press feedback on a day                                           |

### CalendarViewDefaults

| Constant              | Type       | Default Value | COUI Source                                          |
| --------------------- | ---------- | ------------- | ---------------------------------------------------- |
| HeaderHeight          | Dp         | 32.dp         | coui_calendar_picker_header_material layout_height   |
| HeaderPadding         | Dp         | 24.dp         | coui_calendar_picker_header_material paddingStart/End |
| NavigationIconSize    | Dp         | 24.dp         | header ImageButton minWidth/minHeight; both chevron vectors are 24x24dp |
| NavigationIconPadding | Dp         | 4.dp          | coui_calendar_picker_header_material padding         |
| NavigationButtonSize  | Dp         | 32.dp         | icon size + padding on both sides                    |
| WeekdayHeight         | Dp         | 40.dp         | coui_calendar_picker_day_of_week_height              |
| DayHeight             | Dp         | 40.dp         | coui_calendar_picker_day_height                      |
| DayWidth              | Dp         | 40.dp         | coui_calendar_picker_day_width                       |
| DayMinColumnPadding   | Dp         | 8.dp          | coui_calendar_picker_day_min_col_padding             |
| HorizontalPadding     | Dp         | 16.dp         | coui_calendar_picker_padding_horizontal              |
| VerticalPadding       | Dp         | 0.dp          | coui_calendar_picker_padding_top                     |
| SelectedCircleRadius  | Dp         | 16.dp         | calendar_picker_current_day_radius / 2               |
| TodayRingWidth        | Dp         | 1.8.dp        | coui_calendar_picker_current_day_stroke_radius       |
| ROW_COUNT             | Int        | 6             | COUIDateMonthView.onMeasure                          |
| YearRange             | IntRange   | 1900..2100    | COUICalendarDayPickerView minDate / maxDate fallback |
| FIRST_DAY_OF_WEEK     | Int        | 1             | Calendar.getFirstDayOfWeek() fallback (ISO-8601)     |

#### Methods

| Method Name       | Type      | Description                                                                        |
| ----------------- | --------- | ---------------------------------------------------------------------------------- |
| titleTextStyle()  | TextStyle | Header title (COUITheme.textStyles.button, COUI header TextView 16sp sans-serif-medium) |
| dayTextStyle()    | TextStyle | Day number (COUITheme.textStyles.button, COUI couiTextAppearanceHeadline6 16sp)    |
| weekdayTextStyle()| TextStyle | Column header (COUITheme.textStyles.body2 + Medium, COUI couiTextAppearanceButton 14sp) |
| colors()          | CalendarViewColors | The theme slots listed below                                              |

The header's two buttons draw `drawable/coui_ic_chevron_start` and `drawable/coui_ic_chevron_end`; this port uses `COUIIcons.Basic.ChevronStart` / `ChevronEnd`, taken 1:1 from those files into `coui-ui`'s own `icon/basic` package (24x24dp viewport, 1.4dp round-cap stroke, auto-mirrored, tinted `?attr/couiColorPrimaryNeutral`). The header `TextView` is `maxLines="2"`, and the header keeps its fixed 32dp `layout_height`, so a wrapped title overflows the row rather than growing it.

### Colours

| COUI token                                                                  | Value                 | Used for                              | Port                     |
| --------------------------------------------------------------------------- | --------------------- | ------------------------------------- | ------------------------ |
| attr/couiColorPrimaryNeutral → color/coui_color_primary_neutral             | #e6000000 / #e6ffffff | Header title, chevrons, default day   | onSurface                |
| attr/couiColorHintNeutral → color/coui_color_hint_neutral                   | #42000000 / #4dffffff | Weekday column headers                | disabledOnSurface        |
| attr/couiColorDisabledNeutral → color/coui_color_disabled_neutral           | #42000000 / #4dffffff | Neighbouring months' days             | disabledOnSurface        |
| attr/couiColorPrimary → color/coui_color_primary                            | theme primary         | Today's ring and number, selected fill | primary                 |
| attr/couiColorOnPrimary → color/coui_color_on_primary                       | #ffffffff             | Number on the selected day            | onPrimary                |
| color/coui_color_control_highlight                                          | #7fd0d0d0 / #7f707070 | Day press highlight                   | file constant, no theme slot |
| android:attr/disabledAlpha                                                  | 0.5                   | Alpha of a day outside the year range | 0.5f on onSurface        |

The header title and the default day colour come from the same attribute: the header `TextView` sets `textColor=?attr/couiColorPrimaryNeutral`, and the default item of the day state list `color/coui_primary_text_inverse_when_activated_material_light` is `?attr/couiColorPrimaryNeutral` too.

### Text

| Item           | Value                                    | Source                                                     |
| -------------- | ---------------------------------------- | ---------------------------------------------------------- |
| Title          | 16sp, sans-serif-medium                  | coui_calendar_picker_header_material TextView              |
| Day number     | couiTextAppearanceHeadline6, 16sp        | TextAppearance.Material.Widget.Calendar.Day                |
| Weekday header | couiTextAppearanceButton, 14sp, medium   | TextAppearance.Material.Widget.Calendar.DayOfWeek          |
| Month label    | couiTextAppearanceButton, 14sp           | TextAppearance.Material.Widget.Calendar.Month (never drawn) |

The theme has no 14sp medium tier, so `weekdayTextStyle()` is the 14sp `body2` with the native weight applied.

## Behavior

- The grid always reserves six day rows: `COUIDateMonthView.onMeasure` measures `dayHeight * 6 + dayOfWeekHeight + monthHeight`. A month whose leading days plus its own days exceed 35 cells fills all six rows at the 40dp pitch; one that fits in five rows is pitched at 40dp + 8dp (`calendar_picker_day_min_col_padding`) so that five rows still cover the same 240dp and the sixth row is left empty. That is the native's `fIsNeedSixRows` flag, `rowPitch = dayHeight + (fIsNeedSixRows ? 0 : dayMinColPadding)`.
- The selected day is a filled circle of `couiColorPrimary` drawn at half the 32dp `calendar_picker_current_day_radius` — `canvas.drawCircle(x, y, this.H / 2f, …)`, so the dimen is the diameter, not the radius.
- Today is a ring of the same radius stroked at `calendar_picker_current_day_stroke_radius` (1.8dp) in `couiColorPrimary`, with its number in the same colour.
- The native checks the selected day first, so the press highlight never shows on it, and pressing today draws the `coui_color_control_highlight` circle over its ring.
- Days outside `yearRange` are drawn at `?android:attr/disabledAlpha` (0.5) and do not react; days of the neighbouring months are clickable and follow `onValueChange`.

## Not Ported

- `COUICalendarYearView` and the `COUICalendarViewAnimator` that cross-fades between it and the month grid on `anim/coui_calendar_picker_fade_in_material` (280ms, `android:interpolator/linear`) and `anim/coui_calendar_picker_fade_out_material` (150ms, `anim/coui_ease`). There is no year grid here, so the header's rotating `coui_calendar_arrow` inside a `COUIRotateView` goes with it.
- `COUICalendarDayViewPager` and its custom scroller, i.e. the horizontal swipe between months. Only the two header buttons change the month.
- The `ExploreByTouchHelper` accessibility tree of `COUIDateMonthView`: per-day virtual nodes carrying the checked state, plus the `calendar_picker_prev_content` / `calendar_picker_next_content` strings. The cells are ordinary selectable nodes instead.
- The `TextAppearance.Material.Widget.Calendar.Month` measurement pass: the native measures the month label into `mMonthWidth` but never draws it, the visible title comes from the header `TextView`.
- `calendar_picker_month_height` (0dp), `calendar_picker_month_padding_start` (26dp) and `calendar_picker_title_icon_size` (16dp), all of them only meaningful for the year-view arrow.
- `calendar_picker_min_width` (300dp), `calendar_picker_max_width` (724dp) and `calendar_picker_textinput_margin_left` (32dp): no calendar class in the dex reads them, they belong to the date-picker text-input mode.
- The 8dp `android:layout_marginStart` of the header's trailing button, which `RelativeLayout` + `android:layout_alignParentEnd` resolves to no positional change.
- The locale-dependent `firstDayOfWeek` and the `DateUtils` weekday names; see above.
