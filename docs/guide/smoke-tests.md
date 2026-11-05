# Live smoke tests

Opt-in tests that call the real api.github.com through the SDK with a fine-grained token. `mvn verify` never runs them.

## Setup

Use the sandbox repository of
[github-rest-client](https://github.com/piotr-janczukowicz/github-rest-client/blob/main/docs/guide/smoke-tests.md): an
empty repository with a README whose name contains `smoke` (default `piotr-janczukowicz/github-api-smoke`). The tests
refuse any other repository.

Create a fine-grained personal access token. Resource owner: the owner of the sandbox. Repository access: only the
sandbox repository.

- Repository permissions, read and write: Commit statuses, Contents, Issues, Pull requests, Webhooks.
- Repository permissions, read: Metadata.

Reading check runs needs the `checks: read` permission (GitHub names it in the `X-Accepted-GitHub-Permissions` response
header), but the fine-grained token form does not offer it and no other permission grants it. A classic token with the
`repo` scope can read them. The check-run test is skipped, not failed, when GitHub answers 403.

```bash
export SMOKE_GITHUB_TOKEN=github_pat_...
export SMOKE_GITHUB_REPO=piotr-janczukowicz/github-api-smoke   # optional, this is the default
```

## Run

```bash
mvn -Psmoke verify
mvn -Psmoke verify -Dit.test=IssuesSmokeIT
```

Without `SMOKE_GITHUB_TOKEN` every smoke class is skipped.

## What is covered

| Class | Area |
|---|---|
| `IssuesSmokeIT` | fetch the sandbox, create, list, update, comment, edit and delete comments, close and reopen, labels, lock, 404 on a missing issue |
| `PullRequestsSmokeIT` | create a pull request between two branches, read, list, files, commits, 404 |
| `PullRequestWriteSmokeIT` | update, close, reopen, comment through the issue, squash merge into a throwaway branch |
| `PullRequestReviewsSmokeIT` | comment review, draft review (edit, submit, delete), inline comment (reply, edit, list, delete), review comments. Approve, request changes and dismiss need a second user, because GitHub refuses them on one's own pull request |
| `ChecksSmokeIT` | commit status: create, list, combined state; check runs of the head commit when there are any; 404 on a missing check run. Check runs cannot be created, because GitHub accepts that only from GitHub Apps |
| `ReactionsSmokeIT` | react, list and delete a reaction on an issue, an issue comment and a pull request review comment; issue events and timeline |
| `ContentSmokeIT` | default branch, its head commit, branch and commit lists, 404 on a missing branch |
| `FilesSmokeIT` | write, update, read, delete a file on a throwaway branch (binary and UTF-8 text), directory and missing paths |
| `ReleasesSmokeIT` | draft release: create, update, list, refresh, delete; latest release; tags |
| `LabelsAndMilestonesSmokeIT` | labels and milestones: create, read, list, update, refresh, close, reopen, delete; a milestone on an issue |
| `WebhooksSmokeIT` | create, list, update, refresh, delete a webhook |
| `AccountsSmokeIT` | current user, user lookup, public repositories of a user |
| `OrganizationsSmokeIT` | organization lookup and the user's organizations; repositories, members and teams run only when the sandbox owner is an organization |
| `SearchSmokeIT` | search issues, repositories, users and commits; rate limit |

GitHub's list of issues can lag behind a write: it did not show an issue created seconds earlier, even with every
state selected. The issue smoke test therefore lists issues that already exist and loads one of them by number, instead
of expecting a new issue in the list.

Every resource a test creates is named `smoke-<runId>-...` and is removed in a `finally` block, so a failed run leaves
nothing behind. Two exceptions: GitHub's REST API cannot delete issues, so closed `smoke-` issues stay in the sandbox,
and a merged pull request stays in its history.

Not covered: Actions, creating check runs, Git data objects beyond what the tests need, gists, projects, packages, deploy keys,
reactions on releases and commit comments, reviewer requests on a pull request (they need a second user), GitHub Enterprise Server, and the organization
operations on a personal-account sandbox.

## Reading a failure

A failing smoke test means the SDK and the live API disagree. "Resource not accessible by personal access token" (403)
means the token lacks one of the permissions above. A 422 from a search means the query is not valid for that search
type.
