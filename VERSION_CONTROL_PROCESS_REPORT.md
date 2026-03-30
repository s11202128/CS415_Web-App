# Version Control Process Report

Date: 2026-03-30
Repository: CS415-A1
Current branch: Native-App
Default branch target (as provided): version1

## 1) Current Repository Snapshot

- Active branch: Native-App
- Current branch HEAD commit:
  - d742c64 - setup for transition from web app to mobile app
- Working tree status: dirty (tracked and untracked changes present)

## 2) Tracked Files Currently Modified

The following tracked files are modified and not yet committed:

- MOBILE_MIGRATION_PLAN.md
- mobile-android/app/build.gradle.kts
- mobile-android/app/src/main/AndroidManifest.xml
- mobile-android/app/src/main/java/com/bof/mobile/MainActivity.kt
- mobile-android/app/src/main/java/com/bof/mobile/data/remote/ApiService.kt
- mobile-android/app/src/main/java/com/bof/mobile/data/remote/NetworkModule.kt
- mobile-android/app/src/main/java/com/bof/mobile/data/repository/AuthRepository.kt
- mobile-android/app/src/main/java/com/bof/mobile/ui/AppRoot.kt
- mobile-android/app/src/main/java/com/bof/mobile/ui/accounts/AccountsScreen.kt
- mobile-android/app/src/main/java/com/bof/mobile/ui/auth/LoginScreen.kt
- mobile-android/app/src/main/java/com/bof/mobile/ui/auth/RegisterScreen.kt
- mobile-android/app/src/main/java/com/bof/mobile/ui/dashboard/DashboardScreen.kt
- mobile-android/app/src/main/java/com/bof/mobile/ui/transfers/TransferScreen.kt
- mobile-android/build.gradle.kts
- mobile-android/gradle.properties
- mobile-android/settings.gradle.kts
- server/package-lock.json
- server/package.json
- server/src/docs/mobile-api.md
- server/src/routes/apiRoutes.js
- server/src/store-mysql.js

## 3) Untracked/Generated Noise Observed

Untracked files include IDE and Gradle-generated artifacts, for example:

- .idea/*
- mobile-android/.gradle/9.0.0/*
- multiple generated Android build intermediates under mobile-android/app/build/*

These files are environment-generated and should not be version controlled.

## 4) Version Control Risk Assessment

- Risk: commit pollution from generated files
  - Impact: noisy diffs, larger PRs, harder code review.
- Risk: mixed concern changes in one batch (Android UI, Android build config, backend routes/docs)
  - Impact: difficult rollback and low traceability.
- Risk: root .gitignore is currently minimal
  - Impact: local tooling outputs are likely to be added accidentally.

## 5) Recommended Control Process (Immediate)

1. Add project-specific ignore rules before next commit:
   - .idea/
   - mobile-android/.gradle/
   - mobile-android/**/build/
   - local.properties
   - *.iml
2. Stage only source and documentation files relevant to a single feature.
3. Split commits by concern:
   - Commit A: Android migration/build config
   - Commit B: Android UI/auth/account/transfer screens
   - Commit C: server API route/store/docs updates
4. Use clear commit messages (Conventional style recommended):
   - feat(android): add auth and dashboard screens for migration
   - chore(android): update gradle config for mobile transition
   - feat(server): extend api routes for mobile app integration
5. Open PR from Native-App to version1 with a review checklist:
   - build passes
   - no generated files included
   - API docs updated with route changes
   - Android and server contract tested

## 6) Suggested Ongoing Workflow

- Branch model:
  - feature/mobile-auth
  - feature/mobile-dashboard
  - chore/android-build-config
  - feature/server-mobile-routes
- Commit frequency: small, test-backed commits every logical change.
- PR size target: 200 to 500 lines changed where possible.
- Tagging: create milestone tags for migration phases (for example v2-mobile-phase1, v2-mobile-phase2).

## 7) Executive Summary

The branch Native-App is actively progressing the web-to-mobile migration with meaningful Android and backend updates. The current version-control weakness is repository hygiene (generated files) and mixed-scope changes. Tightening ignore rules and splitting commits by concern will significantly improve traceability, review quality, and rollback safety.
