# COUI AGENT Guide

本文件汇总后续 AI 在本仓库里应遵守的核心规则。本仓库基于 [Miuix](https://github.com/compose-miuix-ui/miuix) 改造，目标是**就地演进成一套 COUI（ColorOS 设计语言）Compose Multiplatform 组件库**。

> 工程规范（构建命令、代码风格、API 约定、性能约束）见 `CLAUDE.md`，那份仍然有效，写 COUI 组件时**必须同时遵守**。本文件只补充「COUI 改造」这条主线：项目定位、设计来源、设计与代码的映射。

## 1. 必读目标

后续 AI 在本仓库里的默认目标不是只分析，而是：

1. 把 Miuix（MIUI/HyperOS 观感）逐步换皮成 COUI（ColorOS 观感）
2. 设计 token（颜色 / 圆角 / 字号 / 控件尺寸 / 间距 / 动效）**必须可溯源到真实的 ColorOS 设计语言**，不靠拍脑袋
3. 信息不足时，先核对 ColorOS 的设计规范与参考机型上的实际观感，再落代码
4. 直接完成代码实现，而不是停在「研究完了」
5. 改完做最小自检（编译 + 至少一个平台跑通）

除非用户明确只要分析，否则不要停在结论阶段。

## 2. 项目定位（已确认）

- 本仓库 fork 自上游 Miuix（[`compose-miuix-ui/miuix`](https://github.com/compose-miuix-ui/miuix)），是改造基底；<br>改造完成后以 [`suqi8/coui`](https://github.com/suqi8/coui) 为唯一对外仓库，Maven 坐标为 `io.github.suqi8.coui.kmp`。
- **定位：就地改造成 coui 库。** Miuix 作为基底，不再单独对齐上游；最终可改包名 / Maven 坐标为 coui。
- **改动深度：主题换肤优先（渐进）。** 先调 `theme/` 层默认值让现有组件整体呈现 COUI 观感，之后再按需逐个深挖组件。先不做大规模组件重构，除非某个组件的结构/交互和 ColorOS 差异大到换肤补不平。
- 目标设计系统：**COUI 16 起步、对齐 ColorOS 17 的设计语言**（仓库目录名 `COUI16` 即源于早期目标）。

## 3. 设计来源与溯源原则

设计值（颜色 / 圆角 / 字号 / 控件尺寸 / 间距 / 动效）应尽量对齐 ColorOS 的设计语言与参考机型上的实际观感：

- **首选依据**：ColorOS 公开的设计语言规范与 ColorOS 17 参考机型上的实际观感（界面截图取色、运行时读数）。
- **次选依据**：本仓库既有的同族组件取值——同一控件族内保持一致，不要为单个组件另立一套。
- **结论要求**：每个 token 都要能说清「为什么是这个值」，而不是「某个数看起来差不多」。
- **冲突处理**：静态设计值与运行时观感冲突时，以运行时观感为准（动态取色 / 主题会覆盖静态默认值）。

本文件只讨论设计依据与落地方式，不记录任何工具链布局、设备连接方式或资源提取流程。

## 4. Miuix → COUI 的换肤抓手（先看清，再动手）

Miuix 的视觉语言**高度集中在 `theme/` 层**，组件全部走 `COUITheme.colorScheme.*` / `COUITheme.textStyles.*` 取色取字，从不硬编码颜色 —— 这是换肤的核心利好，改默认值即可全局生效。

| 抓手 | 文件 | 改什么 |
| :--- | :--- | :--- |
| 配色 | `coui-ui/src/commonMain/kotlin/io/github/suqi8/coui/kmp/theme/Colors.kt` | `lightColorScheme()` / `darkColorScheme()` 的默认色值（约 50 个色槽）。当前主色是 HyperOS 蓝 `#3482FF`，COUI 应换成 ColorOS 主色 |
| 字体 | `…/theme/TextStyles.kt` | `Main/Body/Title/...` 的 `fontSize` / `fontWeight` / `lineHeight`。当前主字号 17sp |
| 形状 | 各组件 `ComponentDefaults.CornerRadius` + `coui-squircle/` | COUI 圆角偏大且是连续曲率（squircle）；本仓库已内置 `coui-squircle`，校准 squircle 平滑度参数即可 |
| 动效 | `…/anim/` | 缓动曲线 / 时长 |

**取色取字断言（来自真实代码，写组件时照此模式）**：组件如 `basic/Button.kt` 通过 `ButtonDefaults.buttonColors()` 取 `COUITheme.colorScheme.secondaryVariant` 等，形状走 `Modifier.squircleSurface(color, cornerRadius)`。所以「换主色」= 改 `Colors.kt` 默认值；「换某控件配色」= 改该组件的 `XxxDefaults.xxxColors()` 默认引用的色槽；**任何情况下都不在组件里写死 `Color(0x...)`**。

> ⚠️ 命名现状：色槽 / 主题类目前仍叫 `COUITheme` / `Colors` / `lightColorScheme`。换肤阶段**不重命名**，只改值，避免一次性大改爆炸。重命名为 `CouiTheme` 等留到后续「改包名 / 坐标」专项再做。

## 5. 设计 token 必须先溯源的判定

满足任一条件时，**不要直接硬写色值 / 尺寸**，先找到可复核的真实依据：

- 需要 COUI 主色 / 强调色 / 语义色（错误红、链接蓝等）的精确值
- 需要某控件（开关、滑块、按钮、卡片、列表项、对话框）的真实圆角 / 尺寸 / 内边距
- 需要 ColorOS 字体梯度（标题 / 正文 / 脚注的 sp 与字重）
- 需要 squircle 平滑度 / 动效曲线参数
- 用户对某个组件「不像 ColorOS」提出具体异议

够明确的微调（如已确认主色后改一处引用）可直接改，不必每次都重新溯源。

## 6. 设计 token 的溯源与记录

找到可信依据后，按下面记录，方便后续复核：

- 目标组件 + 对标的 ColorOS 版本（如 ColorOS 16 / 17）
- 取到的 token：名称 → 值（亮 / 暗）
- 设计依据：来自设计语言规范、同族组件取值，还是参考机型实测
- 该 token 在本仓库里对应哪个色槽 / 哪个 `Defaults`（落点见 §7）
- 是静态设计值，还是运行时实测值（后者更可信）
- 未确认 / 待校准项

记录方式与工具无关：结论写进代码 KDoc 或对应的 `docs/components/` 文档即可。本仓库不维护环境路径或提取流程，也不要为空缺的设计值臆造来源。

## 7. token → 代码落点映射

找到 token 后，按下表落到代码（**只改值，不重命名**）：

| COUI token 类型 | Miuix 落点 | 备注 |
| :--- | :--- | :--- |
| 主色 / 强调色 | `Colors.kt` → `primary` / `primaryVariant` / `primaryContainer`（light + dark 两处） | 当前 `#3482FF`，替换为 ColorOS 主色 |
| 语义色（错误等） | `Colors.kt` → `error` / `errorContainer` 等 | light + dark 都要改 |
| 背景 / 表面层级 | `Colors.kt` → `background` / `surface` / `surfaceContainer*` | ColorOS 卡片层级感强，注意 container 梯度 |
| 文字色 | `Colors.kt` → `onBackground` / `onSurface*` | |
| 全局字号 / 字重 | `TextStyles.kt` → `Main` / `Body*` / `Title*` 等私有 `val` | |
| 控件圆角 | 对应组件的 `XxxDefaults.CornerRadius`（如 `ButtonDefaults.CornerRadius`） | 配合 squircle |
| 控件尺寸 / 内边距 | 对应组件的 `XxxDefaults.MinWidth/MinHeight/InsideMargin` | |
| squircle 平滑度 | `coui-squircle/` 模块参数 | 全局连续曲率 |
| 单控件配色 | 该组件 `XxxDefaults.xxxColors()` 默认引用的色槽 | 不在组件体内写死颜色 |

改完同一改动要顺带检查（见 `CLAUDE.md`「Modifying a Component」）：

1. 文档 `docs/components/` 与 `docs/zh_CN/components/`
2. 文档 demo `docs/demo/`
3. 示例 app `example/shared/src/commonMain/kotlin/component/`

## 8. 默认执行顺序

1. 明确目标：要改哪个视觉面（全局主色？某控件？字体梯度？）
2. 看现状：定位 §4 的抓手文件 / 对应 `Defaults`，确认当前值与取色路径
3. 判定是否需要溯源（§5）：需要就先按 §6 找到可复核的真实依据
4. 改代码（§7，只改值不重命名）
5. 同步文档 / demo / 示例 app（§7 末）
6. 最小自检（§9）

## 9. 最小自检清单

- 改的是 `theme/` 默认值或 `Defaults`，没有在组件体里硬编码 `Color(0x...)`
- light 和 dark **两套**色值都改了（只改一套是最常见遗漏）
- token 有可复核的真实依据（设计语言规范或参考机型实测），不是凭空写的
- 命名未被擅自改动（换肤阶段不重命名主题类 / 色槽）
- 关联文档 / demo / 示例 app 已同步（若改了组件 API 或默认值）
- 构建自检默认直接执行：
  - 快速：`./gradlew compileKotlinDesktop`
  - 完整：`./gradlew assemble`
  - 格式：先 `./gradlew spotlessCheck`，有违规才 `./gradlew spotlessApply`
- 至少在一个平台（Android 或 Desktop）肉眼确认观感更接近 ColorOS

## 10. 默认完成标准

当用户说「做 COUI 换肤 / 改成 ColorOS 风格」时，默认完成标准不是「分析完」，而是：

1. token 已有可复核的真实依据（设计语言规范 / 同族取值 / 参考机型实测）
2. 已落到 `theme/` 默认值或对应 `Defaults`，light/dark 双套
3. 编译通过，至少一个平台跑通并肉眼校准
4. 关联文档 / demo / 示例已同步（若涉及）
5. 关键依据与未校准项已在回复里说明

不要用泛泛「已完成」收尾；说明取到的 token 依据、改了哪些落点、还有哪些待校准。

## 11. 共享约束

- 沿用 Miuix 现有架构与命名，不发明新的模块结构 / 主题机制（换肤阶段尤其如此）。
- 引用真实的模块路径与公开类型，不给泛泛 Android / Compose 建议。
- 设计 token 不臆造，必须可溯源到 ColorOS 设计语言或参考机型实测。
- 静态设计值与运行时观感冲突时，以运行时观感为准（动态取色 / 主题会覆盖静态默认）。
- 写 / 改组件仍遵守 `CLAUDE.md` 的 API 约定、性能约束、`@Immutable`/`@Stable` 规则。
- 与用户用中文交流、用中文输出 Plan；生成的代码（含注释 / KDoc）保持英文。

## 12. COUI preference 组件（ColorOS 17 移植，2026-09-30）

`coui-preference/src/commonMain/kotlin/io/github/suqi8/coui/kmp/preference/` 本轮新增 15 个组件。
每项用法见 `docs/components/<名称小写>.md` 与 `docs/zh_CN/components/<名称小写>.md`。
示例落位：原复刻页 `SettingsScreen.kt` 已随 COS17 改版删除，本批组件当前没有展廊条目，只有 `docs/components/` 的文档。

| 组件 | 说明 |
| :--- | :--- |
| `StepperPreference` | 标题 + 数值步进器行（包装 `basic/Stepper.kt`），`MinValue` / `MaxValue` / `Step` 在 `StepperPreferenceDefaults` |
| `EditTextPreference` | 点击后从底部面板输入文本的行（`TextField` + 取消 / 确认按钮栏） |
| `InputPreference` | 行内嵌输入框（`TextField(backgroundMode = Line)`），可选字数上限与清除按钮 |
| `StatusSwitchingPreference` | 右侧状态文本 / 小图标的行（`StatusSwitchingContent.Text` / `.Icon`），可配 `bottomAction` |
| `SpannablePreference` | 富文本摘要行（`AnnotatedString`，可点链接），标题可有可无 |
| `BottomPreference` | 列表尾部留白（32dp `Spacer`） |
| `MarkWithDividerPreference` | 对勾单选行 + 1dp×22dp 竖分隔线 |
| `CheckBoxWithDividerPreference` | 复选行 + 竖分隔线 |
| `SwitchWithDividerPreference` | 开关行 + 竖分隔线 |
| `LoadingPreferenceCategory` | 分类标题 + 右侧加载指示器与文案 |
| `CheckBoxPreferenceCategory` | 分类标题 + 右侧三态 `Checkbox` |
| `LoadInstallProgressPreference` | 按钮式下载 / 安装进度行（包装 `basic/InstallLoadProgress.kt`） |
| `PagerHeaderPreference` | 页首说明行（32dp 水平内边距、12sp，可选标题） |
| `PagerFooterPreference` | 页脚说明行（`AnnotatedString` 摘要，默认 24dp 底边距） |
| `AppInfoPreference` | 「关于本机」应用信息块（squircle 图标 60dp / 名称 20sp / 版本 12sp） |

共用内部件：`internal/CategoryHeader.kt`（`SmallTitle` + 尾部 slot，供两个 `*Category` 复用）、
`internal/PreferenceWidgetDivider.kt`（`*WithDivider` 共用的竖线）。

## 13. COUI 侧栏 / 侧面板组件（ColorOS 17 移植，2026-09-30）

`coui-ui/src/commonMain/kotlin/io/github/suqi8/coui/kmp/basic/` 本轮新增 2 个组件，另在
`coui-ui/src/commonMain/kotlin/io/github/suqi8/coui/kmp/icon/basic/` 新增 1 个图标。
每项用法见 `docs/components/<名称小写>.md` 与 `docs/zh_CN/components/<名称小写>.md`，
示例落位：原复刻页 `GalleryScreen.kt` 与宿主 `GalleryActivity` 已随 COS17 改版删除，
本批组件当前没有展廊条目，只有 `docs/components/` 的文档。

| 组件 | 说明 |
| :--- | :--- |
| `SideNavigationBar` | 响应式侧边抽屉：紧凑窗口为临时抽屉（遮罩色、可点遮罩关闭），宽窗口转为固定栏（无遮罩、内容仍可交互）。配 `SideNavigationBarState` / `SideNavigationBarMode` / `SideNavigationBarListener` / `SideNavigationBarDefaults` / `rememberSideNavigationBarState` |
| `SidePaneLayout` | 起始边可拖拽侧面板：`coverStyle` 决定覆盖还是推开内容，`alwaysShowMask` 决定遮罩是否常显，折角按钮负责开合。配 `SidePaneLayoutState` / `SidePaneState` / `SidePanePanelSlideListener` / `SidePaneMaskListener` / `SidePaneUtils` / `SidePaneDefaults` / `SidePaneFoldIconColors` / `rememberSidePaneLayoutState` |
| `COUIIcons.Basic.FoldPane` | 折角图标的 1:1 移植（48×52 视口、1.4 圆头描边），供 `SidePaneLayout` 的折角按钮使用 |

注意：`SidePaneLayout` 在参考应用里没有找到调用点，属按设计语言重建；`SideNavigationBar` 有实际调用点。
因此 `SidePaneLayout` 的演示为按设计重建，非实测对齐。

## 14. COUI 卡片 / 网格弹簧链组件（ColorOS 17 移植，2026-09-30）

本轮在 `coui-ui/.../basic/` 新增 1 个网格越界组件，在 `coui-preference/.../preference/` 新增 2 个卡片组件。
每项用法见 `docs/components/<名称小写>.md` 与 `docs/zh_CN/components/<名称小写>.md`，
示例落位：原复刻页 `GalleryScreen.kt` 已随 COS17 改版删除，本批组件当前没有展廊条目。

| 组件 | 说明 |
| :--- | :--- |
| `GridSpringChain` | 网格越界弹簧链：`Modifier.gridSpringChainOverscroll(state)` 加在网格上度量并阻尼越界量，`Modifier.gridSpringChainItem(state, row)` 把控制距离换算为按行位移。配 `GridSpringChainState` / `GridSpringChainTranslation` / `GridSpringChainDefaults` / `rememberGridSpringChainState`。复用既有 `utils/Overscroll.kt`（`SpringMath.obtainDampingDistance`）与 `utils/SpringUtils.kt`（`SpringEngine.runSettleAnimation`），未新写弹簧积分器 |
| `CardEntrancePreference` | 可点击跳转的入口卡片，小卡（图标在上、12dp 圆角）/ 大卡（图标在左、16dp 圆角）两套布局。配 `CardEntranceType` / `CardEntranceTintIcon` / `CardEntrancePreferenceDefaults` / `CardEntranceColors` |
| `CardInstructionPreference` | 功能引导卡片，Description（`HorizontalPager` + `PageIndicator` 轮播）/ Selector（一行单选）两态。配 `CardInstructionItem` / `CardInstructionType` / `CardInstructionPreferenceDefaults` / `CardInstructionColors` |

常量来源（逐条回查设计依据，不臆造）：`GridSpringChain` 取最大距离系数 `0.25f` / 最大距离比 `0.5f` /
越界系数 `0.3f`、默认位移 `(row + 1) * 0.1f * distance`、主张力 / 主摩擦 `150f` / `10f`，另有
`40f/6f` 与 `70f/10f` 两组弹簧参数、装饰器系数 `0.3f` 与默认垂直边距 `24f`；两张卡片取小卡 12dp /
大卡 16dp 圆角、引导卡 16dp 圆角。

未移植项：弹簧链的 7 个顶层类与 3 个接口属 Android View 基础设施；`COUISpringChain` 的 tension/friction
无法一一映射到临界阻尼的 `SpringEngine`；卡片里的 `RecyclerView.ItemDecoration` / `SpanSizeLookup` /
`Adapter` 体系与 View 层基础设施库内已有等价覆盖。`CardEntrancePreferenceDefaults.IconSize = 32.dp`
是移植自选值（参考实现为 `wrap_content`），已在 KDoc 与文档注明；`GridSpringChainDefaults.ChainMarginVertical = 24f`
按参考值原样保留为 px 值。

## 15. COUI 列表-详情容器与农历日期选择器（ColorOS 17 移植，2026-09-30）

本轮在 `coui-ui/.../basic/` 新增 2 个组件，在 `coui-ui/.../utils/` 新增 1 个换算工具。
每项用法见 `docs/components/<名称小写>.md` 与 `docs/zh_CN/components/<名称小写>.md`。

| 组件 | 说明 |
| :--- | :--- |
| `ListDetailView` | 响应式列表-详情容器：窄于 600dp 时只组合 `mainContent` / `detailContent` 之一（由 `detailVisible` 决定），达到 600dp 后主面板占 40%、后接 0.33dp 细线、详情（未选中时为 `emptyPageContent`）占满剩余。配 `ListDetailViewDefaults`（`SplitModeMinWidth` 600dp / `MinMainWidth` 280dp / `MaxMainWidth` 412dp / `DividerWidth` 0.33000004dp），`isSplitMode(width)` 判定分栏 |
| `LunarDatePicker` | 农历日期滚轮：三列均宽 76dp，月列显示月名并带「闰」前缀，年 / 日列为纯数字。配 `LunarDatePickerDefaults`（`YearRange` 1910..2099 / `ColumnWidth` / `MonthNames` / `LeapPrefix` / `MonthLabel`） |
| `LunarUtil` + `LunarDateValue` | 农历换算工具的直接移植：`LUNAR_INFO`（1900..2100，201 项）与 `SOLAR_TERM_DAYS`（1970..2036，67×24）逐项转写，`calculateLunarByGregorian` / `lunarToSolar` / `toLunarDate` / `leapMonth` / `daysOfALunarMonth` / `chineseStringOfALunarDay` 等纯整数 API 独立可用 |

常量来源（逐条回查设计依据，不臆造）：`ListDetailView` 取主面板宽度占比 `0.4f`、主面板 min/max 宽
280 / 412dp、分栏细线宽 0.33dp，分栏阈值取紧凑 / 中等窗口的边界 600dp；`LunarDatePicker` 取农历年范围
1910..2099 与三列均宽 76dp。演示落位：原 `GalleryScreen.kt`（相册列表 → 照片详情）与 `SettingsScreen.kt`
（系统与更新 → 日期和时间，Dialog 宿主）已随 COS17 改版删除，这两个组件当前没有展廊条目。

未移植项：响应式栅格工具属 Android View 基础设施（库内已有等价物）；`LunarDatePicker` 的滚动时滚轮重排
未逐行照抄，且 `getLunarDateString(Calendar)` / `@Deprecated changeALunarYearByOne` 因 `java.util.Calendar`
入参在 commonMain 不适用。

## 16. COUI 顶栏材质叠加层、工具栏高亮按钮与底栏胶囊几何（ColorOS 17 移植，2026-09-30）

本轮在 `coui-ui/src/commonMain/kotlin/io/github/suqi8/coui/kmp/basic/` 新增 2 个组件，并把底栏的胶囊
几何与垂直偏移按参考机型实测改正。每项用法见 `docs/components/<名称小写>.md`
与 `docs/zh_CN/components/<名称小写>.md`。

| 组件 | 说明 |
| :--- | :--- |
| `AppBarBlurOverlay` | 顶栏材质叠加层：节点由调用方定尺寸为「顶栏高 + `ExtraHeight`」，剖面锚定在**栏底**而非节点底，整体 alpha 由滚动驱动（`scrollAlpha(scrollPx, density)` = `min(1, scrollPx / 30dp)`，为 0 时不绘制）。配 `AppBarBlurOverlayDefaults`（`ExtraHeight` 14dp / `ScrollAlphaThresholdDp` 30f / `scrollAlpha` / `containerColor()`：浅色 `surface` `#f0f1f2`、深色 `DarkContainerColor` `#333333`） |
| `HighlightedIconButton` | 工具栏返回键 / 三点菜单键的高亮按钮：正圆 40dp（非 squircle），径向填充（深色 `#1e1e1e`→`#242424`、浅色 249→251，alpha 0.78），按角度分布的白描边走 `Modifier.materialShadowEdge`（`GradientStrokeStyle.EDGE_STYLE_1` + `InnerShadowStyle.SHADOW_STYLE_1`），深色只在 60°–120° 下半弧有外辉光、浅色为对称暗晕。按下时整块弹到 0.89 并淡出描边与光晕。配 `HighlightedIconButtonDefaults` |
| `FloatingCapsuleRow`（`internal`） | 胶囊内容行：定宽测量——`totalItemMax = n * 70 + 2 * 12 + (n - 1) * 8`，放得下即胶囊宽 `totalItemMax`、每项 70dp，放不下则胶囊取满可用宽度、每项 `(available - 24 - (n - 1) * 8) / n` 截断到整像素，子项精确定宽并放在 `x = 12 + i * (itemWidth + 8)`。`BottomFloatingNavigationBar` 与 `BottomFloatingAccessoryNavigationBar` 共用，父级可用 `trailingReserve` 扣掉展开视图 / 配件的占位 |
| `BottomFloatingNavigationBarDefaults.MenuToScreenNaviDistance` | 胶囊与导航栏盒子下边缘之间的间距，8dp。组件内部已自带这 8dp，调用方只需再叠 `navigationBarsPadding()`；静止时胶囊底距屏底 = `navigationBars` inset + 8dp。`BottomFloatingNavigationBar` 与 `BottomFloatingAccessoryNavigationBar` 的两种形态（普通胶囊 / 熔球组）都按它抬起 |

常量来源（逐条回查设计依据，不臆造）：`AppBarBlurOverlay` 取滚动 alpha 阈值 30dp 与 `AppBarBlurParams`
默认值（`startBlurRadius = 30px`、`endBlurRadius = 0px`、`gradientBlurSpeed = 3.0f`、浅色 positions
`[0.0, 0.4, 1.0]` / alphas `[0.0, 0.7, 1.0]`，深色两档 `[0.0, 1.0]` / `[0.0, 0.4]`）与顶栏材质额外高度
14dp；模糊半径与混合曲线复用 `TopAppBar.kt` 的 `AppBarGlassDefaults`（`GradientBlurRadiusPx` /
`GradientBlurSpeed`）。深色基色 `DarkContainerColor = #333333` 取顶边压暗后的中性面板色；深色两档 ramp
由主题判定（`background.luminance() < 0.5f`），不由基色亮度决定。`HighlightedIconButton` 的数值来自
**参考机型实测**与 `ShadowEdge` AGSL 着色器，全部标注为实测所得。底栏取 12 / 8 / 70 / 56dp
（item padding / item space / item max width / height）。

未移植项：`AppBarBlurOverlay` 只实现渐变路径——均匀路径（`normalBlurRadiusPx = 50px`、
`normalBackgroundAlphaFraction = 0.8`、深色 alpha 0.6）在已研究的 ColorOS 17 形态中无调用点，属死代码；
`scrollDrivenAlphaEnabled` 属性、基色解析链与宿主侧的 `setAppBarLayoutTransparent()` 改由调用方承担。
`HighlightedIconButton` 未移植工具栏自身的布局（40dp 圆距工具栏起始边的距离与垂直居中属工具栏职责），
且 `ShadowEdge` 的 halo 只覆盖节点自身边界，填充与光晕由本组件自行绘制。底栏侧未移植第二套居中排列
（属性默认关闭，已研究形态中无设置点）。

演示落位：`example/shared/src/commonMain/kotlin/component/BackNavigationIcon.kt` 改用
`HighlightedIconButton` 绘制工具栏返回键（原先为 `IconButton`）。

## 17. `example/shared` 展廊条目拆分约定（决策 2，2026-09-30）

`example/shared/src/commonMain/kotlin/component/*.kt` 共 48 个 `*Section.kt`，每个
`fun LazyListScope.xxxSection()` 已拆成「包装」+「与布局无关的内容 composable」，
内容一份真源、包装两份（coui16 用旧包装，coui17 自己布局）：

- **切分边界是每个 `SmallTitle(...)`**。包装保留 `SmallTitle` 与紧随其后的内容调用；顺序、
  `item(key = ...)`、section 函数名一律不变，`MainPage.kt` 不感知这次拆分。
- 内容 composable 是 **public 顶层 `@Composable fun`**，命名 = `SmallTitle` 文本去标点
  PascalCase + `Demo`（`"TopAppBar (ColorOS 17 forms)"` → `TopAppBarColorOS17FormsDemo`）。
  与文件内既有 `private` 助手撞名时加 `Content` 后缀（如 `CalendarViewDemoContent`）。
- 前置守卫（如 `if (!isRuntimeShaderSupported()) return`）留在包装里；跨组共享的状态提升到
  包装并以参数传入（典型是 `InputViewSection` 的 `focusManager`）。控制行自己的
  `padding(horizontal = 16.dp, vertical = 8.dp)` 属于内容，不留在包装。
- 内容函数常在同一层级连续发射多个节点——这是它的语义（片段，由调用方放进容器），所以这些
  文件带 `@file:Suppress("ktlint:compose:multiple-emitters-check")`。该规则由
  `io.nlopez.compose.rules` 提供且按**文件**上报，函数级 `@Suppress` 无效。
- 拆分是逐字搬运：标识符多重集除 `private` → public、新增函数名与注解外必须不变；回归靠
  coui16 demo 装机截图比对（可滚动页用锚点相对位移做与滚动无关的判等，页首图要求逐字节相同）。
- 骨架层 `Home()` 增加 `layout: @Composable (HomeLayoutScope) -> Unit` 槽，默认值等于原行为，
  coui17 可据此替换整页外壳。

## 18. `example/coui17` 改为 ColorOS 17 设置式展廊（2026-09-30）

coui17 不再复用 `MainPage`，内容真源仍是 `example/shared` 的 82 个 `component.*` 内容 composable（§17）。

**结构**（`example/coui17/src/main/kotlin/io/github/suqi8/coui17/`）：

| 文件 | 职责 |
| :--- | :--- |
| `Coui17App.kt` | 根组件。`Scaffold` 内容盒里 `layerBackdrop` 只包 pager 内容，顶栏 / 配件栏 / 面板 / 搜索浮层一律是它的**兄弟节点**——材质叠加层若被录进它自己要采样的层，会在平台渲染树的准备阶段自采样递归爆栈。5 页由 `enum Coui17Page` 定义，前 3 页走胶囊、后 2 页走面板（`isPanelPage` 决定顶栏是否带返回键） |
| `Coui17Bars.kt` | `Coui17HomeTopBar` / `Coui17CompactToolbar` / `Coui17AccessoryBar`（`BottomFloatingAccessoryNavigationBar`：左球搜索、右球更多、胶囊三项）/ `Coui17Panel`（`OverlayCascadingListPopup`） |
| `Coui17HomePage.kt` | 按 coui16 原序调用 82 个内容 composable，每条一个 `coui17Entry` |
| `Coui17Dimens.kt` | 尺寸表的唯一落点 |

删除：`SettingsScreen.kt`、`GalleryScreen.kt`、`GalleryActivity.kt`，及 `AndroidManifest.xml` 的该
activity、`strings.xml` 的 `gallery_app_name`。`example/shared` 侧把页面层移进 `pages` 包
（`IconsPage` / `ColorPage` / `TextStylePage` / `SettingsPage`）、应用状态移进 `appstate` 包
（`AppState` / `LocalAppState` / `LocalUpdateAppState` / `LocalNavigator` / `LocalIsWideScreen`），
因为 Kotlin 的默认包无法被 `import`。

**尺寸（ColorOS 17 设置页设计值，dp）**：

| 常量 | 值 | 说明 |
| :--- | :--- | :--- |
| `HomeAppBarBottom` | 88.52dp | 设置主页顶栏底边 |
| `AppBarBlurExtra` | 14dp | 顶栏材质叠加层的额外高度 |
| `HomeSearchBandHeight` | 52.13dp | 首页搜索带高 |
| `SearchPillHeight` / `BarHorizontalPadding` | 40dp / 16dp | 搜索胶囊高 / 水平内边距 |
| `CompactToolbarHeight` | 52.13dp | 二级页工具栏高 |
| `BackButtonSize` | 40dp | 返回键尺寸 |
| `ToolbarTitleStart` | 65.8dp | 二级页标题起始边距 |
| `ListRowHeight` | 52.13dp | 列表行高 |
| `ListTextIndent` | 72dp | 行文字缩进 |
| `ListCardInset` / `ListGroupGap` | 16dp | 卡片水平内边距 / 组间空留白 |

标题带高度 = 状态栏高，不另立常量。首页列表不加分割线——设计上只有两处 16dp 空留白带。

**搜索浮层语义树修复**：`example/shared` 的 `SearchPager` 根 `Column` 是 `fillMaxSize()`，原先无条件挂
`semantics { onClick { false } }`。折叠态下这个全屏可点击节点会让整页自身的语义节点全部消失。现在
`semantics` 与 `pointerInput` 一起，只在该浮层真的铺满屏幕时才挂。两者都不参与绘制，故**渲染不变**，
但因为动的是 `example/shared`，同轮单独构建 `:example:android:assembleDebug` 通过。

**已知缺口**（范围外，仅记录）：

- `OtherDemo` 的三条导航箭头在 coui17 里是惰性的（只给了空 `Navigator` 栈，没有 `NavDisplay`）；
  删除复刻页后 `SideNavigationBar` / `SidePaneLayout` / `CardEntrancePreference` / `CardInstructionPreference` /
  `ListDetailView` / `LunarDatePicker` 不再有任何展廊条目。
- 深色展开态搜索浮层整屏渲染为黑（既有缺陷，根因未证）；浅色下不可见。属范围外。
