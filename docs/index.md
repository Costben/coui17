---
# https://vitepress.dev/reference/default-theme-home-page
layout: home

hero:
  name: COUI
  text: A UI library for Compose MultiPlatform
  tagline: Provides components with ColorOS design style
  image:
    src: /Icon.webp
    alt: Logo
  actions:
    - theme: brand
      text: Getting Started
      link: /guide/getting-started
    - theme: alt
      text: Components
      link: /components/index
    - theme: alt
      text: GitHub
      link: https://github.com/Costben/coui17

features:
  - icon: 🚀
    title: Easy to Use
    details: Similar to Compose Material with low learning curve for quick integration.
  - icon: 🎨
    title: ColorOS Aesthetics
    details: Authentic ColorOS design language and interactive effects for your apps.
  - icon: 📦
    title: Rich Components
    details: Comprehensive basic and complex components for most application scenarios.
  - icon: 🌐
    title: Cross-Platform
    details: One codebase for Android, iOS, Desktop and more via Compose Multiplatform.
  - icon: 🧬
    title: Based on Miuix
    details: Built on the excellent Miuix Compose Multiplatform UI library.
    link: https://github.com/compose-miuix-ui/miuix
    linkText: Visit Miuix

nolebase:
  gitChangelog: false
  pageProperties: false
---

::: info About this fork
This site documents [`Costben/coui17`](https://github.com/Costben/coui17), a **ColorOS 17 drop-in
fork** of [`suqi8/coui`](https://github.com/suqi8/coui) (which is based on
[Miuix](https://github.com/compose-miuix-ui/miuix)). It publishes under independent Maven
coordinates (`io.github.costben.coui17:*`) while preserving the `io.github.suqi8.coui.kmp.*` Kotlin
package prefix and Android `namespace` for drop-in compatibility. See `UPSTREAM.md` in the
repository for full lineage, attribution, and sync details.
:::
