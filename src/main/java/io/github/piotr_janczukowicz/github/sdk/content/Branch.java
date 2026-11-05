package io.github.piotr_janczukowicz.github.sdk.content;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchWithProtection;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleDetailed;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A branch of a {@link Repository}, as GitHub returned it. The object is immutable, and {@link #response()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class Branch {

    private final Repository repository;
    private final GitHubBranchWithProtection response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Branch(final Repository repository, final GitHubBranchWithProtection response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
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
        return response.name();
    }

    /**
     * Tells whether the branch is protected.
     *
     * @return {@code true} when protected
     */
    public boolean isProtected() {
        return response.protectedBranch();
    }

    /**
     * Returns the commit the branch points to.
     *
     * @return the head commit
     */
    public Commit commit() {
        return new Commit(repository, response.commit());
    }

    /**
     * Loads the protection rule of the branch ({@code GET /repos/{owner}/{repo}/branches/{branch}/protection}).
     *
     * @return the rule, or empty when the branch has none or GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<BranchProtection> protection() throws IOException {
        final GitHub github = Internals.github(repository);
        return github.branches()
                .branchProtection()
                .getBranchProtection(repository.owner(), repository.name(), name())
                .map(loaded -> new BranchProtection(this, loaded));
    }

    /**
     * Starts a protection rule for the branch; nothing is sent until {@link BranchProtectionUpdate#send()}. GitHub
     * replaces the whole rule, so a setting that is not chosen is switched off.
     *
     * @return a new update for this branch
     */
    public BranchProtectionUpdate protect() {
        return new BranchProtectionUpdate(this);
    }

    /**
     * Lists the rules of rulesets that apply to the branch ({@code GET /repos/{owner}/{repo}/rules/branches/{branch}}).
     * Branch protection is not part of it. The stream is lazy and can be used once.
     *
     * @return the rules, each with the ruleset it comes from
     * @throws IOException if the transport fails
     */
    public Stream<GitHubRepositoryRuleDetailed> rules() throws IOException {
        final GitHub github = Internals.github(repository);
        return github.repositories().rules().getBranchRules(repository.owner(), repository.name(), name()).stream();
    }

    /**
     * Loads the branch again ({@code GET /repos/{owner}/{repo}/branches/{branch}}). This object is not changed.
     *
     * @return the branch as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the branch is gone
     */
    public Branch refresh() throws IOException {
        return repository.branch(name()).orElseThrow(() -> new IllegalStateException("Branch " + name() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubBranchWithProtection response() {
        return response;
    }
}
