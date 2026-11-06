# BottomFloatingAccessoryNavigationBar

`BottomFloatingAccessoryNavigationBar` 是 ColorOS 17 的底部悬浮导航栏，两侧各带一个配件视图（`COUIBottomFloatingAccessoryNavigationBar`）。它相当于 `BottomFloatingNavigationBar` 再加上位于导航栏起始边与结束边的两个圆形视图。

两个配件视图都是 56dp 的圆形，与胶囊之间留 12dp 间距。已配置的配件始终会占位，但只有在对应的 `*Visible` 标志置位时才会展开显示：它以 0.5 的缩放起步淡入放大，并从胶囊中滑出，全部由原生弹簧驱动。任一配件展开期间，起始配件、胶囊、结束配件三个轮廓会融合成一团，配件因此从胶囊中「融出」：ColorOS 上由系统材质负责融合（`setMetaBallChain`），其它平台由运行时 `MetaBallSurface` 着色器绘制。两个插槽都未配置时，该组件就是普通的玻璃胶囊，表现与 `BottomFloatingNavigationBar` 完全一致。

胶囊的填充、模糊、阴影与触摸高亮共用一个 `CapsuleOutlineShape`，走系统胶囊曲线（`addSegmentCapsule`），而不是正圆跑道形；两侧配件视图仍为正圆。`MetaBallSurface` 的胶囊粒子仍是圆角矩形 SDF，因此融合状态下胶囊端头是该着色器的圆弧近似。

导航栏通过 `LocalBackdrop` 采样其后方的画面；没有可用的背景层时保持不透明填充。

融合轮廓在 ColorOS 上由系统材质绘制，其它平台由运行时着色器绘制，具体走哪条见下文「系统材质」。着色器是回退路径：静止态与它替代的渲染器相差不到 1 级亮度。连续拖动会保留弹簧速度，松手回弹按控件面积和释放速度调整。侧球收回时，在其形变后面积与胶囊重叠超过 50% 后才逐渐淡出；拉伸直接变换原始圆形，保持椭圆轮廓。胶囊统一负责导航项的按压形变，导航项不会再叠加第二次缩放。

按压光效归属于手势开始的区域：左侧圆球、中间胶囊或右侧圆球。光源通过弹簧跟随手指，坐标允许超出区域，但光效始终裁切在原区域内；移出较近时保留部分光晕，移远后完全消失，拖回后重新出现，不会转移到经过的其他区域。中间各导航项共用胶囊的光效。

下光晕改用系统焦散阴影绘制：底栏主体采用 level-4（底边偏移 4.899994dp、距离场宽度 10.399994dp），附属球采用 level-5（11.199982dp、20dp）。两者使用同一纵向幂次衰减公式，颜色取 `coui_caustic_color_2`；不再按 75° 角度截取。ColorOS 直接调用系统接口，其他平台运行还原公式。

上、下轮廓高光使用 `TYPE_FRAMEWORK_CAPSULE_3` 的 `EDGE_STYLE_0` 渐变：线宽 2.5dp，深色强度 0.8、浅色 0.25。上缘采用较窄的 far 线，下缘采用较宽的 near 线。系统对上下缘使用同一个 `edgeAlpha`，实测下缘亮度是上缘的 0.89 倍（深色）/ 1.24 倍（浅色）；`EDGE_STYLE_0` 两条线的权重为 0.7 / 1.0，差额由 `BottomFloatingNavigationBarDefaults.rimBottomScale(isDark)` 补齐，修正后 Compose 下缘与系统相差在一级以内。光晕与轮廓高光独立绘制，关闭背景模糊仍保留两条高光。

`BottomFloatingNavigationBar` 与本组件的纯胶囊形态（无展开视图、无侧球）走同一套轮廓渲染，因此同样带有这套描边与焦散光；在此之前它只画得上缘，下缘外侧没有任何光。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.BottomFloatingAccessoryNavigationBar
import io.github.suqi8.coui.kmp.basic.BottomFloatingAccessoryNavigationBarDefaults
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.BottomFloatingRedDot
```

## 基本用法

```kotlin
var selectedIndex by remember { mutableIntStateOf(0) }

BottomFloatingAccessoryNavigationBar(
    startAccessory = {
        // 融合轮廓已经包含了配件所在的圆形，插槽本身不需要再画背景。
        Icon(imageVector = COUIIcons.Search, contentDescription = "搜索")
    },
    endAccessory = {
        Icon(imageVector = COUIIcons.Add, contentDescription = "添加")
    },
) {
    BottomFloatingNavigationBarItem(
        selected = selectedIndex == 0,
        onClick = { selectedIndex = 0 },
        icon = COUIIcons.Home,
        label = "首页",
    )
    BottomFloatingNavigationBarItem(
        selected = selectedIndex == 1,
        onClick = { selectedIndex = 1 },
        icon = COUIIcons.Messages,
        label = "消息",
    )
}
```

## 收起配件视图

`startAccessoryVisible` 与 `endAccessoryVisible` 分别控制两侧配件在展开与收起之间切换。无论是否展开，插槽都保持已配置状态，占位依旧保留，只有视图本身参与动画：

```kotlin
BottomFloatingAccessoryNavigationBar(
    startAccessoryVisible = showSearch,
    startAccessory = {
        Icon(imageVector = COUIIcons.Search, contentDescription = "搜索")
    },
) {
    // ... 导航项 ...
}
```

两侧配件都配置、且都收起，就是 ColorOS 17 相册「创作」页的状态：只剩一个胶囊，因为两侧插槽占位相同，胶囊仍然居中。要做这种形态请两侧都配置；只配置一侧会让胶囊偏离居中。PLK110 相册实测，配件展开与收起时胶囊是同一种半透明材质，各页签下胶囊亮度都随背后内容变化。

## 属性

### BottomFloatingAccessoryNavigationBar 属性

| 属性名                | 类型                                | 说明                                                          | 默认值                    | 是否必须 |
| --------------------- | ----------------------------------- | ------------------------------------------------------------- | ------------------------- | -------- |
| modifier              | Modifier                            | 应用于导航栏的修饰符                                          | Modifier                  | 否       |
| containerColor        | Color                               | 胶囊与融合轮廓的背景颜色                                      | COUITheme.colorScheme.bar | 否       |
| backdrop              | Backdrop?                           | 导航栏后方被模糊的内容；`null` 时保持不透明                   | LocalBackdrop.current     | 否       |
| startAccessoryVisible | Boolean                             | 起始配件视图是否展开                                          | true                      | 否       |
| endAccessoryVisible   | Boolean                             | 结束配件视图是否展开                                          | true                      | 否       |
| startAccessory        | (@Composable BoxScope.() -> Unit)?  | 起始边的视图；`null` 表示未配置，不占位                       | null                      | 否       |
| endAccessory          | (@Composable BoxScope.() -> Unit)?  | 结束边的视图；`null` 表示未配置，不占位                       | null                      | 否       |
| content               | @Composable RowScope.() -> Unit     | 导航栏内容，通常是 `BottomFloatingNavigationBarItem`          | -                         | 是       |

### BottomFloatingAccessoryNavigationBarDefaults 对象

#### 常量

| 常量名                   | 类型              | 说明                                        | 值                          |
| ------------------------ | ----------------- | ------------------------------------------- | --------------------------- |
| AccessorySize            | Dp                | 配件视图的直径                              | 56.dp                       |
| SideSpacing              | Dp                | 胶囊与配件视图之间的间距                    | 12.dp                       |
| PaddingHorizontal        | Dp                | 小屏下保留的外部内边距                      | 16.dp                       |
| MinScale                 | Float             | 配件视图动画的起始缩放                      | 0.5f                        |
| AlphaSpring              | `SpringSpec<Float>` | 配件的透明度弹簧                            | 响应 0.35 / 回弹 0          |
| ScaleSpring              | `SpringSpec<Float>` | 配件的缩放弹簧                              | 响应 0.55 / 回弹 0.35       |
| TranslationXSpring       | `SpringSpec<Float>` | 让配件从胶囊中滑出的弹簧                    | 响应 0.55 / 回弹 0.35       |
| LowEndAlphaSpring        | `SpringSpec<Float>` | 设备报告低动效等级时使用的透明度弹簧        | 响应 0.2 / 回弹 0           |
| LowEndScaleSpring        | `SpringSpec<Float>` | 低动效等级的缩放弹簧                        | 响应 0.5 / 回弹 0.3         |
| LowEndTranslationXSpring | `SpringSpec<Float>` | 低动效等级的位移动画弹簧                    | 响应 0.5 / 回弹 0.3         |

## 响应式边距

与普通悬浮导航栏一致，16dp 的外部内边距只在小屏下保留，中屏与大屏下收缩为 0。胶囊会在两个配件视图让出的空间中居中。

胶囊本身按 `BaseBottomFloatingMenuView.onMeasure` 测量原生菜单视图的方式测量——能放下时等于各项在 `BottomFloatingNavigationBarDefaults.ItemMaxWidth`（70dp）下的总宽加上 12dp 内边距与 8dp 间距，放不下时被压缩进配件视图让出的宽度。纵向上它在导航栏 88dp 的盒子里居中，与 `COUIBottomFloatingAccessoryNavigationBar.onLayout` 让子视图居中一致。盒子下方的留白由宿主决定：用 `BottomFloatingNavigationBarDefaults.navigationBarPadding()`（手势导航 8dp、三键导航 36dp）代替 `navigationBarsPadding()`，手势导航下胶囊底边即距屏幕边缘 24dp，与 ColorOS 17 相册一致。

布局在同一档位上会在小屏与中屏摆放之间选择，但这个选择在这里观察不到。小屏摆放把每个配件视图贴到导航栏各自的外侧边缘，并把菜单居中于两者之间——本实现已经如此，因为原生组件把自身测量为「内容 + 内边距」，而本移植同样包裹内容。

## 系统材质

在 ColorOS 17 上，底栏不再自己绘制玻璃，而是把材质交给系统：`setBackgroundRenderEffect` 负责背景模糊与染色，`setCornerParams` / `setEdgeParams` / `setShadowParams` 绘制 `TYPE_FRAMEWORK_CAPSULE_3` 的连续圆角描边及其衰减带，外轮廓焦散阴影（焦散不可用时退回 `level3b` 投影）负责投影抬升，`setMetaBallChain` 把胶囊与两个侧球融成一团。

`LocalPlatformBottomBarMaterialEnabled`（默认 `true`）控制是否走系统材质，`LocalBlurEnabled`（默认 `true`）是它的前置条件。系统材质自己会模糊背景，因此当模糊被关闭时——无论是用户关闭还是系统省电模式——底栏改回 Compose 自绘，而不是继续走原生模糊。其它平台，以及缺少上述任一接口的 ColorOS 设备，同样走 Compose 自绘：胶囊背后是 `textureBlur`，融合轮廓是 `MetaBallSurface`。

被这条路径取代的 `setBaseParams` 渲染器会在叠加层状态变化时重放系统自己的材质动画——弹出对话框时整块轮廓先缩到 84% 再弹回约 160ms——当时因此一度在 ColorOS 上强制改用运行时着色器。改用背景 RenderEffect 的这版渲染器不会重放：在参考机上把下拉面板打开，逐帧比对显示轮廓保持静止几何（相对静止帧的最佳拟合缩放 1.000，相关系数 ≥ 0.99），开关打开与关闭都一样。

该开关默认开启，因为这就是系统自己画出来的样子，尽管它是更贵的那条路。同一台设备、同一个 release 包、同样的 500px 中段拖拽手势（atrace 的 `DrawFrame` 切片，两轮交错测量）下：Compose 自绘 p50 为 120Hz 5.82ms / 90Hz 8.97ms，只叠加系统背景模糊为 6.04 / 9.39ms，完整系统材质为 6.56 / 10.46ms。差距是 0.7–1.5ms（约一帧的 13–16%），但不损失帧：两条路都能跑满当前刷新率，漏帧比例相差约一个百分点（120Hz 3.1% vs 3.5%，90Hz 8.8% vs 10.0%）。这部分开销分散在系统背景通道与其后的显示列表工作上，不是某一个可摘除的参数，摘掉任何一项都会偏离系统本身的观感。更看重这部分余量时，把开关设为 `false`。

描边与焦散阴影也不是无条件绘制的。它们跟随 `COUIMaterialStateManager` 读取的那几个开关：`system_material_stroke_enable`、低电量与超级省电模式、应用自身的 `coui_blur_enable` / `is_immersive_theme` 布尔值，以及 `persist.sys.oplus.anim_level` 动画级别，全部实时监听。描边开关关闭时底栏应用 `TYPE_NO_EFFECT`——完全不再画描边，而不是画得更淡——投影也从焦散退回 `level3b` 投影。

## 与 BottomFloatingNavigationBar 的关系

两者共用导航项、红点与默认值：`BottomFloatingNavigationBarItem` 与 `BottomFloatingRedDot` 在这里原样使用，胶囊高度、圆角、导航项度量与触摸高亮都来自 `BottomFloatingNavigationBarDefaults`。不需要侧边配件时使用 `BottomFloatingNavigationBar`，需要在胶囊两侧放置一到两个圆形操作入口时使用本组件。

上下高光现在采样着色前的实际模糊背景，按 `libhwui blendEdge` 做亮度修正与 RGB 混合；0.8/0.25 控制材质混合权重，不再表示直接覆盖白色描边。顶部圆形按钮共用此公式并保留自己的系统参数；外侧焦散光参数保持原生值。
