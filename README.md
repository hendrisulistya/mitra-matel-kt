# Mitra Matel — Android app (Jetpack Compose)

This project is now an Android-only application built with Jetpack Compose. The previous Kotlin Multiplatform (KMP) setup has been removed and sources were moved from `composeApp` to the `app` module.

## Project structure

- `app/`
  - `build.gradle.kts` — Android application module (Compose enabled)
  - `src/main/AndroidManifest.xml`
  - `src/main/kotlin/app/mitra/matel/` — Kotlin source (MainActivity, Composables)
  - `src/main/res/` — resources (layouts, drawables, values)
- Root Gradle files:
  - `settings.gradle.kts` includes `:app`
  - `gradle/libs.versions.toml` — version catalog for dependencies and plugins

## Requirements

- Android SDK installed
- Set SDK path in `local.properties`:
  - On macOS: `sdk.dir=$HOME/Library/Android/sdk`
- JDK 11+ (the project targets Java 11)

## Build and Run

- Build debug APK (macOS/Linux):
  ```bash
  ./gradlew :app:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…