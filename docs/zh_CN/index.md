---
# https://vitepress.dev/reference/default-theme-home-page
layout: home

hero:
  name: COUI
  text: A UI library for Compose MultiPlatform
  tagline: 提供 ColorOS 设计风格的组件库
  image:
    src: /Icon.webp
    alt: Logo
  actions:
    - theme: brand
      text: 快速开始
      link: /zh_CN/guide/getting-started
    - theme: alt
      text: 组件库
      link: /zh_CN/components/index
    - theme: alt
      text: GitHub
      link: https://github.com/Costben/coui17

features:
  - icon: 🚀
    title: 简单易用
    details: 类似 Compose Material 的使用方式，低学习成本，快速上手。
  - icon: 🎨
    title: ColorOS 美学
    details: 精确还原 ColorOS 原生组件的设计与交互体验。
  - icon: 📦
    title: 组件丰富
    details: 提供全面的基础组件和部分复杂的交互组件，满足大多数应用开发需求。
  - icon: 🌐
    title: 全平台支持
    details: 基于 Compose Multiplatform，一套代码支持 Android、iOS、Desktop 等平台。
  - icon: 🧬
    title: 基于 Miuix
    details: 基于优秀的 Miuix Compose Multiplatform UI 组件库构建。
    link: https://github.com/compose-miuix-ui/miuix
    linkText: 访问 Miuix

nolebase:
  gitChangelog: false
  pageProperties: false
---

::: info 关于本分支
本文档站属于 [`Costben/coui17`](https://github.com/Costben/coui17)，它是上游 COUI 项目
[`suqi8/coui`](https://github.com/suqi8/coui)（基于
[Miuix](https://github.com/compose-miuix-ui/miuix)）的 **ColorOS 17 Drop-in Fork**。本分支以独立
Maven 坐标（`io.github.costben.coui17:*`）发布，同时保持 `io.github.suqi8.coui.kmp.*` Kotlin 包名与
Android `namespace` 不变以实现零改码兼容。仓库根目录的 `UPSTREAM.md` 记录了完整的上游血缘、版权归属与同步方式。
:::
