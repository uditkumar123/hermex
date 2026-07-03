# Requirements: Hermex Android — Quality & Parity

**Defined:** 2026-07-02
**Core Value:** The Android app must be a trustworthy, secure, and functional control surface for Hermes — on par with the iOS experience in reliability, security, and core feature set.

## v2 Requirements

Requirements for Milestone 2. Each maps to roadmap phases.

### CI/CD (CI)

- [ ] **CI-01**: Fix macOS runner labels in GitHub Actions workflows so iOS CI actually runs
- [ ] **CI-02**: Merge internal and external TestFlight workflows into a single reusable workflow
- [ ] **CI-03**: Make Android lint blocking (remove `continue-on-error: true`)
- [ ] **CI-04**: Add `./gradlew test` step to Android CI workflow
- [ ] **CI-05**: Add Gradle ecosystem to Dependabot config

### Security (SEC)

- [ ] **SEC-01**: Restrict `android:usesCleartextTraffic="true"` to debug builds only via network security config
- [ ] **SEC-02**: Upgrade `security-crypto` from alpha (`1.1.0-alpha06`) to latest stable
- [ ] **SEC-03**: Add ProGuard/R8 keep rules for Retrofit interfaces and kotlinx.serialization data classes
- [ ] **SEC-04**: Add network security config XML for debug/release split
- [ ] **SEC-05**: Add SSL certificate pinning configuration (at minimum, documentation of the risk)

### Architecture (ARC)

- [ ] **ARC-01**: Implement singleton AuthManager with Hilt/Koin DI (currently instantiated per-ViewModel)
- [ ] **ARC-02**: Fix repository getter leaks (currently recreates ApiClient + SSEClient on every access)
- [ ] **ARC-03**: Add SavedStateHandle to all ViewModels for process-death state restoration
- [ ] **ARC-04**: Fix SettingsScreen coroutine scope (replace `MainScope()` with `viewModelScope`)
- [ ] **ARC-05**: Replace error-detection-by-string-matching with proper HttpException handling
- [ ] **ARC-06**: Fix PersistentCookieJar to actually persist (currently in-memory only) or rename it

### Testing (TST)

- [ ] **TST-01**: Add unit tests for AuthManager state machine (login, logout, 401, multi-server)
- [ ] **TST-02**: Add unit tests for ApiClient/RetrofitProvider (error handling, header injection, cookie jar)
- [ ] **TST-03**: Add unit tests for SSEClient (event decoding, error handling, reconnect)
- [ ] **TST-04**: Add unit tests for ChatViewModel (streaming, cancel, message management)
- [ ] **TST-05**: Add unit tests for SessionListViewModel (search, pin, archive, delete, projects)
- [ ] **TST-06**: Add Compose UI tests for critical flows (onboarding → connect → chat)
- [ ] **TST-07**: Add test dependencies: kotlinx-coroutines-test, MockK, Turbine
- [ ] **TST-08**: Configure testOptions in build.gradle.kts (animationsDisabled, returnDefaultValues)

### Feature Parity (FEAT)

- [ ] **FEAT-01**: Implement model selection UI in chat composer (picker with favorites/recents)
- [ ] **FEAT-02**: Implement workspace file browser (list/search files, text preview, image preview)
- [ ] **FEAT-03**: Implement proper markdown rendering (code blocks with highlighting, math, links)
- [ ] **FEAT-04**: Implement slash command system (/help, /new, /model, /workspace, /reasoning, /title, /personality, /skills, /queue, /steer, /interrupt, /status, /btw, /background, /branch, /undo, /retry, /compress)
- [ ] **FEAT-05**: Implement read-only skills browser (list, search, detail view)
- [ ] **FEAT-06**: Implement read-only memory panel (notes, profile view)
- [ ] **FEAT-07**: Add message action context menus (edit, fork, regenerate, copy, listen)
- [ ] **FEAT-08**: Add proper reasoning block rendering (differentiate in-progress vs complete)

## v3 Requirements

Deferred to future release.

### Feature Parity (Cont.)

- **FEAT-09**: Insights/analytics UI
- **FEAT-10**: Voice notes and recording
- **FEAT-11**: Git workspace integration
- **FEAT-12**: Offline chat message caching (Room database)
- **FEAT-13**: Archived sessions view
- **FEAT-14**: App icon customization
- **FEAT-15**: Deep linking and notifications
- **FEAT-16**: Haptic feedback

### Platform Parity

- **PLAT-01**: Android equivalent of iOS Live Activities
- **PLAT-02**: Android equivalent of Share Extension

## Out of Scope

| Feature | Reason |
|---------|--------|
| iOS code quality (ChatViewModel split, SwiftData fixes) | iOS is mature; separate milestone warranted |
| New iOS features | Focus is Android parity |
| KMP shared module | Too large for this milestone; architectural decision deferred |
| SSL certificate pinning implementation | Complex; documented as future hardening |
| Full feature parity (all 22+ iOS features) | Phase 5 covers top 5; rest deferred to v3 |
| Server-side changes | hermes-webui is upstream dependency |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| CI-01 | 1 | Pending |
| CI-02 | 1 | Pending |
| CI-03 | 1 | Pending |
| CI-04 | 1 | Pending |
| CI-05 | 1 | Pending |
| SEC-01 | 2 | Pending |
| SEC-02 | 2 | Pending |
| SEC-03 | 2 | Pending |
| SEC-04 | 2 | Pending |
| SEC-05 | 2 | Pending |
| ARC-01 | 3 | Pending |
| ARC-02 | 3 | Pending |
| ARC-03 | 3 | Pending |
| ARC-04 | 3 | Pending |
| ARC-05 | 3 | Pending |
| ARC-06 | 3 | Pending |
| TST-01 | 4 | Pending |
| TST-02 | 4 | Pending |
| TST-03 | 4 | Pending |
| TST-04 | 4 | Pending |
| TST-05 | 4 | Pending |
| TST-06 | 4 | Pending |
| TST-07 | 4 | Pending |
| TST-08 | 4 | Pending |
| FEAT-01 | 5 | Pending |
| FEAT-02 | 5 | Pending |
| FEAT-03 | 5 | Pending |
| FEAT-04 | 5 | Pending |
| FEAT-05 | 5 | Pending |
| FEAT-06 | 5 | Pending |
| FEAT-07 | 5 | Pending |
| FEAT-08 | 5 | Pending |

**Coverage:**
- v2 requirements: 32 total
- Mapped to phases: 32
- Unmapped: 0

---
*Requirements defined: 2026-07-02*
*Last updated: 2026-07-02 after Milestone 2 initialization*
