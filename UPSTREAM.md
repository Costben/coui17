# Upstream and fork relationship

This repository is a **downstream fork**. It is not the canonical COUI project, and it does not
own the COUI name, the Maven coordinates or the published documentation site. This page records
where the code came from, what this fork changed, and what a maintainer taking over has to know.

## Lineage

| Order | Repository | Role |
| :--- | :--- | :--- |
| 1 | [`compose-miuix-ui/miuix`](https://github.com/compose-miuix-ui/miuix) | Original project. A MIUI/HyperOS-styled Compose Multiplatform UI library by YuKongA and the compose-miuix-ui contributors. |
| 2 | [`suqi8/coui`](https://github.com/suqi8/coui) | Forks Miuix, rethemes it to the ColorOS 16 (COUI) design language, renames the modules to `coui-*` and the package prefix to `io.github.suqi8.coui.kmp`, and publishes to Maven Central. **The direct upstream of this repository.** |
| 3 | this repository (`coui17`) | Forks `suqi8/coui` and continues the library towards the ColorOS 17 design language. |

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
| The demo shell `example/shared/src/commonMain/kotlin/coui17/` (Android app id `io.github.suqi8.coui17`) | This fork |
| `UPSTREAM.md`, the fork statements in `README.md` / `NOTICE` / the docs home pages | This fork |

Roughly 158 commits sit on top of the fork point, adding new components (side pane, list-detail,
grid spring chain, empty and complete states, guide dialog, page indicator, chip tab, floating
toolbar, spot light, and others) and recalibrating existing ones against ColorOS 17.

## Identity that still belongs to upstream

This is the part that most often confuses a newcomer, so it is stated explicitly:

| Thing | Current value | Owned by |
| :--- | :--- | :--- |
| Maven group and coordinates | `io.github.suqi8.coui.kmp` | `suqi8/coui` |
| Kotlin package prefix | `io.github.suqi8.coui.kmp.*` | `suqi8/coui` |
| Documentation site | <https://suqi8.github.io/coui/> | `suqi8/coui` |
| Dokka API reference | <https://suqi8.github.io/coui/dokka/index.html> | `suqi8/coui` |
| Maven Central artifacts | `io.github.suqi8.coui.kmp:*` | `suqi8/coui` |
| Security reporting channel | `suqi8/coui` private vulnerability reporting | `suqi8/coui` |
| POM `url`, `scm`, `developers` | `https://github.com/suqi8/coui`, developer `suqi8` | `suqi8/coui` |

Because of this, **a link to "the docs" or "the published artifact" from this repository resolves
to the upstream project, not to this fork's code.** That is deliberate for now: this fork has not
taken over the coordinates, and republishing under them from a different account would be wrong.

## Open decision: publish under its own coordinates

Making this repository a standalone project means, at minimum:

- a new Maven group and Kotlin package prefix, which is a breaking change for every consumer;
- regenerating the committed API dumps under `coui-*/api/` (`./gradlew apiDump`);
- repointing `docs/.vitepress/config.ts` (`base`, social link, `GitChangelog.repoURL`), the docs
  home pages, the README badges and `build-plugins/.../module.publication.gradle.kts` (POM `url`,
  `scm`, `developers`);
- repointing `.github/workflows/release.yml`, which verifies the staged bundle at the upstream
  Maven Central path;
- a `SECURITY.md` reporting channel this fork actually controls.

None of that has been done. It is tracked here so a successor does not have to rediscover it.

### What this fork does publish

`publishAllToGithubPackages` pushes the snapshot set to
`https://maven.pkg.github.com/Costben/coui17` under the inherited coordinates. That is a fork-local
snapshot channel for consumers of this fork, not a release: Maven Central, the version numbers there
and the `1.x` release train remain upstream's. The endpoint is overridable with `GITHUB_PACKAGES_URL`
or `-PcouiGithubPackagesUrl`, and pointing it at a `file:` URL runs the whole path — publish,
layout verification — without credentials:

```bash
./gradlew publishAndVerifyGithubPackages \
    -PcouiGithubPackagesUrl=file:///tmp/coui-github-packages
```

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
