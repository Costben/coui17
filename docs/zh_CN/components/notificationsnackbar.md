# NotificationSnackBar

通知样式的 Snackbar,对应 ColorOS 的 `com.coui.appcompat.snackbar.COUINotificationSnackBar` 及其 `res/layout/coui_notification_snack_bar_item.xml`。它带有前置通知图(图片框形态由原生 `ImageType` 决定)、最多两行的标题、可选的两行副文本、可选的操作按钮,以及固定在右上角的关闭图标。宿主([NotificationSnackBarHost] 与 [NotificationSnackBarHostState])同一时刻只显示一个,并把显示时长限制在 4–10 秒,与原生 `make()` 一致。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.NotificationSnackBar
import io.github.suqi8.coui.kmp.basic.NotificationSnackBarColors
import io.github.suqi8.coui.kmp.basic.NotificationSnackBarDefaults
import io.github.suqi8.coui.kmp.basic.NotificationSnackBarHost
import io.github.suqi8.coui.kmp.basic.NotificationSnackBarHostState
import io.github.suqi8.coui.kmp.basic.NotificationSnackBarImageType
```

## 基本用法

```kotlin
val hostState = remember { NotificationSnackBarHostState() }

// 放在布局底部:
NotificationSnackBarHost(state = hostState)

// 显示一个:
scope.launch {
    hostState.showNotification(
        title = "正在充电",
        subtitle = "电量 80%",
        icon = COUIIcons.WorldClock,
        actionText = "详情",
    )
}
```

## 图片形态

`COUINotificationSnackBar.ImageType` 有三种形态,尺寸均取自 `coui_notification_snack_bar_*`:

```kotlin
NotificationSnackBar(
    title = "新消息",
    subtitle = "方形图片",
    icon = COUIIcons.Email,
    imageType = NotificationSnackBarImageType.Square, // 44dp x 44dp
)

NotificationSnackBar(
    title = "正在播放",
    subtitle = "竖向图片",
    icon = COUIIcons.Photos,
    imageType = NotificationSnackBarImageType.Vertical, // 36dp x 48dp
)

NotificationSnackBar(
    title = "闹钟已设置",
    subtitle = "横向图片",
    icon = COUIIcons.WorldClock,
    imageType = NotificationSnackBarImageType.Horizontal, // 58dp x 44dp
)
```

## 单独使用

`NotificationSnackBar` 是宿主渲染的叶子组件,应用自己掌控时序时可以直接摆放:

```kotlin
NotificationSnackBar(
    title = "同步失败",
    subtitle = "请检查网络",
    actionText = "重试",
    onAction = { retry() },
    onClose = { hide() },
)
```

## 属性

### NotificationSnackBar

| 属性         | 类型                          | 说明                     | 默认值                            | 必填 |
| ------------ | ----------------------------- | ------------------------ | --------------------------------- | ---- |
| title        | String                        | 标题,最多两行            | -                                 | 是   |
| modifier     | Modifier                      | 应用于 Snackbar 的修饰符 | Modifier                          | 否   |
| subtitle     | String?                       | 可选副文本               | null                              | 否   |
| icon         | ImageVector?                  | 可选前置通知图           | null                              | 否   |
| imageType    | NotificationSnackBarImageType | `icon` 的图片框形态      | NotificationSnackBarImageType.Square | 否 |
| actionText   | String?                       | 可选操作按钮文案         | null                              | 否   |
| onAction     | (() -> Unit)?                 | 点击操作按钮的回调       | null                              | 否   |
| onClose      | (() -> Unit)?                 | 点击关闭图标的回调       | null                              | 否   |
| cornerRadius | Dp                            | 容器圆角半径             | NotificationSnackBarDefaults.CornerRadius | 否 |
| colors       | NotificationSnackBarColors    | Snackbar 配色            | NotificationSnackBarDefaults.colors() | 否 |

### NotificationSnackBarHostState

| 方法                                                         | 说明                                                                         |
| ------------------------------------------------------------ | ---------------------------------------------------------------------------- |
| `suspend showNotification(title, subtitle, icon, imageType, actionText, durationMillis, onAction, onClose)` | 显示一个 Snackbar(会替换当前可见的),并挂起直到它被关闭或操作被执行 |

### NotificationSnackBarDefaults

| 常量                   | 类型    | 值      | COUI 来源                                                      |
| ---------------------- | ------- | ------- | -------------------------------------------------------------- |
| CornerRadius           | Dp      | 16.dp   | `couiRoundCornerL`(`coui_round_corner_l`)                       |
| ContentMarginStart     | Dp      | 16.dp   | `coui_snack_bar_context_margin_start_with_icon`                 |
| ContentVerticalPadding | Dp      | 16.dp   | `coui_snack_bar_icon_margin_top_horizontal`                     |
| LayoutMarginEnd        | Dp      | 6.dp    | `coui_notification_snack_bar_layout_margin_end`                 |
| IconEndPadding         | Dp      | 8.dp    | `coui_notification_snack_bar_icon_end`                          |
| SubContentTopPadding   | Dp      | 2.dp    | `coui_notification_snack_bar_sub_content_top`                   |
| CloseSize              | Dp      | 20.dp   | `coui_notification_snack_bar_close_width`                       |
| ClosePadding           | Dp      | 2.dp    | `coui_notification_snack_bar_close_padding`                     |
| CloseStartMargin       | Dp      | 2.dp    | `coui_notification_snack_bar_close_start`                       |
| CloseEndMargin         | Dp      | 6.dp    | `coui_notification_snack_bar_close_end`                         |
| CloseTopMargin         | Dp      | 6.dp    | `coui_notification_snack_bar_close_top`                         |
| ActionMaxWidth         | Dp      | 120.dp  | `coui_snack_bar_button_width`                                   |
| ActionMinWidth         | Dp      | 52.dp   | `coui_btn_small_width_min`                                      |
| ActionMinHeight        | Dp      | 28.dp   | `coui_btn_small_height_min`                                     |
| ActionStartMargin      | Dp      | 12.dp   | `coui_snack_bar_action_margin_horizontal_start`                 |
| ActionPadding          | PaddingValues | 12dp x 4dp | `Widget.COUI.Button.Small` 内边距                          |
| BottomMargin           | Dp      | 32.dp   | `coui_snack_bar_margin_bottom`                                  |
| MinDurationMillis      | Long    | 4000    | `COUINotificationSnackBar.MIN_DURATION`                         |
| MaxDurationMillis      | Long    | 10000   | `COUINotificationSnackBar.MAX_DURATION`                         |
| ReturnSpring           | SpringSpec&lt;Float&gt; | response 0.35 / bounce 0 | `getViewSpringTranslationAnimator`       |

#### 图片框

| `ImageType` | `iconWidth`               | `iconHeight`                |
| ----------- | ------------------------- | --------------------------- |
| Square      | 44dp(`coui_notification_snack_bar_icon_width`) | 44dp(同上) |
| Vertical    | 36dp(`..._vertical_icon_width`) | 48dp(`..._vertical_icon_height`) |
| Horizontal  | 58dp(`..._horizontal_icon_width`) | 44dp(`..._horizontal_icon_height`) |

### 颜色

| COUI 令牌                                    | 值                    | 用途       | 移植                                     |
| -------------------------------------------- | --------------------- | ---------- | ---------------------------------------- |
| `attr/couiColorBackgroundTop` → `color/coui_color_background_top` | `#ffffffff` / `#ff333333` | 容器底色 | `surfaceContainerHighest`(浅色完全一致,深色为 `#ff1e1e1e`) |
| `attr/couiColorPrimaryNeutral` → `color/coui_color_primary_neutral` | `#e6000000` / `#e6ffffff` | 标题 | `onSurface`                    |
| `attr/couiColorSecondNeutral` → `color/coui_color_secondary_neutral` | `#8a000000` / `#8affffff` | 副文本 | `onSurfaceSecondary`        |
| `color/coui_snack_bar_action_text_selector`(`..._enable`) | `#ffffffff` | 操作文案 | 文件常量                 |
| `attr/couiColorLabelTertiary` → `color/coui_color_label_tertiary` | `#42000000` / `#4dffffff` | 关闭图标 | `onSurfaceVariantActions` |

## 未移植

- 未移植 `RoundCornerUtil.isVersionSupport()` 为真时 `OplusOutline.setSmoothRoundRect`(`couiRoundCornerLRadius` 13dp / `couiRoundCornerLWeight` 0.99)绘制的平滑圆角;组件绘制的是原生在其余情况下使用的 `RoundedCornerShape` 回退(`outline.setRoundRect` + `couiRoundCornerL`)。
- 未移植 `onInterceptTouchEvent` / `onTouchEvent` 的滑动关闭(沿 X 或向下沿 Y 超过测量宽度的 1/8 即关闭,取消时按 response 0.35 / bounce 0 弹回,该弹簧以 `NotificationSnackBarDefaults.ReturnSpring` 暴露)。
- `ShadowUtils.setElevationToView(..., 2, couiRoundCornerL, support_shadow_size_level_four, coui_snack_bar_background_shadow_color)` 的阴影交由调用方的修饰符,而不是内置。
- `make()` 中「同一时刻只有一个」的父容器扫描由 `NotificationSnackBarHostState` 替代:显示新的会以 `NotificationSnackBarResult.Dismissed` 关闭上一个。
- 未移植 `make()` 在宿主主题解析不到 `couiColorBackgroundTop` / `couiColorPrimaryNeutral` 时的 `ContextThemeWrapper(Theme.COUI_Main)` 回退。
- 显示 / 关闭动画为淡入淡出加垂直滑动:原生的显示路径在 `COUISnackBar` 中,参考数据未给出其曲线。
- `coui_notification_snack_bar_item.xml` 中的 `tv_snack_bar_action` 在通知路径中并未使用(原生使用 `bt_notification_snack_bar` 按钮),因此移植只实现按钮形态。
