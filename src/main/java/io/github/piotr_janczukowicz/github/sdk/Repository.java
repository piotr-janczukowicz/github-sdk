package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.issues.GitHubCreatePayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubCreatePayloadBuilder;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubFullRepository;
import java.io.IOException;
import java.util.Optional;
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
     * Returns the SDK this repository belongs to.
     *
     * @return the SDK
     */
    GitHubSdk sdk() {
        return sdk;
    }
}
