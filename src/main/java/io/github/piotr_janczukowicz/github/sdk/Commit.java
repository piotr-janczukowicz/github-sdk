package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommit;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.util.Optional;

/**
 * A commit of a {@link Repository}, as GitHub returned it. The object is immutable, and {@link #record()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class Commit {

    private final Repository repository;
    private final GitHubCommit record;

    Commit(final Repository repository, final GitHubCommit record) {
        this.repository = requireNonNull(repository, "repository");
        this.record = requireNonNull(record, "record");
    }

    /**
     * Returns the repository the commit belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the commit hash.
     *
     * @return the SHA
     */
    public String sha() {
        return record.sha();
    }

    /**
     * Returns the commit message.
     *
     * @return the message
     */
    public String message() {
        return record.commit().message();
    }

    /**
     * Returns the GitHub user who wrote the commit.
     *
     * @return the author, or empty when the commit is not linked to a GitHub user
     */
    public Optional<GitHubSimpleUser> author() {
        return Optional.ofNullable(record.author());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubCommit record() {
        return record;
    }
}
