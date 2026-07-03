# Hermex Android — Project State

**Milestone:** v2.0 — Quality & Parity
**Current Phase:** Phase 1 — CI/CD Unblocking
**Overall Progress:** 0/5 phases complete

---

## Phase Status

| Phase | Status | Progress |
|-------|--------|----------|
| 1. CI/CD Unblocking | ⏳ Ready | Not started |
| 2. Security Hardening | ⬜ Pending | Not started |
| 3. Architecture Foundation | ⬜ Pending | Not started |
| 4. Test Infrastructure | ⬜ Pending | Not started |
| 5. Feature Parity Kickoff | ⬜ Pending | Not started |

---

## Build Status
- **Last known good:** 2026-07-02 (Milestone 1 Phase 5)
- **Android CI:** Builds debug APK, lint is non-blocking, no tests executed
- **iOS CI:** BLOCKED — `macos-26` runner label does not exist

## Key Issues (from review)

### CRITICAL
- macOS CI workflows use non-existent `macos-26` runner
- Zero Android tests (no unit or instrumented tests)
- `android:usesCleartextTraffic="true"` in release builds
- No ProGuard/R8 keep rules for critical libraries

### HIGH
- AuthManager instantiated per-ViewModel (no shared auth state)
- Repository getters recreate HTTP clients on every access
- Error detection uses fragile string matching (AuthManager.kt:126-133)
- SettingsScreen uses MainScope() instead of viewModelScope
- Outdated Compose BOM, AGP, Kotlin, lifecycle dependencies

### MEDIUM
- PersistentCookieJar is in-memory only (not actually persistent)
- Alpha security-crypto dependency in production
- Auto-scroll snaps user back during streaming
- No SavedStateHandle in any ViewModel
- Dependabot only monitors GitHub Actions

## Blockers
None currently.

## Decisions
- Package name: `com.hermex.app`
- Min SDK: 26 (Android 8.0)
- Target SDK: 35
- Compile SDK: 35
- DI framework: Hilt (to be added in Phase 3)
- Test frameworks: JUnit 4 + Espresso + Compose UI Test + MockK + Turbine
- Security: No cleartext in release, no alpha libs, ProGuard rules required
- Parity reference: iOS app behavior and UX patterns

## Previous Milestone
Milestone 1 (v1.0) — Android MVP: Chat + Sessions — 5/5 phases complete
Archive: `.planning/v1.0-ROADMAP.md`, `.planning/v1.0-STATE.md`

---
*Last updated: 2026-07-02 — Milestone 2 initialization*
