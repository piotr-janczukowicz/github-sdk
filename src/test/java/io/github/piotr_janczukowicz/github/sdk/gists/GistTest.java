package io.github.piotr_janczukowicz.github.sdk.gists;

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
import io.github.piotr_janczukowicz.github.api.gists.GitHubBaseGist;
import io.github.piotr_janczukowicz.github.api.gists.GitHubCreateCommentPayload;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistApi;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistComment;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistCommit;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistSimple;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistSimpleBuilder;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistsCommentsApi;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.fixtures.GistFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class GistTest {

    private static final String ID = GistFixtures.ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);
    private final GitHubGistSimple response = GistFixtures.gist();
    private final Gist sut = new Gist(sdk, response);

    @Test
    void accessors_shouldExposeTheResponse_whenGistIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.description()).hasValue("A greeting");
            softly.assertThat(sut.isPublic()).isTrue();
            softly.assertThat(sut.htmlUrl()).endsWith(ID);
            softly.assertThat(sut.owner()).hasValue("octocat");
            softly.assertThat(sut.files()).containsOnlyKeys(GistFixtures.FILE);
            softly.assertThat(sut.content(GistFixtures.FILE)).hasValue("hello");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmptyOrNeutral_whenGitHubReportsNothing() {
        final Gist bare = new Gist(sdk, GistFixtures.bareGist());

        assertSoftly(softly -> {
            softly.assertThat(bare.description()).isEmpty();
            softly.assertThat(bare.isPublic()).isFalse();
            softly.assertThat(bare.owner()).isEmpty();
            softly.assertThat(bare.files()).isEmpty();
            softly.assertThat(bare.content(GistFixtures.FILE)).isEmpty();
        });
    }

    @Test
    void content_shouldBeEmpty_whenTheFileWasCutOff() {
        final Gist cut =
                new Gist(sdk, GistFixtures.gistWithFiles(Map.of(GistFixtures.FILE, GistFixtures.file("hel", true))));

        assertThat(cut.content(GistFixtures.FILE)).isEmpty();
    }

    @Test
    void refresh_shouldReturnTheReloadedGist_whenItExists() throws IOException {
        final GitHubGistSimple reloaded =
                GitHubGistSimpleBuilder.builder(response).description("Changed").build();
        final GitHubGistApi api = github.gists().gist();
        doReturn(Optional.of(reloaded)).when(api).getGist(ID);

        final Gist result = sut.refresh();

        assertThat(result.description()).hasValue("Changed");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheGistIsGone() throws IOException {
        final GitHubGistApi api = github.gists().gist();
        doReturn(Optional.empty()).when(api).getGist(ID);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void delete_shouldDeleteTheGist_whenCalled() throws IOException {
        final GitHubGistApi api = github.gists().gist();

        sut.delete();

        verify(api).deleteGist(ID);
    }

    @Test
    void star_shouldStarTheGist_whenCalled() throws IOException {
        final GitHubGistApi api = github.gists().gist();

        sut.star();

        verify(api).starGist(ID);
    }

    @Test
    void unstar_shouldRemoveTheStar_whenCalled() throws IOException {
        final GitHubGistApi api = github.gists().gist();

        sut.unstar();

        verify(api).unstarGist(ID);
    }

    @Test
    void isStarred_shouldReportGitHubsAnswer_whenCalled() throws IOException {
        final GitHubGistApi api = github.gists().gist();
        doReturn(true).when(api).isGistStarred(ID);

        final boolean isStarred = sut.isStarred();

        assertThat(isStarred).isTrue();
    }

    @Test
    void fork_shouldReturnTheLoadedFork_whenCalled() throws IOException {
        final GitHubBaseGist forked = mock(GitHubBaseGist.class);
        when(forked.id()).thenReturn("fork1");
        final GitHubGistApi api = github.gists().gist();
        doReturn(forked).when(api).forkGist(ID);
        final GitHubGistSimple loaded =
                GitHubGistSimpleBuilder.builder(response).id("fork1").build();
        doReturn(Optional.of(loaded)).when(api).getGist("fork1");

        final Gist fork = sut.fork();

        assertThat(fork.id()).isEqualTo("fork1");
    }

    @Test
    void fork_shouldThrowIllegalStateException_whenTheForkIsGone() throws IOException {
        final GitHubBaseGist forked = mock(GitHubBaseGist.class);
        when(forked.id()).thenReturn("fork1");
        final GitHubGistApi api = github.gists().gist();
        doReturn(forked).when(api).forkGist(ID);
        doReturn(Optional.empty()).when(api).getGist("fork1");

        final Throwable thrown = catchThrowable(sut::fork);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void forks_shouldStreamTheForks_whenTheyExist() throws IOException {
        final GitHubGistSimple listed = GistFixtures.gist();
        when(github.gists().gist().listGistForks(eq(ID), any()).stream()).thenReturn(Stream.of(listed));

        final List<Gist> forks = sut.forks().toList();

        assertThat(forks)
                .singleElement()
                .satisfies(fork -> assertThat(fork.response()).isSameAs(listed));
    }

    @Test
    void commits_shouldStreamTheRevisions_whenTheyExist() throws IOException {
        final GitHubGistCommit revision = mock(GitHubGistCommit.class);
        when(github.gists().gist().listGistCommits(eq(ID), any()).stream()).thenReturn(Stream.of(revision));

        final List<GitHubGistCommit> commits = sut.commits().toList();

        assertThat(commits).containsExactly(revision);
    }

    @Test
    void revision_shouldReturnTheGistAtThatRevision_whenGitHubHasIt() throws IOException {
        final GitHubGistSimple old = GistFixtures.gist();
        final GitHubGistApi api = github.gists().gist();
        doReturn(Optional.of(old)).when(api).getGistRevision(ID, "abc");

        final Optional<Gist> found = sut.revision("abc");

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(old);
    }

    @Test
    void revision_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubGistApi api = github.gists().gist();
        doReturn(Optional.empty()).when(api).getGistRevision(ID, "none");

        final Optional<Gist> found = sut.revision("none");

        assertThat(found).isEmpty();
    }

    @Test
    void revision_shouldThrowIllegalArgumentException_whenShaIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.revision(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sha");
    }

    @Test
    void comments_shouldStreamTheComments_whenTheyExist() throws IOException {
        final GitHubGistComment listed = GistFixtures.comment();
        when(github.gists().comments().listComments(eq(ID), any()).stream()).thenReturn(Stream.of(listed));

        final List<GistComment> comments = sut.comments().toList();

        assertThat(comments).singleElement().satisfies(comment -> {
            assertThat(comment.id()).isEqualTo(GistFixtures.COMMENT_ID);
            assertThat(comment.gist()).isSameAs(sut);
        });
    }

    @Test
    void findComment_shouldReturnTheComment_whenGitHubHasIt() throws IOException {
        final GitHubGistsCommentsApi api = github.gists().comments();
        doReturn(Optional.of(GistFixtures.comment())).when(api).getComment(ID, GistFixtures.COMMENT_ID);

        final Optional<GistComment> found = sut.findComment(GistFixtures.COMMENT_ID);

        assertThat(found).isPresent();
    }

    @Test
    void findComment_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubGistsCommentsApi api = github.gists().comments();
        doReturn(Optional.empty()).when(api).getComment(ID, 1L);

        final Optional<GistComment> found = sut.findComment(1L);

        assertThat(found).isEmpty();
    }

    @Test
    void comment_shouldPostTheBodyAndReturnTheComment_whenCalled() throws IOException {
        final GitHubGistsCommentsApi api = github.gists().comments();
        doReturn(GistFixtures.comment()).when(api).createComment(ID, new GitHubCreateCommentPayload("Nice"));

        final GistComment comment = sut.comment("Nice");

        assertSoftly(softly -> {
            softly.assertThat(comment.body()).isEqualTo("Nice");
            softly.assertThat(comment.gist()).isSameAs(sut);
        });
    }

    @Test
    void comment_shouldThrowIllegalArgumentException_whenBodyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.comment(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("body");
    }

    @Test
    void content_shouldThrowIllegalArgumentException_whenTheFileNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.content(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("fileName");
    }
}
