# TouchSearchView

纵向 A–Z 索引栏,对应 ColorOS 的 `COUITouchSearchView`(`com/coui/appcompat/touchsearchview/COUITouchSearchView`),即贴在联系人 / 设置列表尾侧的字母条。手指在栏上上下滑动即可选中其下的字母,同时一个放大的字母磁贴浮在栏旁。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.TouchSearchView
import io.github.suqi8.coui.kmp.basic.TouchSearchViewColors
import io.github.suqi8.coui.kmp.basic.TouchSearchViewDefaults
```

## 基本用法

```kotlin
var selectedKey by remember { mutableStateOf("-") }

Box(modifier = Modifier.fillMaxWidth()) {
    Text(text = "Selected: $selectedKey", modifier = Modifier.align(Alignment.CenterStart))
    TouchSearchView(
        keys = TouchSearchViewDefaults.NormalKeys,
        onKeySelected = { selectedKey = it },
        modifier = Modifier.align(Alignment.TopEnd),
    )
}
```

每当手指下的字母发生变化就会触发 `onKeySelected`,首次按下也会触发——即原生的 `COUITouchSearchView.TouchSearchActionListener`。重复按同一个字母不会再次触发,与 `mFindKeyIndex` 一致。

请在索引栏的**起始侧**留出空间(或把它放在不裁剪的父级的末尾):放大磁贴在原生组件中是一个独立窗口,定位在 `locationInWindow[0] - mPopupWindowEndMargin - mPopupFirstWidth`,因此本移植把它绘制在索引栏自身边界**之外**,距栏的起始边 0.5dp。

## 字母集合

```kotlin
TouchSearchView(keys = TouchSearchViewDefaults.NormalKeys, onKeySelected = { })  // *、A…Z、#
TouchSearchView(keys = TouchSearchViewDefaults.SpecialKeys, onKeySelected = { }) // A…Z、#
```

二者分别对应 `array/normal_touchsearch_keys` 与 `array/special_touchsearch_keys`。`NormalKeys` 依次是 `*`(`string/star_character`)、26 个字母(`string/coui_touchsearch_character_a` … `_z`)、`#`(`string/well_character`);`SpecialKeys` 去掉了开头的 `*`。传入其它字母列表同样可用——索引栏完全依据 `keys.size` 排布。

## 属性

| 属性                  | 类型               | 说明                                     | 默认值                           | 必需 |
| --------------------- | ------------------ | ---------------------------------------- | -------------------------------- | ---- |
| keys                  | List&lt;String&gt; | 索引字母,自上而下                        | -                                | 是   |
| onKeySelected         | (String) -> Unit   | 手指下的字母变化时的回调                  | -                                | 是   |
| modifier              | Modifier           | 应用于该索引栏的修饰符                    | Modifier                         | 否   |
| enabled               | Boolean            | 是否响应触摸                              | true                             | 否   |
| hapticFeedbackEnabled | Boolean            | 字母变化时是否触发触感反馈                | true                             | 否   |
| colors                | TouchSearchViewColors | 颜色                                  | TouchSearchViewDefaults.colors() | 否   |

索引栏自身尺寸为 `BackgroundWidth` x(`PaddingTop` + `keys.size` * (`EachItemHeight` + `ItemSpacing`) + `PaddingBottom`)。把它放进有尺寸的父级即可让它短于内容——在 `NormalKeys` 与默认 16dp 步长下,尺寸为 39dp x 480dp。

### TouchSearchViewDefaults

| 常量            | 类型               | 取值                             | COUI 来源                                            |
| --------------- | ------------------ | -------------------------------- | ---------------------------------------------------- |
| BackgroundWidth | Dp                 | 39.dp                            | coui_touchsearch_background_width                    |
| EachItemHeight  | Dp                 | 14.dp                            | coui_touchsearch_each_item_height                    |
| ItemSpacing     | Dp                 | 2.dp                             | coui_touchsearch_item_spacing                        |
| PaddingTop      | Dp                 | 16.dp                            | coui_touchsearch_padding_top                         |
| PaddingBottom   | Dp                 | 16.dp                            | coui_touchsearch_padding_bottom                      |
| KeyTextSize     | Dp                 | 12.dp                            | coui_touchsearch_key_textsize                        |
| DotTextSize     | Dp                 | 20.dp                            | coui_touchsearch_key_dot_textsize                    |
| PopupSize       | Dp                 | 48.dp                            | coui_touchsearch_popup_first_default_width / _height |
| PopupTextSize   | TextUnit           | 20.sp                            | coui_touchsearch_popupwin_first_textsize             |
| PopupEndMargin  | Dp                 | 0.5.dp                           | coui_touchsearch_popupwin_right_margin               |
| CharOffset      | Dp                 | 0.dp                             | coui_touchsearch_char_offset                         |
| TouchPadding    | Dp                 | 8.5.dp                           | coui_touchsearch_touch_padding_start / _end          |
| TouchEndGap     | Dp                 | 8.dp                             | coui_touchsearch_touch_end_gap                       |
| DefaultHeight   | Dp                 | 432.dp                           | coui_touchsearch_default_height                      |
| LimitHeights    | List&lt;Dp&gt;     | 416 / 288 / 224 / 192 / 144 dp   | coui_touchsearch_limit1..5_height                    |
| MinHeight       | Dp                 | 296.dp                           | coui_touchsearch_min_height                          |
| StarCharacter   | String             | `*`                              | string/star_character                                |
| WellCharacter   | String             | `#`                              | string/well_character                                |
| Dot             | String             | `·`                              | string/coui_touchsearch_dot                          |
| Letters         | List&lt;String&gt; | A…Z                              | string/coui_touchsearch_character_a … _z             |
| NormalKeys      | List&lt;String&gt; | `*`、A…Z、`#`                    | array/normal_touchsearch_keys                        |
| SpecialKeys     | List&lt;String&gt; | A…Z、`#`                         | array/special_touchsearch_keys                       |

#### 方法

| 方法名   | 类型                  | 说明             |
| -------- | --------------------- | ---------------- |
| colors() | TouchSearchViewColors | 从主题解析的配色 |

### 颜色

| COUI token                                                        | 取值                  | 用途                       | 移植目标                          |
| ----------------------------------------------------------------- | --------------------- | -------------------------- | --------------------------------- |
| attr/couiColorSecondNeutral → color/coui_color_secondary_neutral  | #8a000000 / #8affffff | 未按下的字母文本           | onSurfaceSecondary                |
| attr/couiColorPrimaryTextOnPopup → color/couiDefaultPrimaryR      | #ff347cff             | 按下的字母文本             | primary(#FF0080FF,最接近的槽位)   |
| attr/couiColorBackgroundTop → color/coui_color_background_top     | #ffffffff / #ff333333 | 放大磁贴的填充             | 文件内常量,主题无对应槽位         |
| color/coui_touchsearch_popup_text_color                           | #d9000000 / #d9ffffff | 放大磁贴的文字             | 文件内常量,主题无对应槽位         |
| color/coui_touchsearchview_key_text_color_unpressed               | #8c000000 / #8cffffff | 未按下字母文本(样式)       | 被上面的选择器取代                |
| color/coui_touchsearchview_popup_bg_color                         | #ffffffff             | 弹窗背景(样式)             | 被 couiColorBackgroundTop 取代    |
| color/coui_touchsearchview_dot_color                              | selector              | 被折叠字母的点号           | 未移植,见下文                     |

`color/coui_touchsearchview_text_color` 是一个选择器:按下项为 `?attr/couiColorPrimaryTextOnPopup`,默认项为 `?attr/couiColorSecondNeutral`。`coui_touchsearchview_popup_bg_color`(`#ffffffff`)是弹窗背景色,但磁贴实际使用的 drawable 是 `drawable/coui_touch_search_popup_bg`——一个 `<shape android:shape="rectangle">`,只有单个 `<solid android:color="?attr/couiColorBackgroundTop">`,**没有 `<corners>`**,也就是一个没有圆角的方块,这正是磁贴按不带圆角的矩形绘制的原因。

### 文本

| 项目       | 取值                  | 来源                                        |
| ---------- | --------------------- | ------------------------------------------- |
| 字母       | 12dp,medium           | coui_touchsearch_key_textsize               |
| 字母字体   | `sans-serif-medium`   | COUITouchSearchView.refresh()               |
| 磁贴字母   | 20sp,medium           | coui_touchsearch_popupwin_first_textsize    |

## 行为

- 几何来自原生 `updateKeys` + `onDraw`:每个字母占据 14dp 的格子外加 2dp 间距,步长为 16dp;索引栏宽 39dp,第一个字母之上与最后一个字母之下各有 16dp 内边距。文本在格子内水平与垂直居中,这也正是原生基线的落点(`key.top + (touchBottom - top) / 2 - (descent + ascent) / 2`)。
- 按下与拖动通过 `awaitFirstDown` + `awaitPointerEvent` 跟随手指,按 `(y - paddingTop) / stride` 取整并夹到字母范围内选中对应字母。抬手会清除选中并隐藏磁贴。
- 字母变化会触发 `HapticFeedbackType.TextHandleMove`,取代原生经 `VibrateUtils` / `couiAdaptiveVibrator`(`attr/couiTouchSearchVibrateLevel` = 0)发送的 `VibrationEffect`。传 `hapticFeedbackEnabled = false` 可关闭。

## 未移植

- 点号折叠路径:`initHeightRangeSpec`、`getShowLettersSize`、`getLetterSizeFromDot`、`getLimitDotLevel` 与 `LetterLimitLevelInfo`,它们会在索引栏低于 `coui_touchsearch_limit1_height` 时把字母折叠成 `·`(`string/coui_touchsearch_dot`)。`TouchSearchViewDefaults.LimitHeights`、`MinHeight` 与 `Dot` 只记录了恢复出的 token,没有任何代码消费它们,因此短于内容的索引栏只会被裁剪。
- 开头的收藏键图标(`mKeyCollectDrawable`,`drawable/coui_touchsearch_collect_normal` / `_popup`,尺寸由 `coui_touchsearch_collect_drawable_popup_width` / `_height` = 16dp 决定),它在 `attr/couiFirstIsCharacter` 为 false 时取代 `*` 键绘制。
- 二级弹窗:`coui_touchsearch_popupwin_second_textsize`(18sp)、`coui_touchsearch_popupwin_second_marginEnd`(8dp)、`coui_touchsearch_popupname_max_height`(304dp)、`coui_touchsearch_popupwin_default_offset`(23dp)与 `coui_touchsearch_popup_second_text_height`(40dp)都属于列出某字母下条目的那个窗口,它位于索引栏之外。
- `coui_touchsearch_right_margin`(5dp)、`coui_touchsearch_popupwin_margin`(4dp)与 `coui_touchsearch_popup_first_key_padding_bottom`(10dp),它们用于定位而非绘制原生弹窗。
- 放大磁贴按不带圆角的矩形绘制在索引栏起始侧,而非放在独立的 `PopupWindow` 中,因此任何裁剪自身边界的祖先都会把它裁掉。请给索引栏一个不裁剪的父级,并在其起始侧留出至少 48.5dp 的空间。
