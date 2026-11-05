package io.github.piotr_janczukowicz.github.sdk.security;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAnalysis;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAnalysisDeletion;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningCodeqlDatabase;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningDefaultSetup;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningDefaultSetupUpdate;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningDefaultSetupUpdateResponse;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningSarifsReceipt;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningSarifsStatus;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubListRecentAnalysesFilter;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubUploadSarifPayload;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The code scanning analyses, SARIF uploads, CodeQL databases and default setup of a {@link Repository}. The alerts are
 * on {@link Repository#codeScanningAlerts()}. The records are returned as GitHub sends them. Nothing is requested until
 * a method is called, and reading needs the Code scanning alerts permission.
 */
public final class RepositoryCodeScanning {

    private final Repository repository;

    public RepositoryCodeScanning(final Repository repository) {
        this.repository = requireNonNull(repository, "repository");
    }

    /**
     * Lists the recent analyses, newest first ({@code GET /repos/{owner}/{repo}/code-scanning/analyses}). GitHub
     * answers HTTP 404 with "no analysis found" for a repository that was never analyzed, which surfaces as
     * {@link io.github.piotr_janczukowicz.github.client.exception.GitHubNotFoundException}. The stream is lazy and can
     * be used once.
     *
     * @return the analyses
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubCodeScanningAnalysis> analyses() throws IOException {
        return github()
                .codeScanning()
                .codeScanning()
                .listRecentAnalyses(repository.owner(), repository.name())
                .stream();
    }

    /**
     * Lists the recent analyses that match a filter, such as a tool, a ref or a pull request.
     *
     * @param filter the client filter record
     * @return the matching analyses
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubCodeScanningAnalysis> analyses(final GitHubListRecentAnalysesFilter filter) throws IOException {
        return github()
                .codeScanning()
                .codeScanning()
                .listRecentAnalyses(
                        repository.owner(),
                        repository.name(),
                        requireNonNull(filter, "filter"),
                        GitHubPageRequest.byPage())
                .stream();
    }

    /**
     * Loads one analysis ({@code GET /repos/{owner}/{repo}/code-scanning/analyses/{analysis_id}}).
     *
     * @param id the analysis id
     * @return the analysis, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GitHubCodeScanningAnalysis> analysis(final long id) throws IOException {
        return github().codeScanning().codeScanning().getAnalysis(repository.owner(), repository.name(), id);
    }

    /**
     * Loads the SARIF file of an analysis.
     *
     * @param id the analysis id
     * @return the SARIF text, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<String> sarif(final long id) throws IOException {
        return github().codeScanning().codeScanning().getAnalysisSarif(repository.owner(), repository.name(), id);
    }

    /**
     * Deletes an analysis ({@code DELETE /repos/{owner}/{repo}/code-scanning/analyses/{analysis_id}}). GitHub deletes
     * only the last analysis of a set, and answers with the link to the next deletable one, so clearing a set means
     * repeating this call. Deleted analyses cannot be restored.
     *
     * @param id the analysis id
     * @return the links to the next deletable analysis and to the confirmed delete, as GitHub returns them
     * @throws IOException if the transport fails
     */
    public GitHubCodeScanningAnalysisDeletion deleteAnalysis(final long id) throws IOException {
        return github().codeScanning().codeScanning().deleteAnalysis(repository.owner(), repository.name(), id);
    }

    /**
     * Uploads SARIF results ({@code POST /repos/{owner}/{repo}/code-scanning/sarifs}). The payload carries the commit,
     * the ref and the SARIF content, which the caller compresses and encodes as GitHub requires. GitHub processes the
     * upload asynchronously, so follow it with {@link #sarifStatus(String)}. The resulting analysis cannot be undone
     * except by {@link #deleteAnalysis(long)}.
     *
     * @param payload the upload, as the client record
     * @return the receipt with the upload id
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public GitHubCodeScanningSarifsReceipt uploadSarif(final GitHubUploadSarifPayload payload) throws IOException {
        return github().codeScanning()
                .codeScanning()
                .uploadSarif(repository.owner(), repository.name(), requireNonNull(payload, "payload"));
    }

    /**
     * Loads the processing status of a SARIF upload ({@code GET
     * /repos/{owner}/{repo}/code-scanning/sarifs/{sarif_id}}).
     *
     * @param sarifId the upload id from the receipt
     * @return the status, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code sarifId} is {@code null}
     * @throws IllegalArgumentException if {@code sarifId} is blank
     * @throws IOException if the transport fails
     */
    public Optional<GitHubCodeScanningSarifsStatus> sarifStatus(final String sarifId) throws IOException {
        return github().codeScanning()
                .codeScanning()
                .getSarif(repository.owner(), repository.name(), requireNotBlank(sarifId, "sarifId"));
    }

    /**
     * Lists the CodeQL databases ({@code GET /repos/{owner}/{repo}/code-scanning/codeql/databases}).
     *
     * @return the databases, empty when there are none
     * @throws IOException if the transport fails
     */
    public List<GitHubCodeScanningCodeqlDatabase> databases() throws IOException {
        return github().codeScanning().codeScanning().listCodeqlDatabases(repository.owner(), repository.name());
    }

    /**
     * Loads the CodeQL database of a language.
     *
     * @param language the language, for example {@code java}
     * @return the database, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code language} is {@code null}
     * @throws IllegalArgumentException if {@code language} is blank
     * @throws IOException if the transport fails
     */
    public Optional<GitHubCodeScanningCodeqlDatabase> database(final String language) throws IOException {
        return github().codeScanning()
                .codeScanning()
                .getCodeqlDatabase(repository.owner(), repository.name(), requireNotBlank(language, "language"));
    }

    /**
     * Returns the location the CodeQL database of a language can be downloaded from. The location is short lived and
     * this library does not download it.
     *
     * @param language the language, for example {@code java}
     * @return the download location
     * @throws NullPointerException if {@code language} is {@code null}
     * @throws IllegalArgumentException if {@code language} is blank
     * @throws IOException if the transport fails
     */
    public URI databaseDownloadLocation(final String language) throws IOException {
        return github().codeScanning()
                .codeScanning()
                .getCodeqlDatabaseDownloadLocation(
                        repository.owner(), repository.name(), requireNotBlank(language, "language"));
    }

    /**
     * Deletes the CodeQL database of a language ({@code DELETE
     * /repos/{owner}/{repo}/code-scanning/codeql/databases/{language}}).
     *
     * @param language the language, for example {@code java}
     * @throws NullPointerException if {@code language} is {@code null}
     * @throws IllegalArgumentException if {@code language} is blank
     * @throws IOException if the transport fails
     */
    public void deleteDatabase(final String language) throws IOException {
        github().codeScanning()
                .codeScanning()
                .deleteCodeqlDatabase(repository.owner(), repository.name(), requireNotBlank(language, "language"));
    }

    /**
     * Loads the default setup of code scanning ({@code GET /repos/{owner}/{repo}/code-scanning/default-setup}).
     *
     * @return the setup as GitHub returns it
     * @throws IOException if the transport fails
     */
    public GitHubCodeScanningDefaultSetup defaultSetup() throws IOException {
        return github().codeScanning().codeScanning().getDefaultSetup(repository.owner(), repository.name());
    }

    /**
     * Changes the default setup ({@code PATCH /repos/{owner}/{repo}/code-scanning/default-setup}). GitHub applies the
     * change with a workflow run and answers with the run when it started one.
     *
     * @param update the changes, as the client record
     * @return the answer with the run, or empty when GitHub applied the change without one
     * @throws NullPointerException if {@code update} is {@code null}
     * @throws IOException if the transport fails
     */
    public Optional<GitHubCodeScanningDefaultSetupUpdateResponse> updateDefaultSetup(
            final GitHubCodeScanningDefaultSetupUpdate update) throws IOException {
        return github().codeScanning()
                .codeScanning()
                .updateDefaultSetup(repository.owner(), repository.name(), requireNonNull(update, "update"));
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
