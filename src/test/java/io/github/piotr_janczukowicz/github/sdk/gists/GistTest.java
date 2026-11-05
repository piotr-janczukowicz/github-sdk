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
import io.github.piotr_janczukowicz.github.api.gists.GitHubBaseGistBuilder;
import io.github.piotr_janczukowicz.github.api.gists.GitHubCreateCommentPayload;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistApi;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistComment;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistCommit;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistSimple;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistSimpleBuilder;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistSimpleFile;
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
    void id_shouldReturnTheId_whenGistIsLoaded() {
        final String id = sut.id();

        assertThat(id).isEqualTo(ID);
    }

    @Test
    void description_shouldReturnTheDescription_whenGistHasOne() {
        final Optional<String> description = sut.description();

        assertThat(description).hasValue("A greeting");
    }

    @Test
    void description_shouldBeEmpty_whenGitHubReportsNone() {
        final Gist bare = new Gist(sdk, GistFixtures.bareGist());

        final Optional<String> description = bare.description();

        assertThat(description).isEmpty();
    }

    @Test
    void isPublic_shouldBeTrue_whenGistIsPublic() {
        final boolean isPublic = sut.isPublic();

        assertThat(isPublic).isTrue();
    }

    @Test
    void isPublic_shouldBeFalse_whenGitHubReportsNothing() {
        final Gist bare = new Gist(sdk, GistFixtures.bareGist());

        final boolean isPublic = bare.isPublic();

        assertThat(isPublic).isFalse();
    }

    @Test
    void htmlUrl_shouldReturnTheUrl_whenGistIsLoaded() {
        final Optional<String> htmlUrl = sut.htmlUrl();

        assertThat(htmlUrl).hasValue("https://gist.github.com/" + ID);
    }

    @Test
    void owner_shouldReturnTheLogin_whenGistHasOwner() {
        final Optional<String> owner = sut.owner();

        assertThat(owner).hasValue("octocat");
    }

    @Test
    void owner_shouldBeEmpty_whenGitHubReportsNone() {
        final Gist bare = new Gist(sdk, GistFixtures.bareGist());

        final Optional<String> owner = bare.owner();

        assertThat(owner).isEmpty();
    }

    @Test
    void files_shouldReturnTheFiles_whenGistIsLoaded() {
        final Map<String, GitHubGistSimpleFile> files = sut.files();

        assertThat(files).containsOnlyKeys(GistFixtures.FILE);
    }

    @Test
    void files_shouldBeEmpty_whenGitHubReportsNone() {
        final Gist bare = new Gist(sdk, GistFixtures.bareGist());

        final Map<String, GitHubGistSimpleFile> files = bare.files();

        assertThat(files).isEmpty();
    }

    @Test
    void content_shouldReturnTheContent_whenTheFileExists() {
        final Optional<String> content = sut.content(GistFixtures.FILE);

        assertThat(content).hasValue("hello");
    }

    @Test
    void content_shouldBeEmpty_whenTheGistHasNoSuchFile() {
        final Gist bare = new Gist(sdk, GistFixtures.bareGist());

        final Optional<String> content = bare.content(GistFixtures.FILE);

        assertThat(content).isEmpty();
    }

    @Test
    void content_shouldBeEmpty_whenTheFileWasCutOff() {
        final Gist cut =
                new Gist(sdk, GistFixtures.gistWithFiles(Map.of(GistFixtures.FILE, GistFixtures.file("hel", true))));

        final Optional<String> content = cut.content(GistFixtures.FILE);

        assertThat(content).isEmpty();
    }

    @Test
    void response_shouldReturnTheResponse_whenGistIsLoaded() {
        final GitHubGistSimple result = sut.response();

        assertThat(result).isSameAs(response);
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenSdkIsNull() {
        final Throwable thrown = catchThrowable(() -> new Gist(null, response));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("sdk");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new Gist(sdk, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenResponseHasNoId() {
        final GitHubGistSimple withoutId =
                GitHubGistSimpleBuilder.builder(response).id(null).build();

        final Throwable thrown = catchThrowable(() -> new Gist(sdk, withoutId));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("id");
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
        final GitHubBaseGist forked = GitHubBaseGistBuilder.builder(GistFixtures.baseGist())
                .id("fork1")
                .build();
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
        final GitHubBaseGist forked = GitHubBaseGistBuilder.builder(GistFixtures.baseGist())
                .id("fork1")
                .build();
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

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(old);
        });
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
    void revision_shouldThrowNullPointerException_whenShaIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.revision(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("sha");
    }

    @Test
    void comments_shouldStreamTheComments_whenTheyExist() throws IOException {
        final GitHubGistComment listed = GistFixtures.comment();
        when(github.gists().comments().listComments(eq(ID), any()).stream()).thenReturn(Stream.of(listed));

        final List<GistComment> comments = sut.comments().toList();

        assertSoftly(softly -> {
            softly.assertThat(comments).hasSize(1);
            softly.assertThat(comments.getFirst().id()).isEqualTo(GistFixtures.COMMENT_ID);
            softly.assertThat(comments.getFirst().gist()).isSameAs(sut);
        });
    }

    @Test
    void findComment_shouldReturnTheComment_whenGitHubHasIt() throws IOException {
        final GitHubGistsCommentsApi api = github.gists().comments();
        doReturn(Optional.of(GistFixtures.comment())).when(api).getComment(ID, GistFixtures.COMMENT_ID);

        final Optional<GistComment> found = sut.findComment(GistFixtures.COMMENT_ID);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().id()).isEqualTo(GistFixtures.COMMENT_ID);
            softly.assertThat(found.orElseThrow().gist()).isSameAs(sut);
        });
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
    void comment_shouldThrowNullPointerException_whenBodyIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.comment(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("body");
    }

    @Test
    void content_shouldThrowIllegalArgumentException_whenTheFileNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.content(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("fileName");
    }

    @Test
    void content_shouldThrowNullPointerException_whenTheFileNameIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.content(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("fileName");
    }
}
