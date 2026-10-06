## COUI

A ColorOS-styled UI library for Compose Multiplatform, forked from
[COUI](https://github.com/suqi8/coui), which is itself based on
[Miuix](https://github.com/compose-miuix-ui/miuix).

> Components are calibrated against ColorOS 16 (COUI) design: colors, metrics, motion curves and press feedback are transcribed from the real system implementation.
>
> This library is experimental. APIs may change without notice.

> [!IMPORTANT]
> **This repository is a fork.** It is a downstream fork of
> [suqi8/coui](https://github.com/suqi8/coui) (COUI), which is itself a fork of
> [Miuix](https://github.com/compose-miuix-ui/miuix). The COUI name, the
> `io.github.suqi8.coui.kmp` Maven coordinates and the published documentation site belong to the
> upstream COUI project; this fork does not publish its own releases. [`UPSTREAM.md`](UPSTREAM.md)
> records the full lineage, what this fork changes, and how to sync with upstream.

[![Kotlin](https://img.shields.io/badge/kotlin-2.4.10-7F52FF)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/compose-1.11.1-4285F4)](https://kotlinlang.org/compose-multiplatform/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.suqi8.coui.kmp/coui-ui)](https://central.sonatype.com/search?q=g:io.github.suqi8.coui.kmp)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

### Supported Platforms

![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-Native-white?logo=apple)
![macOS](https://img.shields.io/badge/macOS-Native-white?logo=apple)
![Desktop](https://img.shields.io/badge/Desktop-JVM-007396?logo=openjdk)
![JsCanvas](https://img.shields.io/badge/Web-JsCanvas-F7DF1E?logo=javascript&logoColor=white)
![WasmJs](https://img.shields.io/badge/Web-WasmJs-654FF0?logo=webassembly&logoColor=white)

### Documentation & Demo

[![Docs](https://img.shields.io/badge/Docs-VitePress-3EAF7C?logo=vitepress&logoColor=white)](https://suqi8.github.io/coui/)
[![API Reference](https://img.shields.io/badge/API-Dokka-7F52FF?logo=kotlin&logoColor=white)](https://suqi8.github.io/coui/dokka/index.html)
[![WasmJs Demo](https://img.shields.io/badge/Demo-WasmJs-654FF0?logo=webassembly&logoColor=white)](https://suqi8.github.io/coui/example/)
[![Other](https://img.shields.io/badge/Demo-Other-white?logo=githubactions&logoColor=white)](https://github.com/suqi8/coui/actions/workflows/example.yml)

The API reference for every published module is served from
<https://suqi8.github.io/coui/dokka/index.html>; the Dokka HTML is generated at release time and
published together with the documentation site.

These links point at the site and the demo published by the upstream COUI project. This fork does
not host its own documentation or release artifacts — see [`UPSTREAM.md`](UPSTREAM.md).

### Repository layout

| Path | Contents |
| :--- | :--- |
| `coui-core/`, `coui-ui/`, `coui-preference/`, `coui-icons/`, `coui-blur/`, `coui-squircle/`, `coui-shader/`, `coui-nav/`, `coui-navigation3-ui/` | The nine published library modules |
| `coui-*/api/` | Committed binary-compatibility dumps, checked by `apiCheck` |
| `example/` | Sample applications: `shared`, `android`, `coui17`, `desktop`, `web`, `ios`, `macos` |
| `docs/` | VitePress documentation site, English at the root and Chinese under `docs/zh_CN/` |
| `build-plugins/` | Included Gradle build with the shared module conventions and the publication plugin |
| `baselineprofile/` | Baseline-profile generator for the Android artifacts |
| `verification/` | Standalone consumer projects that check compatibility and artifact completeness |
| `smoke/` | Minimal consumer that resolves the published coordinates |
| `CLAUDE.md`, `AGENT.md` | Conventions for AI coding assistants working in this repository |

### Building from source

The toolchain is pinned by Gradle: JDK 21, Android SDK `compileSdk` 37 / build-tools 37.0.0.

```bash
./gradlew spotlessCheck          # formatting (spotlessApply to fix)
./gradlew apiCheck               # binary compatibility against the committed dumps
./gradlew :coui-ui:desktopTest   # component tests
./gradlew :coui-nav:desktopTest  # navigation runtime tests
./gradlew check                  # everything the host platform can run
```

The documentation site and the sample applications have their own steps; both are covered in
[`CONTRIBUTING.md`](CONTRIBUTING.md).

### Modules

All modules are published to Maven Central under the group `io.github.suqi8.coui.kmp` and share the
same version number. Every module exposes the full target set: Android, iOS (`iosArm64`,
`iosSimulatorArm64`), macOS (`macosArm64`), Desktop JVM, JsCanvas and WasmJs.

| Module                | Maven coordinate                                | Description                                                                                              | Depends on              |
| --------------------- | ----------------------------------------------- | -------------------------------------------------------------------------------------------------------- | ----------------------- |
| `coui-ui`             | `io.github.suqi8.coui.kmp:coui-ui`              | Core UI component library (ColorOS 16 design language)                                                   | `coui-core`             |
| `coui-core`           | `io.github.suqi8.coui.kmp:coui-core`            | Shared primitives and platform utilities used by the other modules                                       | —                       |
| `coui-preference`     | `io.github.suqi8.coui.kmp:coui-preference`      | Preference components (`SwitchPreference`, `CheckboxPreference`, …)                                      | `coui-ui`               |
| `coui-icons`          | `io.github.suqi8.coui.kmp:coui-icons`           | Extended Material icon set, usable standalone or with `coui-ui`                                          | `coui-core`             |
| `coui-blur`           | `io.github.suqi8.coui.kmp:coui-blur`            | Backdrop blur / blur effect modifiers, usable standalone                                                | —                       |
| `coui-squircle`       | `io.github.suqi8.coui.kmp:coui-squircle`        | Squircle (continuous corner) shape, usable standalone                                                    | —                       |
| `coui-shader`         | `io.github.suqi8.coui.kmp:coui-shader`          | Runtime shader abstraction (`RuntimeShader`) with a no-op fallback on unsupported platforms             | —                       |
| `coui-nav`            | `io.github.suqi8.coui.kmp:coui-nav`             | Self-contained navigation runtime (stack, driver, back handling)                                         | —                       |
| `coui-navigation3-ui` | `io.github.suqi8.coui.kmp:coui-navigation3-ui`  | Navigation3 UI integration, usable standalone                                                            | —                       |

#### Android `minSdk`

Every published module declares `minSdk` 23, so a plain consumer of `coui-ui` or `coui-preference`
needs no `tools:overrideLibrary`:

| Module      | Android `minSdk` |
| ----------- | ---------------- |
| All modules | 23               |

`coui-blur`'s effects need `RuntimeShader`, which exists from API 33. On lower API levels the effects
are skipped and the content draws normally, so no module has to be left out of the dependency set.

### Getting Started

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.suqi8.coui.kmp:coui-ui:1.2.0")
            // Optional: Preference components (includes coui-ui)
            implementation("io.github.suqi8.coui.kmp:coui-preference:1.2.0")
            // Optional: Extended icon set
            implementation("io.github.suqi8.coui.kmp:coui-icons:1.2.0")
            // Optional: Blur effects (no-op below Android API 33)
            implementation("io.github.suqi8.coui.kmp:coui-blur:1.2.0")
            // Optional: Squircle shapes
            implementation("io.github.suqi8.coui.kmp:coui-squircle:1.2.0")
            // Optional: Runtime shaders
            implementation("io.github.suqi8.coui.kmp:coui-shader:1.2.0")
            // Optional: Navigation runtime
            implementation("io.github.suqi8.coui.kmp:coui-nav:1.2.0")
            // Optional: Navigation3 UI
            implementation("io.github.suqi8.coui.kmp:coui-navigation3-ui:1.2.0")
            // Other dependencies...
        }
        // Other sourceSets...
    }
    // Other configurations...
}
```

> `coui-core` is pulled in transitively by `coui-ui` and `coui-icons`; you normally do not need to
> declare it explicitly.

### Versioning & Stability

- **Status:** experimental. The library is usable, but the API surface is not frozen.
- **Versioning:** [Semantic Versioning](https://semver.org/). Patch releases contain bug fixes only,
  minor releases may add API, major releases may break API.
- **Breaking changes:** published only in major versions, and always documented in
  [`CHANGELOG.md`](CHANGELOG.md) with a migration note.
- **Deprecations:** an API is first marked `@Deprecated`, kept for at least one minor release, and only
  removed in the next major version.
- **Opt-in API:** APIs that are not yet stable carry an explicit `@RequiresOptIn` marker and require an
  opt-in at the call site; the marker documents that the API may change in a minor release.
  `@ExperimentalScrollBarApi` and `@ExperimentalCOUIApi` are the current examples.
- **Binary compatibility:** every release is checked with the Kotlin Binary Compatibility Validator;
  the accepted API surface is committed under `coui-*/api/`. The dumps cover the JVM surface, i.e.
  the desktop and Android artifacts; Kotlin/Native and web consumers recompile from source, so
  source compatibility is the contract that applies to them.

### Usage

- Provide a color scheme via `COUITheme(colors = ...)`, e.g., `lightColorScheme()` or `darkColorScheme()`.

```kotlin
@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    return COUITheme(
        colors = colors,
        content = content
    )
}
```

- Use `ThemeController` to manage modes and enable Monet dynamic colors. Pass `keyColor` to set a custom seed color.

```kotlin
@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val controller = remember {
        ThemeController(
            ColorSchemeMode.MonetSystem,
            keyColor = Color(0xFF3482FF)
        )
    }
    return COUITheme(
        controller = controller,
        content = content
    )
}
```

### Credits

This library is the work of three projects in a row, all licensed under Apache-2.0:

1. [**Miuix**](https://github.com/compose-miuix-ui/miuix) — the original Compose Multiplatform UI
   library, by YuKongA and the compose-miuix-ui contributors. This fork's history begins there, and
   most of the component implementations, build tooling and documentation structure originate from
   it.
2. [**COUI**](https://github.com/suqi8/coui) — by suqi8. Rethemed Miuix to the ColorOS 16 design
   language, renamed the modules to `coui-*` and the package prefix to
   `io.github.suqi8.coui.kmp`, and took over publication. This fork starts from COUI 1.1.0.
3. **This fork** — continues COUI towards the ColorOS 17 design language and adds the `coui17`
   sample application.

[`UPSTREAM.md`](UPSTREAM.md) describes the relationship in full.
