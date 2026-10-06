# InstallLoadProgress

`InstallLoadProgress` 是应用安装进度控件，也就是安装器里那个会在按钮与进度之间来回变形的控件。它是安装器实际 inflate 的按钮类，使用安装进度样式的 `InstallDownloadSmall` 变体。

本组件对标 ColorOS 17 的安装进度控件实现，包括平滑圆角分支上的 `Build.VERSION.SDK_INT >= 30` 判断。

<div style="position: relative; height: 250px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../../compose/index.html?id=installLoadProgress" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## 导入

```kotlin
import io.github.suqi8.coui.kmp.basic.InstallLoadProgress
import io.github.suqi8.coui.kmp.basic.InstallLoadProgressColors
import io.github.suqi8.coui.kmp.basic.InstallLoadProgressDefaults
import io.github.suqi8.coui.kmp.basic.InstallLoadStyle
```

## 原生来源

[LoadProgressButton](../components/loadprogressbutton) 是同一族控件的另一半，并且先落地：它移植了基类 `COUILoadProgress` 的状态机（`DEFAULT_UP_OR_DOWN` = 0、`UPING_OR_DOWNING` = 1、`UP_OR_DOWN_WAIT` = 2、`UP_OR_DOWN_FAIL` = 3、`INSTALL_HAVE_GIFT` = 4，以及 `toggle()`），并对 `COUIInstallLoadProgress` 补充的绘制做了一版初步实现。

`InstallLoadProgress` 不重复其中任何一项：它直接取用那个组件的 `LoadProgressState`、`LoadProgressButtonSize` 与 `LoadProgressButtonDefaults.glyph`，只补上 `COUILoadProgress` 完全没有的概念——加载样式、按状态决定的填充、两趟文案配色、平滑圆角与按压几何。

同包下的 `COUILoadingView` 是相邻的类，但它是一个无关的只读转圈指示器（由 `ProgressIndicator` 移植），与本控件不共用任何代码。

## 基本用法

```kotlin
var state by remember { mutableStateOf(LoadProgressState.Default) }

InstallLoadProgress(
    onClick = { state = state.toggled() },
    state = state,
    text = "Download",
    style = InstallLoadStyle.Default
)
```

## 样式档位

`mLoadStyle` 来自 `couiStyle` 属性，其枚举在资源转储中声明为 `defaultSmallRoundButton`（0）、`bigRoundButton`（1）与 `smallCircleButton`（2）。两个 APK 的布局使用的都是 `InstallDownloadSmall`，它设置 `couiStyle=0`。

| 档位 | `couiStyle` | 尺寸 | 形状 | 来源 |
| :--- | :--- | :--- | :--- | :--- |
| `Default` | 0 | 52dp × 28dp | 胶囊，半径取半高 | `dimen/coui_install_download_progress_width` / `_height` |
| `BigRound` | 1 | 220dp × 44dp | 胶囊，半径取半高 | `dimen/coui_install_download_progress_width_large` / `_height_large` |
| `Circle` | 2 | 33dp × 33dp 方框 | 30dp 平滑圆角圆盘 | `dimen/coui_install_download_progress_circle_width` / `_circle_height` / `_default_circle_radius` |

`Circle` 档位是最容易凭肉眼写错的一个。`updateRoundRectPath` 用 `mCurrentCircleRadius` 内缩路径矩形，而 `setLoadStyle` 把它算成 `getDefaultSize(dimen/coui_install_download_progress_default_circle_radius, 1.5f, true)`——即 16.5dp 半径**减去** 1.5dp，所以 33dp 触摸框里的圆盘只有 30dp；同时它传入的圆角半径是 `dimen/coui_install_download_progress_circle_round_border_radius`（7dp），而不是圆盘的一半。这个轮廓是平滑圆角方块，不是圆。

`isOs16()`（`RoundCornerUtil.getSmoothStyleType() == 1`）在两条圆角路径之间二选一，而且只对胶囊生效：`updateRoundRectPath` 在 `mLoadStyle == 2` 时提前返回，根本到不了这个分支。ColorOS 17 分支调用 `OplusPathAdapter.addSmoothRoundRect(rect, rect.height() * getScaleY() / 2, …, CCW)`，即取半高半径走平台的平滑圆角路径；旧分支调用 `COUIShapePath.getRoundRectPath(rect, height / 2 - dimen/coui_install_download_progress_round_border_radius_offset)`。本移植用 `SquircleShape` 表达两者，并把旧版圆角占用倍数（`radius * 128.19 / 100`，即 `COUIShapePath.getRoundRectPath` 中所有曲线偏移的缩放系数）保留为 `InstallLoadProgressDefaults.LEGACY_CORNER_EXTENSION`，用于圆盘。

## 状态矩阵

`onDraw` 先按 `mState` 分支，再按 `mLoadStyle` 分支。`onDrawCircle` / `onDrawRoundRect` 的 `z` 参数为 true 时取 `mColorPrimary`，为 false 时取 `mColorSecondary`：

| 状态 | `Circle`（2） | `BigRound`（1） | `Default`（0） |
| :--- | :--- | :--- | :--- |
| `Default`（0） | 圆盘 `z = false`，无填充 | 形状 `z = true`，无填充 | 形状 `z = false`，无填充 |
| `Loading`（1） | 圆盘 `z = true` | 形状 `z = false` + 填充 `z = true` | 同 `BigRound` |
| `Wait`（2） | 圆盘 `z = true` | 形状 `z = false` + 填充 `z = true` | 同 `BigRound` |
| `Fail`（3） | 圆盘 `z = true`，重试图标 | 形状 `z = true`，**整个**形状 | 同 `BigRound` |
| `InstallHaveGift`（4） | 什么都不画 | 只画文案 | 只画文案 |

这里有三个 `LoadProgressButton` 不具备的结论：

- 静止时只有 `bigRoundButton` 胶囊是主题色实心；小胶囊与圆盘都停在 `mColorSecondary` 上；
- 填充只存在于传输态。`mSpace` 只在 `mState == 1 || mState == 2` 分支中计算，取值 `(int) (fraction * (mTouchModeWidth - 2 * mExpandOffsetX)) + mExpandOffsetX`——静止时就是 `fraction * width`。`Fail` 画的是整条路径而不是某个比例，`Default` 完全不画填充，因此静止按钮上的 `progress` 不会产生任何视觉变化；
- `INSTALL_HAVE_GIFT` 不匹配任何分支。`onDraw` 会跳过全部绘制调用，只剩文案画在什么都没有的背景上；而 `Circle` 档位连文案也不画，因为那一趟本身被 `mLoadStyle != 2` 挡住。赠品徽标本身只从 `couiInstallGiftBg` 属性读进 `mInstallGiftBg`，在这个版本里从未被绘制。

## 文案配色

胶囊档位的文案会画两趟。`onDrawText` 先用解析出的文案色画一次，当 `mIsChangeTextColor` 被置位时，再在 `canvas.clipRect(f, f2, mSpace, f4)` 内用 `mSurpassProgressColor` 画一次，让压在填充区上的那部分翻转颜色：

| 状态 | 填充区之外 | 填充区之内 |
| :--- | :--- | :--- |
| `Default`（0） | `mColorPrimary` | — |
| `Loading` / `Wait` | `mColorPrimary` | `mSurpassProgressColor`（`#ffffffff`） |
| `Fail`（3） | `mColorWhite`（`#ffffffff`），只画一趟 | — |

## 动画

| 动画器 | 时长 | 效果 |
| :--- | :--- | :--- |
| `mPressedAnimator` | 200ms | 亮度 1.0 → `brightness`；`mExpandOffsetX` / `mExpandOffsetY` 升到实测尺寸的 5%；文案收窄到 0.92；`Circle` 档位半径收缩到 `mDefaultCircleRadius * 0.9` |
| `mNormalAnimator` | 340ms | 以上全部回到静止值；`Circle` 档位做图标交叉淡入淡出（`CIRCLE_IN_ALPHA_HOLDER` 0 → 255，`CIRCLE_OUT_ALPHA_HOLDER` 255 → 0） |

两者都用 `COUIMoveEaseInterpolator`，即 `PathInterpolator(0.3f, 0f, 0.1f, 1f)`。

外扩偏移其实是**内缩**：`updateRoundRectPath` 构造的是 `RectF(mExpandOffsetX, mExpandOffsetY, width - mExpandOffsetX, height - mExpandOffsetY)`，所以按压时形状是每条边各收 5%，而不是变大。按下动画会把每个偏移限制在对应实测尺寸的 `DEFAULT_MIN_PRESS_FEEDBACK`（0.005）以上；抬起动画没有这个下限，因此数值可以自由回到 0。`super.performClick()` 在 `mNormalAnimator` 的 `onAnimationStart` 中触发，所以状态在抬手时翻转——并且只有抬手点落在 `mTouchModeWidth` × `mTouchModeHeight` 内才会触发。

`getCurrentColor` 不是简单的 RGB 压暗：它走 `ColorUtils.colorToHSL`，把亮度乘以 `mCurrentBrightness` 再转回 RGB，并保留原 alpha。只有形状走这条路径；文案与图标直接取原始颜色。

## 复原的令牌

| 令牌 | 值 | 来源 |
| :--- | :--- | :--- |
| `MAX_BRIGHTNESS` | 0.8 | `DEFAULT_BRIGHTNESS_MAX_VALUE` 与 `brightness` 属性的默认值 |
| `MIN_PRESS_FEEDBACK` | 0.005 | `DEFAULT_MIN_PRESS_FEEDBACK` |
| `SCALE_PARAMETER` | 0.05 | `DEFAULT_SCALE_PARAMETER` |
| `NARROW_FINAL_VALUE` | 0.92 | `DEFAULT_NARROW_FINAL_VALUE` |
| `CIRCLE_RADIUS_SCALE` | 0.9 | `mDefaultCircleRadius * 0.9` |
| `PRESS_DURATION_MILLIS` / `MORPH_DURATION_MILLIS` | 200 / 340 | `PRESS_ANIMATOR_DURATION` / `NORMAL_ANIMATOR_DURATION` |
| `CROSSFADE_DURATION_MILLIS` | 340 | `mNormalAnimator` 的图标透明度轨道（`CIRCLE_MAX_ALPHA` 255） |
| `CircleCornerRadius` | 7dp | `dimen/coui_install_download_progress_circle_round_border_radius` |
| `OnePointFive` | 1.5dp | `ONE_POINT_FIVE`，即 `getDefaultSize` 的差值 |
| `RadiusOffset` | 0.5dp | `dimen/coui_install_download_progress_round_border_radius_offset` |
| `LEGACY_CORNER_EXTENSION` | 1.2819 | `COUIShapePath.getRoundRectPath` 的圆角占用 `128.19 / 100` |
| `MoveEase` | cubic-bezier(0.3, 0, 0.1, 1) | `COUIMoveEaseInterpolator` |
| 主色 | `?couiColorPrimary` | `style/COUIOverlay.Theme.Blue.Default` → `color/couiBlueTintControlNormal`（`#ff006aff`） |
| 次色 | `?couiColorSecondary` | 同一主题 → `color/couiBlueTintLightNormal`（`#26007aff`） |
| 文案白 | `#ffffffff` | `color/coui_install_load_progress_text_color_in_progress` |
| 禁用色 | `#14000000` | `disabledColor` = `?attr/couiBtnDrawableColorDisabled` → `color/coui_btn_drawable_color_disabled` |
| 文案内边距 | 6dp | `style/Widget.COUI.COUILoadProgress` 的 `couiInstallPadding` |
| 文案字号 | 14sp | `dimen/coui_install_download_progress_textsize` |
| 上限 / 进度 / 文案 | 100 / 0 / `"Download"` | `COUILoadProgress.init()`、`string/coui_install_download_progress_textview` |

## 无障碍

`onInitializeAccessibilityEvent` 把 `mMax` 作为条目总数、`mProgress` 作为当前条目下标发布，`getAccessibilityClassName` 报告为 `ProgressBar`。内容描述在状态 0、2、3 下是文案本身，而在状态 1 下是 `mDownloadingContentDecrpition`——即 `setDownloadingContentDecrpition(String)` 设置的值，本移植暴露为 `downloadingContentDescription`。

`onTouchEvent` 在 `ACTION_DOWN` 与 `performTouchEndAnim` 开头都会调用 `performHapticFeedback(COUIHapticFeedbackConstants.GRANULAR_SHORT_VIBRATE)`，由 `mIsNeedVibrate` 控制（`loadingButtonNeedVibrate` 属性，在 `style/Widget.COUI.COUILoadProgress` 中为 false）。本移植暴露为 `hapticFeedback`。

## 未移植

- **`addApostrophe` 与文案适配**——原生用 `TextPaint.breakText` 按 `mDefaultWidth - mTextPadding * 2 - apostropheWidth` 测量（`mDefaultWidth` 即 `mTouchModeWidth - 1.5dp * 2`），英文文案再经 `isEnglish` 在最后一个空格处截断，并追加 `string/coui_install_load_progress_apostrophe`（`"..."`）。本移植交给 `TextOverflow.Ellipsis`，多词文案的截断位置可能与原生不同。
- **`mButtonDrawable` / `couiDefaultDrawable`**——`COUILoadProgress` 允许传入一个矢量图，居中绘制以取代文案，并配一个用 `mSurpassProgressColor` 上色、在同一个填充 `clipRect` 内绘制的 `mButtonDrawableReverseColor`。两个 APK 中没有任何样式提供 `couiDefaultDrawable`，因此本移植始终绘制文案。
- **`mInstallGiftBg`**——构造时从 `couiInstallGiftBg` 读入后从未使用；为保持一致，这里的 `INSTALL_HAVE_GIFT` 同样不画任何东西。
- **RTL 镜像**——`onDraw` 在 `ViewUtils.isLayoutRtl` 为真时会同时镜像填充与文案 `clipRect`。本移植的填充也会镜像（`Alignment.CenterStart` 跟随布局方向），但文案裁剪没有镜像，因为 Compose 的 `drawWithContent` 读不到布局方向；在 RTL 布局下文案会在错误的一侧翻转颜色。`LoadProgressButton` 有同样的缺口。
- **`mThemeColorStateList`、`mThemeSecondaryColorStateList`、`mBtnTextColorStateList`**——原生允许调用方用 `ColorStateList` 覆盖每个角色；本移植改为暴露普通的 `Color`。
- **`setTouchModeWidth` / `onConfigurationChanged` 的宽度补偿**——COUI 在语言于中文与其他语言之间切换时手工增删 `dimen/coui_install_download_progress_width_in_foreign_language`（30dp）。本移植按固定尺寸布局，不做这个调整。
- **`OplusPathAdapter` 与 outline provider**——原生通过 `setOutlineProvider` + `setClipToOutline(true)` 裁剪自己的路径。本移植用 `Modifier.clip(shape)`。
- **死字段**——`mTouchModeCircleRadius`（`dimen/coui_install_download_progress_default_circle_radius`）只赋值不读取，`DEFAULT_SCALE_PERCENT`（0.4）只声明不使用。两者都未移植。
- **`setMaxBrightness(int)`**——原生 setter 收 `int` 而 `mMaxBrightness` 是 `float`，调用无法把存进去的值取回来。本移植只通过 `InstallLoadProgressDefaults.MAX_BRIGHTNESS` 暴露亮度。
- **字号自适应**——`setDefaultTextSize` 会把解析出的字号交给 `COUIChangeTextUtil.getSuitableFontSize(size, fontScale, 2)`。本移植使用 `sp`，Compose 文本栈本身就会按平台字体缩放。
- **无障碍事件节流**——`AccessibilityEventSender` 每 10ms 投递并合并一次进度事件；Compose 自身的语义直接发布 `progressBarRangeInfo`。
