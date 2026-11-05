package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchApi;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitApi;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitComparison;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubBlob;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateRefPayload;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGetTreeFilter;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitBlobsApi;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitRef;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitRefsApi;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitTree;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitTreesApi;
import io.github.piotr_janczukowicz.github.sdk.content.Branch;
import io.github.piotr_janczukowicz.github.sdk.fixtures.GitFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.github.sdk.git.Comparison;
import io.github.piotr_janczukowicz.github.sdk.git.Ref;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryGitTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void compare_shouldReturnTheComparison_whenGitHubHasIt() throws IOException {
        final GitHubCommitComparison loaded = GitFixtures.comparison();
        final GitHubCommitApi api = github.commits().commit();
        doReturn(Optional.of(loaded)).when(api).compareCommits(OWNER, NAME, "main...feature");

        final Optional<Comparison> found = sut.compare("main", "feature");

        assertThat(found).isPresent();
        assertSoftly(softly -> {
            softly.assertThat(found.orElseThrow().response()).isSameAs(loaded);
            softly.assertThat(found.orElseThrow().repository()).isSameAs(sut);
        });
    }

    @Test
    void compare_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubCommitApi api = github.commits().commit();
        doReturn(Optional.empty()).when(api).compareCommits(OWNER, NAME, "main...missing");

        final Optional<Comparison> found = sut.compare("main", "missing");

        assertThat(found).isEmpty();
    }

    @Test
    void compare_shouldThrowIllegalArgumentException_whenBaseIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.compare(" ", "feature"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("base");
    }

    @Test
    void compare_shouldThrowIllegalArgumentException_whenHeadIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.compare("main", " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("head");
    }

    @Test
    void refs_shouldStreamTheMatchingRefs_whenTheyExist() throws IOException {
        final GitHubGitRef listed = GitFixtures.ref();
        when(github.gitDatabase().refs().listMatchingRefs(OWNER, NAME, "heads/").stream())
                .thenReturn(Stream.of(listed));

        final List<Ref> refs = sut.refs("heads/").toList();

        assertThat(refs).singleElement().satisfies(ref -> {
            assertThat(ref.response()).isSameAs(listed);
            assertThat(ref.repository()).isSameAs(sut);
        });
    }

    @Test
    void refs_shouldThrowIllegalArgumentException_whenPrefixIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.refs(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("prefix");
    }

    @Test
    void ref_shouldReturnTheRef_whenGitHubHasIt() throws IOException {
        final GitHubGitRef loaded = GitFixtures.ref();
        final GitHubGitRefsApi refs = github.gitDatabase().refs();
        doReturn(Optional.of(loaded)).when(refs).getRef(OWNER, NAME, "heads/main");

        final Optional<Ref> found = sut.ref("heads/main");

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(loaded);
    }

    @Test
    void ref_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubGitRefsApi refs = github.gitDatabase().refs();
        doReturn(Optional.empty()).when(refs).getRef(OWNER, NAME, "heads/none");

        final Optional<Ref> found = sut.ref("heads/none");

        assertThat(found).isEmpty();
    }

    @Test
    void ref_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.ref(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ref");
    }

    @Test
    void createRef_shouldSendTheNameAndShaAndReturnTheRef_whenCalled() throws IOException {
        final GitHubGitRef created = GitFixtures.refWith("refs/tags/v1", GitFixtures.SHA);
        final GitHubGitRefsApi refs = github.gitDatabase().refs();
        doReturn(created).when(refs).createRef(eq(OWNER), eq(NAME), any(GitHubCreateRefPayload.class));
        final ArgumentCaptor<GitHubCreateRefPayload> payload = ArgumentCaptor.forClass(GitHubCreateRefPayload.class);

        final Ref ref = sut.createRef("refs/tags/v1", GitFixtures.SHA);

        verify(refs).createRef(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().ref()).isEqualTo("refs/tags/v1");
            softly.assertThat(payload.getValue().sha()).isEqualTo(GitFixtures.SHA);
            softly.assertThat(ref.response()).isSameAs(created);
        });
    }

    @Test
    void createRef_shouldThrowIllegalArgumentException_whenShaIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createRef("refs/tags/v1", " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sha");
    }

    @Test
    void createBranch_shouldCreateTheRefAndReturnTheBranch_whenCalled() throws IOException {
        final GitHubGitRefsApi refs = github.gitDatabase().refs();
        doReturn(GitFixtures.refWith("refs/heads/feature", GitFixtures.SHA))
                .when(refs)
                .createRef(eq(OWNER), eq(NAME), any(GitHubCreateRefPayload.class));
        final GitHubBranchApi branches = github.branches().branch();
        doReturn(Optional.of(RepositoryContentFixtures.branch())).when(branches).getBranch(OWNER, NAME, "feature");
        final ArgumentCaptor<GitHubCreateRefPayload> payload = ArgumentCaptor.forClass(GitHubCreateRefPayload.class);

        final Branch branch = sut.createBranch("feature", GitFixtures.SHA);

        verify(refs).createRef(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().ref()).isEqualTo("refs/heads/feature");
            softly.assertThat(branch.repository()).isSameAs(sut);
        });
    }

    @Test
    void createBranch_shouldThrowIllegalStateException_whenTheBranchIsGone() throws IOException {
        final GitHubGitRefsApi refs = github.gitDatabase().refs();
        doReturn(GitFixtures.refWith("refs/heads/feature", GitFixtures.SHA))
                .when(refs)
                .createRef(eq(OWNER), eq(NAME), any(GitHubCreateRefPayload.class));
        final GitHubBranchApi branches = github.branches().branch();
        doReturn(Optional.empty()).when(branches).getBranch(OWNER, NAME, "feature");

        final Throwable thrown = catchThrowable(() -> sut.createBranch("feature", GitFixtures.SHA));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void createBranch_shouldThrowIllegalArgumentException_whenBranchIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createBranch(" ", GitFixtures.SHA));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("branch");
    }

    @Test
    void tree_shouldLoadTheDirectEntries_whenNotRecursive() throws IOException {
        final GitHubGitTree loaded = mock(GitHubGitTree.class);
        final GitHubGitTreesApi trees = github.gitDatabase().trees();
        doReturn(Optional.of(loaded)).when(trees).getTree(OWNER, NAME, GitFixtures.SHA, GitHubGetTreeFilter.empty());

        final Optional<GitHubGitTree> tree = sut.tree(GitFixtures.SHA);

        assertThat(tree).containsSame(loaded);
    }

    @Test
    void tree_shouldLoadAllEntries_whenRecursive() throws IOException {
        final GitHubGitTree loaded = mock(GitHubGitTree.class);
        final GitHubGitTreesApi trees = github.gitDatabase().trees();
        doReturn(Optional.of(loaded))
                .when(trees)
                .getTree(OWNER, NAME, GitFixtures.SHA, GitHubGetTreeFilter.recursively());

        final Optional<GitHubGitTree> tree = sut.tree(GitFixtures.SHA, true);

        assertThat(tree).containsSame(loaded);
    }

    @Test
    void tree_shouldThrowIllegalArgumentException_whenShaIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.tree(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sha");
    }

    @Test
    void blob_shouldReturnTheBlob_whenGitHubHasIt() throws IOException {
        final GitHubBlob loaded = mock(GitHubBlob.class);
        final GitHubGitBlobsApi blobs = github.gitDatabase().blobs();
        doReturn(Optional.of(loaded)).when(blobs).getBlob(OWNER, NAME, GitFixtures.SHA);

        final Optional<GitHubBlob> blob = sut.blob(GitFixtures.SHA);

        assertThat(blob).containsSame(loaded);
    }

    @Test
    void blob_shouldThrowIllegalArgumentException_whenShaIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.blob(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sha");
    }
}
