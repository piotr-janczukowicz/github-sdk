package io.github.piotr_janczukowicz.github.sdk.apps;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.apps.GitHubCreateInstallationAccessTokenPayload;
import io.github.piotr_janczukowicz.github.api.apps.GitHubInstallationToken;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubInstallation;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;

/**
 * An installation of an {@link App} on an organization or user account. All methods need the App JWT of the app that
 * owns the installation. The object is immutable, and {@link #response()} gives the complete response.
 */
public final class Installation {

    private final GitHubSdk sdk;
    private final GitHubInstallation response;

    public Installation(final GitHubSdk sdk, final GitHubInstallation response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the installation id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the slug of the app the installation belongs to.
     *
     * @return the app slug
     */
    public String appSlug() {
        return response.appSlug();
    }

    /**
     * Returns the id of the account the app is installed on.
     *
     * @return the target id
     */
    public long targetId() {
        return response.targetId();
    }

    /**
     * Returns the kind of account the app is installed on, {@code Organization} or {@code User}.
     *
     * @return the target type
     */
    public String targetType() {
        return response.targetType();
    }

    /**
     * Tells whether the installation is suspended.
     *
     * @return {@code true} when suspended
     */
    public boolean isSuspended() {
        return response.suspendedAt() != null;
    }

    /**
     * Loads the installation again ({@code GET /app/installations/{installation_id}}). This object is not changed.
     *
     * @return the installation as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the installation is gone
     */
    public Installation refresh() throws IOException {
        return github().apps()
                .app()
                .getInstallation(id())
                .map(loaded -> new Installation(sdk, loaded))
                .orElseThrow(() -> new IllegalStateException("Installation " + id() + " is gone"));
    }

    /**
     * Suspends the installation ({@code PUT /app/installations/{installation_id}/suspended}), which blocks the app from
     * acting on the account. This object is not changed.
     *
     * @return the installation as GitHub holds it afterwards
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the installation is gone
     */
    public Installation suspend() throws IOException {
        github().apps().app().suspendInstallation(id());
        return refresh();
    }

    /**
     * Lifts the suspension ({@code DELETE /app/installations/{installation_id}/suspended}). This object is not changed.
     *
     * @return the installation as GitHub holds it afterwards
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the installation is gone
     */
    public Installation unsuspend() throws IOException {
        github().apps().app().unsuspendInstallation(id());
        return refresh();
    }

    /**
     * Uninstalls the app from the account ({@code DELETE /app/installations/{installation_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        github().apps().app().deleteInstallation(id());
    }

    /**
     * Creates an installation access token with all the permissions and repositories of the installation ({@code POST
     * /app/installations/{installation_id}/access_tokens}). The token expires after one hour; it is a secret and should
     * not be logged.
     *
     * @return the token as GitHub returns it
     * @throws IOException if the transport fails
     */
    public GitHubInstallationToken accessToken() throws IOException {
        return github().apps().app().createInstallationAccessToken(id());
    }

    /**
     * Creates an installation access token limited to some repositories of the installation.
     *
     * @param repositoryNames the names of the repositories, without the owner
     * @return the token as GitHub returns it
     * @throws NullPointerException if {@code repositoryNames} is {@code null}
     * @throws IOException if the transport fails
     */
    public GitHubInstallationToken accessToken(final List<String> repositoryNames) throws IOException {
        return github().apps()
                .app()
                .createInstallationAccessToken(
                        id(),
                        new GitHubCreateInstallationAccessTokenPayload(
                                List.copyOf(requireNonNull(repositoryNames, "repositoryNames")), null, null));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubInstallation response() {
        return response;
    }

    private GitHub github() {
        return Internals.github(sdk);
    }
}
