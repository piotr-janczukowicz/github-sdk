# github-sdk

Java 21 object layer over [github-rest-client](https://github.com/piotr-janczukowicz/github-rest-client): objects that
carry methods which call the GitHub REST API, such as `repo.createIssue(...)` and `issue.close()`.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.piotr-janczukowicz/github-sdk)](https://central.sonatype.com/artifact/io.github.piotr-janczukowicz/github-sdk)
![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Build](https://github.com/piotr-janczukowicz/github-sdk/actions/workflows/main.yml/badge.svg)](https://github.com/piotr-janczukowicz/github-sdk/actions/workflows/main.yml)
[![Codacy Coverage](https://app.codacy.com/project/badge/Coverage/4db2d5c4297c4124b0675df471afbdb1)](https://app.codacy.com/gh/piotr-janczukowicz/github-sdk/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_coverage)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/4db2d5c4297c4124b0675df471afbdb1)](https://app.codacy.com/gh/piotr-janczukowicz/github-sdk/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)

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
    GitHubSdk sdk = GitHubSdk.of(client);

    Repository repository = sdk.repository("octocat", "Hello-World");
    Issue issue = repository.createIssue("Found a bug");
    issue.comment("Looking into it");
    Issue closed = issue.close();                       // GitHub's answer, not a local copy

    repository.issues().limit(10).forEach(listed -> System.out.println(listed.number() + " " + listed.title()));
}
```

The `try` block only keeps the example short and closes the client at its end. In an application, create the client once
and close it once, when the application stops (see [Good practices](#good-practices)).

## What you can do

Each area below is an object you reach from `sdk`, a `Repository`, an `Organization` or a `User`. The
[full list of objects and methods](docs/guide/coverage.md) names every call.

| Area | What it covers |
|---|---|
| **Repositories** | create, update, archive, transfer, fork, star and watch; topics and languages; collaborators and invitations; webhooks and deploy keys; Pages |
| **Code** | branches and branch protection, rulesets, commits, comparisons, files, multi-file commits, refs, tags, releases and their assets, annotated tags, README |
| **Issues and pull requests** | issues, comments, labels, milestones, reactions, events, sub-issues and dependencies; pull requests, reviews, review comments, merging |
| **Actions and CI** | workflows, runs, jobs, artifacts, variables, caches, self-hosted runners, check runs, commit statuses, deployments and environments |
| **Security** | Dependabot, secret scanning and code scanning alerts, code quality, security advisories, dependency graph and SBOM, interaction limits |
| **People and organizations** | users, the signed-in account, organizations, teams, members and invitations, SSH, GPG and signing keys, notifications, gists |
| **Projects and packages** | Projects v2 (read and edit items and fields), GitHub Packages |
| **Platform** | search, rate limit, events, GitHub Apps and Marketplace, licenses, Markdown rendering |

Not covered: Copilot, codespaces, billing, Enterprise Server administration and everything that needs GraphQL, such as
discussions. Use `github-rest-client` for the REST ones. Which areas ran against the real GitHub API is listed in
[Live smoke tests](docs/guide/smoke-tests.md).

## When the SDK does not cover a call

The SDK covers the common work, not every endpoint. For the rest, use `github-rest-client` directly: the `GitHub` client
behind the SDK is yours, so there is no second client, no second credential and no second configuration. Create the
`GitHub` object yourself, hand it to `GitHubSdk.of(...)`, and keep it for the calls that have no SDK object. Both use the
same authentication, retries, cache and rate-limit handling.

```java
try (GitHubClient client = GitHubClients.create(AuthenticationProviders.bearer(System.getenv("GITHUB_TOKEN")), "my-app/1.0")) {
    GitHub github = new GitHub(GitHubCaller.of(client));
    GitHubSdk sdk = GitHubSdk.of(github);

    Repository repository = sdk.repository("octocat", "Hello-World");
    repository.createIssue("Found a bug");                                  // an SDK object

    // no SDK object for this call yet: use the client with the same coordinates
    GitHubRepositoryHashAlgorithmResponse hash =
            github.repositories().repository().getHashAlgorithm(repository.owner(), repository.name());
}
```

The client names its resources as `github.<area>().<group>().<method>(...)`, and the Javadoc of each method names the HTTP
request it sends; see the [`github-rest-client` documentation](https://github.com/piotr-janczukowicz/github-rest-client).
What the SDK returns links back to it as well: every object has `response()`, the complete client record, which you can
pass to the client again. [What the SDK covers](docs/guide/coverage.md) lists every object, so you can check first.

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
  `limit(...)` bounds the cost. An item that is gone before it is loaded is skipped.
- A field that GitHub may leave out is an `Optional` (`issue.milestone()`, `issue.author()`).
- `IOException` and the client's `GitHubApiException` pass through unchanged.

## Good practices

[Conventions](#conventions) says what the SDK does. This is what to do with it.

- **Create the client once and close it once.** One client per credential, with one `GitHubSdk` on top: the client owns the
  transport and the rate-limit state, so share it for the life of the application, and do not open a client per call. Close
  it when the application stops; a Spring `@Bean` of a `GitHubClient` is closed with the application context. The SDK never
  closes a client you passed in.
- **Keep the object an operation returns.** The object you called it on is not updated; use the result, and call
  `refresh()` only when something else may have changed the object.
- **Use handles, load when you need data.** `sdk.repository(owner, name)` costs nothing; call `fetch()` only for fields the
  handle does not hold.
- **Always bound a list you read from a large repository.** Add `limit(...)` or stop the stream early, and avoid `toList()`
  on a list that can grow, especially one that loads an item per request.
- **Prefer one request over many.** `update()`, `protect()`, `newRuleset(...)` and `commitTo(branch)` collect changes and
  send them together, instead of one call per field or per file.
- **Decide what an empty lookup means.** Handle the `Optional` instead of calling `orElseThrow()` by habit, and catch
  `GitHubApiException` (not found, forbidden, rate limit) at the place where you can act on it.
- **Configure the client, not the SDK.** Retry, backoff, the cache and page size are client settings; see
  [Configure the client](#configure-the-client).
- **Give the token what the call needs.** Prefer a fine-grained token with the permissions of the areas you use. Some areas
  need more: notifications and packages need a classic token, and creating check runs, installations and Marketplace calls
  need a GitHub App.
- **Keep secrets out of logs.** `response()` of a secret scanning alert, the payload URL of a webhook, runner tokens and the
  signed links of artifacts and logs can carry credentials; the Javadoc of each says so.
- **Try an integration on a throwaway repository first.** The [live smoke tests](docs/guide/smoke-tests.md) show how.

## Configure the client

Authentication, retries, caching, page size and the GitHub product (github.com, Enterprise Cloud or Enterprise Server)
are the client's job, not the SDK's: every object calls the client you pass to `GitHubSdk.of(...)` (a `GitHubClient`, a `GitHubCaller` or a `GitHub`), so one
configuration covers all of them. The SDK has no configuration of its own, and a call made through an object behaves
exactly like the same call on the client.

```java
GitHubClientConfiguration configuration = GitHubClientConfiguration.builder()
        .retry(RetryConfiguration.builder().maxRateLimitAttempts(5).build())
        .conditionalCacheMaxEntries(500)
        .pagination(PaginationConfiguration.builder().perPage(100).maxPages(5).build())
        .build();

try (GitHubClient client = GitHubClients.create(
        AuthenticationProviders.bearer(System.getenv("GITHUB_TOKEN")), "my-app/1.0", configuration)) {
    GitHubSdk sdk = GitHubSdk.of(client);
    // use the sdk as usual
}
```

The options are described in the `github-rest-client` documentation:

- [Configuration](https://github.com/piotr-janczukowicz/github-rest-client/blob/main/docs/guide/configuration.md): retry,
  backoff, conditional cache and timeouts
- [Rate limiting](https://github.com/piotr-janczukowicz/github-rest-client/blob/main/docs/guide/rate-limiting.md): waits
  and the rate limit listener
- [Pagination](https://github.com/piotr-janczukowicz/github-rest-client/blob/main/docs/guide/pagination.md): page size
  and the page limit; a list of the SDK is a lazy `Stream`, so `limit(...)` stops further page requests too
- [Authentication](https://github.com/piotr-janczukowicz/github-rest-client#authentication) and
  [Flavors and GitHub Enterprise](https://github.com/piotr-janczukowicz/github-rest-client/blob/main/docs/guide/flavors.md)

## Receive a webhook and act on it

Receiving webhooks is the job of a separate library,
[github-webhook](https://github.com/piotr-janczukowicz/github-webhook): a typed record for every event GitHub
documents, and a receiver that checks the signature before it parses anything. It has no dependency on the REST client
or on this SDK, so a service that only receives webhooks does not pull either in. The two fit together without a bridge,
because an event carries the owner, the repository name and the number of the issue or pull request:

```java
GitHubWebhookReceiver receiver = GitHubWebhookReceiver.of(webhookSecret);
GitHubSdk sdk = GitHubSdk.of(client);

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
`io.github.piotr_janczukowicz.github.sdk`. The other objects are grouped by area in sub-packages such as `issues`,
`pulls`, `content`, `git`, `actions`, `security`, `rulesets`, `projects` and `packages`.

The `internal` package is not exported by the module and is not part of the API. The module system hides it only on the
module path; on the classpath it is a convention and not a security boundary.

## Comparison with hub4j/github-api

[hub4j/github-api](https://github.com/hub4j/github-api) is the long-established Java library for GitHub and a fine
choice. It has the same object-oriented style as this SDK, with classes such as `GHRepository` and `GHUser` that carry the
operations. The differences are in the foundation. The hub4j column is taken from its
[documentation](https://hub4j.github.io/github-api/) as of November 2026.

| | github-sdk | hub4j/github-api |
|---|---|---|
| Style | Objects with methods, on top of a typed client generated from GitHub's OpenAPI descriptions | Objects with methods |
| Objects | Immutable; an operation returns a new object built from GitHub's answer, and `response()` gives the full typed record | Its own model classes |
| Escape hatch | The `github-rest-client` is underneath, so any operation without an object is one call away | Its own API |
| Java | 21 | 11 |
| Maturity | Prerelease, see the [Disclaimer](#disclaimer) | Long-established and widely used |

Choose hub4j/github-api if you need Java 11 or a library with a long track record. Choose this SDK if you want
immutable objects over a client that follows GitHub's own descriptions, with the full typed response always within reach.

## Disclaimer

The SDK is a thin layer over `github-rest-client`, which is built on the OpenAPI JSON schemas that GitHub publishes in
[github/rest-api-description](https://github.com/github/rest-api-description/tree/main/descriptions-next); the caveats of
that project apply here too.

**Only smoke tests have been run on the live GitHub environment, covering particular feature sets. Not every operation
has been tested. This library is a prerelease. Verify the behavior of anything you depend on before using it in
production.**
