package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.search.GitHubCodeSearchResultItem;
import io.github.piotr_janczukowicz.github.api.search.GitHubCommitSearchResultItem;
import io.github.piotr_janczukowicz.github.api.search.GitHubIssueSearchResultItem;
import io.github.piotr_janczukowicz.github.api.search.GitHubUserSearchResultItem;
import java.io.IOException;
import java.util.stream.Stream;

/**
 * Searches GitHub ({@code GET /search/...}). GitHub caps a search at 1,000 results and applies a separate, lower rate
 * limit to it. Pages are fetched only as a stream is consumed, so {@code limit} stops further requests; transport
 * failures during iteration are {@link java.io.UncheckedIOException}.
 */
public final class Search {

    private static final String QUERY = "query";

    private final GitHubSdk sdk;

    Search(final GitHubSdk sdk) {
        this.sdk = requireNonNull(sdk, "sdk");
    }

    /**
     * Searches issues and pull requests. GitHub's hits hold only a summary, so every hit is loaded with one more
     * request as the stream reaches it; a {@code limit} on the stream bounds the cost. A hit that is gone before it is
     * loaded is an {@link IllegalStateException}.
     *
     * @param query the search query, in GitHub's search syntax
     * @return the issues
     * @throws IllegalArgumentException if {@code query} is blank
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Issue> issues(final String query) throws IOException {
        return sdk.github().search().searchIssues(requireNotBlank(query, QUERY)).stream()
                .map(this::loadIssue);
    }

    /**
     * Searches repositories. The repositories are handles built from the hits and hold no data; call
     * {@link Repository#fetch()} to load one.
     *
     * @param query the search query, in GitHub's search syntax
     * @return the repository handles
     * @throws IllegalArgumentException if {@code query} is blank
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Repository> repositories(final String query) throws IOException {
        return sdk.github().search().searchRepos(requireNotBlank(query, QUERY)).stream()
                .map(hit -> {
                    final String fullName = hit.fullName();
                    final int slash = fullName.indexOf('/');
                    if (slash <= 0) {
                        throw new IllegalArgumentException("Unexpected repository name in a search hit: " + fullName);
                    }
                    return sdk.repository(fullName.substring(0, slash), fullName.substring(slash + 1));
                });
    }

    /**
     * Searches users and organizations. GitHub's hits hold only a summary, so every hit is loaded with one more request
     * as the stream reaches it; a {@code limit} on the stream bounds the cost. A hit that is gone before it is loaded
     * is an {@link IllegalStateException}.
     *
     * @param query the search query, in GitHub's search syntax
     * @return the users
     * @throws IllegalArgumentException if {@code query} is blank
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<User> users(final String query) throws IOException {
        return sdk.github().search().searchUsers(requireNotBlank(query, QUERY)).stream()
                .map(this::loadUser);
    }

    /**
     * Searches code and returns GitHub's hits as they are.
     *
     * @param query the search query, in GitHub's search syntax
     * @return the hits
     * @throws IllegalArgumentException if {@code query} is blank
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubCodeSearchResultItem> code(final String query) throws IOException {
        return sdk.github().search().searchCode(requireNotBlank(query, QUERY)).stream();
    }

    /**
     * Searches commits and returns GitHub's hits as they are.
     *
     * @param query the search query, in GitHub's search syntax
     * @return the hits
     * @throws IllegalArgumentException if {@code query} is blank
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubCommitSearchResultItem> commits(final String query) throws IOException {
        return sdk.github().search().searchCommits(requireNotBlank(query, QUERY)).stream();
    }

    private Issue loadIssue(final GitHubIssueSearchResultItem hit) {
        return Lookups.require(
                () -> repositoryOf(hit.repositoryUrl()).issue(hit.number()),
                () -> "Issue " + hit.number() + " is gone");
    }

    private Repository repositoryOf(final String url) {
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        final int nameStart = url.lastIndexOf('/', end - 1) + 1;
        if (nameStart == 0) {
            throw new IllegalArgumentException("Unexpected repository URL in a search hit: " + url);
        }
        final int ownerStart = url.lastIndexOf('/', nameStart - 2) + 1;
        return sdk.repository(url.substring(ownerStart, nameStart - 1), url.substring(nameStart, end));
    }

    private User loadUser(final GitHubUserSearchResultItem hit) {
        return Lookups.require(() -> sdk.user(hit.login()), () -> "User " + hit.login() + " is gone");
    }
}
