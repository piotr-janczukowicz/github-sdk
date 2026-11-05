# Code Quality — github-sdk Wave 1 Core & internals — 704fa9e

- HEAD `704fa9e41dbbdb9e5c1b18fda82af3b1a2a65094` on `chore/align-repository` (Europe/Warsaw). Live tree read-only: only untracked `review/kick-sdk-w1-core-704fa9e.md`; no edits. File reads from snapshot `%TEMP%\github-sdk-review-704fa9e-w1`. Builds/PMD/CPD on throwaway `%TEMP%\github-sdk-q-w1-704fa9e` (deleted after).
- Scope (full-tree baseline): prod `sdk` root + `sdk.internal` (16 types); Quality adds 38 root `*Test`, 5 `internal/*Test`, 23 `fixtures/*Fixtures`. Out of scope: W2–W7 packages, smoke/, pom/.github/docs.
- Global rules re-read: `java.mdc`, `java-main.mdc`, `java-performance.mdc` (prod smell only; Perf idle), `java-test.mdc`. No repo `.cursor/rules/`.

## Verdict: PASS with Important (Critical 0 / Important 3 / Minor 3)

## Findings

### Bugs / correctness smells → SCR
None owned as Quality Musts. No invented Critical correctness claims.

| Note | File:line | Handover |
|------|-----------|----------|
| H1 | `Search.java:116-127` (`repositoryOf`) | URL path parsing for search hits is brittle if GitHub ever changes `repository_url` shape — behaviour/API contract → **SCR**, not a java.mdc violation. |
| H2 | `Repository.java:2394-2396` (`read`) | `instanceof GitHubContentFile` is also a Quality sealed-switch issue (Q1). If empty Optional for directory/symlink/submodule is wrong for some callers, that behaviour → **SCR**. |

### Rule violations — production

| # | Sev | File:line | Rule / principle | Why it matters | Concrete change |
|---|-----|-----------|------------------|----------------|-----------------|
| Q1 | Important | `Repository.java:2389-2396` | `java.mdc`: prefer `sealed` + exhaustive pattern `switch` over `instanceof` + cast | `GitHubContentResult` is `sealed` (`permits` Directory, File, Symlink, Submodule). `instanceof` hides the other cases and will not fail compile when a new permit is added. | Replace the `flatMap` body with `switch (result) { case GitHubContentFile file -> Optional.of(new RepositoryFile(this, file, ref)); case GitHubContentDirectory _, GitHubContentSymlink _, GitHubContentSubmodule _ -> Optional.empty(); }` (or named unused bindings). Keep empty for non-files unless SCR says otherwise. |
| Q2 | Important | `Repository.java` (2418 LOC, ~150 public members) | `java-main` S / cohesion; kick: cite LOC clusters, not slogans | Explicit REST facade (allowed by java-main’s facade exception), but one type still mixes many change reasons. Clusters (public methods): git/content ~24; repo-mgmt ~21; actions ~18; deployments ~9; issues/PRs ~9; labels/milestones ~8; collaborators ~7; activity ~7; security-alerts ~7; plus smaller groups (releases, webhooks, checks, …). Private helpers at `:2346–2416`. Organization is the same pattern at 631 LOC / ~40 publics. | **Do not rewrite for style alone.** Coordinate with SCR: either keep the public `Repository` facade and only tidy private helpers, or extract domain collaborators (`RepositoryActions`, `RepositoryGit`, …) that `Repository` delegates to without changing the public surface. See **DECISION D-1** in the prompt. |
| Q3 | Minor | `Repository.java:2365-2377` | One job / duplication vs premature abstraction | `loadPullRequest`, `loadSecretScanningAlert`, `loadCodeScanningAlert` are the same two lines (null-check number + `Lookups.require` + `IS_GONE`). Three copies are enough to justify a tiny private helper; a fourth copy would make it overdue. | Optional: `requireListed(Integer number, Lookups.IoLookup<T> lookup, String kind)` used by the three. Skip if D-1 extracts those domains away. |

### Rule violations — tests

| # | Sev | File:line | Rule / principle | Why it matters | Concrete change |
|---|-----|-----------|------------------|----------------|-----------------|
| Q4 | Important | Many root + `internal` tests (see list) | `java-test.mdc`: ≥2 `assertThat` → `assertSoftly`; **One Act** | ~14 tests use `assertThat(x).isPresent(); assertThat(x.orElseThrow()…)` so a missing value masks the value assert. Several `ArgumentsTest` / `ClientValuesTest` / `InternalsTest` methods pack **two Acts** (two `catchThrowable` or two SUT calls) plus dual asserts. | Prefer one AssertJ chain where possible (`assertThat(found).get().extracting(…).isSameAs(…)`), else `assertSoftly`. **Split** multi-Act tests (one production call per `@Test`, or `@ParameterizedTest` when assertion shape matches). Full target list below. |
| Q5 | Minor | `ConfiguredClientTest.java:19-23` | `java-test.mdc`: No Javadoc | Class Javadoc restates README intent; tests must not carry Javadoc. | Delete the class Javadoc block; keep the `@UnitTest` and method name. |
| Q6 | Minor | ~22 `satisfies` / `hasValueSatisfying` groups (e.g. `RepositoryActionsTest`, `OrganizationManagementTest`, `RepositoryCollaboratorsTest`) | `java-test.mdc` soft-assert consistency | Nested `assertThat` inside `satisfies`/`hasValueSatisfying` is common AssertJ style; first nested failure can still hide the second depending on AssertJ path. | Prefer `assertSoftly` (or a single chained `assertThat(list).singleElement().returns(…).returns(…)`) when touching those tests for Q4. Not a mass rewrite by itself. |

**Q4 target methods (present + second assert, or dual Act / dual assert without `assertSoftly`):**

- Present+value: `EventsAndAppsEntryPointsTest.app_shouldReturnThePublicApp_whenTheSlugExists`; `GitHubSdkGistsTest.gist_shouldReturnTheGist_whenGitHubHasIt`; `GitHubSdkNotificationsTest.notification_shouldReturnTheNotification_whenGitHubHasIt`; `OwnerProjectsTest.project_shouldLoadTheProjectOfTheOrganization_whenItExists` / `…OfTheUser…`; `OwnerRegistryPackagesTest.registryPackage_shouldLoadThePackageOfTheOrganization_whenItExists` / `…User…` / `…AuthenticatedUser…`; `RepositoryActionsTest.workflow_shouldReturnTheWorkflow_whenGitHubHasIt` / `workflowRun_shouldReturnTheRun_whenGitHubHasIt`; `RepositoryDeploymentsTest.pages_shouldReturnTheSite_whenTheRepositoryHasOne`; `RepositoryExtrasTest.checkSuite_shouldReturnTheSuite_whenGitHubHasIt`; `RepositoryGitTest.ref_shouldReturnTheRef_whenGitHubHasIt`; `RepositoryRulesetsTest.ruleset_shouldReturnTheRuleset_whenItExists`.
- Dual Act / dual assert: `ConfiguredClientTest.defaults_shouldKeepTheClientsOwnPaging_whenNothingIsConfigured`; `ArgumentsTest.requireNotBlankElements_shouldReturnTheValues_whenTheListMayBeEmpty`; `requireSafePath_shouldReturnThePath_whenItIsAnOrdinaryPathOrRef`; `requireSafePath_shouldThrow…DotSegment`; `requireSafePath_shouldThrow…ControlCharacter`; `ClientValuesTest.statusStateToClient_…` / `organizationRoleToClient_…` / `teamRoleToClient_…` (name-map + size); `InternalsTest.register_shouldKeepTheFirstAccess_whenAnotherOneIsRegisteredLater`.

### PMD / CPD

- Throwaway copy: `mvn -o compile` then `mvn -o pmd:cpd`, `pmd:cpd-check`, `pmd:check`, and `-Ppmd-test pmd:check`.
- Repo ruleset `pmd/pmd-ruleset.xml` → `pmd-model-ruleset.xml` **excluding** CouplingBetweenObjects, CyclomaticComplexity, LoosePackageCoupling, NPathComplexity, ShortClassName (by design for this facade-heavy module).
- Parent sets `pmd.skip` for the default lifecycle; direct `pmd:*` goals still ran. **PMD: 0 violations** (`PMD found no problems in your source code`).
- **CPD: 0 duplications** at plugin defaults (token report only; no `<duplication>` entries). Main sources scanned; no hot CPD pairs in W1. Near-dup of private `load*` helpers noted as Q3 (below CPD threshold / too small to abstract aggressively).
- Transient PMD parser noise around JSpecify `@Nullable` type annotations appeared in the Maven log; report XML stayed empty of violations — treat as tooling noise, not findings.
- JaCoCo: unit gate unchanged; `jacoco.skip` only on the `smoke` profile — do not flag.

### Readability / simplification

- Public entry methods on `Repository` / `Organization` / `GitHubSdk` are already short named pipelines (validate → client call → wrap). That matches java-main; Q2 is about **type** cohesion, not long methods.
- `sdk.github().….…` chains are anti-corruption over the REST client (Demeter exception in java-main). Not flagged.
- `RateLimit` digs `response.resources().core()` — thin DTO adapter; leave unless SCR wants a richer domain.
- Perf (idle): `Arguments.requireSafePath` uses `String.split` on API-boundary validation (cold) — no Quality/Perf Must.

## Already good

- `@NullMarked` on `sdk` and `sdk.internal` package-info; `@Nullable` only on holes.
- Exhaustive enum `switch` in `ClientValues` / `RepositoryVisibility.toClient()`.
- Public Javadoc complete on W1 production types (inputs, `@throws`, nullness); no missing public-member docs found.
- Validation via shared `ValidationUtils` / `Arguments.requireSafePath` at boundaries; exception types/messages preserved in wrappers (`Lookups`, `threadId`).
- Tests: naming overwhelmingly `{method}_should{Outcome}_when{Condition}`; `catchThrowable` (no `assertThatThrownBy`); AAA blank lines present in sampled Acts; utility-ctor reflection tests match java-test; fixtures are `public final` with private ctors and shared factories; no fixture Javadoc.
- Internals access via `Internals` + module non-export documented honestly (classpath convention, not a security boundary).
- `UpdateState` keeps empty-update refusal out of every setter.

## Out of ownership

- Public API split of `Repository` / `Organization` surface → SCR (+ D-1).
- Search hit URL / content non-file behaviour → SCR (H1/H2).
- Hot-path allocation on list/map wrappers → Perf (idle).
- W2–W7 packages, smoke ITs, pom/.github/docs.
