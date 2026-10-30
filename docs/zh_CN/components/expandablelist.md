# ExpandableList

ColorOS 17 的可展开列表:一个可滚动的分组列表,每组展开后露出子项,收起后再合上,对标 ColorOS 17 设计规范。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.ExpandableList
import io.github.suqi8.coui.kmp.basic.ExpandableListDefaults
```

## 基本用法

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

分组头由调用方提供,与 `ExpandableListAdapter.getGroupView` 完全一致;点击分组头会切换该组,对应 `COUIExpandableListView.init()` 内部的 `OnGroupClickListener`。由于分组头归调用方所有,指示器也由调用方负责绘制,详见下文。

列表不支持 `wrap_content`。`COUIExpandableListView.setLayoutParams` 在高度为 `-2` 时抛出 `"cannot set wrap_content"`,而且连接器是相对 recycler 自身的底边做动画的,因此限定高度的 `modifier`——`Modifier.height`,或 `Column` 中的 `Modifier.weight`——会保留自行滚动的 `LazyColumn`。高度不受限时则改用普通 `Column` 包裹各组,设置卡片可以随内容增高,不会在最后一组下方留下空白带。

## 限制同时展开的分组数

`maxExpandedGroupCount` 对应 `ExpandableRecyclerConnector.setMaxExpGroupCount`。默认值是 `Integer.MAX_VALUE`,即不限制;设为 `1` 就是手风琴效果:

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

到达上限时再展开一组,会先收起一个已展开的组腾出位置。连接器收起的是它按插入顺序维护的元数据列表的第 0 项,即最先展开的那一组;`Set` 不携带顺序,因此本移植改为收起下标最小的组。

## 属性

### ExpandableList

| 属性                   | 类型                                         | 说明                                   | 默认值                        | 必需 |
| ---------------------- | -------------------------------------------- | -------------------------------------- | ----------------------------- | ---- |
| groupCount             | Int                                          | 分组数量                               | -                             | 是   |
| childCount             | (groupIndex: Int) -> Int                     | 某分组的子项数量                       | -                             | 是   |
| expandedGroups         | Set&lt;Int&gt;                               | 已展开的分组                           | -                             | 是   |
| onExpandedGroupsChange | (Set&lt;Int&gt;) -> Unit                     | 切换分组时返回新集合的回调             | -                             | 是   |
| groupContent           | (groupIndex: Int, expanded: Boolean) -> Unit | 分组头;会传入该组是否展开              | -                             | 是   |
| modifier               | Modifier                                     | 应用于列表的修饰符;限定高度时自行滚动,不限定高度时包裹各组 | Modifier                      | 否   |
| maxExpandedGroupCount  | Int                                          | 同时可展开的分组数                     | Int.MAX_VALUE                 | 否   |
| durationMillis         | Int                                          | 展开/收起时长                          | 400                           | 否   |
| easing                 | Easing                                       | 展开/收起缓动                          | cubic-bezier(0.3, 0, 0.1, 1)  | 否   |
| childContent           | (groupIndex: Int, childIndex: Int) -> Unit   | 单个子项                               | -                             | 是   |

`groupContent` 之所以排在 `modifier` 之前,是因为它没有默认值,而 `compose:param-order-check` 只允许一个位于末尾的、无默认值的函数参数;两者都是必需参数,调用时需具名传入。

### ExpandableListDefaults

| 常量                  | 类型   | 默认值                       | COUI 来源                                       |
| --------------------- | ------ | ---------------------------- | ----------------------------------------------- |
| ANIMATION_DURATION      | Int    | 400                          | ExpandableRecyclerConnector.ANIMATION_DURATION   |
| MoveEase                 | Easing | cubic-bezier(0.3, 0, 0.1, 1) | COUIMoveEaseInterpolator,anim/coui_ease         |
| MAX_EXPANDED_GROUP_COUNT | Int   | Int.MAX_VALUE                | ExpandableRecyclerConnector.mMaxExpGroupCount    |

## 行为

- 展开/收起动画历时 `ANIMATION_DURATION`(400ms),使用 `ExpandableRecyclerConnector` 传给每个 `ExpandAnimator` 的 `COUIMoveEaseInterpolator`。
- 连接器对承载该组子项的 dummy view 做*高度*动画(`view.getLayoutParams().height = animatedValue`),并把它的透明度设为 `animatedValue / groupInfo.totalHeight`,于是子项一起长高并淡入。本移植用同一时长的高度补间 `expandVertically` 配合 `fadeIn` 实现。
- 一个分组只能展开一次:`ExpandableRecyclerConnector.expandGroup` 对已在 `mExpGroupMetadataList` 中的分组返回 `false`,当 `mMaxExpGroupCount` 为 0 时对任何分组都返回 `false`。
- 分组头以自身下标作为 key,因此切换别的分组不会丢失它的滚动位置与组合状态。

## 未移植

- 分组指示器与分隔线。`COUIExpandableListView.init()` 调用 `setDivider(null)`、`setChildDivider(null)` 与 `setGroupIndicator(null)`,而这三个 setter 一旦收到非空值就抛异常,所以原生根本不绘制它们;这部分完全由 App 自己填充的分组视图负责。同理,`drawable/coui_list_expandable_indicator`(24dp、`autoMirrored` 的矢量图,viewport 24,两条 1.4 宽描边,着色为 `?attr/couiColorLabelTertiary`)以及为它定尺寸的 24dp `dimen/expandable_list_item_size` 在这里都是死资源。
- `ExpandableRecyclerConnector` 的动画中途处理:对仍在播放动画的分组发起新动画时,它会取消旧动画并把时长按 `(剩余高度 / totalHeight) * 400` 缩放;它的 update listener 还会在 dummy view 滚出可见范围或触到列表底边时提前结束动画。重新开始的 `AnimatedVisibility` 补间本就会从当前位置继续,因此只缺提前结束这一条。
- `COUIExpandableListView` 分组点击处理里的 `getLastVisiblePosition` 判断,它会拒绝切换分组头正好贴在列表最底部的分组。
- `ExpandableListAdapter` 的 `isChildSelectable`(原生还以 `getGroupInfo(i).animating` 为条件,使子项在动画期间不可点)、`getGroupId` / `getChildId` / `hasStableIds` / `isEmpty`,以及 `OnGroupExpandListener` / `OnGroupCollapseListener` / `OnChildClickListener` 这些监听器接口,它们由 `expandedGroups` 状态与点击 lambda 取代。
- `DummyView` 对子项视图的回收复用,这里由普通组合取代。
- 分组点击时的 `playSoundEffect(0)`。
