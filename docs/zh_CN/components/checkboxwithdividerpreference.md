# CheckBoxWithDividerPreference

`CheckBoxWithDividerPreference` 是用竖直细线与文字块分隔复选框的 `CheckboxPreference`，对应 ColorOS 的 `COUICheckBoxWithDividerPreference`。设备端子类把整行换成 `coui_check_box_divider_preference.xml`：文字块与复选框都和普通复选行相同，只是复选框被移到行尾，并在其之前加了一条 1dp × 22dp 的 `?couiColorDivider` 细线。细线两侧的留白合计正好等于设备端的 16dp（挂件侧是 `preference_divider_margin_end`，再加上 `BasicComponent` 本身预留的 8dp 间隔）。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.CheckBoxWithDividerPreference
```

## 基本用法

适用于复选框在文字旁排成一列的场景，例如一组互相独立的通知开关：

```kotlin
CheckBoxWithDividerPreference(
    title = "锁屏显示通知",
    checked = lockScreenNotify,
    onCheckedChange = { lockScreenNotify = it }
)
```

## 组件状态

### 未选中

```kotlin
CheckBoxWithDividerPreference(
    title = "未选中的行",
    checked = false,
    onCheckedChange = {}
)
```

### 禁用状态

```kotlin
CheckBoxWithDividerPreference(
    title = "禁用的行",
    checked = true,
    onCheckedChange = {},
    enabled = false
)
```

## 属性

### CheckBoxWithDividerPreference 属性

| 属性名           | 类型                            | 说明                                        | 默认值                                | 是否必须 |
| ---------------- | ------------------------------- | ------------------------------------------- | ------------------------------------- | -------- |
| title            | String                          | 设置项标题                                  | -                                     | 是       |
| checked          | Boolean                         | 复选框的选中状态                            | -                                     | 是       |
| onCheckedChange  | ((Boolean) -> Unit)?            | 选中状态变化时的回调                        | -                                     | 是       |
| modifier         | Modifier                        | 应用于组件的修饰符                          | Modifier                              | 否       |
| titleColor       | BasicComponentColors            | 标题文本的颜色配置                          | BasicComponentDefaults.titleColor()   | 否       |
| summary          | String?                         | 设置项摘要                                  | null                                  | 否       |
| summaryColor     | BasicComponentColors            | 摘要文本的颜色配置                          | BasicComponentDefaults.summaryColor() | 否       |
| checkboxColors   | CheckboxColors                  | 复选框控件的颜色配置                        | CheckboxDefaults.checkboxColors()     | 否       |
| startAction      | @Composable (() -> Unit)?       | 左侧显示的自定义内容                        | null                                  | 否       |
| endActions       | @Composable (RowScope.() -> Unit)? | 右侧显示的自定义内容（细线与复选框之间） | null                                  | 否       |
| bottomAction     | @Composable (() -> Unit)?       | 底部显示的自定义内容                        | null                                  | 否       |
| insideMargin     | PaddingValues                   | 组件内部内容的边距                          | BasicComponentDefaults.InsideMargin   | 否       |
| cardListPosition | CardListPosition                | 该行在卡片组中的位置，圆角外边缘会获得额外内边距 | CardListPosition.None             | 否       |
| holdDownState    | Boolean                         | 组件是否处于按下状态                        | false                                 | 否       |
| enabled          | Boolean                         | 组件是否可交互                              | true                                  | 否       |
