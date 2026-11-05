# Haven Changelog

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
