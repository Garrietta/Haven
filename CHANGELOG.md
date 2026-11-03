# Haven Changelog

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
