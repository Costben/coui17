# GridSpringChain

The ColorOS 17 grid spring-chain overscroll effect, mirroring `com.coui.appcompat.springchain.COUIGridSpringChainScrollView` (a `COUIScrollView` subclass) and the `com.coui.appcompat.springchain.COUIGridSpringChain` chain that drives it.

On the device the scroll view holds a single child implementing `ICOUIGridSpringChainViewGroup`; when a touch passes an edge it forwards the overscroll to the chain, which moves each grid **row** by its own translation, so the tiles trail off in a chain rather than moving as one block. This port keeps that split: `Modifier.gridSpringChainOverscroll` on the grid measures the drag that the grid could not consume and damps it into one control distance, while `Modifier.gridSpringChainItem` on each tile turns that distance into a per-row translation.

The damping curve and the release animation reuse the repository's existing overscroll engine — `SpringMath.obtainDampingDistance` for the drag and `SpringEngine.runSettleAnimation` (the same spring `utils/Overscroll.kt` drives) for the settle.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.GridSpringChainDefaults
import io.github.suqi8.coui.kmp.basic.GridSpringChainTranslation
import io.github.suqi8.coui.kmp.basic.gridSpringChainItem
import io.github.suqi8.coui.kmp.basic.gridSpringChainOverscroll
import io.github.suqi8.coui.kmp.basic.rememberGridSpringChainState
```

## Basic Usage

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
            // Tiles of one row share a translation; pass the row index, not the item index.
            modifier = Modifier.gridSpringChainItem(chainState, row = index / 2),
        )
    }
}
```

The grid must be able to report an edge the drag cannot consume — a `LazyVerticalGrid`, a `Column` with `verticalScroll`, or any other nested-scroll source works. `gridSpringChainItem` reads the chain distance inside a `graphicsLayer` block, so a changing distance only re-layers the tiles, it does not recompose them.

## Properties

### rememberGridSpringChainState

| Property                | Type                         | Description                                              | Default Value                                        | Required |
| ----------------------- | ---------------------------- | -------------------------------------------------------- | ---------------------------------------------------- | -------- |
| translationCalculator   | GridSpringChainTranslation   | Maps a row index and the control distance to its translation | GridSpringChainDefaults.AutoAccumulatingTranslation | No       |

### GridSpringChainState

| Property        | Type    | Description                                                              |
| --------------- | ------- | ------------------------------------------------------------------------ |
| isActive        | Boolean | Whether the grid is currently pulled past its edge                       |
| translationFor  | Float   | `translationFor(row: Int)`: the pixel translation for a grid row         |

### Modifier.gridSpringChainOverscroll

| Parameter | Type                  | Description                                     | Default Value | Required |
| --------- | --------------------- | ----------------------------------------------- | ------------- | -------- |
| state     | GridSpringChainState  | The state shared with the grid's tiles          | -             | Yes      |
| enabled   | Boolean               | When false the grid scrolls normally            | true          | No       |

### Modifier.gridSpringChainItem

| Parameter | Type                  | Description                                              | Default Value | Required |
| --------- | --------------------- | -------------------------------------------------------- | ------------- | -------- |
| state     | GridSpringChainState  | The state shared with the grid                           | -             | Yes      |
| row       | Int                   | The zero-based row index of the tile                    | -             | Yes      |

### GridSpringChainDefaults

| Constant                  | Type | Value | Device source                                                |
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
| GridMainTension           | Float | 150f  | `COUIGridSpringChain.MAIN_TENSION` (overrides MainTension)   |
| GridMainFriction          | Float | 10f   | `COUIGridSpringChain.MAIN_FRICTION` (overrides MainFriction) |
| BackToTop / BackToBottom / BackToLeft / BackToRight | Int | 1 / 2 / 3 / 4 | `COUIGridSpringChain.BACK_TO_*`              |
| AutoAccumulatingTranslation | GridSpringChainTranslation | `(row + 1) * 0.1f * distance` | `COUIGridSpringChain` with `enableAutoAcc = true` and `DefaultTransCalculator` |
| ChainLadderTranslation      | GridSpringChainTranslation | `COUIChainDecorator`'s `backToTop` ladder | `COUIChainDecorator` (defined but overwritten on the device) |

## Behavior

- The grid's leftover drag is accumulated and damped with `SpringMath.obtainDampingDistance` over `MaxDistanceRatio * container height`, then stored as the control distance. A fling settles it back to zero with the repository's spring engine.
- The default [GridSpringChainDefaults.AutoAccumulatingTranslation] reproduces the device's actual chain: `COUIGridSpringChain` runs with `enableAutoAcc = true` and both edge calculators left at `DefaultTransCalculator`, so `calculateTranslation` sums `getTrans(0..row, distance)` = `(row + 1) * 0.1f * distance`. Later rows carry a larger translation than earlier ones.
- [GridSpringChainDefaults.ChainLadderTranslation] is the decorator's own trailing-row ladder, exposed for callers that want it. The device defines it but then calls `setTranCalculator(1)` / `setTranCalculator(2)`, which overwrite both installed calculators with `DefaultTransCalculator`, so it is **not** what the default grid uses.

## Not ported

These device classes are Android View infrastructure with no Compose-side equivalent, so they have no counterpart here:

| Device class | Superclass / role |
| ------------ | ----------------- |
| `COUIGridSpringChainScrollView` | `COUIScrollView` → `ScrollView`; the grid is the caller's scrollable |
| `COUIChainDecorator` | Touch helper that wires a `View.OnTouchListener` and `RecyclerView.ItemDecoration`-style edges |
| `ICOUIGridSpringChainViewGroup` | Interface implemented by the scroll view's single child |
| `ICheckViewEdge` | `getHeight` / `isReachTopEdge` / `isReachBottomEdge` edge contract |
| `IChainItem` / `COUIChainItem` | A `View` proxy that calls `setTranslationX/Y` |
| `COUIGridSpringChainItem` | `FrameLayout` implementing `IChainItem` |
| `ISpringUpdateListener` | Callback into the View layer |

The spring constants above are recorded but not consumed directly: the repository's `SpringEngine` is a critically damped integrator parameterised by damping ratio and natural period, so the device's tension/friction pairs cannot be mapped onto it one-for-one. The release therefore follows the same spring as the repository's existing overscroll.
