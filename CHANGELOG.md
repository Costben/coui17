# Changelog

All notable user-visible changes are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `SquircleShape` takes one radius per corner, so a surface that rounds only some corners, such as a bottom sheet's top edge, gets smooth corners too. `copy` with differing corners now returns a `SquircleShape` instead of a `RoundedCornerShape`.
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

- `PinnedTopAppBar` reproduces the ColorOS 17 Gallery's main-tab toolbar: a 32sp title on the first row beside the actions that never moves while the page scrolls, over the `AppBarBlurOverlay` material. Metrics measured on the PLK110 Gallery: 52dp row, 16dp insets, 40dp actions 10dp apart.
- `PinnedScrollBehavior` tracks how far the content has scrolled under a bar without ever collapsing it, so a pinned bar can still fade its material in. `TopAppBar` given this behavior fades its divider in with the content instead of keeping it hidden.
- `rememberGalleryDensity` returns the density the Gallery renders its bars at (a 360dp short edge, capped at the platform density, platform font scale kept); `GalleryDensityDefaults` holds the maths.
- `DialogButtonBarDefaults.dangerButtonColors()` paints a destructive action the way ColorOS 17 does: the capsule keeps its translucent fill and only the label turns red (`#EA3447`, `#66EA3447` when disabled — `color_delete_alert_dialog_button_warning_color` in the Settings resources). `buttonColors()` exposes the ordinary recipe the bar itself defaults to, and `OverlayAlertDialog` / `AlertDialogContentLayout` gained `buttonColors` / `recommendColors` so a dialog can pass the danger set without dropping a layer.
- `SheetActionButton` is the header action of a COUI panel — the tick and the cross at the two ends of a bottom sheet's title. It is a 40dp square with no resting fill, a 24dp icon and a circular press wash (`?couiColorPress`, radius `min(w, h) / 2`, response 0.3 / bounce 0), matching `COUIActionMenuItemView` inside a `COUIToolbar`. `BottomSheetDefaults` gained `HeaderHeight` 50dp (`coui_list_dialog_toolbar_height`), `HeaderHorizontalPadding` 16dp, `ActionButtonSize` 40dp, `ActionButtonIconSize` 24dp, `ActionButtonSpacing` 10dp and `actionButtonPressColor()`. The sheet title row now lays out at the native 50dp height instead of a 6dp/12dp padding pair, so a plain `IconButton` — 48dp with a rounded-square ripple — no longer inflates it.

### Fixed

- On Android 16 outside ColorOS, the app bar material now uses the system's own backdrop blur as well, the way ColorOS apps do: the effect is set once on the overlay view, the RenderThread blurs whatever is drawn behind it in the same frame, and scrolling only changes the view's alpha. The ColorOS gradient blur is rebuilt from three full-resolution Gaussians weighted along its radius curve. Nothing is re-recorded or downsampled by the app, so the bar no longer shimmers or flickers in the middle while a list scrolls under it. Older Android versions and other platforms keep COUI's own engine.
- On Android without the ColorOS platform blur, the app bar material no longer pulses while a list scrolls under it. The blur engine cleared its box prefilter from each downsample layer right after recording the draw, but Android renders those layers later with the properties they hold then, so the prefilter never ran. The remaining bilinear decimation only saw thin content such as text when it crossed a sample, and the blur jumped once every downscaled texel (8px at the bar's 150px radius). The same applied to the noise pass. The fix covers every blur that downscales: bars, dialogs, sheets and popups.
- Bottom sheets and dialogs stop below the status bar in an app that sets its own `LocalDensity`. Their height cap is the window height less the top inset, and the window height came from `containerDpSize`, which is converted at the platform density: with the gallery pinned to 3.875 px/dp on a 3.0 px/dp phone the cap came out 29% taller than the window, swallowed the inset, and a tall sheet ran up under the status bar. The window height is now converted at the composition's density.
- Dark dialogs and bottom sheets are translucent again. Their frosted surface was a flat 86% grey layer fitted to one measurement, which hid the page's colour almost entirely. `DialogDefaults.BlurColorsDark` and `BottomSheetDefaults.BlurColorsDark` are now the native night-mode recipe as BlurService's composite shader runs it, a `Luminosity` layer then a `ColorDodge` layer (`#B2292929` / `#8C777777` for dialogs, `#B30F0F0F` / `#A4515151` for sheets): the panel takes a fixed dark luminance but keeps the hue and saturation of what lies behind it. `blurColors()` puts `windowDimming` in front of them in dark theme, because the platform blurs the already-dimmed page.
- Dialog buttons are no longer pale grey in dark theme. `DialogButtonBarDefaults.buttonFill()` returned the light-theme 67% white layer in both themes, so over a (43,43,43) dark panel the capsule painted at (185,185,185) and its white label all but vanished. ColorOS 17's `TYPE_CONTENT_SECONDARY_BUTTON` uses `#33FFFFFF` in dark theme (`CONTENT_SECONDARY_BUTTON_PARAMS_DARK`), which lands on the (86,86,86) a system dark dialog shows; `disabledButtonFill()` likewise follows `couiColorContainer8` to `#1AFFFFFF` in dark theme.
- The panel toolbar's halo no longer bands in dark theme. It is a 5% white shadow whose ~60px falloff climbs only 13 levels over the panel, so a plain blurred shadow quantised into rings 4–5px wide; the gallery now draws it through a shader that adds the interleaved-gradient-noise dither ColorOS ends its material passes with, and neighbouring pixels alternate between single levels instead of stepping.
- The guide dialog's button row no longer rises on the last page. The narrowing skip button handed its width to the label, the label wrapped onto four lines, and the 44dp row grew around it: the primary button, centred in that taller row, sat 18.6dp above the 8dp bottom margin it keeps on every other page (measured on a PLK110: 32px on pages 1-2, 104px on page 3, panel edge unmoved). The label is now measured unbounded and clipped by a fixed-height box, so the row keeps its height and the button keeps its margin.
- `TopTips`'s AI payment dual-button row is symmetric. The two labels carried `coui_toptips_ai_payment_btn_min_width` (144dp) each but drew their text from the box's start edge and took their margins from the row's end only, so one label sat 26px from the banner's left edge and the other left 167px of empty space on the right. The pair now splits the row evenly, centres each label in its half, and keeps `coui_toptips_view_content_end_margin` on both sides with `coui_toptips_ai_payment_dual_btn_bottom_margin` underneath, matching `applyDualButtonsHorizontalConstraints()`.
- Popups no longer wear a white-to-grey vertical wash. `popupMaterialShade` and `popupBottomShade` invented a 7% white top and a 12% black bottom to fake material depth; on an opaque panel that read as a pure-white top fading into a grey bottom, which the platform never draws. Both are gone, and a popup that cannot sample a backdrop is a flat `?couiColorBackgroundTop` fill with no rim, as `RoundFrameLayout.updateMaterialEffectsState()` decides.
- The bottom sheet header's cross and tick now read as circles. The platform's `TYPE_FRAMEWORK_CIRCLE_1` rim is a 1.9dp white line at alpha 0.2, which measures +2 of 255 against a light panel and is invisible; `SheetActionButton` now paints the `?attr/couiColorControls` disc (#29000000 light / #40FFFFFF dark) under it, the same circle the platform puts behind a close cross.
- `BottomFloatingNavigationBar` and `BottomFloatingAccessoryNavigationBar` no longer blink when an overlay opens: the merged silhouette is now drawn by the runtime shader on ColorOS as well, because the system material renderer replayed its material animation on the overlay state change and shrank the whole blob to 84% for one frame before springing back over ~160ms. Frame-by-frame capture on a PLK110 shows the shader drawing the resting silhouette within one level of the renderer it replaces.
- A popup that is not on the blur path is now an opaque panel with no material rim, which is what `RoundFrameLayout.updateMaterialEffectsState()` does: `useBackgroundBlur()` false clears `mShadowEdgeEnabled` and `mSpotLightEnabled`, and `onDraw` skips both drawables. Before this the flat fallback was filled at 88% alpha *and* wore the white gradient rim and inner shadow, so the menus that cannot sample a page read as a washed-out panel with a bright edge. The overflow (three-dot) menu is one of those — `COUIActionMenuView` passes `blur=false` — so its background no longer looks different from the dropdowns that can blur.
- The guide dialog's skip button no longer leaves the tree when its width reaches zero. Removing a child re-measures the button row on that frame, and because the guide panel is centred the bar stepped down for a frame while `Next` was growing into the full width; the button now shrinks to zero and the spacer collapses with it.
- `DialogButtonBar` now reproduces the ColorOS 17 alert-dialog button bar instead of spilling past the panel. The capsules sit 16dp in from the panel's sides, 8dp apart, 16dp above its bottom edge, at the native 44dp height with a `minHeight / 2` stadium corner; the fill is the translucent `#AAFFFFFF` the platform blur layer produces, the rim is `TYPE_CONTENT_CAPSULE_5` drawn over the fill and under the label, and the dividers, the 8dp footer band and the oversized inside margin are gone because ColorOS 17 draws none of them. `TextButton`/`Button` gained an `edgeStroke` parameter so the capsule can wear that rim.
- On ColorOS, the app bar material now uses the system's own gradient blur, the way Gallery and Settings do, so text and colours scrolling under the bar no longer shimmer. Other platforms keep COUI's blur.
- The app bar material no longer flashes on and off at the top of a list. Its scroll alpha now fades the blur as well, where any alpha above 0 used to paint the full material; it follows the content and the collapse animation instead of jumping between transparent and opaque in one frame; and the blur stays in place at rest instead of being rebuilt on the first frame of every scroll.
- A `LayerBackdrop` attached to more than one node no longer blurs the wrong content or flickers off: the most recently attached node owns it, other nodes neither record nor publish coordinates, and a detaching node only clears a registration it owns. The documentation now states the one-node-per-backdrop rule and how to record a pager once.
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

- The example app keeps one entry per component. The single-input dialog is a row of the "Dialog" card, built from the same `OverlayDialog` as every other dialog there instead of a button of its own, and the panel template is the first row of the "BottomSheet" card with the overlay and window sheets under it, so a variant is a row rather than a separate entry.
- A collapsible `TopAppBar` snaps instead of tracking the scroll continuously: while the list is scrolled the title stays in the 52dp toolbar row, and the large title expands into its second row only when the list returns to the top. `COUIScrollBehavior(expandOnContentScroll = true)` keeps the continuous behaviour.
- Swiping a notification no longer recomposes the notification box or rebuilds the card and menu shapes on every frame.
- `HighlightedIconButtonDefaults` light-theme halo lowered (`LightHaloColor` alpha 0.05 to 0.03, `LightHaloRadius` 15.5dp to 12dp). Provisional values until a live circular-button reference can be measured.
- `DropdownDefaults.ChevronSize` is 24x24dp (the native expandable-indicator box) instead of 12x24dp, and submenu rows draw `ExpandableIndicator` instead of an `ArrowRight`.
- `OverlayCascadingListPopup` defaults `enableWindowDim` to `false`, matching the native popup window.
- The GitHub Packages channel publishes at the fixed version `1.2.0-SNAPSHOT` (`-ProllingSnapshot`): each push replaces what that coordinate resolves to, instead of publishing a new commit-hash version that every consumer would have to follow.

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
