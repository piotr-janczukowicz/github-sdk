# github-sdk

Java 21 object layer over [github-rest-client](https://github.com/piotr-janczukowicz/github-rest-client): objects that
carry methods which call the GitHub REST API, such as `repo.createIssue(...)` and `issue.close()`.

[![Build](https://github.com/piotr-janczukowicz/github-sdk/actions/workflows/main.yml/badge.svg)](https://github.com/piotr-janczukowicz/github-sdk/actions/workflows/main.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## Why this library

- **Objects instead of coordinates.** An issue knows its repository, so `issue.comment("Thanks")` needs no owner, name or
  number.
- **What GitHub holds, not a copy.** Every operation returns a new object built from GitHub's answer. Objects are
  immutable, and `refresh()` reloads one.
- **No hidden requests.** A `Repository` from `sdk.repository("octocat", "Hello-World")` is a handle and makes no
  request until you call `fetch()`. Every other object is loaded, and every network call is a visible method call.
- **The client stays within reach.** Every object has `response()`, the complete `github-rest-client` response, and you
  keep the `GitHub` client you passed in for everything the objects do not cover.
- **Small on purpose.** Synchronous REST only: no GraphQL and no async API.

## Install

**Requirements:** Java 21. The library is a Java module, `io.github.piotr_janczukowicz.githubsdk`, and also works on the
class path. It depends on `github-rest-client`, which Maven brings in.

```xml
<dependency>
    <groupId>io.github.piotr-janczukowicz</groupId>
    <artifactId>github-sdk</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
```

Replace `LATEST_VERSION` with the newest entry on the
[releases page](https://github.com/piotr-janczukowicz/github-sdk/releases). The versions follow the OpenAPI version of
`github-rest-client` (`2026.3.10.N`). The library is in prerelease; see the [Disclaimer](#disclaimer).

## Quick start

`GitHubSdk` wraps a `GitHub` client from `github-rest-client`, so authentication, the GitHub product (github.com,
Enterprise Cloud or Enterprise Server) and limits are configured on the client as usual. The SDK has no configuration of
its own.

```java
try (GitHubClient client = GitHubClients.create(AuthenticationProviders.bearer(System.getenv("GITHUB_TOKEN")), "my-app/1.0")) {
    GitHubSdk sdk = GitHubSdk.of(new GitHub(GitHubCaller.of(client)));

    Repository repository = sdk.repository("octocat", "Hello-World");
    Issue issue = repository.createIssue("Found a bug");
    issue.comment("Looking into it");
    Issue closed = issue.close();                       // GitHub's answer, not a local copy

    repository.issues().limit(10).forEach(listed -> System.out.println(listed.number() + " " + listed.title()));
}
```

## Configure the client

Retries, caching and page size are the client's job, not the SDK's: every object calls the `GitHub` client you pass to
`GitHubSdk.of(...)`, so one configuration covers all of them. `GitHubClientConfiguration` offers:

- **Retry and backoff.** `retry(...)` sets how often a rate-limited call, a server error or an I/O failure is retried,
  the backoff and its jitter, the longest wait, and a total deadline. A rate-limited call waits for the reset time GitHub
  reports, within the configured limits, instead of failing at once.
- **Conditional cache.** The client sends `If-None-Match` and reuses the body when GitHub answers 304, which does not
  count against the rate limit. `conditionalCacheMaxEntries(...)`, `conditionalCacheMaxBodyBytes(...)` and
  `conditionalCacheMaxBytes(...)` bound it.
- **Rate limit listener.** `rateLimitListener(...)` is called with every snapshot, and `client.lastRateLimit(resource)`
  returns the latest one.
- **Paging.** `pagination(...)` sets the page size (up to 100) and the most pages one list walks. A list of the SDK is
  a lazy `Stream`, so `limit(...)` stops further page requests too.

```java
GitHubClientConfiguration configuration = GitHubClientConfiguration.builder()
        .retry(RetryConfiguration.builder()
                .maxRateLimitAttempts(5)
                .maxBackoff(Duration.ofSeconds(30))
                .totalDeadline(Duration.ofMinutes(5))
                .build())
        .conditionalCacheMaxEntries(500)
        .rateLimitListener(snapshot -> {
            if (snapshot.isExhausted()) {
                System.err.println("Rate limit exhausted until " + snapshot.resetAt());
            }
        })
        .pagination(PaginationConfiguration.builder().perPage(100).maxPages(5).build())
        .build();

try (GitHubClient client = GitHubClients.create(
        AuthenticationProviders.bearer(System.getenv("GITHUB_TOKEN")), "my-app/1.0", configuration)) {
    GitHubSdk sdk = GitHubSdk.of(new GitHub(GitHubCaller.of(client)));
    // use the sdk as usual
}
```

The SDK adds no layer of its own, so a call made through an object behaves exactly like the same call made on the
client. `ConfiguredClientTest` builds this configuration, so a change in the client's API shows up in the build.

## Receive a webhook and act on it

Receiving webhooks is the job of a separate library,
[github-webhook](https://github.com/piotr-janczukowicz/github-webhook): a typed record for every event GitHub
documents, and a receiver that checks the signature before it parses anything. It has no dependency on the REST client
or on this SDK, so a service that only receives webhooks does not pull either in. The two fit together without a bridge,
because an event carries the owner, the repository name and the number of the issue or pull request:

```java
GitHubWebhookReceiver receiver = GitHubWebhookReceiver.of(webhookSecret);
GitHubSdk sdk = GitHubSdk.of(new GitHub(GitHubCaller.of(client)));

GitHubWebhookEvent event = receiver.receive(
        request.header("X-GitHub-Event"), request.header("X-Hub-Signature-256"), request.readBody());

if (event instanceof GitHubWebhookIssuesOpenedEvent opened) {
    Repository repository = sdk.repository(opened.repository().owner().login(), opened.repository().name());
    repository.issue(opened.issue().number()).orElseThrow().comment("Thanks, we will look at it");
}
```

The event comes from `github-webhook` and the `Issue` from this SDK, so add both dependencies. This example is not built
by the tests of this repository; the record and accessor names are those of `github-webhook` and change when that library
is synced with GitHub's descriptions.

## Packages

The entry points (`GitHubSdk`, `Repository`, `User`, `Organization`, `Team`, `Search`, `RateLimit`) are in
`io.github.piotr_janczukowicz.github.sdk`. The other objects are grouped by area in sub-packages: `issues`, `pulls`,
`content` (branches, commits, files, releases), `git` (refs, comparisons, multi-file commits), `gists`,
`accounts`, `organizations`, `notifications`, `metrics`, `utilities`, `deploykeys`, `deployments`, `pages`, `security`, `rulesets`, `projects`, `actions`, `checks`, `collaborators`, `reactions` and
`webhooks`. The `internal` package is not exported by the module and is not part of the API.

## What is covered

| Entry point | Objects and what they do |
|---|---|
| `sdk.repository(owner, name)` | `Repository`: `fetch()`, issues, pull requests, branches, commits, files (`file(path)`, `writeFile(...)`), releases, tags, labels, milestones, webhooks |
| `Issue` | `update()`, `refresh()`, `close()`, `reopen()`, `lock()`, `unlock()`, labels, assignees, `comment(...)`, `comments()`, `react(...)`, `reactions()`, `events()`, `timeline()` |
| `Collaborator`, `Invitation` | `repository.collaborators()`, `isCollaborator(...)`, `collaboratorPermission(...)`, `addCollaborator(...)`, `invitations()`; a collaborator has `remove()`, an invitation `changePermission(...)` and `cancel()` |
| `Repository` (activity) | `star()`, `unstar()`, `isStarred()`, `stargazers()`, `watchers()`, `subscription()`, `watch()`, `ignore()`, `unwatch()`, `forks()`, `fork()`; `User` lists `starredRepositories()` and `watchedRepositories()` |
| `Repository` (management) | `update()` (name, description, homepage, visibility, default branch, merge settings), `archive()` (not undoable through the API), `delete()`, `transfer(...)`, `topics()`, `replaceTopics(...)`, `languages()`, `contributors()`, `dispatchEvent(...)`, `generate(...)` from a template; `sdk.createRepository(payload)` creates one for the authenticated user |
| `Workflow`, `WorkflowRun`, `WorkflowJob`, `Artifact`, `Variable` | `repository.workflows()`, `workflowRuns()`, `artifacts()`, `variables()`, `createVariable(...)`; a workflow has `enable()`, `disable()`, `dispatch(ref)` and `runs()`; a run has `cancel()`, `rerun()`, `rerunFailedJobs()`, `delete()`, `jobs()`, `artifacts()` and `logs()` (a short-lived link, nothing is downloaded); `repository.secrets()` lists names only, because creating a secret needs encryption that the client covers |
| `Reaction` | `content()`, `user()`, `delete()`; made on an issue, an `IssueComment` or a `PullRequestReviewComment` with `react(...)` |
| `PullRequest` | `update()`, `refresh()`, `close()`, `reopen()`, `merge(...)`, `reviews()`, `review(...)`, `draftReview(...)`, `reviewComments()`, `commentOnLine(...)`, reviewers, files, commits, `issue()` |
| `PullRequestReview` | `edit(...)`, `submit(...)`, `dismiss(...)`, `deletePending()`, `comments()`, `refresh()` |
| `PullRequestReviewComment` | `edit(...)`, `reply(...)`, `delete()`, `refresh()` |
| `sdk.gist(id)`, `sdk.gists()`, `sdk.createGist()` | `Gist`: `update()`, `refresh()`, `delete()`, `star()`, `unstar()`, `isStarred()`, `fork()`, `forks()`, `commits()`, `revision(sha)`, `comments()`, `comment(...)`; `GistComment`: `edit(...)`, `delete()`; `sdk.publicGists()`, `sdk.starredGists()` and `user.gists()` list gists |
| `sdk.notifications()`, `repository.notifications()` | `Notification`: `markRead()`, `markDone()`, `subscription()`, `subscribe()`, `ignore()`, `unsubscribe()`, `refresh()`; `sdk.markNotificationsRead()` marks all as read. Needs a classic token, because GitHub does not support fine-grained tokens for notifications |
| `Repository` (deployments) | `deployments()`, `deployment(id)`, `deploy(ref, environment)` (GitHub's auto merge is off), `deploy(payload)`; a `Deployment` has `statuses()`, `latestStatus()`, `createStatus(state)`, `refresh()` and `delete()`; `environments()`, `environment(name)` and `saveEnvironment(name)` give `Environment` objects with `refresh()` and `delete()` |
| `Repository` (Pages) | `pages()` gives a `PagesSite` with `update(...)`, `delete()`, `builds()`, `latestBuild()` and `requestBuild()`; `enablePages(branch)` turns Pages on |
| `Repository` (security) | `dependabotAlerts()`, `secretScanningAlerts()`, `codeScanningAlerts()` and a lookup by number for each; every alert has `refresh()`, `dismiss(...)` or `resolve(...)` and `reopen()`. A secret scanning alert never returns the secret from its accessors, only from `response()`. `security()` reads and switches Dependabot alerts, automated security fixes, private vulnerability reporting and immutable releases |
| `Repository` (metrics) | `traffic()` (views, clones, top paths and referrers, needs push access), `statistics()` (contributors, commit activity, code frequency, participation, punch card; empty while GitHub is still computing them) and `communityProfile()` |
| `Commit` (comments), `Autolink`, `CheckSuite` | `commit.comments()`, `commit.comment(...)`, `repository.commitComments()` with `CommitComment` (`edit(...)`, `delete()`); `repository.autolinks()`, `addAutolink(...)`; `commit.checkSuites()`, `repository.checkSuite(id)` with `rerequest()` |
| `sdk.licenses()`, `sdk.utilities()` | licenses (`sdk.license(key)`, `repository.license()`), and `Utilities`: `zen()`, `overview()`, `emojis()`, `renderMarkdown(...)`, `gitignoreTemplates()` |
| `Repository` (deploy keys) | `deployKeys()`, `deployKey(id)`, `addDeployKey(title, key[, isReadOnly])`; a `DeployKey` has `refresh()` and `delete()` |
| `Repository` (git data) | `commitTo(branch)` collects `write(...)` and `delete(...)` and `send(message)` makes one commit; `compare(base, head)` gives a `Comparison` (status, ahead and behind, commits, files, `diff()`, `patch()`); `refs(prefix)`, `ref(name)`, `createRef(...)` and `createBranch(...)` give `Ref` objects with `update(...)` and `delete()`; `tree(sha)` and `blob(sha)` read git objects |
| `Branch` (protection) | `protection()` reads the rule (`BranchProtection`: `refresh()`, `remove()`); `protect()` collects status checks, review requirements, admin enforcement, push restrictions and the other options, and `send()` sets the rule. GitHub replaces the whole rule, so anything not chosen is switched off |
| `Ruleset`, `RulesetDraft`, `RulesetUpdate`, `Rules` | `repository.rulesets()`, `ruleset(id)`, `newRuleset(name, target, enforcement)` collecting rules (`Rules.requirePullRequest(...)`, `requireStatusChecks(...)`, `requireLinearHistory()`, `restrictDeletion()` and more, or any client rule through `rule(...)`), ref patterns and bypass actors, then `create()`; a ruleset has `edit()` (`name`, `enforcement`, `refs`, `addRule`, `removeRule`, `send()`), `refresh()`, `history()` and `delete()`. `repository.ruleSuites()` and `ruleSuite(id)` return how rulesets judged pushes and merges, and `branch.rules()` lists the rules of rulesets that apply to a branch (not branch protection). Rulesets are the newer replacement of branch protection. GitHub replaces the whole list of rules on update, so `addRule` and `removeRule` read the current rules first. The evaluate (dry run) enforcement needs the Enterprise plan |
| `Project`, `ProjectItem`, `ProjectField`, `ProjectView` | `organization.projects()`, `project(number)` and the same on `User` read Projects v2. A project has `items()` (or `items(query)`), `item(id)`, `addIssue(...)`, `addPullRequest(...)`, `addDraft(...)`, `fields()`, `field(id)`, `addTextField(...)`, `addNumberField(...)`, `addDateField(...)`, `addSingleSelectField(...)` and `createView(...)`; an item has `setField(...)`, `clearField(...)`, `refresh()` and `delete()`; a view has `items()`. Every object has a `nodeId()` for GraphQL. GitHub's REST API cannot create, edit, close or delete a project, edit or delete a field, delete a view, or write status updates, so those are not here and need GraphQL |
| `Branch`, `Commit`, `RepositoryFile` | read; a file gives `text()` and `bytes()`, and writes with `writeFile(...)` and `delete(...)` |
| `Commit` (CI) | `checkRuns()`, `checkRuns(name)`, `statuses()`, `combinedStatus()`, `createStatus(...)` |
| `CheckRun`, `CommitStatus`, `CombinedStatus` | read-only views of CI results; `CheckRun` also has `annotations()` and `refresh()` |
| `Release`, `Label`, `Milestone`, `Webhook` | `update()`, `refresh()`, `delete()` |
| `sdk.currentUser()`, `sdk.user(login)` | `User`: `repositories()`, `organizations()`, `gists()`, `followers()`, `following()`, `sshKeys()`, `gpgKeys()`, `signingKeys()`, `socialAccounts()` |
| `sdk.me()` | `Me`, for the account the client authenticates as: `update()` the profile, `emails()`, `addEmail(...)`, `changeEmailVisibility(...)`; SSH, GPG and signing keys (`SshKey`, `GpgKey` and `SigningKey` have `refresh()` and `delete()`); `follow(...)`, `block(...)` and their lists; social accounts |
| `sdk.organization(login)` | `Organization`: `repositories()`, `createRepository(...)`, `members()`, `isMember(...)`, `membership(...)`, `changeMembership(login, role)`, `removeMember(...)`, `outsideCollaborators()`, `inviteByEmail(...)`, `inviteUser(...)`, `invitations()`, `teams()`, `team(slug)`, `createTeam(...)`, `webhooks()`, `createWebhook(...)`, `variables()`, `createVariable(...)`, `secrets()` (names only), `deleteSecret(...)` |
| `Team`, `OrganizationInvitation`, `OrganizationWebhook`, `OrganizationVariable` | `Team`: `members()`, `repositories()`, `childTeams()`, `update(...)`, `delete()`, `addMember(...)`, `removeMember(...)`, `membership(...)`, `addRepository(...)`, `removeRepository(...)`; an invitation has `cancel()`; a webhook has `update(...)`, `ping()`, `deliveries()`, `refresh()` and `delete()`; a variable has `update(value)`, `refresh()` and `delete()` |
| `sdk.search()` | issues, repositories, users, code and commits |
| `sdk.rateLimit()` | `RateLimit` |

Copilot, Apps and installations, packages, codespaces, GitHub Enterprise Server administration, billing, enterprise
administration, code scanning uploads, CodeQL databases and everything that needs GraphQL, such as discussions, have no
objects here yet; use `github-rest-client` for the REST ones. Which areas ran against the real GitHub API is listed in
[Live smoke tests](docs/guide/smoke-tests.md).

## Conventions

All objects behave the same way, so you learn them once:

- Objects are **immutable**. An operation returns a new object; the old one keeps its old state.
- A **lookup** (`repository.issue(42)`, `sdk.user("octocat")`) returns an `Optional`, empty when GitHub answers 404.
- A **list** is a lazy, single-use `Stream`. Pages are fetched as you consume it, so `limit(...)` stops further requests;
  a transport failure during iteration is an `UncheckedIOException`.
- **Several fields at once** go through `update()`: `issue.update().title("New").labels(List.of("bug")).send()` sends one
  request with only the fields you set. `send()` with nothing set throws `IllegalStateException`.
- **State changes** that are not fields are methods on the object: `close()`, `reopen()`, `merge(...)`, `lock()`.
- `refresh()` reloads an object and throws `IllegalStateException` when it is gone.
- Where GitHub's list returns a smaller record than a single lookup (pull requests, branches, teams, search hits for
  issues and users), the SDK loads each item with one more request as the stream reaches it. The Javadoc says so, and
  `limit(...)` bounds the cost.
- `IOException` and the client's `GitHubApiException` pass through unchanged.

## Smoke tests

The smoke tests call the real GitHub API and are opt-in; see [Live smoke tests](docs/guide/smoke-tests.md).

## Disclaimer

The SDK is a thin layer over `github-rest-client`, which is built on the OpenAPI JSON schemas that GitHub publishes in
[github/rest-api-description](https://github.com/github/rest-api-description/tree/main/descriptions-next); the caveats of
that project apply here too.

**Only smoke tests have been run on the live GitHub environment, covering particular feature sets. Not every operation
has been tested. This library is a prerelease. Verify the behavior of anything you depend on before using it in
production.**
