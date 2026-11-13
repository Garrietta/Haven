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
| Which color a state gets | `data/SemanticColors.kt`, `ui/components/DesignTokens.kt` |
| What a room says on Home (color, sentence, switches) | `data/RoomDigest.kt`, drawn by `ui/screens/RoomsHome.kt` |
| The Climate tab (thermostats, every temperature and humidity reading) | `data/Climate.kt`, drawn by `ui/screens/ClimateScreen.kt` |
| Shared UI pieces | `ui/components/` |
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
- The JVM cannot judge motion, haptics, camera streams or real insets. Say so in the handoff; those are checked on the phone.
- Pure logic lives in `data/` and in small pure functions beside the UI, each with a unit test. Keep it that way: a rule that can be a pure function should be one.

## Design invariants

Breaking one is a bug even when it compiles. Reasons are in `DESIGN.md`.

1. **No color literals outside `ui/theme/Palette.kt`**, and that file is generated (`tools/palette.py`). Surfaces and text come from `MaterialTheme.colorScheme`; state comes from a `StateFamily`.
2. **A state's color is its family's**, through `domainFamily` / `semanticFamily`. Do not give one device type its own color path.
3. **Selected is the lime capsule** (`HavenChrome`, `choiceChipColors()`): tabs, rooms, any one-of-several choice.
4. **Text takes a role from `MaterialTheme.typography`.** Inline `fontSize` is legacy; move it when you touch it. Keep line heights in em in `Type.kt`.
5. **Corners come from `Corner` / `MaterialTheme.shapes`; cards default to `leafCard`.** A container is never a polygon and a badge is never a rectangle.
6. **Motion uses the named springs in `Motion.kt`.** Never an inline `spring(...)`. Their numbers were tuned on the phone: do not change one without a device pass.
7. **Headings and menus use the shared components**: `ScreenTitle`, `SectionTitle`, `MenuSection` / `MenuGroup` / `MenuRow`.
8. **The logo is generated** (`tools/logo.py`). Change the geometry there, never the XML or `BrandGeometry.kt`.

## Frozen: stored in users' settings

Renaming any of these silently resets or breaks a saved choice.

- Tile preset keys (`lime`, `heating`, `cooling`, `lock`, `alert`, `media`, `amber`, `sage`).
- `corners_0` .. `corners_7`, and the keys in `SHAPE_OPTIONS`.
- Home section keys, including `round3-security`, `round3-vacuum`, `round3-media`, `round3-greenhouse`.
- SharedPreferences file and key names in the stores.

## Things that bite

| Trap | What happens |
|---|---|
| Polygon morph progress outside 0..1 | Throws. A bouncy spring overshoots; clamp with `clampMorphProgress` first. |
| The wide layout judged on width alone | Split-screen flips to the tablet layout. Use `isWideLayout` (600dp and at least as wide as tall). |
| A wide control as a `MenuRow` trailing | Fine: the row drops it under the text. Do not work around it with a fixed width. |
| A `FilterChip` with stock colors | Its selected state is not the lime capsule. Pass `choiceChipColors()`. |
| `targetSdk` bumped along with `compileSdk` | Changes runtime behaviour (predictive back, large-screen rules) that has not had a device pass. `compileSdk` moves freely; `targetSdk` is held at 35. |
| Media3 bumped casually | The camera transports (HLS, RTSP) were tuned against 1.6.1. Bump it with a camera test pass. |
| A screenshot test waiting for idle | Never returns: something is always animating. Drive `mainClock` by hand, as `shoot()` does. |

## Known debt

- `HavenApp.kt` and `ui/screens/Screens.kt` are very large. Split by feature when a change gives a reason to; do not reorganise them in passing.
- About 280 inline `fontSize` call sites.
- The Home panels (vacuum, media, security, greenhouse) use fixed entity ids in `data/HomePanels.kt`.
- Test files named `RoundNNLogicTest` are regression suites from individual feedback rounds and mix features; new tests go in a file named for the thing tested.
- Release builds are not minified.

## Writing here

Comments say what the code does and why it is that way, for someone reading it cold. They do not narrate the history of how it got there; that belongs in the commit message and `CHANGELOG.md`.
