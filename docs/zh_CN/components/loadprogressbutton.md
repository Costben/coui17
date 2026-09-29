# LoadProgressButton

`LoadProgressButton` 是 ColorOS 的安装/下载按钮。它是一个**按钮**而不是转圈指示器：点击它会在一个五值状态机上循环，切换所显示的图标——下载箭头、暂停双竖条、重试箭头——在胶囊档位上还会在文案背后填充进度。

本组件移植自 `com.coui.appcompat.progressbar.COUILoadProgress`（继承自 `AppCompatButton`）及其子类 `COUIInstallLoadProgress` 的绘制部分，对应样式 `Widget.COUI.COUILoadProgress` 及其 `InstallDownload` / `InstallDownloadSmall` 变体。

<div style="position: relative; height: 250px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../../compose/index.html?id=loadProgressButton" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## 导入

```kotlin
import io.github.suqi8.coui.kmp.basic.LoadProgressButton
import io.github.suqi8.coui.kmp.basic.LoadProgressButtonDefaults
import io.github.suqi8.coui.kmp.basic.LoadProgressButtonSize
import io.github.suqi8.coui.kmp.basic.LoadProgressState
```

## 原生来源

[ProgressIndicator](../components/progressindicator) 移植的是 `COUILoadingView`、`COUICircularProgressDrawable` 和 `COUILottieLoadingView`——都是只读的转圈指示器。`COUILoadProgress` 是另一个控件：它继承 `AppCompatButton`，可点击，并且自带状态机。两者没有重叠，因此独立成一个组件。

`performClick()` 会调用 `toggle()`，其状态转移为 `0 -> 1`、`1 -> 2`、`2 -> 1`、`3 -> 1`，而 `INSTALL_HAVE_GIFT`（4）在原生代码中没有分支、保持不变。`LoadProgressState.toggled()` 完整重现了这一行为：

| 状态 | 值 | 含义 | 圆形档位图标 |
| :--- | :--- | :--- | :--- |
| `Default` | 0 | 空闲，「Download」 | `circle_load` |
| `Loading` | 1 | 传输中 | `circle_pause` |
| `Wait` | 2 | 排队等待 | `circle_reload` |
| `Fail` | 3 | 传输失败 | `circle_reload` |
| `InstallHaveGift` | 4 | 安装附带赠品 | `circle_load`（原生对状态 4 没有圆形图标） |

## 基本用法

```kotlin
var state by remember { mutableStateOf(LoadProgressState.Default) }

LoadProgressButton(
    onClick = { state = state.toggled() },
    state = state,
    size = LoadProgressButtonSize.Circle
)
```

## 进度

```kotlin
var state by remember { mutableStateOf(LoadProgressState.Default) }
var progress by remember { mutableIntStateOf(0) }

LoadProgressButton(
    onClick = { state = state.toggled() },
    state = state,
    progress = progress,
    max = 100,
    text = if (state == LoadProgressState.Loading) "$progress%" else "Download",
    size = LoadProgressButtonSize.Large
)
```

`progress` 会像 `setProgress` 一样被约束到 `0..max`，填充动画使用 `COUISpringForce` 的 `setBounce(0)` 与 `setResponse(1.0)`。`refreshProgressWithAnim` 在新值与当前值之差不超过 `ONE_MILLION`（1.0E-7）时跳过动画，本移植保留这一判断，避免在空进度条上画出细如发丝的高亮。

## 尺寸档位

`mLoadStyle` 决定绘制形状。`mLoadStyle == 2`（`InstallDownloadSmall`）是圆形：一个按复原半径绘制的实心圆，内部只有图标、没有文案。其余样式都是带文案的圆角矩形。

| 档位 | 宽 | 高 | 圆角半径 | 来源资源 |
| :--- | :--- | :--- | :--- | :--- |
| `Circle` | 33dp | 33dp | 16.5dp | `dimen/coui_install_download_progress_circle_width` / `_circle_height` / `_default_circle_radius` |
| `Small` | 52dp | 28dp | 13.5dp | `dimen/coui_install_download_progress_width` / `_height` / `_round_border_radius_small` |
| `Large` | 220dp | 44dp | 21.5dp | `dimen/coui_install_download_progress_width_large` / `_height_large` / `_round_border_radius` |

## 动画

| 动画器 | 时长 | 效果 |
| :--- | :--- | :--- |
| `mPressedAnimator` | 200ms | 亮度 1.0 → 样式的 `brightness`（0.8）；胶囊按 `DEFAULT_SCALE_PARAMETER`（0.05）的比例外扩；文案收窄到 `DEFAULT_NARROW_FINAL_VALUE`（0.92）；圆形半径收缩到 `mDefaultCircleRadius * 0.9` |
| `mNormalAnimator` | 340ms | 以上全部回到静止值，并在圆形档位上做图标交叉淡入淡出（`CIRCLE_IN_ALPHA_HOLDER` 0 → 255，`CIRCLE_OUT_ALPHA_HOLDER` 255 → 0） |

`super.performClick()` 在 `mNormalAnimator` **开始**时触发，因此状态在抬手时翻转，随后图标交叉淡入到位。

## 复原的令牌

| 令牌 | 值 | 来源资源 |
| :--- | :--- | :--- |
| `Max` | 100 | `COUILoadProgress.init()` |
| `Progress` | 0 | `COUILoadProgress.init()` |
| `DefaultText` | `"Download"` | `string/coui_install_download_progress_textview` |
| `Apostrophe` | `"..."` | `string/coui_install_load_progress_apostrophe` |
| `TextSize` | 14sp | `dimen/coui_install_download_progress_textsize` / `_textsize_large` |
| `TextPadding` | 6dp | `style/Widget.COUI.COUILoadProgress` 的 `couiInstallPadding` |
| `MorphDurationMillis` | 340 | `COUIInstallLoadProgress.NORMAL_ANIMATOR_DURATION` |
| `PressDurationMillis` | 200 | `COUIInstallLoadProgress.PRESS_ANIMATOR_DURATION` |
| `CrossfadeDurationMillis` | 340 | `mNormalAnimator` 的图标透明度轨道 |
| `PressBrightness` | 0.8 | `style/Widget.COUI.COUILoadProgress.InstallDownload{Small}` 的 `brightness` |
| `PressScaleParameter` | 0.05 | `DEFAULT_SCALE_PARAMETER` |
| `PressNarrowFinalValue` | 0.92 | `DEFAULT_NARROW_FINAL_VALUE` |
| `PressCircleRadiusScale` | 0.9 | `mDefaultCircleRadius * 0.9` |
| `GlyphWidth` / `GlyphHeight` | 12dp / 14dp | `drawable/coui_install_load_progress_circle_{load,pause,reload}` 的 `android:width` / `android:height` |
| `ForeignLanguageWidthDelta` | 30dp | `dimen/coui_install_download_progress_width_in_foreign_language` |
| 圆角半径偏移 | 0.5dp | `dimen/coui_install_download_progress_round_border_radius_offset` |
| 容器 / 进度填充 | `?couiColorPrimary`，按压态 `?couiColorPrimaryPressed` | `color/coui_tint_selector`，由 `couiInstallDefaultColor` 与 `couiLoadProgressColor` 指向 |
| 内容（文案与图标） | `#ffffffff` | `color/coui_install_load_progress_text_color_in_progress`、`color/coui_install_load_progress_circle_color_pause`、`color/coui_install_load_progress_circle_color_reload` |
| 下载图标填充 | `?couiColorPrimary` | `drawable/coui_install_load_progress_circle_load` 的 `fillColor` |
| 进度弹簧 | bounce 0, response 1.0 | `refreshProgressWithAnim` 中的 `COUISpringForce` |

下载与重试图标共用同一段路径、同一个 36x42 viewport，仅填充色不同：

```
M 20 34.2 L 31.6 22.5 C 32.4 21.7 33.6 21.7 34.4 22.5 C 35.2 23.3 35.2 24.5 34.4 25.3
L 20.8 39.1 C 19.2 40.7 16.7 40.7 15.1 39.1 L 1.6 25.5 C 0.8 24.7 0.8 23.5 1.6 22.7
C 2.4 21.9 3.6 21.9 4.4 22.7 L 16 34.2 L 16 4 C 16 2.9 16.9 2 18 2 C 19.1 2 20 2.9 20 4
L 20 34.2 Z
```

暂停图标是两条圆角竖条：

```
M 12 8 L 12 34 C 12 35.1 11.1 36 10 36 C 8.9 36 8 35.1 8 34 L 8 8 C 8 6.9 8.9 6 10 6
C 11.1 6 12 6.9 12 8 Z M 28 8 L 28 34 C 28 35.1 27.1 36 26 36 C 24.9 36 24 35.1 24 34
L 24 8 C 24 6.9 24.9 6 26 6 C 27.1 6 28 6.9 28 8 Z
```

## 未移植

- **`INSTALL_HAVE_GIFT`（4）的 `couiInstallGiftBg` 徽标**——赠品背景由属性设置，并非来自可复原的 drawable，因此这里该状态回退为下载箭头。
- **`mSurpassProgressColor`**——原生由资源 id 设置，该 id 在转储中无法解析；本移植把文案覆盖填充区的颜色映射为 `COUITheme.colorScheme.onPrimary`，而非某个复原的常量。
- **未填充轨道色**——`onDrawRoundRect(..., z = false)` 用样式的 `couiColorSecondary` 绘制，该属性的值不在资源转储中；本移植将其映射为 `COUITheme.colorScheme.dividerLine`，与 `ProgressIndicatorDefaults` 使用的轨道角色一致。
- **`ForeignLanguageWidthDelta`**——COUI 会在语言于中文与其他语言之间切换时增删这段宽度，手工调整触摸区域。这里作为令牌暴露但未实际应用。
- **`mThemeColorStateList` / `mThemeSecondaryColorStateList`**——原生允许调用方用 `ColorStateList` 覆盖解析出的颜色；本移植通过 `LoadProgressButtonColors` 暴露普通的 `Color`。
- **无障碍事件节流**——`AccessibilityEventSender` 每 10ms 投递并合并一次进度事件；Compose 自身的语义直接发布 `progressBarRangeInfo`。
