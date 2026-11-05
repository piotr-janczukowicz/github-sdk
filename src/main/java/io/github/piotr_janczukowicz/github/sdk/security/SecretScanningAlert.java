package io.github.piotr_janczukowicz.github.sdk.security;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningAlertResolution;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningAlertState;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningAlertWithMetadata;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningLocation;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubUpdateSecretScanningAlertPayload;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import io.github.piotr_janczukowicz.json.OptionalNull;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A secret scanning alert of a {@link Repository}, as GitHub returned it. The object is immutable. None of the
 * accessors returns the detected secret; it is only in {@link #response()}, so a secret is never exposed by accident,
 * for example in a log line.
 */
public final class SecretScanningAlert {

    private final Repository repository;
    private final GitHubSecretScanningAlertWithMetadata response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public SecretScanningAlert(final Repository repository, final GitHubSecretScanningAlertWithMetadata response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the alert belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the alert number.
     *
     * @return the number
     */
    public int number() {
        return response.number();
    }

    /**
     * Returns the state, as the client decoded it.
     *
     * @return the state
     */
    public GitHubSecretScanningAlertState state() {
        return response.state();
    }

    /**
     * Tells whether the alert is open.
     *
     * @return {@code true} when the state is {@code OPEN}
     */
    public boolean isOpen() {
        return response.state() == GitHubSecretScanningAlertState.OPEN;
    }

    /**
     * Returns how a resolved alert was resolved, as the client decoded it.
     *
     * @return the resolution, or empty while the alert is open
     */
    public Optional<GitHubSecretScanningAlertResolution> resolution() {
        return Optional.ofNullable(response.resolution());
    }

    /**
     * Returns the type of the detected secret, for example {@code github_personal_access_token}.
     *
     * @return the secret type
     */
    public String secretType() {
        return response.secretType();
    }

    /**
     * Tells whether the secret is known to have leaked publicly.
     *
     * @return {@code true} when GitHub reports a public leak
     */
    public boolean isPubliclyLeaked() {
        return Boolean.TRUE.equals(response.publiclyLeaked());
    }

    /**
     * Returns the link to the alert on GitHub.
     *
     * @return the URL
     */
    public String htmlUrl() {
        return response.htmlUrl();
    }

    /**
     * Loads the alert again ({@code GET /repos/{owner}/{repo}/secret-scanning/alerts/{alert_number}}). This object is
     * not changed.
     *
     * @return the alert as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the alert is gone
     */
    public SecretScanningAlert refresh() throws IOException {
        return repository
                .secretScanningAlert(number())
                .orElseThrow(() -> new IllegalStateException("Secret scanning alert " + number() + " is gone"));
    }

    /**
     * Lists the places the secret was found ({@code GET
     * /repos/{owner}/{repo}/secret-scanning/alerts/{alert_number}/locations}) as GitHub returns them. Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the locations
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubSecretScanningLocation> locations() throws IOException {
        return github()
                .secretScanning()
                .alerts()
                .listAlertLocations(repository.owner(), repository.name(), number(), GitHubPageRequest.byPage())
                .stream();
    }

    /**
     * Resolves the alert without a comment. See {@link #resolve(SecretResolution, String)}.
     *
     * @param resolution how the alert is resolved
     * @return the alert as GitHub holds it afterwards
     * @throws NullPointerException if {@code resolution} is {@code null}
     * @throws IOException if the transport fails
     */
    public SecretScanningAlert resolve(final SecretResolution resolution) throws IOException {
        return update(
                GitHubSecretScanningAlertState.RESOLVED,
                requireNonNull(resolution, "resolution").toClient(),
                null);
    }

    /**
     * Resolves the alert ({@code PATCH /repos/{owner}/{repo}/secret-scanning/alerts/{alert_number}}). This object is
     * not changed.
     *
     * @param resolution how the alert is resolved
     * @param comment a comment that explains the resolution
     * @return the alert as GitHub holds it afterwards
     * @throws NullPointerException if {@code resolution} or {@code comment} is {@code null}
     * @throws IOException if the transport fails
     */
    public SecretScanningAlert resolve(final SecretResolution resolution, final String comment) throws IOException {
        return update(
                GitHubSecretScanningAlertState.RESOLVED,
                requireNonNull(resolution, "resolution").toClient(),
                requireNonNull(comment, "comment"));
    }

    /**
     * Opens a resolved alert again ({@code PATCH /repos/{owner}/{repo}/secret-scanning/alerts/{alert_number}}). This
     * object is not changed.
     *
     * @return the alert as GitHub holds it afterwards
     * @throws IOException if the transport fails
     */
    public SecretScanningAlert reopen() throws IOException {
        return update(GitHubSecretScanningAlertState.OPEN, null, null);
    }

    /**
     * Returns the complete response this object was built from, including the detected secret. The record prints the
     * secret in its {@code toString()}, so do not log it or put it in a message; the accessors of this class never
     * expose the secret.
     *
     * @return the response record
     */
    public GitHubSecretScanningAlertWithMetadata response() {
        return response;
    }

    private SecretScanningAlert update(
            final GitHubSecretScanningAlertState state,
            final @Nullable GitHubSecretScanningAlertResolution resolution,
            final @Nullable String comment)
            throws IOException {
        final OptionalNull<String> sentComment = comment == null ? null : OptionalNull.of(comment);
        return new SecretScanningAlert(
                repository,
                github().secretScanning()
                        .alerts()
                        .updateAlert(
                                repository.owner(),
                                repository.name(),
                                number(),
                                new GitHubUpdateSecretScanningAlertPayload(
                                        state, resolution, sentComment, null, null)));
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
