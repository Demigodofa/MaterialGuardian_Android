# Codex Handoff for Material Guardian

Last updated: 2026-03-25

## Project location

- Repo root: `C:\Users\KevinPenfield\source\repos\Demigodofa\MaterialGuardian_Android`
- Remote: `https://github.com/Demigodofa/MaterialGuardian_Android.git`
- Branch: `main`
- Current synced commit: `a2ac042` `Harden release flow and add iOS handoff folder`

## Current state

- The shipping app in this repo is Android-native Kotlin.
- There is no iOS app built yet.
- The repo is clean and `main` is tracking `origin/main`.
- The next Codex session should treat Android as the reference implementation and build the Apple version under `ios/`.
- If a newer clone, branch, or Mac working copy already contains iOS files, preserve that work and extend it instead of replacing it with a fresh scaffold.

## Read first

1. `AGENTS.md`
2. `README.md`
3. `docs/release_handoff.md`
4. `docs/play_release.md`
5. `docs/google_play_submission.md`
6. `docs/welders_helper_suite.md`
7. `www/privacy-policy.html`

## Where the important files are

### Android reference implementation

- App source: `app/src/main/java/com/asme/receiving/`
- UI screens: `app/src/main/java/com/asme/receiving/ui/`
- Local data / Room: `app/src/main/java/com/asme/receiving/data/local/`
- Export logic: `app/src/main/java/com/asme/receiving/data/export/`
- Resources: `app/src/main/res/`
- Bundled assets: `app/src/main/assets/`

### Documentation and store materials

- Release handoff: `docs/release_handoff.md`
- Play release/signing notes: `docs/play_release.md`
- Play submission notes: `docs/google_play_submission.md`
- Suite styling/UX rules: `docs/welders_helper_suite.md`
- Privacy policy: `www/privacy-policy.html`
- Additional product/site assets: `assets/` and `www/`

### iOS work area

- Put Apple-specific code and docs under `ios/`
- Keep Android and iOS in the same repo, but do not mix build systems or signing files
- If `ios/` already contains Swift/Xcode project files in another environment, treat them as authoritative in-progress work and avoid destructive rewrites

## Behavior the iOS version should match

- Offline-first local workflow
- Jobs list and job detail flow
- Receiving inspection report form
- Material list with create/edit flow
- Up to 4 photos per material
- Up to 8 scans per material
- Local export behavior centered on one job folder and one packet PDF per material
- Privacy-policy and store wording where still applicable

## Important Android details to preserve when porting

- Draft safety was hardened recently; interrupted receiving-report sessions should not lose user work
- Local data is intentionally kept on-device; cloud sync/export is deferred
- Export/share behavior was adjusted to work better with real-world share targets like SharePoint
- Release signing and Play setup exist for Android only and should not be copied into iOS signing

## Local-only files and folders

These help on Kevin's PC but should not drive iOS architecture:

- `release-signing.properties`
- `keystore/`
- `local.properties`
- `.gradle/`
- `.gradle-user-home/`
- `.android-user-home/`
- `.idea/`
- `.idx/`
- `.kotlin/`
- `.tmp/`

## Suggested starting point for the next Codex session

Ask Codex to:

`Open C:\Users\KevinPenfield\source\repos\Demigodofa\MaterialGuardian_Android and read ios/Codex Handoff for Material Guardian.md first. Use the Android app under app/ as the behavior reference, keep Apple work under ios/, and if there is already iOS work in this clone, branch, or another newer Mac copy, preserve and extend it instead of replacing it. Do not touch Android signing files.`
