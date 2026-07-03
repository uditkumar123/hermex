# Hermex — Android Quality & Parity

## What This Is

Hermex is a native mobile client for controlling a self-hosted Hermes AI agent server. The Android app (this milestone's focus) mirrors the iOS app's core functionality: connect to a self-hosted hermes-webui server, manage chat sessions, and stream real-time agent responses. The iOS app is production-grade with 79 test files; the Android app shipped MVP (chat + sessions) but lacks tests, has security regressions, and is missing 22+ features present on iOS.

## Core Value

The Android app must be a trustworthy, secure, and functional control surface for Hermes — on par with the iOS experience in reliability, security, and core feature set.

## Requirements

### Validated

- ✓ Real-time SSE streaming chat — existing (Milestone 1 Phase 4)
- ✓ Session list with swipe actions, search, projects — existing (Milestone 1 Phase 3)
- ✓ Auth flow with multi-server support — existing (Milestone 1 Phase 2)
- ✓ API client, Retrofit, OkHttp SSE — existing (Milestone 1 Phase 1)
- ✓ Offline session cache via DataStore — existing (Milestone 1 Phase 5)

### Active

- [ ] Zero Android tests — no unit tests or instrumented tests exist
- [ ] CI/CD pipeline is broken (macOS-26 runner, non-blocking lint, no test execution)
- [ ] Android security regressions (cleartext traffic, alpha crypto lib, no ProGuard rules)
- [ ] Architecture issues (AuthManager per-ViewModel, leaked HTTP clients, no SavedStateHandle)
- [ ] Feature parity gap — 22+ iOS features missing (model picker, file browser, markdown, slash commands, skills, memory)
- [ ] Outdated dependencies (18+ months behind on BOM, AGP, Kotlin, lifecycle)

### Out of Scope

- iOS code quality improvements (ChatViewModel split, SwiftData fixes) — defer to iOS-focused milestone
- New iOS features — iOS is mature, focus on Android parity
- Server-side changes — hermes-webui is upstream, not in scope
- Full feature parity with every iOS feature — focus on the 5 highest-impact features first

## Context

The Android app was built as a companion to the mature iOS app. It covers the MVP: project scaffold, data models, API layer, SSE client, auth manager, repositories, session list with swipe actions, and streaming chat. All 5 MVP phases compiled and verified successfully. However, it was built for speed over quality — zero tests, several security shortcuts, and fundamental architecture compromises that need to be addressed before adding features.

The iOS app serves as the reference implementation for expected behavior, API contracts, and UX patterns. The upstream hermes-webui server API is pinned at tag v0.51.85.

## Constraints

- **Platform**: Android only (this milestone; iOS out of scope)
- **Language**: Kotlin 2.1.0+ (target latest stable)
- **Min SDK**: API 26 (Android 8.0)
- **Target SDK**: 35+
- **Compile SDK**: 35+
- **Build**: Gradle 8.7.3+
- **Testing**: JUnit 4 + Espresso + Compose UI Test + MockK
- **DI**: Hilt or Koin (manual singletons are not sustainable)
- **Security**: No cleartext traffic in release, no alpha crypto libs, proper ProGuard/R8 rules
- **Parity**: Match iOS behavior for all implemented features; use iOS as reference UX

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Android-only milestone | Android has biggest quality/security gaps; iOS is mature | ✓ Adopted |
| Standard granularity (6 phases) | Balanced between speed and manageability | ✓ Adopted |
| All 5 parity features in Phase 5 | User wants comprehensive parity kickoff | ✓ Adopted |
| No manual DI (use Hilt) | AuthManager-per-ViewModel is a known bug factory | — Pending |
| Fix CI before adding tests | Tests need CI to run; fix pipeline first | — Pending |
| Security before features | Cleartext traffic and alpha crypto are shipping risks | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition:**
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone:**
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-07-02 after Milestone 2 initialization*
