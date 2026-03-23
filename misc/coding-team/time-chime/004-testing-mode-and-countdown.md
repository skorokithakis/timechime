# Task 004: Every-minute chiming and countdown timer

## Context

Currently chimes every 10 minutes with `count = minutes / 10`. User wants to test more frequently.

## Objective

Three changes:

### 1. Chime every minute instead of every 10 minutes

In `AlarmScheduler.kt`, change `nextTenMinuteBoundaryMillis` to target the next whole minute (i.e., next time where seconds == 0), not the next 10-minute boundary. Rename the function accordingly (e.g., `nextMinuteBoundaryMillis`).

### 2. Chime count = minutes mod 10

In `ChimeReceiver.kt`, change the count logic from `minutes / 10` to `minutes % 10`. So at :03 → 3 short beeps, at :10 → long beep (count 0), at :17 → 7 short beeps, etc.

### 3. Countdown timer on the main screen

In `MainActivity.kt`, add a text element showing the seconds remaining until the next chime. Something like "Next chime in 43s". Update it every second.

Implementation approach:
- Use `LaunchedEffect` with a `while(true)` loop that computes seconds until the next whole minute and delays 1 second.
- Only show the countdown when `isRunning` is true. When stopped, show nothing (or "Stopped" or similar — keep it minimal).
- Place it below the button with some spacing.

## Non-goals

- No permanent config for chime interval — this is a testing convenience. We'll revert to 10-minute intervals later.

## Constraints

- Keep the existing long-beep-at-zero / short-beeps-otherwise logic, just change the count source.
