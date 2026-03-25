# Welders Helper Suite

This document is the suite-level operating reference for Welders Helper Android apps.

Use it when building, revising, or reviewing any app that ships under the Welders Helper umbrella.

## Purpose

Welders Helper is not a single app. It is a suite of field-use Android apps that should feel related, deliberate, and easy to trust on a jobsite.

Each app may solve a different workflow, but the user should immediately recognize:
- the same umbrella identity
- the same interaction quality
- the same visual language
- the same export and storage expectations

## Current Suite Members

- `Flange Helper`
  - Flange assembly / torque workflow
  - Form-driven data entry
  - Optional photos and signatures
  - PDF report export

- `Material Guardian`
  - Receiving inspection workflow
  - Material-by-material inspection forms
  - Material photos and MTR / CoC scan capture
  - Signature capture should reuse the proven Flange Helper pattern
  - Job package export to local storage / Downloads

## Suite Rules

### Branding

- Every app is "brought to you by Welders Helper".
- The umbrella identity appears before the product identity on startup.
- The splash should use the Welders Helper fade-in sequence before handing off to the app-specific logo or home screen.
- Each product keeps its own icon/logo, but the parent brand should be visible in the app shell.
- Launcher icons should use the product's prepared square icon asset when that reads better than the default adaptive icon crop on Samsung phones.

### Navigation and Shell

- Start screen / jobs screen should follow one family of patterns across the suite.
- Back button treatment should be consistent across apps.
- Top-right logo placement should be consistent across apps.
- Empty states, section headers, card spacing, and primary button styling should feel like siblings, not separate products.

### Design System

- Do not rely on default dynamic Material colors for suite identity.
- Use explicit design tokens for:
  - screen background
  - card background
  - divider
  - primary action
  - edit / neutral actions
  - export / success actions
  - delete / destructive actions
  - primary / secondary / muted text
- Prefer one shared visual grammar over app-by-app improvisation.

### Workflow Style

- Apps should be form-driven, explicit, and field-usable.
- Buttons should say exactly what they do.
- Use clear step transitions instead of hidden state changes.
- Keep the main workflow obvious for gloved, distracted, or time-constrained users.
- Favor visible confirmation and save/export feedback over silent success.

### Storage and Export

- Data should be usable offline by default.
- App-private storage is the working store; user-visible export goes to a predictable Downloads subfolder.
- Export location, filenames, and package structure should be deliberate and stable.
- Users should be able to understand what was exported without opening source code.
- For Material Guardian, the preferred export unit is one multi-page packet PDF per material inside the job folder.
- Packet page order should be:
  - receiving report first
  - full-size MTR / CoC scan pages next
  - 4-up photo pages last
- If the app offers a share action, default to export shapes that Android business apps reliably accept, such as one bundled zip or one packet PDF, instead of assuming folder shares or multi-file PDF shares will appear in every target app.
- If media is captured, export should preserve the distinction between:
  - photos
  - scans / PDFs
  - generated reports

### Release and Version Naming

- Use one versioning scheme across Welders Helper apps and platforms.
- Public app version should use `major.minor.patch` with no `v` prefix, for example `1.0.2`.
- Android `versionCode` and iOS build number should be monotonically increasing integers and should stay aligned when practical for the same shipped milestone.
- Play Console / TestFlight / internal release labels should use the pattern `major.minor.patch (build) - summary`, for example `1.0.2 (3) - Export Fixes`.
- Add the track name only when it helps humans distinguish test-only releases, for example `1.0.2 (3) Internal - Export Fixes`.
- Keep the summary short, plain, and professional. Avoid slang, dates unless needed, and the `v` prefix.

### Media Capture

- Photo capture and document scanning are separate workflows and must stay separate in the data model and UI.
- The user should always understand whether they are:
  - taking a photo
  - scanning a document to PDF
  - retaking / replacing an existing item
- Thumbnail behavior, limits, and replacement rules should be predictable.

### Quality Bar

- "Feature exists" is not enough.
- A Welders Helper app is not considered ready when:
  - layout feels inconsistent
  - export works only sometimes or unclearly
  - photo and scan behavior is confusing
  - the app shell does not match the suite

## Computer-Side Function

Future Codex should understand the development posture:

- Android Studio is the normal IDE, but terminal work can be done outside Android Studio.
- Use Gradle wrapper commands from repo root for build/test/install.
- Prefer testing on a real Android phone before optimizing for Google Play release.
- First make the app reliable as a direct-install debug/release APK.
- Then prepare Play-ready signing and AAB packaging.
- During active dev, avoid unnecessary uninstall/reinstall cycles because local jobs/forms live in app data and can be wiped even when export is working correctly.

## Product Development Order

For suite apps, prefer this order:

1. Core workflow works on-device.
2. Suite shell and branding match Welders Helper.
3. Storage and export are reliable and understandable.
4. Media capture behavior is clean and predictable.
5. Real-device testing is solid.
6. Release packaging and store submission come after product quality.

## Material Guardian Retrofit Direction

Material Guardian should be aligned to Flange Helper as the current reference app for:
- splash behavior
- tokenized colors
- header treatment
- jobs screen layout
- button hierarchy
- card spacing
- export feedback quality

Material Guardian should not stay on scattered inline styling if Flange Helper already has a stronger suite pattern.

## Documentation Rule

When a Welders Helper app improves in a durable way, record the pattern in repo docs or the maintained `.codex` layer so future sessions do not rediscover the same decisions.
