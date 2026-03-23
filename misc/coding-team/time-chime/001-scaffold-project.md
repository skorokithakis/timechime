# Task 001: Scaffold Android project

## Context

Empty repo at `/home/stavros/Code/Android/timechime`. We need a buildable Android project from scratch.

## Objective

Create a minimal but complete Android project structure using Kotlin and Jetpack Compose that compiles and runs, showing a placeholder screen.

## Scope

- `build.gradle.kts` (project-level) with AGP, Kotlin plugin.
- `app/build.gradle.kts` with Compose dependencies, `minSdk 26`, `targetSdk 34`, `compileSdk 34`.
  - `applicationId`: `com.stavros.timechime`
- `settings.gradle.kts` with repository config.
- `gradle.properties` with standard Android/Compose flags.
- Gradle wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`).
- `AndroidManifest.xml` — declare `MainActivity`, plus these permissions (needed in task 002):
  - `FOREGROUND_SERVICE`
  - `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
  - `POST_NOTIFICATIONS`
  - `SCHEDULE_EXACT_ALARM`
  - `USE_EXACT_ALARM`
  - `WAKE_LOCK`
- `MainActivity.kt` — single activity with Compose, just display centered text "Time Chime" for now.
- A basic Compose theme file is fine but keep it minimal.
- `.gitignore` for standard Android ignores.

## Non-goals

- No service, receiver, or any chime logic yet.
- No tests.

## Constraints

- Package: `com.stavros.timechime`
- Use version catalog (`libs.versions.toml`) only if you find it simpler; plain dependency declarations are fine too.
- Must build successfully with `./gradlew assembleDebug`.
