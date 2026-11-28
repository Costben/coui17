# Toast

`Toast` 是 1:1 对标 ColorOS 17 系统原生 Toast（`OplusToastLayout` + `OplusToastDefaultAnimation` + `OplusToastUI`）的胶囊轻提示组件：单槽位固定底部居中原地进退场、无堆叠无上下滑入滑出，同时支持可选的轻交互操作与关闭按钮。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.Toast
import io.github.suqi8.coui.kmp.basic.ToastColors
import io.github.suqi8.coui.kmp.basic.ToastData
import io.github.suqi8.coui.kmp.basic.ToastDefaults
import io.github.suqi8.coui.kmp.basic.ToastHost
import io.github.suqi8.coui.kmp.basic.ToastHostState
import io.github.suqi8.coui.kmp.basic.rememberToastHostState
```

## 基本用法

将 `ToastHost` 常驻挂载在根层叠加容器中。当 `state.hasActiveToast` 为 `false` 时，`ToastHost` 直接返回，不做全屏测量也不订阅 `WindowInsets.ime`，避免键盘弹出/收起掉帧。

```kotlin
val toastState = rememberToastHostState()

Box(Modifier.fillMaxSize()) {
    // 页面内容
    TextButton(
        text = "保存",
        onClick = { toastState.showToast("已保存到相册") },
    )

    ToastHost(state = toastState)
}
```

## 轻交互与关闭按钮

`showToast` 支持可选的 `actionLabel` / `onAction` 以及关闭按钮（`withDismissAction = true`）：

```kotlin
toastState.showToast(
    message = "剪贴板发现微博链接",
    durationMillis = 6_000L,
    actionLabel = "打开",
    withDismissAction = true,
    onAction = { openLink() },
)
```

## 空闲时跳过 `WindowInsets.ime` 读取

若业务端需要根据软键盘与底部悬浮栏动态调整底部留白，可通过 `state.hasActiveToast` 在无提示时跳过 `WindowInsets.ime` 订阅：

```kotlin
val bottomPadding = if (toastState.hasActiveToast) {
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    if (!imeOpen && hasFloatingBottomBar) 82.dp else 44.dp
} else {
    44.dp
}

ToastHost(
    state = toastState,
    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = bottomPadding),
)
```

## 属性与 API

### ToastHostState

| 成员 | 类型 / 签名 | 说明 |
| --- | --- | --- |
| `currentToast` | `ToastData?` | 当前展示中的 Toast（含进退场阶段），无展示时为 `null` |
| `hasActiveToast` | `Boolean` | 当前是否存在活跃 Toast |
| `showToast(...)` | `(message, durationMillis = 2000L, actionLabel = null, withDismissAction = false, onAction = null)` | 展示 Toast；若已有 Toast 则先退场再原地进场，退场期间连续到达的新消息会合并只展示最新一条 |
| `dismiss()` | `() -> Unit` | 手动关闭当前 Toast（播放 250ms 退场动画） |

### ToastHost

| 属性 | 类型 | 说明 | 默认值 |
| --- | --- | --- | --- |
| `state` | `ToastHostState` | 控制 Toast 展示的状态对象 | 必填 |
| `modifier` | `Modifier` | 有活跃 Toast 时应用于宿主容器的修饰符 | `Modifier` |
| `contentAlignment` | `Alignment` | Toast 在宿主内的对齐方式 | `Alignment.BottomCenter` |
| `contentPadding` | `PaddingValues` | Toast 外部留白 | `ToastDefaults.HostContentPadding` |
| `applySystemInsets` | `Boolean` | 展示时是否自动叠加 `navigationBarsPadding()` 与 `imePadding()` | `true` |
| `toast` | `@Composable (ToastData) -> Unit` | 自定义 Toast 渲染槽位 | `{ Toast(it, onDismiss = state::dismiss) }` |

### Toast

| 属性 | 类型 | 说明 | 默认值 |
| --- | --- | --- | --- |
| `message` | `String` | 提示文案（最多 2 行，超出省略） | 必填 |
| `modifier` | `Modifier` | 应用于胶囊容器的修饰符 | `Modifier` |
| `actionLabel` | `String?` | 可选的尾部操作文案 | `null` |
| `withDismissAction` | `Boolean` | 是否显示尾部关闭图标 | `false` |
| `onAction` | `(() -> Unit)?` | 点击操作按钮或胶囊主体时的回调 | `null` |
| `onDismiss` | `(() -> Unit)?` | 触发关闭时的回调 | `null` |
| `colors` | `ToastColors` | 背景、文字、操作色、关闭色与投影配置 | `ToastDefaults.colors()` |
| `elevation` | `Dp` | 胶囊阴影高度 | `ToastDefaults.Elevation`（`12.dp`） |
| `dismissContentDescription` | `String` | 关闭按钮的无障碍描述 | `ToastDefaults.DismissContentDescription` |
