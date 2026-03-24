# Architecture

## What the app does

Time Chime is an Android app that plays a subtle 15 kHz sine-wave tone every 10
minutes to help the user track time passively. At the top of the hour (:00) it
plays one long beep (400 ms). At :10–:50 it plays 1–5 short beeps (200 ms each,
100 ms gap between them) corresponding to how many tens of minutes past the hour
it is. The tone is chosen to be inaudible to most people nearby.

## Detected stack

| Layer | Technology | Evidence |
|---|---|---|
| Language | Kotlin 2.0.21 | `gradle/libs.versions.toml` |
| UI framework | Jetpack Compose + Material 3 | `app/build.gradle.kts`, `MainActivity.kt` |
| Android Gradle Plugin | 8.5.2 | `gradle/libs.versions.toml` |
| Compile / target SDK | 34 | `app/build.gradle.kts` |
| Min SDK | 26 (Android 8.0) | `app/build.gradle.kts` |
| Build system | Gradle (Kotlin DSL, version catalog) | `build.gradle.kts`, `gradle/libs.versions.toml` |
| No third-party libraries | Only AndroidX / Compose BOM | `app/build.gradle.kts` |
| No DI framework | Plain constructor / top-level functions | all source files |
| No database | State persisted via `SharedPreferences` | `MainActivity.kt`, `ChimeService.kt` |

## Project structure

```
timechime/
├── app/
│   ├── build.gradle.kts          # Module-level build config (SDK versions, deps)
│   └── src/main/
│       ├── AndroidManifest.xml   # Permissions, activity, service, receiver declarations
│       ├── kotlin/com/stavros/timechime/
│       │   ├── MainActivity.kt   # Single activity; hosts all Compose UI
│       │   ├── ChimeService.kt   # Foreground service; owns the persistent notification
│       │   ├── ChimeReceiver.kt  # BroadcastReceiver; plays beeps when alarm fires
│       │   └── AlarmScheduler.kt # Schedules / cancels the exact repeating alarm
│       └── res/
│           ├── values/strings.xml
│           └── values/themes.xml
├── gradle/libs.versions.toml     # Version catalog (single source of truth for versions)
├── build.gradle.kts              # Root build file (plugin declarations only)
├── settings.gradle.kts           # Module inclusion + repository config
├── misc/coding-team/time-chime/  # Iterative spec documents (not shipped)
└── README.md
```

## How chimes are triggered and played

### Scheduling (`AlarmScheduler.kt`)

- `scheduleNextChimeAlarm()` computes the next wall-clock time where
  `minutes % 10 == 0` and `seconds == 0`, then fires a one-shot
  `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, …)` broadcast to
  `ChimeReceiver`.
- The alarm is **not** repeating. Each firing of `ChimeReceiver.onReceive()`
  calls `scheduleNextChimeAlarm()` again to chain the next alarm. This avoids
  drift and keeps the alarm cancellable with a single `cancelChimeAlarm()` call.
- `ChimeService.onDestroy()` calls `cancelChimeAlarm()` to stop the chain.

### Playing audio (`ChimeReceiver.kt`)

- `ChimeReceiver` extends `BroadcastReceiver` and calls `goAsync()` immediately
  so the process is not killed before the `Handler` runnables finish.
- A `PowerManager.PARTIAL_WAKE_LOCK` is acquired (10 s timeout) to keep the CPU
  alive while audio plays.
- `playBeep(durationMs, handler)` synthesises a PCM 16-bit mono sine wave at
  15 000 Hz entirely in memory (`ShortArray`), writes it to an `AudioTrack` in
  `MODE_STATIC`, calls `track.play()`, and schedules `track.release()` via
  `handler.postDelayed` after the duration elapses.
- Beep sequencing uses tail-recursive calls to `scheduleBeeps()`, each
  incrementing `delayMillis` by `SHORT_BEEP_MS + GAP_BETWEEN_BEEPS_MS` (300 ms
  per step). No coroutines or threads are used; everything runs on the main
  looper via `Handler`.
- Audio attributes: `USAGE_MEDIA` / `CONTENT_TYPE_SONIFICATION`.

### Foreground service (`ChimeService.kt`)

- Started as a foreground service with `foregroundServiceType="mediaPlayback"`.
- Posts a persistent notification with a "Stop" action that sends `ACTION_STOP`
  back to the service.
- Does **not** play audio itself; it only owns the notification and calls
  `scheduleNextChimeAlarm` on start and `cancelChimeAlarm` on destroy.

## UI structure (`MainActivity.kt`)

- Single `ComponentActivity` → single `@Composable` screen `TimeChimeScreen`.
- State: one `Boolean` (`isRunning`) and one `Long` (`secondsUntilChime`).
- `isRunning` is persisted to `SharedPreferences` (`timechime_prefs` /
  `is_running`) so the service can be auto-restarted on process death.
- On resume, `LifecycleEventEffect(ON_RESUME)` re-reads `SharedPreferences` so
  the UI reflects a stop triggered from the notification.
- The countdown (`secondsUntilChime`) is recomputed every second via a
  `LaunchedEffect` coroutine loop using `System.currentTimeMillis() % 600_000`.
- Layout: `Box(Center)` → `Column(Center)` → single `Button` ("Start"/"Stop") +
  optional countdown `Text`.
- On API 33+ the `POST_NOTIFICATIONS` runtime permission is requested via
  `rememberLauncherForActivityResult`; the service is started inside the
  callback to avoid a race where the notification is posted while the dialog is
  still visible.
- Two `@Preview` composables: stopped state and running state.

## Build and test commands

```bash
# Build a debug APK
./gradlew assembleDebug

# Build a release APK
./gradlew assembleRelease

# Run all checks (lint + unit tests — no tests exist yet)
./gradlew check
```

There are **no unit or instrumentation tests** in the repository. There is no
pre-commit hook, no linter config, and no CI configuration.

## Conventions

- **Formatting**: `kotlin.code.style=official` in `gradle.properties`; no
  ktlint or detekt config present.
- **Error handling**: no try/catch anywhere; exceptions propagate naturally.
- **Logging**: no logging framework; no `Log.*` calls in source.
- **Configuration**: `SharedPreferences` only; no dotenv or config library.
- **Comments**: explain "why" and trade-offs, not "what" (e.g. the `goAsync()`
  comment, the permission-dialog race comment).
- **Naming**: full words, no abbreviations (`pendingResult`, `remaining`,
  `delayMillis`).

## Do and don't patterns

### Do
- **Chain one-shot alarms** rather than using `setRepeating` — gives exact
  timing and clean cancellation. (`AlarmScheduler.kt`)
- **`goAsync()` + wake lock** in `BroadcastReceiver` to extend lifetime past
  `onReceive()` return. (`ChimeReceiver.kt`)
- **Synthesise audio in-process** with `AudioTrack MODE_STATIC` — no audio
  files bundled. (`ChimeReceiver.kt`)
- **Persist running state** to `SharedPreferences` so the service survives
  process death and the UI stays consistent with the notification. (`MainActivity.kt`,
  `ChimeService.kt`)
- **Re-read state on resume** via `LifecycleEventEffect` rather than relying on
  in-memory state that may be stale after a notification action. (`MainActivity.kt`)

### Don't
- No coroutines in the receiver — beep sequencing uses `Handler.postDelayed`
  chains instead. (`ChimeReceiver.kt`)
- No audio files / raw resources — tone is synthesised at runtime.
- No ViewModel — state lives directly in the composable with `remember`.
- No dependency injection framework.
- No broad exception swallowing.

## Open questions

- There is no test suite. It is unclear whether the exact-alarm permission
  (`USE_EXACT_ALARM` vs `SCHEDULE_EXACT_ALARM`) behaves correctly on all OEM
  variants of Android 12+ without a device test.
- `isMinifyEnabled = false` in the release build type — intentional or
  oversight?
- The `misc/coding-team/time-chime/` spec documents (001–006) suggest planned
  features (custom frequency, gap, testing mode) that are not yet implemented.
