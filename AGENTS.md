# AGENTS.md — working agreement for Hermex

Hermex is a native Android app (Gradle/Kotlin, Google Play + GitHub APK) for a
self-hosted `hermes-webui` server. `PROJECT_SPEC.md` is the product/API source of
truth — if a request conflicts with it, **stop and ask**. Keep it tool-agnostic.

## Session start & wrap-up
- Read `CURRENT.md` first if it exists — gitignored local state; never committed.
- Read only `PROJECT_SPEC.md` sections listed in CURRENT.md's **Spec Read** field;
  never the full ~850-line spec unless told to.
- Implement only the GitHub Issue the human selects, one labeled `ready-for-agent`,
  or one named in CURRENT.md — not every open issue.
- On wrap-up: verify build + tests pass, overwrite CURRENT.md, then commit.
  No append-only log — history is `git log` and merged PRs.

## Workflow
- One issue → `issue/<n>-slug` branch → one PR (no issue → `chore/` or `fix/`).
- `master` is the protected release-candidate branch. Never do feature work on it.
- Pushing, opening/updating a PR, or merging needs **explicit human approval**.
- **Auto version bump**: merges to master trigger `version-bump.yml` (conventional
  commits: `feat:` → minor, `fix:`/`perf:` → patch, `BREAKING` → major).
  `[skip ci]` and `chore:` commits are skipped.

## Commands
```
cd android && ./gradlew assembleDebug      # build
cd android && ./gradlew test               # unit tests (JVM, no emulator)
cd android && ./gradlew lint               # lint
cd android && ./gradlew assembleDebug test # combined
```
- **Pre-commit hook**: stages under `android/` trigger `assembleDebug`.
  Bypass with `git commit --no-verify` if broken, but fix the build before pushing.
- Build before review or committing; manual build-test for UI changes.
- The maintainer uses **VS Code**, not Android Studio. Prefer terminal validation.

## Testing
- Unit tests use **Robolectric** + **MockK**, run on JVM (no emulator needed).
  `android.testOptions.unitTests.isReturnDefaultValues = true` is set.
- Test sources mirror main at `android/app/src/test/java/com/hermex/app/…`.
- `Turbine` for Flow assertions, `MockWebServer` for HTTP fixtures.
- `androidTest/` (instrumented) exists but CI only runs unit tests.

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
Tested against `hermes-webui` v0.51.85 (SHA in `UPSTREAM_TESTED_SHA`).

## Source structure
```
android/app/src/main/java/com/hermex/app/
├── data/          api(Retrofit + SSE), auth, model(@Serializable DTOs), offline, repository
├── ui/            auth, chat, navigation, sessionlist, theme, workspace
├── util/
├── HermexApp.kt   Application class
└── MainActivity.kt  Single-activity entrypoint
```

## Working with the human
- Surface tradeoffs before non-obvious choices; when in doubt, ask.
- Ask before touching anything under "Open questions" in the spec.
- After each slice report: files changed, build/test result, next step,
  and manual test plan when UI changed.

## Staleness
If this file contradicts the codebase or surprises you, tell the developer
and **propose** an edit — don't silently update it.
