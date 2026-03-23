# Task 002: Chime service and alarm logic

## Context

The project scaffold is in place. Package is `com.stavros.timechime`. Manifest already has all needed permissions. minSdk is 26.

## Objective

Implement the background chime system: a foreground service, a broadcast receiver for alarm firings, alarm scheduling, and tone playback.

## Scope

### Files to create (all under `app/src/main/kotlin/com/stavros/timechime/`)

**`ChimeService.kt`** — Foreground service (`Service` subclass).
- On `startCommand`: post a foreground notification (simple, static text like "Time Chime is running"), then schedule the next alarm.
- On `stopCommand` / `onDestroy`: cancel any pending alarm, stop the foreground service.
- Notification channel: create in `onCreate`. Channel ID `"chime_service"`, name "Chime Service". Low importance (it's just a persistent indicator).
- `foregroundServiceType = mediaPlayback` (already in manifest permission).

**`ChimeReceiver.kt`** — `BroadcastReceiver`.
- On receive: acquire a partial wake lock (timeout 10s), play the appropriate beep pattern for the current time, then schedule the next alarm.
- Beep logic:
  - Get current minutes of the hour.
  - Compute `count = minutes / 10` (integer division).
  - If `count == 0` → play one long beep (400ms).
  - Else → play `count` short beeps (200ms each), with 300ms silence between beeps.
- Use `android.media.ToneGenerator(AudioManager.STREAM_MUSIC, 100)` with `ToneGenerator.TONE_DTMF_0` (or any clean-sounding tone).
- For timing the gaps between beeps, use a `Handler` posting delayed runnables. Release the wake lock after the last beep finishes.

**Alarm scheduling** (can be a top-level function or in a small utility object, your call):
- Calculate the next 10-minute boundary: find the next time where `minutes % 10 == 0` and `seconds == 0`.
- Use `AlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)`.
- The PendingIntent should target `ChimeReceiver`.
- Use `PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE`.

### Files to modify

**`AndroidManifest.xml`** — Add:
- `<service>` declaration for `ChimeService` with `android:foregroundServiceType="mediaPlayback"`.
- `<receiver>` declaration for `ChimeReceiver`.

## Non-goals

- No UI changes in this task (that's task 003).
- No runtime permission requests yet.
- Don't worry about the service being started/stopped from the UI — that's task 003.

## Constraints

- The receiver must re-schedule the next alarm after each firing (one-shot alarms, not repeating — repeating alarms are inexact).
- The tone playback must work even when the screen is off (hence wake lock + STREAM_MUSIC).
- Keep the wake lock scope tight — acquire before playback, release after last beep.
