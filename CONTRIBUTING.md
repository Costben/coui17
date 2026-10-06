# Contributing to COUI

Thanks for taking the time to contribute. COUI is a ColorOS-flavoured UI library for Compose
Multiplatform; this document describes how to build it, how changes are reviewed, and what a
complete contribution looks like.

## Before you start

- Read [`UPSTREAM.md`](UPSTREAM.md) first. This repository is a downstream fork of
  [`suqi8/coui`](https://github.com/suqi8/coui): it does not own the COUI name, the
  `io.github.suqi8.coui.kmp` coordinates or the published documentation site, and changes that
  belong upstream may have to be proposed there instead.
- Read [`CLAUDE.md`](CLAUDE.md) for the code style, API conventions and performance constraints.
  It is written for AI coding assistants, but it is the authoritative engineering convention for
  human contributors too. [`AGENT.md`](AGENT.md) covers the design-sourcing rules that apply when a
  change touches theming, colours or component metrics.
- Read [`SECURITY.md`](SECURITY.md) before reporting anything that looks like a vulnerability.

## Requirements

| Tool | Version |
| :--- | :--- |
| JDK | 21 (the Gradle toolchain pins 21 for every module) |
| Android SDK | compileSdk 37, build-tools 37.0.0 |
| Node.js | see `docs/.nvmrc` (only needed for the documentation site) |
| Yarn | managed by Corepack, pinned through `packageManager` in `docs/package.json` |

## Build and test

```bash
./gradlew spotlessCheck          # formatting (run spotlessApply to fix)
./gradlew apiCheck               # binary compatibility against the committed API dumps
./gradlew :coui-ui:desktopTest   # desktop component tests
./gradlew :coui-nav:desktopTest  # navigation runtime tests
./gradlew check                  # everything the host platform can run
```

Documentation:

```bash
cd docs
corepack enable
yarn install --immutable
yarn docs:build
```

## Changing a component's public API

1. Implement the change in `commonMain`; keep it additive with defaults whenever possible.
2. Add or extend a test. `coui-ui` and `coui-nav` keep their behavioural tests in `desktopTest`.
3. Update both language versions of the component page (`docs/components/…` and
   `docs/zh_CN/components/…`).
4. Update the demo in `example/shared/src/commonMain/kotlin/component/…`.
5. Generate the API dump and commit it: `./gradlew apiDump`.

`apiCheck` fails the build when the public ABI changes without a matching dump update. A change
that breaks the published ABI needs a major version bump and an entry in `CHANGELOG.md`; do not
silence the check.

The committed dumps are JVM dumps, so they guard the desktop artifact and — because the Android
artifact is built from the same `commonMain` — the same public surface there.
`verification/compat-consumer` extends that guard end to end: it compiles a 1.1.0-shaped consumer
against both releases and runs the 1.1.0-compiled bytecode against the candidate, so a compatibility
entry point cannot be dropped by accident. Kotlin/Native (iOS, macOS) and web have no stable ABI
contract — those consumers recompile from source — so source compatibility is what applies there.
What does apply to them is artifact completeness, and `verification/apple-web-consumer` checks it: a
downstream Kotlin Multiplatform project resolves the nine modules for `iosArm64`, `iosSimulatorArm64`,
`macosArm64`, `js` and `wasmJs` from the repository under test, compiles against them and links the
iOS and macOS frameworks. All three consumer projects are separate Gradle builds run through the root
wrapper, e.g. `bash verification/apple-web-consumer/verify.sh <version> <repository-url>`. They are
gated in CI on the staged candidate; the Apple/web one needs a macOS host, so it runs in the release
workflow's macOS publish job rather than on every pull request.

## Style

- Formatting is enforced by Spotless + ktlint with the Compose rules. Run `./gradlew spotlessCheck`
  before committing; only run `spotlessApply` when the check reports violations.
- Every `.kt` and `.kts` file starts with:

  ```kotlin
  // Copyright <year>, compose-coui-ui contributors
  // SPDX-License-Identifier: Apache-2.0
  ```

  Spotless fills the year in; do not hand-edit the year on existing files.
- Code, comments and KDoc are written in English.

## Commits

Commits follow [Conventional Commits](https://www.conventionalcommits.org/): `<type>(<scope>): <subject>`.

- Types: `feat`, `fix`, `perf`, `refactor`, `docs`, `chore`, `chore(release)`, `revert`. The `build`,
  `ci` and `test` types on older commits are legacy; fold that work under `chore` for new commits.
- Scopes: `coui-core`, `coui-ui`, `coui-preference`, `coui-shader`, `coui-blur`, `coui-squircle`,
  `coui-icons`, `coui-nav`, `coui-navigation3-ui`, `coui17`, `docs`, `build`. Omit the scope for
  cross-cutting work.
- The subject is written in Chinese, in the imperative mood, and states the user-visible result
  rather than the process. Keep it within 40 characters with no trailing period — if it does not fit,
  the change is two commits.
- Write a body only when the reason for the change cannot be read off the subject and the code: the
  motivation, the constraint, the trade-off. No file lists, no step-by-step process, no version-bump
  narration.
- Do not add signature lines (`Co-Authored-By`, "Generated with …", bot attributions).

## Issues

Use the [issue forms](.github/ISSUE_TEMPLATE) — bug reports and feature requests each have one.
Include the COUI version, the platform and a minimal reproduction; a report that cannot be
reproduced cannot be acted on. Security problems do not go through the public tracker, see
[`SECURITY.md`](SECURITY.md).

Because this repository is a fork, say which repository you are reporting against: a bug in code
that has not diverged from upstream may need to be reported to
[`suqi8/coui`](https://github.com/suqi8/coui) instead.

## Pull requests

Open the pull request against this repository's `main` branch and fill in the template. If the
change also applies to upstream COUI, say so in the description — whether to propose it upstream is
a maintainer decision, not a reason to open two pull requests.

A pull request is expected to:

- pass `spotlessCheck`, `apiCheck` and the desktop test suites;
- compile for Android, iOS, Desktop and Web (the CI matrix covers all four);
- include English and Chinese documentation when a user-facing API or default changes;
- update `CHANGELOG.md` under the unreleased version when the change is user-visible;
- avoid unrelated refactors and reformatting.

Small, focused pull requests are reviewed much faster than large mixed ones.
