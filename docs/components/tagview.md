# TagView

A small text corner badge mirroring ColorOS's `com.coui.appcompat.tagview.COUITagView` / `COUITagBackgroundView` and their `res/layout/coui_tag_view_layout.xml`. The badge is a single-line, end-ellipsized label with an optional 5dp x 10dp leading image and an optional trailing image, on a fill that can carry a uniform corner radius and an outline. It is the "NEW" / "BETA" style marker.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.TagView
import io.github.suqi8.coui.kmp.basic.TagViewColors
import io.github.suqi8.coui.kmp.basic.TagViewDefaults
```

## Basic Usage

```kotlin
TagView(text = "NEW", cornerRadius = 4.dp)
```

## With an Image

The leading slot is the fixed 5dp x 10dp box of `coui_tag_view_layout.xml` (`tagLeftImageView`, 3dp top/bottom margin, 2dp end margin); the trailing slot (`tagImageView`) is `wrap_content`, so it is drawn at the icon's intrinsic size:

```kotlin
TagView(
    text = "BETA",
    leadingIcon = COUIIcons.Refresh,
    cornerRadius = 4.dp,
)
```

## Properties

### TagView

| Property     | Type         | Description                              | Default Value              | Required |
| ------------ | ------------ | ---------------------------------------- | -------------------------- | -------- |
| text         | String       | Badge label, single line, end ellipsized | -                          | Yes      |
| modifier     | Modifier     | Modifier applied to the badge            | Modifier                   | No       |
| leadingIcon  | ImageVector? | Optional 5dp x 10dp leading image        | null                       | No       |
| trailingIcon | ImageVector? | Optional trailing image (`wrap_content`) | null                       | No       |
| cornerRadius | Dp           | Uniform corner radius                    | TagViewDefaults.CornerRadius | No     |
| strokeWidth  | Dp           | Width of the outline                     | TagViewDefaults.StrokeWidth  | No     |
| colors       | TagViewColors | Badge colors                            | TagViewDefaults.tagViewColors() | No  |

### TagViewColors

| Property       | Type  | Description                                  |
| -------------- | ----- | -------------------------------------------- |
| containerColor | Color | Fill of the badge                            |
| contentColor   | Color | Color of the label and of the two image slots |
| strokeColor    | Color | Color of the outline                         |

### TagViewDefaults

| Constant                | Type | Value  | COUI Source                                                   |
| ----------------------- | ---- | ------ | ------------------------------------------------------------- |
| CornerRadius            | Dp   | 0.dp   | `couiTagCornerRadius` (default 0dp)                           |
| StrokeWidth             | Dp   | 0.dp   | `couiTagStrokeWidth` (default 0dp)                            |
| HorizontalPadding       | Dp   | 3.dp   | `coui_tag_view_layout.xml` root `paddingStart` / `paddingEnd` |
| LeadingIconWidth        | Dp   | 5.dp   | `coui_tag_view_layout.xml` `tagLeftImageView` width           |
| LeadingIconHeight       | Dp   | 10.dp  | `coui_tag_view_layout.xml` `tagLeftImageView` height          |
| LeadingIconVerticalMargin | Dp | 3.dp   | `coui_tag_view_layout.xml` `tagLeftImageView` margins         |
| LeadingIconEndMargin    | Dp   | 2.dp   | `coui_tag_view_layout.xml` `tagLeftImageView` `marginEnd`     |
| TextSize                | TextUnit | 10.sp | `coui_default_tag_textsize` / `couiTextTag`                 |

#### Methods

| Method Name | Type      | Description                                                            |
| ----------- | --------- | ---------------------------------------------------------------------- |
| textStyle() | TextStyle | Label style (`couiTextTag`: 10sp `sans-serif-medium`, no font padding) |
| tagViewColors() | TagViewColors | Default badge colors (see below)                                |

### Tokens

| COUI token                                        | Value                 | Used for            | Port                                              |
| ------------------------------------------------- | --------------------- | ------------------- | ------------------------------------------------- |
| `coui_default_tag_textsize`                       | 10dp                  | Label size          | `TagViewDefaults.TextSize` (10sp)                 |
| `couiTextTag` (`style/couiTextTag`)               | 10sp sans-serif-medium, `coui_spacing_multiplier_tag` 1 | Label style | `textStyle()` |
| `couiTagCornerRadius`                             | 0dp (attr default)    | Corner radius       | `TagViewDefaults.CornerRadius`                    |
| `couiTagStrokeWidth` / `couiTagStrokeColor`       | 0dp / transparent     | Outline             | `TagViewDefaults.StrokeWidth` / `strokeColor`     |
| `attr/couiColorBackgroundWithTag`                 | not resolvable        | Default fill        | replaced — see Deviations                          |
| `color/coui_color_white`                          | `#ffffffff`           | Default label color | replaced — see Deviations                          |
| `attr/couiColorContainer8` → `color/coui_color_container8` | `#14000000` / `#1affffff` | Fill, from the recovered Dialer consumer | file constant, no theme slot |
| `color/coui_color_label_secondary`                | `#8a000000` / `#8affffff` | Label color, from the recovered Dialer consumer | `onSurfaceSecondary`   |

## Not Ported / Deviations

- The four per-corner radii of `COUITagBackgroundView` (`couiTagTLCornerRadius`, `couiTagTRCornerRadius`, `couiTagBLCornerRadius`, `couiTagBRCornerRadius`) are collapsed into the single `cornerRadius`; every recovered consumer uses the uniform `couiTagCornerRadius`.
- `couiColorBackgroundWithTag`, the theme value behind the default `couiTagBackgroundColor`, could not be resolved: the attribute is declared in `attrs.xml` but neither its `Theme.COUI` assignment nor a `coui_color_background_with_tag` colour exists in `Settings.apk`, `Clock.apk` or `UXDesign.apk`. The port therefore adopts the concrete pair the one recovered consumer passes — Dialer's `customize_popup_list_window_item.xml` sets `app:couiTagBackgroundColor="?attr/couiColorContainer8"`, `app:couiTagCornerRadius="4dp"` and `app:couiTagViewTextColor="@color/coui_color_label_secondary"`. White text on the light fill would have been invisible, so this is a deliberate substitution, not a recovered framework default.
- `couiTagViewTextSize` is applied by the native through `setTextSize(0, px)` with a 10dp dimension, while the `couiTextTag` style the layout gives the label declares 10sp; the port uses 10sp.
- `COUITagBackgroundView.onAttachedToWindow` setting the parent's `clipChildren = false`, and the `Path` clip of its `dispatchDraw`, are not needed because a Compose parent does not clip its children by default.
- The imperative `setImageBitmap` / `setImageDrawable` / `setImageResoure` setters (and their left-image twins) are replaced by the `leadingIcon` / `trailingIcon` parameters.
