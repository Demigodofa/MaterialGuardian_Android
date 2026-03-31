# Codex Handoff for Material Guardian

Last updated: 2026-03-31

## Project location

- Repo root: `C:\Users\KevinPenfield\source\repos\Demigodofa\MaterialGuardian_Android`
- Remote: `https://github.com/Demigodofa/MaterialGuardian_Android.git`
- Branch: `main`
- Current synced commit: `8be96bf` `Tighten report layout and clean launch splash`

## Current state

- The shipping app in this repo is Android-native Kotlin.
- There is no iOS app built yet.
- Android app work is ahead of the older iOS notes and should be treated as the current product reference.
- This Windows clone is not fully clean because Kevin keeps live handoff/docs notes here; do not assume `ios/` or `AGENTS.md` being modified means Android app code is out of sync.
- The next Codex session should treat Android as the reference implementation and build the Apple version under `ios/`.
- If a newer clone, branch, or Mac working copy already contains iOS files, preserve that work and extend it instead of replacing it with a fresh scaffold.
- This Windows clone currently only has the handoff files under `ios/`; if a newer Mac working copy has unpushed iOS files, pull and push carefully there so an older tree does not overwrite newer Apple work.

## Read first

1. `AGENTS.md`
2. `C:\Users\KevinPenfield\.codex\skills\kevin-codex\SKILL.md`
3. `C:\Users\KevinPenfield\.codex\skills\kevin-codex\references\foundation.md`
4. `C:\Users\KevinPenfield\.codex\skills\kevin-codex\references\web-apps.md`
5. `README.md`
6. `docs/release_handoff.md`
7. `docs/play_release.md`
8. `docs/google_play_submission.md`
9. `docs/welders_helper_suite.md`
10. `www/privacy-policy.html`
11. `docs/monetization_backend_handoff.md`
12. `ios/android-preferences-change-notes-2026-03-30.md`
13. `ios/apple-backend-coordination-2026-03-30.md`

## Review-first workflow

- For this repo, treat rereads, fix-hunting, adjustment ideas, design decisions, live output checks, and "does it actually run" validation as part of a code-review pass first.
- Use regular Codex work for the implementation/editing phase after the review pass identifies the needed change.
- Keep the runtime checks real, but group them under the review umbrella when possible to reduce pure Codex token use.

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

## Android behavior that changed after the first iOS note

- Landing screen now exposes a real `Customization` entry point.
- Customization/preferences currently drive:
  - whether ASME B16 receiving fields appear
  - whether surface-finish fields appear
  - which fixed surface-finish unit is shown on the receiving form
  - whether a company logo is embedded in exported reports
- The receiving form still keeps the `Imperial` / `Metric` material-level choice on-device.
- New-material form entry was hardened so starting a new receiving report should reset cleanly instead of restoring the last abandoned `__new__` draft.
- Photo previews were corrected to respect image orientation while exported photos already retained proper orientation.
- Export PDF layout was tightened repeatedly to keep the receiving report on one page and keep lower sections aligned.
- Android launch behavior was cleaned so the app should go straight into the intended splash flow instead of briefly showing the launcher icon first.

## Important Android details to preserve when porting

- Draft safety was hardened recently; interrupted receiving-report sessions should not lose user work
- Local data is intentionally kept on-device; cloud sync/export is deferred
- Export/share behavior was adjusted to work better with real-world share targets like SharePoint
- Optional report/logo/customization behavior is now part of the product, not just an Android experiment
- The Android team is already thinking ahead to backend-driven auth, seat control, trials, and cross-platform entitlement handling; do not hard-code store-only account assumptions on iOS
- Release signing and Play setup exist for Android only and should not be copied into iOS signing
- Version nomenclature should stay aligned across platforms: use `major.minor.patch` with no `v` prefix for the user-facing app version, keep Android `versionCode` and iOS build number as increasing integers, and use operational release labels like `1.0.2 (3) - Export Fixes` or `1.0.2 (3) Internal - Export Fixes`

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

`Open C:\Users\KevinPenfield\source\repos\Demigodofa\MaterialGuardian_Android and read ios/Codex Handoff for Material Guardian.md first. Use the Android app under app/ as the behavior reference, keep new Apple work under ios/, and help plan or build an iOS version of Material Guardian without touching Android signing files.`

Also tell Codex that if there is already iOS work in this clone, branch, or another newer Mac copy, it should preserve and extend that work instead of replacing it with a fresh scaffold. Have it also check `C:\Users\KevinPenfield\.codex\skills\kevin-codex\` on this PC to pick up Kevin-machine workflow context and see whether any durable guidance or legacy workflow notes should be updated as the iOS work progresses.
