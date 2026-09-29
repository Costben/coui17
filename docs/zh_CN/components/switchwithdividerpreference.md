# SwitchWithDividerPreference

`SwitchWithDividerPreference` 是用竖直细线与文字块分隔开关的 `SwitchPreference`，对应 ColorOS 的 `COUISwitchWithDividerPreference`。设备端子类把整行换成 `coui_switch_with_divider_preference.xml`：文字块与行尾开关都和普通开关行相同，只是开关之前加了一条 1dp × 22dp 的 `?couiColorDivider` 细线。细线两侧的留白合计正好等于设备端的 16dp（挂件侧是 `android_preference_switch_margin_left`，再加上 `BasicComponent` 本身预留的 8dp 间隔）。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.SwitchWithDividerPreference
```

## 基本用法

适用于开关在文字旁排成一列的场景，例如一组通知开关：

```kotlin
SwitchWithDividerPreference(
    title = "允许通知",
    checked = notifyEnabled,
    onCheckedChange = { notifyEnabled = it }
)
```

## 组件状态

### 关闭

```kotlin
SwitchWithDividerPreference(
    title = "关闭的行",
    checked = false,
    onCheckedChange = {}
)
```

### 禁用状态

```kotlin
SwitchWithDividerPreference(
    title = "禁用的行",
    checked = true,
    onCheckedChange = {},
    enabled = false
)
```

## 属性

### SwitchWithDividerPreference 属性

| 属性名           | 类型                            | 说明                                        | 默认值                                | 是否必须 |
| ---------------- | ------------------------------- | ------------------------------------------- | ------------------------------------- | -------- |
| checked          | Boolean                         | 开关的选中状态                              | -                                     | 是       |
| onCheckedChange  | (Boolean) -> Unit               | 开关状态变化时的回调                        | -                                     | 是       |
| title            | String                          | 设置项标题                                  | -                                     | 是       |
| modifier         | Modifier                        | 应用于组件的修饰符                          | Modifier                              | 否       |
| titleColor       | BasicComponentColors            | 标题文本的颜色配置                          | BasicComponentDefaults.titleColor()   | 否       |
| summary          | String?                         | 设置项摘要                                  | null                                  | 否       |
| summaryColor     | BasicComponentColors            | 摘要文本的颜色配置                          | BasicComponentDefaults.summaryColor() | 否       |
| startAction      | @Composable (() -> Unit)?       | 左侧显示的自定义内容                        | null                                  | 否       |
| endActions       | @Composable (RowScope.() -> Unit)? | 右侧显示的自定义内容（细线与开关之间）   | null                                  | 否       |
| bottomAction     | @Composable (() -> Unit)?       | 底部显示的自定义内容                        | null                                  | 否       |
| switchColors     | SwitchColors                    | 开关控件的颜色配置                          | SwitchDefaults.switchColors()         | 否       |
| insideMargin     | PaddingValues                   | 组件内部内容的边距                          | BasicComponentDefaults.InsideMargin   | 否       |
| cardListPosition | CardListPosition                | 该行在卡片组中的位置，圆角外边缘会获得额外内边距 | CardListPosition.None             | 否       |
| holdDownState    | Boolean                         | 组件是否处于按下状态                        | false                                 | 否       |
| enabled          | Boolean                         | 组件是否可交互                              | true                                  | 否       |
