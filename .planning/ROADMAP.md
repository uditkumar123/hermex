# Hermex Android — Milestone 3: Parity Deepening

**Goal:** Close the remaining feature gaps with iOS, deepen test coverage, and fix persistence/recovery.

---

## Phase 6: Android Test Deepening
**Goal:** Add unit tests for ViewModels, Compose UI tests for critical flows.
**Success Criteria**:
1. ChatViewModel unit tests (streaming, message management, cancel, steer, approvals)
2. SessionListViewModel unit tests (search, CRUD, projects, profiles)
3. Compose UI test for onboarding → connect → chat flow
4. ApiClient/RetrofitProvider unit tests (error handling, cookies, headers)
5. All new tests pass in CI

**Requirements:** TST-09, TST-10, TST-11, TST-12

---

## Phase 7: File Browser + Markdown Rendering
**Goal:** Add workspace file browsing and proper markdown rendering.
**Success Criteria**:
1. File list/search UI fetching from `/api/list` and `/api/file`
2. Text file preview with syntax highlighting
3. Image preview for supported formats
4. Code block rendering with language-based syntax highlighting
5. Inline code, bold, italic, links, lists in chat messages

**Requirements:** FEAT-09, FEAT-10, FEAT-11

---

## Phase 8: Slash Command System
**Goal:** Implement slash command autocomplete and execution matching iOS behavior.
**Success Criteria**:
1. `/` trigger in composer opens autocomplete dropdown
2. 18+ commands: help, new, model, workspace, reasoning, title, personality, skills, queue, steer, interrupt, status, btw, background, branch, undo, retry, compress
3. Direct skill slash shortcut support
4. Unsupported commands show friendly local message

**Requirements:** FEAT-12, FEAT-13

---

## Phase 9: Skills + Memory Panels
**Goal:** Add read-only skills browser and memory panel views.
**Success Criteria**:
1. Skills list with search from `/api/skills`
2. Skill detail view from `/api/skills/content`
3. Linked file rendering in skill details
4. Memory notes display from `/api/memory`
5. Memory profile display

**Requirements:** FEAT-14, FEAT-15, FEAT-16

---

## Phase 10: Persistence + SSE Recovery
**Goal:** Fix PersistentCookieJar and add SSE stream reconnection.
**Success Criteria**:
1. PersistentCookieJar saves/loads cookies from SharedPreferences (not in-memory)
2. SSE reconnection with Last-Event-ID on transport errors
3. Cookies shared between Retrofit client and SSE OkHttp client
4. Offline chat message caching via Room database (basic implementation)

**Requirements:** FEAT-17, FEAT-18, FEAT-19

---

## Progress Log

| Date | Phase | Action | Notes |
|------|-------|--------|-------|
| 2026-07-02 | — | Milestone created | 5 phases, 11 requirements, continuing from M2 parity work |
| 2026-07-02 | Phase 6 | Completed | 73 total tests (29 new ViewModel tests, Robolectric) |
| 2026-07-02 | Phase 7 | Completed | File browser (list/search/preview), markdown in messages |
| 2026-07-02 | Phase 8 | Completed | 21 slash commands, autocomplete dropdown in composer |
| 2026-07-02 | Phase 9 | Completed | Skills browser, memory panel with tabs |
| 2026-07-02 | Phase 10 | Completed | PersistentCookieJar saves to SharedPreferences |
| 2026-07-02 | — | Milestone complete | 5/5 phases, all builds + tests passing |

## Previous Milestones
- Milestone 1 (v1.0): Android MVP — 5/5 ✅
- Milestone 2 (v2.0): Quality & Parity — 5/5 ✅
