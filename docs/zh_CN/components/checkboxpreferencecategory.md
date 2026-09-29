# CheckBoxPreferenceCategory

`CheckBoxPreferenceCategory` 是右侧挂件为复选框的分组标题，对应 ColorOS 的 `COUICheckBoxPreferenceCategory`。设备端该类继承 `COUIPreferenceCategory`，并把 `coui_preference_category_widget_layout_checkbox.xml` 绑定到分组的 `widget_frame`：一个裸 `COUICheckBox`（id 为 `android.R.id.checkbox`），`paddingEnd` 为 16dp，`layout_marginVertical` 为 4dp。

标题本身只是普通文字：`COUIPreferenceCategory` 保持整行不可点击，只有复选框响应，因此这里没有整行的 `onClick`。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.CheckBoxPreferenceCategory
```

## 基本用法

最自然的用法是分组级「全选」：当组内只有部分项开启时，复选框显示 `Indeterminate`，这正是 `COUICheckBox` 三态设计的用途。

```kotlin
val allOn = itemA && itemB && itemC
val allOff = !itemA && !itemB && !itemC

CheckBoxPreferenceCategory(
    title = "通知",
    state = when {
        allOn -> ToggleableState.On
        allOff -> ToggleableState.Off
        else -> ToggleableState.Indeterminate
    },
    onClick = {
        val turnOn = !allOn
        itemA = turnOn
        itemB = turnOn
        itemC = turnOn
    }
)
```

## 组件状态

### 选中与未选中

```kotlin
CheckBoxPreferenceCategory(
    title = "全部开启",
    state = ToggleableState.On,
    onClick = {}
)
```

```kotlin
CheckBoxPreferenceCategory(
    title = "全部关闭",
    state = ToggleableState.Off,
    onClick = {}
)
```

### 禁用状态

```kotlin
CheckBoxPreferenceCategory(
    title = "禁用的分组",
    state = ToggleableState.On,
    onClick = {},
    enabled = false
)
```

## 属性

### CheckBoxPreferenceCategory 属性

| 属性名         | 类型                      | 说明                                       | 默认值                                    | 是否必须 |
| -------------- | ------------------------- | ------------------------------------------ | ----------------------------------------- | -------- |
| title          | String                    | 分组标题                                   | -                                         | 是       |
| state          | ToggleableState           | 复选框当前状态                             | -                                         | 是       |
| onClick        | (() -> Unit)?             | 点击复选框时的回调，由调用方更新 `state`   | -                                         | 是       |
| modifier       | Modifier                  | 应用于组件的修饰符                         | Modifier                                  | 否       |
| titleColor     | Color                     | 标题文本颜色（`?couiColorSecondNeutral`）  | COUITheme.colorScheme.onBackgroundVariant | 否       |
| checkboxColors | CheckboxColors            | 复选框控件的颜色配置                       | CheckboxDefaults.checkboxColors()         | 否       |
| enabled        | Boolean                   | 复选框是否可用                             | true                                      | 否       |

### CheckBoxPreferenceCategoryDefaults

| 属性                 | 类型 | 说明                                                                    | 值  |
| -------------------- | ---- | ----------------------------------------------------------------------- | --- |
| WidgetMarginVertical | Dp   | `coui_preference_category_widget_layout_margin_vertical`，复选框的垂直外边距 | 4dp |

## 说明

### 为什么状态是三态

`COUICheckBox` 是三态控件，设备端的 `OnStateChangeListener` 也会上报三种状态，因此 `state` 是 `ToggleableState` 而不是 `Boolean`。反映部分选中的分组级复选框需要 `Indeterminate`，用 `Boolean` 无法表达。

### 有意省略

- **`default_checkbox_state`。** 设备端从加载的属性里读取它来决定初始状态。在 Compose 中状态由调用方持有，该属性没有可映射的参数。
