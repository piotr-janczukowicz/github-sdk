# Merged review — github-sdk Wave 1 Core & internals @ 704fa9e

- **Repo:** `C:\Users\Admin\IdeaProjects\github-sdk`
- **HEAD:** `704fa9e41dbbdb9e5c1b18fda82af3b1a2a65094` (`704fa9e`) on `chore/align-repository`; tracked tree clean
- **Snapshot:** `C:\Users\Admin\AppData\Local\Temp\github-sdk-review-704fa9e-w1`
- **Mode:** full-tree baseline W1 (prod root `sdk` + `sdk.internal`; Quality also root/internal tests + `fixtures`)
- **Room:** SCR + Quality (Perf / O2J idle)
- **Sources:** SCR section (0 Crit / 4 Imp / 10 Minor), Quality section (PASS with Important: 0 Crit / 3 Imp / 3 Minor), plan `review\review-plan-github-sdk-001d5de.md`

## Verdict: **With fixes**

After dedupe (SCR-I4 ≡ Q-2 merged; Quality H1/H2 handoffs closed per SCR ACK, not refiled):
**0 Critical / 6 Important (Must 5 + Should-behind-D-1 1) / 13 Minor** — Must: SCR-I1, SCR-I2, SCR-I3, Q-1, Q-4.
PMD 0 violations, CPD 0 duplications (Quality throwaway run).

## Strengths (union)

- Clear entry surface: `GitHubSdk.of`, handle vs loaded object (`repository()` no request, `fetch()` new object, `response()` ISE with actionable message).
- Uniform Optional-on-404; documented IOException / UncheckedIOException stream contract; `Lookups` keeps laziness and IOE causes.
- Consistent `requireNotBlank` / `requireNonNull` / `requireSafePath` validation with `@throws` Javadoc; full public Javadoc on W1.
- `@NullMarked` on `sdk` and `sdk.internal`; `@Nullable` only where needed.
- `Internals`: CAS first-registration-wins, honest classpath-convention docs.
- Exhaustive enum switches in `ClientValues` / `RepositoryVisibility`; `UpdateState` centralises empty-update refusal.
- Tests: `{method}_should{Outcome}_when{Condition}` naming, `catchThrowable`, AAA, clean fixtures.

## Locked handoffs (SCR ACK)

- **Quality H1** (`Search.java:116-127`, `repositoryOf` URL parsing) = **SCR-M7**. Not a new item.
- **Quality H2** (`Repository.read` empty Optional for directory/symlink/submodule) — **contract stays**. SCR-I2 covers `writeFile` only. Not a finding.
- **Q-1** (sealed switch in `read`) stays **Quality-owned**, behaviour unchanged.

## Must (before W1 is 1.0-ready)

| ID | File:line | What | Why | Fix |
|----|-----------|------|-----|-----|
| SCR-I1 | `GitHubSdk.java:158-185`, `:401-403` (skip via `Lookups.find`) vs `Search.java:110-131`, `Repository.java:2365-2382`, `Organization.java:623-624`, `User.java:297-300` (abort via `Lookups.require`) | Item deleted between list page and per-item GET is skipped in some streams, ends the stream with ISE in others | Unpredictable; long `pullRequests()` / `search().issues()` runs fail on normal concurrent deletion | One policy: `find` + `flatMap(Optional::stream)` for listed items; document in `package-info`; keep `require` only for just-created reads |
| SCR-I2 | `Repository.java:2399-2417` (`writeFile`) | GET sha → PUT → GET; PUT response discarded | Concurrent writer race; read-after-write lag throws "gone after the write" though PUT succeeded; extra round-trips | Build `RepositoryFile` from the PUT response; optional expected-sha overload; document non-atomicity |
| SCR-I3 | `User.java:36-47` vs package-private ctors in `Organization.java:62`, `Team.java:32`, `Repository.java:157`, `RateLimit.java:12`, `Search.java:26` | Public ctor takes client type `GitHubUserView` | Freezes client type in public API / semver surface | Factory via `Internals` (as `registerSdkAccess`); ctor package-private |
| Q-1 | `Repository.java:2389-2396` (`read`) | `instanceof GitHubContentFile` on sealed `GitHubContentResult` | New permit would not fail compile (`java.mdc`) | Exhaustive `switch`: File → `Optional.of(new RepositoryFile(...))`; Directory/Symlink/Submodule → `Optional.empty()` (contract unchanged) |
| Q-4 | W1 tests (list in prompt Part 3) | ~14 present+value tests with two top-level asserts; several `ArgumentsTest` / `ClientValuesTest` / `InternalsTest` / `ConfiguredClientTest` methods with two Acts | `java-test.mdc`: `assertSoftly` for ≥2 asserts; One Act | Single AssertJ chain or `assertSoftly`; split multi-Act tests or parameterize |

## Should (behind decision)

| ID | File:line | What | Why | Fix |
|----|-----------|------|-----|-----|
| SCR-I4 / Q-2 (merged) | `Repository.java` 2418 LOC, ~150 public methods, 125 imports; facades already at `:2000-2128`; inline Actions ~170 LOC, git DB ~160, deployments/environments ~160, security alerts ~115 (`Organization` 631 LOC / ~40 publics same pattern) | God-class; inconsistent extraction criterion | Maintainability / API evolution; not a runtime bug | **D-1.** Not a release gate once Must lands |

## Nice (Minor)

| ID | File:line | What / fix |
|----|-----------|------------|
| SCR-M1 | `Arguments.java:87-101` | `requireSafePath` misses percent-encoded / backslash segments; stricter hex check for `sha`. Pending client URL-encoding check (out of W1) |
| SCR-M2 | `Repository.java:1437-1444` | `refs(prefix)` only `requireNotBlank`; use `requireSafePath` like `ref`/`createRef` |
| SCR-M3 | path/ref APIs Javadoc | Docs say "blank" only; also mention control chars and `.`/`..` |
| SCR-M4 | `Repository.java:2340-2344` | `apply` keeps handle owner; take owner from response like `fetch`/`createFork` |
| SCR-M5 | `Repository.java:1499-1503` | `createBranch` read-after-write lag can ISE; build from create response |
| SCR-M6 | `Repository.java:157-163` | ctor lacks `requireNonNull` |
| SCR-M7 (= Quality H1) | `Search.java:59-63`, `:116-127` | IAE for malformed server hit → ISE; fragile `repositoryOf` URL parse |
| SCR-M8 | `RepositoryUpdate` | Document single-use / not thread-safe (or guard reuse after `send()`) |
| SCR-M9 | `RateLimit` | Only `core` bucket exposed |
| SCR-M10 | create/fetch/fork `owner().login()` | Verify client `@NullMarked` guarantee; else guard |
| Q-3 | `Repository.java:2365-2377` | Three identical `load*` helpers → private `requireListed(...)`; skip if D-1 B moves them (coordinate with SCR-I1) |
| Q-5 | `ConfiguredClientTest.java:19-23` | Remove class Javadoc (D-3) |
| Q-6 | ~22 nested `satisfies` / `hasValueSatisfying` groups | `assertSoftly` / chained `returns` only when touching file for Q-4 (D-2) |

## Decisions for Piotr (defaults)

- **D-1 Repository / Organization cohesion (SCR-I4 / Q-2)** — **Default A: keep** the public facade this wave; apply Must; optional Q-3 helper; track split as follow-up issue. **B:** incremental non-breaking extract (`actions()`, `git()`, `deployments()` + environments, `alerts()`, `releases()`, settings: webhooks/deployKeys/autolinks) with delegation/deprecation; identity/fetch/update/issues/PRs/branches/contents stay on `Repository`; SCR reviews API shape.
- **D-2 Q-6 sweep** — **Default A:** only in files touched for Q-4. B: full W1 sweep.
- **D-3 Q-5** — **Default A:** delete class Javadoc. B: move note to README.
- **D-4 SCR-I2 expected-sha overload** — **Default:** fix via PUT response + docs only; no new public overload unless Piotr wants it.
- **D-5 SCR-I1 policy** — **Default:** skip-on-gone (`find`) for all listed streams.

## Declined / out of scope

- `Internals` public accessors on classpath (intentional, documented); raw client model pass-through from `GitHubSdk`; N+1 cost of gists/search streams (documented, use `limit`); `ClientValues` enum duplication (decoupling); wrapper thread-safety (final fields OK).
- `Repository.read` empty Optional for non-files (contract, per SCR ACK).
- Demeter chains over REST client; `RateLimit` thin adapter; `requireSafePath` `split` (cold path); PMD parser noise on JSpecify annotations.
- W2–W7 packages, smoke ITs (W8), pom/.github/docs; Perf/O2J idle.

## Recommended apply order

1. Q-1 (isolated, no behaviour change).
2. SCR-I1 (+ Q-3 helper if D-1 A) — same `load*` area.
3. SCR-I2 (+ SCR-M5 same read-after-write pattern).
4. SCR-I3.
5. Q-4 (+ Q-5, Q-6 per D-2/D-3).
6. Minor batch: SCR-M2, M3, M4, M6, M8 (M7, M9, M10 optional; M1 pending transport check).
7. D-1 B only if chosen — separate run.
