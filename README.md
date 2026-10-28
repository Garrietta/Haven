# Haven

A native Material 3 Expressive Home Assistant client for Android.

Haven is **not** a Lovelace WebView shell like the official app. It talks to Home Assistant over the WebSocket API + REST and renders dashboards, areas, and entity controls natively in Kotlin/Compose — with the same expressive design language as Relay, Koda, and Pixel Music: real M3E polygons (`graphics-shapes`), bouncy springs (380 / 0.62), dynamic Material You color, per-entity accent colors, a pill-bar bottom navigation, and a control designed for each entity domain. No WebView anywhere in v1.

## Login

Two ways in, both against your own instance:

- **Sign in with Home Assistant (OAuth + PKCE S256).** The app opens your instance's authorize page in the browser and comes back via `haven://auth-callback`. Home Assistant requires the client-ID URL to declare that redirect, so this repo's GitHub Pages site (`https://garrietta.github.io/Haven/`) carries the `<link rel="redirect_uri">` tag. Tokens refresh automatically; a dead refresh token sends you back to sign-in.
- **Long-lived access token (fallback, always works).** Create one in your HA profile (Profile → Long-Lived Access Tokens) and paste it in. Tokens are encrypted at rest with an AndroidKeystore AES-256/GCM key and are never sent anywhere except your instance.

## What v1 covers

- **Today screen:** greeting, weather dial, who's home, now playing, lights-on summary with all-off, and attention alerts.
- **Boards:** reads your instance's Lovelace config and renders it natively — Entities, Tile, Button, Light, Thermostat, Glance, Sensor, Weather Forecast, Media Control, Area, stacks, grids, Conditional, Markdown. Unknown card types get an honest placeholder, never a web page. **In-app editing:** create views, add/remove/reorder cards, and save the dashboard back to the instance (`lovelace/config/save`, storage-mode dashboards).
- **Entity controls, one design per domain:** light (blooming bulb toggle, brightness, color-temp, hue dots), switch (pill toggle), cover incl. garage door (door visual, open/close/stop, position slider), climate (**both** a dial and a slider, mode + preset chips), vacuum (start/pause/dock/locate, fan speed, battery), media player (transport, volume, source select), fan (speed slider, presets, oscillate), lock (unlock asks first), sensor/binary_sensor tiles, weather (dial + forecast), camera (snapshot tile + fullscreen), scene/script tap-fire, automation toggle + Run, todo checklists, helpers (input_boolean/number/select/text/datetime, timer, counter), alarm keypad.
- **Creation inside the app:** guided automation builder (trigger → condition → action), scene builder (capture current device states), script builder (service-call steps), helper builder (input_*, timer, counter). Automations/scripts/scenes save over the REST config API (`POST /api/config/<kind>/config/<kind>.<slug>`); helpers save over WebSocket (`<domain>/create`).
- **Areas browser, entity search** (name / id / area / domain), and **entity detail** with attributes and a 24-hour history sparkline.
- **Live updates** over WebSocket (`get_states` + `subscribe_events state_changed`) with automatic reconnect.

## Honest gaps (v1)

These are known, not hidden. They are the next rounds, not bugs:

- **Not yet verified against a live instance:** automation/script/scene REST saves, helper creates for `timer`/`counter` (the `<domain>/create` pattern is directly evidenced only for `input_number`), todo item create/update/delete (only `todo/item/move` was seen in reference code), and the OAuth Pages propagation (GitHub Pages can take minutes to publish; long-lived tokens work regardless). If a save fails, the builder says so on screen — it never pretends success.
- **Not built (roadmap):** release signing (debug APK only this round), Wear/Auto, widgets, voice satellite, energy view, admin panels (template evaluator, service JSON editor, backups), floors in the areas browser, multi-dashboard editing (v1 edits the default dashboard), picture/map/history-graph/logbook cards (placeholder for now), todo *list* creation (lists come from integrations like Local Todo; item CRUD is in).
- **Design notes:** OAuth uses PKCE S256 (the reference app's no-PKCE shortcut was deliberately not copied). Dashboard editing writes the full config back (storage-mode only; YAML-mode dashboards can't be saved this way).

## Build

```sh
./gradlew :app:assembleDebug
```

GitHub Actions builds a debug APK on every push to `main` and on manual dispatch (`.github/workflows/build.yml`, JDK 17, SDK 35). The APK artifact is `haven-debug`.

## Architecture

- `data/HaClient.kt` — OkHttp WebSocket client (auth, states, events, services, registries, Lovelace, todo) + REST (history, camera proxy, config saves).
- `data/TokenStore.kt` — Keystore AES/GCM token storage. `data/SettingsStore.kt` — theme / reduce-motion prefs.
- `ui/theme/` — M3E polygon shapes + morphing, dynamic color, house spring.
- `ui/controls/` — one control per entity domain. `ui/screens/` — login, Today, boards + editor, areas/search/detail, builders.
- No WebView, no JavaScript bridge, no third-party HA SDK. Original code throughout; R1HA (Unlicense) was used only as API reference.
