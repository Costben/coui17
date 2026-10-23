# Implementation Specification

This specification defines the component selection, density scopes, glass backdrop wiring, popup hosts, surface color hierarchy, interaction details, and verification standards for building ColorOS 17 interfaces with COUI. All specifications are calibrated against native ColorOS 17 system apps on the reference device (OnePlus PLK110, ColorOS V17.0.0, 1272×2772).

---

## 1. System Screen Counterparts & Component Selection

Follow this workflow when implementing any screen:

1. **Identify the system counterpart**: Determine which ColorOS 17 system app screen the target screen corresponds to (main tab screens map to *Gallery · Photos*, secondary configuration screens map to *Settings*, and confirmation prompts map to the system confirmation dialog).
2. **Map system controls to COUI components**: Inspect the counterpart node tree via `uiautomator dump`, match each `resource-id` and control type to the table below, and assemble the screen from standard library components.
3. **Assign density scopes**: Apply the app density, Gallery density, or panel physical density to each region according to Section 2.
4. **Wire glass materials**: For any region that uses real-time blur on the system counterpart, connect recording, providing, and sampling together as specified in Section 3.
5. **Verify via side-by-side profiles**: Measure geometric and optical profiles on the reference device using identical scripts against the native system app (Section 8).

### System Controls to Library Components Mapping

| System Screen Element | System Node ID / Type | Standard COUI Component Combination | Target Density |
| :--- | :--- | :--- | :--- |
| Main tab title bar and action keys | `toolbar`, `action_main_tab_search` | `PinnedTopAppBar` or `TopAppBar(gradientGlass = true)` + `HighlightedIconButton` + `Modifier.couiTopAppBarActionItem()` | Gallery density |
| Secondary settings back and overflow keys | `coui_toolbar_back_view`, `coui_toolbar_more_view` | `TopAppBar` / `SmallTopAppBar` + `HighlightedIconButton` + `Modifier.couiTopAppBarActionItem()` | App density |
| Toolbar overflow popup menu | `coui_popup_wrapper`, `coui_popup_list_view` | `OverlayListPopup` + `ListPopupColumn` + `DropdownImpl(role = Role.Button)` | Panel physical density (switched automatically) |
| Floating bottom navigation capsule | `TYPE_FRAMEWORK_BOTTOM_BAR` | `BottomFloatingNavigationBar` | Gallery density |
| Floating bottom bar with side accessory balls | `TYPE_FRAMEWORK_BOTTOM_BAR` (accessory form) | `BottomFloatingAccessoryNavigationBar` | Gallery density |
| Confirmation & action dialog | System confirmation dialog | `WindowAlertDialog` | App density |
| Custom-content modal dialog | System modal panel | `WindowDialog` (or `OverlayDialog` / `OverlayBottomSheet` when glass material is required) | App density |
| Settings group cards and preference rows | System settings rows | `Card` + `coui-preference` components | App density |
| Capsule segmented control | System segmented bar | `TabRowWithContour` | App density |
| Search input capsule | System search bar | `InputField` | App density |

---

## 2. Density System Specification

A single ColorOS 17 screen operates across three distinct density baselines. Each UI region must bind to its designated density scope:

| Density Tier | Reference Value on PLK110 | Applicable Scope | Integration Method |
| :--- | :--- | :--- | :--- |
| **App Density** | `620dpi = 3.875 px/dp` (scales with system *Display size*) | Default app window layout, secondary settings pages, content lists, standard dialogs | Default Compose `LocalDensity`; no extra wrapper needed |
| **Gallery Density** | `565dpi = 3.53125 px/dp` (short edge fixed at `360dp`) | Main tab toolbar action keys, floating bottom navigation bar, and side accessory balls | Provide `LocalDensity` explicitly via [`rememberGalleryDensity()`](../components/pinnedtopappbar#gallery-density) |
| **Panel Physical Density** | `560dpi = 3.5 px/dp` (panel hardware `ro.sf.lcd_density`) | System popup menus (`OverlayListPopup`, `WindowListPopup`, cascading menus) | Switched automatically inside the popup via `popupPhysicalDensity`; no caller wrapper needed |

### Density Scoping Rules

1. **Fixed-size chrome on main tab screens uses Gallery density**: A `40dp` toolbar action key renders at `141px` under Gallery density (matching native Gallery) versus `155px` under app density; a `56dp` floating bottom capsule renders roughly 10% larger if left at app density.
2. **Scope Gallery density strictly to the fixed-size subtree**: Outer layout and positioning modifiers (`align`, `statusBarsPadding`, `navigationBarsPadding`, outer margins) must resolve at the outer app density so controls do not shift position.
3. **Two standard Gallery-density strategies for main tab toolbars**:
   - **Wrap the entire `TopAppBar`**: Action key dimensions, anchor positions, and large title typography all match native Gallery 1:1 (the large title renders ~9% smaller than under Display-size scaling). Use this for strict 1:1 system replicas.
   - **Wrap only `HighlightedIconButton`**: The action key remains `141px`, while the toolbar row height and title typography continue to scale with the user's *Display size* setting.
4. **Popup menus are invariant to outer Gallery density**: Placing `OverlayListPopup` inside a Gallery-density scope is safe because `popupPhysicalDensity` always converts from the base density to the physical panel density.
5. **Pixel-to-dp conversion baselines**: Divide pixel measurements by `3.53125` for Gallery-density chrome, by `3.5` for popup menus, and by `3.875` for standard app-density layouts on the reference device.

---

## 3. Glass Material & Backdrop Wiring Specification

Real-time blur and glass edge highlights require three connected steps:

1. **Create the backdrop layer**: Call `rememberLayerBackdrop` and **paint the solid page background (`drawRect(pageColor)`) before `drawContent()`** inside its drawing lambda. This guarantees that empty lists and dark-mode pages sample the true page background rather than a transparent buffer.
2. **Attach the recording modifier**: Apply `Modifier.layerBackdrop(backdrop)` to the content container being sampled.
3. **Provide the backdrop**: Supply `CompositionLocalProvider(LocalBackdrop provides backdrop)` to the toolbar, bottom bar, and popup menus.

### Backdrop Topology & Isolation Rules

- **Single-node recording rule**: Each `LayerBackdrop` instance must be attached to **at most one** `Modifier.layerBackdrop` node. Attach the bottom bar's `barBackdrop` once on the outer `HorizontalPager`, never separately inside each page.
- **Strict separation between recording and sampling**: A component that samples a backdrop (`TopAppBar`, `BottomFloatingNavigationBar`) must sit **outside** the `Modifier.layerBackdrop` subtree it samples (as a sibling layer). Placing a sampling bar inside its own recorded layer causes it to sample its previous frame and flicker during scroll.
- **Dedicated backdrops for the bottom bar and per-page toolbars**:
  - `barBackdrop` records the entire `HorizontalPager` and feeds the outer `Scaffold`'s bottom navigation bar;
  - Each tab page creates its own `topBarBackdrop`, attaches it to that page's content container, and provides it exclusively to that page's `TopAppBar`.
- **Popup menu backdrop inheritance**: `OverlayListPopup` re-provides the call site's `LocalBackdrop` inside the root `Scaffold`'s popup host. When an overflow menu is declared inside `TopAppBar(actions = ...)`, it automatically samples `topBarBackdrop` and renders live glass over the page content.
- **Blur radius conversion**: Native ColorOS blur radii are specified as pixel Gaussian `sigma`. Convert pixel `sigma` to Compose `Dp` via `sigmaPx / 0.45 / density` rather than passing the pixel value directly as `dp`.

---

## 4. Popup & Dialog Host Specification

### 1. `Overlay*` Components (Scaffold-Hosted Overlays with Live Glass)

- **Host requirement**: `OverlayListPopup`, `OverlayDialog`, and `OverlayBottomSheet` compose inside the root `Scaffold`'s `COUIPopupHost`. Every screen that uses `Overlay*` must reside inside a COUI `Scaffold`.
- **Transparent host wrapper for standalone routes**: Full-screen routes that do not use a standard bar shell (such as image viewers, WebView screens, or custom canvas pages) must be wrapped in a transparent `Scaffold` at the navigation entry so `Overlay*` popups have a valid host:

```kotlin
entry<Route.ImageViewer> { route ->
    Scaffold(containerColor = Color.Transparent) {
        ImageViewer(route)
    }
}
```

- **Preserve the default `popupHost`**: Never pass an empty `popupHost = { }` to `Scaffold`, as doing so disables all `Overlay*` rendering across its entire subtree.

### 2. `Window*` Components (Platform Dialog Windows)

- **Standard confirmation dialogs**: Use `WindowAlertDialog(title, summary, confirmText, cancelText)`, which provides the calibrated panel corner radius, adaptive horizontal/vertical button bar, window scrim, and enter/exit transitions.
- **Custom modal panels**: Use `WindowDialog`, which binds the panel surface to the theme background. Do not wrap a `Card` inside a bare Compose `Dialog` (in dark mode `surfaceContainer` is 10% white and becomes nearly transparent without a dialog panel background or scrim).
- **Material boundary**: `Window*` components are hosted in separate platform `Dialog` windows and cannot sample the host window's `LocalBackdrop`; always choose `Overlay*` when glass material is required.

---

## 5. Page Color & Surface Hierarchy Specification

ColorOS 17 screens use a strict **two-level surface hierarchy**: the page background (`background`) and the card container (`surfaceContainer`).

| Surface Level | Color Token | Light Mode Value | Dark Mode Value | Target Components & Regions |
| :--- | :--- | :--- | :--- | :--- |
| **Level 1: Page Background** | `COUITheme.colorScheme.background` | Page light grey | Dark grey (`~#1E1E1E`) | `Scaffold(containerColor)`, `TopAppBar(color)`, `SmallTopAppBar(color)`, `layerBackdrop` base rect |
| **Level 2: Card Container** | `COUITheme.colorScheme.surfaceContainer` | Pure white `#FFFFFF` | 10% white over page background | `Card`, grouped preference containers |
| **Nested Block Inside a Card** | `background` in light mode, `surfaceContainer` in dark mode | Page light grey (contrasts against white card) | Additional 10% white overlay (lifts above dark card) | Quoted replies, reposted blocks, embedded callouts inside a card |

### Color Usage Rules

1. **Explicitly pass `background` to `Scaffold` and `TopAppBar`**: Both `Scaffold(containerColor)` and `TopAppBar(color)` / `SmallTopAppBar(color)` default to `surface` (which is pure black `#111111` in dark mode). Always pass `COUITheme.colorScheme.background` explicitly for standard pages.
2. **Do not use `surface` for embedded capsules or tracks**: Because `surface` matches the page background in standard themes, controls or tracks painted with `surface` directly on the page background become invisible.
3. **Match native Android `windowBackground`**: Set `android:windowBackground` in both `themes.xml` and `values-night/themes.xml` to match `COUITheme.colorScheme.background` so window and activity transitions never flash white or black.

---

## 6. Standard Screen Implementations

### 6.1 Main Tab Shell & Floating Bottom Navigation Bar (Counterpart: *Gallery · Photos*)

A single outer `Scaffold` hosts a `HorizontalPager`. The floating bottom bar sits above the pager and samples `barBackdrop`; each tab page manages its own toolbar and per-page backdrop.

```kotlin
@Composable
fun MainShell(pages: List<Tab>) {
    val pageColor = COUITheme.colorScheme.background
    val barBackdrop = rememberLayerBackdrop {
        drawRect(pageColor)
        drawContent()
    }
    val pagerState = rememberPagerState { pages.size }

    Scaffold(
        containerColor = pageColor,
        bottomBar = {
            CompositionLocalProvider(
                LocalBackdrop provides barBackdrop,
                LocalDensity provides rememberGalleryDensity(),
            ) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding()) {
                    BottomFloatingNavigationBar {
                        pages.forEachIndexed { index, tab ->
                            BottomFloatingNavigationBarItem(
                                selected = pagerState.currentPage == index,
                                onClick = { /* switch page */ },
                                icon = tab.icon,
                                selectedIcon = tab.selectedIcon,
                                label = tab.label,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().layerBackdrop(barBackdrop),
        ) { page ->
            TabPage(pages[page], outerPadding = padding)
        }
    }
}
```

**Implementation Requirements**:
- **Content scrolling beneath the floating capsule**: Lists inside each tab page apply `outerPadding.calculateBottomPadding()` plus visual breathing room to their own `contentPadding`, allowing list items to scroll behind the glass capsule while the final item can still scroll clear above it.
- **Consume `Scaffold` padding during measurement**: On pages with collapsible toolbars or high-frequency scrolling, read `PaddingValues` lazily during measurement/layout rather than unpacking dynamic heights during composition, avoiding full-page recomposition on every scroll frame.

### 6.2 Main Tab Toolbar, Collapsing Scroll & Overflow Menu

```kotlin
@Composable
fun TabPage(tab: Tab, outerPadding: PaddingValues) {
    val scrollBehavior = COUIScrollBehavior()
    val pageColor = COUITheme.colorScheme.background
    val topBarBackdrop = rememberLayerBackdrop {
        drawRect(pageColor)
        drawContent()
    }
    Box(
        Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        TabContent(
            tab = tab,
            outerPadding = outerPadding,
            modifier = Modifier.fillMaxSize().layerBackdrop(topBarBackdrop),
        )
        CompositionLocalProvider(
            LocalBackdrop provides topBarBackdrop,
            LocalDensity provides rememberGalleryDensity(),
        ) {
            TopAppBar(
                title = tab.title,
                color = pageColor,
                scrollBehavior = scrollBehavior,
                gradientGlass = true,
                actions = { OverflowMenu(onOpenSettings = { /* open settings */ }) },
            )
        }
    }
}

@Composable
fun OverflowMenu(onOpenSettings: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        HighlightedIconButton(
            onClick = { expanded = true },
            modifier = Modifier.couiTopAppBarActionItem(),
        ) {
            Icon(COUIIcons.More, contentDescription = "More")
        }
        OverlayListPopup(
            show = expanded,
            alignment = PopupPositionProvider.Align.End,
            onDismissRequest = { expanded = false },
        ) {
            ListPopupColumn {
                DropdownImpl(
                    item = DropdownItem(text = "Settings"),
                    optionSize = 1,
                    isSelected = false,
                    index = 0,
                    role = Role.Button,
                    onSelectedIndexChange = {
                        expanded = false
                        onOpenSettings()
                    },
                )
            }
        }
    }
}
```

**Implementation Requirements**:
- **Attach `nestedScroll`**: Always attach `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` to the container wrapping both the scrollable content and the `TopAppBar` so scroll deltas drive the large-title collapse state machine.
- **Apply `Modifier.couiTopAppBarActionItem()`**: Attach this modifier to toolbar action keys so key dimensions, internal margins, and popup right-edge anchoring align with native Gallery (`1214px` right edge on PLK110).
- **Retain default `OverlayListPopup` geometry**: Do not override `minWidth`, do not add custom `heightIn` constraints to menu rows, and do not supply a custom `PopupPositionProvider`. Panel physical density, single-row height (`191px` vs. native `190px` on PLK110), and corner profiles are pre-calibrated (at a single-row height of `57dp`, the `28dp` corner radius naturally forms full semicircular ends).
- **Accessibility role**: Pass `role = Role.Button` for navigation or action menu items (the default `Role.RadioButton` is reserved for single-selection lists).

### 6.3 Floating Bottom Bar with Side Accessories (Counterpart: *Gallery · Create*)

```kotlin
BottomFloatingAccessoryNavigationBar(
    startAccessoryVisible = showFilter,
    startAccessory = { FilterBall() },
    endAccessoryVisible = showCompose,
    endAccessory = {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Button,
                    onClick = onCompose,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = COUIIcons.Add,
                contentDescription = "Compose",
                tint = COUITheme.colorScheme.onSurface,
            )
        }
    },
) {
    /* BottomFloatingNavigationBarItem entries */
}
```

**Implementation Requirements**:
- **Distinguish slot visibility from slot removal**:
  - Setting `startAccessoryVisible = false` or `endAccessoryVisible = false` animates the accessory ball melting into or out of the main capsule while **preserving the reserved slot width**, keeping the capsule width and centering constant across tabs;
  - Passing `null` to `startAccessory` or `endAccessory` releases the slot width so the main capsule expands to full width.
- **Disable duplicate indication inside accessories**: `BottomFloatingAccessoryNavigationBar` already provides circular press scaling and highlight feedback for the accessory ball; pass `indication = null` on inner `clickable` modifiers to avoid drawing a rectangular state overlay.
- **Animate the inner icon when switching tabs**: When adjacent tabs share an accessory ball with different actions, keep the accessory slot mounted and animate the icon swap inside the ball (shrink out, then grow in).

### 6.4 Secondary Settings Screen & Grouped Preferences (Counterpart: *Settings*)

```kotlin
@Composable
fun SettingsPage(onBack: () -> Unit, onOverflow: () -> Unit) {
    val scrollBehavior = COUIScrollBehavior()
    val pageColor = COUITheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        drawRect(pageColor)
        drawContent()
    }
    Scaffold(
        containerColor = pageColor,
        topBar = {
            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                TopAppBar(
                    title = "Settings",
                    color = pageColor,
                    scrollBehavior = scrollBehavior,
                    gradientGlass = true,
                    navigationIcon = {
                        HighlightedIconButton(
                            onClick = onBack,
                            modifier = Modifier.couiTopAppBarActionItem(),
                        ) {
                            Icon(COUIIcons.Back, contentDescription = "Back")
                        }
                    },
                    actions = { OverflowMenu(onOpenSettings = onOverflow) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .layerBackdrop(backdrop),
            contentPadding = padding,
        ) {
            item {
                Card {
                    /* coui-preference items */
                }
            }
        }
    }
}
```

**Implementation Requirements**:
- **Use default app density**: Native ColorOS *Settings* renders toolbar keys at the system display density (`40dp = 155px` on PLK110); do **not** wrap secondary settings toolbars in `rememberGalleryDensity()`.
- **Read card corner radius from theme dimens**: Always reference `COUITheme.dimens.card.cornerRadius` (`17dp` on ColorOS 17) rather than hard-coding corner radii.

### 6.5 Segmented Controls, Search Fields & Pull-to-Refresh

- **Segmented controls**: Use `TabRowWithContour`, passing `indicatorMaterial = SearchBarDefaults.searchBarStroke()` when an edge highlight is required.
- **Search fields**: Use `InputField`, which provides the `40dp` capsule, edge highlight, editing caustic glow, and clear button; trigger submission via the keyboard IME search action rather than adding a separate text button beside the bar.
- **Pull-to-refresh**: Use `PullToRefresh(topAppBarScrollBehavior = scrollBehavior, contentPadding = padding)` so the refresh indicator positions itself below the toolbar while content continues to scroll behind the glass bar.

---

## 7. Shapes, Press Feedback, Navigation & Icons

### 1. Continuous-Curvature Squircles vs. Circular Capsules

- `SquircleShape` uses a superellipse curve. When the corner radius equals half the component height, the superellipse bulges outward at 45° and leaves short near-vertical segments at both ends rather than forming a true semicircular stadium.
- When a counterpart control on the reference device is verified to be a **true circular capsule (stadium/pill)**, provide `CompositionLocalProvider(LocalSquircleEnabled provides false)` locally so `SquircleShape` resolves to `Outline.Rounded(min(w, h) / 2)`.

### 2. Press Feedback (`LocalIndication`)

- `COUITheme` sets `LocalIndication` to `COUIIndication`, which paints a state layer across the component's full rectangular bounds.
- When building custom capsule buttons, circular keys, or segmented controls:
  - Either disable the rectangular state layer (`indication = null`) and drive press feedback via scale animation, or clip the composable with `Modifier.clip(shape)` before applying `clickable`.
  - For quick taps, latch the highlight state on press and hold it for a brief minimum duration so fast releases remain visible, and clear the overlay before capturing route-transition snapshots.

### 3. Predictive Back & Navigation 3

- **Delegate back handling exclusively to `NavDisplay`**: Do not register an inner `PredictiveBackHandler` inside individual page composables hosted by `NavDisplay` (inner handlers intercept back events first and prevent `NavDisplay` from rendering the underlying page preview).
- **Configure `enableOnBackInvokedCallback` in the manifest**: Declare `android:enableOnBackInvokedCallback="true"` in `AndroidManifest.xml` and never mutate `ApplicationInfo` via reflection at runtime.
- **Store scroll and collapse state in back-stack entries**: Persist list scroll positions, toolbar collapse states, and pager indices on the route's back-stack entry so the underlying page rebuilt during a predictive-back gesture matches the settled state after release.

### 4. Icon Weights

- All five weights of `COUIIcons.*` (`Light`, `Normal`, `Regular`, `Medium`, `Demibold`) are **solid filled vector paths**. Differentiate selected and unselected states through weight and tint rather than mixing outline and filled icon sets.
- Maintain visual stroke balance when placing icons side by side (for example, `COUIIcons.Tune` uses a thinner stroke relative to solid glyphs).

---

## 8. Build, Publishing & Reference Device Verification

### 1. Local Publishing & Toolchain

- **Publish all modules in a single invocation**: When publishing to the local repository, run `./gradlew publishAllToLocalRepository` so all artifacts are staged together (running per-module publication tasks recreates `build/repository/local` and removes previously staged modules).
- **Clean working tree before staging**: Ensure the Git working tree has no uncommitted changes before publishing local snapshots so artifacts correspond strictly to committed revisions.
- **JDK 21 bytecode target**: COUI compiles Android and Desktop classes for Java 21 (Class File 65); configure downstream unit-test JVMs to run on JDK 21.

### 2. Side-by-Side Profile Measurement & Performance Baselines

- **Single reference device**: Calibrate all ColorOS 17 geometry and optical metrics against **OnePlus PLK110 (ColorOS V17.0.0, 1272×2772)**. Do not mix measurements from ColorOS 16 devices.
- **Profile-based verification**:
  - **Corner inset profile**: Measure horizontal edge insets at `0 / 4 / 8 / 13 / 20 / 30 / 40 / 60 / 80 px` from the top edge (`OverlayListPopup` measures `63/50/40/32/24/16/10/3/1 px` on PLK110 against native Gallery's `63/50/41/33/25/16/10/3/1 px`);
  - **Row height and pitch**: Compute vertical distances from the center `bounds` of adjacent text nodes in `uiautomator dump`;
  - **Button and glass luminance profiles**: Sample horizontal and vertical luminance profiles through the control center, reading glass edges along sides backed by uniform background color;
  - **Validate `uiautomator dump` output**: Verify that stdout contains `UI hierchary dumped to` before reading the XML file, and distinguish outer clickable container nodes from inner icon nodes that carry `content-desc`.
- **Measure scrolling performance on Release builds**: Always evaluate 120Hz scrolling frame pacing and shader performance on **Release builds** with R8 optimization enabled.
