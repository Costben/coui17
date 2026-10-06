# GridSpringChain

The ColorOS 17 grid spring-chain overscroll effect, aligned with the ColorOS 17 design language: a scrollable grid whose tiles trail off in a chain when the content is pulled past its edge.

In ColorOS the scroll view holds a single child; when a touch passes an edge it forwards the overscroll to a chain, which moves each grid **row** by its own translation, so the tiles trail off in a chain rather than moving as one block. This port keeps that split: `Modifier.gridSpringChainOverscroll` on the grid measures the drag that the grid could not consume and damps it into one control distance, while `Modifier.gridSpringChainItem` on each tile turns that distance into a per-row translation.

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

| Constant                  | Type | Value |
| ------------------------- | ---- | ----- |
| MaxDistanceFactor         | Float | 0.25f |
| MaxDistanceRatio          | Float | 0.5f  |
| OverScrollFactor          | Float | 0.3f  |
| DefaultTranslationFactor  | Float | 0.1f  |
| ChainFactor               | Float | 0.3f  |
| ChainMarginVertical       | Float | 24f   |
| MainTension               | Float | 40f   |
| MainFriction              | Float | 6f    |
| AttachmentTension         | Float | 70f   |
| AttachmentFriction        | Float | 10f   |
| GridMainTension           | Float | 150f  |
| GridMainFriction          | Float | 10f   |
| BackToTop / BackToBottom / BackToLeft / BackToRight | Int | 1 / 2 / 3 / 4 |
| AutoAccumulatingTranslation | GridSpringChainTranslation | `(row + 1) * 0.1f * distance` |
| ChainLadderTranslation      | GridSpringChainTranslation | The trailing-row ladder |

## Behavior

- The grid's leftover drag is accumulated and damped with `SpringMath.obtainDampingDistance` over `MaxDistanceRatio * container height`, then stored as the control distance. A fling settles it back to zero with the repository's spring engine.
- The default [GridSpringChainDefaults.AutoAccumulatingTranslation] reproduces the reference chain: automatic accumulation is on and both edge calculators are left at their default, so the translation is summed from row 0, giving `(row + 1) * 0.1f * distance` for each row. Later rows carry a larger translation than earlier ones.
- [GridSpringChainDefaults.ChainLadderTranslation] is the trailing-row ladder, exposed for callers that want it. ColorOS defines it but then overwrites both installed calculators, so it is **not** what the default grid uses.

## Not ported

ColorOS's spring-chain implementation is Android View infrastructure with no Compose-side equivalent, so none of it has a direct counterpart here: the scroll view that hosts the effect, the decorator that wires touches and edges, the child interface and its edge contract, the per-item view proxy, and the spring-update listener.

The spring constants above are recorded but not consumed directly: the repository's `SpringEngine` is a critically damped integrator parameterised by damping ratio and natural period, so ColorOS's tension/friction pairs cannot be mapped onto it one-for-one. The release therefore follows the same spring as the repository's existing overscroll.
