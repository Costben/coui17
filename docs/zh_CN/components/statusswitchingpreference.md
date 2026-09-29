# StatusSwitchingPreference

`StatusSwitchingPreference` 是右侧挂件可在状态文本与图标之间切换的偏好设置行，对应 ColorOS 的 `COUIStatusSwitchingPreference`。设备端把 `coui_preference_widget_status_switching.xml` 塞进标准行的 `widget_frame`：一个 `FrameLayout`，内含 `COUITextView`（`couiTextAppearanceBody`、`?couiColorLabelSecondary`）、`ImageView` 与 `EffectiveAnimationView`，由 `showText` / `showIcon` / `showAnim` 保证三者中只有一个可见。

动画分支（`STATUS_TYPE_ANIM`，循环播放的 Lottie `raw` 资源）有意未建模：该类保存的是资源 id 而不是 drawable，没有可交给 Compose 调用方的东西，仓库也不带动画运行时。因此只提供 `StatusSwitchingContent.Text` 与 `StatusSwitchingContent.Icon`，分别对应 `STATUS_TYPE_TEXT` 与 `STATUS_TYPE_IMAGE`。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.StatusSwitchingPreference
import io.github.suqi8.coui.kmp.preference.StatusSwitchingContent
```

## 基本用法

```kotlin
StatusSwitchingPreference(
    title = "电池电量",
    status = StatusSwitchingContent.Text("约 9 小时 12 分可用"),
    onClick = { /* 打开电池详情 */ }
)
```

## 组件状态

### 文本状态

```kotlin
StatusSwitchingPreference(
    title = "蓝牙",
    status = StatusSwitchingContent.Text("已连接")
)
```

### 图标状态

```kotlin
StatusSwitchingPreference(
    title = "定位",
    status = StatusSwitchingContent.Icon(
        imageVector = COUIIcons.Info,
        contentDescription = "定位已开启",
        tint = COUITheme.colorScheme.primary
    )
)
```

### 禁用状态

```kotlin
StatusSwitchingPreference(
    title = "禁用的设置项",
    status = StatusSwitchingContent.Text("不可用"),
    enabled = false
)
```

## 属性

### StatusSwitchingPreference 属性

| 属性名           | 类型                            | 说明                                        | 默认值                                | 是否必须 |
| ---------------- | ------------------------------- | ------------------------------------------- | ------------------------------------- | -------- |
| title            | String                          | 设置项标题                                  | -                                     | 是       |
| status           | StatusSwitchingContent          | 右侧挂件内容：`Text` 或 `Icon`              | -                                     | 是       |
| modifier         | Modifier                        | 应用于组件的修饰符                          | Modifier                              | 否       |
| titleColor       | BasicComponentColors            | 标题文本的颜色配置                          | BasicComponentDefaults.titleColor()   | 否       |
| summary          | String?                         | 设置项摘要                                  | null                                  | 否       |
| summaryColor     | BasicComponentColors            | 摘要文本的颜色配置                          | BasicComponentDefaults.summaryColor() | 否       |
| startAction      | @Composable (() -> Unit)?       | 左侧显示的自定义内容                        | null                                  | 否       |
| statusColor      | Color                           | 状态文本颜色（COUI `?couiColorLabelSecondary`） | COUITheme.colorScheme.onSurfaceSecondary | 否   |
| bottomAction     | @Composable (() -> Unit)?       | 底部显示的自定义内容                        | null                                  | 否       |
| insideMargin     | PaddingValues                   | 组件内部内容的边距                          | BasicComponentDefaults.InsideMargin   | 否       |
| cardListPosition | CardListPosition                | 该行在卡片组中的位置，圆角外边缘会获得额外内边距 | CardListPosition.None             | 否       |
| holdDownState    | Boolean                         | 组件是否处于按下状态                        | false                                 | 否       |
| enabled          | Boolean                         | 组件是否可交互                              | true                                  | 否       |
| onClick          | (() -> Unit)?                   | 点击整行时的回调                            | null                                  | 否       |

### StatusSwitchingContent

| 变体 | 属性                                                          | 对应设备分支    |
| ---- | ------------------------------------------------------------- | --------------- |
| Text | text: String                                                  | `STATUS_TYPE_TEXT` (2)  |
| Icon | imageVector: ImageVector、contentDescription: String?、tint: Color | `STATUS_TYPE_IMAGE` (3) |

### StatusSwitchingPreferenceDefaults

| 属性              | 类型 | 说明                                                                     | 值   |
| ----------------- | ---- | ------------------------------------------------------------------------ | ---- |
| WidgetMarginStart | Dp   | 挂件框的起始外边距，取自 `coui_status_switching_preference_widget_margin_start` | 16dp |

## 说明

### 有意省略

- **`STATUS_TYPE_ANIM`。** 设备端的 `EffectiveAnimationView` 播放 `raw` 资源里的 Lottie 动画。仓库没有动画运行时，且该类只保存资源 id，没有可暴露的 drawable。这里如实记录，而不是用替代的转圈动画冒充。
