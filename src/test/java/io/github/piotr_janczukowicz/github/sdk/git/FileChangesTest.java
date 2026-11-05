package io.github.piotr_janczukowicz.github.sdk.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitApi;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateBlobPayload;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateCommitPayload;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateTreeEntry;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateTreeEntryMode;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateTreePayload;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitBlobsApi;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitCommit;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitCommitTree;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitCommitsApi;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitDatabaseApi;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitRefsApi;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitTree;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitTreeEntry;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitTreesApi;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubShortBlob;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubUpdateRefPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommit;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.content.Commit;
import io.github.piotr_janczukowicz.github.sdk.fixtures.GitFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class FileChangesTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final String URL = "https://api.github.com/x";
    private static final String DOCS_TREE = "4".repeat(40);
    private static final String TREE = "1".repeat(40);
    private static final String BLOB = "3".repeat(40);
    private static final String NEW_TREE = "2".repeat(40);
    private static final String NEW_COMMIT = GitFixtures.NEW_SHA;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubGitDatabaseApi git = github.gitDatabase();
    private final GitHubGitRefsApi refs = git.refs();
    private final GitHubGitBlobsApi blobs = git.blobs();
    private final GitHubGitTreesApi trees = git.trees();
    private final GitHubGitCommitsApi gitCommits = git.commits();
    private final GitHubCommitApi commitApi = github.commits().commit();
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final FileChanges sut = repository.commitTo("main");

    @Test
    void send_shouldCommitWritesAndDeletionsAsOneCommit_whenChangesAreChosen() throws IOException {
        arrangeBranch();
        final GitHubCommit created = RepositoryContentFixtures.commit();
        doReturn(Optional.of(created)).when(commitApi).getCommit(OWNER, NAME, NEW_COMMIT);
        final ArgumentCaptor<GitHubCreateTreePayload> tree = ArgumentCaptor.forClass(GitHubCreateTreePayload.class);
        final ArgumentCaptor<GitHubCreateCommitPayload> commit =
                ArgumentCaptor.forClass(GitHubCreateCommitPayload.class);
        final ArgumentCaptor<GitHubUpdateRefPayload> ref = ArgumentCaptor.forClass(GitHubUpdateRefPayload.class);

        final Commit result = sut.write("docs/a.txt", "hello").delete("old.txt").send("Update docs");

        verify(trees).createTree(eq(OWNER), eq(NAME), tree.capture());
        verify(gitCommits).createCommit(eq(OWNER), eq(NAME), commit.capture());
        verify(refs).updateRef(eq(OWNER), eq(NAME), eq("heads/main"), ref.capture());
        verify(blobs, never()).createBlob(any(), any(), any());
        final List<GitHubCreateTreeEntry> entries = tree.getValue().tree();
        assertSoftly(softly -> {
            softly.assertThat(tree.getValue().baseTree()).isEqualTo(TREE);
            softly.assertThat(entries).extracting(GitHubCreateTreeEntry::path).containsExactly("docs/a.txt", "old.txt");
            softly.assertThat(entries.get(0).content()).isEqualTo("hello");
            softly.assertThat(entries.get(0).sha()).isNull();
            softly.assertThat(entries.get(0).mode()).isEqualTo(GitHubCreateTreeEntryMode.FILE);
            softly.assertThat(entries.get(1).sha().value()).isNull();
            softly.assertThat(commit.getValue().message()).isEqualTo("Update docs");
            softly.assertThat(commit.getValue().tree()).isEqualTo(NEW_TREE);
            softly.assertThat(commit.getValue().parents()).containsExactly(GitFixtures.SHA);
            softly.assertThat(ref.getValue().sha()).isEqualTo(NEW_COMMIT);
            softly.assertThat(ref.getValue().force()).isFalse();
            softly.assertThat(result.response()).isSameAs(created);
        });
    }

    @Test
    void send_shouldKeepTheModeOfTheExistingFile_whenAnExecutableFileIsRewritten() throws IOException {
        arrangeBranch();
        doReturn(Optional.of(RepositoryContentFixtures.commit()))
                .when(commitApi)
                .getCommit(OWNER, NAME, NEW_COMMIT);
        final ArgumentCaptor<GitHubCreateTreePayload> tree = ArgumentCaptor.forClass(GitHubCreateTreePayload.class);

        sut.write("run.sh", "echo hi")
                .write("docs/old.md", "text")
                .write("docs/new.md", "text")
                .write("docs/old.md/inner", "text")
                .write("link", "target")
                .write("link/inner", "text")
                .send("Rewrite");

        verify(trees).createTree(eq(OWNER), eq(NAME), tree.capture());
        assertThat(tree.getValue().tree())
                .extracting(GitHubCreateTreeEntry::path, GitHubCreateTreeEntry::mode)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple("run.sh", GitHubCreateTreeEntryMode.EXECUTABLE),
                        org.assertj.core.api.Assertions.tuple("docs/old.md", GitHubCreateTreeEntryMode.FILE),
                        org.assertj.core.api.Assertions.tuple("docs/new.md", GitHubCreateTreeEntryMode.FILE),
                        org.assertj.core.api.Assertions.tuple("docs/old.md/inner", GitHubCreateTreeEntryMode.FILE),
                        org.assertj.core.api.Assertions.tuple("link", GitHubCreateTreeEntryMode.SYMLINK),
                        org.assertj.core.api.Assertions.tuple("link/inner", GitHubCreateTreeEntryMode.FILE));
    }

    @Test
    void send_shouldCreateABlob_whenTheTextIsEmptyOrHasANulCharacter() throws IOException {
        arrangeBranch();
        doReturn(new GitHubShortBlob("https://api.github.com/blob", BLOB))
                .when(blobs)
                .createBlob(eq(OWNER), eq(NAME), any(GitHubCreateBlobPayload.class));
        doReturn(Optional.of(RepositoryContentFixtures.commit()))
                .when(commitApi)
                .getCommit(OWNER, NAME, NEW_COMMIT);
        final ArgumentCaptor<GitHubCreateTreePayload> tree = ArgumentCaptor.forClass(GitHubCreateTreePayload.class);

        sut.write("empty.txt", "").write("nul.txt", "a\u0000b").send("Odd text");

        verify(blobs, times(2)).createBlob(eq(OWNER), eq(NAME), any(GitHubCreateBlobPayload.class));
        verify(trees).createTree(eq(OWNER), eq(NAME), tree.capture());
        assertThat(tree.getValue().tree())
                .extracting(entry -> entry.sha().value())
                .containsOnly(BLOB);
    }

    @Test
    void send_shouldThrowIllegalStateException_whenABaseTreeIsGone() throws IOException {
        arrangeHead();
        doReturn(Optional.empty()).when(trees).getTree(OWNER, NAME, TREE);
        final FileChanges changes = sut.write("a.txt", "x");

        final Throwable thrown = catchThrowable(() -> changes.send("Gone"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void send_shouldWriteBytesAsTheyAre_whenContentIsBinary() throws IOException {
        arrangeBranch();
        doReturn(new GitHubShortBlob("https://api.github.com/blob", BLOB))
                .when(blobs)
                .createBlob(eq(OWNER), eq(NAME), any(GitHubCreateBlobPayload.class));
        doReturn(Optional.of(RepositoryContentFixtures.commit()))
                .when(commitApi)
                .getCommit(OWNER, NAME, NEW_COMMIT);
        final byte[] content = {1, 0, 3};
        final ArgumentCaptor<GitHubCreateBlobPayload> blobPayload =
                ArgumentCaptor.forClass(GitHubCreateBlobPayload.class);

        sut.write("bin/data", content).send("Add data");

        verify(blobs).createBlob(eq(OWNER), eq(NAME), blobPayload.capture());
        assertThat(Base64.getDecoder().decode(blobPayload.getValue().content())).containsExactly(1, 0, 3);
    }

    @Test
    void send_shouldKeepOnlyTheLastChoiceForAPath_whenAPathIsChosenTwice() throws IOException {
        arrangeBranch();
        doReturn(Optional.of(RepositoryContentFixtures.commit()))
                .when(commitApi)
                .getCommit(OWNER, NAME, NEW_COMMIT);
        final ArgumentCaptor<GitHubCreateTreePayload> tree = ArgumentCaptor.forClass(GitHubCreateTreePayload.class);

        sut.write("a.txt", "first").delete("a.txt").send("Drop it");

        verify(trees).createTree(eq(OWNER), eq(NAME), tree.capture());
        verify(blobs, never()).createBlob(any(), any(), any());
        assertThat(tree.getValue().tree()).singleElement().satisfies(entry -> {
            assertThat(entry.path()).isEqualTo("a.txt");
            assertThat(entry.sha().value()).isNull();
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingIsChosen() {
        final Throwable thrown = catchThrowable(() -> sut.send("Nothing"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("nothing to commit");
    }

    @Test
    void send_shouldThrowIllegalStateException_whenTheBranchIsGone() throws IOException {
        doReturn(Optional.empty()).when(refs).getRef(OWNER, NAME, "heads/main");
        final FileChanges changes = sut.write("a.txt", "x");

        final Throwable thrown = catchThrowable(() -> changes.send("Add"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("Branch main is gone");
    }

    @Test
    void send_shouldThrowIllegalStateException_whenTheHeadCommitIsGone() throws IOException {
        doReturn(Optional.of(GitFixtures.ref())).when(refs).getRef(OWNER, NAME, "heads/main");
        doReturn(Optional.empty()).when(gitCommits).getCommit(OWNER, NAME, GitFixtures.SHA);
        final FileChanges changes = sut.write("a.txt", "x");

        final Throwable thrown = catchThrowable(() -> changes.send("Add"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void send_shouldThrowIllegalStateException_whenTheNewCommitIsGone() throws IOException {
        arrangeBranch();
        doReturn(Optional.empty()).when(commitApi).getCommit(OWNER, NAME, NEW_COMMIT);
        final FileChanges changes = sut.delete("a.txt");

        final Throwable thrown = catchThrowable(() -> changes.send("Drop"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void send_shouldThrowIllegalArgumentException_whenMessageIsBlank() {
        final FileChanges changes = sut.delete("a.txt");

        final Throwable thrown = catchThrowable(() -> changes.send(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("message");
    }

    @Test
    void write_shouldThrowIllegalArgumentException_whenPathIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.write(" ", "x"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("path");
    }

    @Test
    void write_shouldThrowNullPointerException_whenTextIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.write("a.txt", (String) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("text");
    }

    @Test
    void write_shouldThrowNullPointerException_whenBytesAreNull() {
        final Throwable thrown = catchThrowable(() -> sut.write("a.txt", (byte[]) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("content");
    }

    @Test
    void delete_shouldThrowIllegalArgumentException_whenPathIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.delete(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("path");
    }

    @Test
    void commitTo_shouldThrowIllegalArgumentException_whenBranchIsBlank() {
        final Throwable thrown = catchThrowable(() -> repository.commitTo(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("branch");
    }

    private void arrangeHead() throws IOException {
        doReturn(Optional.of(GitFixtures.ref())).when(refs).getRef(OWNER, NAME, "heads/main");
        final GitHubGitCommit head = mock(GitHubGitCommit.class);
        final GitHubGitCommitTree headTree = mock(GitHubGitCommitTree.class);
        when(headTree.sha()).thenReturn(TREE);
        when(head.tree()).thenReturn(headTree);
        doReturn(Optional.of(head)).when(gitCommits).getCommit(OWNER, NAME, GitFixtures.SHA);
    }

    private void arrangeBranch() throws IOException {
        arrangeHead();
        final GitHubGitTreeEntry script = new GitHubGitTreeEntry("run.sh", "100755", "blob", BLOB, 10, URL);
        final GitHubGitTreeEntry link = new GitHubGitTreeEntry("link", "120000", "blob", BLOB, 10, URL);
        final GitHubGitTreeEntry docs = new GitHubGitTreeEntry("docs", "040000", "tree", DOCS_TREE, null, URL);
        doReturn(Optional.of(new GitHubGitTree(TREE, URL, false, List.of(script, link, docs))))
                .when(trees)
                .getTree(OWNER, NAME, TREE);
        final GitHubGitTreeEntry old = new GitHubGitTreeEntry("old.md", "100644", "blob", BLOB, 10, URL);
        doReturn(Optional.of(new GitHubGitTree(DOCS_TREE, URL, false, List.of(old))))
                .when(trees)
                .getTree(OWNER, NAME, DOCS_TREE);
        final GitHubGitTree created = mock(GitHubGitTree.class);
        when(created.sha()).thenReturn(NEW_TREE);
        doReturn(created).when(trees).createTree(eq(OWNER), eq(NAME), any(GitHubCreateTreePayload.class));
        final GitHubGitCommit newCommit = mock(GitHubGitCommit.class);
        when(newCommit.sha()).thenReturn(NEW_COMMIT);
        doReturn(newCommit).when(gitCommits).createCommit(eq(OWNER), eq(NAME), any(GitHubCreateCommitPayload.class));
    }

    @Test
    void send_shouldThrowIllegalStateException_whenTheBaseTreeIsTruncated() throws IOException {
        arrangeHead();
        doReturn(Optional.of(new GitHubGitTree(TREE, URL, true, List.of())))
                .when(trees)
                .getTree(OWNER, NAME, TREE);
        final FileChanges changes = sut.write("a.txt", "x");

        final Throwable thrown = catchThrowable(() -> changes.send("Big"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("too large");
    }
}
