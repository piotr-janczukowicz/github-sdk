package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.collaborators.GitHubCollaborator;
import java.io.IOException;
import java.util.Optional;

/**
 * A user with access to a {@link Repository}, as GitHub returned it. The object is immutable, and {@link #response()}
 * gives the complete response for every field the accessors here do not cover.
 */
public final class Collaborator {

    private final Repository repository;
    private final GitHubCollaborator response;

    Collaborator(final Repository repository, final GitHubCollaborator response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the user has access to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the user's login.
     *
     * @return the login
     */
    public String login() {
        return response.login();
    }

    /**
     * Returns the name of the user's role on the repository, for example {@code admin} or a custom role.
     *
     * @return the role name, or empty when GitHub does not return one
     */
    public Optional<String> roleName() {
        return Optional.ofNullable(response.roleName());
    }

    /**
     * Tells whether the user is a GitHub site administrator.
     *
     * @return {@code true} for a site administrator
     */
    public boolean isSiteAdmin() {
        return response.siteAdmin();
    }

    /**
     * Removes the user's access to the repository ({@code DELETE /repos/{owner}/{repo}/collaborators/{username}}).
     *
     * @throws IOException if the transport fails
     */
    public void remove() throws IOException {
        repository
                .sdk()
                .github()
                .collaborators()
                .collaborator()
                .removeCollaborator(repository.owner(), repository.name(), login());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubCollaborator response() {
        return response;
    }
}
