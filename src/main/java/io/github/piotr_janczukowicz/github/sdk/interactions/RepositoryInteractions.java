package io.github.piotr_janczukowicz.github.sdk.interactions;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.interactions.GitHubInteractionLimit;
import io.github.piotr_janczukowicz.github.api.interactions.GitHubInteractionLimitResponse;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * The temporary interaction limit of a {@link Repository}, which restricts who may comment, open issues and open pull
 * requests for a while, for example during a flood of spam. Nothing is requested until a method is called. A limit set
 * on the owner account or organization applies to the repository too and cannot be changed here.
 */
public final class RepositoryInteractions {

    private final Repository repository;

    /**
     * Creates the handle. Applications normally get it from the object it belongs to. Nothing is requested until a
     * method is called.
     *
     * @param repository the repository the object belongs to
     * @throws NullPointerException if an argument is {@code null}
     */
    public RepositoryInteractions(final Repository repository) {
        this.repository = requireNonNull(repository, "repository");
    }

    /**
     * Loads the limit that is active on the repository ({@code GET /repos/{owner}/{repo}/interaction-limits}).
     *
     * @return the limit as GitHub returns it, or empty when none is active
     * @throws IOException if the transport fails
     */
    public Optional<GitHubInteractionLimitResponse> current() throws IOException {
        return github().interactions().repos().getRestrictionsForRepo(repository.owner(), repository.name());
    }

    /**
     * Limits interactions to a group for one day ({@code PUT /repos/{owner}/{repo}/interaction-limits}). A limit that
     * is already active is replaced.
     *
     * @param group who may still interact
     * @return the limit as GitHub returns it
     * @throws NullPointerException if {@code group} is {@code null}
     * @throws IOException if the transport fails
     */
    public GitHubInteractionLimitResponse restrict(final InteractionGroup group) throws IOException {
        return restrict(group, InteractionExpiry.ONE_DAY);
    }

    /**
     * Limits interactions to a group for a chosen time. A limit that is already active is replaced.
     *
     * @param group who may still interact
     * @param expiry how long the limit lasts
     * @return the limit as GitHub returns it
     * @throws NullPointerException if {@code group} or {@code expiry} is {@code null}
     * @throws IOException if the transport fails
     */
    public GitHubInteractionLimitResponse restrict(final InteractionGroup group, final InteractionExpiry expiry)
            throws IOException {
        final GitHubInteractionLimit limit = new GitHubInteractionLimit(
                requireNonNull(group, "group").toClient(),
                requireNonNull(expiry, "expiry").toClient());
        return github().interactions().repos().setRestrictionsForRepo(repository.owner(), repository.name(), limit);
    }

    /**
     * Removes the limit ({@code DELETE /repos/{owner}/{repo}/interaction-limits}). Nothing happens when none is active.
     *
     * @throws IOException if the transport fails
     */
    public void remove() throws IOException {
        github().interactions().repos().removeRestrictionsForRepo(repository.owner(), repository.name());
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
