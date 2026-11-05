# github-sdk — code review wave plan (HEAD 001d5de)

Repo: C:\Users\Admin\IdeaProjects\github-sdk (origin https://github.com/piotr-janczukowicz/github-sdk.git)
Branch: chore/align-repository (ahead of origin by 1). Working tree DIRTY: 13 prod + 5 test files modified, new internal/Arguments.java + ArgumentsTest.java untracked.
Build: Maven (pom.xml, parent common-parent:15), artifact github-sdk 2026.3.10.0-SNAPSHOT. Single module. Depends on github-rest-client and common-test-support (test). No direct dependency on github-api / github-webhook-internal. No OpenAPI/generated sources found; all code is hand-written.
Base package: src/main/java/io/github/piotr_janczukowicz/github/sdk (below: paths relative to it).

Totals: production 171 files / ~19.6k LOC (29 packages); tests 214 files / ~25.5k LOC (incl. fixtures 23 files, smoke ITs 38 files).

## Rules
- SCR (superpowers-code-review): src/main only.
- Quality (Code Quality): src/main + matching src/test packages.
- One wave at a time; findings fixed/triaged before the next wave.

## Waves
| # | Name | Prod packages | SCR files/LOC | Quality adds tests | Why |
|---|------|---------------|---------------|--------------------|-----|
| W1 | Core & internals | (root: GitHubSdk, Repository 2368 LOC, Organization 627, ...), internal | 17 / ~4.8k | root 38, internal 5, fixtures 23 (66 / ~8.3k) | Entry points + shared helpers every other package uses. Repository.java is the biggest risk (god-class). |
| W2 | Repo content & git | content, git, collaborators, deploykeys, webhooks, interactions | 27 / ~2.5k | 21 / ~2.2k | Repository sub-resources around code, refs, access. |
| W3 | Collaboration flow | issues, pulls, reactions, checks | 26 / ~2.8k | 23 / ~2.9k | Issue/PR/review/check lifecycle, cross-referencing types. |
| W4 | CI/CD | actions, deployments, pages | 19 / ~2.3k | 13 / ~2.1k | Workflows, runs, runners, caches, environments. |
| W5 | Security & governance | security, advisories, dependencies, rulesets | 24 / ~2.2k | 14 / ~2.0k | Code/secret scanning, advisories, rules — sensitive semantics. |
| W6 | Accounts & orgs | accounts, organizations, apps, notifications | 21 / ~2.0k | 10 / ~1.7k | User/org/app identity surface. |
| W7 | Misc leaf APIs | projects, packages, gists, metrics, utilities | 27 / ~2.4k | 19 / ~2.7k | Independent leaves, low coupling. |
| W8 | Smoke ITs (Quality only) | — | 0 (SCR skip) | smoke 38 / ~3.3k | Integration tests against live API; review for flakiness, secrets handling, cleanup. |

Kick order: W1 → W2 → W3 → W4 → W5 → W6 → W7 → W8. Per wave: SCR and Quality may run in parallel on the same prod scope.
If W1 is too heavy, split: W1a = Repository.java alone; W1b = rest of root + internal.

## Out of scope (both reviewers)
- pom.xml, .github/workflows (main, pull-request, publish, release, codeql, scorecard), dependabot.yml, maven-settings.xml, CODEOWNERS — build/CI config; optional separate light pass.
- docs/ (5 files), README.md, SECURITY.md, LICENSE, pmd/, .codacy.yml, .idea/, target/, .checkstyle-cache, .spotless-index.
- package-info.java files: included but trivial.

## Baseline vs diff
Project is new: do a full-tree baseline (W1–W8) first. After baseline, switch to staged-diff-only reviews per change.
Before starting: commit or stash the current dirty chore/align-repository changes (Arguments.java etc.) so the baseline is pinned to a clean SHA.

## Open questions
1. Baseline against 001d5de + uncommitted changes, or wait until chore/align-repository is committed/pushed?
2. Include smoke ITs (W8) in Quality, or skip as live-API tests?
3. Review build/CI config at all?
