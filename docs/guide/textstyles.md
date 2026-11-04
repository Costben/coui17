# Text Styles

This page documents all text styles provided by COUI, based on the actual implementation.

## Using Text Styles

- Use `COUITheme.textStyles.<name>` in composables.
- The color of all styles is set from `COUITheme.colorScheme.onBackground` at runtime.

## Styles

Line heights come from each `couiTextAppearance*`'s `lineSpacingMultiplier`, taken from the
`values-v35` bucket that ColorOS 16 resolves.

| Style Name | Size | Weight | Line Height | COUI text appearance |
|------------|------|--------|-------------|----------------------|
| `main`     | 17sp | Normal | 1.158em     | Miuix (no COUI 17sp tier) |
| `paragraph`| 17sp | Normal | 1.158em     | Miuix (no COUI 17sp tier) |
| `body1`    | 16sp | Normal | 1.158em     | couiTextAppearanceBodyL |
| `body2`    | 14sp | Normal | 1.2245em    | couiTextAppearanceBody |
| `button`   | 16sp | Medium | 1.263em     | couiTextAppearanceButtonL |
| `footnote1`| 13sp | Normal | 1.2245em    | couiTextAppearanceArticleBody |
| `footnote2`| 12sp | Normal | 1.143em     | couiTextAppearanceDescription |
| `headline1`| 16sp | Medium | 1.158em     | couiTextAppearanceHeadline6 |
| `headline2`| 16sp | Medium | 1.158em     | couiTextAppearanceHeadline6 |
| `subtitle` | 14sp | Bold   | 1.2245em    | couiTextAppearanceBody |
| `title1`   | 32sp | SemiBold | 1.2322em  | couiTextAppearanceHeadline1 |
| `title2`   | 24sp | SemiBold | 1.2em     | couiTextAppearanceHeadline3 |
| `title3`   | 20sp | Medium | 1.1831em    | couiTextAppearanceHeadline4 |
| `title4`   | 18sp | Medium | 1.2381em    | couiTextAppearanceHeadline5 |

::: tip
COUI has no 17sp text appearance, so `main` and `paragraph` keep their inherited Miuix size and
borrow the line spacing of the closest tier by font size. `headline1` was measured rather than
inferred: a ColorOS 16 preference title node is 21.71dp tall at 560 dpi, which is 16sp — not the
17sp inherited from Miuix. Weights follow the source appearances: `600` (`textFontWeight`) for the
two largest titles, `sans-serif-medium` (500) for `title3`, `title4` and both headlines.

Every style carries `LineHeightStyle(alignment = Top, trim = None)` to match Android's
`lineSpacingMultiplier`: the extra leading sits below the line and is never trimmed. Compose's
default centres the leading and trims it on the first and last line, which cancels the line height
out entirely on single-line text.

:::

::: tip
`Text` and `MarqueeText` take `main` at the 15sp of `TextAppearance.COUI`, which is what a bare
`TextView` resolves under `Theme.COUI` (`coui_textview_text_size`). Components that mirror a specific
text appearance pass their own style.

:::

::: tip Top bar titles use fixed dp
`TopAppBar`, `SmallTopAppBar` and `PinnedTopAppBar` do not use the `sp` scale for their titles.
The large title is 32dp, the collapsed title 24dp (18dp with a subtitle), and the small toolbar
title 18dp; none of them follows the system font size. See
[Implementation Specification § Top Bar Title Sizes](./specification#top-bar-title-sizes).
:::

## Usage

```kotlin
Text(
    text = "Title",
    style = COUITheme.textStyles.title2
)

Text(
    text = "Body",
    style = COUITheme.textStyles.body1
)
```

## Customization

- Override styles via `defaultTextStyles(...)` and pass to `COUITheme(textStyles = ...)`.

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
) { /* Content */ }
```
