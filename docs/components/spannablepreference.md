# SpannablePreference

`SpannablePreference` is a preference row whose summary is an `AnnotatedString`, mirroring ColorOS's `COUISpannablePreference`.

The device class adds no fields or methods of its own: its constructor only passes `R.style.Preference_COUI_COUISpannablePreference`, which is `Preference.COUI` plus `<item name="couiEnalbeClickSpan">true</item>`. `COUIPreference.onBindViewHolder` reads that flag into `mIsEnableClickSpan` and, when set, runs the summary through `COUIPreferenceUtils.setSummaryView`, which attaches a `LinkMovementMethod` to the summary `TextView` so the spans inside it react to taps. The Compose equivalent of "a summary whose spans respond" is an `AnnotatedString` summary, which the repo's `Text` already renders with link styles.

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.SpannablePreference
```

## Basic Usage

```kotlin
SpannablePreference(
    title = "Legal information",
    summary = buildAnnotatedString {
        append("《")
        withLink(
            LinkAnnotation.Clickable(tag = "privacy") { openPrivacyPolicy() }
        ) {
            append("Privacy policy")
        }
        append("》")
    }
)
```

## Component States

### Plain Summary

An `AnnotatedString` without links renders exactly like a normal summary text:

```kotlin
SpannablePreference(
    summary = AnnotatedString("This page is for component demonstration only.")
)
```

### Clickable Row

Passing `onClick` makes the whole row react as well as the links inside the summary:

```kotlin
SpannablePreference(
    title = "Legal information",
    summary = annotatedSummary,
    onClick = { openLegalPage() }
)
```

### Disabled State

```kotlin
SpannablePreference(
    title = "Disabled row",
    summary = AnnotatedString("Cannot be interacted with"),
    enabled = false
)
```

## Properties

### SpannablePreference Properties

| Property Name    | Type                            | Description                                    | Default Value                         | Required |
| ---------------- | ------------------------------- | ---------------------------------------------- | ------------------------------------- | -------- |
| summary          | AnnotatedString                 | Summary, with its clickable spans              | -                                     | Yes      |
| modifier         | Modifier                        | Component modifier                             | Modifier                              | No       |
| title            | String?                         | Optional title                                 | null                                  | No       |
| colors           | SpannablePreferenceColors       | Row color configuration                        | SpannablePreferenceDefaults.colors()  | No       |
| startAction      | @Composable (() -> Unit)?       | Custom start side content                      | null                                  | No       |
| endActions       | @Composable (RowScope.() -> Unit)? | Custom end side content                     | null                                  | No       |
| bottomAction     | @Composable (() -> Unit)?       | Custom bottom side content                     | null                                  | No       |
| insideMargin     | PaddingValues                   | Component internal content padding             | BasicComponentDefaults.InsideMargin   | No       |
| cardListPosition | CardListPosition                | Row position inside its card group; rounded outer edges gain extra padding | CardListPosition.None           | No       |
| onClick          | (() -> Unit)?                   | Callback when the row is clicked               | null                                  | No       |
| holdDownState    | Boolean                         | Whether the component is held down             | false                                 | No       |
| enabled          | Boolean                         | Component interactive state                    | true                                  | No       |

### SpannablePreferenceColors

| Property             | Type  | Description                                  |
| -------------------- | ----- | -------------------------------------------- |
| titleColor           | Color | Color of the title while enabled             |
| disabledTitleColor   | Color | Color of the title while disabled            |
| summaryColor         | Color | Color of the summary while enabled           |
| disabledSummaryColor | Color | Color of the summary while disabled          |

### SpannablePreferenceDefaults

| Property         | Type | Description                                                                        | Value |
| ---------------- | ---- | ---------------------------------------------------------------------------------- | ----- |
| MarginBetweenLine | Dp  | Gap between the title and the summary, from `support_preference_margin_between_line` | 2dp  |
| colors()         | SpannablePreferenceColors | Default colors: the same four roles `BasicComponentDefaults` resolves for a standard row | - |

## Notes

### Why Its Own Color Class

The row needs to pick its title and summary colors itself, because it renders its own text block rather than going through `BasicComponent`'s `title`/`summary` parameters. The repo's `BasicComponentColors.color(enabled)` is `internal` to `coui-ui` and therefore unreachable from `coui-preference`, so `SpannablePreferenceColors` restates the same four roles.
