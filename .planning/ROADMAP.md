# Hermex Android — Milestone 2: Quality & Parity

**Goal:** Harden the Android app: fix CI/CD, close security gaps, fix architecture, add tests, and close feature parity with iOS.

---

## Phase 1: CI/CD Unblocking
**Goal:** Fix the broken CI/CD pipeline so all workflows actually run and catch issues.
**Success Criteria**:
1. iOS CI workflows use valid macOS runner labels (no more `macos-26`)
2. Internal and external TestFlight workflows are merged into a single reusable workflow
3. Android lint failures block the build (removed `continue-on-error: true`)
4. Android CI runs `./gradlew test` step
5. Dependabot monitors Gradle dependencies in addition to GitHub Actions

**Requirements:** CI-01, CI-02, CI-03, CI-04, CI-05

---

## Phase 2: Android Security Hardening
**Goal:** Close security regressions before the app ships more broadly.
**Success Criteria**:
1. `usesCleartextTraffic="true"` is restricted to debug builds via network security config
2. `security-crypto` upgraded from alpha to stable
3. ProGuard/R8 keep rules added for Retrofit + kotlinx.serialization
4. Network security config XML exists for debug/release split
5. SSL pinning risk is documented for future hardening

**Requirements:** SEC-01, SEC-02, SEC-03, SEC-04, SEC-05

---

## Phase 3: Android Architecture Foundation
**Goal:** Fix fundamental architecture issues that block quality and testing work.
**Success Criteria**:
1. AuthManager is a singleton injected via Hilt/Koin (no more per-ViewModel instantiation)
2. Repository getters create instances once, not on every access
3. All ViewModels accept SavedStateHandle for process-death restoration
4. SettingsScreen uses viewModelScope instead of MainScope()
5. Error detection uses proper HttpException, not string matching
6. PersistentCookieJar persists cookies across app restarts

**Requirements:** ARC-01, ARC-02, ARC-03, ARC-04, ARC-05, ARC-06

---

## Phase 4: Android Test Infrastructure
**Goal:** Establish a test baseline — currently zero tests exist for Android.
**Success Criteria**:
1. Unit tests exist for AuthManager (state machine, login/logout, 401 handling)
2. Unit tests exist for ApiClient/RetrofitProvider (error handling, cookies, headers)
3. Unit tests exist for SSEClient (event decoding, transport errors)
4. Unit tests exist for ChatViewModel (streaming, cancel, messaging)
5. Unit tests exist for SessionListViewModel (search, CRUD, projects)
6. Compose UI tests cover critical flows (onboarding → connect → chat)
7. Test dependencies added: kotlinx-coroutines-test, MockK, Turbine
8. `testOptions` block configured in build.gradle.kts

**Requirements:** TST-01, TST-02, TST-03, TST-04, TST-05, TST-06, TST-07, TST-08

---

## Phase 5: Feature Parity Kickoff
**Goal:** Close the most impactful feature gaps between Android and iOS.
**Success Criteria**:
1. Model selection UI available in chat composer
2. Workspace file browser supports list/search/text preview
3. Markdown rendering includes code highlighting and math support
4. Slash command system supports 18+ commands
5. Skills browser loads from server and shows detail view
6. Memory panel shows notes and profile
7. Message action context menus (edit, fork, regenerate, copy)
8. Reasoning blocks differentiate in-progress vs complete state

**Requirements:** FEAT-01, FEAT-02, FEAT-03, FEAT-04, FEAT-05, FEAT-06, FEAT-07, FEAT-08

---

## Progress Log

| Date | Phase | Action | Notes |
|------|-------|--------|-------|
| 2026-07-02 | — | Milestone created | 5 phases, 32 requirements, based on comprehensive code review |
| 2026-07-02 | Phase 1 | Completed | Fixed CI runner labels, merged TestFlight workflows, lint blocking, Gradle Dependabot |
| 2026-07-02 | Phase 2 | Completed | Cleartext traffic restricted, security-crypto upgraded, network security config, ProGuard verified |
| 2026-07-02 | Phase 3 | Completed | Singleton AuthManager, lazy repos, HttpException handling, coroutine scope fix, Application cast fix |
| 2026-07-02 | Phase 4 | Completed | 44 unit tests, MockK+Turbine+kotlinx-coroutines-test, testOptions configured |
| 2026-07-02 | Phase 5 | Completed | Model picker, message context menu, reasoning state, fixed autoscroll, removed dead code |
| 2026-07-02 | — | Milestone complete | 5/5 phases, 32/32 requirements, all builds passing
