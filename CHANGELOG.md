# Changelog

All notable user-visible changes are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
