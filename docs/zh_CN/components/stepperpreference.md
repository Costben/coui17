# StepperPreference

`StepperPreference` 是右侧带步进器的偏好设置行，对应 ColorOS 的 `COUIStepperPreference`。设备端把 `coui_preference_widget_stepper_view.xml`（一个裸 `COUIStepperView`）塞进标准 `coui_preference` 行的 `widget_frame`，因此整行仍保持常规的标题/摘要结构，步进器位于行尾。适用于字号、重复次数这类数值型设置。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.StepperPreference
```

## 基本用法

```kotlin
var level by remember { mutableIntStateOf(2) }

StepperPreference(
    value = level,
    onValueChange = { level = it },
    title = "字体大小",
    summary = listOf("小", "标准", "大", "超大")[level],
    minValue = 0,
    maxValue = 3
)
```

## 组件状态

### 到达边界

越界的按钮会被步进器禁用，因此该行不会产生 `minValue..maxValue` 之外的值：

```kotlin
StepperPreference(
    value = 0,
    onValueChange = {},
    title = "已到最小值",
    minValue = 0,
    maxValue = 10
)
```

### 禁用状态

```kotlin
StepperPreference(
    value = 3,
    onValueChange = {},
    title = "禁用的步进器",
    enabled = false
)
```

## 属性

### StepperPreference 属性

| 属性名           | 类型                          | 说明                                       | 默认值                                | 是否必须 |
| ---------------- | ----------------------------- | ------------------------------------------ | ------------------------------------- | -------- |
| value            | Int                           | 步进器当前值                               | -                                     | 是       |
| onValueChange    | (Int) -> Unit                 | 用户加/减后以新值触发的回调                | -                                     | 是       |
| title            | String                        | 设置项标题                                 | -                                     | 是       |
| modifier         | Modifier                      | 应用于组件的修饰符                         | Modifier                              | 否       |
| titleColor       | BasicComponentColors          | 标题文本的颜色配置                         | BasicComponentDefaults.titleColor()   | 否       |
| summary          | String?                       | 设置项摘要                                 | null                                  | 否       |
| summaryColor     | BasicComponentColors          | 摘要文本的颜色配置                         | BasicComponentDefaults.summaryColor() | 否       |
| minValue         | Int                           | 步进器可达最小值（COUI `couiMinimum`）     | StepperPreferenceDefaults.MinValue    | 否       |
| maxValue         | Int                           | 步进器可达最大值（COUI `couiMaximum`）     | StepperPreferenceDefaults.MaxValue    | 否       |
| step             | Int                           | 每次点击按钮的增量（COUI `couiUnit`）      | StepperPreferenceDefaults.Step        | 否       |
| startAction      | @Composable (() -> Unit)?     | 左侧显示的自定义内容                       | null                                  | 否       |
| endActions       | @Composable (RowScope.() -> Unit)? | 右侧显示的自定义内容（步进器之前）     | null                                  | 否       |
| stepperColors    | StepperColors                 | 步进器控件的颜色配置                       | StepperDefaults.stepperColors()       | 否       |
| bottomAction     | @Composable (() -> Unit)?     | 底部显示的自定义内容                       | null                                  | 否       |
| insideMargin     | PaddingValues                 | 组件内部内容的边距                         | BasicComponentDefaults.InsideMargin   | 否       |
| cardListPosition | CardListPosition              | 该行在卡片组中的位置，圆角外边缘会获得额外内边距 | CardListPosition.None           | 否       |
| holdDownState    | Boolean                       | 组件是否处于按下状态                       | false                                 | 否       |
| enabled          | Boolean                       | 组件是否可交互                             | true                                  | 否       |

### StepperPreferenceDefaults

| 属性     | 类型 | 说明                                                | 值   |
| -------- | ---- | --------------------------------------------------- | ---- |
| MinValue | Int  | `ObservableStep.MIN_VALUE`，即 `couiMinimum` 的默认值 | -999 |
| MaxValue | Int  | `ObservableStep.MAX_VALUE`，即 `couiMaximum` 的默认值 | 9999 |
| Step     | Int  | `couiUnit` 的默认值；`plus()` 加、`minus()` 减        | 1    |

## 进阶用法

### 带左侧图标

```kotlin
StepperPreference(
    value = repeatCount,
    onValueChange = { repeatCount = it },
    title = "重复次数",
    minValue = 1,
    maxValue = 99,
    startAction = {
        Icon(
            imageVector = COUIIcons.Refresh,
            contentDescription = null,
            tint = COUITheme.colorScheme.onSurfaceSecondary
        )
    }
)
```
