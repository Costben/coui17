# MarkWithDividerPreference

`MarkWithDividerPreference` 是用竖直细线与文字块分隔标记的 `MarkPreference`，对应 ColorOS 的 `COUIMarkWithDividerPreference`。设备端子类把整行换成 `coui_mark_with_divider_preference.xml`：文字块与行尾标记都和普通标记行相同，只是标记之前多了一条 1dp × 22dp 的 `?couiColorDivider` 细线。细线两侧的留白合计正好等于设备端的 16dp（挂件侧是 `preference_divider_margin_end`，再加上 `BasicComponent` 本身预留的 8dp 间隔）。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.MarkWithDividerPreference
```

## 基本用法

适用于互斥设置项成组出现、且希望标记看起来像独立一列的场景，例如「显示通知内容」/「隐藏通知内容」这对选项：

```kotlin
MarkWithDividerPreference(
    title = "显示通知内容",
    checked = notifyDetail,
    onClick = { notifyDetail = true }
)
MarkWithDividerPreference(
    title = "隐藏通知内容",
    checked = !notifyDetail,
    onClick = { notifyDetail = false }
)
```

## 组件状态

### 未选中

```kotlin
MarkWithDividerPreference(
    title = "选项 B",
    checked = false,
    onClick = {}
)
```

### 禁用状态

```kotlin
MarkWithDividerPreference(
    title = "禁用的选项",
    checked = true,
    onClick = {},
    enabled = false
)
```

## 属性

### MarkWithDividerPreference 属性

| 属性名           | 类型                            | 说明                                        | 默认值                                | 是否必须 |
| ---------------- | ------------------------------- | ------------------------------------------- | ------------------------------------- | -------- |
| title            | String                          | 设置项标题                                  | -                                     | 是       |
| checked          | Boolean                         | 该行当前是否带标记                          | -                                     | 是       |
| onClick          | (() -> Unit)?                   | 点击整行时的回调                            | -                                     | 是       |
| modifier         | Modifier                        | 应用于组件的修饰符                          | Modifier                              | 否       |
| titleColor       | BasicComponentColors            | 标题文本的颜色配置                          | BasicComponentDefaults.titleColor()   | 否       |
| summary          | String?                         | 设置项摘要                                  | null                                  | 否       |
| summaryColor     | BasicComponentColors            | 摘要文本的颜色配置                          | BasicComponentDefaults.summaryColor() | 否       |
| markColors       | RadioButtonColors               | 标记控件的颜色配置                          | RadioButtonDefaults.radioButtonColors() | 否     |
| startAction      | @Composable (() -> Unit)?       | 左侧显示的自定义内容                        | null                                  | 否       |
| endActions       | @Composable (RowScope.() -> Unit)? | 右侧显示的自定义内容（细线与标记之间）   | null                                  | 否       |
| bottomAction     | @Composable (() -> Unit)?       | 底部显示的自定义内容                        | null                                  | 否       |
| insideMargin     | PaddingValues                   | 组件内部内容的边距                          | BasicComponentDefaults.InsideMargin   | 否       |
| cardListPosition | CardListPosition                | 该行在卡片组中的位置，圆角外边缘会获得额外内边距 | CardListPosition.None             | 否       |
| holdDownState    | Boolean                         | 组件是否处于按下状态                        | false                                 | 否       |
| enabled          | Boolean                         | 组件是否可交互                              | true                                  | 否       |

## 说明

### 有意省略

- **拆分点击区域。** `COUIMarkWithDividerPreference` 把整行拆成两个命中区——`main_layout` 触发 `OnMainLayoutClickListener`，`radio_layout` 切换标记。仓库的 `MarkPreference` 与本封装都没有建模这一拆分，整行只保留一个点击目标。
