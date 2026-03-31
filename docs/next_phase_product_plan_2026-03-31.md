# Next-Phase Product Plan

Date: 2026-03-31

This note captures the next agreed product direction before more Android or iOS implementation begins.

## Draft and new-material behavior

- `Add Material` should always open a fresh blank receiving form.
- A new receiving form should never reopen pre-populated unless the user explicitly opened:
  - an existing saved/completed material, or
  - an explicit draft entry
- If the user exits a new receiving form without saving the final material, the app should still autosave that work as a draft.
- Drafts should be reachable through an explicit draft access point in the job flow.
- Do not silently reuse the latest `__new__` draft when the user taps `Add Material`.

## Export/share behavior

- Keep both export/share shapes available:
  - `Share PDFs`
  - `Share ZIP`
- Do not hide zip behind an automatic fallback that depends on chooser behavior.
- SharePoint-style business workflows are the main reason to keep zip explicitly available.
- Near-term SharePoint support should be handled by reliable export shapes, not by assuming Android folder sharing will work.
- Long-term direct SharePoint/Microsoft upload could be added later, but it is a separate integration project and is not the first move.

## Customization additions

- Add optional customization fields for:
  - default `QC Inspector` printed name
  - default `QC Manager` printed name
- If those values are set, prepopulate the printed-name boxes on new receiving reports.
- Add an option to store a default QC inspector signature.
- If a saved inspector signature exists, tapping the signature button on the receiving form should offer something like:
  - `Apply saved signature for <name>`
  - `Draw new signature`
  - `Clear signature`
- Using the saved signature should speed up repeated reports and reduce signature-entry mistakes.
- A newly drawn signature should not automatically overwrite the saved default unless the user explicitly chooses to save it as the default later.

## iOS / cross-platform parity implications

- The future iOS app should mirror the same product behavior:
  - blank `Add Material`
  - explicit drafts access
  - both PDF and ZIP export choices
  - default printed QC names
  - optional saved inspector signature flow
- Do not let iOS assume that autosaved drafts should reopen through the main create action.

## Pickup guidance

If the next session continues Android product work, read this file first along with:

1. `ios/android-preferences-change-notes-2026-03-30.md`
2. `ios/apple-backend-coordination-2026-03-30.md`
3. `docs/monetization_backend_handoff.md`
