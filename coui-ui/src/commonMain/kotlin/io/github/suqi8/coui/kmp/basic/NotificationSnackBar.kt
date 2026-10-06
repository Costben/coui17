// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Close
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The notification-style snackbar of ColorOS.
 *
 * Unlike the plain [Snackbar] it carries a leading notification image whose box follows the
 * native `COUINotificationSnackBar.ImageType`: a 44dp square, a 36x48 vertical rectangle or a
 * 58x44 horizontal rectangle. Next to the image sit a two-line title
 * (`couiTextAppearanceButton`, 14sp `sans-serif-medium`) and an optional two-line subtitle
 * (`couiTextAppearanceDescription`, 12sp, 2dp below the title), an optional trailing action
 * button (`Widget.COUI.Button.Small`) and an optional close icon pinned to the top-end corner.
 *
 * The container is the `?attr/couiColorBackgroundTop` card with the `couiRoundCornerL` (16dp)
 * corner radius and the `support_shadow_size_level_four` (24dp) elevation the native applies
 * through `ShadowUtils`; the port keeps the radius and drops the elevation to the caller's
 * modifier so it can be composed with the app's own shadow stack.
 *
 * Not ported, because no consumer in this library reaches them:
 * - the smooth-round corner of `OplusOutline.setSmoothRoundRect` (`couiRoundCornerLRadius` 13dp /
 *   `couiRoundCornerLWeight` 0.99) used when `RoundCornerUtil.isVersionSupport()` is true; the port
 *   draws the `RoundedCornerShape` fallback the native uses otherwise (`outline.setRoundRect` with
 *   `couiRoundCornerL`);
 * - the swipe-to-dismiss gesture of `onInterceptTouchEvent` / `onTouchEvent` (a swipe past 1/8 of the
 *   measured width dismisses along X, a downward swipe past 1/8 dismisses along Y, the cancel
 *   animation returns on the `COUISpringAnimation` response 0.35 / bounce 0 exposed as
 *   [NotificationSnackBarDefaults.ReturnSpring]);
 * - the "one snackbar at a time" bookkeeping of `make()`, which scans the parent for an already
 *   visible `COUISnackBar`; [NotificationSnackBarHostState] replaces the newest entry instead;
 * - the `ContextThemeWrapper(Theme.COUI_Main)` fallback of `make()` when the host theme does not
 *   resolve `couiColorBackgroundTop` / `couiColorPrimaryNeutral`.
 *
 * @param title The title of the snackbar, up to two lines.
 * @param modifier The modifier to be applied to the snackbar.
 * @param subtitle The optional secondary line, shown 2dp below the title.
 * @param icon The optional leading notification image, laid out in the [imageType] box.
 * @param imageType The box of [icon], one of the three `COUINotificationSnackBar.ImageType`
 *   geometries.
 * @param actionText The optional label of the trailing action button.
 * @param onAction The callback when the action button is clicked.
 * @param onClose The callback when the close icon is clicked. The icon is only shown when this is
 *   not `null`.
 * @param cornerRadius The corner radius of the container. COUI `couiRoundCornerL`
 *   ([NotificationSnackBarDefaults.CornerRadius]).
 * @param colors The [NotificationSnackBarColors] of the snackbar.
 */
@Composable
fun NotificationSnackBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    imageType: NotificationSnackBarImageType = NotificationSnackBarImageType.Square,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    cornerRadius: Dp = NotificationSnackBarDefaults.CornerRadius,
    colors: NotificationSnackBarColors = NotificationSnackBarDefaults.colors(),
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val closeSize = NotificationSnackBarDefaults.CloseSize
    Box(modifier = modifier.padding(end = NotificationSnackBarDefaults.LayoutMarginEnd)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = colors.containerColor,
            contentColor = colors.titleColor,
        ) {
            Row(
                modifier = Modifier.padding(
                    start = NotificationSnackBarDefaults.ContentMarginStart,
                    top = NotificationSnackBarDefaults.ContentVerticalPadding,
                    end = if (onClose != null) {
                        closeSize + NotificationSnackBarDefaults.CloseStartMargin
                    } else {
                        NotificationSnackBarDefaults.LayoutMarginEnd
                    },
                    bottom = NotificationSnackBarDefaults.ContentVerticalPadding,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.titleColor,
                        modifier = Modifier.size(
                            width = NotificationSnackBarDefaults.iconWidth(imageType),
                            height = NotificationSnackBarDefaults.iconHeight(imageType),
                        ),
                    )
                    Spacer(modifier = Modifier.width(NotificationSnackBarDefaults.IconEndPadding))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = colors.titleColor,
                        style = NotificationSnackBarDefaults.titleStyle(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = colors.subtitleColor,
                            style = NotificationSnackBarDefaults.subtitleStyle(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(
                                top = NotificationSnackBarDefaults.SubContentTopPadding,
                            ),
                        )
                    }
                }
                if (actionText != null) {
                    Text(
                        text = actionText,
                        color = colors.actionColor,
                        style = NotificationSnackBarDefaults.actionStyle(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = NotificationSnackBarDefaults.ActionStartMargin)
                            .widthIn(
                                min = NotificationSnackBarDefaults.ActionMinWidth,
                                max = NotificationSnackBarDefaults.ActionMaxWidth,
                            )
                            .heightIn(min = NotificationSnackBarDefaults.ActionMinHeight)
                            // Widget.COUI.Button.Small paddingLeft/Right 12dp, top/bottom 4dp, with the
                            // capsule press mask COUIMaskRippleDrawable gives a text button.
                            .clip(CircleShape)
                            .clickable(enabled = onAction != null) { onAction?.invoke() }
                            .padding(NotificationSnackBarDefaults.ActionPadding),
                    )
                }
            }
        }
        if (onClose != null) {
            Icon(
                imageVector = COUIIcons.Basic.Close,
                contentDescription = null,
                tint = colors.closeIconColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        start = NotificationSnackBarDefaults.CloseStartMargin,
                        top = NotificationSnackBarDefaults.CloseTopMargin,
                    )
                    .size(closeSize)
                    .clip(CircleShape)
                    .clickable { onClose() }
                    // `coui_notification_snack_bar_close_padding` (2dp) insets the close drawable.
                    .padding(NotificationSnackBarDefaults.ClosePadding),
            )
        }
    }
}

/**
 * The three notification image geometries of `COUINotificationSnackBar.ImageType`.
 */
enum class NotificationSnackBarImageType {
    /** `ImageType.Square` (0): a square box. COUI `coui_notification_snack_bar_icon_width`. */
    Square,

    /** `ImageType.Vertical` (1): a portrait box. COUI `coui_notification_snack_bar_vertical_icon_*`. */
    Vertical,

    /** `ImageType.Horizontal` (2): a landscape box. COUI `coui_notification_snack_bar_horizontal_icon_*`. */
    Horizontal,
}

/**
 * Colors used by a [NotificationSnackBar].
 *
 * @param containerColor The card fill. COUI `?attr/couiColorBackgroundTop`.
 * @param titleColor The title colour. COUI `?attr/couiColorPrimaryNeutral`.
 * @param subtitleColor The subtitle colour. COUI `?attr/couiColorSecondNeutral`.
 * @param actionColor The action label colour. COUI `color/coui_snack_bar_action_text_color`.
 * @param closeIconColor The close icon tint. COUI `?attr/couiColorLabelTertiary`.
 */
@Immutable
data class NotificationSnackBarColors(
    val containerColor: Color,
    val titleColor: Color,
    val subtitleColor: Color,
    val actionColor: Color,
    val closeIconColor: Color,
)

/** Contains default values used by [NotificationSnackBar]. */
object NotificationSnackBarDefaults {

    /** The corner radius of the container. COUI `couiRoundCornerL` (`coui_round_corner_l`, 16dp). */
    val CornerRadius = 16.dp

    /**
     * The start inset of the notification image. COUI `coui_snack_bar_context_margin_start_with_icon`
     * (16dp).
     */
    val ContentMarginStart = 16.dp

    /**
     * The top / bottom inset of the content row. COUI `coui_snack_bar_icon_margin_top_horizontal`
     * (16dp), which the item layout gives both image slots.
     */
    val ContentVerticalPadding = 16.dp

    /**
     * The end margin of the container. COUI `coui_notification_snack_bar_layout_margin_end` (6dp).
     */
    val LayoutMarginEnd = 6.dp

    /** The gap between the notification image and the text column. COUI `coui_notification_snack_bar_icon_end` (8dp). */
    val IconEndPadding = 8.dp

    /** The top margin of the subtitle. COUI `coui_notification_snack_bar_sub_content_top` (2dp). */
    val SubContentTopPadding = 2.dp

    /** The width of the close icon. COUI `coui_notification_snack_bar_close_width` (20dp). */
    val CloseSize = 20.dp

    /** The padding inside the close icon. COUI `coui_notification_snack_bar_close_padding` (2dp). */
    val ClosePadding = 2.dp

    /** The start margin of the close icon. COUI `coui_notification_snack_bar_close_start` (2dp). */
    val CloseStartMargin = 2.dp

    /** The end margin of the close icon. COUI `coui_notification_snack_bar_close_end` (6dp). */
    val CloseEndMargin = 6.dp

    /** The top margin of the close icon. COUI `coui_notification_snack_bar_close_top` (6dp). */
    val CloseTopMargin = 6.dp

    /** The maximum width of the action button. COUI `coui_snack_bar_button_width` (120dp). */
    val ActionMaxWidth = 120.dp

    /** The minimum width of the action button. COUI `coui_btn_small_width_min` (52dp). */
    val ActionMinWidth = 52.dp

    /** The minimum height of the action button. COUI `coui_btn_small_height_min` (28dp). */
    val ActionMinHeight = 28.dp

    /**
     * The start margin of the action button. COUI `coui_snack_bar_action_margin_horizontal_start`
     * (12dp).
     */
    val ActionStartMargin = 12.dp

    /** The padding of the action button. `Widget.COUI.Button.Small` `paddingLeft/Right` 12dp, `paddingTop/Bottom` 4dp. */
    val ActionPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)

    /** The bottom margin of the snackbar. COUI `coui_snack_bar_margin_bottom` (32dp). */
    val BottomMargin = 32.dp

    /**
     * The shortest display time. `COUINotificationSnackBar.MIN_DURATION` (4000ms); `make()` clamps
     * the requested duration into `[MIN_DURATION, MAX_DURATION]`.
     */
    val MinDurationMillis = 4000L

    /** The longest display time. `COUINotificationSnackBar.MAX_DURATION` (10000ms). */
    val MaxDurationMillis = 10000L

    /**
     * The swipe-cancel spring. `COUINotificationSnackBar.getViewSpringTranslationAnimator` builds a
     * `COUISpringAnimation` with `bounce` 0 and `response` 0.35.
     */
    val ReturnSpring: SpringSpec<Float> = folmeSpring(damping = 1f, response = 0.35f)

    /**
     * The width of the notification image box for [type]. COUI
     * `coui_notification_snack_bar_icon_width` (44dp, the square),
     * `coui_notification_snack_bar_vertical_icon_width` (36dp) and
     * `coui_notification_snack_bar_horizontal_icon_width` (58dp).
     */
    fun iconWidth(type: NotificationSnackBarImageType): Dp = when (type) {
        NotificationSnackBarImageType.Square -> 44.dp
        NotificationSnackBarImageType.Vertical -> 36.dp
        NotificationSnackBarImageType.Horizontal -> 58.dp
    }

    /**
     * The height of the notification image box for [type]. COUI
     * `coui_notification_snack_bar_vertical_icon_height` (48dp) and
     * `coui_notification_snack_bar_horizontal_icon_height` (44dp); the square shares the 44dp
     * `coui_notification_snack_bar_icon_width`.
     */
    fun iconHeight(type: NotificationSnackBarImageType): Dp = when (type) {
        NotificationSnackBarImageType.Square -> 44.dp
        NotificationSnackBarImageType.Vertical -> 48.dp
        NotificationSnackBarImageType.Horizontal -> 44.dp
    }

    /**
     * The title style. `coui_notification_snack_bar_item.xml` gives `tv_snack_bar_content`
     * `android:textAppearance="@style/couiTextAppearanceButton"` (14sp, `sans-serif-medium`), the
     * same tier [SnackbarDefaults.textStyle] uses.
     */
    @Composable
    fun titleStyle(): TextStyle = COUITheme.textStyles.body2.copy(fontWeight = FontWeight.Medium)

    /**
     * The subtitle style. `tv_snack_bar_sub_content` uses `?attr/couiTextAppearanceDescription`
     * (`style/couiTextAppearanceDescription`, 12sp); the theme's closest tier is
     * `COUITheme.textStyles.footnote2`.
     */
    @Composable
    fun subtitleStyle(): TextStyle = COUITheme.textStyles.footnote2

    /**
     * The action label style. `bt_notification_snack_bar` uses `style/Widget.COUI.Button.Small`,
     * whose `android:textSize` is `coui_btn_small_text_size` (14sp) with `sans-serif-medium`.
     */
    @Composable
    fun actionStyle(): TextStyle = COUITheme.textStyles.body2.copy(fontWeight = FontWeight.Medium)

    /**
     * The default [NotificationSnackBarColors].
     *
     * The title and subtitle map onto the theme's primary / secondary label slots exactly
     * (`couiColorPrimaryNeutral` `#e6000000` / `#e6ffffff` is `onSurface`, `couiColorSecondNeutral`
     * `#8a000000` / `#8affffff` is `onSurfaceSecondary`). The action label keeps the native
     * `coui_snack_bar_action_text_enable` white (`#ffffffff`) as a file constant; the close icon
     * uses `?attr/couiColorLabelTertiary` (`onSurfaceVariantActions`). The container follows
     * `?attr/couiColorBackgroundTop` (`#ffffffff` light / `#ff333333` dark); the nearest theme slot
     * is `COUITheme.colorScheme.surfaceContainerHighest`, which is exact in light and `#ff1e1e1e`
     * rather than `#ff333333` in dark.
     */
    @Composable
    fun colors(
        containerColor: Color = COUITheme.colorScheme.surfaceContainerHighest,
        titleColor: Color = COUITheme.colorScheme.onSurface,
        subtitleColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
        actionColor: Color = SnackBarActionTextEnable,
        closeIconColor: Color = COUITheme.colorScheme.onSurfaceVariantActions,
    ): NotificationSnackBarColors = remember(
        containerColor,
        titleColor,
        subtitleColor,
        actionColor,
        closeIconColor,
    ) {
        NotificationSnackBarColors(
            containerColor = containerColor,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
            actionColor = actionColor,
            closeIconColor = closeIconColor,
        )
    }
}

/** `color/coui_snack_bar_action_text_enable` (`#ffffffff`), the enabled action label colour. */
private val SnackBarActionTextEnable = Color(0xFFFFFFFF)

/**
 * Possible results of a [NotificationSnackBarHostState.showNotification] call.
 */
enum class NotificationSnackBarResult {
    /** The snackbar timed out or its close icon was clicked. */
    Dismissed,

    /** The snackbar's action button was clicked. */
    ActionPerformed,
}

/**
 * The visuals of one [NotificationSnackBar].
 *
 * @param title The title of the snackbar.
 * @param subtitle The optional secondary line.
 * @param icon The optional leading notification image.
 * @param imageType The box of [icon].
 * @param actionText The optional label of the trailing action button.
 * @param durationMillis The display time, already clamped to
 *   `[NotificationSnackBarDefaults.MinDurationMillis, NotificationSnackBarDefaults.MaxDurationMillis]`.
 */
@Immutable
data class NotificationSnackBarVisuals(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val imageType: NotificationSnackBarImageType = NotificationSnackBarImageType.Square,
    val actionText: String? = null,
    val durationMillis: Long = NotificationSnackBarDefaults.MinDurationMillis,
)

/**
 * State of a [NotificationSnackBarHost].
 *
 * It replaces the "scan the parent for a visible `COUISnackBar`" bookkeeping of
 * `COUINotificationSnackBar.make()` with a single slot: showing a second snackbar dismisses the
 * first one as [NotificationSnackBarResult.Dismissed].
 */
@Stable
class NotificationSnackBarHostState {
    internal var visuals by mutableStateOf<NotificationSnackBarVisuals?>(null)
        private set

    internal var visible by mutableStateOf(false)
        private set

    internal var onAction: (() -> Unit)? = null
        private set

    internal var onClose: (() -> Unit)? = null
        private set

    internal var notificationId by mutableLongStateOf(0L)
        private set

    private val mutex = Mutex()
    private var pending: CompletableDeferred<NotificationSnackBarResult>? = null

    /**
     * Shows a [NotificationSnackBar] and suspends until it is dismissed or its action is performed.
     *
     * @param title The title of the snackbar.
     * @param subtitle The optional secondary line.
     * @param icon The optional leading notification image.
     * @param imageType The box of [icon].
     * @param actionText The optional label of the trailing action button.
     * @param durationMillis The display time; clamped into
     *   `[NotificationSnackBarDefaults.MinDurationMillis, NotificationSnackBarDefaults.MaxDurationMillis]`
     *   like `COUINotificationSnackBar.make()`.
     * @param onAction The callback when the action button is clicked.
     * @param onClose The callback when the close icon is clicked.
     * @return the [NotificationSnackBarResult] of the shown snackbar.
     */
    suspend fun showNotification(
        title: String,
        subtitle: String? = null,
        icon: ImageVector? = null,
        imageType: NotificationSnackBarImageType = NotificationSnackBarImageType.Square,
        actionText: String? = null,
        durationMillis: Long = NotificationSnackBarDefaults.MinDurationMillis,
        onAction: (() -> Unit)? = null,
        onClose: (() -> Unit)? = null,
    ): NotificationSnackBarResult {
        val result = CompletableDeferred<NotificationSnackBarResult>()
        mutex.withLock {
            pending?.complete(NotificationSnackBarResult.Dismissed)
            notificationId += 1
            this.onAction = onAction
            this.onClose = onClose
            visuals = NotificationSnackBarVisuals(
                title = title,
                subtitle = subtitle,
                icon = icon,
                imageType = imageType,
                actionText = actionText,
                durationMillis = durationMillis.coerceIn(
                    NotificationSnackBarDefaults.MinDurationMillis,
                    NotificationSnackBarDefaults.MaxDurationMillis,
                ),
            )
            visible = true
            pending = result
        }
        return result.await()
    }

    internal suspend fun dismiss(result: NotificationSnackBarResult) {
        mutex.withLock {
            if (!visible) return
            visible = false
            val current = pending
            pending = null
            if (current != null && !current.isCompleted) current.complete(result)
        }
    }
}

/**
 * Host for [NotificationSnackBar]s to be shown, the notification twin of [SnackbarHost].
 *
 * @param state The state of the host.
 * @param modifier The modifier to be applied to the host.
 */
@Composable
fun NotificationSnackBarHost(
    state: NotificationSnackBarHostState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val visuals = state.visuals
    val visible = state.visible
    val onAction = state.onAction
    val onClose = state.onClose

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = NotificationSnackBarDefaults.BottomMargin),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { height -> height },
            exit = fadeOut() + slideOutVertically { height -> height },
        ) {
            if (visuals != null) {
                NotificationSnackBar(
                    title = visuals.title,
                    subtitle = visuals.subtitle,
                    icon = visuals.icon,
                    imageType = visuals.imageType,
                    actionText = visuals.actionText,
                    onAction = onAction?.let {
                        {
                            it()
                            scope.launch { state.dismiss(NotificationSnackBarResult.ActionPerformed) }
                        }
                    },
                    onClose = onClose?.let {
                        {
                            it()
                            scope.launch { state.dismiss(NotificationSnackBarResult.Dismissed) }
                        }
                    } ?: {
                        scope.launch { state.dismiss(NotificationSnackBarResult.Dismissed) }
                    },
                )
            }
        }
    }

    LaunchedEffect(state.notificationId) {
        val current = state.visuals ?: return@LaunchedEffect
        delay(current.durationMillis)
        state.dismiss(NotificationSnackBarResult.Dismissed)
    }
}
