# MarqueeText

当文字超出所给空间时横向滚动的文本,对标 ColorOS 17 设计规范。放得下的文字正常绘制、不滚动;比容器宽的文字以每秒 25dp 向左移动,起点距左边缘 10dp,每轮结束后留出 80dp 间隔,启动前等待 1000ms,并在两侧被裁切的边缘各做 26dp 的渐隐。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.MarqueeText
import io.github.suqi8.coui.kmp.basic.MarqueeTextDefaults
```

## 基本用法

```kotlin
MarqueeText(
    text = "这句话对于它所在的窄盒子来说实在太长了",
    modifier = Modifier.width(160.dp),
)
```

## 按需滚动

原生在应用启用之前一直保持 `isMarqueeEnable` 为 false;`marqueeEnabled` 对应这个开关。关闭它会停止滚动并把文字复位到起点:

```kotlin
MarqueeText(
    text = longText,
    marqueeEnabled = keepScrolling,
    modifier = Modifier.width(160.dp),
)
```

## 属性

### MarqueeText

| 属性              | 类型      | 说明                     | 默认值                                 | 必填 |
| ----------------- | --------- | ------------------------ | -------------------------------------- | ---- |
| text              | String    | 要显示的文本             | -                                      | 是   |
| modifier          | Modifier  | 应用于文本的修饰符       | Modifier                               | 否   |
| color             | Color     | 文本颜色                 | Color.Unspecified                      | 否   |
| style             | TextStyle | 文本样式                 | MarqueeTextDefaults.textStyle()        | 否   |
| marqueeEnabled    | Boolean   | 是否允许滚动             | true                                   | 否   |
| speed             | Dp        | 滚动速度                 | MarqueeTextDefaults.ScrollSpeed        | 否   |
| startLocation     | Dp        | 文字起点距左边缘的距离   | MarqueeTextDefaults.ScrollStartLocation | 否  |
| textInterval      | Dp        | 两轮文字之间的间隔       | MarqueeTextDefaults.TextInterval       | 否   |
| fadingEdgeWidth   | Dp        | 每侧被裁切边缘的渐隐宽度 | MarqueeTextDefaults.FadingEdgeWidth    | 否   |
| scrollDelayMillis | Long      | 滚动开始前的延迟         | MarqueeTextDefaults.ScrollDelayMillis  | 否   |

### MarqueeTextDefaults

| 常量                | 类型     | 值    | COUI 来源                                               |
| ------------------- | -------- | ----- | ------------------------------------------------------- |
| ScrollSpeed         | Dp       | 25.dp | `coui_top_tips_scroll_speed`                            |
| ScrollStartLocation | Dp       | 10.dp | `coui_top_tips_scroll_text_start_location`              |
| TextInterval        | Dp       | 80.dp | `coui_top_tips_scroll_text_interval`                    |
| FadingEdgeWidth     | Dp       | 26.dp | `coui_top_tips_fading_edge_size`                        |
| ScrollDelayMillis   | Long     | 1000  | `COUIMarqueeTextView.DEFAULT_SCROLL_DELAY_DURATION`     |

#### 方法

| 方法名      | 类型      | 说明                                                  |
| ----------- | --------- | ----------------------------------------------------- |
| textStyle() | TextStyle | 默认文本样式(`COUITheme.textStyles.main`,与 `Text` 一致) |

## 行为

- `COUIMarqueeTextView.onMeasure` 仅在测量的文字宽于视图时才启动滚动;移植用同样的样式测量文字,否则回退为一行静态、末尾省略的文本。
- `initSpeed` 把每帧步长算作 `coui_top_tips_scroll_speed / Display.getRefreshRate()`,即每秒 25dp;移植以此作为每轮滚动的时长。
- `setContent` 在文字后追加 `coui_top_tips_scroll_text_interval`(80dp)并把字符串重复到填满视图;移植以间隔分隔的两份文字实现同样的无缝循环。
- `initTextViewAttributes` 通过 `setFadingEdgeLength(coui_top_tips_fading_edge_size)`(26dp)开启横向渐隐边缘;移植以 `BlendMode.DstOut` 渐变绘制同样的边缘渐隐——右侧在内容继续延伸时始终渐隐,左侧在文字越过起点后才渐隐。
- 滚动在文字完成布局后 `DEFAULT_SCROLL_DELAY_DURATION`(1000ms)开始,`mCurrentScrollLocation` 的初值为 `coui_top_tips_scroll_text_start_location`(10dp)。

## 未移植

- 命令式的 `stopRoll()` / `continueRoll()` 这一对 API:`marqueeEnabled` 切换同一状态,把它设回 `false` 会停止滚动并把文字复位到起点。
- `mSuppressAccessibilityEvents` / `onInitializeAccessibilityEvent` 的无障碍屏蔽,以及用 `mOriginText` 而非重复字符串上报的 `onInitializeAccessibilityNodeInfo` 覆写;移植始终上报原始文本,因为它从不替换已布局的字符串。
- `checkAllCharactersDirections` 与 `isAllCharactersLtR` 对滚动方向的镜像;移植把方向交给 Compose 的 `LayoutDirection`。
- `COUIMarqueeTextView` 是 `AppCompatTextView`,自身不声明文本外观,因此 `textStyle()` 默认使用主题的 `COUITheme.textStyles.main`,与 `Text` 的默认一致。
- `id/marquee` 属于应用布局的视图 id,不属于本组件。
