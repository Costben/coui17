// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import component.ArrowDemo
import component.BadgeDemo
import component.BasicComponentDemo
import component.BlurDemo
import component.BottomFloatingAccessoryNavigationBarDemo
import component.BottomFloatingEditToolbarDemo
import component.BottomFloatingNavigationBarDemo
import component.BottomFloatingToolbarDemo
import component.BottomSheetDemo
import component.BreadcrumbBarDemo
import component.ButtonDemo
import component.ButtonPreferenceDemo
import component.CalendarViewDemoContent
import component.CardDemo
import component.CheckboxDemo
import component.ChipDemo
import component.ChipTabDemo
import component.ClickSelectMenuDemoContent
import component.CodeTextFieldDemo
import component.CodeTextFieldSecurityDemo
import component.ColorPaletteDemo
import component.ColorPickerHSVDemo
import component.ColorPickerOKHSVDemo
import component.ColorPickerOKLABDemo
import component.ColorPickerOKLCHDemo
import component.ColorSwatchPickerCard
import component.DatePickerDemo
import component.DialogDemo
import component.DropdownDemo
import component.DropdownItemVariants
import component.ExpandableListDemoContent
import component.FloatingButtonDemoContent
import component.ForegroundBlurDemo
import component.FullPageStatementDemoContent
import component.GuideDialogDemo
import component.HintAnimationLayoutDemo
import component.HorizontalPopupMenuDemo
import component.InputViewCardMultiInputDemo
import component.InputViewCardSingleInputDemo
import component.InstallLoadProgressDemo
import component.IntentSeekBarDemoContent
import component.ListPreferenceDemoContent
import component.LoadProgressButtonDemo
import component.LoadingButtonDemo
import component.LockAndBannerDemo
import component.MarkPreferenceDemo
import component.MarkPreferenceStandaloneFullDemo
import component.MarqueeTextDemo
import component.MaterialShadowEdgeDemo
import component.MetaBallSurfaceDemo
import component.NavigationPopupMenuDemo
import component.NotificationSnackBarDemo
import component.NumberPickerDemo
import component.OtherDemo
import component.PageIndicatorDemo
import component.PopupMenuRotationDemo
import component.ProgressIndicatorDemo
import component.RadioButtonDemo
import component.RangeSliderDemo
import component.RecommendedPreferenceDemo
import component.SearchHistoryViewDemoContent
import component.SearchViewAnimateDemo
import component.SliderDemo
import component.SnackbarDemo
import component.SpinnerDemo
import component.SpotLightDemo
import component.SwitchDemo
import component.SwitchLoadingPreferenceDemo
import component.TabRowDemo
import component.TagViewDemo
import component.TextFieldInCardSettingsPreferenceFormDemo
import component.TextFieldLineSettingsDialogFormDemo
import component.TextFieldRectangleFloatingLabelDemo
import component.TimePicker12HourDemo
import component.TimePicker24HourDemo
import component.TooltipDemo
import component.TopAppBarColorOS17FormsDemo
import component.TopAppBarGlassDemo
import component.TopTipsDemoContent
import component.TopTipsStylesDemo
import component.TouchSearchViewDemoContent
import component.VerticalSliderDemo
import io.github.suqi8.coui.kmp.basic.SnackbarHostState
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The home page: the whole component list, in ColorOS 17 list form.
 *
 * The topology is the coui16 gallery's — the same 48 sections, in the same order, with no grouping
 * above them — but every entry now wears a COS17 list row as its group title, and the entries
 * themselves are the layout-free content composables the shared module exposes.
 */
@Composable
fun Coui17HomePage(
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
    ) {
        coui17Entry("Basic Component") { BasicComponentDemo() }
        coui17Entry("Checkbox") { CheckboxDemo() }
        coui17Entry("RadioButton") { RadioButtonDemo() }
        coui17Entry("Switch") { SwitchDemo() }
        coui17Entry("Arrow") { ArrowDemo() }
        coui17Entry("Dialog") { DialogDemo() }
        coui17Entry("GuideDialog") { GuideDialogDemo() }
        coui17Entry("PageIndicator") { PageIndicatorDemo() }
        coui17Entry("BottomSheet") { BottomSheetDemo() }
        coui17Entry("Dropdown") { DropdownDemo() }
        coui17Entry("Dropdown Item Variants") { DropdownItemVariants() }
        coui17Entry("HorizontalPopupMenu") { HorizontalPopupMenuDemo() }
        coui17Entry("Spinner") { SpinnerDemo() }
        coui17Entry("Button") { ButtonDemo() }
        coui17Entry("Snackbar") { SnackbarDemo(snackbarHostState) }
        coui17Entry("ProgressIndicator") { ProgressIndicatorDemo() }
        coui17Entry("TextField (Line, Settings dialog form)") {
            TextFieldLineSettingsDialogFormDemo(focusManager)
        }
        coui17Entry("TextField (in card, Settings preference form)") {
            TextFieldInCardSettingsPreferenceFormDemo(focusManager)
        }
        coui17Entry("TextField (Rectangle & floating label)") {
            TextFieldRectangleFloatingLabelDemo(focusManager)
        }
        coui17Entry("InputView (card single input)") {
            InputViewCardSingleInputDemo(focusManager)
        }
        coui17Entry("InputView (card multi input)") {
            InputViewCardMultiInputDemo(focusManager)
        }
        coui17Entry("CodeTextField") { CodeTextFieldDemo(focusManager) }
        coui17Entry("CodeTextField (security)") { CodeTextFieldSecurityDemo(focusManager) }
        coui17Entry("Slider") { SliderDemo() }
        coui17Entry("RangeSlider") { RangeSliderDemo() }
        coui17Entry("VerticalSlider") { VerticalSliderDemo() }
        coui17Entry("TabRow") { TabRowDemo() }
        coui17Entry("BottomFloatingNavigationBar") { BottomFloatingNavigationBarDemo() }
        coui17Entry("BottomFloatingAccessoryNavigationBar") {
            BottomFloatingAccessoryNavigationBarDemo()
        }
        coui17Entry("BottomFloatingEditToolbar") { BottomFloatingEditToolbarDemo() }
        coui17Entry("BottomFloatingToolbar") { BottomFloatingToolbarDemo() }
        coui17Entry("NavigationPopupMenu") { NavigationPopupMenuDemo() }
        coui17Entry("MetaBallSurface") { MetaBallSurfaceDemo() }
        coui17Entry("MaterialShadowEdge") { MaterialShadowEdgeDemo() }
        coui17Entry("SpotLight") { SpotLightDemo() }
        coui17Entry("TopAppBar (glass)") { TopAppBarGlassDemo() }
        coui17Entry("TopAppBar (ColorOS 17 forms)") { TopAppBarColorOS17FormsDemo() }
        coui17Entry("BreadcrumbBar") { BreadcrumbBarDemo() }
        coui17Entry("Tooltip") { TooltipDemo() }
        coui17Entry("NumberPicker") { NumberPickerDemo() }
        coui17Entry("DatePicker") { DatePickerDemo() }
        coui17Entry("TimePicker (24-hour)") { TimePicker24HourDemo() }
        coui17Entry("TimePicker (12-hour)") { TimePicker12HourDemo() }
        coui17Entry("ColorSwatchPicker") { ColorSwatchPickerCard() }
        coui17Entry("ColorPicker (HSV)") { ColorPickerHSVDemo() }
        coui17Entry("ColorPicker (OKHSV)") { ColorPickerOKHSVDemo() }
        coui17Entry("ColorPicker (OKLAB)") { ColorPickerOKLABDemo() }
        coui17Entry("ColorPicker (OKLCH)") { ColorPickerOKLCHDemo() }
        coui17Entry("ColorPalette") { ColorPaletteDemo() }
        coui17Entry("Card") { CardDemo() }
        coui17Entry("Chip") { ChipDemo() }
        coui17Entry("TopTips") { TopTipsDemoContent() }
        coui17Entry("ChipTab") { ChipTabDemo() }
        coui17Entry("CalendarView") { CalendarViewDemoContent() }
        coui17Entry("FloatingButton") { FloatingButtonDemoContent() }
        coui17Entry("ExpandableList") { ExpandableListDemoContent() }
        coui17Entry(
            "LockPatternView / NumericKeyboard / SimpleLockView / LockScreenPwdInput / Banner",
        ) {
            LockAndBannerDemo()
        }
        coui17Entry("IntentSeekBar") { IntentSeekBarDemoContent() }
        coui17Entry("TouchSearchView") { TouchSearchViewDemoContent() }
        coui17Entry("SearchHistoryView") { SearchHistoryViewDemoContent() }
        coui17Entry("SearchViewAnimate") { SearchViewAnimateDemo() }
        coui17Entry("LoadProgressButton") { LoadProgressButtonDemo() }
        coui17Entry("PopupMenuRotation") { PopupMenuRotationDemo() }
        coui17Entry("HintAnimationLayout") { HintAnimationLayoutDemo() }
        coui17Entry("TopTips Styles") { TopTipsStylesDemo() }
        coui17Entry("ClickSelectMenu") { ClickSelectMenuDemoContent() }
        coui17Entry("InstallLoadProgress") { InstallLoadProgressDemo() }
        coui17Entry("TagView") { TagViewDemo() }
        coui17Entry("NotificationSnackBar") { NotificationSnackBarDemo() }
        coui17Entry("MarqueeText") { MarqueeTextDemo() }
        coui17Entry("Loading Button") { LoadingButtonDemo() }
        coui17Entry("Badge") { BadgeDemo() }
        coui17Entry("SwitchLoadingPreference") { SwitchLoadingPreferenceDemo() }
        coui17Entry("MarkPreference") { MarkPreferenceDemo() }
        coui17Entry("MarkPreference (standalone, Full)") { MarkPreferenceStandaloneFullDemo() }
        coui17Entry("ButtonPreference") { ButtonPreferenceDemo() }
        coui17Entry("RecommendedPreference") { RecommendedPreferenceDemo() }
        coui17Entry("ListPreference") { ListPreferenceDemoContent() }
        coui17Entry("FullPageStatement") { FullPageStatementDemoContent() }
        coui17Entry("Texture Blur") { BlurDemo() }
        coui17Entry("Foreground Blur") { ForegroundBlurDemo() }
        coui17Entry("Other") { OtherDemo() }
    }
}

/**
 * One gallery entry in ColorOS 17 list form: a group-title row, the entry's content in a card inset
 * 16dp from each edge, and the 16dp band the device leaves between groups.
 */
private fun LazyListScope.coui17Entry(title: String, content: @Composable () -> Unit) {
    item(key = "title:$title") { Coui17GroupTitle(title) }
    item(key = "content:$title") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Coui17Dimens.ListCardInset),
        ) {
            content()
        }
    }
    item(key = "gap:$title") { Spacer(modifier = Modifier.height(Coui17Dimens.ListGroupGap)) }
}

/** A COS17 list row holding the entry's name: 52.13dp tall, text starting at x=72dp. */
@Composable
private fun Coui17GroupTitle(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Coui17Dimens.ListRowHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(start = Coui17Dimens.ListTextIndent),
            style = COUITheme.textStyles.headline2,
            color = COUITheme.colorScheme.onSurface,
        )
    }
}
