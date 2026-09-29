# GridSpringChain

ColorOS 17 的网格弹簧链越界效果,对应 `com.coui.appcompat.springchain.COUIGridSpringChainScrollView`(`COUIScrollView` 的子类)与驱动它的 `com.coui.appcompat.springchain.COUIGridSpringChain` 弹簧链。

设备上滚动视图只持有一个实现 `ICOUIGridSpringChainViewGroup` 的子 View;触摸越界时它把越界量交给弹簧链,链按**行**各自位移,于是磁贴逐行拖尾而不是整块移动。本移植保留这一拆分:`Modifier.gridSpringChainOverscroll` 加在网格上,度量网格无法消费的拖拽量并阻尼成一个控制距离;`Modifier.gridSpringChainItem` 加在每个磁贴上,把该距离换算为按行的位移。

阻尼曲线与释放动画复用仓库已有的越界引擎 —— 拖拽用 `SpringMath.obtainDampingDistance`,回弹用 `SpringEngine.runSettleAnimation`(与 `utils/Overscroll.kt` 同一套弹簧)。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.GridSpringChainDefaults
import io.github.suqi8.coui.kmp.basic.GridSpringChainTranslation
import io.github.suqi8.coui.kmp.basic.gridSpringChainItem
import io.github.suqi8.coui.kmp.basic.gridSpringChainOverscroll
import io.github.suqi8.coui.kmp.basic.rememberGridSpringChainState
```

## 基本用法

```kotlin
val chainState = rememberGridSpringChainState()

LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    modifier = Modifier
        .fillMaxSize()
        .gridSpringChainOverscroll(chainState),
) {
    itemsIndexed(albums) { index, album ->
        AlbumTile(
            album = album,
            // 同一行的磁贴共用一个位移,因此传行号而不是条目下标。
            modifier = Modifier.gridSpringChainItem(chainState, row = index / 2),
        )
    }
}
```

网格必须能上报无法消费的越界量 —— `LazyVerticalGrid`、带 `verticalScroll` 的 `Column`,或任何嵌套滚动源都可以。`gridSpringChainItem` 在 `graphicsLayer` 块内读取链距离,因此距离变化只会重绘磁贴图层,不会触发重组。

## 属性

### rememberGridSpringChainState

| 属性名                | 类型                       | 说明                                    | 默认值                                             | 是否必须 |
| --------------------- | -------------------------- | --------------------------------------- | -------------------------------------------------- | -------- |
| translationCalculator | GridSpringChainTranslation | 由行号与控制距离换算该行的位移          | GridSpringChainDefaults.AutoAccumulatingTranslation | 否       |

### GridSpringChainState

| 属性名         | 类型    | 说明                                             |
| -------------- | ------- | ------------------------------------------------ |
| isActive       | Boolean | 网格当前是否被拖过边界                           |
| translationFor | Float   | `translationFor(row: Int)`:第 row 行的像素位移   |

### Modifier.gridSpringChainOverscroll

| 参数名  | 类型                 | 说明                              | 默认值 | 是否必须 |
| ------- | -------------------- | --------------------------------- | ------ | -------- |
| state   | GridSpringChainState | 与网格磁贴共享的状态              | -      | 是       |
| enabled | Boolean              | 为 false 时网格正常滚动           | true   | 否       |

### Modifier.gridSpringChainItem

| 参数名 | 类型                 | 说明                       | 默认值 | 是否必须 |
| ------ | -------------------- | -------------------------- | ------ | -------- |
| state  | GridSpringChainState | 与网格共享的状态           | -      | 是       |
| row    | Int                  | 磁贴的零基行号             | -      | 是       |

### GridSpringChainDefaults

| 常量                      | 类型 | 值    | 设备来源                                                     |
| ------------------------- | ---- | ----- | ------------------------------------------------------------ |
| MaxDistanceFactor         | Float | 0.25f | `COUIGridSpringChainScrollView.MAX_DISTANCE_FACTOR`          |
| MaxDistanceRatio          | Float | 0.5f  | `COUIGridSpringChainScrollView.MAX_DISTANCE_RATIO`           |
| OverScrollFactor          | Float | 0.3f  | `COUIGridSpringChainScrollView.OVER_SCROLL_FACTOR`           |
| DefaultTranslationFactor  | Float | 0.1f  | `COUIGridSpringChain.DefaultTransCalculator#getTrans`        |
| ChainFactor               | Float | 0.3f  | `COUIChainDecorator.FACTOR`                                  |
| ChainMarginVertical       | Float | 24f   | `COUIChainDecorator.DEFAULT_MARGIN_VERTICAL`                 |
| MainTension               | Float | 40f   | `COUISpringChain.DEFAULT_MAIN_TENSION`                       |
| MainFriction              | Float | 6f    | `COUISpringChain.DEFAULT_MAIN_FRICTION`                      |
| AttachmentTension         | Float | 70f   | `COUISpringChain.DEFAULT_ATTACHMENT_TENSION`                 |
| AttachmentFriction        | Float | 10f   | `COUISpringChain.DEFAULT_ATTACHMENT_FRICTION`                |
| GridMainTension           | Float | 150f  | `COUIGridSpringChain.MAIN_TENSION`(覆盖 MainTension)         |
| GridMainFriction          | Float | 10f   | `COUIGridSpringChain.MAIN_FRICTION`(覆盖 MainFriction)       |
| BackToTop / BackToBottom / BackToLeft / BackToRight | Int | 1 / 2 / 3 / 4 | `COUIGridSpringChain.BACK_TO_*`     |
| AutoAccumulatingTranslation | GridSpringChainTranslation | `(row + 1) * 0.1f * distance` | `COUIGridSpringChain`(`enableAutoAcc = true` 且 `DefaultTransCalculator`) |
| ChainLadderTranslation      | GridSpringChainTranslation | `COUIChainDecorator` 的 `backToTop` 阶梯 | `COUIChainDecorator`(设备上定义后被覆盖) |

## 行为

- 网格剩余的拖拽量会被累加,并用 `SpringMath.obtainDampingDistance` 在 `MaxDistanceRatio * 容器高度` 上阻尼,存为控制距离。抛掷后由仓库的弹簧引擎把控制距离回弹到零。
- 默认的 `GridSpringChainDefaults.AutoAccumulatingTranslation` 复刻设备实际使用的链:`COUIGridSpringChain` 以 `enableAutoAcc = true` 运行、两个边界计算器都保持 `DefaultTransCalculator`,于是 `calculateTranslation` 求和 `getTrans(0..row, distance)` = `(row + 1) * 0.1f * distance`。行号越靠后位移越大。
- `GridSpringChainDefaults.ChainLadderTranslation` 是 decorator 自带的尾部行阶梯,暴露给需要它的调用方。设备定义了它,但随后调用 `setTranCalculator(1)` / `setTranCalculator(2)` 把两个已安装的计算器都改写成 `DefaultTransCalculator`,所以它**不是**默认网格使用的那个。

## 未移植

以下设备类属于 Android View 基础设施,Compose 侧没有对应物,故本库不提供:

| 设备类 | 超类 / 角色 |
| ------ | ----------- |
| `COUIGridSpringChainScrollView` | `COUIScrollView` → `ScrollView`;网格即调用方的可滚动容器 |
| `COUIChainDecorator` | 触摸助手,绑定 `View.OnTouchListener` 与 `RecyclerView.ItemDecoration` 式边界 |
| `ICOUIGridSpringChainViewGroup` | 滚动视图唯一子 View 实现的接口 |
| `ICheckViewEdge` | `getHeight` / `isReachTopEdge` / `isReachBottomEdge` 边界契约 |
| `IChainItem` / `COUIChainItem` | 调用 `setTranslationX/Y` 的 `View` 代理 |
| `COUIGridSpringChainItem` | 实现 `IChainItem` 的 `FrameLayout` |
| `ISpringUpdateListener` | 回调到 View 层 |

上表的弹簧常量只作记录,不直接参与计算:仓库的 `SpringEngine` 是临界阻尼积分器,以阻尼比与自然周期为参数,设备的 tension/friction 数对无法一一映射到它。因此释放动画沿用仓库既有的越界弹簧。
