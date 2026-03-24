# Google Play Submission Packet

Last updated: 2026-03-23

## Repo/build status

Submission build status on this machine:

- signed release bundle exists at `app/build/outputs/bundle/release/app-release.aab`
- package name: `com.asme.receiving`
- version name: `1.0.1`
- version code: `2`
- `targetSdk`: `36`
- tests/lint/release bundle were previously verified in `docs/release_handoff.md`

## What is still required outside the repo

The codebase is effectively Play-ready. The remaining work is Play Console material:

1. Create or confirm the Play Console app entry for `com.asme.receiving`.
2. Upload the signed `.aab`.
3. Add a public privacy-policy URL.
4. Add phone screenshots.
5. Add a feature graphic.
6. Complete the Data safety form.
7. Complete the content rating questionnaire.
8. Complete the app access and testing-track steps.
9. Submit for review.

## Store listing draft

App name:

- `Material Guardian`

Short description draft:

- `Offline receiving inspection reports with photos, scans, signatures, and PDF export.`

Full description draft:

`Material Guardian helps field and shop teams document receiving inspections on Android without depending on a live connection. Create jobs, enter material-by-material receiving reports, capture material photos, scan MTR/CoC documents, collect signatures, and export organized packet PDFs for each job.`

`Built for real receiving workflows, Material Guardian keeps the work explicit and local-first. Reports are saved on-device, interrupted report entry is autosaved as a draft, and completed jobs can be exported to a predictable folder under Downloads for handoff or sharing.`

`Current capabilities include:`

- create and manage local jobs
- enter receiving inspection reports per material
- capture up to 4 material photos per material
- scan up to 8 MTR/CoC pages per material
- capture QC signatures
- export receiving packets to PDF and device storage
- keep working offline on-device

## Data safety working draft

This draft is based on the current app behavior in the repo as of 2026-03-23.

Likely declaration:

- user data collected by the developer or developer-controlled backend: `No`
- user data shared with third parties by the developer/app libraries: `No`
- data is processed and stored on-device for core app functionality: `Yes`

Reasoning:

- the current release is local-first and does not include active cloud sync
- app data backup/device transfer is disabled
- the manifest only requests camera permission
- export/share flows are user-initiated device actions, not developer backend collection

Important caution:

- review the final Data safety answers carefully in Play Console before submitting. If any future build adds analytics, crash reporting, login, cloud sync, or backend upload, these answers change.

Reference:

- Google says all Play apps must complete the Data safety form, even apps that do not collect user data, and must provide a privacy-policy link: https://support.google.com/googleplay/android-developer/answer/10787469?hl=en

## Content rating working draft

Expected outcome: low maturity / suitable for general business use, assuming no user-generated public content, chat, dating, gambling, or social feed features are added.

Reasoning:

- no violence, gambling, sexual content, or public user interaction features were found
- this is a field/business documentation app

Reference:

- Google requires the content rating questionnaire for each new app and removes unrated apps: https://support.google.com/googleplay/android-developer/answer/9898843?hl=en

## Privacy policy

In-app coverage:

- the app now includes an in-app privacy-policy screen linked from the jobs screen

Public URL requirement:

- Play Console still needs a public privacy-policy URL
- host `www/privacy-policy.html` on your website, GitHub Pages, or another public HTTPS location and use that URL in Play Console

## Asset checklist

Present in repo:

- app icon assets under `assets/icons/` and `www/assets/icons/`

Still needed:

- phone screenshots from the actual app
- 1024x500 feature graphic
- optional promo graphic/video if desired

## Suggested release notes

`Initial Google Play release of Material Guardian with offline receiving inspection workflows, autosaved receiving-report drafts, material photo capture, MTR/CoC scan capture, signatures, and PDF export.`
