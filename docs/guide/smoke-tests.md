# Live smoke tests

Opt-in tests that call the real api.github.com through the SDK with a fine-grained token. `mvn verify` never runs them.

## Setup

Use the sandbox repository and token of [github-rest-client](https://github.com/piotr-janczukowicz/github-rest-client/blob/main/docs/guide/smoke-tests.md):
an empty repository whose name contains `smoke` (default `piotr-janczukowicz/github-api-smoke`) and a fine-grained token
with Issues read and write, Contents read and Metadata read on that repository. The tests refuse any other repository.

```bash
export SMOKE_GITHUB_TOKEN=github_pat_...
export SMOKE_GITHUB_REPO=piotr-janczukowicz/github-api-smoke   # optional, this is the default
```

## Run

```bash
mvn -Psmoke verify
```

Without `SMOKE_GITHUB_TOKEN` every smoke class is skipped.

## What is covered

| Class | Area |
|---|---|
| `IssuesSmokeIT` | fetch the sandbox, create an issue, comment, edit, list comments, close, reopen, delete the comment, 404 on a missing issue |

Every issue a test creates is named `smoke-<runId>-...` and is closed in a `finally` block; GitHub's REST API cannot delete
issues, so closed `smoke-` issues stay in the sandbox.

## Reading a failure

A failing smoke test means the SDK and the live API disagree. "Resource not accessible by personal access token" (403)
means the token lacks one of the permissions above.
