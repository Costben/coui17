# EditTextPreference

`EditTextPreference` 是把当前值显示在行尾、并在底部面板中编辑的偏好设置行，对应 ColorOS 的 `COUIEditTextPreference`。设备端整行使用标准 `coui_preference` 布局并靠 `app:couiAssignment` 显示取值，编辑界面是 `COUIEditTextPreferenceDialogFragment`（基于 `coui_preference_dialog_edittext.xml`）。本移植为了与其他选择类偏好保持一致，改用 `OverlayBottomSheet` 承载编辑面板（与 `ListPreference` 相同），因此页面同样需要包裹在 `Scaffold` 中。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.EditTextPreference
```

## 基本用法

面板内是一行 `TextField` 加一行取消/确认 `TextButton`。取值由调用方持有：编辑内容先暂存在面板里，只有按下确认时才会通过 `onValueChange` 提交。

```kotlin
var deviceName by remember { mutableStateOf("OnePlus PLK110") }

EditTextPreference(
    title = "设备名称",
    value = deviceName,
    onValueChange = { deviceName = it },
    confirmButtonText = "确定",
    cancelButtonText = "取消",
    dialogTitle = "设备名称",
    hint = "最多 24 个字符"
)
```

## 组件状态

### 空输入

默认情况下，输入框为空时确认按钮保持禁用。设置 `supportEmptyInput` 可以允许清空取值：

```kotlin
EditTextPreference(
    title = "昵称",
    value = nickname,
    onValueChange = { nickname = it },
    confirmButtonText = "确定",
    cancelButtonText = "取消",
    supportEmptyInput = true
)
```

### 禁用状态

```kotlin
EditTextPreference(
    title = "禁用的输入项",
    value = "不可编辑",
    onValueChange = {},
    confirmButtonText = "确定",
    cancelButtonText = "取消",
    enabled = false
)
```

## 属性

### EditTextPreference 属性

| 属性名            | 类型                            | 说明                                        | 默认值                                | 是否必须 |
| ----------------- | ------------------------------- | ------------------------------------------- | ------------------------------------- | -------- |
| title             | String                          | 设置项标题                                  | -                                     | 是       |
| value             | String                          | 当前取值，显示在行尾                        | -                                     | 是       |
| onValueChange     | (String) -> Unit                | 面板确认时以编辑后的文本触发的回调          | -                                     | 是       |
| confirmButtonText | String                          | 面板中确认按钮的文案                        | -                                     | 是       |
| cancelButtonText  | String                          | 面板中取消按钮的文案                        | -                                     | 是       |
| modifier          | Modifier                        | 应用于组件的修饰符                          | Modifier                              | 否       |
| titleColor        | BasicComponentColors            | 标题文本的颜色配置                          | BasicComponentDefaults.titleColor()   | 否       |
| summary           | String?                         | 设置项摘要                                  | null                                  | 否       |
| summaryColor      | BasicComponentColors            | 摘要文本的颜色配置                          | BasicComponentDefaults.summaryColor() | 否       |
| dialogTitle       | String                          | 编辑面板的标题                              | title                                 | 否       |
| hint              | String                          | 输入框为空时显示的占位文本                  | ""                                    | 否       |
| supportEmptyInput | Boolean                         | 是否允许确认空内容                          | false                                 | 否       |
| startAction       | @Composable (() -> Unit)?       | 左侧显示的自定义内容                        | null                                  | 否       |
| bottomAction      | @Composable (() -> Unit)?       | 底部显示的自定义内容                        | null                                  | 否       |
| insideMargin      | PaddingValues                   | 组件内部内容的边距                          | BasicComponentDefaults.InsideMargin   | 否       |
| cardListPosition  | CardListPosition                | 该行在卡片组中的位置，圆角外边缘会获得额外内边距 | CardListPosition.None             | 否       |
| enabled           | Boolean                         | 组件是否可交互                              | true                                  | 否       |
| showValue         | Boolean                         | 是否在行尾显示取值                          | true                                  | 否       |
| renderInRootScaffold | Boolean                      | 面板是否渲染在根 Scaffold 中                | true                                  | 否       |
| onExpandedChange  | ((Boolean) -> Unit)?            | 面板打开或关闭时的回调                      | null                                  | 否       |

### EditTextPreferenceDefaults

| 属性                   | 类型 | 说明                                                                        | 值   |
| ---------------------- | ---- | --------------------------------------------------------------------------- | ---- |
| SheetHorizontalPadding | Dp   | 编辑输入框的水平内边距，取自 `support_preference_dialog_edittext_margin_horizontal` | 24dp |
| ButtonBarTopPadding    | Dp   | 输入框与按钮行之间的间距，取自 `coui_edittextdialog_marginbottom`            | 14dp |
| ButtonBarBottomPadding | Dp   | 按钮行下方的内边距                                                          | 12dp |
