# Task 005: 17kHz tone and 100ms gap

## Context

Currently using `ToneGenerator.TONE_DTMF_0` (941+1336 Hz dual tone) with 300ms gaps between beeps. User wants 17kHz and 100ms gaps.

## Objective

Two changes:

### 1. Replace ToneGenerator with AudioTrack sine wave generation

`ToneGenerator` doesn't support arbitrary frequencies. Replace it with `AudioTrack` generating a 17kHz sine wave.

In `ChimeReceiver.kt`:
- Remove `ToneGenerator` usage entirely.
- Write a helper function that generates a sine wave PCM buffer at 17kHz for a given duration in ms, and plays it via `AudioTrack` in `MODE_STATIC`.
- Use `AudioTrack.Builder` with `AudioFormat` set to mono, 44100Hz sample rate, PCM 16-bit encoding, and `USAGE_MEDIA` / `CONTENT_TYPE_SONIFICATION`.
- The function should be blocking (write + play + sleep for duration, then stop + release), since it's already called from delayed Handler runnables.
- Actually, since the beeps are scheduled via `Handler.postDelayed`, keep the same pattern: post a runnable that calls `playTone(durationMs)` at each delay offset. The tone playback itself can be fire-and-forget (AudioTrack in static mode plays to completion). Create a new `AudioTrack` per beep to keep it simple — no need to reuse.
- Remove the `toneGenerator` parameter threading through `scheduleBeeps`. Instead, each beep runnable creates its own AudioTrack, writes the buffer, plays, and posts a delayed release.

Wait — simpler approach: pre-generate two buffers (short and long) once at the top of `onReceive`, then each beep runnable just creates an AudioTrack, writes the buffer, and plays. AudioTrack in MODE_STATIC auto-stops when the buffer is exhausted. Release it after the duration via the same handler.

Actually, simplest: just make a top-level `playBeep(durationMs: Int)` function that creates an AudioTrack, writes a 17kHz sine wave of the given duration, calls `play()`, and returns. The track stops itself when the buffer runs out. Leaking the AudioTrack is not ideal, so have it post a delayed release on a handler after `durationMs`.

### 2. Change gap from 300ms to 100ms

In `ChimeReceiver.kt`, change `GAP_BETWEEN_BEEPS_MS` from 300 to 100.

## Constraints

- 44100 Hz sample rate, mono, PCM 16-bit.
- 17000 Hz tone frequency.
- Volume: max (amplitude = Short.MAX_VALUE for the sine wave). Actual volume is controlled by device media volume.
- Keep the existing `scheduleBeeps` pattern with Handler.postDelayed — just swap what each runnable does.
