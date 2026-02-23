# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Native Android app (Kotlin) using MVVM architecture with Navigation Component. The project is a Bottom Navigation Activity template with three tabs — actual MPG calculation logic has not yet been implemented.

- **Package**: `com.example.mpgcalculator`
- **Min SDK**: 24, **Target SDK**: 35
- **Kotlin**: 2.0.21, **AGP**: 8.10.1

## Build Commands

```bash
# Build debug APK
./gradlew build

# Install on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run a specific test class
./gradlew test --tests "com.example.mpgcalculator.ExampleUnitTest"
```

## Architecture

**MVVM + Navigation Component + View Binding**

Each feature tab follows this pattern:
- `ui/<feature>/FeatureFragment.kt` — observes LiveData from ViewModel, manages binding lifecycle
- `ui/<feature>/FeatureViewModel.kt` — holds UI state via `MutableLiveData`

`MainActivity.kt` hosts the `NavHostFragment` and wires `BottomNavigationView` to the nav controller.

Navigation is defined in `res/navigation/mobile_navigation.xml`. View Binding is enabled project-wide — never use `findViewById`.

## Key Dependencies

Managed via `gradle/libs.versions.toml`:
- **Navigation**: `androidx.navigation:navigation-fragment-ktx` / `navigation-ui-ktx` 2.6.0
- **Lifecycle**: `lifecycle-livedata-ktx` / `lifecycle-viewmodel-ktx` 2.9.1
- **UI**: AppCompat 1.7.1, ConstraintLayout 2.2.1, Material 1.12.0
- **Testing**: JUnit 4.13.2, Espresso 3.6.1

Add new dependencies using version catalog aliases in `gradle/libs.versions.toml`, then reference them as `libs.<alias>` in `app/build.gradle.kts`.
