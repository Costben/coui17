// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package coui17

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import component.ArrowDemo
import component.BadgeDemo
import component.BasicComponentDemo
import component.BlurDemo
import component.BottomAssignmentDialogDemo
import component.BottomFloatingAccessoryNavigationBarDemo
import component.BottomFloatingNavigationBarDemo
import component.BottomFloatingScrollToolbarDemo
import component.BottomFloatingToolbarDemo
import component.BottomSheetDemo
import component.ButtonDemo
import component.ButtonPreferenceDemo
import component.CardDemo
import component.CheckboxDemo
import component.ChipDemo
import component.ChipTabDemo
import component.ClickSelectMenuDemoContent
import component.CodeTextFieldDemo
import component.CodeTextFieldSecurityDemo
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
import component.LoadingButtonDemo
import component.LockAndBannerDemo
import component.MarkPreferenceDemo
import component.MarkPreferenceStandaloneFullDemo
import component.MarqueeTextDemo
import component.MaterialShadowEdgeDemo
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
import component.SlideViewDemo
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
import component.WindowIconMenuVariantsDemo
import io.github.suqi8.coui.kmp.basic.SnackbarHostState
import io.github.suqi8.coui.kmp.blur.isRuntimeShaderSupported

/**
 * One demo on a component's page. [title] is an English source key; a page with a single demo shows
 * no title of its own, since the bar already names the component.
 */
internal class Coui17Demo(
    val title: String,
    val content: @Composable (SnackbarHostState) -> Unit,
)

/**
 * One row on the home page and the page it opens. Its variants are the demos on that page, never
 * rows of their own, so a component has exactly one way in.
 *
 * [title] is the English source key and doubles as the route id.
 */
internal class Coui17Component(
    val title: String,
    val demos: List<Coui17Demo>,
)

/** A titled card of rows on the home page. */
internal class Coui17Category(
    val title: String,
    val components: List<Coui17Component>,
)

/** Finds the component a [navigation3.Route.Component] names, or null for a stale id. */
internal fun coui17ComponentOf(id: String): Coui17Component? = Coui17Catalog.firstNotNullOfOrNull { category ->
    category.components.firstOrNull { it.title == id }
}

/**
 * Every gallery entry, grouped the way the home page lists them.
 *
 * The list is built once: it holds no state, only which demo belongs where, so the home page and
 * the component pages read the same instance.
 */
internal val Coui17Catalog: List<Coui17Category> = buildList {
    category("Basic") {
        single("Switch") { SwitchDemo() }
        single("Checkbox") { CheckboxDemo() }
        single("RadioButton") { RadioButtonDemo() }
        component("Basic Component") {
            demo("Basic Component") { BasicComponentDemo() }
            demo("Arrow") { ArrowDemo() }
        }
        single("Card") { CardDemo() }
        component("Chip") {
            demo("Chip") { ChipDemo() }
            demo("TagView") { TagViewDemo() }
        }
        single("Badge") { BadgeDemo() }
        component("PageIndicator") {
            demo("PageIndicator") { PageIndicatorDemo() }
            demo("BannerIndicator") { BannerIndicatorDemo() }
        }
        single("MarqueeText") { MarqueeTextDemo() }
        component("FlowLayout") {
            demo("FlowLayout") { FlowLayoutDemo() }
            demo("Folded FlowLayout") { FoldedFlowLayoutDemo() }
        }
        single("ExpandableList") { ExpandableListDemoContent() }
        single("SlideView") { SlideViewDemo() }
    }
    category("Buttons & actions") {
        component("Button") {
            demo("Button") { ButtonDemo() }
            demo("Loading Button") { LoadingButtonDemo() }
        }
        single("FloatingButton") { FloatingButtonDemoContent() }
        component("ProgressIndicator") {
            demo("ProgressIndicator") { ProgressIndicatorDemo() }
            demo("InstallLoadProgress") { InstallLoadProgressDemo() }
        }
        component("Snackbar") {
            demo("Snackbar") { snackbarHostState -> SnackbarDemo(snackbarHostState) }
            demo("NotificationSnackBar") { NotificationSnackBarDemo() }
            demo("NotificationSwipeBox") { NotificationSwipeDemo() }
        }
        component("TopTips") {
            demo("TopTips") { TopTipsDemoContent() }
            demo("TopTips Styles") { TopTipsStylesDemo() }
        }
    }
    category("Text input") {
        component("TextField") {
            demo("TextField (Line, Settings dialog form)") {
                TextFieldLineSettingsDialogFormDemo(LocalFocusManager.current)
            }
            demo("TextField (in card, Settings preference form)") {
                TextFieldInCardSettingsPreferenceFormDemo(LocalFocusManager.current)
            }
            demo("TextField (Rectangle & floating label)") {
                TextFieldRectangleFloatingLabelDemo(LocalFocusManager.current)
            }
            demo("InputView (card single input)") { InputViewCardSingleInputDemo(LocalFocusManager.current) }
            demo("InputView (card multi input)") { InputViewCardMultiInputDemo(LocalFocusManager.current) }
        }
        component("CodeTextField") {
            demo("CodeTextField") { CodeTextFieldDemo(LocalFocusManager.current) }
            demo("CodeTextField (security)") { CodeTextFieldSecurityDemo(LocalFocusManager.current) }
        }
        component("Search") {
            demo("SearchViewAnimate") { SearchViewAnimateDemo() }
            demo("SearchHistoryView") { SearchHistoryViewDemoContent() }
            demo("TouchSearchView") { TouchSearchViewDemoContent() }
        }
    }
    category("Pickers") {
        component("Slider") {
            demo("Slider") { SliderDemo() }
            demo("RangeSlider") { RangeSliderDemo() }
            demo("VerticalSlider") { VerticalSliderDemo() }
            demo("IntentSeekBar") { IntentSeekBarDemoContent() }
        }
        single("NumberPicker") { NumberPickerDemo() }
        component("DatePicker") {
            demo("DatePicker") { DatePickerDemo() }
            demo("LunarDatePicker") { LunarDatePickerDemo() }
        }
        component("TimePicker") {
            demo("TimePicker (24-hour)") { TimePicker24HourDemo() }
            demo("TimePicker (12-hour)") { TimePicker12HourDemo() }
        }
    }
    category("Overlays & menus") {
        // Every dialog type is shown once, on the overlay host; the "Dialog (W)" row keeps the
        // window host demonstrated, since the two hosts share the same content layouts.
        component("Dialog") {
            demo("Dialog") { DialogDemo() }
            demo("OverlayBottomAssignmentDialog") { BottomAssignmentDialogDemo() }
            demo("GuideDialog") { GuideDialogDemo() }
        }
        single("BottomSheet") { BottomSheetDemo() }
        component("Dropdown") {
            demo("Dropdown") { DropdownDemo() }
            demo("Dropdown Item Variants") { DropdownItemVariants() }
            demo("Spinner") { SpinnerDemo() }
            demo("ClickSelectMenu") { ClickSelectMenuDemoContent() }
        }
        component("Popup menu") {
            demo("HorizontalPopupMenu") { HorizontalPopupMenuDemo() }
            demo("WindowIconMenu variants") { WindowIconMenuVariantsDemo() }
            demo("NavigationPopupMenu") { NavigationPopupMenuDemo() }
            demo("PopupMenuRotation") { PopupMenuRotationDemo() }
        }
        single("Tooltip") { TooltipDemo() }
    }
    category("Navigation & layout") {
        component("TopAppBar") {
            demo("TopAppBar (glass)") { TopAppBarGlassDemo() }
            demo("TopAppBar (ColorOS 17 forms)") { TopAppBarColorOS17FormsDemo() }
            demo("PinnedTopAppBar") { PinnedTopAppBarDemo() }
        }
        component("TabRow") {
            demo("TabRow") { TabRowDemo() }
            demo("ChipTab") { ChipTabDemo() }
        }
        component("BottomFloatingNavigationBar") {
            demo("BottomFloatingNavigationBar") { BottomFloatingNavigationBarDemo() }
            demo("BottomFloatingAccessoryNavigationBar") { BottomFloatingAccessoryNavigationBarDemo() }
            demo("NavigationBarEnterExit") { NavigationBarEnterExitDemo() }
        }
        component("BottomFloatingToolbar") {
            demo("BottomFloatingToolbar") { BottomFloatingToolbarDemo() }
            demo("BottomFloatingScrollToolbar") { BottomFloatingScrollToolbarDemo() }
        }
        single("HorizontalScrollBar") { HorizontalScrollBarDemo() }
        single("ListDetailView") { ListDetailViewDemo() }
        single("LockPatternView / NumericKeyboard / SimpleLockView / LockScreenPwdInput / Banner") {
            LockAndBannerDemo()
        }
        single("EmptyState") { EmptyStateDemo() }
    }
    category("Preferences") {
        component("Switch preferences") {
            demo("SwitchLoadingPreference") { SwitchLoadingPreferenceDemo() }
            demo("StatusSwitchingPreference") { StatusSwitchingPreferenceDemo() }
            demo("CheckBoxWithDivider / SwitchWithDivider / MarkWithDivider") { DividerPreferenceGroupDemo() }
        }
        component("Selection preferences") {
            demo("MarkPreference") { MarkPreferenceDemo() }
            demo("MarkPreference (standalone, Full)") { MarkPreferenceStandaloneFullDemo() }
            demo("ListPreference") { ListPreferenceDemoContent() }
        }
        component("Input preferences") {
            demo("EditTextPreference") { EditTextPreferenceDemo() }
            demo("InputPreference") { InputPreferenceDemo() }
            demo("StepperPreference") { StepperPreferenceDemo() }
        }
        component("Button & card preferences") {
            demo("ButtonPreference") { ButtonPreferenceDemo() }
            demo("RecommendedPreference") { RecommendedPreferenceDemo() }
            demo("CardEntrancePreference") { CardEntrancePreferenceDemo() }
            demo("CardInstructionPreference") { CardInstructionPreferenceDemo() }
        }
        component("App preferences") {
            demo("AppInfoPreference") { AppInfoPreferenceDemo() }
            demo("LoadInstallProgressPreference") { LoadInstallProgressPreferenceDemo() }
        }
        component("Headers & statements") {
            demo("CheckBoxPreferenceCategory / LoadingPreferenceCategory / BottomPreference") {
                CategoryHeaderPreferenceDemo()
            }
            demo("PagerHeaderPreference / PagerFooterPreference") { PagerHeaderFooterPreferenceDemo() }
            demo("SpannablePreference") { SpannablePreferenceDemo() }
            demo("FullPageStatement") { FullPageStatementDemoContent() }
        }
    }
    category("Visual effects") {
        single("MaterialShadowEdge") { MaterialShadowEdgeDemo() }
        single("SpotLight") { SpotLightDemo() }
        single("HintAnimationLayout") { HintAnimationLayoutDemo() }
        if (isRuntimeShaderSupported()) {
            component("Blur") {
                demo("Texture Blur") { BlurDemo() }
                demo("Foreground Blur") { ForegroundBlurDemo() }
            }
        }
    }
    category("Misc") {
        single("Other") { OtherDemo() }
    }
}

private class CategoryBuilder {
    val components = mutableListOf<Coui17Component>()

    /** A component with one demo, shown without a title of its own. */
    fun single(title: String, content: @Composable (SnackbarHostState) -> Unit) {
        components += Coui17Component(title, listOf(Coui17Demo(title, content)))
    }

    /** A component whose page runs several titled demos. */
    fun component(title: String, block: ComponentBuilder.() -> Unit) {
        components += Coui17Component(title, ComponentBuilder().apply(block).demos.toList())
    }
}

private class ComponentBuilder {
    val demos = mutableListOf<Coui17Demo>()

    fun demo(title: String, content: @Composable (SnackbarHostState) -> Unit) {
        demos += Coui17Demo(title, content)
    }
}

private fun MutableList<Coui17Category>.category(title: String, block: CategoryBuilder.() -> Unit) {
    add(Coui17Category(title, CategoryBuilder().apply(block).components.toList()))
}
