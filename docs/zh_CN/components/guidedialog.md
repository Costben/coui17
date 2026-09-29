---
title: GuideDialog
requiresScaffoldHost: false
prerequisites:
  - 可在任意位置使用，无需 `Scaffold` 或 `COUIPopupHost`
  - 在窗口层级渲染
hostComponent: None
popupHost: None
---

# GuideDialog

`WindowGuideDialog` 是 ColorOS 17 的多页首次引导对话框，对应 `COUIGuideDialogBuilder` 及其内容视图 `COUIGuideLayoutContentView`。它是一个贴底的卡片：内部为可左右翻页的页面（可选图片、标题、描述）、圆点指示器，以及「跳过 / 下一页」按钮栏。

面板复用了通用对话框布局，因此窗口呈现方式、遮罩、安全区、平滑圆角裁切以及 `Animation.COUI.Dialog` 的底部滑入 / 滑出都与 `WindowDialog` 一致；只有内容列是本组件特有的。

::: tip
本组件不依赖 `Scaffold`，可在任意 composable 作用域中使用。
:::

## 引入

```kotlin
import io.github.suqi8.coui.kmp.window.GuidePage
import io.github.suqi8.coui.kmp.window.GuideDialogDefaults
import io.github.suqi8.coui.kmp.window.WindowGuideDialog
```

## 基本用法

```kotlin
var showGuide by remember { mutableStateOf(false) }

ArrowPreference(
    title = "显示引导",
    onClick = { showGuide = true },
)

WindowGuideDialog(
    show = showGuide,
    pages = listOf(
        GuidePage(
            title = "欢迎使用 COUI",
            description = "面向 Compose Multiplatform 的 ColorOS 组件库。",
            image = painterResource(Res.drawable.onboarding),
        ),
        GuidePage(
            title = "组件由主题驱动",
            description = "颜色、文字样式与形状来自同一套主题。",
            image = painterResource(Res.drawable.theming),
        ),
        GuidePage(
            title = "开始构建",
            description = "把组件放进你的应用即可。",
            image = painterResource(Res.drawable.build),
        ),
    ),
    onDismissRequest = { showGuide = false },
)
```

还有后续页面时，主按钮会把分页前进一页；到达最后一页后它变为「知道了」按钮，点击即关闭引导；「跳过」在任意页面都会立即关闭。

## 单页引导

只有一页时，指示器隐藏、跳过按钮移除，主按钮占满整行按钮栏——与 `COUIGuideLayoutContentView.setDefaultButtonText` 切换为 `SINGLE_BUTTON` 的行为一致：

```kotlin
WindowGuideDialog(
    show = showGuide,
    pages = listOf(
        GuidePage(
            title = "单页引导",
            description = "只有一页时，跳过按钮与指示器都会隐藏。",
        ),
    ),
    onDismissRequest = { showGuide = false },
)
```

## 属性

### WindowGuideDialog

| 属性名            | 类型                | 描述                                                                            | 默认值                       | 必填 |
| ----------------- | ------------------- | ------------------------------------------------------------------------------- | ---------------------------- | ---- |
| show              | Boolean             | 是否显示引导                                                                    | -                            | 是   |
| pages             | List&lt;GuidePage&gt; | 引导页面列表，按顺序展示                                                      | -                            | 是   |
| onDismissRequest  | () -> Unit          | 用户关闭引导时回调（跳过、最后一页主按钮、返回手势或点击外部）                   | -                            | 是   |
| modifier          | Modifier            | 根内容修饰符                                                                    | Modifier                     | 否   |
| onDismissFinished | () -> Unit          | 隐藏动画结束后回调；若隐藏被中途取消（例如 `show` 又变回 true）则不回调          | {}                           | 否   |
| skipText          | String              | 次要按钮文案，`coui_guide_dialog_skip_text`                                      | GuideDialogDefaults.SkipText | 否   |
| nextText          | String              | 未到最后一页时主按钮文案，`coui_guide_dialog_next_text`                          | GuideDialogDefaults.NextText | 否   |
| knownText         | String              | 到达最后一页后主按钮文案，`coui_guide_dialog_known_text`                         | GuideDialogDefaults.KnownText | 否  |
| imageHeight       | Dp                  | 页面图片高度，`ImageHeightStyle.DEFAULT`（328dp）或 `HEIGHT_240DP`（240dp）      | GuideDialogDefaults.ImageHeight | 否 |
| onPageChanged     | (Int) -> Unit       | 分页稳定后回调其下标，首页也会回调                                              | {}                           | 否   |

### GuidePage

| 属性        | 类型      | 描述                                                                                        | 默认值 | 必填 |
| ----------- | --------- | ------------------------------------------------------------------------------------------- | ------ | ---- |
| title       | String    | 页面标题，18sp medium，`couiColorLabelPrimary`（`guide_tips_title`）                        | -      | 是   |
| description | String?   | 可选页面描述，13sp，`couiColorLabelSecondary`（`guide_tips_description`）                   | null   | 否   |
| image       | Painter?  | 可选页面图片，通栏宽度、`imageHeight` 高、`CENTER_CROP`（`pager_item_im`）                  | null   | 否   |

### GuideDialogDefaults

| 常量                        | 类型                 | 值     | COUI 来源                                                  |
| --------------------------- | -------------------- | ------ | ---------------------------------------------------------- |
| CornerRadius                | Dp                   | 32.dp  | coui_dialog_guide_clip_radius_root                         |
| MaxHeight                   | Dp                   | 632.dp | coui_dialog_guide_max_height                               |
| ViewPagerMarginBottom       | Dp                   | 16.dp  | coui_dialog_guide_viewpager2_margin_bottom                 |
| ViewPagerMarginBottomSingle | Dp                   | 20.dp  | coui_dialog_guide_viewpager2_margin_single_bottom          |
| IndicatorMarginBottom       | Dp                   | 20.dp  | coui_dialog_guide_indicator_margin_bottom                  |
| ButtonBarMarginBottom       | Dp                   | 8.dp   | coui_dialog_guide_button_layout_margin_bottom              |
| ButtonBarPaddingHorizontal  | Dp                   | 16.dp  | coui_dialog_guide_button_layout_padding_horizontal         |
| ButtonSpacing               | Dp                   | 8.dp   | coui_dialog_guide_button_layout_space                      |
| ButtonMinWidth              | Dp                   | 132.dp | coui_btn_large_width_min                                   |
| ButtonMinHeight             | Dp                   | 44.dp  | coui_btn_large_height_min                                  |
| ImageHeight                 | Dp                   | 328.dp | ImageHeightStyle.DEFAULT                                   |
| ImageHeightAlt              | Dp                   | 240.dp | ImageHeightStyle.HEIGHT_240DP                              |
| TitlePaddingHorizontal      | Dp                   | 24.dp  | coui_guide_page_item.xml guide_tips_title paddingHorizontal |
| TitleMarginTop              | Dp                   | 24.dp  | coui_guide_page_item.xml guide_tips_title marginTop         |
| TitleMarginBottom           | Dp                   | 6.dp   | coui_guide_page_item.xml guide_tips_title marginBottom      |
| BodyPaddingHorizontal       | Dp                   | 24.dp  | coui_guide_page_item.xml guide_tips_description paddingHorizontal |
| SkipText                    | String               | "Skip" | coui_guide_dialog_skip_text                                 |
| NextText                    | String               | "Next" | coui_guide_dialog_next_text                                 |
| KnownText                   | String               | "Got it" | coui_guide_dialog_known_text                             |
| TextFadeStartProgress       | Float                | 0.5f   | COUIGuideDialogButtonLayout.TEXT_FADE_START_PROGRESS        |
| TextFadeEndProgress         | Float                | 0.85f  | COUIGuideDialogButtonLayout.TEXT_FADE_END_PROGRESS          |
| TextFadeRange               | Float                | 0.35f  | COUIGuideDialogButtonLayout.TEXT_FADE_RANGE                 |

## 布局

内容列与 `COUIGuideLayoutContentView.initLayout()` 完全一致，自上而下为：

1. 分页器，`weight` 为 1，底边距 `ViewPagerMarginBottom`（16dp）；单页时改为 `ViewPagerMarginBottomSingle`（20dp）。每页是一个可纵向滚动的列：图片、标题、描述；
2. `PageIndicator` 指示器，水平居中，底边距 `IndicatorMarginBottom`（20dp）。单页时隐藏，原生同样置为 `GONE`；
3. 按钮栏，通栏宽度，底边距 `ButtonBarMarginBottom`（8dp），水平内边距 `ButtonBarPaddingHorizontal`（16dp）。

`MaxHeight`（632dp，`coui_dialog_guide_max_height`）限制整列高度。

## 颜色与文字

| 项目           | COUI token                                                   | 浅色 / 深色           | 本移植            |
| -------------- | ------------------------------------------------------------ | --------------------- | ----------------- |
| 页面标题       | attr/couiColorLabelPrimary → coui_color_label_primary         | #e6000000 / #e6ffffff | onSurface         |
| 页面描述       | attr/couiColorLabelSecondary → coui_color_label_secondary     | #8a000000 / #8affffff | onSurfaceSecondary |
| 指示器当前圆点 | coui_color_label_primary                                      | #e6000000 / #e6ffffff | onSurface         |
| 指示器其余圆点 | coui_color_hint_neutral                                       | #42000000 / #4dffffff | disabledOnSurface |

| 项目   | 值                                                           | 来源                                            |
| ------ | ------------------------------------------------------------ | ----------------------------------------------- |
| 标题   | couiTextHeadlineS，18sp，sans-serif-medium（字重 550）        | coui_guide_page_item.xml attr/couiTextHeadlineS |
| 描述   | couiTextBodyS，13sp                                          | coui_guide_page_item.xml attr/couiTextBodyS     |

## 行为

- 按钮栏动画遵循 `COUIGuideDialogButtonLayout`：分页位置映射为 `clamp((currentPage + offsetFraction) - (count - 2), 0f, 1f)`，并经 `COUIEaseInterpolator`（cubic-bezier 0.33, 0, 0.67, 1）缓动。「跳过」在进度 0.6 前淡出（`SECONDARY_BUTTON_ALPHA_END_PROGRESS`），完全透明后隐藏；主按钮从进度 0.2 起（`WIDTH_CHANGE_START_PROGRESS`）由半宽增长到整行宽，使用 response 0.15 / bounce 0 的弹簧（`SPRING_RESPONSE`、`SPRING_MINIMUM_VISIBLE_CHANGE` 为 0.001）；其文案在进度 0.5 到 0.85 之间由「下一页」交叉淡变为「知道了」（`TEXT_FADE_*`）。
- 关闭方式：跳过、最后一页的主按钮、返回手势、点击外部都会调用 `onDismissRequest`；中间页的主按钮则滚动到下一页。
- `onPageChanged` 在分页稳定后触发，首页也会触发。

## 未移植 / 差异

- 原生 `COUIGuideDialogBuilder.show()` 会调用 `setCanceledOnTouchOutside(false)`。本移植保留「点击外部关闭」：库内所有对话框的点击外部处理都由共享布局统一负责，为一个对话框单独改造会改变共享行为。
- 未移植 `COUIGuideBehavior`：报告显示它只是重命名的 Material `BottomSheetBehavior` 加上 OPlus 上拉扩展，不引用任何引导对话框资源，`COUIGuideDialogBuilder` 也没有使用它。
- 未移植 `createGuideDialogConfig` 的 ViewPager2 惯性调参：650ms 的程序滚动、500–1000ms 的惯性滚动、2500 的速度阈值、6000 的上限、2.0 的衰减系数与 `COUISpringInterpolator(0.6, 0.0)` 都是 ViewPager2 特有的。Compose 的 `HorizontalPager` 有自己的惯性模型，因此主按钮改用默认的 `animateScrollToPage` 动画。
- 未移植 `COUITwoTextButton` 的双 `TextPaint` 绘制；文案交叉淡变由两个叠加的文本按还原出的 alpha 实现。
- 未复现原生页面根视图 `COUINestedScrollView` 的 32dp 纵向渐隐边（`bottom_dialog_scroll_fading_edge_length` = 32dp），页面滚动时没有该效果。
- 只保留 `coui_guide_dialog_skip_text` / `_next_text` / `_known_text` 的英文默认值，未移植其他语言的翻译。
- 报告缺口 1 指出没有任何 style、drawable 或引导类引用 `coui_dialog_guide_clip_radius_root`。32dp 这一数值是确认的，这里用作面板圆角，但其原生调用点仍未定位。
- 原生按钮的圆角与表面颜色继承自 `Widget.COUI.Button`，并未固定在引导对话框的字面量上；本移植使用共享的 `Button` / `TextButton` 样式（主按钮用 `ButtonDefaults.buttonColorsPrimary()`，跳过按钮用默认的次级 `TextButton` 配色），圆角跟随主题。
