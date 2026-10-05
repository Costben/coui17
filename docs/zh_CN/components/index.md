# 组件库

COUI 提供了丰富的 UI 组件，严格遵循 OPPO ColorOS 设计语言。每个组件都经过精心设计，确保视觉与交互效果与 ColorOS 原生体验一致。

## 脚手架

| 组件                               | 描述           | 常见用途           |
| ---------------------------------- | -------------- | ------------------ |
| [Scaffold](../components/scaffold) | 应用的基础布局 | 页面结构、内容展示 |

::: warning 注意
Scaffold 组件为跨平台提供了一个合适的弹出窗口的容器。`OverlayDialog`、`OverlayDropdownPreference`、`OverlaySpinnerPreference`、`OverlayListPopup` 等组件都基于此实现弹出窗口，因此都需要被该组件包裹。
:::

## 基础组件

| 组件                                                       | 描述                   | 常见用途             |
| ---------------------------------------------------------- | ---------------------- | -------------------- |
| [Surface](../components/surface)                           | 基础容器组件           | 内容展示、背景容器   |
| [TopAppBar](../components/topappbar)                       | 应用顶部的导航栏       | 页面标题、主要操作   |
| [AppBarBlurOverlay](../components/appbarbluroverlay)       | 随滚动淡入的顶栏材质叠加层 | 覆盖页面的渐变模糊顶栏 |
| [NavigationBar](../components/navigationbar)               | 底部导航组件           | 主要页面切换         |
| [BottomFloatingAccessoryNavigationBar](../components/bottomfloatingaccessorynavigationbar) | 带两侧配件的悬浮胶囊导航栏 | 底部导航与快捷操作 |
| [BottomFloatingEditToolbar](../components/bottomfloatingedittoolbar) | 悬浮编辑工具栏       | 编辑操作、快捷工具   |
| [BottomFloatingToolbar](../components/bottomfloatingtoolbar) | 悬浮文本选择工具栏   | 文本选择操作、分组操作 |
| [NavigationPopupMenu](../components/navigationpopupmenu)   | 导航溢出菜单           | 导航栏放不下的项     |
| [HorizontalPopupMenu](../components/horizontalpopupmenu)   | 弹窗列表中的横向图标块 | 弹窗内的图标操作     |
| [NavigationRail](../components/navigationrail)             | 侧边导航组件           | 主要页面切换（大屏）  |
| [TabRow](../components/tabrow)                             | 水平标签页切换栏       | 内容分类浏览         |
| [BreadcrumbBar](../components/breadcrumbbar)               | 水平面包屑导航栏       | 文件路径导航、位置轨迹 |
| [Card](../components/card)                                 | 包含相关信息的容器     | 信息展示、内容分组   |
| [BasicComponent](../components/basiccomponent)             | 通用基础组件           | 自定义组件开发       |
| [Button](../components/button)                             | 触发操作的交互元素     | 表单提交、操作确认   |
| [LoadingButton](../components/loadingbutton)               | 内置加载状态的文本按钮 | 异步操作提交         |
| [IconButton](../components/iconbutton)                     | 图标按钮组件           | 辅助操作、工具栏     |
| [HighlightedIconButton](../components/highlightediconbutton) | 带光晕的工具栏图标按钮 | 工具栏返回键、三点菜单键 |
| [Chip](../components/chip)                                 | 可选中的胶囊筛选标签   | 内容筛选、标签选择   |
| [ChipTab](../components/chiptab)                           | 横向可滚动的页签栏     | 页签切换、分类浏览   |
| [CalendarView](../components/calendarview)                 | 带星期表头的月历网格   | 日期选择、按月浏览   |
| [FloatingButton](../components/floatingbutton)             | 快速拨号浮动按钮       | 快捷操作、功能入口   |
| [ExpandableList](../components/expandablelist)             | 可展开的分组列表       | 设置分组、分组内容   |
| [LockPatternView](../components/lockpatternview)           | 3x3 图案解锁九宫格     | 图案解锁、手势密码   |
| [NumericKeyboard](../components/numerickeyboard)           | 数字 PIN 键盘          | PIN 输入、密码弹窗   |
| [SimpleLockView](../components/simplelockview)             | 锁屏 PIN 圆点指示行       | 锁屏 PIN 输入     |
| [LockScreenPwdInput](../components/lockscreenpwdinput)     | 胶囊密码输入框            | 锁屏密码输入        |
| [Banner](../components/banner)                             | 自动轮播的卡片横幅     | 功能轮播、活动推广   |
| [SearchHistoryView](../components/searchhistoryview)       | 换行排列的搜索历史标签面板 | 搜索历史、最近查询 |
| [TouchSearchView](../components/touchsearchview)           | 带放大字母磁贴的 A–Z 索引栏 | 按字母快速定位列表 |
| [IntentSeekBar](../components/intentseekbar)               | 带次级进度层的滑块     | 媒体播放、缓冲进度   |
| [MarqueeText](../components/marqueetextview)               | 溢出时横向滚动的文本   | 长标签、跑马灯       |
| [NotificationSnackBar](../components/notificationsnackbar) | 通知样式 Snackbar      | 应用内通知、状态提示 |
| [TagView](../components/tagview)                           | 小型文字角标           | 「NEW」「BETA」标记  |
| [MaterialShadowEdge](../components/materialshadowedge)     | 材质边缘描边与内阴影                      | 悬浮工具栏、面板描边                    |
| [SpotLight](../components/spotlight)                       | 可移动的材质高光光源                      | 按压、拖动高光反馈                     |
| [PageIndicator](../components/pageindicator)               | 带当前位置标记的圆点指示器 | 分页位置、引导页   |
| [Text](../components/text)                                 | 展示各种样式的文字内容 | 标题、正文、描述文本 |
| [SmallTitle](../components/smalltitle)                     | 小型标题组件           | 辅助标题、分类标识   |
| [TextField](../components/textfield)                       | 接收用户文本输入       | 表单填写、搜索框     |
| [InputView](../components/inputview)                       | 卡片式输入框           | 表单填写、对话框输入 |
| [CodeTextField](../components/codetextfield)               | 分格验证码输入框       | 验证码 / OTP 输入    |
| [Switch](../components/switch)                             | 双态切换控件           | 设置项开关、功能启用 |
| [Checkbox](../components/checkbox)                         | 多选控件               | 多项选择、条款同意   |
| [RadioButton](../components/radiobutton)                   | 单选控件               | 单项选择、选项切换   |
| [Slider](../components/slider)                             | 调节值的滑动控件       | 音量调节、范围选择   |
| [NumberPicker](../components/numberpicker)                 | 垂直滚动数字选择器     | 时间选择、数量选择   |
| [DatePicker](../components/datepicker)                     | 日期滚轮选择器（年/月/日） | 日期选择、表单       |
| [LunarDatePicker](../components/lunardatepicker)           | 农历日期滚轮选择器（年/月/日） | 农历日期选择、表单 |
| [TimePicker](../components/timepicker)                     | 时间滚轮选择器（时/分） | 时间选择、闹钟       |
| [Stepper](../components/stepper)                           | 加减调节整数值的控件   | 数量调整、数值微调   |
| [ProgressIndicator](../components/progressindicator)       | 展示操作进度状态       | 加载中、进度展示     |
| [SearchViewAnimate](../components/searchviewanimate)       | 图标与输入框间磁吸切换的搜索栏 | 搜索入口、可折叠搜索栏 |
| [HintAnimationLayout](../components/hintanimationlayout)   | 轮播搜索栏提示文案 | 搜索栏提示、占位文案 |
| [LoadProgressButton](../components/loadprogressbutton)     | 带加载状态机的下载按钮 | 安装、下载、暂停、重试 |
| [PopupMenuRotation](../components/popupmenurotation)       | 旋转弹窗菜单的几何计算 | 横屏布局下的溢出菜单 |
| [ClickSelectMenu](../components/clickselectmenu)           | 在点击处弹出的选择菜单 | 在手指处弹出的行内菜单 |
| [InstallLoadProgress](../components/installloadprogress)   | 安装按钮与进度切换   | 应用安装、下载     |
| [SideNavigationBar](../components/sidenavigationbar)       | 可切换固定模式的响应式侧边抽屉 | 相册导航、平板侧栏 |
| [SidePaneLayout](../components/sidepanelayout)             | 贴内容可拖拽的侧面板 | 相册侧栏、导航面板 |
| [ListDetailView](../components/listdetailview)             | 响应式列表-详情容器（窄窗口单面板 / 宽窗口双面板） | 主从页面、平板分栏 |
| [Snackbar](../components/snackbar)                         | 底部临时消息条组件     | 状态提示、操作结果   |
| [TopTips](../components/toptips)                           | 页面顶部提示条         | 页面提示、公告       |
| [Tooltip](../components/tooltip)                           | 悬停或长按显示的简短标签 | 图标按钮标签、元素提示 |
| [Badge](../components/badge)                               | 锚点上的小型状态叠加     | 未读数、状态圆点       |
| [Icon](../components/icon)                                 | 图标展示组件           | 图标按钮、状态指示   |
| [FloatingActionButton](../components/floatingactionbutton) | 悬浮操作按钮           | 主要操作、快捷功能   |
| [FloatingToolbar](../components/floatingtoolbar)           | 悬浮工具栏             | 快捷操作、信息展示   |
| [Divider](../components/divider)                           | 内容分隔线             | 区块分隔、层次划分   |
| [PullToRefresh](../components/pulltorefresh)               | 下拉触发刷新操作       | 数据更新、页面刷新   |
| [SearchBar](../components/searchbar)                       | 执行搜索的输入框       | 内容搜索、快速查找   |
| [FullPageStatement](../components/fullpagestatement)       | 全屏用户协议声明页     | 首次启动协议、隐私声明 |
| [EmptyState](../components/emptystate)                     | 空状态占位页           | 搜索无结果、列表为空   |
| [CompleteState](../components/completestate)               | 操作完成结果页         | 操作结果、任务完成     |
| [ColorPalette](../components/colorpalette)                 | 网格调色盘（含透明度） | 主题设置、颜色选择   |
| [ColorPicker](../components/colorpicker)                   | 选择颜色的控件         | 主题设置、颜色选择   |
| [ColorSwatchPicker](../components/colorswatchpicker)       | 圆点色板选择器（选中描边环） | 主题设置、颜色选择   |
| [GridSpringChain](../components/gridspringchain)           | 网格按行错峰的弹簧链越界效果 | 相册网格、磁贴网格   |

## 扩展组件

| 组件                                                 | 描述                                                                               | 常见用途               |
| ---------------------------------------------------- | ---------------------------------------------------------------------------------- | ---------------------- |
| [ArrowPreference](../components/arrowpreference)               | 基于 BasicComponent 的带箭头组件                                                   | 指示可点击、导航提示   |
| [SwitchPreference](../components/switchpreference)             | 基于 BasicComponent 的开关组件                                                     | 设置项开关、功能启用   |
| [SwitchLoadingPreference](../components/switchloadingpreference) | 带加载状态的开关设置项                                                             | 异步生效的设置项开关   |
| [CheckboxPreference](../components/checkboxpreference)         | 基于 BasicComponent 的复选框组件                                                   | 多项选择、条款同意     |
| [RadioButtonPreference](../components/radiobuttonpreference)   | 基于 BasicComponent 的单选组件                                                     | 单项选择、选项切换     |
| [MarkPreference](../components/markpreference)                 | 带选中标记的单选行                                                                 | 单选列表、选项选择     |
| [ButtonPreference](../components/buttonpreference)             | 尾部带小型按钮的设置项                                                             | 设置列表内联操作       |
| [ListPreference](../components/listpreference)                 | 打开底部选择面板的设置项（需在 `Scaffold` 中使用）                                 | 单选 / 多选设置项      |
| [RecommendedPreference](../components/recommendedpreference)   | 推荐相关设置的圆角卡片                                                             | 相关设置推荐           |
| [CardEntrancePreference](../components/cardentrancepreference) | 可点击跳转的入口卡片（小卡 / 大卡两套布局）                                        | 功能入口、页面入口     |
| [CardInstructionPreference](../components/cardinstructionpreference) | 带插图、标题与描述的功能引导卡片                                            | 功能引导、使用说明     |
| [SliderPreference](../components/sliderpreference)             | 基于 BasicComponent 的滑块组件                                                     | 数值调节、音量/亮度    |
| [RangeSliderPreference](../components/rangesliderpreference)   | 基于 BasicComponent 的范围滑块组件                                                 | 范围选择、价格筛选     |
| [StepperPreference](../components/stepperpreference)           | 基于 BasicComponent 的步进器组件                                                   | 数值型设置、字号       |
| [EditTextPreference](../components/edittextpreference)         | 在底部面板中编辑取值的设置项（需在 `Scaffold` 中使用）                             | 文本设置、设备名称     |
| [InputPreference](../components/inputpreference)               | 就地编辑输入框的卡片行                                                             | 文本设置、名称         |
| [SpannablePreference](../components/spannablepreference)       | 摘要为带可点击 span 的 `AnnotatedString` 的设置项                                  | 法律条款、链接         |
| [StatusSwitchingPreference](../components/statusswitchingpreference) | 尾部挂件可在文本与图标间切换的设置项                                         | 状态展示               |
| [LoadInstallProgressPreference](../components/loadinstallprogresspreference) | 尾部带下载/安装进度按钮的设置项                                       | 系统更新、下载         |
| [MarkWithDividerPreference](../components/markwithdividerpreference) | 用细线与标题分隔标记的设置项                                                  | 互斥选项组             |
| [CheckBoxWithDividerPreference](../components/checkboxwithdividerpreference) | 用细线与标题分隔复选框的设置项                                        | 多选组                 |
| [SwitchWithDividerPreference](../components/switchwithdividerpreference) | 用细线与标题分隔开关的设置项                                              | 开关组                 |
| [LoadingPreferenceCategory](../components/loadingpreferencecategory) | 尾部带加载指示器的分组标题                                                  | 扫描中、加载中的分组   |
| [CheckBoxPreferenceCategory](../components/checkboxpreferencecategory) | 尾部带复选框的分组标题                                                    | 分组全选               |
| [PagerHeaderPreference](../components/pagerheaderpreference)   | 页面开头的说明文字                                                                 | 页面表头               |
| [PagerFooterPreference](../components/pagerfooterpreference)   | 页面结尾的说明文字（支持可点击 span）                                              | 页面脚注、法律文本     |
| [AppInfoPreference](../components/appinfopreference)           | 关于页顶部的图标 + 应用名 + 版本信息头                                              | 关于页面               |
| [BottomPreference](../components/bottompreference)             | 设置列表结尾的不可见底部占位                                                       | 列表底部留白           |
| [OverlayListPopup](../components/overlaylistpopup)             | 基于 BasicComponent 的列表弹窗组件（使用 COUIPopupUtils，需在 `Scaffold` 中使用） | 选项选择、功能列表     |
| [OverlayCascadingListPopup](../components/overlaycascadinglistpopup) | 二级级联列表弹窗组件（使用 COUIPopupUtils，需在 `Scaffold` 中使用）               | 子菜单、分组动作面板   |
| [OverlayDropdownPreference](../components/overlaydropdownpreference) | 基于 BasicComponent 的下拉选择器组件（使用 COUIPopupUtils，需在 `Scaffold` 中使用） | 选项选择、功能列表     |
| [OverlaySpinnerPreference](../components/overlayspinnerpreference) | 基于 BasicComponent 的高级选择器组件（使用 COUIPopupUtils，需在 `Scaffold` 中使用） | 进阶选项选择、功能列表 |
| [OverlayDropdownMenu](../components/overlaydropdownmenu)       | 基于 BasicComponent 的动作菜单组件（使用 COUIPopupUtils，需在 `Scaffold` 中使用） | 动作菜单、多选场景     |
| [OverlayIconDropdownMenu](../components/overlayicondropdownmenu) | 基于 IconButton 的动作菜单组件（使用 COUIPopupUtils，需在 `Scaffold` 中使用）     | 工具栏动作、更多菜单   |
| [OverlayIconCascadingDropdownMenu](../components/overlayiconcascadingdropdownmenu) | 基于 IconButton 的二级级联动作菜单组件（使用 COUIPopupUtils，需在 `Scaffold` 中使用） | 含子菜单的工具栏动作   |
| [OverlayBottomSheet](../components/overlaybottomsheet)         | 基于 BasicComponent 的底部抽屉组件（使用 COUIPopupUtils，需在 `Scaffold` 中使用） | 底部抽屉、附加选项     |
| [OverlayDialog](../components/overlaydialog)                   | 基于 BasicComponent 的对话弹窗组件（使用 COUIPopupUtils，需在 `Scaffold` 中使用） | 提示、确认操作         |
| [OverlayAlertDialog](../components/overlayalertdialog)         | 居中警示弹窗变体（使用 COUIPopupUtils，需在 `Scaffold` 中使用）                    | 破坏性操作、强提示     |
| [OverlayProgressDialog](../components/overlayprogressdialog)   | 横向进度弹窗变体，带确定性进度条                                                   | 长任务的进度反馈       |
| [LoadingDialog](../components/loadingdialog)                   | 带旋转加载指示的加载对话框（含 Overlay 与 Window 两种变体）                        | 阻塞式加载反馈         |
| [SecurityDialog](../components/securitydialog)                 | 安全声明对话框（含 Overlay 与 Window 两种变体）                                    | 隐私声明、安全确认     |
| [DialogButtonBar](../components/dialogbuttonbar)               | 对话框按钮栏，文案放不下时自动翻成纵排                                             | 对话框动作             |
| [WindowListPopup](../components/windowlistpopup)     | 窗口级列表弹窗组件                                                                 | 选项选择、功能列表     |
| [WindowCascadingListPopup](../components/windowcascadinglistpopup) | 窗口级二级级联列表弹窗组件                                                         | 子菜单、分组动作面板   |
| [WindowDropdownPreference](../components/windowdropdownpreference) | 窗口级下拉选择器组件                                                               | 选项选择、功能列表     |
| [WindowSpinnerPreference](../components/windowspinnerpreference) | 窗口级高级选择器组件                                                               | 进阶选项选择、功能列表 |
| [WindowDropdownMenu](../components/windowdropdownmenu) | 窗口级动作菜单组件                                                                 | 动作菜单、多选场景     |
| [WindowIconDropdownMenu](../components/windowicondropdownmenu) | 基于 IconButton 的窗口级动作菜单组件                                               | 工具栏动作、更多菜单   |
| [WindowIconCascadingDropdownMenu](../components/windowiconcascadingdropdownmenu) | 基于 IconButton 的窗口级二级级联动作菜单组件                                       | 含子菜单的工具栏动作   |
| [WindowBottomSheet](../components/windowbottomsheet) | 窗口级底部抽屉组件                                                                 | 底部抽屉、附加选项     |
| [WindowDialog](../components/windowdialog)           | 窗口级对话框组件                                                                   | 提示、确认操作         |
| [WindowAlertDialog](../components/windowalertdialog) | 窗口级居中警示弹窗变体                                                             | 破坏性操作、强提示     |
| [WindowProgressDialog](../components/windowprogressdialog) | 窗口级横向进度弹窗变体                                                       | 长任务的进度反馈       |
| [GuideDialog](../components/guidedialog)             | 窗口级多页首次引导对话框                                                           | 新手引导、首次使用引导 |
