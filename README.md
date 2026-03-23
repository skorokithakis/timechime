# Time Chime

An Android app that chimes every 10 minutes to help you keep track of time.

## How it works

- Tap **Start** to begin. The app runs a foreground service that fires every 10
  minutes on the clock (:00, :10, :20, :30, :40, :50).
- At the **top of the hour** (:00), it plays one long beep.
- At **:10 through :50**, it plays 1–5 short beeps corresponding to how many
  tens of minutes past the hour it is (e.g., 3 beeps at :30).
- Tap **Stop** (in the app or from the notification) to stop.

The tone is a 15 kHz sine wave, chosen to be subtle and inaudible to most
people nearby.

## Building

```
./gradlew assembleDebug
```

## Requirements

- Android 8.0+ (API 26)
- Kotlin, Jetpack Compose
