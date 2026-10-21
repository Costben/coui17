# Components

COUI provides a rich set of UI components that strictly follow the OPPO ColorOS design language. Each component is carefully designed to ensure visual and interactive consistency with the native ColorOS experience.

## Scaffold Components

| Component                          | Description                   | Common Usage                    |
| ---------------------------------- | ----------------------------- | ------------------------------- |
| [Scaffold](../components/scaffold) | Basic layout for applications | Page structure, content display |

::: warning
The Scaffold component provides a suitable container for cross-platform popup windows. Components such as `OverlayDialog`, `OverlayDropdownPreference`, `OverlaySpinnerPreference`, and `OverlayListPopup` are all implemented based on this and therefore need to be wrapped by this component.
:::

## Basic Components

| Component                                                  | Description                                | Common Usage                          |
| ---------------------------------------------------------- | ------------------------------------------ | ------------------------------------- |
| [Surface](../components/surface)                           | Basic container component                  | Content display, background container |
| [TopAppBar](../components/topappbar)                       | Application top navigation bar             | Page title, primary actions           |
| [AppBarBlurOverlay](../components/appbarbluroverlay)       | Scrolling app bar material overlay         | Gradient-blur app bar over the page   |
| [NavigationBar](../components/navigationbar)               | Bottom navigation component                | Main page switching                   |
| [BottomFloatingAccessoryNavigationBar](../components/bottomfloatingaccessorynavigationbar) | Floating capsule navigation bar with side accessories | Bottom navigation with quick actions |
| [BottomFloatingEditToolbar](../components/bottomfloatingedittoolbar) | Floating edit toolbar            | Editing actions, quick tools          |
| [BottomFloatingToolbar](../components/bottomfloatingtoolbar) | Floating text-selection toolbar | Selection actions, grouped actions |
| [NavigationPopupMenu](../components/navigationpopupmenu)   | Navigation overflow menu                   | Items that do not fit in the bar      |
| [HorizontalPopupMenu](../components/horizontalpopupmenu)   | Horizontal icon block inside a popup list  | Icon actions in popups                |
| [NavigationRail](../components/navigationrail)             | Side navigation component                  | Main page switching (Large screen)    |
| [TabRow](../components/tabrow)                             | Horizontal tab bar                         | Content category browsing             |
| [BreadcrumbBar](../components/breadcrumbbar)               | Horizontal breadcrumb navigation bar         | File path navigation, location trail   |
| [Card](../components/card)                                 | Container with related information         | Information display, content grouping |
| [BasicComponent](../components/basiccomponent)             | Universal base component                   | Custom component development          |
| [Button](../components/button)                             | Interactive element for triggering actions | Form submission, action confirmation  |
| [LoadingButton](../components/loadingbutton)               | Text button with a built-in loading state  | Asynchronous action submission        |
| [IconButton](../components/iconbutton)                     | Icon button component                      | Auxiliary actions, toolbars           |
| [HighlightedIconButton](../components/highlightediconbutton) | Toolbar icon button with a light halo    | Toolbar back and overflow keys        |
| [Chip](../components/chip)                                 | Checkable capsule filter tag               | Content filtering, tag selection      |
| [ChipTab](../components/chiptab)                           | Horizontal chip tab bar                    | Tab switching, category browsing      |
| [CalendarView](../components/calendarview)                 | Month calendar grid with a weekday header  | Date selection, month browsing        |
| [FloatingButton](../components/floatingbutton)             | Speed-dial floating button                 | Quick actions, shortcuts              |
| [ExpandableList](../components/expandablelist)             | Collapsible group list                     | Settings sections, grouped content    |
| [LockPatternView](../components/lockpatternview)           | 3x3 pattern unlock grid                    | Pattern unlock, gesture password      |
| [NumericKeyboard](../components/numerickeyboard)           | Numeric PIN pad                            | PIN entry, passcode dialogs           |
| [SimpleLockView](../components/simplelockview)             | Row of PIN dots that fill as you type      | PIN entry, passcode status            |
| [LockScreenPwdInput](../components/lockscreenpwdinput)     | Pill password field with a next button     | Lock screen password entry            |
| [Banner](../components/banner)                             | Auto-scrolling card banner with dots       | Feature carousels, promo banners      |
| [SearchHistoryView](../components/searchhistoryview)       | Wrapping panel of recent search chips      | Search history, recent queries        |
| [TouchSearchView](../components/touchsearchview)           | A–Z index rail with a magnified key tile   | Alphabetical list navigation          |
| [IntentSeekBar](../components/intentseekbar)               | Seek bar with a secondary progress layer   | Media playback, buffered ranges       |
| [MarqueeText](../components/marqueetextview)               | Text that scrolls when it overflows         | Long labels, tickers                  |
| [NotificationSnackBar](../components/notificationsnackbar) | Notification-style snackbar                | In-app notifications, status messages |
| [TagView](../components/tagview)                           | Small text corner badge                    | "NEW" / "BETA" markers                |
| [MaterialShadowEdge](../components/materialshadowedge)     | Gradient rim plus inner shadow             | Material surface edges                |
| [SpotLight](../components/spotlight)                       | Positional light painted over a surface    | Press and drag highlights             |
| [PageIndicator](../components/pageindicator)               | Row of page dots with an active marker     | Page position in a pager, guides      |
| [Text](../components/text)                                 | Display text content with various styles   | Titles, body text, descriptive text   |
| [SmallTitle](../components/smalltitle)                     | Small title component                      | Auxiliary titles, category labels     |
| [TextField](../components/textfield)                       | Receives user text input                   | Form filling, search boxes            |
| [InputView](../components/inputview)                       | Card-style input field                     | Form filling, dialog input            |
| [CodeTextField](../components/codetextfield)               | Verification code input with cells         | OTP / verification code entry         |
| [Switch](../components/switch)                             | Binary state toggle control                | Setting switches, feature enabling    |
| [Checkbox](../components/checkbox)                         | Multiple selection control                 | Multiple choices, terms agreement     |
| [RadioButton](../components/radiobutton)                   | Single selection control                   | Exclusive choices, option selection   |
| [Slider](../components/slider)                             | Sliding control for value adjustment       | Volume control, range selection       |
| [NumberPicker](../components/numberpicker)                 | Vertical scroll picker for number selection | Time picker, quantity selection       |
| [DatePicker](../components/datepicker)                     | Date wheel picker (year / month / day)     | Date selection, forms                 |
| [LunarDatePicker](../components/lunardatepicker)           | Lunar date wheel picker (year / month / day) | Lunar date selection, forms         |
| [TimePicker](../components/timepicker)                     | Time wheel picker (hour / minute)          | Time selection, alarms                |
| [Stepper](../components/stepper)                           | Increment / decrement integer control      | Quantity adjustment, value tuning     |
| [ProgressIndicator](../components/progressindicator)       | Displays operation progress status         | Loading, progress display             |
| [SearchViewAnimate](../components/searchviewanimate)       | Magnetic search bar between icon and input | Search entry, collapsing search bars  |
| [HintAnimationLayout](../components/hintanimationlayout)   | Rotating search-bar hint text              | Search bar hints, placeholders        |
| [LoadProgressButton](../components/loadprogressbutton)     | Download button with a load state machine  | Install, download, pause, retry       |
| [PopupMenuRotation](../components/popupmenurotation)       | Rotated popup menu geometry                | Overflow menus in landscape layouts   |
| [ClickSelectMenu](../components/clickselectmenu)           | Menu placed at the touch point             | Row menus that open under the finger  |
| [InstallLoadProgress](../components/installloadprogress)   | Install button that morphs into progress   | App installs, downloads, retries      |
| [SideNavigationBar](../components/sidenavigationbar)       | Responsive side drawer with a fixed mode    | Album navigation, tablet side panels  |
| [SidePaneLayout](../components/sidepanelayout)             | Draggable pane beside the page content      | Gallery side panes, navigation panes  |
| [ListDetailView](../components/listdetailview)             | Responsive one-pane / two-pane list-detail container | Master-detail pages, tablet split |
| [Snackbar](../components/snackbar)                         | Temporary message bar for brief feedback   | Operation result, status notification |
| [TopTips](../components/toptips)                           | Tip banner at the top of a page            | Page hints, announcements             |
| [Tooltip](../components/tooltip)                           | Brief label shown on hover or long press   | Icon button labels, element hints     |
| [Badge](../components/badge)                               | Small status overlay on an anchor          | Unread counts, status dots            |
| [Icon](../components/icon)                                 | Icon display component                     | Icon buttons, status indicators       |
| [FloatingActionButton](../components/floatingactionbutton) | Floating action button                     | Primary actions, quick functions      |
| [FloatingToolbar](../components/floatingtoolbar)           | Floating toolbar                           | Quick actions, information display    |
| [Divider](../components/divider)                           | Content separator                          | Block separation, hierarchy division  |
| [PullToRefresh](../components/pulltorefresh)               | Pull-down refresh operation                | Data update, page refresh             |
| [FullPagePullToRefresh](../components/fullpagepulltorefresh) | Full-page refresh with a revealed backdrop | App Market style, featured feeds    |
| [SearchBar](../components/searchbar)                       | Search input field                         | Content search, quick find            |
| [FullPageStatement](../components/fullpagestatement)       | Full-page user agreement statement         | First-launch agreement, privacy statement |
| [EmptyState](../components/emptystate)                     | Empty page placeholder                     | No search results, empty lists        |
| [CompleteState](../components/completestate)               | Action completion page                     | Result pages, task finished           |
| [ColorPalette](../components/colorpalette)                 | Grid palette with alpha slider             | Theme settings, color selection       |
| [ColorPicker](../components/colorpicker)                   | Color selection control                    | Theme settings, color selection       |
| [ColorSwatchPicker](../components/colorswatchpicker)       | Circular color swatch row with selection ring | Theme settings, color selection    |
| [GridSpringChain](../components/gridspringchain)           | Per-row spring-chain overscroll for grids  | Album grids, tile grids               |

## Extended Components

| Component                                            | Description                                                                              | Common Usage                           |
| ---------------------------------------------------- | ---------------------------------------------------------------------------------------- | -------------------------------------- |
| [ArrowPreference](../components/arrowpreference)               | Arrow component based on BasicComponent                                                  | Clickable indication, navigation hints |
| [SwitchPreference](../components/switchpreference)             | Switch component based on BasicComponent                                                 | Setting switches, feature enabling     |
| [SwitchLoadingPreference](../components/switchloadingpreference) | Switch preference with a loading state                                                   | Asynchronous setting switches          |
| [CheckboxPreference](../components/checkboxpreference)         | Checkbox component based on BasicComponent                                               | Multiple selection, terms agreement    |
| [RadioButtonPreference](../components/radiobuttonpreference)   | Radio button component based on BasicComponent                                           | Exclusive choices, option selection    |
| [MarkPreference](../components/markpreference)                 | Single-select row with a selection mark                                                  | Single-select lists, option selection  |
| [ButtonPreference](../components/buttonpreference)             | Preference row with a small inline button                                                | Inline actions in settings lists       |
| [ListPreference](../components/listpreference)                 | Preference row opening a bottom-sheet choice panel (requires `Scaffold`)                 | Single / multiple choice settings      |
| [RecommendedPreference](../components/recommendedpreference)   | Rounded card recommending related settings                                               | Related settings recommendations       |
| [CardEntrancePreference](../components/cardentrancepreference) | Tappable card that opens another page (small / large layouts)                            | Feature entries, page entries          |
| [CardInstructionPreference](../components/cardinstructionpreference) | Card that walks through a feature with an illustration, title and description      | Onboarding, feature instructions       |
| [SliderPreference](../components/sliderpreference)             | Slider component based on BasicComponent                                                 | Value adjustment, volume/brightness    |
| [RangeSliderPreference](../components/rangesliderpreference)   | Range slider component based on BasicComponent                                           | Range selection, price filter          |
| [StepperPreference](../components/stepperpreference)           | Stepper component based on BasicComponent                                                | Numeric settings, font size            |
| [EditTextPreference](../components/edittextpreference)         | Preference row editing its value in a bottom sheet (requires `Scaffold`)                 | Text settings, device name             |
| [InputPreference](../components/inputpreference)               | Card row whose input field is edited in place                                            | Text settings, device name             |
| [SpannablePreference](../components/spannablepreference)       | Preference row whose summary is an `AnnotatedString` with clickable spans                 | Legal notices, links                   |
| [StatusSwitchingPreference](../components/statusswitchingpreference) | Preference row whose trailing widget switches between text and icon               | Status readouts                        |
| [LoadInstallProgressPreference](../components/loadinstallprogresspreference) | Preference row with a download/install progress button                      | System update, downloads               |
| [MarkWithDividerPreference](../components/markwithdividerpreference) | Mark preference row separated from the title by a divider                          | Exclusive option groups                |
| [CheckBoxWithDividerPreference](../components/checkboxwithdividerpreference) | Checkbox preference row separated from the title by a divider              | Multi-select groups                    |
| [SwitchWithDividerPreference](../components/switchwithdividerpreference) | Switch preference row separated from the title by a divider                  | Switch groups                          |
| [LoadingPreferenceCategory](../components/loadingpreferencecategory) | Category header with a loading indicator                                          | Scanning, loading groups               |
| [CheckBoxPreferenceCategory](../components/checkboxpreferencecategory) | Category header with a checkbox                                                  | Group select-all                       |
| [PagerHeaderPreference](../components/pagerheaderpreference)   | Page-opening explanatory note                                                            | Page headers                           |
| [PagerFooterPreference](../components/pagerfooterpreference)   | Page-closing note with clickable spans                                                   | Page footers, legal text               |
| [AppInfoPreference](../components/appinfopreference)           | About-page header with icon, app name and version                                        | About pages                            |
| [BottomPreference](../components/bottompreference)             | Invisible bottom spacer for a settings list                                              | List bottom padding                    |
| [OverlayListPopup](../components/overlaylistpopup)             | List popup component based on BasicComponent (uses COUIPopupUtils; requires `Scaffold`) | Option selection, feature list         |
| [OverlayCascadingListPopup](../components/overlaycascadinglistpopup) | Two-level cascading list popup (uses COUIPopupUtils; requires `Scaffold`)               | Submenus, grouped action panels        |
| [OverlayDropdownPreference](../components/overlaydropdownpreference) | Dropdown selector based on BasicComponent (uses COUIPopupUtils; requires `Scaffold`)    | Option selection, feature list         |
| [OverlaySpinnerPreference](../components/overlayspinnerpreference) | Advanced selector based on BasicComponent (uses COUIPopupUtils; requires `Scaffold`)    | Advanced options, feature list         |
| [OverlayDropdownMenu](../components/overlaydropdownmenu)       | Action menu based on BasicComponent (uses COUIPopupUtils; requires `Scaffold`)          | Action menus, multi-select choices     |
| [OverlayIconDropdownMenu](../components/overlayicondropdownmenu) | Icon-button action menu (uses COUIPopupUtils; requires `Scaffold`)                      | Toolbar actions, overflow menu         |
| [OverlayIconCascadingDropdownMenu](../components/overlayiconcascadingdropdownmenu) | Icon-button cascading two-level menu (uses COUIPopupUtils; requires `Scaffold`)         | Toolbar actions with submenus          |
| [OverlayBottomSheet](../components/overlaybottomsheet)         | Bottom sheet based on BasicComponent (uses COUIPopupUtils; requires `Scaffold`)         | Bottom drawer, additional options      |
| [OverlayDialog](../components/overlaydialog)                   | Dialog window based on BasicComponent (uses COUIPopupUtils; requires `Scaffold`)        | Prompts, action confirmation           |
| [OverlayAlertDialog](../components/overlayalertdialog)         | Centred warning dialog variant (uses COUIPopupUtils; requires `Scaffold`)                | Destructive or blocking warnings      |
| [OverlayProgressDialog](../components/overlayprogressdialog)   | Horizontal progress dialog variant with a determinate bar                                | Progress of a long-running operation  |
| [LoadingDialog](../components/loadingdialog)                   | Loading dialog with spinner (Overlay and Window variants)                                | Blocking loading feedback              |
| [SecurityDialog](../components/securitydialog)                 | Security statement dialog (Overlay and Window variants)                                  | Privacy statement, security confirmation |
| [DialogButtonBar](../components/dialogbuttonbar)               | Dialog button bar that auto-stacks vertically when labels do not fit                      | Dialog actions                         |
| [WindowListPopup](../components/windowlistpopup)     | Window-level list popup component                                                        | Option selection, feature list         |
| [WindowCascadingListPopup](../components/windowcascadinglistpopup) | Window-level two-level cascading list popup                                              | Submenus, grouped action panels        |
| [WindowDropdownPreference](../components/windowdropdownpreference) | Window-level dropdown selector component                                                 | Option selection, feature list         |
| [WindowSpinnerPreference](../components/windowspinnerpreference)   | Window-level advanced selector component                                                 | Advanced options, feature list         |
| [WindowDropdownMenu](../components/windowdropdownmenu) | Window-level action menu component                                                       | Action menus, multi-select choices     |
| [WindowIconDropdownMenu](../components/windowicondropdownmenu) | Window-level icon-button action menu component                                           | Toolbar actions, overflow menu         |
| [WindowIconCascadingDropdownMenu](../components/windowiconcascadingdropdownmenu) | Window-level icon-button cascading two-level menu                                        | Toolbar actions with submenus          |
| [WindowBottomSheet](../components/windowbottomsheet) | Window-level bottom sheet component                                                      | Bottom drawer, additional options      |
| [WindowDialog](../components/windowdialog)           | Window-level dialog component                                                            | Prompts, action confirmation           |
| [WindowAlertDialog](../components/windowalertdialog) | Window-level centred warning dialog variant                                               | Destructive or blocking warnings      |
| [WindowProgressDialog](../components/windowprogressdialog) | Window-level horizontal progress dialog variant                                    | Progress of a long-running operation  |
| [GuideDialog](../components/guidedialog)             | Window-level first-run multi-page guide dialog                                           | Onboarding, first-run guides           |
