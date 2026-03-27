# Material Guardian iOS

This folder is the intended landing area for the iOS version of Material Guardian so Android and iOS work can stay in one repository.

Current state:

- The shipping app in this repo is Android-native Kotlin under `app/`.
- A native SwiftUI iOS scaffold now exists under `MaterialGuardianIOS/`.
- `project.yml` is the source-of-truth project definition for XcodeGen.
- `MaterialGuardianIOS.xcodeproj` can be regenerated from `project.yml`.
- Persistence, export, media capture, scans, signatures, and store-ready signing are not implemented yet.
- The future iOS app should use this repo's existing product references first:
  - `README.md`
  - `AGENTS.md`
  - `docs/release_handoff.md`
  - `docs/google_play_submission.md`
  - `www/privacy-policy.html`
  - Android UI/assets under `app/`, `assets/`, and `www/`

Recommended iOS handoff inputs:

- screen flow and wording from the Android app
- export behavior and folder/package rules
- local-first data model and validation rules
- privacy policy and store-listing language where still applicable
- icons, colors, and suite branding references

Keep Apple-specific signing material, provisioning profiles, and secrets out of git just as with the Android signing files.

## Scaffold commands

Generate the Xcode project:

```bash
xcodegen generate
```

Build from this folder after the required iOS simulator platform is installed in Xcode:

```bash
xcodebuild -project MaterialGuardianIOS.xcodeproj -scheme MaterialGuardianIOS -sdk iphonesimulator build
```

Latest known result:

- simulator build succeeded on 2026-03-24 after installing the iOS 26.4 simulator runtime
