# iOS Handoff

Last updated: 2026-03-26

## Current State

- The repository now contains an initial native SwiftUI iOS scaffold under `ios/MaterialGuardianIOS/`.
- The repo-managed Xcode project definition lives in `ios/project.yml`.
- `xcodegen generate` creates `ios/MaterialGuardianIOS.xcodeproj`.
- `xcodebuild -project MaterialGuardianIOS.xcodeproj -scheme MaterialGuardianIOS -sdk iphonesimulator build` succeeded on 2026-03-24 after installing the iOS 26.4 simulator platform.
- Full Xcode is now installed on this Mac.
- Apple account sign-in in Xcode has been completed.
- `mas` and Homebrew are also installed on this Mac, but they are optional helpers rather than blockers.
- Jobs and materials now persist locally on disk in the iOS scaffold.
- Material Form drafts now persist locally on disk and survive relaunches.
- Material Form now includes parity-focused iOS dialogs for save success/failure, draft exit, media review/options, scan fallback, and signature capture.
- Material photos and scans now persist as real local files on device storage.
- Job export now generates packet PDFs and a manifest under a stable local export folder.
- Signature capture now persists stroke data plus PNG signature assets for export use.
- A simulator build succeeded again on 2026-03-26 after media/export/signature wiring updates.
- Git branch `codex/ios-media-export` is pushed with commit `bd73c44` as the first remote iOS checkpoint.

## Product Intent

The iOS version of Material Guardian should follow the existing Android product behavior as closely as practical while staying native to Apple platforms.

Branding note:

- Material Guardian is a child app in the Welders Helper suite, not a standalone brand.
- The parent Welders Helper identity should appear before or alongside product identity in the iOS shell, matching the Android splash and suite rules.

Export note:

- iOS should preserve the Android export contract as closely as possible.
- The primary deliverable remains one multi-page packet PDF per received material.
- Export naming and folder rules should stay stable and deliberate.
- Avoid HEIC as a downstream handoff format; normalize image export to JPEG if image files are exported separately.

## Existing Reference Inputs

- `README.md`
- `AGENTS.md`
- `docs/release_handoff.md`
- `docs/google_play_submission.md`
- `docs/ios_parity_tree.md`
- `docs/welders_helper_suite.md`
- `www/privacy-policy.html`
- Android source and assets under `app/`, `assets/`, and `www/`

## Recommended Starting Point

- Use a native SwiftUI app.
- Keep iOS work in this repository so Android and iOS product context stay together.
- Treat the current Android app as the behavior reference, not a code-sharing base.
- Use the current scaffold as the shell for jobs, job detail, and receiving report workflows.

## First Practical Tasks

1. Confirm the bundle identifier and development team in Xcode.
2. Continue validating camera/photo-library/document-scanner flows in Simulator and on device.
3. Tighten export/open/share behavior against real iOS file-handling expectations.
4. Continue top-level polish against Android flow and Welders Helper splash-to-landing branding.
5. Prepare App Store privacy metadata and submission-safe copy once the user-facing flow is stable.

## Notes

- `iTerm` is installed on this Mac.
- Full Xcode, not `iTerm`, is the essential Apple development tool.
