# Task 006: App launcher icon

## Context

The app currently has no custom icon — it uses the default Android icon.

## Objective

Create a simple, clean adaptive icon for the app. The design should evoke "time chime" — a bell or clock concept.

## Scope

Create an adaptive icon using vector drawables:

1. **`app/src/main/res/drawable/ic_launcher_foreground.xml`** — Vector drawable for the foreground layer. Design: a simple bell icon, centered on the 108dp adaptive icon canvas (the safe zone is the inner 72dp). Use a white or light-colored bell shape. Keep it simple — no fine details, just a recognizable bell silhouette.

2. **`app/src/main/res/drawable/ic_launcher_background.xml`** — Vector drawable for the background layer. A solid color fill — use a nice teal/blue-green (#00897B or similar).

3. **`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`** — Adaptive icon XML pointing to the foreground and background drawables.

4. **`app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`** — Same as above (Android uses this for round icon masks).

5. **`app/src/main/res/values/ic_launcher_background.xml`** — Define the background color as a color resource if needed.

6. Update `AndroidManifest.xml` to set `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"` on the `<application>` tag.

7. Remove any existing default `mipmap-*` PNG icon directories if they exist (they shouldn't, but clean up if present).

## Non-goals

- No PNG rasterizations for older densities — adaptive icons with vector drawables on minSdk 26 is sufficient.
- No splash screen or themed icon.

## Constraints

- Adaptive icon canvas is 108x108dp. Safe zone for the foreground is the center 72x72dp.
- Both layers must be 108x108dp viewBox.
