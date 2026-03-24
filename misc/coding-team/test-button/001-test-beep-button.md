# 001 — Extract beep playback and add test button

## Context

`ChimeReceiver.kt` has `playBeep()` (synthesises a sine wave) and `scheduleBeeps()` (sequences N beeps with gaps). Both are `private` and `scheduleBeeps` is coupled to wake lock / `PendingResult` cleanup. The UI in `MainActivity.kt` has a single Start/Stop button.

## Objective

Add a "Test" button that plays 3 short beeps (the :30 chime pattern).

## Scope

### `ChimeReceiver.kt`

1. Remove `private` from `playBeep` (make it package-level / `internal`).
2. Add a new top-level function `playBeepSequence(count: Int)` that:
   - Creates its own `Handler(Looper.getMainLooper())`.
   - Schedules `count` short beeps using the same timing constants (`SHORT_BEEP_MS`, `GAP_BETWEEN_BEEPS_MS`).
   - No wake lock, no `PendingResult` — this runs in-process from the UI.
3. Refactor `ChimeReceiver.onReceive()` to use `playBeepSequence` for the short-beep case. The receiver still manages its own wake lock and `pendingResult.finish()` separately — schedule the cleanup on the handler after the total playback duration. The long-beep (top-of-hour) path stays as-is.

### `MainActivity.kt`

1. Add a "Test" `Button` below the Start/Stop button (always visible, regardless of `isRunning` state).
2. `onClick` calls `playBeepSequence(3)`.
3. Use the same `fontSize = 24.sp` as the existing button for visual consistency.

## Non-goals

- No changes to alarm scheduling, service, or notification.
- No debouncing or disabling the button during playback.
- No persistence of any test-related state.

## Constraints

- Keep the beep constants (`SHORT_BEEP_MS`, `GAP_BETWEEN_BEEPS_MS`, etc.) as `private const val` in `ChimeReceiver.kt` — `playBeepSequence` lives in the same file and can access them.
