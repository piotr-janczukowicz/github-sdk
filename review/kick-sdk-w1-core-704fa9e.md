# Kick: SCR ∥ Quality — github-sdk Wave 1 Core & internals (baseline)

- Repo: C:\Users\Admin\IdeaProjects\github-sdk
- HEAD: 704fa9e / 704fa9e41dbbdb9e5c1b18fda82af3b1a2a65094 on chore/align-repository
- Dirty: clean (empty porcelain)
- Snapshot (prefer): C:\Users\Admin\AppData\Local\Temp\github-sdk-review-704fa9e-w1
- Live tree: READ-ONLY — no mutate
- Mode: **full-tree baseline** of Wave 1 scope (not a staged diff). Review every production (and for Quality: test) file listed below as it exists at HEAD.
- Plan: C:\Users\Admin\IdeaProjects\github-sdk\review\review-plan-github-sdk-001d5de.md (waves W1–W8; this is W1 only)
- Room: SCR + Quality (no Perf, no O2J)
- Base package: `io.github.piotr_janczukowicz.github.sdk`

## Production scope (SCR + Quality)

Root (`src/main/java/.../sdk/`):
- GitHubSdk.java, Organization.java, RateLimit.java, Repository.java, RepositoryUpdate.java, RepositoryVisibility.java, Search.java, Team.java, User.java, package-info.java

internal (`.../sdk/internal/`):
- Arguments.java, ClientValues.java, Internals.java, Lookups.java, UpdateState.java, package-info.java

**Watch:** `Repository.java` (~2.3k+ LOC) — likely god-class; call out cohesion / split risks with concrete evidence, not slogans.

## Quality-only additions (tests)

Root tests (`src/test/java/.../sdk/`): all 38 `*Test.java` in that package (GitHubSdk*, Organization*, Repository*, Team*, User*, Search*, RateLimit*, ConfiguredClient*, EventsAndApps*, GlobalAdvisories*, OidcSettings*, Owner*).
internal tests: ArgumentsTest, ClientValuesTest, InternalsTest, LookupsTest, UpdateStateTest.
fixtures (`.../sdk/fixtures/`): all 23 `*Fixtures.java` (shared by later waves — review as shared test helpers this wave).

## Out of scope this wave
- Other prod packages (content, git, issues, actions, …) → W2–W7
- smoke/ ITs → W8 Quality-only
- pom.xml, .github/, docs/, README, SECURITY, LICENSE, pmd/, .idea/, target/

## Roles
- **SCR** (@superpowers-code-review): production only. Bugs, edge cases, API design risks, nullability/error handling, consistency of entry points (GitHubSdk → Repository/Organization/…), internals contracts. Critical / Important / Minor. English section → `/workspace/code-review/superpowers/github-sdk-w1-core-704fa9e-section.md`. Short Polish note in SCR + Quality room when done.
- **Quality** (@Code Quality): production + tests above. java.mdc / java-test.mdc / PMD on HW code; CPD only if hot. Do not rewrite style for its own sake. English section → `/workspace/code-quality/github-sdk-w1-core-704fa9e-section.md`. Short Polish note in room when done.
- **Perf / O2J**: idle

## Deliverable notes
- Verify everything in scope at HEAD; cite file:line evidence.
- Unique section filenames as above (do not overwrite other waves).
- Omit `/workspace/…` from any download-oriented paths meant for Piotr; use his PC paths or repo-relative.
- Merge later by Grok: `merged-*-github-sdk-w1-core-704fa9e.md`
