package io.github.piotr_janczukowicz.github.sdk.content;

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
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRun;
import io.github.piotr_janczukowicz.github.api.checks.GitHubListForRefFilter;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCombinedCommitStatus;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitStatus;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitStatusState;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitsStatusesApi;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCreateCommitStatusPayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.checks.CheckRun;
import io.github.piotr_janczukowicz.github.sdk.checks.CombinedStatus;
import io.github.piotr_janczukowicz.github.sdk.checks.CommitStatus;
import io.github.piotr_janczukowicz.github.sdk.checks.CommitStatusState;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ChecksFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class CommitChecksTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final String SHA = ChecksFixtures.SHA;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final Commit sut = new Commit(repository, RepositoryContentFixtures.commit());

    @Test
    void checkRuns_shouldStreamTheCheckRuns_whenTheyExist() throws IOException {
        final GitHubCheckRun listed = ChecksFixtures.checkRun();
        when(github.checks().runs().listForRef(OWNER, NAME, SHA).stream()).thenReturn(Stream.of(listed));

        final List<CheckRun> checkRuns = sut.checkRuns().toList();

        assertThat(checkRuns).singleElement().satisfies(checkRun -> {
            assertThat(checkRun.response()).isSameAs(listed);
            assertThat(checkRun.repository()).isSameAs(repository);
        });
    }

    @Test
    void checkRuns_shouldFilterByName_whenANameIsGiven() throws IOException {
        final GitHubCheckRun listed = ChecksFixtures.checkRun();
        when(github.checks().runs().listForRef(eq(OWNER), eq(NAME), eq(SHA), any(), any()).stream())
                .thenReturn(Stream.of(listed));
        final ArgumentCaptor<GitHubListForRefFilter> filter = ArgumentCaptor.forClass(GitHubListForRefFilter.class);

        final List<CheckRun> checkRuns = sut.checkRuns("build").toList();

        verify(github.checks().runs()).listForRef(eq(OWNER), eq(NAME), eq(SHA), filter.capture(), any());
        assertSoftly(softly -> {
            softly.assertThat(filter.getValue().checkName()).isEqualTo("build");
            softly.assertThat(checkRuns).hasSize(1);
        });
    }

    @Test
    void checkRuns_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.checkRuns(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void statuses_shouldStreamTheStatuses_whenTheyExist() throws IOException {
        final GitHubCommitStatus listed = ChecksFixtures.commitStatus();
        when(github.commits().statuses().listCommitStatusesForRef(OWNER, NAME, SHA).stream())
                .thenReturn(Stream.of(listed));

        final List<CommitStatus> statuses = sut.statuses().toList();

        assertThat(statuses).singleElement().satisfies(status -> {
            assertThat(status.response()).isSameAs(listed);
            assertThat(status.commit()).isSameAs(sut);
        });
    }

    @Test
    void combinedStatus_shouldReturnTheStatus_whenTheCommitExists() throws IOException {
        final GitHubCombinedCommitStatus loaded = ChecksFixtures.combinedStatus("success");
        final GitHubCommitsStatusesApi statuses = github.commits().statuses();
        doReturn(Optional.of(loaded)).when(statuses).getCombinedStatusForRef(OWNER, NAME, SHA);

        final Optional<CombinedStatus> status = sut.combinedStatus();

        assertThat(status).hasValueSatisfying(combined -> {
            assertThat(combined.response()).isSameAs(loaded);
            assertThat(combined.commit()).isSameAs(sut);
        });
    }

    @Test
    void combinedStatus_shouldBeEmpty_whenTheCommitIsGone() throws IOException {
        final GitHubCommitsStatusesApi statuses = github.commits().statuses();
        doReturn(Optional.empty()).when(statuses).getCombinedStatusForRef(OWNER, NAME, SHA);

        final Optional<CombinedStatus> status = sut.combinedStatus();

        assertThat(status).isEmpty();
    }

    @Test
    void createStatus_shouldSendTheStateAndContext_whenOnlyThoseAreGiven() throws IOException {
        final GitHubCommitStatus created = ChecksFixtures.commitStatus();
        when(github.commits().statuses().createCommitStatus(eq(OWNER), eq(NAME), eq(SHA), any()))
                .thenReturn(created);
        final ArgumentCaptor<GitHubCreateCommitStatusPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateCommitStatusPayload.class);

        final CommitStatus status = sut.createStatus(CommitStatusState.SUCCESS, "ci/build");

        verify(github.commits().statuses()).createCommitStatus(eq(OWNER), eq(NAME), eq(SHA), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubCommitStatusState.SUCCESS);
            softly.assertThat(payload.getValue().context()).isEqualTo("ci/build");
            softly.assertThat(payload.getValue().description()).isNull();
            softly.assertThat(payload.getValue().targetUrl()).isNull();
            softly.assertThat(status.response()).isSameAs(created);
            softly.assertThat(status.commit()).isSameAs(sut);
        });
    }

    @Test
    void createStatus_shouldSendDescriptionAndLink_whenTheyAreGiven() throws IOException {
        when(github.commits().statuses().createCommitStatus(eq(OWNER), eq(NAME), eq(SHA), any()))
                .thenReturn(ChecksFixtures.commitStatus());
        final ArgumentCaptor<GitHubCreateCommitStatusPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateCommitStatusPayload.class);

        sut.createStatus(CommitStatusState.PENDING, "ci/build", "Running", "https://ci.example.com/1");

        verify(github.commits().statuses()).createCommitStatus(eq(OWNER), eq(NAME), eq(SHA), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubCommitStatusState.PENDING);
            softly.assertThat(payload.getValue().description()).isEqualTo("Running");
            softly.assertThat(payload.getValue().targetUrl()).isEqualTo("https://ci.example.com/1");
        });
    }

    @Test
    void createStatus_shouldThrowNullPointerException_whenStateIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createStatus(null, "ci/build"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("state");
    }
}
