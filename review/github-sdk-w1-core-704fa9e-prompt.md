# Coding-agent prompt: github-sdk W1 Core & internals Quality fixes (HEAD 704fa9e)

Source: Quality section `github-sdk-w1-core-704fa9e-section.md` (Q-IDs).

## Constraints (hard)
- **No commit.** Do not touch untracked `tmp-*.py`. Do not edit live tree outside intended W1 files. Prefer a clean working tree aside from your changes.
- **No behaviour change** beyond what a finding states. Preserve exception types and messages. Public API signatures stay the same unless **DECISION D-1** explicitly chooses a visible split (default: no).
- Do not use `-Dpmd.skip`. Do not change JaCoCo thresholds or the smoke-profile `jacoco.skip`.
- Do not rewrite facade boilerplate for style alone. Do not mass-edit `satisfies`/`hasValueSatisfying` tests unless you are already in that file for Q4.

## Baseline (before Part 1)
```
cd <github-sdk>
git rev-parse HEAD                    # expect 704fa9e41dbbdb9e5c1b18fda82af3b1a2a65094 or successor after merge
git status --short
mvn -o spotless:check
mvn -o clean test                     # record: unit test count, failures = 0
mvn -o pmd:check pmd:cpd-check        # record: PMD 0; CPD 0 (or note existing)
```

## DECISION (Piotr), with defaults
1. **D-1 Repository / Organization cohesion (Q2, optional Q3):**
   - Default **A**: Keep the public `Repository` (and `Organization`) facade. Apply Q1. Optionally apply Q3 private helper only. No new public types.
   - Alternative **B**: Extract package-private (or public) domain collaborators and delegate; public methods on `Repository` remain trampolines. Coordinate with SCR before merging; update tests only if constructors move.
2. **D-2 Nested `satisfies` soft-assert cleanup (Q6):**
   - Default **A**: Fix only when editing the same test for Q4; no repo-wide sweep.
   - Alternative **B**: Convert all nested multi-`assertThat` in W1 tests to `assertSoftly` / chained `returns`.
3. **D-3 ConfiguredClientTest documentation (Q5):**
   - Default **A**: Delete the class Javadoc (java-test: no Javadoc).
   - Alternative **B**: Move the README-guard note into a one-line method name / README only (still no Javadoc on the class).

## Part 1: Production — sealed switch (+ optional helper) (Q1, Q3, D-1 A)
- In `Repository.read`, replace `instanceof final GitHubContentFile` with an exhaustive `switch` on `GitHubContentResult`.
- Non-file permits → `Optional.empty()` (same behaviour as today).
- If D-1 A and you want Q3: extract a private `requireListed` (or similar) for the three number-based `load*` helpers; do not change messages (`"… is gone"` via `IS_GONE`).

**Acceptance:**
- [ ] `read` uses exhaustive `switch` on `GitHubContentResult` (no `instanceof`).
- [ ] Existing Repository content/file tests still pass without message changes.
- [ ] No new public API (unless D-1 B was chosen and documented).

## Part 2: Tests — soft asserts & one Act (Q4, D-2 A, D-3)
- Present+value tests: one chain (`assertThat(found).get()…`) or `assertSoftly`.
- Split multi-Act tests in `ArgumentsTest` (empty vs non-empty; path vs ref; `..` vs `.`; newline vs DEL) into separate `@Test` methods or a `@ParameterizedTest` with the same assertion shape.
- `ClientValuesTest` name-map + size: `assertSoftly` or split size into its own test.
- `InternalsTest.register_…`: `assertSoftly` for the two `Internals.github` asserts.
- `ConfiguredClientTest`: remove class Javadoc (D-3 A); soft-assert or split the two independent asserts in `defaults_shouldKeep…`.

**Acceptance:**
- [ ] No W1 test in the Q4 list has ≥2 top-level `assertThat` without `assertSoftly` (nested-only left only under D-2 A).
- [ ] No Q4 method has two production Acts in one `@Test` unless parameterized with one Act per invocation.
- [ ] Names stay `{method}_should{Outcome}_when{Condition}`.
- [ ] Unit test count only stays the same or **increases** (from splits); no silent deletions.

## Part 3 (optional): D-1 B only — facade extract
Skip unless Piotr chose D-1 B. Extract collaborators; keep public signatures; SCR reviews API shape.

**Acceptance:**
- [ ] Public call sites compile unchanged.
- [ ] SCR section ACK or paired review note.

## Verify block (after EACH part)
```
mvn -o spotless:check
mvn -o clean test
mvn -o pmd:check pmd:cpd-check
git status --short       # only intended files; no commit; tmp-*.py untouched
```
Report: changed files, unit test counts before/after, any public API change (expected: none under D-1 A).
