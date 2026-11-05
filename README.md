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
- **The client stays within reach.** Every object has `record()`, the complete `github-rest-client` response, and you
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

## What is covered

| Entry point | Objects and what they do |
|---|---|
| `sdk.repository(owner, name)` | `Repository`: `fetch()`, issues, pull requests, branches, commits, files (`file(path)`, `writeFile(...)`), releases, tags, labels, milestones, webhooks |
| `Issue` | `update()`, `refresh()`, `close()`, `reopen()`, `lock()`, `unlock()`, labels, assignees, `comment(...)`, `comments()` |
| `PullRequest` | `update()`, `refresh()`, `close()`, `reopen()`, `merge(...)`, reviews, reviewers, files, commits, `issue()` |
| `Branch`, `Commit`, `RepositoryFile` | read; a file gives `text()` and `bytes()`, and writes with `writeFile(...)` and `delete(...)` |
| `Release`, `Label`, `Milestone`, `Webhook` | `update()`, `refresh()`, `delete()` |
| `sdk.currentUser()`, `sdk.user(login)` | `User`: `repositories()`, `organizations()` |
| `sdk.organization(login)` | `Organization`: `repositories()`, `createRepository(...)`, `members()`, `teams()`, `team(slug)`; `Team`: `members()`, `repositories()` |
| `sdk.search()` | issues, repositories, users, code and commits |
| `sdk.rateLimit()` | `RateLimit` |

Security products, Copilot, Apps and installations, GitHub Enterprise Server administration, billing and enterprise
administration have no objects here; use `github-rest-client` for them.

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
