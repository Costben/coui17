# InputPreference

`InputPreference` 是就地编辑的卡片输入行，对应 ColorOS 的 `COUIInputPreference`。设备端整行是 `coui_input_preference.xml`：一个空的 `edittext_container`，由 `COUIInputPreference.onBindViewHolder` 填入内部的 `COUICardListItemInputView`（一个 `COUIInputView`，`mTitle` 即偏好设置标题，内部的 `COUIScrolledEditText` 运行在 line 模式并开启 `couiJustShowFocusLine`）。没有面板——输入框就是行的一部分，这也是它与 `EditTextPreference` 的区别。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.InputPreference
```

## 基本用法

```kotlin
var name by remember { mutableStateOf("COUI-Lab 17") }

InputPreference(
    title = "蓝牙名称",
    value = name,
    onValueChange = { name = it }
)
```

## 组件状态

### 字数限制与清除按钮

```kotlin
InputPreference(
    title = "蓝牙名称",
    value = name,
    onValueChange = { name = it },
    maxCount = 24,
    showClearButton = true
)
```

### 只读与禁用

`readOnly` 保留选中但拒绝编辑；`enabled = false` 还会让整行变灰，对应设备端 `COUIInputPreference` 对输入框调用 `setEnabled(isEnabled())`：

```kotlin
InputPreference(
    title = "只读输入项",
    value = "不可编辑",
    onValueChange = {},
    readOnly = true
)
```

```kotlin
InputPreference(
    title = "禁用的输入项",
    value = "不可编辑",
    onValueChange = {},
    enabled = false
)
```

## 属性

### InputPreference 属性

| 属性名           | 类型                            | 说明                                       | 默认值                                | 是否必须 |
| ---------------- | ------------------------------- | ------------------------------------------ | ------------------------------------- | -------- |
| title            | String                          | 设置项标题，即输入框的浮动标签             | -                                     | 是       |
| value            | String                          | 当前文本取值                               | -                                     | 是       |
| onValueChange    | (String) -> Unit                | 每次编辑时触发的回调                       | -                                     | 是       |
| modifier         | Modifier                        | 应用于组件的修饰符                         | Modifier                              | 否       |
| startAction      | @Composable (() -> Unit)?       | 左侧显示的自定义内容                       | null                                  | 否       |
| endActions       | @Composable (RowScope.() -> Unit)? | 右侧显示的自定义内容                    | null                                  | 否       |
| bottomAction     | @Composable (() -> Unit)?       | 底部显示的自定义内容                       | null                                  | 否       |
| insideMargin     | PaddingValues                   | 组件内部内容的边距                         | BasicComponentDefaults.InsideMargin   | 否       |
| cardListPosition | CardListPosition                | 该行在卡片组中的位置，圆角外边缘会获得额外内边距 | CardListPosition.None           | 否       |
| enabled          | Boolean                         | 输入框是否可编辑                           | true                                  | 否       |
| readOnly         | Boolean                         | 输入框是否拒绝编辑                         | false                                 | 否       |
| maxCount         | Int?                            | 限制的最大字数，不限制时为 null            | null                                  | 否       |
| showClearButton  | Boolean                         | 输入框获得焦点且非空时是否显示清除按钮     | false                                 | 否       |

## 进阶用法

### 与行标题对齐

设备端输入框的浮动标签就是偏好设置标题，因此标签落在整行的文字列上，而不是另起一个文本块。这也是 `InputPreference` 自己渲染标题、不使用 `BasicComponent` 的原因：

```kotlin
InputPreference(
    title = "设备名称",
    value = deviceName,
    onValueChange = { deviceName = it },
    modifier = Modifier.heightIn(min = 52.dp)
)
```
