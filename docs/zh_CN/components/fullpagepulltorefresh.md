# FullPagePullToRefresh

`FullPagePullToRefresh` 是仿 OPPO 应用商店「精选」页的整页下拉刷新容器。与 miuix 风格的 [PullToRefresh](./pulltorefresh) 不同，它不会在列表上方长出一个球，而是让整页跟随手指下移，露出背后的全屏刷新层，顶部提示文字从固定裁切线逐行显现。

::: warning 注意
此组件仅适用于支持触控的环境!
:::

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.FullPagePullToRefresh
import io.github.suqi8.coui.kmp.basic.rememberFullPagePullToRefreshState
```

## 基本用法

```kotlin
var isRefreshing by rememberSaveable { mutableStateOf(false) }
val refreshState = rememberFullPagePullToRefreshState()
var items by remember { mutableIntStateOf(12) }
val contentPadding = PaddingValues(top = 72.dp)

LaunchedEffect(isRefreshing) {
    if (isRefreshing) {
        delay(1000)
        items += 4
        isRefreshing = false
    }
}

FullPagePullToRefresh(
    isRefreshing = isRefreshing,
    onRefresh = { isRefreshing = true },
    state = refreshState,
    contentPadding = contentPadding,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(items) { index ->
            Card(Modifier.fillMaxWidth()) {
                BasicComponent(title = "资源 ${index + 1}")
            }
        }
    }
}
```

::: tip 顶部留白就是显现基准线
请把同样的顶部留白同时传给可滚动内容。`contentPadding` 的 top 会作为沉浸高度：提示文字从这条线开始随页面下移而显现。如果内容在首项之上绘制了不透明背景，提示会被内容盖住。
:::

## 组件状态

1. `Idle`：无交互，页面处于静止状态
2. `Pulling`：页面以 0.5 线性阻尼跟随手指
3. `ThresholdReached`：下拉距离已超过刷新阈值
4. `Refreshing`：页面停在阈值处，加载弧线旋转
5. `RefreshComplete`：成功提示停留 800ms，随后页面回弹

## 属性

### FullPagePullToRefresh 属性

| 属性名                      | 类型                                     | 说明                                | 默认值                                | 是否必须 |
| --------------------------- | ---------------------------------------- | ----------------------------------- | ------------------------------------- | -------- |
| isRefreshing                | Boolean                                  | 提升的刷新状态                      | 无                                    | 是       |
| onRefresh                   | () -> Unit                               | 手势触发刷新并停稳后的回调          | 无                                    | 是       |
| modifier                    | Modifier                                 | 应用于容器的修饰符                  | Modifier                              | 否       |
| state                       | FullPagePullToRefreshState               | 可视状态控制器                      | rememberFullPagePullToRefreshState()  | 否       |
| contentPadding              | PaddingValues                            | 内容内边距，top 同时作为显现基准线  | PaddingValues(0.dp)                   | 否       |
| topAppBarScrollBehavior     | ScrollBehavior?                          | 顶部应用栏滚动行为                  | null                                  | 否       |
| hapticFeedbackEnabled       | Boolean                                  | 越过阈值时是否触发触感反馈          | true                                  | 否       |
| completeHoldDurationMillis  | Long                                     | 成功提示停留时长                    | 800                                   | 否       |
| colors                      | FullPagePullToRefreshColors              | 背景、文字与加载弧线颜色            | fullPagePullToRefreshColors()         | 否       |
| refreshThreshold            | Dp                                       | 触发刷新的下拉距离                  | 93.187.dp                             | 否       |
| textStyle                   | TextStyle                                | 提示文字样式                        | 14sp                                  | 否       |
| refreshTexts                | FullPagePullToRefreshTexts               | 四种状态下的提示文案                | 英文默认值                            | 否       |
| background                  | (@Composable BoxScope.(State) -> Unit)?   | 内容背后的全屏刷新层                | null                                  | 否       |
| content                     | @Composable () -> Unit                   | 可滚动的页面内容                    | 无                                    | 是       |

### FullPagePullToRefreshState

| 属性名        | 类型                 | 说明                                |
| ------------- | -------------------- | ----------------------------------- |
| refreshState  | FullPageRefreshState | 当前可视状态                        |
| dragOffset    | Float                | 当前页面下移距离（像素）            |
| pullProgress  | Float                | 相对阈值的下拉进度，范围 0 到 1     |

`startRefresh(onRefresh)` 可以在没有手势的情况下播放刷新动画。`isRefreshing` 提升状态仍由调用方维护。

### FullPagePullToRefreshDefaults

| 属性名                     | 类型                       | 说明             | 默认值    |
| -------------------------- | -------------------------- | ---------------- | --------- |
| refreshThreshold           | Dp                         | 应用商店阈值     | 93.187.dp |
| spinnerSize                | Dp                         | 加载弧线直径     | 26.dp     |
| textStyle                  | TextStyle                  | 提示文字样式     | 14sp      |
| COMPLETE_HOLD_DURATION_MILLIS | Long                    | 成功提示停留时长 | 800       |
| refreshTexts               | FullPagePullToRefreshTexts | 默认提示文案     | 英文      |

## 背景层

`background` 插槽绘制在内容背后的全屏区域，并会收到状态，可用于让宣传图随下拉淡入或视差移动：

```kotlin
FullPagePullToRefresh(
    // ...
    background = { state ->
        Image(
            painter = painterResource("refresh_bg.png"),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = state.pullProgress },
            contentScale = ContentScale.Crop,
        )
    },
) {
    // 内容
}
```

## 动画细节

默认参数复刻应用商店实现：

- 阻尼：线性，`dragOffset = 手指位移 * 0.5`
- 松手到达阈值：`DecelerateInterpolator(2)` 风格曲线，时长按距离缩放，最长 500ms
- 未达阈值回弹：同一曲线，时长按距离缩放，最长 500ms
- 成功提示：`isRefreshing` 变为 `false` 后停留 800ms，再回弹
- 800ms 停留期间再次触摸会取消停留，立即回弹

## 注意事项

- 内容必须可滚动，并应消费与 `contentPadding` 相同的顶部留白，否则提示会被内容遮住。
- 刷新中与成功提示停留期间会消费纵向嵌套滚动，页面无法滚动。
- `PullToRefresh` 与 `FullPagePullToRefresh` 是独立组件，同一页面选用其一即可。
