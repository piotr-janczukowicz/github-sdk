package io.github.piotr_janczukowicz.github.sdk.checks;

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
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckAnnotation;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRun;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRunBuilder;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRunConclusion;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRunStatus;
import io.github.piotr_janczukowicz.github.api.checks.GitHubChecksRunsApi;
import io.github.piotr_janczukowicz.github.api.checks.GitHubUpdateCheckRunConclusion;
import io.github.piotr_janczukowicz.github.api.checks.GitHubUpdateCheckRunPayload;
import io.github.piotr_janczukowicz.github.api.checks.GitHubUpdateCheckRunStatus;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ChecksFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class CheckRunTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = ChecksFixtures.CHECK_RUN_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubCheckRun response = ChecksFixtures.checkRun();
    private final CheckRun sut = new CheckRun(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenCheckRunIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.name()).isEqualTo("build");
            softly.assertThat(sut.headSha()).isEqualTo(ChecksFixtures.SHA);
            softly.assertThat(sut.status()).isEqualTo(GitHubCheckRunStatus.COMPLETED);
            softly.assertThat(sut.conclusion()).hasValue(GitHubCheckRunConclusion.SUCCESS);
            softly.assertThat(sut.detailsUrl()).hasValue("https://ci.example.com/run/5005");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void conclusion_shouldBeEmpty_whenTheCheckHasNotCompleted() {
        final CheckRun running = runningCheckRun();

        final Optional<GitHubCheckRunConclusion> conclusion = running.conclusion();

        assertThat(conclusion).isEmpty();
    }

    @Test
    void detailsUrl_shouldBeEmpty_whenTheCheckHasNoDetails() {
        final CheckRun running = runningCheckRun();

        final Optional<String> detailsUrl = running.detailsUrl();

        assertThat(detailsUrl).isEmpty();
    }

    @Test
    void isCompleted_shouldBeTrue_whenStatusIsCompleted() {
        final boolean completed = sut.isCompleted();

        assertThat(completed).isTrue();
    }

    @Test
    void isCompleted_shouldBeFalse_whenStatusIsInProgress() {
        final CheckRun running = checkRun(GitHubCheckRunStatus.IN_PROGRESS, null);

        final boolean completed = running.isCompleted();

        assertThat(completed).isFalse();
    }

    @Test
    void isSuccessful_shouldBeTrue_whenTheConclusionIsSuccess() {
        final boolean successful = sut.isSuccessful();

        assertThat(successful).isTrue();
    }

    @Test
    void isSuccessful_shouldBeFalse_whenTheConclusionIsFailure() {
        final CheckRun failed = checkRun(GitHubCheckRunStatus.COMPLETED, GitHubCheckRunConclusion.FAILURE);

        final boolean successful = failed.isSuccessful();

        assertThat(successful).isFalse();
    }

    @Test
    void isSuccessful_shouldBeFalse_whenTheCheckIsStillRunning() {
        final CheckRun running = checkRun(GitHubCheckRunStatus.IN_PROGRESS, null);

        final boolean successful = running.isSuccessful();

        assertThat(successful).isFalse();
    }

    @Test
    void annotations_shouldStreamTheAnnotations_whenTheyExist() throws IOException {
        final GitHubCheckAnnotation annotation = mock(GitHubCheckAnnotation.class);
        when(github.checks().runs().listAnnotations(OWNER, NAME, ID).stream()).thenReturn(Stream.of(annotation));

        final List<GitHubCheckAnnotation> annotations = sut.annotations().toList();

        assertThat(annotations).containsExactly(annotation);
    }

    @Test
    void refresh_shouldReturnTheReloadedCheckRun_whenItExists() throws IOException {
        final GitHubCheckRun reloaded = ChecksFixtures.checkRunWith(GitHubCheckRunStatus.COMPLETED, null);
        final GitHubChecksRunsApi runs = github.checks().runs();
        doReturn(Optional.of(reloaded)).when(runs).getCheckRun(OWNER, NAME, ID);

        final CheckRun result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(result.repository()).isSameAs(repository);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheCheckRunIsGone() throws IOException {
        final GitHubChecksRunsApi runs = github.checks().runs();
        doReturn(Optional.empty()).when(runs).getCheckRun(OWNER, NAME, ID);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    private CheckRun runningCheckRun() {
        final GitHubCheckRun running = GitHubCheckRunBuilder.builder(response)
                .status(GitHubCheckRunStatus.IN_PROGRESS)
                .conclusion(null)
                .detailsUrl(null)
                .build();
        return new CheckRun(repository, running);
    }

    private CheckRun checkRun(final GitHubCheckRunStatus status, final GitHubCheckRunConclusion conclusion) {
        return new CheckRun(repository, ChecksFixtures.checkRunWith(status, conclusion));
    }

    @Test
    void update_shouldSendThePayload_whenCalledWithOne() throws IOException {
        final GitHubUpdateCheckRunPayload payload =
                new GitHubUpdateCheckRunPayload("build", null, null, null, null, null, null, null, null);
        final GitHubCheckRun updated = ChecksFixtures.checkRun();
        when(github.checks().runs().updateCheckRun(OWNER, NAME, ID, payload)).thenReturn(updated);

        final CheckRun result = sut.update(payload);

        assertThat(result.response()).isSameAs(updated);
    }

    @Test
    void update_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.update(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void complete_shouldSendCompletedWithTheConclusion_whenCalled() throws IOException {
        final GitHubCheckRun updated = ChecksFixtures.checkRun();
        when(github.checks().runs().updateCheckRun(eq(OWNER), eq(NAME), eq(ID), any()))
                .thenReturn(updated);
        final ArgumentCaptor<GitHubUpdateCheckRunPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateCheckRunPayload.class);

        final CheckRun result = sut.complete(GitHubUpdateCheckRunConclusion.FAILURE);

        verify(github.checks().runs()).updateCheckRun(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(updated);
            softly.assertThat(payload.getValue().status()).isEqualTo(GitHubUpdateCheckRunStatus.COMPLETED);
            softly.assertThat(payload.getValue().conclusion()).isEqualTo(GitHubUpdateCheckRunConclusion.FAILURE);
        });
    }

    @Test
    void rerequest_shouldRerequestTheRun_whenCalled() throws IOException {
        sut.rerequest();

        verify(github.checks().runs()).rerequestRun(OWNER, NAME, ID);
    }
}
