# AGENTS.md — working agreement for Hermex

Hermex is a native Android app (Gradle/Kotlin, Google Play + GitHub APK) for a
self-hosted `hermes-webui` server. `PROJECT_SPEC.md` is the product/API source of
truth — if a request conflicts with it, **stop and ask**. Keep it tool-agnostic.

**This file is gitignored** — edits are local only. `CLAUDE.md` is a 1-line
pointer that just reads `AGENTS.md`.

## Session start
- Read `CURRENT.md` if it exists — gitignored local state; never committed.
- Implement only the GitHub Issue the human selects, one labeled `ready-for-agent`,
  or one named in CURRENT.md — not every open issue.
- On wrap-up: verify build + tests pass, overwrite CURRENT.md (don't commit it).

## Workflow
- One issue → `issue/<n>-slug` branch → one PR (no issue → `chore/` or `fix/`).
- `master` is the protected release-candidate branch. Never do feature work on it.
- Pushing, opening/updating a PR, or merging needs **explicit human approval**.
- **Auto version bump**: merges to master trigger `version-bump.yml` (conventional
  commits: `feat:` → minor, `fix:`/`perf:` → patch, `BREAKING` → major).
  `[skip ci]` and `chore:` commits are skipped.

## Commands (PowerShell on Windows)

```powershell
# Build, test, lint — all from android\ directory
.\gradlew assembleDebug
.\gradlew test
.\gradlew lint
.\gradlew assembleDebug; if ($?) { .\gradlew test }
```

- A bash pre-commit hook lives at `.git-hooks/pre-commit` (source) and
  `.git/hooks/pre-commit` (active copy). It checks staged Android files and runs
  `assembleDebug`. Bypass with `git commit --no-verify` if broken, but fix before
  pushing. The hook only triggers in Git Bash/WSL — not from PowerShell.
- Build before review or committing; manual build-test for UI changes.
- The maintainer uses **VS Code**, not Android Studio. Prefer terminal validation.

## Testing
- Unit tests use **Robolectric** + **MockK**, run on JVM (no emulator needed).
  `unitTests.isReturnDefaultValues = true` is set in `build.gradle.kts`.
- **Turbine** for Flow assertions, **MockWebServer** for HTTP fixtures.
- Test packages: `data/api/`, `data/auth/`, `ui/chat/`, `ui/navigation/`,
  `ui/sessionlist/`, `ui/workspace/`.
- `androidTest/` (instrumented) exists but CI runs unit tests only.
- **Known fragility**: shared `AuthManager` singleton leaks state across test
  classes. Each `@Before` should reset via `authManager.handleSessionExpired()`
  + `clearError()` + `RetrofitProvider.invalidate()`.

## Source structure

```
android/app/src/main/java/com/hermex/app/
├── data/
│   ├── api/        Retrofit interface (HermesApi), RetrofitProvider, SSEClient
│   ├── auth/       AuthManager (singleton), ServerRegistry, CustomHeaders
│   ├── model/      @Serializable DTOs (Session, Chat, SSE, Auth, Common)
│   ├── offline/    DataStore (sessions) + Room (messages, DAO, DB)
│   └── repository/ SessionRepository, ChatRepository, OfflineMessageRepository
├── ui/
│   ├── auth/       OnboardingScreen, ConnectScreen, AuthViewModel, SettingsScreen
│   ├── chat/       ChatScreen, ChatViewModel, MessageBubbleView, ToolCallCardView
│   ├── navigation/ NavGraph, StartupRouteResolver
│   ├── sessionlist/SessionListScreen, SessionListViewModel
│   ├── splash/     SplashActivity
│   ├── theme/      Color, Theme (light/dark Material3), Type
│   └── workspace/  FileBrowserScreen, SkillsViewModel, MemoryViewModel
├── util/           Constants, Extensions
├── HermexApp.kt    Application class (Timber)
└── MainActivity.kt Single-activity entrypoint
```

## Hard rules
1. **Never invent API endpoints or JSON shapes.** Verify against running server
   (`curl`), then https://get-hermes.ai/api-docs/, then `.codex-tmp/hermes-webui/api/routes.py`
   (clone if missing: `git clone https://github.com/nesquena/hermes-webui .codex-tmp/hermes-webui`).
   The upstream copy is read-only.
2. **No new third-party dependencies** without approval (spec has locked list).
3. **Tolerant decoding only** — every `@Serializable` model uses optionals.
   Never crash on unknown upstream fields.
4. **No destructive commands.** Suggest them; let the human run them.
5. **Don't commit broken builds.** Fix failures before writing more code.

## App identity
Namespace `com.hermex.app` · version in `android/app/build.gradle.kts`.
Upstream pin: `UPSTREAM_TESTED_SHA` (`f1d399b4` = `hermes-webui` v0.51.85).
Gradle 8.9 · JDK 17 · SDK 35 · minSdk 26.

## Working with the human
- Surface tradeoffs before non-obvious choices; when in doubt, ask.
- Ask before touching anything under "Open questions" in the spec.
- After each slice report: files changed, build/test result, next step,
  and manual test plan when UI changed.

## Staleness
If this file contradicts the codebase or surprises you, tell the developer
and **propose** an edit — don't silently update it.
