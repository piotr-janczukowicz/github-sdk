package io.github.piotr_janczukowicz.github.sdk.security;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlert;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlertDismissedReason;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlertInstanceList;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlertSetState;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlertState;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubUpdateAlertPayload;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A code scanning alert of a {@link Repository}, as GitHub returned it. The object is immutable, and
 * {@link #response()} gives the complete response, including the rule, for every field the accessors here do not cover.
 */
public final class CodeScanningAlert {

    private final Repository repository;
    private final GitHubCodeScanningAlert response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public CodeScanningAlert(final Repository repository, final GitHubCodeScanningAlert response) {
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
     * @return the state, or empty when the response has none
     */
    public Optional<GitHubCodeScanningAlertState> state() {
        return Optional.ofNullable(response.state());
    }

    /**
     * Tells whether the alert is open.
     *
     * @return {@code true} when the state is {@code OPEN}
     */
    public boolean isOpen() {
        return response.state() == GitHubCodeScanningAlertState.OPEN;
    }

    /**
     * Returns the id of the rule that raised the alert, for example {@code java/sql-injection}.
     *
     * @return the rule id, or empty when the response has none
     */
    public Optional<String> ruleId() {
        return Optional.ofNullable(response.rule().id());
    }

    /**
     * Returns the name of the rule that raised the alert.
     *
     * @return the rule name, or empty when the response has none
     */
    public Optional<String> ruleName() {
        return Optional.ofNullable(response.rule().name());
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
     * Loads the alert again ({@code GET /repos/{owner}/{repo}/code-scanning/alerts/{alert_number}}). This object is not
     * changed.
     *
     * @return the alert as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the alert is gone
     */
    public CodeScanningAlert refresh() throws IOException {
        return repository
                .codeScanningAlert(number())
                .orElseThrow(() -> new IllegalStateException("Code scanning alert " + number() + " is gone"));
    }

    /**
     * Lists the instances of the alert, one per branch or pull request where it appears ({@code GET
     * /repos/{owner}/{repo}/code-scanning/alerts/{alert_number}/instances}) as GitHub returns them. Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the instances
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubCodeScanningAlertInstanceList> instances() throws IOException {
        return github()
                .codeScanning()
                .codeScanning()
                .listAlertInstances(repository.owner(), repository.name(), number())
                .stream();
    }

    /**
     * Dismisses the alert without a comment. See {@link #dismiss(GitHubCodeScanningAlertDismissedReason, String)}.
     *
     * @param reason why the alert is dismissed
     * @return the alert as GitHub holds it afterwards
     * @throws NullPointerException if {@code reason} is {@code null}
     * @throws IOException if the transport fails
     */
    public CodeScanningAlert dismiss(final GitHubCodeScanningAlertDismissedReason reason) throws IOException {
        return update(GitHubCodeScanningAlertSetState.DISMISSED, requireNonNull(reason, "reason"), null);
    }

    /**
     * Dismisses the alert ({@code PATCH /repos/{owner}/{repo}/code-scanning/alerts/{alert_number}}). This object is not
     * changed.
     *
     * @param reason why the alert is dismissed
     * @param comment a comment that explains the dismissal
     * @return the alert as GitHub holds it afterwards
     * @throws NullPointerException if {@code reason} or {@code comment} is {@code null}
     * @throws IOException if the transport fails
     */
    public CodeScanningAlert dismiss(final GitHubCodeScanningAlertDismissedReason reason, final String comment)
            throws IOException {
        return update(
                GitHubCodeScanningAlertSetState.DISMISSED,
                requireNonNull(reason, "reason"),
                requireNonNull(comment, "comment"));
    }

    /**
     * Opens a dismissed alert again ({@code PATCH /repos/{owner}/{repo}/code-scanning/alerts/{alert_number}}). This
     * object is not changed.
     *
     * @return the alert as GitHub holds it afterwards
     * @throws IOException if the transport fails
     */
    public CodeScanningAlert reopen() throws IOException {
        return update(GitHubCodeScanningAlertSetState.OPEN, null, null);
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubCodeScanningAlert response() {
        return response;
    }

    private CodeScanningAlert update(
            final GitHubCodeScanningAlertSetState state,
            final @Nullable GitHubCodeScanningAlertDismissedReason reason,
            final @Nullable String comment)
            throws IOException {
        return new CodeScanningAlert(
                repository,
                github().codeScanning()
                        .codeScanning()
                        .updateAlert(
                                repository.owner(),
                                repository.name(),
                                number(),
                                new GitHubUpdateAlertPayload(state, reason, comment, null, null)));
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
