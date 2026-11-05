package io.github.piotr_janczukowicz.github.sdk.security;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlert;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertDependency;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertPackage;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertSeverity;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertState;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertUpdateState;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubUpdateAlertPayload;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * A Dependabot alert of a {@link Repository}, as GitHub returned it. The object is immutable, and {@link #response()}
 * gives the complete response, including the advisory, for every field the accessors here do not cover.
 */
public final class DependabotAlert {

    private final Repository repository;
    private final GitHubDependabotAlert response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public DependabotAlert(final Repository repository, final GitHubDependabotAlert response) {
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
     * Returns the state, as the client decoded it. The client maps a value it does not know to its own
     * {@code UNRECOGNIZED} constant.
     *
     * @return the state
     */
    public GitHubDependabotAlertState state() {
        return response.state();
    }

    /**
     * Tells whether the alert is open.
     *
     * @return {@code true} when the state is {@code OPEN}
     */
    public boolean isOpen() {
        return response.state() == GitHubDependabotAlertState.OPEN;
    }

    /**
     * Returns the name of the vulnerable package.
     *
     * @return the package name, or empty when GitHub reports none
     */
    public Optional<String> packageName() {
        return Optional.of(response.dependency())
                .map(GitHubDependabotAlertDependency::dependencyPackage)
                .map(GitHubDependabotAlertPackage::name);
    }

    /**
     * Returns the severity of the advisory behind the alert, as the client decoded it. The client maps a value it does
     * not know to its own {@code UNRECOGNIZED} constant.
     *
     * @return the severity
     */
    public GitHubDependabotAlertSeverity severity() {
        return response.securityAdvisory().severity();
    }

    /**
     * Returns the short summary of the advisory behind the alert.
     *
     * @return the summary
     */
    public String summary() {
        return response.securityAdvisory().summary();
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
     * Loads the alert again ({@code GET /repos/{owner}/{repo}/dependabot/alerts/{alert_number}}). This object is not
     * changed.
     *
     * @return the alert as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the alert is gone
     */
    public DependabotAlert refresh() throws IOException {
        return repository
                .dependabotAlert(number())
                .orElseThrow(() -> new IllegalStateException("Dependabot alert " + number() + " is gone"));
    }

    /**
     * Dismisses the alert without a comment. See {@link #dismiss(DependabotDismissal, String)}.
     *
     * @param reason why the alert is dismissed
     * @return the alert as GitHub holds it afterwards
     * @throws NullPointerException if {@code reason} is {@code null}
     * @throws IOException if the transport fails
     */
    public DependabotAlert dismiss(final DependabotDismissal reason) throws IOException {
        return update(GitHubDependabotAlertUpdateState.DISMISSED, requireNonNull(reason, "reason"), null);
    }

    /**
     * Dismisses the alert ({@code PATCH /repos/{owner}/{repo}/dependabot/alerts/{alert_number}}). This object is not
     * changed.
     *
     * @param reason why the alert is dismissed
     * @param comment a comment that explains the dismissal
     * @return the alert as GitHub holds it afterwards
     * @throws NullPointerException if {@code reason} or {@code comment} is {@code null}
     * @throws IOException if the transport fails
     */
    public DependabotAlert dismiss(final DependabotDismissal reason, final String comment) throws IOException {
        return update(
                GitHubDependabotAlertUpdateState.DISMISSED,
                requireNonNull(reason, "reason"),
                requireNonNull(comment, "comment"));
    }

    /**
     * Opens a dismissed alert again ({@code PATCH /repos/{owner}/{repo}/dependabot/alerts/{alert_number}}). This object
     * is not changed.
     *
     * @return the alert as GitHub holds it afterwards
     * @throws IOException if the transport fails
     */
    public DependabotAlert reopen() throws IOException {
        return update(GitHubDependabotAlertUpdateState.OPEN, null, null);
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubDependabotAlert response() {
        return response;
    }

    private DependabotAlert update(
            final GitHubDependabotAlertUpdateState state,
            final @Nullable DependabotDismissal reason,
            final @Nullable String comment)
            throws IOException {
        return new DependabotAlert(
                repository,
                github().dependabot()
                        .alerts()
                        .updateAlert(
                                repository.owner(),
                                repository.name(),
                                number(),
                                new GitHubUpdateAlertPayload(
                                        state, reason == null ? null : reason.toClient(), comment, null, null)));
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
