# Contributing to COUI

Thanks for taking the time to contribute. COUI is a ColorOS-flavoured UI library for Compose
Multiplatform; this document describes how to build it, how changes are reviewed, and what a
complete contribution looks like.

## Before you start

- Read [`CLAUDE.md`](CLAUDE.md) for the code style, API conventions and performance constraints.
- Read [`AGENT.md`](AGENT.md) if your change touches theming, colours or component metrics.
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

- Types in use: `feat`, `fix`, `perf`, `refactor`, `docs`, `build`, `chore`, `test`.
- Scopes in use: `coui-core`, `coui-ui`, `coui-preference`, `coui-shader`, `coui-blur`,
  `coui-squircle`, `coui-icons`, `coui-nav`, `coui-navigation3-ui`, `coui17`, `docs`, `build`.
- The subject describes the user-visible result, uses the imperative mood, and carries no trailing
  period. Commits in this repository are written in Chinese.
- Do not add signature lines (`Co-Authored-By`, "Generated with …", bot attributions).

## Pull requests

A pull request is expected to:

- pass `spotlessCheck`, `apiCheck` and the desktop test suites;
- compile for Android, iOS, Desktop and Web (the CI matrix covers all four);
- include English and Chinese documentation when a user-facing API or default changes;
- update `CHANGELOG.md` under the unreleased version when the change is user-visible;
- avoid unrelated refactors and reformatting.

Small, focused pull requests are reviewed much faster than large mixed ones.
