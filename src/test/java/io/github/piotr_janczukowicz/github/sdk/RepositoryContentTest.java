package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchWithProtection;
import io.github.piotr_janczukowicz.github.api.branches.GitHubListBranchesFilter;
import io.github.piotr_janczukowicz.github.api.branches.GitHubShortBranch;
import io.github.piotr_janczukowicz.github.api.commits.GitHubListCommitsFilter;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommit;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.content.Branch;
import io.github.piotr_janczukowicz.github.sdk.content.Commit;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryContentTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void branch_shouldReturnBranch_whenGitHubHasIt() throws IOException {
        final GitHubBranchWithProtection response = RepositoryContentFixtures.branch();
        when(github.branches().branch().getBranch(OWNER, NAME, "main")).thenReturn(Optional.of(response));

        final Optional<Branch> branch = sut.branch("main");

        assertSoftly(softly -> {
            softly.assertThat(branch).isPresent();
            softly.assertThat(branch.orElseThrow().response()).isSameAs(response);
            softly.assertThat(branch.orElseThrow().repository()).isSameAs(sut);
        });
    }

    @Test
    void branch_shouldReturnEmpty_whenGitHubAnswersNotFound() throws IOException {
        when(github.branches().branch().getBranch(OWNER, NAME, "gone")).thenReturn(Optional.empty());

        final Optional<Branch> branch = sut.branch("gone");

        assertThat(branch).isEmpty();
    }

    @Test
    void branch_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.branch(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void branches_shouldLoadEachListedBranch_whenCalledWithAFilter() throws IOException {
        final GitHubListBranchesFilter filter = mock(GitHubListBranchesFilter.class);
        final GitHubShortBranch listed = mock(GitHubShortBranch.class);
        when(listed.name()).thenReturn("main");
        final GitHubBranchWithProtection response = RepositoryContentFixtures.branch();
        when(github
                        .branches()
                        .branch()
                        .listBranches(eq(OWNER), eq(NAME), eq(filter), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(listed));
        when(github.branches().branch().getBranch(OWNER, NAME, "main")).thenReturn(Optional.of(response));

        final List<Branch> branches = sut.branches(filter).toList();

        assertThat(branches).extracting(Branch::response).containsExactly(response);
    }

    @Test
    void branches_shouldListWithAnEmptyFilter_whenCalledWithoutOne() throws IOException {
        final GitHubShortBranch listed = mock(GitHubShortBranch.class);
        when(listed.name()).thenReturn("main");
        final GitHubBranchWithProtection response = RepositoryContentFixtures.branch();
        when(github
                        .branches()
                        .branch()
                        .listBranches(
                                eq(OWNER), eq(NAME), any(GitHubListBranchesFilter.class), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(listed));
        when(github.branches().branch().getBranch(OWNER, NAME, "main")).thenReturn(Optional.of(response));

        final List<Branch> branches = sut.branches().toList();

        assertThat(branches).extracting(Branch::response).containsExactly(response);
    }

    @Test
    void branches_shouldSkipTheItem_whenAListedBranchIsGone() throws IOException {
        final GitHubShortBranch listed = mock(GitHubShortBranch.class);
        when(listed.name()).thenReturn("main");
        when(github
                        .branches()
                        .branch()
                        .listBranches(
                                eq(OWNER), eq(NAME), any(GitHubListBranchesFilter.class), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(listed));
        when(github.branches().branch().getBranch(OWNER, NAME, "main")).thenReturn(Optional.empty());

        final long count = sut.branches().count();

        assertThat(count).isZero();
    }

    @Test
    void branches_shouldThrowUncheckedIoException_whenLoadingAListedBranchFails() throws IOException {
        final GitHubShortBranch listed = mock(GitHubShortBranch.class);
        when(listed.name()).thenReturn("main");
        when(github
                        .branches()
                        .branch()
                        .listBranches(
                                eq(OWNER), eq(NAME), any(GitHubListBranchesFilter.class), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(listed));
        when(github.branches().branch().getBranch(OWNER, NAME, "main")).thenThrow(new IOException("transport failed"));
        final Stream<Branch> branches = sut.branches();

        final Throwable thrown = catchThrowable(branches::toList);

        assertThat(thrown).isInstanceOf(UncheckedIOException.class).hasCauseInstanceOf(IOException.class);
    }

    @Test
    void commit_shouldReturnCommit_whenGitHubHasIt() throws IOException {
        final GitHubCommit response = RepositoryContentFixtures.commit();
        when(github.commits().commit().getCommit(OWNER, NAME, "abc123")).thenReturn(Optional.of(response));

        final Optional<Commit> commit = sut.commit("abc123");

        assertSoftly(softly -> {
            softly.assertThat(commit).isPresent();
            softly.assertThat(commit.orElseThrow().response()).isSameAs(response);
            softly.assertThat(commit.orElseThrow().repository()).isSameAs(sut);
        });
    }

    @Test
    void commit_shouldReturnEmpty_whenGitHubAnswersNotFound() throws IOException {
        when(github.commits().commit().getCommit(OWNER, NAME, "abc123")).thenReturn(Optional.empty());

        final Optional<Commit> commit = sut.commit("abc123");

        assertThat(commit).isEmpty();
    }

    @Test
    void commit_shouldThrowIllegalArgumentException_whenShaIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.commit(""));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sha");
    }

    @Test
    void commits_shouldWrapEachCommit_whenCalledWithAFilter() throws IOException {
        final GitHubListCommitsFilter filter = mock(GitHubListCommitsFilter.class);
        final GitHubCommit response = RepositoryContentFixtures.commit();
        when(github
                        .commits()
                        .commit()
                        .listCommits(eq(OWNER), eq(NAME), eq(filter), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(response));

        final List<Commit> commits = sut.commits(filter).toList();

        assertThat(commits).extracting(Commit::response).containsExactly(response);
    }

    @Test
    void commits_shouldListWithAnEmptyFilter_whenCalledWithoutOne() throws IOException {
        final GitHubCommit response = RepositoryContentFixtures.commit();
        when(github
                        .commits()
                        .commit()
                        .listCommits(
                                eq(OWNER), eq(NAME), any(GitHubListCommitsFilter.class), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(response));

        final List<Commit> commits = sut.commits().toList();

        assertThat(commits).extracting(Commit::response).containsExactly(response);
    }

    @Test
    void file_shouldThrowIllegalArgumentException_whenThePathHasADotSegment() {
        final Throwable thrown = catchThrowable(() -> sut.file("docs/../secrets.txt"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("path");
    }

    @Test
    void compare_shouldThrowIllegalArgumentException_whenARefHasADotSegment() {
        final Throwable thrown = catchThrowable(() -> sut.compare("main", ".."));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("head");
    }
}
