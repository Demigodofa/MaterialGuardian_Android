# Repository Guidelines

## Project Structure & Module Organization
MaterialGuardian is a single-module Android app.
- `app/`: main Android module.
  - `app/src/main/java/com/asme/receiving/`: Kotlin source (data layer, Room, export, and Compose UI).
  - `app/src/main/res/`: resources (themes, strings, launcher icons).
  - `app/src/main/assets/`: bundled PDFs/templates.
  - `app/src/test/`: JVM unit tests.
  - `app/src/androidTest/`: instrumented and Compose UI tests.
- `docs/`: product specs and future add-on notes.
- `docs/welders_helper_suite.md`: suite-level rules for branding, flow, storage/export, and cross-app consistency.
- `assets/` and `www/`: static assets used for documentation and distribution.

## Build, Test, and Development Commands
Use the Gradle wrapper from the repo root:
- `./gradlew assembleDebug`: build a debug APK.
- `./gradlew installDebug`: install the debug build on a connected device.
- `./gradlew testDebugUnitTest`: run local JVM tests (JUnit/Robolectric).
- `./gradlew connectedDebugAndroidTest`: run instrumented tests on a device/emulator.
- `./gradlew lint`: run Android Lint checks.
- `./gradlew bundleRelease`: build the Play upload `.aab`.

Android Studio is the default workflow; Java 17 and Android SDK 36+ are required.
For product/UX changes, read `docs/welders_helper_suite.md` before proposing a new shell or interaction pattern.

## Coding Style & Naming Conventions
- Kotlin source uses standard Android/Kotlin style: 4-space indentation, trailing commas where helpful, and clear Compose function naming.
- Classes/Composables: `PascalCase` (e.g., `JobDetailScreen`).
- Functions/vars: `camelCase`; constants: `UPPER_SNAKE_CASE`.
- Packages are lowercase (e.g., `com.asme.receiving.data.export`).

## Testing Guidelines
- Unit tests live under `app/src/test/` and use JUnit4 with Robolectric/AndroidX test utilities.
- Instrumented and UI tests live under `app/src/androidTest/` and use Compose test APIs.
- Name tests `*Test.kt` and keep UI tests focused on visible behavior (text, enabled/disabled states).

## Commit & Pull Request Guidelines
- Recent commits use short, imperative summaries (e.g., "Update dependencies and improve export compatibility"); follow that style unless a team convention is introduced.
- PRs should include: a brief summary, testing notes (commands and results), and screenshots/GIFs for UI changes. Link related issues when available.

## Configuration & Security Notes
- `local.properties` holds local SDK paths; do not commit machine-specific values.
- `docs/future_add_ons/google-services.json` is archival only; production configs should not be added without review.

## Recent Changes (Tooling Alignment)
- What changed: aligned AGP/Kotlin/KSP and Gradle wrapper for a stable toolchain, fixed theme resource API-25 override for lint, updated AndroidX test libs, and adjusted one instrumented test signature.
- Why: consistent versions eliminate KSP/Room compiler errors; API-25 override resolves lint NewApi; Android 16 emulator required newer test libs; test signature kept UI tests compiling.
- Next steps: commit changes, keep emulator/device handy for `./gradlew connectedDebugAndroidTest`, and review lint warnings in `app/build/reports/lint-results-debug.html` if you want cleanup.

## Recent Changes (Material Guardian Build Notes)
- Current debug APK path: `app/build/outputs/apk/debug/app-debug.apk`
- Current release bundle path: `app/build/outputs/bundle/release/app-release.aab`
- On this machine, a successful `adb install` can still be followed by an immediate `am start` failure (`Activity class ... does not exist`) until Android finishes settling the package; verify with `pm list packages` / `cmd package resolve-activity` and then retry launch.
- `connectedDebugAndroidTest` is still a required confidence check for export/layout/device behavior, but it can leave the debug APK missing or the device disconnected afterward; reinstall and relaunch are normal follow-up steps.
- Material Guardian export now assumes:
  - one job folder under `Downloads/MaterialGuardian/<job>/`
  - one packet PDF per material under `material_packets/`
  - share flow uses a bundled zip from app-private `exports/` storage because SharePoint may not appear for multi-file PDF shares even when installed.

## Release Handoff
- Start any fresh release-readiness session by reading `docs/release_handoff.md` and `docs/play_release.md`.
- For Play Console work, also read `docs/google_play_submission.md`.
- The repo now supports local Play upload signing through `release-signing.properties` or `MG_*` environment variables.
- The local upload keystore and release signing properties are intentionally ignored by git.
- Receiving report draft safety was hardened: interrupted form sessions now autosave drafts and exit offers `Keep Draft` or `Delete Draft`.
