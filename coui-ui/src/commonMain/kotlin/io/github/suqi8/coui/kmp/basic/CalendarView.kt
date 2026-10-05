// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.ChevronEnd
import io.github.suqi8.coui.kmp.icon.basic.ChevronStart
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.time.Clock

/**
 * The ColorOS 17 month calendar: a weekday header row over a 7-column grid of day cells, with the
 * month/year title and the previous/next month buttons above it.
 *
 * It mirrors `com.coui.appcompat.calendar.COUIDateMonthView` (one month page, the view the
 * `COUICalendarDayViewPager` pages through) plus the header of `coui_calendar_picker_header_material`
 * and the day-picker chrome of `COUICalendarDayPickerView`. `basic/DatePicker.kt` is a three-wheel
 * scroll picker and is *not* this component: ColorOS ships both, and this is the grid one.
 *
 * Geometry, all of it from `Clock.apk`:
 * - the header is [CalendarViewDefaults.HeaderHeight] tall with [CalendarViewDefaults.HeaderPadding]
 *   on each side; the title is centred across the whole header and the two chevron buttons sit at the
 *   ends. The title is `TextView` with `maxLines="2"` and `sans-serif-medium` at 16sp, and the header
 *   keeps its fixed height, so a wrapped title overflows the row rather than growing it;
 * - the two buttons draw `COUIIcons.Basic.ChevronStart` / `ChevronEnd`, taken 1:1 from the
 *   `drawable/coui_ic_chevron_start` / `coui_ic_chevron_end` sources of the header's `ImageButton`s:
 *   24x24dp, auto-mirrored, a 1.4dp round-cap stroke, tinted `?attr/couiColorPrimaryNeutral` like
 *   the title;
 * - the weekday row is [CalendarViewDefaults.WeekdayHeight] tall and the day cells are
 *   [CalendarViewDefaults.DayWidth] by [CalendarViewDefaults.DayHeight];
 * - the grid always reserves [CalendarViewDefaults.ROW_COUNT] day rows
 *   (`COUIDateMonthView.onMeasure` = `dayHeight * 6 + dayOfWeekHeight + monthHeight`). A month whose
 *   leading days plus its own days exceed 35 cells fills all six rows at the 40dp pitch; one that
 *   fits in five rows is pitched at 40dp + 8dp (`calendar_picker_day_min_col_padding`) so that five
 *   rows still cover the same 240dp, and the sixth row is left empty. That is the native's
 *   `fIsNeedSixRows` flag: `rowPitch = dayHeight + (fIsNeedSixRows ? 0 : dayMinColPadding)`;
 * - the selected day is a filled circle of [CalendarViewDefaults.SelectedCircleRadius] in
 *   `couiColorPrimary`, drawn at half the 32dp `calendar_picker_current_day_radius` dimen
 *   (`canvas.drawCircle(x, y, this.H / 2f, …)`);
 * - today is a ring of the same radius stroked at `calendar_picker_current_day_stroke_radius`
 *   (1.8dp) in `couiColorPrimary`, with its number in the same colour.
 *
 * The displayed month is internal state, as in the native pager: it follows [value] when the
 * selection moves to another month, and the previous/next buttons move it without touching the
 * selection. The neighbouring months' days are drawn in the grid (dimmed) and are clickable, which is
 * what `COUIDateMonthView.onTouchEvent` does for a day index outside `1..daysInMonth`.
 *
 * Not ported:
 * - `COUICalendarYearView` and the `COUICalendarViewAnimator` that cross-fades between it and the
 *   month grid on `anim/coui_calendar_picker_fade_in_material` (280ms, `android:interpolator/linear`)
 *   and `anim/coui_calendar_picker_fade_out_material` (150ms, `anim/coui_ease`). There is no year
 *   grid here, so the header's rotating `coui_calendar_arrow` inside a `COUIRotateView` goes with it;
 * - `COUICalendarDayViewPager` and its custom scroller, i.e. the horizontal swipe between months.
 *   Only the two header buttons change the month;
 * - the `ExploreByTouchHelper` accessibility tree of `COUIDateMonthView` (per-day virtual nodes
 *   carrying the checked state, and the `calendar_picker_prev_content` /
 *   `calendar_picker_next_content` strings). The cells are ordinary selectable nodes instead;
 * - the `TextAppearance.Material.Widget.Calendar.Month` measurement pass: the native measures the
 *   month label into `mMonthWidth` but never draws it, the visible title comes from the header
 *   `TextView`;
 * - `calendar_picker_month_height` (0dp), `calendar_picker_month_padding_start` (26dp) and
 *   `calendar_picker_title_icon_size` (16dp), all of them only meaningful for the year-view arrow;
 * - `calendar_picker_min_width` (300dp), `calendar_picker_max_width` (724dp) and
 *   `calendar_picker_textinput_margin_left` (32dp): no calendar class in the dex reads them, they
 *   belong to the date-picker text-input mode;
 * - the 8dp `android:layout_marginStart` of the header's trailing button, which
 *   `RelativeLayout` + `android:layout_alignParentEnd` resolves to no positional change.
 *
 * @param value The selected date. Fields outside [yearRange] are coerced.
 * @param onValueChange Called with the date the user picked.
 * @param modifier The modifier to be applied to the calendar.
 * @param enabled Whether the calendar reacts to clicks at all.
 * @param yearRange The selectable years. The native default is 1900..2100
 *   (`COUICalendarDayPickerView`'s fallback `minDate` / `maxDate`); a month at the ends of the range
 *   greys out the button that would leave it.
 * @param firstDayOfWeek The first column of the grid, ISO-8601: 1 = Monday .. 7 = Sunday.
 * @param today The date drawn as today. `null` reads the current date from `kotlin.time.Clock`.
 * @param colors The [CalendarViewColors] of the grid.
 * @param title Converts a year and a month (1..12) to the header title.
 * @param weekdayLabel Converts an ISO day of week (1 = Monday .. 7 = Sunday) to a column header.
 */
@Composable
fun CalendarView(
    value: DateValue,
    onValueChange: (DateValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    yearRange: IntRange = CalendarViewDefaults.YearRange,
    firstDayOfWeek: Int = CalendarViewDefaults.FIRST_DAY_OF_WEEK,
    today: DateValue? = null,
    colors: CalendarViewColors = CalendarViewDefaults.colors(),
    title: (year: Int, month: Int) -> String = CalendarViewDefaults.Title,
    weekdayLabel: (dayOfWeek: Int) -> String = CalendarViewDefaults.WeekdayLabel,
) {
    require(yearRange.first <= yearRange.last) { "yearRange must not be empty" }
    require(firstDayOfWeek in 1..7) { "firstDayOfWeek must be in 1..7 (ISO-8601)" }

    val selected = value.coercedTo(yearRange)
    val todayDate = today ?: remember { currentDate() }

    // The native pager follows setDate(); browsing with the header buttons only moves the page.
    var displayedMonthIndex by remember(selected.year, selected.month) {
        mutableIntStateOf(selected.year * 12 + selected.month - 1)
    }
    val displayedYear = displayedMonthIndex / 12
    val displayedMonth = displayedMonthIndex % 12 + 1

    val daysInMonth = daysInMonth(displayedYear, displayedMonth)
    val leading = (isoDayOfWeek(displayedYear, displayedMonth, 1) - firstDayOfWeek + 7) % 7
    // COUIDateMonthView: six rows when the month and its offset do not fit in five.
    val needsSixRows = leading + daysInMonth > 35
    val rowCount = if (needsSixRows) CalendarViewDefaults.ROW_COUNT else CalendarViewDefaults.ROW_COUNT - 1
    val rowHeight = if (needsSixRows) {
        CalendarViewDefaults.DayHeight
    } else {
        CalendarViewDefaults.DayHeight + CalendarViewDefaults.DayMinColumnPadding
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CalendarViewDefaults.HeaderHeight)
                .padding(horizontal = CalendarViewDefaults.HeaderPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title(displayedYear, displayedMonth),
                color = colors.titleColor,
                style = CalendarViewDefaults.titleTextStyle(),
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CalendarNavigationButton(
                    icon = COUIIcons.Basic.ChevronStart,
                    contentDescription = "Previous month",
                    enabled = enabled && displayedMonthIndex > yearRange.first * 12,
                    color = colors.titleColor,
                    highlightColor = colors.highlightColor,
                    onClick = { displayedMonthIndex-- },
                )
                CalendarNavigationButton(
                    icon = COUIIcons.Basic.ChevronEnd,
                    contentDescription = "Next month",
                    enabled = enabled && displayedMonthIndex < yearRange.last * 12 + 11,
                    color = colors.titleColor,
                    highlightColor = colors.highlightColor,
                    onClick = { displayedMonthIndex++ },
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CalendarViewDefaults.HorizontalPadding)
                .padding(top = CalendarViewDefaults.VerticalPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CalendarViewDefaults.WeekdayHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (column in 0 until 7) {
                    Text(
                        text = weekdayLabel((firstDayOfWeek - 1 + column) % 7 + 1),
                        color = colors.weekdayColor,
                        style = CalendarViewDefaults.weekdayTextStyle(),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Column(
                modifier = Modifier.height(
                    CalendarViewDefaults.DayHeight * CalendarViewDefaults.ROW_COUNT,
                ),
            ) {
                val firstCell = DateValue(displayedYear, displayedMonth, 1).plusDays(-leading)
                for (row in 0 until rowCount) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (column in 0 until 7) {
                            val cell = firstCell.plusDays(row * 7 + column)
                            DayCell(
                                day = cell.day,
                                inMonth = cell.year == displayedYear && cell.month == displayedMonth,
                                isSelected = cell == selected,
                                isToday = cell == todayDate,
                                enabled = enabled && cell.year in yearRange,
                                colors = colors,
                                onClick = { onValueChange(cell) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The palette of a [CalendarView], resolved from the ColorOS 17 attributes and text appearances that
 * `COUIDateMonthView` reads.
 *
 * @property titleColor The month/year title and the chevron buttons. The header `TextView` resolves
 *   `?attr/couiColorPrimaryNeutral` (`coui_color_primary_neutral`, `#e6000000` light /
 *   `#e6ffffff` night), which is the theme's `COUITheme.colorScheme.onSurface`.
 * @property weekdayColor The column headers. `TextAppearance.Material.Widget.Calendar.DayOfWeek`
 *   resolves `?attr/couiColorHintNeutral` (`coui_color_hint_neutral`, `#42000000` / `#4dffffff`),
 *   the theme's `COUITheme.colorScheme.disabledOnSurface`.
 * @property dayColor The days of the displayed month. The default item of the day state list
 *   `color/coui_primary_text_inverse_when_activated_material_light` is
 *   `?attr/couiColorPrimaryNeutral` (`#e6000000` / `#e6ffffff`), i.e.
 *   `COUITheme.colorScheme.onSurface`.
 * @property otherMonthDayColor The days of the neighbouring months. `COUIDateMonthView.mDisabledText`
 *   is `?attr/couiColorDisabledNeutral` (`coui_color_disabled_neutral`, `#42000000` / `#4dffffff`).
 * @property disabledDayColor The days of the displayed month that fall outside the selectable range.
 *   The `state_enabled=false` items of the day state list draw `?attr/couiColorPrimaryNeutral` at
 *   `?android:attr/disabledAlpha` (0.5).
 * @property todayColor Today's number and its ring. `COUIDateMonthView.mTodayText` is
 *   `?attr/couiColorPrimary` (`coui_color_primary`).
 * @property selectedDayContainerColor The fill of the selected day. `COUICalendarDayPickerView` hands
 *   it to `COUIDateMonthView.setDaySelectorColor` as `couiColorPrimary`.
 * @property selectedDayContentColor The number on the selected day. The `state_activated=true` items
 *   of the day state list draw `?attr/couiColorOnPrimary` (`coui_color_on_primary`, `#ffffffff`).
 * @property highlightColor The press feedback. `COUIDateMonthView.mDayHighlight` is
 *   `android.R.attr.colorControlHighlight`, which the ColorOS theme sets to
 *   `color/coui_color_control_highlight` (`#7fd0d0d0` light / `#7f707070` night).
 */
@Immutable
class CalendarViewColors(
    val titleColor: Color,
    val weekdayColor: Color,
    val dayColor: Color,
    val otherMonthDayColor: Color,
    val disabledDayColor: Color,
    val todayColor: Color,
    val selectedDayContainerColor: Color,
    val selectedDayContentColor: Color,
    val highlightColor: Color,
)

/** Contains default values used by [CalendarView]. */
object CalendarViewDefaults {

    /** The height of the header row. `coui_calendar_picker_header_material` `android:layout_height`. */
    val HeaderHeight = 32.dp

    /**
     * The start/end padding of the header. `coui_calendar_picker_header_material`
     * `android:paddingStart` / `android:paddingEnd`.
     */
    val HeaderPadding = 24.dp

    /** The size of the chevron glyph. `coui_calendar_picker_header_material` `android:minWidth`. */
    val NavigationIconSize = 24.dp

    /** The padding around the chevron. `coui_calendar_picker_header_material` `android:padding`. */
    val NavigationIconPadding = 4.dp

    /** The touch target of a previous/next button: the glyph plus its padding on both sides. */
    val NavigationButtonSize = NavigationIconSize + NavigationIconPadding * 2

    /** The height of the weekday header row. `coui_calendar_picker_day_of_week_height`. */
    val WeekdayHeight = 40.dp

    /** The height of a day cell. `coui_calendar_picker_day_height`. */
    val DayHeight = 40.dp

    /** The natural width of a day cell. `coui_calendar_picker_day_width`. */
    val DayWidth = 40.dp

    /**
     * The extra height a day row takes when the month fits in five rows, so that five rows still
     * cover the six-row area. `coui_calendar_picker_day_min_col_padding`.
     */
    val DayMinColumnPadding = 8.dp

    /**
     * The start/end padding of the grid. `coui_calendar_picker_month_item_material`
     * `android:paddingStart` / `android:paddingEnd` (`coui_calendar_picker_padding_horizontal`).
     */
    val HorizontalPadding = 16.dp

    /**
     * The top padding of the grid. `coui_calendar_picker_month_item_material` `android:paddingTop`
     * (`coui_calendar_picker_padding_top`), which is 0dp.
     */
    val VerticalPadding = 0.dp

    /**
     * The radius of the selected-day circle and of today's ring. The native draws
     * `calendar_picker_current_day_radius` (32dp) as a *diameter*,
     * `canvas.drawCircle(…, this.H / 2f, …)`, so the radius is half of it.
     */
    val SelectedCircleRadius = 16.dp

    /** The stroke width of today's ring. `coui_calendar_picker_current_day_stroke_radius` (1.8dp). */
    val TodayRingWidth = 1.8.dp

    /**
     * The number of day rows the grid reserves, whatever the month needs.
     * `COUIDateMonthView.onMeasure` adds `coui_calendar_picker_day_height * 6`.
     */
    const val ROW_COUNT = 6

    /**
     * The selectable years. `COUICalendarDayPickerView` falls back to 1900-01-01 and 2100-12-31 when
     * the `android:minDate` / `android:maxDate` attributes are absent.
     */
    val YearRange: IntRange = 1900..2100

    /**
     * The first column of the grid. The native takes it from
     * `Calendar.getInstance().getFirstDayOfWeek()` when the `android:firstDayOfWeek` attribute is
     * absent, which common code cannot read; the port fixes it to Monday (ISO-8601 1) and exposes the
     * parameter.
     */
    const val FIRST_DAY_OF_WEEK = 1

    /**
     * The header title style. `coui_calendar_picker_header_material`'s `TextView` is 16sp in
     * `sans-serif-medium`, the theme's `COUITheme.textStyles.button` tier.
     */
    @Composable
    fun titleTextStyle(): TextStyle = COUITheme.textStyles.button

    /**
     * The day number style. `TextAppearance.Material.Widget.Calendar.Day` inherits
     * `couiTextAppearanceHeadline6` (16sp, `sans-serif-medium`), which is the theme's
     * `COUITheme.textStyles.button` tier.
     */
    @Composable
    fun dayTextStyle(): TextStyle = COUITheme.textStyles.button

    /**
     * The weekday label style. `TextAppearance.Material.Widget.Calendar.DayOfWeek` inherits
     * `couiTextAppearanceButton` (14sp, `sans-serif-medium`); the theme has no 14sp medium tier, so
     * its 14sp `COUITheme.textStyles.body2` carries the native weight.
     */
    @Composable
    fun weekdayTextStyle(): TextStyle = COUITheme.textStyles.body2.copy(fontWeight = FontWeight.Medium)

    /** The default [CalendarViewColors]: the theme slots listed on [CalendarViewColors]. */
    @Composable
    fun colors(
        titleColor: Color = COUITheme.colorScheme.onSurface,
        weekdayColor: Color = COUITheme.colorScheme.disabledOnSurface,
        dayColor: Color = COUITheme.colorScheme.onSurface,
        otherMonthDayColor: Color = COUITheme.colorScheme.disabledOnSurface,
        disabledDayColor: Color = COUITheme.colorScheme.onSurface.copy(alpha = 0.5f),
        todayColor: Color = COUITheme.colorScheme.primary,
        selectedDayContainerColor: Color = COUITheme.colorScheme.primary,
        selectedDayContentColor: Color = COUITheme.colorScheme.onPrimary,
        highlightColor: Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
            CalendarHighlightDark
        } else {
            CalendarHighlightLight
        },
    ): CalendarViewColors = CalendarViewColors(
        titleColor = titleColor,
        weekdayColor = weekdayColor,
        dayColor = dayColor,
        otherMonthDayColor = otherMonthDayColor,
        disabledDayColor = disabledDayColor,
        todayColor = todayColor,
        selectedDayContainerColor = selectedDayContainerColor,
        selectedDayContentColor = selectedDayContentColor,
        highlightColor = highlightColor,
    )

    /**
     * The default header title: the month's full English name and the year, the `"MMMMy"` pattern
     * `COUIDateMonthView.getMonthYearLabel` builds with
     * `DateFormat.getBestDateTimePattern(locale, "MMMMy")`.
     */
    val Title: (year: Int, month: Int) -> String = { year, month ->
        "${MonthNames[month - 1]} $year"
    }

    /**
     * The default column headers: the English abbreviations. The native labels them with
     * `DateUtils.getDayOfWeekString(day, 50)`, the locale's shortest day names, which common code
     * cannot ask the platform for; the caller can pass its own formatter.
     */
    val WeekdayLabel: (dayOfWeek: Int) -> String = { dayOfWeek -> WeekdayNames[dayOfWeek - 1] }

    private val MonthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )

    private val WeekdayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
}

/** color/coui_color_control_highlight in light themes (#7fd0d0d0): the day press highlight. */
private val CalendarHighlightLight = Color(0x7FD0D0D0)

/** color/coui_color_control_highlight in dark themes (#7f707070): the day press highlight. */
private val CalendarHighlightDark = Color(0x7F707070)

/**
 * One day of the grid.
 *
 * @param day The day of the month.
 * @param inMonth Whether the day belongs to the displayed month. The neighbouring months' days are
 *   drawn in [CalendarViewColors.otherMonthDayColor], like the native's `mDisabledText`.
 * @param isSelected Whether the day is [CalendarView]'s value.
 * @param isToday Whether the day is today.
 * @param enabled Whether the day can be picked.
 * @param colors The palette.
 * @param onClick Called when the day is picked.
 * @param modifier The modifier to be applied to the cell.
 */
@Composable
private fun DayCell(
    day: Int,
    inMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    enabled: Boolean,
    colors: CalendarViewColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val contentColor = when {
        !inMonth -> colors.otherMonthDayColor
        !enabled -> colors.disabledDayColor
        isSelected -> colors.selectedDayContentColor
        isToday -> colors.todayColor
        else -> colors.dayColor
    }
    Box(
        modifier = modifier
            .fillMaxHeight()
            // The native checks the selected day first, so the press highlight never shows on it, and
            // today keeps its ring while the highlight is drawn over it.
            .drawBehind {
                val radius = CalendarViewDefaults.SelectedCircleRadius.toPx()
                if (isSelected) {
                    drawCircle(
                        color = colors.selectedDayContainerColor,
                        radius = radius,
                        center = center,
                    )
                } else {
                    if (isPressed && enabled) {
                        drawCircle(color = colors.highlightColor, radius = radius, center = center)
                    }
                    if (isToday) {
                        drawCircle(
                            color = colors.todayColor,
                            radius = radius,
                            center = center,
                            style = Stroke(width = CalendarViewDefaults.TodayRingWidth.toPx()),
                        )
                    }
                }
            }
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            color = contentColor,
            style = CalendarViewDefaults.dayTextStyle(),
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A previous/next month button.
 *
 * @param icon The chevron glyph.
 * @param contentDescription The accessibility label, like the native's
 *   `string/calendar_picker_prev_content` / `string/calendar_picker_next_content`.
 * @param enabled Whether the button can move the month.
 * @param color The glyph colour.
 * @param highlightColor The press highlight.
 * @param onClick Called when the button is clicked.
 * @param modifier The modifier to be applied to the button.
 */
@Composable
private fun CalendarNavigationButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    color: Color,
    highlightColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(CalendarViewDefaults.NavigationButtonSize)
            .drawBehind {
                if (isPressed && enabled) {
                    drawRect(color = highlightColor)
                }
            }
            .selectable(
                selected = false,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) color else color.copy(alpha = 0.38f),
            modifier = Modifier.size(CalendarViewDefaults.NavigationIconSize),
        )
    }
}

/** Coerces a date into [range], clamping the day to the month it lands in. */
private fun DateValue.coercedTo(range: IntRange): DateValue {
    val year = this.year.coerceIn(range)
    val month = this.month.coerceIn(1, 12)
    return DateValue(year, month, this.day.coerceIn(1, daysInMonth(year, month)))
}

/** The day count of a month, leap years included. */
private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    else -> error("Invalid month: $month")
}

/** The ISO day of week of a date, 1 = Monday .. 7 = Sunday. */
private fun isoDayOfWeek(year: Int, month: Int, day: Int): Int = (((daysFromCivil(year, month, day) + 3) % 7 + 7) % 7 + 1).toInt()

/** Adds a day count to a date, rolling over the month and the year. */
private fun DateValue.plusDays(days: Int): DateValue = if (days == 0) this else civilFromDays(daysFromCivil(year, month, day) + days)

/**
 * The current date, read from `kotlin.time.Clock` and converted with the civil-from-days algorithm
 * (`Instant.epochSeconds` counts days from 1970-01-01, which was a Thursday).
 */
private fun currentDate(): DateValue = civilFromDays(Clock.System.now().epochSeconds.floorDiv(86_400))

/** Days since 1970-01-01 of a proleptic Gregorian date (Howard Hinnant's `days_from_civil`). */
private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yearOfEra = y - era * 400
    val monthIndex = if (month > 2) month - 3 else month + 9
    val dayOfYear = (153 * monthIndex + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return era * 146_097L + dayOfEra - 719_468L
}

/** The proleptic Gregorian date of a day count since 1970-01-01 (Hinnant's `civil_from_days`). */
private fun civilFromDays(days: Long): DateValue {
    val z = days + 719_468L
    val era = (if (z >= 0) z else z - 146_096L) / 146_097L
    val dayOfEra = z - era * 146_097L
    val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
    val year = yearOfEra + era * 400
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val monthIndex = (5 * dayOfYear + 2) / 153
    val day = dayOfYear - (153 * monthIndex + 2) / 5 + 1
    val month = if (monthIndex < 10) monthIndex + 3 else monthIndex - 9
    return DateValue(
        year = (if (month <= 2) year + 1 else year).toInt(),
        month = month.toInt(),
        day = day.toInt(),
    )
}
