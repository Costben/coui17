# 踩坑记录

BiliCO（`erl.bilico`）与 coui 联调期间**真实踩过**的坑。每条按「现象 → 根因 → 做法」写，能给命令的直接给命令。

规则只有一条：**复发过的才写进来**。这里不是设计文档，是止损清单。

---

## 构建与依赖

### 1. 改了 coui，BiliCO 装出来还是旧行为

**现象**：coui 源码改了、`assembleRelease` 也成功，装到机器上行为没变。

**根因**：BiliCO 走本地 Maven 快照（`1.2.0-SNAPSHOT`），要过三关才会生效：

1. coui 重新发布到 `coui/build/repository/local`；
2. 把发布结果同步到 `../coui-local-repo`（BiliCO 的 `settings.gradle.kts` 两处都会解析）；
3. 清掉 Gradle 对快照的模块缓存，否则它认旧的模块描述符。

**做法**：

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
cd /Users/rinshibuya/Downloads/Erl-Project/coui
./gradlew -ProllingSnapshot :coui-ui:publishAndroidPublicationToLocalRepository \
  :coui-ui:publishKotlinMultiplatformPublicationToLocalRepository --console=plain

R=/Users/rinshibuya/Downloads/Erl-Project/coui/build/repository/local/io/github/suqi8/coui/kmp
L=/Users/rinshibuya/Downloads/Erl-Project/coui-local-repo/io/github/suqi8/coui/kmp
rm -rf "$L"/coui-ui*
cp -R "$R"/coui-ui* "$L"/

rm -rf ~/.gradle/caches/modules-2/files-2.1/io.github.suqi8.coui.kmp \
       ~/.gradle/caches/modules-2/metadata-*/descriptors/io.github.suqi8.coui.kmp

cd /Users/rinshibuya/Downloads/Erl-Project/BiliCO
./gradlew assembleRelease --offline --console=plain
```

**怎么确认发布里真的含这次改动**：比时间戳，别猜。

```bash
stat -f "%Sm %N" -t "%Y-%m-%d %H:%M:%S" \
  coui/coui-ui/src/.../被改的文件.kt \
  coui-local-repo/io/github/suqi8/coui/kmp/coui-ui-android/1.2.0-SNAPSHOT/*.aar
```

AAR 的时间晚于源文件，才算发布到位。

### 2. `--refresh-dependencies` 不带 `--offline` 会卡死

**现象**：`./gradlew assembleRelease --refresh-dependencies` 十几分钟无输出，卡在 task graph / 依赖解析。

**做法**：不要用它。清缓存（见上一条）再 `--offline`，几十秒结束。

### 3. 快照只覆盖 `.aar` 不够

`coui-ui` 有多个 artifact（`coui-ui`、`coui-ui-android`），每个目录里 `aar` / `pom` / `module` / `maven-metadata.xml` 以及各自的 `md5` / `sha1` / `sha256` / `sha512` 是一整套。只拷 `aar` 会解析失败，或者静默退回到旧版本。

同步时按 artifact 整目录覆盖：`rm -rf "$L"/coui-ui*` 再 `cp -R`。

### 4. 改 coui 公开 API 必须先 `apiDump`

**现象**：`./gradlew :coui-ui:desktopApiCheck` 直接红。

**做法**：

```bash
./gradlew :coui-ui:apiDump        # 会同时跑 desktopApiDump 与 klibApiDump
git diff coui-ui/api/             # 确认只多了预期的签名
```

提交前跑的门禁：`:coui-ui:spotlessApply`、`:coui-ui:spotlessCheck`、`:coui-ui:desktopApiCheck`、`:coui-ui:desktopTest`。

---

## 设备与安装

### 5. 增量安装会让 LSPosed 的 system 钩子静默失效

**规矩**：一律 `adb -s SERIAL install -r --no-incremental APK`。禁止 `--incremental`，禁止 `uninstall`，禁止 `pm clear`，禁止删应用数据。

**现象**：装的时候 `Success`，模块日志里却是：

```
W/ziparchive   Unable to open '.../base.apk': No such file or directory
E/app_process  Failed to open APK '.../base.apk': I/O error
W/LSPosedService Failure to parse package archive '.../base.apk'
```

**根因**：增量安装把 APK 挂在 `incremental-fs` 上，`/data/app/~~xxxx` 不再是普通目录，system_server 早期解析不到它。

**装完必须核对落点，不能只看 `Success`**：

```bash
adb -s SERIAL shell "cat /proc/mounts" | grep incremental-fs   # 应为空
adb -s SERIAL shell pm path erl.bilico                          # 应落在 /data/app/~~.../base.apk
```

`pm path` 的路径若在某个 `incremental-fs` 挂载点之下，必须用 `--no-incremental` 重装。

### 6. `192.168.31.216:5555` 不是我们的设备

`adb devices` 里常年挂着三台。装包前先确认序列号：

- **165** `192.168.31.165:5555`：只装不测，用户自己看。
- **132** `192.168.31.132:5555`：验证机，锁屏 `000000`。
- **216** `192.168.31.216:5555`：**别人的，别装**。

### 7. 设备产物只落 `/sdcard/dev-dump/`

UI dump、截图、日志只允许写进：

```
/sdcard/dev-dump/erl.bilico/<yyyyMMdd-HHmmss>/
```

不许写 `/sdcard` 根目录，也不许写 `DCIM` / `Pictures` / `Download` / `Documents` / `Movies`。任务结束删掉本次目录。

能流回本机就别落设备：`adb exec-out screencap -p > /tmp/shot.png`。

### 8. 虚拟屏用完必须关

- 列表接口是 `GET http://192.168.31.132:8888/api/vscreen`（返回 `{"ok":true,"screens":[...]}`）。`/api/vscreen/screens` 是 **404**，别用。
- 每块屏创建时发一把 `screenToken`，只返回一次；launch / frame / ui / input / close 都要带。
- 用完 `close`，并确认 `screens: []`。
- 丢了 token 的孤儿屏：`adb shell su -c 'kill <vscreen daemon pid>'`，daemon 会自己重启并释放屏。

---

## 顶栏与玻璃

### 9. 胶囊采样必须采「材质之后」

**现象**（浅色模式，搜索结果页）：黑色视频封面从顶栏下经过时，筛选胶囊左半边发黑、「视频」胶囊偏粉，而同一张封面在胶囊之间的顶栏区域只是浅灰。

**根因**：顶栏和胶囊采的是**同一份原始列表**。顶栏叠了 80% 容器色 + 模糊，胶囊只作了未选中洗色（约 8% `ColorDodge` + 8% `Luminosity`）。这组便签配色的前提是「胶囊采样的是顶栏材质之后的画面」，跳过顶栏那层白就压不住黑封面。

**做法**：把顶栏材质节点单独录进一个 `LayerBackdrop`，胶囊采样它。

```kotlin
val materialBackdrop = rememberLayerBackdrop(upstream = pageBackdrop) {
    if (scrollFraction() < 0.999f) drawRect(pageColor)  // 未完全显形时垫底色，别给半透明画面
    drawContent()
}
Box(Modifier.fillMaxWidth().height(barHeight + extraHeight).layerBackdrop(materialBackdrop)) {
    AppBarBlurOverlay(modifier = Modifier.height(barHeight), platformRenderer = false) { /* 空 */ }
}
// 两行画在录制之外，胶囊采样 materialBackdrop
```

coui 里这条已经封成 `SearchFilterTopAppBar`，新页面直接调用它，别再手搓。

### 10. 自采样反馈环

录制层里**绝不能包含会采样它的东西**（胶囊、行内容本身），否则自己喂自己，画面越滚越糊/发黑。

由此推出 `TwoRowTopAppBar` 不能拿去当搜索页的栏：它的 `secondRow` 画在材质内容里，胶囊会在录制范围内。

### 11. 下边缘高光必须和模糊在同一层

**现象**：搜索栏下边缘单独亮一条，能在那条高光里看见背后的内容，而周围一圈高光之外又是模糊的，看起来像贴上去的一层。

**做法**：高光与模糊画在同一层、同一份采样上，不要给边缘单独开一层玻璃。（对应 commit `45af31d1`：让搜索栏下边缘高光与胶囊共用同一次模糊。）

### 12. `platformRenderer = true` 与自录 backdrop 互斥

平台材质采的是**整个窗口**。页面里一旦自己录了 backdrop，平台效果会把自己的输出吃回去——滚动时丢帧、发黑。

**做法**：页面自录材质时一律 `platformRenderer = false`，走 coui 引擎采样录制层。

### 13. 密度：相册和便签不是一回事

**现象**：BiliCO 的胶囊比便签小一截（252×112 px 对 279×124 px，差 10.7%）。

**根因**：`rememberGalleryDensity()` 取的是**物理 560dpi**——这是照抄 ColorOS **相册**的行为（相册忽略系统「显示大小」）。便签不这么干，跟着系统 620 走。机器读数：`Physical density 560 / Override density 620`。

**做法**：这是有意的取舍，不是 bug。BiliCO 的顶栏/底栏钉在 560（`PinnedBarDensity`），页面内容跟系统。**要改先确认参照 App 的行为**，改完给 gfxinfo 对比；只凭「看起来小」就改会翻车（试过一版跟随系统，用户看过之后否掉了）。

### 14. `TopAppBarSearchBar` 是整条工具栏，不是输入框

它自带标题、动作区、展开/收起和返回键处理。放进两行栏的第一行时，只负责槽位与高度（`TwoRowTopAppBarDefaults.FirstRowHeight` 52dp 里居中），不要再套一层自己的内边距，否则会和状态栏 inset 叠加。

### 15. 组件里录材质时，渐隐带不能被布局约束裁掉

材质节点比栏高 `extraHeight`。用 `Layout` 时，材质子节点要按**无界高度**测量，否则 `extraHeight` 那条渐隐带被裁在栏的下沿，接缝会出现一条硬边：

```kotlin
Layout(...) { measurables, constraints ->
    val material = measurables[0].measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
    val rows = measurables[1].measure(constraints.copy(minHeight = 0))
    layout(rows.width, rows.height) {   // 只报两行之高，渐隐带不占布局
        material.placeRelative(0, 0)
        rows.placeRelative(0, 0)
    }
}
```

### 16. 一个 `LayerBackdrop` 只能被一个节点录制

`Modifier.layerBackdrop` 是单属主的：同一个 backdrop 挂到多个节点上，只有最近附加的那个在录。**不要**把同一个 backdrop 铺到 `HorizontalPager` 的每一页上——页会抢属主，模糊可能采到屏外邻居。

---

## Compose / 文本

### 17. 光标手柄溢出胶囊，还会逐帧闪

**现象**：点一下搜索框，蓝色手柄在胶囊下沿一明一灭地跳。

**根因**（三层叠在一起，缺一层都不成立）：

1. `BasicTextField(value: String)` 走 `CoreTextField`，它在 `Modifier.onGloballyPositioned` 里**每帧**重算 `showCursorHandle`，判据是 `visibleBounds().containsInclusive(getHandlePosition())`——对边界极其敏感；
2. 搜索栏的 `couiPressMotion` 弹簧每帧移动输入节点（实测 `positionInWindow` 56.0 → 58.5 → 56.0）；
3. 手柄 25dp（实测弹窗 80×97px @620），锚在光标底部**向下**画，溢出 40dp 胶囊约 15dp，且走独立 `Popup` 窗口——光标不闪，它闪。

**做法**：把选中手柄设为透明，选中底色沿用外层：

```kotlin
val inherited = LocalTextSelectionColors.current
val selectionColors = remember(inherited) {
    TextSelectionColors(
        handleColor = Color.Transparent,
        backgroundColor = inherited.backgroundColor,
    )
}
CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
    BasicTextField(...)
}
```

### 18. 光标 1 秒一闪是正常的

实测周期 **1.000s ± 0.02**（500ms 亮 / 500ms 灭），和系统一致。**别去"修"**。曾经因为手柄闪去怀疑光标闪烁，白查了一轮。

### 19. 别用「看见抖动就调参」的思路

先量。`positionInWindow` 逐帧值、节点尺寸、像素差异，量出来再动手。上面那条手柄问题，靠逐帧截图差分定位（改前单帧 +2381px 整块跳变，改后最大 426px 只剩光标竖线）才一次改对。

---

## 验证方法

### 20. 动骨架/列表必须给 gfxinfo 前后对比

```bash
adb -s 192.168.31.132:5555 shell dumpsys gfxinfo erl.bilico reset
# 同样滚动一段
adb -s 192.168.31.132:5555 shell dumpsys gfxinfo erl.bilico
```

报 **janky frames 比例**和 **50 / 90 / 99 分位帧时间**。性能变差就如实说，不要藏。

### 21. 颜色问题给像素数字，不要只给「看起来好了」

用 Python 取均值亮度：胶囊内部取一块，紧邻它的顶栏区域取同一横坐标、同一封面上方的一块。修复后**胶囊内部亮度 ≥ 周围顶栏亮度**，且不再出现黑块 / 粉色透色。

改前先抓一张对照，没有对照就没有结论。

### 22. 主会话不读图片

图片会以 base64 永久留在对话历史里，几轮就撑爆上下文。要看图就派**子 agent**读原图，只让它回文字结论（位置、颜色、文本内容）。

---

## 协作与提交

### 23. 不碰别人未提交的 WIP

`git status -s` 先看清。别人留在工作区的改动（例如 `BottomBarBackdropRenderer.android.kt`）不提交、不格式化、不回退。

**逐文件 `git add`**，不要 `git add -A` / `git add .`。

### 24. 提交规范

- Conventional Commits：`<type>(<scope>): <subject>`，subject 中文祈使句，不加句号，不写实现黑话。
- type：`feat` / `fix` / `perf` / `refactor` / `docs` / `chore`。
- BiliCO scope：`player` `comment` `dynamic` `history` `live` `login` `feed` `data` `ui` `build`。
- coui scope：`coui-ui`。
- **禁止任何署名行**：不写 `Co-Authored-By`、`Generated with`、`🤖`。

### 25. 回退试验后 `compileReleaseKotlin FROM-CACHE` 是正常且有力的证据

删掉试验改动后重新构建，Kotlin 编译命中缓存不是「没生效」，恰恰说明输入与试验前完全一致、产物等价。设备侧再复核一次可感知数值（例如控件高度 168px = 48dp @ 560dpi，而不是跟随系统的 186px）。

### 26. 收尾检查新文件

跑完测试/构建后 `git status`，把本次产生的日志、临时产物删掉；构建输出目录不入库。判断不了是不是自己造的，就留着并在汇报里说明。

---

## 环境

| 项 | 值 |
| :--- | :--- |
| `JAVA_HOME` | `/opt/homebrew/opt/openjdk@21` |
| BiliCO 单文件上限 | 600 行 |
| 单个 ViewModel 上限 | 400 行 |
| coui 本地仓库 | `coui/build/repository/local` → `../coui-local-repo` |
| 组件库坐标 | `io.github.suqi8.coui.kmp:*-android:1.2.0-SNAPSHOT` |
| 只读参考 | `../BiliPai-coui`（不许改，不许依赖） |
