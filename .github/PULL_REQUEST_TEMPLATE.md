<!--
Thanks for contributing. Keep this short — a description that says what changed and why, and the
checklist below. Delete the comment blocks before submitting.
-->

## What this changes

<!-- One or two sentences. The user-visible result, not the process. -->

## Why

<!-- Only if the reason cannot be read off the diff: the motivation, the constraint, the trade-off. -->

## Scope

- [ ] This is a change to this fork only
- [ ] This change also applies to upstream COUI (`suqi8/coui`) — maintainers will decide whether to propose it there

## Checklist

- [ ] `./gradlew spotlessCheck` passes
- [ ] `./gradlew apiCheck` passes (and `apiDump` output is committed if the public API changed)
- [ ] Desktop test suites pass (`:coui-ui:desktopTest`, `:coui-nav:desktopTest`)
- [ ] English and Chinese documentation updated, if a user-facing API or default changed
- [ ] `CHANGELOG.md` updated under the unreleased version, if the change is user-visible
- [ ] No unrelated refactors or reformatting

## Notes for the reviewer

<!-- Screenshots for visual changes, the ColorOS reference the values were taken from, or anything
     that is deliberately out of scope. -->
