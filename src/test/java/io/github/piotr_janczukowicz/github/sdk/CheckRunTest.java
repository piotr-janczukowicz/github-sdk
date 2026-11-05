package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckAnnotation;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRun;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRunBuilder;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRunConclusion;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRunStatus;
import io.github.piotr_janczukowicz.github.api.checks.GitHubChecksRunsApi;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

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
    void optionalAccessors_shouldBeEmpty_whenTheCheckHasNotCompleted() {
        final GitHubCheckRun running = GitHubCheckRunBuilder.builder(response)
                .status(GitHubCheckRunStatus.IN_PROGRESS)
                .conclusion(null)
                .detailsUrl(null)
                .build();
        final CheckRun checkRun = new CheckRun(repository, running);

        assertSoftly(softly -> {
            softly.assertThat(checkRun.conclusion()).isEmpty();
            softly.assertThat(checkRun.detailsUrl()).isEmpty();
        });
    }

    @Test
    void outcomeChecks_shouldMatchStatusAndConclusion_whenCalled() {
        final CheckRun passed = sut;
        final CheckRun failed = checkRun(GitHubCheckRunStatus.COMPLETED, GitHubCheckRunConclusion.FAILURE);
        final CheckRun running = checkRun(GitHubCheckRunStatus.IN_PROGRESS, null);

        assertSoftly(softly -> {
            softly.assertThat(passed.isCompleted()).isTrue();
            softly.assertThat(passed.isSuccessful()).isTrue();
            softly.assertThat(failed.isCompleted()).isTrue();
            softly.assertThat(failed.isSuccessful()).isFalse();
            softly.assertThat(running.isCompleted()).isFalse();
            softly.assertThat(running.isSuccessful()).isFalse();
        });
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

    private CheckRun checkRun(final GitHubCheckRunStatus status, final GitHubCheckRunConclusion conclusion) {
        return new CheckRun(repository, ChecksFixtures.checkRunWith(status, conclusion));
    }
}
