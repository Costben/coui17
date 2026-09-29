# COUI16 AGENT Guide

本文件汇总后续 AI 在本仓库里应遵守的核心规则。本仓库基于 [Miuix](https://github.com/compose-coui-ui/miuix) 改造，目标是**就地演进成一套 COUI（ColorOS 设计语言）Compose Multiplatform 组件库**。

> 工程规范（构建命令、代码风格、API 约定、性能约束）见 `CLAUDE.md`，那份仍然有效，写 COUI 组件时**必须同时遵守**。本文件只补充「COUI 改造」这条主线：项目定位、设计来源、逆向校准工作流。

## 1. 必读目标

后续 AI 在本仓库里的默认目标不是只分析，而是：

1. 把 Miuix（MIUI/HyperOS 观感）逐步换皮成 COUI（ColorOS 观感）
2. 设计 token（颜色 / 圆角 / 字号 / 控件尺寸 / 间距 / 动效）**必须来自真实 ColorOS**，不靠拍脑袋
3. 信息不足时，先逆向 ColorOS 系统应用取证，再落代码
4. 直接完成代码实现，而不是停在「研究完了」
5. 改完做最小自检（编译 + 至少一个平台跑通）

除非用户明确只要分析，否则不要停在结论阶段。

## 2. 项目定位（已确认）

- 本仓库由 `git clone` 自上游 Miuix（`origin = compose-coui-ui/miuix`），是改造基底。
- **定位：就地改造成 coui 库。** Miuix 作为基底，不再单独对齐上游；最终可改包名 / Maven 坐标为 coui。
- **改动深度：主题换肤优先（渐进）。** 先调 `theme/` 层默认值让现有组件整体呈现 COUI 观感，之后再按需逐个深挖组件。先不做大规模组件重构，除非某个组件的结构/交互和 ColorOS 差异大到换肤补不平。
- 目标设计系统：**COUI 16 = ColorOS 16 的设计语言**（仓库目录名 `COUI16` 即源于此）。

## 3. 设备与逆向工具环境（已确认）

| 项目 | 值 |
| :--- | :--- |
| 设备 | OnePlus PJE110（一加 13） |
| 系统 | ColorOS 16 / Android 16（`ro.build.version.oplusrom = V16.0.0`，SDK 36） |
| Root | 已 root（KernelSU，`u:r:ksu:s0`）→ 可提取 framework / overlay 资源 |
| ADB | 已连接（`adb devices` 可见），直接用 `adb` |
| JADX | `D:\jadx-1.5.0\bin\jadx.bat` |
| Apktool | `D:\ApkTool\apktool_3.0.1.jar` |
| 逆向工作区 | `D:\AndroidStudioProjects\jadx\work\<target_name>\` |

设备状态可能变化，每次进入逆向前先 `adb devices` 确认连接，再 `adb shell getprop ro.build.version.oplusrom` 确认版本未漂移。

### 关键逆向目标包（已确认存在于设备）

| 包名 | 用途 / 能提取什么 |
| :--- | :--- |
| `com.oplus.uxdesign` | **ColorOS 设计语言资源主库**，COUI 主色 / 强调色 / 设计规范的首选来源 |
| `com.android.settings` | 设置页：列表项、卡片、开关、分组标题、对话框的真实尺寸与配色 |
| `com.coloros.alarmclock` | 时钟：大标题字号、按钮、Tab、滑块、数字选择器的真实样式 |
| `com.android.systemui` | 状态栏 / 通知 / 控制中心：圆角、模糊、Toast/Snackbar 观感 |
| `com.android.launcher` | 桌面 / 文件夹：动效曲线、squircle 圆角参数 |
| `com.heytap.themestore` / `com.oplus.themestore` | 主题商店：强调色色板、动态取色参考 |
| `io.github.suqi8.coui.uitest` | 用户自建的 COUI 测试 app（包前缀与 OShin `io.github.suqi8.*` 同源），可作落地验证场 |

COUI 控件资源历史上常以 `couiXxx` / `coloros_xxx` / `oplus_xxx` 命名（颜色、dimen、style）。framework 级 COUI 资源可能在 RRO overlay 里：`com.oplus.framework.res.overlay`、`com.oplus.framework.rro.oneplus`、`com.android.systemui.oplus.res.overlay`（`adb shell cmd overlay list | grep oplus` 查看）。

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

## 5. 设计 token 必须先逆向的判定

满足任一条件时，**不要直接硬写色值 / 尺寸**，先逆向取证：

- 需要 COUI 主色 / 强调色 / 语义色（错误红、链接蓝等）的精确值
- 需要某控件（开关、滑块、按钮、卡片、列表项、对话框）的真实圆角 / 尺寸 / 内边距
- 需要 ColorOS 字体梯度（标题 / 正文 / 脚注的 sp 与字重）
- 需要 squircle 平滑度 / 动效曲线参数
- 用户对某个组件「不像 ColorOS」提出具体异议

够明确的微调（如已确认主色后改一处引用）可直接改，不必每次都逆向。

## 6. 设计 token 逆向工作流

### 6.0 资源类逆向优先 Apktool

COUI 设计 token 主要是**资源**（`res/values/colors.xml`、`dimens.xml`、`styles.xml`、`color/*.xml`），不是 dex 逻辑。所以与 OShin（hook 逻辑、优先 JADX/DexKit）不同，**本仓库逆向优先 Apktool 解出 `res/`**，JADX 仅在需要看「某 token 在代码里怎么被消费 / 计算」时辅助。

### 6.1 目录规则

逆向工作区统一放到：

```text
D:\AndroidStudioProjects\jadx\work\<package_name>\
  input\      原始 APK
  jadx\       JADX 输出（看消费逻辑时用）
  apktool\    Apktool 输出（看 res/ token，主战场）
  notes.md    token 提取结论
```

### 6.2 提取已安装系统应用 APK

```powershell
$Package = "com.oplus.uxdesign"
$Target  = "D:\AndroidStudioProjects\jadx\work\$Package"

New-Item -ItemType Directory -Force -Path "$Target\input", "$Target\jadx", "$Target\apktool" | Out-Null

$RemotePaths = adb shell pm path $Package |
  ForEach-Object { $_.Trim() } |
  Where-Object { $_ -like "package:*" } |
  ForEach-Object { $_.Substring(8) }

$Index = 0
foreach ($Remote in $RemotePaths) {
  $LocalName = if ($Index -eq 0) { "$Package.apk" } else { "$Package-$Index.apk" }
  adb pull $Remote "$Target\input\$LocalName"
  $Index++
}
```

### 6.3 Apktool 解资源（主战场）

```powershell
$Package = "com.oplus.uxdesign"
$Target  = "D:\AndroidStudioProjects\jadx\work\$Package"
$BaseApk = "$Target\input\$Package.apk"

if (Test-Path "$Target\apktool") { Remove-Item "$Target\apktool" -Recurse -Force }
java -jar "D:\ApkTool\apktool_3.0.1.jar" d -f "$BaseApk" -o "$Target\apktool"
```

解出后，token 主要看：

- `apktool\res\values\colors.xml` / `values-night\colors.xml` —— 亮/暗色值
- `apktool\res\values\dimens.xml` —— 圆角、控件尺寸、间距
- `apktool\res\values\styles.xml` / `themes.xml` —— 控件样式聚合
- `apktool\res\color\*.xml` —— 状态色（selector）

用 Grep 在 `apktool\res\values\` 里搜 `coui` / `color_primary` / `oplus` / `corner` / `radius` 定位候选。

### 6.4 提取 framework / overlay 资源（需 root）

framework 级 COUI token 在 RRO overlay 或 framework-res：

```powershell
# 列出 oplus overlay，挑目标
adb shell cmd overlay list | Select-String -Pattern "oplus|coui|color"

# 例：拉 framework overlay apk（路径用 pm path 或 dumpsys 查）
$Remote = "/system/.../com.oplus.framework.res.overlay.apk"   # 用 adb shell pm path <overlay包名> 确认真实路径
adb shell su -c "cp $Remote /data/local/tmp/o.apk"
adb pull /data/local/tmp/o.apk "D:\AndroidStudioProjects\jadx\work\oplus_framework_overlay\input\o.apk"
adb shell su -c "rm /data/local/tmp/o.apk"
```

### 6.5 运行时实测（交叉验证，最可信）

静态资源可能被主题 / 动态取色覆盖。拿不准时直接读运行时值：

```powershell
# 当前强调色 / 主题相关 settings（key 名可能随版本变，先 grep 再读）
adb shell settings list system   | Select-String -Pattern "theme|color|accent"
adb shell settings list secure   | Select-String -Pattern "theme|color|monet|accent"

# 截图肉眼/取色校准（截当前 ColorOS 界面）
adb exec-out screencap -p > D:\AndroidStudioProjects\jadx\work\_shots\settings.png
```

截图配合放大取色，是校准「这个蓝到底是哪个蓝」最直接的手段。

### 6.6 逆向时至少记录（写进 notes.md）

- 目标包名 + 版本（`adb shell dumpsys package <pkg> | grep versionName`）
- 取到的 token：资源名 → 值（亮 / 暗）
- 来源文件路径（`apktool\res\values\colors.xml` 等）
- 该 token 在 Miuix 里对应哪个色槽 / 哪个 `Defaults`（落点见 §7）
- 是静态资源值，还是运行时实测值（后者更可信）
- 未确认 / 待校准项

## 7. token → 代码落点映射

提取到 token 后，按下表落到代码（**只改值，不重命名**）：

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
2. 看 Miuix 现状：定位 §4 的抓手文件 / 对应 `Defaults`，确认当前值与取色路径
3. 判定是否需要逆向（§5）：需要就进 §6 取证（优先 Apktool 解 `res/`，必要时运行时实测）
4. 在 `notes.md` 记录 token → 落点结论（§6.6）
5. 改代码（§7，只改值不重命名）
6. 同步文档 / demo / 示例 app（§7 末）
7. 最小自检（§9）

## 9. 最小自检清单

- 改的是 `theme/` 默认值或 `Defaults`，没有在组件体里硬编码 `Color(0x...)`
- light 和 dark **两套**色值都改了（只改一套是最常见遗漏）
- token 有真实来源（资源路径或运行时实测），不是凭空写的
- 命名未被擅自改动（换肤阶段不重命名主题类 / 色槽）
- 关联文档 / demo / 示例 app 已同步（若改了组件 API 或默认值）
- 构建自检默认直接执行：
  - 快速：`./gradlew compileKotlinDesktop`
  - 完整：`./gradlew assemble`
  - 格式：先 `./gradlew spotlessCheck`，有违规才 `./gradlew spotlessApply`
- 至少在一个平台（Android 或 Desktop）肉眼确认观感更接近 ColorOS

## 10. 默认完成标准

当用户说「做 COUI 换肤 / 改成 ColorOS 风格」时，默认完成标准不是「分析完」，而是：

1. token 已有真实 ColorOS 来源（逆向资源或运行时实测）
2. 已落到 `theme/` 默认值或对应 `Defaults`，light/dark 双套
3. 编译通过，至少一个平台跑通并肉眼校准
4. 关联文档 / demo / 示例已同步（若涉及）
5. 关键来源与未校准项已在回复里说明

不要用泛泛「已完成」收尾；说明取到的 token 来源、改了哪些落点、还有哪些待校准。

## 11. 共享约束

- 沿用 Miuix 现有架构与命名，不发明新的模块结构 / 主题机制（换肤阶段尤其如此）。
- 引用真实路径与真实包名，不给泛泛 Android / Compose 建议。
- 设计 token 不臆造，必须可溯源到 ColorOS（资源文件路径或运行时实测）。
- 静态资源与运行时值冲突时，以运行时实测为准（动态取色 / 主题会覆盖静态默认）。
- 逆向工作区统一放 `D:\AndroidStudioProjects\jadx\work\<package_name>\`，结论写进该目录 `notes.md`。
- 写 / 改组件仍遵守 `CLAUDE.md` 的 API 约定、性能约束、`@Immutable`/`@Stable` 规则。
- 与用户用中文交流、用中文输出 Plan；生成的代码（含注释 / KDoc）保持英文。

## 12. COUI preference 组件（ColorOS 17 移植，2026-09-30）

`coui-preference/src/commonMain/kotlin/io/github/suqi8/coui/kmp/preference/` 本轮新增 15 个组件。
逐类裁决、设备侧证据与有意省略项见 `docs/coui-coloros17-component-coverage.md` §3 的 G-9 / G-10，
每项用法见 `docs/components/<名称小写>.md` 与 `docs/zh_CN/components/<名称小写>.md`，
示例落位见 `example/coui17/src/main/kotlin/io/github/suqi8/coui17/SettingsScreen.kt`。

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
逐类裁决、设备侧证据与有意省略项见 `docs/coui-coloros17-component-coverage.md` §3 的 G-3 / G-4，
每项用法见 `docs/components/<名称小写>.md` 与 `docs/zh_CN/components/<名称小写>.md`，
示例落位见 `example/coui17/src/main/kotlin/io/github/suqi8/coui17/GalleryScreen.kt`（宿主 `GalleryActivity`）。

| 组件 | 说明 |
| :--- | :--- |
| `SideNavigationBar` | 响应式侧边抽屉：紧凑窗口为临时抽屉（`coui_color_mask` 遮罩、可点遮罩关闭），宽窗口转为固定栏（无遮罩、内容仍可交互）。配 `SideNavigationBarState` / `SideNavigationBarMode` / `SideNavigationBarListener` / `SideNavigationBarDefaults` / `rememberSideNavigationBarState` |
| `SidePaneLayout` | 起始边可拖拽侧面板：`coverStyle` 决定覆盖还是推开内容，`alwaysShowMask` 决定遮罩是否常显，折角按钮负责开合。配 `SidePaneLayoutState` / `SidePaneState` / `SidePanePanelSlideListener` / `SidePaneMaskListener` / `SidePaneUtils` / `SidePaneDefaults` / `SidePaneFoldIconColors` / `rememberSidePaneLayoutState` |
| `COUIIcons.Basic.FoldPane` | `drawable/coui_pane_fold_normal` 的 1:1 移植（48×52 视口、1.4 圆头描边），供 `SidePaneLayout` 的折角按钮使用 |

注意：`COUISidePaneLayout` 在被研究的 Gallery / Dialer 中**没有应用侧调用点**（只在库自身源码、`R.*` 生成类与 dex 二进制命中）；Gallery 的侧栏实为 `extends COUISideNavigationBar` 的子类。因此 `SideNavigationBar` 有设备实证调用点，`SidePaneLayout` 的演示为按设备类重建，非实测对齐。

## 14. COUI 卡片 / 网格弹簧链组件（ColorOS 17 移植，2026-09-30）

本轮在 `coui-ui/.../basic/` 新增 1 个网格越界组件，在 `coui-preference/.../preference/` 新增 2 个卡片组件。
逐类裁决、设备侧证据与有意省略项见 `docs/coui-coloros17-component-coverage.md` §3 的 G-7 / G-8，
每项用法见 `docs/components/<名称小写>.md` 与 `docs/zh_CN/components/<名称小写>.md`，
示例落位见 `example/coui17/src/main/kotlin/io/github/suqi8/coui17/GalleryScreen.kt`。

| 组件 | 说明 |
| :--- | :--- |
| `GridSpringChain` | 网格越界弹簧链：`Modifier.gridSpringChainOverscroll(state)` 加在网格上度量并阻尼越界量，`Modifier.gridSpringChainItem(state, row)` 把控制距离换算为按行位移。配 `GridSpringChainState` / `GridSpringChainTranslation` / `GridSpringChainDefaults` / `rememberGridSpringChainState`。复用既有 `utils/Overscroll.kt`（`SpringMath.obtainDampingDistance`）与 `utils/SpringUtils.kt`（`SpringEngine.runSettleAnimation`），未新写弹簧积分器 |
| `CardEntrancePreference` | 可点击跳转的入口卡片，小卡（图标在上、12dp 圆角）/ 大卡（图标在左、16dp 圆角）两套布局。配 `CardEntranceType` / `CardEntranceTintIcon` / `CardEntrancePreferenceDefaults` / `CardEntranceColors` |
| `CardInstructionPreference` | 功能引导卡片，Description（`HorizontalPager` + `PageIndicator` 轮播）/ Selector（一行单选）两态。配 `CardInstructionItem` / `CardInstructionType` / `CardInstructionPreferenceDefaults` / `CardInstructionColors` |

常量来源（逐条回查设备资源，不臆造）：`GridSpringChain` 取 `COUIGridSpringChainScrollView`（`MAX_DISTANCE_FACTOR=0.25f` / `MAX_DISTANCE_RATIO=0.5f` / `OVER_SCROLL_FACTOR=0.3f`）、`COUIGridSpringChain`（默认位移 `(row + 1) * 0.1f * distance`、`MAIN_TENSION/MAIN_FRICTION=150f/10f`）、`COUISpringChain`（40/6、70/10）、`COUIChainDecorator`（`FACTOR=0.3f`、`DEFAULT_MARGIN_VERTICAL=24f`）；两张卡片取 `coui_component_card_entrance_{small,large}_bg.xml`（圆角 12dp / 16dp）、`coui_component_card_entrance_preference_type_{small,large}.xml`、`coui_component_card_instruction_bg.xml`（16dp）、`coui_component_card_instruction_preference.xml` 与 `dimens.xml` 的 `coui_component_card_instruction_*` 全组。

未移植项：`springchain/` 的 7 个顶层类与 `api/` 3 个接口（`COUIGridSpringChainScrollView` / `COUIChainDecorator` / `COUIGridSpringChainItem` / `ICOUIGridSpringChainViewGroup` / `IChainItem` / `ICheckViewEdge` / `ISpringUpdateListener` 等）属 Android View 基础设施；`COUISpringChain` 的 tension/friction 无法一一映射到临界阻尼的 `SpringEngine`；`card/` 的 17 个类（`RecyclerView.ItemDecoration` / `SpanSizeLookup` / `Adapter` 体系、`COUIMutableSizeScrollView`、`BaseDisplayInfo` 等）属 RecyclerView 与 View 层基础设施，或库内已有等价覆盖。`CardEntrancePreferenceDefaults.IconSize = 32.dp` 是移植自选值（设备为 `wrap_content`，无 `coui_*` dimen），已在 KDoc 与文档注明；`GridSpringChainDefaults.ChainMarginVertical = 24f` 按设备原样保留为 px 值。

## 15. COUI 列表-详情容器与农历日期选择器（ColorOS 17 移植，2026-09-30）

本轮在 `coui-ui/.../basic/` 新增 2 个组件，在 `coui-ui/.../utils/` 新增 1 个换算工具。
逐类裁决、设备侧证据与有意省略项见 `docs/coui-coloros17-component-coverage.md` §3 的 G-6 / G-11，
每项用法见 `docs/components/<名称小写>.md` 与 `docs/zh_CN/components/<名称小写>.md`。

| 组件 | 说明 |
| :--- | :--- |
| `ListDetailView` | 响应式列表-详情容器：窄于 600dp 时只组合 `mainContent` / `detailContent` 之一（由 `detailVisible` 决定），达到 600dp 后主面板占 40%、后接 `coui_fragment_gap_width` 细线、详情（未选中时为 `emptyPageContent`）占满剩余。配 `ListDetailViewDefaults`（`SplitModeMinWidth` 600dp / `MinMainWidth` 280dp / `MaxMainWidth` 412dp / `DividerWidth` 0.33000004dp），`isSplitMode(width)` 判定分栏 |
| `LunarDatePicker` | 农历日期滚轮：三列均宽 76dp（`coui_number_picker_width_middle`），月列显示 `coui_lunar_month` 月名并带 `coui_lunar_leap_string`（闰）前缀，年 / 日列为纯数字。配 `LunarDatePickerDefaults`（`YearRange` 1910..2099 / `ColumnWidth` / `MonthNames` / `LeapPrefix` / `MonthLabel`） |
| `LunarUtil` + `LunarDateValue` | `LunarUtil` 的直接移植：`LUNAR_INFO`（1900..2100，201 项）与 `SOLAR_TERM_DAYS`（1970..2036，67×24）逐项转写，`calculateLunarByGregorian` / `lunarToSolar` / `toLunarDate` / `leapMonth` / `daysOfALunarMonth` / `chineseStringOfALunarDay` 等纯整数 API 独立可用 |

常量来源（逐条回查设备资源，不臆造）：`ListDetailView` 取 `COUIListDetailView`（`DEFAULT_MAIN_WIDTH_PERCENT=0.4f`）与 `coui_main_fragment_min_width` / `coui_main_fragment_max_width` / `coui_fragment_gap_width`，分栏阈值取 `WindowTotalSizeClass.Compact` 的边界 `Breakpoints.BP_MEDIUM_WINDOW_BASE_WIDTH`（600dp）；`LunarDatePicker` 取 `COUILunarDatePicker`（`DEFAULT_START_YEAR=1910` / `DEFAULT_END_YEAR=2099`）与 `layout/coui_lunar_date_picker.xml`、`coui_lunar_month`、`coui_lunar_leap_string`。演示落位见 `example/coui17/.../GalleryScreen.kt`（相册列表 → 照片详情）与 `example/coui17/.../SettingsScreen.kt`（系统与更新 → 日期和时间，Dialog 宿主）。

未移植项：`grid/` 的 `COUIPercentWidth*` / `COUIGridLayout` / `COUIPercentUtils` 属 Android View 基础设施（`COUIResponsiveUtils` 库内已有）；`COUILunarDatePicker` 的 `updateSpinners()` 与 `IncompleteDate.change(int,int,int)` jadx 无法反编译（产出桩），滚动时滚轮重排未逐行照抄，且 `getLunarDateString(Calendar)` / `@Deprecated changeALunarYearByOne` 因 `java.util.Calendar` 入参在 commonMain 不适用。
