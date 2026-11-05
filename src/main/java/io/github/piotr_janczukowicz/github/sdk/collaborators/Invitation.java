package io.github.piotr_janczukowicz.github.sdk.collaborators;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubRepositoryInvitation;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubRepositoryInvitationPermission;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubUpdateRepositoryInvitationPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A pending invitation to collaborate on a {@link Repository}, as GitHub returned it. The object is immutable, and
 * {@link #response()} gives the complete response for every field the accessors here do not cover. There is no
 * {@code refresh()}, because GitHub has no endpoint that loads one invitation.
 */
public final class Invitation {

    private final Repository repository;
    private final GitHubRepositoryInvitation response;

    public Invitation(final Repository repository, final GitHubRepositoryInvitation response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the invitation is for.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the invitation id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the invited user.
     *
     * @return the invitee, or empty when GitHub does not return one
     */
    public Optional<GitHubSimpleUser> invitee() {
        return Optional.ofNullable(response.invitee());
    }

    /**
     * Returns the user who sent the invitation.
     *
     * @return the inviter, or empty when GitHub does not return one
     */
    public Optional<GitHubSimpleUser> inviter() {
        return Optional.ofNullable(response.inviter());
    }

    /**
     * Returns the access the invitation grants, as the client decoded it. The client maps a value it does not know to
     * its own {@code UNRECOGNIZED} constant.
     *
     * @return the permission
     */
    public GitHubRepositoryInvitationPermission permission() {
        return response.permissions();
    }

    /**
     * Tells whether the invitation has expired.
     *
     * @return {@code true} when GitHub reports it as expired
     */
    public boolean isExpired() {
        return Boolean.TRUE.equals(response.expired());
    }

    /**
     * Changes the access the invitation grants ({@code PATCH /repos/{owner}/{repo}/invitations/{invitation_id}}). This
     * object is not changed.
     *
     * @param permission the new access level
     * @return the invitation as GitHub returned it after the change
     * @throws NullPointerException if {@code permission} is {@code null}
     * @throws IOException if the transport fails
     */
    public Invitation changePermission(final RepositoryPermission permission) throws IOException {
        final GitHubUpdateRepositoryInvitationPayload payload = new GitHubUpdateRepositoryInvitationPayload(
                requireNonNull(permission, "permission").toInvitationUpdate());
        final GitHub github = Internals.github(repository);
        return new Invitation(
                repository,
                github.collaborators()
                        .invitations()
                        .updateInvitation(repository.owner(), repository.name(), id(), payload));
    }

    /**
     * Withdraws the invitation ({@code DELETE /repos/{owner}/{repo}/invitations/{invitation_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void cancel() throws IOException {
        final GitHub github = Internals.github(repository);
        github.collaborators().invitations().deleteInvitation(repository.owner(), repository.name(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubRepositoryInvitation response() {
        return response;
    }
}
