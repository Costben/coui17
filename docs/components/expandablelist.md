# ExpandableList

The ColorOS 17 expandable list: a scrollable list of groups, each of which grows open to reveal its children and shrinks closed again, aligned with the ColorOS 17 design language.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.ExpandableList
import io.github.suqi8.coui.kmp.basic.ExpandableListDefaults
```

## Basic Usage

```kotlin
var expandedGroups by remember { mutableStateOf(setOf(0)) }

ExpandableList(
    groupCount = groups.size,
    childCount = { groupIndex -> groups[groupIndex].children.size },
    expandedGroups = expandedGroups,
    onExpandedGroupsChange = { expandedGroups = it },
    modifier = Modifier.fillMaxWidth().height(320.dp),
    groupContent = { groupIndex, expanded ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = groups[groupIndex].title,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) COUIIcons.ExpandLess else COUIIcons.ExpandMore,
                contentDescription = null,
            )
        }
    },
    childContent = { groupIndex, childIndex ->
        Text(
            text = groups[groupIndex].children[childIndex],
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        )
    },
)
```

The group header is supplied by the caller, exactly as `ExpandableListAdapter.getGroupView` does, and clicking it toggles the group — that is the internal `OnGroupClickListener` of `COUIExpandableListView.init()`. Because the header is yours, it also owns its own indicator; see below.

The list is not `wrap_content`. `COUIExpandableListView.setLayoutParams` throws `"cannot set wrap_content"` for a height of `-2`, and the connectors animate against the recycler's own bottom edge, so `modifier` has to bound the height — a `Modifier.height`, or a `Modifier.weight` inside a `Column`. The list scrolls itself.

## Capping How Many Groups Are Open

`maxExpandedGroupCount` mirrors `ExpandableRecyclerConnector.setMaxExpGroupCount`. Its default is `Integer.MAX_VALUE`, i.e. no cap; `1` gives an accordion:

```kotlin
ExpandableList(
    groupCount = groups.size,
    childCount = { groups[it].children.size },
    expandedGroups = expandedGroups,
    onExpandedGroupsChange = { expandedGroups = it },
    maxExpandedGroupCount = 1,
    modifier = Modifier.fillMaxWidth().height(224.dp),
    groupContent = { groupIndex, expanded -> /* … */ },
    childContent = { groupIndex, childIndex -> /* … */ },
)
```

Opening a group at the cap closes an already-open one to make room. The connector collapses the group at index 0 of its insertion-ordered metadata list, i.e. the one opened first; a `Set` carries no order, so the port closes the lowest group index instead.

## Properties

### ExpandableList

| Property                | Type                                             | Description                                                       | Default Value                    | Required |
| ----------------------- | ------------------------------------------------ | ----------------------------------------------------------------- | -------------------------------- | -------- |
| groupCount              | Int                                              | The number of groups                                              | -                                | Yes      |
| childCount              | (groupIndex: Int) -> Int                         | The number of children of a group                                 | -                                | Yes      |
| expandedGroups          | Set&lt;Int&gt;                                   | The groups that are open                                          | -                                | Yes      |
| onExpandedGroupsChange  | (Set&lt;Int&gt;) -> Unit                         | Called with the new set when a group is toggled                   | -                                | Yes      |
| groupContent            | (groupIndex: Int, expanded: Boolean) -> Unit     | The group header; the group's open state is passed along          | -                                | Yes      |
| modifier                | Modifier                                         | Modifier applied to the list; it must bound the height            | Modifier                         | No       |
| maxExpandedGroupCount   | Int                                              | How many groups may be open at once                               | Int.MAX_VALUE                    | No       |
| durationMillis          | Int                                              | The open/close duration                                           | 400                              | No       |
| easing                  | Easing                                           | The open/close easing                                             | cubic-bezier(0.3, 0, 0.1, 1)     | No       |
| childContent            | (groupIndex: Int, childIndex: Int) -> Unit       | One child row                                                     | -                                | Yes      |

`groupContent` sits before `modifier` because it has no default and `childContent` is the one trailing lambda `compose:param-order-check` allows; both are required, so callers pass them by name.

### ExpandableListDefaults

| Constant             | Type   | Default Value | COUI Source                                             |
| -------------------- | ------ | ------------- | ------------------------------------------------------- |
| ANIMATION_DURATION      | Int    | 400           | ExpandableRecyclerConnector.ANIMATION_DURATION   |
| MoveEase                | Easing | cubic-bezier(0.3, 0, 0.1, 1) | COUIMoveEaseInterpolator, anim/coui_ease |
| MAX_EXPANDED_GROUP_COUNT | Int   | Int.MAX_VALUE | ExpandableRecyclerConnector.mMaxExpGroupCount    |

## Behavior

- The open/close animation runs for `ANIMATION_DURATION` (400ms) on the `COUIMoveEaseInterpolator` that `ExpandableRecyclerConnector` passes to every `ExpandAnimator`.
- The connector animates the *height* of a dummy view that carries the group's children (`view.getLayoutParams().height = animatedValue`) and sets its alpha to `animatedValue / groupInfo.totalHeight`, so the children grow and fade in together. This port pairs a height-tweened `expandVertically` with a `fadeIn` over the same duration.
- A group can only be open once: `ExpandableRecyclerConnector.expandGroup` returns `false` for a group that is already in `mExpGroupMetadataList`, and for any group when `mMaxExpGroupCount` is 0.
- Group headers are keyed by their index, so their scroll position and composition survive the toggling of another group.

## Not Ported

- The group indicator and the separators. `COUIExpandableListView.init()` calls `setDivider(null)`, `setChildDivider(null)` and `setGroupIndicator(null)`, and the three setters throw if handed anything else, so the native draws neither; the group view the app inflates owns all of that. `drawable/coui_list_expandable_indicator` (a 24dp `autoMirrored` vector with two 1.4-wide strokes at viewport 24, tinted `?attr/couiColorLabelTertiary`) and the 24dp `dimen/expandable_list_item_size` that sizes it are dead resources here for the same reason.
- `ExpandableRecyclerConnector`'s mid-flight handling: when a new animation starts for a group whose animator is still running it cancels it and rescales the duration to `(remaining / totalHeight) * 400`, and its update listener ends the animation early once the dummy view scrolls out of the visible range or reaches the list's bottom edge. Restarting an `AnimatedVisibility` tween already resumes from where it was, so only the early exit is missing.
- The `getLastVisiblePosition` guard in `COUIExpandableListView`'s group-click handler, which refuses to toggle a group whose header sits at the very bottom of the list.
- The `ExpandableListAdapter` callbacks `isChildSelectable` (which the native gates on `getGroupInfo(i).animating`, so children are unclickable mid-animation), `getGroupId` / `getChildId` / `hasStableIds` / `isEmpty`, and the `OnGroupExpandListener` / `OnGroupCollapseListener` / `OnChildClickListener` interfaces, which the `expandedGroups` state and the click lambdas replace.
- `DummyView`'s recycling of the children's views, replaced by ordinary composition here.
- The `playSoundEffect(0)` of the group click.
