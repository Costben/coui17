# BottomPreference

`BottomPreference` 是 COUI 设置页结尾处的不可见底部占位，对应 ColorOS 的 `COUIBottomPreference`。它是唯一什么都不画的偏好设置项：设备端加载 `coui_preference_bottom.xml`——一个不可见的 `FrameLayout`，唯一内容是 `android:layout_height="@dimen/support_preference_foot_preference_padding_bottom"`——并由 `onBindViewHolder` 把 `mPlaceholderHeight` 写进该项的布局参数。`couiPlaceholderHeight` 属性可以覆盖该高度。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.BottomPreference
```

## 基本用法

放在设置列表的最后一项，避免最后一张卡片紧贴底边：

```kotlin
LazyColumn {
    // ... 卡片 ...
    item { BottomPreference() }
}
```

## 组件状态

### 自定义高度

```kotlin
BottomPreference(height = 48.dp)
```

## 属性

### BottomPreference 属性

| 属性名   | 类型     | 说明                              | 默认值                          | 是否必须 |
| -------- | -------- | --------------------------------- | ------------------------------- | -------- |
| modifier | Modifier | 应用于组件的修饰符                | Modifier                        | 否       |
| height   | Dp       | 占位高度（COUI `couiPlaceholderHeight`） | BottomPreferenceDefaults.Height | 否 |

### BottomPreferenceDefaults

| 属性   | 类型 | 说明                                                                               | 值   |
| ------ | ---- | ---------------------------------------------------------------------------------- | ---- |
| Height | Dp   | `support_preference_foot_preference_padding_bottom`，别名 `coui_list_to_ex_bottom_padding` | 32dp |
