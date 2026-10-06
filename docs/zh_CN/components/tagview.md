# TagView

小型文字角标,对标 ColorOS 17 设计规范。角标是一行末尾省略的文字,可选 5dp x 10dp 前置图片与可选后置图片,底色支持统一圆角与外描边,用于「NEW」「BETA」这类标记。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.TagView
import io.github.suqi8.coui.kmp.basic.TagViewColors
import io.github.suqi8.coui.kmp.basic.TagViewDefaults
```

## 基本用法

```kotlin
TagView(text = "NEW", cornerRadius = 4.dp)
```

## 带图片

前置槽位是 `coui_tag_view_layout.xml` 里固定的 5dp x 10dp 盒子(`tagLeftImageView`,上下外边距 3dp、尾侧外边距 2dp);后置槽位(`tagImageView`)是 `wrap_content`,因此按图标的固有尺寸绘制:

```kotlin
TagView(
    text = "BETA",
    leadingIcon = COUIIcons.Refresh,
    cornerRadius = 4.dp,
)
```

## 属性

### TagView

| 属性         | 类型           | 说明                             | 默认值                       | 必填 |
| ------------ | -------------- | -------------------------------- | ---------------------------- | ---- |
| text         | String         | 角标文字,单行末尾省略            | -                            | 是   |
| modifier     | Modifier       | 应用于角标的修饰符               | Modifier                     | 否   |
| leadingIcon  | ImageVector?   | 可选 5dp x 10dp 前置图片         | null                         | 否   |
| trailingIcon | ImageVector?   | 可选后置图片(`wrap_content`)     | null                         | 否   |
| cornerRadius | Dp             | 统一圆角半径                     | TagViewDefaults.CornerRadius | 否   |
| strokeWidth  | Dp             | 外描边宽度                       | TagViewDefaults.StrokeWidth  | 否   |
| colors       | TagViewColors  | 角标配色                         | TagViewDefaults.tagViewColors() | 否 |

### TagViewColors

| 属性           | 类型  | 说明                     |
| -------------- | ----- | ------------------------ |
| containerColor | Color | 角标底色                 |
| contentColor   | Color | 文字与两个图片槽位的颜色 |
| strokeColor    | Color | 外描边颜色               |

### TagViewDefaults

| 常量                    | 类型     | 值     | COUI 来源                                                     |
| ----------------------- | -------- | ------ | ------------------------------------------------------------- |
| CornerRadius            | Dp       | 0.dp   | `couiTagCornerRadius`(默认 0dp)                               |
| StrokeWidth             | Dp       | 0.dp   | `couiTagStrokeWidth`(默认 0dp)                                |
| HorizontalPadding       | Dp       | 3.dp   | `coui_tag_view_layout.xml` 根节点 `paddingStart` / `paddingEnd` |
| LeadingIconWidth        | Dp       | 5.dp   | `coui_tag_view_layout.xml` `tagLeftImageView` 宽度            |
| LeadingIconHeight       | Dp       | 10.dp  | `coui_tag_view_layout.xml` `tagLeftImageView` 高度            |
| LeadingIconVerticalMargin | Dp     | 3.dp   | `coui_tag_view_layout.xml` `tagLeftImageView` 上下外边距      |
| LeadingIconEndMargin    | Dp       | 2.dp   | `coui_tag_view_layout.xml` `tagLeftImageView` `marginEnd`     |
| TextSize                | TextUnit | 10.sp  | `coui_default_tag_textsize` / `couiTextTag`                   |

#### 方法

| 方法名          | 类型           | 说明                                                      |
| --------------- | -------------- | --------------------------------------------------------- |
| textStyle()     | TextStyle      | 文字样式(`couiTextTag`:10sp `sans-serif-medium`,无字体内边距) |
| tagViewColors() | TagViewColors  | 默认配色(见下)                                            |

### 令牌

| COUI 令牌                                          | 值                    | 用途           | 移植                                             |
| -------------------------------------------------- | --------------------- | -------------- | ------------------------------------------------ |
| `coui_default_tag_textsize`                        | 10dp                  | 文字大小       | `TagViewDefaults.TextSize`(10sp)                 |
| `couiTextTag`(`style/couiTextTag`)                 | 10sp sans-serif-medium,`coui_spacing_multiplier_tag` 1 | 文字样式 | `textStyle()` |
| `couiTagCornerRadius`                              | 0dp(属性默认值)       | 圆角半径       | `TagViewDefaults.CornerRadius`                   |
| `couiTagStrokeWidth` / `couiTagStrokeColor`        | 0dp / 透明            | 外描边         | `TagViewDefaults.StrokeWidth` / `strokeColor`     |
| `attr/couiColorBackgroundWithTag`                  | 无法解析              | 默认底色       | 已替换,见「未移植」                              |
| `color/coui_color_white`                           | `#ffffffff`           | 默认文字颜色   | 已替换,见「未移植」                              |
| `attr/couiColorContainer8` → `color/coui_color_container8` | `#14000000` / `#1affffff` | 底色,取自可复现的拨号盘用例 | 文件常量,无主题槽位 |
| `color/coui_color_label_secondary`                 | `#8a000000` / `#8affffff` | 文字颜色,取自可复现的拨号盘用例 | `onSurfaceSecondary` |

## 未移植

- `COUITagBackgroundView` 的四个独立圆角(`couiTagTLCornerRadius`、`couiTagTRCornerRadius`、`couiTagBLCornerRadius`、`couiTagBRCornerRadius`)合并为单个 `cornerRadius`;目前复现到的用例都使用统一的 `couiTagCornerRadius`。
- 默认 `couiTagBackgroundColor` 背后的主题值 `couiColorBackgroundWithTag` 无法解析:该属性在 `attrs.xml` 中有声明,但 ColorOS 17 资源中既没有它的 `Theme.COUI` 赋值,也没有 `coui_color_background_with_tag` 颜色。因此移植采用唯一观察到的消费方写法——拨号盘的 `app:couiTagBackgroundColor="?attr/couiColorContainer8"`、`app:couiTagCornerRadius="4dp"`、`app:couiTagViewTextColor="@color/coui_color_label_secondary"`。白色文字在浅色底上不可见,这是一次有意的替换,不是还原到的框架默认值。
- `couiTagViewTextSize` 在原生中通过 `setTextSize(0, px)` 传入 10dp 尺寸,而布局给文字的 `couiTextTag` 样式声明 10sp;移植采用 10sp。
- `COUITagBackgroundView.onAttachedToWindow` 中把父容器 `clipChildren` 设为 `false`,以及 `dispatchDraw` 的 `Path` 裁剪,在 Compose 中不需要,因为父容器默认不裁剪子节点。
- 命令式的 `setImageBitmap` / `setImageDrawable` / `setImageResoure`(以及对应的左侧图片方法)由 `leadingIcon` / `trailingIcon` 参数替代。
