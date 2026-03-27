# iOS Parity Tree

Last updated: 2026-03-24

Purpose:

- use the Android app as the source-of-truth behavior tree
- prevent missing screens, dialogs, and edge cases during iOS implementation
- separate `UI parity` from `native iOS wiring`

## Root Navigation Tree

1. Splash
2. Jobs
3. Job Detail
4. Material Form
5. Privacy Policy

## 1. Splash

Android reference:

- `app/src/main/java/com/asme/receiving/ui/SplashScreen.kt`

Behavior:

- phase 1 text: `Brought to you by:`
- phase 2 Welders Helper logo
- fade sequence before app shell

iOS status:

- implemented in scaffold

## 2. Jobs

Android reference:

- `app/src/main/java/com/asme/receiving/ui/JobsScreen.kt`

Main surface:

- product logo
- create job button
- divider
- privacy policy link
- loading state
- empty state
- current jobs list

Dialogs / states:

- create new job dialog
  - fields:
    - job number
    - description
    - notes
- delete job confirmation
  - wording changes depending on export state

Per-job row:

- job number link styling
- description
- exported / not exported state
- delete button

iOS status:

- scaffolded with Android-style create dialog fields and delete confirmation wording
- still needs closer loading/empty-state parity and more exact shell spacing polish

Native iOS wiring needed:

- local persistence
- exact export-state logic

## 3. Job Detail

Android reference:

- `app/src/main/java/com/asme/receiving/ui/JobDetailScreen.kt`
- `app/src/main/java/com/asme/receiving/ui/components/MaterialGuardianHeader.kt`

Main surface:

- shared header
  - back button left
  - small Material Guardian logo right
- `JOB DETAILS` title
- job number
- editable description / add description link state
- exported / not exported status
- add receiving report button
- materials received list
- export job button
- latest export folder section
- open export folder button

Dialogs / states:

- edit description
- edit job number
- export confirmation
- export success feedback
- export error feedback

iOS status:

- shell implemented
- shared header mirrored
- edit description, edit job number, export confirm, export success, and export error states are now present
- export/open/share actions are still placeholder/incomplete on iOS

Native iOS wiring needed:

- persistence for job edits
- export implementation
- Files/share/open-folder equivalent behavior for iOS

## 4. Material Form

Android reference:

- `app/src/main/java/com/asme/receiving/ui/MaterialFormScreen.kt`
- `app/src/main/java/com/asme/receiving/ui/CameraCaptureOverlay.kt`
- `app/src/main/java/com/asme/receiving/ui/MaterialFormDraftStore.kt`

Main surface areas:

1. Header
   - back button left
   - small Material Guardian logo right
   - large receiving report title
2. Receiving/basic fields
3. Product/spec fields
4. Inspection toggles and acceptability fields
5. QC / signatures / comments
6. Photos
7. Scans / MTR capture
8. Save / exit behavior

Material form field parity target:

- material description
- vendor
- quantity
- PO number
- product type
- specification prefix / numbers
- grade type
- fitting standard / suffix
- dimensions and units
- markings
- acceptability flags
- MTR flags
- acceptance status
- comments
- QC initials / dates
- QC manager fields
- signatures
- photo counts / scan counts

Dialogs / popups to match:

- max photos reached
- review photo
  - keep
  - retake
  - exit
- review scan
  - keep
  - retake
  - exit
- material saved
- save failed
- media options
  - retake
  - delete
  - cancel
- scan limit reached
- scanner unavailable fallback
- signature dialog
  - save
  - clear
  - cancel
- date picker dialog
- confirm exit dialog
  - keep draft
  - keep editing
  - delete draft

Other behavior to match:

- autosave drafts
- immediate save on background / exit / media transitions
- editing existing material vs new material
- camera/photo workflow separate from document scan workflow

iOS status:

- shell now covers most Android field groups, including fitting, thickness, QC manager, approval, signatures, and media sections
- shared header mirrored
- Android-style draft exit confirmation is present
- local draft persistence is now real and survives relaunches
- save success, save failed, max photo, scan limit, review photo, review scan, media options, scanner fallback, and signature capture flows are now present as iOS-native equivalents
- media capture is still placeholder/parity-only UI, not real camera/document-scanner wiring yet

Native iOS wiring needed:

- camera capture flow
- document scan flow
- export packet generation

## 5. Privacy Policy

Android reference:

- `app/src/main/java/com/asme/receiving/ui/PrivacyPolicyScreen.kt`

Main surface:

- in-app readable privacy policy page
- back navigation

iOS status:

- implemented as an in-app screen with shared header and section cards

Native iOS wiring needed:

- likely simple in-app static screen or web view using repo privacy policy content

## Suite-Level Shell Rules To Preserve

Android/reference docs:

- `docs/welders_helper_suite.md`

Must match:

- Welders Helper umbrella appears before product identity
- parent brand remains visible in app shell
- back button placement consistency
- top-right product logo placement consistency
- explicit design tokens, not default platform theme drift

## Export Contract To Preserve

- iOS export output should match the Android business contract unless intentionally revised.
- Keep the same predictable job-folder concept, adapted to iOS storage/sharing rules.
- Keep stable folder and file naming rules.
- Export one multi-page packet PDF per received material.
- Packet order should remain:
  - receiving report first
  - scan / MTR / CoC pages next
  - photo pages last
- Do not rely on iPhone-native HEIC image output as the handoff format.
- If standalone image export is needed, normalize to JPEG instead of HEIC.
- The exported packet PDF should remain the primary deliverable so downstream users do not need Apple-specific tooling.

## Recommended Build Order

1. Finish screen and header parity for all top-level screens
2. Add dialog/popover parity tree for Jobs and Job Detail
3. Expand Material Form to full field parity
4. Add draft persistence
5. Add media and signature workflows
6. Add export behavior
7. Add privacy policy screen
8. Do real-device polish passes against Android side by side
