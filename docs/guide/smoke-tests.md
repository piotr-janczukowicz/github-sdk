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
  Variables, Webhooks, Deploy keys, Deployments, Environments, Pages. Account permissions, read and write: Email addresses, Git SSH keys, SSH signing keys. Repository permissions, read: Dependabot alerts, Secret scanning alerts, Code scanning alerts. The Actions, Administration, Deploy keys, Deployments, Environments, Pages and Variables tests are skipped, not failed, when GitHub answers
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
| `PackagesSmokeIT` | list the container packages of the sandbox owner. Skipped when GitHub refuses the token, which it does for every fine-grained token, because the REST API for packages needs a classic token with `read:packages` |
| `InteractionsAndCodeQualitySmokeIT` | set, read and remove a one-day interaction limit on the sandbox, and read the code quality findings and setup. The limit test needs the Administration permission; code quality is skipped on 403. Right after a removal, GitHub once answered the next set with an empty body, which the client refuses to decode; running the test again passed |
| `EventsSmokeIT` | read the repository, repository network, public and user event feeds. The feeds may be empty for a quiet sandbox, so only the public feed is required to hold events |
| `RulesetsSmokeIT` | create an active ruleset for a ref pattern that no branch matches, rename and disable it, add and remove rules, reload it, list it, read its history, delete it, read the rule suites. It never touches an existing branch. The evaluate (dry run) enforcement is not covered, because GitHub offers it only on the Enterprise plan. Skipped when GitHub answers 403 |
| `ProjectsSmokeIT` | add a draft item to a project of the sandbox owner, list the fields and items, set a text field when the project has one, reload the item and remove it. REST cannot create a project, so create one by hand and set `SMOKE_GITHUB_PROJECT` to its number; the token needs the account permission Projects, read and write. Skipped without the variable or on 403 |
| `BranchProtectionSmokeIT` | protect a throwaway branch, replace the rule, read and refresh it, remove it. The default branch is never touched. Skipped when GitHub answers 403, which a private repository on a plan without branch protection does |
| `GitDataSmokeIT` | create a throwaway branch, commit two files and then delete one as separate commits, compare with the default branch, force the ref back, delete it; list refs and load a tree |
| `GistsSmokeIT` | create a secret gist, update and rename a file, comment (edit, delete), star and unstar, list its revisions and your gists, delete it; 404 on a missing gist. Forking is not run live, because GitHub does not let you fork your own gist. Skipped when GitHub answers 403 |
| `DeployKeysAndNotificationsSmokeIT` | add a throwaway Ed25519 deploy key, list, reload and delete it; list your notifications without changing any. GitHub does not support fine-grained tokens for notifications, so that test is skipped on 403 or 404; marking notifications as read is never run live |
| `DeploymentsSmokeIT` | create a deployment on the default branch, report statuses, list it, mark it inactive and delete it; save, list, reload and delete an environment; read Pages. Enabling Pages is not run live, because a private repository on a free plan refuses it |
| `SecuritySmokeIT` | list Dependabot, secret scanning and code scanning alerts and reload them, read the four security switches. Reads only: no alert is dismissed or resolved and no switch is changed. Each test is skipped when GitHub answers 403 or 404, which is the case when the permission is missing or the feature is off |
| `RepositoryExtrasSmokeIT` | create, list, edit and delete a commit comment; add, list, reload and delete an autolink; read the community profile, statistics, traffic and license of the repository; read licenses, emojis, Markdown rendering and gitignore templates. Traffic needs push access, so that test skips on 403 |
| `MeSmokeIT` | add, list, reload and delete a throwaway SSH key and a throwaway signing key; read your profile, emails, followers, following, blocked users and social accounts, and the public keys of your own user. Following, blocking and email changes are never run live, because they would affect real accounts. Key tests skip on 403 |
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

## Not yet verified live

These areas are covered by unit tests only, because the smoke tests skip with a fine-grained token or on the private
sandbox repository. Their behavior against the real GitHub API has not been checked:

| Area | Why it skips |
|---|---|
| Gists | the token has no Gists permission |
| Deployments and Pages | the token has no Deployments or Pages permission; Pages also needs a plan that offers it for private repositories |
| Security alerts and switches | the token has no Dependabot, secret scanning or code scanning permission, or the feature is off |
| Notifications | GitHub does not support fine-grained tokens for them |
| Check runs | fine-grained tokens have no Checks permission |
| Stars and watching | the token form does not offer the permissions |
| Workflow dispatch | `SMOKE_GITHUB_WORKFLOW` is not set |
| Apps, installations and Marketplace | they need an App JWT from a registered GitHub App and its private key, so they are covered by unit tests only |
| Packages | the REST API for packages does not support fine-grained tokens; deleting and restoring packages is not smoke tested even with a classic token, because the sandbox has no packages to lose |
| Projects v2 | the token has no Projects permission and `SMOKE_GITHUB_PROJECT` is not set; adding fields and views is not smoke tested, because REST cannot delete them again |
| Organizations and teams, including membership, invitations, team management, organization webhooks and variables | the account has no organization, so these are covered by unit tests only |

A classic token or a public sandbox repository would cover most of them.

## Reading a failure

A failing smoke test means the SDK and the live API disagree. "Resource not accessible by personal access token" (403)
means the token lacks one of the permissions above. A 422 from a search means the query is not valid for that search
type.
