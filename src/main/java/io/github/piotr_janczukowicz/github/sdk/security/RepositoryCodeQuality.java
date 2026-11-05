package io.github.piotr_janczukowicz.github.sdk.security;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.codequality.GitHubCodeQualityFinding;
import io.github.piotr_janczukowicz.github.api.codequality.GitHubCodeQualitySetup;
import io.github.piotr_janczukowicz.github.api.codequality.GitHubListFindingsForRepoFilter;
import io.github.piotr_janczukowicz.github.api.codequality.GitHubListFindingsForRepoState;
import io.github.piotr_janczukowicz.github.client.exception.GitHubNotFoundException;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The code quality findings and setup of a {@link Repository}. The records are returned as GitHub sends them. Nothing
 * is requested until a method is called, and GitHub answers HTTP 404 for a repository where code quality is not
 * available.
 */
public final class RepositoryCodeQuality {

    private final Repository repository;

    /**
     * Creates the handle. Applications normally get it from the object it belongs to. Nothing is requested until a
     * method is called.
     *
     * @param repository the repository the object belongs to
     * @throws NullPointerException if an argument is {@code null}
     */
    public RepositoryCodeQuality(final Repository repository) {
        this.repository = requireNonNull(repository, "repository");
    }

    /**
     * Lists the findings ({@code GET /repos/{owner}/{repo}/code-quality/findings}). The stream is lazy and can be used
     * once.
     *
     * @return the findings
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubCodeQualityFinding> findings() throws IOException {
        return listFindings(GitHubListFindingsForRepoFilter.empty());
    }

    /**
     * Lists the findings in one state, open or dismissed.
     *
     * @param state the state to list
     * @return the findings
     * @throws NullPointerException if {@code state} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubCodeQualityFinding> findings(final GitHubListFindingsForRepoState state) throws IOException {
        return listFindings(new GitHubListFindingsForRepoFilter(null, requireNonNull(state, "state")));
    }

    /**
     * Loads one finding ({@code GET /repos/{owner}/{repo}/code-quality/findings/{finding_number}}).
     *
     * @param number the finding number
     * @return the finding, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GitHubCodeQualityFinding> finding(final int number) throws IOException {
        return github().codeQuality().getFinding(repository.owner(), repository.name(), number);
    }

    /**
     * Loads the code quality setup ({@code GET /repos/{owner}/{repo}/code-quality/setup}).
     *
     * @return the setup, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GitHubCodeQualitySetup> setup() throws IOException {
        try {
            return Optional.of(github().codeQuality().getSetup(repository.owner(), repository.name()));
        } catch (final GitHubNotFoundException e) {
            return Optional.empty();
        }
    }

    private Stream<GitHubCodeQualityFinding> listFindings(final GitHubListFindingsForRepoFilter filter)
            throws IOException {
        return github()
                .codeQuality()
                .listFindingsForRepo(repository.owner(), repository.name(), filter, GitHubPageRequest.byCursor())
                .stream();
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
