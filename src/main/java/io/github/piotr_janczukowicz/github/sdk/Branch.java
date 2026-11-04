package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchWithProtection;

/**
 * A branch of a {@link Repository}, as GitHub returned it. The object is immutable, and {@link #record()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class Branch {

    private final Repository repository;
    private final GitHubBranchWithProtection record;

    Branch(final Repository repository, final GitHubBranchWithProtection record) {
        this.repository = requireNonNull(repository, "repository");
        this.record = requireNonNull(record, "record");
    }

    /**
     * Returns the repository the branch belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the branch name.
     *
     * @return the name
     */
    public String name() {
        return record.name();
    }

    /**
     * Tells whether the branch is protected.
     *
     * @return {@code true} when protected
     */
    public boolean isProtected() {
        return record.protectedBranch();
    }

    /**
     * Returns the commit the branch points to.
     *
     * @return the head commit
     */
    public Commit commit() {
        return new Commit(repository, record.commit());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubBranchWithProtection record() {
        return record;
    }
}
