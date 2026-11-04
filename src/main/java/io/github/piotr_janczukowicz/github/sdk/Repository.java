package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.issues.GitHubCreatePayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubCreatePayloadBuilder;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubCreatePullRequestPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubListPullRequestsFilter;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubFullRepository;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubPullRequestSimple;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A repository, identified by its owner and name. The object holds no data from GitHub yet; it is the starting point
 * for the operations that belong to a repository.
 */
public final class Repository {

    private final GitHubSdk sdk;
    private final String owner;
    private final String name;
    private final @Nullable GitHubFullRepository record;

    Repository(
            final GitHubSdk sdk, final String owner, final String name, final @Nullable GitHubFullRepository record) {
        this.sdk = sdk;
        this.owner = owner;
        this.name = name;
        this.record = record;
    }

    /**
     * Returns the account that owns the repository.
     *
     * @return the owner login
     */
    public String owner() {
        return owner;
    }

    /**
     * Returns the repository name.
     *
     * @return the name without the owner
     */
    public String name() {
        return name;
    }

    /**
     * Loads the repository ({@code GET /repos/{owner}/{repo}}). This object is not changed.
     *
     * @return the loaded repository, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Repository> fetch() throws IOException {
        return sdk.github()
                .repositories()
                .repository()
                .get(owner, name)
                .map(loaded -> new Repository(sdk, owner, name, loaded));
    }

    /**
     * Returns the complete response of a loaded repository.
     *
     * @return the response record
     * @throws IllegalStateException if this is a handle that was not loaded with {@link #fetch()}
     */
    public GitHubFullRepository record() {
        if (record == null) {
            throw new IllegalStateException("Repository " + owner + "/" + name + " is not loaded; call fetch()");
        }
        return record;
    }

    /**
     * Creates an issue with a title ({@code POST /repos/{owner}/{repo}/issues}).
     *
     * @param title the issue title
     * @return the new issue as GitHub returned it
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IOException if the transport fails
     */
    public Issue createIssue(final String title) throws IOException {
        return createIssue(GitHubCreatePayloadBuilder.builder()
                .title(requireNonNull(title, "title"))
                .build());
    }

    /**
     * Creates an issue ({@code POST /repos/{owner}/{repo}/issues}).
     *
     * @param payload the issue to create
     * @return the new issue as GitHub returned it
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Issue createIssue(final GitHubCreatePayload payload) throws IOException {
        return new Issue(this, sdk.github().issues().issue().create(owner, name, requireNonNull(payload, "payload")));
    }

    /**
     * Loads one issue of this repository ({@code GET /repos/{owner}/{repo}/issues/{issue_number}}).
     *
     * @param number the issue number
     * @return the issue, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Issue> issue(final int number) throws IOException {
        return sdk.github().issues().issue().get(owner, name, number).map(record -> new Issue(this, record));
    }

    /**
     * Loads one pull request of this repository ({@code GET /repos/{owner}/{repo}/pulls/{pull_number}}).
     *
     * @param number the pull request number
     * @return the pull request, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<PullRequest> pullRequest(final int number) throws IOException {
        return sdk.github()
                .pullRequests()
                .pullRequest()
                .get(owner, name, number)
                .map(loaded -> new PullRequest(this, loaded));
    }

    /**
     * Lists the open pull requests ({@code GET /repos/{owner}/{repo}/pulls}), the way GitHub lists them by default. See
     * {@link #pullRequests(GitHubListPullRequestsFilter)}.
     *
     * @return the pull requests
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<PullRequest> pullRequests() throws IOException {
        return pullRequests(GitHubListPullRequestsFilter.empty());
    }

    /**
     * Lists pull requests ({@code GET /repos/{owner}/{repo}/pulls}). GitHub's list holds only a summary of each pull
     * request, so every listed pull request is loaded with one more request as the stream reaches it; a {@code limit}
     * on the stream bounds the cost. A transport failure during iteration is an {@link java.io.UncheckedIOException},
     * and a listed pull request that is gone before it is loaded is an {@link IllegalStateException}.
     *
     * @param filter the list filter
     * @return the pull requests
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<PullRequest> pullRequests(final GitHubListPullRequestsFilter filter) throws IOException {
        return sdk
                .github()
                .pullRequests()
                .pullRequest()
                .list(owner, name, requireNonNull(filter, "filter"), GitHubPageRequest.byPage())
                .stream()
                .map(this::load);
    }

    /**
     * Creates a pull request ({@code POST /repos/{owner}/{repo}/pulls}).
     *
     * @param payload the pull request to create
     * @return the new pull request as GitHub returned it
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public PullRequest createPullRequest(final GitHubCreatePullRequestPayload payload) throws IOException {
        return new PullRequest(
                this,
                sdk.github().pullRequests().pullRequest().create(owner, name, requireNonNull(payload, "payload")));
    }

    /**
     * Returns the SDK this repository belongs to.
     *
     * @return the SDK
     */
    GitHubSdk sdk() {
        return sdk;
    }

    private PullRequest load(final GitHubPullRequestSimple listed) {
        try {
            return pullRequest(listed.number())
                    .orElseThrow(() -> new IllegalStateException("Pull request " + listed.number() + " is gone"));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
