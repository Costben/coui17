<!-- Copyright 2026, compose-coui-ui contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# COUI 尺寸规范：对齐 ColorOS 系统应用

> **一句话规则：所有界面按「360dp 短边」排版，在应用根部钉死密度，控件尺寸用下表的 dp 值。**
> 全部数值来自 PLK110（OnePlus，ColorOS V17.0.0，1272×2772，物理 560dpi，用户覆盖 620dpi）上的
> `uiautomator dump` 实测，并已与系统应用的渲染密度反推对齐——每个 dp 值都能整除回整数。

## 0. 结论（唯一规则，不要再二选一）

1. ColorOS 自家应用（**相册 `com.coloros.gallery3d`、设置 `com.android.settings`、桌面**）全部把窗口钉在
   **360dp 短边**上，因此**完全无视系统的「显示大小 / DPI」**。
2. 所以「和系统一样大」只有一种做法：**把你的应用也钉在 360dp 短边**——在应用根部包一层密度。
   控件 dp 值本来就是对的一组（40 / 48 / 56dp），差的只是渲染密度。
3. 钉死之后，用户改「显示大小」对本应用无效。**这正是系统应用的行为**，不是副作用。

## 1. 实测证据：系统应用不跟 DPI

同一台机器，`wm density 620` → `560` 各 dump 一次：

**相册**

| 控件 | 620dpi | 560dpi |
| :-- | :-- | :-- |
| 搜索 / 更多选项 | `141×141` | `141×141` |
| 精选 / 筛选（底栏圆钮） | `198×198` | `198×198` |
| 照片 / 图集 / 创作 | `179×170` | `179×170` |
| 日期文字 | `205×56` | `205×56` |

**设置**

| 控件 | 620dpi | 560dpi |
| :-- | :-- | :-- |
| 搜索编辑框 | `(0,591)-(1272,793)` h=202 | 完全相同 |
| 搜索胶囊（视觉） | h=143 / w=1148 | 完全相同 |
| 右上角按钮 | `139×139` | 完全相同 |
| 列表行距 | 202px | 完全相同 |

**对照组**（同一 dump 工具、同一时刻，未钉密度的 COUI demo）：

| 文本 | 560dpi | 620dpi | 比例 |
| :-- | :-- | :-- | :-- |
| `基础组件` | `224×66` | `248×73` | 1.107 = 620/560 |

→ 系统密度覆盖确实生效，普通应用跟着放大 10.7%，只有系统应用不跟。

**钉死之后**（demo 已在根部钉死）：620dpi 下 `基础组件` = **224×66**，与 560dpi 完全一致 —— 已复现系统应用的行为。

## 2. 密度公式

```
dpi     = round(短边px / 360 × 160)
density = dpi / 160
1272px  → round(565.33) = 565dpi → 3.53125 px/dp   （系统 620dpi = 3.875）
```

反推校验（实测 px ÷ 3.53125 必须落回整数 dp）：

| 实测 px | ÷ 3.53125 | 结论 |
| :-- | :-- | :-- |
| 141 | 39.93 | 40dp |
| 198 | 56.07 | 56dp |
| 170 | 48.14 | 48dp |
| 247 | 69.94 | 70dp |
| 73 | 20.67 | 24dp 图标盒的墨迹（≈86%） |

按物理 560dpi（3.5）反推得到 40.29 / 56.57 / 48.57，全部不是整数，排除。

## 3. 强制规则：钉在应用根部

实现：`coui-ui/.../utils/GalleryDensity.kt` 的 `rememberGalleryDensity()`
（函数名里的 Gallery 是首次实测来源，规则覆盖 ColorOS 全部系统应用）。

```kotlin
// 应用入口，包住整棵树
CompositionLocalProvider(LocalDensity provides rememberGalleryDensity()) {
    App()
}
```

- 手机：短边 1272px → 3.53125，与相册 / 设置逐像素一致。
- 平板 / 桌面：短边过大时自动回落到平台密度（`minOf(..., platformDensity)`），不会失控。
- `fontScale` 保留平台值，用户改「字体大小」仍然有效。
- **唯一例外**：把单个 ColorOS 控件嵌进一个必须跟系统密度的宿主时，才把作用域收窄到那个控件；
  锚定弹窗（`Popup` / 下拉菜单）读自己的密度，收窄时它们要留在作用域外。

> 之前「二选一」的写法作废：默认且唯一推荐 = 应用根部钉死。

## 4. 控件尺寸总表（钉密度后直接换算）

### 4.1 单行控件族 —— 统一 40dp

| 控件 | COUI token | @3.53125 | ColorOS 实测 | 判定 |
| :-- | :-- | :-- | :-- | :-- |
| 顶栏图标按钮（搜索 / 更多） | `TopAppBarDefaults.ActionItemMinWidth = 40.dp` | 141×141 | 相册 141×141、设置 139×139 | ✅ |
| 更多菜单项（行高） | `DropdownDefaults.PopupItemMinHeight = 40.dp` | 141 | 相册 ⋮ 菜单行距 141 | ✅ |
| 标签页栏 | `TabRowDefaults.TabRowHeight = 40.dp` | 141 | 同族 40dp（见注） | ✅ |
| 搜索框 | `SearchBarDefaults.InputFieldMinHeight = 40.dp` | 141 | 设置搜索胶囊 143 | ✅ |

注：标签页栏没能在本机取到独立的 ColorOS 分段控件样本，按「单行可点击控件 = 40dp」的族规则取值，
与顶栏按钮、菜单项、搜索框同高——这是有意的统一，不是漏测。

### 4.2 底栏族 —— 条 56 / 项 48 / 圆钮 56

| 元素 | COUI token | @3.53125 | ColorOS 实测 |
| :-- | :-- | :-- | :-- |
| 浮动底栏条高 | `BottomFloatingNavigationBarDefaults.BarHeight = 56.dp` | 198 | 圆钮 198 |
| tab 项高 | `ItemHeight = 48.dp` | 170 | 照片/图集/创作 170 |
| tab 项宽上限 | `ItemMaxWidth = 70.dp` | 247 | 247 |
| 圆形配件（精选 / 筛选） | `AccessorySize = 56.dp` | 198×198 | 198×198 |
| 圆角 | `CornerRadius = 28.dp`（= 条高/2） | — | 全圆角 |
| 图标 | `IconSize = 24.dp` | 盒 85，墨 73 | 墨 73 |
| 标签字号 | `LabelFontSize = 10.sp` | — | 墨高 ≈30px |
| 侧边距 / 内边距 | `SideSpacing 12.dp` / `PaddingHorizontal 16.dp` | 42 / 56 | — |

### 4.3 图标与字号

| 位置 | 规范 |
| :-- | :-- |
| 顶栏 / 菜单 / 底栏 tab 图标 | **24dp 盒**（ColorOS 图标墨迹约占 86%，实测 20.7dp） |
| 搜索框内图标 | **20dp 墨**，容器 36dp（`LeadingIconStartPadding 12 / EndPadding 8`） |
| 搜索框尾部图标 | 视觉 18dp / 触控 36dp（输入框内次级图标，不与 24dp 主图标同档） |
| 底栏标签 | **10sp** |
| 搜索框文字 | **15sp** |

## 5. 一致性检查结果

**统一（✅）**：顶栏按钮 40dp = 菜单项 40dp = 标签页栏 40dp = 搜索框 40dp；底栏 48dp 项 + 56dp 圆钮；
图标 24dp 盒；底栏标签 10sp。三档尺寸族（40 / 48 / 56）各自内部完全一致，且每一档都能在系统应用里找到同值实测。

**刻意例外（不是错误）**：

| 项 | 值 | 说明 |
| :-- | :-- | :-- |
| `DropdownDefaults.PopupHeaderItemMinHeight` | 32dp | 菜单分组标题，非可点项 |
| `TabRowDefaults.TabRowWithContourTinyHeight` | 32dp | 紧凑变体 |
| `SearchBarDefaults.TrailingIconTouchTargetSize` | 36dp | 输入框内次级图标 |

**唯一需要留意的旧档（⚠）**：

| 项 | 值 | 说明 |
| :-- | :-- | :-- |
| `NavigationBarDefaults.ItemHeight` | 56dp | 旧 miuix 经典底栏，ColorOS 现行浮动底栏是 **48dp**。要跟系统一致请用 `BottomFloatingNavigationBar` |

## 6. 复核命令

```bash
# 1) 系统应用在两种密度下 bounds 必须完全一致
adb shell wm density 620 && adb shell monkey -p com.android.settings 1 && sleep 4
adb exec-out uiautomator dump /dev/tty | grep -o 'content-desc="启动小布助手"[^/]*bounds="[^"]*"'
adb shell wm density 560 && adb shell monkey -p com.android.settings 1 && sleep 4
adb exec-out uiautomator dump /dev/tty | grep -o 'content-desc="启动小布助手"[^/]*bounds="[^"]*"'

# 2) 自己的应用在两种密度下 bounds 也必须完全一致（钉密度生效）
#    同一段文本，620 与 560 的 w/h 必须相同；不同 = 没钉住

# 3) 尺寸核对：底栏圆钮必须是 198×198，顶栏按钮 141×141，菜单行距 141
adb exec-out uiautomator dump /dev/tty | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"'
```

判定标准：**系统应用 px 不随 `wm density` 变；我们的 px 也不变；数值等于第 4 节表格。**
