# AppInfoPreference

`AppInfoPreference` 是 COUI 关于页顶部的应用信息头，对应 `aboutpage` 包下的 `COUIAppInfoPreference`。设备端该类继承 `COUIPreference`，布局 `coui_component_preference_application_info.xml` 是一个居中的 `ConstraintLayout`：60dp 的 `COUIRoundImageView`（`couiBorderRadius` 16dp、`couiType="round"`），其上方留 42dp 外边距；接着是 20sp 的应用名（`?couiColorPrimaryNeutral`，位于图标下方 8dp）和 12sp 的版本号（`?couiColorSecondNeutral`，位于应用名下方 4dp）。根节点带 8dp 底部外边距。

`couiType="round"` 会让图标走 `COUIRoundRectUtil.getPath`，画出的是 `COUIShapePath` 的平滑圆角而非圆弧，因此本移植用 `SquircleShape`（仓库对同一条路径的移植）裁剪，并像其他所有 COUI 表面一样把 `isSquircleEnabled` 透传进去。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.AppInfoPreference
```

## 基本用法

```kotlin
AppInfoPreference(
    appName = "COUI 17",
    appVersion = "V17.0.0",
    icon = rememberAppIcon()
)
```

## 组件状态

### 不带图标

图标是可选的；不传时整个信息块从应用名开始，42dp 的顶部外边距也随之省略：

```kotlin
AppInfoPreference(
    appName = "COUI 17",
    appVersion = "V17.0.0"
)
```

### 长按版本号

设备端长按版本号会弹出一个 `COUIPopupWindow`，其中是复制操作：点击后把版本号写入剪贴板并弹 Toast 提示。该弹窗属于应用文案而非偏好设置几何，因此本移植只到回调为止：

```kotlin
val clipboard = LocalClipboardManager.current

AppInfoPreference(
    appName = "COUI 17",
    appVersion = "V17.0.0",
    icon = appIcon,
    onVersionLongClick = { clipboard.setText(AnnotatedString("V17.0.0")) }
)
```

## 属性

### AppInfoPreference 属性

| 属性名             | 类型              | 说明                                                | 默认值                                    | 是否必须 |
| ------------------ | ----------------- | --------------------------------------------------- | ----------------------------------------- | -------- |
| appName            | String            | 应用名（COUI `appName`）                             | -                                         | 是       |
| appVersion         | String            | 版本号（COUI `appVersion`）                          | -                                         | 是       |
| modifier           | Modifier          | 应用于组件的修饰符                                  | Modifier                                  | 否       |
| icon               | Painter?          | 应用图标（COUI `appIcon`），不着色绘制               | null                                      | 否       |
| onVersionLongClick | (() -> Unit)?     | 长按版本号时的回调；为 null 时该行无反应             | null                                      | 否       |
| appNameColor       | Color             | 应用名颜色（`?couiColorPrimaryNeutral`）             | COUITheme.colorScheme.onBackground        | 否       |
| appVersionColor    | Color             | 版本号颜色（`?couiColorSecondNeutral`）              | COUITheme.colorScheme.onBackgroundVariant | 否       |

### AppInfoPreferenceDefaults

| 属性               | 类型     | 说明                                                            | 值   |
| ------------------ | -------- | --------------------------------------------------------------- | ---- |
| IconSize           | Dp       | `about_app_icon` 的 `layout_width` / `layout_height`            | 60dp |
| IconCornerRadius   | Dp       | `about_app_icon` 的 `couiBorderRadius`                          | 16dp |
| IconMarginTop      | Dp       | `about_app_icon` 的 `layout_marginTop`                          | 42dp |
| NameMarginTop      | Dp       | `about_app_name` 的 `layout_marginTop`                          | 8dp  |
| VersionMarginTop   | Dp       | `about_app_version` 的 `layout_marginTop`                       | 4dp  |
| MarginBottom       | Dp       | 根节点的 `layout_marginBottom`                                   | 8dp  |
| AppNameTextSize    | TextUnit | `about_app_name` 显式指定的 `android:textSize`，覆盖其 16sp 标题样式 | 20sp |
| AppVersionTextSize | TextUnit | `about_app_version` 显式指定的 `android:textSize`                | 12sp |

## 说明

### 有意省略

- **复制弹窗。** 设备端会用 `coui_component_popup_window_layout.xml` 构建 `COUIPopupWindow`，定位到版本号上方，点击后把 `appVersion` 写入剪贴板并弹出 `copyFinishText` 的 Toast。这些都不是偏好设置几何或主题信息，而是调用方本来就该拥有的应用行为，因此本移植暴露 `onVersionLongClick`，而不是写死剪贴板操作。
- **`copyText` / `copyFinishText` 参数。** 它们只为填充该弹窗而存在，随弹窗一并省略。
