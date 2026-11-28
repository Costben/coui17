# Upstream and fork relationship

This repository (`Costben/coui17`) is a **ColorOS 17 drop-in downstream fork** of
[`suqi8/coui`](https://github.com/suqi8/coui). It publishes under its own Maven coordinates
(`io.github.costben.coui17`) and documentation site (<https://costben.github.io/coui17/>), while
deliberately keeping the Kotlin package prefix (`io.github.suqi8.coui.kmp.*`) and Android library
`namespace` compatible with upstream COUI. This page records where the code came from, what this
fork changed, and what a maintainer taking over has to know.

## Lineage

| Order | Repository | Role |
| :--- | :--- | :--- |
| 1 | [`compose-miuix-ui/miuix`](https://github.com/compose-miuix-ui/miuix) | Original project. A MIUI/HyperOS-styled Compose Multiplatform UI library by YuKongA and the compose-miuix-ui contributors. |
| 2 | [`suqi8/coui`](https://github.com/suqi8/coui) | Forks Miuix, rethemes it to the ColorOS 16 (COUI) design language, renames the modules to `coui-*` and the package prefix to `io.github.suqi8.coui.kmp`, and publishes to Maven Central. **The direct upstream of this repository.** |
| 3 | [`Costben/coui17`](https://github.com/Costben/coui17) | Forks `suqi8/coui`, continues the library towards the ColorOS 17 design language, and publishes drop-in compatible artifacts under `io.github.costben.coui17`. |

Both ancestors are Apache-2.0. The attribution obligations that follow from that are recorded in
[`NOTICE`](NOTICE) and [`THIRD-PARTY-LICENSES.md`](THIRD-PARTY-LICENSES.md).

## Fork point

This fork branched from `suqi8/coui` at commit `e4e915ca` (2026-08-04, the COUI 1.1.0 release).
Every commit up to and including that one is upstream work by the Miuix and COUI contributors;
everything after it is this fork's. The full upstream history is preserved, so `git log`,
`git blame` and `git bisect` still resolve against it.

## What is inherited and what is this fork's

| Area | Origin |
| :--- | :--- |
| The nine `coui-*` library modules and their public API | Inherited |
| Component implementations, ColorOS 16 metrics, theme and text styles | Inherited |
| Documentation site scaffolding and the 134 component pages in both languages | Inherited |
| Build tooling, publication plugin, release workflows, verification projects | Inherited |
| ColorOS 17 metrics, component additions and reworked bars | This fork |
| The demo shell `example/shared/src/commonMain/kotlin/coui17/` (Android app id & iOS bundle id `io.github.costben.coui17`) | This fork |
| Independent Maven group (`io.github.costben.coui17`), POM metadata, docs site, and security channel | This fork |
| `UPSTREAM.md`, the fork statements in `README.md` / `NOTICE` / the docs home pages | This fork |

Roughly 158+ commits sit on top of the fork point, adding new components (side pane, list-detail,
grid spring chain, empty and complete states, guide dialog, page indicator, chip tab, floating
toolbar, spot light, and others) and recalibrating existing ones against ColorOS 17.

## Identity and drop-in compatibility boundary

This fork separates **publication identity** from **source/ABI compatibility**:

| Thing | Current value | Owned by / Policy |
| :--- | :--- | :--- |
| Maven group and coordinates | `io.github.costben.coui17:*` | `Costben/coui17` |
| POM `url`, `scm`, `developers` | `https://github.com/Costben/coui17`, developer `Costben` | `Costben/coui17` |
| Documentation site | <https://costben.github.io/coui17/> | `Costben/coui17` |
| Dokka API reference | <https://costben.github.io/coui17/dokka/index.html> | `Costben/coui17` |
| Security & conduct reporting channel | `Costben/coui17` private vulnerability reporting | `Costben/coui17` |
| Demo Android `applicationId` & iOS `BUNDLE_ID` | `io.github.costben.coui17` | `Costben/coui17` |
| Kotlin package prefix | `io.github.suqi8.coui.kmp.*` | Kept unchanged for drop-in source/binary compatibility |
| Android library `namespace` | `io.github.suqi8.coui.kmp.*` | Kept unchanged for drop-in compatibility |
| Upstream Maven Central artifacts | `io.github.suqi8.coui.kmp:*` | `suqi8/coui` (verified against `1.1.0` in `verification/compat-consumer`) |

Keeping the Kotlin package prefix and Android library `namespace` at `io.github.suqi8.coui.kmp.*`
means downstream apps migrating from upstream `suqi8/coui` only change the Maven `groupId` in their
Gradle files — zero Kotlin import rewrites, and JVM binary compatibility against upstream `1.1.0`
remains continuously verified by `verification/compat-consumer`.

### What this fork publishes

Every push to `main` runs `.github/workflows/publish.yml` on macOS, publishing the all-platform
`io.github.costben.coui17:*` snapshot set (`-ProllingSnapshot`, fixed version `1.2.0-SNAPSHOT`) to
the single-commit orphan branch `maven-repo` under `coui-repo/`. Consumers resolve it directly with
no authentication token:

```kotlin
maven("https://raw.githubusercontent.com/Costben/coui17/maven-repo/coui-repo")
```

Force-pushing a single-commit orphan branch replaces the snapshot in place without accumulating
binary history in the repository. Maintainers can also trigger the same extraction and push locally
via `./scripts/publish-maven-repo.sh` (or `--no-build` to push an existing `build/repository/local`).

## Syncing with upstream

The `upstream` remote is already configured:

```bash
git fetch upstream
git log --oneline main..upstream/main     # what upstream has that this fork does not
git merge upstream/main                   # or: git rebase upstream/main
```

Upstream may have moved on since the fork point, so expect conflicts: the fork recalibrated many of
the same component files that upstream also owns. Merge deliberately rather than taking a whole
side, and re-run `./gradlew spotlessCheck apiCheck :coui-ui:desktopTest :coui-nav:desktopTest`
afterwards. Note that `main` and `upstream/main` diverged at the fork point, so the diff above is
the complete set of upstream commits this fork has not taken.

## Licensing

The library is Apache-2.0. A fork that redistributes it must keep the upstream copyright and
attribution notices; `NOTICE` and `THIRD-PARTY-LICENSES.md` carry them. Do not remove the Miuix or
COUI entries when editing those files.
