# COUI 16 缺失组件清单（对照盘点）

> **状态回写（2026-09-30，ColorOS 17 阶段二收尾）**：本清单的多数条目**已落地**。
> 24 项已落地的代码位置、5 项沿用既有、8 族「不适用」的判定，以及 4 项仍未闭合的结论均已复核。
> 本文件保留 2026-07-26 的原始盘点口径，**不再逐条更新**，以免出现两处真相。

> **状态回写（2026-10-06，1.2.0）**：§5「状态完备性结论」里「记录待立项（组件级）」的条目，连同仅记于同表 Dialog 行的横向进度对话框，共 **8 项已于 1.2.0 落地**：Chip 关闭 X 形态、Slider / RangeSlider / VerticalSlider `secondaryProgress`、居中警示对话框、横向进度对话框、TopTips 自定义插槽、Button `Tiny` 尺寸档、CircularProgress `error` / `pause` 状态、`RecommendedPreference.enabled`。逐项代码位置见新增的 §5.1。
> 本文件的 2026-07-26 原始盘点口径与其余条目不变。

> 盘点日期：2026-07-26。对照基线：`coui-ui/src/commonMain/.../basic/`（32 个文件）+ `overlay/`、`window/`、`layout/` + `coui-preference/`（19 个文件）。
> 对照对象是 COUI / ColorOS 17 的组件全集与设计语言：先按设计语言列出应有的组件与状态，再与本仓库公开 API 逐项比对。
> 本文件只做盘点，不承诺 API 设计；实现时按 `AGENT.md` 的设计值溯源原则补齐 token。

---

## 一、缺失组件清单（按优先级排序）

优先级定义：**高** = 设置页 / 常规 app 高频出现；**中** = 特定场景常见；**低** = 小众或 Compose 已有廉价替代。

### 高优先级

| COUI 组件 | 功能 | 优先级 | 建议落点 |
| :--- | :--- | :--- | :--- |
| `COUIEditText`（line / 错误态 / 计数 / 密码 / 快删多形态） | 输入框全形态，详见 [第二节](#二textfield-输入框多形态差距专项) | 高 | `basic/TextField.kt` 扩展参数（backgroundMode / isError / counter / trailing 预制件） |
| `COUIInputView`（+`COUICardSingleInputView` / `COUICardMultiInputView`） | 「标题 + 输入框 + 错误提示 + 计数 + 密码钮」组合输入卡，COUI 表单标准形态 | 高 | `basic/`（新文件 `InputView.kt`，内部复用 TextField） |
| `COUILoadingButton` | 按钮加载态（文字→三点加载动画，禁点） | 高 | `basic/Button.kt` 增加 loading 形态或新 `LoadingButton.kt` |
| `COUIHintRedDot` | 红点 / 数字角标（Tab、导航、设置项右侧），带出现/消失弹性动画 | 高 | `basic/`（新 `Badge.kt`，供 NavigationBar/TabRow 复用） |
| `COUIDatePicker` / `COUITimePicker` / `COUITimeLimitPicker` / `COUILunarDatePicker` | 日期选择（年月日）、带日期的时间选择、时分选择、农历选择；仓库目前只有 NumberPicker 无任何日期时间组合件 | 高 | `basic/`（`DatePicker.kt` / `TimePicker.kt`，组合已有 NumberPicker） |
| `COUIDefaultTopTips`（+`COUIMarqueeTopTips` / `COUICustomTopTips`） | 页面顶部提示条卡片：图标 + 文案（可跑马灯）+ 关闭/操作按钮，设置页顶部提醒高频 | 高 | `basic/`（新 `TopTips.kt`） |
| `COUIRotatingDialogBuilder`（+水平进度对话框） | 加载中对话框（转圈 + 文案）与带横向进度条对话框 | 高 | `overlay/` + `window/`（Dialog 变体，复用 ProgressIndicator） |
| `COUISecurityAlertDialogBuilder` | 安全提示对话框：声明文案 + 「不再提醒」勾选 + 链接高亮 | 高 | `overlay/` + `window/`（Dialog 变体） |
| `COUISwitchLoadingPreference`（+`COUIStatusSwitchingPreference`） | 开关加载态设置项：点击后 Switch 位置转圈（WLAN/蓝牙标准交互） | 高 | `coui-preference/preference/`（SwitchPreference 扩展 loading 态） |
| `COUIMarkPreference`（+`COUIMarkWithDividerPreference`） | 单选「对勾」列表项（选中项右/左侧打勾，无圆圈），COUI 单选列表标准形态 | 高 | `coui-preference/preference/`（新 `MarkPreference.kt`） |
| `COUIInputPreference` / `COUIEditTextPreference` | 设置项内嵌输入框 / 点击弹输入对话框 | 高 | `coui-preference/preference/` |
| `COUIListPreference` / `COUIMultiSelectListPreference` / `COUIActivityDialogPreference` | 点击弹「底部面板/对话框列表」的单选、多选设置项（区别于已有的下拉 DropdownPreference） | 高 | `coui-preference/preference/`（复用 overlay BottomSheet） |
| `COUIChip` / `COUIChipGroup` | 胶囊筛选标签（可选中、可关闭、组内单/多选），搜索与筛选页高频 | 高 | `basic/`（新 `Chip.kt`） |
| `COUIFullPageStatement`（+`COUIUserStatementDialog` / `COUIIndividualStatementDialog`） | 全页隐私声明（标题 + 滚动正文 + 底部同意/退出双钮）与底部声明面板，App 首启合规必备 | 高 | `basic/`（页面级）+ `overlay/`（面板形态） |
| `COUIRecommendedPreference` | 页面底部「相关设置 / 其他人还搜索」推荐卡（圆角卡内多行链接） | 高 | `coui-preference/preference/` |
| `COUIButtonPreference` / `COUICardButtonPreference` | 设置列表中的整行按钮项（居中文字按钮 / 卡片按钮） | 高 | `coui-preference/preference/` |

### 中优先级

| COUI 组件 | 功能 | 优先级 | 建议落点 |
| :--- | :--- | :--- | :--- |
| `COUICodeInputView` | 验证码输入框（N 个方格、自动跳格、可密文） | 中 | `basic/`（`CodeInput.kt`） |
| `COUIToolTips` | 带箭头气泡提示（纯文字 / 带图标 / 带图片三种样式，可指向任意锚点） | 中 | `basic/`（依托 Popup 定位） |
| `COUIPageIndicator`（/`COUIPageIndicator2`） | 轮播/引导页圆点指示器（蠕虫动画、可点击） | 中 | `basic/`（`PageIndicator.kt`） |
| `COUISearchViewAnimate` / `COUISearchBar` 磁吸形态 | 搜索栏进入/退出动画形态：点击后上浮进 TitleBar、键盘联动（我们 SearchBar 只有静态形态） | 中 | `basic/SearchBar.kt` 增强 |
| `COUINotificationSnackBar` | 通知样式 Snackbar（图标 + 标题 + 副文本 + 操作，浮窗观感） | 中 | `basic/Snackbar.kt` 变体 |
| `COUIFloatingButton` | 可展开 FAB（speed-dial：主钮展开若干带标签子钮，我们 FAB 仅单钮） | 中 | `basic/FloatingActionButton.kt` 扩展 |
| `COUIInstallLoadProgress`（+`COUILoadInstallProgressPreference`） | 按钮式下载/安装进度（按钮即进度条，文字随进度变色） | 中 | `basic/`（`InstallProgressButton.kt`） |
| `COUITouchSearchView` | 侧边字母索引条（触摸弹出首字母气泡，联系人/应用列表） | 中 | `basic/` |
| `COUISearchHistoryView`（+`COUIFlowLayout`） | 搜索历史流式标签面板（自动换行、展开/收起、删除） | 中 | `basic/`（FlowRow 组合） |
| `COUIExpandableRecyclerView` | 可展开分组列表（组展开/收起动画，设置「更多」分组） | 中 | `coui-preference/`（展开分组容器） |
| `COUICalendarDayPickerView`（日历） | 月历视图选择器（月翻页 + 年列表），`COUICalendarPicker` 实为此家族 | 中 | `basic/`（`CalendarPicker.kt`） |
| `COUIBanner` | 轮播 Banner（自动轮播 + 页码指示点，圆角卡片） | 中 | `basic/`（依托 Pager + PageIndicator） |
| `COUIEmptyStateView` | 空状态页（插画/动画 + 主副文案 + 操作按钮） | 中 | `basic/`（`EmptyState.kt`） |
| `COUILockPatternView` | 九宫格图案解锁（错误红显、路径动画） | 中 | `basic/` |
| `COUINumericKeyboard`（+`COUISimpleLock`） | 安全数字键盘（圆形按键、按压动效）与 PIN 圆点显示 | 中 | `basic/` |
| `COUICardInstructionPreference` / `COUICardEntrancePreference` | 页首图文说明卡（插图轮播 + 描述）/ 宫格入口卡 | 中 | `coui-preference/preference/` |
| `COUISlideView` | 列表项侧滑菜单/侧滑删除（含删除动画） | 中 | `basic/`（`SwipeItem.kt`） |
| `COUITagView` | 小型标签角标（如「新」「BETA」，文字底色块） | 中 | `basic/`（可并入 Badge） |
| `COUIButtonBarLayout` 推荐按钮档 | 对话框按钮栏的「推荐按钮」形态：某一按钮提升为高亮填充主按钮。**已于 2026-09-30 落地**：`DialogButtonBarAction.recommended` + `DialogButtonBar(recommendColors = ButtonDefaults.textButtonColorsPrimary())`，对齐「主按钮 / 次按钮」分流与文案色 / 实心填充两种推荐样式。**订正**：早先记「其存在会强制整栏纵排」有误 —— 纵排只取决于栏宽与按钮数，与是否有推荐按钮无关；推荐按钮只影响单按钮纵排栏的顶边距。AI 支付档依赖系统材质特性开关，**不落地** | 中 | `layout/DialogButtonBar.kt`（已实施） |
| `COUIStepperPreference` | Stepper 设置项包装（Stepper 本体已有） | 中 | `coui-preference/preference/` |
| `COUIMarqueeTextView` | 跑马灯文本（自动滚动、渐隐边缘） | 中 | `basic/Text.kt` 变体或 modifier |
| `COUICompleteStateView` | 操作完成状态页（对勾动画 + 文案） | 中 | `basic/`（可与 EmptyState 同族） |
| `COUIAppInfoPreference` | 「关于页」应用信息头（图标 + 名称 + 版本） | 中 | `coui-preference/preference/` |

### 低优先级

| COUI 组件 | 功能 | 优先级 | 建议落点 |
| :--- | :--- | :--- | :--- |
| `COUILockScreenPwdInputLayout` / `COUILockScreenPwdInputView` | 锁屏密码输入组合（输入框 + 确认圆钮，内阴影/光效） | 低 | `basic/`（如做锁屏场景再上） |
| `COUIRotateView` | 展开/收起旋转箭头（180° 旋转动画的 ImageView） | 低 | Compose `rotate` 动画即可，随展开列表一并做 |
| `COUIRoundImageView` | 圆角/圆形图片（带描边） | 低 | Compose `clip` 即可，不建议单独建组件 |
| `COUIContextMenu` / `COUIIsolatedPopupListWindow` | 指针位置上下文菜单（长按/右键处弹出，区别于锚点下拉） | 低 | `coui-preference/popup/` 增强 |
| `COUISidePaneLayout` / `COUISideNavigationBar` | 平板/折叠屏侧栏分栏布局、侧导航 | 低 | 大屏适配专项再评估 |
| `COUIListDetailView` / `COUIPercentWidth*` / `COUIGridLayout` | 响应式栅格与 list-detail 分栏容器 | 低 | `layout/`（响应式规则可先做 util） |
| `COUIChipGroupEditText` | 输入框内嵌 Chip（收件人式输入） | 低 | Chip 落地后再评估 |
| `COUILottieLoadingView` | 系统默认的旋转加载指示器（动画本体只是一个椭圆 + 一条 trim path） | **已做** | **本轮已落地为 `basic/ProgressIndicator.kt` 的 `RotatingProgressIndicator`**。原判「不建议做（依赖 Lottie，平台受限）」有误：该动画只是一个 26×26 椭圆 + 单条 trim path，可用一个 `drawArc` 完整复刻，**不需要 Lottie 依赖**，且它正是 COUI 绑定的系统默认加载指示器 |
| `COUICompProgressIndicator` | 加载指示器 + 文案组合件 | 低 | `RotatingProgressIndicator` + Text 组合即可（注意它承载的是**裸圆弧旋转器**而非圆环+圆点形态，且对尺寸有 40dp 上限校验，超出会打 warning） |
| `COUISlideSelectPreference` | 点击后在当前值处弹出对齐选择列表 | 低 | 已有 SpinnerPreference 近似覆盖，观感差异再校准 |
| `COUILoadingPreferenceCategory` | 分组标题右侧带加载圈（扫描中…） | 低 | `basic/SmallTitle.kt` 扩展 |
| `COUIPagerHeaderPreference` / `COUIPagerFooterPreference` / `COUIBottomPreference` / `COUISpannablePreference` | 列表首尾留白、富文本说明等杂项 preference | 低 | `coui-preference/`（按需） |
| `COUICheckBoxWithDividerPreference` / `COUISwitchWithDividerPreference` | 带竖分隔线的复合行（左区点击进入、右区独立开关） | 低 | `coui-preference/preference/` |
| `COUIOpenSourceStatement` | 开源声明长文页 | 低 | 不建议单独做 |
| `COUIBottomFloatingToolbar` | 文本选择浮动工具条（横排操作 + 溢出菜单） | 低 | 已有 `basic/FloatingToolbar.kt` 近似覆盖，按需校准 |

---

## 二、TextField 输入框多形态差距专项

我们现状：`basic/TextField.kt` 只有「圆角矩形背景 + 浮动 label + leading/trailing 槽位」一种形态（`TextFieldDefaults.CornerRadius`，rect 模式）。COUI 的输入框背景形态枚举为 `none(0) / line(1) / rectangle(2) / noLine(3)`。逐形态对照：

| 形态 | 说明 | 我们现状 | 结论 |
| :--- | :--- | :--- | :--- |
| rect 模式（圆角矩形背景） | 圆角矩形背景 | TextField 默认即此 | 已覆盖 |
| line 模式（下划线，聚焦下划线变色加粗动画） | 下划线输入框 | 无 | **缺失**：TextField 增加 `backgroundMode` 参数 |
| none / noLine 模式（无背景裸输入） | 裸输入 | 可用透明色勉强模拟，无显式 API | **缺失**（低成本，随 backgroundMode 一并做） |
| 错误态（红描边/红下划线 + 内容抖动动画 + 错误文案变色） | 错误反馈 | 无 `isError` 参数 | **缺失**：`isError` + 抖动动画 + `errorColor` 色槽 |
| 字数统计（右下 `当前/上限`，超限变红） | 计数 | 无 | **缺失** |
| 密码可见切换（眼睛钮，密文/明文切换） | 密码 | 需调用方自己塞 trailingIcon | **缺失**（提供预制 trailing 组件） |
| 快速删除（聚焦且非空时显示 ×，一键清空） | 清空 | 无 | **缺失**（预制 trailing 组件） |
| 组合输入 `COUIInputView`（上方标题/必填星号 + 输入 + 错误行 + 计数 + 密码钮） | 组合输入卡 | 无 | **缺失**（新组件，见第一节高优先级） |
| 卡片输入 `COUICardSingleInputView` / `COUICardMultiInputView` | 卡片式输入 | 无 | **缺失** |
| 验证码 `COUICodeInputView`（N 格自动跳格） | 验证码 | 无 | **缺失**（新组件） |
| 多行滚动 `COUIScrolledEditText` | 多行 | `lineLimits` + `scrollState` 已覆盖 | 已覆盖 |
| Chip 输入 `COUIChipGroupEditText` | 输入框内嵌 Chip | 无 | 缺失（低优先级） |

> 注意：miuix TextField 的「浮动 label」是 MIUI 特性；COUI 输入框无浮动 label，用 hint + `COUIInputView` 的上方独立标题。COUI 化时需评估默认关闭浮动动画。

---

## 三、组件命名澄清

少数被点名的类名在 COUI 里并没有对应物：`COUIButtonGroup` 实际对应的是按钮栏布局 + 按钮组控制类；`COUIBottomTips` 不存在（提示条家族只有 TopTips，底部浮条另有 bottomfloatingtoolbar）；`COUICalendarPicker` 无此确切类名，家族是 `COUICalendarDayPickerView` 等。`COUISegmentButton` 实际名为 `COUISegmentButtonLayout`，已由 `basic/TabRow.kt` 的 `TabRowWithContour` 覆盖。

---

## 四、已覆盖对照（无需新建，仅后续观感校准）

| COUI 能力 | 本仓库对应 | 备注 |
| :--- | :--- | :--- |
| Button 家族 | `basic/Button.kt` / `TextButton` | 缺 loading 形态（见高优先级） |
| Switch 家族 | `basic/Switch.kt` | |
| Checkbox 家族（三态） | `basic/Checkbox.kt` | 已含 `Indeterminate` 三态（对标 COUI 的三态勾选资产） |
| Slider / SeekBar 家族（含分段、竖向） | `basic/Slider.kt`（`steps`/`keyPoints`）+ `VerticalSlider` | |
| ProgressIndicator 家族（Linear / Circular / Infinite） | `basic/ProgressIndicator.kt` | 缺 `COUIInstallLoadProgress`（见中优先级） |
| Stepper 家族 | `basic/Stepper.kt` | 已按 COUI 对齐 |
| NumberPicker 家族 | `basic/NumberPicker.kt` | 日期/时间组合件缺失（见高优先级） |
| TabLayout / SegmentButton | `basic/TabRow.kt` / `TabRowWithContour` | TabRow 缺可滚动 Tab + Tab 红点，随 Badge 一并补 |
| Toolbar | `basic/TopAppBar.kt` | 溢出 action menu 依赖 ListPopup 组合 |
| NavigationView / NavigationRail | `basic/NavigationBar.kt` / `NavigationRail.kt` | 角标待 Badge |
| Snackbar | `basic/Snackbar.kt`（含 action） | 缺通知样式变体（见中优先级） |
| CardView / 卡片分组 | `basic/Card.kt` + `BasicComponent` 分组 | 分组圆角/按压态已具备，观感差异走换肤 |
| AlertDialog 家族 | `overlay/OverlayDialog.kt` + `window/WindowDialog.kt` | 缺 rotating 变体（见高优先级）；security 变体已有 `SecurityDialogLayout.kt`；按钮栏纵横自适应与**推荐按钮档**均已由 `layout/DialogButtonBar.kt` 覆盖（2026-09-30） |
| BottomSheetDialog | `overlay/OverlayBottomSheet.kt` + `window/WindowBottomSheet.kt` | 列表面板变体随 ListPreference 做 |
| 弹出列表 / 级联子菜单 | `basic/ListPopup.kt`、`Dropdown.kt` + `coui-preference` menu/popup 全家 | 指针位置 ContextMenu 低优先级 |
| ScrollBar | `basic/ScrollBar.kt` | |
| SearchView | `basic/SearchBar.kt` | 动画磁吸形态见中优先级 |
| preference 家族（Switch / Checkbox / Jump / Menu / Category / Slider） | `coui-preference` 的 SwitchPreference / CheckboxPreference / ArrowPreference / Dropdown&SpinnerPreference / `basic/SmallTitle.kt` / SliderPreference | |
| FloatingButton（单钮部分） | `basic/FloatingActionButton.kt` | 展开 speed-dial 见中优先级 |
| 弹性过滚动（scrollview / scroll） | `utils/Overscroll.kt` 等 | 行为族，无需组件 |
| ViewPager2 | Compose `Pager` | 无需自建 |
| RotateView / ImageView / TextView / TintImageView | Compose 原语 | 无需自建 |

---

## 五、状态 / 变体缺口（2026-07-26 横向盘点）

> 对照方法：逐个读本仓库 `basic/` + `coui-preference/` 公开 API 的参数集，对照 COUI 的样式变体、状态色与布局。只列「状态/变体」维度，整组件缺口见第一节。
> 标注：✅ = 本轮已实施；📝 = 仅记录（组件级改动，另行立项）。

### 已实施（✅，2026-07-26）

| 组件 | 补齐内容 | 落点 |
| :--- | :--- | :--- |
| Button | Small 尺寸档常量：`MinWidthSmall` 52dp / `MinHeightSmall` 28dp / `CornerRadiusSmall` 14dp（随高度胶囊）/ `InsideMarginSmall` 12dp/4dp；`TextButton` 新增 `textStyle` 参数（小档配 14sp） | `basic/Button.kt` |
| ProgressIndicator | large 档常量：`LargeCircularProgressIndicatorSize` 40dp / `...StrokeWidth` 5dp；`LargeInfiniteProgressIndicatorSize` 26dp / `...StrokeWidth` 3.33dp | `basic/ProgressIndicator.kt` |
| Snackbar | `Snackbar` 新增 `icon` 前置图标槽（30dp 盒 + 16dp 纵向净空 + 16dp 图文间距），经 `SnackbarHost` 的 `content` 插槽使用 | `basic/Snackbar.kt` |
| DialogButtonBar | 新增公开 `DialogButtonBar` + `DialogButtonBarAction` + `DialogButtonBarDefaults`，实现**自动纵排**：按测量判定「文案放得下 且 恰好 2 个按钮 且 无推荐按钮」才横排，否则整栏翻纵排；文案用 `TextMeasurer` 单行测量，可用宽度按栏宽（夹到 392dp）按按钮数均分、减分割线、再减两侧 24dp。纵排度量：52dp 最小高（最底部 64dp = +12dp）、14dp 垂直内边距（顶部无内容时 +6dp、最底部 +12dp）、栏上边距 16dp（>1 按钮时）、0.33dp 分割线内缩 24dp；顺序按反转（纵排 neutral/positive/negative，横排 negative/neutral/positive）；单独 negative 按钮回退横排度量（58dp）。已接入 `SecurityDialogLayout`、示例 app、两个 docs demo | `layout/DialogButtonBar.kt`（新文件） |

### 已实施（✅，2026-10-06，1.2.0）

#### 5.1 1.2.0 落地清单

| 组件 | 补齐内容 | 代码位置与符号 |
| :--- | :--- | :--- |
| Chip | 关闭 / 移除 X 形态，独立第二触摸目标 | `basic/Chip.kt`：`Chip(onDismiss = …)`、`ChipDefaults.DismissButtonSize`（48dp）、`DismissIconSize`（16dp）、`DISMISS_CONTENT_DESCRIPTION`；dismiss 是独立 `Role.Button`，不改变 `selected` |
| Slider | Intent 档 / 第二进度层 | `basic/Slider.kt`：`Slider.secondaryProgress` / `VerticalSlider.secondaryProgress` / `RangeSlider.secondaryProgress`；`SliderColors.secondaryProgressColor` / `disabledSecondaryProgressColor` |
| Dialog · 居中警示 | 居中警示对话框形态 | `layout/AlertDialogLayout.kt`：`AlertDialogContentLayout` / `AlertDialogDefaults` / `AlertDialogColors`；`overlay/OverlayAlertDialog.kt`、`window/WindowAlertDialog.kt` |
| Dialog · 横向进度 | 横向进度对话框形态 | `layout/ProgressDialogLayout.kt`：`ProgressDialogContentLayout` / `ProgressDialogDefaults` / `ProgressDialogColors`；`overlay/OverlayProgressDialog.kt`、`window/WindowProgressDialog.kt` |
| TopTips | 自定义消息插槽 | `basic/TopTips.kt`：`TopTips(content = …)`；跑马灯沿用既有 `marqueeEnabled`（核心 `basic/MarqueeTextView.kt`） |
| Button | `Tiny` 尺寸档 | `basic/Button.kt`：`ButtonSize.Tiny`、`ButtonDefaults.MinWidthTiny`（44dp）/ `MinHeightTiny`（20dp）/ `CornerRadiusTiny`（10dp）/ `InsideMarginTiny` |
| ProgressIndicator | `error` / `pause` 状态 | `basic/ProgressIndicator.kt`：`CircularProgressState.Error` / `.Paused`、`CircularProgressIndicator(state = …)`、`ProgressIndicatorDefaults.circularProgressIndicatorColors()`（`errorColor` / `onErrorColor`） |
| RecommendedPreference | `enabled` + 禁用视觉态 | `coui-preference/src/commonMain/kotlin/io/github/suqi8/coui/kmp/preference/RecommendedPreference.kt`：`RecommendedPreference(enabled = …)`、`RecommendedPreferenceColors.disabledTitleColor` / `disabledItemColor` |

### 状态完备性结论（逐组件）

| 组件 | 结论 | 备注 |
| :--- | :--- | :--- |
| Button / TextButton / LoadingButton | 基本完备 | primary / secondary / borderless / disabled / loading（LoadingButton，含 loadingText 三点动画）齐；Small 档本轮补齐。COUI **无描边 outline 按钮**，无需补。📝 剩余：Tiny 色系变体（`Large.TinyFull` / `Small.TinySmall` / `.HalfColor` 半色调 / `.Translate` 透明，中性灰按钮 + 直角圆角 + 专用文字色 selector），属颜色/圆角预设族。✅ **1.2.0 已落地** `Tiny` 尺寸档；本行原记的「Tiny 色系变体」是另一项，仍未落地。 |
| Switch | 完备 | disabled / loading / disabled+checked 全有；loading 时吞点击与 COUI 一致 |
| Checkbox | 完备 | On / Off / Indeterminate 三态 + disabled 全套颜色 |
| RadioButton | 完备 | enabled/disabled 全套（含 disabledSelectedColor） |
| Slider / VerticalSlider | 大体完备 | disabled / steps（= 分段档，其样式仅关 physics）/ keyPoints / 竖向齐。📝 原缺 **Intent 档**（第二进度层 + 4dp 固定轨道）与 `couiSeekBarStartMiddle`（从中点起算的双向滑条）无对应参数。✅ **1.2.0 已落地** `secondaryProgress` 第二进度层，镜像到 `Slider` / `VerticalSlider` / `RangeSlider`；双向滑条仍无对应参数。 |
| Chip | 选中态完备 | selected / unselected / disabled×2 齐。**无下拉箭头 chip**，无需补。📝 原缺关闭 X 形态（Suggestion 与 Input 两档尺寸、第二触摸目标）与 Icon 档。✅ **1.2.0 已落地** 关闭 X 形态：`Chip(onDismiss = …)` 及 `ChipDefaults.DismissButtonSize`；实现为通用 48dp 触达目标，未逐条复刻 Suggestion / Input 两档尺寸。 |
| Badge | 完备 | dot / 数字（1..999 三档宽度动画）/ 1000+ 省略号 / stroke 描边形态 / BadgeBox 锚定全有 |
| ProgressIndicator | 大体完备 | 定值/不定值/large 档齐（本轮补 large）。**本轮补齐 `RotatingProgressIndicator`**：COUI 系统默认的不确定态旋转指示器（无背景环的裸圆弧、平头端点、1250ms 周期内转两整圈、弧长 273.6°↔50.4° 脉动），用单个 `drawArc` 实现、不依赖 Lottie。注意 COUI 的加载指示器**有两种形态并存**：强调色「圆环 + 环绕圆点」（刷新中状态用，见 `InfiniteProgressIndicator`）与中性色裸圆弧旋转器。📝 原缺 error / pause 图标态（下载场景用）。✅ **1.2.0 已落地** `error` / `pause` 态：枚举 `CircularProgressState.Error` / `.Paused` + `CircularProgressIndicator(state = …)`。 |
| Stepper / NumberPicker / DatePicker / TimePicker | 完备 | 均有 `enabled` + disabled 颜色（Stepper 到边界还会单侧禁用）；NumberPicker disabled 吞手势 |
| Snackbar | 大体完备 | action / dismiss / 单行大圆角自适应齐；图标槽本轮补齐。📝 通知样式 `COUINotificationSnackBar` 见第一节中优先级 |
| TopTips | 大体完备 | 关闭型 / 按钮型互斥 + 图标 + 折行降级齐。📝 原缺跑马灯与全自定义插槽形态。✅ **1.2.0 已落地** 自定义消息插槽（`TopTips(content = …)`）；跑马灯沿用既有 `marqueeEnabled` / `basic/MarqueeTextView.kt`。 |
| preference 系 | 完备（一处例外） | 全部 preference 均带 `enabled` 且经 `BasicComponent` 将 title/summary 变灰、widget（Switch/Checkbox/箭头/对勾）走各自 disabled 色。例外：`RecommendedPreference` 原先无 `enabled` 参数。✅ **1.2.0 已落地** `RecommendedPreference(enabled = …)` + `RecommendedPreferenceColors` 的禁用色槽。 |
| Dialog / BottomSheet | 形态缺口 | Loading（rotating）/ Security 双宿主（overlay+window）齐；列表单选/多选由 List/MultiSelectListPreference 覆盖。📝 原缺**居中警示形态**（含 `.Tiny` / `.Rotate` 子形态）与横向进度对话框。✅ **1.2.0 两项均已落地**：居中警示与横向进度，代码位置见 §5.1；`.Tiny` / `.Rotate` 子形态未移植。 |

### 本轮结论摘要

- 确凿缺失且已实施：Button Small 尺寸档、ProgressIndicator large 档、Snackbar 图标槽（3 项，均为小改）。
- 记录待立项（组件级）：Chip 关闭 X 形态、Slider Intent 档（secondaryProgress）、Dialog 居中警示形态、TopTips 跑马灯、Button Tiny 色系、CircularProgress error/pause 态、RecommendedPreference enabled。
- **1.2.0（2026-10-06）已闭合**：上列「记录待立项（组件级）」条目连同仅记于 Dialog 行的横向进度对话框共 8 项全部落地，逐项代码位置见 §5.1。其中 Button 落地的是 `Tiny` 尺寸档，原记的「Tiny 色系变体」仍属另一项、未落地。
- 证实无需补：Button outline 描边变体、Chip 下拉箭头变体（COUI 均无此物）；Switch「disabled+checked」视觉已有专用色槽。
