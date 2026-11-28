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

## 跨沉浸页固定高度与软键盘抬升

`ToastHost` 默认开启 `applySystemInsets = true` 与 `lockNavigationBarInsets = true`，自动锁定非沉浸态的系统导航栏高度（Android 直接读取忽略可见性的系统导航栏高度，iOS / KMP 结合 `UIWindow.safeAreaInsets`、窗口尺寸分桶缓存与 `CouiSystemBarsController` 状态）。无论在普通页、沉浸式看图/视频页（隐藏导航栏）还是系统栏显隐动画过渡期间，甚至首次弹出就处于沉浸页，Toast 始终落在同一固定高度不跳动。

软键盘（IME）弹起时，底部偏移自动按以下公式平滑衔接：

```kotlin
max(lockedNavigationBarBottom + contentPadding.bottom, imeBottom + imeBottomPadding)
```

既不会重复累加导航栏高度，也不会把底栏预留高度叠加到键盘上方导致 Toast 浮在半空。例如应用有悬浮底栏时，只需传入固定 `contentPadding`：

```kotlin
ToastHost(
    state = toastState,
    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 82.dp),
)
```

当 `state.hasActiveToast` 为 `false` 时，`ToastHost` 不会订阅 `WindowInsets.ime`，避免键盘弹出/收起时每帧触发重组。

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
| `applySystemInsets` | `Boolean` | 展示时是否自动叠加导航栏与软键盘安全区 | `true` |
| `lockNavigationBarInsets` | `Boolean` | 是否锁定非沉浸态系统导航栏高度，防止沉浸页及系统栏显隐动画期间 Toast 位置跳动 | `true` |
| `imeBottomPadding` | `Dp` | 软键盘抬起超过静态底部留白时，Toast 与键盘顶部的间距 | `ToastDefaults.ImeBottomPadding`（`24.dp`） |
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
