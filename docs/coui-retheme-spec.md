<!-- Copyright 2025, compose-coui-ui contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# ColorOS 16 / 17 (COUI) Retheme Spec

> miuix → COUI 换肤的权威规范。全部色值、圆角、尺寸与字体梯度都取自 ColorOS 16 / ColorOS 17 的设计语言，
> 并与参考机型上的实际观感逐项核对——每一条都有可复核的设计依据，不是拍脑袋定的。
>
> **策略：只改值，不重命名主题类 / 色槽。** 组件读 `COUITheme.colorScheme.*` / `textStyles.*`，绝不硬编码 `Color(0x...)`。
> 约定：`#RRGGBB` / `#AARRGGBB`；L=浅色 D=深色；透明度前缀 12%=`1F` 15%=`26` 30%=`4D` 40%=`66`。

## 0. 主色（设计语言基准）

| | L | D | 设计依据 |
| :-- | :-- | :-- | :-- |
| **primary** | `#0066FF` | `#247CFF` | ColorOS 蓝：浅色为标准品牌蓝，深色按暗底可读性提亮 |

miuix 原值：L `#3482FF` / D `#277AF7`（HyperOS 蓝）。

## 1. ⚠ 关键：背景层级是「反相」的（已校验消费点）

ColorOS = **灰底 + 白卡**；miuix 现状 = 白底 + 浅灰面。已确认 miuix 实际消费点：

| 色槽 | 消费者 | COUI 目标 L | COUI 目标 D |
| :-- | :-- | :-- | :-- |
| `surface` | **Scaffold 页面底**、Surface、TopAppBar、NavigationRail、TabRow 底 | `#F0F1F2`（灰底） | `#000000`（纯黑 AMOLED） |
| `surfaceContainer` | **Card**、FloatingToolbar | `#FFFFFF`（白卡） | `#1AFFFFFF`（10% 白） |
| （popup 专用，非色槽） | ListPopup / Dropdown / CascadingPopup 容器走 `popupSurfaceColor()` | `#FFFFFF` | `#333333`（不透明面板底） |
| `surfaceVariant` | （卡片同义） | `#FFFFFF` | `#1AFFFFFF` |
| `background` | **Dialog / BottomSheet 面板背景**、NavigationBar 底（tab 栏比页面底更亮） | `#FFFFFF`（白面板，**勿刷灰**） | `#1E1E1E` |

> **校正**：把 `background` 也设成灰 `#F0F1F2` 是错的——`background` 只被对话框 / 底部表单当面板背景用，刷灰会让弹窗发灰。`background` 应保持白 / elevated；页面「灰底」由 `surface` 承担。

## 2. Colors.kt 全色槽落点表

`lightColorScheme()` @341 / `darkColorScheme()` @451。置信度：高/中/低。标 *保留* 的无良好 ColorOS 对应色，维持 miuix 原值。

| slot | L | D | 设计依据 | 置信 |
| :-- | :-- | :-- | :-- | :-- |
| primary | `#0066FF` | `#247CFF` | ColorOS 主蓝 | 高 |
| onPrimary | `#FFFFFF` | `#FFFFFF` | 主色上的语义白 | 高 |
| primaryVariant | `#0066FF` | `#247CFF` | 复用主蓝 | 中 |
| onPrimaryVariant | *保留* `#AECDFF` | *保留* `#99C7F1` | 无干净对应色 | 低·保留 |
| error | `#DB382C` | `#FF6C61` | ColorOS 语义红 | 高 |
| onError | `#FFFFFF` | `#FFFFFF` | 语义白 | 高 |
| errorContainer | `#26EB3B2F` | `#40EB493D` | 红 15% / 25% 容器底 | 中 |
| onErrorContainer | `#DB382C` | `#FF6C61` | 红色文字 | 中 |
| disabledPrimary | `#4D0066FF` | `#4D247CFF` | 30% 真主色（不照抄偏色资源） | 中 |
| disabledOnPrimary | `#FFFFFF` | `#404040` | 禁用态前景 | 低 |
| disabledPrimaryButton | `#4D0066FF` | `#4D247CFF` | 主按钮禁用底 | 高 |
| disabledOnPrimaryButton | `#8AFFFFFF` | `#29FFFFFF` | 主按钮禁用文字 | 高 |
| disabledPrimarySlider | `#EBEBEB` | `#262626` | 滑块禁用轨道（去蓝调） | 中 |
| primaryContainer | `#0066FF` | `#5C9DFF` | 弹出层主蓝 | 中 |
| onPrimaryContainer | `#FFFFFF` | `#FFFFFF` | 语义白 | 高 |
| secondary | `#E5E5E5` | `#757575` | 未选中开关轨道 | 高 |
| onSecondary | `#FFFFFF` | `#FFFFFF` | 开关拇指恒白 | 高 |
| secondaryVariant | `#14000000` | `#26FFFFFF` | 次级按钮 / 按压底 | 中 |
| onSecondaryVariant | `#E6000000` | `#E6FFFFFF` | 主文字色 | 中 |
| disabledSecondary | `#F2F2F2` | `#4D757575` | 开关轨道禁用 | 中 |
| disabledOnSecondary | `#42000000` | `#4DFFFFFF` | 中性禁用色 | 低 |
| disabledSecondaryVariant | `#14000000` | `#26FFFFFF` | 次级按钮禁用 | 低 |
| disabledOnSecondaryVariant | `#42000000` | `#4DFFFFFF` | 中性禁用色 | 低 |
| secondaryContainer | `#14000000` | `#26FFFFFF` | 四级文字色 | 低 |
| onSecondaryContainer | `#66000000` | `#66FFFFFF` | 次级变体文字色 | 低 |
| secondaryContainerVariant | `#14000000` | `#26FFFFFF` | 四级文字色 | 低 |
| onSecondaryContainerVariant | `#66000000` | `#66FFFFFF` | 次级变体文字色 | 低 |
| tertiaryContainer | `#260066FF` | `#40247CFF` | 主蓝 15% / 25% 容器 | 高 |
| onTertiaryContainer | `#0066FF` | `#5C9DFF` | 主题蓝文字 | 高 |
| tertiaryContainerVariant | `#260066FF` | `#40247CFF` | 主蓝 15% / 25% 容器 | 中 |
| background | `#FFFFFF` | `#1E1E1E` | 见 §1（对话框面板，**勿刷灰**） | 高·校正 |
| onBackground | `#E6000000` | `#E6FFFFFF` | 主文字色 | 高 |
| onBackgroundVariant | `#8A000000` | `#8AFFFFFF` | 次级文字色 | 中 |
| surface | `#F0F1F2` | `#000000` | 页面灰底 | 高 |
| onSurface | `#E6000000` | `#E6FFFFFF` | 主文字色 | 高 |
| surfaceVariant | `#FFFFFF` | `#1AFFFFFF` | 卡片白 | 高 |
| onSurfaceSecondary | `#8A000000` | `#8AFFFFFF` | 次级文字色 | 高 |
| onSurfaceVariantSummary | `#66000000` | `#66FFFFFF` | 摘要文字色 | 中 |
| onSurfaceVariantActions | `#42000000` | `#4DFFFFFF` | 操作文字色 | 中 |
| disabledOnSurface | `#42000000` | `#4DFFFFFF` | 中性禁用色 | 高 |
| surfaceContainer | `#FFFFFF` | `#1AFFFFFF` | 卡片白 | 高 |
| onSurfaceContainer | `#E6000000` | `#E6FFFFFF` | 主文字色 | 高 |
| onSurfaceContainerVariant | `#66000000` | `#66FFFFFF` | 次级变体文字色 | 中 |
| surfaceContainerHigh | `#E6E6E6` | `#33FFFFFF` | 卡片按压底 | 中 |
| onSurfaceContainerHigh | `#42000000` | `#4DFFFFFF` | 操作文字色 | 低 |
| surfaceContainerHighest | `#FFFFFF` | `#1E1E1E` | 抬升背景 | 中 |
| onSurfaceContainerHighest | `#E6000000` | `#E6FFFFFF` | 主文字色 | 中 |
| outline | `#26000000` | `#26FFFFFF` | 输入框默认描边 | 中 |
| dividerLine | `#1F000000` | `#33FFFFFF` | 12% 分割线 | 高 |
| windowDimming | `#33000000` | `#99000000` | 窗口遮罩 | 高 |
| sliderKeyPoint | `#F2F2F2` | `#595959` | 滑块刻度点 | 中 |
| sliderKeyPointForeground | *保留* `#6EB5FF` | *保留* `#5DAAFF` | 无干净对应色 | 低·保留 |
| sliderBackground | `#0D000000` | `#1AFFFFFF` | 滑块轨道底 | 高 |

## 3. 形状（圆角）— 所有圆角 L==D（dimens 无夜间变体）

| 落点 | 现值 | 目标 | 设计依据 | 置信 |
| :-- | :-- | :-- | :-- | :-- |
| `CardDefaults.CornerRadius` | 16dp | **12dp** | 卡角取中档连续曲率半径（见 §9.5） | 高 |
| `ButtonDefaults.CornerRadius` | 16dp | **21.5dp**（胶囊=44/2） | 大按钮胶囊，半径 = 高 / 2 | 高 |
| `TextFieldDefaults.CornerRadius` | 16dp | **10dp** | 输入框圆角 | 中 |
| `DialogContentLayout.kt` 底角 `coerceAtLeast(32.dp)` | 32dp | **33dp** | 对话框底角 | 高 |
| `BottomSheetContentLayout.kt` 28dp | 28dp | ✓ 无需改 | 面板圆角 | 高 |
| `ListPopup` 16dp + `CascadingPopupCornerRadius` 16dp | 16dp | ✓ 无需改 | 弹出列表圆角 | 高 |
| `TabRowDefaults.TabRowWithContourCornerRadius` 8dp | 8dp | ✓ | 轮廓 tab 圆角 | 中 |
| `TabRowDefaults.TabRowCornerRadius` | 20dp | ✓ 无需改 | tab 圆角；原据的 8dp 只服务 tool 型蒙层，tab 型不消费，不可作 tab 圆角依据 | 中 |
| `FloatingToolbarDefaults.CornerRadius` | 50dp | (分歧)24dp | 浮动工具条圆角 — **改前确认** | 中·flag |

**Squircle 连续曲率**：`SquircleDefaults.Extension`（`SquirclePath.kt`，现 `1.0f`，clamp 1f–2f）已按 CO17 主路径收口——hwui G2 SDF 的角部 tile 严格等于 `cornerRadius`，故取 `1.0`；旧值 `1.2819` 来自已停用的 COS16 平滑路径（tile `128.19/100`）。CO17 的 `_weight` **不是** extent/radius 比，而是超椭圆指数 n（`n ≥ 2`，`2` 即正圆）；「M=1.33 / XL=1.25 / XXL=1.26 / L=1.23 / S·XS=1.0」那套读法是 pre-16.1 的旧平滑方案（`tile=(1+weight)·R`），不适用于 CO17 的 NEW_G2。命中 weight 2.5 / 3.0 的曲线控制点为 `0.6877` / `0.7832`。

## 4. 尺寸 — L==D

| 落点 | 现值 | 目标 | 设计依据 | 置信 |
| :-- | :-- | :-- | :-- | :-- |
| `ButtonDefaults.MinHeight` | 40dp | **44dp** | 大按钮最小高 | 高 |
| `ButtonDefaults.InsideMargin` 水平 | 16dp | **12dp** | 按钮水平内边距 | 中 |
| `SliderDefaults.MinHeight`（轨道厚） | 28dp | **20dp** | 滑块轨道厚 | 高 |
| `SliderDefaults.KeyPointRadius` | 3.855dp | **3.0dp** | 滑块刻度点半径 | 中 |
| ⚠`Component.kt` `heightIn(min=…)` | 56dp | **48dp**（修订，原定 60） | 设置页 preference 行最小高 48，行外加首尾各 2 | 高·修订 |
| `BasicComponentDefaults.InsideMargin` 水平 | 16dp | ✓ | 列表行左/右内边距 | 高 |
| `BasicComponentDefaults.InsideMargin` 垂直 | 16dp | **10dp** | 设置页行文本区上下各 10 | 高 |
| `TopAppBarDefaults.CollapsedHeight` | 52dp | ✓ | 顶栏高 | 高 |
| `TopAppBarDefaults.TitlePadding` | 26dp | **16dp**（手机 compact 档） | 折叠工具栏标题起始边距（24=medium / 40=expanded） | 高 |
| `SmallTitleDefaults.InsideMargin` | (28,8) | **(32,8)**（修订，曾误定 (32,12)） | 分组标题水平 32=16 页边+16 卡内，上下各 8 | 高·修订 |
| `IconButtonDefaults.MinWidth/Height` | 40dp | **48dp** | 工具栏图标钮触达尺寸 | 中 |
| `FloatingActionButtonDefaults.MinWidth/Height` | 60dp | **56dp** | 浮动按钮常规尺寸 | 中 |

## 5. 字体梯度（置信偏低，建议独立可选批次）

| TextStyles val | 现值 | 目标 | 设计依据 | 置信 |
| :-- | :-- | :-- | :-- | :-- |
| `Button` | 17sp | **16sp** | 按钮文字 | 中 |
| `Title1` | 32sp | ✓ | 展开态大标题 | 高 |
| `Title4` | 18sp | ✓ | 折叠态标题 | 中 |
| `Main` | 17sp | (可选)16sp | 设置页标题字号 — Main 用途广，改前评估 | 低·flag |
| `Body2`/summary | 14sp | (可选)12sp | 设置页摘要字号 | 低·flag |
| `Subtitle` | 14sp Bold | 评估 Medium(500) | ColorOS 子标题偏 medium | 低·flag |

## 6. 关键控件（改 §2 色值即自动生效，无需改组件代码）

Switch / Slider / Checkbox / RadioButton / Button 的所有 accent 态都读 `colorScheme.primary / onPrimary / secondary / disabled* / sliderBackground / sliderKeyPoint`。**纯色换肤改 Colors.kt 即传播**。

几何属可选批次（代码字面量，非 Defaults）：

- **Switch** `.size(49,28)` 拇指 `20dp` 偏移 `4..25dp` 拖拽 `coerceIn(-21f,0f)` → COUI 36×22，改尺寸须重算偏移 / 拖拽数学。纯换肤建议**保留 49×28**。
- **Slider** `MinHeight 28→20`（COUI 标志 20dp 胶囊轨）、`KeyPointRadius 3.855→3`；拇指 `barRadius*0.72f`，MinHeight=20 时得 7.2dp（COUI 6–8dp，达标）。
- **Checkbox** `.requiredSize(26.dp)`（COUI 24，保留）；COUI 关闭 / 禁用为半透明描边环，与 miuix 实心填充结构不同 → 低置信近似，需目测。

## 7. 改造批次（仅现有组件，高影响·低风险优先）

- **批次 0 — Colors.kt 纯值替换**（最高 ROI，零代码风险）：primary 家族 + surface/card 家族 + secondary + slider* + 语义色 + 文字 alpha + 分隔线 + 遮罩。一改传播到所有组件。先做、单独提交、目测。
- **批次 1 — Button + Card Defaults**：Card 16→17；Button CornerRadius 16→21.5、MinHeight 40→44、InsideMargin 水平 16→12。
- **批次 2 — Slider 几何**：MinHeight 28→20、KeyPointRadius 3.855→3。
- **批次 3 — Squircle Extension**：已结案为 `1.0f`（CO17 主路径 hwui G2 SDF 的角部 tile 严格等于 `cornerRadius`）。
- **批次 4 — 次级 Defaults**：TextField 角 16→10、SmallTitle (28,8)→(32,12)、TopAppBar TitlePadding 26→24、IconButton 40→48、FAB 60→56。
- **批次 5 — 代码字面量（中风险）**：Component heightIn 56→60、Dialog 底角 32→33。
- **批次 6 — 几何（已做 Switch + FloatingToolbar）**：Switch 49×28→36×22（拇指 18dp，行程 / 偏移 / 拖拽阈值全由 `SwitchDefaults.Travel` 推导，数学自洽）；FloatingToolbar 50→24。**未做**：TabRow 12→8（COS17 已结案：该 8dp 只服务 tool 型蒙层，不可作 tab 圆角依据）、字体梯度（用户决策「暂不动」）。

## 8. 未决项（需运行时目测）

1. 深色 surface=`#000000` 纯黑 AMOLED vs miuix 现 `#242424`，确认是否接受纯黑底。
2. `disabledPrimary` 用 30% 真主色 `#4D0066FF`，不照抄偏色资源 `#4D2660F5`。
3. Checkbox / Radio 关闭 & 禁用为半透明环，与 miuix 实心填充结构不符，低置信。
4. windowDimming 浅色 0.3→0.2，遮罩变浅。
5. FloatingToolbar 50→24 设计分歧；Switch / Slider 几何需重算数学。
6. 字体角色映射不定，Subtitle 字重 ColorOS 偏 medium。

## 9. 页面设计系统（卡片 / 分割线 / 节奏）— 设置页 ground truth

> 参照 ColorOS 设置页的卡片分组、行内分割线与页面节奏。以下所有 dp 值都与参考机型截图互证
> （页边距在 3.5 密度下为 56px = 16dp 整除自证），不是从别处抄一个数字了事。

### 9.1 卡片分组

分组算法：以 **category 内可见项**为一组——单条=FULL、首条=HEAD、末条=TAIL、其余=MIDDLE。

| 规则 | 值 | 设计依据 | 实测 | miuix 落点 |
| :-- | :-- | :-- | :-- | :-- |
| 卡片水平页边距 | **16dp**（tiny 12dp） | 设置页标准页边距 | 56px ✓ | example:`Card(Modifier.padding(horizontal = 16.dp))` |
| 卡片圆角 | 名义 **12dp**（见下注） | 中档连续曲率半径（见 §9.5） | wifi 页弧高 30px≈8.6dp ✓ | `CardDefaults.CornerRadius=12dp` ✓ 已对齐 |
| 首 / 尾行额外 padding | **+2dp** | HEAD 顶 +2 / TAIL 底 +2 / FULL 上下各 +2 / MIDDLE 无 | 首尾行 50dp ✓ | 列表卡 `Card(insideMargin = PaddingValues(vertical = 2.dp))` 或首末行自加 |
| 行最小高度 | **48dp** | 设置页 preference 行高 | 行高 50dp（48+2 首行）✓ | `BasicComponent heightIn(min=48.dp)` ✓ 已对齐 |
| 行内容水平 padding | 距卡边 **16dp** | 标题距卡边 16dp（含 16dp 页边距，文本起点距屏 32dp） | 文本起点 32dp 距屏 ✓ | `BasicComponentDefaults.InsideMargin` 水平 16dp ✓ |
| 行文本区垂直 padding | **10dp** | 行文本上下各 10dp | — | `BasicComponentDefaults.InsideMargin` 垂直 10dp ✓ |
| title↔summary 行间 | **2dp** | 主副文本行距 | — | BasicComponent summary `padding(top=2.dp)` ✓ |
| 卡片背景 | #FFFFFF / #1AFFFFFF | 白卡 | 白卡 ✓ | `surfaceContainer` ✓ §2 |

> 圆角注：卡片圆角用平滑曲线绘制（系统默认平滑权重），名义半径取中档 12dp。item 视图占满全宽，卡面向内绘制，涟漪 / 选中态被裁剪在卡 path 内。

### 9.2 行间分割线

**规则：卡内相邻两行之间恰好一条——末行之后、单行卡（FULL）一律不画。** 分割线开关默认开启。

| 属性 | 值 | 设计依据 | 实测 |
| :-- | :-- | :-- | :-- |
| 厚度 | **0.33dp**，绘制时 `max(1px, 0.33dp)` | 极细分割线 | 1px ✓ |
| 绘制方式 | onDrawOver 叠画在行底边上，**不占布局高度** | — | ✓ |
| 颜色 | **#1F000000 / #33FFFFFF** | 白卡上 12% 黑 | RGB(223) ✓ = `dividerLine` §2 |
| start inset | **动态对齐 title 文本起点** | 与标题文字左对齐 | 无 icon 行距卡边 16dp；wifi 自定义 icon 行距卡边 48dp ✓ |
| — 无 icon 行 | 距卡边 **16dp**（=距屏 32dp） | 即 title padding start | 112px ✓ |
| — 标准 icon 行 | 距卡边 **68dp**（=距屏 84dp） | 16 + icon 36 + gap 16 | — |
| end inset | 距卡边 **16dp**（=距屏 32dp），固定 | 与标题右对齐 | 16dp ✓ |
| RTL | start/end 镜像交换 | — | — |
| 按压态 | 按压行上下两条分割线 alpha 过渡 | 可选增强 | 未复刻（可选） |

> 遗留的 18dp 左右边距在本设计里无可消费点，**勿用 18dp**——正确值是距卡边 16dp。
>
> **miuix 落点**：`HorizontalDivider(Modifier.padding(horizontal = DividerDefaults.CardInset))`（CardInset=16dp，已加，KDoc 含 icon 行规则）；example 现有手拼写法已符合。末行后不画由使用侧保证（仅在相邻行之间插入）。

### 9.3 分组标题（category）

| 规则 | 值 | 设计依据 | 实测 |
| :-- | :-- | :-- | :-- |
| category 容器与上一卡间距（topMargin） | **16dp** 默认 | 卡间距（small=12 / zero=0 可切） | 参与 48.3dp 合成 ✓ |
| 标题字体 | **12sp sans-serif-medium** | 分组标题字号 | 字形高 10.9dp ✓ |
| 标题颜色 | #8A000000 / #8AFFFFFF | 次级文字色 | ✓ = `onBackgroundVariant`（SmallTitle 现用）✓ |
| 标题上下 margin | **各 8dp**（minHeight 16dp） | 标题盒上下内边距 | ✓ |
| 标题 start | 距屏 **32dp** = 距卡边 16dp | 与卡片文字左对齐 | 字形沿 33.4dp ✓ |
| **卡→卡（带标题）合成** | 16 + 8 + 行盒(~16) + 8 ≈ **48dp** | 上述四项相加 | 48.3dp ✓✓ |
| **卡→卡（无标题 category）** | **16dp** | 仅剩容器 topMargin | — |

**miuix 落点**：`SmallTitleDefaults.InsideMargin = PaddingValues(32.dp, 8.dp)`（已改，水平 32=16 页边+16 卡内对齐）；16dp 卡间距由卡片间隔提供（example 惯例 `Card(Modifier.padding(bottom = 16.dp))`），合成 卡→标题文本 24dp / 标题文本→卡 8dp，与 ColorOS 一致。

### 9.4 页面节奏

| 位置 | 值 | 设计依据 | 实测 |
| :-- | :-- | :-- | :-- |
| toolbar 高度 | 52dp（无附加 padding） | 设置页顶栏高 | 卡顶 = 状态栏 40 + toolbar 52 + 12 ✓ |
| 页顶：toolbar 底 → 第一张卡 | **12dp** | 页面顶部留白 | ✓ |
| 页底：最后一张卡 → 内容末 | **32dp** | 页面底部留白（含 32dp 脚垫） | — |

**miuix 落点**：页面滚动容器 `contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)`（example LazyColumn / Scaffold 内容区）。

### 9.5 分歧与备注

1. **卡片圆角 12 vs 17（已定案：12dp）**：ColorOS 设置页实际消费的是中档连续曲率半径 `12dp`；`17dp` 是更新版设计 token，设置页未消费。参考机型像素复核（density 3.5）：卡角弧高 ≈30px≈8.6dp，与 12dp squircle 弧高（≈12×0.71≈8.5dp）吻合，17dp 会得到 ≈15dp 弧高、与实测不符。`CardDefaults.CornerRadius` 已由 17 回退到 12dp。
2. 按压行分割线 alpha 过渡、卡片选中 / 涟漪态未复刻，不影响静态视觉。
3. 首 / 尾行 +2dp 属**卡内**空间（计入行高 48→50），不是卡外间距；Compose 侧最简落点是列表卡 `insideMargin = PaddingValues(vertical = 2.dp)`，example 待接入。
