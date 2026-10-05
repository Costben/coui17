# Third-party licenses

COUI itself is licensed under Apache-2.0 (see `LICENSE`). This page records the third-party
software that the published modules, the sample applications and the documentation site depend on,
together with the attributions those licenses require.

The direct dependencies below are declared in `gradle/libs.versions.toml` and
`docs/package.json`; the versions in use are pinned there. Transitive dependencies are shipped by
these projects under the same or a compatible license.

## Runtime dependencies (published modules)

| Component | License | Copyright / attribution |
| :--- | :--- | :--- |
| Kotlin standard library, `kotlinx-serialization`, `kotlinx-collections-immutable`, `kotlinx-coroutines` | Apache-2.0 | JetBrains s.r.o. |
| Compose Multiplatform (`org.jetbrains.compose.*`), JetBrains Compose Material 3 window size class | Apache-2.0 | JetBrains s.r.o. |
| AndroidX (`androidx.activity`, `androidx.collection`, `androidx.navigationevent`, `androidx.navigation3`) | Apache-2.0 | The Android Open Source Project |
| JetBrains AndroidX Lifecycle (`org.jetbrains.androidx.lifecycle`) | Apache-2.0 | JetBrains s.r.o. / The Android Open Source Project |
| `com.materialkolor:material-color-utilities` | Apache-2.0 | Material Design / Google LLC |
| `com.mikepenz:aboutlibraries-core` | Apache-2.0 | Mike Penz |
| Miuix — the upstream project COUI is forked from | Apache-2.0 | YuKongA and the compose-miuix-ui contributors |

## Build-time only

| Component | License |
| :--- | :--- |
| Android Gradle Plugin | Apache-2.0 |
| Spotless and ktlint (`com.diffplug.spotless`, `io.nlopez.compose.rules:ktlint`) | Apache-2.0 |
| JetBrains Dokka | Apache-2.0 |
| Kotlinx binary-compatibility-validator | Apache-2.0 |
| AndroidX Baseline Profile plugin, `androidx.test`, Espresso, UIAutomator, `benchmark-macro-junit4`, `androidx.profileinstaller` | Apache-2.0 |

## Transitive, compile-time only

These arrive through the Compose/Kotlin dependency graph and contribute annotations only; they are
not part of any COUI artifact's runtime behaviour.

| Component | License | Copyright / attribution |
| :--- | :--- | :--- |
| `dev.drewhamilton.poko:poko-annotations` | Apache-2.0 | Drew Hamilton |
| `org.jspecify:jspecify` | Apache-2.0 | the JSpecify authors |
| `androidx.arch.core:core-common` | Apache-2.0 | The Android Open Source Project |

## Documentation site

| Component | License |
| :--- | :--- |
| VitePress, Vue | MIT |
| `medium-zoom` | MIT |
| `vitepress-plugin-comment-with-giscus` | MIT |
| `@nolebase/vitepress-plugin-*` | MIT |
| MiSans font, loaded from `cdn-font.hyperos.mi.com` for the documentation site | MiSans font licence (Xiaomi) — the font is used unmodified and only as a web font on the documentation site; it is not redistributed inside any published artifact |

## Verifying the current lists

The Gradle dependency graph is the source of truth for what each module resolves:

```bash
./gradlew :coui-ui:dependencies --configuration desktopRuntimeClasspath
./gradlew :coui-nav:dependencies --configuration desktopRuntimeClasspath
```

To re-derive the coordinate set behind the tables above over every published module:

```bash
for m in coui-core coui-ui coui-icons coui-preference coui-blur coui-squircle coui-shader coui-nav coui-navigation3-ui; do
  ./gradlew ":$m:dependencies" --configuration desktopRuntimeClasspath -q
  ./gradlew ":$m:dependencies" --configuration desktopCompileClasspath -q
done | grep -oE '[a-z][a-z0-9_.-]*(\.[a-z0-9_.-]+)+:[a-z0-9_.-]+:[0-9][A-Za-z0-9_.+-]*' \
  | sed 's/:[0-9][^:]*$//' | sort -u
```

The documentation site's resolved tree is recorded in `docs/yarn.lock`.
