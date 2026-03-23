# Task 003: UI and service wiring

## Context

`ChimeService` and `ChimeReceiver` are implemented. The service can be started/stopped via standard `startForegroundService` / `stopService` intents. `MainActivity` currently just shows centered "Time Chime" text.

## Objective

Replace the placeholder UI with a functional Start/Stop toggle, wire it to the service, handle runtime permissions, and persist the running state.

## Scope

### `MainActivity.kt` — rewrite the Compose UI

- Single screen with a large, centered toggle button.
- Button text: "Start" when stopped, "Stop" when running.
- Tapping "Start":
  1. Request `POST_NOTIFICATIONS` permission (Android 13+). If denied, still proceed (the service works without a visible notification, it's just less reliable).
  2. Call `startForegroundService(Intent(this, ChimeService::class.java))`.
  3. Save `running = true` to SharedPreferences.
- Tapping "Stop":
  1. Call `stopService(Intent(this, ChimeService::class.java))`.
  2. Save `running = false` to SharedPreferences.
- On activity `onCreate`, read the persisted state to set the initial button label. If the state says running, also re-start the service (covers process death / app restart scenarios).

### SharedPreferences

- Use a simple `getSharedPreferences("timechime_prefs", MODE_PRIVATE)`.
- Single boolean key: `"is_running"`.
- No need for a DataStore or anything fancier.

## Non-goals

- No theme customization, settings screen, or about page.
- No boot receiver.
- No service binding or two-way communication — fire-and-forget intents are sufficient.

## Constraints

- The `POST_NOTIFICATIONS` permission request should use the Activity Result API (`registerForActivityResult` with `RequestPermission`), not the deprecated `requestPermissions`.
- Keep the Compose code in `MainActivity.kt` — no need for separate screen files for a single-screen app.
