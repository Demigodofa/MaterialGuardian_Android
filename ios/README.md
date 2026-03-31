# Material Guardian iOS

This folder is the intended landing area for the iOS version of Material Guardian so Android and iOS work can stay in one repository.

Start with:

- `ios/Codex Handoff for Material Guardian.md`

Review/validation preference:

- Use a code-review pass first when rereading code, searching for fixes, exploring adjustment ideas, making design decisions, checking live outputs, or confirming the iOS-side process/build actually runs.
- Use regular Codex work for the implementation/editing phase after that review pass.

Current state:

- The shipping app in this repo is Android-native Kotlin under `app/`.
- There is no iOS implementation in this folder yet.
- The Android reference implementation has moved materially since the first iOS handoff note:
  - customization/preferences now control optional B16 usage, optional surface-finish fields, and company-logo inclusion on exported reports
  - the receiving form still keeps per-material `Imperial` / `Metric`
  - export/report layout and launch splash behavior were tightened through recent Android validation on a real phone
- If another clone or branch already has iOS files, preserve and extend that work rather than replacing it with a new scaffold.
- This Windows clone currently only contains the iOS handoff docs; if a newer Mac working copy has unpushed Apple files, reconcile there carefully so older files do not overwrite newer work.
- The future iOS app should use this repo's existing product references first:
  - `README.md`
  - `AGENTS.md`
  - `C:\Users\KevinPenfield\.codex\skills\kevin-codex\`
  - `docs/release_handoff.md`
  - `docs/monetization_backend_handoff.md`
  - `docs/next_phase_product_plan_2026-03-31.md`
  - `docs/google_play_submission.md`
  - `www/privacy-policy.html`
  - Android UI/assets under `app/`, `assets/`, and `www/`
  - iOS coordination notes already in this folder:
    - `ios/android-preferences-change-notes-2026-03-30.md`
    - `ios/apple-backend-coordination-2026-03-30.md`

Recommended iOS handoff inputs:

- screen flow and wording from the Android app
- export behavior and folder/package rules
- local-first data model and validation rules
- the newer Android customization/preferences behavior, optional receiving sections, logo workflow, and launch/splash expectations
- the next-phase draft/export/customization plan in `docs/next_phase_product_plan_2026-03-31.md`
- privacy policy and store-listing language where still applicable
- icons, colors, and suite branding references

Keep Apple-specific signing material, provisioning profiles, and secrets out of git just as with the Android signing files.
