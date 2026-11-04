package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.users.GitHubAuthenticatedUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubUserView;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A GitHub user, as GitHub returned it. The object is immutable, and {@link #record()} gives the complete response for
 * every field the accessors here do not cover.
 */
@SuppressWarnings("PMD.ShortClassName")
public final class User {

    private final GitHubSdk sdk;
    private final GitHubUserView record;

    User(final GitHubSdk sdk, final GitHubUserView record) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.record = requireNonNull(record, "record");
    }

    /**
     * Returns the login name.
     *
     * @return the login
     */
    public String login() {
        return switch (record) {
            case final GitHubUser user -> user.login();
            case final GitHubAuthenticatedUser user -> user.login();
        };
    }

    /**
     * Returns the display name.
     *
     * @return the name, or empty when the user has none
     */
    public Optional<String> name() {
        return Optional.ofNullable(
                switch (record) {
                    case final GitHubUser user -> user.name();
                    case final GitHubAuthenticatedUser user -> user.name();
                });
    }

    /**
     * Lists the public repositories of the user ({@code GET /users/{username}/repos}). The repositories are handles
     * built from GitHub's list and hold no data; call {@link Repository#fetch()} to load one. Pages are fetched only as
     * the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the repository handles
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Repository> repositories() throws IOException {
        return sdk.github().repositories().repository().listForUser(login()).stream()
                .map(listed -> sdk.repository(listed.owner().login(), listed.name()));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record, a plain user or the authenticated user
     */
    public GitHubUserView record() {
        return record;
    }
}
