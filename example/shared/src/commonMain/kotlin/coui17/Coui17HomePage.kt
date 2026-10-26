// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package coui17

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
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
import component.HorizontalScrollBarDemo
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
import component.NavigationBarEnterExitDemo
import component.NavigationPopupMenuDemo
import component.NotificationSnackBarDemo
import component.NotificationSwipeDemo
import component.NumberPickerDemo
import component.OtherDemo
import component.PageIndicatorDemo
import component.PinnedTopAppBarDemo
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
import component.WindowDialogVariantsDemo
import component.WindowIconMenuVariantsDemo
import i18n.str
import io.github.suqi8.coui.kmp.basic.SnackbarHostState
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.blur.isRuntimeShaderSupported
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The home page: the whole component list, in ColorOS 17 list form.
 *
 * The list is grouped into nine named runs — basics, actions, text input, pickers, overlays,
 * navigation, preferences, visual effects, misc — because a flat run of 110 entries gave a reader no
 * way to find anything. Each entry keeps its own COS17 group-title row under the section heading, and
 * the entries themselves are the layout-free content composables the shared module exposes.
 *
 * Every heading and every string a demo shows resolves through [str], so the whole page follows the
 * language switch: the section names and entry titles are English source keys, never a second copy
 * of the same name in another language.
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
        coui17Section("Basic")
        coui17Entry("Basic Component") { BasicComponentDemo() }
        coui17Entry("Checkbox") { CheckboxDemo() }
        coui17Entry("RadioButton") { RadioButtonDemo() }
        coui17Entry("Switch") { SwitchDemo() }
        coui17Entry("Arrow") { ArrowDemo() }
        coui17Entry("Card") { CardDemo() }
        coui17Entry("Chip") { ChipDemo() }
        coui17Entry("ChipTab") { ChipTabDemo() }
        coui17Entry("TagView") { TagViewDemo() }
        coui17Entry("Badge") { BadgeDemo() }
        coui17Entry("PageIndicator") { PageIndicatorDemo() }
        coui17Entry("BannerIndicator") { BannerIndicatorDemo() }
        coui17Entry("MarqueeText") { MarqueeTextDemo() }
        coui17Entry("FlowLayout") { FlowLayoutDemo() }
        coui17Entry("ExpandableList") { ExpandableListDemoContent() }

        coui17Section("Buttons & actions")
        coui17Entry("Button") { ButtonDemo() }
        coui17Entry("Loading Button") { LoadingButtonDemo() }
        coui17Entry("LoadProgressButton") { LoadProgressButtonDemo() }
        coui17Entry("FloatingButton") { FloatingButtonDemoContent() }
        coui17Entry("ProgressIndicator") { ProgressIndicatorDemo() }
        coui17Entry("InstallLoadProgress") { InstallLoadProgressDemo() }
        coui17Entry("Snackbar") { SnackbarDemo(snackbarHostState) }
        coui17Entry("NotificationSnackBar") { NotificationSnackBarDemo() }
        coui17Entry("NotificationSwipeBox") { NotificationSwipeDemo() }
        coui17Entry("TopTips") { TopTipsDemoContent() }
        coui17Entry("TopTips Styles") { TopTipsStylesDemo() }

        coui17Section("Text input")
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
        coui17Entry("TouchSearchView") { TouchSearchViewDemoContent() }
        coui17Entry("SearchHistoryView") { SearchHistoryViewDemoContent() }
        coui17Entry("SearchViewAnimate") { SearchViewAnimateDemo() }

        coui17Section("Pickers")
        coui17Entry("Slider") { SliderDemo() }
        coui17Entry("RangeSlider") { RangeSliderDemo() }
        coui17Entry("VerticalSlider") { VerticalSliderDemo() }
        coui17Entry("NumberPicker") { NumberPickerDemo() }
        coui17Entry("DatePicker") { DatePickerDemo() }
        coui17Entry("TimePicker (24-hour)") { TimePicker24HourDemo() }
        coui17Entry("TimePicker (12-hour)") { TimePicker12HourDemo() }
        coui17Entry("LunarDatePicker") { LunarDatePickerDemo() }
        coui17Entry("CalendarView") { CalendarViewDemoContent() }
        coui17Entry("IntentSeekBar") { IntentSeekBarDemoContent() }
        coui17Entry("ColorSwatchPicker") { ColorSwatchPickerCard() }
        coui17Entry("ColorPicker (HSV)") { ColorPickerHSVDemo() }
        coui17Entry("ColorPicker (OKHSV)") { ColorPickerOKHSVDemo() }
        coui17Entry("ColorPicker (OKLAB)") { ColorPickerOKLABDemo() }
        coui17Entry("ColorPicker (OKLCH)") { ColorPickerOKLCHDemo() }
        coui17Entry("ColorPalette") { ColorPaletteDemo() }

        coui17Section("Overlays & menus")
        coui17Entry("Dialog") { DialogDemo() }
        coui17Entry("SingleInputDialog (ColorOS 17)") { SingleInputDialogDemo() }
        coui17Entry("PanelTemplate (ColorOS 17)") { PanelTemplateDemo() }
        coui17Entry("BottomSheet") { BottomSheetDemo() }
        coui17Entry("WindowDialog variants") { WindowDialogVariantsDemo() }
        coui17Entry("WindowIconMenu variants") { WindowIconMenuVariantsDemo() }
        coui17Entry("GuideDialog") { GuideDialogDemo() }
        coui17Entry("Dropdown") { DropdownDemo() }
        coui17Entry("Dropdown Item Variants") { DropdownItemVariants() }
        coui17Entry("HorizontalPopupMenu") { HorizontalPopupMenuDemo() }
        coui17Entry("Spinner") { SpinnerDemo() }
        coui17Entry("PopupMenuRotation") { PopupMenuRotationDemo() }
        coui17Entry("ClickSelectMenu") { ClickSelectMenuDemoContent() }
        coui17Entry("Tooltip") { TooltipDemo() }

        coui17Section("Navigation & layout")
        coui17Entry("TabRow") { TabRowDemo() }
        coui17Entry("BottomFloatingNavigationBar") { BottomFloatingNavigationBarDemo() }
        coui17Entry("BottomFloatingAccessoryNavigationBar") {
            BottomFloatingAccessoryNavigationBarDemo()
        }
        coui17Entry("BottomFloatingEditToolbar") { BottomFloatingEditToolbarDemo() }
        coui17Entry("BottomFloatingToolbar") { BottomFloatingToolbarDemo() }
        coui17Entry("NavigationPopupMenu") { NavigationPopupMenuDemo() }
        coui17Entry("TopAppBar (glass)") { TopAppBarGlassDemo() }
        coui17Entry("TopAppBar (ColorOS 17 forms)") { TopAppBarColorOS17FormsDemo() }
        coui17Entry("PinnedTopAppBar") { PinnedTopAppBarDemo() }
        coui17Entry("BreadcrumbBar") { BreadcrumbBarDemo() }
        coui17Entry("NavigationBarEnterExit") { NavigationBarEnterExitDemo() }
        coui17Entry("HorizontalScrollBar") { HorizontalScrollBarDemo() }
        coui17Entry("ListDetailView") { ListDetailViewDemo() }
        coui17Entry("SideNavigationBar") { SideNavigationBarDemo() }
        coui17Entry("SidePaneLayout") { SidePaneLayoutDemo() }
        coui17Entry(
            "LockPatternView / NumericKeyboard / SimpleLockView / LockScreenPwdInput / Banner",
        ) {
            LockAndBannerDemo()
        }
        coui17Entry("EmptyState") { EmptyStateDemo() }
        coui17Entry("CompleteState") { CompleteStateDemo() }

        coui17Section("Preferences")
        coui17Entry("SwitchLoadingPreference") { SwitchLoadingPreferenceDemo() }
        coui17Entry("MarkPreference") { MarkPreferenceDemo() }
        coui17Entry("MarkPreference (standalone, Full)") { MarkPreferenceStandaloneFullDemo() }
        coui17Entry("ButtonPreference") { ButtonPreferenceDemo() }
        coui17Entry("RecommendedPreference") { RecommendedPreferenceDemo() }
        coui17Entry("ListPreference") { ListPreferenceDemoContent() }
        coui17Entry("FullPageStatement") { FullPageStatementDemoContent() }
        coui17Entry("EditTextPreference") { EditTextPreferenceDemo() }
        coui17Entry("InputPreference") { InputPreferenceDemo() }
        coui17Entry("LoadInstallProgressPreference") { LoadInstallProgressPreferenceDemo() }
        coui17Entry("AppInfoPreference") { AppInfoPreferenceDemo() }
        coui17Entry("CardEntrancePreference") { CardEntrancePreferenceDemo() }
        coui17Entry("CardInstructionPreference") { CardInstructionPreferenceDemo() }
        coui17Entry("PagerHeaderPreference / PagerFooterPreference") {
            PagerHeaderFooterPreferenceDemo()
        }
        coui17Entry("SpannablePreference") { SpannablePreferenceDemo() }
        coui17Entry("StepperPreference") { StepperPreferenceDemo() }
        coui17Entry("StatusSwitchingPreference") { StatusSwitchingPreferenceDemo() }
        coui17Entry("CheckBoxPreferenceCategory / LoadingPreferenceCategory / BottomPreference") {
            CategoryHeaderPreferenceDemo()
        }
        coui17Entry("CheckBoxWithDivider / SwitchWithDivider / MarkWithDivider") {
            DividerPreferenceGroupDemo()
        }

        coui17Section("Visual effects")
        coui17Entry("MetaBallSurface") { MetaBallSurfaceDemo() }
        coui17Entry("MaterialShadowEdge") { MaterialShadowEdgeDemo() }
        coui17Entry("SpotLight") { SpotLightDemo() }
        coui17Entry("HintAnimationLayout") { HintAnimationLayoutDemo() }
        if (isRuntimeShaderSupported()) {
            coui17Entry("Texture Blur") { BlurDemo() }
            coui17Entry("Foreground Blur") { ForegroundBlurDemo() }
        }

        coui17Section("Misc")
        coui17Entry("Other") { OtherDemo() }
    }
}

/**
 * A named run of gallery entries: one heading row, then the entries that belong to it.
 *
 * [title] is the English source string, resolved by [Coui17SectionTitle], so the entry list stays a
 * plain list of keys and switching language renames every heading under it.
 */
private fun LazyListScope.coui17Section(title: String) {
    item(key = "section:$title") { Coui17SectionTitle(title) }
}

/**
 * The heading of one section: 20sp medium, one step above the 16sp group titles it contains.
 *
 * It shares the group title's 16dp card inset and 16dp text indent, so a section heading, an entry
 * title and the text inside an entry's card all line up on the same x.
 */
@Composable
private fun Coui17SectionTitle(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Coui17Dimens.ListCardInset)
            .padding(top = Coui17Dimens.SectionTopGap, bottom = Coui17Dimens.SectionBottomGap)
            .height(Coui17Dimens.ListRowHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = str(title),
            modifier = Modifier.padding(start = Coui17Dimens.ListTextIndent),
            style = COUITheme.textStyles.title3.copy(fontWeight = FontWeight.Medium),
            color = COUITheme.colorScheme.onBackground,
        )
    }
}

/**
 * One gallery entry in ColorOS 17 list form: a compact group-title row and the entry's content.
 * Shared demos own their 16dp card inset and bottom spacing, so this wrapper adds neither again.
 *
 * The content sits in a [Column], not a box: several demos emit more than one root — `ButtonDemo`
 * alone emits four rows — and a box stacks its children on the same origin, which drew those rows on
 * top of one another. The coui16 gallery never showed it because its sections push their roots as
 * separate lazy-list items.
 */
private fun LazyListScope.coui17Entry(title: String, content: @Composable () -> Unit) {
    item(key = "title:$title") { Coui17GroupTitle(title) }
    item(key = "heading-gap:$title") {
        Spacer(modifier = Modifier.height(Coui17Dimens.ListHeadingContentGap))
    }
    item(key = "content:$title") {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            content()
        }
    }
    item(key = "gap:$title") { Spacer(modifier = Modifier.height(Coui17Dimens.ListGroupGap)) }
}

/**
 * A compact Settings section heading aligned with the text inside its card.
 *
 * [title] is the English source string; the heading resolves it here, so the entry list above stays
 * a plain list of keys and the language can change under it.
 */
@Composable
private fun Coui17GroupTitle(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Coui17Dimens.ListCardInset)
            .height(Coui17Dimens.ListRowHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = str(title),
            modifier = Modifier.padding(start = Coui17Dimens.ListTextIndent),
            style = COUITheme.textStyles.headline2,
            color = COUITheme.colorScheme.onSurface,
        )
    }
}
