# SpannablePreference

`SpannablePreference` 是摘要为 `AnnotatedString` 的偏好设置行，对应 ColorOS 的 `COUISpannablePreference`。

设备端该类本身没有新增任何字段或方法：构造函数只传入 `R.style.Preference_COUI_COUISpannablePreference`，即 `Preference.COUI` 加上 `<item name="couiEnalbeClickSpan">true</item>`。`COUIPreference.onBindViewHolder` 把该标记读入 `mIsEnableClickSpan`，置位时对摘要调用 `COUIPreferenceUtils.setSummaryView`，为摘要 `TextView` 挂上 `LinkMovementMethod`，其中的 span 便能响应点击。在 Compose 中，「摘要里的 span 能响应」的等价物就是 `AnnotatedString` 摘要，而仓库的 `Text` 已经会为其套用链接样式。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.SpannablePreference
```

## 基本用法

```kotlin
SpannablePreference(
    title = "法律信息",
    summary = buildAnnotatedString {
        append("《")
        withLink(
            LinkAnnotation.Clickable(tag = "privacy") { openPrivacyPolicy() }
        ) {
            append("隐私政策")
        }
        append("》")
    }
)
```

## 组件状态

### 纯文本摘要

不含链接的 `AnnotatedString` 渲染结果与普通摘要文本完全一致：

```kotlin
SpannablePreference(
    summary = AnnotatedString("本页仅用于组件演示。")
)
```

### 整行可点击

传入 `onClick` 后，除了摘要内的链接，整行也可响应点击：

```kotlin
SpannablePreference(
    title = "法律信息",
    summary = annotatedSummary,
    onClick = { openLegalPage() }
)
```

### 禁用状态

```kotlin
SpannablePreference(
    title = "禁用的设置项",
    summary = AnnotatedString("不可交互"),
    enabled = false
)
```

## 属性

### SpannablePreference 属性

| 属性名           | 类型                            | 说明                                       | 默认值                                | 是否必须 |
| ---------------- | ------------------------------- | ------------------------------------------ | ------------------------------------- | -------- |
| summary          | AnnotatedString                 | 摘要，含其中的可点击 span                  | -                                     | 是       |
| modifier         | Modifier                        | 应用于组件的修饰符                         | Modifier                              | 否       |
| title            | String?                         | 可选的标题                                 | null                                  | 否       |
| colors           | SpannablePreferenceColors       | 整行的颜色配置                             | SpannablePreferenceDefaults.colors()  | 否       |
| startAction      | @Composable (() -> Unit)?       | 左侧显示的自定义内容                       | null                                  | 否       |
| endActions       | @Composable (RowScope.() -> Unit)? | 右侧显示的自定义内容                    | null                                  | 否       |
| bottomAction     | @Composable (() -> Unit)?       | 底部显示的自定义内容                       | null                                  | 否       |
| insideMargin     | PaddingValues                   | 组件内部内容的边距                         | BasicComponentDefaults.InsideMargin   | 否       |
| cardListPosition | CardListPosition                | 该行在卡片组中的位置，圆角外边缘会获得额外内边距 | CardListPosition.None           | 否       |
| onClick          | (() -> Unit)?                   | 点击整行时的回调                           | null                                  | 否       |
| holdDownState    | Boolean                         | 组件是否处于按下状态                       | false                                 | 否       |
| enabled          | Boolean                         | 组件是否可交互                             | true                                  | 否       |

### SpannablePreferenceColors

| 属性                 | 类型  | 说明             |
| -------------------- | ----- | ---------------- |
| titleColor           | Color | 启用时的标题颜色 |
| disabledTitleColor   | Color | 禁用时的标题颜色 |
| summaryColor         | Color | 启用时的摘要颜色 |
| disabledSummaryColor | Color | 禁用时的摘要颜色 |

### SpannablePreferenceDefaults

| 属性              | 类型 | 说明                                                                    | 值   |
| ----------------- | ---- | ----------------------------------------------------------------------- | ---- |
| MarginBetweenLine | Dp   | 标题与摘要之间的间距，取自 `support_preference_margin_between_line`      | 2dp  |
| colors()          | SpannablePreferenceColors | 默认颜色：与 `BasicComponentDefaults` 为普通行解析的四种角色一致 | - |

## 说明

### 为什么需要单独的颜色类

该行自己渲染文本块，不走 `BasicComponent` 的 `title`/`summary` 参数，因此必须自己决定标题与摘要颜色。而仓库的 `BasicComponentColors.color(enabled)` 在 `coui-ui` 中是 `internal`，`coui-preference` 无法调用，所以 `SpannablePreferenceColors` 复述了同样的四种角色。
