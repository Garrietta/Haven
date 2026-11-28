# CLAUDE.md

Guidance for working in this repository. It holds the facts and invariants every session needs; the design system's full reasoning is in `DESIGN.md`, and the history of how the app got here is in `CHANGELOG.md`.

## What this is

**Haven** (`com.garrietta.haven`): a native Android client for Home Assistant. Kotlin, Compose, Material 3 Expressive (material3 1.5). It talks to the user's own instance over WebSocket and REST and draws everything itself. It is a personal app, used daily on a foldable, by one household.

MVVM without a DI framework: one `HavenViewModel` owns the `HaClient` and the stores, and screens are Compose functions that take the ViewModel.

| Question | Code |
|---|---|
| Talking to Home Assistant | `data/HaClient.kt` |
| App state, every action | `HavenViewModel.kt` |
| Root, navigation, bar and rail, popups | `HavenApp.kt` |
| Colors, type, shape, motion | `ui/theme/` (`DESIGN.md`) |
| Color styles (Haven, Home Assistant, Wallpaper, Tide, Orchid, Graphite) | `ui/theme/ColorStyles.kt`, ramps in `tools/palette.py`, chosen in `AppearancePage` |
| Which color a state gets | `data/SemanticColors.kt`, `ui/components/DesignTokens.kt` |
| Home: the status deck, places and tiles | `data/HomeLayout.kt`, drawn by `ui/home/HavenHome.kt` |
| How every menu works (four places, three windows, one popup per device) | `DESIGN.md`, How every menu works |
| A device's popup | `ui/screens/DetailPages.kt` (`DetailPageScreen`, `EntityPopupMenuRows`); a thermostat's in `ThermostatPage.kt` (`ThermostatControl`, also drawn by the Climate tab) |
| The names devices go by | `deviceDisplayName` in `data/HomeLayout.kt`, `rememberEntityNamer` in `CardSettings.kt` |
| The Climate tab (thermostats, every temperature and humidity reading) | `data/Climate.kt`, drawn by `ui/screens/ClimateScreen.kt` |
| Shared UI pieces | `ui/components/` |
| A tile's options (name, Show on Home, color, icon), the icon catalog | `ui/screens/CardSettings.kt`, `ui/components/CardIcons.kt` |
| Picking one of several | `ChoiceRow` in `ui/components/Choices.kt` |
| A device's control | `ui/controls/` |
| Screens | `ui/screens/` |
| Stored settings | `data/SettingsStore.kt`, `TabsStore`, `CardSizeStore`, `TokenStore` |
| Settings pages, and Home Assistant's own settings | `ui/screens/Settings*.kt` (routes in `SettingsHome.kt`), calls in `data/HaAdminApi.kt`, parsers in `data/HaAdmin.kt` |
| Device Controls, notifications | `HavenControlsService.kt`, `data/HavenNotificationManager.kt` |
| Home screen widgets | `widget/HavenWidgets.kt`, Home Assistant from outside the app in `widget/WidgetHa.kt`, rules in `data/WidgetModels.kt` |
| Updating the app itself | `update/` (check in `AppUpdate.kt`, install in `ApkInstaller.kt`); CI's `publish-test-build` job replaces the `test-build` pre-release after every signed build, compared by `BuildConfig.BUILD_NUMBER` |

## Build, test, look

```sh
./gradlew :app:compileDebugKotlin       # fastest check
./gradlew :app:testDebugUnitTest        # unit tests
./gradlew :app:recordRoborazziDebug     # screenshots -> app/build/outputs/roborazzi
./gradlew :app:assembleDebug            # debug APK (installs beside release, .debug suffix)
```

- Needs JDK 17+, Android SDK platform `android-37.0` and build-tools 36. In a cloud session the SDK is not preinstalled and both it and Google's Maven live on `dl.google.com`, which the environment must allow.
- **Look at what you changed.** `HavenScreenshotTest` renders the real app on the JVM with a demo home (`DemoHome`), folded and unfolded, light and dark. For any visual change, record before and after and read the PNGs. Add a test there when you touch a screen it does not cover.
- **And on the real home.** `RealHomeScreenshotTest` draws whole pages from a snapshot of the household's Home Assistant in `app/build/real-home/` (`states.json` from `/api/states`; `registry.json` from a `/api/template` call over `device_id` / `device_attr`, with `auxiliary` marked by name, since templates cannot see entity categories). It skips without one. The snapshot is personal: it lives in the build directory and is never committed.
- The JVM cannot judge motion, haptics, camera streams or real insets. Say so in the handoff; those are checked on the phone.
- Pure logic lives in `data/` and in small pure functions beside the UI, each with a unit test. Keep it that way: a rule that can be a pure function should be one.

## Design invariants

Breaking one is a bug even when it compiles. Reasons are in `DESIGN.md`.

1. **No color literals outside `ui/theme/Palette.kt`**, and that file is generated (`tools/palette.py`). Surfaces and text come from `MaterialTheme.colorScheme`; state comes from a `StateFamily`.
2. **A state's color is its family's**, through `domainFamily` / `semanticFamily`. Do not give one device type its own color path.
3. **Selected is the style's capsule** (`HavenChrome.capsule`; lime in Haven's style), and **picking one of several is `ChoiceRow`**, wherever it is.
4. **Text takes a role from `MaterialTheme.typography`.** Inline `fontSize` is legacy; move it when you touch it. Keep line heights in em in `Type.kt`.
5. **Corners come from `Corner` / `MaterialTheme.shapes`; cards default to `leafCard`.** A container is never a polygon and a badge is never a rectangle.
6. **Motion uses the named springs in `Motion.kt`.** Never an inline `spring(...)`. Their numbers were tuned on the phone: do not change one without a device pass.
7. **Headings and menus use the shared components**: `ScreenTitle`, `SectionTitle`, `MenuSection` / `MenuGroup` / `MenuRow`.
8. **Design is not a setting.** Shapes and corners are fixed by kind; color is the one choice, a whole-app style. Do not add per-tile or per-type look options.
9. **One popup per device.** Everything that shows a device opens `Screen.EntityDetail`; the popup ends with `EntityPopupMenuRows`. A kind of device has one control design, used wherever that kind is controlled (the thermostat's is `ThermostatControl`).
10. **The bar is fixed**: Home, Security, Climate, Settings (`barItems`). **Only the bar switches tab**: every other press opens a window over where you are (a popup, a panel or a sheet). Home's Climate and Security cards open their pages as popups (`Screen.ClimatePopup`, `Screen.SecurityPopup`), drawn from the same body as the tabs.
11. **Gestures, not buttons.** Reorder by holding the item itself and dragging (`holdToReorder`, `tileGestures`), remove from a list by swiping it away, set a level by sliding across the tile. Never arrows to step an item, never a mode to enter first; every gesture also has a TalkBack action.
12. **The logo is generated** (`tools/logo.py`). Change the geometry there, never the XML or `BrandGeometry.kt`. Haven's own icon glyphs are generated the same way (`tools/glyphs.py` writes `HavenGlyphs.kt`).

## Frozen: stored in users' settings

Renaming any of these silently resets or breaks a saved choice.

- Tile preset keys (`lime`, `heating`, `cooling`, `lock`, `alert`, `media`, `amber`, `sage`).
- Icon keys in `CARD_ICON_OPTIONS` (add new ones freely).
- `corners_0` .. `corners_7`, and the keys in `SHAPE_OPTIONS` (no longer read).
- Color style keys (`ColorStyle.key`) and the `color_style` preference.
- Home section keys, including `round3-security`, `round3-vacuum`, `round3-media`, `round3-greenhouse`.
- SharedPreferences file and key names in the stores.

## Things that bite

| Trap | What happens |
|---|---|
| Polygon morph progress outside 0..1 | Throws. A bouncy spring overshoots; clamp with `clampMorphProgress` first. |
| The wide layout judged on width alone | Split-screen flips to the tablet layout. Use `isWideLayout` (600dp and at least as wide as tall). |
| A wide control as a `MenuRow` trailing | Fine: the row drops it under the text. Do not work around it with a fixed width. |
| A `FilterChip` for a choice | Not the app's choice style. Use `ChoiceRow`. |
| A top-level `val` holding a theme or state color | Keeps the first color style it saw. Make it a getter (`val X: Color get() = ...`). |
| A chart or graph at a fixed height | Wrong at every other width. Take the height from the width (`historyChartHeight`), and let `Sparkline` size its line. |
| `targetSdk` bumped along with `compileSdk` | Changes runtime behaviour (predictive back, large-screen rules) that has not had a device pass. `compileSdk` moves freely; `targetSdk` is held at 35. |
| Media3 bumped casually | The camera transports (HLS, RTSP) were tuned against 1.6.1. Bump it with a camera test pass. |
| A screenshot test waiting for idle | Never returns: something is always animating. Drive `mainClock` by hand, as `shoot()` does. |
| A live camera body that does not register with `registerLiveView` | Keeps streaming while the app is in the background: a stopped app does not recompose, so its `DisposableEffect` never ends the stream. |
| A camera asked for pictures or streams without checking `isBatteryCamera` | Drains a battery camera, which wakes for each one. |

## Known debt

- `HavenApp.kt` is very large. Split by feature when a change gives a reason to; do not reorganise it in passing.
- Many comments narrate the feedback round that produced the code; rewrite them to say what the code does when you touch a file.
- About 280 inline `fontSize` call sites.
- The Home panels (vacuum, media, security, greenhouse) use fixed entity ids in `data/HomePanels.kt`.
- Test files named `RoundNNLogicTest` are regression suites from individual feedback rounds and mix features; new tests go in a file named for the thing tested.
- Release builds are not minified.

## Writing here

Comments say what the code does and why it is that way, for someone reading it cold. They do not narrate the history of how it got there; that belongs in the commit message and `CHANGELOG.md`.
