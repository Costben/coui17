// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * The ColorOS 17 expandable list: a scrollable list of groups, each of which grows open to reveal its
 * children and shrinks closed again.
 *
 * A recycler-view connector does the work, wrapping a standard expandable list view:
 * - the group header is supplied by the caller; clicking it toggles the group, which plays the
 *   system click sound and then expands / collapses it;
 * - the open/close animation runs for [ExpandableListDefaults.ANIMATION_DURATION] (400ms) on a
 *   cubic-bezier(0.3, 0, 0.1, 1) move-ease passed to every `ExpandAnimator`;
 * - the connector animates the *height* of a dummy view that carries the group's children
 *   (`view.getLayoutParams().height = animatedValue`) and sets its alpha to
 *   `animatedValue / groupInfo.totalHeight`, so the children grow and fade in together — which is
 *   what a height-tweened [expandVertically] paired with a [fadeIn] over the same duration does here;
 * - a group can only be open once, and [maxExpandedGroupCount] caps how many are open at a time
 *   (`ExpandableRecyclerConnector.setMaxExpGroupCount`). The connector's default is
 *   [ExpandableListDefaults.MAX_EXPANDED_GROUP_COUNT], `Integer.MAX_VALUE`, i.e. no cap.
 *
 * The list is not `wrap_content`: `COUIExpandableListView.setLayoutParams` throws
 * `"cannot set wrap_content"` for a height of `-2`, and the connectors animate against the
 * recycler's own bottom edge. [modifier] therefore has to carry a bounded height — a
 * `Modifier.weight` inside a `Column`, or a `Modifier.height` — and the list scrolls itself.
 *
 * Not ported:
 * - the group indicator and the separators. `COUIExpandableListView.init()` calls
 *   `setDivider(null)`, `setChildDivider(null)` and `setGroupIndicator(null)`, and the three setters
 *   throw if handed anything else, so the native draws neither; the group view the app inflates owns
 *   all of that. `drawable/coui_list_expandable_indicator` (a 24dp `autoMirrored` vector with two
 *   1.4-wide strokes at viewport 24, tinted `?attr/couiColorLabelTertiary`) and the 24dp
 *   `dimen/expandable_list_item_size` that sizes it are dead resources here for the same reason;
 * - `ExpandableRecyclerConnector`'s mid-flight handling: when a new animation starts for a group
 *   whose animator is still running it cancels it and rescales the duration to
 *   `(remaining / totalHeight) * 400`, and its update listener ends the animation early once the
 *   dummy view scrolls out of the visible range or reaches the list's bottom edge. Restarting an
 *   [AnimatedVisibility] tween already resumes from where it was, so only the early exit is missing;
 * - the `getLastVisiblePosition` guard in `COUIExpandableListView`'s group-click handler, which
 *   refuses to toggle a group whose header sits at the very bottom of the list;
 * - the `ExpandableListAdapter` callbacks `isChildSelectable` (which the native gates on
 *   `getGroupInfo(i).animating`, so children are unclickable mid-animation), `getGroupId` /
 *   `getChildId` / `hasStableIds` / `isEmpty`, and the `OnGroupExpandListener` /
 *   `OnGroupCollapseListener` / `OnChildClickListener` interfaces, which the `expandedGroups` state
 *   and the click lambdas replace;
 * - `DummyView`'s view recycling of the children, replaced by ordinary composition here.
 *
 * @param groupCount The number of groups.
 * @param childCount The number of children of a group.
 * @param expandedGroups The groups that are open.
 * @param onExpandedGroupsChange Called with the new set when a group is toggled, after
 *   [maxExpandedGroupCount] has been applied.
 * @param groupContent The group header. The group's open state is passed along so it can draw its
 *   own indicator. It is the first function parameter the list cannot default, so it sits before
 *   [modifier]; `compose:param-order-check` allows only one trailing function without a default.
 * @param modifier The modifier to be applied to the list. It must bound the height; see above.
 * @param maxExpandedGroupCount The number of groups that may be open at once. When opening one would
 *   exceed it, an already-open group is closed to make room, as the connector does.
 * @param durationMillis The open/close duration.
 * @param easing The open/close easing.
 * @param childContent One child row.
 */
@Composable
fun ExpandableList(
    groupCount: Int,
    childCount: (groupIndex: Int) -> Int,
    expandedGroups: Set<Int>,
    onExpandedGroupsChange: (Set<Int>) -> Unit,
    groupContent: @Composable (groupIndex: Int, expanded: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    maxExpandedGroupCount: Int = ExpandableListDefaults.MAX_EXPANDED_GROUP_COUNT,
    durationMillis: Int = ExpandableListDefaults.ANIMATION_DURATION,
    easing: Easing = ExpandableListDefaults.MoveEase,
    childContent: @Composable (groupIndex: Int, childIndex: Int) -> Unit,
) {
    LazyColumn(modifier = modifier) {
        items(count = groupCount, key = { groupIndex -> groupIndex }) { groupIndex ->
            ExpandableGroup(
                expanded = groupIndex in expandedGroups,
                childCount = childCount(groupIndex),
                durationMillis = durationMillis,
                easing = easing,
                onToggle = {
                    if (groupIndex in expandedGroups) {
                        onExpandedGroupsChange(expandedGroups - groupIndex)
                    } else {
                        val opened = expandedGroups + groupIndex
                        if (opened.size <= maxExpandedGroupCount) {
                            onExpandedGroupsChange(opened)
                        } else {
                            // ExpandableRecyclerConnector.expandGroup collapses the group at index 0
                            // of its insertion-ordered metadata list, i.e. the one opened first.
                            val oldest = expandedGroups.minOrNull()
                            if (oldest != null) onExpandedGroupsChange(opened - oldest)
                        }
                    }
                },
                groupContent = { groupContent(groupIndex, groupIndex in expandedGroups) },
                childContent = { childIndex -> childContent(groupIndex, childIndex) },
            )
        }
    }
}

/** Contains default values used by [ExpandableList]. */
object ExpandableListDefaults {

    /**
     * The open/close duration in milliseconds.
     * `ExpandableRecyclerConnector.ANIMATION_DURATION`, passed to every `ExpandAnimator`.
     */
    const val ANIMATION_DURATION = 400

    /**
     * The open/close easing: `COUIMoveEaseInterpolator`, `PathInterpolator(0.3f, 0f, 0.1f, 1f)` —
     * the control points of `anim/coui_ease`.
     */
    val MoveEase: Easing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

    /**
     * How many groups may be open at once. `ExpandableRecyclerConnector.mMaxExpGroupCount`, which
     * starts at `Integer.MAX_VALUE`, i.e. no cap.
     */
    const val MAX_EXPANDED_GROUP_COUNT = Int.MAX_VALUE
}

/**
 * One group of an [ExpandableList]: the clickable header, and the children that open under it.
 *
 * @param expanded Whether the group is open.
 * @param childCount The number of children of the group.
 * @param durationMillis The open/close duration.
 * @param easing The open/close easing.
 * @param onToggle Called when the header is clicked.
 * @param groupContent The group header.
 * @param childContent One child row.
 */
@Composable
private fun ExpandableGroup(
    expanded: Boolean,
    childCount: Int,
    durationMillis: Int,
    easing: Easing,
    onToggle: () -> Unit,
    groupContent: @Composable () -> Unit,
    childContent: @Composable (childIndex: Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle),
        ) {
            groupContent()
        }
        AnimatedVisibility(
            visible = expanded,
            // The connector tweens the dummy view's height and sets alpha = height / totalHeight, so
            // the children grow and fade on the same clock.
            enter = expandVertically(animationSpec = tween(durationMillis = durationMillis, easing = easing)) +
                fadeIn(animationSpec = tween(durationMillis = durationMillis, easing = easing)),
            exit = shrinkVertically(animationSpec = tween(durationMillis = durationMillis, easing = easing)) +
                fadeOut(animationSpec = tween(durationMillis = durationMillis, easing = easing)),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                for (childIndex in 0 until childCount) {
                    childContent(childIndex)
                }
            }
        }
    }
}
