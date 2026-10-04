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
示例落位：原复刻页 `SettingsScreen.kt` 已随 COS17 改版删除（`docs/coui17-gallery-plan.md` 决策 7），
本批组件当前没有展廊条目，只有 `docs/components/` 的文档。

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
示例落位：原复刻页 `GalleryScreen.kt` 与宿主 `GalleryActivity` 已随 COS17 改版删除（决策 7），
本批组件当前没有展廊条目，只有 `docs/components/` 的文档。

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
示例落位：原复刻页 `GalleryScreen.kt` 已随 COS17 改版删除（决策 7），本批组件当前没有展廊条目。

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

常量来源（逐条回查设备资源，不臆造）：`ListDetailView` 取 `COUIListDetailView`（`DEFAULT_MAIN_WIDTH_PERCENT=0.4f`）与 `coui_main_fragment_min_width` / `coui_main_fragment_max_width` / `coui_fragment_gap_width`，分栏阈值取 `WindowTotalSizeClass.Compact` 的边界 `Breakpoints.BP_MEDIUM_WINDOW_BASE_WIDTH`（600dp）；`LunarDatePicker` 取 `COUILunarDatePicker`（`DEFAULT_START_YEAR=1910` / `DEFAULT_END_YEAR=2099`）与 `layout/coui_lunar_date_picker.xml`、`coui_lunar_month`、`coui_lunar_leap_string`。演示落位：原 `GalleryScreen.kt`（相册列表 → 照片详情）与 `SettingsScreen.kt`（系统与更新 → 日期和时间，Dialog 宿主）已随 COS17 改版删除（决策 7），这两个组件当前没有展廊条目。

未移植项：`grid/` 的 `COUIPercentWidth*` / `COUIGridLayout` / `COUIPercentUtils` 属 Android View 基础设施（`COUIResponsiveUtils` 库内已有）；`COUILunarDatePicker` 的 `updateSpinners()` 与 `IncompleteDate.change(int,int,int)` jadx 无法反编译（产出桩），滚动时滚轮重排未逐行照抄，且 `getLunarDateString(Calendar)` / `@Deprecated changeALunarYearByOne` 因 `java.util.Calendar` 入参在 commonMain 不适用。

## 16. COUI 顶栏材质叠加层、工具栏高亮按钮与底栏胶囊几何（ColorOS 17 移植，2026-09-30）

本轮在 `coui-ui/src/commonMain/kotlin/io/github/suqi8/coui/kmp/basic/` 新增 2 个组件，并把底栏的胶囊
几何与垂直偏移按设备反编译改正。逐类裁决、设备侧证据与有意省略项见
`docs/coui-coloros17-component-coverage.md` §3 的 G-12，每项用法见 `docs/components/<名称小写>.md`
与 `docs/zh_CN/components/<名称小写>.md`。

| 组件 | 说明 |
| :--- | :--- |
| `AppBarBlurOverlay` | 顶栏材质叠加层（`AppBarBlurOverlayView` + `AppBarBlurOverlayBehavior` + `AppBarBlurHelper`）：节点由调用方定尺寸为「顶栏高 + `ExtraHeight`」，剖面锚定在**栏底**而非节点底，整体 alpha 由滚动驱动（`scrollAlpha(scrollPx, density)` = `min(1, scrollPx / 30dp)`，为 0 时不绘制）。配 `AppBarBlurOverlayDefaults`（`ExtraHeight` 14dp / `ScrollAlphaThresholdDp` 30f / `scrollAlpha` / `containerColor()`：浅色 `surface` `#f0f1f2`、深色 `DarkContainerColor` `#333333`） |
| `HighlightedIconButton` | 工具栏返回键 / 三点菜单键的高亮按钮：正圆 40dp（非 squircle），径向填充（深色 `#1e1e1e`→`#242424`、浅色 249→251，alpha 0.78），按角度分布的白描边走 `Modifier.materialShadowEdge`（`GradientStrokeStyle.EDGE_STYLE_1` + `InnerShadowStyle.SHADOW_STYLE_1`），深色只在 60°–120° 下半弧有外辉光、浅色为对称暗晕。按下时整块弹到 0.89 并淡出描边与光晕。配 `HighlightedIconButtonDefaults` |
| `FloatingCapsuleRow`（`internal`） | 胶囊内容行：照 `BaseBottomFloatingMenuView.onMeasure` 定宽测量——`totalItemMax = n * 70 + 2 * 12 + (n - 1) * 8`，放得下即胶囊宽 `totalItemMax`、每项 70dp，放不下则胶囊取满可用宽度、每项 `(available - 24 - (n - 1) * 8) / n` 截断到整像素，子项精确定宽并放在 `x = 12 + i * (itemWidth + 8)`。`BottomFloatingNavigationBar` 与 `BottomFloatingAccessoryNavigationBar` 共用，父级可用 `trailingReserve` 扣掉展开视图 / 配件的占位 |
| `BottomFloatingNavigationBarDefaults.MenuToScreenNaviDistance` | 胶囊与导航栏盒子下边缘之间的间距，8dp。取值有据——`coui_bottom_floating_navigation_bar_menu_to_screen_navi_distance` 在设备资源里确为 8dp——但**语义是本仓解释**：Settings 的底栏类从不读该 token（真正读它的是 Clock），8dp 是本仓按实测反推并沿用的命名与含义。本机 `navigationBars` inset 实测 16dp（`InsetsSource navigationBars frame=[0,2710][1272,2772]`，62px），故静止时胶囊底距屏底 16 + 8 = 24dp；同一设备实测 Clock 底栏胶囊底距屏底 93px = 24.0dp，两者一致。注意组件内部已自带这 8dp，调用方只需再叠 `navigationBarsPadding()`。`BottomFloatingNavigationBar` 与 `BottomFloatingAccessoryNavigationBar` 的两种形态（普通胶囊 / 熔球组）都按它抬起 |

常量来源（逐条回查设备资源与反编译，不臆造）：`AppBarBlurOverlay` 取
`AppBarBlurOverlayBehavior.ALPHA_SCROLL_THRESHOLD_DP = 30f` 与 `AppBarBlurHelper.GRADIENT_DARK_ALPHA = 0.4f`
（两者分属不同类，勿混记）、`AppBarBlurParams` 默认值
（`startBlurRadius = 30px`、`endBlurRadius = 0px`、`gradientBlurSpeed = 3.0f`、浅色 positions
`[0.0, 0.4, 1.0]` / alphas `[0.0, 0.7, 1.0]`，深色两档 `[0.0, 1.0]` / `[0.0, 0.4]`）与
`homepage_appbar_blur_extra_height`（14dp，= `AppBarBlurOverlayDefaults.ExtraHeight`）；模糊半径与
混合曲线复用 `TopAppBar.kt` 的 `AppBarGlassDefaults`（`GradientBlurRadiusPx` / `GradientBlurSpeed`）。
深色基色 `DarkContainerColor = #333333` 取最近的夜间 token `coui_color_background_top` / `coui_color_surface_top`
（`?couiColorBackgroundWithCard` 在 night 下是 `#ffffff`，与真机实测的顶边压暗不可调和）；
`AppBarBlurHelper.setGradientBlurBgColor` 在 Settings APK 十四个 dex 里无调用点，故静态 override 已排除，
深色缺口归为框架 `com.oplus.view.*` 的压暗层。深色两档 ramp 由主题判定（`background.luminance() < 0.5f`），
不由基色亮度决定。
`HighlightedIconButton` 的数值来自**真机实测**（UI dump + 像素采样，1dp = 3.875px）与 `ShadowEdge`
AGSL 着色器，工具栏自身的 `coui_toolbar_back_view` 属性表尚未定位，故全部标注为实测所得而非反编译
所得（详见该组件文档的「取值溯源」与 `docs/components/materialshadowedge.md` 的 `EDGE_STYLE_1` /
`SHADOW_STYLE_1` 预设）。底栏取 `coui_bottom_floating_navigation_bar_{item_padding,item_space,item_max_width,height}`
（12 / 8 / 70 / 56dp）；`coui_bottom_floating_navigation_bar_menu_to_screen_navi_distance` 同为 8dp，
但语义解释见上表。

未移植项：`AppBarBlurOverlay` 只实现渐变路径——框架的均匀路径 `setUpNormalBlurBackground`
（`normalBlurRadiusPx = 50px`、`normalBackgroundAlphaFraction = 0.8`、`NORMAL_BLUR_DARK_ALPHA = 0.6`）
在已研究的 ColorOS 17 产物中无调用点，属死代码；`scrollDrivenAlphaEnabled` 属性、基色解析链
`resolveGradientBlurBaseColorForAppBar` 与宿主侧的 `setAppBarLayoutTransparent()` 改由调用方承担。
`HighlightedIconButton` 未移植工具栏自身的布局（40dp 圆距工具栏起始边的距离与垂直居中属工具栏职责），
且 `ShadowEdge` 的 halo 只覆盖节点自身边界，填充与光晕由本组件自行绘制。底栏侧未移植
`couiMenuViewCentered` 的第二套排列（属性默认 false，已研究产物无设置点）。

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

## 18. `example/coui17` 改为 ColorOS 17 设置式展廊（决策 1–21，2026-09-30）

改写依据 `docs/coui17-gallery-plan.md`。coui17 不再复用 `MainPage`（决策 20），内容真源仍是
`example/shared` 的 82 个 `component.*` 内容 composable（§17）。

**结构**（`example/coui17/src/main/kotlin/io/github/suqi8/coui17/`）：

| 文件 | 职责 |
| :--- | :--- |
| `Coui17App.kt` | 根组件。`Scaffold` 内容盒里 `layerBackdrop` 只包 pager 内容，顶栏 / 配件栏 / 面板 / 搜索浮层一律是它的**兄弟节点**——材质叠加层若被录进它自己要采样的层，会在 `libhwui.so` 的 `RenderNode::prepareTreeImpl` 里自采样递归爆栈。5 页由 `enum Coui17Page` 定义，前 3 页走胶囊、后 2 页走面板（`isPanelPage` 决定顶栏是否带返回键） |
| `Coui17Bars.kt` | `Coui17HomeTopBar` / `Coui17CompactToolbar` / `Coui17AccessoryBar`（`BottomFloatingAccessoryNavigationBar`：左球搜索、右球更多、胶囊三项）/ `Coui17Panel`（`OverlayCascadingListPopup`） |
| `Coui17HomePage.kt` | 按 coui16 原序调用 82 个内容 composable，每条一个 `coui17Entry` |
| `Coui17Dimens.kt` | §18.2 尺寸表的唯一落点 |

删除：`SettingsScreen.kt`、`GalleryScreen.kt`、`GalleryActivity.kt`，及 `AndroidManifest.xml` 的该
activity、`strings.xml` 的 `gallery_app_name`。`example/shared` 侧把页面层移进 `pages` 包
（`IconsPage` / `ColorPage` / `TextStylePage` / `SettingsPage`）、应用状态移进 `appstate` 包
（`AppState` / `LocalAppState` / `LocalUpdateAppState` / `LocalNavigator` / `LocalIsWideScreen`），
因为 Kotlin 的默认包无法被 `import`。

**尺寸来源**（参考机 OnePlus PLK110，ColorOS V17.0.0，1272x2772px @620dpi，1dp = 3.875px）：

| 常量 | 值 | 设备实测 |
| :--- | :--- | :--- |
| `HomeAppBarBottom` | 88.52dp | 设置主页 `app_bar_layout` `[0,0][1272,343]` |
| `AppBarBlurExtra` | 14dp | `blur_overlay` `[0,0][1272,397]` = 343px + 54px，即 `homepage_appbar_blur_extra_height` |
| `HomeSearchBandHeight` | 52.13dp | `searchView` `[0,141][1272,343]` |
| `SearchPillHeight` / `BarHorizontalPadding` | 40dp / 16dp | pill `[62,164][1210,319]` |
| `CompactToolbarHeight` | 52.13dp | 二级页 `toolbar` `[0,141][1272,343]` |
| `BackButtonSize` | 40dp | `coui_toolbar_back_view` `[62,164][217,319]` |
| `ToolbarTitleStart` | 65.8dp | 二级页标题 x = 255px |
| `ListRowHeight` | 52.13dp | 列表行 202px（首行 205px、带副标题 209px），取常见档 |
| `ListTextIndent` | 72dp | 行文字 x = 279px |
| `ListCardInset` / `ListGroupGap` | 16dp | 卡片 x 62..1210px；组间空留白 62px |

标题带高度 = 状态栏高，不另立常量：本机 `WindowInsets.statusBars` 顶部 = 141px（由标题文字底边
118px + 6dp 内边距反推，且与设备 `toolbar_options` `[0,0][1272,141]` 相等）。首页列表不加分割线——
设备实测无分割线节点，只有两处 16dp 空留白带。

**搜索浮层语义树修复**：`example/shared` 的 `SearchPager` 根 `Column` 是 `fillMaxSize()`，原先无条件挂
`semantics { onClick { false } }`。折叠态下这个全屏可点击节点会让整页自身的语义节点全部消失
（`uiautomator dump` 只剩 1 个节点、2649 字节；修复后 27273 字节；设置主页与图标页各复现一次）。
现在 `semantics` 与 `pointerInput` 一起，只在该浮层真的铺满屏幕时才挂。两者都不参与绘制，故**渲染不变**，
但因为动的是 `example/shared`，同轮单独构建 `:example:android:assembleDebug` 通过。

**已知缺口**（留待 Phase 4）：

- **「搜索浮层的返回键要按两次才关闭」前提已证伪，未做代码改动**。真机与实现行为一致：键盘弹起时第一次
  返回只收键盘（`dumpsys input_method` 的 `mInputShown` true→false），第二次才关浮层；键盘收起时一次即关。
  源码复核确认搜索浮层里只有**一处** `NavigationBackHandler`（`example/shared/src/commonMain/kotlin/component/SuperSearchBar.kt`
  的 `SearchPager` 内），此处先前的「`coui-ui` 的 `SearchBar` 与 `SearchPager` 各注册一个 handler」是读码误判。
- **展开态搜索框的语义节点已对齐**（commit `fix(coui17): 展开态搜索栏的放大镜移出输入框，位置与真机对齐`）。
  真机 `search_src_text` 是 `android.widget.EditText`、`clickable/focusable`、`[216,164][763,319]`；本实现为
  `EditText`、`clickable/focusable`、`[217,149][937,335]`——角色、输入能力、左沿（217 vs 216）与竖向中心
  （242 vs 241.5）对齐。残余两条，均为平台/组件限制：高度 186px vs 155px（Compose 把可点击节点的触摸框
  抬到 a11y 下限之上，视觉胶囊本身实测 y≈165..315）；右沿 937 vs 763（真机右沿由「启动小布助手」入口决定，
  本实现无此入口）。文本一侧，真机 `EditText` 在空文本时把 hint 报成 `text="搜索设置项"`，compose-ui 1.11.1
  的 `SemanticsProperties` 没有 Hint，不能伪造 Text 节点。
- 首页搜索 pill 的 leading icon 已由 44dp 改为 40dp 槽位（commit `fix(coui17): 首页搜索胶囊的放大镜与文字起点对齐真机`）。
- `OtherDemo` 的三条导航箭头在 coui17 里是惰性的（只给了空 `Navigator` 栈，没有 `NavDisplay`）；
  删除复刻页后 `SideNavigationBar` / `SidePaneLayout` / `CardEntrancePreference` / `CardInstructionPreference` /
  `ListDetailView` / `LunarDatePicker` 不再有任何展廊条目。以上两项**范围外，仅记录**。
- **深色展开态搜索浮层整屏渲染为黑**（新增记录，未修）：浅色展开态正常（整屏 231.9），深色展开态除搜索栏
  一行外整屏 0（mean 16.1，真机深色展开态恒 48）。该现象在基线 `4199933c` 的 APK 上同样复现（mean 16.2），
  与本次 6 个 commit 及本轮改动无关，属既有缺陷，根因未证（浅色下不可见：展开态整屏 surface 未上色，
  浅色页面底色与 surface 接近；深色下页面层被渲染成黑）。**范围外，仅记录**。

**Phase 4 复核结论（2026-09-30）**：

- 底栏胶囊底距屏底：库内已按 `MenuToScreenNaviDistance` 抬起 8dp，示例又叠了一层 `.padding(bottom = 8.dp)`，
  胶囊因此离屏底 32dp。去掉多余的一层后为 24.0dp，与同机 Clock 底栏（`[62,2462][1210,2679]`、
  底距屏底 93px = 24.0dp）逐像素吻合；本机 `navigationBars` inset 实测 16dp（62px）。
- 条目拓扑：源码声明 **82** 条（`Coui17HomePage.kt` 的 `coui17Entry`，含 1 条跨行标题），coui16 为 81 条，
  多出的 1 条是 `Snackbar`（`MainPage.kt` 从未调用 `snackbarSection()`），**该条保留**。真机滚动遍历
  （5 轮、约 250 次 `uiautomator dump`）按序观察到 81/82 条标题；第 70 条 `MarqueeText` 当时未抓到，
  后用慢速右侧滑动（从 x=1230 起、约 900ms、每步立即 `adb pull`）**已取证**：
  `<node text="MarqueeText" class="android.widget.TextView" clickable="false" bounds="[279,2637][657,2710]"/>`，
  包名 `io.github.suqi8.coui17`（证据 `/tmp/marquee_70_evidence.xml` + `/tmp/marquee_70_evidence.png`）。
  真机运行遍历 82/82 全部验到。
- **已解决：app bar 下方 14dp 带的那条材质色带与 397px 硬边**。根因是本仓 backdrop 与屏上内容不一致：
  `Coui17App.kt` 的 `drawWallpaper` 往记录给玻璃采样的那一层里垫了壁纸，而页面本身并不画壁纸，于是
  `AppBarBlurOverlay` 的 `progressiveTextureBlur` 把壁纸铺满整个节点（含 α 恒为 0 的 14dp 带），并在
  节点底边 y=397px 收成硬边：实测左留白 x=5 从 y=0 的 240.8 一路压到 y=390 的 **208.7**，y=400 起突变为
  250。修法（commit `fix(coui17): 顶栏材质不再压出色带，页面底色对齐真机`）：记录两层——顶栏只读页面
  那层（`pageBackdrop`），页内的玻璃组件仍读带壁纸的那层；页面底色改用材质基色 `containerColor()`。
  装机复测：顶栏区域 y∈[0,520] 恒为 **240.8**，y=397 处无跳变，与真机一致（真机同处 240.8）。
  真机这一带**不是材质**，是**搜索栏自己的投影**（`SHADOW_LV3_A`，收起态才出现），来源与证据见
  `docs/coui17-gallery-plan.md` §九。
- **已解决：搜索栏投影本身**（commit `feat(coui17): 顶栏搜索栏补上真机投影，随收起态淡入`）。真机的
  `OplusView.setOverrideLightSourceGeometry(-1, -200dp, 10000dp, 2000dp, 0)` 走 Skia 的
  `SkDrawShadowMetrics::GetSpotParams`，没有闭式解；`SearchBarShadow.kt` 的 `searchBarShadow` 复用
  `ListPopup.PopupShadow` 同一套推导，把它折成一个普通 `Shadow`：`zRatio = 100/(10000−100) = 0.01010`、
  `blurRadius = 2000dp × zRatio ≈ 20.20dp`、`offsetY = +2.02dp`、`offsetX = 0`，颜色 α 浅色 34/255、
  深色 68/255（token `coui_shadow_color_lv3a`），再乘 `scrollFraction`，形状绑 40dp 高胶囊。
  装机复测（浅色、收起态、滚动后）：x=50 深度 −10.0（真机 −11.9）、x=5 深度 −3.6（真机 −6.1），
  左右逐像素对称（x=50 与 x=1222 差值全为 0.0），远场残 ~2–4 luma；展开态无投影（与真机一致）。
  原本直接套用 `Modifier.shadow(elevation = 100.dp, black@34/255)` 得到的全屏均匀压暗 −2.8 形状不对，已弃用。
- **深色顶栏滚动后发白：既有缺陷，已修**（commit `fix(coui17): 顶栏材质采样不透明页面层，深色滚动不再发白`）。
  现象是深色主题滚动后顶栏 y∈[0,343] 抬到 146–195（真机恒 48）；基线 `4199933c` 构建后同样复现，不是本轮引入。
  隔离实验定位到根因：给顶栏采样的 `pageBackdrop` 是用 `rememberLayerBackdrop { drawContent() }` 录的，
  **背景透明**；`progressiveTextureBlur` 读到透明层后把顶栏洗成近白。浅色看不出来是因为浅色 ramp 顶层
  α=1（`AppBarGlassDefaults.GradientAlpha2`）把模糊层盖住，深色 ramp 顶层 α 只有 0.4（`GradientAlphaDark`）。
  修法：新增 `barBackdrop = rememberLayerBackdrop { drawRect(pageColor); drawLayer(pageBackdrop.graphicsLayer) }`
  专供顶栏/工具栏采样，修饰符链改 `.layerBackdrop(backdrop).layerBackdrop(barBackdrop).layerBackdrop(pageBackdrop)`，
  页内玻璃仍读 `backdrop`。装机复测：深色滚动 y30=50.0（修前 144.7）、y100=85.0，与真机同级；
  浅色静止点值不变；三层都只是 `drawLayer` 复用已录图层，没有多出 `drawContent()`。

