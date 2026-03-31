# Monetization And Backend Handoff

Date: 2026-03-30

This note captures the current monetization direction for Material Guardian so future Android, backend, and iOS sessions do not rediscover the same product decisions.

Authoritative location note:

- the backend/monetization source of truth now lives in `app-platforms-backend/docs/material_guardian_monetization_source_of_truth.md`
- if this file and the backend repo ever differ, the backend repo wins

## Current product direction

Material Guardian is moving from a local-only paid utility toward a cross-platform, account-based product with:

- individual paid access
- business/admin paid access
- seat management
- free trial limits
- one active signed-in device per user at a time
- cross-platform entitlement recognition between Apple and Android

Client direction note:

- the long-term client target is now a shared Flutter app for Android and iPhone
- the current Kotlin Android app remains the behavior reference until that migration exists
- backend design should not assume separate native clients as the long-term destination
- active repo split:
  - `MaterialGuardian_Android` for the current shipping/reference app
  - `material-guardian-mobile` for the future shared Flutter client
  - `app-platforms-backend` for the shared backend

## Core business model in mind

- Individual plan:
  - `$9.99 / month`
  - `$99.99 / year`
  - annual plan should show savings versus 12 monthly payments
- Business plan:
  - `$49.99 / month`
  - `$499.99 / year`
  - includes up to `5 seats`
  - annual plan should show savings versus 12 monthly payments
- Current savings math to preserve in pricing UI and backend plan metadata:
  - individual annual saves `$19.89`
  - business annual saves `$99.89`
- There is no final one-time purchase model defined right now.
- One user can own unlimited devices, but can only be actively logged in on one device at a time.
- If the same user signs into a second device, the app should ask whether to sign out the old device and move the active session to the new one.
- Re-login on another device should allow the user to recover their data.
- Trial users should get:
  - `2` job creations free
  - unlimited material receiving inside those jobs
- Trial messaging should warn early:
  - when starting the second job, show something like `1 of 2 free trial jobs has been used`

## Important architecture decision

This cannot be solved cleanly as store-only logic inside the mobile apps.

The stores are payment rails.
The backend must become the source of truth for:

- accounts
- organizations
- admins
- seats
- invited users
- active sessions
- trial usage
- cross-platform entitlements
- synced data if users are expected to recover work on a different device

## Recommended account and access model

- Every human user gets an app account.
- Every business/customer gets an organization record.
- Organizations own seat entitlements.
- Admins can:
  - invite users
  - resend login code
  - delete user
  - see seat usage
- Invited users should likely be created with:
  - name
  - email
  - generated invite or login token handled by backend
- Prefer email code / magic link / backend-issued token over a permanent static code as the true auth mechanism.

## Session model

Target rule:

- unlimited registered devices per user
- one active authenticated session at a time

Expected flow:

1. user signs in on new device
2. backend sees an active session on another device
3. app asks whether to replace the active session
4. if confirmed, backend revokes the old session
5. old device is forced out on next sync / token refresh / API call

## Trial model

Trial should be enforced server-side once the backend exists.

Suggested logic:

- trial account starts with `2` free job creations
- materials within those jobs remain unlimited
- app should surface progress before the limit is hit
- backend should own the canonical counter so users cannot bypass the trial by reinstalling or changing devices

## Cross-platform purchase/entitlement model

Desired user experience:

- a business buys on iPhone
- the admin later signs into Android
- the Android app recognizes the entitlement with little or no friction

Clean implementation:

- Apple purchase is verified by backend
- backend attaches entitlement to org/account
- Android app signs into same app account
- Android queries backend
- backend returns active entitlement and seat availability

Important note:

- Apple-to-Android access is fine if backend entitlements exist
- it is not a direct store-to-store transfer
- backend is the bridge

## Pricing and catalog implications

The first concrete plan catalog should assume:

- `material_guardian_individual_monthly`
- `material_guardian_individual_yearly`
- `material_guardian_business_5_monthly`
- `material_guardian_business_5_yearly`

The backend should return enough plan data for the app to show:

- plan name
- billing interval
- seat count included
- display price
- annual savings amount and/or savings copy for yearly plan presentation

Do not make mobile clients infer plan meaning from raw store product identifiers alone.

## Apple and Google store implications

High-level policy direction already noted during planning:

- if selling digital access inside iOS, Apple IAP is expected
- if selling digital access inside Android, Google Play Billing is expected
- previously purchased access can be recognized across platforms through backend entitlements

Do not design the product around raw Apple receipts or raw Play purchases being the long-term user identity layer.

## Admin feature direction

Admin page / admin area will eventually need:

- org summary
- seats purchased
- seats assigned
- seats remaining
- user list
- add user
- delete user
- send login code
- resend login code
- maybe disable user

Suggested user row fields:

- name
- email
- status
- seat assigned yes/no
- last active
- active device summary

## Data sync implications

Current Android app is local-first and local-only.

If the product goal is:

- sign into another device
- recover jobs/materials/photos/scans

then backend sync is required.

Auth alone does not move local data between devices.

This means monetization work and cloud/data architecture are linked.

## Recommended phased implementation order

### Phase 1

- define backend schema
- define auth/session model
- define trial counters
- define org/admin/seat model
- define Apple and Google entitlement mapping

### Phase 2

- implement backend auth
- implement admin invite flow
- implement one-session-per-user enforcement
- implement trial enforcement
- implement entitlement recognition in Android and iOS

### Phase 3

- implement cloud sync for jobs/materials/media
- add conflict handling
- add offline sync queue / retry behavior

## Android-specific notes for future work

- keep current local data model workable during migration
- do not break existing offline-only flow while backend is incomplete
- if sync is introduced, preserve packet export quality and predictable local export behavior
- trial and entitlement checks should degrade gracefully when device is offline

## Pickup guidance for the next agent

If the next session is about monetization, backend, auth, seats, or business subscriptions:

1. read this file first
2. read `ios/android-preferences-change-notes-2026-03-30.md`
3. read the latest iOS backend coordination note in `ios/`
4. do not assume store purchase alone is enough
5. treat backend entitlement design as the first-class problem
