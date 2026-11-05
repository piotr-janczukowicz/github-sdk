package io.github.piotr_janczukowicz.github.sdk.actions;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.actions.GitHubAuthenticationToken;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunnerApplication;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The self-hosted runners of one repository or organization. {@code Repository.selfHostedRunners()} and
 * {@code Organization.selfHostedRunners()} are the usual way in; this type exists so both owner kinds share the same
 * runner objects. Reading needs the Administration permission for a repository, or the organization self-hosted runners
 * permission. Runner groups, just-in-time configurations and enterprise runners are not covered.
 */
public final class SelfHostedRunners {

    private final RunnerGateway gateway;

    private SelfHostedRunners(final RunnerGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * Returns the runners of a repository; no request is made.
     *
     * @param repository the repository
     * @return the runners handle
     * @throws NullPointerException if {@code repository} is {@code null}
     */
    public static SelfHostedRunners forRepository(final Repository repository) {
        requireNonNull(repository, "repository");
        return new SelfHostedRunners(
                new RepositoryRunnerGateway(Internals.github(repository), repository.owner(), repository.name()));
    }

    /**
     * Returns the runners of an organization; no request is made.
     *
     * @param sdk the SDK to call GitHub with
     * @param login the organization login
     * @return the runners handle
     * @throws NullPointerException if {@code sdk} or {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     */
    public static SelfHostedRunners forOrganization(final GitHubSdk sdk, final String login) {
        return new SelfHostedRunners(new OrganizationRunnerGateway(
                Internals.github(requireNonNull(sdk, "sdk")), requireNotBlank(login, "login")));
    }

    /**
     * Lists the runners ({@code GET /repos/{owner}/{repo}/actions/runners} or {@code GET /orgs/{org}/actions/runners}).
     * The stream is lazy and can be used once.
     *
     * @return the runners
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<SelfHostedRunner> all() throws IOException {
        return gateway.runners().map(listed -> new SelfHostedRunner(this, listed));
    }

    /**
     * Lists the runners with an exact name.
     *
     * @param name the runner name
     * @return the matching runners
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<SelfHostedRunner> named(final String name) throws IOException {
        return gateway.runners(requireNotBlank(name, "name")).map(listed -> new SelfHostedRunner(this, listed));
    }

    /**
     * Loads one runner ({@code GET /repos/{owner}/{repo}/actions/runners/{runner_id}} or the organization variant).
     *
     * @param id the runner id
     * @return the runner, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<SelfHostedRunner> find(final long id) throws IOException {
        return gateway.runner(id).map(loaded -> new SelfHostedRunner(this, loaded));
    }

    /**
     * Lists the runner software downloads for each platform ({@code GET .../actions/runners/downloads}) as GitHub
     * returns them.
     *
     * @return the downloads
     * @throws IOException if the transport fails
     */
    public List<GitHubRunnerApplication> applications() throws IOException {
        return gateway.applications();
    }

    /**
     * Creates a token that registers a new runner ({@code POST .../actions/runners/registration-token}). The token
     * expires after one hour and is a secret, so do not log it.
     *
     * @return the token as GitHub returns it
     * @throws IOException if the transport fails
     */
    public GitHubAuthenticationToken registrationToken() throws IOException {
        return gateway.registrationToken();
    }

    /**
     * Creates a token that removes a runner ({@code POST .../actions/runners/remove-token}). The token expires after
     * one hour and is a secret, so do not log it.
     *
     * @return the token as GitHub returns it
     * @throws IOException if the transport fails
     */
    public GitHubAuthenticationToken removeToken() throws IOException {
        return gateway.removeToken();
    }

    RunnerGateway gateway() {
        return gateway;
    }
}
