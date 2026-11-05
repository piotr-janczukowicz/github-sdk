package io.github.piotr_janczukowicz.github.sdk.pages;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPageBuild;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPageBuildStatus;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPagesBuildType;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPagesSite;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPagesSiteStatus;
import io.github.piotr_janczukowicz.github.api.pages.GitHubUpdatePagesSitePayload;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The GitHub Pages site of a {@link Repository}, as GitHub returned it. The object is immutable, and
 * {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class PagesSite {

    private final Repository repository;
    private final GitHubPagesSite response;

    public PagesSite(final Repository repository, final GitHubPagesSite response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the site belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the public address of the site.
     *
     * @return the URL
     */
    public String htmlUrl() {
        return response.htmlUrl();
    }

    /**
     * Returns the build status of the site, as the client decoded it. The client maps a value it does not know to its
     * own {@code UNRECOGNIZED} constant.
     *
     * @return the status
     */
    public GitHubPagesSiteStatus status() {
        return response.status();
    }

    /**
     * Returns how the site is built, from a branch or by a workflow.
     *
     * @return the build type
     */
    public GitHubPagesBuildType buildType() {
        return response.buildType();
    }

    /**
     * Returns the custom domain.
     *
     * @return the domain, or empty when the site uses the default address
     */
    public Optional<String> cname() {
        return Optional.ofNullable(response.cname());
    }

    /**
     * Tells whether the site can be read by everyone.
     *
     * @return {@code true} when the site is public
     */
    public boolean isPublic() {
        return response.sitePublic();
    }

    /**
     * Tells whether the site is only served over HTTPS.
     *
     * @return {@code true} when HTTPS is enforced
     */
    public boolean isHttpsEnforced() {
        return Boolean.TRUE.equals(response.httpsEnforced());
    }

    /**
     * Loads the site again ({@code GET /repos/{owner}/{repo}/pages}). This object is not changed.
     *
     * @return the site as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the repository has no Pages site any more
     */
    public PagesSite refresh() throws IOException {
        return repository.pages().orElseThrow(() -> new IllegalStateException("The repository has no Pages site"));
    }

    /**
     * Changes the site ({@code PUT /repos/{owner}/{repo}/pages}) and loads it again. This object is not changed.
     *
     * @param payload the changes, as the client defines them
     * @return the site as GitHub holds it afterwards
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the repository has no Pages site any more
     */
    public PagesSite update(final GitHubUpdatePagesSitePayload payload) throws IOException {
        github().pages()
                .updateInformationAboutPagesSite(
                        repository.owner(), repository.name(), requireNonNull(payload, "payload"));
        return refresh();
    }

    /**
     * Turns the site off ({@code DELETE /repos/{owner}/{repo}/pages}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        github().pages().deletePagesSite(repository.owner(), repository.name());
    }

    /**
     * Lists the builds of the site ({@code GET /repos/{owner}/{repo}/pages/builds}) as GitHub returns them. Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the builds, newest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubPageBuild> builds() throws IOException {
        return github()
                .pages()
                .listPagesBuilds(repository.owner(), repository.name(), GitHubPageRequest.byPage())
                .stream();
    }

    /**
     * Loads the latest build ({@code GET /repos/{owner}/{repo}/pages/builds/latest}).
     *
     * @return the build
     * @throws IOException if the transport fails
     */
    public GitHubPageBuild latestBuild() throws IOException {
        return github().pages().getLatestPagesBuild(repository.owner(), repository.name());
    }

    /**
     * Asks GitHub to build the site again ({@code POST /repos/{owner}/{repo}/pages/builds}). The build runs
     * asynchronously.
     *
     * @return the queued build as GitHub reports it
     * @throws IOException if the transport fails
     */
    public GitHubPageBuildStatus requestBuild() throws IOException {
        return github().pages().requestPagesBuild(repository.owner(), repository.name());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubPagesSite response() {
        return response;
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
