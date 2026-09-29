# PagerFooterPreference

`PagerFooterPreference` 是 COUI 设置页结尾的说明文字，对应 ColorOS 的 `COUIPagerFooterPreference`。设备端该类与 `COUIPagerHeaderPreference` 共用同一种布局形态——图标、标题与其下的摘要、空的 `widget_frame`——但有三处不同：`coui_pager_footer_preference.xml` 把摘要字号固定为 `coui_preference_header_footer_textsize`（12sp）；`onBindViewHolder` 在 `withExtraMarginBottom` 不为 false 时给该项写入 24dp 的底部外边距；并且它会响应 `COUIPreference_couiEnalbeClickSpan` 标记，对摘要调用 `COUIPreferenceUtils.setSummaryView`，使其中的 span 可点击。

正因为存在该标记，`summary` 的类型是 `AnnotatedString`：仓库的 `Text` 本身就会为它套用链接样式，因此不需要再传一个布尔值——不需要任何 span 响应时，传 `AnnotatedString(text)` 即可。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.PagerFooterPreference
```

## 基本用法

```kotlin
PagerFooterPreference(
    summary = AnnotatedString("本页仅用于组件演示。")
)
```

## 组件状态

### 带可点击 span

```kotlin
PagerFooterPreference(
    summary = buildAnnotatedString {
        append("更多信息请查看")
        withLink(
            LinkAnnotation.Clickable(tag = "legal") { openLegalPage() }
        ) {
            append("法律声明")
        }
        append("。")
    }
)
```

### 不附加底部外边距

```kotlin
PagerFooterPreference(
    summary = AnnotatedString("列表的最后一行"),
    withExtraMarginBottom = false
)
```

## 属性

### PagerFooterPreference 属性

| 属性名                 | 类型                            | 说明                                                       | 默认值                                    | 是否必须 |
| ---------------------- | ------------------------------- | ---------------------------------------------------------- | ----------------------------------------- | -------- |
| summary                | AnnotatedString                 | 说明文字，含其中的可点击 span                              | -                                         | 是       |
| modifier               | Modifier                        | 应用于组件的修饰符                                         | Modifier                                  | 否       |
| title                  | String?                         | 可选标题                                                   | null                                      | 否       |
| startAction            | @Composable (() -> Unit)?       | 左侧显示的自定义内容，对应布局的 `@android:id/icon`         | null                                      | 否       |
| endActions             | @Composable RowScope.() -> Unit | 右侧显示的自定义内容，对应布局的 `widget_frame`             | {}                                        | 否       |
| titleColor             | Color                           | 标题颜色（`coui_preference_title_color`，`?couiColorLabelPrimary`） | COUITheme.colorScheme.onBackground     | 否       |
| summaryColor           | Color                           | 摘要颜色（`coui_preference_secondary_text_color`，`?couiColorSecondNeutral`） | COUITheme.colorScheme.onBackgroundVariant | 否 |
| withExtraMarginBottom  | Boolean                         | 是否应用 24dp 底部外边距（COUI `withExtraMarginBottom`）    | true                                      | 否       |

### PagerFooterPreferenceDefaults

| 属性         | 类型          | 说明                                                       | 值   |
| ------------ | ------------- | ---------------------------------------------------------- | ---- |
| InsideMargin | PaddingValues | 与表头行相同的内边距：水平 32dp、垂直 8dp                   | 水平 32dp，垂直 8dp |
| MarginBottom | Dp            | `support_preference_footer_preference_margin_bottom`       | 24dp |
| TextSize     | TextUnit      | `coui_preference_header_footer_textsize`                   | 12sp |
