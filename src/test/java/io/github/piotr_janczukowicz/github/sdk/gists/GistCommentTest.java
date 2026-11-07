package io.github.piotr_janczukowicz.github.sdk.gists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistComment;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistCommentBuilder;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistsCommentsApi;
import io.github.piotr_janczukowicz.github.api.gists.GitHubUpdateCommentPayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.fixtures.GistFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class GistCommentTest {

    private static final String ID = GistFixtures.ID;
    private static final long COMMENT_ID = GistFixtures.COMMENT_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Gist gist = new Gist(GitHubSdk.of(github), GistFixtures.gist());
    private final GitHubGistComment response = GistFixtures.comment();
    private final GistComment sut = new GistComment(gist, response);

    @Test
    void gist_shouldReturnTheOwningGist_whenCommentIsLoaded() {
        final Gist owner = sut.gist();

        assertThat(owner).isSameAs(gist);
    }

    @Test
    void id_shouldReturnTheId_whenCommentIsLoaded() {
        final long id = sut.id();

        assertThat(id).isEqualTo(COMMENT_ID);
    }

    @Test
    void body_shouldReturnTheBody_whenCommentIsLoaded() {
        final String body = sut.body();

        assertThat(body).isEqualTo("Nice");
    }

    @Test
    void author_shouldReturnTheLogin_whenCommentHasAuthor() {
        final Optional<String> author = sut.author();

        assertThat(author).hasValue("octocat");
    }

    @Test
    void author_shouldBeEmpty_whenGitHubReportsNone() {
        final GistComment anonymous = new GistComment(gist, GistFixtures.commentWithoutAuthor());

        final Optional<String> author = anonymous.author();

        assertThat(author).isEmpty();
    }

    @Test
    void response_shouldReturnTheResponse_whenCommentIsLoaded() {
        final GitHubGistComment result = sut.response();

        assertThat(result).isSameAs(response);
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenGistIsNull() {
        final Throwable thrown = catchThrowable(() -> new GistComment(null, response));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("gist");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new GistComment(gist, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }

    @Test
    void refresh_shouldReturnTheReloadedComment_whenItExists() throws IOException {
        final GitHubGistComment reloaded =
                GitHubGistCommentBuilder.builder(response).body("Edited").build();
        final GitHubGistsCommentsApi api = github.gists().comments();
        doReturn(Optional.of(reloaded)).when(api).getComment(ID, COMMENT_ID);

        final GistComment result = sut.refresh();

        assertThat(result.body()).isEqualTo("Edited");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheCommentIsGone() throws IOException {
        final GitHubGistsCommentsApi api = github.gists().comments();
        doReturn(Optional.empty()).when(api).getComment(ID, COMMENT_ID);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void edit_shouldSendTheBodyAndReturnTheComment_whenCalled() throws IOException {
        final GitHubGistComment edited =
                GitHubGistCommentBuilder.builder(response).body("Edited").build();
        final GitHubGistsCommentsApi api = github.gists().comments();
        doReturn(edited).when(api).updateComment(ID, COMMENT_ID, new GitHubUpdateCommentPayload("Edited"));

        final GistComment result = sut.edit("Edited");

        assertSoftly(softly -> {
            softly.assertThat(result.body()).isEqualTo("Edited");
            softly.assertThat(sut.body()).isEqualTo("Nice");
        });
    }

    @Test
    void edit_shouldThrowIllegalArgumentException_whenBodyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.edit(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("body");
    }

    @Test
    void edit_shouldThrowNullPointerException_whenBodyIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.edit(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("body");
    }

    @Test
    void delete_shouldDeleteTheComment_whenCalled() throws IOException {
        final GitHubGistsCommentsApi api = github.gists().comments();

        sut.delete();

        verify(api).deleteComment(ID, COMMENT_ID);
    }
}
