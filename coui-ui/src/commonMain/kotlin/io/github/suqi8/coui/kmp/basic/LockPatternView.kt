// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.COUIInEase
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The ColorOS 17 pattern-unlock grid, the port of `COUILockPatternView` and `COUILockPatternUtils`
 * (`com.coui.appcompat.lockview`).
 *
 * Nine dots sit in a 3x3 grid inside [LockPatternViewDefaults.Width] x
 * [LockPatternViewDefaults.Height] (312dp x 280dp) with
 * [LockPatternViewDefaults.HorizontalPadding] (21dp) / [LockPatternViewDefaults.VerticalPadding]
 * (5dp) around them, which makes each cell 90dp square — the geometry of
 * `getCenterXForColumn` / `getCenterYForRow`, whose `mSquareWidth` is
 * `(width - paddingLeft - paddingRight) / 3`. A pointer grabs a cell when it is inside the middle
 * `mHitFactor` (0.6) of it, as `getColumnHit` / `getRowHit` test.
 *
 * The pattern is reported as a `List<Int>` of cell indices, `row * 3 + column`, which is the order
 * `COUILockPatternUtils.patternToString` writes as `(char) (index + '1')`: index 0 is the top-left
 * dot, index 8 the bottom-right one.
 *
 * Two details of `detectAndAddHit` are kept because they change what the user gets: jumping over the
 * middle of a row, a column or a diagonal inserts the skipped cell first, and exactly one cell is
 * ever added per event, so a fast drag cannot pass through a dot unnoticed.
 *
 * Drawing follows `onDraw`:
 * - the connected path is drawn first as the [LockPatternViewDefaults.PathWidth] round-joined,
 *   round-capped stroke of `mPathPaint` (`lock_pattern_dot_line_width`);
 * - each activated cell then draws the two PNG discs of the native — the
 *   `coui_lock_pattern_inner_circle` fill fading in over 230ms on `COUIEaseInterpolator`, and the
 *   `coui_lock_pattern_outer_circle` ripple growing from scale 1 to scale 7 over 460ms on
 *   `COUIInEaseInterpolator` while its alpha ramps 0 -> peak -> 0 on keyframes at fractions 0 / 0.5
 *   / 1 (`startInnerAnimation`, `startOuterAnimation`);
 * - every cell whose `CellState.needDrawCircle` is true also draws the plain
 *   [LockPatternColors.dotColor] dot of `drawCircle`.
 *
 * The colours are the two styles the constructor picks between,
 * `Widget.COUI.COUILockPatternView` and `Widget.COUI.COUILockPatternView.Dark`
 * (`COUIContextUtil.isCOUIDarkTheme(context)`). `COUITheme.colorScheme` has no slot for any of them:
 * the closest is [COUITheme.colorScheme.error] (`#ffdb382c` light / `#ffff6c61` dark) against the
 * native `#ffea3447`, so the recovered values are file constants behind
 * [LockPatternViewDefaults.colors].
 *
 * The native is deliberately inconsistent about alpha and so is the port: `mPathPaint` gets
 * `setColor(getCurrentColor(true))` and then `setAlpha(mPathAlpha * 255)`, so the path is fully
 * opaque even while the 30% [LockPatternColors.dotColor] is the current colour, whereas the two cell
 * discs are tinted with that colour and therefore keep its alpha. `getCurrentColor(true)` returns
 * the regular colour while `mPatternInProgress`, which is why a finished pattern tightens from the
 * 30% dot colour to the opaque success colour.
 *
 * Not ported:
 * - `PatternExploreByTouchHelper` (`ExploreByTouchHelper`) with its virtual views 1..9, the
 *   `announceForAccessibility` calls of `notifyPatternStarted` / `notifyPatternCellAdded` /
 *   `notifyPatternCleared` / `notifyPatternDetected`, and the `lockscreen_access_pattern_*` strings;
 * - `SavedState` and the pattern (de)serialisation of `COUILockPatternUtils`: the pattern goes to
 *   the caller instead of into the view's own saved state;
 * - the haptics of `performHitFeedback` / `performWrongModeFeedback`
 *   (`COUIHapticFeedbackConstants.GRANULAR_SHORT_VIBRATE` / `SHORT_TRIPLE_VIBRATE`); commonMain has
 *   no vibration API;
 * - `mIsLinearMotorVersion` (`VibrateUtils.isLinearMotorVersion`) and `mIsSetPassword`, which only
 *   gate the haptics and are never drawn;
 * - `getEnterAnim`: the stagger of `initCellAnim` runs once on first composition instead of being
 *   handed back as an `AnimatorSet`;
 * - `dispatchHoverEvent` / `onHoverEvent`, the touch-exploration remapping;
 * - `clearPattern(boolean)`, `getFailAnimator` and `getSuccessAnimator`, which the report shows are
 *   deprecated no-ops.
 *
 * @param onPatternComplete Called with the pattern the user drew, as `row * 3 + column` indices, when
 *   the pointer is lifted over at least one cell (`OnPatternListener.onPatternDetected`). The grid
 *   keeps drawing that pattern until the next gesture starts, exactly like the native keeps
 *   `mPattern` until `clearPattern()`.
 * @param modifier The modifier to be applied to the grid.
 * @param mode What the drawn pattern means right now. `Wrong` and `FingerprintNoMatch` switch the
 *   path and the discs to [LockPatternColors.errorColor] and run the 1000ms flash of
 *   `startWrongAnimator` / `startFingerprintNoMatchAnimator`; the other three values use
 *   [LockPatternColors.successColor]. `Animate` replays [pattern] and ignores the pointer.
 * @param pattern The pattern [LockPatternDisplayMode.Animate] replays, one cell per
 *   [LockPatternViewDefaults.AnimateStepMillis]. The native takes it through
 *   `setPattern(DisplayMode, List<Cell>)`.
 * @param enabled Whether the grid reacts to pointers; `COUILockPatternView.onTouchEvent` returns
 *   false when `mInputEnabled` or `isEnabled()` is false.
 * @param stealthMode Whether the path and the activation animations are suppressed, leaving only the
 *   dots. `COUILockPatternView.mInStealthMode`.
 * @param colors The dot, path, error and success colours plus the ripple peak.
 * @param onPatternStart Called when the first cell of a gesture is grabbed
 *   (`OnPatternListener.onPatternStart`).
 * @param onPatternCellAdded Called with the pattern so far whenever a cell is added
 *   (`OnPatternListener.onPatternCellAdded`).
 * @param onPatternCleared Called when a gesture is cancelled or begins outside every cell
 *   (`OnPatternListener.onPatternCleared`).
 */
@Composable
fun LockPatternView(
    onPatternComplete: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
    mode: LockPatternDisplayMode = LockPatternDisplayMode.Correct,
    pattern: List<Int> = emptyList(),
    enabled: Boolean = true,
    stealthMode: Boolean = false,
    colors: LockPatternColors = LockPatternViewDefaults.colors(),
    onPatternStart: () -> Unit = {},
    onPatternCellAdded: (List<Int>) -> Unit = {},
    onPatternCleared: () -> Unit = {},
) {
    val currentOnPatternComplete by rememberUpdatedState(onPatternComplete)
    val currentOnPatternStart by rememberUpdatedState(onPatternStart)
    val currentOnPatternCellAdded by rememberUpdatedState(onPatternCellAdded)
    val currentOnPatternCleared by rememberUpdatedState(onPatternCleared)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val horizontalPaddingPx = with(density) { LockPatternViewDefaults.HorizontalPadding.toPx() }
    val verticalPaddingPx = with(density) { LockPatternViewDefaults.VerticalPadding.toPx() }
    val maxTranslateYPx = with(density) { LockPatternViewDefaults.MaxTranslateY.toPx() }

    val cells = remember(maxTranslateYPx) { List(LockPatternViewDefaults.CellCount) { LockPatternCell(maxTranslateYPx) } }
    val lookup = remember { mutableStateListOf<Boolean>().apply { repeat(9) { add(false) } } }
    val drawn = remember { mutableStateListOf<Int>() }
    var finger by remember { mutableStateOf<Offset?>(null) }
    var inProgress by remember { mutableStateOf(false) }
    var nodeSize by remember { mutableStateOf(IntSize.Zero) }
    var flashProgress by remember { mutableStateOf<Float?>(null) }
    var completedPattern by remember { mutableStateOf<List<Int>?>(null) }
    var flashJob by remember { mutableStateOf<Job?>(null) }
    var animateCount by remember { mutableIntStateOf(0) }
    val geometry = remember(nodeSize, horizontalPaddingPx, verticalPaddingPx) {
        if (nodeSize == IntSize.Zero) {
            null
        } else {
            LockPatternGeometry(nodeSize, horizontalPaddingPx, verticalPaddingPx)
        }
    }

    fun cancelFlash() {
        flashJob?.cancel()
        flashJob = null
        flashProgress = null
    }

    fun startCellActivation(index: Int) {
        val target = geometry?.center(index) ?: return
        val cell = cells[index]
        scope.launch {
            cell.outerCircleProgress.snapTo(0f)
            cell.outerCircleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = LockPatternViewDefaults.OuterCircleDurationMillis,
                    easing = COUIInEase,
                ),
            )
        }
        scope.launch {
            cell.innerCircleAlpha.snapTo(0f)
            cell.innerCircleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = LockPatternViewDefaults.InnerCircleDurationMillis,
                    easing = LockPatternEase,
                ),
            )
        }
        // startLineEndAnimation: the segment end travels from the pointer to the cell centre over
        // 100ms on the framework's fast_out_slow_in curve.
        val start = finger ?: target
        cell.lineAnimation?.cancel()
        cell.lineEnd = start
        cell.lineAnimation = scope.launch {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = LockPatternViewDefaults.LineEndDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            ) { value, _ ->
                cell.lineEnd = Offset(
                    x = start.x + (target.x - start.x) * value,
                    y = start.y + (target.y - start.y) * value,
                )
            }
        }
    }

    fun addCell(index: Int) {
        if (lookup[index]) return
        lookup[index] = true
        drawn.add(index)
        if (!stealthMode) startCellActivation(index)
        currentOnPatternCellAdded(drawn.toList())
    }

    fun resetPattern() {
        drawn.clear()
        for (index in 0 until LockPatternViewDefaults.CellCount) lookup[index] = false
        cells.forEach { cell ->
            cell.lineAnimation?.cancel()
            cell.lineAnimation = null
            cell.lineEnd = null
        }
    }

    /** `detectAndAddHit`: returns true when a new cell was added. */
    fun detectAndAddHit(position: Offset): Boolean {
        val geo = geometry ?: return false
        val row = geo.rowHit(position.y)
        val column = geo.columnHit(position.x)
        if (row < 0 || column < 0) return false
        val hit = row * 3 + column
        if (lookup[hit]) return false
        if (drawn.isNotEmpty()) {
            val lastRow = drawn.last() / 3
            val lastColumn = drawn.last() % 3
            val rowDelta = row - lastRow
            val columnDelta = column - lastColumn
            var middleRow = lastRow
            var middleColumn = lastColumn
            if (abs(rowDelta) == 2 && abs(columnDelta) != 1) {
                middleRow += if (rowDelta > 0) 1 else -1
            }
            if (abs(columnDelta) == 2 && abs(rowDelta) != 1) {
                middleColumn += if (columnDelta <= 0) -1 else 1
            }
            val middle = middleRow * 3 + middleColumn
            if (!lookup[middle]) addCell(middle)
        }
        addCell(hit)
        return true
    }

    fun handleDown(position: Offset) {
        cancelFlash()
        resetPattern()
        val hit = detectAndAddHit(position)
        if (hit) {
            inProgress = true
            currentOnPatternStart()
        } else if (inProgress) {
            inProgress = false
            currentOnPatternCleared()
        }
        finger = position
    }

    fun handleMove(position: Offset) {
        val empty = drawn.isEmpty()
        val hit = detectAndAddHit(position)
        if (hit && empty && drawn.size == 1) {
            inProgress = true
            currentOnPatternStart()
        }
        finger = position
    }

    fun handleUp() {
        if (drawn.isEmpty()) return
        inProgress = false
        // cancelLineAnimations: the segment ends snap back onto the cell centres.
        cells.forEach { cell ->
            cell.lineAnimation?.cancel()
            cell.lineAnimation = null
            cell.lineEnd = null
        }
        completedPattern = drawn.toList()
        currentOnPatternComplete(drawn.toList())
    }

    fun handleCancel() {
        if (!inProgress) return
        inProgress = false
        resetPattern()
        currentOnPatternCleared()
    }

    // COUILockPatternView.getEnterAnim: cell i starts its 500ms translateY at i * 16ms and its
    // 167ms alpha at 166ms + i * 16ms, so the nine dots rise and fade in one after another.
    LaunchedEffect(maxTranslateYPx) {
        repeat(LockPatternViewDefaults.CellCount) { index ->
            launch {
                delay(LockPatternViewDefaults.EnterStaggerMillis * index)
                animate(
                    initialValue = maxTranslateYPx,
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = LockPatternViewDefaults.EnterTranslateDurationMillis,
                        easing = COUIInEase,
                    ),
                ) { value, _ -> cells[index].enterTranslateY = value }
            }
            launch {
                delay(LockPatternViewDefaults.EnterAlphaDelayMillis + LockPatternViewDefaults.EnterStaggerMillis * index)
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = LockPatternViewDefaults.EnterAlphaDurationMillis,
                        easing = LockPatternEase,
                    ),
                ) { value, _ -> cells[index].enterAlpha = value }
            }
        }
    }

    // startWrongAnimator walks "pathAlpha" through its keyframes on the cells of the pattern;
    // startFingerprintNoMatchAnimator runs the identical curve on all nine.
    LaunchedEffect(mode, completedPattern) {
        if (mode != LockPatternDisplayMode.Wrong && mode != LockPatternDisplayMode.FingerprintNoMatch) {
            cancelFlash()
            return@LaunchedEffect
        }
        flashJob?.cancel()
        flashProgress = 0f
        flashJob = scope.launch {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = LockPatternViewDefaults.WrongFlashDurationMillis,
                    easing = LinearEasing,
                ),
            ) { value, _ -> flashProgress = value }
            flashProgress = null
        }
    }

    // DisplayMode.Animate: one cell every 700ms, with the segment to the next cell growing over the
    // step — the mAnimatingPeriodStart arithmetic of onDraw.
    LaunchedEffect(mode, pattern, geometry) {
        if (mode != LockPatternDisplayMode.Animate || pattern.isEmpty()) {
            animateCount = 0
            return@LaunchedEffect
        }
        val step = LockPatternViewDefaults.AnimateStepMillis
        val period = (pattern.size + 1) * step
        val start = withFrameNanos { it }
        while (true) {
            val elapsed = (withFrameNanos { it } - start) / 1_000_000L
            val count = ((elapsed % period) / step).toInt()
            animateCount = count
            if (count in 1 until pattern.size) {
                val fraction = (elapsed % step).toFloat() / step
                val from = geometry?.center(pattern[count - 1])
                val to = geometry?.center(pattern[count])
                finger = if (from != null && to != null) {
                    Offset(from.x + (to.x - from.x) * fraction, from.y + (to.y - from.y) * fraction)
                } else {
                    from
                }
            } else if (count >= pattern.size) {
                finger = geometry?.center(pattern.last())
            }
        }
    }

    Box(
        modifier = modifier
            .size(LockPatternViewDefaults.Width, LockPatternViewDefaults.Height)
            .clipToBounds()
            .onSizeChanged { nodeSize = it }
            .pointerInput(enabled, geometry, stealthMode) {
                if (!enabled || geometry == null) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    handleDown(down.position)
                    down.consume()
                    var lifted = false
                    while (!lifted) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.pressed) {
                            handleMove(change.position)
                            change.consume()
                        } else {
                            handleUp()
                            change.consume()
                            lifted = true
                        }
                    }
                    if (!lifted) handleCancel()
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val geo = geometry ?: return@Canvas
            val animating = mode == LockPatternDisplayMode.Animate
            val flash = flashProgress
            val modeColor = when (mode) {
                LockPatternDisplayMode.Wrong,
                LockPatternDisplayMode.FingerprintNoMatch,
                -> colors.errorColor

                LockPatternDisplayMode.Correct,
                LockPatternDisplayMode.Animate,
                LockPatternDisplayMode.FingerprintMatch,
                -> colors.successColor
            }
            val currentColor = if (inProgress) colors.dotColor else modeColor
            val pathAlpha = if (flash != null) LockPatternViewDefaults.flashAlpha(flash) else 1f

            if (!stealthMode) {
                val path = Path()
                var last: Offset? = null
                var drew = false
                for (index in drawn) {
                    val visible = if (animating) {
                        pattern.indexOf(index).let { it in 0 until animateCount }
                    } else {
                        lookup[index]
                    }
                    if (!visible) break
                    val center = geo.center(index)
                    if (!drew) {
                        path.moveTo(center.x, center.y)
                    } else {
                        val lineEnd = cells[index].lineEnd
                        if (lineEnd != null) path.lineTo(lineEnd.x, lineEnd.y) else path.lineTo(center.x, center.y)
                    }
                    last = center
                    drew = true
                }
                val head = finger
                if ((inProgress || animating) && drew && last != null && head != null) {
                    path.moveTo(last.x, last.y)
                    path.lineTo(head.x, head.y)
                }
                drawPath(
                    path = path,
                    // setAlpha() replaces the colour's own alpha, so the path is opaque.
                    color = currentColor.copy(alpha = pathAlpha),
                    style = Stroke(
                        width = LockPatternViewDefaults.PathWidth.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }

            for (index in 0 until LockPatternViewDefaults.CellCount) {
                val cell = cells[index]
                val center = geo.center(index)
                val shifted = Offset(center.x, center.y + cell.enterTranslateY)
                val activated = if (animating) {
                    pattern.indexOf(index).let { it in 0 until animateCount }
                } else {
                    lookup[index]
                }
                val flashApplies = flash != null &&
                    (mode == LockPatternDisplayMode.FingerprintNoMatch || drawn.contains(index))
                val flashValue = if (flash != null) LockPatternViewDefaults.flashAlpha(flash) else 1f
                val innerAlpha = if (flashApplies) flashValue else cell.innerCircleAlpha.value

                if (activated || mode == LockPatternDisplayMode.FingerprintNoMatch) {
                    if (innerAlpha > 0f) {
                        val innerRadius = LockPatternViewDefaults.InnerCircleRadius.toPx()
                        val glowRadius = LockPatternViewDefaults.InnerCircleGlowRadius.toPx()
                        val disc = currentColor.copy(alpha = currentColor.alpha * innerAlpha)
                        // coui_lock_pattern_inner_circle: an opaque disc out to 6.67dp and a soft tail
                        // that dies at 14.67dp, drawn as one radial gradient.
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0f to disc,
                                    LockPatternViewDefaults.InnerCircleSolidFraction to disc,
                                    LockPatternViewDefaults.InnerCircleEdgeFraction to disc.copy(
                                        alpha = disc.alpha * LockPatternViewDefaults.InnerCircleGlowAlpha,
                                    ),
                                    1f to disc.copy(alpha = 0f),
                                ),
                                center = shifted,
                                radius = glowRadius,
                            ),
                            radius = glowRadius,
                            center = shifted,
                        )
                        drawCircle(color = disc, radius = innerRadius, center = shifted)
                    }
                    val outerProgress = cell.outerCircleProgress.value
                    if (outerProgress > 0f) {
                        val outerRadius = LockPatternViewDefaults.OuterCircleRadius.toPx() *
                            (1f + (LockPatternViewDefaults.OuterCircleScale - 1f) * outerProgress)
                        drawCircle(
                            color = currentColor.copy(
                                alpha = colors.outerCircleMaxAlpha *
                                    LockPatternViewDefaults.rippleAlpha(outerProgress),
                            ),
                            radius = outerRadius,
                            center = shifted,
                        )
                    }
                }

                // CellState.needDrawCircle, which the two flash animators clear while the disc is
                // still more than ALPHA_MIN_VISIBLE_CHANGE (0.1) opaque.
                val needDrawCircle = if (flashApplies) flashValue <= 0.1f else true
                if (needDrawCircle) {
                    drawCircle(
                        color = colors.dotColor.copy(alpha = colors.dotColor.alpha * cell.enterAlpha),
                        radius = LockPatternViewDefaults.DotSize.toPx() / 2f,
                        center = shifted,
                    )
                }
            }
        }
    }
}

/** What a [LockPatternView] is currently showing, mirroring `COUILockPatternView.DisplayMode`. */
enum class LockPatternDisplayMode {
    /** The resting state; the pattern is drawn in the success colour. `DisplayMode.Correct`. */
    Correct,

    /**
     * Replays [LockPatternView.pattern] one cell per [LockPatternViewDefaults.AnimateStepMillis].
     * `DisplayMode.Animate`.
     */
    Animate,

    /** A rejected pattern: error colour plus the 1000ms flash. `DisplayMode.Wrong`. */
    Wrong,

    /** A matched fingerprint: the pattern stays in the success colour. `DisplayMode.FingerprintMatch`. */
    FingerprintMatch,

    /** A rejected fingerprint: error colour, with the flash on all nine cells. `DisplayMode.FingerprintNoMatch`. */
    FingerprintNoMatch,
}

/**
 * The colours of a [LockPatternView] — the five attributes `Widget.COUI.COUILockPatternView`
 * resolves through `R.styleable.COUILockPatternView`.
 *
 * @param dotColor The unactivated dot. `couiRegularColor` -> `coui_lock_pattern_dot_color`.
 * @param pathColor The connected path. `couiPathColor` -> `coui_lock_pattern_path_color`.
 * @param errorColor The wrong-pattern colour. `couiErrorColor` -> `coui_lock_pattern_error_color`.
 * @param successColor The correct-pattern colour. `couiSuccessColor` -> `coui_lock_pattern_success_color`.
 * @param outerCircleMaxAlpha The peak alpha of the activation ripple. `couiOuterCircleMaxAlpha` ->
 *   `coui_lock_pattern_outer_circle_max_alpha`.
 */
@Immutable
data class LockPatternColors(
    val dotColor: Color,
    val pathColor: Color,
    val errorColor: Color,
    val successColor: Color,
    val outerCircleMaxAlpha: Float,
)

/** Contains the default values used by [LockPatternView]. */
object LockPatternViewDefaults {

    /** The number of cells, `3 * 3`. `COUILockPatternUtils.ROWS` and `COLUMNS` are both 3. */
    val CellCount = 9

    /** The default width. COUI `coui_lock_pattern_view_width`, 312dp. */
    val Width = 312.dp

    /** The default height. COUI `coui_lock_pattern_view_height`, 280dp. */
    val Height = 280.dp

    /**
     * The horizontal padding, read into the view as `android:paddingStart` / `android:paddingEnd`.
     * COUI `coui_lock_pattern_view_horizontal_padding`, 21dp.
     */
    val HorizontalPadding = 21.dp

    /** The vertical padding, read as `android:paddingTop` / `android:paddingBottom`. 5dp. */
    val VerticalPadding = 5.dp

    /**
     * The diameter of an unactivated dot. COUI `lock_pattern_dot_size`, halved into
     * `CellState.radius`, 10dp.
     */
    val DotSize = 10.dp

    /**
     * The width of the connected path. COUI `lock_pattern_dot_line_width`, set on `mPathPaint` with
     * `Paint.Join.ROUND` and `Paint.Cap.ROUND`, 2dp.
     */
    val PathWidth = 2.dp

    /**
     * The share of a cell a pointer must be inside to grab it. `COUILockPatternView.mHitFactor`,
     * 0.6: the hit rect is the middle 60% of the 90dp cell, i.e. 54dp.
     */
    val HitFactor = 0.6f

    /**
     * The radius of the filled disc an activated cell draws. Not a resource: it is the opaque area of
     * `drawable/coui_lock_pattern_inner_circle`, a 90px x 90px (30dp at xxhdpi) grey+alpha PNG whose
     * disc ends at 20/90 of its width, i.e. 6.67dp.
     */
    val InnerCircleRadius = 6.67.dp

    /**
     * The radius the soft tail of `coui_lock_pattern_inner_circle` reaches: 44/90 of the drawable's
     * 30dp width, i.e. 14.67dp. Measured from that PNG, which is what the port approximates with a
     * radial gradient.
     */
    val InnerCircleGlowRadius = 14.67.dp

    /** Where the opaque part of the inner-circle gradient stops, `20 / 44` of its radius. */
    val InnerCircleSolidFraction = 0.4545f

    /** Where the gradient drops to its tail, `21 / 44` of its radius. */
    val InnerCircleEdgeFraction = 0.4773f

    /** The tail's peak alpha, 48/255, measured from `coui_lock_pattern_inner_circle`. */
    val InnerCircleGlowAlpha = 0.19f

    /**
     * The radius of the ripple before it is scaled. `drawable/coui_lock_pattern_outer_circle` is the
     * same 30dp PNG with a disc ending at 10.5/90 of its width, i.e. 3.5dp.
     */
    val OuterCircleRadius = 3.5.dp

    /**
     * The scale the ripple grows to. `startOuterAnimation` animates `outerCircleScale` over
     * `ValueAnimator.ofFloat(1.0f, 7.0f)`.
     */
    val OuterCircleScale = 7f

    /**
     * The duration of the fill fade-in. `startInnerAnimation`, 230ms on `COUIEaseInterpolator`.
     */
    val InnerCircleDurationMillis = 230

    /** The duration of the ripple. `startOuterAnimation`, 460ms on `COUIInEaseInterpolator`. */
    val OuterCircleDurationMillis = 460

    /**
     * The duration of the line-end catch-up. `startLineEndAnimation` interpolates the segment end
     * from the pointer to the cell centre over 100ms on `fast_out_slow_in`.
     */
    val LineEndDurationMillis = 100

    /**
     * The duration of the wrong-pattern flash. `startWrongAnimator` and
     * `startFingerprintNoMatchAnimator` are both a 1000ms `PropertyValuesHolder` over `pathAlpha`.
     */
    val WrongFlashDurationMillis = 1000

    /**
     * How long [LockPatternDisplayMode.Animate] dwells on one cell.
     * `COUILockPatternView.MILLIS_PER_CIRCLE_ANIMATING`, 700ms.
     */
    val AnimateStepMillis = 700

    /**
     * The delay before a cell's enter alpha starts. `COUILockPatternView.ALPHA_DELAY`, 166ms, plus
     * `ALPHA_OFFSET` (16ms) per cell index.
     */
    val EnterAlphaDelayMillis = 166L

    /** The duration of a cell's enter alpha. `COUILockPatternView.ALPHA_DURATION`, 167ms. */
    val EnterAlphaDurationMillis = 167

    /** The stagger between two cells. `COUILockPatternView.ALPHA_OFFSET` / `TRANSLATE_Y_OFFSET`, 16ms. */
    val EnterStaggerMillis = 16L

    /** The duration of a cell's enter translateY. `COUILockPatternView.TRANSLATE_Y_DURATION`, 500ms. */
    val EnterTranslateDurationMillis = 500

    /**
     * How far below its final place a cell starts. COUI `color_lock_pattern_view_max_translate_y`,
     * which the constructor reads into `mMaxTranslateY`, 258dp.
     */
    val MaxTranslateY = 258.dp

    /** The dot colour of the light style. COUI `coui_lock_pattern_dot_color`, `#4d000000`. */
    val DotColor = Color(0x4D000000)

    /** The dot colour of the dark style. COUI `coui_lock_pattern_dot_dark_color`, `#66ffffff`. */
    val DotColorDark = Color(0x66FFFFFF)

    /** The path colour of the light style. COUI `coui_lock_pattern_path_color`, `#ff000000`. */
    val PathColor = Color(0xFF000000)

    /** The path colour of the dark style. COUI `coui_lock_pattern_path_dark_color`, `#ffffffff`. */
    val PathColorDark = Color(0xFFFFFFFF)

    /**
     * The wrong-pattern colour. COUI `coui_lock_pattern_error_color`, `#ffea3447` — the `_light_`
     * and `_dark_` variants carry the same value, so both styles share it.
     */
    val ErrorColor = Color(0xFFEA3447)

    /** The success colour of the light style. COUI `coui_lock_pattern_success_color`, `#ff000000`. */
    val SuccessColor = Color(0xFF000000)

    /**
     * The success colour of the dark style. COUI `coui_lock_pattern_success_dark_color`, `#ffffffff`.
     */
    val SuccessColorDark = Color(0xFFFFFFFF)

    /** The launcher palette's dot colour. COUI `coui_lock_pattern_launcher_dot_color`, `#66ffffff`. */
    val LauncherDotColor = Color(0x66FFFFFF)

    /** The launcher palette's path colour. COUI `coui_lock_pattern_launcher_path_color`, `#ffffffff`. */
    val LauncherPathColor = Color(0xFFFFFFFF)

    /** The launcher palette's success colour. `coui_lock_pattern_launcher_success_color`, `#ffffffff`. */
    val LauncherSuccessColor = Color(0xFFFFFFFF)

    /** The launcher palette's error colour. COUI `coui_lock_pattern_launcher_error_color`, `#ffea3447`. */
    val LauncherErrorColor = Color(0xFFEA3447)

    /**
     * `coui_lock_pattern_launcher_gradient_color`, `#4dffffff`. Recovered but never read by
     * `COUILockPatternView`: no draw path in the class consumes a gradient for the launcher palette,
     * so the port only exposes the token.
     */
    val LauncherGradientColor = Color(0x4DFFFFFF)

    /**
     * The colours of the light style, or of the dark style when the theme is dark.
     *
     * The native selects `Widget.COUI.COUILockPatternView.Dark` through
     * `COUIContextUtil.isCOUIDarkTheme(context)`; the port reads the same signal from the theme
     * background's luminance, as the other components of this library do. The dark style's ripple
     * peak is `coui_lock_pattern_outer_circle_max_dark_alpha` (0.2), the light style's is
     * `coui_lock_pattern_outer_circle_max_alpha` (0.1 / 0.2 night).
     */
    @Composable
    fun colors(): LockPatternColors {
        val colorScheme = COUITheme.colorScheme
        return if (colorScheme.background.luminance() < 0.5f) {
            LockPatternColors(
                dotColor = DotColorDark,
                pathColor = PathColorDark,
                errorColor = ErrorColor,
                successColor = SuccessColorDark,
                outerCircleMaxAlpha = DarkOuterCircleMaxAlpha,
            )
        } else {
            LockPatternColors(
                dotColor = DotColor,
                pathColor = PathColor,
                errorColor = ErrorColor,
                successColor = SuccessColor,
                outerCircleMaxAlpha = LightOuterCircleMaxAlpha,
            )
        }
    }

    /**
     * The `coui_lock_pattern_launcher_*` palette, the fixed set of colours the launcher uses over a
     * wallpaper. It reuses the dark style's ripple peak, because the launcher palette has no ripple
     * attribute of its own.
     */
    fun launcherColors(): LockPatternColors = LockPatternColors(
        dotColor = LauncherDotColor,
        pathColor = LauncherPathColor,
        errorColor = LauncherErrorColor,
        successColor = LauncherSuccessColor,
        outerCircleMaxAlpha = DarkOuterCircleMaxAlpha,
    )

    /** `coui_lock_pattern_outer_circle_max_alpha` in the light style. */
    val LightOuterCircleMaxAlpha = 0.1f

    /** `coui_lock_pattern_outer_circle_max_dark_alpha`, the Dark style's ripple peak. */
    val DarkOuterCircleMaxAlpha = 0.2f

    /**
     * The `pathAlpha` curve of `startWrongAnimator` / `startFingerprintNoMatchAnimator`: keyframes at
     * fractions 0 / 0.2 / 0.4 / 0.6 / 0.8 / 1 with values 1 / 0.35 / 1 / 0.15 / 0.5 / 0, evaluated
     * piecewise-linearly.
     */
    fun flashAlpha(progress: Float): Float = piecewise(
        progress = progress,
        stops = floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f),
        values = floatArrayOf(1f, 0.35f, 1f, 0.15f, 0.5f, 0f),
    )

    /**
     * The `outerCircleAlpha` keyframes of `startOuterAnimation` — 0 at fraction 0, the peak at 0.5
     * and 0 again at 1 — read on the eased progress the ripple scale shares.
     */
    fun rippleAlpha(progress: Float): Float {
        val clamped = progress.coerceIn(0f, 1f)
        return if (clamped <= 0.5f) clamped * 2f else (1f - clamped) * 2f
    }

    private fun piecewise(progress: Float, stops: FloatArray, values: FloatArray): Float {
        val clamped = progress.coerceIn(0f, 1f)
        for (index in 0 until stops.lastIndex) {
            if (clamped <= stops[index + 1]) {
                val span = stops[index + 1] - stops[index]
                val fraction = if (span <= 0f) 1f else (clamped - stops[index]) / span
                return values[index] + (values[index + 1] - values[index]) * fraction
            }
        }
        return values.last()
    }
}

/** `COUIEaseInterpolator`: the `cubic-bezier(0.33, 0, 0.67, 1)` the native uses for its alphas. */
private val LockPatternEase: Easing = CubicBezierEasing(0.33f, 0f, 0.67f, 1f)

/**
 * The per-cell animation state of a [LockPatternView]: the two `Animatable`s behind
 * `CellState.innerCircleAlpha` / `outerCircleScale` / `outerCircleAlpha`, the animated line end of
 * `CellState.lineEndX` / `lineEndY`, and the two enter values of `initCellAnim`.
 *
 * @param initialTranslateY The `mMaxTranslateY` a cell starts at, i.e. `CellState.translationY`
 *   before `initCellAnim` moves it to 0.
 */
private class LockPatternCell(initialTranslateY: Float) {
    val innerCircleAlpha = Animatable(0f)
    val outerCircleProgress = Animatable(0f)
    var lineEnd by mutableStateOf<Offset?>(null)
    var lineAnimation: Job? = null
    var enterAlpha by mutableFloatStateOf(0f)
    var enterTranslateY by mutableFloatStateOf(initialTranslateY)
}

/**
 * The cell geometry of `COUILockPatternView`, i.e. `mSquareWidth` / `mSquareHeight` from
 * `onSizeChanged` and `getCenterXForColumn` / `getCenterYForRow` / `getColumnHit` / `getRowHit`.
 *
 * @param size The measured size of the grid.
 * @param paddingLeft The horizontal padding in pixels.
 * @param paddingTop The vertical padding in pixels.
 */
private class LockPatternGeometry(
    size: IntSize,
    private val paddingLeft: Float,
    private val paddingTop: Float,
) {
    private val squareWidth = ((size.width - paddingLeft * 2f) / 3f).coerceAtLeast(0f)
    private val squareHeight = ((size.height - paddingTop * 2f) / 3f).coerceAtLeast(0f)

    fun centerX(column: Int): Float = paddingLeft + column * squareWidth + squareWidth / 2f

    fun centerY(row: Int): Float = paddingTop + row * squareHeight + squareHeight / 2f

    fun center(index: Int): Offset = Offset(centerX(index % 3), centerY(index / 3))

    /** `getColumnHit`: the middle `mHitFactor` of a column, or -1 outside the grid. */
    fun columnHit(x: Float): Int = hitAt(x, squareWidth, paddingLeft)

    /** `getRowHit`: the middle `mHitFactor` of a row, or -1 outside the grid. */
    fun rowHit(y: Float): Int = hitAt(y, squareHeight, paddingTop)

    private fun hitAt(position: Float, square: Float, padding: Float): Int {
        val span = square * LockPatternViewDefaults.HitFactor
        val start = padding + (square - span) / 2f
        for (index in 0..2) {
            val left = index * square + start
            if (position >= left && position <= left + span) return index
        }
        return -1
    }
}
