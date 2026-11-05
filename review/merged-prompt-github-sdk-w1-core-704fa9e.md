# Coding-agent prompt — github-sdk W1 Core & internals fixes @ 704fa9e

Source: `review\merged-report-github-sdk-w1-core-704fa9e.md` (IDs SCR-I*, SCR-M*, Q-*). Repo `C:\Users\Admin\IdeaProjects\github-sdk`, branch `chore/align-repository`. Package `io.github.piotr_janczukowicz.github.sdk`.

## Constraints (hard)

- Review is done (read-only); you now apply fixes. **No commit / push** unless Piotr says so.
- Touch only W1: `src/main/java/.../sdk/*.java` (root), `.../sdk/internal/*`, and matching tests/fixtures. Do **not** touch other packages (content, git, issues, actions, …), smoke/, pom.xml, .github/, docs/, pmd/.
- Preserve public exception types and messages unless the item fixes that exact bug (SCR-I1, SCR-I2, SCR-M7 only). Keep `IS_GONE` messages.
- No public API change except SCR-I3 (ctor → package-private + `Internals` factory). D-1 default: no new public types.
- `Repository.read` keeps empty Optional for directory/symlink/submodule (contract).
- No `-Dpmd.skip`; do not change JaCoCo thresholds or smoke `jacoco.skip`. No style-only rewrites.
- Decisions (apply defaults unless Piotr overrides): D-1 A keep facade, D-2 A, D-3 A delete Javadoc, D-4 no new overload, D-5 skip-on-gone.

## Baseline

```
cd C:\Users\Admin\IdeaProjects\github-sdk
git rev-parse HEAD          # 704fa9e41dbbdb9e5c1b18fda82af3b1a2a65094 (or successor)
git status --short
mvn -o spotless:check
mvn -o clean test           # record unit test count, 0 failures
mvn -o pmd:check pmd:cpd-check   # expect PMD 0 / CPD 0
```

## Part 1 — Must, production (Q-1, SCR-I1, Q-3, SCR-I2, SCR-M5, SCR-I3)

1. **Q-1** `Repository.java:2389-2396`: replace `instanceof GitHubContentFile` with exhaustive `switch` on sealed `GitHubContentResult`; non-file permits → `Optional.empty()`.
2. **SCR-I1**: listed-item streams use `Lookups.find` + `flatMap(Optional::stream)` (skip on gone) in `Search.java:110-131`, `Repository.java:2365-2382`, `Organization.java:623-624`, `User.java:297-300`, matching `GitHubSdk.java:158-185/401-403`. Keep `Lookups.require` only for just-created reads. Document the policy in `sdk/package-info.java`. **Q-3:** while there, fold the three `load*` helpers into one private helper.
3. **SCR-I2** `Repository.java:2399-2417`: build `RepositoryFile` from the PUT response (drop the post-PUT GET); Javadoc non-atomic sha GET→PUT. **SCR-M5** `:1499-1503`: same for `createBranch` if the create response carries what's needed.
4. **SCR-I3** `User.java:36-47`: ctor package-private; add `Internals` factory (as `registerSdkAccess`); update callers in other packages via that factory only (signature change only, no logic).

Acceptance:
- [ ] No `instanceof` in `read`; non-file → empty.
- [ ] Every listed-item stream skips gone items; tests added for skip case (Search, Repository PR/alerts, Organization, User).
- [ ] `writeFile` does one GET + one PUT; returns file from PUT; test for that.
- [ ] `User` has no public ctor; compile clean.
- [ ] Message changes only where the I1 abort path was removed.

## Part 2 — Must, tests (Q-4, Q-5, Q-6 per D-2 A)

- Present+value → one chain (`assertThat(found).get()…`) or `assertSoftly`: `EventsAndAppsEntryPointsTest.app_…`; `GitHubSdkGistsTest.gist_…`; `GitHubSdkNotificationsTest.notification_…`; `OwnerProjectsTest.project_…` (org + user); `OwnerRegistryPackagesTest.registryPackage_…` (org, user, authenticated user); `RepositoryActionsTest.workflow_…` / `workflowRun_…`; `RepositoryDeploymentsTest.pages_…`; `RepositoryExtrasTest.checkSuite_…`; `RepositoryGitTest.ref_…`; `RepositoryRulesetsTest.ruleset_…`.
- Split / parameterize two-Act tests: `ArgumentsTest.requireNotBlankElements_…`, `requireSafePath_shouldReturnThePath_…`, `…DotSegment`, `…ControlCharacter`; `ClientValuesTest.statusStateToClient_…` / `organizationRoleToClient_…` / `teamRoleToClient_…` (`assertSoftly` or split size); `InternalsTest.register_…` (`assertSoftly`); `ConfiguredClientTest.defaults_…`.
- Q-5: delete class Javadoc `ConfiguredClientTest.java:19-23`.

Acceptance:
- [ ] No listed test has ≥2 top-level `assertThat` without `assertSoftly`; one Act per `@Test` (or per parameterized invocation).
- [ ] Naming `{method}_should{Outcome}_when{Condition}`; test count same or higher.

## Part 3 — Nice (Minor batch)

SCR-M2 (`refs(prefix)` → `requireSafePath`), SCR-M3 (Javadoc: controls, `.`/`..`), SCR-M4 (`apply` owner from response), SCR-M6 (`requireNonNull` in `Repository` ctor), SCR-M8 (document `RepositoryUpdate` single-use/not thread-safe). Optional: SCR-M7 (ISE for malformed search hit, sturdier `repositoryOf`), SCR-M10 (verify nullness). Skip SCR-M1 (pending transport encoding check) and SCR-M9 unless asked.

Acceptance: [ ] each touched item has a test or Javadoc diff; no other behaviour change.

## Part 4 — only if Piotr picks D-1 B

Incremental extract of `Repository` areas (`actions()`, `git()`, `deployments()`+environments, `alerts()`, `releases()`, settings) with delegation; old methods stay (optionally `@Deprecated`). Separate run; SCR reviews API shape.

## Verify (after each part)

```
mvn -o spotless:check
mvn -o clean test
mvn -o pmd:check pmd:cpd-check
mvn -o -Ppmd-test pmd:check
git status --short          # only intended files; no commit
```

Report: changed files, test counts before/after, public API diff (expected: only `User` ctor), any message changes and why.
