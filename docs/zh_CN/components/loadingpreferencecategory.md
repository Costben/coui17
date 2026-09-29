# LoadingPreferenceCategory

`LoadingPreferenceCategory` 是右侧挂件为加载指示器的分组标题，对应 ColorOS 的 `COUILoadingPreferenceCategory`。设备端该类继承 `COUIPreferenceCategory`，并通过 `LoadingType` 切换分组 `widget_frame` 的内容：

| `LoadingType`       | 挂件                                                      |
| ------------------- | --------------------------------------------------------- |
| `LOADING` (0)       | `coui_preference_category_widget_layout_loading.xml`，指示器播放中 |
| `PAUSE` (1)         | 同一布局，指示器暂停                                      |
| `INVISIBLE` (2)     | 同一布局，指示器 `gone`                                   |
| `AFTER_LOADING` (3) | `coui_loading_after_layout` 资源                          |
| `BEFORE_LOADING` (4) | `coui_loading_before_layout` 资源                        |

`PAUSE` 与 `INVISIBLE` 有意未建模。两者都保留分组已有的挂件布局，只是启动或停止 `COUICompProgressIndicator` 的动画；在 Compose 中调用方只要不切换 `loading` 就能表达，没有需要承载的状态。两个换资源的类型则合并为 `widget`，调用方无论对应哪一个，交给它的都是同一份内容。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.LoadingPreferenceCategory
```

## 基本用法

```kotlin
var loading by remember { mutableStateOf(true) }

LaunchedEffect(Unit) {
    delay(1200)
    loading = false
}

LoadingPreferenceCategory(
    title = "电池",
    loading = loading,
    loadingText = "正在统计用电情况…"
)
```

## 组件状态

### 仅指示器

`loadingText` 保持 `null` 时会隐藏文字，只留 16dp 的指示器——与设备端 `TextUtils.isEmpty(mLoadingText)` 分支的行为一致：

```kotlin
LoadingPreferenceCategory(
    title = "扫描中",
    loading = true
)
```

### 加载结束

`loading` 变为 false 后，分组显示 `widget` 插槽，即 `coui_loading_after_layout` 的移植：

```kotlin
LoadingPreferenceCategory(
    title = "电池",
    loading = false,
    widget = {
        Text(
            text = "82%",
            color = COUITheme.colorScheme.onSurfaceSecondary
        )
    }
)
```

## 属性

### LoadingPreferenceCategory 属性

| 属性名      | 类型                      | 说明                                                    | 默认值                                    | 是否必须 |
| ----------- | ------------------------- | ------------------------------------------------------- | ----------------------------------------- | -------- |
| title       | String                    | 分组标题                                                | -                                         | 是       |
| modifier    | Modifier                  | 应用于组件的修饰符                                      | Modifier                                  | 否       |
| loading     | Boolean                   | 是否显示加载挂件（COUI `LoadingType.LOADING`）           | true                                      | 否       |
| loadingText | String?                   | 指示器之前的文字（COUI `text_in_loading`），null 则隐藏 | null                                      | 否       |
| widget      | (@Composable () -> Unit)? | `loading` 为 false 时显示的右侧挂件                     | null                                      | 否       |
| titleColor  | Color                     | 标题文本颜色（`?couiColorSecondNeutral`）                | COUITheme.colorScheme.onBackgroundVariant | 否       |

### LoadingPreferenceCategoryDefaults

| 属性          | 类型 | 说明                                                          | 值   |
| ------------- | ---- | ------------------------------------------------------------- | ---- |
| LoadingSize   | Dp   | `couiSmallLottieLoadingViewWidth` / `_Height`，指示器尺寸      | 16dp |
| TextMarginEnd | Dp   | 加载挂件布局中 `text_in_loading` 的 `layout_marginEnd`         | 4dp  |

## 说明

### 为什么复用 SmallTitle

分组标题使用 `Widget.COUI.List.Category.Title`：`couiTextAppearanceSmallButton`（12sp medium）、`?couiColorSecondNeutral`，水平内边距 32dp、垂直 8dp，最小高度 16dp。仓库的 `SmallTitle` 已经逐项移植了这些值，因此表头复用它，并为右侧挂件对齐同样的内边距，而不是重述一遍文字样式。

### 有意省略

- **`LoadingType.PAUSE` 与 `LoadingType.INVISIBLE`。** 设备端用它们在不改变挂件布局的前提下暂停或隐藏指示器的 Lottie 动画。仓库的 `CircularProgressIndicator` 自己管理动画，只存在「画」与「不画」两种状态，没有可暴露的暂停态。
