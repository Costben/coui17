# PagerHeaderPreference

`PagerHeaderPreference` 是 COUI 设置页开头的说明文字，对应 ColorOS 的 `COUIPagerHeaderPreference`。设备端该类除布局外没有给 `androidx.preference.Preference` 增加任何东西，布局是 `coui_pager_header_preference.xml`：一个横向 `LinearLayout`，含可选的圆形图标、标题（`COUIPreferenceTitleStyle`）与其下的摘要（`COUIPreferenceDescriptionStyle`），以及一个空的 `widget_frame`。该行垂直内边距 8dp、水平内边距 32dp，顶部外边距 12dp。

它有意不复用 `BasicComponent`：后者强制 48dp 最小高度、摘要用 14sp 正文样式，而本行高度仅由文字决定，摘要用的是 12sp 描述样式。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.PagerHeaderPreference
```

## 基本用法

放在页面第一张卡片上方：

```kotlin
PagerHeaderPreference(
    summary = "以下开关仅影响本机。"
)
```

## 组件状态

### 带标题

```kotlin
PagerHeaderPreference(
    title = "开始之前",
    summary = "更改这些设置可能会重启设备。"
)
```

### 带左侧图标

```kotlin
PagerHeaderPreference(
    summary = "只读信息，无需任何权限。",
    startAction = {
        Icon(
            imageVector = COUIIcons.Info,
            contentDescription = null,
            tint = COUITheme.colorScheme.onSurfaceSecondary
        )
    }
)
```

## 属性

### PagerHeaderPreference 属性

| 属性名       | 类型                            | 说明                                                       | 默认值                                    | 是否必须 |
| ------------ | ------------------------------- | ---------------------------------------------------------- | ----------------------------------------- | -------- |
| summary      | String                          | 说明文字                                                   | -                                         | 是       |
| modifier     | Modifier                        | 应用于组件的修饰符                                         | Modifier                                  | 否       |
| title        | String?                         | 可选标题                                                   | null                                      | 否       |
| startAction  | @Composable (() -> Unit)?       | 左侧显示的自定义内容，对应布局的 `@android:id/icon`         | null                                      | 否       |
| endActions   | @Composable RowScope.() -> Unit | 右侧显示的自定义内容，对应布局的 `widget_frame`             | {}                                        | 否       |
| titleColor   | Color                           | 标题颜色（`coui_preference_title_color`，`?couiColorLabelPrimary`） | COUITheme.colorScheme.onBackground     | 否       |
| summaryColor | Color                           | 摘要颜色（`coui_preference_secondary_text_color`，`?couiColorSecondNeutral`） | COUITheme.colorScheme.onBackgroundVariant | 否 |

### PagerHeaderPreferenceDefaults

| 属性         | 类型          | 说明                                                                              | 值   |
| ------------ | ------------- | --------------------------------------------------------------------------------- | ---- |
| InsideMargin | PaddingValues | `support_preference_header_preference_padding_vertical`（8dp）配合 `support_preference_title_padding_start` / `_end`（32dp） | 水平 32dp，垂直 8dp |
| MarginTop    | Dp            | `support_preference_header_preference_margin_top`                                 | 12dp |
| TextSize     | TextUnit      | `coui_preference_header_footer_textsize`，摘要最终采用的字号                      | 12sp |
