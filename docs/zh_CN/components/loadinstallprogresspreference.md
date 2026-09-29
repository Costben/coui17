# LoadInstallProgressPreference

`LoadInstallProgressPreference` 是右侧挂件为 `InstallLoadProgress` 按钮的偏好设置行，对应 ColorOS 的 `COUILoadInstallProgressPreference`。设备端该类继承 `COUIPreference`，并靠行样式 `Preference_COUI_COUILoadInstallProgressPreference` 把 `coui_preference_load_progress.xml` 加载进标准行的 `widget_frame`：一个裸 `COUIInstallLoadProgress`，`style="Widget.COUI.COUILoadProgress.InstallDownloadSmall"`，即 `InstallLoadStyle.Default`（52dp × 28dp 胶囊）。随后 `onBindViewHolder` 把偏好设置的文字、字号、颜色状态列表、`max`、`progress` 与 `state` 复制上去。

设备端的 `setState` 是设置应用调用的，不是按钮调用的：`COUILoadProgress` 点击后自行切换状态，并通过 `OnStateChangeListener` 上报，由偏好设置转发。在 Compose 中状态就是参数，因此没有监听器——`onClick` 是点击，状态由调用方推进。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.preference.LoadInstallProgressPreference
import io.github.suqi8.coui.kmp.basic.LoadProgressState
```

## 基本用法

```kotlin
var progress by remember { mutableIntStateOf(0) }
var state by remember { mutableStateOf(LoadProgressState.Default) }

LoadInstallProgressPreference(
    title = "系统更新",
    summary = "已是最新版本",
    state = state,
    progress = progress,
    progressText = if (state == LoadProgressState.Loading) "暂停" else "检查更新",
    onClick = { /* 开始或暂停下载 */ }
)
```

## 组件状态

### 下载中

状态为 `Loading` 时，胶囊按 `progress` 填充，与已填充部分重叠的文字会用第二颜色重绘：

```kotlin
LoadInstallProgressPreference(
    title = "系统更新",
    state = LoadProgressState.Loading,
    progress = 40,
    max = 100,
    progressText = "暂停",
    onClick = {}
)
```

### 其他状态

```kotlin
LoadInstallProgressPreference(
    title = "安装",
    state = LoadProgressState.Wait,
    progressText = "重试",
    onClick = {}
)
```

### 禁用状态

```kotlin
LoadInstallProgressPreference(
    title = "禁用",
    state = LoadProgressState.Default,
    progressText = "检查",
    onClick = {},
    enabled = false
)
```

## 属性

### LoadInstallProgressPreference 属性

| 属性名           | 类型                          | 说明                                              | 默认值                                 | 是否必须 |
| ---------------- | ----------------------------- | ------------------------------------------------- | -------------------------------------- | -------- |
| title            | String                        | 设置项标题                                        | -                                      | 是       |
| onClick          | () -> Unit                    | 进度按钮在边界内按下并抬起时的回调                | -                                      | 是       |
| modifier         | Modifier                      | 应用于组件的修饰符                                | Modifier                               | 否       |
| titleColor       | BasicComponentColors          | 标题文本的颜色配置                                | BasicComponentDefaults.titleColor()    | 否       |
| summary          | String?                       | 设置项摘要                                        | null                                   | 否       |
| summaryColor     | BasicComponentColors          | 摘要文本的颜色配置                                | BasicComponentDefaults.summaryColor()  | 否       |
| state            | LoadProgressState             | 当前状态（COUI `mState`）                          | LoadProgressState.Default              | 否       |
| progress         | Int                           | 当前进度，会被钳制到 `0..max`（COUI `mProgress`）  | LoadProgressButtonDefaults.PROGRESS    | 否       |
| max              | Int                           | `progress` 的参照值（COUI `mMax`）                 | LoadProgressButtonDefaults.MAX         | 否       |
| progressText     | String                        | 按钮文案（COUI `progressText`）                    | LoadProgressButtonDefaults.DEFAULT_TEXT | 否      |
| style            | InstallLoadStyle              | `couiStyle` 档位；设备端该行使用 `Default`（52×28dp 胶囊） | InstallLoadStyle.Default        | 否       |
| colors           | InstallLoadProgressColors     | 按钮配色                                          | InstallLoadProgressDefaults.colors()   | 否       |
| startAction      | @Composable (() -> Unit)?     | 左侧显示的自定义内容                              | null                                   | 否       |
| endActions       | @Composable RowScope.() -> Unit | 右侧显示的自定义内容（进度按钮之前）            | {}                                     | 否       |
| bottomAction     | @Composable (() -> Unit)?     | 底部显示的自定义内容                              | null                                   | 否       |
| insideMargin     | PaddingValues                 | 组件内部内容的边距                                | BasicComponentDefaults.InsideMargin    | 否       |
| cardListPosition | CardListPosition              | 该行在卡片组中的位置，圆角外边缘会获得额外内边距  | CardListPosition.None                  | 否       |
| holdDownState    | Boolean                       | 组件是否处于按下状态                              | false                                  | 否       |
| enabled          | Boolean                       | 进度按钮是否可用                                  | true                                   | 否       |
| hapticFeedback   | Boolean                       | 按钮按下与抬起时是否振动                          | false                                  | 否       |
| downloadingContentDescription | String?          | 加载中替代 `progressText` 的无障碍描述            | null                                   | 否       |

## 说明

### 有意省略

- **`OnStateChangeListener`。** 设备端的 `COUILoadProgress.toggle()` 会自行改变状态，并通过该监听器上报，再由偏好设置转发出来。在 Compose 中，本移植的 `InstallLoadProgress` 不会自行改变状态，监听器没有任何触发点——状态由调用方持有，并在 `onClick` 中推进。
- **`progressTextSize` 与三个颜色状态列表。** 设备端为文字字号，以及文字、背景、安装三处颜色提供了 `ColorStateList` 版本的 setter。`InstallLoadProgress` 会从主题内部解析这些值，因此这里只暴露 `colors`。
