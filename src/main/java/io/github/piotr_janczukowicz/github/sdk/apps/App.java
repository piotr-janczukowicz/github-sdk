package io.github.piotr_janczukowicz.github.sdk.apps;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.apps.GitHubIntegration;
import io.github.piotr_janczukowicz.github.api.apps.GitHubIntegrationInstallationRequest;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubInstallation;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubHookDelivery;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubHookDeliveryItem;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubWebhookConfig;
import io.github.piotr_janczukowicz.github.client.exception.GitHubNotFoundException;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A GitHub App. {@link GitHubSdk#app()} returns the app the SDK is authenticated as, and {@link GitHubSdk#app(String)}
 * any public app. The installation and webhook methods work only for the app the SDK is authenticated as, and only with
 * an App JWT, which the client builds from the private key of the app; with any other token GitHub answers HTTP 401.
 * The object is immutable, and {@link #response()} gives the complete response.
 */
public final class App {

    private final GitHubSdk sdk;
    private final GitHubIntegration response;

    public App(final GitHubSdk sdk, final GitHubIntegration response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the app id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the app slug, the name used in URLs.
     *
     * @return the slug
     */
    public String slug() {
        return response.slug();
    }

    /**
     * Returns the app name.
     *
     * @return the name
     */
    public String name() {
        return response.name();
    }

    /**
     * Loads the app again ({@code GET /apps/{app_slug}}). This object is not changed.
     *
     * @return the app as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the app is gone
     */
    public App refresh() throws IOException {
        return sdk.app(slug()).orElseThrow(() -> new IllegalStateException("App " + slug() + " is gone"));
    }

    /**
     * Lists the installations of the app ({@code GET /app/installations}). The stream is lazy and can be used once.
     *
     * @return the installations
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Installation> installations() throws IOException {
        return github().apps().app().listInstallations().stream().map(listed -> new Installation(sdk, listed));
    }

    /**
     * Loads one installation ({@code GET /app/installations/{installation_id}}).
     *
     * @param installationId the installation id
     * @return the installation, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Installation> installation(final long installationId) throws IOException {
        return github().apps().app().getInstallation(installationId).map(loaded -> new Installation(sdk, loaded));
    }

    /**
     * Loads the installation of the app on an organization ({@code GET /orgs/{org}/installation}).
     *
     * @param login the organization login
     * @return the installation, or empty when the app is not installed there
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Installation> organizationInstallation(final String login) throws IOException {
        final String organization = requireNotBlank(login, "login");
        return found(() -> github().apps().app().getOrgInstallation(organization));
    }

    /**
     * Loads the installation of the app on a repository ({@code GET /repos/{owner}/{repo}/installation}).
     *
     * @param repository the repository
     * @return the installation, or empty when the app is not installed there
     * @throws NullPointerException if {@code repository} is {@code null}
     * @throws IOException if the transport fails
     */
    public Optional<Installation> repositoryInstallation(final Repository repository) throws IOException {
        requireNonNull(repository, "repository");
        return found(() -> github().apps().app().getRepoInstallation(repository.owner(), repository.name()));
    }

    /**
     * Loads the installation of the app on a user account ({@code GET /users/{username}/installation}).
     *
     * @param login the user login
     * @return the installation, or empty when the app is not installed there
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Installation> userInstallation(final String login) throws IOException {
        return github().apps()
                .app()
                .getUserInstallation(requireNotBlank(login, "login"))
                .map(loaded -> new Installation(sdk, loaded));
    }

    /**
     * Lists the pending requests to install the app ({@code GET /app/installation-requests}) as GitHub returns them.
     * The stream is lazy and can be used once.
     *
     * @return the requests
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubIntegrationInstallationRequest> installationRequests() throws IOException {
        return github().apps().app().listInstallationRequests().stream();
    }

    /**
     * Loads the webhook configuration of the app ({@code GET /app/hook/config}). The secret is not returned by GitHub.
     *
     * @return the configuration as GitHub returns it
     * @throws IOException if the transport fails
     */
    public GitHubWebhookConfig webhookConfig() throws IOException {
        return github().apps().webhooks().getWebhookConfigForApp();
    }

    /**
     * Lists the webhook deliveries of the app, newest first ({@code GET /app/hook/deliveries}). The stream is lazy and
     * can be used once.
     *
     * @return the deliveries as GitHub returns them
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubHookDeliveryItem> webhookDeliveries() throws IOException {
        return github().apps().webhooks().listWebhookDeliveries().stream();
    }

    /**
     * Loads one webhook delivery with its request and response ({@code GET /app/hook/deliveries/{delivery_id}}).
     *
     * @param deliveryId the delivery id
     * @return the delivery as GitHub returns it, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GitHubHookDelivery> webhookDelivery(final long deliveryId) throws IOException {
        return github().apps().webhooks().getWebhookDelivery(deliveryId);
    }

    /**
     * Asks GitHub to deliver a webhook again ({@code POST /app/hook/deliveries/{delivery_id}/attempts}).
     *
     * @param deliveryId the delivery id
     * @throws IOException if the transport fails
     */
    public void redeliver(final long deliveryId) throws IOException {
        github().apps().webhooks().redeliverWebhookDelivery(deliveryId);
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubIntegration response() {
        return response;
    }

    private Optional<Installation> found(final InstallationLookup lookup) throws IOException {
        try {
            return Optional.of(new Installation(sdk, lookup.find()));
        } catch (final GitHubNotFoundException e) {
            return Optional.empty();
        }
    }

    private GitHub github() {
        return Internals.github(sdk);
    }

    @FunctionalInterface
    private interface InstallationLookup {

        GitHubInstallation find() throws IOException;
    }
}
