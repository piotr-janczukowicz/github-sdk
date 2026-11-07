package io.github.piotr_janczukowicz.github.sdk.advisories;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubCreateRepositoryAdvisoryCommentPayload;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubRepositoryAdvisory;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubRepositoryAdvisoryComment;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubRepositoryAdvisorySeverity;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubRepositoryAdvisoryState;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubUpdateRepositoryAdvisoryPayload;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A security advisory of a {@link Repository}, the private report a maintainer prepares before a vulnerability is
 * published. The object is immutable, and {@link #response()} gives the complete response. Reading needs the Repository
 * security advisories permission, and managing an advisory needs write access to it.
 */
public final class Advisory {

    private final Repository repository;
    private final GitHubRepositoryAdvisory response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Advisory(final Repository repository, final GitHubRepositoryAdvisory response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the advisory belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the GitHub Security Advisory id, for example {@code GHSA-xxxx-xxxx-xxxx}.
     *
     * @return the GHSA id
     */
    public String ghsaId() {
        return response.ghsaId();
    }

    /**
     * Returns the advisory summary.
     *
     * @return the summary
     */
    public String summary() {
        return response.summary();
    }

    /**
     * Returns the severity of the advisory.
     *
     * @return the severity, or empty when the response has none
     */
    public Optional<GitHubRepositoryAdvisorySeverity> severity() {
        return Optional.ofNullable(response.severity());
    }

    /**
     * Returns the state of the advisory, such as draft, published or closed.
     *
     * @return the state
     */
    public GitHubRepositoryAdvisoryState state() {
        return response.state();
    }

    /**
     * Loads the advisory again ({@code GET /repos/{owner}/{repo}/security-advisories/{ghsa_id}}). This object is not
     * changed.
     *
     * @return the advisory as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the advisory is gone
     */
    public Advisory refresh() throws IOException {
        return new RepositoryAdvisories(repository)
                .find(ghsaId())
                .orElseThrow(() -> new IllegalStateException("Advisory " + ghsaId() + " is gone"));
    }

    /**
     * Changes the advisory ({@code PATCH /repos/{owner}/{repo}/security-advisories/{ghsa_id}}). Fields left
     * {@code null} in the payload stay as they are. This object is not changed.
     *
     * @param payload the changes, as the client record
     * @return the advisory as GitHub holds it afterwards
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Advisory update(final GitHubUpdateRepositoryAdvisoryPayload payload) throws IOException {
        return new Advisory(
                repository,
                github().securityAdvisories()
                        .repositoryAdvisories()
                        .updateRepositoryAdvisory(
                                repository.owner(), repository.name(), ghsaId(), requireNonNull(payload, "payload")));
    }

    /**
     * Lists the comments of the advisory ({@code GET /repos/{owner}/{repo}/security-advisories/{ghsa_id}/comments}) as
     * GitHub returns them. The stream is lazy and can be used once.
     *
     * @return the comments
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubRepositoryAdvisoryComment> comments() throws IOException {
        return github()
                .securityAdvisories()
                .repositoryAdvisories()
                .listRepositoryAdvisoryComments(repository.owner(), repository.name(), ghsaId())
                .stream();
    }

    /**
     * Adds a comment ({@code POST /repos/{owner}/{repo}/security-advisories/{ghsa_id}/comments}).
     *
     * @param body the comment text
     * @return the new comment as GitHub returns it
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public GitHubRepositoryAdvisoryComment addComment(final String body) throws IOException {
        return github().securityAdvisories()
                .repositoryAdvisories()
                .createRepositoryAdvisoryComment(
                        repository.owner(),
                        repository.name(),
                        ghsaId(),
                        new GitHubCreateRepositoryAdvisoryCommentPayload(requireNotBlank(body, "body")));
    }

    /**
     * Asks GitHub to assign a CVE id to the advisory ({@code POST
     * /repos/{owner}/{repo}/security-advisories/{ghsa_id}/cve}). GitHub answers asynchronously, so the CVE id shows up
     * after {@link #refresh()} some time later.
     *
     * @throws IOException if the transport fails
     */
    public void requestCve() throws IOException {
        github().securityAdvisories()
                .repositoryAdvisories()
                .createRepositoryAdvisoryCveRequest(repository.owner(), repository.name(), ghsaId());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubRepositoryAdvisory response() {
        return response;
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
