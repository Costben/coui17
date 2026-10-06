# COUI

Compose Multiplatform UI component library. Targets Android, iOS, Desktop (JVM), macOS, Web (Wasm/JS).

> **COUI 改造主线**：本仓库正基于 Miuix 就地演进成一套 COUI（ColorOS 设计语言）组件库。凡涉及「换成 ColorOS 风格 / COUI 换肤 / 调主题或控件视觉 / 校准 ColorOS 设计 token」的任务，先读 `AGENT.md`（项目定位、设计来源与溯源原则、token→代码落点映射），再按本文件的工程规范落地。本文件的代码风格、API 约定、性能约束在 COUI 改造中**继续生效**。

## Quick Start

- For significant features or refactors, sketch an Plan first; keep it updated as you work.
- Run the component-specific checks below before handing work off; do not skip failing steps.
- Use Context7 to pull library/API docs when you touch unfamiliar Compose/Android/JVM/Js/WasmJs/Swift APIs or deps.
- Always use Chinese to communicate with users and output Plan content, but the generated code (including comments and KDoc) must remain in English.

## Key Commands

| Action                | Command                                                          |
| :-------------------- | :--------------------------------------------------------------- |
| Build (full)          | `./gradlew assemble`                                             |
| Build (quick check)   | `./gradlew compileKotlinDesktop`                                 |
| Test                  | `./gradlew check`                                                |
| Check formatting      | `./gradlew spotlessCheck`                                        |
| **Fix formatting**    | `./gradlew spotlessApply`                                        |
| **Check public API**  | `./gradlew apiCheck` (update the dumps with `./gradlew apiDump`) |
| Generate API docs     | `./gradlew dokkaGenerate`                                        |
| Verify local release  | `./gradlew publishAllToLocalRepository verifyLocalPublication`   |
| Verify staged release | `./gradlew verifyStagedMavenCentral -Prelease` (wipes and re-stages `build/publishing/mavenCentral`; `-Prelease` requires GPG signatures) |
| Smoke-test published  | `./gradlew -p smoke compileKotlin -PcouiVersion=<version>`       |
| Release-scenario drill | `bash .github/scripts/test-staged-repository-verification.sh` (staged-repository checks) and `bash .github/scripts/test-release-tag-resolution.sh` (is the release built from the tag?) |
| Run Android demo      | `./gradlew :example:android:installDebug`                        |
| Run Desktop demo      | `./gradlew :example:desktop:hotRunDesktop --auto`                |
| Run WasmJs demo       | `./gradlew :example:web:wasmJsBrowserRun`                        |
| Run Js demo           | `./gradlew :example:web:jsBrowserDevelopmentRun`                 |
| Run macOS demo        | `./gradlew :example:macos:runDebugExecutableMacosArm64`          |
| Run iOS demo          | Open `example/ios/iosApp.xcodeproj` in Xcode and run             |
| Docs site             | `cd docs && corepack yarn docs:build` (needs `./gradlew :docs:iconGen:generateIcons` first) |

Before committing, run `./gradlew spotlessCheck`; only run `./gradlew spotlessApply` if the check reports violations. CI will reject formatting violations.

## Repository Structure

| Directory               | Purpose                                                       |
| :---------------------- | :------------------------------------------------------------ |
| `coui-core/`           | Utilities + COUIIcons base (depended on by `ui` and `icons`) |
| `coui-ui/`             | Main UI library                                               |
| `coui-preference/`     | Preference / menu / popup components                          |
| `coui-blur/`           | Blur effects (minSdk 23; effects need API 33)                 |
| `coui-squircle/`       | Squircle (continuous corner) shape                            |
| `coui-shader/`         | Runtime shader abstraction                                    |
| `coui-nav/`            | Self-contained navigation runtime                             |
| `coui-icons/`          | Extended icon resources                                       |
| `coui-navigation3-ui/` | Navigation 3 UI (depends on `coui-squircle` for transition clip) |
| `smoke/`               | Separate Gradle build that consumes the published artifacts   |
| `verification/`        | Separate Gradle builds that consume the published artifacts the way a downstream project does: `compat-consumer` (1.1.0 source + bytecode), `android-api23-consumer` (minSdk 23, no `overrideLibrary`), `apple-web-consumer` (iosArm64/iosSimulatorArm64/macosArm64/js/wasmJs, links the frameworks) |
| `example/`              | Demo app                                                      |
| `baselineprofile/`      | Android baseline profile generation                           |
| `docs/`                 | VitePress documentation site                                  |
| `build-plugins/`        | Custom Gradle plugins                                         |
| `gradle/`               | Version catalog + wrapper                                     |

### Component Source Layout

`coui-ui/src/commonMain/kotlin/io/github/suqi8/coui/kmp/`:

| Subdir        | Contents                                                        |
| :------------ | :-------------------------------------------------------------- |
| `basic/`      | Fundamental components (Button, Switch, TextField, Surface, …)  |
| `overlay/`    | Scaffold-hosted overlays (OverlayDialog, OverlayBottomSheet, …) |
| `window/`     | Platform window components (WindowDialog, WindowBottomSheet, …) |
| `layout/`     | Shared layout primitives                                        |
| `theme/`      | COUITheme, Colors, TextStyles, ThemeController, DynamicColors  |
| `color/`      | Color utilities, Material Color                                 |
| `anim/`       | Animation utilities                                             |
| `utils/`      | General utilities                                               |
| `icon/`       | Built-in basic icons (ArrowRight, Check, Search, …)             |
| `interfaces/` | Shared interfaces                                               |

`coui-preference/src/commonMain/kotlin/io/github/suqi8/coui/kmp/`:

| Subdir        | Contents                                                        |
| :------------ | :-------------------------------------------------------------- |
| `preference/` | Preference components (SwitchPreference, CheckboxPreference, …) |
| `menu/`       | Dropdown menus (Overlay/Window variants, cascading)             |
| `popup/`      | Dropdown popup primitives shared by menus                       |

### Platform Source Sets

```
commonMain
├── androidMain
└── skikoMain (Skiko rendering — all non-Android targets)
    ├── darwinMain (iOS + macOS)
    │   ├── iosMain
    │   └── macosMain
    ├── desktopMain (JVM)
    └── webMain
        ├── wasmJsMain
        └── jsMain
```

99% of UI logic lives in `commonMain`. Only use platform source sets for genuinely platform-specific code.

## Code Style

- **Formatter**: Spotless + ktlint with Compose rules (`io.nlopez.compose.rules:ktlint`). Exact versions live in `build-plugins/src/main/kotlin/module.spotless.gradle.kts`
- **License header** (required on all `.kt` and `.kts` files):

  ```
  // Copyright $YEAR, compose-coui-ui contributors
  // SPDX-License-Identifier: Apache-2.0
  ```

  Spotless auto-fills `$YEAR` with the current year. Do not manually change years in existing file headers.

- **Spotless exclusions**: Icon files (`**/icon/**/*.kt`) and a subset of navigation3 sources (`**/navigation3/ListUtils.kt`, `**/navigation3/scene/*.kt`, `**/navigation3/ui/*.kt`) are excluded from formatting.
- Line endings: platform-native
- Composable function names may use PascalCase (ktlint rule disabled for `@Composable`)

## API Conventions

### Composable Function Signature

Follow this parameter ordering:

```kotlin
@Composable
fun ComponentName(
    // 1. Required parameters (functional callbacks first)
    onClick: () -> Unit,
    // 2. Modifier
    modifier: Modifier = Modifier,
    // 3. Boolean flags
    enabled: Boolean = true,
    // 4. Visual parameters with defaults from ComponentDefaults
    cornerRadius: Dp = ComponentDefaults.CornerRadius,
    minWidth: Dp = ComponentDefaults.MinWidth,
    minHeight: Dp = ComponentDefaults.MinHeight,
    colors: ComponentColors = ComponentDefaults.componentColors(),
    insideMargin: PaddingValues = ComponentDefaults.InsideMargin,
    // 5. Content lambda (always last)
    content: @Composable () -> Unit,
)
```

Add `@NonRestartableComposable` only when the component is a thin wrapper that fully delegates to another composable and reads no state itself — see the Key Patterns below. Do not include it on the default template.

### Defaults Object

Each component provides a `ComponentDefaults` object:

```kotlin
object ButtonDefaults {
    val MinWidth = 58.dp                    // Constant dimensions as val
    val MinHeight = 40.dp
    val CornerRadius = 16.dp
    val InsideMargin = PaddingValues(horizontal = 16.dp, vertical = 13.dp)

    @Composable
    fun buttonColors(                       // Color factories must be @Composable
        color: Color = COUITheme.colorScheme.secondaryVariant,
        disabledColor: Color = COUITheme.colorScheme.disabledSecondaryVariant,
        contentColor: Color = COUITheme.colorScheme.onSecondaryVariant,
        disabledContentColor: Color = COUITheme.colorScheme.disabledOnSecondaryVariant,
    ): ButtonColors = remember(color, disabledColor, contentColor, disabledContentColor) {
        ButtonColors(
            color = color,
            disabledColor = disabledColor,
            contentColor = contentColor,
            disabledContentColor = disabledContentColor,
        )
    }
}
```

### Colors Data Class

```kotlin
@Immutable
data class ButtonColors(
    val color: Color,
    val disabledColor: Color,
    val contentColor: Color,
    val disabledContentColor: Color,
)
```

### Key Patterns

- **`rememberUpdatedState`** for values whose latest reading must be visible to a long-lived closure without re-running an effect or rebuilding a Modifier: use inside `LaunchedEffect`/`DisposableEffect` so the effect body sees the newest callback without being keyed on it, and inside `remember { }`-cached lambdas (e.g., the `onClick` you pass to `Modifier.clickable` after wrapping it in `remember`) so the cached body still calls the up-to-date callback. Note: this prevents _stale captures_, it does not stabilize the outer lambda's identity — a `Modifier.clickable { current() }` literal is still a fresh object on each composition. Do NOT use it when forwarding a callback directly to a child composable (e.g., `Button(onClick = onClick)`) — Compose's skip mechanism handles lambda stability there
- **`remember` with keys** for derived values: `val alpha = remember(enabled) { if (enabled) 1f else 0.38f }`
- **`@NonRestartableComposable`** on thin wrapper composables that fully delegate to other composables and read no state themselves; avoid on composables with multiple internal state reads (they benefit from smart recomposition)
- **`@Immutable`** on color/style data classes
- **Shapes**: Use `RoundedCornerShape(cornerRadius)` for rounded corners and `CircleShape` for capsules from `androidx.compose.foundation.shape`
- **Theme colors**: Always use `COUITheme.colorScheme.*`, never hardcode colors
- **Text styles**: Always use `COUITheme.textStyles.*` (e.g., `COUITheme.textStyles.button`)

## Critical Constraints

### Do NOT Replace Custom Layout in Component.kt

`BasicComponent` (`basic/Component.kt`) uses a custom `Layout` with intrinsic measurement and a weighted distribution algorithm (2:5:3). **Do NOT replace it with `Row + weight(1f)`** — this was tried and caused catastrophic layout breakage (text rendered as single vertical characters) when start/end content overflows.

### Performance

- `LaunchedEffect` keys: only include values actually read in the effect body
- `minIntrinsicWidth`/`maxIntrinsicWidth` triggers full subtree traversal — defer to overflow branch when possible
- Use `@Immutable` on truly immutable data classes (all `val`, never mutated, **and** no lambda/callback fields — lambda equality is reference-based and breaks `@Immutable`'s "equals stays equal forever" contract); use `@Stable` for data classes that hold lambdas/callbacks, or for classes whose mutable properties notify Compose via `MutableState`; `@Stable` is also the standard pattern for internal helper functions within `@Immutable` data classes (e.g., `@Stable internal fun color(enabled: Boolean): Color`)
- Standard collections (`List`, `Set`, `Map`) are unstable to Compose. Prefer `kotlinx.collections.immutable` (`ImmutableList`/`PersistentList`) — the Compose compiler recognizes them as stable. Wrapping a raw `List` in an `@Immutable` data class only works if the caller keeps the wrapper's identity stable (e.g., builds it inside a `remember`); otherwise the wrapper is "new" each composition and the `@Immutable` annotation is a false promise to Compose

## Workflows

### Adding a New Component

1. Create the `@Composable` function in `coui-ui/src/commonMain/kotlin/io/github/suqi8/coui/kmp/basic/` (or `preference/` in `coui-preference` for preference components)
2. Follow API conventions above (parameter ordering, Defaults object, Colors data class)
3. Add a behaviour or maths test under `coui-ui/src/desktopTest/` (or `coui-preference/src/desktopTest/`)
4. Add a demo section in `example/shared/src/commonMain/kotlin/component/`
5. Register the demo in the example app
6. Update `docs/components/` and `docs/zh_CN/components/` in both languages
7. Verify on at least Android and Desktop, and run `./gradlew apiDump` so the new public API is recorded

### Modifying a Component

When changing a component's API, defaults, or behavior, check and update all related artifacts:

1. **Documentation** (`docs/components/` and `docs/zh_CN/components/`): update properties tables, Defaults object sections, and code examples in both English and Chinese docs
2. **Docs demo code** (`docs/demo/`): update the interactive demo if it uses changed APIs
3. **Example app** (`example/shared/src/commonMain/kotlin/component/`): update demo code to reflect the changes
4. **API dump**: run `./gradlew apiDump` and commit the diff under `coui-*/api/`; `apiCheck` runs in CI
5. **Changelog**: record user-visible changes in `CHANGELOG.md`

### Fixing Bugs

1. Reproduce in the `example` app
2. Fix in `coui-ui/` or `coui-preference/`
3. Add a regression test that fails before the fix
4. If platform-specific, verify the fix across affected platforms

## Git Commit Style

Format: `<type>(<scope>): <subject>` ([Conventional Commits](https://www.conventionalcommits.org/)).

- Types: `feat`, `fix`, `perf`, `refactor`, `docs`, `chore`, `chore(release)`, `revert`. The `build`,
  `ci` and `test` types that appear on older commits are legacy — fold that work under `chore` now.
- Scopes: the library module or area (`coui-core`, `coui-ui`, `coui-preference`, `coui-shader`,
  `coui-blur`, `coui-squircle`, `coui-icons`, `coui-nav`, `coui-navigation3-ui`, `coui17`, `docs`,
  `build`); omit the scope for cross-cutting work.
- The subject is written in Chinese, in the imperative mood, and states the user-visible result
  rather than the process. Keep it within 40 characters with no trailing period — if it does not fit,
  the change is two commits.
- Write a body only when the reason for the change cannot be read off the subject and the code: the
  motivation, the constraint, the trade-off. No file lists, no step-by-step process, no version-bump
  narration.
- Never add signature lines (`Co-Authored-By`, "Generated with …", bot attributions).
- Check recent `git log --oneline` to stay consistent with current conventions
- **Run `./gradlew spotlessCheck` before every commit; only run `./gradlew spotlessApply` if violations are reported**
