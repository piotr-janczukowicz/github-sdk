package io.github.piotr_janczukowicz.github.sdk.deployments;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubCreateDeploymentStatusPayload;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeployment;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentStatus;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A deployment of a {@link Repository}, as GitHub returned it. The object is immutable, and {@link #response()} gives
 * the complete response for every field the accessors here do not cover. A deployment has no state of its own; its
 * state is the latest of its statuses.
 */
public final class Deployment {

    private final Repository repository;
    private final GitHubDeployment response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Deployment(final Repository repository, final GitHubDeployment response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the deployment belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the deployment id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the ref that was deployed, a branch, tag or sha.
     *
     * @return the ref
     */
    public String ref() {
        return response.ref();
    }

    /**
     * Returns the sha that was deployed.
     *
     * @return the sha
     */
    public String sha() {
        return response.sha();
    }

    /**
     * Returns the name of the environment the deployment targets.
     *
     * @return the environment name
     */
    public String environment() {
        return response.environment();
    }

    /**
     * Returns the description.
     *
     * @return the description, or empty when the deployment has none
     */
    public Optional<String> description() {
        return Optional.ofNullable(response.description());
    }

    /**
     * Returns the login of the user or app that created the deployment.
     *
     * @return the login, or empty when GitHub reports no creator
     */
    public Optional<String> creator() {
        return Optional.ofNullable(response.creator()).map(GitHubSimpleUser::login);
    }

    /**
     * Tells whether the deployment targets a production environment.
     *
     * @return {@code true} when GitHub marks the environment as production
     */
    public boolean isProduction() {
        return Boolean.TRUE.equals(response.productionEnvironment());
    }

    /**
     * Lists the statuses of the deployment ({@code GET /repos/{owner}/{repo}/deployments/{deployment_id}/statuses}) as
     * GitHub returns them. Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the statuses, newest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubDeploymentStatus> statuses() throws IOException {
        return github()
                .deployments()
                .statuses()
                .listDeploymentStatuses(repository.owner(), repository.name(), id(), GitHubPageRequest.byPage())
                .stream();
    }

    /**
     * Loads the newest status, which is the current state of the deployment.
     *
     * @return the newest status, or empty when the deployment has none yet
     * @throws IOException if the transport fails
     */
    public Optional<GitHubDeploymentStatus> latestStatus() throws IOException {
        try (Stream<GitHubDeploymentStatus> statuses = statuses()) {
            return statuses.findFirst();
        }
    }

    /**
     * Reports a new state of the deployment. See {@link #createStatus(GitHubCreateDeploymentStatusPayload)}.
     *
     * @param state the new state
     * @return the new status
     * @throws NullPointerException if {@code state} is {@code null}
     * @throws IOException if the transport fails
     */
    public GitHubDeploymentStatus createStatus(final DeploymentState state) throws IOException {
        return createStatus(new GitHubCreateDeploymentStatusPayload(
                requireNonNull(state, "state").toClient(), null, null, null, null, null, null));
    }

    /**
     * Reports a new state of the deployment with a description. See
     * {@link #createStatus(GitHubCreateDeploymentStatusPayload)}.
     *
     * @param state the new state
     * @param description a short description of the state
     * @return the new status
     * @throws NullPointerException if {@code state} or {@code description} is {@code null}
     * @throws IOException if the transport fails
     */
    public GitHubDeploymentStatus createStatus(final DeploymentState state, final String description)
            throws IOException {
        return createStatus(new GitHubCreateDeploymentStatusPayload(
                requireNonNull(state, "state").toClient(),
                null,
                null,
                requireNonNull(description, "description"),
                null,
                null,
                null));
    }

    /**
     * Reports a new state of the deployment with every option the client offers ({@code POST
     * /repos/{owner}/{repo}/deployments/{deployment_id}/statuses}).
     *
     * @param payload the state and its options
     * @return the new status
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public GitHubDeploymentStatus createStatus(final GitHubCreateDeploymentStatusPayload payload) throws IOException {
        return github().deployments()
                .statuses()
                .createDeploymentStatus(
                        repository.owner(), repository.name(), id(), requireNonNull(payload, "payload"));
    }

    /**
     * Loads the deployment again ({@code GET /repos/{owner}/{repo}/deployments/{deployment_id}}). This object is not
     * changed.
     *
     * @return the deployment as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the deployment is gone
     */
    public Deployment refresh() throws IOException {
        return repository
                .deployment(id())
                .orElseThrow(() -> new IllegalStateException("Deployment " + id() + " is gone"));
    }

    /**
     * Deletes the deployment ({@code DELETE /repos/{owner}/{repo}/deployments/{deployment_id}}). GitHub deletes only an
     * inactive deployment, so report {@link DeploymentState#INACTIVE} first; a deployment that is still active makes
     * GitHub answer HTTP 422.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        github().deployments().deployment().deleteDeployment(repository.owner(), repository.name(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubDeployment response() {
        return response;
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
