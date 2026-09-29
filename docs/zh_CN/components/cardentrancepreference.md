# CardEntrancePreference

`CardEntrancePreference` 是一张可点击的卡片,用于跳转到其它页面,对应 ColorOS 17 的 `com.coui.appcompat.card.COUICardEntrancePreference`(本身是 `COUIPressFeedbackJumpPreference`)。它有两套设备布局,由 `cardType` 切换:

- **Small** —— 竖向排布:圆角图标在上,标题在其下 12dp 处,摘要再下 4dp,底色圆角 12dp。
- **Large** —— 横向排布:圆角图标在起始端,标题与摘要在其右侧 16dp(摘要位于标题下 3dp),底色圆角 16dp。

卡片的按压反馈复用仓库 `Card` 的按压着色,即 COUI 卡片的按压行为。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.CardEntrancePreference
import io.github.suqi8.coui.kmp.preference.CardEntrancePreferenceDefaults
import io.github.suqi8.coui.kmp.preference.CardEntranceType
import io.github.suqi8.coui.kmp.preference.CardEntranceTintIcon
```

## 基本用法

```kotlin
CardEntrancePreference(
    title = "云端相册",
    cardType = CardEntranceType.Large,
    icon = rememberVectorPainter(COUIIcons.Basic.Cloud),
    summary = "在每台设备上浏览照片",
    onClick = { /* 打开相册 */ },
)
```

## 属性

### CardEntrancePreference 属性

| 属性名   | 类型                  | 说明                                                          | 默认值                                               | 是否必须 |
| -------- | --------------------- | ------------------------------------------------------------- | ---------------------------------------------------- | -------- |
| title    | String                | 卡片标题(`?couiColorPrimaryNeutral`)                          | -                                                    | 是       |
| modifier | Modifier              | 应用于卡片的修饰符                                            | Modifier                                             | 否       |
| cardType | CardEntranceType      | 使用哪套设备布局,见 [CardEntranceType](#cardentrancetype)    | CardEntranceType.Small                               | 否       |
| icon     | Painter?              | 卡片图标,为 null 时不显示                                     | null                                                 | 否       |
| summary  | String?               | 卡片摘要(`?couiColorSecondNeutral`),为 null 时不显示该行     | null                                                 | 否       |
| statusOn | Boolean               | 摘要改用 `?couiColorPrimaryText` 着色                         | false                                                | 否       |
| tintIcon | CardEntranceTintIcon  | 图标着色方式,见 [CardEntranceTintIcon](#cardentrancetinticon) | CardEntranceTintIcon.None                           | 否       |
| onClick  | (() -> Unit)?         | 卡片点击回调,为 null 时卡片不可点击                           | null                                                 | 否       |
| colors   | CardEntranceColors    | 卡片配色                                                      | CardEntrancePreferenceDefaults.cardEntranceColors()  | 否       |

### CardEntranceType

| 取值  | 设备常量           | 布局                                                                    | 圆角 |
| ----- | ------------------ | ----------------------------------------------------------------------- | ---- |
| Small | `CARD_TYPE_SMALL` (1) | `coui_component_card_entrance_preference_type_small.xml`(图标在上)   | 12dp |
| Large | `CARD_TYPE_LARGE` (2) | `coui_component_card_entrance_preference_type_large.xml`(图标在左)   | 16dp |

### CardEntranceTintIcon

| 取值          | 设备常量                   | 含义                         |
| ------------- | -------------------------- | ---------------------------- |
| None          | `TINT_ICON_NONE` (0)       | 图标不着色                   |
| ByGlobalTheme | `TINT_ICON_BY_GLOBAL_THEME` (1) | 按全局 COUI 主题着色    |
| Always        | `TINT_ICON_ANYWAY` (2)     | 不理会全局主题标记,始终着色 |

设备把两个非 `None` 模式都交给 `COUIThemeOverlay.applyCOUITintIcon`;本移植只有一套主题着色,因此 `ByGlobalTheme` 与 `Always` 解析为同一颜色。

### CardEntrancePreferenceDefaults 对象

| 常量                  | 类型          | 值                                             | 设备来源                                                   |
| --------------------- | ------------- | ---------------------------------------------- | ---------------------------------------------------------- |
| SmallCornerRadius     | Dp            | 12.dp                                          | `coui_component_card_entrance_small_bg` 的 `<corners android:radius>` |
| LargeCornerRadius     | Dp            | 16.dp                                          | `coui_component_card_entrance_large_bg` 的 `<corners android:radius>` |
| SmallContentPadding   | PaddingValues | 四周 16.dp                                     | small 布局根 `android:padding`                              |
| SmallTitleMarginTop   | Dp            | 12.dp                                          | small 布局标题 `android:layout_marginTop`                   |
| SmallSummaryMarginTop | Dp            | 4.dp                                           | small 布局摘要 `android:layout_marginTop`                   |
| LargeContentPadding   | PaddingValues | start 16.dp, end 12.dp, top 20.dp, bottom 20.dp | large 布局根内边距                                         |
| LargeIconMarginEnd    | Dp            | 16.dp                                          | large 布局文本块 `android:layout_marginStart`               |
| LargeSummaryMarginTop | Dp            | 3.dp                                           | large 布局摘要 `android:layout_marginTop`                   |
| IconSize              | Dp            | 32.dp                                          | 非设备 dimen,见下方说明                                     |
| SummaryTextSize       | TextUnit      | 12.sp                                          | `couiTextAppearanceDescription`                             |

`IconSize` 是移植时的选择:设备把图标设为 `wrap_content`,由 `COUIRoundImageView` 按给定 drawable 定尺,因此没有对应的 `coui_*` dimen。

### `cardEntranceColors()` 工厂

| 参数               | 类型  | 默认值                                      | COUI 角色                   |
| ------------------ | ----- | ------------------------------------------- | --------------------------- |
| containerColor     | Color | COUITheme.colorScheme.surfaceContainer      | `?couiColorCardBackground`  |
| titleColor         | Color | COUITheme.colorScheme.onBackground          | `?couiColorPrimaryNeutral`  |
| summaryColor       | Color | COUITheme.colorScheme.onSurfaceSecondary    | `?couiColorSecondNeutral`   |
| activeSummaryColor | Color | COUITheme.colorScheme.onBackground          | `?couiColorPrimaryText`     |
| iconTint           | Color | COUITheme.colorScheme.onBackground          | 主题图标着色                |
| pressedColor       | Color | COUITheme.colorScheme.surfaceContainerHigh  | `couiColorCardPressed`      |

### CardEntranceColors 类

| 属性名             | 类型  | 说明                              |
| ------------------ | ----- | --------------------------------- |
| containerColor     | Color | 卡片背景                          |
| titleColor         | Color | 标题颜色                          |
| summaryColor       | Color | 摘要颜色                          |
| activeSummaryColor | Color | 状态为开时的摘要颜色              |
| iconTint           | Color | `ByGlobalTheme` / `Always` 的图标色 |
| pressedColor       | Color | 按压时卡片渐变到的填充色          |

## 行为

- **Small** 布局把图标、标题(下 12dp)与摘要(标题下 4dp)依次放进四周 16dp 内边距的 `Column`。
- **Large** 布局把图标放在起始端、标题/摘要块放在其右侧 16dp,垂直居中;文本块占据剩余宽度。
- 标题使用 COUI `headline2` 文字样式,最多两行;摘要使用 `footnote1` 的 12sp,同样最多两行。
- `statusOn = true` 时摘要改用 `?couiColorPrimaryText` 着色,对应 `COUICardEntrancePreference#setStatusOn`。

## 未移植

设备的 `COUIPressFeedbackHelper`(按压反馈触摸基础设施)与 `showSummary = false` 时的独立状态行属于 Android View 基础设施,未建模 —— 按压反馈由仓库 `Card` 的按压着色承担,摘要则只是 [summary](#cardentrancepreference-属性) 参数。

## 进阶用法

### Small 卡片

```kotlin
CardEntrancePreference(
    title = "最近",
    cardType = CardEntranceType.Small,
    icon = rememberVectorPainter(COUIIcons.Basic.Recent),
    onClick = { /* 打开 */ },
)
```

### 着色图标 + 激活态摘要

```kotlin
CardEntrancePreference(
    title = "备份",
    cardType = CardEntranceType.Large,
    icon = rememberVectorPainter(COUIIcons.Basic.Cloud),
    summary = "已是最新",
    statusOn = true,
    tintIcon = CardEntranceTintIcon.ByGlobalTheme,
    onClick = { /* 打开 */ },
)
```
