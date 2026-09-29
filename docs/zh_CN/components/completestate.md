# CompleteState

`CompleteState` 是 COUI 中的整页完成状态,用于操作结束之后——优化完成、同步完成、开通超时——对应 ColorOS 的 COUICompleteStateView(coui_component_complete_state.xml):100dp 的动画、标题与可选副标题,整体水平居中。

页面没有按钮:设备上本就没有,退出的时机交给调用方。设备的宽度来自响应式栅格——样式里 `gridNumber = @integer/complete_page_grid_guide_column`(4)、`paddingType = cardListType`——因此宽度取决于屏幕尺寸档位而非固定 dp;请通过 `modifier` 传入你需要的宽度。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.CompleteState
import io.github.suqi8.coui.kmp.basic.CompleteStateDefaults
import io.github.suqi8.coui.kmp.basic.CompleteStateStatus
```

## 基本用法

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(COUITheme.colorScheme.surface),
    contentAlignment = Alignment.Center,
) {
    CompleteState(
        status = CompleteStateStatus.Success,
        title = "已优化 12 项设置",
        subtitle = "系统状态已更新",
        modifier = Modifier.fillMaxWidth(),
    )
}
```

## 属性

### CompleteState 属性

| 属性名       | 类型                      | 说明                                        | 默认值                                      | 是否必须 |
| ------------ | ------------------------- | ------------------------------------------- | ------------------------------------------- | -------- |
| status       | CompleteStateStatus       | 页面呈现的完成状态                          | -                                           | 是       |
| title        | String                    | 居中标题(20sp,中等字重)                   | -                                           | 是       |
| modifier     | Modifier                  | 应用于完成状态的修饰符                      | Modifier                                    | 否       |
| subtitle     | String?                   | 居中副标题(12sp),为 null 时不显示          | null                                        | 否       |
| illustration | (@Composable () -> Unit)? | 文字上方的动画,置于 100dp 的框内            | null                                        | 否       |
| colors       | CompleteStateColors       | 文字颜色配置                                | CompleteStateDefaults.completeStateColors() | 否       |

### CompleteStateStatus

| 取值    | COUI 常量                 | 设备上的动画                        | 时长   |
| ------- | ------------------------- | ----------------------------------- | ------ |
| Default | COMPLETE_TYPE_DEFAULT (0) | 无                                  | 0ms    |
| Success | COMPLETE_TYPE_SUCCESS (1) | raw/complete_page_success_anim.json | 1483ms |
| Failed  | COMPLETE_TYPE_FAILED (2)  | raw/complete_page_fail_anim.json    | 1017ms |
| Wait    | COMPLETE_TYPE_WAIT (3)    | raw/complete_page_wait_anim.json    | 3000ms |

### CompleteStateDefaults 对象

| 常量 / 函数                | 类型     | 默认值    | COUI 来源                                        |
| -------------------------- | -------- | --------- | ------------------------------------------------ |
| IllustrationSize           | Dp       | 100.dp    | complete_anim(100dp × 100dp)                    |
| TextPaddingHorizontal      | Dp       | 8.dp      | complete_page_text_padding_hor                   |
| TitleMarginTop             | Dp       | 8.dp      | complete_page_text_title_margin_top              |
| SubtitleMarginTop          | Dp       | 8.dp      | complete_page_text_subtitle_margin_top           |
| SuccessDuration            | Duration | 1483ms    | complete_page_success_anim.json(60fps 下 89 帧) |
| FailedDuration             | Duration | 1017ms    | complete_page_fail_anim.json(60fps 下 61 帧)    |
| WaitDuration               | Duration | 3000ms    | complete_page_wait_anim.json(60fps 下 180 帧)   |
| animationDuration(status)  | Duration | 按状态    | 各 raw 的 Lottie `op` / `fr`                    |

`CompleteStateDefaults.animationDuration(status)` 返回该状态动画的时长,调用方据此计时。

### `completeStateColors()` 工厂

| 参数          | 类型  | 默认值                                   | COUI 角色               |
| ------------- | ----- | ---------------------------------------- | ----------------------- |
| titleColor    | Color | COUITheme.colorScheme.onSurface          | couiColorLabelPrimary   |
| subtitleColor | Color | COUITheme.colorScheme.onSurfaceSecondary | couiColorLabelSecondary |

### CompleteStateColors 类

| 属性名        | 类型  | 说明         |
| ------------- | ----- | ------------ |
| titleColor    | Color | 标题颜色     |
| subtitleColor | Color | 副标题颜色   |

## 行为

- 插画框为 100dp × 100dp 并居中。设备无论处于 `CompleteStateStatus.Default`(未设置动画)还是其他状态,都按这个尺寸测量并自动播放(`anim_autoPlay` 默认 true);本库不含动画资源,因此只在传入 `illustration` 时保留该框。
- 动画是 300×300 画布的 oplus-anim(Lottie)文件,缩放进 100dp 的框内。三个 raw 都没有夜间变体,因此插画在明暗主题下一致,文字则跟随主题颜色。
- 标题与副标题撑满宽度,左右各 8dp 内边距(`complete_page_text_padding_hor`),各自上方 8dp。
- 页面没有按钮也没有遮罩:设备上由调用方用它替换页面内容,再在结束时移除。

## 进阶用法

### 按动画时长计时

```kotlin
var status by remember { mutableStateOf(CompleteStateStatus.Wait) }

LaunchedEffect(status) {
    delay(CompleteStateDefaults.animationDuration(status))
    /* 离开该页面 */
}
```

### 自定义颜色

```kotlin
CompleteState(
    status = CompleteStateStatus.Failed,
    title = "未能完成",
    subtitle = "请检查网络后重试",
    colors = CompleteStateDefaults.completeStateColors(
        titleColor = COUITheme.colorScheme.error
    ),
)
```
