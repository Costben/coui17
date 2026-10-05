# Security Policy

## Supported versions

Security fixes are published for the latest stable release line only. Older releases are not
maintained; upgrading to the newest published version is the supported path.

| Version | Supported |
| :--- | :--- |
| Latest `1.x` release | Yes |
| Older releases | No |

Snapshots published from `main` are development builds and are not covered by this policy.

## Reporting a vulnerability

Please do not open a public issue for a security problem.

Use GitHub's private vulnerability reporting on the
[`suqi8/coui`](https://github.com/suqi8/coui) repository (**Security → Report a vulnerability**).
If you cannot use that channel, contact a maintainer directly and ask for a private channel before
sharing details.

A useful report contains:

- the affected artifact and version (`io.github.suqi8.coui.kmp:*`);
- the platform (Android / iOS / macOS / Desktop / Web) and OS version;
- a minimal reproduction, and the impact you believe it has;
- any suggested fix or mitigation.

## What is in scope

- The nine published library modules: `coui-core`, `coui-ui`, `coui-preference`, `coui-shader`,
  `coui-blur`, `coui-squircle`, `coui-icons`, `coui-navigation3-ui`, `coui-nav`.
- The published build and release pipeline (signing, publication metadata, CI configuration).

Out of scope: the sample applications under `example/`, the documentation site, and any local
tooling that is not published.

## Our commitment

- We acknowledge a report within 5 working days.
- We aim to provide an initial assessment, including whether we consider the report in scope,
  within 10 working days.
- We will credit reporters in the release notes unless they ask to stay anonymous.
- We ask that you give us a reasonable window to ship a fix before publishing details.
