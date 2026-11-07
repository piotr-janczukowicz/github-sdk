package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryVisibility;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.github.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to a {@link Repository} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. Archiving is {@link Repository#archive()}, because GitHub cannot undo it.
 */
public final class RepositoryUpdate {

    private final Repository repository;
    private final GitHubUpdatePayloadBuilder payload = GitHubUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    RepositoryUpdate(final Repository repository) {
        this.repository = requireNonNull(repository, "repository");
    }

    /**
     * Renames the repository. The repository returned by {@link #send()} has the new name.
     *
     * @param name the new name
     * @return this update
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public RepositoryUpdate name(final String name) {
        payload.name(requireNotBlank(name, "name"));
        return changed();
    }

    /**
     * Sets the description.
     *
     * @param description the new description
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public RepositoryUpdate description(final String description) {
        payload.description(requireNonNull(description, "description"));
        return changed();
    }

    /**
     * Sets the homepage URL.
     *
     * @param homepage the new homepage
     * @return this update
     * @throws NullPointerException if {@code homepage} is {@code null}
     */
    public RepositoryUpdate homepage(final String homepage) {
        payload.homepage(requireNonNull(homepage, "homepage"));
        return changed();
    }

    /**
     * Sets who can see the repository.
     *
     * @param visibility the new visibility
     * @return this update
     * @throws NullPointerException if {@code visibility} is {@code null}
     */
    public RepositoryUpdate visibility(final GitHubRepositoryVisibility visibility) {
        payload.visibility(requireNonNull(visibility, "visibility"));
        return changed();
    }

    /**
     * Sets the default branch, which must exist.
     *
     * @param defaultBranch the branch name
     * @return this update
     * @throws NullPointerException if {@code defaultBranch} is {@code null}
     * @throws IllegalArgumentException if {@code defaultBranch} is blank
     */
    public RepositoryUpdate defaultBranch(final String defaultBranch) {
        payload.defaultBranch(requireNotBlank(defaultBranch, "defaultBranch"));
        return changed();
    }

    /**
     * Sets whether the repository has issues.
     *
     * @param isIssues {@code true} to enable issues
     * @return this update
     */
    public RepositoryUpdate issues(final boolean isIssues) {
        payload.hasIssues(isIssues);
        return changed();
    }

    /**
     * Sets whether the repository has projects.
     *
     * @param isProjects {@code true} to enable projects
     * @return this update
     */
    public RepositoryUpdate projects(final boolean isProjects) {
        payload.hasProjects(isProjects);
        return changed();
    }

    /**
     * Sets whether the repository has a wiki.
     *
     * @param isWiki {@code true} to enable the wiki
     * @return this update
     */
    public RepositoryUpdate wiki(final boolean isWiki) {
        payload.hasWiki(isWiki);
        return changed();
    }

    /**
     * Sets whether the repository is a template for new repositories.
     *
     * @param isTemplate {@code true} to make it a template
     * @return this update
     */
    public RepositoryUpdate template(final boolean isTemplate) {
        payload.isTemplate(isTemplate);
        return changed();
    }

    /**
     * Sets whether pull requests may be squash merged.
     *
     * @param isAllowed {@code true} to allow it
     * @return this update
     */
    public RepositoryUpdate allowSquashMerge(final boolean isAllowed) {
        payload.allowSquashMerge(isAllowed);
        return changed();
    }

    /**
     * Sets whether pull requests may be merged with a merge commit.
     *
     * @param isAllowed {@code true} to allow it
     * @return this update
     */
    public RepositoryUpdate allowMergeCommit(final boolean isAllowed) {
        payload.allowMergeCommit(isAllowed);
        return changed();
    }

    /**
     * Sets whether pull requests may be rebase merged.
     *
     * @param isAllowed {@code true} to allow it
     * @return this update
     */
    public RepositoryUpdate allowRebaseMerge(final boolean isAllowed) {
        payload.allowRebaseMerge(isAllowed);
        return changed();
    }

    /**
     * Sets whether pull requests may be set to merge automatically once their requirements are met.
     *
     * @param isAllowed {@code true} to allow it
     * @return this update
     */
    public RepositoryUpdate allowAutoMerge(final boolean isAllowed) {
        payload.allowAutoMerge(isAllowed);
        return changed();
    }

    /**
     * Sets whether the head branch of a pull request is deleted when it is merged.
     *
     * @param isDeleted {@code true} to delete it
     * @return this update
     */
    public RepositoryUpdate deleteBranchOnMerge(final boolean isDeleted) {
        payload.deleteBranchOnMerge(isDeleted);
        return changed();
    }

    /**
     * Sets whether the head branch of a pull request may be updated with the base branch from the pull request page.
     *
     * @param isAllowed {@code true} to allow it
     * @return this update
     */
    public RepositoryUpdate allowUpdateBranch(final boolean isAllowed) {
        payload.allowUpdateBranch(isAllowed);
        return changed();
    }

    /**
     * Sets whether the repository may be forked, which matters for private repositories.
     *
     * @param isAllowed {@code true} to allow forks
     * @return this update
     */
    public RepositoryUpdate allowForking(final boolean isAllowed) {
        payload.allowForking(isAllowed);
        return changed();
    }

    /**
     * Sets whether contributors must sign off commits made in the web interface.
     *
     * @param isRequired {@code true} to require it
     * @return this update
     */
    public RepositoryUpdate webCommitSignoffRequired(final boolean isRequired) {
        payload.webCommitSignoffRequired(isRequired);
        return changed();
    }

    /**
     * Sends the changes ({@code PATCH /repos/{owner}/{repo}}). The repository this update was made from is not changed.
     *
     * @return the repository as GitHub returned it after the update, under its new name after a rename
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public Repository send() throws IOException {
        state.requireChange();
        return repository.apply(payload.build());
    }

    private RepositoryUpdate changed() {
        state.mark();
        return this;
    }
}
