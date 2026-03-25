# Release Handoff

Last updated: 2026-03-23

## Current status

Material Guardian is now in a release-hardened state on this machine.

Verified:

- local JVM tests passed
- lint passed
- release app bundle build passed
- connected Android instrumented tests passed on a Samsung phone
- receiving report draft persistence was hardened so interrupted form sessions are not lost

Current release bundle:

- `app/build/outputs/bundle/release/app-release.aab`
- Current Android version target after export fixes: `versionName 1.0.2`, `versionCode 3`

## Versioning convention

- App version labels use `major.minor.patch` with no `v` prefix.
- Android Play builds use an incrementing integer `versionCode`.
- Play release names use `major.minor.patch (build) - summary`.
- Internal-track example: `1.0.2 (3) Internal - Export Fixes`
- Production example: `1.0.2 (3) - Export Fixes`

## What changed in this hardening pass

- Replaced Room destructive fallback with explicit migration wiring in `AppDatabaseProvider.kt` and `AppDatabaseMigrations.kt`.
- Added Play upload signing support through `release-signing.properties` or `MG_*` environment variables in `app/build.gradle.kts`.
- Generated a local upload keystore under `keystore/` and a local ignored `release-signing.properties`.
- Disabled backup/device-transfer of local inspection data through manifest and XML backup rules.
- Changed receiving report flow to autosave drafts continuously, save immediately on app background/exit/media transitions, and present `Keep Draft` or `Delete Draft` on exit.
- Cleaned the app-owned lint issues that were blocking release confidence.

## Important files

- Signing guide: `docs/play_release.md`
- Play submission packet: `docs/google_play_submission.md`
- Local signing config: `release-signing.properties` (ignored)
- Local upload keystore: `keystore/material-guardian-upload.jks` (ignored)
- Release bundle: `app/build/outputs/bundle/release/app-release.aab`
- Release config: `app/build.gradle.kts`
- Draft persistence: `app/src/main/java/com/asme/receiving/ui/MaterialFormScreen.kt`
- Draft store: `app/src/main/java/com/asme/receiving/ui/MaterialFormDraftStore.kt`
- Database safety: `app/src/main/java/com/asme/receiving/data/local/AppDatabaseProvider.kt`
- Public privacy policy draft: `www/privacy-policy.html`

## Verification snapshot

Successful commands during this pass:

- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat lint`
- `.\gradlew.bat bundleRelease`
- `.\gradlew.bat connectedDebugAndroidTest`

Observed results:

- JVM tests: 9 passed, 0 failed, 2 ignored export JVM tests
- Connected Android tests: 10 passed, 0 failed

## Remaining non-code work for Play Console

- app listing copy
- screenshots / feature graphic / icon review
- Data safety questionnaire
- content rating
- app access / internal testing track setup
- Play App Signing enrollment and first upload

## Pickup guidance for a fresh Codex session

Read these first:

1. `AGENTS.md`
2. `docs/release_handoff.md`
3. `docs/play_release.md`
4. `docs/google_play_submission.md`

Then verify:

1. `app/build/outputs/bundle/release/app-release.aab` exists
2. `release-signing.properties` exists locally
3. `.\gradlew.bat bundleRelease`
4. `.\gradlew.bat connectedDebugAndroidTest` with a connected phone

If Play upload fails, check signing setup before touching app code.
