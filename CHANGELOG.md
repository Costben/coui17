# Changelog

All notable user-visible changes are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `FullPagePullToRefresh` reproduces the App Market's featured page: the whole page follows the finger, a full-screen layer and a clipped hint are revealed underneath, with the native 93.187dp threshold, 0.5 damping and 800ms success hold.
- Notifications reveal settings and delete actions with a melting transition while swiping left.
- `NotificationSwipeBox` accepts a `rimColor`, the light traced along the merged outline. The default half-transparent white suits a card on a bright wallpaper; a shade card passes opaque white.
- The notification swipe demonstration sits in the example app's home list, beside the other notification components.
- `TabRowDefaults.tabRowColors` accepts an `indicatorShadowColor` for the drop shadow under the sliding indicator.
- `BottomFloatingNavigationBar` accepts a `shadow`, so a light colour can turn it into an outer glow on a dark theme.
- `BottomFloatingNavigationBarDefaults` exposes the native bottom-bar material: `BlurColorsLight`, `BlurColorsDark`, `blurColors`, `blurRadius`, `NATIVE_BLUR_RADIUS_PX`, `RIM_ALPHA_LIGHT`, `RIM_ALPHA_DARK` and `CausticColor`.
- `Modifier.popupBottomBarrier` marks a custom bottom bar that list popups keep clear of, with `PopupBarrierDefaults` holding the ColorOS gap and minimum window height.
- `TooltipDefaults.HorizontalEdgeMargin`, the gap a tooltip keeps from the window's side edges.
- `BreadcrumbBarDefaults.FadeWidth`, the width of the fade at an edge the breadcrumb bar can still scroll towards.
- `ChipDefaults.DismissIconStartPadding` and `ChipDefaults.DismissIconEndPadding`, the spacing around the dismiss icon.
- `TabRowWithContour` accepts an `indicatorMaterial`, which adds the search-capsule rim and inner shadow to the sliding indicator. `null` keeps the plain indicator.
- `AppBarBlurOverlay` paints the ColorOS 17 app bar material: a scroll-driven gradient blur that reaches `AppBarBlurOverlayDefaults.ExtraHeight` past the bar's bottom edge while the overlay reports only the bar's height.
- `TopAppBar` and `SmallTopAppBar` accept `gradientGlass`, which swaps the uniform bar material for the ColorOS gradient one.
- `COUIScrollBehavior` accepts `expandOnContentScroll`, so a collapsible bar expands on a downward drag anywhere in the list instead of only once the list reaches its top.
- `AppBarBlurOverlayDefaults.scrollAlpha` has an overload taking a `minAlpha` floor, for a bar whose material never fades out completely.
- `Modifier.couiGlassHeader` paints a top-weighted glass header whose colour holds down to a fade band at the bottom edge, with `GlassDefaults.HeaderFadeHeight` as the default band height.
- `COUIIcons.Basic.ExpandableIndicator`, the native `coui_list_expandable_indicator` glyph used by popup rows that open a submenu.
- `TabRowDefaults.opaqueTabRowColors` paints the opaque segment capsule ColorOS 17 Settings' Privacy & security page uses: container `#E2E3E4` light / `#353535` dark, with the standard translucent indicator (rendering as `#F6F7F7` / `#5D5D5D`). It takes the same colour parameters as `tabRowColors`.
- `InputField` accepts a `stroke`, defaulting to `SearchBarDefaults.searchBarStroke()`, so the search capsule wears the ColorOS rim and inner shadow (`TYPE_FRAMEWORK_CAPSULE_8`). Pass `null` when the caller paints the edge on a wider capsule.
- `InputField` accepts a `glowColor`, defaulting to `SearchBarDefaults.causticGlowColor()`, so the capsule casts the native caustic light below it (`coui_caustic_color_1`). The light is punched out of the capsule, so the translucent fill is never brightened; measured on the device it reads +18 grey levels and fades out within 4.4dp. Pass `null` to opt out.
- `SearchBarActionButton` reproduces `COUISearchBar`'s functional button: the 40dp capsule carrying the "取消"/"Cancel" label, with `SearchBarActionButtonDefaults` holding the native 16dp label inset and 12dp start gap. It casts the same caustic light as the field.
- `Dimens`, the metric half of the theme: `COUITheme` takes a `dimens` argument and exposes it as `COUITheme.dimens`, and `ProvideCOUIDimens` overrides it for a subtree. Every field defaults to the matching `XxxDefaults` constant, so `Dimens()` reproduces the stock ColorOS metrics exactly. Groups: `button` (`regular` / `small` / `tiny`), `card`, `chip`, `textField`.
- `Button`, `TextButton`, `Card`, `Chip` and `TextField` take their default metrics from `COUITheme.dimens`, so a project can retheme corner radii, control heights and inside margins without touching component call sites. An explicit argument still wins over the theme.

### Fixed

- Notification cards match the shade: the corner follows the device outline (20dp, the fitted circular equivalent of ColorOS's 16dp continuous corner), the top and bottom edges carry the native `#4E4E4E` rim, and the example app's cards use the measured `#2D2D2D` fill, 8dp gaps, 32dp app icons and a 12sp timestamp.
- The search capsule uses the toolbar material instead of the blur-off fallback: its fill reads 40 at night (was 61) and its rim 78 (was 177), so it is no longer brighter than the toolbar icons beside it. The light theme keeps the value it already matched.
- The search field's hint stays visible in the expanded search bar until the first character is typed, as the device's `EditText` hint does, instead of disappearing on focus.
- List popup rows consume the release, so selecting a row no longer also counts as an outside tap and closes a submenu it just opened.
- Toolbar icon buttons keep the whole light-theme halo: `SmallTopAppBar` and `TopAppBar` no longer clip their content at the bar's bounds.
- `BottomFloatingNavigationBar` casts the native spot shadow under its capsule and expansion view instead of none.
- `BottomFloatingNavigationBar` wears the native bottom-bar glass: content scrolling underneath keeps its colour and stays recognisable instead of turning into a flat grey smear, the rim light matches the device in both themes, and the bar casts the native caustic light.
- `BottomFloatingNavigationBar` no longer stacks its default shadow on the system caustic shadow, so the light under the bar is no longer dimmed.
- `BottomFloatingNavigationBarItem` labels are 10dp, no longer grow with the system font and sit at the device's height below the icon; they were 14sp.
- Popup shadows retain soft edges throughout opening and closing animations.
- The COUI17 settings show controls supported by the current layout, and the FPS, page-swipe and blur switches now affect the gallery.
- Native light halos initialize with their rounded shadow bounds before shadow casting is enabled.
- The calendar's today ring follows the device's time zone and moves at local midnight instead of staying on the previous day.
- Cascading dropdown popups no longer print a depth warning to standard output.
- A notification swipe state created revealed, or restored after the screen is recreated, stays revealed instead of closing on first layout.
- `PlainTooltip` text is readable again: it is white on the ColorOS tips blue instead of near-black on near-black.
- On Android, window dialogs, sheets and popup menus keep the page's status and navigation bar icon colour instead of switching to white icons on a light page.
- The big round `InstallLoadProgress` button shows its label in white at rest instead of drawing it in the same blue as its fill.
- List popups stay 8dp above bottom floating navigation bars, bottom floating toolbars and navigation bars instead of opening over them.
- Tooltips keep 16dp from the window's side edges instead of touching them.
- `BreadcrumbBar` fades its segments out at an edge it can still scroll towards instead of cutting them off.
- A list popup that fits neither below nor above its anchor starts at the top of the available area instead of opening over its anchor.
- A dismissible `Chip` keeps the 32dp chip height: the close icon is 14dp with the ColorOS spacing, and its 28dp touch target no longer stretches the chip to 48dp.
- List popups tint the blurred page with the native luminosity and overlay blends instead of stacking two opaque grey layers, so content behind a menu stays visible through it.
- `BottomFloatingNavigationBar` blurs with the native 25px sigma instead of a 14.9px one, so what shows through the bar keeps its shapes as it does on the device.
- Cascading popups no longer dim the page: the native `COUIPopupListWindow` clears its background drawable, so a submenu leaves the content behind it at full brightness.
- A cascading submenu is as wide as the card that hosts it, so its rows stop shifting 28dp towards the start edge when the submenu opens — the trailing expandable indicator used to jump left at the moment of expansion.
- Popup menus follow the panel density (`ro.sf.lcd_density`) instead of the display-size override: on a phone whose density was raised, a menu keeps the native 40dp row and 15sp title instead of growing with the rest of the app.
- A menu whose rows own submenus floors at the native 196dp (`coui_popup_list_window_width_with_icon`) instead of the plain 168dp.
- The list popup press highlight is a single mask that slides between rows while a finger drags, instead of one row's mask fading out under the next one fading in.
- The list popup highlight uses the native hover tint (`coui_color_hover`, #14000000 light / #26FFFFFF dark) instead of the darker press colour, and the hairlines around the finger stay put during a drag instead of fading in and out.
- A cascading submenu header keeps its final row height and trailing icon position while expanding instead of morphing from the anchor row's measurements.
- A `TabRowWithContour` indicator keeps its material rim on the capsule at every position; the rim used to stay anchored to the row's top-left corner, so every segment after the first lost it.

### Deprecated

- `InstallLoadProgressDefaults.labelColor(state, colors)`; use the overload that also takes the `InstallLoadStyle`.

### Changed

- Swiping a notification no longer recomposes the notification box or rebuilds the card and menu shapes on every frame.
- `HighlightedIconButtonDefaults` light-theme halo lowered (`LightHaloColor` alpha 0.05 to 0.03, `LightHaloRadius` 15.5dp to 12dp). Provisional values until a live circular-button reference can be measured.
- `DropdownDefaults.ChevronSize` is 24x24dp (the native expandable-indicator box) instead of 12x24dp, and submenu rows draw `ExpandableIndicator` instead of an `ArrowRight`.
- `OverlayCascadingListPopup` defaults `enableWindowDim` to `false`, matching the native popup window.

## [1.2.0] - 2026-10-06

### Added

- `Chip` close/dismiss affordance with its own touch target.
- `Slider` secondary progress tier (`secondaryProgress`), mirrored on the range and vertical
  sliders where the track geometry is shared.
- A `Tiny` size tier for `Button`.
- `error` and `pause` states for `CircularProgressIndicator`.
- A centred warning dialog variant (overlay and window hosts).
- A horizontal progress dialog variant (overlay and window hosts).
- A custom content slot for `TopTips` alongside the existing marquee behaviour.
- `enabled` on `RecommendedPreference`, with a disabled visual state.
- Binary compatibility validation: every published module now ships a committed API dump.
- The new standalone API in this release carries the `@ExperimentalCOUIApi` opt-in marker, so
  callers opt in explicitly and the surface may still change in a minor release. Markers are not
  applied to types that already appear in a stable signature, and Kotlin rejects opt-in markers on
  value parameters, so a parameter added to an existing composable is flagged in its KDoc instead.

### Changed

- Dokka documentation is generated for every published module, the published `-javadoc` artifacts
  now contain the real API documentation instead of an empty archive, and the documentation site
  exposes the API reference of all nine modules.
- Publication is staged into a single Maven Central bundle that is validated locally before upload.

### Fixed

- Restored source and binary compatibility with 1.1.0. Every parameter added in 1.2.0 now carries a
  default, and every public JVM member the 1.1.0 artifacts exposed is declared again, so an
  application compiled against 1.1.0 keeps linking and running without a rebuild. Two
  compiler-generated lambda accessors inside `ComposableSingletons$TopAppBarKt` cannot be
  re-declared; they are referenced only from `TopAppBarKt` inside the library, never from consumer
  bytecode, so no application can link against them.
- The release workflow no longer combines tag triggers with a path filter, which previously
  prevented tagged releases from running at all.
- The Android baseline profiles now resolve against the current library: entries that named
  classes removed or renamed before 1.2.0 are updated or dropped, so the shipped profiles no longer
  spend startup budget on classes that do not exist.
- Removed the redundant clock settling in `NavNestedStressTest`. The helper advanced the manual test
  clock roughly 4x past the driver's own settle curves and settled a second time after every arm was
  already at rest, so its wall time grew with machine load until it crossed the framework's 60s
  test-body timeout. The test now settles exactly as far as the driver's tween and spring need.
- Dropped the now-redundant `-Xcontext-parameters` opt-in from `coui-blur`, which is no longer
  required by the Kotlin version in use.
- Removed an unnecessary safe call in `SearchBar`'s action slot.
- The sample app no longer reaches for the lock-screen wallpaper on Android versions that do not
  provide it, and the wallpaper lookups no longer trip the missing-permission lint error.

## [1.1.0] - 2026-08-04

### Added

- Popup row variants.
- An upgrade guide covering the migration from Miuix.

### Changed

- Rebranded the project to COUI and moved every coordinate to `io.github.suqi8.coui.kmp`.
- Matched text styles, list metrics and the dialog button bar to ColorOS metrics.
- Replaced the rotating progress indicator with the COUI implementation.

### Fixed

- Dropdowns now open at the finger and no longer dim the content behind the popup.
- The dialog button bar stacks its buttons when the labels do not fit.

## [1.0.0] - 2026-07-26

Initial public release: the nine library modules, the sample applications and the documentation
site.
