# Material Guardian iOS

This folder is the intended landing area for the iOS version of Material Guardian so Android and iOS work can stay in one repository.

Start with:

- `ios/Codex Handoff for Material Guardian.md`

Current state:

- The shipping app in this repo is Android-native Kotlin under `app/`.
- There is no iOS implementation in this folder yet.
- If another clone or branch already has iOS files, preserve and extend that work rather than replacing it with a new scaffold.
- This Windows clone currently only contains the iOS handoff docs; if a newer Mac working copy has unpushed Apple files, reconcile there carefully so older files do not overwrite newer work.
- The future iOS app should use this repo's existing product references first:
  - `README.md`
  - `AGENTS.md`
  - `C:\Users\KevinPenfield\.codex\skills\kevin-codex\`
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
