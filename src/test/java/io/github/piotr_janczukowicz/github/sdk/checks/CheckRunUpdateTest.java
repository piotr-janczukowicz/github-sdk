package io.github.piotr_janczukowicz.github.sdk.checks;

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
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRun;
import io.github.piotr_janczukowicz.github.api.checks.GitHubUpdateCheckRunConclusion;
import io.github.piotr_janczukowicz.github.api.checks.GitHubUpdateCheckRunPayload;
import io.github.piotr_janczukowicz.github.api.checks.GitHubUpdateCheckRunStatus;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ChecksFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class CheckRunUpdateTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = ChecksFixtures.CHECK_RUN_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final CheckRun sut = new CheckRun(repository, ChecksFixtures.checkRun());

    @Test
    void send_shouldSendOnlyTheSetFields_whenSeveralAreSet() throws IOException {
        final GitHubCheckRun updated = ChecksFixtures.checkRun();
        when(github.checks().runs().updateCheckRun(eq(OWNER), eq(NAME), eq(ID), any()))
                .thenReturn(updated);
        final ArgumentCaptor<GitHubUpdateCheckRunPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateCheckRunPayload.class);
        final Instant start = Instant.parse("2026-03-04T05:06:07Z");
        final Instant end = Instant.parse("2026-03-04T05:16:07Z");

        final CheckRun result = sut.update()
                .name("build")
                .detailsUrl("https://ci.example.com/1")
                .externalId("ci-1")
                .startedAt(start)
                .status(GitHubUpdateCheckRunStatus.COMPLETED)
                .conclusion(GitHubUpdateCheckRunConclusion.SUCCESS)
                .completedAt(end)
                .output("Title", "Summary")
                .send();

        verify(github.checks().runs()).updateCheckRun(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(updated);
            softly.assertThat(payload.getValue().name()).isEqualTo("build");
            softly.assertThat(payload.getValue().detailsUrl()).isEqualTo("https://ci.example.com/1");
            softly.assertThat(payload.getValue().externalId()).isEqualTo("ci-1");
            softly.assertThat(payload.getValue().startedAt()).isEqualTo(start);
            softly.assertThat(payload.getValue().status()).isEqualTo(GitHubUpdateCheckRunStatus.COMPLETED);
            softly.assertThat(payload.getValue().conclusion()).isEqualTo(GitHubUpdateCheckRunConclusion.SUCCESS);
            softly.assertThat(payload.getValue().completedAt()).isEqualTo(end);
            softly.assertThat(payload.getValue().output().summary()).isEqualTo("Summary");
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingIsSet() {
        final CheckRunUpdate update = sut.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void send_shouldThrowIllegalArgumentException_whenCompletedWithoutAConclusion() {
        final CheckRunUpdate update = sut.update().status(GitHubUpdateCheckRunStatus.COMPLETED);

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("conclusion");
    }

    @Test
    void name_shouldThrowIllegalArgumentException_whenBlank() {
        final CheckRunUpdate update = sut.update();

        final Throwable thrown = catchThrowable(() -> update.name(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void output_shouldThrowIllegalArgumentException_whenSummaryIsBlank() {
        final CheckRunUpdate update = sut.update();

        final Throwable thrown = catchThrowable(() -> update.output("Title", " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("summary");
    }

    @Test
    void setters_shouldThrowNullPointerException_whenAValueIsNull() {
        final CheckRunUpdate update = sut.update();

        final Throwable detailsUrl = catchThrowable(() -> update.detailsUrl(null));
        final Throwable externalId = catchThrowable(() -> update.externalId(null));
        final Throwable startedAt = catchThrowable(() -> update.startedAt(null));
        final Throwable status = catchThrowable(() -> update.status(null));
        final Throwable conclusion = catchThrowable(() -> update.conclusion(null));
        final Throwable completedAt = catchThrowable(() -> update.completedAt(null));

        assertSoftly(softly -> {
            softly.assertThat(detailsUrl).isInstanceOf(NullPointerException.class);
            softly.assertThat(externalId).isInstanceOf(NullPointerException.class);
            softly.assertThat(startedAt).isInstanceOf(NullPointerException.class);
            softly.assertThat(status).isInstanceOf(NullPointerException.class);
            softly.assertThat(conclusion).isInstanceOf(NullPointerException.class);
            softly.assertThat(completedAt).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenCheckRunIsNull() {
        final Throwable thrown = catchThrowable(() -> new CheckRunUpdate(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("checkRun");
    }
}
