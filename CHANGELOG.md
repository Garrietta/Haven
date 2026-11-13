# Haven Changelog

## 2.9: A Climate tab, and rooms without a plug's housekeeping (2026-11-13)

**What you will see**

- A Climate tab sits right of Security. At its top, the house in one card: what it is inside (the thermostat's reading), the humidity, the outside, and what the thermostat is doing, in ember while heating and frost while cooling.
- Each thermostat is a card: its reading, its set point with − and + beside it (or heat and cool set points for a range), its modes, its comfort settings (an ecobee's Home, Away and Sleep) and its fan, with the dial and history a tap away.
- Rooms lists every temperature and humidity reading, one card a device (an ecobee remote sensor's temperature and humidity together), each with a bar placing it between cold and hot and its last day as a line. Outside holds the weather and any outdoor sensor.
- Show Climate on bar is under Home & navigation.
- A device's configuration and diagnostic entities no longer fill the room it is in. A dual smart plug in the Greenhouse shows its two outlets, not also its LED, auto-update, cloud connection and signal level, on Home, on the room's page and in its count.
- Adding to an area moves just what you picked: one outlet of a dual plug can be the greenhouse fan while the other is elsewhere. The area lists each device with what of it is there, by name, and Remove takes all of it out.

**Under it**

- `data/Climate.kt` holds the rules (which sensors, grouped by device, outdoor by name, the summary, a thermostat's activity, labels), tested in `ClimateTest`. `ui/screens/ClimateScreen.kt` draws them; `navShowClimate` joins the bar's built-ins.
- The entity registry's `entity_category` and `hidden_by` mark auxiliary entities (`HaClient.auxiliaryEntities`); they are left out of `entityAreas`, so out of every room.

## 2.8: Devices land in their area (2026-11-12)

**What you will see**

- A device you put in an area in Home Assistant now shows up there in Haven, on Home and in Settings. Before, a heater or fan set to Greenhouse left Greenhouse at "0 devices" and empty on Home.
- A device's page in Settings leads with its on/off. Its entities follow in Home Assistant's groups (Controls, Sensors, Configuration, Diagnostic), named without the device's name in front, with a switch on anything that turns on and off and a Press button on buttons. Tap one for its controls; the tune button opens its settings.
- An area in Areas, labels & zones lists its devices, the way Home Assistant does: one row for a smart plug rather than one for each of its sensors and timers. Add a device and Remove move the whole device; something that belongs to no device still moves on its own.

**Under it**

- Android's `JSONObject.optString` returns the text "null" for a JSON null, so every `"area_id": null` on an entity read as an area called "null" and hid the device's area behind it. `JSONObject.text()` (`data/HaJson.kt`) reads null as empty, and the registry, event and media parsers use it. The JVM's org.json returns "" there, which is why no test caught it.
- `entityGroup`, `mainControl`, `nameUnderDevice` and `areaMembers` in `HaAdmin.kt` are pure and tested in `DeviceGroupingTest`.

## 2.7: Haven updates itself (2026-11-12)

**What you will see**

- Settings, App updates shows the build installed and the newest test build. Download and install fetches it and hands it to Android, which asks you to confirm. The first time, Android asks you to let Haven install apps.
- The App updates row in Settings says when a newer build is waiting.

**Under it**

- CI's new `publish-test-build` job replaces one pre-release, `test-build`, after every signed build, with the APK and a `Build: <run number>` line. The app knows its own run number (`BuildConfig.BUILD_NUMBER`, 0 locally), so a higher one is newer. The repository is public, so no sign-in is needed.
- `update/ApkInstaller.kt` downloads into the cache and commits a `PackageInstaller` session; `UpdateInstallReceiver` shows Android's confirmation and words a refusal (an APK signed with another key cannot update this one). `REQUEST_INSTALL_PACKAGES` is new.
- A debug build says that test builds install beside it, since they are the release app.

## 2.6: Settings laid out like Home Assistant's (2026-11-12)

**What you will see**

- Settings opens on Home Assistant's own sections, in its order: Devices & services, Automations & scenes, Areas, labels & zones, Dashboards, People, System and About. Haven's own settings follow under Haven: Appearance, Device styles, Home & navigation, Notifications and Connection.
- Devices & services has Home Assistant's four tabs. Integrations: reload, disable or delete one, and add a new one by answering Home Assistant's own questions (a step that needs a browser sign-in opens the browser). Devices: rename, move to an area, disable, and see their entities. Entities: search every entity and open its page, with Home Assistant's settings (name, icon, area, entity ID, visible, enabled) and how its card looks in Haven. Helpers: create and delete.
- Automations & scenes lists every automation with its switch and when it last ran, every scene and every script: run, switch, delete, or start a new one.
- Areas, labels & zones holds your rooms (each with Home Assistant's settings and Haven's, and a grip to set their order on Home), and adds floors, labels and zones.
- People lists people with their trackers, to add, rename or remove, and the users. System has updates, repairs, logs, backups, the general settings (name, units, time zone, location), checking the configuration, reloading YAML and restarting, which asks first. About shows the versions.
- Device styles sets the color, badge shape and card shape for each type of device (every lamp, every lock) with a preview. The Card settings, Badge shape and Card shape rows are gone from popups; one device can still differ from its type on its own page.
- Rooms and tabs each have one place: rooms under Areas, your tabs under Home & navigation. Browse, Shapes, Rooms & tabs, User tabs and the Create hub are gone from Settings.
- Every list in Settings reorders by dragging its grip: Home sections, favorites, dashboard cards, rooms, tabs and a tab's cards. The arrows are gone.

**Under it**

- `data/HaAdminApi.kt` makes the calls (the WebSocket admin commands and the REST config API, through new awaited `HaClient.command` and `HaClient.rest`); `data/HaAdmin.kt` parses the replies, with 11 tests. `ui/screens/Settings*.kt` are the pages, routed by `Screen.SettingsPage(route)`.
- `RoomsTabs.kt` now draws Areas and Your tabs as two pages. `Screen.RoomsTabs`, `Screen.BuilderHub` and the type-defaults dialog are gone; `reorderSubset` moves the shown part of a list among the rest.
- An area carries its floor.

## 2.5: a Security tab (2026-11-12)

**What you will see**

- A Security tab beside Home on the bar. It opens with one line: "Everything is shut and locked." when it is, or what is not ("Back Door is open, and Side Door is unlocked.") in red.
- The alarm as four choices: Home, Away, Night and Off. A panel that asks for a code opens its keypad instead.
- Every camera, as a grid of their pictures. Tap one to open it, live where it can be.
- Motion detection: each camera's detection switches (motion, person and so on) under the camera's name, with when it last saw motion where the camera reports it. Switches that belong to no camera are listed by their own names.
- Doors & locks: locks, garage doors and door, window and motion sensors, each opening its own popup. A lock still asks before unlocking.
- The outdoor siren, which sounds only after a second tap.
- The cameras have moved off Home to this tab. Manage home page can put them back on Home.
- Security can be taken off the bar in Rooms & tabs. The bar holds six at most, so with Security, Scenes and Energy all on there is room for one pinned tab or room; a switch that would push a pinned tab off says the bar is full. After the update, Security only appears on its own if it fits beside what you have pinned.

**Under it**

- `data/SecurityPage.kt` (opening line, detection switches by device, labels) with 7 tests; `ui/screens/SecurityScreen.kt` draws it. `barExtraSlots` keeps the bar at six; NavItems has 2 new tests.
- A one-time move hides the Home Cameras section (`cameras_moved_to_security`). `nav_show_security` is absent until chosen.
- The state cache restore can carry device links, so the screenshot test draws cameras with their switches.

## 2.4: drag to reorder in Rooms & tabs (2026-11-12)

**What you will see**

- Rooms & tabs lists each room and each tab as one row: a grip, its icon, its name, and a line saying how many devices or cards it holds and where it is pinned.
- Hold a row's grip and drag it to move it. The row lifts and the others step out of its way; the order is saved when you let go. The order of rooms is the order Home shows them in, and the order of tabs is the order of User Tabs.
- Tap a row to open its settings under it: names, icon, devices and where it is pinned for a room; title, icon, cards and where it is pinned for a tab. One row is open at a time, so the list stays short enough to reorder.
- A tab's cards reorder the same way, by their grips. The up and down arrows are gone everywhere on this screen.
- With TalkBack, each grip offers Move up and Move down.

**Under it**

- `ui/components/ReorderList.kt`: `LazyListReorder` for rows of a LazyColumn (reads where rows really are, so rows of different heights reorder correctly, and scrolls the list while a row is held near an edge) and `ColumnReorder` for equal rows inside one item. The rules (which row is under the dragged row's middle, edge scroll speed, equal-row steps) are pure functions with 5 tests.
- Rooms & tabs moved out of `Screens.kt` into `ui/screens/RoomsTabs.kt`; `HavenViewModel.setTabOrder` writes a whole tab order at once.
- Screenshot tests open a room and drag one, and check the stored order. The screen is drawn on its own in them: nothing on it animates at rest, so the test pushes state changes through by hand.

## 2.3: device popups lead with one control (2026-11-12)

**What you will see**

- A lamp's popup opens on one large slab in the lamp's yellow. The fill is the brightness: drag up or down anywhere on it to set the level, tap it to switch the lamp on or off. It replaces the small on/off pill and the thin brightness slider.
- Warm to cool is a track painted from amber to cool white, with a handle where the lamp sits. Tap a point on it or drag the handle.
- Switches, fans and locks lead with the same slab. A fan's fill is its speed and can be dragged; a plug with a meter says how many watts it is drawing; a lock is full and blue while locked and asks before unlocking, as before. The Turn on / Turn off / Toggle buttons are gone from these popups.
- The card options at the foot of every popup (Card settings, Badge shape, Card shape) sit together in one group under "Customize".
- The lamp's details card no longer repeats on and brightness; it keeps the room, when it last changed and its smart plug.

**Under it**

- `ui/controls/ControlSlab.kt`: `ControlSlab` and the four device slabs, plus `ColorTempTrack`. The level, kelvin and watt rules are pure functions with 7 tests.
- `powerWatts` (data/SemanticColors.kt) is the one reading of a plug's meter, used by the tile's family and the slab's caption.
- Screenshot tests for the switch, fan and lock popups and the lamp popup at night.

## 2.2: popups that grow, not stretch (2026-11-12)

**What you will see**

- A device, room or status popup now grows out of its tile as itself: it is laid out at full size from the first frame and revealed through a window that opens from the tile's rectangle to the popup's. It used to be the finished popup scaled down to the tile and back up, which stretched its text and icons on the way.
- Opening and closing settle the way Rondo's music player does: no bounce against the popup's edges, and a calmer arrival.
- Pull-to-dismiss runs the same motion backwards under your finger.

**Under it**

- `RevealWindowShape` (ui/components/MorphOverlay.kt) clips a full-size layer to the in-between rectangle; the folded surface and the unfolded centered popup both use it, moved but never scaled.
- `popupMorphSpring` is 350 / 0.95, Rondo's player transform, up from 500 / 0.85, on the owner's word that the popups felt underworked beside that player.
- The centered popup measures itself for the drag range only when fully open.

## 2.1: Home as rooms (2026-11-11)

The first screen is laid out around the rooms of the house rather than a grid of equal tiles.

**What you will see**

- Home opens with a sentence about the house: how many lights are on, and whether the doors are locked. The last word is a pill in that state's color, so "locked" reads calm and "unlocked" reads as a warning. A welcome message you set yourself still replaces it.
- Under it, one card per room. A room with something happening wears that state's color and says what in a line ("Floor Lamp is on, Heating to 70°"); a room with nothing happening is a single quiet line.
- Each card carries the room's devices as round switches. Tap a light, plug or fan to flip it; tap anything else, or hold any of them, to open its controls, which grow out of the switch.
- Tap a card to open that room as a popup that grows out of the card: its name, what is happening in it, and every device as a tile. A tile opens its control on top; closing that comes back to the room, and closing the room comes back to Home. The chip view of a room is no longer how a room opens.
- The sentence is also the controls. Each line ends in a state word you can tap to undo it: "on" turns those lights off, "open" closes the garage door, "running" sends the vacuum home, "playing" pauses. "locked" opens the lock's control instead of unlocking. Tap the inside temperature to open the thermostat.
- A lit lamp is yellow everywhere: its tile, its switch on a room card and the "on" in the sentence. The room card itself stays green.
- Home Status is gone from Home while the sentence is showing, since the sentence does its job. Cameras, Favorites and the other Home sections follow the rooms, in the order you set.

**Under it**

- `data/RoomDigest.kt` decides each room's color, sentence, temperature and switches, and the headline with its actions; `ui/screens/RoomsHome.kt` draws them. 17 tests cover the rules.
- A new state family, Light, on the Volt ramp. By day its badge is the bright tone, the one exception to the tone recipe besides Quiet.
- Rooms shown as cards follow the same placement setting the chips did. "No room" is always the quiet line.

## 2.0: a new identity (2026-11-10)

A redesign of the whole visual system, not a feedback round. `DESIGN.md` is now the source of truth for how the app looks and moves.

**What you will see**

- A new logo: a house with the light on in its doorway and the sun rising behind the roof. New launcher icon (with a themed-icon version), a splash screen that matches the app's background, and a Haven icon on notifications in place of the stock info glyph.
- A new typeface, Google Sans Flex, rounded, across the whole app. Text that used to look double-spaced or letter-spaced now sits properly.
- A rebuilt sign-in screen: the address and the instances found on your network in one card, one "Sign in" button, and the access-token route one tap away.
- Home opens on the Haven signature and the greeting as the big line.
- Lime now means "this one" everywhere: the selected tab, room, mode and choice.
- Tiles for temperature, humidity and motion follow the same color recipe as every other tile. Every default card is a leaf card; the sharp and pillow outlines are still in the shape picker.
- Settings rows have calmer badges, and a wide control (the thermostat range steppers) now drops under its text instead of squeezing the title.

**Under it**

- Every color is a tone of a generated palette (`tools/palette.py`) read through ten state families; no hex values in screens. Hues are the ones the app already used.
- The theme is `MaterialExpressiveTheme` on material3 1.5. Toolchain: AGP 9.3, Kotlin 2.4, Gradle 9.6, compileSdk 37 (targetSdk still 35).
- Haven's own springs are unchanged in value and now live in `Motion.kt`.
- A screenshot harness renders the real app on the JVM with a demo home: `./gradlew :app:recordRoborazziDebug`.
- CI tests, renders and builds the signed release for every pull request and for `main`.
- 538 unit tests pass. Tests that pinned hex values now pin the decisions and the contrast floors.

**Not yet seen on a phone:** motion, haptics, camera streams and real insets cannot be judged on the JVM. Worth a look on the Fold: the splash on a cold start, the sign-in screen with the keyboard up, popup morphs with the new type sizes, and the themed icon.

## Feedback round 25 (2026-11-05, seven items from his written spec on #135 — no video attached)

PASSES from #135 (do not regress): badge glyphs match their cards; vacuum/garage split heroes; swipe-down settles at tap-outside calm speed; no tile flash on ×3 cycles; default card shapes stable on Climate/Lights tabs; Settings compact on unfolded; Tall visibly doubles.

1. **"Sign-in expired" signs him out when he taps the status line.** DIAGNOSED FIRST: both TodayScreen call sites in HavenApp.kt wired `onSignInExpired = onSignOut`, and `vm.signOut()` (HavenViewModel.kt) does `client.disconnect()` + `tokenStore.clear()` + `isAuthenticated = false` — so tapping "Sign-in expired — tap to sign in again" wiped the stored refresh token and forced a full re-sign-in. `HaClient`'s `auth_invalid` branch only set `ConnectionState.Error(...)` with no refresh attempt and no callback to the VM; `refreshAndConnect()` existed but was called only from `autoConnect` when the token was near expiry, and on refresh failure it set `isAuthenticated = false` + "Your session expired" with no alternate-URL fallback; `scheduleReconnect()` was a fixed 3s retry to the SAME URL with no backoff growth. FIX: the status line now calls new `vm.reconnectNow()` (renamed param `onReconnectTap`; line reads "Sign-in expired — tap to reconnect"), which starts the auth-retry loop: `tryRefresh(target)` → `client.connect()` on success; in AUTO, the alternate URL is tried before waiting (`alternateConnectionTarget`, pure, in Round16Logic.kt); backoff 2s/5s/10s/30s/60s (`authRetryDelayMs`, pure), giving up only after 8 attempts (~5 min) — and only then surfacing "Your session expired. Please sign in again." With no refresh token stored (long-lived-token login) it gives up immediately, since there is nothing to retry. `HaClient` gained `onAuthInvalid` / `onTransportFailure` callbacks (wired in `HavenViewModel.init` before `autoConnect`); a transport drop in AUTO switches to the alternate URL immediately (same token works through both), at most once per 30s so a flapping link cannot ping-pong; `scheduleReconnect()` now backs off exponentially (3s, 6s, 12s, 24s, 48s, 60s cap) and resets on connect/auth_ok. `signOut()` cancels the retry loop first and stays strictly on the Settings button — nothing else in the app calls it. Unit-tested (Round25LogicTest): alternate-target selection for AUTO/LOCAL/CLOUD, missing-alternate keeps current, both-unset is null, backoff grows then caps at 60s.
2. **Cold start shows an empty home until the socket connects.** DIAGNOSED FIRST: camera snapshots were already cached on disk (`filesDir/camera_snapshots/*.jpg`, loaded in `init` via `loadDiskSnapshots()` before first composition) — the snapshot half already existed. The gap was states/areas/entityAreas: no `haven_state_cache.json` existed, and `HaClient`'s `_states`/`_areas`/`_entityAreas` are private MutableStateFlows with no restore path. FIX: new pure `serializeStateCache` / `parseStateCache` (data/StateCache.kt — the manufacturer field is deliberately not serialized; it is patched in from the device registry after connect) + `HaClient.restoreCachedStates()` (applies only non-empty parts, so a partial cache never blanks a part that already arrived). `HavenViewModel.init` loads the cache synchronously right after `loadDiskSnapshots()`, before first composition; a `combine(states, areas, entityAreas)` collector persists the cache throttled to one write per 10s (empty states are never written, so a blank write cannot poison the next cold start). On cold launch TodayScreen has cached entities, `heroCameras` resolves the 4 camera IDs, CameraQuadrant paints the cached disk JPEGs on frame 0 with no green-placeholder flash, and the socket updates silently behind. Unit-tested (Round25LogicTest): round-trip with nested maps/lists/numbers/timestamps, empty document parses to empty, corrupt input is null.
3. **Vacuum popup buttons invisible at rest; setup/consumables truncate.** DIAGNOSED FIRST (measured, not guessed): `PillAction`/`SquircleAction` default to `secondaryContainer` when the container color is null, and the docked vacuum's `detailPageAccent` is null (`presetAppliesForState` is false when docked, round-21 reversal) — so the 7 hero buttons were `secondaryContainer` on the popup's `surfaceContainerHigh`: light 0xFFDCE5D0 vs 0xFFDDE5D0 and dark 0xFF2A352A vs 0xFF2B2B2B, both ~1.0:1 — the buttons blended into the popup until pressed. FIX: the hero fill is now `vacuumPanelAccent ?: primaryContainer` (tonal green, like the Garage Door buttons) with `onPrimaryContainer` text when no preset applies; a set tile preset still wins, as before. All 7 hero buttons (Start, Pause, Return to Base, Empty Dust Bin, Wash Mop, Dry Mop, Stop Dry Mop) plus the no-map "Refresh map" fallback get the fill. Below the hero, Cleaning setup + Consumables go BACK to 1-column stacked full-width PanelCards — an explicit reversal of round 24 item 2's 2-column grid for setup/consumables only; the 2-column top hero stays.
4. **Thermostat popup controls are wireframe outlines.** FIX: the `SegmentedOptionBar` (SingleChoiceSegmentedButtonRow) is deleted; Mode (Off, Heat, Cool, Heat • Cool), Fan (Auto, On), and Heat/Cool are now solid filled pill buttons (`FilledOptionRow`, PressablePillButton like PanelPill): `secondaryContainer` unselected, `primaryContainer` selected — or the caller's domain-tinted pair for Heat/Cool (heat/cool setpoint containers, unchanged) — 8dp spacing, zero thin wireframe borders. The −/+ `ExpressiveStepper` buttons get a visible contrasting fill at rest (`secondaryContainer` by default, the tile preset accent when set, as before) plus a subtle 1dp `outlineVariant` border in the popout (`PressablePillButton` gained a `borderColor` param, before the trailing `content` lambda; `ExpressiveStepper` gained `outlineColor`). Thermostat behavior is untouched — dial rules (1°F snap, handle-grab-only, inside-tap toggle, clamps, gap-proofing), setpoint reporting, and mode/fan service calls are byte-identical; the Hero's steppers are unchanged (no outline).
5. **Unfolded room-chip switches leave the left pane empty; a single Climate card spans the whole tab.** FIX: on unfolded, Cameras + Home Status stay visible in the left pane (42%) at all times — the `!isRoomView` gates are removed from those two left-pane blocks only, a scoped exception to round 18 item 1's "room view hides overview blocks" rule; the right pane still switches (Favorites + overview on My Home, the room's RoomSectionGrid on room views) and the other overview blocks still hide on room views. `TabTileGrid` locks NORMAL cards to a 2-column grid (`GridCells.Fixed(2)` on folded and unfolded), superseding the round-6 Adaptive rule (220dp wide / 160dp folded) — a single NORMAL card on Climate now occupies one column instead of stretching across the whole screen like a WIDE card. WIDE cards still span the full row; the reorder vertical step is one full 2-column row.
6. **Tall card edges don't align with its two stacked Normal neighbors.** DIAGNOSED FIRST: the round-24 pairing already existed in FavoritesSection and RoomSectionGrid, but the spec's structure was missing — the paired Row had no `height(IntrinsicSize.Max)`, the TALL cell was `weight(1f)` only (no `fillMaxHeight`/`heightIn`), the stacked Column used manual Spacers instead of `spacedBy`, and `HomeRoomCard`'s Surface got no `fillMaxSize`. With the unfolded 14dp spacing, 136×2+14 = 286 ≠ the 284dp `tallCardHeightDp()`, so a spacing-blind height could never align there. FIX: the paired Row is `height(IntrinsicSize.Max)`; the TALL cell is `weight(1f).fillMaxHeight().heightIn(min = tallPairHeightDp(spacing))` (new pure `tallPairHeightDp(spacing) = 136.dp * 2 + spacing` — 284dp at 12dp spacing, 286dp at 14dp); the stacked Column is `weight(1f)` with `verticalArrangement = Arrangement.spacedBy(spacing)` (manual Spacers removed); `HomeRoomCard` takes a new `tallHeight` param (before the trailing `onOpenPanel` lambda) and the Surface fills it with `fillMaxSize`. The TALL height on the Surface is now a FLOOR (`heightIn(min = tallHeight ?: tallCardHeightDp())`), not an exact height, so the card fills its cell exactly even when a stacked neighbor grows past 284dp — the round-24 failure cannot recur, since the floor IS the TALL height. Unit-tested (Round25LogicTest): 12dp → 284dp (== `tallCardHeightDp()`), 14dp → 286dp.
7. **Resize clips text mid-drag; drag reorder is choppy.** DIAGNOSED FIRST — the clipping crop: only the wrapper Box animated (`resizeSettle` = `animateContentSize(settleSpring())`) while the card's Surface snapped to its target size instantly, so a growing card overflowed its cell mid-animation and the neighboring card painted over it — the Ecobee text "chopped in half" on Wide/Tall/Normal switches. The reorder gaps vs the spec: `reorderDrag` already implemented drag-under-finger with a 1.04 lift, half-cell center-crossing steps, and a spring return — but `zIndex` was 1f, no shadow, no placeholder outline, and the remaining cards re-laid-out instantly on list change. FIX: `resizeSettle()` now uses `animateContentSize(springSpatial())`, and the `HomeRoomCard` Surface animates its own size changes on the same spring, so the card never overflows its cell and no text is ever clipped mid-drag. Reorder is now launcher-style: the dragged card floats at `zIndex(10f)` with 1.04 lift and `shadowElevation` 16dp, a subtle rounded placeholder outline marks its current target slot (drawn outside the drag transform, so the outline stays in the slot while the card moves), the center-crossing step rule is extracted pure as `reorderSteps` (one slot per crossing, offset re-bases by a full cell), and the remaining cards glide into their new slots via `slotSpringOffset` (position tracked with `localToRoot`, animated from the old slot with the spatial spring; scrolling never triggers it; the dragged card is excluded while dragging) — the lazy tab grid uses the same slot spring with its card index as the key. Two 1.8.2 API notes, verified against the cached jars: `Offset.VectorConverter` and `animateItemPlacement` do not exist in this Compose version, so the slot-spring converter is spelled out explicitly and the lazy grid uses the slot spring instead of `animateItemPlacement`. Unit-tested (Round25LogicTest): sub-threshold no-step, half-cell crossing steps once and re-bases, negative crossing steps backwards, long drag walks multiple slots.

Not in this round (kept, no action): the settings epic queue (Devices & services → Automations → People → System); the garage-door confirmation (decided against, permanently); the front-door wave test (his device action, still open). WebRTC stays gated off (`webrtcEnabled = false`).

PASSES from this video (do not regress): the app is looking pretty good overall; chart colors read well (red temp / blue humidity); thermostat and lamp badges read correctly; room chips work; the morph routing holds; a defined card shape locks.

1. **Badge glyphs — garage door and eufy AE C10 render white icons on green/sage cards.** DIAGNOSED FIRST: `MatrixRow.toCardColors` (SemanticColors.kt) applied the glyph rule to ACTIVE rows only (glyph = container color in light, dark ink in dark); the INACTIVE row hardcoded `Color.White` (light) / grey (dark). Garage Closed renders INACTIVE because its preset is set but `presetAppliesForState` is false for closed (`tileBadgePair` branch 2 → `inactiveCardColors`); eufy Docked takes the vacuum else-branch → INACTIVE. Pixel samples from the annotated frame confirmed it: lamp/ecobee glyphs equal their containers; garage/eufy glyphs are white. FIX: the INACTIVE row now follows the same rule — light glyph = `lightContainer` (sage 0xFFE6EBD3 on badge 0xFF52564E, ~6:1); dark glyph = `hslToColor(hue-of-container, 1.0f, 0.72f)`, the `presetBadgeColors` formula. Active rows are untouched (thermostat/lamp rendering unchanged). `tileBadgePair` is the single badge-pair source for cards, headers, and the thermostat popout, so the fix propagates everywhere. Unit-tested (DomainCardColorsTest: the inactive-dark 3:1 exception is removed — 4.5:1 for all now — plus a new `inactiveGlyphMatchesContainer` test).
2. **Vacuum and garage popups — stacked layouts, long scroll.** FIX: the vacuum gets a split hero — Lidar map left (weight 0.48, 24dp container, `aspectRatio(0.85f)`, `ContentScale.Crop` so the snapshot fills the frame with no black sidebars), Start / Pause / Return stacked as full-width pills plus a compact 2×2 squircle grid (Empty Dust Bin, Wash Mop, Dry Mop, Stop Dry Mop; new `SquircleAction` on the `MediaSquircle` shape with two-line labels) right (weight 0.52). Cleaning setup + Consumables are their own cards (`VacuumCleaningSetupCard` / `VacuumConsumablesCard`) in a 2-column grid on wide screens, stacked on narrow (a consumable row needs the full width for label + bar + Reset pill). The garage gets the same treatment: `GarageDoorVisual` 150×168 left, Open / Close / Stop stacked vertically right. Service calls are byte-identical; the invalid-direction law (disabled + 0.38 alpha) is untouched; no confirmation was added (decided against, permanently).
3. **Swipe-down dismiss too fast inside popups; tile flash STILL present on #134.** DIAGNOSED FIRST — why round 23 missed it (three compounding breaks): (A) the tile alpha reads the ANIMATABLE, not the finger-tracked progress: `LocalMorphCardAlpha` provides `morphTileAlpha(rootMorphProgress(), …)` while surfaces render with `dragProgressFor`; during a drag the tile sits at 0 by luck, but on release-with-commit `dismissDetail()` snaps `detailProgress` to the drag progress and flips `detailExiting=true` BEFORE the settle runs, so the tile alpha JUMPS 0 → `morphExitCardAlpha(0.3)` ≈ 0.65 in one frame (0.96 at dragProgress 0.1) while the container is still a small solid shape. Round 23 assumed every close starts at p=1 (tap-outside/Back path); the drag path enters the exit curve mid-way at high alpha. (B) During the drag-settle, `isClosing = exiting && !settling` is false, so the container follows the OPEN fade while the tile follows the EXIT fade — both partially visible at once. (C) Drag range degenerate for centered tiles: `dragRangeForCenteredPopup` = `abs(placedCenterY − tileCenterY)` — near-zero for Standing Lamp, so a small finger movement scrubbed the whole morph 3–4× too fast; `dragRangeForPanelOverlay` = `sourceTop − targetTop`, same class. FIX: `morphTileAlpha` is now BINARY on both enter and exit (1f at progress ≤ 0.01, 0f above — the round-23 fade is SUPERSEDED, not re-applied); new `morphSwapContainerAlpha` keeps the container at 1f down to 0.01 while its background lerps to `fromBackground`, so the moving surface IS the single visible card until landing; both drag ranges gain a `minDragRangePx` floor (call sites pass 380.dp); `dismissDetail()` / `dismissPopout()` / `dismissPanelDetail()` settle on `popupMorphSpring()` (the open's spring) instead of `settleSpring()`, so swipe-down settles at the same calm speed as tap-outside. Unit-tested (Round24LogicTest + Round23LogicTest updated to pin the binary rule): drag-range minimum, swap alpha binary, drag-snap cases stay hidden, tile + container sum to exactly 1 at every progress.
4. **Default card shapes mutate on Climate/Lights tabs.** DIAGNOSED FIRST: `TabTileGrid` declared `var tileIndex = 0` in the LazyVerticalGrid DSL scope and did `gridIndex = tileIndex++` inside `item {}` lambdas. A single item's recomposition re-invokes that item's lambda without the scope re-running, so the shared counter had moved on and the card got a different `gridIndex` → a different `generalCardShape` corner variant on every cycle. Defined shapes were immune (the picker override ignores `gridIndex`) — exactly "this one I defined the card shape. This one is on the default. So when it's default card shape, it changes every time." The spec's "background tabs overwrite the maps" half is WRONG: HavenApp renders exactly one screen via AnimatedContent, so inactive tabs are never composed. FIX: new pure `tabTileGridIndices(cards)` precomputes the flattened entity → index map before any item is emitted (gauge/button/unparseable contribute no ids; missing entities still consume an index; duplicates keep their first index); the three item sites read `fixedIndexById[id] ?: 0`. Unit-tested (Round24LogicTest).
5. **Settings muddy and all over the place; rooms can't move up and down.** FIX: the 4 thermostat dial rows move out of Settings into the thermostat popout's bottom settings section (new reusable `ThermostatDisplayRangeRows`, alongside Badge shape / Card shape / Card settings — same rows, same store writes). The 4 Create rows collapse into one "Automations, scenes & helpers" row opening a new `Screen.BuilderHub` screen (the four existing builders, unchanged). The remaining sections are extracted into their own composables and laid out in two columns on wide screens (left: Appearance through Notifications; right: Shapes through System plus Sign out), one column on narrow. The duplicate Rooms display-name/order block leaves Manage home page; Rooms & tabs room cards gain the display-name field, the icon picker (`TAB_ICON_OPTIONS` + `setRoomIconKey`), Up/Down reorder (`moveRoom`: swap with the neighbour in stored room order, `setRoomOrder` + `bumpHomePrefs`), and render in stored room order; the room's chosen icon leads its heading. The hide switch goes with the block — PlacementSelector Hidden covers visibility. Nothing is deleted: every setting remains reachable.
6. **Tall does nothing; resize has no live feedback; no drag reorder.** DIAGNOSED FIRST: `HomeRoomCard`'s Surface modifier was `fillMaxWidth().heightIn(min = 136.dp)` — the `heightIn(min=136)` AFTER the caller's `fillMaxSize()` clobbered the max-height, so the card measured to content height (~136dp) regardless of the wrapper Box's `heightIn(min = 272.dp)`; the Box was 272 tall but had no background, so visually NOTHING changed. FIX: TALL sets the Surface to an exact 284dp (`tallCardHeightDp()` = 136×2+12) with a `weight(1f)` Spacer between badge row and title/status (both `HomeRoomCard` and `HomeRoomMediaCard`); 2-column grids pair a TALL card with up to two stacked NORMAL cards in the adjacent column (284dp flush; a second TALL gets its own paired row; WIDE never stacks); `TabTileGrid` takes the 284dp cell height (no lazy row spanning — round-20 limit — its row-mate stays top-aligned). `CardResizeHandle` gains an `onPreview` callback feeding `resizePreviewScale` (same `snapCardSize` rule; stretches toward the pending size up to 1.8× on the pending axis at a full 96px drag) via `ArrangeableCardCell`, one place owning both Arrange gestures. Drag reorder in Arrange mode (new `reorderDrag` modifier: the card follows the finger with a 1.04 lift, one move per half-cell / half-row crossing, springs into the slot on release): FavoritesSection → `moveFavorite` (mapped through the display order, since wide-media rows render first), RoomSectionGrid → `moveRoomEntity` (new per-room entity order in SettingsStore: `room_entity_order_json`, resolved against HA membership at render), TabTileGrid → `moveCardInTab`. All three persist across a restart. Known interaction: the card's long-press still toggles Arrange mode, so a press-hold-then-drag can exit Arrange mid-drag — reorder drags are prompt motions, not holds. Unit-tested (Round24LogicTest): Tall height math, `movedByDelta` (moves/clamps/no-ops), `resizePreviewScale`.

## Feedback round 23 (2026-11-05, two defects from the narrated video on #133)

PASSES from this video (do not regress): every tile opens as a morphing popup (round 22 item 1 routing holds); Lamp opens Lamp, never Garage Door (item 4); room chips work; the thermostat dial reads well; the wide-tile morph is consistent ("the width of the tile doesn't affect the consistency of the animation"); the Card shape picker works and a defined shape locks.

1. **Exit flash — the popup collapses to a bare solid-color shape on close.** DIAGNOSED FIRST: the close curves are misaligned by 45% of the animation. The popup content fades out early on close (`morphCloseContentAlpha` — fully gone by progress 0.85, CenteredPopup's content graphicsLayer and MorphSurface's content box), while the source tile fades back in late: the root provides `LocalMorphCardAlpha` from `morphCardAlpha(rootMorphProgress())` (HavenApp.kt), and `morphCardAlpha` keeps the tile invisible until progress 0.4 — and the container stays fully opaque on close (alpha 1f when isClosing) with its color lerping to the tile's exact container color over [0, 0.35]. So for close progress in (0.4, 0.85) the frame holds an invisible tile, invisible content, and a fully opaque solid-color surface shrinking toward the tile — the solid yellow shape that "flashes back and then boom the standing lamp UI is there", same on the Ecobee popout. The designed fix already existed as dead code: `morphExitCardAlpha` (round 4 — "keeps the card hidden exactly while panel content is still visible, then fades the card in smoothly over [0.85, 0]") had zero call sites anywhere in main or test. FIX: new pure selector `morphTileAlpha(progress, closing)` (MorphOverlay.kt) — open follows `morphCardAlpha` (gone by 0.4), close follows `morphExitCardAlpha` (fading in from the exact moment the content is gone, so no frame has both at full opacity and no frame has neither). The root provider now supplies `morphTileAlpha(rootMorphProgress(), morphClosing)` where `morphClosing` is the hoisted `detailExiting || panelDetailExiting || popoutExiting` (the same expression LocalMorphClosing provides, so the two can never disagree about direction). The camera dialog is untouched (its progress is not in rootMorphProgress, and its closing flag was never in the OR). Finger-drag dismiss is unchanged: during a drag the exiting flags are false, so the drag still follows morphCardAlpha. Unit-tested (Round23LogicTest): open branch equals morphCardAlpha; close branch equals morphExitCardAlpha; the handoff at 0.85 is exact (content 0, tile starting from 0); no dead zone at 0.6/0.7 on close.
2. **Shape not locked — the tile changes shape on every tap/exit when no explicit shape is defined.** DIAGNOSED FIRST: the resting tile is NOT drifting — pixel measurement across the video's at-rest frames shows Lamp (28, 8, 28, 28) and Standing Lamp (8, 28, 28, 28), exactly generalCardShape(2)/(3) by grid index, stable across cycles. The capture is also correct (round 20's `anim.sourceShape`, read from the tile's reported shape at open) and the surfaces lerp toward it (`morphPopupCorners`: 28dp at rest to the tile's corners at landing). The break is the clip's coordinate space: the unfolded CenteredPopup applies that clip inside a graphicsLayer that ALSO scales the whole layer by tileMorphTransform's scaleX/scaleY (tile size / popup size), and a graphicsLayer clip is defined in the layer's local coordinates — so the VISIBLE corner radius is the local radius times the scale. At landing the surface shows the tile's corners multiplied by the scale (a lamp tile at scale ~0.6–0.75 renders 28dp as ~17–21dp; the wide Climate Ecobee card, scaleX > 1 and scaleY ~0.3, lands stretched in x and squashed in y), then the surface is removed and the tile appears with its true corners — the snap on every exit. A defined shape (Sharp 8dp, all corners equal and small) minimizes the absolute error, which is exactly why a defined shape reads as "locked". This is the same class of bug round 17 fixed on the folded path (`morphSurfaceShape`/`morphSurfaceRadii` divide the lerped radii by the layer scale); the unfolded popup path never got it — `morphPopupShape` took no scale and its single call site passed none. FIX: new pure `morphPopupRadii(progress, landingShape, scaleX, scaleY, density)` and a scale-aware `morphPopupShape(progress, landingShape, scaleX, scaleY)` overload (MorphOverlay.kt), mirroring morphSurfaceRadii; the sourced CenteredPopup branch now passes `morph.scaleX`/`morph.scaleY` (hoisted into locals, since the clip line sits outside the source branch), so the visible corners follow the 28dp → tile ramp exactly on both axes. The two-argument overload is unchanged (sourceless popups keep the scale-1 clip; the existing MorphMathTest assertions on it still hold). Polygon/null landings keep the popup's soft 28dp, divided by the scale like every other corner. Unit-tested (Round23LogicTest): scale-1 matches the corner lerp; uniform 0.5 scale doubles the local radii so the visible radii land exactly; the wide-tile split (1.4 / 0.3) divides each axis out; polygon/null keep soft corners; overshoot clamps; non-positive scales guard to 1.

Not in this round (kept, no action): the settings epic queue (Devices & services → Automations → People → System); the garage-door confirmation (decided against, permanently); the front-door wave test (his device action, still open); the still-owed round 22 device checks the video did not show (folded popup top buffer, pill-bar return on short pages, rail capsule, unavailable formatting). WebRTC stays gated off (`webrtcEnabled = false`).

## Feedback round 22 (2026-11-05, seven feedback items from the two narrated videos on #132)

PASSES from these videos (do not regress): room chips work now ("Those are working now" — Bedroom/Living Room switch); the thermostat popout is "pretty well refined" and is the reference behavior for everything else.

1. **Only the thermostat opens as the morphing popup; every other tile opens as a full-screen page.** DIAGNOSED FIRST: `detailAnim` is never constructed in #132. In #131, `HavenApp.kt` lines 969–971 synchronously constructed `detailAnim = DetailAnim(detailScreen, ...)` whenever the backstack top was a detail screen; round 21 deleted that block alongside the sheetAnim construction (verified via the #131→#132 commit diff: `HavenApp.kt` +257/−395, the construction is in the deleted hunks), while the detail overlay block (`if (detailAnim != null)`) and the `detailScreen`/`detailEntityId` helpers survived — so the overlay is dead code and every detail screen falls through to the base Scaffold as a full-screen sliding page. FIX: the #131 construction is restored verbatim, immediately before the `LaunchedEffect(detailScreen...)` that animates the detail progress — `Screen.EntityDetail`, `Screen.VacuumPanel`, `Screen.MediaPanel`, `Screen.SecurityPanel`, `Screen.GreenhousePanel`, `Screen.UpdatesPanel`, and `Screen.HomeStatusPanel` all construct their `DetailAnim` (source rect/color/shape captured at open) and render strictly inside `CenteredPopup` (unfolded) / `PanelOverlay` (folded) with the thermostat's spring morph, container transform, and finger-tracked dismiss. The `base` substitution (detail screens substitute their backstack predecessor) already keeps the Scaffold from transitioning underneath; the thermostat popout (`popoutAnim`) is untouched. Open note: the exact Scaffold-side mechanism that renders the detail page in #132 (the `base` substitution says it should show the predecessor) was not established by static analysis — the overlay sits above the Scaffold, so the popup wins visually either way; the device check is the arbiter.
2. **Folded popups: header badge/title overlaps the status bar (5179, Garage Door, Lamp).** DIAGNOSED FIRST: the collision is the item-1 fall-through, not a missing inset — `DetailPageScreen`'s comment states its sizing/insets are dialog-owned ("no fillMaxSize, no status bars padding — the dialog panel owns both"), so a detail page rendered in the Scaffold has no status-bar inset at all, while the Ecobee popout (rendered in its overlay) gets `PanelOverlay`'s `statusBarsPadding()` plus its own 16dp top padding. `PanelOverlay` already applies `statusBarsPadding()` to its scroll container (verified, unchanged). FIX: item 1's overlay routing restores the inset for all detail pages; plus the four Round 3 panels whose root Columns were horizontal-only (`MediaPanelScreen`, `SecurityPanelScreen`, `GreenhousePanelScreen`, `UpdatesPanelScreen`, null branches included) now carry `padding(start = 16.dp, top = 16.dp, end = 16.dp)`, matching the Ecobee popout, the vacuum panel, and every `DetailPageScreen` branch — so every folded popup header clears the status bar with the exact same 16dp buffer. Ecobee's buffer is unchanged (its 16dp stays content-owned; moving it to the container would double it to 32dp).
3. **Source tile still visible behind the popup + white/cream flash on close.** DIAGNOSED FIRST: the hide wiring exists end to end on paper — `HomeRoomCard` applies `.hideWhenMorphSource(entity.entityId)` (Screens.kt), the root provides `LocalHiddenCardId` from `topMorphSourceId(panelDetailAnim?.entityId, detailAnim?.entityId, popoutAnim?.id)` and `LocalMorphCardAlpha` from `morphCardAlpha(rootMorphProgress())` (HavenApp.kt), the popout id IS the entity id (`openClimatePopout(entity.entityId)`), and the pure math is correct (`morphCardAlpha(1f) = 0`, `smoothstep` verified) — yet the frame at 14s with the popout fully at rest shows the Ecobee tile fully visible. The remaining break is the delivery: the alpha was read only inside a `graphicsLayer {}` block within a `composed {}` modifier, through two layers of lambda indirection (CompositionLocal lambda → `rootMorphProgress` lambda → Animatable), and that read never re-evaluated on device — while `MorphSurface`, which reads its progress lambda directly as a composable parameter, animates correctly. FIX (at the mechanism): `hideWhenMorphSource` and `hideBodyWhenMorphSource` now read the progress during composition when (and only when) the card IS the hidden source — the card recomposes per animation frame while hidden and the alpha follows `morphCardAlpha` exactly (gone by progress 0.4, back over the last 40% of exit, fully visible at progress 0 before any state clears); non-source cards never read the progress, so they pay no per-frame recomposition cost. The hidden-source id now also falls back to `vm.cameraDialogId` (new 4th `topMorphSourceId` parameter, default null — existing 3-arg call sites and tests unaffected; priority stays panel-detail > detail > popout > camera). FLASH: `CenteredPopup`'s Surface color is now `Color.Transparent` — its background comes only from the color-lerp rect, which cross-fades from the tile's exact container color at p = 0 to the popup surface color at p = 1 — and its close alpha stays 1 (the old close fade to 0 by p = 0.25 vanished the surface mid-flight); `MorphSurface` likewise no longer fades its container to transparent on close (container alpha 1, the `fromBackground` lerp carries the surface into the tile's exact color). Open keeps its fade-in over the first 15% on both surfaces.
4. **Tapping Lamp opens Garage Door.** DIAGNOSED FIRST: the non-lazy grids never keyed their cells — `FavoritesSection` (HomeDashboard.kt) and `RoomSectionGrid` (Screens.kt) render cards in plain `for`/`while` row loops with unkeyed `Box` wrappers, so Compose reuses card slots by position when the member list recomposes, and a slot can keep the previous entity's tap closure. `TabTileGrid` (LazyVerticalGrid) was already `item(key = id)` — the control case proving keying is the difference. FIX: every cell in `FavoritesSection` (wide-media, WIDE, and chunked rows), `RoomSectionGrid` (same three loops), and the `HomeStatusPanelScreen` rows is now wrapped in `key(member.entityId)` / `key(entity.entityId)`; inside `HomeRoomCard` and `HomeRoomMediaCard`, all `remember` blocks (`interactionSource`, `badgeInteraction`, `confirmUnlock`, `zoneInteraction`, `textZoneInteraction`) are now `remember(entityId) { ... }`, so per-card state resets with the entity.
5. **Folded bottom pill bar gets stuck hidden.** DIAGNOSED FIRST: `ScrollVisibilityState.nestedScrollConnection` implemented only `onPreScroll`, and `scrollVisibility.reset()` was called only in `goTop`. A downward drag on a page already at the top — or with too few items to scroll (Climate tab) — is never offered as pre-scroll delta: the child consumes nothing and the leftover arrives in `onPostScroll`, which did not exist, so no event ever restored the bar (his frame at ~50s shows Home with no pill bar at all). FIX: `onPostScroll` now applies the same threshold rule (`scrollVisibilityAfter`) to the unconsumed leftover, so a downward drag at scroll top immediately shows the bar; a passive, never-consuming pointer observer (`showPillOnSwipeDown`, attached to the scaffold content Box) watches raw finger travel as a fallback for pages with no scrollable at all; and `scrollVisibility.reset()` is now also called whenever any popup/panel opens or closes (`LaunchedEffect(anyModalOpen)` over detail/panel-detail/popout/camera-dialog) and whenever the room filter changes (`LaunchedEffect(vm.roomFilter)`). Tab switches already reset via `goTop`.
6. **Unfolded rail green indicator disappears.** DIAGNOSED FIRST: `lastRailTabScreen` started as `null`, and the rail's selected index passed a null straight through whenever `selectedNavIndex(navActiveScreen(current), barItems)` returned null (popup, detail panel, or sub-screen open) — or fell back to a nullable `lastRailTabScreen` that had never been set. His frame at 38s shows no capsule on any icon. FIX: `lastRailTabScreen` is initialized to `Screen.Today` and is now non-null (`Screen`, never `Screen?`), and the index resolves through the pure `railSelectedIndexWithFallback(onTabIndex, lastRailTabScreen, barItems)` (unit tested: on-tab index wins; off-tab falls back to the last on-tab screen resolved fresh against the current bar list; a last tab that left the bar falls back to index 0, Home) — so the green capsule is always visible. The folded pill is untouched (it hides on scroll/modal by design).
7. **"unavailable °F" on the 5179 card.** DIAGNOSED FIRST: `HaEntity.stateText()` (EntityControls.kt) appended `unit_of_measurement` for sensors unconditionally (`"$state $unit"`), so an unavailable temperature sensor read "unavailable °F". FIX: `stateText()` returns "Unavailable" / "Unknown" (case-insensitive) before any domain/unit logic, for every domain; `dialogHeaderState()` delegates to `stateText()` for non-weather entities, so headers get the same clean string. Audit: every other unit site either uses `stateText()` (`SensorTile` value, thermostat sensor history) or is numeric-guarded (`toDoubleOrNull` filters in `powerSensors`/`energySensors`, `parseSensorTemp`, sparkline Min/Current/Max rows) — no other change needed. `tileStatus()` does not exist in #132 (deleted in round 21); the spec's EntityTiles.kt guess is stale.

Device checks owed on this round: (1) all tile taps open the morphing popup overlay (CenteredPopup unfolded / PanelOverlay folded) with the thermostat's spring morph, container transform, and finger-tracked dismiss — never a full Scaffold page — folded + unfolded, Home (Favorites, Rooms, Home Status) and user-tab grids; (2) folded popup headers (5179, Garage Door, Lamp, Media/Security/Greenhouse/Updates panels) clear the status bar with the Ecobee 16dp buffer; (3) source tile disappears while its popup is open and reappears with zero white/cream flash on close, folded + unfolded; (4) tapping Lamp opens Lamp, never Garage Door — including after favorites/room membership changes; (5) pill bar returns on a downward swipe even on short pages (Climate tab), and resets on popup/tab/room changes; (6) rail green capsule always visible — including while a popup is open and after it closes; (7) unavailable/unknown states render "Unavailable"/"Unknown" with no unit suffix on cards, headers, and detail pages.

## Feedback round 21 (2026-11-05, eight feedback items from the three narrated videos on #131)

1. **Room chips stopped switching (regression on #131).** DIAGNOSED FIRST: `RoomFilterChips` in `ui/screens/Screens.kt` had `.clickable { chip.onClick }` — a function-reference expression, never invoked — so `onClick()` was never called and taps did nothing (a Haley notification banner covered the chips ~44s in the video and may have eaten some taps, but taps after it cleared still didn't switch, so the banner is not the cause). FIX: `.clickable { chip.onClick() }`, with the chip list extracted into the pure `buildHomeChips` (unit tested: Bedroom/Living/No-room/My Home taps dispatch their filters, hidden/nav-tab rooms produce no chip, a tab chip navigates).
2. **Thermostat dial contrast (CORRECTED twice by Garrett — the rail was never the issue).** What he meant in video 1 was the THERMOSTAT's radial dial lacking contrast against the scalloped expressive shape background (the dial arc/track blends into the dark scallop). DIAGNOSED FIRST: the track was `scheme.outline`, claimed ~3.5:1 by the round 8 comment — measured ~1.3:1 in BOTH themes (dark outline #3F3F3A on dark disc #2B2B2B; light outline #C6C4B4 on light disc #DDE5D0), so the round 8 claim was wrong. FIX: the pure `dialTrackColor(disc, onSurface) = lerp(disc, onSurface, 0.55f)` (unit tested: 4.9:1 dark / 3.5:1 light against the plain disc, holds ≥3:1 on the 20% mode-tinted popout disc), threaded from `numberBackdrop` in `ThermostatDialBlock` through a new `trackBackdrop` param on `ThermostatDial`; both `scheme.outline` track draws replaced, the off-mode dashed track and the heat/cool gradient arcs untouched. The side rail gets only the minor polish he agreed to: `VerticalSidePill`'s Surface is now `if (isDark) Color(0xFF263026) else PillBarContainer` (#161D17 measured 1.1:1 vs the black dark background, bump to ~1.35:1); the folded pill bar is untouched.
3. **Front Door isn't actually live.** DIAGNOSED FIRST: when `camera.front_door_69a1` is down, the panel's Live tab rendered `CameraMjpegBody` on `camera.front_door_live_view` with `recording = false` and NO onFirstFrame — and the codebase's established finding (2026-10-30 verdict, confirmed; CameraStream.kt header) is that HA's core Ring integration builds that MJPEG proxy from the most recent motion recording — a recording by construction. HLS is refused deterministically by HA; WebRTC answers empty deterministically and stays GATED OFF. The only remaining known live transport is ring-mqtt RTSP `rtsp://10.0.0.11:8554/5c475ecd13a7_live`, so it is wired as the fallback live attempt. FIX: `media3-exoplayer-rtsp` 1.6.1 added (version catalog + `app/build.gradle.kts` — the ONLY change to that file is the one dependency line; the signing config is untouched); `HaClient.createRtspPlayer` (`RtspMediaSource.Factory().setForceUseRtpTcp(true)`, unauthenticated LAN, no Bearer); pure `frontDoorRtspUrl(instanceUrl)` (LAN host only, null for cloud/null/blank, IPv6 bracketed); new `CameraRtspPlayerBody` (bare SurfaceView letterboxed to video aspect, same enter curve, last cached live_view frame dimmed behind loading) with an 8s first-frame timeout, so a dead RTSP endpoint degrades to the recording instead of hanging on "Loading camera…". The fallback branch now tries RTSP first — first frame marks `frontDoorRtspProven` and the badge reads LIVE; error/timeout clears the proof and falls through to the LABELED MJPEG recording (`recording = true`, so the body reads "Loading recording…" / "Recording unavailable"); the status line gains "Live", "Connecting to live view…", and "Recording unavailable". The quadrant badge rule is updated to match (LIVE when 69a1 is healthy OR the fallback is proven streaming in-session). Liveness is unprovable from the VM; the wave test remains the arbiter. Known limitation: the quadrant proof flag is sticky in-session — set on first frame, cleared only on an RTSP error — so a stale proof could linger if RTSP dies while the panel is closed; the next fallback panel open re-attempts RTSP and clears it on error.
4. **Dark-mode garage panel.** DIAGNOSED FIRST, two mechanisms: (a) `PressablePillButton` (EntityControls.kt) accepts a `contentColor` param but NEVER applies it — the Row never sets LocalContentColor, so bare `Text("Open")` inherits the ambient content color, whose Compose default is BLACK (no Surface provides it on the panel path). Pixel-verified from the video frame: Open pill container ~(25,46,76) = #192E4C, text pure black (0,0,0). FIX: `CompositionLocalProvider(LocalContentColor provides contentColor)` around the Row — one fix for every caller (garage, vacuum, steppers, media transports); caller-side disabled-pill alpha is unaffected. (b) The garage tile has a "cooling" blue preset, but the door is CLOSED so the preset is INACTIVE on the tile (tile shows the matrix INACTIVE container #1C201B dark sage-green), while `detailPageAccent` was DELIBERATELY UNGATED per round 5 (the popup followed the preset regardless). ROUND 21 REVERSES that round 5 rule: `detailPageAccent` is now gated by `presetAppliesForState` — it returns null when the preset doesn't apply for the current state, so the panel falls back to matrix/scheme colors the tile shows (garage pills → secondaryContainer #2A352A dark sage, door → primary #AED6A0 light green in dark mode — both green family, matching the tile). `tileBadgePair` was already gated (unchanged). NOTE: this intentionally reverses the round 5 behavior for the docked vacuum too (its sage preset no longer colors its popup while docked) — confirm that reversal. The existing `CardCustomizationTest` ungated test is updated to the gated rule.
5. **One popup, not two (SUPERSEDES round 17's long-press settings sheet).** The long-press menu is gone entirely: `EntityPopupSheetContent` + the whole sheetAnim machinery (`SheetAnim`, `sheetProgress`, `sheetDrag`, `sheetPanelBounds`, `sheetExiting`, `dismissSheet`/`dismissSheetInstant`, the `popupEntityId` capture effect and render block) + `vm.openPopup`/`popupEntityId`/`closePopup` are deleted; every former sheet call site now navigates to the full page (`navigate(Screen.EntityDetail(id))`): Search, Lovelace boards, DeviceSheet (gains a `navigate` param), FavoritesSheet (gains a `navigate` param; `HomeEditorScreen` gains one too, passed from its call site). The single-press popup gains the shared `EntityPopupMenuRows` (DetailPages.kt) at the very bottom for EVERY tile — Details / Badge shape / Card shape / Card settings, verbatim the sheet's rows — appended to the generic detail page (both layouts), the thermostat popout, the weather page, the camera column, and the vacuum panel. The Details row is OMITTED where the current surface IS the full page (generic detail, thermostat popout, weather page — pushing the same EntityDetail screen is not a safe no-op: the backstack would grow and back would pop to the same page) and INCLUDED where a deeper surface exists (vacuum panel → generic detail page; cameras keep their Open live view button as the Details action). The popup-header pencil goes away with it (`DetailPopupHeader` loses its `onEdit` param, `CardSettingsPencil` is deleted, `detailHeaderCustom` simplifies Triple → Pair). The clips timeline, formerly a camera-tile long-press, moves to an "Event history" row in the camera column (dismiss-then-navigate, the sheet's Details handoff pattern); the Front Door keeps its in-panel Event History tab. The weather card loses its long-press (it is not an Arrange-able grid card; its settings live in its popup). `CardSettingsSheet` itself stays — it is reached from the Card settings row, not long-press.
6. **One tile style.** Tab-added entities (UserTabScreen / custom-tab pages) rendered the old `EntityTile` (64dp badge, wide inner box, status dot). `TabTileGrid` now renders `HomeRoomCard` / `HomeRoomMediaCard` — the one current tile style used on Home/room pages — with the same wide-media rule as room grids (a playing or favorited media_player takes the full-line media card). `EntityTile`, `ClimateTileBody`, `tileStepNext`, `CameraTileVisual`, `tileStatus`, and `openPage` are deleted (verified used only within EntityTiles.kt); the file is now TabTileGrid + cardEntityIds + MissingEntityTile. `lightBrightnessPercent` (the 0–255 → % conversion) is restored as a pure function in EntityTiles.kt — the old tile's body was deleted with it, but the conversion is still the tile status text's source and its unit test still covers it; the two inline copies in Screens.kt now call it.
7. **Arrange moves to long-press (SUPERSEDES the round 20 Arrange button entry point).** The Arrange/Done button is gone from Home, tab pages, and room views. Pressing and holding a card enters Arrange/resize mode instead (`arrangeMode` + `onArrangeToggle` params on `HomeRoomCard` and `HomeRoomMediaCard`, before `onOpenPanel`/`navigate`); all four long-press sites on a card (surface, binary zone, cover zone, badge shapeAction, media card, text zone) now toggle Arrange, taps are swallowed while in Arrange mode (no panel, no actuation), and `BackHandler(enabled = arrangeMode)` exits (per-section/per-page, so sections don't swallow each other's back presses). The 32dp drag handles, vector snap, settle spring, and per-placement persistence are unchanged from round 20 — only the entry point changes. `HomeRoomMediaCard`'s fixed `height(136.dp)` is now `heightIn(min = 136.dp)` so a TALL-resized media card actually grows (it used to leave a 136dp card in a 272dp box). The weather card and Home Status panel cards take no Arrange toggle. `TabTileGrid`'s `navigate` is now non-null (its only caller passes it; the sheet fallback is gone).
8. **The tile BECOMES the popup (SUPERSEDES the round 15–20 morph behavior where the source tile stayed rendered under the scrim).** Tapping any tile grows that tile into the popup, leaving a blank space in the grid where the tile was; title and icon travel with it. FIX: the overlay root now provides the topmost morph's source entity id as `LocalHiddenCardId` (panel detail > detail > popout; the camera dialog is deliberately absent, as before) and the source card's alpha as `LocalMorphCardAlpha` — now a `() -> Float` lambda reading the topmost morph's progress through the pure `morphCardAlpha` (gone by progress 0.4, back over the last 40% of exit, clamped), invoked only inside graphicsLayer so the progress read costs no recomposition. A lambda, not a Float, because MorphSurface shadows `LocalMorphProgress` inside panel subtrees — the unshadowed CardAlpha lambda is what lets panel-internal cards fade on the ROOT progress for a stacked panel detail instead of the underlying panel's rest progress. The pure `topMorphSourceId` / `topMorphProgress` priority helpers are unit tested (panel detail > detail > popout > rest 1f). The hide is alpha-only (layout unchanged), so the grid never reflows around the blank slot, and the card is fully visible again at progress 0, before any dismiss clears state — no blank frame follows the landing. The round 20 open-time shape capture and the finger-tracked drag dismiss are unchanged; during a finger drag the card stays hidden (the animatable holds 1f while the surface moves under the finger) and fades in over the last 40% of the settle. The round 6 "no source card ever hides" rule and the round 5 full-alpha rule are both superseded by this item (comments updated at the root providers and in MorphOverlay's design KDoc).

Tests: 465 total, 0 failures (baseline 449 + 16 new Round21LogicTest: room-chip tap mapping, dial-track contrast, RTSP URL/badge/quadrant rules, gated detail accent, morphCardAlpha curve + topmost-source/progress priority). No signing changes except the one RTSP dependency line in app/build.gradle.kts (signing config untouched; build.yml untouched); webrtcEnabled untouched.

1. **Tab-page card shape stability.** DIAGNOSED FIRST: the morph landing shape was read live from the shared entity-keyed shape map at close time, while bounds and container color were already captured at open (round 16 pattern). The map is shared across every rendering of the same entity, and different pages give the same entity different grid-index corner variants — so the landing shape could disagree with the tile's own resting shape, and when the morph surface was removed at close end the tile snapped to its own shape, visibly changing shape on every exit. The map entry could also be absent, falling back to the uniform prefs default. FIX: every morph (sheet, detail, panel-detail, popout) now captures the tile's shape at open alongside bounds/color (`sourceShape` on the anim classes), and every landing-shape call site prefers the captured shape (`anim.sourceShape ?: cardShapes[...] ?: prefs`, via the new pure `resolveLandingShape`, unit tested: captured wins even when the map moves mid-popup). The tile's own resting-shape inputs (prefs/domain override + grid index) were verified stable across a popup cycle, so the live read was the moving part. Camera quadrants keep their live read (their corners are not tab cards).
2. **Consistent chart-line colors for like entities.** The 24h strips drew `accentFor(entityId)` — a per-entity hash color — so two temperature sensors drew different lines (the 5179 sensor teal, the Temperature sensor red). A new pure `chartLineColor(kind, isDark)` is now the ONE chart-line source, keyed by measurement, never by entity, and all three Sparkline sites route through it (detail history strip, thermostat sensor history, sensor tile — badge/value colors in the sensor tile are untouched, only the line changes). Temperature uses the exact badge-bypass red #FFEF5350 and humidity the exact badge sky #FF29B6F6 on both themes; power, battery, illuminance, and pressure use their matrix rows' badge tones (pale on dark theme, deep ink on light theme, so the line reads on both); unclassified sensors share the MEDIA row tones, so any two like sensors still match. A new `PRESSURE` sensor kind (device_class pressure, neutral Info icon and null badge/tint — badge behavior unchanged) covers pressure sensors. The weather hourly forecast keeps its condition colors by design (weather, not a measurement line). Badges themselves are untouched.
3. **Entity-search keyboard fix (Add-card flow).** The tab editor's entity picker rendered suggestions in a `DropdownMenu` Popup, which stole focus on every open — and each keystroke reopened it with new matches, closing the keyboard on every letter. The picker now renders its (still 8-capped) suggestion list INLINE in the card's own Column: filtering never moves focus, the field keeps focus, and the keyboard stays up across keystrokes. Rows are keyed by entity id so a row never swaps slots with a sibling on re-filter. The room add UI already used an inline list and never had this bug; it is untouched.
4. **Multi-select entities in the Add-card flow.** In the same picker, every suggestion row now carries a checkbox; tapping a row toggles that entity in a selection set held by the editor (keyed by tab, outside the filtered list, via the pure `toggleEntitySelection`, unit tested) so selected entities stay selected while the query changes. An "Add N cards" button appears when the selection is non-empty and adds one card per selected entity through the SAME YAML builder as the single "Add card" path, so a multi-added card is byte-identical to the same card added singly; the selection clears after adding. The single-add path and its resulting card content are unchanged. Row taps now toggle instead of filling the field (the field still accepts a typed id for single adds) — the toggle is the multi-select flow the round asked for.
5. **Resizable cards via drag handle (user's explicit choice: drag-handle, not a size picker).** A 32dp grip at the card's bottom-end corner, visible ONLY in the page's Arrange mode (a new Arrange/Done text button: next to Edit on tab pages, next to the Favorites pencil on Home, per-section on room views) — never in normal browsing, so it cannot hijack taps, morphs, or popup drags. The grip owns its gesture on the dial-thumb discipline (every drag change consumed, so the parent scroll never also moves; grip taps are swallowed so they never open the panel). Dragging shows a live preview label (Wide/Tall/Normal); on release the vector snaps (right = wide, left = normal, down = tall, up = normal, 24px threshold, pure `snapCardSize`, unit tested) and the size settles on the established settle spring. Sizes persist per placement in a new `haven_card_sizes` SharedPreferences store (JSON key → "wide"/"tall", absent = normal; keys `home:<entity>`, `room:<area>:<entity>`, `tab:<tab>:<entity>`) via the ViewModel, so they survive recompositions and restarts; exiting Arrange mode keeps sizes. WIDE = full-row span (Lazy grid `GridItemSpan(maxLineSpan)`; manual grids give the card its own row); TALL = 272dp minimum (twice the 136dp floor). DOCUMENTED LIMIT: a true multi-row tall span is NOT expressible — LazyVerticalGrid has no row spanning and manual grids size rows by their tallest card — so TALL is a minimum height and a tall card's row-mate stays top-aligned at 136dp. A resized card keeps its corner-variant/shape rules and morph behavior (grid indices still count flat card order, never row layout, so a wide card never shifts its neighbors' variants; the morph still lands on the tile's shape via item 1's capture). Media cards (already full-width by rule) and missing-entity placeholders take no handle.
6. **Animation refinement pass.** Audited every morph, popup open/close, tab switch, and sub-screen slide path: the open/close fade ordering is principled (content fades in over [0.15, 0.5] on open and out early on close, container fades over the final 25% on close, scrim alpha tracks clamped progress); tiles are never hidden during a morph (LocalHiddenCardId null, LocalMorphCardAlpha 1f at the tile layer); the shared TravelingBadge component stays unwired (zero call sites, verified); tab-switch and sub-screen slide code is untouched by this round (round 19's back-slide direction and bezel slides stand). The artifact found is the item 1 exit snap — the thermostat popup's exit doubling against the Climate page was the landing-shape disagreement, fixed by the open-time capture, not by retuning any curve. No motion behavior is redesigned; no new snap, blank flash, or doubling frame is introduced on the audited paths.

## Feedback round 19 (2026-11-05, six feedback items from the 2026-11-04 folded + unfolded test runs)

1. **Unfolded left-edge clipping.** The outer Box wrapping the top-level `AnimatedContent` no longer reserves the 84dp rail zone: pages slide all the way to the physical left bezel underneath the rail, and the 84dp inset now lives INSIDE the transition container as content padding on wide layouts, so resting geometry is unchanged. The transition uses `SizeTransform(clip = false)` — this SUPERSEDES round 17 item 8's clip = true for exactly this symptom (the fade removal from that item, which was the actual ghosting fix, is retained). Full-bleed holds because the outer Box still paints the scheme background full-width and every screen-root background is the same scheme color, so the inset shift changes no pixel at rest.
2. **Back-navigation slide direction.** A new pure `nonTabSlideDirection` resolves push (+1) vs pop (−1), and `tabSlideOffsets` already carried the backward pair; `navDirection` is tracked in HavenApp (navigate/goTop = forward, backstack pop = backward) and the transition's else-branch now slides non-tab pages by direction: push enters from the right / exits left, pop exits to the right / parent re-enters from the left. Tab switches keep their rank-based slides untouched.
3. **Room-filter persistence + scroll jump.** `roomFilter` is hoisted from TodayScreen into the ViewModel, so selecting Bedroom or Living Room survives popup opens, closes, and recompositions — returning from a detail lands back in the room view, not My Home — and `goTop` maps Today → All and RoomTab(area) → Area(area). `AnimatedContent(targetState = base)` is now ALWAYS mounted (the old `detailAnim != null` branch that swapped in `content(base)` directly is deleted), so TodayScreen never disposes or flashes underneath a morphing popup. Folded nav clearance is now unconditional on folded layouts (new pure `shouldApplyFoldedNavClearance`: folded → navBottom, wide → 0), matching the unfolded rule, so opening a detail panel at the bottom of the Home list never changes contentPadding or jumps the scroll offset. DEVICE-CHECK NOTE: a base change under an opening centered popup (e.g. HomeStatusPanel → detail) now slides under the popup on unfolded where the old direct branch rendered it instantly — the always-mounted behavior is as specified, and the visual result is a device check.
4. **Dynamic LIVE vs Recording badge on the Front Door quadrant.** A new pure `frontDoorQuadrantShowsLive` gates the green LIVE badge on the resolver returning the live source (`camera.front_door_69a1`) with no stream error. Built LITERALLY per spec: if 69a1 is down but the fallback feed plays, the badge stays Recording, since the live source is what's down. The quadrant renders snapshots, not a stream, so it has no quadrant-level stream error — the call site passes false and the panel keeps its own stream-error UI. Non-front-door quadrants keep the old rule (LIVE unless a Ring camera).
5. **Unified Rooms & tabs management (SUPERSEDES the separate screens).** Settings now has ONE "Rooms & tabs" entry replacing both "Rooms & areas" and "Manage tabs" (both old screen functions are deleted; `Screen.RoomManager`/`Screen.TabManager` become `Screen.RoomsTabs` + `Screen.RoomTab`). The unified screen keeps every behavior of both parents: room create/rename/delete/assign writing through to real Home Assistant, the searchable add-entity list, the No-room card, navigation switches, pill style, reset, tab editors, and add-a-tab. Every room and every tab carries a placement selector — Room chip (Home chip row), Nav tab (bar/rail), or Hidden (neither surface, still manageable here) — backed by a single `Placement` enum, `placementFromKey`, and `tabPlacementFor` (pinned wins over chip; a tab with neither flag is Hidden, matching its historical browse-list-only visibility, so existing unpinned tabs do not flood the chip row). The 2-cap counts pinned tabs plus nav-tab rooms TOGETHER (new pure `canPinBarExtra`, ViewModel `canPinToBar` gate; the selector disables the Nav-tab option at the cap with the reason line, and a group already on the bar is never disabled out from under itself); bar extras order is user tabs first, then rooms. Home and Settings stay locked by construction. The pinned+hidden tab combination no longer has a selector value (choosing any placement clears the other surface), and No room is not placeable (always a chip). Room tabs navigate to the room view through the chip-row path; tab chips navigate to the tab screen. Existing tests referencing the old screens now reference `RoomsTabs` (the Settings-highlight and tap-back mappings are preserved through the new screen).
6. **Real-time Home Assistant event notifications.** `POST_NOTIFICATIONS` is declared and requested once at launch on Android 13+ (a denial is never re-asked; the Settings permission row is the re-grant path via system settings). A new `HavenNotificationManager` owns three high-priority channels (garage, motion, lights) and is hooked into HaClient's `state_changed` handler — it fires ONLY when the event is applied to state, so removals and optimistic-guard drops never notify. Trigger rules are pure and unit tested (`NotificationRules`): garage/door covers to open/opening and door binary_sensors off→on; motion binary_sensors off→on only; lights on→off NOT from a local Haven tap (every `actuate()` stamps a 10s local-actuation window; an automation parent id always notifies even inside the window). Each notification's tap carries `EXTRA_CONTROL_ENTITY_ID`, deep-linking to that camera, garage door, or light through the existing router. Settings gains a Notifications section with three switches — Garage & door alerts, Motion alerts, Automated light alerts — all ON by default once permission is granted, plus the permission status row.

## Feedback round 18 (2026-11-04, seven feedback items from the 2026-11-04 folded + unfolded test runs)

1. **Room filtering on Home is a view switch (DECIDED organization rule).** "My Home" now shows ONLY the overview sections (Greeting, Cameras, Home Status, Favorites, and enabled overview blocks) — `visibleRooms` is `emptyList()` under it, so Bedroom/Living Room sections never render there. A room view (Area or Unassigned) shows ONLY the greeting, the room-filter chips, and that room's grid — Cameras, Home Status, Favorites, Weather, Who's home, and all Round 3 blocks hide. This SUPERSEDES the visual-fidelity §4 rule that the filter "narrows room sections only" and that Home Status/Favorites "stay GLOBAL under any filter". The rule is extracted pure (`visibleRoomsFor`, `isRoomViewFilter`, unit tested) and applied at every render site on both postures (folded list loop, unfolded left pane cameras/status, unfolded right pane favorites/weather/home/round3). Rooms stay as the top chip row; no category filter row and no Automations destination tab are built here (later work, tied to the settings epic).
2. **Detail pages use the full popup width + no duplicate hero + centered unfolded popups.** The two-column Row split in `DetailPageContent` now applies ONLY when the state parses as a number (sensors: control left, history right — new pure `useDetailTwoColumnLayout`, unit tested). Non-numeric entities (light, cover, media_player, lock, switch, fan, vacuum) use the full popup width instead of being crushed into the left 50% column. The hero panel is REMOVED for all domains (the hero lambda, its icon, and the no-hero domain gate are gone): `DetailPopupHeader` already displays icon, title, and state, so the Garage Door and Garrett's TV duplicate icon/state is fixed by removal, not by gating. Unfolded popups are ALWAYS centered in the content area at max 460dp wide (was: side-anchored by tile third, 400dp cap) — the `popupSideAnchor` call is gone from `CenteredPopup` (the pure helper itself stays unit tested); the 84dp rail padding is already the content area's origin, so centering within it is the rail-aware center.
3. **Favorites edit pencil never leaves composition (SUPERSEDES round 17, item 6).** The `if (!LocalModalActive.current)` gate around the 40dp edit pencil Box in `FavoritesSection` is removed: the button stays composed whether or not a modal is open. Round 17's comment claimed hiding causes no reflow because the title carries weight(1f) — but the grid DOES shift, because the Row height changes from the 40dp pencil to the text height when hidden. Always composed = never shifts. The scrim still blocks its taps while a modal is open, as before.
4. **Giant flashing tile icon on folded morphs — traveler deleted.** The round-17 traveling badge is GONE entirely: `TravelingBadgeContent` and its three `PanelOverlay` call-site args are deleted, `PanelOverlay`/`MorphSurface` lose their traveler params and the in-surface traveler rendering block, and `LocalHiddenHeroId` now provides null on BOTH postures (matching `CenteredPopup`). Header badges render IN PLACE inside their header row and fade with the content curves instead (new `morphHeaderBadgeAlpha`: `morphEnterContentAlpha` on open, `morphCloseContentAlpha` on close, via `LocalMorphProgress` + `LocalMorphClosing`) — no 3x scaled badge flash on folded open/close, no 2% handoff. The old `hideWhileTraveling` modifier is deleted (its three header call sites now wear the new fade); the pure traveler math (`badgeRectInRow`, `morphSourceBadge`, `heroHeaderAlpha`, `travelerBlend`, `travelerLocalRect`) is retained unit-tested even though no production caller remains. The shared `TravelingBadge` component stays available but unwired; hero-target reporting and `LocalSuppressTraveler` providers are retained dormant.
5. **Finger-tracked swipe-to-dismiss (EXPLICIT REVERSAL of round 11, item 1).** `dragProgressFor` now returns the drag state's `dragProgress` while a drag is in flight and the animatable otherwise, so dragging down on any popup scrubs its morph progress in real time with the finger. `popupDragDetector` is attached to the non-scrollable header rows of `CenteredPopup` sheets (`EntityPopupSheetContent` header), detail pages (`DetailPopupHeader`), and the thermostat popout header — via new `LocalPopupDragHandle`/`LocalPopupDragRangePx` CompositionLocals provided by `CenteredPopup` and `PanelOverlay` — so dragging down anywhere outside the dial ring thumbs pulls the popup down into its tile. Dial thumbs, toggles, and sliders keep owning their own drags (the nested-scroll connection is untouched, scroll-at-top gating intact); the release rule (velocity first, otherwise 40dp) is unchanged. Two correctness fixes ride with the reversal: dismisses snap the animatable onto the released drag progress BEFORE clearing `isDragging` (otherwise the surface would jump back to full-open for one frame, since progress reads dragProgress while dragging and anim.value once cleared), and the restore branch snaps while `isDragging` is still true before animating back to open. This reversal is for Haven only and does not change Koda (which never finger-tracked).
6. **Sub-screen transition bleed + rail active taps + panel polish.** (a) Every screen root now carries an opaque `.background(MaterialTheme.colorScheme.background)` (HomeEditor, RoomManager — already had it — TabManager, UserTabs, Settings, Scenes, Energy, Areas, Search), and sub-screen slides run full travel both ways (`slideInHorizontally { it }` / `slideOutHorizontally { -it }`, was one-third) — sub-screen transitions can no longer overlap transparent text. (b) Rail/pill tap guards compare SCREENS, not indices (new pure `shouldNavigateOnTabTap`, unit tested; `currentScreen` param added BEFORE `onSelect` in both `VerticalSidePill` and `PillNav`, round-5 rule): tapping the highlighted Settings icon while inside Manage home page or Rooms & areas now navigates back to Settings, where the old index guard swallowed the tap because the highlighted tab's index matched while the actual current screen differed. Drag-scrub commit guards stay index-based (they are drag commits, not tap guards). (c) `HomeStatusPanelScreen` and `VacuumPanelScreen` gain 16dp top padding; `VacuumPanelScreen` gets the standard `DetailPopupHeader` (vacuum badge + edit pencil via `detailHeaderCustom`, same as detail pages — the big state line below stays, since it carries battery + state in one line); the Lidar map Image moves from `ContentScale.Crop` to `ContentScale.Fit` so the whole map shows uncropped.
7. **State collection performance.** `CameraQuadrant` no longer collects the whole states map: it collects its own entity (`vm.entityFlow(cameraId)`), the front-door trio (69a1, live_view, snapshot), and its motion sensor (new cached `motionSensorFlow` on the ViewModel, mirroring `entityFlow`'s map + `distinctUntilChanged` + `WhileSubscribed(5000)` pattern, resolving via `motionSensorForCamera` over the states + entityDevices flows) — a state change on any other entity no longer recomposes a quadrant. The front-door behavior is byte-identical: the trio feeds the SAME shared resolver through a new pure `frontDoorResolverStates` helper (unit tested: 69a1 leads, live_view fallback, both-down null, absent = unavailable), and the snapshot thumbnail rule is unchanged. `reportCardBounds` and `reportHeroBounds` now write only when the new rect differs from the stored rect by MORE than 1f pixel on any edge (new pure `rectDiffersByMoreThan1px`, unit tested, boundary pinned: exactly 1f does not write); the first capture always lands (null stored = write), so morph source-rect capture is unchanged and sub-pixel layout jitter no longer schedules recompositions. Camera entity flows themselves are not unit-testable without a ViewModel harness; the resolver + threshold + filter + layout + tap-back rules above are the tested surface.

## Feedback round 17 (2026-11-04, animation, navigation, and UI bugs from the 2026-11-04 folded + unfolded test runs)

1. **Unfolded popup surface + source-card blanking.** The centered popup now renders on an ELEVATED surface (`surfaceContainerHigh`) with a 1dp `outlineVariant` border, so it never blends invisibly into the black dashboard (the old `background` color read as pure #000000 on unfolded). The settled color-lerp target moved with it, so the panel at rest is the elevated color under its border, not background. And the sourced popup now carries the folded morph's container-alpha curves (in over the first 15% on open, out over the final 25% on close) instead of rendering opaque from frame 0 — the old opaque-from-frame-0 surface sat on the tile as a blank tile-colored rect on open (the Ecobee empty blue rectangle) and a solid dark slab on close (the eufy black box over the right pane). The source card is never hidden or blanked: it stays fully rendered underneath the scrim, and the surface fades in as it grows. Sourceless popups keep their fade + scale; the morph shape, spring, and drag behavior are untouched.
2. **Folded traveler pinned to the header slot, tile-true colors, squircle landing.** The traveling badge no longer flies as a sibling overlay lerping badge-to-badge in window space (that path cut across the panel center on shrink while the surface's header slot hugged the shrinking rect's top-left). It now renders INSIDE the morphing surface's own transform (a second child of MorphSurface, outside the content-alpha Box), laid out at the target slot and mapped per frame onto a blended local rect: exactly on the source badge at frame 0 (no pop — the tile's own badge is hidden during flight, so a slot-only path would have shown a squashed badge at the tap moment), converged onto the surface's header slot by 30% of the flight, riding the slot for the rest in both directions, with the linear badge size throughout (badges never squash under the surface's non-uniform scale). On close the close-tail fade hides the return to the linear path below 30%. And the badge colors are now the tile's EXACT pair everywhere (new shared `tileBadgePair`: preset-active → preset pair, preset-inactive → INACTIVE pair, else matrix pair, else hash fallback) — the sheet/detail/weather/popout headers and the traveler all resolve through it, so eufy AE C10 keeps its muted-green docked pair through flight and at rest instead of flashing hash-yellow in headers while its tile read muted green (the old preset-only override returned null with no preset, and headers fell back to the hash accent). The folded shrink also lands on the squircle now: the morph clip's corner radii are divided by the surface's axis scales, so the VISIBLE corners interpolate 0 → landing exactly instead of double-shrinking (a 28dp tile from an 800dp panel showed ~12dp visible corners mid-shrink and never landed on the squircle). Polygon-tile early-return and the null-landing fallback are unchanged; the centered popup keeps its own shape.
3. **Tap routing — taps always open the control panel.** HomeRoomCard, EntityTile, and the media card now route single taps to the control panel for every domain (climate popout, VacuumPanel for vacuum.t2292, detail page otherwise) — the old binary-only rule sent vacuum/cover/media taps to the settings sheet while Lamp/Ecobee taps opened full panels. The settings sheet (`vm.openPopup`) is now strictly long-press or the header pencil button (Home Status panel cards keep their onOpenPanel override for both, since a sheet opened from inside a panel would paint underneath the opaque detail panel and read as a dead tap; Boards cards keep the openPopup fallback where there is no navigator, or their taps would do nothing). Long-press wiring on EntityTile is unchanged (camera → clips, climate → popout, light → panel).
4. **Garage door sheet + visual.** GarageDoorMenu loses its redundant "Garage door · Closed" line (it duplicated DetailPopupHeader directly above; the position line stays, since the header carries no position). GarageDoorVisual's frame is real now: the old trailing Box was an empty transparent stub, so at openFraction 1f the risen door left only the flat container background (the empty dark blue box). A 2dp outlineVariant border plus a top header track bar now draw over the sliding content at every open fraction, in the panel, sheet, and card alike. No confirmation dialog and no tap-target change, per the explicit 2026-11-04 decision.
5. **Unfolded rail highlighting.** The indicator offset now comes from the item INDEX (new pure `railIndicatorTop`: 12dp column padding + 8dp icon centering + index × 48dp rows), never from measured window rects — the old boundsInWindow measurements went stale across bar-list changes and the pill-enter translation, clearing the indicator or parking it on the wrong tab (Home displayed with the highlight off Home on the 2026-11-04 runs). This SUPERSEDES round 16's screen-keyed rect fix for the same symptom: there are no rects at all now, so nothing can go stale; the scrub-follow-finger behavior, Koda glide spec, and 56×32dp pill are unchanged. `selectedNavIndex` itself was already correct for Today (Today is structurally first in the bar list) and is pinned by test, not changed. Settings sub-screens now map to the Settings tab: Dashboards, Areas, Search, and UserTabs join HomeEditor, RoomManager, TabManager, and the builders/updates (round 15's Browse-destination carve-out is overridden for exactly these four by the round-17 brief); DashboardEdit, AreaDetail, and CameraClips keep the nothing-selected rule. The panel-detail rail-condition gap is a verified non-issue (the opaque panel covers the rail during panel-detail) and is left as is.
6. **Null subtitles, grammar, room labels.** The RoomManager picker omits the subtitle when the registry device name is null, blank, or the literal string "null" (new pure `cleanDeviceName`, plus a belt-and-suspenders HaClient guard treating a literal-"null" displayName as absent — HA carries the string "null" in registry name fields, the same class as the known device_id "null" case; the old picker rendered it verbatim). Both "N devices" headers use a new pure `deviceCountLabel` ("1 device" at 1, "N devices" otherwise). HomeEditor room rows gain a bold room title above each field, and the field label reads "Display name (Room)" so the row is labeled even when the field is empty and unfocused.
7. **No-room filter.** `isHomeRoomEntity` now excludes `sensor.home_assistant` AND `conversation.home_assistant` outright — the actual "Home Assistant / Unknown" tile is conversation.home_assistant (state unknown), not sensor.home_assistant as the brief assumed; both ids are excluded — plus any unknown/unavailable entity in the system-service domains (conversation, stt, tts, notify). The state rule is deliberately narrow: an unavailable real device (an offline switch, a dead media player) still shows under No room so it can be assigned. This closes the standing residual; the KDoc's old "update rows such as Home Assistant / Unknown" line is corrected.
8. **Ghosted crossfade + back-gesture flash (one shared root cause).** The top-level AnimatedContent transition no longer fades: both branches (full-travel tab slides, one-third non-tab slides) are opaque slides now, and SizeTransform clips to the content area. The old slide + fade let outgoing and incoming pages show through each other mid-transition (readable ghost text — Settings over Manage home page, visible behind the rail on the 2026-11-04 runs), and clip = false let sliding pages spill past the content boundary into the rail zone. The same fade removal ends the back-navigation page fade that flashed cards (Garage Door) semi-transparent on return to Home. Slide directions and offsets are untouched (Koda port character preserved); this SUPERSEDES round 3's no-clip choice for exactly this symptom.
9. **Overlapping pencils.** The dashboard Favorites pencil hides while any modal is open (new `LocalModalActive` CompositionLocal, provided from HavenApp's existing modalActive — sheet/detail/panelDetail/popout; the camera dialog has no pencil so its exclusion is moot). The old always-visible pencil sat right next to a popup's own header pencil under the scrim; the scrim already blocks its taps, so nothing is lost, and the title's weight(1f) means hiding causes no reflow.

## Feedback round 16 (2026-11-04, nine feedback items from the 2026-11-04 videos)

1. **Split-screen rule (aspect ratio).** The unfolded/tablet layout now engages ONLY once the window is at least 600dp wide AND at least as wide as it is tall (new pure `isWideLayout`, unit tested). The old width-only gate flipped to tablet mode the moment the width crossed 600dp, so at ~50% split-screen the app wrongly showed tablet mode; folded portrait now stays folded portrait in split-screen until the width reaches the height. Unfolded portrait (e.g. 707x822dp) is folded layout under this rule, by explicit decision. The window size class itself is provided but consumed nowhere, so only the wide-layout gate changed.
2. **Card/panel edge buffer (top + sides only, bottom untouched).** Detail pages (generic, camera, weather, both unfolded two-column and folded single-column roots) gain the thermostat popout's 16dp top buffer — they were flush to the panel edge — and the sheet's top spacer grows 12dp to 16dp. Sides were already 16dp everywhere; tiles (16dp top/sides) and panel cards (16dp all) already matched. The bottom is untouched: content still scrolls up from the very bottom of the card. Round 15's 8dp→12dp row spacing was a different axis and did not satisfy this; this is the edge buffer. Standing Lamp panel title is the known still-short case this fixes.
3. **Switch–text buffer.** A default 12dp buffer between toggle switches and their labels everywhere a label+switch row pairs them: KodaMenuRow (covers all Settings switches), Tab Manager show/hide rows, Home Editor section rows, Automation/Helper builder condition rows, and Round 3 toggle rows. A long label used to run flush into the switch; the buffer is now guaranteed regardless of label length.
4. **Morph refinement (three sub-fixes).** (a) White exit flash, killed at the mechanism: tiles and cards now report their container color alongside bounds/shape (`LocalCardColors` + `reportCardColor`), each morph captures it at open, and both surfaces start there — the folded surface lerps from the tile's true color (its call sites previously passed one constant, surfaceContainerHighest, so a yellow lamp tile or purple media tile ended its exit as a pale rect over the hidden colored tile) and the unfolded popup now lerps the same way (it previously had no lerp at all: full panel cream from frame 0). At rest the panel color is byte-identical to before. (b) Speed: the five popup open effects (sheet/detail/panel-detail/popout/camera-dialog) move from springBounds (0.8/MediumLow) to a new `popupMorphSpring` token (500/0.85, matched to Koda's recorded player expansion, kept separate from sheetSpring so popup speed tunes independently); dismiss keeps settleSpring (Koda's player settle). (c) Origin: sheet and popout render sites now read the captured-at-open rect first (`anim.source ?: cardBounds[id]`), matching detail/panel-detail — while a panel is open, that panel's own cards report their panel-internal rects under the same entity id, poisoning the live map, so live-first exits landed on the panel-internal rect ("opening out of proper location"). The thermostat popout never showed this because it contains no reporting card. Rotation mid-panel is the one accepted edge: the exit lands on the pre-rotation tile rect rather than tracking the moved tile.
5. **Domain defaults dialog — Entity-type bar cream.** ONLY the "Entity type: Lights" selector bar changes: cream container #F6F5EC (the app's background cream) with on-surface text, differentiated from the green around it. Nothing else in the dialog moves.
6. **Add Tab bug — fixed as a visibility gap, not a creation failure.** Creation itself was sound (tab persisted, listed in Manage tabs, navigable): the "climate" tab was created unpinned, so it appeared nowhere the user looked — not on the bar, and the empty tab screen reads as nothing happening. A newly created tab is now auto-pinned when a pin slot is free (2-cap respected, same rule as the Pin button, reversible in one tap), so it lands on the bar where he looked. Pure `shouldAutoPinNewTab`, unit tested.
7. **Connection switcher.** Settings → Connection → Instance row is now tappable and shows Local vs Cloud for the EFFECTIVE target: anything that is not a local address reads Cloud (pure `isLocalAddress` — localhost, loopback, RFC 1918, link-local, mDNS .local, IPv6 loopback/link-local/unique-local — unit tested, including the 172.16/31 vs 172.32 boundary and hostnames merely starting with "fc"). The dialog offers Automatic / Local only / Cloud only plus a Cloud URL paste field for the Nabu Casa remote-UI URL. AUTO means Wi-Fi → local, off Wi-Fi → cloud, re-resolved by a NetworkCallback on transport change (reconnects only when the target changes). Wi-Fi TRANSPORT is the platform-allowed signal — SSID-specific "home" detection would need location permission, so any Wi-Fi counts as local-capable; pinning Local/Cloud overrides AUTO. The same HA token works through both URLs (remote UI forwards to the same instance), so switching never re-logs in. Login slots both sides without losing the other (a cloud login no longer wipes a stored local URL). Cloud-URL auto-detection from HA's cloud integration is not exposed through any API the app uses, so the cloud side is a paste field — no URL is ever guessed or hardcoded from a screenshot.
8. **Bottom-clearance clipping fix.** The unfolded 20dp "daylight" reserve is gone (unfolded has no bottom pill to mirror; the gesture-bar inset stays, so content still clears the system bar), and trailing reserves grow 16dp → 32dp on the folded Home list, the unfolded Home right column, and the Settings list, so the last row (Bedroom media tile on unfolded Home, Instance tile in Settings) scrolls fully into view and draws to the bottom. The exact pixel mechanism was not provable statically — the padding math already said the last row should be visible — so the device check (last row fully visible on both surfaces) is the acceptance for this item.
9. **Rail highlight off-by-one.** Two stale-index mechanisms fixed: the rail's modal fallback now stores the last on-tab SCREEN and resolves its index fresh against the current bar list every composition (a stored index pointed one slot off after Add-a-tab shifted the list), and the rail's icon-rect map is now keyed by SCREEN instead of item index (the old index keys went stale the moment the list changed, so the indicator glided to the previous item's rect). Both chrome variants already share `selectedNavIndex`, so they cannot disagree about the active tab; the fix keeps that single rule and removes the two stale-index caches around it.

## Front-door camera fallback (2026-11-04, device-proven diagnosis)

Device evidence the same day: `camera.front_door_69a1` (ring-mqtt) is UNAVAILABLE, while the official `camera.front_door_live_view` loads video in the HA app and stays "idle" while playing — viewing a camera changes no entity states, so there is no trigger entity; the stream is pulled, not triggered. Haven's front-door panel led with the 69a1 MJPEG path, so when 69a1 went down the panel cycled through a futile switch-wait and error path instead of showing the working camera.

1. **Shared source resolution.** New pure `resolveFrontDoorCameraSource` (CameraStream.kt, unit tested): use `camera.front_door_69a1` whenever it is present and not "unavailable"; otherwise fall back to `camera.front_door_live_view` when it is present and not "unavailable"; both down resolves to null. Only the literal "unavailable" state triggers the fallback — any other 69a1 state still tries 69a1 first, keeping the existing error path.
2. **Panel Live View.** The Live tab now resolves on every state change: when 69a1 is down it skips the 69a1 attempt entirely (no switch-wait, no error cycle) and shows the official camera's MJPEG proxy directly through the same MJPEG body. The fallback body passes NO onFirstFrame, so its frames never feed `mjpegFrames` and never earn the LIVE badge; the status line reads the neutral "Front door camera" (no liveness claim, no recording claim). When 69a1 recovers, the panel re-arms the live body automatically (failure latch cleared, body remounted) — no panel reopen needed. The 69a1 LIVE-badge rule itself is unchanged, as is the recording fallback after a 69a1 stream error and the untouched Event History tab. WebRTC stays gated off.
3. **Hero quadrant.** The front-door quadrant keeps `camera.front_door_live_view` as its id — tap routing to the front-door panel and morph-source keys are untouched — but when BOTH sources are down it now shows an honest "Unavailable" state instead of a stale cached snapshot, with no overlay pill contradicting it.
4. **Both down.** Panel and hero both show an honest unavailable state, never a blank or broken frame.

## Feedback round 15 (2026-11-04, six feedback items from the 2026-11-04 videos)

1. **Tile saturation.** The six pale light tile colors carry a little more saturation (chroma scale around the channel mean for the chromatic pastels, hand-tuned sage), so pale tiles read against the cream background like the red and yellow tiles do. Heating FFDBD0→FFD1B9, cooling D4E3FF→BDDEFF, lock DCE1FF→CCD7FF, alert FFDAD6→FFCDC4, media EFE0FF→EFCEFF, sage EFF1E8→E6EBD3 — applied to both the tile presets and the domain-matrix light containers, so presets and defaults never diverge. Lime and amber were already vivid and are untouched, as are all dark values and the background cream #F6F5EC itself. Inactive dark badges stay visible; temperature coral-red and humidity sky-blue stay as bypasses.
2. **Unfolded panel morph-from-tile.** SUPERSEDES the old rule that unfolded popups "enter from the pressed tile's direction": the centered popup no longer fades/slides in from the side. It now grows out of the tapped tile's own rect and shape (new pure `tileMorphTransform` in MorphOverlay.kt, same container transform as the folded morph and Koda's player), with the Koda reveal rule for content (invisible while the surface grows, in late; out early on close) and the round-11 corner landing (clip lerps into the tile's own outline). Exit restores the exact prior view; the unfolded rail never disappears during a panel. The target rect is computed analytically from anchor + overlay geometry + the surface's own untransformed size — never from a placed rect, so the transform cannot feed back on itself. Sourceless popups keep the old fade + scale in place. Folded full-screen morph unchanged.
3. **Customizable tabs.** SUPERSEDES the 2026-11-03 four-destination lock: Home and Settings are LOCKED and always first/last; Scenes and Energy are now hideable (Settings → Manage tabs switches, both on by default); up to two custom tabs can be pinned to the bar (Pin/Unpin per tab, sitting between Energy and Settings, six tabs max). Pinned tabs navigate through the existing UserTab screen; hidden and pinned are independent flags. Manage-tabs copy now says Home and Settings always stay; a Reset restores Scenes/Energy shown and clears all pins. Tab switches keep Koda's pill-bar transition; the active item stays one unified lime-on-near-black capsule in both themes.
4. **Settings rail highlight.** Settings submenus (Home editor, Room manager, Tab manager, Automation/Scene/Script/Helper builders, System updates) now map back to Settings for rail active state (`navActiveScreen`), so the Settings rail highlight persists inside any Settings submenu. Browse destinations (Boards, Areas, Search, User tabs, Dashboard edit, Area detail, Camera clips) keep the nothing-selected rule.
5. **Domain defaults.** New Settings → Shapes → Domain defaults dialog sets the DEFAULT badge shape, DEFAULT card shape, and DEFAULT tile color per entity type/domain. Resolution everywhere is per-entity Card setting > domain default > global > built-in, keyed off domain (a socket-named lamp is still a light). A chosen default tile color drives badge and icon tints as dark versions of itself through the same preset derivation as a per-entity color.
6. **Lamp-row vertical padding.** Light detail-panel rows (Standing Lamp panel: Power / Room / Last changed / linked plug) now use the thermostat panel's 12dp block rhythm (was 8dp), so all entity rows share one default vertical padding.

## Feedback round 14 (2026-11-04, launcher icon v7 rebuild only)

User approved the v7 icon render ("Build this as round 14"). v7 is
a from-scratch PROGRAMMATIC rebuild, not an AI mockup: the AI image
generator kept anchoring to the v2 mockup through three iterations
(wrong scale, harsh center seam, swoop bleeding into the field), so
the icon was rendered deterministically in code from the user's
coordinate spec (script: haven-overnight/make-icon-round14.py) and
verified by pixel measurement, not eyeballing. This round ships
those exact pixels — no color, geometry, or proportion tweaks.

### v7 construction (108x108 adaptive grid)

- Field: cream/off-white #F6F5EC squircle (background vector fill
  changed from round 13's #EFF1E1 so both layers agree exactly).
- House: single smooth symmetrical pentagon — flat base Y=78
  (X 30..76), vertical walls of equal height to Y=50, straight
  roof slopes to a softly rounded center peak (53,29), uniform
  3.375-unit corner rounding (peak, both eaves, both bottom
  corners). No eaves, no ears, no letters, no dip in the roofline.
- Four layers, all strictly clipped to the house pentagon:
  L1 base shell, linear gradient vibrant medium-green #62BD59 at
  the peak to rich forest green #1E8E4E at the bottom-right;
  L2 warm golden-amber radial glow (#FFB732 -> #F4CE50 ->
  transparent) centered (43,54), sweeping above the curve from
  the lower-left wall (Y=66) to the right eave (76,50);
  L3 left-wall arch (X 30..42), cap curve starting where the
  eave corner arc meets the wall, pastel-yellow #DFDF88 at the
  cap fading to bright lime #7CE253 at the base, crisp right
  edge exactly at X=42; L4 lower-right emerald sweep below the
  curve, #5FD352 to deep emerald #179352. Plus a soft mint
  highlight (#B0EAB8) along the upper-left roof slope.
- Star: chunky symmetrical 4-point golden sparkle (gradient
  #FFD54F -> #F5A623), center (66,36), radius 7, outer tips
  rounded 1.25dp, placed BEHIND the house pentagon so the
  roofline cuts it cleanly — its bottom point tucks behind the
  upper-right roof slope instead of poking over the edge — with
  a faint warm glow trailing into the upper-right roof.
- Measured on the render (not claimed): content bbox
  X 30..76, Y 29.7..78; margins left 27.8% / right 29.6% /
  top 27.5% / bottom 27.8% (all clear of the 24% floor); no
  vertical center split; no dark bottom bar; no fringe outside
  the house (left-of-wall strip deviation from field: 0).

### Production variant (transparent corners)

- The v7 preview file has white corners (chat presentation). The
  shipped foregrounds are a transparent-corner variant: identical
  artwork inside the squircle, alpha 0 outside it (same squircle
  rect and radius, outside RGB set to the field color first so
  downsampled edge pixels can never fringe). Verified: variant
  matches the v7 preview pixel-for-pixel on all artwork pixels
  (max diff 0), squircle edge in the same place (first opaque
  pixel on the diagonal at 95, as predicted for radius 324).
- Foreground drawables (drawable-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi)
  at 108/162/216/324/432 px; legacy mipmap PNGs (launcher +
  round identical, per repo convention) at 48/72/96/144/192 px.
  All LANCZOS-downsampled from the 1080 master (RGB and alpha
  resized separately — PIL's RGBA resize premultiplies and had
  darkened squircle edge pixels; split resize fixed it, verified
  by a zero-deviation composite). The anydpi XMLs already
  reference @drawable/ic_launcher_foreground — no XML edits.
  Monochrome vector untouched (themed icons need it).
- Masked-render check (launcher simulation at 432, background
  #F6F5EC + foreground): ZERO house/star pixels lost under the
  circle mask and ZERO under the squircle mask; composite
  outside the house matches the field with deviation 0 (no white
  or dark fringe at any mask edge); masked result matches the
  v7 preview inside the safe zone with max diff 0.

### Unchanged

- No Kotlin, no dial, morph, drag, scroll, or theme code touched.
  Signing files byte-identical to 616e3a96; webrtcEnabled=false.

## Feedback round 13 (2026-11-04, launcher icon raster rebuild only)

User verdict on the shipped round-12 icon (2026-11-04 morning,
device screenshot): STILL BAD — "a bastardized version of the
mockup." The screenshot shows hard vertical seams, a flat roof,
and a small plain-yellow sparkle where the mockup has soft
blends and a big two-tone sparkle. Diagnosis: his launcher's
vector path flattened the round-12 alpha layers, exactly as it
dropped vector gradients in round 7. Two vector approximations
in a row landed wrong on device, so this round stops
approximating in vector entirely: the icon IS the mockup's
pixels now.

### Raster construction

- Foreground: raster PNGs derived from the user-approved
  layered preview image (2026-11-04, "Use this, build it as
  round 12"), in drawable-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi at
  108/162/216/324/432 px for the 108dp adaptive layer. The
  anydpi XMLs already reference @drawable/ic_launcher_foreground,
  which now resolves to these PNGs — no XML edits needed.
- Correction to an earlier assumption: the preview file has NO
  dark backdrop (the dark surround seen in chat was the chat
  UI's presentation background). The file is full-bleed pale
  field with a soft edge vignette in the outer ~2-3%; the
  vignette is cropped off (1% safety inset, square center-crop)
  so no gray border survives into the icon.
- Background: the existing cream vector, fill changed to the
  preview's sampled field color #EFF1E1 (round 7's #FAF7F0 was
  the vector era's cream), so background and foreground fields
  agree exactly. The foreground is opaque (it IS the preview),
  so parallax edges stay seamless.
- Old vector foreground (round 12's layered polygons) DELETED,
  replaced by the PNGs. Monochrome vector UNTOUCHED (themed
  icons need a vector silhouette; it does not affect the
  normal icon and already matches the house geometry).
- Legacy mipmap PNGs regenerated from the same raster artwork
  (flat #EFF1E1 field + foreground) at 48/72/96/144/192 px,
  launcher + round identical per repo convention, so the grid
  (PNG path) and the launcher (adaptive path) agree.

### Masked-render comparison (before pushing)

Composed background + foreground at 432 and applied circle AND
squircle masks the way a launcher would: ZERO house/sparkle
pixels lost under either mask; composite matches the preview
crop 100% within tolerance 45 (expected — the foreground IS
the crop; the masks are the real check). Sampled tones: roof
(133,196,91), center (223,188,68), door (232,210,111),
bottom-left (145,226,90), bottom-right (48,158,99).
Bottom-center band mean brightness 143 (a dark block would
read < 60). No dark curved band, no dark bottom rectangle,
no fringe — the outer ring is flat field by construction.

### Device checklist (owed)

1. Icon on the launcher grid matches the mockup.
2. Icon in the unfolded recents pane matches the mockup — the
   acceptance case.
3. Grid vs recents AGREE this time: there is no more
   vector-path divergence excuse. If they still differ, the
   raster itself is the issue and must be reported as such.
4. Nothing else changed (no code touched).

## Feedback round 12 (2026-11-04, launcher icon rebuild only)

User verdict on the shipped round-10 icon (2026-11-04 morning,
device screenshot + reference): INCORRECT. The target was
never in doubt — the approved no-curve image (reference
minus the dark green curved band from the top left, and
minus the dark rectangle at the bottom center; bottom is
light green left + green right) — but round 10 traced it as
SOLID FLAT OPAQUE facets, and on device that reads as hard
flat shapes, not the reference's depth. Round 10's error was
the construction, not the target. This round rebuilds the
foreground vector the way the user specified it (his words):
"a series of layered semiopaque polygons."

### Layer stack (bottom to top, all in ic_launcher_foreground.xml)

  L1 shell        #3FA85E alpha 1.00 — full house silhouette
  L2 roof         #8EE059 alpha 0.55 — roof polygon
  L3 golden band  #E8B93B alpha 0.78 — broad center band
  L4 golden core  #F5C542 alpha 0.55 — inner blob
  L5 door         #F0D67E alpha 0.92 — pale arch, LEFT
  L6 bottom wash  #8EE059 alpha 0.35 — full-width bottom
  L7 bottom lime  #8EE059 alpha 0.85 — bottom-left
  L8 bottom teal  #1E7A4C alpha 0.25 — bottom from x=56
  L9 bottom teal  #1E7A4C alpha 0.30 — bottom from x=68
  L10 sparkle     #F8C230 alpha 1.00 — star
  L11 sparkle pale #FFFBEA alpha 0.55 — pale quadrant hint

The overlaps — not opaque color boundaries — create the
blends: golden center over green shell, bright roof over
shell, lime-to-teal grade across the bottom (three teal
steps, not one flat block). No layer is dark where the
removed dark band or dark bottom block were; the bottom
center below the band is shell green with the lime/teal
split, exactly as in the approved preview. Geometry is
unchanged from round 10 (house 30..78 x 31..76 apex, door
left, sparkle at (70,33)), so the monochrome variant (same
silhouette + evenOdd door cutout + sparkle) and the cream
background layer are untouched.

### Verification before pushing (not claimed from CI alone)

The new vector was rasterized locally (render_vector.py
parses the vector file itself) and pixel-compared against
the user-approved preview image: 71.8% of house-region
pixels within per-channel tolerance 45, and every sampled
tone within ~35/channel (roof #6AC75B vs preview #6BC462,
golden center #DEBE42 vs #F2BF3A, door #ECD379 vs #F5CC60,
bottom grade lime -> teal tracked at all five stations).
Zero dark-green pixels in the house region; bottom-center
brightness 123 (a dark block would read < 60). The residual
is the preview's continuous gradients vs. finite polygon
steps — inherent to the vector medium, and the steps are
within one green/gold family, not unrelated opaque facets.
Legacy PNGs are rasterized from the vector file itself
(make-legacy-icon-round12.py), so they match the vector
layer for layer; the 192x192 PNG carries cream 83.7%,
roof 3.9%, center 1.6%, door 2.4%, sparkle 0.3%, zero
dark-green pixels.

### Hard constraints and the open risk

ZERO <gradient> elements in the vector (element count
after stripping comments, local and remote). Round 7
proved the Fold8 adaptive-icon path does not render
gradient-filled vector paths (the house vanished). Alpha
fills are NOT gradients and are used here instead — but
whether that same path honors fillAlpha is UNPROVEN: it
may render the translucency, or it may flatten the layers
to opaque (which would read close to round 10 again).
Only the device test settles it. The legacy PNGs render
alpha correctly by construction (local rasterization), so
the launcher grid (PNG path) and the recents pane (vector
path) may differ on device — the device checklist below
asks for that comparison explicitly, and a flat recents
rendering must be reported as a finding, not hidden.

### Device checklist (owed — needs the Fold8 pass)

1. Icon house visible on the launcher grid.
2. Icon house visible in the unfolded recents pane — the
   acceptance case.
3. Grid vs recents compared: if recents looks flat/opaque
   while the grid shows the blends, the device dropped
   alpha — report it, do not adjust the vector blindly.
4. Nothing else changed: no Kotlin, dial, morph, drag,
   scroll, or theme code was touched in this round.

## Feedback round 11 (2026-11-03, morph behavior + icon resolution only)

Device verdict on the round-9 build (2026-11-03 21:36 video,
no audio, frames reviewed): round 9's drag fix is CONFIRMED
working — the popup now follows the finger on exit — but
the user REDIRECTS the behavior in three items below. Round
10's icon vectors and PNGs are untouched; thermostat dial,
theme, nav, and every other behavior are untouched.

### Item 1: NO finger-following on drag dismiss

Round 7 shipped a finger-tracked scrub (the popup follows
the finger toward its tile); the user corrects that Koda
does NOT do that, and Haven must not either. Change: during
a drag-down, the popup does NOT move, shrink, or scrub under
the finger — it stays fully open. The drag remains a dismiss
gesture with the existing release rule unchanged (velocity
±120 px/s first, else 40dp travel): on release past the
threshold the popup springs down into its tile on the calm
settle spring (damping 0.95 / stiffness 350, no visible
bounce); below threshold it settles back open. Mechanically:
`dragProgressFor` now returns the animatable's value only
(the drag state's `dragProgress` survives as travel
bookkeeping for the release rule, never rendered); every
dismiss prologue's snap-to-drag-position is deleted (the
animatable never left open, so the settle starts exactly
where it sits); the non-commit release path drops its
snap-back; the centered popup's drag rect-lerp
(`centeredPopupDragTransform`, deleted with its test) is
gone — the enter transform owns every frame; the camera
branch's progress lambdas read the animatable only.
`LocalMorphClosing` no longer goes true mid-drag (round
7's mid-drag source-badge crossfade had nothing to
crossfade toward once the popup stops moving). KEEP
unchanged, all verified by reading the call sites: the
scroll-at-top gating (drag engages only from scroll top on
scrollable surfaces), dial/toggle/slider ownership
(on-thumb dial drags unaffected, pill toggles tap-only, M3
sliders horizontal), reduce-motion (drag never engages),
the single haptic tick at the dismiss commit, the
NestedScrollConnection-ancestor-of-scrollable order
(round-9 lesson), and the programmatic/back dismiss path
(still springs to the tile).

### Item 2: exit morph lands on the tile's corner radius

Mid-exit frames showed the morphing popup outline with
SHARP corners while the tile it lands in is rounded. Root
cause: `MorphSurface` clipped to a hardcoded
`morphCornerTop` (28dp at the card end) regardless of the
tile's configured shape, and the centered popup kept a
fixed 28dp surface. Change: every morph source now reports
its exact outline alongside its bounds
(`reportCardShape` into the new `LocalCardShapes` map —
grid tiles, both home-card variants, panel cards, Round 3
section cards, the weather card, and camera quadrants),
and every morph call site passes that outline as
`landingShape` (falling back to the store's
override/global card-shape resolution, then to the old
28dp behavior for a source that never reported). The
full-screen morph lerps its corners from square to the
tile's own four corners in step with the rect lerp
(`morphSurfaceCorners`, pure and unit-tested), so the
shrinking outline always reads as a scaled tile and lands
exactly; polygon tile outlines switch to the exact polygon
in the final fade window (progress < 0.1), where the
container alpha is already fading and the never-hidden
source tile shows through beneath — a crossfade onto the
coincident tile, not a pop. The centered popup lerps its
28dp corners into the tile's corners
(`morphPopupCorners`); its Surface shape is raised to
36dp (the largest tile corner) so the Surface clip cannot
cap a 36dp landing at 28dp, while the graphicsLayer morph
clip governs the visible outline; polygon tiles keep soft
28dp there because the centered popup scale-fades and
never lands on the tile rect. At minimum the corner radius
matches at landing with no sharp-corner outline at any
point of the morph, on both postures.

### Item 3: ONE icon resolution for tile, header, and traveler

Concrete case: eufy AE C10 (vacuum.t2292), an entity whose
icon the user changed. On the round-9 build the Favorites
tile badge showed the custom vacuum glyph, while the
detail header badge — and the badge traveling down during
the exit morph — showed the yellow circular-arrows glyph.
Root cause: the tile (and the sheet header, and the detail
page header via `detailHeaderCustom`) honored the custom
icon key, but the traveler (`TravelingHeader`) and the
detail page's hero badge (`DetailPageContent`) read
`domainIcon` directly — and the vacuum domain icon IS the
circular arrows (`Icons.Filled.Autorenew`), which is why
the mismatch read as "vacuum glyph vs circular arrows"
rather than two different icons. Change: one resolver,
`entityIcon(iconKeys, entity)` in `EntityControls`
(custom key wins, unknown keys fall back to the domain
icon), now used by all seven entity-badge sites: the grid
tile, the sheet header, the detail page header, the detail
hero badge, the thermostat popout header badge, the
control-card header badge, and the traveler. Deliberately
NOT changed: the sheet/detail menu-row icons (navigation
chrome, not entity badges), the shape-picker option
previews (neutral preview glyphs), and the
`DetailPopupHeader` default (unreachable — all four
callers pass `displayIcon` through `detailHeaderCustom`,
now via `entityIcon`). The icon picker UI and the stored
overrides themselves are untouched.

### Verification notes

- Baseline: remote HEAD be465850 (round 10), 357 tests.
- New pure tests: `roundedLandingCorners`,
  `morphSurfaceCorners`, `morphPopupCorners`,
  `morphSurfaceShape` polygon window (MorphMathTest);
  the deleted rect-lerp transform's test is removed with
  it (PopupDragTest). No test total change is claimed
  here beyond the suite's own run.
- Deliberately NOT changed: round-10 icon vectors/PNGs,
  thermostat dial, theme, nav, signing files (byte-
  identical to 616e3a96), WebRTC still gated off
  (`webrtcEnabled = false`), no negative padding
  anywhere (all progress/corner math clamped to [0, 1]
  before use).

## Feedback round 10 (2026-11-03/04, app icon only — user-approved generated image)

The user approved a generated icon image as the new app icon
(2026-11-03, "Yes use that"): cream squircle field, rounded
house with a smooth green-to-yellow roof, pale arched door on
the LEFT, yellow sparkle upper right, and NO dark green curved
seam down the middle from the top left (that line was
deliberately removed from the reference and must not reappear).
Baseline proven green first (357 tests, 0 failures on the
round-9 tree). Icon files only — no Kotlin, no dial, morph,
drag, scroll, or theme code touched.

### The trace: raster reference into a vector foreground

The generated image is a raster reference, not a drop-in
asset: the adaptive icon needs a vector foreground, so the
reference was traced into
`ic_launcher_foreground.xml` as flat facets sampled from
the reference image itself — roof #72C75F (sampled roof
peak), center #E8B93B (sampled golden center), door #F0D67E
(pale gold, lightened vs the sampled mid-tone so the arch
still reads against the golden center), bottom-left #8EE059
(sampled bright lime), body #3FA85E (sampled bottom-right
green), sparkle #F8C230 (sampled sparkle yellow). Band
boundaries were measured from the reference image (warm
band ~0.43..0.66 of image height at center, closing at the
right tip), not guessed: the golden center is a BROAD band
(top ~43-44, bottom ~62-64 at center), not the thin wedge
an early draft produced. The roof is ONE flat with its lower
edge shared with the golden band's top edge — no internal
seam, so the removed dark curve cannot reappear. The bottom
band's vertical lime/green seam is in the reference and is
kept (it is not the removed curve). Geometry
simplifications, stated: the reference's gradients become
flats; the sparkle is a single solid yellow (the reference
has yellow + pale quadrants); the house is normalized to
the rounds 7/9 safe envelope (30..78 x 26..76, sparkle
inside the 66dp safe-zone circle) rather than the
reference render's wider framing. The monochrome variant
is rebuilt to the same rounded geometry (silhouette +
evenOdd door cutout + sparkle). All 10 legacy mipmap PNGs
are regenerated from the same geometry
(`make-legacy-icon-round10.py`, 4x supersample, LANCZOS)
and pixel-verified (cream corners, all six house colors,
sparkle present).

### No-gradients constraint (why flats, again)

Round 9 proved on-device that the Fold8 adaptive-icon path
does not render gradient-filled vector paths (round 7's
house vanished, leaving only the solid sparkle, while the
shipped PNGs contained the house). The round-10 vectors
therefore contain ZERO `<gradient>` elements (verified by
element count after stripping comments). The background
layer (cream field) is unchanged.

### Verification notes

Local Gradle build could not run in this session (the
Android Gradle Plugin failed to resolve in this
environment, both online and offline) — CI is the verifier
for compilation and tests, as in round 5. What was verified
locally before pushing: aapt2 compiles all three icon
vectors cleanly; both vectors contain zero gradient
elements; the regenerated PNGs carry every facet color;
`webrtcEnabled = false`; no negative padding (vectors
contain no padding); signing files byte-identical to
616e3a96; round-9 dial/morph/drag/scroll code untouched.

## Feedback round 9 (2026-11-03/04, drag-down fix + Home scroll restore + flat icon)

The user's 2026-11-03 20:34 video (44s, no audio, frames
reviewed at 1fps) on the round-7 signed build reported three
fails: finger drag-down to close does nothing on the detail
surfaces he used (lamp detail from the Lights panel,
thermostat detail); Home returns to the top on exit from
detail instead of its pre-open scroll position; and the
launcher icon renders as a cream squircle + sparkle only —
the house is missing (circled by him in the recents pane).
Baseline proven green first (357 tests, 0 failures on the
round-8 tree).

### Item 1 — finger drag-down to close: ROOT CAUSE, the nested-scroll modifier order

Root cause, found by reading the gesture chain (not by
guessing): a `NestedScrollConnection` receives events ONLY
from scrolling DESCENDANTS — it must be an ANCESTOR of the
scrollable. Round 7 attached it the wrong way round in both
drag wrappers: `verticalScroll(...)` first, then
`.nestedScroll(PopupDismissConnection(...))` — i.e. the
connection sat INSIDE the scrollable's own node. The
scrollable's post-scroll leftovers (the at-top downward
drag that should start a pull) dispatch to ANCESTORS, so
they never reached the connection: `onDragDelta` never
fired, no scrub, no dismiss, on EVERY scrollContent
surface — sheet, detail, panel-detail, popout, folded and
unfolded alike. The camera branch was unaffected (it uses
the direct `popupDragDetector`, no scroll competition),
which is why the mechanism looked right in review: the
plumbing existed everywhere, but the scrollContent path
could never deliver a delta. The detail content itself
was exonerated first: `DetailPageContent` is a plain
Column with no own scroll, the pill toggle is
clickable-only, and the thermostat dial consumes only
on-thumb drags — nothing in the content swallows the
gesture; the wrapper order was the whole failure.
Fix: in `PanelOverlay`'s scroll branch and in
`CenteredPopup`'s inner Column (`HavenApp.kt`), the
connection now sits OUTSIDE (before) `verticalScroll`.
Both sites carry a comment stating the ancestor rule so
a future tidy-up cannot silently revert it. No change to
the drag controller, the release rule (velocity ±120 px/s
first, else 40dp travel), the calm settle spring (0.95 /
350, no visible bounce), scroll-at-top gating, dial /
toggle / slider gesture ownership, or reduce-motion
behavior — all of round 7's behavior, now actually
reachable by a finger.

### Item 2 — Home scroll position restored on detail return

Root cause: the scaffold renders its base page through
TWO different parents — directly via `content(base)`
while a detail overlay is open, and inside
`AnimatedContent(targetState = base)` otherwise (round 4,
item 4's bleed/flash rule: the base must not slide/fade
under an open morph). Swapping parents DISPOSES the base
content, and with it any scroll state remembered inside
`TodayScreen` (the folded `LazyColumn`'s implicit
`LazyListState`, the unfolded panes'
`rememberScrollState`), so Home recomposed at offset 0
on every detail return — twice per visit, once on open
and once on close. Fix: the three Home scroll states are
hoisted into `HavenApp` scope beside the backstack
(`homeListState`, `homeLeftScrollState`,
`homeRightScrollState`), threaded through `ScreenContent`
(new params before the lambda params, round-5 lesson) into
`TodayScreen`, and attached to the folded `LazyColumn`
and both unfolded panes. The state objects now outlive
the branch swap, so Home lands on its exact pre-open
offset after exiting any detail or panel, on both
postures. The branch-swap rule itself is untouched, and
Home's layout, content order, and pill behavior are
untouched. Two stated consequences: the offset now also
survives tab switches (previously a switch disposed Home
and reset it) — the same retain-position principle, not
a second feature; and sign-out still resets, because
these remembers live below the auth early-return and
dispose with the post-login composition. Other tabs'
scroll states have the same branch-swap disposal
mechanism and are NOT hoisted here — Home only, per the
complaint; a later round can hoist them the same way.

### Item 3 — launcher icon rebuilt with solid flat facets (no gradients)

Root cause, narrowed by elimination: the shipped round-7
APK's legacy PNGs DID contain the house (pixel-verified
by the parent: cream corners, yellow-orange centers, all
5 densities × 2), and the device rendered the cream
field + solid yellow sparkle — so the failure is
specifically the VECTOR foreground's five
gradient-filled house paths not rendering in the
device's adaptive-icon path, while solid fills render.
Fix: `ic_launcher_foreground.xml` is rebuilt with SOLID
FLAT facet colors and zero `<gradient>` elements, same
segmented-house design and same 30..78 x 30..76 envelope
(safe-zone discipline unchanged, outermost ink still
under ~30.7dp from center): each roof side split into a
lime #9BF67C ridge triangle + deep leaf #3E6B2F eave
trapezoid (outer edges stay deep leaf, crisp on cream;
lime never touches cream directly, exactly as round 7's
gradient direction intended), mid green #66BB6A body,
center panel as two flats (yellow #FFD54F top,
orange #FF8F00 bottom), cream arched door on the LEFT,
yellow sparkle upper right. Background (cream field)
and monochrome (already solid fills) are unchanged.
All 10 legacy mipmap PNGs regenerated from the new
vector (`make-legacy-icon-round9.py`) and pixel-verified
(cream corners, lime ridge, deep-leaf eaves, mid-green
body, yellow/orange center, cream door, yellow sparkle).
The recents pane on the user's device remains the
acceptance case — this round cannot device-test it, so
the icon still needs his eyes on the launcher grid and
the unfolded recents pane.

## Feedback round 8 (2026-11-03/04, thermostat radial + set-temp size)

The user's 2026-11-03 screenshot + words: "The radial is too
dark a color to see and the set temps need to be reduced in
font size so they don't clip the radial." Screenshot state:
Ecobee Thermostat detail, Heat 66° / Cool 69°, Indoor 69°,
steppers below the dial. Baseline proven green first (357
tests, 0 failures on the round-7 tree).

### Item 1 — thermostat arc track lightened

Root cause: the dial's base track was `scheme.surfaceVariant`
— in the dark theme that is a dark gray on the dark scallop
disc (dark-on-dark, near-invisible), and in the light theme
only a weak gray on the pastel disc. Fix: the base track is
now `scheme.outline`, M3's mid-tone border color, which reads
~3.5:1 against the disc in BOTH themes. Applied at all three
sites that draw a thermostat track: `ThermostatDial`'s
heat_cool base arc and its else-branch arc
(`ThermostatDial.kt`), and the shared generic `Dial`'s track
(`Expressive.kt`), which the thermostat sheet uses. The
shaded band between the thumbs keeps its thumb-matched
container blend deliberately — it carries the setpoint colors,
not the track color — and the heat/cool gradient arcs, the
off-mode dashed track, thumb colors, and mode colors are
untouched. Ride-along, stated not hidden: the weather dial
shares the generic `Dial` component, so its track lightens
too. Track only: no setpoint, drag, clamp, or gap-proofing
behavior changed.

### Item 2 — dual set-temp numbers smaller (52.sp -> 44.sp)

The dual Heat/Cool numbers dropped from 52.sp to 44.sp in
`ThermostatDialBlock` (`ThermostatPage.kt`): both the measured
`numberStyle` and `KineticTempText`'s default, which the dual
call sites rely on, changed together so measurement still
matches rendering. Round 5's `dualNumberGapPx` separation rule
is untouched. Why 44.sp: at 52.sp the popout row (~185dp)
kissed the ring at rest and the kinetic 1.15x scale-out
pushed it past the ring mid-change — the clipping the user
saw. At 44.sp the labels span ~129dp, the row spreads to
~176dp (8dp daylight per side at rest), and even the 1.15x
outgoing peak (~93dp from center against the 96dp inner
edge) clears the ring on the 240dp popout; the 300dp hero
gets the full computed spread. 44.sp is the largest size
that clears the ring even at kinetic peak. The single
setpoint number stays 72.sp, and the steppers stay below
the dial.

### The 62°/72° axis ends — investigated, CLOSED: user-confirmed override, no resolver change

The same screenshot's arc ends read 62°/72° where the
round-6 formula gives 50/80 for setpoints 66/69. Checked
first, before touching anything: `resolveThermostatDisplayRange`
(`ThermostatLogic.kt`) implements the formula literally
(low = max(40, floor(low/10)*10 − 10), high = min(100,
ceil(high/10)*10 + 10)), and the formula's outputs are
always multiples of 10 — it can NEVER produce 62/72. A full
surface audit confirmed every live thermostat surface funnels
through that one resolver: detail and popout
(`DetailPages.kt` → `ThermostatPopout` →
`ThermostatDialBlock`), the panel and Device Controls deep
link (both → `ThermostatPopoutContent` → `ThermostatPopout`),
and the sheet (generic `Dial` with the resolved window).
`ThermostatHero` takes the range as a parameter and has no
call sites; no other range producers exist; nothing hardcodes
62/72; the Settings prefs are contains-guarded null by
default. So a stored Settings override of 62/72 was the only
code-consistent explanation — and the user CONFIRMED it
(2026-11-03 20:39: "Settings shows an override"). Working as
designed. The resolver and all axis logic are untouched in
this round. (Reproduced on the round-7 signed build first,
which is what made the override-vs-bug question sharp: the
signed build's uninstall should have wiped prefs, so the
override was either re-set after reinstall or restored with
the app data — either way the stored override is the cause,
and the user's confirmation closes it.)

### Dial spacing fixes (folded into round 8 from the user's 4 bullets)

Same dial code as items 1–2 above, so these ride in round 8
rather than queuing behind it. Applied to the shared
`ThermostatDial` / `ThermostatDialBlock` (popout, detail,
hero) consistently:

(a) Wider arc: track inset 16dp -> 12dp
(`ThermostatDial.kt` `trackRadiusPx`), opening the center
by 8dp of diameter (popout inner width 192dp -> 200dp, hero
252dp -> 260dp; `ThermostatDialBlock`'s gap math moves from
diameter − 48dp to diameter − 40dp). Guardrail honored:
grabbed stroke outer sits 1dp inside the canvas (12dp −
11dp half-stroke), the unstaggered circular thumb 2dp
inside, and the staggered cool thumb 10dp past the canvas
edge but still 10.8dp inside the popout scallop's valley
(140.8dp) — the scallop is the visible bound, exactly as
round 6's valley analysis had it. 10dp would clip the
grabbed stroke, so 12dp is the smallest safe inset.

(b) Centered readout: the dual numbers + Indoor label
overlay drops 16dp (`ThermostatDialBlock`, `Modifier.offset`
— a visual shift, no layout change, and NOT negative
padding) to the dial's vertical center, filling the empty
bottom-gap space. Composes with the smaller numbers: the
row's bottom corners sit ~7dp inside the ring at rest on
the popout, glyphs clear at kinetic peak.

(c) Circular thumbs: `drawThumb` draws 20dp circles (fill,
white 1.5dp ring, 4dp dot, unchanged bloom) instead of
30x20 ovals, so the right handle never looms over the 69°
text. VISUAL ONLY: the grab target stays 32dp and
handle-grab-only (nearest wins) is preserved — the touch
target does not shrink with the visual thumb. The stagger
radii (`dualThumbRadii`) are untouched, so close setpoints
still separate exactly as before.

(d) Anchored min/max labels: the legend row below the dial
is REMOVED from `ThermostatDialBlock`, and the range ends
are now drawn inside the dial canvas itself
(`ThermostatDial`, `drawText`), each centered on its arc
endpoint's x and 29dp below the endpoint (10dp thumb radius
+ 9dp half text height + 10dp gap) — so even a thumb
staggered outward at the extreme end clears its label, and
the popout label bottom (234dp of 240dp) stays inside the
canvas. The labels always show the EFFECTIVE ends (formula
or override, e.g. 62°/72°).

Sheet mapping, stated not hidden: the thermostat sheet uses
the generic `Dial`, not this code. Its handle is already a
circle (no (c) change needed), its track already sits at
the handle edge with no scallop to widen toward (no (a)
change possible without redesigning the handle), its
center content is a single number, not the dual readout
(no (b) target), and its range-end legend row below the
dial also labels the slider under it, so it stays (no (d)
change). The sheet DOES get the round-8 track lightening
above, since it shares the generic `Dial`'s track color.

Unchanged by everything in this round: setpoint values,
axis formula, override behavior, Mode/Fan bars, steppers
(stay below the dial), scallop shape, morph/drag/icon/
signing code.

## Feedback round 7 (2026-11-03/04, Koda now-playing morph port)

Port of Koda's now-playing open/close animation into Haven for
every tile-to-popup transition, per the user's 2026-11-03 spec
(video of Koda's morph; the video's audio was just the playing
podcast, no narration — the written spec is the directive).
Port source: `koda-morph-port-plan.md` (Koda remote HEAD
`12c47f64` on branch `podcasts`; the finger-scrubbed morph is
Koda's video `VideoPlayerOverlay` minimize, NOT the music
player, whose expand is threshold-gated). Baseline proven
green first (349 tests, 0 failures on the round-6 tree).
Final: 357 tests, 0 failures (349 + 8 new in
`PopupDragTest`). Local APK built this round (daemon
healthy); the shipped artifact is the CI release APK (see
rebase note).

### Rebase note (read first)

Round 6 landed after the port plan was written, so every plan
mapping was re-verified against the round-6 tree before
applying anything. Two plan claims corrected against the
code: (1) CenteredPopup's `onPanelPlaced` callback reports
`boundsInRoot()` — the plan's rebase note said boundsInWindow,
which is wrong; the drag rect-lerp therefore uses the same
coordinate space as `cardBounds` with no conversion. (2) The
"Round 7" comments already in the tree predate this round and
were not reconciled against remote state; code was reported
as found on disk. Remote HEAD moved during this round from
`bffe4642` (#114) to `616e3a96` (release signing: CI builds a
signed release APK — `.github/workflows/build.yml` +
`app/build.gradle.kts` only, no Kotlin changes). Round 7
rebased onto `616e3a96` (those two files taken verbatim from
the new head, so the push cannot regress signing); the
round-7 parent is `616e3a96`. Consequence for delivery: CI
now uploads `haven-release` (signed release APK) instead of
`haven-debug`, while local builds stay debug for testing.
Migration cost (standing): the release signature differs
from all debug keys, so the first signed build will NOT
install over a debug build — uninstall once, which wipes
SharedPreferences (custom presets, thermostat override) and
they must be re-set; after that one move, signed updates
install over cleanly.

### 1. Finger-tracked drag + calm settle (all popup surfaces)

One `PopupDragState` per surface (sheet, detail, panelDetail,
popout, cameraDialog), hoisted next to the progress
animatables; the progress lambda each surface and its
travelers read is drag-aware (`dragProgress` while dragging,
animatable otherwise). Plain snapshot state on the finger —
NOT an Animatable scrubbed mid-gesture (Koda's MutatorMutex
cancels per-event `snapTo`, which is why Koda's minimize
keeps `dragProgress` as a plain float). Drag formula, from
Koda: `progress -= dy / rangePx`, clamped to [0,1], floor 0
(Haven has no curtain path, so no 0.25 floor). Release rule,
from Koda: velocity momentum first (±120 px/s — downward
drift commits, upward drift always restores), else travel >
40dp of the drag range. Settle spring, locked by the user:
Koda's `PLAYER_CONTAINER_SPRING` as-is — damping 0.95,
stiffness 350, near-critically damped, NO visible bounce —
for drag-release AND programmatic dismiss (new `settleSpring`
token in Theme.kt; all five dismiss `animateTo(0f, …)` sites
moved from `springBounds` to it). Opens stay on
`springBounds`; camera fullscreen enter/exit keep
`sheetSpring` (different mechanism, out of scope). A
cancelled drag springs back open on the settle spring.
Haptics: a single `ContextClick` tick at the dismiss-commit
branch only — never during drag, never on cancel.
Drag ranges (decided, stated): PanelOverlay morphs use the
exact top-edge travel (source top − target top, target-height
fallback); CenteredPopup uses the vertical travel of the
surface center toward the tile center (placed-height
fallback). The 40dp commit threshold is independent of range.
CenteredPopup drag needs a rect-lerp extension, not just
scrubbing its enter transform (it never morphed from the
tile rect): during a drag ONLY, the Surface lerps from its
placed rect (captured via `onGloballyPositioned`, root
coordinates) toward the tile rect — translation + scale
about the center origin, alpha on the enter fade curve
(solid until 0.5, out by 0.15, Koda's full-content window).
At progress 1 the drag transform is identity, so the
open-to-drag handoff is continuous; the open path
(centeredPopupEnter 96dp/scale/alpha) is byte-identical.
Scrim alpha already reads the drag-aware progress, so it
fades as the popup is pulled down. Panel-detail included
(the spec listed sheet/detail/popout/camera, but it uses
the same wrappers and leaving it undraggable would be an
inconsistent surface).

Judgment call — open curves through drag and settle, NOT
close curves: Koda's minimize fade (solid until 0.5, out by
0.12) matches Haven's ENTER curve, not its close curve
(content gone by 0.85). Close curves would make popup
content vanish almost immediately on drag start,
contradicting "popup follows my finger". A per-surface
`settling` flag carries this: `isClosing` passed to surfaces
and travelers is `exiting && !settling`, and the root
`LocalMorphClosing` is `exiting || anyDragging`, so during a
drag the content rides down visibly, the traveler badge
lerps back toward the tile, and the source card badge
crossfades in on the close curve — and during a
drag-initiated settle the same curves hold. Dismiss
functions are the single choke point (Back, badge tap,
scrim tap, drag release all land there): the prologue marks
`settling` BEFORE flipping `exiting`, and the launch snaps
the animatable to the drag position before clearing the
drag flag, so the progress lambda never jumps
(dragProgress equals the anim value at every step). Drag
release calls the surface's dismiss while still dragging so
its prologue keeps the open curves; a restore snaps to the
release position, clears the flag, then animates back open.
Koda's two latched-drag defenses are both copied: snap in
`try`, flag clear in `finally`, plus the open effects
resetting drag state on every open (backstop). At-rest gate:
a drag only engages at anim ≥ 0.99, so a grab mid-open
never fights an in-flight open animation. No 2D
translation: vertical only (Koda has no horizontal container
drift; horizontal on bars is skip).

### 2. Scroll + control coordination

No competing parent pointerInput detector on scrollable
popups: consume-order arbitration is unprovable from app
code, so the port uses the nested-scroll protocol instead
(parent pre-scroll runs before child consumption).
`PopupDismissConnection` (MorphOverlay.kt) attaches to the
popup scroll container itself — never the outer box (Koda's
comments-panel lesson: a nested scrollable inside the popup
keeps its own dismiss). onPreScroll: once `pulling` (the
latch, stored in drag state so recomposition creating a
fresh connection cannot lose it) latched, feeds every delta
to the drag and consumes all of it, UserInput only.
onPostScroll: leftover downward drag (scroll container
already at top, or content shorter than the viewport so
nothing scrolls) latches pulling, feeds the drag, consumes
all — this IS the at-top signal, so no ScrollState hoisting
was needed (every popup ScrollState is an anonymous inline
`rememberScrollState`). onPostFling: only if this
connection started the pull, unlatches and releases with
the fling velocity, consuming it; a fling that merely runs
out of list at the top is left alone. The non-scroll camera
branch (PanelOverlay `scrollContent = false`) carries a
direct vertical-drag detector instead — no scroll
competition exists there; child controls that consume win
(innermost-first), bare-surface drags reach it. Control
isolation, verified in code: the thermostat dial already
consumes only on-thumb (off-thumb drags pull the popup,
which IS the spec); pill toggles are tap-only, so a vertical
drag starting on one is not their gesture; M3 sliders are
horizontal, so a pure vertical drag starting on one is not
the slider's gesture (diagonal drags are consumed wholesale
by the slider, cancelling the popup detector). One
targeted fix, spec-required: the generic Dial
(Expressive.kt) NEVER consumed even on a genuine handle
drag, so a sheet-dial handle drag would both change the
value and drag the popup (and already wrongly scrolled the
popup today) — it now calls `change.consume()` when
grabbing. Tiles unchanged: no HomeRoomCard, EntityTile,
card color/shape, inline switch, or cardBounds reporting
change is in this round.

### 3. Back arrows + close buttons removed; DetailTopBar stays

Removed: PanelOverlay's back row + `showBackBar` parameter
(the parameter goes with the row — it had no other reader —
and the camera call site's explicit `showBackBar = false`
argument goes with it; PanelOverlay's `onDismiss` parameter
goes too, since the row was its only reader — BackHandlers
and traveling-badge taps call the dismiss functions
directly); CenteredPopup's back row (no parameter existed);
FrontDoorCameraPanel's header ArrowBack (title + LIVE badge
kept; `onClose` stays — the video bodies use it for error
paths); the non-Front-Door camera dialog header ArrowBack
(name + subtitle column expands to full width);
CameraHeader's top-right Close (X) + its `onClose` parameter
(both call sites updated; the video bodies' own error-path
Close buttons are untouched); ClipPlayerScreen's top-end
Close TextButton (the content "Close" button in the
unavailable state stays — that state has no other visible
dismiss affordance). Each removed row's height simply
disappears; content below shifts up by that amount, no
spacer compensation (none compensated today). All
BackHandlers stay wired to the dismiss functions, so system
back plays the same spring-down into the tile. DetailTopBar
STAYS (verified, not assumed): all 11 call sites are
backstack pages in ui/screens (Browse, Builders,
Dashboards, DetailPages CameraClipsScreen, HomeEditor,
OAuthWebView, Screens, Tabs) and zero are inside a popup —
CameraClipsScreen is a full page reached via navigate, not
an overlay screen, and its RecordingFullscreen /
ClipPlayerScreen overlays sit on top of it, not the reverse.
Predictive back DEFERRED (not half-wired): the manifest
flag is not enabled, and enabling it means manifest +
handler-replacement surgery with back-button regression
risk; the spec only requires BackHandler wired, which is
done. If it lands cleanly later, the recommended peek is
0.82 (Koda's video source).

### 4. Launcher icon redesign (folded into 7, user's words)

The user's icon direction, folded into round 7 instead of a
later round, REPLACES round 6's construction outright
(full-bleed Haven leaf green #3E6B2F field + white line-art
house): no green field and no white line art remains in any
icon resource. New construction, from the user's reference
image: a cream/off-white squircle field (the squircle shape
itself comes from the launcher mask — the background layer
is a full-bleed cream fill the mask crops) carrying a house
built from gradient segments, colorful and bright, not line
art. Facets, in the same 30..78 x 30..76 envelope as round 6
(same safe-zone discipline): roof left/right triangles
grading deep leaf #3E6B2F at the eaves to light lime
#9BF67C at the ridge and back; body left/right grading
deep leaf at the outer edges to mid green #66BB6A at the
center; center panel (50..62) grading yellow #FFD54F at
the top to orange #FF8F00 at the bottom; arched door
segment on the LEFT (39..47, arch apex ~61, base 76) in
cream #FAF7F0, the field color, so it reads as a doorway
against every body facet; small yellow #FFD54F 4-point
sparkle centered (71,37), clear of the roof edge (roof edge
at y=37 is x~61.6; sparkle starts x=65). The vectors use
real linear gradients (minSdk 26, so VectorDrawable
gradients, API 24+, are safe — verified by the local build
compiling them, not assumed). Monochrome variant: same
geometry, single color — the facets merge into one black
silhouette, the door is an evenOdd cutout so it still reads
as an opening, the sparkle stays a separate filled star;
the round-6 line-art monochrome is replaced, not recolored.
Adaptive wrappers (mipmap-anydpi-v26/v33) unchanged. All 10
legacy mipmap PNGs regenerated from the new artwork
(`make-legacy-icon-round7.py`: 4x supersample, LANCZOS;
square and round PNGs identical files, the repo
convention); the roof diagonals are approximated as
horizontal gradients in the PNGs (the adaptive vector
carries the true diagonals). Pixel-verified on all
densities: corners cream (250,247,240); roof apex lime;
center panel yellow-to-orange top-to-bottom; door cream;
sparkle yellow; body facets green blends; door-arch and
sparkle-roof gaps cream.
Gradient direction, deliberate: outer edges are deep
green, the interior grades toward lime/yellow — so the
house boundary on cream is crisp everywhere (deep leaf on
cream 5.86:1), while the light-lime facet never carries an
outer edge against cream (light lime on cream 1.24:1, a
soft edge by design — the light facet is an interior
highlight bounded by darker facets). Safe zone: roof eaves
~24.1dp out, apex 24dp, body corners ~28.4dp, sparkle
extremes ~28.6dp — all outermost ink under ~30.7dp from
center (54,54), so circle, squircle, square, rectangle,
and expressive masks (including Samsung's default shape)
never clip critical content. RECENTS, the stated
acceptance case, reasoned explicitly: round 6 proved the
Fold8 recents pane can blacken the BACKGROUND layer while
the foreground survives. If this cream field blackens, the
bright foreground survives — lime on black 15.87:1,
yellow on black 14.88:1, mid green on black 8.88:1, cream
doorway on black 19.63:1 — and the softest element, deep
leaf on black (3.35:1), is bounded by bright neighbors on
both sides, so the silhouette survives. This inverts round
5's failure mode (a dark mark vanishing into a blackened
light field) instead of repeating it: blackening the field
IMPROVES bright-mark contrast. The one element that goes
soft on black (deep leaf) never carries the silhouette
alone. No app-name or theme changes (per the addition's
scope guard). Files: the three drawable vectors + all 10
mipmap PNGs; no Kotlin changes for this item.

### Reduce-motion, and what was not done

Under reduce-motion the drag never engages (Back/dismiss
snap as today). Deliberate new behavior, stated: the
existing idiom never disabled a gesture, but a scrubbed
morph under reduce-motion would be animation by another
name. WebRTC stays gated off (`webrtcEnabled = false`,
verified). No negative padding in changed files (all drag
progress clamped to [0,1] before geometry math). Not in
this round: trigger-button identification (still awaiting
his answer), video-26 recording-button musing
(inconclusive), garage-door actuation (deliberately
untested), camera fullscreen SheetPageContent drag
(different mechanism), M3 sheets' built-in swipe-to-dismiss
(DeviceSheet, CardSettings), the queued round-8 thermostat
radial/number work, and the 62°/72° screenshot note (ends
read 62°/72° where the round-6 formula gives 50/80 —
unconfirmed whether a Settings override he set while
testing, expected if so, or a range bug; investigate if
not). No live device state touched (no setpoint writes).

### Residuals (device-test items, not code gaps)

- Source-rect precedence stays inconsistent BY DESIGN
(sheet/popout read the live map first, detail/panelDetail
the captured rect first — the base page relayout
overwrites the map mid-flight). A sheet/popout scrub could
land on a different rect than open if the card moved
mid-drag — rare (requires base relayout + recomposition
mid-drag + a moved card).
- The drag range is recomputed per composition from the
current source rect; a card moving mid-drag shifts the
mapping slightly (same rarity as above).
- The nested-vertical LazyColumn case (camera detail clips
list inside a popup scroll inside the drag connection) is
handled by protocol order but untested on device, as is
every drag behavior in this round.
- The generic Dial consume fix also fixes the existing
scroll-during-dial-drag quirk; the thermostat dial's
off-thumb fall-through remains framework arbitration, not
app-code proof (same caveat as the rebase findings).
- Test note: the travel-rule boundary assertion uses exact
float values (travel 1.0 vs threshold 40/40) — a 0.96f-style
boundary is not exact in float32 (1f − 0.96f lands a hair
above 0.04f) and failed the first run; the implementation
was correct, the test constant was not.

## Feedback round 6 (2026-11-03, screenshots + icon example)

Three items against the feedback-round-5 build (`c71572c3`, CI
Build APKs run #113 green). Baseline proven green first
(339 tests, 0 failures — confirming round 5's expected total,
and the sandbox Gradle daemon works again after round 5's IPC
breakage). Final: 349 tests, 0 failures (339 + 10 new:
thermostat axis acceptance/clamp/decade cases + 5 Settings
resolver cases). Local APK built this round (daemon healthy).

### 1. Camera letterboxing (Living Room + Garage)

Root cause, verified in code: the stretch lived in the HLS
path only. `CameraMjpegBody` already renders every stage with
`ContentScale.Fit` and cannot stretch. Stream-capable non-Ring
cameras (the Tapo cameras, when they advertise the
`SUPPORT_STREAM` bit) take the HLS attempt chain into
`CameraHlsPlayerBody`, which rendered on a bare `SurfaceView`
at `fillMaxSize()`; MediaCodec's `SCALE_TO_FIT` — the player
default, which `createHlsPlayer` leaves unset — means "scaled
to the surface dimensions", not "fit inside with bars", and
AOSP requires the surface itself to be positioned at the
proper display aspect ratio. The surface was the whole screen,
so a 16:9 Tapo frame was pulled vertically to fill it. Fix
sizes the surface itself, the way `PlayerView` does:
`onVideoSizeChanged` (width × pixel aspect / height) drives a
centered `BoxWithConstraints` rect of the video aspect inside
the black fullscreen `Box`, which becomes the top/bottom
bars; the scaling mode is pinned to `SCALE_TO_FIT` only as
belt-and-braces (the doc comment says explicitly that the
mode line is NOT the letterboxing mechanism, so a future
cleanup does not delete the sizing and reintroduce the
stretch). Covers both entry points (`CameraFullscreen` and
the centered live-feed dialog funnel through
`CameraLiveBody`). Untouched: MJPEG body, Outdoor path, Ring
recording path/labels, hero quadrants, WebRTC (still gated
off). Coordinator fix during integration: the scaling-mode
constant lives on `androidx.media3.common.C`, not `Player`
(one-line fix, first integration build failed to compile
on it). Residual: which transport each Tapo camera negotiates
at runtime cannot be proven from code — if a camera's HLS
request fails it falls terminally to the already-letterboxed
MJPEG body; rotated-stream metadata (90°/270°) is not
specially handled (out of scope).

### 2. Thermostat arc — geometry + axis function + Settings override

(a) Axis function: `thermostatDisplayRange` no longer uses
the flat ±10°F bracket (65/67 read 55°/77°). Approved
formula, implemented literally: lowAxis = max(40,
floor(low/10)*10 − 10), highAxis = min(100,
ceil(high/10)*10 + 10). Acceptance: 66/67 reads 50°/80°.
Clamps are hard: axis never below 40 or above 100, extremes
only when the formula would otherwise exceed them. Step size
(arc degrees per °F) follows from the axis. Applies
everywhere the display range is used (popout, sheet, detail
via the popout). Judgment call: the formula is NOT
intersected with the HA min/max range (round 2 intersected);
the axis is exactly what the formula says, which matches the
approved spec literally and is identical for typical Ecobee
HA ranges (45–95); it diverges only if an HA range is
narrower than the formula window. Guards kept: degenerate HA
range passes through; HA range entirely outside [40,100]
(Celsius) keeps its HA range; empty setpoints keep the
HA∩[40,100] fallback.

(b) Arc geometry: sweep 240° → 270° (start 150° → 135°,
end 45°; gap 90° centered at the bottom), chosen over
280°/300° to keep a clear bottom gap for the inside-tap zone
and gap-strain logic; a 2°F pair now spans exactly 18° of
arc, the dual-thumb separation floor. Track inset 20dp →
16dp in `trackRadiusPx` (smallest safe value: grabbed stroke
outer edge 5dp inside the canvas, pill rect 1dp inside at
the 180° west point; 14dp would clip). All arc consumers
flow through the constants (drag mapping, thumbs, segments);
the drag handler's gap-strain block was the one hardcoded
consumer (literal 30°/120°) and is now derived from the
constants. End labels are the `SpaceBetween` row below the
dial, now sitting directly under the lower arc ends, clear
of the steppers (still below the disc/dial block per round
5). Scallop background untouched. `dualNumberGapPx`
follow-through: its 20dp assumption lived at the
`ThermostatDialBlock` call site (`diameter − 56dp`), updated
to `diameter − 48dp` (= 2×(16dp inset + 8dp half-stroke));
the popout pair now clears the ring by ~7dp total instead of
kissing it.

(c) Settings override (user refinement, addition not
reversal): the formula is the AUTOMATIC default, and
Settings gains explicit min/max controls that override it.
New "Thermostat" section in `SettingsScreen` (after
"Shapes", before "Browse"), reading `vm.settingsStore`
directly (signature unchanged). Prefs
`thermostat_display_min` / `thermostat_display_max`
(presence = set; null = Automatic). One shared resolver,
`resolveThermostatDisplayRange`, used by all three surfaces:
both values set → clamped to [40,100] and pinned; clamping
to min ≥ max → formula governs (never inverted, never a
crash); either unset → formula governs. Settings rows:
"Dial range" status, "Display minimum" / "Display maximum"
(value or "Automatic", −/+ steppers, 1°F, clamped [40,100],
first tap on an unset value starts at 50°/80°), "Reset to
Automatic" clearing both. Status text always names the
effective state ("Override (50–80°) — pinned…" vs
"Automatic (50–80° from setpoints) — …", with explicit
fallback lines for invalid and half-set states). Display
range only — no setpoint writes. Note: popout/sheet read
the prefs at composition, so an override takes effect when
the thermostat surface next composes, not live while a dial
is already open. `ThermostatHero` has no live call sites; a
future caller must pass the resolver's result. Hard rules
kept throughout: visual changes never rewrite setpoints, 1°F
snap, 32dp handle-grab-only nearest-wins, inside-tap
toggles heat↔cool, clamps/gap-proofing, round-5 number gap.
Acceptance: explicit 50/80 pins the axis at 50/80 regardless
of setpoints; clearing returns to formula behavior.

### 3. Launcher icon — Haven green line art

Dark-render finding, verified from the actual round-5
resources: the device path breaks the BACKGROUND layer —
the round-5 comments record the Fold8 recents pane
blackening the solid lime `#9BF67C` field (One UI forced
dark icons / Android auto-theming) while the foreground
rendered intact. The round-5 fix (bright off-white tile +
charcoal house) kept the launcher grid legible but did not
cure the unfolded recents pane, the acceptance case, because
the construction still depended on a light field carrying a
near-black (`#161D17`) mark: blacken the field and the dark
mark vanishes into it. Round 6 removes the failure mode:
no tile/plate and no dark mark anywhere. Field: full-bleed
Haven leaf green `#3E6B2F` (light-theme primary in
`Theme.kt`, round-5 spark color). Mark: WHITE line art only
— roof `M30,52 L54,30 L78,52`, body rect outline, arch-top
door opening; 4.5dp strokes, round caps/joins, no fills; the
filled leaf spark is dropped (it does not translate to
stroke at 48dp). Contrast (WCAG): white on `#3E6B2F` =
6.27:1 (AA pass); white on pale lime `#9BF67C` = 1.32:1
(rejected, as briefed); `#1B3708` forest = 13.15:1 but
rejected because at icon sizes it reads as near-black,
defeating the green identity. Safe zone: outermost ink
under ~30.7dp from center 54,54 (radius 33), so all standard
masks survive; even if a device path blackens the field
outright, white-on-black still reads (21:1). Monochrome
variant carries the same stroke geometry in a single color
for themed icons. Adaptive wrappers (v26/v33) verified, no
change needed; manifest icon references unchanged. All 10
legacy mipmap PNGs regenerated from the same vector
geometry (4× supersampled, LANCZOS; corners pixel-verified
`#3E6B2F`, roof white, at every density). Device checks
owed: mask rendering on the Fold8 (especially the unfolded
recents pane), small-size legibility, themed-icon
appearance.

### Still open (not in this round)

Front Door wave test (blocked on his device action);
trigger-button identification (awaiting his answer);
garage-door actuation (deliberately untested). Live
thermostat state untouched (his screenshot showed Heat 65° /
Cool 67°; no setpoint writes in this round).


## Feedback round 5 (2026-11-03, videos new1 + new2 + new3)

Three narrated videos against the feedback-round-4 build
(`6cebaa52`, CI Build APKs run #111 green). 339 unit tests, 0
failures (336 baseline + 3 new: two dual-number-gap cases and
one popup-accent gating case). Fix-up: CI #112 failed to
compile — the new `pageAccent` / `pageBadge` parameters had
been appended after `EntityControlCard`'s `onDetail` lambda,
breaking Browse/Dashboards trailing-lambda call sites (same
class as the round's `PillAction` reorder); `onDetail` moved
last and all other round-5 signatures were audited for the
same pattern (only `EntityControlCard` was affected). NOTE: no local APK this round
— the sandbox's Gradle daemon IPC broke after the baseline
(a fresh Gradle user home and a trivial `help` task fail the
same way: the client cannot read a valid message from its own
daemon), so local builds and local test runs were impossible
and CI is the verifier; the CI haven-debug artifact is the
installable build.

### Item 1 — panel-card tap opens the detail directly (video new1)

Tapping a card inside a Home Status panel (e.g. the Lights
panel) used to switch the base page to a full-width panel page
behind the detail popup — a middleman screen the user never
asked for. The detail now opens as a stacked overlay directly
over the panel popup: a backstack-independent `PanelDetailAnim`
layer in `HavenApp.kt`, sourced from the tapped panel card's
open-captured rect (live bounds map as fallback), with back
returning to the panel popup, never to Home. Round 4's
nested-panel ghosting fixes stay intact (base renders
instantly while an overlay is open; the traveler stays
suppressed under overlays; the panel-detail layer reports no
traveler targets of its own, so it cannot clobber the panel's).
Wide postures get the centered popup, folded gets the panel
overlay; climate via this layer renders the same
`ThermostatPopout` as the detail path.

### Item 2 — detail toggle track takes the preset pastel (video new1)

Round 4 set the preset detail toggle track to the preset badge
background (dark). The user corrected himself on video: it is
the BACKGROUND that changes, not the knob. The detail toggle
track now uses the preset's pastel container (Standing Lamp
reads yellow #FFE082, matching its card; the knob stays
white) via the new `detailPageAccent` helper, which supersedes
round 4's judgment call for the toggle. Non-preset detail
toggles are unchanged.

### Item 3 — card text can no longer clip at card edges (video new2)

On the Home Status page the thermostat card's "Cooling to 67°"
lost its descender and the Garage Door card's "Closed" was cut
in half at the card's bottom edge. Root cause: the fixed
136dp card left ≤6dp of slack on the filmed variants (48dp
garage door visual, 26sp climate trailing temperature, title
+ subtitle line heights), so any font-scale growth overflowed
the Surface clip. The fix is a real layout buffer, not an
overlay: 136dp is now a FLOOR (`heightIn(min = 136.dp)`), so
standard cards stay exactly 136dp and overflowing content
grows its card instead of clipping — title + subtitle fully
fit on every variant. The change is in `HomeRoomCard` itself,
so it covers all three surfaces that render it (Home grid,
Home Status panels, Favorites); the polygon path keeps its
round-2 spacer/padding shift. A grown card makes its grid row
taller; its row-mate stays 136dp top-aligned. Search rows use
`EntityTile`, a different component with no fixed height, and
are unaffected.

### Item 4 — tile color themes the whole popup page (video new3)

Changing a tile's color used to change only the tile. The
stored tile choice now drives the popup page's key elements
for every entity type, through `detailPageAccent` threaded
from the detail content and the sheet card funnels (optional
params defaulting to current behavior, so untouched callers
are byte-identical). Themed: cover sheet door fill, Open /
Close / Stop pills and position slider; garage detail slatted
door, 56dp pills and slider; vacuum Start / Pause / Dock /
Locate pills, battery progress and selected fan-speed chip;
media transport central squircle, volume slider and source
pill; switch / fan toggles and sliders; lock pills; climate
sheet slider; thermostat stepper pills (see item 6); Round-3
vacuum and media panel pills, sliders and toggles. Pill
content reads onSurface when an accent is present, else the
previous onSecondaryContainer; disabled pills keep the muted
treatment and 0.38 alpha dim law.

Judgment call, reported prominently: the popup accent is
deliberately UNGATED, diverging from the brief's "keep
presetAppliesForState gating" clause. Both filmed examples
are inactive-state entities — a CLOSED garage door with
cooling blue, a DOCKED vacuum with sage neutral — and a
gated accent would show NOTHING for exactly the two cases
the user filmed ("any time you change the color of a tile…
for ANY entity"). The accent returns the container whenever
a preset is set and null only when none is set, so no color
is ever invented where the user chose none. Card faces keep
the on-state law (a preset card renders inactive gray when
off) and badges keep `detailBadgeOverride`'s gating (badges
signal state); toggle off-states still read gray via the
unchecked branch.

Deliberately left on defaults: the page background stays
scheme.background (chrome, not an element; tinting the page
face risks washing out readability and disturbing morph
fromBackground reconciliation); section headers stay text
colors (pastel accent as text fails contrast); hero state
text keeps hashed accentFor (readability); light hero
kelvin / brightness panels and light sliders / hue dots
stay device colors (they show actual color temperature, not
a theme choice); light status cards stay scheme surfaces;
sensor / binary_sensor semantic tints stay (temperature
coral / humidity sky bypass the matrix by standing law);
generic Turn on / off / Toggle outlined buttons stay
(outlined text; accent text unreadable); media flanking
squircles stay scheme (flanking-vs-central hierarchy);
fan / vacuum unselected chips stay scheme (selected-vs-not
is the signal); Round-3 vacuum tile Clean / Dock pills are a
card surface, not a popup page, and keep the card law; media
panel D-pad buttons are remote.tv's chrome, a different
entity than the player; security / greenhouse / updates
panel actions have no single entity preset to resolve;
thermostat dial mode colors and Mode / Fan segmented bars
keep mode semantics (the header badge is already preset-
themed via `detailBadgeOverride`).

### Item 5 — thermostat numbers get a computed gap (video new3)

The dual setpoint numbers (e.g. Heat 66° / Cool 70°) sat 16dp
apart and crowded each other at rest and during changes. The
gap is now computed from dial geometry
(`dualNumberGapPx` in `ThermostatLogic.kt`, the label
analogue of `separatedDualAngles`): measured label widths
plus the track's inner width (diameter minus twice the 20dp
track inset and the 8dp idle half-stroke) spread the pair to
fill the inner width with 8dp of clearance per side, floored
at an ample 32dp. The 300dp hero dial gets the full spread
(~75dp); the 240dp popout dial cannot fit 32dp plus full
clearance at 52sp (inner width 184dp against ~153dp of
labels), so there the floor wins and the pair kisses the
ring's inner edge by ~2dp of label box per side — the drawn
glyphs stay clear on their side bearings. VISUAL ONLY, like
the thumb separation: setpoints are never rewritten,
clamped, or nudged; only label positions move.

### Item 6 — steppers move below the dial; no text on the radial (video new3)

The - / + steppers sat inside the dial face, on the radial.
They now render as their own row BELOW the dial on both
surfaces that share `ThermostatDialBlock`: under the scallop
disc in the popout (16dp of daylight) and under the dial
block in the hero. The scalloped card background is kept
exactly as the user likes it; only the layout around it
moved. With the steppers out, the centered overlay holds
only the numbers + Indoor label, so no text overlaps the
radial at rest. Residual: the kinetic number animation's
outgoing text scales to 1.15x while sliding vertically away
during a value change, which can transiently touch the ring
mid-change; at rest the pair is clear (see item 5).

### Standing law re-verified

WebRTC stays gated off (`webrtcEnabled = false`); no
negative padding in any changed file (progress clamped to
[0,1] before geometry math); thermostat stays 1°F,
handle-grab-only, inside-tap toggles heat / cool, clamps +
gap-proofing, onFinished reports the actually-set value;
nav stays Home / Scenes / Energy / Settings; pill tokens
unchanged.

## Feedback round 4 (2026-11-03, videos df1 + df2)

Two narrated videos against the feedback-round-3 build
(`da9364a8`, CI Build APKs run #110 green). 336 unit tests, 0
failures (334 baseline + 2 new: a light-theme glyph-equals-
container case and a detail-override gating case). Local APK:
`haven-feedback-round4-debug.apk`.

### Item 1 — unfolded root restructure: the rail clipping ledge (video df1)

Round 3's zIndex-only fix was insufficient, and this round
names why: zIndex changes draw order, not size. The Scaffold
itself sat beside the rail in a horizontal Row, so its content
area ended at the rail boundary and sliding pages vanished at
that line instead of reaching the physical bezel. The root is
now a single full-screen `Box` (`HavenApp.kt`): the Scaffold
spans the full width (`Modifier.fillMaxSize()`) with 84dp start
content padding, and the `VerticalSidePill` floats on top at
`Alignment.CenterStart` (`zIndex(1f)` + status/nav-bars
padding). At rest, content still sits right of the rail — the
padding defines where tab content draws, and resting geometry
is otherwise unchanged. During tab transitions, pages slide
all the way to the physical bezel underneath the floating
rail, with only the rail's own shape obscuring them. The
`AnimatedContent` transition container is explicitly
full-size, and the round-3 tab travel (full off-screen both
directions, +84dp rail over-travel on unfolded leftward) is
unchanged. Folded is untouched (single full-size Scaffold, no
rail).

### Item 2 — custom tile presets into detail surfaces (video df2)

A Lamp card set to warm amber/yellow rendered #FFE082 +
bronze badge/switch on the card, but its detail panel reverted
the top-left header polygon badge and the large
`VerticalPillToggle` to default forest green (#1B3708). This
round explicitly overrides round 2's decision to leave
detail/sheet header badges untouched (that exclusion was
based on morph-traveler color-jump risk; since card, traveler,
and header now share one preset resolution, there is no jump
to risk — the traveler computes the same pair internally from
the open-time snapshot). New pure helper
`detailBadgeOverride` (`data/CardCustomization.kt`): preset
pair via `presetBadgeColors`, gated by the round-2
`presetAppliesForState` active rules (inactive gray otherwise;
null when no preset is set). Wired into `DomainBadge` and
`TravelingBadge` as an optional `badgeOverride` pair
(`ui/components/Expressive.kt`), and into every detail
surface: `DetailPageScreen`/`DetailPageContent` (header badge,
52dp hero badge, and the light toggle, whose track is now the
preset badge bg — the card's switch track — with the white
knob unchanged), `EntityPopupSheetContent` (sheet header
badge), and `ThermostatPopout` (climate header badge, covering
both the detail-climate path and the popout path). The
traveler (`TravelingHeader`) resolves the same pair from the
snapshot, so traveler and destination header coincide exactly
at the 2% handoff.

### Item 3 — badge glyph + switch thumb = pastel container on active cards (video df2)

SUPERSEDES the rewrite matrix's active-light #FFFFFF glyph
rule for active states. On active cards the lightbulb inside
the dark polygon badge rendered pure white; it is now the
card's pastel container color — #C8F5A6 for green, #FFE082
for yellow, the same color as the switch thumb — while the
badge polygon background and switch track stay deep tonal ink
(#1B3708 / #261900). Changes: `MatrixRow.toCardColors`
(`data/SemanticColors.kt`) — light active glyph =
`lightContainer` (dark unchanged: a dark container glyph
would fail on the light badge); `presetBadgeColors`
(`data/CardCustomization.kt`) — light glyph = the preset's
pastel container (dark keeps the 0.72-lightness bright tint,
for the same reason). The switch thumb already used the card
color (`domainSwitchColors` checked thumb = container;
`presetSwitchColors` checked thumb = container) — verified,
not changed; only the glyph moved. Every affected pair still
clears 4.5:1 (pastel on deep ink, roughly 12–14:1), pinned by
`DomainCardColorsTest` (new glyph-equals-container case) and
`CardCustomizationTest` (updated preset-badge case). Scope
note: the dynamic-scheme `badgeColors` path (non-preset detail
headers/heroes, which use scheme.primary tokens rather than
the matrix) is a separate token system and is untouched; item
3 governs the matrix + preset systems (cards everywhere, and
preset details via item 2's override).

### Item 4 — nested-panel ghosting + exit flash (video df2)

Opening/closing Lamp or Standing Lamp from the Lights-on
sub-panel bled the Home dashboard through semi-transparent
morph layers, left a stray green badge floating on Home, and
flashed one frame landing back on the Lights grid. The
mechanism, verified in code (not assumed): (a) the panel
itself renders as a morph overlay on top of Home, so when a
detail opens from it, the base page switches Home → panel
page — and that base change used to animate (slide + fade)
UNDERNEATH the detail morph, letting Home show at the edges
through the semi-transparent morph; (b) the base page's cards
overwrite the shared card/hero bounds maps with base-page
rects, so the detail morph's live-read source and the
traveler's origin tracked the base page, not the tapped panel
card — the traveler landed on a position matching no visible
card. Fixes (`HavenApp.kt`): while any detail/panel overlay is
open, the base page renders instantly with no
`AnimatedContent` transition — the intermediate panel stays
opaque and stationary underneath the top-most morph (tab
switches always run with no overlay open, since `goTop`
clears overlays atomically first, so they keep the slide);
the detail morph source is the rect captured at open, not the
live map (the map stays the fallback for never-reported
cards); and `TravelingHeader` is suppressed when the detail
opened from a sub-panel or another detail (previous backstack
entry is an overlay screen), with the destination header badge
rendering from frame 0 via a new `LocalSuppressTraveler`
composition local (read by `DetailPopupHeader` and the
thermostat header badge) instead of hiding for a traveler
that never flies. The sheet traveler is unchanged: sheets
don't move the backstack, so a sheet from a panel card flies
from the correct panel position. Known residual, stated
plainly: the panel popup still plays its own enter morph on
return from a detail (from its sourceless fallback, since
panels carry no source card) — but it now plays over an
identical opaque panel page, so no Home bleed and no flash;
only the settle animation itself remains.

### Item 5 — morphs locked to RenderThread

Audit of `MorphSurface` (`ui/components/MorphOverlay.kt`),
`TravelingHeader`, and `TravelingArc` (`HavenApp.kt`), plus
`PanelOverlay` and `CenteredPopup`: all three morph
components were already clean (Round 6's graphicsLayer-only
rule held — nothing regressed since). `MorphSurface` lays out
once at the fixed target size and moves only via
`graphicsLayer` (translation/scale/shape/clip) plus a
`drawBehind` background blend; `TravelingHeader` and
`TravelingArc` lay out once at the static target rect and move
only via `graphicsLayer` from the progress lambda; `PanelOverlay`
and `CenteredPopup` measure once from fixed constraints and
enter via `graphicsLayer` only (scrim alpha also
`graphicsLayer`). No layout/size/padding animation exists on
any morph path, so no code changed here. One observation,
out of scope (a control animation, not a morph): the
`VerticalPillToggle` knob still travels via clamped padding
(the recurring crash site, already bounded to ≥0dp); moving
it to `graphicsLayer` translation would change a pressable
control's behavior and was not attempted in this round.

## Follow-up round (2026-11-03, videos 71 + 63 — folded into the feedback round)

Two narrated follow-up videos arrived minutes after the feedback
round's push (commit `9c2358ad`, CI Build APKs run #107 green) and
were folded into the SAME round before building — no separate
round, one commit on top of `9c2358ad`. 322 unit tests, 0
failures (318 baseline + 4 new `TabSlideTest` cases). Local APK:
`haven-followup-debug.apk`.

### Follow-up A — video-71 (unfolded, ~68s)

1. **Rail/nav never disappear during panels.** The demo (Front
   Door 69A1 Mute detail, Home Status "Lights on" panel) was
   recorded on the pre-feedback build, but the feedback round's
   rail fix left one residual gap: `unfoldedBottomClearance`
   (20dp + nav-bars inset) was gated on `isTopLevel`, so the
   underlying page — VISIBLE behind the unfolded centered popup
   — jumped 20dp on panel open and back on close. The gate is
   now wide-only, not top-level: the page never moves on panel
   open/close, and exiting restores the exact prior view with no
   pop-back, no displacement, no rail re-entry animation (the
   rail stays mounted throughout — it remounts only on
   fold/unfold). The folded clearance gate is unchanged: folded
   panels are opaque full-screen, so the shift underneath is
   invisible. Boundaries, unchanged: the camera dialog (incl.
   the Front Door panel) is a full-screen opaque overlay and
   covers the rail while open — the video-surface case the
   feedback round kept full-screen; rail taps during a centered
   popup hit the scrim and dismiss the popup (visible and
   stationary were the requirements, not interactivity while a
   popup is open). Rail scrub semantics untouched (the video's
   tail drew no complaint).
2. **Home Status panel shows the real home cards.**
   `HomeStatusTile` is deleted; the panel renders actual
   `HomeRoomCard`s (the home page's card: 136dp, 44dp badge,
   16sp bold title, 13sp subtitle, shape rotation, custom
   title/icon/color/shape, preset-driven badge tints) in a
   left-aligned flow underneath the panel header — each card
   (maxWidth − 12dp) / 2 wide with 12dp spacing, so a lone card
   hugs the left edge and two cards sit side-by-side butted
   against the side. The 50/50 split is relaxed per his words
   ("maybe not even split down the middle... left oriented and
   just butted up against the side"); full two-card rows
   necessarily measure 50/50, but the home card design is the
   rule, not the split. Tap → detail exactly as the old rows
   did, via a new optional `onOpenPanel` override on
   `HomeRoomCard` (default null keeps every existing call site
   byte-identical): without it, a sheet opened from inside a
   panel would paint UNDER the opaque detail panel and read as
   a dead tap. Badge-zone taps (toggle/toggleCover) and the
   switch stay live as on the home page.

### Follow-up B — video-63 (Koda's pill bar as the tab reference)

Koda's values were measured from the Koda repo source
(`podcasts` branch, head `12c47f64`), not from video eyeballing
— and from the M3 library sources Koda builds against for the
component defaults Koda does not override. Koda is the reference
only; no Koda code changed.

Measured Koda values (folded `HorizontalFloatingToolbar` item,
HomeScreen.kt; rail, HomeNavigationRail.kt; transition,
HomeScreen.kt `AnimatedContent`):
- Bar container: M3 default — fully round (`CornerFull`), 64dp
  tall, 8dp edge padding, 4dp item gap. Placement: BottomCenter
  + navigationBarsPadding + 20dp bottom padding; hides on scroll
  (`exitAlwaysScrollBehavior`).
- Bar item: 48dp tall, `CircleShape`, horizontal padding
  animated 12dp (unselected) ↔ 20dp (selected) on
  `fastSpatialSpec`, 24dp icon (filled when selected, outlined
  otherwise), label `labelLarge` Bold with an 8dp icon–label gap,
  label `AnimatedVisibility` fade + expand/shrink horizontally
  on `fastSpatialSpec`, container/content colors animated on
  tween 200.
- Rail item (`VerticalFloatingToolbar`): 72dp toolbar held 12dp
  off the edge (84dp total — exactly Haven's rail width);
  56dp-wide items, 28dp corners, 16dp vertical padding, 24dp
  icon, label below the icon ONLY when selected (fade +
  expand/shrink vertically, `fastSpatialSpec`, 4dp gap),
  colors tween 200; never hides on scroll.
- Page transition: directional by `visualTabOrder` rank (music
  mode: AI DJ, Home, Explore, Library — AI DJ left of Home).
  Forward: new page enters full-width from the right + fade,
  old leaves one-third to the left + fade. Backward: new page
  enters one-third from the left + fade, old leaves full-width
  to the right + fade. No explicit animationSpec in either
  branch (AnimatedContent defaults).
- Bar spring: Koda's theme sets
  `motionScheme = MotionScheme.expressive()` on material3
  1.5.0-alpha29 (where MotionScheme is public), so its
  `fastSpatialSpec()` is the EXPRESSIVE fast spatial spring:
  damping 0.6, stiffness 800 (vs the standard scheme's 0.9 /
  1400, and vs Haven's house `springSpatial` at LowBouncy 0.75 /
  MediumLow 400). Haven stays on material3 1.4.0 STABLE, where
  MotionScheme is internal — the first build failed on exactly
  that — so the numbers are pinned as the local `kodaBarSpec()`
  token instead of a theme scheme: identical motion, no
  dependency move.

Ported to Haven (tabs only — details keep the unfolded centered
popup / folded full-screen morph):
- Bar spring pinned as a local token `kodaBarSpec()` (damping
  0.6, stiffness 800 — see the "Bar spring" note below for why
  it is a token, not a theme scheme). No other animation moves:
  Haven previously read motionScheme nowhere, and the token is
  used only at the bar call sites.
- Folded `PillNav`: one item composable (no selected/unselected
  branch swap) — padding animates 12dp ↔ 20dp on
  `kodaBarSpec`, label fades + expands/shrinks horizontally
  on `kodaBarSpec` with `animateContentSize` on the item
  row, capsule/glyph colors cross-fade on tween 200. Resting
  geometry otherwise unchanged (44dp item, 22dp icon, 8dp gap,
  14sp bold label, #9BF67C capsule on #161D17, unselected
  #D7E2D4) except selected padding 16dp → 20dp (Koda's value).
  Haven's container radii (32dp Floating / 26dp Compact),
  heights (64dp / 52dp), row edge padding (18dp / 12dp), and 4dp
  item gap are kept: they are Haven's own Floating/Compact
  style distinction, and Haven's container/item radius
  relationship already matches Koda's (large container radius +
  fully-round item pills). The Floating/Compact setting still
  visibly changes bar height and padding.
- Unfolded rail: icon glyph colors cross-fade on tween 200
  (Koda's rail color animation); the selection indicator glide
  moves to `kodaBarSpec` (Koda has no sliding indicator, so
  Haven keeps its indicator and takes Koda's spec for it). The
  rail stays icon-only (Round 8 law): Koda's labeled 28dp-corner
  rail items are NOT copied — on an icon-only rail a 28dp radius
  on a 32dp-tall pill is a full round anyway, and the labels
  were removed by directive. Rail scrub semantics untouched.
- Tab page transition: `AnimatedContent` (`topLevelScreen`)
  now slides directionally by bar rank (Home 0, Scenes 1,
  Energy 2, Settings 3) — forward/backward exactly as Koda's
  branches, defaults for the spec, non-tab screens keep Round
  4's fixed slide, reduce-motion stays instant. Direction logic
  is the pure, unit-tested `tabSlideDirection` (4 new tests).
- Collision rule, unchanged and reported: a tab tap while a
  panel is open clears the panel INSTANTLY via `goTop`
  (pre-existing), then the directional slide runs — no
  panel/tab animation collision, because the panel is gone
  before the slide starts.

Not device-verified for this follow-up: install/launch; rail
staying visible + stationary on unfolded panel open/close for
both video-71 demo cases; Home Status real cards left-oriented;
Koda-style bar padding/radii/bounce on the folded bar and the
rail; directional tab slides in both directions; plus the
standing front-door wave test.

## Feedback backlog round (2026-11-03, from the 13:08 videos)

Eight items from Garrett's 13:08 feedback videos (transcripts
`video-1a.txt` + `video-55.txt`), built on the pushed tree
(`bef61a5e`, CI Build APKs run #106 green) and pushed through
GitHub + GitHub Actions. 318 unit tests, 0 failures (313 baseline
+ 5 new). Local APK: `haven-feedback-debug.apk` (67,089,289 bytes).

1. **Icon dark rendering — investigated, then redesigned.** The
   customization round's icon verified CORRECT in the built APK
   (compiled adaptive XML wiring, gradient background drawable,
   legacy PNG pixels all lime) — yet Garrett's Fold8 recents pane
   rendered a black house on a black background. Pixel sampling of
   his screenshot shows the foreground intact (charcoal house,
   white door, green spark) over a black field: the device path
   blackened the background layer, consistent with system
   dark-icon treatment (One UI 8.5 forced dark icons / Android 16
   QPR2 auto-theming). The exact Samsung code path cannot be
   proven from the build VM, so the redesign removes the variable
   instead of guessing at it: background is now SOLID capsule lime
   `#9BF67C` (no gradient vector), and the foreground is
   SELF-SUFFICIENT — a bright off-white `#FAF7F0` tile (52×52,
   radius 16) carrying the charcoal house, a LIME `#9BF67C` door
   (an off-white door would merge with the tile), and the
   leaf-green spark. The mark stays inside the 66dp safe zone
   (base corners ~21.3dp, apex 14dp, spark tips ~29.7dp from
   center); the tile corners (~36.8dp) may nick under a circle
   mask, but the tile is a plate, not critical content. Result
   survives all three renders: normal (lime field + white tile),
   black-background (black field + white tile), and themed
   (monochrome house+spark silhouette, updated to the new
   geometry). Legacy PNGs regenerated to match. Verified from the
   built APK's compiled drawables, not just the sources.
2. **Unfolded pill choppiness — root cause + fix.** The unfolded
   rail was gated on `isTopLevel(current)`, so tapping a card
   unmounted the 84dp rail (current stopped being top-level), the
   Scaffold snapped 84dp wider, and the rail slid back in on
   close — the "pushes to the left and then has like a choppy
   return". The rail now mounts whenever wide, regardless of
   top-level. A mounted rail during panels is covered, never
   interactive (panel overlays are later siblings above the Row),
   and renders unhighlighted off-tab (`selectedNavIndex` null →
   `-1`). The folded floating pill is untouched.
3. **Unfolded centered popups (folded keeps full-screen morph).**
   This deliberately reverses the both-postures full-screen law
   for unfolded/tablet only. New `CenteredPopup` (HavenApp.kt):
   centered rounded-corner surface (88% of the overlay, capped at
   560dp wide, 28dp corners, 16dp shadow) over a dim scrim (black
   0.45 × progress, tap dismisses — a stated behavior addition,
   since full-screen panels have no scrim), entering FROM the
   pressed tile's direction via pure `centeredPopupEnter`
   (MorphOverlay.kt, unit-tested): offset = (1−progress) × 96dp
   along the tile→popup-center unit vector, scale 0.92→1, alpha
   0→1, progress clamped — so the close exits back toward the
   tile automatically. No TravelingHeader/Arc on this path; two
   badge guards replace them: `LocalMorphProgress` pinned to 1f
   inside the popup (destination headers render fully visible
   from frame 0) and `LocalHiddenHeroId` nulled on wide while a
   modal is open (the source card's badge never hides behind the
   scrim, since the page behind stays visible). Sheet callers
   keep `onPanelPlaced` so the sheet→detail handoff
   (`vm.pageOrigin`) works. The camera panel keeps PanelOverlay
   on both postures — a video surface needs the full area; the
   complaint was about info pages. ModalBottomSheet dialogs
   (Favorites, shape picker, card settings, device sheet) were
   already dialog surfaces and are out of scope.
4. **Camera grain (living room + garage Tapo).** Haven decoded the
   MJPEG proxy at full resolution — no downsampling in the app —
   so the grain is the SOURCE (HA ffmpeg-transcoding the Tapo
   RTSP feeds to MJPEG), not Haven. The HA app is sharper because
   it plays the native H.264 stream. New `CameraLiveBody`
   (HavenApp.kt): stream-capable non-Ring cameras (the
   SUPPORT_STREAM bit, HA's frontend gate) now try HLS first via
   the existing `HaClient.requestHlsUrl` + `createHlsPlayer`
   (Bearer-authed ExoPlayer on a SurfaceView, no media3-ui
   dependency; 8s timeout), falling back to the MJPEG body
   TERMINALLY on any failure, timeout, or player error (no retry
   loop). Ring cameras, non-stream cameras, and unknown entities
   keep the MJPEG body byte for byte. Wired into the fullscreen
   viewer and the hero panel body; the Front Door panel's bodies
   keep `CameraMjpegBody` directly and its liveness rules are
   untouched. Hero quadrants keep snapshots (still thumbnails).
   Before/after per camera: Tapo living room + garage — MJPEG
   proxy → HLS native stream with MJPEG fallback; outdoor Tapo —
   same route (was already clear, now also HLS-first); Ring/front
   door — unchanged, MJPEG recording path + 69a1 live rules intact.
5. **Custom tile color drives badge/icon tints (explicit override
   of the customization round's matrix-driven badge rule, when a
   preset is set; unset cards keep the matrix).** New pure
   `presetBadgeColors` (CardCustomization.kt, unit-tested):
   badgeBg = the preset hue darkened to near-black (HSV value
   0.13, saturation ≥ 0.55); badgeGlyph = the preset hue at full
   saturation, 0.72 HSL lightness — the bright tint of the
   assigned color (yellow tile → yellow-tinted almost-black badge
   + amber/gold glyph, exactly as described). A pure vivid HSV
   glyph was tried first and REJECTED: vivid blue/purple are
   perceptually dark and fail 4.5:1 on near-black; the
   0.72-lightness tint clears 4.5:1 for all 8 presets in both
   themes. Applied to HomeRoomCard, HomeRoomMediaCard, EntityTile,
   and the new Home Status tiles. Switch tracks stay
   matrix-driven (he pointed at badges/icons, not switches).
6. **Search-by-device, not a separate add page.** The Favorites
   sheet's standalone "Add devices" list is REMOVED; the Devices
   section (search + tap-to-expand dropdown) is the only add
   path, with add/remove/reorder behavior identical. Entities
   with no registry device link keep an add path as ONE
   "Ungrouped entities" expandable row in the same pattern (not
   a separate page) — otherwise removing the list would silently
   strand them, which the no-silent-removals law forbids.
7. **Home Status panel as 50/50 tiles.** `HomeStatusPanelScreen`
   renders entities as 136dp tile cards in a 2-column grid (new
   `HomeStatusTile` in Screens.kt: 44dp badge, 16sp bold title,
   13sp state subtitle, general card shape by index, custom
   title/icon/color/shape honored, preset-driven badge tints),
   tap → detail exactly as the old rows did. Plain chunked Rows,
   not a lazy grid (the panel content already scrolls in
   PanelOverlay's wrapper). The cleared-group all-clear line is
   unchanged. Subtitle is the humanized state text (the rows
   showed the raw state string).
8. **Device null row — mechanism proven, then fixed at three
   layers.** The frame shows "Device null / 18 entities" in the
   Favorites sheet's Devices section. Proven from the code + a
   JVM test against the actual org.json jar: `optString` returns
   "" for JSON null AND for a missing key, so the "null" is a
   literal string arriving as `device_id` from HA's entity
   registry for those 18 entities — not a JSON null. Fixes:
   HaClient's entity-registry parse treats blank or
   case-insensitive "null" device ids as absent;
   `groupEntitiesByDevice` drops them from any source;
   `deviceDisplayName` falls back to "Unknown device" for
   blank/"null" ids (registry name still wins; model/manufacturer
   stay in the row subtitle where both call sites already render
   them, never duplicated into the title); the Favorites sheet
   now uses `deviceDisplayName` (single source of truth, as
   SearchScreen already did). The literal string "Device null"
   can no longer render anywhere. The 18 entities surface under
   the new Ungrouped row (item 6), still addable.

Standing law kept: `webrtcEnabled = false`; nav locked to
Home/Scenes/Energy/Settings with user tabs under Settings →
Browse; active capsule `#9BF67C` on `#161D17`; 14-row domain
matrix except item 5's explicit override; thermostat scallop
containment, 1°F steps, handle-grab-only; folded full-screen
morph; no fake devices, no invented controls, no silent feature
removals. `maybeSeedTabs` remains zero hits.

## Unreleased local rounds — landed 2026-11-02 → 2026-11-03 (base: #105 `5f1fea9f`)

Five rounds were implemented and built locally (no CI, no pushes) against the
#105 tree, then pushed to `main` as a single commit on 2026-11-03. The
per-round local APKs below are the build artifacts of each round; the
pushed tree is the cumulative result. Nothing in the local program is
device-verified except where explicitly noted in "Device-verification
status" at the bottom.

### Overnight program — Stages B–H (2026-11-02 night → 2026-11-03 ~03:48)
- 275 unit tests, 0 failures. Local APK: `haven-debug.apk` (65,892,314 bytes).
- Stage B: pinned sage/lime light theme (warm off-white background, sage
  surfaces, charcoal text, leaf primary, lime primaryContainer); dynamic
  Material You kept optional and OFF by default; dark secondary text floored.
- Stage C: pill-over-headers fix (scaffold reserves the pill zone); nav icon
  picker (11 options), tab hide/show, floating/compact pill styles, nav reset.
- Stage D: Home Status (real entities only, rows expand to entity lists →
  detail); Favorites (`favorites_json`, 2-col folded / 4-col wide,
  FavoritesSheet with stale ids shown as "Unknown entity: <id>").
- Stage E: SemanticColors (21-slot SemanticState, light+dark ≥ 4.5:1 by test);
  nav/room alert dots; badge prefs deliberately not migrated.
- Stage F: Scenes + Energy as top-level pill destinations. Scenes via direct
  `scene.turn_on` with Sent/Failed state, never a fake "active". Energy:
  power-now, per-device breakdown, cumulative sensors, honest "No energy
  data" state, no fake charts.
- Stage G: section-order storage + room presentation overrides (no area-ID
  touches) + reset (favorites/tabs survive).
- Stage H: verification pass. No camera-path lines changed;
  `webrtcEnabled = false` intact.

### Visual-fidelity refactor (2026-11-03 morning)
- 290 unit tests, 0 failures. Local APK:
  `haven-visual-refactor-debug.apk` (66,783,694 bytes).
- Cards: inner sub-box removed, fixed 136dp height, 44dp DomainBadge +
  22dp icon, 16sp Bold title / 13sp subtitle, M3 pill Switch from real
  state, lock unlock-confirm preserved, climate 26sp ExtraBold temp with no
  steppers, wide media rows, corners by stable per-entity hash.
  Temp/humidity keep their coral/sky bypass of the domain matrix.
- Unfolded two-pane Row (left ~42% masthead/greeting/chips/cameras 2×2 16/9;
  right ~58% Favorites + sections, independently scrolling) — reversing the
  earlier unified-column decision by explicit directive. Right grid measured
  (2 cols only if pane ≥ 364dp, else 1 col), 14dp spacing.
- Front Door: recording buttons + diagnostic line removed (segmented control
  is the sole selector); offer timeout cancelled once frames arrive; capped
  auto-restart (up to 3 consecutive post-frame 69a1 errors with a
  "Reconnecting to live view…" status, then truthful recording fallback).
  Known limit: `HaClient.streamCamera` ends silently on EOF-with-frames
  (frozen last frame, no error posted).
- My Home filter hides the No room section; Home Status / Favorites /
  weather / who's-home / Round 3 blocks stay global under any filter.
- Round 3 blocks unbundled into four toggles (security/vacuum/media/
  greenhouse) with tested legacy migration.
- Badge tail gap on panel close fixed with closing-aware
  `morphSourceBadgeAlphaOnClose`; thermostat thumb separation is visual-only
  (`separatedDualAngles`, 18° arc) with `grabAngleOffset` so drags never jump
  values; dial→pill gap 28dp.
- Dashboard made entity-scoped (per-id StateFlow collection); the global
  366-item sort dropped.
- Pill rectangle root cause found: the unfolded rail zone (84dp outer Box)
  sat outside the Scaffold and showed the activity window's white background
  (light parent theme) — a full-height white strip in dark mode. Fixed with
  scheme backgrounds on the outer Row + Scaffold container; pill floats
  Koda-style; unfolded bottom clearance remnant kept so the pill never
  overlaps content; folded scroll-away preserved.

### UI rewrite (2026-11-03 late morning)
- 289 unit tests, 0 failures. Local APK:
  `haven-ui-rewrite-debug.apk` (66,836,320 bytes).
- Nav locked to exactly Home / Scenes / Energy / Settings on the folded pill
  bar and the unfolded rail; starter-tab seeding disabled (`maybeSeedTabs`
  deleted); custom user tabs kept, moved to Settings → Browse (User tabs +
  Manage tabs rows, new UserTabsScreen).
- Active capsule: unified #9BF67C capsule wrapping 22dp icon + 14sp bold
  label, both #0A2005; unselected 22dp outlined #D7E2D4; container #161D17
  in both themes; no blue on the dark rail. Floating 64dp vs Compact 52dp
  bar heights now visibly wired (folded bar only; rail icons stay 24dp).
- Full 14-row domain color matrix applied state by state (all rows verbatim);
  active light glyphs #FFFFFF, active dark glyphs tonal ink (#0A2005 green
  rows, #111D52 others), all active pairs ≥ 4.5:1 tested. Known exception:
  inactive-dark glyph pair ≈ 4.2:1 (below 4.5, above the 3:1 icon floor) —
  both hexes user-specified, kept not altered. Temp/humidity keep coral/sky
  bypass.
- Cards unified: Favorites rebuilt on `HomeRoomCard`; `EntityTile` remains
  only in `TabTileGrid` (user-tab YAML cards). PillSwitch 52×30, 22dp thumb,
  haptic tick; corners three 28dp + one 12dp rotating by grid index; 136dp,
  16dp padding, 44dp badge/22dp icon, 16sp Bold title + 13sp Medium
  subtitle; climate 26sp ExtraBold no steppers; media 2x1 136dp; lock
  confirm preserved; body tap opens the panel (Favorites light body tap no
  longer toggles).
- Two-pane verified/locked: left 42%, right 58%, right grid 2 cols 14dp with
  measured fallback to 1 col below 364dp pane; folded 2-col 12dp. Defaults
  guard: fresh state hides weather/home/round3 blocks (visible: Cameras →
  Home Status → Favorites); stored prefs always win; reset writes the
  default set.
- Room filters + 4 Round 3 toggles verified intact (no changes needed).
- Thermostat containment via ENLARGED SCALLOP (track-shrink rejected: it
  would push the ring inside the dual-number row); bar restructured below
  the disc with 28dp daylight; popout+page share the composable; sheet Dial
  has no scallop, unchanged; separation/grab-offset/1°F/clamps untouched.
  Exit flash: nav bar transparent + root/Scaffold/content scheme
  backgrounds; PanelOverlay deliberately left transparent (painting it
  opaque would hide Home from frame 0 of opens). Full-opacity card text
  verified.
- Home Status: tapping a group morphs to a full-screen panel listing
  entities; entity tap routes to detail as before. Who's home: "Other" now
  inside its chip beside the count badge; tap opens PersonSheet.

### Expressive menus (2026-11-03 midday)
- 296 unit tests, 0 failures. Local APK:
  `haven-expressive-menus-debug.apk` (67,611,999 bytes).
- New launcher icon (superseded by the customization round's redesign):
  charcoal #161D17 background, lime #9BF67C squircle tile, charcoal house,
  off-white #FAF7F0 door, small off-white spark; adaptive v26 + v33 with
  monochrome house+spark silhouette; legacy PNGs.
- Expressive action shapes (`ui/controls/ExpressiveActions.kt`): MediaSquircle
  28dp; MediaTransportRow (two 72dp flank squircles + 96dp central
  play/pause squircle); MediaVolumeRow (56dp mute squircle + slider);
  DialogActionButtons (56dp Cancel tonal + Confirm filled pills);
  SheetIconSquircleButton 44dp. Applied to sheet/detail/Round 3 media
  controls, vacuum/security/media PanelPills (56dp), garage Open/Close/Stop
  (56dp), media card play/pause (64dp squircle), KodaMenuRow badge 48dp +
  divider indent 72dp, lock buttons + both lock dialogs, shape picker
  buttons, Favorites sheet icon buttons, YAML editor Cancel/Save. No queue
  chip invented (HA media_player exposes no queue). Not restyled: dense
  editor rows, PersonSheet (display-only), media "Turn remote on" inline
  link, Front Door close button, thermostat steppers.
- Folded pill white box FIXED: the folded pill lived in the Scaffold's
  `bottomBar` slot and Stage C statically reserved its zone, painting scheme
  background (#FAF7F0 reads white in light theme) that stayed when the pill
  hid. Fix: `bottomBar` removed; the pill is a floating overlay
  (BottomCenter, paints nothing, no taps outside the pill, below panel
  overlays); content runs full-height beneath it. By the user's explicit
  directive this supersedes Stage C's no-cover rule — the pill may overlap
  the last row at rest and auto-hides on scroll-down.
- Favorites search FIXED: search field atop FavoritesSheet, filters
  favorites + add-candidates by name or entity id, case-insensitive
  substring; empty states shown.
- Lamp card not updating FIXED (root cause): an ungated travel snapshot in
  HomeRoomCard/HomeRoomMediaCard/EntityTile leaked on every close
  (instant-dismiss nulled trigger+anim together so the LaunchedEffect clear
  branch never fired) — the card froze at close-time state while live
  toggles worked. Fix: dismissDetail/Sheet/PopoutInstant now
  clearTravelSnapshot; LaunchedEffect branches stay as backup. Displayed
  state still comes only from real entity state / the optimistic guard;
  failed calls still revert.
- Home Status tap-through VERIFIED-NOT-BROKEN: full chain card →
  HomeStatusPanelScreen → entity row → EntityDetail → CoverPageControl →
  GarageDoorMenu lands; two taps by design, not a dead tap.
- Unfolded swipe nav FIXED: folded gesture is pill-bar scrub (NavScrub.kt),
  not page swipe; VerticalSidePill had none. Added `tabAtY` +
  `pillScrubVertical` (same hover/tick/commit semantics); rail taps +
  transitions untouched; scrub may claim vertical drags on the
  rarely-scrolling 4-item rail (same trade-off as folded).
- Card shape as general shape FIXED: `generalCardShape(index)` in
  DesignTokens (three 28dp + one 12dp rotating by index); conformed
  EntityControlCard fallback, EntityTile (+now honors shape-picker
  overrides), HomeStatus cards, PanelCard, Round 3 banners, detail
  hero/status surfaces, weather card, YAML cards, TabEditor cards,
  KodaMenuGroup. Excluded: camera hero quadrant corners (joined-picture),
  dialogs, pill/slider tracks, shape-picker tiles, editor rows, inner
  boxes, lidar image clips. Shape-picker overrides still beat the rule
  everywhere.
- Camera motion info FIXED where real: HaClient publishes `entityDevices`
  from the registry; `motionSensorForCamera` links camera → same-device
  motion binary_sensor (device_class preferred); CameraQuadrant shows an
  amber-dot "Motion" pill only when the linked sensor is on. Tapo has no
  motion binary_sensor (only enable-switches); Front Door live-view vs
  69a1 device linkage is runtime-dependent, NOT hardcoded; no link → no
  chip, never faked.

### Customization (2026-11-03 midday)
- 313 unit tests, 0 failures. Local APK:
  `haven-customization-debug.apk` (67,673,432 bytes).
- Card shape variety: `generalCardShape` now cycles 8 variants (0–3 old
  three-28+one-12 rule; 4 sharp 8dp all; 5 soft 28dp all; 6 pillow 36dp all;
  7 diagonal 28/12/12/28); shape picker offers 8 corner variants first,
  then 16 expressive polygons; badge/center language untouched.
- Per-entity customization (any entity/device): one Card settings surface
  (`data/CardCustomization.kt`, `ui/screens/CardSettings.kt`, 35-icon
  `CardIcons.kt`; SettingsStore customTitles/customIconKeys/customTilePresets
  maps) with title/rename, icon picker, badge-shape picker (existing
  picker), tile color from theme presets only (light+dark hex pairs; badge
  colors stay matrix-driven so contrast floors hold). Reachable from
  detail/panel title pencil, sheet header pencil, and a Card settings row
  in the entity sheet. Applies everywhere the store is read (home/media
  cards, EntityTile, control headers, detail/panel/sheet headers, Home
  Status rows, Favorites rows, search rows, Round 3 vacuum title). Cannot
  honor: YAML-explicit names (YAML wins), camera hero quadrants, group
  panels/banners (not single entities), thermostat dial title row,
  PersonSheet. Playing media titles still win over custom titles.
- Devices + entities in search: HaClient publishes `deviceInfo` from the
  device registry; SearchScreen gains a Devices section above Entities
  (rows open DeviceSheet with the device's real entities); FavoritesSheet
  gains a Devices section expanding inline; RoomManagerScreen add-entity
  list is searchable (name/id/device name, blank = first 25); TabEditor
  entity picker matches device names. P400M Heater row surfaces its real
  entities only — no master switch invented.
- Icon redesign (researched): full-bleed vertical gradient background
  #C1E88A → #9BF67C (no dark frame, no inner tile — that was the clipping
  cause); foreground = large charcoal #161D17 house with soft rounded
  apex/eaves, off-white #FAF7F0 door, leaf-green #3E6B2F spark upper-right;
  mark inside the 66dp safe zone (base corners ≈ 31.1dp from center <
  33dp radius); monochrome = house+spark silhouette. Sources: Android
  adaptive-icon specs (108dp layers, no baked masks/shadows), M3
  Expressive guidance (66dp safe zone, monochrome layer), Google's 2026
  gradient-icon direction (no containers, one hero color per app — Haven
  uses only its own lime family), Samsung One UI masking (squircle clips
  more than Pixel circle; no built-in shape picker since One UI 3 — so the
  design survives every mask by construction, verified by geometry).
- Garage door control: new `ui/controls/GarageDoor.kt` — slatted door panel
  in a frame; tap toggles via `toggleCover`; panel genuinely translates UP
  as it opens (2×-height slatted door + empty opening sliding by open ×
  frame height) on the house spring, progress clamped [0,1]. On the garage
  card inline control (compact 44×48dp, device_class garage only; other
  covers keep the switch) and the garage panel (replaces the 28dp track).
  Open/Close/Stop 56dp pills, position slider, invalid-direction
  disable+dim, Stop-always-enabled unchanged; sheet CoverControl buttons
  also 56dp pills.
- Vacuum audit: Round 3 panel already good (Start/Pause/Dock, lidar,
  consumables, options, lifetime); generic VacuumControl fixed (56dp pills,
  humanized status e.g. Boost IQ, battery line + clamped progress; only
  real services start/pause/return_to_base/locate/set_fan_speed). No
  second Round 3 block for vacuum_homekit (same physical Eufy — would
  duplicate). Vacuum cards still have no inline control (136dp, no room);
  body tap opens the panel.
- "Trigger button" UNIDENTIFIED — nothing changed. The user's "fix things
  like this trigger button" line could not be matched to a button on screen
  in the video's final frames (candidates: the circled switch, the Toggle
  button, an automation trigger Run button); it awaits the user naming it.
- Dark media slider: root cause was badgeGlyph (dark ink #111D52 on dark
  purple #3B2554 card); all four sites now use badgeBg (#DAB9FF dark,
  #2B1246 light — better in both themes).
- Home Status placement: unfolded left pane renders HomeStatusSection
  directly below CameraQuadrantHero; right-pane loop "status" branch is an
  empty skip (never rendered twice); folded order unchanged; default
  cameras → home status → favorites verified still holding; user reorder
  moves folded status but unfolded status stays below cameras (positional).
- Device Controls provider: manifest service `.HavenControlsService`
  (BIND_CONTROLS, ControlsProviderService intent-filter, @RequiresApi R);
  `HavenControlsService.kt` custom Flow publisher (no new dependency) +
  pure `ControlsMapping.kt`. Reuses TokenStore/SettingsStore/HaClient.
  Mapping: light TYPE_LIGHT (brightness range else toggle), outlet
  TYPE_OUTLET, other switches/input_boolean TYPE_SWITCH, fan TYPE_FAN,
  cover garage/curtain/blinds/shutter/door/generic (range when position
  exists else toggle), climate TYPE_THERMOSTAT (mode flags from
  hvac_modes). Excluded deliberately: lock (quick-settings unlock = safety
  footgun), media_player, vacuum, camera, sensors, binary_sensors. Default
  set: favorites first (mappable only, favorite order), then all other
  mappable entities by name. Climate single setpoint clamped to min/max;
  heat/cool band shift moves the whole band by midpoint delta (never
  collapses to one value); boolean climate action = heat-preferred non-off
  / off. Not logged in / no states in ~10s: one stateless "Open Haven to
  sign in" control (app intent) — never a fake device. No token refresh
  reimplemented. The user's SmartThings/Home/HA selections untouched.

## Device-verification status (honest)

- The local program was never device-verified end to end: install/launch, HA
  connection (LAN-only), folded/unfolded behavior, frame rate, and the Front
  Door live + wave test (liveness still unproven — never claim live until
  the wave appears within ~2s) all remain owed on the user's Fold 8.
- User-confirmed on his own videos/screenshots: the customization round's
  tile color and icon changes took (yellow Lamp/Standing Lamp tiles, changed
  icons visible in his screenshots); the garage-door slatted visual was
  approved by him, with actuation deliberately untested (he will not open
  it to test); the OUTDOOR Tapo camera showed his wave live (Tapo liveness
  evidence — the Front Door 69a1 wave test is a different camera and is
  still open).
- User-reported, not yet resolved: the customization icon renders as a black
  house on a black background in the unfolded recents task pane — very
  dark, almost impossible to see. Cause TBD (background layer not showing
  vs monochrome/themed path); the next icon round must verify from the
  APK's actual icon resources, not guess.
- Carried known issues (not fixed in these rounds): inactive-dark glyph
  pair ≈ 4.2:1 (user hexes, kept); PanelOverlay root deliberately left
  transparent; pre-existing dual-number kinetic graze (~5dp during
  thermostat transitions); `HaClient.streamCamera` silent EOF-with-frames;
  sheet Dial has no scallop; #99 garage duplicate-header-badge removal
  never confirmed from diff; chart-pager surface never named by the user.
## Feedback round 2 (videos 53 / 26 / cc / 88 / 1b, against #108)

1. **Custom-color off state (53).** A card with a custom tile color now renders the matrix INACTIVE gray when its entity is off — the custom on-color never persists into the off state. New pure helper `presetAppliesForState` (CardCustomization.kt): binary toggleables "on" only, vacuum cleaning/returning only, lock locked only, cover anything but closed, climate anything but off, media/sensors always active except unavailable/unknown (media idle/paused keeps its row; a motion sensor reading "off" is its normal state, not an off state). Applied to the container AND badge pair on HomeRoomCard, HomeRoomMediaCard, and EntityTile together; unset cards are byte-identical (matrix behavior unchanged).
2. **Switch + icon follow the custom theme (26).** The card switch now follows the preset with the badge icon's chromatic styling: new `presetSwitchColors` (checked track = preset badge bg dark tint, checked thumb = preset container, unchecked = matrix inactive pair). The badge icon already followed the preset (feedback round, item 5) and is unchanged. Detail/sheet header badges were deliberately NOT changed in this round: the complaint names the card, and header badge colors feed the morph traveler — changing them risks open/dismiss color jumps.
3. **Subtitle descender clipping (26).** Polygon card outlines are inscribed in a circle of the card's min dimension, so their edges cut into the bottom text zone ("heat cool", "closed", bottom of "O"). Rounded-corner outlines never clip (28dp corners clear the 16dp-padded text zone), so only polygon cards shift: badge-to-text spacer 16dp -> 4dp and bottom padding 16dp -> 28dp, moving the text block up 12dp with card height unchanged at 136dp. Bottom padding alone cannot move top-anchored content — the spacer reduction is the mechanism. Applied to HomeRoomCard and EntityTile; the media card is skipped (centered subtitle + bottom slider, not bottom-anchored text).
4. **Thermostat dial rework (53).** SUPERSEDES the fixed 55–85 display range. `thermostatDisplayRange` now brackets the current setpoints with a 10°F margin, clamped to [40, 100] (67/69 -> 57–79, inside his example bands 55–60 / 75–80; max never above 100, min floor 40); Celsius ranges with no [40, 100] overlap still fall back to the HA range untouched. The bar protrudes more: track inset 22dp -> 20dp, stroke 12dp -> 16dp idle / 18dp -> 22dp grabbed (outer edge out 4dp, inner edge unchanged, so the dual-number row keeps its clearance; worst-case pulse-bloom margin narrows to 0.8dp — a soft alpha fade kissing the scallop valley, never a hard crossing). The +/− steppers move down (spacer 8dp -> 16dp), still inside the dial face, no panel room added. 1°F steps, handle-grab-only, clamps, and never-rewrite-setpoints all hold. KNOWN LIMIT (pre-existing architecture): drags map through the display range, so drags are limited to the dynamic window; steppers clamp to the full HA range, so the full range stays reachable via steppers.
5. **Pill background removal (cc).** Both pill containers (folded PillNav, unfolded VerticalSidePill) are now fully transparent with zero elevations — no background bar/band, content visibly scrolls underneath during transitions. `unfoldedBottomClearance` stays: it is unpainted padding, not a band, and the video-71 page-stability rule (page never moves on panel open/close) still holds. JUDGMENT CALL, stated plainly: with no dark container behind the pill, the old both-themes light-sage unselected glyph (#D7E2D4) would be nearly invisible over light content, so unselected glyphs are now theme-aware (light sage on dark, dark charcoal #161D17 on light); the selected #9BF67C capsule is opaque and unchanged.
6. **Popups: content-sized + side-anchored (cc; refines #108's centered popups).** CenteredPopup is now 62% of the overlay capped at 400dp wide and 75% tall (was 88% / 560dp / 88%), scrolling internally when taller, anchored toward the source tile's side (left third -> start, right third -> end, else centered; pure `popupSideAnchor` helper, unit-tested) with a 16dp side margin, entering from the tile's direction via the analytically computed side-anchored center (known at frame 0, before layout). The thermostat disc (320dp + 32dp padding = 352dp) sets the width floor and the narrowest wide layout is 600dp, so 0.62 x (600 - 32) = 352dp fits exactly at the threshold. JUDGMENT CALL: internal polygons keep their sizes — a literal half-width window (280dp) would clip the dial, and shrinking the disc would break the dual-number row (169dp needs the 240dp dial) and grab targets; the window shrink + side anchor deliver the complaint. "Cool" text buffer is reconciled with item 3 (same padding rule). Folded full-screen morph unchanged.
7. **Rail selected indicator persists (1b).** ROOT CAUSE: the wide call site passed `selectedNavIndex(current, barItems)`, which returns null off-tab, and a popup IS off-tab — so the indicator Box was never rendered while a popup was open. The call site now remembers the last on-tab index and falls back to it while any sheet/detail/popout anim is active; other off-tab screens keep the old unhighlighted behavior, and dialogs never change `current` so they never need the fallback. This extends the video-71 rule (rail stays mounted) to its selected indicator. Folded bar unaffected (opaque panels cover it).
8. **Device Controls: in-panel popup, not app launch (88).** The temperature panel widget IS the Controls API panel affordance (Android docs: tap = panel, long-press = app). Two construction defects could push a panel to the app intent instead, both fixed: (1) `currentActiveMode` duplicated the selected mode, so a heat_cool system advertised HEAT_COOL as its active mode — a selected mode, not an action; it now comes from hvac_action via pure `climateActiveModeFromHvacAction` (heating -> HEAT, cooling -> COOL, idle -> selected mode). (2) A thermostat whose hvac_modes mapped to no flags published an empty mode set, which cannot render a detail panel; it now falls back to the selected mode's flag. Template verified against SDK 35 (javap: `TemperatureControlTemplate(String, ControlTemplate, int, int, int)`); device type THERMOSTAT 49, mode/flag constants, and the ModeAction/BooleanAction/FloatAction handlers in `performControlAction` were already correct. The "Open Haven to sign in" control keeps its app intent (launcher by design). If Samsung's panel still launches the app for the Ecobee tile after this, that is panel-side (SmartThings uses Samsung's private path) — the app side is now maximally correct per the public API.
9. **Device Controls transient loading (88).** Three app-side causes found and fixed: (1) NO eager connect — the panel binds the service before asking for controls, so a cold open paid full WS-connect + state-dump latency inside SystemUI's publish window; `onCreate` now connects eagerly. (2) The one-shot `started` flag never re-read the token/URL, so a sign-out/in or token rotation kept the stale session forever; `ensureStarted` now tracks the (url, token) pair and disconnects + reconnects when it changes (and disconnects on sign-out). (3) The one-shot publisher emitted the SIGN-IN tile when logged in but states timed out — misleading for a signed-in user, and it lingered as the panel's only Haven tile; it now completes empty in that case (the sign-in tile is kept only for the actually-logged-out branch), and the panel re-queries on its next open. Residual uncertainty, stated honestly: Samsung's panel may cache control lists across opens, so if the transient persists after this build it is a panel caching quirk, not an app-side publish failure — the video-88 frames already show the service basically works (Lamp toggled in-panel).

Not in this round (kept, no action): TV Material You buttons; faster camera live view. The video-26 recording-button musing stays inconclusive ("I don't know") — nothing changed, awaiting his clarification. Garage-door actuation untouched (he will not open it to test). Front-door wave test still open (his device action).

## Feedback round 3 (videos ba / a3 / 6d, against #109)

Baseline proven green before any change: clean assembleDebug +
testDebugUnitTest BUILD SUCCESSFUL, 327 tests, 0 failures. This
round: 334 tests, 0 failures (327 + 7 new). Local APK:
`haven-feedback-round3-debug.apk` (67,092,697 bytes).

1. **Pill correction — round 2 item 5 was a MISREAD, reverted (ba).**
   His words decide it: "I want the pill to be here still. I want
   the pill and the pill background... what I was saying to remove
   is the box that is clipping the pages as they transition." TWO
   halves, both shipped: (a) RESTORED — both pill containers
   (folded PillNav, unfolded VerticalSidePill) are opaque #161D17
   again in both themes, exactly as #108 had them, with the
   floating-bar shadow restored (6dp); the round-2 theme-aware
   glyph change is reverted along with the transparency, since its
   precondition (no dark container) is gone by his own correction
   — unselected glyphs are #D7E2D4 in both themes again, and
   `pillTokensMatchRewriteDirective` is reverted to the #108 law
   (charcoal-on-white assertion removed with it). (b) The actual
   complaint fixed — tab transitions now DRAW FULL-SCREEN: the
   rail draws after (on top of) Scaffold content via zIndex(1f)
   at the VerticalSidePill call site, so sliding pages pass UNDER
   the pill and only the pill's own shape obscures them; leftward
   travel adds the 84dp rail width (pure `tabSlideOffsets`,
   unit-tested: forward unfolded 1000px content exits -1084px,
   backward mirrored, folded exits exactly -1000px), so pages
   slide ALL the way off-screen in both directions — Koda's
   one-third distances are superseded for the tab branches by his
   latest words. Resting page positioning is UNCHANGED (84dp rail
   + weight(1f) Scaffold, byte-identical layout; zIndex changes
   draw order only). JUDGMENT CALL: the rail stays in the Row
   rather than moving after the Scaffold in a Box — same draw
   result, zero resting-geometry change. The non-tab fixed slide
   (1/3 both ways) is deliberately UNCHANGED: he complained about
   tab slides, and scoping corrections exactly is the rule; the
   Box restructure would not have changed its distances either.
   Distinct from the video-71 panel rule (panels never move the
   page) — not conflated.
2. **Lights panel popup sizing (a3).** ROOT CAUSE, verified in
   code: `HomeStatusPanelScreen` emitted its card Rows + Spacers
   as DIRECT children of a BoxWithConstraints — a Box stacks
   children, it does not flow them vertically — so every row
   rendered on top of the first, the trailing Spacer added no
   height, and the popup measured one row tall: the second card
   overlapped the first and the cards ended flush with the
   popup's bottom edge (zero daylight below them — reading as
   clipping). Fix: the rows live in a Column inside the
   BoxWithConstraints (same card width math, 12dp gaps kept) plus
   16dp of daylight below the last card, so the popup sizes to
   its content and both cards are fully visible. This also fixes
   the latent >2-entity case, where rows 3+ would all pile onto
   row 1. The 62%/75% CenteredPopup caps still apply — scroll
   only when content genuinely exceeds the screen — but the caps
   now yield to real content height instead of a mis-measured
   single row.
3. **Panel-card tap route (a3) — VERIFIED, no change needed.**
   His words: "when I click on one it opens up the full menu all
   lights." Checked both the code and his own video: the panel
   passes an `onOpenPanel` override of
   `navigate(Screen.EntityDetail(entityId))` to HomeRoomCard, and
   the a3 video's 20s frame shows exactly that — a tap opens the
   Standing Lamp detail popup (title, On toggle, Power / Last
   changed / Socket rows) over the still-open Lights panel. The
   home-page card tap for a light routes identically
   (binary → EntityDetail). Before = that lamp's own detail
   popup; after = the same — there is no all-lights-panel route
   on card tap to remove. If he wants a different tap behavior
   (e.g. toggle on tap), that is a new directive, not assumed
   here.
4. **Panel custom colors (a3) — VERIFIED identical path, locked
   by test.** His words: the panel's cards "still not matching
   the yellow that I've assigned" while home matches. Pixel
   evidence confirms the symptom (home Standing Lamp card
   #FFE082 amber; panel Standing Lamp card lime #CCFDAB) — but
   the code path is PROVABLY shared, not divergent:
   HomeStatusPanelScreen renders the same HomeRoomCard
   composable as home, reading the same SettingsStore maps
   through the same `rememberCardCustom` snapshot (version-keyed
   on `cardCustomVersion`, bumped on every save), for the same
   entity IDs, through the same pure resolution (`tilePresetFor`
   → `presetAppliesForState` → `tilePresetColor` /
   `presetBadgeColors` / `presetSwitchColors`); the store reads
   SharedPreferences fresh on every call (no in-memory cache to
   go stale), and a lime panel card with a preset set is
   impossible from this resolution (preset + on → preset color;
   preset + off → inactive gray, never matrix lime). The only
   way the panel reads lime while home reads amber is a store
   read that lacked the amber entry at panel-open time — e.g.
   presets lost on a reinstall (a key-mismatch uninstall wipes
   SharedPreferences; the store is global by entity ID, so
   re-assigning once restores every surface). No color code was
   changed on a guess. New contract test
   `panelInputsResolveSamePresetColorsAsHome` pins the shared
   resolution for the panel's exact inputs (both lamps on, Lamp
   lime, Standing Lamp amber: container + badge + switch all
   resolve from the assigned preset). DEVICE CHECK OWED: on the
   new build, does home still show amber? If home shows amber
   but the panel does not on the SAME build, that reproduction
   isolates a real panel bug and reopens this item.
5. **Thermostat dynamic scale on EVERY surface (a3).** The popout
   already had the round-2 dynamic window (10°F margin, clamped
   [40, 100]) and the expanded bar — but it never SHOWED the
   window's ends, and the sheet dial still used the full HA
   min..max. All three surfaces now: (a) SHEET — ClimateControl's
   Dial and slider move from min..max to
   `thermostatDisplayRange(min, max, setpoints)`, exactly like
   the popout (state coercion stays in HA range, as the popout
   does; out-of-window values pin at the ends). (b) LABELS — both
   dials gain range-end legends spread to the dial width: the
   generic Dial gains opt-in `showRangeEnds` (min at the bar's
   left end, max at its right end; off by default so the weather
   and other Dial users keep their exact layout), and
   ThermostatDialBlock gains the same row below the popout dial
   (the detail page renders ThermostatPopout, so it inherits
   both the window and the labels). 67/69 now reads 57°/79° at
   the bar ends on every surface, with the thumbs spread across
   more of the bar. 1°F steps, handle-grab-only, and
   never-rewrite-setpoints hold everywhere. KNOWN LIMIT (same as
   the popout's): sheet drags/sliders map through the display
   window; the full range stays reachable via the popout's
   steppers.
6. **Device Controls deep links (6d).** He ACCEPTS app-launch for
   now (the in-panel popup aspiration is parked — Samsung's
   panel launches the app for the Ecobee tile despite round 2).
   The gap was real: `appIntentFor` carried NO entity extra, so
   every control tap landed on Home. Now: the provider's app
   intent carries `EXTRA_CONTROL_ENTITY_ID` (per-entity
   requestCode already kept intents distinct; SINGLE_TOP added
   so a tap routes into the running single-activity instance via
   onNewIntent instead of stacking a second MainActivity);
   MainActivity stores it on the VM from onCreate AND onNewIntent
   (`handleControlDeepLink`, blank-safe); a render-nothing
   `ControlDeepLinkRouter` in HavenApp consumes it once the
   entity's state has loaded (cold start: the effect re-runs per
   states emission until the entity appears) and jumps to that
   entity's surface — climate → `openClimatePopout` (thermostat
   page, the same machinery a thermostat card tap uses),
   everything else → EntityDetail, exactly as a home-page card
   tap does (popup on unfolded, detail as home cards do).
   Unknown ids (the sign-in control's "haven_open_app", deleted
   entities) clear without routing once states are non-empty, so
   a stale link never hangs the slot; a duplicate detail (already
   viewing that entity) is skipped. Pure routing helper
   `controlDeepLinkTarget` in ControlsMapping, unit-tested
   (climate → ThermostatPopout; light/switch/cover/fan → Detail;
   unknown domain → Detail). The router's states collection
   recomposes only itself, never UI.

Not in this round (kept, no action): the still-unanswered
"trigger button" identification; the inconclusive video-26
recording-button musing; garage-door actuation (he will not open
it to test); the front-door wave test (his device action, still
open). WebRTC stays gated off (`webrtcEnabled = false`).
