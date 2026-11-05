# Live smoke tests

Opt-in tests that call the real api.github.com through the SDK with a fine-grained token. `mvn verify` never runs them.

## Setup

Use the sandbox repository of
[github-rest-client](https://github.com/piotr-janczukowicz/github-rest-client/blob/main/docs/guide/smoke-tests.md): an
empty repository with a README whose name contains `smoke` (default `piotr-janczukowicz/github-api-smoke`). The tests
refuse any other repository.

Create a fine-grained personal access token. Resource owner: the owner of the sandbox. Repository access: only the
sandbox repository.

- Repository permissions, read and write: Actions, Administration, Commit statuses, Contents, Issues, Pull requests,
  Variables, Webhooks, Deploy keys. The Actions, Administration, Deploy keys and Variables tests are skipped, not failed, when GitHub answers
  403.
- Repository permissions, read: Metadata.
- Account permissions, read and write: Gists, and Starring and Watching, when your token form offers them. The gist,
  star and watch tests are skipped, not failed, when GitHub answers 403.

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
| `CollaboratorsSmokeIT` | list collaborators, check membership, the owner's permission level, list invitations. Adding, removing and changing collaborators are not run live, because an invitation goes to a real account |
| `ActivitySmokeIT` | star, check, list stargazers and starred repositories, unstar; watch, ignore and unwatch; list watchers and forks. The tests restore your original star and subscription. Forking is not run live, because a user cannot fork their own repository |
| `ActionsSmokeIT` | create, list, update, delete an Actions variable; list workflows and runs, reload runs, list their jobs and log links; 404 on a missing run. Dispatching a workflow and cancelling the run runs only when `SMOKE_GITHUB_WORKFLOW` names the file of a `workflow_dispatch` workflow already on the sandbox default branch, so the tests never commit workflow files |
| `RepositoryManagementSmokeIT` | change and restore the description and homepage, replace and restore the topics, list languages and contributors. Create, rename, transfer, archive and delete are not run live: fine-grained tokens cannot create repositories, and GitHub's API cannot unarchive one |
| `BranchProtectionSmokeIT` | protect a throwaway branch, replace the rule, read and refresh it, remove it. The default branch is never touched. Skipped when GitHub answers 403, which a private repository on a plan without branch protection does |
| `GitDataSmokeIT` | create a throwaway branch, commit two files and then delete one as separate commits, compare with the default branch, force the ref back, delete it; list refs and load a tree |
| `GistsSmokeIT` | create a secret gist, update and rename a file, comment (edit, delete), star and unstar, list its revisions and your gists, delete it; 404 on a missing gist. Forking is not run live, because GitHub does not let you fork your own gist. Skipped when GitHub answers 403 |
| `DeployKeysAndNotificationsSmokeIT` | add a throwaway Ed25519 deploy key, list, reload and delete it; list your notifications without changing any. GitHub does not support fine-grained tokens for notifications, so that test is skipped on 403 or 404; marking notifications as read is never run live |
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
