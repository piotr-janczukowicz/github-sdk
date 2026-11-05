package io.github.piotr_janczukowicz.github.sdk.advisories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubCreateRepositoryAdvisoryCommentPayload;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubRepositoryAdvisory;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubRepositoryAdvisoryComment;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubRepositoryAdvisorySeverity;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubRepositoryAdvisoryState;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubSecurityAdvisoriesRepositoryAdvisoriesApi;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubUpdateRepositoryAdvisoryPayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.AdvisoryFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class AdvisoryTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final String GHSA = AdvisoryFixtures.GHSA;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubSecurityAdvisoriesRepositoryAdvisoriesApi api =
            github.securityAdvisories().repositoryAdvisories();
    private final GitHubRepositoryAdvisory response = AdvisoryFixtures.advisory();
    private final Advisory sut = new Advisory(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenAdvisoryIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.ghsaId()).isEqualTo(GHSA);
            softly.assertThat(sut.summary()).isEqualTo("Injection in parser");
            softly.assertThat(sut.severity()).contains(GitHubRepositoryAdvisorySeverity.HIGH);
            softly.assertThat(sut.state()).isEqualTo(GitHubRepositoryAdvisoryState.DRAFT);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new Advisory(repository, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }

    @Test
    void refresh_shouldReturnTheReloadedAdvisory_whenItExists() throws IOException {
        final GitHubRepositoryAdvisory reloaded = AdvisoryFixtures.advisory();
        when(api.getRepositoryAdvisory(OWNER, NAME, GHSA)).thenReturn(Optional.of(reloaded));

        final Advisory result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheAdvisoryIsGone() throws IOException {
        when(api.getRepositoryAdvisory(OWNER, NAME, GHSA)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void update_shouldSendThePayloadAndReturnTheAdvisory_whenCalled() throws IOException {
        final GitHubUpdateRepositoryAdvisoryPayload payload = mock(GitHubUpdateRepositoryAdvisoryPayload.class);
        final GitHubRepositoryAdvisory updated = AdvisoryFixtures.advisory();
        when(api.updateRepositoryAdvisory(OWNER, NAME, GHSA, payload)).thenReturn(updated);

        final Advisory result = sut.update(payload);

        assertThat(result.response()).isSameAs(updated);
    }

    @Test
    void update_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.update(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void comments_shouldStreamTheComments_whenTheyExist() throws IOException {
        final GitHubRepositoryAdvisoryComment comment = mock(GitHubRepositoryAdvisoryComment.class);
        when(api.listRepositoryAdvisoryComments(OWNER, NAME, GHSA).stream()).thenReturn(Stream.of(comment));

        final List<GitHubRepositoryAdvisoryComment> comments = sut.comments().toList();

        assertThat(comments).containsExactly(comment);
    }

    @Test
    void addComment_shouldSendTheBodyAndReturnTheComment_whenCalled() throws IOException {
        final GitHubRepositoryAdvisoryComment created = mock(GitHubRepositoryAdvisoryComment.class);
        when(api.createRepositoryAdvisoryComment(eq(OWNER), eq(NAME), eq(GHSA), any()))
                .thenReturn(created);
        final ArgumentCaptor<GitHubCreateRepositoryAdvisoryCommentPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateRepositoryAdvisoryCommentPayload.class);

        final GitHubRepositoryAdvisoryComment result = sut.addComment("Looking into it");

        verify(api).createRepositoryAdvisoryComment(eq(OWNER), eq(NAME), eq(GHSA), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(result).isSameAs(created);
            softly.assertThat(payload.getValue().body()).isEqualTo("Looking into it");
        });
    }

    @Test
    void addComment_shouldThrowIllegalArgumentException_whenBodyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addComment(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("body");
    }

    @Test
    void requestCve_shouldAskForACveId_whenCalled() throws IOException {
        sut.requestCve();

        verify(api).createRepositoryAdvisoryCveRequest(OWNER, NAME, GHSA);
    }
}
