# HintAnimationLayout

`HintAnimationLayout` 是搜索栏的轮播提示文案。它在搜索框上方绘制两条提示字符串，每隔几秒把当前一条滑出、下一条滑入，两者同时做交叉淡入淡出。`query` 为空时轮播，输入内容后停止并隐藏提示，行为与原生 `TextWatcher` 完全一致。

本组件移植自 `com.coui.appcompat.searchview.COUIHintAnimationLayout`，以及 `res/layout/coui_search_view_animated_support_layout.xml` 中的提示视图。

<div style="position: relative; height: 250px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../../compose/index.html?id=hintAnimationLayout" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## 导入

```kotlin
import io.github.suqi8.coui.kmp.basic.HintAnimationDefaults
import io.github.suqi8.coui.kmp.basic.HintAnimationLayout
```

## 原生来源

原生控件是一个 `FrameLayout`，内部持有两个提示 `TextView`（`mHintTextViewFirst` / `mHintTextViewThen`）。每隔 `INTERVAL_TIME`，它把提示下标加一并对提示列表取模，再由 `applyHintChangeAnimation` 播放两组时长为 `DURATION_TIME` 的 `AnimatorSet`。旧的文案边淡出边向 `-distance` 位移；新的文案边淡入边从 `+distance` 进入，其动画组晚 `IN_DELAY_TIME` 启动。位移动画使用 `TRANSLATEINTERPOLATOR`（`PathInterpolatorCompat.create(0.3f, 0f, 0.2f, 1f)`），透明度动画使用 `EASEINTERPOLATOR`（`COUIEaseInterpolator`，即 `PathInterpolator(0.33f, 0f, 0.67f, 1f)`）。

`COUIHintAnimationLayout` 通过 `setSearchEditText` 给搜索 `EditText` 挂上 `TextWatcher`——`coui_search_view_animated_support_layout.xml` 则直接取第 0 个子 View。输入非空时取消待执行的轮播、隐藏两个提示视图并复位其位移与透明度；输入为空时把最后一条字符串放回。在 Compose 中文本值由输入框自己持有，因此当前文本通过 `query` 传入。

本组件正是 [SearchViewAnimate](../components/searchviewanimate) 未包含的那部分；两者是独立的类，本组件有各自的公开 API。

## 基本用法

```kotlin
var query by remember { mutableStateOf("") }
val hints = listOf("搜索设置", "搜索消息", "搜索应用和文件")

HintAnimationLayout(
    hints = hints,
    query = query,
    modifier = Modifier.fillMaxWidth()
) {
    InputField(
        query = query,
        onQueryChange = { query = it },
        onSearch = {},
        expanded = false,
        onExpandedChange = {},
        leadingIcon = {},
        label = "",
        modifier = Modifier.fillMaxWidth()
    )
}
```

## 参数

| 参数 | 类型 | 说明 |
| :--- | :--- | :--- |
| `hints` | `List<String>` | 轮播的字符串列表；空列表（或全为空串）会停止轮播 |
| `modifier` | `Modifier` | 应用于布局的修饰符 |
| `query` | `String` | 搜索框当前文本；非空时隐藏提示并停止轮播 |
| `enabled` | `Boolean` | 是否运行轮播 |
| `repeatCount` | `Int` | 播放多少次切换后停止；`-1` 表示持续轮播直到组合变化 |
| `textSize` | `TextUnit` | 提示文字大小；`TextUnit.Unspecified` 保留 `style` 的大小 |
| `textColor` | `Color` | 提示颜色；`Color.Unspecified` 保留样式自身颜色 |
| `style` | `TextStyle` | 提示文本样式，默认使用主题中 `couiTextAppearanceBodyL` 这一档 |
| `onHintAnimationChange` | `(index: Int, hint: String) -> Unit` | 切换开始时回调，携带新的下标与即将进入的字符串 |
| `content` | `@Composable BoxScope.() -> Unit` | 提示所覆盖的搜索框 |

### HintAnimationDefaults

| 常量 | 类型 | 值 | COUI 来源 |
| :--- | :--- | :--- | :--- |
| `DurationMillis` | `Int` | 250 | `COUIHintAnimationLayout.DURATION_TIME` |
| `InDelayMillis` | `Long` | 150 | `COUIHintAnimationLayout.IN_DELAY_TIME` |
| `IntervalMillis` | `Long` | 3000 | `COUIHintAnimationLayout.INTERVAL_TIME` |
| `RepeatCount` | `Int` | -1 | `COUIHintAnimationLayout.mRepeatCount` |
| `TranslateExtra` | `Dp` | 5.dp | `dimen/coui_search_bar_animation_translate_extra` |

`textStyle()` 是默认的提示样式：`COUIHintAnimationLayout` 用 `Widget_COUI_EditText_SearchViewStyle_HintText` 包装主题里的 `couiTextAppearanceBodyL`（16sp，`coui_spacing_multiplier_body_l`）给两个提示视图，本移植使用主题的 `body1`，即同一档 16sp。

## 动画

一次切换把提示下标对列表取模后加一，然后播放两条轨道，时长均为 `DurationMillis`：

| 轨道 | 目标 | 时长 | 缓动 |
| :--- | :--- | :--- | :--- |
| 旧文案位移 | `-distance` | 250ms | `TRANSLATE_EASING` |
| 旧文案透明度 | 0 | 250ms | `EASE_EASING` |
| 新文案位移 | 0（从 `+distance` 起） | 250ms，延迟 150ms | `TRANSLATE_EASING` |
| 新文案透明度 | 1（从 0 起） | 250ms，延迟 150ms | `EASE_EASING` |

`distance` 为 `(measuredHeight - lineHeight) / 2 + TranslateExtra`，让每条文案刚好走出裁剪边缘。两个槽位各自持有一对 slider 并复用，对应原生对已有 `AnimatorSet` 的重新定目标。

## 复原的令牌

以下数值全部取自 ColorOS 17 的 `Settings.apk` 资源转储或反编译的 `COUIHintAnimationLayout`，没有一个是臆造的。

| 令牌 | 值 | 来源 |
| :--- | :--- | :--- |
| `DurationMillis` | 250ms | `COUIHintAnimationLayout.DURATION_TIME` |
| `InDelayMillis` | 150ms | `COUIHintAnimationLayout.IN_DELAY_TIME` |
| `IntervalMillis` | 3000ms | `COUIHintAnimationLayout.INTERVAL_TIME` |
| `RepeatCount` | -1 | `COUIHintAnimationLayout.mRepeatCount` |
| `TranslateExtra` | 5dp | `dimen/coui_search_bar_animation_translate_extra` |
| `TranslateEasing` | `cubic-bezier(0.3, 0, 0.2, 1)` | `COUIHintAnimationLayout.TRANSLATEINTERPOLATOR`（`PathInterpolatorCompat.create(0.3f, 0f, 0.2f, 1f)`） |
| `EaseEasing` | `cubic-bezier(0.33, 0, 0.67, 1)` | `COUIEaseInterpolator` |
| 提示文字外观 | `couiTextAppearanceBodyL`（16sp） | `Widget_COUI_EditText_SearchViewStyle_HintText` |
| 位移距离 | `(measuredHeight - lineHeight) / 2 + TranslateExtra` | `applyHintChangeAnimation` |

## 未移植

- **`setHintsAnimation` 在输入框已有文本时拒绝执行**——原生会通过 `TAG` 打日志并调用 `mSearchEditText.setHint("")`；本移植在 `query` 非空时直接停止轮播，输入框自身的占位文案交给调用方。
- **`setSearchEditText` 与「第 0 个子 View 必须是 `EditText`」的兜底逻辑**，以及强制它们的两个 `Log.e` 保护；搜索框由调用方通过 `content` 传入。
- **`setHintTextColor(ColorStateList)`**——`textColor` 只携带单个 `Color`；`Color.Unspecified` 保留样式自身颜色。
- **`setTextSize(int)` 复制 `EditText` 的像素字号**——改为 `textSize` 这个 `TextUnit` 覆盖项。
- **`onAttachedToWindow` / `onDetachedFromWindow` / `onWindowVisibilityChanged`** 以及命令式的 `pauseHintsAnimation` / `resumeHintsAnimation` 组合；本移植把轮播绑定到组合、`enabled` 与 `query`。
- **`getCurrentHintTextView()` 与 `COUIHintAnimationChangeListener` 的 `TextView` 参数**——`onHintAnimationChange` 只回调下标与字符串。
- **`id/coui_hint_text_view_first` / `id/coui_hint_text_view_then` 两个视图 id**，原生把它们分配给自己创建的子 View。
