# 文本样式

本页基于实际实现，完整列出 COUI 提供的文本样式。

## 使用方式

- 在组合函数中通过 `COUITheme.textStyles.<名称>` 访问。
- 所有样式的颜色会在运行时由 `COUITheme.colorScheme.onBackground` 统一设置。

## 样式列表

行高来自 COUI 各 `couiTextAppearance*` 的 `lineSpacingMultiplier`，取 `values-v35` 下的值（ColorOS 16 生效的资源桶）。

| 样式名       | 字号  | 字重   | 行高      | 对应 COUI 样式 |
|--------------|-------|--------|-----------|----------------|
| `main`       | 17sp  | Normal | 1.158em   | Miuix（COUI 无 17sp 档） |
| `paragraph`  | 17sp  | Normal | 1.158em   | Miuix（COUI 无 17sp 档） |
| `body1`      | 16sp  | Normal | 1.158em   | couiTextAppearanceBodyL |
| `body2`      | 14sp  | Normal | 1.2245em  | couiTextAppearanceBody |
| `button`     | 16sp  | Medium | 1.263em   | couiTextAppearanceButtonL |
| `footnote1`  | 13sp  | Normal | 1.2245em  | couiTextAppearanceArticleBody |
| `footnote2`  | 12sp  | Normal | 1.143em   | couiTextAppearanceDescription |
| `headline1`  | 16sp  | Medium | 1.158em   | couiTextAppearanceHeadline6 |
| `headline2`  | 16sp  | Medium | 1.158em   | couiTextAppearanceHeadline6 |
| `subtitle`   | 14sp  | Bold   | 1.2245em  | couiTextAppearanceBody |
| `title1`     | 32sp  | SemiBold | 1.2322em | couiTextAppearanceHeadline1 |
| `title2`     | 24sp  | SemiBold | 1.2em    | couiTextAppearanceHeadline3 |
| `title3`     | 20sp  | Medium | 1.1831em  | couiTextAppearanceHeadline4 |
| `title4`     | 18sp  | Medium | 1.2381em  | couiTextAppearanceHeadline5 |

::: tip 提示
COUI 没有 17sp 这一档 text appearance，`main` / `paragraph` 保留从 Miuix 继承的字号，取字号最接近的那一档的行高倍数。`headline1` 靠实测而非推断得出：ColorOS 16 在 560dpi 下的偏好标题节点高 21.71dp，对应 16sp，而不是从 Miuix 继承的 17sp。字重跟随对应的 text appearance：最大的两级标题用 `textFontWeight` 600，`title3` / `title4` 与两级 headline 用 `sans-serif-medium`（500）。

所有样式都带 `LineHeightStyle(alignment = Top, trim = None)`，以对应 Android `lineSpacingMultiplier` 的行为：额外行距落在文字下方且不被裁剪。Compose 默认会把行距居中并在首尾行裁掉，那样单行文本上的行高设置会完全失效。
:::

::: tip 顶栏标题使用固定 dp
`TopAppBar`、`SmallTopAppBar` 与 `PinnedTopAppBar` 的标题不使用 `sp` 梯度：大标题 32dp，
折叠态 24dp（有副标题时 18dp），小工具栏 18dp，都不跟随系统字体大小。详见
[实现规范 · 顶栏标题字号](./specification#顶栏标题字号)。
:::

## 使用示例

```kotlin
Text(
    text = "标题",
    style = COUITheme.textStyles.title2
)

Text(
    text = "正文",
    style = COUITheme.textStyles.body1
)
```

## 自定义

- 通过 `defaultTextStyles(...)` 覆盖样式，并传入 `COUITheme(textStyles = ...)`。

```kotlin
val customTextStyles = defaultTextStyles(
    title1 = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold
    )
)

val controller = remember { ThemeController(ColorSchemeMode.System) }
COUITheme(
    controller = controller,
    textStyles = customTextStyles
) { /* 内容 */ }
```
