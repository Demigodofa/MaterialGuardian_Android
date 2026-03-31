# Flutter + Backend Direction

Date: 2026-03-31

This note records the current product/engineering direction for Material Guardian so future sessions stop assuming the long-term plan is "native Android first, separate iOS later."

Authoritative location note:

- the Flutter/mobile source of truth now lives in `material-guardian-mobile/docs/material_guardian_flutter_source_of_truth.md`
- backend pricing/entitlement source of truth now lives in `app-platforms-backend/docs/material_guardian_monetization_source_of_truth.md`

## Decision

Material Guardian should move toward:

- a shared Flutter client for Android and iPhone
- a backend as the source of truth for:
  - accounts
  - organizations
  - admins
  - seats
  - active subscriptions/entitlements
  - trial limits
  - active session control
  - eventual sync/recovery

## Active repo split

The working repo split is now:

- `MaterialGuardian_Android`
  current shipping/reference implementation
- `material-guardian-mobile`
  future shared Flutter client
- `app-platforms-backend`
  shared backend for auth, orgs, seats, subscriptions, entitlements, trials, and session control

## What this does and does not mean

This does mean:

- reduce future duplicated client work across Android and iOS
- treat the current Android-native app as the working product and behavior reference
- plan new product work so it can migrate into a shared Flutter client

This does not mean:

- rewrite the whole product immediately before product behavior is clear
- discard the current Android repo as useless
- assume store billing rules disappear

Apple and Google are still the payment rails where required.
The backend becomes the entitlement bridge after purchase verification.

## Why this direction was chosen

- Maintaining two separate full mobile clients is likely to become expensive and slow.
- The harder business problem is cross-platform users, admins, seats, subscriptions, and entitlement recognition.
- A backend is required anyway for trials, orgs, seat assignment, and cross-device account logic.
- Once a backend exists, a shared Flutter client is a cleaner long-term client strategy than maintaining separate native Android and iOS feature stacks for this product.

## Current source of truth while migration has not happened

Until a Flutter client exists, the current Kotlin Android app remains the reference for:

- receiving-report behavior
- export/share behavior
- draft behavior
- customization/signature behavior
- local/offline workflow details

Do not let future planning hand-wave away the current Android behavior.
Port behavior deliberately.

## Recommended implementation order

### Phase 1: backend-first product foundation

Define and build:

- app account model
- org/admin/seat model
- invite/login flow
- subscription/entitlement records
- purchase verification hooks for Apple and Google
- server-side trial tracking
- one-active-session-per-user rules

### Phase 2: Flutter app foundation

Build a new Flutter client shell that proves:

- sign-in
- job list
- job detail
- receiving form shell
- local draft persistence
- export/share entry points

Use the Android app as the behavior and layout reference, not as a code-sharing source.

### Phase 3: migration of core workflows

Move over:

- material create/edit
- photos
- scans
- PDF export
- ZIP/PDF sharing
- customization defaults
- signature handling

### Phase 4: sync and business controls

Add:

- backend-aware entitlement gating
- admin seat UX
- trial enforcement
- optional sync/recovery
- session replacement flow

## Architectural caution

The shared client does not eliminate platform-specific work.
Expect some native/platform-channel/plugin work for:

- camera
- document scanning
- file sharing
- export folder access
- purchase SDKs
- store-specific restore/account flows

The real simplifier is shared client UI/business logic plus backend-owned entitlements, not "100% identical mobile code."

## Immediate next-step guidance

For the next architecture/planning session:

1. keep the current Android app stable
2. design the backend schema and auth/session model first
3. choose the backend stack intentionally
4. then scaffold Flutter around the agreed backend model

Do not start by blindly porting screens into Flutter before the backend contract exists.
