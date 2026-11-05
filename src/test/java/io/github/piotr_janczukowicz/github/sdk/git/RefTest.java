package io.github.piotr_janczukowicz.github.sdk.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitRef;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitRefsApi;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubUpdateRefPayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.GitFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RefTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubGitRef response = GitFixtures.ref();
    private final Ref sut = new Ref(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenRefIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.name()).isEqualTo("refs/heads/main");
            softly.assertThat(sut.sha()).isEqualTo(GitFixtures.SHA);
            softly.assertThat(sut.type()).isEqualTo("commit");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldReturnTheReloadedRef_whenItExists() throws IOException {
        final GitHubGitRef reloaded = GitFixtures.refWith("refs/heads/main", GitFixtures.NEW_SHA);
        final GitHubGitRefsApi refs = github.gitDatabase().refs();
        doReturn(Optional.of(reloaded)).when(refs).getRef(OWNER, NAME, "heads/main");

        final Ref result = sut.refresh();

        assertThat(result.sha()).isEqualTo(GitFixtures.NEW_SHA);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheRefIsGone() throws IOException {
        final GitHubGitRefsApi refs = github.gitDatabase().refs();
        doReturn(Optional.empty()).when(refs).getRef(OWNER, NAME, "heads/main");

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void update_shouldMoveWithoutForce_whenOnlyTheShaIsGiven() throws IOException {
        final GitHubGitRef moved = GitFixtures.refWith("refs/heads/main", GitFixtures.NEW_SHA);
        final GitHubGitRefsApi refs = github.gitDatabase().refs();
        doReturn(moved).when(refs).updateRef(eq(OWNER), eq(NAME), eq("heads/main"), any(GitHubUpdateRefPayload.class));
        final ArgumentCaptor<GitHubUpdateRefPayload> payload = ArgumentCaptor.forClass(GitHubUpdateRefPayload.class);

        final Ref result = sut.update(GitFixtures.NEW_SHA);

        verify(refs).updateRef(eq(OWNER), eq(NAME), eq("heads/main"), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().sha()).isEqualTo(GitFixtures.NEW_SHA);
            softly.assertThat(payload.getValue().force()).isFalse();
            softly.assertThat(result.sha()).isEqualTo(GitFixtures.NEW_SHA);
            softly.assertThat(sut.sha()).isEqualTo(GitFixtures.SHA);
        });
    }

    @Test
    void update_shouldForce_whenAskedTo() throws IOException {
        final GitHubGitRefsApi refs = github.gitDatabase().refs();
        doReturn(response).when(refs).updateRef(eq(OWNER), eq(NAME), eq("heads/main"), any());
        final ArgumentCaptor<GitHubUpdateRefPayload> payload = ArgumentCaptor.forClass(GitHubUpdateRefPayload.class);

        sut.update(GitFixtures.NEW_SHA, true);

        verify(refs).updateRef(eq(OWNER), eq(NAME), eq("heads/main"), payload.capture());
        assertThat(payload.getValue().force()).isTrue();
    }

    @Test
    void update_shouldThrowIllegalArgumentException_whenShaIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.update(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sha");
    }

    @Test
    void delete_shouldDeleteTheRef_whenCalled() throws IOException {
        final GitHubGitRefsApi refs = github.gitDatabase().refs();

        sut.delete();

        verify(refs).deleteRef(OWNER, NAME, "heads/main");
    }

    @Test
    void delete_shouldUseTheNameAsItIs_whenItHasNoRefsPrefix() throws IOException {
        final Ref unprefixed = new Ref(repository, GitFixtures.refWith("heads/feature", GitFixtures.SHA));
        final GitHubGitRefsApi refs = github.gitDatabase().refs();

        unprefixed.delete();

        verify(refs).deleteRef(OWNER, NAME, "heads/feature");
    }
}
