// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package coui17

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
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
import component.InputViewDemo
import component.InstallLoadProgressDemo
import component.IntentSeekBarDemoContent
import component.ListPreferenceDemoContent
import component.LoadingButtonDemo
import component.LockAndBannerDemo
import component.MarkPreferenceDemo
import component.MarqueeTextDemo
import component.MaterialShadowEdgeDemo
import component.MultiLineIconRowsDemo
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
import component.RecommendedPreferenceDemo
import component.SearchBarDemo
import component.SearchHistoryChipDemoContent
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
import component.TextFieldDemo
import component.TimePickerDemo
import component.TooltipDemo
import component.TopAppBarColorOS17FormsDemo
import component.TopAppBarGlassDemo
import component.TopTipsDemo
import component.TouchSearchViewDemoContent
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
 * One row on a category page and the page it opens. Its variants are the demos on that page, never
 * rows of their own, so a component has exactly one way in.
 *
 * [title] is the English source key and doubles as the route id.
 */
internal class Coui17Component(
    val title: String,
    val demos: List<Coui17Demo>,
)

/**
 * One row on the home card and the category page it opens.
 *
 * [title] is the English source key and doubles as the route id. [tint] and [icon] are what the row
 * shows on its start side — a 24dp dual-tone glyph in one of the five ColorOS 17 settings tints,
 * picked per category so no two neighbours share a colour.
 */
internal class Coui17Category(
    val title: String,
    val components: List<Coui17Component>,
    val tint: Coui17CategoryTint,
    val icon: ImageVector,
)

/** Finds the category a [navigation3.Route.Category] names, or null for a stale id. */
internal fun coui17CategoryOf(id: String): Coui17Category? = Coui17Catalog.firstOrNull { it.title == id }

/** Finds the component a [navigation3.Route.Component] names, or null for a stale id. */
internal fun coui17ComponentOf(id: String): Coui17Component? = Coui17Catalog.firstNotNullOfOrNull { category ->
    category.components.firstOrNull { it.title == id }
}

/**
 * Every gallery entry, grouped the way the home page draws it: one card per inner list, one row per
 * category inside that card, and each category page shows that category's components.
 *
 * The grouping is presentation only. [Coui17Catalog] flattens it back into the one ordered list the
 * routes and the search resolve against, so where a card breaks never changes a route id.
 *
 * The list is built once: it holds no state, only which demo belongs where, so the home page, the
 * category pages and the component pages read the same instance.
 */
internal val Coui17CatalogGroups: List<List<Coui17Category>> = listOf(
    listOf(
        category("Basic", Coui17CategoryTint.Green, Coui17CategoryIcons.Basic) {
            single("Switch") { SwitchDemo() }
            single("Checkbox") { CheckboxDemo() }
            single("RadioButton") { RadioButtonDemo() }
            component("Basic Component") {
                demo("Basic Component") { BasicComponentDemo() }
                demo("Arrow") { ArrowDemo() }
                demo("Multi-line icon rows") { MultiLineIconRowsDemo() }
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
            single("FlowLayout") { FlowLayoutDemo() }
            single("ExpandableList") { ExpandableListDemoContent() }
            single("SlideView") { SlideViewDemo() }
        },
        category("Buttons & actions", Coui17CategoryTint.Blue, Coui17CategoryIcons.Actions) {
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
            single("TopTips") { TopTipsDemo() }
        },
        category("Text input", Coui17CategoryTint.Orange, Coui17CategoryIcons.TextInput) {
            component("TextField") {
                demo("TextField") { TextFieldDemo(LocalFocusManager.current) }
                demo("InputView") { InputViewDemo(LocalFocusManager.current) }
            }
            single("CodeTextField") { CodeTextFieldDemo(LocalFocusManager.current) }
            component("Search") {
                demo("SearchBar") { SearchBarDemo() }
                demo("SearchViewAnimate") { SearchViewAnimateDemo() }
                demo("SearchHistoryView") { SearchHistoryViewDemoContent() }
                demo("SearchHistoryChip") { SearchHistoryChipDemoContent() }
                demo("TouchSearchView") { TouchSearchViewDemoContent() }
            }
        },
    ),
    listOf(
        category("Pickers", Coui17CategoryTint.Yellow, Coui17CategoryIcons.Pickers) {
            component("Slider") {
                demo("Slider") { SliderDemo() }
                demo("IntentSeekBar") { IntentSeekBarDemoContent() }
            }
            single("NumberPicker") { NumberPickerDemo() }
            single("DatePicker") { DatePickerDemo() }
            single("TimePicker") { TimePickerDemo() }
        },
        category("Overlays & menus", Coui17CategoryTint.Blue, Coui17CategoryIcons.Overlays) {
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
        },
    ),
    listOf(
        category("Navigation & layout", Coui17CategoryTint.Green, Coui17CategoryIcons.Navigation) {
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
        },
        category("Preferences", Coui17CategoryTint.Orange, Coui17CategoryIcons.Preferences) {
            component("Switch preferences") {
                demo("SwitchLoadingPreference") { SwitchLoadingPreferenceDemo() }
                demo("StatusSwitchingPreference") { StatusSwitchingPreferenceDemo() }
                demo("CheckBoxWithDivider / SwitchWithDivider / MarkWithDivider") { DividerPreferenceGroupDemo() }
            }
            component("Selection preferences") {
                demo("MarkPreference") { MarkPreferenceDemo() }
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
        },
    ),
    listOf(
        category("Visual effects", Coui17CategoryTint.Red, Coui17CategoryIcons.VisualEffects) {
            single("MaterialShadowEdge") { MaterialShadowEdgeDemo() }
            single("SpotLight") { SpotLightDemo() }
            single("HintAnimationLayout") { HintAnimationLayoutDemo() }
            if (isRuntimeShaderSupported()) {
                component("Blur") {
                    demo("Texture Blur") { BlurDemo() }
                    demo("Foreground Blur") { ForegroundBlurDemo() }
                }
            }
        },
        category("Misc", Coui17CategoryTint.Blue, Coui17CategoryIcons.Misc) {
            single("Other") { OtherDemo() }
        },
    ),
)

/** The same entries as [Coui17CatalogGroups], flattened in reading order. */
internal val Coui17Catalog: List<Coui17Category> = Coui17CatalogGroups.flatten()

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

private fun category(
    title: String,
    tint: Coui17CategoryTint,
    icon: ImageVector,
    block: CategoryBuilder.() -> Unit,
): Coui17Category = Coui17Category(
    title = title,
    components = CategoryBuilder().apply(block).components.toList(),
    tint = tint,
    icon = icon,
)
