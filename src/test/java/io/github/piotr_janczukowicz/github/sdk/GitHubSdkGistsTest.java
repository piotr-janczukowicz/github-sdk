package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.gists.GitHubBaseGist;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistApi;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistSimple;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.fixtures.GistFixtures;
import io.github.piotr_janczukowicz.github.sdk.gists.Gist;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class GitHubSdkGistsTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sut = GitHubSdk.of(github);

    @Test
    void gist_shouldReturnTheGist_whenGitHubHasIt() throws IOException {
        final GitHubGistSimple loaded = GistFixtures.gist();
        final GitHubGistApi api = github.gists().gist();
        doReturn(Optional.of(loaded)).when(api).getGist(GistFixtures.ID);

        final Optional<Gist> found = sut.gist(GistFixtures.ID);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(loaded);
    }

    @Test
    void gist_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubGistApi api = github.gists().gist();
        doReturn(Optional.empty()).when(api).getGist("none");

        final Optional<Gist> found = sut.gist("none");

        assertThat(found).isEmpty();
    }

    @Test
    void gist_shouldThrowIllegalArgumentException_whenIdIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.gist(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("id");
    }

    @Test
    void gists_shouldLoadEachListedGist_whenTheyExist() throws IOException {
        final GitHubBaseGist listed = GistFixtures.baseGist();
        final GitHubGistSimple loaded = GistFixtures.gist();
        when(github.gists().gist().listGists(any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(listed));
        final GitHubGistApi api = github.gists().gist();
        doReturn(Optional.of(loaded)).when(api).getGist(GistFixtures.ID);

        final List<Gist> gists = sut.gists().toList();

        assertThat(gists)
                .singleElement()
                .satisfies(gist -> assertThat(gist.response()).isSameAs(loaded));
    }

    @Test
    void gists_shouldFail_whenAListedGistIsGone() throws IOException {
        final GitHubBaseGist listed = GistFixtures.baseGist();
        when(github.gists().gist().listGists(any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(listed));
        final GitHubGistApi api = github.gists().gist();
        doReturn(Optional.empty()).when(api).getGist(GistFixtures.ID);
        final Stream<Gist> gists = sut.gists();

        final Throwable thrown = catchThrowable(gists::toList);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void gists_shouldFail_whenLoadingAListedGistFails() throws IOException {
        final GitHubBaseGist listed = GistFixtures.baseGist();
        when(github.gists().gist().listGists(any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(listed));
        final GitHubGistApi api = github.gists().gist();
        doThrow(new IOException("down")).when(api).getGist(GistFixtures.ID);
        final Stream<Gist> gists = sut.gists();

        final Throwable thrown = catchThrowable(gists::toList);

        assertThat(thrown).isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void publicGists_shouldLoadEachListedGist_whenTheyExist() throws IOException {
        final GitHubBaseGist listed = GistFixtures.baseGist();
        final GitHubGistSimple loaded = GistFixtures.gist();
        when(github.gists().gist().listPublicGists(any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(listed));
        final GitHubGistApi api = github.gists().gist();
        doReturn(Optional.of(loaded)).when(api).getGist(GistFixtures.ID);

        final List<Gist> gists = sut.publicGists().toList();

        assertThat(gists)
                .singleElement()
                .satisfies(gist -> assertThat(gist.response()).isSameAs(loaded));
    }

    @Test
    void starredGists_shouldLoadEachListedGist_whenTheyExist() throws IOException {
        final GitHubBaseGist listed = GistFixtures.baseGist();
        final GitHubGistSimple loaded = GistFixtures.gist();
        when(github.gists().gist().listStarredGists(any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(listed));
        final GitHubGistApi api = github.gists().gist();
        doReturn(Optional.of(loaded)).when(api).getGist(GistFixtures.ID);

        final List<Gist> gists = sut.starredGists().toList();

        assertThat(gists)
                .singleElement()
                .satisfies(gist -> assertThat(gist.response()).isSameAs(loaded));
    }
}
